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
import java.io.Writer;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteArrayType extends AbstractPrimitiveArrayType<byte[]> {

    public static final String BYTE_ARRAY = byte[].class.getSimpleName();

    private final Type<Byte> elementType;

    PrimitiveByteArrayType() {
        super(BYTE_ARRAY);

        elementType = TypeFactory.getType(byte.class);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<byte[]> clazz() {
        return byte[].class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<Byte> getElementType() {
        return elementType;
    }

    /**
     * Checks if is primitive byte array.
     *
     * @return true, if is primitive byte array
     */
    @Override
    public boolean isPrimitiveByteArray() {
        return true;
    }

    /**
     *
     * @param x
     * @return {@code null} if {@code (x == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public String stringOf(byte[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return "[]";
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        sb.append(WD._BRACKET_L);

        for (int i = 0, len = x.length; i < len; i++) {
            if (i > 0) {
                sb.append(ELEMENT_SEPARATOR);
            }

            sb.append(x[i]);
        }

        sb.append(WD._BRACKET_R);

        String str = sb.toString();

        Objectory.recycle(sb);

        return str;
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (str == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public byte[] valueOf(String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.length() == 0 || "[]".equals(str)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        String[] strs = split(str);
        int len = strs.length;
        byte[] a = new byte[len];

        if (len > 0) {
            for (int i = 0; i < len; i++) {
                a[i] = elementType.valueOf(strs[i]);
            }
        }

        return a;
    }

    /**
     *
     * @param obj
     * @return {@code null} if {@code (obj == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public byte[] valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Blob blob) {
            try {
                return blob.getBytes(1, (int) blob.length());
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    blob.free();
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e); //NOSONAR
                }
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
    public byte[] get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBytes(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public byte[] get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getBytes(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, byte[] x) throws SQLException {
        stmt.setBytes(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, byte[] x) throws SQLException {
        stmt.setBytes(parameterName, x);
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
    public void set(PreparedStatement stmt, int columnIndex, byte[] x, int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(columnIndex, x);
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
    public void set(CallableStatement stmt, String parameterName, byte[] x, int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(parameterName, x);
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, byte[] x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                IOUtil.write(writer, x[i]);
            }

            writer.write(WD._BRACKET_R);
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
    public void writeCharacter(CharacterWriter writer, byte[] x, JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR);
                }

                writer.write(x[i]);
            }

            writer.write(WD._BRACKET_R);
        }
    }

    /**
     * Collection 2 array.
     *
     * @param c
     * @return {@code null} if {@code (c == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public byte[] collection2Array(Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        byte[] a = new byte[c.size()];

        int i = 0;

        for (Object e : c) {
            a[i++] = (Byte) e;
        }

        return a;
    }

    /**
     * Array 2 collection.
     *
     * @param <E>
     * @param resultCollection
     * @param x
     * @return
     */
    @Override
    public <E> Collection<E> array2Collection(Collection<E> resultCollection, byte[] x) {
        if (N.isEmpty(x)) {
            return resultCollection;
        }

        Collection<Object> c = (Collection<Object>) resultCollection;

        for (byte element : x) {
            c.add(element);
        }

        return resultCollection;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(byte[] x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return true, if successful
     */
    @Override
    public boolean equals(byte[] x, byte[] y) {
        return N.equals(x, y);
    }
}
