/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * A map-based entity implementation that stores property values in a HashMap.
 * This class is designed to store the properties' values of an object dynamically,
 * making it useful for scenarios where the structure is not known at compile time.
 * 
 * <p>This class supports both simple property names and canonical property names
 * (e.g., "EntityName.propertyName"). When using canonical names, the entity name
 * prefix is automatically handled.</p>
 * 
 * <p><strong>Note:</strong> This object is used to store the properties' values of an object.
 * It should not set or get values for another object's property.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * MapEntity user = new MapEntity("User");
 * user.set("name", "John");
 * user.set("age", 30);
 * 
 * String name = user.get("name"); // returns "John"
 * int age = user.get("age", Integer.class); // returns 30
 * }</pre>
 * 
 * @since 1.0
 */
@Internal
public final class MapEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = -6595007303962724540L;

    private final String entityName;

    private final Map<String, Object> values = new HashMap<>();

    /**
     * Constructor for Kryo serialization.
     * Creates a MapEntity with an empty entity name.
     */
    MapEntity() {
        this(Strings.EMPTY);
    }

    /**
     * Constructs a new MapEntity with the specified entity name.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity product = new MapEntity("Product");
     * }</pre>
     * 
     * @param entityName the name of the entity
     */
    public MapEntity(final String entityName) {
        this.entityName = entityName;
    }

    /**
     * Constructs a new MapEntity with the specified entity name and initial properties.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> props = new HashMap<>();
     * props.put("id", 1);
     * props.put("name", "Widget");
     * MapEntity product = new MapEntity("Product", props);
     * }</pre>
     * 
     * @param entityName the name of the entity
     * @param props the initial properties to set
     */
    public MapEntity(final String entityName, final Map<String, Object> props) {
        this(entityName);

        set(props);
    }

    /**
     * Returns the entity name.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * String name = user.entityName(); // returns "User"
     * }</pre>
     * 
     * @return the entity name
     */
    public String entityName() {
        return entityName;
    }

    /**
     * Gets the value of the specified property.
     * Supports both simple property names and canonical names (e.g., "EntityName.propertyName").
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("email", "john@example.com");
     * 
     * String email = user.get("email"); // returns "john@example.com"
     * String sameEmail = user.get("User.email"); // also returns "john@example.com"
     * }</pre>
     * 
     * @param <T> the type of the property value
     * @param propName the property name (can be simple or canonical)
     * @return the property value, or null if not found
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
     * Gets the value of the specified property and converts it to the target type.
     * If the property value is null, returns the default value for the target type.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("age", "25");
     * 
     * Integer age = user.get("age", Integer.class); // returns 25
     * Boolean active = user.get("active", Boolean.class); // returns {@code false} (default)
     * }</pre>
     * 
     * @param <T> the target type
     * @param propName the property name (can be simple or canonical)
     * @param targetType the class of the target type to convert to
     * @return the property value converted to the target type, or the default value if null
     */
    public <T> T get(final String propName, final Class<? extends T> targetType) {
        Object propValue = get(propName);

        if (propValue == null) {
            propValue = N.defaultValueOf(targetType);
        }

        return N.convert(propValue, targetType);
    }

    /**
     * Sets the value of the specified property.
     * Supports both simple property names and canonical names (e.g., "EntityName.propertyName").
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("name", "John")
     *     .set("age", 30)
     *     .set("User.email", "john@example.com"); // canonical name
     * }</pre>
     * 
     * @param propName the property name (can be simple or canonical)
     * @param propValue the property value to set
     * @return this MapEntity instance for method chaining
     */
    public MapEntity set(String propName, final Object propValue) {
        if (NameUtil.isCanonicalName(entityName, propName)) {
            propName = NameUtil.getSimpleName(propName);
        }

        values.put(propName, propValue);

        return this;
    }

    /**
     * Sets multiple properties from the provided map.
     * Each entry in the map will be set as a property.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Map<String, Object> props = new HashMap<>();
     * props.put("name", "John");
     * props.put("age", 30);
     * 
     * MapEntity user = new MapEntity("User");
     * user.set(props);
     * }</pre>
     * 
     * @param nameValues a map of property names to values
     */
    public void set(final Map<String, Object> nameValues) {
        for (final Map.Entry<String, Object> entry : nameValues.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Removes the specified property from this entity.
     * Supports both simple property names and canonical names (e.g., "EntityName.propertyName").
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("tempData", "temp");
     * Object removed = user.remove("tempData"); // returns "temp"
     * }</pre>
     * 
     * @param propName the property name to remove (can be simple or canonical)
     * @return the previous value associated with the property, or null if there was no mapping
     */
    @SuppressWarnings("UnusedReturnValue")
    @MayReturnNull
    public Object remove(String propName) {
        if (values.isEmpty()) {
            return null;
        }

        if (NameUtil.isCanonicalName(entityName, propName)) {
            propName = NameUtil.getSimpleName(propName);
        }

        return values.remove(propName);
    }

    /**
     * Removes all specified properties from this entity.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("temp1", "value1").set("temp2", "value2");
     * user.removeAll(Arrays.asList("temp1", "temp2"));
     * }</pre>
     * 
     * @param propNames a collection of property names to remove
     */
    public void removeAll(final Collection<String> propNames) { // NOSONAR
        for (final String propName : propNames) {
            remove(propName);
        }
    }

    /**
     * Checks if this entity contains a property with the specified name.
     * Supports both simple property names and canonical names (e.g., "EntityName.propertyName").
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("email", "john@example.com");
     * 
     * boolean hasEmail = user.containsKey("email"); // returns true
     * boolean hasPhone = user.containsKey("phone"); // returns false
     * }</pre>
     * 
     * @param propName the property name to check (can be simple or canonical)
     * @return {@code true} if this entity contains the specified property, {@code false} otherwise
     */
    public boolean containsKey(final String propName) {
        if (values.isEmpty()) {
            return false;
        }

        if (NameUtil.isCanonicalName(entityName, propName)) {
            return values.containsKey(NameUtil.getSimpleName(propName));

        } else {
            return values.containsKey(propName);
        }
    }

    /**
     * Returns a set of all property names that have been set in this entity.
     * The returned set contains simple property names (without entity name prefix).
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("name", "John").set("age", 30);
     * 
     * Set<String> keys = user.keySet(); // returns ["name", "age"]
     * }</pre>
     * 
     * @return a set of property names
     * @see com.landawn.abacus.util.MapEntity#keySet()
     */
    public Set<String> keySet() {
        return values.keySet();
    }

    /**
     * Returns a set view of all property entries in this entity.
     * Each entry contains a property name and its corresponding value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("name", "John").set("age", 30);
     * 
     * for (Map.Entry<String, Object> entry : user.entrySet()) {
     *     System.out.println(entry.getKey() + ": " + entry.getValue());
     * }
     * }</pre>
     * 
     * @return a set view of the property entries
     */
    public Set<Map.Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    /**
     * Returns the underlying map containing all properties.
     * The returned map is the actual internal storage, so modifications to it
     * will affect this MapEntity.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("name", "John");
     * 
     * Map<String, Object> props = user.props();
     * props.put("age", 30); // This modifies the MapEntity
     * }</pre>
     * 
     * @return the internal map of properties
     */
    public Map<String, Object> props() {
        return values;
    }

    /**
     * Returns the number of properties in this entity.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("name", "John").set("age", 30);
     * int count = user.size(); // returns 2
     * }</pre>
     * 
     * @return the number of properties
     */
    public int size() {
        return values.size();
    }

    /**
     * Checks if this entity has no properties.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * boolean empty = user.isEmpty(); // returns true
     * 
     * user.set("name", "John");
     * empty = user.isEmpty(); // returns false
     * }</pre>
     * 
     * @return {@code true} if this entity has no properties, {@code false} otherwise
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Creates a copy of this MapEntity with the same entity name and properties.
     * The copy is a shallow copy - the property values themselves are not cloned.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity original = new MapEntity("User");
     * original.set("name", "John").set("age", 30);
     * 
     * MapEntity copy = original.copy();
     * copy.set("age", 31); // Doesn't affect original
     * }</pre>
     * 
     * @return a new MapEntity instance with the same entity name and properties
     */
    public MapEntity copy() {
        return new MapEntity(entityName, values);
    }

    /**
     * Returns a hash code value for this MapEntity.
     * The hash code is based on both the entity name and the properties.
     * 
     * @return a hash code value for this object
     */
    @Override
    public int hashCode() {
        int h = 17;
        h = (h * 31) + entityName.hashCode();
        return (h * 31) + values.hashCode();
    }

    /**
     * Compares this MapEntity to the specified object for equality.
     * Two MapEntity objects are equal if they have the same entity name
     * and contain the same properties with equal values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user1 = new MapEntity("User");
     * user1.set("name", "John");
     * 
     * MapEntity user2 = new MapEntity("User");
     * user2.set("name", "John");
     * 
     * boolean equal = user1.equals(user2); // returns true
     * }</pre>
     * 
     * @param obj the object to compare with
     * @return {@code true} if this object is equal to the specified object, {@code false} otherwise
     */
    @SuppressFBWarnings
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof MapEntity other) {
            return N.equals(entityName, other.entityName) && N.equals(values, other.values);
        }

        return false;
    }

    /**
     * Returns a string representation of this MapEntity.
     * The string representation is the same as the string representation
     * of the underlying properties map.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = new MapEntity("User");
     * user.set("name", "John").set("age", 30);
     * String str = user.toString(); // returns "{name=John, age=30}"
     * }</pre>
     * 
     * @return a string representation of this object
     */
    @Override
    public String toString() {
        return values.toString();
    }

    /**
     * Creates a builder for constructing MapEntity instances.
     * The builder provides a fluent API for setting properties.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MapEntity user = MapEntity.builder("User")
     *     .put("name", "John")
     *     .put("age", 30)
     *     .put("email", "john@example.com")
     *     .build();
     * }</pre>
     * 
     * @param entityName the name of the entity
     * @return a new MapEntityBuilder instance
     */
    public static MapEntityBuilder builder(final String entityName) {
        return new MapEntityBuilder(entityName);
    }

    /**
     * A builder class for constructing MapEntity instances with a fluent API.
     * This builder allows chaining multiple property settings before building
     * the final MapEntity instance.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * MapEntity product = MapEntity.builder("Product")
     *     .put("id", 123)
     *     .put("name", "Widget")
     *     .put("price", 19.99)
     *     .build();
     * }</pre>
     */
    public static class MapEntityBuilder {
        private final MapEntity mapEntity;

        /**
         * Constructs a new builder for the specified entity name.
         * 
         * @param entityName the name of the entity
         */
        MapEntityBuilder(final String entityName) {
            mapEntity = new MapEntity(entityName);
        }

        /**
         * Adds a property to the entity being built.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * builder.put("name", "John")
         *        .put("age", 30);
         * }</pre>
         * 
         * @param idPropName the property name
         * @param idPropVal the property value
         * @return this builder instance for method chaining
         */
        public MapEntityBuilder put(final String idPropName, final Object idPropVal) {
            mapEntity.set(idPropName, idPropVal);

            return this;
        }

        /**
         * Builds and returns the MapEntity instance.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * MapEntity entity = builder.build();
         * }</pre>
         * 
         * @return the constructed MapEntity instance
         */
        public MapEntity build() {
            return mapEntity;
        }
    }
}