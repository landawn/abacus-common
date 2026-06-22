/*
 * Copyright (C) 2025 HaiYang Li
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSortedMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Google Guava Multiset implementations.
 * This class provides serialization and deserialization capabilities for various Guava Multiset types
 * including HashMultiset, LinkedHashMultiset, and TreeMultiset.
 * Multisets are serialized as {@code Map<E, Integer>} structures where the value represents the count of each element.
 *
 * @param <E> the element type of the multiset
 * @param <T> the multiset type (must extend {@code Multiset<E>})
 */
@SuppressWarnings("java:S2160")
public class GuavaMultisetType<E, T extends Multiset<E>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final List<Type<?>> parameterTypes;

    private final Type<E> elementType;

    private final boolean isOrdered;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for GuavaMultisetType.
     * This constructor is called by the TypeFactory to create Guava Multiset type instances.
     *
     * @param typeClass the concrete or abstract Multiset class to handle
     * @param parameterTypeName the name of the element type parameter
     */
    @SuppressWarnings("unchecked")
    GuavaMultisetType(final Class<T> typeClass, final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        this.typeClass = typeClass;
        declaringName = getTypeName(typeClass, parameterTypeName, true);
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);
        isOrdered = LinkedHashMultiset.class.isAssignableFrom(typeClass) || TreeMultiset.class.isAssignableFrom(typeClass);

        jdc = JsonDeserConfig.create().setMapKeyType(elementType).setMapValueType(Integer.class).setElementType(elementType);
    }

    /**
     * Returns the declaring name of this multiset type, using the canonical class name for the
     * multiset class and each element type's declaring name for the type parameter
     * (e.g., {@code "com.google.common.collect.HashMultiset<String>"} when the element type is
     * {@code String}).
     *
     * @return the declaring name of this type
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Java class represented by this type handler.
     *
     * @return the {@link Class} object for the multiset type
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this multiset.
     *
     * @return the {@link Type} instance representing the element type of this multiset
     */
    @Override
    public Type<E> elementType() {
        return elementType;
    }

    /**
     * Returns an immutable list containing the single parameter type of this generic multiset type
     * (the element type).
     *
     * @return an immutable list containing the element type as the only parameter type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * Multiset types are always generic types as they have an element type parameter.
     *
     * @return {@code true}, as Multiset is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * Guava multisets are serializable through their {@link java.util.Map} representation.
     *
     * @return {@code true}, always, because multisets are serialized as element-to-count maps
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Serializes a multiset to its JSON string representation.
     * The multiset is serialized as a JSON object where each element maps to its count
     * (e.g., {@code {"apple":3,"banana":2}}).
     * For ordered multisets ({@link LinkedHashMultiset}, {@link TreeMultiset}), insertion or sort order is preserved.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the multiset to serialize; may be {@code null}
     * @return the JSON string representation, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null;
        }

        final Map<E, Integer> map = isOrdered ? N.newLinkedHashMap(x.size()) : N.newHashMap(x.size());

        for (final E e : x.elementSet()) {
            map.put(e, x.count(e));
        }

        return Utils.jsonParser.serialize(map, Utils.jsc);
    }

    /**
     * Deserializes a JSON string into a multiset instance.
     * The string must represent a {@code Map<E, Integer>} in JSON format, where each value is the element count.
     * Creates the appropriate multiset implementation based on the type class.
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse; may be {@code null} or empty
     * @return a new multiset instance containing the parsed elements with their counts,
     *         or {@code null} if {@code str} is {@code null} or empty
     * @see #valueOf(Object)
     * @see #stringOf(Multiset)
     */
    @SuppressWarnings("rawtypes")
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        final Map<E, Integer> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        if (map == null) {
            return null;
        }

        final T multiset = newInstance(map.size());

        for (final Map.Entry<E, Integer> entry : map.entrySet()) {
            multiset.add(entry.getKey(), entry.getValue());
        }

        // Immutable targets are abstract, so newInstance() built a mutable HashMultiset: convert
        // (like the GuavaMultimapType sibling) instead of returning the wrong runtime type.
        if (ImmutableSortedMultiset.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableSortedMultiset.copyOf((Multiset) multiset);
        } else if (ImmutableMultiset.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableMultiset.copyOf(multiset);
        }

        return multiset;
    }

    /**
     * Generates a type name string for a multiset type with the specified element type.
     *
     * @param typeClass the multiset class
     * @param parameterTypeName the name of the element type
     * @param isDeclaringName {@code true} to build the declaring name using the element type's
     *        declaring name; {@code false} to build the registered name using the element type's
     *        full name. In both cases the multiset class itself is rendered with its canonical class name.
     * @return the formatted type name
     *         (e.g., {@code "com.google.common.collect.HashMultiset<java.lang.String>"})
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN;
        }
    }

    /**
     * Creates a new instance of the appropriate multiset implementation.
     * Selects the concrete implementation based on the type class:
     * <ul>
     *   <li>{@link HashMultiset} for unordered multisets and abstract types</li>
     *   <li>{@link LinkedHashMultiset} for ordered multisets</li>
     *   <li>{@link TreeMultiset} for sorted multisets</li>
     * </ul>
     * Falls back to reflection-based instantiation for custom implementations.
     *
     * @param size the expected number of distinct elements
     * @return a new multiset instance
     * @throws IllegalArgumentException if no suitable constructor or factory method is found
     */
    protected T newInstance(int size) {
        if (HashMultiset.class.isAssignableFrom(typeClass) || Modifier.isAbstract(typeClass.getModifiers())) {
            return (T) HashMultiset.create(size);
        } else if (LinkedHashMultiset.class.isAssignableFrom(typeClass)) {
            return (T) LinkedHashMultiset.create(size);
        } else if (TreeMultiset.class.isAssignableFrom(typeClass)) {
            return (T) TreeMultiset.create();
        } else {
            Constructor<T> constructor = ClassUtil.getDeclaredConstructor(typeClass);

            if (constructor != null) {
                return ClassUtil.invokeConstructor(constructor);
            } else {
                constructor = ClassUtil.getDeclaredConstructor(typeClass, int.class);

                if (constructor != null) {
                    return ClassUtil.invokeConstructor(constructor, size);
                } else {
                    Method method = ClassUtil.getDeclaredMethod(typeClass, "create");

                    if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                            && typeClass.isAssignableFrom(method.getReturnType())) {
                        return ClassUtil.invokeMethod(method);
                    } else {
                        method = ClassUtil.getDeclaredMethod(typeClass, "create", int.class);

                        if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                                && typeClass.isAssignableFrom(method.getReturnType())) {
                            return ClassUtil.invokeMethod(method, size);
                        }
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported Multiset type: " + typeClass.getName() + ". No constructor or static factory method found.");
        }
    }
}
