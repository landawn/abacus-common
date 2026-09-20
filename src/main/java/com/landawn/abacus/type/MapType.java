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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
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
     * @throws IllegalArgumentException if {@code typeClass} is {@code null}, or a supplied type name is {@code null}, blank, or structurally invalid.
     */
    @SuppressWarnings("rawtypes")
    MapType(final Class<T> typeClass, final String keyTypeName, final String valueTypeName) throws IllegalArgumentException {
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
            // A MultiValueMap<K, V> is a Map<K, List<V>>, so the map's value type -- which is what
            // Type.parameterTypes() describes and what JsonParserImpl reads for map-typed bean
            // properties -- is List<V>, not V.
            parameterTypes = List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType("List<" + valueTypeName + ">"));
            mapValueType = parameterTypes.get(1);

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
     * The list always contains exactly two elements: the declared key type at index 0 and the value type at index 1.
     * For Spring's {@code MultiValueMap<K, V>}, the second parameter is {@code List<V>}, matching
     * the map's internal {@code Map<K, List<V>>} structure.
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
     * Indicates whether instances of this type support direct scalar serialization.
     * {@code Map} objects are converted to/from JSON string form rather than using
     * direct scalar serialization.
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
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * <p>A {@code null} key is written as the quoted string {@code "null"} (JSON object keys must be strings). Reading
     * that text back yields the String key {@code "null"} when the key type is {@code String}, and follows the selected key handler's conversion of {@code "null"} for other key types (which may fail).
     * A null key therefore is not guaranteed to round-trip. An unquoted {@code null} key in the input
     * text (e.g. {@code {null: 1}}) is parsed directly as a null key, if the target map permits it.</p>
     *
     * @param x the {@code Map} object to convert, may be {@code null}
     * @return the map's JSON representation, or {@code null} if {@code x} is {@code null}
     * @throws RuntimeException if a value or bean property cannot be serialized by its selected type handler.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @MayReturnNull
    @Override
    public String stringOf(final T x) throws RuntimeException {
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
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse, may be {@code null} or blank
     * @return the parsed {@code Map} object, or {@code null} if the input is {@code null} or blank
     * @throws ParsingException if {@code str} is not a well-formed JSON object text
     * @throws IllegalArgumentException if the map class cannot be instantiated (an interface without a known implementation, an abstract class, or a
     *         class without an accessible no-arg constructor)
     * @throws NumberFormatException if a key in the text cannot be converted to a numeric declared key type; a JSON array text such as {@code "[]"}
     *         is read as one key, so it fails this way for {@code Map<Integer, String>} and as a {@code ParsingException} for {@code Map<String,
     *         Integer>}
     * @throws RuntimeException if a selected type handler cannot convert a parsed value, or constructing the target value fails.
     * @see #valueOf(Object)
     * @see #stringOf(Map)
     */
    @MayReturnNull
    @Override
    public T valueOf(final String str) throws ParsingException, IllegalArgumentException, NumberFormatException, RuntimeException {
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
     * Each key and each value is appended by its declared handler. When a declared handler is {@code Object} the
     * handler of that entry half's runtime class is used instead, exactly as {@code AbstractTupleType.appendElement} -
     * the slot writer the Pair/Triple/Tuple, {@code Map.Entry} and optional handlers use - resolves a slot:
     * {@code ObjectType} has no {@code appendTo} of its own, so it would otherwise fall back to {@code stringOf}, i.e.
     * the JSON form. A map, collection or bean key or value therefore keeps the {@code toString()}-style form
     * ({@code {k:{a:1}}}, not {@code {k:{"a": 1}}}), matching what the same value appends as when it is not in a map.
     * A {@code null} key or value is appended as the literal {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the target to write to
     * @param x the {@code Map} to append, may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
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
    public void appendTo(final Appendable appendable, final T x) throws NullPointerException, IOException, RuntimeException {
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
                    AbstractTupleType.appendElement(appendable, keyType, entry.getKey());
                }

                appendable.append(SK._COLON);

                if (entry.getValue() == null) {
                    appendable.append(NULL_STRING);
                } else {
                    AbstractTupleType.appendElement(appendable, valueType, entry.getValue());
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
     * @throws IllegalArgumentException if {@code typeClass} is {@code null}, or a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueTypeName, final boolean isDeclaringName)
            throws IllegalArgumentException {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(keyTypeName).name() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + SK.GREATER_THAN;
        }
    }
}
