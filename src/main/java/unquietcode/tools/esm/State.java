package unquietcode.tools.esm;

/**
 * State interface. Each state in a system should have
 * a unique name.
 *
 * This interface can be applied to any enum class, since
 * they already support a name() method.
 *
 * @author Ben Fagin
 * @version 2013-07-07
 */
public interface State {
	String name();
}