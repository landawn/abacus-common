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
 * <p>This class is a specialized version of {@link Indexed} for primitive int values,
 * providing better performance by avoiding boxing/unboxing overhead.</p>
 * 
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedInt indexedInt = IndexedInt.of(42, 5);
 * int value = indexedInt.value(); // 42
 * long index = indexedInt.index(); // 5
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

    private final int value;

    /**
     * Constructs an IndexedInt instance with the specified index and value.
     *
     * @param index the index position
     * @param value the int value to be indexed
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
     * @param value the int value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new IndexedInt instance
     * @throws IllegalArgumentException if index is negative
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
     * @param value the int value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new IndexedInt instance
     * @throws IllegalArgumentException if index is negative
     */
    public static IndexedInt of(final int value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedInt(index, value);
    }

    /**
     * Returns the int value stored in this IndexedInt instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedInt indexed = IndexedInt.of(42, 5);
     * int value = indexed.value(); // 42
     * }</pre>
     *
     * @return the int value
     */
    public int value() {
        return value;
    }

    /**
     * Returns the hash code of this IndexedInt instance.
     * 
     * <p>The hash code is computed based on both the index and the value.</p>
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return (int) index + value * 31;
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
     * indexed1.equals(indexed2); // true
     * indexed1.equals(indexed3); // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedInt && ((IndexedInt) obj).index == index && N.equals(((IndexedInt) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedInt instance.
     * 
     * <p>The format is: [index]=value</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedInt indexed = IndexedInt.of(42, 5);
     * System.out.println(indexed); // prints: [5]=42
     * }</pre>
     *
     * @return a string representation in the format [index]=value
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }

}