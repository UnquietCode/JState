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

import unquietcode.tools.esm.sequences.Pattern;
import unquietcode.tools.esm.sequences.PatternBuilder;
import unquietcode.tools.esm.sequences.SequenceHandler;

import java.util.List;

/**
 * @author Ben Fagin
 * @version 2013-07-07
 */
public interface ProgrammableStateMachine<T> {

	/**
	 * Will not reset, just sets the initial state.
	 *
	 * @param state initial state to be set after next reset
	 */
	void setInitialState(T state);

	/**
	 * Adds a callback which will be executed whenever any state
	 * is entered.
	 */
	HandlerRegistration onEntering(StateHandler<T> callback);

	/**
	 * Adds a callback which will be executed whenever the specified state
	 * is entered, via any transition.
	 */
	HandlerRegistration onEntering(T state, StateHandler<T> callback);

	/**
	 * Adds a callback which will be executed whenever any state
	 * is exited.
	 */
	HandlerRegistration onExiting(StateHandler<T> callback);

	/**
	 * Adds a callback which will be executed whenever the specified state
	 * is exited, via any transition.
	 */
	HandlerRegistration onExiting(T state, StateHandler<T> callback);

	/**
	 * Adds a callback which will be executed whenever any state
	 * transitions to another state.
	 */
	HandlerRegistration onTransition(TransitionHandler<T> callback);

	/**
	 * Adds a callback which will be executed whenever the specified state
	 * is exited, via any transition.
	 */
	HandlerRegistration onTransition(T from, T to, TransitionHandler<T> callback);

	/**
	 * Adds a callback which will be executed whenever the specified sequence
	 * of states has been traversed.
	 *
	 * @param pattern to match
	 * @param handler to handle the match
	 * @return registration to assist in removing the handler
	 */
	default HandlerRegistration onSequence(List<T> pattern, SequenceHandler<T> handler) {
		Pattern<T> pattern_ = PatternBuilder.createFrom(pattern);
		return onSequence(pattern_, handler);
	}

	HandlerRegistration onSequence(Pattern<T> pattern, SequenceHandler<T> handler);

	/*
		Adds a transition from one state to another.
	 */
	boolean addTransition(T fromState, T toState);

	/**
	 * Add a transition between one and one-or-more states.
	 *
	 * @param fromState the initial state
	 * @param toStates one or more states to move to
	 */
	boolean addTransitions(T fromState, T...toStates);

	/*
		Adds a transition from one state to one-or-many states.
    */
	boolean addTransitions(T fromState, List<T> toStates);

	/*
		Adds a transition from one state to another, and adds a callback.
	 */
	boolean addTransition(T fromState, T toState, TransitionHandler<T> callback);

	/*
	    Add a transition between one and one-or-more states, and
	    provide a callback to execute.
	 */
	boolean addTransitions(TransitionHandler<T> callback, T fromState, T...toStates);

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
	boolean addTransitions(T fromState, List<T> toStates, TransitionHandler<T> callback);

	/**
	 * For every state in the list, create a transition to every other
	 * state. If the includeSelf parameter is true, then each state will
	 * also have a transition added which loops back to itself.
	 *
	 * @param states to add
	 * @param includeSelf if we should add loops as well
	 */
	void addAllTransitions(List<T> states, boolean includeSelf);

	/**
	 * Removes the set of transitions from the given state.
	 * When the state machine is modified, this method will
	 * return true and the state machine will be reset.
	 *
	 * @param fromState from state
	 * @param toStates to states
	 * @return true if the transitions were modified, false otherwise
	 */
	boolean removeTransitions(T fromState, T...toStates);

	boolean removeTransitions(T fromState, List<T> toStates);
}
