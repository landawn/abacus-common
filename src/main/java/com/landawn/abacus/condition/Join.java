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

import static com.landawn.abacus.util.WD._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

// TODO: Auto-generated Javadoc
/**
 * The Class Join.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Join extends AbstractCondition {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -6241397386906482856L;

    /** The join entities. */
    private List<String> joinEntities;

    /** The condition. */
    private Condition condition;

    /**
     * Instantiates a new join.
     */
    // For Kryo
    Join() {
    }

    /**
     * Instantiates a new join.
     *
     * @param joinEntity
     */
    public Join(String joinEntity) {
        this(Operator.JOIN, joinEntity);
    }

    /**
     * Instantiates a new join.
     *
     * @param operator
     * @param joinEntity
     */
    protected Join(Operator operator, String joinEntity) {
        this(operator, joinEntity, null);
    }

    /**
     * Instantiates a new join.
     *
     * @param joinEntity
     * @param condition
     */
    public Join(String joinEntity, Condition condition) {
        this(Operator.JOIN, joinEntity, condition);
    }

    /**
     * Instantiates a new join.
     *
     * @param operator
     * @param joinEntity
     * @param condition
     */
    protected Join(Operator operator, String joinEntity, Condition condition) {
        this(operator, N.asList(joinEntity), condition);
    }

    /**
     * Instantiates a new join.
     *
     * @param joinEntities
     * @param condition
     */
    public Join(Collection<String> joinEntities, Condition condition) {
        this(Operator.JOIN, joinEntities, condition);
    }

    /**
     * Instantiates a new join.
     *
     * @param operator
     * @param joinEntities
     * @param condition
     */
    protected Join(Operator operator, Collection<String> joinEntities, Condition condition) {
        super(operator);
        this.joinEntities = new ArrayList<>(joinEntities);
        this.condition = condition;
    }

    /**
     * Gets the join entities.
     *
     * @return
     */
    public List<String> getJoinEntities() {
        return joinEntities;
    }

    /**
     * Gets the condition.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends Condition> T getCondition() {
        return (T) condition;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> getParameters() {
        return (condition == null) ? N.<Object> emptyList() : condition.getParameters();
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        if (condition != null) {
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
        Join copy = (Join) super.copy();

        if (joinEntities != null) {
            copy.joinEntities = new ArrayList<>(joinEntities);
        }

        if (condition != null) {
            copy.condition = condition.copy();
        }

        return (T) copy;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        return getOperator().toString() + _SPACE + concatPropNames(joinEntities)
                + ((condition == null) ? N.EMPTY_STRING : (_SPACE + getCondition().toString(namingPolicy)));
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + joinEntities.hashCode();

        if (condition != null) {
            h = (h * 31) + condition.hashCode();
        }

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

        if (obj instanceof Join) {
            Join other = (Join) obj;

            return N.equals(operator, other.operator) && N.equals(joinEntities, other.joinEntities) && N.equals(condition, other.condition);
        }

        return false;
    }
}
