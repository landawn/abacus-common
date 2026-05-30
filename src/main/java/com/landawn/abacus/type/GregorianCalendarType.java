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

import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link java.util.GregorianCalendar} objects.
 * This class provides serialization, deserialization, and database access capabilities
 * for {@code GregorianCalendar} instances. It extends {@code AbstractCalendarType} to inherit
 * common calendar handling functionality.
 *
 * <p>String representations follow the standard date/time formats supported by the
 * {@code Dates} utility. The special string {@code "sysTime"} resolves to the current
 * system time. Numeric strings are interpreted as milliseconds since the epoch.
 * Database columns are read and written as {@link java.sql.Timestamp} values.
 */
@SuppressWarnings("java:S2160")
public class GregorianCalendarType extends AbstractCalendarType<GregorianCalendar> {

    /**
     * The type name constant for GregorianCalendar type identification.
     */
    public static final String GREGORIAN_CALENDAR = GregorianCalendar.class.getSimpleName();

    /**
     * Package-private constructor for GregorianCalendarType.
     * This constructor is called by the TypeFactory to create GregorianCalendar type instances.
     */
    GregorianCalendarType() {
        super(GREGORIAN_CALENDAR);
    }

    /**
     * Returns the Class object representing the {@code GregorianCalendar} type.
     *
     * @return {@code GregorianCalendar.class}
     */
    @Override
    public Class<GregorianCalendar> javaType() {
        return GregorianCalendar.class;
    }

    /**
     * Converts various object types to a {@code GregorianCalendar} instance.
     * Supported input types include:
     * <ul>
     *   <li>{@link Number}: interpreted as milliseconds since the epoch</li>
     *   <li>{@link java.util.Date}: converted directly to {@code GregorianCalendar}</li>
     *   <li>{@link java.util.Calendar}: converted to {@code GregorianCalendar} preserving the time</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Other types: converted to their string representation and then parsed</li>
     * </ul>
     *
     * @param obj the object to convert to {@code GregorianCalendar}; may be {@code null}
     * @return a {@code GregorianCalendar} instance, or {@code null} if {@code obj} is {@code null}
     */
    @Override
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
     * Parses a string representation into a {@code GregorianCalendar} instance.
     * The method handles:
     * <ul>
     *   <li>{@code null} or empty strings: returns {@code null}</li>
     *   <li>{@code "sysTime"}: returns current time as {@code GregorianCalendar}</li>
     *   <li>Numeric strings: interpreted as milliseconds since the epoch</li>
     *   <li>Date/time strings: parsed according to standard date formats</li>
     * </ul>
     *
     * @param str the string to parse into a {@code GregorianCalendar}; may be {@code null} or empty
     * @return the parsed {@code GregorianCalendar} instance, or {@code null} if {@code str} is {@code null} or empty
     */
    @Override
    public GregorianCalendar valueOf(final String str) {
        return isNullDateTime(str) ? null : (isSysTime(str) ? Dates.currentGregorianCalendar() : Dates.parseGregorianCalendar(str));
    }

    /**
     * Parses a character array into a {@code GregorianCalendar} instance.
     * This method is optimized for performance when parsing from character buffers.
     * If the character sequence appears to be a {@code long} number, it is interpreted as
     * milliseconds since the epoch. Otherwise, the characters are converted to a string and
     * parsed using standard date parsing.
     *
     * @param cbuf the character array containing the date/time representation; may be {@code null}
     * @param offset the start offset in the character array
     * @param len the number of characters to parse
     * @return the parsed {@code GregorianCalendar} instance, or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     */
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
     * Retrieves a {@code GregorianCalendar} value from the specified column in a {@link ResultSet}.
     * The column value is read as a {@link java.sql.Timestamp} and converted to a
     * {@code GregorianCalendar}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to read
     * @return the {@code GregorianCalendar} value from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public GregorianCalendar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);
        return ts == null ? null : asGregorianCalendar(ts);
    }

    /**
     * Retrieves a {@code GregorianCalendar} value from the specified column in a {@link ResultSet}
     * using the column label. The column value is read as a {@link java.sql.Timestamp} and
     * converted to a {@code GregorianCalendar}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to read
     * @return the {@code GregorianCalendar} value from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public GregorianCalendar get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);
        return ts == null ? null : asGregorianCalendar(ts);
    }

    /**
     * Converts a {@link java.sql.Timestamp} to a {@link GregorianCalendar} instance.
     *
     * @param value the {@link java.sql.Timestamp} to convert; may be {@code null}
     * @return a new {@link GregorianCalendar} set to the timestamp's time,
     *         or {@code null} if {@code value} is {@code null}
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
