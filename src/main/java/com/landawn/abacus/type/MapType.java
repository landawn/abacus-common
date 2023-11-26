/*
 * Copyright (C) 2015 HaiYang Li
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
import java.util.Map;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <V> the value type
 * @param <T>
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class MapType<K, V, T extends Map<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    MapType(Class<T> typeClass, String keyTypeName, String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));

        this.declaringName = getTypeName(typeClass.isInterface() ? typeClass : Map.class, keyTypeName, valueTypeName, true);

        this.typeClass = typeClass;
        this.parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName) };

        this.jdc = JDC.create().setMapKeyType(parameterTypes[0]).setMapValueType(parameterTypes[1]);
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
    public Class<T> clazz() {
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
     * Checks if is map.
     *
     * @return true, if is map
     */
    @Override
    public boolean isMap() {
        return true;
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
     * Checks if is serializable.
     *
     * @return true, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return false;
    }

    /**
     * Gets the serialization type.
     *
     * @return
     */
    @Override
    public SerializationType getSerializationType() {
        return SerializationType.MAP;
    }

    /**
     *
     * @param x
     * @return {@code null} if {@code (x == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public String stringOf(T x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.size() == 0) {
            return "{}";
        }

        return Utils.jsonParser.serialize(x, Utils.jsc);
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (str == null)}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public T valueOf(String str) {
        if (str == null) {
            return null; // NOSONAR
        } else if (str.length() == 0 || "{}".equals(str)) {
            return (T) N.newMap(typeClass);
        } else {
            return Utils.jsonParser.deserialize(typeClass, str, jdc);
        }
    }

    /**
     *
     * @param writer
     * @param x
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Override
    public void write(Writer writer, T x) throws IOException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else {
            // writer.write(stringOf(x));

            Utils.jsonParser.serialize(writer, x, Utils.jsc);
        }
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param keyTypeName
     * @param valueTypeName
     * @param isDeclaringName
     * @return
     */
    protected static String getTypeName(Class<?> typeClass, String keyTypeName, String valueTypeName, boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;
        }
    }
}
