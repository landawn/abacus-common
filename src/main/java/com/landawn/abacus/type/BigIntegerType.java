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

import java.math.BigInteger;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.util.Strings;

/**
 * Type handler for BigInteger operations.
 * This class provides serialization/deserialization and database operations
 * for java.math.BigInteger instances. BigInteger values are stored as strings
 * in the database to preserve precision.
 */
public final class BigIntegerType extends NumberType<BigInteger> {

    /**
     * The type name constant for BigInteger type identification.
     */
    public static final String BIG_INTEGER = BigInteger.class.getSimpleName();

    /**
     * Package-private constructor for BigIntegerType.
     * This constructor is called by the TypeFactory to create BigInteger type instances.
     */
    BigIntegerType() {
        super(BIG_INTEGER);
    }

    /**
     * Returns the Class object representing the BigInteger class.
     *
     * @return the Class object for BigInteger.class
     */
    @Override
    public Class<BigInteger> clazz() {
        return BigInteger.class;
    }

    /**
     * Converts a BigInteger value to its string representation in base 10.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigInteger> type = TypeFactory.getType(BigInteger.class);
     * BigInteger value = new BigInteger("12345678901234567890");
     * String str = type.stringOf(value);      // returns "12345678901234567890"
     * String nullStr = type.stringOf(null);   // returns null
     * }</pre>
     *
     * @param x the BigInteger value to convert
     * @return the string representation of the BigInteger in decimal format,
     *         or {@code null} if input is null
     */
    @Override
    public String stringOf(final BigInteger x) {
        return (x == null) ? null : x.toString(10);
    }

    /**
     * Converts a string representation to a BigInteger value.
     * Parses the string as a base 10 integer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigInteger> type = TypeFactory.getType(BigInteger.class);
     * BigInteger value = type.valueOf("12345678901234567890");
     * BigInteger nullValue = type.valueOf(null);   // returns null
     * BigInteger emptyValue = type.valueOf("");    // returns null
     * }</pre>
     *
     * @param str the string to parse as a BigInteger in decimal format
     * @return a new BigInteger parsed from the string, or {@code null} if str is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a valid BigInteger
     */
    @Override
    public BigInteger valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new BigInteger(str.trim(), 10);
    }

    /**
     * Retrieves a BigInteger value from a ResultSet at the specified column index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigInteger> type = TypeFactory.getType(BigInteger.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * BigInteger largeId = type.get(rs, 1);   // retrieves BigInteger from column 1
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return the BigInteger value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public BigInteger get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String stringValue = rs.getString(columnIndex);

        return Strings.isEmpty(stringValue) ? null : new BigInteger(stringValue.trim());
    }

    /**
     * Retrieves a BigInteger value from a ResultSet using the specified column label.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigInteger> type = TypeFactory.getType(BigInteger.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     * BigInteger largeId = type.get(rs, "large_id");   // retrieves BigInteger from "large_id" column
     * }</pre>
     *
     * @param rs the ResultSet containing the data, must not be {@code null}
     * @param columnName the label of the column to retrieve the value from, must not be {@code null}
     * @return the BigInteger value at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public BigInteger get(final ResultSet rs, final String columnName) throws SQLException {
        final String stringValue = rs.getString(columnName);

        return Strings.isEmpty(stringValue) ? null : new BigInteger(stringValue.trim());
    }

    /**
     * Sets a BigInteger parameter in a PreparedStatement at the specified position.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigInteger> type = TypeFactory.getType(BigInteger.class);
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO large_nums (id, value) VALUES (?, ?)");
     * type.set(stmt, 1, new BigInteger("12345678901234567890"));
     * stmt.executeUpdate();
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on, must not be {@code null}
     * @param columnIndex the parameter index (1-based) to set
     * @param x the BigInteger value to set, may be {@code null}
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final BigInteger x) throws SQLException {
        stmt.setString(columnIndex, (x == null) ? null : x.toString());
    }

    /**
     * Sets a named BigInteger parameter in a CallableStatement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<BigInteger> type = TypeFactory.getType(BigInteger.class);
     * CallableStatement stmt = conn.prepareCall("{call processLargeNumber(?)}");
     * type.set(stmt, "largeNum", new BigInteger("12345678901234567890"));
     * stmt.execute();
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on, must not be {@code null}
     * @param parameterName the name of the parameter to set, must not be {@code null}
     * @param x the BigInteger value to set, may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final BigInteger x) throws SQLException {
        stmt.setString(parameterName, (x == null) ? null : x.toString());
    }
}