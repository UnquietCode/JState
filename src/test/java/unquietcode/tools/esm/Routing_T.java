package unquietcode.tools.esm;

import org.junit.Assert;
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

	@Test(expected=TransitionException.class)
	public void testSyncWithinAsync() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<>(null);
		esm.addAll(TestStates.class, true, true);

		esm.onEntering(TestStates.Two, (state) -> {
			esm.transition(TestStates.Three);
		});

		esm.transition(TestStates.One);
		esm.transition(TestStates.Two);
		Assert.fail();
	}

	@Test
	public void testAsyncWithinAsync() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<>(null);
		esm.addAll(TestStates.class, true, true);

		final AtomicInteger c1 = new AtomicInteger(0);
		final AtomicInteger c2 = new AtomicInteger(0);
		final AtomicInteger c3 = new AtomicInteger(0);

		esm.onEntering(TestStates.One, state -> c1.incrementAndGet());

		esm.onEntering(TestStates.Two, (state) -> {
			c2.incrementAndGet();
			esm.transitionAsync(TestStates.Three);
		});

		esm.onEntering(TestStates.Three, state -> c3.incrementAndGet());

		esm.transition(TestStates.One);
		esm.transition(TestStates.Two);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		assertEquals(1, c1.get());
		assertEquals(1, c2.get());
		assertEquals(1, c3.get());
	}

	@Test
	public void testSimpleRedirect() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<>(TestStates.One);
		esm.addAll(TestStates.class, true);

		final AtomicInteger c1 = new AtomicInteger(0);
		final AtomicInteger c2 = new AtomicInteger(0);
		final AtomicInteger c3 = new AtomicInteger(0);

		esm.onEntering(TestStates.One, state -> c1.incrementAndGet());
		esm.onEntering(TestStates.Two, state -> c2.incrementAndGet());
		esm.onEntering(TestStates.Three, state -> c3.incrementAndGet());

		esm.routeBeforeEntering(TestStates.Three, (current, next) -> {
			assertEquals(next, TestStates.Three);
			return TestStates.Two;
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
		EnumStateMachine<TestStates> esm = new EnumStateMachine<>(TestStates.One);
		esm.addAll(TestStates.class, true);

		final AtomicInteger c1 = new AtomicInteger(0);
		final AtomicInteger c2 = new AtomicInteger(0);
		final AtomicInteger c3 = new AtomicInteger(0);

		esm.onEntering(TestStates.One, state -> c1.incrementAndGet());
		esm.onEntering(TestStates.Two, state -> c2.incrementAndGet());
		esm.onEntering(TestStates.Three, state -> c3.incrementAndGet());

		RoundRobinStateRouter<TestStates> router = new RoundRobinStateRouter<>(TestStates.One, TestStates.Two, TestStates.Three);
		esm.routeBeforeEntering(TestStates.Three, router);

		for (int i=0; i < 12; ++i) {
			esm.transition(TestStates.Three);
		}

		assertEquals(4, c1.get());
		assertEquals(4, c2.get());
		assertEquals(4, c3.get());
	}

	@Test
	public void testRandomRedirect() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<>(TestStates.One);
		esm.addAll(TestStates.class, true);

		final AtomicInteger c1 = new AtomicInteger(0);
		final AtomicInteger c2 = new AtomicInteger(0);
		final AtomicInteger c3 = new AtomicInteger(0);

		esm.onEntering(TestStates.One, state -> c1.incrementAndGet());
		esm.onEntering(TestStates.Two, state -> c2.incrementAndGet());
		esm.onEntering(TestStates.Three, state -> c3.incrementAndGet());

		RandomStateRouter<TestStates> router = new RandomStateRouter<>(TestStates.One, TestStates.Two, TestStates.Three);
		esm.routeBeforeEntering(TestStates.Three, router);

		final int sampleCount = 1000;

		// perform a bunch of transitions
		for (int i=0; i < sampleCount; ++i) {
			esm.transition(TestStates.Three);
		}

		// the sum should be the sample size
		assertEquals(sampleCount, c1.get() + c2.get() + c3.get());

		// the difference should less than 10%
		final int epsilon = (int) (0.1 * sampleCount);
		assertTrue(Math.abs(c1.get()-c2.get()) < epsilon);
		assertTrue(Math.abs(c2.get()-c3.get()) < epsilon);
		assertTrue(Math.abs(c3.get()-c1.get()) < epsilon);
	}

	@Test(expected=TransitionException.class)
	public void testInvalidRoutingResult() {
		EnumStateMachine<TestStates> esm = new EnumStateMachine<TestStates>();
		esm.addTransition(null, TestStates.One);
		esm.addTransition(TestStates.One, TestStates.Two);

		// this should cause a failure, One -> Three not valid
		esm.routeAfterExiting(TestStates.One, (current, next) -> TestStates.Three);

		esm.transition(TestStates.One);
		esm.transition(TestStates.Two);
	}

	enum TestStates { One, Two, Three }
}