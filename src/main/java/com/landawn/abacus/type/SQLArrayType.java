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

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLArrayType extends AbstractType<Array> {

    static final String SQL_ARRAY = "SQLArray";

    SQLArrayType() {
        super(SQL_ARRAY);
    }

    /**
     * Returns the Class object representing the SQL Array type.
     *
     * @return the Class object for java.sql.Array.class
     */
    @Override
    public Class<Array> clazz() {
        return Array.class;
    }

    /**
     * Indicates whether this type is serializable.
     * SQL Array types are not serializable as they represent database-specific array structures.
     *
     * @return false, indicating this type is not serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Converts an Array object to its string representation.
     * This operation is not supported for SQL Array types as they are database-specific
     * and cannot be reliably converted to a string format.
     *
     * @param x the Array object to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as Array cannot be converted to string
     */
    @Override
    public String stringOf(final Array x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates an Array object from a string representation.
     * This operation is not supported for SQL Array types as they are database-specific
     * and cannot be created from a string representation.
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as Array cannot be created from string
     */
    @Override
    public Array valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a SQL ARRAY value from the specified column in the ResultSet.
     * A SQL ARRAY represents an array value in the database.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the Array value from the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Array get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getArray(columnIndex);
    }

    /**
     * Retrieves a SQL ARRAY value from the specified column in the ResultSet.
     * A SQL ARRAY represents an array value in the database.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the Array value from the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Array get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getArray(columnLabel);
    }

    /**
     * Sets an Array parameter in a PreparedStatement.
     * The Array represents a SQL ARRAY value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Array value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Array x) throws SQLException {
        stmt.setArray(columnIndex, x);
    }

    /**
     * Sets an Array parameter in a CallableStatement.
     * The Array represents a SQL ARRAY value.
     * Note: This method uses setObject instead of setArray as CallableStatement may not support setArray with parameter names.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Array value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Array x) throws SQLException {
        // stmt.setArray(parameterName, x);

        stmt.setObject(parameterName, x);
    }
}