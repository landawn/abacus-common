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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import com.landawn.abacus.exception.AbacusException;
import com.landawn.abacus.util.Configuration;
import com.landawn.abacus.util.N;

/**
 * 
 * @since 1.3
 * 
 * @author Haiyang Li
 */
public final class DataSourceManagerConfiguration extends Configuration {
    /**
     * Field DATA_SOURCE_MANAGER. (value is ""dataSourceManager"")
     */
    public static final String DATA_SOURCE_MANAGER = "dataSourceManager";

    /**
     * Field DATA_SOURCE_SELECTOR. (value is ""dataSourceSelector"")
     */
    public static final String DATA_SOURCE_SELECTOR = "dataSourceSelector";

    /**
     * Field LIVE_ENV. (value is ""liveEnv"")
     */
    public static final String LIVE_ENV = "liveEnv";

    private final String liveEnv;
    private List<DataSourceConfiguration> dataSourceConfigurationList;

    public DataSourceManagerConfiguration(Element element, Map<String, String> properties) {
        super(element, properties);

        liveEnv = this.getAttribute(LIVE_ENV);

        if (N.isNullOrEmpty(liveEnv)) {
            throw new AbacusException("must set the 'liveEnv' attribute in 'dataSourceManager' element. for example: <dataSourceManager liveEnv=\"dev\"> ");
        }
    }

    public String getLiveEnv() {
        return liveEnv;
    }

    public List<DataSourceConfiguration> getDataSourceConfigurationList() {
        return dataSourceConfigurationList;
    }

    @Override
    protected void init() {
        dataSourceConfigurationList = new ArrayList<>();
    }

    @Override
    protected void complexElement2Attr(Element element) {
        String eleName = element.getNodeName();

        if (DataSourceConfiguration.DATA_SOURCE.equals(eleName)) {
            dataSourceConfigurationList.add(new DataSourceConfiguration(element, this.props));
        } else {
            throw new AbacusException("Unknown element: " + eleName);
        }
    }
}
