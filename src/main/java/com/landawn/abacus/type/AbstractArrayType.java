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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;

/**
 * The abstract base class for array types in the type system.
 * <p>
 * This class provides common functionality for handling object array types,
 * including serialization, conversion, and collection interoperability operations.
 * Concrete subclasses handle specific array element types
 * (e.g., {@code String[]}, {@code Integer[]}, {@code Object[]}).
 * </p>
 *
 * @param <T> the array type this handler represents (e.g., {@code String[]}, {@code Integer[]})
 * @see AbstractPrimitiveArrayType
 */
public abstract class AbstractArrayType<T> extends AbstractType<T> {

    /**
     * Constructs a new {@code AbstractArrayType} with the specified type name.
     *
     * @param typeName the name of the array type (e.g., "int[]", "String[]", "Object[]")
     */
    protected AbstractArrayType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns {@code true} because this type represents an array type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isArray() {
        return true;
    }

    /**
     * Returns the serialization type for this array type.
     * Returns {@link SerializationType#SERIALIZABLE} if the array type is serializable;
     * otherwise returns {@link SerializationType#ARRAY}.
     *
     * @return the {@link SerializationType} for this array type
     */
    @Override
    public SerializationType serializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.ARRAY;
    }

    /**
     * Converts the specified array to a new collection of the given collection class.
     * <p>
     * Each element from the array is added to a newly created collection instance of
     * the specified type. If the input array is {@code null}, {@code null} is returned.
     * </p>
     *
     * @param <E> the element type of the resulting collection
     * @param array the array to convert, may be {@code null}
     * @param collClass the class of the collection to create (e.g., {@code ArrayList.class}, {@code HashSet.class})
     * @return a new collection containing all elements from the array,
     *         or {@code null} if the input array is {@code null}
     * @throws IllegalArgumentException if the collection class cannot be instantiated
     */
    @Override
    public <E> Collection<E> arrayToCollection(final T array, final Class<?> collClass) {
        if (array == null) {
            return null; // NOSONAR
        }

        @SuppressWarnings("rawtypes")
        final Collection<E> result = N.newCollection((Class<Collection>) collClass, Array.getLength(array));

        arrayToCollection(array, result);

        return result;
    }

    /**
     * Splits the specified string into an array of substrings using predefined delimiters.
     * <p>
     * This method first attempts to split using the {@code ELEMENT_SEPARATOR}. If that
     * results in a single element and the string contains a comma, it falls back to
     * splitting by comma. Leading {@code [} and trailing {@code ]} brackets are stripped
     * from the first and last elements respectively, if present.
     * </p>
     *
     * @param str the string to split; must not be {@code null}
     * @return an array of trimmed substrings after splitting and bracket-stripping
     */
    protected static String[] split(final String str) {
        String[] elements = str.split(ELEMENT_SEPARATOR); // NOSONAR

        if ((elements.length == 1) && (str.indexOf(SK._COMMA) >= 0)) {
            elements = str.split(SK.COMMA);
        }

        final int len = elements.length;

        if (len > 0) {
            final int lastIndex = len - 1;

            if (!elements[0].isEmpty() && !elements[lastIndex].isEmpty() && (elements[0].charAt(0) == SK._BRACKET_L)
                    && (elements[lastIndex].charAt(elements[lastIndex].length() - 1) == SK._BRACKET_R)) {
                elements[0] = elements[0].substring(1);

                elements[lastIndex] = elements[lastIndex].substring(0, elements[lastIndex].length() - 1);
            }
        }

        return elements;
    }
}
