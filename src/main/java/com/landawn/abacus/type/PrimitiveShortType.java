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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Numbers;

@SuppressWarnings("java:S2160")
public final class PrimitiveShortType extends AbstractShortType {

    public static final String SHORT = short.class.getSimpleName();

    private static final Short DEFAULT_VALUE = 0;

    PrimitiveShortType() {
        super(SHORT);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return short.class;
    }

    /**
     * Checks if is primitive type.
     *
     * @return {@code true}, if is primitive type
     */
    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    @Override
    public Short defaultValue() {
        return DEFAULT_VALUE;
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @MayReturnNull
    @Override
    public Short get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Short) {
            return (Short) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).shortValue();
        } else {
            return Numbers.toShort(ret.toString());
        }
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @MayReturnNull
    @Override
    public Short get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Short) {
            return (Short) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).shortValue();
        } else {
            return Numbers.toShort(ret.toString());
        }
    }
}
