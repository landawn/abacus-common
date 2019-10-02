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
import java.util.Set;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class CriteriaUtil.
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
public final class CriteriaUtil {

    /** The Constant clauseOperators. */
    private static final Set<Operator> clauseOperators;

    static {
        final Set<Operator> set = N.newLinkedHashSet();
        // it has order, don't change the order.
        set.add(Operator.JOIN);
        set.add(Operator.LEFT_JOIN);
        set.add(Operator.RIGHT_JOIN);
        set.add(Operator.FULL_JOIN);
        set.add(Operator.CROSS_JOIN);
        set.add(Operator.INNER_JOIN);
        set.add(Operator.NATURAL_JOIN);
        set.add(Operator.WHERE);
        set.add(Operator.GROUP_BY);
        set.add(Operator.HAVING);
        set.add(Operator.ORDER_BY);
        set.add(Operator.LIMIT);
        // clauseOperators.add(Operator.FOR_UPDATE);
        // Notice: If there are several connection operator,
        // this is their order.
        set.add(Operator.UNION_ALL);
        set.add(Operator.UNION);
        set.add(Operator.INTERSECT);
        set.add(Operator.EXCEPT);
        set.add(Operator.MINUS);

        clauseOperators = ImmutableSet.of(set);
    }

    /**
     * Instantiates a new criteria util.
     */
    private CriteriaUtil() {
        // singleton
    }

    /**
     * Gets the clause operators.
     *
     * @return
     */
    public static Set<Operator> getClauseOperators() {
        return clauseOperators;
    }

    /**
     * Checks if is clause.
     *
     * @param operator
     * @return true, if is clause
     */
    public static boolean isClause(Operator operator) {
        return (operator == null) ? false : clauseOperators.contains(operator);
    }

    /**
     * Checks if is clause.
     *
     * @param operator
     * @return true, if is clause
     */
    public static boolean isClause(String operator) {
        return isClause(Operator.getOperator(operator));
    }

    /**
     * Checks if is clause.
     *
     * @param condition
     * @return true, if is clause
     */
    public static boolean isClause(Condition condition) {
        //        if (condition == null) {
        //            return false;
        //        }
        //
        //        if (condition instanceof Expression) {
        //            Expression exp = (Expression) condition;
        //
        //            if (N.isNullOrEmpty(exp.getLiteral())) {
        //                return false;
        //            } else {
        //                SQLParser sqlParser = SQLParser.valueOf(exp.getLiteral());
        //                String word = sqlParser.nextWord();
        //
        //                return isClause(word) || isClause(word + D._SPACE + sqlParser.nextWord());
        //            }
        //        } else {
        //            return isClause(condition.getOperator());
        //        }
        return (condition == null) ? false : isClause(condition.getOperator());
    }

    /**
     *
     * @param criteria
     * @param conditions
     */
    @SafeVarargs
    public static void add(Criteria criteria, Condition... conditions) {
        criteria.add(conditions);
    }

    /**
     *
     * @param criteria
     * @param conditions
     */
    public static void add(Criteria criteria, Collection<Condition> conditions) {
        criteria.add(conditions);
    }

    /**
     *
     * @param criteria
     * @param operator
     */
    public static void remove(Criteria criteria, Operator operator) {
        criteria.remove(operator);
    }

    /**
     *
     * @param criteria
     * @param conditions
     */
    @SafeVarargs
    public static void remove(Criteria criteria, Condition... conditions) {
        criteria.remove(conditions);
    }

    /**
     *
     * @param criteria
     * @param conditions
     */
    public static void remove(Criteria criteria, Collection<Condition> conditions) {
        criteria.remove(conditions);
    }
}
