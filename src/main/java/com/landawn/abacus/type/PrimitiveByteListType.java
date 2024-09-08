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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public final class PrimitiveByteListType extends AbstractPrimitiveListType<ByteList> {

    public static final String BYTE_LIST = ByteList.class.getSimpleName();

    private final Type<byte[]> arrayType = N.typeOf(byte[].class);

    private final Type<?> elementType = N.typeOf(byte.class);

    protected PrimitiveByteListType() {
        super(BYTE_LIST);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<ByteList> clazz() {
        return ByteList.class;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<?> getElementType() {
        return elementType;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final ByteList x) {
        return x == null ? null : arrayType.stringOf(x.trimToSize().array());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public ByteList valueOf(final String str) {
        return Strings.isEmpty(str) ? null : ByteList.of(arrayType.valueOf(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public ByteList get(final ResultSet rs, final int columnIndex) throws SQLException {
        final byte[] bytes = rs.getBytes(columnIndex);
        return bytes == null ? null : ByteList.of(bytes);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public ByteList get(final ResultSet rs, final String columnLabel) throws SQLException {
        final byte[] bytes = rs.getBytes(columnLabel);
        return bytes == null ? null : ByteList.of(bytes);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final ByteList x) throws SQLException {
        stmt.setBytes(columnIndex, x == null ? null : x.trimToSize().array());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final ByteList x) throws SQLException {
        stmt.setBytes(parameterName, x == null ? null : x.trimToSize().array());
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
    public void set(final PreparedStatement stmt, final int columnIndex, final ByteList x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(columnIndex, x == null ? null : x.trimToSize().array());
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
    public void set(final CallableStatement stmt, final String parameterName, final ByteList x, final int sqlTypeOrLength) throws SQLException {
        stmt.setBytes(parameterName, x == null ? null : x.trimToSize().array());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final ByteList x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            arrayType.appendTo(appendable, x.trimToSize().array());
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
    public void writeCharacter(final CharacterWriter writer, final ByteList x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            arrayType.writeCharacter(writer, x.trimToSize().array(), config);
        }
    }
}
