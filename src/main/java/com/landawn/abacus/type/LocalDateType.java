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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class LocalDateType extends AbstractTemporalType<LocalDate> {

    public static final String LOCAL_DATE = LocalDate.class.getSimpleName();

    LocalDateType() {
        super(LOCAL_DATE);
    }

    /**
     * Returns the Class object representing the LocalDate type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * Class<LocalDate> clazz = type.clazz();
     * }</pre>
     *
     * @return The Class object for LocalDate
     */
    @Override
    public Class<LocalDate> clazz() {
        return LocalDate.class;
    }

    /**
     * Converts a LocalDate object to its string representation.
     * The string format follows the ISO-8601 standard (yyyy-MM-dd).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * LocalDate date = LocalDate.of(2021, 1, 1);
     * String str = type.stringOf(date); // "2021-01-01"
     * }</pre>
     *
     * @param x The LocalDate object to convert
     * @return The string representation of the LocalDate, or {@code null} if the input is null
     */
    @Override
    @MayReturnNull
    public String stringOf(final LocalDate x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an Object to a LocalDate.
     * If the object is a Number, it is treated as milliseconds since epoch and converted to LocalDate using the default zone ID.
     * Otherwise, the object is converted to a string and parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * LocalDate date1 = type.valueOf(1609459200000L);
     * LocalDate date2 = type.valueOf("2021-01-01");
     * }</pre>
     *
     * @param obj The object to convert to LocalDate
     * @return The LocalDate representation of the object, or {@code null} if the input is null
     */
    @Override
    @MayReturnNull
    public LocalDate valueOf(final Object obj) {
        if (obj instanceof Number) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string to create a LocalDate object.
     * The method supports multiple formats:
     * - Empty string returns null
     * - "SYS_TIME" returns the current LocalDate
     * - Numeric strings are treated as milliseconds since epoch
     * - ISO-8601 formatted strings are parsed directly
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * LocalDate date1 = type.valueOf("2021-01-01");
     * LocalDate date2 = type.valueOf("SYS_TIME");
     * LocalDate date3 = type.valueOf("1609459200000");
     * }</pre>
     *
     * @param str The string to parse
     * @return The parsed LocalDate object, or {@code null} if the input is {@code null} or empty
     * @throws java.time.format.DateTimeParseException if the string cannot be parsed as a LocalDate
     */
    @MayReturnNull
    @Override

    public LocalDate valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return LocalDate.now();
        }

        if (isPossibleLong(str)) {
            try {
                return LocalDate.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e2) {
                // ignore;
            }
        }

        return LocalDate.parse(str);
    }

    /**
     * Converts a character array to a LocalDate object.
     * The character array is first converted to a string, then parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * char[] chars = "2021-01-01".toCharArray();
     * LocalDate date = type.valueOf(chars, 0, chars.length);
     * }</pre>
     *
     * @param cbuf The character array containing the LocalDate representation
     * @param offset The starting position in the character array
     * @param len The number of characters to use
     * @return The parsed LocalDate object, or {@code null} if the input is {@code null} or empty
     */
    @MayReturnNull
    @Override

    public LocalDate valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a LocalDate value from a ResultSet at the specified column index.
     * First attempts to get the value as a LocalDate object directly. If that fails,
     * falls back to retrieving it as a java.sql.Date and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalDate date = type.get(rs, 1);
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return The LocalDate value from the ResultSet, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    @MayReturnNull
    public LocalDate get(final ResultSet rs, final int columnIndex) throws SQLException {
        try {
            return rs.getObject(columnIndex, LocalDate.class);
        } catch (final SQLException e) {
            final Date ts = rs.getDate(columnIndex);

            return ts == null ? null : ts.toLocalDate();
        }
    }

    /**
     * Retrieves a LocalDate value from a ResultSet using the specified column name.
     * First attempts to get the value as a LocalDate object directly. If that fails,
     * falls back to retrieving it as a java.sql.Date and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalDate date = type.get(rs, "birth_date");
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnName The name of the column to retrieve the value from
     * @return The LocalDate value from the ResultSet, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column name is not found
     */
    @Override
    @MayReturnNull
    public LocalDate get(final ResultSet rs, final String columnName) throws SQLException {
        try {
            return rs.getObject(columnName, LocalDate.class);
        } catch (final SQLException e) {
            final Date ts = rs.getDate(columnName);

            return ts == null ? null : ts.toLocalDate();
        }
    }

    /**
     * Sets a LocalDate parameter in a PreparedStatement at the specified position.
     * First attempts to set the value as a LocalDate object directly. If that fails,
     * falls back to setting it as a java.sql.Date.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * LocalDate date = LocalDate.of(2021, 1, 1);
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (birth_date) VALUES (?)")) {
     *     type.set(stmt, 1, date);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The LocalDate value to set, or {@code null} to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final LocalDate x) throws SQLException {
        try {
            stmt.setObject(columnIndex, x);
        } catch (final SQLException e) {
            stmt.setDate(columnIndex, x == null ? null : Date.valueOf(x));
        }
    }

    /**
     * Sets a LocalDate parameter in a CallableStatement using the specified parameter name.
     * First attempts to set the value as a LocalDate object directly. If that fails,
     * falls back to setting it as a java.sql.Date.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LocalDateType type = new LocalDateType();
     * LocalDate date = LocalDate.of(2021, 1, 1);
     * try (CallableStatement stmt = conn.prepareCall("{call update_birth_date(?)}")) {
     *     type.set(stmt, "birth_date", date);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param columnName The name of the parameter to set
     * @param x The LocalDate value to set, or {@code null} to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String columnName, final LocalDate x) throws SQLException {
        try {
            stmt.setObject(columnName, x);
        } catch (final SQLException e) {
            stmt.setDate(columnName, x == null ? null : Date.valueOf(x));
        }
    }
}
