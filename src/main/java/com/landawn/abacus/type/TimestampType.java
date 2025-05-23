/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class TimestampType extends AbstractDateType<Timestamp> {

    public static final String TIMESTAMP = Timestamp.class.getSimpleName();

    TimestampType() {
        super(TIMESTAMP);
    }

    TimestampType(final String typeName) {
        super(typeName);
    }

    @Override
    public Class<Timestamp> clazz() {
        return Timestamp.class;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public Timestamp valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new Timestamp(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new Timestamp(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Timestamp valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentTimestamp() : Dates.parseTimestamp(str));
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
    public Timestamp valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createTimestamp(parseLong(cbuf, offset, len));
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
    public Timestamp get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getTimestamp(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Timestamp get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getTimestamp(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Timestamp x) throws SQLException {
        stmt.setTimestamp(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Timestamp x) throws SQLException {
        stmt.setTimestamp(parameterName, x);
    }
}
