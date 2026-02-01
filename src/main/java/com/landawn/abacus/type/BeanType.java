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

package com.landawn.abacus.type;

import com.landawn.abacus.util.Strings;

/**
 * Type handler for JavaBean objects.
 * This class provides JSON-based serialization and deserialization for arbitrary JavaBean types,
 * enabling conversion between bean instances and their JSON string representations.
 *
 * <p>BeanType instances are typically obtained through the TypeFactory and are used internally
 * by the serialization framework. Users rarely need to interact with BeanType directly,
 * as the framework automatically handles bean conversions.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get BeanType through TypeFactory
 * Type<User> userType = TypeFactory.getType(User.class);
 *
 * // Serialize bean to string
 * User user = new User("John", "john@example.com");
 * String json = userType.stringOf(user);
 * // Result: {"name":"John","email":"john@example.com"}
 *
 * // Deserialize string to bean
 * String jsonInput = "{\"name\":\"Jane\",\"email\":\"jane@example.com\"}";
 * User parsedUser = userType.valueOf(jsonInput);
 * }</pre>
 *
 * @param <T> the JavaBean type this handler manages
 */
@SuppressWarnings("java:S2160")
public final class BeanType<T> extends AbstractType<T> {

    private final Class<T> typeClass;
    private final java.lang.reflect.Type javaType;

    /**
     * Package-private constructor for BeanType.
     * This constructor is called by the TypeFactory to create BeanType instances
     * for arbitrary JavaBean classes.
     *
     * @param clazz the Class object representing the bean type
     * @param javaType the Java reflection Type for the bean (may be {@code null} for simple classes)
     */
    BeanType(final Class<T> clazz, final java.lang.reflect.Type javaType) {
        super(javaType == null ? TypeFactory.getClassName(clazz) : TypeFactory.getJavaTypeName(javaType));
        this.typeClass = clazz;
        this.javaType = javaType == null ? clazz : javaType;
    }

    /**
     * Returns the Class object representing the bean type.
     *
     * @return the Class object for the specific bean type T
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Returns the Java Type representation of the bean type.
     *
     * @return the Java Type for the specific bean type T
     */
    @Override
    public java.lang.reflect.Type javaType() {
        return javaType;
    }

    /**
     * Determines whether this type represents a JavaBean.
     * Always returns {@code true} for BeanType instances.
     *
     * @return {@code true} indicating this is a bean type
     */
    @Override
    public boolean isBean() {
        return true;
    }

    /**
     * Determines whether this bean type is directly serializable.
     * Bean types require JSON conversion and are not directly serializable.
     *
     * @return {@code false} indicating beans are not directly serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Gets the serialization type classification for this bean type.
     * Beans are classified as ENTITY types in the serialization system.
     *
     * @return SerializationType.ENTITY indicating this is an entity type
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.ENTITY;
    }

    /**
     * Converts a bean instance to its JSON string representation.
     * Uses the internal JSON parser to serialize the bean with default configuration.
     *
     * @param x the bean instance to serialize
     * @return the JSON string representation of the bean, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Converts a JSON string representation back to a bean instance.
     * Uses the internal JSON parser to deserialize the string into the bean type.
     *
     * @param str the JSON string to deserialize
     * @return a new instance of the bean type populated from the JSON data,
     *         or {@code null} if the input string is {@code null} or empty
     * @throws ParseException if the JSON is invalid or cannot be mapped to the bean type
     */
    @Override
    public T valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, Type.of(javaType));
    }
}
