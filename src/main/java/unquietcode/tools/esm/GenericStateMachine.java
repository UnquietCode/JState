/*******************************************************************************
 The MIT License (MIT)

 Copyright (c) 2013 Benjamin Fagin

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

package unquietcode.tools.esm;

import unquietcode.tools.esm.routing.StateRouter;
import unquietcode.tools.esm.sequences.Pattern;
import unquietcode.tools.esm.sequences.PatternBuilder;
import unquietcode.tools.esm.sequences.SequenceHandler;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;


/**
 * Note that null is a valid state in this system, and
 * the initial starting point is null by default.
 *
 * @author  Benjamin Fagin
 * @version 12-23-2010
 */
public class GenericStateMachine<T extends State> implements StateMachine<T> {

	// states, and the routers that route them
	private ExecutorService executor = _newExecutor();
	private final Map<StateWrapper, StateContainer> states = new HashMap<>();
	private final List<StateRouter<T>> routers = new ArrayList<>();

	// sequence matching
	private int maxRecent = 0;
	private final Queue<StateContainer> recentStates = new ArrayDeque<>();
	private final Set<PatternMatcher<T>> matchers = new HashSet<>();

	// global handlers
	private final Set<StateHandler<T>> globalOnEntryHandlers = new HashSet<>();
	private final Set<StateHandler<T>> globalOnExitHandlers = new HashSet<>();
	private final Set<TransitionHandler<T>> globalOnTransitionHandlers = new HashSet<>();

	// locks
	private final ReentrantLock transitionLock = new ReentrantLock(true);
	private final ReadWriteLock routingLock = new ReentrantReadWriteLock(true);
	private final Lock sequenceLock = new ReentrantLock(true);

	// backing data
	private StateContainer initial;
	private StateContainer current;
	private long transitions;


	public GenericStateMachine() {
		this(null);
	}

	public GenericStateMachine(T initial) {
		setInitialState(initial);
		reset();
	}

	private void doWithTransitionLock(Runnable fn) {
		doWithLock(transitionLock, fn);
	}

	private <Z> Z doWithTransitionLock(Supplier<Z> fn) {
		return doWithLock(transitionLock, fn);
	}

	private void doWithLock(Lock lock, Runnable fn) {
		doWithLock(lock, (Supplier<Void>) () -> {
			fn.run();
			return null;
		});
	}

	private <Z> Z doWithLock(Lock lock, Supplier<Z> fn) {
		lock.lock();

		try {
			return fn.get();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void reset() {
		doWithTransitionLock(() -> {

			// wait for all tasks to finish first
			executor.shutdown();

			transitions = 0;
			current = initial;
			executor = _newExecutor();

			doWithLock(sequenceLock, () -> {

				// clear all recent states
				recentStates.clear();

				// add back the initial state for pattern matching
				recentStates.add(initial);
			});
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean transition(final T next) throws TransitionException {
		if (transitionLock.isLocked() && transitionLock.isHeldByCurrentThread()) {
			throw new TransitionException("a transition inside of a transition cannot be synchronous");
		}

		Future<Boolean> result = transitionAsync(next);

		try {
			return result.get();
		} catch (InterruptedException e) {
			throw new TransitionException(e);
		} catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			} else {
				throw new TransitionException(e.getCause());
			}
		}
	}

	@Override
	public Future<Boolean> transitionAsync(final T next) throws TransitionException {
		final AtomicReference<StateContainer> _requestedState = new AtomicReference<>();
		final AtomicReference<StateContainer> _nextState = new AtomicReference<>();

		doWithTransitionLock(() -> {
			_requestedState.set(getState(next));
			_nextState.set(route(next, _requestedState.get()));
		});

		final StateContainer requestedState = _requestedState.get();
		final StateContainer nextState = _nextState.get();

		Callable<Boolean> callable = () -> {
			return doWithTransitionLock(() -> {
				if (!current.transitions.containsKey(nextState)) {
					throw new TransitionException("No transition exists between "+current+" and "+requestedState);
				}

				try {
					return _transition(nextState);
				} catch (Exception e) {  // TODO maybe scope this to only our own exception types
					List<Runnable> unfinished = executor.shutdownNow();

					for (Runnable runnable : unfinished) {
						if (runnable instanceof Future) {
							((Future) runnable).cancel(true);
						}
					}

					executor = _newExecutor();
					throw e;
				}
			});
		};

		return executor.submit(callable);
	}

	private boolean _transition(final StateContainer nextState) {
		onExit();
		onTransition(nextState);
		onEntry(nextState);

		transitions += 1;
		doPatternMatching(nextState);

		if (current == nextState) {
			return false;
		} else {
			current = nextState;
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	private void doPatternMatching(StateContainer nextState) {
		final AtomicReference<List<StateContainer>> _recent = new AtomicReference<>();
		final AtomicReference<List<PatternMatcher<T>>> _matchers = new AtomicReference<>();

		doWithLock(sequenceLock, () -> {

			if (maxRecent != 0) {
				recentStates.add(nextState);
			}

			if (recentStates.size() > maxRecent) {
				recentStates.remove();
			}

			_recent.set(new ArrayList<>(recentStates));
			_matchers.set(new ArrayList<>(matchers));
		});

		List<StateContainer> recent = Collections.unmodifiableList(_recent.get());
		List<PatternMatcher<T>> matchers = _matchers.get();

		for (PatternMatcher<T> matcher : matchers) {
			Optional<List<T>> matches = matcher.matches(recent);

			if (matches.isPresent()) {
				matcher.handler.onMatch(matches.get());
			}
		}

	}

	private StateContainer route(T next, StateContainer requestedState) {
		final List<StateRouter<T>> _routers = doWithLock(routingLock.readLock(), () -> {
			return new ArrayList<>(routers);
		});

		StateContainer nextState = null;

		// routing
		for (StateRouter<T> router : _routers) {

			@SuppressWarnings("unchecked")
			T decision = router.route((T) current.state, next);

			if (decision != null) {

				// if it's the same, bypass lookup
				if (decision == next) {
					nextState = requestedState;
				}

				// otherwise lookup the new state
				else {
					nextState = getState(decision);
				}
			}
		}

		// default to the originally requested state
		if (nextState == null) {
			nextState = requestedState;
		}

		return nextState;
	}

	@SuppressWarnings("unchecked")
	private void onEntry(StateContainer nextState) {
		for (StateHandler handler : globalOnEntryHandlers) {
			handler.onState(nextState.state);
		}

		for (StateHandler entryAction : nextState.entryActions) {
			entryAction.onState(nextState.state);
		}
	}

	@SuppressWarnings("unchecked")
	private void onTransition(StateContainer nextState) {
		for (TransitionHandler handler : globalOnTransitionHandlers) {
			handler.onTransition(current.state, nextState.state);
		}

		Transition transition = current.transitions.get(nextState);

		for (TransitionHandler handler : transition.callbacks) {
			handler.onTransition(current.state, nextState.state);
		}
	}

	@SuppressWarnings("unchecked")
	private void onExit() {
		for (StateHandler handler : globalOnExitHandlers) {
			handler.onState(current.state);
		}

		for (StateHandler handler : current.exitActions) {
			handler.onState(current.state);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public T currentState() {
		return doWithTransitionLock(() -> {
			return (T) current.state;
		});
	}

	@Override
	public long transitionCount() {
		return doWithTransitionLock(() -> {
			return this.transitions;
		});
	}

	@Override
	@SuppressWarnings("unchecked")
	public T initialState() {
		return doWithTransitionLock(() -> {
			return (T) initial.state;
		});
	}

	@Override
	public void setInitialState(T state) {
		doWithTransitionLock(() -> {
			initial = getState(state);
		});
	}

	@Override
	public void addAllTransitions(List<T> states, boolean includeSelf) {
		doWithTransitionLock(() -> {
			for (T state : states) {
				List<T> _toStates;

				if (includeSelf) {
					_toStates = states;
				} else {
					_toStates = new ArrayList<T>(states);
					_toStates.remove(state);
				}

				addTransitions(state, _toStates);
			}
		});
	}

	@Override
	public HandlerRegistration onEntering(final StateHandler<T> callback) {
		doWithTransitionLock(() -> {
			globalOnEntryHandlers.add(callback);
		});

		return new HandlerRegistration() {
			public void unregister() {
				doWithTransitionLock(() -> {
					globalOnEntryHandlers.remove(callback);
				});
			}
		};
	}

	@Override
	public HandlerRegistration onEntering(T state, final StateHandler<T> callback) {
		final StateContainer s = doWithTransitionLock(() -> {
			StateContainer _s = getState(state);
			_s.entryActions.add(callback);
			return _s;
		});

		return new HandlerRegistration() {
			public void unregister() {
				doWithTransitionLock(() -> {
					s.entryActions.remove(callback);
				});
			}
		};
	}

	@Override
	public HandlerRegistration onExiting(final StateHandler<T> callback) {
		doWithTransitionLock(() -> {
			globalOnExitHandlers.add(callback);
		});

		return new HandlerRegistration() {
			public void unregister() {
				doWithTransitionLock(() -> {
					globalOnExitHandlers.remove(callback);
				});
			}
		};
	}

	@Override
	public HandlerRegistration onExiting(T state, final StateHandler<T> callback) {
		final StateContainer s = doWithTransitionLock(() -> {
			StateContainer _s = getState(state);
			_s.exitActions.add(callback);
			return _s;
		});

		return new HandlerRegistration() {
			public void unregister() {
				doWithTransitionLock(() -> {
					s.exitActions.remove(callback);
				});
			}
		};
	}

	@Override
	public HandlerRegistration onTransition(final TransitionHandler<T> callback) {
		doWithTransitionLock(() -> {
			globalOnTransitionHandlers.add(callback);
		});

		return new HandlerRegistration() {
			public void unregister() {
				doWithTransitionLock(() -> {
					globalOnTransitionHandlers.remove(callback);
				});
			}
		};
	}


	@Override
	@SuppressWarnings("unchecked")
	public HandlerRegistration onTransition(final T from, final T to, final TransitionHandler<T> callback) {
		doWithTransitionLock(() -> {
			addTransitions(callback, false, from, Collections.singletonList(to));
		});

		return new HandlerRegistration() {
			public void unregister() {
				doWithTransitionLock(() -> {
					removeCallback(callback, from, to);
				});
			}
		};
	}

	@Override
	public HandlerRegistration routeOnTransition(final StateRouter<T> router) {
		if (router == null) {
			throw new IllegalArgumentException("router cannot be null");
		}

		doWithLock(routingLock.writeLock(), () -> {
			routers.add(router);
		});

		return new HandlerRegistration() {
			public void unregister() {
				doWithLock(routingLock.writeLock(), () -> {
					routers.remove(router);
				});
			}
		};
	}

	@Override
	public HandlerRegistration routeOnTransition(final T from, final T to, final StateRouter<T> router) {
		return routeOnTransition(new StateRouter<T>() {
			public T route(T current, T next) {

				// only route if it matches the pattern
				if (current == from && next == to) {
					return router.route(current, next);
				} else {
					return null;
				}
			}
		});
	}

	@Override
	public HandlerRegistration routeBeforeEntering(final T to, final StateRouter<T> router) {
		return routeOnTransition(new StateRouter<T>() {
			public T route(T current, T next) {

				// only route if it matches the pattern
				if (next == to) {
					return router.route(current, next);
				} else {
					return null;
				}
			}
		});
	}

	@Override
	public HandlerRegistration routeAfterExiting(final T from, final StateRouter<T> router) {
		return routeOnTransition(new StateRouter<T>() {
			public T route(T current, T next) {

				// only route if it matches the pattern
				if (current == from) {
					return router.route(current, next);
				} else {
					return null;
				}
			}
		});
	}

	@Override
	public HandlerRegistration onSequence(Pattern<T> pattern, SequenceHandler<T> handler) {
		final PatternMatcher<T> matcher = new PatternMatcher<>(pattern, handler);

		doWithLock(sequenceLock, () -> {
			matchers.add(matcher);

			// recalculate the cache size on add
			maxRecent = Math.max(maxRecent, pattern.length());
		});

		return () -> {
			doWithLock(sequenceLock, () -> {
				matchers.remove(matcher);

				// recalculate the cache size on remove
				Optional<Integer> max = matchers.stream()
					.map(e -> e.pattern.length())
					.max(Integer::compareTo);

				maxRecent = max.orElse(0);
			});
		};
	}

	@Override
	public boolean addTransition(T fromState, T toState) {
		return addTransitions(null, true, fromState, Collections.singletonList(toState));
	}

	@Override
	public boolean addTransition(T fromState, T toState, TransitionHandler<T> callback) {
		return addTransitions(callback, true, fromState, Collections.singletonList(toState));
	}

	@Override
	public boolean addTransitions(T fromState, T...toStates) {
		return addTransitions(null, true, fromState, Arrays.asList(toStates));
	}

	@Override
	public boolean addTransitions(T fromState, List<T> toStates, TransitionHandler<T> callback) {
		return addTransitions(callback, true, fromState, toStates);
	}

	@Override
	public boolean addTransitions(T fromState, List<T> toStates) {
		return addTransitions(null, true, fromState, toStates);
	}

	@Override
	public boolean addTransitions(TransitionHandler<T> callback, T fromState, T...toStates) {
		return addTransitions(callback, true, fromState, Arrays.asList(toStates));
	}

	private boolean addTransitions(TransitionHandler<T> callback, boolean create, T fromState, List<T> toStates) {
		Set<T> set = new HashSet<>(toStates);

		return doWithTransitionLock(() -> {
			StateContainer from = getState(fromState);
			boolean modified = false;

			for (T state : set) {
				StateContainer to = getState(state);
				Transition transition = null;

				if (from.transitions.containsKey(to)) {
					transition = from.transitions.get(to);
				} else if (create) {
					transition = new Transition(to);
					from.transitions.put(to, transition);
					modified = true;
				}

				if (transition != null && callback != null) {
					transition.callbacks.add(callback);
				}
			}

			if (modified) { reset(); }
			return modified;
		});
	}

	@Override
	public boolean removeTransitions(T fromState, T...toStates) {
		return removeTransitions(fromState, Arrays.asList(toStates));
	}

	@Override
	public boolean removeTransitions(T fromState, List<T> toStates) {
		Set<T> set = new HashSet<>(toStates);

		return doWithTransitionLock(() -> {
			StateContainer from = getState(fromState);
			boolean modified = false;

			for (T state : set) {
				StateContainer to = getState(state);

				if (from.transitions.remove(to) != null) {
					modified = true;
				}
			}

			if (modified) { reset(); }
			return modified;
		});
	}

	private void removeCallback(TransitionHandler callback, T fromState, T...toStates) {
		Set<T> set = new HashSet<T>(Arrays.asList(toStates));

		doWithTransitionLock(() -> {
			StateContainer from = getState(fromState);

			for (T state : set) {
				StateContainer to = getState(state);
				Transition transition = from.transitions.get(to);

				if (transition != null) {
					transition.callbacks.remove(callback);
				}
			}
		});
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		doWithTransitionLock(() -> {
			if (initial != null) {
				sb.append(fullString(initial.state)).append(" | \n");
			}

			int i = 1;
			Map<StateWrapper, StateContainer> sortedStates = new TreeMap<>(states);

			for (Map.Entry<StateWrapper, StateContainer> entry : sortedStates.entrySet()) {
				sb.append("\t").append(fullString(entry.getKey().state)).append(" : {");

				int j = 1;
				Map<StateContainer, Transition> sortedTransitions = new TreeMap<>(entry.getValue().transitions);

				for (Transition t : sortedTransitions.values()) {
					sb.append(fullString(t.next.state));

					if (j++ != sortedTransitions.size()) {
						sb.append(", ");
					}
				}

				sb.append("}");

				if (i++ != states.size()) {
					sb.append(" | \n");
				}
			}
		});

		return sb.toString();
	}

	private StateContainer getState(T token) {
		StateWrapper wrapped = new StateWrapper(token);

		return doWithTransitionLock(() -> {
			if (states.containsKey(wrapped)) {
				return states.get(wrapped);
			}

			StateContainer s = new StateContainer(token);
			states.put(wrapped, s);
			return s;
		});
	}

	private static class StateContainer implements Comparable<StateContainer> {
		final State state;
		final Map<StateContainer, Transition> transitions = new HashMap<>();
		final Set<StateHandler> entryActions = new HashSet<>();
		final Set<StateHandler> exitActions = new HashSet<>();

		StateContainer(State state) {
			this.state = state;
		}

		@Override
		public String toString() {
			return state != null ? state.name() : "null";
		}

		@Override
		public int compareTo(StateContainer other) {
			String n1 = state == null ? "" : state.name();
			String n2 = other == null ? "" : other.state.name();
			return n1.compareTo(n2);
		}
	}

	private static class StateWrapper implements Comparable<StateWrapper> {
		private final State state;

		StateWrapper(State state) {
			this.state = state;
		}

		@Override
		public int compareTo(StateWrapper other) {
			String n1 = state == null ? "" : state.name();
			String n2 = other.state == null ? "" : other.state.name();
			return n1.compareTo(n2);
		}

		@Override
		public boolean equals(Object obj) {
			StateWrapper that = (StateWrapper) obj;

			String s1 = this.state != null ? this.state.name().trim() : null;
			String s2 = that.state != null ? that.state.name().trim() : null;

			if (s1 == null ^ s2 == null) {
				return false;
			}

			if (s1 == null) {
				return true;
			}

			return s1.equals(s2);
		}

		@Override
		public int hashCode() {
			return state != null ? state.name().trim().hashCode() : 0;
		}
	}

	private static class Transition {
		final StateContainer next;
		final Set<TransitionHandler> callbacks = new HashSet<>();

		Transition(StateContainer next) {
			this.next = next;
		}

//		public @Override boolean equals(Object obj) {
//			if (!(obj instanceof Transition)) { return false; }
//			Transition other = (Transition) obj;
//
//			if (this.next.theEnum == null) {
//				return other.next.theEnum == null;
//			} else {
//				return this.next.theEnum.equals(other.next.theEnum);
//			}
//		}
	}

	private static class PatternMatcher<T> {
		private final Pattern<T> pattern;
		private final SequenceHandler handler;

		PatternMatcher(Pattern<T> pattern, SequenceHandler handler) {
			this.pattern = pattern;
			this.handler = handler;
		}

		Optional<List<T>> matches(List<StateContainer> states) {
			List<T> matches = new ArrayList<>();
			List<Object> patternStates = this.pattern.pattern();

			for (int i = patternStates.size() - 1; i >= 0; --i) {
				int j = states.size() - (patternStates.size() - i);

				if (j < 0) {
					return Optional.empty();
				}

				Object matchState = patternStates.get(i);
				State recentState = states.get(j).state;

				if (PatternBuilder.isWildcard(matchState)) {
					// nothing, do no state checking
				} else if (matchState == null) {
					if (recentState != null) {
						return Optional.empty();
					}
				} else if (recentState == null) {
					return Optional.empty();
				} else if (!matchState.equals(recentState)) {
					return Optional.empty();
				}

				@SuppressWarnings("unchecked")
				T recentState_ = (T) recentState;
				matches.add(recentState_);
			}

			Collections.reverse(matches);
			return Optional.of(matches);
		}
	}

	private static String fullString(State state) {
		return state != null ? state.name() : null;
	}

	private static ExecutorService _newExecutor() {
		return Executors.newSingleThreadExecutor();
	}
}