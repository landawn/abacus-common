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
public final class PrimitiveShortArrayType extends AbstractPrimitiveArrayType<short[]> {

    public static final String SHORT_ARRAY = short[].class.getSimpleName();

    private final Type<Short> elementType;
    private final Type<Short>[] parameterTypes;

    PrimitiveShortArrayType() {
        super(SHORT_ARRAY);

        elementType = TypeFactory.getType(short.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the primitive short array type (short[].class).
     *
     * @return the Class object for short[] type
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return short[].class;
    }

    /**
     * Returns the Type instance for the element type of this array, which is Short.
     * This method provides access to the Type representation of individual array elements.
     *
     * @return the Type instance representing Short type for array elements
     */
    @Override
    public Type<Short> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Short Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Short>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a primitive short array to its string representation.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * For example, an array {1, 2, 3} becomes "[1, 2, 3]".
     * 
     * @param x the short array to convert to string
     * @return the string representation of the array, or null if the input array is null.
     *         Returns "[]" for empty arrays.
     */
    @MayReturnNull
    @Override
    public String stringOf(final short[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation of a short array and returns the corresponding short array.
     * The string should contain comma-separated short values enclosed in square brackets.
     * For example, "[1, 2, 3]" will be parsed to a short array {1, 2, 3}.
     * 
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed short array, or null if the input string is null.
     *         Returns an empty array for empty string or "[]".
     * @throws NumberFormatException if any element in the string cannot be parsed as a short
     */
    @MayReturnNull
    @Override
    public short[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_SHORT_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final short[] a = new short[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     * Appends the string representation of a short array to the given Appendable.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * If the array is null, appends "null".
     * 
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the short array to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final short[] x) throws IOException {
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
     * Writes the character representation of a short array to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * 
     * @param writer the CharacterWriter to write to
     * @param x the short array to write
     * @param config the serialization configuration (currently unused for primitive arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final short[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
     * Converts a Collection of Short objects to a primitive short array.
     * Each element in the collection is unboxed to its primitive short value.
     * 
     * @param c the Collection of Short objects to convert
     * @return a primitive short array containing all elements from the collection,
     *         or null if the input collection is null
     * @throws ClassCastException if any element in the collection is not a Short
     */
    @MayReturnNull
    @Override
    public short[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final short[] a = new short[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Short) e;
        }

        return a;
    }

    /**
     * Converts a primitive short array to a Collection by adding all array elements to the provided collection.
     * Each primitive short value is autoboxed to a Short object before being added.
     * 
     * @param x the short array to convert
     * @param output the Collection to add the array elements to
     * @param <E> the type parameter of the output collection
     * @throws ClassCastException if the output collection cannot accept Short objects
     */
    @Override
    public <E> void array2Collection(final short[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final short element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Computes and returns the hash code for the given short array.
     * The hash code is calculated based on the contents of the array using the standard
     * array hash code algorithm, which considers all elements in the array.
     * 
     * @param x the short array to compute hash code for
     * @return the hash code of the array, or 0 if the array is null
     */
    @Override
    public int hashCode(final short[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two short arrays for equality.
     * Two arrays are considered equal if they have the same length and contain the same
     * elements in the same order. Two null references are considered equal.
     * 
     * @param x the first short array to compare
     * @param y the second short array to compare
     * @return true if the arrays are equal, false otherwise
     */
    @Override
    public boolean equals(final short[] x, final short[] y) {
        return N.equals(x, y);
    }
}