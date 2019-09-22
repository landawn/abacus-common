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

package com.landawn.abacus.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.IsolationLevel;
import com.landawn.abacus.Transaction;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;

// TODO: Auto-generated Javadoc
/**
 * DO NOT CLOSE the connection manually. It will be automatically closed after the transaction is committed or rolled back.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class SQLTransaction implements Transaction {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SQLTransaction.class);

    /** The Constant threadTransactionMap. */
    private static final Map<String, SQLTransaction> threadTransactionMap = new ConcurrentHashMap<>();
    // private static final Map<String, SQLTransaction> attachedThreadTransactionMap = new ConcurrentHashMap<>();

    /** The id. */
    private final String id;

    /** The timed id. */
    private final String timedId;

    /** The ds. */
    private final javax.sql.DataSource ds;

    /** The conn. */
    private final Connection conn;

    /** The close connection. */
    private final boolean closeConnection;

    /** The original auto commit. */
    private final boolean originalAutoCommit;

    /** The original isolation level. */
    private final int originalIsolationLevel;

    /** The status. */
    private Transaction.Status status = Status.ACTIVE;

    /** The ref count. */
    private final AtomicInteger refCount = new AtomicInteger();

    /** The isolation level stack. */
    private final Stack<IsolationLevel> isolationLevelStack = new Stack<>();

    /** The is for update only stack. */
    private final Stack<Boolean> isForUpdateOnlyStack = new Stack<>();

    /** The isolation level. */
    private IsolationLevel isolationLevel;

    /** The is for update only. */
    private boolean isForUpdateOnly;

    /** The is marked by commit previously. */
    private boolean isMarkedByCommitPreviously = false;

    /**
     * Instantiates a new SQL transaction.
     *
     * @param ds
     * @param conn
     * @param isolationLevel
     * @param creator
     * @param closeConnection
     * @throws SQLException the SQL exception
     */
    SQLTransaction(final javax.sql.DataSource ds, final Connection conn, final IsolationLevel isolationLevel, final CreatedBy creator,
            final boolean closeConnection) throws SQLException {
        N.checkArgNotNull(conn);
        N.checkArgNotNull(isolationLevel);

        this.id = getTransactionId(ds, creator);
        this.timedId = id + "_" + System.currentTimeMillis();
        this.ds = ds;
        this.conn = conn;
        this.isolationLevel = isolationLevel;
        this.closeConnection = closeConnection;

        this.originalAutoCommit = conn.getAutoCommit();
        this.originalIsolationLevel = conn.getTransactionIsolation();

        conn.setAutoCommit(false);

        if (isolationLevel != IsolationLevel.DEFAULT) {
            conn.setTransactionIsolation(isolationLevel.intValue());
        }
    }

    /**
     *
     * @return
     */
    @Override
    public String id() {
        return timedId;
    }

    /**
     *
     * @return
     */
    public Connection connection() {
        return conn;
    }

    /**
     *
     * @return
     */
    @Override
    public IsolationLevel isolationLevel() {
        return isolationLevel;
    }

    /**
     *
     * @return
     */
    @Override
    public Transaction.Status status() {
        return status;
    }

    /**
     * Checks if is active.
     *
     * @return true, if is active
     */
    @Override
    public boolean isActive() {
        return status == Status.ACTIVE;
    }

    //    /**
    //     * Attaches this transaction to current thread.
    //     *
    //     */
    //    public void attach() {
    //        final String currentThreadName = Thread.currentThread().getName();
    //        final String resourceId = ttid.substring(ttid.lastIndexOf('_') + 1);
    //        final String targetTTID = currentThreadName + "_" + resourceId;
    //
    //        if (attachedThreadTransactionMap.containsKey(targetTTID)) {
    //            throw new IllegalStateException("Transaction(id=" + attachedThreadTransactionMap.get(targetTTID).id()
    //                    + ") has already been attached to current thread: " + currentThreadName);
    //        } else if (threadTransactionMap.containsKey(targetTTID)) {
    //            throw new IllegalStateException(
    //                    "Transaction(id=" + threadTransactionMap.get(targetTTID).id() + ") has already been created in current thread: " + currentThreadName);
    //        }
    //
    //        attachedThreadTransactionMap.put(targetTTID, this);
    //        threadTransactionMap.put(targetTTID, this);
    //    }
    //
    //    public void detach() {
    //        final String currentThreadName = Thread.currentThread().getName();
    //        final String resourceId = ttid.substring(ttid.lastIndexOf('_') + 1);
    //        final String targetTTID = currentThreadName + "_" + resourceId;
    //
    //        if (!attachedThreadTransactionMap.containsKey(targetTTID)) {
    //            throw new IllegalStateException(
    //                    "Transaction(id=" + attachedThreadTransactionMap.get(targetTTID).id() + ") is not attached to current thread: " + currentThreadName);
    //        }
    //
    //        threadTransactionMap.remove(targetTTID);
    //        attachedThreadTransactionMap.remove(targetTTID);
    //    }

    /**
     *
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    @Override
    public void commit() throws UncheckedSQLException {
        final int refCount = decrementAndGetRef();
        isMarkedByCommitPreviously = true;

        if (refCount > 0) {
            return;
        } else if (refCount < 0) {
            logger.warn("Transaction(id={}) is already: {}. This committing is ignored", timedId, status);
            return;
        }

        if (status == Status.MARKED_ROLLBACK) {
            logger.warn("Transaction(id={}) will be rolled back because it's marked for roll back only", timedId);
            executeRollback();
            return;
        }

        if (status != Status.ACTIVE) {
            throw new IllegalArgumentException("Transaction(id=" + timedId + ") is already: " + status + ". It can not be committed");
        }

        logger.info("Committing transaction(id={})", timedId);

        status = Status.FAILED_COMMIT;

        try {
            if (originalAutoCommit) {
                conn.commit();
            }

            status = Status.COMMITTED;
        } catch (SQLException e) {
            throw new UncheckedSQLException("Failed to commit transaction(id=" + id + ")", e);
        } finally {
            if (status == Status.COMMITTED) {
                logger.info("Transaction(id={}) has been committed successfully", timedId);

                resetAndCloseConnection();
            } else {
                logger.warn("Failed to commit transaction(id={}). It will automatically be rolled back ", timedId);
                executeRollback();
            }
        }
    }

    /**
     * Transaction can be started:
     *
     * <pre>
     * <code>
     *   final SQLTransaction tran = sqlExecutor.beginTransaction(IsolationLevel.READ_COMMITTED);
     *   try {
     *       // sqlExecutor.insert(...);
     *       // sqlExecutor.update(...);
     *       // sqlExecutor.query(...);
     *
     *       tran.commit();
     *   } finally {
     *       // The connection will be automatically closed after the transaction is committed or rolled back.
     *       tran.rollbackIfNotCommitted();
     *   }
     * </code>
     * </pre>
     *
     * @throws UncheckedSQLException the unchecked SQL exception
     * @see SQLExecutor#beginTransaction(IsolationLevel)
     * @deprecated replaced by {@code #rollbackIfNotCommitted()}
     */
    @Deprecated
    @Override
    public void rollback() throws UncheckedSQLException {
        final int refCount = decrementAndGetRef();
        isMarkedByCommitPreviously = true;

        if (refCount > 0) {
            status = Status.MARKED_ROLLBACK;
            return;
        } else if (refCount < 0) {
            logger.warn("Transaction(id={}) is already: {}. This rollback is ignored", timedId, status);
            return;
        }

        if (!(status.equals(Status.ACTIVE) || status.equals(Status.MARKED_ROLLBACK) || status == Status.FAILED_COMMIT)) {
            throw new IllegalStateException("Transaction(id=" + timedId + ") is already: " + status);
        }

        executeRollback();
    }

    /**
     * Rollback if not committed.
     *
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public void rollbackIfNotCommitted() throws UncheckedSQLException {
        if (isMarkedByCommitPreviously) { // Do nothing. It happened in finally block.
            isMarkedByCommitPreviously = false;
            return;
        }

        final int refCount = decrementAndGetRef();

        if (refCount > 0) {
            status = Status.MARKED_ROLLBACK;
            return;
        } else if (refCount < 0) {
            if (refCount == -1
                    && (status == Status.COMMITTED || status == Status.FAILED_COMMIT || status == Status.ROLLED_BACK || status == Status.FAILED_ROLLBACK)) {
                // Do nothing. It happened in finally block.
            } else {
                logger.warn("Transaction(id={}) is already: {}. This rollback is ignored", timedId, status);
            }

            return;
        }

        if (!(status == Status.ACTIVE || status == Status.MARKED_ROLLBACK || status == Status.FAILED_COMMIT || status == Status.FAILED_ROLLBACK)) {
            throw new IllegalArgumentException("Transaction(id=" + timedId + ") is already: " + status + ". It can not be rolled back");
        }

        executeRollback();
    }

    /**
     *
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    private void executeRollback() throws UncheckedSQLException {
        logger.warn("Rolling back transaction(id={})", timedId);

        status = Status.FAILED_ROLLBACK;

        try {
            if (originalAutoCommit) {
                conn.rollback();
            }

            status = Status.ROLLED_BACK;
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            if (status == Status.ROLLED_BACK) {
                logger.warn("Transaction(id={}) has been rolled back successfully", timedId);
            } else {
                logger.warn("Failed to roll back transaction(id={})", timedId);
            }

            resetAndCloseConnection();
        }
    }

    /**
     * Reset and close connection.
     */
    private void resetAndCloseConnection() {
        try {
            conn.setAutoCommit(originalAutoCommit);
            conn.setTransactionIsolation(originalIsolationLevel);
        } catch (SQLException e) {
            logger.warn("Failed to reset connection", e);
        } finally {
            if (closeConnection) {
                JdbcUtil.releaseConnection(conn, ds);
            }
        }
    }

    /**
     * Increment and get ref.
     *
     * @param isolationLevel
     * @param forUpdateOnly
     * @return
     */
    synchronized int incrementAndGetRef(final IsolationLevel isolationLevel, final boolean forUpdateOnly) {
        if (!status.equals(Status.ACTIVE)) {
            throw new IllegalStateException("Transaction(id=" + timedId + ") is already: " + status);
        }

        isMarkedByCommitPreviously = false;

        if (conn != null) {
            try {
                if (isolationLevel == IsolationLevel.DEFAULT) {
                    conn.setTransactionIsolation(originalIsolationLevel);
                } else {
                    conn.setTransactionIsolation(isolationLevel.intValue());
                }
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }
        }

        if (refCount.get() > 0) {
            this.isolationLevelStack.push(this.isolationLevel);
            this.isForUpdateOnlyStack.push(this.isForUpdateOnly);
        }

        this.isolationLevel = isolationLevel;
        this.isForUpdateOnly = forUpdateOnly;

        return refCount.incrementAndGet();
    }

    /**
     * Decrement and get ref.
     *
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    synchronized int decrementAndGetRef() throws UncheckedSQLException {
        final int res = refCount.decrementAndGet();

        if (res == 0) {
            threadTransactionMap.remove(id);

            logger.info("Finishing transaction(id={})", timedId);

            logger.debug("Remaining active transactions: {}", threadTransactionMap.values());
        } else if (res > 0) {
            this.isolationLevel = isolationLevelStack.pop();
            this.isForUpdateOnly = isForUpdateOnlyStack.pop();

            if (conn != null) {
                try {
                    if (isolationLevel == IsolationLevel.DEFAULT) {
                        conn.setTransactionIsolation(originalIsolationLevel);
                    } else {
                        conn.setTransactionIsolation(isolationLevel.intValue());
                    }
                } catch (SQLException e) {
                    throw new UncheckedSQLException(e);
                }
            }
        }

        return res;
    }

    /**
     * Checks if is for update only.
     *
     * @return true, if is for update only
     */
    boolean isForUpdateOnly() {
        return isForUpdateOnly;
    }

    /**
     *
     * @param ds
     * @param creator
     * @return
     */
    static String getTransactionId(javax.sql.DataSource ds, final CreatedBy creator) {
        return StringUtil.concat(System.identityHashCode(ds), "_", Thread.currentThread().getId(), "_", Thread.currentThread().getName(), "_",
                creator.ordinal());
    }

    /**
     *
     * @param ds
     * @param creator
     * @return
     */
    static SQLTransaction getTransaction(final javax.sql.DataSource ds, final CreatedBy creator) {
        return threadTransactionMap.get(getTransactionId(ds, creator));
    }

    /**
     *
     * @param tran
     * @return
     */
    static SQLTransaction putTransaction(final SQLTransaction tran) {
        return threadTransactionMap.put(tran.id, tran);
    }

    /**
     * Execute the specified {@code Runnable} not this transaction.
     *
     * @param <E>
     * @param cmd
     * @throws E
     */
    public <E extends Exception> void runNotInMe(Try.Runnable<E> cmd) throws E {
        threadTransactionMap.remove(id);

        try {
            cmd.run();
        } finally {
            if (threadTransactionMap.put(id, this) != null) {
                throw new IllegalStateException("Another transaction is opened but not closed in 'Transaction.runNotInMe'.");
            }
        }
    }

    /**
     * Execute the specified {@code Callable} not this transaction.
     *
     * @param <R>
     * @param <E>
     * @param cmd
     * @return
     * @throws E
     */
    public <R, E extends Exception> R callNotInMe(Try.Callable<R, E> cmd) throws E {
        threadTransactionMap.remove(id);

        try {
            return cmd.call();
        } finally {
            if (threadTransactionMap.put(id, this) != null) {
                throw new IllegalStateException("Another transaction is opened but not closed in 'Transaction.callNotInMe'.");
            }
        }
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return timedId.hashCode();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SQLTransaction && timedId.equals(((SQLTransaction) obj).timedId);
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return "SQLTransaction={id=" + timedId + "}";
    }

    /**
     * The Enum CreatedBy.
     */
    enum CreatedBy {
        /**
         * Global for all.
         */
        JDBC_UTIL,

        /**
         * SQLExecutor.
         */
        SQL_EXECUTOR;
    }
}
