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
import java.time.Instant;
import java.time.LocalDateTime;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

public class LocalDateTimeType extends AbstractTemporalType<LocalDateTime> {

    public static final String LOCAL_DATE_TIME = LocalDateTime.class.getSimpleName();

    LocalDateTimeType() {
        super(LOCAL_DATE_TIME);
    }

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
    public String stringOf(final LocalDateTime x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public LocalDateTime valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return LocalDateTime.now();
        }

        if (isPossibleLong(str)) {
            try {
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(Numbers.toLong(str)), DEFAULT_ZONE_ID);
            } catch (final NumberFormatException e2) {
                // ignore;
            }
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
    @MayReturnNull
    @Override
    public LocalDateTime valueOf(final char[] cbuf, final int offset, final int len) {
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
    public LocalDateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        try {
            return rs.getObject(columnIndex, LocalDateTime.class);
        } catch (final SQLException e) {
            final Timestamp ts = rs.getTimestamp(columnIndex);

            return ts == null ? null : ts.toLocalDateTime();
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
    public LocalDateTime get(final ResultSet rs, final String columnName) throws SQLException {
        try {
            return rs.getObject(columnName, LocalDateTime.class);
        } catch (final SQLException e) {
            final Timestamp ts = rs.getTimestamp(columnName);

            return ts == null ? null : ts.toLocalDateTime();
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
    public void set(final PreparedStatement stmt, final int columnIndex, final LocalDateTime x) throws SQLException {
        try {
            stmt.setObject(columnIndex, x);
        } catch (final SQLException e) {
            stmt.setTimestamp(columnIndex, x == null ? null : Timestamp.valueOf(x));
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
    public void set(final CallableStatement stmt, final String columnName, final LocalDateTime x) throws SQLException {
        try {
            stmt.setObject(columnName, x);
        } catch (final SQLException e) {
            stmt.setTimestamp(columnName, x == null ? null : Timestamp.valueOf(x));
        }
    }
}
