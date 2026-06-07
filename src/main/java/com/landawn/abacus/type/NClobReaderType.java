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

import java.io.Reader;
import java.sql.CallableStatement;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for NClobReader objects, providing database interaction capabilities
 * for handling National Character Large Objects (NCLOB) as Reader streams.
 * This type automatically converts NCLOB database values to Reader objects.
 */
public class NClobReaderType extends ReaderType {

    /** The type name constant for NClobReader type identification, equal to {@code "NClobReader"}. */
    public static final String NCLOB_READER = "NClobReader";

    /**
     * Package-private constructor for NClobReaderType.
     * This constructor is called by the TypeFactory to create NClobReader type instances.
     */
    NClobReaderType() {
        super(NCLOB_READER);
    }

    /**
     * Retrieves an {@link java.sql.NClob} from the specified column in the {@link ResultSet}
     * and converts it to a {@link java.io.Reader} via {@link java.sql.NClob#getCharacterStream()}.
     * The {@code NClob} itself is not freed by this method; callers are responsible for
     * closing the returned {@code Reader} and releasing any associated database resources.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve the {@code NClob} from
     * @return a {@code Reader} for the {@code NClob} character stream,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        final NClob clob = rs.getNClob(columnIndex);
        return clobToReader(clob);
    }

    /**
     * Retrieves an {@link java.sql.NClob} from the specified column in the {@link ResultSet}
     * and converts it to a {@link java.io.Reader} via {@link java.sql.NClob#getCharacterStream()}.
     * The {@code NClob} itself is not freed by this method; callers are responsible for
     * closing the returned {@code Reader} and releasing any associated database resources.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code Reader} for the {@code NClob} character stream,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Reader get(final ResultSet rs, final String columnName) throws SQLException {
        return clobToReader(rs.getNClob(columnName));
    }

    /**
     * Sets a parameter in a {@link PreparedStatement} to an {@code NCLOB} value using a {@link java.io.Reader}.
     * The database will read from the {@code Reader} until end-of-file is reached.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code Reader} containing the character data to be stored as {@code NCLOB}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setNClob(columnIndex, x);
    }

    /**
     * Sets a parameter in a {@link CallableStatement} by name to an {@code NCLOB} value using a {@link java.io.Reader}.
     * The database will read from the {@code Reader} until end-of-file is reached.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Reader} containing the character data to be stored as {@code NCLOB}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setNClob(parameterName, x);
    }

    /**
     * Sets a parameter in a {@link PreparedStatement} at the specified index to an {@code NCLOB} value,
     * reading at most {@code sqlTypeOrLength} characters from the {@link java.io.Reader}.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code Reader} containing the character data to be stored as {@code NCLOB}
     * @param sqlTypeOrLength the maximum number of characters to read from the stream
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNClob(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a parameter in a {@link CallableStatement} by name to an {@code NCLOB} value,
     * reading at most {@code sqlTypeOrLength} characters from the {@link java.io.Reader}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Reader} containing the character data to be stored as {@code NCLOB}
     * @param sqlTypeOrLength the maximum number of characters to read from the stream
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNClob(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Converts an NClob object to a Reader by extracting its character stream.
     * The NClob is not freed by this method; the caller is responsible
     * for managing the NClob lifecycle.
     *
     * @param clob the NClob to convert to a Reader
     * @return a Reader for the NClob character stream, or {@code null} if the input NClob is {@code null}
     * @throws SQLException if a database access error occurs while accessing the NClob
     */
    static Reader clobToReader(final NClob clob) throws SQLException {
        if (clob != null) {
            return clob.getCharacterStream();
        }

        return null; // NOSONAR
    }
}
