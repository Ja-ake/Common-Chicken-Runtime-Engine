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
package ccre.workarounds;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import ccre.log.Logger;

/**
 * A provider that can print out a Throwable to a given PrintStream.
 *
 * This is only needed because Squawk doesn't have a
 * Throwable.printStackTrace(PrintStream) method, so a different provider is
 * needed for Squawk.
 *
 * Also provides {@link #getMethodCaller(int)} because that's a similar idea.
 *
 * @author skeggsc
 */
public abstract class ThrowablePrinter {

    /**
     * The current ThrowablePrinter.
     */
    private static ThrowablePrinter provider;

    /**
     * Set the active ThrowablePrinter for the system.
     *
     * @param provider The ThrowablePrinter to use.
     * @throws IllegalStateException if a provider is already registered.
     */
    static synchronized void setProvider(ThrowablePrinter provider) throws IllegalStateException {
        if (ThrowablePrinter.provider != null) {
            throw new IllegalStateException("Provider already registered!");
        }
        ThrowablePrinter.provider = provider;
    }

    /**
     * Ensure that there is an available provider. At least, there will be a
     * fake throwable printer that can't actually print the exception traceback.
     */
    public static synchronized void initProvider() {
        if (provider == null) {
            try {
                setProvider((ThrowablePrinter) Class.forName("ccre.workarounds.DefaultThrowablePrinter").newInstance());
            } catch (Throwable thr) {
                setProvider(new ThrowablePrinter() {
                    @Override
                    public void send(Throwable thr2, PrintStream pstr) {
                        pstr.println(thr2);
                    }

                    @Override
                    public CallerInfo findMethodCaller(int index) {
                        return null;
                    }
                });
                Logger.warning("No ThrowablePrinter provider!", thr);
            }
        }
    }

    /**
     * Print the specified Throwable to the specified PrintStream.
     *
     * @param thr the throwable to print.
     * @param pstr the PrintStream to write to.
     */
    public static void printThrowable(Throwable thr, PrintStream pstr) {
        if (thr == null || pstr == null) {
            throw new NullPointerException();
        }
        initProvider();
        provider.send(thr, pstr);
    }

    /**
     * Convert the specified Throwable to a String that contains what would have
     * been printed by printThrowable.
     *
     * Printing this value is equivalent to just calling printThrowable
     * originally.
     *
     * @param thr the throwable to print.
     * @return the String version of the throwable, including the traceback, or
     * null if the throwable was null.
     */
    public static String toStringThrowable(Throwable thr) {
        if (thr == null) {
            return null;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        printThrowable(thr, new PrintStream(out));
        return out.toString();
    }

    /**
     * Get diagnostic information for someone in the call stack of this method,
     * at a given index.
     *
     * Index 0 is the caller of this method; 1 is the caller of that method,
     * etc.
     *
     * This should contain, at the very least, the class, but should also
     * contain the method, source file, and line number if possible.
     *
     * @param index which frame to report.
     * @return a CallerInfo for the specified caller, or null.
     */
    public static CallerInfo getMethodCaller(int index) {
        initProvider();
        return index == -1 ? null : provider.findMethodCaller(index + 1);
    }

    /**
     * Get diagnostic information for someone in the call stack of this method,
     * at a given index.
     *
     * Index 0 is the caller of this method; 1 is the caller of that method,
     * etc.
     *
     * This should contain, at the very least, the class, but should also
     * contain the method, source file, and line number if possible.
     *
     * @param index which frame to report.
     * @return a CallerInfo for the specified caller, or null.
     */
    public abstract CallerInfo findMethodCaller(int index);

    /**
     * Send the specified Throwable to the specified PrintStream.
     *
     * @param thr the throwable
     * @param pstr the PrintStream.
     */
    public abstract void send(Throwable thr, PrintStream pstr);
}
