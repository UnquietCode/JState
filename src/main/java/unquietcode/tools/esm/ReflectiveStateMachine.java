package unquietcode.tools.esm;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ben Fagin
 * @version 2013-07-15
 */
public abstract class ReflectiveStateMachine extends StringStateMachine {

	protected ReflectiveStateMachine(String initial) {
		super(initial);
		init();
	}

	protected ReflectiveStateMachine() {
		super();
		init();
	}

	/**
	 * A method which can be used to declare transitions inline at the
	 * time that the class is created, which supports anonymous instances.
	 */
	protected void declareTransitions() {
		// children can override to provide functionality
	}

	private void init() {
		discover();
		declareTransitions();
	}

	private void discover() {
		// build a map, ensure no duplicate names
		Map<String, Method> methodMap = new HashMap<String, Method>();

		for (Method method : this.getClass().getDeclaredMethods()) {
			String name = method.getName();

			if (methodMap.containsKey(name)) {
				throw new RuntimeException("Found two methods with name '"+name+"'.");
			}

			methodMap.put(name, method);
		}

		// iterate the map, adding callbacks to parent
		for (Map.Entry<String, Method> entry : methodMap.entrySet()) {
			Method method = entry.getValue();
			String name = method.getName().toLowerCase();

			if ("onentering".equals(name)) {
				super.onEntering(createOnStateInvoker(method));
			}

			else if ("onexiting".equals(name)) {
				super.onExiting(createOnStateInvoker(method));
			}

			else if ("ontransition".equals(name)) {
				super.onTransition(createOnTransitionInvoker(method));
			}

			else if (name.startsWith("onentering")) {
				String state = name.substring("onentering".length());
				super.onEntering(state, createOnStateInvoker(method));
			}

			else if (name.startsWith("onexiting")) {
				String state = name.substring("onexiting".length());
				super.onExiting(state, createOnStateInvoker(method));
			}

			else if (name.startsWith("on")) {
				String state = name.substring("on".length());
				super.onEntering(state, createOnStateInvoker(method));
			}
		}
	}

	private RuntimeException convertException(Exception ex) {
		Throwable t;

		if (ex instanceof InvocationTargetException) {
			t = ((InvocationTargetException) ex).getTargetException();
		} else {
			t = ex;
		}

		if (t instanceof RuntimeException) {
			return (RuntimeException) t;
		} else {
			return new RuntimeException(t);
		}
	}

	private StateHandler<String> createOnStateInvoker(final Method m) {
		return new StateHandler<String>() {
			public void onState(String state) {
				int arguments = m.getParameterTypes().length;

				try {
					if (arguments == 0) {
						m.invoke(ReflectiveStateMachine.this);
					} else if (arguments == 1) {
						m.invoke(ReflectiveStateMachine.this, state);
					} else {
						throw new IllegalStateException("expected 0 or 1 arguments");
					}
				} catch (Exception ex) {
					throw convertException(ex);
				}
			}
		};
	}

	private TransitionHandler<String> createOnTransitionInvoker(final Method m) {
		return new TransitionHandler<String>() {
			public void onTransition(String from, String to) {
				int arguments = m.getParameterTypes().length;

				try {
					if (arguments == 0) {
						m.invoke(ReflectiveStateMachine.this);
					} else if (arguments == 1) {
						m.invoke(ReflectiveStateMachine.this, from);
					} else if (arguments == 2) {
						m.invoke(ReflectiveStateMachine.this, from, to);
					} else {
						throw new IllegalStateException("expected 0, 1, or 2 arguments");
					}
				} catch (Exception ex) {
					throw convertException(ex);
				}
			}
		};
	}
}
