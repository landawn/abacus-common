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
import java.lang.reflect.Modifier;
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

    @SuppressWarnings("rawtypes")
    private final Class<? extends Map> mapImplClass;

    private final List<Type<?>> parameterTypes;

    private final Type<?> mapValueType;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for {@code MapType}.
     * This constructor is called by the {@code TypeFactory} to create
     * {@code Map<K, V>} type instances for the given implementation class.
     *
     * @param typeClass the concrete {@code Map} implementation class to handle
     * @param keyTypeName the name of the key type parameter
     * @param valueTypeName the name of the value type parameter
     */
    @SuppressWarnings("rawtypes")
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

        Class<? extends Map> deserializationClass = typeClass;

        if (isSpringMultiValueMap) {
            parameterTypes = List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName));
            mapValueType = TypeFactory.getType("List<" + valueTypeName + ">");

            if (typeClass.isInterface() || Modifier.isAbstract(typeClass.getModifiers())) {
                final Class<? extends Map> linkedMultiValueMapClass = ClassUtil.forName("org.springframework.util.LinkedMultiValueMap");

                if (typeClass.isAssignableFrom(linkedMultiValueMapClass)) {
                    deserializationClass = linkedMultiValueMapClass;
                }
            }
        } else {
            parameterTypes = List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName));
            mapValueType = parameterTypes.get(1);
        }

        mapImplClass = deserializationClass;

        jdc = JsonDeserConfig.create().setMapKeyType(parameterTypes.get(0)).setMapValueType(mapValueType);
    }

    /**
     * Returns the declaring name of this {@code Map} type.
     * The declaring name uses simple (non-fully-qualified) class names; for concrete
     * implementation classes it is rendered using the {@code Map} interface (e.g., a
     * {@code LinkedHashMap} type yields {@code "Map<KeyDeclaringName, ValueDeclaringName>"}).
     * Map interfaces (e.g., {@code SortedMap}) keep their own simple name.
     *
     * @return the declaring name in the format {@code "MapInterface<KeyDeclaringName, ValueDeclaringName>"}
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
     * The list always contains exactly two elements: the declared key type at index 0 and the declared value type at index 1.
     * For Spring's {@code MultiValueMap<K, V>}, the second parameter is {@code V}; its internal
     * {@code Map<K, List<V>>} storage is a deserialization detail, not a declared type argument.
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
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@code Map} object to convert, may be {@code null}
     * @return the map's JSON representation
     *         or {@code null} if the input is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
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
     * The Spring {@code MultiValueMap} interface is materialized as a {@code LinkedMultiValueMap}.
     * Handles the following cases:
     * <ul>
     *   <li>{@code null} or blank string — returns {@code null}</li>
     *   <li>{@code "{}"} — returns an empty {@code Map} of the appropriate implementation type</li>
     *   <li>Any other valid JSON object string — deserializes into a populated {@code Map}</li>
     * </ul>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse, may be {@code null} or blank
     * @return the parsed {@code Map} object, or {@code null} if the input is {@code null} or blank
     * @see #valueOf(Object)
     * @see #stringOf(Map)
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if ("{}".equals(str)) {
            return (T) N.newMap(mapImplClass);
        } else {
            return (T) Utils.jsonParser.deserialize(str, jdc, mapImplClass);
        }
    }

    /**
     * Appends the {@code toString()}-style string representation of a {@link java.util.Map} to an {@link Appendable},
     * in the form {@code {key1:value1, key2:value2}}, with each key and value rendered by its own type's {@code appendTo}.
     * If the map is {@code null}, the literal string {@code "null"} is appended.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target to write to
     * @param x the {@code Map} to append, may be {@code null}
     * @throws IOException if an I/O error occurs while appending
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void appendTo(final Appendable appendable, final T x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            final Type keyType = parameterTypes.get(0);
            final Type valueType = mapValueType;

            appendable.append(SK._BRACE_L);

            int i = 0;
            for (final Map.Entry<K, V> entry : x.entrySet()) {
                if (i++ > 0) {
                    appendable.append(ELEMENT_SEPARATOR);
                }

                if (entry.getKey() == null) {
                    appendable.append(NULL_STRING);
                } else {
                    keyType.appendTo(appendable, entry.getKey());
                }

                appendable.append(SK._COLON);

                if (entry.getValue() == null) {
                    appendable.append(NULL_STRING);
                } else {
                    valueType.appendTo(appendable, entry.getValue());
                }
            }

            appendable.append(SK._BRACE_R);
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
