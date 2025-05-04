/*
 * Copyright (C) 2019 HaiYang Li
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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@SuppressWarnings("java:S2160")
public final class BooleanCharType extends AbstractType<Boolean> {

    private static final String typeName = "BooleanChar";
    private static final String Y = "Y";
    private static final String N = "N";

    BooleanCharType() {
        super(typeName);
    }

    @Override
    public Class<Boolean> clazz() {
        return Boolean.class;
    }

    @Override
    public Boolean defaultValue() {
        return Boolean.FALSE;
    }

    /**
     * Checks if is non quoted csv type.
     *
     * @return {@code true}, if is non quoted csv type
     */
    @Override
    public boolean isNonQuotableCsvType() {
        return true;
    }

    /**
     *
     * @param b
     * @return
     */
    @Override
    public String stringOf(final Boolean b) {
        return (b == null || !b) ? N : Y;
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Boolean valueOf(final String str) {
        return Y.equalsIgnoreCase(str) ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public Boolean valueOf(final char[] cbuf, final int offset, final int len) {
        return (cbuf == null || len == 0) ? Boolean.FALSE : ((len == 1 && (cbuf[offset] == 'Y' || cbuf[offset] == 'y')) ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Boolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Boolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        return valueOf(rs.getString(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setString(columnIndex, x ? Y : N);
        }
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setString(parameterName, x ? Y : N);
        }
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Boolean x) throws IOException {
        appendable.append(stringOf(x));
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Boolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        final char ch = config == null ? 0 : config.getCharQuotation();

        if (ch == 0) {
            writer.write(stringOf(x));
        } else {
            writer.write(ch);
            writer.write(stringOf(x));
            writer.write(ch);
        }
    }
}
