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

// TODO: Auto-generated Javadoc
/**
 * The Class IndexedShort.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class IndexedShort extends AbstractIndexed {

    /** The value. */
    private final short value;

    /**
     * Instantiates a new indexed short.
     *
     * @param index the index
     * @param value the value
     */
    IndexedShort(long index, short value) {
        super(index);
        this.value = value;
    }

    /**
     * Of.
     *
     * @param value the value
     * @param index the index
     * @return the indexed short
     */
    public static IndexedShort of(short value, int index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedShort(index, value);
    }

    /**
     * Of.
     *
     * @param value the value
     * @param index the index
     * @return the indexed short
     */
    public static IndexedShort of(short value, long index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedShort(index, value);
    }

    /**
     * Of.
     *
     * @param iter the iter
     * @return the obj iterator
     */
    public static ObjIterator<IndexedShort> of(final ShortIterator iter) {
        return of(iter, 0);
    }

    /**
     * Of.
     *
     * @param iter the iter
     * @param startIndex the start index
     * @return the obj iterator
     */
    public static ObjIterator<IndexedShort> of(final ShortIterator iter, final int startIndex) {
        return of(iter, (long) startIndex);
    }

    /**
     * Of.
     *
     * @param iter the iter
     * @param startIndex the start index
     * @return the obj iterator
     */
    public static ObjIterator<IndexedShort> of(final ShortIterator iter, final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        return new ObjIterator<IndexedShort>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedShort next() {
                return IndexedShort.of(iter.nextShort(), idx++);
            }
        };
    }

    /**
     * Value.
     *
     * @return the short
     */
    public short value() {
        return value;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return (int) index + value * 31;
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedShort && ((IndexedShort) obj).index == index && N.equals(((IndexedShort) obj).value, value);
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}
