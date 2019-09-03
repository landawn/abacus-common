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
 * The Class IndexedDouble.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class IndexedDouble extends AbstractIndexed {

    /** The value. */
    private final double value;

    /**
     * Instantiates a new indexed double.
     *
     * @param index
     * @param value
     */
    IndexedDouble(long index, double value) {
        super(index);
        this.value = value;
    }

    /**
     *
     * @param value
     * @param index
     * @return
     */
    public static IndexedDouble of(double value, int index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedDouble(index, value);
    }

    /**
     *
     * @param value
     * @param index
     * @return
     */
    public static IndexedDouble of(double value, long index) {
        N.checkArgNotNegative(index, "index");

        return new IndexedDouble(index, value);
    }

    /**
     *
     * @param iter
     * @return
     */
    public static ObjIterator<IndexedDouble> of(final DoubleIterator iter) {
        return of(iter, 0);
    }

    /**
     *
     * @param iter
     * @param startIndex
     * @return
     */
    public static ObjIterator<IndexedDouble> of(final DoubleIterator iter, final int startIndex) {
        return of(iter, (long) startIndex);
    }

    /**
     *
     * @param iter
     * @param startIndex
     * @return
     */
    public static ObjIterator<IndexedDouble> of(final DoubleIterator iter, final long startIndex) {
        if (startIndex < 0) {
            throw new IllegalArgumentException("Invalid start index: " + startIndex);
        }

        return new ObjIterator<IndexedDouble>() {
            private long idx = startIndex;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public IndexedDouble next() {
                return IndexedDouble.of(iter.nextDouble(), idx++);
            }
        };
    }

    /**
     *
     * @return
     */
    public double value() {
        return value;
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return (int) index + (int) (value * 31);
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof IndexedDouble && ((IndexedDouble) obj).index == index && N.equals(((IndexedDouble) obj).value, value);
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
