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
 * The Class IndexedBoolean.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class IndexedBoolean extends AbstractIndexed {

    /** The value. */
    private final boolean value;

    /**
     * Instantiates a new indexed boolean.
     *
     * @param index the index
     * @param value the value
     */
    IndexedBoolean(long index, boolean value) {
        super(index);
        this.value = value;
    }

    /**
     * Of.
     *
     * @param value the value
     * @param index the index
     * @return the indexed boolean
     */
    public static IndexedBoolean of(boolean value, int index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedBoolean(index, value);
    }

    /**
     * Of.
     *
     * @param value the value
     * @param index the index
     * @return the indexed boolean
     */
    public static IndexedBoolean of(boolean value, long index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedBoolean(index, value);
    }

    /**
     * Of.
     *
     * @param iter the iter
     * @return the obj iterator
     */
    public static ObjIterator<IndexedBoolean> of(final BooleanIterator iter) {
        return of(iter, 0);
    }

    /**
     * Of.
     *
     * @param iter the iter
     * @param startIndex the start index
     * @return the obj iterator
     */
    public static ObjIterator<IndexedBoolean> of(final BooleanIterator iter, final int startIndex) {
        return of(iter, (long) startIndex);
    }

    /**
     * Of.
     *
     * @param iter the iter
     * @param startIndex the start index
     * @return the obj iterator
     */
    public static ObjIterator<IndexedBoolean> of(final BooleanIterator iter, final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        return new ObjIterator<IndexedBoolean>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedBoolean next() {
                return IndexedBoolean.of(iter.nextBoolean(), idx++);
            }
        };
    }

    /**
     * Value.
     *
     * @return true, if successful
     */
    public boolean value() {
        return value;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    @Override
    public int hashCode() {
        return (int) index + (value ? 0 : 31);
    }

    /**
     * Equals.
     *
     * @param obj the obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedBoolean && ((IndexedBoolean) obj).index == index && N.equals(((IndexedBoolean) obj).value, value);
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
