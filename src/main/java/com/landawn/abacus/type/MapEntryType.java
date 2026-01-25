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
import java.util.Map;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserializationConfig;
import com.landawn.abacus.parser.JsonDeserializationConfig.JDC;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Clazz;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 * Type handler for Map.Entry objects with generic key and value types.
 * This class handles serialization and deserialization of Map.Entry instances.
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

    private final Type<?>[] parameterTypes;

    private final JsonDeserializationConfig jdc;

    MapEntryType(final String keyTypeName, final String valueTypeName) {
        super(getTypeName(keyTypeName, valueTypeName, false));

        declaringName = getTypeName(keyTypeName, valueTypeName, true);
        keyType = TypeFactory.getType(keyTypeName);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = new Type[] { keyType, valueType };
        jdc = JDC.create().setMapKeyType(keyType).setMapValueType(valueType);
    }

    /**
     * Returns the declaring name of this MapEntry type.
     * The declaring name includes the fully qualified class names of the key and value types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     * String name = type.declaringName();
     * // Returns: "Map.Entry<String, Integer>"
     * }</pre>
     *
     * @return The declaring name in format "Map.Entry&lt;KeyDeclaringName, ValueDeclaringName&gt;"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Map.Entry type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     * Class<Map.Entry<String, Integer>> clazz = type.clazz();
     * // Returns: Map.Entry.class
     * }</pre>
     *
     * @return The Class object for Map.Entry
     */
    @Override
    public Class<Map.Entry<K, V>> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types for this generic Map.Entry type.
     * The array contains two elements: the key type at index 0 and the value type at index 1.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     * Type<?>[] paramTypes = type.getParameterTypes();
     * // Returns: [StringType, IntegerType]
     * // paramTypes[0] is the key type (String)
     * // paramTypes[1] is the value type (Integer)
     * }</pre>
     *
     * @return An array containing the key type and value type
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a generic type.
     * For MapEntryType, this always returns {@code true} since Map.Entry is parameterized with key and value types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     * boolean isGeneric = type.isGenericType();
     * // Returns: true
     * }</pre>
     *
     * @return {@code true}, indicating that Map.Entry is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts a Map.Entry object to its JSON string representation.
     * The entry is serialized as a JSON object with a single key-value pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     * Map.Entry<String, Integer> entry = Map.entry("age", 25);
     *
     * String json = type.stringOf(entry);
     * // Returns: "{\"age\":25}"
     *
     * json = type.stringOf(null);
     * // Returns: null
     * }</pre>
     *
     * @param x The Map.Entry object to convert
     * @return The JSON string representation in format "{key:value}", or {@code null} if the input is null
     */
    @Override
    public String stringOf(final Map.Entry<K, V> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asMap(x.getKey(), x.getValue()));
    }

    /**
     * Parses a JSON string to create a Map.Entry object.
     * The string should represent a JSON object with exactly one key-value pair.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     *
     * Map.Entry<String, Integer> entry = type.valueOf("{\"age\":25}");
     * // Returns: Map.Entry with key="age" and value=25
     *
     * entry = type.valueOf(null);
     * // Returns: null
     *
     * entry = type.valueOf("{}");
     * // Returns: null
     * }</pre>
     *
     * @param str The JSON string to parse
     * @return The parsed Map.Entry object, or {@code null} if the input is {@code null}, empty, or represents an empty object "{}"
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

        return map.entrySet().iterator().next();
    }

    /**
     * Appends the string representation of a Map.Entry to an Appendable.
     * The entry is formatted as a JSON object with the key and value properly serialized according to their types.
     * If the Appendable is a Writer, buffering is used for better performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     * Map.Entry<String, Integer> entry = Map.entry("age", 25);
     * StringBuilder sb = new StringBuilder();
     *
     * type.appendTo(sb, entry);
     * // sb now contains: {"age":25}
     *
     * type.appendTo(sb, null);
     * // sb now contains: {"age":25}null
     * }</pre>
     *
     * @param appendable The Appendable to write to
     * @param x The Map.Entry to append
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
                    bw.write(WD._BRACE_L);

                    keyType.appendTo(bw, x.getKey());
                    bw.write(WD._COLON);
                    valueType.appendTo(bw, x.getValue());

                    bw.write(WD._BRACE_R);

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
                appendable.append(WD._BRACE_L);

                keyType.appendTo(appendable, x.getKey());
                appendable.append(WD._COLON);
                valueType.appendTo(appendable, x.getValue());

                appendable.append(WD._BRACE_R);
            }
        }
    }

    /**
     * Writes the character representation of a Map.Entry to a CharacterWriter.
     * The entry is formatted as a JSON object with the key and value properly serialized.
     * This method is optimized for character-based writing.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type<Map.Entry<String, Integer>> type = TypeFactory.getType("Map.Entry<String, Integer>");
     * Map.Entry<String, Integer> entry = Map.entry("age", 25);
     * CharacterWriter writer = CharacterWriter.of(new StringWriter());
     * JsonXmlSerializationConfig<?> config = JsonXmlSerializationConfig.of();
     *
     * type.writeCharacter(writer, entry, config);
     * // writer now contains: {"age":25}
     *
     * type.writeCharacter(writer, null, config);
     * // writer now contains: {"age":25}null
     * }</pre>
     *
     * @param writer The CharacterWriter to write to
     * @param x The Map.Entry to write
     * @param config The serialization configuration to use for formatting
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Map.Entry<K, V> x, final JsonXmlSerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACE_L);

                keyType.writeCharacter(writer, x.getKey(), config);
                writer.write(WD._COLON);
                valueType.writeCharacter(writer, x.getValue(), config);

                writer.write(WD._BRACE_R);

            } catch (final IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Generates the type name for a Map.Entry with the specified key and value types.
     * This is an internal method used by the type system.
     *
     * @param keyTypeName The name of the key type
     * @param valueTypeName The name of the value type
     * @param isDeclaringName Whether to use declaring names (true) or regular names (false)
     * @return The formatted type name string
     */
    protected static String getTypeName(final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return "Map.Entry" + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return "Map.Entry" + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(valueTypeName).name()
                    + WD.GREATER_THAN;
        }
    }
}
