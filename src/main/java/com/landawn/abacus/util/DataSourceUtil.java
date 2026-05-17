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
 * of their creation (ResultSet -&gt; Statement -&gt; Connection) to ensure proper cleanup.</p>
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

    // volatile because releaseConnection writes this from any thread on a NoClassDefFoundError;
    // without it, other threads may keep reading the stale `true` indefinitely.
    private static volatile boolean isInSpring = true;

    static {
        try {
            //noinspection ConstantValue
            isInSpring = ClassUtil.forName("org.springframework.jdbc.datasource.DataSourceUtils") != null;
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
     * When Spring is present in the classpath and a non-{@code null} DataSource is supplied,
     * delegates to {@code org.springframework.jdbc.datasource.DataSourceUtils.releaseConnection}
     * to ensure proper transaction synchronization. Otherwise, the connection is closed quietly
     * (any {@link SQLException} thrown by {@code close()} is logged and swallowed).
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
     * <p>If {@code conn} is {@code null}, this method returns immediately and does nothing.</p>
     *
     * @param conn the Connection to release; if {@code null}, this method does nothing
     * @param ds the DataSource that the Connection was obtained from; may be {@code null},
     *           in which case the connection is simply closed quietly
     * @see #closeQuietly(Connection)
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
     * Closes the given ResultSet.
     * If {@code rs} is {@code null}, this method does nothing.
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
     * @param rs the ResultSet to close; may be {@code null}
     * @throws UncheckedSQLException if a database access error occurs while closing the ResultSet
     * @see #closeQuietly(ResultSet)
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
     * If {@code closeStatement} is {@code true}, attempts to retrieve and close the Statement that
     * created the ResultSet. If {@code rs} is {@code null}, this method does nothing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ResultSet rs = stmt.executeQuery("SELECT * FROM users");
     * try {
     *     // Process results
     * } finally {
     *     DataSourceUtil.close(rs, true);   // Also closes the statement
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close; may be {@code null}
     * @param closeStatement if {@code true}, also closes the Statement that created the ResultSet
     * @throws UncheckedSQLException if a database access error occurs while closing the resources
     */
    public static void close(final ResultSet rs, final boolean closeStatement) throws UncheckedSQLException {
        close(rs, closeStatement, false);
    }

    /**
     * Closes a ResultSet and optionally its associated Statement and Connection.
     * Resources are closed in reverse order: ResultSet -&gt; Statement -&gt; Connection.
     * If {@code rs} is {@code null}, this method does nothing (after validating the argument combination).
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
     * @param rs the ResultSet to close; may be {@code null}
     * @param closeStatement if {@code true}, also closes the Statement that created the ResultSet
     * @param closeConnection if {@code true}, also closes the Connection (requires {@code closeStatement} to be {@code true})
     * @throws IllegalArgumentException if {@code closeStatement} is {@code false} while {@code closeConnection} is {@code true}
     * @throws UncheckedSQLException if a database access error occurs while retrieving or closing the resources
     */
    public static void close(final ResultSet rs, final boolean closeStatement, final boolean closeConnection)
            throws IllegalArgumentException, UncheckedSQLException {
        if (closeConnection && !closeStatement) {
            throw new IllegalArgumentException("'closeStatement' cannot be false while 'closeConnection' is true");
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
     * Closes the given Statement.
     * If {@code stmt} is {@code null}, this method does nothing.
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
     * @param stmt the Statement to close; may be {@code null}
     * @throws UncheckedSQLException if a database access error occurs while closing the Statement
     * @see #closeQuietly(Statement)
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
     * Closes the given Connection.
     * If {@code conn} is {@code null}, this method does nothing.
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
     * @param conn the Connection to close; may be {@code null}
     * @throws UncheckedSQLException if a database access error occurs while closing the Connection
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
     * The ResultSet is closed first, followed by the Statement. The Statement is closed
     * even if closing the ResultSet throws; any {@code null} argument is skipped.
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
     * @param rs the ResultSet to close; may be {@code null}
     * @param stmt the Statement to close; may be {@code null}
     * @throws UncheckedSQLException if a database access error occurs while closing either resource
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
     * The Statement is closed first, followed by the Connection. The Connection is closed
     * even if closing the Statement throws; any {@code null} argument is skipped.
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
     * @param stmt the Statement to close; may be {@code null}
     * @param conn the Connection to close; may be {@code null}
     * @throws UncheckedSQLException if a database access error occurs while closing either resource
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
     * Resources are closed in reverse order of creation: ResultSet -&gt; Statement -&gt; Connection.
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
     * @param rs the ResultSet to close; may be {@code null}
     * @param stmt the Statement to close; may be {@code null}
     * @param conn the Connection to close; may be {@code null}
     * @throws UncheckedSQLException if a database access error occurs while closing any of the resources
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
     * @param rs the ResultSet to close; may be {@code null}, in which case this method does nothing
     * @see #close(ResultSet)
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
     *     DataSourceUtil.closeQuietly(rs, true);   // Also closes statement
     * }
     * }</pre>
     *
     * @param rs the ResultSet to close; may be {@code null}
     * @param closeStatement if {@code true}, also closes the Statement that created the ResultSet
     */
    public static void closeQuietly(final ResultSet rs, final boolean closeStatement) {
        closeQuietly(rs, closeStatement, false);
    }

    /**
     * Unconditionally closes a ResultSet and optionally its associated Statement and Connection.
     * Any exceptions during closing are ignored and logged. Any failure to retrieve the Statement
     * or Connection from the ResultSet is also logged and swallowed. If {@code rs} is {@code null},
     * this method does nothing (after validating the argument combination).
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
     * @param rs the ResultSet to close; may be {@code null}
     * @param closeStatement if {@code true}, also closes the Statement that created the ResultSet
     * @param closeConnection if {@code true}, also closes the Connection (requires {@code closeStatement} to be {@code true})
     * @throws IllegalArgumentException if {@code closeStatement} is {@code false} while {@code closeConnection} is {@code true}
     */
    public static void closeQuietly(final ResultSet rs, final boolean closeStatement, final boolean closeConnection) throws IllegalArgumentException {
        if (closeConnection && !closeStatement) {
            throw new IllegalArgumentException("'closeStatement' cannot be false while 'closeConnection' is true");
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
     * @param stmt the Statement to close; may be {@code null}, in which case this method does nothing
     * @see #close(Statement)
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
     * @param conn the Connection to close; may be {@code null}, in which case this method does nothing
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
     * @param rs the ResultSet to close; may be {@code null}
     * @param stmt the Statement to close; may be {@code null}
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
     * @param stmt the Statement to close; may be {@code null}
     * @param conn the Connection to close; may be {@code null}
     */
    public static void closeQuietly(final Statement stmt, final Connection conn) {
        closeQuietly(null, stmt, conn);
    }

    /**
     * Unconditionally closes a ResultSet, Statement, and Connection.
     * Any exceptions during closing are ignored and logged.
     * Resources are closed in the proper order: ResultSet -&gt; Statement -&gt; Connection.
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
     * @param rs the ResultSet to close; may be {@code null}
     * @param stmt the Statement to close; may be {@code null}
     * @param conn the Connection to close; may be {@code null}
     * @see #close(ResultSet, Statement, Connection)
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
     * @param stmt the Statement containing the batch commands to execute; must not be {@code null}
     * @return an array of update counts containing one element for each command in the batch;
     *         the elements are ordered according to the order in which commands were added to the batch
     * @throws SQLException if a database access error occurs or the driver does not support batch statements
     * @see Statement#executeBatch()
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
