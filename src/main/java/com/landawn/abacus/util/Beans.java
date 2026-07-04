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
import com.landawn.abacus.util.Fn.BiPredicates;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Nullable;
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
 *   <li><b>Object Conversion:</b> {@link #beanToMap(Object)}, {@link #mapToBean(Map, Class)} with deep and shallow conversion options</li>
 *   <li><b>Object Lifecycle:</b> {@link #newBean(Class)}, {@link #copyAs(Object, Class)}, {@link #deepCopy(Object)}, {@link #mergeInto(Object, Object)} for object management</li>
 *   <li><b>Comparison Operations:</b> {@link N#equalsByProps(Object, Object, Collection)}, {@link N#compareByProps(Object, Object, Collection)} with configurable properties</li>
 *   <li><b>Transformation:</b> {@link #randomize(Object)} for object manipulation</li>
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
 * boolean isBean = Beans.isBeanClass(User.class);                // returns true for a standard bean class
 * List<String> properties = Beans.getPropNameList(User.class);   // returns cached property names
 * Beans.BuilderInfo builderInfo = Beans.getBuilderInfo(User.class);   // returns null if User has no builder pattern
 *
 * // Property access operations
 * User user = new User();
 * Beans.setPropValue(user, "name", "John Doe");     // user is updated with name "John Doe"
 * String name = Beans.getPropValue(user, "name");   // returns "John Doe"
 *
 * // Object creation and instantiation
 * User newUser = Beans.newBean(User.class);       // returns a new User instance
 * User copied = Beans.copyAs(user, User.class);   // returns a copy with matching properties
 * User cloned = Beans.deepCopy(user);             // returns a deep copy
 *
 * // Bean to Map conversion (various formats)
 * Map<String, Object> flatMap = Beans.beanToMap(user);                                       // returns a map of non-null properties
 * Map<String, Object> deepMap = Beans.deepBeanToMap(user);                                   // returns nested bean properties as maps
 * Map<String, Object> selectedMap = Beans.beanToMap(user, Arrays.asList("name", "email"));   // returns selected properties only
 *
 * // Map to Bean conversion
 * Map<String, Object> userData = Map.of("name", "Jane", "age", 25, "email", "jane@example.com");
 * User userFromMap = Beans.mapToBean(userData, User.class);                             // returns a populated User
 * User userFromMapIgnoreUnknown = Beans.mapToBean(userData, true, User.class);   // treats unknown properties as ignored
 *
 * // Object merging with strategies
 * User source = new User("John", 30, "john@example.com");
 * User target = new User("Jane", 25, null);
 * Beans.mergeInto(source, target);                                        // target is updated from source
 * Beans.mergeInto(source, target, (sourceVal, targetVal) -> sourceVal);   // uses source values
 *
 * // Object comparison operations
 * User user1 = new User("John", 30);
 * User user2 = new User("John", 40);
 * boolean isEqual = N.equalsByProps(user1, user2, Arrays.asList("name"));   // returns true
 *
 * // Null-safe operations
 * Map<String, Object> nullSafeMap = Beans.beanToMap(null);   // returns empty map
 * User nullSafeUser = Beans.mapToBean(null, User.class);     // returns null
 * boolean nullClassCheck = Beans.isBeanClass(null);          // returns false
 * }</pre>
 *
 * <p><b>Bean-to-Map Conversion Options:</b>
 * <ul>
 *   <li><b>Flat Conversion:</b> {@code beanToFlatMap()} - Flat map representation with dot notation</li>
 *   <li><b>Deep Conversion:</b> {@code deepBeanToMap(bean)} - Recursive nested object conversion</li>
 *   <li><b>Selected Conversion:</b> {@code beanToMap(bean, selectPropNames)} - Specific property selection</li>
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
 *   <li><b>Default Merge:</b> Source values overwrite target values unless the source value is {@code null}</li>
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
 *   <li><b>Immutable Operations:</b> Conversion and copy methods create new objects; merge/fill methods
 *       ({@code mergeInto}, {@code clearProps}, {@code randomize}, output-map overloads) modify the supplied target in place</li>
 *   <li><b>Shared State:</b> Internal static caches are mutable but thread-safe (concurrent/synchronized access)</li>
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
 *   <li><b>DTO Conversion:</b> {@code DTO dto = Beans.copyAs(entity, DTO.class);}</li>
 *   <li><b>Configuration Mapping:</b> {@code Config config = Beans.mapToBean(properties, Config.class);}</li>
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
 * <p><b>Usage Examples: Complex Object Processing</b></p>
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
 * List<String> allProps = Beans.getPropNameList(User.class);   // allProps contains name, age, address, and roles
 *
 * // Complex conversion operations
 * Map<String, Object> deepMap = Beans.deepBeanToMap(user);      // returns nested bean properties as maps
 * Map<String, Object> flatMap = Beans.beanToFlatMap(user, Arrays.asList("address"));       // returns flattened address properties
 * Map<String, Object> filteredMap = Beans.beanToMap(user, Arrays.asList("name", "age"));   // returns name and age only
 *
 * // Advanced copying with transformations
 * UserDTO dto = Beans.copyAs(user, UserDTO.class);   // returns a DTO with matching properties
 * User cloned = Beans.deepCopy(user);   // returns a deep copy
 * User partial = Beans.copyAs(user, Arrays.asList("name", "age"), User.class);   // returns a partial copy
 *
 * // Merging with different strategies
 * User updates = new User();
 * updates.setName("Jane Doe");
 *
 * Beans.mergeInto(updates, user);   // user is updated from updates
 * Beans.mergeInto(updates, user, (source, target) ->
 *     source != null && !source.equals("") ? source : target);
 *
 * // Validation and comparison
 * boolean isValid = Beans.isBeanClass(User.class);
 * boolean isEqual = N.equalsByProps(user, cloned, Arrays.asList("name", "age"));
 * }</pre>
 *
 * <p><b>Usage Examples: Configuration Management</b></p>
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
 * // Merge configuration from multiple sources
 * Map<String, Object> envVars = N.asMap("host", "db.example.com", "username", "app");
 * Map<String, Object> properties = N.asMap("port", 5433, "database", "orders");
 * Map<String, Object> defaults = Beans.beanToMap(new DatabaseConfig());   // returns default property values
 *
 * // Merge configurations with precedence: env vars > properties > defaults
 * Map<String, Object> finalConfig = new HashMap<>(defaults);
 * finalConfig.putAll(properties);
 * finalConfig.putAll(envVars);
 *
 * // Convert to configuration bean
 * DatabaseConfig config = Beans.mapToBean(finalConfig, DatabaseConfig.class);
 *
 * // Validate configuration
 * boolean isValidBean = Beans.isBeanClass(DatabaseConfig.class);
 * List<String> requiredProps = Arrays.asList("host", "port", "database", "username");
 * boolean hasAllRequired = requiredProps.stream()
 *     .allMatch(prop -> Beans.getPropValue(config, prop) != null);
 *
 * // Generate configuration summary
 * Map<String, Object> summary = Beans.beanToMap(config, Arrays.asList("host", "port", "database", "ssl"));
 * }</pre>
 *
 * <p><b>Selection convention:</b> for methods that take a {@code selectPropNames} collection (such as
 * {@code beanToMap}, {@code deepBeanToMap}, {@code beanToFlatMap}, {@code copy}, {@code copyAs},
 * {@code mapToBean}), a {@code null} value means "not specified" and selects ALL properties, whereas an
 * empty collection is an explicit selection of NO properties. The inverse {@code ignoredPropNames} family
 * treats {@code null} and empty alike as "exclude nothing". This follows the library's null/empty
 * selection convention.</p>
 *
 * <p><b>Optional-returning accessor:</b> {@link #getPropValueIfPresent(Object, String)} returns a
 * {@link Nullable} (empty when the property is absent or a nested intermediate is {@code null}) rather than
 * throwing, and carries the {@code *IfPresent} suffix. The sibling utility classes spell the same
 * "look up a possibly-absent value without throwing" idea with different verbs: {@code *IfExists} in
 * {@link Maps} (e.g.&nbsp;{@code getIfExists}) and {@code find*} in {@link Iterables}
 * (e.g.&nbsp;{@code findFirstOrLast}); plain {@code getPropValue} returns the raw value, or
 * {@code null}/throws for an unmatched property.</p>
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

    // Shared, stateless splitter for nested property paths (e.g. "address.city"); reused to avoid per-call allocation.
    private static final Splitter PROP_NAME_SPLITTER = Splitter.with(PROP_NAME_SEPARATOR);

    // ...
    private static final String GET = "get";

    private static final String SET = "set";

    private static final String IS = "is";

    private static final String HAS = "has";

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    private static final Map<String, String> camelCasePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<String, String> snakeCasePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<String, String> screamingSnakeCasePropNamePool = new ObjectPool<>(POOL_SIZE * 2);

    private static final Map<Class<?>, Boolean> registeredXmlBindingClassList = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Set<String>> registeredNonPropGetSetMethodPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableList<String>> beanDeclaredPropNameListPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, ImmutableMap<String, Field>> beanDeclaredPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Field>> beanPropFieldPool = new ObjectPool<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Field>> declaredFieldPool = new ObjectPool<>(POOL_SIZE);

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
    private static final Map<Class<?>, BuilderInfo> builderMap = new ConcurrentHashMap<>();
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
     *   <li>Is annotated with {@code @Entity} (including {@code javax.persistence.Entity} or {@code jakarta.persistence.Entity}), or</li>
     *   <li>Is a record class (Java 14+) or annotated with {@code @Record}, or</li>
     *   <li>Has at least one property with getter/setter methods and is not a
     *       {@link CharSequence}, {@link Number}, {@link Map}, {@link Collection}, or {@link Map.Entry} implementation</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.isBeanClass(User.class);      // returns true  (typical POJO with getters/setters)
     * Beans.isBeanClass(Integer.class);   // returns false (Number subclass)
     * Beans.isBeanClass(String.class);    // returns false (CharSequence)
     * Beans.isBeanClass(null);            // returns false
     * }</pre>
     *
     * @param cls the class to be checked.
     * @return {@code true} if the specified class is a bean class, {@code false} otherwise.
     */
    public static boolean isBeanClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        Boolean ret = beanClassPool.get(cls);

        if (ret == null) {
            ret = annotatedWithEntity(cls) || isRecordClass(cls)
                    || (!CharSequence.class.isAssignableFrom(cls) && !Number.class.isAssignableFrom(cls) && !Map.class.isAssignableFrom(cls)
                            && !Collection.class.isAssignableFrom(cls) && !Map.Entry.class.isAssignableFrom(cls) && N.notEmpty(getPropNameList(cls)));
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
     * Beans.isRecordClass(Point.class);    // returns true  (extends java.lang.Record)
     * Beans.isRecordClass(User.class);     // returns false (regular bean)
     * Beans.isRecordClass(String.class);   // returns false
     * Beans.isRecordClass(null);           // returns false
     * }</pre>
     *
     * @param cls the class to be checked.
     * @return {@code true} if the specified class is a record class, {@code false} otherwise.
     */
    public static boolean isRecordClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        return recordClassPool.computeIfAbsent(cls, k -> (recordClass != null && recordClass.isAssignableFrom(cls)) || cls.getAnnotation(Record.class) != null);
    }

    private static final BuilderInfo NO_BUILDER_INFO = new BuilderInfo(null, null, null);

    /**
     * Retrieves or creates a {@link BeanInfo} instance for the specified type.
     *
     * <p>This method maintains a cache of BeanInfo instances to improve performance.
     * The BeanInfo contains metadata about the class including property information,
     * annotations, and type details.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BeanInfo beanInfo = Beans.getBeanInfo(User.class);   // beanInfo is cached metadata for User
     * for (PropInfo prop : beanInfo.propInfoList) {
     *     System.out.println(prop.name + ": " + prop.clazz);   // prints e.g. "name: class java.lang.String"
     * }
     *
     * Beans.getBeanInfo(String.class);   // throws IllegalArgumentException (not a bean class)
     * }</pre>
     *
     * @param beanType the bean type to get bean information for.
     * @return a {@link BeanInfo} instance containing metadata about the specified type.
     * @see ParserUtil#getBeanInfo(Class)
     */
    public static BeanInfo getBeanInfo(final java.lang.reflect.Type beanType) {
        return ParserUtil.getBeanInfo(beanType);
    }

    /**
     * Refreshes the cached bean property information for the specified class.
     *
     * <p>This method invalidates the cached {@link BeanInfo} for the specified class, forcing
     * it to be recreated on the next call to {@link #getBeanInfo(java.lang.reflect.Type)}.
     * This is only needed when a class's structure is modified dynamically at runtime.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // After dynamically modifying a class
     * Beans.refreshBeanPropInfo(ModifiedClass.class);
     * }</pre>
     *
     * @param cls the class whose cached bean property information should be refreshed.
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
     * The builder information includes the builder class type, a factory for creating builder instances,
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
     * // Person has a static builder() method returning a Builder with a build() method
     * Beans.BuilderInfo info = Beans.getBuilderInfo(Person.class);   // returns a non-null BuilderInfo
     * if (info != null) {
     *     Object builder = info.newBuilder();      // returns a new builder
     *     // ... populate the builder ...
     *     Object instance = info.build(builder);   // returns the built instance
     * }
     *
     * Beans.getBuilderInfo(User.class);   // returns null (no builder pattern detected)
     * Beans.getBuilderInfo(null);         // throws IllegalArgumentException
     * }</pre>
     *
     * @param cls the class for which the builder information is to be retrieved.
     * @return a {@link BuilderInfo} describing the builder class, a builder factory, and a build function,
     *         or {@code null} if no builder pattern is detected for the class.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    @MayReturnNull
    public static BuilderInfo getBuilderInfo(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        BuilderInfo builderInfo = builderMap.get(cls);

        if (builderInfo == null) {
            Method buildMethod = null;
            Class<?> builderClass = null;
            Method builderMethod = getBuilderMethod(cls);

            if (builderMethod == null) {
                for (final Class<?> declaredClass : cls.getDeclaredClasses()) {
                    if (getBuilderMethod(declaredClass) != null && getBuildMethod(declaredClass, cls) != null) {
                        builderClass = declaredClass;

                        break;
                    }
                }

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

                    builderInfo = new BuilderInfo(builderClass, builderSupplier, buildFunc);

                    builderMap.put(cls, builderInfo);

                    return builderInfo;
                }
            }

            builderInfo = NO_BUILDER_INFO;
            builderMap.put(cls, builderInfo);
        }

        return builderInfo.builderClass == null ? null : builderInfo;
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
     * The builder metadata for a bean class, as returned by {@link #getBuilderInfo(Class)}: the builder
     * class, a factory that creates a new (empty) builder instance, and a function that builds the target
     * bean from a populated builder.
     */
    public static final class BuilderInfo {
        private final Class<?> builderClass;
        private final com.landawn.abacus.util.function.Supplier<Object> builderSupplier;
        private final com.landawn.abacus.util.function.Function<Object, Object> buildFunc;

        BuilderInfo(final Class<?> builderClass, final com.landawn.abacus.util.function.Supplier<Object> builderSupplier,
                final com.landawn.abacus.util.function.Function<Object, Object> buildFunc) {
            this.builderClass = builderClass;
            this.builderSupplier = builderSupplier;
            this.buildFunc = buildFunc;
        }

        /**
         * Returns the builder class (the type returned by the discovered {@code builder()} /
         * {@code newBuilder()} / {@code createBuilder()} method).
         *
         * @return the builder class.
         */
        public Class<?> builderClass() {
            return builderClass;
        }

        /**
         * Creates and returns a new, empty builder instance by invoking the discovered builder factory method.
         *
         * @return a new builder instance.
         */
        public Object newBuilder() {
            return builderSupplier.get();
        }

        /**
         * Builds the target bean from the given (populated) builder instance by invoking the builder's
         * {@code build()} / {@code create()} method.
         *
         * @param builder a builder instance, typically obtained from {@link #newBuilder()}.
         * @return the built bean instance.
         */
        public Object build(final Object builder) {
            return buildFunc.apply(builder);
        }
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
     * Beans.isBeanClass(Money.class);        // returns true before registration
     *
     * // From now on Money is treated as a simple value type during introspection.
     * Beans.registerNonBeanClass(Money.class);
     *
     * Beans.registerNonBeanClass(Money.class);   // no exception thrown
     * }</pre>
     *
     * @param cls the class to be registered as a non-bean class.
     */
    @SuppressWarnings("deprecation")
    public static void registerNonBeanClass(final Class<?> cls) {
        registeredNonBeanClass.put(cls, cls);

        synchronized (beanDeclaredPropGetMethodPool) {
            // isBeanClass caches its result; without invalidation it would keep answering true
            // for a class that is now (per this registration) treated as a simple value type.
            beanClassPool.remove(cls);

            registeredXmlBindingClassList.put(cls, false);

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
     * // Exclude the "internal" property (getInternal/setInternal) from introspection
     * // Now MyClass.getInternal() won't be considered a property getter
     * Beans.registerNonPropertyAccessor(MyClass.class, "internal");
     *
     * // Registering a name that is not even a property is harmless (no exception)
     * Beans.registerNonPropertyAccessor(MyClass.class, "nonexistent");
     * }</pre>
     *
     * @param cls the class for which the non-property get/set method is to be registered.
     * @param propName the name of the property to be registered as a non-property get/set method.
     */
    @SuppressWarnings("deprecation")
    public static void registerNonPropertyAccessor(final Class<?> cls, final String propName) {
        synchronized (registeredNonPropGetSetMethodPool) {
            Set<String> set = registeredNonPropGetSetMethodPool.computeIfAbsent(cls, k -> N.newHashSet());

            set.add(propName);

            ParserUtil.refreshBeanPropInfo(cls);
        }
    }

    /**
     * Registers a getter or setter method as the property accessor for the specified property name.
     * The method must be recognized as a getter (starts with {@code get}, {@code is}, or {@code has},
     * or matches a field name) or a setter (starts with {@code set}, or matches a field name).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Method customGetter = MyClass.class.getMethod("getFullName");
     *
     * // Now getFullName() is registered as the getter for property "name"
     * Beans.registerPropertyAccessor("name", customGetter);
     *
     * Method toString = Object.class.getMethod("toString");
     * Beans.registerPropertyAccessor("x", toString);   // throws IllegalArgumentException (not a getter/setter)
     * }</pre>
     *
     * @param propName the name of the property to associate with the method.
     * @param method the getter or setter method to register as the property accessor.
     * @throws IllegalArgumentException if the method is not a valid getter or setter,
     *         or if {@code propName} is already registered with a different method.
     */
    @SuppressWarnings("deprecation")
    public static void registerPropertyAccessor(final String propName, final Method method) {
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
     * Beans.isRegisteredXmlBindingClass(JaxbBean.class);   // returns false initially
     * Beans.registerXmlBindingClass(JaxbBean.class);
     * Beans.isRegisteredXmlBindingClass(JaxbBean.class);   // returns true
     *
     * Beans.registerXmlBindingClass(JaxbBean.class);   // no exception thrown
     * }</pre>
     *
     * @param cls the class to be registered for XML binding.
     */
    @SuppressWarnings("deprecation")
    public static void registerXmlBindingClass(final Class<?> cls) {
        // The map also holds FALSE entries (registerNonBeanClass / instantiation-failure demotion),
        // so only an existing TRUE registration may short-circuit.
        if (Boolean.TRUE.equals(registeredXmlBindingClassList.get(cls))) {
            return;
        }

        synchronized (beanDeclaredPropGetMethodPool) {
            registeredXmlBindingClassList.put(cls, true);

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
     * Beans.isRegisteredXmlBindingClass(JaxbBean.class);   // returns false (not registered yet)
     * Beans.registerXmlBindingClass(JaxbBean.class);
     * Beans.isRegisteredXmlBindingClass(JaxbBean.class);   // returns true
     * }</pre>
     *
     * @param cls the class to check.
     * @return {@code true} if the class is registered for XML binding, {@code false} otherwise.
     */
    public static boolean isRegisteredXmlBindingClass(final Class<?> cls) {
        // The map also holds FALSE entries (registerNonBeanClass / instantiation-failure demotion).
        return Boolean.TRUE.equals(registeredXmlBindingClassList.get(cls));
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
     * <p>If the method name does not follow an accessor pattern ({@code get}/{@code set}/{@code is}/{@code has}
     * prefix) and no backing field matches, the raw method name itself is returned as a fallback
     * (e.g. {@code toString} for {@code Object.toString()}); this method does not return {@code null}
     * or throw for a non-accessor method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.getPropNameByMethod(User.class.getMethod("getName"));           // returns "name"
     * Beans.getPropNameByMethod(User.class.getMethod("setAge", int.class)); // returns "age"
     * Beans.getPropNameByMethod(User.class.getMethod("getActive"));         // returns "active"
     * Beans.getPropNameByMethod(null);                                      // throws NullPointerException
     * }</pre>
     *
     * @param getSetMethod the method whose property name is to be retrieved.
     * @return the property name associated with the specified method, or the raw method name itself
     *         if the method does not look like a getter/setter and no backing field matches.
     * @throws NullPointerException if {@code getSetMethod} is {@code null}
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

            if (Strings.isEmpty(propName)) {
                field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + methodName);

                if (field != null && field.getType().isAssignableFrom(targetType)) {
                    propName = field.getName();
                }
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
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), Beans.normalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName) && Strings.isNotEmpty(newName) && newName.charAt(0) != '_') {
                    field = getDeclaredField(getSetMethod.getDeclaringClass(), "_" + Beans.normalizePropName(newName));

                    if (field != null && field.getType().isAssignableFrom(targetType)) {
                        propName = field.getName();
                    }
                }

                if (Strings.isEmpty(propName)) {
                    propName = Beans.normalizePropName(newName);
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
     * following JavaBean conventions. The order reflects the field declaration
     * order in the class hierarchy.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // class User { String name; int age; Boolean active; ...accessors... }
     * Beans.getPropNameList(User.class);   // returns ["name", "age", "active"] (field-declaration order)
     * Beans.getPropNameList(null);         // throws IllegalArgumentException
     * }</pre>
     *
     * @param cls the class whose property names are to be retrieved; must not be {@code null}.
     * @return an immutable list of property names for the specified class.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
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
     * // class User { String name; int age; Boolean active; ... }
     * Beans.getPropNames(User.class, Arrays.asList("age"));           // returns ["name", "active"]
     * Beans.getPropNames(User.class, Arrays.asList("age", "active")); // returns ["name"]
     * Beans.getPropNames(User.class, (Collection<String>) null);      // returns ["name", "age", "active"]
     * Beans.getPropNames((Class<?>) null, Arrays.asList("age"));      // throws IllegalArgumentException
     * }</pre>
     *
     * @param cls the class whose property names are to be retrieved; must not be {@code null}.
     * @param propNameToExclude the collection of property names to exclude from the result.
     * @return a list of property names for the specified class, excluding the specified property names.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
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
     * // class User { String name; int age; Boolean active; ... }
     * Beans.getPropNames(User.class, Set.of("age", "active"));   // returns ["name"]
     * Beans.getPropNames(User.class, Set.of());                  // returns ["name", "age", "active"]
     * Beans.getPropNames(User.class, (Set<String>) null);        // returns ["name", "age", "active"]
     * Beans.getPropNames((Class<?>) null, Set.of());             // throws IllegalArgumentException
     * }</pre>
     *
     * @param cls the class whose property names are to be retrieved; must not be {@code null}.
     * @param propNameToExclude the set of property names to exclude from the result.
     * @return a list of property names for the specified class, excluding the specified property names.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    public static List<String> getPropNames(final Class<?> cls, final Set<String> propNameToExclude) {
        N.checkArgNotNull(cls, cs.cls);

        final ImmutableList<String> propNameList = getPropNameList(cls);

        if (N.isEmpty(propNameToExclude)) {
            return new ArrayList<>(propNameList);
        }

        final List<String> result = new ArrayList<>(N.max(0, propNameList.size() - propNameToExclude.size()));

        for (final String propName : propNameList) {
            if (!propNameToExclude.contains(propName)) {
                result.add(propName);
            }
        }

        return result;
    }

    static final BiPredicate<String, Object> NON_PROP_VALUE = (propName, propValue) -> propValue != null;

    /**
     * Retrieves the property names of the given bean object, optionally filtering out
     * properties with {@code null} values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     *
     * Beans.getPropNames(user, false);   // returns ["name", "age", "active"] (all props)
     * Beans.getPropNames(user, true);    // returns ["name", "age"] (active is null, excluded)
     * }</pre>
     *
     * @param bean the bean object whose property names are to be retrieved; must not be {@code null}
     *        (a {@code NullPointerException} is thrown otherwise).
     * @param ignoreNullValue if {@code true}, properties with {@code null} values are excluded from the result.
     * @return a mutable list of property names of the given bean object;
     *         properties with {@code null} values are excluded when {@code ignoreNullValue} is {@code true}.
     * @see #getPropNameList(Class)
     * @see #getPropNames(Object, Predicate)
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
     * User user = new User("John", 25);
     *
     * Beans.getPropNames(user, name -> name.startsWith("a"));   // returns ["age", "active"]
     * Beans.getPropNames(user, name -> false);                  // returns [] (empty)
     * Beans.getPropNames(user, name -> true);                   // returns ["name", "age", "active"]
     * }</pre>
     *
     * @param bean the bean object whose property names are to be retrieved; must not be {@code null}
     *        (a {@code NullPointerException} is thrown otherwise).
     * @param propNameFilter the predicate to filter property names.
     * @return a list of property names for the specified bean, filtered by the given predicate.
     */
    public static List<String> getPropNames(final Object bean, final Predicate<? super String> propNameFilter) {
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
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * // String-valued properties
     * Beans.getPropNames(user,
     *     (name, value) -> value instanceof String);   // returns ["name"]
     * // Numeric properties greater than 20
     * Beans.getPropNames(user,
     *     (name, value) -> value instanceof Number && ((Number) value).intValue() > 20);   // returns ["age"]
     * }</pre>
     *
     * @param bean the bean object whose property names are to be retrieved; must not be {@code null}
     *        (a {@code NullPointerException} is thrown otherwise).
     * @param propNameValueFilter the bi-predicate to filter property names and values, where the first parameter is the property name and the second parameter is the property value.
     * @return a list of property names for the specified bean, filtered by the given bi-predicate.
     */
    public static List<String> getPropNames(final Object bean, final BiPredicate<? super String, Object> propNameValueFilter) {
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
     * Retrieves an immutable set of property names that are excluded from diff operations
     * (e.g., {@link com.landawn.abacus.util.Difference.BeanDifference#of(Object, Object)}) for the specified class.
     *
     * <p>A property is excluded if it is annotated with {@link DiffIgnore}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User {
     *     private String name;
     *     @DiffIgnore
     *     private Date lastModified;
     * }
     *
     * Beans.getIgnoredPropNamesForDiff(User.class);    // returns ["lastModified"]
     * Beans.getIgnoredPropNamesForDiff(Address.class); // returns [] (no @DiffIgnore properties)
     * }</pre>
     *
     * @param cls the class for which the diff-ignored property names are to be retrieved.
     * @return an immutable set of property names excluded from diff operations; never {@code null}.
     * @see com.landawn.abacus.util.Difference.MapDifference
     * @see com.landawn.abacus.util.Difference.BeanDifference#of(Object, Object)
     */
    public static ImmutableSet<String> getIgnoredPropNamesForDiff(final Class<?> cls) {
        ImmutableSet<String> propNames = beanDiffIgnoredPropNamesPool.get(cls);

        if (propNames == null) {
            propNames = Stream.of(ParserUtil.getBeanInfo(cls).propInfoList)
                    // annotationType(), not getClass(): annotation instances are JDK dynamic proxies
                    // whose simple name is "$ProxyN", so getClass() would never match.
                    .filter(propInfo -> propInfo.isAnnotationPresent(DiffIgnore.class) || propInfo.annotations.values()
                            .stream()
                            .anyMatch(it -> Strings.equalsAnyIgnoreCase(it.annotationType().getSimpleName(), "DiffIgnore", "DifferenceIgnore")))
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
                || (!fieldName.isEmpty() && fieldName.charAt(0) == '_' && propName.equalsIgnoreCase(fieldName.substring(1)));
    }

    private static boolean isGetMethod(final Method method) {
        if (Object.class.equals(method.getDeclaringClass()) || Modifier.isStatic(method.getModifiers())) {
            return false;
        }

        final String mn = method.getName();

        return (mn.startsWith(GET) || mn.startsWith(IS) || mn.startsWith(HAS) || getDeclaredField(method.getDeclaringClass(), mn) != null)
                && (N.isEmpty(method.getParameterTypes())) && !void.class.equals(method.getReturnType()) && !nonGetSetMethodName.contains(mn);
    }

    private static boolean isJAXBGetMethod(final Class<?> cls, final Object instance, final Method method, final Field field) {
        try {
            return (instance != null)
                    && ((registeredXmlBindingClassList.getOrDefault(cls, false) || N.anyMatch(cls.getAnnotations(), Beans::isXmlTypeAnno))
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
            throw new IllegalArgumentException("The property name exceeds 128 characters: " + inputPropName);
        }

        inputPropName = inputPropName.trim();

        if (inputPropName.equalsIgnoreCase(propNameByMethod)) {
            return true;
        }

        if (inputPropName.indexOf(SK._UNDERSCORE) >= 0 && inputPropName.replace(SK.UNDERSCORE, Strings.EMPTY).equalsIgnoreCase(propNameByMethod)) {
            return true;
        }

        final String simpleClassName = ClassUtil.getSimpleClassName(cls);

        if (inputPropName.length() == (simpleClassName.length() + 1 + propNameByMethod.length())
                && inputPropName.equalsIgnoreCase(simpleClassName + SK._PERIOD + propNameByMethod)) {
            return true;
        }

        return (inputPropName.startsWith(GET) && inputPropName.length() > 3 && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(SET) && inputPropName.length() > 3 && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(IS) && inputPropName.length() > 2 && inputPropName.substring(2).equalsIgnoreCase(propNameByMethod))
                || (inputPropName.startsWith(HAS) && inputPropName.length() > 3 && inputPropName.substring(3).equalsIgnoreCase(propNameByMethod));
    }

    private static boolean isSetMethod(final Method method) {
        final String mn = method.getName();

        return !Modifier.isStatic(method.getModifiers()) && (mn.startsWith(SET) || getDeclaredField(method.getDeclaringClass(), mn) != null)
                && N.len(method.getParameterTypes()) == 1
                && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(method.getDeclaringClass()))
                && !nonGetSetMethodName.contains(mn);
    }

    /**
     * Loads the property getter and setter method list for the specified class.
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

                    if (registeredXmlBindingClassList.containsKey(cls)) {
                        registeredXmlBindingClassList.put(cls, false);
                    }
                }
            }

            final List<Class<?>> allClasses = new ArrayList<>();
            allClasses.add(cls);
            Class<?> superClass = null;

            while ((superClass = allClasses.get(allClasses.size() - 1).getSuperclass()) != null && !superClass.equals(Object.class)) {
                allClasses.add(superClass);
            }

            final BuilderInfo builderInfo = getBuilderInfo(cls);
            final Class<?> builderClass = builderInfo == null ? null : builderInfo.builderClass();

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
                final Field[] declaredFields = clazz.getDeclaredFields();
                final Method[] methods = clazz.getMethods();

                final List<Tuple2<Field, Method>> fieldGetMethodList = new ArrayList<>(declaredFields.length);

                // sort the methods by the order of declared fields
                for (final Field field : declaredFields) {
                    Method fieldGetMethod = null;

                    for (final Method method : methods) {
                        if (isFieldGetMethod(method, field) && (fieldGetMethod == null || method.getName().length() >= fieldGetMethod.getName().length())) {
                            fieldGetMethod = method;
                        }
                    }

                    if (fieldGetMethod == null) {
                        if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
                            fieldGetMethodList.add(Tuple.of(field, null));
                        }
                    } else {
                        fieldGetMethodList.add(Tuple.of(field, fieldGetMethod));
                    }
                }

                if (noArgConstructor == null && clazz == cls) {
                    int argCount = 0;

                    for (final Tuple2<Field, Method> tp : fieldGetMethodList) {
                        if (tp._2 != null) {
                            argCount++;
                        }
                    }

                    final Class<?>[] args = new Class<?>[argCount];
                    int argIndex = 0;

                    for (final Tuple2<Field, Method> tp : fieldGetMethodList) {
                        if (tp._2 != null) {
                            args[argIndex++] = tp._1.getType();
                        }
                    }

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

                for (final Method method : methods) {
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
                final Method[] builderMethods = builderClass.getMethods();

                for (final Method method : builderMethods) {
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
     * Returns {@code null} if no field is found by the specified name.
     *
     * @param cls the class from which the field is to be retrieved.
     * @param fieldName the name of the field to retrieve.
     * @return the declared field with the specified name, or {@code null} if not found.
     */
    private static Field getDeclaredField(final Class<?> cls, final String fieldName) {
        Map<String, Field> fieldMap = declaredFieldPool.get(cls);

        if (fieldMap == null) {
            synchronized (declaredFieldPool) {
                fieldMap = declaredFieldPool.get(cls);

                if (fieldMap == null) {
                    Field[] fields = null;

                    try {
                        fields = cls.getDeclaredFields();
                    } catch (final SecurityException e) {
                        // ignore
                    }

                    fieldMap = new ObjectPool<>(fields == null ? 0 : N.max(16, fields.length));

                    if (fields != null) {
                        for (final Field field : fields) {
                            fieldMap.put(field.getName(), field);
                        }
                    }

                    declaredFieldPool.put(cls, fieldMap);
                }
            }
        }

        return fieldMap.get(fieldName);
    }

    private static Map<String, String> getPublicStaticStringFields(final Class<?> cls) {
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
     * Beans.getPropField(User.class, "name").getName();   // returns "name" (the backing field)
     * Beans.getPropField(User.class, "NAME").getName();   // returns "name" (case-insensitive match)
     * Beans.getPropField(User.class, "nonExistent");      // returns null (no such property)
     * }</pre>
     *
     * @param cls the class from which the field is to be retrieved.
     * @param propName the name of the property whose backing field is to be retrieved.
     * @return the field associated with the specified property name, or {@code null} if no matching field is found and the class is a bean class.
     * @throws IllegalArgumentException if no matching field is found and the specified class is not a bean class
     *         (i.e. it has no property getter/setter method or public field).
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
                final Map<String, Method> getterMethodList = getPropGetters(cls);

                for (final String key : getterMethodList.keySet()) {
                    if (Beans.isPropName(cls, propName, key)) {
                        field = propFieldMap.get(key);

                        break;
                    }
                }

                if ((field == null) && !propName.equalsIgnoreCase(Beans.normalizePropName(propName))) {
                    field = getPropField(cls, Beans.normalizePropName(propName));
                }

                // set method mask to avoid a query next time.
                if (field == null) {
                    field = ClassUtil.SENTINEL_FIELD;
                }

                //    } else {
                //       ClassUtil.setAccessibleQuietly(field, true);
                //    }

                propFieldMap.put(propName, field);
            }
        }

        return (field == ClassUtil.SENTINEL_FIELD) ? null : field;
    }

    /**
     * Returns an immutable map of the backing fields for all bean properties of the specified class,
     * keyed by property name.
     *
     * <p>Only properties that have a corresponding declared field in the class hierarchy are included.
     * Properties backed solely by getter/setter methods without a matching field are not included.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Field> fields = Beans.getPropFields(User.class);
     * fields.containsKey("name");          // returns true
     * fields.get("name").getType();        // returns class java.lang.String
     * fields.get("nonExistent");           // returns null
     * }</pre>
     *
     * @param cls the class whose property fields are to be retrieved.
     * @return an immutable map of property name to backing {@link Field} for the specified class; never {@code null}.
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
     * {@code null} is returned if no matching method is found and {@code cls} is a bean class.
     *
     * <p>Call {@link #registerXmlBindingClass(Class)} first to retrieve the property
     * getter/setter method for a class/bean generated according to the JAXB specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.getPropGetter(User.class, "name").getName();   // returns "getName"
     * Beans.getPropGetter(User.class, "NAME").getName();   // returns "getName" (case-insensitive)
     * Beans.getPropGetter(User.class, "nonExistent");      // returns null (User is a bean class)
     * }</pre>
     *
     * @param cls the class from which the property get method is to be retrieved.
     * @param propName the name of the property whose get method is to be retrieved.
     * @return the property get method declared in the specified class, or {@code null} if no matching method
     *         is found and the class is a bean class.
     * @throws IllegalArgumentException if no matching method is found and the specified class is not a bean class
     *         (i.e. it has no property getter/setter method or public field).
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Method getPropGetter(final Class<?> cls, final String propName) {
        Map<String, Method> propGetMethodMap = beanPropGetMethodPool.get(cls);

        if (propGetMethodMap == null) {
            Beans.loadPropGetSetMethodList(cls);
            propGetMethodMap = beanPropGetMethodPool.get(cls);
        }

        Method method = propGetMethodMap.get(propName);

        if (method == null) {
            if (!Beans.isBeanClass(cls)) {
                throw new IllegalArgumentException(
                        "No property getter/setter method or public field found in the specified bean: " + ClassUtil.getCanonicalClassName(cls));
            }

            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> getterMethodList = getPropGetters(cls);

                for (final Map.Entry<String, Method> entry : getterMethodList.entrySet()) { //NOSONAR
                    if (Beans.isPropName(cls, propName, entry.getKey())) {
                        method = entry.getValue();

                        break;
                    }
                }

                if ((method == null) && !propName.equalsIgnoreCase(Beans.normalizePropName(propName))) {
                    method = getPropGetter(cls, Beans.normalizePropName(propName));
                }

                // set method mask to avoid query next time.
                if (method == null) {
                    method = ClassUtil.SENTINEL_METHOD;
                }

                propGetMethodMap.put(propName, method);
            }
        }

        return (method == ClassUtil.SENTINEL_METHOD) ? null : method;
    }

    /**
     * Retrieves an immutable map of property get methods for the specified class.
     *
     * <p>Call {@link #registerXmlBindingClass(Class)} first to retrieve the property
     * getter/setter method for a class/bean generated according to the JAXB specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Method> getters = Beans.getPropGetters(User.class);
     * getters.get("name").getName();   // returns "getName"
     * getters.containsKey("age");      // returns true
     * getters.get("nonExistent");      // returns null
     * }</pre>
     *
     * @param cls the class from which the property getter methods are to be retrieved.
     * @return an immutable map of property name to getter {@link Method} for the specified class; never {@code null}.
     */
    public static ImmutableMap<String, Method> getPropGetters(final Class<?> cls) {
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
     * {@code null} is returned if no matching method is found and {@code cls} is a bean class.
     *
     * <p>Call {@link #registerXmlBindingClass(Class)} first to retrieve the property
     * getter/setter method for a class/bean generated according to the JAXB specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.getPropSetter(User.class, "name").getName();   // returns "setName"
     * Beans.getPropSetter(User.class, "NAME").getName();   // returns "setName" (case-insensitive)
     * Beans.getPropSetter(User.class, "nonExistent");      // returns null (User is a bean class)
     * }</pre>
     *
     * @param cls the class from which the property set method is to be retrieved.
     * @param propName the name of the property whose set method is to be retrieved.
     * @return the property set method declared in the specified class, or {@code null} if no matching method
     *         is found and the class is a bean class.
     * @throws IllegalArgumentException if no matching method is found and the specified class is not a bean class
     *         (i.e. it has no property getter/setter method or public field).
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Method getPropSetter(final Class<?> cls, final String propName) {
        Map<String, Method> propSetMethodMap = beanPropSetMethodPool.get(cls);

        if (propSetMethodMap == null) {
            Beans.loadPropGetSetMethodList(cls);
            propSetMethodMap = beanPropSetMethodPool.get(cls);
        }

        Method method = propSetMethodMap.get(propName);

        if (method == null) {
            if (!Beans.isBeanClass(cls)) {
                throw new IllegalArgumentException(
                        "No property getter/setter method or public field found in the specified bean: " + ClassUtil.getCanonicalClassName(cls));
            }

            synchronized (beanDeclaredPropGetMethodPool) {
                final Map<String, Method> setterMethodList = getPropSetters(cls);

                for (final Map.Entry<String, Method> entry : setterMethodList.entrySet()) {
                    if (Beans.isPropName(cls, propName, entry.getKey())) {
                        method = entry.getValue();

                        break;
                    }
                }

                if ((method == null) && !propName.equalsIgnoreCase(Beans.normalizePropName(propName))) {
                    method = getPropSetter(cls, Beans.normalizePropName(propName));
                }

                // set method mask to avoid a query next time.
                if (method == null) {
                    method = ClassUtil.SENTINEL_METHOD;
                }

                propSetMethodMap.put(propName, method);
            }
        }

        return (method == ClassUtil.SENTINEL_METHOD) ? null : method;
    }

    /**
     * Retrieves an immutable map of property set methods for the specified class.
     *
     * <p>Call {@link #registerXmlBindingClass(Class)} first to retrieve the property
     * getter/setter method for a class/bean generated according to the JAXB specification.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Method> setters = Beans.getPropSetters(User.class);
     * setters.get("name").getName();   // returns "setName"
     * setters.containsKey("age");      // returns true
     * setters.get("nonExistent");      // returns null
     * }</pre>
     *
     * @param cls the class from which the property setter methods are to be retrieved.
     * @return an immutable map of property name to setter {@link Method} for the specified class; never {@code null}.
     */
    public static ImmutableMap<String, Method> getPropSetters(final Class<?> cls) {
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
     * User user = new User("John", 25);
     * Method getName = User.class.getMethod("getName");
     * Beans.getPropValue(user, getName);   // returns "John" (same as user.getName())
     *
     * Method getAge = User.class.getMethod("getAge");
     * int age = Beans.getPropValue(user, getAge);   // returns 25
     * }</pre>
     *
     * @param <T> the type of the property value.
     * @param bean the object from which the property value is to be retrieved.
     * @param propGetMethod the getter method to invoke on the bean.
     * @return the value returned by the getter method; may be {@code null}.
     * @throws RuntimeException wrapping {@link IllegalAccessException} or {@link InvocationTargetException}
     *         if the method cannot be invoked.
     */
    @SuppressWarnings("unchecked")
    @MayReturnNull
    public static <T> T getPropValue(final Object bean, final Method propGetMethod) {
        try {
            return (T) propGetMethod.invoke(bean);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Returns the value of the specified property by invoking the getter method associated
     * with the given property name on the provided bean.
     *
     * <p>This is a convenience overload equivalent to
     * {@link #getPropValue(Object, String, boolean) getPropValue(bean, propName, false)},
     * which throws {@link IllegalArgumentException} if the property is not found.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * String name = Beans.getPropValue(user, "name");    // returns "John"
     * Integer age = Beans.getPropValue(user, "age");     // returns 25
     * Beans.getPropValue(user, "nonExistent");           // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the property value.
     * @param bean the object from which the property value is to be retrieved; must not be {@code null}.
     * @param propName the name of the property whose value is to be retrieved.
     * @return the value of the specified property; may be {@code null}.
     * @throws IllegalArgumentException if no property with the given name is found.
     * @see #getPropValue(Object, Method)
     * @see #getPropValue(Object, String, boolean)
     */
    @MayReturnNull
    public static <T> T getPropValue(final Object bean, final String propName) {
        return getPropValue(bean, propName, false);
    }

    /**
     * Returns the value of the specified property by invoking the getter method associated with the given property name on the provided bean.
     * If the property cannot be found and ignoreUnmatchedProperty is {@code true}, it returns {@code null}.
     *
     * <p>This method also supports nested properties using dot notation.</p>
     *
     * <p>For nested paths, if an intermediate property resolves to {@code null}, this method returns
     * the default value of the final getter's return type (e.g., {@code 0} for primitive numeric types,
     * {@code false} for {@code boolean}); this is non-{@code null} only when the final getter's return
     * type is a primitive type &mdash; for a wrapper or reference leaf type the default is {@code null}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Order order = new Order();
     * order.setAddress(new Address("NYC"));
     *
     * // Simple property
     * Beans.getPropValue(order, "id", false);            // returns null (id not set)
     *
     * // Nested property via dot notation
     * Beans.getPropValue(order, "address.city", false);  // returns "NYC"
     *
     * // Non-existent property with ignore flag
     * Beans.getPropValue(order, "unknown", true);        // returns null
     * Beans.getPropValue(order, "unknown", false);       // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the property value.
     * @param bean the object from which the property value is to be retrieved; must not be {@code null}.
     * @param propName the name of the property whose value is to be retrieved; supports dot notation for nested properties.
     * @param ignoreUnmatchedProperty if {@code true}, returns {@code null} when the property is not found;
     *        if {@code false}, throws {@link IllegalArgumentException}.
     * @return the value of the specified property, or {@code null} if the property is not found and
     *         {@code ignoreUnmatchedProperty} is {@code true}.
     * @throws IllegalArgumentException if the specified property cannot be found and {@code ignoreUnmatchedProperty} is {@code false}.
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

            final String[] strs = PROP_NAME_SPLITTER.splitToArray(propName);

            if (strs.length > 1) {
                Class<?> targetClass = cls;

                for (final String str : strs) {
                    // A non-bean intermediate type can't be navigated further: stop here (path unresolvable)
                    // rather than letting getPropGetter throw for a non-bean class.
                    if (!isBeanClass(targetClass)) {
                        inlinePropGetMethodQueue.clear();

                        break;
                    }

                    final Method method = getPropGetter(targetClass, str);

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
     * Returns the value of the specified property wrapped in a {@link Nullable}, distinguishing
     * "property present (value may be {@code null})" from "property absent / unreachable".
     *
     * <p>Unlike {@link #getPropValue(Object, String, boolean)} — which returns {@code null} both for a
     * genuinely {@code null} value and for an unmatched property, and returns the leaf type's default value
     * when a nested intermediate is {@code null} — this method returns:</p>
     * <ul>
     *   <li>{@code Nullable.of(value)} when the property is found (the wrapped value may itself be {@code null});</li>
     *   <li>{@code Nullable.empty()} when no property/path matches {@code propName}, or when a nested
     *       intermediate along a dotted path is {@code null} (so the leaf is unreachable).</li>
     * </ul>
     *
     * <p>Dot notation is supported for nested properties (e.g. {@code "address.city"}). A {@code null} value
     * at the final (leaf) segment is reported as present ({@code Nullable.of(null)}); a {@code null} at any
     * intermediate segment is reported as absent ({@code Nullable.empty()}).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Order order = new Order();   // id == null, address == null
     *
     * Beans.getPropValueIfPresent(order, "id");           // Nullable.of(null)  -> present, value null
     * Beans.getPropValueIfPresent(order, "unknown");      // Nullable.empty()   -> no such property
     * Beans.getPropValueIfPresent(order, "address.city"); // Nullable.empty()   -> address is null (unreachable)
     *
     * order.setAddress(new Address("NYC"));
     * Beans.getPropValueIfPresent(order, "address.city"); // Nullable.of("NYC")
     * }</pre>
     *
     * @param <T> the type of the property value.
     * @param bean the object from which the property value is to be retrieved; must not be {@code null}.
     * @param propName the property name; supports dot notation for nested properties.
     * @return a {@link Nullable} holding the property value if present, or {@link Nullable#empty()} if the
     *         property is not found or a nested intermediate is {@code null}; never {@code null}.
     * @see #getPropValue(Object, String, boolean)
     */
    @SuppressWarnings("unchecked")
    public static <T> Nullable<T> getPropValueIfPresent(final Object bean, final String propName) {
        final Class<?> cls = bean.getClass();
        final ParserUtil.PropInfo propInfo = ParserUtil.getBeanInfo(cls).getPropInfo(propName);

        if (propInfo != null) {
            return Nullable.of((T) propInfo.getPropValue(bean));
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

            final String[] strs = PROP_NAME_SPLITTER.splitToArray(propName);

            if (strs.length > 1) {
                Class<?> targetClass = cls;

                for (final String str : strs) {
                    // A non-bean intermediate type can't be navigated further: stop here (path unresolvable)
                    // rather than letting getPropGetter throw for a non-bean class.
                    if (!isBeanClass(targetClass)) {
                        inlinePropGetMethodQueue.clear();

                        break;
                    }

                    final Method method = getPropGetter(targetClass, str);

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

        final int len = inlinePropGetMethodQueue.size();

        if (len == 0) {
            return Nullable.empty();
        }

        Object propBean = bean;

        for (int i = 0; i < len; i++) {
            propBean = getPropValue(propBean, inlinePropGetMethodQueue.get(i));

            if (propBean == null) {
                // null at the leaf segment => present-but-null; null at an intermediate => unreachable.
                return i == len - 1 ? Nullable.of((T) null) : Nullable.empty();
            }
        }

        return Nullable.of((T) propBean);
    }

    /**
     * Sets the specified property value on the given bean by invoking the provided setter method.
     * If the property value is {@code null}, it sets the default value of the parameter type.
     * If the initial attempt to set the property value fails, it tries to convert the property value to the appropriate type and set it again.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     * Method setName = User.class.getMethod("setName", String.class);
     * Beans.setPropValue(user, setName, "John");   // user is updated with name "John"
     *
     * // null value on a primitive setter applies the type default (0 for int)
     * Method setAge = User.class.getMethod("setAge", int.class);
     * Beans.setPropValue(user, setAge, null);      // user is updated with age 0
     * }</pre>
     *
     * @param bean the object on which the property value is to be set.
     * @param propSetMethod the setter method to be invoked on the bean.
     * @param propValue the value to be set; if {@code null}, the type's default value is used instead.
     * @return the actual value that was passed to the setter (after any type conversion); may be {@code null}
     *         when {@code propValue} is {@code null} and the setter's parameter type has a {@code null} default
     *         value (e.g. an object/reference type).
     * @throws RuntimeException wrapping {@link IllegalAccessException} or {@link InvocationTargetException}
     *         if the method cannot be invoked and conversion also fails.
     */
    @MayReturnNull
    public static Object setPropValue(final Object bean, final Method propSetMethod, Object propValue) {
        final Class<?>[] paramTypes = propSetMethod.getParameterTypes();

        if (propValue == null) {
            if (paramTypes.length > 0) {
                propValue = N.defaultValueOf(paramTypes[0]);
            }

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
                if (logger.isDebugEnabled()) {
                    // Recoverable: falls through to a type-converting retry below, so this is a routine
                    // fallback, not a failure worth a WARN.
                    logger.debug("Failed to set value for field by method: {} in class: {} with value type {}; will retry after type conversion",
                            propSetMethod.getName(), propSetMethod.getDeclaringClass().getName(), propValue.getClass().getName());
                }

                final PropInfo propInfo = ParserUtil.getBeanInfo(bean.getClass()).getPropInfo(getPropNameByMethod(propSetMethod));

                if (propInfo != null) {
                    propValue = N.convert(propValue, propInfo.jsonXmlType);

                    try {
                        propSetMethod.invoke(bean, propValue);
                    } catch (IllegalAccessException | InvocationTargetException e2) {
                        e.addSuppressed(e2);
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }
                } else {
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
     * Beans.setPropValue(user, "name", "John");   // user is updated with name "John"
     * Beans.setPropValue(user, "age", 25);        // user is updated with age 25
     * Beans.setPropValue(user, "nonExistent", 1); // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the object on which the property value is to be set.
     * @param propName the name of the property whose value is to be set.
     * @param propValue the value to set; if {@code null}, the property's type default is used.
     * @throws IllegalArgumentException if the specified property cannot be found or set.
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
     * // Set simple property
     * Beans.setPropValue(user, "name", "John", false);            // returns true
     *
     * // Ignore unmatched property
     * Beans.setPropValue(user, "unknown", "value", true);         // returns false (not found, ignored)
     * Beans.setPropValue(user, "unknown", "value", false);        // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the object on which the property value is to be set.
     * @param propName the name of the property whose value is to be set.
     * @param propValue the value to set; if {@code null}, the property's type default is used.
     * @param ignoreUnmatchedProperty if {@code true}, returns {@code false} when the property is not found
     *        instead of throwing an exception.
     * @return {@code true} if the property value was set successfully, {@code false} if the property
     *         was not found and {@code ignoreUnmatchedProperty} is {@code true}.
     * @throws IllegalArgumentException if the property cannot be found and {@code ignoreUnmatchedProperty} is {@code false}.
     * @deprecated replaced by {@link ParserUtil.BeanInfo#setPropValue(Object, String, Object, boolean)}
     */
    @Deprecated
    public static boolean setPropValue(final Object bean, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty) {

        return ParserUtil.getBeanInfo(bean.getClass()).setPropValue(bean, propName, propValue, ignoreUnmatchedProperty);
    }

    /**
     * Sets the property value returned by invoking the getter method on the provided bean.
     * The returned type of the get method should be {@code Collection} or {@code Map}.
     * And the specified property value and the returned value must be the same type.
     * If {@code propValue} is {@code null}, this method does nothing.
     *
     * <p>This method is particularly useful for JAXB-style beans where collections
     * are exposed only through getter methods without corresponding setters.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For a JAXB bean with: List<String> getTags() { return tags; }
     * Method getTags = bean.getClass().getMethod("getTags");
     *
     * // bean.getTags() is cleared and repopulated with ["tag1", "tag2"]
     * Beans.setPropValueByGetter(bean, getTags, Arrays.asList("tag1", "tag2"));
     *
     * Beans.setPropValueByGetter(bean, getTags, null);   // no change
     *
     * // Getter returning a non-collection/non-map type is rejected:
     * Method getName = User.class.getMethod("getName");
     * Beans.setPropValueByGetter(new User(), getName, "x");   // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the object on which the property value is to be set.
     * @param propGetMethod the getter method whose return value (a {@link java.util.Collection} or {@link Map})
     *        will be cleared and repopulated with the contents of {@code propValue}.
     * @param propValue the new contents to populate into the existing collection or map;
     *        must be the same collection/map type as the getter's return type.
     *        If {@code null}, the method does nothing.
     * @throws IllegalArgumentException if the getter does not return a {@link java.util.Collection} or {@link Map}.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static void setPropValueByGetter(final Object bean, final Method propGetMethod, final Object propValue) {
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
     * Normalizes the given property name by converting it to camel case and
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
     * Beans.normalizePropName("user_name");       // returns "userName"
     * Beans.normalizePropName("class");           // returns "clazz" (reserved keyword)
     * Beans.normalizePropName("ID");              // returns "id"
     * Beans.normalizePropName("address_line_1");  // returns "addressLine1"
     * }</pre>
     *
     * @param str the property name to be normalized; returned as-is if {@code null} or empty.
     * @return the normalized (camelCase, keyword-mapped) property name, or the original string unchanged
     *         if {@code str} is {@code null} or empty (so {@code null} in yields {@code null} out).
     */
    @MayReturnNull
    public static String normalizePropName(final String str) {
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
     * <p>This is a caching wrapper around {@link Strings#toCamelCase(String)}: the conversion
     * behavior is identical, but results are cached for repeated property-name lookups.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.toCamelCase("user_name");        // returns "userName"
     * Beans.toCamelCase("FIRST_NAME");       // returns "firstName"
     * Beans.toCamelCase("address-line-1");   // returns "addressLine1"
     * Beans.toCamelCase("");                 // returns "" (unchanged)
     * Beans.toCamelCase((String) null);      // returns null
     * }</pre>
     *
     * @param str the string to be converted; returned as-is if {@code null} or empty.
     * @return the camelCase version of the input string, or the original string unchanged if {@code str}
     *         is {@code null} or empty (so {@code null} in yields {@code null} out).
     * @see Strings#toCamelCase(String)
     */
    @MayReturnNull
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
     * <p>This is a caching wrapper around {@link Strings#toSnakeCase(String)}: the conversion
     * behavior is identical, but results are cached for repeated property-name lookups.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.toSnakeCase("userName");      // returns "user_name"
     * Beans.toSnakeCase("FirstName");     // returns "first_name"
     * Beans.toSnakeCase("userID");        // returns "user_id"
     * Beans.toSnakeCase("");              // returns "" (unchanged)
     * Beans.toSnakeCase((String) null);   // returns null
     * }</pre>
     *
     * @param str the string to be converted; returned as-is if {@code null} or empty.
     * @return the snake_case (lowercase with underscores) version of the string, or the original string
     *         unchanged if {@code str} is {@code null} or empty (so {@code null} in yields {@code null} out).
     * @see Strings#toSnakeCase(String)
     */
    @MayReturnNull
    public static String toSnakeCase(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String result = snakeCasePropNamePool.get(str);

        if (result == null) {
            result = Strings.toSnakeCase(str);
            snakeCasePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * Converts the given string to upper case with underscores.
     *
     * <p>This is a caching wrapper around {@link Strings#toScreamingSnakeCase(String)}: the conversion
     * behavior is identical, but results are cached for repeated property-name lookups.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.toScreamingSnakeCase("userName");      // returns "USER_NAME"
     * Beans.toScreamingSnakeCase("firstName");     // returns "FIRST_NAME"
     * Beans.toScreamingSnakeCase("userID");        // returns "USER_ID"
     * Beans.toScreamingSnakeCase("");              // returns "" (unchanged)
     * Beans.toScreamingSnakeCase((String) null);   // returns null
     * }</pre>
     *
     * @param str the string to be converted; returned as-is if {@code null} or empty.
     * @return the SCREAMING_SNAKE_CASE (uppercase with underscores) version of the string, or the original
     *         string unchanged if {@code str} is {@code null} or empty (so {@code null} in yields {@code null} out).
     * @see Strings#toScreamingSnakeCase(String)
     */
    @MayReturnNull
    public static String toScreamingSnakeCase(final String str) {
        if (Strings.isEmpty(str)) {
            return str;
        }

        String result = screamingSnakeCasePropNamePool.get(str);

        if (result == null) {
            result = Strings.toScreamingSnakeCase(str);
            screamingSnakeCasePropNamePool.put(str, result);
        }

        return result;
    }

    /**
     * Converts a map into a bean object of the specified type.
     * This method takes a map where the keys are the property names and the values are the corresponding property values,
     * and transforms it into a bean object of the specified type.
     * The resulting bean object has its properties set to the values from the map.
     * Unmatched properties from the specified map are ignored by default.
     *
     * <p><b>Nested properties.</b> A nested-bean property may be supplied in either form, so this method is the
     * inverse of both {@link #beanToMap(Object)}/{@link #deepBeanToMap(Object)} and {@link #beanToFlatMap(Object)}:
     * <ul>
     *   <li>as a nested {@code Map} value, e.g. {@code "address" -> {"city": "NYC"}} (inverse of {@code deepBeanToMap}); or</li>
     *   <li>as flat, dot-separated keys, e.g. {@code "address.city" -> "NYC"} (inverse of {@code beanToFlatMap}). Any
     *       missing intermediate bean (here {@code address}) is created automatically as the dotted path is resolved;
     *       each intermediate type must itself be a JavaBean (a {@code Record}/immutable intermediate cannot be populated).</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // class User { String name; int age; Address address; ... }  class Address { String city; String zipCode; }
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", 25);
     *
     * // user.getName() returns "John", user.getAge() returns 25
     * User user = Beans.mapToBean(userMap, User.class);
     *
     * // Nested bean supplied as a sub-map (inverse of deepBeanToMap):
     * User u1 = Beans.mapToBean(N.asMap("name", "John", "address", N.asMap("city", "NYC")), User.class);
     * // u1.getAddress().getCity() returns "NYC"
     *
     * // Nested bean supplied as flat, dotted keys (inverse of beanToFlatMap):
     * User u2 = Beans.mapToBean(N.asMap("name", "John", "address.city", "NYC"), User.class);
     * // u2.getAddress().getCity() returns "NYC"
     *
     * Beans.mapToBean((Map<String, Object>) null, User.class);   // returns null
     * }</pre>
     *
     * @param <T> the type of the bean object to be returned.
     * @param map the map to be converted; keys are property names (a dot-separated key targets a nested-bean property)
     *        and values are the property values; if {@code null}, {@code null} is returned.
     * @param targetType the class of the bean to create; must be a valid bean class.
     * @return a new bean of the specified type with properties populated from the map,
     *         or {@code null} if {@code map} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class.
     * @see #mapToBean(Map, boolean, Class)
     * @see #mapToBean(Map, Collection, Class)
     * @see #beanToFlatMap(Object)
     * @see #beanToMap(Object)
     */
    @MayReturnNull
    public static <T> T mapToBean(final Map<String, Object> map, final Class<? extends T> targetType) {
        return mapToBean(map, true, targetType);
    }

    /**
     * Converts a map into a bean object of the specified type, with control over unmatched properties.
     * This method takes a map where the keys are the property names and the values are the corresponding property values,
     * and transforms it into a bean object of the specified type. Map entries with {@code null} values are set on the
     * corresponding bean properties (a {@code null} mapped to a primitive property becomes that type's default value).
     *
     * <p>Nested-bean properties may be supplied either as a nested {@code Map} value or as flat, dot-separated keys
     * (e.g. {@code "address.city"}); see {@link #mapToBean(Map, Class)} for details and examples. Note that a
     * dot-separated key only resolves to a nested property when its dotted form is genuinely unmatched as a whole;
     * if {@code ignoreUnmatchedProperty} is {@code false}, a dotted key whose path cannot be resolved throws.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("unknownField", "value");
     *
     * // Ignore unmatched properties
     * // user.getName() returns "John"; unknownField ignored
     * User user = Beans.mapToBean(userMap, true, User.class);
     *
     * // Don't ignore unmatched properties -> fails on "unknownField"
     * Beans.mapToBean(userMap, false, User.class);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the bean object to be returned.
     * @param map the map to be converted; if {@code null}, {@code null} is returned.
     * @param ignoreUnmatchedProperty if {@code true}, map keys that do not correspond to any bean property
     *        (and cannot be resolved as a dotted nested-property path) are silently ignored; if {@code false},
     *        an {@link IllegalArgumentException} is thrown.
     * @param targetType the class of the bean to create; must be a valid bean class.
     * @return a new bean of the specified type with properties populated from the map,
     *         or {@code null} if {@code map} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class, or if
     *         {@code ignoreUnmatchedProperty} is {@code false} and an unmatched key is encountered.
     * @see #mapToBean(Map, Class)
     * @see #mapToBean(Map, Collection, Class)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T mapToBean(final Map<String, Object> map, final boolean ignoreUnmatchedProperty, final Class<? extends T> targetType) {
        N.checkBeanClass(targetType);

        if (map == null) {
            return null;
        }

        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        ParserUtil.PropInfo propInfo = null;

        String propName = null;
        Object propValue = null;

        for (final Map.Entry<String, Object> entry : map.entrySet()) {
            propName = entry.getKey();
            propValue = entry.getValue();

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, ignoreUnmatchedProperty);
            } else {
                if (propValue != null && propInfo.type.isBean() && Type.of(propValue.getClass()).isMap()) {
                    propInfo.setPropValue(result, mapToBean((Map<String, Object>) propValue, ignoreUnmatchedProperty, propInfo.clazz));
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
     * Only the properties specified in selectPropNames will be set on the bean. If
     * {@code selectPropNames} is {@code null}, all properties are considered. If it is empty,
     * no properties are set.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> userMap = new HashMap<>();
     * userMap.put("name", "John");
     * userMap.put("age", 25);
     *
     * // Only include the "name" property
     * // user.getName() returns "John"; user.getAge() stays at default 0
     * User user = Beans.mapToBean(userMap, Arrays.asList("name"), User.class);
     *
     * Beans.mapToBean((Map<String, Object>) null, Arrays.asList("name"), User.class);   // returns null
     * }</pre>
     *
     * @param <T> the type of the bean object to be returned.
     * @param map the map to be converted; if {@code null}, {@code null} is returned.
     * @param selectPropNames the property names to copy from the map to the bean. If {@code null},
     *        all properties are considered. If empty, no properties are set. Properties not in this
     *        collection are left at their default values.
     * @param targetType the class of the bean to create; must be a valid bean class.
     * @return a new bean of the specified type with the selected properties populated from the map,
     *         or {@code null} if {@code map} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class,
     *         or if a selected property does not exist in the target bean class.
     */
    @MayReturnNull
    public static <T> T mapToBean(final Map<String, Object> map, final Collection<String> selectPropNames, final Class<? extends T> targetType) {
        if (selectPropNames == null) {
            return mapToBean(map, targetType);
        }

        N.checkBeanClass(targetType);

        if (map == null) {
            return null;
        }

        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);
        final Object result = beanInfo.createBeanResult();
        ParserUtil.PropInfo propInfo = null;
        Object propValue = null;

        for (final String propName : selectPropNames) {
            propValue = map.get(propName);

            propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, false);
            } else {
                if (propValue != null && propInfo.type.isBean() && Type.of(propValue.getClass()).isMap()) {
                    propInfo.setPropValue(result, mapToBean((Map<String, Object>) propValue, propInfo.clazz));
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
     * <p>Each map is converted via {@link #mapToBean(Map, Class)}, so a nested-bean property in any map may be
     * supplied either as a nested {@code Map} value (e.g. {@code "address" -> {"city": "NYC"}}) or as flat,
     * dot-separated keys (e.g. {@code "address.city" -> "NYC"}); see {@link #mapToBean(Map, Class)} for full
     * details and examples.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     * userMaps.add(Map.of("name", "John", "age", 25));
     * userMaps.add(Map.of("name", "Jane", "age", 30));
     *
     * // users.size() == 2; users.get(0).getName() == "John"; users.get(1).getName() == "Jane"
     * List<User> users = Beans.mapsToBeans(userMaps, User.class);
     *
     * Beans.mapsToBeans(Collections.emptyList(), User.class);   // returns [] (empty list)
     * }</pre>
     *
     * @param <T> the type of the bean objects to be returned.
     * @param mapList the collection of maps to convert; if {@code null} or empty, an empty list is returned.
     * @param targetType the class of the bean to create for each map; must be a valid bean class.
     * @return a list of new bean instances with properties populated from the corresponding map entries.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class.
     * @see #mapToBean(Map, Class)
     * @see #mapsToBeans(Collection, Collection, Class)
     */
    public static <T> List<T> mapsToBeans(final Collection<? extends Map<String, Object>> mapList, final Class<? extends T> targetType) {
        return mapsToBeans(mapList, true, targetType);
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type, with control over unmatched properties.
     * Each map in the collection represents a bean object where the map's keys are the property names
     * and the values are the corresponding property values.
     * The resulting list contains bean objects of the specified type with their properties set to the values from the corresponding map.
     * Map entries with {@code null} values are set on the corresponding bean properties (a {@code null} mapped to a primitive
     * property becomes that type's default value).
     *
     * <p>Each map is converted via {@link #mapToBean(Map, boolean, Class)}, so nested-bean properties may be supplied
     * as nested {@code Map} values or as flat, dot-separated keys (e.g. {@code "address.city"}); see
     * {@link #mapToBean(Map, Class)} for details and examples.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     * Map<String, Object> user1 = new HashMap<>();
     * user1.put("name", "John");
     * user1.put("unknownField", "value");
     * userMaps.add(user1);
     *
     * // Ignore unmatched properties
     * List<User> users = Beans.mapsToBeans(userMaps, true, User.class);
     * // users.get(0).getName() returns "John"; unknownField ignored
     * }</pre>
     *
     * @param <T> the type of the bean objects to be returned.
     * @param mapList the collection of maps to convert; if {@code null} or empty, an empty list is returned.
     * @param ignoreUnmatchedProperty if {@code true}, map keys without a matching bean property are silently ignored;
     *        if {@code false}, an {@link IllegalArgumentException} is thrown for unmatched keys.
     * @param targetType the class of the bean to create for each map; must be a valid bean class.
     * @return a list of new bean instances with properties populated from the corresponding map entries.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class, or if
     *         {@code ignoreUnmatchedProperty} is {@code false} and a map contains an unmatched key.
     * @see #mapToBean(Map, Class)
     */
    public static <T> List<T> mapsToBeans(final Collection<? extends Map<String, Object>> mapList, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetType) {
        N.checkBeanClass(targetType);

        final int size = N.size(mapList);
        final List<T> beanList = new ArrayList<>(size);

        if (size == 0) {
            return beanList;
        }

        for (final Map<String, Object> map : mapList) {
            beanList.add(mapToBean(map, ignoreUnmatchedProperty, targetType));
        }

        return beanList;
    }

    /**
     * Converts a collection of maps into a list of bean objects of the specified type, including only selected properties.
     * This method takes a collection of maps where each map represents a bean object.
     * The keys in the map are the property names and the values are the corresponding property values.
     * Only the properties specified in selectPropNames will be set on the beans.
     *
     * <p>Each map is converted via {@link #mapToBean(Map, Collection, Class)}; among the selected properties, a
     * nested-bean property may be supplied as a nested {@code Map} value or as flat, dot-separated keys (e.g.
     * {@code "address.city"}); see {@link #mapToBean(Map, Class)} for details and examples.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Map<String, Object>> userMaps = new ArrayList<>();
     * userMaps.add(Map.of("name", "John", "age", 25));
     * userMaps.add(Map.of("name", "Jane", "age", 30));
     *
     * // Only include the "name" property
     * List<User> users = Beans.mapsToBeans(userMaps, Arrays.asList("name"), User.class);
     * // users.get(0).getName() returns "John"; age stays at default 0
     * }</pre>
     *
     * @param <T> the type of the bean objects to be returned.
     * @param mapList the collection of maps to convert; if {@code null} or empty, an empty list is returned.
     * @param selectPropNames the property names to populate on each bean from the corresponding map.
     *        If {@code null}, all properties are considered. If empty, no properties are set.
     * @param targetType the class of the bean to create for each map; must be a valid bean class.
     * @return a list of new bean instances with the selected properties populated from the corresponding maps.
     * @throws IllegalArgumentException if {@code targetType} is not a valid bean class,
     *         or if a selected property does not exist in the target bean class.
     * @see #mapToBean(Map, Class)
     */
    public static <T> List<T> mapsToBeans(final Collection<? extends Map<String, Object>> mapList, final Collection<String> selectPropNames,
            final Class<? extends T> targetType) {
        N.checkBeanClass(targetType);

        final int size = N.size(mapList);
        final List<T> beanList = new ArrayList<>(size);

        if (size == 0) {
            return beanList;
        }

        for (final Map<String, Object> map : mapList) {
            beanList.add(mapToBean(map, selectPropNames, targetType));
        }

        return beanList;
    }

    /**
     * Converts a bean object into a map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The resulting map is a LinkedHashMap to preserve the order of properties.
     * By default, properties with {@code null} values are omitted; use
     * {@link #beanToMap(Object, boolean)} with {@code ignoreNullProperty=false} to include them.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * Beans.beanToMap(user);   // returns {name=John, age=25, active=true}
     *
     * user.setActive(null);
     * Beans.beanToMap(user);   // returns {name=John, age=25} (null "active" omitted)
     *
     * Beans.beanToMap((Object) null);   // returns {} (empty map)
     * }</pre>
     *
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @return a {@link java.util.LinkedHashMap} where the keys are property names and the values are
     *         the corresponding non-{@code null} property values of the bean; never {@code null}.
     */
    public static Map<String, Object> beanToMap(final Object bean) {
        return beanToMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map using the provided map supplier.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * The map supplier function determines the type of the map to be returned.
     * By default, properties with {@code null} values are omitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * // Using TreeMap to get keys sorted
     * // sortedMap: {age=25, name=John} (sorted by key; null "active" omitted)
     * TreeMap<String, Object> sortedMap = Beans.beanToMap(user, size -> new TreeMap<>());
     *
     * Beans.beanToMap((Object) null, HashMap::new);   // returns {} (empty map)
     * }</pre>
     *
     * @param <M> the type of the resulting Map.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type containing the non-{@code null} property name-value pairs of the bean;
     *         never {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return beanToMap(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     * The resulting map is a LinkedHashMap to preserve the order of properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * // Only include "name" and "age"
     * Beans.beanToMap(user, Arrays.asList("name", "age"));   // returns {name=John, age=25}
     *
     * Beans.beanToMap(user, (Collection<String>) null);     // returns all non-null props
     * }</pre>
     *
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included in the map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. Selected properties are included even when their values are {@code null}.
     * @return a {@link java.util.LinkedHashMap} with the selected (or all non-{@code null}) property name-value pairs;
     *         never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist in the bean class.
     */
    public static Map<String, Object> beanToMap(final Object bean, final Collection<String> selectPropNames) {
        return beanToMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified in the provided collection.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the <i>selectPropNames</i> collection are added to the map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     * The map supplier function determines the type of the map to be returned.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * // Only include "name" and "age", using a TreeMap
     * TreeMap<String, Object> sortedMap = Beans.beanToMap(user, Arrays.asList("name", "age"), size -> new TreeMap<>());
     * // sortedMap: {age=25, name=John} (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the resulting Map.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included in the map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. Selected properties are included even when their values are {@code null}.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type with the selected (or all non-{@code null}) property name-value pairs;
     *         never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist in the bean class.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return beanToMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     * The keys are named according to the provided naming policy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // class User { String firstName; String lastName; ... }
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * Collection<String> props = Arrays.asList("firstName", "lastName");
     *
     * Beans.beanToMap(user, props, NamingPolicy.SNAKE_CASE, HashMap::new);
     * // returns {first_name=John, last_name=Doe}
     *
     * Beans.beanToMap(user, props, NamingPolicy.SCREAMING_SNAKE_CASE, HashMap::new);
     * // returns {FIRST_NAME=John, LAST_NAME=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param selectPropNames the property names to include. If {@code null}, all non-{@code null}
     *        properties are included. If empty, no properties are included. Selected properties are
     *        included even when their values are {@code null}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to {@link NamingPolicy#CAMEL_CASE}.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type with property name-value pairs; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist in the bean class.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final M output = mapSupplier.apply(selectPropNames == null ? getPropNameList(bean.getClass()).size() : selectPropNames.size());

        beanToMap(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into the provided output map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * By default, only non-{@code null} properties are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     *
     * // existingMap now also contains name=John, age=25, active=true (id=123 preserved)
     * Beans.beanToMap(user, existingMap);
     *
     * Beans.beanToMap(null, existingMap);   // existingMap is unchanged
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param output the map into which the bean's non-{@code null} properties will be put.
     */
    public static <M extends Map<String, Object>> void beanToMap(final Object bean, final M output) {
        beanToMap(bean, null, output);
    }

    /**
     * Converts a bean object into the provided output map, selecting only specified properties.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * Only the properties whose names are included in the selectPropNames collection are added to the map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     *
     * Beans.beanToMap(user, Arrays.asList("name"), existingMap);
     * // existingMap now also contains name=John (id=123 preserved; age/active not included)
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included in the map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. Selected properties are included even when their values are {@code null}.
     * @param output the map into which the bean's properties will be put.
     * @throws IllegalArgumentException if a selected property does not exist in the bean class.
     */
    public static <M extends Map<String, Object>> void beanToMap(final Object bean, final Collection<String> selectPropNames, final M output) {
        beanToMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map, selecting only the properties specified.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     * The keys are named according to the provided naming policy.
     * The output map is provided as a parameter and will be filled with the bean's properties.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // class User { String firstName; String lastName; ... }
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     *
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Beans.beanToMap(user, Arrays.asList("firstName", "lastName"), NamingPolicy.SNAKE_CASE, outputMap);
     * // outputMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param selectPropNames the property names to include. If {@code null}, all non-{@code null}
     *        properties are included. If empty, no properties are included. Selected properties are
     *        included even when their values are {@code null}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to {@link NamingPolicy#CAMEL_CASE}.
     * @param output the map into which the bean's properties will be put.
     * @throws IllegalArgumentException if a selected property does not exist in the bean class.
     */
    public static <M extends Map<String, Object>> void beanToMap(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.CAMEL_CASE : keyNamingPolicy;
        final boolean isCamelCaseOrNoChange = NamingPolicy.CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (selectPropNames == null) {
            beanToMap(bean, true, null, keyNamingPolicy, output);
        } else {
            ParserUtil.PropInfo propInfo = null;
            Object propValue = null;

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass); //NOSONAR
                }

                propValue = propInfo.getPropValue(bean);

                if (isCamelCaseOrNoChange) {
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
     * User user = new User("John", 25);
     *
     * // Include null properties
     * Beans.beanToMap(user, false);   // returns {name=John, age=25, active=null}
     *
     * // Ignore null properties
     * Beans.beanToMap(user, true);    // returns {name=John, age=25}
     * }</pre>
     *
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are excluded from the map.
     * @return a {@link java.util.LinkedHashMap} with the bean's property name-value pairs; never {@code null}.
     */
    public static Map<String, Object> beanToMap(final Object bean, final boolean ignoreNullProperty) {
        return beanToMap(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a map with optional {@code null} property filtering and property exclusion.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("age"));
     *
     * // Ignore null properties and the "age" property
     * Beans.beanToMap(user, true, ignoredProps);
     * // returns {name=John} (active is null, age is ignored)
     * }</pre>
     *
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are excluded from the map.
     * @param ignoredPropNames a set of property names to exclude from the map; ignored if {@code null}.
     * @return a {@link java.util.LinkedHashMap} with the bean's property name-value pairs; never {@code null}.
     */
    public static Map<String, Object> beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return beanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE);
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
     * TreeMap<String, Object> sortedMap = Beans.beanToMap(user, true, ignoredProps, size -> new TreeMap<>());
     * // sortedMap: {email=john@example.com, name=John} (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are excluded from the map.
     * @param ignoredPropNames a set of property names to exclude from the map; ignored if {@code null}.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type with the bean's property name-value pairs; never {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return beanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
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
     * Map<String, Object> snakeMap = Beans.beanToMap(user, true, ignoredProps,
     *     NamingPolicy.SNAKE_CASE);
     * // snakeMap: {first_name=John, last_name=Doe}
     * // age is not included because it's null
     * }</pre>
     *
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are excluded from the map.
     * @param ignoredPropNames a set of property names to exclude from the map; ignored if {@code null}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to {@link NamingPolicy#CAMEL_CASE}.
     * @return a {@link java.util.LinkedHashMap} with the bean's property name-value pairs; never {@code null}.
     */
    public static Map<String, Object> beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return beanToMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
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
     * TreeMap<String, Object> customMap = Beans.beanToMap(user, true, ignoredProps,
     *     NamingPolicy.SNAKE_CASE, size -> new TreeMap<>());
     * // customMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are excluded from the map.
     * @param ignoredPropNames a set of property names to exclude from the map; ignored if {@code null}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to {@link NamingPolicy#CAMEL_CASE}.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type with the bean's property name-value pairs; never {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = getPropNameList(bean.getClass()).size();
        final int initCapacity = N.max(0, beanPropNameSize - N.size(ignoredPropNames));

        final M output = mapSupplier.apply(initCapacity);

        beanToMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

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
     * User user = new User("John", 25);
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("id", 123);
     *
     * // Add properties to existing map, ignoring nulls
     * Beans.beanToMap(user, true, existingMap); // existingMap contains name=John, age=25 (active omitted; id=123 preserved)
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are not added to the output map.
     * @param output the map into which the bean's properties will be put.
     */
    public static <M extends Map<String, Object>> void beanToMap(final Object bean, final boolean ignoreNullProperty, final M output) {
        beanToMap(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map.
     * The keys of the map are the property names of the bean, and the values are the corresponding property values of the bean.
     * If <i>ignoreNullProperty</i> is {@code true}, properties of the bean with {@code null} values will not be included in the map.
     * Properties whose names are included in the <i>ignoredPropNames</i> set will not be added to the map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * Map<String, Object> existingMap = new HashMap<>();
     * Set<String> ignoredProps = new HashSet<>(Arrays.asList("active"));
     *
     * Beans.beanToMap(user, true, ignoredProps, existingMap);
     * // existingMap: {name=John, age=25} (active is ignored)
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are not added to the output map.
     * @param ignoredPropNames a set of property names to exclude from the output map; ignored if {@code null}.
     * @param output the map into which the bean's properties will be put.
     */
    public static <M extends Map<String, Object>> void beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        beanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, output);
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
     * Beans.beanToMap(user, true, ignoredProps,
     *     NamingPolicy.SNAKE_CASE, outputMap);
     * // outputMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be filled.
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are not added to the output map.
     * @param ignoredPropNames a set of property names to exclude from the output map; ignored if {@code null}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to {@link NamingPolicy#CAMEL_CASE}.
     * @param output the map into which the bean's properties will be put.
     */
    public static <M extends Map<String, Object>> void beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            NamingPolicy keyNamingPolicy, final M output) {
        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.CAMEL_CASE : keyNamingPolicy;
        final boolean isCamelCaseOrNoChange = NamingPolicy.CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
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

            if (isCamelCaseOrNoChange) {
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
     * By default, properties with {@code null} values are omitted.
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
     * Map<String, Object> deepMap = Beans.deepBeanToMap(user);
     * // deepMap: {
     * //   name=John,
     * //   address={city=New York, zipCode=10001}
     * // }
     * // Note: address is converted to a Map, not kept as Address object
     * }</pre>
     *
     * @param bean the bean to be converted into a Map; if {@code null}, an empty map is returned.
     * @return a {@link java.util.LinkedHashMap} representation of the provided bean; never {@code null}.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean) {
        return deepBeanToMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The map type is determined by the provided mapSupplier.
     * By default, properties with {@code null} values are omitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with custom map type
     * User user = new User();
     * user.setName("John");
     * Address address = new Address();
     * address.setCity("New York");
     * user.setAddress(address);
     * // Using TreeMap for sorted keys
     * TreeMap<String, Object> sortedDeepMap = Beans.deepBeanToMap(user, size -> new TreeMap<>());
     * // sortedDeepMap: {
     * //   address={city=New York},
     * //   name=John
     * // } (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the Map to which the bean will be converted.
     * @param bean the bean to be converted into a Map; if {@code null}, an empty map is returned.
     * @param mapSupplier a supplier function to create the Map instance.
     * @return a Map of the specified type representing the provided bean; never {@code null}.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return deepBeanToMap(bean, null, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included. If {@code selectPropNames} is
     * {@code null}, all non-{@code null} properties are included. If it is empty, no properties
     * are included.
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
     * Map<String, Object> selectedDeepMap = Beans.deepBeanToMap(user, props);
     * // selectedDeepMap: {
     * //   name=John,
     * //   address={city=New York}
     * // }
     * // age is not included
     * }</pre>
     *
     * @param bean the bean to be converted into a Map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included even when
     *        {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @return a {@link java.util.LinkedHashMap} representation of the provided bean; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final Collection<String> selectPropNames) {
        return deepBeanToMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included, and the map type is determined by mapSupplier.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
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
     * HashMap<String, Object> customDeepMap = Beans.deepBeanToMap(user, props, HashMap::new);
     * // customDeepMap: {
     * //   name=John,
     * //   address={city=New York, zipCode=10001}
     * // }
     * }</pre>
     *
     * @param <M> the type of the Map to which the bean will be converted.
     * @param bean the bean to be converted into a Map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included even when
     *        {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param mapSupplier a supplier function to create the Map instance.
     * @return a Map of the specified type representing the provided bean; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBeanToMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
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
     * Map<String, Object> snakeMap = Beans.deepBeanToMap(user, props,
     *     NamingPolicy.SNAKE_CASE, HashMap::new);
     * // snakeMap: {
     * //   first_name=John,
     * //   home_address={street_name=Main St}
     * // }
     * // Note: nested properties are also converted
     * }</pre>
     *
     * @param <M> the type of the Map to which the bean will be converted.
     * @param bean the bean to be converted into a Map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included even when
     *        {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param keyNamingPolicy the naming policy to be used for the keys in the resulting Map.
     * @param mapSupplier a supplier function to create the Map instance into which the bean properties will be put.
     * @return a Map of the specified type representing the provided bean; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final M output = mapSupplier.apply(selectPropNames == null ? getPropNameList(bean.getClass()).size() : selectPropNames.size());

        deepBeanToMap(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * By default, only non-{@code null} properties are included and stored in the provided output map.
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
     * Beans.deepBeanToMap(user, existingMap);
     * // existingMap: {
     * //   id=123,
     * //   name=John,
     * //   address={city=New York}
     * // }
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean to be converted into a Map; if {@code null}, the output map is not modified.
     * @param output the map into which the bean's properties will be put.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBeanToMap(final Object bean, final M output) {
        deepBeanToMap(bean, null, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included and stored in the provided output map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
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
     * Beans.deepBeanToMap(user, props, outputMap);
     * // outputMap: {
     * //   name=John,
     * //   address={city=New York}
     * // }
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean to be converted into a Map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included even when
     *        {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param output the map into which the bean's properties will be put.
     * @throws IllegalArgumentException if a selected property does not exist
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBeanToMap(final Object bean, final Collection<String> selectPropNames, final M output) {
        deepBeanToMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Only properties specified in selectPropNames are included, keys are transformed according to the naming policy, and results are stored in the output map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
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
     * Beans.deepBeanToMap(user, props,
     *     NamingPolicy.SNAKE_CASE, outputMap);
     * // outputMap: {
     * //   first_name=John,
     * //   home_address={street_name=Main St}
     * // }
     * }</pre>
     *
     * @param <M> the type of the output map.
     * @param bean the bean to be converted into a Map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included even when
     *        {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param keyNamingPolicy the naming policy to be used for the keys in the resulting Map.
     * @param output the map into which the bean's properties will be put.
     * @throws IllegalArgumentException if a selected property does not exist
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBeanToMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        if (bean == null) {
            return;
        }

        final boolean isCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.CAMEL_CASE == keyNamingPolicy
                || NamingPolicy.NO_CHANGE == keyNamingPolicy;

        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (selectPropNames == null) {
            deepBeanToMap(bean, true, null, keyNamingPolicy, output);
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
                    if (isCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isCamelCaseOrNoChange) {
                        output.put(propName, deepBeanToMap(propValue, true, null, keyNamingPolicy));
                    } else {
                        output.put(keyNamingPolicy.convert(propName), deepBeanToMap(propValue, true, null, keyNamingPolicy));
                    }
                }
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Whether {@code null} values are included is controlled by {@code ignoreNullProperty}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a User bean with nested Address
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * // result: {name=John, age=25, address={city=NYC, zipCode=10001}}
     * Map<String, Object> result = deepBeanToMap(user, false);
     *
     * // With ignoreNullProperty=true
     * User userWithNull = new User("Jane", null, null);
     * Map<String, Object> filtered = deepBeanToMap(userWithNull, true);
     * // filtered: {name=Jane} (null properties excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @return a {@link java.util.LinkedHashMap} representation of the bean where nested beans are recursively converted to Maps; never {@code null}.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final boolean ignoreNullProperty) {
        return deepBeanToMap(bean, ignoreNullProperty, (Set<String>) null);
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
     * Map<String, Object> result = deepBeanToMap(user, false, ignored);
     * // result: {name=John, address={city=NYC}} (email and age excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process. Can be {@code null}.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @return a {@link java.util.LinkedHashMap} representation of the bean with specified properties excluded; never {@code null}.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE);
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
     * TreeMap<String, Object> result = deepBeanToMap(user, false, null,
     *     size -> new TreeMap<>());
     * // result: TreeMap with {address={city=NYC}, age=25, name=John} (sorted keys)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a Map of the specified type containing the bean properties; never {@code null}.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
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
     * Map<String, Object> snakeCase = deepBeanToMap(user, false, null,
     *     NamingPolicy.SNAKE_CASE);
     * // snakeCase: {first_name=John, last_name=Doe}
     *
     * Map<String, Object> upperCase = deepBeanToMap(user, false, null,
     *     NamingPolicy.SCREAMING_SNAKE_CASE);
     * // upperCase: {FIRST_NAME=John, LAST_NAME=Doe}
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting Map. If {@code null}, defaults to {@link NamingPolicy#CAMEL_CASE}.
     * @return a {@link java.util.LinkedHashMap} representation of the bean with keys transformed according to the naming policy; never {@code null}.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * Provides full control over the conversion process including naming policy and Map type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom conversion with all options
     * User user = new User("John", (Integer) null, new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalId"));
     *
     * LinkedHashMap<String, Object> result = deepBeanToMap(user, true, ignored,
     *     NamingPolicy.SCREAMING_SNAKE_CASE,
     *     size -> new LinkedHashMap<>(size));
     * // result: {NAME=John, ADDRESS={CITY=NYC}} (ordered, uppercase with underscores)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting Map. If {@code null}, defaults to {@link NamingPolicy#CAMEL_CASE}.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a Map of the specified type with full customization applied; never {@code null}.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = getPropNameList(bean.getClass()).size();
        final int initCapacity = N.max(0, beanPropNameSize - N.size(ignoredPropNames));

        final M output = mapSupplier.apply(initCapacity);

        deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

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
     * deepBeanToMap(user, false, existingMap); // existingMap contains {timestamp=..., name=John, age=25}
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a Map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param output the Map instance into which the bean properties will be put. Existing entries are preserved.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final M output) {
        deepBeanToMap(bean, ignoreNullProperty, null, output);
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
     * deepBeanToMap(user, false, ignored, output);
     * // output: {name=John} (sensitive fields excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a Map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param output the Map instance into which the bean properties will be put.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method performs a deep conversion, meaning that if a property value is itself a bean, it will also be converted into a Map.
     * The conversion process can be customized by specifying properties to ignore, whether to ignore {@code null} properties, and the naming policy for keys.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full control over in-place conversion
     * Map<String, Object> output = new TreeMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("id"));
     *
     * Product product = new Product("Widget", 29.99, new Category("Electronics"));
     * deepBeanToMap(product, true, ignored, NamingPolicy.SCREAMING_SNAKE_CASE, output);
     * // output: {CATEGORY={NAME=Electronics}, NAME=Widget, PRICE=29.99} (sorted)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a Map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param keyNamingPolicy the naming policy to apply to the keys in the output Map.
     * @param output the Map instance into which the bean properties will be put.
     */
    public static <M extends Map<String, Object>> void deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        // Cycle guard: if `bean` is already being converted higher up the call stack (e.g.
        // bidirectional `parent <-> child` references), bail out instead of infinite-recursing
        // into a StackOverflowError.
        if ((bean == null) || !enterDeepBean(bean)) {
            return;
        }
        try {
            final boolean isCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.CAMEL_CASE == keyNamingPolicy
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
                    if (isCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isCamelCaseOrNoChange) {
                        output.put(propName, deepBeanToMap(propValue, ignoreNullProperty, null, keyNamingPolicy));
                    } else {
                        output.put(keyNamingPolicy.convert(propName), deepBeanToMap(propValue, ignoreNullProperty, null, keyNamingPolicy));
                    }
                }
            }
        } finally {
            exitDeepBean(bean);
        }
    }

    /**
     * Per-thread visited-bean set (identity-keyed) for {@link #deepBeanToMap} and
     * {@link #beanToFlatMap}. Used to detect reference cycles in the bean graph.
     */
    private static final ThreadLocal<java.util.IdentityHashMap<Object, Boolean>> DEEP_BEAN_VISITED = ThreadLocal.withInitial(java.util.IdentityHashMap::new);

    /** @return true if the bean was added (no cycle); false if it was already in progress. */
    private static boolean enterDeepBean(final Object bean) {
        return DEEP_BEAN_VISITED.get().putIfAbsent(bean, Boolean.TRUE) == null;
    }

    private static void exitDeepBean(final Object bean) {
        final java.util.IdentityHashMap<Object, Boolean> visited = DEEP_BEAN_VISITED.get();
        visited.remove(bean);
        if (visited.isEmpty()) {
            DEEP_BEAN_VISITED.remove();
        }
    }

    /**
     * Converts a bean object into a flat map representation where nested properties are represented with dot notation.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * By default, properties with {@code null} values are omitted from the result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given nested beans
     * User user = new User("John", new Address("NYC", "10001"));
     * // flat: {name=John, address.city=NYC, address.zipCode=10001}
     * Map<String, Object> flat = beanToFlatMap(user);
     *
     * // Deep nesting
     * Company company = new Company("TechCorp",
     *     new Address("NYC", new Location(40.7128, -74.0060)));
     * Map<String, Object> result = beanToFlatMap(company);
     * // result: {name=TechCorp, address.city=NYC,
     * //          address.location.latitude=40.7128,
     * //          address.location.longitude=-74.0060}
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @return a map representing the bean object with nested properties flattened using dot notation; never {@code null}.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean) {
        return beanToFlatMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation where nested properties are represented with dot notation.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * The type of Map returned can be customized using the mapSupplier. By default, properties with {@code null} values are omitted.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a sorted flat map
     * User user = new User("John", new Address("NYC", "10001"));
     * TreeMap<String, Object> sortedFlat = beanToFlatMap(user,
     *     size -> new TreeMap<>());
     * // sortedFlat: {address.city=NYC, address.zipCode=10001, name=John} (sorted)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with nested properties flattened; never {@code null}.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final IntFunction<? extends M> mapSupplier) {
        return beanToFlatMap(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included in the result. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select specific properties including nested ones
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * Collection<String> select = Arrays.asList("name", "address");
     * // result: {name=John, address.city=NYC, address.zipCode=10001}
     * Map<String, Object> result = beanToFlatMap(user, select);
     *
     * // Select only top-level properties
     * Collection<String> topLevel = Arrays.asList("name", "age");
     * Map<String, Object> flat = beanToFlatMap(user, topLevel);
     * // flat: {name=John, age=25} (address excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included in the resulting map. Nested properties of selected beans are automatically included.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @return a map with only the selected properties flattened; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final Collection<String> selectPropNames) {
        return beanToFlatMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties and custom Map type.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines property selection with Map type customization. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select properties and use custom map type
     * Employee emp = new Employee("John", "IT", new Manager("Jane"));
     * Collection<String> select = Arrays.asList("name", "manager");
     *
     * LinkedHashMap<String, Object> result = beanToFlatMap(emp, select,
     *     size -> new LinkedHashMap<>(size));
     * // result: {name=John, manager.name=Jane} (ordered, dept excluded)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included in the resulting map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with selected properties flattened; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return beanToFlatMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and a specified naming policy.
     * This method takes a bean object and transforms it into a map where the keys are the property names of the bean and the values are the corresponding property values.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With naming policy transformation
     * User user = new User();
     * user.setFirstName("John");
     * user.setHomeAddress(new Address("NYC"));
     *
     * Collection<String> select = Arrays.asList("firstName", "homeAddress");
     * Map<String, Object> snakeCase = beanToFlatMap(user, select,
     *     NamingPolicy.SNAKE_CASE,
     *     size -> new HashMap<>(size));
     * // snakeCase: {first_name=John, home_address.city=NYC}
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included in the resulting map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param keyNamingPolicy the naming policy for the keys in the resulting map.
     * @param mapSupplier a function that generates a new map instance. The function argument is the initial map capacity.
     * @return a map of the specified type with the bean's (selected) properties flattened using dot notation for nested beans; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final M output = mapSupplier.apply(selectPropNames == null ? getPropNameList(bean.getClass()).size() : selectPropNames.size());

        beanToFlatMap(bean, selectPropNames, keyNamingPolicy, output);

        return output;
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * By default, only non-{@code null} properties from the bean are included in the output.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Populate existing map with flattened bean
     * Map<String, Object> output = new HashMap<>();
     * output.put("version", "1.0");
     *
     * User user = new User("John", new Address("NYC"));
     * beanToFlatMap(user, output);
     * // output: {version=1.0, name=John, address.city=NYC}
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param output the Map instance into which the flattened bean properties will be put. Existing entries are preserved.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void beanToFlatMap(final Object bean, final M output) {
        beanToFlatMap(bean, null, output);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Selective flattening into existing map
     * Map<String, Object> output = new LinkedHashMap<>();
     * Collection<String> select = Arrays.asList("name", "contact");
     *
     * Customer customer = new Customer("John", "123-456",
     *     new Contact("john@email.com", "555-1234"));
     * beanToFlatMap(customer, select, output);
     * // output: {name=John, contact.email=john@email.com, contact.phone=555-1234}
     * // (customerId excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included in the output map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param output the Map instance into which the flattened bean properties will be put.
     * @throws IllegalArgumentException if a selected property does not exist
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void beanToFlatMap(final Object bean, final Collection<String> selectPropNames, final M output) {
        beanToFlatMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation with full customization options and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides control over property selection and key naming policy. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full customization of flattening process
     * Map<String, Object> output = new TreeMap<>();
     * Collection<String> select = Arrays.asList("productName", "category");
     *
     * Product product = new Product("WidgetPro",
     *     new Category("Electronics", "Gadgets"));
     * beanToFlatMap(product, select, NamingPolicy.SCREAMING_SNAKE_CASE, output);
     * // output: {CATEGORY.NAME=Electronics, CATEGORY.SUBCATEGORY=Gadgets,
     * //          PRODUCT_NAME=WidgetPro} (sorted, uppercase)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included in the output map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     * @param keyNamingPolicy the naming policy to apply to the keys in the output map.
     * @param output the Map instance into which the flattened bean properties will be put.
     * @throws IllegalArgumentException if a selected property does not exist
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void beanToFlatMap(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy,
            final M output) {
        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.CAMEL_CASE : keyNamingPolicy;
        final boolean isCamelCaseOrNoChange = NamingPolicy.CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (selectPropNames == null) {
            beanToFlatMap(bean, true, null, keyNamingPolicy, output);
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
                    if (isCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    beanToFlatMap(propValue, true, null, keyNamingPolicy, isCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), output);
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
     * User user = new User("John", (Integer) null, new Address("NYC", (String) null));
     * // withNulls: {name=John, age=null, address.city=NYC, address.zipCode=null}
     * Map<String, Object> withNulls = beanToFlatMap(user, false);
     *
     * // Exclude null properties
     * Map<String, Object> noNulls = beanToFlatMap(user, true);
     * // noNulls: {name=John, address.city=NYC}
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @return a flat map representation of the bean with {@code null} handling as specified; never {@code null}.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final boolean ignoreNullProperty) {
        return beanToFlatMap(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} property handling and property exclusion.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines {@code null} value filtering with property name exclusion.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter nulls and exclude specific properties
     * User user = new User("John", (Integer) null, "secret123", new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("password"));
     *
     * Map<String, Object> result = beanToFlatMap(user, true, ignored);
     * // result: {name=John, address.city=NYC}
     * // (age is null, password is in ignored set)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @return a flat map with the specified filtering applied; never {@code null}.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) {
        return beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE);
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
     * TreeMap<String, Object> result = beanToFlatMap(emp, true, ignored,
     *     size -> new TreeMap<>());
     * // result: {name=John, office.building=Building A}
     * // (sorted, salary null excluded, department ignored)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with filtering applied; never {@code null}.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) {
        return beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
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
     * Map<String, Object> result = beanToFlatMap(profile, true, ignored,
     *     NamingPolicy.SNAKE_CASE);
     * // result: {first_name=John, home_address.city=NYC}
     * // (last_login null excluded, internal_id ignored, snake_case keys)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting map.
     * @return a flat map with comprehensive customization applied; never {@code null}.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
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
     * LinkedHashMap<String, Object> result = beanToFlatMap(order, true, ignored,
     *     NamingPolicy.SCREAMING_SNAKE_CASE,
     *     size -> new LinkedHashMap<>(size * 2));
     * // result: {ORDER_ID=ORD-123, CUSTOMER.NAME=John,
     * //          CUSTOMER.ADDRESS.CITY=NYC, CUSTOMER.ADDRESS.ZIP_CODE=10001}
     * // (amount null excluded, internal_notes ignored, ordered map)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @param ignoredPropNames a set of property names to be excluded from the resulting map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @param keyNamingPolicy the naming policy to apply to the keys in the resulting map.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a fully customized flat map representation of the bean; never {@code null}.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) {
        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = getPropNameList(bean.getClass()).size();
        final int initCapacity = N.max(0, beanPropNameSize - N.size(ignoredPropNames));

        final M output = mapSupplier.apply(initCapacity);

        beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, output);

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
     * User user = new User("John", (Integer) null, new Address("NYC"));
     * beanToFlatMap(user, true, output);
     * // output: {timestamp=..., name=John, address.city=NYC}
     * // (age null is excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param output the map into which the flattened bean properties will be put.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final M output) {
        beanToFlatMap(bean, ignoreNullProperty, null, output);
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
     * beanToFlatMap(account, true, ignored, output);
     * // output: {username=john123}
     * // (password and ssn ignored, balance null excluded)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames a set of property names to be excluded from the output map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @param output the map into which the flattened bean properties will be put.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final M output) {
        beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with full customization.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This method provides complete control over the in-place flattening operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full control in-place flattening
     * Map<String, Object> output = new TreeMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("metadata"));
     *
     * Document doc = new Document("Report", null,
     *     new Author("John", new Department("Research")));
     * beanToFlatMap(doc, true, ignored,
     *     NamingPolicy.SNAKE_CASE, output);
     * // output: {author.department.name=Research, author.name=John, title=Report}
     * // (sorted keys, snake_case, version null excluded, metadata ignored)
     * }</pre>
     *
     * @param <M> the type of Map to populate.
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames a set of property names to be excluded from the output map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @param keyNamingPolicy the naming policy to apply to the keys in the output map.
     * @param output the map into which the flattened bean properties will be put.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> void beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final M output) {
        beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, null, output);
    }

    private static <M extends Map<String, Object>> void beanToFlatMap(final Object bean, final boolean ignoreNullProperty,
            final Collection<String> ignoredPropNames, final NamingPolicy keyNamingPolicy, final String parentPropName, final M output) {
        // Cycle guard — see deepBeanToMap.
        if ((bean == null) || !enterDeepBean(bean)) {
            return;
        }
        try {
            beanToFlatMapBody(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, parentPropName, output);
        } finally {
            exitDeepBean(bean);
        }
    }

    private static <M extends Map<String, Object>> void beanToFlatMapBody(final Object bean, final boolean ignoreNullProperty,
            final Collection<String> ignoredPropNames, final NamingPolicy keyNamingPolicy, final String parentPropName, final M output) {
        final boolean isCamelCaseOrNoChange = keyNamingPolicy == null || NamingPolicy.CAMEL_CASE == keyNamingPolicy
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
                    if (isCamelCaseOrNoChange) {
                        output.put(propName, propValue);
                    } else {
                        output.put(keyNamingPolicy.convert(propName), propValue);
                    }
                } else {
                    if (isCamelCaseOrNoChange) {
                        output.put(parentPropName + SK.PERIOD + propName, propValue);
                    } else {
                        output.put(parentPropName + SK.PERIOD + keyNamingPolicy.convert(propName), propValue);
                    }
                }
            } else {
                if (isNullParentPropName) {
                    beanToFlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy, isCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName),
                            output);
                } else {
                    beanToFlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy,
                            parentPropName + SK.PERIOD + (isCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName)), output);
                }
            }
        }
    }

    /**
     * Creates a fluent builder for converting the given bean into a {@link Map}, capping the large family of
     * {@code beanToMap} / {@code deepBeanToMap} / {@code beanToFlatMap} overloads with a single configurable
     * entry point.
     *
     * <p>All configuration methods are optional and may be chained in any order; a terminal method
     * ({@link BeanMapBuilder#toMap()}, {@link BeanMapBuilder#toMap(IntFunction)}, or {@link BeanMapBuilder#into(Map)})
     * produces the result. Property selection is composable: {@code select} restricts the candidate properties
     * (default: all), {@code exclude} removes properties, {@code filter} keeps only properties for which the
     * predicate returns {@code true}, and {@code skipNulls} drops {@code null}-valued properties. By default all
     * properties (including {@code null}-valued ones) are converted, using {@link NamingPolicy#CAMEL_CASE} keys and
     * a {@link java.util.LinkedHashMap}, in shallow mode.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> m = Beans.mapBuilder(user)
     *         .exclude("password")
     *         .skipNulls()
     *         .naming(NamingPolicy.SNAKE_CASE)
     *         .deep()
     *         .toMap();
     *
     * // Filter by name/value (no overload ambiguity: filter is a builder method, not an overload)
     * Map<String, Object> strings = Beans.mapBuilder(user)
     *         .filter((name, value) -> value instanceof String)
     *         .toMap();
     * }</pre>
     *
     * @param bean the bean to convert; if {@code null}, the terminal methods produce an empty map.
     * @return a new {@link BeanMapBuilder} for {@code bean}.
     * @see BeanMapBuilder
     */
    public static BeanMapBuilder mapBuilder(final Object bean) {
        return new BeanMapBuilder(bean);
    }

    /**
     * A fluent builder, created by {@link Beans#mapBuilder(Object)}, that converts a bean into a {@link Map} with
     * configurable property selection, {@code null} handling, key naming, conversion depth, and output map type.
     * Not thread-safe; intended to be configured and consumed in a single statement.
     *
     * @see Beans#mapBuilder(Object)
     */
    public static final class BeanMapBuilder {
        private enum Shape {
            SHALLOW, DEEP, FLAT
        }

        private final Object bean;
        private Collection<String> selectPropNames;
        private Set<String> excludePropNames;
        private BiPredicate<? super String, Object> propFilter;
        private boolean skipNulls;
        private NamingPolicy namingPolicy = NamingPolicy.CAMEL_CASE;
        private Shape shape = Shape.SHALLOW;

        BeanMapBuilder(final Object bean) {
            this.bean = bean;
        }

        /**
         * Restricts the conversion to the specified properties, in the given order. By default all properties
         * are converted. Calling this replaces any previously specified selection.
         *
         * @param propNames the property names to include.
         * @return this builder.
         */
        public BeanMapBuilder select(final String... propNames) {
            return select(java.util.Arrays.asList(propNames));
        }

        /**
         * Restricts the conversion to the specified properties. By default all properties are converted; passing
         * {@code null} clears the selection (all properties). Calling this replaces any previously specified selection.
         *
         * @param propNames the property names to include; {@code null} clears the selection.
         * @return this builder.
         */
        public BeanMapBuilder select(final Collection<String> propNames) {
            this.selectPropNames = propNames;
            return this;
        }

        /**
         * Excludes the specified properties from the conversion. Calling this replaces any previously specified exclusions.
         *
         * @param propNames the property names to exclude.
         * @return this builder.
         */
        public BeanMapBuilder exclude(final String... propNames) {
            return exclude(java.util.Arrays.asList(propNames));
        }

        /**
         * Excludes the specified properties from the conversion. Calling this replaces any previously specified exclusions.
         *
         * @param propNames the property names to exclude; {@code null} or empty clears the exclusions.
         * @return this builder.
         */
        public BeanMapBuilder exclude(final Collection<String> propNames) {
            this.excludePropNames = N.isEmpty(propNames) ? null : N.newHashSet(propNames);
            return this;
        }

        /**
         * Includes only the properties for which the given predicate returns {@code true}. The predicate receives
         * the property name and its value; it is applied to top-level properties only.
         *
         * @param propFilter the predicate receiving the property name and value.
         * @return this builder.
         */
        public BeanMapBuilder filter(final BiPredicate<? super String, Object> propFilter) {
            this.propFilter = propFilter;
            return this;
        }

        /**
         * Drops properties whose value is {@code null}. By default {@code null}-valued properties are kept.
         *
         * @return this builder.
         */
        public BeanMapBuilder skipNulls() {
            this.skipNulls = true;
            return this;
        }

        /**
         * Sets the naming policy applied to the map keys. Defaults to {@link NamingPolicy#CAMEL_CASE}.
         *
         * @param keyNamingPolicy the key naming policy; {@code null} resets to {@link NamingPolicy#CAMEL_CASE}.
         * @return this builder.
         */
        public BeanMapBuilder naming(final NamingPolicy keyNamingPolicy) {
            this.namingPolicy = keyNamingPolicy == null ? NamingPolicy.CAMEL_CASE : keyNamingPolicy;
            return this;
        }

        /**
         * Converts nested bean-valued properties recursively into nested maps. Mutually exclusive with
         * {@link #flat()}: calling both (in either order) throws {@link IllegalStateException}. Calling
         * {@code deep()} more than once is harmless.
         *
         * @return this builder.
         * @throws IllegalStateException if {@link #flat()} has already been called on this builder.
         */
        public BeanMapBuilder deep() {
            if (shape == Shape.FLAT) {
                throw new IllegalStateException("flat() was already called; deep() and flat() are mutually exclusive");
            }

            shape = Shape.DEEP;
            return this;
        }

        /**
         * Flattens nested bean-valued properties into the resulting map using dot-separated keys (e.g.
         * {@code "address.city"}). Mutually exclusive with {@link #deep()}: calling both (in either order)
         * throws {@link IllegalStateException}. Calling {@code flat()} more than once is harmless.
         *
         * @return this builder.
         * @throws IllegalStateException if {@link #deep()} has already been called on this builder.
         */
        public BeanMapBuilder flat() {
            if (shape == Shape.DEEP) {
                throw new IllegalStateException("deep() was already called; deep() and flat() are mutually exclusive");
            }

            shape = Shape.FLAT;
            return this;
        }

        /**
         * Performs the conversion, returning a {@link java.util.LinkedHashMap}.
         *
         * @return a new {@link java.util.LinkedHashMap} with the converted properties; never {@code null}.
         * @throws IllegalArgumentException if a {@code select}ed property is not found in the bean class.
         */
        public Map<String, Object> toMap() {
            return toMap(IntFunctions.ofLinkedHashMap());
        }

        /**
         * Performs the conversion into a map created by the given supplier.
         *
         * @param <M> the map type.
         * @param mapSupplier a function that creates a new map given an initial capacity.
         * @return the created map with the converted properties; never {@code null}.
         * @throws IllegalArgumentException if a {@code select}ed property is not found in the bean class.
         */
        public <M extends Map<String, Object>> M toMap(final IntFunction<? extends M> mapSupplier) {
            final Collection<String> effective = effectivePropNames();
            final M output = mapSupplier.apply(effective == null ? 0 : effective.size());
            fill(effective, output);
            return output;
        }

        /**
         * Performs the conversion into the provided map, which is returned. Existing entries are preserved unless
         * overwritten by a converted key.
         *
         * @param <M> the map type.
         * @param output the map to fill; must not be {@code null}.
         * @return {@code output}.
         * @throws IllegalArgumentException if {@code output} is {@code null}, or if a {@code select}ed property is
         *         not found in the bean class.
         */
        public <M extends Map<String, Object>> M into(final M output) {
            N.checkArgNotNull(output, cs.output);

            fill(effectivePropNames(), output);

            return output;
        }

        private Collection<String> effectivePropNames() {
            if (bean == null) {
                return null;
            }

            final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
            final boolean needValue = skipNulls || propFilter != null;
            final List<String> result = new ArrayList<>();

            if (selectPropNames == null) {
                for (final PropInfo propInfo : beanInfo.propInfoList) {
                    if (accept(propInfo, propInfo.name, needValue)) {
                        result.add(propInfo.name);
                    }
                }
            } else {
                for (final String propName : selectPropNames) {
                    final PropInfo propInfo = beanInfo.getPropInfo(propName);

                    if (propInfo == null) {
                        throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + bean.getClass());
                    }

                    if (accept(propInfo, propName, needValue)) {
                        result.add(propName);
                    }
                }
            }

            return result;
        }

        private boolean accept(final PropInfo propInfo, final String propName, final boolean needValue) {
            if (excludePropNames != null && excludePropNames.contains(propName)) {
                return false;
            }

            if (needValue) {
                final Object value = propInfo.getPropValue(bean);

                if (skipNulls && value == null) {
                    return false;
                }

                return propFilter == null || propFilter.test(propName, value);
            }

            return true;
        }

        private <M extends Map<String, Object>> void fill(final Collection<String> effective, final M output) {
            if (bean == null) {
                return;
            }

            switch (shape) {
                case DEEP:
                    deepBeanToMap(bean, effective, namingPolicy, output);
                    break;
                case FLAT:
                    beanToFlatMap(bean, effective, namingPolicy, output);
                    break;
                default:
                    beanToMap(bean, effective, namingPolicy, output);
            }
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
     * // user.getName() returns null; user.getAge() returns 0
     * User user = Beans.newBean(User.class);   // returns a new User instance
     *
     * }</pre>
     *
     * @param <T> the type of the object to be created.
     * @param targetType the class to instantiate; must have an accessible no-argument constructor.
     * @return a new instance of the specified class; never {@code null}.
     * @throws IllegalArgumentException if the class cannot be instantiated (e.g., abstract, no accessible no-arg constructor).
     */
    public static <T> T newBean(final Class<T> targetType) {
        return N.newInstance(targetType);
    }

    private static final Set<Class<?>> notKryoCompatible = N.newConcurrentHashSet();

    /**
     * Creates a deep clone of the given object using serialization.
     *
     * <p>This method performs a deep copy by serializing the object to Kryo or XML format
     * and then deserializing it back to create a new instance. This ensures that all nested
     * objects are also cloned, not just the top-level object.</p>
     *
     * <p>The object must be serializable through either Kryo or XML. If Kryo serialization
     * fails, the method automatically falls back to XML serialization.</p>
     *
     * <p>Note: this method returns {@code null} for a {@code null} source, whereas
     * {@link #deepCopyAs(Object, Class)} returns a new empty instance of the target type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User original = new User("John", 25);
     *
     * User clone = Beans.deepCopy(original);
     *
     * // clone != original but clone.getName().equals("John")
     * // original.getName() is still "John"
     * clone.setName("Jane");
     *
     * Beans.deepCopy(null);   // returns null
     * }</pre>
     *
     * @param <T> the type of the object to be cloned.
     * @param obj the object to clone; must be serializable via Kryo or Abacus XML.
     * @return a deep clone of the object, or {@code null} if {@code obj} is {@code null}.
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(final T obj) {
        if (obj == null) {
            return null; // NOSONAR
        }

        return (T) deepCopyAs(obj, obj.getClass());
    }

    /**
     * Creates a deep clone of the given object and converts it to the specified target type.
     *
     * <p>This method performs a deep copy by serializing the object and then deserializing it
     * as an instance of the target type. This is useful for creating type-converted copies
     * or for ensuring type safety when cloning objects.</p>
     *
     * <p>If the source object is {@code null}, the method creates a new instance of the
     * target type by calling {@link #copyAs(Object, Class)} for bean targets or
     * {@link N#newInstance(Class)} for non-bean targets (which requires an accessible no-arg
     * constructor).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     * User copy = Beans.deepCopyAs(user, User.class);   // returns a populated User copy
     *
     * // Null source produces a new empty (non-null) instance
     * User empty = Beans.deepCopyAs(null, User.class);  // returns a new empty User
     *
     * Beans.deepCopyAs(user, null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the target object.
     * @param obj the source object; if {@code null}, a new empty instance of {@code targetType} is returned.
     * @param targetType the class of the target type to create; must not be {@code null}.
     * @return a new instance of the target type populated from the source object; never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopyAs(final Object obj, @NotNull final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (obj == null) {
            if (isBeanClass(targetType)) {
                return copyAs(null, targetType);
            } else {
                return N.newInstance(targetType);
            }
        }

        final Class<?> srcCls = obj.getClass();
        Object copy = null;

        if (Utils.kryoParser != null && targetType.equals(srcCls) && !notKryoCompatible.contains(srcCls)) {
            try {
                copy = Utils.kryoParser.deepCopy(obj);
            } catch (final Exception e) {
                notKryoCompatible.add(srcCls);

                // ignore.
            }
        }

        if (copy == null) {
            final String xml = Utils.abacusXmlParser.serialize(obj, Utils.xscForClone);
            copy = Utils.abacusXmlParser.deserialize(xml, targetType);
        }

        return (T) copy;
    }

    /**
     * Creates a shallow copy of the given source bean.
     *
     * <p>This method creates a new instance of the same class as the source bean and copies
     * all property values from the source to the new instance. Unlike {@link #deepCopy(Object)},
     * this method performs a shallow copy - nested objects are not cloned but referenced.</p>
     *
     * <p>Note: this method returns {@code null} for a {@code null} source, whereas
     * {@link #copyAs(Object, Class)} returns a new empty instance of the target type.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User original = new User("John", 25);
     *
     * // copy != original; copy.getName() returns "John", copy.getAge() returns 25
     * User copy = Beans.copy(original);
     *
     * Beans.copy((User) null);   // returns null
     * }</pre>
     *
     * @param <T> the type of the source bean.
     * @param sourceBean the source bean to copy; may be {@code null}.
     * @return a new instance of the same type with all properties shallow-copied from {@code sourceBean},
     *         or {@code null} if {@code sourceBean} is {@code null}.
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T copy(final T sourceBean) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copyAs(sourceBean, (Class<T>) sourceBean.getClass());
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
     * User original = new User("John", 25);
     *
     * // Copy only "name"
     * // partial.getName() returns "John"; partial.getAge() returns 0 (default)
     * User partial = Beans.copy(original, Arrays.asList("name"));
     *
     * Beans.copy(original, Collections.emptyList());   // returns an empty copy (no props set)
     * }</pre>
     *
     * @param <T> the type of the source bean.
     * @param sourceBean the source bean to copy; may be {@code null}.
     * @param selectPropNames the property names to copy; unselected properties retain their default values.
     *        If {@code null}, all properties are copied; an empty collection copies no properties.
     * @return a new instance of the same type with the selected properties copied from {@code sourceBean},
     *         or {@code null} if {@code sourceBean} is {@code null}.
     * @throws IllegalArgumentException if a selected property is not found in the bean class.
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final Collection<String> selectPropNames) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copyAs(sourceBean, selectPropNames, (Class<T>) sourceBean.getClass());
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
     * User original = new User("John", 25);
     *
     * // Copy only String-valued properties
     * User copy = Beans.copy(original,
     *     (propName, propValue) -> propValue instanceof String);
     * // copy.getName() returns "John"; copy.getAge() returns 0 (age is int, filtered out)
     * }</pre>
     *
     * @param <T> the type of the source bean.
     * @param sourceBean the source bean to copy; may be {@code null}.
     * @param propFilter a predicate receiving the property name and value; returns {@code true} to include
     *        the property in the copy.
     * @return a new instance of the same type with matching properties copied from {@code sourceBean},
     *         or {@code null} if {@code sourceBean} is {@code null}.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final BiPredicate<? super String, Object> propFilter) {
        if (sourceBean == null) {
            return null; // NOSONAR
        }

        return copyAs(sourceBean, propFilter, (Class<T>) sourceBean.getClass());
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
     * User user = new User("John", 25);
     *
     * // Convert/copy to another bean type that shares property names
     * UserDTO dto = Beans.copyAs(user, UserDTO.class);   // returns UserDTO with matching properties
     *
     * Beans.copyAs(null, User.class);          // returns a new empty (non-null) instance
     * Beans.copyAs(user, (Class<User>) null);  // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean to copy properties from; may be {@code null}.
     * @param targetType the class of the target bean to create; must not be {@code null}.
     * @return a new instance of the target type with matching properties copied from {@code sourceBean};
     *         never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     */
    public static <T> T copyAs(final Object sourceBean, final Class<? extends T> targetType) throws IllegalArgumentException {
        return copyAs(sourceBean, (Collection<String>) null, targetType);
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
     * UserDTO dto = Beans.copyAs(entity,
     *     Arrays.asList("name", "age", "email"),
     *     UserDTO.class); // dto has name, age, and email but not password
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean to copy properties from; may be {@code null}.
     * @param selectPropNames the property names to copy; unselected properties retain their default values.
     *        If {@code null}, all matching properties are copied; an empty collection copies no properties.
     * @param targetType the class of the target bean to create; must not be {@code null}.
     * @return a new instance of the target type with the selected properties copied; never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a selected property
     *         is not found in the source bean or in the target bean.
     */
    public static <T> T copyAs(final Object sourceBean, final Collection<String> selectPropNames, @NotNull final Class<? extends T> targetType)
            throws IllegalArgumentException {
        return copyAs(sourceBean, selectPropNames, Fn.identity(), targetType);
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
     * UserDTO dto = Beans.copyAs(entity,
     *     Arrays.asList("firstName", "lastName"),
     *     propName -> Strings.toSnakeCase(propName),
     *     UserDTO.class);
     * // dto.first_name will be "John"
     * // dto.last_name will be "Doe"
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean to copy properties from; may be {@code null}.
     * @param selectPropNames the source property names to copy; if {@code null}, all matching properties
     *        are copied; an empty collection copies no properties.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged.
     * @param targetType the class of the target bean to create; must not be {@code null}.
     * @return a new instance of the target type with properties copied and names converted; never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a selected property
     *         is not found in the source bean or its (converted) name is not found in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copyAs(final Object sourceBean, final Collection<String> selectPropNames, final Function<String, String> propNameConverter,
            @NotNull final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            // The Kryo shortcut copies properties verbatim, so it must not be taken when a
            // non-identity propNameConverter is supposed to remap property names.
            if (selectPropNames == null && propNameConverter == Fn.<String> identity() && Utils.kryoParser != null && targetType.equals(srcCls)
                    && !notKryoCompatible.contains(srcCls)) {
                try {
                    final T copy = (T) Utils.kryoParser.shallowCopy(sourceBean);

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
            mergeInto(sourceBean, result, selectPropNames, propNameConverter, Fn.selectFirst(), targetBeanInfo);
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
     * UserDTO dto = Beans.copyAs(entity,
     *     (propName, propValue) -> propValue != null && !propName.equals("password"),
     *     UserDTO.class); // dto has name and age but not email (null) or password
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean to copy properties from; may be {@code null}.
     * @param propFilter a predicate receiving the property name and value; returns {@code true} to include
     *        the property in the copy.
     * @param targetType the class of the target bean to create; must not be {@code null}.
     * @return a new instance of the target type with filtered properties copied; never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a source property that
     *         passes the filter has no matching property in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T copyAs(final Object sourceBean, final BiPredicate<? super String, Object> propFilter, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        return copyAs(sourceBean, propFilter, Fn.identity(), targetType);
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
     * UserDTO dto = Beans.copyAs(entity,
     *     (propName, propValue) -> !propName.equals("password"),
     *     propName -> Strings.toSnakeCase(propName),
     *     UserDTO.class);
     * // dto.first_name will be "John"
     * // dto.last_name will be "Doe"
     * // password is excluded
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean to copy properties from; may be {@code null}.
     * @param propFilter a predicate receiving the property name and value; returns {@code true} to include
     *        the property in the copy.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged.
     * @param targetType the class of the target bean to create; must not be {@code null}.
     * @return a new instance of the target type with filtered and name-converted properties copied;
     *         never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a source property that
     *         passes the filter has no matching (converted) property name in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T copyAs(final Object sourceBean, final BiPredicate<? super String, Object> propFilter, final Function<String, String> propNameConverter,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            // The Kryo shortcut copies properties verbatim, so it must not be taken when a
            // non-identity propNameConverter is supposed to remap property names.
            if (propFilter == BiPredicates.alwaysTrue() && propNameConverter == Fn.<String> identity() && Utils.kryoParser != null && targetType.equals(srcCls)
                    && !notKryoCompatible.contains(srcCls)) {
                try {
                    final T copy = (T) Utils.kryoParser.shallowCopy(sourceBean);

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
            mergeInto(sourceBean, result, propFilter, propNameConverter, Fn.selectFirst(), targetBeanInfo);
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
     * <p><b>Note:</b> unlike the other {@code copyAs} overloads, source properties whose value is
     * {@code null} (or equal to its runtime type's default value) are skipped: for those properties the
     * new instance keeps the value assigned by its constructor/initializer. Because primitive property
     * values are read as their boxed wrapper types (e.g. {@code Integer}/{@code Boolean}), whose default
     * value is {@code null}, a primitive equal to its default &mdash; e.g. an {@code int} of {@code 0}
     * or a {@code boolean} of {@code false} &mdash; is <b>not</b> treated as default here and <em>is</em>
     * copied. The unmatched-property check (and the resulting exception when
     * {@code ignoreUnmatchedProperty} is {@code false}) is therefore only applied to source properties
     * whose value is non-{@code null} (and non-default).</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * UserEntity entity = new UserEntity("John", 25, "john@example.com", "password123");
     *
     * // Copy all properties except sensitive ones
     * UserDTO dto = Beans.copyAs(entity,
     *     true,
     *     N.asSet("password", "internalId"),
     *     UserDTO.class); // dto has all properties except password and internalId
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean to copy properties from; may be {@code null}.
     * @param ignoreUnmatchedProperty if {@code true}, source properties without a matching target property
     *        are silently skipped; if {@code false}, an {@link IllegalArgumentException} is thrown.
     * @param ignoredPropNames a set of source property names to exclude from copying; ignored if {@code null}.
     * @param targetType the class of the target bean to create; must not be {@code null}.
     * @return a new instance of the target type with properties copied (excluding ignored ones);
     *         never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if
     *         {@code ignoreUnmatchedProperty} is {@code false} and an unmatched property with a
     *         non-{@code null} source value is found.
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> T copyAs(final Object sourceBean, final boolean ignoreUnmatchedProperty, final Set<String> ignoredPropNames,
            @NotNull final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        // No Kryo shallowCopy fast-path here: this overload skips null/default-valued source properties
        // (see javadoc), but shallowCopy clones verbatim including nulls, which would violate that contract
        // whenever Kryo is present and targetType == source class (making the result Kryo-presence dependent).
        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            mergeInto(sourceBean, result, ignoreUnmatchedProperty, ignoredPropNames, targetBeanInfo);
        }

        result = targetBeanInfo.finishBeanResult(result);

        return (T) result;
    }

    @SuppressWarnings("deprecation")
    private static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty,
            final Set<String> ignoredPropNames, final BeanInfo targetBeanInfo) throws IllegalArgumentException {
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

    private static final BinaryOperator<?> DEFAULT_MERGE_FUNC = (a, b) -> a == null ? b : a;

    /**
     * Merges properties from the source bean into the target bean.
     *
     * <p>This method copies all properties from the source bean to the target bean.
     * The default merge strategy uses the source value when it is non-{@code null}; when the
     * source value is {@code null}, the existing target value is retained (note: primitive
     * properties are never {@code null}, so their values — including defaults such as {@code 0} —
     * always overwrite the target). Source properties with no matching property in the target
     * bean are silently skipped. Unlike {@code copy}
     * methods, which create new instances, this modifies the existing target bean in place.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User source = new User("Jane", 30);
     * User target = new User("John", 25);
     *
     * // target.getName() returns "Jane"; target.getAge() returns 30
     * Beans.mergeInto(source, target);
     *
     * Beans.mergeInto(null, target);   // target is unchanged
     * Beans.mergeInto(source, null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @return {@code targetBean} with merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     */
    public static <T> T mergeInto(final Object sourceBean, final T targetBean) throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, DEFAULT_MERGE_FUNC);
    }

    /**
     * Merges properties from the source bean into the target bean using a custom merge function.
     *
     * <p>The merge function determines how to combine values when a property exists in both beans.
     * It receives the source value and target value, and returns the value to set in the target.</p>
     *
     * <p><b>Note:</b> source properties with no matching property in the target bean are silently
     * skipped, consistent with {@link #mergeInto(Object, Object)} and
     * {@link #mergeInto(Object, Object, Function, BinaryOperator)}. Use an overload that accepts a
     * {@code boolean ignoreUnmatchedProperty} (passing {@code false}) if you instead want an
     * {@link IllegalArgumentException} thrown for an unmatched source property.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User source = new User("A", 5);
     * User target = new User("B", 10);
     *
     * // Sum numeric properties; otherwise keep the source value
     * Beans.mergeInto(source, target, (sourceVal, targetVal) -> {
     *     if (sourceVal instanceof Integer && targetVal instanceof Integer) {
     *         return ((Integer) sourceVal) + ((Integer) targetVal);
     *     }
     *     return sourceVal;
     * });
     * // target.getName() returns "A"; target.getAge() returns 15 (5 + 10)
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target; must not be {@code null}.
     * @return {@code targetBean} with merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, final T targetBean, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, true, null, mergeFunc);
    }

    /**
     * Merges properties from the source bean into the target bean, excluding specified properties.
     *
     * <p>This method merges all properties except those in the ignored set. The
     * {@code ignoreUnmatchedProperty} parameter controls whether an exception is thrown
     * when a property exists in the source but not in the target.
     * The default merge strategy is null-preserving: a non-{@code null} source value replaces the
     * target value, while a {@code null} source value leaves the existing target value unchanged.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User source = new User("John", 30, "john@example.com", "password123");
     * User target = new User("Jane", 25, "jane@example.com", "oldpass");
     *
     * // Merge all except password
     * Beans.mergeInto(source, target,
     *     true,
     *     N.asSet("password")); // keeps target password unchanged
     * // target: name="John", age=30, email="john@example.com", password="oldpass"
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param ignoreUnmatchedProperty if {@code true}, source properties without a matching target property
     *        are silently skipped; if {@code false}, an {@link IllegalArgumentException} is thrown.
     * @param ignoredPropNames a set of source property names to exclude from merging; ignored if {@code null}.
     * @return {@code targetBean} with properties merged (excluding ignored ones).
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if
     *         {@code ignoreUnmatchedProperty} is {@code false} and an unmatched property is found.
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty,
            final Set<String> ignoredPropNames) throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, ignoreUnmatchedProperty, ignoredPropNames, DEFAULT_MERGE_FUNC);
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
     * // Merge all except createdDate, using custom logic to add integer values
     * Beans.mergeInto(source, target,
     *     true,
     *     N.asSet("createdDate"),
     *     (srcVal, tgtVal) -> {
     *         // For integer values, add them together
     *         if (srcVal instanceof Integer && tgtVal instanceof Integer) {
     *             return ((Integer) srcVal) + ((Integer) tgtVal);
     *         }
     *         return srcVal;
     *     });
     * // target: name="New Product", price=350, quantity=25, createdDate="2023-01-01"
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param ignoreUnmatchedProperty if {@code true}, source properties without a matching target property
     *        are silently skipped; if {@code false}, an {@link IllegalArgumentException} is thrown.
     * @param ignoredPropNames a set of source property names to exclude from merging; ignored if {@code null}.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target.
     * @return {@code targetBean} with properties merged using custom logic.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if
     *         {@code ignoreUnmatchedProperty} is {@code false} and an unmatched property is found.
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty,
            final Set<String> ignoredPropNames, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
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

    /**
     * Merges properties from the source bean into the target bean with property name conversion
     * and a custom merge function.
     *
     * <p>This method allows property name mapping during the merge operation, useful when
     * source and target beans have different naming conventions. Source properties whose
     * (converted) names have no matching property in the target bean are silently skipped.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Source has camelCase, target has snake_case
     * SourceBean source = new SourceBean();
     * source.setFirstName("John");
     * TargetBean target = new TargetBean();
     *
     * Beans.mergeInto(source, target,
     *     propName -> Strings.toSnakeCase(propName),
     *     (srcVal, tgtVal) -> srcVal != null ? srcVal : tgtVal);
     * // target.first_name is now "John"
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target.
     * @return {@code targetBean} with merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, final T targetBean, final Function<String, String> propNameConverter,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        final BiPredicate<? super String, Object> propFilter = (srcPropName,
                srcPropValue) -> targetBeanInfo.getPropInfo(propNameConverter.apply(srcPropName)) != null;

        final Collection<String> selectPropNames = getPropNames(sourceBean, propFilter);

        return mergeInto(sourceBean, targetBean, selectPropNames, propNameConverter, mergeFunc, targetBeanInfo);
    }

    /**
     * Merges selected properties from the source bean into the target bean.
     *
     * <p>Only properties whose names are in the selection list will be merged.
     * This is useful for partial updates where only specific fields should be modified.
     * The default merge strategy is null-preserving: a non-{@code null} source value replaces the
     * target value, while a {@code null} source value leaves the existing target value unchanged.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User existingUser = new User("John", 25, "john@old.com");
     * User updates = new User("Jane", 30, "jane@new.com");
     *
     * // Only update email
     * Beans.mergeInto(updates, existingUser, Arrays.asList("email"));
     * // existingUser still has name="John", age=25, but email="jane@new.com"
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param selectPropNames the source property names to merge. If {@code null}, all properties
     *        are merged. If empty, no properties are merged.
     * @return {@code targetBean} with the selected properties merged.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property
     *         is not found in the source bean or in the target bean.
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames)
            throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, selectPropNames, Fn.identity());
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
     * // Only merge age and score, keeping the higher value when both are integers
     * Beans.mergeInto(updates, existingUser,
     *     Arrays.asList("age", "score"),
     *     (srcVal, tgtVal) -> {
     *         if (srcVal instanceof Integer && tgtVal instanceof Integer) {
     *             return Math.max((Integer) srcVal, (Integer) tgtVal);
     *         }
     *         return srcVal;
     *     });
     * // existingUser: name="John", age=30, score=200
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param selectPropNames the source property names to merge. If {@code null}, all properties
     *        are merged. If empty, no properties are merged.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target.
     * @return {@code targetBean} with the selected properties merged.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property
     *         is not found in the source bean or in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, selectPropNames, Fn.identity(), mergeFunc);
    }

    /**
     * Merges selected properties from the source bean into the target bean with property name conversion.
     *
     * <p>This method combines selective property merging with name conversion, useful when
     * merging between beans with different naming conventions.
     * The default merge strategy is null-preserving: a non-{@code null} source value replaces the
     * target value, while a {@code null} source value leaves the existing target value unchanged.</p>
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
     * Beans.mergeInto(source, target,
     *     Arrays.asList("firstName"),
     *     propName -> Strings.toSnakeCase(propName));
     * // target.first_name is now "John"
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param selectPropNames the source property names to merge. If {@code null}, all properties
     *        are merged. If empty, no properties are merged.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged.
     * @return {@code targetBean} with the selected (and name-converted) properties merged.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property
     *         is not found in the source bean or in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        return mergeInto(sourceBean, targetBean, selectPropNames, propNameConverter, DEFAULT_MERGE_FUNC);
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
     * Beans.mergeInto(source, target,
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
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param selectPropNames the source property names to merge. If {@code null}, all properties
     *        are merged. If empty, no properties are merged.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target.
     * @return {@code targetBean} with the selected, name-converted, and merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property
     *         is not found in the source bean or in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        return mergeInto(sourceBean, targetBean, selectPropNames, propNameConverter, mergeFunc, targetBeanInfo);
    }

    private static <T> T mergeInto(final Object sourceBean, final T targetBean, final Collection<String> selectPropNames,
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

    /**
     * Merges properties from the source bean into the target bean based on a filter predicate.
     *
     * <p>The predicate receives each property name and value from the source bean and
     * determines whether that property should be merged into the target.
     * The default merge strategy is null-preserving: a non-{@code null} source value replaces the
     * target value, while a {@code null} source value leaves the existing target value unchanged.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User source = new User("John", 0, null);
     * User target = new User("Jane", 25, "jane@example.com");
     *
     * // Only merge non-null and non-zero values
     * Beans.mergeInto(source, target,
     *     (propName, propValue) -> propValue != null &&
     *         !(propValue instanceof Number && ((Number) propValue).intValue() == 0));
     * // target keeps age=25 and email="jane@example.com" but name becomes "John"
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param propFilter a predicate receiving the property name and source value; returns {@code true} to
     *        merge the property into the target.
     * @return {@code targetBean} with matching properties merged.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes
     *         the filter has no matching property in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, final T targetBean, final BiPredicate<? super String, Object> propFilter)
            throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, propFilter, DEFAULT_MERGE_FUNC);
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
     * Beans.mergeInto(source, target,
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
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param propFilter a predicate receiving the property name and source value; returns {@code true} to
     *        merge the property into the target.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target.
     * @return {@code targetBean} with filtered and merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes
     *         the filter has no matching property in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, propFilter, Fn.identity(), mergeFunc);
    }

    /**
     * Merges properties from the source bean into the target bean based on a filter predicate
     * with property name conversion.
     *
     * <p>This method allows filtering properties and converting their names during the merge,
     * useful when working with beans that have different naming conventions.
     * The default merge strategy is null-preserving: a non-{@code null} source value replaces the
     * target value, while a {@code null} source value leaves the existing target value unchanged.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SourceBean source = new SourceBean();
     * source.setFirstName("John");
     * source.setLastName("");   // source is set with an empty lastName
     *
     * TargetBean target = new TargetBean();
     *
     * // Only merge non-empty strings, converting to snake_case
     * Beans.mergeInto(source, target,
     *     (propName, propValue) -> propValue instanceof String && !((String) propValue).isEmpty(),
     *     propName -> Strings.toSnakeCase(propName));
     * // target.first_name = "John", last_name is not merged
     * }</pre>
     *
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param propFilter a predicate receiving the property name and source value; returns {@code true} to
     *        merge the property into the target.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged.
     * @return {@code targetBean} with filtered and name-converted properties merged.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes
     *         the filter has no matching property in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final Function<String, String> propNameConverter) throws IllegalArgumentException {
        return mergeInto(sourceBean, targetBean, propFilter, propNameConverter, DEFAULT_MERGE_FUNC);
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
     * Beans.mergeInto(source, target,
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
     * @param <T> the type of the target bean.
     * @param sourceBean the source bean from which properties are copied; if {@code null}, the target bean is returned unchanged.
     * @param targetBean the target bean into which properties are merged; must not be {@code null}.
     * @param propFilter a predicate receiving the property name and source value; returns {@code true} to
     *        merge the property into the target; must not be {@code null}.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged; must not be {@code null}.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target; must not be {@code null}.
     * @return {@code targetBean} with filtered, name-converted, and merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes
     *         the filter has no matching property in the target bean.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException {
        N.checkArgNotNull(targetBean, cs.targetBean);

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        return mergeInto(sourceBean, targetBean, propFilter, propNameConverter, mergeFunc, targetBeanInfo);
    }

    private static <T> T mergeInto(final Object sourceBean, final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc, final BeanInfo targetBeanInfo) {
        if (sourceBean == null) {
            return targetBean;
        }

        final boolean isIdentityPropNameConverter = propNameConverter == Fn.<String> identity();
        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BinaryOperator<Object> objPropMergeFunc = (BinaryOperator<Object>) mergeFunc;

        Object propValue = null;
        PropInfo targetPropInfo = null;
        String targetPropName = null;

        for (final PropInfo propInfo : srcBeanInfo.propInfoList) {
            propValue = propInfo.getPropValue(sourceBean);

            if (propFilter.test(propInfo.name, propValue)) {
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
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * // user.getName() returns null; user.getAge() returns 0; user.getActive() still true
     * Beans.clearProps(user, "name", "age");
     *
     * Beans.clearProps(user, new String[0]);   // no change
     * Beans.clearProps(null, "name");          // no change
     * }</pre>
     *
     * @param bean the bean object whose properties are to be cleared; if {@code null}, the method does nothing.
     * @param propNames the names of the properties to clear; if empty, the method does nothing.
     * @throws IllegalArgumentException if any name in {@code propNames} is not a property of the bean.
     */
    public static void clearProps(final Object bean, final String... propNames) {
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
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * // user.getName() returns null; user.getAge() returns 0; user.getActive() still true
     * Beans.clearProps(user, Arrays.asList("name", "age"));
     *
     * Beans.clearProps(user, Collections.emptyList());   // no change
     * Beans.clearProps(null, Arrays.asList("name"));     // no change
     * }</pre>
     *
     * @param bean the bean object whose properties are to be cleared; if {@code null}, the method does nothing.
     * @param propNames the collection of property names to clear; if empty, the method does nothing.
     * @throws IllegalArgumentException if any name in {@code propNames} is not a property of the bean.
     */
    public static void clearProps(final Object bean, final Collection<String> propNames) {
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
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * // user.getName() returns null; user.getAge() returns 0; user.getActive() returns null
     * Beans.clearAllProps(user);
     *
     * Beans.clearAllProps(null);   // no change
     * }</pre>
     *
     * @param bean the bean object whose properties are to be erased. If this is {@code null}, the method does nothing.
     */
    public static void clearAllProps(final Object bean) {
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
     * User user = new User();
     * // every property is now filled with a random value
     * // user.getName() is a non-null random String (e.g. a 16-char UUID fragment)
     * Beans.randomize(user);
     *
     * Beans.randomize((Object) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the bean object to populate; must not be {@code null} and must be a valid bean class.
     * @throws IllegalArgumentException if {@code bean} is {@code null} or not a valid bean class.
     */
    public static void randomize(final Object bean) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);

        final Class<?> beanClass = bean.getClass();
        N.checkBeanClass(beanClass);

        randomize(bean, Beans.getPropNameList(beanClass));
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
     * Beans.randomize(user, Arrays.asList("name"));
     * // user.getName() is now a non-null random value; user.getAge() stays 0; active stays null
     * }</pre>
     *
     * @param bean the bean object to populate; must not be {@code null} and must be a valid bean class.
     * @param propNamesToFill the names of the properties to fill with random values; must not be {@code null}.
     * @throws IllegalArgumentException if {@code bean} is {@code null} or not a valid bean class,
     *         or if a property name is not found in the bean.
     */
    public static void randomize(final Object bean, final Collection<String> propNamesToFill) {
        N.checkArgNotNull(bean, cs.bean);
        N.checkBeanClass(bean.getClass());

        populateWithRandomValues(ParserUtil.getBeanInfo(bean.getClass()), bean, propNamesToFill);
    }

    /**
     * Creates a new instance of the specified bean class and fills all its properties with random values.
     *
     * <p>This is a convenience method that combines object creation and property filling in one step.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // user is non-null with every property filled (user.getName() is non-null)
     * User user = Beans.newRandomBean(User.class);
     *
     * Beans.newRandomBean((Class<?>) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the bean.
     * @param beanClass the class to instantiate and populate; must not be {@code null} and must be a valid bean class.
     * @return a new instance with all properties filled with random values; never {@code null}.
     * @throws IllegalArgumentException if {@code beanClass} is {@code null} or not a valid bean class.
     */
    public static <T> T newRandomBean(final Class<? extends T> beanClass) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        N.checkBeanClass(beanClass);

        return newRandomBean(beanClass, Beans.getPropNameList(beanClass));
    }

    /**
     * Creates a new instance of the specified bean class and fills only the specified properties.
     *
     * <p>Properties not included in the collection will retain their default values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // class User { String name; int age; Boolean active; ... }
     * User user = Beans.newRandomBean(User.class, Arrays.asList("name"));
     *
     * // user.getName() is a non-null random value; user.getAge() stays 0; active stays null
     * }</pre>
     *
     * @param <T> the type of the bean.
     * @param beanClass the class to instantiate and populate; must not be {@code null} and must be a valid bean class.
     * @param propNamesToFill the names of the properties to fill with random values; must not be {@code null}.
     * @return a new instance with the specified properties filled with random values; never {@code null}.
     * @throws IllegalArgumentException if {@code beanClass} is {@code null} or not a valid bean class,
     *         or if a property name is not found in the class.
     */
    public static <T> T newRandomBean(final Class<? extends T> beanClass, final Collection<String> propNamesToFill) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        N.checkBeanClass(beanClass);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        final Object result = beanInfo.createBeanResult();

        // Track classes currently being filled in this thread so a self-referential bean
        // (e.g. `class Node { Node parent; }`) doesn't infinitely recurse.
        final java.util.Set<Class<?>> visited = NEW_RANDOM_VISITED.get();
        final boolean topLevel = visited.isEmpty();
        visited.add(beanClass);
        try {
            populateWithRandomValues(beanInfo, result, propNamesToFill);
        } finally {
            visited.remove(beanClass);
            if (topLevel) {
                NEW_RANDOM_VISITED.remove();
            }
        }

        return beanInfo.finishBeanResult(result);
    }

    /** Per-thread visited-class set used by {@link #newRandomBean(Class, Collection)} to break cycles. */
    private static final ThreadLocal<java.util.Set<Class<?>>> NEW_RANDOM_VISITED = ThreadLocal.withInitial(java.util.HashSet::new);

    /**
     * Creates multiple instances of the specified bean class, each filled with random values.
     *
     * <p>This method is useful for generating test data sets or when you need multiple
     * test objects with varying random data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // users.size() == 3; each element has every property filled with a random value
     * List<User> users = Beans.newRandomBeanList(User.class, 3);
     *
     * Beans.newRandomBeanList(User.class, 0);   // returns [] (empty list)
     * }</pre>
     *
     * @param <T> the type of the bean.
     * @param beanClass the class to instantiate and populate; must not be {@code null} and must be a valid bean class.
     * @param count the number of instances to create; must not be negative.
     * @return a list containing exactly {@code count} newly created and fully populated bean instances;
     *         never {@code null}.
     * @throws IllegalArgumentException if {@code beanClass} is {@code null}, not a valid bean class,
     *         or {@code count} is negative.
     */
    public static <T> List<T> newRandomBeanList(final Class<? extends T> beanClass, final int count) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        N.checkBeanClass(beanClass);

        return newRandomBeanList(beanClass, Beans.getPropNameList(beanClass), count);
    }

    /**
     * Creates multiple instances of the specified bean class with only the specified properties filled.
     *
     * <p>Each instance will have the same set of properties filled but with different random values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // class User { String name; int age; Boolean active; ... }
     * // users.size() == 2; each user has a random name but age stays 0 and active stays null
     * List<User> users = Beans.newRandomBeanList(User.class, Arrays.asList("name"), 2);
     *
     * Beans.newRandomBeanList(User.class, Arrays.asList("name"), 0);   // returns [] (empty list)
     * }</pre>
     *
     * @param <T> the type of the bean.
     * @param beanClass the class to instantiate and populate; must not be {@code null} and must be a valid bean class.
     * @param propNamesToFill the names of the properties to fill with random values; must not be {@code null}.
     * @param count the number of instances to create; must not be negative.
     * @return a list containing exactly {@code count} newly created bean instances with the specified
     *         properties filled; never {@code null}.
     * @throws IllegalArgumentException if {@code beanClass} is {@code null}, not a valid bean class,
     *         {@code count} is negative, or a property name is not found in the class.
     */
    public static <T> List<T> newRandomBeanList(final Class<? extends T> beanClass, final Collection<String> propNamesToFill, final int count)
            throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        N.checkBeanClass(beanClass);
        N.checkArgNotNegative(count, cs.count);

        final List<T> resultList = new ArrayList<>(count);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        Object result = null;

        for (int i = 0; i < count; i++) {
            result = beanInfo.createBeanResult();

            populateWithRandomValues(beanInfo, result, propNamesToFill);

            resultList.add(beanInfo.finishBeanResult(result));
        }

        return resultList;
    }

    private static void populateWithRandomValues(final BeanInfo beanInfo, final Object bean, final Collection<String> propNamesToFill) {
        N.checkArgNotNull(propNamesToFill, cs.propNamesToFill);

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
                if (Strings.containsIgnoreCase(propName, "email")) {
                    propValue = Strings.uuid().substring(0, 12) + "@email.com";
                } else {
                    propValue = Strings.uuid().substring(0, 16);
                }
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
                // Skip recursion if we'd revisit a class already on this thread's call stack
                // (cycle / self-reference). Leaves the property at its default (null) value.
                if (NEW_RANDOM_VISITED.get().contains(parameterClass)) {
                    propValue = type.defaultValue();
                } else {
                    propValue = newRandomBean(parameterClass);
                }
            } else {
                propValue = type.defaultValue();
            }

            propInfo.setPropValue(bean, propValue);
        }
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
     * User user = new User("John", 25);
     * user.setActive(true);
     *
     * Beans.stream(user).count();   // returns 3 (one entry per property)
     * // Outputs: name: John / age: 25 / active: true
     * Beans.stream(user).forEach(e -> System.out.println(e.getKey() + ": " + e.getValue()));
     *
     * Beans.stream(null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the bean object to extract properties from; must not be {@code null}.
     * @return a {@link Stream} of {@link Map.Entry} objects where each key is a property name
     *         and each value is the corresponding property value (which may be {@code null}).
     * @throws IllegalArgumentException if {@code bean} is {@code null}.
     */
    public static Stream<Map.Entry<String, Object>> stream(final Object bean) {
        N.checkArgNotNull(bean, cs.bean);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        return Stream.of(beanInfo.propInfoList).map(propInfo -> N.newEntry(propInfo.name, propInfo.getPropValue(bean)));
    }

    /**
     * Creates a filtered stream of property name-value pairs from the specified bean.
     *
     * <p>This method is similar to {@link #stream(Object)} but allows filtering
     * of properties based on a predicate. Only properties that match the predicate
     * criteria are included in the returned stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User("John", 25);
     *
     * // Only String-valued properties
     * Beans.stream(user, (name, value) -> value instanceof String).count();   // returns 1 (name)
     *
     * // Only non-null properties (active is null, excluded)
     * Beans.stream(user, (name, value) -> value != null).count();             // returns 2 (name, age)
     * }</pre>
     *
     * @param bean the bean object to extract properties from; must not be {@code null}.
     * @param propFilter a {@link BiPredicate} that receives the property name and its value and returns
     *        {@code true} to include the property in the stream, or {@code false} to skip it.
     * @return a {@link Stream} of {@link Map.Entry} objects where each key is a property name
     *         and each value is the corresponding property value (which may be {@code null}),
     *         containing only those properties for which {@code propFilter} returned {@code true}.
     * @throws IllegalArgumentException if {@code bean} is {@code null}.
     */
    public static Stream<Map.Entry<String, Object>> stream(final Object bean, final BiPredicate<? super String, Object> propFilter) {
        N.checkArgNotNull(bean, cs.bean);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        return Stream.of(beanInfo.propInfoList).map(propInfo -> {
            final Object propValue = propInfo.getPropValue(bean);
            return propFilter.test(propInfo.name, propValue) ? N.newEntry(propInfo.name, propValue) : null;
        }).skipNulls();
    }
}
