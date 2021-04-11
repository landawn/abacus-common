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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <K> the key type
 * @param <E>
 * @param <V> the value type
 * @since 0.8
 */
public class MultimapType<K, E, V extends Collection<E>> extends AbstractType<Multimap<K, E, V>> {

    private static final Class<?> typeClass = Multimap.class;

    private final String declaringName;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    MultimapType(String keyTypeName, String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));
        parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType(valueTypeName) };

        this.declaringName = getTypeName(typeClass, keyTypeName, valueTypeName, true);

        CollectionType<?, ?> collType = null;
        if (parameterTypes[1] instanceof CollectionType) {
            collType = (CollectionType<?, ?>) parameterTypes[1];
        } else {
            throw new IllegalArgumentException("The value type of Multimap must be a collection type: " + valueTypeName);
        }

        jdc = JDC.create().setMapKeyType(parameterTypes[0]).setMapValueType(parameterTypes[1]).setElementType(collType.getElementType());
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public Class<Multimap<K, E, V>> clazz() {
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
        return true;
    }

    /**
     *
     * @param x
     * @return
     */
    @Override
    public String stringOf(Multimap<K, E, V> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     *
     * @param st
     * @return
     */
    @SuppressWarnings("unchecked")
    @Override
    public Multimap<K, E, V> valueOf(String st) {
        if (N.isNullOrEmpty(st)) {
            return null;
        }

        final Map<K, Collection<E>> map = Utils.jsonParser.deserialize(Map.class, st, jdc);

        if (List.class.isAssignableFrom(parameterTypes[1].clazz())) {
            final Multimap<K, E, V> multiMap = (Multimap<K, E, V>) N.newLinkedListMultimap(map.size());

            for (Map.Entry<K, Collection<E>> entry : map.entrySet()) {
                multiMap.putAll(entry.getKey(), entry.getValue());
            }

            return multiMap;
        } else if (Set.class.isAssignableFrom(parameterTypes[1].clazz())) {
            final Multimap<K, E, V> multiMap = (Multimap<K, E, V>) N.newLinkedSetMultimap(map.size());
            for (Map.Entry<K, Collection<E>> entry : map.entrySet()) {
                multiMap.putAll(entry.getKey(), entry.getValue());
            }

            return multiMap;
        } else {
            final Multimap<K, E, V> multiMap = (Multimap<K, E, V>) N.newLinkedListMultimap(map.size());

            for (Map.Entry<K, Collection<E>> entry : map.entrySet()) {
                multiMap.putAll(entry.getKey(), entry.getValue());
            }

            return multiMap;
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
    @SuppressWarnings("hiding")
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
