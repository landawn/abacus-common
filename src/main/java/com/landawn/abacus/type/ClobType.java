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

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class ClobType extends AbstractType<Clob> {

    public static final String CLOB = Clob.class.getSimpleName();

    ClobType() {
        super(CLOB);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<Clob> clazz() {
        return Clob.class;
    }

    /**
     * 
     *
     * @param x 
     * @return 
     * @throws UnsupportedOperationException 
     */
    @Override
    public String stringOf(Clob x) throws UnsupportedOperationException {
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
    public Clob valueOf(String str) throws UnsupportedOperationException {
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
    public Clob get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getClob(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Clob get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getClob(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Clob x) throws SQLException {
        stmt.setClob(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Clob x) throws SQLException {
        stmt.setClob(parameterName, x);
    }
}
