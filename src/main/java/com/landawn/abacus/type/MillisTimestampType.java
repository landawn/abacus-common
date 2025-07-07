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

import com.landawn.abacus.util.Dates;

public class MillisTimestampType extends TimestampType {

    public static final String MILLIS_TIMESTAMP = "MillisTimestamp";

    MillisTimestampType() {
        super(MILLIS_TIMESTAMP);
    }

    /**
     * Retrieves a Timestamp value from a ResultSet at the specified column index.
     * The value is read as a long representing milliseconds since epoch.
     * A value of 0 is treated as NULL and returns null.
     *
     * @param rs The ResultSet containing the data
     * @param columnIndex The column index (1-based) to retrieve the value from
     * @return A Timestamp object created from the milliseconds value, or null if the value is 0
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Timestamp get(final ResultSet rs, final int columnIndex) throws SQLException {
        final long lng = rs.getLong(columnIndex);

        return (lng == 0) ? null : Dates.createTimestamp(lng);
    }

    /**
     * Retrieves a Timestamp value from a ResultSet using the specified column label.
     * The value is read as a long representing milliseconds since epoch.
     * A value of 0 is treated as NULL and returns null.
     *
     * @param rs The ResultSet containing the data
     * @param columnLabel The label of the column to retrieve the value from
     * @return A Timestamp object created from the milliseconds value, or null if the value is 0
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Timestamp get(final ResultSet rs, final String columnLabel) throws SQLException {
        final long lng = rs.getLong(columnLabel);

        return (lng == 0) ? null : Dates.createTimestamp(lng);
    }

    /**
     * Sets a Timestamp parameter in a PreparedStatement at the specified position.
     * The Timestamp is stored as a long value representing milliseconds since epoch.
     * If the Timestamp is null, 0 is stored.
     *
     * @param stmt The PreparedStatement to set the parameter on
     * @param columnIndex The parameter index (1-based) to set
     * @param x The Timestamp value to set, or null to store 0
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Timestamp x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.getTime());
    }

    /**
     * Sets a Timestamp parameter in a CallableStatement using the specified parameter name.
     * The Timestamp is stored as a long value representing milliseconds since epoch.
     * If the Timestamp is null, 0 is stored.
     *
     * @param stmt The CallableStatement to set the parameter on
     * @param parameterName The name of the parameter to set
     * @param x The Timestamp value to set, or null to store 0
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Timestamp x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.getTime());
    }
}