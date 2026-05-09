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
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for {@code double} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code double} values,
 * including string conversion, JDBC read/write operations, and serialization.
 * This class uses {@code Number} as its generic type parameter so that both the primitive
 * {@code double} type and the {@code Double} wrapper type can share this implementation.
 * Concrete subclasses cover each of those two variants.
 * </p>
 *
 * @see DoubleType
 * @see PrimitiveDoubleType
 */
public abstract class AbstractDoubleType extends NumberType<Number> {

    /**
     * Constructs an {@code AbstractDoubleType} with the specified type name.
     *
     * @param typeName the name of the double type (e.g., "Double", "double")
     */
    protected AbstractDoubleType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a {@code Number} value to its string representation as a {@code double}.
     * <p>
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation obtained from the {@code Number}'s {@code toString()} method.
     * </p>
     *
     * @param x the {@code Number} value to convert
     * @return the string representation of the {@code double} value, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return x.toString();
    }

    /**
     * Converts a string to a {@code Double} value.
     * <p>
     * This method handles various string formats:
     * </p>
     * <ul>
     *   <li>Empty or {@code null} strings return the default value.</li>
     *   <li>The string is trimmed of leading and trailing whitespace before parsing.</li>
     *   <li>If parsing fails and the trimmed string ends with {@code 'l'}, {@code 'L'}, {@code 'f'},
     *       {@code 'F'}, {@code 'd'}, or {@code 'D'}, the suffix is stripped and parsing is retried.</li>
     *   <li>Valid numeric strings are parsed to {@code Double} values.</li>
     * </ul>
     *
     * @param str the string to convert, may be {@code null}
     * @return the {@code Double} value, or the default value if {@code str} is empty or {@code null}
     * @throws NumberFormatException if the string cannot be parsed as a {@code double}
     */
    @Override
    public Double valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return (Double) defaultValue();
        }

        final String trimmedStr = str.trim();
        try {
            return Double.valueOf(trimmedStr);
        } catch (final NumberFormatException e) {
            if (trimmedStr.length() > 1) {
                final char ch = trimmedStr.charAt(trimmedStr.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Double.valueOf(trimmedStr.substring(0, trimmedStr.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     * Returns {@code true} because this type represents a {@code double} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isDouble() {
        return true;
    }

    /**
     * Retrieves a {@code double} value from a {@code ResultSet} at the specified column index.
     * Uses {@link java.sql.ResultSet#getDouble(int)} which returns {@code 0.0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the {@code double} value at the specified column, or {@code 0.0} if SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public Double get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getDouble(columnIndex);
    }

    /**
     * Retrieves a {@code double} value from a {@code ResultSet} using the specified column label.
     * Uses {@link java.sql.ResultSet#getDouble(String)} which returns {@code 0.0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the {@code double} value at the specified column, or {@code 0.0} if SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the {@code columnName} is not found
     */
    @Override
    public Double get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getDouble(columnName);
    }

    /**
     * Sets a {@code double} parameter in a {@code PreparedStatement} at the specified position.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code double} value using {@link Numbers#toDouble(Object)}.
     * </p>
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Number} value to set as {@code double}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.DOUBLE);
        } else {
            stmt.setDouble(columnIndex, Numbers.toDouble(x));
        }
    }

    /**
     * Sets a {@code double} parameter in a {@code CallableStatement} using the specified parameter name.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code double} value using {@link Numbers#toDouble(Object)}.
     * </p>
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Number} value to set as {@code double}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.DOUBLE);
        } else {
            stmt.setDouble(parameterName, Numbers.toDouble(x));
        }
    }

    /**
     * Appends the string representation of a {@code double} value to an {@code Appendable}.
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value
     * using its {@code toString()} representation.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Number} value to append as {@code double}
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
     * Writes a {@code double} value to a {@code CharacterWriter} with optional configuration.
     * <p>
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes {@code 0.0} instead of {@code null}.
     * </p>
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Number} value to write as {@code double}
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullNumberAsZero() ? Numbers.DOUBLE_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(Numbers.toDouble(x));
        }
    }
}
