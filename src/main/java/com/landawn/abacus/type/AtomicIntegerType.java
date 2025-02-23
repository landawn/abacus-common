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
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class AtomicIntegerType extends AbstractAtomicType<AtomicInteger> {

    public static final String ATOMIC_INTEGER = AtomicInteger.class.getSimpleName();

    AtomicIntegerType() {
        super(ATOMIC_INTEGER);
    }

    @Override
    public Class<AtomicInteger> clazz() {
        return AtomicInteger.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final AtomicInteger x) {
        return (x == null) ? null : N.stringOf(x.get());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public AtomicInteger valueOf(final String str) {
        return Strings.isEmpty(str) ? null : new AtomicInteger(Integer.parseInt(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public AtomicInteger get(final ResultSet rs, final int columnIndex) throws SQLException {
        return new AtomicInteger(rs.getInt(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public AtomicInteger get(final ResultSet rs, final String columnLabel) throws SQLException {
        return new AtomicInteger(rs.getInt(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final AtomicInteger x) throws SQLException {
        stmt.setInt(columnIndex, (x == null) ? 0 : x.get());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final AtomicInteger x) throws SQLException {
        stmt.setInt(parameterName, (x == null) ? 0 : x.get());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final AtomicInteger x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x.toString());
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
    public void writeCharacter(final CharacterWriter writer, final AtomicInteger x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.writeInt(x.get());
        }
    }
}
