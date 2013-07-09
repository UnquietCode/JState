package unquietcode.tools.esm;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Ben Fagin
 * @version 2013-04-02
 */
public class Routing_T {

	@Test
	public void testSimpleRedirect() {
		final AtomicInteger c1 = new AtomicInteger(0);
		final AtomicInteger c2 = new AtomicInteger(0);
		final AtomicInteger c3 = new AtomicInteger(0);

		EnumStateMachine<TestStates> esm = new EnumStateMachine<TestStates>(TestStates.One);
		esm.addAll(TestStates.class, true);

		esm.onEntering(TestStates.One, new StateMachineCallback() {
			public void performAction() {
				c1.incrementAndGet();
			}
		});

		esm.onEntering(TestStates.Two, new StateMachineCallback() {
			public void performAction() {
				c2.incrementAndGet();
			}
		});

		esm.onEntering(TestStates.Three, new StateMachineCallback() {
			public void performAction() {
				c3.incrementAndGet();
			}
		});

		esm.routeBeforeEntering(TestStates.Three, new StateRouter<TestStates>() {
			public TestStates route(TestStates current, TestStates next) {
				assertEquals(next, TestStates.Three);
				return TestStates.Two;
			}
		});

		esm.transition(TestStates.One);
		esm.transition(TestStates.Two);
		esm.transition(TestStates.Three);

		assertEquals(1, c1.get());
		assertEquals(2, c2.get());
		assertEquals(0, c3.get());
	}

	enum TestStates { One, Two, Three }
}
