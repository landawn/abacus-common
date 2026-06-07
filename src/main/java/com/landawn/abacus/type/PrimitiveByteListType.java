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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link ByteList} objects, which is a primitive-backed list of {@code byte} values.
 * Provides serialization, deserialization, and database operations for {@link ByteList} instances
 * by delegating to the underlying {@code byte[]} array type handler.
 * String representations use the format {@code [1, 2, 3]} with comma-separated numeric values
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
     * This constructor is protected to allow subclassing while maintaining controlled instantiation
     * through the TypeFactory.
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
     * Converts a ByteList to its string representation.
     * The list is first converted to a byte array, then serialized using the array type handler.
     * Returns {@code null} if the input list is {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ByteList> type = TypeFactory.getType(ByteList.class);
     * ByteList list = ByteList.of(1, 2, 3, 127, -128);
     * String result = type.stringOf(list);
     * // result: "[1, 2, 3, 127, -128]"
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the ByteList to convert
     * @return the string representation of the list, or {@code null} if input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
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
     * ByteList list = type.valueOf("[1, 2, 3, 127, -128]");
     * // list: ByteList containing the byte values 1, 2, 3, 127, -128
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse
     * @return a ByteList created from the parsed values, or {@code null} if input is {@code null} or empty
     * @see #valueOf(Object)
     * @see #stringOf(ByteList)
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
     * @param columnName the column label/name
     * @return a ByteList containing the bytes from the database, or {@code null} if the column value is null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public ByteList get(final ResultSet rs, final String columnName) throws SQLException {
        final byte[] bytes = rs.getBytes(columnName);
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
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the ByteList to append
     * @throws IOException if an I/O error occurs
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
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the ByteList to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final ByteList x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.serializeTo(writer, x.toArray(), config);
        }
    }
}
