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
import com.landawn.abacus.util.u.OptionalInt;

/**
 * Type handler for {@link OptionalInt} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional integer values. This handler manages
 * the conversion between database integer values and OptionalInt wrapper objects.
 */
class OptionalIntType extends AbstractOptionalType<OptionalInt> {

    public static final String OPTIONAL_INT = OptionalInt.class.getSimpleName();

    protected OptionalIntType() {
        super(OPTIONAL_INT);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OptionalInt} class object
     */
    @Override
    public Class<OptionalInt> clazz() {
        return OptionalInt.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalInt values support comparison operations.
     *
     * @return {@code true}, as OptionalInt values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Numeric values typically don't require quotes in CSV.
     *
     * @return {@code true}, indicating integer values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Returns the default value for OptionalInt type, which is an empty OptionalInt.
     *
     * @return OptionalInt.empty()
     */
    @Override
    public OptionalInt defaultValue() {
        return OptionalInt.empty();
    }

    /**
     * Converts an {@link OptionalInt} object to its string representation.
     *
     * @param x the OptionalInt object to convert
     * @return the string representation of the integer value, or {@code null} if empty or null
     */
    @Override
    public String stringOf(final OptionalInt x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalInt} object.
     *
     * @param str the string to convert
     * @return an OptionalInt containing the parsed integer value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as an integer
     */
    @Override
    public OptionalInt valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalInt.empty() : OptionalInt.of(Numbers.toInt(str));
    }

    /**
     * Retrieves an integer value from a ResultSet at the specified column index and wraps it in an {@link OptionalInt}.
     * Handles type conversion if the database column is not an integer type.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalInt containing the integer value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalInt get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalInt.empty() : OptionalInt.of(obj instanceof Integer ? (Integer) obj : Numbers.toInt(obj));
    }

    /**
     * Retrieves an integer value from a ResultSet using the specified column label and wraps it in an {@link OptionalInt}.
     * Handles type conversion if the database column is not an integer type.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalInt containing the integer value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalInt get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalInt.empty() : OptionalInt.of(obj instanceof Integer ? (Integer) obj : Numbers.toInt(obj));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalInt}.
     * If the OptionalInt is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalInt value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalInt}.
     * If the OptionalInt is {@code null} or empty, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalInt value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalInt} to an Appendable.
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalInt value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalInt x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalInt} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalInt value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalInt x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.get());
        }
    }
}
