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

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.Map} objects with generic key and value types.
 * This class handles serialization and deserialization of {@code Map} instances,
 * converting them to and from their JSON object representation.
 *
 * <p>Special support is provided for Spring's {@code MultiValueMap}, where the value type
 * is treated as {@code List<V>} instead of {@code V}.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <T> the specific Map implementation type
 */
@SuppressWarnings("java:S2160")
public class MapType<K, V, T extends Map<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final List<Type<?>> parameterTypes;

    private final JsonDeserConfig jdc;

    MapType(final Class<T> typeClass, final String keyTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));

        declaringName = getTypeName(typeClass.isInterface() ? typeClass : Map.class, keyTypeName, valueTypeName, true);

        this.typeClass = typeClass;

        boolean isSpringMultiValueMap = false;

        try {
            isSpringMultiValueMap = ClassUtil.forName("org.springframework.util.MultiValueMap").isAssignableFrom(typeClass);
        } catch (final Throwable e) {
            // ignore
        }

        if (isSpringMultiValueMap) {
            parameterTypes = List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType("List<" + valueTypeName + ">"));
        } else {
            parameterTypes = List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName));
        }

        jdc = JsonDeserConfig.create().setMapKeyType(parameterTypes.get(0)).setMapValueType(parameterTypes.get(1));
    }

    /**
     * Returns the declaring name of this {@code Map} type.
     * The declaring name uses simple (non-fully-qualified) class names for the map class
     * and its key and value type parameters.
     *
     * @return the declaring name in the format {@code "MapClass<KeyDeclaringName, ValueDeclaringName>"}
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the {@link Class} object representing the specific {@link java.util.Map} implementation type.
     *
     * @return the class of the concrete {@code Map} implementation backing this type (e.g., {@code LinkedHashMap.class})
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the parameter types for this generic {@code Map} type.
     * The list always contains exactly two elements: the key type at index 0 and the value type at index 1.
     * For Spring's {@code MultiValueMap}, the value type is {@code List<V>} rather than {@code V} directly.
     *
     * @return an immutable two-element list containing the key type and the value type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents a {@link java.util.Map}.
     * Always returns {@code true} for {@code MapType}.
     *
     * @return {@code true}
     */
    @Override
    public boolean isMap() {
        return true;
    }

    /**
     * Indicates whether this is a parameterized type.
     * Always returns {@code true} because {@code Map} is parameterized with key and value types.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Indicates whether instances of this type support direct byte-level serialization.
     * {@code Map} objects are converted to/from JSON string form rather than using
     * direct byte-level serialization.
     *
     * @return {@code false}
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for {@code Map}.
     * This value informs the serialization framework how to handle {@code Map} instances.
     *
     * @return {@link SerializationType#MAP}
     */
    @Override
    public SerializationType serializationType() {
        return SerializationType.MAP;
    }

    /**
     * Converts a {@link java.util.Map} object to its JSON string representation.
     * An empty map is represented as {@code "{}"}.
     *
     * @param x the {@code Map} object to convert, may be {@code null}
     * @return the JSON string representation of the map ({@code "{}"} for an empty map),
     *         or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final T x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.isEmpty()) {
            return "{}";
        }

        return Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     * Parses a JSON string to create a {@link java.util.Map} object of the appropriate implementation type.
     * Handles the following cases:
     * <ul>
     *   <li>{@code null} or blank string — returns {@code null}</li>
     *   <li>{@code "{}"} — returns an empty {@code Map} of the appropriate implementation type</li>
     *   <li>Any other valid JSON object string — deserializes into a populated {@code Map}</li>
     * </ul>
     *
     * @param str the JSON string to parse, may be {@code null} or blank
     * @return the parsed {@code Map} object, or {@code null} if the input is {@code null} or blank
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if ("{}".equals(str)) {
            return (T) N.newMap(typeClass);
        } else {
            return Utils.jsonParser.deserialize(str, jdc, typeClass);
        }
    }

    /**
     * Appends the JSON representation of a {@link java.util.Map} to an {@link Appendable}.
     * When the {@code Appendable} is a {@link java.io.Writer}, the JSON serializer writes
     * directly to the {@code Writer} for better performance.
     * If the map is {@code null}, the literal string {@code "null"} is appended.
     *
     * @param appendable the target to write to
     * @param x the {@code Map} to append, may be {@code null}
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            // writer.write(stringOf(x));

            if (appendable instanceof Writer writer) {
                Utils.jsonParser.serialize(x, Utils.jsc, writer);
            } else {
                appendable.append(Utils.jsonParser.serialize(x, Utils.jsc));
            }
        }
    }

    /**
     * Generates the type name for a {@code Map} with the specified implementation class, key, and value types.
     *
     * @param typeClass the Map implementation class
     * @param keyTypeName the name of the key type
     * @param valueTypeName the name of the value type
     * @param isDeclaringName {@code true} to use declaring (simple) names; {@code false} for canonical names
     * @return the formatted type name string, e.g. {@code "Map<String, Integer>"}
     */
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + SK.GREATER_THAN;
        }
    }
}
