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

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class SQLDataSourceManager implements DataSourceManager {
    private static final Logger logger = LoggerFactory.getLogger(SQLDataSourceManager.class);

    private final DataSourceManagerConfiguration dsmConfig;
    private final Properties<String, String> properties;
    private final Map<String, DataSource> activeDataSources;
    private final DataSource primaryDataSource;
    private final DataSourceSelector dataSourceSelector;

    private boolean isClosed = false;

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

    @Override
    public DataSource getPrimaryDataSource() {
        return primaryDataSource;
    }

    @Override
    public Map<String, DataSource> getActiveDataSources() {
        return activeDataSources;
    }

    @Override
    public DataSourceSelector getDataSourceSelector() {
        return dataSourceSelector;
    }

    @Override
    public Properties<String, String> getProperties() {
        return properties;
    }

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

    @Override
    public boolean isClosed() {
        return isClosed;
    }
}
