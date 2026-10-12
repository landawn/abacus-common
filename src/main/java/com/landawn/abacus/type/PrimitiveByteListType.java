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
import java.util.List;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link ByteList} objects, which is a primitive-backed list of {@code byte} values.
 * Uses the matching primitive-array format for serialization and deserialization, while honoring
 * the {@code byte[]} type handler resolved when this instance is constructed.
 * Supports JDBC reads and writes using byte arrays.
 * With the built-in handler, string representations use the format {@code [1, 2, 3]} with comma-separated numeric values
 * enclosed in square brackets.
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteListType extends AbstractPrimitiveListType<ByteList> {

    /** The type name constant for {@link ByteList} type identification, equal to {@code "ByteList"}. */
    public static final String BYTE_LIST = ByteList.class.getSimpleName();

    private final Type<byte[]> arrayType = Type.of(byte[].class);
    private final Type<Byte> elementType = Type.of(byte.class);
    private final List<Type<?>> parameterTypes = List.of(elementType);

    /**
     * Constructs a new PrimitiveByteListType instance.
     * This constructor is protected to keep instantiation controlled by the TypeFactory.
     */
    protected PrimitiveByteListType() {
        super(BYTE_LIST);
    }

    /**
     * Returns the Class object representing the ByteList type.
     *
     * @return the Class object for ByteList
     */
    @Override
    public Class<ByteList> javaType() {
        return ByteList.class;
    }

    /**
     * Returns the cached type handler for {@code byte[]} values.
     *
     * <p>The handler is resolved through {@link Type#of(Class)} when this list type is constructed.
     * It is used for parsing and for serialization that delegates to an array handler, including a custom
     * handler registered before construction. Repeated calls return the same handler.</p>
     *
     * @return the cached {@code byte[]} type handler; never {@code null}
     * @see #valueOf(String)
     */
    @Override
    public Type<byte[]> arrayType() {
        return arrayType;
    }

    /**
     * Returns the Type object for the byte element type.
     *
     * @return the Type object representing byte elements
     */
    @Override
    public Type<Byte> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types associated with this list type.
     *
     * @return an immutable list containing the Byte Type that describes the elements of this list type
     * @see #elementType()
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Converts the logical elements of a {@link ByteList} to their array-type string representation.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets,
     * for example {@code [1, 2, 3]}. An empty list produces {@code []}; a {@code null} list returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ByteList> type = TypeFactory.getType(ByteList.class);
     * ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 127, (byte) -128);
     * String result = type.stringOf(list);
     * // result: "[1, 2, 3, 127, -128]"
     * }</pre>
     *
     * <p>The representation is intended to be parsed by {@link #valueOf(String)}.</p>
     *
     * @param x the list to convert; may be {@code null}
     * @return the array-type string representation, or {@code null} if {@code x} is {@code null}
     * @implNote With the built-in array handler, this method formats the backing array directly up to {@code x.size()},
     * without copying or trimming it. A custom cached array handler instead receives a snapshot containing only the
     * logical elements. Unused backing-array capacity is never included.
     * @see #arrayType()
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @SuppressWarnings("deprecation")
    @Override
    public String stringOf(final ByteList x) {
        if (x == null) {
            return null;
        }

        return arrayType instanceof PrimitiveByteArrayType ? stringOf(x.internalArray(), x.size()) : arrayType.stringOf(x.toArray());
    }

    /**
     * Parses a string representation and creates a ByteList.
     * The string is first parsed as a byte array, then wrapped in a ByteList.
     * Returns {@code null} if the input string is {@code null}, empty or blank.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ByteList> type = TypeFactory.getType(ByteList.class);
     * ByteList list = type.valueOf("[1, 2, 3, 127, -128]");
     * // list: ByteList containing the byte values 1, 2, 3, 127, -128
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return a ByteList created from the parsed values, or {@code null} if input is {@code null}, empty or blank
     * @throws IllegalArgumentException if an unquoted element is empty or whitespace-only.
     * @throws NumberFormatException if an element is not a valid integer literal
     * @throws ArithmeticException if an element is out of the {@code byte} range (e.g. {@code 128})
     * @see #valueOf(Object)
     * @see #stringOf(ByteList)
     */
    @MayReturnNull
    @Override
    public ByteList valueOf(final String str) throws IllegalArgumentException, NumberFormatException, ArithmeticException {
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
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public ByteList get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final byte[] bytes = rs.getBytes(columnIndex);
        return bytes == null ? null : ByteList.of(bytes);
    }

    /**
     * Retrieves a ByteList from a ResultSet using the specified column label.
     * The bytes are read from the database and wrapped in a ByteList.
     *
     * @param rs the ResultSet to read from
     * @param columnName the column label/name
     * @return a ByteList containing the bytes from the database, or {@code null} if the column value is null
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public ByteList get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final byte[] bytes = rs.getBytes(columnName);
        return bytes == null ? null : ByteList.of(bytes);
    }

    /**
     * Sets a ByteList value in a PreparedStatement at the specified parameter index.
     * The ByteList is converted to a byte array before being set in the statement.
     *
     * @param statement the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the ByteList to set, or null
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final ByteList x) throws NullPointerException, SQLException {
        statement.setBytes(columnIndex, x == null ? null : x.toArray());
    }

    /**
     * Sets a ByteList value in a CallableStatement using the specified parameter name.
     * The ByteList is converted to a byte array before being set in the statement.
     *
     * @param statement the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the ByteList to set, or null
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final ByteList x) throws NullPointerException, SQLException {
        statement.setBytes(parameterName, x == null ? null : x.toArray());
    }

    /**
     * Sets a ByteList value in a PreparedStatement with SQL type information.
     * The ByteList is converted to a byte array before being set in the statement.
     * The sqlTypeOrLength parameter is ignored as byte arrays have their own specific SQL type.
     *
     * @param statement the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the ByteList to set, or null
     * @param sqlTypeOrLength the SQL type or length (ignored for byte arrays)
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final ByteList x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        statement.setBytes(columnIndex, x == null ? null : x.toArray());
    }

    /**
     * Sets a ByteList value in a CallableStatement with SQL type information.
     * The ByteList is converted to a byte array before being set in the statement.
     * The sqlTypeOrLength parameter is ignored as byte arrays have their own specific SQL type.
     *
     * @param statement the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the ByteList to set, or null
     * @param sqlTypeOrLength the SQL type or length (ignored for byte arrays)
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final ByteList x, final int sqlTypeOrLength)
            throws NullPointerException, SQLException {
        statement.setBytes(parameterName, x == null ? null : x.toArray());
    }

    /**
     * Appends the logical elements of a {@link ByteList} to the destination using the array-type text format.
     * With the built-in handler, elements are separated by a comma and a space and enclosed in square brackets.
     * An empty list produces {@code []}; a {@code null} list produces the literal {@code "null"}.
     * The built-in format matches {@link #stringOf(ByteList)} for non-null lists.
     *
     * <p>This method does not flush or close the destination.</p>
     *
     * @param appendable the destination to append to; must not be {@code null}
     * @param x the list to append; may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}
     * @throws IOException if writing to the destination fails
     * @implNote With the built-in array handler and a destination known not to invoke application callbacks,
     * this method reads the backing array directly up to {@code x.size()}. Otherwise, it delegates to the cached
     * array handler with a snapshot of the logical elements, taken before invoking destination or handler callbacks.
     * Neither path trims the list or includes unused backing-array capacity.
     * @see #arrayType()
     * @see #serializeTo(CharacterWriter, ByteList, JsonXmlSerConfig)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void appendTo(final Appendable appendable, final ByteList x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else if (arrayType instanceof PrimitiveByteArrayType && canWriteDirectly(appendable)) {
            appendTo(appendable, x.internalArray(), x.size());
        } else {
            arrayType.appendTo(appendable, x.toArray());
        }
    }

    /**
     * Writes the logical elements of a {@link ByteList} using primitive-array serialization.
     * With the built-in handler, elements are written as unquoted literals separated by a comma and a space
     * and enclosed in square brackets. The configuration does not change their literal representation.
     * The built-in handler writes {@code []} for an empty list; a {@code null} list produces the literal {@code "null"}.
     *
     * <p>This method does not flush or close the writer.</p>
     *
     * @param writer the destination writer; must not be {@code null}
     * @param x the list to serialize; may be {@code null}
     * @param config the serialization configuration; may be {@code null} and is passed unchanged when delegating to the array handler
     * @throws NullPointerException if {@code writer} is {@code null}
     * @throws IOException if writing to the destination fails
     * @implNote With the built-in array handler, a writer known not to invoke application callbacks, and either
     * a {@code null} configuration or an unextended built-in JSON/XML configuration, this method reads the backing
     * array directly up to {@code x.size()}. Otherwise, it delegates to the cached array handler with a snapshot of
     * the logical elements, taken before invoking writer, configuration, or handler callbacks. Neither path trims
     * the list or includes unused backing-array capacity.
     * @see #arrayType()
     * @see #appendTo(Appendable, ByteList)
     */
    @SuppressWarnings("deprecation")
    @Override
    public void serializeTo(final CharacterWriter writer, final ByteList x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (arrayType instanceof PrimitiveByteArrayType && canWriteDirectly(writer, config)) {
            serializeTo(writer, x.internalArray(), x.size());
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
