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
import java.sql.Types;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.Strings;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractLongType extends NumberType<Number> {

    protected AbstractLongType(String typeName) {
        super(typeName);
    }

    /**
     *
     * @param x
     * @return {@code null} if {@code (x == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public String stringOf(Number x) {
        if (x == null) {
            return null; // NOSONAR
        }

        return N.stringOf(x.longValue());
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Long valueOf(String str) {
        if (Strings.isEmpty(str)) {
            return (Long) defaultValue();
        }

        try {
            return Numbers.toLong(str);
        } catch (NumberFormatException e) {
            if (str.length() > 1) {
                char ch = str.charAt(str.length() - 1);

                if ((ch == 'l') || (ch == 'L') || (ch == 'f') || (ch == 'F') || (ch == 'd') || (ch == 'D')) {
                    return Numbers.toLong(str.substring(0, str.length() - 1));
                }
            }

            throw e;
        }
    }

    /**
     *
     * @param cbuf
     * @param offset
     * @param len
     * @return
     */
    @Override
    public Long valueOf(char[] cbuf, int offset, int len) {
        return ((cbuf == null) || (len == 0)) ? ((Long) defaultValue()) : (Long) parseLong(cbuf, offset, len);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Long get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getLong(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Long get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getLong(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(columnIndex, Types.BIGINT);
        } else {
            stmt.setLong(columnIndex, x.longValue());
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
    public void set(CallableStatement stmt, String parameterName, Number x) throws SQLException {
        if (x == null) {
            stmt.setNull(parameterName, Types.BIGINT);
        } else {
            stmt.setLong(parameterName, x.longValue());
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, Number x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            IOUtil.write(writer, x.longValue());
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
    public void writeCharacter(CharacterWriter writer, Number x, JSONXMLSerializationConfig<?> config) throws IOException {
        x = x == null && config != null && config.writeNullNumberAsZero() ? Numbers.LONG_ZERO : x;

        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (config != null && config.writeLongAsString() && config.getStringQuotation() != 0) {
                final char ch = config.getStringQuotation();

                writer.write(ch);
                writer.write(x.longValue());
                writer.write(ch);
            } else {
                writer.write(x.longValue());
            }
        }
    }
}
