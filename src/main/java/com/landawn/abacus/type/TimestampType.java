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

import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.sql.Timestamp} objects.
 * This class provides methods to convert between Timestamp objects and their string representations,
 * as well as methods to interact with JDBC result sets and statements.
 */
class TimestampType extends AbstractDateType<Timestamp> {

    /**
     * The type name constant for Timestamp type.
     */
    public static final String TIMESTAMP = Timestamp.class.getSimpleName();

    TimestampType() {
        super(TIMESTAMP);
    }

    TimestampType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the Java class that this type handler manages.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Timestamp> type = TypeFactory.getType(Timestamp.class);
     * Class<Timestamp> clazz = type.clazz(); // Returns Timestamp.class
     * }</pre>
     *
     * @return {@code Timestamp.class}
     */
    @Override
    public Class<Timestamp> clazz() {
        return Timestamp.class;
    }

    /**
     * Converts the given object to a Timestamp.
     * <p>
     * Conversion rules:
     * <ul>
     *   <li>If obj is a Number, creates a Timestamp using the long value as milliseconds since epoch</li>
     *   <li>If obj is a java.util.Date, creates a Timestamp from the date's time value</li>
     *   <li>If obj is {@code null}, returns null</li>
     *   <li>Otherwise, converts obj to string and parses it as a Timestamp</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Timestamp> type = TypeFactory.getType(Timestamp.class);
     * Timestamp ts1 = type.valueOf(1609459200000L); // From milliseconds
     * Timestamp ts2 = type.valueOf(new Date()); // From Date
     * Timestamp ts3 = type.valueOf("2021-01-01 00:00:00"); // From String
     * }</pre>
     *
     * @param obj the object to convert to Timestamp
     * @return a Timestamp representation of the object, or {@code null} if obj is null
     */
    @Override
    public Timestamp valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new Timestamp(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses the given string into a Timestamp.
     * <p>
     * Special handling:
     * <ul>
     *   <li>If str is {@code null} or empty, returns null</li>
     *   <li>If str equals "SYS_TIME", returns the current timestamp</li>
     *   <li>Otherwise, parses the string using the configured date format</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Timestamp> type = TypeFactory.getType(Timestamp.class);
     * Timestamp ts1 = type.valueOf("2021-01-01 12:30:45");
     * Timestamp ts2 = type.valueOf("SYS_TIME"); // Returns current timestamp
     * Timestamp ts3 = type.valueOf(null); // Returns null
     * }</pre>
     *
     * @param str the string to parse
     * @return a Timestamp parsed from the string, or {@code null} if str is empty
     */
    @Override
    public Timestamp valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentTimestamp() : Dates.parseTimestamp(str));
    }

    /**
     * Parses a character array into a Timestamp.
     * <p>
     * This method first attempts to parse the character array as a long value representing
     * milliseconds since epoch. If that fails, it converts the character array to a string
     * and parses it using the string parsing logic.
     *
     * @param cbuf the character array containing the timestamp representation
     * @param offset the starting position in the character array
     * @param len the number of characters to parse
     * @return a Timestamp parsed from the character array, or {@code null} if the array is {@code null} or length is 0
     */
    @Override
    public Timestamp valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createTimestamp(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a Timestamp value from the specified column in the ResultSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Timestamp> type = TypeFactory.getType(Timestamp.class);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         Timestamp createdAt = type.get(rs, 1);
     *         System.out.println("Created at: " + createdAt);
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the Timestamp value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Timestamp get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getTimestamp(columnIndex);
    }

    /**
     * Retrieves a Timestamp value from the specified column in the ResultSet.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label/name
     * @return the Timestamp value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public Timestamp get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getTimestamp(columnLabel);
    }

    /**
     * Sets a Timestamp parameter in the PreparedStatement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Timestamp> type = TypeFactory.getType(Timestamp.class);
     * Timestamp now = new Timestamp(System.currentTimeMillis());
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (created_at) VALUES (?)")) {
     *     type.set(stmt, 1, now);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Timestamp value to set, may be null
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Timestamp x) throws SQLException {
        stmt.setTimestamp(columnIndex, x);
    }

    /**
     * Sets a named Timestamp parameter in the CallableStatement.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the Timestamp value to set, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Timestamp x) throws SQLException {
        stmt.setTimestamp(parameterName, x);
    }
}
