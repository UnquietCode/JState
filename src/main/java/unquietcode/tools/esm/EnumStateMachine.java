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


import java.util.*;

/**
 * A state machine which runs on enums. Note that null is a valid state
 * in this system. The initial starting point is null by default.
 *
 * @author  Benjamin Fagin
 * @version 12-23-2010
 */
public class EnumStateMachine<T extends Enum<T>> extends WrappedStateMachine<EnumStateMachine.EnumWrapper<T>, T> {

	public EnumStateMachine() {
		super();
	}

	public EnumStateMachine(T initial) {
		super(initial);
	}

	public void addAll(Class<T> clazz, boolean includeSelf) {
		if (clazz == null || !clazz.isEnum()) {
			throw new IllegalArgumentException("A valid enum class must be provided.");
		}

		addAllTransitions(Arrays.asList(clazz.getEnumConstants()), includeSelf);
	}

	//---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---//

	@Override
	protected EnumWrapper<T> wrap(T unwrapped) {
		return new EnumWrapper<T>(unwrapped);
	}

	@Override
	protected T unwrap(EnumWrapper<T> wrapped) {
		return wrapped.value;
	}

	static class EnumWrapper<T extends Enum<T>> implements State {
		public final T value;

		private EnumWrapper(T value) {
			this.value = value;
		}

		@Override
		public String name() {
			return value != null ? value.name() : "null";
		}
	}
}