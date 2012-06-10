# Enum State Machine
A core Java tool which provides state machine semantics using enums to represent the various states.
States have transitions which can move them to other states. Callbacks are provided for transitions,
and for each state when entering or exiting.

Any enums can be used, and mixing and matching from different classes is allowed. The `addAll(...)`
method will add all possible connections within an enum class. Use `addTransition(...)` to manually
add states and possible transitions to an instance.

# Installation
The project is set up with a Maven POM for easy use. Download a stable tag of the project and run
`mvn install` to install to your local environment. You can release the Maven artifact to a shared
repository for others to use if you would like.

You can also use the repository which I host myself. Just add the following to your project's POM:
```
<repositories>
  <repository>
		<id>uqc</id>
		<name>UnquietCode Repository</name>
		<url>http://www.unquietcode.com/maven/releases</url>
	</repository>
</repositories>
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
    
    EnumStateMachine esm = new EnumStateMachine(State.Ready);
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
StateMachineCallback cb = new StateMachineCallback() {
    public void performAction() {
        System.out.println("Hello");
    }
};

esm.addTransitions(cb, State.Ready, State.Running);
```

Callbacks can also be added on entering or exiting a state.
```java
esm.onEntering(State.Running, new StateMachineCallback() {
    public void performAction() {
        System.out.println("State moved to 'Running'");
    }
});

esm.onExiting(State.Running, new StateMachineCallback() {
    public void performAction() {
        System.out.println("State moved from 'Running'");
    }
});
```

# Questions / Comments / Feedback
Send an email to blouis@unquietcode.com
  
Thanks!