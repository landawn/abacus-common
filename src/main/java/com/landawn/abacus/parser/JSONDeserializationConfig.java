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

/**
 * Configuration class for JSON deserialization operations.
 * 
 * <p>This class extends {@link DeserializationConfig} and provides JSON-specific configuration options
 * for controlling how JSON is parsed into Java objects. It allows fine-grained control over
 * deserialization behavior including handling of null/empty values, Map implementation types,
 * and custom property handlers.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Control over {@code null} and empty value handling during deserialization</li>
 *   <li>Specification of concrete Map implementation types</li>
 *   <li>Custom property handlers for collection processing</li>
 *   <li>Fluent API for easy configuration</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JSONDeserializationConfig config = new JSONDeserializationConfig()
 *     .ignoreUnmatchedProperty(true)
 *     .readNullToEmpty(true)
 *     .setMapInstanceType(LinkedHashMap.class);
 *
 * Person person = parser.deserialize(jsonString, config, Person.class);
 * }</pre>
 * 
 * @see DeserializationConfig
 * @see JSONSerializationConfig
 */
public class JSONDeserializationConfig extends DeserializationConfig<JSONDeserializationConfig> {

    /**
     * Constructs a new JSONDeserializationConfig with default settings.
     */
    public JSONDeserializationConfig() {
    }

    private boolean ignoreNullOrEmpty = false;

    private boolean readNullToEmpty = false;

    @SuppressWarnings("rawtypes")
    private Class<? extends Map> mapInstanceType = HashMap.class;

    private Map<String, BiConsumer<? super Collection<Object>, ?>> propHandlerMap = null;

    /**
     * Checks if {@code null} or empty values should be ignored during deserialization.
     * 
     * <p>When this setting is enabled, {@code null} or empty values in the JSON will not be
     * set on the target object. This applies to:</p>
     * <ul>
     *   <li>CharSequence fields (empty strings)</li>
     *   <li>Array fields (empty arrays)</li>
     *   <li>Collection fields (empty collections)</li>
     *   <li>Map fields (empty maps)</li>
     * </ul>
     *
     * @return {@code true} if {@code null} or empty values are ignored, {@code false} otherwise
     * @see #ignoreNullOrEmpty(boolean)
     */
    public boolean ignoreNullOrEmpty() {
        return ignoreNullOrEmpty;
    }

    /**
     * Sets whether to ignore {@code null} or empty values during deserialization.
     * 
     * <p>When enabled, {@code null} or empty CharSequence/Array/Collection/Map values won't be set/added/put
     * to the target bean/array/list/map. This is useful when you want to preserve existing
     * values in the target object rather than overwriting them with {@code null} or empty values
     * from the JSON.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.ignoreNullOrEmpty(true);
     * // JSON: {"name": "", "items": [], "data": null}
     * // These empty/null values will not be set on the target object
     * }</pre>
     *
     * @param ignoreNullOrEmpty {@code true} to ignore {@code null} or empty values, {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONDeserializationConfig ignoreNullOrEmpty(final boolean ignoreNullOrEmpty) {
        this.ignoreNullOrEmpty = ignoreNullOrEmpty;

        return this;
    }

    /**
     * Checks if {@code null} values should be read as empty values.
     * 
     * <p>This setting determines whether {@code null} values in JSON should be converted to
     * empty instances of the corresponding type during deserialization.</p>
     *
     * @return {@code true} if {@code null} values are read as empty, {@code false} otherwise
     * @see #readNullToEmpty(boolean)
     */
    public boolean readNullToEmpty() {
        return readNullToEmpty;
    }

    /**
     * Sets whether to deserialize {@code null} values to empty CharSequence/Array/Collection/Map.
     * 
     * <p>When enabled, {@code null} values in JSON will be converted to empty instances instead of {@code null}.
     * This is particularly useful when you want to avoid {@code null} checks in your code and prefer
     * working with empty collections or strings.</p>
     * 
     * <p>The conversion rules are:</p>
     * <ul>
     *   <li>null String → empty String ("")</li>
     *   <li>null Array → empty array</li>
     *   <li>null Collection → empty collection of the appropriate type</li>
     *   <li>null Map → empty map of the appropriate type</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.readNullToEmpty(true);
     * // JSON: {"name": null, "items": null}
     * // Result: name = "", items = []
     * }</pre>
     *
     * @param readNullToEmpty {@code true} to read {@code null} as empty, {@code false} otherwise
     * @return this instance for method chaining
     */
    public JSONDeserializationConfig readNullToEmpty(final boolean readNullToEmpty) {
        this.readNullToEmpty = readNullToEmpty;

        return this;
    }

    /**
     * Gets the Map implementation class to use when deserializing to Map instances.
     * 
     * <p>This returns the concrete Map class that will be instantiated when the deserializer
     * needs to create a Map instance. The default implementation is {@link HashMap}.</p>
     *
     * @return the Map implementation class
     * @see #setMapInstanceType(Class)
     */
    @SuppressWarnings("rawtypes")
    public Class<? extends Map> getMapInstanceType() {
        return mapInstanceType;
    }

    /**
     * Sets the Map implementation class to use when deserializing to Map instances.
     *
     * <p>This allows control over the concrete Map type created during deserialization.
     * The specified class must be a concrete implementation of Map with a no-argument
     * constructor. Common use cases include:</p>
     * <ul>
     *   <li>Using {@link java.util.LinkedHashMap} to preserve insertion order</li>
     *   <li>Using {@link java.util.TreeMap} for sorted keys</li>
     *   <li>Using {@link java.util.concurrent.ConcurrentHashMap} for thread-safe operations</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setMapInstanceType(LinkedHashMap.class);
     * // Maps will be created as LinkedHashMap to preserve insertion order
     *
     * config.setMapInstanceType(TreeMap.class);
     * // Maps will be created as TreeMap for sorted keys
     * }</pre>
     *
     * @param mapInstanceType the Map implementation class to use (must not be {@code null})
     * @return this instance for method chaining
     * @throws IllegalArgumentException if mapInstanceType is {@code null}
     */
    @SuppressWarnings("rawtypes")
    public JSONDeserializationConfig setMapInstanceType(final Class<? extends Map> mapInstanceType) throws IllegalArgumentException {
        N.checkArgNotNull(mapInstanceType, cs.mapInstanceType);

        this.mapInstanceType = mapInstanceType;

        return this;
    }

    /**
     * Sets a custom handler for processing collection property values during deserialization.
     *
     * <p>This method allows you to register a custom handler that will be invoked for each
     * element being added to a collection property during deserialization. This is particularly
     * useful for:</p>
     * <ul>
     *   <li>Processing large collections efficiently</li>
     *   <li>Applying custom transformation or validation logic</li>
     *   <li>Filtering elements before adding them to the collection</li>
     *   <li>Implementing custom collection population strategies</li>
     * </ul>
     *
     * <p>The handler receives two parameters:</p>
     * <ol>
     *   <li>The collection (or Map) being populated</li>
     *   <li>The current element (or Map.Entry) to be added</li>
     * </ol>
     *
     * <p>Property names support dot notation for nested properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setPropHandler("items", (collection, element) -> {
     *     // Custom processing for each element
     *     if (element != null && isValid(element)) {
     *         collection.add(processElement(element));
     *     }
     * });
     *
     * // For nested properties
     * config.setPropHandler("account.devices.model", (collection, model) -> {
     *     collection.add(model.toString().toUpperCase());
     * });
     * }</pre>
     *
     * @param propName the property name (supports nested properties like "account.devices.model")
     * @param handler the handler to process collection values (first parameter is Collection or Map, second is current element/entry)
     * @return this instance for method chaining
     * @throws IllegalArgumentException if propName is empty or handler is {@code null}
     */
    public JSONDeserializationConfig setPropHandler(final String propName, final BiConsumer<? super Collection<Object>, ?> handler)
            throws IllegalArgumentException {
        N.checkArgNotEmpty(propName, cs.propName);
        N.checkArgNotNull(handler, cs.handler);

        if (propHandlerMap == null) {
            propHandlerMap = new HashMap<>();
        }

        propHandlerMap.put(propName, handler);

        return this;
    }

    /**
     * Gets the property handler for the specified property name.
     *
     * <p>Returns the custom handler that was previously registered for the given property name
     * using {@link #setPropHandler(String, BiConsumer)}. If no handler has been registered
     * for the property, this method returns {@code null}.</p>
     *
     * @param propName the property name (must not be empty)
     * @return the handler for the property, or {@code null} if not set
     * @throws IllegalArgumentException if propName is empty
     * @see #setPropHandler(String, BiConsumer)
     */
    public BiConsumer<? super Collection<Object>, ?> getPropHandler(final String propName) { //NOSONAR
        N.checkArgNotEmpty(propName, cs.propName);

        if (propHandlerMap == null) {
            return null;
        }

        return propHandlerMap.get(propName);
    }

    /**
     * Calculates the hash code for this configuration object.
     * 
     * <p>The hash code is computed based on all configuration settings including:</p>
     * <ul>
     *   <li>Ignored property names</li>
     *   <li>Ignore unmatched property setting</li>
     *   <li>Ignore {@code null} or empty setting</li>
     *   <li>Read {@code null} to empty setting</li>
     *   <li>Element, map key, and map value types</li>
     *   <li>Map instance type</li>
     *   <li>Property handlers</li>
     * </ul>
     *
     * @return the hash code value for this object
     */
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
     * Compares this configuration with another object for equality.
     * 
     * <p>Two JSONDeserializationConfig objects are considered equal if all their settings match,
     * including:</p>
     * <ul>
     *   <li>All inherited settings from {@link DeserializationConfig}</li>
     *   <li>ignoreNullOrEmpty setting</li>
     *   <li>readNullToEmpty setting</li>
     *   <li>mapInstanceType</li>
     *   <li>All registered property handlers</li>
     * </ul>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
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

    /**
     * Returns a string representation of this configuration object.
     * 
     * <p>The string representation includes all configuration settings in a human-readable
     * format, showing the current values of all properties. This is useful for debugging
     * and logging purposes.</p>
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty)
                + ", ignoreNullOrEmpty=" + N.toString(ignoreNullOrEmpty) + ", readNullToEmpty=" + N.toString(readNullToEmpty) + ", elementType="
                + N.toString(elementType) + ", mapKeyType=" + N.toString(mapKeyType) + ", mapValueType=" + N.toString(mapValueType) + ", valueTypeMap="
                + N.toString(valueTypeMap) + ", beanInfoForValueTypes=" + N.toString(beanInfoForValueTypes) + ", mapInstanceType=" + N.toString(mapInstanceType)
                + ", propHandlerMap=" + N.toString(propHandlerMap) + "}";
    }

    /**
     * Factory class for creating JSONDeserializationConfig instances.
     * 
     * <p>This class provides convenient static factory methods for creating configurations
     * with common settings. It follows the builder pattern and supports method chaining
     * for easy configuration.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = JDC.create()
     *     .ignoreUnmatchedProperty(true)
     *     .setElementType(Person.class);
     * }</pre>
     * 
     * @see JSONDeserializationConfig
     */
    public static final class JDC extends JSONDeserializationConfig {

        /**
         * Constructs a new JDC instance.
         */
        public JDC() {
        }

        /**
         * Creates a new instance of JSONDeserializationConfig with default settings.
         *
         * <p>This is the recommended way to create a new configuration. The returned
         * configuration can be further customized using method chaining.</p>
         *
         * <p>Default settings:</p>
         * <ul>
         *   <li>ignoreNullOrEmpty: false</li>
         *   <li>readNullToEmpty: false</li>
         *   <li>mapInstanceType: HashMap.class</li>
         *   <li>ignoreUnmatchedProperty: true</li>
         * </ul>
         *
         * @return a new JSONDeserializationConfig instance
         */
        public static JSONDeserializationConfig create() {
            return new JSONDeserializationConfig();
        }

        /**
         * Creates a new JSONDeserializationConfig with the specified element type.
         * 
         * <p>This method is deprecated and will be removed in a future version.
         * Use {@link #create()} followed by {@link JSONDeserializationConfig#setElementType(Class)}
         * instead for better clarity and consistency.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * JSONDeserializationConfig config = JDC.create()
         *     .setElementType(Person.class);
         * }</pre>
         *
         * @param elementClass the class of collection/array elements
         * @return a new configured JSONDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> elementClass) {
            return create().setElementType(elementClass);
        }

        /**
         * Creates a new JSONDeserializationConfig with specified map key and value types.
         * 
         * <p>This method is deprecated and will be removed in a future version.
         * Use {@link #create()} with method chaining for better flexibility.</p>
         * 
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * JSONDeserializationConfig config = JDC.create()
         *     .setMapKeyType(String.class)
         *     .setMapValueType(Person.class);
         * }</pre>
         *
         * @param keyClass the class of map keys
         * @param valueClass the class of map values
         * @return a new configured JSONDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> keyClass, final Class<?> valueClass) {
            return create().setMapKeyType(keyClass).setMapValueType(valueClass);
        }

        /**
         * Creates a new JSONDeserializationConfig with property matching and ignored properties settings.
         * 
         * <p>This method is deprecated and will be removed in a future version.
         * Use {@link #create()} with method chaining for better clarity.</p>
         *
         * @param ignoreUnmatchedProperty whether to ignore properties that don't match the target class
         * @param ignoredPropNames map of class to set of property names to ignore
         * @return a new configured JSONDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final boolean ignoreUnmatchedProperty, final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new JSONDeserializationConfig with element type and property settings.
         * 
         * <p>This method is deprecated and will be removed in a future version.
         * Use {@link #create()} with method chaining for better maintainability.</p>
         *
         * @param elementClass the class of collection/array elements
         * @param ignoreUnmatchedProperty whether to ignore unmatched properties
         * @param ignoredPropNames map of class to set of property names to ignore
         * @return a new configured JSONDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
         */
        @Deprecated
        public static JSONDeserializationConfig of(final Class<?> elementClass, final boolean ignoreUnmatchedProperty,
                final Map<Class<?>, Set<String>> ignoredPropNames) {
            return create().setElementType(elementClass).ignoreUnmatchedProperty(ignoreUnmatchedProperty).setIgnoredPropNames(ignoredPropNames);
        }

        /**
         * Creates a new JSONDeserializationConfig with map types and property settings.
         * 
         * <p>This method is deprecated and will be removed in a future version.
         * Use {@link #create()} with method chaining for better readability.</p>
         *
         * @param keyClass the class of map keys
         * @param valueClass the class of map values
         * @param ignoreUnmatchedProperty whether to ignore unmatched properties
         * @param ignoredPropNames map of class to set of property names to ignore
         * @return a new configured JSONDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
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
         * Creates a new JSONDeserializationConfig with all type and property settings.
         * 
         * <p>This method is deprecated and will be removed in a future version.
         * Use {@link #create()} with method chaining which provides better flexibility
         * and allows setting only the needed properties.</p>
         *
         * @param elementClass the class of collection/array elements
         * @param keyClass the class of map keys
         * @param valueClass the class of map values
         * @param ignoreUnmatchedProperty whether to ignore unmatched properties
         * @param ignoredPropNames map of class to set of property names to ignore
         * @return a new configured JSONDeserializationConfig instance
         * @deprecated to be removed in a future version. Use {@link #create()} with method chaining instead.
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
