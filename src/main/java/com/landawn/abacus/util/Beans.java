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
package com.landawn.abacus.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;

import com.landawn.abacus.annotation.DiffIgnore;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.annotation.NotNull;
import com.landawn.abacus.annotation.Record;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Builder.ComparisonBuilder;
import com.landawn.abacus.util.Fn.BiPredicates;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.stream.Stream;

/**
 * A comprehensive utility class providing an extensive collection of static methods for JavaBean operations,
 * introspection, property manipulation, and object transformation. This class serves as the primary bean
 * utility facade in the Abacus library, offering null-safe, performance-optimized operations for JavaBean
 * patterns with extensive support for reflection, type conversion, and object lifecycle management.
 *
 * <p>The {@code Beans} class is designed as a final utility class that provides a complete toolkit
 * for JavaBean processing including property access, bean-to-map conversion, map-to-bean conversion,
 * object cloning, merging, validation, and introspection. All methods are static, thread-safe, and
 * designed to handle complex object hierarchies while maintaining optimal performance through caching.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Bean Introspection:</b> Complete property discovery and analysis using reflection</li>
 *   <li><b>Property Access:</b> Dynamic getter/setter invocation with type safety</li>
 *   <li><b>Object Conversion:</b> Bean-to-Map and Map-to-Bean transformations</li>
 *   <li><b>Deep Cloning:</b> Recursive object copying with configurable depth control</li>
 *   <li><b>Bean Merging:</b> Intelligent object merging with customizable merge strategies</li>
 *   <li><b>Type Conversion:</b> Automatic type conversion between compatible types</li>
 *   <li><b>Performance Caching:</b> Extensive caching of reflection metadata for optimal performance</li>
 *   <li><b>Annotation Support:</b> Full support for JAXB, validation, and custom annotations</li>
 * </ul>
 *
 * <p><b>Core Functional Categories:</b>
 * <ul>
 *   <li><b>Bean Validation:</b> {@link #isBeanClass(Class)}, {@link #isRecordClass(Class)} for type checking</li>
 *   <li><b>Property Discovery:</b> {@link #getPropNameList(Class)}, {@link #getPropNames(Object, boolean)} for introspection</li>
 *   <li><b>Property Access:</b> {@link #getPropValue(Object, String)}, {@link #setPropValue(Object, Method, Object)} with type-safe operations</li>
 *   <li><b>Object Conversion:</b> {@link #bean2Map(Object)}, {@link #map2Bean(Map, Class)} with deep and shallow conversion options</li>
 *   <li><b>Object Lifecycle:</b> {@link #newBean(Class)}, {@link #copy(Object, Class)}, {@link #clone(Object)}, {@link #merge(Object, Object)} for object management</li>
 *   <li><b>Comparison Operations:</b> {@link #equalsByProps(Object, Object, Collection)}, {@link #compareByProps(Object, Object, Collection)} with configurable properties</li>
 *   <li><b>Transformation:</b> {@link #fill(Object)} for object manipulation</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>JavaBean Standards:</b> Full compliance with JavaBean conventions including getter/setter
 *       patterns, no-arg constructors, and serialization support</li>
 *   <li><b>Null Safety:</b> Methods handle {@code null} inputs gracefully, returning sensible
 *       defaults or empty results rather than throwing exceptions</li>
 *   <li><b>Performance First:</b> Extensive caching of reflection metadata to minimize runtime overhead</li>
 *   <li><b>Type Safety:</b> Generic methods with compile-time type checking and runtime validation</li>
 *   <li><b>Flexibility:</b> Support for various object patterns including builders, records, and entities</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Bean validation and introspection
 * boolean isBean = Beans.isBeanClass(User.class);              // Check if class follows bean pattern
 * List<String> properties = Beans.getPropNameList(User.class); // Get all property names
 * Tuple3<Class<?>, Supplier<Object>, Function<Object, Object>> builderInfo = Beans.getBuilderInfo(User.class); // Get builder info
 *
 * // Property access operations
 * User user = new User();
 * Beans.setPropValue(user, "name", "John Doe");                // Set property value
 * String name = Beans.getPropValue(user, "name");              // Get property value
 *
 * // Object creation and instantiation
 * User newUser = Beans.newBean(User.class);                    // Create new instance
 * User copied = Beans.copy(user, User.class);                  // Create copy with type conversion
 * User cloned = Beans.clone(user);                             // Deep clone existing object
 *
 * // Bean to Map conversion (various formats)
 * Map<String, Object> flatMap = Beans.bean2Map(user);                    // Map conversion
 * Map<String, Object> deepMap = Beans.deepBean2Map(user);                // Deep map conversion
 * Map<String, Object> selectedMap = Beans.bean2Map(user, Arrays.asList("name", "email")); // Selected properties
 *
 * // Map to Bean conversion
 * Map<String, Object> userData = Map.of("name", "Jane", "age", 25, "email", "jane@example.com");
 * User userFromMap = Beans.map2Bean(userData, User.class);               // Convert map to bean
 * User userFromMapIgnoreUnknown = Beans.map2Bean(userData, false, true, User.class); // Ignore unknown properties
 *
 * // Object merging with strategies
 * User source = new User("John", 30, "john@example.com");
 * User target = new User("Jane", 25, null);
 * Beans.merge(source, target);                                          // Merge source into target
 * Beans.merge(source, target, (sourceVal, targetVal) -> sourceVal);     // Custom merge function
 *
 * // Object comparison operations
 * boolean isEqual = Beans.equalsByProps(user1, user2, Arrays.asList("name")); // Equality check by props
 *
 * // Null-safe operations
 * Map<String, Object> nullSafeMap = Beans.bean2Map(null);               // Returns empty map
 * User nullSafeUser = Beans.map2Bean(null, User.class);                 // Returns null
 * boolean nullClassCheck = Beans.isBeanClass(null);                     // Returns false
 * }</pre>
 *
 * <p><b>Bean-to-Map Conversion Options:</b>
 * <ul>
 *   <li><b>Flat Conversion:</b> {@code bean2FlatMap()} - Flat map representation with dot notation</li>
 *   <li><b>Deep Conversion:</b> {@code deepBean2Map(bean)} - Recursive nested object conversion</li>
 *   <li><b>Selected Conversion:</b> {@code bean2Map(bean, selectPropNames)} - Specific property selection</li>
 * </ul>
 *
 * <p><b>Map-to-Bean Conversion Features:</b>
 * <ul>
 *   <li><b>Type Conversion:</b> Automatic conversion between compatible types</li>
 *   <li><b>Unknown Property Handling:</b> Option to ignore or throw exceptions for unknown properties</li>
 *   <li><b>Nested Object Support:</b> Recursive conversion of nested maps to nested beans</li>
 *   <li><b>Collection Support:</b> Conversion of map collections to bean collections</li>
 * </ul>
 *
 * <p><b>Object Merging Strategies:</b>
 * <ul>
 *   <li><b>Default Merge:</b> Source values overwrite target values unconditionally</li>
 *   <li><b>Custom Functions:</b> User-defined merge logic with BiFunction parameters</li>
 *   <li><b>Partial Merge:</b> Merge only selected properties</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Reflection Caching:</b> Extensive caching of Method, Field, and Constructor objects</li>
 *   <li><b>BeanInfo Caching:</b> Cached property metadata for repeated operations</li>
 *   <li><b>Type Conversion Caching:</b> Cached conversion functions for performance</li>
 *   <li><b>Lazy Initialization:</b> On-demand initialization of expensive reflection operations</li>
 *   <li><b>Memory Efficient:</b> Optimized object allocation and garbage collection friendly</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All static methods are stateless and thread-safe</li>
 *   <li><b>Concurrent Caching:</b> Thread-safe caching using ConcurrentHashMap</li>
 *   <li><b>Immutable Operations:</b> Methods create new objects rather than modifying inputs</li>
 *   <li><b>No Shared State:</b> No mutable static fields that could cause race conditions</li>
 * </ul>
 *
 * <p><b>Annotation Support:</b>
 * <ul>
 *   <li><b>JAXB Annotations:</b> Full support for XML binding annotations</li>
 *   <li><b>Validation Annotations:</b> Integration with bean validation frameworks</li>
 *   <li><b>Custom Annotations:</b> Support for {@code @Entity}, {@code @Record}, {@code @DiffIgnore}</li>
 *   <li><b>Reflection Metadata:</b> Annotation-based property filtering and transformation</li>
 * </ul>
 *
 * <p><b>Builder Pattern Support:</b>
 * <ul>
 *   <li><b>Builder Detection:</b> Automatic detection of builder pattern implementations</li>
 *   <li><b>Builder Creation:</b> Factory methods for creating builder instances</li>
 *   <li><b>Builder Integration:</b> Seamless integration with bean conversion operations</li>
 *   <li><b>Custom Builders:</b> Support for registration of custom builder patterns</li>
 * </ul>
 *
 * <p><b>Error Handling Strategy:</b>
 * <ul>
 *   <li><b>Graceful Degradation:</b> Methods handle edge cases without throwing exceptions</li>
 *   <li><b>Null Tolerance:</b> Comprehensive null input handling throughout the API</li>
 *   <li><b>Type Safety:</b> Runtime type validation with clear error messages</li>
 *   <li><b>Exception Wrapping:</b> Reflection exceptions wrapped in clear, actionable messages</li>
 * </ul>
 *
 * <p><b>Integration with Abacus Framework:</b>
 * <ul>
 *   <li><b>Type System:</b> Full integration with Abacus type conversion system</li>
 *   <li><b>Parser Utilities:</b> Integration with ParserUtil for advanced parsing</li>
 *   <li><b>Stream API:</b> Compatible with Abacus Stream operations</li>
 *   <li><b>Collection Utilities:</b> Integration with Maps, Iterables, and other utilities</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use cached property access methods for better performance in loops</li>
 *   <li>Prefer specific property selection over full bean conversion when possible</li>
 *   <li>Use appropriate merge strategies based on your use case requirements</li>
 *   <li>Leverage builder pattern support for immutable object creation</li>
 *   <li>Use null-safe operations to build robust applications</li>
 *   <li>Cache BeanInfo objects for repeated operations on the same class</li>
 * </ul>
 *
 * <p><b>Performance Tips:</b>
 * <ul>
 *   <li>Use property name constants instead of string literals for better performance</li>
 *   <li>Batch multiple property operations when working with the same object</li>
 *   <li>Consider using flat maps instead of deep conversion for simple use cases</li>
 *   <li>Leverage the caching mechanisms by reusing the same classes</li>
 *   <li>Use appropriate collection types for optimal conversion performance</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Bean Validation:</b> {@code if (Beans.isBeanClass(clazz)) { ... }}</li>
 *   <li><b>Safe Property Access:</b> {@code Object value = Beans.getPropValue(bean, propName);}</li>
 *   <li><b>DTO Conversion:</b> {@code DTO dto = Beans.copy(entity, DTO.class);}</li>
 *   <li><b>Configuration Mapping:</b> {@code Config config = Beans.map2Bean(properties, Config.class);}</li>
 * </ul>
 *
 * <p><b>Related Utility Classes:</b>
 * <ul>
 *   <li><b>{@link com.landawn.abacus.parser.ParserUtil}:</b> Parser utilities and BeanInfo management</li>
 *   <li><b>{@link com.landawn.abacus.util.Maps}:</b> Map utilities for bean-map conversion</li>
 *   <li><b>{@link com.landawn.abacus.util.N}:</b> General utility class with object operations</li>
 *   <li><b>{@link com.landawn.abacus.util.CommonUtil}:</b> Base utility operations</li>
 *   <li><b>{@link com.landawn.abacus.util.Strings}:</b> String utilities for property name transformation</li>
 *   <li><b>{@link com.landawn.abacus.util.TypeReference}:</b> Type utilities for conversion operations</li>
 *   <li><b>{@link com.landawn.abacus.util.Clazz}:</b> Class utilities and reflection helpers</li>
 *   <li><b>{@link java.beans.BeanInfo}:</b> Standard Java bean introspection</li>
 * </ul>
 *
 * <p><b>Example: Complex Object Processing</b>
 * <pre>{@code
 * // Complex bean processing example
 * @Entity
 * public class User {
 *     private String name;
 *     private int age;
 *     private Address address;
 *     private List<String> roles;
 *     // getters and setters...
 * }
 *
 * // Comprehensive bean operations
 * User user = new User();
 * user.setName("John Doe");
 * user.setAge(30);
 * user.setAddress(new Address("123 Main St", "Anytown", "12345"));
 * user.setRoles(Arrays.asList("admin", "user"));
 *
 * // Deep introspection
 * List<String> allProps = Beans.getPropNameList(User.class);           // [name, age, address, roles]
 *
 * // Complex conversion operations
 * Map<String, Object> deepMap = Beans.deepBean2Map(user);              // Deep nested conversion
 * Map<String, Object> flatMap = Beans.bean2FlatMap(user, Arrays.asList("address"));   // Flatten address properties
 * Map<String, Object> filteredMap = Beans.bean2Map(user, Arrays.asList("name", "age")); // Only name and age
 *
 * // Advanced copying with transformations
 * UserDTO dto = Beans.copy(user, UserDTO.class);                       // Convert to DTO
 * User cloned = Beans.clone(user);                                     // Deep clone
 * User partial = Beans.copy(user, Arrays.asList("name", "age"), User.class); // Partial copy
 *
 * // Merging with different strategies
 * User updates = new User();
 * updates.setName("Jane Doe");
 *
 * Beans.merge(updates, user);                                          // Merge updates into user
 * Beans.merge(updates, user, (source, target) ->
 *     source != null && !source.equals("") ? source : target);         // Custom merge logic
 *
 * // Validation and comparison
 * boolean isValid = Beans.isBeanClass(User.class);
 * boolean isEqual = Beans.equalsByProps(user, cloned, Arrays.asList("name", "age"));
 * }</pre>
 *
 * <p><b>Example: Configuration Management</b>
 * <pre>{@code
 * // Configuration bean processing
 * public class DatabaseConfig {
 *     private String host = "localhost";
 *     private int port = 5432;
 *     private String database;
 *     private String username;
 *     private String password;
 *     private boolean ssl = false;
 *     // getters and setters...
 * }
 *
 * // Load configuration from multiple sources
 * Map<String, Object> envVars = System.getenv().entrySet().stream()
 *     .filter(entry -> entry.getKey().startsWith("DB_"))
 *     .collect(Collectors.toMap(
 *         entry -> Strings.toCamelCase(entry.getKey().substring(3).toLowerCase()),
 *         Map.Entry::getValue
 *     ));
 *
 * Map<String, Object> properties = loadPropertiesFile("database.properties");
 * Map<String, Object> defaults = Beans.bean2Map(new DatabaseConfig()); // Get defaults
 *
 * // Merge configurations with precedence: env vars > properties > defaults
 * Map<String, Object> finalConfig = new HashMap<>(defaults);
 * finalConfig.putAll(properties);
 * finalConfig.putAll(envVars);
 *
 * // Convert to configuration bean
 * DatabaseConfig config = Beans.map2Bean(finalConfig, DatabaseConfig.class);
 *
 * // Validate configuration
 * boolean isValidBean = Beans.isBeanClass(DatabaseConfig.class);
 * List<String> requiredProps = Arrays.asList("host", "port", "database", "username");
 * boolean hasAllRequired = requiredProps.stream()
 *     .allMatch(prop -> Beans.getPropValue(config, prop) != null);
 *
 * // Generate configuration summary
 * Map<String, Object> summary = Beans.bean2Map(config, Arrays.asList("host", "port", "database", "ssl"));
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons BeanUtils, Spring Framework, and other open
 * source projects under the Apache License 2.0. Methods from these libraries may have been modified
 * for consistency, performance optimization, and enhanced functionality within the Abacus framework.</p>
 *
 * @see com.landawn.abacus.parser.ParserUtil
 * @see com.landawn.abacus.parser.ParserUtil.BeanInfo
 * @see com.landawn.abacus.parser.ParserUtil.PropInfo
 * @see com.landawn.abacus.util.Maps
 * @see com.landawn.abacus.util.N
 * @see com.landawn.abacus.util.CommonUtil
 * @see com.landawn.abacus.util.Strings
 * @see com.landawn.abacus.util.TypeReference
 * @see com.landawn.abacus.util.Clazz
 * @see com.landawn.abacus.util.stream.Stream
 * @see com.landawn.abacus.annotation.Entity
 * @see com.landawn.abacus.annotation.Record
 * @see java.beans.BeanInfo
 * @see java.lang.reflect.Method
 * @see java.lang.reflect.Field
 */
public final class Beans {

    private Beans() {
        // utility class
    }

    private static final Logger logger = LoggerFactory.getLogger(Beans.class);

    // ...
    private static final String PROP_NAME_SEPARATOR = ".";

    // ...
    private static final String GET = "get";

    private static final String SET = "set";

    private static final String IS = "is";

    private static final String HAS = "has";

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final Map<String, String> camelCasePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<String, String> lowerCaseWithUnderscorePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<String, String> upperCaseWithUnderscorePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<Class<?>, Boolean> registeredXMLBindingClassList = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Set<String>> registeredNonPropGetSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableList<String>> beanDeclaredPropNameListPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Field>> beanDeclaredPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Field>> beanPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Method>> beanDeclaredPropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Method>> beanDeclaredPropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> beanPropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> beanPropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, List<Method>>> beanInlinePropGetMethodPool = new ObjectPool<>(POOL_SIZE);

    //    /** The Constant beanInlinePropSetMethodPool. */
    //    private static final Map<Class<?>, Map<String, List<Method>>> beanInlinePropSetMethodPool = new ObjectPool<>(POOL_SIZE);

    // ...
    private static final Map<String, String> formalizedPropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<Method, String> methodPropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    // reserved words.
    private static final Map<String, String> keyWordMapper = new HashMap<>(16);

    static {
        keyWordMapper.put("class", "clazz");
    }

    private static final Set<String> nonGetSetMethodName = N.newHashSet(16);

    static {
        nonGetSetMethodName.add("getClass");
        nonGetSetMethodName.add("hashCode");
        nonGetSetMethodName.add("toString");
    }

    private static final Map<Class<?>, ImmutableSet<String>> beanDiffIgnoredPropNamesPool = new ObjectPool<>(POOL_SIZE);
    private static final Map<Class<?>, Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>>> builderMap = new ConcurrentHashMap<>();
    private static final Map<Class<?>, Boolean> beanClassPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Class<?>> registeredNonBeanClass = new ObjectPool<>(POOL_SIZE);

    static {
        registeredNonBeanClass.put(Object.class, Object.class);
        registeredNonBeanClass.put(Class.class, Class.class);
        registeredNonBeanClass.put(Calendar.class, Calendar.class);
        registeredNonBeanClass.put(java.util.Date.class, java.util.Date.class);
        registeredNonBeanClass.put(java.sql.Date.class, java.sql.Date.class);
        registeredNonBeanClass.put(java.sql.Time.class, java.sql.Time.class);
        registeredNonBeanClass.put(java.sql.Timestamp.class, java.sql.Timestamp.class);
    }

    private static final Class<?> recordClass;

    static {
        Class<?> cls = null;

        try {
            cls = Class.forName("java.lang.Record");
        } catch (final ClassNotFoundException e) {
            // ignore.
        }

        recordClass = cls;
    }

    private static final Map<Class<?>, Boolean> recordClassPool = new ObjectPool<>(POOL_SIZE);

    /**
     * Checks if the specified class is a bean class.
     *
     * <p>A class is considered a bean class if it:</p>
     * <ul>
     *   <li>Is annotated with {@code @Entity}</li>
     *   <li>Is a record class (Java 14+) or annotated with {@code @Record}</li>
     *   <li>Has at least one property with getter/setter methods</li>
     *   <li>Is not a CharSequence, Number, or Map.Entry implementation</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean isBean = Beans.isBeanClass(User.class);  // true for typical POJO
     * boolean isNotBean = Beans.isBeanClass(String.class);  // false
     * }</pre>
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a bean class, {@code false} otherwise
     */
    public static boolean isBeanClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        Boolean ret = beanClassPool.get(cls);

        if (ret == null) {
            ret = annotatedWithEntity(cls) || isRecordClass(cls) || (!CharSequence.class.isAssignableFrom(cls) && !Number.class.isAssignableFrom(cls)
                    && !Map.Entry.class.isAssignableFrom(cls) && N.notEmpty(getPropNameList(cls)));
            beanClassPool.put(cls, ret);
        }

        return ret;
    }

    /**
     * Checks if the specified class is a record class.
     *
     * <p>A class is considered a record class if:</p>
     * <ul>
     *   <li>It extends java.lang.Record (Java 14+)</li>
     *   <li>It is annotated with {@code @Record}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * record Point(int x, int y) {}
     * boolean isRec = Beans.isRecordClass(Point.class);  // true
     * }</pre>
     *
     * @param cls the class to be checked
     * @return {@code true} if the specified class is a record class, {@code false} otherwise
     */
    public static boolean isRecordClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        return recordClassPool.computeIfAbsent(cls, k -> (recordClass != null && recordClass.isAssignableFrom(cls)) || cls.getAnnotation(Record.class) != null);
    }

    /**
     * Retrieves or creates a {@link BeanInfo} instance for the specified class.
     *
     * <p>This method maintains a cache of BeanInfo instances to improve performance.
     * The BeanInfo contains metadata about the class including property information,
     * annotations, and type details.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo beanInfo = Beans.getBeanInfo(User.class);
     * List<PropInfo> properties = beanInfo.propInfoList;
     * for (PropInfo prop : properties) {
     *     System.out.println(prop.name + ": " + prop.clazz);
     * }
     * }</pre>
     *
     * @param beanType the bean type to get bean information for
     * @return a BeanInfo instance containing metadata about the class
     * @throws IllegalArgumentException if the class is not a bean class (no properties)
     * @see ParserUtil#getBeanInfo(Class)
     */
    public static BeanInfo getBeanInfo(final java.lang.reflect.Type beanType) {
        return ParserUtil.getBeanInfo(beanType);
    }

    /**
     * Refreshes the cached bean property information for the specified class.
     *
     * <p>This method removes the cached BeanInfo for the specified class, forcing
     * it to be recreated on the next call to {@link #getBeanInfo(java.lang.reflect.Type)}.</p>
     *
     * <p>This method is marked as deprecated and is for internal use only.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // After dynamically modifying a class
     * Beans.refreshBeanPropInfo(ModifiedClass.class);
     * }</pre>
     *
     * @param cls the class whose bean property information should be refreshed
     * @see ParserUtil#refreshBeanPropInfo(java.lang.reflect.Type)
     * @deprecated internal use only
     */
    @Deprecated
    @Internal
    public static void refreshBeanPropInfo(final Class<?> cls) {
        ParserUtil.refreshBeanPropInfo(cls);
    }

    /**
     * Retrieves the builder information for the specified class.
     * The builder information includes the builder class type, a supplier for creating builder instances, 
     * and a function to build the target object from the builder.
     *
     * <p>This method looks for common builder patterns:</p>
     * <ul>
     *   <li>Static methods named "builder", "newBuilder", or "createBuilder"</li>
     *   <li>Builder classes with "build" or "create" methods</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Tuple3<Class<?>, Supplier<Object>, Function<Object, Object>> builderInfo =
     *     Beans.getBuilderInfo(Person.class);
     * if (builderInfo != null) {
     *     Object builder = builderInfo._2.get();  // Create builder
     *     Object instance = builderInfo._3.apply(builder);  // Build instance
     * }
     * }</pre>
     *
     * @param cls the class for which the builder information is to be retrieved
     * @return a tuple containing the builder class type, a supplier for creating builder instances,
     *         and a function to build the target object from the builder, or {@code null} if no builder is found
     * @throws IllegalArgumentException if the specified class is {@code null}
     */
    @MayReturnNull
    public static Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> getBuilderInfo(
            final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> builderInfo = builderMap
                .get(cls);

        if (builderInfo == null) {
            Method buildMethod = null;
            Class<?> builderClass = null;
            Method builderMethod = getBuilderMethod(cls);

            if (builderMethod == null) {
                builderClass = Stream.of(cls.getDeclaredClasses())
                        .filter(it -> getBuilderMethod(it) != null && getBuildMethod(it, cls) != null)
                        .first()
                        .orElseNull();

                if (builderClass != null) {
                    builderMethod = getBuilderMethod(builderClass);
                }
            }

            if (builderMethod != null) {
                builderClass = builderMethod.getReturnType();
                buildMethod = getBuildMethod(builderClass, cls);

                if (buildMethod != null) {
                    final Method finalBuilderMethod = builderMethod;
                    final Method finalBuildMethod = buildMethod;

                    final com.landawn.abacus.util.function.Supplier<Object> builderSupplier = () -> ClassUtil.invokeMethod(finalBuilderMethod);
                    final com.landawn.abacus.util.function.Function<Object, Object> buildFunc = instance -> ClassUtil.invokeMethod(instance, finalBuildMethod);

                    builderInfo = Tuple.of(builderClass, builderSupplier, buildFunc);

                    builderMap.put(cls, builderInfo);

                    return builderInfo;
                }
            }

            builderInfo = Tuple.of(null, null, null);
            builderMap.put(cls, builderInfo);
        }

        return builderInfo._1 == null ? null : builderInfo;
    }

    private static Method getBuilderMethod(final Class<?> cls) {
        Method builderMethod = null;

        try {
            builderMethod = cls.getDeclaredMethod("builder");
        } catch (final Exception e) {
            // ignore
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            try {
                builderMethod = cls.getDeclaredMethod("newBuilder");
            } catch (final Exception e) {
                // ignore
            }
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            try {
                builderMethod = cls.getDeclaredMethod("createBuilder");
            } catch (final Exception e) {
                // ignore
            }
        }

        if (builderMethod == null || builderMethod.getParameterCount() != 0
                || !(Modifier.isStatic(builderMethod.getModifiers()) && Modifier.isPublic(builderMethod.getModifiers()))) {
            return null;
        }

        return builderMethod;
    }

    private static Method getBuildMethod(final Class<?> builderClass, final Class<?> beanClass) {
        Method buildMethod = null;

        try {
            buildMethod = builderClass.getDeclaredMethod("build");
        } catch (final Exception e) {
            // ignore
        }

        if (buildMethod == null || buildMethod.getParameterCount() != 0 || !Modifier.isPublic(buildMethod.getModifiers())
                || !beanClass.isAssignableFrom(buildMethod.getReturnType())) {
            try {
                buildMethod = builderClass.getDeclaredMethod("create");
            } catch (final Exception e) {
                // ignore
            }
        }

        if (buildMethod == null || buildMethod.getParameterCount() != 0 || !Modifier.isPublic(buildMethod.getModifiers())
                || !beanClass.isAssignableFrom(buildMethod.getReturnType())) {
            return null;
        }

        return buildMethod;
    }

    /**
     * Registers a class as a non-bean class. Non-bean classes are excluded from
     * bean property introspection and are treated as simple value types.
     *
     * <p>This is useful for classes that should not be treated as JavaBeans,
     * such as primitive wrappers, dates, or custom value objects.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Register a custom value class
     * Beans.registerNonBeanClass(Money.class);
     * Beans.registerNonBeanClass(PhoneNumber.class);
     * }</pre>
     *
     * @param cls the class to be registered as a non-bean class
     */
    @SuppressWarnings("deprecation")
    public static void registerNonBeanClass(final Class<?> cls) {
        registeredNonBeanClass.put(cls, cls);

        synchronized (beanDeclaredPropGetMethodPool) {
            registeredXMLBindingClassList.put(cls, false);

            if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                beanDeclaredPropGetMethodPool.remove(cls);
                beanDeclaredPropSetMethodPool.remove(cls);

                beanPropFieldPool.remove(cls);
                loadPropGetSetMethodList(cls);
            }

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Registers a non-property get/set method for the specified class.
     * This excludes specific methods from being considered as property accessors
     * during bean introspection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Exclude 'getInternal' from being treated as a property getter
     * Beans.registerNonPropGetSetMethod(MyClass.class, "internal");
     * // Now MyClass.getInternal() won't be considered a property getter
     * }</pre>
     *
     * @param cls the class for which the non-property get/set method is to be registered
     * @param propName the name of the property to be registered as a non-property get/set method
     */
    @SuppressWarnings("deprecation")
    public static void registerNonPropGetSetMethod(final Class<?> cls, final String propName) {
        synchronized (registeredNonPropGetSetMethodPool) {
            Set<String> set = registeredNonPropGetSetMethodPool.computeIfAbsent(cls, k -> N.newHashSet());

            set.add(propName);

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Registers a property get/set method for the specified property name.
     * This allows manual registration of methods as property accessors that might
     * not follow standard JavaBean naming conventions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Method customGetter = MyClass.class.getMethod("fetchName");
     * Beans.registerPropGetSetMethod("name", customGetter);
     * // Now fetchName() is recognized as the getter for property "name"
     * }</pre>
     *
     * @param propName the name of the property
     * @param method the method to be registered as a property get/set method
     * @throws IllegalArgumentException if the method is not a valid getter or setter,
     *         or if the property is already registered with a different method
     */
    @SuppressWarnings("deprecation")
    public static void registerPropGetSetMethod(final String propName, final Method method) {
        final Class<?> cls = method.getDeclaringClass();

        synchronized (beanDeclaredPropGetMethodPool) {
            if (isGetMethod(method)) {
                Map<String, Method> propMethodMap = beanPropGetMethodPool.get(cls);

                if (propMethodMap == null) {
                    loadPropGetSetMethodList(cls);
                    propMethodMap = beanPropGetMethodPool.get(cls);
                }

                if (propMethodMap.containsKey(propName)) {
                    if (!method.equals(propMethodMap.get(propName))) {
                        throw new IllegalArgumentException(
                                propName + " has already been registered with different method: " + propMethodMap.get(propName).getName());
                    }
                } else {
                    propMethodMap.put(propName, method);
                }
            } else if (isSetMethod(method)) {
                Map<String, Method> propMethodMap = beanPropSetMethodPool.get(cls);

                if (propMethodMap == null) {
                    loadPropGetSetMethodList(cls);
                    propMethodMap = beanPropSetMethodPool.get(cls);
                }

                if (propMethodMap.containsKey(propName)) {
                    if (!method.equals(propMethodMap.get(propName))) {
                        throw new IllegalArgumentException(
                                propName + " has already been registered with different method: " + propMethodMap.get(propName).getName());
                    }
                } else {
                    propMethodMap.put(propName, method);
                }
            } else {
                throw new IllegalArgumentException("The name of property getter/setter method must start with 'get/is/has' or 'set': " + method.getName());
            }

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Registers a class for XML binding (JAXB) support. When a class is registered
     * for XML binding, properties that only have getter methods (without setters)
     * are still considered valid properties if they return collection or map types.
     *
     * <p>This is particularly useful for JAXB-generated classes where collections
     * are typically exposed only through getters.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Register JAXB-generated class
     * Beans.registerXMLBindingClass(JAXBGeneratedClass.class);
     * // Now collection properties with only getters are recognized
     * }</pre>
     *
     * @param cls the class to be registered for XML binding
     */
    @SuppressWarnings("deprecation")
    public static void registerXMLBindingClass(final Class<?> cls) {
        if (registeredXMLBindingClassList.containsKey(cls)) {
            return;
        }

        synchronized (beanDeclaredPropGetMethodPool) {
            registeredXMLBindingClassList.put(cls, true);

            if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                beanDeclaredPropGetMethodPool.remove(cls);
                beanDeclaredPropSetMethodPool.remove(cls);

                beanPropFieldPool.remove(cls);
                loadPropGetSetMethodList(cls);
            }

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Checks if the specified class is registered for XML binding.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (Beans.isRegisteredXMLBindingClass(MyClass.class)) {
     *     // Handle XML binding specific logic
     * }
     * }</pre>
     *
     * @param cls the class to check
     * @return {@code true} if the class is registered for XML binding, {@code false} otherwise
     */
    public static boolean isRegisteredXMLBindingClass(final Class<?> cls) {
        return registeredXMLBindingClassList.containsKey(cls);
    }

    /**
     * Retrieves the property name associated with the specified getter or setter method.
     *
     * <p>This method extracts the property name from method names following JavaBean conventions:</p>
     * <ul>
     *   <li>getName() -&gt; "name"</li>
     *   <li>isActive() -&gt; "active"</li>
     *   <li>hasChildren() -&gt; "children"</li>
     *   <li>setAge(int) -&gt; "age"</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Method getter = User.class.getMethod("getName");
     * String propName = Beans.getPropNameByMethod(getter);  // Returns "name"
     * }</pre>
     *
     * @param getSetMethod the method whose property name is to be retrieved
     * @return the property name associated with the specified method
     */
    public static String getPropNameByMethod(final Method getSetMethod) {
        String propName = methodPropNamePool.get(getSetMethod);

        if (propName == null) {
            final String methodName = getSetMethod.getName();
            final Class<?>[] paramTypes = getSetMethod.getParameterTypes();
            final Class<?> targetType = N.isEmpty(paramTypes) ? getSetMethod.getReturnType() : paramTypes[0];

            Field field = getDeclaredField(getSetMethod.getDeclaringClass(), methodName);

            if (field != null && field.getType().isAssignableFrom(targetType)) {
                propName = field.getName();
            }

            field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + methodName);

            if (field != null && field.getType().isAssignableFrom(targetType)) {
                propName = field.getName();
            }

            if (Strings.isEmpty(propName) && ((methodName.startsWith(IS) && methodName.length() > 2)
                    || ((methodName.startsWith(GET) || methodName.startsWith(SET) || methodName.startsWith(HAS)) && methodName.length() > 3))) {
                final String newName = methodName.substring(methodName.startsWith(IS) ? 2 : 3);
                field = getDeclaredField(getSetMethod.getDeclaringClass(), Strings.uncapitalize(newName));

                if (field != null && field.getType().isAssignableFrom(targetType)) {
                    propName = field.getName();
                }

                if (Strings.isEmpty(propName) && Strings.isNotEmpty(newName) && newName.charAt(0) != '_') {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + Strings.uncapitalize(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName)) {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), Beans.formalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName) && Strings.isNotEmpty(newName) && newName.charAt(0) != '_') {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + Beans.formalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName)) {
                    propName = Beans.formalizePropName(newName);
                }
            }

            if (Strings.isEmpty(propName)) {
                propName = methodName;
            }

            methodPropNamePool.put(getSetMethod, propName);
        }

        return propName;
    }

    /**
     * Returns an immutable list of property names for the specified class.
     *
     * <p>The list includes all properties that have getter and/or setter methods
     * following JavaBean conventions. The order is consistent with the declaration
     * order in the class.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableList<String> props = Beans.getPropNameList(User.class);
     * // Returns ["id", "name", "email", "age"] for a typical User class
     * }</pre>
     *
     * @param cls the class whose property names are to be retrieved
     * @return an immutable list of property names for the specified class
     */
    public static ImmutableList<String> getPropNameList(final Class<?> cls) {
        N.checkArgNotNull(cls, cs.cls);

        ImmutableList<String> propNameList = beanDeclaredPropNameListPool.get(cls);

        if (propNameList == null) {
            Beans.loadPropGetSetMethodList(cls);
            propNameList = beanDeclaredPropNameListPool.get(cls);
        }

        return propNameList;
    }

    /**
     * Retrieves a list of property names for the specified class, excluding the specified property names.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> props = Beans.getPropNames(User.class, Arrays.asList("password", "ssn"));
     * // Returns all properties except "password" and "ssn"
     * }</pre>
     *
     * @param cls the class whose property names are to be retrieved
     * @param propNameToExclude the collection of property names to exclude from the result
     * @return a list of property names for the specified class, excluding the specified property names
     * @deprecated replaced by {@link #getPropNames(Class, Set)}
     * @see #getPropNames(Class, Set)
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static List<String> getPropNames(final Class<?> cls, final Collection<String> propNameToExclude) {
        N.checkArgNotNull(cls, cs.cls);

        if (N.isEmpty(propNameToExclude)) {
            return new ArrayList<>(getPropNameList(cls));
        }

        if (propNameToExclude instanceof Set) {
            return getPropNames(cls, (Set) propNameToExclude);
        }

        return getPropNames(cls, N.newHashSet(propNameToExclude));
    }

    /**
     * Retrieves a list of property names for the specified class, excluding the specified property names.
     *
     * <p>This method is more efficient than the deprecated Collection-based version when
     * the excluded properties are already in a Set.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Set<String> excluded = Set.of("password", "internalId");
     * List<String> props = Beans.getPropNames(User.class, excluded);
     * // Returns ["id", "name", "email"] if User has those properties
     * }</pre>
     *
     * @param cls the class whose property names are to be retrieved
     * @param propNameToExclude the set of property names to exclude from the result
     * @return a list of property names for the specified class, excluding the specified property names
     */
    public static List<String> getPropNames(final Class<?> cls, final Set<String> propNameToExclude) {
        N.checkArgNotNull(cls, cs.cls);

        final ImmutableList<String> propNameList = getPropNameList(cls);

        if (N.isEmpty(propNameToExclude)) {
            return new ArrayList<>(propNameList);
        }

        final List<String> result = new ArrayList<>(propNameList.size() - propNameToExclude.size());

        for (final String propName : propNameList) {
            if (!propNameToExclude.contains(propName)) {
                result.add(propName);
            }
        }

        return result;
    }

    static final BiPredicate<String, Object> NON_PROP_VALUE = (propName, propValue) -> propValue != null;

    /**
     * Retrieves the property names of the given bean object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", null, "john@email.com");
     *
     * // Get all properties
     * List<String> allProps = Beans.getPropNames(user, false);
     * // Returns ["name", "age", "email"]
     *
     * // Get only non-null properties
     * List<String> nonNullProps = Beans.getPropNames(user, true);
     * // Returns ["name", "email"] (age is null)
     * }</pre>
     *
     * @param bean the bean object whose property names are to be retrieved.
     * @param ignoreNullValue if {@code true}, the method will ignore property names with {@code null} values.
     * @return a list of strings representing the property names of the given bean object. If {@code ignoreNullValue} is {@code true}, properties with {@code null} values are not included in the list.
     * @see #getPropNameList
     * @see #getPropNames
     */
    public static List<String> getPropNames(final Object bean, final boolean ignoreNullValue) {
        if (ignoreNullValue) {
            return getPropNames(bean, NON_PROP_VALUE);
        } else {
            return getPropNames(bean, Fn.alwaysTrue());
        }
    }

    /**
     * Retrieves a list of property names for the specified bean, filtered by the given predicate.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25, "john@email.com");
     *
     * // Get properties starting with "e"
     * List<String> eProps = Beans.getPropNames(user, name -> name.startsWith("e"));
     * // Returns ["email"]
     * }</pre>
     *
     * @param bean the bean object whose property names are to be retrieved
     * @param propNameFilter the predicate to filter property names
     * @return a list of property names for the specified bean, filtered by the given predicate
     */
    public static List<String> getPropNames(final Object bean, final Predicate<String> propNameFilter) {
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        final int size = beanInfo.propInfoList.size();
        final List<String> result = new ArrayList<>(size < 10 ? size : size / 2);

        for (final ParserUtil.PropInfo propInfo : beanInfo.propInfoList) {
            if (propNameFilter.test(propInfo.name)) {
                result.add(propInfo.name);
            }
        }

        return result;
    }

    /**
     * Retrieves a list of property names for the specified bean, filtered by the given BiPredicate.
     *
     * <p>The BiPredicate receives both the property name and its value, allowing for
     * more sophisticated filtering based on both.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25, "john@email.com");
     *
     * // Get string properties with non-empty values
     * List<String> nonEmptyStrings = Beans.getPropNames(user,
     *     (name, value) -> value instanceof String && !((String)value).isEmpty());
     * // Returns ["name", "email"]
     *
     * // Get numeric properties greater than 20
     * List<String> largeNumbers = Beans.getPropNames(user,
     *     (name, value) -> value instanceof Number && ((Number)value).intValue() > 20);
     * // Returns ["age"]
     * }</pre>
     *
     * @param bean the bean object whose property names are to be retrieved
     * @param propNameValueFilter the bi-predicate to filter property names and values, where the first parameter is the property name and the second parameter is the property value
     * @return a list of property names for the specified bean, filtered by the given bi-predicate
     */
    public static List<String> getPropNames(final Object bean, final BiPredicate<String, Object> propNameValueFilter) {
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        final int size = beanInfo.propInfoList.size();
        final List<String> result = new ArrayList<>(size < 10 ? size : size / 2);

        for (final ParserUtil.PropInfo propInfo : beanInfo.propInfoList) {
            if (propNameValueFilter.test(propInfo.name, propInfo.getPropValue(bean))) {
                result.add(propInfo.name);
            }
        }

        return result;
    }

    /**
     * Retrieves a set of property names that are ignored during the {@code MapDifference.of(Object, Object)} operation for the specified class.
     *
     * <p>Properties are ignored if they are annotated with {@code @DiffIgnore} or similar annotations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User {
     *     private String name;
     *     @DiffIgnore
     *     private Date lastModified;
     * }
     *
     * ImmutableSet<String> ignored = Beans.getDiffIgnoredPropNames(User.class);
     * // Returns ["lastModified"]
     * }</pre>
     *
     * @param cls the class for which the {@code MapDifference.of(Object, Object)} operation ignored property names are to be retrieved
     * @return an immutable set of property names that are ignored during the {@code MapDifference.of(Object, Object)} operation
     * @see com.landawn.abacus.util.Difference.MapDifference
     * @see com.landawn.abacus.util.Difference.BeanDifference#of(Object, Object)
     */
    public static ImmutableSet<String> getDiffIgnoredPropNames(final Class<?> cls) {
        ImmutableSet<String> propNames = beanDiffIgnoredPropNamesPool.get(cls);

        if (propNames == null) {
            propNames = Stream.of(ParserUtil.getBeanInfo(cls).propInfoList)
                    .filter(propInfo -> propInfo.isAnnotationPresent(DiffIgnore.class) || propInfo.annotations.values()
                            .stream()
                            .anyMatch(it -> Strings.equalsAnyIgnoreCase(it.getClass().getSimpleName(), "DiffIgnore", "DifferenceIgnore")))
                    .map(it -> it.name)
                    .toImmutableSet();

            beanDiffIgnoredPropNamesPool.put(cls, propNames);
        }

        return propNames;
    }

    private static boolean annotatedWithEntity(final Class<?> cls) {
        if (cls.getAnnotation(Entity.class) != null) {
            return true;
        }

        final Annotation[] annotations = cls.getAnnotations();

        if (N.notEmpty(annotations)) {
            for (final Annotation annotation : annotations) {
                try {
                    if (annotation.annotationType().equals(javax.persistence.Entity.class)) {
                        return true;
                    }
                } catch (final Throwable e) {
                    // ignore
                }

                try {
                    if (annotation.annotationType().equals(jakarta.persistence.Entity.class)) {
                        return true;
                    }
                } catch (final Throwable e) {
                    // ignore
                }
            }
        }

        return false;
    }

    private static boolean isFieldGetMethod(final Method method, final Field field) {
        if (!isGetMethod(method) || Object.class.equals(method.getDeclaringClass()) || !method.getReturnType().isAssignableFrom(field.getType())) {
            return false;
        }

        final String fieldName = field.getName();
        final String methodName = method.getName();

        if (fieldName.equals(methodName) && getDeclaredField(method.getDeclaringClass(), fieldName) != null) {
            return true;
        }

        final String propName = methodName
                .substring(methodName.startsWith(IS) ? 2 : ((methodName.startsWith(HAS) || methodName.startsWith(GET) || methodName.startsWith(SET)) ? 3 : 0));

        return propName.equalsIgnoreCase(fieldName)
                || (fieldName.length() > 0 && fieldName.charAt(0) == '_' && propName.equalsIgnoreCase(fieldName.substring(1)));
    }

    private static boolean isGetMethod(final Method method) {
        if (Object.class.equals(method.getDeclaringClass())) {
            return false;
        }

        final String mn = method.getName();

        return (mn.startsWith(GET) || mn.startsWith(IS) || mn.startsWith(HAS) || getDeclaredField(method.getDeclaringClass(), mn) != null)
                && (N.isEmpty(method.getParameterTypes())) && !void.class.equals(method.getReturnType()) && !nonGetSetMethodName.contains(mn);
    }

    private static boolean isJAXBGetMethod(final Class<?> cls, final Object instance, final Method method, final Field field) {
        try {
            return (instance != null)
                    && ((registeredXMLBindingClassList.getOrDefault(cls, false) || N.anyMatch(cls.getAnnotations(), Beans::isXmlTypeAnno))
                            || (N.anyMatch(method.getAnnotations(), Beans::isXmlElementAnno)
                                    || (field != null && N.anyMatch(field.getAnnotations(), Beans::isXmlElementAnno))))
                    && (Collection.class.isAssignableFrom(method.getReturnType()) || Map.class.isAssignableFrom(method.getReturnType()))
                    && (ClassUtil.invokeMethod(instance, method) != null);
        } catch (final Exception e) {
            return false;
        }
    }

    private static boolean isXmlTypeAnno(final Annotation it) {
        final String simpleTypeName = it.annotationType().getSimpleName();

        return simpleTypeName.equals("XmlRootElement") || simpleTypeName.equals("XmlType");
    }

    private static boolean isXmlElementAnno(final Annotation it) {
        final String simpleTypeName = it.annotationType().getSimpleName();

        return simpleTypeName.equals("XmlElement") || simpleTypeName.equals("XmlElements");
    }

    private static boolean isPropName(final Class<?> cls, String inputPropName, final String propNameByMethod) {
        if (inputPropName.length() > 128) {
            throw new IllegalArgumentException("The property name exceed 128: " + inputPropName);
        }

        inputPropName = inputPropName.trim();

        return inputPropName.equalsIgnoreCase(propNameByMethod) || inputPropName.replace(WD.UNDERSCORE, Strings.EMPTY).equalsIgnoreCase(propNameByMethod)
                || inputPropName.equalsIgnoreCase(ClassUtil.getSimpleClassName(cls) + WD._PERIOD + propNameByMethod)
                || (inputPropName.startsWith(GET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(SET) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(IS) && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(HAS) && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod));
    }

    private static boolean isSetMethod(final Method method) {
        final String mn = method.getName();

        return (mn.startsWith(SET) || getDeclaredField(method.getDeclaringClass(), mn) != null) && N.len(method.getParameterTypes()) == 1
                && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(method.getDeclaringClass()))
                && !nonGetSetMethodName.contains(mn);
    }

    /**
     * Load prop get set method list.
     *
     * @param cls the class to load property getter and setter methods for
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    private static void loadPropGetSetMethodList(final Class<?> cls) {
        synchronized (beanDeclaredPropGetMethodPool) {
            if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                return;
            }

            Object instance = null;

            if (!registeredNonBeanClass.containsKey(cls)) {
                try {
                    instance = cls.getDeclaredConstructor().newInstance();
                } catch (final Exception e) {
                    if (logger.isWarnEnabled()) {
                        //noinspection StatementWithEmptyBody
                        if (Strings.isNotEmpty(cls.getPackageName()) && cls.getPackageName().startsWith("java.")) {
                            // ignore
                        } else {
                            logger.warn("Failed to new instance of class: " + cls.getCanonicalName() + " to check setter method by getter method");
                        }
                    }

                    if (registeredXMLBindingClassList.containsKey(cls)) {
                        registeredXMLBindingClassList.put(cls, false);
                    }
                }
            }

            final List<Class<?>> allClasses = new ArrayList<>();
            allClasses.add(cls);
            Class<?> superClass = null;

            while ((superClass = allClasses.get(allClasses.size() - 1).getSuperclass()) != null && !superClass.equals(Object.class)) {
                allClasses.add(superClass);
            }

            final Tuple3<Class<?>, com.landawn.abacus.util.function.Supplier<Object>, com.landawn.abacus.util.function.Function<Object, Object>> builderInfo = getBuilderInfo(
                    cls);
            final Class<?> builderClass = builderInfo == null ? null : builderInfo._1;

            final Map<String, Field> propFieldMap = new LinkedHashMap<>();
            final Map<String, Method> propGetMethodMap = new LinkedHashMap<>();
            final Map<String, Method> propSetMethodMap = new LinkedHashMap<>();

            Class<?> clazz = null;
            Method setMethod = null;

            final Constructor<?> noArgConstructor = ClassUtil.getDeclaredConstructor(cls);
            Constructor<?> allArgsConstructor = null;
            boolean isImmutable = false;

            for (int i = allClasses.size() - 1; i >= 0; i--) {
                clazz = allClasses.get(i);

                if (registeredNonBeanClass.containsKey(clazz)) {
                    continue;
                }

                final Map<String, String> staticFinalFields = getPublicStaticStringFields(clazz);

                final List<Tuple2<Field, Method>> fieldGetMethodList = new ArrayList<>();

                // sort the methods by the order of declared fields
                for (final Field field : clazz.getDeclaredFields()) {
                    Stream.of(clazz.getMethods())
                            .filter(method -> isFieldGetMethod(method, field))
                            .sortedBy(method -> method.getName().length())
                            .last()
                            .ifPresentOrElse(method -> fieldGetMethodList.add(Tuple.of(field, method)), () -> {
                                if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                                        && !Modifier.isFinal(field.getModifiers())) {
                                    fieldGetMethodList.add(Tuple.of(field, null));
                                }
                            });
                }

                if (noArgConstructor == null && clazz == cls) {
                    final Class<?>[] args = fieldGetMethodList.stream()
                            .filter(it -> it._2 != null)
                            .map(it -> it._1.getType())
                            .toArray(len -> new Class<?>[len]);

                    allArgsConstructor = ClassUtil.getDeclaredConstructor(cls, args);

                    isImmutable = allArgsConstructor != null && Modifier.isPublic(allArgsConstructor.getModifiers());
                }

                String propName = null;

                {
                    Field field = null;
                    Method method = null;

                    // sort the methods by the order of declared fields
                    for (final Tuple2<Field, Method> tp : fieldGetMethodList) {
                        field = tp._1;
                        method = tp._2;

                        if (method != null) {
                            propName = getPropNameByMethod(method);

                            if (!field.equals(getDeclaredField(clazz, propName))) {
                                propName = field.getName();
                            }

                            propName = (staticFinalFields.get(propName) != null) ? staticFinalFields.get(propName) : propName;

                            if (propGetMethodMap.containsKey(propName)) {
                                continue;
                            }

                            setMethod = getSetMethod(method);

                            if (setMethod != null) {
                                //ClassUtil.setAccessibleQuietly(field, true);
                                ClassUtil.setAccessibleQuietly(method, true);
                                ClassUtil.setAccessibleQuietly(setMethod, true);

                                propFieldMap.put(propName, field);
                                propGetMethodMap.put(propName, method);
                                propSetMethodMap.put(propName, setMethod);

                                continue;
                            }

                            if (isJAXBGetMethod(cls, instance, method, field) || annotatedWithEntity(cls) || Beans.isRecordClass(clazz) || builderClass != null
                                    || isImmutable) {
                                //ClassUtil.setAccessibleQuietly(field, true);
                                ClassUtil.setAccessibleQuietly(method, true);

                                propFieldMap.put(propName, field);
                                propGetMethodMap.put(propName, method);

                                //NOSONAR
                            }
                        } else if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers())
                                && !Modifier.isFinal(field.getModifiers())) {
                            propName = field.getName();
                            propName = (staticFinalFields.get(propName) != null) ? staticFinalFields.get(propName) : propName;

                            if (!propGetMethodMap.containsKey(propName)) {
                                ClassUtil.setAccessibleQuietly(field, true);

                                propFieldMap.put(propName, field);
                            }
                        }
                    }
                }

                for (final Method method : clazz.getMethods()) {
                    if (isGetMethod(method)) {
                        propName = getPropNameByMethod(method);
                        propName = (staticFinalFields.get(propName) != null) ? staticFinalFields.get(propName) : propName;

                        if (propGetMethodMap.containsKey(propName)) {
                            continue;
                        }

                        setMethod = getSetMethod(method);

                        if (setMethod != null && !propGetMethodMap.containsValue(method)) {
                            ClassUtil.setAccessibleQuietly(method, true);
                            ClassUtil.setAccessibleQuietly(setMethod, true);

                            propGetMethodMap.put(propName, method);
                            propSetMethodMap.put(propName, setMethod);

                            continue;
                        }

                        if ((isJAXBGetMethod(cls, instance, method, null) || annotatedWithEntity(cls) || Beans.isRecordClass(clazz))
                                && !propGetMethodMap.containsValue(method)) {
                            ClassUtil.setAccessibleQuietly(method, true);

                            propGetMethodMap.put(propName, method);

                            //NOSONAR
                        }
                    }
                }
            }

            for (final Map.Entry<Class<?>, Set<String>> entry : registeredNonPropGetSetMethodPool.entrySet()) { //NOSONAR
                if (entry.getKey().isAssignableFrom(cls)) {
                    final Set<String> set = entry.getValue();
                    final List<String> methodNames = new ArrayList<>(propGetMethodMap.keySet());

                    for (final String nonPropName : set) {
                        for (final String propName : methodNames) {
                            if (propName.equalsIgnoreCase(nonPropName)) {
                                propFieldMap.remove(propName);
                                propGetMethodMap.remove(propName);
                                propSetMethodMap.remove(propName);

                                break;
                            }
                        }
                    }
                }
            }

            // for Double-Checked Locking is Broke initialize it before
            final ImmutableMap<String, Field> unmodifiableFieldMap = ImmutableMap.wrap(propFieldMap);
            //noinspection ResultOfMethodCallIgnored
            unmodifiableFieldMap.keySet(); // initialize? //NOSONAR
            beanDeclaredPropFieldPool.put(cls, unmodifiableFieldMap);

            // put it into map.
            final Map<String, Field> tempFieldMap = new ObjectPool<>(N.max(64, propFieldMap.size()));
            tempFieldMap.putAll(propFieldMap);
            beanPropFieldPool.put(cls, tempFieldMap);

            final ImmutableMap<String, Method> unmodifiableGetMethodMap = ImmutableMap.wrap(propGetMethodMap);
            //noinspection ResultOfMethodCallIgnored
            unmodifiableGetMethodMap.keySet(); // initialize? //NOSONAR
            beanDeclaredPropGetMethodPool.put(cls, unmodifiableGetMethodMap);

            Map<String, Method> existingGetMethodMap = beanPropGetMethodPool.get(cls);
            if (existingGetMethodMap == null) {
                final Map<String, Method> tmp = new ObjectPool<>(N.max(64, propGetMethodMap.size()));
                tmp.putAll(propGetMethodMap);
                beanPropGetMethodPool.put(cls, tmp);
            } else {
                existingGetMethodMap.putAll(propGetMethodMap);
            }

            // for Double-Checked Locking is Broke to initialize it before put it into map.
            final ImmutableMap<String, Method> unmodifiableSetMethodMap = ImmutableMap.wrap(propSetMethodMap);
            //noinspection ResultOfMethodCallIgnored
            unmodifiableSetMethodMap.keySet(); // initialize? //NOSONAR
            beanDeclaredPropSetMethodPool.put(cls, unmodifiableSetMethodMap);

            Map<String, Method> existingSetMethodMap = beanPropSetMethodPool.get(cls);
            if (existingSetMethodMap == null) {
                final Map<String, Method> tmp = new ObjectPool<>(N.max(64, propSetMethodMap.size()));
                tmp.putAll(propSetMethodMap);
                beanPropSetMethodPool.put(cls, tmp);
            } else {
                existingSetMethodMap.putAll(propSetMethodMap);
            }

            final List<String> propNameList = new ArrayList<>(propFieldMap.keySet());

            for (final String propName : propGetMethodMap.keySet()) {
                if (!propNameList.contains(propName)) {
                    propNameList.add(propName);
                }
            }

            beanDeclaredPropNameListPool.put(cls, ImmutableList.wrap(propNameList));

            if (builderClass != null) {
                String propName = null;

                final Map<String, Method> builderPropSetMethodMap = new LinkedHashMap<>();

                for (final Method method : builderClass.getMethods()) {
                    if (Modifier.isPublic(method.getModifiers()) && !Object.class.equals(method.getDeclaringClass()) && method.getParameterCount() == 1
                            && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(builderClass))) {
                        propName = getPropNameByMethod(method);
                        builderPropSetMethodMap.put(propName, method);
                    }
                }

                final ImmutableMap<String, Method> unmodifiableBuilderPropSetMethodMap = ImmutableMap.wrap(builderPropSetMethodMap);
                //noinspection ResultOfMethodCallIgnored
                unmodifiableBuilderPropSetMethodMap.keySet(); // initialize? //NOSONAR
                beanDeclaredPropSetMethodPool.put(builderClass, unmodifiableBuilderPropSetMethodMap);

                final Map<String, Method> tmp = new ObjectPool<>(N.max(64, builderPropSetMethodMap.size()));
                tmp.putAll(builderPropSetMethodMap);
                beanPropSetMethodPool.put(builderClass, tmp);
            }
        }
    }

    private static Method getSetMethod(final Method getMethod) {
        final Class<?> declaringClass = getMethod.getDeclaringClass();
        final String getMethodName = getMethod.getName();

        final String setMethodName = SET
                + (getMethodName.substring(getMethodName.startsWith(IS) ? 2 : ((getMethodName.startsWith(HAS) || getMethodName.startsWith(GET)) ? 3 : 0)));

        Method setMethod = ClassUtil.lookupDeclaredMethod(declaringClass, setMethodName, getMethod.getReturnType());

        if (setMethod == null && getDeclaredField(declaringClass, getMethodName) != null) {
            setMethod = ClassUtil.lookupDeclaredMethod(declaringClass, getMethodName, getMethod.getReturnType());
        }

        return ((setMethod != null)
                && (void.class.equals(setMethod.getReturnType()) || setMethod.getReturnType().isAssignableFrom(setMethod.getDeclaringClass()))) ? setMethod
                        : null;
    }

    /**
     * Retrieves the declared field with the specified name from the given class.
     * {@code null} is returned if no field is found by the specified name.
     *
     * @param cls the class from which the field is to be retrieved
     * @param fieldName the name of the field to retrieve
     * @return the declared field with the specified name
     */
    private static Field getDeclaredField(final Class<?> cls, final String fieldName) {
        try {
            return cls.getDeclaredField(fieldName);
        } catch (NoSuchFieldException | SecurityException e) {
            // ignore
        }

        return null;
    }

    private static <T> Map<String, String> getPublicStaticStringFields(final Class<T> cls) {
        final Map<String, String> staticFinalFields = new HashMap<>();

        for (final Field field : cls.getFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                    && String.class.equals(field.getType())) {
                String value;

                try {
                    value = (String) field.get(null);
                    staticFinalFields.put(value, value);
                } catch (final Exception e) {
                    // ignore. should never happen
                }
            }
        }

        return staticFinalFields;
    }

    /**
     * Retrieves the field associated with the specified property name from the given class.
     *
     * <p>This method searches for fields that correspond to JavaBean property names,
     * handling various naming conventions and transformations.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Field nameField = Beans.getPropField(User.class, "name");
     * // Returns the field backing the "name" property
     *
     * Field idField = Beans.getPropField(User.class, "ID");
     * // Case-insensitive matching finds "id" field
     * }</pre>
     *
     * @param cls the class from which the field is to be retrieved
     * @param propName the name of the property whose field is to be retrieved
     * @return the field associated with the specified property name, or {@code null} if no field is found
     * @throws IllegalArgumentException if the class is not a bean class
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Field getPropField(final Class<?> cls, final String propName) {
        Map<String, Field> propFieldMap = beanPropFieldPool.get(cls);

        if (propFieldMap == null) {
            Beans.loadPropGetSetMethodList(cls);
            propFieldMap = beanPropFieldPool.get(cls);
        }

        Field field = propFieldMap.get(propName);

        if (field == null) {
            if (!Beans.isBeanClass(cls)) {
                throw new IllegalArgumentException(
                        "No property getter/setter method or public field found in the specified bean: " + ClassUtil.getCanonicalClassName(cls));
            }

            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> getterMethodList = getPropGetMethods(cls);

                for (final String key : getterMethodList.keySet()) {
                    if (Beans.isPropName(cls, propName, key)) {
                        field = propFieldMap.get(key);

                        break;
                    }
                }

                if ((field == null) && !propName.equalsIgnoreCase(Beans.formalizePropName(propName))) {
                    field = getPropField(cls, Beans.formalizePropName(propName));
                }

                // set method mask to avoid a query next time.
                if (field == null) {
                    field = ClassUtil.FIELD_MASK;
                }

                //    } else {
                //       ClassUtil.setAccessibleQuietly(field, true);
                //    }

                propFieldMap.put(propName, field);
            }
        }

        return (field == ClassUtil.FIELD_MASK) ? null : field;
    }

    /**
     * Retrieves an immutable map of property fields for the specified class.
     *
     * <p>The map contains all fields that back bean properties, keyed by property name.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Field> fields = Beans.getPropFields(User.class);
     * for (Map.Entry<String, Field> entry : fields.entrySet()) {
     *     System.out.println(entry.getKey() + " -> " + entry.getValue().getType());
     * }
     * }</pre>
     *
     * @param cls the class whose property fields are to be retrieved
     * @return an immutable map of property fields for the specified class
     */
    public static ImmutableMap<String, Field> getPropFields(final Class<?> cls) {
        ImmutableMap<String, Field> getterMethodList = beanDeclaredPropFieldPool.get(cls);

        if (getterMethodList == null) {
            Beans.loadPropGetSetMethodList(cls);
            getterMethodList = beanDeclaredPropFieldPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property get method declared in the specified {@code cls}
     * with the specified property name {@code propName}.
     * {@code null} is returned if no method is found.
     *
     * <p>Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB specification</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Method getter = Beans.getPropGetMethod(User.class, "name");
     * // Returns the getName() method
     *
     * Object value = getter.invoke(userInstance);
     * }</pre>
     *
     * @param cls the class from which the property get method is to be retrieved
     * @param propName the name of the property whose get method is to be retrieved
     * @return the property get method declared in the specified class, or {@code null} if no method is found
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Method getPropGetMethod(final Class<?> cls, final String propName) {
        Map<String, Method> propGetMethodMap = beanPropGetMethodPool.get(cls);

        if (propGetMethodMap == null) {
            Beans.loadPropGetSetMethodList(cls);
            propGetMethodMap = beanPropGetMethodPool.get(cls);
        }

        Method method = propGetMethodMap.get(propName);

        if (method == null) {
            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> getterMethodList = getPropGetMethods(cls);

                for (final Map.Entry<String, Method> entry : getterMethodList.entrySet()) { //NOSONAR
                    if (Beans.isPropName(cls, propName, entry.getKey())) {
                        method = entry.getValue();

                        break;
                    }
                }

                if ((method == null) && !propName.equalsIgnoreCase(Beans.formalizePropName(propName))) {
                    method = getPropGetMethod(cls, Beans.formalizePropName(propName));
                }

                // set method mask to avoid query next time.
                if (method == null) {
                    method = ClassUtil.METHOD_MASK;
                }

                propGetMethodMap.put(propName, method);
            }
        }

        return (method == ClassUtil.METHOD_MASK) ? null : method;
    }

    /**
     * Retrieves an immutable map of property get methods for the specified class.
     *
     * <p>Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Method> getters = Beans.getPropGetMethods(User.class);
     * // Map: {"name" -> getName(), "age" -> getAge(), ...}
     * }</pre>
     *
     * @param cls the class from which the property get methods are to be retrieved
     * @return an immutable map of property getter methods for the specified class
     */
    public static ImmutableMap<String, Method> getPropGetMethods(final Class<?> cls) {
        ImmutableMap<String, Method> getterMethodList = beanDeclaredPropGetMethodPool.get(cls);

        if (getterMethodList == null) {
            Beans.loadPropGetSetMethodList(cls);
            getterMethodList = beanDeclaredPropGetMethodPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property set method declared in the specified {@code cls}
     * with the specified property name {@code propName}.
     * {@code null} is returned if no method is found.
     *
     * <p>Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Method setter = Beans.getPropSetMethod(User.class, "name");
     * // Returns the setName(String) method
     *
     * setter.invoke(userInstance, "John");
     * }</pre>
     *
     * @param cls the class from which the property set method is to be retrieved
     * @param propName the name of the property whose set method is to be retrieved
     * @return the property set method declared in the specified class, or {@code null} if no method is found
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Method getPropSetMethod(final Class<?> cls, final String propName) {
        Map<String, Method> propSetMethodMap = beanPropSetMethodPool.get(cls);

        if (propSetMethodMap == null) {
            Beans.loadPropGetSetMethodList(cls);
            propSetMethodMap = beanPropSetMethodPool.get(cls);
        }

        Method method = propSetMethodMap.get(propName);

        if (method == null) {
            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> setterMethodList = getPropSetMethods(cls);

                for (final String key : setterMethodList.keySet()) {
                    if (Beans.isPropName(cls, propName, key)) {
                        method = propSetMethodMap.get(key);

                        break;
                    }
                }

                if ((method == null) && !propName.equalsIgnoreCase(Beans.formalizePropName(propName))) {
                    method = getPropSetMethod(cls, Beans.formalizePropName(propName));
                }

                // set method mask to avoid a query next time.
                if (method == null) {
                    method = ClassUtil.METHOD_MASK;
                }

                propSetMethodMap.put(propName, method);
            }
        }

        return (method == ClassUtil.METHOD_MASK) ? null : method;
    }

    /**
     * Retrieves an immutable map of property set methods for the specified class.
     *
     * <p>Call registerXMLBindingClassForPropGetSetMethod first to retrieve the property
     * getter/setter method for the class/bean generated/wrote by JAXB specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Method> setters = Beans.getPropSetMethods(User.class);
     * // Map: {"name" -> setName(String), "age" -> setAge(int), ...}
     * }</pre>
     *
     * @param cls the class from which the property set methods are to be retrieved
     * @return an immutable map of property set methods for the specified class
     */
    public static ImmutableMap<String, Method> getPropSetMethods(final Class<?> cls) {
        ImmutableMap<String, Method> setterMethodList = beanDeclaredPropSetMethodPool.get(cls);

        if (setterMethodList == null) {
            Beans.loadPropGetSetMethodList(cls);
            setterMethodList = beanDeclaredPropSetMethodPool.get(cls);
        }

        return setterMethodList;
    }

    /**
     * Returns the value of the specified property by invoking the given getter method on the provided bean.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Method getName = User.class.getMethod("getName");
     * String name = Beans.getPropValue(userInstance, getName);
     * // Equivalent to: String name = userInstance.getName();
     * }</pre>
     *
     * @param <T> the type of the property value
     * @param bean the object from which the property value is to be retrieved
     * @param propGetMethod the method to be invoked to get the property value
     * @return the value of the specified property
     * @throws RuntimeException if the method cannot be invoked
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPropValue(final Object bean, final Method propGetMethod) {
        try {
            return (T) propGetMethod.invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Returns the value of the specified property by invoking the getter method associated with the given property name on the provided bean.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * String name = Beans.getPropValue(user, "name");  // Returns "John"
     * Integer age = Beans.getPropValue(user, "age");   // Returns 25
     * }</pre>
     *
     * @param <T> the type of the property value
     * @param bean the object from which the property value is to be retrieved
     * @param propName the name of the property whose value is to be retrieved
     * @return the value of the specified property
     * @throws IllegalArgumentException if the property cannot be found
     * @see #getPropValue(Object, Method)
     */
    public static <T> T getPropValue(final Object bean, final String propName) {
        return getPropValue(bean, propName, false);
    }

    /**
     * Returns the value of the specified property by invoking the getter method associated with the given property name on the provided bean.
     * If the property cannot be found and ignoreUnmatchedProperty is {@code true}, it returns {@code null}.
     *
     * <p>This method also supports nested properties using dot notation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", new Address("NYC"));
     *
     * // Simple property
     * String name = Beans.getPropValue(user, "name", false);  // "John"
     *
     * // Nested property
     * String city = Beans.getPropValue(user, "address.city", false);  // "NYC"
     *
     * // Non-existent property with ignore flag
     * Object value = Beans.getPropValue(user, "unknown", true);  // null
     * }</pre>
     *
     * @param <T> the type of the property value
     * @param bean the object from which the property value is to be retrieved
     * @param propName the name of the property whose value is to be retrieved
     * @param ignoreUnmatchedProperty if {@code true}, ignores unmatched properties and returns null
     * @return the value of the specified property, or {@code null} if the property is not found and ignoreUnmatchedProperty is true
     * @throws IllegalArgumentException if the specified property cannot be retrieved and ignoreUnmatchedProperty is false
     */
    @MayReturnNull
    public static <T> T getPropValue(final Object bean, final String propName, final boolean ignoreUnmatchedProperty) {
        // N.checkArgNotNull(bean, cs.bean);

        final Class<?> cls = bean.getClass();
        final ParserUtil.PropInfo propInfo = ParserUtil.getBeanInfo(cls).getPropInfo(propName);

        if (propInfo != null) {
            return propInfo.getPropValue(bean);
        }
        Map<String, List<Method>> inlinePropGetMethodMap = beanInlinePropGetMethodPool.get(cls);
        List<Method> inlinePropGetMethodQueue = null;

        if (inlinePropGetMethodMap == null) {
            inlinePropGetMethodMap = new ObjectPool<>(getPropNameList(cls).size());
            beanInlinePropGetMethodPool.put(cls, inlinePropGetMethodMap);
        } else {
            inlinePropGetMethodQueue = inlinePropGetMethodMap.get(propName);
        }

        if (inlinePropGetMethodQueue == null) {
            inlinePropGetMethodQueue = new ArrayList<>();

            final String[] strs = Splitter.with(PROP_NAME_SEPARATOR).splitToArray(propName);

            if (strs.length > 1) {
                Class<?> targetClass = cls;

                for (final String str : strs) {
                    final Method method = getPropGetMethod(targetClass, str);

                    if (method == null) {
                        inlinePropGetMethodQueue.clear();

                        break;
                    }

                    inlinePropGetMethodQueue.add(method);

                    targetClass = method.getReturnType();
                }
            }

            inlinePropGetMethodMap.put(propName, inlinePropGetMethodQueue);
        }

        if (inlinePropGetMethodQueue.size() == 0) {
            if (ignoreUnmatchedProperty) {
                return null;
            }
            throw new IllegalArgumentException(
                    "No property method found with property name: " + propName + " in class " + ClassUtil.getCanonicalClassName(cls));
        }
        final int len = inlinePropGetMethodQueue.size();
        Object propBean = bean;

        for (final Method method : inlinePropGetMethodQueue) {
            propBean = getPropValue(propBean, method);

            if (propBean == null) {
                return (T) N.defaultValueOf(inlinePropGetMethodQueue.get(len - 1).getReturnType());
            }
        }

        return (T) propBean;
    }

    /**
     * Sets the specified property value on the given bean by invoking the provided setter method.
     * If the property value is {@code null}, it sets the default value of the parameter type.
     * If the initial attempt to set the property value fails, it tries to convert the property value to the appropriate type and set it again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Method setName = User.class.getMethod("setName", String.class);
     * Beans.setPropValue(userInstance, setName, "John");
     * // Equivalent to: userInstance.setName("John");
     *
     * // With type conversion
     * Method setAge = User.class.getMethod("setAge", int.class);
     * Beans.setPropValue(userInstance, setAge, "25");  // String converted to int
     * }</pre>
     *
     * @param bean the object on which the property value is to be set
     * @param propSetMethod the method to be invoked to set the property value
     * @param propValue the value to be set to the property
     * @return the final value that was set to the property
     * @throws RuntimeException if the underlying method is inaccessible or the method is invoked with incorrect arguments or the underlying method throws an exception
     */
    public static Object setPropValue(final Object bean, final Method propSetMethod, Object propValue) {
        if (propValue == null) {
            propValue = N.defaultValueOf(propSetMethod.getParameterTypes()[0]);

            try {
                propSetMethod.invoke(bean, propValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } else {
            try {
                propSetMethod.invoke(bean, propValue);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            } catch (final Exception e) {
                if (logger.isWarnEnabled()) {
                    logger.warn("Failed to set value for field by method: {} in class: {} with value type {}", propSetMethod.getName(),
                            propSetMethod.getDeclaringClass().getName(), propValue.getClass().getName());
                }

                propValue = N.convert(propValue, ParserUtil.getBeanInfo(bean.getClass()).getPropInfo(propSetMethod.getName()).jsonXmlType);

                try {
                    propSetMethod.invoke(bean, propValue);
                } catch (IllegalAccessException | InvocationTargetException e2) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        }

        return propValue;
    }

    /**
     * Sets the specified property value on the given bean by invoking the setter method associated with the given property name.
     * If the property value is {@code null}, it sets the default value of the parameter type.
     * If the initial attempt to set the property value fails, it tries to convert the property value to the appropriate type and set it again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     * Beans.setPropValue(user, "name", "John");
     * Beans.setPropValue(user, "age", 25);
     * }</pre>
     *
     * @param bean the object on which the property value is to be set
     * @param propName the name of the property whose value is to be set
     * @param propValue the value to be set to the property
     * @throws IllegalArgumentException if the specified property cannot be set
     * @deprecated replaced by {@link ParserUtil.BeanInfo#setPropValue(Object, String, Object)}
     */
    @Deprecated
    public static void setPropValue(final Object bean, final String propName, final Object propValue) {
        setPropValue(bean, propName, propValue, false);
    }

    /**
     * Sets the specified property value on the given bean by invoking the setter method associated with the given property name.
     * If the property value is {@code null}, it sets the default value of the parameter type.
     * If the initial attempt to set the property value fails, it tries to convert the property value to the appropriate type and set it again.
     *
     * <p>This method supports nested properties and can handle JAXB-style collection properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     *
     * // Set simple property
     * Beans.setPropValue(user, "name", "John", false);
     *
     * // Ignore unmatched property
     * boolean set = Beans.setPropValue(user, "unknown", "value", true);  // Returns false
     *
     * // Set nested property
     * Beans.setPropValue(user, "address.city", "NYC", false);
     * }</pre>
     *
     * @param bean the object on which the property value is to be set
     * @param propName the name of the property whose value is to be set
     * @param propValue the value to be set to the property
     * @param ignoreUnmatchedProperty if {@code true}, ignores unmatched properties and returns false
     * @return {@code true} if the property value has been set, {@code false} otherwise
     * @throws IllegalArgumentException if the specified property cannot be set and ignoreUnmatchedProperty is false
     * @deprecated replaced by {@link ParserUtil.BeanInfo#setPropValue(Object, String, Object, boolean)}
     */
    @Deprecated
    public static boolean setPropValue(final Object bean, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty) {

        return ParserUtil.getBeanInfo(bean.getClass()).setPropValue(bean, propName, propValue, ignoreUnmatchedProperty);
    }

    /**
     * Sets the property value by invoking the getter method on the provided bean.
     * The returned type of the get method should be {@code Collection} or {@code Map}. 
     * And the specified property value and the returned value must be the same type.
     *
     * <p>This method is particularly useful for JAXB-style beans where collections
     * are exposed only through getter methods without corresponding setters.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For a JAXB bean with: List<String> getTags() { return tags; }
     * Method getTags = bean.getClass().getMethod("getTags");
     * List<String> newTags = Arrays.asList("tag1", "tag2");
     * Beans.setPropValueByGet(bean, getTags, newTags);
     * // The bean's tags list is cleared and populated with newTags
     * }</pre>
     *
     * @param bean the object on which the property value is to be set
     * @param propGetMethod the method to be invoked to get the property value
     * @param propValue the value to be set to the property
     * @throws IllegalArgumentException if the getter doesn't return a Collection or Map
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setPropValueByGet(final Object bean, final Method propGetMethod, final Object propValue) {
        if (propValue == null) {
            return;
        }

        final Object rt = ClassUtil.invokeMethod(bean, propGetMethod);

        if (rt instanceof Collection<?> c) {
            c.clear();
            c.addAll((Collection) propValue);
        } else if (rt instanceof Map<?, ?> m) {
            m.clear();
            m.putAll((Map) propValue);
        } else {
            throw new IllegalArgumentException("Failed to set property value by getter method '" + propGetMethod.getName() + "'");
        }
    }

    /**
     * Formalizes the given property name by converting it to camel case and
     * replacing any reserved keywords with their mapped values.
     * This method is designed for field/method/class/column/table names,
     * and both source and target strings are cached for performance.
     *
     * <p>The method performs the following transformations:</p>
     * <ul>
     *   <li>Converts underscore-separated names to camelCase</li>
     *   <li>Replaces reserved keywords (e.g., "class" becomes "clazz")</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String formal = Beans.formalizePropName("user_name");     // Returns "userName"
     * String formal2 = Beans.formalizePropName("first_name");   // Returns "firstName"
     * String formal3 = Beans.formalizePropName("class");        // Returns "clazz"
     * }</pre>
     *
     * @param str the property name to be formalized
     * @return the formalized property name
     */
    public static String formalizePropName(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String newPropName = formalizedPropNamePool.get(str);

        if (newPropName == null) {
            newPropName = Beans.toCamelCase(str);

            for (final Map.Entry<String, String> entry : keyWordMapper.entrySet()) { //NOSONAR
                if (entry.getKey().equalsIgnoreCase(newPropName)) {
                    newPropName = entry.getValue();

                    break;
                }
            }

            formalizedPropNamePool.put(str, newPropName);
        }

        return newPropName;
    }

    /**
     * Converts the given property name to camel case.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String camel = Beans.toCamelCase("user_name");        // Returns "userName"
     * String camel2 = Beans.toCamelCase("FIRST_NAME");      // Returns "firstName"
     * String camel3 = Beans.toCamelCase("address-line-1");  // Returns "addressLine1"
     * }</pre>
     *
     * @param str the property name to be converted
     * @return the camel case version of the property name
     */
    public static String toCamelCase(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String newPropName = camelCasePropNamePool.get(str);

        if (newPropName == null) {
            newPropName = Strings.toCamelCase(str);
            newPropName = NameUtil.getCachedName(newPropName);
            camelCasePropNamePool.put(str, newPropName);
        }

        return newPropName;
    }

    /**
     * Converts the given string to lower case with underscores.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String snake = Beans.toLowerCaseWithUnderscore("userName");    // Returns "user_name"
     * String snake2 = Beans.toLowerCaseWithUnderscore("FirstName");  // Returns "first_name"
     * String snake3 = Beans.toLowerCaseWithUnderscore("userID");     // Returns "user_id"
     * }</pre>
     *
     * @param str the string to be converted
     * @return the lower case version of the string with underscores
     */
    public static String toLowerCaseWithUnderscore(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String result = lowerCaseWithUnderscorePropNamePool.get(str);

        if (result == null) {
            result = Strings.toLowerCaseWithUnderscore(str);
            lowerCaseWithUnderscorePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * Converts the given string to upper case with underscores.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String upper = Beans.toUpperCaseWithUnderscore("userName");    // Returns "USER_NAME"
     * String upper2 = Beans.toUpperCaseWithUnderscore("firstName");  // Returns "FIRST_NAME"
     * String upper3 = Beans.toUpperCaseWithUnderscore("userID");     // Returns "USER_ID"
     * }</pre>
     *
     * @param str the string to be converted
     * @return the upper case version of the string with underscores
     */
    public static String toUpperCaseWithUnderscore(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String result = upperCaseWithUnderscorePropNamePool.get(str);

        if (result == null) {
            result = Strings.toUpperCaseWithUnderscore(str);
            upperCaseWithUnderscorePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * Converts the keys in the provided map to camel case.
     * This method modifies the map in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("user_name", "John");
     * map.put("first_name", "Jane");
     *
     * Beans.toCamelCase(map);
     * // map now contains: {"userName": "John", "firstName": "Jane"}
     * }</pre>
     *
     * @param props the map whose keys are to be converted to camel case
     */
    @SuppressWarnings("deprecation")
    public static void toCamelCase(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (final Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(Beans.toCamelCase(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    /**
     * Converts the keys in the provided map to lower case with underscores.
     * This method modifies the map in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("userName", "John");
     * map.put("firstName", "Jane");
     *
     * Beans.toLowerCaseWithUnderscore(map);
     * // map now contains: {"user_name": "John", "first_name": "Jane"}
     * }</pre>
     *
     * @param props the map whose keys are to be converted to lower case with underscores
     */
    @SuppressWarnings("deprecation")
    public static void toLowerCaseWithUnderscore(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (final Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(Beans.toLowerCaseWithUnderscore(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    /**
     * Converts the keys in the provided map to upper case with underscores.
     * This method modifies the map in-place.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("userName", "John");
     * map.put("firstName", "Jane");
     *
     * Beans.toUpperCaseWithUnderscore(map);
     * // map now contains: {"USER_NAME": "John", "FIRST_NAME": "Jane"}
     * }</pre>
     *
     * @param props the map whose keys are to be converted to upper case with underscores
     */
    @SuppressWarnings("deprecation")
    public static void toUpperCaseWithUnderscore(final Map<String, Object> props) {
        final Map<String, Object> tmp = Objectory.createLinkedHashMap();

        for (final Map.Entry<String, Object> entry : props.entrySet()) {
            tmp.put(Beans.toUpperCaseWithUnderscore(entry.getKey()), entry.getValue());
        }

        props.clear();
        props.putAll(tmp);

        Objectory.recycle(tmp);
    }

    /**
     * Converts a map into a bean object of the specified type.
     * This method takes a map where the keys are the property names and the values are the corresponding property values,
     * and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     * Unmatched properties from the specified map are ignored by default.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with a User bean class
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", 25);
     * userMap.put("email", "john@example.com");
     *
     * User user = Beans.map2Bean(userMap, User.class);
     * // user.getName() returns "John"
     * // user.getAge() returns 25
     * // user.getEmail() returns "john@example.com"
     * }</pre>
     *
     * @param <T> the type of the bean object to be returned.
     * @param m the map to be converted into a bean object.
     * @param targetType the type of the bean object to be returned.
     * @return a bean object of the specified type with its properties set to the values from the map.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class
     * @see #map2Bean(Map, boolean, boolean, Class)
     * @see #map2Bean(Map, Collection, Class)
     */
    public static <T> T map2Bean(final Map<String, Object> m, final Class<? extends T> targetType) {
        return map2Bean(m, false, true, targetType);
    }

    /**
     * Converts a map into a bean object of the specified type with control over {@code null} and unmatched properties.
     * This method takes a map where the keys are the property names and the values are the corresponding property values,
     * and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     * You can control whether {@code null} properties should be ignored and whether unmatched properties should cause an error.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with ignoring null properties
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", null);
     * userMap.put("unknownField", "value"); // This field doesn't exist in User class
     *
     * // Ignore null properties and unmatched properties
     * User user = Beans.map2Bean(userMap, true, true, User.class);
     * // user.getName() returns "John"
     * // user.getAge() remains unchanged (not set to null)
     * // unknownField is ignored
     *
     * // Don't ignore null, but throw exception for unmatched properties
     * User user2 = Beans.map2Bean(userMap, false, false, User.class);
     * // This would throw an exception due to "unknownField"
     * }</pre>
     *
     * @param <T> the type of the bean object to be returned.
     * @param m the map to be converted into a bean object.
     * @param ignoreNullProperty if {@code true}, {@code null} values in the map will not be set on the bean.
     * @param ignoreUnmatchedProperty if {@code true}, map entries with keys that don't match any bean property will be ignored; if {@code false}, an exception will be thrown.
     * @param targetType the type of the bean object to be returned.
     * @return a bean object of the specified type with its properties set to the values from the map, or {@code null} if the input map is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class, or if {@code ignoreUnmatchedProperty} is {@code false} and an unmatched property is encountered
     * @see #map2Bean(Map, Collection, Class)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T map2Bean(final Map<String, Object> m, final boolean ignoreNullProperty, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        if (m == null) {
            return null;
        }

        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        ParserUtil.PropInfo propInfo = null;

        String propName = null;
        Object propValue = null;

        for (final Map.Entry<String, Object> entry : m.entrySet()) {
            propName = entry.getKey();
            propValue = entry.getValue();

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, ignoreUnmatchedProperty);
            } else {
                if (propValue != null && propInfo.type.isBean() && Type.of(propValue.getClass()).isMap()) {
                    propInfo.setPropValue(result, map2Bean((Map<String, Object>) propValue, ignoreNullProperty, ignoreUnmatchedProperty, propInfo.clazz));
                } else {
                    propInfo.setPropValue(result, propValue);
                }
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Converts a map into a bean object of the specified type, including only selected properties.
     * This method takes a map where the keys are the property names and the values are the corresponding property values,
     * and transforms it into a bean object of the specified type.
     * Only the properties specified in selectPropNames will be set on the bean.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with selected properties
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", 25);
     * userMap.put("email", "john@example.com");
     * userMap.put("password", "secret");
     *
     * // Only include name and email
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * User user = Beans.map2Bean(userMap, selectedProps, User.class);
     * // user.getName() returns "John"
     * // user.getEmail() returns "john@example.com"
     * // user.getAge() and user.getPassword() remain unset
     * }</pre>
     *
     * @param <T> the type of the bean object to be returned.
     * @param m the map to be converted into a bean object.
     * @param selectPropNames a collection of property names to be included in the resulting bean objects.
     * @param targetType the type of the bean object to be returned.
     * @return a bean object of the specified type with its properties set to the values from the map, or {@code null} if the input map is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class
     */
    @MayReturnNull
    public static <T> T map2Bean(final Map<String, Object> m, final Collection<String> selectPropNames, final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        if (m == null) {
            return null;
        }

        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        ParserUtil.PropInfo propInfo = null;
        Object propValue = null;

        for (final String propName : selectPropNames) {
            propValue = m.get(propName);

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, false);
            } else {
                if (propValue != null && propInfo.type.isBean() && Type.of(propValue.getClass()).isMap()) {
                    propInfo.setPropValue(result, map2Bean((Map<String, Object>) propValue, propInfo.clazz));
                } else {
                    propInfo.setPropValue(result, propValue);
                }
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type.
     * Each map in the collection represents a bean object where the map's keys are the property names
     * and the values are the corresponding property values.
     * Unmatched properties from the maps are ignored by default.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with a list of user maps
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     *
     * Map<String, Object> user1 = new HashMap<>();
     * user1.put("name", "John");
     * user1.put("age", 25);
     * userMaps.add(user1);
     *
     * Map<String, Object> user2 = new HashMap<>();
     * user2.put("name", "Jane");
     * user2.put("age", 30);
     * userMaps.add(user2);
     *
     * List<User> users = Beans.map2Bean(userMaps, User.class);
     * // users.get(0).getName() returns "John"
     * // users.get(1).getName() returns "Jane"
     * }</pre>
     *
     * @param <T> the type of the bean objects to be returned.
     * @param mList the collection of maps to be converted into bean objects.
     * @param targetType the type of the bean objects to be returned.
     * @return a list of bean objects of the specified type with their properties set to the values from the corresponding map.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class
     * @see #map2Bean(Collection, Collection, Class)
     */
    public static <T> List<T> map2Bean(final Collection<? extends Map<String, Object>> mList, final Class<? extends T> targetType) {
        return map2Bean(mList, false, true, targetType);
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type with control over {@code null} and unmatched properties.
     * Each map in the collection represents a bean object where the map's keys are the property names
     * and the values are the corresponding property values.
     * The resulting list contains bean objects of the specified type with their properties set to the values from the corresponding map.
     * The ignoreNullProperty parameter allows the user to specify whether {@code null} properties should be ignored.
     * The ignoreUnmatchedProperty parameter allows the user to specify whether unmatched properties should be ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with null handling
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     *
     * Map<String, Object> user1 = new HashMap<>();
     * user1.put("name", "John");
     * user1.put("age", null);
     * user1.put("unknownField", "value");
     * userMaps.add(user1);
     *
     * // Ignore null values and unmatched properties
     * List<User> users = Beans.map2Bean(userMaps, true, true, User.class);
     * // users.get(0).getName() returns "John"
     * // users.get(0).getAge() remains unchanged (not set to null)
     * // unknownField is ignored
     * }</pre>
     *
     * @param <T> the type of the bean objects to be returned.
     * @param mList the collection of maps to be converted into bean objects.
     * @param ignoreNullProperty a boolean that determines whether {@code null} properties should be ignored.
     * @param ignoreUnmatchedProperty a boolean that determines whether unmatched properties should be ignored.
     * @param targetType the type of the bean objects to be returned.
     * @return a list of bean objects of the specified type with their properties set to the values from the corresponding map.
     */
    public static <T> List<T> map2Bean(final Collection<? extends Map<String, Object>> mList, final boolean ignoreNullProperty,
            final boolean ignoreUnmatchedProperty, final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (final Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, ignoreNullProperty, ignoreUnmatchedProperty, targetType));
        }

        return beanList;
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type, including only selected properties.
     * This method takes a collection of maps where each map represents a bean object.
     * The keys in the map are the property names and the values are the corresponding property values.
     * Only the properties specified in selectPropNames will be set on the beans.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with selected properties
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     *
     * Map<String, Object> user1 = new HashMap<>();
     * user1.put("name", "John");
     * user1.put("age", 25);
     * user1.put("email", "john@example.com");
     * user1.put("password", "secret1");
     * userMaps.add(user1);
     *
     * // Only include name and email
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * List<User> users = Beans.map2Bean(userMaps, selectedProps, User.class);
     * // users.get(0).getName() returns "John"
     * // users.get(0).getEmail() returns "john@example.com"
     * // age and password remain unset
     * }</pre>
     *
     * @param <T> the type of the bean objects to be returned.
     * @param mList the collection of maps to be converted into bean objects.
     * @param selectPropNames a collection of property names to be included in the resulting bean objects. If this is empty, all properties are included.
     * @param targetType the type of the bean objects to be returned.
     * @return a list of bean objects of the specified type with their properties set to the values from the corresponding map.
     */
    public static <T> List<T> map2Bean(final Collection<? extends Map<String, Object>> mList, final Collection<String> selectPropNames,
            final Class<? extends T> targetType) {
        checkBeanClass(targetType);

        final List<T> beanList = new ArrayList<>(mList.size());

        for (final Map<String, Object> m : mList) {
            beanList.add(map2Bean(m, selectPropNames, targetType));
        }

        return beanList;
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The resulting map is a LinkedHashMap to preserve the order of properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with a User bean
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     *
     * Map<String, Object> userMap = Beans.bean2Map(user);
     * // userMap: {name=John, age=25, email=john@example.com}
     * }</pre>
     *
     * @param bean the bean object to be converted into a map.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean) {
        return bean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map using the provided map supplier.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The map supplier function determines the type of the map to be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with a custom map type
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     *
     * // Using TreeMap to get sorted properties
     * TreeMap<String, Object> sortedMap = Beans.bean2Map(user, TreeMap::new);
     * // sortedMap: {age=25, name=John} (sorted by key)
     *
     * // Using HashMap for better performance
     * HashMap<String, Object> hashMap = Beans.bean2Map(user, HashMap::new);
     * // hashMap: {name=John, age=25} (order not guaranteed)
     * }</pre>
     *
     * @param <M> the type of the resulting Map.
     * @param bean the bean object to be converted into a map.
     * @param mapSupplier a function that generates a new Map instance.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     * The resulting map is a LinkedHashMap to preserve the order of properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with selected properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     * user.setPassword("secret");
     *
     * // Only include name and email
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * Map<String, Object> userMap = Beans.bean2Map(user, selectedProps);
     * // userMap: {name=John, email=john@example.com}
     * // age and password are not included
     * }</pre>
     *
     * @param bean the bean object to be converted into a map.
     * @param selectPropNames a collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @return a map where the keys are the selected property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final Collection<String> selectPropNames) {
        return bean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     * The map supplier function determines the type of the map to be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with selected properties and custom map type
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     *
     * // Only include name and age, using TreeMap
     * Collection<String> selectedProps = Arrays.asList("name", "age");
     * TreeMap<String, Object> sortedMap = Beans.bean2Map(user, selectedProps, TreeMap::new);
     * // sortedMap: {age=25, name=John} (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the resulting Map.
     * @param bean the bean object to be converted into a map.
     * @param selectPropNames a collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @param mapSupplier a function that generates a new Map instance.
     * @return a map where the keys are the selected property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with naming policy
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     *
     * // Convert to snake_case
     * Collection<String> props = Arrays.asList("firstName", "lastName");
     * Map<String, Object> snakeMap = Beans.bean2Map(user, props, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, HashMap::new);
     * // snakeMap: {first_name=John, last_name=Doe}
     *
     * // Convert to UPPER_CASE
     * Map<String, Object> upperMap = Beans.bean2Map(user, props, NamingPolicy.UPPER_CASE, HashMap::new);
     * // upperMap: {FIRSTNAME=John, LASTNAME=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map.
     * @param selectPropNames the collection of property names to be included in the map.
     * @param keyNamingPolicy the naming policy to be used for the keys in the map.
     * @param mapSupplier the supplier function to create a new instance of the map.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2Map(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into the provided output map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * All properties of the bean are included in the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with existing map
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     *
     * Beans.bean2Map(user, existingMap);
     * // existingMap: {id=123, name=John, age=25}
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map.
     * @param output the map into which the bean's properties will be put.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final M output) {
        bean2Map(bean, null, output);
    }

    /**
     * Converts a bean object into the provided output map, selecting only specified properties.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the selectPropNames collection are added to the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with existing map and selected properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * user.setEmail("john@example.com");
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     *
     * Collection<String> selectedProps = Arrays.asList("name", "email");
     * Beans.bean2Map(user, selectedProps, existingMap);
     * // existingMap: {id=123, name=John, email=john@example.com}
     * // age is not included
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map.
     * @param selectPropNames a collection of property names to be included in the map. If this is {@code null}, all properties are included.
     * @param output the map into which the bean's properties will be put.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final Collection<String> selectPropNames, final M output) {
        bean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     * The output map is provided as a parameter and will be filled with the bean's properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with naming policy and output map
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     *
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Collection<String> props = Arrays.asList("firstName", "lastName");
     *
     * // Convert property names to snake_case
     * Beans.bean2Map(user, props, NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, outputMap);
     * // outputMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map.
     * @param selectPropNames the set of property names to be included during the conversion.
     * @param keyNamingPolicy the naming policy to be used for the keys in the map.
     * @param output the map into which the bean's properties will be put.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2Map(bean, true, null, keyNamingPolicy, output);
        } else {
            ParserUtil.PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass); //NOSONAR
                }

                propValue = propInfo.getPropValue(bean);

                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, propValue);
                } else {
                    output.put(keyNamingPolicy.convert(propName), propValue);
                }
            }
        }
    }

    /**
     * Converts a bean object into a map with optional {@code null} property filtering.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with null property filtering
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     *
     * // Include null properties
     * Map<String, Object> mapWithNulls = Beans.bean2Map(user, false);
     * // mapWithNulls: {name=John, age=null, email=john@example.com}
     *
     * // Ignore null properties
     * Map<String, Object> mapWithoutNulls = Beans.bean2Map(user, true);
     * // mapWithoutNulls: {name=John, email=john@example.com}
     * }</pre>
     *
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty) {
        return bean2Map(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a map with optional {@code null} property filtering and property exclusion.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with null filtering and ignored properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     * user.setPassword("secret");
     *
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     *
     * // Ignore null properties and password field
     * Map<String, Object> filteredMap = Beans.bean2Map(user, true, ignoredProps);
     * // filteredMap: {name=John, email=john@example.com}
     * // age (null) and password (ignored) are not included
     * }</pre>
     *
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts a bean object into a map with optional {@code null} property filtering and property exclusion.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     * The map is created by the provided <i>mapSupplier</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with custom map type
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     *
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     *
     * // Create TreeMap ignoring null properties
     * TreeMap<String, Object> sortedMap = Beans.bean2Map(user, true, ignoredProps, TreeMap::new);
     * // sortedMap: {email=john@example.com, name=John} (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param mapSupplier a function that returns a new map.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a map with optional {@code null} property filtering, property exclusion, and key naming policy.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     * The keys of the map are formatted according to the provided <i>keyNamingPolicy</i>.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with naming policy
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * user.setAge(null);
     *
     * Set<String> ignoredProps = new HashSet<>();
     *
     * // Convert to snake_case, ignoring null properties
     * Map<String, Object> snakeMap = Beans.bean2Map(user, true, ignoredProps,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE);
     * // snakeMap: {first_name=John, last_name=Doe}
     * // age is not included because it's null
     * }</pre>
     *
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param keyNamingPolicy the policy used to name the keys in the map.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static Map<String, Object> bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Properties can be filtered based on {@code null} values, excluded by name, and keys can be transformed using a naming policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with all options
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * user.setAge(null);
     * user.setPassword("secret");
     *
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     *
     * // Create custom map with snake_case keys, ignoring nulls and password
     * TreeMap<String, Object> customMap = Beans.bean2Map(user, true, ignoredProps,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, TreeMap::new);
     * // customMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames the set of property names to be ignored during the conversion.
     * @param keyNamingPolicy the naming policy to be used for the keys in the map.
     * @param mapSupplier the supplier function to create a new instance of the map.
     * @return a map where the keys are the property names of the bean and the values are the corresponding property values of the bean.
     */
    public static <M extends Map<String, Object>> M bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        bean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * The result is stored in the provided output map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with existing map and null filtering
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     *
     * // Add properties to existing map, ignoring nulls
     * Beans.bean2Map(user, true, existingMap);
     * // existingMap: {id=123, name=John, email=john@example.com}
     * // age is not included because it's null
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param output the map where the result should be stored.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final M output) {
        bean2Map(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with existing map, null filtering, and ignored properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(null);
     * user.setEmail("john@example.com");
     * user.setPassword("secret");
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     *
     * Beans.bean2Map(user, true, ignoredProps, existingMap);
     * // existingMap: {name=John, email=john@example.com}
     * // age (null) and password (ignored) are not included
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion. If this is {@code null}, no properties are ignored.
     * @param output the map where the result should be stored.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        bean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The keys are named according to the provided naming policy.
     * The output map is provided as a parameter and will be filled with the bean's properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with all options and output map
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * user.setAge(null);
     * user.setPassword("secret");
     *
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("password"));
     *
     * // Fill output map with snake_case keys, ignoring nulls and password
     * Beans.bean2Map(user, true, ignoredProps,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, outputMap);
     * // outputMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * @param ignoredPropNames the set of property names to be ignored during the conversion.
     * @param keyNamingPolicy the naming policy to be used for the keys in the map.
     * @param output the map to be filled with the bean's properties.
     */
    public static <M extends Map<String, Object>> void bean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            NamingPolicy keyNamingPolicy, final M output) {
        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (final ParserUtil.PropInfo propInfo : beanInfo.propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if (isLowerCamelCaseOrNoChange) {
                output.put(propName, propValue);
            } else {
                output.put(keyNamingPolicy.convert(propName), propValue);
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The resulting map uses LinkedHashMap to preserve property order.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with nested beans
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * address.setZipCode("10001");
     * user.setAddress(address);
     *
     * Map<String, Object> deepMap = Beans.deepBean2Map(user);
     * // deepMap: {
     * //   name=John,
     * //   address={city=New York, zipCode=10001}
     * // }
     * // Note: address is converted to a Map, not kept as Address object
     * }</pre>
     *
     * @param bean the bean to be converted into a Map.
     * @return a Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean) {
        return deepBean2Map(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The map type is determined by the provided mapSupplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with custom map type
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     *
     * // Using TreeMap for sorted keys
     * TreeMap<String, Object> sortedDeepMap = Beans.deepBean2Map(user, TreeMap::new);
     * // sortedDeepMap: {
     * //   address={city=New York},
     * //   name=John
     * // } (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the Map to which the bean will be converted.
     * @param bean the bean to be converted into a Map.
     * @param mapSupplier a supplier function to create the Map instance.
     * @return a Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, null, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with selected properties
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     *
     * Collection<String> props = Arrays.asList("name", "address");
     * Map<String, Object> selectedDeepMap = Beans.deepBean2Map(user, props);
     * // selectedDeepMap: {
     * //   name=John,
     * //   address={city=New York}
     * // }
     * // age is not included
     * }</pre>
     *
     * @param bean the bean to be converted into a Map.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     * @return a Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final Collection<String> selectPropNames) {
        return deepBean2Map(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included, and the map type is determined by mapSupplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with selected properties and custom map
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * address.setZipCode("10001");
     * user.setAddress(address);
     *
     * Collection<String> props = Arrays.asList("name", "address");
     * HashMap<String, Object> customDeepMap = Beans.deepBean2Map(user, props, HashMap::new);
     * // customDeepMap: {
     * //   name=John,
     * //   address={city=New York, zipCode=10001}
     * // }
     * }</pre>
     *
     * @param <M> the type of the Map to which the bean will be converted.
     * @param bean the bean to be converted into a Map.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     * @param mapSupplier a supplier function to create the Map instance.
     * @return a Map representation of the provided bean.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The keys in the map are transformed according to the specified naming policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with naming policy
     * User user = new User();
     * user.setFirstName("John");
     * Address address = new Address();
     * address.setStreetName("Main St");
     * user.setHomeAddress(address);
     *
     * Collection<String> props = Arrays.asList("firstName", "homeAddress");
     * Map<String, Object> snakeMap = Beans.deepBean2Map(user, props,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, HashMap::new);
     * // snakeMap: {
     * //   first_name=John,
     * //   home_address={street_name=Main St}
     * // }
     * // Note: nested properties are also converted
     * }</pre>
     *
     * @param <M> the type of the Map to which the bean will be converted.
     * @param bean the bean to be converted into a Map.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     * @param keyNamingPolicy the naming policy to be used for the keys in the resulting Map.
     * @param mapSupplier a supplier function to create the Map instance into which the bean properties will be put.
     * @return a Map representation of the provided bean.
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? getPropNameList(bean.getClass()).size() : selectPropNames.size());

        deepBean2Map(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * All properties are included and stored in the provided output map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with output map
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     *
     * Beans.deepBean2Map(user, existingMap);
     * // existingMap: {
     * //   id=123,
     * //   name=John,
     * //   address={city=New York}
     * // }
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean to be converted into a Map.
     * @param output the map into which the bean's properties will be put.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final M output) {
        deepBean2Map(bean, null, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included and stored in the provided output map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with selected properties and output map
     * User user = new User();
     * user.setName("John");
     * user.setAge(25);
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     *
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Collection<String> props = Arrays.asList("name", "address");
     *
     * Beans.deepBean2Map(user, props, outputMap);
     * // outputMap: {
     * //   name=John,
     * //   address={city=New York}
     * // }
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean to be converted into a Map.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     * @param output the map into which the bean's properties will be put.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final Collection<String> selectPropNames, final M output) {
        deepBean2Map(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included, keys are transformed according to the naming policy, and results are stored in the output map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with all options
     * User user = new User();
     * user.setFirstName("John");
     * Address address = new Address();
     * address.setStreetName("Main St");
     * user.setHomeAddress(address);
     *
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Collection<String> props = Arrays.asList("firstName", "homeAddress");
     *
     * Beans.deepBean2Map(user, props,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORE, outputMap);
     * // outputMap: {
     * //   first_name=John,
     * //   home_address={street_name=Main St}
     * // }
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean to be converted into a Map.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     * @param keyNamingPolicy the naming policy to be used for the keys in the resulting Map.
     * @param output the map into which the bean's properties will be put.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        if (bean == null) {
            return;
        }

        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            deepBean2Map(bean, true, null, keyNamingPolicy, output);
        } else {
            ParserUtil.PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, deepBean2Map(propValue, true, null, keyNamingPolicy));
                    } else {
                        output.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, true, null, keyNamingPolicy));
                    }
                }
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Properties with {@code null} values will be included in the resulting Map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a User bean with nested Address
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * Map<String, Object> result = deepBean2Map(user, false);
     * // result: {name=John, age=25, address={city=NYC, zipCode=10001}}
     *
     * // With ignoreNullProperty=true
     * User userWithNull = new User("Jane", null, null);
     * Map<String, Object> filtered = deepBean2Map(userWithNull, true);
     * // filtered: {name=Jane} (null properties excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map. Can be any Java object with getter/setter methods.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @return a Map representation of the bean where nested beans are recursively converted to Maps.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty) {
        return deepBean2Map(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Properties whose names are in the ignoredPropNames set will be excluded from the conversion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a User bean with multiple properties
     * User user = new User("John", 25, "john@example.com", new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("email", "age"));
     * Map<String, Object> result = deepBean2Map(user, false, ignored);
     * // result: {name=John, address={city=NYC}} (email and age excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process. Can be {@code null}.
     * @return a Map representation of the bean with specified properties excluded.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The resulting Map type can be customized using the mapSupplier function.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a TreeMap instead of default LinkedHashMap
     * User user = new User("John", 25, new Address("NYC"));
     * TreeMap<String, Object> result = deepBean2Map(user, false, null,
     *     size -> new TreeMap<>());
     * // result: TreeMap with {address={city=NYC}, age=25, name=John} (sorted keys)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a Map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a Map of the specified type containing the bean properties.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The keys in the resulting Map can be transformed according to the specified naming policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a bean with camelCase properties
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     *
     * Map<String, Object> snakeCase = deepBean2Map(user, false, null,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
     * // snakeCase: {first_name=John, last_name=Doe}
     *
     * Map<String, Object> upperCase = deepBean2Map(user, false, null,
     *     NamingPolicy.UPPER_CASE);
     * // upperCase: {FIRSTNAME=John, LASTNAME=Doe}
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting Map. If {@code null}, defaults to LOWER_CAMEL_CASE.
     * @return a Map representation of the bean with keys transformed according to the naming policy.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Provides full control over the conversion process including naming policy and Map type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom conversion with all options
     * User user = new User("John", null, new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalId"));
     *
     * LinkedHashMap<String, Object> result = deepBean2Map(user, true, ignored,
     *     NamingPolicy.UPPER_CASE_WITH_UNDERSCORES,
     *     size -> new LinkedHashMap<>(size));
     * // result: {NAME=John, ADDRESS={CITY=NYC}} (ordered, uppercase with underscores)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a Map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting Map.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a Map of the specified type with full customization applied.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The conversion is performed in-place into the provided output Map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Reuse existing map
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("timestamp", System.currentTimeMillis());
     *
     * User user = new User("John", 25);
     * deepBean2Map(user, false, existingMap);
     * // existingMap now contains: {timestamp=..., name=John, age=25}
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a Map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param output the Map instance into which the bean properties will be put. Existing entries are preserved.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final M output) {
        deepBean2Map(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Properties whose names are in the ignoredPropNames set will be excluded from the conversion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Populate map with selective properties
     * Map<String, Object> output = new HashMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("password", "ssn"));
     *
     * User user = new User("John", "pass123", "123-45-6789");
     * deepBean2Map(user, false, ignored, output);
     * // output: {name=John} (sensitive fields excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a Map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     * @param output the Map instance into which the bean properties will be put.
     * @see #deepBean2Map(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        deepBean2Map(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The conversion process can be customized by specifying properties to ignore, whether to ignore {@code null} properties, and the naming policy for keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full control over in-place conversion
     * Map<String, Object> output = new TreeMap<>(); // Sorted map
     * Set<String> ignored = new HashSet<>(Arrays.asList("id"));
     *
     * Product product = new Product("Widget", 29.99, new Category("Electronics"));
     * deepBean2Map(product, true, ignored, NamingPolicy.UPPER_CASE, output);
     * // output: {CATEGORY={NAME=Electronics}, NAME=Widget, PRICE=29.99} (sorted)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a Map.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     * @param keyNamingPolicy the naming policy to apply to the keys in the output Map.
     * @param output the Map instance into which the bean properties will be put.
     */
    public static <M extends Map<String, Object>> void deepBean2Map(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        if (bean == null) {
            return;
        }

        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        String propName = null;
        Object propValue = null;

        for (final ParserUtil.PropInfo propInfo : beanInfo.propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, propValue);
                } else {
                    output.put(keyNamingPolicy.convert(propName), propValue);
                }
            } else {
                if (isLowerCamelCaseOrNoChange) {
                    output.put(propName, deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                } else {
                    output.put(keyNamingPolicy.convert(propName), deepBean2Map(propValue, ignoreNullProperty, null, keyNamingPolicy));
                }
            }
        }
    }

    /**
     * Converts a bean object into a flat map representation where nested properties are represented with dot notation.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * All properties from the bean are included in the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given nested beans
     * User user = new User("John", new Address("NYC", "10001"));
     * Map<String, Object> flat = bean2FlatMap(user);
     * // flat: {name=John, address.city=NYC, address.zipCode=10001}
     *
     * // Deep nesting
     * Company company = new Company("TechCorp",
     *     new Address("NYC", new Location(40.7128, -74.0060)));
     * Map<String, Object> result = bean2FlatMap(company);
     * // result: {name=TechCorp, address.city=NYC,
     * //          address.location.latitude=40.7128,
     * //          address.location.longitude=-74.0060}
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map.
     * @return a map representing the bean object with nested properties flattened using dot notation.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean) {
        return bean2FlatMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation where nested properties are represented with dot notation.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * The type of Map returned can be customized using the mapSupplier.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a sorted flat map
     * User user = new User("John", new Address("NYC", "10001"));
     * TreeMap<String, Object> sortedFlat = bean2FlatMap(user,
     *     size -> new TreeMap<>());
     * // sortedFlat: {address.city=NYC, address.zipCode=10001, name=John} (sorted)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with nested properties flattened.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included in the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select specific properties including nested ones
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * Collection<String> select = Arrays.asList("name", "address");
     * Map<String, Object> result = bean2FlatMap(user, select);
     * // result: {name=John, address.city=NYC, address.zipCode=10001}
     *
     * // Select only top-level properties
     * Collection<String> topLevel = Arrays.asList("name", "age");
     * Map<String, Object> flat = bean2FlatMap(user, topLevel);
     * // flat: {name=John, age=25} (address excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map.
     * @param selectPropNames a collection of property names to be included in the resulting map. Nested properties of selected beans are automatically included.
     * @return a map with only the selected properties flattened.
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final Collection<String> selectPropNames) {
        return bean2FlatMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties and custom Map type.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines property selection with Map type customization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select properties and use custom map type
     * Employee emp = new Employee("John", "IT", new Manager("Jane"));
     * Collection<String> select = Arrays.asList("name", "manager");
     *
     * LinkedHashMap<String, Object> result = bean2FlatMap(emp, select,
     *     size -> new LinkedHashMap<>(size));
     * // result: {name=John, manager.name=Jane} (ordered, dept excluded)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map.
     * @param selectPropNames a collection of property names to be included in the resulting map.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with selected properties flattened.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and a specified naming policy.
     * This method takes a bean object and transforms it into a map where the keys are the property names of the bean and the values are the corresponding property values.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With naming policy transformation
     * User user = new User();
     * user.setFirstName("John");
     * user.setHomeAddress(new Address("NYC"));
     *
     * Collection<String> select = Arrays.asList("firstName", "homeAddress");
     * Map<String, Object> snakeCase = bean2FlatMap(user, select,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES,
     *     size -> new HashMap<>(size));
     * // snakeCase: {first_name=John, home_address.city=NYC}
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a flat map.
     * @param selectPropNames a collection of property names to be included in the resulting map. If this is empty, all properties are included.
     * @param keyNamingPolicy the naming policy for the keys in the resulting map.
     * @param mapSupplier a function that generates a new map instance. The function argument is the initial map capacity.
     * @return a map representing the bean object. Each key-value pair in the map corresponds to a property of the bean.
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final M output = mapSupplier.apply(N.isEmpty(selectPropNames) ? getPropNameList(bean.getClass()).size() : selectPropNames.size());

        bean2FlatMap(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * All properties from the bean are included in the output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Populate existing map with flattened bean
     * Map<String, Object> output = new HashMap<>();
     * output.put("version", "1.0");
     *
     * User user = new User("John", new Address("NYC"));
     * bean2FlatMap(user, output);
     * // output: {version=1.0, name=John, address.city=NYC}
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map.
     * @param output the Map instance into which the flattened bean properties will be put. Existing entries are preserved.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final M output) {
        bean2FlatMap(bean, null, output);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Selective flattening into existing map
     * Map<String, Object> output = new LinkedHashMap<>();
     * Collection<String> select = Arrays.asList("name", "contact");
     *
     * Customer customer = new Customer("John", "123-456",
     *     new Contact("john@email.com", "555-1234"));
     * bean2FlatMap(customer, select, output);
     * // output: {name=John, contact.email=john@email.com, contact.phone=555-1234}
     * // (customerId excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map.
     * @param selectPropNames a collection of property names to be included in the output map.
     * @param output the Map instance into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final Collection<String> selectPropNames, final M output) {
        bean2FlatMap(bean, selectPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation with full customization options and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides control over property selection and key naming policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full customization of flattening process
     * Map<String, Object> output = new TreeMap<>();
     * Collection<String> select = Arrays.asList("productName", "category");
     *
     * Product product = new Product("WidgetPro",
     *     new Category("Electronics", "Gadgets"));
     * bean2FlatMap(product, select, NamingPolicy.UPPER_CASE_WITH_UNDERSCORE, output);
     * // output: {CATEGORY.NAME=Electronics, CATEGORY.SUBCATEGORY=Gadgets,
     * //          PRODUCT_NAME=WidgetPro} (sorted, uppercase)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map.
     * @param selectPropNames a collection of property names to be included in the output map.
     * @param keyNamingPolicy the naming policy to apply to the keys in the output map.
     * @param output the Map instance into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.LOWER_CAMEL_CASE : keyNamingPolicy;
        final boolean isLowerCamelCaseOrNoChange = NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (N.isEmpty(selectPropNames)) {
            bean2FlatMap(bean, true, null, keyNamingPolicy, output);
        } else {
            ParserUtil.PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                propValue = propInfo.getPropValue(bean);

                if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    bean2FlatMap(propValue, true, null, keyNamingPolicy, isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), output);
                }
            }
        }
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} property handling.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Properties with {@code null} values can be included or excluded based on the ignoreNullProperty parameter.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Include null properties
     * User user = new User("John", null, new Address("NYC", null));
     * Map<String, Object> withNulls = bean2FlatMap(user, false);
     * // withNulls: {name=John, age=null, address.city=NYC, address.zipCode=null}
     *
     * // Exclude null properties
     * Map<String, Object> noNulls = bean2FlatMap(user, true);
     * // noNulls: {name=John, address.city=NYC}
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @return a flat map representation of the bean with {@code null} handling as specified.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty) {
        return bean2FlatMap(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} property handling and property exclusion.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines {@code null} value filtering with property name exclusion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter nulls and exclude specific properties
     * User user = new User("John", null, "secret123", new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("password"));
     *
     * Map<String, Object> result = bean2FlatMap(user, true, ignored);
     * // result: {name=John, address.city=NYC}
     * // (age is null so excluded, password is in ignored set)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     * @return a flat map with specified filtering applied.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE);
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} handling, property exclusion, and Map type.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides flexibility in filtering and Map implementation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom map with filtering
     * Employee emp = new Employee("John", null, "IT", new Office("Building A"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("department"));
     *
     * TreeMap<String, Object> result = bean2FlatMap(emp, true, ignored,
     *     size -> new TreeMap<>());
     * // result: {name=John, office.building=Building A}
     * // (sorted, salary null excluded, department ignored)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with filtering applied.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} handling, property exclusion, and key naming policy.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides comprehensive control over the flattening process.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Transform to snake_case with filtering
     * UserProfile profile = new UserProfile();
     * profile.setFirstName("John");
     * profile.setLastLogin(null);
     * profile.setHomeAddress(new Address("NYC"));
     *
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalId"));
     * Map<String, Object> result = bean2FlatMap(profile, true, ignored,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
     * // result: {first_name=John, home_address.city=NYC}
     * // (last_login null excluded, internal_id ignored, snake_case keys)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting map.
     * @return a flat map with comprehensive customization applied.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation with full control over all conversion aspects.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This is the most flexible variant offering complete customization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Complete customization example
     * Order order = new Order("ORD-123", null,
     *     new Customer("John", new Address("NYC", "10001")));
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalNotes"));
     *
     * LinkedHashMap<String, Object> result = bean2FlatMap(order, true, ignored,
     *     NamingPolicy.UPPER_CASE_WITH_UNDERSCORES,
     *     size -> new LinkedHashMap<>(size * 2)); // larger initial capacity
     * // result: {ORDER_ID=ORD-123, CUSTOMER.NAME=John,
     * //          CUSTOMER.ADDRESS.CITY=NYC, CUSTOMER.ADDRESS.ZIP_CODE=10001}
     * // (amount null excluded, internal_notes ignored, ordered map)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting map.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a fully customized flat map representation of the bean.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = getPropNameList(bean.getClass()).size();
        final int initCapacity = beanPropNameSize - N.size(ignoredPropNames);

        final M output = mapSupplier.apply(initCapacity);

        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with {@code null} handling.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This is an in-place operation that modifies the provided output Map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Populate existing map with null filtering
     * Map<String, Object> output = new HashMap<>();
     * output.put("timestamp", new Date());
     *
     * User user = new User("John", null, new Address("NYC"));
     * bean2FlatMap(user, true, output);
     * // output: {timestamp=..., name=John, address.city=NYC}
     * // (age null is excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param output the map into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final M output) {
        bean2FlatMap(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with filtering options.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines in-place operation with {@code null} handling and property exclusion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // In-place population with multiple filters
     * Map<String, Object> output = new LinkedHashMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("password", "ssn"));
     *
     * Account account = new Account("john123", "pass", null, "123-45-6789");
     * bean2FlatMap(account, true, ignored, output);
     * // output: {username=john123}
     * // (password and ssn ignored, balance null excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames a set of property names to be excluded from the output map.
     * @param output the map into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.LOWER_CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with full customization.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This method provides complete control over the in-place flattening operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full control in-place flattening
     * Map<String, Object> output = new TreeMap<>(); // sorted output
     * Set<String> ignored = new HashSet<>(Arrays.asList("metadata"));
     *
     * Document doc = new Document("Report", null,
     *     new Author("John", new Department("Research")));
     * bean2FlatMap(doc, true, ignored,
     *     NamingPolicy.LOWER_CASE_WITH_UNDERSCORES, output);
     * // output: {author.department.name=research, author.name=john, title=report}
     * // (sorted keys, snake_case, version null excluded, metadata ignored)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames a set of property names to be excluded from the output map.
     * @param keyNamingPolicy the naming policy to apply to the keys in the output map.
     * @param output the map into which the flattened bean properties will be put.
     * @see #bean2FlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        bean2FlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, null, output);
    }

    private static <M extends Map<String, Object>> void bean2FlatMap(final Object bean, final boolean ignoreNullProperty,
            final Collection<String> ignoredPropNames, final NamingPolicy keyNamingPolicy, final String parentPropName, final M output) {
        if (bean == null) {
            return;
        }

        final boolean isLowerCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.LOWER_CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final boolean isNullParentPropName = (parentPropName == null);
        final Class<?> beanClass = bean.getClass();

        String propName = null;
        Object propValue = null;

        for (final ParserUtil.PropInfo propInfo : ParserUtil.getBeanInfo(beanClass).propInfoList) {
            propName = propInfo.name;

            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = propInfo.getPropValue(bean);

            if (ignoreNullProperty && (propValue == null)) {
                continue;
            }

            if ((propValue == null) || !propInfo.jsonXmlType.isBean()) {
                if (isNullParentPropName) {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isLowerCamelCaseOrNoChange) {
                        output.put(parentPropName + WD.PERIOD + propName, propValue);
                    } else {
                        output.put(parentPropName + WD.PERIOD + keyNamingPolicy.convert(propName), propValue);
                    }
                }
            } else {
                if (isNullParentPropName) {
                    bean2FlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy,
                            isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), output);
                } else {
                    bean2FlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy,
                            parentPropName + WD.PERIOD + (isLowerCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName)), output);
                }
            }
        }
    }

    private static <T> void checkBeanClass(final Class<T> cls) {
        if (!Beans.isBeanClass(cls)) {
            throw new IllegalArgumentException(
                    "Bean class is required. No property getter/setter method is found in the specified class: " + ClassUtil.getCanonicalClassName(cls));
        }
    }

    /**
     * Creates a new instance of the specified bean class.
     *
     * <p>This method uses reflection to invoke the no-argument constructor of the class.
     * The class must have an accessible no-argument constructor.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = Beans.newBean(User.class);
     * // Equivalent to: User user = new User();
     * }</pre>
     *
     * @param <T> the type of the object to be created
     * @param targetType the class of the object to be created
     * @return a new instance of the specified class
     * @throws IllegalArgumentException if the class is abstract or cannot be instantiated
     */
    public static <T> T newBean(final Class<T> targetType) {
        return CommonUtil.newInstance(targetType);
    }

    private static final Set<Class<?>> notKryoCompatible = N.newConcurrentHashSet();

    /**
     * Creates a deep clone of the given object using serialization.
     *
     * <p>This method performs a deep copy by serializing the object to Kryo or JSON format
     * and then deserializing it back to create a new instance. This ensures that all nested
     * objects are also cloned, not just the top-level object.</p>
     *
     * <p>The object must be serializable through either Kryo or JSON. If Kryo serialization
     * fails, the method automatically falls back to JSON serialization.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User originalUser = new User("John", 25);
     * originalUser.setAddress(new Address("123 Main St"));
     *
     * User clonedUser = Beans.clone(originalUser);
     * // clonedUser is a deep copy - modifying it won't affect originalUser
     * clonedUser.getAddress().setStreet("456 Oak St");
     * // originalUser.getAddress().getStreet() is still "123 Main St"
     * }</pre>
     *
     * @param <T> the type of the object to be cloned
     * @param obj a Java object which must be serializable and deserializable through Kryo or JSON
     * @return a deep clone of the object, or {@code null} if the input is {@code null}
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T clone(final T obj) {
        if (obj == null) {
            return null; // NOSONAR
        }

        return (T) clone(obj, obj.getClass());
    }

    /**
     * Creates a deep clone of the given object and converts it to the specified target type.
     *
     * <p>This method performs a deep copy by serializing the object and then deserializing it
     * as an instance of the target type. This is useful for creating type-converted copies
     * or for ensuring type safety when cloning objects.</p>
     *
     * <p>If the source object is {@code null}, the method will create a new instance of the
     * target type using its default constructor if it's a bean class, or return the default
     * value for the type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserDTO userDTO = new UserDTO("John", 25);
     *
     * // Clone and convert to User entity
     * User user = Beans.clone(userDTO, User.class);
     *
     * // Clone null object - creates new instance
     * User emptyUser = Beans.clone(null, User.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param obj a Java object which must be serializable and deserializable through Kryo or JSON
     * @param targetType the class of the target type to create
     * @return a new instance of the target type with properties cloned from the source object
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> T clone(final Object obj, @NotNull final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (obj == null) {
            if (isBeanClass(targetType)) {
                return copy(null, targetType);
            } else {
                return N.newInstance(targetType);
            }
        }

        final Class<?> srcCls = obj.getClass();
        Object copy = null;

        if (Utils.kryoParser != null && targetType.equals(obj.getClass()) && !notKryoCompatible.contains(srcCls)) {
            try {
                copy = Utils.kryoParser.clone(obj);
            } catch (final Exception e) {
                notKryoCompatible.add(srcCls);

                // ignore.
            }
        }

        if (copy == null) {
            final String xml = Utils.abacusXMLParser.serialize(obj, Utils.xscForClone);
            copy = Utils.abacusXMLParser.deserialize(xml, targetType);
        }

        return (T) copy;
    }

    /**
     * Creates a shallow copy of the given source bean.
     *
     * <p>This method creates a new instance of the same class as the source bean and copies
     * all property values from the source to the new instance. Unlike {@link #clone(Object)},
     * this method performs a shallow copy - nested objects are not cloned but referenced.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User originalUser = new User("John", 25);
     * originalUser.setAddress(new Address("123 Main St"));
     *
     * User copiedUser = Beans.copy(originalUser);
     * // copiedUser has same property values as originalUser
     * // But nested objects are shared references
     * copiedUser.getAddress().setStreet("456 Oak St");
     * // originalUser.getAddress().getStreet() is now also "456 Oak St"
     * }</pre>
     *
     * @param <T> the type of the source bean
     * @param sourceBean the source bean to copy
     * @return a new instance with properties copied from the source bean, or {@code null} if the source is {@code null}
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T copy(final T sourceBean) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copy(sourceBean, (Class<T>) sourceBean.getClass());
    }

    /**
     * Creates a shallow copy of the source bean with only selected properties.
     *
     * <p>This method creates a new instance of the same class as the source bean and copies
     * only the specified properties. Properties not in the selection list will have their
     * default values in the new instance.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User originalUser = new User("John", 25);
     * originalUser.setEmail("john@example.com");
     * originalUser.setPhone("123-456-7890");
     *
     * // Copy only name and email
     * User partialCopy = Beans.copy(originalUser, Arrays.asList("name", "email"));
     * // partialCopy.getName() returns "John"
     * // partialCopy.getEmail() returns "john@example.com"
     * // partialCopy.getAge() returns 0 (default value)
     * // partialCopy.getPhone() returns null
     * }</pre>
     *
     * @param <T> the type of the source bean
     * @param sourceBean the source bean to copy
     * @param selectPropNames the collection of property names to be copied
     * @return a new instance with selected properties copied from the source bean, or {@code null} if the source is {@code null}
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final Collection<String> selectPropNames) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copy(sourceBean, selectPropNames, (Class<T>) sourceBean.getClass());
    }

    /**
     * Creates a shallow copy of the source bean with properties filtered by a predicate.
     *
     * <p>This method creates a new instance of the same class as the source bean and copies
     * only the properties that pass the filter predicate. The predicate receives the property
     * name and value and should return {@code true} to include the property in the copy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User originalUser = new User("John", 25);
     * originalUser.setEmail("john@example.com");
     * originalUser.setPhone(null);
     *
     * // Copy only non-null properties
     * User copiedUser = Beans.copy(originalUser,
     *     (propName, propValue) -> propValue != null);
     * // copiedUser will have name, age, and email but not phone
     *
     * // Copy only string properties
     * User stringPropsOnly = Beans.copy(originalUser,
     *     (propName, propValue) -> propValue instanceof String);
     * }</pre>
     *
     * @param <T> the type of the source bean
     * @param sourceBean the source bean to copy
     * @param propFilter the predicate to filter properties to be copied
     * @return a new instance with filtered properties copied from the source bean, or {@code null} if the source is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final BiPredicate<? super String, ?> propFilter) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copy(sourceBean, propFilter, (Class<T>) sourceBean.getClass());
    }

    /**
     * Creates a new instance of the specified target type with properties copied from the source bean.
     *
     * <p>This method is useful for converting between different bean types that share common
     * properties. Properties are matched by name - if a property exists in both source and
     * target types with the same name, its value will be copied.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserEntity entity = new UserEntity("John", 25, "john@example.com");
     *
     * // Convert entity to DTO
     * UserDTO dto = Beans.copy(entity, UserDTO.class);
     * // dto will have matching properties copied from entity
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean to copy properties from
     * @param targetType the class of the target type
     * @return a new instance of the target type with properties copied from the source
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     */
    public static <T> T copy(final Object sourceBean, final Class<? extends T> targetType) throws IllegalArgumentException {
        return copy(sourceBean, (Collection<String>) null, targetType);
    }

    /**
     * Creates a new instance of the target type with selected properties copied from the source bean.
     *
     * <p>This method allows selective property copying when converting between different bean types.
     * Only properties whose names are in the selection list will be copied, and they must exist
     * in both source and target types.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserEntity entity = new UserEntity("John", 25, "john@example.com", "password123");
     *
     * // Convert to DTO excluding sensitive data
     * UserDTO dto = Beans.copy(entity,
     *     Arrays.asList("name", "age", "email"),
     *     UserDTO.class);
     * // dto will have name, age, and email but not password
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean to copy properties from
     * @param selectPropNames the collection of property names to be copied
     * @param targetType the class of the target type
     * @return a new instance of the target type with selected properties copied
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     */
    public static <T> T copy(final Object sourceBean, final Collection<String> selectPropNames, @NotNull final Class<? extends T> targetType)
            throws IllegalArgumentException {
        return copy(sourceBean, selectPropNames, Fn.identity(), targetType);
    }

    /**
     * Creates a new instance of the target type with selected properties copied from the source bean,
     * applying property name conversion.
     *
     * <p>This method is useful when property names differ between source and target beans.
     * The property name converter function transforms source property names to their corresponding
     * target property names.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Source has camelCase, target expects snake_case
     * UserEntity entity = new UserEntity();
     * entity.setFirstName("John");
     * entity.setLastName("Doe");
     *
     * UserDTO dto = Beans.copy(entity,
     *     Arrays.asList("firstName", "lastName"),
     *     propName -> Strings.toSnakeCase(propName),  // Convert camelCase to snake_case
     *     UserDTO.class);
     * // dto.first_name will be "John"
     * // dto.last_name will be "Doe"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean to copy properties from
     * @param selectPropNames the collection of property names to be copied
     * @param propNameConverter function to convert property names from source to target format
     * @param targetType the class of the target type
     * @return a new instance of the target type with converted and copied properties
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copy(final Object sourceBean, final Collection<String> selectPropNames, final Function<String, String> propNameConverter,
            @NotNull final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            if (selectPropNames == null && Utils.kryoParser != null && targetType.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
                try {
                    final T copy = (T) Utils.kryoParser.copy(sourceBean);

                    if (copy != null) {
                        return copy;
                    }
                } catch (final Exception e) {
                    notKryoCompatible.add(srcCls);

                    // ignore
                }
            }
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, selectPropNames, propNameConverter, Fn.selectFirst(), targetBeanInfo);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    /**
     * Creates a new instance of the target type with properties filtered by a predicate and copied from the source bean.
     *
     * <p>This method combines property filtering with type conversion. Only properties that pass
     * the filter predicate will be copied to the new instance of the target type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserEntity entity = new UserEntity("John", 25, null, "password123");
     *
     * // Copy only non-null and non-sensitive properties
     * UserDTO dto = Beans.copy(entity,
     *     (propName, propValue) -> propValue != null && !propName.equals("password"),
     *     UserDTO.class);
     * // dto will have name and age but not email (null) or password
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean to copy properties from
     * @param propFilter predicate to filter which properties should be copied
     * @param targetType the class of the target type
     * @return a new instance of the target type with filtered properties copied
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T copy(final Object sourceBean, final BiPredicate<? super String, ?> propFilter, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        return copy(sourceBean, propFilter, Fn.identity(), targetType);
    }

    /**
     * Creates a new instance of the target type with properties filtered by a predicate and copied from the source bean,
     * applying property name conversion.
     *
     * <p>This method provides the most flexible copying mechanism, combining property filtering,
     * name conversion, and type conversion in a single operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserEntity entity = new UserEntity();
     * entity.setFirstName("John");
     * entity.setLastName("Doe");
     * entity.setPassword("secret");
     *
     * // Copy non-sensitive properties with name conversion
     * UserDTO dto = Beans.copy(entity,
     *     (propName, propValue) -> !propName.equals("password"),
     *     propName -> Strings.toSnakeCase(propName),
     *     UserDTO.class);
     * // dto.first_name will be "John"
     * // dto.last_name will be "Doe"
     * // password is excluded
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean to copy properties from
     * @param propFilter predicate to filter which properties should be copied
     * @param propNameConverter function to convert property names from source to target format
     * @param targetType the class of the target type
     * @return a new instance of the target type with filtered and converted properties
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T copy(final Object sourceBean, final BiPredicate<? super String, ?> propFilter, final Function<String, String> propNameConverter,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            if (propFilter == BiPredicates.alwaysTrue() && Utils.kryoParser != null && targetType.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
                try {
                    final T copy = (T) Utils.kryoParser.copy(sourceBean);

                    if (copy != null) {
                        return copy;
                    }
                } catch (final Exception e) {
                    notKryoCompatible.add(srcCls);

                    // ignore
                }
            }
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, propFilter, propNameConverter, Fn.selectFirst(), targetBeanInfo);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    /**
     * Creates a new instance of the target type with properties copied from the source bean,
     * excluding specified properties and optionally ignoring unmatched properties.
     *
     * <p>This method is useful when you want to copy most properties except for a specific set.
     * The {@code ignoreUnmatchedProperty} parameter controls whether an exception is thrown
     * when a property exists in the source but not in the target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserEntity entity = new UserEntity("John", 25, "john@example.com", "password123");
     *
     * // Copy all properties except sensitive ones
     * UserDTO dto = Beans.copy(entity,
     *     true,  // Ignore properties that don't exist in DTO
     *     N.asSet("password", "internalId"),
     *     UserDTO.class);
     * // dto will have all properties except password and internalId
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean to copy properties from
     * @param ignoreUnmatchedProperty if {@code true}, properties not found in target are ignored; if {@code false}, throws exception
     * @param ignoredPropNames set of property names to exclude from copying
     * @param targetType the class of the target type
     * @return a new instance of the target type with properties copied except ignored ones
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or if unmatched properties found when not ignoring
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copy(final Object sourceBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            @NotNull final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            if (ignoredPropNames == null && Utils.kryoParser != null && targetType.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
                try {
                    final T copy = (T) Utils.kryoParser.copy(sourceBean);

                    if (copy != null) {
                        return copy;
                    }
                } catch (final Exception e) {
                    notKryoCompatible.add(srcCls);

                    // ignore
                }
            }
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            merge(sourceBean, result, ignoreUnmatchedProperty, ignoredPropNames, targetBeanInfo);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    private static final BinaryOperator<?> DEFAULT_MERGE_FUNC = (a, b) -> a == null ? b : a;

    /**
     * Merges properties from the source bean into the target bean.
     *
     * <p>This method copies all properties from the source bean to the target bean.
     * Properties in the target bean are overwritten with values from the source bean.
     * Unlike {@code copy} methods which create new instances, this modifies the existing target bean.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User existingUser = new User("John", 25);
     * User updates = new User("John", 26);
     * updates.setEmail("john@example.com");
     *
     * Beans.merge(updates, existingUser);
     * // existingUser now has age=26 and email="john@example.com"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @return the same target bean instance with merged properties
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     */
    public static <T> T merge(final Object sourceBean, final T targetBean) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, (Collection<String>) null);
    }

    /**
     * Merges properties from the source bean into the target bean using a custom merge function.
     *
     * <p>The merge function determines how to combine values when a property exists in both beans.
     * It receives the source value and target value, and returns the value to set in the target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User existingUser = new User("John", 25);
     * existingUser.setScore(100);
     * User updates = new User("John", 26);
     * updates.setScore(50);
     *
     * // Merge using max value for numeric properties
     * Beans.merge(updates, existingUser,
     *     (sourceVal, targetVal) -> {
     *         if (sourceVal instanceof Integer && targetVal instanceof Integer) {
     *             return Math.max((Integer) sourceVal, (Integer) targetVal);
     *         }
     *         return sourceVal != null ? sourceVal : targetVal;
     *     });
     * // existingUser.getScore() is now 100 (max of 100 and 50)
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param mergeFunc binary operator to determine the merged value for each property
     * @return the same target bean instance with merged properties
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, BiPredicates.alwaysTrue(), mergeFunc);
    }

    /**
     * Merges properties from the source bean into the target bean with property name conversion
     * and a custom merge function.
     *
     * <p>This method allows property name mapping during the merge operation, useful when
     * source and target beans have different naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Source has camelCase, target has snake_case
     * SourceBean source = new SourceBean();
     * source.setFirstName("John");
     * TargetBean target = new TargetBean();
     *
     * Beans.merge(source, target,
     *     propName -> Strings.toSnakeCase(propName),
     *     (srcVal, tgtVal) -> srcVal != null ? srcVal : tgtVal);
     * // target.first_name is now "John"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param propNameConverter function to convert property names from source to target format
     * @param mergeFunc binary operator to determine the merged value for each property
     * @return the same target bean instance with merged properties
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc)
            throws IllegalArgumentException {
        return merge(sourceBean, targetBean, (Collection<String>) null, propNameConverter, mergeFunc);
    }

    /**
     * Merges selected properties from the source bean into the target bean.
     *
     * <p>Only properties whose names are in the selection list will be merged.
     * This is useful for partial updates where only specific fields should be modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User existingUser = new User("John", 25, "john@old.com");
     * User updates = new User("Jane", 30, "jane@new.com");
     *
     * // Only update email
     * Beans.merge(updates, existingUser, Arrays.asList("email"));
     * // existingUser still has name="John", age=25, but email="jane@new.com"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param selectPropNames collection of property names to merge
     * @return the same target bean instance with selected properties merged
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, selectPropNames, Fn.identity());
    }

    /**
     * Merges selected properties from the source bean into the target bean using a custom merge function.
     *
     * <p>Combines selective property merging with custom merge logic for maximum control
     * over the merge process.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User existingUser = new User("John", 25);
     * existingUser.setScore(100);
     * User updates = new User("Jane", 30);
     * updates.setScore(200);
     *
     * // Only merge age and score, keeping the higher score
     * Beans.merge(updates, existingUser,
     *     Arrays.asList("age", "score"),
     *     (srcVal, tgtVal) -> {
     *         if ("score".equals(currentPropName) && srcVal instanceof Integer) {
     *             return Math.max((Integer) srcVal, (Integer) tgtVal);
     *         }
     *         return srcVal;
     *     });
     * // existingUser: name="John", age=30, score=200
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param selectPropNames collection of property names to merge
     * @param mergeFunc binary operator to determine the merged value for each property
     * @return the same target bean instance with selected properties merged
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames, final BinaryOperator<?> mergeFunc)
            throws IllegalArgumentException {
        return merge(sourceBean, targetBean, selectPropNames, Fn.identity(), mergeFunc);
    }

    /**
     * Merges selected properties from the source bean into the target bean with property name conversion.
     *
     * <p>This method combines selective property merging with name conversion, useful when
     * merging between beans with different naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SourceBean source = new SourceBean();
     * source.setFirstName("John");
     * source.setLastName("Doe");
     *
     * TargetBean target = new TargetBean();
     *
     * // Merge only firstName, converting to snake_case
     * Beans.merge(source, target,
     *     Arrays.asList("firstName"),
     *     propName -> Strings.toSnakeCase(propName));
     * // target.first_name is now "John"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param selectPropNames collection of property names to merge
     * @param propNameConverter function to convert property names from source to target format
     * @return the same target bean instance with selected properties merged
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        return merge(sourceBean, targetBean, selectPropNames, propNameConverter, DEFAULT_MERGE_FUNC, ParserUtil.getBeanInfo(targetBean.getClass()));
    }

    /**
     * Merges selected properties from the source bean into the target bean with property name conversion
     * and a custom merge function.
     *
     * <p>This method provides the most flexible selective merging, combining property selection,
     * name conversion, and custom merge logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SourceBean source = new SourceBean();
     * source.setFirstName("John");
     * source.setTotalAmount(150);
     *
     * TargetBean target = new TargetBean();
     * target.setTotal_amount(100);
     *
     * Beans.merge(source, target,
     *     Arrays.asList("firstName", "totalAmount"),
     *     propName -> Strings.toSnakeCase(propName),
     *     (srcVal, tgtVal) -> {
     *         // For amounts, add them together
     *         if (srcVal instanceof Number && tgtVal instanceof Number) {
     *             return ((Number) srcVal).intValue() + ((Number) tgtVal).intValue();
     *         }
     *         return srcVal;
     *     });
     * // target.first_name = "John"
     * // target.total_amount = 250 (150 + 100)
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param selectPropNames collection of property names to merge
     * @param propNameConverter function to convert property names from source to target format
     * @param mergeFunc binary operator to determine the merged value for each property
     * @return the same target bean instance with selected properties merged
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        return merge(sourceBean, targetBean, selectPropNames, propNameConverter, mergeFunc, targetBeanInfo);
    }

    /**
     * Merges properties from the source bean into the target bean based on a filter predicate.
     *
     * <p>The predicate receives each property name and value from the source bean and
     * determines whether that property should be merged into the target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User source = new User("John", 0, null);
     * User target = new User("Jane", 25, "jane@example.com");
     *
     * // Only merge non-null and non-zero values
     * Beans.merge(source, target,
     *     (propName, propValue) -> propValue != null &&
     *         !(propValue instanceof Number && ((Number) propValue).intValue() == 0));
     * // target keeps age=25 and email="jane@example.com" but name becomes "John"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param propFilter predicate to determine which properties should be merged
     * @return the same target bean instance with filtered properties merged
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, final T targetBean, final BiPredicate<? super String, ?> propFilter) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, propFilter, DEFAULT_MERGE_FUNC);
    }

    /**
     * Merges properties from the source bean into the target bean based on a filter predicate
     * and using a custom merge function.
     *
     * <p>Combines property filtering with custom merge logic for fine-grained control
     * over the merge process.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Product source = new Product("New Name", 150, 10);
     * Product target = new Product("Old Name", 100, 5);
     *
     * // Merge numeric properties by adding them, others by replacing
     * Beans.merge(source, target,
     *     (propName, propValue) -> propValue != null,
     *     (srcVal, tgtVal) -> {
     *         if (srcVal instanceof Number && tgtVal instanceof Number) {
     *             return ((Number) srcVal).intValue() + ((Number) tgtVal).intValue();
     *         }
     *         return srcVal;
     *     });
     * // target: name="New Name", price=250, quantity=15
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param propFilter predicate to determine which properties should be merged
     * @param mergeFunc binary operator to determine the merged value for each property
     * @return the same target bean instance with filtered properties merged
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, ?> propFilter,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, propFilter, Fn.identity(), mergeFunc);
    }

    /**
     * Merges properties from the source bean into the target bean based on a filter predicate
     * with property name conversion.
     *
     * <p>This method allows filtering properties and converting their names during the merge,
     * useful when working with beans that have different naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SourceBean source = new SourceBean();
     * source.setFirstName("John");
     * source.setLastName("");  // Empty string
     *
     * TargetBean target = new TargetBean();
     *
     * // Only merge non-empty strings, converting to snake_case
     * Beans.merge(source, target,
     *     (propName, propValue) -> propValue instanceof String && !((String) propValue).isEmpty(),
     *     propName -> Strings.toSnakeCase(propName));
     * // target.first_name = "John", last_name is not merged
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param propFilter predicate to determine which properties should be merged
     * @param propNameConverter function to convert property names from source to target format
     * @return the same target bean instance with filtered and converted properties merged
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, ?> propFilter,
            final Function<String, String> propNameConverter) throws IllegalArgumentException {
        return merge(sourceBean, targetBean, propFilter, propNameConverter, DEFAULT_MERGE_FUNC);
    }

    /**
     * Merges properties from the source bean into the target bean with full control over filtering,
     * name conversion, and merge logic.
     *
     * <p>This is the most flexible merge method, providing complete control over which properties
     * are merged, how their names are converted, and how values are combined.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SourceBean source = new SourceBean();
     * source.setFirstName("John");
     * source.setTotalCount(10);
     * source.setLastUpdated(new Date());
     *
     * TargetBean target = new TargetBean();
     * target.setTotal_count(5);
     *
     * Beans.merge(source, target,
     *     // Only merge non-null values
     *     (propName, propValue) -> propValue != null,
     *     // Convert camelCase to snake_case
     *     propName -> Strings.toSnakeCase(propName),
     *     // Custom merge logic
     *     (srcVal, tgtVal) -> {
     *         if (srcVal instanceof Integer && tgtVal instanceof Integer) {
     *             return ((Integer) srcVal) + ((Integer) tgtVal);
     *         } else if (srcVal instanceof Date && tgtVal instanceof Date) {
     *             // Keep the more recent date
     *             return ((Date) srcVal).after((Date) tgtVal) ? srcVal : tgtVal;
     *         }
     *         return srcVal;
     *     });
     * // Result: first_name="John", total_count=15, last_updated=most recent date
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged; must not be {@code null}
     * @param propFilter predicate to determine which properties should be merged; must not be {@code null}
     * @param propNameConverter function to convert property names from source to target format; must not be {@code null}
     * @param mergeFunc binary operator to determine the merged value for each property; must not be {@code null}
     * @return the same target bean instance with filtered, converted, and merged properties
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}
     * @see BiPredicates#alwaysTrue()
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, ?> propFilter,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        return merge(sourceBean, targetBean, propFilter, propNameConverter, mergeFunc, targetBeanInfo);
    }

    /**
     * Merges properties from the source bean into the target bean, excluding specified properties.
     *
     * <p>This method merges all properties except those in the ignored set. The
     * {@code ignoreUnmatchedProperty} parameter controls whether an exception is thrown
     * when a property exists in the source but not in the target.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User source = new User("John", 30, "john@example.com", "password123");
     * User target = new User("Jane", 25, "jane@example.com", "oldpass");
     *
     * // Merge all except password
     * Beans.merge(source, target,
     *     true,  // Ignore if source has properties not in target
     *     N.asSet("password"));
     * // target: name="John", age=30, email="john@example.com", password="oldpass"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param ignoreUnmatchedProperty if {@code true}, properties not found in target are ignored; if {@code false}, throws exception
     * @param ignoredPropNames set of property names to exclude from merging
     * @return the same target bean instance with properties merged except ignored ones
     * @throws IllegalArgumentException if {@code targetBean} is {@code null} or if unmatched properties found when not ignoring
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames)
            throws IllegalArgumentException {
        return merge(sourceBean, targetBean, ignoreUnmatchedProperty, ignoredPropNames, DEFAULT_MERGE_FUNC);
    }

    /**
     * Merges properties from the source bean into the target bean with a custom merge function,
     * excluding specified properties.
     *
     * <p>This method combines exclusion-based merging with custom merge logic, useful when
     * you want to merge most properties with special handling for certain values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Product source = new Product("New Product", 200, 15, "2024-01-01");
     * Product target = new Product("Old Product", 150, 10, "2023-01-01");
     *
     * // Merge all except createdDate, using custom logic for quantities
     * Beans.merge(source, target,
     *     true,
     *     N.asSet("createdDate"),
     *     (srcVal, tgtVal) -> {
     *         // For quantities, add them together
     *         if (srcVal instanceof Integer && "quantity".equals(currentPropName)) {
     *             return ((Integer) srcVal) + ((Integer) tgtVal);
     *         }
     *         return srcVal;
     *     });
     * // target: name="New Product", price=200, quantity=25, createdDate="2023-01-01"
     * }</pre>
     *
     * @param <T> the type of the target bean
     * @param sourceBean the source bean from which properties are copied
     * @param targetBean the target bean into which properties are merged
     * @param ignoreUnmatchedProperty if {@code true}, properties not found in target are ignored; if {@code false}, throws exception
     * @param ignoredPropNames set of property names to exclude from merging
     * @param mergeFunc binary operator to determine the merged value for each property
     * @return the same target bean instance with properties merged using custom logic
     * @throws IllegalArgumentException if {@code targetBean} is {@code null} or if unmatched properties found when not ignoring
     */
    public static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());
        final BinaryOperator<Object> objMergeFunc = (BinaryOperator<Object>) mergeFunc;

        PropInfo targetPropInfo = null;
        Object propValue = null;

        for (final PropInfo propInfo : srcBeanInfo.propInfoList) {
            if (ignoredPropNames == null || !ignoredPropNames.contains(propInfo.name)) {
                targetPropInfo = targetBeanInfo.getPropInfo(propInfo);

                if (targetPropInfo == null) {
                    if (!ignoreUnmatchedProperty) {
                        throw new IllegalArgumentException("No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                    }
                } else {
                    propValue = propInfo.getPropValue(sourceBean);
                    targetPropInfo.setPropValue(targetBean, objMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
                }
            }
        }

        return targetBean;
    }

    @SuppressWarnings("deprecation")
    private static <T> T merge(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            final BeanInfo targetBeanInfo) throws IllegalArgumentException {
        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());

        Object propValue = null;

        for (final PropInfo propInfo : srcBeanInfo.propInfoList) {
            if (ignoredPropNames == null || !ignoredPropNames.contains(propInfo.name)) {
                propValue = propInfo.getPropValue(sourceBean);

                if (N.notNullOrDefault(propValue)) {
                    targetBeanInfo.setPropValue(targetBean, propInfo, propValue, ignoreUnmatchedProperty);
                }
            }
        }

        return targetBean;
    }

    private static <T> T merge(final Object sourceBean, final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc, final BeanInfo targetBeanInfo) {
        if (sourceBean == null) {
            return targetBean;
        }

        final boolean isIdentityPropNameConverter = propNameConverter == Fn.<String> identity();
        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BinaryOperator<Object> objMergeFunc = (BinaryOperator<Object>) mergeFunc;
        final boolean ignoreUnmatchedProperty = selectPropNames == null;

        Object propValue = null;
        String targetPropName = null;
        PropInfo targetPropInfo = null;

        if (selectPropNames == null) {
            for (final PropInfo propInfo : srcBeanInfo.propInfoList) {
                if (isIdentityPropNameConverter) {
                    targetPropInfo = targetBeanInfo.getPropInfo(propInfo);
                } else {
                    targetPropName = propNameConverter.apply(propInfo.name);

                    if (propInfo.name.equals(targetPropName)) {
                        targetPropInfo = targetBeanInfo.getPropInfo(propInfo);
                    } else {
                        targetPropInfo = targetBeanInfo.getPropInfo(targetPropName);
                    }
                }

                if (targetPropInfo == null) {
                    //    if (!ignoreUnmatchedProperty) {
                    //        throw new IllegalArgumentException(
                    //                "No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                    //    }
                } else {
                    propValue = propInfo.getPropValue(sourceBean);
                    targetPropInfo.setPropValue(targetBean, objMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
                }
            }
        } else {
            PropInfo propInfo = null;

            for (final String propName : selectPropNames) {
                propInfo = srcBeanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("No property found by name: " + propName + " in source bean class: " + sourceBean.getClass());
                }

                if (isIdentityPropNameConverter) {
                    targetPropInfo = targetBeanInfo.getPropInfo(propInfo);
                } else {
                    targetPropName = propNameConverter.apply(propInfo.name);

                    if (propInfo.name.equals(targetPropName)) {
                        targetPropInfo = targetBeanInfo.getPropInfo(propInfo);
                    } else {
                        targetPropInfo = targetBeanInfo.getPropInfo(targetPropName);
                    }
                }

                if (targetPropInfo == null) {
                    //noinspection ConstantValue
                    if (!ignoreUnmatchedProperty) { //NOSONAR
                        throw new IllegalArgumentException("No property found by name: " + propName + " in target bean class: " + targetBean.getClass()); //NOSONAR
                    }
                } else {
                    propValue = srcBeanInfo.getPropValue(sourceBean, propName);
                    targetPropInfo.setPropValue(targetBean, objMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
                }
            }
        }

        return targetBean;
    }

    private static <T> T merge(final Object sourceBean, final T targetBean, final BiPredicate<? super String, ?> propFilter,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc, final BeanInfo targetBeanInfo) {
        if (sourceBean == null) {
            return targetBean;
        }

        final boolean isIdentityPropNameConverter = propNameConverter == Fn.<String> identity();
        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BiPredicate<? super String, Object> objPropFilter = (BiPredicate<? super String, Object>) propFilter;
        final BinaryOperator<Object> objPropMergeFunc = (BinaryOperator<Object>) mergeFunc;

        Object propValue = null;
        PropInfo targetPropInfo = null;
        String targetPropName = null;

        for (final PropInfo propInfo : srcBeanInfo.propInfoList) {
            propValue = propInfo.getPropValue(sourceBean);

            if (objPropFilter.test(propInfo.name, propValue)) {
                if (isIdentityPropNameConverter) {
                    targetPropInfo = targetBeanInfo.getPropInfo(propInfo);
                } else {
                    targetPropName = propNameConverter.apply(propInfo.name);

                    if (propInfo.name.equals(targetPropName)) {
                        targetPropInfo = targetBeanInfo.getPropInfo(propInfo);
                    } else {
                        targetPropInfo = targetBeanInfo.getPropInfo(targetPropName);
                    }
                }

                if (targetPropInfo == null) {
                    throw new IllegalArgumentException("No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                }

                targetPropInfo.setPropValue(targetBean, objPropMergeFunc.apply(propValue, targetPropInfo.getPropValue(targetBean)));
            }
        }

        return targetBean;
    }

    /**
     * Erases (sets to default values) the specified properties of the given bean.
     *
     * <p>This method sets the specified properties to their default values:</p>
     * <ul>
     *   <li>Primitive numeric types: 0</li>
     *   <li>Primitive boolean: {@code false}</li>
     *   <li>Object references: {@code null}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25, "john@example.com");
     *
     * // Clear sensitive data
     * Beans.erase(user, "email", "password");
     * // user.getEmail() returns null
     * // user.getPassword() returns null
     * // user.getName() still returns "John"
     * }</pre>
     *
     * @param bean the bean object whose properties are to be erased
     * @param propNames the names of the properties to be erased
     */
    public static void erase(final Object bean, final String... propNames) {
        if (bean == null || N.isEmpty(propNames)) {
            return;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        for (final String propName : propNames) {
            beanInfo.setPropValue(bean, propName, null);
        }
    }

    /**
     * Erases (sets to default values) the specified properties of the given bean.
     *
     * <p>This method sets the specified properties to their default values.
     * This overload accepts a collection of property names instead of varargs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25, "john@example.com");
     *
     * List<String> propsToErase = Arrays.asList("age", "email", "phone");
     * Beans.erase(user, propsToErase);
     * // user.getAge() returns 0
     * // user.getEmail() returns null
     * // user.getPhone() returns null
     * }</pre>
     *
     * @param bean the bean object whose properties are to be erased
     * @param propNames the collection of property names to be erased
     */
    public static void erase(final Object bean, final Collection<String> propNames) {
        if (bean == null || N.isEmpty(propNames)) {
            return;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        for (final String propName : propNames) {
            beanInfo.setPropValue(bean, propName, null);
        }
    }

    /**
     * Erases all properties of the given bean, setting them to their default values.
     *
     * <p>This method sets all properties of the bean to their default values:</p>
     * <ul>
     *   <li>Primitive numeric types: 0</li>
     *   <li>Primitive boolean: {@code false}</li>
     *   <li>Object references: {@code null}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25, "john@example.com");
     * user.setPhone("123-456-7890");
     *
     * // Clear all data
     * Beans.eraseAll(user);
     * // All properties are now set to default values
     * // user.getName() returns null
     * // user.getAge() returns 0
     * // user.getEmail() returns null
     * // user.getPhone() returns null
     * }</pre>
     *
     * @param bean the bean object whose properties are to be erased. If this is {@code null}, the method does nothing.
     */
    public static void eraseAll(final Object bean) {
        if (bean == null) {
            return;
        }

        final Class<?> cls = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

        for (final PropInfo propInfo : beanInfo.propInfoList) {
            propInfo.setPropValue(bean, null);
        }
    }

    /**
     * Fills all properties of the specified bean with random values.
     *
     * <p>This method uses reflection to discover all properties of the bean and fills them
     * with appropriate random values based on their types. Nested bean properties are also
     * filled recursively.</p>
     *
     * <p><b>Supported types:</b></p>
     * <ul>
     *   <li>Primitives and their wrappers (int, boolean, etc.)</li>
     *   <li>String (random UUID substring)</li>
     *   <li>Date and Calendar (current timestamp)</li>
     *   <li>Number subclasses</li>
     *   <li>Nested bean objects (filled recursively)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person();
     * Beans.fill(person);
     * // person now has all properties filled with random values
     * System.out.println(person.getName()); // e.g., "a1b2c3d4-e5f6-78"
     * }</pre>
     *
     * @param bean a bean object with getter/setter methods
     * @throws IllegalArgumentException if bean is {@code null} or not a valid bean class
     */
    public static void fill(final Object bean) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);

        final Class<?> beanClass = bean.getClass();
        checkBeanClass(beanClass);

        fill(bean, Beans.getPropNameList(beanClass));
    }

    /**
     * Fills the specified properties of the bean with random values.
     *
     * <p>Only the properties whose names are contained in the provided collection will be filled.
     * This is useful when you want to test specific scenarios with only certain fields populated.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     * Beans.fill(user, Arrays.asList("username", "email"));
     * // Only username and email are filled, other properties remain unchanged
     * }</pre>
     *
     * @param bean a bean object with getter/setter methods
     * @param propNamesToFill collection of property names to fill
     * @throws IllegalArgumentException if bean is {@code null} or not a valid bean class
     */
    public static void fill(final Object bean, final Collection<String> propNamesToFill) {
        N.checkArgNotNull(bean, cs.bean);
        checkBeanClass(bean.getClass());

        fill(ParserUtil.getBeanInfo(bean.getClass()), bean, propNamesToFill);
    }

    /**
     * Creates a new instance of the specified bean class and fills all its properties with random values.
     *
     * <p>This is a convenience method that combines object creation and property filling in one step.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a fully populated test object
     * Customer customer = Beans.fill(Customer.class);
     * // Use the customer object in tests
     * assertNotNull(customer.getName());
     * assertNotNull(customer.getAddress());
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @return a new instance with all properties filled with random values
     * @throws IllegalArgumentException if beanClass is {@code null} or not a valid bean class
     */
    public static <T> T fill(final Class<? extends T> beanClass) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);

        return fill(beanClass, Beans.getPropNameList(beanClass));
    }

    /**
     * Creates multiple instances of the specified bean class, each filled with random values.
     *
     * <p>This method is useful for generating test data sets or when you need multiple
     * test objects with varying random data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Generate 50 test users
     * List<User> testUsers = Beans.fill(User.class, 50);
     * // Use in batch operations or performance tests
     * userService.saveAll(testUsers);
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @param count number of instances to create
     * @return a list containing the specified number of filled bean instances
     * @throws IllegalArgumentException if beanClass is {@code null}, not a valid bean class, or count is negative
     */
    public static <T> List<T> fill(final Class<? extends T> beanClass, final int count) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);

        return fill(beanClass, Beans.getPropNameList(beanClass), count);
    }

    /**
     * Creates a new instance of the specified bean class and fills only the specified properties.
     *
     * <p>Properties not included in the collection will retain their default values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a user with only required fields filled
     * User user = Beans.fill(User.class, Arrays.asList("id", "username", "email"));
     * // Other fields like address, phone, etc. remain null/default
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @param propNamesToFill collection of property names to fill
     * @return a new instance with specified properties filled with random values
     * @throws IllegalArgumentException if beanClass is {@code null} or not a valid bean class
     */
    public static <T> T fill(final Class<? extends T> beanClass, final Collection<String> propNamesToFill) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        final Object result = beanInfo.createBeanResult();

        fill(beanInfo, result, propNamesToFill);

        return beanInfo.finishBeanResult(result);
    }

    /**
     * Creates multiple instances of the specified bean class with only the specified properties filled.
     *
     * <p>Each instance will have the same set of properties filled but with different random values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create test data with only essential fields
     * List<Product> products = Beans.fill(
     *     Product.class,
     *     Arrays.asList("id", "name", "price"),
     *     100
     * );
     * // Each product has random id, name, and price, but other fields are default
     * }</pre>
     *
     * @param <T> the type of the bean
     * @param beanClass bean class with getter/setter methods
     * @param propNamesToFill collection of property names to fill
     * @param count number of instances to create
     * @return a list containing the specified number of partially filled bean instances
     * @throws IllegalArgumentException if beanClass is {@code null}, not a valid bean class, or count is negative
     */
    public static <T> List<T> fill(final Class<? extends T> beanClass, final Collection<String> propNamesToFill, final int count)
            throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        checkBeanClass(beanClass);
        N.checkArgNotNegative(count, cs.count);

        final List<T> resultList = new ArrayList<>(count);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        Object result = null;

        for (int i = 0; i < count; i++) {
            result = beanInfo.createBeanResult();

            fill(beanInfo, result, propNamesToFill);

            resultList.add(beanInfo.finishBeanResult(result));
        }

        return resultList;
    }

    private static void fill(final BeanInfo beanInfo, final Object bean, final Collection<String> propNamesToFill) {
        PropInfo propInfo = null;
        Type<Object> type = null;
        Class<?> parameterClass = null;
        Object propValue = null;

        for (final String propName : propNamesToFill) {
            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanInfo.clazz);
            }

            parameterClass = propInfo.clazz;
            type = propInfo.jsonXmlType;

            if (String.class.equals(parameterClass)) {
                propValue = Strings.uuid().substring(0, 16);
            } else if (boolean.class.equals(parameterClass) || Boolean.class.equals(parameterClass)) {
                propValue = N.RAND.nextBoolean();
            } else if (char.class.equals(parameterClass) || Character.class.equals(parameterClass)) {
                propValue = (char) ('a' + N.RAND.nextInt(26));
            } else if (int.class.equals(parameterClass) || Integer.class.equals(parameterClass)) {
                propValue = N.RAND.nextInt();
            } else if (long.class.equals(parameterClass) || Long.class.equals(parameterClass)) {
                propValue = N.RAND.nextLong();
            } else if (float.class.equals(parameterClass) || Float.class.equals(parameterClass)) {
                propValue = N.RAND.nextFloat();
            } else if (double.class.equals(parameterClass) || Double.class.equals(parameterClass)) {
                propValue = N.RAND.nextDouble();
            } else if (byte.class.equals(parameterClass) || Byte.class.equals(parameterClass)) {
                propValue = Integer.valueOf(N.RAND.nextInt()).byteValue(); //NOSONAR
            } else if (short.class.equals(parameterClass) || Short.class.equals(parameterClass)) {
                propValue = Integer.valueOf(N.RAND.nextInt()).shortValue(); //NOSONAR
            } else if (Number.class.isAssignableFrom(parameterClass)) {
                propValue = type.valueOf(String.valueOf(N.RAND.nextInt()));
            } else if (java.util.Date.class.isAssignableFrom(parameterClass) || Calendar.class.isAssignableFrom(parameterClass)) {
                propValue = type.valueOf(String.valueOf(System.currentTimeMillis()));
            } else if (Beans.isBeanClass(parameterClass)) {
                propValue = fill(parameterClass);
            } else {
                propValue = type.defaultValue();
            }

            propInfo.setPropValue(bean, propValue);
        }
    }

    /**
     * Compares the properties of two beans to determine if they are equal by common properties.
     *
     * <p>This method automatically identifies properties that exist in both bean classes
     * and compares only those common properties. This is useful when comparing objects
     * of different types that share some common properties.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserDTO dto = new UserDTO("John", "john@example.com");
     * UserEntity entity = new UserEntity("John", "john@example.com", new Date());
     * boolean equal = Beans.equalsByCommonProps(dto, entity);
     * // Returns true if common properties (name, email) are equal
     * }</pre>
     *
     * @param bean1 the first bean to compare, must not be null
     * @param bean2 the second bean to compare, must not be null
     * @return {@code true} if all the common properties of the beans are equal, {@code false} otherwise
     * @throws IllegalArgumentException if no common property is found
     */
    public static boolean equalsByCommonProps(@NotNull final Object bean1, @NotNull final Object bean2) throws IllegalArgumentException {
        N.checkArgNotNull(bean1);
        N.checkArgNotNull(bean2);
        checkBeanClass(bean1.getClass());
        checkBeanClass(bean2.getClass());

        final List<String> propNamesToCompare = new ArrayList<>(getPropNameList(bean1.getClass()));
        propNamesToCompare.retainAll(getPropNameList(bean2.getClass()));

        if (N.isEmpty(propNamesToCompare)) {
            throw new IllegalArgumentException("No common property found in class: " + bean1.getClass() + " and class: " + bean2.getClass());
        }

        return equalsByProps(bean1, bean2, propNamesToCompare);
    }

    /**
     * Compares the properties of two beans to determine if they are equal.
     *
     * <p>This method compares only the specified properties of the two bean objects.
     * Properties are compared using their equals() method. If all specified properties
     * are equal, the method returns {@code true}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user1 = new User("John", 25, "john@example.com");
     * User user2 = new User("John", 30, "john@example.com");
     * boolean equal = Beans.equalsByProps(user1, user2, Arrays.asList("name", "email"));
     * // Returns true because name and email are equal, age is not compared
     * }</pre>
     *
     * @param bean1 the first bean to compare, must not be null
     * @param bean2 the second bean to compare, must not be null
     * @param propNamesToCompare the collection of property names to compare, must not be {@code null} or empty
     * @return {@code true} if all the specified properties of the beans are equal, {@code false} otherwise
     * @throws IllegalArgumentException if the {@code propNamesToCompare} is empty
     */
    public static boolean equalsByProps(final Object bean1, final Object bean2, final Collection<String> propNamesToCompare) throws IllegalArgumentException {
        N.checkArgNotEmpty(propNamesToCompare, cs.propNamesToCompare);

        return compareByProps(bean1, bean2, propNamesToCompare) == 0;
    }

    /**
     * Compares two beans based on the specified properties.
     *
     * <p>This method performs a property-by-property comparison in the order specified.
     * The comparison follows the standard Comparable contract: returning negative, zero,
     * or positive values for less than, equal to, or greater than relationships.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user1 = new User("Alice", 25);
     * User user2 = new User("Bob", 30);
     * int result = Beans.compareByProps(user1, user2, Arrays.asList("name"));
     * // Returns negative value because "Alice" < "Bob"
     * }</pre>
     *
     * <p><b>Note:</b> This method uses reflection to access properties which may impact
     * performance in tight loops or high-frequency operations.</p>
     *
     * @param bean1 the first bean to compare, must not be null
     * @param bean2 the second bean to compare, must not be null
     * @param propNamesToCompare the collection of property names to compare, which may be null
     * @return a negative integer, zero, or a positive integer as the first bean is less than, equal to, or greater than the second bean
     * @throws IllegalArgumentException if any of the arguments are null
     * @deprecated call {@code getPropValue} by reflection APIs during comparing or sorting may have a huge impact on performance. Use {@link ComparisonBuilder} instead.
     * @see Builder#compare(Object, Object, Comparator)
     * @see ComparisonBuilder
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static int compareByProps(@NotNull final Object bean1, @NotNull final Object bean2, final Collection<String> propNamesToCompare) {
        N.checkArgNotNull(propNamesToCompare);
        N.checkArgNotNull(bean1);
        N.checkArgNotNull(bean2);
        checkBeanClass(bean1.getClass());
        checkBeanClass(bean2.getClass());

        final BeanInfo beanInfo1 = ParserUtil.getBeanInfo(bean1.getClass());
        final BeanInfo beanInfo2 = ParserUtil.getBeanInfo(bean2.getClass());

        PropInfo propInfo1 = null;
        PropInfo propInfo2 = null;
        int ret = 0;

        for (final String propName : propNamesToCompare) {
            propInfo1 = beanInfo1.getPropInfo(propName);

            if (propInfo1 == null) {
                throw new IllegalArgumentException("No field found in class: " + bean1.getClass() + " by name: " + propName);
            }

            propInfo2 = beanInfo2.getPropInfo(propName);

            if (propInfo2 == null) {
                throw new IllegalArgumentException("No field found in class: " + bean2.getClass() + " by name: " + propName);
            }

            if ((ret = N.compare(propInfo1.getPropValue(bean1), (Comparable) propInfo2.getPropValue(bean2))) != 0) {
                return ret;
            }
        }

        return 0;
    }

    /**
     * Creates a stream of property name-value pairs from the specified bean.
     *
     * <p>This method uses reflection to extract all properties of the bean and returns
     * them as a stream of Map.Entry objects. Each entry contains the property name as
     * the key and the property value as the value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25, "john@example.com");
     * Beans.properties(user)
     *     .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
     * // Outputs: name: John, age: 25, email: john@example.com
     * }</pre>
     *
     * @param bean the bean object to extract properties from
     * @return a stream of Map.Entry objects containing property names and values
     * @throws IllegalArgumentException if bean is null
     */
    public static Stream<Map.Entry<String, Object>> properties(final Object bean) {
        N.checkArgNotNull(bean, cs.bean);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        return Stream.of(beanInfo.propInfoList).map(propInfo -> N.newEntry(propInfo.name, propInfo.getPropValue(bean)));
    }

    /**
     * Creates a filtered stream of property name-value pairs from the specified bean.
     *
     * <p>This method is similar to {@link #properties(Object)} but allows filtering
     * of properties based on a predicate. Only properties that match the predicate
     * criteria are included in the returned stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25, null);
     * Beans.properties(user, (name, value) -> value != null)
     *     .forEach(entry -> System.out.println(entry.getKey() + ": " + entry.getValue()));
     * // Only outputs non-null properties: name: John, age: 25
     * }</pre>
     *
     * @param bean the bean object to extract properties from
     * @param propFilter a predicate that tests property name and value; returns {@code true} to include the property
     * @return a stream of Map.Entry objects containing filtered property names and values
     * @throws IllegalArgumentException if bean is null
     */
    public static Stream<Map.Entry<String, Object>> properties(final Object bean, final BiPredicate<String, Object> propFilter) {
        N.checkArgNotNull(bean, cs.bean);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        return Stream.of(beanInfo.propInfoList).map(propInfo -> {
            final Object propValue = propInfo.getPropValue(bean);
            return propFilter.test(propInfo.name, propValue) ? N.newEntry(propInfo.name, propValue) : null;
        }).skipNulls();
    }
}
