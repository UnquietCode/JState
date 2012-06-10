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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author  Benjamin Fagin
 * @version 12-23-2010
 * @version 06-10-2012
 */
public class EnumStringParser {
	private Map<Enum, String> valueCache = new HashMap<Enum, String>();
	private StringBuffer buffer;


	public EnumStringParser(String string) {
		buffer = new StringBuffer(string);
	}

	public EnumStringParser(StringBuffer buffer) {
		this.buffer = new StringBuffer(buffer);
	}


	public boolean isEmpty() {
		// I hope the string is put on the stack for immediate collection.
		return buffer.toString().trim().length() == 0;
	}

	public String value(Enum e) {
		return extractValue(e);
	}

	public void chomp(Enum token) {
		match(token);
	}

	public String getString(Enum token) {
		return match(token);
	}

	public Integer getInt(Enum token) {
		String s = getString(token);
		if (s == null) { return null; }

		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException ex) {
			throw new ParseException(ex);
		}
	}

	public Boolean getBoolean(Enum token) {
		String s = getString(token);
		if (s == null) { return null; }

		try {
			return Boolean.parseBoolean(s);
		} catch (Exception ex) {
			throw new ParseException(ex);
		}
	}

	public Double getDouble(Enum token) {
		String s = getString(token);
		if (s == null) { return null; }

		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException ex) {
			throw new ParseException(ex);
		}
	}

	public String[] getStrings(Enum token, Enum delimiter) {
		String list = match(token);
		if (list == null) {	return null; }
		if (list.isEmpty()) { return new String[]{}; }


		String cut[] = list.split(extractValue(delimiter));
		for (int i=0; i < cut.length; ++i) {
			cut[i] = cut[i].trim();
		}

		return cut;
	}

	public Integer[] getInts(Enum token, Enum delimiter) {
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

	public Boolean[] getBooleans(Enum token, Enum delimiter) {
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

	public Double[] getDoubles(Enum token, Enum delimiter) {
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

	public void clearValueCache() {
		valueCache.clear();
	}

	//-------------------------------------------------------------------------------------------//

	private String match(Enum token) {
		String value = extractValue(token);

		int index = buffer.indexOf(value);
		if (index == -1) {
			return null;
		}

		String front = buffer.substring(0, index);
		buffer.delete(0, index +value.length());
		front = front.trim();

		return front;
	}

	private String extractValue(Enum e) {
		if (valueCache.containsKey(e)) {
			return valueCache.get(e);
		}

		Class clazz = e.getClass();
		String retval;
		Field field;

		try {
			field = clazz.getField(e.name());
		} catch (NoSuchFieldException ex) {
			throw new RuntimeException(ex);
		}

		Value name;
		name = field.getAnnotation(Value.class);

		if (name != null) {
			retval = name.value();
		} else {
			retval = e.toString();
		}

		valueCache.put(e, retval);
		return retval;
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface Value {
		String value();
	}
}
