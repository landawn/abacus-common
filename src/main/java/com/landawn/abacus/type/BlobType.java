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

import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for SQL Blob (Binary Large Object) operations.
 * This class provides direct handling of java.sql.Blob objects for database operations.
 * Note that Blob objects cannot be converted to/from strings, so string conversion
 * methods throw UnsupportedOperationException.
 */
public class BlobType extends AbstractType<Blob> {

    /**
     * The type name constant for Blob type identification.
     */
    public static final String BLOB = Blob.class.getSimpleName();

    private final Class<Blob> clazz;

    BlobType() {
        this(Blob.class);
    }

    BlobType(Class<? extends Blob> clazz) {
        super(BLOB);
        this.clazz = (Class<Blob>) clazz;
    }

    /**
     * Returns the Class object representing the Blob interface.
     *
     * @return the Class object for java.sql.Blob
     */
    @Override
    public Class<Blob> clazz() {
        return clazz;
    }

    /**
     * String conversion is not supported for Blob types.
     * Blob objects contain binary data that cannot be meaningfully represented as strings.
     *
     * @param x the Blob value (ignored)
     * @return never returns, always throws exception
     * @throws UnsupportedOperationException always, as Blobs cannot be converted to strings
     */
    @Override
    public String stringOf(final Blob x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * String parsing is not supported for Blob types.
     * Blob objects cannot be created from string representations.
     *
     * @param str the string value (ignored)
     * @return never returns, always throws exception
     * @throws UnsupportedOperationException always, as Blobs cannot be created from strings
     */
    @Override
    public Blob valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves a Blob value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to retrieve the Blob from
     * @param columnIndex the column index (1-based) of the Blob value
     * @return the Blob object at the specified column, or null if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Blob get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBlob(columnIndex);
    }

    /**
     * Retrieves a Blob value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to retrieve the Blob from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return the Blob object in the specified column, or null if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public Blob get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getBlob(columnLabel);
    }

    /**
     * Sets a Blob parameter in a PreparedStatement at the specified position.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Blob value to set, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Blob x) throws SQLException {
        stmt.setBlob(columnIndex, x);
    }

    /**
     * Sets a named Blob parameter in a CallableStatement.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Blob value to set, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Blob x) throws SQLException {
        stmt.setBlob(parameterName, x);
    }
}