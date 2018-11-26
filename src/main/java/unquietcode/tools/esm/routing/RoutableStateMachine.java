/*******************************************************************************
 The MIT License (MIT)

 Copyright (c) 2018 Benjamin Fagin

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

package unquietcode.tools.esm.routing;

import unquietcode.tools.esm.HandlerRegistration;

/**
 * @author Ben Fagin
 * @version 2013-07-08
 */
public interface RoutableStateMachine<T> {

	/**
	 * Add a new router to the state machine. Order matters!
	 * The decision of the first router to return a non-null
	 * value is honored above the others.
	 *
	 * @param router to add
	 * @return handle for unregistering
	 */
	HandlerRegistration routeOnTransition(StateRouter<T> router);

	HandlerRegistration routeOnTransition(T from, T to, StateRouter<T> router);

	HandlerRegistration routeBeforeEntering(T to, StateRouter<T> router);

	HandlerRegistration routeAfterExiting(T from, StateRouter<T> router);
}