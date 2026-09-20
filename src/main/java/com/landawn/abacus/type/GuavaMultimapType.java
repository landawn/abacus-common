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
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Google Guava {@link Multimap} implementations.
 * This class provides serialization and deserialization for Guava multimap types
 * including {@link ListMultimap}, {@link SetMultimap}, and their concrete implementations.
 * Multimaps are serialized as {@code Map<K, Collection<V>>} structures.
 *
 * @param <K> the key type of the multimap
 * @param <V> the value type of the multimap
 * @param <T> the multimap type (must extend {@code Multimap<K, V>})
 * @see Multimap
 */
@SuppressWarnings("java:S2160")
public class GuavaMultimapType<K, V, T extends Multimap<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final List<Type<?>> parameterTypes;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for GuavaMultimapType.
     * This constructor is called by the TypeFactory to create Guava Multimap type instances.
     *
     * @param typeClass the concrete or abstract Multimap class to handle
     * @param keyTypeName the name of the key type parameter
     * @param valueTypeName the name of the value type parameter
     * @throws IllegalArgumentException if {@code typeClass} is {@code null}, or a supplied type name is {@code null}, blank, or structurally invalid.
     */
    GuavaMultimapType(final Class<T> typeClass, final String keyTypeName, final String valueTypeName) throws IllegalArgumentException {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));

        declaringName = getTypeName(typeClass, keyTypeName, valueTypeName, true);

        this.typeClass = typeClass;

        parameterTypes = List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName));

        // Linked/immutable targets can only keep the order they are given: the intermediate map and the
        // intermediate value set must be linked too, otherwise the JSON document order is scrambled before the copy.
        final Type<?> collectionValueType = SetMultimap.class.isAssignableFrom(typeClass) ? TypeFactory.getType("LinkedHashSet<" + valueTypeName + ">")
                : TypeFactory.getType("List<" + valueTypeName + ">");

        jdc = JsonDeserConfig.create()
                .setMapKeyType(parameterTypes.get(0))
                .setMapValueType(collectionValueType)
                .setElementType(parameterTypes.get(1))
                .setMapInstanceType(LinkedHashMap.class);
    }

    /**
     * Returns the declaring name of this multimap type. The multimap class is rendered with its
     * canonical class name, while each type parameter uses its own declaring name
     * (e.g., {@code "com.google.common.collect.Multimap<String, Integer>"}).
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
     * @return the {@link Class} object for the multimap type
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the two declared generic arguments of this Guava multimap type: the key type
     * and the individual value type. The collection used internally for each key is a
     * serialization detail and is not a declared {@code Multimap<K, V>} type argument.
     *
     * @return an immutable list containing {@code [KeyType, ValueType]}
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * Multimap types are always generic types as they have key and value type parameters.
     *
     * @return {@code true}, as Multimap is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * Guava multimaps are serializable through their {@link java.util.Map} representation.
     *
     * <p>Because this type is reported as serializable and does not override {@code serializeTo}, a multimap that is
     * nested in a bean property, a map value or a collection element is written as a quoted JSON <i>string</i> holding
     * the value of {@link #stringOf(Multimap)} (e.g. {@code {"gm": "{\"a\": [1]}"}}), not as a JSON object; in XML the
     * same text is written as the element's escaped character content. The JSON and XML parsers read that form back.
     * Only a multimap serialized as the root value is emitted as a plain JSON object.</p>
     *
     * @return {@code true}, always, because multimaps are serialized via their map view
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Serializes a multimap to its JSON string representation.
     * The multimap is serialized as a JSON object where each key maps to a collection of values,
     * using the multimap's {@code asMap()} view (e.g., {@code {"colors":[1,2],"sizes":[10]}}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the multimap to serialize; may be {@code null}
     * @return the JSON string representation, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @MayReturnNull
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null;
        }

        final Map<K, Collection<V>> map = x.asMap();

        return Utils.jsonParser.serialize(map, Utils.jsc);
    }

    /**
     * Deserializes a JSON string into a multimap instance.
     * The string must represent a {@code Map<K, Collection<V>>} in JSON format.
     * For immutable multimap types, an immutable multimap built from the parsed entries is returned.
     *
     * <p>The keys and values of the JSON document are fed to the target in document order, so a target class that
     * keeps insertion order ({@link LinkedHashMultimap}, {@link LinkedListMultimap}, the immutable multimaps) reflects
     * the document order; for the other targets ({@link Multimap}, {@link ListMultimap}, {@link SetMultimap},
     * {@link ArrayListMultimap}, {@link HashMultimap}, ...) the iteration order is unspecified, and a sorted target
     * ({@link TreeMultimap}, {@link SortedSetMultimap}) is sorted.</p>
     *
     * <p>A key whose JSON value is {@code null} or an empty array (e.g. {@code {"k": null}} or {@code {"k": []}}) is
     * dropped: a multimap never holds a key without values. A duplicate key keeps the position of its first occurrence
     * and the values of its last one.</p>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse; may be {@code null} or empty
     * @return a new multimap instance containing the parsed data, or {@code null} if {@code str} is {@code null} or empty
     * @throws ParsingException if {@code str} is not a well-formed JSON object text
     * @throws NullPointerException if a JSON array contains {@code null} and the target multimap rejects {@code null}
     *         values (the immutable multimaps and {@link TreeMultimap})
     * @see #valueOf(Object)
     * @see #stringOf(Multimap)
     */
    @MayReturnNull
    @Override
    public T valueOf(final String str) throws ParsingException, NullPointerException {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        // jdc targets a LinkedHashMap (see the constructor), so the entries below are in document order.
        final Map<K, Collection<V>> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        if (map == null) {
            return null;
        }

        if (ImmutableMultimap.class.isAssignableFrom(typeClass)) {
            // Build the immutable result straight from the ordered map: the Guava builders keep put order,
            // whereas the ArrayListMultimap/HashMultimap temporary newInstance() would return scrambles it.
            if (ImmutableSetMultimap.class.isAssignableFrom(typeClass)) {
                final ImmutableSetMultimap.Builder<K, V> builder = ImmutableSetMultimap.builder();

                for (final Map.Entry<K, Collection<V>> entry : map.entrySet()) {
                    if (entry.getValue() != null) {
                        builder.putAll(entry.getKey(), entry.getValue());
                    }
                }

                return (T) builder.build();
            } else {
                // ImmutableListMultimap and the abstract ImmutableMultimap (whose copyOf also yields a list multimap).
                final ImmutableListMultimap.Builder<K, V> builder = ImmutableListMultimap.builder();

                for (final Map.Entry<K, Collection<V>> entry : map.entrySet()) {
                    if (entry.getValue() != null) {
                        builder.putAll(entry.getKey(), entry.getValue());
                    }
                }

                return (T) builder.build();
            }
        }

        int keyCount = 0;
        int valueCount = 0;

        for (final Collection<V> values : map.values()) {
            if (values != null) {
                keyCount++;
                valueCount += values.size();
            }
        }

        final T multimap = newInstance(map.size(), keyCount == 0 ? 0 : valueCount / keyCount);

        for (final Map.Entry<K, Collection<V>> entry : map.entrySet()) {
            // A null value ({"k": null}) drops the key, like the abacus ListMultimap/SetMultimap handlers do.
            if (entry.getValue() != null) {
                multimap.putAll(entry.getKey(), entry.getValue());
            }
        }

        return multimap;
    }

    /**
     * Generates a type name string for a multimap type with the specified key and value types.
     *
     * @param typeClass the multimap class
     * @param keyTypeName the name of the key type
     * @param valueTypeName the name of the value type
     * @param isDeclaringName {@code true} to build the declaring name using each type parameter's
     *        declaring name; {@code false} to build the registered name using each type parameter's
     *        full name. In both cases the multimap class itself is rendered with its canonical class name.
     * @return the formatted type name
     *         (e.g., {@code "com.google.common.collect.Multimap<java.lang.String, java.lang.Integer>"})
     * @throws IllegalArgumentException if {@code typeClass} is {@code null}, or a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueTypeName, final boolean isDeclaringName)
            throws IllegalArgumentException {
        if (isDeclaringName) {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + SK.GREATER_THAN;
        }
    }

    /**
     * Creates a new instance of the appropriate multimap implementation.
     * Selects the concrete implementation based on the type class:
     * <ul>
     *   <li>{@link ArrayListMultimap} for {@link ListMultimap} types</li>
     *   <li>{@link HashMultimap} for {@link SetMultimap} types</li>
     *   <li>{@link TreeMultimap} for {@link SortedSetMultimap} types</li>
     *   <li>{@link LinkedHashMultimap}/{@link LinkedListMultimap} for ordered types</li>
     * </ul>
     * Falls back to reflection-based instantiation for custom implementations.
     *
     * @param keySize the expected number of keys
     * @param avgValueSize the expected average number of values per key
     * @return a new multimap instance
     * @throws IllegalArgumentException if no suitable constructor or factory method is found.
     */
    private T newInstance(int keySize, final int avgValueSize) throws IllegalArgumentException {
        if (ArrayListMultimap.class.isAssignableFrom(typeClass) || ImmutableListMultimap.class.isAssignableFrom(typeClass) //
                || Multimap.class.equals(typeClass) || ImmutableMultimap.class.equals(typeClass)
                || (Modifier.isAbstract(typeClass.getModifiers()) && ListMultimap.class.isAssignableFrom(typeClass))) {
            return (T) ArrayListMultimap.create(keySize, avgValueSize);
        } else if (LinkedListMultimap.class.isAssignableFrom(typeClass)) {
            return (T) LinkedListMultimap.create(keySize);
        } else if (TreeMultimap.class.isAssignableFrom(typeClass)
                || (Modifier.isAbstract(typeClass.getModifiers()) && SortedSetMultimap.class.isAssignableFrom(typeClass))) {
            return (T) TreeMultimap.create();
        } else if (HashMultimap.class.isAssignableFrom(typeClass) || ImmutableSetMultimap.class.isAssignableFrom(typeClass)
                || (Modifier.isAbstract(typeClass.getModifiers()) && SetMultimap.class.isAssignableFrom(typeClass))) {
            return (T) HashMultimap.create(keySize, avgValueSize);
        } else if (LinkedHashMultimap.class.isAssignableFrom(typeClass)) {
            return (T) LinkedHashMultimap.create(keySize, avgValueSize);
        } else {
            Constructor<T> constructor = ClassUtil.getDeclaredConstructor(typeClass);

            if (constructor != null) {
                return ClassUtil.invokeConstructor(constructor);
            } else {
                constructor = ClassUtil.getDeclaredConstructor(typeClass, int.class, int.class);

                if (constructor != null) {
                    return ClassUtil.invokeConstructor(constructor, keySize, avgValueSize);
                } else {
                    Method method = ClassUtil.getDeclaredMethod(typeClass, "create");

                    if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                            && typeClass.isAssignableFrom(method.getReturnType())) {
                        return ClassUtil.invokeMethod(method);
                    } else {
                        method = ClassUtil.getDeclaredMethod(typeClass, "create", int.class, int.class);

                        if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                                && typeClass.isAssignableFrom(method.getReturnType())) {
                            return ClassUtil.invokeMethod(method, keySize, avgValueSize);
                        }
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported Multimap type: " + typeClass.getName() + ". No constructor or static factory method found.");
        }
    }
}
