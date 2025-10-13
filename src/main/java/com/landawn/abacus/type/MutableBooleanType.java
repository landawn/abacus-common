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
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class MutableBooleanType extends AbstractType<MutableBoolean> {

    public static final String MUTABLE_BOOLEAN = MutableBoolean.class.getSimpleName();

    protected MutableBooleanType() {
        super(MUTABLE_BOOLEAN);
    }

    /**
     * Returns the Class object representing the MutableBoolean type.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * Class<MutableBoolean> clazz = type.clazz();
     * // Returns: MutableBoolean.class
     * }</pre>
     *
     * @return The Class object for MutableBoolean
     */
    @Override
    public Class<MutableBoolean> clazz() {
        return MutableBoolean.class;
    }

    /**
     * Indicates whether values of this type are comparable.
     * MutableBoolean implements Comparable, so this returns true.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * boolean comparable = type.isComparable();
     * // Returns: true
     * }</pre>
     *
     * @return true, indicating that MutableBoolean values can be compared
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Converts a MutableBoolean object to its string representation.
     * The boolean value is converted to "true" or "false".
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     *
     * MutableBoolean mb = MutableBoolean.of(true);
     * String result = type.stringOf(mb);
     * // Returns: "true"
     *
     * mb = MutableBoolean.of(false);
     * result = type.stringOf(mb);
     * // Returns: "false"
     *
     * result = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x The MutableBoolean object to convert
     * @return The string representation ("true" or "false"), or null if the input is null
     */
    @Override
    public String stringOf(final MutableBoolean x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a MutableBoolean object.
     * The string is parsed as a boolean value using standard boolean parsing rules.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     *
     * MutableBoolean result = type.valueOf("true");
     * // Returns: MutableBoolean with value true
     *
     * result = type.valueOf("false");
     * // Returns: MutableBoolean with value false
     *
     * result = type.valueOf(null);
     * // Returns: null
     *
     * result = type.valueOf("");
     * // Returns: null
     * }</pre>
     *
     * @param str The string to parse
     * @return A MutableBoolean containing the parsed value, or null if the input is null or empty
     */
    @Override
    public MutableBoolean valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableBoolean.of(Strings.parseBoolean(str));
    }

    /**
     * Retrieves a MutableBoolean value from a ResultSet at the specified column index.
     * The database boolean value is wrapped in a MutableBoolean object.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column contains boolean value true
     * MutableBoolean mb = type.get(rs, 1);
     * // Returns: MutableBoolean with value true
     *
     * // Column contains boolean value false
     * mb = type.get(rs, 2);
     * // Returns: MutableBoolean with value false
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A MutableBoolean containing the retrieved value
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public MutableBoolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableBoolean.of(rs.getBoolean(columnIndex));
    }

    /**
     * Retrieves a MutableBoolean value from a ResultSet using the specified column label.
     * The database boolean value is wrapped in a MutableBoolean object.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column "is_active" contains boolean value true
     * MutableBoolean mb = type.get(rs, "is_active");
     * // Returns: MutableBoolean with value true
     *
     * // Column "is_deleted" contains boolean value false
     * mb = type.get(rs, "is_deleted");
     * // Returns: MutableBoolean with value false
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A MutableBoolean containing the retrieved value
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableBoolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableBoolean.of(rs.getBoolean(columnLabel));
    }

    /**
     * Sets a MutableBoolean parameter in a PreparedStatement at the specified position.
     * If the MutableBoolean is null, {@code false} is stored. Otherwise, the wrapped boolean value is stored.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * PreparedStatement stmt = connection.prepareStatement(
     *     "INSERT INTO users (id, is_active) VALUES (?, ?)");
     *
     * MutableBoolean mb = MutableBoolean.of(true);
     * type.set(stmt, 2, mb);
     * // Sets parameter to true
     *
     * type.set(stmt, 2, null);
     * // Sets parameter to false
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The MutableBoolean value to set
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableBoolean x) throws SQLException {
        stmt.setBoolean(columnIndex, x != null && x.value());
    }

    /**
     * Sets a MutableBoolean parameter in a CallableStatement using the specified parameter name.
     * If the MutableBoolean is null, {@code false} is stored. Otherwise, the wrapped boolean value is stored.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * CallableStatement stmt = connection.prepareCall("{call update_status(?, ?)}");
     *
     * MutableBoolean mb = MutableBoolean.of(true);
     * type.set(stmt, "p_is_active", mb);
     * // Sets parameter to true
     *
     * type.set(stmt, "p_is_deleted", null);
     * // Sets parameter to false
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The MutableBoolean value to set
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableBoolean x) throws SQLException {
        stmt.setBoolean(parameterName, x != null && x.value());
    }

    /**
     * Appends the string representation of a MutableBoolean to an Appendable.
     * The value is written as "true", "false", or "null".
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * StringBuilder sb = new StringBuilder();
     *
     * MutableBoolean mb = MutableBoolean.of(true);
     * type.appendTo(sb, mb);
     * // sb contains: "true"
     *
     * sb.setLength(0);
     * mb = MutableBoolean.of(false);
     * type.appendTo(sb, mb);
     * // sb contains: "false"
     *
     * sb.setLength(0);
     * type.appendTo(sb, null);
     * // sb contains: "null"
     * }</pre>
     *
     * @param appendable The Appendable to write to
     * @param x The MutableBoolean to append
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableBoolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x.value() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes the character representation of a MutableBoolean to a CharacterWriter.
     * The value is written as the character arrays for "true", "false", or "null".
     * This method is optimized for character-based writing.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * MutableBooleanType type = new MutableBooleanType();
     * CharacterWriter writer = new CharacterWriter();
     * JSONXMLSerializationConfig config = JSONXMLSerializationConfig.of();
     *
     * MutableBoolean mb = MutableBoolean.of(true);
     * type.writeCharacter(writer, mb, config);
     * String result = writer.toString();
     * // result: "true"
     *
     * writer.reset();
     * type.writeCharacter(writer, null, config);
     * result = writer.toString();
     * // result: "null"
     * }</pre>
     *
     * @param writer The CharacterWriter to write to
     * @param x The MutableBoolean to write
     * @param config The serialization configuration (currently unused for boolean values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableBoolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        writer.write((x == null) ? NULL_CHAR_ARRAY : (x.value() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}