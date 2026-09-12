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

import java.util.List;

import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for parameterized {@link Class} references (e.g., {@code Clazz<Integer>}).
 * This class provides serialization and deserialization for Java {@link Class} instances.
 *
 * <p>Class objects are serialized using their canonical class name (as returned by
 * {@link com.landawn.abacus.util.ClassUtil#getCanonicalClassName(Class)}) and deserialized
 * using {@link com.landawn.abacus.util.ClassUtil#forName(String)}. Primitive type names (except
 * {@code void}) and array notations are supported; names of hidden classes (lambdas) are emitted by
 * {@code stringOf} but cannot be resolved by {@code valueOf}. A {@link Class} instance handed to
 * {@link #valueOf(Object)} is returned as is.</p>
 *
 * <p>This class uses raw {@link Class} types due to the inherent erasure of generic type
 * parameters at runtime.</p>
 *
 * @see AbstractType
 */
@SuppressWarnings({ "rawtypes", "java:S2160" })
public class ClazzType extends AbstractType<Class> {

    /**
     * The base type name for this type handler, equal to {@code "Clazz"}.
     * The full type name (e.g., {@code "Clazz<java.lang.Integer>"}) is constructed in the constructor.
     */
    public static final String CLAZZ = "Clazz"; //NOSONAR

    /** The parameter class wrapped by this {@code Clazz<T>} type (the resolved type argument). */
    private final Class clazz; //NOSONAR
    private final List<Type<?>> parameterTypes;

    /**
     * Constructs a {@code ClazzType} for the class identified by {@code typeName}.
     * The resulting full type name is {@code "Clazz<" + typeName + ">"}.
     *
     * @param typeName the fully qualified (or canonical) name of the type parameter class,
     *                 e.g., {@code "java.lang.Integer"} or {@code "int"}
     * @throws IllegalArgumentException if {@code typeName} names no loadable class (the unbounded wildcards
     *         {@code "?"} and {@code "? super X"}, which denote {@link Object}, are accepted).
     */
    protected ClazzType(final String typeName) throws IllegalArgumentException {
        super("Clazz<" + typeName + ">");

        final Type<?> parameterType = TypeFactory.getType(typeName);

        // TypeFactory answers a class token it cannot resolve with a fresh ObjectType over Object.class,
        // named after the token itself. Accepting it would silently make Clazz<com.nosuch.Missing> a handler
        // whose parameterClass() is Object.class, so reject it: resolution failure is reported here, as it was
        // before the type argument was resolved through TypeFactory. A name that really denotes Object
        // resolves to the pooled "Object" handler, and the unbounded wildcards map to Object deliberately.
        if (parameterType instanceof ObjectType<?> && parameterType.javaType() == Object.class && !ObjectType.OBJECT.equals(parameterType.name())
                && !isUnboundedWildcard(typeName)) {
            throw new IllegalArgumentException("No class found by name: " + typeName + " for type: " + name());
        }

        clazz = parameterType.javaType();
        parameterTypes = List.of(parameterType);
    }

    private static boolean isUnboundedWildcard(final String typeName) {
        // The two spellings TypeFactory itself maps to Object.class; "? extends X" resolves to its bound.
        return "?".equals(typeName) || typeName.startsWith("? super ");
    }

    /**
     * Returns the Class object representing the Java type handled by this Type, which is
     * always {@link Class}. Use {@link #parameterClass()} to inspect the resolved
     * type-parameter class of this {@code Clazz<T>}.
     *
     * @return {@link Class}{@code .class}
     */
    @Override
    public Class<Class> javaType() {
        return Class.class;
    }

    /**
     * Returns the parameter class wrapped by this {@code Clazz<T>} type
     * (e.g. {@code Integer.class} for {@code Clazz<Integer>}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<?> registered = TypeFactory.getType("Clazz<java.lang.Integer>");
     * ClazzType type = (ClazzType) registered;
     * type.parameterClass();   // returns Integer.class
     * }</pre>
     *
     * <p>This is always the class the type argument actually resolved to: a name that resolves to no class
     * is rejected when the handler is constructed, so {@code Object.class} is returned only for a type
     * argument that genuinely denotes {@link Object} (including the unbounded wildcards {@code "?"} and
     * {@code "? super X"}).</p>
     *
     * @return the parameter class of this {@code Clazz<T>} type; never {@code null}
     */
    public Class parameterClass() {
        return clazz;
    }

    /**
     * Returns the type argument represented by this {@code Clazz<T>} handler.
     *
     * @return an immutable one-element list containing the class's declared type argument
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether instances of this type are immutable.
     * {@link Class} objects are effectively immutable in Java.
     *
     * @return {@code true}, always, because {@link Class} objects are immutable
     */
    @Override
    public boolean isImmutable() {
        return true;
    }

    /**
     * Converts a {@link Class} object to its canonical string name.
     * Uses {@link com.landawn.abacus.util.ClassUtil#getCanonicalClassName(Class)} for serialization.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link Class} to convert; may be {@code null}
     * @return the canonical class name, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final Class x) {
        return x == null ? null : ClassUtil.getCanonicalClassName(x);
    }

    /**
     * Converts a fully qualified (or canonical) class name to the corresponding {@link Class} object.
     * Delegates to {@link com.landawn.abacus.util.ClassUtil#forName(String)}, which supports
     * primitive type names (e.g., {@code "int"}, but not {@code "void"}) and array notations.
     * The name of a hidden class (a lambda) cannot be resolved.
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the class name to resolve; may be {@code null} or empty
     * @return the resolved {@link Class} object, or {@code null} if {@code str} is {@code null} or empty
     * @throws IllegalArgumentException if the class cannot be found or loaded (including {@code "void"} and
     *         hidden-class names).
     * @see #valueOf(Object)
     * @see #stringOf(Class)
     */
    @Override
    public Class valueOf(final String str) throws IllegalArgumentException {
        return Strings.isEmpty(str) ? null : ClassUtil.forName(str);
    }

    /**
     * Converts an arbitrary object to a {@link Class}. A {@link Class} instance is returned as is; any other
     * object is converted through its runtime type's string form and {@link #valueOf(String)}.
     *
     * <p>Without this short-circuit a {@code Class} argument would be stringified by the generic handler
     * registered for {@code Class.class} (as {@code "class java.lang.Integer"}), which no class name resolves.</p>
     *
     * @param obj the object to convert; may be {@code null}
     * @return {@code obj} itself if it is a {@link Class}, the class named by its string form otherwise,
     *         or {@code null} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if the string form names no loadable class.
     */
    @Override
    public Class valueOf(final Object obj) throws IllegalArgumentException {
        return obj instanceof Class<?> cls ? cls : super.valueOf(obj);
    }
}
