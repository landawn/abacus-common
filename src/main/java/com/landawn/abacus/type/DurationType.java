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
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class DurationType extends AbstractType<Duration> {

    public static final String DURATION = Duration.class.getSimpleName();

    DurationType() {
        super(DURATION);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<Duration> clazz() {
        return Duration.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final Duration x) {
        return (x == null) ? null : String.valueOf(x.toMillis());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Duration valueOf(final String str) {
        return Strings.isEmpty(str) ? null : Duration.ofMillis(Numbers.toLong(str));
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Duration get(final ResultSet rs, final int columnIndex) throws SQLException {
        return Duration.ofMillis(rs.getLong(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Duration get(final ResultSet rs, final String columnLabel) throws SQLException {
        return Duration.ofMillis(rs.getLong(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final Duration x) throws SQLException {
        stmt.setLong(columnIndex, (x == null) ? 0 : x.toMillis());
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final Duration x) throws SQLException {
        stmt.setLong(parameterName, (x == null) ? 0 : x.toMillis());
    }

    /**
     *
     * @param appendable
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(final Appendable appendable, final Duration x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(String.valueOf(x.toMillis()));
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
    public void writeCharacter(final CharacterWriter writer, final Duration x, final JSONXMLSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(x.toMillis());
        }
    }
}
