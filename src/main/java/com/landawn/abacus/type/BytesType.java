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

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class BytesType extends AbstractType<byte[]> {

    public static final String BYTES = "Bytes";

    BytesType() {
        super(BYTES);
    }

    @Override
    public Class<byte[]> clazz() {
        return byte[].class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(byte[] x) {
        return (x == null) ? null : N.base64Encode(x);
    }

    /**
     *
     * @param st
     * @return
     */
    @Override
    public byte[] valueOf(String st) {
        return (st == null) ? null : N.base64Decode(st);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public byte[] get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public byte[] get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getBytes(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, byte[] x) throws SQLException {
        stmt.setBytes(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, byte[] x) throws SQLException {
        stmt.setBytes(parameterName, x);
    }
}
