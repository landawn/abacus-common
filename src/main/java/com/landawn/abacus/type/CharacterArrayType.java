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
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Character array (Character[]) values.
 * This class provides serialization, deserialization, and output operations for Character arrays.
 * It handles proper formatting with brackets, separators, and {@code null} value representation.
 */
public final class CharacterArrayType extends ObjectArrayType<Character> {

    CharacterArrayType() {
        super(Character[].class);
    }

    /**
     * Converts a Character array to its string representation.
     * The output format is: [element1, element2, ...]
     * - Null elements are represented as "null"
     * - Non-null characters are wrapped in single quotes
     * - Empty arrays return "[]"
     *
     * @param x the Character array to convert. Can be {@code null}.
     * @return A string representation of the array with quoted characters, or {@code null} if input is null
     */
    @Override
    public String stringOf(final Character[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 5));
        sb.append(WD._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            if (x[i] == null) {
                sb.append(NULL_CHAR_ARRAY);
            } else {
                sb.append(WD.QUOTATION_S);
                sb.append(x[i]);
                sb.append(WD.QUOTATION_S);
            }
        }

        sb.append(WD._BRACKET_R);

        final String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     * Converts a string representation back to a Character array.
     * Expects format: [element1, element2, ...]
     * - Handles both quoted and unquoted characters
     * - "null" strings (4 characters) are converted to {@code null} elements
     * - Empty string or "[]" returns empty array
     *
     * @param str the string to parse. Can be {@code null}.
     * @return A Character array parsed from the string, or {@code null} if input is null
     */
    @Override
    public Character[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_CHAR_OBJ_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final Character[] a = new Character[len];

        if (len > 0) {
            boolean isQuoted = (strs[0].length() > 1) && ((strs[0].charAt(0) == WD._QUOTATION_S) || (strs[0].charAt(0) == WD._QUOTATION_D));

            if (isQuoted) {
                for (int i = 0; i < len; i++) {
                    if (strs[i].length() == 4 && strs[i].equals(NULL_STRING)) {
                        a[i] = null;
                    } else {
                        a[i] = elementType.valueOf(strs[i].substring(1, strs[i].length() - 1));
                    }
                }
            } else {
                for (int i = 0; i < len; i++) {
                    if (strs[i].length() == 4 && strs[i].equals(NULL_STRING)) {
                        a[i] = null;
                    } else {
                        a[i] = elementType.valueOf(strs[i]);
                    }
                }
            }
        }

        return a;
    }

    /**
     * Appends a Character array to an Appendable output.
     * The output format is: [element1, element2, ...]
     * Null elements are represented as "null" (without quotes).
     *
     * @param appendable the Appendable to write to
     * @param x the Character array to append. Can be {@code null}.
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Character[] x) throws IOException {
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
                    appendable.append(x[i]);
                }
            }

            appendable.append(WD._BRACKET_R);
        }
    }

    /**
     * Writes a Character array to a CharacterWriter with optional quotation based on configuration.
     * The output format is: [element1, element2, ...]
     * - If quotation is configured, characters are wrapped in the specified quote character
     * - Single quotes within characters are escaped when using single quote quotation
     * - Null elements are always represented as "null" without quotes
     *
     * @param writer the CharacterWriter to write to
     * @param x the Character array to write. Can be {@code null}.
     * @param config the serialization configuration that specifies quotation settings. Can be {@code null}.
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Character[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            final char charQuotation = (config == null) ? WD.CHAR_ZERO : config.getCharQuotation();

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

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Converts a Character array to a string representation using standard array formatting.
     * This is an alternative string representation that uses join utility for formatting.
     * The output format is: [element1, element2, ...]
     *
     * @param x the Character array to convert. Can be {@code null}.
     * @return A string representation of the array, or {@code null} if input is null
     */
    @Override
    public String toString(final Character[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }
}
