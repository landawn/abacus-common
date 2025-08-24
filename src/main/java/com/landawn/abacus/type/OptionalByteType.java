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
import com.landawn.abacus.util.u.OptionalByte;

/**
 * Type handler for {@link OptionalByte} objects, providing serialization, deserialization,
 * and database interaction capabilities for optional byte values. This handler manages
 * the conversion between database byte/numeric values and OptionalByte wrapper objects.
 */
public class OptionalByteType extends AbstractOptionalType<OptionalByte> {

    public static final String OPTIONAL_BYTE = OptionalByte.class.getSimpleName();

    protected OptionalByteType() {
        super(OPTIONAL_BYTE);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OptionalByte} class object
     */
    @Override
    public Class<OptionalByte> clazz() {
        return OptionalByte.class;
    }

    /**
     * Indicates whether values of this type can be compared.
     * OptionalByte values support comparison operations.
     *
     * @return true, as OptionalByte values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether values of this type should be quoted when written to CSV format.
     * Numeric values typically don't require quotes in CSV.
     *
     * @return true, indicating byte values don't need quotes in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts an {@link OptionalByte} object to its string representation.
     * 
     * @param x the OptionalByte object to convert
     * @return the string representation of the byte value, or null if empty or null
     */
    @Override
    public String stringOf(final OptionalByte x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.get());
    }

    /**
     * Converts a string representation to an {@link OptionalByte} object.
     * 
     * @param str the string to convert
     * @return an OptionalByte containing the parsed byte value, or empty if the input is empty or null
     * @throws NumberFormatException if the string cannot be parsed as a byte
     */
    @Override
    public OptionalByte valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalByte.empty() : OptionalByte.of(Numbers.toByte(str));
    }

    /**
     * Retrieves a byte value from a ResultSet at the specified column index and wraps it in an {@link OptionalByte}.
     * Handles type conversion if the database column is not a byte type.
     * 
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OptionalByte containing the byte value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalByte get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalByte.empty() : OptionalByte.of(obj instanceof Byte ? (Byte) obj : Numbers.toByte(obj));
    }

    /**
     * Retrieves a byte value from a ResultSet using the specified column label and wraps it in an {@link OptionalByte}.
     * Handles type conversion if the database column is not a byte type.
     * 
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return an OptionalByte containing the byte value, or empty if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public OptionalByte get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalByte.empty() : OptionalByte.of(obj instanceof Byte ? (Byte) obj : Numbers.toByte(obj));
    }

    /**
     * Sets a parameter in a PreparedStatement to the value contained in an {@link OptionalByte}.
     * If the OptionalByte is null or empty, sets the parameter to SQL NULL.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OptionalByte value to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalByte x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.TINYINT);
        } else {
            stmt.setByte(columnIndex, x.get());
        }
    }

    /**
     * Sets a named parameter in a CallableStatement to the value contained in an {@link OptionalByte}.
     * If the OptionalByte is null or empty, sets the parameter to SQL NULL.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalByte value to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalByte x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.TINYINT);
        } else {
            stmt.setByte(parameterName, x.get());
        }
    }

    /**
     * Appends the string representation of an {@link OptionalByte} to an Appendable.
     * 
     * @param appendable the Appendable to write to
     * @param x the OptionalByte value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalByte x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.get()));
        }
    }

    /**
     * Writes the character representation of an {@link OptionalByte} to a CharacterWriter.
     * This method is typically used for JSON/XML serialization.
     * 
     * @param writer the CharacterWriter to write to
     * @param x the OptionalByte value to write
     * @param config the serialization configuration
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalByte x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.get());
        }
    }
}