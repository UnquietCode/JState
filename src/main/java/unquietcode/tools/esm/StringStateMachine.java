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
