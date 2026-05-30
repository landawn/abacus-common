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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Multimap} objects with generic key, element, and collection value types.
 * A {@code Multimap} is a map where each key can be associated with multiple values stored in a {@link Collection}.
 * This class handles serialization and deserialization of {@code Multimap} instances.
 *
 * @param <K> the key type
 * @param <E> the element type (individual values in the collection)
 * @param <V> the value collection type (e.g., List&lt;E&gt; or Set&lt;E&gt;)
 * @param <T> the specific Multimap implementation type
 */
@SuppressWarnings("java:S2160")
public class MultimapType<K, E, V extends Collection<E>, T extends Multimap<K, E, V>> extends AbstractType<T> {

    private final Class<?> typeClass;

    private final String declaringName;

    private final List<Type<?>> parameterTypes;

    protected final JsonDeserConfig jdc;

    /**
     * Package-private constructor for {@code MultimapType}.
     * This constructor is called by the {@code TypeFactory} (and by {@link ListMultimapType}/
     * {@link SetMultimapType}) to create {@code Multimap} type instances.
     *
     * @param typeClass the concrete {@code Multimap} implementation class to handle
     * @param keyTypeName the name of the key type parameter
     * @param valueElementTypeName the name of the element type parameter; may be empty
     *        when the value collection type is specified instead
     * @param valueTypeName the name of the value collection type parameter; may be empty
     *        when the element type is specified instead
     */
    MultimapType(final Class<?> typeClass, final String keyTypeName, final String valueElementTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueElementTypeName, valueTypeName, false));

        parameterTypes = Strings.isEmpty(valueElementTypeName) ? List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName))
                : (Strings.isEmpty(valueTypeName) ? List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueElementTypeName))
                        : List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueElementTypeName), TypeFactory.getType(valueTypeName)));

        this.typeClass = typeClass;

        declaringName = getTypeName(typeClass, keyTypeName, valueElementTypeName, valueTypeName, true);

        jdc = JsonDeserConfig.create().setMapKeyType(parameterTypes.get(0));

        if (Strings.isEmpty(valueElementTypeName)) {
            if (!(parameterTypes.get(1) instanceof CollectionType)) {
                throw new IllegalArgumentException("The value type of Multimap must be a collection type: " + valueTypeName);
            }

            jdc.setMapValueType(parameterTypes.get(1));
            jdc.setElementType(parameterTypes.get(1).elementType());
        } else if (Strings.isEmpty(valueTypeName)) {
            if (ListMultimap.class.isAssignableFrom(typeClass)) {
                jdc.setMapValueType(TypeFactory.getType("List<" + parameterTypes.get(1).name() + ">"));
            } else if (SetMultimap.class.isAssignableFrom(typeClass)) {
                jdc.setMapValueType(TypeFactory.getType("Set<" + parameterTypes.get(1).name() + ">"));
            } else {
                throw new IllegalArgumentException("Unsupported Multimap type: " + typeClass);
            }

            jdc.setElementType(parameterTypes.get(1));
        } else {
            if (!(parameterTypes.get(2) instanceof CollectionType)) {
                throw new IllegalArgumentException("The value type of Multimap must be a collection type: " + valueTypeName);
            }

            jdc.setMapValueType(parameterTypes.get(2));
            jdc.setElementType(parameterTypes.get(1));
        }
    }

    /**
     * Returns the declaring name of this {@code Multimap} type.
     * The declaring name uses simple (non-fully-qualified) class names for the multimap class
     * and its key, element, and value collection type parameters.
     *
     * @return the declaring name, e.g. {@code "ListMultimap<String, Integer>"}
     *         or {@code "Multimap<String, Integer, List<Integer>>"}
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the {@link Class} object representing the specific {@link Multimap} implementation type.
     *
     * @return the class of the concrete {@code Multimap} implementation (e.g., {@code ListMultimap.class})
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public Class<T> javaType() {
        return (Class) typeClass;
    }

    /**
     * Returns the parameter types for this generic {@code Multimap} type.
     * The returned list has either two or three elements depending on how the type was constructed:
     * <ul>
     *   <li>Two elements {@code [keyType, collectionType/elementType]} when only the key type and
     *       either the element type or the collection value type are specified.</li>
     *   <li>Three elements {@code [keyType, elementType, collectionType]} when all three are specified.</li>
     * </ul>
     *
     * @return an immutable list containing the parameter types
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a parameterized type.
     * Always returns {@code true} because {@link Multimap} is parameterized with key, element, and value types.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Indicates whether instances of this type support serialization.
     * {@link Multimap} objects are serialized to and deserialized from JSON through this type handler.
     *
     * @return {@code true}
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Converts a {@link Multimap} object to its JSON string representation.
     * The {@code Multimap} is first converted to a regular {@link java.util.Map} via
     * {@link Multimap#toMap()}, where each key maps to its collection of values,
     * and the resulting map is then serialized as JSON.
     *
     * @param x the {@code Multimap} object to convert, may be {@code null}
     * @return the JSON string representation of the {@code Multimap}
     *         (e.g., {@code {"colors":["red","blue"],"sizes":["large"]}}),
     *         or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     * Parses a JSON string to create a {@link Multimap} object.
     * The JSON must be an object where each key maps to an array of values,
     * e.g. {@code {"colors":["red","blue"],"sizes":["large"]}}.
     * A {@link java.util.Set}-backed multimap is returned when the declared multimap class is a
     * {@link com.landawn.abacus.util.SetMultimap SetMultimap} or its value collection type is a
     * {@link java.util.Set}; otherwise a {@link com.landawn.abacus.util.ListMultimap ListMultimap}-backed
     * multimap is returned. In both cases the result preserves insertion order (a linked implementation).
     *
     * @param str the JSON string to parse; may be {@code null} or blank
     * @return the parsed {@code Multimap} object, or {@code null} if the input is {@code null} or blank
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        final Map<K, Collection<E>> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        if (map == null) {
            return null; // NOSONAR
        }

        // Decide Set- vs List-backed multimap. Prefer the declared multimap class, since
        // parameterTypes.get(1) is the *element* type when the type name omits the value
        // collection (e.g. "Multimap<K, E>" via SetMultimap/ListMultimap) — checking
        // Set.class.isAssignableFrom against the element would always be false.
        final boolean useSet;
        if (SetMultimap.class.isAssignableFrom(typeClass)) {
            useSet = true;
        } else if (ListMultimap.class.isAssignableFrom(typeClass)) {
            useSet = false;
        } else if (parameterTypes.size() > 2) {
            useSet = Set.class.isAssignableFrom(parameterTypes.get(2).javaType());
        } else {
            // size == 2 with valueElementTypeName empty: parameterTypes.get(1) is the value-collection Type
            final Type<?> second = parameterTypes.get(1);
            useSet = second instanceof CollectionType && Set.class.isAssignableFrom(second.javaType());
        }

        if (useSet) {
            final Multimap<K, E, V> multiMap = (Multimap<K, E, V>) N.newLinkedSetMultimap(map.size());

            for (final Map.Entry<K, Collection<E>> entry : map.entrySet()) {
                multiMap.putValues(entry.getKey(), entry.getValue());
            }

            return (T) multiMap;
        } else {
            final Multimap<K, E, V> multimap = (Multimap<K, E, V>) N.newLinkedListMultimap(map.size());

            for (final Map.Entry<K, Collection<E>> entry : map.entrySet()) {
                multimap.putValues(entry.getKey(), entry.getValue());
            }

            return (T) multimap;
        }
    }

    /**
     * Generates the type name for a Multimap with the specified implementation class, key, element and value types.
     * This is an internal method used by the type system.
     *
     * @param typeClass the Multimap implementation class
     * @param keyTypeName the name of the key type
     * @param valueElementTypeName the name of the element type (can be empty)
     * @param valueTypeName the name of the value collection type (can be empty)
     * @param isDeclaringName {@code true} to use declaring (simple) names; {@code false} for canonical names
     * @return the formatted type name string
     */
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueElementTypeName, final String valueTypeName,
            final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName()
                    + (Strings.isEmpty(valueElementTypeName) ? "" : (SK.COMMA_SPACE + TypeFactory.getType(valueElementTypeName).declaringName()))
                    + (Strings.isEmpty(valueTypeName) ? "" : (SK.COMMA_SPACE + TypeFactory.getType(valueTypeName).declaringName())) + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).name()
                    + (Strings.isEmpty(valueElementTypeName) ? "" : (SK.COMMA_SPACE + TypeFactory.getType(valueElementTypeName).name()))
                    + (Strings.isEmpty(valueTypeName) ? "" : (SK.COMMA_SPACE + TypeFactory.getType(valueTypeName).name())) + SK.GREATER_THAN;

        }
    }
}
