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
import com.landawn.abacus.util.MutableDouble;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class MutableDoubleType extends NumberType<MutableDouble> {

    public static final String MUTABLE_DOUBLE = MutableDouble.class.getSimpleName();

    protected MutableDoubleType() {
        super(MUTABLE_DOUBLE);
    }

    /**
     * Returns the Class object representing the MutableDouble type.
     *
     * @return The Class object for MutableDouble
     */
    @Override
    public Class<MutableDouble> clazz() {
        return MutableDouble.class;
    }

    /**
     * Converts a MutableDouble object to its string representation.
     * The double value is converted to a decimal string representation.
     *
     * @param x The MutableDouble object to convert
     * @return The string representation of the double value, or null if the input is null
     */
    @Override
    public String stringOf(final MutableDouble x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Parses a string to create a MutableDouble object.
     * The string is parsed as a double value.
     *
     * @param str The string to parse
     * @return A MutableDouble containing the parsed value, or null if the input is null or empty
     * @throws NumberFormatException if the string cannot be parsed as a double
     */
    @Override
    public MutableDouble valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableDouble.of(Numbers.toDouble(str));
    }

    /**
     * Retrieves a MutableDouble value from a ResultSet at the specified column index.
     * The database double value is wrapped in a MutableDouble object.
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A MutableDouble containing the retrieved value
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public MutableDouble get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableDouble.of(rs.getDouble(columnIndex));
    }

    /**
     * Retrieves a MutableDouble value from a ResultSet using the specified column label.
     * The database double value is wrapped in a MutableDouble object.
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A MutableDouble containing the retrieved value
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableDouble get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableDouble.of(rs.getDouble(columnLabel));
    }

    /**
     * Sets a MutableDouble parameter in a PreparedStatement at the specified position.
     * If the MutableDouble is null, 0.0 is stored. Otherwise, the wrapped double value is stored.
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The MutableDouble value to set
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableDouble x) throws SQLException {
        stmt.setDouble(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a MutableDouble parameter in a CallableStatement using the specified parameter name.
     * If the MutableDouble is null, 0.0 is stored. Otherwise, the wrapped double value is stored.
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The MutableDouble value to set
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableDouble x) throws SQLException {
        stmt.setDouble(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a MutableDouble to an Appendable.
     * The value is written as a decimal string or "null".
     *
     * @param appendable The Appendable to write to
     * @param x The MutableDouble to append
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableDouble x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the character representation of a MutableDouble to a CharacterWriter.
     * The value is written as numeric characters or the null character array.
     * This method is optimized for character-based writing.
     *
     * @param writer The CharacterWriter to write to
     * @param x The MutableDouble to write
     * @param config The serialization configuration (currently unused for double values)
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableDouble x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}