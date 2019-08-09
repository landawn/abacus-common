/*
 * Copyright (C) 2017 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

// TODO: Auto-generated Javadoc
/**
 * The Class Timed.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.9
 */
public class Timed<T> {

    /** The time in millis. */
    private final long timeInMillis;

    /** The value. */
    private final T value;

    /**
     * Instantiates a new timed.
     *
     * @param value the value
     * @param timeInMillis the time in millis
     */
    Timed(T value, long timeInMillis) {
        this.value = value;
        this.timeInMillis = timeInMillis;
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param value the value
     * @return the timed
     */
    public static <T> Timed<T> of(T value) {
        return new Timed<>(value, System.currentTimeMillis());
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param value the value
     * @param timeInMillis the time in millis
     * @return the timed
     */
    public static <T> Timed<T> of(T value, long timeInMillis) {
        return new Timed<>(value, timeInMillis);
    }

    /**
     * Timestamp.
     *
     * @return time in milliseconds.
     */
    public long timestamp() {
        return timeInMillis;
    }

    /**
     * Value.
     *
     * @return the t
     */
    public T value() {
        return value;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return (int) (timeInMillis * 31 + (value == null ? 0 : value.hashCode()));
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Timed) {
            final Timed<?> other = (Timed<?>) obj;

            return this.timeInMillis == other.timeInMillis && N.equals(this.value, other.value);
        }

        return false;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return timeInMillis + ": " + N.toString(value);
    }
}
