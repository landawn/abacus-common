/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.landawn.abacus.util;

import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * <p>Note: It's copied from Google Guava under Apache License 2.0 and may be modified.</p>
 *
 * An object that measures elapsed time in nanoseconds. It is useful to measure elapsed time using
 * this class instead of direct calls to {@link System#nanoTime} for a few reasons:
 *
 * <ul>
 *   <li>An alternate time source can be substituted, for testing or performance reasons.
 *   <li>As documented by {@code nanoTime}, the value returned has no absolute meaning, and can only
 *       be interpreted as relative to another timestamp returned by {@code nanoTime} at a different
 *       time. {@code Stopwatch} is a more effective abstraction because it exposes only these
 *       relative values, not the absolute ones.
 * </ul>
 *
 * <p><b>Basic usage:</b></p>
 * <pre>{@code
 * Stopwatch stopwatch = Stopwatch.createStarted();
 * doSomething();
 * stopwatch.stop(); // optional
 *
 * Duration duration = stopwatch.elapsed();
 *
 * log.info("time: " + stopwatch); // formatted string like "12.3 ms"
 * }</pre>
 *
 * <p>Stopwatch methods are not idempotent; it is an error to start or stop a stopwatch that is
 * already in the desired state.
 *
 * <p>When testing code that uses this class, use {@link #createUnstarted(Ticker)} or {@link
 * #createStarted(Ticker)} to supply a fake or mock ticker. This allows you to simulate any valid
 * behavior of the stopwatch.
 *
 * <p><b>Note:</b> This class is not thread-safe.
 *
 * <p><b>Warning for Android users:</b> a stopwatch with default behavior may not continue to keep
 * time while the device is asleep. Instead, create one like this:
 *
 * <pre>{@code
 * Stopwatch.createStarted(
 *      new Ticker() {
 *        public long read() {
 *          return android.os.SystemClock.elapsedRealtimeNanos();
 *        }
 *      });
 * }</pre>
 *
 * @author Kevin Bourrillion
 */
public final class Stopwatch {

    private final Ticker ticker;

    private boolean isRunning;

    private long elapsedNanos;

    private long startTick;

    /**
     * Creates (but does not start) a new stopwatch using {@link System#nanoTime} as its time source.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stopwatch stopwatch = Stopwatch.createUnstarted();
     * stopwatch.start();
     * // ... do something ...
     * stopwatch.stop();
     * }</pre>
     *
     * @return a new stopwatch instance that is not running
     */
    public static Stopwatch createUnstarted() {
        return new Stopwatch();
    }

    /**
     * Creates (but does not start) a new stopwatch, using the specified time source.
     * This is useful for testing or when you need to use an alternative time source.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Ticker mockTicker = new Ticker() {
     *     private long time = 0;
     *     public long read() { return time += 1000000; } // Advance 1ms each call
     * };
     * Stopwatch stopwatch = Stopwatch.createUnstarted(mockTicker);
     * }</pre>
     *
     * @param ticker the time source to use for measuring elapsed time
     * @return a new stopwatch instance that is not running
     */
    public static Stopwatch createUnstarted(final Ticker ticker) {
        return new Stopwatch(ticker);
    }

    /**
     * Creates (and starts) a new stopwatch using {@link System#nanoTime} as its time source.
     * This is a convenience method equivalent to calling {@code createUnstarted().start()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stopwatch stopwatch = Stopwatch.createStarted();
     * doSomething();
     * long elapsed = stopwatch.elapsed(TimeUnit.MILLISECONDS);
     * }</pre>
     *
     * @return a new stopwatch instance that is already running
     */
    public static Stopwatch createStarted() {
        return new Stopwatch().start();
    }

    /**
     * Creates (and starts) a new stopwatch, using the specified time source.
     * This is a convenience method equivalent to calling {@code createUnstarted(ticker).start()}.
     * 
     * <p><b>Example usage for Android:</b></p>
     * <pre>{@code
     * Stopwatch stopwatch = Stopwatch.createStarted(
     *     new Ticker() {
     *         public long read() {
     *             return android.os.SystemClock.elapsedRealtimeNanos();
     *         }
     *     });
     * }</pre>
     *
     * @param ticker the time source to use for measuring elapsed time
     * @return a new stopwatch instance that is already running
     */
    public static Stopwatch createStarted(final Ticker ticker) {
        return new Stopwatch(ticker).start();
    }

    Stopwatch() {
        ticker = Ticker.systemTicker();
    }

    Stopwatch(final Ticker ticker) {
        this.ticker = N.checkArgNotNull(ticker, "ticker");
    }

    /**
     * Returns {@code true} if {@link #start()} has been called on this stopwatch, and {@link #stop()}
     * has not been called since the last call to {@code start()}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (stopwatch.isRunning()) {
     *     stopwatch.stop();
     * }
     * }</pre>
     *
     * @return {@code true} if the stopwatch is currently running, {@code false} otherwise
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Starts the stopwatch. Time measurement begins with this call.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stopwatch stopwatch = Stopwatch.createUnstarted();
     * stopwatch.start();
     * // ... timed operation ...
     * stopwatch.stop();
     * }</pre>
     *
     * @return this {@code Stopwatch} instance for method chaining
     * @throws IllegalStateException if the stopwatch is already running
     * @see #stop()
     * @see #isRunning()
     */
    public Stopwatch start() {
        N.checkState(!isRunning, "This stopwatch is already running.");
        isRunning = true;
        startTick = ticker.read();
        return this;
    }

    /**
     * Stops the stopwatch. Future reads will return the fixed duration that had elapsed up to this
     * point. Calling {@code elapsed()} after stopping will always return the same duration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * stopwatch.stop();
     * Duration duration = stopwatch.elapsed(); // Time when stopped
     * Thread.sleep(1000);
     * Duration sameDuration = stopwatch.elapsed(); // Still the same time
     * }</pre>
     *
     * @return this {@code Stopwatch} instance for method chaining
     * @throws IllegalStateException if the stopwatch is already stopped
     * @see #start()
     * @see #isRunning()
     */
    public Stopwatch stop() {
        final long tick = ticker.read();
        N.checkState(isRunning, "This stopwatch is already stopped.");
        isRunning = false;
        elapsedNanos += tick - startTick;
        return this;
    }

    /**
     * Sets the elapsed time for this stopwatch to zero, and places it in a stopped state.
     * This is useful for reusing a stopwatch instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stopwatch stopwatch = Stopwatch.createStarted();
     * doFirstOperation();
     * System.out.println("First: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
     *
     * stopwatch.reset().start();
     * doSecondOperation();
     * System.out.println("Second: " + stopwatch.elapsed(TimeUnit.MILLISECONDS));
     * }</pre>
     *
     * @return this {@code Stopwatch} instance for method chaining
     * @see #start()
     */
    public Stopwatch reset() {
        elapsedNanos = 0;
        isRunning = false;
        return this;
    }

    private long elapsedNanos() {
        return isRunning ? ticker.read() - startTick + elapsedNanos : elapsedNanos;
    }

    /**
     * Returns the current elapsed time shown on this stopwatch, expressed in the desired time unit,
     * with any fraction rounded down. This method can be called whether the stopwatch is running or stopped.
     *
     * <p><b>Note:</b> the overhead of measurement can be more than a microsecond, so it is generally
     * not useful to specify {@link TimeUnit#NANOSECONDS} precision here.</p>
     *
     * <p>It is generally not a good idea to use an ambiguous, unitless {@code long} to represent
     * elapsed time. Therefore, we recommend using {@link #elapsed()} instead, which returns a
     * strongly-typed {@link Duration} instance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
     * long seconds = stopwatch.elapsed(TimeUnit.SECONDS);
     * }</pre>
     *
     * @param desiredUnit the unit of time to express the elapsed time in
     * @return the elapsed time in the specified unit, rounded down
     * @see #elapsed()
     */
    public long elapsed(final TimeUnit desiredUnit) {
        return desiredUnit.convert(elapsedNanos(), NANOSECONDS);
    }

    /**
     * Returns the current elapsed time shown on this stopwatch as a {@link Duration}.
     * Unlike {@link #elapsed(TimeUnit)}, this method returns a strongly-typed {@code Duration}
     * instance that preserves nanosecond precision.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Duration elapsed = stopwatch.elapsed();
     * System.out.println("Elapsed: " + elapsed.toMillis() + " ms");
     * 
     * // Can be used with Java 8+ time APIs
     * if (elapsed.compareTo(Duration.ofSeconds(5)) > 0) {
     *     System.out.println("Operation took more than 5 seconds");
     * }
     * }</pre>
     *
     * @return a {@code Duration} representing the elapsed time
     */
    public Duration elapsed() {
        return Duration.ofNanos(elapsedNanos());
    }

    /**
     * Returns a string representation of the current elapsed time.
     * The string uses the most appropriate unit and shows up to 4 significant figures.
     * 
     * <p>Example outputs:</p>
     * <ul>
     *   <li>{@code "38.0 ns"} for 38 nanoseconds</li>
     *   <li>{@code "1.234 μs"} for 1234 nanoseconds</li>
     *   <li>{@code "5.678 ms"} for 5678123 nanoseconds</li>
     *   <li>{@code "1.234 s"} for 1.234 seconds</li>
     *   <li>{@code "2.500 min"} for 150 seconds</li>
     *   <li>{@code "1.500 h"} for 90 minutes</li>
     *   <li>{@code "2.000 d"} for 48 hours</li>
     * </ul>
     *
     * @return a string representation of the elapsed time with appropriate unit
     */
    @Override
    public String toString() {
        final long nanos = elapsedNanos();

        final TimeUnit unit = chooseUnit(nanos);
        final double value = (double) nanos / NANOSECONDS.convert(1, unit);

        // Too bad this functionality is not exposed as a regular method call
        return formatCompact4Digits(value) + " " + abbreviate(unit);
    }

    /**
     * Formats a double value to 4 significant figures using scientific notation when appropriate.
     * This method uses {@code "%.4g"} format which automatically chooses between fixed-point
     * and exponential notation.
     * 
     * <p>Example outputs:</p>
     * <ul>
     *   <li>{@code "1.234"} for 1.234</li>
     *   <li>{@code "1234"} for 1234.0</li>
     *   <li>{@code "1.234e+06"} for 1234000.0</li>
     *   <li>{@code "0.001234"} for 0.001234</li>
     * </ul>
     *
     * @param value the value to format
     * @return the formatted string with up to 4 significant figures
     */
    static String formatCompact4Digits(final double value) {
        return String.format(Locale.ROOT, "%.4g", value);
    }

    /**
     * Chooses the most appropriate time unit for displaying the given nanoseconds value.
     * The unit is selected to ensure the value is at least 1 in that unit, preferring
     * larger units when possible for better readability.
     *
     * <p>Selection logic:</p>
     * <ul>
     *   <li>DAYS if ≥ 1 day</li>
     *   <li>HOURS if ≥ 1 hour</li>
     *   <li>MINUTES if ≥ 1 minute</li>
     *   <li>SECONDS if ≥ 1 second</li>
     *   <li>MILLISECONDS if ≥ 1 millisecond</li>
     *   <li>MICROSECONDS if ≥ 1 microsecond</li>
     *   <li>NANOSECONDS otherwise</li>
     * </ul>
     *
     * @param nanos the elapsed time in nanoseconds
     * @return the most appropriate {@code TimeUnit} for displaying the given duration
     */
    private static TimeUnit chooseUnit(final long nanos) {
        if (DAYS.convert(nanos, NANOSECONDS) > 0) {
            return DAYS;
        }
        if (HOURS.convert(nanos, NANOSECONDS) > 0) {
            return HOURS;
        }
        if (MINUTES.convert(nanos, NANOSECONDS) > 0) {
            return MINUTES;
        }
        if (SECONDS.convert(nanos, NANOSECONDS) > 0) {
            return SECONDS;
        }
        if (MILLISECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MILLISECONDS;
        }
        if (MICROSECONDS.convert(nanos, NANOSECONDS) > 0) {
            return MICROSECONDS;
        }
        return NANOSECONDS;
    }

    /**
     * Returns the abbreviated string representation for the given time unit.
     * This is used for formatting the stopwatch's {@code toString()} output.
     *
     * <p>Abbreviations:</p>
     * <ul>
     *   <li>NANOSECONDS → "ns"</li>
     *   <li>MICROSECONDS → "μs"</li>
     *   <li>MILLISECONDS → "ms"</li>
     *   <li>SECONDS → "s"</li>
     *   <li>MINUTES → "min"</li>
     *   <li>HOURS → "h"</li>
     *   <li>DAYS → "d"</li>
     * </ul>
     *
     * @param unit the time unit to abbreviate
     * @return the abbreviated string representation of the time unit
     * @throws AssertionError if the unit is not one of the standard TimeUnit values
     */
    private static String abbreviate(final TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return "ns";
            case MICROSECONDS:
                //noinspection UnnecessaryUnicodeEscape
                return "\u03bcs"; // μs  // NOSONAR
            case MILLISECONDS:
                return "ms";
            case SECONDS:
                return "s";
            case MINUTES:
                return "min";
            case HOURS:
                return "h";
            case DAYS:
                return "d";
            default:
                throw new AssertionError();
        }
    }
}
