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
 * Type handler for password fields that automatically hashes passwords before storing them in a database.
 * This type extends {@link AbstractStringType} and uses SHA-256 as the default digest algorithm,
 * delegating to {@link Password} which performs a one-way hash via {@link java.security.MessageDigest}
 * and returns a Base64-encoded digest.
 *
 * <p>When setting password values via {@link #set(PreparedStatement, int, String)} or
 * {@link #set(CallableStatement, String, String)}, the input plain-text password is hashed before
 * being bound to the statement. Reads via {@link #get(ResultSet, int)} or
 * {@link #get(ResultSet, String)} return the stored hashed value as-is; because hashing is one-way,
 * there is no {@code stringOf}/{@code valueOf} round-trip that recovers the original plain-text password.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Storing a password (automatically hashed before being bound)
 * String plainPassword = "mySecretPassword";
 * PreparedStatement stmt = conn.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
 * stmt.setString(1, "john");
 *
 * Type<String> passwordType = TypeFactory.getType("Password");
 * passwordType.set(stmt, 2, plainPassword);   // Password is hashed before storage
 * stmt.executeUpdate();
 *
 * // Retrieving a password (returns the stored hashed form)
 * ResultSet rs = stmt.executeQuery("SELECT password FROM users WHERE username = 'john'");
 * if (rs.next()) {
 *     String hashedPassword = passwordType.get(rs, 1);   // Returns the hashed value
 * }
 * }</pre>
 *
 */
@SuppressWarnings("java:S2160")
public class PasswordType extends AbstractStringType {

    /** The type name constant for Password type identification, equal to {@code "Password"}. */
    public static final String PASSWORD = "Password";

    private static final String DEFAULT_ALGORITHM = "SHA-256";

    private final Password password; //NOSONAR

    /**
     * Constructs a new PasswordType with the default SHA-256 hashing algorithm.
     * This constructor is package-private and intended to be called only by the TypeFactory.
     */
    PasswordType() {
        this(DEFAULT_ALGORITHM);
    }

    /**
     * Constructs a new PasswordType with the specified hashing algorithm.
     * This constructor is protected to allow subclassing with custom hashing algorithms
     * while maintaining controlled instantiation through the TypeFactory.
     *
     * @param algorithm the message-digest algorithm to use for password hashing (e.g., "SHA-256", "MD5")
     */
    protected PasswordType(final String algorithm) {
        super(PASSWORD);
        password = new Password(algorithm);
    }

    /**
     * Retrieves a password value from a ResultSet at the specified column index.
     * The password is returned as-is from the database (typically the previously stored hashed form).
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
     * The password is returned as-is from the database (typically the previously stored hashed form).
     *
     * @param rs the ResultSet to read from
     * @param columnName the column label/name
     * @return the password string from the database
     * @throws SQLException if a database access error occurs
     */
    @Override
    public String get(final ResultSet rs, final String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    /**
     * Sets a password value in a PreparedStatement at the specified parameter index.
     * The plain-text password is automatically hashed using the configured algorithm before being bound.
     *
     * @param stmt the PreparedStatement to set the parameter on
     * @param columnIndex the parameter index (1-based)
     * @param x the plain-text password to hash and set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final PreparedStatement stmt, final int columnIndex, final String x) throws SQLException {
        stmt.setString(columnIndex, password.encrypt(x));
    }

    /**
     * Sets a password value in a CallableStatement using the specified parameter name.
     * The plain-text password is automatically hashed using the configured algorithm before being bound.
     *
     * @param stmt the CallableStatement to set the parameter on
     * @param parameterName the name of the parameter
     * @param x the plain-text password to hash and set
     * @throws SQLException if a database access error occurs
     */
    @Override
    public void set(final CallableStatement stmt, final String parameterName, final String x) throws SQLException {
        stmt.setString(parameterName, password.encrypt(x));
    }
}
