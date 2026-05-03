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
 * Type handler for {@code Boolean[]} (boxed Boolean array) values.
 * Provides serialization, deserialization, and I/O operations for {@code Boolean[]} arrays,
 * including proper handling of {@code null} array elements.
 *
 * <p>String representation: a bracket-enclosed, comma-separated list of boolean values,
 * e.g. {@code "[true, false, null, true]"}. The empty array is represented as {@code "[]"}.
 * Individual {@code null} elements are represented as the literal string {@code "null"}.</p>
 */
public final class BooleanArrayType extends ObjectArrayType<Boolean> {

    /**
     * Package-private constructor for {@code BooleanArrayType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BooleanArrayType() {
        super(Boolean[].class);
    }

    /**
     * Converts a {@code Boolean[]} array to its string representation.
     * The result is a bracket-enclosed, comma-separated list of element values.
     * {@code null} elements are rendered as the literal string {@code "null"}.
     *
     * @param x the {@code Boolean[]} to convert; may be {@code null}
     * @return {@code "[true, false, null]"} style string, {@code "[]"} for an empty array,
     *         or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final Boolean[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string in the format {@code "[true, false, null]"} and returns a {@code Boolean[]} array.
     * The exact 4-character string {@code "null"} (case-sensitive) is parsed as a {@code null} element.
     * Returns {@code null} for a {@code null}, empty, or blank input string.
     * Returns an empty array for the string {@code "[]"}.
     *
     * @param str the string to parse; may be {@code null}, empty, or blank
     * @return the parsed {@code Boolean[]} array, an empty array for {@code "[]"},
     *         or {@code null} if {@code str} is {@code null}, empty, or blank
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
     * Appends a {@code Boolean[]} array to an {@link Appendable} in bracket-enclosed format.
     * Appends the literal {@code "null"} string if {@code x} is {@code null}.
     * Each {@code null} element is written as {@code "null"};
     * non-null elements are written as {@code "true"} or {@code "false"}.
     *
     * @param appendable the target {@code Appendable}
     * @param x the {@code Boolean[]} array to append; may be {@code null}
     * @throws IOException if an I/O error occurs during appending
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean[] x) throws IOException {
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
                    appendable.append(x[i] ? TRUE_STRING : FALSE_STRING); //NOSONAR
                }
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes a {@code Boolean[]} array to a {@link CharacterWriter} in bracket-enclosed format.
     * Uses pre-allocated character arrays for {@code true}/{@code false}/{@code null} literals
     * for efficient output. The format is identical to {@link #appendTo(Appendable, Boolean[])}.
     * {@code config} is not used.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Boolean[]} array to write; may be {@code null}
     * @param config the serialization configuration (unused for boolean arrays); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Boolean[] x, final JsonXmlSerConfig<?> config) throws IOException {
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
                    writer.write(x[i] ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY); //NOSONAR
                }
            }

            writer.write(SK._BRACKET_R);
        }
    }
}
