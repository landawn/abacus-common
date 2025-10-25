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
import java.sql.Time;
import java.time.Instant;
import java.time.LocalTime;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class LocalTimeType extends AbstractTemporalType<LocalTime> {

    public static final String LOCAL_TIME = LocalTime.class.getSimpleName();

    LocalTimeType() {
        super(LOCAL_TIME);
    }

    /**
     * Returns the Class object representing the LocalTime type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * Class<LocalTime> clazz = type.clazz();
     * }</pre>
     *
     * @return The Class object for LocalTime
     */
    @Override
    public Class<LocalTime> clazz() {
        return LocalTime.class;
    }

    /**
     * Converts a LocalTime object to its string representation.
     * The string format follows the ISO-8601 standard (HH:mm:ss).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * LocalTime time = LocalTime.of(10, 30, 0);
     * String str = type.stringOf(time); // "10:30:00"
     * }</pre>
     *
     * @param x The LocalTime object to convert
     * @return The string representation of the LocalTime, or null if the input is null
     */
    @Override
    public String stringOf(final LocalTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an Object to a LocalTime.
     * If the object is a Number, it is treated as milliseconds since epoch and converted to LocalTime using the default zone ID.
     * Otherwise, the object is converted to a string and parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * LocalTime time1 = type.valueOf(1609459200000L);
     * LocalTime time2 = type.valueOf("10:30:00");
     * }</pre>
     *
     * @param obj The object to convert to LocalTime
     * @return The LocalTime representation of the object, or null if the input is null
     */
    @Override
    public LocalTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return LocalTime.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string to create a LocalTime object.
     * The method supports multiple formats:
     * - Empty string returns null
     * - "SYS_TIME" returns the current LocalTime
     * - Numeric strings are treated as milliseconds since epoch
     * - ISO-8601 formatted strings are parsed directly
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * LocalTime time1 = type.valueOf("10:30:00");
     * LocalTime time2 = type.valueOf("SYS_TIME");
     * LocalTime time3 = type.valueOf("1609459200000");
     * }</pre>
     *
     * @param str The string to parse
     * @return The parsed LocalTime object, or null if the input is null or empty
     * @throws java.time.format.DateTimeParseException if the string cannot be parsed as a LocalTime
     */
    @MayReturnNull
    @Override
    public LocalTime valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return LocalTime.now();
        }

        if (isPossibleLong(str)) {
            try {
                return LocalTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e2) {
                // ignore;
            }
        }

        return LocalTime.parse(str);
    }

    /**
     * Converts a character array to a LocalTime object.
     * The character array is first converted to a string, then parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * char[] chars = "10:30:00".toCharArray();
     * LocalTime time = type.valueOf(chars, 0, chars.length);
     * }</pre>
     *
     * @param cbuf The character array containing the LocalTime representation
     * @param offset The starting position in the character array
     * @param len The number of characters to use
     * @return The parsed LocalTime object, or null if the input is null or empty
     */
    @MayReturnNull
    @Override
    public LocalTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a LocalTime value from a ResultSet at the specified column index.
     * First attempts to get the value as a LocalTime object directly. If that fails,
     * falls back to retrieving it as a java.sql.Time and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalTime time = type.get(rs, 1);
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return The LocalTime value from the ResultSet, or null if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public LocalTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        try {
            return rs.getObject(columnIndex, LocalTime.class);
        } catch (final SQLException e) {
            final Time ts = rs.getTime(columnIndex);

            return ts == null ? null : ts.toLocalTime();
        }
    }

    /**
     * Retrieves a LocalTime value from a ResultSet using the specified column name.
     * First attempts to get the value as a LocalTime object directly. If that fails,
     * falls back to retrieving it as a java.sql.Time and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalTime time = type.get(rs, "start_time");
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnName The name of the column to retrieve the value from
     * @return The LocalTime value from the ResultSet, or null if the database value is NULL
     * @throws SQLException if a database access error occurs or the column name is not found
     */
    @Override
    public LocalTime get(final ResultSet rs, final String columnName) throws SQLException {
        try {
            return rs.getObject(columnName, LocalTime.class);
        } catch (final SQLException e) {
            final Time ts = rs.getTime(columnName);

            return ts == null ? null : ts.toLocalTime();
        }
    }

    /**
     * Sets a LocalTime parameter in a PreparedStatement at the specified position.
     * First attempts to set the value as a LocalTime object directly. If that fails,
     * falls back to setting it as a java.sql.Time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * LocalTime time = LocalTime.of(10, 30, 0);
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO schedules (start_time) VALUES (?)")) {
     *     type.set(stmt, 1, time);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The LocalTime value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final LocalTime x) throws SQLException {
        try {
            stmt.setObject(columnIndex, x);
        } catch (final SQLException e) {
            stmt.setTime(columnIndex, x == null ? null : Time.valueOf(x));
        }
    }

    /**
     * Sets a LocalTime parameter in a CallableStatement using the specified parameter name.
     * First attempts to set the value as a LocalTime object directly. If that fails,
     * falls back to setting it as a java.sql.Time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalTimeType type = new LocalTimeType();
     * LocalTime time = LocalTime.of(10, 30, 0);
     * try (CallableStatement stmt = conn.prepareCall("{call update_schedule(?)}")) {
     *     type.set(stmt, "start_time", time);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param columnName The name of the parameter to set
     * @param x The LocalTime value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String columnName, final LocalTime x) throws SQLException {
        try {
            stmt.setObject(columnName, x);
        } catch (final SQLException e) {
            stmt.setTime(columnName, x == null ? null : Time.valueOf(x));
        }
    }
}