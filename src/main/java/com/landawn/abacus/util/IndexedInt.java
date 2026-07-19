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
 * Represents a primitive int value paired with an index position.
 *
 * <p>This class is a specialized version of {@code Indexed<Integer>} for primitive int
 * values, providing better performance by avoiding boxing/unboxing overhead.</p>
 *
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedInt indexedInt = IndexedInt.of(42, 5);
 * int value = indexedInt.value();   // returns 42
 * int index = indexedInt.index();   // returns 5
 * }</pre>
 *
 * @see Indexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedLong
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class IndexedInt extends AbstractIndexed {

    /** The int value associated with the index. */
    private final int value;

    /**
     * Constructs an IndexedInt instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(int, int)} or
     * {@link #of(int, long)} factory methods for creating instances.
     *
     * @param index the index position (non-negative long value)
     * @param value the int value to be associated with the index
     */
    IndexedInt(final long index, final int value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedInt instance with the specified value and index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedInt indexed = IndexedInt.of(42, 5);
     * }</pre>
     *
     * @param value the int value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable IndexedInt instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static IndexedInt of(final int value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedInt(index, value);
    }

    /**
     * Creates a new IndexedInt instance with the specified value and index.
     *
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedInt indexed = IndexedInt.of(42, 5000000000L);
     * }</pre>
     *
     * @param value the int value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable IndexedInt instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static IndexedInt of(final int value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedInt(index, value);
    }

    /**
     * Returns the int value stored in this IndexedInt instance.
     *
     * <p>The index associated with this value can be retrieved through the {@link #index()}
     * method inherited from {@link AbstractIndexed}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedInt indexed = IndexedInt.of(42, 5);
     * int value = indexed.value();   // returns 42
     * }</pre>
     *
     * @return the int value associated with this index
     */
    public int value() {
        return value;
    }

    /**
     * Returns the hash code of this {@code IndexedInt} instance.
     *
     * <p>The hash code is computed from both the index and the value.</p>
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        return 31 * Integer.hashCode(value) + hashLong(index);
    }

    /**
     * Checks if this IndexedInt instance is equal to another object.
     *
     * <p>Two IndexedInt instances are equal if they have the same index and value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedInt indexed1 = IndexedInt.of(42, 5);
     * IndexedInt indexed2 = IndexedInt.of(42, 5);
     * IndexedInt indexed3 = IndexedInt.of(43, 5);
     *
     * indexed1.equals(indexed2);   // returns true
     * indexed1.equals(indexed3);   // returns false
     * }</pre>
     *
     * @param obj the object to compare with this IndexedInt instance for equality
     * @return {@code true} if the specified object is an IndexedInt with the same
     *         index and value, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedInt && ((IndexedInt) obj).index == index && N.equals(((IndexedInt) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedInt instance.
     *
     * <p>The format is: {@code [index]=value}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedInt indexed = IndexedInt.of(42, 5);
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
