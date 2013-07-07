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


import java.util.ArrayList;
import java.util.List;

/**
 * A state machine which runs on enums. Note that null is a valid state
 * in this system. The initial starting point is null by default.
 *
 * @author  Benjamin Fagin
 * @version 12-23-2010
 */
public class EnumStateMachine<T extends Enum<T>> extends StateMachine<EnumStateMachine.EnumWrapper<T>> {

	public EnumStateMachine() {
		this(null);
	}

	public EnumStateMachine(T initial) {
		super(new EnumWrapper<T>(initial));
	}

	static class EnumWrapper<T extends Enum<T>> implements State {
		public final T value;

		public static <T extends Enum<T>> EnumWrapper<T> $(T value) {
			return new EnumWrapper<T>(value);
		}

		EnumWrapper(T value) {
			this.value = value;
		}

		@Override
		public String name() {
			return value != null ? value.name() : "null";
		}

		@Override
		public boolean equals(Object obj) {
			EnumWrapper other = (EnumWrapper) obj;

			if (value == null) {
				return other == null;
			} else {
				return value.name().equals(other.name());
			}
		}
	}

	/**
	 * For every enum in the class, creates a transition between that enum and the others.
	 * If includeSelf is true, the enums are allowed to transition back to themselves.
	 *
	 * @param clazz         The class of the enum to add.
	 * @param includeSelf   True if enums are allowed to transition to themselves.
	 */
	@SuppressWarnings("unchecked")
	public synchronized void addAll(Class<T> clazz, boolean includeSelf) {
		if (clazz == null || !clazz.isEnum()) {
			throw new IllegalArgumentException("A valid enum class must be provided.");
		}

		// wrap enums
		List<EnumWrapper<T>> full = new ArrayList<EnumWrapper<T>>();

		for (T t : clazz.getEnumConstants()) {
			full.add(new EnumWrapper<T>(t));
		}

		for (EnumWrapper<T> wrapped : full) {
			List<EnumWrapper<T>> _toStates;

			if (includeSelf) {
				_toStates = full;
			} else {
				_toStates = new ArrayList<EnumWrapper<T>>(full);
				_toStates.remove(wrapped);
			}

			EnumWrapper[] toStates = _toStates.toArray(new EnumWrapper[full.size()]);
			addTransitions(wrapped, toStates);
		}
	}

	public synchronized boolean addTransition(T fromState, T toState) {
		return super.addTransition(EnumWrapper.$(fromState), EnumWrapper.$(toState));
	}

	public synchronized boolean addTransition(T fromState, T toState, StateMachineCallback callback) {
		return super.addTransition(EnumWrapper.$(fromState), EnumWrapper.$(toState), callback);
	}

	@SuppressWarnings("unchecked")
	public synchronized boolean addTransitions(StateMachineCallback callback, T fromState, T...toStates) {
		EnumWrapper[] wrapped = new EnumWrapper[toStates.length];

		for (int i = 0; i < toStates.length; i++) {
			wrapped[i] = EnumWrapper.$(toStates[i]);
		}

		return super.addTransitions(callback, EnumWrapper.$(fromState), wrapped);
	}

	public synchronized boolean addTransitions(T fromState, T...toStates) {
		return addTransitions(null, fromState, toStates);
	}

	public synchronized boolean addTransitions(T fromState, T[] toStates, StateMachineCallback callback) {
		return addTransitions(callback, fromState, toStates);
	}
}