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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Type handler for NCharacterStream (National Character Stream) objects, providing
 * database interaction capabilities for handling Unicode character streams in SQL operations.
 * This type is specifically designed for databases that support national character sets.
 */
public class NCharacterStreamType extends ReaderType {

    public static final String N_CHARACTER_STREAM = "NCharacterStream";

    /**
     * Constructs an NCharacterStreamType.
     * This constructor initializes the type handler for national character stream objects.
     */
    NCharacterStreamType() {
        super(N_CHARACTER_STREAM);
    }

    /**
     * Retrieves a national character stream (Reader) from a ResultSet at the specified column index.
     * The returned Reader can be used to read Unicode character data from the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     *
     * // Reading Unicode text from NCLOB column
     * Reader reader = type.get(rs, 1);
     * if (reader != null) {
     *     String content = IOUtils.readAllChars(reader);
     *     reader.close();
     * }
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) to retrieve the character stream from
     * @return a Reader for the national character stream, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getNCharacterStream(columnIndex);
    }

    /**
     * Retrieves a national character stream (Reader) from a ResultSet using the specified column label.
     * The returned Reader can be used to read Unicode character data from the database.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * ResultSet rs = org.mockito.Mockito.mock(ResultSet.class);
     *
     * // Reading Unicode text from NCLOB column by name
     * Reader reader = type.get(rs, "description");
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
     * @return a Reader for the national character stream, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnName is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getNCharacterStream(columnName);
    }

    /**
     * Sets a parameter in a PreparedStatement to a national character stream value.
     * The Reader will be read until end-of-file is reached for the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
     *
     * // Setting Unicode text to NCLOB column
     * String unicodeText = "Unicode content: 中文";
     * Reader reader = new StringReader(unicodeText);
     * type.set(stmt, 2, reader);
     * stmt.executeUpdate();
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Reader containing the Unicode character stream to set
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setNCharacterStream(columnIndex, x);
    }

    /**
     * Sets a named parameter in a CallableStatement to a national character stream value.
     * The Reader will be read until end-of-file is reached for the stream.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
     *
     * // Setting Unicode text to named parameter
     * String unicodeText = "Content: 日本語";
     * Reader reader = new StringReader(unicodeText);
     * type.set(stmt, "p_content", reader);
     * stmt.execute();
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader containing the Unicode character stream to set
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setNCharacterStream(parameterName, x);
    }

    /**
     * Sets a parameter in a PreparedStatement to a national character stream value with a specified length.
     * Only the specified number of characters will be read from the Reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * PreparedStatement stmt = org.mockito.Mockito.mock(PreparedStatement.class);
     *
     * // Setting Unicode text with specific length
     * String longText = "Very long Unicode text...";
     * Reader reader = new StringReader(longText);
     * type.set(stmt, 2, reader, 100);  // Only read first 100 characters
     * stmt.executeUpdate();
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the Reader containing the Unicode character stream to set
     * @param sqlTypeOrLength the number of characters in the stream
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNCharacterStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a named parameter in a CallableStatement to a national character stream value with a specified length.
     * Only the specified number of characters will be read from the Reader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Reader> type = TypeFactory.getType(Reader.class);
     * CallableStatement stmt = org.mockito.Mockito.mock(CallableStatement.class);
     *
     * // Setting Unicode text with specific length to named parameter
     * String text = "Summary text in Unicode...";
     * Reader reader = new StringReader(text);
     * type.set(stmt, "p_summary", reader, 50);  // Only read first 50 characters
     * stmt.execute();
     * }</pre>
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the Reader containing the Unicode character stream to set
     * @param sqlTypeOrLength the number of characters in the stream
     * @throws SQLException if a database access error occurs or the parameterName is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNCharacterStream(parameterName, x, sqlTypeOrLength);
    }
}