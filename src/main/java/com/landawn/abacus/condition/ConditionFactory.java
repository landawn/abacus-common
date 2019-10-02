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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.EntityId;
import com.landawn.abacus.condition.Expression.Expr;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SortDirection;
import com.landawn.abacus.util.WD;

// TODO: Auto-generated Javadoc
/**
 * A factory for creating Condition objects.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class ConditionFactory {
    /**
     * Expression with question mark literal: "?".
     */
    public static final Expression QME = Expr.of(WD.QUESTION_MARK);

    /** The Constant ASC. */
    public static final SortDirection ASC = SortDirection.ASC;

    /** The Constant DESC. */
    public static final SortDirection DESC = SortDirection.DESC;

    /** The Constant ALWAYS_TRUE. */
    private static final Expression ALWAYS_TRUE = Expression.of("1 < 2");

    /** The Constant ALWAYS_FALSE. */
    private static final Expression ALWAYS_FALSE = Expression.of("1 > 2");

    /**
     * Instantiates a new condition factory.
     */
    private ConditionFactory() {
        // No instance;
    }

    /**
     *
     * @return
     */
    public static Expression alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     *
     * @return
     */
    @Deprecated
    public static Expression alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     *
     * @param propName
     * @return
     */
    public static NamedProperty namedProperty(final String propName) {
        return NamedProperty.of(propName);
    }

    /**
     *
     * @param literal
     * @return
     */
    public static Expression expr(final String literal) {
        return Expression.of(literal);
    }

    /**
     *
     * @param propName
     * @param operator
     * @param propValue
     * @return
     */
    public static Binary binary(final String propName, final Operator operator, final Object propValue) {
        return new Binary(propName, operator, propValue);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Equal equal(final String propName, final Object propValue) {
        return new Equal(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static Equal equal(final String propName) {
        return equal(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Equal eq(final String propName, final Object propValue) {
        return new Equal(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static Equal eq(final String propName) {
        return eq(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValues
     * @return
     */
    @SafeVarargs
    public static Or eqOr(final String propName, final Object... propValues) {
        N.checkArgNotNullOrEmpty(propValues, "propValues");

        Or or = CF.or();

        for (Object propValue : propValues) {
            or.add(eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param propName
     * @param propValues
     * @return
     */
    public static Or eqOr(final String propName, final Collection<?> propValues) {
        N.checkArgNotNullOrEmpty(propValues, "propValues");

        Or or = CF.or();

        for (Object propValue : propValues) {
            or.add(eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param props
     * @return
     */
    public static Or eqOr(final Map<String, ?> props) {
        N.checkArgNotNullOrEmpty(props, "props");

        final Collection<String> selectPropNames = props.keySet();
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return or(eq(propName, props.get(propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, props.get(propName1)).or(eq(propName2, props.get(propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, props.get(propName));
            }

            return or(conds);
        }
    }

    /**
     *
     * @param entity
     * @return
     */
    public static Or eqOr(final Object entity) {
        return eqOr(entity, ClassUtil.getPropNameList(entity.getClass()));
    }

    /**
     *
     * @param entity
     * @param selectPropNames
     * @return
     */
    public static Or eqOr(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotNullOrEmpty(selectPropNames, "selectPropNames");

        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return or(eq(propName, ClassUtil.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, ClassUtil.getPropValue(entity, propName1)).or(eq(propName2, ClassUtil.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, ClassUtil.getPropValue(entity, propName));
            }

            return or(conds);
        }
    }

    /**
     *
     * @param props
     * @return
     */
    public static And eqAnd(final Map<String, ?> props) {
        N.checkArgNotNullOrEmpty(props, "props");

        final Collection<String> selectPropNames = props.keySet();
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(eq(propName, props.get(propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, props.get(propName1)).and(eq(propName2, props.get(propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, props.get(propName));
            }

            return and(conds);
        }
    }

    /**
     *
     * @param entity
     * @return
     */
    public static And eqAnd(final Object entity) {
        return eqAnd(entity, ClassUtil.getPropNameList(entity.getClass()));
    }

    /**
     *
     * @param entity
     * @param selectPropNames
     * @return
     */
    public static And eqAnd(final Object entity, final Collection<String> selectPropNames) {
        N.checkArgNotNullOrEmpty(selectPropNames, "selectPropNames");

        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(eq(propName, ClassUtil.getPropValue(entity, propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, ClassUtil.getPropValue(entity, propName1)).and(eq(propName2, ClassUtil.getPropValue(entity, propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, ClassUtil.getPropValue(entity, propName));
            }

            return and(conds);
        }
    }

    /**
     * Eq and or.
     *
     * @param propsList
     * @return
     */
    public static Or eqAndOr(final List<? extends Map<String, ?>> propsList) {
        N.checkArgNotNullOrEmpty(propsList, "propsList");

        final Condition[] conds = new Condition[propsList.size()];

        for (int i = 0, size = propsList.size(); i < size; i++) {
            conds[i] = eqAnd(propsList.get(i));
        }

        return or(conds);
    }

    /**
     * Eq and or.
     *
     * @param entities
     * @return
     */
    public static Or eqAndOr(final Collection<?> entities) {
        N.checkArgNotNullOrEmpty(entities, "entities");

        return eqAndOr(entities, ClassUtil.getPropNameList(N.firstNonNull(entities).orNull().getClass()));
    }

    /**
     * Eq and or.
     *
     * @param entities
     * @param selectPropNames
     * @return
     */
    public static Or eqAndOr(final Collection<?> entities, final Collection<String> selectPropNames) {
        N.checkArgNotNullOrEmpty(entities, "entities");
        N.checkArgNotNullOrEmpty(selectPropNames, "selectPropNames");

        final Iterator<?> iter = entities.iterator();
        final Condition[] conds = new Condition[entities.size()];

        for (int i = 0, size = entities.size(); i < size; i++) {
            conds[i] = eqAnd(iter.next(), selectPropNames);
        }

        return or(conds);
    }

    /**
     *
     * @param entity
     * @return
     */
    public static And id2Cond(final EntityId entityId) {
        N.checkArgNotNull(entityId, "entityId");

        final Collection<String> selectPropNames = entityId.keySet();
        final Iterator<String> iter = selectPropNames.iterator();

        if (selectPropNames.size() == 1) {
            final String propName = iter.next();
            return and(eq(propName, entityId.get(propName)));
        } else if (selectPropNames.size() == 2) {
            final String propName1 = iter.next();
            final String propName2 = iter.next();
            return eq(propName1, entityId.get(propName1)).and(eq(propName2, entityId.get(propName2)));
        } else {
            final Condition[] conds = new Condition[selectPropNames.size()];
            String propName = null;

            for (int i = 0, size = selectPropNames.size(); i < size; i++) {
                propName = iter.next();
                conds[i] = CF.eq(propName, entityId.get(propName));
            }

            return and(conds);
        }
    }

    public static Or id2Cond(final Collection<? extends EntityId> entityIds) {
        N.checkArgNotNullOrEmpty(entityIds, "entityIds");

        final Iterator<? extends EntityId> iter = entityIds.iterator();
        final Condition[] conds = new Condition[entityIds.size()];

        for (int i = 0, size = entityIds.size(); i < size; i++) {
            conds[i] = CF.id2Cond(iter.next());
        }

        return CF.or(conds);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotEqual notEqual(final String propName, final Object propValue) {
        return new NotEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static NotEqual notEqual(final String propName) {
        return notEqual(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static NotEqual ne(final String propName, final Object propValue) {
        return new NotEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static NotEqual ne(final String propName) {
        return ne(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterThan greaterThan(final String propName, final Object propValue) {
        return new GreaterThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterThan greaterThan(final String propName) {
        return greaterThan(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterThan gt(final String propName, final Object propValue) {
        return new GreaterThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterThan gt(final String propName) {
        return gt(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterEqual greaterEqual(final String propName, final Object propValue) {
        return new GreaterEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterEqual greaterEqual(final String propName) {
        return greaterEqual(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static GreaterEqual ge(final String propName, final Object propValue) {
        return new GreaterEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static GreaterEqual ge(final String propName) {
        return ge(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessThan lessThan(final String propName, final Object propValue) {
        return new LessThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessThan lessThan(final String propName) {
        return lessThan(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessThan lt(final String propName, final Object propValue) {
        return new LessThan(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessThan lt(final String propName) {
        return lt(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessEqual lessEqual(final String propName, final Object propValue) {
        return new LessEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessEqual lessEqual(final String propName) {
        return lessEqual(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static LessEqual le(final String propName, final Object propValue) {
        return new LessEqual(propName, propValue);
    }

    /**
     * It's for parameterized SQL with question mark or named parameters.
     *
     * @param propName
     * @return
     * @see com.landawn.abacus.util.SQLBuilder
     */
    public static LessEqual le(final String propName) {
        return le(propName, QME);
    }

    /**
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static Between between(final String propName, final Object minValue, final Object maxValue) {
        return new Between(propName, minValue, maxValue);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static Between between(final String propName) {
        return new Between(propName, CF.QME, CF.QME);
    }

    /**
     *
     * @param propName
     * @param minValue
     * @param maxValue
     * @return
     */
    public static Between bt(final String propName, final Object minValue, final Object maxValue) {
        return new Between(propName, minValue, maxValue);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static Between bt(final String propName) {
        return new Between(propName, CF.QME, CF.QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like like(final String propName, final Object propValue) {
        return new Like(propName, propValue);
    }

    /**
     *
     * @param propName
     * @return
     */
    public static Like like(final String propName) {
        return like(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Not notLike(final String propName, final Object propValue) {
        return like(propName, propValue).not();
    }

    /**
     *
     * @param propName
     * @return
     */
    public static Not notLike(final String propName) {
        return notLike(propName, QME);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like contains(final String propName, final Object propValue) {
        return new Like(propName, WD._PERCENT + N.stringOf(propValue) + WD._PERCENT);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like startsWith(final String propName, final Object propValue) {
        return new Like(propName, N.stringOf(propValue) + WD._PERCENT);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Like endsWith(final String propName, final Object propValue) {
        return new Like(propName, WD._PERCENT + N.stringOf(propValue));
    }

    /**
     * Checks if is null.
     *
     * @param propName
     * @return
     */
    public static IsNull isNull(final String propName) {
        return new IsNull(propName);
    }

    /**
     * Checks if is not null.
     *
     * @param propName
     * @return
     */
    public static IsNotNull isNotNull(final String propName) {
        return new IsNotNull(propName);
    }

    /**
     * Checks if is na N.
     *
     * @param propName
     * @return
     */
    public static IsNaN isNaN(final String propName) {
        return new IsNaN(propName);
    }

    /**
     * Checks if is not na N.
     *
     * @param propName
     * @return
     */
    public static IsNotNaN isNotNaN(final String propName) {
        return new IsNotNaN(propName);
    }

    /**
     * Checks if is infinite.
     *
     * @param propName
     * @return
     */
    public static IsInfinite isInfinite(final String propName) {
        return new IsInfinite(propName);
    }

    /**
     * Checks if is not infinite.
     *
     * @param propName
     * @return
     */
    public static IsNotInfinite isNotInfinite(final String propName) {
        return new IsNotInfinite(propName);
    }

    /**
     * Checks if is.
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static Is is(final String propName, final Object propValue) {
        return new Is(propName, propValue);
    }

    /**
     * Checks if is not.
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static IsNot isNot(final String propName, final Object propValue) {
        return new IsNot(propName, propValue);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public static XOR xor(final String propName, final Object propValue) {
        return new XOR(propName, propValue);
    }

    /**
     *
     * @param conditions
     * @return
     */
    @SafeVarargs
    public static Or or(final Condition... conditions) {
        return new Or(conditions);
    }

    /**
     *
     * @param conditions
     * @return
     */
    public static Or or(final Collection<? extends Condition> conditions) {
        return new Or(conditions);
    }

    /**
     *
     * @param conditions
     * @return
     */
    @SafeVarargs
    public static And and(final Condition... conditions) {
        return new And(conditions);
    }

    /**
     *
     * @param conditions
     * @return
     */
    public static And and(final Collection<? extends Condition> conditions) {
        return new And(conditions);
    }

    /**
     *
     * @param operator
     * @param conditions
     * @return
     */
    @SafeVarargs
    public static Junction junction(final Operator operator, final Condition... conditions) {
        return new Junction(operator, conditions);
    }

    /**
     *
     * @param operator
     * @param conditions
     * @return
     */
    public static Junction junction(final Operator operator, final Collection<? extends Condition> conditions) {
        return new Junction(operator, conditions);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Where where(final Condition condition) {
        return new Where(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Where where(final String condition) {
        return new Where(expr(condition));
    }

    /**
     *
     * @param propNames
     * @return
     */
    @SafeVarargs
    public static GroupBy groupBy(final String... propNames) {
        return new GroupBy(propNames);
    }

    /**
     *
     * @param propName
     * @param direction
     * @return
     */
    public static GroupBy groupBy(final String propName, final SortDirection direction) {
        return new GroupBy(propName, direction);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static GroupBy groupBy(final Collection<String> propNames) {
        return groupBy(propNames, SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @param direction
     * @return
     */
    public static GroupBy groupBy(final Collection<String> propNames, final SortDirection direction) {
        return new GroupBy(propNames, direction);
    }

    /**
     *
     * @param orders
     * @return
     */
    public static GroupBy groupBy(final Map<String, SortDirection> orders) {
        return new GroupBy(orders);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static GroupBy groupBy(final Condition condition) {
        return new GroupBy(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Having having(final Condition condition) {
        return new Having(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Having having(final String condition) {
        return new Having(expr(condition));
    }

    /**
     *
     * @param propNames
     * @return
     */
    @SafeVarargs
    public static OrderBy orderBy(final String... propNames) {
        return new OrderBy(propNames);
    }

    /**
     *
     * @param propName
     * @param direction
     * @return
     */
    public static OrderBy orderBy(final String propName, final SortDirection direction) {
        return new OrderBy(propName, direction);
    }

    /**
     *
     * @param propNames
     * @return
     */
    public static OrderBy orderBy(final Collection<String> propNames) {
        return orderBy(propNames, SortDirection.ASC);
    }

    /**
     *
     * @param propNames
     * @param direction
     * @return
     */
    public static OrderBy orderBy(final Collection<String> propNames, final SortDirection direction) {
        return new OrderBy(propNames, direction);
    }

    /**
     *
     * @param orders
     * @return
     */
    public static OrderBy orderBy(final Map<String, SortDirection> orders) {
        return new OrderBy(orders);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static OrderBy orderBy(final Condition condition) {
        return new OrderBy(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static On on(final Condition condition) {
        return new On(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static On on(final String condition) {
        return new On(expr(condition));
    }

    /**
     *
     * @param propName
     * @param anoPropName
     * @return
     */
    public static On on(final String propName, final String anoPropName) {
        return new On(propName, anoPropName);
    }

    /**
     *
     * @param propNamePair
     * @return
     */
    public static On on(final Map<String, String> propNamePair) {
        return new On(propNamePair);
    }

    /**
     * It's recommended to use {@code On}, instead of {@code Using}.
     *
     * @param columnNames
     * @return
     */
    @Deprecated
    @SafeVarargs
    public static Using using(final String... columnNames) {
        return new Using(columnNames);
    }

    /**
     * It's recommended to use {@code On}, instead of {@code Using}.
     *
     * @param columnNames
     * @return
     */
    @Deprecated
    public static Using using(final Collection<String> columnNames) {
        return new Using(columnNames);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static Join join(final String joinEntity) {
        return new Join(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static Join join(final String joinEntity, final Condition condition) {
        return new Join(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static Join join(final Collection<String> joinEntities, final Condition condition) {
        return new Join(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static LeftJoin leftJoin(final String joinEntity) {
        return new LeftJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static LeftJoin leftJoin(final String joinEntity, final Condition condition) {
        return new LeftJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static LeftJoin leftJoin(final Collection<String> joinEntities, final Condition condition) {
        return new LeftJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static RightJoin rightJoin(final String joinEntity) {
        return new RightJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static RightJoin rightJoin(final String joinEntity, final Condition condition) {
        return new RightJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static RightJoin rightJoin(final Collection<String> joinEntities, final Condition condition) {
        return new RightJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static CrossJoin crossJoin(final String joinEntity) {
        return new CrossJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static CrossJoin crossJoin(final String joinEntity, final Condition condition) {
        return new CrossJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static CrossJoin crossJoin(final Collection<String> joinEntities, final Condition condition) {
        return new CrossJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static FullJoin fullJoin(final String joinEntity) {
        return new FullJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static FullJoin fullJoin(final String joinEntity, final Condition condition) {
        return new FullJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static FullJoin fullJoin(final Collection<String> joinEntities, final Condition condition) {
        return new FullJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static InnerJoin innerJoin(final String joinEntity) {
        return new InnerJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static InnerJoin innerJoin(final String joinEntity, final Condition condition) {
        return new InnerJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static InnerJoin innerJoin(final Collection<String> joinEntities, final Condition condition) {
        return new InnerJoin(joinEntities, condition);
    }

    /**
     *
     * @param joinEntity
     * @return
     */
    public static NaturalJoin naturalJoin(final String joinEntity) {
        return new NaturalJoin(joinEntity);
    }

    /**
     *
     * @param joinEntity
     * @param condition
     * @return
     */
    public static NaturalJoin naturalJoin(final String joinEntity, final Condition condition) {
        return new NaturalJoin(joinEntity, condition);
    }

    /**
     *
     * @param joinEntities
     * @param condition
     * @return
     */
    public static NaturalJoin naturalJoin(final Collection<String> joinEntities, final Condition condition) {
        return new NaturalJoin(joinEntities, condition);
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static In in(final String propName, final Object[] values) {
        return in(propName, Arrays.asList(values));
    }

    /**
     *
     * @param propName
     * @param values
     * @return
     */
    public static In in(final String propName, final Collection<?> values) {
        return new In(propName, values);
    }

    //    public static Condition in(final String propName, final SubQuery condition) {
    //        return new Binary(propName, Operator.IN, condition);
    //    }
    //
    //    public static Condition in(final Collection<String> propNames, final SubQuery condition) {
    //        return new Binary(AbstractCondition.concatPropNames(propNames), Operator.IN, condition);
    //    }

    /**
     *
     * @param condition
     * @return
     */
    public static All all(final SubQuery condition) {
        return new All(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Any any(final SubQuery condition) {
        return new Any(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Some some(final SubQuery condition) {
        return new Some(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Exists exists(final SubQuery condition) {
        return new Exists(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Not notExists(final SubQuery condition) {
        return exists(condition).not();
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Union union(final SubQuery condition) {
        return new Union(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static UnionAll unionAll(final SubQuery condition) {
        return new UnionAll(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Except except(final SubQuery condition) {
        return new Except(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Intersect intersect(final SubQuery condition) {
        return new Intersect(condition);
    }

    /**
     *
     * @param condition
     * @return
     */
    public static Minus minus(final SubQuery condition) {
        return new Minus(condition);
    }

    /**
     *
     * @param operator
     * @param condition
     * @return
     */
    public static Cell cell(final Operator operator, final Condition condition) {
        return new Cell(operator, condition);
    }

    /**
     *
     * @param entityName
     * @param propNames
     * @param condition
     * @return
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final Condition condition) {
        return new SubQuery(entityName, propNames, condition);
    }

    /**
     *
     * @param entityName
     * @param propNames
     * @param condition
     * @return
     */
    public static SubQuery subQuery(final String entityName, final Collection<String> propNames, final String condition) {
        return new SubQuery(entityName, propNames, expr(condition));
    }

    /**
     *
     * @param entityName
     * @param sql
     * @return
     */
    public static SubQuery subQuery(final String entityName, final String sql) {
        return new SubQuery(entityName, sql);
    }

    /**
     *
     * @param count
     * @return
     */
    public static Limit limit(final int count) {
        return new Limit(count);
    }

    /**
     *
     * @param offset
     * @param count
     * @return
     */
    public static Limit limit(final int offset, final int count) {
        return new Limit(offset, count);
    }

    /**
     *
     * @return
     */
    public static Criteria criteria() {
        return new Criteria();
    }

    /**
     * The Class CF.
     */
    public static final class CF extends ConditionFactory {

        /**
         * Instantiates a new cf.
         */
        private CF() {
        }
    }
}
