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
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for float types in the type system.
 * This class provides common functionality for handling float values,
 * including conversion, database operations, and serialization.
 * Note that this class uses Number as its generic type to allow for both
 * primitive float and Float wrapper handling.
 */
public abstract class AbstractFloatType extends NumberType<Number> {

    protected AbstractFloatType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a Number value to its string representation as a float.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation obtained from the Number's toString() method.
     *
     * @param x the Number value to convert
     * @return the string representation of the float value, or {@code null} if input is {@code null}
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
     * Converts a string to a Float value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing</li>
     *   <li>Valid numeric strings are parsed to float values</li>
     * </ul>
     *
     * @param str the string to convert
     * @return the Float value
     * @throws NumberFormatException if the string cannot be parsed as a float
     */
    @MayReturnNull
    @Override

    public Float valueOf(final String str) {
        try {
            return Strings.isEmpty(str) ? ((Float) defaultValue()) : Float.valueOf(str);
        } catch (final NumberFormatException e) {
            if (str.length() > 1) {
                final char ch = str.charAt(str.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Float.valueOf(str.substring(0, str.length() - 1));
                }
            }

            throw e;
        }
    }

    @Override
    public boolean isFloat() {
        return true;
    }

    /**
     * Retrieves a float value from a ResultSet at the specified column index.
     * This method uses rs.getFloat() which returns 0.0f for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive float types
     * PrimitiveFloatType type = TypeFactory.getType(float.class);
     * float value = type.get(rs, 1); // Returns 0.0f for SQL NULL
     *
     * // For wrapper Float types
     * FloatType type = TypeFactory.getType(Float.class);
     * Float value = type.get(rs, 1); // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the float value at the specified column; returns 0.0f if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    @MayReturnNull
    public Float get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getFloat(columnIndex);
    }

    /**
     * Retrieves a float value from a ResultSet using the specified column label.
     * This method uses rs.getFloat() which returns 0.0f for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive float types
     * PrimitiveFloatType type = TypeFactory.getType(float.class);
     * float value = type.get(rs, "ratio"); // Returns 0.0f for SQL NULL
     *
     * // For wrapper Float types
     * FloatType type = TypeFactory.getType(Float.class);
     * Float value = type.get(rs, "ratio"); // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label
     * @return the float value at the specified column; returns 0.0f if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    @MayReturnNull
    public Float get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getFloat(columnLabel);
    }

    /**
     * Sets a float parameter in a PreparedStatement at the specified position.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a float value using {@link Numbers#toFloat(Object)}.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Number value to set as float, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.FLOAT);
        } else {
            stmt.setFloat(columnIndex, Numbers.toFloat(x));
        }
    }

    /**
     * Sets a float parameter in a CallableStatement using the specified parameter name.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a float value using {@link Numbers#toFloat(Object)}.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the Number value to set as float, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.FLOAT);
        } else {
            stmt.setFloat(parameterName, Numbers.toFloat(x));
        }
    }

    /**
     * Appends the string representation of a float value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value
     * using its toString() representation.
     *
     * @param appendable the Appendable to write to
     * @param x the Number value to append as float
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
     * Writes a float value to a CharacterWriter with optional configuration.
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes 0.0f instead of {@code null}. Uses {@link IOUtil#write(char, Writer)}
     * for efficient float writing.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Number value to write as float
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JSONXMLSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullNumberAsZero() ? Numbers.FLOAT_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(Numbers.toFloat(x), writer);
        }
    }
}
