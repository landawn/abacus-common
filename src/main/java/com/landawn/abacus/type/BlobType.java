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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.ClassUtil;

/**
 * Type handler for SQL {@link java.sql.Blob} (Binary Large Object) values.
 * Provides direct JDBC read/write operations for {@code Blob} objects.
 *
 * <p>String conversion ({@link #stringOf(java.sql.Blob)} and {@link #valueOf(String)}) is
 * <em>not supported</em> for {@code Blob} values; both methods throw
 * {@link UnsupportedOperationException}. {@link #valueOf(Object)} returns an object that already is
 * an instance of the handled {@code Blob} class unchanged, maps {@code null} to {@code null}, and
 * rejects everything else without touching it.
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
@SuppressWarnings("java:S2160")
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
     * @throws IllegalArgumentException if {@code clazz} is {@code null}.
     */
    BlobType(final Class<? extends Blob> clazz) throws IllegalArgumentException {
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
     * Indicates that {@code Blob} values do not support the type system's string-based
     * serialization path. They must be handled through the dedicated JDBC operations.
     *
     * @return {@code false}, because both string conversion directions are unsupported
     */
    @Override
    public boolean isSerializable() {
        return false;
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
     * Returns {@code obj} unchanged if it already is an instance of the {@link Blob} class handled by
     * this type; otherwise the conversion is not supported.
     * Unlike the inherited default, this method never reads or frees the supplied object: a
     * {@link Blob} cannot be reconstructed from its string form, so converting through
     * {@link #stringOf(Blob)} would only fail after the caller's locator had been read.
     *
     * @param obj the object to convert; may be {@code null}
     * @return the same {@link Blob} instance if {@code obj} is an instance of {@link #javaType()},
     *         or {@code null} if {@code obj} is {@code null}
     * @throws UnsupportedOperationException if {@code obj} is non-null and not an instance of {@link #javaType()}
     */
    @MayReturnNull
    @Override
    public Blob valueOf(final Object obj) throws UnsupportedOperationException {
        if (obj == null) {
            return null; // NOSONAR
        } else if (clazz.isInstance(obj)) {
            return clazz.cast(obj);
        }

        throw new UnsupportedOperationException("Blob cannot be created from " + obj.getClass().getName());
    }

    /**
     * Retrieves a {@link java.sql.Blob} from a {@link java.sql.ResultSet} at the specified column index.
     * Delegates to {@link java.sql.ResultSet#getBlob(int)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the BLOB column
     * @return the {@code Blob} object at the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public Blob get(final ResultSet rs, final int columnIndex) throws NullPointerException, SQLException {
        return rs.getBlob(columnIndex);
    }

    /**
     * Retrieves a {@link java.sql.Blob} from a {@link java.sql.ResultSet} using the specified column label.
     * Delegates to {@link java.sql.ResultSet#getBlob(String)}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return the {@code Blob} object in the specified column, or {@code null} if the column value is SQL NULL
     * @throws NullPointerException if {@code rs} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Blob get(final ResultSet rs, final String columnName) throws NullPointerException, SQLException {
        return rs.getBlob(columnName);
    }

    /**
     * Sets a {@link java.sql.Blob} parameter on a {@link java.sql.PreparedStatement} at the specified position.
     * Delegates to {@link java.sql.PreparedStatement#setBlob(int, java.sql.Blob)}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code Blob} value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Blob x) throws NullPointerException, SQLException {
        stmt.setBlob(columnIndex, x);
    }

    /**
     * Sets a named {@link java.sql.Blob} parameter on a {@link java.sql.CallableStatement}.
     * Delegates to {@link java.sql.CallableStatement#setBlob(String, java.sql.Blob)}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code Blob} value to set; may be {@code null}
     * @throws NullPointerException if {@code stmt} is null when the JDBC operation is invoked
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Blob x) throws NullPointerException, SQLException {
        stmt.setBlob(parameterName, x);
    }
}
