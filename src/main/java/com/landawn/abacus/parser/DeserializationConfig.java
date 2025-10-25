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
import com.landawn.abacus.util.Beans;
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
 * DeserializationConfig config = new MyDeserializationConfig()
 *     .ignoreUnmatchedProperty(true)
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
 * @param <C> the concrete configuration type for method chaining
 * @since 0.8
 * @see JSONDeserializationConfig
 * @see XMLDeserializationConfig
 */
public abstract class DeserializationConfig<C extends DeserializationConfig<C>> extends ParserConfig<C> {

    boolean ignoreUnmatchedProperty = true;

    Type<?> elementType;

    Type<?> mapKeyType;

    Type<?> mapValueType;

    Map<String, Type<?>> valueTypeMap;

    BeanInfo beanInfoForValueTypes;

    /**
     * Checks if unmatched properties should be ignored during deserialization.
     * When set to true (default), properties in the source data that don't match
     * any property in the target class will be silently ignored. When false,
     * unmatched properties will cause an error.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (config.ignoreUnmatchedProperty()) {
     *     // Extra properties in JSON will be ignored
     * }
     * }</pre>
     *
     * @return {@code true} if unknown properties can be ignored, {@code false} otherwise
     */
    public boolean ignoreUnmatchedProperty() {
        return ignoreUnmatchedProperty;
    }

    /**
     * Sets whether unmatched properties should be ignored during deserialization.
     * This is useful when deserializing data that may contain extra fields not
     * present in the target class.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // JSON: {"name": "John", "age": 30, "unknownField": "value"}
     * // Target class only has name and age properties
     * config.ignoreUnmatchedProperty(true);  // unknownField will be ignored
     * config.ignoreUnmatchedProperty(false); // unknownField will cause an error
     * }</pre>
     *
     * @param ignoreUnmatchedProperty {@code true} to ignore unmatched properties, {@code false} to throw errors
     * @return this instance for method chaining
     */
    public C ignoreUnmatchedProperty(final boolean ignoreUnmatchedProperty) {
        this.ignoreUnmatchedProperty = ignoreUnmatchedProperty;

        return (C) this;
    }

    /**
     * Gets the element type for collection and array deserialization.
     * This type is used when deserializing JSON arrays or XML sequences
     * to determine the type of elements in the collection.
     *
     * @param <T> the element type
     * @return the configured element type, or null if not set
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
     * List<Person> people = parser.deserialize(jsonArray, List.class);
     * }</pre>
     *
     * @param cls the class of collection/array elements
     * @return this instance for method chaining
     */
    public C setElementType(final Class<?> cls) {
        return setElementType(N.typeOf(cls));
    }

    /**
     * Sets the element type for collection and array deserialization using a Type.
     * This allows for more complex type specifications including generic types.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For complex generic types
     * Type<List<String>> listType = N.typeOf("List<String>");
     * config.setElementType(listType);
     * }</pre>
     *
     * @param type the type of collection/array elements
     * @return this instance for method chaining
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
     * @return this instance for method chaining
     */
    public C setElementType(final String type) {
        return setElementType(N.typeOf(type));
    }

    /**
     * Gets the key type for map deserialization.
     * This type is used when deserializing to Map instances to determine
     * the type of keys in the map.
     *
     * @param <T> the key type
     * @return the configured map key type, or null if not set
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
     * @return this instance for method chaining
     */
    public C setMapKeyType(final Class<?> cls) {
        return this.setMapKeyType(N.typeOf(cls));
    }

    /**
     * Sets the key type for map deserialization using a Type.
     * This allows for more complex key type specifications.
     *
     * @param keyType the type of map keys
     * @return this instance for method chaining
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
     * @return this instance for method chaining
     */
    public C setMapKeyType(final String keyType) {
        return this.setMapKeyType(N.typeOf(keyType));
    }

    /**
     * Gets the value type for map deserialization.
     * This type is used when deserializing to Map instances to determine
     * the type of values in the map.
     *
     * @param <T> the value type
     * @return the configured map value type, or null if not set
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
     * Map<String, Person> people = parser.deserialize(json, Map.class);
     * }</pre>
     *
     * @param cls the class of map values
     * @return this instance for method chaining
     */
    public C setMapValueType(final Class<?> cls) {
        return this.setMapValueType(N.typeOf(cls));
    }

    /**
     * Sets the value type for map deserialization using a Type.
     * This allows for more complex value type specifications including generics.
     *
     * @param valueType the type of map values
     * @return this instance for method chaining
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
     * @return this instance for method chaining
     */
    public C setMapValueType(final String valueType) {
        return this.setMapValueType(N.typeOf(valueType));
    }

    /**
     * Checks if any value types have been configured.
     * Returns true if either individual value types have been set for specific properties
     * or a bean class has been set for value type information.
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
     * <p>The key name supports nested properties using dot notation.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Address> addressType = config.getValueType("address");
     * Type<String> cityType = config.getValueType("address.city");
     * }</pre>
     *
     * @param <T> the value type
     * @param keyName the property name, supporting nested properties (e.g., "account.devices.model")
     * @return the type for the specified property, or null if not configured
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
     * Type<String> stringType = N.typeOf(String.class);
     * Type<?> type = config.getValueType("unknownProp", stringType);
     * // Returns stringType if "unknownProp" is not configured
     * }</pre>
     *
     * @param <T> the value type
     * @param keyName the property name, supporting nested properties (e.g., "account.devices.model")
     * @param defaultType the type to return if no type is configured for the property
     * @return the type for the specified property, or defaultType if not configured
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
     * Gets the value type class for a specific property by its key name.
     * This is a convenience method that returns the raw class type
     * instead of the Type wrapper.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Address> addressClass = config.getValueTypeClass("address");
     * }</pre>
     *
     * @param <T> the value type
     * @param keyName the property name, supporting nested properties (e.g., "account.devices.model")
     * @return the class for the specified property, or null if not configured
     */
    public <T> Class<T> getValueTypeClass(final String keyName) {
        return getValueTypeClass(keyName, null);
    }

    /**
     * Gets the value type class for a specific property with a default class.
     * If no type is configured for the specified property, returns the default class.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<?> typeClass = config.getValueTypeClass("data", HashMap.class);
     * // Returns HashMap.class if "data" property type is not configured
     * }</pre>
     *
     * @param <T> the value type
     * @param keyName the property name, supporting nested properties (e.g., "account.devices.model")
     * @param defaultTypeClass the class to return if no type is configured for the property
     * @return the class for the specified property, or defaultTypeClass if not configured
     */
    public <T> Class<T> getValueTypeClass(final String keyName, final Class<T> defaultTypeClass) {
        final Type<T> ret = getValueType(keyName);

        return ret == null ? defaultTypeClass : ret.clazz();
    }

    /**
     * Sets the value type for a specific property using a Class.
     * This allows fine-grained control over deserialization of nested properties.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * config.setValueType("address", Address.class);
     * config.setValueType("phoneNumbers", List.class);
     * config.setValueType("metadata.tags", Set.class);
     * }</pre>
     *
     * @param keyName the property name, supporting nested properties (e.g., "account.devices.model")
     * @param typeClass the class to use for deserializing this property
     * @return this instance for method chaining
     */
    public C setValueType(final String keyName, final Class<?> typeClass) {
        return setValueType(keyName, N.typeOf(typeClass));
    }

    /**
     * Sets the value type for a specific property using a Type.
     * This allows for complex type specifications including generics.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<List<String>> listType = N.typeOf("List<String>");
     * config.setValueType("tags", listType);
     * }</pre>
     *
     * @param keyName the property name, supporting nested properties (e.g., "account.devices.model")
     * @param type the type to use for deserializing this property
     * @return this instance for method chaining
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
     * @param keyName the property name, supporting nested properties (e.g., "account.devices.model")
     * @param typeName the type name string
     * @return this instance for method chaining
     */
    public C setValueType(final String keyName, final String typeName) {
        return setValueType(keyName, N.typeOf(typeName));
    }

    /**
     * Sets multiple value types at once using a map.
     * This is useful when configuring types for many properties at once.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Type<?>> types = new HashMap<>();
     * types.put("address", N.typeOf(Address.class));
     * types.put("items", N.typeOf("List<Item>"));
     * config.setValueTypes(types);
     * }</pre>
     *
     * @param valueTypes map of property names to their types
     * @return this instance for method chaining
     */
    public C setValueTypes(final Map<String, Type<?>> valueTypes) {
        valueTypeMap = valueTypes;

        return (C) this;
    }

    /**
     * Sets value types by analyzing a bean class.
     * This method extracts type information from all properties of the specified
     * bean class and uses it for deserialization type resolution.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // If Person class has properties: name (String), age (int), address (Address)
     * config.setValueTypesByBeanClass(Person.class);
     * // Now "name", "age", and "address" properties will use their declared types
     * }</pre>
     *
     * @param beanClass the bean class to extract type information from
     * @return this instance for method chaining
     * @throws IllegalArgumentException if the specified class is not a valid bean class
     */
    public C setValueTypesByBeanClass(final Class<?> beanClass) throws IllegalArgumentException {
        if (beanClass == null) {
            beanInfoForValueTypes = null;
        } else {
            N.checkArgument(Beans.isBeanClass(beanClass), "{} is not a valid bean class", beanClass);

            beanInfoForValueTypes = ParserUtil.getBeanInfo(beanClass);
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

        if (obj instanceof DeserializationConfig) {
            final DeserializationConfig<C> other = (DeserializationConfig<C>) obj;

            return N.equals(getIgnoredPropNames(), other.getIgnoredPropNames()) && N.equals(ignoreUnmatchedProperty, other.ignoreUnmatchedProperty)
                    && N.equals(elementType, other.elementType) && N.equals(mapKeyType, other.mapKeyType) && N.equals(mapValueType, other.mapValueType)
                    && N.equals(valueTypeMap, other.valueTypeMap) && N.equals(beanInfoForValueTypes, other.beanInfoForValueTypes);
        }

        return false;
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