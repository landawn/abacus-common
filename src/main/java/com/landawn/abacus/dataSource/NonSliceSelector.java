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

import java.util.List;
import java.util.Map;

import com.landawn.abacus.SliceSelector;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public class NonSliceSelector implements SliceSelector {
    @Override
    public String select(String entityName, String sql, Object[] parameters, Map<String, Object> options) {
        return sql;
    }

    @Override
    public String select(String entityName, String sql, List<?> parameters, Map<String, Object> options) {
        return sql;
    }
}
