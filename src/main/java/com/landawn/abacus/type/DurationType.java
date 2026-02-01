/*
 * Copyright (C) 2015 HaiYang Li
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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Duration values.
 * This class provides serialization, deserialization, and database operations for Duration objects.
 * Durations are stored as milliseconds (long values) in the database and string representations.
 */
public class DurationType extends AbstractType<Duration> {

    public static final String DURATION = Duration.class.getSimpleName();

    DurationType() {
        super(DURATION);
    }

    /**
     * Indicates whether Duration values are comparable.
     * Duration implements Comparable, so this returns {@code true}.
     *
     * @return {@code true}, indicating Duration values are comparable
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing Duration.class
     */
    @Override
    public Class<Duration> clazz() {
        return Duration.class;
    }

    /**
     * Indicates whether this type should be quoted in CSV format.
     * Duration values are numeric (milliseconds) and don't require quotes.
     *
     * @return {@code true}, indicating Duration values should not be quoted in CSV
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Duration to its string representation.
     * The duration is represented as the number of milliseconds.
     *
     * @param x the Duration to convert. Can be {@code null}.
     * @return A string containing the milliseconds value, or {@code null} if input is null
     */
    @Override
    public String stringOf(final Duration x) {
        return (x == null) ? null : N.stringOf(x.toMillis());
    }

    /**
     * Converts a string representation back to a Duration.
     * The string should contain a numeric value representing milliseconds.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Duration> type = TypeFactory.getType(Duration.class);
     * Duration duration1 = type.valueOf("5000");
     * // Creates 5-second duration
     *
     * Duration duration2 = type.valueOf("60000");
     * // Creates 1-minute duration
     *
     * Duration duration3 = type.valueOf(null);
     * // Returns null
     * }</pre>
     *
     * @param str the string containing milliseconds value. Can be {@code null} or empty.
     * @return A Duration created from the milliseconds value, or {@code null} if input is null/empty
     */
    @Override
    public Duration valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Duration.ofMillis(Numbers.toLong(str));
    }

    /**
     * Retrieves a Duration value from a ResultSet at the specified column index.
     * The database value is read as a long (milliseconds) and converted to Duration.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the duration value
     * @return A Duration created from the milliseconds value in the database
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Duration get(final ResultSet rs, final int columnIndex) throws SQLException {
        return Duration.ofMillis(rs.getLong(columnIndex));
    }

    /**
     * Retrieves a Duration value from a ResultSet using the specified column label.
     * The database value is read as a long (milliseconds) and converted to Duration.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the duration value
     * @return A Duration created from the milliseconds value in the database
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Duration get(final ResultSet rs, final String columnLabel) throws SQLException {
        return Duration.ofMillis(rs.getLong(columnLabel));
    }

    /**
     * Sets a Duration value as a parameter in a PreparedStatement.
     * The duration is stored as milliseconds (long value) in the database.
     * Null durations are stored as 0.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Duration value to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Duration x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.toMillis());
    }

    /**
     * Sets a Duration value as a named parameter in a CallableStatement.
     * The duration is stored as milliseconds (long value) in the database.
     * Null durations are stored as 0.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the Duration value to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Duration x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.toMillis());
    }

    /**
     * Appends a Duration value to an Appendable output.
     * The duration is written as its milliseconds value.
     *
     * @param appendable the Appendable to write to
     * @param x the Duration to append. Can be {@code null}.
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
     * Writes a Duration value to a CharacterWriter.
     * The duration is written as its milliseconds value using optimized numeric writing.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Duration to write. Can be {@code null}.
     * @param config the serialization configuration (currently unused for Duration)
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