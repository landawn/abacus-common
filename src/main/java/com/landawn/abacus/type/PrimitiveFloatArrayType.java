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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for primitive float arrays (float[]).
 * Provides functionality for serialization, deserialization, and conversion between
 * float arrays and their string representations or collections.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveFloatArrayType extends AbstractPrimitiveArrayType<float[]> {

    public static final String FLOAT_ARRAY = float[].class.getSimpleName();

    private final Type<Float> elementType;
    private final Type<Float>[] parameterTypes;

    PrimitiveFloatArrayType() {
        super(FLOAT_ARRAY);

        elementType = TypeFactory.getType(float.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the float array type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * Class<float[]> clazz = type.clazz();
     * // Returns: float[].class
     * }</pre>
     *
     * @return the Class object for float[]
     */
    @Override
    public Class<float[]> clazz() {
        return float[].class;
    }

    /**
     * Returns the Type object for the float element type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * Type<Float> elementType = type.getElementType();
     * // Returns: Type instance for float
     * }</pre>
     *
     * @return the Type object representing Float/float elements
     */
    @Override
    public Type<Float> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Float Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Float>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a float array to its string representation.
     * The format is: [1.5, 2.7, 3.14] with elements separated by commas.
     * Returns {@code null} if the input array is {@code null}, or "[]" if the array is empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * float[] array = {1.5f, 2.7f, 3.14f};
     * String result = type.stringOf(array);
     * // Returns: "[1.5, 2.7, 3.14]"
     *
     * String empty = type.stringOf(new float[0]);
     * // Returns: "[]"
     *
     * String nullResult = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x the float array to convert
     * @return the string representation of the array, or {@code null} if input is null
     */
    @Override
    public String stringOf(final float[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation and creates a float array.
     * Expected format: [1.5, 2.7, 3.14] or similar numeric value representations.
     * Returns {@code null} if input is {@code null}, empty array if input is empty or "[]".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * float[] result = type.valueOf("[1.5, 2.7, 3.14]");
     * // Returns: {1.5f, 2.7f, 3.14f}
     *
     * float[] empty = type.valueOf("[]");
     * // Returns: {} (empty array)
     *
     * float[] nullResult = type.valueOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param str the string to parse
     * @return the parsed float array, or {@code null} if input is null
     */
    @Override
    public float[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_FLOAT_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final float[] a = new float[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     * Appends the string representation of a float array to an Appendable.
     * The format is: [1.5, 2.7, 3.14] with proper element separation.
     * Appends "null" if the array is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * StringBuilder sb = new StringBuilder();
     * float[] array = {1.5f, 2.7f, 3.14f};
     * type.appendTo(sb, array);
     * // sb now contains: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * @param appendable the Appendable to write to
     * @param x the float array to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final float[] x) throws IOException {
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
     * Writes the character representation of a float array to a CharacterWriter.
     * Uses optimized write methods for better performance.
     * Writes "null" if the array is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * CharacterWriter writer = new CharacterWriter();
     * float[] array = {1.5f, 2.7f, 3.14f};
     * type.writeCharacter(writer, array, null);
     * // Writer contains: "[1.5, 2.7, 3.14]"
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the float array to write
     * @param config the serialization configuration (currently unused for float arrays)
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final float[] x, final JsonXmlSerializationConfig<?> config) throws IOException {
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
     * Converts a Collection of Float objects to a primitive float array.
     * Each element in the collection is unboxed to its primitive float value.
     * Returns {@code null} if the input collection is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * List<Float> list = Arrays.asList(1.5f, 2.7f, 3.14f);
     * float[] array = type.collectionToArray(list);
     * // Returns: {1.5f, 2.7f, 3.14f}
     * }</pre>
     *
     * @param c the Collection of Float objects to convert
     * @return a float array containing the unboxed values, or {@code null} if input is null
     */
    @Override
    public float[] collectionToArray(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final float[] a = new float[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Float) e;
        }

        return a;
    }

    /**
     * Converts a float array to a Collection.
     * Each primitive float value is boxed to a Float object and added to the output collection.
     * Does nothing if the input array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * float[] array = {1.5f, 2.7f, 3.14f};
     * List<Float> list = new ArrayList<>();
     * type.arrayToCollection(array, list);
     * // list now contains: [1.5f, 2.7f, 3.14f]
     * }</pre>
     *
     * @param <E> the type of elements in the output collection
     * @param x the float array to convert
     * @param output the Collection to add the boxed Float values to
     */
    @Override
    public <E> void arrayToCollection(final float[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final float element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Calculates the hash code for a float array.
     * Uses the standard Arrays.hashCode algorithm for consistency.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * float[] array = {1.5f, 2.7f, 3.14f};
     * int hash = type.hashCode(array);
     * // Returns the hash code for the array
     * }</pre>
     *
     * @param x the float array to hash
     * @return the hash code of the array
     */
    @Override
    public int hashCode(final float[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two float arrays for equality.
     * Arrays are considered equal if they have the same length and all corresponding elements are equal.
     * Two {@code null} arrays are considered equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<float[]> type = TypeFactory.getType(float[].class);
     * float[] array1 = {1.5f, 2.7f, 3.14f};
     * float[] array2 = {1.5f, 2.7f, 3.14f};
     * boolean equal = type.equals(array1, array2);
     * // Returns: true
     *
     * float[] array3 = {1.5f, 2.8f};
     * equal = type.equals(array1, array3);
     * // Returns: false
     * }</pre>
     *
     * @param x the first float array
     * @param y the second float array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final float[] x, final float[] y) {
        return N.equals(x, y);
    }
}
