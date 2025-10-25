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

    NStringType() {
        super(NSTRING);
    }

    /**
     * Retrieves a national character string value from a ResultSet at the specified column index.
     * This method is designed to handle Unicode strings stored in national character columns.
     * 
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the string from
     * @return the national string value from the ResultSet, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public String get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getNString(columnIndex);
    }

    /**
     * Retrieves a national character string value from a ResultSet using the specified column label.
     * This method is designed to handle Unicode strings stored in national character columns.
     * 
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return the national string value from the ResultSet, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public String get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getNString(columnLabel);
    }

    /**
     * Sets a parameter in a PreparedStatement to a national character string value.
     * This method ensures the string is stored using the database's national character set.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the national string value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final String x) throws SQLException {
        stmt.setNString(columnIndex, x);
    }

    /**
     * Sets a named parameter in a CallableStatement to a national character string value.
     * This method ensures the string is stored using the database's national character set.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the national string value to set, or null to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final String x) throws SQLException {
        stmt.setNString(parameterName, x);
    }
}