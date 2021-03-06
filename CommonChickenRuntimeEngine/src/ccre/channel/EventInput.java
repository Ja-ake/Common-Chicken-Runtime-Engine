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
package ccre.channel;

/**
 * An event input or source. This produces events when it fires. A user can
 * register listeners to be called when the EventInput fires.
 * ccre.event.EventStatus is a good implementation of this so that you don't
 * have to write your own listener management code.
 *
 * @see EventStatus
 * @author skeggsc
 */
public interface EventInput {

    /**
     * Register a listener for when this event is fired, so that whenever this
     * event is fired, the specified output will get fired as well.
     *
     * If the same listener is added multiple times, it has the same effect as
     * if it was added once.
     *
     * @param listener the listener to add.
     * @see #unsend(EventOutput)
     */
    void send(EventOutput listener);

    /**
     * Remove a listener for when this event is fired. This reverses the actions
     * of a previous send call.
     *
     * If the listener was not added previously (or had been removed), this call
     * will do nothing.
     *
     * After unsend is called, a listener can be reregistered with send.
     *
     * @param listener the listener to remove.
     * @see #send(EventOutput)
     */
    void unsend(EventOutput listener);
}
