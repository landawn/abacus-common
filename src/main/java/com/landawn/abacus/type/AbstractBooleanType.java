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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for boolean types in the type system.
 * This class provides common functionality for handling boolean values,
 * including conversion, database operations, and serialization.
 */
public abstract class AbstractBooleanType extends AbstractPrimaryType<Boolean> {

    /**
     * Constructs a new {@code AbstractBooleanType} with the specified type name.
     *
     * @param typeName the name of the boolean type (e.g., "Boolean", "boolean")
     */
    protected AbstractBooleanType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts the specified {@code Boolean} value to its string representation.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * the string representation of the boolean value ("true" or "false").
     *
     * @param b the {@code Boolean} value to convert
     * @return the string representation of the boolean value, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null) ? null : b.toString();
    }

    /**
     * Converts the specified object to a {@code Boolean} value.
     * This method handles various input types:
     * <ul>
     *   <li>{@code null} returns the default value.</li>
     *   <li>{@code Boolean} instances are returned as-is.</li>
     *   <li>Numbers are converted to {@code true} if greater than 0, {@code false} otherwise.</li>
     *   <li>{@code CharSequence} instances: single character 'Y', 'y', or '1' returns {@code true};
     *       otherwise parsed using {@link Boolean#valueOf(String)}.</li>
     *   <li>Other objects are converted via {@code Boolean.valueOf(obj.toString())}.</li>
     * </ul>
     *
     * @param obj the source object to convert
     * @return the {@code Boolean} value, or default value if the input is {@code null}
     */
    @Override
    public Boolean valueOf(final Object obj) {
        if (obj == null) {
            return defaultValue();
        }

        if (obj instanceof Boolean b) {
            return b;
        }

        if (obj instanceof Number num) {
            return num.longValue() > 0;
        }

        if (obj instanceof CharSequence) {
            return parseBoolean(obj.toString());
        }

        return Boolean.valueOf(obj.toString());
    }

    /**
     * Converts the specified string to a {@code Boolean} value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value.</li>
     *   <li>Single character strings: 'Y', 'y', or '1' return {@code true}.</li>
     *   <li>Other strings are parsed using {@link Boolean#valueOf(String)}.</li>
     * </ul>
     *
     * @param str the string to convert
     * @return the {@code Boolean} value, or default value if the input is empty or {@code null}
     */
    @Override
    public Boolean valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return defaultValue();
        }

        return parseBoolean(str);
    }

    /**
     * Converts the specified character array to a {@code Boolean} value.
     * Handles the following cases:
     * <ul>
     *   <li>{@code null} or empty array returns the default value.</li>
     *   <li>Single character 'Y', 'y', or '1' returns {@code true}.</li>
     *   <li>"true" (case-insensitive) returns {@code true}.</li>
     *   <li>Any other value returns {@code false}.</li>
     * </ul>
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return {@code true} if the array contains "true" (case-insensitive), {@code false} otherwise
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return defaultValue();
        }

        if (len == 1) {
            final char ch = cbuf[offset];
            return ch == 'Y' || ch == 'y' || ch == '1';
        }

        return ((len == 4) && (((cbuf[offset] == 't') || (cbuf[offset] == 'T')) && ((cbuf[offset + 1] == 'r') || (cbuf[offset + 1] == 'R'))
                && ((cbuf[offset + 2] == 'u') || (cbuf[offset + 2] == 'U')) && ((cbuf[offset + 3] == 'e') || (cbuf[offset + 3] == 'E')))) ? Boolean.TRUE
                        : Boolean.FALSE;
    }

    /**
     * Returns {@code true} because this type represents a boolean type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isBoolean() {
        return true;
    }

    /**
     * Returns {@code false} because boolean values do not require quoting in CSV format.
     *
     * @return {@code false}
     */
    @Override
    public boolean isCsvQuoteRequired() {
        return false;
    }

    /**
     * Retrieves a boolean value from the specified {@code ResultSet} at the given column index.
     * <p>Note: This method uses {@code rs.getBoolean()} which returns {@code false} for SQL {@code NULL} values.
     * Therefore, SQL {@code NULL} values are converted to {@code Boolean.valueOf(false)}, not {@code null}.</p>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the boolean value at the specified column, or {@code Boolean.valueOf(false)} if the value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBoolean(columnIndex);
    }

    /**
     * Retrieves a boolean value from the specified {@code ResultSet} using the given column label.
     * <p>Note: This method uses {@code rs.getBoolean()} which returns {@code false} for SQL {@code NULL} values.
     * Therefore, SQL {@code NULL} values are converted to {@code Boolean.valueOf(false)}, not {@code null}.</p>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the boolean value at the specified column, or {@code Boolean.valueOf(false)} if the value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getBoolean(columnName);
    }

    /**
     * Sets the specified boolean parameter in a {@code PreparedStatement} at the given position.
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the boolean value to set, or {@code null} for SQL {@code NULL}
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
     * Sets the specified boolean parameter in a {@code CallableStatement} using the given parameter name.
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the boolean value to set, or {@code null} for SQL {@code NULL}
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
     * Appends the string representation of the specified boolean value to the given {@code Appendable}.
     * Writes "null" if the value is {@code null}, "true" if {@code true}, or "false" if {@code false}.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the boolean value to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x ? TRUE_STRING : FALSE_STRING));
    }

    /**
     * Writes the specified boolean value to the given {@code CharacterWriter} with optional configuration.
     * If the configuration specifies {@code writeNullBooleanAsFalse} and the value is {@code null},
     * writes {@code false} instead of {@code null}.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the boolean value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, Boolean x, final JsonXmlSerConfig<?> config) throws IOException {
        x = x == null && config != null && config.isWriteNullBooleanAsFalse() ? Boolean.FALSE : x;

        writer.write((x == null) ? NULL_CHAR_ARRAY : (x ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}
