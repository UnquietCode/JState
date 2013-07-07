package unquietcode.tools.esm;

/**
 * Provides a simple mechanism to remove callbacks from the
 * {@link StateMachine} instance after they have been attached
 * to a transition, entry action, or exit action.
 *
 * @author Ben Fagin
 * @version 2013-07-07
 */
public interface CallbackRegistration {
	void unregister();
}
