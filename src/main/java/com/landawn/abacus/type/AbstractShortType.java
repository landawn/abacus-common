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
 * The Abstract base class for {@code short} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code short} values,
 * including conversion, database operations, and serialization.
 * Note that this class uses {@code Number} as its generic type to allow for both
 * primitive {@code short} and {@code Short} wrapper handling.
 * </p>
 */
public abstract class AbstractShortType extends NumberType<Number> {

    /**
     * Constructs an {@code AbstractShortType} with the specified type name.
     *
     * @param typeName the name of the short type (e.g., "Short", "short")
     */
    protected AbstractShortType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a {@code Number} value to its string representation as a {@code short}.
     * <p>
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the {@code short} value.
     * </p>
     *
     * @param x the {@code Number} value to convert
     * @return the string representation of the {@code short} value, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.shortValue());
    }

    /**
     * Converts a string to a {@code Short} value.
     * <p>
     * This method handles various string formats:
     * </p>
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing</li>
     *   <li>Valid numeric strings are parsed to {@code short} values</li>
     * </ul>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(Short.class);
     * Short value1 = type.valueOf("32767");   // returns 32767 (max short value)
     * Short value2 = type.valueOf("100");     // returns 100
     * Short value3 = type.valueOf("42L");     // returns 42 (suffix stripped)
     * Short value4 = type.valueOf("");        // returns default value
     * }</pre>
     *
     * @param str the string to convert
     * @return the {@code Short} value
     * @throws NumberFormatException if the string cannot be parsed as a {@code short}
     */
    @Override
    public Short valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return (Short) defaultValue();
        }

        try {
            return Numbers.toShort(str);
        } catch (final NumberFormatException e) {
            if (str.length() > 1) {
                final char ch = str.charAt(str.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Numbers.toShort(str.substring(0, str.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     * Converts a character array to a {@code Short} value.
     * <p>
     * Parses the character array as an integer and checks if it's within short range
     * ({@code Short.MIN_VALUE} to {@code Short.MAX_VALUE}).
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(Short.class);
     * char[] buffer = "1000".toCharArray();
     * Short value = type.valueOf(buffer, 0, 4);   // returns 1000
     * }</pre>
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the {@code Short} value, or default value if input is {@code null} or empty
     * @throws NumberFormatException if the value is out of short range or not a valid number
     */
    @Override
    public Short valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return (Short) defaultValue();
        }

        final int i = parseInt(cbuf, offset, len);

        if ((i < Short.MIN_VALUE) || (i > Short.MAX_VALUE)) {
            throw new NumberFormatException("Value out of range. Value:\"" + i + "\" Radix:" + 10);
        }

        return (short) i;
    }

    /**
     * Checks if this type represents a {@code short} type.
     * <p>
     * This method always returns {@code true} for {@code short} types.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<Short> type = TypeFactory.getType(Short.class);
     * if (type.isShort()) {
     *     // Handle short type specific logic
     *     System.out.println("This is a short type");
     * }
     * }</pre>
     *
     * @return {@code true}, indicating this is a {@code short} type
     */
    @Override
    public boolean isShort() {
        return true;
    }

    /**
     * Retrieves a {@code short} value from a {@code ResultSet} at the specified column index.
     * <p>
     * This method uses {@code rs.getShort()} which returns {@code 0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * // For primitive short types
     * Type<Short> type = TypeFactory.getType(short.class);
     * short value = type.get(rs, 1);   // Returns 0 for SQL NULL
     *
     * // For wrapper Short types
     * Type<Short> type = TypeFactory.getType(Short.class);
     * Short value = type.get(rs, 1);   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the {@code short} value at the specified column; returns {@code 0} if SQL {@code NULL} (may be overridden by subclasses to return {@code null})
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public Short get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getShort(columnIndex);
    }

    /**
     * Retrieves a {@code short} value from a {@code ResultSet} using the specified column label.
     * <p>
     * This method uses {@code rs.getShort()} which returns {@code 0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * // For primitive short types
     * Type<Short> type = TypeFactory.getType(short.class);
     * short value = type.get(rs, "port");   // Returns 0 for SQL NULL
     *
     * // For wrapper Short types
     * Type<Short> type = TypeFactory.getType(Short.class);
     * Short value = type.get(rs, "port");   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the {@code short} value at the specified column; returns {@code 0} if SQL {@code NULL} (may be overridden by subclasses to return {@code null})
     * @throws SQLException if a database access error occurs or the {@code columnName} is not found
     */
    @Override
    public Short get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getShort(columnName);
    }

    /**
     * Sets a {@code short} parameter in a {@code PreparedStatement} at the specified position.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code short} value.
     * </p>
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Number} value to set as {@code short}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.SMALLINT);
        } else {
            stmt.setShort(columnIndex, x.shortValue());
        }
    }

    /**
     * Sets a {@code short} parameter in a {@code CallableStatement} using the specified parameter name.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code short} value.
     * </p>
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Number} value to set as {@code short}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.SMALLINT);
        } else {
            stmt.setShort(parameterName, x.shortValue());
        }
    }

    /**
     * Appends the string representation of a {@code short} value to an {@code Appendable}.
     * <p>
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value.
     * </p>
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Number} value to append as {@code short}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final Number x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
        }
    }

    /**
     * Writes a {@code short} value to a {@code CharacterWriter} with optional configuration.
     * <p>
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes {@code 0} instead of {@code null}.
     * </p>
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Number} value to write as {@code short}
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullNumberAsZero() ? Numbers.SHORT_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.shortValue());
        }
    }
}
