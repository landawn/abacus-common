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

import static com.landawn.abacus.dataSource.DataSourceConfiguration.DBCP2;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_ISOLATION;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.HIKARI_CP;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.PERF_LOG;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.PROVIDER;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.QUERY_WITH_READ_ONLY_CONNECTION_BY_DEFAULT;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.SQL_LOG;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Map;

import com.landawn.abacus.IsolationLevel;
import com.landawn.abacus.SliceSelector;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.TypeAttrParser;

// TODO: Auto-generated Javadoc
/**
 * The Class SQLDataSource.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class SQLDataSource extends AbstractDataSource implements com.landawn.abacus.DataSource {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SQLDataSource.class);

    /** The name. */
    private final String name;

    /** The properties. */
    private final Properties<String, String> properties;

    /** The connection manager. */
    private final ConnectionManager connectionManager;

    /** The read onlyconnection manager. */
    private final ConnectionManager readOnlyconnectionManager;

    /** The database product name. */
    private final String databaseProductName;

    /** The database product version. */
    private final String databaseProductVersion;

    /** The default isolation level. */
    private final IsolationLevel defaultIsolationLevel;

    /** The default connection isolation. */
    private final int defaultConnectionIsolation;

    /** The query with read only connection by default. */
    private final boolean queryWithReadOnlyConnectionByDefault;

    /** The sql log. */
    private final boolean sqlLog;

    /** The is perf log. */
    private final boolean isPerfLog;

    /** The perf log. */
    private final long perfLog;

    /** The slice selector. */
    private final SliceSelector sliceSelector;

    /** The persistent connection. */
    private Connection persistentConnection;

    /** The is closed. */
    private boolean isClosed = false;

    /**
     * Instantiates a new SQL data source.
     *
     * @param dsConfig
     */
    public SQLDataSource(DataSourceConfiguration dsConfig) {
        properties = new Properties<>();

        for (String attrName : dsConfig.getAttrNames()) {
            properties.put(attrName, dsConfig.getAttribute(attrName));
        }

        properties.putAll(dsConfig.getConnectionProps());

        name = properties.get(DataSourceConfiguration.NAME).intern();

        queryWithReadOnlyConnectionByDefault = Boolean.valueOf(properties.get(QUERY_WITH_READ_ONLY_CONNECTION_BY_DEFAULT));

        sqlLog = Boolean.valueOf(properties.get(SQL_LOG));

        String attr = properties.get(PERF_LOG);
        isPerfLog = (N.notNullOrEmpty(attr)) && (Long.valueOf(attr) >= 0);
        perfLog = isPerfLog ? Long.valueOf(attr) : Long.MAX_VALUE;

        String provider = properties.get(PROVIDER);
        connectionManager = createConnectionManager(provider, dsConfig.getConnectionProps());

        if (dsConfig.getReadOnlyConnectionProps() == null) {
            readOnlyconnectionManager = connectionManager;
        } else {
            readOnlyconnectionManager = createConnectionManager(provider, dsConfig.getReadOnlyConnectionProps());
        }

        Connection conn = null;

        try {
            conn = connectionManager.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();

            databaseProductName = metaData.getDatabaseProductName();
            databaseProductVersion = metaData.getDatabaseProductVersion();
            defaultConnectionIsolation = conn.getTransactionIsolation();
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            close(conn);
        }

        defaultIsolationLevel = properties.containsKey(DEFAULT_ISOLATION) ? IsolationLevel.valueOf(properties.get(DEFAULT_ISOLATION)) : IsolationLevel.DEFAULT;

        attr = properties.get(DataSourceConfiguration.SLICE_SELECTOR);

        if (attr == null) {
            sliceSelector = new NonSliceSelector();
        } else {
            sliceSelector = (SliceSelector) TypeAttrParser.newInstance(null, attr);
        }
    }

    /**
     * Instantiates a new SQL data source.
     *
     * @param props
     */
    public SQLDataSource(Map<String, ?> props) {
        properties = new Properties<>();

        for (Map.Entry<String, ?> entry : props.entrySet()) {
            properties.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
        }

        if (properties.containsKey(DataSourceConfiguration.NAME)) {
            name = properties.get(DataSourceConfiguration.NAME).intern();
        } else {
            name = null;
        }

        queryWithReadOnlyConnectionByDefault = Boolean.valueOf(properties.get(QUERY_WITH_READ_ONLY_CONNECTION_BY_DEFAULT));

        sqlLog = Boolean.valueOf(properties.get(SQL_LOG));

        String attr = properties.get(PERF_LOG);
        isPerfLog = (N.notNullOrEmpty(attr)) && (Long.valueOf(attr) >= 0);
        perfLog = isPerfLog ? Long.valueOf(attr) : Long.MAX_VALUE;

        String provider = properties.get(PROVIDER);
        connectionManager = createConnectionManager(provider, properties);
        readOnlyconnectionManager = connectionManager;

        Connection conn = null;

        try {
            conn = connectionManager.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();

            databaseProductName = metaData.getDatabaseProductName();
            databaseProductVersion = metaData.getDatabaseProductVersion();
            defaultConnectionIsolation = conn.getTransactionIsolation();
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        } finally {
            close(conn);
        }

        defaultIsolationLevel = properties.containsKey(DEFAULT_ISOLATION) ? IsolationLevel.valueOf(properties.get(DEFAULT_ISOLATION)) : IsolationLevel.DEFAULT;

        attr = properties.get(DataSourceConfiguration.SLICE_SELECTOR);

        if (attr == null) {
            sliceSelector = new NonSliceSelector();
        } else {
            sliceSelector = (SliceSelector) TypeAttrParser.newInstance(null, attr);
        }
    }

    /**
     *
     * @param conn
     */
    private void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getMessage(e));
                }
            }
        }
    }

    /**
     * Gets the connection.
     *
     * @return
     */
    @Override
    public Connection getConnection() {
        return connectionManager.getConnection();
    }

    /**
     * Gets the read only connection.
     *
     * @return
     */
    @Override
    public Connection getReadOnlyConnection() {
        return readOnlyconnectionManager.getConnection();
    }

    /**
     * Gets the slice selector.
     *
     * @return
     */
    @Override
    public SliceSelector getSliceSelector() {
        return sliceSelector;
    }

    /**
     * Gets the name.
     *
     * @return
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Gets the properties.
     *
     * @return
     */
    @Override
    public Properties<String, String> getProperties() {
        return properties;
    }

    /**
     * Creates the connection manager.
     *
     * @param provider
     * @param props
     * @return
     */
    private ConnectionManager createConnectionManager(String provider, Map<String, ?> props) {
        if (N.isNullOrEmpty(provider)) {
            return new SQLConnectionManager(props);
        } else if (DBCP2.equalsIgnoreCase(provider)) {
            return new DBCP2ConnectionManager(props);
        } else if (HIKARI_CP.equalsIgnoreCase(provider)) {
            return new HikariConnectionManager(props);
        } else {
            try {
                return (ConnectionManager) ClassUtil.forClass(provider).getConstructor(Map.class).newInstance(props);
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    /**
     * Gets the database product name.
     *
     * @return
     */
    public String getDatabaseProductName() {
        return databaseProductName;
    }

    /**
     * Gets the database product version.
     *
     * @return
     */
    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    /**
     * Gets the default connection isolation.
     *
     * @return
     */
    public int getDefaultConnectionIsolation() {
        return defaultConnectionIsolation;
    }

    /**
     * Checks if is persistent connection.
     *
     * @param conn
     * @return true, if is persistent connection
     */
    public boolean isPersistentConnection(Connection conn) {
        return conn == persistentConnection;
    }

    /**
     * Gets the persistent connection.
     *
     * @return
     */
    public synchronized Connection getPersistentConnection() {
        if (persistentConnection == null) {
            persistentConnection = getReadOnlyConnection();
        } else {
            try {
                if (persistentConnection.isClosed()) {
                    persistentConnection.close();
                    persistentConnection = getReadOnlyConnection();
                }
            } catch (SQLException e) {
                // ignore;

                if (logger.isWarnEnabled()) {
                    logger.warn(ExceptionUtil.getMessage(e));
                }

                try {
                    persistentConnection.close();
                } catch (SQLException e1) {
                    // ignore;

                    if (logger.isWarnEnabled()) {
                        logger.warn(ExceptionUtil.getMessage(e1));
                    }
                } finally {
                    persistentConnection = getReadOnlyConnection();
                }
            }
        }

        return persistentConnection;
    }

    /**
     * Gets the default isolation level.
     *
     * @return
     */
    public IsolationLevel getDefaultIsolationLevel() {
        return defaultIsolationLevel;
    }

    /**
     * Checks if is sql log enable.
     *
     * @return true, if is sql log enable
     */
    public boolean isSqlLogEnable() {
        return sqlLog;
    }

    /**
     * Checks if is perf log.
     *
     * @return true, if is perf log
     */
    public boolean isPerfLog() {
        return isPerfLog;
    }

    /**
     * Gets the perf log.
     *
     * @return
     */
    public long getPerfLog() {
        return perfLog;
    }

    /**
     * Checks if is query with read only connection by default.
     *
     * @return true, if is query with read only connection by default
     */
    public boolean isQueryWithReadOnlyConnectionByDefault() {
        return queryWithReadOnlyConnectionByDefault;
    }

    /**
     * Gets the max active.
     *
     * @return
     */
    @Override
    public int getMaxActive() {
        return connectionManager.getMaxActive();
    }

    /**
     * Gets the current active.
     *
     * @return
     */
    @Override
    public int getCurrentActive() {
        return connectionManager.getNumActive();
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        connectionManager.close();

        isClosed = true;
    }

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    @Override
    public boolean isClosed() {
        return isClosed;
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((properties == null) ? 0 : properties.hashCode());

        return result;
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

        if (obj instanceof SQLDataSource) {
            SQLDataSource other = (SQLDataSource) obj;

            return N.equals(properties, other.properties);
        }

        return false;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return properties.toString();
    }
}
