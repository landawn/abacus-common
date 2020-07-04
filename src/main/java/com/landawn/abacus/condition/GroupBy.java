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

package com.landawn.abacus.condition;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.SortDirection;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class GroupBy extends Clause {

    private static final long serialVersionUID = -8805648115111124665L;

    // For Kryo
    GroupBy() {
    }

    public GroupBy(Condition condition) {
        super(Operator.GROUP_BY, condition);
    }

    @SafeVarargs
    public GroupBy(final String... propNames) {
        this(CF.expr(OrderBy.createCondition(propNames)));
    }

    public GroupBy(final String propName, final SortDirection direction) {
        this(CF.expr(OrderBy.createCondition(propName, direction)));
    }

    public GroupBy(final Collection<String> propNames, final SortDirection direction) {
        this(CF.expr(OrderBy.createCondition(propNames, direction)));
    }

    /**
     * 
     * @param orders should be a {@code LinkedHashMap}
     */
    public GroupBy(final Map<String, SortDirection> orders) {
        this(OrderBy.createCondition(orders));
    }

    public GroupBy(final LinkedHashMap<String, SortDirection> orders) {
        this(OrderBy.createCondition(orders));
    }
}
