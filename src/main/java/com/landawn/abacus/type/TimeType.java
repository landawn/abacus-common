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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link java.sql.Time} objects. This class provides serialization,
 * deserialization, and database operations for Time instances. It supports parsing
 * time values from various string formats and millisecond values.
 */
public class TimeType extends AbstractDateType<Time> {

    /**
     * The type name identifier for Time type, equal to the simple class name {@code "Time"}.
     */
    public static final String TIME = Time.class.getSimpleName();

    /**
     * Constructs a TimeType instance with the default type name.
     * This constructor is package-private and should only be called by TypeFactory.
     */
    TimeType() {
        super(TIME);
    }

    /**
     * Constructs a TimeType instance with the specified type name.
     * This constructor is package-private and should only be called by TypeFactory or subclasses.
     *
     * @param typeName the name to use for this type
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    TimeType(final String typeName) throws IllegalArgumentException {
        super(typeName);
    }

    /**
     * Returns the Class object representing the Time type.
     *
     * @return the Class object for java.sql.Time
     */
    @Override
    public Class<Time> javaType() {
        return Time.class;
    }

    /**
     * Converts an object to a Time.
     * Supports conversion from Number (as milliseconds), java.util.Date, and String types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * Time time1 = type.valueOf(1609459200000L);        // From milliseconds
     * Time time2 = type.valueOf(new java.util.Date());  // From Date
     * Time time3 = type.valueOf("12:30:45");            // From String
     * }</pre>
     *
     * @param obj the object to convert
     * @return a Time object, or {@code null} if {@code obj} is {@code null}
     *         or is a value whose string form {@link #valueOf(String)} maps to {@code null}
     * @throws IllegalArgumentException if the non-null value is not a supported date/time representation, or a non-lenient calendar contains invalid fields.
     */
    @Override
    public Time valueOf(final Object obj) throws IllegalArgumentException {
        if (obj instanceof Number) {
            return new Time(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new Time(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Creates a Time from its string representation.
     * Supports parsing various time formats and the special value {@code "SYS_TIME"} (case-insensitive,
     * also accepted as {@code "sysTime"}) for the current system time.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * Time time1 = type.valueOf("12:30:45");
     * Time time2 = type.valueOf("SYS_TIME");   // Returns current system time
     * Time time3 = type.valueOf(null);         // Returns null
     * }</pre>
     *
     * <p>This method accepts the millisecond-precision UTC representation produced by {@code stringOf} and
     * delegates its formatted parsing to {@link Dates#parseToTime(String)}. Consequently, within the Common Era
     * year range supported by the default format, {@code valueOf(stringOf(value)).getTime() == value.getTime()}
     * for every non-null {@code Time}. Purely numeric input (possible epoch milliseconds) is converted via
     * {@code Dates.createTime(long)}.</p>
     *
     * @param str the string to parse
     * @return a Time object, or {@code null} if {@code str} is {@code null}, empty, or the literal {@code "null"}
     * @throws IllegalArgumentException if a nonempty value other than a recognized null or system-time token cannot be parsed as a supported
     *         date/time or epoch-millisecond representation.
     * @see #valueOf(Object)
     * @see #stringOf(java.util.Date)
     */
    @MayReturnNull
    @Override
    public Time valueOf(final String str) throws IllegalArgumentException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return Dates.currentTime();
        }

        if (isPossibleMillis(str)) {
            try {
                return Dates.createTime(Long.parseLong(str));
            } catch (final NumberFormatException e) {
                // not a pure long after all; fall through to formatted parsing
            }
        }

        return Dates.parseToTime(str);
    }

    /**
     * Creates a Time from a character array.
     * First parses epoch milliseconds when the text has more than four characters and consists of an optional sign
     * followed only by ASCII decimal digits (no hexadecimal prefix or type suffix); otherwise delegates to
     * {@link #valueOf(String)}, so both overloads give the same answer for the same text.
     *
     * @param cbuf the character buffer containing the value
     * @param offset the start offset in the character buffer
     * @param len the number of characters to use
     * @return a Time object, or {@code null} if the input is {@code null} or empty
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws IllegalArgumentException if the text is not a recognized time or numeric form (see {@link #valueOf(String)}),
     *         including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public Time valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, IllegalArgumentException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // Check the entire token for decimal digits and an optional leading sign: parseLong(char[]) also
        // accepts suffixes and some hexadecimal forms. Rejected syntax and numeric overflow fall through
        // to valueOf(String), preserving the String overload's parsing and exception behavior.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return Dates.createTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a Time value from the specified column in the ResultSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * try (ResultSet rs = stmt.executeQuery("SELECT start_time FROM events")) {
     *     if (rs.next()) {
     *         Time startTime = type.get(rs, 1);
     *         System.out.println("Start time: " + startTime);
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet containing the query results
     * @param columnIndex the index of the column to retrieve (1-based)
     * @return a Time object, or {@code null} if the database value is null
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Time get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getTime(columnIndex);
    }

    /**
     * Retrieves a Time value from the specified column in the ResultSet using the column label.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * try (ResultSet rs = stmt.executeQuery("SELECT start_time FROM events")) {
     *     if (rs.next()) {
     *         Time startTime = type.get(rs, "start_time");
     *         System.out.println("Start time: " + startTime);
     *     }
     * }
     * }</pre>
     *
     * @param rs the ResultSet containing the query results
     * @param columnName the label of the column to retrieve
     * @return a Time object, or {@code null} if the database value is null
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Time get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getTime(columnName);
    }

    /**
     * Sets a Time value at the specified parameter index in the PreparedStatement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * Time startTime = Time.valueOf("09:00:00");
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO events (start_time) VALUES (?)")) {
     *     type.set(stmt, 1, startTime);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Time value to set, may be null
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Time x) throws NullPointerException, SQLException {
        stmt.setTime(columnIndex, x);
    }

    /**
     * Sets a Time value for the specified parameter name in the CallableStatement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * Time startTime = Time.valueOf("09:00:00");
     * try (CallableStatement stmt = conn.prepareCall("{call scheduleEvent(?)}")) {
     *     type.set(stmt, "startTime", startTime);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Time value to set, may be null
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Time x) throws NullPointerException, SQLException {
        stmt.setTime(parameterName, x);
    }
}
