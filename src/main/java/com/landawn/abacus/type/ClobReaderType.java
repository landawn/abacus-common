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
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for CLOB character stream values.
 * This class provides database operations for handling Character Large Objects (CLOBs)
 * as character Readers. It extends ReaderType to handle CLOB data specifically
 * through character stream representations.
 */
public class ClobReaderType extends ReaderType {

    public static final String CLOB_READER = "ClobReader";

    ClobReaderType() {
        super(CLOB_READER);
    }

    /**
     * Retrieves a CLOB value as a character Reader from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the CLOB value
     * @return A Reader containing the character stream of the CLOB, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Clob clob = rs.getClob(columnIndex);
        return clob2Reader(clob);
    }

    /**
     * Retrieves a CLOB value as a character Reader from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the CLOB value
     * @return A Reader containing the character stream of the CLOB, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public Reader get(final ResultSet rs, final String columnLabel) throws SQLException {
        return clob2Reader(rs.getClob(columnLabel));
    }

    /**
     * Sets a Reader as a CLOB parameter in a PreparedStatement.
     * The reader's content will be stored as CLOB data in the database.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Reader containing the character data. Can be null.
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setClob(columnIndex, x);
    }

    /**
     * Sets a Reader as a named CLOB parameter in a CallableStatement.
     * The reader's content will be stored as CLOB data in the database.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the Reader containing the character data. Can be null.
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setClob(parameterName, x);
    }

    /**
     * Sets a Reader as a CLOB parameter in a PreparedStatement with a specified length.
     * Only the specified number of characters will be read from the Reader and stored.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Reader containing the character data. Can be null.
     * @param sqlTypeOrLength the number of characters to read from the Reader
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setClob(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a Reader as a named CLOB parameter in a CallableStatement with a specified length.
     * Only the specified number of characters will be read from the Reader and stored.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the Reader containing the character data. Can be null.
     * @param sqlTypeOrLength the number of characters to read from the Reader
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setClob(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Converts a CLOB to a character Reader.
     * This is a utility method used internally to extract character streams from CLOB objects.
     *
     * @param clob the CLOB to convert. Can be null.
     * @return A Reader for the CLOB's character stream, or null if the CLOB is null
     * @throws SQLException if a database access error occurs while accessing the CLOB
     */
    static Reader clob2Reader(final Clob clob) throws SQLException {
        if (clob != null) {
            return clob.getCharacterStream();
        }

        return null; // NOSONAR
    }
}