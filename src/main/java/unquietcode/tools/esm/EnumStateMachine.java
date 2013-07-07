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
public class EnumStateMachine<T extends Enum<T>> implements IStateMachine<T> {
	private final StateMachine<EnumWrapper<T>> proxy = new StateMachine<EnumWrapper<T>>();

	public EnumStateMachine() {
		this(null);
	}

	public EnumStateMachine(T initial) {
		proxy.setInitialState(EnumWrapper.$(initial));
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
			proxy.addTransitions(wrapped, toStates);
		}
	}

	//==o==o==o==o==o==o==| interface methods |==o==o==o==o==o==o==//

	@Override
	public T currentState() {
		return proxy.currentState().value;
	}

	@Override
	public synchronized void reset() {
		proxy.reset();
	}

	@Override
	public synchronized boolean transition(T state) {
		return proxy.transition(EnumWrapper.$(state));
	}

	@Override
	public long transitionCount() {
		return proxy.transitionCount();
	}

	@Override
	public T initialState() {
		return proxy.initialState().value;
	}

	@Override
	public synchronized void setInitialState(T state) {
		proxy.setInitialState(EnumWrapper.$(state));
	}

	@Override
	public CallbackRegistration onEntering(T state, StateMachineCallback callback) {
		return proxy.onEntering(EnumWrapper.$(state), callback);
	}

	@Override
	public CallbackRegistration onExiting(T state, StateMachineCallback callback) {
		return proxy.onExiting(EnumWrapper.$(state), callback);
	}

	@Override
	public CallbackRegistration onTransition(T from, T to, StateMachineCallback callback) {
		return proxy.onTransition(EnumWrapper.$(from), EnumWrapper.$(to), callback);
	}

	@Override
	public synchronized boolean removeTransitions(T fromState, T...toStates) {
		return proxy.removeTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public synchronized void setTransitions(T fromState, T...toStates) {
		proxy.setTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public synchronized void setTransitions(StateMachineCallback callback, T fromState, T...toStates) {
		proxy.setTransitions(callback, EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public synchronized boolean addTransition(T fromState, T toState) {
		return proxy.addTransition(EnumWrapper.$(fromState), EnumWrapper.$(toState));
	}

	@Override
	public synchronized boolean addTransition(T fromState, T toState, StateMachineCallback callback) {
		return proxy.addTransition(EnumWrapper.$(fromState), EnumWrapper.$(toState), callback);
	}

	@Override
	public synchronized boolean addTransitions(StateMachineCallback callback, T fromState, T...toStates) {
		return proxy.addTransitions(callback, EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public synchronized boolean addTransitions(T fromState, T...toStates) {
		return addTransitions(null, fromState, toStates);
	}

	@Override
	public synchronized boolean addTransitions(T fromState, T[] toStates, StateMachineCallback callback) {
		return addTransitions(callback, fromState, toStates);
	}

	//---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---//

	@SuppressWarnings("unchecked")
	private EnumWrapper<T>[] wrap(T[] array) {
		EnumWrapper[] wrapped = new EnumWrapper[array.length];

		for (int i = 0; i < array.length; i++) {
			wrapped[i] = EnumWrapper.$(array[i]);
		}

		return wrapped;
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
}