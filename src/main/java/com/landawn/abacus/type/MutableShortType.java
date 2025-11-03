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
import com.landawn.abacus.util.MutableShort;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link MutableShort} objects, providing serialization, deserialization,
 * and database interaction capabilities for mutable short wrapper objects.
 */
class MutableShortType extends NumberType<MutableShort> {

    public static final String MUTABLE_SHORT = MutableShort.class.getSimpleName();

    protected MutableShortType() {
        super(MUTABLE_SHORT);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link MutableShort} class object
     */
    @Override
    public Class<MutableShort> clazz() {
        return MutableShort.class;
    }

    /**
     * Converts a {@link MutableShort} object to its string representation.
     *
     * @param x the MutableShort object to convert
     * @return the string representation of the short value, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final MutableShort x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Converts a string representation to a {@link MutableShort} object.
     *
     * @param str the string to convert
     * @return a MutableShort containing the parsed short value, or {@code null} if the input string is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a short
     */
    @Override
    public MutableShort valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableShort.of(Numbers.toShort(str));
    }

    /**
     * Retrieves a {@link MutableShort} value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a MutableShort containing the short value from the ResultSet
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public MutableShort get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableShort.of(rs.getShort(columnIndex));
    }

    /**
     * Retrieves a {@link MutableShort} value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return a MutableShort containing the short value from the ResultSet
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public MutableShort get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableShort.of(rs.getShort(columnLabel));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value of a {@link MutableShort}.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the MutableShort value to set, or {@code null} (will be stored as 0)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableShort x) throws SQLException {
        stmt.setShort(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value of a {@link MutableShort}.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the MutableShort value to set, or {@code null} (will be stored as 0)
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableShort x) throws SQLException {
        stmt.setShort(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a {@link MutableShort} to an Appendable.
     *
     * @param appendable the Appendable to write to
     * @param x the MutableShort value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableShort x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the character representation of a {@link MutableShort} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the MutableShort value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableShort x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}
