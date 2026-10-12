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
 * including string conversion, conversion from {@code Number} values, JDBC read/write operations using
 * {@link java.sql.Types#BOOLEAN}, and JSON/XML serialization with optional {@code writeNullBooleanAsFalse} support.
 * Conversions follow the library-wide rule: an {@code Integer}, {@code Short} or {@code Byte} {@code 1} and the texts
 * {@code "1"}, {@code "y"}, {@code "Y"} and {@code "true"} (any case) are {@code true}; an {@code Integer},
 * {@code Short} or {@code Byte} {@code 0} or {@code -1} and the texts {@code "0"}, {@code "-1"}, {@code "n"},
 * {@code "N"} and {@code "false"} (any case) are {@code false}; any other number, a {@code Number} of any other class
 * ({@code 1L}, {@code 1.0}), and any other text are rejected with an {@link IllegalArgumentException}.
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
     *   <li>{@code Integer}, {@code Short} or {@code Byte} — returns {@code true} if the value is {@code 1} and
     *       {@code false} if it is {@code 0} or {@code -1}; any other value ({@code 2}, {@code -2}) is rejected.</li>
     *   <li>Any other {@code Number} ({@code Long}, {@code Float}, {@code Double}, {@code BigInteger},
     *       {@code BigDecimal}, {@code AtomicInteger}, ...) — rejected whatever its value, so {@code 1L} and
     *       {@code 1.0} are rejected too.</li>
     *   <li>Anything else, a {@code CharSequence} or {@code Character} included — its string form is read as by
     *       {@link #valueOf(String)}: {@code "1"}, {@code "y"}, {@code "Y"} and {@code "true"} (any case) return
     *       {@code true}; {@code "0"}, {@code "-1"}, {@code "n"}, {@code "N"} and {@code "false"} (any case) return
     *       {@code false}; any other text is rejected.</li>
     * </ul>
     * <p><b>Note:</b> unlike {@link #valueOf(String)}, this method does not special-case blank text: an empty or
     * whitespace-only string form yields {@link Boolean#FALSE} rather than the default value.</p>
     *
     * @param obj the source object to convert, may be {@code null}
     * @return the corresponding {@code Boolean} value, or the default value if the input is {@code null}
     * @throws IllegalArgumentException if {@code obj} is an {@code Integer}, {@code Short} or {@code Byte} other than
     *         {@code -1}, {@code 0} or {@code 1}, a {@code Number} of any other class, or an object whose string form is
     *         not one of the accepted texts
     */
    @Override
    public Boolean valueOf(final Object obj) throws IllegalArgumentException {
        if (obj == null) {
            return defaultValue();
        }

        if (obj instanceof Boolean b) {
            return b;
        }

        if (obj instanceof Number num) {
            // An Integer, Short or Byte 1 is true, 0 and -1 are false; anything else, and any other Number class, is
            // rejected. longValue() > 0 truncated 0.5 to 0 and wrapped wide values, so a number that is not a boolean
            // silently became one.
            return booleanOf(num);
        }

        final String str = obj.toString();

        // Blank (not only empty) keeps the documented FALSE special case; everything else follows the padding-stripped
        // valueOf(String) rule, so " Y" and Character 'Y' agree with the String overload (and XML with JSON). Any other
        // object used to go through Boolean.valueOf, which read every unrecognized string form as false.
        return Strings.isBlank(str) ? Boolean.FALSE : parseBoolean(str);
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
     *   <li>{@code "1"}, {@code "y"}, {@code "Y"} and {@code "true"} (any case) return {@code true}.</li>
     *   <li>{@code "0"}, {@code "-1"}, {@code "n"}, {@code "N"} and {@code "false"} (any case) return {@code false}.</li>
     *   <li>Any other string is rejected. Numeric text is not parsed, so {@code "2"}, {@code "1.0"} and {@code "01"}
     *       are rejected, as are {@code "yes"}, {@code "T"} and {@code "abc"}.</li>
     * </ul>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert, may be {@code null}
     * @return the {@code Boolean} value, or the default value if the input is {@code null} or blank
     * @throws IllegalArgumentException if {@code str} is not blank and, without its padding, is none of the accepted texts
     * @see #valueOf(Object)
     * @see #stringOf(Boolean)
     */
    @Override
    public Boolean valueOf(final String str) throws IllegalArgumentException {
        if (Strings.isBlank(str)) {
            return defaultValue();
        }

        return parseBoolean(str);
    }

    /**
     * Converts a region of the specified character array to a {@code Boolean} value, exactly as
     * {@link #valueOf(String)} converts the same text:
     * <ul>
     *   <li>{@code null} or zero-length region — returns the default value.</li>
     *   <li>{@code "1"}, {@code "y"}, {@code "Y"} and {@code "true"} (any case) — returns {@code true}; {@code "0"},
     *       {@code "-1"}, {@code "n"}, {@code "N"} and {@code "false"} (any case) — returns {@code false}. These are
     *       read in place, without allocating.</li>
     *   <li>A region that begins or ends with padding (a character {@code <= ' '} or any
     *       {@linkplain Character#isWhitespace(char) Unicode whitespace}) is stripped first, so {@code " Y"} and
     *       {@code " true "} return {@code true}, and a whitespace-only region (of any length) returns the default
     *       value. Interior whitespace is not removed.</li>
     *   <li>Any other region is rejected.</li>
     * </ul>
     *
     * @param cbuf the character array to convert, may be {@code null}
     * @param offset the starting position in the array (0-based)
     * @param length the number of characters to read
     * @return the corresponding {@code Boolean} value, or the default value if {@code cbuf} is {@code null} or {@code length} is 0
     * @throws IndexOutOfBoundsException if the requested nonempty region is read outside {@code cbuf}; a {@code null} buffer or zero length returns the default value without reading.
     * @throws IllegalArgumentException if the region is not blank and, without its padding, is none of the accepted texts
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int length) throws IndexOutOfBoundsException, IllegalArgumentException {
        if ((cbuf == null) || (length == 0)) {
            return defaultValue();
        }

        final Boolean token = booleanToken(cbuf, offset, length);

        // Miss path only: padded text (" Y", " true "), blank text and text that is not a boolean are read by
        // valueOf(String), so the JSON (char[]) and XML (String) deserializers agree on all of them; the check above
        // keeps the common case allocation-free.
        return token != null ? token : valueOf(new String(cbuf, offset, length));
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
     * @param statement the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the boolean value to set, or {@code null} for SQL {@code NULL}
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement statement, final int columnIndex, final Boolean x) throws NullPointerException, SQLException {
        if (x == null) {
            statement.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            statement.setBoolean(columnIndex, x);
        }
    }

    /**
     * Sets the specified boolean parameter in a {@code CallableStatement} using the given parameter name.
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param statement the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the boolean value to set, or {@code null} for SQL {@code NULL}
     * @throws NullPointerException if {@code statement} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement statement, final String parameterName, final Boolean x) throws NullPointerException, SQLException {
        if (x == null) {
            statement.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            statement.setBoolean(parameterName, x);
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
