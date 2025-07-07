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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class LocalDateTimeType extends AbstractTemporalType<LocalDateTime> {

    public static final String LOCAL_DATE_TIME = LocalDateTime.class.getSimpleName();

    LocalDateTimeType() {
        super(LOCAL_DATE_TIME);
    }

    /**
     * Returns the Class object representing the LocalDateTime type.
     *
     * @return The Class object for LocalDateTime
     */
    @Override
    public Class<LocalDateTime> clazz() {
        return LocalDateTime.class;
    }

    /**
     * Converts a LocalDateTime object to its string representation.
     * The string format follows the ISO-8601 standard (yyyy-MM-ddTHH:mm:ss).
     *
     * @param x The LocalDateTime object to convert
     * @return The string representation of the LocalDateTime, or null if the input is null
     */
    @Override
    public String stringOf(final LocalDateTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an Object to a LocalDateTime.
     * If the object is a Number, it is treated as milliseconds since epoch and converted to LocalDateTime using the default zone ID.
     * Otherwise, the object is converted to a string and parsed.
     *
     * @param obj The object to convert to LocalDateTime
     * @return The LocalDateTime representation of the object, or null if the input is null
     */
    @Override
    public LocalDateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string to create a LocalDateTime object.
     * The method supports multiple formats:
     * - Empty string returns null
     * - "SYS_TIME" returns the current LocalDateTime
     * - Numeric strings are treated as milliseconds since epoch
     * - ISO-8601 formatted strings are parsed directly
     *
     * @param str The string to parse
     * @return The parsed LocalDateTime object, or null if the input is null or empty
     * @throws java.time.format.DateTimeParseException if the string cannot be parsed as a LocalDateTime
     */
    @MayReturnNull
    @Override
    public LocalDateTime valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return LocalDateTime.now();
        }

        if (isPossibleLong(str)) {
            try {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e2) {
                // ignore;
            }
        }

        return LocalDateTime.parse(str);
    }

    /**
     * Converts a character array to a LocalDateTime object.
     * The character array is first converted to a string, then parsed.
     *
     * @param cbuf The character array containing the LocalDateTime representation
     * @param offset The starting position in the character array
     * @param len The number of characters to use
     * @return The parsed LocalDateTime object, or null if the input is null or empty
     */
    @MayReturnNull
    @Override
    public LocalDateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a LocalDateTime value from a ResultSet at the specified column index.
     * First attempts to get the value as a LocalDateTime object directly. If that fails,
     * falls back to retrieving it as a Timestamp and converting it.
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return The LocalDateTime value from the ResultSet, or null if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public LocalDateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        try {
            return rs.getObject(columnIndex, LocalDateTime.class);
        } catch (final SQLException e) {
            final Timestamp ts = rs.getTimestamp(columnIndex);

            return ts == null ? null : ts.toLocalDateTime();
        }
    }

    /**
     * Retrieves a LocalDateTime value from a ResultSet using the specified column name.
     * First attempts to get the value as a LocalDateTime object directly. If that fails,
     * falls back to retrieving it as a Timestamp and converting it.
     *
     * @param rs The ResultSet containing the data
     * @param columnName The name of the column to retrieve the value from
     * @return The LocalDateTime value from the ResultSet, or null if the database value is NULL
     * @throws SQLException if a database access error occurs or the column name is not found
     */
    @Override
    public LocalDateTime get(final ResultSet rs, final String columnName) throws SQLException {
        try {
            return rs.getObject(columnName, LocalDateTime.class);
        } catch (final SQLException e) {
            final Timestamp ts = rs.getTimestamp(columnName);

            return ts == null ? null : ts.toLocalDateTime();
        }
    }

    /**
     * Sets a LocalDateTime parameter in a PreparedStatement at the specified position.
     * First attempts to set the value as a LocalDateTime object directly. If that fails,
     * falls back to setting it as a Timestamp.
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The LocalDateTime value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final LocalDateTime x) throws SQLException {
        try {
            stmt.setObject(columnIndex, x);
        } catch (final SQLException e) {
            stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.valueOf(x));
        }
    }

    /**
     * Sets a LocalDateTime parameter in a CallableStatement using the specified parameter name.
     * First attempts to set the value as a LocalDateTime object directly. If that fails,
     * falls back to setting it as a Timestamp.
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param columnName The name of the parameter to set
     * @param x The LocalDateTime value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String columnName, final LocalDateTime x) throws SQLException {
        try {
            stmt.setObject(columnName, x);
        } catch (final SQLException e) {
            stmt.setTimestamp(columnName, x == null ? null : Timestamp.valueOf(x));
        }
    }
}