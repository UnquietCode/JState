/*******************************************************************************
 Copyright 2011 Benjamin Fagin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.


    Read the included LICENSE.TXT for more information.
 ******************************************************************************/

package unquietcode.tools.esm;

import unquietcode.tools.esm.EnumStringParser.Value;

import java.util.*;


/**
 * @author  Benjamin Fagin
 * @version 12-23-2010
 *
 * Null is a valid state in this system. The initial starting point is null by default.
 */
public class EnumStateMachine {
	private Map<Enum, Set<Enum>> states = new HashMap<Enum, Set<Enum>>();
	private Enum initial;
	private Enum current;
	int transitions;

	/**
	 * Basic form is:
	 * "state1 : {transition1, transition2}, state2 : {transition3, transition4}, state3 : {}"
	 *
	 * Listing empty sets is optional. Can also add the intial state to the front like so:
	 * "initial | state1 : {transition1, transition2} | state2 : {transition3, transition4}"
	 *
	 * The states are the class strings of the enums being used. They will be processed via reflection.
	 * The existing information is preserved; used clear() prior to avoid that.
	 * When the operation is complete, the state machine is reset.
	 *
	 * @param   string   configuration string
	 * @throws  ParseException  if the configuration string is malformed
	 */
	public void configureByString(String string) throws ParseException {
		EnumStringParser parser = new EnumStringParser(string.trim());
		StringBuilder sb = new StringBuilder();

		String initialString = parser.getString(Token.DIVIDER);
		if (initialString != null) {
			initial = instantiate(initialString);
		}

		while (!parser.isEmpty()) {
			String name = parser.getString(Token.NAME_END);
			parser.chomp(Token.SET_START);
			String elements[] = parser.getStrings(Token.SET_END, Token.COMMA);
			parser.chomp(Token.DIVIDER);

			if (name == null || elements == null) {
				throw new ParseException("Malformed configuration string.");
			}

			Enum state = instantiate(name);
			HashSet<Enum> transitions = new HashSet<Enum>();

			for (String e : elements) {
				transitions.add(instantiate(e));
			}

			states.put(state, transitions);
		}

		reset();
	}

	enum Token {
		@Value("{")
		SET_START,

		@Value("}")
		SET_END,

		@Value("|")
		DIVIDER,

		@Value(":")
		NAME_END,

		@Value(",")
		COMMA;

	}


	/*
		I guess you can break it down into basic iterations:

			+ the initial, which could be empty
			+ the state strings, which could number 0
				+ the state string
				+ the set string
					+ the class strings, which could number 0


			find pipe. if none, is it initial only or no initial?
			find colon, if none, fail
			find { after colon
			find } after {
	 */

	private <T, V> void addToHashSet(Map<T, Set<V>> map, T key, Set<V> data) {
		if (map.containsKey(key)) {
			map.get(key).addAll(data);
		} else {
			Set<V> set = new HashSet<V>();
			set.addAll(data);
			map.put(key, set);
		}
	}

	private <T, V> void addToHashSet(Map<T, Set<V>> map, T key, V data) {
		if (map.containsKey(key)) {
			map.get(key).add(data);
		} else {
			Set<V> set = new HashSet<V>();
			set.add(data);
			map.put(key, set);
		}
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
		Enum e = null;

		try {
			Class c = Class.forName(front);
			e = Enum.valueOf(c, back);
		} catch (ClassNotFoundException ex) {
			throw new ParseException("Invalid class string: " + front);
		}

		return e;
	}


	public @Override
	String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(fullString(initial)).append(" | \n");

		int i = 1;
		for (Map.Entry<Enum, Set<Enum>> entry : states.entrySet()) {
			sb.append(fullString(entry.getKey())).append(" : {");

			int j = 1;
			for (Enum e : entry.getValue()) {
				sb.append(fullString(e));

				if (j++ != entry.getValue().size())
					sb.append(", ");
			}

			sb.append("}");

			if (i++ != states.size())
				sb.append(" | \n");
		}

		return sb.toString();
	}

	public String fullString(Enum e) {
		if (e == null)
			return null;

		return e.getClass().getName() + "." + e.toString();
	}

	// need full body because this(null) is ambiguous with String
	public EnumStateMachine() {
		initial = null;
		current = initial;
		transitions = 0;
	}

	public EnumStateMachine(Enum initial) {
		this.initial = initial;
		current = initial;
		transitions = 0;
	}

	public EnumStateMachine(String configuration) throws ParseException {
		configureByString(configuration);
	}

	public static class TransitionException extends RuntimeException {
		public TransitionException(String message) {
			super(message);
		}
	}

	public static class ParseException extends Exception {
		public ParseException(String message) {
			super(message);
		}
	}

	public void reset() {
		transitions = 0;
		current = initial;
	}

	public boolean transition(Enum state) {
		Set<Enum> set = states.get(current);

		if (set == null  ||  set.size() == 0  ||  !set.contains(state)) {
			throw new TransitionException("No transition exists between "+ current +" and "+ state);
		} else {
			transitions += 1;

			if (current == state) {
				return false;
			} else {
				current = state;
				return true;
			}
		}
	}

	public Enum state() {
		return current;
	}

	public Enum getInitialState() {
		return initial;
	}

	/**
	 * will not reset
	 *
	 * @param state
	 */
	public void setInitialState(Enum state) {
		initial = state;
	}

	public boolean addTransitions(Enum state, Enum...transitions) {
		Set<Enum> set = makeSet(transitions);

		if (states.containsKey(state)) {
			states.get(state).addAll(set);
			return false;
		} else {
			states.put(state, set);
			return true;
		}
	}

	public boolean setTransitions(Enum state, Enum...transitions) {
		Set<Enum> set = makeSet(transitions);
		return states.put(state, set) == null;
	}

	private Set<Enum> makeSet(Enum states[]) {
		Set<Enum> set = new HashSet<Enum>();

		if (states == null) {
			set.add(null);
		} else {
			set.addAll(Arrays.asList(states));
		}

		return set;
	}


	public @Override
	boolean equals(Object o) {
		if (o.getClass() != this.getClass())
			return false;

		EnumStateMachine other = (EnumStateMachine) o;

		if (states.size() != other.states.size()) {
			return false;
		}

		for (Map.Entry<Enum, Set<Enum>> entry : states.entrySet()) {
			if (!other.states.containsKey(entry.getKey()))
				return false;

			Set<Enum> tSet = entry.getValue();
			Set<Enum> oSet = other.states.get(entry.getKey());

			if (tSet.size() != oSet.size())
				return false;

			for (Enum e : tSet) {
				if (!oSet.contains(e))
					return false;
			}
		}

		return true;
	}

	/**
	 * For every enum in the class, creates a transition between that enum and the others.
	 * If includeSelf is true, the enums are allowed to transition back to themselves.
	 *
	 * @param clazz         The class of the enum to add.
	 * @param includeSelf   True if enums are allowed to transition to themselves.
	 */
	public void addAll(Class clazz, boolean includeSelf) {
		if (clazz == null || !clazz.isEnum())
			return;

		Set<Enum> full = new HashSet<Enum>(Arrays.asList((Enum[]) clazz.getEnumConstants()));

		if (includeSelf) {
			for (Enum e : full) {
				states.put(e, new HashSet<Enum>(full));
			}
		} else {
			for (Enum e : full) {
				Set<Enum> rest = new HashSet<Enum>(full);
				rest.remove(e);
				states.put(e,rest);
			}
		}
	}
}





//TODO some kind of listener system that allows the user to respond to transitions
/*
	something like
		addListener(Enum state1, Enum state2, Listener listener)

	then, whenever a transition from state1 to state2 occurs, the lister will be notified with
	some sort of event.

	So either use the built-in property-change listener system, or make a new system.
	(better to just work with the existing stuff). Could use the weaklistener wrapepr from
	PaperTrail to prevent lapsed listeners.
*/
