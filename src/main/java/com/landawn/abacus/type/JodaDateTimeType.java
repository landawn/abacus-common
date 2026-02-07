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

import org.joda.time.DateTime;

import com.landawn.abacus.util.N;

/**
 * Type handler for Joda-Time DateTime objects.
 * <p>
 * This class provides serialization, deserialization, and database access capabilities for
 * {@code org.joda.time.DateTime} instances. It supports multiple string formats including ISO 8601 formats
 * and provides database conversion using {@code java.sql.Timestamp}.
 *
 * @see org.joda.time.DateTime
 * @see AbstractJodaDateTimeType
 */
public class JodaDateTimeType extends AbstractJodaDateTimeType<DateTime> {

    public static final String JODA_DATE_TIME = "JodaDateTime";

    JodaDateTimeType() {
        super(JODA_DATE_TIME);
    }

    JodaDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Gets the class type for Joda DateTime.
     * This method returns the Class object representing org.joda.time.DateTime, which is used
     * for type identification and reflection operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     * Class<DateTime> clazz = type.clazz();
     * System.out.println(clazz.getName());   // Outputs: org.joda.time.DateTime
     * }</pre>
     *
     * @return the Class object representing org.joda.time.DateTime
     */
    @Override
    public Class<DateTime> clazz() {
        return DateTime.class;
    }

    /**
     * Converts the specified object to a Joda DateTime instance.
     * This method provides flexible conversion from various types including Number, java.util.Date,
     * and String representations. It intelligently handles different input types to create a
     * proper DateTime object.
     *
     * This method handles the following conversions:
     * - Number: treated as milliseconds since epoch and converted to DateTime
     * - java.util.Date: converted using the date's time in milliseconds
     * - String: parsed using the valueOf(String) method
     * - null: returns null
     * - Other types: converted to string first, then parsed
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     *
     * // From Number (milliseconds since epoch)
     * DateTime dt1 = type.valueOf(1609459200000L);
     *
     * // From java.util.Date
     * Date date = new Date();
     * DateTime dt2 = type.valueOf(date);
     *
     * // From String
     * DateTime dt3 = type.valueOf("2021-01-01T00:00:00");
     *
     * // null input returns null
     * DateTime dt4 = type.valueOf(null);   // returns null
     * }</pre>
     *
     * @param obj the object to convert to DateTime
     * @return a DateTime instance, or {@code null} if the input is null
     */
    @Override
    public DateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new DateTime(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new DateTime(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string representation into a Joda DateTime instance.
     * This method supports multiple string formats including ISO 8601 formats and a special
     * "SYS_TIME" keyword for current time. It intelligently detects the format based on
     * string length and content.
     *
     * This method handles the following string formats:
     * - Empty/null string: returns null
     * - "SYS_TIME": returns current system time as DateTime
     * - ISO 8601 date-time format (20 characters): parsed as yyyy-MM-dd'T'HH:mm:ss
     * - ISO 8601 timestamp format (24 characters): parsed as yyyy-MM-dd'T'HH:mm:ss.SSS
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     *
     * // Parse ISO 8601 date-time format
     * DateTime dt1 = type.valueOf("2021-01-01T10:30:00");
     *
     * // Parse ISO 8601 timestamp format
     * DateTime dt2 = type.valueOf("2021-01-01T10:30:00.123");
     *
     * // Get current system time
     * DateTime dt3 = type.valueOf("SYS_TIME");
     *
     * // Empty or null returns null
     * DateTime dt4 = type.valueOf(null);   // returns null
     * DateTime dt5 = type.valueOf("");     // returns null
     * }</pre>
     *
     * @param str the string to parse
     * @return a DateTime instance, or {@code null} if the string is empty or null
     * @throws IllegalArgumentException if the string format is invalid
     */
    @Override
    public DateTime valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return new DateTime(System.currentTimeMillis());
        }

        return str.length() == 20 ? jodaISO8601DateTimeFT.parseDateTime(str) : jodaISO8601TimestampFT.parseDateTime(str);
    }

    /**
     * Parses a character array into a Joda DateTime instance.
     * This method provides efficient parsing from character arrays, attempting numeric parsing first
     * for performance, then falling back to string parsing. This is useful for parsing data from
     * streams or buffers without creating intermediate String objects.
     *
     * This method first attempts to parse the character array as a long value (milliseconds since epoch).
     * If that fails, it converts the character array to a string and delegates to valueOf(String).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     *
     * // Parse from character array containing milliseconds
     * char[] millisChars = "1609459200000".toCharArray();
     * DateTime dt1 = type.valueOf(millisChars, 0, millisChars.length);
     *
     * // Parse from character array containing ISO 8601 format
     * char[] isoChars = "2021-01-01T10:30:00".toCharArray();
     * DateTime dt2 = type.valueOf(isoChars, 0, isoChars.length);
     *
     * // Parse substring from character array
     * char[] buffer = "prefix2021-01-01T10:30:00suffix".toCharArray();
     * DateTime dt3 = type.valueOf(buffer, 6, 19);
     *
     * // null or empty array returns null
     * DateTime dt4 = type.valueOf(null, 0, 0);   // returns null
     * }</pre>
     *
     * @param cbuf the character buffer containing the value to parse
     * @param offset the start offset in the character buffer
     * @param len the number of characters to parse
     * @return a DateTime instance, or {@code null} if the character buffer is {@code null} or length is 0
     */
    @Override
    public DateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return new DateTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a DateTime value from the specified column in a ResultSet.
     * This method provides database-to-Java type conversion for DateTime objects, reading
     * from SQL TIMESTAMP columns and converting them to Joda-Time DateTime instances.
     *
     * This method reads a Timestamp from the ResultSet and converts it to a Joda DateTime.
     * If the timestamp is {@code null}, this method returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     *
     * try (Connection conn = dataSource.getConnection();
     *      PreparedStatement stmt = conn.prepareStatement("SELECT created_at FROM events WHERE id = ?")) {
     *     stmt.setInt(1, eventId);
     *     try (ResultSet rs = stmt.executeQuery()) {
     *         if (rs.next()) {
     *             DateTime createdAt = type.get(rs, 1);
     *             System.out.println("Event created at: " + createdAt);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a DateTime instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public DateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : new DateTime(ts.getTime());
    }

    /**
     * Retrieves a DateTime value from the specified column in a ResultSet using column label.
     * This method provides database-to-Java type conversion for DateTime objects by column name,
     * reading from SQL TIMESTAMP columns and converting them to Joda-Time DateTime instances.
     *
     * This method reads a Timestamp from the ResultSet and converts it to a Joda DateTime.
     * If the timestamp is {@code null}, this method returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     *
     * try (Connection conn = dataSource.getConnection();
     *      PreparedStatement stmt = conn.prepareStatement("SELECT created_at, updated_at FROM events WHERE id = ?")) {
     *     stmt.setInt(1, eventId);
     *     try (ResultSet rs = stmt.executeQuery()) {
     *         if (rs.next()) {
     *             DateTime createdAt = type.get(rs, "created_at");
     *             DateTime updatedAt = type.get(rs, "updated_at");
     *             System.out.println("Created: " + createdAt + ", Updated: " + updatedAt);
     *         }
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the column label to retrieve the value from
     * @return a DateTime instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public DateTime get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : new DateTime(ts.getTime());
    }

    /**
     * Sets a DateTime parameter in a PreparedStatement.
     * This method provides Java-to-database type conversion, transforming Joda-Time DateTime
     * objects into SQL TIMESTAMP values for database insertion or update operations.
     *
     * This method converts the Joda DateTime to a SQL Timestamp before setting it in the statement.
     * If the DateTime is {@code null}, a SQL NULL is set for the parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     * DateTime eventTime = new DateTime(2021, 1, 1, 10, 30, 0);
     *
     * try (Connection conn = dataSource.getConnection();
     *      PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (name, created_at) VALUES (?, ?)")) {
     *     stmt.setString(1, "Conference");
     *     type.set(stmt, 2, eventTime);
     *     stmt.executeUpdate();
     * }
     *
     * // Setting null value
     * type.set(stmt, 2, null);   // Sets SQL NULL
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the DateTime value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final DateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Sets a named DateTime parameter in a CallableStatement.
     * This method provides Java-to-database type conversion for stored procedure calls,
     * transforming Joda-Time DateTime objects into SQL TIMESTAMP values using named parameters.
     *
     * This method converts the Joda DateTime to a SQL Timestamp before setting it in the statement.
     * If the DateTime is {@code null}, a SQL NULL is set for the parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<DateTime> type = TypeFactory.getType(DateTime.class);
     * DateTime eventTime = new DateTime(2021, 1, 1, 10, 30, 0);
     *
     * try (Connection conn = dataSource.getConnection();
     *      CallableStatement stmt = conn.prepareCall("{call create_event(?, ?)}")) {
     *     stmt.setString("event_name", "Conference");
     *     type.set(stmt, "event_time", eventTime);
     *     stmt.execute();
     * }
     *
     * // Setting null value
     * type.set(stmt, "event_time", null);   // Sets SQL NULL
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the DateTime value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final DateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }
}