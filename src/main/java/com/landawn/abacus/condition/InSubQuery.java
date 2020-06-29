/*
 * Copyright (C) 2020 HaiYang Li
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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 1.10.12
 */
public class InSubQuery extends AbstractCondition {
    private static final long serialVersionUID = 1212947034078244134L;

    // For Kryo
    final String propName;

    private SubQuery subQuery;

    // For Kryo
    InSubQuery() {
        propName = null;
    }

    public InSubQuery(String propName, SubQuery subQuery) {
        super(Operator.IN);

        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propName = propName;
        this.subQuery = subQuery;
    }

    /**
     * Gets the prop name.
     *
     * @return
     */
    public String getPropName() {
        return propName;
    }

    /**
     * Gets the subQuery.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public SubQuery getSubQuery() {
        return subQuery;
    }

    /**
     * Sets the subQuery.
     *
     * @param subQuery the new subQuery
     */
    public void setSubQuery(SubQuery subQuery) {
        this.subQuery = subQuery;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        return subQuery.getParameters();
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        subQuery.clearParameters();
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        InSubQuery copy = (InSubQuery) super.copy();

        copy.subQuery = subQuery.copy();

        return (T) copy;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        if (namingPolicy == NamingPolicy.LOWER_CAMEL_CASE) {
            return propName + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy) + WD.PARENTHESES_R;
        } else {
            return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                    + WD.PARENTHESES_R;
        }
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + ((subQuery == null) ? 0 : subQuery.hashCode());

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

        if (obj instanceof InSubQuery) {
            InSubQuery other = (InSubQuery) obj;

            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(subQuery, other.subQuery);
        }

        return false;
    }
}
