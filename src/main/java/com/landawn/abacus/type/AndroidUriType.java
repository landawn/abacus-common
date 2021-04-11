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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.util.N;

import android.net.Uri;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class AndroidUriType extends AbstractType<Uri> {

    public static final String Uri = Uri.class.getSimpleName();

    AndroidUriType() {
        super(Uri);
    }

    @Override
    public Class<Uri> clazz() {
        return Uri.class;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(Uri x) {
        return (x == null) ? null : x.toString();
    }

    /**
     *
     * @param str
     * @return
     */
    @Override
    public Uri valueOf(String str) {
        if (N.isNullOrEmpty(str)) {
            return null;
        }

        return android.net.Uri.parse(str);
    }

    /**
     *
     * @param rs
     * @param columnIndex
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public Uri get(ResultSet rs, int columnIndex) throws SQLException {
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
    public Uri get(ResultSet rs, String columnLabel) throws SQLException {
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
    public void set(PreparedStatement stmt, int columnIndex, Uri x) throws SQLException {
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
    public void set(CallableStatement stmt, String parameterName, Uri x) throws SQLException {
        stmt.setString(parameterName, stringOf(x));
    }
}
