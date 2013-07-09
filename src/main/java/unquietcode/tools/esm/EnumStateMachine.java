/*******************************************************************************
 Copyright 2013 Benjamin Fagin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


    Read the included LICENSE.TXT for more information.
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