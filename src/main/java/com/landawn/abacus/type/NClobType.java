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
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Type<NClob> type = TypeFactory.getType(NClob.class);
 *
 * // Reading from database
 * NClob nclob = type.get(resultSet, 1);
 * String content = type.stringOf(nclob);   // Converts NClob to String
 *
 * // Writing to database
 * type.set(preparedStatement, 1, nclob);
 * }</pre>
 */
public class NClobType extends AbstractType<NClob> {

    public static final String NCLOB = NClob.class.getSimpleName();

    private final Class<NClob> clazz;

    /**
     * Constructs an NClobType for the standard NClob class.
     * This constructor initializes the type handler for NCLOB database objects.
     */
    NClobType() {
        this(NClob.class);
    }

    /**
     * Constructs an NClobType for a specific NClob implementation class.
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
    public Class<NClob> clazz() {
        return clazz;
    }

    /**
     * Converts an {@link NClob} object to its string representation.
     * <p>
     * This method extracts the entire content of the NClob as a string. The NClob is
     * automatically freed after extraction. Note that this operation loads the entire
     * NClob content into memory, which may not be suitable for very large objects.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NClob nclob = resultSet.getNClob(1);
     * String content = type.stringOf(nclob);   // Extracts full content and frees NClob
     * }</pre>
     *
     * @param x the NClob object to convert
     * @return the string content of the NClob, or {@code null} if the input is null
     * @throws UncheckedSQLException if a database access error occurs during extraction or freeing
     */
    @Override
    public String stringOf(final NClob x) throws UnsupportedOperationException {
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
        throw new UnsupportedOperationException();
    }

    /**
     * Retrieves an {@link NClob} value from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the NCLOB from
     * @return the NClob object from the ResultSet, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public NClob get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getNClob(columnIndex);
    }

    /**
     * Retrieves an {@link NClob} value from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return the NClob object from the ResultSet, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public NClob get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getNClob(columnLabel);
    }

    /**
     * Sets a parameter in a PreparedStatement to an {@link NClob} value.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the NClob value to set, or {@code null} to set SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final NClob x) throws SQLException {
        stmt.setNClob(columnIndex, x);
    }

    /**
     * Sets a named parameter in a CallableStatement to an {@link NClob} value.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the NClob value to set, or {@code null} to set SQL NULL
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final NClob x) throws SQLException {
        stmt.setNClob(parameterName, x);
    }
}
