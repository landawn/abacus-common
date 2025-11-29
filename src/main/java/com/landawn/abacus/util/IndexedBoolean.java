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
 * Represents a primitive boolean value paired with an index position.
 * 
 * <p>This class is a specialized version of {@link Indexed} for primitive boolean values,
 * providing better performance by avoiding boxing/unboxing overhead.</p>
 * 
 * <p>The class is immutable and extends {@link AbstractIndexed}.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * IndexedBoolean indexedBool = IndexedBoolean.of(true, 5);
 * boolean value = indexedBool.value();  // true
 * long index = indexedBool.index();  // 5
 * }</pre>
 *
 * @see Indexed
 * @see IndexedByte
 * @see IndexedChar
 * @see IndexedShort
 * @see IndexedInt
 * @see IndexedLong
 * @see IndexedFloat
 * @see IndexedDouble
 */
public final class IndexedBoolean extends AbstractIndexed {

    private final boolean value;

    /**
     * Constructs an IndexedBoolean instance with the specified index and value.
     *
     * @param index the index position
     * @param value the boolean value to be indexed
     */
    IndexedBoolean(final long index, final boolean value) {
        super(index);
        this.value = value;
    }

    /**
     * Creates a new IndexedBoolean instance with the specified value and index.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedBoolean indexed = IndexedBoolean.of(true, 5);
     * }</pre>
     *
     * @param value the boolean value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new IndexedBoolean instance
     * @throws IllegalArgumentException if index is negative
     */
    public static IndexedBoolean of(final boolean value, final int index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedBoolean(index, value);
    }

    /**
     * Creates a new IndexedBoolean instance with the specified value and index.
     * 
     * <p>This overload accepts a long index for cases where the index might exceed Integer.MAX_VALUE.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedBoolean indexed = IndexedBoolean.of(true, 5000000000L);
     * }</pre>
     *
     * @param value the boolean value to be indexed
     * @param index the index position (must be non-negative)
     * @return a new IndexedBoolean instance
     * @throws IllegalArgumentException if index is negative
     */
    public static IndexedBoolean of(final boolean value, final long index) throws IllegalArgumentException {
        N.checkArgNotNegative(index, cs.index);

        return new IndexedBoolean(index, value);
    }

    /**
     * Returns the boolean value stored in this IndexedBoolean instance.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedBoolean indexed = IndexedBoolean.of(true, 5);
     * boolean value = indexed.value();  // true
     * }</pre>
     *
     * @return the boolean value
     */
    public boolean value() {
        return value;
    }

    /**
     * Returns the hash code of this IndexedBoolean instance.
     * 
     * <p>The hash code is computed based on both the index and the value.
     * The formula ensures that {@code true} and {@code false} values at the same index have different hash codes.</p>
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return (int) index + (value ? 0 : 31);
    }

    /**
     * Checks if this IndexedBoolean instance is equal to another object.
     * 
     * <p>Two IndexedBoolean instances are equal if they have the same index and value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedBoolean indexed1 = IndexedBoolean.of(true, 5);
     * IndexedBoolean indexed2 = IndexedBoolean.of(true, 5);
     * IndexedBoolean indexed3 = IndexedBoolean.of(false, 5);
     * 
     * indexed1.equals(indexed2);   // true
     * indexed1.equals(indexed3);   // false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof IndexedBoolean && ((IndexedBoolean) obj).index == index && N.equals(((IndexedBoolean) obj).value, value);
    }

    /**
     * Returns a string representation of this IndexedBoolean instance.
     * 
     * <p>The format is: [index]=value</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IndexedBoolean indexed = IndexedBoolean.of(true, 5);
     * System.out.println(indexed);  // prints: [5]=true
     * }</pre>
     *
     * @return a string representation in the format [index]=value
     */
    @Override
    public String toString() {
        return "[" + index + "]=" + value;
    }
}
