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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.condition.ConditionFactory.CF;
import com.landawn.abacus.util.N;

// TODO: Auto-generated Javadoc
/**
 * The Class NamedProperty.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class NamedProperty {

    /** The Constant instancePool. */
    private static final Map<String, NamedProperty> instancePool = new ConcurrentHashMap<>();

    /** The prop name. */
    // for Kryo
    final String propName;

    /**
     * Instantiates a new named property.
     */
    // For Kryo
    NamedProperty() {
        propName = null;
    }

    /**
     * Instantiates a new named property.
     *
     * @param propName
     */
    public NamedProperty(String propName) {
        this.propName = propName;
    }

    /**
     *
     * @param propName
     * @return
     */
    public static final NamedProperty of(String propName) {
        if (N.isNullOrEmpty(propName)) {
            throw new IllegalArgumentException("the property name can't be null or empty string.");
        }

        NamedProperty instance = instancePool.get(propName);

        if (instance == null) {
            instance = new NamedProperty(propName);
            instancePool.put(propName, instance);
        }

        return instance;
    }

    /**
     *
     * @param values
     * @return
     */
    public Equal eq(Object values) {
        return CF.eq(propName, values);
    }

    /**
     *
     * @param values
     * @return
     */
    @SafeVarargs
    public final Or eqOr(Object... values) {
        Or or = CF.or();

        for (Object propValue : values) {
            or.add(CF.eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param values
     * @return
     */
    public Or eqOr(Collection<?> values) {
        Or or = CF.or();

        for (Object propValue : values) {
            or.add(CF.eq(propName, propValue));
        }

        return or;
    }

    /**
     *
     * @param values
     * @return
     */
    public NotEqual ne(Object values) {
        return CF.ne(propName, values);
    }

    /**
     *
     * @param value
     * @return
     */
    public GreaterThan gt(Object value) {
        return CF.gt(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public GreaterEqual ge(Object value) {
        return CF.ge(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public LessThan lt(Object value) {
        return CF.lt(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public LessEqual le(Object value) {
        return CF.le(propName, value);
    }

    /**
     * Checks if is null.
     *
     * @return
     */
    public IsNull isNull() {
        return CF.isNull(propName);
    }

    /**
     * Checks if is not null.
     *
     * @return
     */
    public IsNotNull isNotNull() {
        return CF.isNotNull(propName);
    }

    /**
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    public Between bt(Object minValue, Object maxValue) {
        return CF.between(propName, minValue, maxValue);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like like(Object value) {
        return CF.like(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public Not notLike(Object value) {
        return CF.notLike(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like startsWith(Object value) {
        return CF.startsWith(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like endsWith(Object value) {
        return CF.endsWith(propName, value);
    }

    /**
     *
     * @param value
     * @return
     */
    public Like contains(Object value) {
        return CF.contains(propName, value);
    }

    /**
     *
     * @param values
     * @return
     */
    @SafeVarargs
    public final In in(Object... values) {
        return CF.in(propName, values);
    }

    /**
     *
     * @param values
     * @return
     */
    public In in(Collection<?> values) {
        return CF.in(propName, values);
    }

    /**
     *
     * @return
     */
    @Override
    public int hashCode() {
        return propName.hashCode();
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @Override
    public boolean equals(Object obj) {
        return this == obj || (obj instanceof NamedProperty && N.equals(((NamedProperty) obj).propName, propName));
    }

    /**
     *
     * @return
     */
    @Override
    public String toString() {
        return propName;
    }
}
