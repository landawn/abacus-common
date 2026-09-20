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
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for {@code float} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code float} values,
 * including string conversion, JDBC read/write operations, and serialization.
 * This class uses {@code Number} as its generic type parameter so that both the primitive
 * {@code float} type and the {@code Float} wrapper type can share this implementation.
 * Concrete subclasses cover each of those two variants.
 * </p>
 *
 * @see FloatType
 * @see PrimitiveFloatType
 */
public abstract class AbstractFloatType extends NumberType<Number> {

    /**
     * Constructs an {@code AbstractFloatType} with the specified type name.
     *
     * @param typeName the name of the float type (e.g., "Float", "float")
     * @throws IllegalArgumentException if {@code typeName} is {@code null}.
     */
    protected AbstractFloatType(final String typeName) throws IllegalArgumentException {
        super(typeName);
    }

    /**
     * Converts a {@code Number} value to its string representation.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation obtained from the {@code Number}'s own {@code toString()} method.
     * <p>
     * The argument is <i>not</i> narrowed to {@code float} first: a {@code Float} yields its float text
     * ({@code 1.5f} -> {@code "1.5"}), but any other {@code Number} yields that number's text unchanged
     * ({@code Integer 42} -> {@code "42"}, not {@code "42.0"}; {@code Long.MAX_VALUE} -> {@code "9223372036854775807"}).
     * These standard numeric strings are accepted by {@link #valueOf(String)}, possibly with loss of
     * precision or range. A custom {@code Number.toString()} need not produce parseable numeric text.
     * </p>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code Number} value to convert
     * @return {@code x.toString()}, or {@code null} if input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @MayReturnNull
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return x.toString();
    }

    /**
     * Converts a string to a {@code Float} value.
     * <p>
     * This method handles various string formats:
     * </p>
     * <ul>
     *   <li>Only {@code null} and the empty string return the default value.</li>
     *   <li>Any other string is trimmed of leading and trailing whitespace before parsing; a whitespace-only string
     *       therefore trims to empty and is rejected with {@code NumberFormatException} (it does not yield the
     *       default value).</li>
     *   <li>If parsing fails and the trimmed string ends with {@code 'l'}, {@code 'L'}, {@code 'f'},
     *       {@code 'F'}, {@code 'd'}, or {@code 'D'}, a single suffix is stripped and parsing is retried.
     *       The suffix must immediately follow the number; repeated or combined suffixes such as
     *       {@code "1LL"}, {@code "1fD"}, and {@code "1F L"} are rejected.</li>
     *   <li>Valid numeric strings are parsed to {@code Float} values.</li>
     * </ul>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the string to convert, may be {@code null}
     * @return the {@code Float} value, or the default value if {@code str} is empty or {@code null}
     * @throws NumberFormatException if the string cannot be parsed as a {@code float}
     * @see #valueOf(Object)
     * @see #stringOf(Number)
     */
    @Override
    public Float valueOf(final String str) throws NumberFormatException {
        if (Strings.isEmpty(str)) {
            return (Float) defaultValue();
        }

        final String trimmedStr = str.trim();
        try {
            return Float.valueOf(trimmedStr);
        } catch (final NumberFormatException e) {
            if (trimmedStr.length() > 1) {
                final char ch = trimmedStr.charAt(trimmedStr.length() - 1);

                final char preceding = trimmedStr.charAt(trimmedStr.length() - 2);

                // The JDK parser trims its input. Removing a suffix after whitespace would turn an invalid
                // token such as "1 L" into valid "1 "; reject that gap as well as a second suffix.
                if (isNumericTypeSuffix(ch) && preceding > ' ' && !isNumericTypeSuffix(preceding)) {
                    return Float.valueOf(trimmedStr.substring(0, trimmedStr.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     * Returns {@code true} because this type represents a {@code float} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isFloat() {
        return true;
    }

    /**
     * Retrieves a {@code float} value from a {@code ResultSet} at the specified column index.
     * Uses {@link java.sql.ResultSet#getFloat(int)} which returns {@code 0.0f} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the {@code float} value at the specified column, or {@code 0.0f} if SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Float get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getFloat(columnIndex);
    }

    /**
     * Retrieves a {@code float} value from a {@code ResultSet} using the specified column label.
     * Uses {@link java.sql.ResultSet#getFloat(String)} which returns {@code 0.0f} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the {@code float} value at the specified column, or {@code 0.0f} if SQL {@code NULL}
     * @throws NullPointerException if {@code rs} is {@code null}.
     * @throws SQLException if the result set is closed, the requested column is invalid, or the JDBC read fails.
     */
    @Override
    public Float get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getFloat(columnName);
    }

    /**
     * Sets a {@code float} parameter in a {@code PreparedStatement} at the specified position.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code float} value using {@link Numbers#toFloat(Object)}.
     * </p>
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Number} value to set as {@code float}, or {@code null} for SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws NullPointerException, SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.REAL);
        } else {
            stmt.setFloat(columnIndex, Numbers.toFloat(x));
        }
    }

    /**
     * Sets a {@code float} parameter in a {@code CallableStatement} using the specified parameter name.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code float} value using {@link Numbers#toFloat(Object)}.
     * </p>
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Number} value to set as {@code float}, or {@code null} for SQL {@code NULL}
     * @throws NullPointerException if {@code stmt} is {@code null}.
     * @throws SQLException if the statement is closed, the parameter is invalid, or the JDBC bind fails.
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws NullPointerException, SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.REAL);
        } else {
            stmt.setFloat(parameterName, Numbers.toFloat(x));
        }
    }

    /**
     * Appends the string representation of a {@code float} value to an {@code Appendable}.
     * Writes {@code "null"} if the value is {@code null}, otherwise writes the numeric value
     * using its {@code toString()} representation.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} writes this type's JSON/XML
     * literal form and ignores string quotation/escaping config.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Number} value to append as {@code float}
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
    public void appendTo(final Appendable appendable, final Number x) throws NullPointerException, IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
        }
    }

    /**
     * Writes a {@code float} value to a {@code CharacterWriter} with optional configuration.
     * <p>
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes {@code 0.0} instead of {@code null}. Uses {@link IOUtil#write(float, Writer)}
     * for efficient {@code float} writing.
     * </p>
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes this type's literal form to the
     * {@code CharacterWriter}. String quotation/escaping config is ignored.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML literal output,
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Number} value to write as {@code float}
     * @param config the serialization configuration, may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null} and the value is written as the null literal.
     * @throws IllegalArgumentException if {@code writer} is {@code null} and a numeric value is passed to {@code IOUtil.write}.
     * @throws IOException if writing the representation to the destination fails.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, Number x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IllegalArgumentException, IOException {
        x = x == null && config != null && config.isWriteNullNumberAsZero() ? Numbers.FLOAT_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(Numbers.toFloat(x), writer);
        }
    }
}
