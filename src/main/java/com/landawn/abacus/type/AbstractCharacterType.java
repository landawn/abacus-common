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
import com.landawn.abacus.util.Strings;

/**
 * Abstract base class for Character types in the type system.
 * This class provides common functionality for handling Character values,
 * including conversion, database operations, and serialization.
 */
public abstract class AbstractCharacterType extends AbstractPrimaryType<Character> {

    protected AbstractCharacterType(String typeName) {
        super(typeName);
    }

    /**
     * Converts a Character value to its string representation.
     * Returns {@code null} if the input is {@code null}, otherwise returns
     * a single-character string.
     *
     * @param x the Character value to convert
     * @return the string representation of the character, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(Character x) {
        return (x == null) ? null : N.stringOf(x.charValue());
    }

    /**
     * Converts a string to a Character value.
     * This method handles various string formats:
     * <ul>
     *   <li>Empty or {@code null} strings return the default value</li>
     *   <li>Single character strings return that character</li>
     *   <li>Numeric strings are parsed as character codes</li>
     * </ul>
     *
     * @param str the string to convert
     * @return the Character value, or default value if input is empty or {@code null}
     */
    @Override
    public Character valueOf(String str) {
        // NullPointerException Here
        // return N.isEmpty(st) ? defaultValue()
        // : (char) ((st.length() == 1) ? st.charAt(0) : Integer.parseInt(st));
        if (Strings.isEmpty(str)) {
            return defaultValue();
        }

        return Strings.parseChar(str);
    }

    /**
     * Converts a character array to a Character value.
     * For single character arrays, returns that character directly.
     * For longer arrays, parses the content as a numeric character code.
     *
     * @param cbuf the character array to convert
     * @param offset the starting position in the array
     * @param len the number of characters to read
     * @return the Character value, or default value if input is {@code null} or empty
     */
    @Override
    public Character valueOf(char[] cbuf, int offset, int len) {
        // NullPointerException Here
        // return ((cbuf == null) || (len == 0)) ? defaultValue()
        // : ((len == 1) ? cbuf[offset] : (char) N.parseInt(cbuf, offset, len));
        if (N.isEmpty(cbuf) || (len == 0)) {
            return defaultValue();
        }

        return (len == 1) ? cbuf[offset] : (char) parseInt(cbuf, offset, len);
    }

    /**
     * Checks if this type represents a Character type.
     * This method always returns {@code true} for Character types.
     *
     * @return {@code true}, indicating this is a Character type
     */
    @Override
    public boolean isCharacter() {
        return true;
    }

    /**
     * Retrieves a character value from a ResultSet at the specified column index.
     * Gets the value as a string and returns the first character.
     * Returns the {@code null} character (0) if the database value is NULL.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the first character of the string value, or 0 if NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Character get(ResultSet rs, int columnIndex) throws SQLException {
        final String ret = rs.getString(columnIndex);

        if (ret == null) {
            return (char) 0;
        } else {
            return ret.charAt(0);
        }
    }

    /**
     * Retrieves a character value from a ResultSet using the specified column label.
     * Gets the value as a string and returns the first character.
     * Returns the {@code null} character (0) if the database value is NULL.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label
     * @return the first character of the string value, or 0 if NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public Character get(ResultSet rs, String columnLabel) throws SQLException {
        final String ret = rs.getString(columnLabel);

        if (ret == null) {
            return (char) 0;
        } else {
            return ret.charAt(0);
        }
    }

    /**
     * Sets a character parameter in a PreparedStatement at the specified position.
     * The character is stored as a VARCHAR in the database.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the Character value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Character x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.VARCHAR);
        } else {
            stmt.setString(columnIndex, x.toString());
        }
    }

    /**
     * Sets a character parameter in a CallableStatement using the specified parameter name.
     * The character is stored as a VARCHAR in the database.
     * If the value is {@code null}, sets the parameter to SQL NULL.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the parameter name
     * @param x the Character value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Character x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.VARCHAR);
        } else {
            stmt.setString(parameterName, x.toString());
        }
    }

    /**
     * Appends a character value to an Appendable.
     * Writes "null" if the value is {@code null}, otherwise appends the character directly.
     *
     * @param appendable the Appendable to write to
     * @param x the Character value to append
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void appendTo(Appendable appendable, Character x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x);
        }
    }

    /**
     * Writes a character value to a CharacterWriter with optional configuration.
     * If quotation is specified in the configuration, the character is wrapped in quotes.
     * Special handling is provided for single quotes when they are used as the quotation character.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Character value to write
     * @param config the serialization configuration, may be {@code null}
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeCharacter(CharacterWriter writer, Character x, JSONXMLSerializationConfig<?> config) throws IOException {
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
