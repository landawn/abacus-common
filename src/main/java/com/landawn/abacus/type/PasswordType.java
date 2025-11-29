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

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.landawn.abacus.util.Password;

/**
 * Type handler for password fields that automatically encrypts passwords before storing them in a database.
 * This type extends AbstractStringType and uses SHA-256 as the default encryption algorithm.
 * When setting password values in prepared statements or callable statements, the password is automatically
 * encrypted using the configured algorithm before being stored.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Storing a password (automatically encrypted)
 * String plainPassword = "mySecretPassword";
 * PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
 * stmt.setString(1, "john");
 *
 * Type<String> passwordType = TypeFactory.getType("Password");
 * passwordType.set(stmt, 2, plainPassword);  // Password is encrypted before storage
 * stmt.executeUpdate();
 *
 * // Retrieving a password (returns encrypted form)
 * ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE username = 'john'");
 * if (rs.next()) {
 *     String encryptedPassword = passwordType.get(rs, 1);  // Returns encrypted password
 * }
 * }</pre>
 */
@SuppressWarnings("java:S2160")
public class PasswordType extends AbstractStringType {

    public static final String PASSWORD = "Password";

    private static final String DEFAULT_ALGORITHM = "SHA-256";

    private final Password password; //NOSONAR

    PasswordType() {
        this(DEFAULT_ALGORITHM);
    }

    protected PasswordType(final String algorithm) {
        super(PASSWORD);
        password = new Password(algorithm);
    }

    /**
     * Retrieves a password value from a ResultSet at the specified column index.
     * The password is returned as-is from the database (in its encrypted form).
     *
     * @param rs the ResultSet to read from
     * @param columnIndex the column index (1-based)
     * @return the password string from the database
     * @throws SQLException if a database access error occurs
     */
    @Override
    public String get(final ResultSet rs, final int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    /**
     * Retrieves a password value from a ResultSet using the specified column label.
     * The password is returned as-is from the database (in its encrypted form).
     *
     * @param rs the ResultSet to read from
     * @param columnLabel the column label/name
     * @return the password string from the database
     * @throws SQLException if a database access error occurs
     */
    @Override
    public String get(final ResultSet rs, final String columnLabel) throws SQLException {
        return rs.getString(columnLabel);
    }

    /**
     * Sets a password value in a PreparedStatement at the specified parameter index.
     * The password is automatically encrypted using the configured algorithm before being set.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the plain text password to encrypt and set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final String x) throws SQLException {
        stmt.setString(columnIndex, password.encrypt(x));
    }

    /**
     * Sets a password value in a CallableStatement using the specified parameter name.
     * The password is automatically encrypted using the configured algorithm before being set.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the plain text password to encrypt and set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final String x) throws SQLException {
        stmt.setString(parameterName, password.encrypt(x));
    }
}
