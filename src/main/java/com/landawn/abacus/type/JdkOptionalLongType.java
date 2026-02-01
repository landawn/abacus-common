/*
 * Copyright (c) 2016, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for java.util.OptionalLong.
 * This class provides serialization, deserialization, and database access capabilities for OptionalLong instances.
 * OptionalLong is a container that may or may not contain a long value.
 * Empty optionals are represented as {@code null} in serialized form.
 */
public class JdkOptionalLongType extends AbstractOptionalType<OptionalLong> {

    public static final String OPTIONAL_LONG = "JdkOptionalLong";

    protected JdkOptionalLongType() {
        super(OPTIONAL_LONG);
    }

    /**
     * Returns the Class object representing the OptionalLong type.
     *
     * @return OptionalLong.class
     */
    @Override
    public Class<OptionalLong> clazz() {
        return OptionalLong.class;
    }

    /**
     * Indicates whether instances of this type implement the Comparable interface.
     * OptionalLong values can be compared when both are present.
     *
     * @return {@code true}, as OptionalLong values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Indicates whether this type should be written without quotes in CSV format.
     * Long values are numeric and should not be quoted.
     *
     * @return {@code true}, indicating that OptionalLong values should not be quoted in CSV output
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Returns the default value for OptionalLong type, which is an empty OptionalLong.
     *
     * @return OptionalLong.empty()
     */
    @Override
    public OptionalLong defaultValue() {
        return OptionalLong.empty();
    }

    /**
     * Converts an OptionalLong to its string representation.
     * If the optional is empty or {@code null}, returns {@code null}.
     * Otherwise, returns the string representation of the contained long value.
     *
     * @param x the OptionalLong to convert to string
     * @return the string representation of the long value, or {@code null} if empty or null
     */
    @Override
    public String stringOf(final OptionalLong x) {
        return x == null || x.isEmpty() ? null : N.stringOf(x.getAsLong());
    }

    /**
     * Parses a string representation into an OptionalLong.
     * Empty or {@code null} strings result in an empty OptionalLong.
     * Non-empty strings are parsed as long values and wrapped in OptionalLong.
     *
     * @param str the string to parse
     * @return OptionalLong.empty() if the string is {@code null} or empty, otherwise OptionalLong containing the parsed value
     */
    @Override
    public OptionalLong valueOf(final String str) {
        return Strings.isEmpty(str) ? OptionalLong.empty() : OptionalLong.of(Numbers.toLong(str));
    }

    /**
     * Retrieves an OptionalLong value from the specified column in a ResultSet.
     * If the column value is {@code null}, returns an empty OptionalLong.
     * Otherwise, converts the value to long and wraps it in OptionalLong.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return OptionalLong.empty() if the column is {@code null}, otherwise OptionalLong containing the value
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OptionalLong get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object result = rs.getObject(columnIndex);

        return result == null ? OptionalLong.empty()
                : OptionalLong.of(result instanceof Long num ? num : (result instanceof Number num ? num.longValue() : Numbers.toLong(result.toString())));
    }

    /**
     * Retrieves an OptionalLong value from the specified column in a ResultSet using the column label.
     * If the column value is {@code null}, returns an empty OptionalLong.
     * Otherwise, converts the value to long and wraps it in OptionalLong.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return OptionalLong.empty() if the column is {@code null}, otherwise OptionalLong containing the value
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public OptionalLong get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object result = rs.getObject(columnLabel);

        return result == null ? OptionalLong.empty()
                : OptionalLong.of(result instanceof Long num ? num : (result instanceof Number num ? num.longValue() : Numbers.toLong(result.toString())));
    }

    /**
     * Sets an OptionalLong parameter in a PreparedStatement.
     * If the OptionalLong is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the long value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the OptionalLong to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OptionalLong x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(columnIndex, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.getAsLong());
        }
    }

    /**
     * Sets an OptionalLong parameter in a CallableStatement using a parameter name.
     * If the OptionalLong is {@code null} or empty, sets the parameter to SQL NULL.
     * Otherwise, sets the long value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OptionalLong to set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OptionalLong x) throws SQLException {
        if (x == null || x.isEmpty()) {
            stmt.setNull(parameterName, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.getAsLong());
        }
    }

    /**
     * Appends the string representation of an OptionalLong to an Appendable.
     * Empty optionals are written as "null".
     *
     * @param appendable the Appendable to write to
     * @param x the OptionalLong to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final OptionalLong x) throws IOException {
        if (x == null || x.isEmpty()) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.getAsLong()));
        }
    }

    /**
     * Writes the character representation of an OptionalLong to a CharacterWriter.
     * Empty optionals are written as {@code null}.
     * Present values are written as numeric values without quotes.
     *
     * @param writer the CharacterWriter to write to
     * @param x the OptionalLong to write
     * @param config the serialization configuration (not used for numeric values)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final OptionalLong x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null || x.isEmpty()) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.getAsLong());
        }
    }
}