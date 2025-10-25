/*
 * Copyright (C) 2023 HaiYang Li
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
package com.landawn.abacus.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

/**
 * Utility class for managing database resources including connections, statements, and result sets.
 * This class provides methods for properly closing JDBC resources and handling Spring-managed connections
 * when Spring is available in the classpath.
 * 
 * <p>The class automatically detects if Spring Framework is present and uses Spring's
 * DataSourceUtils for connection management when available. This ensures proper transaction
 * participation and connection pooling in Spring environments.</p>
 * 
 * <p>All close methods in this class follow the pattern of closing resources in reverse order
 * of their creation (ResultSet -> Statement -> Connection) to ensure proper cleanup.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * Connection conn = null;
 * Statement stmt = null;
 * ResultSet rs = null;
 * try {
 *     conn = dataSource.getConnection();
 *     stmt = conn.createStatement();
 *     rs = stmt.executeQuery("SELECT * FROM users");
 *     // Process results
 * } finally {
 *     DataSourceUtil.closeQuietly(rs, stmt, conn);
 * }
 * }</pre>
 * 
 * @see java.sql.Connection
 * @see java.sql.Statement
 * @see java.sql.ResultSet
 */
@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
@Internal
public final class DataSourceUtil {

    static final Logger logger = LoggerFactory.getLogger(DataSourceUtil.class);

    private static boolean isInSpring = true;

    static {
        try {
            //noinspection ConstantValue
            isInSpring = ClassUtil.forClass("org.springframework.datasource.DataSourceUtils") != null;
        } catch (final Throwable e) {
            isInSpring = false;
        }
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private DataSourceUtil() {
        // utility class.
    }

    /**
     * Releases a Connection, returning it to the connection pool if applicable.
     * When Spring is present in the classpath, delegates to Spring's DataSourceUtils
     * to ensure proper transaction synchronization. Otherwise, simply closes the connection.
     * 
     * <p>This method should be used instead of directly calling {@code Connection.close()}
     * when working with Spring-managed data sources to ensure proper transaction handling.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = dataSource.getConnection();
     * try {
     *     // Use connection
     * } finally {
     *     DataSourceUtil.releaseConnection(conn, dataSource);
     * }
     * }</pre>
     *
     * @param conn the Connection to release, may be null
     * @param ds the DataSource that the Connection was obtained from, may be null
     */
    public static void releaseConnection(final Connection conn, final javax.sql.DataSource ds) {
        if (conn == null) {
            return;
        }

        if (isInSpring && ds != null) { //NOSONAR
            try {
                org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection(conn, ds);
            } catch (final NoClassDefFoundError e) {
                isInSpring = false;
                closeQuietly(conn);
            }
        } else {
            closeQuietly(conn);
        }
    }

    /**
     * Closes a ResultSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     DataSourceUtil.close(rs);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @throws UncheckedSQLException if a database access error occurs
     */
    public static void close(final ResultSet rs) throws UncheckedSQLException {
        if (rs != null) {
            try {
                rs.close();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     * Closes a ResultSet and optionally its associated Statement.
     * If closeStatement is true, attempts to retrieve and close the Statement that created the ResultSet.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     DataSourceUtil.close(rs, true); // Also closes the statement
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param closeStatement if true, also closes the Statement that created the ResultSet
     * @throws UncheckedSQLException if a database access error occurs
     */
    public static void close(final ResultSet rs, final boolean closeStatement) throws UncheckedSQLException {
        close(rs, closeStatement, false);
    }

    /**
     * Closes a ResultSet and optionally its associated Statement and Connection.
     * Resources are closed in reverse order: ResultSet -> Statement -> Connection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     // Closes ResultSet, Statement, and Connection
     *     DataSourceUtil.close(rs, true, true);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param closeStatement if true, also closes the Statement that created the ResultSet
     * @param closeConnection if true, also closes the Connection (requires closeStatement to be true)
     * @throws IllegalArgumentException if closeStatement is false while closeConnection is true
     * @throws UncheckedSQLException if a database access error occurs
     */
    public static void close(final ResultSet rs, final boolean closeStatement, final boolean closeConnection)
            throws IllegalArgumentException, UncheckedSQLException {
        if (closeConnection && !closeStatement) {
            throw new IllegalArgumentException("'closeStatement' can't be false while 'closeConnection' is true");
        }

        if (rs == null) {
            return;
        }

        Connection conn = null;
        Statement stmt = null;

        try {
            if (closeStatement) {
                stmt = rs.getStatement();
            }

            if (closeConnection && stmt != null) {
                conn = stmt.getConnection();
            }
        } catch (final SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            close(rs, stmt, conn);
        }
    }

    /**
     * Closes a Statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Statement stmt = conn.createStatement();
     * try {
     *     stmt.executeUpdate("UPDATE users SET active = true");
     * } finally {
     *     DataSourceUtil.close(stmt);
     * }
     * }</pre>
     *
     * @param stmt the Statement to close, may be null
     * @throws UncheckedSQLException if a database access error occurs
     */
    public static void close(final Statement stmt) throws UncheckedSQLException {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     * Closes a Connection.
     * Consider using {@link #releaseConnection(Connection, javax.sql.DataSource)} instead
     * when working with Spring-managed connections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = dataSource.getConnection();
     * try {
     *     // Use connection
     * } finally {
     *     DataSourceUtil.close(conn);
     * }
     * }</pre>
     *
     * @param conn the Connection to close, may be null
     * @throws UncheckedSQLException if a database access error occurs
     * @deprecated consider using {@link #releaseConnection(Connection, javax.sql.DataSource)}
     */
    @Deprecated
    public static void close(final Connection conn) throws UncheckedSQLException {
        if (conn != null) {
            try {
                conn.close();
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }
    }

    /**
     * Closes a ResultSet and Statement in the proper order.
     * The ResultSet is closed first, followed by the Statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Statement stmt = conn.createStatement();
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     DataSourceUtil.close(rs, stmt);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param stmt the Statement to close, may be null
     * @throws UncheckedSQLException if a database access error occurs
     */
    public static void close(final ResultSet rs, final Statement stmt) throws UncheckedSQLException {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (final SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e); //NOSONAR
            }
        }
    }

    /**
     * Closes a Statement and Connection in the proper order.
     * The Statement is closed first, followed by the Connection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = dataSource.getConnection();
     * Statement stmt = conn.createStatement();
     * try {
     *     stmt.executeUpdate("UPDATE users SET active = true");
     * } finally {
     *     DataSourceUtil.close(stmt, conn);
     * }
     * }</pre>
     *
     * @param stmt the Statement to close, may be null
     * @param conn the Connection to close, may be null
     * @throws UncheckedSQLException if a database access error occurs
     */
    public static void close(final Statement stmt, final Connection conn) throws UncheckedSQLException {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (final SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e); //NOSONAR
            }
        }
    }

    /**
     * Closes a ResultSet, Statement, and Connection in the proper order.
     * Resources are closed in reverse order of creation: ResultSet -> Statement -> Connection.
     * If any close operation fails, the exception is thrown after attempting to close remaining resources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = dataSource.getConnection();
     * Statement stmt = conn.createStatement();
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     DataSourceUtil.close(rs, stmt, conn);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param stmt the Statement to close, may be null
     * @param conn the Connection to close, may be null
     * @throws UncheckedSQLException if a database access error occurs
     */
    public static void close(final ResultSet rs, final Statement stmt, final Connection conn) throws UncheckedSQLException {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (final SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            try {
                if (stmt != null) {
                    stmt.close();
                }
            } catch (final SQLException e) {
                throw new UncheckedSQLException(e); //NOSONAR
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (final SQLException e) {
                    throw new UncheckedSQLException(e); //NOSONAR
                }
            }
        }
    }

    /**
     * Unconditionally closes a ResultSet.
     * Equivalent to {@link ResultSet#close()}, except any exceptions will be ignored and logged.
     * This is typically used in finally blocks where exception handling is not desired.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ResultSet rs = null;
     * try {
     *     rs = stmt.executeQuery("SELECT * FROM users");
     *     // Process results
     * } finally {
     *     DataSourceUtil.closeQuietly(rs);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     */
    public static void closeQuietly(final ResultSet rs) {
        closeQuietly(rs, null, null);
    }

    /**
     * Unconditionally closes a ResultSet and optionally its associated Statement.
     * Any exceptions during closing are ignored and logged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     DataSourceUtil.closeQuietly(rs, true); // Also closes statement
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param closeStatement if true, also closes the Statement that created the ResultSet
     * @throws UncheckedSQLException if unable to retrieve the Statement from ResultSet
     */
    public static void closeQuietly(final ResultSet rs, final boolean closeStatement) throws UncheckedSQLException {
        closeQuietly(rs, closeStatement, false);
    }

    /**
     * Unconditionally closes a ResultSet and optionally its associated Statement and Connection.
     * Any exceptions during closing are ignored and logged.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     // Quietly closes all resources
     *     DataSourceUtil.closeQuietly(rs, true, true);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param closeStatement if true, also closes the Statement that created the ResultSet
     * @param closeConnection if true, also closes the Connection (requires closeStatement to be true)
     * @throws IllegalArgumentException if closeStatement is false while closeConnection is true
     */
    public static void closeQuietly(final ResultSet rs, final boolean closeStatement, final boolean closeConnection) throws IllegalArgumentException {
        if (closeConnection && !closeStatement) {
            throw new IllegalArgumentException("'closeStatement' can't be false while 'closeConnection' is true");
        }

        if (rs == null) {
            return;
        }

        Connection conn = null;
        Statement stmt = null;

        try {
            if (closeStatement) {
                stmt = rs.getStatement();
            }

            if (closeConnection && stmt != null) {
                conn = stmt.getConnection();
            }
        } catch (final SQLException e) {
            logger.error("Failed to get Statement or Connection by ResultSet", e);
        } finally {
            closeQuietly(rs, stmt, conn);
        }
    }

    /**
     * Unconditionally closes a Statement.
     * Equivalent to {@link Statement#close()}, except any exceptions will be ignored and logged.
     * This is typically used in finally blocks where exception handling is not desired.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Statement stmt = null;
     * try {
     *     stmt = conn.createStatement();
     *     // Use statement
     * } finally {
     *     DataSourceUtil.closeQuietly(stmt);
     * }
     * }</pre>
     *
     * @param stmt the Statement to close, may be null
     */
    public static void closeQuietly(final Statement stmt) {
        closeQuietly(null, stmt, null);
    }

    /**
     * Unconditionally closes a Connection.
     * Equivalent to {@link Connection#close()}, except any exceptions will be ignored and logged.
     * Consider using {@link #releaseConnection(Connection, javax.sql.DataSource)} instead
     * when working with Spring-managed connections.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = null;
     * try {
     *     conn = dataSource.getConnection();
     *     // Use connection
     * } finally {
     *     DataSourceUtil.closeQuietly(conn);
     * }
     * }</pre>
     *
     * @param conn the Connection to close, may be null
     * @deprecated consider using {@link #releaseConnection(Connection, javax.sql.DataSource)}
     */
    @Deprecated
    public static void closeQuietly(final Connection conn) {
        closeQuietly(null, null, conn);
    }

    /**
     * Unconditionally closes a ResultSet and Statement.
     * Any exceptions during closing are ignored and logged.
     * Resources are closed in the proper order: ResultSet first, then Statement.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Statement stmt = null;
     * ResultSet rs = null;
     * try {
     *     stmt = conn.createStatement();
     *     rs = stmt.executeQuery("SELECT * FROM users");
     *     // Process results
     * } finally {
     *     DataSourceUtil.closeQuietly(rs, stmt);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param stmt the Statement to close, may be null
     */
    public static void closeQuietly(final ResultSet rs, final Statement stmt) {
        closeQuietly(rs, stmt, null);
    }

    /**
     * Unconditionally closes a Statement and Connection.
     * Any exceptions during closing are ignored and logged.
     * Resources are closed in the proper order: Statement first, then Connection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = null;
     * Statement stmt = null;
     * try {
     *     conn = dataSource.getConnection();
     *     stmt = conn.createStatement();
     *     // Use statement
     * } finally {
     *     DataSourceUtil.closeQuietly(stmt, conn);
     * }
     * }</pre>
     *
     * @param stmt the Statement to close, may be null
     * @param conn the Connection to close, may be null
     */
    public static void closeQuietly(final Statement stmt, final Connection conn) {
        closeQuietly(null, stmt, conn);
    }

    /**
     * Unconditionally closes a ResultSet, Statement, and Connection.
     * Any exceptions during closing are ignored and logged.
     * Resources are closed in the proper order: ResultSet -> Statement -> Connection.
     * This method is typically used in finally blocks where exception handling is not desired.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = null;
     * Statement stmt = null;
     * ResultSet rs = null;
     * try {
     *     conn = dataSource.getConnection();
     *     stmt = conn.createStatement();
     *     rs = stmt.executeQuery("SELECT * FROM users");
     *     // Process results
     * } finally {
     *     DataSourceUtil.closeQuietly(rs, stmt, conn);
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close, may be null
     * @param stmt the Statement to close, may be null
     * @param conn the Connection to close, may be null
     */
    public static void closeQuietly(final ResultSet rs, final Statement stmt, final Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (final Exception e) {
                logger.error("Failed to close ResultSet", e);
            }
        }

        if (stmt != null) {
            try {
                stmt.close();
            } catch (final Exception e) {
                logger.error("Failed to close Statement", e);
            }
        }

        if (conn != null) {
            try {
                conn.close();
            } catch (final Exception e) {
                logger.error("Failed to close Connection", e);
            }
        }
    }

    /**
     * Executes a batch of commands on a Statement and clears the batch.
     * This method ensures that the batch is cleared even if the execution fails,
     * preventing memory leaks from accumulated batch commands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * PreparedStatement pstmt = conn.prepareStatement("INSERT INTO users (name) VALUES (?)");
     * for (String name : names) {
     *     pstmt.setString(1, name);
     *     pstmt.addBatch();
     * }
     * int[] results = DataSourceUtil.executeBatch(pstmt);
     * }</pre>
     *
     * @param stmt the Statement containing the batch commands to execute
     * @return an array of update counts containing one element for each command in the batch
     * @throws SQLException if a database access error occurs or the driver does not support batch statements
     */
    @SuppressWarnings("UnusedReturnValue")
    public static int[] executeBatch(final Statement stmt) throws SQLException {
        try {
            return stmt.executeBatch();
        } finally {
            try {
                stmt.clearBatch();
            } catch (final SQLException e) {
                logger.error("Failed to clear batch parameters after executeBatch", e);
            }
        }
    }
}