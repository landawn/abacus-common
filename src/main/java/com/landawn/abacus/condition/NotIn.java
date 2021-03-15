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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li 
 */
public class NotIn extends AbstractCondition {
    private static final long serialVersionUID = 6819362182648251435L;

    // For Kryo
    final String propName;

    private List<?> values;

    // For Kryo
    NotIn() {
        propName = null;
    }

    public NotIn(String propName, Collection<?> values) {
        super(Operator.NOT_IN);

        N.checkArgNotNullOrEmpty(values, "'values' can't be null or empty");

        this.propName = propName;
        this.values = new ArrayList<>(values);
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
     * Gets the values.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<?> getValues() {
        return values;
    }

    /**
     * Sets the values.
     *
     * @param values the new values
     */
    public void setValues(List<?> values) {
        this.values = values;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        return values == null ? N.emptyList() : (List<Object>) values;
    }

    /**
     * Clear parameters.
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void clearParameters() {
        if (N.notNullOrEmpty(values)) {
            N.fill((List) values, null);
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
        NotIn copy = (NotIn) super.copy();

        copy.values = new ArrayList<>(values);

        return (T) copy;
    }

    /**
     *
     * @param namingPolicy
     * @return
     */
    @Override
    public String toString(NamingPolicy namingPolicy) {
        if (namingPolicy == NamingPolicy.NO_CHANGE) {
            return propName + WD._SPACE + getOperator().toString()
                    + Joiner.with(WD.COMMA_SPACE, WD.SPACE_PARENTHESES_L, WD.PARENTHESES_R).reuseCachedBuffer().appendAll(values).toString();
        } else {
            return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString()
                    + Joiner.with(WD.COMMA_SPACE, WD.SPACE_PARENTHESES_L, WD.PARENTHESES_R).reuseCachedBuffer().appendAll(values).toString();
        }
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + ((values == null) ? 0 : values.hashCode());

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

        if (obj instanceof NotIn) {
            NotIn other = (NotIn) obj;

            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(values, other.values);
        }

        return false;
    }
}
