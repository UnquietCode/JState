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


import java.util.Arrays;

/**
 * A state machine which runs on enums. Note that null is a valid state
 * in this system. The initial starting point is null by default.
 *
 * @author  Benjamin Fagin
 * @version 12-23-2010
 */
public class EnumStateMachine<T extends Enum<T>> extends WrappedStateMachine<EnumStateMachine.EnumWrapper<T>, T> implements FactoryStateMachine<T> {
	private Class<T> genericType;

	public EnumStateMachine() {
		super();
	}

	public EnumStateMachine(T initial) {
		super(initial);

		// try to help by setting the type
		if (initial != null) {
			setType(initial.getDeclaringClass());
		}
	}

	/**
	 * Add transitions between every enum in the class.
	 * If includeSelf is set, then every enum state will
	 * also have a transition added which returns to itself.
	 *
	 * @param clazz this enum class
	 * @param includeSelf if true, will also create loops
	 */
	public void addAll(Class<T> clazz, boolean includeSelf) {
		setType(clazz);
		addAllTransitions(Arrays.asList(clazz.getEnumConstants()), includeSelf);
	}

	/**
	 * Set the class of the enum used for this state machine.
	 * The class is removed from the constructor as a convenience.
	 * This method MUST be called before an EnumStateMachine can be
	 * used to resolve named states, via {@link #getState(String)}.
	 *
	 * @param clazz the enum class for this state machine
	 */
	@Override
	public void setType(Class<T> clazz) {
		if (clazz == null || !clazz.isEnum()) {
			throw new IllegalArgumentException("A valid enum class must be provided.");
		}
		this.genericType = clazz;
	}

	//---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---//

	@Override
	protected EnumWrapper<T> wrap(T unwrapped) {
		return new EnumWrapper<>(unwrapped);
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

	@Override
	public T getState(String name) {
		if (genericType == null) {
			throw new IllegalStateException("setType must be called before names can be resolved");
		}

		if (name == null) {
			throw new IllegalArgumentException("name cannot be null");
		}

		if (name.equals("null")) {
			return null;
		}

		for (T t : genericType.getEnumConstants()) {
			String enumName = t.name();

			if (enumName.equals(name)) {
				return t;
			}
		}

		throw new RuntimeException("unknown name '"+name+"'");
	}
}