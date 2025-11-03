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

import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for BLOB (Binary Large Object) InputStream operations.
 * This class specializes in converting between SQL Blob objects and InputStreams,
 * providing seamless integration between database BLOB data and Java I/O streams.
 */
class BlobInputStreamType extends InputStreamType {

    /**
     * The type name constant for BLOB InputStream type identification.
     */
    public static final String BLOB_INPUT_STREAM = "BlobInputStream";

    BlobInputStreamType() {
        super(BLOB_INPUT_STREAM);
    }

    /**
     * Retrieves a BLOB as an InputStream from a ResultSet at the specified column index.
     * Converts the SQL Blob to an InputStream for reading binary data.
     *
     * @param rs the ResultSet to retrieve the BLOB from
     * @param columnIndex the column index (1-based) of the BLOB value
     * @return an InputStream for reading the BLOB data, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Blob blob = rs.getBlob(columnIndex);
        return blob2InputStream(blob);
    }

    /**
     * Retrieves a BLOB as an InputStream from a ResultSet using the specified column label.
     * Converts the SQL Blob to an InputStream for reading binary data.
     *
     * @param rs the ResultSet to retrieve the BLOB from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return an InputStream for reading the BLOB data, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnLabel) throws SQLException {
        return blob2InputStream(rs.getBlob(columnLabel));
    }

    /**
     * Sets an InputStream as a BLOB parameter in a PreparedStatement at the specified position.
     * The JDBC driver will read from the stream to populate the BLOB.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the InputStream containing the binary data for the BLOB, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setBlob(columnIndex, x);
    }

    /**
     * Sets a named InputStream as a BLOB parameter in a CallableStatement.
     * The JDBC driver will read from the stream to populate the BLOB.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the InputStream containing the binary data for the BLOB, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setBlob(parameterName, x);
    }

    /**
     * Sets an InputStream as a BLOB parameter in a PreparedStatement with a specified length.
     * This method allows specification of the stream length for optimization.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the InputStream containing the binary data for the BLOB, may be null
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBlob(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a named InputStream as a BLOB parameter in a CallableStatement with a specified length.
     * This method allows specification of the stream length for optimization.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the InputStream containing the binary data for the BLOB, may be null
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBlob(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Converts a SQL Blob object to an InputStream for reading its binary content.
     * This static utility method extracts the binary stream from the Blob.
     *
     * @param blob the SQL Blob to convert
     * @return an InputStream for reading the Blob's content, or {@code null} if the blob is null
     * @throws SQLException if there is an error accessing the BLOB value
     */
    static InputStream blob2InputStream(final Blob blob) throws SQLException {
        if (blob != null) {
            return blob.getBinaryStream();
        }

        return null; // NOSONAR
    }
}
