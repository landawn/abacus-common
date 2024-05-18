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
import java.sql.Clob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractStringType extends AbstractCharSequenceType<String> {

    protected AbstractStringType(String typeName) {
        super(typeName);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<String> clazz() {
        return String.class;
    }

    /**
     * Checks if is string.
     *
     * @return true, if is string
     */
    @Override
    public boolean isString() {
        return true;
    }

    /**
     * Checks if is immutable.
     *
     * @return true, if is immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public String stringOf(String str) {
        return str;
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public String valueOf(String str) {
        return str;
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public String valueOf(char[] cbuf, int offset, int len) {
        return cbuf == null ? null : ((cbuf.length == 0 || len == 0) ? Strings.EMPTY_STRING : String.valueOf(cbuf, offset, len));
    }

    /**
     *
     * @param obj
     * @return {@code null} if {@code (obj == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @SuppressFBWarnings
    @Override
    public String valueOf(final Object obj) {
        if (obj == null) {
            return null; // NOSONAR
        } else if (obj instanceof Clob clob) {
            try {
                return clob.getSubString(1, (int) clob.length());
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            } finally {
                try {
                    clob.free();
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
    public String get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public String get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getString(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, String x) throws SQLException {
        stmt.setString(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, String x) throws SQLException {
        stmt.setString(parameterName, x);
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void appendTo(Appendable appendable, String x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            appendable.append(x);
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
    public void writeCharacter(CharacterWriter writer, String x, JSONXMLSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullStringAsEmpty() ? Strings.EMPTY_STRING : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            final char ch = config == null ? 0 : config.getStringQuotation();

            if (ch == 0) {
                writer.writeCharacter(x);
            } else {
                writer.write(ch);
                writer.writeCharacter(x);
                writer.write(ch);
            }
        }
    }
}
