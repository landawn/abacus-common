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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.Strings;

public class URLType extends AbstractType<URL> {

    public static final String URL = URL.class.getSimpleName();

    URLType() {
        super(URL);
    }

    @Override
    public Class<URL> clazz() {
        return URL.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final URL x) {
        return (x == null) ? null : x.toExternalForm();
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public URL valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        try {
            return URI.create(str).toURL();
        } catch (final MalformedURLException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
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
    public URL get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getURL(columnIndex);
    }

    /**
     *
     * @param rs
     * @param columnLabel
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public URL get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getURL(columnLabel);
    }

    /**
     *
     * @param stmt
     * @param columnIndex
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final URL x) throws SQLException {
        stmt.setURL(columnIndex, x);
    }

    /**
     *
     * @param stmt
     * @param parameterName
     * @param x
     * @throws SQLException the SQL exception
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final URL x) throws SQLException {
        stmt.setURL(parameterName, x);
    }
}
