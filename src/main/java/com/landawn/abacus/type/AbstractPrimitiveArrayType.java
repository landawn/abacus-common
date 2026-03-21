/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.type;

/**
 * The Abstract base class for primitive array types in the type system.
 * <p>
 * This class provides common functionality for handling primitive arrays
 * ({@code int[]}, {@code double[]}, {@code boolean[]}, etc.) with optimized implementations for
 * hash code calculation, equality checking, and string conversion.
 * </p>
 *
 * @param <T> the primitive array type (e.g., {@code int[]}, {@code double[]}, {@code boolean[]})
 */
public abstract class AbstractPrimitiveArrayType<T> extends AbstractArrayType<T> {

    /**
     * Constructs an {@code AbstractPrimitiveArrayType} with the specified type name.
     *
     * @param typeName the name of the primitive array type (e.g., "int[]", "double[]")
     */
    protected AbstractPrimitiveArrayType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents a primitive array.
     * <p>
     * This method always returns {@code true} for primitive array types,
     * distinguishing them from object arrays.
     * </p>
     *
     * @return {@code true}, indicating this is a primitive array type
     */
    @Override
    public boolean isPrimitiveArray() {
        return true;
    }

    /**
     * Calculates the deep hash code for a primitive array.
     * <p>
     * For primitive arrays, the deep hash code is the same as the regular hash code
     * since primitive arrays don't contain nested objects that require deep traversal.
     * </p>
     *
     * @param x the primitive array
     * @return the hash code of the array
     */
    @Override
    public int deepHashCode(final T x) {
        return hashCode(x);
    }

    /**
     * Performs deep equality comparison between two primitive arrays.
     * <p>
     * For primitive arrays, deep equals is the same as regular equals
     * since primitive arrays don't contain nested objects that require deep comparison.
     * </p>
     *
     * @param x the first primitive array
     * @param y the second primitive array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean deepEquals(final T x, final T y) {
        return equals(x, y);
    }

    /**
     * Converts a primitive array to its string representation.
     * <p>
     * Returns "null" if the array is {@code null}, otherwise delegates
     * to the {@link #stringOf(Object)} method for the actual conversion.
     * </p>
     *
     * @param x the primitive array
     * @return the string representation of the array
     */
    @Override
    public String toString(final T x) {
        if (x == null) {
            return NULL_STRING;
        }

        return stringOf(x);
    }

    /**
     * Converts a primitive array to its deep string representation.
     * <p>
     * For primitive arrays, the deep string representation is the same as
     * the regular string representation since they don't contain nested objects.
     * </p>
     *
     * @param x the primitive array
     * @return the string representation of the array
     */
    @Override
    public String deepToString(final T x) {
        return toString(x);
    }
}
