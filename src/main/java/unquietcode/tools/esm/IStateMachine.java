package unquietcode.tools.esm;

/**
 * @author Ben Fagin
 * @version 2013-07-07
 */
public interface IStateMachine<T> extends StateMachineController<T> {

	/**
	 * Get the total number of transitions performed by the state machine, since
	 * construction or the most recent call to {@link #reset()}. Transitions which
	 * are in progress do not count towards the overall count. In progress means
	 * that the exit callbacks, transition callbacks, and entry callbacks have all
	 * been completed for a given transition.
	 *
	 * @return the current number of transitions performed
	 */
	long transitionCount();

	/**
	 * Will not reset, just sets the initial state.
	 *
	 * @param state initial state to be set after next reset
	 */
	void setInitialState(T state);

	/**
	 * Adds a callback which will be executed whenever the specified state
	 * is entered, via any transition.
	 */
	CallbackRegistration onEntering(T state, StateMachineCallback callback);

	/**
	 * Adds a callback which will be executed whenever the specified state
	 * is exited, via any transition.
	 */
	CallbackRegistration onExiting(T state, StateMachineCallback callback);

	/**
	 * Adds a callback which will be executed whenever the specified state
	 * is exited, via any transition.
	 */
	CallbackRegistration onTransition(T from, T to, StateMachineCallback callback);

	boolean addTransition(T fromState, T toState);
	boolean addTransition(T fromState, T toState, StateMachineCallback callback);

	/**
	 * Add a transition between two states.
	 * @param fromState the initial state
	 * @param toStates one or more states to move to
	 */
	boolean addTransitions(T fromState, T...toStates);

	/**
	 * Add a transition from one state to 0..n other states. The callback
	 * will be executed as the transition is occurring. If the state machine
	 * is modified during this operation, it will be reset. Adding a new
	 * callback to an existing transition will not be perceived as modification.
	 *
	 * @param callback callback, can be null
	 * @param fromState state moving from
	 * @param toStates states moving to
	 * @return true if the state machine was modified and a reset occurred, false otherwise
	 */
	boolean addTransitions(T fromState, T[] toStates, StateMachineCallback callback);

	/*
		Gets around the inability to create generic arrays by flipping the
		callback parameter position, thus freeing up the vararg parameter.
	 */
	boolean addTransitions(StateMachineCallback callback, T fromState, T...toStates);

	/**
	 * Removes the set of transitions from the given state.
	 * When the state machine is modified, this method will
	 * return true and the state machine will be reset.
	 *
	 * @param fromState from state
	 * @param toStates to states
	 * @return true if the transitions were modified, false otherwise
	 */
	boolean removeTransitions(T fromState, T... toStates);

	void setTransitions(T fromState, T... toStates);
	void setTransitions(StateMachineCallback callback, T fromState, T... toStates);
}
