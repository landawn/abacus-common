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

import java.io.IOException;
import java.util.Collection;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

@SuppressWarnings("java:S2160")
public final class PrimitiveLongArrayType extends AbstractPrimitiveArrayType<long[]> {

    public static final String LONG_ARRAY = long[].class.getSimpleName();

    private final Type<Long> elementType;
    private final Type<Long>[] parameterTypes;

    PrimitiveLongArrayType() {
        super(LONG_ARRAY);

        elementType = TypeFactory.getType(long.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the primitive long array type (long[].class).
     *
     * @return the Class object for long[] type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return long[].class;
    }

    /**
     * Returns the Type instance for the element type of this array, which is Long.
     * This method provides access to the Type representation of individual array elements.
     *
     * @return the Type instance representing Long type for array elements
     */
    @Override
    public Type<Long> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Long Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Long>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a primitive long array to its string representation.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * For example, an array {1L, 2L, 3L} becomes "[1, 2, 3]".
     * 
     * @param x the long array to convert to string
     * @return the string representation of the array, or null if the input array is null.
     *         Returns "[]" for empty arrays.
     */
    @MayReturnNull
    @Override
    public String stringOf(final long[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation of a long array and returns the corresponding long array.
     * The string should contain comma-separated long values enclosed in square brackets.
     * For example, "[1, 2, 3]" will be parsed to a long array {1L, 2L, 3L}.
     * 
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed long array, or null if the input string is null.
     *         Returns an empty array for empty string or "[]".
     * @throws NumberFormatException if any element in the string cannot be parsed as a long
     */
    @MayReturnNull
    @Override
    public long[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_LONG_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final long[] a = new long[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     * Appends the string representation of a long array to the given Appendable.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * If the array is null, appends "null".
     * 
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the long array to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final long[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a long array to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * 
     * @param writer the CharacterWriter to write to
     * @param x the long array to write
     * @param config the serialization configuration (currently unused for primitive arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final long[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Converts a Collection of Long objects to a primitive long array.
     * Each element in the collection is unboxed to its primitive long value.
     * 
     * @param c the Collection of Long objects to convert
     * @return a primitive long array containing all elements from the collection,
     *         or null if the input collection is null
     * @throws ClassCastException if any element in the collection is not a Long
     */
    @MayReturnNull
    @Override
    public long[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final long[] a = new long[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Long) e;
        }

        return a;
    }

    /**
     * Converts a primitive long array to a Collection by adding all array elements to the provided collection.
     * Each primitive long value is autoboxed to a Long object before being added.
     * 
     * @param x the long array to convert
     * @param output the Collection to add the array elements to
     * @param <E> the type parameter of the output collection
     * @throws ClassCastException if the output collection cannot accept Long objects
     */
    @Override
    public <E> void array2Collection(final long[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final long element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Computes and returns the hash code for the given long array.
     * The hash code is calculated based on the contents of the array using the standard
     * array hash code algorithm, which considers all elements in the array.
     * 
     * @param x the long array to compute hash code for
     * @return the hash code of the array, or 0 if the array is null
     */
    @Override
    public int hashCode(final long[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two long arrays for equality.
     * Two arrays are considered equal if they have the same length and contain the same
     * elements in the same order. Two null references are considered equal.
     * 
     * @param x the first long array to compare
     * @param y the second long array to compare
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final long[] x, final long[] y) {
        return N.equals(x, y);
    }
}