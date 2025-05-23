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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.util.Dates;

public class MillisDateType extends DateType {

    public static final String MILLIS_DATE = "MillisDate";

    MillisDateType() {
        super(MILLIS_DATE);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Date get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long lng = rs.getLong(columnIndex);

        return (lng == 0) ? null : Dates.createDate(lng);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Date get(final ResultSet rs, final String columnLabel) throws SQLException {
        final long lng = rs.getLong(columnLabel);

        return (lng == 0) ? null : Dates.createDate(lng);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Date x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.getTime());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Date x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.getTime());
    }
}
