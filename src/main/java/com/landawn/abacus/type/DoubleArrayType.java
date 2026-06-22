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
 * Type handler for boxed-double array ({@code Double[]}) values.
 * This class provides serialization, deserialization, and output operations for {@code Double[]} arrays.
 *
 * <p>The canonical string format is a bracket-enclosed, comma-separated list where null elements
 * are written as the literal {@code null} (e.g., {@code [1.5, null, 3.14, -0.5]}).
 *
 * @see ObjectArrayType
 */
public final class DoubleArrayType extends ObjectArrayType<Double> {

    /**
     * Package-private constructor for {@code DoubleArrayType}.
     * Instances are created by the {@code TypeFactory}.
     */
    DoubleArrayType() {
        super(Double[].class);
    }

    /**
     * Converts a {@code Double[]} to its canonical string representation.
     * The output is a bracket-enclosed, comma-separated list; null elements appear as {@code null}.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code [1.5, null, 3.14, -0.5]} for {@code new Double[]{1.5, null, 3.14, -0.5}}</li>
     *   <li>{@code []} for an empty array</li>
     *   <li>{@code null} if the input is {@code null}</li>
     * </ul>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code Double[]} to convert; may be {@code null}
     * @return the string representation, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Double[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string representation back into a {@code Double[]} array.
     * The expected format is a bracket-enclosed, comma-separated list as produced by {@link #stringOf}.
     * The literal {@code null} (4 characters) is converted to a {@code null} array element.
     *
     * <p>Special cases:
     * <ul>
     *   <li>{@code null}, blank, or empty string returns {@code null}</li>
     *   <li>{@code "[]"} returns an empty array</li>
     * </ul>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null}
     * @return the parsed {@code Double[]}, or {@code null} if {@code str} is {@code null} or blank
     * @throws NumberFormatException if any non-{@code null} value cannot be parsed as a {@code Double}
     * @see #valueOf(Object)
     * @see #stringOf(Double[])
     */
    @Override
    public Double[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_DOUBLE_OBJ_ARRAY;
        }

        final String[] elements = split(str);
        final int len = elements.length;
        final Double[] array = new Double[len];

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
     * Appends a {@code Double[]} to an {@link Appendable}.
     * The output format is a bracket-enclosed, comma-separated list.
     * Null elements are written as {@code null}; non-null values use {@link Double#toString()}.
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     * <p>
     * <b>appendTo vs. serializeTo:</b> both methods use the same bracket-enclosed scalar-element syntax for
     * {@code Double[]} values; {@code serializeTo} writes to a {@code CharacterWriter} for serializer pipelines.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the {@code Double[]} to append; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
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
    public void appendTo(final Appendable appendable, final Double[] x) throws IOException {
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
     * Writes a {@code Double[]} to a {@link CharacterWriter}.
     * The output format is a bracket-enclosed, comma-separated list.
     * Null elements are written as {@code null}; non-null values use the writer's optimized numeric
     * double-write method. If {@code x} is {@code null}, the literal {@code null} is written.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes numeric literals and {@code null}
     * elements directly to the {@code CharacterWriter}. The supplied serialization config is not used by this
     * implementation.
     * <p>
     * <b>serializeTo vs. appendTo:</b> both methods use the same bracket-enclosed scalar-element syntax for
     * {@code Double[]} values; {@code serializeTo} writes to a {@code CharacterWriter} for serializer pipelines.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the {@code Double[]} to write; may be {@code null}
     * @param config serialization configuration (not used for {@code Double} arrays); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Double[] x, final JsonXmlSerConfig<?> config) throws IOException {
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
