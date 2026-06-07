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

/**
 * Type handler for {@link Boolean} values that are stored as single-character strings
 * ({@code 'Y'}/{@code 'N'}) in the database.
 * Maps {@code Boolean.TRUE} to the string {@code "Y"} and {@code Boolean.FALSE} / {@code null}
 * to the string {@code "N"}, providing compatibility with legacy database schemas that
 * represent boolean flags as {@code CHAR(1)} or {@code VARCHAR(1)} columns.
 *
 * <p>JDBC mapping: values are read and written as {@code VARCHAR} strings
 * ({@link java.sql.Types#VARCHAR}). SQL NULL values are read back as {@code Boolean.FALSE}.</p>
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
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
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
     * Parses a string to a {@link Boolean} using the {@code Y}/{@code N} convention.
     * The comparison is case-insensitive; {@code "Y"} or {@code "y"} yields {@code true}.
     * Any other value (including {@code null}) yields {@code false}.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the string to parse; may be {@code null}
     * @return {@link Boolean#TRUE} if {@code str} equals {@code "Y"} (case-insensitive),
     *         {@link Boolean#FALSE} otherwise
     * @see #valueOf(Object)
     * @see #stringOf(Boolean)
     */
    @Override
    public Boolean valueOf(final String str) {
        return Y.equalsIgnoreCase(str) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Parses a character array sub-sequence to a {@link Boolean}.
     * Returns {@link Boolean#TRUE} only if {@code len} is {@code 1} and the character at
     * {@code cbuf[offset]} is {@code 'Y'} or {@code 'y'}; returns {@link Boolean#FALSE} otherwise,
     * including when {@code cbuf} is {@code null} or {@code len} is {@code 0}.
     *
     * @param cbuf the character array; may be {@code null}
     * @param offset the 0-based start position within {@code cbuf}
     * @param len the number of characters to examine
     * @return {@link Boolean#TRUE} if the single character is {@code 'Y'} or {@code 'y'},
     *         {@link Boolean#FALSE} otherwise
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        return (cbuf == null || len == 0) ? Boolean.FALSE : ((len == 1 && (cbuf[offset] == 'Y' || cbuf[offset] == 'y')) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} at the specified column index.
     * The column value is read as a {@code VARCHAR} string and converted via {@link #valueOf(String)}.
     * SQL NULL values are treated as {@code Boolean.FALSE}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the Y/N value
     * @return {@link Boolean#TRUE} if the column value is {@code "Y"} (case-insensitive),
     *         {@link Boolean#FALSE} for any other value including SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     * Retrieves a {@link Boolean} from a {@link java.sql.ResultSet} using the specified column label.
     * The column value is read as a {@code VARCHAR} string and converted via {@link #valueOf(String)}.
     * SQL NULL values are treated as {@code Boolean.FALSE}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return {@link Boolean#TRUE} if the column value is {@code "Y"} (case-insensitive),
     *         {@link Boolean#FALSE} for any other value including SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnName) throws SQLException {
        return valueOf(rs.getString(columnName));
    }

    /**
     * Sets a {@link Boolean} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * Converts {@code true} to the string {@code "Y"} and {@code false} to {@code "N"}.
     * A {@code null} value is stored as SQL NULL ({@link java.sql.Types#VARCHAR}).
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code Boolean} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(columnIndex, x ? Y : N);
        }
    }

    /**
     * Sets a named {@link Boolean} parameter on a {@link java.sql.CallableStatement}.
     * Converts {@code true} to the string {@code "Y"} and {@code false} to {@code "N"}.
     * A {@code null} value is stored as SQL NULL ({@link java.sql.Types#VARCHAR}).
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code Boolean} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(parameterName, x ? Y : N);
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
     * @throws IOException if an I/O error occurs during appending
     * @implNote
     * This implementation appends {@code stringOf(x)} to {@code appendable}: {@code "Y"} for {@code true} and
     * {@code "N"} for {@code false} or {@code null}. The appended text is therefore identical to {@code stringOf(x)}
     * and round-trips through {@link #valueOf(String)}.
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws IOException {
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
