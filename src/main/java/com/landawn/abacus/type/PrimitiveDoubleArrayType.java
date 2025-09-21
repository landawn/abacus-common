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

/**
 * Type handler for primitive double arrays (double[]).
 * Provides functionality for serialization, deserialization, and conversion between
 * double arrays and their string representations or collections.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveDoubleArrayType extends AbstractPrimitiveArrayType<double[]> {

    public static final String DOUBLE_ARRAY = double[].class.getSimpleName();

    private final Type<Double> elementType;
    private final Type<Double>[] parameterTypes;

    PrimitiveDoubleArrayType() {
        super(DOUBLE_ARRAY);

        elementType = TypeFactory.getType(double.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the double array type.
     *
     * @return the Class object for double[]
     */
    @Override
    public Class<double[]> clazz() {
        return double[].class;
    }

    /**
     * Returns the Type object for the double element type.
     *
     * @return the Type object representing Double/double elements
     */
    @Override
    public Type<Double> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Double Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Double>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a double array to its string representation.
     * The format is: [1.5, 2.7, 3.14] with elements separated by commas.
     * Returns null if the input array is null, or "[]" if the array is empty.
     *
     * @param x the double array to convert
     * @return the string representation of the array, or null if input is null
     */
    @MayReturnNull
    @Override
    public String stringOf(final double[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 8));
        //
        //    sb.append(WD._BRACKET_L);
        //
        //    for (int i = 0, len = x.length; i < len; i++) {
        //        if (i > 0) {
        //            sb.append(ELEMENT_SEPARATOR);
        //        }
        //
        //        sb.append(x[i]);
        //    }
        //
        //    sb.append(WD._BRACKET_R);
        //
        //    final String str = sb.toString();
        //
        //    Objectory.recycle(sb);
        //
        //    return str;

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation and creates a double array.
     * Expected format: [1.5, 2.7, 3.14] or similar numeric value representations.
     * Returns null if input is null, empty array if input is empty or "[]".
     *
     * @param str the string to parse
     * @return the parsed double array, or null if input is null
     */
    @MayReturnNull
    @Override
    public double[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_DOUBLE_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final double[] a = new double[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     * Appends the string representation of a double array to an Appendable.
     * The format is: [1.5, 2.7, 3.14] with proper element separation.
     * Appends "null" if the array is null.
     *
     * @param appendable the Appendable to write to
     * @param x the double array to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final double[] x) throws IOException {
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
     * Writes the character representation of a double array to a CharacterWriter.
     * Uses optimized write methods for better performance.
     * Writes "null" if the array is null.
     *
     * @param writer the CharacterWriter to write to
     * @param x the double array to write
     * @param config the serialization configuration (currently unused for double arrays)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final double[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
     * Converts a Collection of Double objects to a primitive double array.
     * Each element in the collection is unboxed to its primitive double value.
     * Returns null if the input collection is null.
     *
     * @param c the Collection of Double objects to convert
     * @return a double array containing the unboxed values, or null if input is null
     */
    @MayReturnNull
    @Override
    public double[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final double[] a = new double[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Double) e;
        }

        return a;
    }

    /**
     * Converts a double array to a Collection.
     * Each primitive double value is boxed to a Double object and added to the output collection.
     * Does nothing if the input array is null or empty.
     *
     * @param <E> the type of elements in the output collection
     * @param x the double array to convert
     * @param output the Collection to add the boxed Double values to
     */
    @Override
    public <E> void array2Collection(final double[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final double element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Calculates the hash code for a double array.
     * Uses the standard Arrays.hashCode algorithm for consistency.
     *
     * @param x the double array to hash
     * @return the hash code of the array
     */
    @Override
    public int hashCode(final double[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two double arrays for equality.
     * Arrays are considered equal if they have the same length and all corresponding elements are equal.
     * Two null arrays are considered equal.
     *
     * @param x the first double array
     * @param y the second double array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final double[] x, final double[] y) {
        return N.equals(x, y);
    }
}