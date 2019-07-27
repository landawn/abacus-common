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

import static com.landawn.abacus.dataSource.DataSourceConfiguration.DRIVER;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.EVICT_DELAY;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.INITIAL_SIZE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.JNDI_NAME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.LIVE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_ACTIVE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_IDLE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_IDLE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_WAIT_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MIN_IDLE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.PASSWORD;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.TEST_ON_BORROW;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.TEST_ON_RETURN;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.URL;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.USER;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.VALIDATION_QUERY;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.pool.ObjectPool;
import com.landawn.abacus.pool.PoolFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Configuration;
import com.landawn.abacus.util.JdbcUtil;
import com.landawn.abacus.util.N;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class SQLConnectionManager extends AbstractConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(SQLConnectionManager.class);

    private final DataSource ds;
    private final Map<Connection, Connection> xpool = new IdentityHashMap<Connection, Connection>();
    private final ObjectPool<PoolableConnection> pool;

    private final String driver;
    private final String url;
    private final String user;
    private final String password;
    private final int initialSize;
    private final int minIdle;
    private final int maxIdle;
    private final int maxActive;
    private final long liveTime;
    private final long maxIdleTime;
    private final long maxWaitTime;
    private final int maxOpenPreparedStatementsPerConnection;
    private final String validationQuery;
    private final boolean testOnBorrow;
    private final boolean testOnReturn;

    private boolean isClosed = false;

    public SQLConnectionManager(Map<String, ?> props) {
        super(props);
        driver = properties.get(DRIVER);
        url = properties.get(URL);
        user = properties.get(USER);
        password = properties.get(PASSWORD);

        initialSize = Integer.valueOf(properties.get(INITIAL_SIZE));
        minIdle = Integer.valueOf(properties.get(MIN_IDLE));
        maxIdle = Integer.valueOf(properties.get(MAX_IDLE));
        maxActive = Integer.valueOf(properties.get(MAX_ACTIVE));
        maxWaitTime = Configuration.readTimeValue(properties.get(MAX_WAIT_TIME));
        liveTime = Configuration.readTimeValue(properties.get(LIVE_TIME));
        maxIdleTime = Configuration.readTimeValue(properties.get(MAX_IDLE_TIME));
        maxOpenPreparedStatementsPerConnection = Integer.valueOf(properties.get(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION));
        validationQuery = properties.get(VALIDATION_QUERY);
        testOnBorrow = Boolean.valueOf(properties.get(TEST_ON_BORROW));
        testOnReturn = Boolean.valueOf(properties.get(TEST_ON_RETURN));

        if (properties.containsKey(JNDI_NAME)) {
            ds = createJNDIDataSource(properties);
        } else {
            ds = new DriverManagerDataSource(driver, url, user, password, connectionProperties);
        }

        pool = PoolFactory.createObjectPool(maxActive, Integer.valueOf(properties.get(EVICT_DELAY)));

        Thread th = new Thread() {
            @Override
            public void run() {
                initPool();
            }
        };

        th.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.warn("Starting to shutdown task in SQLConnectionManager");
                try {
                    close();
                } finally {
                    logger.warn("Completed to shutdown task in SQLConnectionManager");
                }
            }
        });
    }

    @Override
    public void updateLastSQLExecutionFailureTime() {
        pool.lock();

        try {
            lastSQLExecutionFailureTime = System.currentTimeMillis();
        } finally {
            pool.unlock();
        }
    }

    @Override
    public int getMaxActive() {
        return maxActive;
    }

    @Override
    public int getNumActive() {
        return xpool.size();
    }

    @Override
    public Connection getConnection() {
        checkClose();

        PoolableConnection conn = pool.take();

        if (conn != null) {
            if (testOnBorrow && (conn.getLastSQLExecutionTime() <= lastSQLExecutionFailureTime)) {
                if (!validate(conn)) {
                    detroyConnection(conn);

                    conn = (PoolableConnection) getConnection();
                }
            }
        }

        if (conn == null) {
            conn = newConnection();
        }

        if (conn == null) {
            try {
                conn = pool.take(maxWaitTime, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                // ignore;

                if (logger.isWarnEnabled()) {
                    logger.warn(AbacusException.getErrorMsg(e));
                }
            } finally {
                if (conn == null) {
                    conn = newConnection();
                }
            }

            if (conn == null) {
                throw new RuntimeException("Can not get connection. Max active connection is " + maxActive + ". Current active connection: " + xpool.size());
            }
        }

        return conn;
    }

    @Override
    public void closeConnection(Connection conn) {
        if (conn == null) {
            return;
        }

        final PoolableConnection poolableConn = (PoolableConnection) conn;

        if (isClosed) {
            detroyConnection(poolableConn);
        }

        if (pool.contains(poolableConn)) {
            return;
        }

        try {
            synchronized (xpool) {
                if (!xpool.containsKey(poolableConn)) {
                    detroyConnection(poolableConn);
                } else if ((testOnReturn && (poolableConn.getLastSQLExecutionTime() <= lastSQLExecutionFailureTime)) && !validate(poolableConn)) {
                    detroyConnection(poolableConn);
                } else if (!pool.add(poolableConn)) {
                    detroyConnection(poolableConn);
                }
            }
        } catch (Exception e) {
            detroyConnection(poolableConn);

            if (logger.isWarnEnabled()) {
                logger.warn(AbacusException.getErrorMsg(e));
            }
        }
    }

    @Override
    public void detroyConnection(Connection conn) {
        PoolableConnection poolableConn = (PoolableConnection) conn;

        if (conn != null) {
            synchronized (xpool) {
                if (xpool.remove(conn) != null) {

                    if (logger.isWarnEnabled()) {
                        logger.warn("The " + (xpool.size() + 1) + "th connection is permanently closed for data source: " + url);
                    }
                }
            }

            try {
                poolableConn.destroy();
            } catch (Exception e) {
                // ignore;

                if (logger.isWarnEnabled()) {
                    logger.warn(AbacusException.getErrorMsg(e));
                }
            }
        }
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        clear(true);

        isClosed = true;
    }

    void clear(boolean isClose) {
        if (isClosed) {
            return;
        }

        if (xpool.size() != pool.size()) {
            logger.warn("Starting to wait connection to be returned before clear/close pool");

            int timeout = 60 * 1000;

            while (timeout > 0) {
                if (xpool.size() == pool.size()) {
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // ignore

                    if (logger.isWarnEnabled()) {
                        logger.warn(AbacusException.getErrorMsg(e));
                    }
                }

                timeout = timeout - 100;
            }

            logger.warn("Completed to wait connection to be returned before clear/close pool");
        }

        logger.warn("Starting to lock pool and close connection one by one");
        pool.lock();

        try {
            if (isClosed) {
                return;
            }

            synchronized (xpool) {
                List<Connection> list = new ArrayList<Connection>(xpool.keySet());

                for (Connection conn : list) {
                    if (conn != null) {
                        try {
                            ((PoolableConnection) conn).destroy();
                        } catch (Exception e) {
                            // ignore

                            if (logger.isWarnEnabled()) {
                                logger.warn(AbacusException.getErrorMsg(e));
                            }
                        }
                    }
                }

                xpool.clear();
            }

            if (isClose) {
                pool.close();
            } else {
                pool.clear();
            }
        } finally {
            pool.unlock();
        }

        logger.warn("Completed to lock pool and close connection one by one");
    }

    private void checkClose() {
        if (isClosed) {
            throw new IllegalStateException("The connection pool has been closed");
        }
    }

    /**
     * Method newConnection.
     * 
     * @param liveTime
     * @param maxIdleTime
     * @return PoolableConnection
     */
    private synchronized PoolableConnection newConnection() {
        synchronized (xpool) {
            if (xpool.size() >= maxActive) {
                return null;
            }

            try {
                PoolableConnection conn = null;

                if (xpool.size() < minIdle) {
                    conn = new PoolableConnection(this, ds.getConnection(), liveTime, Integer.MAX_VALUE, maxOpenPreparedStatementsPerConnection);
                } else if (xpool.size() < maxIdle) {
                    conn = new PoolableConnection(this, ds.getConnection(), liveTime, maxIdleTime, maxOpenPreparedStatementsPerConnection);
                } else {
                    conn = new PoolableConnection(this, ds.getConnection(), liveTime, 60 * 1000L, maxOpenPreparedStatementsPerConnection);
                }

                xpool.put(conn, conn);

                if (logger.isWarnEnabled()) {
                    logger.warn("The " + xpool.size() + "th of " + maxActive + " connections is created for data source: " + url);
                }

                return conn;
            } catch (SQLException e) {
                String msg = "Faied to create new connection for data source '" + url + "'." + " [Active connection number]: " + (xpool.size() + 1) + ". "
                        + AbacusException.getErrorMsg(e);
                throw new UncheckedSQLException(msg, e);
            }
        }
    }

    /**
     * Method finalize.
     * 
     * @throws Throwable
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

    /**
     * Method initPool.
     */
    private void initPool() {
        if (logger.isWarnEnabled()) {
            logger.warn("Start to initialize connection pool with url: " + url + " ...");
        }

        for (int i = 0; (i < initialSize) && (xpool.size() < initialSize); i++) {
            pool.lock();

            try {
                if (pool.isClosed()) {
                    break;
                }

                if ((i < initialSize) && (xpool.size() < initialSize)) {
                    PoolableConnection poolableConn = newConnection();

                    if (!pool.add(poolableConn)) {
                        detroyConnection(poolableConn);
                    }
                }
            } finally {
                pool.unlock();
            }
        }

        if (logger.isWarnEnabled()) {
            logger.warn("End to initialize connection pool with url: " + url + " ...");
        }
    }

    private boolean validate(Connection conn) {
        if (conn == null) {
            return false;
        }

        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            if (validationQuery != null) {
                stmt = conn.prepareStatement(validationQuery);
                rs = stmt.executeQuery();
                rs.next();
            } else {
                return !conn.isClosed();
            }
        } catch (SQLException e) {
            // ignore;

            if (logger.isWarnEnabled()) {
                logger.warn(AbacusException.getErrorMsg(e));
            }

            return false;
        } finally {
            JdbcUtil.closeQuietly(rs, stmt);
        }

        return true;
    }

    class DriverManagerDataSource extends AbstractDataSource {
        private final String url;

        private final Properties connectionProperties;

        DriverManagerDataSource(String driver, String url, String user, String password, Properties props) {
            this.url = url;
            this.connectionProperties = new Properties();

            if (props != null) {
                connectionProperties.putAll(props);
            }

            connectionProperties.put(USER, user);
            connectionProperties.put(PASSWORD, password);

            try {
                DriverManager.registerDriver((Driver) ClassUtil.forClass(driver).newInstance());
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(url, connectionProperties);
        }
    }
}
