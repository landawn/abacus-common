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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.Reader;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class NCharacterStreamType extends ReaderType {

    public static final String N_CHARACTER_STREAM = "NCharacterStream";

    NCharacterStreamType() {
        super(N_CHARACTER_STREAM);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getNCharacterStream(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getNCharacterStream(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Reader x) throws SQLException {
        stmt.setNCharacterStream(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Reader x) throws SQLException {
        stmt.setNCharacterStream(parameterName, x);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Reader x, int sqlTypeOrLength) throws SQLException {
        stmt.setNCharacterStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Reader x, int sqlTypeOrLength) throws SQLException {
        stmt.setNCharacterStream(parameterName, x, sqlTypeOrLength);
    }
}
