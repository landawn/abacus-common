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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Byte array operations.
 * This class provides serialization/deserialization and database operations
 * for Byte[] arrays, including conversion between primitive byte[] and Byte[].
 */
public final class ByteArrayType extends ObjectArrayType<Byte> {

    ByteArrayType() {
        super(Byte[].class);
    }

    /**
     * Converts a Byte array to its string representation.
     * The array is formatted with square brackets and comma-separated elements.
     * Null elements are represented as "null" in the output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Byte[]> type = TypeFactory.getType(Byte[].class);
     * Byte[] array = {1, 2, null, 127};
     * String result = type.stringOf(array);
     * // result: "[1, 2, null, 127]"
     *
     * String empty = type.stringOf(new Byte[0]);
     * // empty: "[]"
     * }</pre>
     *
     * @param x the Byte array to convert
     * @return a string representation like "[1, 2, null]", or {@code null} if input is {@code null},
     *         or "[]" if the array is empty
     */
    @Override
    public String stringOf(final Byte[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return Strings.join(x, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     * Converts a string representation back to a Byte array.
     * Parses a string in the format "[1, 2, null]" into a Byte array.
     * The string "null" (case-sensitive) is parsed as a {@code null} element.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Byte[]> type = TypeFactory.getType(Byte[].class);
     * Byte[] result = type.valueOf("[1, 2, null, 127]");
     * // result: {1, 2, null, 127}
     *
     * Byte[] empty = type.valueOf("[]");
     * // empty: {} (empty array)
     * }</pre>
     *
     * @param str the string to parse, expecting format like "[1, 2, null]"
     * @return a Byte array parsed from the string, or {@code null} if str is {@code null},
     *         or an empty array if str is empty or equals "[]"
     * @throws NumberFormatException if any {@code non-null} element cannot be parsed as a byte
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
     * Retrieves a Byte array from a ResultSet at the specified column index.
     * The primitive byte[] from the database is converted to a Byte[] object array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Byte[]> type = TypeFactory.getType(Byte[].class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * Byte[] data = type.get(rs, 1);
     * // Converts database byte[] to Byte[]
     * }</pre>
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the byte array
     * @return a Byte array boxed from the database byte[], or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Byte[] get(final ResultSet rs, final int columnIndex) throws SQLException {
        return Array.box(rs.getBytes(columnIndex));
    }

    /**
     * Retrieves a Byte array from a ResultSet using the specified column label.
     * The primitive byte[] from the database is converted to a Byte[] object array.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnName the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return a Byte array boxed from the database byte[], or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public Byte[] get(final ResultSet rs, final String columnName) throws SQLException {
        return Array.box(rs.getBytes(columnName));
    }

    /**
     * Sets a Byte array parameter in a PreparedStatement at the specified position.
     * The Byte[] object array is converted to a primitive byte[] before setting.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Byte array to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Byte[] x) throws SQLException {
        stmt.setBytes(columnIndex, Array.unbox(x));
    }

    /**
     * Sets a named Byte array parameter in a CallableStatement.
     * The Byte[] object array is converted to a primitive byte[] before setting.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Byte array to set, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Byte[] x) throws SQLException {
        stmt.setBytes(parameterName, Array.unbox(x));
    }

    /**
     * Sets a Byte array parameter in a PreparedStatement with additional SQL type information.
     * The Byte[] object array is converted to a primitive byte[] before setting.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Byte array to set, may be null
     * @param sqlTypeOrLength the SQL type code (ignored for byte arrays)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Byte[] x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(columnIndex, Array.unbox(x));
    }

    /**
     * Sets a named Byte array parameter in a CallableStatement with additional SQL type information.
     * The Byte[] object array is converted to a primitive byte[] before setting.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Byte array to set, may be null
     * @param sqlTypeOrLength the SQL type code (ignored for byte arrays)
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Byte[] x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(parameterName, Array.unbox(x));
    }

    /**
     * Appends a Byte array to an Appendable object.
     * Formats the array with square brackets and comma-separated elements.
     * Null array elements are appended as "null".
     *
     * @param appendable the Appendable object to append to
     * @param x the Byte array to append, may be null
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Byte[] x) throws IOException {
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
     * Writes a Byte array to a CharacterWriter.
     * Uses the writer's optimized write method for byte values.
     * The output format matches the string representation with square brackets.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Byte array to write, may be null
     * @param config the serialization configuration (not used for byte arrays)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Byte[] x, final JsonXmlSerializationConfig<?> config) throws IOException {
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