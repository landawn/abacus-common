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
import static com.landawn.abacus.dataSource.DataSourceConfiguration.JNDI_NAME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.LIVE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_ACTIVE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_IDLE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MIN_IDLE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.PASSWORD;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.URL;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.USER;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.VALIDATION_QUERY;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ExceptionUtil;

// TODO: Auto-generated Javadoc
/**
 * The Class HikariConnectionManager.
 *
 * @author Haiyang Li
 * @since 0.8
 */
class HikariConnectionManager extends AbstractConnectionManager {

    /** The Constant logger. */
    static final Logger logger = LoggerFactory.getLogger(HikariConnectionManager.class);

    /** The ds. */
    private final com.zaxxer.hikari.HikariDataSource ds;

    /**
     * Instantiates a new hikari connection manager.
     *
     * @param props
     */
    public HikariConnectionManager(Map<String, ?> props) {
        super(props);

        if (properties.containsKey(JNDI_NAME)) {
            throw new UnsupportedOperationException(); // TODO
        } else {
            final com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
            config.setDriverClassName(properties.get(DRIVER));
            config.setJdbcUrl(properties.get(URL));
            config.setUsername(properties.get(USER));
            config.setPassword(properties.get(PASSWORD));

            config.setMinimumIdle(Integer.valueOf(properties.get(MIN_IDLE)));
            config.setMaximumPoolSize(Integer.valueOf(properties.get(MAX_ACTIVE)));
            config.setIdleTimeout(Long.valueOf(properties.get(MAX_IDLE_TIME)));
            config.setMaxLifetime(Long.valueOf(properties.get(LIVE_TIME)));
            config.setConnectionTestQuery(properties.get(VALIDATION_QUERY));

            config.setDataSourceProperties(connectionProperties);

            ds = new com.zaxxer.hikari.HikariDataSource(config);
        }
    }

    /**
     * Gets the max active.
     *
     * @return
     */
    @Override
    public int getMaxActive() {
        return ds.getMaximumPoolSize();
    }

    /**
     * Gets the num active.
     *
     * @return
     */
    @Override
    public int getNumActive() {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the connection.
     *
     * @return
     */
    @Override
    public Connection getConnection() {
        try {
            return ds.getConnection();
        } catch (SQLException e) {
            throw new UncheckedSQLException(ExceptionUtil.getMessage(e), e);
        }
    }

    /**
     *
     * @param conn
     */
    @Override
    public void closeConnection(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new UncheckedSQLException(ExceptionUtil.getMessage(e), e);
            }
        }
    }

    /**
     *
     * @param conn
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
        if (ds != null) {
            ds.close();
        }
    }
}
