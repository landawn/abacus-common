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
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Calendar values.
 * This class provides serialization, deserialization, and database operations for Calendar objects.
 * It supports conversion from various formats including timestamps, date strings, and the special "sysTime" value.
 */
public class CalendarType extends AbstractCalendarType<Calendar> {

    public static final String CALENDAR = Calendar.class.getSimpleName();

    CalendarType() {
        super(CALENDAR);
    }

    CalendarType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing Calendar.class
     */
    @Override
    public Class<Calendar> clazz() {
        return Calendar.class;
    }

    /**
     * Converts various object types to a Calendar instance.
     * Supported input types include:
     * - Number: interpreted as milliseconds since epoch
     * - java.util.Date: converted to Calendar
     * - Calendar: cloned to a new instance
     * - String: parsed as a date/time string
     * - Other objects: converted to string first, then parsed
     *
     * @param obj the object to convert to Calendar. Can be {@code null}.
     * @return A Calendar instance representing the input value, or {@code null} if input is null
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
     * Converts a string representation to a Calendar instance.
     * Special handling for:
     * - {@code null} or empty string: returns null
     * - "sysTime": returns the current system time as Calendar
     * - Other strings: parsed using date parsing utilities
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Calendar> type = TypeFactory.getType(Calendar.class);
     * Calendar cal1 = type.valueOf("2023-12-25 10:30:45");
     * // Parses ISO date-time string
     *
     * Calendar cal2 = type.valueOf("sysTime");
     * // Returns current system time
     *
     * Calendar cal3 = type.valueOf(null);
     * // Returns null
     * }</pre>
     *
     * @param str the string to parse. Can be {@code null} or empty.
     * @return A Calendar instance parsed from the string, or {@code null} if input is null/empty
     */
    @Override
    public Calendar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentCalendar() : Dates.parseCalendar(str));
    }

    /**
     * Converts a character array to a Calendar instance.
     * If the character array appears to represent a numeric value (timestamp),
     * it attempts to parse it as milliseconds since epoch.
     * Otherwise, it converts to string and uses string parsing.
     *
     * @param cbuf the character array containing the value to parse
     * @param offset the starting position in the character array
     * @param len the number of characters to use
     * @return A Calendar instance parsed from the character array, or {@code null} if input is {@code null} or empty
     */
    @Override
    public Calendar valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createCalendar(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a Calendar value from a ResultSet at the specified column index.
     * The method reads the value as a Timestamp and converts it to Calendar.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the timestamp value
     * @return A Calendar instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Calendar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return (ts == null) ? null : Dates.createCalendar(ts);
    }

    /**
     * Retrieves a Calendar value from a ResultSet using the specified column label.
     * The method reads the value as a Timestamp and converts it to Calendar.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the timestamp value
     * @return A Calendar instance created from the timestamp, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Calendar get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);

        return (ts == null) ? null : Dates.createCalendar(ts);
    }
}
