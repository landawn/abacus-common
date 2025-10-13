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
 * An immutable container that pairs a float value with its index position.
 * This class is useful for operations where both the floating-point value and its
 * original position need to be preserved, such as when sorting or filtering numeric
 * collections while maintaining knowledge of the original indices.
 * 
 * <p>This class extends {@link AbstractIndexed} and provides factory methods for creating
 * instances with validated non-negative indices.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * IndexedFloat ifloat = IndexedFloat.of(3.14f, 2);
 * System.out.println(ifloat.value()); // prints: 3.14
 * System.out.println(ifloat.index()); // prints: 2
 * }</pre>
 * 
 * @since 1.0
 */
public final class IndexedFloat extends AbstractIndexed {

    private final float value;

    IndexedFloat(final long index, final float value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new {@code IndexedFloat} instance with the specified value and index.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IndexedFloat indexed = IndexedFloat.of(2.718f, 10);
     * }</pre>
     *
     * @param value the float value to be stored
     * @param index the index position, must be non-negative
     * @return a new {@code IndexedFloat} instance
     * @throws IllegalArgumentException if the index is negative
     */
    public static IndexedFloat of(final float value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedFloat(index, value);
    }

    /**
     * Creates a new {@code IndexedFloat} instance with the specified value and long index.
     * This method is useful when working with large arrays or collections where the index
     * might exceed the range of an int.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * IndexedFloat indexed = IndexedFloat.of(1.414f, 1000000000L);
     * }</pre>
     *
     * @param value the float value to be stored
     * @param index the index position as a long, must be non-negative
     * @return a new {@code IndexedFloat} instance
     * @throws IllegalArgumentException if the index is negative
     */
    public static IndexedFloat of(final float value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedFloat(index, value);
    }

    /**
     * Returns the float value stored in this container.
     * 
     * @return the float value
     */
    public float value() {
        return value;
    }

    /**
     * Returns a hash code value for this object. The hash code is computed
     * using both the index and the value to ensure proper distribution in
     * hash-based collections.
     * 
     * <p>Note: The float value is cast to int for hash calculation, which may
     * result in hash collisions for values that differ only in their fractional parts.</p>
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        return (int) index + (int) (value * 31); // NOSONAR
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     * Two {@code IndexedFloat} objects are considered equal if they have
     * the same index and the same value.
     * 
     * <p>Float comparison is performed using the {@code N.equals} method,
     * which handles special float values (NaN, positive/negative infinity) correctly.</p>
     *
     * @param obj the reference object with which to compare
     * @return {@code true} if this object is equal to the obj argument;
     *         {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedFloat && ((IndexedFloat) obj).index == index && N.equals(((IndexedFloat) obj).value, value);
    }

    /**
     * Returns a string representation of this {@code IndexedFloat}.
     * The string representation consists of the index in square brackets
     * followed by an equals sign and the float value.
     * 
     * <p>For example, an {@code IndexedFloat} with index 7 and value 3.14f
     * would return the string {@code "[7]=3.14"}.</p>
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}