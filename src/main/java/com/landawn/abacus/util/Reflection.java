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

import com.landawn.abacus.annotation.MayReturnNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A utility class that provides simplified reflection operations with improved performance through caching.
 * This class wraps common reflection tasks like field access, method invocation, and object instantiation
 * in an easy-to-use fluent API.
 * 
 * <p>For better performance, add the <a href="https://github.com/EsotericSoftware/reflectasm">reflectasm</a>
 * library to your build path. When available, this class will automatically use ReflectASM for improved
 * reflection performance.</p>
 * 
 * <p>All reflection metadata (fields, constructors, methods) is cached for performance optimization.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create instance from class name
 * MyClass obj = Reflection.on("com.example.MyClass")._new();
 * 
 * // Access instance fields and methods
 * Reflection.on(obj)
 *     .set("name", "John")
 *     .set("age", 30)
 *     .invoke("processData", "input");
 * 
 * // Get field value
 * String name = Reflection.on(obj).get("name");
 * }</pre>
 *
 * @param <T> the type of the target class or object being reflected upon
 */
public final class Reflection<T> {

    @SuppressWarnings("rawtypes")
    static final Class[] EMPTY_CLASSES = {};

    static final boolean isReflectASMAvailable;

    static {
        boolean tmp = true;

        try {
            ClassUtil.forName("com.esotericsoftware.reflectasm.ConstructorAccess");
            ClassUtil.forName("com.esotericsoftware.reflectasm.FieldAccess");
            ClassUtil.forName("com.esotericsoftware.reflectasm.MethodAccess");
        } catch (final Exception e) {
            tmp = false;
        }

        isReflectASMAvailable = tmp;
    }

    static final Map<Class<?>, Map<String, Field>> clsFieldPool = new ConcurrentHashMap<>();

    static final Map<Class<?>, Map<Wrapper<Class<?>[]>, Constructor<?>>> clsConstructorPool = new ConcurrentHashMap<>();

    static final Map<Class<?>, Map<String, Map<Wrapper<Class<?>[]>, Method>>> clsMethodPool = new ConcurrentHashMap<>();

    private final Class<T> cls;

    private final T target;

    private final ReflectASM<T> reflectASM;

    Reflection(final Class<T> cls, final T target) {
        this.cls = cls;
        this.target = target;
        reflectASM = isReflectASMAvailable ? new ReflectASM<>(cls, target) : null;
    }

    /**
     * Creates a Reflection instance for the specified class name.
     * The class is loaded using the current thread's context class loader.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reflection<MyClass> ref = Reflection.on("com.example.MyClass");
     * MyClass instance = ref._new();
     * }</pre>
     *
     * @param <T> the type of the class
     * @param clsName the fully qualified name of the class
     * @return a Reflection instance for the specified class
     * @throws RuntimeException if the class cannot be found
     */
    public static <T> Reflection<T> on(final String clsName) {
        return on(ClassUtil.forName(clsName));
    }

    /**
     * Creates a Reflection instance for the specified class.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reflection<String> ref = Reflection.on(String.class);
     * String str = ref._new("Hello");
     * }</pre>
     *
     * @param <T> the type of the class
     * @param cls the class to reflect upon
     * @return a Reflection instance for the specified class
     */
    public static <T> Reflection<T> on(final Class<T> cls) {
        return new Reflection<>(cls, null);
    }

    /**
     * Creates a Reflection instance for the specified target object.
     * The class is determined from the runtime type of the object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyClass obj = new MyClass();
     * Reflection<MyClass> ref = Reflection.on(obj);
     * ref.set("field", "value");
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param target the object to reflect upon
     * @return a Reflection instance for the specified object
     */
    public static <T> Reflection<T> on(final T target) {
        return new Reflection<>((Class<T>) target.getClass(), target);
    }

    /**
     * Creates a new instance of the reflected class using its no-argument constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyClass obj = Reflection.on(MyClass.class)._new();
     * }</pre>
     *
     * @return a new Reflection instance wrapping the newly created object
     * @throws RuntimeException if the class cannot be instantiated
     */
    public Reflection<T> _new() { //NOSONAR
        return new Reflection<>(cls, N.newInstance(cls));
    }

    /**
     * Creates a new instance of the reflected class using a constructor that matches the given arguments.
     * The constructor is selected based on the types of the provided arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = Reflection.on(Person.class)._new("John", 30).instance();
     * }</pre>
     *
     * @param args the arguments to pass to the constructor
     * @return a new Reflection instance wrapping the newly created object
     * @throws RuntimeException if no matching constructor is found or instantiation fails
     */
    public Reflection<T> _new(final Object... args) { //NOSONAR
        if (N.isEmpty(args)) {
            return _new();
        }

        final Constructor<T> constructor = getDeclaredConstructor(cls, getTypes(args));
        ClassUtil.setAccessibleQuietly(constructor, true);

        return new Reflection<>(cls, ClassUtil.invokeConstructor(constructor, args));
    }

    /**
     * Returns the target instance being reflected upon.
     * Returns {@code null} if this Reflection was created from a Class rather than an instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyClass obj = Reflection.on(MyClass.class)._new().instance();
     * }</pre>
     *
     * @return the target instance, or {@code null} if reflecting on a class
     */
    @MayReturnNull
    public T instance() {
        return target;
    }

    /**
     * Gets the value of the specified field from the target instance.
     * If ReflectASM is available, it will be used for better performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String name = Reflection.on(person).get("name");
     * Integer age = Reflection.on(person).get("age");
     * }</pre>
     *
     * @param <V> the value type
     * @param fieldName the name of the field to get
     * @return the value of the field
     * @throws RuntimeException if the field doesn't exist or cannot be accessed
     */
    public <V> V get(final String fieldName) {
        if (reflectASM != null) {
            return reflectASM.get(fieldName);
        } else {
            try {
                final Field field = getField(fieldName);
                ClassUtil.setAccessibleQuietly(field, true);

                return (V) field.get(target);
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Sets the value of the specified field in the target instance.
     * If ReflectASM is available, it will be used for better performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reflection.on(person)
     *     .set("name", "John")
     *     .set("age", 30)
     *     .set("active", true);
     * }</pre>
     *
     * @param fieldName the name of the field to set
     * @param value the value to set
     * @return this Reflection instance for method chaining
     * @throws RuntimeException if the field doesn't exist or cannot be accessed
     */
    public Reflection<T> set(final String fieldName, final Object value) {
        if (reflectASM != null) {
            reflectASM.set(fieldName, value);
        } else {
            try {
                final Field field = getField(fieldName);
                ClassUtil.setAccessibleQuietly(field, true);

                field.set(target, value); //NOSONAR
            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        return this;
    }

    /**
     * Invokes the specified method on the target instance with the given arguments and returns the result.
     * The method is selected based on its name and the types of the provided arguments.
     * If ReflectASM is available, it will be used for better performance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String result = Reflection.on(obj).invoke("toString");
     * Integer sum = Reflection.on(calculator).invoke("add", 5, 3);
     * }</pre>
     *
     * @param <V> the value type
     * @param methodName the name of the method to invoke
     * @param args the arguments to pass to the method
     * @return the result of the method invocation
     * @throws RuntimeException if the method doesn't exist or invocation fails
     */
    public <V> V invoke(final String methodName, final Object... args) {
        if (reflectASM != null) {
            return reflectASM.invoke(methodName, args);
        } else {
            try {
                final Method method = getDeclaredMethod(cls, methodName, getTypes(args));
                ClassUtil.setAccessibleQuietly(method, true);

                return (V) method.invoke(target, args);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Invokes the specified method on the target instance with the given arguments without returning a result.
     * This is a convenience method for void methods or when the return value is not needed.
     * The method is selected based on its name and the types of the provided arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reflection.on(logger)
     *     .call("debug", "Starting process")
     *     .call("info", "Process completed");
     * }</pre>
     *
     * @param methodName the name of the method to invoke
     * @param args the arguments to pass to the method
     * @return this Reflection instance for method chaining
     * @throws RuntimeException if the method doesn't exist or invocation fails
     */
    public Reflection<T> call(final String methodName, final Object... args) {
        if (reflectASM != null) {
            reflectASM.call(methodName, args);
        } else {
            invoke(methodName, args);
        }

        return this;
    }

    /**
     * Gets the field.
     *
     * @param fieldName the name of the field to retrieve
     * @return the Field object corresponding to the field name
     * @throws NoSuchFieldException the no such field exception
     */
    private Field getField(final String fieldName) throws NoSuchFieldException {
        Map<String, Field> fieldPool = clsFieldPool.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());

        Field field = fieldPool.get(fieldName);

        if (field == null) {
            field = cls.getField(fieldName);
            fieldPool.put(fieldName, field);
        }

        return field;
    }

    /**
     * Gets the declared constructor.
     *
     * @param cls the class to search for the constructor
     * @param argTypes the array of parameter types for the constructor
     * @return the Constructor object matching the parameter types
     * @throws SecurityException the security exception
     */
    private Constructor<T> getDeclaredConstructor(final Class<T> cls, final Class<?>[] argTypes) throws SecurityException {
        Map<Wrapper<Class<?>[]>, Constructor<?>> constructorPool = clsConstructorPool.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());

        final Wrapper<Class<?>[]> key = Wrapper.of(argTypes);
        Constructor<?> result = constructorPool.get(key);

        if (result == null) {
            try {
                result = cls.getDeclaredConstructor(argTypes);
            } catch (final NoSuchMethodException e) {
                for (final Constructor<?> constructor : cls.getDeclaredConstructors()) {
                    final Class<?>[] paramTypes = constructor.getParameterTypes();

                    //noinspection ConstantValue
                    if (paramTypes != null && paramTypes.length == argTypes.length) {
                        for (int i = 0, len = paramTypes.length; i < len; i++) {
                            if ((argTypes[i] == null || paramTypes[i].isAssignableFrom(argTypes[i]) || wrap(paramTypes[i]).isAssignableFrom(wrap(argTypes[i])))
                                    && (i == len - 1)) {
                                result = constructor;
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

    /**
     * Gets the declared method.
     *
     * @param cls the class to search for the method
     * @param methodName the name of the method to retrieve
     * @param argTypes the array of parameter types for the method
     * @return the Method object matching the name and parameter types
     * @throws SecurityException the security exception
     */
    private Method getDeclaredMethod(final Class<?> cls, final String methodName, final Class<?>[] argTypes) throws SecurityException {
        Map<String, Map<Wrapper<Class<?>[]>, Method>> methodPool = clsMethodPool.computeIfAbsent(cls, k -> new ConcurrentHashMap<>());

        Map<Wrapper<Class<?>[]>, Method> argsMethodPool = methodPool.computeIfAbsent(methodName, k -> new ConcurrentHashMap<>());

        final Wrapper<Class<?>[]> key = Wrapper.of(argTypes);
        Method result = argsMethodPool.get(key);

        if (result == null) {
            try {
                result = cls.getDeclaredMethod(methodName, argTypes);
            } catch (final NoSuchMethodException e) {
                for (final Method method : cls.getDeclaredMethods()) {
                    final Class<?>[] paramTypes = method.getParameterTypes();

                    //noinspection ConstantValue
                    if (method.getName().equals(methodName) && (paramTypes != null && paramTypes.length == argTypes.length)) {
                        for (int i = 0, len = paramTypes.length; i < len; i++) {
                            if ((argTypes[i] == null || paramTypes[i].isAssignableFrom(argTypes[i]) || wrap(paramTypes[i]).isAssignableFrom(wrap(argTypes[i])))
                                    && (i == len - 1)) {
                                result = method;
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

    private Class<?>[] getTypes(final Object... values) {
        if (N.isEmpty(values)) {
            return EMPTY_CLASSES;
        }

        final Class<?>[] result = new Class[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = values[i] == null ? null : values[i].getClass();
        }

        return result;
    }

    /**
     * Wraps a primitive type to its wrapper class if applicable.
     *
     * @param cls the class to wrap
     * @return the wrapped class if primitive, otherwise the original class
     */
    private Class<?> wrap(final Class<?> cls) {
        return ClassUtil.isPrimitiveType(cls) ? ClassUtil.wrap(cls) : cls;
    }
}
