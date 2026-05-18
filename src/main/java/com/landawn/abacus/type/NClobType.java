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
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.exception.UncheckedSQLException;

/**
 * Type handler for {@link NClob} (National Character Large Object) objects, providing
 * database interaction capabilities for handling large Unicode text data.
 * The {@link #stringOf(NClob)} method reads the entire NCLOB content into a {@link String}
 * and then frees the NCLOB; {@link #valueOf(String)} is not supported.
 */
public class NClobType extends AbstractType<NClob> {

    /** The type name constant for NClob type identification, equal to {@code "NClob"}. */
    public static final String NCLOB = NClob.class.getSimpleName();

    private final Class<NClob> clazz;

    /**
     * Package-private constructor for NClobType using the standard {@link NClob} class.
     * This constructor is called by the TypeFactory to create NClob type instances.
     */
    NClobType() {
        this(NClob.class);
    }

    /**
     * Package-private constructor for NClobType for a specific NClob implementation class.
     * This constructor allows handling of custom NClob implementations.
     *
     * @param clazz the specific NClob class or subclass to handle
     */
    NClobType(Class<? extends NClob> clazz) {
        super(NCLOB);
        this.clazz = (Class<NClob>) clazz;
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the {@link NClob} class object
     */
    @Override
    public Class<NClob> javaType() {
        return clazz;
    }

    /**
     * Converts an {@link NClob} object to its string representation by extracting the full
     * character content. The {@code NClob} is freed via {@link NClob#free()} after extraction.
     * This operation loads the entire NCLOB content into memory and is therefore not suitable
     * for very large objects.
     *
     * @param x the {@code NClob} object to convert, may be {@code null}
     * @return the string content of the {@code NClob}, or {@code null} if the input is {@code null}
     * @throws UncheckedSQLException if a database access error occurs during extraction or freeing
     * @throws UnsupportedOperationException if the NCLOB length exceeds {@link Integer#MAX_VALUE}
     */
    @Override
    public String stringOf(final NClob x) throws UnsupportedOperationException {
        if (x == null) {
            return null;
        }

        RuntimeException primaryException = null;

        try {
            final long len = x.length();
            if (len > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException("NClob too large to convert to String: " + len + " characters");
            }
            return x.getSubString(1, (int) len);
        } catch (final SQLException e) {
            primaryException = new UncheckedSQLException(e);
            throw primaryException;
        } catch (final RuntimeException e) {
            primaryException = e;
            throw primaryException;
        } finally {
            try {
                x.free();
            } catch (final SQLException e) {
                final UncheckedSQLException freeException = new UncheckedSQLException(e);
                if (primaryException != null) {
                    primaryException.addSuppressed(freeException);
                } else {
                    throw freeException; //NOSONAR
                }
            }
        }
    }

    /**
     * Converts a string representation to an {@link NClob} object.
     * This operation is not supported as NCLOBs cannot be created from strings directly
     * and must be obtained from database operations.
     *
     * @param str the string to convert
     * @return never returns normally
     * @throws UnsupportedOperationException always thrown as this operation is not supported
     */
    @Override
    public NClob valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException("NClob cannot be created from string representation");
    }

    /**
     * Retrieves an {@link NClob} value from the specified column in the {@link ResultSet}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve the {@code NClob} from
     * @return the {@code NClob} object, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public NClob get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getNClob(columnIndex);
    }

    /**
     * Retrieves an {@link NClob} value from the specified column in the {@link ResultSet}.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return the {@code NClob} object, or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public NClob get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getNClob(columnName);
    }

    /**
     * Sets a parameter in a {@link PreparedStatement} at the specified index to an {@link NClob} value.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code NClob} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final NClob x) throws SQLException {
        stmt.setNClob(columnIndex, x);
    }

    /**
     * Sets a parameter in a {@link CallableStatement} by name to an {@link NClob} value.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code NClob} value to set, or {@code null} to set SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final NClob x) throws SQLException {
        stmt.setNClob(parameterName, x);
    }
}
