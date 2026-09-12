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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for boolean types in the type system.
 * <p>
 * This class provides common functionality for handling {@code boolean}/{@code Boolean} values,
 * including string conversion (recognising {@code "true"}, {@code "Y"}, {@code "y"}, and
 * {@code "1"} as {@code true}), conversion from {@code Number} values ({@code true} when
 * {@code longValue() > 0}), JDBC read/write operations using {@link java.sql.Types#BOOLEAN}, and JSON/XML
 * serialization with optional {@code writeNullBooleanAsFalse} support.
 * Concrete subclasses cover the primitive {@code boolean} type and the {@code Boolean} wrapper.
 * </p>
 *
 * @see BooleanType
 * @see PrimitiveBooleanType
 */
public abstract class AbstractBooleanType extends AbstractPrimaryType<Boolean> {

    /**
     * Constructs a new {@code AbstractBooleanType} with the specified type name.
     *
     * @param typeName the name of the boolean type (e.g., "Boolean", "boolean")
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    protected AbstractBooleanType(final String typeName) throws IllegalArgumentException {
        super(typeName);
    }

    /**
     * Converts the specified {@code Boolean} value to its string representation.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the boolean value ("true" or "false").
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param b the {@code Boolean} value to convert
     * @return the string representation of the boolean value, or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null) ? null : b.toString();
    }

    /**
     * Converts the specified object to a {@code Boolean} value.
     * <p>
     * This method handles the following input types:
     * </p>
     * <ul>
     *   <li>{@code null} — returns the default value.</li>
     *   <li>{@code Boolean} — returned as-is.</li>
     *   <li>{@code Number} — returns {@code true} if {@code longValue() > 0}, {@code false} otherwise.</li>
     *   <li>{@code CharSequence} or {@code Character} — leading and trailing padding is removed and the text is then read exactly as by
     *       {@link #valueOf(String)}: a single {@code 'Y'}, {@code 'y'}, or {@code '1'} returns {@code true}; any
     *       other single character returns {@code false}; longer text is parsed using
     *       {@link Boolean#valueOf(String)} (case-insensitive {@code "true"} yields {@code true}).</li>
     *   <li>Other objects — converted via {@code Boolean.valueOf(obj.toString())}.</li>
     * </ul>
     * <p><b>Note:</b> unlike {@link #valueOf(String)}, this method does not special-case blank text: an empty or
     * whitespace-only {@code CharSequence} yields {@link Boolean#FALSE} rather than the default value. A
     * {@code Character} is treated as a one-character string, so {@code 'Y'}, {@code 'y'} and {@code '1'} yield
     * {@code true} (earlier releases passed it through {@code Boolean.valueOf}, which always gave {@code false}).</p>
     *
     * @param obj the source object to convert, may be {@code null}
     * @return the corresponding {@code Boolean} value, or the default value if the input is {@code null}
     */
    @Override
    public Boolean valueOf(final Object obj) {
        if (obj == null) {
            return defaultValue();
        }

        if (obj instanceof Boolean b) {
            return b;
        }

        if (obj instanceof Number num) {
            return num.longValue() > 0;
        }

        if (obj instanceof CharSequence || obj instanceof Character) {
            final String str = obj.toString();

            // Blank (not only empty) keeps the documented FALSE special case; everything else follows the padding-
            // stripped valueOf(String) rule so " Y" and Character 'Y' agree with the String overload (and XML with JSON).
            return Strings.isBlank(str) ? Boolean.FALSE : parseBoolean(str);
        }

        return Boolean.valueOf(obj.toString());
    }

    /**
     * Converts the specified string to a {@code Boolean} value.
     * This method handles various string formats:
     * <ul>
     *   <li>{@code null}, empty or whitespace-only strings return the default value.</li>
     *   <li>Other strings have leading and trailing padding removed before parsing (so {@code " Y"} and
     *       {@code " true "} are accepted). Padding is a character the blank test above would discard
     *       ({@linkplain Character#isWhitespace(char) Unicode whitespace}, e.g. {@code U+3000}) or a character
     *       {@code <= ' '} (e.g. {@code U+0001}); interior padding is not removed.</li>
     *   <li>Single character strings: {@code 'Y'}, {@code 'y'}, or {@code '1'} return {@code true};
     *       any other single character returns {@code false}.</li>
     *   <li>Other strings are parsed using {@link Boolean#valueOf(String)} (i.e. {@code true} only
     *       if the string equals {@code "true"} case-insensitively).</li>
     * </ul>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert, may be {@code null}
     * @return the {@code Boolean} value, or the default value if the input is {@code null} or blank
     * @see #valueOf(Object)
     * @see #stringOf(Boolean)
     */
    @Override
    public Boolean valueOf(final String str) {
        if (Strings.isBlank(str)) {
            return defaultValue();
        }

        return parseBoolean(str);
    }

    /**
     * Converts a region of the specified character array to a {@code Boolean} value.
     * <p>
     * Handles the following cases:
     * </p>
     * <ul>
     *   <li>{@code null} or zero-length region — returns the default value.</li>
     *   <li>Single character {@code 'Y'}, {@code 'y'}, or {@code '1'} — returns {@code true}; any other single
     *       non-padding character — returns {@code false}.</li>
     *   <li>Four-character sequence equal to {@code "true"} (case-insensitive) — returns {@code true}.</li>
     *   <li>A region that begins or ends with padding (a character {@code <= ' '} or any
     *       {@linkplain Character#isWhitespace(char) Unicode whitespace}) — read as by
     *       {@link #valueOf(String)}: stripped first, so {@code " Y"} and {@code " true "} return {@code true} and a
     *       whitespace-only region (of any length) returns the default value. Interior whitespace is not removed.</li>
     *   <li>Any other value — returns {@code false}.</li>
     * </ul>
     *
     * @param cbuf the character array to convert, may be {@code null}
     * @param offset the starting position in the array (0-based)
     * @param len the number of characters to read
     * @return the corresponding {@code Boolean} value, or the default value if {@code cbuf} is {@code null} or {@code len} is 0
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) throws IndexOutOfBoundsException {
        if ((cbuf == null) || (len == 0)) {
            return defaultValue();
        }

        if (len == 1) {
            final char ch = cbuf[offset];

            if (ch == 'Y' || ch == 'y' || ch == '1') {
                return Boolean.TRUE;
            }

            // A lone padding character is text that valueOf(String) - and therefore the XML reader - either rejects
            // as blank (the default value) or strips away, leaving "" for Boolean.valueOf (FALSE). Which of the two
            // it is depends on the character, so defer to it rather than answering FALSE here.
            return isPadding(ch) ? valueOf(String.valueOf(ch)) : Boolean.FALSE;
        }

        if ((len == 4) && (((cbuf[offset] == 't') || (cbuf[offset] == 'T')) && ((cbuf[offset + 1] == 'r') || (cbuf[offset + 1] == 'R'))
                && ((cbuf[offset + 2] == 'u') || (cbuf[offset + 2] == 'U')) && ((cbuf[offset + 3] == 'e') || (cbuf[offset + 3] == 'E')))) {
            return Boolean.TRUE;
        }

        // Miss path only: a region padded with whitespace (" Y", " true ") is read by valueOf(String) so the JSON
        // (char[]) and XML (String) deserializers agree; the fast checks above keep the common case allocation-free.
        // isPadding covers both definitions (<= ' ' and Character.isWhitespace), as parseBoolean's strip does.
        if (isPadding(cbuf[offset]) || isPadding(cbuf[offset + len - 1])) {
            return valueOf(new String(cbuf, offset, len));
        }

        return Boolean.FALSE;
    }

    /**
     * Returns {@code true} because this type represents a boolean type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isBoolean() {
        return true;
    }

    /**
     * Returns {@code false} because boolean values do not require quoting in CSV format.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Retrieves a boolean value from the specified {@code ResultSet} at the given column index.
     * Uses {@link java.sql.ResultSet#getBoolean(int)} which returns {@code false} for SQL {@code NULL} values.
     * Therefore, SQL {@code NULL} values are converted to {@code false}, not {@code null}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the boolean value at the specified column, or {@code false} if the value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getBoolean(columnIndex);
    }

    /**
     * Retrieves a boolean value from the specified {@code ResultSet} using the given column label.
     * Uses {@link java.sql.ResultSet#getBoolean(String)} which returns {@code false} for SQL {@code NULL} values.
     * Therefore, SQL {@code NULL} values are converted to {@code false}, not {@code null}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the boolean value at the specified column, or {@code false} if the value is SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getBoolean(columnName);
    }

    /**
     * Sets the specified boolean parameter in a {@code PreparedStatement} at the given position.
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the boolean value to set, or {@code null} for SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Boolean x) throws NullPointerException, SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x);
        }
    }

    /**
     * Sets the specified boolean parameter in a {@code CallableStatement} using the given parameter name.
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the boolean value to set, or {@code null} for SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Boolean x) throws NullPointerException, SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x);
        }
    }

    /**
     * Appends the string representation of the specified boolean value to the given {@code Appendable}.
     * Writes "null" if the value is {@code null}, "true" if {@code true}, or "false" if {@code false}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the boolean value to append, may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
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
    public void appendTo(final Appendable appendable, final Boolean x) throws NullPointerException, IOException {
        appendable.append((x == null) ? NULL_STRING : (x ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes the specified boolean value to the given {@code CharacterWriter} with optional configuration.
     * If the configuration specifies {@code writeNullBooleanAsFalse} and the value is {@code null},
     * writes {@code false} instead of {@code null}.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the boolean value to write, may be {@code null}
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, Boolean x, final JsonXmlSerConfig<?> config) throws NullPointerException, IOException {
        x = x == null && config != null && config.isWriteNullBooleanAsFalse() ? Boolean.FALSE : x;

        writer.write((x == null) ? NULL_CHAR_ARRAY : (x ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}
