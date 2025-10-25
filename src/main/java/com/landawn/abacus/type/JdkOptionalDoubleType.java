/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalDouble;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for java.util.OptionalDouble.
 * This class provides serialization, deserialization, and database access capabilities for OptionalDouble instances.
 * OptionalDouble is a container that may or may not contain a double value.
 * Empty optionals are represented as null in serialized form.
 */
public class JdkOptionalDoubleType extends AbstractOptionalType<OptionalDouble> {

    public static final String OPTIONAL_DOUBLE = "JdkOptionalDouble";

    protected JdkOptionalDoubleType() {
        super(OPTIONAL_DOUBLE);
    }

    /**
     * Returns the Class object representing the OptionalDouble type.
     *
     * @return OptionalDouble.class
     */
    @Override
    public Class<OptionalDouble> clazz() {
        return OptionalDouble.class;
    }

    /**
     * Indicates whether instances of this type implement the Comparable interface.
     * OptionalDouble values can be compared when both are present.
     *
     * @return true, as OptionalDouble values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether this type should be written without quotes in CSV format.
     * Double values are numeric and should not be quoted.
     *
     * @return true, indicating that OptionalDouble values should not be quoted in CSV output
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
     * Converts an OptionalDouble to its string representation.
     * If the optional is empty or null, returns null.
     * Otherwise, returns the string representation of the contained double value.
     *
     * @param x the OptionalDouble to convert to string
     * @return the string representation of the double value, or null if empty or null
     */
    @Override
    public String stringOf(final OptionalDouble x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.getAsDouble());
    }

    /**
     * Parses a string representation into an OptionalDouble.
     * Empty or null strings result in an empty OptionalDouble.
     * Non-empty strings are parsed as double values and wrapped in OptionalDouble.
     *
     * @param str the string to parse
     * @return OptionalDouble.empty() if the string is null or empty, otherwise OptionalDouble containing the parsed value
     */
    @Override
    public OptionalDouble valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalDouble.empty() : OptionalDouble.of(Numbers.toDouble(str));
    }

    /**
     * Retrieves an OptionalDouble value from the specified column in a ResultSet.
     * If the column value is null, returns an empty OptionalDouble.
     * Otherwise, converts the value to double and wraps it in OptionalDouble.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return OptionalDouble.empty() if the column is null, otherwise OptionalDouble containing the value
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object obj = rs.getObject(columnIndex);

        return obj == null ? OptionalDouble.empty() : OptionalDouble.of(obj instanceof Double ? (Double) obj : Numbers.toDouble(obj));
    }

    /**
     * Retrieves an OptionalDouble value from the specified column in a ResultSet using the column label.
     * If the column value is null, returns an empty OptionalDouble.
     * Otherwise, converts the value to double and wraps it in OptionalDouble.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return OptionalDouble.empty() if the column is null, otherwise OptionalDouble containing the value
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public OptionalDouble get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object obj = rs.getObject(columnLabel);

        return obj == null ? OptionalDouble.empty() : OptionalDouble.of(obj instanceof Double ? (Double) obj : Numbers.toDouble(obj));
    }

    /**
     * Sets an OptionalDouble parameter in a PreparedStatement.
     * If the OptionalDouble is null or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the double value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the OptionalDouble to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalDouble x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(columnIndex, x.getAsDouble());
        }
    }

    /**
     * Sets an OptionalDouble parameter in a CallableStatement using a parameter name.
     * If the OptionalDouble is null or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the double value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalDouble to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalDouble x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.DOUBLE);
        } else {
            stmt.setDouble(parameterName, x.getAsDouble());
        }
    }

    /**
     * Appends the string representation of an OptionalDouble to an Appendable.
     * Empty optionals are written as "null".
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalDouble to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalDouble x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.getAsDouble()));
        }
    }

    /**
     * Writes the character representation of an OptionalDouble to a CharacterWriter.
     * Empty optionals are written as null.
     * Present values are written as numeric values without quotes.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalDouble to write
     * @param config the serialization configuration (not used for numeric values)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalDouble x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.getAsDouble());
        }
    }
}