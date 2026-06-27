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
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.concurrent.atomic.AtomicLong} values.
 * Provides serialization, deserialization, and JDBC operations for {@code AtomicLong} instances.
 *
 * <p>String representation: the decimal string of the contained {@code long} value.</p>
 * <p>JDBC mapping: stored and retrieved as a SQL {@code BIGINT} column
 * ({@link java.sql.Types#BIGINT}).</p>
 *
 * @see java.util.concurrent.atomic.AtomicLong
 */
public class AtomicLongType extends AbstractAtomicType<AtomicLong> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "AtomicLong"}).
     */
    public static final String ATOMIC_LONG = AtomicLong.class.getSimpleName();

    /**
     * Package-private constructor for {@code AtomicLongType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    AtomicLongType() {
        super(ATOMIC_LONG);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code AtomicLong.class}
     */
    @Override
    public Class<AtomicLong> javaType() {
        return AtomicLong.class;
    }

    /**
     * Retrieves an {@link java.util.concurrent.atomic.AtomicLong} from a {@link java.sql.ResultSet}
     * at the specified column index.
     * The column value is read as a SQL {@code BIGINT} and wrapped in a new {@code AtomicLong}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the long value
     * @return a new {@code AtomicLong} wrapping the retrieved value, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public AtomicLong get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long value = rs.getLong(columnIndex);

        return rs.wasNull() ? null : new AtomicLong(value);
    }

    /**
     * Retrieves an {@link java.util.concurrent.atomic.AtomicLong} from a {@link java.sql.ResultSet}
     * using the specified column label.
     * The column value is read as a SQL {@code BIGINT} and wrapped in a new {@code AtomicLong}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return a new {@code AtomicLong} wrapping the retrieved value, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public AtomicLong get(final ResultSet rs, final String columnName) throws SQLException {
        final long value = rs.getLong(columnName);

        return rs.wasNull() ? null : new AtomicLong(value);
    }

    /**
     * Sets an {@link java.util.concurrent.atomic.AtomicLong} parameter on a
     * {@link java.sql.PreparedStatement} at the specified position.
     * If {@code x} is {@code null}, the parameter is set to SQL NULL
     * ({@link java.sql.Types#BIGINT}); otherwise the contained long value is used.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code AtomicLong} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicLong x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.get());
        }
    }

    /**
     * Sets a named {@link java.util.concurrent.atomic.AtomicLong} parameter on a
     * {@link java.sql.CallableStatement}.
     * If {@code x} is {@code null}, the parameter is set to SQL NULL
     * ({@link java.sql.Types#BIGINT}); otherwise the contained long value is used.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code AtomicLong} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicLong x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.get());
        }
    }

    /**
     * Converts an {@link java.util.concurrent.atomic.AtomicLong} to its decimal string representation.
     * The contained {@code long} value is obtained via {@link java.util.concurrent.atomic.AtomicLong#get()}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code AtomicLong} to convert; may be {@code null}
     * @return the decimal string representation of the contained value,
     *         or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final AtomicLong x) {
        return (x == null) ? null : N.stringOf(x.get());
    }

    /**
     * Parses a decimal string and returns a new {@link java.util.concurrent.atomic.AtomicLong}
     * containing the parsed value. Leading and trailing whitespace is trimmed before parsing.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the decimal string to parse; may be {@code null} or empty
     * @return a new {@code AtomicLong} containing the parsed value,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @throws NumberFormatException if {@code str} cannot be parsed as a valid {@code long}
     * @see #valueOf(Object)
     * @see #stringOf(AtomicLong)
     */
    @Override
    public AtomicLong valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new AtomicLong(Long.parseLong(str.trim()));
    }

    /**
     * Appends an {@link java.util.concurrent.atomic.AtomicLong} value to an {@link Appendable}.
     * Appends {@code "null"} if {@code x} is {@code null}; otherwise appends the decimal string of
     * the contained long value (equivalent to {@link java.util.concurrent.atomic.AtomicLong#toString()}).
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the target {@code Appendable}
     * @param x the {@code AtomicLong} value to append; may be {@code null}
     * @throws IOException if an I/O error occurs during appending
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
    public void appendTo(final Appendable appendable, final AtomicLong x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
        }
    }

    /**
     * Writes an {@link java.util.concurrent.atomic.AtomicLong} value to a {@link CharacterWriter}.
     * If {@code x} is {@code null}, writes the literal {@code "null"} character array unless
     * {@code config.isWriteNullNumberAsZero()} is set (in which case {@code 0} is written); otherwise writes the
     * contained long value via the writer's optimized {@code write(long)} method. When
     * {@code config.isWriteLongAsString()} is set with a non-zero {@code stringQuotation}, the value is wrapped
     * in that quotation character.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code AtomicLong} value to write; may be {@code null}
     * @param config the serialization configuration (honors {@code writeNullNumberAsZero} and
     *               {@code writeLongAsString}/{@code stringQuotation}); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final AtomicLong x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null && !(config != null && config.isWriteNullNumberAsZero())) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final long value = x == null ? 0L : x.get();

            if (config != null && config.isWriteLongAsString() && config.getStringQuotation() != 0) {
                final char ch = config.getStringQuotation();

                writer.write(ch);
                writer.write(value);
                writer.write(ch);
            } else {
                writer.write(value);
            }
        }
    }
}
