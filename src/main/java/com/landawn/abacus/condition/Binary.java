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

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.WD;

// TODO: Auto-generated Javadoc
/**
 * The Class Binary.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class Binary extends AbstractCondition {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1552347171930293343L;

    /** The prop name. */
    // For Kryo
    final String propName;

    /** The prop value. */
    private Object propValue;

    /**
     * Instantiates a new binary.
     */
    // For Kryo
    Binary() {
        propName = null;
    }

    /**
     * Instantiates a new binary.
     *
     * @param propName
     * @param operator
     * @param propValue
     */
    public Binary(String propName, Operator operator, Object propValue) {
        super(operator);

        if (N.isNullOrEmpty(propName)) {
            throw new IllegalArgumentException("property name can't be null or empty.");
        }

        this.propName = propName;
        this.propValue = propValue;
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
     * Gets the prop value.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T getPropValue() {
        return (T) propValue;
    }

    /**
     * Sets the prop value.
     *
     * @param propValue the new prop value
     */
    public void setPropValue(Object propValue) {
        this.propValue = propValue;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @Override
    public List<Object> getParameters() {
        if ((propValue != null) && propValue instanceof Condition) {
            return ((Condition) propValue).getParameters();
        } else {
            return N.asList(propValue);
        }
    }

    /**
     * Clear parameters.
     */
    @Override
    public void clearParameters() {
        if ((propValue != null) && propValue instanceof Condition) {
            ((Condition) propValue).clearParameters();
        } else {
            propValue = null;
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
        Binary copy = (Binary) super.copy();

        if ((propValue != null) && propValue instanceof Condition) {
            copy.propValue = ((Condition) propValue).copy();
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
                return propName + WD._SPACE + getOperator().toString() + WD._SPACE + parameter2String(propValue, namingPolicy);

            case LOWER_CASE_WITH_UNDERSCORE:
                return ClassUtil.toLowerCaseWithUnderscore(propName) + WD._SPACE + getOperator().toString() + WD._SPACE
                        + parameter2String(propValue, namingPolicy);

            case UPPER_CASE_WITH_UNDERSCORE:
                return ClassUtil.toUpperCaseWithUnderscore(propName) + WD._SPACE + getOperator().toString() + WD._SPACE
                        + parameter2String(propValue, namingPolicy);

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
        h = (h * 31) + ((propValue == null) ? 0 : propValue.hashCode());

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

        if (obj instanceof Binary) {
            Binary other = (Binary) obj;

            return N.equals(propName, other.propName) && N.equals(operator, other.operator) && N.equals(propValue, other.propValue);
        }

        return false;
    }
}
