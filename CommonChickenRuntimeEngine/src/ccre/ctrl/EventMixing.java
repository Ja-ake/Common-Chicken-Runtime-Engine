/*
 * Copyright 2013-2014 Colby Skeggs
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

import ccre.channel.BooleanInputPoll;
import ccre.channel.EventInput;
import ccre.channel.EventOutput;
import ccre.channel.EventStatus;

/**
 * EventMixing is a class that provides a wide variety of useful static methods
 * to accomplish various common actions primarily relating to event channels.
 *
 * @author skeggsc
 * @see BooleanMixing
 * @see FloatMixing
 * @see Mixing
 */
public class EventMixing {

    /**
     * An EventOutput that, when fired, does absolutely nothing.
     */
    public static final EventOutput ignored = new EventOutput() {
        public void event() {
        }
    };
    /**
     * An EventInput that will never be fired. Ever.
     */
    public static final EventInput never = new EventInput() {
        public void send(EventOutput listener) {
        }

        public void unsend(EventOutput listener) {
        }
    };

    /**
     * Combine two EventSources so that either event firing will fire the result
     * event.
     *
     * @param a the first event source
     * @param b the second event source
     * @return the source that is fired by either of the original sources.
     */
    public static EventInput combine(EventInput a, EventInput b) {
        EventStatus e = new EventStatus();
        a.send(e);
        b.send(e);
        return e;
    }

    /**
     * Combine multiple EventSources so that any event firing will fire the
     * result event.
     *
     * @param sources the event sources
     * @return the source that is fired by any of the original sources.
     */
    public static EventInput combine(EventInput... sources) {
        EventStatus e = new EventStatus();
        for (EventInput es : sources) {
            es.send(e);
        }
        return e;
    }

    /**
     * Returns a combination of the specified events such that the returned
     * EventConsumer will fire all arguments when fired.
     *
     * @param events the events to fire
     * @return the trigger for firing the arguments.
     */
    public static EventOutput combine(final EventOutput... events) {
        return new EventOutput() {
            public void event() {
                for (EventOutput cnsm : events) {
                    cnsm.event();
                }
            }
        };
    }

    /**
     * Returns a combination of the specified events such that the returned
     * EventConsumer will fire both arguments when fired.
     *
     * @param a the first event
     * @param b the second event
     * @return the trigger for firing the arguments.
     */
    public static EventOutput combine(final EventOutput a, final EventOutput b) {
        return new EventOutput() {
            public void event() {
                a.event();
                b.event();
            }
        };
    }

    /**
     * Returns a debounced version of the specified EventConsumer, such that
     * there is a minimum delay of minMillis milliseconds between events.
     *
     * Any event sent before the timeout will be ignored.
     *
     * @param orig The EventConsumer to debounce.
     * @param minMillis The minimum event delay.
     * @return The debounced version of the event consumer.
     */
    public static EventOutput debounce(EventOutput orig, int minMillis) {
        return new MixingImpls.DebounceImpl(orig, minMillis);
    }

    /**
     * Returns a debounced version of the specified EventSource, such that there
     * is a minimum delay of minMillis milliseconds between events.
     *
     * Any event sent before the timeout will be ignored.
     *
     * @param orig The EventSource to debounce.
     * @param minMillis The minimum event delay.
     * @return The debounced version of the event source.
     */
    public static EventInput debounce(EventInput orig, int minMillis) {
        EventStatus e = new EventStatus();
        orig.send(new MixingImpls.DebounceImpl(e, minMillis));
        return e;
    }

    /**
     * When the returned EventConsumer is fired and the specified
     * BooleanInputPoll is the specified requirement, fire the passed
     * EventConsumer.
     *
     * @param input the input to test.
     * @param requirement the value to require.
     * @param target the target to fire.
     * @return when to check if the target should be fired.
     */
    public static EventOutput filterEvent(BooleanInputPoll input, boolean requirement, EventOutput target) {
        return new MixingImpls.FEC(input, requirement, target);
    }

    /**
     * Return an EventSource that is fired when the specified EventSource is
     * fired and the specified BooleanInputPoll is the specified requirement.
     *
     * @param input the input to test.
     * @param requirement the value to require.
     * @param when when to check if the target should be fired.
     * @return the target to fire.
     */
    public static EventInput filterEvent(BooleanInputPoll input, boolean requirement, EventInput when) {
        final EventStatus out = new EventStatus();
        when.send(new MixingImpls.FES(input, requirement, out));
        return out;
    }

    private EventMixing() {
    }
}
