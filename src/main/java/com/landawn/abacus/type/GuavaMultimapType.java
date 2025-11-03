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
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Google Guava Multimap implementations.
 * This class provides serialization and deserialization capabilities for various Guava Multimap types
 * including ListMultimap, SetMultimap, and their concrete implementations.
 * Multimaps are serialized as Map&lt;K, Collection&lt;V&gt;&gt; structures.
 *
 * @param <K> the key type of the multimap
 * @param <V> the value type of the multimap
 * @param <T> the multimap type (must extend Multimap&lt;K, V&gt;)
 */
@SuppressWarnings("java:S2160")
class GuavaMultimapType<K, V, T extends Multimap<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    GuavaMultimapType(final Class<T> typeClass, final String keyTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));

        declaringName = getTypeName(typeClass, keyTypeName, valueTypeName, true);

        this.typeClass = typeClass;

        if (SetMultimap.class.isAssignableFrom(typeClass)) {
            parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType("Set<" + valueTypeName + ">") };
        } else {
            parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType("List<" + valueTypeName + ">") };
        }

        jdc = JDC.create().setMapKeyType(parameterTypes[0]).setMapValueType(parameterTypes[1]).setElementType(parameterTypes[1].getElementType());
    }

    /**
     * Returns the declaring name of this multimap type.
     * The declaring name represents the type in a format suitable for type declarations,
     * using canonical class names with type parameters.
     *
     * @return the declaring name of this type (e.g., "com.google.common.collect.Multimap&lt;String, Integer&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the multimap type handled by this type handler.
     *
     * @return the Class object for the multimap type
     */
    @Override
    public Class<T> clazz() {
        return typeClass;
    }

    /**
     * Returns an array containing the parameter types of this generic multimap type.
     * For multimap types, this includes the key type and the collection value type.
     * SetMultimap types return [KeyType, Set&lt;ValueType&gt;], while other multimaps return [KeyType, List&lt;ValueType&gt;].
     *
     * @return an array containing the key type and collection value type
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Indicates whether instances of this type can be serialized.
     * Guava multimaps are serializable through their Map representation.
     *
     * @return {@code true}, as multimaps can be serialized
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Converts a multimap to its string representation.
     * The multimap is serialized as a JSON object where each key maps to a collection of values.
     * Uses the multimap's asMap() view for serialization.
     *
     * @param x the multimap to convert to string
     * @return the JSON string representation of the multimap, or {@code null} if the input is null
     */
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null;
        }

        final Map<K, Collection<V>> map = x.asMap();

        return Utils.jsonParser.serialize(map, Utils.jsc);
    }

    /**
     * Converts a string representation back to a multimap instance.
     * The string should be in JSON format representing a Map&lt;K, Collection&lt;V&gt;&gt;.
     * Creates the appropriate multimap implementation based on the type class.
     * For immutable types, returns an immutable copy of the constructed multimap.
     *
     * @param str the JSON string to parse
     * @return a new multimap instance containing the parsed data, or {@code null} if the input is {@code null} or empty
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Map<K, Collection<V>> map = Utils.jsonParser.deserialize(str, jdc, Map.class);
        final int avgValueSize = (int) map.values().stream().mapToInt(Collection::size).average().orElse(0);

        final T multimap = newInstance(map.size(), avgValueSize);

        for (final Map.Entry<K, Collection<V>> entry : map.entrySet()) {
            multimap.putAll(entry.getKey(), entry.getValue());
        }

        if (ImmutableListMultimap.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableListMultimap.copyOf(multimap);
        } else if (ImmutableSetMultimap.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableSetMultimap.copyOf(multimap);
        } else if (ImmutableMultimap.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableMultimap.copyOf(multimap);
        }

        return multimap;
    }

    /**
     * Generates a type name string for a multimap type with the specified key and value types.
     * The format depends on whether a declaring name or full name is requested.
     *
     * @param typeClass the multimap class
     * @param keyTypeName the name of the key type
     * @param valueTypeName the name of the value type
     * @param isDeclaringName {@code true} to generate a declaring name, {@code false} for the full name
     * @return the formatted type name (e.g., "Multimap&lt;String, Integer&gt;")
     */
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;
        }
    }

    /**
     * Creates a new instance of the appropriate multimap implementation.
     * Selects the concrete implementation based on the type class:
     * - ArrayListMultimap for ListMultimap types
     * - HashMultimap for SetMultimap types
     * - TreeMultimap for SortedSetMultimap types
     * - LinkedHashMultimap/LinkedListMultimap for ordered types
     * Falls back to reflection-based instantiation for custom implementations.
     *
     * @param keySize the expected number of keys
     * @param avgValueSize the expected average number of values per key
     * @return a new multimap instance
     * @throws IllegalArgumentException if no suitable constructor or factory method is found
     */
    private T newInstance(int keySize, final int avgValueSize) {
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
                    return ClassUtil.invokeConstructor(constructor, keySize);
                } else {
                    Method method = ClassUtil.getDeclaredMethod(typeClass, "create");

                    if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                            && typeClass.isAssignableFrom(method.getReturnType())) {
                        return ClassUtil.invokeMethod(method);
                    } else {
                        method = ClassUtil.getDeclaredMethod(typeClass, "create", int.class, int.class);

                        if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                                && typeClass.isAssignableFrom(method.getReturnType())) {
                            return ClassUtil.invokeMethod(method, keySize);
                        }
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported Multimap type: " + typeClass.getName() + ". No constructor or static factory method found.");
        }
    }
}
