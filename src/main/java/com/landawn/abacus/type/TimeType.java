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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.sql.Time} objects. This class provides serialization,
 * deserialization, and database operations for Time instances. It supports parsing
 * time values from various string formats and millisecond values.
 */
public class TimeType extends AbstractDateType<Time> {

    /**
     * The type name identifier for Time type.
     */
    public static final String TIME = Time.class.getSimpleName();

    TimeType() {
        super(TIME);
    }

    TimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the Class object representing the Time type.
     *
     * @return the Class object for java.sql.Time
     */
    @Override
    public Class<Time> clazz() {
        return Time.class;
    }

    /**
     * Converts an object to a Time.
     * Supports conversion from Number (as milliseconds), java.util.Date, and String types.
     *
     * @param obj the object to convert
     * @return a Time object, or {@code null} if obj is null
     */
    @Override
    public Time valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new Time(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new Time(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Creates a Time from its string representation.
     * Supports parsing various time formats and the special value "sysTime" for current time.
     *
     * @param str the string to parse
     * @return a Time object, or {@code null} if str is empty
     */
    @Override
    public Time valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentTime() : Dates.parseTime(str));
    }

    /**
     * Creates a Time from a character array.
     * First attempts to parse as milliseconds if the format suggests a long value,
     * otherwise delegates to string parsing.
     *
     * @param cbuf the character buffer containing the value
     * @param offset the start offset in the character buffer
     * @param len the number of characters to use
     * @return a Time object, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public Time valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a Time value from the specified column in the ResultSet.
     *
     * @param rs the ResultSet containing the query results
     * @param columnIndex the index of the column to retrieve (1-based)
     * @return a Time object, or {@code null} if the database value is null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Time get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getTime(columnIndex);
    }

    /**
     * Retrieves a Time value from the specified column in the ResultSet.
     *
     * @param rs the ResultSet containing the query results
     * @param columnLabel the label of the column to retrieve
     * @return a Time object, or {@code null} if the database value is null
     * @throws SQLException if a database access error occurs or the columnLabel is not found
     */
    @Override
    public Time get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getTime(columnLabel);
    }

    /**
     * Sets a Time value at the specified parameter index in the PreparedStatement.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the index of the parameter to set (1-based)
     * @param x the Time value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Time x) throws SQLException {
        stmt.setTime(columnIndex, x);
    }

    /**
     * Sets a Time value for the specified parameter name in the CallableStatement.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Time value to set, may be null
     * @throws SQLException if a database access error occurs or the parameterName is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Time x) throws SQLException {
        stmt.setTime(parameterName, x);
    }
}
