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
 * Represents a primitive short value paired with an index position.
 *
 * <p>This class is a specialized version of {@code Indexed<Short>} for primitive short
 * values, providing better performance by avoiding boxing/unboxing overhead.</p>
 *
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedShort indexedShort = IndexedShort.of((short)42, 5);
 * short value = indexedShort.value();   // 42
 * int index = indexedShort.index();   // 5
 * }</pre>
 *
 * @see Indexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class IndexedShort extends AbstractIndexed {

    /** The short value associated with the index. */
    private final short value;

    /**
     * Constructs an IndexedShort instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(short, int)} or
     * {@link #of(short, long)} factory methods for creating instances.
     *
     * @param index the index position (non-negative long value)
     * @param value the short value to be associated with the index
     */
    IndexedShort(final long index, final short value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedShort instance with the specified value and index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedShort indexed = IndexedShort.of((short)42, 5);
     * }</pre>
     *
     * @param value the short value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable IndexedShort instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static IndexedShort of(final short value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedShort(index, value);
    }

    /**
     * Creates a new IndexedShort instance with the specified value and index.
     *
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedShort indexed = IndexedShort.of((short)42, 5000000000L);
     * }</pre>
     *
     * @param value the short value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable IndexedShort instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static IndexedShort of(final short value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedShort(index, value);
    }

    /**
     * Returns the short value stored in this IndexedShort instance.
     *
     * <p>The index associated with this value can be retrieved through the {@link #index()}
     * method inherited from {@link AbstractIndexed}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedShort indexed = IndexedShort.of((short)42, 5);
     * short value = indexed.value();   // 42
     * }</pre>
     *
     * @return the short value associated with this index
     */
    public short value() {
        return value;
    }

    /**
     * Returns the hash code of this {@code IndexedShort} instance.
     *
     * <p>The hash code is computed from both the index and the value.</p>
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        return (int) index + value * 31;
    }

    /**
     * Checks if this IndexedShort instance is equal to another object.
     *
     * <p>Two IndexedShort instances are equal if they have the same index and value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedShort indexed1 = IndexedShort.of((short)42, 5);
     * IndexedShort indexed2 = IndexedShort.of((short)42, 5);
     * IndexedShort indexed3 = IndexedShort.of((short)43, 5);
     *
     * indexed1.equals(indexed2);   // true
     * indexed1.equals(indexed3);   // false
     * }</pre>
     *
     * @param obj the object to compare with this IndexedShort instance for equality
     * @return {@code true} if the specified object is an IndexedShort with the same
     *         index and value, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedShort && ((IndexedShort) obj).index == index && N.equals(((IndexedShort) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedShort instance.
     *
     * <p>The format is: {@code [index]=value}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedShort indexed = IndexedShort.of((short)42, 5);
     * System.out.println(indexed);   // prints: [5]=42
     * }</pre>
     *
     * @return a string representation in the format {@code [index]=value}
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}
