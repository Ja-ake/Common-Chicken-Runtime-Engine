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
package ccre.igneous;

import ccre.channel.BooleanInput;
import ccre.channel.BooleanInputPoll;
import ccre.channel.BooleanOutput;
import ccre.channel.EventInput;
import ccre.channel.FloatInputPoll;
import ccre.channel.FloatOutput;
import ccre.channel.SerialIO;
import ccre.ctrl.BooleanMixing;
import ccre.ctrl.ExtendedMotor;
import ccre.ctrl.FloatMixing;
import ccre.ctrl.IJoystick;
import ccre.ctrl.IJoystickWithPOV;
import ccre.ctrl.Ticker;
import ccre.ctrl.binding.CluckControlBinder;
import ccre.ctrl.binding.ControlBindingCreator;
import ccre.ctrl.binding.ControlBindingDataSource;
import ccre.ctrl.binding.ControlBindingDataSourceBuildable;
import ccre.instinct.InstinctModule;
import ccre.log.Logger;

/**
 * The main class to access Igneous channels.
 *
 * @author skeggsc
 */
public class Igneous {

    /**
     * Signifies that the motor should be directly outputted without negation.
     */
    public static final boolean MOTOR_FORWARD = false;
    /**
     * Signifies that the motor should be outputted after negating the value.
     */
    public static final boolean MOTOR_REVERSE = true;
    /**
     * Signifies that no ramping should be applied to this motor.
     */
    public static final float NO_RAMPING = 0.0f;
    /**
     * The battery power channel. Reading this voltage is the only power channel
     * that exists on the cRIO.
     */
    public static final int POWER_CHANNEL_BATTERY = 0;
    /**
     * The 3.3V rail power channel.
     */
    public static final int POWER_CHANNEL_3V3 = 1;
    /**
     * The 5V rail power channel.
     */
    public static final int POWER_CHANNEL_5V = 2;
    /**
     * The 6V rail power channel.
     */
    public static final int POWER_CHANNEL_6V = 3;

    /**
     * The IgneousLauncher providing access to the robot.
     */
    public static final IgneousLauncher launcher = IgneousLauncherHolder.getLauncher();

    /**
     * Joystick 1 on the Driver Station.
     */
    public static final IJoystickWithPOV joystick1 = launcher.getJoystick(1);
    /**
     * Joystick 2 on the Driver Station.
     */
    public static final IJoystickWithPOV joystick2 = launcher.getJoystick(2);
    /**
     * Joystick 3 on the Driver Station.
     */
    public static final IJoystickWithPOV joystick3 = launcher.getJoystick(3);
    /**
     * Joystick 4 on the Driver Station.
     */
    public static final IJoystickWithPOV joystick4 = launcher.getJoystick(4);
    /**
     * Joystick 5 on the Driver Station. Not available on cRIO.
     */
    public static final IJoystickWithPOV joystick5 = launcher.getJoystick(5);
    /**
     * Joystick 6 on the Driver Station. Not available on cRIO.
     */
    public static final IJoystickWithPOV joystick6 = launcher.getJoystick(6);
    /**
     * Produced during every mode if the driver station is attached.
     */
    public static final EventInput globalPeriodic = launcher.getGlobalPeriodic();
    /**
     * Constant time periodic. Should pulse every 10 ms, as accurately as
     * possible.
     */
    public static final EventInput constantPeriodic = new Ticker(10);
    /**
     * Produced when the robot enters autonomous mode.
     */
    public static final EventInput startAuto = launcher.getStartAuto();
    /**
     * Produced during autonomous mode.
     */
    public static final EventInput duringAuto = launcher.getDuringAuto();
    /**
     * Produced when the robot enters teleop mode.
     */
    public static final EventInput startTele = launcher.getStartTele();
    /**
     * Produced during teleop mode.
     */
    public static final EventInput duringTele = launcher.getDuringTele();
    /**
     * Produced when the robot enters testing mode.
     */
    public static final EventInput startTest = launcher.getStartTest();
    /**
     * Produced during testing mode.
     */
    public static final EventInput duringTest = launcher.getDuringTest();
    /**
     * Produced when the robot enters disabled mode.
     */
    public static final EventInput startDisabled = launcher.getStartDisabled();
    /**
     * Produced while the robot is disabled.
     */
    public static final EventInput duringDisabled = launcher.getDuringDisabled();

    /**
     * Get an IJoystick for the specified Kinect virtual joystick. Joysticks on
     * the driver station are accessed through the variables
     * joystick1...joystick4.
     *
     * @param isRightArm If the right arm joystick should be used instead of the
     * left. (6 instead of 5 if you're used to the old system.)
     * @return the IJoystick.
     */
    public static final IJoystick getKinectJoystick(boolean isRightArm) {
        return launcher.getKinectJoystick(isRightArm);
    }

    /**
     * Create a reference to a Jaguar speed controller on the specified PWM port
     * and motor reversal, with a specified ramping rate.
     *
     * If the ramping rate is zero, then no ramping is applied. Don't use this
     * if you don't know what you're doing! Otherwise, the ramping rate is the
     * maximum difference allowed per 10 milliseconds (constantPeriodic). (So a
     * rate of 0.1f means that you need 200 milliseconds to go from -1.0 to
     * 1.0.)
     *
     * @param id the motor port ID, from 1 to 10, inclusive.
     * @param negate MOTOR_FORWARD if the motor direction should be unmodified,
     * MOTOR_REVERSE if the motor direction should be reversed.
     * @param ramping the ramping rate.
     * @return the output that will output to the specified motor.
     * @see #MOTOR_FORWARD
     * @see #MOTOR_REVERSE
     */
    public static FloatOutput makeJaguarMotor(int id, boolean negate, float ramping) {
        FloatOutput motor = launcher.makeMotor(id, IgneousLauncher.JAGUAR);
        FloatOutput ramped = FloatMixing.addRamping(ramping, constantPeriodic, negate ? FloatMixing.negate(motor) : motor);
        FloatMixing.setWhen(startDisabled, ramped, 0.0f);
        return ramped;
    }

    /**
     * Create a reference to a Victor speed controller on the specified PWM port
     * and motor reversal, with a specified ramping rate.
     *
     * If the ramping rate is zero, then no ramping is applied. Don't use this
     * if you don't know what you're doing! Otherwise, the ramping rate is the
     * maximum difference allowed per 10 milliseconds (constantPeriodic). (So a
     * rate of 0.1f means that you need 200 milliseconds to go from -1.0 to
     * 1.0.)
     *
     * @param id the motor port ID, from 1 to 10, inclusive.
     * @param negate MOTOR_FORWARD if the motor direction should be unmodified,
     * MOTOR_REVERSE if the motor direction should be reversed.
     * @param ramping the ramping rate.
     * @return the output that will output to the specified motor.
     * @see #MOTOR_FORWARD
     * @see #MOTOR_REVERSE
     */
    public static FloatOutput makeVictorMotor(int id, boolean negate, float ramping) {
        FloatOutput motor = launcher.makeMotor(id, IgneousLauncher.VICTOR);
        FloatOutput ramped = FloatMixing.addRamping(ramping, constantPeriodic, negate ? FloatMixing.negate(motor) : motor);
        FloatMixing.setWhen(startDisabled, ramped, 0.0f);
        return ramped;
    }

    /**
     * Create a reference to a Talon speed controller on the specified PWM port
     * and motor reversal, with a specified ramping rate.
     *
     * If the ramping rate is zero, then no ramping is applied. Don't use this
     * if you don't know what you're doing! Otherwise, the ramping rate is the
     * maximum difference allowed per 10 milliseconds (constantPeriodic). (So a
     * rate of 0.1f means that you need 200 milliseconds to go from -1.0 to
     * 1.0.)
     *
     * @param id the motor port ID, from 1 to 10, inclusive.
     * @param negate MOTOR_FORWARD if the motor direction should be unmodified,
     * MOTOR_REVERSE if the motor direction should be reversed.
     * @param ramping the ramping rate.
     * @return the output that will output to the specified motor.
     * @see #MOTOR_FORWARD
     * @see #MOTOR_REVERSE
     */
    public static FloatOutput makeTalonMotor(int id, boolean negate, float ramping) {
        FloatOutput motor = launcher.makeMotor(id, IgneousLauncher.TALON);
        FloatOutput ramped = FloatMixing.addRamping(ramping, constantPeriodic, negate ? FloatMixing.negate(motor) : motor);
        FloatMixing.setWhen(startDisabled, ramped, 0.0f);
        return ramped;
    }

    /**
     * **** WARNING ****: THIS API IS NOT YET FINALIZED. Future changes may
     * break your code!
     *
     * Create a reference to a CAN Jaguar speed controller with the specified
     * CAN device number. This may, of course, fail, if the Jaguar cannot be
     * found.
     *
     * @param deviceNumber the device number to connect to.
     * @return the ExtendedMotor representing this output.
     */
    public static ExtendedMotor makeCANJaguar(int deviceNumber) {
        Logger.warning("The CCRE CAN functionality is NOT yet complete and is UNTESTED! Use with your own risk.");
        return launcher.makeCANJaguar(deviceNumber);
    }

    /**
     * **** WARNING ****: THIS API IS NOT YET FINALIZED. Future changes may
     * break your code!
     *
     * Create a reference to a CAN Talon speed controller with the specified CAN
     * device number. This may, of course, fail, if the Talon cannot be found.
     *
     * This is not supported on the cRIO!
     *
     * @param deviceNumber the device number to connect to.
     * @return the ExtendedMotor representing this output.
     */
    public static ExtendedMotor makeCANTalon(int deviceNumber) {
        Logger.warning("The CCRE CAN functionality is NOT yet complete and is UNTESTED! Use with your own risk.");
        return launcher.makeCANTalon(deviceNumber);
    }

    /**
     * Create a reference to a solenoid on the specified port and the default
     * module.
     *
     * @param id the port of the solenoid.
     * @return the output that will control the solenoid.
     */
    public static BooleanOutput makeSolenoid(int id) {
        return launcher.makeSolenoid(launcher.isRoboRIO() ? 0 : 1, id);
    }

    /**
     * Create a reference to a solenoid on the specified port and module.
     *
     * @param module the module of the solenoid (PCM on roboRIO)
     * @param id the port of the solenoid.
     * @return the output that will control the solenoid.
     */
    public static BooleanOutput makeSolenoid(int module, int id) {
        return launcher.makeSolenoid(module, id);
    }

    /**
     * Create a reference to a digital output on the specified port.
     *
     * @param id the port of the digital output.
     * @return the output that will control the digital output.
     */
    public static BooleanOutput makeDigitalOutput(int id) {
        return launcher.makeDigitalOutput(id);
    }

    /**
     * Get a reference to the analog input that reads the current battery
     * voltage, scaled to represent the real battery voltage.
     *
     * @return The current battery voltage.
     */
    public static FloatInputPoll getBatteryVoltage() {
        return launcher.getBatteryVoltage();
    }

    /**
     * Create a reference to an analog input on the specified port.
     *
     * @param id the port number.
     * @return the analog input, reporting in voltage.
     */
    public static FloatInputPoll makeAnalogInput(int id) {
        return launcher.makeAnalogInput(id);
    }

    /**
     * Create a reference to an analog input on the specified port with the
     * specified number of average bits.
     *
     * @param id the port number.
     * @param averageBits the number of averaging bits.
     * @return the analog input, reporting in voltage.
     */
    public static FloatInputPoll makeAnalogInput(int id, int averageBits) {
        return launcher.makeAnalogInput(id, averageBits);
    }

    /**
     * Create a reference to an analog input's raw value on the specified port
     * with the specified number of average bits.
     *
     * @param id the port number.
     * @param averageBits the number of averaging bits.
     * @return the analog input, reporting in uncalibrated units.
     * @deprecated makeAnalogInput should be used directly because it gives more
     * useful volts instead of a raw value.
     */
    @Deprecated
    public static FloatInputPoll makeAnalogInput_ValueBased(int id, int averageBits) {
        return launcher.makeAnalogInput_ValuedBased(id, averageBits);
    }

    /**
     * Create a reference to a digital input on the specified port.
     *
     * @param id the port number.
     * @return the digital input.
     */
    public static BooleanInputPoll makeDigitalInput(int id) {
        return launcher.makeDigitalInput(id);
    }

    /**
     * Create a reference to a digital input on the specified port, as a
     * BooleanInput (not a BooleanInputPoll) that updates by using FPGA
     * interrupts or an equivalent. Warning: many systems have a limit on the
     * maximum number of interrupts available - use sparingly!
     *
     * @param id the port number.
     * @return the digital input.
     */
    public static BooleanInput makeDigitalInputByInterrupt(int id) {
        return launcher.makeDigitalInputByInterrupt(id);
    }

    /**
     * Create a reference to a servo controller for the specified port and
     * minimum and maximum values.
     *
     * @param id the port number.
     * @param minInput the value on the output that should correspond to the
     * servo's minimum position.
     * @param maxInput the value on the output that should correspond to the
     * servo's maximum position.
     * @return the FloatOutput that controls the servo.
     */
    public static FloatOutput makeServo(int id, float minInput, float maxInput) {
        return launcher.makeServo(id, minInput, maxInput);
    }

    /**
     * Create an output that will display the current value on the driver
     * station's LCD.
     *
     * @param prefix the prefix, or label, of the output. this is prepended to
     * the value.
     * @param line the line to display the value on, from 1 to 6.
     * @return the output that will write to the LCD.
     */
    public static FloatOutput makeDSFloatReadout(final String prefix, final int line) {
        return new DSFloatReadout(prefix, line);
    }

    /**
     * Create an output that will display the current value on the driver
     * station's LCD.
     *
     * @param prefix the prefix, or label, of the output. this is prepended to
     * the value.
     * @param line the line to display the value on, from 1 to 6.
     * @return the output that will write to the LCD.
     */
    public static BooleanOutput makeDSBooleanReadout(final String prefix, final int line) {
        return new DSBooleanReadout(prefix, line);
    }

    /**
     * Display the current value of the specified FloatInputPoll on the driver
     * station's LCD, whenever the specified event is triggered.
     *
     * @param prefix the prefix, or label, of the output. this is prepended to
     * the value.
     * @param line the line to display the value on, from 1 to 6.
     * @param value the value to display.
     * @param when when to update the output.
     */
    public static void makeDSFloatReadout(String prefix, int line, FloatInputPoll value, EventInput when) {
        FloatMixing.pumpWhen(when, value, makeDSFloatReadout(prefix, line));
    }

    /**
     * Display the current value of the specified BooleanInputPoll on the driver
     * station's LCD, whenever the specified event is triggered.
     *
     * @param prefix the prefix, or label, of the output. this is prepended to
     * the value.
     * @param line the line to display the value on, from 1 to 6.
     * @param value the value to display.
     * @param when when to update the output.
     */
    public static void makeDSBooleanReadout(String prefix, int line, BooleanInputPoll value, EventInput when) {
        BooleanMixing.pumpWhen(when, value, makeDSBooleanReadout(prefix, line));
    }

    /**
     * Send the specified string to the specified line of the driver station.
     *
     * @param value The string to display.
     * @param line The line number (1-6).
     */
    public static void sendDSUpdate(String value, int line) {
        launcher.sendDSUpdate(value, line);
    }

    /**
     * Get a boolean input that checks if the robot is currently disabled.
     *
     * @return the input.
     */
    public static BooleanInputPoll getIsDisabled() {
        return launcher.getIsDisabled();
    }

    /**
     * Get a boolean input that checks if the robot is currently in autonomous,
     * as opposed to teleop and testing.
     *
     * @return the input.
     */
    public static BooleanInputPoll getIsAutonomous() {
        return launcher.getIsAutonomous();
    }

    /**
     * Get a boolean input that checks if the robot is currently in testing
     * mode, as opposed to teleop and autonomous.
     *
     * @return the input.
     */
    public static BooleanInputPoll getIsTest() {
        return launcher.getIsTest();
    }

    /**
     * Get a boolean input that checks if the robot is currently in teleop mode,
     * as opposed to testing and autonomous.
     *
     * @return the input.
     */
    public static BooleanInputPoll getIsTeleop() {
        return BooleanMixing.invert(BooleanMixing.orBooleans(launcher.getIsTest(), launcher.getIsAutonomous()));
    }

    /**
     * Get a boolean input that checks if the robot is currently connected to
     * the FMS, as opposed to being off the playing field.
     *
     * @return the input.
     */
    public static BooleanInputPoll getIsFMS() {
        return launcher.getIsFMS();
    }

    /**
     * Activate the compressor on the given pressure switch channel and
     * compressor relay channel.
     *
     * @param pressureSwitchChannel the channel of the pressure switch digital
     * input.
     * @param compressorRelayChannel the channel of the compressor's relay.
     */
    public static void useCompressor(int pressureSwitchChannel, int compressorRelayChannel) {
        useCustomCompressor(makeDigitalInput(pressureSwitchChannel), compressorRelayChannel);
    }

    /**
     * Get control of the PCM-attached compressor. This makes sure that the PCM
     * compressor is running the closed loop control.
     *
     * @return a BooleanOutput that can turn closed loop control on and off.
     */
    public static BooleanOutput usePCMCompressor() {
        return launcher.usePCMCompressor();
    }

    /**
     * Reads the current status of the PCM pressure switch.
     *
     * @return the pressure switch status.
     */
    public static BooleanInputPoll getPCMPressureSwitch() {
        return launcher.getPCMPressureSwitch();
    }

    /**
     * Reads the current status of the PCM compressor enable output.
     *
     * @return the compressor enable output.
     */
    public static BooleanInputPoll getPCMCompressorRunning() {
        return launcher.getPCMCompressorRunning();
    }

    /**
     * Reads the current draw of the PCM compressor.
     *
     * @return the current being used by the compressor.
     */
    public static FloatInputPoll getPCMCompressorCurrent() {
        return launcher.getPCMCompressorCurrent();
    }

    /**
     * Reads the current draw of the specified PDP channel.
     *
     * @param channel the channel to monitor
     * @return the current being used by the specified channel.
     */
    public static FloatInputPoll getPDPChannelCurrent(int channel) {
        return launcher.getPDPChannelCurrent(channel);
    }

    /**
     * Reads the voltage of the PDP.
     *
     * @return the voltage being measured by the PDP.
     */
    public static FloatInputPoll getPDPVoltage() {
        return launcher.getPDPVoltage();
    }

    /**
     * Reads the voltage from a specified power reading channel.
     *
     * @param powerChannel the power channel to read from.
     * @return the voltage being measured.
     */
    public static FloatInputPoll getChannelVoltage(int powerChannel) {
        return launcher.getChannelVoltage(powerChannel);
    }

    /**
     * Reads the current from a specified power reading channel.
     *
     * @param powerChannel the power channel to read from.
     * @return the current being measured.
     */
    public static FloatInputPoll getChannelCurrent(int powerChannel) {
        return launcher.getChannelCurrent(powerChannel);
    }

    /**
     * Checks if the specified power reading channel is enabled.
     *
     * @param powerChannel the power channel to read from.
     * @return if the channel is enabled.
     */
    public static BooleanInputPoll getChannelEnabled(int powerChannel) {
        return launcher.getChannelEnabled(powerChannel);
    }

    /**
     * Checks if this platform is a roboRIO.
     *
     * @return if the platform is a roboRIO.
     */
    public static boolean isRoboRIO() {
        return launcher.isRoboRIO();
    }

    /**
     * Activate the compressor on the given pressure switch input and compressor
     * relay channel.
     *
     * @param shouldDisable should the compressor be turned off.
     * @param compressorRelayChannel the channel of the compressor's relay.
     */
    public static void useCustomCompressor(BooleanInputPoll shouldDisable, int compressorRelayChannel) {
        launcher.useCustomCompressor(shouldDisable, compressorRelayChannel);
    }

    /**
     * Create a reference to an Encoder on the specified ports with the
     * specified number of average bits.
     *
     * @param aChannel The alpha-channel for the encoder.
     * @param bChannel The beta-channel for the encoder.
     * @param reverse Should the result of the encoder be negated?
     * @param resetWhen If provided, the Encoder's value will be reset when this
     * event is produced.
     * @return the Encoder, reporting encoder ticks.
     */
    public static FloatInputPoll makeEncoder(int aChannel, int bChannel, boolean reverse, EventInput resetWhen) {
        return launcher.makeEncoder(aChannel, bChannel, reverse, resetWhen);
    }

    /**
     * Create a reference to an Encoder on the specified ports with the
     * specified number of average bits.
     *
     * @param aChannel The alpha-channel for the encoder.
     * @param bChannel The beta-channel for the encoder.
     * @param reverse Should the result of the encoder be negated?
     * @return the Encoder, reporting encoder ticks.
     */
    public static FloatInputPoll makeEncoder(int aChannel, int bChannel, boolean reverse) {
        return launcher.makeEncoder(aChannel, bChannel, reverse, null);
    }

    /**
     * Create a reference to the Forward side of the relay on the specified
     * channel - this side can be turned on and off.
     *
     * @param channel The relay channel.
     * @return the output that will modify the forward side of the channel.
     */
    public static BooleanOutput makeForwardRelay(int channel) {
        return launcher.makeRelayForwardOutput(channel);
    }

    /**
     * Create a reference to the Reverse side of the relay on the specified
     * channel - this side can be turned on and off.
     *
     * @param channel The relay channel.
     * @return the output that will modify the reverse side of the channel.
     */
    public static BooleanOutput makeReverseRelay(int channel) {
        return launcher.makeRelayReverseOutput(channel);
    }

    /**
     * Create a reference to a Gyro on the specified port with the specified
     * sensitivity. This will allow reading the current rotation of the Gyro.
     * This also takes an EventInput, and when this is fired, the Gyro will be
     * reset.
     *
     * Increased sensitivity means a smaller output for the same turn.
     *
     * @param port The Gyro port number.
     * @param sensitivity The sensitivity of the Gyro. This is the number of
     * volts/degree/second sensitivity of the gyro and is used in calculations
     * to allow the code to work with multiple gyros. 0.007 is a good default
     * value.
     * @param evt When to reset the Gyro.
     * @return The reference to the Gyro's current value.
     */
    public static FloatInputPoll makeGyro(int port, double sensitivity, EventInput evt) {
        return launcher.makeGyro(port, sensitivity, evt);
    }

    /**
     * Create a reference to a Gyro on the specified port with the specified
     * sensitivity. This will allow reading the current rotation of the Gyro.
     *
     * Increased sensitivity means a smaller output for the same turn.
     *
     * @param port The Gyro port number.
     * @param sensitivity The sensitivity of the Gyro. This is the number of
     * volts/degree/second sensitivity of the gyro and is used in calculations
     * to allow the code to work with multiple gyros. 0.007 is a good default
     * value.
     * @return The reference to the Gyro's current value.
     */
    public static FloatInputPoll makeGyro(int port, double sensitivity) {
        return launcher.makeGyro(port, sensitivity, null);
    }

    /**
     * Create a reference to a Accelerometer Axis on the specified port, with
     * the specified sensitivity and voltage zero point.
     *
     * @param port The port number to attach to.
     * @param sensitivity The sensitivity of the accelerometer. This varies per
     * model.
     * @param zeropoint The voltage that corresponds to 0 G. This also varies by
     * model.
     * @return The reference to the axis on the Accelerometer.
     * @deprecated This is literally a subtraction and a division.
     */
    @Deprecated
    public static FloatInputPoll makeAccelerometerAxis(int port, double sensitivity, double zeropoint) {
        return launcher.makeAccelerometerAxis(port, sensitivity, zeropoint);
    }

    /**
     * Register the specified InstinctModule as an autonomous mode. Note that
     * registering multiple autonomous modes probably won't work properly.
     *
     * @param module the InstinctModule to register.
     */
    public static void registerAutonomous(InstinctModule module) {
        module.setShouldBeRunning(BooleanMixing.andBooleans(BooleanMixing.invert(getIsDisabled()), getIsAutonomous()));
    }

    /**
     * Open the onboard serial port of the robot.
     *
     * @param baudRate the baud rate of the port.
     * @param deviceName the name of the device the serial port is connected to
     * (used for debugging and the emulator.)
     * @return a SerialIO interface to the port.
     */
    public static SerialIO makeRS232_Onboard(int baudRate, String deviceName) {
        return launcher.makeRS232_Onboard(baudRate, deviceName);
    }

    /**
     * Open the roboRIO's MXP-based serial port.
     *
     * @param baudRate the baud rate of the port.
     * @param deviceName the name of the device the serial port is connected to
     * (used for debugging and the emulator.)
     * @return a SerialIO interface to the port.
     */
    public static SerialIO makeRS232_MXP(int baudRate, String deviceName) {
        return launcher.makeRS232_MXP(baudRate, deviceName);
    }

    /**
     * Open a USB-attached serial port on the roboRIO.
     *
     * @param baudRate the baud rate of the port.
     * @param deviceName the name of the device the serial port is connected to
     * (used for debugging and the emulator.)
     * @return a SerialIO interface to the port.
     */
    public static SerialIO makeRS232_USB(int baudRate, String deviceName) {
        return launcher.makeRS232_USB(baudRate, deviceName);
    }

    Igneous() {
    }

    private static class DSFloatReadout implements FloatOutput {

        private final String prefix;
        private final int line;

        DSFloatReadout(String prefix, int line) {
            this.prefix = prefix;
            this.line = line;
        }

        public void set(float f) {
            sendDSUpdate(prefix + f, line);
        }
    }

    private static class DSBooleanReadout implements BooleanOutput {

        private final String prefix;
        private final int line;

        DSBooleanReadout(String prefix, int line) {
            this.prefix = prefix;
            this.line = line;
        }

        public void set(boolean f) {
            sendDSUpdate(prefix + f, line);
        }
    }

    private static ControlBindingDataSource builtControlSource;

    /**
     * Get a ControlBindingDataSource for the six Joysticks.
     *
     * If on a cRIO, only four Joysticks are available.
     *
     * @return the data source.
     * @see #getControlBindingDataSource(String...) if you want to provide your
     * own names, or use a different number of Joysticks.
     * @see #makeControlBindingCreator(String) if you just want to bind
     * controls.
     */
    public static synchronized ControlBindingDataSource getControlBindingDataSource() {
        if (builtControlSource == null) {
            if (isRoboRIO()) {
                builtControlSource = getControlBindingDataSource("Joystick 1", "Joystick 2", "Joystick 3", "Joystick 4", "Joystick 5", "Joystick 6");
            } else {
                builtControlSource = getControlBindingDataSource("Joystick 1", "Joystick 2", "Joystick 3", "Joystick 4");
            }
        }
        return builtControlSource;
    }

    /**
     * Get a ControlBindingCreator that the user can bind, over Cluck, to any
     * Joystick inputs.
     *
     * If you're running in the emulator, and bypassEmulation is false, then the
     * Emulator will skip over including Joysticks at all and just show you your
     * control bindings directly. Much easier to work with!
     *
     * @param name the name of the module that this creator is for. For example,
     * "Drive Code".
     * @param bypassEmulation if the emulator shouldn't try to emulate control
     * bindings directly.
     * @return the ControlBindingCreator that you can make your controls
     * available over.
     */
    public static ControlBindingCreator makeControlBindingCreator(String name, boolean bypassEmulation) {
        if (!bypassEmulation) {
            ControlBindingCreator out = launcher.tryMakeControlBindingCreator(name);
            if (out != null) {
                return out;
            }
        }
        return CluckControlBinder.makeCreator(name, getControlBindingDataSource(), launcher.getOnInitComplete());
    }

    /**
     * Get a ControlBindingCreator that the user can bind, over Cluck, to any
     * Joystick inputs.
     *
     * If you're running in the emulator, then the Emulator will skip over
     * including Joysticks at all and just show you your control bindings
     * directly. Much easier to work with!
     *
     * @param name the name of the module that this creator is for. For example,
     * "Drive Code".
     * @return the ControlBindingCreator that you can make your controls
     * available over.
     * @see #makeControlBindingCreator(String, boolean) if you want to choose
     * whether or not the emulator emulates control bindings directly.
     */
    public static ControlBindingCreator makeControlBindingCreator(String name) {
        return makeControlBindingCreator(name, false);
    }

    /**
     * This is similar to {@link #getControlBindingDataSource()} but lets you
     * give better names to your Joysticks. For example, you could say
     * <code>Igneous.getControlBindingDataSource("Drive Joystick", "Copilot Joystick");</code>
     *
     * @param names the names of the Joysticks to attach to, in order.
     * @return the generated control binding source.
     */
    public static ControlBindingDataSource getControlBindingDataSource(String... names) {
        ControlBindingDataSourceBuildable ds = new ControlBindingDataSourceBuildable(globalPeriodic);
        for (int i = 0; i < names.length; i++) {
            if (isRoboRIO()) {
                ds.addJoystick(names[i], launcher.getJoystick(i + 1), 12, 6);
            } else {
                ds.addJoystick(names[i], (IJoystick) launcher.getJoystick(i + 1), 12, 6); // don't include POV on cRIO
            }
        }
        return ds;
    }
}
