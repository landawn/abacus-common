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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Double array (Double[]) values.
 * This class provides serialization, deserialization, and output operations for Double arrays.
 * It handles proper formatting with brackets, separators, and null value representation.
 */
public final class DoubleArrayType extends ObjectArrayType<Double> {

    DoubleArrayType() {
        super(Double[].class);
    }

    /**
     * Converts a Double array to its string representation.
     * The output format is: [element1, element2, ...]
     * - Null elements are represented as "null"
     * - Empty arrays return "[]"
     * - Uses efficient string joining for performance
     *
     * @param x the Double array to convert. Can be null.
     * @return A string representation of the array, or null if input is null
     */
    @MayReturnNull
    @Override
    public String stringOf(final Double[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Converts a string representation back to a Double array.
     * Expects format: [element1, element2, ...]
     * - "null" strings (4 characters) are converted to null elements
     * - Empty string or "[]" returns empty array
     * - Individual elements are parsed as Double values
     *
     * @param str the string to parse. Can be null.
     * @return A Double array parsed from the string, or null if input is null
     */
    @MayReturnNull
    @Override
    public Double[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_DOUBLE_OBJ_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final Double[] a = new Double[len];

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
     * Appends a Double array to an Appendable output.
     * The output format is: [element1, element2, ...]
     * Null elements are represented as "null".
     * Uses toString() for Double values to ensure proper formatting.
     *
     * @param appendable the Appendable to write to
     * @param x the Double array to append. Can be null.
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Double[] x) throws IOException {
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
     * Writes a Double array to a CharacterWriter.
     * The output format is: [element1, element2, ...]
     * Null elements are represented as "null".
     * Uses optimized numeric writing for Double values.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Double array to write. Can be null.
     * @param config the serialization configuration (currently unused for Double arrays)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Double[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
