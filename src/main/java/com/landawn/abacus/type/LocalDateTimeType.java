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
import java.sql.Timestamp;
import java.time.LocalDateTime;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class LocalDateTimeType extends AbstractType<LocalDateTime> {

    public static final String LOCAL_DATE_TIME = LocalDateTime.class.getSimpleName();

    LocalDateTimeType() {
        super(LOCAL_DATE_TIME);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<LocalDateTime> clazz() {
        return LocalDateTime.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(LocalDateTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public LocalDateTime valueOf(String str) {
        if (Strings.isEmpty(str)) {
            return null;
        }

        if (N.equals(str, SYS_TIME)) {
            return LocalDateTime.now();
        }

        return LocalDateTime.parse(str);
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public LocalDateTime valueOf(char[] cbuf, int offset, int len) {
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
    public LocalDateTime get(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : ts.toLocalDateTime();
    }

    /**
     *
     * @param rs
     * @param columnName
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public LocalDateTime get(ResultSet rs, String columnName) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnName);

        return ts == null ? null : ts.toLocalDateTime();
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, LocalDateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.valueOf(x));
    }

    /**
     *
     * @param stmt
     * @param columnName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String columnName, LocalDateTime x) throws SQLException {
        stmt.setTimestamp(columnName, x == null ? null : Timestamp.valueOf(x));
    }
}
