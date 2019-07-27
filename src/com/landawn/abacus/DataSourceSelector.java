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

import java.util.List;
import java.util.Map;

/**
 * It's designed to support distributed data source.
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public interface DataSourceSelector {
    /**
     * select data source by sql statement and parameters. 
     * Generally, the <code>DataSource</code> specified by option <code>QUERY_WITH_DATA_SOURCE</code> should be checked/returned first if it exists.
     * 
     * @param dataSourceManager
     * @param entityName
     * @param sql
     * @param parameters
     * @param options the target data source may be specified by <code>com.landawn.abacus.util.Options.Query.QUERY_WITH_DATA_SOURCE</code>
     * @return
     */
    DataSource select(DataSourceManager dataSourceManager, String entityName, String sql, Object[] parameters, Map<String, Object> options);

    /**
     * select data source by sql statement and parameters for batch operation.
     * Generally, the <code>DataSource</code> specified by option <code>QUERY_WITH_DATA_SOURCE</code> should be checked/returned first if it exists.
     * 
     * @param dataSourceManager
     * @param entityName
     * @param sql
     * @param parameters
     * @param options the target data source may be specified by <code>com.landawn.abacus.util.Options.Query.QUERY_WITH_DATA_SOURCE</code>
     * @return
     */
    DataSource select(DataSourceManager dataSourceManager, String entityName, String sql, List<?> parameters, Map<String, Object> options);
}
