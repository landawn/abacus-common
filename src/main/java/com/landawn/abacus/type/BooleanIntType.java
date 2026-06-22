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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

/**
 * Type handler for {@link Boolean} values that are stored as integers ({@code 1}/{@code 0})
 * in the database.
 * Maps {@code Boolean.TRUE} to {@code 1} and {@code Boolean.FALSE} / {@code null} to {@code 0},
 * providing compatibility with database schemas that represent boolean flags as integer columns.
 *
 * <p>JDBC mapping: values are stored via {@link java.sql.PreparedStatement#setInt} and
 * retrieved via {@link java.sql.ResultSet#getInt}. Any positive integer value is treated as
 * {@code true}; zero or negative values are {@code false}. SQL NULL maps to {@code null} on read
 * and is stored as SQL NULL ({@link java.sql.Types#INTEGER}) on write.</p>
 */
@SuppressWarnings("java:S2160")
public final class BooleanIntType extends AbstractType<Boolean> {

    private static final String TYPE_NAME = "BooleanInt";
    private static final String _0 = "0";
    private static final String _1 = "1";

    /**
     * Package-private constructor for {@code BooleanIntType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BooleanIntType() {
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
     * Returns the default value for the {@code BooleanInt} type.
     *
     * @return {@link Boolean#FALSE}
     */
    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }

    /**
     * Indicates whether values of this type require quoting in CSV output.
     * Integer {@code 0}/{@code 1} values are numeric and do not require quoting.
     *
     * @return {@code false} always
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Converts a {@link Boolean} to its integer string representation.
     * Maps {@code true} to {@code "1"} and {@code false} / {@code null} to {@code "0"}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param b the {@code Boolean} value to convert; may be {@code null}
     * @return {@code "1"} if {@code b} is {@link Boolean#TRUE}, {@code "0"} otherwise
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null || !b) ? _0 : _1;
    }

    /**
     * Parses a string to a {@link Boolean} using the {@code 1}/{@code 0} convention.
     * Only the exact string {@code "1"} yields {@code true}; any other value (including
     * {@code null}) yields {@code false}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse (typically {@code "0"} or {@code "1"}); may be {@code null}
     * @return {@link Boolean#TRUE} if {@code str} equals {@code "1"},
     *         {@link Boolean#FALSE} otherwise
     * @see #valueOf(Object)
     * @see #stringOf(Boolean)
     */
    @Override
    public Boolean valueOf(final String str) {
        return _1.equals(str) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Converts an object to a {@link Boolean} value.
     * Booleans are returned as-is and numbers map via {@code > 0} (matching {@code get(ResultSet)});
     * other objects go through their string form using the {@code 1}/{@code 0} convention.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the Boolean value, or {@code false} if {@code obj} is {@code null}
     * @see #valueOf(String)
     */
    @Override
    public Boolean valueOf(final Object obj) {
        // Without this override the inherited valueOf(Object) stringified a Boolean to
        // "true"/"false", which the 1/0 parser mapped to FALSE - even for Boolean.TRUE; a
        // Number went through its decimal string, disagreeing with the JDBC get() path.
        if (obj == null) {
            return Boolean.FALSE;
        } else if (obj instanceof Boolean b) {
            return b;
        } else if (obj instanceof Number n) {
            return n.longValue() > 0;
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
     * Parses a character array sub-sequence to a {@link Boolean}.
     * Returns {@link Boolean#TRUE} only if {@code len} is {@code 1} and the character at
     * {@code cbuf[offset]} is {@code '1'}; returns {@link Boolean#FALSE} otherwise,
     * including when {@code cbuf} is {@code null} or {@code len} is {@code 0}.
     *
     * @param cbuf the character array; may be {@code null}
     * @param offset the 0-based start position within {@code cbuf}
     * @param len the number of characters to examine
     * @return {@link Boolean#TRUE} if the single character is {@code '1'},
     *         {@link Boolean#FALSE} otherwise
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        return (cbuf == null || len == 0) ? Boolean.FALSE : ((len == 1 && (cbuf[offset] == '1')) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} at the specified column index.
     * The column value is read as a SQL {@code INTEGER}. Any positive value maps to {@code true};
     * zero or negative values map to {@code false}. SQL NULL maps to {@code null}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the integer value
     * @return {@link Boolean#TRUE} if the integer is {@code > 0},
     *         {@link Boolean#FALSE} if it is {@code <= 0},
     *         or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        final int value = rs.getInt(columnIndex);

        return rs.wasNull() ? null : value > 0;
    }

    /**
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} using the specified column label.
     * The column value is read as a SQL {@code INTEGER}. Any positive value maps to {@code true};
     * zero or negative values map to {@code false}. SQL NULL maps to {@code null}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return {@link Boolean#TRUE} if the integer is {@code > 0},
     *         {@link Boolean#FALSE} if it is {@code <= 0},
     *         or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnName) throws SQLException {
        final int value = rs.getInt(columnName);

        return rs.wasNull() ? null : value > 0;
    }

    /**
     * Sets a {@link Boolean} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * Converts {@code true} to the integer {@code 1} and {@code false} to {@code 0}.
     * A {@code null} value is stored as SQL NULL ({@link java.sql.Types#INTEGER}).
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code Boolean} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x ? 1 : 0);
        }
    }

    /**
     * Sets a named {@link Boolean} parameter on a {@link java.sql.CallableStatement}.
     * Converts {@code true} to the integer {@code 1} and {@code false} to {@code 0}.
     * A {@code null} value is stored as SQL NULL ({@link java.sql.Types#INTEGER}).
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code Boolean} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x ? 1 : 0);
        }
    }

    /**
     * Appends a {@link Boolean} value to an {@link Appendable} as a 0/1 string.
     * Appends {@code "1"} if {@code x} is {@code true}; appends {@code "0"} otherwise
     * (including when {@code x} is {@code null}).
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target {@code Appendable}
     * @param x the {@code Boolean} value to append; may be {@code null}
     * @throws IOException if an I/O error occurs during appending
     * @implNote
     * This implementation appends {@code stringOf(x)} to {@code appendable}: {@code "1"} for {@code true} and
     * {@code "0"} for {@code false} or {@code null}. The appended text is therefore identical to {@code stringOf(x)}
     * and round-trips through {@link #valueOf(String)}.
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws IOException {
        appendable.append(stringOf(x));
    }

    /**
     * Writes a {@link Boolean} value to a {@link CharacterWriter} as a {@code 0}/{@code 1} string.
     * If {@code config} specifies a non-zero character quotation character, the value is wrapped
     * in that quotation character; otherwise the raw {@code "1"} or {@code "0"} string is written.
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
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final Boolean x, final JsonXmlSerConfig<?> config) throws IOException {
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
