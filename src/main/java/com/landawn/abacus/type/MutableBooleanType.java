/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link com.landawn.abacus.util.MutableBoolean} objects.
 * <p>
 * This class provides serialization, deserialization, and database operations for
 * MutableBoolean instances, which are mutable wrappers around primitive boolean values.
 *
 * @see com.landawn.abacus.util.MutableBoolean
 * @see AbstractType
 */
public class MutableBooleanType extends AbstractType<MutableBoolean> {

    public static final String MUTABLE_BOOLEAN = MutableBoolean.class.getSimpleName();

    protected MutableBooleanType() {
        super(MUTABLE_BOOLEAN);
    }

    /**
     * Returns the Class object representing the MutableBoolean type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
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
     * MutableBoolean implements Comparable, so this returns {@code true}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
     * boolean comparable = type.isComparable();
     * // Returns: true
     * }</pre>
     *
     * @return {@code true}, indicating that MutableBoolean values can be compared
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Converts a MutableBoolean object to its string representation.
     * The boolean value is converted to "true" or "false".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
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
     * @return The string representation ("true" or "false"), or {@code null} if the input is null
     */
    @Override
    public String stringOf(final MutableBoolean x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a MutableBoolean object.
     * The string is parsed as a boolean value using standard boolean parsing rules.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
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
     * @return A MutableBoolean containing the parsed value, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public MutableBoolean valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableBoolean.of(Strings.parseBoolean(str));
    }

    /**
     * Retrieves a MutableBoolean value from a ResultSet at the specified column index.
     * The database boolean value is wrapped in a MutableBoolean object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
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
     * @param columnName The label of the column to retrieve the value from
     * @return A MutableBoolean containing the retrieved value
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableBoolean get(final ResultSet rs, final String columnName) throws SQLException {
        return MutableBoolean.of(rs.getBoolean(columnName));
    }

    /**
     * Sets a MutableBoolean parameter in a PreparedStatement at the specified position.
     * If the MutableBoolean is {@code null}, {@code false} is stored. Otherwise, the wrapped boolean value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
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
     * If the MutableBoolean is {@code null}, {@code false} is stored. Otherwise, the wrapped boolean value is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
     * CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<MutableBoolean> type = TypeFactory.getType(MutableBoolean.class);
     * CharacterWriter writer = new CharacterWriter();
     *
     * MutableBoolean mb = MutableBoolean.of(true);
     * type.writeCharacter(writer, mb, null);
     * String result = writer.toString();
     * // result: "true"
     *
     * writer.reset();
     * type.writeCharacter(writer, null, null);
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
    public void writeCharacter(final CharacterWriter writer, final MutableBoolean x, final JsonXmlSerializationConfig<?> config) throws IOException {
        writer.write((x == null) ? NULL_CHAR_ARRAY : (x.value() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}