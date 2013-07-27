/*******************************************************************************
 The MIT License (MIT)

 Copyright (c) 2013 Benjamin Fagin

 Permission is hereby granted, free of charge, to any person obtaining a copy of
 this software and associated documentation files (the "Software"), to deal in
 the Software without restriction, including without limitation the rights to
 use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 the Software, and to permit persons to whom the Software is furnished to do so,
 subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 ******************************************************************************/

package unquietcode.tools.esm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author  Benjamin Fagin
 * @version 07-06-2013
 */
public class StateMachineStringParser<V extends State, T extends FactoryStateMachine<V> & ProgrammableStateMachine<V>> {
	private final StringBuilder buffer;
	private final T stateMachine;

	private StateMachineStringParser(StringBuilder buffer, T stateMachine) {
		this.buffer = buffer;
		this.stateMachine = stateMachine;
	}

	/**
	 * Basic form is:
	 * "state1 : {transition1, transition2}, state2 : {transition3, transition4}, state3 : {}"
	 *
	 * Listing empty sets is optional. Can also add the initial state to the front like so:
	 * "initial | state1 : {transition1, transition2} | state2 : {transition3, transition4}"
	 *
	 * The states are the class strings of the enums being used. They will be processed via reflection.
	 * The existing information is preserved; create a new state machine instead to avoid that.
	 * When the operation is complete, the state machine is reset.
	 *
	 * @param   string   configuration string
	 * @throws  ParseException  if the configuration string is malformed
	 */

	public static <V extends State, T extends FactoryStateMachine<V> & ProgrammableStateMachine<V>>
	void configureStateMachine(Class<V> clazz, String string, T stateMachine) throws ParseException {
		configure(clazz, new StringBuilder(string), stateMachine);
	}

	public static <V extends State, T extends FactoryStateMachine<V> & ProgrammableStateMachine<V>>
	void configureStateMachine(Class<V> clazz, StringBuilder buffer, T stateMachine) throws ParseException {
		configure(clazz, new StringBuilder(buffer), stateMachine);
	}

	private static <V extends State, T extends FactoryStateMachine<V> & ProgrammableStateMachine<V>>
	void configure(Class<V> clazz, StringBuilder buffer, T stateMachine) throws ParseException {
		StateMachineStringParser<V, T> parser = new StateMachineStringParser<V, T>(buffer, stateMachine);
		stateMachine.setType(clazz);
		parser.build();
	}

	//---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---//

	@SuppressWarnings("unchecked")
	private T build() throws ParseException {
		eatWhiteSpace();

		String initialString = getString(Token.DIVIDER);
		if (initialString != null) {
			stateMachine.setInitialState(instantiate(initialString));
		}

		while (!isEmpty()) {
			String name = getString(Token.NAME_END);
			chomp(Token.SET_START);
			String elements[] = getStrings(Token.SET_END, Token.COMMA);
			chomp(Token.DIVIDER);

			if (name == null || elements == null) {
				throw new ParseException("Malformed configuration string.");
			}

			V from = instantiate(name);

			for (String _to : elements) {
				V to = instantiate(_to);
				stateMachine.addTransition(from, to);
			}
		}

		return stateMachine;
	}

	// TODO get rid of this with smarter parsing
	private boolean isEmpty() {
		// I hope the string is put on the stack for immediate collection.
		return buffer.toString().trim().length() == 0;
	}

	private void chomp(Token token) {
		match(token);
	}

	private String getString(Token token) {
		return match(token);
	}

	private Integer getInt(Token token) throws ParseException {
		String s = getString(token);
		if (s == null) { return null; }

		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ex) {
			throw new ParseException(ex);
		}
	}

	private Boolean getBoolean(Token token) throws ParseException {
		String s = getString(token);
		if (s == null) { return null; }

		try {
			return Boolean.parseBoolean(s);
		} catch (Exception ex) {
			throw new ParseException(ex);
		}
	}

	private Double getDouble(Token token) throws ParseException {
		String s = getString(token);
		if (s == null) { return null; }

		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException ex) {
			throw new ParseException(ex);
		}
	}

	private String[] getStrings(Token token, Token delimiter) {
		String list = match(token);
		if (list == null) {	return null; }
		if (list.isEmpty()) { return new String[]{}; }


		String cut[] = list.split(delimiter.value+"");
		for (int i=0; i < cut.length; ++i) {
			cut[i] = cut[i].trim();
		}

		return cut;
	}

	private Integer[] getInts(Token token, Token delimiter) throws ParseException {
		String[] list = getStrings(token, delimiter);
		if (list == null) {	return null; }

		List<Integer> retval = new ArrayList<Integer>();
		for (String s : list) {
			Integer i;
			try {
				i = Integer.parseInt(s);
			} catch (NumberFormatException ex) {
				throw new ParseException(ex);
			}

			retval.add(i);
		}

		return retval.toArray(new Integer[retval.size()]);
	}

	private Boolean[] getBooleans(Token token, Token delimiter) throws ParseException {
		String[] list = getStrings(token, delimiter);
		if (list == null) {	return null; }

		List<Boolean> retval = new ArrayList<Boolean>();
		for (String s : list) {
			Boolean b;
			try {
				b = Boolean.parseBoolean(s);
			} catch (Exception ex) {
				throw new ParseException(ex);
			}

			retval.add(b);
		}

		return retval.toArray(new Boolean[retval.size()]);
	}

	private Double[] getDoubles(Token token, Token delimiter) throws ParseException {
		String[] list = getStrings(token, delimiter);
		if (list == null) {	return null; }

		List<Double> retval = new ArrayList<Double>();
		for (String s : list) {
			Double d;
			try {
				d = Double.parseDouble(s);
			} catch (NumberFormatException ex) {
				throw new ParseException(ex);
			}

			retval.add(d);
		}

		return retval.toArray(new Double[retval.size()]);
	}

	private V instantiate(String string) throws ParseException {
		return stateMachine.getState(string);
	}

	private void eatWhiteSpace() {
		while (true) {
			char c = buffer.charAt(0);

			if (Character.isWhitespace(c)) {
				buffer.deleteCharAt(0);
			} else {
				return;
			}
		}
	}

	private String match(Token token) {
		int index = buffer.indexOf(token.value+"");
		if (index == -1) {
			return null;
		}

		String front = buffer.substring(0, index);
		buffer.delete(0, index+1);
		front = front.trim();

		return front;
	}

	public enum Token {
		SET_START('{'),
		SET_END('}'),
		DIVIDER('|'),
		NAME_END(':'),
		COMMA(',')

		;

		public final char value;

		Token(char value) {
			this.value = value;
		}
	}
}
