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

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for boolean types in the type system.
 * This class provides common functionality for handling boolean values,
 * including conversion, database operations, and serialization.
 */
public abstract class AbstractBooleanType extends AbstractPrimaryType<Boolean> {

    protected AbstractBooleanType(final String typeName) {
        super(typeName);
    }

    /**
     * Checks if this type represents a boolean type.
     * This method always returns {@code true} for boolean types.
     *
     * @return {@code true}, indicating this is a boolean type
     */
    @Override
    public boolean isBoolean() {
        return true;
    }

    /**
     * Checks if this type represents values that should not be quoted in CSV format.
     * Boolean values are self-delimiting and don't require quotes in CSV files.
     *
     * @return {@code true}, indicating that boolean values should not be quoted in CSV format
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Boolean value to its string representation.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the boolean value ("true" or "false").
     *
     * @param b the Boolean value to convert
     * @return the string representation of the boolean value, or  {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null) ? null : b.toString();
    }

    /**
     * Converts an object to a Boolean value.
     * This method handles various input types:
     * <ul>
     *   <li>{@code null} returns the default value</li>
     *   <li>Boolean instances are returned as-is</li>
     *   <li>Numbers are converted to {@code true} if greater than 0, {@code false} otherwise</li>
     *   <li>Single character strings: 'Y', 'y', or '1' return {@code true}</li>
     *   <li>Other strings are parsed using {@link Boolean#valueOf(String)}</li>
     * </ul>
     *
     * @param src the source object to convert
     * @return the Boolean value, or default value if input is {@code null}
     */
    @Override
    public Boolean valueOf(final Object src) {
        if (src == null) {
            return defaultValue();
        }

        if (src instanceof Boolean b) {
            return b;
        }

        if (src instanceof Number num) {
            return num.longValue() > 0;
        }

        if (src instanceof CharSequence) {
            return parseBoolean(src.toString());
        }

        return Boolean.valueOf(src.toString());
    }

    /**
     * Converts a string to a Boolean value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Single character strings: 'Y', 'y', or '1' return {@code true}</li>
     *   <li>Other strings are parsed using {@link Boolean#valueOf(String)}</li>
     * </ul>
     *
     * @param str the string to convert
     * @return the Boolean value, or default value if input is empty or {@code null}
     */
    @Override
    public Boolean valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return defaultValue();
        }

        return parseBoolean(str);
    }

    /**
     * Converts a character array to a Boolean value.
     * This method checks if the character array contains "true" (case-insensitive).
     * Any other value, including {@code null} or empty array, returns {@code false}.
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return {@code true} if the array contains "true" (case-insensitive), {@code false} otherwise
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        return ((cbuf == null) || (len == 0)) ? defaultValue()
                : (((len == 4) && (((cbuf[offset] == 't') || (cbuf[offset] == 'T')) && ((cbuf[offset + 1] == 'r') || (cbuf[offset + 1] == 'R'))
                        && ((cbuf[offset + 2] == 'u') || (cbuf[offset + 2] == 'U')) && ((cbuf[offset + 3] == 'e') || (cbuf[offset + 3] == 'E')))) ? Boolean.TRUE
                                : Boolean.FALSE);
    }

    /**
     * Retrieves a boolean value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the boolean value at the specified column
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBoolean(columnIndex);
    }

    /**
     * Retrieves a boolean value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label
     * @return the boolean value at the specified column
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getBoolean(columnLabel);
    }

    /**
     * Sets a boolean parameter in a PreparedStatement at the specified position.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the boolean value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x);
        }
    }

    /**
     * Sets a boolean parameter in a CallableStatement using the specified parameter name.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the boolean value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x);
        }
    }

    /**
     * Appends the string representation of a boolean value to an Appendable.
     * Writes "null" if the value is {@code null}, "true" if {@code true}, or "false" if {@code false}.
     *
     * @param appendable the Appendable to write to
     * @param x the boolean value to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes a boolean value to a CharacterWriter with optional configuration.
     * If the configuration specifies {@code writeNullBooleanAsFalse} and the value is {@code null},
     * writes {@code false} instead of {@code null}.
     *
     * @param writer the CharacterWriter to write to
     * @param x the boolean value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Boolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullBooleanAsFalse() ? Boolean.FALSE : x;

        writer.write((x == null) ? NULL_CHAR_ARRAY : (x ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}