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
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * The Abstract base class for {@code long} types in the type system.
 * <p>
 * This class provides common functionality for handling {@code long} values,
 * including conversion from various date/time types, database operations, and serialization.
 * Note that this class uses {@code Number} as its generic type to allow for both
 * primitive {@code long} and {@code Long} wrapper handling.
 * </p>
 */
public abstract class AbstractLongType extends NumberType<Number> {

    /**
     * Constructs an {@code AbstractLongType} with the specified type name.
     *
     * @param typeName the name of the long type (e.g., "Long", "long")
     */
    protected AbstractLongType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a {@code Number} value to its string representation as a {@code long}.
     * <p>
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the {@code long} value.
     * </p>
     *
     * @param x the {@code Number} value to convert
     * @return the string representation of the {@code long} value, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.longValue());
    }

    /**
     * Converts an object to a {@code Long} value.
     * <p>
     * This method handles various input types:
     * </p>
     * <ul>
     *   <li>{@code null} returns the default value</li>
     *   <li>{@code Date} objects return their time in milliseconds</li>
     *   <li>{@code Calendar} objects return their time in milliseconds</li>
     *   <li>{@code Instant} objects return their epoch milliseconds</li>
     *   <li>{@code ZonedDateTime} objects return their epoch milliseconds</li>
     *   <li>{@code LocalDateTime} objects are converted to {@code Timestamp} then to milliseconds</li>
     *   <li>Other objects are converted to string and parsed</li>
     * </ul>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(Long.class);
     * Long value1 = type.valueOf(new Date());      // returns timestamp in milliseconds
     * Long value2 = type.valueOf(Instant.now());   // returns epoch milliseconds
     * Long value3 = type.valueOf("1234567890");    // returns 1234567890L
     * }</pre>
     *
     * @param obj the object to convert
     * @return the {@code Long} value representing milliseconds for date/time types, or parsed value for others
     */
    @Override
    public Long valueOf(final Object obj) {
        if (obj == null) {
            return (Long) defaultValue();
        }

        if (obj instanceof Date) {
            return ((Date) obj).getTime();
        } else if (obj instanceof Calendar) {
            return ((Calendar) obj).getTimeInMillis();
        } else if (obj instanceof Instant) {
            return ((Instant) obj).toEpochMilli();
        } else if (obj instanceof ZonedDateTime) {
            return ((ZonedDateTime) obj).toInstant().toEpochMilli();
        } else if (obj instanceof LocalDateTime) {
            return Timestamp.valueOf((LocalDateTime) obj).getTime();
        }

        return valueOf(Type.<Object> of(obj.getClass()).stringOf(obj));
    }

    /**
     * Converts a string to a {@code Long} value.
     * <p>
     * This method handles various string formats:
     * </p>
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing</li>
     *   <li>Valid numeric strings are parsed to {@code long} values</li>
     * </ul>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(Long.class);
     * Long value1 = type.valueOf("1234567890");    // returns 1234567890L
     * Long value2 = type.valueOf("9999999999L");   // returns 9999999999L (suffix stripped)
     * Long value3 = type.valueOf("42");            // returns 42L
     * Long value4 = type.valueOf("");              // returns default value
     * }</pre>
     *
     * @param str the string to convert
     * @return the {@code Long} value
     * @throws NumberFormatException if the string cannot be parsed as a {@code long}
     */
    @Override
    public Long valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return (Long) defaultValue();
        }

        try {
            return Numbers.toLong(str);
        } catch (final NumberFormatException e) {
            if (str.length() > 1) {
                final char ch = str.charAt(str.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Numbers.toLong(str.substring(0, str.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     * Converts a character array to a {@code Long} value.
     * <p>
     * Delegates to the {@link #parseLong(char[], int, int)} method for parsing.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(Long.class);
     * char[] buffer = "9876543210".toCharArray();
     * Long value = type.valueOf(buffer, 0, 10);   // returns 9876543210L
     * }</pre>
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the {@code Long} value, or default value if input is {@code null} or empty
     */
    @Override
    public Long valueOf(final char[] cbuf, final int offset, final int len) {
        return ((cbuf == null) || (len == 0)) ? ((Long) defaultValue()) : (Long) parseLong(cbuf, offset, len);
    }

    /**
     * Checks if this type represents a {@code long} type.
     * <p>
     * This method always returns {@code true} for {@code long} types.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * Type<Long> type = TypeFactory.getType(Long.class);
     * if (type.isLong()) {
     *     // Handle long type specific logic
     *     System.out.println("This is a long type");
     * }
     * }</pre>
     *
     * @return {@code true}, indicating this is a {@code long} type
     */
    @Override
    public boolean isLong() {
        return true;
    }

    /**
     * Retrieves a {@code long} value from a {@code ResultSet} at the specified column index.
     * <p>
     * This method uses {@code rs.getLong()} which returns {@code 0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * // For primitive long types
     * Type<Long> type = TypeFactory.getType(long.class);
     * long value = type.get(rs, 1);   // Returns 0L for SQL NULL
     *
     * // For wrapper Long types
     * Type<Long> type = TypeFactory.getType(Long.class);
     * Long value = type.get(rs, 1);   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the {@code long} value at the specified column; returns {@code 0L} if SQL {@code NULL} (may be overridden by subclasses to return {@code null})
     * @throws SQLException if a database access error occurs or the {@code columnIndex} is invalid
     */
    @Override
    public Long get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getLong(columnIndex);
    }

    /**
     * Retrieves a {@code long} value from a {@code ResultSet} using the specified column label.
     * <p>
     * This method uses {@code rs.getLong()} which returns {@code 0} for SQL {@code NULL} values.
     * Subclasses may override this to return {@code null} for SQL {@code NULL} values.
     * </p>
     *
     * <p>Usage Examples:</p>
     * <pre>{@code
     * // For primitive long types
     * Type<Long> type = TypeFactory.getType(long.class);
     * long value = type.get(rs, "timestamp");   // Returns 0L for SQL NULL
     *
     * // For wrapper Long types
     * Type<Long> type = TypeFactory.getType(Long.class);
     * Long value = type.get(rs, "timestamp");   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the {@code long} value at the specified column; returns {@code 0L} if SQL {@code NULL} (may be overridden by subclasses to return {@code null})
     * @throws SQLException if a database access error occurs or the {@code columnName} is not found
     */
    @Override
    public Long get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getLong(columnName);
    }

    /**
     * Sets a {@code long} parameter in a {@code PreparedStatement} at the specified position.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code long} value.
     * </p>
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Number} value to set as {@code long}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.longValue());
        }
    }

    /**
     * Sets a {@code long} parameter in a {@code CallableStatement} using the specified parameter name.
     * <p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     * Otherwise, converts the {@code Number} to a {@code long} value.
     * </p>
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Number} value to set as {@code long}, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.longValue());
        }
    }

    /**
     * Appends the string representation of a {@code long} value to an {@code Appendable}.
     * <p>
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value.
     * </p>
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Number} value to append as {@code long}
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
     * Writes a {@code long} value to a {@code CharacterWriter} with optional configuration.
     * <p>
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes {@code 0L} instead of {@code null}. If the configuration specifies {@code writeLongAsString},
     * the long value is wrapped in quotation marks.
     * </p>
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Number} value to write as {@code long}
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullNumberAsZero() ? Numbers.LONG_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (config != null && config.isWriteLongAsString() && config.getStringQuotation() != 0) {
                final char ch = config.getStringQuotation();

                writer.write(ch);
                writer.write(x.longValue());
                writer.write(ch);
            } else {
                writer.write(x.longValue());
            }
        }
    }
}
