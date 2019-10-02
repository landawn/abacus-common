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

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;

// TODO: Auto-generated Javadoc
/**
 * The Class AbstractCondition.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public abstract class AbstractCondition implements Condition, Serializable, Cloneable {

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -8343776796087792859L;

    /** The operator. */
    protected final Operator operator;

    /**
     * Instantiates a new abstract condition.
     */
    // For Kryo
    AbstractCondition() {
        operator = null;
    }

    /**
     * Instantiates a new abstract condition.
     *
     * @param operator
     */
    protected AbstractCondition(Operator operator) {
        this.operator = operator;
    }

    /**
     * Gets the operator.
     *
     * @return
     */
    @Override
    public Operator getOperator() {
        return operator;
    }

    /**
     *
     * @param condition
     * @return
     */
    @Override
    public And and(Condition condition) {
        return new And(this, condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    @Override
    public Or or(Condition condition) {
        return new Or(this, condition);
    }

    /**
     *
     * @return
     */
    @Override
    public Not not() {
        return new Not(this);
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        AbstractCondition copy = null;

        try {
            copy = (AbstractCondition) super.clone();
        } catch (CloneNotSupportedException e) {
            // ignore, won't happen.
        }

        return (T) copy;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return toString(NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Parameter 2 string.
     *
     * @param parameter
     * @param namingPolicy
     * @return
     */
    protected static String parameter2String(Object parameter, NamingPolicy namingPolicy) {
        if (parameter == null) {
            return null;
        }

        if (parameter instanceof String) {
            return WD._QUOTATION_S + parameter.toString() + WD._QUOTATION_S;
        }

        if (parameter instanceof Condition) {
            return ((Condition) parameter).toString(namingPolicy);
        }

        return parameter.toString();
    }

    /**
     * Concat prop names.
     *
     * @param propNames
     * @return
     */
    protected static String concatPropNames(String... propNames) {
        if (N.isNullOrEmpty(propNames)) {
            return N.EMPTY_STRING;
        }

        int size = propNames.length;

        switch (size) {
            case 1:
                return propNames[0];

            case 2:
                return WD.PARENTHESES_L + propNames[0] + WD.COMMA_SPACE + propNames[1] + WD.PARENTHESES_R;

            case 3:
                return WD.PARENTHESES_L + propNames[0] + WD.COMMA_SPACE + propNames[1] + WD.COMMA_SPACE + propNames[2] + WD.PARENTHESES_R;

            default:
                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(WD._PARENTHESES_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(WD.COMMA_SPACE);
                        }

                        sb.append(propNames[i]);
                    }

                    sb.append(WD._PARENTHESES_R);

                    return sb.toString();

                } finally {
                    Objectory.recycle(sb);
                }
        }
    }

    /**
     * Concat prop names.
     *
     * @param propNames
     * @return
     */
    protected static String concatPropNames(Collection<String> propNames) {
        if (N.isNullOrEmpty(propNames)) {
            return N.EMPTY_STRING;
        }

        Iterator<String> it = propNames.iterator();
        int size = propNames.size();

        switch (size) {
            case 1:
                return it.next();

            case 2:
                return WD.PARENTHESES_L + it.next() + WD.COMMA_SPACE + it.next() + WD.PARENTHESES_R;

            case 3:
                return WD.PARENTHESES_L + it.next() + WD.COMMA_SPACE + it.next() + WD.COMMA_SPACE + it.next() + WD.PARENTHESES_R;

            default:

                final StringBuilder sb = Objectory.createStringBuilder();

                try {
                    sb.append(WD._PARENTHESES_L);

                    for (int i = 0; i < size; i++) {
                        if (i > 0) {
                            sb.append(WD.COMMA_SPACE);
                        }

                        sb.append(it.next());
                    }

                    sb.append(WD._PARENTHESES_R);

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
        }
    }
}
