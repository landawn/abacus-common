/*
 * Copyright (C) 2017 HaiYang Li
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

package com.landawn.abacus.util;

import java.lang.reflect.ParameterizedType;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

/**
 * A utility class for capturing and representing generic type information at runtime.
 * This abstract class provides a workaround for Java's type erasure by requiring
 * subclasses to specify their generic type parameter, which can then be retrieved
 * using reflection.
 * 
 * <p>Due to Java's type erasure, generic type information is normally lost at runtime.
 * This class captures that information by analyzing the generic superclass of concrete
 * subclasses, allowing frameworks to access complete type information including
 * generic parameters.
 * 
 * <p><b>To use this class, create an anonymous subclass with the desired generic type:</b></p>
 * <pre>{@code
 * // Capture type information for List<String>
 * TypeReference<List<String>> listType = new TypeReference<List<String>>() {};
 * 
 * // Capture type information for Map<String, Integer>
 * TypeReference<Map<String, Integer>> mapType = new TypeReference<Map<String, Integer>>() {};
 * 
 * // Use with JSON parsing or other frameworks
 * List<String> list = jsonParser.parse(json, listType);
 * }</pre>
 * 
 * <p>Note: This approach cannot capture types with wildcard parameters such as
 * {@code Class<?>} or {@code List<? extends CharSequence>}. Attempting to do so
 * will result in a runtime exception.
 * 
 * <p>Common use cases include:
 * <ul>
 *   <li>JSON/XML deserialization with generic types</li>
 *   <li>Dependency injection frameworks</li>
 *   <li>ORM frameworks for mapping generic collections</li>
 *   <li>Any framework that needs runtime access to generic type information</li>
 * </ul>
 *
 * @param <T> the type to be captured and represented
 * @see Type
 * @see TypeFactory
 */
@SuppressWarnings({ "java:S1694" })
public abstract class TypeReference<T> {

    /**
     * The raw java.lang.reflect.Type instance representing the captured generic type information.
     * This field is initialized in the constructor by analyzing the generic
     * superclass of the concrete subclass.
     */
    protected final java.lang.reflect.Type javaType;

    /**
     * The Type instance representing the captured generic type information.
     * This field is initialized in the constructor by analyzing the generic
     * superclass of the concrete subclass.
     */
    protected final Type<T> type;

    /**
     * Constructs a new TypeReference by capturing the generic type parameter
     * from the concrete subclass. This constructor uses reflection to extract
     * the actual type argument from the parameterized superclass.
     *
     * <p>This constructor must be called from a concrete subclass that specifies
     * the generic type parameter. Direct instantiation of TypeReference is not
     * possible as it is abstract.
     *
     * <p>The constructor performs several validation steps:
     * <ol>
     *   <li>Verifies the superclass is a ParameterizedType (not a raw type)</li>
     *   <li>Ensures at least one type argument is present</li>
     *   <li>Resolves the type using TypeFactory</li>
     *   <li>Validates the resolved Type is not null</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // This creates an anonymous subclass that captures List<String>
     * TypeReference<List<String>> ref = new TypeReference<List<String>>() {};
     * }</pre>
     *
     * @throws IllegalArgumentException if the TypeReference is constructed without actual type information,
     *         or if the parameterized type has no type arguments
     * @throws IllegalStateException if the type cannot be resolved by TypeFactory
     */
    protected TypeReference() {
        java.lang.reflect.Type superClass = getClass().getGenericSuperclass();

        if (superClass instanceof Class<?>) {
            throw new IllegalArgumentException("TypeReference constructed without actual type information");
        }

        java.lang.reflect.Type[] typeArguments = ((ParameterizedType) superClass).getActualTypeArguments();

        if (typeArguments.length == 0) {
            throw new IllegalArgumentException("TypeReference constructed without type arguments");
        }

        javaType = typeArguments[0];

        type = TypeFactory.getType(javaType);

        if (type == null) {
            throw new IllegalStateException("Failed to resolve type from TypeFactory for: " + javaType);
        }
    }

    /**
     * Returns the raw {@link java.lang.reflect.Type} instance representing the captured generic type.
     *
     * <p>This method provides access to the underlying {@code java.lang.reflect.Type} object
     * that contains complete generic type information, including nested type parameters,
     * wildcards, and type bounds. The returned Type is extracted during construction via
     * reflection from the parameterized superclass.</p>
     *
     * <p>The returned Type can be:</p>
     * <ul>
     *   <li>A {@link Class} object for simple types (e.g., {@code String.class})</li>
     *   <li>A {@link ParameterizedType} for generic types (e.g., {@code List<String>})</li>
     *   <li>A {@link java.lang.reflect.GenericArrayType} for generic arrays (e.g., {@code T[]})</li>
     *   <li>A {@link java.lang.reflect.TypeVariable} for type variables (e.g., {@code T})</li>
     *   <li>A {@link java.lang.reflect.WildcardType} for wildcard types (e.g., {@code ? extends Number})</li>
     * </ul>
     *
     * <p>This raw Type representation is particularly useful when working with reflection-based
     * frameworks, serialization libraries, or any code that needs to introspect generic types
     * at runtime.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Capture complex nested generic type
     * TypeReference<List<Map<String, Integer>>> complexType =
     *     new TypeReference<List<Map<String, Integer>>>() {};
     *
     * java.lang.reflect.Type type = complexType.getType();
     *
     * // Use with serialization framework
     * Object result = deserializer.deserialize(data, type);
     *
     * // Or inspect the type structure
     * if (type instanceof ParameterizedType) {
     *     ParameterizedType pt = (ParameterizedType) type;
     *     System.out.println("Raw type: " + pt.getRawType());
     *     System.out.println("Type arguments: " + Arrays.toString(pt.getActualTypeArguments()));
     * }
     * }</pre>
     *
     * @return the raw {@link java.lang.reflect.Type} instance representing the generic type T;
     *         never {@code null} (validated during construction)
     * @see #type()
     * @see ParameterizedType
     */
    public java.lang.reflect.Type javaType() {
        return javaType;
    }

    /**
     * Returns the {@link Type} instance representing the captured generic type.
     *
     * <p>This method provides access to the framework-specific {@link Type} wrapper that
     * encapsulates the generic type information. Unlike {@link #javaType()}, which returns
     * the raw {@code java.lang.reflect.Type}, this method returns a {@code Type<T>} instance
     * from the abacus type system, which provides additional type manipulation and
     * introspection capabilities.</p>
     *
     * <p>The {@link Type} interface is part of the abacus type system and offers rich
     * functionality for:</p>
     * <ul>
     *   <li>Type conversion and casting</li>
     *   <li>Serialization and deserialization</li>
     *   <li>Type introspection and validation</li>
     *   <li>Working with generic collections and maps</li>
     *   <li>Database mapping and ORM operations</li>
     * </ul>
     *
     * <p>The returned Type is resolved using {@link TypeFactory#getType(java.lang.reflect.Type)}
     * during construction and is guaranteed to be {@code non-null} (validated in the constructor).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Capture complex nested generic type
     * TypeReference<List<Map<String, Integer>>> complexType =
     *     new TypeReference<List<Map<String, Integer>>>() {};
     *
     * Type<List<Map<String, Integer>>> type = complexType.type();
     *
     * // Use with abacus framework operations
     * String json = N.toJSON(data, type);
     * List<Map<String, Integer>> result = N.fromJSON(json, type);
     *
     * // Or access type metadata
     * System.out.println("Type name: " + type.name());
     * System.out.println("Type class: " + type.clazz());
     * }</pre>
     *
     * @return the {@link Type} instance representing the generic type T;
     *         never {@code null} (validated during construction)
     * @see #javaType()
     * @see Type
     * @see TypeFactory
     */
    public Type<T> type() {
        return type;
    }

    /**
     * An alternative abstract base class for capturing type information at runtime.
     *
     * <p>This class serves the exact same purpose as {@link TypeReference} but uses the
     * "TypeToken" naming convention, which may be more familiar to developers coming from
     * other popular Java frameworks and libraries such as Google Guava, Gson, or Jackson.</p>
     *
     * <p>{@code TypeToken} is functionally identical to {@code TypeReference}. It extends
     * {@code TypeReference} without adding any additional functionality, serving purely as
     * an alias to provide naming consistency with other frameworks. The choice between
     * using {@code TypeToken} or {@code TypeReference} is entirely stylistic and based on
     * developer preference or team conventions.</p>
     *
     * <p>Like its parent class, {@code TypeToken} works by capturing generic type information
     * through anonymous subclassing. When you create an instance with {@code new TypeToken<T>() {}},
     * Java's reflection API can inspect the parameterized superclass to retrieve the actual
     * type argument {@code T}, effectively working around Java's type erasure.</p>
     *
     * <p><strong>Use this class when:</strong></p>
     * <ul>
     *   <li>Your team is more familiar with the "TypeToken" naming from other frameworks</li>
     *   <li>You're migrating code from Guava or Gson and want consistent naming</li>
     *   <li>You prefer the "Token" terminology for type representations</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Using TypeToken instead of TypeReference
     * TypeToken<List<String>> token = new TypeToken<List<String>>() {};
     *
     * // Access the Type instance
     * Type<List<String>> type = token.type();
     *
     * // Or access the raw java.lang.reflect.Type
     * java.lang.reflect.Type rawType = token.getType();
     *
     * // Use with framework operations
     * List<String> result = N.fromJSON(json, type);
     * }</pre>
     *
     * <p><strong>Comparison with other frameworks:</strong></p>
     * <ul>
     *   <li><strong>Guava:</strong> {@code com.google.common.reflect.TypeToken}</li>
     *   <li><strong>Gson:</strong> {@code com.google.gson.reflect.TypeToken}</li>
     *   <li><strong>Jackson:</strong> {@code com.fasterxml.jackson.core.type.TypeReference}</li>
     *   <li><strong>Abacus:</strong> {@code com.landawn.abacus.util.TypeReference.TypeToken}</li>
     * </ul>
     *
     * @param <T> the type to be captured and represented
     * @see TypeReference
     * @see Type
     */
    public abstract static class TypeToken<T> extends TypeReference<T> {
        /**
         * Constructs a new TypeToken by capturing the generic type parameter
         * from the concrete subclass. This constructor delegates to the parent
         * {@link TypeReference} constructor to perform type capture and validation.
         *
         * <p>This constructor must be called from a concrete subclass that specifies
         * the generic type parameter. Direct instantiation of TypeToken is not
         * possible as it is abstract.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // This creates an anonymous subclass that captures List<String>
         * TypeToken<List<String>> token = new TypeToken<List<String>>() {};
         * }</pre>
         *
         * @throws IllegalArgumentException if the TypeToken is constructed without actual type information,
         *         or if the parameterized type has no type arguments
         * @throws IllegalStateException if the type cannot be resolved by TypeFactory
         * @see TypeReference#TypeReference()
         */
        protected TypeToken() {
            super();
        }
    }
}
