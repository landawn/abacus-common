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
 * Represents a primitive char value paired with an index position.
 *
 * <p>This class is a specialized version of {@code Indexed<Character>} for primitive char
 * values, providing better performance by avoiding boxing/unboxing overhead.</p>
 *
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedChar indexedChar = IndexedChar.of('A', 0);
 * char value = indexedChar.value();   // returns 'A'
 * int index = indexedChar.index();    // returns 0
 * }</pre>
 *
 * @see Indexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class IndexedChar extends AbstractIndexed {

    /** The char value associated with the index. */
    private final char value;

    /**
     * Constructs an IndexedChar instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(char, int)} or
     * {@link #of(char, long)} factory methods for creating instances.
     *
     * @param index the index position (non-negative long value)
     * @param value the char value to be associated with the index
     */
    IndexedChar(final long index, final char value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedChar instance with the specified value and index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedChar indexed = IndexedChar.of('X', 5);
     * }</pre>
     *
     * @param value the char value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable IndexedChar instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
     */
    public static IndexedChar of(final char value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedChar(index, value);
    }

    /**
     * Creates a new IndexedChar instance with the specified value and index.
     *
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedChar indexed = IndexedChar.of('Z', 5_000_000_000L);
     * }</pre>
     *
     * @param value the char value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable IndexedChar instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
     */
    public static IndexedChar of(final char value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedChar(index, value);
    }

    /**
     * Returns the char value stored in this IndexedChar instance.
     *
     * <p>The index associated with this value can be retrieved through the {@link #index()}
     * method inherited from {@link AbstractIndexed}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedChar indexed = IndexedChar.of('X', 5);
     * char v = indexed.value();   // returns 'X'
     * int i = indexed.index();    // returns 5
     * }</pre>
     *
     * @return the char value associated with this index
     */
    public char value() {
        return value;
    }

    /**
     * Returns the hash code of this {@code IndexedChar} instance.
     *
     * <p>The hash code is computed from both the index and the value.</p>
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        return 31 * Character.hashCode(value) + hashLong(index);
    }

    /**
     * Checks if this IndexedChar instance is equal to another object.
     *
     * <p>Two IndexedChar instances are equal if they have the same index and value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedChar indexed1 = IndexedChar.of('X', 5);
     * IndexedChar indexed2 = IndexedChar.of('X', 5);
     * IndexedChar indexed3 = IndexedChar.of('Y', 5);
     *
     * indexed1.equals(indexed2);   // returns true
     * indexed1.equals(indexed3);   // returns false
     * }</pre>
     *
     * @param obj the object to compare with this IndexedChar instance for equality
     * @return {@code true} if the specified object is an IndexedChar with the same
     *         index and value, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedChar && ((IndexedChar) obj).index == index && N.equals(((IndexedChar) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedChar instance.
     *
     * <p>The format is: {@code [index]=value}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedChar indexed = IndexedChar.of('B', 3);
     * System.out.println(indexed);   // prints [3]=B
     * }</pre>
     *
     * @return a string representation in the format {@code [index]=value}
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
