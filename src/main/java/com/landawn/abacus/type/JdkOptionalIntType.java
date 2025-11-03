/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalInt;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for java.util.OptionalInt.
 * This class provides serialization, deserialization, and database access capabilities for OptionalInt instances.
 * OptionalInt is a container that may or may not contain an int value.
 * Empty optionals are represented as {@code null} in serialized form.
 */
class JdkOptionalIntType extends AbstractOptionalType<OptionalInt> {

    public static final String OPTIONAL_INT = "JdkOptionalInt";

    protected JdkOptionalIntType() {
        super(OPTIONAL_INT);
    }

    /**
     * Returns the Class object representing the OptionalInt type.
     *
     * @return OptionalInt.class
     */
    @Override
    public Class<OptionalInt> clazz() {
        return OptionalInt.class;
    }

    /**
     * Indicates whether instances of this type implement the Comparable interface.
     * OptionalInt values can be compared when both are present.
     *
     * @return {@code true}, as OptionalInt values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether this type should be written without quotes in CSV format.
     * Integer values are numeric and should not be quoted.
     *
     * @return {@code true}, indicating that OptionalInt values should not be quoted in CSV output
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
     * Converts an OptionalInt to its string representation.
     * If the optional is empty or {@code null}, returns {@code null}.
     * Otherwise, returns the string representation of the contained int value.
     *
     * @param x the OptionalInt to convert to string
     * @return the string representation of the int value, or {@code null} if empty or null
     */
    @Override
    public String stringOf(final OptionalInt x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.getAsInt());
    }

    /**
     * Parses a string representation into an OptionalInt.
     * Empty or {@code null} strings result in an empty OptionalInt.
     * Non-empty strings are parsed as int values and wrapped in OptionalInt.
     *
     * @param str the string to parse
     * @return OptionalInt.empty() if the string is {@code null} or empty, otherwise OptionalInt containing the parsed value
     */
    @Override
    public OptionalInt valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalInt.empty() : OptionalInt.of(Numbers.toInt(str));
    }

    /**
     * Retrieves an OptionalInt value from the specified column in a ResultSet.
     * If the column value is {@code null}, returns an empty OptionalInt.
     * Otherwise, converts the value to int and wraps it in OptionalInt.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return OptionalInt.empty() if the column is {@code null}, otherwise OptionalInt containing the value
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalInt get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalInt.empty() : OptionalInt.of(obj instanceof Integer ? (Integer) obj : Numbers.toInt(obj));
    }

    /**
     * Retrieves an OptionalInt value from the specified column in a ResultSet using the column label.
     * If the column value is {@code null}, returns an empty OptionalInt.
     * Otherwise, converts the value to int and wraps it in OptionalInt.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return OptionalInt.empty() if the column is {@code null}, otherwise OptionalInt containing the value
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public OptionalInt get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalInt.empty() : OptionalInt.of(obj instanceof Integer ? (Integer) obj : Numbers.toInt(obj));
    }

    /**
     * Sets an OptionalInt parameter in a PreparedStatement.
     * If the OptionalInt is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the int value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the OptionalInt to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x.getAsInt());
        }
    }

    /**
     * Sets an OptionalInt parameter in a CallableStatement using a parameter name.
     * If the OptionalInt is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the int value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalInt to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalInt x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.getAsInt());
        }
    }

    /**
     * Appends the string representation of an OptionalInt to an Appendable.
     * Empty optionals are written as "null".
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalInt to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalInt x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.getAsInt()));
        }
    }

    /**
     * Writes the character representation of an OptionalInt to a CharacterWriter.
     * Empty optionals are written as {@code null}.
     * Present values are written as numeric values without quotes using the optimized writeInt method.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalInt to write
     * @param config the serialization configuration (not used for numeric values)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalInt x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.getAsInt());
        }
    }
}
