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

import org.joda.time.DateTime;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class JodaDateTimeType extends AbstractJodaDateTimeType<DateTime> {

    public static final String JODA_DATE_TIME = "JodaDateTime";

    JodaDateTimeType() {
        super(JODA_DATE_TIME);
    }

    JodaDateTimeType(final String typeName) {
        super(typeName);
    }

    /**
     * Gets the class type for Joda DateTime.
     *
     * @return the Class object representing org.joda.time.DateTime
     */
    @Override
    public Class<DateTime> clazz() {
        return DateTime.class;
    }

    /**
     * Converts the specified object to a Joda DateTime instance.
     * 
     * This method handles the following conversions:
     * - Number: treated as milliseconds since epoch and converted to DateTime
     * - java.util.Date: converted using the date's time in milliseconds
     * - String: parsed using the valueOf(String) method
     * - null: returns null
     * - Other types: converted to string first, then parsed
     *
     * @param obj the object to convert to DateTime
     * @return a DateTime instance, or null if the input is null
     */
    @Override
    public DateTime valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new DateTime(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new DateTime(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Parses a string representation into a Joda DateTime instance.
     * 
     * This method handles the following string formats:
     * - Empty/null string: returns null
     * - "SYS_TIME": returns current system time as DateTime
     * - ISO 8601 date-time format (20 characters): parsed as yyyy-MM-dd'T'HH:mm:ss
     * - ISO 8601 timestamp format (24 characters): parsed as yyyy-MM-dd'T'HH:mm:ss.SSS
     * 
     * @param str the string to parse
     * @return a DateTime instance, or null if the string is empty or null
     * @throws IllegalArgumentException if the string format is invalid
     */
    @MayReturnNull
    @Override
    public DateTime valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        if (N.equals(str, SYS_TIME)) {
            return new DateTime(System.currentTimeMillis());
        }

        return str.length() == 20 ? jodaISO8601DateTimeFT.parseDateTime(str) : jodaISO8601TimestampFT.parseDateTime(str);
    }

    /**
     * Parses a character array into a Joda DateTime instance.
     * 
     * This method first attempts to parse the character array as a long value (milliseconds since epoch).
     * If that fails, it converts the character array to a string and delegates to valueOf(String).
     * 
     * @param cbuf the character buffer containing the value to parse
     * @param offset the start offset in the character buffer
     * @param len the number of characters to parse
     * @return a DateTime instance, or null if the character buffer is null or length is 0
     */
    @MayReturnNull
    @Override
    public DateTime valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return new DateTime(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a DateTime value from the specified column in a ResultSet.
     * 
     * This method reads a Timestamp from the ResultSet and converts it to a Joda DateTime.
     * If the timestamp is null, this method returns null.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the value from
     * @return a DateTime instance created from the timestamp, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public DateTime get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnIndex);

        return ts == null ? null : new DateTime(ts.getTime());
    }

    /**
     * Retrieves a DateTime value from the specified column in a ResultSet.
     * 
     * This method reads a Timestamp from the ResultSet and converts it to a Joda DateTime.
     * If the timestamp is null, this method returns null.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label to retrieve the value from
     * @return a DateTime instance created from the timestamp, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public DateTime get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Timestamp ts = rs.getTimestamp(columnLabel);

        return ts == null ? null : new DateTime(ts.getTime());
    }

    /**
     * Sets a DateTime parameter in a PreparedStatement.
     * 
     * This method converts the Joda DateTime to a SQL Timestamp before setting it in the statement.
     * If the DateTime is null, a SQL NULL is set for the parameter.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the DateTime value to set, or null for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final DateTime x) throws SQLException {
        stmt.setTimestamp(columnIndex, x == null ? null : new Timestamp(x.getMillis()));
    }

    /**
     * Sets a named DateTime parameter in a CallableStatement.
     * 
     * This method converts the Joda DateTime to a SQL Timestamp before setting it in the statement.
     * If the DateTime is null, a SQL NULL is set for the parameter.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the DateTime value to set, or null for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final DateTime x) throws SQLException {
        stmt.setTimestamp(parameterName, x == null ? null : new Timestamp(x.getMillis()));
    }
}