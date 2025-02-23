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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * The InputStream must be encoded by base64.
 *
 */
@SuppressWarnings("java:S2160")
public class InputStreamType extends AbstractType<InputStream> {

    public static final String INPUT_STREAM = InputStream.class.getSimpleName();

    private final Class<InputStream> typeClass;

    private final Constructor<?> bytesConstructor;

    private final Constructor<?> streamConstructor;

    InputStreamType() {
        this(INPUT_STREAM);
    }

    InputStreamType(final String typeName) {
        super(typeName);

        typeClass = InputStream.class;

        bytesConstructor = null;
        streamConstructor = null;
    }

    InputStreamType(final Class<InputStream> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        typeClass = cls;

        if (Modifier.isAbstract(cls.getModifiers())) {
            bytesConstructor = null;
            streamConstructor = null;
        } else {
            bytesConstructor = ClassUtil.getDeclaredConstructor(cls, byte[].class);
            streamConstructor = ClassUtil.getDeclaredConstructor(cls, InputStream.class);
        }
    }

    @Override
    public Class<InputStream> clazz() {
        return typeClass;
    }

    /**
     * Checks if is input stream.
     *
     * @return {@code true}, if is input stream
     */
    @Override
    public boolean isInputStream() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final InputStream x) {
        return x == null ? null : Strings.base64Encode(IOUtil.readAllBytes(x));
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public InputStream valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        }

        if (bytesConstructor != null) {
            //noinspection PrimitiveArrayArgumentToVarargsMethod
            return (InputStream) ClassUtil.invokeConstructor(bytesConstructor, Strings.base64Decode(str)); //NOSONAR
        } else if (streamConstructor != null) {
            return (InputStream) ClassUtil.invokeConstructor(streamConstructor, new ByteArrayInputStream(Strings.base64Decode(str)));
        } else {
            return new ByteArrayInputStream(Strings.base64Decode(str));
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
    public InputStream valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Blob blob) {
            try {
                return blob.getBinaryStream();
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
    public InputStream get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getBinaryStream(columnIndex);
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
        return rs.getBinaryStream(columnLabel);
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
        stmt.setBinaryStream(columnIndex, x);
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
        stmt.setBinaryStream(parameterName, x);
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
        stmt.setBinaryStream(columnIndex, x, sqlTypeOrLength);
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
        stmt.setBinaryStream(parameterName, x, sqlTypeOrLength);
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
            if (appendable instanceof Writer writer) {
                IOUtil.write(new InputStreamReader(x), writer);
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
            if ((config == null) || (config.getStringQuotation() == 0)) {
                writer.write(Strings.base64Encode(IOUtil.readAllBytes(t)));
            } else {
                writer.write(config.getStringQuotation());
                writer.write(Strings.base64Encode(IOUtil.readAllBytes(t)));
                writer.write(config.getStringQuotation());
            }
        }
    }
}
