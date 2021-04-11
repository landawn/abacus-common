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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Objectory;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class AsciiStreamType extends InputStreamType {

    public static final String ASCII_STREAM = "AsciiStream";

    AsciiStreamType() {
        super(ASCII_STREAM);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getAsciiStream(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public InputStream get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getAsciiStream(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, InputStream x) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, InputStream x) throws SQLException {
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
    public void set(PreparedStatement stmt, int columnIndex, InputStream x, int sqlTypeOrLength) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, InputStream x, int sqlTypeOrLength) throws SQLException {
        stmt.setAsciiStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     *
     * @param writer
     * @param t
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, InputStream t) throws IOException {
        if (t == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(writer, new InputStreamReader(t));
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
    public void writeCharacter(CharacterWriter writer, InputStream t, SerializationConfig<?> config) throws IOException {
        if (t == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }

            final Reader reader = new InputStreamReader(t);
            final char[] buf = Objectory.createCharArrayBuffer();

            try {
                int count = 0;

                while (IOUtil.EOF != (count = IOUtil.read(reader, buf, 0, buf.length))) {
                    writer.writeCharacter(buf, 0, count);
                }
            } catch (IOException e) {
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
