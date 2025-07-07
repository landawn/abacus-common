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

import java.util.Map;
import java.util.Set;

/**
 * Configuration class for XML deserialization settings.
 * 
 * <p>This class extends {@link DeserializationConfig} to provide XML-specific
 * deserialization options. It inherits all configuration options from its parent
 * class without adding XML-specific settings.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * XMLDeserializationConfig config = new XMLDeserializationConfig()
 *     .ignoreUnmatchedProperty(true)
 *     .setElementType(String.class)
 *     .setMapKeyType(String.class)
 *     .setMapValueType(Integer.class);
 * }</pre>
 * 
 * @see DeserializationConfig
 * @see XMLParser
 */
public class XMLDeserializationConfig extends DeserializationConfig<XMLDeserializationConfig> {

    /**
     * Creates a new XMLDeserializationConfig with default settings.
     * 
     * <p>Inherits all default settings from {@link DeserializationConfig}.</p>
     */
    public XMLDeserializationConfig() {
        super();
    }

    /**
     * Factory class for creating XMLDeserializationConfig instances.
     * 
     * <p>Provides static factory methods for convenient configuration creation.
     * This inner class is named XDC for brevity.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * XMLDeserializationConfig config = XDC.create()
     *     .ignoreUnmatchedProperty(true)
     *     .setElementType(Product.class);
     * }</pre>
     */
    public static final class XDC extends XMLDeserializationConfig {

        /**
         * Creates a new XMLDeserializationConfig instance with default settings.
         *
         * @return a new XMLDeserializationConfig instance
         */
        public static XMLDeserializationConfig create() {
            return new XMLDeserializationConfig();
        }

        /**
         * Creates a new XMLDeserializationConfig with the specified element type.
         * 
         * <p>This is useful when deserializing collections where the element type
         * cannot be inferred from the target class alone.</p>
         * 
         * @param elementClass the class type of collection elements
         * @return a new configured XMLDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} followed by {@link #setElementType(Class)} instead.
         */
        @Deprecated
        public static XMLDeserializationConfig of(final Class<?> elementClass) {
            return create().setElementType(elementClass);
        }

        /**
         * Creates a new XMLDeserializationConfig with specified map key and value types.
         * 
         * <p>This is useful when deserializing maps where the key and value types
         * cannot be inferred from the target class alone.</p>
         * 
         * @param keyClass the class type of map keys
         * @param valueClass the class type of map values
         * @return a new configured XMLDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLDeserializationConfig of(final Class<?> keyClass, final Class<?> valueClass) {
            return create().setMapKeyType(keyClass).setMapValueType(valueClass);
        }

        /**
         * Creates a new XMLDeserializationConfig with unmatched property handling and ignored properties.
         * 
         * @param ignoreUnmatchedProperty whether to ignore properties not found in the target class
         * @param ignoredPropNames map of property names to ignore by class
         * @return a new configured XMLDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLDeserializationConfig of(final boolean ignoreUnmatchedProperty, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new XMLDeserializationConfig with element type and property handling settings.
         * 
         * @param elementClass the class type of collection elements
         * @param ignoreUnmatchedProperty whether to ignore unmatched properties
         * @param ignoredPropNames map of property names to ignore by class
         * @return a new configured XMLDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLDeserializationConfig of(final Class<?> elementClass, final boolean ignoreUnmatchedProperty,
                final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass).ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new XMLDeserializationConfig with map types and property handling settings.
         * 
         * @param keyClass the class type of map keys
         * @param valueClass the class type of map values
         * @param ignoreUnmatchedProperty whether to ignore unmatched properties
         * @param ignoredPropNames map of property names to ignore by class
         * @return a new configured XMLDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLDeserializationConfig of(final Class<?> keyClass, final Class<?> valueClass, final boolean ignoreUnmatchedProperty,
                final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .ignoreUnmatchedProperty(ignoreUnmatchedProperty)
                    .setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new XMLDeserializationConfig with all type information and property handling settings.
         * 
         * @param elementClass the class type of collection elements
         * @param keyClass the class type of map keys
         * @param valueClass the class type of map values
         * @param ignoreUnmatchedProperty whether to ignore unmatched properties
         * @param ignoredPropNames map of property names to ignore by class
         * @return a new configured XMLDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static XMLDeserializationConfig of(final Class<?> elementClass, final Class<?> keyClass, final Class<?> valueClass,
                final boolean ignoreUnmatchedProperty, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass)
                    .setMapKeyType(keyClass)
                    .setMapValueType(valueClass)
                    .ignoreUnmatchedProperty(ignoreUnmatchedProperty)
                    .setIgnoredPropNames(ignoredPropNames);
        }
    }
}