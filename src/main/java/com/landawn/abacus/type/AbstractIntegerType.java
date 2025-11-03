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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for integer types in the type system.
 * This class provides common functionality for handling integer values,
 * including conversion, database operations, and serialization.
 * Note that this class uses Number as its generic type to allow for both
 * primitive int and Integer wrapper handling.
 */
public abstract class AbstractIntegerType extends NumberType<Number> {

    protected AbstractIntegerType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a Number value to its string representation as an integer.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the integer value.
     *
     * @param x the Number value to convert
     * @return the string representation of the integer value, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.intValue());
    }

    /**
     * Converts a string to an Integer value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing</li>
     *   <li>Valid numeric strings are parsed to integer values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractIntegerType type = TypeFactory.getType(Integer.class);
     * Integer value1 = type.valueOf("42");      // returns 42
     * Integer value2 = type.valueOf("1000");    // returns 1000
     * Integer value3 = type.valueOf("100L");    // returns 100 (suffix stripped)
     * Integer value4 = type.valueOf("");        // returns default value
     * }</pre>
     *
     * @param str the string to convert
     * @return the Integer value
     * @throws NumberFormatException if the string cannot be parsed as an integer
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
     * Converts a character array to an Integer value.
     * Delegates to the {@link #parseInt(char[], int, int)} method for parsing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractIntegerType type = TypeFactory.getType(Integer.class);
     * char[] buffer = "12345".toCharArray();
     * Integer value = type.valueOf(buffer, 0, 5); // returns 12345
     * }</pre>
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the Integer value, or default value if input is {@code null} or empty
     */
    @Override
    public Integer valueOf(final char[] cbuf, final int offset, final int len) {
        return ((cbuf == null) || (len == 0)) ? ((Integer) defaultValue()) : (Integer) parseInt(cbuf, offset, len);
    }

    /**
     * Checks if this type represents an integer type.
     * This method always returns {@code true} for integer types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractIntegerType type = TypeFactory.getType(Integer.class);
     * if (type.isInteger()) {
     *     // Handle integer type specific logic
     *     System.out.println("This is an integer type");
     * }
     * }</pre>
     *
     * @return {@code true}, indicating this is an integer type
     */
    @Override
    public boolean isInteger() {
        return true;
    }

    /**
     * Retrieves an integer value from a ResultSet at the specified column index.
     * This method uses rs.getInt() which returns 0 for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive int types
     * PrimitiveIntType type = TypeFactory.getType(int.class);
     * int value = type.get(rs, 1); // Returns 0 for SQL NULL
     *
     * // For wrapper Integer types
     * IntegerType type = TypeFactory.getType(Integer.class);
     * Integer value = type.get(rs, 1); // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the integer value at the specified column; returns 0 if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Integer get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getInt(columnIndex);
    }

    /**
     * Retrieves an integer value from a ResultSet using the specified column label.
     * This method uses rs.getInt() which returns 0 for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive int types
     * PrimitiveIntType type = TypeFactory.getType(int.class);
     * int value = type.get(rs, "count"); // Returns 0 for SQL NULL
     *
     * // For wrapper Integer types
     * IntegerType type = TypeFactory.getType(Integer.class);
     * Integer value = type.get(rs, "count"); // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label
     * @return the integer value at the specified column; returns 0 if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public Integer get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getInt(columnLabel);
    }

    /**
     * Sets an integer parameter in a PreparedStatement at the specified position.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to an integer value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Number value to set as integer, or {@code null} for SQL NULL
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
     * Sets an integer parameter in a CallableStatement using the specified parameter name.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to an integer value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the Number value to set as integer, or {@code null} for SQL NULL
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
     * Appends the string representation of an integer value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value.
     *
     * @param appendable the Appendable to write to
     * @param x the Number value to append as integer
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
     * Writes an integer value to a CharacterWriter with optional configuration.
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes 0 instead of {@code null}. Uses {@link CharacterWriter#writeInt(int)}
     * for efficient integer writing.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Number value to write as integer
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JSONXMLSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullNumberAsZero() ? Numbers.INTEGER_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.intValue());
        }
    }
}
