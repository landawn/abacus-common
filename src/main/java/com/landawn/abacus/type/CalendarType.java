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
 *   <li>{@link Calendar}: cloned to a new independent instance</li>
 *   <li>{@link String}: parsed as a date-time string, or {@code "sysTime"} for the current time</li>
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
     */
    CalendarType(final String typeName) {
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
     *   <li>{@link Calendar}: a new independent copy is created via {@link com.landawn.abacus.util.Dates#createCalendar(Calendar)}</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to its string representation, then parsed as a date-time string</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return a {@link Calendar} representing the input value, or {@code null} if {@code obj} is {@code null}
     */
    @Override
    public Calendar valueOf(final Object obj) {
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
     *   <li>{@code "sysTime"}: returns a {@link Calendar} for the current system time</li>
     *   <li>All other values: parsed by {@link com.landawn.abacus.util.Dates#parseCalendar(String)}</li>
     * </ul>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return a {@link Calendar} parsed from {@code str}, or {@code null} if {@code str} is {@code null}, empty, or the literal {@code "null"}
     * @see #valueOf(Object)
     * @see #stringOf(java.util.Calendar)
     */
    @Override
    public Calendar valueOf(final String str) {
        return isNullDateTime(str) ? null : (isSysTime(str) ? Dates.currentCalendar() : Dates.parseCalendar(str));
    }

    /**
     * Converts a region of a character array to a {@link Calendar} instance.
     * If the character sequence looks like a {@code long} value (an epoch-millisecond timestamp),
     * it is parsed as such; otherwise the characters are converted to a {@link String} and
     * delegated to {@link #valueOf(String)}.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return the parsed calendar value
     *         or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     */
    @Override
    public Calendar valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return Dates.createCalendar(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
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
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public Calendar get(final ResultSet rs, final int columnIndex) throws SQLException {
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
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Calendar get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return (ts == null) ? null : Dates.createCalendar(ts);
    }
}
