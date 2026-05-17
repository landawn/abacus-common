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
import java.sql.Types;
import java.util.Calendar;

import com.landawn.abacus.util.Dates;

/**
 * Type handler for {@link Calendar} objects that stores and retrieves date/time values
 * as milliseconds since the Unix epoch (January 1, 1970, 00:00:00 UTC) in the database.
 *
 * <p>The database column type used is {@link java.sql.Types#BIGINT BIGINT}.
 * On read, the stored {@code long} value is converted to a {@link Calendar} instance
 * via {@link com.landawn.abacus.util.Dates#createCalendar(long)}.
 * On write, the {@code Calendar}'s time-in-milliseconds is stored as a {@code long}.
 * SQL {@code NULL} is mapped to Java {@code null} in both directions.
 *
 * @see MillisDateType
 * @see MillisTimeType
 * @see MillisTimestampType
 */
public class MillisCalendarType extends CalendarType {

    /**
     * The type name identifier for this Calendar type handler that uses milliseconds.
     */
    public static final String MILLIS_CALENDAR = "MillisCalendar";

    /**
     * Package-private constructor for {@code MillisCalendarType}.
     * Instances are created by the {@code TypeFactory}.
     */
    MillisCalendarType() {
        super(MILLIS_CALENDAR);
    }

    /**
     * Retrieves a {@link Calendar} value from the specified column in the {@link ResultSet}.
     * The column is read as a {@code BIGINT} representing milliseconds since the Unix epoch
     * and converted to a {@code Calendar} instance.
     * SQL {@code NULL} (detected via {@link ResultSet#wasNull()}) is returned as {@code null}.
     *
     * @param rs the {@code ResultSet} containing the query results
     * @param columnIndex the 1-based index of the column to retrieve
     * @return a {@code Calendar} created from the stored millisecond value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public Calendar get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long lng = rs.getLong(columnIndex);

        return (lng == 0 && rs.wasNull()) ? null : Dates.createCalendar(lng);
    }

    /**
     * Retrieves a {@link Calendar} value from the specified column in the {@link ResultSet}.
     * The column is read as a {@code BIGINT} representing milliseconds since the Unix epoch
     * and converted to a {@code Calendar} instance.
     * SQL {@code NULL} (detected via {@link ResultSet#wasNull()}) is returned as {@code null}.
     *
     * @param rs the {@code ResultSet} containing the query results
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code Calendar} created from the stored millisecond value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Calendar get(final ResultSet rs, final String columnName) throws SQLException {
        final long lng = rs.getLong(columnName);

        return (lng == 0 && rs.wasNull()) ? null : Dates.createCalendar(lng);
    }

    /**
     * Sets a {@link Calendar} parameter in a {@link PreparedStatement} at the specified index.
     * The {@code Calendar}'s time-in-milliseconds is stored as a {@code BIGINT} value.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#BIGINT}) is set.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code Calendar} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Calendar x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.getTimeInMillis());
        }
    }

    /**
     * Sets a {@link Calendar} parameter in a {@link CallableStatement} by name.
     * The {@code Calendar}'s time-in-milliseconds is stored as a {@code BIGINT} value.
     * If {@code x} is {@code null}, SQL {@code NULL} ({@link java.sql.Types#BIGINT}) is set.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Calendar} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Calendar x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.getTimeInMillis());
        }
    }
}
