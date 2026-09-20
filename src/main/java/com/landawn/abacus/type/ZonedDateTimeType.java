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

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link java.time.ZonedDateTime} instances.
 * <p>
 * This class provides conversion between ZonedDateTime objects and their string representations,
 * supporting ISO 8601 formats and epoch milliseconds. It handles database operations by converting
 * ZonedDateTime to/from JDBC Timestamp using the default system timezone.
 * </p>
 *
 * <p><b>Supported Input Formats:</b></p>
 * <ul>
 *   <li>ISO 8601 date-time with 'Z' suffix (e.g., "2023-10-15T10:30:00Z")</li>
 *   <li>ISO 8601 timestamp with milliseconds (e.g., "2023-10-15T10:30:00.123Z")</li>
 *   <li>Standard ZonedDateTime string format</li>
 *   <li>Epoch milliseconds as numeric string</li>
 *   <li>{@code "sysTime"} or {@code "SYS_TIME"} keyword (case-insensitive) for current time</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get the ZonedDateTime type handler
 * Type<ZonedDateTime> type = TypeFactory.getType(ZonedDateTime.class);
 *
 * // Convert string to ZonedDateTime
 * ZonedDateTime zdt = type.valueOf("2023-10-15T10:30:00Z");
 *
 * // Convert from epoch milliseconds
 * ZonedDateTime fromEpoch = type.valueOf(1697364600000L);
 *
 * // Get current time
 * ZonedDateTime now = type.valueOf("SYS_TIME");
 *
 * // Convert to ZonedDateTime's standard text form (including a region zone when present)
 * String str = type.stringOf(zdt);
 *
 * // Use with PreparedStatement
 * type.set(preparedStatement, 1, zdt);
 *
 * // Retrieve from ResultSet
 * ZonedDateTime result = type.get(resultSet, "event_time");
 * }</pre>
 *
 * @see java.time.ZonedDateTime
 * @see AbstractTemporalType
 */
public class ZonedDateTimeType extends AbstractTemporalType<ZonedDateTime> {

    /**
     * The type name identifier for {@link ZonedDateTime} type, equal to the simple class name
     * {@code "ZonedDateTime"}.
     */
    public static final String ZONED_DATE_TIME = ZonedDateTime.class.getSimpleName();

    /**
     * Constructs a ZonedDateTimeType instance.
     * This constructor is package-private and should only be called by TypeFactory.
     */
    ZonedDateTimeType() {
        super(ZONED_DATE_TIME);
    }

    /**
     * Returns the Class object representing the ZonedDateTime class.
     * <p>
     * This method returns {@code ZonedDateTime.class}, which is the Class object for the
     * {@link java.time.ZonedDateTime} class that this type handles.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<ZonedDateTime> type = TypeFactory.getType(ZonedDateTime.class);
     * Class<ZonedDateTime> clazz = type.javaType();   // Returns ZonedDateTime.class
     * }</pre>
     *
     * @return the Class object for ZonedDateTime.class
     */
    @Override
    public Class<ZonedDateTime> javaType() {
        return ZonedDateTime.class;
    }

    /**
     * Converts a ZonedDateTime instance to its string representation.
     * <p>
     * This method uses the standard {@link ZonedDateTime#toString()} representation, including both
     * the UTC offset and the region zone ID when one is present (for example,
     * {@code "2023-10-15T10:30:45+02:00[Europe/Paris]"}). If the input is {@code null}, this method
     * returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = ZonedDateTime.now();
     * String str = type.stringOf(zdt);   // e.g. "2023-10-15T10:30:45+02:00[Europe/Paris]"
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed by {@link #valueOf(String)}.
     * It preserves the local date-time, offset, instant, and region {@link java.time.ZoneId}, so the parsed value
     * remains subject to the same regional time-zone rules.</p>
     *
     * @param x the ZonedDateTime instance to convert to string
     * @return the ISO 8601 string representation, or {@code null} if the input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final ZonedDateTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an object to a ZonedDateTime instance.
     * <p>
     * This method handles conversion from:
     * </p>
     * <ul>
     *   <li>{@link ZonedDateTime} - returned unchanged</li>
     *   <li>{@link Number} or {@link java.util.Date} - interpreted as epoch milliseconds in the default zone</li>
     *   <li>{@link java.util.Calendar} - interpreted as epoch milliseconds in the calendar's own
     *       {@linkplain java.util.Calendar#getTimeZone() time zone} (the default zone if it has none), so the zone the
     *       caller supplied survives the conversion</li>
     *   <li>Other non-null objects - converted to a string and parsed according to supported date/time formats</li>
     *   <li>{@code null} - returns {@code null}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt1 = type.valueOf(1697364600000L);           // From epoch milliseconds
     * ZonedDateTime zdt2 = type.valueOf("2023-10-15T10:30:00Z");   // From string
     * }</pre>
     *
     * <p>SQL Timestamp inputs preserve nanoseconds in the default zone.</p>
     *
     * @param obj the object to convert to ZonedDateTime
     * @return a ZonedDateTime instance, or {@code null} if the input is null
     * @throws IllegalArgumentException if the input is a non-lenient calendar containing invalid field values.
     * @throws DateTimeParseException if the text representation is neither a supported millisecond value nor a valid
     *         ISO-8601 {@code ZonedDateTime} (see {@link #valueOf(String)}).
     */
    @MayReturnNull
    @Override
    public ZonedDateTime valueOf(final Object obj) throws IllegalArgumentException, DateTimeParseException {
        if (obj == null) {
            return null;
        } else if (obj instanceof ZonedDateTime zonedDateTime) {
            return zonedDateTime;
        } else if (obj instanceof Number) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        } else if (obj instanceof java.sql.Timestamp timestamp) {
            return ZonedDateTime.ofInstant(timestamp.toInstant(), DEFAULT_ZONE_ID);
        } else if (obj instanceof java.util.Date date) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), DEFAULT_ZONE_ID);
        } else if (obj instanceof java.util.Calendar cal) {
            // Keep the zone the caller attached to the Calendar - this type exists to carry one. Matches
            // GregorianCalendar.toZonedDateTime() for every Calendar subclass, not just Gregorian.
            final java.util.TimeZone tz = cal.getTimeZone();

            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(cal.getTimeInMillis()), tz == null ? DEFAULT_ZONE_ID : tz.toZoneId());
        }

        return valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string to a ZonedDateTime instance.
     * <p>
     * This method handles several input formats:
     * </p>
     * <ul>
     *   <li>{@code null}, empty string, or the literal "null" returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive) returns the current ZonedDateTime</li>
     *   <li>Numeric strings of more than four characters (an optional sign followed by ASCII decimal digits only; no {@code 0x} hex, no {@code L} suffix) are interpreted as
     *       epoch milliseconds in the system default zone (shorter numeric strings such as {@code "1234"} are handed
     *       to the ISO parser and rejected)</li>
     *   <li>ISO 8601 date-time strings with 'Z' suffix (20 chars) are parsed as ISO date-time</li>
     *   <li>ISO 8601 timestamp strings with 'Z' suffix (24 chars) are parsed as ISO timestamp</li>
     *   <li>Any other value is parsed with the default {@link ZonedDateTime#parse(CharSequence)} parser</li>
     * </ul>
     * Invalid calendar values ({@code 2023-02-30}, {@code 2023-04-31}, {@code 2023-02-29}, {@code 24:00}) are
     * rejected on every path, the two fixed-length forms included.
     *
     * <p>Every string produced by {@link ZonedDateTime#toString()} can be parsed back into an equivalent value,
     * including:</p>
     * <ul>
     *   <li>the seconds-omitted form (e.g. {@code "2023-10-15T10:30Z"})</li>
     *   <li>fractional seconds of any precision (e.g. {@code "2023-10-15T10:30:45.123456789Z"})</li>
     *   <li>numeric UTC offsets, with optional offset-seconds (e.g. {@code "2023-10-15T10:30:45+05:30:15"})</li>
     *   <li>the region-zone suffix (e.g. {@code "2023-10-15T10:30:45-07:00[America/Los_Angeles]"}), whose
     *       {@link java.time.ZoneId} is preserved</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt1 = type.valueOf("2023-10-15T10:30:00Z");   // ISO 8601 format
     * ZonedDateTime zdt2 = type.valueOf("SYS_TIME");               // Current time
     *
     * // Round-trips the output of ZonedDateTime.toString(), region zone included
     * ZonedDateTime src = ZonedDateTime.now(ZoneId.of("America/Los_Angeles"));
     * ZonedDateTime copy = type.valueOf(src.toString());          // equal to src
     * }</pre>
     *
     * <p>This method parses the string produced by {@code stringOf} back into the same local date-time, offset,
     * instant, and region {@link java.time.ZoneId}. Because {@code stringOf} delegates to
     * {@link ZonedDateTime#toString()}, values written with {@code toString()} round-trip as well.</p>
     *
     * @param str the string to convert to ZonedDateTime
     * @return a ZonedDateTime instance, or {@code null} if the string is {@code null}, empty, or the literal "null"
     * @throws DateTimeParseException if the string is neither a millisecond number of more than four characters
     *         (within the {@code long} range) nor a valid ISO-8601 date/time representation
     * @see #valueOf(Object)
     * @see #stringOf(ZonedDateTime)
     */
    @MayReturnNull
    @Override
    public ZonedDateTime valueOf(final String str) throws DateTimeParseException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return ZonedDateTime.now();
        }

        if (isPossibleMillis(str)) {
            try {
                // Long.parseLong, not Numbers.toLong: epoch text is decimal digits only, like the java.util.Date /
                // Calendar handlers ("0x1F4A0" must not become 128160 ms). Overflow is reported as NFE here; the
                // ArithmeticException arm mirrors the char[] overload so both paths end in DateTimeParseException.
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        final int len = str.length();

        // Fast path for the two most common ISO-8601 UTC forms produced by stringOf/serializeTo. If the fast-path
        // formatter rejects the input, fall back to the general parser below so that every ZonedDateTime.toString()
        // form remains parseable.
        if ((len == 20 && str.charAt(19) == 'Z') || (len == 24 && str.charAt(23) == 'Z')) {
            try {
                return ZonedDateTime.parse(str, len == 20 ? iso8601DateTimeDTF : iso8601TimestampDTF);
            } catch (final DateTimeParseException e) {
                // fall through to the general parser below.
            }
        }

        // General path: ISO_ZONED_DATE_TIME, which parses every form produced by ZonedDateTime.toString(),
        // including the optional region-zone suffix, numeric offsets (with optional offset-seconds),
        // fractional seconds of any precision, and the seconds-omitted form.
        return ZonedDateTime.parse(str);
    }

    /**
     * Converts a character array to a ZonedDateTime instance.
     * <p>
     * This method first checks if the character array represents a long value (epoch milliseconds: digits ending
     * in a digit, so a trailing {@code L}/{@code d}/{@code f} type suffix is not accepted). If so, it creates a
     * ZonedDateTime from that timestamp. Otherwise, it converts the character array to a string and delegates to
     * {@link #valueOf(String)}, so both overloads give the same answer for the same text.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = "1697364600000".toCharArray();
     * ZonedDateTime zdt = type.valueOf(chars, 0, chars.length);   // From epoch millis
     * }</pre>
     *
     * @param cbuf the character array containing the date/time representation
     * @param offset the starting position in the character array
     * @param len the number of characters to process
     * @return a ZonedDateTime instance, or {@code null} if the input is {@code null} or empty
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns {@code null} without reading.
     * @throws DateTimeParseException if the text is neither a millisecond number nor a valid ISO-8601 representation
     *         (see {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public ZonedDateTime valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, DateTimeParseException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // Check the entire token for decimal digits and an optional leading sign: parseLong(char[]) also
        // accepts suffixes and some hexadecimal forms. Rejected syntax and numeric overflow fall through
        // to valueOf(String), preserving the String overload's parsing and exception behavior.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(parseLong(cbuf, offset, len)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a ZonedDateTime value from a ResultSet at the specified column index.
     * <p>
     * This method reads a Timestamp value from the ResultSet and converts it to a
     * ZonedDateTime in the default timezone.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = type.get(resultSet, 1);
     * // Retrieves timestamp from first column as ZonedDateTime
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) of the timestamp value
     * @return the ZonedDateTime value, or {@code null} if the database value is NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public ZonedDateTime get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Retrieves a ZonedDateTime value from a ResultSet using the specified column label.
     * <p>
     * This method reads a Timestamp value from the ResultSet and converts it to a
     * ZonedDateTime in the default timezone.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = type.get(resultSet, "created_date");
     * // Retrieves timestamp from the "created_date" column
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column containing the timestamp value
     * @return the ZonedDateTime value, or {@code null} if the database value is NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public ZonedDateTime get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : ZonedDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Sets a ZonedDateTime value in a PreparedStatement at the specified parameter index.
     * <p>
     * This method converts the ZonedDateTime to a Timestamp and sets it in the
     * PreparedStatement. If the ZonedDateTime is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = ZonedDateTime.now();
     * type.set(preparedStatement, 1, zdt);   // Sets timestamp at first parameter
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the value in
     * @param columnIndex the parameter index (1-based) where to set the value
     * @param x the ZonedDateTime value to set, or {@code null} for SQL NULL
     * @throws IllegalArgumentException if a non-null value cannot be converted to a {@code Timestamp} because its epoch-millisecond value overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final ZonedDateTime x)
            throws IllegalArgumentException, NullPointerException, SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Sets a ZonedDateTime value in a CallableStatement using the specified parameter name.
     * <p>
     * This method converts the ZonedDateTime to a Timestamp and sets it in the
     * CallableStatement. If the ZonedDateTime is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = ZonedDateTime.now();
     * type.set(callableStatement, "created_date", zdt);   // Sets timestamp parameter
     * }</pre>
     *
     * @param stmt the CallableStatement to set the value in
     * @param parameterName the name of the parameter where to set the value
     * @param x the ZonedDateTime value to set, or {@code null} for SQL NULL
     * @throws IllegalArgumentException if a non-null value cannot be converted to a {@code Timestamp} because its epoch-millisecond value overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final ZonedDateTime x)
            throws IllegalArgumentException, NullPointerException, SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Appends the string representation of a ZonedDateTime to an Appendable.
     * <p>
     * This method appends the value returned by {@link ZonedDateTime#toString()} to the provided
     * Appendable. If the ZonedDateTime is {@code null}, it appends the string {@code "null"}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * type.appendTo(sb, ZonedDateTime.now());   // Appends formatted date/time
     * }</pre>
     *
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes the configured JSON/XML
     * serialized form, including string quotation for non-LONG date/time formats when requested by config.
     *
     * @param appendable the Appendable to write to
     * @param x the ZonedDateTime value to append
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if appending the formatted date/time text or null literal to {@code appendable} fails
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @Override
    public void appendTo(final Appendable appendable, final ZonedDateTime x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes the character representation of a ZonedDateTime to a CharacterWriter.
     * <p>
     * This method handles different date/time formats based on the provided configuration:
     * </p>
     * <ul>
     *   <li>LONG format: writes the epoch milliseconds as a number</li>
     *   <li>ISO_8601_DATE_TIME: writes an offset date-time at whole-second precision</li>
     *   <li>ISO_8601_TIMESTAMP: writes an offset date-time at exactly millisecond precision</li>
     *   <li>Default: delegates to {@link #stringOf(ZonedDateTime)}, preserving the region zone and available precision</li>
     * </ul>
     * <p>
     * The output may be quoted based on the configuration settings, except for LONG format.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
     * JsonSerConfig config = JsonSerConfig.create();
     * try {
     *     type.serializeTo(writer, ZonedDateTime.now(), config);   // Writes formatted date/time
     * } finally {
     *     Objectory.recycle(writer);
     * }
     * }</pre>
     *
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the configured serialized form of
     * {@code x} to the {@code CharacterWriter}. {@link DateTimeFormat#LONG} writes epoch milliseconds without quotes;
     * string date/time formats use the requested string quotation when configured.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML using the selected
     * date/time format, whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering.
     *
     * @param writer the CharacterWriter to write to
     * @param x the ZonedDateTime value to write
     * @param config the serialization configuration controlling format and quoting; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the selected date/time representation, quotation marks or null literal to {@code writer} fails
     * @throws ArithmeticException if the LONG format is selected and the epoch-millisecond value overflows a long
     */
    @SuppressWarnings("null")
    @Override
    public void serializeTo(final CharacterWriter writer, final ZonedDateTime x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, ArithmeticException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final boolean isQuote = (config != null) && (config.getStringQuotation() != 0) && (config.getDateTimeFormat() != DateTimeFormat.LONG);

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }

            if ((config == null) || (config.getDateTimeFormat() == null)) {
                writer.write(stringOf(x));
            } else {
                switch (config.getDateTimeFormat()) {
                    case LONG:
                        writer.write(x.toInstant().toEpochMilli());

                        break;

                    case ISO_8601_DATE_TIME:
                        writer.write(iso8601DateTimeDTF.format(x));

                        break;

                    case ISO_8601_TIMESTAMP:
                        writer.write(iso8601TimestampDTF.format(x));

                        break;

                    default:
                        throw new RuntimeException("Unsupported DateTimeFormat: " + config.getDateTimeFormat());
                }
            }

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
