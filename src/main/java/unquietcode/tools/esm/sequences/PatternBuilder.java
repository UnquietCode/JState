/*******************************************************************************
 The MIT License (MIT)

 Copyright (c) 2018 Benjamin Fagin

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

package unquietcode.tools.esm.sequences;

import unquietcode.tools.esm.State;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ben Fagin
 * @version 2018-07-18
 */
public class PatternBuilder<T> {
	private PatternBuilder() {}
	private final List<Object> pattern = new ArrayList<>();


	public static <T extends State> PatternBuilder<T> create() {
		return new PatternBuilder<>();
	}

	public static <T> Pattern<T> createFrom(List<T> states) {
		return new Pattern<>(states);
	}

	@SafeVarargs
	public final PatternBuilder<T> add(T...states) {
		pattern.addAll(Arrays.asList(states));
		return this;
	}

	public Pattern<T> build() {
		Pattern<Object> p1 = new Pattern<>(pattern);

		@SuppressWarnings("unchecked")
		Pattern<T> p2 = (Pattern<T>) p1;

		return p2;
	}

	// wildcard matching state
	private static final Object WILDCARD = new Object();

	public static boolean isWildcard(Object element) {
		return element == WILDCARD;
	}

	public PatternBuilder<T> addWildcard() {
		pattern.add(WILDCARD);
		return this;
	}
}