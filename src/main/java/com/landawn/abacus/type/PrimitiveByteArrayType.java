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
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

@SuppressWarnings("java:S2160")
public final class PrimitiveByteArrayType extends AbstractPrimitiveArrayType<byte[]> {

    public static final String BYTE_ARRAY = byte[].class.getSimpleName();

    private final Type<Byte> elementType;

    PrimitiveByteArrayType() {
        super(BYTE_ARRAY);

        elementType = TypeFactory.getType(byte.class);
    }

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
     * Checks if is a primitive byte array.
     *
     * @return {@code true}, if is primitive byte array
     */
    @Override
    public boolean isPrimitiveByteArray() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @MayReturnNull
    @Override
    public String stringOf(final byte[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        //    final StringBuilder sb = Objectory.createStringBuilder(calculateBufferSize(x.length, 6));
        //
        //    sb.append(WD._BRACKET_L);
        //
        //    for (int i = 0, len = x.length; i < len; i++) {
        //        if (i > 0) {
        //            sb.append(ELEMENT_SEPARATOR);
        //        }
        //
        //        sb.append(x[i]);
        //    }
        //
        //    sb.append(WD._BRACKET_R);
        //
        //    final String str = sb.toString();
        //
        //    Objectory.recycle(sb);
        //
        //    return str;

        return Strings.join(x, 0, x.length, ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public byte[] valueOf(final String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.isEmpty() || STR_FOR_EMPTY_ARRAY.equals(str)) {
            return N.EMPTY_BYTE_ARRAY;
        }

        final String[] strs = split(str);
        final int len = strs.length;
        final byte[] a = new byte[len];

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
     * @return
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
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    blob.free();
                } catch (final SQLException e) {
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
    public byte[] get(final ResultSet rs, final int columnIndex) throws SQLException {
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
    public byte[] get(final ResultSet rs, final String columnLabel) throws SQLException {
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
    public void set(final PreparedStatement stmt, final int columnIndex, final byte[] x) throws SQLException {
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
    public void set(final CallableStatement stmt, final String parameterName, final byte[] x) throws SQLException {
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
    public void set(final PreparedStatement stmt, final int columnIndex, final byte[] x, final int sqlTypeOrLength) throws SQLException {
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
    public void set(final CallableStatement stmt, final String parameterName, final byte[] x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(parameterName, x);
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final byte[] x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(WD._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                appendable.append(N.stringOf(x[i]));
            }

            appendable.append(WD._BRACKET_R);
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
    public void writeCharacter(final CharacterWriter writer, final byte[] x, final JSONXMLSerializationConfig<?> config) throws IOException {
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
     * @return
     */
    @MayReturnNull
    @Override
    public byte[] collection2Array(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        final byte[] a = new byte[c.size()];

        int i = 0;

        for (final Object e : c) {
            a[i++] = (Byte) e;
        }

        return a;
    }

    @Override
    public <E> void array2Collection(final byte[] x, final Collection<E> output) {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            for (final byte element : x) {
                c.add(element);
            }
        }
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public int hashCode(final byte[] x) {
        return N.hashCode(x);
    }

    /**
     *
     * @param x
     * @param y
     * @return {@code true}, if successful
     */
    @Override
    public boolean equals(final byte[] x, final byte[] y) {
        return N.equals(x, y);
    }
}
