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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Add <a href="https://github.com/EsotericSoftware/reflectasm/blob/master/src/com/esotericsoftware/reflectasm/AccessClassLoader.java">reflectasm</a> library to build path for better performance.
 * 
 * @param <T>
 * @since 0.8
 * 
 * @author haiyangl
 */
public final class Reflection<T> {
    @SuppressWarnings("rawtypes")
    static final Class[] EMPTY_CLASSES = new Class[0];

    static final boolean isReflectASMAvailable;

    static {
        boolean tmp = true;

        try {
            ClassUtil.forClass("com.esotericsoftware.reflectasm.ConstructorAccess");
            ClassUtil.forClass("com.esotericsoftware.reflectasm.FieldAccess");
            ClassUtil.forClass("com.esotericsoftware.reflectasm.MethodAccess");
        } catch (Exception e) {
            tmp = false;
        }

        isReflectASMAvailable = tmp;
    }

    static final Map<Class<?>, Map<String, Field>> clsFieldPool = new ConcurrentHashMap<>();
    static final Map<Class<?>, Map<Wrapper<Class<?>[]>, Constructor<?>>> clsConstructorPool = new ConcurrentHashMap<>();
    static final Map<Class<?>, Map<String, Map<Wrapper<Class<?>[]>, Method>>> clsMethodPool = new ConcurrentHashMap<>();

    private final Class<T> cls;
    private final T target;
    private ReflectASM<T> reflectASM;

    Reflection(Class<T> cls, T target) {
        this.cls = cls;
        this.target = target;
        this.reflectASM = isReflectASMAvailable ? new ReflectASM<>(cls, target) : null;
    }

    public static <T> Reflection<T> on(String clsName) {
        return on((Class<T>) ClassUtil.forClass(clsName));
    }

    public static <T> Reflection<T> on(Class<T> cls) {
        return new Reflection<>(cls, null);
    }

    public static <T> Reflection<T> on(T target) {
        return new Reflection<>((Class<T>) target.getClass(), target);
    }

    public Reflection<T> _new() {
        return new Reflection<>(cls, N.newInstance(cls));
    }

    @SafeVarargs
    public final Reflection<T> _new(Object... args) {
        if (N.isNullOrEmpty(args)) {
            return _new();
        }

        final Constructor<T> constructor = getDeclaredConstructor(cls, getTypes(args));

        if (Modifier.isPublic(constructor.getModifiers()) == false && constructor.isAccessible() == false) {
            constructor.setAccessible(true);
        }

        return new Reflection<>(cls, ClassUtil.invokeConstructor(constructor, args));
    }

    public T instance() {
        return target;
    }

    public <V> V get(String fieldName) {
        if (reflectASM != null) {
            return reflectASM.get(fieldName);
        } else {
            try {
                final Field field = getField(fieldName);

                if (Modifier.isPublic(field.getModifiers()) == false && field.isAccessible() == false) {
                    field.setAccessible(true);
                }

                return (V) field.get(target);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    public Reflection<T> set(String fieldName, Object value) {
        if (reflectASM != null) {
            reflectASM.set(fieldName, value);
        } else {
            try {
                final Field field = getField(fieldName);

                if (Modifier.isPublic(field.getModifiers()) == false && field.isAccessible() == false) {
                    field.setAccessible(true);
                }

                field.set(target, value);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw N.toRuntimeException(e);
            }
        }

        return this;
    }

    @SafeVarargs
    public final <V> V invoke(String methodName, Object... args) {
        if (reflectASM != null) {
            return reflectASM.invoke(methodName, args);
        } else {
            try {
                final Method method = getDeclaredMethod(cls, methodName, getTypes(args));

                if (Modifier.isPublic(method.getModifiers()) == false && method.isAccessible() == false) {
                    method.setAccessible(true);
                }

                return (V) method.invoke(target, args);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw N.toRuntimeException(e);
            }
        }
    }

    @SafeVarargs
    public final Reflection<T> invokke(String methodName, Object... args) {
        if (reflectASM != null) {
            reflectASM.invokke(methodName, args);
        } else {
            invoke(methodName, args);
        }

        return this;
    }

    private Field getField(String fieldName) throws NoSuchFieldException {
        Map<String, Field> fieldPool = clsFieldPool.get(cls);

        if (fieldPool == null) {
            fieldPool = new ConcurrentHashMap<>();
            clsFieldPool.put(cls, fieldPool);
        }

        Field field = fieldPool.get(fieldName);

        if (field == null) {
            field = cls.getField(fieldName);
            fieldPool.put(fieldName, field);
        }

        return field;
    }

    private Constructor<T> getDeclaredConstructor(final Class<T> cls, final Class<?>[] argTypes) throws SecurityException {
        Map<Wrapper<Class<?>[]>, Constructor<?>> constructorPool = clsConstructorPool.get(cls);

        if (constructorPool == null) {
            constructorPool = new ConcurrentHashMap<>();
            clsConstructorPool.put(cls, constructorPool);
        }

        final Wrapper<Class<?>[]> key = Wrapper.of(argTypes);
        Constructor<?> result = constructorPool.get(key);

        if (result == null) {
            try {
                result = cls.getDeclaredConstructor(argTypes);
            } catch (NoSuchMethodException e) {
                for (Constructor<?> constructor : cls.getDeclaredConstructors()) {
                    final Class<?>[] paramTypes = constructor.getParameterTypes();

                    if (paramTypes != null && paramTypes.length == argTypes.length) {
                        for (int i = 0, len = paramTypes.length; i < len; i++) {
                            if (argTypes[i] == null || paramTypes[i].isAssignableFrom(argTypes[i]) || wrap(paramTypes[i]).isAssignableFrom(wrap(argTypes[i]))) {
                                if (i == len - 1) {
                                    result = constructor;
                                }
                            }
                        }
                    }

                    if (result != null) {
                        break;
                    }
                }
            }

            if (result == null) {
                throw new RuntimeException("No constructor found with parameter types: " + N.toString(argTypes));
            }

            constructorPool.put(key, result);
        }

        return (Constructor<T>) result;
    }

    private Method getDeclaredMethod(final Class<?> cls, final String methodName, final Class<?>[] argTypes) throws SecurityException {
        Map<String, Map<Wrapper<Class<?>[]>, Method>> methodPool = clsMethodPool.get(cls);

        if (methodPool == null) {
            methodPool = new ConcurrentHashMap<>();
            clsMethodPool.put(cls, methodPool);
        }

        Map<Wrapper<Class<?>[]>, Method> argsMethodPool = methodPool.get(methodName);

        if (argsMethodPool == null) {
            argsMethodPool = new ConcurrentHashMap<>();
            methodPool.put(methodName, argsMethodPool);
        }

        final Wrapper<Class<?>[]> key = Wrapper.of(argTypes);
        Method result = argsMethodPool.get(key);

        if (result == null) {
            try {
                result = cls.getDeclaredMethod(methodName, argTypes);
            } catch (NoSuchMethodException e) {
                for (Method method : cls.getDeclaredMethods()) {
                    final Class<?>[] paramTypes = method.getParameterTypes();

                    if (method.getName().equals(methodName) && (paramTypes != null && paramTypes.length == argTypes.length)) {
                        for (int i = 0, len = paramTypes.length; i < len; i++) {
                            if (argTypes[i] == null || paramTypes[i].isAssignableFrom(argTypes[i]) || wrap(paramTypes[i]).isAssignableFrom(wrap(argTypes[i]))) {
                                if (i == len - 1) {
                                    result = method;
                                }
                            }
                        }
                    }

                    if (result != null) {
                        break;
                    }
                }

                if (result == null) {
                    throw new RuntimeException("No method found by name: " + methodName + " with parameter types: " + N.toString(argTypes));
                }

                argsMethodPool.put(key, result);
            }
        }

        return result;
    }

    private Class<?>[] getTypes(Object... values) {
        if (N.isNullOrEmpty(values)) {
            return EMPTY_CLASSES;
        }

        final Class<?>[] result = new Class[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] == null ? null : values[i].getClass();
        }

        return result;
    }

    private Class<?> wrap(final Class<?> cls) {
        return Primitives.isPrimitiveType(cls) ? Primitives.wrap(cls) : cls;
    }
}
