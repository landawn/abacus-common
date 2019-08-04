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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class Indexed<T> extends AbstractIndexed {
    private final T value;

    Indexed(long index, T value) {
        super(index);
        this.value = value;
    }

    public static <T> Indexed<T> of(T value, int index) {
        N.checkArgNotNegative(index, "index");

        return new Indexed<>(index, value);
    }

    public static <T> Indexed<T> of(T value, long index) {
        N.checkArgNotNegative(index, "index");

        return new Indexed<>(index, value);
    }

    public static <T> ObjIterator<Indexed<T>> iterate(final Iterator<? extends T> iter) {
        return iterate(iter, 0);
    }

    public static <T> ObjIterator<Indexed<T>> iterate(final Iterator<? extends T> iter, final int startIndex) {
        N.checkArgNotNegative(startIndex, "startIndex");

        return iterate(iter, (long) startIndex);
    }

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

    public T value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) (index * 31 + (value == null ? 0 : value.hashCode()));
    }

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

    @Override
    public String toString() {
        return "[" + index + "]=" + N.toString(value);
    }
}
