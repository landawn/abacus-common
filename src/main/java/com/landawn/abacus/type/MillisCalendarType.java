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
import java.util.Calendar;

import com.landawn.abacus.util.Dates;

/**
 * Type handler for {@link Calendar} objects that stores and retrieves them as milliseconds
 * in the database. This implementation converts between Calendar instances and their
 * millisecond representation (time since epoch).
 */
public class MillisCalendarType extends CalendarType {

    /**
     * The type name identifier for this Calendar type handler that uses milliseconds.
     */
    public static final String MILLIS_CALENDAR = "MillisCalendar";

    MillisCalendarType() {
        super(MILLIS_CALENDAR);
    }

    /**
     * Retrieves a Calendar value from the specified column in the ResultSet.
     * The method reads a long value representing milliseconds from the database
     * and converts it to a Calendar instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Calendar> type = TypeFactory.getType(Calendar.class);
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column contains milliseconds value 1609459200000 (Jan 1, 2021)
     * Calendar cal = type.get(rs, 1);
     * // Returns: Calendar object for Jan 1, 2021
     *
     * // Column contains 0 (representing NULL)
     * cal = type.get(rs, 2);
     * // Returns: null
     * }</pre>
     *
     * @param rs the ResultSet containing the query results
     * @param columnIndex the index of the column to retrieve (1-based)
     * @return a Calendar object created from the milliseconds value, or {@code null} if the database value was 0
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Calendar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long lng = rs.getLong(columnIndex);

        return (lng == 0) ? null : Dates.createCalendar(lng);
    }

    /**
     * Retrieves a Calendar value from the specified column in the ResultSet.
     * The method reads a long value representing milliseconds from the database
     * and converts it to a Calendar instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Calendar> type = TypeFactory.getType(Calendar.class);
     * ResultSet rs = ...; // obtained from database query
     *
     * // Column "created_date" contains milliseconds value 1609459200000
     * Calendar cal = type.get(rs, "created_date");
     * // Returns: Calendar object for Jan 1, 2021
     *
     * // Column "deleted_date" contains 0 (representing NULL)
     * cal = type.get(rs, "deleted_date");
     * // Returns: null
     * }</pre>
     *
     * @param rs the ResultSet containing the query results
     * @param columnLabel the label of the column to retrieve
     * @return a Calendar object created from the milliseconds value, or {@code null} if the database value was 0
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public Calendar get(final ResultSet rs, final String columnLabel) throws SQLException {
        final long lng = rs.getLong(columnLabel);

        return (lng == 0) ? null : Dates.createCalendar(lng);
    }

    /**
     * Sets a Calendar value at the specified parameter index in the PreparedStatement.
     * The method converts the Calendar to its millisecond representation and stores it
     * as a long value in the database. If the Calendar is {@code null}, 0 is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Calendar> type = TypeFactory.getType(Calendar.class);
     * PreparedStatement stmt = connection.prepareStatement(
     *     "INSERT INTO events (id, event_date) VALUES (?, ?)");
     *
     * Calendar cal = Calendar.getInstance();
     * cal.setTimeInMillis(1609459200000L); // Jan 1, 2021
     * type.set(stmt, 2, cal);
     * // Sets parameter to 1609459200000
     *
     * type.set(stmt, 2, null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Calendar value to set, or {@code null} to store 0
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Calendar x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.getTimeInMillis());
    }

    /**
     * Sets a Calendar value for the specified parameter name in the CallableStatement.
     * The method converts the Calendar to its millisecond representation and stores it
     * as a long value in the database. If the Calendar is {@code null}, 0 is stored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Calendar> type = TypeFactory.getType(Calendar.class);
     * CallableStatement stmt = connection.prepareCall("{call set_event(?, ?)}");
     *
     * Calendar cal = Calendar.getInstance();
     * cal.setTimeInMillis(1609459200000L); // Jan 1, 2021
     * type.set(stmt, "p_event_date", cal);
     * // Sets parameter to 1609459200000
     *
     * type.set(stmt, "p_cancelled_date", null);
     * // Sets parameter to 0
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Calendar value to set, or {@code null} to store 0
     * @throws SQLException if a database access error occurs or the parameterName is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Calendar x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.getTimeInMillis());
    }
}
