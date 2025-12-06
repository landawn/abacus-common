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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;

/**
 * Type handler for ASCII InputStream operations.
 * This class specializes in handling ASCII character streams from databases and
 * provides conversion between InputStream and various output formats specifically
 * for ASCII-encoded data.
 */
public class AsciiStreamType extends InputStreamType {

    /**
     * The type name constant for ASCII stream type identification.
     */
    public static final String ASCII_STREAM = "AsciiStream";

    /**
     * Package-private constructor for AsciiStreamType.
     * This constructor is called by the TypeFactory to create AsciiStream type instances.
     */
    AsciiStreamType() {
        super(ASCII_STREAM);
    }

    /**
     * Retrieves an ASCII InputStream from a ResultSet at the specified column index.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<InputStream> type = TypeFactory.getType("AsciiStream");
     * ResultSet rs = ...;  // from SQL query
     * InputStream stream = type.get(rs, 1);
     * // Read ASCII text data from database column
     * String content = IOUtil.readAllToString(stream);
     * }</pre>
     *
     * @param rs the ResultSet to retrieve the ASCII stream from
     * @param columnIndex the column index (1-based) of the ASCII stream
     * @return an InputStream containing the ASCII data, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getAsciiStream(columnIndex);
    }

    /**
     * Retrieves an ASCII InputStream from a ResultSet using the specified column label.
     *
     * @param rs the ResultSet to retrieve the ASCII stream from
     * @param columnLabel the label for the column specified with the SQL AS clause,
     *                    or the column name if no AS clause was specified
     * @return an InputStream containing the ASCII data, or {@code null} if the value is SQL NULL
     * @throws SQLException if a database access error occurs or the columnLabel is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getAsciiStream(columnLabel);
    }

    /**
     * Sets an ASCII InputStream parameter in a PreparedStatement at the specified position.
     * The JDBC driver will read the stream as needed until end-of-file is reached.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the InputStream containing ASCII data, may be null
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setAsciiStream(columnIndex, x);
    }

    /**
     * Sets a named ASCII InputStream parameter in a CallableStatement.
     * The JDBC driver will read the stream as needed until end-of-file is reached.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the InputStream containing ASCII data, may be null
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setAsciiStream(parameterName, x);
    }

    /**
     * Sets an ASCII InputStream parameter in a PreparedStatement with a specified length.
     * This method allows specification of the stream length for optimization.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based) to set
     * @param x the InputStream containing ASCII data, may be null
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or the columnIndex is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a named ASCII InputStream parameter in a CallableStatement with a specified length.
     * This method allows specification of the stream length for optimization.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter to set
     * @param x the InputStream containing ASCII data, may be null
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the content of an ASCII InputStream to an Appendable object.
     * If the InputStream is {@code null}, appends the string "null".
     * For Writer instances, performs direct stream-to-writer copying for efficiency.
     * For other Appendable types, reads the entire stream into a string first.
     *
     * @param appendable the Appendable object to append to
     * @param x the InputStream containing ASCII data to append, may be null
     * @throws IOException if an I/O error occurs during the read or append operation
     */
    @Override
    public void appendTo(final Appendable appendable, final InputStream x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer) {
                IOUtil.write(IOUtil.newInputStreamReader(x), (Writer) appendable);   // NOSONAR
            } else {
                appendable.append(IOUtil.readAllToString(x));
            }
        }
    }

    /**
     * Writes the content of an ASCII InputStream to a CharacterWriter with optional quotation.
     * This method handles {@code null} values and applies string quotation marks if specified in the configuration.
     * The stream is read in chunks using a buffer from the object pool for efficiency.
     *
     * @param writer the CharacterWriter to write to
     * @param t the InputStream containing ASCII data to write, may be null
     * @param config the serialization configuration that may specify string quotation preferences
     * @throws IOException if an I/O error occurs during the read or write operation
     * @throws UncheckedIOException wraps any IOException that occurs during stream reading
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final InputStream t, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (t == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }

            final Reader reader = IOUtil.newInputStreamReader(t);   // NOSONAR
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
}
