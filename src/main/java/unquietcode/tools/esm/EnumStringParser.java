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
import java.util.HashMap;
import java.util.Map;

/**
 * @author  Benjamin Fagin
 * @version 12-23-2010
 */
public class EnumStringParser {
	private Map<Enum, String> valueCache = new HashMap<Enum, String>();
	private StringBuffer buffer;

	public EnumStringParser(String string) {
		buffer = new StringBuffer(string);
	}

	public EnumStringParser(StringBuffer buffer) {
		this.buffer = new StringBuffer(buffer.toString());  // I don't think string buffers copy. TODO Do a test.
	}

	public boolean isEmpty() {
		return buffer.toString().trim().length() == 0;  //TODO this is too intensive
	}

	private String extractValue(Enum e) {
		String retval;

		dance: {
			if (valueCache.containsKey(e)) {
				retval = valueCache.get(e);
				break dance;
			}

			Class clazz = e.getClass();
			if (!clazz.isEnum()) {
				retval = e.toString();
				break dance;
			}

			Value name;
			try {
				Field field = clazz.getField(e.name());
				name = field.getAnnotation(Value.class);
			} catch (Exception ex) {
				retval = e.toString();
				break dance;
			}

			if (name == null) {
				retval = e.toString();
				break dance;
			}

			retval = name.value();
			valueCache.put(e, retval);
		}

		return retval;
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
		String s = match(token);
		if (s == null)
			return null;
		else
			return Integer.parseInt(s);
	}

	public Boolean getBoolean(Enum token) {
		String s = match(token);
		if (s == null)
			return null;
		else
			return Boolean.parseBoolean(s);
	}

	public Double getDouble(Enum token) {
		String s = match(token);
		if (s == null)
			return null;
		else
			return Double.parseDouble(s);
	}

	public String[] getStrings(Enum token, Enum delimiter) {
		String list = match(token);
		if (list == null)
			return null;
		else
			return list.split(value(delimiter));
	}

/*	public int[] getInts(Enum token, Enum delimiter) {
		String list = match(token);
		if (list == null)
			return null;
		else
			return list.split(value(delimiter));
	}
*/
//TODO this and getDoubles, getBooleans, etc


	public String match(Enum token) {
		String value = extractValue(token);

		int index = buffer.indexOf(value);
		if (index == -1) {
			return null;
		}

		String front = buffer.substring(0, index);
		buffer.delete(0, index +value.length());
		front = front.trim();
		return front.isEmpty() ? null : front;
	}

	public void clearValueCache() {
		valueCache.clear();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	public static @interface Value {
		String value(); // no default means required
	}

/*
	public static class ParseException extends RuntimeException {
		public ParseException(String message) {
			super(message);
		}
	}*/
}
