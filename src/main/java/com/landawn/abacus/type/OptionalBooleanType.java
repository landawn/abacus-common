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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalBoolean;

/**
 * Type handler for {@link OptionalBoolean} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional boolean values. This handler manages
 * the conversion between database boolean values and OptionalBoolean wrapper objects.
 */
public class OptionalBooleanType extends AbstractOptionalType<OptionalBoolean> {

    public static final String OPTIONAL_BOOLEAN = OptionalBoolean.class.getSimpleName();

    protected OptionalBooleanType() {
        super(OPTIONAL_BOOLEAN);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OptionalBoolean} class object
     */
    @Override
    public Class<OptionalBoolean> clazz() {
        return OptionalBoolean.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalBoolean values support comparison operations.
     *
     * @return true, as OptionalBoolean values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Boolean values typically don't require quotes in CSV.
     *
     * @return true, indicating boolean values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Returns the default value for OptionalBoolean type, which is an empty OptionalBoolean.
     *
     * @return OptionalBoolean.empty()
     */
    @Override
    public OptionalBoolean defaultValue() {
        return OptionalBoolean.empty();
    }

    /**
     * Converts an {@link OptionalBoolean} object to its string representation.
     * 
     * @param x the OptionalBoolean object to convert
     * @return "true" or "false" if the Optional contains a value, or null if empty or null
     */
    @Override
    public String stringOf(final OptionalBoolean x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalBoolean} object.
     * 
     * @param str the string to convert ("true", "false", or parseable boolean strings)
     * @return an OptionalBoolean containing the parsed boolean value, or empty if the input is empty or null
     */
    @Override
    public OptionalBoolean valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalBoolean.empty() : OptionalBoolean.of(parseBoolean(str));
    }

    /**
     * Retrieves a boolean value from a ResultSet at the specified column index and wraps it in an {@link OptionalBoolean}.
     * Handles type conversion if the database column is not a boolean type.
     * 
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalBoolean containing the boolean value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalBoolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalBoolean.empty() : OptionalBoolean.of(obj instanceof Boolean ? (Boolean) obj : N.convert(obj, Boolean.class));
    }

    /**
     * Retrieves a boolean value from a ResultSet using the specified column label and wraps it in an {@link OptionalBoolean}.
     * Handles type conversion if the database column is not a boolean type.
     * 
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalBoolean containing the boolean value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalBoolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalBoolean.empty() : OptionalBoolean.of(obj instanceof Boolean ? (Boolean) obj : N.convert(obj, Boolean.class));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalBoolean}.
     * If the OptionalBoolean is null or empty, sets the parameter to SQL NULL.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalBoolean value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalBoolean x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalBoolean}.
     * If the OptionalBoolean is null or empty, sets the parameter to SQL NULL.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalBoolean value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalBoolean x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalBoolean} to an Appendable.
     * Writes "true", "false", or "null" depending on the value.
     * 
     * @param appendable the Appendable to write to
     * @param x the OptionalBoolean value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalBoolean x) throws IOException {
        appendable.append((x == null || x.isEmpty()) ? NULL_STRING : (x.get() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes the character representation of an {@link OptionalBoolean} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     * 
     * @param writer the CharacterWriter to write to
     * @param x the OptionalBoolean value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalBoolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        writer.write((x == null || x.isEmpty()) ? NULL_CHAR_ARRAY : (x.get() ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}