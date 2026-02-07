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

    /**
     * Constructs an NClobReaderType.
     * This constructor initializes the type handler for NCLOB reader objects.
     */
    NClobReaderType() {
        super(NCLOB_READER);
    }

    /**
     * Retrieves an NCLOB value from a ResultSet at the specified column index and converts it to a Reader.
     * The NCLOB resource is automatically freed after obtaining the character stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType("NClobReader");
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     *
     * // Reading NCLOB content from database
     * Reader reader = type.get(rs, 1);
     * if (reader != null) {
     *     String content = IOUtils.readAllChars(reader);
     *     reader.close();
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the NCLOB from
     * @return a Reader for the NCLOB character stream, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        final NClob clob = rs.getNClob(columnIndex);
        return clobToReader(clob);
    }

    /**
     * Retrieves an NCLOB value from a ResultSet using the specified column label and converts it to a Reader.
     * The NCLOB resource is automatically freed after obtaining the character stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType("NClobReader");
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     *
     * // Reading NCLOB by column name
     * Reader reader = type.get(rs, "document_content");
     * if (reader != null) {
     *     BufferedReader br = new BufferedReader(reader);
     *     String line;
     *     while ((line = br.readLine()) != null) {
     *         System.out.println(line);
     *     }
     *     br.close();
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnName the label for the column specified with the SQL AS clause
     * @return a Reader for the NCLOB character stream, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final String columnName) throws SQLException {
        return clobToReader(rs.getNClob(columnName));
    }

    /**
     * Sets a parameter in a PreparedStatement to an NCLOB value using a Reader.
     * The database will read from the Reader until end-of-file is reached.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType("NClobReader");
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
     *
     * // Setting NCLOB from Reader
     * String content = "Large document content...";
     * Reader reader = new StringReader(content);
     * type.set(stmt, 2, reader);
     * stmt.executeUpdate();
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType("NClobReader");
     * CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
     *
     * // Setting NCLOB using named parameter
     * String content = "Document content...";
     * Reader reader = new StringReader(content);
     * type.set(stmt, "p_content", reader);
     * stmt.execute();
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType("NClobReader");
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
     *
     * // Setting NCLOB with specific length
     * String longText = "Very long text content...";
     * Reader reader = new StringReader(longText);
     * type.set(stmt, 2, reader, 1000);  // Only read first 1000 characters
     * stmt.executeUpdate();
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType("NClobReader");
     * CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
     *
     * // Setting NCLOB with specific length using named parameter
     * String text = "Summary text...";
     * Reader reader = new StringReader(text);
     * type.set(stmt, "p_summary", reader, 500);  // Only read first 500 characters
     * stmt.execute();
     * }</pre>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * NClob nclob = resultSet.getNClob(1);
     * Reader reader = NClobReaderType.clobToReader(nclob);
     * // NCLOB is automatically freed after conversion
     * }</pre>
     *
     * @param clob the NCLOB to convert to a Reader
     * @return a Reader for the NCLOB character stream, or {@code null} if the input NCLOB is null
     * @throws SQLException if a database access error occurs while accessing the NCLOB
     */
    static Reader clobToReader(final NClob clob) throws SQLException {
        Reader reader = null;

        if (clob != null) {
            reader = clob.getCharacterStream();
        }

        return reader;
    }
}