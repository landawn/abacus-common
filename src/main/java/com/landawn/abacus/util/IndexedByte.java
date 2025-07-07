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
 * An immutable container that pairs a byte value with its index position.
 * This class is useful for operations where both the value and its original position
 * need to be preserved, such as when sorting or filtering collections while maintaining
 * knowledge of the original indices.
 * 
 * <p>This class extends {@link AbstractIndexed} and provides factory methods for creating
 * instances with validated non-negative indices.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * IndexedByte ib = IndexedByte.of((byte) 42, 5);
 * System.out.println(ib.value()); // prints: 42
 * System.out.println(ib.index()); // prints: 5
 * }</pre>
 * 
 * @author HaiYang Li
 * @since 1.0
 */
public final class IndexedByte extends AbstractIndexed {

    private final byte value;

    IndexedByte(final long index, final byte value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new {@code IndexedByte} instance with the specified value and index.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IndexedByte indexed = IndexedByte.of((byte) 10, 3);
     * }</pre>
     *
     * @param value the byte value to be stored
     * @param index the index position, must be non-negative
     * @return a new {@code IndexedByte} instance
     * @throws IllegalArgumentException if the index is negative
     */
    public static IndexedByte of(final byte value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedByte(index, value);
    }

    /**
     * Creates a new {@code IndexedByte} instance with the specified value and long index.
     * This method is useful when working with large collections or arrays where the index
     * might exceed the range of an int.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IndexedByte indexed = IndexedByte.of((byte) 25, 1000000000L);
     * }</pre>
     *
     * @param value the byte value to be stored
     * @param index the index position as a long, must be non-negative
     * @return a new {@code IndexedByte} instance
     * @throws IllegalArgumentException if the index is negative
     */
    public static IndexedByte of(final byte value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedByte(index, value);
    }

    /**
     * Returns the byte value stored in this container.
     * 
     * @return the byte value
     */
    public byte value() {
        return value;
    }

    /**
     * Returns a hash code value for this object. The hash code is computed
     * using both the index and the value to ensure proper distribution in
     * hash-based collections.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return (int) index + value * 31;
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code IndexedByte} objects are considered equal if they have
     * the same index and the same value.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument;
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedByte && ((IndexedByte) obj).index == index && N.equals(((IndexedByte) obj).value, value);
    }

    /**
     * Returns a string representation of this {@code IndexedByte}.
     * The string representation consists of the index in square brackets
     * followed by an equals sign and the value.
     * 
     * <p>For example, an {@code IndexedByte} with index 5 and value 42
     * would return the string {@code "[5]=42"}.</p>
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}