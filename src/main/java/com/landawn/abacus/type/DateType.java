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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Dates;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for java.sql.Date values.
 * This class provides serialization, deserialization, and database operations for SQL Date objects.
 * SQL Dates represent dates without time components (year, month, day only).
 * It supports conversion from various formats including timestamps, date strings, and the special "sysTime" value.
 */
public class DateType extends AbstractDateType<Date> {

    public static final String DATE = Date.class.getSimpleName();

    DateType() {
        super(DATE);
    }

    DateType(final String typeName) {
        super(typeName);
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing java.sql.Date.class
     */
    @Override
    public Class<Date> clazz() {
        return Date.class;
    }

    /**
     * Converts various object types to a SQL Date instance.
     * Supported input types include:
     * - Number: interpreted as milliseconds since epoch
     * - java.util.Date: converted to SQL Date (time portion is truncated)
     * - String: parsed as a date string
     * - Other objects: converted to string first, then parsed
     *
     * @param obj the object to convert to SQL Date. Can be {@code null}.
     * @return A SQL Date instance representing the input value, or {@code null} if input is null
     */
    @Override
    @MayReturnNull
    public Date valueOf(final Object obj) {
        if (obj instanceof Number) {
            return new Date(((Number) obj).longValue());
        } else if (obj instanceof java.util.Date) {
            return new Date(((java.util.Date) obj).getTime());
        }

        return obj == null ? null : valueOf(N.stringOf(obj));
    }

    /**
     * Converts a string representation to a SQL Date instance.
     * Special handling for:
     * - {@code null} or empty string: returns null
     * - "sysTime": returns the current system date
     * - Other strings: parsed using date parsing utilities
     *
     * @param str the string to parse. Can be {@code null} or empty.
     * @return A SQL Date instance parsed from the string, or {@code null} if input is null/empty
     */
    @Override
    @MayReturnNull
    public Date valueOf(final String str) {
        return Strings.isEmpty(str) ? null : (N.equals(str, SYS_TIME) ? Dates.currentDate() : Dates.parseDate(str));
    }

    /**
     * Converts a character array to a SQL Date instance.
     * If the character array appears to represent a numeric value (timestamp),
     * it attempts to parse it as milliseconds since epoch.
     * Otherwise, it converts to string and uses string parsing.
     *
     * @param cbuf the character array containing the value to parse
     * @param offset the starting position in the character array
     * @param len the number of characters to use
     * @return A SQL Date instance parsed from the character array, or {@code null} if input is {@code null} or empty
     */
    @MayReturnNull
    @Override

    public Date valueOf(final char[] cbuf, final int offset, final int len) {
        if ((cbuf == null) || (len == 0)) {
            return null; // NOSONAR
        }

        if (isPossibleLong(cbuf, offset, len)) {
            try {
                return Dates.createDate(parseLong(cbuf, offset, len));
            } catch (final NumberFormatException e) {
                // ignore;
            }
        }

        return valueOf(String.valueOf(cbuf, offset, len));
    }

    /**
     * Retrieves a SQL Date value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the date value
     * @return A SQL Date instance from the result set, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Date get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getDate(columnIndex);
    }

    /**
     * Retrieves a SQL Date value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the date value
     * @return A SQL Date instance from the result set, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Date get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getDate(columnLabel);
    }

    /**
     * Sets a SQL Date value as a parameter in a PreparedStatement.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the SQL Date value to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Date x) throws SQLException {
        stmt.setDate(columnIndex, x);
    }

    /**
     * Sets a SQL Date value as a named parameter in a CallableStatement.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the SQL Date value to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Date x) throws SQLException {
        stmt.setDate(parameterName, x);
    }
}
