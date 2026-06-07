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
 * JsonDeserConfig config = new JsonDeserConfig()
 *     .setIgnoreUnmatchedProperty(true)
 *     .setReadNullToEmpty(true)
 *     .setMapInstanceType(LinkedHashMap.class);
 *
 * Person person = parser.deserialize(jsonString, config, Person.class);
 * }</pre>
 *
 * @see DeserializationConfig
 * @see JsonSerConfig
 */
public class JsonDeserConfig extends DeserializationConfig<JsonDeserConfig> {

    /**
     * Constructs a new JsonDeserConfig with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setReadNullToEmpty(true);
     * }</pre>
     *
     */
    public JsonDeserConfig() {
    }

    private boolean ignoreNullOrEmpty = false;

    private boolean readNullToEmpty = false;

    @SuppressWarnings("rawtypes")
    private Class<? extends Map> mapInstanceType = HashMap.class;

    private Map<String, BiConsumer<? super Collection<Object>, ?>> propHandlerMap = null;

    /**
     * Checks whether {@code null} or empty values in the JSON source should be silently ignored
     * (i.e., not assigned to the target bean property) during deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * boolean ignored = config.isIgnoreNullOrEmpty(); // returns false (default)
     *
     * config.setIgnoreNullOrEmpty(true);
     * boolean enabled = config.isIgnoreNullOrEmpty(); // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} or empty source values are ignored and the target
     *         property is left unchanged, {@code false} (default) if they are assigned normally
     */
    public boolean isIgnoreNullOrEmpty() {
        return ignoreNullOrEmpty;
    }

    /**
     * Sets whether {@code null} or empty values encountered in the JSON source should be ignored
     * during deserialization. When {@code true}, a JSON {@code null} or empty string will not
     * overwrite the corresponding property on the target bean, leaving it at its existing value.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * config.setIgnoreNullOrEmpty(true);              // returns this (config)
     * boolean enabled = config.isIgnoreNullOrEmpty(); // returns true
     *
     * config.setIgnoreNullOrEmpty(false);              // returns this (config)
     * boolean disabled = config.isIgnoreNullOrEmpty(); // returns false (default)
     * }</pre>
     *
     * @param ignoreNullOrEmpty {@code true} to skip assigning {@code null} or empty JSON values
     *        to target properties; {@code false} (default) to assign them normally
     * @return {@code this} instance for method chaining
     */
    public JsonDeserConfig setIgnoreNullOrEmpty(final boolean ignoreNullOrEmpty) {
        this.ignoreNullOrEmpty = ignoreNullOrEmpty;

        return this;
    }

    /**
     * Checks whether {@code null} values should be deserialized to empty values
     * (empty CharSequence/Array/Collection/Map) instead of {@code null}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * boolean asNull = config.isReadNullToEmpty(); // returns false (default)
     *
     * config.setReadNullToEmpty(true);
     * boolean asEmpty = config.isReadNullToEmpty(); // returns true
     * }</pre>
     *
     * @return {@code true} if {@code null} values are read as empty, {@code false} (default) otherwise
     */
    public boolean isReadNullToEmpty() {
        return readNullToEmpty;
    }

    /**
     * Sets whether to deserialize {@code null} values to empty CharSequence/Array/Collection/Map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * config.setReadNullToEmpty(true);              // returns this (config)
     * boolean asEmpty = config.isReadNullToEmpty(); // returns true
     *
     * config.setReadNullToEmpty(false);            // returns this (config)
     * boolean asNull = config.isReadNullToEmpty(); // returns false (default)
     * }</pre>
     *
     * @param readNullToEmpty {@code true} to read {@code null} as empty, {@code false} otherwise
     * @return {@code this} instance for method chaining
     */
    public JsonDeserConfig setReadNullToEmpty(final boolean readNullToEmpty) {
        this.readNullToEmpty = readNullToEmpty;

        return this;
    }

    /**
     * Gets the Map implementation class to use when deserializing to Map instances.
     *
     * <p>This returns the concrete Map class that will be instantiated when the deserializer
     * needs to create a Map instance. The default implementation is {@link HashMap}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * Class<?> type = config.getMapInstanceType(); // returns HashMap.class (default)
     *
     * config.setMapInstanceType(java.util.LinkedHashMap.class);
     * Class<?> updated = config.getMapInstanceType(); // returns LinkedHashMap.class
     * }</pre>
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
     * @return {@code this} instance for method chaining
     * @throws IllegalArgumentException if {@code mapInstanceType} is {@code null}
     */
    @SuppressWarnings("rawtypes")
    public JsonDeserConfig setMapInstanceType(final Class<? extends Map> mapInstanceType) throws IllegalArgumentException {
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
     * @return {@code this} instance for method chaining
     * @throws IllegalArgumentException if {@code propName} is empty or {@code handler} is {@code null}
     */
    public JsonDeserConfig setPropHandler(final String propName, final BiConsumer<? super Collection<Object>, ?> handler) throws IllegalArgumentException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * Object handler = config.getPropHandler("items"); // returns null (none registered)
     *
     * config.setPropHandler("items", (collection, element) -> collection.add(element));
     * Object registered = config.getPropHandler("items"); // returns the registered handler (non-null)
     * }</pre>
     *
     * @param propName the property name (must not be empty)
     * @return the handler for the property, or {@code null} if not set
     * @throws IllegalArgumentException if {@code propName} is empty
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
     * <p>Two JsonDeserConfig objects are considered equal if all their settings match,
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

        if (obj instanceof JsonDeserConfig other) {
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
     * Creates a new instance of JsonDeserConfig with default settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = JsonDeserConfig.create()
     *     .setMapInstanceType(java.util.LinkedHashMap.class)
     *     .setIgnoreNullOrEmpty(true);
     * }</pre>
     *
     * @return a new JsonDeserConfig instance
     */
    public static JsonDeserConfig create() {
        return new JsonDeserConfig();
    }

}
