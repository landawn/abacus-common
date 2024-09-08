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

import org.joda.time.DateTime;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class JodaDateTimeType extends AbstractJodaDateTimeType<DateTime> {

    public static final String JODA_DATE_TIME = "JodaDateTime";

    JodaDateTimeType() {
        super(JODA_DATE_TIME);
    }

    JodaDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<DateTime> clazz() {
        return DateTime.class;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public DateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new DateTime(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new DateTime(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (Strings.isEmpty(str))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public DateTime valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return new DateTime(System.currentTimeMillis());
        }

        return str.length() == 20 ? jodaISO8601DateTimeFT.parseDateTime(str) : jodaISO8601TimestampFT.parseDateTime(str);
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
    public DateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return new DateTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
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
    public DateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : new DateTime(ts.getTime());
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public DateTime get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);

        return ts == null ? null : new DateTime(ts.getTime());
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final DateTime x) throws SQLException {
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
    public void set(final CallableStatement stmt, final String parameterName, final DateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }
}
