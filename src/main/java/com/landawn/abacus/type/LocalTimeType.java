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

import java.sql.Time;
import java.time.LocalTime;

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class LocalTimeType extends AbstractType<LocalTime> {

    public static final String LOCAL_TIME = LocalTime.class.getSimpleName();

    LocalTimeType() {
        super(LOCAL_TIME);
    }

    @Override
    public Class<LocalTime> clazz() {
        return LocalTime.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(LocalTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public LocalTime valueOf(String str) {
        if (N.isNullOrEmpty(str)) {
            return null;
        }

        if (N.equals(str, SYS_TIME)) {
            return LocalTime.now();
        }

        return LocalTime.parse(str);
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public LocalTime valueOf(char[] cbuf, int offset, int len) {
        if ((cbuf == null) || (len == 0)) {
            return null;
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public LocalTime get(ResultSet rs, int columnIndex) throws SQLException {
        Time ts = rs.getTime(columnIndex);

        return ts == null ? null : ts.toLocalTime();
    }

    /**
     *
     * @param rs
     * @param columnName
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public LocalTime get(ResultSet rs, String columnName) throws SQLException {
        Time ts = rs.getTime(columnName);

        return ts == null ? null : ts.toLocalTime();
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, LocalTime x) throws SQLException {
        stmt.setTime(columnIndex, x == null ? null : Time.valueOf(x));
    }

    /**
     *
     * @param stmt
     * @param columnName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String columnName, LocalTime x) throws SQLException {
        stmt.setTime(columnName, x == null ? null : Time.valueOf(x));
    }
}
