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

public final class ShortArrayType extends ObjectArrayType<Short> {

    /**
     * Constructs a new ShortArrayType instance for handling Short object arrays.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    ShortArrayType() {
        super(Short[].class);
    }

    /**
     * Converts a Short object array to its string representation.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * Null elements are represented as "null" in the output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short[]> type = TypeFactory.getType(Short[].class);
     * Short[] array = {1, null, 3};
     * String str = type.stringOf(array);   // Returns "[1, null, 3]"
     * }</pre>
     *
     * @param x the Short array to convert to string
     * @return the string representation of the array, or {@code null} if the input array is {@code null}.
     *         Returns "[]" for empty arrays.
     */
    @Override
    public String stringOf(final Short[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation of a Short array and returns the corresponding Short array.
     * The string should contain comma-separated values enclosed in square brackets.
     * The string "null" (case-sensitive, exactly 4 characters) is parsed as a {@code null} element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short[]> type = TypeFactory.getType(Short[].class);
     * Short[] array = type.valueOf("[1, null, 3]");   // Returns {1, null, 3}
     * }</pre>
     *
     * @param str the string to parse, expected format is "[value1, value2, ...]"
     * @return the parsed Short array, or {@code null} if the input string is {@code null}.
     *         Returns an empty array for empty string or "[]".
     * @throws NumberFormatException if any {@code non-null} element in the string cannot be parsed as a short
     */
    @Override
    public Short[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_SHORT_OBJ_ARRAY;
        }

        final String[] elements = split(str);
        final int len = elements.length;
        final Short[] array = new Short[len];

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
     * Appends the string representation of a Short array to the given Appendable.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * Null elements are appended as "null". If the array itself is {@code null}, appends "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short[]> type = TypeFactory.getType(Short[].class);
     * StringBuilder sb = new StringBuilder();
     * type.appendTo(sb, new Short[] {1, 2, 3});   // Appends "[1, 2, 3]"
     * }</pre>
     *
     * @param appendable the Appendable to write to (e.g., StringBuilder, Writer)
     * @param x the Short array to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Short[] x) throws IOException {
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
     * Writes the character representation of a Short array to the given CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The array is formatted as comma-separated values enclosed in square brackets.
     * Null elements are written as "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Short[]> type = TypeFactory.getType(Short[].class);
     * CharacterWriter writer = new CharacterWriter();
     * type.writeCharacter(writer, new Short[] {1, 2, 3}, config);   // Writes "[1, 2, 3]"
     * }</pre>
     *
     * @param writer the CharacterWriter to write to
     * @param x the Short array to write
     * @param config the serialization configuration (currently unused for Short arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Short[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
