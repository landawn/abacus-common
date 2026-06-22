/*
 * Copyright (C) 2020 HaiYang Li
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
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Clazz;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableEntry;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for immutable Map.Entry objects.
 * This class provides serialization and deserialization capabilities for SimpleImmutableEntry instances,
 * which represent immutable key-value pairs. The entries are serialized as single-entry JSON objects.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@SuppressWarnings("java:S2160")
public class ImmutableMapEntryType<K, V> extends AbstractType<AbstractMap.SimpleImmutableEntry<K, V>> {

    static final String MAP_IMMUTABLE_ENTRY = "Map.ImmutableEntry";

    @SuppressWarnings("rawtypes")
    private final Class<AbstractMap.SimpleImmutableEntry<K, V>> typeClass = (Class) AbstractMap.SimpleImmutableEntry.class; //NOSONAR

    private final String declaringName;

    private final Type<K> keyType;

    private final Type<V> valueType;

    private final List<Type<?>> parameterTypes;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for ImmutableMapEntryType.
     * This constructor is called by the TypeFactory to create
     * {@code Map.ImmutableEntry<K, V>} type instances.
     *
     * @param keyTypeName the name of the key type parameter
     * @param valueTypeName the name of the value type parameter
     */
    ImmutableMapEntryType(final String keyTypeName, final String valueTypeName) {
        super(getTypeName(keyTypeName, valueTypeName, false));

        declaringName = getTypeName(keyTypeName, valueTypeName, true);
        keyType = TypeFactory.getType(keyTypeName);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = List.of(keyType, valueType);
        jdc = JsonDeserConfig.create().setMapKeyType(keyType).setMapValueType(valueType);
    }

    /**
     * Returns the declaring name of this immutable map entry type.
     * The declaring name represents the type in a simplified format suitable for type declarations.
     *
     * @return the declaring name of this type (e.g., "Map.ImmutableEntry&lt;String, Integer&gt;")
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the SimpleImmutableEntry type handled by this type handler.
     *
     * @return the Class object for AbstractMap.SimpleImmutableEntry
     */
    @Override
    public Class<AbstractMap.SimpleImmutableEntry<K, V>> javaType() {
        return typeClass;
    }

    /**
     * Returns an immutable list containing the parameter types of this generic map entry type.
     * For map entry types, this list contains two elements: the key type and the value type.
     *
     * @return an immutable list containing the key type and value type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type is a generic type with type parameters.
     * ImmutableMapEntry types are always parameterized with key and value types.
     *
     * @return {@code true}, as {@code Map.ImmutableEntry} is a generic type
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Serializes an immutable map entry to its JSON string representation as a single-entry object
     * (e.g., {@code {"age":25}}).
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; {@code stringOf} and {@code valueOf} are inverse operations that round-trip. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the immutable map entry to serialize; may be {@code null}
     * @return the JSON string, or {@code null} if {@code x} is {@code null}
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @Override
    public String stringOf(final AbstractMap.SimpleImmutableEntry<K, V> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asMap(x.getKey(), x.getValue()), Utils.jsc);
    }

    /**
     * Deserializes a JSON string into an immutable map entry instance.
     * The string must be a single-entry JSON object (e.g., {@code {"age":25}}).
     * Returns {@code null} for empty strings or the empty JSON object ({@code "{}"}).
     *
     * <p>This method is the inverse of {@code stringOf} and round-trips with it: it parses the string produced by
     * {@code stringOf} back into a value of this type. Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse; may be {@code null} or empty
     * @return a new immutable map entry, or {@code null} if the input is {@code null}, empty, or {@code "{}"}
     * @throws IllegalArgumentException if the JSON object contains more than one entry
     * @see #valueOf(Object)
     * @see #stringOf(AbstractMap.SimpleImmutableEntry)
     */
    @Override
    public AbstractMap.SimpleImmutableEntry<K, V> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str) || "{}".equals(str)) {
            return null; // NOSONAR
        }

        final Map<K, V> map = Utils.jsonParser.deserialize(str, jdc, Clazz.<K, V> ofMap());

        if (map == null || map.isEmpty()) {
            return null; // NOSONAR
        }

        if (map.size() != 1) {
            throw new IllegalArgumentException("Map.ImmutableEntry JSON must contain exactly one entry, but got: " + map.size());
        }

        return ImmutableEntry.copyOf(map.entrySet().iterator().next());
    }

    /**
     * Appends the {@code toString()}-style string representation of an immutable map entry to an {@link Appendable}.
     * The output format is a single-entry map (e.g., {@code {age:25}}).
     * {@link Writer} instances are wrapped in a buffered writer for better performance.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the {@link Appendable} to write to
     * @param x the immutable map entry to append; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
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
    @Override
    public void appendTo(final Appendable appendable, final AbstractMap.SimpleImmutableEntry<K, V> x) throws IOException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

                IOException thrown = null;

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
                    thrown = e;
                    throw e;
                } finally {
                    if (!isBufferedWriter) {
                        try {
                            Objectory.recycle((BufferedWriter) bw);
                        } catch (final UncheckedIOException e) {
                            final Throwable cause = e.getCause();

                            if (thrown == null && cause instanceof IOException) {
                                throw (IOException) cause;
                            } else if (thrown == null) {
                                throw e;
                            }
                        }
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
     * Writes the JSON representation of an immutable map entry to a {@link CharacterWriter}
     * as a single-entry JSON object (e.g., {@code {"age":25}}).
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x the immutable map entry to write; may be {@code null}
     * @param config the serialization configuration to use; may be {@code null}
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final AbstractMap.SimpleImmutableEntry<K, V> x, final JsonXmlSerConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            writer.write(SK._BRACE_L);

            serializeKey(writer, x.getKey(), config);
            writer.write(SK._COLON);
            valueType.serializeTo(writer, x.getValue(), config);

            writer.write(SK._BRACE_R);
        }
    }

    private void serializeKey(final CharacterWriter writer, final K key, final JsonXmlSerConfig<?> config) throws IOException {
        final boolean isQuoteMapKey = config instanceof JsonSerConfig jsonConfig ? jsonConfig.isQuoteMapKey() : config != null;

        if (key == null) {
            if (isQuoteMapKey) {
                writer.write(SK._DOUBLE_QUOTE);
                writer.write(NULL_CHAR_ARRAY);
                writer.write(SK._DOUBLE_QUOTE);
            } else {
                writer.write(NULL_CHAR_ARRAY);
            }
        } else if (keyType.isSerializable() && !(keyType.isArray() || keyType.isCollection() || keyType.javaType().isEnum())) {
            if (isQuoteMapKey || !(keyType.isNumber() || keyType.isBoolean())) {
                writer.write(SK._DOUBLE_QUOTE);
                writer.writeCharacter(keyType.stringOf(key));
                writer.write(SK._DOUBLE_QUOTE);
            } else {
                writer.writeCharacter(keyType.stringOf(key));
            }
        } else {
            keyType.serializeTo(writer, key, config);
        }
    }

    /**
     * Generates a type name string for an immutable map entry type with the specified key and value types.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     *
     * @param keyTypeName the name of the key type
     * @param valueTypeName the name of the value type
     * @param isDeclaringName {@code true} to generate a declaring name with simple type names, {@code false} for fully qualified names
     * @return the formatted type name (e.g., "Map.ImmutableEntry&lt;String, Integer&gt;")
     */
    protected static String getTypeName(final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return MAP_IMMUTABLE_ENTRY + SK.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + SK.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return MAP_IMMUTABLE_ENTRY + SK.LESS_THAN + TypeFactory.getType(keyTypeName).name() + SK.COMMA_SPACE + TypeFactory.getType(valueTypeName).name()
                    + SK.GREATER_THAN;
        }
    }
}
