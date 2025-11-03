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
import java.sql.Time;

import com.landawn.abacus.util.Dates;

public class MillisTimeType extends TimeType {

    public static final String MILLIS_TIME = "MillisTime";

    MillisTimeType() {
        super(MILLIS_TIME);
    }

    /**
     * Retrieves a Time value from a ResultSet at the specified column index.
     * The value is read as a long representing milliseconds since epoch.
     * A value of 0 is treated as NULL and returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column contains milliseconds value 3600000 (01:00:00)
     * Time time = type.get(rs, 1);
     * // Returns: Time object for 01:00:00
     *
     * // Column contains 0 (representing NULL)
     * time = type.get(rs, 2);
     * // Returns: null
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A Time object created from the milliseconds value, or {@code null} if the value is 0
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Time get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long lng = rs.getLong(columnIndex);

        return (lng == 0) ? null : Dates.createTime(lng);
    }

    /**
     * Retrieves a Time value from a ResultSet using the specified column label.
     * The value is read as a long representing milliseconds since epoch.
     * A value of 0 is treated as NULL and returns {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column "start_time" contains milliseconds value 3600000 (01:00:00)
     * Time time = type.get(rs, "start_time");
     * // Returns: Time object for 01:00:00
     *
     * // Column "end_time" contains 0 (representing NULL)
     * time = type.get(rs, "end_time");
     * // Returns: null
     * }</pre>
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A Time object created from the milliseconds value, or {@code null} if the value is 0
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Time get(final ResultSet rs, final String columnLabel) throws SQLException {
        final long lng = rs.getLong(columnLabel);

        return (lng == 0) ? null : Dates.createTime(lng);
    }

    /**
     * Sets a Time parameter in a PreparedStatement at the specified position.
     * The Time is stored as a long value representing milliseconds since epoch.
     * If the Time is {@code null}, 0 is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * PreparedStatement stmt = connection.prepareStatement(
     *     "INSERT INTO schedule (id, start_time) VALUES (?, ?)");
     *
     * Time time = new Time(3600000L); // 01:00:00
     * type.set(stmt, 2, time);
     * // Sets parameter to 3600000
     *
     * type.set(stmt, 2, null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The Time value to set, or {@code null} to store 0
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Time x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.getTime());
    }

    /**
     * Sets a Time parameter in a CallableStatement using the specified parameter name.
     * The Time is stored as a long value representing milliseconds since epoch.
     * If the Time is {@code null}, 0 is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Time> type = TypeFactory.getType(Time.class);
     * CallableStatement stmt = connection.prepareCall("{call set_schedule(?, ?)}");
     *
     * Time time = new Time(3600000L); // 01:00:00
     * type.set(stmt, "p_start_time", time);
     * // Sets parameter to 3600000
     *
     * type.set(stmt, "p_end_time", null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The Time value to set, or {@code null} to store 0
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Time x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.getTime());
    }
}
