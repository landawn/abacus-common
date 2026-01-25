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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Boolean array operations.
 * This class provides serialization/deserialization and I/O operations
 * for Boolean[] arrays, handling {@code null} elements and array formatting.
 */
public final class BooleanArrayType extends ObjectArrayType<Boolean> {

    BooleanArrayType() {
        super(Boolean[].class);
    }

    /**
     * Converts a Boolean array to its string representation.
     * The array is formatted with square brackets and comma-separated elements.
     * Null elements are represented as "null" in the output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Boolean[]> type = TypeFactory.getType(Boolean[].class);
     * Boolean[] array = {true, false, null, true};
     * String result = type.stringOf(array);
     * // result: "[true, false, null, true]"
     *
     * String empty = type.stringOf(new Boolean[0]);
     * // empty: "[]"
     *
     * String nullResult = type.stringOf(null);
     * // nullResult: null
     * }</pre>
     *
     * @param x the Boolean array to convert
     * @return a string representation like "[true, false, null]", or {@code null} if input is {@code null},
     *         or "[]" if the array is empty
     */
    @Override
    public String stringOf(final Boolean[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Converts a string representation back to a Boolean array.
     * Parses a string in the format "[true, false, null]" into a Boolean array.
     * The string "null" (case-sensitive) is parsed as a {@code null} element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Boolean[]> type = TypeFactory.getType(Boolean[].class);
     * Boolean[] result = type.valueOf("[true, false, null, true]");
     * // result: {true, false, null, true}
     *
     * Boolean[] empty = type.valueOf("[]");
     * // empty: {} (empty array)
     *
     * Boolean[] nullResult = type.valueOf(null);
     * // nullResult: null
     * }</pre>
     *
     * @param str the string to parse, expecting format like "[true, false, null]"
     * @return a Boolean array parsed from the string, or {@code null} if str is {@code null},
     *         or an empty array if str is empty or equals "[]"
     */
    @Override
    public Boolean[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_BOOLEAN_OBJ_ARRAY;
        }

        final String[] elements = split(str);
        final int len = elements.length;
        final Boolean[] array = new Boolean[len];

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
     * Appends a Boolean array to an Appendable object.
     * Formats the array with square brackets and comma-separated elements.
     * Null array elements are appended as "null", and boolean values as "true" or "false".
     *
     * @param appendable the Appendable object to append to
     * @param x the Boolean array to append, may be null
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean[] x) throws IOException {
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
                    appendable.append(x[i] ? TRUE_STRING : FALSE_STRING); //NOSONAR
                }
            }

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     * Writes a Boolean array to a CharacterWriter.
     * Uses optimized character arrays for boolean values to improve performance.
     * The output format matches the string representation with square brackets.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Boolean array to write, may be null
     * @param config the serialization configuration (not used for boolean arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Boolean[] x, final JsonXmlSerializationConfig<?> config) throws IOException {
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
                    writer.write(x[i] ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY); //NOSONAR
                }
            }

            writer.write(WD._BRACKET_R);
        }
    }
}
