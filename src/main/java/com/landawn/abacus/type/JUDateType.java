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
import java.util.Date;

import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@SuppressWarnings({ "java:S1942", "java:S2143", "java:S2160" })
public class JUDateType extends AbstractDateType<Date> {

    public static final String JU_DATE = "JUDate";

    private static final String declaringName = Date.class.getCanonicalName();

    JUDateType() {
        super(JU_DATE);
    }

    JUDateType(final String typeName) {
        super(typeName);
    }

    /**
     * Gets the declaring name of this type.
     * This method returns the canonical class name for java.util.Date, which is used
     * for type identification in serialization and metadata operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     * String name = type.declaringName();
     * System.out.println(name); // Outputs: java.util.Date
     * }</pre>
     *
     * @return the canonical name of java.util.Date class
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Gets the class type for java.util.Date.
     * This method returns the Class object representing java.util.Date, which is used
     * for type identification and reflection operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     * Class<Date> clazz = type.clazz();
     * System.out.println(clazz.getName()); // Outputs: java.util.Date
     * }</pre>
     *
     * @return the Class object representing java.util.Date
     */
    @Override
    public Class<Date> clazz() {
        return Date.class;
    }

    /**
     * Converts the specified object to a java.util.Date instance.
     * This method provides flexible conversion from various types, supporting Numbers,
     * existing Date objects, and string representations.
     *
     * This method handles the following conversions:
     * - Number: treated as milliseconds since epoch and converted to Date
     * - Date: creates a new Date instance with the same time value
     * - String: parsed using the valueOf(String) method
     * - null: returns null
     * - Other types: converted to string first, then parsed
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     *
     * // From Number (milliseconds since epoch)
     * Date date1 = type.valueOf(1609459200000L);
     *
     * // From existing Date
     * Date existing = new Date();
     * Date date2 = type.valueOf(existing); // Creates a new instance
     *
     * // From String
     * Date date3 = type.valueOf("2021-01-01");
     * }</pre>
     *
     * @param obj the object to convert to Date
     * @return a Date instance, or {@code null} if the input is null
     */
    @Override
    public Date valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new Date(((Number) obj).longValue());
        } else if (obj instanceof Date) {
            return new Date(((Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string representation into a java.util.Date instance.
     * This method supports multiple date formats including ISO 8601 and common date patterns,
     * delegating to the Dates utility for flexible parsing.
     *
     * This method handles the following string formats:
     * - Empty/null string: returns null
     * - "SYS_TIME": returns current system time as Date
     * - Other formats: parsed using the Dates.parseJUDate utility method
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     *
     * // Parse ISO 8601 date
     * Date date1 = type.valueOf("2021-01-01");
     *
     * // Get current system time
     * Date date2 = type.valueOf("SYS_TIME");
     *
     * // Parse common date formats (supported by Dates utility)
     * Date date3 = type.valueOf("01/01/2021");
     * Date date4 = type.valueOf("2021-01-01 10:30:00");
     * }</pre>
     *
     * @param str the string to parse
     * @return a Date instance, or {@code null} if the string is empty or null
     * @throws IllegalArgumentException if the string format is invalid
     */
    @Override
    public Date valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentJUDate() : Dates.parseJUDate(str));
    }

    /**
     * Parses a character array into a java.util.Date instance.
     * This method provides efficient parsing from character arrays, attempting numeric parsing first
     * for performance optimization.
     *
     * This method first attempts to parse the character array as a long value (milliseconds since epoch).
     * If that fails, it converts the character array to a string and delegates to valueOf(String).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     *
     * // Parse from character array containing milliseconds
     * char[] millisChars = "1609459200000".toCharArray();
     * Date date1 = type.valueOf(millisChars, 0, millisChars.length);
     *
     * // Parse from character array containing date string
     * char[] dateChars = "2021-01-01".toCharArray();
     * Date date2 = type.valueOf(dateChars, 0, dateChars.length);
     * }</pre>
     *
     * @param cbuf the character buffer containing the value to parse
     * @param offset the start offset in the character buffer
     * @param len the number of characters to parse
     * @return a Date instance, or {@code null} if the character buffer is {@code null} or length is 0
     */
    @Override
    public Date valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createJUDate(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a Date value from the specified column in a ResultSet.
     * This method provides database-to-Java type conversion for java.util.Date objects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         Date date = type.get(rs, 1);
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a Date instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Date get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return (ts == null) ? null : new Date(ts.getTime());
    }

    /**
     * Retrieves a Date value from the specified column in a ResultSet using column label.
     * This method provides database-to-Java type conversion for java.util.Date objects by column name.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         Date date = type.get(rs, "created_at");
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label to retrieve the value from
     * @return a Date instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public Date get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);

        return (ts == null) ? null : new Date(ts.getTime());
    }

    /**
     * Sets a Date parameter in a PreparedStatement.
     * This method provides Java-to-database type conversion for java.util.Date objects,
     * efficiently handling both regular Date and Timestamp instances.
     *
     * This method converts the java.util.Date to a SQL Timestamp before setting it in the statement.
     * If the Date is already a Timestamp instance, it is used directly.
     * If the Date is {@code null}, a SQL NULL is set for the parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     * Date date = new Date();
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (timestamp) VALUES (?)")) {
     *     type.set(stmt, 1, date);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Date value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Date x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));
    }

    /**
     * Sets a named Date parameter in a CallableStatement.
     * This method provides Java-to-database type conversion for stored procedure calls
     * using named parameters, efficiently handling both regular Date and Timestamp instances.
     *
     * This method converts the java.util.Date to a SQL Timestamp before setting it in the statement.
     * If the Date is already a Timestamp instance, it is used directly.
     * If the Date is {@code null}, a SQL NULL is set for the parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.util.Date> type = TypeFactory.getType(java.util.Date.class);
     * Date date = new Date();
     * try (CallableStatement stmt = conn.prepareCall("{call log_event(?)}")) {
     *     type.set(stmt, "event_time", date);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Date value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Date x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));
    }
}
