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
 * Type handler for {@link java.sql.Blob} columns exposed as {@link java.io.InputStream} values.
 * Retrieval operations convert a SQL {@link java.sql.Blob} to an {@code InputStream} via
 * {@link java.sql.Blob#getBinaryStream()}, while storage operations delegate to the JDBC
 * {@code setBlob} methods.
 *
 * <p>When reading, the returned stream reads directly from the underlying BLOB object.
 * Callers are responsible for closing the stream after use.</p>
 *
 * @see InputStreamType
 * @see java.sql.Blob
 */
public class BlobInputStreamType extends InputStreamType {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "BlobInputStream"}).
     */
    public static final String BLOB_INPUT_STREAM = "BlobInputStream";

    /**
     * Package-private constructor for {@code BlobInputStreamType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BlobInputStreamType() {
        super(BLOB_INPUT_STREAM);
    }

    /**
     * Retrieves a BLOB column as an {@link java.io.InputStream} from a {@link java.sql.ResultSet}
     * at the specified column index.
     * Reads the column as a {@link java.sql.Blob} and returns its binary stream.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the BLOB column
     * @return an {@code InputStream} for reading the BLOB's binary content,
     *         or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Blob blob = rs.getBlob(columnIndex);
        return blobToInputStream(blob);
    }

    /**
     * Retrieves a BLOB column as an {@link java.io.InputStream} from a {@link java.sql.ResultSet}
     * using the specified column label.
     * Reads the column as a {@link java.sql.Blob} and returns its binary stream.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return an {@code InputStream} for reading the BLOB's binary content,
     *         or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnName) throws SQLException {
        return blobToInputStream(rs.getBlob(columnName));
    }

    /**
     * Sets an {@link java.io.InputStream} as a BLOB parameter on a {@link java.sql.PreparedStatement}
     * at the specified position.
     * The JDBC driver reads data from the stream as needed to populate the BLOB.
     * Delegates to {@link java.sql.PreparedStatement#setBlob(int, java.io.InputStream)}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code InputStream} containing binary BLOB data; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setBlob(columnIndex, x);
    }

    /**
     * Sets a named {@link java.io.InputStream} as a BLOB parameter on a {@link java.sql.CallableStatement}.
     * The JDBC driver reads data from the stream as needed to populate the BLOB.
     * Delegates to {@link java.sql.CallableStatement#setBlob(String, java.io.InputStream)}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code InputStream} containing binary BLOB data; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setBlob(parameterName, x);
    }

    /**
     * Sets an {@link java.io.InputStream} as a BLOB parameter on a {@link java.sql.PreparedStatement}
     * at the specified position, with an explicit byte-length hint for the JDBC driver.
     * Delegates to {@link java.sql.PreparedStatement#setBlob(int, java.io.InputStream, long)}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code InputStream} containing binary BLOB data; may be {@code null}
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBlob(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a named {@link java.io.InputStream} as a BLOB parameter on a {@link java.sql.CallableStatement},
     * with an explicit byte-length hint for the JDBC driver.
     * Delegates to {@link java.sql.CallableStatement#setBlob(String, java.io.InputStream, long)}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code InputStream} containing binary BLOB data; may be {@code null}
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBlob(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Converts a {@link java.sql.Blob} to an {@link java.io.InputStream} for reading its binary content.
     * Returns {@code null} if {@code blob} is {@code null}; otherwise delegates to
     * {@link java.sql.Blob#getBinaryStream()}.
     *
     * @param blob the SQL {@code Blob} to convert; may be {@code null}
     * @return an {@code InputStream} for reading the BLOB's binary content,
     *         or {@code null} if {@code blob} is {@code null}
     * @throws SQLException if a database access error occurs while opening the BLOB stream
     */
    static InputStream blobToInputStream(final Blob blob) throws SQLException {
        if (blob != null) {
            return blob.getBinaryStream();
        }

        return null; // NOSONAR
    }
}
