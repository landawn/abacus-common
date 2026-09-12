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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link java.sql.Date} values.
 * This class provides serialization, deserialization, and database operations for SQL Date objects.
 * SQL {@link java.sql.Date} represents a date without a time component (year, month, and day only).
 *
 * <p>Supported conversions include:
 * <ul>
 *   <li>{@link Number}: interpreted as milliseconds since the Unix epoch</li>
 *   <li>{@link java.util.Date}: the time instant is used to construct a new SQL Date</li>
 *   <li>{@link String}: parsed as a date string, or the special value {@code "sysTime"} (or {@code "SYS_TIME"}) for today's date</li>
 *   <li>{@code char[]}: parsed as a timestamp (milliseconds) or date string</li>
 * </ul>
 *
 * @see AbstractDateType
 * @see java.sql.Date
 */
public class DateType extends AbstractDateType<Date> {

    /**
     * The type name constant for Date type identification, equal to {@code "Date"}.
     */
    public static final String DATE = Date.class.getSimpleName();

    /**
     * Package-private constructor for {@code DateType}.
     * Instances are created by the {@code TypeFactory}.
     */
    DateType() {
        super(DATE);
    }

    /**
     * Package-private constructor for {@code DateType} with a custom type name.
     * Used by subclasses that extend this type with a specialized name.
     *
     * @param typeName the custom type name to register
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    DateType(final String typeName) throws IllegalArgumentException {
        super(typeName);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code java.sql.Date.class}
     */
    @Override
    public Class<Date> javaType() {
        return Date.class;
    }

    /**
     * Converts an arbitrary object to a {@link java.sql.Date} instance.
     * The conversion rules are:
     * <ul>
     *   <li>{@link Number}: treated as milliseconds since the Unix epoch</li>
     *   <li>{@link java.util.Date}: the time instant is used to construct a new {@link java.sql.Date}</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to its string representation, then parsed via {@link #valueOf(String)}</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return a {@link java.sql.Date} representing the input value, or {@code null} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if the non-null value is not a supported date/time representation, or a non-lenient calendar contains invalid fields.
     */
    @Override
    public Date valueOf(final Object obj) throws IllegalArgumentException {
        if (obj instanceof Number) {
            return new Date(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new Date(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to a {@link java.sql.Date} instance.
     * <ul>
     *   <li>{@code null}, empty string, or the literal {@code "null"} (case-insensitive): returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive): returns today's date (via {@link com.landawn.abacus.util.Dates#currentDate()})</li>
     *   <li>Purely numeric values (possible epoch milliseconds): converted via the {@code Dates.create*} epoch factory</li>
     *   <li>All other values: parsed by {@link com.landawn.abacus.util.Dates#parseToDate(String)}</li>
     * </ul>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return a {@link java.sql.Date} parsed from {@code str}, or {@code null} if {@code str} is {@code null}, empty, or the literal {@code "null"}
     * @throws IllegalArgumentException if a nonempty value other than a recognized null or system-time token cannot be parsed as a supported
     *         date/time or epoch-millisecond representation.
     * @see #valueOf(Object)
     * @see #stringOf(java.util.Date)
     */
    @MayReturnNull
    @Override
    public Date valueOf(final String str) throws IllegalArgumentException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return Dates.currentDate();
        }

        if (isPossibleMillis(str)) {
            try {
                return Dates.createDate(Long.parseLong(str));
            } catch (final NumberFormatException e) {
                // not a pure long after all; fall through to formatted parsing
            }
        }

        return Dates.parseToDate(str);
    }

    /**
     * Converts a region of a character array to a {@link java.sql.Date} instance.
     * If the character sequence looks like a {@code long} value (an epoch-millisecond timestamp: digits ending in a
     * digit, so a trailing {@code L}/{@code d}/{@code f} type suffix is not accepted), it is parsed as such; otherwise
     * the characters are converted to a {@link String} and delegated to {@link #valueOf(String)}, so both overloads
     * give the same answer for the same text.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return the parsed SQL date value, or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws IllegalArgumentException if the text is not a recognized date-time or numeric form (see         {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public Date valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, IllegalArgumentException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // isPossibleMillis also requires the last char to be a digit: parseLong(char[]) tolerates a trailing
        // l/L/f/F/d/D, which the String overload rejects, and an overflow (> 18 digits) surfaces as
        // ArithmeticException - both fall through to valueOf(String) so that the two overloads report the same
        // IllegalArgumentException.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return Dates.createDate(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a {@link java.sql.Date} value from a {@link ResultSet} at the specified column index.
     *
     * @param rs          the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return the {@link java.sql.Date} at the specified column,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Date get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getDate(columnIndex);
    }

    /**
     * Retrieves a {@link java.sql.Date} value from a {@link ResultSet} using the specified column label.
     *
     * @param rs         the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return the {@link java.sql.Date} in the specified column,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Date get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getDate(columnName);
    }

    /**
     * Sets a {@link java.sql.Date} value as a parameter in a {@link PreparedStatement}.
     *
     * @param stmt        the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the {@link java.sql.Date} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Date x) throws NullPointerException, SQLException {
        stmt.setDate(columnIndex, x);
    }

    /**
     * Sets a {@link java.sql.Date} value as a named parameter in a {@link CallableStatement}.
     *
     * @param stmt          the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the {@link java.sql.Date} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Date x) throws NullPointerException, SQLException {
        stmt.setDate(parameterName, x);
    }
}
