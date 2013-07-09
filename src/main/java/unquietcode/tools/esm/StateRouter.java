package unquietcode.tools.esm;

/**
 * @author Ben Fagin
 * @version 2013-07-07
 */
public interface StateRouter<T> {

	/**
	 * Given a current state, and the state which has
	 * been requested, determine whether to allow the
	 * transition, stop it, or redirect it.
	 *
	 * To accomplish this, return the state which should
	 * be the next state. If no preference, return null.
	 *
	 * The first router on a {@link StateMachine} instance
	 * to return a non-null value 'wins'.
	 *
	 * @param current the current state
	 * @param next the state being requested
	 * @return the next state, or null if no preference
	 */
	T route(T current, T next);
}