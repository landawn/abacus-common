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

import static com.landawn.abacus.util.WD._PARENTHESES_L;
import static com.landawn.abacus.util.WD._PARENTHESES_R;
import static com.landawn.abacus.util.WD._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;

// TODO: Auto-generated Javadoc
/**
 * This class is used to join multiple conditions. like {@code And}, {@code Or}. But must not put clause(where, order by
 * ...) into it. Those clauses are joined by {@code Criteria}.
 *
 * @author Haiyang Li
 * @see com.landawn.abacus.condition.Criteria
 * @since 0.8
 */
public class Junction extends AbstractCondition {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -5520467185002717755L;

    /** The condition list. */
    private List<Condition> conditionList;

    /**
     * Instantiates a new junction.
     */
    // For Kryo
    Junction() {
    }

    /**
     * Instantiates a new junction.
     *
     * @param operator
     * @param conditions
     */
    @SafeVarargs
    public Junction(Operator operator, Condition... conditions) {
        super(operator);
        conditionList = new ArrayList<>();
        add(conditions);
    }

    /**
     * Instantiates a new junction.
     *
     * @param operator
     * @param conditions
     */
    public Junction(Operator operator, Collection<? extends Condition> conditions) {
        super(operator);
        conditionList = new ArrayList<>();
        add(conditions);
    }

    /**
     * Gets the conditions.
     *
     * @return
     */
    public List<Condition> getConditions() {
        return conditionList;
    }

    /**
     *
     * @param conditions
     */
    @SafeVarargs
    public final void set(Condition... conditions) {
        conditionList.clear();
        add(conditions);
    }

    /**
     *
     * @param conditions
     */
    public void set(Collection<? extends Condition> conditions) {
        conditionList.clear();
        add(conditions);
    }

    /**
     *
     * @param conditions
     */
    @SafeVarargs
    public final void add(Condition... conditions) {
        for (Condition cond : conditions) {
            conditionList.add(cond);
        }
    }

    /**
     *
     * @param conditions
     */
    public void add(Collection<? extends Condition> conditions) {
        conditionList.addAll(conditions);
    }

    /**
     *
     * @param conditions
     */
    @SafeVarargs
    public final void remove(Condition... conditions) {
        for (Condition cond : conditions) {
            conditionList.remove(cond);
        }
    }

    /**
     *
     * @param conditions
     */
    public void remove(Collection<? extends Condition> conditions) {
        conditionList.removeAll(conditions);
    }

    /**
     * Clear.
     */
    public void clear() {
        conditionList.clear();
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        List<Object> parameters = new ArrayList<>();

        for (Condition condition : conditionList) {
            parameters.addAll(condition.getParameters());
        }

        return parameters;
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        for (Condition condition : conditionList) {
            condition.clearParameters();
        }
    }

    /**
     *
     * @param <T>
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends Condition> T copy() {
        Junction result = (Junction) super.copy();

        result.conditionList = new ArrayList<>();

        for (Condition cond : conditionList) {
            result.conditionList.add(cond.copy());
        }

        return (T) result;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        if (N.isNullOrEmpty(conditionList)) {
            return N.EMPTY_STRING;
        }

        final StringBuilder sb = Objectory.createStringBuilder();

        try {
            sb.append(_PARENTHESES_L);

            for (int i = 0; i < conditionList.size(); i++) {
                if (i > 0) {
                    sb.append(_SPACE);
                    sb.append(getOperator().toString());
                    sb.append(_SPACE);
                }

                sb.append(_PARENTHESES_L);
                sb.append(conditionList.get(i).toString(namingPolicy));
                sb.append(_PARENTHESES_R);
            }

            sb.append(_PARENTHESES_R);

            return sb.toString();
        } finally {
            Objectory.recycle(sb);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + conditionList.hashCode();

        return h;
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Junction) {
            Junction other = (Junction) obj;

            return N.equals(operator, other.operator) && N.equals(conditionList, other.conditionList);

        }

        return false;
    }
}
