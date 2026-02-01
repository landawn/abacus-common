/*
 * Copyright (C) 2024 HaiYang Li
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

import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

/**
 * Type handler for Boolean values represented as integers.
 * This class maps Boolean values to 1 (true) and 0 (false) integers,
 * providing compatibility with database systems that store boolean flags
 * as numeric fields.
 */
@SuppressWarnings("java:S2160")
public final class BooleanIntType extends AbstractType<Boolean> {

    private static final String typeName = "BooleanInt";
    private static final String _0 = "0";
    private static final String _1 = "1";

    /**
     * Package-private constructor for BooleanIntType.
     * This constructor is called by the TypeFactory to create BooleanInt type instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Obtained via TypeFactory
     * Type<Boolean> type = TypeFactory.getType("BooleanInt");
     * Boolean value = type.valueOf("1");     // Boolean.TRUE
     * String result = type.stringOf(true);   // "1"
     * }</pre>
     */
    BooleanIntType() {
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
     * Returns the default value for the BooleanInt type.
     * The default Boolean value is {@code false}.
     *
     * @return Boolean.FALSE as the default value
     */
    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }

    /**
     * Determines whether this type should be quoted in CSV format.
     * Boolean integer values are typically not quoted in CSV files.
     *
     * @return {@code true} indicating that 0/1 values should not be quoted in CSV format
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     * Converts a Boolean value to its integer string representation.
     * Maps {@code true} to "1" and false/null to "0".
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Boolean> type = TypeFactory.getType("BooleanInt");
     * String result1 = type.stringOf(Boolean.TRUE);    // "1"
     * String result2 = type.stringOf(Boolean.FALSE);   // "0"
     * String result3 = type.stringOf(null);            // "0"
     * }</pre>
     *
     * @param b the Boolean value to convert
     * @return "1" if b is {@code true}, "0" if b is {@code false} or null
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null || !b) ? _0 : _1;
    }

    /**
     * Converts a string representation to a Boolean value.
     * Parses "1" as {@code true}, all other values as {@code false}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Boolean> type = TypeFactory.getType("BooleanInt");
     * Boolean result1 = type.valueOf("1");     // Boolean.TRUE
     * Boolean result2 = type.valueOf("0");     // Boolean.FALSE
     * Boolean result3 = type.valueOf("xyz");   // Boolean.FALSE
     * Boolean result4 = type.valueOf(null);    // Boolean.FALSE
     * }</pre>
     *
     * @param str the string to parse (typically "0" or "1")
     * @return Boolean.TRUE if str equals "1", Boolean.FALSE otherwise
     */
    @Override
    public Boolean valueOf(final String str) {
        return _1.equals(str) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Converts a character array subset to a Boolean value.
     * Checks if the single character is '1' for {@code true}.
     *
     * @param cbuf the character array containing the character
     * @param offset the starting position in the character array
     * @param len the number of characters to examine (should be 1)
     * @return Boolean.TRUE if the single character is '1', Boolean.FALSE otherwise
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        return (cbuf == null || len == 0) ? Boolean.FALSE : ((len == 1 && (cbuf[offset] == '1')) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Retrieves a Boolean value from a ResultSet at the specified column index.
     * Reads an integer value and converts it to Boolean (positive values are true).
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the integer value
     * @return Boolean.TRUE if the integer value is greater than 0, Boolean.FALSE otherwise
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getInt(columnIndex) > 0;
    }

    /**
     * Retrieves a Boolean value from a ResultSet using the specified column label.
     * Reads an integer value and converts it to Boolean (positive values are true).
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return Boolean.TRUE if the integer value is greater than 0, Boolean.FALSE otherwise
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getInt(columnLabel) > 0;
    }

    /**
     * Sets a Boolean parameter in a PreparedStatement at the specified position.
     * Converts the Boolean to "1" (true) or "0" (false) before setting.
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
            stmt.setNull(columnIndex, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(columnIndex, x ? 1 : 0);
        }
    }

    /**
     * Sets a named Boolean parameter in a CallableStatement.
     * Converts the Boolean to "1" (true) or "0" (false) before setting.
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
            stmt.setNull(parameterName, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(parameterName, x ? 1 : 0);
        }
    }

    /**
     * Appends a Boolean value to an Appendable object as a 0/1 character.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Boolean> type = TypeFactory.getType("BooleanInt");
     * StringBuilder sb = new StringBuilder();
     * type.appendTo(sb, Boolean.TRUE);    // appends "1"
     * type.appendTo(sb, Boolean.FALSE);   // appends "0"
     * // sb.toString() == "10"
     * }</pre>
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
     * Writes a Boolean value to a CharacterWriter as a 0/1 character.
     * Applies optional character quotation based on the configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the Boolean value to write
     * @param config the serialization configuration that may specify character quotation
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Boolean x, final JsonXmlSerializationConfig<?> config) throws IOException {
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