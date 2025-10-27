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
 * An abstract base class for objects that have an associated index value.
 * This class provides a foundation for indexed data structures where elements
 * need to maintain their position or index information.
 * 
 * <p>The class is immutable and stores a long index value that can be retrieved
 * as either an int or long value.</p>
 * 
 * @see Immutable
 * @since 1.0
 */
@com.landawn.abacus.annotation.Immutable
abstract class AbstractIndexed implements Immutable {

    /** The index value associated with this object */
    protected final long index;

    protected AbstractIndexed(final long index) {
        this.index = index;
    }

    /**
     * Returns the index value as an int.
     *
     * <p>This method converts the internal long index value to an int representation.
     * If the long value is outside the range of int values (Integer.MIN_VALUE to 
     * Integer.MAX_VALUE), an ArithmeticException is thrown to prevent data loss.</p>
     *
     * <p>For accessing the full range of index values without conversion, use 
     * {@link #longIndex()} instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> indexed = Indexed.of("val", 42L);
     * int idx = indexed.index();  // Returns 42
     * System.out.println("Index: " + idx);
     * 
     * // This would throw ArithmeticException:
     * Indexed<String> largeIndex = Indexed.of("val", Long.MAX_VALUE);
     * int overflow = largeIndex.index();  // Throws ArithmeticException
     * }</pre>
     *
     * @return the index value as an int
     * @throws ArithmeticException if the long index value overflows an int
     * @see #longIndex()
     * @see Math#toIntExact(long)
     */
    public int index() throws ArithmeticException {
        return Math.toIntExact(index);
    }

    /**
     * Returns the index value as a long.
     *
     * <p>This method returns the full precision index value without any
     * conversion or potential loss of data. Unlike {@link #index()}, this method
     * never throws an exception and can handle the full range of long values.</p>
     *
     * <p>Use this method when you need to work with large index values that might
     * exceed the range of int values, or when you want to avoid the overhead of
     * range checking performed by {@link #index()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> indexed = Indexed.of("value", Long.MAX_VALUE);
     * long idx = indexed.longIndex();  // Returns Long.MAX_VALUE safely
     * System.out.println("Long index: " + idx);
     *
     * // Safe for any long value:
     * Indexed<String> largeIndex = Indexed.of("value", 9_223_372_036_854_775_807L);
     * long safeIndex = largeIndex.longIndex();  // No exceptions thrown
     * }</pre>
     *
     * @return the index value as a long
     * @see #index()
     */
    public long longIndex() {
        return index;
    }
}
