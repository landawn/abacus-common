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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

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
 * generic parameters. Named intermediate subclasses are supported: type-variable
 * substitutions are resolved while walking the superclass hierarchy.
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
 * <p>Note: The generic type parameter {@code T} must be concrete; it cannot itself be
 * a bare type variable or wildcard, and a raw {@code new TypeReference() {}} without type
 * information will fail. Type arguments nested inside {@code T}, such as the bounded
 * wildcard in {@code List<? extends CharSequence>}, are captured and resolved without issue.
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
     * from the concrete subclass. This constructor walks the superclass hierarchy and
     * resolves type-variable substitutions until it reaches {@code TypeReference}.
     *
     * <p>This constructor must be called from a concrete subclass that specifies
     * the generic type parameter. Direct instantiation of TypeReference is not
     * possible as it is abstract.
     *
     * <p>The constructor performs several validation steps:
     * <ol>
     *   <li>Rejects a raw superclass that loses the required type information</li>
     *   <li>Resolves type variables through any named intermediate subclasses</li>
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
     * @throws IllegalArgumentException if a raw superclass loses the type information, the captured
     *         type still contains an unresolved type variable, or the hierarchy is unsupported
     * @throws IllegalStateException if the type cannot be resolved by TypeFactory
     */
    protected TypeReference() {
        javaType = captureTypeArgument(getClass());

        type = TypeFactory.getType(javaType);

        if (type == null) {
            throw new IllegalStateException("Failed to resolve type from TypeFactory for: " + javaType);
        }
    }

    private static java.lang.reflect.Type captureTypeArgument(final Class<?> concreteClass) {
        final Map<TypeVariable<?>, java.lang.reflect.Type> resolvedVariables = new HashMap<>();
        Class<?> currentClass = concreteClass;

        while (currentClass != Object.class) {
            final java.lang.reflect.Type genericSuperClass = currentClass.getGenericSuperclass();

            if (genericSuperClass instanceof ParameterizedType parameterizedSuperClass) {
                if (!(parameterizedSuperClass.getRawType() instanceof Class<?> rawSuperClass)) {
                    throw new IllegalArgumentException("Unsupported generic superclass for TypeReference: " + parameterizedSuperClass);
                }

                final TypeVariable<?>[] typeParameters = rawSuperClass.getTypeParameters();
                final java.lang.reflect.Type[] typeArguments = parameterizedSuperClass.getActualTypeArguments();

                for (int i = 0; i < typeParameters.length; i++) {
                    resolvedVariables.put(typeParameters[i], resolveType(typeArguments[i], resolvedVariables));
                }

                if (rawSuperClass == TypeReference.class) {
                    final java.lang.reflect.Type result = resolvedVariables.get(TypeReference.class.getTypeParameters()[0]);

                    if (result == null || containsTypeVariable(result)) {
                        throw new IllegalArgumentException("TypeReference constructed without concrete type information: " + result);
                    }

                    return result;
                }

                currentClass = rawSuperClass;
            } else if (genericSuperClass instanceof Class<?> rawSuperClass) {
                if (rawSuperClass == TypeReference.class) {
                    throw new IllegalArgumentException("TypeReference constructed without actual type information");
                }

                currentClass = rawSuperClass;
            } else {
                throw new IllegalArgumentException("Unsupported generic superclass for TypeReference: " + genericSuperClass);
            }
        }

        throw new IllegalArgumentException("TypeReference constructed without actual type information");
    }

    private static boolean containsTypeVariable(final java.lang.reflect.Type typeToCheck) {
        if (typeToCheck instanceof TypeVariable<?>) {
            return true;
        }

        if (typeToCheck instanceof ParameterizedType parameterizedType) {
            if (parameterizedType.getOwnerType() != null && containsTypeVariable(parameterizedType.getOwnerType())) {
                return true;
            }

            for (final java.lang.reflect.Type argument : parameterizedType.getActualTypeArguments()) {
                if (containsTypeVariable(argument)) {
                    return true;
                }
            }
        } else if (typeToCheck instanceof GenericArrayType arrayType) {
            return containsTypeVariable(arrayType.getGenericComponentType());
        } else if (typeToCheck instanceof WildcardType wildcardType) {
            for (final java.lang.reflect.Type bound : wildcardType.getUpperBounds()) {
                if (containsTypeVariable(bound)) {
                    return true;
                }
            }

            for (final java.lang.reflect.Type bound : wildcardType.getLowerBounds()) {
                if (containsTypeVariable(bound)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static java.lang.reflect.Type resolveType(final java.lang.reflect.Type source,
            final Map<TypeVariable<?>, java.lang.reflect.Type> resolvedVariables) {
        if (source instanceof TypeVariable<?> variable) {
            final java.lang.reflect.Type resolved = resolvedVariables.get(variable);
            return resolved == null || resolved == variable ? variable : resolveType(resolved, resolvedVariables);
        }

        if (source instanceof ParameterizedType parameterizedType) {
            final java.lang.reflect.Type owner = parameterizedType.getOwnerType();
            final java.lang.reflect.Type resolvedOwner = owner == null ? null : resolveType(owner, resolvedVariables);
            final java.lang.reflect.Type[] arguments = parameterizedType.getActualTypeArguments();
            final java.lang.reflect.Type[] resolvedArguments = new java.lang.reflect.Type[arguments.length];
            boolean changed = resolvedOwner != owner;

            for (int i = 0; i < arguments.length; i++) {
                resolvedArguments[i] = resolveType(arguments[i], resolvedVariables);
                changed |= resolvedArguments[i] != arguments[i];
            }

            return changed ? new ResolvedParameterizedType(resolvedOwner, parameterizedType.getRawType(), resolvedArguments) : parameterizedType;
        }

        if (source instanceof GenericArrayType arrayType) {
            final java.lang.reflect.Type componentType = arrayType.getGenericComponentType();
            final java.lang.reflect.Type resolvedComponentType = resolveType(componentType, resolvedVariables);

            if (resolvedComponentType == componentType) {
                return arrayType;
            }

            return resolvedComponentType instanceof Class<?> componentClass ? Array.newInstance(componentClass, 0).getClass()
                    : new ResolvedGenericArrayType(resolvedComponentType);
        }

        if (source instanceof WildcardType wildcardType) {
            final java.lang.reflect.Type[] upperBounds = resolveTypes(wildcardType.getUpperBounds(), resolvedVariables);
            final java.lang.reflect.Type[] lowerBounds = resolveTypes(wildcardType.getLowerBounds(), resolvedVariables);

            return Arrays.equals(upperBounds, wildcardType.getUpperBounds()) && Arrays.equals(lowerBounds, wildcardType.getLowerBounds()) ? wildcardType
                    : new ResolvedWildcardType(upperBounds, lowerBounds);
        }

        return source;
    }

    private static java.lang.reflect.Type[] resolveTypes(final java.lang.reflect.Type[] sources,
            final Map<TypeVariable<?>, java.lang.reflect.Type> resolvedVariables) {
        final java.lang.reflect.Type[] result = new java.lang.reflect.Type[sources.length];

        for (int i = 0; i < sources.length; i++) {
            result[i] = resolveType(sources[i], resolvedVariables);
        }

        return result;
    }

    private static final class ResolvedParameterizedType implements ParameterizedType {
        private final java.lang.reflect.Type ownerType;
        private final java.lang.reflect.Type rawType;
        private final java.lang.reflect.Type[] typeArguments;

        ResolvedParameterizedType(final java.lang.reflect.Type ownerType, final java.lang.reflect.Type rawType, final java.lang.reflect.Type[] typeArguments) {
            this.ownerType = ownerType;
            this.rawType = rawType;
            this.typeArguments = typeArguments.clone();
        }

        @Override
        public java.lang.reflect.Type[] getActualTypeArguments() {
            return typeArguments.clone();
        }

        @Override
        public java.lang.reflect.Type getRawType() {
            return rawType;
        }

        @Override
        public java.lang.reflect.Type getOwnerType() {
            return ownerType;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof ParameterizedType other && Objects.equals(ownerType, other.getOwnerType()) && Objects.equals(rawType, other.getRawType())
                    && Arrays.equals(typeArguments, other.getActualTypeArguments());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(typeArguments) ^ Objects.hashCode(ownerType) ^ Objects.hashCode(rawType);
        }

        @Override
        public String getTypeName() {
            final StringBuilder result = new StringBuilder(rawType.getTypeName()).append('<');

            for (int i = 0; i < typeArguments.length; i++) {
                if (i > 0) {
                    result.append(", ");
                }

                result.append(typeArguments[i].getTypeName());
            }

            return result.append('>').toString();
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    private static final class ResolvedGenericArrayType implements GenericArrayType {
        private final java.lang.reflect.Type componentType;

        ResolvedGenericArrayType(final java.lang.reflect.Type componentType) {
            this.componentType = componentType;
        }

        @Override
        public java.lang.reflect.Type getGenericComponentType() {
            return componentType;
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof GenericArrayType other && Objects.equals(componentType, other.getGenericComponentType());
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(componentType);
        }

        @Override
        public String getTypeName() {
            return componentType.getTypeName() + "[]";
        }

        @Override
        public String toString() {
            return getTypeName();
        }
    }

    private static final class ResolvedWildcardType implements WildcardType {
        private final java.lang.reflect.Type[] upperBounds;
        private final java.lang.reflect.Type[] lowerBounds;

        ResolvedWildcardType(final java.lang.reflect.Type[] upperBounds, final java.lang.reflect.Type[] lowerBounds) {
            this.upperBounds = upperBounds.clone();
            this.lowerBounds = lowerBounds.clone();
        }

        @Override
        public java.lang.reflect.Type[] getUpperBounds() {
            return upperBounds.clone();
        }

        @Override
        public java.lang.reflect.Type[] getLowerBounds() {
            return lowerBounds.clone();
        }

        @Override
        public boolean equals(final Object obj) {
            return obj instanceof WildcardType other && Arrays.equals(upperBounds, other.getUpperBounds())
                    && Arrays.equals(lowerBounds, other.getLowerBounds());
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(upperBounds) ^ Arrays.hashCode(lowerBounds);
        }

        @Override
        public String getTypeName() {
            if (lowerBounds.length > 0) {
                return "? super " + lowerBounds[0].getTypeName();
            }

            return upperBounds.length == 0 || upperBounds[0] == Object.class ? "?" : "? extends " + upperBounds[0].getTypeName();
        }

        @Override
        public String toString() {
            return getTypeName();
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
     *   <li>A {@link java.lang.reflect.GenericArrayType} for arrays whose component is a
     *       parameterized type (e.g., {@code List<String>[]})</li>
     * </ul>
     * Wildcards can occur inside a returned parameterized type, such as the type argument in
     * {@code List<? extends Number>}. An unresolved {@link java.lang.reflect.TypeVariable} is
     * rejected by the constructor rather than returned from this method.
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
     * java.lang.reflect.Type type = complexType.javaType();
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
     * <p>The name {@code javaType()} is intentional here. {@code TypeReference} is primarily a
     * bridge to Java's reflection type system, and callers typically use this accessor when they
     * need the original {@link java.lang.reflect.Type} captured from the anonymous subclass. That
     * differs from {@link Type#javaType()}, which returns the raw Java {@link Class} for the
     * resolved Abacus {@link Type}.</p>
     *
     * @return the raw {@link java.lang.reflect.Type} instance representing the generic type T;
     *         never {@code null} (validated during construction).
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
     * String json = N.toJson(data);
     * List<Map<String, Integer>> result = N.fromJson(json, type);
     *
     * // Or access type metadata
     * System.out.println("Type name: " + type.name());
     * System.out.println("Reflect type: " + type.reflectType());
     * }</pre>
     *
     * @return the {@link Type} instance representing the generic type T;
     *         never {@code null} (validated during construction).
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
     * java.lang.reflect.Type rawType = token.javaType();
     *
     * // Use with framework operations
     * List<String> result = N.fromJson(json, type);
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
         * @throws IllegalArgumentException if a raw superclass loses the type information, the captured
         *         type still contains an unresolved type variable, or the hierarchy is unsupported
         * @throws IllegalStateException if the type cannot be resolved by TypeFactory
         * @see TypeReference#TypeReference()
         */
        protected TypeToken() {
            super();
        }
    }
}
