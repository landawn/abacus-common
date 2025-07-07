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
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link MutableLong} objects, providing serialization, deserialization,
 * and database interaction capabilities for mutable long wrapper objects.
 */
public class MutableLongType extends NumberType<MutableLong> {

    public static final String MUTABLE_LONG = MutableLong.class.getSimpleName();

    protected MutableLongType() {
        super(MUTABLE_LONG);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link MutableLong} class object
     */
    @Override
    public Class<MutableLong> clazz() {
        return MutableLong.class;
    }

    /**
     * Converts a {@link MutableLong} object to its string representation.
     * 
     * @param x the MutableLong object to convert
     * @return the string representation of the long value, or null if the input is null
     */
    @Override
    public String stringOf(final MutableLong x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Converts a string representation to a {@link MutableLong} object.
     * 
     * @param str the string to convert
     * @return a MutableLong containing the parsed long value, or null if the input string is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a long
     */
    @Override
    public MutableLong valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableLong.of(Numbers.toLong(str));
    }

    /**
     * Retrieves a {@link MutableLong} value from a ResultSet at the specified column index.
     * 
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a MutableLong containing the long value from the ResultSet
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public MutableLong get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableLong.of(rs.getLong(columnIndex));
    }

    /**
     * Retrieves a {@link MutableLong} value from a ResultSet using the specified column label.
     * 
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return a MutableLong containing the long value from the ResultSet
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public MutableLong get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableLong.of(rs.getLong(columnLabel));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value of a {@link MutableLong}.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the MutableLong value to set, or null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableLong x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a named parameter in a CallableStatement to the value of a {@link MutableLong}.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the MutableLong value to set, or null
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableLong x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a {@link MutableLong} to an Appendable.
     * 
     * @param appendable the Appendable to write to
     * @param x the MutableLong value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableLong x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the character representation of a {@link MutableLong} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     * 
     * @param writer the CharacterWriter to write to
     * @param x the MutableLong value to write
     * @param config the serialization configuration (may be null)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableLong x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}