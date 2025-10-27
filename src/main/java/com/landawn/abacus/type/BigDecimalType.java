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
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for BigDecimal operations.
 * This class provides serialization/deserialization and database operations
 * for java.math.BigDecimal instances with unlimited precision.
 */
public final class BigDecimalType extends NumberType<BigDecimal> {

    /**
     * The type name constant for BigDecimal type identification.
     */
    public static final String BIG_DECIMAL = BigDecimal.class.getSimpleName();

    BigDecimalType() {
        super(BIG_DECIMAL);
    }

    /**
     * Returns the Class object representing the BigDecimal class.
     *
     * @return the Class object for BigDecimal.class
     */
    @Override
    public Class<BigDecimal> clazz() {
        return BigDecimal.class;
    }

    /**
     * Converts a BigDecimal value to its string representation.
     * Uses the standard toString() method which may use scientific notation.
     *
     * @param x the BigDecimal value to convert
     * @return the string representation of the BigDecimal, or {@code null} if input is null
     */
    @MayReturnNull
    @Override

    public String stringOf(final BigDecimal x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts a string representation to a BigDecimal value.
     * Creates a BigDecimal with unlimited precision from the string.
     *
     * @param str the string to parse as a BigDecimal
     * @return a new BigDecimal parsed from the string with unlimited precision,
     *         or {@code null} if str is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a valid BigDecimal
     */
    @MayReturnNull
    @Override

    public BigDecimal valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new BigDecimal(str, MathContext.UNLIMITED);
    }

    /**
     * Creates a BigDecimal from a character array subset.
     * Constructs a new BigDecimal from the specified subset of the character array
     * with unlimited precision.
     *
     * @param cbuf the character array containing the digits
     * @param offset the starting position in the character array
     * @param len the number of characters to use
     * @return a new BigDecimal created from the specified characters with unlimited precision,
     *         or {@code null} if len is 0
     * @throws NumberFormatException if the character sequence cannot be parsed as a valid BigDecimal
     */
    @MayReturnNull
    @Override

    public BigDecimal valueOf(final char[] cbuf, final int offset, final int len) {
        return len == 0 ? null : new BigDecimal(cbuf, offset, len, MathContext.UNLIMITED);
    }

    /**
     * Retrieves a BigDecimal value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the BigDecimal value
     * @return the BigDecimal value at the specified column, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @MayReturnNull
    @Override
    public BigDecimal get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBigDecimal(columnIndex);
    }

    /**
     * Retrieves a BigDecimal value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return the BigDecimal value in the specified column, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @MayReturnNull
    @Override
    public BigDecimal get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getBigDecimal(columnLabel);
    }

    /**
     * Sets a BigDecimal parameter in a PreparedStatement at the specified position.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the BigDecimal value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final BigDecimal x) throws SQLException {
        stmt.setBigDecimal(columnIndex, x);
    }

    /**
     * Sets a named BigDecimal parameter in a CallableStatement.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the BigDecimal value to set, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final BigDecimal x) throws SQLException {
        stmt.setBigDecimal(parameterName, x);
    }

    /**
     * Writes a BigDecimal value to a CharacterWriter with optional plain string formatting.
     * Can write the value either in standard notation (which may use scientific notation)
     * or in plain string format based on the configuration.
     *
     * @param writer the CharacterWriter to write to
     * @param x the BigDecimal value to write, may be null
     * @param config the serialization configuration that may specify to write BigDecimal
     *               values in plain format (without scientific notation)
     * @throws IOException if an I/O error occurs during the write operation
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final BigDecimal x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (config != null && config.writeBigDecimalAsPlain()) {
                writer.writeCharacter(x.toPlainString());
            } else {
                writer.writeCharacter(stringOf(x));
            }
        }
    }
}
