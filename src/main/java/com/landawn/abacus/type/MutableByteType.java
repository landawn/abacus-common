/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class MutableByteType extends NumberType<MutableByte> {

    public static final String MUTABLE_BYTE = MutableByte.class.getSimpleName();

    protected MutableByteType() {
        super(MUTABLE_BYTE);
    }

    /**
     * Returns the Class object representing the MutableByte type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     * Class<MutableByte> clazz = type.clazz();
     * // Returns: MutableByte.class
     * }</pre>
     *
     * @return The Class object for MutableByte
     */
    @Override
    public Class<MutableByte> clazz() {
        return MutableByte.class;
    }

    /**
     * Converts a MutableByte object to its string representation.
     * The byte value is converted to a decimal string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     *
     * MutableByte mb = MutableByte.of((byte) 42);
     * String result = type.stringOf(mb);
     * // Returns: "42"
     *
     * mb = MutableByte.of((byte) -10);
     * result = type.stringOf(mb);
     * // Returns: "-10"
     *
     * result = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x The MutableByte object to convert
     * @return The string representation of the byte value, or null if the input is null
     */
    @Override
    public String stringOf(final MutableByte x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a MutableByte object.
     * The string is parsed as a byte value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     *
     * MutableByte result = type.valueOf("42");
     * // Returns: MutableByte with value 42
     *
     * result = type.valueOf("-10");
     * // Returns: MutableByte with value -10
     *
     * result = type.valueOf(null);
     * // Returns: null
     *
     * result = type.valueOf("");
     * // Returns: null
     * }</pre>
     *
     * @param str The string to parse
     * @return A MutableByte containing the parsed value, or null if the input is null or empty
     * @throws NumberFormatException if the string cannot be parsed as a byte
     */
    @Override
    public MutableByte valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableByte.of(Numbers.toByte(str));
    }

    /**
     * Retrieves a MutableByte value from a ResultSet at the specified column index.
     * The database byte value is wrapped in a MutableByte object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column contains byte value 42
     * MutableByte mb = type.get(rs, 1);
     * // Returns: MutableByte with value 42
     *
     * // Column contains byte value -10
     * mb = type.get(rs, 2);
     * // Returns: MutableByte with value -10
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A MutableByte containing the retrieved value
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public MutableByte get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableByte.of(rs.getByte(columnIndex));
    }

    /**
     * Retrieves a MutableByte value from a ResultSet using the specified column label.
     * The database byte value is wrapped in a MutableByte object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column "status_code" contains byte value 1
     * MutableByte mb = type.get(rs, "status_code");
     * // Returns: MutableByte with value 1
     *
     * // Column "priority" contains byte value 5
     * mb = type.get(rs, "priority");
     * // Returns: MutableByte with value 5
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A MutableByte containing the retrieved value
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableByte get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableByte.of(rs.getByte(columnLabel));
    }

    /**
     * Sets a MutableByte parameter in a PreparedStatement at the specified position.
     * If the MutableByte is null, 0 is stored. Otherwise, the wrapped byte value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     * PreparedStatement stmt = connection.prepareStatement(
     *     "INSERT INTO settings (id, status) VALUES (?, ?)");
     *
     * MutableByte mb = MutableByte.of((byte) 1);
     * type.set(stmt, 2, mb);
     * // Sets parameter to 1
     *
     * type.set(stmt, 2, null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The MutableByte value to set
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableByte x) throws SQLException {
        stmt.setByte(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a MutableByte parameter in a CallableStatement using the specified parameter name.
     * If the MutableByte is null, 0 is stored. Otherwise, the wrapped byte value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     * CallableStatement stmt = connection.prepareCall("{call update_status(?, ?)}");
     *
     * MutableByte mb = MutableByte.of((byte) 1);
     * type.set(stmt, "p_status", mb);
     * // Sets parameter to 1
     *
     * type.set(stmt, "p_priority", null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The MutableByte value to set
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableByte x) throws SQLException {
        stmt.setByte(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a MutableByte to an Appendable.
     * The value is written as a decimal string or "null".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     * StringBuilder sb = new StringBuilder();
     *
     * MutableByte mb = MutableByte.of((byte) 42);
     * type.appendTo(sb, mb);
     * // sb contains: "42"
     *
     * sb.setLength(0);
     * mb = MutableByte.of((byte) -10);
     * type.appendTo(sb, mb);
     * // sb contains: "-10"
     *
     * sb.setLength(0);
     * type.appendTo(sb, null);
     * // sb contains: "null"
     * }</pre>
     *
     * @param appendable The Appendable to write to
     * @param x The MutableByte to append
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableByte x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the character representation of a MutableByte to a CharacterWriter.
     * The value is written as numeric characters or the null character array.
     * This method is optimized for character-based writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableByteType type = new MutableByteType();
     * CharacterWriter writer = new CharacterWriter();
     * JSONXMLSerializationConfig config = JSONXMLSerializationConfig.of();
     *
     * MutableByte mb = MutableByte.of((byte) 42);
     * type.writeCharacter(writer, mb, config);
     * String result = writer.toString();
     * // result: "42"
     *
     * writer.reset();
     * type.writeCharacter(writer, null, config);
     * result = writer.toString();
     * // result: "null"
     * }</pre>
     *
     * @param writer The CharacterWriter to write to
     * @param x The MutableByte to write
     * @param config The serialization configuration (currently unused for byte values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableByte x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}