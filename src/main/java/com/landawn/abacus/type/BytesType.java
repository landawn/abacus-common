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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.util.Strings;

/**
 * Type handler for byte array (byte[]) values.
 * This class provides serialization, deserialization, and database operations for byte arrays.
 * Byte arrays are encoded/decoded using Base64 encoding for string representation.
 */
public class BytesType extends AbstractType<byte[]> {

    public static final String BYTES = "Bytes";

    BytesType() {
        super(BYTES);
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing byte[].class
     */
    @Override
    public Class<byte[]> clazz() {
        return byte[].class;
    }

    /**
     * Converts a byte array to its string representation using Base64 encoding.
     * This method is used for serialization purposes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<byte[]> type = TypeFactory.getType(byte[].class);
     * byte[] data = "Hello World".getBytes();
     * String encoded = type.stringOf(data);
     * // encoded: "SGVsbG8gV29ybGQ=" (Base64 of "Hello World")
     *
     * byte[] binary = new byte[]{0x48, 0x65, 0x6C, 0x6C, 0x6F};
     * String result = type.stringOf(binary);
     * // result: Base64 encoded string
     * }</pre>
     *
     * @param x the byte array to convert. Can be {@code null}.
     * @return A Base64 encoded string representation of the byte array, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final byte[] x) {
        return (x == null) ? null : Strings.base64Encode(x);
    }

    /**
     * Converts a Base64 encoded string back to a byte array.
     * This method is used for deserialization purposes.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<byte[]> type = TypeFactory.getType(byte[].class);
     * String encoded = "SGVsbG8gV29ybGQ=";  // Base64 for "Hello World"
     * byte[] data = type.valueOf(encoded);
     * // data: byte array containing "Hello World"
     *
     * String result = new String(data);
     * // result: "Hello World"
     * }</pre>
     *
     * @param str the Base64 encoded string to convert. Can be {@code null}.
     * @return The decoded byte array, or {@code null} if the input string is null
     */
    @Override
    public byte[] valueOf(final String str) {
        return (str == null) ? null : Strings.base64Decode(str);
    }

    /**
     * Retrieves a byte array value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the byte array value
     * @return The byte array value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public byte[] get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }

    /**
     * Retrieves a byte array value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet containing the data
     * @param columnName the label of the column containing the byte array value
     * @return The byte array value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public byte[] get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getBytes(columnName);
    }

    /**
     * Sets a byte array value as a parameter in a PreparedStatement.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the byte array value to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final byte[] x) throws SQLException {
        stmt.setBytes(columnIndex, x);
    }

    /**
     * Sets a byte array value as a named parameter in a CallableStatement.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the byte array value to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final byte[] x) throws SQLException {
        stmt.setBytes(parameterName, x);
    }
}