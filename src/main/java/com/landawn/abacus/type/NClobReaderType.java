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

import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class NClobReaderType extends ReaderType {

    public static final String NCLOB_READER = "NClobReader";

    NClobReaderType() {
        super(NCLOB_READER);
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
        final NClob clob = rs.getNClob(columnIndex);
        return clob2Reader(clob);
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
        return clob2Reader(rs.getNClob(columnLabel));
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
    public void set(CallableStatement stmt, String parameterName, Reader x) throws SQLException {
        stmt.setNClob(parameterName, x);
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
        stmt.setNClob(columnIndex, x, sqlTypeOrLength);
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
        stmt.setNClob(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Clob 2 reader.
     *
     * @param clob
     * @return
     * @throws SQLException the SQL exception
     */
    static Reader clob2Reader(NClob clob) throws SQLException {
        Reader reader = null;

        if (clob != null) {
            reader = clob.getCharacterStream();
            clob.free();
        }

        return reader;
    }
}
