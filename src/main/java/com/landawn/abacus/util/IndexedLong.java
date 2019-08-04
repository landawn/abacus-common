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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class IndexedLong extends AbstractIndexed {
    private final long value;

    IndexedLong(long index, long value) {
        super(index);
        this.value = value;
    }

    public static IndexedLong of(long value, int index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedLong(index, value);
    }

    public static IndexedLong of(long value, long index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedLong(index, value);
    }

    public static ObjIterator<IndexedLong> of(final LongIterator iter) {
        return of(iter, 0);
    }

    public static ObjIterator<IndexedLong> of(final LongIterator iter, final int startIndex) {
        return of(iter, (long) startIndex);
    }

    public static ObjIterator<IndexedLong> of(final LongIterator iter, final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        return new ObjIterator<IndexedLong>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedLong next() {
                return IndexedLong.of(iter.nextLong(), idx++);
            }
        };
    }

    public long value() {
        return value;
    }

    @Override
    public int hashCode() {
        return (int) index + (int) (value * 31);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedLong && ((IndexedLong) obj).index == index && N.equals(((IndexedLong) obj).value, value);
    }

    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}
