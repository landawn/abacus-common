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

import com.landawn.abacus.util.ClassUtil;

/**
 * Type handler for SQL {@link java.sql.Blob} (Binary Large Object) values.
 * Provides direct JDBC read/write operations for {@code Blob} objects.
 *
 * <p>String conversion ({@link #stringOf} and {@link #valueOf}) is <em>not supported</em>
 * for {@code Blob} values; both methods throw {@link UnsupportedOperationException}.
 * Use {@link BlobInputStreamType} to work with BLOB data as an {@link java.io.InputStream}.</p>
 *
 * <p>JDBC mapping: retrieved via {@link java.sql.ResultSet#getBlob(int)} /
 * {@link java.sql.ResultSet#getBlob(String)} and stored via
 * {@link java.sql.PreparedStatement#setBlob(int, java.sql.Blob)} /
 * {@link java.sql.CallableStatement#setBlob(String, java.sql.Blob)}.</p>
 *
 * @see BlobInputStreamType
 * @see java.sql.Blob
 */
public class BlobType extends AbstractType<Blob> {

    /**
     * The type name constant used to identify this type within the type system
     * (value: {@code "Blob"}).
     */
    public static final String BLOB = Blob.class.getSimpleName();

    /** The specific {@link java.sql.Blob} implementation class managed by this handler. */
    private final Class<Blob> clazz;

    /**
     * Package-private constructor for {@code BlobType} using the standard {@link java.sql.Blob} interface.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    BlobType() {
        this(Blob.class);
    }

    /**
     * Package-private constructor for {@code BlobType} with a specific {@link java.sql.Blob} implementation class.
     * Instances are created by {@link TypeFactory}; do not instantiate directly. The handler name is
     * derived from {@code clazz}, so a driver-specific implementation remains distinguishable from
     * the standard {@code Blob} interface in type metadata.
     *
     * @param clazz the specific {@code Blob} implementation class to use as the Java type
     */
    BlobType(final Class<? extends Blob> clazz) {
        super(ClassUtil.getSimpleClassName(clazz));
        this.clazz = (Class<Blob>) clazz;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return the {@code Class} for the {@link java.sql.Blob} type or its specific implementation
     */
    @Override
    public Class<Blob> javaType() {
        return clazz;
    }

    /**
     * Not supported for {@code Blob} types.
     * {@code Blob} objects contain raw binary data with no meaningful string representation.
     *
     * @param x the {@code Blob} value (ignored)
     * @return this method never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public String stringOf(final Blob x) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Blob cannot be converted to string representation");
    }

    /**
     * Not supported for {@code Blob} types.
     * {@code Blob} objects cannot be created from a string representation.
     *
     * @param str the string value (ignored)
     * @return this method never returns normally
     * @throws UnsupportedOperationException always
     */
    @Override
    public Blob valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Blob cannot be created from string representation");
    }

    /**
     * Retrieves a {@link java.sql.Blob} from a {@link java.sql.ResultSet} at the specified column index.
     * Delegates to {@link java.sql.ResultSet#getBlob(int)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the BLOB column
     * @return the {@code Blob} object at the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public Blob get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBlob(columnIndex);
    }

    /**
     * Retrieves a {@link java.sql.Blob} from a {@link java.sql.ResultSet} using the specified column label.
     * Delegates to {@link java.sql.ResultSet#getBlob(String)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the {@code Blob} object in the specified column, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Blob get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getBlob(columnName);
    }

    /**
     * Sets a {@link java.sql.Blob} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * Delegates to {@link java.sql.PreparedStatement#setBlob(int, java.sql.Blob)}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code Blob} value to set; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Blob x) throws SQLException {
        stmt.setBlob(columnIndex, x);
    }

    /**
     * Sets a named {@link java.sql.Blob} parameter on a {@link java.sql.CallableStatement}.
     * Delegates to {@link java.sql.CallableStatement#setBlob(String, java.sql.Blob)}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code Blob} value to set; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Blob x) throws SQLException {
        stmt.setBlob(parameterName, x);
    }
}
