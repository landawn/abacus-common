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

package com.landawn.abacus;

import java.util.Map;

import com.landawn.abacus.util.Properties;

// TODO: Auto-generated Javadoc
/**
 * The Interface DataSourceManager.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public interface DataSourceManager {

    /**
     * Gets the data source selector.
     *
     * @return the data source selector
     */
    DataSourceSelector getDataSourceSelector();

    /**
     * Returns the first(primary) dataSource specified for the live env in the configuration.
     *
     * @return the primary data source
     */
    DataSource getPrimaryDataSource();

    /**
     * Gets the active data source by name.
     * 
     * @param dataSourceName
     * @return
     */
    DataSource getActiveDataSource(String dataSourceName);

    /**
     * Returns all the active data sources for the live env.
     *
     * @return the active data sources
     */
    Map<String, DataSource> getActiveDataSources();

    /**
     * Gets the properties.
     *
     * @return the properties used to manage the data source.
     */
    Properties<String, String> getProperties();

    /**
     * Close.
     */
    void close();

    /**
     * Checks if is closed.
     *
     * @return true, if is closed
     */
    boolean isClosed();
}
