/*******************************************************************************
 Enum State Machine - a Java state machine library

 Written in 2013 by Benjamin Fagin (blouis@unquietcode.com).


 To the extent possible under law, the author(s) have dedicated all copyright
 and related and neighboring rights to this software to the public domain
 worldwide. This software is distributed without any warranty.

 You should have received a copy of the CC0 Public Domain Dedication along with
 this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

 Though not enforced, please consider providing attribution for the original
 authors in your projects which make use of, or derive from, this software.
 ******************************************************************************/

package unquietcode.tools.esm;

import java.util.ArrayList;
import java.util.List;

/**
 * @author  Benjamin Fagin
 * @version 07-06-2013
 */
public class EnumStringParser {
	private final StringBuffer buffer;

	private EnumStringParser(StringBuffer buffer) {
		this.buffer = buffer;
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
	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> EnumStateMachine<T> getStateMachine(String string) throws ParseException {
		EnumStringParser parser = new EnumStringParser(new StringBuffer(string));
		return parser.build();
	}

	@SuppressWarnings("unchecked")
	public static <T extends Enum<T>> EnumStateMachine<T> getStateMachine(StringBuffer buffer) throws ParseException {
		EnumStringParser parser = new EnumStringParser(new StringBuffer(buffer));
		return parser.build();
	}

	//---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---o---//

	@SuppressWarnings("unchecked")
	private EnumStateMachine build() throws ParseException {
		EnumStateMachine esm = new EnumStateMachine();
		eatWhiteSpace();

		String initialString = getString(Token.DIVIDER);
		if (initialString != null) {
			esm.setInitialState(instantiate(initialString));
		}

		while (!isEmpty()) {
			String name = getString(Token.NAME_END);
			chomp(Token.SET_START);
			String elements[] = getStrings(Token.SET_END, Token.COMMA);
			chomp(Token.DIVIDER);

			if (name == null || elements == null) {
				throw new ParseException("Malformed configuration string.");
			}

			Enum from = instantiate(name);

			for (String _to : elements) {
				Enum to = instantiate(_to);
				esm.addTransition(from, to);
			}
		}

		return esm;
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

	@SuppressWarnings("unchecked")
	private Enum instantiate(String string) throws ParseException {
		if (string.equals("null"))
			return null;

		int dot = string.lastIndexOf(".");

		if (dot == -1  ||  dot+1 == string.length()) {
			throw new ParseException("Invalid class string: " + string);
		}

		String front = string.substring(0, dot).trim();
		String back = string.substring(dot+1).trim();
		Enum e;

		try {
			Class c = Class.forName(front);
			e = Enum.valueOf(c, back);
		} catch (ClassNotFoundException ex) {
			throw new ParseException("Invalid class string: " + front);
		}

		return e;
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
