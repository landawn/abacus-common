/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class SQLArrayType extends AbstractType<Array> {

    static final String SQL_ARRAY = "SQLArray";

    SQLArrayType() {
        super(SQL_ARRAY);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<Array> clazz() {
        return Array.class;
    }

    /**
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     *
     *
     * @param x
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public String stringOf(final Array x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     *
     * @param str
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public Array valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Array get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getArray(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Array get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getArray(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Array x) throws SQLException {
        stmt.setArray(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Array x) throws SQLException {
        // stmt.setArray(parameterName, x);

        stmt.setObject(parameterName, x);
    }
}
