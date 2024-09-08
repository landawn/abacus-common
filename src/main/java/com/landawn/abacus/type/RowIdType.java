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
import java.sql.RowId;
import java.sql.SQLException;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class RowIdType extends AbstractType<RowId> {

    public static final String ROW_ID = RowId.class.getSimpleName();

    RowIdType() {
        super(ROW_ID);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<RowId> clazz() {
        return RowId.class;
    }

    /**
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final RowId x) {
        return x == null ? null : x.toString();
    }

    /**
     *
     *
     * @param str
     * @return
     * @throws UnsupportedOperationException
     */
    @Override
    public RowId valueOf(final String str) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public RowId get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getRowId(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public RowId get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getRowId(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final RowId x) throws SQLException {
        stmt.setRowId(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final RowId x) throws SQLException {
        stmt.setRowId(parameterName, x);
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final RowId x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(stringOf(x));
        }
    }
}
