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
				string = string.trim().toUpperCase().intern();
			}
			this.string = string;
		}

		public static StringState $(String string) {
			return new StringState(string);
		}

		public String name() {
			return string;
		}
	}
}
