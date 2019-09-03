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

package com.landawn.abacus.dataSource;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.pool.AbstractPoolable;
import com.landawn.abacus.pool.KeyedObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class PoolableConnection.
 *
 * @author Haiyang Li
 * @since 0.8
 */
class PoolableConnection extends AbstractPoolable implements Connection {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(PoolableConnection.class);

    /** The Constant CACHED_SQL_LENGTH. */
    private static final int CACHED_SQL_LENGTH = 4096;

    /** The Constant EVICTY_DELAY_FOR_STATEMENT_POOL. */
    private static final int EVICTY_DELAY_FOR_STATEMENT_POOL = 60 * 1000;

    /** The cached statement pool. */
    private final KeyedObjectPool<CachedStatmentKey, PoolablePreparedStatement> cachedStatementPool;

    /** The conn manager. */
    private final ConnectionManager connManager;

    /** The internal conn. */
    private final Connection internalConn;

    /** The last SQL execution time. */
    private volatile long lastSQLExecutionTime;

    /** The is closed. */
    private boolean isClosed = false;

    /**
     * Instantiates a new poolable connection.
     *
     * @param dataSource
     * @param conn
     */
    public PoolableConnection(ConnectionManager dataSource, java.sql.Connection conn) {
        this(dataSource, conn, DataSourceConfiguration.DEFAULT_LIVE_TIME, DataSourceConfiguration.DEFAULT_MAX_IDLE_TIME,
                DataSourceConfiguration.DEFAULT_MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION);
    }

    /**
     * Instantiates a new poolable connection.
     *
     * @param connManager
     * @param conn
     * @param liveTime
     * @param maxIdleTime
     * @param cachedStatementSize
     */
    public PoolableConnection(ConnectionManager connManager, java.sql.Connection conn, long liveTime, long maxIdleTime, int cachedStatementSize) {
        super(liveTime, maxIdleTime);

        cachedStatementPool = PoolFactory.createKeyedObjectPool(cachedStatementSize, EVICTY_DELAY_FOR_STATEMENT_POOL);

        this.connManager = connManager;

        this.internalConn = conn;
    }

    /**
     * Gets the connection manager.
     *
     * @return
     */
    ConnectionManager getConnectionManager() {
        return connManager;
    }

    /**
     * Cache prepared statement.
     *
     * @param stmt
     */
    void cachePreparedStatement(PoolablePreparedStatement stmt) {
        if (stmt == null) {
            return;
        } else if (stmt.getId() == null) {
            stmt.destroy();
        } else {
            CachedStatmentKey id = stmt.getId();

            try {
                stmt.reset();

                synchronized (cachedStatementPool) {
                    final PoolablePreparedStatement cached = cachedStatementPool.get(id);
                    if (cached == null) {
                        if (!cachedStatementPool.put(id, stmt)) {
                            stmt.destroy();
                        }
                    } else if (cached != stmt) {
                        stmt.destroy();
                    }
                }
            } catch (SQLException e) {
                stmt.destroy();

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getMessage(e));
                }
            }
        }
    }

    //    void removePreparedStatementFromCache(PoolablePreparedStatement stmt) throws SQLException {
    //        if ((stmt != null) && (stmt.getId() != null)) {
    //            // DO NOT synchronized(cachedStatementPool) because it may cause dead lock.
    //
    //            /*
    //            JNI global references: 294
    //            
    //            
    //            Found one Java-level deadlock:
    //            =============================
    //            "Thread-37":
    //            waiting to lock monitor 0x000000006374ffe8 (object 0x000000078a8ebc70, a com.landawn.abacus.pool.GenericKeyedObjectPool),
    //            which is held by "pool-165-thread-25"
    //            "pool-165-thread-25":
    //            waiting for ownable synchronizer 0x000000078a8ebd38, (a java.util.concurrent.locks.ReentrantLock$NonfairSync),
    //            which is held by "Thread-37"
    //            
    //            Java stack information for the threads listed above:
    //            ===================================================
    //            "Thread-37":
    //            at com.landawn.abacus.core.sql.dataSource.PoolableConnection.removePreparedStatementFromCache(PoolableConnection.java:106)
    //            - waiting to lock <0x000000078a8ebc70> (a com.landawn.abacus.pool.GenericKeyedObjectPool)
    //            at com.landawn.abacus.core.sql.dataSource.PoolablePreparedStatement.destroy(PoolablePreparedStatement.java:89)
    //            at com.landawn.abacus.pool.AbstractPool.destroyObject(AbstractPool.java:327)
    //            at com.landawn.abacus.pool.GenericKeyedObjectPool.destroyObject(GenericKeyedObjectPool.java:248)
    //            at com.landawn.abacus.pool.AbstractPool.destroyObject(AbstractPool.java:340)
    //            at com.landawn.abacus.pool.AbstractPool.clear(AbstractPool.java:238)
    //            at com.landawn.abacus.pool.AbstractPool.close(AbstractPool.java:257)
    //            at com.landawn.abacus.core.sql.dataSource.PoolableConnection.destroy(PoolableConnection.java:150)
    //            at com.landawn.abacus.core.sql.dataSource.SQLConnectionManager.clear(SQLConnectionManager.java:302)
    //            - locked <0x00000007840e1158> (a java.util.IdentityHashMap)
    //            at com.landawn.abacus.core.sql.dataSource.SQLConnectionManager$2.run(SQLConnectionManager.java:119)
    //            "pool-165-thread-25":
    //            at sun.misc.Unsafe.park(Native Method)
    //            - parking to wait for  <0x000000078a8ebd38> (a java.util.concurrent.locks.ReentrantLock$NonfairSync)
    //            at java.util.concurrent.locks.LockSupport.park(LockSupport.java:186)
    //            at java.util.concurrent.locks.AbstractQueuedSynchronizer.parkAndCheckInterrupt(AbstractQueuedSynchronizer.java:834)
    //            at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquireQueued(AbstractQueuedSynchronizer.java:867)
    //            at java.util.concurrent.locks.AbstractQueuedSynchronizer.acquire(AbstractQueuedSynchronizer.java:1197)
    //            at java.util.concurrent.locks.ReentrantLock$NonfairSync.lock(ReentrantLock.java:214)
    //            at java.util.concurrent.locks.ReentrantLock.lock(ReentrantLock.java:290)
    //            at com.landawn.abacus.pool.GenericKeyedObjectPool.containsKey(GenericKeyedObjectPool.java:194)
    //            at com.landawn.abacus.core.sql.dataSource.PoolableConnection.cachePreparedStatement(PoolableConnection.java:85)
    //            - locked <0x000000078a8ebc70> (a com.landawn.abacus.pool.GenericKeyedObjectPool)
    //            at com.landawn.abacus.core.sql.dataSource.PoolablePreparedStatement.close(PoolablePreparedStatement.java:74)
    //            at com.landawn.abacus.util.JdbcUtil.closeQuietly(JdbcUtil.java:636)
    //            at com.landawn.abacus.util.SQLExecutor.closeQuietly(SQLExecutor.java:3295)
    //            at com.landawn.abacus.util.SQLExecutor.query(SQLExecutor.java:1841)
    //            at com.landawn.abacus.util.SQLExecutor.query(SQLExecutor.java:1806)
    //            at com.landawn.abacus.util.SQLExecutor.query(SQLExecutor.java:1757)
    //            at com.landawn.abacus.util.SQLExecutor.query(SQLExecutor.java:1722)
    //            at com.landawn.abacus.util.PropertiesUtil$2.run(PropertiesUtil.java:195)
    //            - locked <0x00000007849489a0> (a java.util.concurrent.ConcurrentHashMap)
    //            at java.util.concurrent.Executors$RunnableAdapter.call(Executors.java:471)
    //            at java.util.concurrent.FutureTask.runAndReset(FutureTask.java:304)
    //            at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.access$301(ScheduledThreadPoolExecutor.java:178)
    //            at java.util.concurrent.ScheduledThreadPoolExecutor$ScheduledFutureTask.run(ScheduledThreadPoolExecutor.java:293)
    //            at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1145)
    //            at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:615)
    //            at java.lang.Thread.run(Thread.java:745)
    //            
    //            Found 1 deadlock.            
    //             */
    //            final PoolablePreparedStatement tmp = cachedStatementPool.remove(stmt.getId());
    //
    //            if (tmp == stmt || tmp == null) {
    //                // do nothing.
    //            } else {
    //                tmp.close();
    //            }
    //        }
    //    }

    /**
     * Update last SQL execution time.
     *
     * @param isOk
     */
    synchronized void updateLastSQLExecutionTime(boolean isOk) {
        lastSQLExecutionTime = System.currentTimeMillis();

        if (!isOk) {
            connManager.updateLastSQLExecutionFailureTime();
        }
    }

    /**
     * Gets the last SQL execution time.
     *
     * @return
     */
    long getLastSQLExecutionTime() {
        return lastSQLExecutionTime;
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#close()
     */
    @Override
    public void close() throws SQLException {
        if (connManager == null) {
            destroy();
        } else {
            connManager.closeConnection(this);
        }
    }

    /**
     * To release resourse of the <tt>AbstractPoolable</tt>.
     * 
     * @see com.landawn.abacus.pool.Poolable#destroy()
     */
    @Override
    public void destroy() {
        if (!isClosed) {
            isClosed = true;

            cachedStatementPool.close();

            try {
                internalConn.close();
            } catch (SQLException e) {
                // ignore;

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getMessage(e));
                }
            } finally {
                if (connManager != null) {
                    connManager.detroyConnection(this);
                }
            }
        }
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#commit()
     */
    @Override
    public void commit() throws SQLException {
        internalConn.commit();
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        internalConn.clearWarnings();
    }

    /**
     *
     * @return Statement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#createStatement()
     */
    @Override
    public Statement createStatement() throws SQLException {
        // return new NativeStatement(internalConn.createStatement(), this);
        return internalConn.createStatement();
    }

    /**
     *
     * @param resultSetType
     * @param resultSetConcurrency
     * @return Statement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#createStatement(int, int)
     */
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        // return new
        // NativeStatement(internalConn.createStatement(resultSetType,
        // resultSetConcurrency), this);
        return internalConn.createStatement(resultSetType, resultSetConcurrency);
    }

    /**
     *
     * @param resultSetType
     * @param resultSetConcurrency
     * @param resultSetHoldability
     * @return Statement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#createStatement(int, int, int)
     */
    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        // return new
        // NativeStatement(internalConn.createStatement(resultSetType,
        // resultSetConcurrency,
        // resultSetHoldability), this);
        return internalConn.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /**
     *
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getAutoCommit()
     */
    @Override
    public boolean getAutoCommit() throws SQLException {
        return internalConn.getAutoCommit();
    }

    /**
     *
     * @return String
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getCatalog()
     */
    @Override
    public String getCatalog() throws SQLException {
        return internalConn.getCatalog();
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getHoldability()
     */
    @Override
    public int getHoldability() throws SQLException {
        return internalConn.getHoldability();
    }

    /**
     *
     * @return DatabaseMetaData
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getMetaData()
     */
    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        return internalConn.getMetaData();
    }

    /**
     *
     * @return int
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getTransactionIsolation()
     */
    @Override
    public int getTransactionIsolation() throws SQLException {
        return internalConn.getTransactionIsolation();
    }

    /**
     *
     * @return Map<String,Class<?>>
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getTypeMap()
     */
    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        return internalConn.getTypeMap();
    }

    /**
     *
     * @return SQLWarning
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        return internalConn.getWarnings();
    }

    /**
     *
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#isClosed()
     */
    @Override
    public boolean isClosed() throws SQLException {
        if (!isClosed) {
            try {
                if (internalConn.isClosed()) {
                    destroy();
                }
            } catch (SQLException e) {
                // ignore
                destroy();

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getMessage(e));
                }
            }
        }

        return isClosed;
    }

    /**
     *
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#isReadOnly()
     */
    @Override
    public boolean isReadOnly() throws SQLException {
        return internalConn.isReadOnly();
    }

    /**
     *
     * @param sql
     * @return String
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#nativeSQL(String)
     */
    @Override
    public String nativeSQL(String sql) throws SQLException {
        return internalConn.nativeSQL(sql);
    }

    /**
     *
     * @param sql
     * @return CallableStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareCall(String)
     */
    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        // return new NativeCallableStatement(internalConn.prepareCall(sql),
        // this);
        return internalConn.prepareCall(sql);
    }

    /**
     *
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @return CallableStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareCall(String, int, int)
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        // return new NativeCallableStatement(internalConn.prepareCall(sql,
        // resultSetType, resultSetConcurrency), this);
        return internalConn.prepareCall(sql, resultSetType, resultSetConcurrency);
    }

    /**
     *
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @param resultSetHoldability
     * @return CallableStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareCall(String, int, int, int)
     */
    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        // return new NativeCallableStatement(internalConn.prepareCall(sql,
        // resultSetType, resultSetConcurrency,
        // resultSetHoldability), this);
        return internalConn.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /**
     *
     * @param sql
     * @return PreparedStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareStatement(String)
     */
    @Override
    public PoolablePreparedStatement prepareStatement(String sql) throws SQLException {
        return prepareStatement(sql, -1, -1, -1, -1);
    }

    /**
     *
     * @param sql
     * @param autoGeneratedKeys
     * @return PreparedStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareStatement(String, int)
     */
    @Override
    public PoolablePreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return prepareStatement(sql, autoGeneratedKeys, -1, -1, -1);
    }

    /**
     *
     * @param sql
     * @param columnIndexes
     * @return PreparedStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareStatement(String, int[])
     */
    @Override
    public PoolablePreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return new PoolablePreparedStatement(internalConn.prepareStatement(sql, columnIndexes), this, null);
    }

    /**
     *
     * @param sql
     * @param columnNames
     * @return PreparedStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareStatement(String, String[])
     */
    @Override
    public PoolablePreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return new PoolablePreparedStatement(internalConn.prepareStatement(sql, columnNames), this, null);
    }

    /**
     *
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @return PreparedStatement
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#prepareStatement(String, int, int)
     */
    @Override
    public PoolablePreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        return prepareStatement(sql, -1, resultSetType, resultSetConcurrency, -1);
    }

    /**
     *
     * @param sql
     * @param resultSetType
     * @param resultSetConcurrency
     * @param resultSetHoldability
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public PoolablePreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        return prepareStatement(sql, -1, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    /**
     *
     * @param sql
     * @param autoGeneratedKeys
     * @param resultSetType
     * @param resultSetConcurrency
     * @param resultSetHoldability
     * @return
     * @throws SQLException the SQL exception
     */
    protected PoolablePreparedStatement prepareStatement(String sql, int autoGeneratedKeys, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException {
        PoolablePreparedStatement stmt = null;
        CachedStatmentKey id = null;

        if (sql.length() < CACHED_SQL_LENGTH) {
            id = new CachedStatmentKey(sql, autoGeneratedKeys, resultSetType, resultSetConcurrency, resultSetHoldability);

            synchronized (cachedStatementPool) {
                stmt = cachedStatementPool.remove(id);
            }
        }

        if (stmt == null || stmt.isClosed()) {
            if ((resultSetType != -1) && (resultSetConcurrency != -1) && (resultSetHoldability != -1)) {
                stmt = new PoolablePreparedStatement(internalConn.prepareStatement(sql, resultSetType, resultSetConcurrency, resultSetHoldability), this, id);
            } else if ((resultSetType != -1) && (resultSetConcurrency != -1)) {
                stmt = new PoolablePreparedStatement(internalConn.prepareStatement(sql, resultSetType, resultSetConcurrency), this, id);
            } else {
                if (autoGeneratedKeys != -1) {
                    stmt = new PoolablePreparedStatement(internalConn.prepareStatement(sql, autoGeneratedKeys), this, id);
                } else {
                    stmt = new PoolablePreparedStatement(internalConn.prepareStatement(sql), this, id);
                }
            }
        }

        return stmt;
    }

    /**
     *
     * @param savepoint
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#releaseSavepoint(Savepoint)
     */
    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        internalConn.releaseSavepoint(savepoint);
    }

    /**
     *
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#rollback()
     */
    @Override
    public void rollback() throws SQLException {
        internalConn.rollback();
    }

    /**
     *
     * @param savepoint
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#rollback(Savepoint)
     */
    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        internalConn.rollback(savepoint);
    }

    /**
     *
     * @param autoCommit the new auto commit
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setAutoCommit(boolean)
     */
    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        internalConn.setAutoCommit(autoCommit);
    }

    /**
     *
     * @param catalog the new catalog
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setCatalog(String)
     */
    @Override
    public void setCatalog(String catalog) throws SQLException {
        internalConn.setCatalog(catalog);
    }

    /**
     *
     * @param holdability the new holdability
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setHoldability(int)
     */
    @Override
    public void setHoldability(int holdability) throws SQLException {
        internalConn.setHoldability(holdability);
    }

    /**
     *
     * @param readOnly the new read only
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setReadOnly(boolean)
     */
    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        internalConn.setReadOnly(readOnly);
    }

    /**
     *
     * @return Savepoint
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setSavepoint()
     */
    @Override
    public Savepoint setSavepoint() throws SQLException {
        return internalConn.setSavepoint();
    }

    /**
     *
     * @param name
     * @return Savepoint
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setSavepoint(String)
     */
    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        return internalConn.setSavepoint(name);
    }

    /**
     *
     * @param level the new transaction isolation
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setTransactionIsolation(int)
     */
    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        internalConn.setTransactionIsolation(level);
    }

    /**
     *
     * @param arg0
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#setTypeMap(Map<String,Class<?>>)
     */
    @Override
    public void setTypeMap(Map<String, Class<?>> arg0) throws SQLException {
        internalConn.setTypeMap(arg0);
    }

    /**
     *
     * @param typeName
     * @param elements
     * @return Array
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#createArrayOf(String, Object[])
     */
    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        return internalConn.createArrayOf(typeName, elements);
    }

    /**
     *
     * @return Blob
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#createBlob()
     */
    @Override
    public Blob createBlob() throws SQLException {
        return internalConn.createBlob();
    }

    /**
     *
     * @return Clob
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#createClob()
     */
    @Override
    public Clob createClob() throws SQLException {
        return internalConn.createClob();
    }

    /**
     *
     * @return NClob
     * @throws SQLException the SQL exception
     * @see java.sql.Conn#createNClob()
     */
    @Override
    public NClob createNClob() throws SQLException {
        return internalConn.createNClob();
    }

    /**
     *
     * @return SQLXML
     * @throws SQLException the SQL exception
     * @see java.sql.Conn#createSQLXML()
     */
    @Override
    public SQLXML createSQLXML() throws SQLException {
        return internalConn.createSQLXML();
    }

    /**
     *
     * @param typeName
     * @param attributes
     * @return Struct
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#createStruct(String, Object[])
     */
    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        return internalConn.createStruct(typeName, attributes);
    }

    /**
     *
     * @return Properties
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getClientInfo()
     */
    @Override
    public Properties getClientInfo() throws SQLException {
        return internalConn.getClientInfo();
    }

    /**
     *
     * @param name
     * @return String
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#getClientInfo(String)
     */
    @Override
    public String getClientInfo(String name) throws SQLException {
        return internalConn.getClientInfo(name);
    }

    /**
     *
     * @param timeout
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Connection#isValid(int)
     */
    @Override
    public boolean isValid(int timeout) throws SQLException {
        return internalConn.isValid(timeout);
    }

    /**
     *
     * @param properties the new client info
     * @throws SQLClientInfoException the SQL client info exception
     * @see java.sql.Connection#setClientInfo(Properties)
     */
    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        internalConn.setClientInfo(properties);
    }

    /**
     *
     * @param name
     * @param value
     * @throws SQLClientInfoException the SQL client info exception
     * @see java.sql.Connection#setClientInfo(String, String)
     */
    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {
        internalConn.setClientInfo(name, value);
    }

    /**
     *
     * @param iface
     * @return boolean
     * @throws SQLException the SQL exception
     * @see java.sql.Wrapper#isWrapperFor(Class<?>)
     */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return internalConn.isWrapperFor(iface);
    }

    /**
     *
     * @param <T>
     * @param iface
     * @return T
     * @throws SQLException the SQL exception
     * @see java.sql.Wrapper#unwrap(Class<T>)
     */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return internalConn.unwrap(iface);
    }

    /**
     * 
     * @return String
     */
    @Override
    public String toString() {
        return internalConn.toString();
    }

    /**
     * 
     * @return int
     */
    @Override
    public int hashCode() {
        return internalConn.hashCode();
    }

    /**
     *
     * @param obj
     * @return boolean
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof PoolableConnection && ((PoolableConnection) obj).internalConn.equals(internalConn));
    }

    /**
     *
     * @param executor
     * @throws SQLException the SQL exception
     */
    @Override
    public void abort(Executor executor) throws SQLException {
        internalConn.abort(executor);
    }

    /**
     * Gets the network timeout.
     *
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public int getNetworkTimeout() throws SQLException {
        return internalConn.getNetworkTimeout();
    }

    /**
     * Gets the schema.
     *
     * @return
     * @throws SQLException the SQL exception
     */
    @Override
    public String getSchema() throws SQLException {
        return internalConn.getSchema();
    }

    /**
     * Sets the network timeout.
     *
     * @param executor
     * @param milliseconds
     * @throws SQLException the SQL exception
     */
    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        internalConn.setNetworkTimeout(executor, milliseconds);
    }

    /**
     * Sets the schema.
     *
     * @param schema the new schema
     * @throws SQLException the SQL exception
     */
    @Override
    public void setSchema(String schema) throws SQLException {
        internalConn.setSchema(schema);
    }

    /**
     * The Class CachedStatmentKey.
     *
     * @author Haiyang Li
     * @version $Revision: 0.8 $
     */
    protected static class CachedStatmentKey {

        /** The sql. */
        private final String sql;

        /** The auto generated keys. */
        private final int autoGeneratedKeys;

        /** The result set type. */
        private final int resultSetType;

        /** The result set concurrency. */
        private final int resultSetConcurrency;

        /** The result set holdability. */
        private final int resultSetHoldability;

        /** The h. */
        private int h;

        /**
         * Instantiates a new cached statment key.
         *
         * @param sql
         * @param autoGeneratedKeys
         * @param resultSetType
         * @param resultSetConcurrency
         * @param resultSetHoldability
         */
        public CachedStatmentKey(String sql, int autoGeneratedKeys, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
            this.sql = sql;
            this.autoGeneratedKeys = autoGeneratedKeys;
            this.resultSetType = resultSetType;
            this.resultSetConcurrency = resultSetConcurrency;
            this.resultSetHoldability = resultSetHoldability;
        }

        /**
         *
         * @return
         */
        @Override
        public int hashCode() {
            if (h == 0) {
                h = 17;
                h = (h * 31) + sql.hashCode();
                h = (h * 31) + autoGeneratedKeys;
                h = (h * 31) + resultSetType;
                h = (h * 31) + resultSetConcurrency;
                h = (h * 31) + resultSetHoldability;
            }

            return h;
        }

        /**
         *
         * @param obj
         * @return true, if successful
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof CachedStatmentKey) {
                CachedStatmentKey other = (CachedStatmentKey) obj;

                return N.equals(sql, other.sql) && (autoGeneratedKeys == other.autoGeneratedKeys) && (resultSetType == other.resultSetType)
                        && (resultSetConcurrency == other.resultSetConcurrency) && (resultSetHoldability == other.resultSetHoldability);
            }

            return false;
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return sql;
        }
    }
}
