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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@code Byte[]} (boxed byte array) values.
 * Provides serialization, deserialization, and JDBC operations for {@code Byte[]} arrays.
 *
 * <p>String representation: a bracket-enclosed, comma-separated list of byte values,
 * e.g. {@code "[1, 2, null, 127]"}. The empty array is represented as {@code "[]"}.
 * Individual {@code null} elements are represented as the literal string {@code "null"}.</p>
 *
 * <p>JDBC mapping: the {@code Byte[]} is unboxed to a primitive {@code byte[]} before being
 * stored via {@link java.sql.PreparedStatement#setBytes} and retrieved via
 * {@link java.sql.ResultSet#getBytes}, then re-boxed to {@code Byte[]}.</p>
 */
public final class ByteArrayType extends ObjectArrayType<Byte> {

    /**
     * Package-private constructor for {@code ByteArrayType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    ByteArrayType() {
        super(Byte[].class);
    }

    /**
     * Converts a {@code Byte[]} array to its string representation.
     * The result is a bracket-enclosed, comma-separated list of element values.
     * {@code null} elements are rendered as the literal string {@code "null"}.
     *
     * @param x the {@code Byte[]} to convert; may be {@code null}
     * @return {@code "[1, 2, null]"} style string, {@code "[]"} for an empty array,
     *         or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final Byte[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, SK.BRACKET_L, SK.BRACKET_R);
    }

    /**
     * Parses a string in the format {@code "[1, 2, null]"} and returns a {@code Byte[]} array.
     * The exact 4-character string {@code "null"} (case-sensitive) is parsed as a {@code null} element.
     * Returns {@code null} for a {@code null}, empty, or blank input string.
     * Returns an empty array for the string {@code "[]"}.
     *
     * @param str the string to parse; may be {@code null}, empty, or blank
     * @return the parsed {@code Byte[]} array, an empty array for {@code "[]"},
     *         or {@code null} if {@code str} is {@code null}, empty, or blank
     * @throws NumberFormatException if any non-null element cannot be parsed as a valid {@code byte}
     */
    @Override
    public Byte[] valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_BYTE_OBJ_ARRAY;
        }

        final String[] elements = split(str);
        final int len = elements.length;
        final Byte[] array = new Byte[len];

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
     * Retrieves a {@code Byte[]} from a {@link java.sql.ResultSet} at the specified column index.
     * The column value is read as a primitive {@code byte[]} and boxed to {@code Byte[]}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the byte-array column
     * @return a boxed {@code Byte[]} array, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public Byte[] get(final ResultSet rs, final int columnIndex) throws SQLException {
        return Array.box(rs.getBytes(columnIndex));
    }

    /**
     * Retrieves a {@code Byte[]} from a {@link java.sql.ResultSet} using the specified column label.
     * The column value is read as a primitive {@code byte[]} and boxed to {@code Byte[]}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return a boxed {@code Byte[]} array, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Byte[] get(final ResultSet rs, final String columnName) throws SQLException {
        return Array.box(rs.getBytes(columnName));
    }

    /**
     * Sets a {@code Byte[]} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * The boxed {@code Byte[]} is unboxed to a primitive {@code byte[]} before being stored.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code Byte[]} value to set; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Byte[] x) throws SQLException {
        stmt.setBytes(columnIndex, Array.unbox(x));
    }

    /**
     * Sets a named {@code Byte[]} parameter on a {@link java.sql.CallableStatement}.
     * The boxed {@code Byte[]} is unboxed to a primitive {@code byte[]} before being stored.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code Byte[]} value to set; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Byte[] x) throws SQLException {
        stmt.setBytes(parameterName, Array.unbox(x));
    }

    /**
     * Sets a {@code Byte[]} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * The boxed {@code Byte[]} is unboxed to a primitive {@code byte[]} before being stored.
     * The {@code sqlTypeOrLength} parameter is not used.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code Byte[]} value to set; may be {@code null}
     * @param sqlTypeOrLength ignored for byte arrays
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Byte[] x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(columnIndex, Array.unbox(x));
    }

    /**
     * Sets a named {@code Byte[]} parameter on a {@link java.sql.CallableStatement}.
     * The boxed {@code Byte[]} is unboxed to a primitive {@code byte[]} before being stored.
     * The {@code sqlTypeOrLength} parameter is not used.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code Byte[]} value to set; may be {@code null}
     * @param sqlTypeOrLength ignored for byte arrays
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Byte[] x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(parameterName, Array.unbox(x));
    }

    /**
     * Appends a {@code Byte[]} array to an {@link Appendable} in bracket-enclosed format.
     * Appends the literal {@code "null"} string if {@code x} is {@code null}.
     * Each {@code null} element is written as {@code "null"};
     * non-null elements are written as their decimal string values.
     *
     * @param appendable the target {@code Appendable}
     * @param x the {@code Byte[]} array to append; may be {@code null}
     * @throws IOException if an I/O error occurs during appending
     */
    @Override
    public void appendTo(final Appendable appendable, final Byte[] x) throws IOException {
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
     * Writes a {@code Byte[]} array to a {@link CharacterWriter} in bracket-enclosed format.
     * Uses the writer's optimised byte-write method for non-null elements.
     * The format is identical to {@link #appendTo(Appendable, Byte[])}.
     * {@code config} is not used.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Byte[]} array to write; may be {@code null}
     * @param config the serialization configuration (unused for byte arrays); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Byte[] x, final JsonXmlSerConfig<?> config) throws IOException {
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
