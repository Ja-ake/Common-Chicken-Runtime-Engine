/*
 * Copyright 2013-2015 Colby Skeggs
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
package ccre.channel;

import java.io.Serializable;
import java.util.Iterator;

import ccre.concurrency.ConcurrentDispatchArray;
import ccre.log.Logger;
import ccre.util.CArrayUtils;

/**
 * An implementation of an EventInput. This can be fired using the .produce()
 * method or by firing this event as an EventOutput.
 *
 * @author skeggsc
 */
public class EventStatus implements EventInput, EventOutputRecoverable, Serializable {

    private static final long serialVersionUID = 115846451690403376L;

    /**
     * The events to fire when this event is fired.
     */
    private final ConcurrentDispatchArray<EventOutput> consumers = new ConcurrentDispatchArray<EventOutput>();

    /**
     * Create a new Event.
     */
    public EventStatus() {
    }

    /**
     * Create a new Event that fires the specified event when fired. This is
     * equivalent to adding the event as a listener.
     *
     * @param event the event to fire when this event is fired.
     * @see #send(ccre.channel.EventOutput)
     */
    public EventStatus(EventOutput event) {
        consumers.add(event);
    }

    /**
     * Create a new Event that fires the specified events when fired. This is
     * equivalent to adding the events as listeners.
     *
     * @param events the events to fire when this event is fired.
     * @see #send(ccre.channel.EventOutput)
     */
    public EventStatus(EventOutput... events) {
        consumers.addAllIfNotFound(CArrayUtils.asList(events));
    }

    /**
     * Returns whether or not this has any consumers that will get fired. If
     * this returns false, the produce() method will do nothing.
     *
     * @return whether or not the produce method would do anything.
     * @see #produce()
     */
    public boolean hasConsumers() {
        return !consumers.isEmpty();
    }

    /**
     * Returns the number of consumers.
     *
     * @return the number of consumers.
     * @deprecated this information does not need to be a public interface.
     */
    @Deprecated
    public int countConsumers() {
        return consumers.size();
    }

    /**
     * Produce this event - fire all listening events.
     */
    public void produce() {
        for (EventOutput ec : consumers) {
            ec.event();
        }
    }

    public void send(EventOutput client) {
        consumers.addIfNotFound(client);
    }

    public void unsend(EventOutput client) throws IllegalStateException {
        consumers.remove(client);
    }

    public void event() {
        produce();
    }

    public boolean eventWithRecovery() {
        return produceWithFailureRecovery();
    }

    /**
     * Same as produce, but if an exception is thrown, the event will be
     * DETACHED and reported as such!
     *
     * @return If anything was detached.
     * @see #produce()
     */
    public boolean produceWithFailureRecovery() {
        boolean found = false;
        for (Iterator<EventOutput> it = consumers.iterator(); it.hasNext();) {
            EventOutput ec = it.next();
            try {
                if (ec instanceof EventOutputRecoverable) {
                    if (((EventOutputRecoverable) ec).eventWithRecovery()) {
                        found = true;
                    }
                } else {
                    ec.event();
                }
            } catch (Throwable thr) {
                Logger.severe("Event Subscriber Detached: " + ec, thr);
                it.remove();
                found = true;
            }
        }
        return found;
    }

    /**
     * Clear all listeners on this EventStatus. Only do this if you have a very
     * good reason!
     */
    public void clearListeners() {
        consumers.clear();
    }

    /**
     * Returns a version of this status as an output. This is equivalent to
     * upcasting to EventOutput.
     *
     * @return this status, as an output.
     */
    public EventOutput asOutput() {
        return this;
    }

    /**
     * Returns a version of this status as an input. This is equivalent to
     * upcasting to EventInput.
     *
     * @return this status, as an input.
     */
    public EventInput asInput() {
        return this;
    }

}
