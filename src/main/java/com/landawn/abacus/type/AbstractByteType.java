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
 * The abstract base class for byte types in the type system.
 * This class provides common functionality for handling byte values,
 * including conversion, database operations, and serialization.
 * <p>Note: this class uses {@code Number} as its generic type to allow for both
 * primitive {@code byte} and {@code Byte} wrapper handling.</p>
 */
public abstract class AbstractByteType extends NumberType<Number> {

    /**
     * Constructs a new {@code AbstractByteType} with the specified type name.
     *
     * @param typeName the name of the byte type (e.g., "Byte", "byte")
     */
    protected AbstractByteType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts the specified {@code Number} value to its string representation as a byte.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the byte value.
     *
     * @param x the {@code Number} value to convert
     * @return the string representation of the byte value, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.byteValue());
    }

    /**
     * Converts the specified string to a {@code Byte} value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value.</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing.</li>
     *   <li>Valid numeric strings are parsed to byte values.</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Byte> type = TypeFactory.getType(Byte.class);
     * Byte value1 = type.valueOf("42");    // returns 42
     * Byte value2 = type.valueOf("127");   // returns 127
     * Byte value3 = type.valueOf("42L");   // returns 42 (suffix stripped)
     * Byte value4 = type.valueOf("");      // returns default value
     * }</pre>
     *
     * @param str the string to convert
     * @return the {@code Byte} value
     * @throws NumberFormatException if the string cannot be parsed as a byte
     */
    @Override
    public Byte valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return (Byte) defaultValue();
        }

        try {
            return Numbers.toByte(str);
        } catch (final NumberFormatException e) {
            if (str.length() > 1) {
                final char ch = str.charAt(str.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Numbers.toByte(str.substring(0, str.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     * Converts the specified character array to a {@code Byte} value.
     * Parses the character array as an integer and checks if it's within byte range
     * ({@code Byte.MIN_VALUE} to {@code Byte.MAX_VALUE}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Byte> type = TypeFactory.getType(Byte.class);
     * char[] buffer = "100".toCharArray();
     * Byte value = type.valueOf(buffer, 0, 3);   // returns 100
     * }</pre>
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the {@code Byte} value, or default value if the input is {@code null} or empty
     * @throws NumberFormatException if the value is out of byte range or not a valid number
     */
    @Override
    public Byte valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return (Byte) defaultValue();
        }

        final int i = parseInt(cbuf, offset, len);

        if ((i < Byte.MIN_VALUE) || (i > Byte.MAX_VALUE)) {
            throw new NumberFormatException("Value out of range. Value:\"" + i + "\" Radix:" + 10);
        }

        return (byte) i;
    }

    /**
     * Returns {@code true} because this type represents a byte type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Byte> type = TypeFactory.getType(Byte.class);
     * if (type.isByte()) {
     *     // Handle byte type specific logic
     *     System.out.println("This is a byte type");
     * }
     * }</pre>
     *
     * @return {@code true}
     */
    @Override
    public boolean isByte() {
        return true;
    }

    /**
     * Retrieves a byte value from the specified {@code ResultSet} at the given column index.
     * This method uses {@code rs.getByte()} which returns 0 for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive byte types
     * Type<Byte> type = TypeFactory.getType(byte.class);
     * byte value = type.get(rs, 1);   // Returns 0 for SQL NULL
     *
     * // For wrapper Byte types
     * Type<Byte> type = TypeFactory.getType(Byte.class);
     * Byte value = type.get(rs, 1);   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the byte value at the specified column; returns 0 if SQL {@code NULL} (may be overridden by subclasses to return {@code null})
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public Byte get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getByte(columnIndex);
    }

    /**
     * Retrieves a byte value from the specified {@code ResultSet} using the given column label.
     * This method uses {@code rs.getByte()} which returns 0 for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive byte types
     * Type<Byte> type = TypeFactory.getType(byte.class);
     * byte value = type.get(rs, "status");   // Returns 0 for SQL NULL
     *
     * // For wrapper Byte types
     * Type<Byte> type = TypeFactory.getType(Byte.class);
     * Byte value = type.get(rs, "status");   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the byte value at the specified column; returns 0 if SQL {@code NULL} (may be overridden by subclasses to return {@code null})
     * @throws SQLException if a database access error occurs or the {@code columnName} is not found
     */
    @Override
    public Byte get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getByte(columnName);
    }

    /**
     * Sets the specified byte parameter in a {@code PreparedStatement} at the given position.
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a byte value.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Number} value to set as byte, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.TINYINT);
        } else {
            stmt.setByte(columnIndex, x.byteValue());
        }
    }

    /**
     * Sets the specified byte parameter in a {@code CallableStatement} using the given parameter name.
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a byte value.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Number} value to set as byte, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.TINYINT);
        } else {
            stmt.setByte(parameterName, x.byteValue());
        }
    }

    /**
     * Appends the string representation of the specified byte value to the given {@code Appendable}.
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Number} value to append as byte
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
     * Writes the specified byte value to the given {@code CharacterWriter} with optional configuration.
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes 0 instead of {@code null}.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Number} value to write as byte
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullNumberAsZero() ? Numbers.BYTE_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.byteValue());
        }
    }
}
