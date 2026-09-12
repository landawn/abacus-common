/*
 * Copyright (C) 2024 HaiYang Li
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
import java.sql.Types;
import java.time.Duration;
import java.time.format.DateTimeParseException;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for JDK {@link java.time.Duration} values.
 * This class provides serialization, deserialization, and database access for {@code Duration} instances.
 * Text uses the lossless ISO-8601 duration form, including nanoseconds. Legacy integer-millisecond
 * text remains readable. JDBC storage uses BIGINT milliseconds and rejects values that cannot be
 * represented exactly in that column type.
 *
 * @see java.time.Duration
 * @see DurationType
 */
public class JdkDurationType extends AbstractType<Duration> {

    /** The type name constant for {@link Duration} (JDK) type identification. */
    public static final String DURATION = "JdkDuration";

    /**
     * Package-private constructor for {@code JdkDurationType}.
     * Instances are created by the {@code TypeFactory}.
     */
    JdkDurationType() {
        super(DURATION);
    }

    /**
     * Indicates whether {@link Duration} values are comparable.
     * {@link Duration} implements {@link Comparable}, so this returns {@code true}.
     *
     * @return {@code true}, always, because {@link Duration} is {@link Comparable}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Duration.class}
     */
    @Override
    public Class<Duration> javaType() {
        return Duration.class;
    }

    /**
     * Indicates whether values of this type require quoting in CSV format.
     * ISO-8601 duration text has no CSV delimiters or quotation characters.
     *
     * @return {@code false}, as JDK Duration values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a Duration to its string representation.
     * The duration is represented by {@link Duration#toString()} in ISO-8601 format.
     *
     * <p>The returned string is a serializable representation designed to be parsed back by {@link #valueOf(String)}
     * without losing nanoseconds or overflowing a millisecond count.</p>
     *
     * @param x the {@code Duration} to convert to string
     * @return the ISO-8601 duration string, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Duration x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Parses a string representation into a Duration.
     * Accepts ISO-8601 duration text (for example, {@code PT0.000000001S}) or a legacy integer
     * millisecond count (for example, {@code 1000}). Legacy text is parsed with the {@link Numbers#toLong(String)}
     * grammar: an optional sign, an optional trailing {@code L}/{@code l} suffix and {@code 0x}/{@code #}
     * hexadecimal are accepted ({@code "1000L"} and {@code "0x3E8"} both yield {@code PT1S}); surrounding whitespace
     * is not.
     *
     * <p>Every representation produced by {@link Duration#toString()} round-trips exactly.</p>
     *
     * @param str the duration text to parse
     * @return the parsed Duration instance, or {@code null} if the input is {@code null} or empty
     * @throws NumberFormatException if legacy millisecond text is not a parsable {@code long}
     * @throws ArithmeticException if legacy millisecond text is an integer outside the {@code long} range
     *@throws DateTimeParseException if ISO-8601 duration text is invalid
     * @see #valueOf(Object)
     * @see #stringOf(Duration)
     */
    @Override
    public Duration valueOf(final String str) throws NumberFormatException, ArithmeticException, DateTimeParseException {
        if (Strings.isEmpty(str)) {
            return null;
        }

        return str.regionMatches(true, 0, "P", 0, 1) || str.regionMatches(true, 1, "P", 0, 1) ? Duration.parse(str) : Duration.ofMillis(Numbers.toLong(str));
    }

    /**
     * Retrieves a Duration value from the specified column in a ResultSet.
     * The column value is read as a {@code long} representing milliseconds and converted to
     * a {@link Duration}.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the Duration created from the milliseconds stored in the column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Duration get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        final long millis = rs.getLong(columnIndex);

        return rs.wasNull() ? null : Duration.ofMillis(millis);
    }

    /**
     * Retrieves a Duration value from the specified column in a ResultSet using the column label.
     * The column value is read as a {@code long} representing milliseconds and converted to
     * a {@link Duration}.
     *
     * @param rs the ResultSet to read from
     * @param columnName the label of the column to read
     * @return the Duration created from the milliseconds stored in the column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or the columnName is not found
     */
    @Override
    public Duration get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        final long millis = rs.getLong(columnName);

        return rs.wasNull() ? null : Duration.ofMillis(millis);
    }

    /**
     * Sets a Duration parameter in a PreparedStatement.
     * The Duration is converted to milliseconds for database storage.
     * If the Duration is {@code null}, SQL NULL is set.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Duration to set, or null
     * @throws ArithmeticException if the duration has sub-millisecond precision or its millisecond count overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Duration x) throws ArithmeticException, NullPointerException, SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, exactMillis(x));
        }
    }

    /**
     * Sets a Duration parameter in a CallableStatement using a parameter name.
     * The Duration is converted to milliseconds for database storage.
     * If the Duration is {@code null}, SQL NULL is set.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Duration to set, or null
     * @throws ArithmeticException if the duration has sub-millisecond precision or its millisecond count overflows
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Duration x) throws ArithmeticException, NullPointerException, SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.BIGINT);
        } else {
            stmt.setLong(parameterName, exactMillis(x));
        }
    }

    /**
     * @throws ArithmeticException if the duration has sub-millisecond precision or its millisecond count overflows a long
     */
    private static long exactMillis(final Duration duration) throws ArithmeticException {
        if (duration.getNano() % 1_000_000 != 0) {
            throw new ArithmeticException("Duration cannot be represented exactly as JDBC milliseconds: " + duration);
        }

        return duration.toMillis();
    }

    /**
     * Appends the string representation of a Duration to an Appendable.
     * The duration is written in ISO-8601 format with full nanosecond precision.
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * string form using the configured quotation character.
     *
     * @param appendable the Appendable to write to
     * @param x the Duration to append
     * @throws IOException if appending the ISO-8601 duration text or null literal to {@code appendable} fails
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
    public void appendTo(final Appendable appendable, final Duration x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
        }
    }

    /**
     * Writes the character representation of a Duration to a CharacterWriter.
     * The duration is written as lossless ISO-8601 text, quoted according to the configuration.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}, applying string quotation/escaping configuration.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Duration to write; may be {@code null}
     * @param config the serialization configuration; {@code null} means unquoted text
     * @throws IOException if writing the duration text, configured quotation or null literal to {@code writer} fails
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Duration x, final JsonXmlSerConfig<?> config) throws IOException {
        super.serializeTo(writer, x, config);
    }
}
