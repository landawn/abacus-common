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
import java.time.DateTimeException;
import java.time.Instant;
import java.time.format.DateTimeParseException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;

/**
 * Type handler for {@link java.time.Instant} values.
 * This class provides serialization, deserialization, and database access for {@code Instant} instances.
 * An {@code Instant} represents a point on the time-line in UTC; the default string form is an ISO-8601 timestamp.
 *
 * @see AbstractTemporalType
 * @see java.time.Instant
 */
public class InstantType extends AbstractTemporalType<Instant> {

    /** The type name constant for Instant type identification, equal to {@code "Instant"}. */
    public static final String INSTANT = Instant.class.getSimpleName();

    /**
     * Package-private constructor for {@code InstantType}.
     * Instances are created by the {@code TypeFactory}.
     */
    InstantType() {
        super(INSTANT);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Instant.class}
     */
    @Override
    public Class<Instant> javaType() {
        return Instant.class;
    }

    /**
     * Serializes an {@link Instant} to its ISO-8601 timestamp string representation.
     * Uses {@link Instant#toString()}, preserving nanoseconds in UTC (for example,
     * {@code "2023-12-25T10:30:45.123456789Z"}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; all supported instants round-trip exactly. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link Instant} to serialize; may be {@code null}
     * @return the full-precision ISO-8601 timestamp string, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Instant x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts an arbitrary object to an {@link Instant} instance.
     * Supported conversions:
     * <ul>
     *   <li>{@link Instant}: returned unchanged</li>
     *   <li>{@link Number}: treated as milliseconds since the epoch</li>
     *   <li>{@link Timestamp}: converted with nanosecond precision</li>
     *   <li>{@link java.util.Date} and {@link java.util.Calendar}: converted from their epoch-millisecond value</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to its string representation, then parsed via {@link #valueOf(String)}</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return an {@link Instant} representing the input value, or {@code null} if {@code obj} is {@code null}
     */
    @MayReturnNull
    @Override
    public Instant valueOf(final Object obj) {
        if (obj == null) {
            return null;
        } else if (obj instanceof Instant instant) {
            return instant;
        } else if (obj instanceof Number) {
            return Instant.ofEpochMilli(((Number) obj).longValue());
        } else if (obj instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        } else if (obj instanceof java.util.Date date) {
            return Instant.ofEpochMilli(date.getTime());
        } else if (obj instanceof java.util.Calendar cal) {
            return Instant.ofEpochMilli(cal.getTimeInMillis());
        }

        return valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to an {@link Instant} instance.
     * <ul>
     *   <li>{@code null} or null-datetime strings: returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive): returns {@link Instant#now()}</li>
     *   <li>Numeric strings of more than four characters (an optional sign followed by decimal digits only, as
     *       accepted by {@link Long#parseLong(String)}; no {@code 0x} hex, no {@code L} suffix): parsed as
     *       milliseconds since the epoch (shorter numeric strings such as {@code "1234"} are handed to the ISO parser
     *       and rejected)</li>
     *   <li>20-character strings ending in {@code 'Z'}: parsed as ISO-8601 date-time</li>
     *   <li>24-character strings ending in {@code 'Z'}: parsed as ISO-8601 timestamp with milliseconds</li>
     *   <li>All other values: parsed via {@link Instant#parse(CharSequence)}</li>
     * </ul>
     * The two fixed-length forms are resolved strictly: an impossible calendar date ({@code 2023-02-30},
     * {@code 2023-04-31}, {@code 2023-02-29}) is rejected. Note that {@code 24:00:00} in a {@code 'Z'}-suffixed
     * string is still accepted as next-day midnight, because the general {@link Instant#parse(CharSequence)} path
     * ({@code ISO_INSTANT}) accepts it by JDK design.
     *
     * <p>Every string produced by {@link Instant#toString()} can be parsed back into an equivalent value,
     * including the second-precision form (e.g. {@code "2023-10-15T10:30:45Z"}) and fractional seconds of any
     * precision (e.g. {@code "2023-10-15T10:30:45.123456789Z"}).</p>
     *
     * <p>This method parses the full-precision string produced by {@code stringOf}. The value returned by
     * {@link Instant#toString()} round-trips as well, including higher fractional precision.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return the parsed {@link Instant}, or {@code null} if {@code str} is {@code null} or a null-datetime string
     * @throws DateTimeParseException if the string is neither a millisecond number of more than four characters
     *         (within the {@code long} range) nor a valid ISO-8601 representation
     * @see #valueOf(Object)
     * @see #stringOf(Instant)
     */
    @MayReturnNull
    @Override
    public Instant valueOf(final String str) throws DateTimeParseException {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return Instant.now();
        }

        if (isPossibleMillis(str)) {
            try {
                // Long.parseLong, not Numbers.toLong: epoch text is decimal digits only, like the java.util.Date /
                // Calendar handlers ("0x1F4A0" must not become 128160 ms). Overflow is reported as NFE here; the
                // ArithmeticException arm mirrors the char[] overload so both paths end in DateTimeParseException.
                return Instant.ofEpochMilli(Long.parseLong(str));
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        final int len = str.length();

        // Fast path for the two most common ISO-8601 UTC forms produced by stringOf/serializeTo. If the fast-path
        // formatter rejects the input, fall back to the general parser below so that every Instant.toString()
        // form remains parseable.
        if ((len == 20 && str.charAt(19) == 'Z') || (len == 24 && str.charAt(23) == 'Z')) {
            try {
                return (len == 20 ? iso8601DateTimeDTF : iso8601TimestampDTF).parse(str, Instant::from);
            } catch (final DateTimeParseException e) {
                // fall through to the general parser below.
            }
        }

        // General path: ISO_INSTANT, which parses every form produced by Instant.toString(),
        // including fractional seconds of any precision.
        return Instant.parse(str);
    }

    /**
     * Converts a region of a character array to an {@link Instant} instance.
     * If the character sequence looks like a {@code long} value (an epoch-millisecond timestamp: digits ending in a
     * digit, so a trailing {@code L}/{@code d}/{@code f} type suffix is not accepted), it is parsed as such; otherwise
     * the characters are converted to a {@link String} and delegated to {@link #valueOf(String)}, so both overloads
     * give the same answer for the same text.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return the parsed {@link Instant}, or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws DateTimeParseException if the text is neither a millisecond number nor a valid ISO-8601 representation
     *         (see {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public Instant valueOf(final char[] cbuf, final int offset, final int len) throws DateTimeParseException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // isPossibleMillis also requires the last char to be a digit: parseLong(char[]) tolerates a trailing
        // l/L/f/F/d/D, which the String overload rejects, and an overflow (> 18 digits) surfaces as
        // ArithmeticException - both fall through to valueOf(String) so that the two overloads report the same
        // DateTimeParseException.
        if (isPossibleMillis(cbuf, offset, len)) {
            try {
                return Instant.ofEpochMilli(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException | ArithmeticException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves an {@link Instant} value from a {@link ResultSet} at the specified column index.
     * The column is read as a {@link Timestamp} and converted via {@link Timestamp#toInstant()}.
     *
     * @param rs          the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return the {@link Instant} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Instant get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : ts.toInstant();
    }

    /**
     * Retrieves an {@link Instant} value from a {@link ResultSet} using the specified column label.
     * The column is read as a {@link Timestamp} and converted via {@link Timestamp#toInstant()}.
     *
     * @param rs         the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return the {@link Instant} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Instant get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : ts.toInstant();
    }

    /**
     * Sets an {@link Instant} value as a parameter in a {@link PreparedStatement}.
     * The {@link Instant} is converted to a {@link Timestamp} via {@link Timestamp#from(Instant)}.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt        the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the {@link Instant} to set; may be {@code null}
     * @throws IllegalArgumentException if a non-null value cannot be converted to a {@code Timestamp} because its epoch-millisecond value overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Instant x) throws IllegalArgumentException, NullPointerException, SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.from(x));
    }

    /**
     * Sets an {@link Instant} value as a named parameter in a {@link CallableStatement}.
     * The {@link Instant} is converted to a {@link Timestamp} via {@link Timestamp#from(Instant)}.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt          the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the {@link Instant} to set; may be {@code null}
     * @throws IllegalArgumentException if a non-null value cannot be converted to a {@code Timestamp} because its epoch-millisecond value overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Instant x)
            throws IllegalArgumentException, NullPointerException, SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : Timestamp.from(x));
    }

    /**
     * Appends the string representation of an {@link Instant} to an {@link Appendable}.
     * Uses the default ISO-8601 timestamp format (e.g., {@code "2023-12-25T10:30:45.123Z"}).
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the {@link Instant} to append; may be {@code null}
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
    public void appendTo(final Appendable appendable, final Instant x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes an {@link Instant} value to a {@link CharacterWriter}.
     * The output format depends on the serialization configuration:
     * <ul>
     *   <li>{@code LONG}: writes epoch milliseconds as an unquoted number</li>
     *   <li>{@code ISO_8601_DATE_TIME}: writes ISO-8601 UTC at whole-second precision</li>
     *   <li>{@code ISO_8601_TIMESTAMP}: writes ISO-8601 UTC at exactly millisecond precision</li>
     *   <li>No config / {@code null} format: uses {@link #stringOf(Instant)}</li>
     * </ul>
     * Non-{@code LONG} formats are quoted when {@code config} specifies a string quotation character.
     * The parser configuration defaults to {@code LONG}; set its date-time format to {@code null}
     * to preserve nanoseconds. Explicit second/millisecond formats intentionally reduce precision.
     * If {@code x} is {@code null}, the literal {@code null} is written.
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
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the {@link Instant} to write; may be {@code null}
     * @param config the serialization configuration; may be {@code null}
     * @throws IOException if writing the selected date/time representation, quotation marks or null literal to {@code writer} fails
     * @throws ArithmeticException if the LONG format is selected and the epoch-millisecond value overflows a long
     * @throws DateTimeException if an ISO format is selected and the instant is outside the range representable as a UTC date-time
     */
    @SuppressWarnings("null")
    @Override
    public void serializeTo(final CharacterWriter writer, final Instant x, final JsonXmlSerConfig<?> config)
            throws IOException, ArithmeticException, DateTimeException {
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
                        writer.write(x.toEpochMilli());

                        break;

                    case ISO_8601_DATE_TIME:
                        writer.write(iso8601DateTimeDTF.format(x.atZone(Dates.UTC_ZONE_ID)));

                        break;

                    case ISO_8601_TIMESTAMP:
                        writer.write(iso8601TimestampDTF.format(x.atZone(Dates.UTC_ZONE_ID)));

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
