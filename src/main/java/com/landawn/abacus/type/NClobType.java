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
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class NClobType extends AbstractType<NClob> {

    public static final String NCLOB = NClob.class.getSimpleName();

    NClobType() {
        super(NCLOB);
    }

    @Override
    public Class<NClob> clazz() {
        return NClob.class;
    }

    /**
     *
     * @param x
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public String stringOf(final NClob x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param str
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public NClob valueOf(final String str) throws UnsupportedOperationException {
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
    public NClob get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getNClob(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public NClob get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getNClob(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final NClob x) throws SQLException {
        stmt.setNClob(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final NClob x) throws SQLException {
        stmt.setNClob(parameterName, x);
    }
}
