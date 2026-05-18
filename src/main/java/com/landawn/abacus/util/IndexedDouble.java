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
 * Represents a primitive double value paired with an index position.
 *
 * <p>This class is a specialized version of {@code Indexed<Double>} for primitive double
 * values, providing better performance by avoiding boxing/unboxing overhead.</p>
 *
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedDouble indexedDouble = IndexedDouble.of(3.14159, 5);
 * double value = indexedDouble.value();   // 3.14159
 * int index = indexedDouble.index();   // 5
 * }</pre>
 *
 * @see Indexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedFloat
 */
public final class IndexedDouble extends AbstractIndexed {

    /** The double value associated with the index. */
    private final double value;

    /**
     * Constructs an IndexedDouble instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(double, int)} or
     * {@link #of(double, long)} factory methods for creating instances.
     *
     * @param index the index position (non-negative long value)
     * @param value the double value to be associated with the index
     */
    IndexedDouble(final long index, final double value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedDouble instance with the specified value and index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedDouble indexed = IndexedDouble.of(3.14159, 5);
     * }</pre>
     *
     * @param value the double value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable IndexedDouble instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static IndexedDouble of(final double value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedDouble(index, value);
    }

    /**
     * Creates a new IndexedDouble instance with the specified value and index.
     *
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedDouble indexed = IndexedDouble.of(3.14159, 5000000000L);
     * }</pre>
     *
     * @param value the double value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable IndexedDouble instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0)
     */
    public static IndexedDouble of(final double value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedDouble(index, value);
    }

    /**
     * Returns the double value stored in this IndexedDouble instance.
     *
     * <p>The index associated with this value can be retrieved through the {@link #index()}
     * method inherited from {@link AbstractIndexed}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedDouble indexed = IndexedDouble.of(3.14159, 5);
     * double value = indexed.value();   // 3.14159
     * }</pre>
     *
     * @return the double value associated with this index
     */
    public double value() {
        return value;
    }

    /**
     * Returns the hash code of this {@code IndexedDouble} instance.
     *
     * <p>The hash code is computed from both the index and the value.
     * Note: The double value is multiplied by 31 and then truncated to {@code int},
     * so values that differ only in their fractional parts may produce the same hash code.</p>
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        return (int) index + (int) (value * 31);
    }

    /**
     * Checks if this {@code IndexedDouble} instance is equal to another object.
     *
     * <p>Two {@code IndexedDouble} instances are equal if they have the same index and value.
     * Double comparison is delegated to {@link N#equals(double, double)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedDouble indexed1 = IndexedDouble.of(3.14159, 5);
     * IndexedDouble indexed2 = IndexedDouble.of(3.14159, 5);
     * IndexedDouble indexed3 = IndexedDouble.of(2.71828, 5);
     *
     * indexed1.equals(indexed2);   // true
     * indexed1.equals(indexed3);   // false
     * }</pre>
     *
     * @param obj the object to compare with this {@code IndexedDouble} instance for equality
     * @return {@code true} if the specified object is an {@code IndexedDouble} with the same
     *         index and value, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedDouble && ((IndexedDouble) obj).index == index && N.equals(((IndexedDouble) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedDouble instance.
     *
     * <p>The format is: {@code [index]=value}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedDouble indexed = IndexedDouble.of(3.14159, 5);
     * System.out.println(indexed);   // prints: [5]=3.14159
     * }</pre>
     *
     * @return a string representation in the format {@code [index]=value}
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
