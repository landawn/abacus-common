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
 * Represents a primitive float value paired with an index position.
 *
 * <p>This class is a specialized version of {@code Indexed<Float>} for primitive float
 * values, providing better performance by avoiding boxing/unboxing overhead.</p>
 *
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedFloat indexedFloat = IndexedFloat.of(3.14f, 2);
 * float value = indexedFloat.value();   // returns 3.14f
 * int index = indexedFloat.index();     // returns 2
 * }</pre>
 *
 * @see Indexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedDouble
 */
public final class IndexedFloat extends AbstractIndexed {

    /** The float value associated with the index. */
    private final float value;

    /**
     * Constructs an IndexedFloat instance with the specified index and value.
     * This is a package-private constructor; use {@link #of(float, int)} or
     * {@link #of(float, long)} factory methods for creating instances.
     *
     * @param index the index position (non-negative long value)
     * @param value the float value to be associated with the index
     */
    IndexedFloat(final long index, final float value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedFloat instance with the specified value and index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedFloat indexed = IndexedFloat.of(2.718f, 10);
     * }</pre>
     *
     * @param value the float value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Integer.MAX_VALUE)
     * @return a new immutable IndexedFloat instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
     */
    public static IndexedFloat of(final float value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedFloat(index, value);
    }

    /**
     * Creates a new IndexedFloat instance with the specified value and index.
     *
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedFloat indexed = IndexedFloat.of(1.414f, 5_000_000_000L);
     * }</pre>
     *
     * @param value the float value to be associated with the index
     * @param index the index position (must be non-negative, 0 to Long.MAX_VALUE)
     * @return a new immutable IndexedFloat instance containing the specified value and index
     * @throws IllegalArgumentException if index is negative (index &lt; 0).
     */
    public static IndexedFloat of(final float value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedFloat(index, value);
    }

    /**
     * Returns the float value stored in this IndexedFloat instance.
     *
     * <p>The index associated with this value can be retrieved through the {@link #index()}
     * method inherited from {@link AbstractIndexed}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedFloat indexed = IndexedFloat.of(2.718f, 10);
     * float v = indexed.value();   // returns 2.718f
     * int i = indexed.index();     // returns 10
     * }</pre>
     *
     * @return the float value associated with this index
     */
    public float value() {
        return value;
    }

    /**
     * Returns the hash code of this {@code IndexedFloat} instance.
     *
     * <p>The hash code is computed from all bits of both the index and the value.</p>
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        return 31 * Float.hashCode(value) + hashLong(index);
    }

    /**
     * Checks if this {@code IndexedFloat} instance is equal to another object.
     *
     * <p>Two {@code IndexedFloat} instances are equal if they have the same index and value.
     * Float comparison is delegated to {@link N#equals(float, float)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedFloat indexed1 = IndexedFloat.of(3.14f, 5);
     * IndexedFloat indexed2 = IndexedFloat.of(3.14f, 5);
     * IndexedFloat indexed3 = IndexedFloat.of(2.71f, 5);
     *
     * indexed1.equals(indexed2);   // returns true
     * indexed1.equals(indexed3);   // returns false
     * }</pre>
     *
     * @param obj the object to compare with this {@code IndexedFloat} instance for equality
     * @return {@code true} if the specified object is an {@code IndexedFloat} with the same
     *         index and value, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedFloat && ((IndexedFloat) obj).index == index && N.equals(((IndexedFloat) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedFloat instance.
     *
     * <p>The format is: {@code [index]=value}</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedFloat indexed = IndexedFloat.of(3.14f, 7);
     * System.out.println(indexed);   // prints [7]=3.14
     * }</pre>
     *
     * @return a string representation in the format {@code [index]=value}
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
