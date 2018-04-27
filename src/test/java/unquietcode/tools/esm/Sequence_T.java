package unquietcode.tools.esm;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

/**
 * @author Ben Fagin
 * @version 2013-07-08
 */
public class Sequence_T {

	@Test
	public void testSequentialMatch() {
		GenericStateMachine<Color> sm = new GenericStateMachine<>(Color.Red);
		sm.addTransition(Color.Red, Color.Blue);
		sm.addTransition(Color.Blue, Color.Green);
		sm.addTransition(Color.Green, Color.Orange);
		sm.addTransition(Color.Orange, Color.Red);

		final List<Color> _pattern = Arrays.asList(Color.Blue, Color.Green, Color.Orange);
		final AtomicInteger counter = new AtomicInteger(0);

		sm.onSequence(_pattern, pattern -> {
			assertEquals(_pattern, pattern);
			counter.incrementAndGet();
		});

		sm.transition(Color.Blue);
		sm.transition(Color.Green);
		sm.transition(Color.Orange);
		sm.transition(Color.Red);

		assertEquals(1, counter.get());
	}

	public enum Color implements State {
		Red, Blue, Green, Orange
	}
}
