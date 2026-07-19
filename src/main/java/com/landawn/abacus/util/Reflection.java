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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * A utility class that provides simplified reflection operations with improved performance through caching.
 * This class wraps common reflection tasks like field access, method invocation, and object instantiation
 * in an easy-to-use fluent API.
 *
 * <p>For better performance, add the <a href="https://github.com/EsotericSoftware/reflectasm">reflectasm</a>
 * library to your build path. When available, this class will automatically use ReflectASM for improved
 * reflection performance.</p>
 *
 * <p>Reflection metadata (fields, constructors, methods) is cached per class with {@link ClassValue},
 * so the cache does not prevent classes and their defining classloaders from being reclaimed.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create instance from class name
 * MyClass obj = Reflection.<MyClass>on("com.example.MyClass").newInstance().instance();
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

    static final ClassValue<Map<String, Field>> clsFieldPool = new ClassValue<>() {
        @Override
        protected Map<String, Field> computeValue(final Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    static final ClassValue<Map<Wrapper<Class<?>[]>, Constructor<?>>> clsConstructorPool = new ClassValue<>() {
        @Override
        protected Map<Wrapper<Class<?>[]>, Constructor<?>> computeValue(final Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    static final ClassValue<Map<String, Map<Wrapper<Class<?>[]>, Method>>> clsMethodPool = new ClassValue<>() {
        @Override
        protected Map<String, Map<Wrapper<Class<?>[]>, Method>> computeValue(final Class<?> type) {
            return new ConcurrentHashMap<>();
        }
    };

    private final Class<T> cls;

    private final T instance;

    private final ReflectASM<T> reflectASM;

    Reflection(final Class<T> cls, final T instance) {
        this.cls = cls;
        this.instance = instance;
        reflectASM = isReflectASMAvailable ? new ReflectASM<>(cls, instance) : null;
    }

    /**
     * Creates a Reflection instance for the specified class name.
     * The class is loaded via {@link ClassUtil#forName(String)}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reflection<MyClass> ref = Reflection.on("com.example.MyClass");
     * MyClass instance = ref.newInstance().instance();
     * }</pre>
     *
     * @param <T> the type of the class
     * @param clsName the fully qualified name of the class; must not be {@code null}
     * @return a Reflection instance for the specified class
     * @throws IllegalArgumentException if the class with the given name cannot be located
     * @throws NullPointerException if {@code clsName} is {@code null}
     * @see ClassUtil#forName(String)
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
     * String str = ref.newInstance("Hello").instance();
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
     * MyClass instance = new MyClass();
     * Reflection<MyClass> ref = Reflection.on(instance);
     * ref.set("field", "value");
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param instance the object to reflect upon, must not be {@code null}
     * @return a Reflection instance for the specified object
     * @throws NullPointerException if {@code instance} is {@code null}
     */
    public static <T> Reflection<T> on(final T instance) {
        return new Reflection<>((Class<T>) instance.getClass(), instance);
    }

    /**
     * Creates a new instance of the reflected class using its no-argument constructor.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyClass obj = Reflection.on(MyClass.class).newInstance().instance();
     * }</pre>
     *
     * @return a new Reflection instance wrapping the newly created object
     * @throws RuntimeException if the class cannot be instantiated
     */
    public Reflection<T> newInstance() { //NOSONAR
        return new Reflection<>(cls, N.newInstance(cls));
    }

    /**
     * Creates a new instance of the reflected class using a constructor that matches the given arguments.
     * The constructor is selected based on the types of the provided arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = Reflection.on(Person.class).newInstance("John", 30).instance();
     * }</pre>
     *
     * @param args the arguments to pass to the constructor
     * @return a new Reflection instance wrapping the newly created object
     * @throws RuntimeException if no matching constructor is found or instantiation fails
     */
    public Reflection<T> newInstance(final Object... args) { //NOSONAR
        if (N.isEmpty(args)) {
            return newInstance();
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
     * MyClass obj = Reflection.on(MyClass.class).newInstance().instance();
     * }</pre>
     *
     * @return the target instance, or {@code null} if reflecting on a class
     */
    @MayReturnNull
    public T instance() {
        return instance;
    }

    /**
     * Returns the value of the specified field from the target instance.
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
            try {
                return reflectASM.get(fieldName);
            } catch (final IllegalArgumentException e) {
                // ReflectASM only exposes non-private fields; fall back to reflection for private/inherited members.
            }
        }

        try {
            final Field field = getField(fieldName);
            ClassUtil.setAccessibleQuietly(field, true);

            return (V) field.get(instance);
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
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
            try {
                reflectASM.set(fieldName, value);
                return this;
            } catch (final IllegalArgumentException e) {
                // ReflectASM only exposes non-private fields; fall back to reflection for private/inherited members.
            }
        }

        try {
            final Field field = getField(fieldName);
            ClassUtil.setAccessibleQuietly(field, true);

            field.set(instance, value); //NOSONAR
        } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }

        return this;
    }

    /**
     * Invokes the specified method on the target instance with the given arguments and returns the result.
     * The method is selected based on its name and the types of the provided arguments.
     * If ReflectASM is available, it will be used for better performance; in that case the method is
     * selected by name and argument count, and methods ReflectASM cannot access (private methods,
     * methods declared by {@code Object}, interface default methods) automatically fall back to
     * standard reflection, which also searches superclasses.
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
        // ReflectASM only exposes non-private methods declared in the class or its superclasses
        // (excluding Object, and excluding interface default methods); fall back to standard
        // reflection for the members it cannot resolve. The check happens BEFORE the invocation:
        // catching the resolution exception around reflectASM.invoke(...) would be unsafe because
        // the invoked method itself may throw IllegalArgumentException after side effects.
        if (reflectASM != null && reflectASM.canInvoke(methodName, args)) {
            return reflectASM.invoke(methodName, args);
        } else {
            try {
                final Method method = getDeclaredMethod(cls, methodName, getTypes(args));
                ClassUtil.setAccessibleQuietly(method, true);

                return (V) method.invoke(instance, args);
            } catch (SecurityException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Invokes the specified method on the target instance with the given arguments without returning a result.
     * This is a convenience method for void methods or when the return value is not needed.
     * The method is selected based on its name and the types of the provided arguments.
     * If ReflectASM is available, it will be used for better performance, with the same
     * standard-reflection fallback as {@link #invoke(String, Object...)}.
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
        if (reflectASM != null && reflectASM.canInvoke(methodName, args)) {
            reflectASM.call(methodName, args);
        } else {
            // Falls back to standard reflection for private/inherited members (see invoke(String, Object...)).
            invoke(methodName, args);
        }

        return this;
    }

    /**
     * Returns the field with the specified name.
     *
     * @param fieldName the name of the field to retrieve
     * @return the Field object corresponding to the field name
     * @throws NoSuchFieldException if no field with the specified name is found
     */
    private Field getField(final String fieldName) throws NoSuchFieldException {
        final Map<String, Field> fieldPool = clsFieldPool.get(cls);

        Field field = fieldPool.get(fieldName);

        if (field == null) {
            Class<?> current = cls;

            while (current != null) {
                try {
                    field = current.getDeclaredField(fieldName);
                    break;
                } catch (final NoSuchFieldException e) {
                    current = current.getSuperclass();
                }
            }

            if (field == null) {
                throw new NoSuchFieldException(fieldName);
            }

            fieldPool.put(fieldName, field);
        }

        return field;
    }

    /**
     * Returns the declared constructor matching the specified parameter types.
     * Results are cached per class for performance. If no exact match is found,
     * invocation compatibility (including unboxing and primitive widening) is used to locate
     * the most-specific compatible constructor. If multiple unrelated overloads are
     * equally applicable, the invocation is rejected instead of depending on reflection
     * enumeration order.
     *
     * @param cls the class to search for the constructor
     * @param argTypes the array of parameter types for the constructor; individual
     *        elements may be {@code null} to match any reference type at that position
     * @return the Constructor object matching the parameter types
     * @throws SecurityException if a security manager denies access to the constructor
     * @throws RuntimeException if no compatible constructor is found
     */
    private Constructor<T> getDeclaredConstructor(final Class<T> cls, final Class<?>[] argTypes) throws SecurityException {
        final Map<Wrapper<Class<?>[]>, Constructor<?>> constructorPool = clsConstructorPool.get(cls);

        final Wrapper<Class<?>[]> key = Wrapper.of(argTypes);
        Constructor<?> result = constructorPool.get(key);

        if (result == null) {
            if (!hasNullArgType(argTypes)) {
                try {
                    result = cls.getDeclaredConstructor(argTypes);
                } catch (final NoSuchMethodException e) {
                    // Fall back to compatible constructor search below.
                }
            }

            if (result == null) {
                final List<Constructor<?>> compatibleConstructors = new ArrayList<>();

                for (final Constructor<?> constructor : cls.getDeclaredConstructors()) {
                    final Class<?>[] paramTypes = constructor.getParameterTypes();

                    //noinspection ConstantValue
                    if (paramTypes != null && paramTypes.length == argTypes.length) {
                        boolean allMatch = true;

                        for (int i = 0, len = paramTypes.length; i < len; i++) {
                            if (!isParameterCompatible(paramTypes[i], argTypes[i])) {
                                allMatch = false;
                                break;
                            }
                        }

                        if (allMatch) {
                            compatibleConstructors.add(constructor);
                        }
                    }
                }

                result = selectMostSpecific(compatibleConstructors, argTypes,
                        "constructor for " + cls.getName() + " with parameter types: " + N.toString(argTypes));
            }

            if (result == null) {
                throw new RuntimeException("No constructor found with parameter types: " + N.toString(argTypes));
            }

            constructorPool.put(key, result);
        }

        return (Constructor<T>) result;
    }

    /**
     * Returns the method matching the specified name and parameter types, searching the class
     * itself first and then its superclasses (mirroring {@link #getField(String)}), so inherited
     * and private methods are found even when ReflectASM is unavailable or cannot resolve them.
     * Results are cached per class for performance. At each level of the hierarchy an exact match
     * is tried first; failing that, invocation compatibility (including unboxing and primitive widening) is
     * used to locate a compatible method. As a last resort, {@link Class#getMethod(String, Class...)}
     * is consulted to resolve public methods that are not declared anywhere in the superclass
     * chain (e.g., interface default methods). Compatible overloads are compared across
     * the complete hierarchy and the most-specific one is selected; unrelated equally
     * applicable overloads are reported as ambiguous.
     *
     * @param cls the class to search for the method
     * @param methodName the name of the method to retrieve
     * @param argTypes the array of parameter types for the method; individual
     *        elements may be {@code null} to match any reference type at that position
     * @return the Method object matching the name and parameter types
     * @throws SecurityException if a security manager denies access to the method
     * @throws RuntimeException if no compatible method is found
     */
    private Method getDeclaredMethod(final Class<?> cls, final String methodName, final Class<?>[] argTypes) throws SecurityException {
        final Map<String, Map<Wrapper<Class<?>[]>, Method>> methodPool = clsMethodPool.get(cls);

        Map<Wrapper<Class<?>[]>, Method> argsMethodPool = methodPool.computeIfAbsent(methodName, k -> new ConcurrentHashMap<>());

        final Wrapper<Class<?>[]> key = Wrapper.of(argTypes);
        Method result = argsMethodPool.get(key);

        if (result == null) {
            Class<?> current = cls;
            final List<Method> compatibleMethods = new ArrayList<>();

            while (result == null && current != null) {
                if (!hasNullArgType(argTypes)) {
                    try {
                        result = current.getDeclaredMethod(methodName, argTypes);
                    } catch (final NoSuchMethodException e) {
                        // Fall back to compatible method search below.
                    }
                }

                if (result == null) {
                    for (final Method method : current.getDeclaredMethods()) {
                        final Class<?>[] paramTypes = method.getParameterTypes();

                        if (method.getName().equals(methodName) && paramTypes.length == argTypes.length) {
                            boolean allMatch = true;

                            for (int i = 0, len = paramTypes.length; i < len; i++) {
                                if (!isParameterCompatible(paramTypes[i], argTypes[i])) {
                                    allMatch = false;
                                    break;
                                }
                            }

                            if (allMatch) {
                                compatibleMethods.add(method);
                            }
                        }
                    }
                }

                current = current.getSuperclass();
            }

            if (result == null) {
                if (!hasNullArgType(argTypes)) {
                    try {
                        // Public methods inherited from interfaces (default methods) are not declared
                        // in any superclass; Class.getMethod() resolves them.
                        result = cls.getMethod(methodName, argTypes);
                    } catch (final NoSuchMethodException e) {
                        // ignore - handled below.
                    }
                }
            }

            if (result == null) {
                // Class#getMethods also contributes public interface/default methods, which are
                // absent from the declared-method walk above. Duplicate overridden signatures are
                // removed by selectMostSpecific while preserving the subclass declaration.
                for (final Method method : cls.getMethods()) {
                    final Class<?>[] paramTypes = method.getParameterTypes();

                    if (method.getName().equals(methodName) && paramTypes.length == argTypes.length) {
                        boolean allMatch = true;

                        for (int i = 0, len = paramTypes.length; i < len; i++) {
                            if (!isParameterCompatible(paramTypes[i], argTypes[i])) {
                                allMatch = false;
                                break;
                            }
                        }

                        if (allMatch) {
                            compatibleMethods.add(method);
                        }
                    }
                }

                result = selectMostSpecific(compatibleMethods, argTypes,
                        "method " + cls.getName() + "." + methodName + " with parameter types: " + N.toString(argTypes));
            }

            if (result == null) {
                throw new RuntimeException("No method found by name: " + methodName + " with parameter types: " + N.toString(argTypes));
            }

            argsMethodPool.put(key, result);
        }

        return result;
    }

    /**
     * Selects the unique most-specific executable from a set of compatible overloads.
     * Resolution follows the fixed-arity invocation phases relevant to runtime argument types:
     * reference widening is considered before unboxing and primitive widening. Within the selected
     * phase, parameter types are compared for specificity, including the primitive widening order.
     * Duplicate signatures (typically an override visible through both hierarchy searches)
     * retain the first executable supplied by the caller.
     */
    private <E extends Executable> E selectMostSpecific(final List<E> compatibleExecutables, final Class<?>[] argTypes, final String description) {
        if (compatibleExecutables.isEmpty()) {
            return null;
        }

        final List<E> uniqueExecutables = new ArrayList<>(compatibleExecutables.size());
        int bestPhase = Integer.MAX_VALUE;

        outer: for (final E executable : compatibleExecutables) {
            final int phase = conversionPhase(executable.getParameterTypes(), argTypes);

            if (phase > bestPhase) {
                continue;
            } else if (phase < bestPhase) {
                uniqueExecutables.clear();
                bestPhase = phase;
            }

            for (final E added : uniqueExecutables) {
                if (Arrays.equals(executable.getParameterTypes(), added.getParameterTypes())) {
                    continue outer;
                }
            }

            uniqueExecutables.add(executable);
        }

        E result = null;

        for (final E candidate : uniqueExecutables) {
            boolean dominated = false;

            for (final E other : uniqueExecutables) {
                if (candidate != other && isStrictlyMoreSpecific(other.getParameterTypes(), candidate.getParameterTypes())) {
                    dominated = true;
                    break;
                }
            }

            if (!dominated) {
                if (result != null) {
                    throw new RuntimeException("Ambiguous " + description);
                }

                result = candidate;
            }
        }

        return result;
    }

    /**
     * Returns {@code 0} for strict (reference-widening) invocation and {@code 1} when
     * unboxing and/or primitive widening is required. The supplied types have already
     * been checked for compatibility.
     */
    private int conversionPhase(final Class<?>[] paramTypes, final Class<?>[] argTypes) {
        for (int i = 0; i < paramTypes.length; i++) {
            final Class<?> argType = argTypes[i];

            if (argType != null && !isStrictInvocationCompatible(paramTypes[i], argType)) {
                return 1;
            }
        }

        return 0;
    }

    private boolean isStrictInvocationCompatible(final Class<?> paramType, final Class<?> argType) {
        if (paramType.isPrimitive()) {
            return argType.isPrimitive() && isWideningPrimitiveConversion(argType, paramType);
        }

        return !argType.isPrimitive() && paramType.isAssignableFrom(argType);
    }

    private boolean isStrictlyMoreSpecific(final Class<?>[] candidateTypes, final Class<?>[] otherTypes) {
        boolean strictlyMoreSpecific = false;

        for (int i = 0; i < candidateTypes.length; i++) {
            if (candidateTypes[i].isPrimitive() && otherTypes[i].isPrimitive()) {
                if (candidateTypes[i] == otherTypes[i]) {
                    continue;
                }

                if (!isWideningPrimitiveConversion(candidateTypes[i], otherTypes[i])) {
                    return false;
                }

                strictlyMoreSpecific = true;
                continue;
            }

            final Class<?> candidateType = wrap(candidateTypes[i]);
            final Class<?> otherType = wrap(otherTypes[i]);

            if (candidateType.equals(otherType)) {
                continue;
            }

            if (!otherType.isAssignableFrom(candidateType)) {
                return false;
            }

            strictlyMoreSpecific = true;
        }

        return strictlyMoreSpecific;
    }

    private boolean hasNullArgType(final Class<?>[] argTypes) {
        for (final Class<?> argType : argTypes) {
            if (argType == null) {
                return true;
            }
        }

        return false;
    }

    private boolean isParameterCompatible(final Class<?> paramType, final Class<?> argType) {
        if (argType == null) {
            return !paramType.isPrimitive();
        }

        if (paramType.isPrimitive()) {
            final Class<?> primitiveArgType = ClassUtil.unwrap(argType);

            return primitiveArgType.isPrimitive() && isWideningPrimitiveConversion(primitiveArgType, paramType);
        }

        return paramType.isAssignableFrom(argType) || (argType.isPrimitive() && paramType.isAssignableFrom(wrap(argType)));
    }

    /**
     * Tests the identity and widening primitive conversions accepted by reflective invocation.
     */
    private boolean isWideningPrimitiveConversion(final Class<?> sourceType, final Class<?> targetType) {
        if (sourceType == targetType) {
            return true;
        }

        if (sourceType == byte.class) {
            return targetType == short.class || targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class;
        } else if (sourceType == short.class) {
            return targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class;
        } else if (sourceType == char.class) {
            return targetType == int.class || targetType == long.class || targetType == float.class || targetType == double.class;
        } else if (sourceType == int.class) {
            return targetType == long.class || targetType == float.class || targetType == double.class;
        } else if (sourceType == long.class) {
            return targetType == float.class || targetType == double.class;
        } else if (sourceType == float.class) {
            return targetType == double.class;
        }

        return false;
    }

    /**
     * Returns an array of runtime classes corresponding to the supplied argument values.
     * A {@code null} value in {@code values} produces a {@code null} entry in the returned
     * array, which is then treated as a wildcard for non-primitive parameters when matching
     * constructors or methods.
     *
     * @param values the argument values whose types are to be extracted
     * @return an array of {@code Class} objects (may contain {@code null} entries),
     *         or {@link #EMPTY_CLASSES} if {@code values} is {@code null} or empty
     */
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
