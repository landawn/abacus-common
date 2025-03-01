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

public final class BigIntegerType extends NumberType<BigInteger> {

    public static final String BIG_INTEGER = BigInteger.class.getSimpleName();

    BigIntegerType() {
        super(BIG_INTEGER);
    }

    @Override
    public Class<BigInteger> clazz() {
        return BigInteger.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final BigInteger x) {
        return (x == null) ? null : x.toString(10);
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public BigInteger valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : new BigInteger(str, 10);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public BigInteger get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String str = rs.getString(columnIndex);

        return Strings.isEmpty(str) ? null : new BigInteger(str);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public BigInteger get(final ResultSet rs, final String columnLabel) throws SQLException {
        final String str = rs.getString(columnLabel);

        return Strings.isEmpty(str) ? null : new BigInteger(str);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final BigInteger x) throws SQLException {
        stmt.setString(columnIndex, (x == null) ? null : x.toString());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final BigInteger x) throws SQLException {
        stmt.setString(parameterName, (x == null) ? null : x.toString());
    }
}
