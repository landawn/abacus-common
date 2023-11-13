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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 * The InputStream must be encoded by base64.
 *
 * @author Haiyang Li
 * @since 0.8
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

    InputStreamType(String typeName) {
        super(typeName);

        this.typeClass = InputStream.class;

        this.bytesConstructor = null;
        this.streamConstructor = null;
    }

    InputStreamType(Class<InputStream> cls) {
        super(ClassUtil.getSimpleClassName(cls));

        this.typeClass = cls;

        if (Modifier.isAbstract(cls.getModifiers())) {
            this.bytesConstructor = null;
            this.streamConstructor = null;
        } else {
            this.bytesConstructor = ClassUtil.getDeclaredConstructor(cls, byte[].class);
            this.streamConstructor = ClassUtil.getDeclaredConstructor(cls, InputStream.class);
        }
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<InputStream> clazz() {
        return typeClass;
    }

    /**
     * Checks if is input stream.
     *
     * @return true, if is input stream
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
    public String stringOf(InputStream x) {
        return x == null ? null : Strings.base64Encode(IOUtil.readAllBytes(x));
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public InputStream valueOf(String str) {
        if (str == null) {
            return null; // NOSONAR
        }

        if (bytesConstructor != null) {
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
    @SuppressFBWarnings
    @Override
    public InputStream valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Blob blob) {
            try {
                return blob.getBinaryStream();
            } catch (SQLException e) {
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
    public InputStream get(ResultSet rs, int columnIndex) throws SQLException {
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
    public InputStream get(ResultSet rs, String columnLabel) throws SQLException {
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
    public void set(PreparedStatement stmt, int columnIndex, InputStream x) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, InputStream x) throws SQLException {
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
    public void set(PreparedStatement stmt, int columnIndex, InputStream x, int sqlTypeOrLength) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, InputStream x, int sqlTypeOrLength) throws SQLException {
        stmt.setBinaryStream(parameterName, x, sqlTypeOrLength);
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
            writer.write(Strings.base64Encode(IOUtil.readAllBytes(t)));
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
