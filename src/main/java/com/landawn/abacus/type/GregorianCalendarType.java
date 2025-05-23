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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

@SuppressWarnings("java:S2160")
public class GregorianCalendarType extends AbstractCalendarType<GregorianCalendar> {

    public static final String GREGORIAN_CALENDAR = GregorianCalendar.class.getSimpleName();

    GregorianCalendarType() {
        super(GREGORIAN_CALENDAR);
    }

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
            return Dates.createGregorianCalendar(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return Dates.createGregorianCalendar((java.util.Date) obj);
        } else if (obj instanceof Calendar) {
            return Dates.createGregorianCalendar((Calendar) obj);
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public GregorianCalendar valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentGregorianCalendar() : Dates.parseGregorianCalendar(str));
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
    public GregorianCalendar valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createGregorianCalendar(parseLong(cbuf, offset, len));
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
    public GregorianCalendar get(final ResultSet rs, final int columnIndex) throws SQLException {
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
    public GregorianCalendar get(final ResultSet rs, final String columnLabel) throws SQLException {
        return asGregorianCalendar(rs.getTimestamp(columnLabel));
    }

    /**
     * As gregorian calendar.
     *
     * @param value
     * @return
     */
    private static GregorianCalendar asGregorianCalendar(final Timestamp value) {
        if (value == null) {
            return null; // NOSONAR
        }

        final GregorianCalendar gc = new GregorianCalendar();
        gc.setTimeInMillis(value.getTime());

        return gc;
    }
}
