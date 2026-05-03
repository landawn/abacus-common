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
 * Type handler for boxed-float array ({@code Float[]}) values.
 * This class provides serialization, deserialization, and output operations for {@code Float[]} arrays.
 *
 * <p>The canonical string format is a bracket-enclosed, comma-separated list where null elements
 * are written as the literal {@code null} (e.g., {@code [1.5, null, 3.14, -0.5]}).
 *
 * @see ObjectArrayType
 */
public final class FloatArrayType extends ObjectArrayType<Float> {

    /**
     * Package-private constructor for {@code FloatArrayType}.
     * Instances are created by the {@code TypeFactory}.
     */
    FloatArrayType() {
        super(Float[].class);
    }

    /**
     * Converts a {@code Float[]} to its canonical string representation.
     * The output is a bracket-enclosed, comma-separated list; null elements appear as {@code null}.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code [1.5, null, 3.14, -0.5]} for {@code new Float[]{1.5f, null, 3.14f, -0.5f}}</li>
     *   <li>{@code []} for an empty array</li>
     *   <li>{@code null} if the input is {@code null}</li>
     * </ul>
     *
     * @param x the {@code Float[]} to convert; may be {@code null}
     * @return the string representation, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final Float[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string representation back into a {@code Float[]} array.
     * The expected format is a bracket-enclosed, comma-separated list as produced by {@link #stringOf}.
     * The literal {@code null} (4 characters) is converted to a {@code null} array element.
     *
     * <p>Special cases:
     * <ul>
     *   <li>{@code null}, blank, or empty string returns {@code null}</li>
     *   <li>{@code "[]"} returns an empty array</li>
     * </ul>
     *
     * @param str the string to parse; may be {@code null}
     * @return the parsed {@code Float[]}, or {@code null} if {@code str} is {@code null} or blank
     */
    @Override
    public Float[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_FLOAT_OBJ_ARRAY;
        }

        final String[] elements = split(str);
        final int len = elements.length;
        final Float[] array = new Float[len];

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
     * Appends a {@code Float[]} to an {@link Appendable}.
     * The output format is a bracket-enclosed, comma-separated list.
     * Null elements are written as {@code null}; non-null values use {@link Float#toString()}.
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the {@code Float[]} to append; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Float[] x) throws IOException {
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
     * Writes a {@code Float[]} to a {@link CharacterWriter}.
     * The output format is a bracket-enclosed, comma-separated list.
     * Null elements are written as {@code null}; non-null values use the writer's optimized
     * float-write method. If {@code x} is {@code null}, the literal {@code null} is written.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the {@code Float[]} to write; may be {@code null}
     * @param config serialization configuration (not used for {@code Float} arrays); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Float[] x, final JsonXmlSerConfig<?> config) throws IOException {
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
                    writer.write(x[i]);
                }
            }

            writer.write(SK._BRACKET_R);
        }
    }
}
