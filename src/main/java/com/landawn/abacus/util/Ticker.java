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
 * Note: Copied from Google Guava under Apache License v2.0
 * <br />
 * <br />
 *
 * A time source; returns a time value representing the number of nanoseconds elapsed since some
 * fixed but arbitrary point in time. Note that most users should use {@link Stopwatch} instead of
 * interacting with this class directly.
 *
 * <p><b>Warning:</b> this interface can only be used to measure elapsed time, not wall time.
 *
 * @author Kevin Bourrillion
 *     source-compatible</a> since 9.0)
 */
public abstract class Ticker {
    /**
     * Constructor for use by subclasses.
     */
    protected Ticker() {
    }

    /**
     * Returns the number of nanoseconds elapsed since this ticker's fixed point of reference.
     *
     * @return
     */
    // TODO(kak): Consider removing this
    public abstract long read();

    /**
     * A ticker that reads the current time using {@link System#nanoTime}.
     *
     * @return
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
