/*
 * Copyright (C) 2026 HaiYang Li
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

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.util.ClassUtil;

/** Resolves the value and creator metadata of a single-value wrapper before numeric inference. */
final class ValueTypeResolver {
    private final Map<TypeVariable<?>, java.lang.reflect.Type> bindings = new HashMap<>();

    /**
     * @throws IllegalArgumentException if a wildcard type argument contradicts its declared type-variable bound
     */
    ValueTypeResolver(final Class<?> wrapper, final List<Type<?>> arguments) throws IllegalArgumentException {
        final TypeVariable<?>[] variables = wrapper.getTypeParameters();
        for (int i = 0; i < Math.min(variables.length, arguments.size()); i++) {
            bindings.put(variables[i], reflectionType(arguments.get(i)));
        }
        for (Map.Entry<TypeVariable<?>, java.lang.reflect.Type> entry : bindings.entrySet()) {
            if (entry.getValue() instanceof WildcardType wildcard && raw(wildcard.getUpperBounds()[0]).isAssignableFrom(raw(entry.getKey().getBounds()[0]))) {
                // A wildcard's effective upper bound also includes the declaration bound, even for
                // Box<? extends Number> when Box declares T extends BigDecimal.
                if (!accepts(wildcard.getUpperBounds()[0], entry.getKey().getBounds()[0], new HashSet<>())) {
                    throw new IllegalArgumentException("Wildcard argument contradicts bound of " + entry.getKey());
                }
                final java.lang.reflect.Type[] lower = wildcard.getLowerBounds();
                entry.setValue(new Wildcard(entry.getKey().getBounds()[0], lower.length == 0 ? null : lower[0]));
            }
        }
    }

    private static java.lang.reflect.Type reflectionType(final Type<?> type) {
        // Most handlers' reflectType() returns only their raw class; reconstruct nested metadata.
        final String name = type.name();
        if (name.startsWith("? extends ")) {
            return new Wildcard(reflectionType(TypeFactory.getType(name.substring(10))), null);
        } else if (name.startsWith("? super ")) {
            return new Wildcard(Object.class, reflectionType(TypeFactory.getType(name.substring(8))));
        } else if (name.equals("?")) {
            return new Wildcard(Object.class, null);
        }
        if (type.isArray()) {
            final java.lang.reflect.Type component = reflectionType(type.elementType());
            return component instanceof Class<?> cls ? Array.newInstance(cls, 0).getClass() : new GenericArray(component);
        }
        final List<Type<?>> parameters = type.parameterTypes();
        if (parameters.isEmpty()) {
            return type.javaType();
        }
        return new Parameterized(type.javaType(), parameters.stream().map(ValueTypeResolver::reflectionType).toArray(java.lang.reflect.Type[]::new));
    }

    Type<Object> valueType(final java.lang.reflect.Type member) {
        return TypeFactory.getType(parsingName(member, new HashSet<>()));
    }

    private String parsingName(final java.lang.reflect.Type type, final Set<TypeVariable<?>> visiting) {
        if (type instanceof TypeVariable<?> variable) {
            if (!visiting.add(variable)) {
                return Object.class.getName();
            }
            try {
                return parsingName(bindings.getOrDefault(variable, variable.getBounds()[0]), visiting);
            } finally {
                visiting.remove(variable);
            }
        } else if (type instanceof WildcardType wildcard) {
            // A lower bound does not constrain the runtime value; only the upper bound is safe to parse.
            return parsingName(wildcard.getUpperBounds()[0], visiting);
        } else if (type instanceof GenericArrayType array) {
            return parsingName(array.getGenericComponentType(), visiting) + "[]";
        } else if (type instanceof ParameterizedType parameterized) {
            final StringBuilder result = new StringBuilder(parameterized.getRawType().getTypeName()).append('<');
            for (java.lang.reflect.Type argument : parameterized.getActualTypeArguments()) {
                if (result.charAt(result.length() - 1) != '<') {
                    result.append(", ");
                }
                result.append(parsingName(argument, visiting));
            }
            return result.append('>').toString();
        }
        return type.getTypeName();
    }

    /**
     * @throws IllegalArgumentException if an inferred type argument violates a declared bound, the creator parameter cannot accept the value type, or its return type contradicts the wrapper arguments
     */
    void checkCreator(final java.lang.reflect.Type value, final java.lang.reflect.Type parameter, final Method factory) throws IllegalArgumentException {
        if (factory != null) {
            // Static factory variables are distinct from class variables, even if they share a name.
            // Infer them from Box<U> (or Box<List<U>>) against this handler's Box<...> arguments.
            final java.lang.reflect.Type returned = factory.getGenericReturnType();
            final Class<?> wrapper = factory.getDeclaringClass();
            if (returned instanceof ParameterizedType pt && pt.getRawType() == wrapper) {
                final TypeVariable<?>[] variables = wrapper.getTypeParameters();
                final java.lang.reflect.Type[] actual = pt.getActualTypeArguments();
                for (int i = 0; i < variables.length; i++) {
                    if (bindings.containsKey(variables[i])) {
                        infer(actual[i], bindings.get(variables[i]), factory);
                    }
                }
            }
            infer(parameter, value, factory);
        }
        // Inference must not erase a factory's declared bounds (for example U extends Number).
        for (Map.Entry<TypeVariable<?>, java.lang.reflect.Type> entry : bindings.entrySet()) {
            for (java.lang.reflect.Type bound : entry.getKey().getBounds()) {
                final java.lang.reflect.Type argument = dereference(entry.getValue());
                boolean valid = accepts(bound, argument instanceof WildcardType wildcard ? wildcard.getUpperBounds()[0] : argument, new HashSet<>());
                if (argument instanceof WildcardType wildcard) {
                    for (java.lang.reflect.Type lower : wildcard.getLowerBounds()) {
                        valid &= accepts(bound, lower, new HashSet<>());
                    }
                }
                if (!valid) {
                    throw new IllegalArgumentException("Type argument violates bound of " + entry.getKey() + ": " + entry.getValue().getTypeName());
                }
            }
        }
        if (!accepts(parameter, value, new HashSet<>())) {
            throw new IllegalArgumentException("Creator parameter " + parameter.getTypeName() + " is incompatible with value type " + value.getTypeName());
        }
        if (factory != null && factory.getGenericReturnType() instanceof ParameterizedType returned && returned.getRawType() == factory.getDeclaringClass()) {
            final TypeVariable<?>[] variables = factory.getDeclaringClass().getTypeParameters();
            final java.lang.reflect.Type[] actual = returned.getActualTypeArguments();
            for (int i = 0; i < variables.length; i++) {
                if (bindings.containsKey(variables[i]) && !acceptsArgument(bindings.get(variables[i]), actual[i], new HashSet<>())) {
                    throw new IllegalArgumentException(
                            "Creator return type " + returned.getTypeName() + " is incompatible with wrapper argument " + variables[i]);
                }
            }
        }
    }

    private void infer(final java.lang.reflect.Type pattern, final java.lang.reflect.Type value, final Method factory) {
        final java.lang.reflect.Type resolved = dereference(value);
        if (pattern instanceof TypeVariable<?> variable && variable.getGenericDeclaration().equals(factory)) {
            if (resolved != variable) {
                bindings.putIfAbsent(variable, resolved);
            }
        } else if (pattern instanceof ParameterizedType a && resolved instanceof ParameterizedType b && a.getRawType().equals(b.getRawType())) {
            final java.lang.reflect.Type[] left = a.getActualTypeArguments();
            final java.lang.reflect.Type[] right = b.getActualTypeArguments();
            for (int i = 0; i < left.length; i++) {
                infer(left[i], right[i], factory);
            }
        } else if (pattern instanceof GenericArrayType array) {
            if (resolved instanceof GenericArrayType other) {
                infer(array.getGenericComponentType(), other.getGenericComponentType(), factory);
            } else if (resolved instanceof Class<?> cls && cls.isArray()) {
                infer(array.getGenericComponentType(), cls.getComponentType(), factory);
            }
        }
    }

    private java.lang.reflect.Type dereference(java.lang.reflect.Type type) {
        final Set<TypeVariable<?>> seen = new HashSet<>();
        while (type instanceof TypeVariable<?> variable && seen.add(variable) && bindings.containsKey(variable)) {
            type = bindings.get(variable);
        }
        return type;
    }

    private boolean accepts(java.lang.reflect.Type target, java.lang.reflect.Type source, final Set<TypeVariable<?>> visiting) {
        target = dereference(target);
        source = dereference(source);
        if (target.equals(source)) {
            return true;
        }
        if (target instanceof WildcardType wildcard) {
            for (java.lang.reflect.Type upper : wildcard.getUpperBounds()) {
                if (!accepts(upper, source, visiting)) {
                    return false;
                }
            }
            for (java.lang.reflect.Type lower : wildcard.getLowerBounds()) {
                if (!accepts(source, lower, visiting)) {
                    return false;
                }
            }
            return true;
        }
        if (target instanceof TypeVariable<?> variable) {
            if (!visiting.add(variable)) {
                return true;
            }
            for (java.lang.reflect.Type bound : variable.getBounds()) {
                if (!accepts(bound, source, visiting)) {
                    return false;
                }
            }
            return true;
        }
        if (!ClassUtil.wrap(raw(target)).isAssignableFrom(ClassUtil.wrap(raw(source)))) {
            return false;
        }
        if (raw(target).isArray() && raw(source).isArray()) {
            final java.lang.reflect.Type targetComponent = target instanceof GenericArrayType array ? array.getGenericComponentType()
                    : raw(target).getComponentType();
            final java.lang.reflect.Type sourceComponent = source instanceof GenericArrayType array ? array.getGenericComponentType()
                    : raw(source).getComponentType();
            return accepts(targetComponent, sourceComponent, visiting);
        }
        // Compare invariant arguments where both sides declare the same generic class. Raw and broader
        // creator types remain legal; rejecting unprovable mismatches would reject safe existing factories.
        if (target instanceof ParameterizedType a && source instanceof ParameterizedType b && a.getRawType().equals(b.getRawType())) {
            final java.lang.reflect.Type[] left = a.getActualTypeArguments();
            final java.lang.reflect.Type[] right = b.getActualTypeArguments();
            for (int i = 0; i < left.length; i++) {
                if (!acceptsArgument(left[i], right[i], visiting)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean acceptsArgument(java.lang.reflect.Type target, java.lang.reflect.Type source, final Set<TypeVariable<?>> visiting) {
        target = dereference(target);
        source = dereference(source);
        if (target instanceof WildcardType || target instanceof TypeVariable<?>) {
            return accepts(target, source, visiting);
        }
        if (source instanceof WildcardType || source instanceof TypeVariable<?>) {
            return true; // No concrete contradiction can be established from an unresolved source.
        }
        return raw(target).equals(raw(source)) && accepts(target, source, visiting);
    }

    private Class<?> raw(final java.lang.reflect.Type type) {
        final java.lang.reflect.Type resolved = dereference(type);
        if (resolved instanceof Class<?> cls) {
            return cls;
        } else if (resolved instanceof ParameterizedType pt) {
            return (Class<?>) pt.getRawType();
        } else if (resolved instanceof GenericArrayType array) {
            return Array.newInstance(raw(array.getGenericComponentType()), 0).getClass();
        } else if (resolved instanceof WildcardType wildcard) {
            return raw(wildcard.getUpperBounds()[0]);
        } else if (resolved instanceof TypeVariable<?> variable) {
            return raw(variable.getBounds()[0]);
        }
        return Object.class;
    }

    private record Parameterized(Class<?> raw, java.lang.reflect.Type[] arguments) implements ParameterizedType {
        @Override
        public java.lang.reflect.Type[] getActualTypeArguments() {
            return arguments.clone();
        }

        @Override
        public java.lang.reflect.Type getRawType() {
            return raw;
        }

        @Override
        public java.lang.reflect.Type getOwnerType() {
            return raw.getDeclaringClass();
        }
    }

    private record GenericArray(java.lang.reflect.Type component) implements GenericArrayType {
        @Override
        public java.lang.reflect.Type getGenericComponentType() {
            return component;
        }
    }

    private record Wildcard(java.lang.reflect.Type upper, java.lang.reflect.Type lower) implements WildcardType {
        @Override
        public java.lang.reflect.Type[] getUpperBounds() {
            return new java.lang.reflect.Type[] { upper };
        }

        @Override
        public java.lang.reflect.Type[] getLowerBounds() {
            return lower == null ? new java.lang.reflect.Type[0] : new java.lang.reflect.Type[] { lower };
        }
    }
}
