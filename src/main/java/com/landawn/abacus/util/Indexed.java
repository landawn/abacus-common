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
 * Represents a value paired with an index position.
 * 
 * <p>This class is useful for operations where you need to track both a value and its
 * position/index, such as when iterating through collections while maintaining position
 * information or when sorting elements while preserving their original indices.</p>
 * 
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Indexed<String> indexedValue = Indexed.of("Hello", 5);
 * String value = indexedValue.value(); // "Hello"
 * long index = indexedValue.index(); // 5
 * }</pre>
 *
 * @param <T> the type of the value being indexed
 * @see IndexedBoolean
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class Indexed<T> extends AbstractIndexed {

    private final T value;

    /**
     * Constructs an Indexed instance with the specified index and value.
     *
     * @param index the index position
     * @param value the value to be indexed
     */
    Indexed(final long index, final T value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new Indexed instance with the specified value and index.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> indexed = Indexed.of("Hello", 5);
     * }</pre>
     *
     * @param <T> the type of the value
     * @param value the value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new Indexed instance
     * @throws IllegalArgumentException if index is negative
     */
    public static <T> Indexed<T> of(final T value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new Indexed<>(index, value);
    }

    /**
     * Creates a new Indexed instance with the specified value and index.
     * 
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> indexed = Indexed.of("Hello", 5000000000L);
     * }</pre>
     *
     * @param <T> the type of the value
     * @param value the value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new Indexed instance
     * @throws IllegalArgumentException if index is negative
     */
    public static <T> Indexed<T> of(final T value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new Indexed<>(index, value);
    }

    /**
     * Returns the value stored in this Indexed instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> indexed = Indexed.of("Hello", 5);
     * String value = indexed.value(); // "Hello"
     * }</pre>
     *
     * @return the value
     */
    public T value() {
        return value;
    }

    /**
     * Returns the hash code of this Indexed instance.
     * 
     * <p>The hash code is computed based on both the index and the value.</p>
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return (int) (index * 31 + (value == null ? 0 : value.hashCode()));
    }

    /**
     * Checks if this Indexed instance is equal to another object.
     * 
     * <p>Two Indexed instances are equal if they have the same index and equal values
     * (where {@code null} values are considered equal to each other).</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> indexed1 = Indexed.of("Hello", 5);
     * Indexed<String> indexed2 = Indexed.of("Hello", 5);
     * Indexed<String> indexed3 = Indexed.of("World", 5);
     * 
     * indexed1.equals(indexed2); // true
     * indexed1.equals(indexed3); // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Indexed<?> other) {

            return index == other.index && N.equals(value, other.value);
        }

        return false;
    }

    /**
     * Returns a string representation of this Indexed instance.
     * 
     * <p>The format is: [index]=value</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Indexed<String> indexed = Indexed.of("Hello", 5);
     * System.out.println(indexed); // prints: [5]=Hello
     * }</pre>
     *
     * @return a string representation in the format [index]=value
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + N.toString(value);
    }
}
