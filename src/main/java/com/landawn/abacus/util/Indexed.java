/*
 * Copyright (C) 2016 HaiYang Li
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
 * This class represents an indexed value. It extends the AbstractIndexed class.
 * The class is parameterized by the type {@code T} which is the type of the value.
 * The value is associated with an index, which can be used to order or identify the value.
 *
 * @param <T>
 */
public final class Indexed<T> extends AbstractIndexed {

    private final T value;

    Indexed(final long index, final T value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new instance of Indexed with the provided value and index.
     *
     * @param <T>
     * @param value
     * @param index
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Indexed<T> of(final T value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new Indexed<>(index, value);
    }

    /**
     * Creates a new instance of Indexed with the provided value and index.
     *
     * @param <T>
     * @param value
     * @param index
     * @return
     * @throws IllegalArgumentException
     */
    public static <T> Indexed<T> of(final T value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new Indexed<>(index, value);
    }

    public T value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) (index * 31 + (value == null ? 0 : value.hashCode()));
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

        if (obj instanceof Indexed<?> other) {

            return index == other.index && N.equals(value, other.value);
        }

        return false;
    }

    @Override
    public String toString() {
        return "[" + index + "]=" + N.toString(value);
    }
}
