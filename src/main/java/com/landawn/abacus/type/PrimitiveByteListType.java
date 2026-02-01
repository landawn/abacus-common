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
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for ByteList objects.
 * Provides functionality for serialization, deserialization, and database operations
 * for ByteList instances by delegating to the underlying byte array type handler.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteListType extends AbstractPrimitiveListType<ByteList> {

    public static final String BYTE_LIST = ByteList.class.getSimpleName();

    private final Type<byte[]> arrayType = Type.of(byte[].class);

    private final Type<Byte> elementType = Type.of(byte.class);
    private final Type<Byte>[] parameterTypes = new Type[] { elementType };

    protected PrimitiveByteListType() {
        super(BYTE_LIST);
    }

    /**
     * Returns the Class object representing the ByteList type.
     *
     * @return the Class object for ByteList
     */
    @Override
    public Class<ByteList> clazz() {
        return ByteList.class;
    }

    /**
     * Returns the Type object for the byte element type.
     *
     * @return the Type object representing byte elements
     */
    @Override
    public Type<Byte> getElementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this array type.
     *
     * @return an array containing the Byte Type that describes the elements of this array type
     * @see #getElementType()
     */
    @Override
    public Type<Byte>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts a ByteList to its string representation.
     * The list is first converted to a byte array, then serialized using the array type handler.
     * Returns {@code null} if the input list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ByteList> type = TypeFactory.getType(ByteList.class);
     * ByteList list = ByteList.of(1, 2, 3, 127, -128);
     * String result = type.stringOf(list);
     * // result: Base64 encoded string of the byte values
     * }</pre>
     *
     * @param x the ByteList to convert
     * @return the string representation of the list, or {@code null} if input is null
     */
    @Override
    public String stringOf(final ByteList x) {
        return x == null ? null : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a ByteList.
     * The string is first parsed as a byte array, then wrapped in a ByteList.
     * Returns {@code null} if the input string is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ByteList> type = TypeFactory.getType(ByteList.class);
     * String encoded = "AQIDAP8=";  // Base64 encoded bytes
     * ByteList list = type.valueOf(encoded);
     * // list: ByteList containing the decoded bytes
     * }</pre>
     *
     * @param str the string to parse
     * @return a ByteList created from the parsed values, or {@code null} if input is {@code null} or empty
     */
    @Override
    public ByteList valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null;
        }

        final byte[] array = arrayType.valueOf(str);
        return array == null ? null : ByteList.of(array);
    }

    /**
     * Retrieves a ByteList from a ResultSet at the specified column index.
     * The bytes are read from the database and wrapped in a ByteList.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return a ByteList containing the bytes from the database, or {@code null} if the column value is null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public ByteList get(final ResultSet rs, final int columnIndex) throws SQLException {
        final byte[] bytes = rs.getBytes(columnIndex);
        return bytes == null ? null : ByteList.of(bytes);
    }

    /**
     * Retrieves a ByteList from a ResultSet using the specified column label.
     * The bytes are read from the database and wrapped in a ByteList.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label/name
     * @return a ByteList containing the bytes from the database, or {@code null} if the column value is null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public ByteList get(final ResultSet rs, final String columnLabel) throws SQLException {
        final byte[] bytes = rs.getBytes(columnLabel);
        return bytes == null ? null : ByteList.of(bytes);
    }

    /**
     * Sets a ByteList value in a PreparedStatement at the specified parameter index.
     * The ByteList is converted to a byte array before being set in the statement.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the ByteList to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final ByteList x) throws SQLException {
        stmt.setBytes(columnIndex, x == null ? null : x.toArray());
    }

    /**
     * Sets a ByteList value in a CallableStatement using the specified parameter name.
     * The ByteList is converted to a byte array before being set in the statement.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the ByteList to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final ByteList x) throws SQLException {
        stmt.setBytes(parameterName, x == null ? null : x.toArray());
    }

    /**
     * Sets a ByteList value in a PreparedStatement with SQL type information.
     * The ByteList is converted to a byte array before being set in the statement.
     * The sqlTypeOrLength parameter is ignored as byte arrays have their own specific SQL type.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the ByteList to set, or null
     * @param sqlTypeOrLength the SQL type or length (ignored for byte arrays)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final ByteList x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(columnIndex, x == null ? null : x.toArray());
    }

    /**
     * Sets a ByteList value in a CallableStatement with SQL type information.
     * The ByteList is converted to a byte array before being set in the statement.
     * The sqlTypeOrLength parameter is ignored as byte arrays have their own specific SQL type.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the ByteList to set, or null
     * @param sqlTypeOrLength the SQL type or length (ignored for byte arrays)
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final ByteList x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(parameterName, x == null ? null : x.toArray());
    }

    /**
     * Appends the string representation of a ByteList to an Appendable.
     * Delegates to the array type handler after converting the list to an array.
     * Appends "null" if the list is {@code null}.
     *
     * @param appendable the Appendable to write to
     * @param x the ByteList to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final ByteList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the character representation of a ByteList to a CharacterWriter.
     * Delegates to the array type handler after converting the list to an array.
     * Writes "null" if the list is {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the ByteList to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final ByteList x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.toArray(), config);
        }
    }
}