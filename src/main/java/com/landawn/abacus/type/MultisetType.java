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
import java.util.Map;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for Multiset objects with a generic element type.
 * A Multiset is a collection that allows duplicate elements and keeps track of their count.
 * This class handles serialization and deserialization of Multiset instances.
 *
 * @param <E> the element type
 */
@SuppressWarnings("java:S2160")
public class MultisetType<E> extends AbstractType<Multiset<E>> {

    private final String declaringName;

    private static final Class<?> typeClass = Multiset.class;

    private final List<Type<?>> parameterTypes;

    private final Type<E> elementType;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for {@code MultisetType}.
     * This constructor is called by the {@code TypeFactory} to create
     * {@code Multiset<E>} type instances.
     *
     * @param parameterTypeName the name of the element type parameter
     */
    @SuppressWarnings("unchecked")
    MultisetType(final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        declaringName = getTypeName(typeClass, parameterTypeName, true);
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);

        jdc = JsonDeserConfig.create().setMapKeyType(elementType).setMapValueType(Integer.class);
    }

    /**
     * Returns the declaring name of this {@link Multiset} type.
     * The declaring name uses the simple (non-fully-qualified) class names for the
     * multiset class and its element type parameter.
     *
     * @return the declaring name, e.g. {@code "Multiset<String>"}
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the {@link Class} object representing the {@link Multiset} type.
     *
     * @return {@code Multiset.class}
     */
    @Override
    public Class<Multiset<E>> javaType() {
        return (Class<Multiset<E>>) typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this {@link Multiset}.
     *
     * @return the {@code Type} instance representing the element type
     */
    @Override
    public Type<E> elementType() {
        return elementType;
    }

    /**
     * Returns the parameter types for this generic {@link Multiset} type.
     * The list always contains a single element: the element type.
     *
     * @return an immutable single-element list containing the element type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a parameterized type.
     * Always returns {@code true} because {@link Multiset} is parameterized with an element type.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Indicates whether instances of this type support serialization.
     * {@link Multiset} objects are serialized to and deserialized from JSON through this type handler.
     *
     * @return {@code true}
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     * Converts a {@link Multiset} object to its JSON string representation.
     * The {@code Multiset} is first converted to a {@link java.util.Map} via {@link Multiset#toMap()}
     * where each element maps to its occurrence count, and the resulting map is then serialized as JSON.
     *
     * @param x the {@code Multiset} object to convert, may be {@code null}
     * @return the JSON string representation of the {@code Multiset} as an element-to-count map
     *         (e.g., {@code {"apple":3,"orange":2}}), or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final Multiset<E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     * Parses a JSON string to create a {@link Multiset} object.
     * The JSON must be an object where each key is an element and its value is the occurrence count,
     * e.g. {@code {"apple":3,"orange":2,"banana":1}}.
     *
     * @param str the JSON string to parse; may be {@code null} or blank
     * @return the parsed {@code Multiset} object, or {@code null} if the input is {@code null} or blank
     */
    @Override
    public Multiset<E> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        final Map<E, Integer> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        if (map == null) {
            return null; // NOSONAR
        }

        final Multiset<E> multiset = N.newMultiset(map.size());

        for (final Map.Entry<E, Integer> entry : map.entrySet()) {
            multiset.add(entry.getKey(), entry.getValue());
        }

        return multiset;
    }

    /**
     * Generates the type name for a Multiset with the specified element type.
     * This is an internal method used by the type system.
     *
     * @param typeClass the Multiset class
     * @param parameterTypeName the name of the element type
     * @param isDeclaringName {@code true} to use declaring (simple) names; {@code false} for canonical names
     * @return the formatted type name string
     */
    @SuppressWarnings("hiding")
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN;

        }
    }
}
