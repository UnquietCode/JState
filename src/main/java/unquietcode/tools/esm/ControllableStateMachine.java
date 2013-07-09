package unquietcode.tools.esm;

/**
 * Provides methods for controlling a state machine without providing direct access to the
 * underlying object. Essentially an immutable {@link EnumStateMachine} instance.
 *
 * @author Ben Fagin
 * @version 2013-04-02
 */
public interface ControllableStateMachine<T> {

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
	 * Transition the state machine to the next state.
	 *
	 * @param state to transition to
	 * @return true if moved to another state, false if continuing on the same state
	 *
	 * @throws TransitionException if a violation of the available transitions occurs
	 */
	boolean transition(T state) throws TransitionException;

	/**
	 * Returns the current state for this state machine.
	 * The value could change if manipulated externally.
	 *
	 * @return the current state
	 */
	T currentState();

	/**
	 * Returns the initial state set for this state machine.
	 * The value could change if manipulated externally.
	 *
	 * @return the initial state
	 */
	T initialState();

	/**
	 * Resets the state machine to its initial state and clears the transition count.
	 */
	void reset();
}
