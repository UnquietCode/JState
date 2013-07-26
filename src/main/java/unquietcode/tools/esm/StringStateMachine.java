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
 * @version 2013-07-08
 */
public class StringStateMachine extends WrappedStateMachine<StringStateMachine.StringState, String> {

	public StringStateMachine() {
		super();
	}

	public StringStateMachine(String initial) {
		super(initial);
	}

	@Override
	protected StringState wrap(String unwrapped) {
		return new StringState(unwrapped);
	}

	@Override
	protected String unwrap(StringState wrapped) {
		return wrapped.string;
	}

	static class StringState implements State {
		private final String string;

		public StringState(String string) {
			if (string != null) {
				string = string.trim().intern();
			}
			this.string = string;
		}

		public String name() {
			return string.toLowerCase().intern();
		}
	}
}
