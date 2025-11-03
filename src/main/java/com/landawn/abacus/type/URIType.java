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

import java.net.URI;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.util.Strings;

public class URIType extends AbstractType<URI> {

    public static final String URI = URI.class.getSimpleName();

    URIType() {
        super(URI);
    }

    /**
     * Returns the Class object representing the URI class.
     * <p>
     * This method returns {@code URI.class}, which is the Class object for the
     * {@link java.net.URI} class that this URIType handles.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<URI> type = TypeFactory.getType(URI.class);
     * Class<URI> clazz = type.clazz(); // Returns URI.class
     * }</pre>
     *
     * @return the Class object for URI.class
     */
    @Override
    public Class<URI> clazz() {
        return URI.class;
    }

    /**
     * Converts a URI instance to its string representation.
     * <p>
     * This method returns the string representation of the URI by calling its {@code toString()} method.
     * If the input URI is {@code null}, this method returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<URI> type = TypeFactory.getType(URI.class);
     * URI uri = URI.create("https://example.com/path");
     * String str = type.stringOf(uri); // Returns "https://example.com/path"
     * }</pre>
     *
     * @param x the URI instance to convert to string
     * @return the string representation of the URI, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final URI x) {
        return (x == null) ? null : x.toString();
    }

    /**
     * Parses a string representation to create a URI instance.
     * <p>
     * This method creates a URI instance from the provided string using {@link URI#create(String)}.
     * If the string is {@code null} or empty, this method returns {@code null}.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<URI> type = TypeFactory.getType(URI.class);
     * URI uri = type.valueOf("https://example.com/path"); // Creates a URI
     * }</pre>
     *
     * @param str the string to convert to a URI
     * @return a URI instance created from the string, or {@code null} if the string is empty
     * @throws IllegalArgumentException if the string violates RFC 2396 URI syntax rules
     */
    @Override
    public URI valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        return java.net.URI.create(str); // NOSONAR
    }

    /**
     * Retrieves a URI value from a ResultSet at the specified column index.
     * <p>
     * This method reads a string value from the ResultSet and converts it to a URI
     * using the {@link #valueOf(String)} method.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<URI> type = TypeFactory.getType(URI.class);
     * ResultSet rs = statement.executeQuery("SELECT homepage FROM websites");
     * URI homepage = type.get(rs, 1); // Get URI from first column
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based) of the URI value
     * @return the URI value, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column index is invalid
     */
    @Override
    public URI get(final ResultSet rs, final int columnIndex) throws SQLException {
        return valueOf(rs.getString(columnIndex));
    }

    /**
     * Retrieves a URI value from a ResultSet using the specified column label.
     * <p>
     * This method reads a string value from the ResultSet and converts it to a URI
     * using the {@link #valueOf(String)} method.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<URI> type = TypeFactory.getType(URI.class);
     * ResultSet rs = statement.executeQuery("SELECT homepage FROM websites");
     * URI homepage = type.get(rs, "homepage"); // Get URI by column name
     * }</pre>
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the label of the column containing the URI value
     * @return the URI value, or {@code null} if the database value is NULL
     * @throws SQLException if a database access error occurs or the column label is invalid
     */
    @Override
    public URI get(final ResultSet rs, final String columnLabel) throws SQLException {
        return valueOf(rs.getString(columnLabel));
    }

    /**
     * Sets a URI value in a PreparedStatement at the specified parameter index.
     * <p>
     * This method converts the URI to its string representation and sets it in the
     * PreparedStatement. If the URI is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<URI> type = TypeFactory.getType(URI.class);
     * PreparedStatement stmt = conn.prepareStatement("INSERT INTO websites (homepage) VALUES (?)");
     * type.set(stmt, 1, URI.create("https://example.com")); // Set URI at parameter index 1
     * }</pre>
     *
     * @param stmt the PreparedStatement to set the value in
     * @param columnIndex the parameter index (1-based) where to set the URI value
     * @param x the URI value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter index is invalid
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final URI x) throws SQLException {
        stmt.setString(columnIndex, stringOf(x));
    }

    /**
     * Sets a URI value in a CallableStatement using the specified parameter name.
     * <p>
     * This method converts the URI to its string representation and sets it in the
     * CallableStatement. If the URI is {@code null}, a NULL value is set.
     * </p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<URI> type = TypeFactory.getType(URI.class);
     * CallableStatement stmt = conn.prepareCall("{call update_website(?)}");
     * type.set(stmt, "homepage", URI.create("https://example.com")); // Set URI by parameter name
     * }</pre>
     *
     * @param stmt the CallableStatement to set the value in
     * @param parameterName the name of the parameter where to set the URI value
     * @param x the URI value to set, or {@code null} for SQL NULL
     * @throws SQLException if a database access error occurs or the parameter name is invalid
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final URI x) throws SQLException {
        stmt.setString(parameterName, stringOf(x));
    }
}
