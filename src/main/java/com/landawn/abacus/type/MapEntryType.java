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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
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

    private final JSONDeserializationConfig jdc;

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
     * @return The declaring name in format "Map.Entry<KeyDeclaringName, ValueDeclaringName>"
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the Map.Entry type.
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
     * @return An array containing the key type and value type
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this is a generic type.
     * For MapEntryType, this always returns true since Map.Entry is parameterized with key and value types.
     *
     * @return true, indicating that Map.Entry is a generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts a Map.Entry object to its JSON string representation.
     * The entry is serialized as a JSON object with a single key-value pair.
     *
     * @param x The Map.Entry object to convert
     * @return The JSON string representation in format "{key:value}", or null if the input is null
     */
    @Override
    public String stringOf(final Map.Entry<K, V> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asMap(x.getKey(), x.getValue()));
    }

    /**
     * Parses a JSON string to create a Map.Entry object.
     * The string should represent a JSON object with exactly one key-value pair.
     *
     * @param str The JSON string to parse
     * @return The parsed Map.Entry object, or null if the input is null, empty, or represents an empty object "{}"
     */
    @MayReturnNull
    @Override
    public Map.Entry<K, V> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str) || "{}".equals(str)) {
            return null; // NOSONAR
        }

        return Utils.jsonParser.deserialize(str, jdc, Clazz.<K, V> ofMap()).entrySet().iterator().next();
    }

    /**
     * Appends the string representation of a Map.Entry to an Appendable.
     * The entry is formatted as a JSON object with the key and value properly serialized according to their types.
     * If the Appendable is a Writer, buffering is used for better performance.
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
                    writer.append(WD._COLON);
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
     * @param writer The CharacterWriter to write to
     * @param x The Map.Entry to write
     * @param config The serialization configuration to use for formatting
     * @throws IOException if an I/O error occurs while writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final Map.Entry<K, V> x, final JSONXMLSerializationConfig<?> config) throws IOException {
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