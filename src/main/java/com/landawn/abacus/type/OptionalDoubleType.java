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
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.OptionalDouble;

/**
 * Type handler for {@link OptionalDouble} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional double-precision floating-point values.
 * This handler manages the conversion between database numeric values and OptionalDouble wrapper objects.
 */
class OptionalDoubleType extends AbstractOptionalType<OptionalDouble> {

    public static final String OPTIONAL_DOUBLE = OptionalDouble.class.getSimpleName();

    protected OptionalDoubleType() {
        super(OPTIONAL_DOUBLE);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OptionalDouble} class object
     */
    @Override
    public Class<OptionalDouble> clazz() {
        return OptionalDouble.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalDouble values support comparison operations.
     *
     * @return {@code true}, as OptionalDouble values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Numeric values typically don't require quotes in CSV.
     *
     * @return {@code true}, indicating double values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Returns the default value for OptionalDouble type, which is an empty OptionalDouble.
     *
     * @return OptionalDouble.empty()
     */
    @Override
    public OptionalDouble defaultValue() {
        return OptionalDouble.empty();
    }

    /**
     * Converts an {@link OptionalDouble} object to its string representation.
     *
     * @param x the OptionalDouble object to convert
     * @return the string representation of the double value, or {@code null} if empty or null
     */
    @Override
    public String stringOf(final OptionalDouble x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalDouble} object.
     *
     * @param str the string to convert
     * @return an OptionalDouble containing the parsed double value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a double
     */
    @Override
    public OptionalDouble valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalDouble.empty() : OptionalDouble.of(Numbers.toDouble(str));
    }

    /**
     * Retrieves a double value from a ResultSet at the specified column index and wraps it in an {@link OptionalDouble}.
     * Handles type conversion if the database column is not a double type.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalDouble containing the double value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalDouble.empty() : OptionalDouble.of(obj instanceof Double ? (Double) obj : Numbers.toDouble(obj));
    }

    /**
     * Retrieves a double value from a ResultSet using the specified column label and wraps it in an {@link OptionalDouble}.
     * Handles type conversion if the database column is not a double type.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalDouble containing the double value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalDouble.empty() : OptionalDouble.of(obj instanceof Double ? (Double) obj : Numbers.toDouble(obj));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalDouble}.
     * If the OptionalDouble is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalDouble value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalDouble x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalDouble}.
     * If the OptionalDouble is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalDouble value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalDouble x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalDouble} to an Appendable.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalDouble value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalDouble x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalDouble} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalDouble value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalDouble x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}
