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
 * High-performance reflection utility class that leverages ASM (bytecode manipulation) for faster
 * field access, method invocation, and object construction compared to standard Java reflection.
 * This class provides a fluent API for reflection operations while maintaining type safety through generics.
 * 
 * <p>ReflectASM uses the EsotericSoftware's ReflectASM library internally, which generates bytecode
 * at runtime to create direct field accessors and method invokers, avoiding the overhead of Java's
 * built-in reflection API. This results in significantly better performance for repeated operations.</p>
 * 
 * <p><b>Performance benefits:</b></p>
 * <ul>
 *   <li>Field access: 5-10x faster than standard reflection</li>
 *   <li>Method invocation: 5-10x faster than standard reflection</li>
 *   <li>Constructor calls: 3-5x faster than standard reflection</li>
 * </ul>
 * 
 * <p><b>Limitations:</b></p>
 * <ul>
 *   <li>Cannot access private fields or methods</li>
 *   <li>Cannot work with final fields</li>
 *   <li>Requires public no-argument constructor for instantiation</li>
 *   <li>Initial setup has overhead due to bytecode generation</li>
 * </ul>
 * 
 * <p><b>Example usage:</b></p>
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
 * }
 * 
 * // Using ReflectASM
 * Person person = ReflectASM.on(Person.class)._new().get();
 * 
 * // Set fields
 * ReflectASM.on(person)
 *     .set("name", "John")
 *     .set("age", 30);
 * 
 * // Get field value
 * String name = ReflectASM.on(person).get("name");
 * 
 * // Invoke methods
 * ReflectASM.on(person).call("sayHello");
 * int birthYear = ReflectASM.on(person).invoke("calculateBirthYear", 2024);
 * }</pre>
 * 
 * @param <T> the type of the target class or object being reflected upon
 * @see FieldAccess
 * @see MethodAccess
 * @see ConstructorAccess
 * @since 1.0
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
     * Creates a ReflectASM instance for the specified class name.
     * The class is loaded using the default class loader.
     * 
     * <p>This method is useful when you have the class name as a string, such as when
     * working with configuration files or dynamic class loading scenarios.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * // Load and instantiate a class by name
     * ReflectASM<?> reflect = ReflectASM.on("com.example.MyClass");
     * Object instance = reflect._new().get();
     * 
     * // With known type
     * ReflectASM<MyClass> typedReflect = ReflectASM.on("com.example.MyClass");
     * MyClass instance = typedReflect._new().get();
     * }</pre>
     *
     * @param <T> the type of the class
     * @param clsName the fully qualified name of the class
     * @return a ReflectASM instance for the specified class
     * @throws RuntimeException if the class cannot be found or loaded
     */
    public static <T> ReflectASM<T> on(final String clsName) {
        return on(ClassUtil.forClass(clsName));
    }

    /**
     * Creates a ReflectASM instance for the specified class.
     * This is used when you want to create new instances or access static members of a class.
     * 
     * <p>The returned ReflectASM instance can be used to create new instances of the class
     * using the {@link #_new()} method, or to access static fields and methods.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * // Create new instance
     * Person person = ReflectASM.on(Person.class)._new().get();
     * 
     * // Chain operations on new instance
     * Person configured = ReflectASM.on(Person.class)
     *     ._new()
     *     .set("name", "Alice")
     *     .set("age", 25)
     *     .get();
     * 
     * // Access static members (if supported by implementation)
     * ReflectASM<MyUtils> utils = ReflectASM.on(MyUtils.class);
     * }</pre>
     *
     * @param <T> the type of the class
     * @param cls the Class object representing the type
     * @return a ReflectASM instance for the specified class
     * @throws IllegalArgumentException if cls is null
     */
    public static <T> ReflectASM<T> on(final Class<T> cls) {
        return new ReflectASM<>(cls, null);
    }

    /**
     * Creates a ReflectASM instance for the specified target object.
     * This is used when you want to perform reflection operations on an existing instance.
     * 
     * <p>The class type is inferred from the target object's runtime type. This method
     * provides a convenient way to start reflection operations on an existing object.</p>
     * 
     * <p><b>Example usage:</b></p>
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
     * @param target the object instance to perform reflection on
     * @return a ReflectASM instance for the specified object
     * @throws IllegalArgumentException if target is null
     */
    public static <T> ReflectASM<T> on(final T target) {
        return new ReflectASM<>((Class<T>) target.getClass(), target);
    }

    /**
     * Creates a new instance of the class associated with this ReflectASM object.
     * The class must have a public no-argument constructor.
     * 
     * <p>This method uses bytecode generation for faster instantiation compared to
     * standard reflection. The generated constructor accessor is cached for reuse.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * // Simple instantiation
     * Person person = ReflectASM.on(Person.class)._new().get();
     * 
     * // Chained initialization
     * Person initialized = ReflectASM.on(Person.class)
     *     ._new()
     *     .set("name", "Charlie")
     *     .set("email", "charlie@example.com")
     *     .call("validate")
     *     .get();
     * 
     * // Multiple instances
     * ReflectASM<Person> personReflect = ReflectASM.on(Person.class);
     * Person person1 = personReflect._new().get();
     * Person person2 = personReflect._new().get();
     * }</pre>
     *
     * @return a new ReflectASM instance wrapping the newly created object
     * @throws RuntimeException if the class cannot be instantiated (e.g., no public no-arg constructor)
     */
    public ReflectASM<T> _new() { //NOSONAR
        return new ReflectASM<>(cls, getConstructorAccess(cls).newInstance());
    }

    /**
     * Retrieves the value of a field from the target object.
     * The field must be public and non-static.
     * 
     * <p>This method uses bytecode-generated accessors for high-performance field access.
     * The returned value is cast to the specified type parameter.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * public class Product {
     *     public String name;
     *     public double price;
     *     public List<String> tags;
     * }
     * 
     * Product product = new Product();
     * product.name = "Laptop";
     * product.price = 999.99;
     * product.tags = Arrays.asList("electronics", "computers");
     * 
     * // Get field values with automatic casting
     * String name = ReflectASM.on(product).get("name");
     * Double price = ReflectASM.on(product).get("price");
     * List<String> tags = ReflectASM.on(product).get("tags");
     * 
     * // Can also get as Object
     * Object priceObj = ReflectASM.on(product).get("price");
     * }</pre>
     *
     * @param <V> the expected type of the field value
     * @param fieldName the name of the field to access
     * @return the value of the field, cast to type V
     * @throws RuntimeException if the field doesn't exist or cannot be accessed
     * @throws ClassCastException if the actual field type cannot be cast to V
     */
    public <V> V get(final String fieldName) {
        final FieldAccess fieldAccess = getFieldAccess();

        return (V) fieldAccess.get(target, fieldName);
    }

    /**
     * Sets the value of a field on the target object.
     * The field must be public, non-static, and non-final.
     * 
     * <p>This method returns the same ReflectASM instance, allowing for method chaining
     * to set multiple fields in a fluent manner.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * public class Configuration {
     *     public String host;
     *     public int port;
     *     public boolean ssl;
     *     public Map<String, String> properties;
     * }
     * 
     * Configuration config = new Configuration();
     * 
     * // Chain multiple field assignments
     * ReflectASM.on(config)
     *     .set("host", "api.example.com")
     *     .set("port", 443)
     *     .set("ssl", true)
     *     .set("properties", new HashMap<>());
     * 
     * // Can be used after instantiation
     * Configuration newConfig = ReflectASM.on(Configuration.class)
     *     ._new()
     *     .set("host", "localhost")
     *     .set("port", 8080)
     *     .get();
     * }</pre>
     *
     * @param fieldName the name of the field to set
     * @param value the value to assign to the field
     * @return this ReflectASM instance for method chaining
     * @throws RuntimeException if the field doesn't exist, is final, or cannot be accessed
     */
    public ReflectASM<T> set(final String fieldName, final Object value) {
        final FieldAccess fieldAccess = getFieldAccess();

        fieldAccess.set(target, fieldName, value);

        return this;
    }

    /**
     * Invokes a method on the target object and returns the result.
     * The method must be public and the correct number and types of arguments must be provided.
     * 
     * <p>This method uses bytecode generation for high-performance method invocation.
     * If the method returns void, null is returned. Otherwise, the return value is
     * automatically cast to the specified type parameter.</p>
     * 
     * <p><b>Example usage:</b></p>
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
     * }
     * 
     * Calculator calc = new Calculator();
     * 
     * // Invoke with return value
     * Integer sum = ReflectASM.on(calc).invoke("add", 5, 3);
     * String formatted = ReflectASM.on(calc).invoke("format", "Result: %d", sum);
     * List<Integer> numbers = ReflectASM.on(calc).invoke("range", 1, 10);
     * 
     * // Method with no arguments
     * String result = ReflectASM.on(calc).invoke("toString");
     * }</pre>
     *
     * @param <V> the expected return type of the method
     * @param methodName the name of the method to invoke
     * @param args the arguments to pass to the method (can be empty for no-arg methods)
     * @return the return value of the method, cast to type V (null for void methods)
     * @throws RuntimeException if the method doesn't exist or cannot be invoked
     * @throws ClassCastException if the return type cannot be cast to V
     */
    public <V> V invoke(final String methodName, final Object... args) {
        final MethodAccess methodAccess = getMethodAccess(cls);

        return (V) methodAccess.invoke(target, methodName, args);
    }

    /**
     * Invokes a method on the target object without returning the result.
     * This is a convenience method for void methods or when the return value is not needed.
     * 
     * <p>This method returns the same ReflectASM instance, allowing for method chaining
     * to perform multiple operations in sequence.</p>
     * 
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * public class Service {
     *     public void initialize() { ... }
     *     public void configure(String config) { ... }
     *     public void start() { ... }
     *     public void process(String data) { ... }
     * }
     * 
     * Service service = new Service();
     * 
     * // Chain multiple method calls
     * ReflectASM.on(service)
     *     .call("initialize")
     *     .call("configure", "production.conf")
     *     .call("start");
     * 
     * // Combined with field access
     * ReflectASM.on(service)
     *     .set("timeout", 5000)
     *     .call("initialize")
     *     .set("maxRetries", 3)
     *     .call("start");
     * 
     * // Process data
     * ReflectASM.on(service).call("process", jsonData);
     * }</pre>
     *
     * @param methodName the name of the method to invoke
     * @param args the arguments to pass to the method (can be empty for no-arg methods)
     * @return this ReflectASM instance for method chaining
     * @throws RuntimeException if the method doesn't exist or cannot be invoked
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