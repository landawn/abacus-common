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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.DateUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class GregorianCalendarType extends AbstractCalendarType<GregorianCalendar> {

    public static final String GREGORIAN_CALENDAR = GregorianCalendar.class.getSimpleName();

    GregorianCalendarType() {
        super(GREGORIAN_CALENDAR);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<GregorianCalendar> clazz() {
        return GregorianCalendar.class;
    }

    /**
     *
     * @param obj
     * @return
     */
    @Override
    public GregorianCalendar valueOf(final Object obj) {
        if (obj instanceof Number) {
            return DateUtil.createGregorianCalendar(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return DateUtil.createGregorianCalendar((java.util.Date) obj);
        } else if (obj instanceof Calendar) {
            return DateUtil.createGregorianCalendar((Calendar) obj);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public GregorianCalendar valueOf(String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? DateUtil.currentGregorianCalendar() : DateUtil.parseGregorianCalendar(str));
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
    public GregorianCalendar valueOf(char[] cbuf, int offset, int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return DateUtil.createGregorianCalendar(parseLong(cbuf, offset, len));
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
    public GregorianCalendar get(ResultSet rs, int columnIndex) throws SQLException {
        return asGregorianCalendar(rs.getTimestamp(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public GregorianCalendar get(ResultSet rs, String columnLabel) throws SQLException {
        return asGregorianCalendar(rs.getTimestamp(columnLabel));
    }

    /**
     * As gregorian calendar.
     *
     * @param value
     * @return
     */
    private static GregorianCalendar asGregorianCalendar(Timestamp value) {
        if (value == null) {
            return null; // NOSONAR
        }

        GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(value.getTime());

        return gc;
    }
}
