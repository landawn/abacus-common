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
 * Type handler for {@link java.math.BigInteger} values.
 * Provides serialization, deserialization, and JDBC operations for {@code BigInteger} instances.
 *
 * <p>String representation: the base-10 decimal string of the value, produced by
 * {@link java.math.BigInteger#toString(int) BigInteger.toString(10)}.</p>
 * <p>JDBC mapping: {@code BigInteger} values have no native SQL type with guaranteed precision,
 * so they are stored and retrieved as {@code VARCHAR} strings via
 * {@link java.sql.PreparedStatement#setString(int, String)} /
 * {@link java.sql.ResultSet#getString(int)}, preserving full precision.</p>
 *
 * @see java.math.BigInteger
 */
public final class BigIntegerType extends NumberType<BigInteger> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "BigInteger"}).
     */
    public static final String BIG_INTEGER = BigInteger.class.getSimpleName();

    /**
     * Package-private constructor for {@code BigIntegerType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BigIntegerType() {
        super(BIG_INTEGER);
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return {@code BigInteger.class}
     */
    @Override
    public Class<BigInteger> javaType() {
        return BigInteger.class;
    }

    /**
     * Converts a {@link java.math.BigInteger} to its base-10 decimal string representation.
     * Delegates to {@link java.math.BigInteger#toString(int) BigInteger.toString(10)}.
     *
     * @param x the {@code BigInteger} to convert; may be {@code null}
     * @return the decimal string representation of the value,
     *         or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final BigInteger x) {
        return (x == null) ? null : x.toString(10);
    }

    /**
     * Parses a base-10 decimal string and returns a new {@link java.math.BigInteger}.
     * Leading and trailing whitespace is trimmed before parsing.
     *
     * @param str the decimal string to parse; may be {@code null} or empty
     * @return a new {@code BigInteger} parsed from the string,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @throws NumberFormatException if {@code str} cannot be parsed as a valid {@code BigInteger}
     */
    @Override
    public BigInteger valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new BigInteger(str.trim(), 10);
    }

    /**
     * Retrieves a {@link java.math.BigInteger} from a {@link java.sql.ResultSet} at the specified column index.
     * The column value is read as a {@code VARCHAR} string and parsed as a decimal integer.
     * Returns {@code null} if the column value is SQL NULL or an empty string.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column containing the integer value
     * @return the {@code BigInteger} value at the specified column, or {@code null} if the column value is SQL NULL or empty
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     * @throws NumberFormatException if the column string value cannot be parsed as a valid {@code BigInteger}
     */
    @Override
    public BigInteger get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String stringValue = rs.getString(columnIndex);

        return Strings.isEmpty(stringValue) ? null : new BigInteger(stringValue.trim());
    }

    /**
     * Retrieves a {@link java.math.BigInteger} from a {@link java.sql.ResultSet} using the specified column label.
     * The column value is read as a {@code VARCHAR} string and parsed as a decimal integer.
     * Returns {@code null} if the column value is SQL NULL or an empty string.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the {@code BigInteger} value at the specified column, or {@code null} if the column value is SQL NULL or empty
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     * @throws NumberFormatException if the column string value cannot be parsed as a valid {@code BigInteger}
     */
    @Override
    public BigInteger get(final ResultSet rs, final String columnName) throws SQLException {
        final String stringValue = rs.getString(columnName);

        return Strings.isEmpty(stringValue) ? null : new BigInteger(stringValue.trim());
    }

    /**
     * Sets a {@link java.math.BigInteger} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * The value is stored as its decimal string representation via
     * {@link java.sql.PreparedStatement#setString(int, String)}, preserving full precision.
     * A {@code null} value sets the parameter to SQL NULL.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code BigInteger} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final BigInteger x) throws SQLException {
        stmt.setString(columnIndex, (x == null) ? null : x.toString());
    }

    /**
     * Sets a named {@link java.math.BigInteger} parameter on a {@link java.sql.CallableStatement}.
     * The value is stored as its decimal string representation via
     * {@link java.sql.CallableStatement#setString(String, String)}, preserving full precision.
     * A {@code null} value sets the parameter to SQL NULL.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code BigInteger} value to set; {@code null} is stored as SQL NULL
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final BigInteger x) throws SQLException {
        stmt.setString(parameterName, (x == null) ? null : x.toString());
    }
}
