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

package com.landawn.abacus.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;

/**
 *
 * @param <T>
 */
final class ReflectASM<T> {

    @SuppressWarnings("rawtypes")
    static final Class[] EMPTY_CLASSES = {};

    static final Map<Class<?>, FieldAccess> clsFieldPool = new ConcurrentHashMap<>();

    static final Map<Class<?>, ConstructorAccess<?>> clsConstructorPool = new ConcurrentHashMap<>();

    static final Map<Class<?>, MethodAccess> clsMethodPool = new ConcurrentHashMap<>();

    private final Class<T> cls;

    private final T target;

    ReflectASM(final Class<T> cls, final T target) {
        this.cls = cls;
        this.target = target;
    }

    /**
     *
     * @param <T>
     * @param clsName
     * @return
     */
    public static <T> ReflectASM<T> on(final String clsName) {
        return on(ClassUtil.forClass(clsName));
    }

    /**
     *
     * @param <T>
     * @param cls
     * @return
     */
    public static <T> ReflectASM<T> on(final Class<T> cls) {
        return new ReflectASM<>(cls, null);
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T> ReflectASM<T> on(final T target) {
        return new ReflectASM<>((Class<T>) target.getClass(), target);
    }

    public ReflectASM<T> _new() { //NOSONAR
        return new ReflectASM<>(cls, getConstructorAccess(cls).newInstance());
    }

    /**
     *
     * @param <V> the value type
     * @param fieldName
     * @return
     */
    public <V> V get(final String fieldName) {
        final FieldAccess fieldAccess = getFieldAccess();

        return (V) fieldAccess.get(target, fieldName);
    }

    /**
     *
     * @param fieldName
     * @param value
     * @return
     */
    public ReflectASM<T> set(final String fieldName, final Object value) {
        final FieldAccess fieldAccess = getFieldAccess();

        fieldAccess.set(target, fieldName, value);

        return this;
    }

    /**
     *
     * @param <V> the value type
     * @param methodName
     * @param args
     * @return
     */
    public <V> V invoke(final String methodName, final Object... args) {
        final MethodAccess methodAccess = getMethodAccess(cls);

        return (V) methodAccess.invoke(target, methodName, args);
    }

    /**
     *
     * @param methodName
     * @param args
     * @return
     */
    public ReflectASM<T> call(final String methodName, final Object... args) {
        invoke(methodName, args);

        return this;
    }

    /**
     * Gets the field access.
     *
     * @return
     */
    private FieldAccess getFieldAccess() {
        FieldAccess fieldAccess = clsFieldPool.get(cls);

        if (fieldAccess == null) {
            fieldAccess = FieldAccess.get(cls);
            clsFieldPool.put(cls, fieldAccess);
        }

        return fieldAccess;
    }

    /**
     * Gets the constructor access.
     *
     * @param cls
     * @return
     * @throws SecurityException the security exception
     */
    private ConstructorAccess<T> getConstructorAccess(final Class<T> cls) throws SecurityException {
        ConstructorAccess<?> constructorAccess = clsConstructorPool.get(cls);

        if (constructorAccess == null) {
            constructorAccess = ConstructorAccess.get(cls);
            clsConstructorPool.put(cls, constructorAccess);
        }

        return (ConstructorAccess<T>) constructorAccess;
    }

    /**
     * Gets the method access.
     *
     * @param cls
     * @return
     */
    private MethodAccess getMethodAccess(final Class<?> cls) {
        MethodAccess methodAccess = clsMethodPool.get(cls);

        if (methodAccess == null) {
            methodAccess = MethodAccess.get(cls);
            clsMethodPool.put(cls, methodAccess);
        }

        return methodAccess;
    }
}
