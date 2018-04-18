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

package unquietcode.tools.esm.routing;

import unquietcode.tools.esm.StateRouter;

import java.security.SecureRandom;
import java.util.*;

import static java.util.Objects.requireNonNull;

/**
 * @author Ben Fagin
 * @version 2018-04-18
 */
public class RandomStateRouter<T> implements StateRouter<T> {
	private final SecureRandom randomGenerator = new SecureRandom();
	private final List<StateRouter<T>> routers;

	public RandomStateRouter(Set<StateRouter<T>> routers) {
		this.routers = new ArrayList<>(requireNonNull(routers));

		// seems like a good idea to shuffle it once beforehand
		Collections.shuffle(this.routers);
	}

	@SafeVarargs
	public RandomStateRouter(StateRouter<T>...routers) {
		this(newIdentitySet(Arrays.asList(requireNonNull(routers))));
	}

	@Override
	public synchronized T route(T current, T next) {
		if (routers.isEmpty()) {
			return next;
		}

		// find the next router by randomly selecting one from the list
		int nextRouter = randomGenerator.nextInt(routers.size());
		StateRouter<T> router = routers.get(nextRouter);

		return router.route(current, next);
	}

	private static <T> Set<StateRouter<T>> newIdentitySet(Collection<StateRouter<T>> items) {
		Set<StateRouter<T>> set = Collections.newSetFromMap(new IdentityHashMap<>());
		set.addAll(items);
		return set;
	}
}