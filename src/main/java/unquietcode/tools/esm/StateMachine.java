/*******************************************************************************
 Copyright 2011 Benjamin Fagin

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
public class StateMachine<T extends State>
	implements ControllableStateMachine<T>, ProgrammableStateMachine<T>, RoutableStateMachine<T>
{
	private final Map<State, StateContainer> states = new HashMap<State, StateContainer>();
	private final List<StateRouter<T>> routers = new ArrayList<StateRouter<T>>();
	private final Queue<State> recentStates = new ArrayDeque<State>();
	private final Set<PatternMatcher> matchers = new HashSet<PatternMatcher>();
	private int maxRecent = 0;
	private StateContainer initial;
	private StateContainer current;
	private long transitions = 0;

	public StateMachine() {
		this(null);
	}

	public StateMachine(T initial) {
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
	public synchronized boolean transition(T next) {
		final StateContainer requestedState = getState(next);

		if (!current.transitions.containsKey(requestedState)) {
			throw new TransitionException("No transition exists between "+ current +" and "+ requestedState);
		}

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

		// exit callbacks
		for (StateMachineCallback exitAction : current.exitActions) {
			exitAction.performAction();
		}

		// transition callbacks
		Transition transition = current.transitions.get(nextState);
		for (StateMachineCallback callback : transition.callbacks) {
			callback.performAction();
		}

		// entry callbacks
		for (StateMachineCallback entryAction : nextState.entryActions) {
			entryAction.performAction();
		}

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
	public synchronized HandlerRegistration onEntering(T state, final StateMachineCallback callback) {
		final StateContainer s = getState(state);
		s.entryActions.add(callback);

		return new HandlerRegistration() {
			public void unregister() {
				s.entryActions.remove(callback);
			}
		};
	}

	@Override
	public synchronized HandlerRegistration onExiting(T state, final StateMachineCallback callback) {
		final StateContainer s = getState(state);
		s.exitActions.add(callback);

		return new HandlerRegistration() {
			public void unregister() {
				s.exitActions.remove(callback);
			}
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized HandlerRegistration onTransition(final T from, final T to, final StateMachineCallback callback) {
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
	public synchronized boolean addTransition(T fromState, T toState, StateMachineCallback callback) {
		return addTransitions(callback, true, fromState, Arrays.asList(toState));
	}

	@Override
	public synchronized boolean addTransitions(T fromState, T...toStates) {
		return addTransitions(null, true, fromState, Arrays.asList(toStates));
	}

	@Override
	public synchronized boolean addTransitions(T fromState, List<T> toStates, StateMachineCallback callback) {
		return addTransitions(callback, true, fromState, toStates);
	}

	@Override
	public boolean addTransitions(T fromState, List<T> toStates) {
		return addTransitions(null, true, fromState, toStates);
	}

	@Override
	public boolean addTransitions(StateMachineCallback callback, T fromState, T...toStates) {
		return addTransitions(callback, true, fromState, Arrays.asList(toStates));
	}

	private boolean addTransitions(StateMachineCallback callback, boolean create, T fromState, List<T> toStates) {
		Set<State> set = new HashSet<State>(toStates);
		StateContainer from = getState(fromState);
		boolean modified = false;

		for (State state : set) {
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
		Set<State> set = new HashSet<State>(toStates);
		StateContainer from = getState(fromState);
		boolean modified = false;

		for (State state : set) {
			StateContainer to = getState(state);
			if (from.transitions.remove(to) != null) {
				modified = true;
			}
		}

		if (modified) { reset(); }
		return modified;
	}

	private void removeCallback(StateMachineCallback callback, T fromState, T...toStates) {
		Set<State> set = makeSet(toStates);
		StateContainer from = getState(fromState);

		for (State state : set) {
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
		StateMachine<State> other = (StateMachine) o;

		if (states.size() != other.states.size()) {
			return false;
		}

		for (Map.Entry<State, StateContainer> entry : states.entrySet()) {
			State key = entry.getKey();

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
		for (Map.Entry<State, StateContainer> entry : states.entrySet()) {
			sb.append("\t").append(fullString(entry.getKey())).append(" : {");

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

	private Set<State> makeSet(State states[]) {
		Set<State> set = new HashSet<State>();

		if (states == null) {
			set.add(null);
		} else {
			set.addAll(Arrays.asList(states));
		}

		return set;
	}

	private StateContainer getState(State token) {
		if (states.containsKey(token)) {
			return states.get(token);
		}

		StateContainer s = new StateContainer(token);
		states.put(token, s);
		return s;
	}

	private static class StateContainer {
		final State state;
		final Map<StateContainer, Transition> transitions = new HashMap<StateContainer, Transition>();
		final Set<StateMachineCallback> entryActions = new HashSet<StateMachineCallback>();
		final Set<StateMachineCallback> exitActions = new HashSet<StateMachineCallback>();

		StateContainer(State state) {
			this.state = state;
		}

		public @Override boolean equals(Object obj) {
			if (!(obj instanceof StateContainer)) { return false; }
			StateContainer other = (StateContainer) obj;

			if (this.state == null) {
				return other.state == null;
			} else {
				return this.state.equals(other.state);
			}
		}

		@Override
		public String toString() {
			return state != null ? state.name() : "null";
		}
	}

	private static class Transition {
		final StateContainer next;
		final Set<StateMachineCallback> callbacks = new HashSet<StateMachineCallback>();

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