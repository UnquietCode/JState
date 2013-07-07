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
public class StateMachine<T extends State> implements IStateMachine<T> {
	private Map<State, StateContainer> states = new IdentityHashMap<State, StateContainer>();
	private StateContainer initial;
	private StateContainer current;
	private long transitions = 0;

	public StateMachine() {
		this(null);
	}

	public StateMachine(T initial) {
		this.initial = getState(initial);
		current = this.initial;
	}

	@Override
	public synchronized void reset() {
		transitions = 0;
		current = initial;
	}

	@Override
	public synchronized boolean transition(T state) {
		StateContainer next = getState(state);
		if (!current.transitions.containsKey(next)) {
			throw new TransitionException("No transition exists between "+ current +" and "+ next);
		}

		// exit callbacks
		for (StateMachineCallback exitAction : current.exitActions) {
			exitAction.performAction();
		}

		// transition callbacks
		Transition transition = current.transitions.get(next);
		for (StateMachineCallback callback : transition.callbacks) {
			callback.performAction();
		}

		// entry callbacks
		for (StateMachineCallback entryAction : next.entryActions) {
			entryAction.performAction();
		}

		// officially finish the transition
		transitions += 1;

		if (current == next) {
			return false;
		} else {
			current = next;
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
	public synchronized void onEntering(T state, StateMachineCallback callback) {
		StateContainer s = getState(state);
		s.entryActions.add(callback);
	}

	@Override
	public synchronized void onExiting(T state, StateMachineCallback callback) {
		StateContainer s = getState(state);
		s.exitActions.add(callback);
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized void onTransition(T from, T to, StateMachineCallback callback) {
		addTransitions(callback, false, from, to);
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized boolean addTransition(T fromState, T toState) {
		return addTransitions(null, true, fromState, toState);
	}

	@Override
	@SuppressWarnings("unchecked")
	public synchronized boolean addTransition(T fromState, T toState, StateMachineCallback callback) {
		return addTransitions(callback, true, fromState, toState);
	}

	@Override
	public synchronized boolean addTransitions(T fromState, T... toStates) {
		return addTransitions(null, true, fromState, toStates);
	}

	@Override
	public synchronized boolean addTransitions(T fromState, T[] toStates, StateMachineCallback callback) {
		return addTransitions(callback, true, fromState, toStates);
	}

	/*
		Gets around the inability to create generic arrays by flipping the
		callback parameter position, thus freeing up the vararg parameter.
	 */
	@Override
	public synchronized boolean addTransitions(StateMachineCallback callback, T fromState, T... toStates) {
		return addTransitions(callback, true, fromState, toStates);
	}

	private boolean addTransitions(StateMachineCallback callback, boolean create, T fromState, T...toStates) {
		Set<State> set = makeSet(toStates);
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
	public synchronized boolean removeTransitions(T fromState, T... toStates) {
		Set<State> set = makeSet(toStates);
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

	@Override
	public synchronized void setTransitions(T fromState, T... toStates) {
		setTransitions(null, fromState, toStates);
	}

	@Override
	public synchronized void setTransitions(StateMachineCallback callback, T fromState, T... toStates) {
		StateContainer state = getState(fromState);
		state.transitions.clear();
		addTransitions(callback, fromState, toStates);
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

	private String fullString(State state) {
		return state != null ? state.getClass().getName() + "." + state.toString() : null;
	}
}