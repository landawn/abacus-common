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
import java.math.BigDecimal;
import java.math.MathContext;
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
public final class BigDecimalType extends NumberType<BigDecimal> {

    public static final String BIG_DECIMAL = BigDecimal.class.getSimpleName();

    BigDecimalType() {
        super(BIG_DECIMAL);
    }

    @Override
    public Class<BigDecimal> clazz() {
        return BigDecimal.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(BigDecimal x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public BigDecimal valueOf(String str) {
        return N.isNullOrEmpty(str) ? null : new BigDecimal(str, MathContext.UNLIMITED);
    }

    @Override
    public BigDecimal valueOf(char[] cbuf, int offset, int len) {
        return len == 0 ? null : new BigDecimal(cbuf, offset, len, MathContext.UNLIMITED);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public BigDecimal get(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getBigDecimal(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public BigDecimal get(ResultSet rs, String columnLabel) throws SQLException {
        return rs.getBigDecimal(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(PreparedStatement stmt, int columnIndex, BigDecimal x) throws SQLException {
        stmt.setBigDecimal(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(CallableStatement stmt, String parameterName, BigDecimal x) throws SQLException {
        stmt.setBigDecimal(parameterName, x);
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(CharacterWriter writer, BigDecimal x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            if (config != null && config.writeBigDecimalAsPlain()) {
                writer.writeCharacter(x.toPlainString());
            } else {
                writer.writeCharacter(stringOf(x));
            }
        }
    }
}
