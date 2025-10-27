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
import java.util.GregorianCalendar;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for GregorianCalendar objects.
 * This class provides serialization, deserialization, and database access capabilities for GregorianCalendar instances.
 * It extends AbstractCalendarType to inherit common calendar handling functionality.
 */
@SuppressWarnings("java:S2160")
public class GregorianCalendarType extends AbstractCalendarType<GregorianCalendar> {

    public static final String GREGORIAN_CALENDAR = GregorianCalendar.class.getSimpleName();

    GregorianCalendarType() {
        super(GREGORIAN_CALENDAR);
    }

    /**
     * Returns the Class object representing the GregorianCalendar type.
     *
     * @return GregorianCalendar.class
     */
    @Override
    public Class<GregorianCalendar> clazz() {
        return GregorianCalendar.class;
    }

    /**
     * Converts various object types to a GregorianCalendar instance.
     * Supported input types include:
     * - Number: interpreted as milliseconds since epoch
     * - java.util.Date: converted directly to GregorianCalendar
     * - Calendar: converted to GregorianCalendar preserving the time
     * - Other types: converted to string and then parsed
     *
     * @param obj the object to convert to GregorianCalendar
     * @return a GregorianCalendar instance, or {@code null} if the input is null
     */
    @Override
    @MayReturnNull
    public GregorianCalendar valueOf(final Object obj) {
        if (obj instanceof Number) {
            return Dates.createGregorianCalendar(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return Dates.createGregorianCalendar((java.util.Date) obj);
        } else if (obj instanceof Calendar) {
            return Dates.createGregorianCalendar((Calendar) obj);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string representation into a GregorianCalendar instance.
     * The method handles:
     * - {@code null} or empty strings: returns null
     * - "sysTime": returns current time as GregorianCalendar
     * - numeric strings: interpreted as milliseconds since epoch
     * - date/time strings: parsed according to standard date formats
     *
     * @param str the string to parse into a GregorianCalendar
     * @return the parsed GregorianCalendar instance, or {@code null} if the input is {@code null} or empty
     */
    @Override
    @MayReturnNull
    public GregorianCalendar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentGregorianCalendar() : Dates.parseGregorianCalendar(str));
    }

    /**
     * Parses a character array into a GregorianCalendar instance.
     * This method is optimized for performance when parsing from character buffers.
     * If the character sequence appears to be a long number, it's interpreted as milliseconds since epoch.
     * Otherwise, the characters are converted to a string and parsed using standard date parsing.
     *
     * @param cbuf the character array containing the date/time representation
     * @param offset the start offset in the character array
     * @param len the number of characters to parse
     * @return the parsed GregorianCalendar instance, or {@code null} if the input is {@code null} or empty
     */
    @MayReturnNull
    @Override

    public GregorianCalendar valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createGregorianCalendar(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a GregorianCalendar value from the specified column in a ResultSet.
     * The method reads a Timestamp from the database and converts it to a GregorianCalendar.
     * If the column value is {@code null}, returns {@code null}.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the GregorianCalendar value from the column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    @MayReturnNull
    public GregorianCalendar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);
        return ts == null ? null : asGregorianCalendar(ts);
    }

    /**
     * Retrieves a GregorianCalendar value from the specified column in a ResultSet using the column label.
     * The method reads a Timestamp from the database and converts it to a GregorianCalendar.
     * If the column value is {@code null}, returns {@code null}.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to read
     * @return the GregorianCalendar value from the column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    @MayReturnNull
    public GregorianCalendar get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);
        return ts == null ? null : asGregorianCalendar(ts);
    }

    /**
     * Converts a SQL Timestamp to a GregorianCalendar instance.
     * This is a helper method used internally for database value conversions.
     *
     * @param value the Timestamp to convert
     * @return a new GregorianCalendar instance set to the timestamp's time, or {@code null} if the input is null
     */
    private static GregorianCalendar asGregorianCalendar(final Timestamp value) {
        if (value == null) {
            return null; // NOSONAR
        }

        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(value.getTime());

        return gc;
    }
}
