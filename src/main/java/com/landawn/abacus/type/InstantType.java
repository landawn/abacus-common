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
import java.time.format.DateTimeParseException;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;

/**
 * Type handler for java.time.Instant.
 * This class provides serialization, deserialization, and database access capabilities for Instant instances.
 * Instant represents a point on the time-line in UTC timezone. The default string format is ISO-8601 timestamp.
 */
public class InstantType extends AbstractTemporalType<Instant> {

    /** The type name constant for Instant type identification. */
    public static final String INSTANT = Instant.class.getSimpleName();

    /**
     * Package-private constructor for InstantType.
     * This constructor is called by the TypeFactory to create Instant type instances.
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
     * Uses a format equivalent to {@link java.time.format.DateTimeFormatter#ISO_OFFSET_DATE_TIME}
     * with the UTC zone, so the result ends in {@code 'Z'} (e.g., {@code "2023-12-25T10:30:45.123Z"}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link Instant} to serialize; may be {@code null}
     * @return the ISO-8601 timestamp string, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Instant x) {
        return (x == null) ? null : iso8601TimestampDTF.format(x.atZone(Dates.UTC_ZONE_ID));
    }

    /**
     * Converts an arbitrary object to an {@link Instant} instance.
     * Supported conversions:
     * <ul>
     *   <li>{@link Number}: treated as milliseconds since the epoch</li>
     *   <li>{@code null}: returns {@code null}</li>
     *   <li>Any other type: converted to its string representation, then parsed via {@link #valueOf(String)}</li>
     * </ul>
     *
     * @param obj the object to convert; may be {@code null}
     * @return an {@link Instant} representing the input value, or {@code null} if {@code obj} is {@code null}
     */
    @Override
    public Instant valueOf(final Object obj) {
        if (obj instanceof Number) {
            return Instant.ofEpochMilli(((Number) obj).longValue());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to an {@link Instant} instance.
     * <ul>
     *   <li>{@code null} or null-datetime strings: returns {@code null}</li>
     *   <li>{@code "sysTime"}: returns {@link Instant#now()}</li>
     *   <li>Numeric strings (possible millis): parsed as milliseconds since the epoch</li>
     *   <li>20-character strings ending in {@code 'Z'}: parsed as ISO-8601 date-time</li>
     *   <li>24-character strings ending in {@code 'Z'}: parsed as ISO-8601 timestamp with milliseconds</li>
     *   <li>All other values: parsed via {@link Instant#parse(CharSequence)}</li>
     * </ul>
     *
     * <p>Every string produced by {@link Instant#toString()} can be parsed back into an equivalent value,
     * including the second-precision form (e.g. {@code "2023-10-15T10:30:45Z"}) and fractional seconds of any
     * precision (e.g. {@code "2023-10-15T10:30:45.123456789Z"}).</p>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. The value returned by {@link Instant#toString()} round-trips
     * as well.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return the parsed {@link Instant}, or {@code null} if {@code str} is {@code null} or a null-datetime string
     * @see #valueOf(Object)
     * @see #stringOf(Instant)
     */
    @Override
    public Instant valueOf(final String str) {
        if (isNullDateTime(str)) {
            return null; // NOSONAR
        }

        if (isSysTime(str)) {
            return Instant.now();
        }

        if (isPossibleMillis(str)) {
            try {
                return Instant.ofEpochMilli(Numbers.toLong(str));
            } catch (final NumberFormatException e) {
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
     * If the character sequence looks like a {@code long} value (an epoch-millisecond timestamp),
     * it is parsed as such; otherwise the characters are converted to a {@link String} and
     * delegated to {@link #valueOf(String)}.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return an {@link Instant} parsed from the specified character region,
     *         or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     */
    @Override
    public Instant valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Instant.ofEpochMilli(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
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
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Instant get(final ResultSet rs, final int columnIndex) throws SQLException {
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
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Instant get(final ResultSet rs, final String columnName) throws SQLException {
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
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Instant x) throws SQLException {
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
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Instant x) throws SQLException {
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
     * @throws IOException if an I/O error occurs during writing
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
     *   <li>{@code ISO_8601_DATE_TIME}: writes in {@code "yyyy-MM-dd'T'HH:mm:ss'Z'"} format</li>
     *   <li>{@code ISO_8601_TIMESTAMP}: writes in {@code "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"} format</li>
     *   <li>No config / {@code null} format: uses {@link #stringOf(Instant)}</li>
     * </ul>
     * Non-{@code LONG} formats are quoted when {@code config} specifies a string quotation character.
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
     * @throws IOException if an I/O error occurs during writing
     */
    @SuppressWarnings("null")
    @Override
    public void serializeTo(final CharacterWriter writer, final Instant x, final JsonXmlSerConfig<?> config) throws IOException {
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
                        throw new RuntimeException("Unsupported operation");
                }
            }

            if (isQuote) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
