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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.N;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class JSONDeserializationConfig extends DeserializationConfig<JSONDeserializationConfig> {

    boolean ignoreNullOrEmpty = false;

    boolean nullToEmpty = false;

    @SuppressWarnings("rawtypes")
    Class<? extends Map> mapInstanceType = HashMap.class;

    public boolean isIgnoreNullOrEmpty() {
        return ignoreNullOrEmpty;
    }

    /**
     * Won't set/add/put the value to entity/array/list/map if it's null or empty {@code CharSequence/Array/Collection/Map}.
     *
     * @param nullToEmpty
     * @return
     */
    public JSONDeserializationConfig setIgnoreNullOrEmpty(boolean ignoreNullOrEmpty) {
        this.ignoreNullOrEmpty = ignoreNullOrEmpty;

        return this;
    }

    public boolean isNullToEmpty() {
        return nullToEmpty;
    }

    /**
     * Deserialize the values to empty {@code CharSequence/Array/Collection/Map}, instead of null.
     *
     * @param nullToEmpty
     * @return
     */
    public JSONDeserializationConfig setNullToEmpty(boolean nullToEmpty) {
        this.nullToEmpty = nullToEmpty;

        return this;
    }

    @SuppressWarnings("rawtypes")
    public Class<? extends Map> getMapInstanceType() {
        return mapInstanceType;
    }

    @SuppressWarnings("rawtypes")
    public JSONDeserializationConfig setMapInstanceType(Class<? extends Map> mapInstanceType) {
        N.checkArgNotNull(mapInstanceType, "mapInstanceType");

        this.mapInstanceType = mapInstanceType;

        return this;
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
        h = 31 * h + N.hashCode(nullToEmpty);
        h = 31 * h + N.hashCode(elementType);
        h = 31 * h + N.hashCode(keyType);
        h = 31 * h + N.hashCode(valueType);
        h = 31 * h + N.hashCode(propTypes);
        return 31 * h + N.hashCode(mapInstanceType);
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
            if (N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty, other.ignoreUnmatchedProperty)
                    && N.equals(ignoreNullOrEmpty, other.ignoreNullOrEmpty) && N.equals(nullToEmpty, other.nullToEmpty)
                    && N.equals(elementType, other.elementType) && N.equals(keyType, other.keyType) && N.equals(valueType, other.valueType)
                    && N.equals(propTypes, other.propTypes) && N.equals(mapInstanceType, other.mapInstanceType)) {

                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty)
                + ", ignoreNullOrEmpty=" + N.toString(ignoreNullOrEmpty) + ", nullToEmpty=" + N.toString(nullToEmpty) + ", elementType="
                + N.toString(elementType) + ", keyType=" + N.toString(keyType) + ", valueType=" + N.toString(valueType) + ", propTypes=" + N.toString(propTypes)
                + ", mapInstanceType=" + N.toString(mapInstanceType) + "}";
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
         * @deprecated
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
         * @deprecated
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
         * @deprecated
         */
        @Deprecated
        public static JSONDeserializationConfig of(boolean ignoreUnmatchedProperty, Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setIgnoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param elementClass
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> elementClass, boolean ignoreUnmatchedProperty, Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setElementType(elementClass).setIgnoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         *
         * @param keyClass
         * @param valueClass
         * @param ignoreUnmatchedProperty
         * @param ignoredPropNames
         * @return
         * @deprecated
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> keyClass, Class<?> valueClass, boolean ignoreUnmatchedProperty,
                Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .setIgnoreUnmatchedProperty(ignoreUnmatchedProperty)
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
         * @deprecated
         */
        @Deprecated
        public static JSONDeserializationConfig of(Class<?> elementClass, Class<?> keyClass, Class<?> valueClass, boolean ignoreUnmatchedProperty,
                Map<Class<?>, Collection<String>> ignoredPropNames) {
            return create().setElementType(elementClass)
                    .setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .setIgnoreUnmatchedProperty(ignoreUnmatchedProperty)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
