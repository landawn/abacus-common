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

/**
 * Type handler for National String (NString) values, providing database interaction
 * capabilities for handling Unicode string data using national character sets.
 * This type is specifically designed for databases that distinguish between
 * regular strings and national character strings.
 */
public class NStringType extends AbstractStringType {

    public static final String NSTRING = "NString";

    /**
     * Package-private constructor for NStringType.
     * This constructor is called by the TypeFactory to create NString type instances.
     */
    NStringType() {
        super(NSTRING);
    }

    /**
     * Retrieves a national character string from the specified column in the {@link ResultSet}
     * using {@link ResultSet#getNString(int)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the national character string, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public String get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getNString(columnIndex);
    }

    /**
     * Retrieves a national character string from the specified column in the {@link ResultSet}
     * using {@link ResultSet#getNString(String)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return the national character string, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public String get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getNString(columnName);
    }

    /**
     * Sets a parameter in a {@link PreparedStatement} at the specified index to a national
     * character string value using {@link PreparedStatement#setNString(int, String)}.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the national character string to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final String x) throws SQLException {
        stmt.setNString(columnIndex, x);
    }

    /**
     * Sets a parameter in a {@link CallableStatement} by name to a national character string value
     * using {@link CallableStatement#setNString(String, String)}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the national character string to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final String x) throws SQLException {
        stmt.setNString(parameterName, x);
    }
}
