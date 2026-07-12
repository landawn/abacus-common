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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for java.time.Duration.
 * This class provides serialization, deserialization, and database access capabilities for Duration instances.
 * Durations are stored and transmitted as milliseconds for compatibility and efficiency.
 */
public class JdkDurationType extends AbstractType<Duration> {

    /** The type name constant for {@link Duration} (JDK) type identification. */
    public static final String DURATION = "JdkDuration";

    /**
     * Package-private constructor for JdkDurationType.
     * This constructor is called by the TypeFactory to create Duration type instances.
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
     * JDK Duration values are numeric and do not require quotes.
     *
     * @return {@code false}, as JDK Duration values do not require quoting in CSV format
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a Duration to its string representation.
     * The duration is represented as the total number of milliseconds.
     *
     * <p>The returned string is a serializable representation designed to be parsed back by {@link #valueOf(String)}
     * at millisecond precision. This is the key distinction from {@link Object#toString()}, whose result is not
     * guaranteed to be convertible back into the original value.</p>
     * <p><b>&#9888;&#65039;</b> Sub-millisecond precision is not preserved: for example, {@code Duration.ofNanos(1)}
     * serializes as {@code "0"}.</p>
     *
     * @param x the Duration to convert to string
     * @return the string representation of milliseconds, or {@code null} if the input is null
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Duration x) {
        return (x == null) ? null : N.stringOf(x.toMillis());
    }

    /**
     * Parses a string representation into a Duration.
     * The string should contain a number representing milliseconds.
     *
     * <p>This method parses the millisecond-precision string produced by {@code stringOf}. Strings produced by
     * {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string containing milliseconds to parse
     * @return the parsed Duration instance, or {@code null} if the input is {@code null} or empty
     * @see #valueOf(Object)
     * @see #stringOf(Duration)
     */
    @Override
    public Duration valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Duration.ofMillis(Numbers.toLong(str));
    }

    /**
     * Retrieves a Duration value from the specified column in a ResultSet.
     * The column value is read as a {@code long} representing milliseconds and converted to
     * a {@link Duration}.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the index of the column to read (1-based)
     * @return the Duration created from the milliseconds stored in the column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Duration get(final ResultSet rs, final int columnIndex) throws SQLException {
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
     * @throws SQLException if a database access error occurs or the columnName is not found
     */
    @Override
    public Duration get(final ResultSet rs, final String columnName) throws SQLException {
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
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Duration x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.toMillis());
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
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Duration x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.toMillis());
        }
    }

    /**
     * Appends the string representation of a Duration to an Appendable.
     * The duration is written as milliseconds.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the Appendable to write to
     * @param x the Duration to append
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
    public void appendTo(final Appendable appendable, final Duration x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.toMillis()));
        }
    }

    /**
     * Writes the character representation of a Duration to a CharacterWriter.
     * The duration is written as a numeric value (milliseconds) without quotes,
     * regardless of the serialization configuration.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Duration to write
     * @param config the serialization configuration (not used for Duration)
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Duration x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.toMillis());
        }
    }
}
