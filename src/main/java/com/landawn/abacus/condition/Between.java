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

import java.util.ArrayList;
import java.util.List;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

// TODO: Auto-generated Javadoc
/**
 * The Class Between.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Between extends AbstractCondition {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 486757046031623324L;

    /** The prop name. */
    // For Kryo
    final String propName;

    /** The min value. */
    private Object minValue;

    /** The max value. */
    private Object maxValue;

    /**
     * Instantiates a new between.
     */
    // For Kryo
    Between() {
        propName = null;
    }

    /**
     * Instantiates a new between.
     *
     * @param propName
     * @param minValue
     * @param maxValue
     */
    public Between(String propName, Object minValue, Object maxValue) {
        super(Operator.BETWEEN);

        if (N.isNullOrEmpty(propName)) {
            throw new IllegalArgumentException("property name can't be null or empty.");
        }

        this.propName = propName;
        this.minValue = minValue;
        this.maxValue = maxValue;
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
     * Gets the min value.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getMinValue() {
        return (T) minValue;
    }

    /**
     * Sets the min value.
     *
     * @param minValue the new min value
     */
    public void setMinValue(Object minValue) {
        this.minValue = minValue;
    }

    /**
     * Gets the max value.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getMaxValue() {
        return (T) maxValue;
    }

    /**
     * Sets the max value.
     *
     * @param maxValue the new max value
     */
    public void setMaxValue(Object maxValue) {
        this.maxValue = maxValue;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        List<Object> parameters = new ArrayList<>();

        if ((minValue != null) && minValue instanceof Condition) {
            parameters.addAll(((Condition) minValue).getParameters());
        } else {
            parameters.add(minValue);
        }

        if ((maxValue != null) && maxValue instanceof Condition) {
            parameters.addAll(((Condition) maxValue).getParameters());
        } else {
            parameters.add(maxValue);
        }

        return parameters;
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        if ((minValue != null) && minValue instanceof Condition) {
            ((Condition) minValue).getParameters().clear();
        } else {
            minValue = null;
        }

        if ((maxValue != null) && maxValue instanceof Condition) {
            ((Condition) maxValue).getParameters().clear();
        } else {
            maxValue = null;
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
        Between copy = (Between) super.copy();

        if ((minValue != null) && minValue instanceof Condition) {
            copy.minValue = ((Condition) minValue).copy();
        }

        if ((minValue != null) && maxValue instanceof Condition) {
            copy.maxValue = ((Condition) maxValue).copy();
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
        switch (namingPolicy) {
            case LOWER_CAMEL_CASE:
                return propName + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + parameter2String(minValue, namingPolicy) + WD.COMMA_SPACE
                        + parameter2String(maxValue, namingPolicy) + WD._PARENTHESES_R;

            case LOWER_CASE_WITH_UNDERSCORE:
                return ClassUtil.toLowerCaseWithUnderscore(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L
                        + parameter2String(minValue, namingPolicy) + WD.COMMA_SPACE + parameter2String(maxValue, namingPolicy) + WD._PARENTHESES_R;

            case UPPER_CASE_WITH_UNDERSCORE:
                return ClassUtil.toUpperCaseWithUnderscore(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L
                        + parameter2String(minValue, namingPolicy) + WD.COMMA_SPACE + parameter2String(maxValue, namingPolicy) + WD._PARENTHESES_R;

            default:
                throw new IllegalArgumentException("Unsupported naming policy: " + namingPolicy);
        }
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + propName.hashCode();
        h = (h * 31) + operator.hashCode();
        h = (h * 31) + ((minValue == null) ? 0 : minValue.hashCode());
        h = (h * 31) + ((maxValue == null) ? 0 : maxValue.hashCode());

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

        if (obj instanceof Between) {
            Between other = (Between) obj;

            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(minValue, other.minValue)
                    && N.equals(maxValue, other.maxValue);
        }

        return false;
    }
}
