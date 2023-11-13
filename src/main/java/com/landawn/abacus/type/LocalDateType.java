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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class LocalDateType extends AbstractType<LocalDate> {

    public static final String LOCAL_DATE = LocalDate.class.getSimpleName();

    LocalDateType() {
        super(LOCAL_DATE);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<LocalDate> clazz() {
        return LocalDate.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(LocalDate x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public LocalDate valueOf(String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return LocalDate.now();
        }

        return LocalDate.parse(str);
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public LocalDate valueOf(char[] cbuf, int offset, int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
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
    public LocalDate get(ResultSet rs, int columnIndex) throws SQLException {
        Date ts = rs.getDate(columnIndex);

        return ts == null ? null : ts.toLocalDate();
    }

    /**
     *
     * @param rs
     * @param columnName
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public LocalDate get(ResultSet rs, String columnName) throws SQLException {
        Date ts = rs.getDate(columnName);

        return ts == null ? null : ts.toLocalDate();
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, LocalDate x) throws SQLException {
        stmt.setDate(columnIndex, x == null ? null : Date.valueOf(x));
    }

    /**
     *
     * @param stmt
     * @param columnName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String columnName, LocalDate x) throws SQLException {
        stmt.setDate(columnName, x == null ? null : Date.valueOf(x));
    }
}
