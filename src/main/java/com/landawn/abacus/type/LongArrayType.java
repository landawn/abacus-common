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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@code Long[]} arrays, providing serialization and deserialization support.
 * Arrays are represented as bracket-enclosed, comma-separated lists (e.g., {@code [1, 2, 3]}).
 * Null elements within the array are serialized as the literal string {@code "null"}.
 */
public final class LongArrayType extends ObjectArrayType<Long> {

    /**
     * Package-private constructor for LongArrayType.
     * This constructor is called by the TypeFactory to create Long[] type instances.
     */
    LongArrayType() {
        super(Long[].class);
    }

    /**
     * Converts a Long array to its string representation.
     * The array is formatted as a comma-separated list of values enclosed in square brackets.
     * {@code null} elements in the array are represented as the literal string {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long[]> type = TypeFactory.getType(Long[].class);
     *
     * Long[] array = {1L, 2L, 3L};
     * String result = type.stringOf(array);
     * // Returns: "[1, 2, 3]"
     *
     * Long[] withNull = {1L, null, 3L};
     * result = type.stringOf(withNull);
     * // Returns: "[1, null, 3]"
     *
     * Long[] empty = {};
     * result = type.stringOf(empty);
     * // Returns: "[]"
     *
     * result = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x The Long array to convert
     * @return the string representation of the array in the format {@code "[value1, value2, ...]"},
     *         or {@code null} if the input array is {@code null}, or {@code "[]"} if the array is empty
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Long[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string to create a Long array.
     * The string should be in the format "[value1, value2, ...]" where each value is either a long number or "null".
     * The method handles:
     * <ul>
     *   <li>{@code null}, empty, or blank input returns {@code null}</li>
     *   <li>{@code "[]"} returns an empty {@code Long[]} array</li>
     *   <li>Values of {@code "null"} (case-sensitive, exactly 4 characters) are converted to {@code null} elements</li>
     *   <li>Other values are parsed as {@code Long} objects</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long[]> type = TypeFactory.getType(Long[].class);
     *
     * Long[] result = type.valueOf("[1, 2, 3]");
     * // Returns: Long[] {1L, 2L, 3L}
     *
     * result = type.valueOf("[1, null, 3]");
     * // Returns: Long[] {1L, null, 3L}
     *
     * result = type.valueOf("[]");
     * // Returns: empty Long array
     *
     * result = type.valueOf(null);
     * // Returns: null
     *
     * result = type.valueOf("");
     * // Returns: null
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str The string to parse
     * @return The parsed Long array, or {@code null} if the input is {@code null}, empty, or blank
     * @throws NumberFormatException if any non-{@code null} value cannot be parsed as a {@code Long}
     * @see #valueOf(Object)
     * @see #stringOf(Long[])
     */
    @Override
    public Long[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_LONG_OBJ_ARRAY;
        }

        final String[] elements = split(str);
        final int len = elements.length;
        final Long[] array = new Long[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                if (elements[i].length() == 4 && elements[i].equals(NULL_STRING)) {
                    array[i] = null;
                } else {
                    array[i] = elementType.valueOf(elements[i]);
                }
            }
        }

        return array;
    }

    /**
     * Appends the string representation of a Long array to an Appendable.
     * The array is formatted as a comma-separated list of values enclosed in square brackets.
     * A {@code null} array is represented as the literal string {@code "null"}, and each
     * {@code null} element is also represented as the literal string {@code "null"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long[]> type = TypeFactory.getType(Long[].class);
     * StringBuilder sb = new StringBuilder();
     *
     * Long[] array = {1L, 2L, 3L};
     * type.appendTo(sb, array);
     * // sb contains: "[1, 2, 3]"
     *
     * sb.setLength(0);
     * Long[] withNull = {1L, null, 3L};
     * type.appendTo(sb, withNull);
     * // sb contains: "[1, null, 3]"
     *
     * sb.setLength(0);
     * type.appendTo(sb, null);
     * // sb contains: "null"
     * }</pre>
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable The Appendable to write to
     * @param x The Long array to append
     * @throws IOException if an I/O error occurs while appending
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
    public void appendTo(final Appendable appendable, final Long[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                if (x[i] == null) {
                    appendable.append(NULL_STRING);
                } else {
                    appendable.append(x[i].toString());
                }
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes the character representation of a Long array to a CharacterWriter.
     * The array is formatted as a comma-separated list of values enclosed in square brackets.
     * This method is optimized for character-based writing and may be more efficient than appendTo
     * for certain output scenarios.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Long[]> type = TypeFactory.getType(Long[].class);
     * BufferedJsonWriter writer = new BufferedJsonWriter();
     * JsonXmlSerConfig<?> config = null;
     *
     * Long[] array = {100L, 200L, 300L};
     * type.serializeTo(writer, array, config);
     * String result = writer.toString();
     * // result: "[100, 200, 300]"
     *
     * BufferedJsonWriter writer2 = new BufferedJsonWriter();
     * type.serializeTo(writer2, null, config);
     * String result2 = writer2.toString();
     * // result2: "null"
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
     * @param writer The CharacterWriter to write to
     * @param x The Long array to write
     * @param config The serialization configuration (currently unused for Long arrays)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Long[] x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                if (x[i] == null) {
                    writer.write(NULL_CHAR_ARRAY);
                } else {
                    elementType.serializeTo(writer, x[i], config);
                }
            }

            writer.write(SK._BRACKET_R);
        }
    }
}
