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
public final class PrimitiveIntArrayType extends AbstractPrimitiveArrayType<int[]> {

    public static final String INT_ARRAY = int[].class.getSimpleName();

    private final Type<Integer> elementType;
    private final Type<Integer>[] parameterTypes;

    PrimitiveIntArrayType() {
        super(INT_ARRAY);

        elementType = TypeFactory.getType(int.class);
        parameterTypes = new Type[] { elementType };
    }

    /**
     * Returns the Class object representing the primitive int array type (int[].class).
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * Class&lt;int[]&gt; clazz = type.clazz();
     * // clazz equals int[].class
     * </code>
     * </pre>
     *
     * @return the Class object for int[] type
     */
    @Override
    public Class<int[]> clazz() {
        return int[].class;
    }

    /**
     * Returns the Type instance for the element type of this array, which is Integer.
     * This method provides access to the Type representation of individual array elements.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * Type&lt;Integer&gt; elemType = type.getElementType();
     * // elemType can be used for element-level operations
     * </code>
     * </pre>
     *
     * @return the Type instance representing Integer type for array elements
     */
    @Override
    public Type<Integer> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * Type&lt;Integer&gt;[] paramTypes = type.getParameterTypes();
     * // paramTypes[0] represents the element type
     * </code>
     * </pre>
     *
     * @return an array containing the Integer Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Integer>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a primitive int array to its string representation.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * For example, an array {1, 2, 3} becomes "[1, 2, 3]".
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * int[] arr = {1, 2, 3};
     * String str = type.stringOf(arr);
     * // str equals "[1, 2, 3]"
     *
     * String empty = type.stringOf(new int[0]);
     * // empty equals "[]"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
     * </code>
     * </pre>
     *
     * @param x the int array to convert to string
     * @return the string representation of the array, or null if the input array is null.
     *         Returns "[]" for empty arrays.
     */
    @MayReturnNull
    @Override
    public String stringOf(final int[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation of an int array and returns the corresponding int array.
     * The string should contain comma-separated integer values enclosed in square brackets.
     * For example, "[1, 2, 3]" will be parsed to an int array {1, 2, 3}.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * int[] arr = type.valueOf("[1, 2, 3]");
     * // arr equals {1, 2, 3}
     *
     * int[] empty = type.valueOf("[]");
     * // empty.length equals 0
     *
     * int[] nullArr = type.valueOf(null);
     * // nullArr equals null
     * </code>
     * </pre>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed int array, or null if the input string is null.
     *         Returns an empty array for empty string or "[]".
     * @throws NumberFormatException if any element in the string cannot be parsed as an integer
     */
    @MayReturnNull
    @Override
    public int[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_INT_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final int[] a = new int[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     * Appends the string representation of an int array to the given Appendable.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * If the array is null, appends "null".
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * StringBuilder sb = new StringBuilder();
     * int[] arr = {10, 20, 30};
     * type.appendTo(sb, arr);
     * // sb.toString() equals "[10, 20, 30]"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
     * </code>
     * </pre>
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the int array to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final int[] x) throws IOException {
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
     * Writes the character representation of an int array to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The array is formatted as comma-separated values enclosed in square brackets.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * CharacterWriter writer = new CharacterWriter();
     * int[] arr = {5, 10, 15};
     * type.writeCharacter(writer, arr, null);
     * // Writes: [5, 10, 15]
     *
     * type.writeCharacter(writer, null, null);
     * // Writes: null
     * </code>
     * </pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the int array to write
     * @param config the serialization configuration (currently unused for primitive arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final int[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.writeInt(x[i]);
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Converts a Collection of Integer objects to a primitive int array.
     * Each element in the collection is unboxed to its primitive int value.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * List&lt;Integer&gt; list = Arrays.asList(1, 2, 3, 4);
     * int[] arr = type.collection2Array(list);
     * // arr equals {1, 2, 3, 4}
     *
     * int[] nullArr = type.collection2Array(null);
     * // nullArr equals null
     * </code>
     * </pre>
     *
     * @param c the Collection of Integer objects to convert
     * @return a primitive int array containing all elements from the collection,
     *         or null if the input collection is null
     * @throws ClassCastException if any element in the collection is not an Integer
     */
    @MayReturnNull
    @Override
    public int[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final int[] a = new int[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Integer) e;
        }

        return a;
    }

    /**
     * Converts a primitive int array to a Collection by adding all array elements to the provided collection.
     * Each primitive int value is autoboxed to an Integer object before being added.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * int[] arr = {1, 2, 3};
     * List&lt;Integer&gt; list = new ArrayList&lt;&gt;();
     * type.array2Collection(arr, list);
     * // list now contains [1, 2, 3]
     *
     * type.array2Collection(null, list);
     * // list remains unchanged
     * </code>
     * </pre>
     *
     * @param x the int array to convert
     * @param output the Collection to add the array elements to
     * @param <E> the type parameter of the output collection
     * @throws ClassCastException if the output collection cannot accept Integer objects
     */
    @Override
    public <E> void array2Collection(final int[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final int element : x) {
                c.add(element);
            }
        }
    }

    /**
     * Computes and returns the hash code for the given int array.
     * The hash code is calculated based on the contents of the array using the standard
     * array hash code algorithm, which considers all elements in the array.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * int[] arr1 = {1, 2, 3};
     * int[] arr2 = {1, 2, 3};
     * int hash1 = type.hashCode(arr1);
     * int hash2 = type.hashCode(arr2);
     * // hash1 equals hash2 (same content produces same hash)
     *
     * int nullHash = type.hashCode(null);
     * // nullHash equals 0
     * </code>
     * </pre>
     *
     * @param x the int array to compute hash code for
     * @return the hash code of the array, or 0 if the array is null
     */
    @Override
    public int hashCode(final int[] x) {
        return N.hashCode(x);
    }

    /**
     * Compares two int arrays for equality.
     * Two arrays are considered equal if they have the same length and contain the same
     * elements in the same order. Two null references are considered equal.
     *
     * <pre>
     * <code>
     * PrimitiveIntArrayType type = new PrimitiveIntArrayType();
     * int[] arr1 = {1, 2, 3};
     * int[] arr2 = {1, 2, 3};
     * int[] arr3 = {3, 2, 1};
     * boolean equal = type.equals(arr1, arr2);
     * // equal is true
     *
     * boolean notEqual = type.equals(arr1, arr3);
     * // notEqual is false
     *
     * boolean bothNull = type.equals(null, null);
     * // bothNull is true
     * </code>
     * </pre>
     *
     * @param x the first int array to compare
     * @param y the second int array to compare
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final int[] x, final int[] y) {
        return N.equals(x, y);
    }
}