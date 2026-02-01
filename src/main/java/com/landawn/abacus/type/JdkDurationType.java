/*
 * Copyright (C) 2024 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for java.time.Duration.
 * This class provides serialization, deserialization, and database access capabilities for Duration instances.
 * Durations are stored and transmitted as milliseconds for compatibility and efficiency.
 */
public class JdkDurationType extends AbstractType<Duration> {

    public static final String DURATION = "JdkDuration";

    JdkDurationType() {
        super(DURATION);
    }

    /**
     * Returns the Class object representing the Duration type.
     *
     * @return Duration.class
     */
    @Override
    public Class<Duration> clazz() {
        return Duration.class;
    }

    /**
     * Indicates whether this type should be written without quotes in CSV format.
     * Duration values are numeric (milliseconds) and should not be quoted.
     *
     * @return {@code true}, indicating that Duration values should not be quoted in CSV output
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Duration to its string representation.
     * The duration is represented as the total number of milliseconds.
     *
     * @param x the Duration to convert to string
     * @return the string representation of milliseconds, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Duration x) {
        return (x == null) ? null : N.stringOf(x.toMillis());
    }

    /**
     * Parses a string representation into a Duration.
     * The string should contain a number representing milliseconds.
     *
     * @param str the string containing milliseconds to parse
     * @return the parsed Duration instance, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public Duration valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Duration.ofMillis(Numbers.toLong(str));
    }

    /**
     * Retrieves a Duration value from the specified column in a ResultSet.
     * The method reads a long value from the database representing milliseconds
     * and converts it to a Duration.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the Duration value created from the milliseconds in the column
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Duration get(final ResultSet rs, final int columnIndex) throws SQLException {
        return Duration.ofMillis(rs.getLong(columnIndex));
    }

    /**
     * Retrieves a Duration value from the specified column in a ResultSet using the column label.
     * The method reads a long value from the database representing milliseconds
     * and converts it to a Duration.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return the Duration value created from the milliseconds in the column
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public Duration get(final ResultSet rs, final String columnLabel) throws SQLException {
        return Duration.ofMillis(rs.getLong(columnLabel));
    }

    /**
     * Sets a Duration parameter in a PreparedStatement.
     * The Duration is converted to milliseconds for database storage.
     * If the Duration is {@code null}, 0 is stored.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Duration to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Duration x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.toMillis());
    }

    /**
     * Sets a Duration parameter in a CallableStatement using a parameter name.
     * The Duration is converted to milliseconds for database storage.
     * If the Duration is {@code null}, 0 is stored.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Duration to set, or null
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Duration x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.toMillis());
    }

    /**
     * Appends the string representation of a Duration to an Appendable.
     * The duration is written as milliseconds.
     *
     * @param appendable the Appendable to write to
     * @param x the Duration to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final Duration x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.toMillis()));
        }
    }

    /**
     * Writes the character representation of a Duration to a CharacterWriter.
     * The duration is written as a numeric value (milliseconds) without quotes,
     * regardless of the serialization configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Duration to write
     * @param config the serialization configuration (not used for Duration)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Duration x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.toMillis());
        }
    }
}