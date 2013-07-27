# Enum State Machine
A core Java tool which provides state machine semantics using enums, strings, or anything else you
want to represent the various states. States have transitions which can move them to other states.
Callbacks are provided for transitions, and for each state when entering or exiting. It is also
possible to route a transition request based on your own logic. You can even provide a callback
which will fire when a sequence of states is matched.

While the original project only suppoted enums, this version allows any type which implements 
`State` to be used. There is also a `ReflectiveStateMachine` worth checking out.

All of the methods which modify, transition, or inquire about the state are synchronized, allowing
multiple threads access to the same state machine. However, to avoid unpredictable behavior, it is
generally better to construct your state machine up front and not modify it thereafter. The
EnumStateMachine and StringStateMachine in particular can be serialized to and from their string
representations.

# Installation
The project is build using Maven. Download a stable tag of the project and run
`mvn install` to install to your local environment. Or you can release the Maven artifact to a shared
repository for others to use if you would like. OR  you can also use the repository which I host myself.
This is just a flat maven-style file system repo. Just add the following to your project's POM:
```
<repositories>
    <repository>
        <id>uqc</id>
        <name>UnquietCode Repository</name>
        <url>http://www.unquietcode.com/maven/releases</url>
    </repository>
</repositories>

...

<dependency>
    <groupId>unquietcode.tools.esm</groupId>
    <artifactId>esm</artifactId>
    <version>2.0</version>
</dependency>
```

# Usage
A typical use case might be a state machine for controlling a process, which can move between the
states [Ready, Running, Paused, Stopping, Stopped, Finished]. After declaring a state enum we can
set up a new state machine as follows:

```java
enum State {
Ready, Running, Paused, Stopping, Stopped, Finished
}

...

EnumStateMachine<State> esm = new EnumStateMachine<State>(State.Ready);
esm.addTransitions(State.Ready, State.Running, State.Finished);
esm.addTransitions(State.Running, State.Paused, State.Stopping);
esm.addTransitions(State.Paused, State.Running, State.Stopping);
esm.addTransitions(State.Stopping, State.Stopped);
esm.addTransitions(State.Stopped, State.Finished);
esm.addTransitions(State.Finished, State.Ready, null);

esm.transition(State.Running);
```

The initial state is set either in the constructor or the `setInitialState(...)` method. The `addTransition(...)`
method supports mapping from 1..n states. In the example above, we see that some states can move to more than
one other states. The `null` state is also a possibility, depending on your preference.

Callbacks can be added as transitions are defined, and fire during transition between states:

```java
TransitionHandler<State> cb = new TransitionHandler<State>() {
    public void onTransition(State from, State to) {
        // ....
    }
};

esm.addTransitions(cb, State.Ready, State.Running);
```

Callbacks can also be added on entering or exiting a state.
```java
esm.onEntering(State.Running, new StateHandler<State>() {
	public void onState(State state) {
		entered.incrementAndGet();
	}
});

esm.onExiting(State.Running, new StateHandler<State>() {
	public void onState(State state) {
		exited.incrementAndGet();
	}
});
```

Routers allow you to 'deflect' or 'redirect' a transition based on your own custom logic.
```java
esm.routeBeforeEntering(TestStates.Three, new StateRouter<TestStates>() {
	public TestStates route(TestStates current, TestStates next) {
		return TestStates.Two;
	}
});
```

Sequence Matchers are callbacks which are triggered whenever the specified sequence of states
occurs in the state machine.
```java
List<Color> _pattern = Arrays.asList(Color.Blue, Color.Green, Color.Orange);

sm.onSequence(_pattern, new SequenceHandler<Color>() {
	public void onMatch(List<Color> pattern) {
		assertEquals(_pattern, pattern);
	}
});
```

A special form of StringStateMachine is available as the ReflectiveStateMachine. This flavor allows you to declare
your callbacks as methods of the state machine class. The arguments are flexible, matching the standalone callback
method's signature and allowing you to skip parameters you don't care about.
```java
ReflectiveStateMachine sm = new ReflectiveStateMachine() {

	// (optional method to declare transitions inline)
	protected void declareTransitions() {
		addTransition(null, "blue");
		addTransition("blue", "green");
		addTransition("green", null);
	}

	public void onEnteringBlue(String state) {
		exitingBlue.incrementAndGet();
	}

	public void onExitingBlue() {
		exitingBlue.incrementAndGet();
	}

	public void onGreen() {
		enteringGreen.incrementAndGet();
	}

	public void onEntering() {
		enteringAny.incrementAndGet();
	}

	public void onExiting() {
		exitingAny.incrementAndGet();
	}

	public void onTransition() {
		transitionAny.incrementAndGet();
	}
};

sm.transition("blue");
sm.transition("green");
sm.transition(null);
```


See the [tests](https://github.com/UnquietCode/Enum-State-Machine/blob/master/src/test/java/unquietcode/tools/esm/EnumStateMachine_T.java)
for more usage examples, as well as the provided javadocs.

# License
ESM is licensed under the MIT license. Go wild.

# Questions / Comments / Feedback
Send an email to blouis@unquietcode.com
  
Peace, love, and code.

# Thanks!
