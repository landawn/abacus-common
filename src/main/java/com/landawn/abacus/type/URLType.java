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

    /**
     * Returns the Class object representing the URL class.
     * <p>
     * This method returns {@code URL.class}, which is the Class object for the
     * {@link java.net.URL} class that this URLType handles.
     * </p>
     *
     * @return the Class object for URL.class
     */
    @Override
    public Class<URL> clazz() {
        return URL.class;
    }

    /**
     * Converts a URL instance to its external string representation.
     * <p>
     * This method returns the external form of the URL by calling its {@code toExternalForm()} method,
     * which produces a string representation of the URL suitable for use in creating new URL instances.
     * If the input URL is null, this method returns null.
     * </p>
     *
     * @param x the URL instance to convert to string
     * @return the external string representation of the URL, or null if the input is null
     */
    @Override
    public String stringOf(final URL x) {
        return (x == null) ? null : x.toExternalForm();
    }

    /**
     * Converts a string to a URL instance.
     * <p>
     * This method creates a URL instance from the provided string by first creating a URI
     * and then converting it to a URL. If the string is null or empty, this method returns null.
     * </p>
     * <p>
     * If the string is not a valid URL format, this method throws a runtime exception.
     * </p>
     *
     * @param str the string to convert to a URL
     * @return a URL instance created from the string, or null if the string is empty
     * @throws RuntimeException if the string is not a valid URL format (wraps MalformedURLException)
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
     * Retrieves a URL value from a ResultSet at the specified column index.
     * <p>
     * This method uses the ResultSet's {@code getURL} method to directly retrieve
     * the URL value from the database.
     * </p>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) of the URL value
     * @return the URL value, or null if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public URL get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getURL(columnIndex);
    }

    /**
     * Retrieves a URL value from a ResultSet using the specified column label.
     * <p>
     * This method uses the ResultSet's {@code getURL} method to directly retrieve
     * the URL value from the database using the column name.
     * </p>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column containing the URL value
     * @return the URL value, or null if the database value is NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public URL get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getURL(columnLabel);
    }

    /**
     * Sets a URL value in a PreparedStatement at the specified parameter index.
     * <p>
     * This method uses the PreparedStatement's {@code setURL} method to directly set
     * the URL value. If the URL is null, a NULL value is set in the database.
     * </p>
     *
     * @param stmt the PreparedStatement to set the value in
     * @param columnIndex the parameter index (1-based) where to set the URL value
     * @param x the URL value to set, or null for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final URL x) throws SQLException {
        stmt.setURL(columnIndex, x);
    }

    /**
     * Sets a URL value in a CallableStatement using the specified parameter name.
     * <p>
     * This method uses the CallableStatement's {@code setURL} method to directly set
     * the URL value. If the URL is null, a NULL value is set in the database.
     * </p>
     *
     * @param stmt the CallableStatement to set the value in
     * @param parameterName the name of the parameter where to set the URL value
     * @param x the URL value to set, or null for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final URL x) throws SQLException {
        stmt.setURL(parameterName, x);
    }
}