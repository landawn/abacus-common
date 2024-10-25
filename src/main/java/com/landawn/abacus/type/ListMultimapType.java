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

import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <K> the key type
 * @param <E>
 */
@SuppressWarnings("java:S2160")
public class ListMultimapType<K, E> extends AbstractType<ListMultimap<K, E>> {

    private static final Class<?> typeClass = ListMultimap.class;

    private final String declaringName;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    ListMultimapType(final String keyTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));
        parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName) };

        declaringName = getTypeName(typeClass, keyTypeName, valueTypeName, true);

        jdc = JDC.create()
                .setMapKeyType(parameterTypes[0])
                .setMapValueType(TypeFactory.getType("List<" + valueTypeName + ">"))
                .setElementType(parameterTypes[1]);
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Class<ListMultimap<K, E>> clazz() {
        return (Class) typeClass;
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
     * @return {@code true}, if is generic type
     */
    @Override
    public boolean isGenericType() {
        return true;
    }

    /**
     * Checks if is serializable.
     *
     * @return {@code true}, if is serializable
     */
    @Override
    public boolean isSerializable() {
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(final ListMultimap<K, E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     *
     * @param st
     * @return
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    @Override
    public ListMultimap<K, E> valueOf(final String st) {
        if (Strings.isEmpty(st)) {
            return null; // NOSONAR
        }

        final Map<K, Collection<E>> map = Utils.jsonParser.deserialize(st, jdc, Map.class);
        final ListMultimap<K, E> multiMap = N.newLinkedListMultimap(map.size());

        for (final Map.Entry<K, Collection<E>> entry : map.entrySet()) {
            multiMap.putMany(entry.getKey(), entry.getValue());
        }

        return multiMap;
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
    @SuppressWarnings("hiding")
    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;

        }
    }
}
