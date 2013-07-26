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