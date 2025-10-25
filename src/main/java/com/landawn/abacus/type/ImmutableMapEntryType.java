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

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Clazz;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.ImmutableEntry;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

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

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    ImmutableMapEntryType(final String keyTypeName, final String valueTypeName) {
        super(getTypeName(keyTypeName, valueTypeName, false));

        declaringName = getTypeName(keyTypeName, valueTypeName, true);
        keyType = TypeFactory.getType(keyTypeName);
        valueType = TypeFactory.getType(valueTypeName);
        parameterTypes = new Type[] { keyType, valueType };
        jdc = JDC.create().setMapKeyType(keyType).setMapValueType(valueType);
    }

    /**
     * Returns the declaring name of this immutable map entry type.
     * The declaring name represents the type in a simplified format suitable for type declarations.
     *
     * @return the declaring name of this type (e.g., "Map.ImmutableEntry<String, Integer>")
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
    public Class<AbstractMap.SimpleImmutableEntry<K, V>> clazz() {
        return typeClass;
    }

    /**
     * Returns an array containing the parameter types of this generic map entry type.
     * For map entry types, this array contains two elements: the key type and the value type.
     *
     * @return an array containing the key type and value type
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates that this is a generic type with type parameters.
     *
     * @return {@code true} as map entry types are generic types
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Converts an immutable map entry to its string representation.
     * The entry is serialized as a JSON object with a single key-value pair.
     *
     * @param x the immutable map entry to convert to string
     * @return the JSON string representation of the entry (e.g., "{\"key\":\"value\"}"), or null if the input is null
     */
    @Override
    public String stringOf(final AbstractMap.SimpleImmutableEntry<K, V> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asMap(x.getKey(), x.getValue()));
    }

    /**
     * Parses a string representation into an immutable map entry instance.
     * The string should be in JSON object format with a single key-value pair.
     * Empty strings or empty JSON objects ("{}") result in null.
     *
     * @param str the JSON string to parse (e.g., "{\"key\":\"value\"}")
     * @return a new immutable map entry instance, or null if the input is null, empty, or "{}"
     */
    @MayReturnNull
    @Override
    public AbstractMap.SimpleImmutableEntry<K, V> valueOf(final String str) {
        if (Strings.isEmpty(str) || Strings.isBlank(str) || "{}".equals(str)) {
            return null; // NOSONAR
        }

        return ImmutableEntry.copyOf(Utils.jsonParser.deserialize(str, jdc, Clazz.<K, V> ofMap()).entrySet().iterator().next());
    }

    /**
     * Appends the string representation of an immutable map entry to an Appendable.
     * The output format is a JSON object with the key and value.
     * Handles Writer instances specially for better performance with buffering.
     *
     * @param appendable the Appendable to write to
     * @param x the immutable map entry to append
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void appendTo(final Appendable appendable, final AbstractMap.SimpleImmutableEntry<K, V> x) throws IOException {
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
     * Writes the character representation of an immutable map entry to a CharacterWriter.
     * This method is optimized for performance when writing to character-based outputs.
     * The entry is serialized as a JSON object.
     *
     * @param writer the CharacterWriter to write to
     * @param x the immutable map entry to write
     * @param config the serialization configuration to use
     * @throws IOException if an I/O error occurs during writing
     */
    @Override
    public void writeCharacter(final CharacterWriter writer, final AbstractMap.SimpleImmutableEntry<K, V> x, final JSONXMLSerializationConfig<?> config)
            throws IOException {
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
     * Generates a type name string for an immutable map entry type with the specified key and value types.
     * The format depends on whether a declaring name (simplified) or full name is requested.
     *
     * @param keyTypeName the name of the key type
     * @param valueTypeName the name of the value type
     * @param isDeclaringName true to generate a declaring name with simple type names, {@code false} for fully qualified names
     * @return the formatted type name (e.g., "Map.ImmutableEntry<String, Integer>")
     */
    protected static String getTypeName(final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return MAP_IMMUTABLE_ENTRY + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return MAP_IMMUTABLE_ENTRY + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(valueTypeName).name()
                    + WD.GREATER_THAN;
        }
    }
}