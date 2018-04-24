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

import unquietcode.tools.esm.routing.StateRouter;

import java.util.*;
import java.util.concurrent.Future;


public abstract class WrappedStateMachine<_Wrapper extends State, _Type> implements StateMachine<_Type> {
	private final GenericStateMachine<_Wrapper> proxy;
	private final Map<_Type, _Wrapper> wrapperCache = new HashMap<_Type, _Wrapper>();
	
	public WrappedStateMachine() {
		proxy = new GenericStateMachine<>();
	}

	public WrappedStateMachine(_Type initial) {
		proxy = new GenericStateMachine<>(wrap(initial));
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WrappedStateMachine)) {
			return false;
		}

		WrappedStateMachine other = (WrappedStateMachine) obj;
		return proxy.equals(other.proxy);
	}

	@Override
	public int hashCode() {
		return proxy.hashCode();
	}

	//==o==o==o==o==o==o==| helper methods |==o==o==o==o==o==o==//
	
	private _Type _unwrap(_Wrapper wrapped) {
		return wrapped == null ? null : unwrap(wrapped);
	}
	
	private _Wrapper _wrap(_Type unwrapped) {
		if (unwrapped == null) { return null; }
		_Wrapper wrapper;

		if (wrapperCache.containsKey(unwrapped)) {
			wrapper = wrapperCache.get(unwrapped);
		} else {
			wrapper = wrap(unwrapped);
			wrapperCache.put(unwrapped, wrapper);
		}

		return wrapper;
	}
	
	protected abstract _Type unwrap(_Wrapper wrapped);
	protected abstract _Wrapper wrap(_Type unwrapped);
	
	//==o==o==o==o==o==o==| interface methods |==o==o==o==o==o==o==//

	@Override
	public _Type currentState() {
		return _unwrap(proxy.currentState());
	}

	@Override
	public void reset() {
		proxy.reset();
	}

	@Override
	public boolean transition(_Type state) {
		return proxy.transition(_wrap(state));
	}

	@Override
	public Future<Boolean> transitionAsync(_Type state) throws TransitionException {
		return proxy.transitionAsync(_wrap(state));
	}

	@Override
	public long transitionCount() {
		return proxy.transitionCount();
	}

	@Override
	public _Type initialState() {
		return _unwrap(proxy.initialState());
	}

	@Override
	public void setInitialState(_Type state) {
		proxy.setInitialState(_wrap(state));
	}

	@Override
	public HandlerRegistration onEntering(StateHandler<_Type> callback) {
		return proxy.onEntering(new StateCallbackWrapper(callback));
	}

	@Override
	public HandlerRegistration onEntering(_Type state, StateHandler<_Type> callback) {
		return proxy.onEntering(_wrap(state), new StateCallbackWrapper(callback));
	}

	@Override
	public HandlerRegistration onExiting(StateHandler<_Type> callback) {
		return proxy.onExiting(new StateCallbackWrapper(callback));
	}

	@Override
	public HandlerRegistration onExiting(_Type state, StateHandler<_Type> callback) {
		return proxy.onExiting(_wrap(state), new StateCallbackWrapper(callback));
	}

	@Override
	public HandlerRegistration onTransition(TransitionHandler<_Type> callback) {
		return proxy.onTransition(new TransitionCallbackWrapper(callback));
	}

	@Override
	public HandlerRegistration onTransition(_Type from, _Type to, TransitionHandler<_Type> callback) {
		return proxy.onTransition(_wrap(from), _wrap(to), new TransitionCallbackWrapper(callback));
	}

	@Override
	public HandlerRegistration routeOnTransition(StateRouter<_Type> router) {
		return proxy.routeOnTransition(new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration routeOnTransition(_Type from, _Type to, StateRouter<_Type> router) {
		return proxy.routeOnTransition(_wrap(from), _wrap(to), new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration routeBeforeEntering(_Type to, StateRouter<_Type> router) {
		return proxy.routeBeforeEntering(_wrap(to), new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration routeAfterExiting(_Type from, StateRouter<_Type> router) {
		return proxy.routeAfterExiting(_wrap(from), new RouterWrapper(router));
	}

	@Override
	public HandlerRegistration onSequence(List<_Type> pattern, SequenceHandler<_Type> handler) {
		return proxy.onSequence(wrap(pattern), new SequenceWrapper(handler));
	}

	@Override
	public boolean removeTransitions(_Type fromState, _Type...toStates) {
		return proxy.removeTransitions(_wrap(fromState), wrap(toStates));
	}

	@Override
	public boolean removeTransitions(_Type fromState, List<_Type> toStates) {
		return proxy.removeTransitions(_wrap(fromState), wrap(toStates));
	}

	@Override
	public boolean addTransitions(TransitionHandler<_Type> callback, _Type fromState, _Type...toStates) {
		return proxy.addTransitions(_wrap(fromState), wrap(toStates), new TransitionCallbackWrapper(callback));
	}

	@Override
	public boolean addTransition(_Type fromState, _Type toState) {
		return proxy.addTransition(_wrap(fromState), _wrap(toState));
	}

	@Override
	public boolean addTransition(_Type fromState, _Type toState, TransitionHandler<_Type> callback) {
		return proxy.addTransition(_wrap(fromState), _wrap(toState), new TransitionCallbackWrapper(callback));
	}

	@Override
	public void addAllTransitions(List<_Type> states, boolean includeSelf) {
		proxy.addAllTransitions(wrap(states), includeSelf);
	}

	@Override
	public boolean addTransitions(_Type fromState, List<_Type> toStates) {
		return proxy.addTransitions(_wrap(fromState), wrap(toStates));
	}

	@Override
	public boolean addTransitions(_Type fromState, List<_Type> toStates, TransitionHandler<_Type> callback) {
		return proxy.addTransitions(_wrap(fromState), wrap(toStates), new TransitionCallbackWrapper(callback));
	}

	@Override
	public boolean addTransitions(_Type fromState, _Type...toStates) {
		return proxy.addTransitions(_wrap(fromState), wrap(toStates));
	}

	@Override
	public String toString() {
		return proxy.toString();
	}

	//---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---//

	private List<_Wrapper> wrap(_Type[] array) {
		return wrap(Arrays.asList(array));
	}

	private List<_Wrapper> wrap(List<_Type> list) {
		List<_Wrapper> wrapped = new ArrayList<_Wrapper>();

		for (_Type unwrapped : list) {
			wrapped.add(_wrap(unwrapped));
		}

		return wrapped;
	}

	private class RouterWrapper implements StateRouter<_Wrapper> {
		private final StateRouter<_Type> proxy;

		RouterWrapper(StateRouter<_Type> proxy) {
			this.proxy = proxy;
		}

		@Override
		public _Wrapper route(_Wrapper current, _Wrapper next) {
			_Type decision = proxy.route(_unwrap(current), _unwrap(next));
			return _wrap(decision);
		}

		@Override
		public int hashCode() {
			return proxy.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			RouterWrapper other = (RouterWrapper) obj;
			return proxy.equals(other.proxy);
		}
	}

	private class SequenceWrapper implements SequenceHandler<_Wrapper> {
		private final SequenceHandler<_Type> handler;

		SequenceWrapper(SequenceHandler<_Type> handler) {
			this.handler = handler;
		}

		@Override
		public void onMatch(List<_Wrapper> pattern) {
			List<_Type> unwrapped = new ArrayList<_Type>();

			for (_Wrapper wrapped : pattern) {
				unwrapped.add(_unwrap(wrapped));
			}

			handler.onMatch(Collections.unmodifiableList(unwrapped));
		}

		@Override
		public int hashCode() {
			return handler.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			SequenceWrapper other = (SequenceWrapper) obj;
			return handler.equals(other.handler);
		}
	}

	private class StateCallbackWrapper implements StateHandler<_Wrapper> {
		private final StateHandler<_Type> callback;

		StateCallbackWrapper(StateHandler<_Type> callback) {
			this.callback = callback;
		}

		@Override
		public void onState(_Wrapper state) {
			callback.onState(_unwrap(state));
		}

		@Override
		public int hashCode() {
			return callback.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			StateCallbackWrapper other = (StateCallbackWrapper) obj;
			return callback.equals(other.callback);
		}
	}

	private class TransitionCallbackWrapper implements TransitionHandler<_Wrapper> {
		private final TransitionHandler<_Type> callback;

		TransitionCallbackWrapper(TransitionHandler<_Type> callback) {
			this.callback = callback;
		}

		@Override
		public void onTransition(_Wrapper from, _Wrapper to) {
			callback.onTransition(_unwrap(from), _unwrap(to));
		}

		@Override
		public int hashCode() {
			return callback.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			TransitionCallbackWrapper other = (TransitionCallbackWrapper) obj;
			return callback.equals(other.callback);
		}
	}
}