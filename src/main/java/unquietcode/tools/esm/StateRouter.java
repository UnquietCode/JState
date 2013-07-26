/*******************************************************************************
 Enum State Machine - a Java state machine library

 Written in 2013 by Benjamin Fagin (blouis@unquietcode.com).


 To the extent possible under law, the author(s) have dedicated all copyright
 and related and neighboring rights to this software to the public domain
 worldwide. This software is distributed without any warranty.

 You should have received a copy of the CC0 Public Domain Dedication along with
 this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

 Though not enforced, please consider providing attribution for the original
 authors in your projects which make use of, or derive from, this software.
 ******************************************************************************/

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