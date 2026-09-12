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
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link java.time.LocalDate} values.
 * Provides serialization, deserialization, and JDBC integration for Java's {@code LocalDate} type,
 * supporting conversions to and from strings and JDBC ResultSet/PreparedStatement operations.
 *
 * <p>String representations follow the ISO-8601 standard ({@code yyyy-MM-dd}, e.g., {@code "2024-03-15"}).
 * Database columns are read and written using JDBC's native {@code LocalDate} support with a
 * {@link java.sql.Date} fallback for older drivers.</p>
 *
 * <p>The serialization {@link com.landawn.abacus.util.DateTimeFormat} of a {@code JsonXmlSerConfig} does not apply
 * to this type: a {@code LocalDate} carries no instant, so {@code serializeTo} always writes the ISO-8601 text
 * (quoted per the config), whatever {@code LONG}/{@code ISO_8601_*} setting is in effect; a field-level
 * {@code @JsonXmlField(dateFormat = "long")} is not supported for {@code LocalDate} and throws.</p>
 *
 * @see AbstractTemporalType
 * @see java.time.LocalDate
 */
public class LocalDateType extends AbstractTemporalType<LocalDate> {

    /** The type name constant for LocalDate type identification, equal to {@code "LocalDate"}. */
    public static final String LOCAL_DATE = LocalDate.class.getSimpleName();

    /**
     * Package-private constructor for LocalDateType.
     * This constructor is called by the TypeFactory to create LocalDate type instances.
     */
    LocalDateType() {
        super(LOCAL_DATE);
    }

    /**
     * Returns the Class object representing the LocalDate type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * Class<LocalDate> clazz = type.javaType();
     * }</pre>
     *
     * @return The Class object for LocalDate
     */
    @Override
    public Class<LocalDate> javaType() {
        return LocalDate.class;
    }

    /**
     * Converts a LocalDate object to its ISO-8601 string representation ({@code yyyy-MM-dd}).
     * Uses {@code LocalDate.toString()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * LocalDate date = LocalDate.of(2021, 1, 1);
     * String str = type.stringOf(date);   // "2021-01-01"
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the LocalDate object to convert; may be {@code null}
     * @return the ISO-8601 date string (e.g., {@code "2021-01-01"}), or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final LocalDate x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an Object to a LocalDate.
     * If the object is a Number, it is treated as milliseconds since epoch and converted to LocalDate using the default zone ID.
     * A {@link java.util.Date} (including its SQL subclasses) is converted the same way from its
     * epoch-millisecond value.
     * A {@link java.util.Calendar} keeps the zone attached to it, so the returned value carries the calendar's own
     * displayed fields (a calendar with no zone falls back to the default zone).
     * Otherwise, the object is converted to a string and parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * LocalDate date1 = type.valueOf(1609459200000L);
     * LocalDate date2 = type.valueOf(new java.util.Date());
     * LocalDate date3 = type.valueOf("2021-01-01");
     * }</pre>
     *
     * @param obj The object to convert to LocalDate
     * @return The LocalDate representation of the object, or {@code null} if the input is null
     * @throws IllegalArgumentException if the input is a non-lenient calendar containing invalid field values.
     * @throws DateTimeParseException if the text representation is neither a supported millisecond value nor a valid ISO-8601
     *         LocalDate.
     */
    @Override
    public LocalDate valueOf(final Object obj) throws IllegalArgumentException, DateTimeParseException {
        if (obj instanceof Number) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        } else if (obj instanceof java.util.Date) {
            return LocalDate.ofInstant(Instant.ofEpochMilli(((java.util.Date) obj).getTime()), DEFAULT_ZONE_ID);
        } else if (obj instanceof java.util.Calendar cal) {
            // Keep the zone the caller attached to the Calendar: a Calendar's displayed fields are stated in its own
            // zone, and a LocalDate is nothing but displayed fields, so rebuilding in the JVM default zone would
            // silently shift them. Matches GregorianCalendar.toZonedDateTime().toLocalDate() and the Calendar branch of
            // ZonedDateTimeType/OffsetDateTimeType.
            final java.util.TimeZone tz = cal.getTimeZone();

            return LocalDate.ofInstant(Instant.ofEpochMilli(cal.getTimeInMillis()), tz == null ? DEFAULT_ZONE_ID : tz.toZoneId());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string to create a LocalDate object.
     * The method supports multiple formats:
     * <ul>
     *   <li>{@code null}, empty string, or the literal {@code "null"} (case-insensitive) returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive) returns the current {@code LocalDate}</li>
     *   <li>Numeric strings of more than four characters (an optional sign followed by decimal digits only, as
     *       accepted by {@link Long#parseLong(String)}; no {@code 0x} hex, no {@code L} suffix) are treated as
     *       milliseconds since the epoch, interpreted in the system default zone (shorter numeric strings such as
     *       {@code "1234"} are handed to the ISO parser and rejected)</li>
     *   <li>ISO-8601 formatted strings are parsed directly via {@link LocalDate#parse(CharSequence)}</li>
     * </ul>
     *
     * <p>Every string produced by {@link LocalDate#toString()} can be parsed back into an equivalent value
     * (e.g. {@code "2021-01-01"}, including the zero-padded {@code yyyy-MM-dd} form used for years 0&ndash;999,
     * e.g. {@code "0005-01-01"}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * LocalDate date1 = type.valueOf("2021-01-01");
     * LocalDate date2 = type.valueOf("SYS_TIME");
     * LocalDate date3 = type.valueOf("1609459200000");
     * }</pre>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form produced by
     * {@code stringOf} back into a value of this type. Because {@code stringOf} delegates to
     * {@link LocalDate#toString()}, the value returned by {@code toString()} round-trips as well.</p>
     *
     * @param str the string to parse
     * @return the parsed {@code LocalDate} object, or {@code null} if the input is {@code null}, empty, or the literal {@code "null"}
     * @throws DateTimeParseException if the string is neither a millisecond number of more than
     *         four characters (within the {@code long} range) nor an ISO-8601 {@code LocalDate} representation
     * @see #valueOf(Object)
     * @see #stringOf(LocalDate)
     */
    @MayReturnNull
    @Override
    public LocalDate valueOf(final String str) throws DateTimeParseException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return LocalDate.now();
        }

        if (isPossibleMillis(str)) {
            try {
                // Long.parseLong, not Numbers.toLong: epoch text is decimal digits only, like the java.util.Date /
                // Calendar handlers ("0x1F4A0" must not become 128160 ms). Overflow is reported as NFE here; the
                // ArithmeticException arm keeps the shape of the sibling handlers, whose char[] fast path reports it
                // that way. Either exception falls through to the ISO parser's documented DateTimeParseException.
                return LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        return LocalDate.parse(str);
    }

    /**
     * Converts a character array to a LocalDate object.
     * The character array is first converted to a string, then parsed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * char[] chars = "2021-01-01".toCharArray();
     * LocalDate date = type.valueOf(chars, 0, chars.length);
     * }</pre>
     *
     * @param cbuf The character array containing the LocalDate representation
     * @param offset The starting position in the character array
     * @param len The number of characters to use
     * @return The parsed LocalDate object, or {@code null} if the input is {@code null} or empty
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws DateTimeParseException if the text representation is neither a supported millisecond value nor a valid ISO-8601 LocalDate.
     */
    @MayReturnNull
    @Override
    public LocalDate valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, DateTimeParseException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a LocalDate value from a ResultSet at the specified column index.
     * First attempts to get the value as a LocalDate object directly. If that fails,
     * falls back to retrieving it as a java.sql.Date and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalDate date = type.get(rs, 1);
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return The LocalDate value from the ResultSet, or {@code null} if the database value is NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fallback fails.
     */
    @Override
    public LocalDate get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        try {
            return rs.getObject(columnIndex, LocalDate.class);
        } catch (final SQLException e) {
            final Date ts = rs.getDate(columnIndex);

            return ts == null ? null : ts.toLocalDate();
        }
    }

    /**
     * Retrieves a LocalDate value from a ResultSet using the specified column name.
     * First attempts to get the value as a LocalDate object directly. If that fails,
     * falls back to retrieving it as a java.sql.Date and converting it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * try (ResultSet rs = stmt.executeQuery()) {
     *     if (rs.next()) {
     *         LocalDate date = type.get(rs, "birth_date");
     *     }
     * }
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnName the column label (or name if no label was specified) to retrieve the value from
     * @return The LocalDate value from the ResultSet, or {@code null} if the database value is NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fallback fails.
     */
    @Override
    public LocalDate get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        try {
            return rs.getObject(columnName, LocalDate.class);
        } catch (final SQLException e) {
            final Date ts = rs.getDate(columnName);

            return ts == null ? null : ts.toLocalDate();
        }
    }

    /**
     * Sets a LocalDate parameter in a PreparedStatement at the specified position.
     * First attempts to set the value as a LocalDate object directly. If that fails,
     * falls back to setting it as a java.sql.Date.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * LocalDate date = LocalDate.of(2021, 1, 1);
     * try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (birth_date) VALUES (?)")) {
     *     type.set(stmt, 1, date);
     *     stmt.executeUpdate();
     * }
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The LocalDate value to set, or {@code null} to set SQL NULL
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fallback fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final LocalDate x) throws NullPointerException, SQLException {
        try {
            stmt.setObject(columnIndex, x);
        } catch (final SQLException e) {
            stmt.setDate(columnIndex, x == null ? null : Date.valueOf(x));
        }
    }

    /**
     * Sets a LocalDate parameter in a CallableStatement using the specified parameter name.
     * First attempts to set the value as a LocalDate object directly. If that fails,
     * falls back to setting it as a java.sql.Date.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<LocalDate> type = TypeFactory.getType(LocalDate.class);
     * LocalDate date = LocalDate.of(2021, 1, 1);
     * try (CallableStatement stmt = conn.prepareCall("{call update_birth_date(?)}")) {
     *     type.set(stmt, "birth_date", date);
     *     stmt.execute();
     * }
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The LocalDate value to set, or {@code null} to set SQL NULL
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fallback fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final LocalDate x) throws NullPointerException, SQLException {
        try {
            stmt.setObject(parameterName, x);
        } catch (final SQLException e) {
            stmt.setDate(parameterName, x == null ? null : Date.valueOf(x));
        }
    }
}
