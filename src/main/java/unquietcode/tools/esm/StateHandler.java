package unquietcode.tools.esm;

/**
 * @author Ben Fagin
 * @version 06-10-2012
 */
public interface StateHandler<T> {
	void onState(T state);
}