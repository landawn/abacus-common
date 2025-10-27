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

import com.landawn.abacus.annotation.MayReturnNull;
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
     * @param x the BigInteger value to convert
     * @return the string representation of the BigInteger in decimal format,
     *         or {@code null} if input is null
     */
    @MayReturnNull
    @Override

    public String stringOf(final BigInteger x) {
        return (x == null) ? null : x.toString(10);
    }

    /**
     * Converts a string representation to a BigInteger value.
     * Parses the string as a base 10 integer.
     *
     * @param str the string to parse as a BigInteger in decimal format
     * @return a new BigInteger parsed from the string, or {@code null} if str is {@code null} or empty
     * @throws NumberFormatException if the string cannot be parsed as a valid BigInteger
     */
    @MayReturnNull
    @Override

    public BigInteger valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : new BigInteger(str, 10);
    }

    /**
     * Retrieves a BigInteger value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnIndex the column index (1-based) of the BigInteger value
     * @return the BigInteger value at the specified column, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @MayReturnNull
    @Override

    public BigInteger get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String str = rs.getString(columnIndex);

        return Strings.isEmpty(str) ? null : new BigInteger(str);
    }

    /**
     * Retrieves a BigInteger value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to retrieve the value from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return the BigInteger value in the specified column, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @MayReturnNull
    @Override

    public BigInteger get(final ResultSet rs, final String columnLabel) throws SQLException {
        final String str = rs.getString(columnLabel);

        return Strings.isEmpty(str) ? null : new BigInteger(str);
    }

    /**
     * Sets a BigInteger parameter in a PreparedStatement at the specified position.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the BigInteger value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final BigInteger x) throws SQLException {
        stmt.setString(columnIndex, (x == null) ? null : x.toString());
    }

    /**
     * Sets a named BigInteger parameter in a CallableStatement.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the BigInteger value to set, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final BigInteger x) throws SQLException {
        stmt.setString(parameterName, (x == null) ? null : x.toString());
    }
}
