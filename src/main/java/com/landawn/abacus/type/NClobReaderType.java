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
 * This type automatically converts NCLOB database values to Reader objects and manages
 * the lifecycle of the NCLOB resources.
 */
public class NClobReaderType extends ReaderType {

    public static final String NCLOB_READER = "NClobReader";

    NClobReaderType() {
        super(NCLOB_READER);
    }

    /**
     * Retrieves an NCLOB value from a ResultSet at the specified column index and converts it to a Reader.
     * The NCLOB resource is automatically freed after obtaining the character stream.
     * 
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the NCLOB from
     * @return a Reader for the NCLOB character stream, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        final NClob clob = rs.getNClob(columnIndex);
        return clob2Reader(clob);
    }

    /**
     * Retrieves an NCLOB value from a ResultSet using the specified column label and converts it to a Reader.
     * The NCLOB resource is automatically freed after obtaining the character stream.
     * 
     * @param rs the ResultSet to read from
     * @param columnLabel the label for the column specified with the SQL AS clause
     * @return a Reader for the NCLOB character stream, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final String columnLabel) throws SQLException {
        return clob2Reader(rs.getNClob(columnLabel));
    }

    /**
     * Sets a parameter in a PreparedStatement to an NCLOB value using a Reader.
     * The database will read from the Reader until end-of-file is reached.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Reader containing the character data to be stored as NCLOB
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setNClob(columnIndex, x);
    }

    /**
     * Sets a named parameter in a CallableStatement to an NCLOB value using a Reader.
     * The database will read from the Reader until end-of-file is reached.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader containing the character data to be stored as NCLOB
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setNClob(parameterName, x);
    }

    /**
     * Sets a parameter in a PreparedStatement to an NCLOB value using a Reader with a specified length.
     * Only the specified number of characters will be read from the Reader.
     * 
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Reader containing the character data to be stored as NCLOB
     * @param sqlTypeOrLength the number of characters in the stream
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNClob(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a named parameter in a CallableStatement to an NCLOB value using a Reader with a specified length.
     * Only the specified number of characters will be read from the Reader.
     * 
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader containing the character data to be stored as NCLOB
     * @param sqlTypeOrLength the number of characters in the stream
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNClob(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Converts an NCLOB object to a Reader and frees the NCLOB resource.
     * This method extracts the character stream from the NCLOB and then releases
     * the NCLOB resources to prevent memory leaks.
     * 
     * @param clob the NCLOB to convert to a Reader
     * @return a Reader for the NCLOB character stream, or null if the input NCLOB is null
     * @throws SQLException if a database access error occurs while accessing the NCLOB
     */
    static Reader clob2Reader(final NClob clob) throws SQLException {
        Reader reader = null;

        if (clob != null) {
            reader = clob.getCharacterStream();
            clob.free();
        }

        return reader;
    }
}