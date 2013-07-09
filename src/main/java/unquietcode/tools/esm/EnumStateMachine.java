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
	private final StateMachine<EnumWrapper<T>> proxy;

	public EnumStateMachine() {
		proxy = new StateMachine<EnumWrapper<T>>();
	}

	public EnumStateMachine(T initial) {
		proxy = new StateMachine<EnumWrapper<T>>(EnumWrapper.$(initial));
	}

	@SuppressWarnings("unchecked")
	public void addAll(Class<T> clazz, boolean includeSelf) {
		if (clazz == null || !clazz.isEnum()) {
			throw new IllegalArgumentException("A valid enum class must be provided.");
		}

		addAllTransitions(Arrays.asList(clazz.getEnumConstants()), includeSelf);
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
		return proxy.onSequence(wrap(pattern), new SequenceWrapper(handler));
	}

	@Override
	public boolean removeTransitions(T fromState, T...toStates) {
		return proxy.removeTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public boolean removeTransitions(T fromState, List<T> toStates) {
		return proxy.removeTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public boolean addTransitions(StateMachineCallback callback, T fromState, T...toStates) {
		return proxy.addTransitions(EnumWrapper.$(fromState), wrap(toStates), callback);
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
	public void addAllTransitions(List<T> states, boolean includeSelf) {
		proxy.addAllTransitions(wrap(states), includeSelf);
	}

	@Override
	public boolean addTransitions(T fromState, List<T> toStates) {
		return proxy.addTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public boolean addTransitions(T fromState, List<T> toStates, StateMachineCallback callback) {
		return proxy.addTransitions(EnumWrapper.$(fromState), wrap(toStates), callback);
	}

	@Override
	public boolean addTransitions(T fromState, T...toStates) {
		return proxy.addTransitions(EnumWrapper.$(fromState), wrap(toStates));
	}

	@Override
	public String toString() {
		return proxy.toString();
	}

	//---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---//

	@SuppressWarnings("unchecked")
	private List<EnumWrapper<T>> wrap(T[] array) {
		return wrap(Arrays.asList(array));
	}

	private List<EnumWrapper<T>> wrap(List<T> list) {
		List<EnumWrapper<T>> wrapped = new ArrayList<EnumWrapper<T>>();

		for (T unwrapped : list) {
			wrapped.add(EnumWrapper.$(unwrapped));
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