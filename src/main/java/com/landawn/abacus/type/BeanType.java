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
 * Type handler for arbitrary JavaBean (POJO) types.
 * Provides JSON-based serialization and deserialization, enabling conversion between
 * bean instances and their JSON string representations.
 *
 * <p>{@code BeanType} instances are created and cached by {@link TypeFactory} and are used
 * internally by the serialization framework. Applications typically obtain a {@code BeanType}
 * via {@code TypeFactory.getType(MyBean.class)} rather than constructing one directly.</p>
 *
 * <p>Beans are classified as {@link SerializationType#ENTITY} and are <em>not</em> directly
 * serializable in the primitive/scalar sense — they always go through JSON conversion.</p>
 *
 * @param <T> the JavaBean type managed by this handler
 */
@SuppressWarnings("java:S2160")
public final class BeanType<T> extends AbstractType<T> {

    private final Class<T> typeClass;
    private final java.lang.reflect.Type javaType;

    /**
     * Package-private constructor for {@code BeanType}.
     * Instances are created by {@link TypeFactory}; do not instantiate directly.
     *
     * @param clazz the {@code Class} object representing the concrete bean type
     * @param javaType the Java reflection {@code Type} for the bean (may be {@code null} for non-generic classes,
     *                 in which case {@code clazz} is used as the reflection type)
     */
    BeanType(final Class<T> clazz, final java.lang.reflect.Type javaType) {
        super(javaType == null ? TypeFactory.getClassName(clazz) : TypeFactory.getJavaTypeName(javaType));
        this.typeClass = clazz;
        this.javaType = javaType == null ? clazz : javaType;
    }

    /**
     * Returns the {@code Class} object representing the concrete bean type.
     *
     * @return the {@code Class} for the bean type {@code T}
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the Java reflection {@code Type} for the bean type.
     * For non-generic beans this is the same as {@link #javaType()};
     * for generic beans it is the full parameterised type.
     *
     * @return the reflection {@code Type} for the bean type {@code T}
     */
    @Override
    public java.lang.reflect.Type reflectType() {
        return javaType;
    }

    /**
     * Indicates that this type represents a JavaBean (POJO).
     *
     * @return {@code true} always, since this handler manages bean types
     */
    @Override
    public boolean isBean() {
        return true;
    }

    /**
     * Indicates that bean types are not directly serializable as scalar values.
     * Bean instances are always serialized via JSON conversion.
     *
     * @return {@code false} always, since beans require JSON serialization
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type classification for this bean type.
     *
     * @return {@link SerializationType#ENTITY} indicating this handler manages entity (bean) types
     */
    @Override
    public SerializationType serializationType() {
        return SerializationType.ENTITY;
    }

    /**
     * Serializes a bean instance to its JSON string representation.
     * Uses the internal JSON parser with default serialization configuration.
     *
     * @param x the bean instance to serialize; may be {@code null}
     * @return the JSON string representation of the bean,
     *         or {@code null} if {@code x} is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Deserializes a JSON string into a new bean instance of type {@code T}.
     * Uses the internal JSON parser targeting the reflection type of this handler.
     *
     * @param str the JSON string to deserialize; may be {@code null} or empty
     * @return a new bean instance populated from the JSON data,
     *         or {@code null} if {@code str} is {@code null} or empty
     */
    @Override
    public T valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, Type.of(javaType));
    }
}
