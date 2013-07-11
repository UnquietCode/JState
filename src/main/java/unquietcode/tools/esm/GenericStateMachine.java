/*******************************************************************************
 Copyright 2013 Benjamin Fagin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


    Read the included LICENSE.TXT for more information.
 ******************************************************************************/

package unquietcode.tools.esm;

import java.util.*;


/**
 * Note that null is a valid state
 * in this system. The initial starting point is null by default.
 *
 * @author  Benjamin Fagin
 * @version 12-23-2010
 */
public class GenericStateMachine<T extends State> implements StateMachine<T> {

	// states, and the routers that route them
	private final Map<StateWrapper, StateContainer> states = new HashMap<StateWrapper, StateContainer>();
	private final List<StateRouter<T>> routers = new ArrayList<StateRouter<T>>();

	// sequence matching
	private int maxRecent = 0;
	private final Queue<State> recentStates = new ArrayDeque<State>();
	private final Set<PatternMatcher> matchers = new HashSet<PatternMatcher>();

	// global handlers
	private final Set<StateHandler<T>> globalOnEntryHandlers = new HashSet<StateHandler<T>>();
	private final Set<StateHandler<T>> globalOnExitHandlers = new HashSet<StateHandler<T>>();
	private final Set<TransitionHandler<T>> globalOnTransitionHandlers = new HashSet<TransitionHandler<T>>();

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

	@Override
	public synchronized void reset() {
		transitions = 0;
		current = initial;
		recentStates.clear();
	}

	/*
		Something like, always add a callback to a queue,
		so default is async, but if hits from the non-async
		method then immediately pull the CB off the queue and execute it.

		Or, returns a callback and can either execute it or queue it.

		Either way, larger effort will be in the transition method to detect
		that there is outstanding work and defer the transition (if not async)
		or queue it (if async).
	 */

	@Override
	@SuppressWarnings("unchecked")
	public synchronized boolean transition(final T next) {
		final StateContainer requestedState = getState(next);
		final StateContainer nextState = route(next, requestedState);

		if (!current.transitions.containsKey(nextState)) {
 			throw new TransitionException("No transition exists between "+ current +" and "+ requestedState);
		}

		onExit();
		onTransition(nextState);
		onEntry(nextState);

		// officially finish the transition
		transitions += 1;

		// async matching
		if (maxRecent != 0) {
			recentStates.add(nextState.state);
		}

		if (recentStates.size() > maxRecent) {
			recentStates.remove();
		}

		final List<State> recent
			= Collections.unmodifiableList(new ArrayList<State>(recentStates));

		for (PatternMatcher matcher : matchers) {
			if (matcher.matches(recent.iterator(), recent.size())) {
				matcher.handler.onMatch(matcher.pattern);
			}
		}

		if (current == nextState) {
			return false;
		} else {
			current = nextState;
			return true;
		}
	}

	@SuppressWarnings("unchecked")
	private StateContainer route(T next, StateContainer requestedState) {
		StateContainer nextState = null;

		// routing
		for (StateRouter<T> router : routers) {
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
	public synchronized T currentState() {
		return (T) current.state;
	}

	@Override
	public synchronized long transitionCount() {
		return transitions;
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized T initialState() {
		return (T) initial.state;
	}

	@Override
	public synchronized void setInitialState(T state) {
		initial = getState(state);
	}

	@Override
	public synchronized void addAllTransitions(List<T> states, boolean includeSelf) {
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
	}

	@Override
	public HandlerRegistration onEntering(final StateHandler<T> callback) {
		globalOnEntryHandlers.add(callback);

		return new HandlerRegistration() {
			public void unregister() {
				globalOnEntryHandlers.remove(callback);
			}
		};
	}

	@Override
	public synchronized HandlerRegistration onEntering(T state, final StateHandler<T> callback) {
		final StateContainer s = getState(state);
		s.entryActions.add(callback);

		return new HandlerRegistration() {
			public void unregister() {
				s.entryActions.remove(callback);
			}
		};
	}

	@Override
	public HandlerRegistration onExiting(final StateHandler<T> callback) {
		globalOnExitHandlers.add(callback);

		return new HandlerRegistration() {
			public void unregister() {
				globalOnExitHandlers.remove(callback);
			}
		};
	}

	@Override
	public synchronized HandlerRegistration onExiting(T state, final StateHandler<T> callback) {
		final StateContainer s = getState(state);
		s.exitActions.add(callback);

		return new HandlerRegistration() {
			public void unregister() {
				s.exitActions.remove(callback);
			}
		};
	}

	@Override
	public HandlerRegistration onTransition(final TransitionHandler<T> callback) {
		globalOnTransitionHandlers.add(callback);

		return new HandlerRegistration() {
			public void unregister() {
				globalOnTransitionHandlers.remove(callback);
			}
		};
	}


	@Override
	@SuppressWarnings("unchecked")
	public synchronized HandlerRegistration onTransition(final T from, final T to, final TransitionHandler<T> callback) {
		addTransitions(callback, false, from, Arrays.asList(to));

		return new HandlerRegistration() {
			public void unregister() {
				removeCallback(callback, from, to);
			}
		};
	}

	@Override
	public synchronized HandlerRegistration routeOnTransition(final StateRouter<T> router) {
		if (router == null) {
			throw new IllegalArgumentException("router cannot be null");
		}

		routers.add(router);

		return new HandlerRegistration() {
			public void unregister() {
				routers.remove(router);
			}
		};
	}

	@Override
	public synchronized HandlerRegistration routeOnTransition(final T from, final T to, final StateRouter<T> router) {
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
	public synchronized HandlerRegistration routeBeforeEntering(final T to, final StateRouter<T> router) {
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
	public synchronized HandlerRegistration routeAfterExiting(final T from, final StateRouter<T> router) {
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
	public synchronized HandlerRegistration onSequence(List<T> pattern, SequenceHandler<T> handler) {
		List<State> _pattern = Collections.<State>unmodifiableList(pattern);
		final PatternMatcher matcher = new PatternMatcher(_pattern, handler);
		matchers.add(matcher);
		maxRecent = Math.max(maxRecent, pattern.size());

		return new HandlerRegistration() {
			public void unregister() {
				matchers.remove(matcher);
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized boolean addTransition(T fromState, T toState) {
		return addTransitions(null, true, fromState, Arrays.asList(toState));
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized boolean addTransition(T fromState, T toState, TransitionHandler<T> callback) {
		return addTransitions(callback, true, fromState, Arrays.asList(toState));
	}

	@Override
	public synchronized boolean addTransitions(T fromState, T...toStates) {
		return addTransitions(null, true, fromState, Arrays.asList(toStates));
	}

	@Override
	public synchronized boolean addTransitions(T fromState, List<T> toStates, TransitionHandler<T> callback) {
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
		Set<T> set = new HashSet<T>(toStates);
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
	}

	@Override
	public synchronized boolean removeTransitions(T fromState, T...toStates) {
		return removeTransitions(fromState, Arrays.asList(toStates));
	}

	@Override
	public boolean removeTransitions(T fromState, List<T> toStates) {
		Set<T> set = new HashSet<T>(toStates);
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
	}

	private void removeCallback(TransitionHandler callback, T fromState, T...toStates) {
		Set<T> set = new HashSet<T>(Arrays.asList(toStates));
		StateContainer from = getState(fromState);

		for (T state : set) {
			StateContainer to = getState(state);
			Transition transition = from.transitions.get(to);

			if (transition != null) {
				transition.callbacks.remove(callback);
			}
		}
	}

	/**
	 * Two state machines are considered equal if they both have the
	 * same number of states, and the same number of transitions.
	 *
	 * The transition actions (entry, exit, and transition callbacks) do
	 * not count towards equality.
	 *
	 * Overall, this is not the fastest method and should be used
	 * lightly.
	 */
	@Override
	public boolean equals(Object o) {
		if (o.getClass() != this.getClass()) {
			return false;
		}

		@SuppressWarnings("unchecked")
		GenericStateMachine<State> other = (GenericStateMachine) o;

		if (states.size() != other.states.size()) {
			return false;
		}

		for (Map.Entry<StateWrapper, StateContainer> entry : states.entrySet()) {
			State key = entry.getKey().state;

			if (!other.states.containsKey(key)) {
				return false;
			}

			StateContainer tState = entry.getValue();
			StateContainer oState = other.states.get(key);

			if (tState.transitions.size() != oState.transitions.size()) {
				return false;
			}

			for (StateContainer s : tState.transitions.keySet()) {
				boolean found = false;

				for (StateContainer os : oState.transitions.keySet()) {
					if (s.state == null) {
						if (os.state == null) {
							found = true;
							break;
						}
					} else {
						if (s.state.equals(os.state)) {
							found = true;
							break;
						}
					}
				}

				if (!found) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (initial != null) {
			sb.append(fullString(initial.state)).append(" | \n");
		}

		int i = 1;
		for (Map.Entry<StateWrapper, StateContainer> entry : states.entrySet()) {
			sb.append("\t").append(fullString(entry.getKey().state)).append(" : {");

			int j = 1;
			for (Transition t : entry.getValue().transitions.values()) {
				sb.append(fullString(t.next.state));

				if (j++ != entry.getValue().transitions.size()) {
					sb.append(", ");
				}
			}

			sb.append("}");

			if (i++ != states.size())
				sb.append(" | \n");
		}

		return sb.toString();
	}

	private StateContainer getState(T token) {
		StateWrapper wrapped = new StateWrapper(token);

		if (states.containsKey(wrapped)) {
			return states.get(wrapped);
		}

		StateContainer s = new StateContainer(token);
		states.put(wrapped, s);
		return s;
	}

	private static class StateContainer {
		final State state;
		final Map<StateContainer, Transition> transitions = new HashMap<StateContainer, Transition>();
		final Set<StateHandler> entryActions = new HashSet<StateHandler>();
		final Set<StateHandler> exitActions = new HashSet<StateHandler>();

		StateContainer(State state) {
			this.state = state;
		}

		@Override
		public String toString() {
			return state != null ? state.name() : "null";
		}
	}

	private static class StateWrapper {
		private final State state;

		StateWrapper(State state) {
			this.state = state;
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
		final Set<TransitionHandler> callbacks = new HashSet<TransitionHandler>();

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

	private static class PatternMatcher {
		private final List<State> pattern;
		private final SequenceHandler handler;

		PatternMatcher(List<State> pattern, SequenceHandler handler) {
			this.pattern = pattern;
			this.handler = handler;
		}

		boolean matches(Iterator<State> it, int size) {
			if (size < pattern.size()) {
				return false;
			}

			for (State state : pattern) {
				if (!state.equals(it.next())) {
					return false;
				}
			}

			return true;
		}
	}

	private static String fullString(State state) {
		return state != null ? state.getClass().getName() + "." + state.toString() : null;
	}
}