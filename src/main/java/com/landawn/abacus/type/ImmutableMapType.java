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
import java.util.List;
import java.util.Map;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableNavigableMap;
import com.landawn.abacus.util.ImmutableSortedMap;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for ImmutableMap objects with generic key and value types.
 * This class handles serialization and deserialization of ImmutableMap instances.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <T> the specific ImmutableMap implementation type
 */
@SuppressWarnings("java:S2160")
public class ImmutableMapType<K, V, T extends ImmutableMap<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final List<Type<?>> parameterTypes;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for ImmutableMapType.
     * This constructor is called by the TypeFactory to create ImmutableMap&lt;K,V&gt; type instances.
     *
     * @param keyTypeName the name of the key type parameter
     * @param valueTypeName the name of the value type parameter
     */
    ImmutableMapType(final String keyTypeName, final String valueTypeName) {
        this(ImmutableMap.class, keyTypeName, valueTypeName);
    }

    /**
     * Package-private constructor for ImmutableMapType with an explicit target class.
     * Sorted/navigable subclasses are routed here by the TypeFactory and must produce the declared
     * subtype from {@link #valueOf(String)}.
     *
     * @param typeClass the concrete ImmutableMap (sub)class this handler produces
     * @param keyTypeName the name of the key type parameter
     * @param valueTypeName the name of the value type parameter
     */
    @SuppressWarnings("rawtypes")
    ImmutableMapType(final Class<?> typeClass, final String keyTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));

        declaringName = getTypeName(typeClass, keyTypeName, valueTypeName, true);

        this.typeClass = (Class) typeClass;

        parameterTypes = List.of(TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName));

        jdc = JsonDeserConfig.create().setMapKeyType(parameterTypes.get(0)).setMapValueType(parameterTypes.get(1));
    }

    /**
     * Returns the declaring name of this ImmutableMap type.
     * The declaring name uses simple class names (rather than fully qualified names) for the map class
     * and its key/value type parameters.
     *
     * @return the declaring name in the format "MapClass&lt;KeyDeclaringName, ValueDeclaringName&gt;"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the specific ImmutableMap implementation type.
     *
     * @return the Class object for the ImmutableMap implementation
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the parameter types for this generic ImmutableMap type.
     * The list contains two elements: the key type at index 0 and the value type at index 1.
     *
     * @return an immutable list containing the key type and value type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents an {@link ImmutableMap}.
     *
     * @return {@code true}, always, because this handler is dedicated to {@link ImmutableMap} objects
     */
    @Override
    public boolean isMap() {
        return true;
    }

    /**
     * Indicates whether this is a generic type with type parameters.
     * {@link ImmutableMap} is always parameterized with key and value types.
     *
     * @return {@code true}, always, because {@link ImmutableMap} is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Indicates whether this type uses string-based serialization.
     * Returns {@code false} because ImmutableMap values are serialized structurally as a
     * {@link SerializationType#MAP} rather than as opaque strings.
     *
     * @return {@code false}, indicating that ImmutableMap uses structural map serialization
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Returns the serialization type category for this ImmutableMap type.
     * ImmutableMap values are serialized and deserialized as structured map objects.
     *
     * @return {@link SerializationType#MAP}
     */
    @Override
    public SerializationType serializationType() {
        return SerializationType.MAP;
    }

    /**
     * Serializes an {@link ImmutableMap} to its JSON string representation.
     * An empty map is represented as {@code "{}"}.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the {@link ImmutableMap} to serialize; may be {@code null}
     * @return the JSON string representation of the map, or {@code null} if {@code x} is {@code null}
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
     * Parses a JSON string back into an {@link ImmutableMap} object.
     * <ul>
     *   <li>{@code null} or blank string: returns {@code null}</li>
     *   <li>{@code "{}"}: returns an empty {@link ImmutableMap}</li>
     *   <li>Valid JSON object string: deserialized into an {@link ImmutableMap}</li>
     * </ul>
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse; may be {@code null} or blank
     * @return the parsed {@link ImmutableMap}, or {@code null} if {@code str} is {@code null} or blank
     * @see #valueOf(Object)
     * @see #stringOf(ImmutableMap)
     */
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if ("{}".equals(str)) {
            // Sorted/navigable subclasses route here too (TypeFactory dispatches on assignability).
            if (ImmutableNavigableMap.class.isAssignableFrom(typeClass)) {
                return (T) ImmutableNavigableMap.empty();
            } else if (ImmutableSortedMap.class.isAssignableFrom(typeClass)) {
                return (T) ImmutableSortedMap.empty();
            }

            return (T) ImmutableMap.empty();
        } else {
            final ImmutableMap<K, V> map = Utils.jsonParser.deserialize(str, jdc, ImmutableMap.class);

            if (map == null) {
                return null;
            }

            // Produce the declared subtype instead of a plain ImmutableMap (ClassCastException).
            if (ImmutableNavigableMap.class.isAssignableFrom(typeClass)) {
                return (T) ImmutableNavigableMap.copyOf(map);
            } else if (ImmutableSortedMap.class.isAssignableFrom(typeClass)) {
                return (T) ImmutableSortedMap.copyOf(map);
            }

            return (T) ImmutableMap.wrap(map);
        }
    }

    /**
     * Appends the {@code toString()}-style string representation of an {@link ImmutableMap} to an {@link Appendable},
     * in the form {@code {key1:value1, key2:value2}}, with each key and value rendered by its own type's {@code appendTo}.
     * If {@code x} is {@code null}, the literal {@code null} is appended.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x          the {@link ImmutableMap} to append; may be {@code null}
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
            final Type valueType = parameterTypes.get(1);

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
     * Generates the type name for an {@link ImmutableMap} with the specified implementation class,
     * key type, and value type.
     *
     * @param typeClass       the {@link ImmutableMap} implementation class
     * @param keyTypeName     the name of the key type
     * @param valueTypeName   the name of the value type
     * @param isDeclaringName {@code true} to use declaring names (simple class names),
     *                        {@code false} to use regular names (canonical class names)
     * @return the formatted type name string
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
