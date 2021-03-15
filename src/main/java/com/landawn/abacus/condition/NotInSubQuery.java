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

import java.util.Collection;
import java.util.List;

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.StringUtil;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li 
 */
public class NotInSubQuery extends AbstractCondition {
    private static final long serialVersionUID = 3151853530575587557L;

    // For Kryo
    final String propName;

    // For Kryo
    final Collection<String> propNames;

    private SubQuery subQuery;

    // For Kryo
    NotInSubQuery() {
        propName = null;
        propNames = null;
    }

    public NotInSubQuery(String propName, SubQuery subQuery) {
        super(Operator.NOT_IN);

        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propName = propName;
        this.subQuery = subQuery;
        this.propNames = null;
    }

    public NotInSubQuery(Collection<String> propNames, SubQuery subQuery) {
        super(Operator.NOT_IN);

        N.checkArgNotNullOrEmpty(propNames, "propNames");
        N.checkArgNotNull(subQuery, "'subQuery' can't be null or empty");

        this.propNames = propNames;
        this.subQuery = subQuery;
        this.propName = null;
    }

    public String getPropName() {
        return propName;
    }

    public Collection<String> getPropNames() {
        return propNames;
    }

    @SuppressWarnings("unchecked")
    public SubQuery getSubQuery() {
        return subQuery;
    }

    public void setSubQuery(SubQuery subQuery) {
        this.subQuery = subQuery;
    }

    @Override
    public List<Object> getParameters() {
        return subQuery.getParameters();
    }

    @Override
    public void clearParameters() {
        subQuery.clearParameters();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Condition> T copy() {
        NotInSubQuery copy = (NotInSubQuery) super.copy();

        copy.subQuery = subQuery.copy();

        return (T) copy;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + (N.notNullOrEmpty(propName) ? N.hashCode(propName) : N.hashCode(propNames));
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

        if (obj instanceof NotInSubQuery) {
            NotInSubQuery other = (NotInSubQuery) obj;

            return N.equals(propName, other.propName) && N.equals(propNames, other.propNames) && N.equals(operator, other.operator)
                    && N.equals(subQuery, other.subQuery);
        }

        return false;
    }

    @Override
    public String toString(final NamingPolicy namingPolicy) {
        if (N.notNullOrEmpty(propName)) {
            if (namingPolicy == NamingPolicy.NO_CHANGE) {
                return propName + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy) + WD.PARENTHESES_R;
            } else {
                return namingPolicy.convert(propName) + WD._SPACE + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                        + WD.PARENTHESES_R;
            }
        } else {
            if (namingPolicy == NamingPolicy.NO_CHANGE) {
                return "(" + StringUtil.join(propNames, ", ") + ") " + getOperator().toString() + WD.SPACE_PARENTHESES_L + subQuery.toString(namingPolicy)
                        + WD.PARENTHESES_R;
            } else {
                final Throwables.Function<String, String, RuntimeException> func = new Throwables.Function<String, String, RuntimeException>() {
                    @Override
                    public String apply(String t) throws RuntimeException {
                        return namingPolicy.convert(t);
                    }
                };

                return "(" + StringUtil.join(N.map(propNames, func), ", ") + ") " + getOperator().toString() + WD.SPACE_PARENTHESES_L
                        + subQuery.toString(namingPolicy) + WD.PARENTHESES_R;
            }
        }
    }

}
