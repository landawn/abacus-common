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
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for boxed-character array ({@code Character[]}) values.
 * This class provides serialization, deserialization, and output operations for {@code Character[]} arrays.
 *
 * <p>The canonical string format is a bracket-enclosed, comma-separated list where each non-null
 * character element is wrapped in single quotes (e.g., {@code ['a', 'b', null, 'z']}),
 * and null elements are written as the literal {@code null}.</p>
 *
 * @see ObjectArrayType
 */
public final class CharacterArrayType extends ObjectArrayType<Character> {

    /**
     * Package-private constructor for {@code CharacterArrayType}.
     * Instances are created by the {@code TypeFactory}.
     */
    CharacterArrayType() {
        super(Character[].class);
    }

    /**
     * Converts a {@code Character[]} to its canonical string representation.
     * The output format is a bracket-enclosed, comma-separated list.
     * Each non-null character is wrapped in single quotes; null elements are written as {@code null}.
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code ['a', 'b', null, 'z']} for {@code new Character[]{'a','b',null,'z'}}</li>
     *   <li>{@code []} for an empty array</li>
     *   <li>{@code null} if the input is {@code null}</li>
     * </ul>
     *
     * @param x the {@code Character[]} to convert; may be {@code null}
     * @return the string representation, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final Character[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 5));
        sb.append(SK._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            if (x[i] == null) {
                sb.append(NULL_CHAR_ARRAY);
            } else {
                sb.append(SK.SINGLE_QUOTE);
                sb.append(x[i]);
                sb.append(SK.SINGLE_QUOTE);
            }
        }

        sb.append(SK._BRACKET_R);

        final String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     * Parses a string representation back into a {@code Character[]} array.
     * The expected format is a bracket-enclosed, comma-separated list as produced by {@link #stringOf}.
     * Elements may be enclosed in single or double quotes; the literal {@code null} (4 characters)
     * is converted to a {@code null} array element.
     *
     * <p>Special cases:</p>
     * <ul>
     *   <li>{@code null}, blank, or empty string returns {@code null}</li>
     *   <li>{@code "[]"} returns an empty array</li>
     * </ul>
     *
     * @param str the string to parse; may be {@code null}
     * @return the parsed {@code Character[]}, or {@code null} if {@code str} is {@code null} or blank
     */
    @Override
    public Character[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_CHAR_OBJ_ARRAY;
        }

        final String[] elements = split(str);
        final int len = elements.length;
        final Character[] array = new Character[len];

        for (int i = 0; i < len; i++) {
            final String element = elements[i];

            if (element.length() == 4 && element.equals(NULL_STRING)) {
                array[i] = null;
                continue;
            }

            if (element.length() >= 2) {
                final char quoteChar = element.charAt(0);

                if ((quoteChar == SK._SINGLE_QUOTE || quoteChar == SK._DOUBLE_QUOTE) && element.charAt(element.length() - 1) == quoteChar) {
                    array[i] = elementType.valueOf(element.substring(1, element.length() - 1));
                    continue;
                }
            }

            array[i] = elementType.valueOf(element);
        }

        return array;
    }

    /**
     * Appends a {@code Character[]} to an {@link Appendable}.
     * The output format is a bracket-enclosed, comma-separated list.
     * Null elements are written as {@code null}; non-null characters are written unquoted.
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the {@code Character[]} to append; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Character[] x) throws IOException {
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
                    appendable.append(x[i]);
                }
            }

            appendable.append(SK._BRACKET_R);
        }
    }

    /**
     * Writes a {@code Character[]} to a {@link CharacterWriter} with optional per-element quotation.
     * The output format is a bracket-enclosed, comma-separated list.
     * <ul>
     *   <li>If {@code config} specifies a non-zero {@code charQuotation}, each non-null character
     *       is wrapped in that quote character, and a single quote ({@code '}) is escaped with a
     *       backslash when using single-quote quotation.</li>
     *   <li>Null elements are always written as {@code null}, without quotes.</li>
     *   <li>If {@code x} is {@code null}, the literal {@code null} is written.</li>
     * </ul>
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the {@code Character[]} to write; may be {@code null}
     * @param config serialization configuration controlling quotation; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Character[] x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACKET_L);

            final char charQuotation = (config == null) ? SK.CHAR_ZERO : config.getCharQuotation();

            if (charQuotation > 0) {
                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    if (x[i] == null) {
                        writer.write(NULL_CHAR_ARRAY);
                    } else {
                        writer.write(charQuotation);

                        if (x[i] == '\'' && charQuotation == '\'') {
                            writer.write('\\');
                        }

                        writer.writeCharacter(x[i]);
                        writer.write(charQuotation);
                    }
                }
            } else {
                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        writer.write(ELEMENT_SEPARATOR);
                    }

                    if (x[i] == null) {
                        writer.write(NULL_CHAR_ARRAY);
                    } else {
                        writer.writeCharacter(x[i]);
                    }
                }
            }

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Converts a {@code Character[]} to a human-readable string using the standard join utility.
     * Unlike {@link #stringOf}, elements are written without quote delimiters.
     * The format is a bracket-enclosed, comma-separated list (e.g., {@code [a, b, c]}).
     *
     * @param x the {@code Character[]} to convert; may be {@code null}
     * @return a string representation of the array, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String toString(final Character[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }
}
