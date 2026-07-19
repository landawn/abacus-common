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
 * using {@link com.landawn.abacus.util.ClassUtil#forName(String)}, which handles primitive
 * type names and array notations.</p>
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
     */
    protected ClazzType(final String typeName) {
        super("Clazz<" + typeName + ">");

        final Type<?> parameterType = TypeFactory.getType(typeName);
        clazz = parameterType.javaType();
        parameterTypes = List.of(parameterType);
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
     * ClazzType type = (ClazzType) TypeFactory.getType("Clazz<java.lang.Integer>");
     * type.parameterClass();   // returns Integer.class
     * }</pre>
     *
     * @return the parameter class
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
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
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
     * primitive type names (e.g., {@code "int"}) and array notations.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the class name to resolve; may be {@code null} or empty
     * @return the resolved {@link Class} object, or {@code null} if {@code str} is {@code null} or empty
     * @throws IllegalArgumentException if the class cannot be found or loaded
     * @see #valueOf(Object)
     * @see #stringOf(Class)
     */
    @Override
    public Class valueOf(final String str) {
        return Strings.isEmpty(str) ? null : ClassUtil.forName(str);
    }
}
