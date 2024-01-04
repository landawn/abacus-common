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

package com.landawn.abacus.parser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class JSONDeserializationConfig extends DeserializationConfig<JSONDeserializationConfig> {

    boolean ignoreNullOrEmpty = false;

    boolean readNullToEmpty = false;

    @SuppressWarnings("rawtypes")
    Class<? extends Map> mapInstanceType = HashMap.class;

    Map<String, BiConsumer<? super Collection<?>, ?>> propHandlerMap = null;

    /**
     *
     *
     * @return
     */
    public boolean ignoreNullOrEmpty() {
        return ignoreNullOrEmpty;
    }

    /**
     * Won't set/add/put the value to bean/array/list/map if it's null or empty {@code CharSequence/Array/Collection/Map}.
     *
     * @param ignoreNullOrEmpty
     * @return
     */
    public JSONDeserializationConfig ignoreNullOrEmpty(boolean ignoreNullOrEmpty) {
        this.ignoreNullOrEmpty = ignoreNullOrEmpty;

        return this;
    }

    /**
     *
     *
     * @return
     * @deprecated Use {@link #readNullToEmpty()} instead
     */
    @Deprecated
    public boolean nullToEmpty() {
        return readNullToEmpty();
    }

    /**
     * Deserialize the values to empty {@code CharSequence/Array/Collection/Map}, instead of null.
     *
     * @param nullToEmpty
     * @return
     * @deprecated Use {@link #readNullToEmpty(boolean)} instead
     */
    @Deprecated
    public JSONDeserializationConfig nullToEmpty(boolean nullToEmpty) {
        return readNullToEmpty(nullToEmpty);
    }

    /**
     *
     *
     * @return
     */
    public boolean readNullToEmpty() {
        return readNullToEmpty;
    }

    /**
     * Deserialize the values to empty {@code CharSequence/Array/Collection/Map}, instead of null.
     *
     * @param readNullToEmpty
     * @return
     */
    public JSONDeserializationConfig readNullToEmpty(boolean readNullToEmpty) {
        this.readNullToEmpty = readNullToEmpty;

        return this;
    }

    /**
     *
     *
     * @return
     */
    @SuppressWarnings("rawtypes")
    public Class<? extends Map> getMapInstanceType() {
        return mapInstanceType;
    }

    /**
     *
     *
     * @param mapInstanceType
     * @return
     */
    @SuppressWarnings("rawtypes")
    public JSONDeserializationConfig setMapInstanceType(Class<? extends Map> mapInstanceType) {
        N.checkArgNotNull(mapInstanceType, "mapInstanceType");

        this.mapInstanceType = mapInstanceType;

        return this;
    }

    /**
     * Sets property handler/converter for (Big) collection values property.
     *
     * @param propName
     * @param handler the first parameter will be Collection or Map, the second parameter will be the current element or entry
     * @return
     */
    public JSONDeserializationConfig setPropHandler(final String propName, final BiConsumer<? super Collection<?>, ?> handler) {
        N.checkArgNotEmpty(propName, "propName");
        N.checkArgNotNull(handler, "handler");

        if (propHandlerMap == null) {
            propHandlerMap = new HashMap<>();
        }

        propHandlerMap.put(propName, handler);

        return this;
    }

    public BiConsumer<? super Collection<?>, ?> getPropHandler(final String propName) {
        N.checkArgNotEmpty(propName, "propName");

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

    /**
     *
     *
     * @return
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(ignoreUnmatchedProperty);
        h = 31 * h + N.hashCode(ignoreNullOrEmpty);
        h = 31 * h + N.hashCode(readNullToEmpty);
        h = 31 * h + N.hashCode(elementType);
        h = 31 * h + N.hashCode(keyType);
        h = 31 * h + N.hashCode(valueType);
        h = 31 * h + N.hashCode(propTypes);
        h = 31 * h + N.hashCode(mapInstanceType);
        return 31 * h + N.hashCode(propHandlerMap);
    }

    /**
     *
     * @param obj
     * @return true, if successful
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof JSONDeserializationConfig other) {
            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty, other.ignoreUnmatchedProperty) //NOSONAR
                    && N.equals(ignoreNullOrEmpty, other.ignoreNullOrEmpty) && N.equals(readNullToEmpty, other.readNullToEmpty)
                    && N.equals(elementType, other.elementType) && N.equals(keyType, other.keyType) && N.equals(valueType, other.valueType)
                    && N.equals(propTypes, other.propTypes) && N.equals(mapInstanceType, other.mapInstanceType)
                    && N.equals(propHandlerMap, other.propHandlerMap)) {

                return true;
            }
        }

        return false;
    }

    /**
     *
     *
     * @return
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty)
                + ", ignoreNullOrEmpty=" + N.toString(ignoreNullOrEmpty) + ", readNullToEmpty=" + N.toString(readNullToEmpty) + ", elementType="
                + N.toString(elementType) + ", keyType=" + N.toString(keyType) + ", valueType=" + N.toString(valueType) + ", propTypes=" + N.toString(propTypes)
                + ", mapInstanceType=" + N.toString(mapInstanceType) + ", propHandlerMap=" + N.toString(propHandlerMap) + "}";
    }

    /**
     * The Class JDC.
     */
    public static final class JDC extends JSONDeserializationConfig {

        /**
         *
         * @return
         */
        public static JSONDeserializationConfig create() {
            return new JSONDeserializationConfig();
        }

        /**
         *
         * @param elementClass
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> elementClass) {
            return create().setElementType(elementClass);
        }

        /**
         *
         * @param keyClass
         * @param valueClass
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> keyClass, Class<?> valueClass) {
            return create().setMapKeyType(keyClass).setMapValueType(valueClass);
        }

        /**
         *
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(boolean ignoreUnmatchedProperty, Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param elementClass
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> elementClass, boolean ignoreUnmatchedProperty, Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass).ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param keyClass
         * @param valueClass
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> keyClass, Class<?> valueClass, boolean ignoreUnmatchedProperty,
                Map<Class<?>, Set<String>> ignoredPropNames) {
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
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> elementClass, Class<?> keyClass, Class<?> valueClass, boolean ignoreUnmatchedProperty,
                Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass)
                    .setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .ignoreUnmatchedProperty(ignoreUnmatchedProperty)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
