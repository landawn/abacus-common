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
 * Represents a primitive long value paired with an index position.
 * 
 * <p>This class is a specialized version of {@link Indexed} for primitive long values,
 * providing better performance by avoiding boxing/unboxing overhead.</p>
 * 
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedLong indexedLong = IndexedLong.of(123456789L, 5);
 * long value = indexedLong.value();   // 123456789L
 * long index = indexedLong.index();   // 5
 * }</pre>
 *
 * @see Indexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class IndexedLong extends AbstractIndexed {

    private final long value;

    /**
     * Constructs an IndexedLong instance with the specified index and value.
     *
     * @param index the index position
     * @param value the long value to be indexed
     */
    IndexedLong(final long index, final long value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedLong instance with the specified value and index.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedLong indexed = IndexedLong.of(123456789L, 5);
     * }</pre>
     *
     * @param value the long value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new IndexedLong instance
     * @throws IllegalArgumentException if index is negative
     */
    public static IndexedLong of(final long value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedLong(index, value);
    }

    /**
     * Creates a new IndexedLong instance with the specified value and index.
     * 
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedLong indexed = IndexedLong.of(123456789L, 5000000000L);
     * }</pre>
     *
     * @param value the long value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new IndexedLong instance
     * @throws IllegalArgumentException if index is negative
     */
    public static IndexedLong of(final long value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedLong(index, value);
    }

    /**
     * Returns the long value stored in this IndexedLong instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedLong indexed = IndexedLong.of(123456789L, 5);
     * long value = indexed.value();   // 123456789L
     * }</pre>
     *
     * @return the long value
     */
    public long value() {
        return value;
    }

    /**
     * Returns the hash code of this IndexedLong instance.
     * 
     * <p>The hash code is computed based on both the index and the value.</p>
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return (int) index + (int) (value * 31);
    }

    /**
     * Checks if this IndexedLong instance is equal to another object.
     * 
     * <p>Two IndexedLong instances are equal if they have the same index and value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedLong indexed1 = IndexedLong.of(123456789L, 5);
     * IndexedLong indexed2 = IndexedLong.of(123456789L, 5);
     * IndexedLong indexed3 = IndexedLong.of(987654321L, 5);
     * 
     * indexed1.equals(indexed2);   // true
     * indexed1.equals(indexed3);   // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedLong && ((IndexedLong) obj).index == index && N.equals(((IndexedLong) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedLong instance.
     * 
     * <p>The format is: [index]=value</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedLong indexed = IndexedLong.of(123456789L, 5);
     * System.out.println(indexed);   // prints: [5]=123456789
     * }</pre>
     *
     * @return a string representation in the format [index]=value
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}
