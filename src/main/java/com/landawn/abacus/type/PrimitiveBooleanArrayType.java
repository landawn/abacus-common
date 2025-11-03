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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for primitive boolean arrays (boolean[]).
 * Provides functionality for serialization, deserialization, and conversion between
 * boolean arrays and their string representations or collections.
 */
@SuppressWarnings("java:S2160")
final class PrimitiveBooleanArrayType extends AbstractPrimitiveArrayType<boolean[]> {

    public static final String BOOLEAN_ARRAY = boolean[].class.getSimpleName();

    private final Type<Boolean> elementType;

    private final Type<Boolean>[] parameterTypes;

    PrimitiveBooleanArrayType() {
        super(BOOLEAN_ARRAY);

        elementType = TypeFactory.getType(boolean.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the boolean array type.
     *
     * @return the Class object for boolean[]
     */
    @Override
    public Class<boolean[]> clazz() {
        return boolean[].class;
    }

    /**
     * Returns the Type object for the boolean element type.
     *
     * @return the Type object representing Boolean/boolean elements
     */
    @Override
    public Type<Boolean> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Boolean Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Boolean>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a boolean array to its string representation.
     * The format is: [true, false, true] with elements separated by commas.
     * Returns {@code null} if the input array is {@code null}, or "[]" if the array is empty.
     *
     * @param x the boolean array to convert
     * @return the string representation of the array, or {@code null} if input is null
     */
    @Override
    public String stringOf(final boolean[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation and creates a boolean array.
     * Expected format: [true, false, true] or similar boolean value representations.
     * Returns {@code null} if input is {@code null}, empty array if input is empty or "[]".
     *
     * @param str the string to parse
     * @return the parsed boolean array, or {@code null} if input is null
     */
    @Override
    public boolean[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_BOOLEAN_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final boolean[] a = new boolean[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     * Appends the string representation of a boolean array to an Appendable.
     * The format is: [true, false, true] with proper element separation.
     * Appends "null" if the array is {@code null}.
     *
     * @param appendable the Appendable to write to
     * @param x the boolean array to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final boolean[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(x[i] ? TRUE_STRING : FALSE_STRING);
            }

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a boolean array to a CharacterWriter.
     * Uses optimized character arrays for true/false values for better performance.
     * Writes "null" if the array is {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the boolean array to write
     * @param config the serialization configuration (currently unused for boolean arrays)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final boolean[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i] ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY);
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Converts a Collection of Boolean objects to a primitive boolean array.
     * Each element in the collection is unboxed to its primitive boolean value.
     * Returns {@code null} if the input collection is {@code null}.
     *
     * @param c the Collection of Boolean objects to convert
     * @return a boolean array containing the unboxed values, or {@code null} if input is null
     */
    @Override
    public boolean[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final boolean[] a = new boolean[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Boolean) e;
        }

        return a;
    }

    /**
     * Converts a boolean array to a Collection.
     * Each primitive boolean value is boxed to a Boolean object and added to the output collection.
     * Does nothing if the input array is {@code null} or empty.
     *
     * @param <E> the type of elements in the output collection
     * @param x the boolean array to convert
     * @param output the Collection to add the boxed Boolean values to
     */
    @Override
    public <E> void array2Collection(final boolean[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final boolean element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Calculates the hash code for a boolean array.
     * Uses the standard Arrays.hashCode algorithm for consistency.
     *
     * @param x the boolean array to hash
     * @return the hash code of the array
     */
    @Override
    public int hashCode(final boolean[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two boolean arrays for equality.
     * Arrays are considered equal if they have the same length and all corresponding elements are equal.
     * Two {@code null} arrays are considered equal.
     *
     * @param x the first boolean array
     * @param y the second boolean array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final boolean[] x, final boolean[] y) {
        return N.equals(x, y);
    }
}
