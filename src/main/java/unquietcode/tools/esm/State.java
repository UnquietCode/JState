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
 * State interface. Each state in a system should have
 * a unique name.
 *
 * This interface can be applied to any enum class, since
 * they already support a name() method.
 *
 * @author Ben Fagin
 * @version 2013-07-07
 */
public interface State {
	String name();
}