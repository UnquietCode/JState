package unquietcode.tools.esm;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Ben Fagin
 * @version 2013-07-15
 */
public class Reflective_T {

	@Test
	public void testSimpleReflective() {
		final AtomicInteger enteringBlue = new AtomicInteger();
		final AtomicInteger exitingBlue = new AtomicInteger();
		final AtomicInteger enteringGreen = new AtomicInteger();
		final AtomicInteger enteringAny = new AtomicInteger();
		final AtomicInteger exitingAny = new AtomicInteger();
		final AtomicInteger transitionAny = new AtomicInteger();

		ReflectiveStateMachine sm = new ReflectiveStateMachine() {

			@Override
			protected void declareTransitions() {
				addTransition(null, "blue");
				addTransition("blue", "green");
				addTransition("green", null);
			}

			public void onEnteringBlue(String state) {
				assertEquals("blue", state);
				enteringBlue.incrementAndGet();
			}

			public void onBlue() {
				enteringBlue.incrementAndGet();
			}

			public void onExitingBlue() {
				exitingBlue.incrementAndGet();
			}

			public void onGreen() {
				enteringGreen.incrementAndGet();
			}

			public void onEntering() {
				enteringAny.incrementAndGet();
			}

			public void onExiting(String state) {
				exitingAny.incrementAndGet();
			}

			public void onTransition(String from, String to) {
				transitionAny.incrementAndGet();
			}
		};

		sm.transition("blue");
		sm.transition("green");
		sm.transition(null);

		assertEquals(2, enteringBlue.get());
		assertEquals(1, exitingBlue.get());
		assertEquals(1, enteringGreen.get());
		assertEquals(3, enteringAny.get());
		assertEquals(3, exitingAny.get());
		assertEquals(3, transitionAny.get());
	}

	@Test(expected=UnsupportedOperationException.class)
	public void testException() {
		ReflectiveStateMachine sm = new ReflectiveStateMachine() {
			public void onBlue() {
				throw new UnsupportedOperationException("error");
			}
		};

		sm.addTransition(null, "blue");
		sm.transition("blue");
	}
}
