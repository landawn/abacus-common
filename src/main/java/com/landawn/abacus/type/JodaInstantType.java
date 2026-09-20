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

import org.joda.time.Instant;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.DateTimeFormat;
import com.landawn.abacus.util.Dates;

/**
 * Type handler for Joda-Time {@link org.joda.time.Instant} objects.
 * <p>
 * This class provides serialization, deserialization, and database access for
 * {@code Instant} instances. It supports multiple string formats including ISO-8601,
 * numeric milliseconds, and maps to the database via {@link java.sql.Timestamp}.
 *
 * @see org.joda.time.Instant
 * @see AbstractJodaDateTimeType
 */
public class JodaInstantType extends AbstractJodaDateTimeType<Instant> {

    /**
     * The type name constant for Joda-Time {@link Instant} type identification, equal to
     * {@code "JodaInstant"} &mdash; deliberately <i>not</i> the simple class name, so that this type
     * does not collide with the registered name of {@code java.time.Instant}.
     */
    public static final String INSTANT = "JodaInstant";

    /**
     * Package-private constructor for JodaInstantType.
     * This constructor is called by the TypeFactory to create Joda Instant type instances.
     */
    JodaInstantType() {
        super(INSTANT);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code org.joda.time.Instant.class}
     */
    @Override
    public Class<Instant> javaType() {
        return Instant.class;
    }

    /**
     * Serializes a Joda {@link Instant} to its ISO-8601 timestamp string representation.
     * Uses millisecond precision in the format {@code "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"}
     * (e.g., {@code "2021-01-01T10:30:00.123Z"}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * <p>Unlike the {@code java.util.Date}/{@code Calendar} handlers, no year-range check is applied here: an instant
     * outside Common Era years 0001 through 9999 is printed as is, for example {@code "10000-01-01T00:00:00.000Z"} or
     * {@code "-0001-01-01T00:00:00.000Z"}. Text whose year has more than four digits or a leading {@code '-'} is
     * rejected by this handler's inverse parser ({@link #valueOf(String)}), so it does not round-trip through this handler. Year {@code 0000} is the one out-of-range value that does:
     * {@code "0000-12-31T23:59:59.999Z"} is read back at the same instant here (Joda accepts year zero), although the
     * {@code Date}/{@code Calendar} handlers reject it.</p>
     *
     * @param x the {@link Instant} to serialize; may be {@code null}
     * @return the ISO-8601 timestamp string, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Instant x) {
        return (x == null) ? null : x.toString(jodaISO8601TimestampFT);
    }

    /**
     * Converts a string representation to a Joda {@link Instant} instance.
     * <ul>
     *   <li>{@code null} or null-datetime strings: returns {@code null}</li>
     *   <li>{@code "sysTime"} or {@code "SYS_TIME"} (case-insensitive): returns {@link Instant#now()}</li>
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
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or empty
     * @return the parsed {@link Instant}, or {@code null} if {@code str} is {@code null} or a null-datetime string
     * @throws IllegalArgumentException if the string format is not recognized, including numeric text outside the
     *         {@code long} range
     * @see #valueOf(Object)
     * @see #stringOf(Instant)
     */
    @MayReturnNull
    @Override
    public Instant valueOf(final String str) throws IllegalArgumentException {
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
                // ArithmeticException arm mirrors the char[] overload so both paths end in the same IAE.
                return Instant.ofEpochMilli(Long.parseLong(str));
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
                return Instant.parse(str, len == 20 ? jodaISO8601DateTimeFT : jodaISO8601TimestampFT);
            } catch (final IllegalArgumentException e) {
                // fall through to the general parser below.
            }
        }

        return Instant.ofEpochMilli(Dates.parseToTimestamp(str).getTime());
    }

    /**
     * Converts a region of a character array to a Joda {@link Instant} instance.
     * If the character sequence has more than four characters and consists of an optional sign followed only by
     * ASCII decimal digits, it is parsed as epoch milliseconds (no hexadecimal prefix or type suffix); otherwise
     * the characters are converted to a {@link String} and delegated to {@link #valueOf(String)}, so both overloads
     * give the same answer for the same text.
     *
     * @param cbuf   the character array containing the value; may be {@code null}
     * @param offset the index of the first character to use
     * @param len    the number of characters to use
     * @return the parsed Joda instant, or {@code null} if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns {@code null} without reading.
     * @throws IllegalArgumentException if the text is not a recognized date-time or numeric form (see
     *         {@link #valueOf(String)}), including numeric text outside the {@code long} range
     */
    @MayReturnNull
    @Override
    public Instant valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException, IllegalArgumentException {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        // Check the entire token for decimal digits and an optional leading sign: parseLong(char[]) also
        // accepts suffixes and some hexadecimal forms. Rejected syntax and numeric overflow fall through
        // to valueOf(String), preserving the String overload's parsing and exception behavior.
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
     * Retrieves a Joda {@link Instant} value from a {@link ResultSet} at the specified column index.
     * The column is read as a {@link java.sql.Timestamp} and converted via
     * {@link Instant#ofEpochMilli(long)}.
     *
     * @param rs          the {@link ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return a Joda {@link Instant} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Instant get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : Instant.ofEpochMilli(ts.getTime());
    }

    /**
     * Retrieves a Joda {@link Instant} value from a {@link ResultSet} using the specified column label.
     * The column is read as a {@link java.sql.Timestamp} and converted via
     * {@link Instant#ofEpochMilli(long)}.
     *
     * @param rs         the {@link ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return a Joda {@link Instant} from the column, or {@code null} if the column value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Instant get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : Instant.ofEpochMilli(ts.getTime());
    }

    /**
     * Sets a Joda {@link Instant} value as a parameter in a {@link PreparedStatement}.
     * The {@link Instant} is converted to a {@link java.sql.Timestamp}.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt        the {@link PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the Joda {@link Instant} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Instant x) throws NullPointerException, SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Sets a Joda {@link Instant} value as a named parameter in a {@link CallableStatement}.
     * The {@link Instant} is converted to a {@link java.sql.Timestamp}.
     * A {@code null} value sets SQL {@code NULL}.
     *
     * @param stmt          the {@link CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the Joda {@link Instant} to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Instant x) throws NullPointerException, SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Appends the string representation of a Joda {@link Instant} to an {@link Appendable}.
     * Uses the ISO-8601 timestamp format (e.g., {@code "2021-01-01T10:30:00.123Z"}).
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the Joda {@link Instant} to append; may be {@code null}
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
    public void appendTo(final Appendable appendable, final Instant x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(stringOf(x));
        }
    }

    /**
     * Writes a Joda {@link Instant} value to a {@link CharacterWriter}.
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
     * @param x      the Joda {@link Instant} to write; may be {@code null}
     * @param config the serialization configuration; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the selected date/time representation, quotation marks or null literal to {@code writer} fails
     */
    @SuppressWarnings("null")
    @Override
    public void serializeTo(final CharacterWriter writer, final Instant x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
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
                        writer.write(x.getMillis());

                        break;

                    case ISO_8601_DATE_TIME:
                        writer.write(x.toString(jodaISO8601DateTimeFT));

                        break;

                    case ISO_8601_TIMESTAMP:
                        writer.write(x.toString(jodaISO8601TimestampFT));

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
