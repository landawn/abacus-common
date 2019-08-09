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
import static com.landawn.abacus.dataSource.DataSourceConfiguration.INITIAL_SIZE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.JNDI_NAME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_ACTIVE;
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

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.sql.DataSource;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class C3P0ConnectionManager.
 *
 * @author Haiyang Li
 * @since 0.8
 */
class C3P0ConnectionManager extends AbstractConnectionManager {

    /** The Constant logger. */
    static final Logger logger = LoggerFactory.getLogger(C3P0ConnectionManager.class);

    /** The ds. */
    private final DataSource ds;

    /** The cpds. */
    private final com.mchange.v2.c3p0.ComboPooledDataSource cpds;

    /**
     * Instantiates a new c 3 P 0 connection manager.
     *
     * @param props the props
     */
    public C3P0ConnectionManager(Map<String, ?> props) {
        super(props);

        if (properties.containsKey(JNDI_NAME)) {
            try {
                ds = com.mchange.v2.c3p0.DataSources.pooledDataSource(createJNDIDataSource(properties));
            } catch (SQLException e) {
                throw new UncheckedSQLException(e);
            }

            cpds = null;
        } else {
            cpds = new com.mchange.v2.c3p0.ComboPooledDataSource();
            cpds.setProperties(connectionProperties);

            try {
                cpds.setDriverClass(properties.get(DRIVER));
            } catch (PropertyVetoException e) {
                throw N.toRuntimeException(e);
            }

            cpds.setJdbcUrl(properties.get(URL));
            cpds.setUser(properties.get(USER));
            cpds.setPassword(properties.get(PASSWORD));

            cpds.setInitialPoolSize(Integer.valueOf(properties.get(INITIAL_SIZE)));
            cpds.setMinPoolSize(Integer.valueOf(properties.get(MIN_IDLE)));
            cpds.setMaxPoolSize(Integer.valueOf(properties.get(MAX_ACTIVE)));
            cpds.setMaxStatementsPerConnection(Integer.valueOf(properties.get(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION)));
            cpds.setMaxIdleTime(Integer.valueOf(properties.get(MAX_IDLE_TIME)));
            cpds.setCheckoutTimeout(Integer.valueOf(properties.get(MAX_WAIT_TIME)));
            cpds.setPreferredTestQuery(properties.get(VALIDATION_QUERY));
            cpds.setTestConnectionOnCheckout(Boolean.valueOf(properties.get(TEST_ON_BORROW)));
            cpds.setTestConnectionOnCheckin(Boolean.valueOf(properties.get(TEST_ON_RETURN)));

            ds = cpds;
        }
    }

    /**
     * Gets the max active.
     *
     * @return the max active
     */
    @Override
    public int getMaxActive() {
        if (cpds == null) {
            throw new UnsupportedOperationException();
        }

        return cpds.getMaxPoolSize();
    }

    /**
     * Gets the num active.
     *
     * @return the num active
     */
    @Override
    public int getNumActive() {
        if (cpds == null) {
            throw new UnsupportedOperationException();
        }

        try {
            return cpds.getNumConnections();
        } catch (SQLException e) {
            throw new UncheckedSQLException(AbacusException.getErrorMsg(e), e);
        }
    }

    /**
     * Gets the connection.
     *
     * @return the connection
     */
    @Override
    public Connection getConnection() {
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            throw new UncheckedSQLException(AbacusException.getErrorMsg(e), e);
        }
    }

    /**
     * Close connection.
     *
     * @param conn the conn
     */
    @Override
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new UncheckedSQLException(AbacusException.getErrorMsg(e), e);
            }
        }
    }

    /**
     * Detroy connection.
     *
     * @param conn the conn
     */
    @Override
    public void detroyConnection(Connection conn) {
        closeConnection(conn);
    }

    /**
     * Close.
     */
    @Override
    public void close() {
        if (cpds != null) {
            cpds.close();
        }
    }
}
