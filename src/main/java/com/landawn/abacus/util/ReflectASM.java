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

// TODO: Auto-generated Javadoc
/**
 * The Class ReflectASM.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
final class ReflectASM<T> {

    /** The Constant EMPTY_CLASSES. */
    @SuppressWarnings("rawtypes")
    static final Class[] EMPTY_CLASSES = new Class[0];

    /** The Constant clsFieldPool. */
    static final Map<Class<?>, FieldAccess> clsFieldPool = new ConcurrentHashMap<>();

    /** The Constant clsConstructorPool. */
    static final Map<Class<?>, ConstructorAccess<?>> clsConstructorPool = new ConcurrentHashMap<>();

    /** The Constant clsMethodPool. */
    static final Map<Class<?>, MethodAccess> clsMethodPool = new ConcurrentHashMap<>();

    /** The cls. */
    private final Class<T> cls;

    /** The target. */
    private final T target;

    /**
     * Instantiates a new reflect ASM.
     *
     * @param cls the cls
     * @param target the target
     */
    ReflectASM(Class<T> cls, T target) {
        this.cls = cls;
        this.target = target;
    }

    /**
     * On.
     *
     * @param <T> the generic type
     * @param clsName the cls name
     * @return the reflect ASM
     */
    public static <T> ReflectASM<T> on(String clsName) {
        return on((Class<T>) ClassUtil.forClass(clsName));
    }

    /**
     * On.
     *
     * @param <T> the generic type
     * @param cls the cls
     * @return the reflect ASM
     */
    public static <T> ReflectASM<T> on(Class<T> cls) {
        return new ReflectASM<T>(cls, null);
    }

    /**
     * On.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the reflect ASM
     */
    public static <T> ReflectASM<T> on(T target) {
        return new ReflectASM<T>((Class<T>) target.getClass(), target);
    }

    /**
     * New.
     *
     * @return the reflect ASM
     */
    public ReflectASM<T> _new() {
        return new ReflectASM<T>(cls, getConstructorAccess(cls).newInstance());
    }

    /**
     * Gets the.
     *
     * @param <V> the value type
     * @param fieldName the field name
     * @return the v
     */
    public <V> V get(String fieldName) {
        final FieldAccess fieldAccess = getFieldAccess(fieldName);

        return (V) fieldAccess.get(target, fieldName);
    }

    /**
     * Sets the.
     *
     * @param fieldName the field name
     * @param value the value
     * @return the reflect ASM
     */
    public ReflectASM<T> set(String fieldName, Object value) {
        final FieldAccess fieldAccess = getFieldAccess(fieldName);

        fieldAccess.set(target, fieldName, value);

        return this;
    }

    /**
     * Invoke.
     *
     * @param <V> the value type
     * @param methodName the method name
     * @param args the args
     * @return the v
     */
    @SafeVarargs
    public final <V> V invoke(String methodName, Object... args) {
        final MethodAccess methodAccess = getMethodAccess(cls);

        return (V) methodAccess.invoke(target, methodName, args);
    }

    /**
     * Invokke.
     *
     * @param methodName the method name
     * @param args the args
     * @return the reflect ASM
     */
    @SafeVarargs
    public final ReflectASM<T> invokke(String methodName, Object... args) {
        invoke(methodName, args);

        return this;
    }

    /**
     * Gets the field access.
     *
     * @param fieldName the field name
     * @return the field access
     */
    private FieldAccess getFieldAccess(String fieldName) {
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
     * @param cls the cls
     * @return the constructor access
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
     * @param cls the cls
     * @return the method access
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
