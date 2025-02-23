/*
 * Copyright (C) 2017 HaiYang Li
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
 *
 * @param <T>
 */
@com.landawn.abacus.annotation.Immutable
public final class Timed<T> implements Immutable {

    private final long timeInMillis;

    private final T value;

    Timed(final T value, final long timeInMillis) {
        this.value = value;
        this.timeInMillis = timeInMillis;
    }

    /**
     *
     * @param <T>
     * @param value
     * @return
     */
    public static <T> Timed<T> of(final T value) {
        return new Timed<>(value, System.currentTimeMillis());
    }

    /**
     *
     * @param <T>
     * @param value
     * @param timeInMillis
     * @return
     */
    public static <T> Timed<T> of(final T value, final long timeInMillis) {
        return new Timed<>(value, timeInMillis);
    }

    /**
     *
     * @return time in milliseconds.
     */
    public long timestamp() {
        return timeInMillis;
    }

    public T value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) (timeInMillis * 31 + (value == null ? 0 : value.hashCode()));
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Timed<?> other) {
            return timeInMillis == other.timeInMillis && N.equals(value, other.value);
        }

        return false;
    }

    @Override
    public String toString() {
        return timeInMillis + ": " + N.toString(value);
    }
}
