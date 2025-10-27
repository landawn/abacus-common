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

import org.joda.time.MutableDateTime;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class JodaMutableDateTimeType extends AbstractJodaDateTimeType<MutableDateTime> {

    public static final String JODA_MUTABLE_DATE_TIME = "JodaMutableDateTime";

    JodaMutableDateTimeType() {
        super(JODA_MUTABLE_DATE_TIME);
    }

    JodaMutableDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Gets the class type for Joda MutableDateTime.
     * This method returns the Class object representing org.joda.time.MutableDateTime, which is used
     * for type identification and reflection operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     * Class<MutableDateTime> clazz = type.clazz();
     * System.out.println(clazz.getName()); // Outputs: org.joda.time.MutableDateTime
     * }</pre>
     *
     * @return the Class object representing org.joda.time.MutableDateTime
     */
    @Override
    public Class<MutableDateTime> clazz() {
        return MutableDateTime.class;
    }

    /**
     * Converts the specified object to a Joda MutableDateTime instance.
     * This method provides flexible conversion from various types, creating mutable DateTime
     * objects that can be modified after creation.
     *
     * This method handles the following conversions:
     * - Number: treated as milliseconds since epoch and converted to MutableDateTime
     * - java.util.Date: converted using the date's time in milliseconds
     * - String: parsed using the valueOf(String) method
     * - null: returns null
     * - Other types: converted to string first, then parsed
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     *
     * // From Number (milliseconds since epoch)
     * MutableDateTime dt1 = type.valueOf(1609459200000L);
     *
     * // From java.util.Date
     * Date date = new Date();
     * MutableDateTime dt2 = type.valueOf(date);
     *
     * // From String
     * MutableDateTime dt3 = type.valueOf("2021-01-01T00:00:00");
     *
     * // Modify the mutable datetime
     * dt3.addDays(1);
     * }</pre>
     *
     * @param obj the object to convert to MutableDateTime
     * @return a MutableDateTime instance, or {@code null} if the input is null
     */
    @Override
    @MayReturnNull
    public MutableDateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new MutableDateTime(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new MutableDateTime(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string representation into a Joda MutableDateTime instance.
     * This method supports multiple string formats, creating mutable DateTime objects
     * that can be modified after parsing.
     *
     * This method handles the following string formats:
     * - Empty/null string: returns null
     * - "SYS_TIME": returns current system time as MutableDateTime
     * - ISO 8601 date-time format (20 characters): parsed as yyyy-MM-dd'T'HH:mm:ss
     * - ISO 8601 timestamp format (other lengths): parsed as yyyy-MM-dd'T'HH:mm:ss.SSS
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     *
     * // Parse ISO 8601 date-time format
     * MutableDateTime dt1 = type.valueOf("2021-01-01T10:30:00");
     *
     * // Parse ISO 8601 timestamp format
     * MutableDateTime dt2 = type.valueOf("2021-01-01T10:30:00.123");
     *
     * // Get current system time
     * MutableDateTime dt3 = type.valueOf("SYS_TIME");
     *
     * // Modify after parsing
     * dt1.addHours(2);
     * }</pre>
     *
     * @param str the string to parse
     * @return a MutableDateTime instance, or {@code null} if the string is empty or null
     * @throws IllegalArgumentException if the string format is invalid
     */
    @MayReturnNull
    @Override

    public MutableDateTime valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return new MutableDateTime(System.currentTimeMillis());
        }

        return str.length() == 20 ? jodaISO8601DateTimeFT.parseMutableDateTime(str) : jodaISO8601TimestampFT.parseMutableDateTime(str);
    }

    /**
     * Parses a character array into a Joda MutableDateTime instance.
     * This method provides efficient parsing from character arrays without creating intermediate
     * String objects, useful for high-performance scenarios.
     *
     * This method first attempts to parse the character array as a long value (milliseconds since epoch).
     * If that fails, it converts the character array to a string and delegates to valueOf(String).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     *
     * // Parse from character array containing milliseconds
     * char[] millisChars = "1609459200000".toCharArray();
     * MutableDateTime dt1 = type.valueOf(millisChars, 0, millisChars.length);
     *
     * // Parse from character array containing ISO 8601 format
     * char[] isoChars = "2021-01-01T10:30:00".toCharArray();
     * MutableDateTime dt2 = type.valueOf(isoChars, 0, isoChars.length);
     * }</pre>
     *
     * @param cbuf the character buffer containing the value to parse
     * @param offset the start offset in the character buffer
     * @param len the number of characters to parse
     * @return a MutableDateTime instance, or {@code null} if the character buffer is {@code null} or length is 0
     */
    @MayReturnNull
    @Override

    public MutableDateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return new MutableDateTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a MutableDateTime value from the specified column in a ResultSet.
     * This method provides database-to-Java type conversion for MutableDateTime objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         MutableDateTime dt = type.get(rs, 1);
     *         dt.addDays(1); // Can modify after retrieval
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a MutableDateTime instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    @MayReturnNull
    public MutableDateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : new MutableDateTime(ts.getTime());
    }

    /**
     * Retrieves a MutableDateTime value from the specified column in a ResultSet using column label.
     * This method provides database-to-Java type conversion for MutableDateTime objects by column name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         MutableDateTime dt = type.get(rs, "created_at");
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label to retrieve the value from
     * @return a MutableDateTime instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    @MayReturnNull
    public MutableDateTime get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);

        return ts == null ? null : new MutableDateTime(ts.getTime());
    }

    /**
     * Sets a MutableDateTime parameter in a PreparedStatement.
     * This method provides Java-to-database type conversion for MutableDateTime objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     * MutableDateTime dt = new MutableDateTime();
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (timestamp) VALUES (?)")) {
     *     type.set(stmt, 1, dt);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the MutableDateTime value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableDateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Sets a named MutableDateTime parameter in a CallableStatement.
     * This method provides Java-to-database type conversion for stored procedure calls
     * using named parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JodaMutableDateTimeType type = new JodaMutableDateTimeType();
     * MutableDateTime dt = new MutableDateTime();
     * try (CallableStatement stmt = conn.prepareCall("{call log_event(?)}")) {
     *     type.set(stmt, "event_time", dt);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the MutableDateTime value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableDateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }
}
