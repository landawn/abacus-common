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
import java.util.List;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for primitive {@code double[]} arrays, providing serialization, deserialization,
 * and conversion between double arrays and their string representations or collections.
 * String representations use the format {@code [1.5, 2.7, 3.14]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveDoubleArrayType extends AbstractPrimitiveArrayType<double[]> {

    /** The type name constant for the double array type, equal to {@code "double[]"}. */
    public static final String DOUBLE_ARRAY = double[].class.getSimpleName();

    private final Type<Double> elementType;
    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a new PrimitiveDoubleArrayType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveDoubleArrayType() {
        super(DOUBLE_ARRAY);

        elementType = TypeFactory.getType(double.class);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the Class object representing the double array type.
     *
     * @return the Class object for double[]
     */
    @Override
    public Class<double[]> javaType() {
        return double[].class;
    }

    /**
     * Returns the Type object for the double element type.
     *
     * @return the Type object representing Double/double elements
     */
    @Override
    public Type<Double> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an immutable list containing the Double Type that describes the elements of this array type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a double array to its string representation.
     * The format is: [1.5, 2.7, 3.14] with elements separated by commas.
     * Returns {@code null} if the input array is {@code null}, or "[]" if the array is empty.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the double array to convert
     * @return the string representation of the array, or {@code null} if input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final double[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string representation and creates a double array.
     * Expected format: [1.5, 2.7, 3.14] or similar numeric value representations.
     * Returns {@code null} if input is {@code null}, empty, or blank, or an empty array if input is {@code "[]"}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return the parsed double array, or {@code null} if input is {@code null}, empty, or blank
     * @throws NumberFormatException if any element in the string cannot be parsed as a double
     * @see #valueOf(Object)
     * @see #stringOf(double[])
     */
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
     * Appends "null" if the array is {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the double array to append
     * @throws IOException if an I/O error occurs
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @Override
    public void appendTo(final Appendable appendable, final double[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a double array to a CharacterWriter.
     * Uses optimized write methods for better performance.
     * Writes "null" if the array is {@code null}.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the double array to write
     * @param config the serialization configuration (currently unused for double arrays)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final double[] x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Converts a Collection of Double objects to a primitive double array.
     * Each element in the collection is unboxed to its primitive double value.
     * Returns {@code null} if the input collection is {@code null}.
     *
     * @param c the Collection of Double objects to convert
     * @return a double array containing the unboxed values, or {@code null} if input is null
     */
    @Override
    public double[] collectionToArray(final Collection<?> c) {
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
     * Does nothing if the input array is {@code null} or empty.
     *
     * @param x the double array to convert
     * @param output the Collection to add the boxed Double values to
     */
    @Override
    public void arrayToCollection(final double[] x, final Collection<?> output) {
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
     * Two {@code null} arrays are considered equal.
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
