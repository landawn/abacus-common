/*
 * Copyright (C) 2025 HaiYang Li
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.TreeMultiset;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 * @param <E>
 */
@SuppressWarnings("java:S2160")
public class GuavaMultisetType<E, T extends Multiset<E>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<E>[] parameterTypes;

    private final Type<E> elementType;

    private final boolean isOrdered;

    private final JSONDeserializationConfig jdc;

    @SuppressWarnings("unchecked")
    GuavaMultisetType(final Class<T> typeClass, final String parameterTypeName) {
        super(getTypeName(typeClass, parameterTypeName, false));

        this.typeClass = typeClass;
        declaringName = getTypeName(typeClass, parameterTypeName, true);
        parameterTypes = new Type[] { TypeFactory.getType(parameterTypeName) };
        elementType = parameterTypes[0];
        isOrdered = declaringName.startsWith("Linked") || declaringName.startsWith("Sorted");

        jdc = JDC.create().setMapKeyType(elementType).setMapValueType(Integer.class).setElementType(elementType);
    }

    @Override
    public String declaringName() {
        return declaringName;
    }

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
    public Type<E>[] getParameterTypes() {
        return parameterTypes;
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
    public String stringOf(final T x) {
        if (x == null) {
            return null;
        }

        final Map<E, Integer> map = isOrdered ? N.newLinkedHashMap(x.size()) : N.newHashMap(x.size());

        for (final E e : x.elementSet()) {
            map.put(e, x.count(e));
        }

        return Utils.jsonParser.serialize(map, Utils.jsc);
    }

    /**
     *
     * @param str
     * @return
     */
    @MayReturnNull
    @Override
    public T valueOf(final String str) {
        if (Strings.isEmpty(str)) {
            return null; // NOSONAR
        }

        final Map<E, Integer> map = Utils.jsonParser.deserialize(str, jdc, Map.class);

        final T multiset = newInstance(map.size());

        for (final Map.Entry<E, Integer> entry : map.entrySet()) {
            multiset.add(entry.getKey(), entry.getValue());
        }

        return multiset;
    }

    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + WD.GREATER_THAN;
        }
    }

    protected T newInstance(int size) {
        if (HashMultiset.class.isAssignableFrom(typeClass) || Modifier.isAbstract(typeClass.getModifiers())) {
            return (T) HashMultiset.create(size);
        } else if (LinkedHashMultiset.class.isAssignableFrom(typeClass)) {
            return (T) LinkedHashMultiset.create(size);
        } else if (TreeMultiset.class.isAssignableFrom(typeClass)) {
            return (T) TreeMultiset.create();
        } else {
            Constructor<T> constructor = ClassUtil.getDeclaredConstructor(typeClass);

            if (constructor != null) {
                return ClassUtil.invokeConstructor(constructor);
            } else {
                constructor = ClassUtil.getDeclaredConstructor(typeClass, int.class);

                if (constructor != null) {
                    return ClassUtil.invokeConstructor(constructor, size);
                } else {
                    Method method = ClassUtil.getDeclaredMethod(typeClass, "create");

                    if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                            && typeClass.isAssignableFrom(method.getReturnType())) {
                        return (T) ClassUtil.invokeMethod(method);
                    } else {
                        method = ClassUtil.getDeclaredMethod(typeClass, "create", int.class);

                        if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                                && typeClass.isAssignableFrom(method.getReturnType())) {
                            return (T) ClassUtil.invokeMethod(method, size);
                        }
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported Multiset type: " + typeClass.getName() + ". No constructor or static factory method found.");
        }
    }
}
