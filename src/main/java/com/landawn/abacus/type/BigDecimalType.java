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

    /**
     * Package-private constructor for BigDecimalType.
     * This constructor is called by the TypeFactory to create BigDecimal type instances.
     */
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * String result = type.stringOf(new BigDecimal("123.45"));   // returns "123.45"
     * String nullResult = type.stringOf(null);                   // returns null
     * }</pre>
     *
     * @param x the BigDecimal value to convert, may be {@code null}
     * @return the string representation of the BigDecimal, or {@code null} if input is {@code null}
     */
    @Override
    public String stringOf(final BigDecimal x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Converts a string representation to a BigDecimal value.
     * Creates a BigDecimal with unlimited precision from the string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * BigDecimal result = type.valueOf("123.456");   // returns BigDecimal with value 123.456
     * BigDecimal nullResult = type.valueOf(null);    // returns null
     * BigDecimal emptyResult = type.valueOf("");     // returns null
     * }</pre>
     *
     * @param str the string to parse as a BigDecimal, may be {@code null}
     * @return a new BigDecimal parsed from the string with unlimited precision,
     *         or {@code null} if str is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a valid BigDecimal
     */
    @Override
    public BigDecimal valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new BigDecimal(str, MathContext.UNLIMITED);
    }

    /**
     * Creates a BigDecimal from a character array subset.
     * Constructs a new BigDecimal from the specified subset of the character array
     * with unlimited precision.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * char[] chars = "123.45".toCharArray();
     * BigDecimal result = type.valueOf(chars, 0, 6);    // returns BigDecimal with value 123.45
     * BigDecimal zeroLen = type.valueOf(chars, 0, 0);   // returns null
     * }</pre>
     *
     * @param cbuf the character array containing the digits, must not be {@code null}
     * @param offset the starting position in the character array (0-based)
     * @param len the number of characters to use
     * @return a new BigDecimal created from the specified characters with unlimited precision,
     *         or {@code null} if len is 0
     * @throws NumberFormatException if the character sequence cannot be parsed as a valid BigDecimal
     */
    @Override
    public BigDecimal valueOf(final char[] cbuf, final int offset, final int len) {
        return len == 0 ? null : new BigDecimal(cbuf, offset, len, MathContext.UNLIMITED);
    }

    /**
     * Retrieves a BigDecimal value from a ResultSet at the specified column index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * ResultSet rs = ...;  // from SQL query
     * BigDecimal price = type.get(rs, 1);   // retrieves BigDecimal from column 1
     * }</pre>
     *
     * @param rs the ResultSet to retrieve the value from, must not be {@code null}
     * @param columnIndex the column index (1-based) of the BigDecimal value
     * @return the BigDecimal value at the specified column, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public BigDecimal get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBigDecimal(columnIndex);
    }

    /**
     * Retrieves a BigDecimal value from a ResultSet using the specified column label.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * ResultSet rs = ...;  // from SQL query
     * BigDecimal price = type.get(rs, "price");   // retrieves BigDecimal from "price" column
     * }</pre>
     *
     * @param rs the ResultSet to retrieve the value from, must not be {@code null}
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified, must not be {@code null}
     * @return the BigDecimal value in the specified column, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public BigDecimal get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getBigDecimal(columnLabel);
    }

    /**
     * Sets a BigDecimal parameter in a PreparedStatement at the specified position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * PreparedStatement stmt = conn.prepareStatement("UPDATE products SET price = ? WHERE id = ?");
     * type.set(stmt, 1, new BigDecimal("99.99"));   // sets parameter 1 to 99.99
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on, must not be {@code null}
     * @param columnIndex the parameter index (1-based) to set
     * @param x the BigDecimal value to set, may be {@code null}
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final BigDecimal x) throws SQLException {
        stmt.setBigDecimal(columnIndex, x);
    }

    /**
     * Sets a named BigDecimal parameter in a CallableStatement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * CallableStatement stmt = conn.prepareCall("{call updatePrice(?, ?)}");
     * type.set(stmt, "price", new BigDecimal("99.99"));   // sets named parameter "price"
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on, must not be {@code null}
     * @param parameterName the name of the parameter to set, must not be {@code null}
     * @param x the BigDecimal value to set, may be {@code null}
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigDecimal> type = TypeFactory.getType(BigDecimal.class);
     * CharacterWriter writer = new CharacterWriter();
     * type.writeCharacter(writer, new BigDecimal("123.45"), null);   // writes "123.45"
     * type.writeCharacter(writer, null, null);                       // writes "null"
     * }</pre>
     *
     * @param writer the CharacterWriter to write to, must not be {@code null}
     * @param x the BigDecimal value to write, may be {@code null}
     * @param config the serialization configuration that may specify to write BigDecimal
     *               values in plain format (without scientific notation), may be {@code null}
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
