/*
 * Copyright (C) 2019 HaiYang Li
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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Boolean} values that are stored as single-character strings
 * ({@code 'Y'}/{@code 'N'}) in the database.
 * Text conversion maps {@code Boolean.TRUE} to {@code "Y"} and {@code Boolean.FALSE} or
 * {@code null} to {@code "N"}, providing compatibility with legacy database schemas that
 * represent boolean flags as {@code CHAR(1)} or {@code VARCHAR(1)} columns.
 *
 * <p>JDBC mapping: values are read and written as {@code VARCHAR} strings
 * ({@link java.sql.Types#VARCHAR}). A {@code null} value is bound as SQL {@code NULL}, while SQL
 * {@code NULL} values are read back as {@code Boolean.FALSE}.</p>
 *
 * <p>Reading follows the library-wide rule, which includes the {@code Y}/{@code N} convention: an {@code Integer},
 * {@code Short} or {@code Byte} {@code 1} and the texts {@code "1"}, {@code "y"}, {@code "Y"} and {@code "true"} (any
 * case) are {@code true}; an {@code Integer}, {@code Short} or {@code Byte} {@code 0} or {@code -1} and the texts
 * {@code "0"}, {@code "-1"}, {@code "n"}, {@code "N"} and {@code "false"} (any case) are {@code false}; any other
 * number, a {@code Number} of any other class ({@code 1L}, {@code 1.0}), and any other text are rejected with an
 * {@link IllegalArgumentException}.</p>
 */
@SuppressWarnings("java:S2160")
public final class BooleanCharType extends AbstractType<Boolean> {

    private static final String TYPE_NAME = "BooleanChar";
    private static final String Y = "Y";
    private static final String N = "N";

    /**
     * Package-private constructor for {@code BooleanCharType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BooleanCharType() {
        super(TYPE_NAME);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code Boolean.class}
     */
    @Override
    public Class<Boolean> javaType() {
        return Boolean.class;
    }

    /**
     * Returns the default value for the {@code BooleanChar} type.
     *
     * @return {@link Boolean#FALSE}
     */
    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }

    /**
     * Indicates whether values of this type require quoting in CSV output.
     * Single-character {@code Y}/{@code N} values do not require quoting.
     *
     * @return {@code false} always
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a {@link Boolean} to its single-character string representation.
     * Maps {@code true} to {@code "Y"} and {@code false} / {@code null} to {@code "N"}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null boolean values round-trip exactly; {@code null} is encoded as {@code "N"}
     * and decodes as {@link Boolean#FALSE}. This is the key distinction from {@link Object#toString()}, whose result is
     * not guaranteed to be convertible back into the original value.</p>
     *
     * @param b the {@code Boolean} value to convert; may be {@code null}
     * @return {@code "Y"} if {@code b} is {@code Boolean.TRUE}, {@code "N"} otherwise
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null || !b) ? N : Y;
    }

    /**
     * Parses a string to a {@link Boolean} with the library-wide rule for text, which includes the {@code Y}/{@code N}
     * convention: {@code "Y"}, {@code "y"}, {@code "1"} and {@code "true"} (any case) yield {@code true}; {@code "N"},
     * {@code "n"}, {@code "0"}, {@code "-1"} and {@code "false"} (any case) yield {@code false}; surrounding padding is
     * removed first; any other text is rejected. {@code null} and blank text yield {@code false}, the default value.
     *
     * <p>This method parses strings produced by {@code stringOf} back into values of this type. Non-null boolean
     * values round-trip exactly; the {@code "N"} produced for {@code null} decodes as {@link Boolean#FALSE}. Strings
     * produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null}
     * @return the parsed value, or {@link Boolean#FALSE} if {@code str} is {@code null} or blank
     * @throws IllegalArgumentException if {@code str} is not blank and, without its padding, is none of the accepted texts
     * @see #valueOf(Object)
     * @see #stringOf(Boolean)
     */
    @Override
    public Boolean valueOf(final String str) throws IllegalArgumentException {
        return Strings.isBlank(str) ? Boolean.FALSE : parseBoolean(str);
    }

    /**
     * Converts an object to a {@link Boolean} value.
     * Booleans are returned as-is. An {@code Integer}, {@code Short} or {@code Byte} returns {@code true} when its
     * value is {@code 1} and {@code false} when it is {@code 0} or {@code -1}; any other value ({@code 2}), and a
     * {@code Number} of any other class ({@code 1L}, {@code 1.0}, {@code BigDecimal}), is rejected. Other objects use their
     * string form, read by {@link #valueOf(String)}.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the Boolean value, or {@code false} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if {@code obj} is an {@code Integer}, {@code Short} or {@code Byte} other than
     *         {@code -1}, {@code 0} or {@code 1}, a {@code Number} of any other class, or an object whose string form is
     *         not blank and is none of the texts {@link #valueOf(String)} accepts
     * @see #valueOf(String)
     */
    @Override
    public Boolean valueOf(final Object obj) throws IllegalArgumentException {
        // Without this override the inherited valueOf(Object) stringified a Boolean to
        // "true"/"false", which the Y/N parser mapped to FALSE - even for Boolean.TRUE.
        if (obj == null) {
            return Boolean.FALSE;
        } else if (obj instanceof Boolean b) {
            return b;
        } else if (obj instanceof Number n) {
            return booleanOf(n);
        }

        return valueOf(obj.toString());
    }

    /**
     * Indicates that this type supports comparison operations, like every other Boolean handler.
     *
     * @return {@code true}
     */
    @Override
    public boolean isComparable() {
        return true;
    }

    /**
     * Parses a character array sub-sequence to a {@link Boolean}, exactly as {@link #valueOf(String)} parses the same
     * text; a {@code null} {@code cbuf} or a {@code length} of {@code 0} yields {@link Boolean#FALSE}.
     *
     * @param cbuf the character array; may be {@code null}
     * @param offset the 0-based start position within {@code cbuf}
     * @param length the number of characters to examine
     * @return the parsed value, or {@link Boolean#FALSE} if the region is empty or blank
     * @throws IndexOutOfBoundsException if {@code cbuf} is non-null, {@code length} is positive, and the region is outside the array.
     * @throws IllegalArgumentException if the region is not blank and, without its padding, is none of the accepted texts
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int length) throws IndexOutOfBoundsException, IllegalArgumentException {
        if (cbuf == null || length == 0) {
            return Boolean.FALSE;
        }

        final Boolean token = booleanToken(cbuf, offset, length);

        // Padded, blank or unrecognized text is read by valueOf(String); "Y" and "N" are read in place.
        return token != null ? token : valueOf(new String(cbuf, offset, length));
    }

    /**
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} at the specified column index.
     * The column value is read as a {@code VARCHAR} string and converted via {@link #valueOf(String)}.
     * SQL NULL values are treated as {@code Boolean.FALSE}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the Y/N value
     * @return the column value read by {@link #valueOf(String)} ({@code "Y"} is {@code true}, {@code "N"} is
     *         {@code false}), or {@link Boolean#FALSE} for SQL NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws IllegalArgumentException if the column value is not blank and is none of the texts {@link #valueOf(String)} accepts
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException, IllegalArgumentException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} using the specified column label.
     * The column value is read as a {@code VARCHAR} string and converted via {@link #valueOf(String)}.
     * SQL NULL values are treated as {@code Boolean.FALSE}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the column value read by {@link #valueOf(String)} ({@code "Y"} is {@code true}, {@code "N"} is
     *         {@code false}), or {@link Boolean#FALSE} for SQL NULL
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     * @throws IllegalArgumentException if the column value is not blank and is none of the texts {@link #valueOf(String)} accepts
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException, IllegalArgumentException {
        return valueOf(rs.getString(columnName));
    }

    /**
     * Sets a {@link Boolean} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * Converts {@code true} to the string {@code "Y"} and {@code false} to {@code "N"}.
     * A {@code null} value is stored as SQL NULL ({@link java.sql.Types#VARCHAR}).
     *
     * @param statement the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code Boolean} value to set; {@code null} is stored as SQL NULL
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final Boolean x) throws NullPointerException, SQLException {
        if (x == null) {
            statement.setNull(columnIndex, java.sql.Types.VARCHAR);
        } else {
            statement.setString(columnIndex, x ? Y : N);
        }
    }

    /**
     * Sets a named {@link Boolean} parameter on a {@link java.sql.CallableStatement}.
     * Converts {@code true} to the string {@code "Y"} and {@code false} to {@code "N"}.
     * A {@code null} value is stored as SQL NULL ({@link java.sql.Types#VARCHAR}).
     *
     * @param statement the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code Boolean} value to set; {@code null} is stored as SQL NULL
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final Boolean x) throws NullPointerException, SQLException {
        if (x == null) {
            statement.setNull(parameterName, java.sql.Types.VARCHAR);
        } else {
            statement.setString(parameterName, x ? Y : N);
        }
    }

    /**
     * Appends a {@link Boolean} value to an {@link Appendable} as a Y/N string.
     * Appends {@code "Y"} if {@code x} is {@code true}; appends {@code "N"} otherwise
     * (including when {@code x} is {@code null}).
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target {@code Appendable}
     * @param x the {@code Boolean} value to append; may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @implNote
     * This implementation appends {@code stringOf(x)} to {@code appendable}: {@code "Y"} for {@code true} and
     * {@code "N"} for {@code false} or {@code null}. The appended text is therefore identical to {@code stringOf(x)}
     * and round-trips non-null values through {@link #valueOf(String)}; a null value decodes as {@code false}.
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws NullPointerException, IOException {
        appendable.append(stringOf(x));
    }

    /**
     * Writes a {@link Boolean} value to a {@link CharacterWriter} as a Y/N string.
     * If {@code config} specifies a non-zero character quotation character, the value is wrapped
     * in that quotation character; otherwise the raw {@code "Y"} or {@code "N"} string is written.
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
     * @param x the {@code Boolean} value to write; may be {@code null}
     * @param config the serialization configuration; if non-{@code null} and its
     *               {@link com.landawn.abacus.parser.JsonXmlSerConfig#getCharQuotation()} is non-zero,
     *               the value is wrapped in that character; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Boolean x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        final char ch = config == null ? 0 : config.getCharQuotation();

        if (ch == 0) {
            writer.write(stringOf(x));
        } else {
            writer.write(ch);
            writer.write(stringOf(x));
            writer.write(ch);
        }
    }
}
