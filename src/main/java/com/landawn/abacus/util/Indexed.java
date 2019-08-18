/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.Iterator;

// TODO: Auto-generated Javadoc
/**
 * The Class Indexed.
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
public final class Indexed<T> extends AbstractIndexed {

    /** The value. */
    private final T value;

    /**
     * Instantiates a new indexed.
     *
     * @param index
     * @param value
     */
    Indexed(long index, T value) {
        super(index);
        this.value = value;
    }

    /**
     * Of.
     *
     * @param <T>
     * @param value
     * @param index
     * @return
     */
    public static <T> Indexed<T> of(T value, int index) {
        N.checkArgNotNegative(index, "index");

        return new Indexed<>(index, value);
    }

    /**
     * Of.
     *
     * @param <T>
     * @param value
     * @param index
     * @return
     */
    public static <T> Indexed<T> of(T value, long index) {
        N.checkArgNotNegative(index, "index");

        return new Indexed<>(index, value);
    }

    /**
     * Iterate.
     *
     * @param <T>
     * @param iter
     * @return
     */
    public static <T> ObjIterator<Indexed<T>> iterate(final Iterator<? extends T> iter) {
        return iterate(iter, 0);
    }

    /**
     * Iterate.
     *
     * @param <T>
     * @param iter
     * @param startIndex
     * @return
     */
    public static <T> ObjIterator<Indexed<T>> iterate(final Iterator<? extends T> iter, final int startIndex) {
        N.checkArgNotNegative(startIndex, "startIndex");

        return iterate(iter, (long) startIndex);
    }

    /**
     * Iterate.
     *
     * @param <T>
     * @param iter
     * @param startIndex
     * @return
     */
    public static <T> ObjIterator<Indexed<T>> iterate(final Iterator<? extends T> iter, final long startIndex) {
        N.checkArgNotNegative(startIndex, "startIndex");

        return new ObjIterator<Indexed<T>>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public Indexed<T> next() {
                return Indexed.of((T) iter.next(), idx++);
            }
        };
    }

    /**
     * Value.
     *
     * @return
     */
    public T value() {
        return value;
    }

    /**
     * Hash code.
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (int) (index * 31 + (value == null ? 0 : value.hashCode()));
    }

    /**
     * Equals.
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Indexed) {
            final Indexed<?> other = (Indexed<?>) obj;

            return this.index == other.index && N.equals(this.value, other.value);
        }

        return false;
    }

    /**
     * To string.
     *
     * @return
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + N.toString(value);
    }
}
