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

import static com.landawn.abacus.util.WD.COMMA_SPACE;
import static com.landawn.abacus.util.WD.SPACE;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SortDirection;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class OrderBy extends Clause {

    private static final long serialVersionUID = 6430369852069478807L;

    // For Kryo
    OrderBy() {
    }

    public OrderBy(final Condition condition) {
        super(Operator.ORDER_BY, condition);
    }

    @SafeVarargs
    public OrderBy(final String... propNames) {
        this(CF.expr(createCondition(propNames)));
    }

    public OrderBy(final String propName, final SortDirection direction) {
        this(CF.expr(createCondition(propName, direction)));
    }

    public OrderBy(final Collection<String> propNames, final SortDirection direction) {
        this(CF.expr(createCondition(propNames, direction)));
    }

    /**
     * 
     * @param orders should be a {@code LinkedHashMap}
     */
    public OrderBy(final Map<String, SortDirection> orders) {
        this(createCondition(orders));
    }

    /**
     * 
     * @param orders
     */
    public OrderBy(final LinkedHashMap<String, SortDirection> orders) {
        this(createCondition(orders));
    }

    /**
     * Creates the condition.
     *
     * @param propNames
     * @return
     */
    static String createCondition(String... propNames) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (String propName : propNames) {
                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates the condition.
     *
     * @param propName
     * @param direction
     * @return
     */
    static String createCondition(String propName, SortDirection direction) {
        return propName + SPACE + direction.toString();
    }

    /**
     * Creates the condition.
     *
     * @param propNames
     * @param direction
     * @return
     */
    static String createCondition(Collection<String> propNames, SortDirection direction) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (String propName : propNames) {
                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
            }

            sb.append(SPACE);
            sb.append(direction.toString());

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     * Creates the condition.
     *
     * @param orders
     * @return
     */
    static String createCondition(Map<String, SortDirection> orders) {
        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            int i = 0;
            for (String propName : orders.keySet()) {
                if (i++ > 0) {
                    sb.append(COMMA_SPACE);
                }

                sb.append(propName);
                sb.append(SPACE);
                sb.append(orders.get(propName).toString());
            }

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }
}
