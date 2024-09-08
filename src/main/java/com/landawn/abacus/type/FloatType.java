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
public final class FloatType extends AbstractFloatType {

    public static final String FLOAT = Float.class.getSimpleName();

    FloatType() {
        super(FLOAT);
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class clazz() {
        return Float.class;
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
    public Float get(final ResultSet rs, final int columnIndex) throws SQLException {
        final Object ret = rs.getObject(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Float) {
            return (Float) ret;
        } else {
            return Numbers.toFloat(ret);
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
    public Float get(final ResultSet rs, final String columnLabel) throws SQLException {
        final Object ret = rs.getObject(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else if (ret instanceof Float) {
            return (Float) ret;
        } else {
            return Numbers.toFloat(ret);
        }
    }
}
