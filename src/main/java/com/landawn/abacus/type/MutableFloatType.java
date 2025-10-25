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
import com.landawn.abacus.util.MutableFloat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link MutableFloat} objects. This class provides serialization,
 * deserialization, and database operations for MutableFloat instances, which are
 * mutable wrappers around primitive float values.
 */
public class MutableFloatType extends NumberType<MutableFloat> {

    /**
     * The type name identifier for MutableFloat type.
     */
    public static final String MUTABLE_FLOAT = MutableFloat.class.getSimpleName();

    protected MutableFloatType() {
        super(MUTABLE_FLOAT);
    }

    /**
     * Returns the Class object representing the MutableFloat type.
     *
     * @return the Class object for MutableFloat
     */
    @Override
    public Class<MutableFloat> clazz() {
        return MutableFloat.class;
    }

    /**
     * Converts a MutableFloat object to its string representation.
     * Returns the string representation of the wrapped float value.
     *
     * @param x the MutableFloat object to convert
     * @return the string representation of the float value, or null if x is null
     */
    @Override
    public String stringOf(final MutableFloat x) {
        return x == null ? null : N.stringOf(x.value());
    }

    /**
     * Creates a MutableFloat object from its string representation.
     * Parses the string as a float value and wraps it in a MutableFloat.
     *
     * @param str the string to parse
     * @return a MutableFloat containing the parsed value, or null if str is empty
     */
    @Override
    public MutableFloat valueOf(final String str) {
        return Strings.isEmpty(str) ? null : MutableFloat.of(Numbers.toFloat(str));
    }

    /**
     * Retrieves a MutableFloat value from the specified column in the ResultSet.
     * Reads a float value from the database and wraps it in a MutableFloat object.
     *
     * @param rs the ResultSet containing the query results
     * @param columnIndex the index of the column to retrieve (1-based)
     * @return a MutableFloat containing the retrieved float value
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public MutableFloat get(final ResultSet rs, final int columnIndex) throws SQLException {
        return MutableFloat.of(rs.getFloat(columnIndex));
    }

    /**
     * Retrieves a MutableFloat value from the specified column in the ResultSet.
     * Reads a float value from the database and wraps it in a MutableFloat object.
     *
     * @param rs the ResultSet containing the query results
     * @param columnLabel the label of the column to retrieve
     * @return a MutableFloat containing the retrieved float value
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public MutableFloat get(final ResultSet rs, final String columnLabel) throws SQLException {
        return MutableFloat.of(rs.getFloat(columnLabel));
    }

    /**
     * Sets a MutableFloat value at the specified parameter index in the PreparedStatement.
     * Extracts the float value from the MutableFloat and sets it in the statement.
     * If the MutableFloat is null, sets 0.0f as the value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the MutableFloat value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableFloat x) throws SQLException {
        stmt.setFloat(columnIndex, (x == null) ? 0 : x.value());
    }

    /**
     * Sets a MutableFloat value for the specified parameter name in the CallableStatement.
     * Extracts the float value from the MutableFloat and sets it in the statement.
     * If the MutableFloat is null, sets 0.0f as the value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the MutableFloat value to set, may be null
     * @throws SQLException if a database access error occurs or the parameterName is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableFloat x) throws SQLException {
        stmt.setFloat(parameterName, (x == null) ? 0 : x.value());
    }

    /**
     * Appends the string representation of a MutableFloat to the given Appendable.
     * Writes "null" if the MutableFloat is null, otherwise writes the string
     * representation of the float value.
     *
     * @param appendable the Appendable to write to
     * @param x the MutableFloat value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final MutableFloat x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.value()));
        }
    }

    /**
     * Writes the character representation of a MutableFloat to the given CharacterWriter.
     * This method is used for JSON/XML serialization. Writes null characters if the
     * MutableFloat is null, otherwise writes the float value directly.
     *
     * @param writer the CharacterWriter to write to
     * @param x the MutableFloat value to write
     * @param config the serialization configuration (may be used for formatting)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final MutableFloat x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.value());
        }
    }
}