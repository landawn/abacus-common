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

import java.util.Map;
import java.util.Set;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
public class XMLDeserializationConfig extends DeserializationConfig<XMLDeserializationConfig> {

    //    /**
    //     *
    //     * @return
    //     */
    //    @Override
    //    public XMLDeserializationConfig copy() {
    //        final XMLDeserializationConfig copy = new XMLDeserializationConfig();
    //
    //        copy.setIgnoredPropNames(this.getIgnoredPropNames());
    //        copy.ignoreUnmatchedProperty = this.ignoreUnmatchedProperty;
    //        copy.elementType = this.elementType;
    //        copy.keyType = this.keyType;
    //        copy.valueType = this.valueType;
    //        copy.propTypes = this.propTypes;
    //
    //        return copy;
    //    }

    /**
     * The Class XDC.
     */
    public static final class XDC extends XMLDeserializationConfig {

        /**
         *
         * @return
         */
        public static XMLDeserializationConfig create() {
            return new XMLDeserializationConfig();
        }

        /**
         *
         * @param elementClass
         * @return
         * @deprecated to be removed in future version.
         */
        @Deprecated
        public static XMLDeserializationConfig of(Class<?> elementClass) {
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
        public static XMLDeserializationConfig of(Class<?> keyClass, Class<?> valueClass) {
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
        public static XMLDeserializationConfig of(boolean ignoreUnmatchedProperty, Map<Class<?>, Set<String>> ignoredPropNames) {
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
        public static XMLDeserializationConfig of(Class<?> elementClass, boolean ignoreUnmatchedProperty, Map<Class<?>, Set<String>> ignoredPropNames) {
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
        public static XMLDeserializationConfig of(Class<?> keyClass, Class<?> valueClass, boolean ignoreUnmatchedProperty,
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
        public static XMLDeserializationConfig of(Class<?> elementClass, Class<?> keyClass, Class<?> valueClass, boolean ignoreUnmatchedProperty,
                Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass)
                    .setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .ignoreUnmatchedProperty(ignoreUnmatchedProperty)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}
