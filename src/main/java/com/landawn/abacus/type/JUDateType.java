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

/**
 * Type handler for {@link java.util.Date} objects.
 * This class provides serialization, deserialization, and database access capabilities for
 * {@code java.util.Date} instances. Database columns are read and written as
 * {@link java.sql.Timestamp} values. The special string {@code "sysTime"} resolves to the
 * current system time, and numeric strings are interpreted as milliseconds since the epoch.
 *
 * <p>The type name used in the type system is {@code "JUDate"} (to avoid ambiguity with
 * {@code java.sql.Date}).</p>
 */
@SuppressWarnings({ "java:S1942", "java:S2143", "java:S2160" })
public class JUDateType extends AbstractDateType<Date> {

    public static final String JU_DATE = "JUDate";

    private static final String declaringName = Date.class.getCanonicalName();

    /**
     * Package-private constructor for JUDateType.
     * This constructor is called by the TypeFactory to create java.util.Date type instances.
     */
    JUDateType() {
        super(JU_DATE);
    }

    /**
     * Package-private constructor for JUDateType with a custom type name.
     * Used by subclasses or factory methods that register this handler under a different name.
     *
     * @param typeName the type name to use for registration
     */
    JUDateType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the canonical class name of {@link java.util.Date}, used for type declarations.
     *
     * @return {@code "java.util.Date"}
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code java.util.Date.class}
     */
    @Override
    public Class<Date> javaType() {
        return Date.class;
    }

    /**
     * Converts an arbitrary object to a {@link java.util.Date} instance.
     * Supported conversions:
     * <ul>
     *   <li>{@link Number}: treated as milliseconds since the epoch</li>
     *   <li>{@link java.util.Date}: creates a new {@link java.util.Date} instance with the same time value</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to string via {@link N#stringOf(Object)} and then parsed via {@link #valueOf(String)}</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return a {@link java.util.Date} representing the input value, or {@code null} if {@code obj} is {@code null}
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
     * Converts a string representation to a {@link java.util.Date} instance.
     * <ul>
     *   <li>{@code null} or null-datetime strings: returns {@code null}</li>
     *   <li>{@code "sysTime"} (case-insensitive): returns the current system time</li>
     *   <li>All other values: parsed via {@link Dates#parseJUDate(String)}</li>
     * </ul>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return the parsed {@link java.util.Date}, or {@code null} if {@code str} is {@code null} or a null-datetime string
     * @throws IllegalArgumentException if the string format is not recognized
     */
    @Override
    public Date valueOf(final String str) {
        return isNullDateTime(str) ? null : (isSysTime(str) ? Dates.currentJUDate() : Dates.parseJUDate(str));
    }

    /**
     * Converts a region of a character array to a {@link java.util.Date} instance.
     * If the character sequence looks like a {@code long} value (an epoch-millisecond timestamp),
     * it is parsed as such; otherwise the characters are converted to a {@link String} and
     * delegated to {@link #valueOf(String)}.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return a {@link java.util.Date} parsed from the specified character region,
     *         or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
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
     * Retrieves a {@link java.util.Date} value from the specified column in a {@link ResultSet}.
     * The column is read as a {@link java.sql.Timestamp} and converted via {@code new Date(ts.getTime())}.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return a {@link java.util.Date} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Date get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return (ts == null) ? null : new Date(ts.getTime());
    }

    /**
     * Retrieves a {@link java.util.Date} value from the specified column in a {@link ResultSet} using the column label.
     * The column is read as a {@link java.sql.Timestamp} and converted via {@code new Date(ts.getTime())}.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return a {@link java.util.Date} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Date get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return (ts == null) ? null : new Date(ts.getTime());
    }

    /**
     * Sets a {@link java.util.Date} value as a parameter in a {@link PreparedStatement}.
     * The value is converted to a {@link java.sql.Timestamp}; if it is already a
     * {@link java.sql.Timestamp} it is used directly without copying.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the {@link java.util.Date} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Date x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));
    }

    /**
     * Sets a {@link java.util.Date} value as a named parameter in a {@link CallableStatement}.
     * The value is converted to a {@link java.sql.Timestamp}; if it is already a
     * {@link java.sql.Timestamp} it is used directly without copying.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@link java.util.Date} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Date x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));
    }
}
