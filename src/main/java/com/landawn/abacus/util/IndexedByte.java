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
 * Represents a primitive byte value paired with an index position.
 *
 * <p>This class is a specialized version of {@code Indexed<Byte>} for primitive byte
 * values, providing better performance by avoiding boxing/unboxing overhead.</p>
 *
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedByte indexedByte = IndexedByte.of((byte) 42, 5);
 * byte value = indexedByte.value();   // returns 42
 * int index = indexedByte.index();    // returns 5
 * }</pre>
 *
 * @see Indexed
 * @see IndexedBoolean
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class IndexedByte extends AbstractIndexed {

    /** The byte value associated with the index. */
    private final byte value;

    /**
     * Constructs an IndexedByte instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(byte, int)} or
     * {@link #of(byte, long)} factory methods for creating instances.
     *
     * @param index the index position (non-negative long value)
     * @param value the byte value to be associated with the index
     */
    IndexedByte(final long index, final byte value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedByte instance with the specified value and index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedByte indexed = IndexedByte.of((byte) 10, 3);
     * }</pre>
     *
     * @param value the byte value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable IndexedByte instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
     */
    public static IndexedByte of(final byte value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedByte(index, value);
    }

    /**
     * Creates a new IndexedByte instance with the specified value and index.
     *
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedByte indexed = IndexedByte.of((byte) 25, 5_000_000_000L);
     * }</pre>
     *
     * @param value the byte value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable IndexedByte instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
     */
    public static IndexedByte of(final byte value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedByte(index, value);
    }

    /**
     * Returns the byte value stored in this IndexedByte instance.
     *
     * <p>The index associated with this value can be retrieved through the {@link #index()}
     * method inherited from {@link AbstractIndexed}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedByte indexed = IndexedByte.of((byte) 10, 3);
     * byte v = indexed.value();   // returns (byte) 10
     * int i = indexed.index();    // returns 3
     * }</pre>
     *
     * @return the byte value associated with this index
     */
    public byte value() {
        return value;
    }

    /**
     * Returns the hash code of this {@code IndexedByte} instance.
     *
     * <p>The hash code is computed from both the index and the value.</p>
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        return 31 * Byte.hashCode(value) + hashLong(index);
    }

    /**
     * Checks if this IndexedByte instance is equal to another object.
     *
     * <p>Two IndexedByte instances are equal if they have the same index and value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedByte indexed1 = IndexedByte.of((byte) 42, 5);
     * IndexedByte indexed2 = IndexedByte.of((byte) 42, 5);
     * IndexedByte indexed3 = IndexedByte.of((byte) 43, 5);
     *
     * indexed1.equals(indexed2);   // returns true
     * indexed1.equals(indexed3);   // returns false
     * }</pre>
     *
     * @param obj the object to compare with this IndexedByte instance for equality
     * @return {@code true} if the specified object is an IndexedByte with the same
     *         index and value, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedByte && ((IndexedByte) obj).index == index && N.equals(((IndexedByte) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedByte instance.
     *
     * <p>The format is: {@code [index]=value}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedByte indexed = IndexedByte.of((byte) 42, 5);
     * System.out.println(indexed);   // prints [5]=42
     * }</pre>
     *
     * @return a string representation in the format {@code [index]=value}
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
