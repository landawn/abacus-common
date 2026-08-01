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

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private final List<Type<?>> parameterTypes;
    private final String xmlName;

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
        this.xmlName = TypeFactory.getJavaTypeName(this.javaType).replace("<", "&lt;").replace(">", "&gt;"); //NOSONAR

        if (this.javaType instanceof ParameterizedType parameterizedType) {
            final java.lang.reflect.Type[] arguments = parameterizedType.getActualTypeArguments();
            final List<Type<?>> types = new ArrayList<>(arguments.length);

            for (final java.lang.reflect.Type argument : arguments) {
                types.add(TypeFactory.getType(argument));
            }

            parameterTypes = Collections.unmodifiableList(types);
        } else {
            parameterTypes = EMPTY_TYPE_LIST;
        }
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
     * for generic beans it is the full parameterized type.
     *
     * @return the reflection {@code Type} for the bean type {@code T}
     */
    @Override
    public java.lang.reflect.Type reflectType() {
        return javaType;
    }

    /**
     * Returns the fully qualified XML type name for this bean.
     *
     * <p>Unlike the compact display {@linkplain #name() name}, a serialized type discriminator
     * must distinguish beans that have the same simple class name in different packages.</p>
     *
     * @return the XML-safe, fully qualified bean type name
     */
    @Override
    public String xmlName() {
        return xmlName;
    }

    /**
     * Returns the generic type arguments of this bean when it was registered as a parameterized type
     * (for example {@code MyBean<String, Integer>}); otherwise an empty list.
     *
     * @return an immutable list of parameter types, or an empty list when the bean is not parameterized
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
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
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the bean instance to serialize; may be {@code null}
     * @return the JSON string representation of the bean,
     *         or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Deserializes a JSON string into a new bean instance of type {@code T}.
     * Uses the internal JSON parser targeting the reflection type of this handler.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to deserialize; may be {@code null} or empty
     * @return a new bean instance populated from the JSON data,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @see #valueOf(Object)
     * @see #stringOf(Object)
     */
    @Override
    public T valueOf(final String str) {
        return (Strings.isEmpty(str)) ? null : Utils.jsonParser.deserialize(str, Type.of(javaType));
    }
}
