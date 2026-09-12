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
 * Type handler for primitive {@code byte[]} values.
 * Provides serialization, deserialization, and JDBC operations for raw byte arrays.
 *
 * <p>String representation: Base64-encoded string produced by
 * {@link com.landawn.abacus.util.Strings#base64Encode(byte[])}. A {@code null} array serializes
 * to {@code null} (unlike {@link Base64EncodedType}, which serializes {@code null} as an empty string).</p>
 *
 * <p>JDBC mapping: stored and retrieved using {@link java.sql.PreparedStatement#setBytes} /
 * {@link java.sql.ResultSet#getBytes}, which maps to SQL {@code BINARY} / {@code VARBINARY} /
 * {@code LONGVARBINARY} column types.</p>
 *
 * <p>This handler is registered under the name {@code "Bytes"} (obtain it via {@code Type.of("Bytes")});
 * {@code Type.of(byte[].class)} resolves to the list-formatted {@link PrimitiveByteArrayType} instead.</p>
 *
 * @see Base64EncodedType
 * @see PrimitiveByteArrayType
 */
public class BytesType extends AbstractType<byte[]> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "Bytes"}).
     */
    public static final String BYTES = "Bytes";

    /**
     * Package-private constructor for {@code BytesType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BytesType() {
        super(BYTES);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code byte[].class}
     */
    @Override
    public Class<byte[]> javaType() {
        return byte[].class;
    }

    /**
     * Encodes a {@code byte[]} as a Base64 string.
     * Uses {@link com.landawn.abacus.util.Strings#base64Encode(byte[])} internally.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the byte array to encode; may be {@code null}
     * @return the Base64-encoded string representation of the array,
     *         or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final byte[] x) {
        return (x == null) ? null : Strings.base64Encode(x);
    }

    /**
     * Decodes a Base64-encoded string back to a {@code byte[]}.
     * Uses {@link com.landawn.abacus.util.Strings#base64Decode(String)} internally.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the Base64-encoded string to decode; may be {@code null}
     * @return the decoded byte array, or {@code null} if {@code str} is {@code null}
     * @throws IllegalArgumentException if {@code str} is not valid Base64 (a character outside the
     *         Base64 alphabet, or malformed padding such as {@code "AQID="})
     * @see #valueOf(Object)
     * @see #stringOf(byte[])
     */
    @Override
    public byte[] valueOf(final String str) throws IllegalArgumentException {
        return (str == null) ? null : Strings.base64Decode(str);
    }

    /**
     * Retrieves a {@code byte[]} from a {@link java.sql.ResultSet} at the specified column index.
     * Delegates to {@link java.sql.ResultSet#getBytes(int)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the byte array
     * @return the {@code byte[]} value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public byte[] get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getBytes(columnIndex);
    }

    /**
     * Retrieves a {@code byte[]} from a {@link java.sql.ResultSet} using the specified column label.
     * Delegates to {@link java.sql.ResultSet#getBytes(String)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the {@code byte[]} value in the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public byte[] get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getBytes(columnName);
    }

    /**
     * Sets a {@code byte[]} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * Delegates to {@link java.sql.PreparedStatement#setBytes(int, byte[])}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code byte[]} value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final byte[] x) throws NullPointerException, SQLException {
        stmt.setBytes(columnIndex, x);
    }

    /**
     * Sets a named {@code byte[]} parameter on a {@link java.sql.CallableStatement}.
     * Delegates to {@link java.sql.CallableStatement#setBytes(String, byte[])}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code byte[]} value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final byte[] x) throws NullPointerException, SQLException {
        stmt.setBytes(parameterName, x);
    }
}
