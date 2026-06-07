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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for {@code int} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code int} values,
 * including string/character-array parsing, JDBC read/write operations, and serialization.
 * This class uses {@code Number} as its generic type parameter so that both the primitive
 * {@code int} type and the {@code Integer} wrapper type can share this implementation.
 * Concrete subclasses cover each of those two variants.
 * </p>
 *
 * @see IntegerType
 * @see PrimitiveIntType
 */
public abstract class AbstractIntegerType extends NumberType<Number> {

    /**
     * Constructs an {@code AbstractIntegerType} with the specified type name.
     *
     * @param typeName the name of the integer type (e.g., "Integer", "int")
     */
    protected AbstractIntegerType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a {@code Number} value to its string representation as an {@code int}.
     * <p>
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the {@code int} value obtained via {@link Number#intValue()}.
     * </p>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code Number} value to convert
     * @return the string representation of the {@code int} value, or {@code null} if input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.intValue());
    }

    /**
     * Converts a string to an {@code Integer} value.
     * <p>
     * This method handles various string formats:
     * </p>
     * <ul>
     *   <li>Empty or {@code null} strings return the default value.</li>
     *   <li>If parsing fails and the string ends with {@code 'l'}, {@code 'L'}, {@code 'f'},
     *       {@code 'F'}, {@code 'd'}, or {@code 'D'}, the suffix is stripped and parsing is retried.</li>
     *   <li>Valid numeric strings are parsed to {@code Integer} values.</li>
     * </ul>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert, may be {@code null}
     * @return the {@code Integer} value, or the default value if {@code str} is empty or {@code null}
     * @throws NumberFormatException if the string cannot be parsed as an {@code int}
     * @see #valueOf(Object)
     * @see #stringOf(Number)
     */
    @Override
    public Integer valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return (Integer) defaultValue();
        }

        try {
            return Numbers.toInt(str);
        } catch (final NumberFormatException e) {
            if (str.length() > 1) {
                final char ch = str.charAt(str.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Numbers.toInt(str.substring(0, str.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     * Converts a character array to an {@code Integer} value.
     * Delegates to the {@link #parseInt(char[], int, int)} method for parsing.
     *
     * @param cbuf the character array to convert, may be {@code null}
     * @param offset the starting position in the array (0-based)
     * @param len the number of characters to read
     * @return the {@code Integer} value, or the default value if {@code cbuf} is {@code null} or {@code len} is {@code 0}
     * @throws NumberFormatException if the character sequence cannot be parsed as an {@code int}
     */
    @Override
    public Integer valueOf(final char[] cbuf, final int offset, final int len) {
        return ((cbuf == null) || (len == 0)) ? ((Integer) defaultValue()) : (Integer) parseInt(cbuf, offset, len);
    }

    /**
     * Returns {@code true} because this type represents an {@code int}/{@code Integer} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isInteger() {
        return true;
    }

    /**
     * Retrieves an {@code int} value from a {@code ResultSet} at the specified column index.
     * Uses {@link java.sql.ResultSet#getInt(int)} which returns {@code 0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the {@code int} value at the specified column, or {@code 0} if SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public Integer get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getInt(columnIndex);
    }

    /**
     * Retrieves an {@code int} value from a {@code ResultSet} using the specified column label.
     * Uses {@link java.sql.ResultSet#getInt(String)} which returns {@code 0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the {@code int} value at the specified column, or {@code 0} if SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnName} is not found
     */
    @Override
    public Integer get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getInt(columnName);
    }

    /**
     * Sets an {@code int} parameter in a {@code PreparedStatement} at the specified position.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to an {@code int} value.
     * </p>
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Number} value to set as {@code int}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x.intValue());
        }
    }

    /**
     * Sets an {@code int} parameter in a {@code CallableStatement} using the specified parameter name.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to an {@code int} value.
     * </p>
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Number} value to set as {@code int}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x.intValue());
        }
    }

    /**
     * Appends the string representation of an {@code int} value to an {@code Appendable}.
     * Writes {@code "null"} if the value is {@code null}, otherwise writes the numeric value.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Number} value to append as {@code int}
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
    public void appendTo(final Appendable appendable, final Number x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(N.stringOf(x.intValue()));
        }
    }

    /**
     * Writes an {@code int} value to a {@code CharacterWriter} with optional configuration.
     * <p>
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes {@code 0} instead of {@code null}.
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
     * @param x the {@code Number} value to write as {@code int}
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serializeTo(final CharacterWriter writer, Number x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullNumberAsZero() ? Numbers.INTEGER_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.intValue());
        }
    }
}
