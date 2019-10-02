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

import java.util.List;

import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;

// TODO: Auto-generated Javadoc
/**
 * The Class Cell.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Cell extends AbstractCondition {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 8889063891169154425L;

    /** The condition. */
    private Condition condition;

    /**
     * Instantiates a new cell.
     */
    // For Kryo
    Cell() {
    }

    /**
     * Instantiates a new cell.
     *
     * @param operator
     * @param condition
     */
    public Cell(Operator operator, Condition condition) {
        super(operator);
        this.condition = condition;
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
     * Sets the condition.
     *
     * @param condition the new condition
     */
    public void setCondition(Condition condition) {
        this.condition = condition;
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
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        Cell copy = (Cell) super.copy();

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
        return getOperator().toString() + ((condition == null) ? N.EMPTY_STRING : WD._SPACE + condition.toString(namingPolicy));
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + ((condition == null) ? 0 : condition.hashCode());

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

        if (obj instanceof Cell) {
            Cell other = (Cell) obj;

            return N.equals(operator, other.operator) && N.equals(condition, other.condition);
        }

        return false;
    }
}
