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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for double types in the type system.
 * This class provides common functionality for handling double values,
 * including conversion, database operations, and serialization.
 * Note that this class uses Number as its generic type to allow for both
 * primitive double and Double wrapper handling.
 */
public abstract class AbstractDoubleType extends NumberType<Number> {

    /**
     * Constructs an AbstractDoubleType with the specified type name.
     *
     * @param typeName the name of the double type (e.g., "Double", "double")
     */
    protected AbstractDoubleType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a Number value to its string representation as a double.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation obtained from the Number's toString() method.
     *
     * @param x the Number value to convert
     * @return the string representation of the double value, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return x.toString();
    }

    /**
     * Converts a string to a Double value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing</li>
     *   <li>Valid numeric strings are parsed to double values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractDoubleType type = TypeFactory.getType(Double.class);
     * Double value1 = type.valueOf("3.14159");   // returns 3.14159
     * Double value2 = type.valueOf("100.5D");    // returns 100.5 (suffix stripped)
     * Double value3 = type.valueOf("42.0f");     // returns 42.0 (suffix stripped)
     * Double value4 = type.valueOf("");          // returns default value
     * }</pre>
     *
     * @param str the string to convert
     * @return the Double value
     * @throws NumberFormatException if the string cannot be parsed as a double
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
     * Checks if this type represents a double type.
     * This method always returns {@code true} for double types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractDoubleType type = TypeFactory.getType(Double.class);
     * if (type.isDouble()) {
     *     // Handle double type specific logic
     *     System.out.println("This is a double type");
     * }
     * }</pre>
     *
     * @return {@code true}, indicating this is a double type
     */
    @Override
    public boolean isDouble() {
        return true;
    }

    /**
     * Retrieves a double value from a ResultSet at the specified column index.
     * This method uses rs.getDouble() which returns 0.0 for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive double types
     * PrimitiveDoubleType type = TypeFactory.getType(double.class);
     * double value = type.get(rs, 1);   // Returns 0.0 for SQL NULL
     *
     * // For wrapper Double types
     * DoubleType type = TypeFactory.getType(Double.class);
     * Double value = type.get(rs, 1);   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the double value at the specified column; returns 0.0 if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Double get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getDouble(columnIndex);
    }

    /**
     * Retrieves a double value from a ResultSet using the specified column label.
     * This method uses rs.getDouble() which returns 0.0 for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive double types
     * PrimitiveDoubleType type = TypeFactory.getType(double.class);
     * double value = type.get(rs, "price");   // Returns 0.0 for SQL NULL
     *
     * // For wrapper Double types
     * DoubleType type = TypeFactory.getType(Double.class);
     * Double value = type.get(rs, "price");   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label
     * @return the double value at the specified column; returns 0.0 if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public Double get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getDouble(columnLabel);
    }

    /**
     * Sets a double parameter in a PreparedStatement at the specified position.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a double value using {@link Numbers#toDouble(Object)}.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Number value to set as double, or {@code null} for SQL NULL
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
     * Sets a double parameter in a CallableStatement using the specified parameter name.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a double value using {@link Numbers#toDouble(Object)}.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the Number value to set as double, or {@code null} for SQL NULL
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
     * Appends the string representation of a double value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value
     * using its toString() representation.
     *
     * @param appendable the Appendable to write to
     * @param x the Number value to append as double
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
     * Writes a double value to a CharacterWriter with optional configuration.
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes 0.0 instead of {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Number value to write as double
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JsonXmlSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullNumberAsZero() ? Numbers.DOUBLE_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(Numbers.toDouble(x));
        }
    }
}
