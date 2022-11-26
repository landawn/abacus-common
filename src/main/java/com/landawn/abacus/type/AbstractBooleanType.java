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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractBooleanType extends AbstractPrimaryType<Boolean> {

    protected AbstractBooleanType(String typeName) {
        super(typeName);
    }

    /**
     * Checks if is boolean.
     *
     * @return true, if is boolean
     */
    @Override
    public boolean isBoolean() {
        return true;
    }

    /**
     *
     * @param b
     * @return
     */
    @Override
    public String stringOf(Boolean b) {
        return (b == null) ? null : b.toString();
    }

    /**
     *
     * @param st
     * @return
     */
    @Override
    public Boolean valueOf(String st) {
        return N.isNullOrEmpty(st) ? defaultValue() : Boolean.valueOf(st);
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public Boolean valueOf(char[] cbuf, int offset, int len) {
        return ((cbuf == null) || (len == 0)) ? defaultValue()
                : (((len == 4) && (((cbuf[offset] == 't') || (cbuf[offset] == 'T')) && ((cbuf[offset + 1] == 'r') || (cbuf[offset + 1] == 'R'))
                        && ((cbuf[offset + 2] == 'u') || (cbuf[offset + 2] == 'U')) && ((cbuf[offset + 3] == 'e') || (cbuf[offset + 3] == 'E')))) ? Boolean.TRUE
                                : Boolean.FALSE);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Boolean get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBoolean(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Boolean get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getBoolean(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(columnIndex, x);
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
    public void set(CallableStatement stmt, String parameterName, Boolean x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, java.sql.Types.BOOLEAN);
        } else {
            stmt.setBoolean(parameterName, x);
        }
    }

    /**
     *
     * @param writer
     * @param t
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, Boolean t) throws IOException {
        writer.write((t == null) ? NULL_CHAR_ARRAY : (t ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }

    /**
     *
     * @param writer
     * @param t
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(CharacterWriter writer, Boolean x, SerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullBooleanAsFalse() ? Boolean.FALSE : x;

        writer.write((x == null) ? NULL_CHAR_ARRAY : (x ? TRUE_CHAR_ARRAY : FALSE_CHAR_ARRAY));
    }
}
