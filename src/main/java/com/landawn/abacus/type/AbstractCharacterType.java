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
import com.landawn.abacus.util.Strings;

/**
 * The abstract base class for {@code Character} types in the type system.
 * This class provides common functionality for handling {@code Character} values,
 * including conversion, database operations, and serialization.
 */
public abstract class AbstractCharacterType extends AbstractPrimaryType<Character> {

    /**
     * Constructs a new {@code AbstractCharacterType} with the specified type name.
     *
     * @param typeName the name of the {@code Character} type (e.g., "Character", "char")
     */
    protected AbstractCharacterType(final String typeName) {
        super(typeName);
    }

    /**
     * Converts the specified {@code Character} value to its string representation.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * a single-character string.
     *
     * @param x the {@code Character} value to convert
     * @return the string representation of the character, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final Character x) {
        return (x == null) ? null : N.stringOf(x.charValue());
    }

    /**
     * Converts the specified string to a {@code Character} value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value.</li>
     *   <li>Single character strings return that character.</li>
     *   <li>Numeric strings are parsed as character codes.</li>
     * </ul>
     *
     * @param str the string to convert
     * @return the {@code Character} value, or default value if the input is empty or {@code null}
     */
    @Override
    public Character valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return defaultValue();
        }

        return Strings.parseChar(str);
    }

    /**
     * Converts the specified character array to a {@code Character} value.
     * <p>For single character arrays, returns that character directly.
     * For longer arrays, parses the content as a numeric character code.</p>
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the {@code Character} value, or default value if the input is {@code null} or empty
     */
    @Override
    public Character valueOf(final char[] cbuf, final int offset, final int len) {
        if (N.isEmpty(cbuf) || (len == 0)) {
            return defaultValue();
        }

        return (len == 1) ? cbuf[offset] : (char) parseInt(cbuf, offset, len);
    }

    /**
     * Returns {@code true} because this type represents a {@code Character} type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCharacter() {
        return true;
    }

    /**
     * Retrieves a character value from the specified {@code ResultSet} at the given column index.
     * <p>Gets the value as a string and returns the first character.
     * Returns the default value if the database value is {@code NULL} or an empty string.</p>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the column index (1-based)
     * @return the first character of the string value, or the default value if {@code NULL} or empty
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Character get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String result = rs.getString(columnIndex);

        if (result == null || result.isEmpty()) {
            return defaultValue();
        } else {
            return result.charAt(0);
        }
    }

    /**
     * Retrieves a character value from the specified {@code ResultSet} using the given column label.
     * <p>Gets the value as a string and returns the first character.
     * Returns the default value if the database value is {@code NULL} or an empty string.</p>
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label
     * @return the first character of the string value, or the default value if {@code NULL} or empty
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Character get(final ResultSet rs, final String columnName) throws SQLException {
        final String result = rs.getString(columnName);

        if (result == null || result.isEmpty()) {
            return defaultValue();
        } else {
            return result.charAt(0);
        }
    }

    /**
     * Sets the specified character parameter in a {@code PreparedStatement} at the given position.
     * <p>The character is stored as a {@code VARCHAR} in the database.</p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the {@code Character} value to set, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Character x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.VARCHAR);
        } else {
            stmt.setString(columnIndex, x.toString());
        }
    }

    /**
     * Sets the specified character parameter in a {@code CallableStatement} using the given parameter name.
     * <p>The character is stored as a {@code VARCHAR} in the database.</p>
     * If the value is {@code null}, sets the parameter to SQL {@code NULL}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the parameter name
     * @param x the {@code Character} value to set, or {@code null} for SQL {@code NULL}
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Character x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.VARCHAR);
        } else {
            stmt.setString(parameterName, x.toString());
        }
    }

    /**
     * Appends the specified character value to the given {@code Appendable}.
     * Writes "null" if the value is {@code null}, otherwise appends the character directly.
     *
     * @param appendable the {@code Appendable} to write to
     * @param x the {@code Character} value to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(final Appendable appendable, final Character x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x);
        }
    }

    /**
     * Writes the specified character value to the given {@code CharacterWriter} with optional configuration.
     * <p>If quotation is specified in the configuration, the character is wrapped in quotes.
     * Special handling is provided for single quotes when they are used as the quotation character.</p>
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Character} value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Character x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getCharQuotation();

            if (ch == 0) {
                writer.writeCharacter(x);
            } else {
                writer.write(ch);

                if (x == '\'' && ch == '\'') {
                    writer.write('\\');
                }

                writer.writeCharacter(x);
                writer.write(ch);
            }
        }
    }
}
