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
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.exception.UncheckedSQLException;

/**
 * Type handler for CLOB (Character Large Object) values.
 * This class provides database operations for handling CLOB objects directly.
 * Note that string serialization/deserialization is not supported for CLOB types
 * as CLOBs are database-specific objects that cannot be meaningfully represented as strings.
 */
public class ClobType extends AbstractType<Clob> {

    public static final String CLOB = Clob.class.getSimpleName();

    private final Class<Clob> clazz;

    ClobType() {
        this(Clob.class);
    }

    ClobType(Class<? extends Clob> clazz) {
        super(CLOB);
        this.clazz = (Class<Clob>) clazz;
    }

    /**
     * Returns the Java class type handled by this type handler.
     *
     * @return The Class object representing Clob.class
     */
    @Override
    public Class<Clob> clazz() {
        return clazz;
    }

    /**
     * Converts a CLOB object to its string representation by extracting the complete character content.
     * This method reads the entire CLOB content and returns it as a String.
     * The CLOB is automatically freed after reading.
     *
     * @param x the CLOB object to convert. Can be {@code null}.
     * @return The complete character content of the CLOB as a String, or {@code null} if input is null
     * @throws UncheckedSQLException if a SQLException occurs while reading the CLOB content or freeing resources
     */
    @Override
    public String stringOf(final Clob x) {
        if (x == null) {
            return null;
        }

        try {
            return x.getSubString(1, (int) x.length());
        } catch (final SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            try {
                x.free();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);   //NOSONAR
            }
        }
    }

    /**
     * String deserialization is not supported for CLOB objects.
     * CLOBs are database-specific objects that must be created through JDBC.
     *
     * @param str the string value (parameter is ignored)
     * @return Never returns normally
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     */
    @Override
    public Clob valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a CLOB value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the CLOB value
     * @return The CLOB object at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Clob get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getClob(columnIndex);
    }

    /**
     * Retrieves a CLOB value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the CLOB value
     * @return The CLOB object in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Clob get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getClob(columnLabel);
    }

    /**
     * Sets a CLOB value as a parameter in a PreparedStatement.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the CLOB object to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Clob x) throws SQLException {
        stmt.setClob(columnIndex, x);
    }

    /**
     * Sets a CLOB value as a named parameter in a CallableStatement.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the CLOB object to set. Can be {@code null}.
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Clob x) throws SQLException {
        stmt.setClob(parameterName, x);
    }
}
