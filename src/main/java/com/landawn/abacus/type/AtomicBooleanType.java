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
import java.util.concurrent.atomic.AtomicBoolean;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Strings;

public class AtomicBooleanType extends AbstractAtomicType<AtomicBoolean> {

    public static final String ATOMIC_BOOLEAN = AtomicBoolean.class.getSimpleName();

    AtomicBooleanType() {
        super(ATOMIC_BOOLEAN);
    }

    @Override
    public Class<AtomicBoolean> clazz() {
        return AtomicBoolean.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final AtomicBoolean x) {
        return (x == null) ? null : (x.get() ? TRUE_STRING : FALSE_STRING);
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public AtomicBoolean valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new AtomicBoolean(Boolean.parseBoolean(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public AtomicBoolean get(final ResultSet rs, final int columnIndex) throws SQLException {
        return new AtomicBoolean(rs.getBoolean(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public AtomicBoolean get(final ResultSet rs, final String columnLabel) throws SQLException {
        return new AtomicBoolean(rs.getBoolean(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicBoolean x) throws SQLException {
        stmt.setBoolean(columnIndex, x != null && x.get());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicBoolean x) throws SQLException {
        stmt.setBoolean(parameterName, x != null && x.get());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final AtomicBoolean x) throws IOException {
        appendable.append((x == null) ? NULL_STRING : (x.get() ? TRUE_STRING : FALSE_STRING));
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final AtomicBoolean x, final JSONXMLSerializationConfig<?> config) throws IOException {
        appendTo(writer, x);
    }
}
