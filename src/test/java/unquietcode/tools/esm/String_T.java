package unquietcode.tools.esm;

import org.junit.Test;

/**
 * @author Ben Fagin
 * @version 2013-07-08
 */
public class String_T {

	@Test
	public void testStringState() {
		StateMachine<String> sm = new StringStateMachine();
		sm.addTransition(null, "hello");
		sm.addTransition("hello", "world", new StateMachineCallback() {
			public void performAction() {
				System.out.println("!!!");
			}
		});
		sm.addTransition("world", "goodbye ");
		sm.addTransition("GOODBYE", null);


		sm.transition("hello");
		System.out.println(sm.currentState());

		sm.transition("world");
		System.out.println(sm.currentState());

	}
}
