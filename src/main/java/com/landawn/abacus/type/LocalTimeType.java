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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class LocalTimeType extends AbstractTemporalType<LocalTime> {

    public static final String LOCAL_TIME = LocalTime.class.getSimpleName();

    LocalTimeType() {
        super(LOCAL_TIME);
    }

    /**
     *
     *
     * @return
     */
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
    public String stringOf(final LocalTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (Strings.isEmpty(str))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public LocalTime valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
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
     * @return {@code null} if {@code ((cbuf == null) || (len == 0))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public LocalTime valueOf(final char[] cbuf, final int offset, final int len) {
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
    public LocalTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        try {
            return rs.getObject(columnIndex, LocalTime.class);
        } catch (final SQLException e) {
            final Time ts = rs.getTime(columnIndex);

            return ts == null ? null : ts.toLocalTime();
        }
    }

    /**
     *
     * @param rs
     * @param columnName
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public LocalTime get(final ResultSet rs, final String columnName) throws SQLException {
        try {
            return rs.getObject(columnName, LocalTime.class);
        } catch (final SQLException e) {
            final Time ts = rs.getTime(columnName);

            return ts == null ? null : ts.toLocalTime();
        }
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final LocalTime x) throws SQLException {
        try {
            stmt.setObject(columnIndex, x);
        } catch (final SQLException e) {
            stmt.setTime(columnIndex, x == null ? null : Time.valueOf(x));
        }
    }

    /**
     *
     * @param stmt
     * @param columnName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String columnName, final LocalTime x) throws SQLException {
        try {
            stmt.setObject(columnName, x);
        } catch (final SQLException e) {
            stmt.setTime(columnName, x == null ? null : Time.valueOf(x));
        }
    }
}
