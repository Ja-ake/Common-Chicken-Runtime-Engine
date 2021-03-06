/*
 * Copyright 2014-2015 Colby Skeggs
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
package org.team1540.tester;

import ccre.channel.FloatOutput;
import ccre.cluck.Cluck;
import ccre.ctrl.FloatMixing;
import ccre.igneous.Igneous;
import ccre.igneous.IgneousApplication;

/**
 * An example program that simply shares all the motors over the network.
 *
 * Surprisingly useful for an example! Make sure to change which types the
 * motors are.
 *
 * @author skeggsc
 */
public class Tester implements IgneousApplication {

    /**
     * Set up the robot. For the testing robot, this means publishing all the
     * motors.
     */
    public void setupRobot() {
        int base = Igneous.isRoboRIO() ? 0 : 1;
        final FloatOutput[] outs = new FloatOutput[Igneous.isRoboRIO() ? 20 : 10];
        for (int i = base; i < (Igneous.isRoboRIO() ? 20 : 10) + base; i++) {
            Cluck.publish("talon-" + i, outs[i - base] = Igneous.makeTalonMotor(i, false, 0.1f));
        }
        Cluck.publish("talon-all", new FloatOutput() {
            public void set(float value) {
                for (FloatOutput out : outs) {
                    out.set(value);
                }
            }
        });
        for (int i = base; i < (Igneous.isRoboRIO() ? 4 : 8) + base; i++) {
            Cluck.publish("relay-" + i + "-fwd", Igneous.makeForwardRelay(i));
            Cluck.publish("relay-" + i + "-rev", Igneous.makeReverseRelay(i));
        }
        for (int i = base; i < 8 + base; i++) {
            Cluck.publish("solenoid-" + i, Igneous.makeSolenoid(i));
        }
        for (int i = base; i < 4 + base; i++) {
            Cluck.publish("analog-" + i, FloatMixing.createDispatch(Igneous.makeAnalogInput(i, 8), Igneous.globalPeriodic));
        }
        Cluck.publish("input-voltage", FloatMixing.createDispatch(Igneous.getChannelVoltage(Igneous.POWER_CHANNEL_BATTERY), Igneous.globalPeriodic));
        if (Igneous.isRoboRIO()) {
            Cluck.publish("input-current", FloatMixing.createDispatch(Igneous.getChannelCurrent(Igneous.POWER_CHANNEL_BATTERY), Igneous.globalPeriodic));
            Cluck.publish("6v-voltage", FloatMixing.createDispatch(Igneous.getChannelVoltage(Igneous.POWER_CHANNEL_6V), Igneous.globalPeriodic));
            Cluck.publish("6v-current", FloatMixing.createDispatch(Igneous.getChannelCurrent(Igneous.POWER_CHANNEL_6V), Igneous.globalPeriodic));
            Cluck.publish("5v-voltage", FloatMixing.createDispatch(Igneous.getChannelVoltage(Igneous.POWER_CHANNEL_5V), Igneous.globalPeriodic));
            Cluck.publish("5v-current", FloatMixing.createDispatch(Igneous.getChannelCurrent(Igneous.POWER_CHANNEL_5V), Igneous.globalPeriodic));
            Cluck.publish("3.3v-voltage", FloatMixing.createDispatch(Igneous.getChannelVoltage(Igneous.POWER_CHANNEL_3V3), Igneous.globalPeriodic));
            Cluck.publish("3.3v-current", FloatMixing.createDispatch(Igneous.getChannelCurrent(Igneous.POWER_CHANNEL_3V3), Igneous.globalPeriodic));
            for (int i = base; i < 16 + base; i++) {
                Cluck.publish("current-" + i, FloatMixing.createDispatch(Igneous.getPDPChannelCurrent(i), Igneous.globalPeriodic));
            }
            Cluck.publish("compressor", Igneous.usePCMCompressor());
            Cluck.publish("pdp-voltage", FloatMixing.createDispatch(Igneous.getPDPVoltage(), Igneous.globalPeriodic));
        }
    }
}
