package unquietcode.tools.esm;

/**
 * @author Ben Fagin
 * @version 2013-07-10
 */
public interface TransitionHandler<T> {
	void onTransition(T from, T to);
}
