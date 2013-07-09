package unquietcode.tools.esm;

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
