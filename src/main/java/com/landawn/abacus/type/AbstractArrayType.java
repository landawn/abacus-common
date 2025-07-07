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

import java.lang.reflect.Array;
import java.util.Collection;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 * Abstract base class for array types in the type system.
 * This class provides common functionality for handling array types,
 * including serialization and conversion operations.
 *
 * @param <T> the array type (e.g., int[], String[], Object[])
 */
public abstract class AbstractArrayType<T> extends AbstractType<T> {

    protected AbstractArrayType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents an array type.
     * This method always returns {@code true} for array types.
     *
     * @return {@code true}, indicating this is an array type
     */
    @Override
    public boolean isArray() {
        return true;
    }

    /**
     * Gets the serialization type for this array type.
     * Returns {@link SerializationType#SERIALIZABLE} if the array type is serializable,
     * otherwise returns {@link SerializationType#ARRAY}.
     *
     * @return the appropriate {@link SerializationType} for this array type
     */
    @Override
    public SerializationType getSerializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.ARRAY;
    }

    /**
     * Converts an array to a collection of the specified type.
     * Creates a new collection instance of the specified class and populates it
     * with the elements from the array using the {@link #array2Collection(Object, Collection)} method.
     *
     * @param <E> the element type of the collection
     * @param array the array to convert, may be {@code null}
     * @param collClass the class of the collection to create (must have a no-arg constructor)
     * @return a new collection containing all elements from the array, or {@code null} if the input array is {@code null}
     * @throws IllegalArgumentException if the collection class cannot be instantiated
     */
    @MayReturnNull
    @Override
    public <E> Collection<E> array2Collection(final T array, final Class<?> collClass) {
        if (array == null) {
            return null; // NOSONAR
        }

        @SuppressWarnings("rawtypes")
        final Collection<E> result = N.newCollection((Class<Collection>) collClass, Array.getLength(array));

        array2Collection(array, result);

        return result;
    }

    /**
     * Splits a string into an array of substrings using predefined delimiters.
     * This method first attempts to split using {@link #ELEMENT_SEPARATOR}, and if that
     * results in a single element and the string contains commas, it splits by comma instead.
     * If the resulting array has elements, it also strips square brackets from the
     * first and last elements if they exist.
     *
     * @param str the string to split
     * @return an array of substrings after splitting and processing
     */
    protected static String[] split(final String str) {
        String[] strs = str.split(ELEMENT_SEPARATOR); // NOSONAR

        if ((strs.length == 1) && (str.indexOf(WD._COMMA) >= 0)) {
            strs = str.split(WD.COMMA);
        }

        final int len = strs.length;

        if (len > 0) {
            final int lastIndex = len - 1;

            if ((strs[0].charAt(0) == WD._BRACKET_L) && (strs[lastIndex].charAt(strs[lastIndex].length() - 1) == WD._BRACKET_R)) {
                strs[0] = strs[0].substring(1);

                strs[lastIndex] = strs[lastIndex].substring(0, strs[lastIndex].length() - 1);
            }
        }

        return strs;
    }
}