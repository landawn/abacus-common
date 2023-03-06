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
import java.util.Date;

import com.landawn.abacus.util.DateUtil;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class JUDateType extends AbstractDateType<Date> {

    public static final String JU_DATE = "JUDate";

    private static final String declaringName = Date.class.getCanonicalName();

    JUDateType() {
        super(JU_DATE);
    }

    JUDateType(String typeName) {
        super(typeName);
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @Override
    public Class<Date> clazz() {
        return Date.class;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public Date valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new Date(((Number) obj).longValue());
        } else if (obj instanceof Date) {
            return new Date(((Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Date valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : (N.equals(str, SYS_TIME) ? DateUtil.currentJUDate() : DateUtil.parseJUDate(str));
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public Date valueOf(char[] cbuf, int offset, int len) {
        if ((cbuf == null) || (len == 0)) {
            return null;
        }

        if (cbuf[offset + 4] != '-') {
            try {
                return DateUtil.createJUDate(parseLong(cbuf, offset, len));
            } catch (NumberFormatException e) {
                // ignore;
            }
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
    public Date get(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp value = rs.getTimestamp(columnIndex);

        return (value == null) ? null : new Date(value.getTime());
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Date get(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp value = rs.getTimestamp(columnLabel);

        return (value == null) ? null : new Date(value.getTime());
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Date x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, Date x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : x instanceof java.sql.Timestamp ? (java.sql.Timestamp) x : new java.sql.Timestamp(x.getTime()));
    }
}
