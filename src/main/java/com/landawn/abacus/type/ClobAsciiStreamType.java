/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class ClobAsciiStreamType extends InputStreamType {

    public static final String CLOB_ASCII_STREAM = "ClobAsciiStream";

    ClobAsciiStreamType() {
        super(CLOB_ASCII_STREAM);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Clob clob = rs.getClob(columnIndex);
        return clob2AsciiStream(clob);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream get(final ResultSet rs, final String columnLabel) throws SQLException {
        return clob2AsciiStream(rs.getClob(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x) throws SQLException {
        stmt.setAsciiStream(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x) throws SQLException {
        stmt.setAsciiStream(parameterName, x);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(columnIndex, x, sqlTypeOrLength);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @param sqlTypeOrLength
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final InputStream x, final int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
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
     *
     * @param writer
     * @param t
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
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
     * Clob 2 ascii stream.
     *
     * @param clob
     * @return
     * @throws SQLException the SQL exception
     */
    static InputStream clob2AsciiStream(final Clob clob) throws SQLException {
        if (clob != null) {
            return clob.getAsciiStream();
        }

        return null; // NOSONAR
    }
}
