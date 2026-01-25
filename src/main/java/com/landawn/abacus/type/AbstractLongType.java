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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for long types in the type system.
 * This class provides common functionality for handling long values,
 * including conversion from various date/time types, database operations, and serialization.
 * Note that this class uses Number as its generic type to allow for both
 * primitive long and Long wrapper handling.
 */
public abstract class AbstractLongType extends NumberType<Number> {

    /**
     * Constructs an AbstractLongType with the specified type name.
     *
     * @param typeName the name of the long type (e.g., "Long", "long")
     */
    protected AbstractLongType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts a Number value to its string representation as a long.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the long value.
     *
     * @param x the Number value to convert
     * @return the string representation of the long value, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.longValue());
    }

    /**
     * Converts an object to a Long value.
     * This method handles various input types:
     * <ul>
     *   <li>{@code null} returns the default value</li>
     *   <li>Date objects return their time in milliseconds</li>
     *   <li>Calendar objects return their time in milliseconds</li>
     *   <li>Instant objects return their epoch milliseconds</li>
     *   <li>ZonedDateTime objects return their epoch milliseconds</li>
     *   <li>LocalDateTime objects are converted to Timestamp then to milliseconds</li>
     *   <li>Other objects are converted to string and parsed</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractLongType type = TypeFactory.getType(Long.class);
     * Long value1 = type.valueOf(new Date());      // returns timestamp in milliseconds
     * Long value2 = type.valueOf(Instant.now());   // returns epoch milliseconds
     * Long value3 = type.valueOf("1234567890");    // returns 1234567890L
     * }</pre>
     *
     * @param obj the object to convert
     * @return the Long value representing milliseconds for date/time types, or parsed value for others
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
     * Converts a string to a Long value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Strings ending with 'l', 'L', 'f', 'F', 'd', or 'D' have the suffix stripped before parsing</li>
     *   <li>Valid numeric strings are parsed to long values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractLongType type = TypeFactory.getType(Long.class);
     * Long value1 = type.valueOf("1234567890");    // returns 1234567890L
     * Long value2 = type.valueOf("9999999999L");   // returns 9999999999L (suffix stripped)
     * Long value3 = type.valueOf("42");            // returns 42L
     * Long value4 = type.valueOf("");              // returns default value
     * }</pre>
     *
     * @param str the string to convert
     * @return the Long value
     * @throws NumberFormatException if the string cannot be parsed as a long
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
     * Converts a character array to a Long value.
     * Delegates to the {@link #parseLong(char[], int, int)} method for parsing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractLongType type = TypeFactory.getType(Long.class);
     * char[] buffer = "9876543210".toCharArray();
     * Long value = type.valueOf(buffer, 0, 10);   // returns 9876543210L
     * }</pre>
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the Long value, or default value if input is {@code null} or empty
     */
    @Override
    public Long valueOf(final char[] cbuf, final int offset, final int len) {
        return ((cbuf == null) || (len == 0)) ? ((Long) defaultValue()) : (Long) parseLong(cbuf, offset, len);
    }

    /**
     * Checks if this type represents a long type.
     * This method always returns {@code true} for long types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * AbstractLongType type = TypeFactory.getType(Long.class);
     * if (type.isLong()) {
     *     // Handle long type specific logic
     *     System.out.println("This is a long type");
     * }
     * }</pre>
     *
     * @return {@code true}, indicating this is a long type
     */
    @Override
    public boolean isLong() {
        return true;
    }

    /**
     * Retrieves a long value from a ResultSet at the specified column index.
     * This method uses rs.getLong() which returns 0 for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive long types
     * PrimitiveLongType type = TypeFactory.getType(long.class);
     * long value = type.get(rs, 1);   // Returns 0L for SQL NULL
     *
     * // For wrapper Long types
     * LongType type = TypeFactory.getType(Long.class);
     * Long value = type.get(rs, 1);   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the long value at the specified column; returns 0L if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Long get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getLong(columnIndex);
    }

    /**
     * Retrieves a long value from a ResultSet using the specified column label.
     * This method uses rs.getLong() which returns 0 for SQL NULL values.
     * Subclasses may override this to return {@code null} for SQL NULL values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For primitive long types
     * PrimitiveLongType type = TypeFactory.getType(long.class);
     * long value = type.get(rs, "timestamp");   // Returns 0L for SQL NULL
     *
     * // For wrapper Long types
     * LongType type = TypeFactory.getType(Long.class);
     * Long value = type.get(rs, "timestamp");   // Returns null for SQL NULL (overridden in subclass)
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label
     * @return the long value at the specified column; returns 0L if SQL NULL (may be overridden by subclasses to return null)
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public Long get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getLong(columnLabel);
    }

    /**
     * Sets a long parameter in a PreparedStatement at the specified position.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a long value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Number value to set as long, or {@code null} for SQL NULL
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
     * Sets a long parameter in a CallableStatement using the specified parameter name.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     * Otherwise, converts the Number to a long value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the Number value to set as long, or {@code null} for SQL NULL
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
     * Appends the string representation of a long value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise writes the numeric value.
     *
     * @param appendable the Appendable to write to
     * @param x the Number value to append as long
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
     * Writes a long value to a CharacterWriter with optional configuration.
     * If the configuration specifies {@code writeNullNumberAsZero} and the value is {@code null},
     * writes 0L instead of {@code null}. If the configuration specifies {@code writeLongAsString},
     * the long value is wrapped in quotation marks.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Number value to write as long
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Number x, final JsonXmlSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullNumberAsZero() ? Numbers.LONG_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (config != null && config.writeLongAsString() && config.getStringQuotation() != 0) {
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
