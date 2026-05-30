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
import java.sql.Types;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Duration} values.
 * This class provides serialization, deserialization, and database operations for
 * {@link Duration} objects.
 *
 * <p>Durations are represented as their millisecond count ({@code long}) in both string form
 * and database storage. In the database, the corresponding SQL type is {@link Types#BIGINT}.
 *
 * @see AbstractType
 * @see Duration
 * @see Types#BIGINT
 */
public class DurationType extends AbstractType<Duration> {

    /** The type name constant for Duration type identification, equal to {@code "Duration"}. */
    public static final String DURATION = Duration.class.getSimpleName();

    /**
     * Package-private constructor for {@code DurationType}.
     * Instances are created by the {@code TypeFactory}.
     */
    DurationType() {
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
     * Duration values are stored as plain numeric strings (millisecond counts) and do not need quotes.
     *
     * @return {@code false}, always, because duration values are plain numbers in CSV
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a {@link Duration} to its string representation.
     * The duration is serialized as its millisecond count (e.g., {@code "5000"} for 5 seconds).
     *
     * @param x the {@link Duration} to convert; may be {@code null}
     * @return a string containing the millisecond count, or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final Duration x) {
        return (x == null) ? null : N.stringOf(x.toMillis());
    }

    /**
     * Parses a millisecond-count string back into a {@link Duration}.
     * The string must contain a valid {@code long} value (e.g., {@code "5000"} for 5 seconds).
     *
     * @param str the millisecond count as a string; may be {@code null} or empty
     * @return the corresponding {@link Duration}, or {@code null} if {@code str} is {@code null} or empty
     * @throws NumberFormatException if {@code str} is non-empty but does not contain a parsable {@code long}
     */
    @Override
    public Duration valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Duration.ofMillis(Numbers.toLong(str));
    }

    /**
     * Retrieves a {@link Duration} value from a {@link java.sql.ResultSet} at the specified column index.
     * The column is read as a {@code long} (millisecond count) via {@link java.sql.ResultSet#getLong(int)};
     * SQL {@code NULL} is detected with {@link java.sql.ResultSet#wasNull()} and returns {@code null}.
     *
     * @param rs          the {@link java.sql.ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return a {@link Duration} created from the stored millisecond count,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Duration get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long millis = rs.getLong(columnIndex);

        return rs.wasNull() ? null : Duration.ofMillis(millis);
    }

    /**
     * Retrieves a {@link Duration} value from a {@link java.sql.ResultSet} using the specified column label.
     * The column is read as a {@code long} (millisecond count) via {@link java.sql.ResultSet#getLong(String)};
     * SQL {@code NULL} is detected with {@link java.sql.ResultSet#wasNull()} and returns {@code null}.
     *
     * @param rs         the {@link java.sql.ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return a {@link Duration} created from the stored millisecond count,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Duration get(final ResultSet rs, final String columnName) throws SQLException {
        final long millis = rs.getLong(columnName);

        return rs.wasNull() ? null : Duration.ofMillis(millis);
    }

    /**
     * Sets a {@link Duration} value as a parameter in a {@link java.sql.PreparedStatement}.
     * The duration is stored as its millisecond count ({@code long}).
     * A {@code null} duration is stored as SQL {@code NULL} with JDBC type {@link Types#BIGINT}.
     *
     * @param stmt        the {@link java.sql.PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the {@link Duration} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter index is invalid
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
     * Sets a {@link Duration} value as a named parameter in a {@link java.sql.CallableStatement}.
     * The duration is stored as its millisecond count ({@code long}).
     * A {@code null} duration is stored as SQL {@code NULL} with JDBC type {@link Types#BIGINT}.
     *
     * @param stmt          the {@link java.sql.CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the {@link Duration} to set; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is not found
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
     * Appends a {@link Duration} value to an {@link Appendable} as its millisecond count.
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the {@link Duration} to append; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
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
     * Writes a {@link Duration} value to a {@link CharacterWriter} as its millisecond count,
     * using the writer's optimized {@code long}-write method.
     * If {@code x} is {@code null}, the literal {@code null} is written.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the {@link Duration} to write; may be {@code null}
     * @param config serialization configuration (not used for {@link Duration}); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Duration x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.toMillis());
        }
    }
}
