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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;

/**
 * Type handler for CLOB ASCII stream values.
 * This class provides database operations for handling Character Large Objects (CLOBs)
 * as ASCII input streams. It extends InputStreamType to handle CLOB data specifically
 * through ASCII stream representations.
 */
public class ClobAsciiStreamType extends InputStreamType {

    public static final String CLOB_ASCII_STREAM = "ClobAsciiStream";

    ClobAsciiStreamType() {
        super(CLOB_ASCII_STREAM);
    }

    /**
     * Retrieves a CLOB value as an ASCII InputStream from a ResultSet at the specified column index.
     *
     * @param rs the ResultSet containing the data
     * @param columnIndex the column index (1-based) of the CLOB value
     * @return An InputStream containing the ASCII representation of the CLOB, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Clob clob = rs.getClob(columnIndex);
        return clob2AsciiStream(clob);
    }

    /**
     * Retrieves a CLOB value as an ASCII InputStream from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet containing the data
     * @param columnLabel the label of the column containing the CLOB value
     * @return An InputStream containing the ASCII representation of the CLOB, or null if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnLabel) throws SQLException {
        return clob2AsciiStream(rs.getClob(columnLabel));
    }

    /**
     * Sets an ASCII InputStream as a parameter in a PreparedStatement.
     * The stream will be read and stored as CLOB data in ASCII format.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the ASCII InputStream to set. Can be null.
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setAsciiStream(columnIndex, x);
    }

    /**
     * Sets an ASCII InputStream as a named parameter in a CallableStatement.
     * The stream will be read and stored as CLOB data in ASCII format.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the ASCII InputStream to set. Can be null.
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setAsciiStream(parameterName, x);
    }

    /**
     * Sets an ASCII InputStream as a parameter in a PreparedStatement with a specified length.
     * The stream will be read up to the specified length and stored as CLOB data.
     *
     * @param stmt the PreparedStatement in which to set the parameter
     * @param columnIndex the parameter index (1-based) to set
     * @param x the ASCII InputStream to set. Can be null.
     * @param sqlTypeOrLength the length of the stream in bytes
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets an ASCII InputStream as a named parameter in a CallableStatement with a specified length.
     * The stream will be read up to the specified length and stored as CLOB data.
     *
     * @param stmt the CallableStatement in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the ASCII InputStream to set. Can be null.
     * @param sqlTypeOrLength the length of the stream in bytes
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the contents of an ASCII InputStream to an Appendable output.
     * If the Appendable is a Writer, data is streamed directly for efficiency.
     * Otherwise, the entire stream is read into a string first.
     *
     * @param appendable the Appendable to write to
     * @param x the ASCII InputStream to append. Can be null.
     * @throws IOException if an I/O error occurs during reading or writing
     */
    @Override
    public void appendTo(final Appendable appendable, final InputStream x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer) {
                IOUtil.write(IOUtil.newInputStreamReader(x), (Writer) appendable); // NOSONAR
            } else {
                appendable.append(IOUtil.readAllToString(x));
            }
        }
    }

    /**
     * Writes the contents of an ASCII InputStream to a CharacterWriter.
     * The stream is read in chunks using a buffer for efficiency.
     * If serialization config specifies string quotation, the content is wrapped in quotes.
     *
     * @param writer the CharacterWriter to write to
     * @param t the ASCII InputStream to write. Can be null.
     * @param config the serialization configuration for quotation settings. Can be null.
     * @throws IOException if an I/O error occurs during reading or writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final InputStream t, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (t == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }

            final Reader reader = IOUtil.newInputStreamReader(t); // NOSONAR
            final char[] buf = Objectory.createCharArrayBuffer();

            try {
                int count = 0;

                while (IOUtil.EOF != (count = IOUtil.read(reader, buf, 0, buf.length))) {
                    writer.writeCharacter(buf, 0, count);
                }
            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                Objectory.recycle(buf);
            }

            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }
        }
    }

    /**
     * Converts a CLOB to an ASCII InputStream.
     * This is a utility method used internally to extract ASCII streams from CLOB objects.
     *
     * @param clob the CLOB to convert. Can be null.
     * @return An ASCII InputStream from the CLOB, or null if the CLOB is null
     * @throws SQLException if a database access error occurs while accessing the CLOB
     */
    static InputStream clob2AsciiStream(final Clob clob) throws SQLException {
        if (clob != null) {
            return clob.getAsciiStream();
        }

        return null; // NOSONAR
    }
}