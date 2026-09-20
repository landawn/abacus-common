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
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link OffsetDateTime} objects, providing serialization, deserialization,
 * and database interaction capabilities for date-time values with timezone offset information.
 * This handler supports multiple date-time formats including ISO-8601 and epoch milliseconds.
 */
public class OffsetDateTimeType extends AbstractTemporalType<OffsetDateTime> {

    /** The type name constant for OffsetDateTime type identification, equal to {@code "OffsetDateTime"}. */
    public static final String OFFSET_DATE_TIME = OffsetDateTime.class.getSimpleName();

    /**
     * Package-private constructor for OffsetDateTimeType.
     * This constructor is called by the TypeFactory to create OffsetDateTime type instances.
     */
    OffsetDateTimeType() {
        super(OFFSET_DATE_TIME);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link OffsetDateTime} class object
     */
    @Override
    public Class<OffsetDateTime> javaType() {
        return OffsetDateTime.class;
    }

    /**
     * Converts an {@link OffsetDateTime} object to its ISO-8601 timestamp string representation.
     * Uses {@link OffsetDateTime#toString()}, preserving nanoseconds and the full UTC offset,
     * including an offset expressed in seconds.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; all supported values round-trip exactly. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the OffsetDateTime object to convert
     * @return the full-precision ISO-8601 formatted string, or {@code null} if the input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final OffsetDateTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an object to an {@link OffsetDateTime}.
     * An {@code OffsetDateTime} is returned unchanged. A {@link Number} or {@link java.util.Date} is
     * converted from epoch milliseconds in the default zone. A {@link java.util.Calendar} is converted
     * from epoch milliseconds in the calendar's own {@linkplain java.util.Calendar#getTimeZone() time zone}
     * (the default zone if it has none), so the offset the caller supplied survives the conversion. Any
     * other non-null object is converted to a string and parsed by {@link #valueOf(String)}.
     * {@link Timestamp} inputs preserve nanoseconds when converted in the default zone.
     *
     * @param obj the object to convert
     * @return the OffsetDateTime value, or {@code null} if the input is null
     * @throws IllegalArgumentException if the input is a non-lenient calendar containing invalid field values.
     * @throws DateTimeParseException if the text representation is neither a supported millisecond value nor a valid
     *         ISO-8601 {@code OffsetDateTime} (see {@link #valueOf(String)}).
     */
    @MayReturnNull
    @Override
    public OffsetDateTime valueOf(final Object obj) throws IllegalArgumentException, DateTimeParseException {
        if (obj == null) {
            return null;
        } else if (obj instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime;
        } else if (obj instanceof Number) {
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(((Number) obj).longValue()), DEFAULT_ZONE_ID);
        } else if (obj instanceof Timestamp timestamp) {
            return OffsetDateTime.ofInstant(timestamp.toInstant(), DEFAULT_ZONE_ID);
        } else if (obj instanceof java.util.Date date) {
            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), DEFAULT_ZONE_ID);
        } else if (obj instanceof java.util.Calendar cal) {
            // Keep the zone the caller attached to the Calendar - this type exists to carry its offset. Matches
            // GregorianCalendar.toZonedDateTime().toOffsetDateTime() for every Calendar subclass, not just Gregorian.
            final java.util.TimeZone tz = cal.getTimeZone();

            return OffsetDateTime.ofInstant(Instant.ofEpochMilli(cal.getTimeInMillis()), tz == null ? DEFAULT_ZONE_ID : tz.toZoneId());
        }

        return valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to an {@link OffsetDateTime} object.
     * Supports multiple formats:
     * <ul>
     *   <li>{@code null}, empty string, or the literal {@code "null"} (case-insensitive) returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive) returns the current {@code OffsetDateTime}</li>
     *   <li>Numeric strings of more than four characters (an optional sign followed by ASCII decimal digits only; no {@code 0x} hex, no {@code L} suffix) are treated as
     *       milliseconds since the epoch, interpreted in the system default zone (shorter numeric strings such as
     *       {@code "1234"} are handed to the ISO parser and rejected)</li>
     *   <li>ISO-8601 date-time format (20 characters ending with {@code 'Z'})</li>
     *   <li>ISO-8601 timestamp format (24 characters ending with {@code 'Z'})</li>
     *   <li>Any other value is parsed with the default {@link OffsetDateTime#parse(CharSequence)} parser</li>
     * </ul>
     * Invalid calendar values ({@code 2023-02-30}, {@code 2023-04-31}, {@code 2023-02-29}, {@code 24:00}) are
     * rejected on every path, the two fixed-length forms included.
     *
     * <p>Every string produced by {@link OffsetDateTime#toString()} can be parsed back into an equivalent value,
     * including:</p>
     * <ul>
     *   <li>the seconds-omitted form (e.g. {@code "2023-10-15T10:30Z"})</li>
     *   <li>fractional seconds of any precision (e.g. {@code "2023-10-15T10:30:45.123456789Z"})</li>
     *   <li>numeric UTC offsets, with optional offset-seconds (e.g. {@code "2023-10-15T10:30:45+05:30:15"})</li>
     * </ul>
     *
     * <p>This method parses the full-precision string produced by {@code stringOf}. It also accepts the more
     * general {@link OffsetDateTime#toString()} representation, including nanoseconds and offsets with seconds.</p>
     *
     * @param str the string to parse
     * @return the parsed OffsetDateTime, or {@code null} if the input is {@code null} or represents a {@code null} date-time
     * @throws DateTimeParseException if the string is neither a millisecond number of more than four characters
     *         (within the {@code long} range) nor a valid ISO-8601 {@code OffsetDateTime} representation
     * @see #valueOf(Object)
     * @see #stringOf(OffsetDateTime)
     */
    @MayReturnNull
    @Override
    public OffsetDateTime valueOf(final String str) throws DateTimeParseException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return OffsetDateTime.now();
        }

        if (isPossibleMillis(str)) {
            try {
                // Long.parseLong, not Numbers.toLong: epoch text is decimal digits only, like the java.util.Date /
                // Calendar handlers ("0x1F4A0" must not become 128160 ms). Overflow is reported as NFE here; the
                // ArithmeticException arm mirrors the char[] overload so both paths end in DateTimeParseException.
                return OffsetDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        final int len = str.length();

        // Fast path for the two most common ISO-8601 UTC forms produced by stringOf/serializeTo. If the fast-path
        // formatter rejects the input, fall back to the general parser below so that every OffsetDateTime.toString()
        // form remains parseable.
        if ((len == 20 && str.charAt(19) == 'Z') || (len == 24 && str.charAt(23) == 'Z')) {
            try {
                return OffsetDateTime.parse(str, len == 20 ? iso8601DateTimeDTF : iso8601TimestampDTF);
            } catch (final DateTimeParseException e) {
                // fall through to the general parser below.
            }
        }

        // General path: ISO_OFFSET_DATE_TIME, which parses every form produced by OffsetDateTime.toString(),
        // including numeric offsets (with optional offset-seconds), fractional seconds of any precision,
        // and the seconds-omitted form.
        return OffsetDateTime.parse(str);
    }

    /**
     * Converts a character array to an {@link OffsetDateTime} object.
     * This method first checks for more than four characters consisting of an optional sign followed only by
     * ASCII decimal digits. If found, it creates an OffsetDateTime from that epoch-millisecond value;
     * hexadecimal prefixes and type suffixes are not accepted. Otherwise, it converts the character array to a string and delegates to
     * {@link #valueOf(String)}, so both overloads give the same answer for the same text.
     *
     * @param cbuf the character array containing the date-time string
     * @param offset the offset in the array where the date-time string starts
     * @param len the length of the date-time string
     * @return the parsed OffsetDateTime, or {@code null} if the input is {@code null} or empty
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns {@code null} without reading.
     * @throws DateTimeParseException if the text is neither a millisecond number nor a valid ISO-8601 representation
     *         (see {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public OffsetDateTime valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, DateTimeParseException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // Check the entire token for decimal digits and an optional leading sign: parseLong(char[]) also
        // accepts suffixes and some hexadecimal forms. Rejected syntax and numeric overflow fall through
        // to valueOf(String), preserving the String overload's parsing and exception behavior.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return OffsetDateTime.ofInstant(Instant.ofEpochMilli(parseLong(cbuf, offset, len)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves an {@link OffsetDateTime} value from a ResultSet at the specified column index.
     * The timestamp from the database is converted to OffsetDateTime using the default zone ID.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return an OffsetDateTime representing the timestamp, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public OffsetDateTime get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Retrieves an {@link OffsetDateTime} value from a ResultSet using the specified column label.
     * The timestamp from the database is converted to OffsetDateTime using the default zone ID.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return an OffsetDateTime representing the timestamp, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public OffsetDateTime get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : OffsetDateTime.ofInstant(ts.toInstant(), DEFAULT_ZONE_ID);
    }

    /**
     * Sets a parameter in a PreparedStatement to an {@link OffsetDateTime} value.
     * The OffsetDateTime is converted to a Timestamp for database storage.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the OffsetDateTime value to set, or {@code null} to set SQL NULL
     * @throws IllegalArgumentException if a non-null value cannot be converted to a {@code Timestamp} because its epoch-millisecond value overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final OffsetDateTime x)
            throws IllegalArgumentException, NullPointerException, SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Sets a named parameter in a CallableStatement to an {@link OffsetDateTime} value.
     * The OffsetDateTime is converted to a Timestamp for database storage.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the OffsetDateTime value to set, or {@code null} to set SQL NULL
     * @throws IllegalArgumentException if a non-null value cannot be converted to a {@code Timestamp} because its epoch-millisecond value overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final OffsetDateTime x)
            throws IllegalArgumentException, NullPointerException, SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : Timestamp.from(x.toInstant()));
    }

    /**
     * Appends the string representation of an {@link OffsetDateTime} to an Appendable.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the OffsetDateTime value to append
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
    public void appendTo(final Appendable appendable, final OffsetDateTime x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes the character representation of an {@link OffsetDateTime} to a CharacterWriter.
     * The format depends on the serialization configuration:
     * <ul>
     *   <li>{@code LONG}: epoch milliseconds written as a numeric value</li>
     *   <li>{@code ISO_8601_DATE_TIME}: ISO offset date-time at whole-second precision</li>
     *   <li>{@code ISO_8601_TIMESTAMP}: ISO offset date-time at exactly millisecond precision</li>
     *   <li>Default ({@code null} config or {@code null} format): same as {@link #stringOf(OffsetDateTime)}</li>
     * </ul>
     * When the format is not {@code LONG} and the config specifies a string quotation character,
     * the formatted value is wrapped in that quotation character.
     * The parser configuration defaults to {@code LONG}; set its date-time format to {@code null}
     * to preserve nanoseconds and offset seconds. Explicit second/millisecond formats reduce precision.
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
     * @param x the OffsetDateTime value to write; if {@code null}, writes the literal {@code "null"}
     * @param config the serialization configuration specifying format and quoting; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the selected date/time representation, quotation marks or null literal to {@code writer} fails
     * @throws ArithmeticException if the LONG format is selected and the epoch-millisecond value overflows a long
     */
    @SuppressWarnings("null")
    @Override
    public void serializeTo(final CharacterWriter writer, final OffsetDateTime x, final JsonXmlSerConfig<?> config)
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
