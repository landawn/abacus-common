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
import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for {@code String} type handling in the type system.
 * <p>
 * This class provides the foundation for {@code String} serialization/deserialization,
 * JDBC read/write operations, and character-based I/O operations.
 * Special handling is provided for {@link java.io.Reader} and {@link java.sql.Clob} input objects
 * during {@link #valueOf(Object)} conversion.
 * </p>
 *
 * @see StringType
 * @see AbstractCharSequenceType
 */
public abstract class AbstractStringType extends AbstractCharSequenceType<String> {

    /**
     * Constructs an {@code AbstractStringType} with the specified type name.
     *
     * @param typeName the name of the string type (e.g., "String")
     */
    protected AbstractStringType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the {@code Class} object representing the {@code String} class.
     *
     * @return {@code String.class}
     */
    @Override
    public Class<String> javaType() {
        return String.class;
    }

    /**
     * Returns {@code true} because this type represents a {@code String} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isString() {
        return true;
    }

    /**
     * Returns the input string unchanged.
     * Since the value is already a {@code String}, no conversion is needed.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param str the {@code String} value to convert, may be {@code null}
     * @return the same {@code String} value, or {@code null} if input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final String str) {
        return str;
    }

    /**
     * Returns the input string unchanged.
     * Since the value is already a {@code String}, no conversion is needed.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string value, may be {@code null}
     * @return the same {@code String} value, or {@code null} if input is {@code null}
     * @see #valueOf(Object)
     * @see #stringOf(String)
     */
    @Override
    public String valueOf(final String str) {
        return str;
    }

    /**
     * Creates a {@code String} from a subset of the given character array.
     *
     * @param cbuf the character array, may be {@code null}
     * @param offset the starting position in the array (0-based)
     * @param len the number of characters to include
     * @return a new {@code String} from the specified characters, {@code null} if {@code cbuf} is
     *         {@code null}, or an empty string if {@code cbuf} is empty or {@code len} is {@code 0}
     */
    @Override
    public String valueOf(final char[] cbuf, final int offset, final int len) {
        return cbuf == null ? null : ((cbuf.length == 0 || len == 0) ? Strings.EMPTY : String.valueOf(cbuf, offset, len));
    }

    /**
     * Converts an {@code Object} to a {@code String} value.
     * <p>
     * Special handling is applied for the following input types:
     * </p>
     * <ul>
     *   <li>{@code null} — returns {@code null}</li>
     *   <li>{@link java.io.Reader} — reads all content and returns it as a {@code String}</li>
     *   <li>{@link java.sql.Clob} — extracts character data via {@link java.sql.Clob#getSubString}</li>
     *   <li>All other types — uses the type-specific {@code stringOf} conversion</li>
     * </ul>
     *
     * @param obj the object to convert, may be {@code null}
     * @return the {@code String} representation of the object, or {@code null} if {@code obj} is {@code null}
     * @throws UncheckedSQLException if a SQL error occurs while reading from a {@code Clob}
     * @throws UnsupportedOperationException if a {@code Clob} is too large to convert (exceeds {@link Integer#MAX_VALUE} characters)
     */
    @SuppressFBWarnings
    @Override
    public String valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Reader reader) {
            return IOUtil.readAllToString(reader);
        } else if (obj instanceof Clob clob) {
            RuntimeException primaryException = null;

            try {
                final long len = clob.length();
                if (len > Integer.MAX_VALUE) {
                    throw new UnsupportedOperationException("Clob too large to convert to String: " + len + " characters");
                }
                return clob.getSubString(1, (int) len);
            } catch (final SQLException e) {
                primaryException = new UncheckedSQLException(e);
                throw primaryException;
            } catch (final RuntimeException e) {
                primaryException = e;
                throw primaryException;
            } finally {
                try {
                    clob.free();
                } catch (final SQLException e) {
                    final UncheckedSQLException freeException = new UncheckedSQLException(e);

                    if (primaryException != null) {
                        primaryException.addSuppressed(freeException);
                    } else {
                        throw freeException; //NOSONAR
                    }
                }
            }
        } else {
            return valueOf(Type.<Object> of(obj.getClass()).stringOf(obj));
        }
    }

    /**
     * Retrieves a {@code String} value from a {@code ResultSet} at the specified column index.
     *
     * @param rs the {@code ResultSet} to retrieve the value from
     * @param columnIndex the column index (1-based) of the value to retrieve
     * @return the {@code String} value at the specified column, or {@code null} if the value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public String get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /**
     * Retrieves a {@code String} value from a {@code ResultSet} using the specified column label.
     *
     * @param rs the {@code ResultSet} to retrieve the value from
     * @param columnName the label for the column specified with the SQL {@code AS} clause,
     *                   or the column name if no {@code AS} clause was specified
     * @return the {@code String} value in the specified column, or {@code null} if the value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnName} is invalid
     */
    @Override
    public String get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    /**
     * Sets a {@code String} parameter in a {@code PreparedStatement} at the specified position.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code String} value to set, may be {@code null}
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final String x) throws SQLException {
        stmt.setString(columnIndex, x);
    }

    /**
     * Sets a named {@code String} parameter in a {@code CallableStatement}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code String} value to set, may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final String x) throws SQLException {
        stmt.setString(parameterName, x);
    }

    /**
     * Appends a {@code String} value to an {@code Appendable}.
     * Appends the literal {@code "null"} string if the value is {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@code Appendable} to append to
     * @param x the {@code String} value to append, may be {@code null}
     * @throws IOException if an I/O error occurs
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
    public void appendTo(final Appendable appendable, final String x) throws IOException {
        appendable.append(Objects.requireNonNullElse(x, NULL_STRING));
    }

    /**
     * Writes a {@code String} value to a {@code CharacterWriter} with optional quotation.
     * <p>
     * If the configuration specifies {@code writeNullStringAsEmpty} and the value is
     * {@code null}, writes an empty string instead. Otherwise writes {@code "null"} for
     * {@code null} values. Applies string quotation marks from {@code config} if specified.
     * </p>
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
     * @param x the {@code String} value to write, may be {@code null}
     * @param config the serialization configuration controlling quotation and null handling,
     *               may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, String x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullStringAsEmpty() ? Strings.EMPTY : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getStringQuotation();

            if (ch == 0) {
                writer.writeCharacter(x);
            } else {
                writer.write(ch);
                writer.writeCharacter(x);
                writer.write(ch);
            }
        }
    }
}
