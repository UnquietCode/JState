package unquietcode.tools.esm;

import org.junit.Test;
import unquietcode.tools.esm.routing.RandomStateRouter;
import unquietcode.tools.esm.routing.RoundRobinStateRouter;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

		EnumStateMachine<TestStates> esm = new EnumStateMachine<>(TestStates.One);
		esm.addAll(TestStates.class, true);

		esm.onEntering(TestStates.One, state -> c1.incrementAndGet());

		esm.onEntering(TestStates.Two, state -> c2.incrementAndGet());

		esm.onEntering(TestStates.Three, state -> c3.incrementAndGet());

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

	@Test
	public void testRoundRobinRedirect() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<TestStates>(TestStates.One);
		esm.addAll(TestStates.class, true);

		final AtomicInteger c3 = new AtomicInteger(0);
		esm.onEntering(TestStates.Three, state -> c3.incrementAndGet());

		final AtomicInteger r1 = new AtomicInteger(0);
		final AtomicInteger r2 = new AtomicInteger(0);
		final AtomicInteger r3 = new AtomicInteger(0);

		StateRouter<TestStates> router1 = (current, next) -> {
			r1.incrementAndGet();
			return next;
		};
		StateRouter<TestStates> router2 = (current, next) -> {
			r2.incrementAndGet();
			return next;
		};
		StateRouter<TestStates> router3 = (current, next) -> {
			r3.incrementAndGet();
			return next;
		};

		esm.routeBeforeEntering(TestStates.Three, new RoundRobinStateRouter<>(router1, router2, router3));

		for (int i=0; i < 12; ++i) {
			esm.transition(TestStates.Three);
		}

		assertEquals(12, c3.get());
		assertEquals(4, r1.get());
		assertEquals(4, r2.get());
		assertEquals(4, r3.get());
	}

	@Test
	public void testRandomRedirect() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<>(TestStates.One);
		esm.addAll(TestStates.class, true);

		final AtomicInteger c3 = new AtomicInteger(0);
		esm.onEntering(TestStates.Three, state -> c3.incrementAndGet());

		final AtomicInteger r1 = new AtomicInteger(0);
		final AtomicInteger r2 = new AtomicInteger(0);
		final AtomicInteger r3 = new AtomicInteger(0);

		StateRouter<TestStates> router1 = (current, next) -> {
			r1.incrementAndGet();
			return next;
		};
		StateRouter<TestStates> router2 = (current, next) -> {
			r2.incrementAndGet();
			return next;
		};
		StateRouter<TestStates> router3 = (current, next) -> {
			r3.incrementAndGet();
			return next;
		};

		esm.routeBeforeEntering(TestStates.Three, new RandomStateRouter<>(router1, router2, router3));

		final int sampleCount = 1000;

		// perform a bunch of transitions
		for (int i=0; i < sampleCount; ++i) {
			esm.transition(TestStates.Three);
		}

		// the sum should be the sample size
		assertEquals(sampleCount, c3.get());
		assertEquals(sampleCount, r1.get() + r2.get() + r3.get());

		// the difference should less than 10%
		final int epsilon = (int) (0.1 * sampleCount);
		assertTrue(Math.abs(r1.get()-r2.get()) < epsilon);
		assertTrue(Math.abs(r2.get()-r3.get()) < epsilon);
		assertTrue(Math.abs(r3.get()-r1.get()) < epsilon);
	}

	@Test(expected=TransitionException.class)
	public void testInvalidRoutingResult() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<TestStates>();
		esm.addTransition(null, TestStates.One);
		esm.addTransition(TestStates.One, TestStates.Two);

		// this should cause a failure, One -> Three not valid
		esm.routeAfterExiting(TestStates.One, new StateRouter<TestStates>() {
			public TestStates route(TestStates current, TestStates next) {
				return TestStates.Three;
			}
		});

		esm.transition(TestStates.One);
		esm.transition(TestStates.Two);
	}

	enum TestStates { One, Two, Three }
}
