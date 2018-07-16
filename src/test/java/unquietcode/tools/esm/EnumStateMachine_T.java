package unquietcode.tools.esm;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ben Fagin
 * @version 06-10-2012
 */
public class EnumStateMachine_T {

	enum State {
		Ready, Running, Paused, Stopping, Stopped, Finished
	}

	enum Something implements unquietcode.tools.esm.State {
		One, Two, Three
	}

	@Test
	public void deadSimpleTest() {
		EnumStateMachine<State> esm = getThreadLikeMachine();

		esm.addTransition(State.Ready, State.Paused);
		esm.transition(State.Paused);
	}

	@Test
	public void removeTransition() {
		EnumStateMachine<State> esm = getThreadLikeMachine();
		esm.transition(State.Running);
		esm.transition(State.Paused);

		boolean modified = esm.removeTransitions(State.Running, State.Paused);
		Assert.assertTrue("expected modification", modified);
		Assert.assertEquals("expected reset", 0, esm.transitionCount());

		esm.transition(State.Running);
		boolean failed = false;

		try {
			esm.transition(State.Paused);
		} catch (TransitionException ex) {
			failed = true;
		}

		Assert.assertTrue("expected an exception", failed);
	}

	@Test
	public void stringParsingFromExistingEnumMachine() throws ParseException {
		EnumStateMachine<Something> esm1 = new EnumStateMachine<>();

		// single
		esm1.addTransition(Something.One, Something.Two);

		// multiple
		esm1.addTransition(Something.Two, Something.One);
		esm1.addTransition(Something.Two, Something.Three);

		// looping
		esm1.addTransition(Something.Three, Something.Three);

		String stringRepresentation = esm1.toString();
		EnumStateMachine<Something> esm2 = new EnumStateMachine<>();
		StateMachineStringParser.configureStateMachine(Something.class, stringRepresentation, esm2);

		Assert.assertEquals(stringRepresentation, esm2.toString());
	}

	@Test
	public void stringParsingFromExistingStringMachine() throws ParseException {
		StringStateMachine esm1 = new StringStateMachine();

		// single
		esm1.addTransition("one", "two");

		// multiple
		esm1.addTransition("two", "one");
		esm1.addTransition("two", "Three");

		// looping
		esm1.addTransition("three", "three");

		String stringRepresentation = esm1.toString();
		StringStateMachine esm2 = new StringStateMachine();
		StateMachineStringParser.configureStateMachine(String.class, stringRepresentation, esm2);

		Assert.assertEquals(stringRepresentation, esm2.toString());
	}

	@Test
	public void entryAndExitCallbacks() {
		final AtomicInteger entered = new AtomicInteger(0);
		final AtomicInteger exited = new AtomicInteger(0);
		final AtomicInteger transitioned = new AtomicInteger(0);

		TransitionHandler<State> cb = (from, to) -> transitioned.incrementAndGet();

		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(cb, State.Ready, State.Running);
		esm.addTransitions(cb, State.Running, State.Stopped);

		esm.onEntering(State.Running, state -> entered.incrementAndGet());

		esm.onExiting(State.Running, state -> exited.incrementAndGet());

		esm.transition(State.Running);
		Assert.assertEquals("expected transitions", 1, transitioned.intValue());
		Assert.assertEquals("expected transitions", 1, esm.transitionCount());
		Assert.assertEquals("expected one entrance marker", 1, entered.intValue());

		esm.transition(State.Stopped);
		Assert.assertEquals("expected transitions", 2, transitioned.intValue());
		Assert.assertEquals("expected transitions", 2, esm.transitionCount());
		Assert.assertEquals("expected one entrance marker", 1, entered.intValue());
		Assert.assertEquals("expected one exit marker", 1, entered.intValue());
	}

	@Test
	public void transitionCallback() {
		final AtomicInteger counter = new AtomicInteger(0);

		TransitionHandler<State> cb = (from, to) -> counter.incrementAndGet();

		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(cb, State.Ready, State.Running);

		esm.transition(State.Running);
		Assert.assertEquals("expected one transition", 1, counter.intValue());
	}

	@Test
	public void duplicateTransitionCallback() {
		final AtomicInteger counter = new AtomicInteger(0);

		TransitionHandler<State> cb = (from, to) -> counter.incrementAndGet();

		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(cb, State.Ready, State.Running);
		esm.addTransitions(cb, State.Ready, State.Running);

		esm.transition(State.Running);
		Assert.assertEquals("expected one transition", 1, counter.intValue());
	}

	@Test
	public void multiUseTransitionCallback() {
		final AtomicInteger counter = new AtomicInteger(0);

		TransitionHandler<State> cb = (from, to) -> counter.incrementAndGet();

		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(cb, State.Ready, State.Running);
		esm.addTransitions(cb, State.Running, State.Stopped);

		esm.transition(State.Running);
		esm.transition(State.Stopped);

		Assert.assertEquals("expected one transition", 2, counter.intValue());
	}

	@Test(expected=TransitionException.class)
	public void transitionFailedToSelf() {
		EnumStateMachine<State> esm = getThreadLikeMachine();

		esm.transition(State.Running);
		esm.transition(State.Paused);
		esm.transition(State.Paused);   // exception!
	}

	@Test(expected=TransitionException.class)
	public void transitionFailedToOther() {
		EnumStateMachine<State> esm = getThreadLikeMachine();

		esm.transition(State.Running);
		esm.transition(State.Paused);
		esm.transition(State.Ready);   // exception!
	}

	@Test
	public void transitionAsync() {
		final EnumStateMachine<State> esm = getThreadLikeMachine();
		final AtomicInteger counter = new AtomicInteger();
		final int SLEEP_MS = 200;

		esm.onTransition((from, to) -> {
			try {
				Thread.sleep(SLEEP_MS);
			} catch (InterruptedException e) {
				Assert.fail(e.toString());
			}
			counter.incrementAndGet();
		});

		esm.transitionAsync(State.Running);
		Assert.assertEquals(0, counter.get());

		try {
			Thread.sleep(SLEEP_MS * 2);
		} catch (InterruptedException e) {
			Assert.fail();
		}
		Assert.assertEquals(1, counter.get());

		esm.transitionAsync(State.Paused);
		Assert.assertEquals(1, counter.get());

		try {
			Thread.sleep(SLEEP_MS * 2);
		} catch (InterruptedException e) {
			Assert.fail();
		}
		Assert.assertEquals(2, counter.get());
	}

	@Test
	public void transitionAsyncWithFailure() {
		final EnumStateMachine<State> esm = getThreadLikeMachine();
		final int SLEEP_MS = 200;

		esm.onTransition((from, to) -> {
			try {
				Thread.sleep(SLEEP_MS);
			} catch (InterruptedException e) {
				Assert.fail(e.toString());
			}
			throw new TransitionException("transition failed");
		});

		Future<Boolean> t1 = esm.transitionAsync(State.Running);
		Future<Boolean> t2 = esm.transitionAsync(State.Paused);

		try {
			Thread.sleep(SLEEP_MS * 2);
		} catch (InterruptedException e) {
			Assert.fail();
		}

		try {
			t1.get();
		} catch (InterruptedException e) {
			Assert.fail();
		} catch (ExecutionException e) {
			Assert.assertTrue(e.getCause() instanceof TransitionException);
		}

		Assert.assertEquals(State.Ready, esm.currentState());
		Assert.assertTrue(t2.isCancelled());
	}


	// ---------------------------------------------------------- //

	private EnumStateMachine<State> getThreadLikeMachine() {
		EnumStateMachine<State> esm = new EnumStateMachine<>(State.Ready);
		esm.addTransitions(State.Ready, State.Running, State.Finished);
		esm.addTransitions(State.Running, State.Paused, State.Stopping);
		esm.addTransitions(State.Paused, State.Running, State.Stopping);
		esm.addTransitions(State.Stopping, State.Stopped);
		esm.addTransitions(State.Stopped, State.Finished);
		esm.addTransitions(State.Finished, State.Ready, null);

		return esm;
	}
}
