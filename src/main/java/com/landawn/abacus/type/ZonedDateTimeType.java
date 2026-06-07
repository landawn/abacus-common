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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

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
 *   <li>"SYS_TIME" keyword for current time</li>
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
 * // Convert to ISO 8601 timestamp string
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
     * The type name identifier for {@link ZonedDateTime} type (the simple class name).
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
     * This method formats the ZonedDateTime using a format equivalent to
     * {@link java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME}, including the UTC offset
     * (for example {@code "2023-10-15T10:30:00+01:00"}, or with a {@code 'Z'} suffix when the offset is UTC).
     * If the input is {@code null}, this method returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt = ZonedDateTime.now();
     * String str = type.stringOf(zdt);   // e.g. "2023-10-15T10:30:00+01:00"
     * }</pre>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the ZonedDateTime instance to convert to string
     * @return the ISO 8601 string representation, or {@code null} if the input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final ZonedDateTime x) {
        return (x == null) ? null : iso8601TimestampDTF.format(x);
    }

    /**
     * Converts an object to a ZonedDateTime instance.
     * <p>
     * This method handles conversion from:
     * </p>
     * <ul>
     *   <li>Number types - interpreted as epoch milliseconds in the default timezone</li>
     *   <li>String types - parsed according to supported date/time formats</li>
     *   <li>null - returns null</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ZonedDateTime zdt1 = type.valueOf(1697364600000L);           // From epoch milliseconds
     * ZonedDateTime zdt2 = type.valueOf("2023-10-15T10:30:00Z");   // From string
     * }</pre>
     *
     * @param obj the object to convert to ZonedDateTime
     * @return a ZonedDateTime instance, or {@code null} if the input is null
     */
    @Override
    public ZonedDateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string to a ZonedDateTime instance.
     * <p>
     * This method handles several input formats:
     * </p>
     * <ul>
     *   <li>{@code null}, empty string, or the literal "null" returns {@code null}</li>
     *   <li>"SYS_TIME" returns the current ZonedDateTime</li>
     *   <li>Numeric strings are interpreted as epoch milliseconds</li>
     *   <li>ISO 8601 date-time strings with 'Z' suffix (20 chars) are parsed as ISO date-time</li>
     *   <li>ISO 8601 timestamp strings with 'Z' suffix (24 chars) are parsed as ISO timestamp</li>
     *   <li>Any other value is parsed with the default {@link ZonedDateTime#parse(CharSequence)} parser</li>
     * </ul>
     *
     * <p>Every string produced by {@link ZonedDateTime#toString()} can be parsed back into an equivalent value,
     * including:</p>
     * <ul>
     *   <li>the seconds-omitted form (e.g. {@code "2023-10-15T10:30Z"})</li>
     *   <li>fractional seconds of any precision (e.g. {@code "2023-10-15T10:30:45.123456789Z"})</li>
     *   <li>numeric UTC offsets, with optional offset-seconds (e.g. {@code "2023-10-15T10:30:45+05:45"})</li>
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
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. It additionally accepts the output of
     * {@link ZonedDateTime#toString()} — including the region-zone suffix shown above, which {@code stringOf} itself
     * does not emit — so values written with {@code toString()} round-trip as well.</p>
     *
     * @param str the string to convert to ZonedDateTime
     * @return a ZonedDateTime instance, or {@code null} if the string is {@code null}, empty, or the literal "null"
     * @throws DateTimeParseException if the string cannot be parsed as a valid date/time
     * @see #valueOf(Object)
     * @see #stringOf(ZonedDateTime)
     */
    @Override
    public ZonedDateTime valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return ZonedDateTime.now();
        }

        if (isPossibleMillis(str)) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e) {
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
     * This method first checks if the character array represents a long value (epoch milliseconds).
     * If so, it creates a ZonedDateTime from that timestamp. Otherwise, it converts the
     * character array to a string and delegates to {@link #valueOf(String)}.
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
     */
    @Override
    public ZonedDateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(parseLong(cbuf, offset, len)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e) {
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
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public ZonedDateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
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
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public ZonedDateTime get(final ResultSet rs, final String columnName) throws SQLException {
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
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final ZonedDateTime x) throws SQLException {
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
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final ZonedDateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Appends the string representation of a ZonedDateTime to an Appendable.
     * <p>
     * This method formats the ZonedDateTime using the ISO 8601 timestamp format and appends it
     * to the provided Appendable. If the ZonedDateTime is {@code null}, it appends the string "null".
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * StringBuilder sb = new StringBuilder();
     * type.appendTo(sb, ZonedDateTime.now());   // Appends formatted date/time
     * }</pre>
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the ZonedDateTime value to append
     * @throws IOException if an I/O error occurs during the append operation
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
    public void appendTo(final Appendable appendable, final ZonedDateTime x) throws IOException {
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
     *   <li>ISO_8601_DATE_TIME: writes in ISO 8601 date-time format</li>
     *   <li>ISO_8601_TIMESTAMP: writes in ISO 8601 timestamp format</li>
     *   <li>Default: uses the ISO 8601 timestamp format</li>
     * </ul>
     * <p>
     * The output may be quoted based on the configuration settings, except for LONG format.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharacterWriter writer = new CharacterWriter();
     * JsonXmlSerConfig<?> config = JsonXmlSerConfig.of();
     * type.serializeTo(writer, ZonedDateTime.now(), config);   // Writes formatted date/time
     * }</pre>
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the ZonedDateTime value to write
     * @param config the serialization configuration controlling format and quoting
     * @throws IOException if an I/O error occurs during the write operation
     */
    @SuppressWarnings("null")
    @Override
    public void serializeTo(final CharacterWriter writer, final ZonedDateTime x, final JsonXmlSerConfig<?> config) throws IOException {
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
                        throw new RuntimeException("Unsupported operation");
                }
            }

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
