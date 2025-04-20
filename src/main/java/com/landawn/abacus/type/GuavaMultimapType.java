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
import java.util.Collection;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.parser.JSONDeserializationConfig;
import com.landawn.abacus.parser.JSONDeserializationConfig.JDC;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;

/**
 *
 */
@SuppressWarnings("java:S2160")
public class GuavaMultimapType<K, V, T extends Multimap<K, V>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final Type<?>[] parameterTypes;

    private final JSONDeserializationConfig jdc;

    GuavaMultimapType(final Class<T> typeClass, final String keyTypeName, final String valueTypeName) {
        super(getTypeName(typeClass, keyTypeName, valueTypeName, false));

        declaringName = getTypeName(typeClass, keyTypeName, valueTypeName, true);

        this.typeClass = typeClass;

        if (SetMultimap.class.isAssignableFrom(typeClass)) {
            parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType("Set<" + valueTypeName + ">") };
        } else {
            parameterTypes = new Type[] { TypeFactory.getType(keyTypeName), TypeFactory.getType("List<" + valueTypeName + ">") };
        }

        jdc = JDC.create().setMapKeyType(parameterTypes[0]).setMapValueType(parameterTypes[1]).setElementType(parameterTypes[1].getElementType());
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
    public String stringOf(final T x) {
        if (x == null) {
            return null;
        }

        final Map<K, Collection<V>> map = x.asMap();

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

        final Map<K, Collection<V>> map = Utils.jsonParser.deserialize(str, jdc, Map.class);
        final int avgValueSize = (int) map.values().stream().mapToInt(Collection::size).average().orElse(0);

        final T multimap = newInstance(map.size(), avgValueSize);

        for (final Map.Entry<K, Collection<V>> entry : map.entrySet()) {
            multimap.putAll(entry.getKey(), entry.getValue());
        }

        if (ImmutableListMultimap.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableListMultimap.copyOf(multimap);
        } else if (ImmutableSetMultimap.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableSetMultimap.copyOf(multimap);
        } else if (ImmutableMultimap.class.isAssignableFrom(typeClass)) {
            return (T) ImmutableMultimap.copyOf(multimap);
        }

        return multimap;
    }

    protected static String getTypeName(final Class<?> typeClass, final String keyTypeName, final String valueTypeName, final boolean isDeclaringName) {
        if (isDeclaringName) {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).declaringName() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).declaringName() + WD.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + WD.LESS_THAN + TypeFactory.getType(keyTypeName).name() + WD.COMMA_SPACE
                    + TypeFactory.getType(valueTypeName).name() + WD.GREATER_THAN;
        }
    }

    private T newInstance(int keySize, final int avgValueSize) {
        if (ArrayListMultimap.class.isAssignableFrom(typeClass) || ImmutableListMultimap.class.isAssignableFrom(typeClass) //
                || Multimap.class.equals(typeClass) || ImmutableMultimap.class.equals(typeClass)
                || (Modifier.isAbstract(typeClass.getModifiers()) && ListMultimap.class.isAssignableFrom(typeClass))) {
            return (T) ArrayListMultimap.create(keySize, avgValueSize);
        } else if (LinkedListMultimap.class.isAssignableFrom(typeClass)) {
            return (T) LinkedListMultimap.create(keySize);
        } else if (TreeMultimap.class.isAssignableFrom(typeClass)
                || (Modifier.isAbstract(typeClass.getModifiers()) && SortedSetMultimap.class.isAssignableFrom(typeClass))) {
            return (T) TreeMultimap.create();
        } else if (HashMultimap.class.isAssignableFrom(typeClass) || ImmutableSetMultimap.class.isAssignableFrom(typeClass)
                || (Modifier.isAbstract(typeClass.getModifiers()) && SetMultimap.class.isAssignableFrom(typeClass))) {
            return (T) HashMultimap.create(keySize, avgValueSize);
        } else if (LinkedHashMultimap.class.isAssignableFrom(typeClass)) {
            return (T) LinkedHashMultimap.create(keySize, avgValueSize);
        } else {
            Constructor<T> constructor = ClassUtil.getDeclaredConstructor(typeClass);

            if (constructor != null) {
                return ClassUtil.invokeConstructor(constructor);
            } else {
                constructor = ClassUtil.getDeclaredConstructor(typeClass, int.class, int.class);

                if (constructor != null) {
                    return ClassUtil.invokeConstructor(constructor, keySize);
                } else {
                    Method method = ClassUtil.getDeclaredMethod(typeClass, "create");

                    if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                            && typeClass.isAssignableFrom(method.getReturnType())) {
                        return ClassUtil.invokeMethod(method);
                    } else {
                        method = ClassUtil.getDeclaredMethod(typeClass, "create", int.class, int.class);

                        if (method != null && Modifier.isStatic(method.getModifiers()) && Modifier.isPublic(method.getModifiers())
                                && typeClass.isAssignableFrom(method.getReturnType())) {
                            return ClassUtil.invokeMethod(method, keySize);
                        }
                    }
                }
            }

            throw new IllegalArgumentException("Unsupported Multimap type: " + typeClass.getName() + ". No constructor or static factory method found.");
        }
    }
}
