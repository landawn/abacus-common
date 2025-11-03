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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

public final class LongArrayType extends ObjectArrayType<Long> {

    LongArrayType() {
        super(Long[].class);
    }

    /**
     * Converts a Long array to its string representation.
     * The array is formatted as a comma-separated list of values enclosed in square brackets.
     * Null values in the array are represented as "null".
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
     * @param x The Long array to convert
     * @return The string representation of the array in format "[value1, value2, ...]",
     *         or {@code null} if the input array is {@code null}, or "[]" if the array is empty
     */
    @Override
    public String stringOf(final Long[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string to create a Long array.
     * The string should be in the format "[value1, value2, ...]" where each value is either a long number or "null".
     * The method handles:
     * - {@code null} input returns null
     * - Empty string or "[]" returns an empty Long array
     * - Values of "null" (case-sensitive, exactly 4 characters) are converted to {@code null} elements
     * - Other values are parsed as Long objects
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
     * @param str The string to parse
     * @return The parsed Long array, or {@code null} if the input is null
     * @throws NumberFormatException if any {@code non-null} value cannot be parsed as a Long
     */
    @Override
    public Long[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_LONG_OBJ_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final Long[] a = new Long[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                if (strs[i].length() == 4 && strs[i].equals(NULL_STRING)) {
                    a[i] = null;
                } else {
                    a[i] = elementType.valueOf(strs[i]);
                }
            }
        }

        return a;
    }

    /**
     * Appends the string representation of a Long array to an Appendable.
     * The array is formatted as a comma-separated list of values enclosed in square brackets.
     * Null array is represented as "null", {@code null} elements are represented as "null".
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
     *
     * @param appendable The Appendable to write to
     * @param x The Long array to append
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final Long[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

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

            appendable.append(WD._BRACKET_R);
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
     * CharacterWriter writer = new CharacterWriter();
     * JSONXMLSerializationConfig config = JSONXMLSerializationConfig.of();
     *
     * Long[] array = {100L, 200L, 300L};
     * type.writeCharacter(writer, array, config);
     * String result = writer.toString();
     * // result: "[100, 200, 300]"
     *
     * writer.reset();
     * type.writeCharacter(writer, null, config);
     * result = writer.toString();
     * // result: "null"
     * }</pre>
     *
     * @param writer The CharacterWriter to write to
     * @param x The Long array to write
     * @param config The serialization configuration (currently unused for Long arrays)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Long[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                if (x[i] == null) {
                    writer.write(NULL_CHAR_ARRAY);
                } else {
                    writer.write(x[i]);
                }
            }

            writer.write(WD._BRACKET_R);
        }
    }
}
