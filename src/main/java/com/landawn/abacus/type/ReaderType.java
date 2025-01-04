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
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;

@SuppressWarnings("java:S2160")
public class ReaderType extends AbstractType<Reader> {

    public static final String READER = Reader.class.getSimpleName();

    private final Class<Reader> typeClass;

    private final Constructor<?> stringConstructor;

    private final Constructor<?> readerConstructor;

    ReaderType() {
        this(READER);
    }

    ReaderType(final String typeName) {
        super(typeName);

        typeClass = Reader.class;

        stringConstructor = null;
        readerConstructor = null;
    }

    ReaderType(final Class<Reader> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;

        if (Modifier.isAbstract(cls.getModifiers())) {
            stringConstructor = null;
            readerConstructor = null;
        } else {
            stringConstructor = ClassUtil.getDeclaredConstructor(cls, String.class);
            readerConstructor = ClassUtil.getDeclaredConstructor(cls, Reader.class);
        }
    }

    @Override
    public Class<Reader> clazz() {
        return typeClass;
    }

    /**
     * Checks if is reader.
     *
     * @return {@code true}, if is reader
     */
    @Override
    public boolean isReader() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final Reader x) {
        return x == null ? null : IOUtil.readAllToString(x);
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public Reader valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        }

        if (stringConstructor != null) {
            return (Reader) ClassUtil.invokeConstructor(stringConstructor, str);
        } else if (readerConstructor != null) {
            return (Reader) ClassUtil.invokeConstructor(readerConstructor, new StringReader(str));
        } else {
            return new StringReader(str);
        }
    }

    /**
     *
     * @param obj
     * @return
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public Reader valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Clob clob) {
            try {
                return clob.getCharacterStream();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        } else {
            return valueOf(N.typeOf(obj.getClass()).stringOf(obj));
        }
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getCharacterStream(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Reader get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getCharacterStream(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x) throws SQLException {
        stmt.setCharacterStream(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Reader x) throws SQLException {
        stmt.setCharacterStream(parameterName, x);
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
    public void set(final PreparedStatement stmt, final int columnIndex, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setCharacterStream(columnIndex, x, sqlTypeOrLength);
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
    public void set(final CallableStatement stmt, final String parameterName, final Reader x, final int sqlTypeOrLength) throws SQLException {
        stmt.setCharacterStream(parameterName, x, sqlTypeOrLength);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Reader x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                IOUtil.write(x, writer);
            } else {
                appendable.append(IOUtil.readAllToString(x));
            }
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Reader x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if ((config != null) && (config.getStringQuotation() != 0)) {
                writer.write(config.getStringQuotation());
            }

            final char[] buf = Objectory.createCharArrayBuffer();

            try {
                int count = 0;

                while (IOUtil.EOF != (count = IOUtil.read(x, buf, 0, buf.length))) {
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
