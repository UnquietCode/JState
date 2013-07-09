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
public class EnumStateMachine<T extends Enum<T>>
	implements ControllableStateMachine<T>, ProgrammableStateMachine<T>, RoutableStateMachine<T> {
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
			full.add(EnumWrapper.$(t));
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
	public void reset() {
		proxy.reset();
	}

	@Override
	public boolean transition(T state) {
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
	public HandlerRegistration onEntering(T state, StateMachineCallback callback) {
		return proxy.onEntering(EnumWrapper.$(state), callback);
	}

	@Override
	public HandlerRegistration onExiting(T state, StateMachineCallback callback) {
		return proxy.onExiting(EnumWrapper.$(state), callback);
	}

	@Override
	public HandlerRegistration onTransition(T from, T to, StateMachineCallback callback) {
		return proxy.onTransition(EnumWrapper.$(from), EnumWrapper.$(to), callback);
	}

	@Override
	public HandlerRegistration routeOnTransition(StateRouter<T> router) {
		return proxy.routeOnTransition(new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration routeOnTransition(T from, T to, StateRouter<T> router) {
		return proxy.routeOnTransition(EnumWrapper.$(from), EnumWrapper.$(to), new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration routeBeforeEntering(T to, StateRouter<T> router) {
		return proxy.routeBeforeEntering(EnumWrapper.$(to), new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration routeAfterExiting(T from, StateRouter<T> router) {
		return proxy.routeAfterExiting(EnumWrapper.$(from), new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration onSequence(List<T> pattern, SequenceHandler<T> handler) {
		List<EnumWrapper<T>> wrapped = new ArrayList<EnumWrapper<T>>();

		for (T unwrapped : pattern) {
			wrapped.add(EnumWrapper.$(unwrapped));
		}

		return proxy.onSequence(wrapped, new SequenceWrapper(handler));
	}

	@Override
	public HandlerRegistration onSequence(T[] pattern, SequenceHandler<T> handler) {
		return null;
	}

	@Override
	public boolean removeTransitions(T fromState, T...toStates) {
		return proxy.removeTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public void setTransitions(T fromState, T...toStates) {
		proxy.setTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public void setTransitions(StateMachineCallback callback, T fromState, T...toStates) {
		proxy.setTransitions(callback, EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public boolean addTransition(T fromState, T toState) {
		return proxy.addTransition(EnumWrapper.$(fromState), EnumWrapper.$(toState));
	}

	@Override
	public boolean addTransition(T fromState, T toState, StateMachineCallback callback) {
		return proxy.addTransition(EnumWrapper.$(fromState), EnumWrapper.$(toState), callback);
	}

	@Override
	public boolean addTransitions(StateMachineCallback callback, T fromState, T...toStates) {
		return proxy.addTransitions(callback, EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public boolean addTransitions(T fromState, T...toStates) {
		return addTransitions(null, fromState, toStates);
	}

	@Override
	public boolean addTransitions(T fromState, T[] toStates, StateMachineCallback callback) {
		return addTransitions(callback, fromState, toStates);
	}

	@Override
	public String toString() {
		return proxy.toString();
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

	private class RouterWrapper implements StateRouter<EnumWrapper<T>> {
		private final StateRouter<T> proxy;

		RouterWrapper(StateRouter<T> proxy) {
			this.proxy = proxy;
		}

		@Override
		public EnumWrapper<T> route(EnumWrapper<T> current, EnumWrapper<T> next) {
			T decision = proxy.route(current.value, next.value);
			return EnumWrapper.$(decision);
		}
	}

	private class SequenceWrapper implements SequenceHandler<EnumWrapper<T>> {
		private final SequenceHandler<T> handler;

		SequenceWrapper(SequenceHandler<T> handler) {
			this.handler = handler;
		}

		@Override
		public void onMatch(List<EnumWrapper<T>> pattern) {
			List<T> unwrapped = new ArrayList<T>();

			for (EnumWrapper<T> wrapped : pattern) {
				unwrapped.add(wrapped.value);
			}

			handler.onMatch(Collections.unmodifiableList(unwrapped));
		}
	}

	static class EnumWrapper<T extends Enum<T>> implements State {
		private static final Map<Enum<?>, EnumWrapper<?>> prewrapped = new WeakHashMap<Enum<?>, EnumWrapper<?>>();
		public final T value;

		@SuppressWarnings("unchecked")
		public static <T extends Enum<T>> EnumWrapper<T> $(T value) {
			EnumWrapper<T> wrapper;

			if (prewrapped.containsKey(value)) {
				wrapper = (EnumWrapper<T>) prewrapped.get(value);
			} else {
				wrapper = value != null
						? new EnumWrapper<T>(value)
						: null
				;
				prewrapped.put(value, wrapper);
			}

			return wrapper;
		}

		private EnumWrapper(T value) {
			this.value = value;
		}

		@Override
		public String name() {
			return value != null ? value.name() : "null";
		}

		@Override
		public boolean equals(Object obj) {
			EnumWrapper other = (EnumWrapper) obj;
			return value.name().equals(other.name());
		}
	}
}