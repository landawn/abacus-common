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
 * Type handler for Integer array (Integer[]).
 * This class provides optimized serialization and deserialization for arrays of Integer objects,
 * including proper handling of null elements within the array.
 */
public final class IntegerArrayType extends ObjectArrayType<Integer> {

    IntegerArrayType() {
        super(Integer[].class);
    }

    /**
     * Converts an Integer array to its string representation.
     * The array is serialized as a JSON array string with proper null handling.
     * Null elements are represented as "null" in the output string.
     * This method is optimized for performance using string joining.
     *
     * @param x the Integer array to convert to string
     * @return the JSON array string representation (e.g., "[1,null,3]"), or null if the input array is null
     */
    @MayReturnNull
    @Override
    public String stringOf(final Integer[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Parses a string representation into an Integer array.
     * The string should be in JSON array format. Handles the following cases:
     * - null string: returns null
     * - empty string or "[]": returns empty Integer array
     * - JSON array: parses each element, treating "null" strings as null values
     *
     * @param str the JSON array string to parse (e.g., "[1,null,3]")
     * @return the parsed Integer array, or null if the input is null
     */
    @MayReturnNull
    @Override
    public Integer[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_INT_OBJ_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final Integer[] a = new Integer[len];

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
     * Appends the string representation of an Integer array to an Appendable.
     * The output format is a JSON array with proper null handling.
     * This method is optimized to avoid string concatenation overhead.
     *
     * @param appendable the Appendable to write to
     * @param x the Integer array to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Integer[] x) throws IOException {
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
     * Writes the character representation of an Integer array to a CharacterWriter.
     * This method is optimized for performance by using specialized writeInt method
     * for non-null values, avoiding string conversion overhead.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Integer array to write
     * @param config the serialization configuration (not used for integer arrays)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Integer[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
                    writer.writeInt(x[i]);
                }
            }

            writer.write(WD._BRACKET_R);
        }
    }
}