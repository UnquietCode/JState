package unquietcode.tools.esm;

import java.util.List;

/**
 * @author Ben Fagin
 * @version 2013-07-08
 */
public interface SequenceHandler<T> {

	/**
	 * Handle a match of the given sequence.
	 *
	 * @param pattern the pattern which was matched
	 */
	void onMatch(List<T> pattern);
}
