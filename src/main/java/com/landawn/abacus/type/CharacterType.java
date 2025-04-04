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

public final class CharacterType extends AbstractCharacterType {

    public static final String CHARACTER = Character.class.getSimpleName();

    CharacterType() {
        super(CHARACTER);
    }

    @Override
    public Class<Character> clazz() {
        return Character.class;
    }

    /**
     * Checks if is primitive wrapper.
     *
     * @return {@code true}, if is primitive wrapper
     */
    @Override
    public boolean isPrimitiveWrapper() {
        return true;
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
    public Character get(final ResultSet rs, final int columnIndex) throws SQLException {
        final String ret = rs.getString(columnIndex);

        if (ret == null) {
            return null; // NOSONAR
        } else {
            return ret.charAt(0);
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
    public Character get(final ResultSet rs, final String columnLabel) throws SQLException {
        final String ret = rs.getString(columnLabel);

        if (ret == null) {
            return null; // NOSONAR
        } else {
            return ret.charAt(0);
        }
    }
}
