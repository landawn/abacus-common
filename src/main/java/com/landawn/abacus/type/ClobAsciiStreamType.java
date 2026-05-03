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
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;

/**
 * Type handler for CLOB (Character Large Object) values accessed as ASCII {@link java.io.InputStream}s.
 * This class extends {@link InputStreamType} and overrides the JDBC accessors so that
 * CLOB columns are read via {@link java.sql.Clob#getAsciiStream()} and written via
 * {@link java.sql.PreparedStatement#setAsciiStream setAsciiStream}.
 *
 * <p>When writing CLOB data to a {@link java.io.Writer}-based {@link Appendable}, the stream is
 * decoded as US-ASCII. For other {@link Appendable} targets, the stream is fully buffered into a
 * string first.</p>
 *
 * @see InputStreamType
 * @see java.sql.Clob
 */
public class ClobAsciiStreamType extends InputStreamType {

    /**
     * The type name constant used for registration, equal to {@code "ClobAsciiStream"}.
     */
    public static final String CLOB_ASCII_STREAM = "ClobAsciiStream";

    /**
     * Package-private constructor for {@code ClobAsciiStreamType}.
     * Instances are created by the {@code TypeFactory}.
     */
    ClobAsciiStreamType() {
        super(CLOB_ASCII_STREAM);
    }

    /**
     * Retrieves a CLOB column as an ASCII {@link java.io.InputStream} from a {@link java.sql.ResultSet}
     * at the specified column index.
     *
     * @param rs          the {@link java.sql.ResultSet} to read from
     * @param columnIndex the 1-based column index
     * @return an ASCII {@link java.io.InputStream} for the CLOB value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Clob clob = rs.getClob(columnIndex);
        return clobToAsciiStream(clob);
    }

    /**
     * Retrieves a CLOB column as an ASCII {@link java.io.InputStream} from a {@link java.sql.ResultSet}
     * using the specified column label.
     *
     * @param rs         the {@link java.sql.ResultSet} to read from
     * @param columnName the label of the column to retrieve
     * @return an ASCII {@link java.io.InputStream} for the CLOB value,
     *         or {@code null} if the column value is SQL {@code NULL}
     * @throws SQLException if a database access error occurs or the column label is not found
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnName) throws SQLException {
        return clobToAsciiStream(rs.getClob(columnName));
    }

    /**
     * Sets an ASCII {@link java.io.InputStream} as a parameter in a {@link java.sql.PreparedStatement}.
     * The stream is bound via {@link java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream)}.
     *
     * @param stmt        the {@link java.sql.PreparedStatement} in which to set the parameter
     * @param columnIndex the 1-based parameter index
     * @param x           the ASCII {@link java.io.InputStream} to bind; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setAsciiStream(columnIndex, x);
    }

    /**
     * Sets an ASCII {@link java.io.InputStream} as a named parameter in a {@link java.sql.CallableStatement}.
     * The stream is bound via {@link java.sql.CallableStatement#setAsciiStream(String, java.io.InputStream)}.
     *
     * @param stmt          the {@link java.sql.CallableStatement} in which to set the parameter
     * @param parameterName the name of the parameter to set
     * @param x             the ASCII {@link java.io.InputStream} to bind; may be {@code null}
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setAsciiStream(parameterName, x);
    }

    /**
     * Sets an ASCII {@link java.io.InputStream} as a parameter in a {@link java.sql.PreparedStatement},
     * specifying the number of bytes to read.
     * The stream is bound via {@link java.sql.PreparedStatement#setAsciiStream(int, java.io.InputStream, int)}.
     *
     * @param stmt            the {@link java.sql.PreparedStatement} in which to set the parameter
     * @param columnIndex     the 1-based parameter index
     * @param x               the ASCII {@link java.io.InputStream} to bind; may be {@code null}
     * @param sqlTypeOrLength the number of bytes to read from the stream
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     * Sets an ASCII {@link java.io.InputStream} as a named parameter in a {@link java.sql.CallableStatement},
     * specifying the number of bytes to read.
     * The stream is bound via {@link java.sql.CallableStatement#setAsciiStream(String, java.io.InputStream, int)}.
     *
     * @param stmt            the {@link java.sql.CallableStatement} in which to set the parameter
     * @param parameterName   the name of the parameter to set
     * @param x               the ASCII {@link java.io.InputStream} to bind; may be {@code null}
     * @param sqlTypeOrLength the number of bytes to read from the stream
     * @throws SQLException if a database access error occurs or the parameter name is not found
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     * Appends the contents of an ASCII {@link java.io.InputStream} to an {@link Appendable}.
     * If {@code appendable} is a {@link java.io.Writer}, the stream is decoded with US-ASCII and
     * piped directly for efficiency. Otherwise, the entire stream is read into a {@link String} first.
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the ASCII {@link java.io.InputStream} whose content to append; may be {@code null}
     * @throws IOException if an I/O error occurs during reading or writing
     */
    @Override
    public void appendTo(final Appendable appendable, final InputStream x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer) {
                IOUtil.write(IOUtil.newInputStreamReader(x, Charsets.US_ASCII), (Writer) appendable); // NOSONAR
            } else {
                appendable.append(IOUtil.readAllToString(x));
            }
        }
    }

    /**
     * Writes the contents of an ASCII {@link java.io.InputStream} to a {@link CharacterWriter}.
     * The stream is decoded with US-ASCII and read in chunks using a pooled character buffer.
     * If {@code config} specifies a non-zero string quotation character, the entire content is
     * wrapped in that quote character. If {@code x} is {@code null}, the literal {@code null} is written.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the ASCII {@link java.io.InputStream} to write; may be {@code null}
     * @param config serialization configuration controlling string quotation; may be {@code null}
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
     * Extracts an ASCII {@link java.io.InputStream} from a {@link java.sql.Clob}.
     * This is a package-private utility used by both {@code get} overloads.
     *
     * @param clob the {@link java.sql.Clob} to read from; may be {@code null}
     * @return an ASCII {@link java.io.InputStream} for the CLOB's content,
     *         or {@code null} if {@code clob} is {@code null}
     * @throws SQLException if a database access error occurs while accessing the CLOB
     */
    static InputStream clobToAsciiStream(final Clob clob) throws SQLException {
        if (clob != null) {
            return clob.getAsciiStream();
        }

        return null; // NOSONAR
    }
}
