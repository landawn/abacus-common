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

import org.joda.time.MutableDateTime;

import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class JodaMutableDateTimeType extends AbstractJodaDateTimeType<MutableDateTime> {

    public static final String JODA_MUTABLE_DATE_TIME = "JodaMutableDateTime";

    JodaMutableDateTimeType() {
        super(JODA_MUTABLE_DATE_TIME);
    }

    JodaMutableDateTimeType(String typeName) {
        super(typeName);
    }

    @Override
    public Class<MutableDateTime> clazz() {
        return MutableDateTime.class;
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public MutableDateTime valueOf(String str) {
        if (N.isNullOrEmpty(str)) {
            return null;
        }

        if (N.equals(str, SYS_TIME)) {
            return new MutableDateTime(System.currentTimeMillis());
        }

        return str.length() == 20 ? iso8601DateTimeFT.parseMutableDateTime(str) : iso8601TimestampFT.parseMutableDateTime(str);
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public MutableDateTime valueOf(char[] cbuf, int offset, int len) {
        if ((cbuf == null) || (len == 0)) {
            return null;
        }

        if (cbuf[offset + 4] != '-') {
            try {
                return new MutableDateTime(parseLong(cbuf, offset, len));
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
    public MutableDateTime get(ResultSet rs, int columnIndex) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : new MutableDateTime(ts.getTime());
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public MutableDateTime get(ResultSet rs, String columnLabel) throws SQLException {
        Timestamp ts = rs.getTimestamp(columnLabel);

        return ts == null ? null : new MutableDateTime(ts.getTime());
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, MutableDateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, MutableDateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }
}
