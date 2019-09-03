/*
 * Copyright (C) 2019 HaiYang Li
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
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.sql.DataSource;

import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class DBCP2ConnectionManager.
 *
 * @author Haiyang Li
 * @since 0.8
 */
class DBCP2ConnectionManager extends AbstractConnectionManager {

    /** The Constant logger. */
    static final Logger logger = LoggerFactory.getLogger(DBCP2ConnectionManager.class);

    /** The ds. */
    private final DataSource ds;

    /** The bds. */
    private final org.apache.commons.dbcp2.BasicDataSource bds;

    /**
     * Instantiates a new DBCP 2 connection manager.
     *
     * @param props
     */
    public DBCP2ConnectionManager(Map<String, ?> props) {
        super(props);

        if (properties.containsKey(JNDI_NAME)) {
            ds = createJNDIDataSource(properties);
            bds = null;
        } else {
            try {
                bds = org.apache.commons.dbcp2.BasicDataSourceFactory.createDataSource(new Properties());
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }

            bds.setDriverClassName(properties.get(DRIVER));
            bds.setUrl(properties.get(URL));
            bds.setUsername(properties.get(USER));
            bds.setPassword(properties.get(PASSWORD));

            bds.setInitialSize(Integer.valueOf(properties.get(INITIAL_SIZE)));
            bds.setMinIdle(Integer.valueOf(properties.get(MIN_IDLE)));
            bds.setMaxIdle(Integer.valueOf(properties.get(MAX_IDLE)));
            bds.setMaxTotal(Integer.valueOf(properties.get(MAX_ACTIVE)));
            bds.setMaxConnLifetimeMillis(Long.valueOf(properties.get(LIVE_TIME)));
            bds.setSoftMinEvictableIdleTimeMillis(Long.valueOf(properties.get(MAX_IDLE_TIME)));
            bds.setTimeBetweenEvictionRunsMillis(Long.valueOf(properties.get(EVICT_DELAY)));
            bds.setMaxWaitMillis(Long.valueOf(properties.get(MAX_WAIT_TIME)));
            bds.setMaxOpenPreparedStatements(Integer.valueOf(properties.get(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION)));
            bds.setTestOnBorrow(Boolean.valueOf(properties.get(TEST_ON_BORROW)));
            bds.setTestOnReturn(Boolean.valueOf(properties.get(TEST_ON_RETURN)));
            bds.setValidationQuery(properties.get(VALIDATION_QUERY));

            String st = "";
            Set<Object> propNames = connectionProperties.keySet();

            for (Object propName : propNames) {
                st += ((propName + "=" + connectionProperties.getProperty(propName.toString())) + ";");
            }

            bds.setConnectionProperties(st);
            ds = bds;
        }
    }

    /**
     * Gets the max active.
     *
     * @return
     */
    @Override
    public int getMaxActive() {
        return bds.getMaxTotal();
    }

    /**
     * Gets the num active.
     *
     * @return
     */
    @Override
    public int getNumActive() {
        return bds.getNumActive();
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
        if (bds != null) {
            try {
                bds.close();
            } catch (SQLException e) {
                throw new UncheckedSQLException(ExceptionUtil.getMessage(e), e);
            }
        }
    }
}
