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

import java.net.URI;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.Strings;

/**
 *
 */
public class URIType extends AbstractType<URI> {

    public static final String URI = URI.class.getSimpleName();

    URIType() {
        super(URI);
    }

    /**
     *
     *
     * @return
     */
    @Override
    public Class<URI> clazz() {
        return URI.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final URI x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public URI valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        return java.net.URI.create(str); // NOSONAR
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public URI get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public URI get(final ResultSet rs, final String columnLabel) throws SQLException {
        return valueOf(rs.getString(columnLabel));
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final URI x) throws SQLException {
        stmt.setString(columnIndex, stringOf(x));
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final URI x) throws SQLException {
        stmt.setString(parameterName, stringOf(x));
    }
}
