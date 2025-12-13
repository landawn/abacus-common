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

package com.landawn.abacus.exception;

import java.io.Serial;
import java.sql.SQLException;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A runtime exception that wraps {@link SQLException}, allowing SQL exceptions to be thrown
 * without being declared in method signatures. This exception preserves the SQL-specific
 * information from the original SQLException, including the SQL state and vendor error code.
 * 
 * <p>This exception is particularly useful in contexts where SQLException cannot be declared:</p>
 * <ul>
 *   <li>Lambda expressions and functional interfaces used with database operations</li>
 *   <li>Stream operations processing database results</li>
 *   <li>DAO implementations where you want to avoid checked exceptions</li>
 *   <li>Transaction management code using functional programming patterns</li>
 * </ul>
 * 
 * <p>The exception preserves important SQL-specific information:</p>
 * <ul>
 *   <li>SQL State - A string identifying the exception per SQL standard</li>
 *   <li>Error Code - A vendor-specific error code</li>
 *   <li>Complete exception chain for debugging</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // In a stream operation processing database results
 * List<User> users = userIds.stream()
 *     .map(id -> {
 *         try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
 *             ps.setLong(1, id);
 *             ResultSet rs = ps.executeQuery();
 *             return mapToUser(rs);
 *         } catch (SQLException e) {
 *             throw new UncheckedSQLException("Failed to load user: " + id, e);
 *         }
 *     })
 *     .collect(Collectors.toList());
 * 
 * // In a lambda expression
 * Supplier<Connection> connectionSupplier = () -> {
 *     try {
 *         return dataSource.getConnection();
 *     } catch (SQLException e) {
 *         throw new UncheckedSQLException(e);
 *     }
 * };
 * 
 * // Handling the exception with SQL-specific information
 * try {
 *     performDatabaseOperation();
 * } catch (UncheckedSQLException e) {
 *     logger.error("SQL State: " + e.getSQLState());
 *     logger.error("Error Code: " + e.getErrorCode());
 *     throw e;
 * }
 * }</pre>
 * 
 * @see UncheckedException
 * @see SQLException
 * @see java.sql.SQLWarning
 * @see java.sql.DataTruncation
 */
public class UncheckedSQLException extends UncheckedException {

    @Serial
    private static final long serialVersionUID = 9083988895292299710L;

    /**
     * The wrapped SQLException that caused this unchecked exception.
     * Contains the original SQL error details including SQL state and error code.
     */
    private final SQLException sqlException;

    /**
     * Constructs a new {@code UncheckedSQLException} by wrapping the specified {@link SQLException}.
     *
     * <p>This constructor preserves all information from the original SQLException including
     * its message, SQL state, vendor error code, stack trace, and any suppressed exceptions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     statement.executeUpdate(sql);
     * } catch (SQLException e) {
     *     throw new UncheckedSQLException(e);
     * }
     * }</pre>
     *
     * @param cause the {@link SQLException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedSQLException(final SQLException cause) {
        super(cause);
        sqlException = cause;
    }

    /**
     * Constructs a new {@code UncheckedSQLException} with the specified detail message
     * and {@link SQLException}.
     *
     * <p>This constructor allows you to provide additional context about the database operation
     * that failed, while preserving all SQL-specific information from the original exception.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     connection.prepareStatement(query).executeQuery();
     * } catch (SQLException e) {
     *     throw new UncheckedSQLException(
     *         "Failed to execute query: " + query + " with params: " + params, e);
     * }
     * }</pre>
     *
     * @param message the detail message. The detail message is saved for later retrieval
     *                by the {@link #getMessage()} method.
     * @param cause the {@link SQLException} to wrap. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code cause} is {@code null}
     */
    public UncheckedSQLException(final String message, final SQLException cause) {
        super(message, cause);
        sqlException = cause;
    }

    /**
     * Retrieves the SQL state string from the underlying SQLException.
     * 
     * <p>The SQL state is a five-character string defined by the SQL standard (XOPEN or SQL:2003)
     * that identifies the type of error. Common SQL states include:</p>
     * <ul>
     *   <li>08xxx - Connection exceptions</li>
     *   <li>22xxx - Data exceptions</li>
     *   <li>23xxx - Constraint violations</li>
     *   <li>40xxx - Transaction rollback</li>
     *   <li>42xxx - Syntax errors or access violations</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     // database operation
     * } catch (UncheckedSQLException e) {
     *     if ("23505".equals(e.getSQLState())) {
     *         // Handle unique constraint violation
     *     }
     * }
     * }</pre>
     *
     * @return the SQL state string from the wrapped SQLException, or {@code null} if not available
     * @see SQLException#getSQLState()
     */
    @MayReturnNull
    public String getSQLState() {
        return sqlException.getSQLState();
    }

    /**
     * Retrieves the vendor-specific error code from the underlying SQLException.
     *
     * <p>This method provides access to the database vendor-specific error code that identifies
     * the specific error that occurred during a database operation. Unlike SQL state codes,
     * these error codes are specific to the database vendor (MySQL, Oracle, SQL Server, etc.)
     * and provide more granular information about the exact nature of the error.</p>
     *
     * <p>Common uses include:</p>
     * <ul>
     *   <li>Identifying specific database constraint violations</li>
     *   <li>Detecting known database-specific errors for specialized handling</li>
     *   <li>Detailed logging of database operations</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try {
     *     // Perform database operation
     * } catch (UncheckedSQLException e) {
     *     // Oracle error code 1 is "unique constraint violated"
     *     if (e.getErrorCode() == 1) {
     *         // Handle duplicate key violation
     *     }
     *     // Log with both general and specific error information
     *     logger.error("Database error: state={}, code={}",
     *                  e.getSQLState(), e.getErrorCode());
     * }
     * }</pre>
     *
     * @return the vendor-specific error code from the wrapped SQLException
     * @see #getSQLState()
     * @see SQLException#getErrorCode()
     */
    public int getErrorCode() {
        return sqlException.getErrorCode();
    }

}
