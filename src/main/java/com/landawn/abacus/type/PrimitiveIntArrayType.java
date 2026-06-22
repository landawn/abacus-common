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
 * Type handler for primitive {@code int[]} arrays, providing serialization, deserialization,
 * and conversion between int arrays and their string representations or collections.
 * String representations use the format {@code [1, 2, 3]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveIntArrayType extends AbstractPrimitiveArrayType<int[]> {

    /** The type name constant for the int array type, equal to {@code "int[]"}. */
    public static final String INT_ARRAY = int[].class.getSimpleName();

    private final Type<Integer> elementType;
    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a new PrimitiveIntArrayType instance.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PrimitiveIntArrayType() {
        super(INT_ARRAY);

        elementType = TypeFactory.getType(int.class);
        parameterTypes = List.of(elementType);
    }

    /**
     * Returns the Class object representing the primitive int array type (int[].class).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * Class<int[]> clazz = type.javaType();
     * // clazz equals int[].class
     * }</pre>
     *
     * @return the Class object for int[] type
     */
    @Override
    public Class<int[]> javaType() {
        return int[].class;
    }

    /**
     * Returns the Type instance for the element type of this array, which is primitive {@code int}.
     * This method provides access to the Type representation of individual array elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * Type<Integer> elemType = type.elementType();
     * // elemType can be used for element-level operations
     * }</pre>
     *
     * @return the Type instance representing primitive {@code int} type for array elements
     */
    @Override
    public Type<Integer> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * List<Type<?>> paramTypes = type.parameterTypes();
     * // paramTypes.get(0) represents the element type
     * }</pre>
     *
     * @return an immutable list containing the primitive {@code int} Type that describes the elements of this array type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a primitive int array to its string representation.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * For example, an array {1, 2, 3} becomes "[1, 2, 3]".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * int[] arr = {1, 2, 3};
     * String str = type.stringOf(arr);
     * // str equals "[1, 2, 3]"
     *
     * String empty = type.stringOf(new int[0]);
     * // empty equals "[]"
     *
     * String nullStr = type.stringOf(null);
     * // nullStr equals null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the int array to convert to string
     * @return the string representation of the array, or {@code null} if the input array is {@code null}.
     *         Returns "[]" for empty arrays.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final int[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string representation of an int array and returns the corresponding int array.
     * The string should contain comma-separated integer values enclosed in square brackets.
     * For example, "[1, 2, 3]" will be parsed to an int array {1, 2, 3}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * int[] arr = type.valueOf("[1, 2, 3]");
     * // arr equals {1, 2, 3}
     *
     * int[] empty = type.valueOf("[]");
     * // empty.length equals 0
     *
     * int[] nullArr = type.valueOf(null);
     * // nullArr equals null
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed int array, or {@code null} if the input string is {@code null}, empty, or blank.
     *         Returns an empty array for "[]".
     * @throws NumberFormatException if any element in the string cannot be parsed as an integer
     * @see #valueOf(Object)
     * @see #stringOf(int[])
     */
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
     * If the array is {@code null}, appends "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * StringBuilder sb = new StringBuilder();
     * int[] arr = {10, 20, 30};
     * type.appendTo(sb, arr);
     * // sb.toString() equals "[10, 20, 30]"
     *
     * StringBuilder sb2 = new StringBuilder();
     * type.appendTo(sb2, null);
     * // sb2.toString() equals "null"
     * }</pre>
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the int array to append
     * @throws IOException if an I/O error occurs during the append operation
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
    public void appendTo(final Appendable appendable, final int[] x) throws IOException {
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
     * Writes the character representation of an int array to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The array is formatted as comma-separated values enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * BufferedJsonWriter writer = new BufferedJsonWriter();
     * int[] arr = {5, 10, 15};
     * type.serializeTo(writer, arr, null);
     * // Writes: [5, 10, 15]
     *
     * type.serializeTo(writer, null, null);
     * // Writes: null
     * }</pre>
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
     * @param x the int array to write
     * @param config the serialization configuration (currently unused for primitive arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final int[] x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.writeInt(x[i]);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Converts a Collection of Integer objects to a primitive int array.
     * Each element in the collection is unboxed to its primitive int value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * List<Integer> list = Arrays.asList(1, 2, 3, 4);
     * int[] arr = type.collectionToArray(list);
     * // arr equals {1, 2, 3, 4}
     *
     * int[] nullArr = type.collectionToArray(null);
     * // nullArr equals null
     * }</pre>
     *
     * @param c the Collection of Integer objects to convert
     * @return a primitive int array containing all elements from the collection,
     *         or {@code null} if the input collection is null
     * @throws ClassCastException if any element in the collection is not an Integer
     */
    @Override
    public int[] collectionToArray(final Collection<?> c) {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * int[] arr = {1, 2, 3};
     * List<Integer> list = new ArrayList<>();
     * type.arrayToCollection(arr, list);
     * // list now contains [1, 2, 3]
     *
     * type.arrayToCollection(null, list);
     * // list remains unchanged
     * }</pre>
     *
     * @param x the int array to convert
     * @param output the Collection to add the array elements to
     * @param <E> the element type accepted by the output collection
     * @throws ClassCastException if the output collection cannot accept Integer objects
     */
    @Override
    public <E> void arrayToCollection(final int[] x, final Collection<E> output) {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
     * int[] arr1 = {1, 2, 3};
     * int[] arr2 = {1, 2, 3};
     * int hash1 = type.hashCode(arr1);
     * int hash2 = type.hashCode(arr2);
     * // hash1 equals hash2 (same content produces same hash)
     *
     * int nullHash = type.hashCode(null);
     * // nullHash equals 0
     * }</pre>
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
     * elements in the same order. Two {@code null} references are considered equal.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<int[]> type = TypeFactory.getType(int[].class);
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
     * }</pre>
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
