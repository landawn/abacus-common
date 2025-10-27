/*
 * Copyright (C) 2011 The Guava Authors
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

/**
 * A time source that provides access to the current value of some underlying clock.
 * The primary purpose of this abstraction is to facilitate testing of time-dependent
 * code, where a controllable time source can be substituted for the system clock.
 * 
 * <p>This class is particularly useful when used with {@link Stopwatch} to measure
 * elapsed time. By default, it uses {@link System#nanoTime()}, but custom implementations
 * can provide alternative time sources for testing or special requirements.</p>
 * 
 * <p><b>Note:</b> This interface can only be used to measure elapsed time, not wall time.
 * The values returned by {@link #read()} have no absolute meaning and can only be
 * interpreted relative to other values returned by the same ticker.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Using the system ticker
 * Ticker ticker = Ticker.systemTicker();
 * long start = ticker.read();
 * // ... do some work ...
 * long elapsed = ticker.read() - start;
 * 
 * // Custom ticker for testing
 * Ticker testTicker = new Ticker() {
 *     private long time = 0;
 *     public long read() { return time += 1000000; } // advances by 1ms each call
 * };
 * }</pre>
 *
 * @author Kevin Bourrillion
 * @see Stopwatch
 * @see System#nanoTime()
 */
public abstract class Ticker {
    /**
     * Constructor for use by subclasses.
     * 
     * <p>This constructor is protected to ensure that Ticker instances are created
     * through factory methods or by extending this class.</p>
     */
    protected Ticker() {
    }

    /**
     * Returns the number of nanoseconds elapsed since this ticker's fixed but arbitrary
     * point of reference.
     * 
     * <p>The value returned has no absolute meaning and can only be interpreted as
     * relative to another timestamp returned by this same ticker. The difference between
     * two readings from the same ticker instance represents elapsed time in nanoseconds.</p>
     * 
     * <p><b>Implementation note:</b> Subclasses must ensure that the values returned are
     * monotonically non-decreasing (i.e., later calls never return a smaller value than
     * earlier calls).</p>
     *
     * @return the current reading of the ticker, in nanoseconds
     */
    // TODO(kak): Consider removing this
    public abstract long read();

    /**
     * Returns a ticker that reads the current time using {@link System#nanoTime}.
     * 
     * <p>This is the default ticker implementation and is suitable for most use cases
     * where actual elapsed time measurement is needed. The returned ticker is a singleton
     * instance that can be safely shared across multiple threads.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Ticker ticker = Ticker.systemTicker();
     * long startTime = ticker.read();
     * // perform operation
     * long elapsedNanos = ticker.read() - startTime;
     * System.out.println("Operation took " + elapsedNanos + " nanoseconds");
     * }</pre>
     *
     * @return a ticker that uses {@link System#nanoTime} as its time source
     */
    public static Ticker systemTicker() {
        return SYSTEM_TICKER;
    }

    private static final Ticker SYSTEM_TICKER = new Ticker() {
        @Override
        public long read() {
            return System.nanoTime();
        }
    };
}
