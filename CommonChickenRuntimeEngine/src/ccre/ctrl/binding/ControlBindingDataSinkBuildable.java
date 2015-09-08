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
package ccre.ctrl.binding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanOutput;
import ccre.channel.BooleanStatus;
import ccre.channel.FloatInput;
import ccre.channel.FloatOutput;
import ccre.channel.FloatStatus;
import ccre.util.Utils;

/**
 * A ControlBindingDataSinkBuildable allows a program to easily fill out a
 * ControlBindingDataSink by way of a ControlBindingCreator.
 *
 * In other words...
 *
 * With a ControlBindingDataSinkBuildable, you can use the easier-to-use
 * interface of a ControlBindingCreator to ask the user to control various parts
 * of a module, and then you can give the ControlBindingDataSink to something
 * like a CluckControlBinder so that it can be configured without special work
 * on your part.
 *
 * But probably, you should just use the interfaces in FRC.
 *
 * @author skeggsc
 */
public class ControlBindingDataSinkBuildable implements ControlBindingDataSink, ControlBindingCreator {

    private final HashMap<String, BooleanOutput> booleans = new HashMap<String, BooleanOutput>();
    private final HashMap<String, FloatOutput> floats = new HashMap<String, FloatOutput>();

    public String[] listBooleans() {
        String[] stra;
        ArrayList<String> strs = Utils.collectIterable(booleans.keySet());
        stra = new String[strs.size()];
        return stra;
    }

    public BooleanOutput getBoolean(String name) {
        return booleans.get(name);
    }

    public String[] listFloats() {
        String[] stra;
        ArrayList<String> strs = Utils.collectIterable(floats.keySet());
        stra = new String[strs.size()];
        return stra;
    }

    public FloatOutput getFloat(String name) {
        return floats.get(name);
    }

    public void addBoolean(String name, BooleanOutput output) {
        if (booleans.containsKey(name)) {
            throw new IllegalArgumentException("Boolean sink already registered: '" + name + "'");
        }
        booleans.put(name, output);
    }

    public BooleanInput addBoolean(String name) {
        BooleanStatus status = new BooleanStatus();
        addBoolean(name, status.asOutput());
        return status.asInput();
    }

    public void addFloat(String name, FloatOutput output) {
        if (floats.containsKey(name)) {
            throw new IllegalArgumentException("Float sink already registered: '" + name + "'");
        }
        floats.put(name, output);
    }

    public FloatInput addFloat(String name) {
        FloatStatus status = new FloatStatus();
        addFloat(name, status.asOutput());
        return status.asInput();
    }
}
