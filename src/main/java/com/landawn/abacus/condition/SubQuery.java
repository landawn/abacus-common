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

import static com.landawn.abacus.util.WD.COMMA_SPACE;
import static com.landawn.abacus.util.WD._SPACE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NamingPolicy;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class SubQuery extends AbstractCondition {

    private static final long serialVersionUID = -2791944344613082244L;

    // For Kryo
    final String entityName;

    // For Kryo
    final Class<?> entityClass;

    private Collection<String> propNames;

    // For Kryo
    final String sql;

    /**
     * Field condition.
     */
    private Condition condition;

    // For Kryo
    SubQuery() {
        entityName = null;
        entityClass = null;
        sql = null;
    }

    public SubQuery(String entityName, String sql) {
        super(Operator.EMPTY);
        this.entityName = entityName;
        this.entityClass = null;

        if (N.isNullOrEmpty(sql)) {
            throw new IllegalArgumentException("The sql script can't be null or empty.");
        }

        this.propNames = null;
        this.condition = null;
        this.sql = sql;
    }

    public SubQuery(String entityName, Collection<String> propNames, Condition condition) {
        super(Operator.EMPTY);
        this.entityName = entityName;
        this.entityClass = null;
        this.propNames = propNames;
        if (condition == null || CriteriaUtil.isClause(condition) || condition instanceof Expression) {
            this.condition = condition;
        } else {
            this.condition = CF.where(condition);
        }

        this.sql = null;
    }

    public SubQuery(Class<?> entityClass, Collection<String> propNames, Condition condition) {
        super(Operator.EMPTY);
        this.entityName = ClassUtil.getSimpleClassName(entityClass);
        this.entityClass = entityClass;
        this.propNames = propNames;
        if (condition == null || CriteriaUtil.isClause(condition) || condition instanceof Expression) {
            this.condition = condition;
        } else {
            this.condition = CF.where(condition);
        }

        this.sql = null;
    }

    /**
     * Gets the sql.
     *
     * @return
     */
    public String getSql() {
        return sql;
    }

    public String getEntityName() {
        return entityName;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    /**
     * Gets the select prop names.
     *
     * @return
     */
    public Collection<String> getSelectPropNames() {
        return propNames;
    }

    /**
     * Gets the condition.
     *
     * @return
     */
    public Condition getCondition() {
        return condition;
    }

    /**
     * Gets the parameters.
     *
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<Object> getParameters() {
        return condition == null ? N.<Object> emptyList() : condition.getParameters();
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
        SubQuery result = (SubQuery) super.copy();

        if (propNames != null) {
            result.propNames = new ArrayList<>(propNames);
        }

        if (condition != null) {
            result.condition = condition.copy();
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
        if (sql == null) {
            final StringBuilder sb = Objectory.createStringBuilder();

            try {
                sb.append(WD.SELECT);
                sb.append(_SPACE);

                int i = 0;

                for (String propName : propNames) {
                    if (i++ > 0) {
                        sb.append(COMMA_SPACE);
                    }

                    sb.append(propName);
                }

                sb.append(_SPACE);
                sb.append(WD.FROM);

                sb.append(_SPACE);
                sb.append(entityName);

                if (condition != null) {
                    sb.append(_SPACE);

                    sb.append(condition.toString(namingPolicy));
                }

                return sb.toString();
            } finally {
                Objectory.recycle(sb);
            }

        } else {
            return sql;
        }
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + ((sql == null) ? 0 : sql.hashCode());
        h = (h * 31) + ((entityName == null) ? 0 : entityName.hashCode());
        h = (h * 31) + ((propNames == null) ? 0 : propNames.hashCode());
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

        if (obj instanceof SubQuery) {
            SubQuery other = (SubQuery) obj;

            return N.equals(sql, other.sql) && N.equals(entityName, other.entityName) && N.equals(propNames, other.propNames)
                    && N.equals(condition, other.condition);
        }

        return false;
    }
}
