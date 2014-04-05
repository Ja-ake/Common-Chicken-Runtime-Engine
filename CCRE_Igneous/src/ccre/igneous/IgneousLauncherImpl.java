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
package ccre.igneous;

import ccre.chan.*;
import ccre.cluck.CluckGlobals;
import ccre.cluck.tcp.CluckTCPServer;
import ccre.ctrl.*;
import ccre.device.DeviceException;
import ccre.device.DeviceRegistry;
import ccre.event.*;
import ccre.log.*;
import ccre.net.IgneousNetworkProvider;
import ccre.reflect.ReflectionConsole;
import ccre.saver.IgneousStorageProvider;
import ccre.util.LineCollectorOutputStream;
import ccre.workarounds.IgneousThrowablePrinter;
import com.sun.squawk.VM;
import edu.wpi.first.wpilibj.*;
import java.io.OutputStream;

/**
 * The Squawk implementation of the IgneousLauncher interface. Do not use this!
 * This should only be referenced from the MANIFEST.MF file.
 *
 * @see IgneousLauncher
 * @author skeggsc
 */
class IgneousLauncherImpl extends IterativeRobot implements IgneousLauncher {

    /**
     * The robot's core program.
     */
    public final IgneousCore core;
    /**
     * The robot's compressor.
     */
    private CCustomCompressor compressor;
    /**
     * The robot's device registry.
     */
    private DeviceRegistry devTree;

    IgneousLauncherImpl() {
        IgneousNetworkProvider.register();
        IgneousThrowablePrinter.register();
        IgneousStorageProvider.register();
        CluckGlobals.ensureInitializedCore();
        NetworkAutologger.register();
        BootLogger.register();
        FileLogger.register();
        ReflectionConsole.attach();
        String name = VM.getManifestProperty("Igneous-Main");
        if (name == null) {
            throw new RuntimeException("Could not find MANIFEST-specified launchee!");
        }
        if (name.equals("cel")) {
            core = new CelCore();
            return;
        }
        try {
            core = (IgneousCore) Class.forName(name).newInstance();
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not load " + name + ": " + ex);
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not load " + name + ": " + ex);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            throw new RuntimeException("Could not load " + name + ": " + ex);
        }
    }
    // Default events
    /**
     * Produced during every state where the driver station is attached.
     */
    protected Event globalPeriodic = new Event();

    public final void robotInit() {
        //CluckGlobals.setupServer() - No longer helpful on the robot because this port is now used by default.
        new CluckTCPServer(CluckGlobals.getNode(), 443).start();
        core.duringAutonomous = this.duringAutonomous;
        core.duringDisabled = this.duringDisabled;
        core.duringTeleop = this.duringTeleop;
        core.duringTesting = this.duringTesting;
        core.globalPeriodic = this.globalPeriodic;
        core.constantPeriodic = new Ticker(10, true);
        core.robotDisabled = this.robotDisabled;
        core.startedAutonomous = this.startedAutonomous;
        core.startedTeleop = this.startedTeleop;
        core.startedTesting = this.startedTesting;
        core.launcher = this;
        try {
            core.createRobotControl();
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Disabled Init", thr);
            if (thr instanceof RuntimeException) {
                throw (RuntimeException) thr;
            } else if (thr instanceof Error) {
                throw (Error) thr;
            }
            throw new RuntimeException("Critical Code Failure: " + thr.getMessage());
        }
    }
    /**
     * Produced when the robot enters autonomous mode.
     */
    protected Event startedAutonomous = new Event();

    public final void autonomousInit() {
        try {
            Logger.fine("Began autonomous mode");
            startedAutonomous.produce();
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Autonomous Init", thr);
        }
    }
    /**
     * Produced during autonomous mode.
     */
    protected Event duringAutonomous = new Event();
    private int countFails = 0;

    public final void autonomousPeriodic() {
        try {
            if (countFails >= 50) {
                countFails--;
                if (duringAutonomous.produceWithFailureRecovery()) {
                    countFails = 0;
                }
                if (globalPeriodic.produceWithFailureRecovery()) {
                    countFails = 0;
                }
            } else {
                duringAutonomous.produce();
                globalPeriodic.produce();
                if (countFails > 0) {
                    countFails--;
                }
            }
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Autonomous Periodic", thr);
            countFails += 10;
        }
    }
    /**
     * Produced when the robot enters disabled mode.
     */
    protected Event robotDisabled = new Event();

    public final void disabledInit() {
        try {
            Logger.fine("Began disabled mode");
            robotDisabled.produce();
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Disabled Init", thr);
        }
    }
    /**
     * Produced while the robot is disabled.
     */
    protected Event duringDisabled = new Event();

    public final void disabledPeriodic() {
        try {
            if (countFails >= 50) {
                countFails--;
                if (duringDisabled.produceWithFailureRecovery()) {
                    countFails = 0;
                }
                if (globalPeriodic.produceWithFailureRecovery()) {
                    countFails = 0;
                }
            } else {
                duringDisabled.produce();
                globalPeriodic.produce();
                if (countFails > 0) {
                    countFails--;
                }
            }
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Disabled Periodic", thr);
            countFails += 10;
        }
    }
    /**
     * Produced when the robot enters teleop mode.
     */
    protected Event startedTeleop = new Event();

    public final void teleopInit() {
        try {
            Logger.fine("Began teleop mode");
            startedTeleop.produce();
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Teleop Init", thr);
        }
    }
    /**
     * Produced during teleop mode.
     */
    protected Event duringTeleop = new Event();

    public final void teleopPeriodic() {
        try {
            if (countFails >= 50) {
                countFails--;
                if (duringTeleop.produceWithFailureRecovery()) {
                    countFails = 0;
                }
                if (globalPeriodic.produceWithFailureRecovery()) {
                    countFails = 0;
                }
            } else {
                duringTeleop.produce();
                globalPeriodic.produce();
                if (countFails > 0) {
                    countFails--;
                }
            }
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Teleop Periodic", thr);
            countFails += 10;
        }
    }
    /**
     * Produced when the robot enters testing mode.
     */
    protected Event startedTesting = new Event();

    public final void testInit() {
        try {
            Logger.fine("Began testing mode");
            startedTesting.produce();
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Testing Init", thr);
        }
    }
    /**
     * Produced during testing mode.
     */
    protected Event duringTesting = new Event();

    public final void testPeriodic() {
        try {
            if (countFails >= 50) {
                countFails--;
                if (duringTesting.produceWithFailureRecovery()) {
                    countFails = 0;
                }
                if (globalPeriodic.produceWithFailureRecovery()) {
                    countFails = 0;
                }
            } else {
                duringTesting.produce();
                globalPeriodic.produce();
                if (countFails > 0) {
                    countFails--;
                }
            }
        } catch (Throwable thr) {
            Logger.log(LogLevel.SEVERE, "Critical Code Failure in Testing Periodic", thr);
            countFails += 10;
        }
    }

    /**
     * Return a FloatOutput that writes to the specified speed controller.
     *
     * @param spc the speed controller
     * @param negate if the motor direction should be negated. See MOTOR_FORWARD
     * and MOTOR_REVERSE.
     * @return the FloatOutput that writes to the controller.
     */
    static FloatOutput wrapSpeedController(final SpeedController spc, final boolean negate) {
        return new FloatOutput() {
            public void writeValue(float f) {
                if (negate) {
                    spc.set(-f);
                } else {
                    spc.set(f);
                }
            }
        };
    }

    public ISimpleJoystick makeSimpleJoystick(int id) {
        return new CSimpleJoystick(id);
    }

    public IDispatchJoystick makeDispatchJoystick(int id, EventSource source) {
        return new CDispatchJoystick(id, source);
    }

    public FloatOutput makeJaguar(int id, boolean negate) {
        return wrapSpeedController(new Jaguar(id), negate);
    }

    public FloatOutput makeVictor(int id, boolean negate) {
        return wrapSpeedController(new Victor(id), negate);
    }

    public FloatOutput makeTalon(int id, boolean negate) {
        return wrapSpeedController(new Talon(id), negate);
    }

    public BooleanOutput makeSolenoid(int id) {
        final Solenoid sol = new Solenoid(id);
        return new BooleanOutput() {
            public void writeValue(boolean bln) {
                sol.set(bln);
            }
        };
    }

    public BooleanOutput makeDigitalOutput(int id) {
        final DigitalOutput dout = new DigitalOutput(id);
        return new BooleanOutput() {
            public void writeValue(boolean bln) {
                dout.set(bln);
            }
        };
    }

    public FloatInputPoll getBatteryVoltage() {
        return new FloatInputPoll() {
            DriverStation d = DriverStation.getInstance();

            public float readValue() {
                return (float) d.getBatteryVoltage();
            }
        };
    }

    public FloatInputPoll makeAnalogInput(int id, int averageBits) {
        final AnalogChannel chan = new AnalogChannel(id);
        chan.setAverageBits(averageBits);
        return new FloatInputPoll() {
            public float readValue() {
                return (float) chan.getAverageVoltage();
            }
        };
    }

    public FloatInputPoll makeAnalogInput_ValuedBased(int id, int averageBits) {
        final AnalogChannel chan = new AnalogChannel(id);
        chan.setAverageBits(averageBits);
        return new FloatInputPoll() {
            public float readValue() {
                return (float) chan.getAverageValue();
            }
        };
    }

    public BooleanInputPoll makeDigitalInput(int id) {
        final DigitalInput dinput = new DigitalInput(id);
        return new BooleanInputPoll() {
            public boolean readValue() {
                return dinput.get();
            }
        };
    }

    public FloatOutput makeServo(int id, final float minInput, float maxInput) {
        final Servo servo = new Servo(id);
        final float deltaInput = maxInput - minInput;
        return new FloatOutput() {
            public void writeValue(float f) {
                servo.set((f - minInput) / deltaInput);
            }
        };
    }

    public void sendDSUpdate(String value, int lineid) {
        final DriverStationLCD.Line line;
        switch (lineid) {
            case 1:
                line = DriverStationLCD.Line.kUser1;
                break;
            case 2:
                line = DriverStationLCD.Line.kUser2;
                break;
            case 3:
                line = DriverStationLCD.Line.kUser3;
                break;
            case 4:
                line = DriverStationLCD.Line.kUser4;
                break;
            case 5:
                line = DriverStationLCD.Line.kUser5;
                break;
            case 6:
                line = DriverStationLCD.Line.kUser6;
                break;
            default:
                throw new IllegalArgumentException("Bad line number (expected 1-6): " + lineid);
        }
        DriverStationLCD dslcd = DriverStationLCD.getInstance();
        dslcd.println(line, 1, "                    ");
        dslcd.println(line, 1, value);
        dslcd.updateLCD();
    }

    public BooleanInputPoll getIsDisabled() {
        return new BooleanInputPoll() {
            public boolean readValue() {
                return DriverStation.getInstance().isDisabled();
            }
        };
    }

    public BooleanInputPoll getIsAutonomous() {
        return new BooleanInputPoll() {
            public boolean readValue() {
                DriverStation is = DriverStation.getInstance();
                return is.isAutonomous() && !is.isTest();
            }
        };
    }

    public BooleanInputPoll getIsTest() {
        return new BooleanInputPoll() {
            public boolean readValue() {
                return DriverStation.getInstance().isTest();
            }
        };
    }

    public void useCustomCompressor(BooleanInputPoll shouldDisable, int compressorRelayChannel) {
        if (compressor == null) {
            compressor = new CCustomCompressor(shouldDisable, compressorRelayChannel);
            compressor.start();
        } else {
            throw new IllegalStateException("Compressor already started!");
        }
    }

    public FloatInputPoll makeEncoder(int aChannel, int bChannel, boolean reverse, EventSource resetWhen) {
        final Encoder enc = new Encoder(aChannel, bChannel, reverse);
        enc.start();
        if (resetWhen != null) {
            resetWhen.addListener(new EventConsumer() {
                public void eventFired() {
                    enc.reset();
                }
            });
        }
        return new FloatInputPoll() {
            public float readValue() {
                return enc.get();
            }
        };
    }

    public BooleanOutput makeRelayForwardOutput(int channel) {
        final Relay r = new Relay(channel, Relay.Direction.kForward);
        return new BooleanOutput() {
            public void writeValue(boolean bln) {
                r.set(bln ? Relay.Value.kOn : Relay.Value.kOff);
            }
        };
    }

    public BooleanOutput makeRelayReverseOutput(int channel) {
        final Relay r = new Relay(channel, Relay.Direction.kReverse);
        return new BooleanOutput() {
            public void writeValue(boolean bln) {
                r.set(bln ? Relay.Value.kOn : Relay.Value.kOff);
            }
        };
    }

    public FloatInputPoll makeGyro(int port, double sensitivity, EventSource evt) {
        final Gyro g = new Gyro(port);
        g.setSensitivity(sensitivity);
        if (evt != null) {
            evt.addListener(new EventConsumer() {
                public void eventFired() {
                    g.reset();
                }
            });
        }
        return new FloatInputPoll() {
            public float readValue() {
                return (float) g.getAngle();
            }
        };
    }

    public FloatInputPoll makeAccelerometerAxis(int port, double sensitivity, double zeropoint) {
        final Accelerometer a = new Accelerometer(port);
        a.setSensitivity(sensitivity);
        a.setZero(zeropoint);
        return new FloatInputPoll() {
            public float readValue() {
                return (float) a.getAcceleration();
            }
        };
    }

    public DeviceRegistry getDeviceRegistry() throws DeviceException {
        if (devTree == null) {
            devTree = new DeviceRegistry();
            devTree.putSimple("modes/auto/init", startedAutonomous, EventSource.class);
            devTree.putSimple("modes/teleop/init", startedTeleop, EventSource.class);
            devTree.putSimple("modes/test/init", startedTesting, EventSource.class);
            devTree.putSimple("modes/disabled/init", robotDisabled, EventSource.class);
            devTree.putSimple("modes/auto/during", duringAutonomous, EventSource.class);
            devTree.putSimple("modes/teleop/during", duringTeleop, EventSource.class);
            devTree.putSimple("modes/test/during", duringTesting, EventSource.class);
            devTree.putSimple("modes/disabled/during", duringDisabled, EventSource.class);
            devTree.putSimple("modes/always", globalPeriodic, EventSource.class);
            devTree.putSimple("modes/constant", core.constantPeriodic, EventSource.class);
            final DriverStation ds = DriverStation.getInstance();
            for (int joy = 1; joy <= 4; joy++) {
                final int cJoy = joy;
                for (int axis = 1; axis <= 6; axis++) {
                    final int cAxis = axis;
                    devTree.putSimple("joysticks/" + joy + "/axis" + axis, new FloatInputPoll() {
                        public float readValue() {
                            return (float) ds.getStickAxis(cJoy, cAxis);
                        }
                    }, FloatInputPoll.class);
                }
                for (int button = 1; button <= 12; button++) {
                    final int cBtn = button;
                    devTree.putSimple("joysticks/" + joy + "/button" + button, new BooleanInputPoll() {
                        public boolean readValue() {
                            return ((1 << (cBtn - 1)) & m_ds.getStickButtons(cJoy)) != 0;
                        }
                    }, FloatInputPoll.class);
                }
            }
            for (int pwm = 1; pwm <= 10; pwm++) {
                devTree.putHandle("pwms/victor" + pwm, new PWMHandle(pwm, PWMHandle.VICTOR));
                devTree.putHandle("pwms/talon" + pwm, new PWMHandle(pwm, PWMHandle.TALON));
                devTree.putHandle("pwms/jaguar" + pwm, new PWMHandle(pwm, PWMHandle.JAGUAR));
                devTree.putHandle("pwms/servo" + pwm, new PWMHandle(pwm, PWMHandle.SERVO));
            }
            for (int sol = 1; sol <= 8; sol++) {
                devTree.putHandle("pneumatics/solen" + sol, new SolenoidHandle(sol));
            }
            final BooleanStatus enableCompressor = new BooleanStatus();
            devTree.putSimple("pneumatics/compressorConf", new LineCollectorOutputStream() {
                protected void collect(String string) {
                    int ii = string.indexOf(' ');
                    if (ii == -1) {
                        int portno = Integer.parseInt(string);
                        useCustomCompressor(enableCompressor, portno);
                    } else {
                        int portno = Integer.parseInt(string.substring(0, ii));
                        int extno = Integer.parseInt(string.substring(ii + 1));
                        enableCompressor.writeValue(true);
                        useCustomCompressor(Mixing.andBooleans(enableCompressor, makeDigitalInput(extno)), portno);
                    }
                }
            }, OutputStream.class);
            devTree.putSimple("pneumatics/compressorEnable", enableCompressor, BooleanOutput.class);
            devTree.putSimple("pneumatics/compressorEnabled", enableCompressor, BooleanInput.class);
            for (int dgt = 1; dgt <= 14; dgt++) {
                devTree.putHandle("gpios/out" + dgt, new GPOHandle(dgt));
                devTree.putHandle("gpios/in" + dgt, new GPIHandle(dgt));
            }
            // TODO: Implement encoders, Gyros, accelerometers.
            //devTree.putHandle("gpios/encoder", null);
            for (int alg = 1; alg <= 8; alg++) {
                devTree.putHandle("analogs/in" + alg, new AnalogInputHandle(alg));
            }
            for (int lcd = 1; lcd <= 6; lcd++) {
                final DriverStationLCD.Line l;
                switch (lcd) {
                    case 1:
                        l = DriverStationLCD.Line.kUser1;
                        break;
                    case 2:
                        l = DriverStationLCD.Line.kUser2;
                        break;
                    case 3:
                        l = DriverStationLCD.Line.kUser3;
                        break;
                    case 4:
                        l = DriverStationLCD.Line.kUser4;
                        break;
                    case 5:
                        l = DriverStationLCD.Line.kUser5;
                        break;
                    case 6:
                        l = DriverStationLCD.Line.kUser6;
                        break;
                    default:
                        throw new RuntimeException("Wait, what?");
                }
                devTree.putSimple("dslcd/line" + lcd, new LineCollectorOutputStream() {
                    protected void collect(String string) {
                        DriverStationLCD lcd = DriverStationLCD.getInstance();
                        lcd.println(l, 1, string);
                        lcd.updateLCD();
                    }
                }, OutputStream.class);
            }
            for (int rel = 1; rel <= 8; rel++) {
                devTree.putHandle("relays/fwd" + rel, new RelayHandle(rel, Relay.Direction.kForward));
                devTree.putHandle("relays/rev" + rel, new RelayHandle(rel, Relay.Direction.kReverse));
            }
        }
        return devTree;
    }
}
