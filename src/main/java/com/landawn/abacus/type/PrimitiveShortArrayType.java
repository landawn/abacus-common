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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * Class clazz = type.clazz();
     * System.out.println(clazz.getName());   // Output: [S
     * System.out.println(clazz.isArray());   // Output: true
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * Type<Short> elementType = type.getElementType();
     * System.out.println(elementType.name());   // Output: short
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * Type<Short>[] paramTypes = type.getParameterTypes();
     * System.out.println(paramTypes.length);      // Output: 1
     * System.out.println(paramTypes[0].name());   // Output: short
     * }</pre>
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * short[] array = {1, 2, 3};
     * String str = type.stringOf(array);
     * System.out.println(str);   // Output: [1, 2, 3]
     *
     * short[] emptyArray = {};
     * System.out.println(type.stringOf(emptyArray));   // Output: []
     *
     * System.out.println(type.stringOf(null));   // Output: null
     * }</pre>
     *
     * @param x the short array to convert to string
     * @return the string representation of the array, or {@code null} if the input array is {@code null}.
     *         Returns "[]" for empty arrays.
     */
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
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * short[] array = type.valueOf("[1, 2, 3]");
     * System.out.println(array.length);   // Output: 3
     * System.out.println(array[0]);       // Output: 1
     *
     * short[] emptyArray = type.valueOf("[]");
     * System.out.println(emptyArray.length);   // Output: 0
     *
     * short[] nullArray = type.valueOf(null);
     * System.out.println(nullArray);   // Output: null
     * }</pre>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed short array, or {@code null} if the input string is {@code null}.
     *         Returns an empty array for empty string or "[]".
     * @throws NumberFormatException if any element in the string cannot be parsed as a short
     */
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
     * If the array is {@code null}, appends "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * short[] array = {10, 20, 30};
     * StringBuilder sb = new StringBuilder("Values: ");
     * type.appendTo(sb, array);
     * System.out.println(sb.toString());   // Output: Values: [10, 20, 30]
     *
     * StringBuilder nullSb = new StringBuilder();
     * type.appendTo(nullSb, null);
     * System.out.println(nullSb.toString());   // Output: null
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * short[] array = {100, 200, 300};
     * CharacterWriter writer = new CharacterWriter();
     * JsonXmlSerializationConfig<?> config = new JsonXmlSerializationConfig<>();
     * type.writeCharacter(writer, array, config);
     * System.out.println(writer.toString());   // Output: [100, 200, 300]
     *
     * CharacterWriter nullWriter = new CharacterWriter();
     * type.writeCharacter(nullWriter, null, config);
     * System.out.println(nullWriter.toString());   // Output: null
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the short array to write
     * @param config the serialization configuration (currently unused for primitive arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final short[] x, final JsonXmlSerializationConfig<?> config) throws IOException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * List<Short> list = Arrays.asList((short) 5, (short) 10, (short) 15);
     * short[] array = type.collectionToArray(list);
     * System.out.println(Arrays.toString(array));   // Output: [5, 10, 15]
     *
     * short[] nullArray = type.collectionToArray(null);
     * System.out.println(nullArray);   // Output: null
     * }</pre>
     *
     * @param c the Collection of Short objects to convert
     * @return a primitive short array containing all elements from the collection,
     *         or {@code null} if the input collection is null
     * @throws ClassCastException if any element in the collection is not a Short
     */
    @Override
    public short[] collectionToArray(final Collection<?> c) {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * short[] array = {1, 2, 3};
     * List<Short> list = new ArrayList<>();
     * type.arrayToCollection(array, list);
     * System.out.println(list);   // Output: [1, 2, 3]
     *
     * Set<Short> set = new HashSet<>();
     * type.arrayToCollection(array, set);
     * System.out.println(set);   // Output: [1, 2, 3]
     * }</pre>
     *
     * @param x the short array to convert
     * @param output the Collection to add the array elements to
     * @param <E> the type parameter of the output collection
     * @throws ClassCastException if the output collection cannot accept Short objects
     */
    @Override
    public <E> void arrayToCollection(final short[] x, final Collection<E> output) {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * short[] array1 = {1, 2, 3};
     * short[] array2 = {1, 2, 3};
     * int hash1 = type.hashCode(array1);
     * int hash2 = type.hashCode(array2);
     * System.out.println(hash1 == hash2);   // Output: true
     *
     * int nullHash = type.hashCode(null);
     * System.out.println(nullHash);   // Output: 0
     * }</pre>
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
     * elements in the same order. Two {@code null} references are considered equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<short[]> type = TypeFactory.getType(short[].class);
     * short[] array1 = {1, 2, 3};
     * short[] array2 = {1, 2, 3};
     * short[] array3 = {1, 2, 4};
     * System.out.println(type.equals(array1, array2));   // Output: true
     * System.out.println(type.equals(array1, array3));   // Output: false
     * System.out.println(type.equals(null, null));       // Output: true
     * }</pre>
     *
     * @param x the first short array to compare
     * @param y the second short array to compare
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final short[] x, final short[] y) {
        return N.equals(x, y);
    }
}
