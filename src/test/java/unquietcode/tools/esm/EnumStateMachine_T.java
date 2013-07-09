package unquietcode.tools.esm;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ben Fagin
 * @version 06-10-2012
 */
public class EnumStateMachine_T {

	enum State {
		Ready, Running, Paused, Stopping, Stopped, Finished
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

//	@Test
//	public void stringParsingFromScratch() {
		// TODO
//	}

	@Test
	@Ignore
	public void stringParsingFromExistingMachine() throws ParseException {
		EnumStateMachine<State> esm1 = getThreadLikeMachine();
		String one = esm1.toString();
		EnumStateMachine<State> esm2 = EnumStringParser.getStateMachine(one);

		Assert.assertTrue(esm1.equals(esm2));
	}

	@Test
	public void entryAndExitCallbacks() {
		final AtomicInteger entered = new AtomicInteger(0);
		final AtomicInteger exited = new AtomicInteger(0);
		final AtomicInteger transitioned = new AtomicInteger(0);

		StateMachineCallback cb = new StateMachineCallback() {
			public void performAction() {
				transitioned.incrementAndGet();
			}
		};

		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(cb, State.Ready, State.Running);
		esm.addTransitions(cb, State.Running, State.Stopped);

		esm.onEntering(State.Running, new StateMachineCallback() {
			public void performAction() {
				entered.incrementAndGet();
			}
		});

		esm.onExiting(State.Running, new StateMachineCallback() {
			public void performAction() {
				exited.incrementAndGet();
			}
		});

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

		StateMachineCallback cb = new StateMachineCallback() {
			public void performAction() {
				counter.incrementAndGet();
			}
		};

		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(cb, State.Ready, State.Running);

		esm.transition(State.Running);
		Assert.assertEquals("expected one transition", 1, counter.intValue());
	}

	@Test
	public void duplicateTransitionCallback() {
		final AtomicInteger counter = new AtomicInteger(0);

		StateMachineCallback cb = new StateMachineCallback() {
			public void performAction() {
				counter.incrementAndGet();
			}
		};

		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(cb, State.Ready, State.Running);
		esm.addTransitions(cb, State.Ready, State.Running);

		esm.transition(State.Running);
		Assert.assertEquals("expected one transition", 1, counter.intValue());
	}

	@Test
	public void multiUseTransitionCallback() {
		final AtomicInteger counter = new AtomicInteger(0);

		StateMachineCallback cb = new StateMachineCallback() {
			public void performAction() {
				counter.incrementAndGet();
			}
		};

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

	// ---------- //

	private EnumStateMachine<State> getThreadLikeMachine() {
		EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
		esm.addTransitions(State.Ready, State.Running, State.Finished);
		esm.addTransitions(State.Running, State.Paused, State.Stopping);
		esm.addTransitions(State.Paused, State.Running, State.Stopping);
		esm.addTransitions(State.Stopping, State.Stopped);
		esm.addTransitions(State.Stopped, State.Finished);
		esm.addTransitions(State.Finished, State.Ready, null);

		return esm;
	}
}
