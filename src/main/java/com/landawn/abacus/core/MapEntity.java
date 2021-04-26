/*
 * Copyright (c) 2015, Haiyang Li.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.core;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.N;

/**
 * This object is used to store the properties' values of an object. So it should not set or get value for other
 * object's property.
 *
 * @author Haiyang Li
 * @since 0.8
 */
@Internal
public final class MapEntity extends AbstractDirtyMarker implements Serializable {

    private static final long serialVersionUID = -6595007303962724540L;

    private final Map<String, Object> values = new HashMap<>();

    // For Kryo
    MapEntity() {
        this(N.EMPTY_STRING);
    }

    public MapEntity(String entityName) {
        super(entityName);
    }

    public MapEntity(String entityName, Map<String, Object> props) {
        this(entityName);

        set(props);
    }

    /**
     *
     * @param entityName
     * @return
     */
    public static MapEntity valueOf(String entityName) {
        return new MapEntity(entityName);
    }

    /**
     *
     * @param entityName
     * @param props
     * @return
     */
    public static MapEntity valueOf(String entityName, Map<String, Object> props) {
        return new MapEntity(entityName, props);
    }

    /**
     *
     * @param <T>
     * @param propName
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String propName) {
        if (NameUtil.isCanonicalName(entityName, propName)) {
            return (T) values.get(NameUtil.getSimpleName(propName));
        } else {
            return (T) values.get(propName);
        }
    }

    /**
     * <br />
     * Node: To follow one of general design rules in {@code Abacus}, if there is a conversion behind when the source value is not assignable to the target type, put the {@code targetType} to last parameter of the method. 
     * Otherwise, put the {@code targetTpye} to the first parameter of the method. 
     *
     * @param propName
     * @param targetType
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String propName, Class<T> targetType) {
        Object propValue = get(propName);

        if (propValue == null) {
            propValue = N.defaultValueOf(targetType);
        }

        return N.convert(propValue, targetType);
    }

    /**
     *
     * @param propName
     * @param propValue
     * @return
     */
    public MapEntity set(String propName, Object propValue) {
        checkForzen();

        if (NameUtil.isCanonicalName(entityName, propName)) {
            propName = NameUtil.getSimpleName(propName);
        }

        values.put(propName, propValue);

        setUpdatedPropName(propName);

        return this;
    }

    /**
     *
     * @param nameValues
     */
    public void set(Map<String, Object> nameValues) {
        checkForzen();

        for (Map.Entry<String, Object> entry : nameValues.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    /**
     *
     * @param propName
     * @return
     */
    public Object remove(String propName) {
        checkForzen();

        if (values.size() == 0) {
            return null;
        }

        if (NameUtil.isCanonicalName(entityName, propName)) {
            propName = NameUtil.getSimpleName(propName);
        }

        dirtyPropNames.remove(propName);

        return values.remove(propName);
    }

    /**
     * Removes the all.
     *
     * @param propNames
     */
    public void removeAll(Collection<String> propNames) {
        checkForzen();

        for (String propName : propNames) {
            remove(propName);
        }
    }

    /**
     *
     * @param propName
     * @return true, if successful
     */
    public boolean containsKey(String propName) {
        if (values.size() == 0) {
            return false;
        }

        if (NameUtil.isCanonicalName(entityName, propName)) {
            return values.containsKey(NameUtil.getSimpleName(propName));

        } else {
            return values.containsKey(propName);
        }
    }

    /**
     * Returns the property names which have been set value.
     * 
     * @return a collection of signed property names
     * @see com.landawn.abacus.MapEntity#keySet()
     */
    public Set<String> keySet() {
        return values.keySet();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    public Map<String, Object> props() {
        return values;
    }

    public int size() {
        return values.size();
    }

    /**
     * Checks if is empty.
     *
     * @return true, if is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Clear.
     */
    public void clear() {
        checkForzen();

        dirtyPropNames().clear();

        values.clear();
    }

    public MapEntity copy() {
        final MapEntity copy = new MapEntity(this.entityName, this.values);

        copy.version = this.version;
        copy.dirtyPropNames.clear();
        copy.dirtyPropNames.addAll(this.dirtyPropNames);

        return copy;
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + entityName.hashCode();
        h = (h * 31) + values.hashCode();

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

        if (obj instanceof MapEntity) {
            MapEntity other = (MapEntity) obj;

            return N.equals(entityName, other.entityName) && N.equals(values, other.values);
        }

        return false;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    /**
     * Signed prop names.
     *
     * @return
     */
    @Override
    public Set<String> signedPropNames() {
        return values.keySet();
    }

    /**
     * Sets the updated prop name.
     *
     * @param propName the new updated prop name
     */
    @Override
    protected void setUpdatedPropName(String propName) {
        dirtyPropNames.add(propName);
    }

    /**
     * Sets the updated prop names.
     *
     * @param propNames the new updated prop names
     */
    @Override
    protected void setUpdatedPropNames(Collection<String> propNames) {
        dirtyPropNames.addAll(propNames);
    }

    /**
     * Inits the.
     */
    @Override
    protected void init() {
        dirtyPropNames = N.newHashSet();
    }
}
