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

import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadableDateTime;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;

/**
 * Type handler for Joda-Time {@link org.joda.time.MutableDateTime} objects.
 * <p>
 * This class provides serialization, deserialization, and database access for
 * {@code MutableDateTime} instances (a mutable counterpart of {@code DateTime}).
 * It supports multiple string formats including ISO-8601 and maps to the database
 * via {@link java.sql.Timestamp}.
 *
 * @see org.joda.time.MutableDateTime
 * @see AbstractJodaDateTimeType
 */
public class JodaMutableDateTimeType extends AbstractJodaDateTimeType<MutableDateTime> {

    /** The type name constant for Joda-Time {@link MutableDateTime} type identification. */
    public static final String JODA_MUTABLE_DATE_TIME = "JodaMutableDateTime";

    /**
     * Package-private constructor for {@code JodaMutableDateTimeType}.
     * Instances are created by the {@code TypeFactory}.
     */
    JodaMutableDateTimeType() {
        super(JODA_MUTABLE_DATE_TIME);
    }

    /**
     * Package-private constructor for JodaMutableDateTimeType with a custom type name.
     * Used by subclasses or factory methods that register this handler under a different name.
     *
     * @param typeName the type name to use for registration
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    JodaMutableDateTimeType(final String typeName) throws IllegalArgumentException {
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
     *   <li>{@link ReadableDateTime} ({@code DateTime}, {@code MutableDateTime}): copied into a new
     *       {@code MutableDateTime} at the same instant with the argument's own time zone and chronology preserved,
     *       so the result is {@code equals} to a {@code MutableDateTime} argument but never the same instance (a Joda
     *       {@code Instant}, which carries no zone, is not covered by this bullet and yields a default-zone result
     *       like the other branches)</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to string via {@link N#stringOf(Object)} and then parsed via {@link #valueOf(String)}</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return a Joda {@link MutableDateTime} representing the input value, or {@code null} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if the non-null value is not a supported date/time representation, or a non-lenient calendar contains invalid fields.
     */
    @Override
    public MutableDateTime valueOf(final Object obj) throws IllegalArgumentException {
        if (obj instanceof Number) {
            return new MutableDateTime(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new MutableDateTime(((java.util.Date) obj).getTime());
        } else if (obj instanceof ReadableDateTime) {
            // Copy zone + chronology instead of round-tripping through ISO text, which re-zones to the default
            // zone; always a fresh instance so a MutableDateTime argument is never aliased. A bare Joda Instant
            // deliberately stays on the string path (no zone to keep; the copy constructor would answer in UTC).
            return new MutableDateTime(obj);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to a Joda {@link MutableDateTime} instance.
     * <ul>
     *   <li>{@code null} or null-datetime strings: returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive): returns the current system time</li>
     *   <li>Numeric strings of more than four characters (an optional sign followed by ASCII decimal digits only;
     *       no {@code 0x} hex, no {@code L} suffix): parsed as milliseconds since the
     *       epoch</li>
     *   <li>20-character strings ending in {@code 'Z'}/{@code 'z'}: parsed as ISO-8601 date-time
     *       ({@code "yyyy-MM-dd'T'HH:mm:ss'Z'"})</li>
     *   <li>24-character strings ending in {@code 'Z'}/{@code 'z'}: parsed as ISO-8601 timestamp
     *       ({@code "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"})</li>
     *   <li>All other values (including 20/24-character text of any other shape, such as a compact {@code +HHmm}
     *       offset or a 4-digit fraction, and {@code 'Z'}-suffixed text the fixed formats reject): parsed as a
     *       timestamp via the default timestamp parser, {@link Dates#parseToTimestamp(String)}</li>
     * </ul>
     * The result is always in the default time zone.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return the parsed Joda {@link MutableDateTime}, or {@code null} if {@code str} is {@code null} or a null-datetime string
     * @throws IllegalArgumentException if the string format is not recognized, including numeric text outside the
     *         {@code long} range
     * @see #valueOf(Object)
     * @see AbstractJodaDateTimeType#stringOf(org.joda.time.base.AbstractInstant)
     */
    @MayReturnNull
    @Override
    public MutableDateTime valueOf(final String str) throws IllegalArgumentException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return new MutableDateTime(System.currentTimeMillis());
        }

        if (isPossibleMillis(str)) {
            try {
                // Long.parseLong, not Numbers.toLong: epoch text is decimal digits only, like the java.util.Date /
                // Calendar handlers ("0x1F4A0" must not become 128160 ms). Overflow is reported as NFE here; the
                // ArithmeticException arm mirrors the char[] overload so both paths end in the same IAE.
                return new MutableDateTime(Long.parseLong(str));
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        final int len = str.length();

        // Fast path for the two ISO-8601 UTC shapes produced by stringOf/serializeTo, selected by the terminal
        // 'Z'/'z' rather than by length alone (a 24-char "+0000" / ".1234" text must reach the general parser).
        // No charAt(10) == 'T' check: Joda's parser is lenient about lower-case 't'/'z' and year 0000, and that
        // leniency is kept. Anything the fixed formatter rejects falls through to the general parser.
        if ((len == 20 && (str.charAt(19) == 'Z' || str.charAt(19) == 'z')) || (len == 24 && (str.charAt(23) == 'Z' || str.charAt(23) == 'z'))) {
            try {
                final MutableDateTime ret = (len == 20 ? jodaISO8601DateTimeFT : jodaISO8601TimestampFT).parseMutableDateTime(str);

                // The formatters parse the 'Z'-suffixed wall time as UTC (correct instant); re-zone to the
                // default zone so the result equals() a locally-constructed MutableDateTime of the same
                // instant, like the numeric-millis path above.
                ret.setZone(DateTimeZone.getDefault());

                return ret;
            } catch (final IllegalArgumentException e) {
                // fall through to the general parser below.
            }
        }

        return new MutableDateTime(Dates.parseToTimestamp(str).getTime());
    }

    /**
     * Converts a region of a character array to a Joda {@link MutableDateTime} instance.
     * If the character sequence has more than four characters and consists of an optional sign followed only by
     * ASCII decimal digits, it is parsed as epoch milliseconds (no hexadecimal prefix or type suffix); otherwise
     * the characters are converted to a {@link String} and delegated to {@link #valueOf(String)}, so both overloads
     * give the same answer for the same text.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return the parsed mutable Joda date-time value, or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws IllegalArgumentException if the text is not a recognized date-time or numeric form (see {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public MutableDateTime valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, IllegalArgumentException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // Check the entire token for decimal digits and an optional leading sign: parseLong(char[]) also
        // accepts suffixes and some hexadecimal forms. Rejected syntax and numeric overflow fall through
        // to valueOf(String), preserving the String overload's parsing and exception behavior.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return new MutableDateTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException | ArithmeticException e) {
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
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public MutableDateTime get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
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
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public MutableDateTime get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
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
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final MutableDateTime x) throws NullPointerException, SQLException {
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
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final MutableDateTime x) throws NullPointerException, SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }
}
