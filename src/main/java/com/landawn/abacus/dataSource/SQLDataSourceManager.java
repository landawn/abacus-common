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

import java.util.LinkedHashMap;
import java.util.Map;

import com.landawn.abacus.DataSource;
import com.landawn.abacus.DataSourceManager;
import com.landawn.abacus.DataSourceSelector;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Properties;
import com.landawn.abacus.util.TypeAttrParser;

// TODO: Auto-generated Javadoc
/**
 * The Class SQLDataSourceManager.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class SQLDataSourceManager implements DataSourceManager {

    /** The Constant logger. */
    private static final Logger logger = LoggerFactory.getLogger(SQLDataSourceManager.class);

    /** The dsm config. */
    private final DataSourceManagerConfiguration dsmConfig;

    /** The properties. */
    private final Properties<String, String> properties;

    /** The active data sources. */
    private final Map<String, DataSource> activeDataSources;

    /** The primary data source. */
    private final DataSource primaryDataSource;

    /** The data source selector. */
    private final DataSourceSelector dataSourceSelector;

    /** The is closed. */
    private boolean isClosed = false;

    /**
     * Instantiates a new SQL data source manager.
     *
     * @param dataSourceManagerConfiguration
     */
    public SQLDataSourceManager(DataSourceManagerConfiguration dataSourceManagerConfiguration) {
        this.dsmConfig = dataSourceManagerConfiguration;

        properties = new Properties<>();

        for (String attrName : dsmConfig.getAttrNames()) {
            properties.put(attrName, dsmConfig.getAttribute(attrName));
        }

        activeDataSources = new LinkedHashMap<>();

        DataSource ds = null;

        for (DataSourceConfiguration dsConfig : dsmConfig.getDataSourceConfigurationList()) {
            if (dsConfig.getAttribute(DataSourceConfiguration.ENV).equalsIgnoreCase(dsmConfig.getLiveEnv())) {
                ds = new SQLDataSource(dsConfig);

                if (activeDataSources.containsKey(ds.getName())) {
                    throw new RuntimeException("duplicated data sources with same name '" + ds.getName() + "' are found.");
                } else {
                    activeDataSources.put(ds.getName(), ds);
                }
            }
        }

        if (activeDataSources.size() == 0) {
            throw new RuntimeException("No DataSource is configured for env '" + dsmConfig.getLiveEnv() + "'");
        }

        primaryDataSource = activeDataSources.values().iterator().next();

        String attr = properties.get(DataSourceManagerConfiguration.DATA_SOURCE_SELECTOR);

        if (attr == null) {
            dataSourceSelector = new SimpleSourceSelector();
        } else {
            dataSourceSelector = (DataSourceSelector) TypeAttrParser.newInstance(null, attr);
        }
    }

    /**
     * Gets the primary data source.
     *
     * @return
     */
    @Override
    public DataSource getPrimaryDataSource() {
        return primaryDataSource;
    }

    /**
     * Gets the active data source.
     *
     * @param dataSourceName
     * @return
     */
    @Override
    public DataSource getActiveDataSource(String dataSourceName) {
        return activeDataSources.get(dataSourceName);
    }

    /**
     * Gets the active data sources.
     *
     * @return
     */
    @Override
    public Map<String, DataSource> getActiveDataSources() {
        return activeDataSources;
    }

    /**
     * Gets the data source selector.
     *
     * @return
     */
    @Override
    public DataSourceSelector getDataSourceSelector() {
        return dataSourceSelector;
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
     * Close.
     */
    @Override
    public void close() {
        if (isClosed) {
            return;
        }

        for (DataSource ds : activeDataSources.values()) {
            try {
                ds.close();
            } catch (Exception e) {
                logger.error("Failed to close data source: " + ds.getName());
            }
        }

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
}
