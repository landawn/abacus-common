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
import java.util.Map;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.SortDirection;

// TODO: Auto-generated Javadoc
/**
 * The Class GroupBy.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class GroupBy extends Clause {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -8805648115111124665L;

    /**
     * Instantiates a new group by.
     */
    // For Kryo
    GroupBy() {
    }

    /**
     * Instantiates a new group by.
     *
     * @param condition
     */
    public GroupBy(Condition condition) {
        super(Operator.GROUP_BY, condition);
    }

    /**
     * Instantiates a new group by.
     *
     * @param propNames
     */
    @SafeVarargs
    public GroupBy(String... propNames) {
        this(CF.expr(OrderBy.createCondition(propNames)));
    }

    /**
     * Instantiates a new group by.
     *
     * @param propName
     * @param direction
     */
    public GroupBy(String propName, SortDirection direction) {
        this(CF.expr(OrderBy.createCondition(propName, direction)));
    }

    /**
     * Instantiates a new group by.
     *
     * @param propNames
     * @param direction
     */
    public GroupBy(Collection<String> propNames, SortDirection direction) {
        this(CF.expr(OrderBy.createCondition(propNames, direction)));
    }

    /**
     * Instantiates a new group by.
     *
     * @param orders
     */
    public GroupBy(Map<String, SortDirection> orders) {
        this(OrderBy.createCondition(orders));
    }
}
