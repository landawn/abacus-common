/*
 * Copyright (C) 2017 HaiYang Li
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

package com.landawn.abacus.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 */
final class ReflectASM<T> {
    @SuppressWarnings("rawtypes")
    static final Class[] EMPTY_CLASSES = new Class[0];

    static final Map<Class<?>, FieldAccess> clsFieldPool = new ConcurrentHashMap<>();
    static final Map<Class<?>, ConstructorAccess<?>> clsConstructorPool = new ConcurrentHashMap<>();
    static final Map<Class<?>, MethodAccess> clsMethodPool = new ConcurrentHashMap<>();

    private final Class<T> cls;
    private final T target;

    ReflectASM(Class<T> cls, T target) {
        this.cls = cls;
        this.target = target;
    }

    public static <T> ReflectASM<T> on(String clsName) {
        return on((Class<T>) ClassUtil.forClass(clsName));
    }

    public static <T> ReflectASM<T> on(Class<T> cls) {
        return new ReflectASM<T>(cls, null);
    }

    public static <T> ReflectASM<T> on(T target) {
        return new ReflectASM<T>((Class<T>) target.getClass(), target);
    }

    public ReflectASM<T> _new() {
        return new ReflectASM<T>(cls, getConstructorAccess(cls).newInstance());
    }

    public <V> V get(String fieldName) {
        final FieldAccess fieldAccess = getFieldAccess(fieldName);

        return (V) fieldAccess.get(target, fieldName);
    }

    public ReflectASM<T> set(String fieldName, Object value) {
        final FieldAccess fieldAccess = getFieldAccess(fieldName);

        fieldAccess.set(target, fieldName, value);

        return this;
    }

    @SafeVarargs
    public final <V> V invoke(String methodName, Object... args) {
        final MethodAccess methodAccess = getMethodAccess(cls);

        return (V) methodAccess.invoke(target, methodName, args);
    }

    @SafeVarargs
    public final ReflectASM<T> invokke(String methodName, Object... args) {
        invoke(methodName, args);

        return this;
    }

    private FieldAccess getFieldAccess(String fieldName) {
        FieldAccess fieldAccess = clsFieldPool.get(cls);

        if (fieldAccess == null) {
            fieldAccess = FieldAccess.get(cls);
            clsFieldPool.put(cls, fieldAccess);
        }

        return fieldAccess;
    }

    private ConstructorAccess<T> getConstructorAccess(final Class<T> cls) throws SecurityException {
        ConstructorAccess<?> constructorAccess = clsConstructorPool.get(cls);

        if (constructorAccess == null) {
            constructorAccess = ConstructorAccess.get(cls);
            clsConstructorPool.put(cls, constructorAccess);
        }

        return (ConstructorAccess<T>) constructorAccess;
    }

    private MethodAccess getMethodAccess(final Class<?> cls) {
        MethodAccess methodAccess = clsMethodPool.get(cls);

        if (methodAccess == null) {
            methodAccess = MethodAccess.get(cls);
            clsMethodPool.put(cls, methodAccess);
        }

        return methodAccess;
    }
}
