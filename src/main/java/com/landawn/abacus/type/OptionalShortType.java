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
import com.landawn.abacus.util.u.OptionalShort;

/**
 * Type handler for {@link OptionalShort} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional short integer values. This handler manages
 * the conversion between database short values and OptionalShort wrapper objects.
 */
public class OptionalShortType extends AbstractOptionalType<OptionalShort> {

    public static final String OPTIONAL_SHORT = OptionalShort.class.getSimpleName();

    protected OptionalShortType() {
        super(OPTIONAL_SHORT);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OptionalShort} class object
     */
    @Override
    public Class<OptionalShort> clazz() {
        return OptionalShort.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalShort values support comparison operations.
     *
     * @return true, as OptionalShort values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Numeric values typically don't require quotes in CSV.
     *
     * @return true, indicating short values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts an {@link OptionalShort} object to its string representation.
     * 
     * @param x the OptionalShort object to convert
     * @return the string representation of the short value, or null if empty or null
     */
    @Override
    public String stringOf(final OptionalShort x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalShort} object.
     * 
     * @param str the string to convert
     * @return an OptionalShort containing the parsed short value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a short
     */
    @Override
    public OptionalShort valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalShort.empty() : OptionalShort.of(Numbers.toShort(str));
    }

    /**
     * Retrieves a short value from a ResultSet at the specified column index and wraps it in an {@link OptionalShort}.
     * Handles type conversion if the database column is not a short type.
     * 
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalShort containing the short value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalShort get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalShort.empty() : OptionalShort.of(obj instanceof Short ? (Short) obj : Numbers.toShort(obj));
    }

    /**
     * Retrieves a short value from a ResultSet using the specified column label and wraps it in an {@link OptionalShort}.
     * Handles type conversion if the database column is not a short type.
     * 
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalShort containing the short value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalShort get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalShort.empty() : OptionalShort.of(obj instanceof Short ? (Short) obj : Numbers.toShort(obj));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalShort}.
     * If the OptionalShort is null or empty, sets the parameter to SQL NULL.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalShort value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalShort x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.SMALLINT);
        } else {
            stmt.setShort(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalShort}.
     * If the OptionalShort is null or empty, sets the parameter to SQL NULL.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalShort value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalShort x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.SMALLINT);
        } else {
            stmt.setShort(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalShort} to an Appendable.
     * 
     * @param appendable the Appendable to write to
     * @param x the OptionalShort value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalShort x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalShort} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     * 
     * @param writer the CharacterWriter to write to
     * @param x the OptionalShort value to write
     * @param config the serialization configuration (may be null)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalShort x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}