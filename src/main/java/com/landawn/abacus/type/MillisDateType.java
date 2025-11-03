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

class MillisDateType extends DateType {

    public static final String MILLIS_DATE = "MillisDate";

    MillisDateType() {
        super(MILLIS_DATE);
    }

    /**
     * Retrieves a Date value from a ResultSet at the specified column index.
     * The value is read as a long representing milliseconds since epoch.
     * A value of 0 is treated as NULL and returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.sql.Date> type = TypeFactory.getType(java.sql.Date.class);
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column contains milliseconds value 1609459200000 (Jan 1, 2021)
     * Date date = type.get(rs, 1);
     * // Returns: Date object for Jan 1, 2021
     *
     * // Column contains 0 (representing NULL)
     * date = type.get(rs, 2);
     * // Returns: null
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A Date object created from the milliseconds value, or {@code null} if the value is 0
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Date get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long lng = rs.getLong(columnIndex);

        return (lng == 0) ? null : Dates.createDate(lng);
    }

    /**
     * Retrieves a Date value from a ResultSet using the specified column label.
     * The value is read as a long representing milliseconds since epoch.
     * A value of 0 is treated as NULL and returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.sql.Date> type = TypeFactory.getType(java.sql.Date.class);
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column "created_date" contains milliseconds value 1609459200000
     * Date date = type.get(rs, "created_date");
     * // Returns: Date object for Jan 1, 2021
     *
     * // Column "deleted_date" contains 0 (representing NULL)
     * date = type.get(rs, "deleted_date");
     * // Returns: null
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A Date object created from the milliseconds value, or {@code null} if the value is 0
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Date get(final ResultSet rs, final String columnLabel) throws SQLException {
        final long lng = rs.getLong(columnLabel);

        return (lng == 0) ? null : Dates.createDate(lng);
    }

    /**
     * Sets a Date parameter in a PreparedStatement at the specified position.
     * The Date is stored as a long value representing milliseconds since epoch.
     * If the Date is {@code null}, 0 is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.sql.Date> type = TypeFactory.getType(java.sql.Date.class);
     * PreparedStatement stmt = connection.prepareStatement(
     *     "INSERT INTO users (id, created_date) VALUES (?, ?)");
     *
     * Date date = new Date(1609459200000L); // Jan 1, 2021
     * type.set(stmt, 2, date);
     * // Sets parameter to 1609459200000
     *
     * type.set(stmt, 2, null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The Date value to set, or {@code null} to store 0
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Date x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.getTime());
    }

    /**
     * Sets a Date parameter in a CallableStatement using the specified parameter name.
     * The Date is stored as a long value representing milliseconds since epoch.
     * If the Date is {@code null}, 0 is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<java.sql.Date> type = TypeFactory.getType(java.sql.Date.class);
     * CallableStatement stmt = connection.prepareCall("{call update_date(?, ?)}");
     *
     * Date date = new Date(1609459200000L); // Jan 1, 2021
     * type.set(stmt, "p_created_date", date);
     * // Sets parameter to 1609459200000
     *
     * type.set(stmt, "p_deleted_date", null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The Date value to set, or {@code null} to store 0
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Date x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.getTime());
    }
}
