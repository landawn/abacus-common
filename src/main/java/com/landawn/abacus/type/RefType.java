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
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.SQLException;

public class RefType extends AbstractType<Ref> {

    public static final String REF = Ref.class.getSimpleName();

    RefType() {
        super(REF);
    }

    /**
     * Returns the Class object representing the SQL Ref type.
     *
     * @return the Class object for java.sql.Ref.class
     */
    @Override
    public Class<Ref> clazz() {
        return Ref.class;
    }

    /**
     * Indicates whether this type is serializable.
     * SQL Ref types are not serializable as they represent database references.
     *
     * @return false, indicating this type is not serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Converts a Ref object to its string representation.
     * This operation is not supported for SQL Ref types.
     *
     * @param x the Ref object to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as Ref cannot be converted to string
     */
    @Override
    public String stringOf(final Ref x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a Ref object from a string representation.
     * This operation is not supported for SQL Ref types.
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as Ref cannot be created from string
     */
    @Override
    public Ref valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a SQL REF value from the specified column in the ResultSet.
     * A REF value represents a reference to an SQL structured type value in the database.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the 1-based index of the column to retrieve
     * @return the Ref value from the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Ref get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getRef(columnIndex);
    }

    /**
     * Retrieves a SQL REF value from the specified column in the ResultSet.
     * A REF value represents a reference to an SQL structured type value in the database.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column to retrieve (column name or alias)
     * @return the Ref value from the specified column, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Ref get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getRef(columnLabel);
    }

    /**
     * Sets a Ref parameter in a PreparedStatement.
     * The Ref represents a reference to an SQL structured type value in the database.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the Ref value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Ref x) throws SQLException {
        stmt.setRef(columnIndex, x);
    }

    /**
     * Sets a Ref parameter in a CallableStatement.
     * The Ref represents a reference to an SQL structured type value in the database.
     * Note: This method uses setObject instead of setRef as CallableStatement may not support setRef with parameter names.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Ref value to set as the parameter
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Ref x) throws SQLException {
        // stmt.setRef(parameterName, x);

        stmt.setObject(parameterName, x);
    }
}