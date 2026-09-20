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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link Calendar} values.
 * This class provides serialization, deserialization, and database operations for {@link Calendar} objects.
 * Calendar values are stored in and retrieved from the database as SQL {@code TIMESTAMP} values.
 *
 * <p>Supported conversions include:</p>
 * <ul>
 *   <li>{@link Number}: interpreted as milliseconds since the Unix epoch</li>
 *   <li>{@link java.util.Date}: converted to a {@link Calendar} with the same instant</li>
 *   <li>{@link Calendar}: rebuilt as a new {@link java.util.GregorianCalendar} at the same instant and time zone
 *       (the calendar system, leniency and week rules of the source are not copied)</li>
 *   <li>{@link String}: parsed as a date-time string, or {@code "sysTime"}/{@code "SYS_TIME"} for the current time</li>
 * </ul>
 *
 * @see AbstractCalendarType
 * @see java.util.Calendar
 */
public class CalendarType extends AbstractCalendarType<Calendar> {

    /** The type name constant for Calendar type identification, equal to {@code "Calendar"}. */
    public static final String CALENDAR = Calendar.class.getSimpleName();

    /**
     * Package-private constructor for {@code CalendarType}.
     * Instances are created by the {@code TypeFactory}.
     */
    CalendarType() {
        super(CALENDAR);
    }

    /**
     * Package-private constructor for {@code CalendarType} with a custom type name.
     * Used by subclasses that extend this type with a specialized name.
     *
     * @param typeName the custom type name to register
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    CalendarType(final String typeName) throws IllegalArgumentException {
        super(typeName);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Calendar.class}
     */
    @Override
    public Class<Calendar> javaType() {
        return Calendar.class;
    }

    /**
     * Converts an arbitrary object to a {@link Calendar} instance.
     * The conversion rules are:
     * <ul>
     *   <li>{@link Number}: treated as milliseconds since the Unix epoch</li>
     *   <li>{@link java.util.Date}: the date's instant is used to construct a new {@link Calendar}</li>
     *   <li>{@link Calendar}: rebuilt as a new {@link java.util.GregorianCalendar} at the same instant and time zone
     *       via {@link com.landawn.abacus.util.Dates#createCalendar(Calendar)}; the calendar system (for example a
     *       Buddhist or Japanese calendar), leniency and week rules of the source are not copied</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to its string representation, then parsed as a date-time string</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return a {@link Calendar} representing the input value, or {@code null} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if the non-null value is not a supported date/time representation, or a non-lenient calendar contains invalid fields.
     */
    @Override
    public Calendar valueOf(final Object obj) throws IllegalArgumentException {
        if (obj instanceof Number) {
            return Dates.createCalendar(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return Dates.createCalendar((java.util.Date) obj);
        } else if (obj instanceof Calendar) {
            return Dates.createCalendar((Calendar) obj);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to a {@link Calendar} instance.
     * <ul>
     *   <li>{@code null}, empty, or the literal {@code "null"} string: returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive): returns a {@link Calendar} for the current system time</li>
     *   <li>Purely numeric values (possible epoch milliseconds): converted via the {@code Dates.create*} epoch factory</li>
     *   <li>All other values: parsed by {@link com.landawn.abacus.util.Dates#parseToCalendar(String)}</li>
     * </ul>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return a {@link Calendar} parsed from {@code str}, or {@code null} if {@code str} is {@code null}, empty, or the literal {@code "null"}
     * @throws IllegalArgumentException if a nonempty value other than a recognized null or system-time token cannot be parsed as a supported
     *         date/time or epoch-millisecond representation.
     * @see #valueOf(Object)
     * @see #stringOf(java.util.Calendar)
     */
    @MayReturnNull
    @Override
    public Calendar valueOf(final String str) throws IllegalArgumentException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return Dates.currentCalendar();
        }

        if (isPossibleMillis(str)) {
            try {
                return Dates.createCalendar(Long.parseLong(str));
            } catch (final NumberFormatException e) {
                // not a pure long after all; fall through to formatted parsing
            }
        }

        return Dates.parseToCalendar(str);
    }

    /**
     * Converts a region of a character array to a {@link Calendar} instance.
     * If the character sequence has more than four characters and consists of an optional sign followed only by
     * ASCII decimal digits, it is parsed as epoch milliseconds (no hexadecimal prefix or type suffix); otherwise
     * the characters are converted to a {@link String} and delegated to {@link #valueOf(String)}, so both overloads
     * give the same answer for the same text.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return the parsed calendar value, or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws IllegalArgumentException if the text is not a recognized date-time or numeric form (see {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public Calendar valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, IllegalArgumentException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // Check the entire token for decimal digits and an optional leading sign: parseLong(char[]) also
        // accepts suffixes and some hexadecimal forms. Rejected syntax and numeric overflow fall through
        // to valueOf(String), preserving the String overload's parsing and exception behavior.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return Dates.createCalendar(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a {@link Calendar} value from a {@link ResultSet} at the specified column index.
     * The column is read as a {@link java.sql.Timestamp} and then converted to a {@link Calendar}.
     *
     * @param rs          the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return a {@link Calendar} created from the column's timestamp value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Calendar get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return (ts == null) ? null : Dates.createCalendar(ts);
    }

    /**
     * Retrieves a {@link Calendar} value from a {@link ResultSet} using the specified column label.
     * The column is read as a {@link java.sql.Timestamp} and then converted to a {@link Calendar}.
     *
     * @param rs         the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return a {@link Calendar} created from the column's timestamp value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Calendar get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return (ts == null) ? null : Dates.createCalendar(ts);
    }
}
