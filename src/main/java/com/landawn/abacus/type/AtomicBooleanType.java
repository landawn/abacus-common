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
import java.util.concurrent.atomic.AtomicBoolean;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.concurrent.atomic.AtomicBoolean} values.
 * Provides serialization, deserialization, and JDBC operations for {@code AtomicBoolean} instances.
 *
 * <p>String representation: {@code "true"} or {@code "false"} (same as {@link Boolean#toString()}).</p>
 * <p>JDBC mapping: stored and retrieved as a SQL {@code BOOLEAN} column
 * ({@link java.sql.Types#BOOLEAN}).</p>
 *
 * @see java.util.concurrent.atomic.AtomicBoolean
 */
public class AtomicBooleanType extends AbstractAtomicType<AtomicBoolean> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "AtomicBoolean"}).
     */
    public static final String ATOMIC_BOOLEAN = AtomicBoolean.class.getSimpleName();

    /**
     * Package-private constructor for {@code AtomicBooleanType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    AtomicBooleanType() {
        super(ATOMIC_BOOLEAN);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code AtomicBoolean.class}
     */
    @Override
    public Class<AtomicBoolean> javaType() {
        return AtomicBoolean.class;
    }

    /**
     * Converts an {@link java.util.concurrent.atomic.AtomicBoolean} value to its string representation.
     * The boolean value is read via {@link java.util.concurrent.atomic.AtomicBoolean#get()} and
     * converted to either {@code "true"} or {@code "false"}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code AtomicBoolean} to convert; may be {@code null}
     * @return {@code "true"} if the contained value is {@code true}, {@code "false"} if it is {@code false},
     *         or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final AtomicBoolean x) {
        return (x == null) ? null : (x.get() ? TRUE_STRING : FALSE_STRING);
    }

    /**
     * Parses a string and returns a new {@link java.util.concurrent.atomic.AtomicBoolean} containing
     * the parsed value. Leading and trailing whitespace is trimmed before parsing. The trimmed string
     * is interpreted using the type system's boolean parsing rules: single-character {@code "Y"},
     * {@code "y"}, or {@code "1"} yields {@code true}; multi-character inputs are interpreted via
     * {@link Boolean#valueOf(String)} (case-insensitive {@code "true"} yields {@code true}); any other
     * value yields {@code false}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null} or blank
     * @return a new {@code AtomicBoolean} containing the parsed boolean value,
     *         or {@code null} if {@code str} is {@code null}, empty, or blank
     * @see #valueOf(Object)
     * @see #stringOf(AtomicBoolean)
     */
    @Override
    public AtomicBoolean valueOf(final String str) {
        return Strings.isBlank(str) ? null : new AtomicBoolean(parseBoolean(str.trim()));
    }

    /**
     * Retrieves an {@link java.util.concurrent.atomic.AtomicBoolean} from a {@link java.sql.ResultSet}
     * at the specified column index.
     * The column value is read as a SQL {@code BOOLEAN} and wrapped in a new {@code AtomicBoolean}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the boolean value
     * @return a new {@code AtomicBoolean} wrapping the retrieved value, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public AtomicBoolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        final boolean value = rs.getBoolean(columnIndex);

        return rs.wasNull() ? null : new AtomicBoolean(value);
    }

    /**
     * Retrieves an {@link java.util.concurrent.atomic.AtomicBoolean} from a {@link java.sql.ResultSet}
     * using the specified column label.
     * The column value is read as a SQL {@code BOOLEAN} and wrapped in a new {@code AtomicBoolean}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return a new {@code AtomicBoolean} wrapping the retrieved value, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public AtomicBoolean get(final ResultSet rs, final String columnName) throws SQLException {
        final boolean value = rs.getBoolean(columnName);

        return rs.wasNull() ? null : new AtomicBoolean(value);
    }

    /**
     * Sets an {@link java.util.concurrent.atomic.AtomicBoolean} parameter on a
     * {@link java.sql.PreparedStatement} at the specified position.
     * If {@code x} is {@code null}, the parameter is set to SQL NULL
     * ({@link java.sql.Types#BOOLEAN}); otherwise the contained boolean value is used.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code AtomicBoolean} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicBoolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x.get());
        }
    }

    /**
     * Sets a named {@link java.util.concurrent.atomic.AtomicBoolean} parameter on a
     * {@link java.sql.CallableStatement}.
     * If {@code x} is {@code null}, the parameter is set to SQL NULL
     * ({@link java.sql.Types#BOOLEAN}); otherwise the contained boolean value is used.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code AtomicBoolean} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicBoolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x.get());
        }
    }

    /**
     * Appends an {@link java.util.concurrent.atomic.AtomicBoolean} value to an {@link Appendable}.
     * Appends {@code "null"} if {@code x} is {@code null}; otherwise appends {@code "true"} or {@code "false"}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target {@code Appendable}
     * @param x the {@code AtomicBoolean} value to append; may be {@code null}
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
    public void appendTo(final Appendable appendable, final AtomicBoolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x.get() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes an {@link java.util.concurrent.atomic.AtomicBoolean} value to a {@link CharacterWriter}.
     * Delegates to {@link #appendTo(Appendable, AtomicBoolean)}; {@code config} is not used.
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
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code AtomicBoolean} value to write; may be {@code null}
     * @param config the serialization configuration (unused for boolean values); may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final AtomicBoolean x, final JsonXmlSerConfig<?> config) throws IOException {
        appendTo(writer, x);
    }
}
