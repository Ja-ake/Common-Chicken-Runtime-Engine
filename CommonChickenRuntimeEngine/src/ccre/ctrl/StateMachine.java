/*
 * Copyright 2015 Colby Skeggs
 *
 * This file is part of the CCRE, the Common Chicken Runtime Engine.
 *
 * The CCRE is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * The CCRE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with the CCRE.  If not, see <http://www.gnu.org/licenses/>.
 */
package ccre.ctrl;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanOutput;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.EventOutputRecoverable;
import ccre.channel.EventStatus;
import ccre.channel.FloatOutput;
import ccre.log.LogLevel;
import ccre.log.Logger;

/**
 * A finite-state machine. This has a number of named, predefined states, which
 * can be switched between, and can affect the functionality of other parts of
 * the code.
 *
 * Users can be notified when a state is exited or entered, and can switch
 * between states either regarding or disregarding the current state. They can
 * also, of course, determine the current state.
 *
 * @author skeggsc
 */
public class StateMachine {
    private int currentState;
    /**
     * The number of states in this machine.
     */
    public final int numberOfStates;
    private final EventStatus onExit = new EventStatus();
    private final EventStatus onEnter = new EventStatus();
    private final String[] stateNames;

    /**
     * Create a new StateMachine with a named defaultState and a list of state
     * names. The names cannot be null or duplicates.
     *
     * @param defaultState the state to initially be in.
     * @param names the names of the states.
     */
    public StateMachine(String defaultState, String... names) {
        checkNamesConsistency(names);
        this.stateNames = names;
        numberOfStates = names.length;
        setState(defaultState);
    }

    /**
     * Create a new StateMachine with an indexed defaultState and a list of
     * state names.
     *
     * @param defaultState the state to initially be in, as an index in the list
     * of names.
     * @param names the names of the states.
     */
    public StateMachine(int defaultState, String... names) {
        checkNamesConsistency(names);
        this.stateNames = names;
        numberOfStates = names.length;
        setState(defaultState);
    }

    private static void checkNamesConsistency(String... names) {
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            if (name == null) {
                throw new NullPointerException();
            }
            for (int j = i + 1; j < names.length; j++) {
                if (name.equals(names[j])) {
                    throw new IllegalArgumentException("Duplicate state name: " + names[i]);
                }
            }
        }
    }

    private int indexOfName(String state) {
        for (int i = 0; i < numberOfStates; i++) {
            if (state.equals(stateNames[i])) {
                return i;
            }
        }
        throw new IllegalArgumentException("State name not found: " + state);
    }

    /**
     * Set the state of this machine to the named state.
     *
     * @param state the state to change to.
     */
    public void setState(String state) {
        setState(indexOfName(state));
    }

    /**
     * Set the state of this machine to the indexed state.
     *
     * @param state the state to change to, as an index in the list of state
     * names.
     */
    public void setState(int state) {
        if (state < 0 || state >= numberOfStates) {
            throw new IllegalArgumentException("Invalid state ID: " + state);
        }
        if (state == currentState) {
            return;
        }
        onExit.produce();
        currentState = state;
        onEnter.produce();
    }

    /**
     * Change to the named state when the event occurs.
     *
     * @param state the state to change to.
     * @param when when to change state.
     */
    public void setStateWhen(String state, EventInput when) {
        setStateWhen(indexOfName(state), when);
    }

    /**
     * Change to the indexed state when the event occurs.
     *
     * @param state the state to change to, as an index in the list of state
     * names.
     * @param when when to change state.
     */
    public void setStateWhen(int state, EventInput when) {
        when.send(getStateSetEvent(state));
    }

    /**
     * Get an event that will change the state to the named state.
     *
     * @param state the state to change to.
     * @return the event that changes state.
     */
    public EventOutput getStateSetEvent(String state) {
        return getStateSetEvent(indexOfName(state));
    }

    /**
     * Get an event that will change the state to the indexed state.
     *
     * @param state the state to change to, as an index in the list of state
     * names.
     * @return the event that changes state.
     */
    public EventOutput getStateSetEvent(final int state) {
        if (state < 0 || state >= numberOfStates) {
            throw new IllegalArgumentException("Invalid state ID: " + state);
        }
        return new EventOutputRecoverable() {
            public void event() {
                if (state == currentState) {
                    return;
                }
                onExit.produce();
                currentState = state;
                onEnter.produce();
            }

            public boolean eventWithRecovery() {
                if (state == currentState) {
                    return false;
                }
                boolean out = onExit.produceWithFailureRecovery();
                currentState = state;
                out |= onEnter.produceWithFailureRecovery();
                return out;
            }
        };
    }

    /**
     * Get the current state.
     *
     * @return the index of the current state.
     */
    public int getState() {
        return currentState;
    }

    /**
     * Get the name of the current state.
     *
     * @return the name of the current state.
     */
    public String getStateName() {
        return stateNames[currentState];
    }

    /**
     * Get the name of the indexed state.
     *
     * @param state the state to look up, as an index in the list of state
     * names.
     * @return the name of the indexed state.
     */
    public String getStateName(int state) {
        if (state < 0 || state >= numberOfStates) {
            throw new IllegalArgumentException("Invalid state ID: " + state);
        }
        return stateNames[state];
    }

    /**
     * Check if the machine is in the named state.
     *
     * @param state the state to check.
     * @return if this machine is in that state.
     */
    public boolean isState(String state) {
        return isState(indexOfName(state));
    }

    /**
     * Check if the machine is in the indexed state.
     *
     * @param state the state to check, as an index in the list of state names.
     * @return if this machine is in that state.
     */
    public boolean isState(int state) {
        return currentState == state;
    }

    /**
     * Return a channel representing if the machine is in the named state.
     *
     * @param state the state to check.
     * @return a channel for if this machine is in that state.
     */
    public BooleanInputPoll getIsState(String state) {
        return getIsState(indexOfName(state));
    }

    /**
     * Return a channel representing if the machine is in the indexed state.
     *
     * @param state the state to check, as an index in the list of state names.
     * @return a channel for if this machine is in that state.
     */
    public BooleanInputPoll getIsState(final int state) {
        if (state < 0 || state >= numberOfStates) {
            throw new IllegalArgumentException("Invalid state ID: " + state);
        }
        return new BooleanInputPoll() {
            public boolean get() {
                return currentState == state;
            }
        };
    }

    /**
     * Return an input representing if the machine is in the named state.
     *
     * @param state the state to check.
     * @return an input for if this machine is in that state.
     */
    public BooleanInput getIsStateDyn(String state) {
        return getIsStateDyn(indexOfName(state));
    }

    /**
     * Return an input representing if the machine is in the indexed state.
     *
     * @param state the state to check, as an index in the list of state names.
     * @return an input for if this machine is in that state.
     */
    public BooleanInput getIsStateDyn(int state) {
        return BooleanMixing.createDispatch(getIsState(state), onEnter);
    }

    /**
     * Return an event that moves the machine to the target state if it is in
     * the source state.
     *
     * @param fromState the source state.
     * @param toState the target state.
     * @return the event to conditionally change the machine's state.
     */
    public EventOutput getStateTransitionEvent(String fromState, String toState) {
        return getStateTransitionEvent(indexOfName(fromState), indexOfName(toState));
    }

    /**
     * Return an event that moves the machine to the target state if it is in
     * the source state.
     *
     * @param fromState the source state, as an index in the list of state
     * names.
     * @param toState the target state, as an index in the list of state names.
     * @return the event to conditionally change the machine's state.
     */
    public EventOutput getStateTransitionEvent(int fromState, int toState) {
        return EventMixing.filterEvent(getIsState(fromState), true, getStateSetEvent(toState));
    }

    /**
     * When the event occurs, move this machine to the target state if it is in
     * the source state.
     *
     * @param fromState the source state.
     * @param toState the target state.
     * @param when when to change state.
     */
    public void transitionStateWhen(String fromState, String toState, EventInput when) {
        transitionStateWhen(indexOfName(fromState), indexOfName(toState), when);
    }

    /**
     * When the event occurs, move this machine to the target state if it is in
     * the source state.
     *
     * @param fromState the source state, as an index in the list of state
     * names.
     * @param toState the target state, as an index in the list of state names.
     * @param when when to change state.
     */
    public void transitionStateWhen(int fromState, int toState, EventInput when) {
        when.send(getStateTransitionEvent(fromState, toState));
    }

    /**
     * Whenever the state changes, log a message constructed from the prefix
     * concatenated with the name of the current state.
     *
     * No space is inserted automatically - include that in the prefix.
     *
     * @param level the logging level at which to log the message.
     * @param prefix the prefix of the message to log.
     */
    public void autologTransitions(final LogLevel level, final String prefix) {
        onEnter.send(new EventOutput() {
            public void event() {
                Logger.log(level, prefix + getStateName());
            }
        });
    }

    /**
     * Get an event that will fire whenever a new state is entered.
     *
     * @return the event input.
     */
    public EventInput getStateEnterEvent() {
        return onEnter;
    }

    /**
     * Fire output whenever a new state is entered.
     *
     * @param output the event to fire.
     */
    public void onStateEnter(EventOutput output) {
        onEnter.send(output);
    }

    /**
     * Get an event that will fire when the named state is entered.
     *
     * @param state the state to monitor.
     * @return the event input.
     */
    public EventInput onEnterState(String state) {
        return onEnterState(indexOfName(state));
    }

    /**
     * Get an event that will fire when the indexed state is entered.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @return the event input.
     */
    public EventInput onEnterState(int state) {
        final EventStatus out = new EventStatus();
        onEnterState(state, out);
        return out;
    }

    /**
     * Fire output when the named state is entered.
     *
     * @param state the state to monitor.
     * @param output the event to fire.
     */
    public void onEnterState(String state, final EventOutput output) {
        onEnterState(indexOfName(state), output);
    }

    /**
     * Fire output when the indexed state is entered.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the event to fire.
     */
    public void onEnterState(int state, final EventOutput output) {
        onEnter.send(EventMixing.filterEvent(getIsState(state), true, output));
    }

    /**
     * Set output to value when the named state is entered.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnEnterState(String state, BooleanOutput output, boolean value) {
        setOnEnterState(indexOfName(state), output, value);
    }

    /**
     * Set output to value when the indexed state is entered.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnEnterState(int state, BooleanOutput output, boolean value) {
        onEnterState(state, BooleanMixing.getSetEvent(output, value));
    }

    /**
     * Set output to true when the named state is entered.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     */
    public void setTrueOnEnterState(String state, BooleanOutput output) {
        setTrueOnEnterState(indexOfName(state), output);
    }

    /**
     * Set output to true when the indexed state is entered.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     */
    public void setTrueOnEnterState(int state, BooleanOutput output) {
        setOnEnterState(state, output, true);
    }

    /**
     * Set output to false when the named state is entered.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     */
    public void setFalseOnEnterState(String state, BooleanOutput output) {
        setFalseOnEnterState(indexOfName(state), output);
    }

    /**
     * Set output to false when the indexed state is entered.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     */
    public void setFalseOnEnterState(int state, BooleanOutput output) {
        setOnEnterState(state, output, false);
    }

    /**
     * Set output to value when the named state is entered.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnEnterState(String state, FloatOutput output, float value) {
        setOnEnterState(indexOfName(state), output, value);
    }

    /**
     * Set output to value when the indexed state is entered.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnEnterState(int state, FloatOutput output, float value) {
        onEnterState(state, FloatMixing.getSetEvent(output, value));
    }

    /**
     * Get an event that will fire whenever a state is exited, and before the
     * next state is entered.
     *
     * @return the event input.
     */
    public EventInput getStateExitEvent() {
        return onExit;
    }

    /**
     * Fire output whenever a state is exited.
     *
     * @param output the output to fire.
     */
    public void onStateExit(EventOutput output) {
        onExit.send(output);
    }

    /**
     * Get an event that will fire when the named state is exited.
     *
     * @param state the state to monitor.
     * @return the event input.
     */
    public EventInput onExitState(String state) {
        return onExitState(indexOfName(state));
    }

    /**
     * Get an event that will fire when the indexed state is exited.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @return the event input.
     */
    public EventInput onExitState(int state) {
        final EventStatus out = new EventStatus();
        onExitState(state, out);
        return out;
    }

    /**
     * Fire output when the named state is exited.
     *
     * @param state the state to monitor.
     * @param output the event to fire.
     */
    public void onExitState(String state, final EventOutput output) {
        onExitState(indexOfName(state), output);
    }

    /**
     * Fire output when the indexed state is exited.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the event to fire.
     */
    public void onExitState(int state, final EventOutput output) {
        onExit.send(EventMixing.filterEvent(getIsState(state), true, output));
    }

    /**
     * Set output to value when the named state is exited.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnExitState(String state, BooleanOutput output, boolean value) {
        setOnExitState(indexOfName(state), output, value);
    }

    /**
     * Set output to value when the indexed state is exited.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnExitState(int state, BooleanOutput output, boolean value) {
        onExitState(state, BooleanMixing.getSetEvent(output, value));
    }

    /**
     * Set output to true when the named state is exited.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     */
    public void setTrueOnExitState(String state, BooleanOutput output) {
        setTrueOnExitState(indexOfName(state), output);
    }

    /**
     * Set output to true when the indexed state is exited.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     */
    public void setTrueOnExitState(int state, BooleanOutput output) {
        setOnExitState(state, output, true);
    }

    /**
     * Set output to false when the named state is exited.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     */
    public void setFalseOnExitState(String state, BooleanOutput output) {
        setFalseOnExitState(indexOfName(state), output);
    }

    /**
     * Set output to false when the indexed state is exited.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     */
    public void setFalseOnExitState(int state, BooleanOutput output) {
        setOnExitState(state, output, false);
    }

    /**
     * Set output to value when the named state is exited.
     *
     * @param state the state to monitor.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnExitState(String state, FloatOutput output, float value) {
        setOnExitState(indexOfName(state), output, value);
    }

    /**
     * Set output to value when the indexed state is exited.
     *
     * @param state the state to monitor, as an index in the list of state
     * names.
     * @param output the output to modify.
     * @param value the value to set the output to.
     */
    public void setOnExitState(int state, FloatOutput output, float value) {
        onExitState(state, FloatMixing.getSetEvent(output, value));
    }
}
