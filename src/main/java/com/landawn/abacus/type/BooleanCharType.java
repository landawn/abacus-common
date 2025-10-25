/*
 * Copyright (C) 2019 HaiYang Li
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

/**
 * Type handler for Boolean values represented as single characters.
 * This class maps Boolean values to 'Y' (true) and 'N' (false) characters,
 * providing compatibility with database systems that store boolean flags
 * as character fields.
 */
@SuppressWarnings("java:S2160")
public final class BooleanCharType extends AbstractType<Boolean> {

    private static final String typeName = "BooleanChar";
    private static final String Y = "Y";
    private static final String N = "N";

    BooleanCharType() {
        super(typeName);
    }

    /**
     * Returns the Class object representing the Boolean class.
     *
     * @return the Class object for Boolean.class
     */
    @Override
    public Class<Boolean> clazz() {
        return Boolean.class;
    }

    /**
     * Returns the default value for this boolean type.
     *
     * @return Boolean.FALSE as the default value
     */
    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }

    /**
     * Determines whether this type should be quoted in CSV format.
     * Boolean character values are typically not quoted in CSV files.
     *
     * @return {@code true} indicating that 'Y'/'N' values should not be quoted in CSV format
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Boolean value to its character string representation.
     * Maps true to "Y" and false/null to "N".
     *
     * @param b the Boolean value to convert
     * @return "Y" if b is true, "N" if b is false or null
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null || !b) ? N : Y;
    }

    /**
     * Converts a string representation to a Boolean value.
     * Case-insensitive parsing where "Y" maps to true, all other values to false.
     *
     * @param str the string to parse (typically "Y" or "N")
     * @return Boolean.TRUE if str equals "Y" (case-insensitive), Boolean.FALSE otherwise
     */
    @Override
    public Boolean valueOf(final String str) {
        return Y.equalsIgnoreCase(str) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Converts a character array subset to a Boolean value.
     * Checks if the single character is 'Y' or 'y' for true.
     *
     * @param cbuf the character array containing the character
     * @param offset the starting position in the character array
     * @param len the number of characters to examine (should be 1)
     * @return Boolean.TRUE if the single character is 'Y' or 'y', Boolean.FALSE otherwise
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        return (cbuf == null || len == 0) ? Boolean.FALSE : ((len == 1 && (cbuf[offset] == 'Y' || cbuf[offset] == 'y')) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Retrieves a Boolean value from a ResultSet at the specified column index.
     * Reads a string value and converts it using the Y/N mapping.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the character value
     * @return Boolean.TRUE if the value is "Y" (case-insensitive), Boolean.FALSE otherwise
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     * Retrieves a Boolean value from a ResultSet using the specified column label.
     * Reads a string value and converts it using the Y/N mapping.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return Boolean.TRUE if the value is "Y" (case-insensitive), Boolean.FALSE otherwise
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        return valueOf(rs.getString(columnLabel));
    }

    /**
     * Sets a Boolean parameter in a PreparedStatement at the specified position.
     * Converts the Boolean to "Y" (true) or "N" (false) before setting.
     * Null values are set as SQL NULL with BOOLEAN type.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Boolean value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setString(columnIndex, x ? Y : N);
        }
    }

    /**
     * Sets a named Boolean parameter in a CallableStatement.
     * Converts the Boolean to "Y" (true) or "N" (false) before setting.
     * Null values are set as SQL NULL with BOOLEAN type.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Boolean value to set, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setString(parameterName, x ? Y : N);
        }
    }

    /**
     * Appends a Boolean value to an Appendable object as a Y/N character.
     *
     * @param appendable the Appendable object to append to
     * @param x the Boolean value to append
     * @throws IOException if an I/O error occurs during the append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws IOException {
        appendable.append(stringOf(x));
    }

    /**
     * Writes a Boolean value to a CharacterWriter as a Y/N character.
     * Applies optional character quotation based on the configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Boolean value to write
     * @param config the serialization configuration that may specify character quotation
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Boolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        final char ch = config == null ? 0 : config.getCharQuotation();

        if (ch == 0) {
            writer.write(stringOf(x));
        } else {
            writer.write(ch);
            writer.write(stringOf(x));
            writer.write(ch);
        }
    }
}