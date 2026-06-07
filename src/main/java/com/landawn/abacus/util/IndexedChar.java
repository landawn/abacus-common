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
 * An immutable container that pairs a char value with its index position.
 * This class is useful for operations where both the character value and its original position
 * need to be preserved, such as when processing strings while maintaining knowledge of
 * character positions.
 *
 * <p>This class is a specialized version of {@code Indexed<Character>} for primitive char
 * values, providing better performance by avoiding boxing/unboxing overhead. It extends
 * {@link AbstractIndexed} and provides factory methods for creating instances with validated
 * non-negative indices.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedChar ic = IndexedChar.of('A', 0);
 * System.out.println(ic.value());   // prints A
 * System.out.println(ic.index());   // prints 0
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
     * Creates a new {@code IndexedChar} instance with the specified value and index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedChar indexed = IndexedChar.of('X', 5);
     * }</pre>
     *
     * @param value the char value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable {@code IndexedChar} instance containing the specified value and index
     * @throws IllegalArgumentException if the index is negative (index &lt; 0)
     */
    public static IndexedChar of(final char value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedChar(index, value);
    }

    /**
     * Creates a new {@code IndexedChar} instance with the specified value and long index.
     * This method is useful when working with large strings or character sequences where
     * the index might exceed the range of an int.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedChar indexed = IndexedChar.of('Z', 1000000000L);
     * }</pre>
     *
     * @param value the char value to be associated with the index
     * @param index the index position as a long (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable {@code IndexedChar} instance containing the specified value and index
     * @throws IllegalArgumentException if the index is negative (index &lt; 0)
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
     * Two {@code IndexedChar} objects are considered equal if they have
     * the same index and the same value.
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument;
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedChar && ((IndexedChar) obj).index == index && N.equals(((IndexedChar) obj).value, value);
    }

    /**
     * Returns a string representation of this {@code IndexedChar}.
     * The string representation consists of the index in square brackets
     * followed by an equals sign and the character value.
     *
     * <p>For example, an {@code IndexedChar} with index 3 and value 'B'
     * would return the string {@code "[3]=B"}.</p>
     *
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
