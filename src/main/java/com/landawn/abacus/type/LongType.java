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

import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Numbers;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class LongType extends AbstractLongType {

    public static final String LONG = Long.class.getSimpleName();

    LongType() {
        super(LONG);
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Long.class;
    }

    /**
     * Checks if is primitive wrapper.
     *
     * @return true, if is primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return {@code null} if {@code (ret == null)}. (auto-generated java doc for return)
     * @throws SQLException the SQL exception
     */
    @MayReturnNull
    @Override
    public Long get(ResultSet rs, int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Long) {
            return (Long) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).longValue();
        } else {
            return Numbers.toLong(ret.toString());
        }
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return {@code null} if {@code (ret == null)}. (auto-generated java doc for return)
     * @throws SQLException the SQL exception
     */
    @MayReturnNull
    @Override
    public Long get(ResultSet rs, String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Long) {
            return (Long) ret;
        } else if (ret instanceof Number) {
            return ((Number) ret).longValue();
        } else {
            return Numbers.toLong(ret.toString());
        }
    }
}
