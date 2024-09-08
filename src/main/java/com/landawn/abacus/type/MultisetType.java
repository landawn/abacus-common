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

import java.util.Map;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 * @author Haiyang Li
 * @param <E>
 * @since 0.8
 */
@SuppressWarnings("java:S2160")
public class MultisetType<E> extends AbstractType<Multiset<E>> {

    private final String declaringName;

    private static final Class<?> typeClass = Multiset.class;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final JSONDeserializationConfig jdc;

    @SuppressWarnings("unchecked")
    MultisetType(final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        declaringName = getTypeName(typeClass, parameterTypeName, true);
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];

        jdc = JDC.create().setMapKeyType(elementType).setMapValueType(Integer.class);
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
    public Class<Multiset<E>> clazz() {
        return (Class<Multiset<E>>) typeClass;
    }

    /**
     * Gets the element type.
     *
     * @return
     */
    @Override
    public Type<E> getElementType() {
        return elementType;
    }

    /**
     * Gets the parameter types.
     *
     * @return
     */
    @Override
    public Type<E>[] getParameterTypes() {
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
    public String stringOf(final Multiset<E> x) {
        return (x == null) ? null : Utils.jsonParser.serialize(x.toMap(), Utils.jsc);
    }

    /**
     *
     * @param str
     * @return {@code null} if {@code (Strings.isEmpty(str))}. (auto-generated java doc for return)
     */
    @MayReturnNull
    @Override
    public Multiset<E> valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Map<E, Integer> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        final Multiset<E> multiSet = N.newMultiset(map.size());

        for (final Map.Entry<E, Integer> entry : map.entrySet()) {
            multiSet.add(entry.getKey(), entry.getValue());
        }

        return multiSet;
    }

    /**
     * Gets the type name.
     *
     * @param typeClass
     * @param parameterTypeName
     * @param isDeclaringName
     * @return
     */
    @SuppressWarnings("hiding")
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;

        }
    }
}
