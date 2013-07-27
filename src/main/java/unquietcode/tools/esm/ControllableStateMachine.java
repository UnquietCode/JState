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
	 * that the exit callbacks, transition callbacks, and entry callbacks have not
	 * all been completed.
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
