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

import org.joda.time.MutableDateTime;

import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for Joda-Time MutableDateTime objects.
 * <p>
 * This class provides serialization, deserialization, and database access capabilities for
 * {@code org.joda.time.MutableDateTime} instances. MutableDateTime is a mutable version of DateTime
 * that allows modification after creation. It supports multiple string formats including
 * ISO 8601 formats and provides database conversion using {@code java.sql.Timestamp}.
 *
 * @see org.joda.time.MutableDateTime
 * @see AbstractJodaDateTimeType
 */
public class JodaMutableDateTimeType extends AbstractJodaDateTimeType<MutableDateTime> {

    /** The type name constant for Joda-Time {@link MutableDateTime} type identification. */
    public static final String JODA_MUTABLE_DATE_TIME = "JodaMutableDateTime";

    /**
     * Package-private constructor for JodaMutableDateTimeType.
     * This constructor is called by the TypeFactory to create Joda MutableDateTime type instances.
     */
    JodaMutableDateTimeType() {
        super(JODA_MUTABLE_DATE_TIME);
    }

    /**
     * Package-private constructor for JodaMutableDateTimeType with a custom type name.
     * Used by subclasses or factory methods that register this handler under a different name.
     *
     * @param typeName the type name to use for registration
     */
    JodaMutableDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code org.joda.time.MutableDateTime.class}
     */
    @Override
    public Class<MutableDateTime> javaType() {
        return MutableDateTime.class;
    }

    /**
     * Converts an arbitrary object to a Joda {@link MutableDateTime} instance.
     * Supported conversions:
     * <ul>
     *   <li>{@link Number}: treated as milliseconds since the epoch</li>
     *   <li>{@link java.util.Date}: converted using the date's time in milliseconds</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to string via {@link N#stringOf(Object)} and then parsed via {@link #valueOf(String)}</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return a Joda {@link MutableDateTime} representing the input value, or {@code null} if {@code obj} is {@code null}
     */
    @Override
    public MutableDateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new MutableDateTime(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new MutableDateTime(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to a Joda {@link MutableDateTime} instance.
     * <ul>
     *   <li>{@code null} or null-datetime strings: returns {@code null}</li>
     *   <li>{@code "sysTime"} (case-insensitive): returns the current system time</li>
     *   <li>Numeric strings: parsed as milliseconds since the epoch</li>
     *   <li>20-character strings: parsed as ISO-8601 date-time ({@code "yyyy-MM-dd'T'HH:mm:ss'Z'"})</li>
     *   <li>24-character strings: parsed as ISO-8601 timestamp ({@code "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"})</li>
     *   <li>All other values: parsed as a timestamp via the default timestamp parser</li>
     * </ul>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return the parsed Joda {@link MutableDateTime}, or {@code null} if {@code str} is {@code null} or a null-datetime string
     * @throws IllegalArgumentException if the string format is not recognized
     */
    @Override
    public MutableDateTime valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return new MutableDateTime(System.currentTimeMillis());
        }

        if (isPossibleMillis(str)) {
            try {
                return new MutableDateTime(Numbers.toLong(str));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return str.length() == 20 ? jodaISO8601DateTimeFT.parseMutableDateTime(str)
                : (str.length() == 24 ? jodaISO8601TimestampFT.parseMutableDateTime(str) : new MutableDateTime(Dates.parseTimestamp(str).getTime()));
    }

    /**
     * Converts a region of a character array to a Joda {@link MutableDateTime} instance.
     * If the character sequence looks like a {@code long} value (an epoch-millisecond timestamp),
     * it is parsed as such; otherwise the characters are converted to a {@link String} and
     * delegated to {@link #valueOf(String)}.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return a Joda {@link MutableDateTime} parsed from the specified character region,
     *         or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     */
    @Override
    public MutableDateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return new MutableDateTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a Joda {@link MutableDateTime} value from the specified column in a {@link ResultSet}.
     * The column is read as a {@link java.sql.Timestamp} and converted via {@code new MutableDateTime(ts.getTime())}.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return a Joda {@link MutableDateTime} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public MutableDateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : new MutableDateTime(ts.getTime());
    }

    /**
     * Retrieves a Joda {@link MutableDateTime} value from the specified column in a {@link ResultSet} using the column label.
     * The column is read as a {@link java.sql.Timestamp} and converted via {@code new MutableDateTime(ts.getTime())}.
     *
     * @param rs the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return a Joda {@link MutableDateTime} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public MutableDateTime get(final ResultSet rs, final String columnName) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : new MutableDateTime(ts.getTime());
    }

    /**
     * Sets a Joda {@link MutableDateTime} value as a parameter in a {@link PreparedStatement}.
     * The {@link MutableDateTime} is converted to a {@link java.sql.Timestamp}.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x the Joda {@link MutableDateTime} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableDateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Sets a Joda {@link MutableDateTime} value as a named parameter in a {@link CallableStatement}.
     * The {@link MutableDateTime} is converted to a {@link java.sql.Timestamp}.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the Joda {@link MutableDateTime} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableDateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }
}
