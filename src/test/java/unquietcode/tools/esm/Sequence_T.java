package unquietcode.tools.esm;

import org.junit.Test;
import unquietcode.tools.esm.sequences.Pattern;
import unquietcode.tools.esm.sequences.PatternBuilder;

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

	@Test
	public void testSequentialMatchWithWildcard() {
		EnumStateMachine<Color> sm = new EnumStateMachine<>();
		sm.addAll(Color.class, true, true);

		final Pattern<Color> _pattern = PatternBuilder.<Color>create()
			.add(Color.Red, Color.Blue)
			.addWildcard()
			.add(Color.Green)
		.build();

		final AtomicInteger counter = new AtomicInteger(0);

		sm.onSequence(_pattern, pattern -> {
			counter.incrementAndGet();
		});

		sm.transition(Color.Red);
		sm.transition(Color.Blue);
		sm.transition(Color.Orange);
		sm.transition(Color.Green);

		assertEquals(1, counter.get());
	}

	@Test
	public void test_sequential_match_on_initial_state() {
		EnumStateMachine<Color> sm = new EnumStateMachine<>(Color.Red);
		sm.addTransition(Color.Red, Color.Green);
		sm.addTransition(Color.Green, Color.Orange);
		sm.addTransition(Color.Orange, Color.Red);

		final List<Color> _pattern = Arrays.asList(Color.Red, Color.Green, Color.Orange);
		final AtomicInteger counter = new AtomicInteger(0);

		sm.onSequence(_pattern, pattern -> {
			assertEquals(_pattern, pattern);
			counter.incrementAndGet();
		});

		sm.transition(Color.Green);
		sm.transition(Color.Orange);

		assertEquals(1, counter.get());
	}

	@Test
	public void test_sequential_match_with_null_initial_state() {
		EnumStateMachine<Color> sm = new EnumStateMachine<>(null);
		sm.addTransition(null, Color.Green);
		sm.addTransition(Color.Green, Color.Orange);
		sm.addTransition(Color.Orange, Color.Red);

		final List<Color> _pattern = Arrays.asList(null, Color.Green, Color.Orange);
		final AtomicInteger counter = new AtomicInteger(0);

		sm.onSequence(_pattern, pattern -> {
			assertEquals(_pattern, pattern);
			counter.incrementAndGet();
		});

		sm.transition(Color.Green);
		sm.transition(Color.Orange);

		assertEquals(1, counter.get());
	}

	@Test
	public void test_sequential_match_with_differing_list_sizes() {
		EnumStateMachine<ZState> esm = new EnumStateMachine<>();
		esm.addAll(ZState.class, true, true);

		List<ZState> threeStatePattern = Arrays.asList(ZState.One, ZState.Two, ZState.Three);
		List<ZState> fourStatePattern = Arrays.asList(ZState.One, ZState.Two, ZState.Three, ZState.Four);

		final AtomicInteger counter1 = new AtomicInteger(0);
		final AtomicInteger counter2 = new AtomicInteger(0);

		esm.onSequence(threeStatePattern, pattern -> counter1.incrementAndGet());
		esm.onSequence(fourStatePattern, pattern -> counter2.incrementAndGet());

		esm.transition(ZState.One);
		esm.transition(ZState.Two);
		assertEquals(0, counter1.get());
		assertEquals(0, counter2.get());

		esm.transition(ZState.Three);
		assertEquals(1, counter1.get());
		assertEquals(0, counter2.get());

		esm.transition(ZState.Four);
		assertEquals(1, counter1.get());
		assertEquals(1, counter2.get());
	}

	public enum Color implements State {
		Red, Blue, Green, Orange
	}

	public enum ZState implements State {
		One, Two, Three, Four, Five
	}
}
