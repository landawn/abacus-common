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

/**
 * 
 * @since 0.9
 * 
 * @author Haiyang Li
 */
public class Timed<T> {
    private final long timeInMillis;
    private final T value;

    Timed(T value, long timeInMillis) {
        this.value = value;
        this.timeInMillis = timeInMillis;
    }

    public static <T> Timed<T> of(T value) {
        return new Timed<>(value, System.currentTimeMillis());
    }

    public static <T> Timed<T> of(T value, long timeInMillis) {
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

    @Override
    public String toString() {
        return timeInMillis + ": " + N.toString(value);
    }
}
