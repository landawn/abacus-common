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

import java.util.Collections;
import java.util.Map;

import org.w3c.dom.Element;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.util.Configuration;

/**
 * 
 * @since 1.3
 * 
 * @author Haiyang Li
 */
public final class DataSourceConfiguration extends Configuration {

    /**
     * Field DATABASE. (value is ""database"")
     */
    public static final String DATABASE = "database";

    /**
     * Field DATA_SOURCE_MANAGER. (value is ""dataSource"")
     */
    public static final String DATA_SOURCE = "dataSource";

    /**
     * Field NAME. (value is ""name"")
     */
    public static final String NAME = "name";

    /**
     * Field ENV. (value is ""env"")
     */
    public static final String ENV = "env";

    /**
     * Field SLICE_SELECTOR. (value is ""sliceSelector"")
     */
    public static final String SLICE_SELECTOR = "sliceSelector";

    /**
     * Field PROVIDER. (value is ""provider"")
     */
    public static final String PROVIDER = "provider";

    /**
     * Field DBCP. (value is ""dbcp"")
     */
    public static final String DBCP = "dbcp";

    /**
     * Field DBCP2. (value is ""dbcp2"")
     */
    public static final String DBCP2 = "dbcp2";

    /**
     * Field C3P0. (value is ""c3p0"")
     */
    public static final String C3P0 = "c3p0";

    /**
     * Field HIKARI_CP. (value is ""HikariCP"")
     */
    public static final String HIKARI_CP = "HikariCP";

    /**
     * Field DEFAULT_ISOLATION. (value is ""defaultIsolation"")
     */
    public static final String DEFAULT_ISOLATION = "defaultIsolation";

    /**
     * Field SQL_LOG. (value is ""sqlLog"")
     */
    public static final String SQL_LOG = "sqlLog";

    /**
     * Field PERF_LOG. (value is ""perfLog"")
     */
    public static final String PERF_LOG = "perfLog";

    /**
     * Field QUERY_WITH_READ_ONLY_CONNECTION_BY_DEFAULT. (value is ""queryWithReadOnlyConnectionByDefault""
     */
    public static final String QUERY_WITH_READ_ONLY_CONNECTION_BY_DEFAULT = "queryWithReadOnlyConnectionByDefault";

    /**
     * Field CONNECTION. (value is ""connection"")
     */
    public static final String CONNECTION = "connection";

    /**
     * Field READ_ONLY_CONNECTION. (value is ""readOnlyConnection"")
     */
    public static final String READ_ONLY_CONNECTION = "readOnlyConnection";

    /**
     * Field JNDI_NAME. (value is ""jndiName"")
     */
    public static final String JNDI_NAME = "jndiName";

    /**
     * Field JNDI_CONTEXT_FACTORY. (value is ""jndiContextFactory"")
     */
    public static final String JNDI_CONTEXT_FACTORY = "jndiContextFactory";

    /**
     * Field JNDI_PROVIDER_URL. (value is ""jndiProviderUrl"")
     */
    public static final String JNDI_PROVIDER_URL = "jndiProviderUrl";

    /**
     * Field DRIVER. (value is ""driver"")
     */
    public static final String DRIVER = "driver";

    /**
     * Field URL. (value is ""url"")
     */
    public static final String URL = "url";

    /**
     * Field USER. (value is ""user"")
     */
    public static final String USER = "user";

    /**
     * Field PASSWORD. (value is ""password"")
     */
    public static final String PASSWORD = "password";

    /**
     * The connection number which is created when the server is started
     */
    public static final String INITIAL_SIZE = "initialSize";

    /**
     * Field DEFAULT_INITIAL_SIZE. (value is ""0"")
     */
    public static final int DEFAULT_INITIAL_SIZE = 0;

    /**
     * The connection number which can be kept in the pool permanently when they're not used.
     */
    public static final String MIN_IDLE = "minIdle";

    /**
     * Field DEFAULT_MAX_ACTIVE. (value is ""8"")
     */
    public static final int DEFAULT_MIN_IDLE = 8;

    /**
     * The connection number which can be kept in the pool permanently when they're not used.
     */
    public static final String MAX_IDLE = "maxIdle";

    /**
     * Field DEFAULT_MAX_ACTIVE. (value is ""8"")
     */
    public static final int DEFAULT_MAX_IDLE = 16;

    /**
     * The connection number which can be created from the pool
     */
    public static final String MAX_ACTIVE = "maxActive";

    /**
     * .Field DEFAULT_MAX_ACTIVE. (value is ""32"")
     */
    public static final int DEFAULT_MAX_ACTIVE = 32;

    /**
     * Field MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION. (value is ""maxOpenPreparedStatementsPerConnection"")
     */
    public static final String MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION = "maxOpenPreparedStatementsPerConnection";

    /**
     * Field DEFAULT_MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION. (value is ""256"")
     */
    public static final int DEFAULT_MAX_OPEN_PREPARED_STATEMENTS_PER_CONNECTION = 256;

    /**
     * Field HANDLE_LIVE_TIME. (value is ""liveTime""). Unit is milliseconds
     */
    public static final String LIVE_TIME = "liveTime";

    /**
     * Field HANDLE_DEFAULT_LIVE_TIME. (value is ""24 * 60 * 60 * 1000"").
     */
    public static final long DEFAULT_LIVE_TIME = 24 * 60 * 60 * 1000;

    /**
     * Field HANDLE_MAX_IDLE_TIME. (value is ""maxIdleTime""). Unit is milliseconds
     */
    public static final String MAX_IDLE_TIME = "maxIdleTime";

    /**
     * Field HANDLE_DEFAULT_MAX_IDLE_TIME. (value is ""30 * 60 * 1000"").
     */
    public static final long DEFAULT_MAX_IDLE_TIME = 30 * 60 * 1000;

    /**
     * Field MAX_WAIT_TIME. (value is ""maxWaitTime""). unit is milliseconds
     */
    public static final String MAX_WAIT_TIME = "maxWaitTime";

    /**
     * Field DEFAULT_MAX_WAIT. (value is ""1000"").
     */
    public static final long DEFAULT_MAX_WAIT = 1000;

    /**
     * Field EVICT_DELAY. (value is ""evictDelay""). unit is milliseconds
     */
    public static final String EVICT_DELAY = "evictDelay";

    /**
     * Field DEFAULT_EVICT_DELAY. (value is ""5000"").
     */
    public static final int DEFAULT_EVICT_DELAY = 5000;

    /**
     * Field VALIDATION_QUERY.
     */
    public static final String VALIDATION_QUERY = "validationQuery";

    /**
     * Field DEFAULT_VALIDATION_QUERY. (value is ""select 1 from dual"")
     */
    public static final String DEFAULT_VALIDATION_QUERY = "SELECT 1";

    /**
     * Field TEST_ON_BORROW. (value is ""testOnBorrow"")
     */
    public static final String TEST_ON_BORROW = "testOnBorrow";

    /**
     * Field DEFAULT_TEST_ON_BORROW. (value is ""true"")
     */
    public static final boolean DEFAULT_TEST_ON_BORROW = true;

    /**
     * Field TEST_ON_RETURN. (value is ""testOnReturn"")
     */
    public static final String TEST_ON_RETURN = "testOnReturn";

    /**
     * Field DEFAULT_TEST_ON_RETURN. (value is ""false"")
     */
    public static final boolean DEFAULT_TEST_ON_RETURN = false;

    private Map<String, String> connectionProps;
    private Map<String, String> readOnlyConnectionProps;

    public DataSourceConfiguration(Element element, Map<String, String> properties) {
        super(element, properties);

        if (this.getAttribute(NAME) == null) {
            throw new AbacusException("must set the 'name' attribute in 'dataSourceManager' element. for example: <dataSource name=\"codes\"> evn=\"dev\">");
        }

        if (this.getAttribute(ENV) == null) {
            throw new AbacusException("must set the 'env' attribute in 'dataSourceManager' element. for example: <dataSource name=\"codes\"> evn=\"dev\">");
        }
    }

    public Map<String, String> getConnectionProps() {
        return connectionProps;
    }

    public Map<String, String> getReadOnlyConnectionProps() {
        return readOnlyConnectionProps;
    }

    @Override
    protected void complexElement2Attr(Element element) {
        String eleName = element.getNodeName();

        if (DataSourceConfiguration.CONNECTION.equals(eleName)) {
            connectionProps = Collections.unmodifiableMap(new Configuration(element, this.props) {
            }.getAttributes());
        } else if (DataSourceConfiguration.READ_ONLY_CONNECTION.equals(eleName)) {
            readOnlyConnectionProps = Collections.unmodifiableMap(new Configuration(element, this.props) {
            }.getAttributes());
        } else {
            throw new AbacusException("Unknown element: " + eleName);
        }
    }
}
