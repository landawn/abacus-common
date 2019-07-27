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

import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_EVICT_DELAY;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_INITIAL_SIZE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_LIVE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_MAX_ACTIVE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_MAX_IDLE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_MAX_IDLE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_MAX_WAIT;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_MIN_IDLE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_TEST_ON_BORROW;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_TEST_ON_RETURN;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DEFAULT_VALIDATION_QUERY;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.DRIVER;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.EVICT_DELAY;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.INITIAL_SIZE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.JNDI_CONTEXT_FACTORY;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.JNDI_NAME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.JNDI_PROVIDER_URL;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.LIVE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_ACTIVE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_IDLE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_IDLE_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MAX_WAIT_TIME;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.MIN_IDLE;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.PASSWORD;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.PERF_LOG;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.SQL_LOG;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.TEST_ON_BORROW;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.TEST_ON_RETURN;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.URL;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.USER;
import static com.landawn.abacus.dataSource.DataSourceConfiguration.VALIDATION_QUERY;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.N;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
abstract class AbstractConnectionManager implements ConnectionManager {
    static final Logger logger = LoggerFactory.getLogger(AbstractConnectionManager.class);

    protected final Map<String, String> properties;
    protected final Properties connectionProperties;
    protected volatile long lastSQLExecutionFailureTime;

    public AbstractConnectionManager(Map<String, ?> props) {
        properties = new HashMap<>();
        connectionProperties = new Properties();
        connectionProperties.putAll(props);

        if (props.containsKey(JNDI_NAME)) {
            connectionProperties.remove(JNDI_NAME);
            properties.put(JNDI_NAME, props.get(JNDI_NAME).toString());
        }

        if (props.containsKey(JNDI_CONTEXT_FACTORY)) {
            connectionProperties.remove(JNDI_CONTEXT_FACTORY);
            properties.put(JNDI_CONTEXT_FACTORY, props.get(JNDI_CONTEXT_FACTORY).toString());
        }

        if (props.containsKey(JNDI_PROVIDER_URL)) {
            connectionProperties.remove(JNDI_PROVIDER_URL);
            properties.put(JNDI_PROVIDER_URL, props.get(JNDI_PROVIDER_URL).toString());
        }

        if (props.containsKey(DRIVER)) {
            connectionProperties.remove(DRIVER);
            properties.put(DRIVER, props.get(DRIVER).toString());
        }

        if (props.containsKey(URL)) {
            connectionProperties.remove(URL);
            properties.put(URL, props.get(URL).toString());
        }

        if (props.containsKey(USER)) {
            connectionProperties.remove(USER);
            properties.put(USER, props.get(USER).toString());
        }

        if (props.containsKey(PASSWORD)) {
            connectionProperties.remove(PASSWORD);
            properties.put(PASSWORD, props.get(PASSWORD).toString());
        }

        if (props.containsKey(INITIAL_SIZE)) {
            connectionProperties.remove(INITIAL_SIZE);
            properties.put(INITIAL_SIZE, props.get(INITIAL_SIZE).toString());
        } else {
            properties.put(INITIAL_SIZE, String.valueOf(DEFAULT_INITIAL_SIZE));
        }

        if (props.containsKey(MIN_IDLE)) {
            connectionProperties.remove(MIN_IDLE);
            properties.put(MIN_IDLE, props.get(MIN_IDLE).toString());
        } else {
            properties.put(MIN_IDLE, String.valueOf(DEFAULT_MIN_IDLE));
        }

        if (props.containsKey(MAX_IDLE)) {
            connectionProperties.remove(MAX_IDLE);
            properties.put(MAX_IDLE, props.get(MAX_IDLE).toString());
        } else {
            properties.put(MAX_IDLE, String.valueOf(DEFAULT_MAX_IDLE));
        }

        if (props.containsKey(MAX_ACTIVE)) {
            connectionProperties.remove(MAX_ACTIVE);
            properties.put(MAX_ACTIVE, props.get(MAX_ACTIVE).toString());
        } else {
            properties.put(MAX_ACTIVE, String.valueOf(DEFAULT_MAX_ACTIVE).toString());
        }

        if (props.containsKey(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION)) {
            connectionProperties.remove(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION);
            properties.put(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION, props.get(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION).toString());
        } else {
            properties.put(MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION, String.valueOf(DEFAULT_MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION));
        }

        if (props.containsKey(LIVE_TIME)) {
            connectionProperties.remove(LIVE_TIME);
            properties.put(LIVE_TIME, props.get(LIVE_TIME).toString());
        } else {
            properties.put(LIVE_TIME, String.valueOf(DEFAULT_LIVE_TIME));
        }

        if (props.containsKey(MAX_IDLE_TIME)) {
            connectionProperties.remove(MAX_IDLE_TIME);
            properties.put(MAX_IDLE_TIME, props.get(MAX_IDLE_TIME).toString());
        } else {
            properties.put(MAX_IDLE_TIME, String.valueOf(DEFAULT_MAX_IDLE_TIME));
        }

        if (props.containsKey(MAX_WAIT_TIME)) {
            connectionProperties.remove(MAX_WAIT_TIME);
            properties.put(MAX_WAIT_TIME, props.get(MAX_WAIT_TIME).toString());
        } else {
            properties.put(MAX_WAIT_TIME, String.valueOf(DEFAULT_MAX_WAIT));
        }

        if (props.containsKey(EVICT_DELAY)) {
            connectionProperties.remove(EVICT_DELAY);
            properties.put(EVICT_DELAY, props.get(EVICT_DELAY).toString());
        } else {
            properties.put(EVICT_DELAY, String.valueOf(DEFAULT_EVICT_DELAY));
        }

        if (props.containsKey(VALIDATION_QUERY)) {
            connectionProperties.remove(VALIDATION_QUERY);
            properties.put(VALIDATION_QUERY, props.get(VALIDATION_QUERY).toString());
        } else {
            String validationQuery = DEFAULT_VALIDATION_QUERY;
            String url = props.get(URL) == null ? null : props.get(URL).toString();

            if (N.notNullOrEmpty(url)) {
                url = url.toLowerCase();

                if ((url.indexOf("oracle") >= 0)) {
                    validationQuery = "SELECT 1 FROM dual";
                } else if ((url.indexOf("db2") >= 0)) {
                    validationQuery = "SELECT 1 FROM sysibm.sysdummy1";
                } else if (url.indexOf("hsql") >= 0) {
                    validationQuery = "SELECT 1 FROM INFORMATION_SCHEMA.SYSTEM_USERS";
                }
            }

            properties.put(VALIDATION_QUERY, validationQuery);
        }

        if (props.containsKey(TEST_ON_BORROW)) {
            connectionProperties.remove(TEST_ON_BORROW);
            properties.put(TEST_ON_BORROW, props.get(TEST_ON_BORROW).toString());
        } else {
            properties.put(TEST_ON_BORROW, String.valueOf(DEFAULT_TEST_ON_BORROW));
        }

        if (props.containsKey(TEST_ON_RETURN)) {
            connectionProperties.remove(TEST_ON_RETURN);
            properties.put(TEST_ON_RETURN, props.get(TEST_ON_RETURN).toString());
        } else {
            properties.put(TEST_ON_RETURN, String.valueOf(DEFAULT_TEST_ON_RETURN));
        }

        if (props.containsKey(SQL_LOG)) {
            connectionProperties.remove(SQL_LOG);
            properties.put(SQL_LOG, props.get(SQL_LOG).toString());
        } else {
            properties.put(SQL_LOG, Boolean.FALSE.toString());
        }

        if (props.containsKey(PERF_LOG)) {
            connectionProperties.remove(PERF_LOG);
            properties.put(PERF_LOG, props.get(PERF_LOG).toString());
        } else {
            properties.put(PERF_LOG, String.valueOf(Long.MAX_VALUE));
        }
    }

    @Override
    public Map<String, String> getProperties() {
        return properties;
    }

    @Override
    public Properties getConnectionProperties() {
        return connectionProperties;
    }

    @Override
    public synchronized void updateLastSQLExecutionFailureTime() {
        lastSQLExecutionFailureTime = System.currentTimeMillis();
    }

    protected DataSource createJNDIDataSource(Map<String, String> properties) {
        String jndiName = connectionProperties.getProperty(JNDI_NAME);

        try {
            Properties jndiProps = new Properties();

            if (properties.get(JNDI_CONTEXT_FACTORY) != null) {
                jndiProps.put(Context.INITIAL_CONTEXT_FACTORY, properties.get(JNDI_CONTEXT_FACTORY));
            }

            if (properties.get(JNDI_PROVIDER_URL) != null) {
                jndiProps.put(Context.PROVIDER_URL, properties.get(JNDI_PROVIDER_URL));
            }

            InitialContext ctx = (jndiProps.size() == 0) ? new InitialContext() : new InitialContext(jndiProps);

            return (DataSource) ctx.lookup(jndiName);
        } catch (NamingException e) {
            throw new UncheckedException("Failed to bind to JNDI: " + jndiName + ". " + AbacusException.getErrorMsg(e), e);
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((connectionProperties == null) ? 0 : connectionProperties.hashCode());
        result = (prime * result) + ((properties == null) ? 0 : properties.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof AbstractConnectionManager) {
            AbstractConnectionManager other = (AbstractConnectionManager) obj;

            return N.equals(connectionProperties, other.connectionProperties) && N.equals(properties, other.properties);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{properties=" + properties + ", connectionProperties=" + connectionProperties + "}";
    }
}
