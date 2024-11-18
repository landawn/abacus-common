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

package com.landawn.abacus.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * This object is used to store the properties' values of an object. So it should not set or get value for other
 * object's property.
 *
 */
@Internal
public final class MapEntity implements Serializable {

    private static final long serialVersionUID = -6595007303962724540L;

    private final String entityName;

    private final Map<String, Object> values = new HashMap<>();

    // For Kryo
    MapEntity() {
        this(Strings.EMPTY_STRING);
    }

    /**
     *
     * @param entityName
     */
    public MapEntity(final String entityName) {
        this.entityName = entityName;
    }

    /**
     *
     * @param entityName
     * @param props
     */
    public MapEntity(final String entityName, final Map<String, Object> props) {
        this(entityName);

        set(props);
    }

    //    /**
    //     *
    //     * @param entityName
    //     * @return
    //     */
    //    public static MapEntity valueOf(final String entityName) {
    //        return new MapEntity(entityName);
    //    }
    //
    //    /**
    //     *
    //     * @param entityName
    //     * @param props
    //     * @return
    //     */
    //    public static MapEntity valueOf(final String entityName, final Map<String, Object> props) {
    //        return new MapEntity(entityName, props);
    //    }

    public String entityName() {
        return entityName;
    }

    /**
     *
     * @param <T>
     * @param propName
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String propName) {
        if (NameUtil.isCanonicalName(entityName, propName)) {
            return (T) values.get(NameUtil.getSimpleName(propName));
        } else {
            return (T) values.get(propName);
        }
    }

    /**
     *
     * @param <T>
     * @param propName
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T> T get(final String propName, final Class<? extends T> targetType) {
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
    public MapEntity set(String propName, final Object propValue) {
        if (NameUtil.isCanonicalName(entityName, propName)) {
            propName = NameUtil.getSimpleName(propName);
        }

        values.put(propName, propValue);

        return this;
    }

    /**
     *
     * @param nameValues
     */
    public void set(final Map<String, Object> nameValues) {
        for (final Map.Entry<String, Object> entry : nameValues.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    /**
     *
     * @param propName
     * @return
     */
    @MayReturnNull
    public Object remove(String propName) {
        if (values.size() == 0) {
            return null;
        }

        if (NameUtil.isCanonicalName(entityName, propName)) {
            propName = NameUtil.getSimpleName(propName);
        }

        return values.remove(propName);
    }

    /**
     * Removes the all.
     *
     * @param propNames
     */
    public void removeAll(final Collection<String> propNames) { // NOSONAR
        for (final String propName : propNames) {
            remove(propName);
        }
    }

    /**
     *
     * @param propName
     * @return {@code true}, if successful
     */
    public boolean containsKey(final String propName) {
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
     * @see com.landawn.abacus.util.MapEntity#keySet()
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
     * @return {@code true}, if is empty
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    public MapEntity copy() {
        return new MapEntity(entityName, values);
    }

    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + entityName.hashCode();
        return (h * 31) + values.hashCode();
    }

    /**
     *
     * @param obj
     * @return {@code true}, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof final MapEntity other) {
            return N.equals(entityName, other.entityName) && N.equals(values, other.values);
        }

        return false;
    }

    @Override
    public String toString() {
        return values.toString();
    }

    /**
     *
     * @param entityName
     * @return
     */
    public static MapEntityBuilder builder(final String entityName) {
        return new MapEntityBuilder(entityName);
    }

    public static class MapEntityBuilder {
        private final MapEntity mapEntity;

        MapEntityBuilder(final String entityName) {
            mapEntity = new MapEntity(entityName);
        }

        /**
         *
         * @param idPropName
         * @param idPropVal
         * @return
         */
        public MapEntityBuilder put(final String idPropName, final Object idPropVal) {
            mapEntity.set(idPropName, idPropVal);

            return this;
        }

        public MapEntity build() {
            return mapEntity;
        }
    }
}
