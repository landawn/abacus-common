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
import java.util.List;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link LongList} objects, which is a primitive-backed list of {@code long} values.
 * Provides serialization and deserialization of {@link LongList} instances by delegating to the
 * underlying {@code long[]} array type handler.
 * String representations use the format {@code [1, 2, 3]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveLongListType extends AbstractPrimitiveListType<LongList> {

    /** The type name constant for {@link LongList} type identification, equal to {@code "LongList"}. */
    public static final String LONG_LIST = LongList.class.getSimpleName();

    private final Type<long[]> arrayType = Type.of(long[].class);

    private final Type<Long> elementType = Type.of(long.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveLongListType instance.
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
     */
    protected PrimitiveLongListType() {
        super(LONG_LIST);
    }

    /**
     * Returns the Class object representing the LongList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * Class<LongList> clazz = type.javaType();
     * System.out.println(clazz.getName());   // Output: com.landawn.abacus.util.LongList
     * }</pre>
     *
     * @return the Class object for LongList.class
     */
    @Override
    public Class<LongList> javaType() {
        return LongList.class;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive long.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * Type<Long> elementType = type.elementType();
     * System.out.println(elementType.name());   // Output: long
     * }</pre>
     *
     * @return the Type instance representing long type for list elements
     */
    @Override
    public Type<Long> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * List<Type<?>> paramTypes = type.parameterTypes();
     * System.out.println(paramTypes.size());          // Output: 1
     * System.out.println(paramTypes.get(0).name());   // Output: long
     * }</pre>
     *
     * @return an immutable list containing the primitive {@code long} Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a LongList to its string representation.
     * The list is converted to an array first, then formatted as comma-separated values
     * enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = LongList.of(1L, 2L, 3L);
     * String str = type.stringOf(list);
     * System.out.println(str);   // Output: [1, 2, 3]
     *
     * String nullStr = type.stringOf(null);
     * System.out.println(nullStr);   // Output: null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the LongList to convert to string
     * @return the string representation of the list, or {@code null} if the input list is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final LongList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation of a long list and returns the corresponding LongList.
     * The string should contain comma-separated long values enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = type.valueOf("[1, 2, 3]");
     * System.out.println(list.size());   // Output: 3
     * System.out.println(list.get(0));   // Output: 1
     *
     * LongList emptyList = type.valueOf("[]");
     * System.out.println(emptyList.isEmpty());   // Output: true
     *
     * LongList nullList = type.valueOf(null);
     * System.out.println(nullList);   // Output: null
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed LongList, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if any element in the string cannot be parsed as a long
     * @see #valueOf(Object)
     * @see #stringOf(LongList)
     */
    @Override
    public LongList valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final long[] array = arrayType.valueOf(str);
        return array == null ? null : LongList.of(array);
    }

    /**
     * Appends the string representation of a LongList to the given Appendable.
     * The list is formatted as comma-separated values enclosed in square brackets.
     * If the list is {@code null}, appends "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = LongList.of(10L, 20L, 30L);
     * StringBuilder sb = new StringBuilder("Values: ");
     * type.appendTo(sb, list);
     * System.out.println(sb.toString());   // Output: Values: [10, 20, 30]
     *
     * StringBuilder nullSb = new StringBuilder();
     * type.appendTo(nullSb, null);
     * System.out.println(nullSb.toString());   // Output: null
     * }</pre>
     *
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the LongList to append
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
    public void appendTo(final Appendable appendable, final LongList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a LongList to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The list is converted to an array and then written as comma-separated values
     * enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LongList> type = TypeFactory.getType(LongList.class);
     * LongList list = LongList.of(100L, 200L, 300L);
     * BufferedJsonWriter writer = new BufferedJsonWriter();
     * JsonXmlSerConfig<?> config = null;
     * type.serializeTo(writer, list, config);
     * System.out.println(writer.toString());   // Output: [100, 200, 300]
     *
     * BufferedJsonWriter nullWriter = new BufferedJsonWriter();
     * type.serializeTo(nullWriter, null, config);
     * System.out.println(nullWriter.toString());   // Output: null
     * }</pre>
     *
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
     * @param x the LongList to write
     * @param config the serialization configuration (passed through to the array type writer)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final LongList x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
