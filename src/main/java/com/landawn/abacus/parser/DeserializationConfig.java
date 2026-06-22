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

import java.util.HashMap;
import java.util.Map;

import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

/**
 * Abstract base configuration class for deserialization operations.
 * This class provides common configuration options for controlling how data is deserialized
 * into Java objects across different formats (JSON, XML, etc.).
 *
 * <p>The configuration supports method chaining for easy setup:</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * DeserializationConfig config = new MyDeserConfig()
 *     .setIgnoreUnmatchedProperty(true)
 *     .setElementType(Person.class)
 *     .setMapKeyType(String.class)
 *     .setMapValueType(Integer.class);
 * }</pre>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Control whether unmatched properties should be ignored or cause errors</li>
 *   <li>Specify type information for collections, arrays, and maps</li>
 *   <li>Configure value types for specific properties by name</li>
 *   <li>Set type information using bean classes for complex deserialization</li>
 * </ul>
 *
 * <p><b>Property-Name Matching:</b></p>
 * <p>Type lookups during deserialization use the immediate property or map-key name at each
 * nesting level. Register the local name (e.g. {@code "city"}) to affect any property or key
 * with that name, at any depth. For example:</p>
 * <pre>{@code
 * config.setValueType("city", String.class);       // applies to a property/key named "city" at any level
 * config.setValueType("model", String.class);      // applies to a property/key named "model" at any level
 * config.setValueType("items", List.class);        // applies to a property/key named "items"
 * config.setValueType("settings", Map.class);      // applies to a property/key named "settings"
 * }</pre>
 *
 * <p>Dotted keys such as {@code "address.city"} are matched only as literal key strings by
 * {@link #getValueType(String)}; they are not resolved as paths and are not consulted by the
 * parsers during deserialization.</p>
 *
 * @param <C> the concrete configuration type for method chaining
 * @see JsonDeserConfig
 * @see XmlDeserConfig
 */
public abstract class DeserializationConfig<C extends DeserializationConfig<C>> extends ParserConfig<C> {

    /**
     * Constructs a new {@code DeserializationConfig}.
     */
    protected DeserializationConfig() {
    }

    /** Whether to ignore properties in the source data that don't have corresponding fields in the target class. */
    boolean ignoreUnmatchedProperty = true;

    /** The element type for collection and array deserialization. */
    Type<?> elementType;

    /** The key type for map deserialization. */
    Type<?> mapKeyType;

    /** The value type for map deserialization. */
    Type<?> mapValueType;

    /** Map of property names to their respective types for fine-grained deserialization control. */
    Map<String, Type<?>> valueTypeMap;

    /** Bean information used to resolve value types based on property names. */
    BeanInfo beanInfoForValueTypes;

    /**
     * Checks if unmatched properties should be ignored during deserialization.
     * When set to {@code true} (default), properties in the source data that don't match
     * any property in the target class will be silently ignored. When {@code false},
     * unmatched properties will cause an exception to be thrown.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * boolean ignored = config.isIgnoreUnmatchedProperty();  // returns true (default)
     *
     * config.setIgnoreUnmatchedProperty(false);
     * boolean strict = config.isIgnoreUnmatchedProperty();   // returns false
     * }</pre>
     *
     * @return {@code true} if unmatched properties are ignored, {@code false} otherwise
     */
    public boolean isIgnoreUnmatchedProperty() {
        return ignoreUnmatchedProperty;
    }

    /**
     * Sets whether unmatched properties should be ignored during deserialization.
     * This is useful when deserializing data that may contain extra fields not
     * present in the target class.
     *
     * <p>When set to {@code false}, the parser will throw an exception (typically
     * {@code IllegalArgumentException} or a parser-specific exception) when encountering
     * properties in the source data that don't have corresponding fields in the target class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // JSON: {"name": "John", "age": 30, "unknownField": "value"}
     * // Target class only has name and age properties
     * config.setIgnoreUnmatchedProperty(true);    // unknownField is ignored
     * config.setIgnoreUnmatchedProperty(false);   // unknownField is rejected, causing an exception
     * }</pre>
     *
     * @param ignoreUnmatchedProperty {@code true} to ignore unmatched properties, {@code false} to throw an exception
     * @return this configuration instance for method chaining
     */
    public C setIgnoreUnmatchedProperty(final boolean ignoreUnmatchedProperty) {
        this.ignoreUnmatchedProperty = ignoreUnmatchedProperty;

        return (C) this;
    }

    /**
     * Gets the element type for collection and array deserialization.
     * This type is used when deserializing JSON arrays or XML sequences
     * to determine the type of elements in the collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * Type<Object> none = config.getElementType();  // returns null (not set)
     *
     * config.setElementType(String.class);
     * Type<String> type = config.getElementType();  // returns the String type
     * String typeName = type.name();                // returns "String"
     * }</pre>
     *
     * @param <T> the element type
     * @return the configured element type, or {@code null} if not set
     */
    public <T> Type<T> getElementType() {
        return (Type<T>) elementType;
    }

    /**
     * Sets the element type for collection and array deserialization using a Class.
     * This is used when deserializing to collections or arrays to specify
     * the type of elements they contain.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For deserializing JSON array to List<Person>
     * config.setElementType(Person.class);
     * List<Person> people = parser.deserialize(jsonArray, config, List.class);
     * }</pre>
     *
     * @param elementClass the class of collection/array elements
     * @return this configuration instance for method chaining
     */
    public C setElementType(final Class<?> elementClass) {
        return setElementType(Type.of(elementClass));
    }

    /**
     * Sets the element type for collection and array deserialization using a Type.
     * This allows for more complex type specifications including generic types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For complex generic types
     * Type<List<String>> listType = Type.of("List<String>");
     * config.setElementType(listType);
     * }</pre>
     *
     * @param type the type of collection/array elements
     * @return this configuration instance for method chaining
     */
    public C setElementType(final Type<?> type) {
        elementType = type;

        return (C) this;
    }

    /**
     * Sets the element type for collection and array deserialization using a type name string.
     * The type name can be a simple class name or a complex generic type expression.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setElementType("Person");
     * config.setElementType("List<String>");
     * config.setElementType("Map<String, Integer>");
     * }</pre>
     *
     * @param type the type name string
     * @return this configuration instance for method chaining
     */
    public C setElementType(final String type) {
        return setElementType(Type.of(type));
    }

    /**
     * Gets the key type for map deserialization.
     * This type is used when deserializing to Map instances to determine
     * the type of keys in the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * Type<Object> none = config.getMapKeyType();  // returns null (not set)
     *
     * config.setMapKeyType(String.class);
     * Type<String> keyType = config.getMapKeyType();  // returns the String key type
     * String typeName = keyType.name();               // returns "String"
     * }</pre>
     *
     * @param <T> the key type
     * @return the configured map key type, or {@code null} if not set
     */
    public <T> Type<T> getMapKeyType() {
        return (Type<T>) mapKeyType;
    }

    /**
     * Sets the key type for map deserialization using a Class.
     * This is used when deserializing to Map instances to specify
     * the type of keys they contain.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For deserializing to Map<Long, String>
     * config.setMapKeyType(Long.class);
     * config.setMapValueType(String.class);
     * }</pre>
     *
     * @param cls the class of map keys
     * @return this configuration instance for method chaining
     */
    public C setMapKeyType(final Class<?> cls) {
        return setMapKeyType(Type.of(cls));
    }

    /**
     * Sets the key type for map deserialization using a Type.
     * This allows for more complex key type specifications.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * Type<Long> longType = Type.of(Long.class);
     * config.setMapKeyType(longType);                  // returns this (config) for chaining
     * String name = config.getMapKeyType().name();     // returns "Long"
     * }</pre>
     *
     * @param keyType the type of map keys
     * @return this configuration instance for method chaining
     */
    public C setMapKeyType(final Type<?> keyType) {
        mapKeyType = keyType;

        return (C) this;
    }

    /**
     * Sets the key type for map deserialization using a type name string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setMapKeyType("String");
     * config.setMapKeyType("Integer");
     * }</pre>
     *
     * @param keyType the key type name string
     * @return this configuration instance for method chaining
     */
    public C setMapKeyType(final String keyType) {
        return setMapKeyType(Type.of(keyType));
    }

    /**
     * Gets the value type for map deserialization.
     * This type is used when deserializing to Map instances to determine
     * the type of values in the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * Type<Object> none = config.getMapValueType();  // returns null (not set)
     *
     * config.setMapValueType(Integer.class);
     * Type<Integer> valueType = config.getMapValueType();  // returns the Integer value type
     * String typeName = valueType.name();                  // returns "Integer"
     * }</pre>
     *
     * @param <T> the value type
     * @return the configured map value type, or {@code null} if not set
     */
    public <T> Type<T> getMapValueType() {
        return (Type<T>) mapValueType;
    }

    /**
     * Sets the value type for map deserialization using a Class.
     * This is used when deserializing to Map instances to specify
     * the type of values they contain.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For deserializing to Map<String, Person>
     * config.setMapKeyType(String.class);
     * config.setMapValueType(Person.class);
     * Map<String, Person> people = parser.deserialize(json, config, Map.class);
     * }</pre>
     *
     * @param cls the class of map values
     * @return this configuration instance for method chaining
     */
    public C setMapValueType(final Class<?> cls) {
        return setMapValueType(Type.of(cls));
    }

    /**
     * Sets the value type for map deserialization using a Type.
     * This allows for more complex value type specifications including generics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * Type<Integer> intType = Type.of(Integer.class);
     * config.setMapValueType(intType);                   // returns this (config) for chaining
     * String name = config.getMapValueType().name();     // returns "Integer"
     * }</pre>
     *
     * @param valueType the type of map values
     * @return this configuration instance for method chaining
     */
    public C setMapValueType(final Type<?> valueType) {
        mapValueType = valueType;

        return (C) this;
    }

    /**
     * Sets the value type for map deserialization using a type name string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setMapValueType("Person");
     * config.setMapValueType("List<String>");
     * }</pre>
     *
     * @param valueType the value type name string
     * @return this configuration instance for method chaining
     */
    public C setMapValueType(final String valueType) {
        return setMapValueType(Type.of(valueType));
    }

    /**
     * Checks if any value types have been configured.
     * Returns {@code true} if either individual value types have been set for specific properties
     * or a bean class has been set for value type information.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig();
     * boolean empty = config.hasValueTypes();  // returns false (none configured)
     *
     * config.setValueType("address", String.class);
     * boolean configured = config.hasValueTypes();  // returns true
     * }</pre>
     *
     * @return {@code true} if value types are configured, {@code false} otherwise
     */
    public boolean hasValueTypes() {
        return beanInfoForValueTypes != null || N.notEmpty(valueTypeMap);
    }

    /**
     * Gets the value type for a specific property by its key name.
     * This is used during deserialization to determine the correct type
     * for nested properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Address> addressType = config.getValueType("address");
     * Type<String> cityType = config.getValueType("city"); // matched as a literal key, not a nested path
     * }</pre>
     *
     * @param <T> the value type
     * @param keyName the property/key name as it appears at its nesting level during deserialization (dotted paths are matched only as literal key strings) - see class documentation
     * @return the type for the specified property, or {@code null} if not configured
     */
    public <T> Type<T> getValueType(final String keyName) {
        return getValueType(keyName, null);
    }

    /**
     * Gets the value type for a specific property by its key name with a default type.
     * If no type is configured for the specified property, returns the default type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<String> stringType = Type.of(String.class);
     * Type<?> type = config.getValueType("unknownProp", stringType);
     * // Returns stringType if "unknownProp" is not configured
     * }</pre>
     *
     * @param <T> the value type
     * @param keyName the property/key name as it appears at its nesting level during deserialization (dotted paths are matched only as literal key strings) - see class documentation
     * @param defaultType the type to return if no type is configured for the property
     * @return the type for the specified property, or {@code defaultType} if not configured
     */
    public <T> Type<T> getValueType(final String keyName, final Type<T> defaultType) {
        Type<T> ret = null;

        if (valueTypeMap != null) {
            ret = (Type<T>) valueTypeMap.get(keyName);
        }

        if (ret == null && beanInfoForValueTypes != null) {
            final PropInfo propInfo = beanInfoForValueTypes.getPropInfo(keyName);

            if (propInfo != null) {
                ret = (Type<T>) propInfo.type;
            }
        }

        return ret == null ? defaultType : ret;
    }

    /**
     * Sets the value type for a specific property using a Class.
     * This allows fine-grained control over deserialization of nested properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setValueType("address", Address.class);
     * config.setValueType("phoneNumbers", List.class);
     * config.setValueType("tags", Set.class); // matched as a literal key, not a nested path
     * }</pre>
     *
     * @param keyName the property/key name as it appears at its nesting level during deserialization (dotted paths are matched only as literal key strings) - see class documentation
     * @param typeClass the class to use for deserializing this property
     * @return this configuration instance for method chaining
     */
    public C setValueType(final String keyName, final Class<?> typeClass) {
        return setValueType(keyName, Type.of(typeClass));
    }

    /**
     * Sets the value type for a specific property using a Type.
     * This allows for complex type specifications including generics.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<List<String>> listType = Type.of("List<String>");
     * config.setValueType("tags", listType);
     * }</pre>
     *
     * @param keyName the property/key name as it appears at its nesting level during deserialization (dotted paths are matched only as literal key strings) - see class documentation
     * @param type the type to use for deserializing this property
     * @return this configuration instance for method chaining
     */
    public C setValueType(final String keyName, final Type<?> type) {
        if (valueTypeMap == null) {
            valueTypeMap = new HashMap<>();
        }

        valueTypeMap.put(keyName, type);

        return (C) this;
    }

    /**
     * Sets the value type for a specific property using a type name string.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setValueType("items", "List<Product>");
     * config.setValueType("metadata", "Map<String, Object>");
     * }</pre>
     *
     * @param keyName the property/key name as it appears at its nesting level during deserialization (dotted paths are matched only as literal key strings) - see class documentation
     * @param typeName the type name string
     * @return this configuration instance for method chaining
     */
    public C setValueType(final String keyName, final String typeName) {
        return setValueType(keyName, Type.of(typeName));
    }

    /**
     * Sets multiple value types at once using a map.
     * This is useful when configuring types for many properties at once. The given map
     * replaces any previously configured value types (it is stored by reference, not copied).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Type<?>> types = new HashMap<>();
     * types.put("address", Type.of(Address.class));
     * types.put("items", Type.of("List<Item>"));
     * config.setValueTypes(types);
     * }</pre>
     *
     * @param valueTypes map of property names to their types
     * @return this configuration instance for method chaining
     */
    public C setValueTypes(final Map<String, Type<?>> valueTypes) {
        valueTypeMap = valueTypes;

        return (C) this;
    }

    /**
     * Sets value types by analyzing a bean type.
     * This method extracts property type information from all properties of the specified
     * bean type and uses it for deserialization type resolution.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // If Person class has properties: name (String), age (int), address (Address)
     * config.setValueTypesByBeanClass(Person.class);
     * // Now "name", "age", and "address" properties will use their declared types
     * }</pre>
     *
     * @param beanType the bean type to extract property type information from; may be {@code null} to clear the setting
     * @return this configuration instance for method chaining
     * @throws IllegalArgumentException if the specified type is not a valid bean type
     */
    public C setValueTypesByBeanClass(final java.lang.reflect.Type beanType) throws IllegalArgumentException {
        if (beanType == null) {
            beanInfoForValueTypes = null;
        } else {
            beanInfoForValueTypes = ParserUtil.getBeanInfo(beanType);
        }

        return (C) this;
    }

    /**
     * Calculates the hash code for this configuration object.
     * The hash code is based on all configuration settings.
     *
     * @return the hash code value for this object
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = 31 * h + N.hashCode(getIgnoredPropNames());
        h = 31 * h + N.hashCode(ignoreUnmatchedProperty);
        h = 31 * h + N.hashCode(elementType);
        h = 31 * h + N.hashCode(mapKeyType);
        h = 31 * h + N.hashCode(mapValueType);
        h = 31 * h + N.hashCode(valueTypeMap);
        return 31 * h + N.hashCode(beanInfoForValueTypes);
    }

    /**
     * Compares this configuration with another object for equality.
     * Two configurations are considered equal if all their settings match.
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        // Require exact same class to keep equals symmetric. JsonDeserConfig.equals uses
        // `instanceof JsonDeserConfig` (which rejects XmlDeserConfig), so this base method must
        // also reject cross-subclass comparisons; otherwise xml.equals(json) could be true while
        // json.equals(xml) is false, violating the Object.equals contract.
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        final DeserializationConfig<C> other = (DeserializationConfig<C>) obj;

        return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty, other.ignoreUnmatchedProperty)
                && N.equals(elementType, other.elementType) && N.equals(mapKeyType, other.mapKeyType) && N.equals(mapValueType, other.mapValueType)
                && N.equals(valueTypeMap, other.valueTypeMap) && N.equals(beanInfoForValueTypes, other.beanInfoForValueTypes);
    }

    /**
     * Returns a string representation of this configuration object.
     * The string contains all configuration settings in a readable format.
     *
     * @return a string representation of this configuration
     */
    @Override
    public String toString() {
        return "{ignoredPropNames=" + N.toString(getIgnoredPropNames()) + ", ignoreUnmatchedProperty=" + N.toString(ignoreUnmatchedProperty) + ", elementType="
                + N.toString(elementType) + ", mapKeyType=" + N.toString(mapKeyType) + ", mapValueType=" + N.toString(mapValueType) + ", valueTypeMap="
                + N.toString(valueTypeMap) + ", beanInfoForValueTypes=" + N.toString(beanInfoForValueTypes) + "}";
    }
}
