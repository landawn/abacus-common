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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for {@link java.time.LocalDateTime} values.
 * Provides serialization, deserialization, and JDBC integration for Java's {@code LocalDateTime} type,
 * supporting conversions to and from strings, milliseconds since the epoch, and SQL timestamps.
 *
 * <p>String representations follow the ISO-8601 standard (e.g., {@code "2024-03-15T10:30:00"}).
 * Database columns are read and written using JDBC's native {@code LocalDateTime} support with a
 * {@link java.sql.Timestamp} fallback for older drivers.</p>
 *
 * @see AbstractTemporalType
 * @see java.time.LocalDateTime
 */
public class LocalDateTimeType extends AbstractTemporalType<LocalDateTime> {

    /** The type name constant for LocalDateTime type identification, equal to {@code "LocalDateTime"}. */
    public static final String LOCAL_DATE_TIME = LocalDateTime.class.getSimpleName();

    /**
     * Package-private constructor for LocalDateTimeType.
     * This constructor is called by the TypeFactory to create LocalDateTime type instances.
     */
    LocalDateTimeType() {
        super(LOCAL_DATE_TIME);
    }

    /**
     * Returns the Class object representing the LocalDateTime type.
     * This method provides type identification for reflection and serialization operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     * Class<LocalDateTime> clazz = type.javaType();
     * System.out.println(clazz.getName());   // Outputs: java.time.LocalDateTime
     * }</pre>
     *
     * @return The Class object for LocalDateTime
     */
    @Override
    public Class<LocalDateTime> javaType() {
        return LocalDateTime.class;
    }

    /**
     * Converts a LocalDateTime object to its string representation.
     * Uses {@code LocalDateTime.toString()}, which produces an ISO-8601 compatible string. The
     * seconds field is omitted when zero (e.g., {@code "2021-01-01T10:30"}), and a fractional
     * part is appended when sub-second precision is present (e.g., {@code "2021-01-01T10:30:00.123456789"}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     * LocalDateTime dateTime = LocalDateTime.of(2021, 1, 1, 10, 30, 30);
     * String str = type.stringOf(dateTime);
     * System.out.println(str);   // Outputs: 2021-01-01T10:30:30
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the LocalDateTime object to convert; may be {@code null}
     * @return the ISO-8601 string representation of the LocalDateTime, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final LocalDateTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an Object to a LocalDateTime.
     * This method provides flexible conversion from various types to LocalDateTime,
     * supporting numeric timestamps and string representations.
     *
     * If the object is a Number, it is treated as milliseconds since epoch and converted to LocalDateTime using the default zone ID.
     * Otherwise, the object is converted to a string and parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     *
     * // From Number (milliseconds since epoch)
     * LocalDateTime dt1 = type.valueOf(1609459200000L);
     *
     * // From String
     * LocalDateTime dt2 = type.valueOf("2021-01-01T10:30:00");
     * }</pre>
     *
     * @param obj The object to convert to LocalDateTime
     * @return The LocalDateTime representation of the object, or {@code null} if the input is null
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
     * This method supports multiple string formats with intelligent format detection:
     * <ul>
     *   <li>{@code null}, empty string, or the literal {@code "null"} (case-insensitive) returns {@code null}</li>
     *   <li>{@code "SYS_TIME"} returns the current {@code LocalDateTime}</li>
     *   <li>Numeric strings are treated as milliseconds since the epoch</li>
     *   <li>ISO-8601 formatted strings are parsed directly via {@link LocalDateTime#parse(CharSequence)}</li>
     * </ul>
     *
     * <p>Every string produced by {@link LocalDateTime#toString()} can be parsed back into an equivalent value,
     * including the seconds-omitted form (e.g. {@code "2021-01-01T10:30"}), the seconds form
     * (e.g. {@code "2021-01-01T10:30:45"}), and fractional seconds of any precision
     * (e.g. {@code "2021-01-01T10:30:45.123456789"}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     *
     * // Parse ISO-8601 format
     * LocalDateTime dt1 = type.valueOf("2021-01-01T10:30:00");
     *
     * // Parse milliseconds
     * LocalDateTime dt2 = type.valueOf("1609459200000");
     *
     * // Get current time
     * LocalDateTime dt3 = type.valueOf("SYS_TIME");
     * }</pre>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Because {@code stringOf} delegates to
     * {@link LocalDateTime#toString()}, the value returned by {@code toString()} round-trips as well.</p>
     *
     * @param str the string to parse
     * @return the parsed {@code LocalDateTime} object, or {@code null} if the input is {@code null}, empty, or the literal {@code "null"}
     * @throws java.time.format.DateTimeParseException if the string is not a valid millisecond
     *         number nor an ISO-8601 {@code LocalDateTime} representation
     * @see #valueOf(Object)
     * @see #stringOf(LocalDateTime)
     */
    @Override
    public LocalDateTime valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return LocalDateTime.now();
        }

        if (isPossibleMillis(str)) {
            try {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return LocalDateTime.parse(str);
    }

    /**
     * Converts a character array to a LocalDateTime object.
     *
     * The character array is first converted to a string, then parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     *
     * char[] chars = "2021-01-01T10:30:00".toCharArray();
     * LocalDateTime dt = type.valueOf(chars, 0, chars.length);
     * }</pre>
     *
     * @param cbuf The character array containing the LocalDateTime representation
     * @param offset The starting position in the character array
     * @param len The number of characters to use
     * @return The parsed LocalDateTime object, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public LocalDateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a LocalDateTime value from a ResultSet at the specified column index.
     * This method provides robust database-to-Java type conversion with fallback support
     * for databases that don't natively support Java 8 time types.
     *
     * First attempts to get the value as a LocalDateTime object directly. If that fails,
     * falls back to retrieving it as a Timestamp and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalDateTime dt = type.get(rs, 1);
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return The LocalDateTime value from the ResultSet, or {@code null} if the database value is NULL
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
     * This method provides robust database-to-Java type conversion by column name with fallback support.
     *
     * First attempts to get the value as a LocalDateTime object directly. If that fails,
     * falls back to retrieving it as a Timestamp and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalDateTime dt = type.get(rs, "created_at");
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnName the column label (or name if no label was specified) to retrieve the value from
     * @return The LocalDateTime value from the ResultSet, or {@code null} if the database value is NULL
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
     * This method provides robust Java-to-database type conversion with fallback support
     * for databases that don't natively support Java 8 time types.
     *
     * First attempts to set the value as a LocalDateTime object directly. If that fails,
     * falls back to setting it as a Timestamp.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     * LocalDateTime dt = LocalDateTime.now();
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (timestamp) VALUES (?)")) {
     *     type.set(stmt, 1, dt);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The LocalDateTime value to set, or {@code null} to set SQL NULL
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
     * This method provides robust Java-to-database type conversion for stored procedures
     * with fallback support for databases that don't natively support Java 8 time types.
     *
     * First attempts to set the value as a LocalDateTime object directly. If that fails,
     * falls back to setting it as a Timestamp.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDateTime> type = TypeFactory.getType(LocalDateTime.class);
     * LocalDateTime dt = LocalDateTime.now();
     * try (CallableStatement stmt = conn.prepareCall("{call log_event(?)}")) {
     *     type.set(stmt, "event_time", dt);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The LocalDateTime value to set, or {@code null} to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final LocalDateTime x) throws SQLException {
        try {
            stmt.setObject(parameterName, x);
        } catch (final SQLException e) {
            stmt.setTimestamp(parameterName, x == null ? null : Timestamp.valueOf(x));
        }
    }
}
