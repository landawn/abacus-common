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

import java.lang.reflect.Field;

import com.esotericsoftware.reflectasm.ConstructorAccess;
import com.esotericsoftware.reflectasm.FieldAccess;
import com.esotericsoftware.reflectasm.MethodAccess;
import com.landawn.abacus.annotation.MayReturnNull;

/**
 * High-performance reflection utility class that leverages ASM (bytecode manipulation) for faster
 * field access, method invocation, and object construction compared to standard Java reflection.
 * This class provides a fluent API, but its generic result casts are unchecked and callers remain
 * responsible for requesting the actual field or method result type.
 *
 * <p><b>Internal adapter.</b> This class is package-private: it exists to give {@link Reflection} a
 * ReflectASM-backed fast path and is not part of the public API. Application code should use
 * {@link Reflection}, which carries the equivalent public entry points. Its {@code get}/{@code set} fall
 * back to standard reflection whenever the generated accessor rejects the field or fails to link, so a
 * field ReflectASM cannot reach is still read and written; its {@code invoke}/{@code call} fall back only
 * for members {@code canInvoke} reports as unresolvable, so a member that resolves but that the generated
 * accessor is then refused access to propagates an {@link IllegalAccessError} instead. The examples below
 * illustrate this internal API and compile only from within {@code com.landawn.abacus.util}.</p>
 *
 * <p>ReflectASM uses the EsotericSoftware's ReflectASM library internally, which generates bytecode
 * at runtime to create direct field accessors and method invokers, avoiding the overhead of Java's
 * built-in reflection API. This results in significantly better performance for repeated operations.</p>
 *
 * <p>Generated accessors are cached per target class. Actual performance depends on the JVM,
 * access pattern, and warm-up state; callers should benchmark their own workload.</p>
 *
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Cannot access private fields or methods</li>
 *   <li>Can read, but cannot assign, final fields; an assignment attempt throws
 *       {@link IllegalAccessError}, which is an {@code Error} rather than a {@code RuntimeException}</li>
 *   <li>Requires public no-argument constructor for instantiation</li>
 *   <li>Initial setup has overhead due to bytecode generation</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Create instance and access fields/methods
 * public class Person {
 *     public String name;
 *     public int age;
 *
 *     public void sayHello() {
 *         System.out.println("Hello, I'm " + name);
 *     }
 *
 *     public int calculateBirthYear(int currentYear) {
 *         return currentYear - age;
 *     }
 *
 *     public static void main(String[] args) {
 *         // Using ReflectASM
 *         Person person = ReflectASM.on(Person.class).newInstance().instance();
 *
 *         // Set fields
 *         ReflectASM.on(person)
 *             .set("name", "John")
 *             .set("age", 30);
 *
 *         // Get field value
 *         String name = ReflectASM.on(person).get("name");
 *
 *         // Invoke methods
 *         ReflectASM.on(person).call("sayHello");
 *         int birthYear = ReflectASM.on(person).invoke("calculateBirthYear", 2024);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of the target class or object being reflected upon
 * @see FieldAccess
 * @see MethodAccess
 * @see ConstructorAccess
 */
final class ReflectASM<T> {

    /** Shared empty parameter-type array used for no-argument lookups. */
    @SuppressWarnings("rawtypes")
    static final Class[] EMPTY_CLASSES = {};

    private static final ClassValue<FieldAccess> FIELD_ACCESS_CACHE = new ClassValue<>() {
        @Override
        protected FieldAccess computeValue(final Class<?> type) {
            return FieldAccess.get(type);
        }
    };

    private static final ClassValue<ConstructorAccess<?>> CONSTRUCTOR_ACCESS_CACHE = new ClassValue<>() {
        @Override
        protected ConstructorAccess<?> computeValue(final Class<?> type) {
            return ConstructorAccess.get(type);
        }
    };

    private static final ClassValue<MethodAccess> METHOD_ACCESS_CACHE = new ClassValue<>() {
        @Override
        protected MethodAccess computeValue(final Class<?> type) {
            return MethodAccess.get(type);
        }
    };

    private final Class<T> cls;

    private final T instance;

    /**
     * Creates a wrapper for a target class and, optionally, an existing instance.
     *
     * @param cls the target class
     * @param instance the wrapped instance, or {@code null} when operating on the class
     */
    ReflectASM(final Class<T> cls, final T instance) {
        this.cls = cls;
        this.instance = instance;
    }

    /**
     * Creates a ReflectASM instance for the specified class name.
     * The class is loaded via {@link ClassUtil#forName(String)}.
     *
     * <p>This method is useful when you have the class name as a string, such as when
     * working with configuration files or dynamic class loading scenarios.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Load and instantiate a class by name
     * ReflectASM<?> reflect = ReflectASM.on("com.example.MyClass");
     * Object instance = reflect.newInstance().instance();
     *
     * // With known type
     * ReflectASM<MyClass> typedReflect = ReflectASM.on("com.example.MyClass");
     * MyClass typedInstance = typedReflect.newInstance().instance();
     * }</pre>
     *
     * @param <T> the type of the class
     * @param clsName the fully qualified name of the class; must not be {@code null}
     * @return a ReflectASM instance for the specified class
     * @throws IllegalArgumentException if {@code clsName} is {@code null}, or if the class with the given name cannot be located.
     * @see ClassUtil#forName(String)
     */
    public static <T> ReflectASM<T> on(final String clsName) throws IllegalArgumentException {
        N.checkArgNotNull(clsName, cs.clsName);

        return on(ClassUtil.forName(clsName));
    }

    /**
     * Creates a ReflectASM instance for the specified class.
     * This is used when you want to create new instances of a class.
     *
     * <p>The returned ReflectASM instance can be used to create new instances of the class
     * using the {@link #newInstance()} method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create new instance
     * Person person = ReflectASM.on(Person.class).newInstance().instance();
     *
     * // Chain operations on new instance
     * Person configured = ReflectASM.on(Person.class)
     *     .newInstance()
     *     .set("name", "Alice")
     *     .set("age", 25)
     *     .instance();
     * }</pre>
     *
     * @param <T> the type of the class
     * @param cls the {@code Class} object representing the type; must not be {@code null}
     * @return a ReflectASM instance for the specified class
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    public static <T> ReflectASM<T> on(final Class<T> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        return new ReflectASM<>(cls, null);
    }

    /**
     * Creates a ReflectASM instance for the specified target object.
     * This is used when you want to perform reflection operations on an existing instance.
     *
     * <p>The class type is inferred from the target object's runtime type. This method
     * provides a convenient way to start reflection operations on an existing object.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person();
     *
     * // Fluent field access
     * ReflectASM.on(person)
     *     .set("name", "Bob")
     *     .set("age", 35)
     *     .call("updateProfile");
     *
     * // Get field values
     * String name = ReflectASM.on(person).get("name");
     * Integer age = ReflectASM.on(person).get("age");
     *
     * // Method invocation with return value
     * String info = ReflectASM.on(person).invoke("getInfo");
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param instance the object instance to perform reflection on; must not be {@code null}
     * @return a ReflectASM instance for the specified object
     * @throws IllegalArgumentException if {@code instance} is {@code null}.
     */
    public static <T> ReflectASM<T> on(final T instance) throws IllegalArgumentException {
        N.checkArgNotNull(instance, cs.instance);

        return new ReflectASM<>((Class<T>) instance.getClass(), instance);
    }

    /**
     * Creates a new instance of the class associated with this ReflectASM object.
     * The class must have a public no-argument constructor.
     *
     * <p>This method uses bytecode generation for faster instantiation compared to
     * standard reflection. The generated constructor accessor is cached for reuse.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple instantiation
     * Person person = ReflectASM.on(Person.class).newInstance().instance();
     *
     * // Chained initialization
     * Person initialized = ReflectASM.on(Person.class)
     *     .newInstance()
     *     .set("name", "Charlie")
     *     .set("email", "charlie@example.com")
     *     .call("validate")
     *     .instance();
     *
     * // Multiple instances
     * ReflectASM<Person> personReflect = ReflectASM.on(Person.class);
     * Person person1 = personReflect.newInstance().instance();
     * Person person2 = personReflect.newInstance().instance();
     * }</pre>
     *
     * <p>The generated accessor invokes the target constructor directly. Target constructor
     * exceptions propagate unchanged, including checked exceptions that are not declared by the accessor API.</p>
     *
     * @return a new ReflectASM instance wrapping the newly created object
     * @throws RuntimeException if constructor access cannot be created, no accessible nonprivate no-argument constructor exists, or the constructor throws an unchecked exception
     * @throws InstantiationError if the represented class is abstract
     */
    public ReflectASM<T> newInstance() throws RuntimeException, InstantiationError { //NOSONAR
        return new ReflectASM<>(cls, getConstructorAccess(cls).newInstance());
    }

    /**
     * Returns the target instance wrapped by this ReflectASM object.
     * Returns {@code null} if this ReflectASM was created from a class rather than an instance.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MyClass created = ReflectASM.on(MyClass.class).newInstance().instance();
     * MyClass existing = ReflectASM.on(created).instance();
     * }</pre>
     *
     * @return the target instance, or {@code null} if reflecting on a class
     */
    @MayReturnNull
    public T instance() {
        return instance;
    }

    /**
     * Retrieves the value of a field from the target object.
     * The field must be non-private and non-static.
     *
     * <p>This method uses bytecode-generated accessors for high-performance field access.
     * The returned value is cast to the specified type parameter.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class Product {
     *     public String name;
     *     public double price;
     *     public List<String> tags;
     *
     *     public static void main(String[] args) {
     *         Product product = new Product();
     *         product.name = "Laptop";
     *         product.price = 999.99;
     *         product.tags = Arrays.asList("electronics", "computers");
     *
     *         // Get field values with automatic casting
     *         String name = ReflectASM.on(product).get("name");
     *         Double price = ReflectASM.on(product).get("price");
     *         List<String> tags = ReflectASM.on(product).get("tags");
     *
     *         // Can also get as Object
     *         Object priceObj = ReflectASM.on(product).get("price");
     *     }
     * }
     * }</pre>
     *
     * <p>Note: this method operates on the wrapped instance. If this {@code ReflectASM} was created
     * from a class via {@link #on(Class)} or {@link #on(String)} without calling {@link #newInstance()},
     * the wrapped instance is {@code null} and a {@code NullPointerException} will be thrown.</p>
     *
     * <p><b>Warning:</b> a {@code private} field that hides a non-private field of the same name in a
     * superclass is invisible to the underlying {@code FieldAccess} table, so the name lookup resolves to
     * the inherited field: this method then reads that superclass field and does not throw. Use
     * {@link Reflection#get(String)}, or the {@code Field} overload of this method, when fields may hide
     * one another. (An unshadowed private field, a static field or an absent field does throw, as
     * documented below.)</p>
     *
     * <p>An incompatible assignment of the generic return value can cause {@link ClassCastException}
     * in the caller after this method returns.</p>
     *
     * @param <V> the expected type of the field value
     * @param fieldName the name of the field to access; must not be {@code null}
     * @return the value of the field, cast to type {@code V}
     * @throws IllegalArgumentException if {@code fieldName} is {@code null}
     * @throws RuntimeException if the field does not exist or cannot be accessed (e.g., it is
     *         private or static)
     * @throws NullPointerException if a field is accessed without an instance
     */
    public <V> V get(final String fieldName) throws IllegalArgumentException, RuntimeException, NullPointerException {
        N.checkArgNotNull(fieldName, cs.fieldName);

        final FieldAccess fieldAccess = getFieldAccess();

        return (V) fieldAccess.get(instance, fieldName);
    }

    /**
     * Reads the exact field selected by reflection, preserving its declaring class when a
     * subclass hides an inherited field with the same name.
     *
     * @param <V> the expected field value type
     * @param field the field to read
     * @return the field value, cast to {@code V}
     * @throws RuntimeException if field access cannot be created or the specified field cannot be accessed
     * @throws NullPointerException if an instance field is accessed without an instance
     */
    <V> V get(final Field field) throws RuntimeException, NullPointerException {
        final FieldAccess fieldAccess = getFieldAccess();

        return (V) fieldAccess.get(instance, fieldAccess.getIndex(field));
    }

    /**
     * Sets the value of a field on the target object.
     * The field must be non-private, non-static, and non-final.
     *
     * <p>This method returns the same ReflectASM instance, allowing for method chaining
     * to set multiple fields in a fluent manner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class Configuration {
     *     public String host;
     *     public int port;
     *     public boolean ssl;
     *     public Map<String, String> properties;
     *
     *     public static void main(String[] args) {
     *         Configuration config = new Configuration();
     *
     *         // Chain multiple field assignments
     *         ReflectASM.on(config)
     *             .set("host", "api.example.com")
     *             .set("port", 443)
     *             .set("ssl", true)
     *             .set("properties", new HashMap<>());
     *
     *         // Can be used after instantiation
     *         Configuration newConfig = ReflectASM.on(Configuration.class)
     *             .newInstance()
     *             .set("host", "localhost")
     *             .set("port", 8080)
     *             .instance();
     *     }
     * }
     * }</pre>
     *
     * <p>Note: this method operates on the wrapped instance. If this {@code ReflectASM} was created
     * from a class via {@link #on(Class)} or {@link #on(String)} without calling {@link #newInstance()},
     * the wrapped instance is {@code null} and a {@code NullPointerException} will be thrown.</p>
     *
     * <p><b>Warning:</b> a {@code private} field that hides a non-private field of the same name in a
     * superclass is invisible to the underlying {@code FieldAccess} table, so the name lookup resolves to
     * the inherited field: this method then writes that superclass field, leaves the subclass field
     * untouched and does not throw. Use {@link Reflection#set(String, Object)}, or the {@code Field}
     * overload of this method, when fields may hide one another. (An unshadowed private field, a static
     * field or an absent field does throw, as documented below.)</p>
     *
     * @param fieldName the name of the field to set; must not be {@code null}
     * @param value the value to assign to the field; may be {@code null} for reference-type fields
     * @return this {@code ReflectASM} instance for method chaining
     * @throws IllegalArgumentException if {@code fieldName} is {@code null}
     * @throws RuntimeException if the field does not exist, is static, or cannot otherwise be accessed
     * @throws ClassCastException if {@code value} is not assignable to the field type or its primitive wrapper type
     * @throws NullPointerException if a field is accessed without an instance, or {@code value} is {@code null} for a primitive field
     * @throws IllegalAccessError if the field is {@code final}: the generated accessor's {@code putfield}
     *         is rejected when it is resolved, and that is an {@code Error}, not a
     *         {@code RuntimeException}. The accessor itself stays usable, so a later read of the same
     *         field through the same instance still succeeds
     */
    public ReflectASM<T> set(final String fieldName, final Object value)
            throws IllegalArgumentException, RuntimeException, ClassCastException, NullPointerException, IllegalAccessError {
        N.checkArgNotNull(fieldName, cs.fieldName);

        final FieldAccess fieldAccess = getFieldAccess();

        fieldAccess.set(instance, fieldName, value);

        return this;
    }

    /**
     * Writes the exact field selected by reflection, preserving its declaring class when a
     * subclass hides an inherited field with the same name.
     *
     * @param field the field to write
     * @param value the value to assign
     * @throws RuntimeException if field access cannot be created or the specified field cannot be accessed
     * @throws ClassCastException if {@code value} is not assignable to the field type or its primitive wrapper type
     * @throws NullPointerException if a field is accessed without an instance, or {@code value} is {@code null} for a primitive field
     * @throws IllegalAccessError if the field is final and the generated write is rejected
     */
    void set(final Field field, final Object value) throws RuntimeException, ClassCastException, NullPointerException, IllegalAccessError {
        final FieldAccess fieldAccess = getFieldAccess();

        fieldAccess.set(instance, fieldAccess.getIndex(field), value);
    }

    /**
     * Invokes a method on the target object and returns the result.
     * The method must be non-private and the correct number and types of arguments must be provided.
     *
     * <p>This method uses bytecode generation for high-performance method invocation.
     * If the method returns void, {@code null} is returned. Otherwise, the return value is
     * automatically cast to the specified type parameter.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class Calculator {
     *     public int add(int a, int b) {
     *         return a + b;
     *     }
     *
     *     public String format(String template, Object... args) {
     *         return String.format(template, args);
     *     }
     *
     *     public List<Integer> range(int start, int end) {
     *         return IntStream.range(start, end).boxed().collect(Collectors.toList());
     *     }
     *
     *     public String description() {
     *         return "calculator";
     *     }
     *
     *     public static void main(String[] args) {
     *         Calculator calc = new Calculator();
     *
     *         // Invoke with return value
     *         Integer sum = ReflectASM.on(calc).invoke("add", 5, 3);
     *         String formatted = ReflectASM.on(calc).invoke("format", "Result: %d", new Object[] { sum });
     *         List<Integer> numbers = ReflectASM.on(calc).invoke("range", 1, 10);
     *
     *         // Method with no arguments
     *         String result = ReflectASM.on(calc).invoke("description");
     *     }
     * }
     * }</pre>
     *
     * <p>Note: this method operates on the wrapped instance. If this {@code ReflectASM} was created
     * from a class via {@link #on(Class)} or {@link #on(String)} without calling {@link #newInstance()},
     * the wrapped instance is {@code null} and a {@code NullPointerException} will be thrown.</p>
     *
     * @param <V> the expected return type of the method
     * @param methodName the name of the method to invoke; must not be {@code null}
     * @param args the arguments to pass to the method; may be omitted for no-argument methods
     * @return the return value of the method cast to {@code V}, or {@code null} for {@code void} methods
     * @throws IllegalArgumentException if {@code methodName} is {@code null}
     * @throws RuntimeException if the method does not exist, is private, or throws an exception
     *         during invocation; checked target exceptions are wrapped, and an
     *         {@link InterruptedException} also restores the current thread's interrupt status
     * @throws ClassCastException if an argument is not compatible with the parameter type selected by method name and arity
     * @throws NullPointerException if an instance method is invoked without an instance, or a primitive parameter receives {@code null}
     */
    public final <V> V invoke(final String methodName, final Object... args)
            throws IllegalArgumentException, RuntimeException, ClassCastException, NullPointerException {
        N.checkArgNotNull(methodName, cs.methodName);

        final MethodAccess methodAccess = getMethodAccess(cls);

        try {
            return (V) methodAccess.invoke(instance, methodName, args);
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            // Generated invocation can propagate checked exceptions without a throws declaration.
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Invokes a method on the target object without returning the result.
     * This is a convenience method for void methods or when the return value is not needed.
     *
     * <p>This method returns the same ReflectASM instance, allowing for method chaining
     * to perform multiple operations in sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * public class Service {
     *     public int timeout;
     *     public int maxRetries;
     *
     *     public void initialize() {}
     *     public void configure(String config) {}
     *     public void start() {}
     *     public void process(String data) {}
     *
     *     public static void main(String[] args) {
     *         Service service = new Service();
     *
     *         // Chain multiple method calls
     *         ReflectASM.on(service)
     *             .call("initialize")
     *             .call("configure", "production.conf")
     *             .call("start");
     *
     *         // Combined with field access
     *         ReflectASM.on(service)
     *             .set("timeout", 5000)
     *             .call("initialize")
     *             .set("maxRetries", 3)
     *             .call("start");
     *
     *         // Process data
     *         String jsonData = "{}";
     *         ReflectASM.on(service).call("process", jsonData);
     *     }
     * }
     * }</pre>
     *
     * <p>Note: this method operates on the wrapped instance. If this {@code ReflectASM} was created
     * from a class via {@link #on(Class)} or {@link #on(String)} without calling {@link #newInstance()},
     * the wrapped instance is {@code null} and a {@code NullPointerException} will be thrown.</p>
     *
     * @param methodName the name of the method to invoke; must not be {@code null}
     * @param args the arguments to pass to the method; may be omitted for no-argument methods
     * @return this {@code ReflectASM} instance for method chaining
     * @throws IllegalArgumentException if {@code methodName} is {@code null}
     * @throws RuntimeException if the method does not exist, is private, or throws an exception
     *         during invocation
     * @throws ClassCastException if an argument is incompatible with the parameter type selected by method name and arity
     * @throws NullPointerException if an instance method is invoked without an instance, or a primitive parameter receives {@code null}
     */
    public final ReflectASM<T> call(final String methodName, final Object... args)
            throws IllegalArgumentException, RuntimeException, ClassCastException, NullPointerException {
        invoke(methodName, args);

        return this;
    }

    /**
     * Checks whether ReflectASM can resolve a method with the specified name and the specified
     * number of arguments. Only non-private methods declared in the class or its superclasses
     * (excluding {@code java.lang.Object}) are visible to ReflectASM, and {@link MethodAccess}
     * resolves the convenience {@code invoke(Object, String, Object...)} call by method name and
     * argument count.
     *
     * <p>This performs the same resolution check without invoking anything, so callers can fall
     * back to standard reflection safely; catching the resolution exception around an actual
     * invocation would be unsafe because the invoked method itself may throw
     * {@code IllegalArgumentException} after side effects have already happened.</p>
     *
     * @param methodName the name of the method to look up
     * @param args the arguments the method would be invoked with; both the count and the runtime
     *        types are used to make sure ReflectASM would dispatch to the same overload that
     *        standard, type-aware reflection would
     * @return {@code true} if exactly one visible method matches the name and argument count and its
     *         parameter types are compatible with the given arguments, {@code false} otherwise
     */
    boolean canInvoke(final String methodName, final Object... args) {
        try {
            final MethodAccess methodAccess = getMethodAccess(cls);
            final int argCount = args == null ? 0 : args.length;
            final String[] methodNames = methodAccess.getMethodNames();
            final Class<?>[][] parameterTypes = methodAccess.getParameterTypes();

            int matchCount = 0;
            int matchIndex = -1;

            for (int i = 0, n = methodNames.length; i < n; i++) {
                if (methodNames[i].equals(methodName) && parameterTypes[i].length == argCount) {
                    matchCount++;
                    matchIndex = i;
                }
            }

            return matchCount == 1 && parametersAssignable(parameterTypes[matchIndex], args);
        } catch (final RuntimeException | LinkageError e) {
            // MethodAccess generation failed for this class; let the caller use standard reflection.
            // LinkageError is included because an incomplete ReflectASM deployment (for example one whose
            // shaded com.esotericsoftware.asm classes are missing) makes the generation throw
            // NoClassDefFoundError, which must disable the optional fast path rather than reach the caller.
            // This probe resolves nothing but the method table, so it runs no application code.
            return false;
        }
    }

    private static boolean parametersAssignable(final Class<?>[] paramTypes, final Object[] args) {
        for (int i = 0, len = paramTypes.length; i < len; i++) {
            final Object arg = args[i];

            if (arg == null) {
                // A null argument fits any reference parameter; a primitive parameter cannot accept
                // null, so defer to standard reflection in that case.
                if (paramTypes[i].isPrimitive()) {
                    return false;
                }
            } else if (!wrap(paramTypes[i]).isAssignableFrom(arg.getClass())) {
                return false;
            }
        }

        return true;
    }

    private static Class<?> wrap(final Class<?> cls) {
        return ClassUtil.isPrimitiveType(cls) ? ClassUtil.wrap(cls) : cls;
    }

    private FieldAccess getFieldAccess() {
        return FIELD_ACCESS_CACHE.get(cls);
    }

    /**
     * Returns the constructor access for the specified class.
     *
     * @param cls the class for which to get the constructor access
     * @return the cached or newly created ConstructorAccess instance for the specified class
     * @throws SecurityException if a security manager denies access
     */
    private ConstructorAccess<T> getConstructorAccess(final Class<T> cls) throws SecurityException {
        return (ConstructorAccess<T>) CONSTRUCTOR_ACCESS_CACHE.get(cls);
    }

    private MethodAccess getMethodAccess(final Class<?> cls) {
        return METHOD_ACCESS_CACHE.get(cls);
    }
}
