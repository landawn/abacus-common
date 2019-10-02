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

import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Enum Operator.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public enum Operator {
    /**
     * Field EQUAL.
     */
    EQUAL(WD.EQUAL),
    /**
     * Field NOT_EQUAL.
     */
    NOT_EQUAL(WD.NOT_EQUAL),
    /**
     * Field NOT_EQUAL2.
     */
    NOT_EQUAL2(WD.NOT_EQUAL2),
    /**
     * Field NOT.
     */
    NOT(WD.NOT),
    /**
     * Field NOT2.
     */
    NOT_OP(WD.EXCLAMATION),
    /**
     * Field XOR.
     */
    XOR(WD.XOR),
    /**
     * Field LIKE.
     */
    LIKE(WD.LIKE),
    /**
     * Field AND.
     */
    AND(WD.AND),
    /**
     * Field AND_OP.
     */
    AND_OP(WD.AND_OP),
    /**
     * Field OR.
     */
    OR(WD.OR),
    /**
     * Field OR_OP.
     */
    OR_OP(WD.OR_OP),
    /**
     * Field GREATER_THAN.
     */
    GREATER_THAN(WD.GREATER_THAN),
    /**
     * Field GREATER_EQUAL.
     */
    GREATER_EQUAL(WD.GREATER_EQUAL),
    /**
     * Field LESS_THAN.
     */
    LESS_THAN(WD.LESS_THAN),
    /**
     * Field LESS_EQUAL.
     */
    LESS_EQUAL(WD.LESS_EQUAL),
    /**
     * Field BETWEEN.
     */
    BETWEEN(WD.BETWEEN),
    /**
     * Field IS.
     */
    IS(WD.IS),
    /**
     * Field IS_NOT.
     */
    IS_NOT(WD.IS_NOT),
    /**
     * Field EXISTS.
     */
    EXISTS(WD.EXISTS),
    /**
     * Field IN.
     */
    IN(WD.IN),
    /**
     * Field ANY.
     */
    ANY(WD.ANY),
    /**
     * Field SOME.
     */
    SOME(WD.SOME),
    /**
     * Field ALL.
     */
    ALL(WD.ALL),
    /**
     * Field ON.
     */
    ON(WD.ON),
    /**
     * 
     */
    USING(WD.USING),
    /**
     * Field JOIN.
     */
    JOIN(WD.JOIN),
    /**
     * Field LEFT_JOIN.
     */
    LEFT_JOIN(WD.LEFT_JOIN),
    /**
     * Field RIGHT_JOIN.
     */
    RIGHT_JOIN(WD.RIGHT_JOIN),
    /**
     * Field FULL_JOIN.
     */
    FULL_JOIN(WD.FULL_JOIN),
    /**
     * Field CROSS_JOIN.
     */
    CROSS_JOIN(WD.CROSS_JOIN),
    /**
     * Field INNER_JOIN.
     */
    INNER_JOIN(WD.INNER_JOIN),
    /**
     * Field NATURAL_JOIN.
     */
    NATURAL_JOIN(WD.NATURAL_JOIN),
    /**
     * Field WHERE.
     */
    WHERE(WD.WHERE),
    /**
     * Field HAVING.
     */
    HAVING(WD.HAVING),
    /**
     * Field GROUP_BY.
     */
    GROUP_BY(WD.GROUP_BY),
    /**
     * Field ORDER_BY.
     */
    ORDER_BY(WD.ORDER_BY),
    /**
     * Field LIMIT.
     */
    LIMIT(WD.LIMIT),
    /**
     * Field OFFSET.
     */
    OFFSET(WD.OFFSET),
    /**
     * 
     * @deprecated
     */
    FOR_UPDATE(WD.FOR_UPDATE),
    /**
     * Field UNION.
     */
    UNION(WD.UNION),
    /**
     * Field UNION_ALL.
     */
    UNION_ALL(WD.UNION_ALL),
    /**
     * Field INTERSECT.
     */
    INTERSECT(WD.INTERSECT),
    /**
     * Field EXCEPT.
     */
    EXCEPT(WD.EXCEPT),
    /**
     * Field MINUS.
     */
    MINUS(WD.EXCEPT2),
    /* Special operator */
    /**
     * Field SPACE.
     */
    EMPTY(N.EMPTY_STRING);
    /**
     * Field name.
     */
    private String name;

    /**
     * Field operatorMap.
     */
    private static final Map<String, Operator> operatorMap = new HashMap<>();

    /**
     * Constructor for Operator.
     *
     * @param name
     */
    Operator(String name) {
        this.name = name;
    }

    /**
     * Gets the operator.
     *
     * @param name
     * @return
     */
    public synchronized static Operator getOperator(String name) {
        if (operatorMap.size() == 0) {
            Operator[] values = Operator.values();

            for (int i = 0; i < values.length; i++) {
                operatorMap.put(values[i].name, values[i]);
            }
        }

        Operator operator = operatorMap.get(name);

        if (operator == null) {
            operator = operatorMap.get(name.toUpperCase());

            if (operator != null) {
                operatorMap.put(name, operator);
            }
        }

        return operator;
    }

    /**
     * Gets the name.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return name;
    }
}
