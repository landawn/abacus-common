/*
 * Copyright (C) 2017 HaiYang Li
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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Clazz;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link java.util.Map.Entry} objects with generic key and value types.
 * This class handles serialization and deserialization of {@code Map.Entry} instances,
 * converting them to and from a single-entry JSON object representation (e.g., {@code {"key":value}}).
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings("java:S2160")
public class MapEntryType<K, V> extends AbstractType<Map.Entry<K, V>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<Map.Entry<K, V>> typeClass = (Class) Map.Entry.class; //NOSONAR

    private final Type<K> keyType;

    private final Type<V> valueType;

    private final List<Type<?>> parameterTypes;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for {@code MapEntryType}.
     * This constructor is called by the {@code TypeFactory} to create
     * {@code Map.Entry<K, V>} type instances.
     *
     * @param keyTypeName the name of the key type parameter
     * @param valueTypeName the name of the value type parameter
     */
    MapEntryType(final String keyTypeName, final String valueTypeName) {
        super(getTypeName(keyTypeName, valueTypeName, false));

        declaringName = getTypeName(keyTypeName, valueTypeName, true);
        keyType = TypeFactory.getType(keyTypeName);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = List.of(keyType, valueType);
        jdc = JsonDeserConfig.create().setMapKeyType(keyType).setMapValueType(valueType);
    }

    /**
     * Returns the declaring name of this {@code MapEntry} type.
     * The declaring name uses simple (non-fully-qualified) class names for the key and value
     * type parameters; the wrapper is rendered as {@code "Map.Entry"}.
     *
     * @return the declaring name in the format {@code "Map.Entry<KeyDeclaringName, ValueDeclaringName>"}
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the {@link Class} object representing the {@code Map.Entry} type.
     *
     * @return {@code Map.Entry.class}
     */
    @Override
    public Class<Map.Entry<K, V>> javaType() {
        return typeClass;
    }

    /**
     * Returns the parameter types for this generic {@code Map.Entry} type.
     * The list always contains exactly two elements: the key type at index 0
     * and the value type at index 1.
     *
     * @return an immutable two-element list containing the key type and the value type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a parameterized type.
     * Always returns {@code true} because {@code Map.Entry} is parameterized with key and value types.
     *
     * @return {@code true}
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Converts a {@link Map.Entry} object to its JSON string representation.
     * The entry is serialized as a single-pair JSON object, e.g., {@code {"age":25}}.
     *
     * @param x the {@code Map.Entry} object to convert, may be {@code null}
     * @return the JSON string representation of the entry, or {@code null} if the input is {@code null}
     */
    @Override
    public String stringOf(final Map.Entry<K, V> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asMap(x.getKey(), x.getValue()));
    }

    /**
     * Parses a JSON string to create a {@link Map.Entry} object.
     * The string must represent a JSON object with exactly one key-value pair,
     * for example {@code {"age":25}}.
     *
     * @param str the JSON string to parse; may be {@code null}, empty, or {@code "{}"}
     * @return the parsed {@code Map.Entry}, or {@code null} if the input is {@code null},
     *         empty, or an empty JSON object ({@code "{}"})
     * @throws IllegalArgumentException if the JSON object contains more than one entry
     */
    @Override
    public Map.Entry<K, V> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str) || "{}".equals(str)) {
            return null; // NOSONAR
        }

        final Map<K, V> map = Utils.jsonParser.deserialize(str, jdc, Clazz.<K, V> ofMap());

        if (map == null || map.isEmpty()) {
            return null; // NOSONAR
        }

        if (map.size() != 1) {
            throw new IllegalArgumentException("Map.Entry JSON must contain exactly one entry, but got: " + map.size());
        }

        return map.entrySet().iterator().next();
    }

    /**
     * Appends the JSON representation of a {@link Map.Entry} to an {@link Appendable}.
     * The entry is formatted as a single-pair JSON object with the key and value
     * serialized according to their respective type handlers.
     * When the {@code Appendable} is a {@link java.io.Writer}, a buffered wrapper is used
     * for better I/O performance.
     * If the entry is {@code null}, the literal string {@code "null"} is appended.
     *
     * @param appendable the target to write to
     * @param x the {@code Map.Entry} to append, may be {@code null}
     * @throws IOException if an I/O error occurs while appending
     */
    @Override
    public void appendTo(final Appendable appendable, final Map.Entry<K, V> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

                try {
                    bw.write(SK._BRACE_L);

                    keyType.appendTo(bw, x.getKey());
                    bw.write(SK._COLON);
                    valueType.appendTo(bw, x.getValue());

                    bw.write(SK._BRACE_R);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw new UncheckedIOException(e);
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            } else {
                appendable.append(SK._BRACE_L);

                keyType.appendTo(appendable, x.getKey());
                appendable.append(SK._COLON);
                valueType.appendTo(appendable, x.getValue());

                appendable.append(SK._BRACE_R);
            }
        }
    }

    /**
     * Writes the JSON representation of a {@link Map.Entry} to a {@link CharacterWriter}.
     * The entry is formatted as a single-pair JSON object, with the key and value serialized
     * using their respective type handlers and the provided configuration.
     * If the entry is {@code null}, the literal {@code "null"} character array is written.
     *
     * @param writer the {@code CharacterWriter} to write to
     * @param x the {@code Map.Entry} to write, may be {@code null}
     * @param config the serialization configuration used when writing the key and value
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Map.Entry<K, V> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(SK._BRACE_L);

                keyType.writeCharacter(writer, x.getKey(), config);
                writer.write(SK._COLON);
                valueType.writeCharacter(writer, x.getValue(), config);

                writer.write(SK._BRACE_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Generates the type name for a {@code Map.Entry} with the specified key and value types.
     *
     * @param keyTypeName the name of the key type
     * @param valueTypeName the name of the value type
     * @param isDeclaringName {@code true} to use declaring (simple) names; {@code false} for canonical names
     * @return the formatted type name string, e.g. {@code "Map.Entry<String, Integer>"}
     */
    protected static String getTypeName(final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return "Map.Entry" + SK.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return "Map.Entry" + SK.LESS_THAN + TypeFactory.getType(keyTypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(valueTypeName).name()
                    + SK.GREATER_THAN;
        }
    }
}
