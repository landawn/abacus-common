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
     * Package-private constructor for NCharacterStreamType.
     * This constructor is called by the TypeFactory to create NCharacterStream type instances.
     */
    NCharacterStreamType() {
        super(N_CHARACTER_STREAM);
    }

    /**
     * Retrieves a national character stream ({@link java.io.Reader}) from the specified column
     * in the {@link ResultSet} using {@link ResultSet#getNCharacterStream(int)}.
     * The returned {@code Reader} can be used to read Unicode character data from the database.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnIndex the 1-based index of the column to retrieve the character stream from
     * @return a {@code Reader} for the national character stream,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getNCharacterStream(columnIndex);
    }

    /**
     * Retrieves a national character stream ({@link java.io.Reader}) from the specified column
     * in the {@link ResultSet} using {@link ResultSet#getNCharacterStream(String)}.
     * The returned {@code Reader} can be used to read Unicode character data from the database.
     *
     * @param rs the {@code ResultSet} to read from
     * @param columnName the label of the column to retrieve (as specified in the SQL AS clause)
     * @return a {@code Reader} for the national character stream,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public Reader get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getNCharacterStream(columnName);
    }

    /**
     * Sets a national character stream parameter in a {@link PreparedStatement} at the specified index.
     * The {@link java.io.Reader} will be read until end-of-file is reached.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code Reader} containing the Unicode character stream to set
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setNCharacterStream(columnIndex, x);
    }

    /**
     * Sets a national character stream parameter in a {@link CallableStatement} by name.
     * The {@link java.io.Reader} will be read until end-of-file is reached.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Reader} containing the Unicode character stream to set
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setNCharacterStream(parameterName, x);
    }

    /**
     * Sets a national character stream parameter in a {@link PreparedStatement} at the specified index,
     * reading at most {@code sqlTypeOrLength} characters from the {@link java.io.Reader}.
     *
     * @param stmt the {@code PreparedStatement} to set the parameter on
     * @param columnIndex the 1-based index of the parameter to set
     * @param x the {@code Reader} containing the Unicode character stream to set
     * @param sqlTypeOrLength the maximum number of characters to read from the stream
     * @throws SQLException if a database access error occurs or {@code columnIndex} is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNCharacterStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a national character stream parameter in a {@link CallableStatement} by name,
     * reading at most {@code sqlTypeOrLength} characters from the {@link java.io.Reader}.
     *
     * @param stmt the {@code CallableStatement} to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the {@code Reader} containing the Unicode character stream to set
     * @param sqlTypeOrLength the maximum number of characters to read from the stream
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setNCharacterStream(parameterName, x, sqlTypeOrLength);
    }
}
