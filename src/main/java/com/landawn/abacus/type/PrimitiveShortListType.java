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
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link ShortList} objects, which is a primitive-backed list of {@code short} values.
 * Provides serialization and deserialization of {@link ShortList} instances by delegating to the
 * underlying {@code short[]} array type handler.
 * String representations use the format {@code [1, 2, 3]} with comma-separated elements
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveShortListType extends AbstractPrimitiveListType<ShortList> {

    /** The type name constant for {@link ShortList} type identification, equal to {@code "ShortList"}. */
    public static final String SHORT_LIST = ShortList.class.getSimpleName();

    private final Type<short[]> arrayType = Type.of(short[].class);

    private final Type<Short> elementType = Type.of(short.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveShortListType instance.
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
     */
    protected PrimitiveShortListType() {
        super(SHORT_LIST);
    }

    /**
     * Returns the Class object representing the ShortList type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * Class<ShortList> clazz = type.javaType();
     * System.out.println(clazz.getName());   // Output: com.landawn.abacus.util.ShortList
     * }</pre>
     *
     * @return the Class object for ShortList.class
     */
    @Override
    public Class<ShortList> javaType() {
        return ShortList.class;
    }

    /**
     * Returns the Type instance for the element type of this list, which is primitive short.
     * This method provides access to the Type representation of individual list elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * Type<Short> elementType = type.elementType();
     * System.out.println(elementType.name());   // Output: short
     * }</pre>
     *
     * @return the Type instance representing short type for list elements
     */
    @Override
    public Type<Short> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * List<Type<?>> paramTypes = type.parameterTypes();
     * System.out.println(paramTypes.size());          // Output: 1
     * System.out.println(paramTypes.get(0).name());   // Output: short
     * }</pre>
     *
     * @return an immutable list containing the primitive {@code short} Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a ShortList to its string representation.
     * The list is converted to an array first, then formatted as comma-separated values
     * enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
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
     * @param x the ShortList to convert to string
     * @return the string representation of the list, or {@code null} if the input list is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final ShortList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation of a short list and returns the corresponding ShortList.
     * The string should contain comma-separated short values enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = type.valueOf("[1, 2, 3]");
     * System.out.println(list.size());   // Output: 3
     * System.out.println(list.get(0));   // Output: 1
     *
     * ShortList emptyList = type.valueOf("[]");
     * System.out.println(emptyList.isEmpty());   // Output: true
     *
     * ShortList nullList = type.valueOf(null);
     * System.out.println(nullList);   // Output: null
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed ShortList, or {@code null} if the input string is {@code null} or empty
     * @throws NumberFormatException if any element in the string cannot be parsed as a short
     * @see #valueOf(Object)
     * @see #stringOf(ShortList)
     */
    @Override
    public ShortList valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final short[] array = arrayType.valueOf(str);
        return array == null ? null : ShortList.of(array);
    }

    /**
     * Appends the string representation of a ShortList to the given Appendable.
     * The list is formatted as comma-separated values enclosed in square brackets.
     * If the list is {@code null}, appends "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);
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
     * @param x the ShortList to append
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
    public void appendTo(final Appendable appendable, final ShortList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a ShortList to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The list is converted to an array and then written as comma-separated values
     * enclosed in square brackets.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ShortList> type = TypeFactory.getType(ShortList.class);
     * ShortList list = ShortList.of((short) 100, (short) 200, (short) 300);
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
     * @param x the ShortList to write
     * @param config the serialization configuration (passed through to the array type writer)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final ShortList x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
