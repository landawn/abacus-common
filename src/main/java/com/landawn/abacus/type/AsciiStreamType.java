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

import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;

/**
 * Type handler for ASCII {@link java.io.InputStream} values mapped to database ASCII stream columns.
 * This class specializes in reading and writing ASCII character streams from databases and
 * provides conversion between {@link java.io.InputStream} and various output formats using
 * US-ASCII character encoding ({@link com.landawn.abacus.util.Charsets#US_ASCII}).
 *
 * <p>JDBC operations use the dedicated ASCII-stream APIs:
 * retrieval via {@link java.sql.ResultSet#getAsciiStream} and storage via
 * {@link java.sql.PreparedStatement#setAsciiStream}. No explicit {@link java.sql.Types} code is bound;
 * the driver chooses the column mapping (typically {@code LONGVARCHAR}/{@code CLOB}).</p>
 *
 * @see InputStreamType
 * @see java.sql.ResultSet#getAsciiStream(int)
 * @see java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream)
 */
public class AsciiStreamType extends InputStreamType {

    /**
     * The type name constant used to identify the ASCII stream type within the type system.
     */
    public static final String ASCII_STREAM = "AsciiStream";

    /**
     * Package-private constructor for {@code AsciiStreamType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     */
    AsciiStreamType() {
        super(ASCII_STREAM);
    }

    /**
     * Retrieves an ASCII {@link java.io.InputStream} from a {@link java.sql.ResultSet} at the specified column index.
     * Delegates to {@link java.sql.ResultSet#getAsciiStream(int)}.
     *
     * @param rs the {@code ResultSet} to retrieve the ASCII stream from
     * @param columnIndex the 1-based column index of the ASCII stream column
     * @return an {@code InputStream} containing the ASCII-encoded data, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getAsciiStream(columnIndex);
    }

    /**
     * Retrieves an ASCII {@link java.io.InputStream} from a {@link java.sql.ResultSet} using the specified column label.
     * Delegates to {@link java.sql.ResultSet#getAsciiStream(String)}.
     *
     * @param rs the {@code ResultSet} to retrieve the ASCII stream from
     * @param columnName the column label as specified in the SQL AS clause, or the column name if no AS clause was used
     * @return an {@code InputStream} containing the ASCII-encoded data, or {@code null} if the column value is SQL NULL
     * @throws SQLException if a database access error occurs or {@code columnName} is not found
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getAsciiStream(columnName);
    }

    /**
     * Sets an ASCII {@link java.io.InputStream} parameter on a {@link java.sql.PreparedStatement} at the given position.
     * The JDBC driver reads data from the stream as needed until end-of-file is reached.
     * Delegates to {@link java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream)}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code InputStream} containing ASCII-encoded data; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setAsciiStream(columnIndex, x);
    }

    /**
     * Sets a named ASCII {@link java.io.InputStream} parameter on a {@link java.sql.CallableStatement}.
     * The JDBC driver reads data from the stream as needed until end-of-file is reached.
     * Delegates to {@link java.sql.CallableStatement#setAsciiStream(String, java.io.InputStream)}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code InputStream} containing ASCII-encoded data; may be {@code null}
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setAsciiStream(parameterName, x);
    }

    /**
     * Sets an ASCII {@link java.io.InputStream} parameter on a {@link java.sql.PreparedStatement} at the given position,
     * with an explicit byte-length hint that the JDBC driver may use for optimization.
     * Delegates to {@link java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, int)}.
     *
     * @param stmt the {@code PreparedStatement} on which to set the parameter
     * @param columnIndex the 1-based parameter index to set
     * @param x the {@code InputStream} containing ASCII-encoded data; may be {@code null}
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or {@code columnIndex} is out of range
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets a named ASCII {@link java.io.InputStream} parameter on a {@link java.sql.CallableStatement},
     * with an explicit byte-length hint that the JDBC driver may use for optimization.
     * Delegates to {@link java.sql.CallableStatement#setAsciiStream(String, java.io.InputStream, int)}.
     *
     * @param stmt the {@code CallableStatement} on which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x the {@code InputStream} containing ASCII-encoded data; may be {@code null}
     * @param sqlTypeOrLength the number of bytes in the stream
     * @throws SQLException if a database access error occurs or {@code parameterName} is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the content of an ASCII {@link java.io.InputStream} to an {@link Appendable}.
     * If {@code x} is {@code null}, the literal string {@code "null"} is appended.
     * When {@code appendable} is a {@link java.io.Writer}, the stream is copied directly for efficiency.
     * Otherwise the entire stream is read into a {@code String} first, then appended.
     *
     * @param appendable the target {@code Appendable} to append to
     * @param x the {@code InputStream} containing ASCII-encoded data to append; may be {@code null}
     * @throws IOException if an I/O error occurs during reading or appending
     */
    @Override
    public void appendTo(final Appendable appendable, final InputStream x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer) {
                IOUtil.write(IOUtil.newInputStreamReader(x, Charsets.US_ASCII), (Writer) appendable); // NOSONAR
            } else {
                appendable.append(IOUtil.readAllToString(x, Charsets.US_ASCII));
            }
        }
    }

    /**
     * Writes the content of an ASCII {@link java.io.InputStream} to a {@link CharacterWriter}, optionally
     * surrounding the value with the string-quotation character specified in {@code config}.
     * If {@code x} is {@code null}, the literal {@code "null"} character array is written.
     * The stream is read in chunks using a pooled character buffer for efficiency.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code InputStream} containing ASCII-encoded data to write; may be {@code null}
     * @param config the serialization/formatting configuration; if non-{@code null} and its
     *               {@link com.landawn.abacus.parser.JsonXmlSerConfig#getStringQuotation()} is non-zero,
     *               the value is wrapped in that quotation character
     * @throws IOException if an I/O error occurs during reading or writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final InputStream x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }

            final Reader reader = IOUtil.newInputStreamReader(x, Charsets.US_ASCII); // NOSONAR
            final char[] buf = Objectory.createCharArrayBuffer();

            try {
                int count = 0;

                while (IOUtil.EOF != (count = IOUtil.read(reader, buf, 0, buf.length))) {
                    writer.writeCharacter(buf, 0, count);
                }
            } finally {
                Objectory.recycle(buf);
            }

            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }
        }
    }
}
