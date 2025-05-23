/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.cs;

public class JSONDeserializationConfig extends DeserializationConfig<JSONDeserializationConfig> {

    private boolean ignoreNullOrEmpty = false;

    private boolean readNullToEmpty = false;

    @SuppressWarnings("rawtypes")
    private Class<? extends Map> mapInstanceType = HashMap.class;

    private Map<String, BiConsumer<? super Collection<?>, ?>> propHandlerMap = null;

    public boolean ignoreNullOrEmpty() {
        return ignoreNullOrEmpty;
    }

    /**
     * Won't set/add/put the value to bean/array/list/map if it's {@code null} or empty {@code CharSequence/Array/Collection/Map}.
     *
     * @param ignoreNullOrEmpty
     * @return
     */
    public JSONDeserializationConfig ignoreNullOrEmpty(final boolean ignoreNullOrEmpty) {
        this.ignoreNullOrEmpty = ignoreNullOrEmpty;

        return this;
    }

    //    /**
    //     *
    //     *
    //     * @return
    //     * @deprecated Use {@link #readNullToEmpty()} instead
    //     */
    //    @Deprecated
    //    public boolean nullToEmpty() {
    //        return readNullToEmpty();
    //    }
    //
    //    /**
    //     * Deserialize the values to empty {@code CharSequence/Array/Collection/Map}, instead of null.
    //     *
    //     * @param nullToEmpty
    //     * @return
    //     * @deprecated Use {@link #readNullToEmpty(boolean)} instead
    //     */
    //    @Deprecated
    //    public JSONDeserializationConfig nullToEmpty(boolean nullToEmpty) {
    //        return readNullToEmpty(nullToEmpty);
    //    }

    public boolean readNullToEmpty() {
        return readNullToEmpty;
    }

    /**
     * Deserialize the values to empty {@code CharSequence/Array/Collection/Map}, instead of {@code null}.
     *
     * @param readNullToEmpty
     * @return
     */
    public JSONDeserializationConfig readNullToEmpty(final boolean readNullToEmpty) {
        this.readNullToEmpty = readNullToEmpty;

        return this;
    }

    @SuppressWarnings("rawtypes")
    public Class<? extends Map> getMapInstanceType() {
        return mapInstanceType;
    }

    /**
     *
     * @param mapInstanceType
     * @return
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("rawtypes")
    public JSONDeserializationConfig setMapInstanceType(final Class<? extends Map> mapInstanceType) throws IllegalArgumentException {
        N.checkArgNotNull(mapInstanceType, cs.mapInstanceType);

        this.mapInstanceType = mapInstanceType;

        return this;
    }

    /**
     * Sets property handler/converter for (Big) collection values property.
     *
     * @param propName TODO should it be {@code parentEntity.propNameA(subEntity).propNameB...} For example: {@code account.devices.model}
     * @param handler the first parameter will be Collection or Map, the second parameter will be the current element or entry
     * @return
     * @throws IllegalArgumentException
     */
    public JSONDeserializationConfig setPropHandler(final String propName, final BiConsumer<? super Collection<?>, ?> handler) throws IllegalArgumentException {
        N.checkArgNotEmpty(propName, cs.propName);
        N.checkArgNotNull(handler, cs.handler);

        if (propHandlerMap == null) {
            propHandlerMap = new HashMap<>();
        }

        propHandlerMap.put(propName, handler);

        return this;
    }

    /**
     *
     * @param propName
     * @return
     */
    public BiConsumer<? super Collection<?>, ?> getPropHandler(final String propName) { //NOSONAR
        N.checkArgNotEmpty(propName, cs.propName);

        if (propHandlerMap == null) {
            return null;
        }

        return propHandlerMap.get(propName);
    }

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public JSONDeserializationConfig copy() {
    //        final JSONDeserializationConfig copy = new JSONDeserializationConfig();
    //
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.ignoreUnmatchedProperty = this.ignoreUnmatchedProperty;
    //        copy.ignoreNullOrEmpty = this.ignoreNullOrEmpty;
    //        copy.nullToEmpty = this.nullToEmpty;
    //        copy.elementType = this.elementType;
    //        copy.keyType = this.keyType;
    //        copy.valueType = this.valueType;
    //        copy.propTypes = this.propTypes;
    //
    //        return copy;
    //    }

    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(ignoreUnmatchedProperty);
        h = 31 * h + N.hashCode(ignoreNullOrEmpty);
        h = 31 * h + N.hashCode(readNullToEmpty);
        h = 31 * h + N.hashCode(elementType);
        h = 31 * h + N.hashCode(mapKeyType);
        h = 31 * h + N.hashCode(mapValueType);
        h = 31 * h + N.hashCode(valueTypeMap);
        h = 31 * h + N.hashCode(beanInfoForValueTypes);
        h = 31 * h + N.hashCode(mapInstanceType);
        return 31 * h + N.hashCode(propHandlerMap);
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

        if (obj instanceof JSONDeserializationConfig other) {
            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty, other.ignoreUnmatchedProperty) //NOSONAR
                    && N.equals(ignoreNullOrEmpty, other.ignoreNullOrEmpty) && N.equals(readNullToEmpty, other.readNullToEmpty)
                    && N.equals(elementType, other.elementType) && N.equals(mapKeyType, other.mapKeyType) && N.equals(mapValueType, other.mapValueType)
                    && N.equals(valueTypeMap, other.valueTypeMap) && N.equals(beanInfoForValueTypes, other.beanInfoForValueTypes)
                    && N.equals(mapInstanceType, other.mapInstanceType) && N.equals(propHandlerMap, other.propHandlerMap);
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty)
                + ", ignoreNullOrEmpty=" + N.toString(ignoreNullOrEmpty) + ", readNullToEmpty=" + N.toString(readNullToEmpty) + ", elementType="
                + N.toString(elementType) + ", mapKeyType=" + N.toString(mapKeyType) + ", mapValueType=" + N.toString(mapValueType) + ", valueTypeMap="
                + N.toString(valueTypeMap) + ", beanInfoForValueTypes=" + N.toString(beanInfoForValueTypes) + ", mapInstanceType=" + N.toString(mapInstanceType)
                + ", propHandlerMap=" + N.toString(propHandlerMap) + "}";
    }

    /**
     * The Class JDC.
     */
    public static final class JDC extends JSONDeserializationConfig {

        public static JSONDeserializationConfig create() {
            return new JSONDeserializationConfig();
        }

        /**
         *
         * @param elementClass
         * @return
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> elementClass) {
            return create().setElementType(elementClass);
        }

        /**
         *
         * @param keyClass
         * @param valueClass
         * @return
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> keyClass, final Class<?> valueClass) {
            return create().setMapKeyType(keyClass).setMapValueType(valueClass);
        }

        /**
         *
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final boolean ignoreUnmatchedProperty, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param elementClass
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> elementClass, final boolean ignoreUnmatchedProperty,
                final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass).ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param keyClass
         * @param valueClass
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> keyClass, final Class<?> valueClass, final boolean ignoreUnmatchedProperty,
                final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .ignoreUnmatchedProperty(ignoreUnmatchedProperty)
                    .setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param elementClass
         * @param keyClass
         * @param valueClass
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in a future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> elementClass, final Class<?> keyClass, final Class<?> valueClass,
                final boolean ignoreUnmatchedProperty, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass)
                    .setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .ignoreUnmatchedProperty(ignoreUnmatchedProperty)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
