/*
 * Copyright (C) 2020 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.io.IOException;
import java.io.Writer;
import java.util.AbstractMap;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.parser.SerializationConfig;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Clazz;
import com.landawn.abacus.util.ImmutableEntry;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @since 0.9
 */
@SuppressWarnings("java:S2160")
public class ImmutableMapEntryType<K, V> extends AbstractType<AbstractMap.SimpleImmutableEntry<K, V>> {

    private final String declaringName;

    @SuppressWarnings("rawtypes")
    private final Class<AbstractMap.SimpleImmutableEntry<K, V>> typeClass = (Class) AbstractMap.SimpleImmutableEntry.class; //NOSONAR

    private final Type<K> keyType;

    private final Type<V> valueType;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    ImmutableMapEntryType(String keyTypeName, String valueTypeName) {
        super(getTypeName(keyTypeName, valueTypeName, false));

        this.declaringName = getTypeName(keyTypeName, valueTypeName, true);
        this.keyType = TypeFactory.getType(keyTypeName);
        this.valueType = TypeFactory.getType(valueTypeName);
        this.parameterTypes = new Type[] { keyType, valueType };
        this.jdc = JDC.create().setMapKeyType(keyType).setMapValueType(valueType);
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * 
     *
     * @return 
     */
    @Override
    public Class<AbstractMap.SimpleImmutableEntry<K, V>> clazz() {
        return typeClass;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<?>[] getParameterTypes() {
        return parameterTypes;
    }

    /**
     * Checks if is generic type.
     *
     * @return true, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(AbstractMap.SimpleImmutableEntry<K, V> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(N.asMap(x.getKey(), x.getValue()));
    }

    /**
     *
     * @param str
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public AbstractMap.SimpleImmutableEntry<K, V> valueOf(String str) {
        if (N.isNullOrEmpty(str) || "{}".equals(str)) {
            return null;
        }

        return ImmutableEntry.copyOf(Utils.jsonParser.deserialize(Clazz.<K, V> ofMap(), str, jdc).entrySet().iterator().next());
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, AbstractMap.SimpleImmutableEntry<K, V> x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);

            try {
                bw.write(WD._BRACE_L);

                keyType.write(bw, x.getKey());
                writer.write(WD._COLON);
                valueType.write(bw, x.getValue());

                bw.write(WD._BRACE_R);

                if (!isBufferedWriter) {
                    bw.flush();
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @param config
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void writeCharacter(CharacterWriter writer, AbstractMap.SimpleImmutableEntry<K, V> x, SerializationConfig<?> config) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            try {
                writer.write(WD._BRACE_L);

                keyType.writeCharacter(writer, x.getKey(), config);
                writer.write(WD._COLON);
                valueType.writeCharacter(writer, x.getValue(), config);

                writer.write(WD._BRACE_R);

            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /**
     * Gets the type name.
     *
     * @param keyTypeName
     * @param valueTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(String keyTypeName, String valueTypeName, boolean isDeclaringName) {
        if (isDeclaringName) {
            return "Map.ImmutableEntry" + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return "Map.ImmutableEntry" + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE + TypeFactory.getType(valueTypeName).name()
                    + WD.GREATER_THAN;
        }
    }
}
