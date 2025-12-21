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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import com.landawn.abacus.annotation.Immutable;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;

/**
 * Simple Entity ID (Seid) - A flexible entity identifier implementation.
 * This class represents an entity identifier that can hold multiple property-value pairs
 * that uniquely identify an entity.
 * 
 * <p>Seid is designed to work with entity objects and their ID properties, providing
 * a convenient way to create, manipulate, and compare entity identifiers. It maintains
 * properties in a sorted order for consistent string representation and comparison.
 * 
 * <p>Key features:
 * <ul>
 *   <li>Supports single or multiple ID properties</li>
 *   <li>Automatic property name extraction from canonical names</li>
 *   <li>Type-safe property value retrieval with conversion support</li>
 *   <li>Immutable-style API (though internally mutable for performance)</li>
 *   <li>Consistent string representation for debugging and logging</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Single property ID
 * Seid userId = Seid.of("User.id", 123);
 * 
 * // Multiple property ID
 * Seid compositeId = Seid.of("Order.customerId", 456, "Order.orderId", 789);
 * 
 * // Create from entity object
 * User user = new User(123, "John");
 * Seid entityId = Seid.create(user);
 * 
 * // Retrieve values
 * int id = userId.getInt("id");
 * Long customerId = compositeId.get("customerId");
 * }</pre>
 * 
 * @see EntityId
 */
public class Seid implements EntityId {

    private static final Comparator<String> keyComparator = Comparators.NATURAL_ORDER;

    private final String entityName;

    private Map<String, Object> values = Collections.emptyMap();

    private String strValue;

    // for Kryo.
    protected Seid() {
        entityName = Strings.EMPTY;
    }

    /**
     * Creates a new Seid with the specified entity name.
     * This constructor is deprecated and for internal use only.
     *
     * @param entityName the name of the entity
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Seid(final String entityName) {
        //    if (N.isEmpty(entityName)) {
        //        throw new IllegalArgumentException("Entity name can't be null or empty");
        //    }

        this.entityName = entityName == null ? Strings.EMPTY : entityName;
    }

    /**
     * Creates a new Seid with a single property-value pair.
     * The entity name is automatically extracted from the property name.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid userId = new Seid("User.id", 123);
     * // Entity name will be "User", property name will be "id"
     * }</pre>
     *
     * @param propName the property name (can be canonical like "Entity.property" or simple like "property")
     * @param propValue the property value
     */
    public Seid(final String propName, final Object propValue) {
        this(NameUtil.getParentName(propName));

        set(propName, propValue); // NOSONAR
    }

    /**
     * Creates a new Seid from a map of property names to values.
     * The entity name is extracted from the first property name in the map.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> props = new HashMap<>();
     * props.put("User.id", 123);
     * props.put("User.version", 1);
     * Seid seid = new Seid(props);
     * }</pre>
     *
     * @param nameValues a map of property names to their values
     */
    public Seid(final Map<String, Object> nameValues) {
        this(NameUtil.getParentName(nameValues.keySet().iterator().next()));

        set(nameValues); // NOSONAR
    }

    /**
     * Creates a new Seid with the specified entity name.
     * This factory method is deprecated and for internal use only.
     *
     * @param entityName the name of the entity
     * @return a new Seid instance
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public static Seid of(final String entityName) {
        return new Seid(entityName);
    }

    /**
     * Creates a new Seid with a single property-value pair.
     * This is a convenient factory method alternative to the constructor.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid userId = Seid.of("User.id", 123);
     * }</pre>
     *
     * @param propName the property name
     * @param propValue the property value
     * @return a new Seid instance
     */
    public static Seid of(final String propName, final Object propValue) {
        return new Seid(propName, propValue);
    }

    /**
     * Creates a new Seid with two property-value pairs.
     * This is useful for composite keys with exactly two components.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid compositeId = Seid.of("Order.customerId", 123, "Order.orderId", 456);
     * }</pre>
     *
     * @param propName1 the first property name
     * @param propValue1 the first property value
     * @param propName2 the second property name
     * @param propValue2 the second property value
     * @return a new Seid instance
     */
    public static Seid of(final String propName1, final Object propValue1, final String propName2, final Object propValue2) {
        final Seid result = new Seid(propName1, propValue1);
        result.set(propName2, propValue2);
        return result;
    }

    /**
     * Creates a new Seid with three property-value pairs.
     * This is useful for composite keys with exactly three components.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid compositeId = Seid.of("Entity.prop1", val1, "Entity.prop2", val2, "Entity.prop3", val3);
     * }</pre>
     *
     * @param propName1 the first property name
     * @param propValue1 the first property value
     * @param propName2 the second property name
     * @param propValue2 the second property value
     * @param propName3 the third property name
     * @param propValue3 the third property value
     * @return a new Seid instance
     */
    public static Seid of(final String propName1, final Object propValue1, final String propName2, final Object propValue2, final String propName3,
            final Object propValue3) {
        final Seid result = new Seid(propName1, propValue1);
        result.set(propName2, propValue2);
        result.set(propName3, propValue3);
        return result;
    }

    /**
     * Creates a new Seid from a map of property names to values.
     * This is an alternative factory method to the constructor.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> props = Map.of("User.id", 123, "User.version", 1);
     * Seid seid = Seid.create(props);
     * }</pre>
     *
     * @param nameValues a map of property names to their values
     * @return a new Seid instance
     * @throws IllegalArgumentException if nameValues is {@code null} or empty
     */
    public static Seid create(final Map<String, Object> nameValues) {
        N.checkArgNotEmpty(nameValues, "nameValues");

        return new Seid(nameValues);
    }

    /**
     * Creates a new Seid from an entity object by extracting its ID properties.
     * The entity class must have properties annotated as ID fields.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * @Entity
     * public class User {
     *     @Id
     *     private Long id;
     *     private String name;
     *     // getters/setters...
     * }
     * 
     * User user = new User(123L, "John");
     * Seid userId = Seid.create(user);   // Will contain id=123
     * }</pre>
     *
     * @param entity the entity object to extract ID from
     * @return a new Seid containing the entity's ID properties
     * @throws IllegalArgumentException if no ID properties are defined in the entity class
     */
    public static Seid create(final Object entity) {
        final List<String> idPropNames = Seid.getIdFieldNames(entity.getClass());

        if (N.isEmpty(idPropNames)) {
            throw new IllegalArgumentException("No id property defined in class: " + ClassUtil.getCanonicalClassName(entity.getClass()));
        }

        return create(entity, idPropNames);
    }

    /**
     * Creates a new Seid from an entity object using specified property names as IDs.
     * This allows custom selection of which properties to use as identifiers.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User(123L, "john@example.com", "John");
     * Seid emailId = Seid.create(user, Arrays.asList("email"));
     * }</pre>
     *
     * @param entity the entity object to extract values from
     * @param idPropNames the names of properties to use as ID
     * @return a new Seid containing the specified properties
     * @throws IllegalArgumentException if idPropNames is {@code null} or empty
     */
    public static Seid create(final Object entity, final Collection<String> idPropNames) {
        if (N.isEmpty(idPropNames)) {
            throw new IllegalArgumentException("Id property names cannot be null or empty");
        }

        final Class<?> cls = entity.getClass();
        final BeanInfo entityInfo = ParserUtil.getBeanInfo(cls);
        final Seid seid = Seid.of(ClassUtil.getSimpleClassName(cls));

        for (final String idPropName : idPropNames) {
            seid.set(idPropName, entityInfo.getPropInfo(idPropName).getPropValue(entity));
        }

        return seid;
    }

    /**
     * Returns the entity name associated with this Seid.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid userId = Seid.of("User.id", 123);
     * String entityName = userId.entityName();   // Returns "User"
     * }</pre>
     *
     * @return the entity name
     */
    @Override
    public String entityName() {
        return entityName;
    }

    /**
     * Gets the value of the specified property.
     * Supports both simple property names and canonical names.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.id", 123);
     * Integer id = seid.get("id");         // Returns 123
     * Integer id2 = seid.get("User.id");   // Also returns 123
     * }</pre>
     *
     * @param <T> the expected type of the property value
     * @param propName the property name
     * @return the property value, or {@code null} if not found
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(final String propName) {
        if (NameUtil.isCanonicalName(entityName, propName)) {
            return (T) values.get(NameUtil.getSimpleName(propName));
        }
        return (T) values.get(propName);
    }

    /**
     * Gets the value of the specified property as an int.
     * Performs automatic conversion if the stored value is not an int.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.age", "25");
     * int age = seid.getInt("age");   // Returns 25 (converted from String)
     * }</pre>
     *
     * @param propName the property name
     * @return the property value as an int
     */
    @Override
    public int getInt(final String propName) {
        final Object value = get(propName);
        return value instanceof Number ? ((Number) value).intValue() : N.convert(value, int.class);
    }

    /**
     * Gets the value of the specified property as a long.
     * Performs automatic conversion if the stored value is not a long.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.id", 123);
     * long id = seid.getLong("id");   // Returns 123L
     * }</pre>
     *
     * @param propName the property name
     * @return the property value as a long
     */
    @Override
    public long getLong(final String propName) {
        final Object value = get(propName);
        return value instanceof Number ? ((Number) value).longValue() : N.convert(value, long.class);
    }

    /**
     * Gets the value of the specified property, converting it to the target type if necessary.
     * Returns the default value for the target type if the property value is {@code null}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.active", "true");
     * Boolean active = seid.get("active", Boolean.class);   // Returns Boolean.TRUE
     * }</pre>
     *
     * @param <T> the target type
     * @param propName the property name
     * @param targetType the class of the target type
     * @return the property value converted to the target type
     */
    @Override
    public <T> T get(final String propName, final Class<? extends T> targetType) {
        Object propValue = get(propName);

        if (propValue == null) {
            propValue = N.defaultValueOf(targetType);
        }

        return N.convert(propValue, targetType);
    }

    /**
     * Sets a property value in this Seid.
     * This method is deprecated and for internal use only.
     * Supports both simple and canonical property names.
     *
     * @param propName the property name
     * @param propValue the property value
     * @return this Seid instance for method chaining
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Seid set(final String propName, final Object propValue) {
        final String simplePropName = NameUtil.isCanonicalName(entityName, propName) ? NameUtil.getSimpleName(propName) : propName;

        if (values.isEmpty() || (values.size() == 1 && values.containsKey(simplePropName))) {
            values = Collections.singletonMap(simplePropName, propValue);
        } else {
            final Map<String, Object> newValues = new TreeMap<>(keyComparator);
            newValues.putAll(values);
            values = newValues;

            values.put(simplePropName, propValue);
        }

        strValue = null;

        return this;
    }

    /**
     * Sets multiple property values in this Seid.
     * This method is deprecated and for internal use only.
     *
     * @param nameValues a map of property names to their values
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public void set(final Map<String, Object> nameValues) {
        if (N.isEmpty(nameValues)) {
            return;
        }
        if (nameValues.size() == 1) {
            final Map.Entry<String, Object> entry = nameValues.entrySet().iterator().next();
            set(entry.getKey(), entry.getValue());
        } else {
            if (!(values instanceof TreeMap)) {
                final Map<String, Object> newValues = new TreeMap<>(keyComparator);
                newValues.putAll(values);
                values = newValues;
            }

            for (final Map.Entry<String, Object> entry : nameValues.entrySet()) {
                if (NameUtil.isCanonicalName(entityName, entry.getKey())) {
                    values.put(NameUtil.getSimpleName(entry.getKey()), entry.getValue());
                } else {
                    values.put(entry.getKey(), entry.getValue());
                }
            }
        }

        strValue = null;
    }

    /**
     * Checks if this Seid contains the specified property.
     * Supports both simple and canonical property names.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.id", 123);
     * boolean hasId = seid.containsKey("id");                 // Returns true
     * boolean hasIdCanonical = seid.containsKey("User.id");   // Also returns true
     * }</pre>
     *
     * @param propName the property name to check
     * @return {@code true} if the property exists, {@code false} otherwise
     */
    @Override
    public boolean containsKey(final String propName) {
        if (values.isEmpty()) {
            return false;
        }

        if (NameUtil.isCanonicalName(entityName, propName)) {
            return values.containsKey(NameUtil.getSimpleName(propName));
        }

        return values.containsKey(propName);
    }

    /**
     * Returns a set view of the property names in this Seid.
     * The returned set is backed by the Seid, so changes to the Seid
     * are reflected in the set.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.id", 123, "User.name", "John");
     * Set<String> keys = seid.keySet();   // Contains ["id", "name"]
     * }</pre>
     *
     * @return a set view of the property names
     */
    @Override
    public Set<String> keySet() {
        return values.keySet();
    }

    /**
     * Returns a set view of the property entries in this Seid.
     * Each entry contains a property name and its corresponding value.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.id", 123);
     * for (Map.Entry<String, Object> entry : seid.entrySet()) {
     *     System.out.println(entry.getKey() + " = " + entry.getValue());
     * }
     * }</pre>
     *
     * @return a set view of the property entries
     */
    @Override
    public Set<Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    /**
     * Returns the number of properties in this Seid.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.id", 123, "User.version", 1);
     * int count = seid.size();   // Returns 2
     * }</pre>
     *
     * @return the number of properties
     */
    @Override
    public int size() {
        return values.size();
    }

    /**
     * Checks if this Seid contains no properties.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User");
     * boolean empty = seid.isEmpty();   // Returns true
     * }</pre>
     *
     * @return {@code true} if this Seid contains no properties, {@code false} otherwise
     */
    @Override
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * Removes all properties from this Seid.
     * This method is deprecated and for internal use only.
     * 
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public void clear() {
        values = Collections.emptyMap();

        strValue = null;
    }

    /**
     * Creates a copy of this Seid.
     * The copy contains the same entity name and all property-value pairs.
     * This method is deprecated and for internal use only.
     *
     * @return a copy of this Seid
     * @deprecated for internal use only
     */
    @Deprecated
    @Internal
    public Seid copy() {
        final Seid copy = new Seid(entityName);

        copy.set(values);
        copy.strValue = strValue;

        return copy;
    }

    /**
     * Checks if this Seid is equal to another object.
     * Two Seids are equal if they have the same string representation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid1 = Seid.of("User.id", 123);
     * Seid seid2 = Seid.of("User.id", 123);
     * boolean equal = seid1.equals(seid2);   // Returns true
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        return obj instanceof EntityId && toString().equals(obj.toString());
    }

    /**
     * Returns a hash code value for this Seid.
     * The hash code is based on the string representation.
     *
     * @return a hash code value
     */
    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    /**
     * Returns a string representation of this Seid.
     * The format is: "EntityName: {prop1=value1, prop2=value2, ...}"
     * Properties are sorted by name for consistent representation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seid seid = Seid.of("User.id", 123, "User.name", "John");
     * String str = seid.toString();   // Returns "User: {id=123, name=John}"
     * }</pre>
     *
     * @return a string representation of this Seid
     */
    @Override
    public String toString() {
        return stringValue();
    }

    private String stringValue() {
        if (strValue == null) {

            final Set<Map.Entry<String, Object>> entrySet = values.entrySet();

            switch (values.size()) {
                case 0: {
                    strValue = entityName + ": {}";

                    break;
                }

                case 1: {
                    final Map.Entry<String, Object> entry = entrySet.iterator().next();
                    final String propName = NameUtil.isCanonicalName(entityName, entry.getKey()) ? NameUtil.getSimpleName(entry.getKey()) : entry.getKey();

                    strValue = entityName + ": {" + propName + "=" + N.stringOf(entry.getValue()) + "}";

                    break;
                }

                case 2: {
                    final Iterator<Map.Entry<String, Object>> it = entrySet.iterator();
                    final Map.Entry<String, Object> entry1 = it.next();
                    final String propName1 = NameUtil.isCanonicalName(entityName, entry1.getKey()) ? NameUtil.getSimpleName(entry1.getKey()) : entry1.getKey();
                    final Map.Entry<String, Object> entry2 = it.next();
                    final String propName2 = NameUtil.isCanonicalName(entityName, entry2.getKey()) ? NameUtil.getSimpleName(entry2.getKey()) : entry2.getKey();

                    strValue = entityName + ": {" + propName1 + "=" + N.stringOf(entry1.getValue()) + Strings.ELEMENT_SEPARATOR + propName2 + "="
                            + N.stringOf(entry2.getValue()) + "}";

                    break;
                }

                default: {

                    final List<String> keys = new ArrayList<>(values.keySet());
                    N.sort(keys);

                    final StringBuilder sb = Objectory.createStringBuilder();

                    sb.append(entityName);
                    sb.append(": {");

                    int i = 0;

                    for (final Map.Entry<String, Object> entry : entrySet) {
                        if (i++ > 0) {
                            sb.append(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                        }

                        final String propName = NameUtil.isCanonicalName(entityName, entry.getKey()) ? NameUtil.getSimpleName(entry.getKey()) : entry.getKey();

                        sb.append(propName);
                        sb.append('=');
                        sb.append(N.stringOf(entry.getValue()));
                    }

                    sb.append('}');

                    strValue = sb.toString();

                    Objectory.recycle(sb);
                }
            }
        }

        return strValue;
    }

    /**
     * Gets the ID field names for the specified class.
     * Returns field names that are annotated as ID fields in the class.
     * This method is for internal use only.
     *
     * @param targetClass the class to inspect
     * @return an immutable list of ID field names
     * @deprecated for internal only
     */
    @Deprecated
    @Internal
    @Immutable
    static List<String> getIdFieldNames(final Class<?> targetClass) {
        final ImmutableList<String> idPropNames = ParserUtil.getBeanInfo(targetClass).idPropNameList;

        return N.isEmpty(idPropNames) ? N.emptyList() : idPropNames;
    }
}
