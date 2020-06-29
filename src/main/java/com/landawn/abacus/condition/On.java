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

import java.util.Map;

import com.landawn.abacus.condition.ConditionFactory.CF;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class On extends Cell {

    private static final long serialVersionUID = -2882073517302039100L;

    // For Kryo
    On() {
    }

    public On(Condition condition) {
        super(Operator.ON, condition);
    }

    public On(String propName, String anoPropName) {
        this(createOnCondition(propName, anoPropName));
    }

    public On(Map<String, String> propNamePair) {
        this(createOnCondition(propNamePair));
    }

    /**
     * Creates the on condition.
     *
     * @param propName
     * @param anoPropName
     * @return
     */
    static Condition createOnCondition(String propName, String anoPropName) {
        return new Equal(propName, CF.expr(anoPropName));
    }

    /**
     * Creates the on condition.
     *
     * @param propNamePair
     * @return
     */
    static Condition createOnCondition(Map<String, String> propNamePair) {
        if (propNamePair.size() == 1) {
            Map.Entry<String, String> entry = propNamePair.entrySet().iterator().next();

            return createOnCondition(entry.getKey(), entry.getValue());
        } else {
            And and = CF.and();

            for (String propName : propNamePair.keySet()) {
                and.add(createOnCondition(propName, propNamePair.get(propName)));
            }

            return and;
        }
    }
}
