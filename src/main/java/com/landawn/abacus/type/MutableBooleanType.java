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
     * If the MutableBoolean is null, false is stored. Otherwise, the wrapped boolean value is stored.
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
     * If the MutableBoolean is null, false is stored. Otherwise, the wrapped boolean value is stored.
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