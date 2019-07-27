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

import static com.landawn.abacus.dataSource.DataSourceConfiguration.C3P0;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DBCP;
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
import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.TypeAttrParser;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class SQLDataSource extends AbstractDataSource implements com.landawn.abacus.DataSource {
    private static final Logger logger = LoggerFactory.getLogger(SQLDataSource.class);

    private final String name;
    private final Properties<String, String> properties;
    private final ConnectionManager connectionManager;
    private final ConnectionManager readOnlyconnectionManager;

    private final String databaseProductName;
    private final String databaseProductVersion;
    private final IsolationLevel defaultIsolationLevel;
    private final int defaultConnectionIsolation;
    private final boolean queryWithReadOnlyConnectionByDefault;
    private final boolean sqlLog;
    private final boolean isPerfLog;
    private final long perfLog;

    private final SliceSelector sliceSelector;

    private Connection persistentConnection;

    private boolean isClosed = false;

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

    private void close(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {

                if (logger.isWarnEnabled()) {
                    logger.warn(AbacusException.getErrorMsg(e));
                }
            }
        }
    }

    @Override
    public Connection getConnection() {
        return connectionManager.getConnection();
    }

    @Override
    public Connection getReadOnlyConnection() {
        return readOnlyconnectionManager.getConnection();
    }

    @Override
    public SliceSelector getSliceSelector() {
        return sliceSelector;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Properties<String, String> getProperties() {
        return properties;
    }

    private ConnectionManager createConnectionManager(String provider, Map<String, ?> props) {
        if (N.isNullOrEmpty(provider)) {
            return new SQLConnectionManager(props);
        } else if (DBCP.equalsIgnoreCase(provider)) {
            return new DBCPConnectionManager(props);
        } else if (DBCP2.equalsIgnoreCase(provider)) {
            return new DBCP2ConnectionManager(props);
        } else if (C3P0.equalsIgnoreCase(provider)) {
            return new C3P0ConnectionManager(props);
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

    public String getDatabaseProductName() {
        return databaseProductName;
    }

    public String getDatabaseProductVersion() {
        return databaseProductVersion;
    }

    public int getDefaultConnectionIsolation() {
        return defaultConnectionIsolation;
    }

    public boolean isPersistentConnection(Connection conn) {
        return conn == persistentConnection;
    }

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
                    logger.warn(AbacusException.getErrorMsg(e));
                }

                try {
                    persistentConnection.close();
                } catch (SQLException e1) {
                    // ignore;

                    if (logger.isWarnEnabled()) {
                        logger.warn(AbacusException.getErrorMsg(e1));
                    }
                } finally {
                    persistentConnection = getReadOnlyConnection();
                }
            }
        }

        return persistentConnection;
    }

    public IsolationLevel getDefaultIsolationLevel() {
        return defaultIsolationLevel;
    }

    public boolean isSqlLogEnable() {
        return sqlLog;
    }

    public boolean isPerfLog() {
        return isPerfLog;
    }

    public long getPerfLog() {
        return perfLog;
    }

    public boolean isQueryWithReadOnlyConnectionByDefault() {
        return queryWithReadOnlyConnectionByDefault;
    }

    @Override
    public int getMaxActive() {
        return connectionManager.getMaxActive();
    }

    @Override
    public int getCurrentActive() {
        return connectionManager.getNumActive();
    }

    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        connectionManager.close();

        isClosed = true;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((properties == null) ? 0 : properties.hashCode());

        return result;
    }

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

    @Override
    public String toString() {
        return properties.toString();
    }
}
