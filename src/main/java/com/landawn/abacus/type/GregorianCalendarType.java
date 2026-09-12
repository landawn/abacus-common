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

/**
 * Type handler for {@link java.util.GregorianCalendar} objects.
 * This class provides serialization, deserialization, and database access capabilities
 * for {@code GregorianCalendar} instances. It extends {@code AbstractCalendarType} to inherit
 * common calendar handling functionality.
 *
 * <p>String representations follow the standard date/time formats supported by the
 * {@code Dates} utility. The special strings {@code "sysTime"} and {@code "SYS_TIME"} (case-insensitive) resolve to the current
 * system time. Numeric strings are interpreted as milliseconds since the epoch.
 * Database columns are read and written as {@link java.sql.Timestamp} values.
 */
@SuppressWarnings("java:S2160")
public class GregorianCalendarType extends AbstractCalendarType<GregorianCalendar> {

    /**
     * The type name constant for GregorianCalendar type identification, equal to {@code "GregorianCalendar"}.
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
     * @throws IllegalArgumentException if the non-null value is not a supported date/time representation, or a non-lenient calendar contains invalid fields.
     */
    @Override
    public GregorianCalendar valueOf(final Object obj) throws IllegalArgumentException {
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
     *   <li>{@code null}, empty, or the literal {@code "null"} strings: returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive): returns current time as {@code GregorianCalendar}</li>
     *   <li>Numeric strings: interpreted as milliseconds since the epoch</li>
     *   <li>Date/time strings: parsed according to standard date formats</li>
     * </ul>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse into a {@code GregorianCalendar}; may be {@code null} or empty
     * @return the parsed {@code GregorianCalendar} instance, or {@code null} if {@code str} is {@code null}, empty, or the literal {@code "null"}
     * @throws IllegalArgumentException if a nonempty value other than a recognized null or system-time token cannot be parsed as a supported
     *         date/time or epoch-millisecond representation.
     * @see #valueOf(Object)
     * @see #stringOf(java.util.Calendar)
     */
    @MayReturnNull
    @Override
    public GregorianCalendar valueOf(final String str) throws IllegalArgumentException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return Dates.currentGregorianCalendar();
        }

        if (isPossibleMillis(str)) {
            try {
                return Dates.createGregorianCalendar(Long.parseLong(str));
            } catch (final NumberFormatException e) {
                // not a pure long after all; fall through to formatted parsing
            }
        }

        return Dates.parseToGregorianCalendar(str);
    }

    /**
     * Parses a character array into a {@code GregorianCalendar} instance.
     * This method is optimized for performance when parsing from character buffers.
     * If the character sequence appears to be a {@code long} number (digits ending in a digit, so a trailing
     * {@code L}/{@code d}/{@code f} type suffix is not accepted), it is interpreted as milliseconds since the epoch.
     * Otherwise, the characters are converted to a string and parsed by {@link #valueOf(String)}, so both overloads
     * give the same answer for the same text.
     *
     * @param cbuf the character array containing the date/time representation; may be {@code null}
     * @param offset the start offset in the character array
     * @param len the number of characters to parse
     * @return the parsed {@code GregorianCalendar} instance, or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws IllegalArgumentException if the text is not a recognized date-time or numeric form (see         {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public GregorianCalendar valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, IllegalArgumentException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // isPossibleMillis also requires the last char to be a digit: parseLong(char[]) tolerates a trailing
        // l/L/f/F/d/D, which the String overload rejects, and an overflow (> 18 digits) surfaces as
        // ArithmeticException - both fall through to valueOf(String) so that the two overloads report the same
        // IllegalArgumentException.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return Dates.createGregorianCalendar(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException | ArithmeticException e) {
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
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public GregorianCalendar get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);
        return ts == null ? null : Dates.createGregorianCalendar(ts);
    }

    /**
     * Retrieves a {@code GregorianCalendar} value from the specified column in a {@link ResultSet}
     * using the column label. The column value is read as a {@link java.sql.Timestamp} and
     * converted to a {@code GregorianCalendar}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to read
     * @return the {@code GregorianCalendar} value from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public GregorianCalendar get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);
        return ts == null ? null : Dates.createGregorianCalendar(ts);
    }
}
