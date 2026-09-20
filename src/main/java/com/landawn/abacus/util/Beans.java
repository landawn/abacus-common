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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.stream.Stream;

/**
 * A comprehensive utility class providing an extensive collection of static methods for JavaBean operations,
 * introspection, property manipulation, and object transformation. This class serves as the primary bean
 * utility facade in the Abacus library, offering performance-optimized operations for JavaBean patterns
 * with extensive support for reflection, type conversion, and object lifecycle management.
 *
 * <p>The {@code Beans} class is designed as a final utility class that provides a complete toolkit
 * for JavaBean processing including property access, bean-to-map conversion, map-to-bean conversion,
 * object cloning, merging, validation, and introspection. Its static methods coordinate internal metadata
 * access; callers must coordinate access to shared mutable beans, collections, output maps and callbacks.
 * The {@link BeanMapBuilder} returned by {@link #mapBuilder(Object)} is a single-threaded, single-use builder.
 * Metadata is cached so that repeated operations on the same class avoid re-running reflection.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Bean Introspection:</b> Complete property discovery and analysis using reflection</li>
 *   <li><b>Property Access:</b> Dynamic getter/setter invocation with type safety</li>
 *   <li><b>Object Conversion:</b> Bean-to-Map and Map-to-Bean transformations</li>
 *   <li><b>Deep Cloning:</b> Recursive object copying via {@link #deepCopy(Object)}. Same-class copies
 *       use Kryo when it is on the classpath and preserve shared identity and cycles; otherwise an XML
 *       round-trip is used, which has a serialization depth cap and cannot restore cyclic graphs</li>
 *   <li><b>Bean Merging:</b> Intelligent object merging with customizable merge strategies</li>
 *   <li><b>Type Conversion:</b> Automatic type conversion between compatible types</li>
 *   <li><b>Performance Caching:</b> Extensive caching of reflection metadata for optimal performance</li>
 *   <li><b>Annotation Support:</b> JAXB XML-binding annotations plus {@code @Entity}, {@code @Record} and
 *       {@code @DiffIgnore}</li>
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
 *   <li><b>JavaBean Conventions:</b> Getter/setter patterns and no-arg constructors, extended beyond
 *       {@link java.beans.Introspector} to also expose public mutable fields, records, and builder classes.
 *       A property is normally a getter <i>with a matching setter</i>; a setter alone is never a property, and
 *       a getter alone is one only on a class that cannot have setters (record, builder-based, immutable,
 *       {@code @Entity}, JAXB). A {@code public} non-{@code static} non-{@code final} field is a property on
 *       its own. See {@link #getPropNameList(Class)} for the exact rule</li>
 *   <li><b>Null Handling:</b> There is no single rule; each method documents its own, and the three shapes
 *       are: the conversion, copy and merge families treat a {@code null} bean or map as "nothing to do"
 *       (an empty map, {@code null}, or an unmodified target); most introspection, creation and randomization
 *       methods reject a {@code null} {@code Class}/{@code bean} argument with
 *       {@link IllegalArgumentException}; and the property accessors that take the bean itself
 *       ({@link #getPropValue(Object, String)}, {@link #getPropValueIfPresent(Object, String)},
 *       {@link #getPropNames(Object, boolean)}) likewise reject a {@code null} bean with
 *       {@link IllegalArgumentException}. A {@code null} bean passed to
 *       {@link #getPropValue(Object, Method)}, {@link #setPropValue(Object, Method, Object)} or
 *       {@link #setPropValueByGetter(Object, Method, Object)} is a {@link NullPointerException} when the
 *       method is an instance method - for {@code setPropValueByGetter} only when {@code propValue} is
 *       non-{@code null}, since a {@code null} value is a no-op. The classifiers {@code isBeanClass(null)}
 *       and {@code isRecordClass(null)} return {@code false}</li>
 *   <li><b>Performance First:</b> Extensive caching of reflection metadata to minimize runtime overhead</li>
 *   <li><b>Type Safety:</b> Generic methods with compile-time type checking and runtime validation</li>
 *   <li><b>Flexibility:</b> Support for various object patterns including builders, records, and entities</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Bean validation and introspection
 * boolean isBean = Beans.isBeanClass(User.class);                     // returns true for a standard bean class
 * List<String> properties = Beans.getPropNameList(User.class);        // returns cached property names
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
 * Map<String, Object> shallowMap = Beans.beanToMap(user);                                    // returns a map of non-null properties
 * Map<String, Object> deepMap = Beans.deepBeanToMap(user);                                   // returns nested bean properties as maps
 * Map<String, Object> selectedMap = Beans.beanToMap(user, Arrays.asList("name", "email"));   // returns selected properties only
 *
 * // Map to Bean conversion
 * Map<String, Object> userData = Map.of("name", "Jane", "age", 25, "email", "jane@example.com");
 * User userFromMap = Beans.mapToBean(userData, User.class);                      // returns a populated User
 * User userFromMapIgnoreUnknown = Beans.mapToBean(userData, true, User.class);   // treats unknown properties as ignored
 *
 * // Object merging with strategies
 * User source = new User("John", 30, "john@example.com");
 * User target = new User("Jane", 25, null);
 * Beans.mergeInto(source, target);                                              // target is updated from source
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
 * <p><b>Key Naming:</b> the bean-to-map methods take a {@link NamingPolicy} for the map keys, defaulting to
 * {@link NamingPolicy#CAMEL_CASE}. {@code CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE} both emit the bean's
 * property names <i>verbatim</i>. The converter is deliberately not re-run for {@code CAMEL_CASE}: a property
 * name is already the bean's canonical name, and re-converting would rewrite the ones whose declaring
 * <i>field</i> is not camelCase &mdash; a field {@code private String URL} yields the property {@code "URL"},
 * which would become {@code "url"}, and {@code XMLData} would become {@code "xmlData"}. It is also the
 * library-wide convention rather than a local choice: the JSON/XML layer applies the same rule, so
 * {@code beanToMap(bean, null, NamingPolicy.CAMEL_CASE, IntFunctions.ofLinkedHashMap())} produces the same keys
 * as the default {@link N#toJson(Object)} serialization of the same bean. Every other policy is applied to each
 * key.
 *
 * <p>The key is always derived from the matched property's own name, never from the spelling you passed.
 * A {@code selectPropNames} entry - and a {@code BeanMapBuilder.exclude} entry - may be any spelling the
 * property resolver accepts (case-insensitive, underscore-stripped, or {@code get}/{@code is}/{@code has}-
 * prefixed), but the resulting key is the bean's canonical property name with the {@link NamingPolicy}
 * applied to it. So {@code beanToMap(user, List.of("getFirstName"), NamingPolicy.SNAKE_CASE, IntFunctions.ofLinkedHashMap())}
 * yields {@code first_name}, exactly as the unselected
 * {@code beanToMap(user, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofLinkedHashMap())} does. A corollary is
 * that two spellings of one property collapse to a single entry:
 * {@code beanToMap(user, List.of("firstName", "first_name"))} produces one {@code firstName} entry.
 *
 * <p><b>What counts as a nested bean:</b> {@code deepBeanToMap} and {@code beanToFlatMap} decide whether to
 * recurse from the property's <b>declared</b> type, not from the runtime class of its value. A property
 * declared {@code Object} (or an interface type) is therefore emitted as-is even when it happens to hold a
 * bean, and beans inside a {@link java.util.Collection}, {@link Map} or array are never expanded. This keeps
 * the output shape a function of the bean class rather than of the data, and keeps it invertible by
 * {@link #mapToBean(Map, Class)}: both directions ask one shared predicate, so what is written as a nested map
 * is exactly what is read back as a nested bean.
 *
 * <p><b>Bean-to-Map Conversion Options:</b>
 * <ul>
 *   <li><b>Flat Conversion:</b> {@code beanToFlatMap(bean)} - Flat map representation with dot notation</li>
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
 *   <li><b>Lazy Initialization:</b> On-demand initialization of expensive reflection operations. A class is
 *       instantiated during discovery only to probe a JAXB-style {@link Collection}/{@link Map} getter, and at
 *       most once. Reading its {@code public static final String} constants does, however, run its static
 *       initializer; a failure there is swallowed rather than propagated to the caller</li>
 *   <li><b>Cache Lifetime and Bounds:</b> Derived per-class metadata is held through {@link ClassValue},
 *       allowing it to be collected with the class; explicit registrations are held permanently.
 *       The amount of derived metadata per class is bounded by the bean.
 *       Caches keyed by a <i>caller-supplied</i> property or converted name stop growing once they reach
 *       an internal cap: the name-conversion pools ({@link #toCamelCase(String)} and friends), and the
 *       per-class property-lookup caches, for both resolved and unresolved names. Passing unbounded
 *       caller-supplied names therefore costs repeated lookups, not memory. Note that type conversion itself
 *       is not cached here; it is delegated to {@link com.landawn.abacus.type.Type}</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Shared Metadata:</b> Reflection metadata and explicit registrations are cached globally;
 *       cache updates are synchronized internally. Deriving a class's property model normally runs <i>outside</i>
 *       that lock, so unrelated classes can be introspected concurrently. Two threads may derive the same
 *       model and one then discards its copy. After repeated invalidation by concurrent registrations,
 *       discovery falls back to holding the metadata lock to ensure progress</li>
 *   <li><b>Concurrent Caching:</b> Thread-safe caching using ConcurrentHashMap</li>
 *   <li><b>Class unloading:</b> the per-class caches this class keeps are held on the class itself (via
 *       {@link ClassValue}) rather than in a {@code Map} keyed by {@code Class}, so introspecting a class
 *       does not by itself stop that class - or its {@code ClassLoader} - from being unloaded. The
 *       {@code register*} methods are the deliberate exception: a registration is a global, permanent
 *       statement about a class and is held strongly until the JVM exits.
 *       <br>This guarantee is limited to derived caches owned by this class. {@link ClassUtil} also scopes
 *       derived reflection metadata by class lifetime, but parser/type caches populated during an operation,
 *       explicit registrations and references retained by callers can still keep a class loader alive</li>
 *   <li><b>Caller-Owned State:</b> Conversion and copy methods can create new objects; merge/fill methods
 *       ({@code mergeInto}, {@code clearProps}, {@code randomize}, output-map overloads) modify the supplied target in place</li>
 *   <li><b>Caller Coordination:</b> Internal cache synchronization does not protect bean fields, getters/setters,
 *       nested mutable values or user callbacks from concurrent access. Coordinate those according to their own contracts</li>
 *   <li><b>Shared State:</b> Internal static caches are mutable but thread-safe (concurrent/synchronized access).
 *       Every read-modify-write of the property model - registration, publication of a scan, and the alias
 *       scans in the {@code getProp*} lookups - is serialized on one private monitor</li>
 *   <li><b>Builder:</b> {@link BeanMapBuilder} is <i>not</i> thread-safe; configure and consume it on one thread</li>
 * </ul>
 *
 * <p><b>Annotation Support:</b>
 * <ul>
 *   <li><b>JAXB Annotations:</b> {@code @XmlRootElement}/{@code @XmlType} on the class and
 *       {@code @XmlElement}/{@code @XmlElements} on an accessor promote a getter-only {@link Collection} or
 *       {@link Map} property to a real property; see {@link #registerXmlBindingClass(Class)}</li>
 *   <li><b>Custom Annotations:</b> {@code @Entity} (including {@code javax}/{@code jakarta.persistence}),
 *       {@code @Record}, and {@code @DiffIgnore} (see {@link #getIgnoredPropNamesForDiff(Class)})</li>
 *   <li><b>No bean-validation integration:</b> this class does not read {@code javax}/{@code jakarta.validation}
 *       constraints and performs no constraint checking</li>
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
 *   <li><b>Contract Validation:</b> Invalid property names, incompatible values, and unsupported
 *       bean shapes may result in documented exceptions</li>
 *   <li><b>Null Handling:</b> Null behavior is method-specific; consult the individual method contract</li>
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
 *   <li>Use property name constants to keep repeated property references consistent</li>
 *   <li>Batch multiple property operations when working with the same object</li>
 *   <li>Consider using flat maps instead of deep conversion for simple use cases</li>
 *   <li>Leverage the caching mechanisms by reusing the same classes</li>
 *   <li>Use appropriate collection types for optimal conversion performance</li>
 * </ul>
 *
 * <p><b>Common Patterns:</b>
 * <ul>
 *   <li><b>Bean Validation:</b> {@code boolean isBeanType = Beans.isBeanClass(User.class);}</li>
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
 * class Address {
 *     private String street;
 *     private String city;
 *     private String zipCode;
 *     public Address() {}
 *     public Address(String street, String city, String zipCode) {
 *         this.street = street;
 *         this.city = city;
 *         this.zipCode = zipCode;
 *     }
 *     public String getStreet() { return street; }
 *     public void setStreet(String street) { this.street = street; }
 *     public String getCity() { return city; }
 *     public void setCity(String city) { this.city = city; }
 *     public String getZipCode() { return zipCode; }
 *     public void setZipCode(String zipCode) { this.zipCode = zipCode; }
 * }
 *
 * @Entity
 * class User {
 *     private String name;
 *     private int age;
 *     private Address address;
 *     private List<String> roles;
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; }
 *     public int getAge() { return age; }
 *     public void setAge(int age) { this.age = age; }
 *     public Address getAddress() { return address; }
 *     public void setAddress(Address address) { this.address = address; }
 *     public List<String> getRoles() { return roles; }
 *     public void setRoles(List<String> roles) { this.roles = roles; }
 * }
 *
 * class UserDTO {
 *     private String name;
 *     private int age;
 *     private Address address;
 *     private List<String> roles;
 *     public String getName() { return name; }
 *     public void setName(String name) { this.name = name; }
 *     public int getAge() { return age; }
 *     public void setAge(int age) { this.age = age; }
 *     public Address getAddress() { return address; }
 *     public void setAddress(Address address) { this.address = address; }
 *     public List<String> getRoles() { return roles; }
 *     public void setRoles(List<String> roles) { this.roles = roles; }
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
 * class DatabaseConfig {
 *     private String host = "localhost";
 *     private int port = 5432;
 *     private String database;
 *     private String username;
 *     private String password;
 *     private boolean ssl = false;
 *     public String getHost() { return host; }
 *     public void setHost(String host) { this.host = host; }
 *     public int getPort() { return port; }
 *     public void setPort(int port) { this.port = port; }
 *     public String getDatabase() { return database; }
 *     public void setDatabase(String database) { this.database = database; }
 *     public String getUsername() { return username; }
 *     public void setUsername(String username) { this.username = username; }
 *     public String getPassword() { return password; }
 *     public void setPassword(String password) { this.password = password; }
 *     public boolean isSsl() { return ssl; }
 *     public void setSsl(boolean ssl) { this.ssl = ssl; }
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

    // Nested property-path syntax and parser.
    private static final String PROP_NAME_SEPARATOR = ".";

    // Shared, stateless splitter for nested property paths (e.g. "address.city"); reused to avoid per-call allocation.
    private static final Splitter PROP_NAME_SPLITTER = Splitter.with(PROP_NAME_SEPARATOR);

    // Conventional JavaBean accessor prefixes.
    private static final String GET = "get";

    private static final String SET = "set";

    private static final String IS = "is";

    private static final String HAS = "has";

    @SuppressWarnings("deprecation")
    private static final int POOL_SIZE = InternalUtil.POOL_SIZE;

    /**
     * A per-class cache whose entries die with the class they describe.
     *
     * <p><b>Why not a {@code Map<Class<?>, V>}:</b> a {@code static} map keyed by {@code Class} is never
     * evicted, so one lookup pins the key class - and therefore its {@code ClassLoader}, and every class that
     * loader defined - for the life of the JVM. In a container that redeploys an application, a single
     * {@link Beans#isBeanClass(Class)} call on an application class was enough to leak the whole deployment.
     * {@link ClassValue} instead stores its value <i>on the class</i>, so the entry becomes unreachable
     * exactly when the class does. (A {@code WeakHashMap<Class<?>, V>} would not do: these values reference
     * their own key - a {@code Method} holds its declaring class - which keeps the weak entry alive forever.)
     * {@link Beans#kryoSupport} already uses {@code ClassValue} for the same reason.</p>
     *
     * <p>The value lives in a mutable {@code Slot} rather than being the {@code ClassValue}'s own value,
     * because these caches need three things {@code ClassValue} alone does not offer: a "look up without
     * computing" read, an explicit invalidation that does not race with a concurrent recompute, and the
     * ability to publish a value derived under an external lock. Reading and writing {@code Slot.value} is
     * the happens-before edge for everything the writer prepared beforehand.</p>
     *
     * @param <V> the cached value type
     */
    private static final class ClassCache<V> {
        private static final class Slot<V> {
            private volatile V value;
        }

        private final ClassValue<Slot<V>> slots = new ClassValue<>() {
            @Override
            protected Slot<V> computeValue(final Class<?> type) {
                return new Slot<>();
            }
        };

        /**
         * @param cls the class to look up; a {@code null} class simply has no entry, matching the
         *        {@code null}-tolerant {@code get} of the {@link ConcurrentCacheMap}s this replaced - several
         *        {@code getProp*} entry points read the pool before validating their argument
         * @return the cached value, or {@code null} if nothing has been published for {@code cls}
         */
        V get(final Class<?> cls) {
            return cls == null ? null : slots.get(cls).value;
        }

        boolean containsKey(final Class<?> cls) {
            return get(cls) != null;
        }

        void put(final Class<?> cls, final V value) {
            slots.get(cls).value = value;
        }

        /** Drops the entry. The {@code Slot} itself stays, so a concurrent reader never sees a stale one. */
        void remove(final Class<?> cls) {
            slots.get(cls).value = null;
        }

        /**
         * Computes and publishes a value if none is present, locking only this class's slot.
         *
         * <p>{@code mappingFunction} must not call back into the same slot, and must not return
         * {@code null} - a {@code null} is indistinguishable from "absent" here, so it would be
         * recomputed under the lock on every subsequent call.</p>
         */
        V computeIfAbsent(final Class<?> cls, final Function<Class<?>, ? extends V> mappingFunction) {
            final Slot<V> slot = slots.get(cls);
            V value = slot.value;

            if (value == null) {
                synchronized (slot) {
                    value = slot.value;

                    if (value == null) {
                        value = mappingFunction.apply(cls);
                        slot.value = value;
                    }
                }
            }

            return value;
        }
    }

    /**
     * The monitor guarding every read-modify-write of the property model: the registration pools, the
     * publication of a scan into the per-class caches, and the alias scans in the {@code getProp*} lookups.
     *
     * <p>A dedicated object rather than {@code beanDeclaredPropGetMethodPool}, which used to serve as both the
     * cache and the lock - that pool is now a {@link ClassCache} and has no identity worth locking on.</p>
     */
    private static final Object METADATA_LOCK = new Object();

    /**
     * The classes whose property model has been published, so {@link #applyRegistration(Class, Runnable)} can
     * find the already-introspected subtypes a registration against a base type also governs.
     *
     * <p>This replaces enumerating {@code beanDeclaredPropGetMethodPool.keySet()}, which a {@link ClassCache}
     * cannot do. Weak keys, and a shared {@link Boolean} as the value so nothing refers back to the key - the
     * entry therefore dies with the class, exactly as the caches it indexes do. Guarded by
     * {@link #METADATA_LOCK}, which is held by both writers ({@link #publishPropAccessors} and
     * {@link #invalidateBeanMetadata}) and the only reader.</p>
     */
    private static final Map<Class<?>, Boolean> introspectedClasses = new java.util.WeakHashMap<>();

    private static final Map<String, String> camelCasePropNamePool = new ConcurrentCacheMap<>(POOL_SIZE * 2);

    private static final Map<String, String> snakeCasePropNamePool = new ConcurrentCacheMap<>(POOL_SIZE * 2);

    private static final Map<String, String> screamingSnakeCasePropNamePool = new ConcurrentCacheMap<>(POOL_SIZE * 2);

    private static final Map<Class<?>, Boolean> registeredXmlBindingClassList = new ConcurrentCacheMap<>(POOL_SIZE);

    private static final Map<Class<?>, Set<String>> registeredNonPropGetSetMethodPool = new ConcurrentCacheMap<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> registeredPropGetMethodPool = new ConcurrentCacheMap<>(POOL_SIZE);

    private static final Map<Class<?>, Map<String, Method>> registeredPropSetMethodPool = new ConcurrentCacheMap<>(POOL_SIZE);

    private static final ClassCache<ImmutableList<String>> beanDeclaredPropNameListPool = new ClassCache<>();

    private static final ClassCache<ImmutableMap<String, Field>> beanDeclaredPropFieldPool = new ClassCache<>();

    private static final ClassCache<Map<String, Field>> beanPropFieldPool = new ClassCache<>();

    private static final ClassCache<Map<String, Field>> declaredFieldPool = new ClassCache<>();

    private static final ClassCache<ImmutableMap<String, Method>> beanDeclaredPropGetMethodPool = new ClassCache<>();

    private static final ClassCache<ImmutableMap<String, Method>> beanDeclaredPropSetMethodPool = new ClassCache<>();

    private static final ClassCache<Map<String, Method>> beanPropGetMethodPool = new ClassCache<>();

    private static final ClassCache<Map<String, Method>> beanPropSetMethodPool = new ClassCache<>();

    private static final ClassCache<Map<String, List<Method>>> beanInlinePropGetMethodPool = new ClassCache<>();

    //    /** The Constant beanInlinePropSetMethodPool. */
    //    private static final Map<Class<?>, Map<String, List<Method>>> beanInlinePropSetMethodPool = new ConcurrentCacheMap<>(POOL_SIZE);

    // Normalized property names and reflected method-name metadata.
    private static final Map<String, String> formalizedPropNamePool = new ConcurrentCacheMap<>(POOL_SIZE * 2);

    /**
     * Property name per accessor, grouped by declaring class.
     *
     * <p>Keyed by {@code Method} within a {@link ClassCache} rather than by {@code Method} directly: a
     * {@code Method} strongly references its declaring class, so a flat {@code static Map<Method, String>}
     * pinned that class - and its {@code ClassLoader} - forever. Nested this way the whole group dies with
     * the class. It cannot be keyed by method <i>name</i> alone: a builder can declare both {@code a()} and
     * {@code a(String)}, which resolve through different branches.
     *
     * <p>Never invalidated: the answer depends only on the class's own fields and method names, which no
     * registration can change. It is bounded by the class's own method count, so it needs no
     * {@link #MAX_CACHED_NAMES} cap.
     */
    private static final ClassCache<Map<Method, String>> methodPropNamePool = new ClassCache<>();

    // Java keywords mapped to legal property identifiers.
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

    private static final ClassCache<ImmutableSet<String>> beanDiffIgnoredPropNamesPool = new ClassCache<>();
    private static final ClassCache<BuilderInfo> builderMap = new ClassCache<>();

    /**
     * The bean class whose introspection published a builder class's setter pools - the reverse of
     * {@link #builderMap}'s {@code beanClass -> BuilderInfo}.
     *
     * <p>A canonical builder (private fields, fluent one-argument methods) is not a bean in its own right, so
     * scanning it on its own derives no properties at all: its setter model exists only because
     * {@link #publishPropAccessors} wrote it while introspecting the bean it builds. This index is what lets
     * {@link #getPropSetters(Class)} and {@link #getPropSetter(Class, String)} re-derive that model through the
     * owning bean once {@link #invalidateBeanMetadata(Class)} has dropped it, instead of publishing an empty
     * standalone scan in its place, and it is also how {@code getPropSetter} tells a published builder class
     * apart from a class that genuinely has no property model.</p>
     *
     * <p>Like the purely structural {@link #builderMap}, it is never invalidated - which bean a builder belongs
     * to follows from the two classes' shapes, and a registration cannot change it. Dropping it along with the
     * pools would remove exactly what the re-derivation needs.</p>
     */
    private static final ClassCache<Class<?>> builderOwnerMap = new ClassCache<>();

    private static final ClassCache<Boolean> beanClassPool = new ClassCache<>();

    private static final Map<Class<?>, Class<?>> registeredNonBeanClass = new ConcurrentCacheMap<>(POOL_SIZE);

    /**
     * Bumped by {@link #invalidateBeanMetadata(Class)} - i.e. by every registration API - so that a property
     * scan running concurrently can tell that the model it just derived is already stale.
     *
     * <p>{@link #loadPropGetSetMethodList(Class)} deliberately scans <i>outside</i> the
     * {@link #METADATA_LOCK} monitor, because the scan executes application code. That opens a
     * window in which a registration can land between the scan and its publication; comparing the epoch under
     * the monitor closes it by rescanning instead of publishing a pre-registration model.</p>
     */
    private static final java.util.concurrent.atomic.AtomicLong beanMetadataEpoch = new java.util.concurrent.atomic.AtomicLong();

    /**
     * How many times {@link #loadPropGetSetMethodList(Class)} will re-derive a property model that a
     * concurrent registration invalidated before it could be published, before falling back to scanning under
     * the metadata monitor. Only there to guarantee termination.
     */
    private static final int MAX_UNLOCKED_SCAN_ATTEMPTS = 3;

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

    private static final ClassCache<Boolean> recordClassPool = new ClassCache<>();

    /**
     * The point past which the name-conversion pools and the per-class negative lookup caches stop
     * accepting new entries.
     *
     * <p>These caches are keyed by caller-supplied strings, so without a cap they grow for the lifetime of
     * the JVM &mdash; deserializing documents with externally-controlled field names is enough to do it. The
     * cap matches the sizing hint the pools are constructed with, so the backing table never has to grow
     * past its declared size.</p>
     */
    private static final int MAX_CACHED_NAMES = POOL_SIZE * 2;

    /**
     * The longest caller-supplied spelling {@link #isPropName} will even consider matching.
     *
     * <p>No real JavaBean property name comes close, so anything longer is caller data rather than a property
     * name; refusing to match it keeps the fuzzy comparison (which strips underscores and accessor prefixes)
     * from being run over arbitrarily long strings. {@code ParserUtil.BeanInfo.isPropName} applies the same
     * limit - keep the two in sync.</p>
     */
    private static final int MAX_PROP_NAME_LENGTH = 128;

    /**
     * Memoizes {@code key -> value} only while {@code pool} is below {@link #MAX_CACHED_NAMES}.
     *
     * <p>Every one of these pools is a pure cache: skipping an insert costs a recomputation on the next
     * lookup and changes no result. See {@link #MAX_CACHED_NAMES} for why the cap is needed at all.</p>
     *
     * @param <V> the cached value type
     * @param pool the cache to insert into
     * @param key the cache key
     * @param value the value to memoize
     * @return {@code value}, so callers can {@code return cacheName(pool, key, compute())}
     */
    private static <V> V cacheName(final Map<String, V> pool, final String key, final V value) {
        // size() on a ConcurrentHashMap is a cheap sumCount(); a small overshoot under concurrency is
        // harmless, since the cap is about bounding growth rather than an exact limit.
        if (pool.size() < MAX_CACHED_NAMES) {
            pool.put(key, value);
        }

        return value;
    }

    /**
     * Memoizes the outcome of one caller-supplied property-name lookup - the resolved accessor, or the
     * not-found sentinel - so the next lookup for the same spelling is a map hit instead of a full accessor
     * rescan. The entry is only added while the per-class cache is below {@link #MAX_CACHED_NAMES}.
     *
     * <p>Both outcomes have to be capped, because both are keyed by the <i>caller's</i> spelling rather than by
     * the bean's property name, and neither is bounded by the bean. The miss side is obvious. The hit side is
     * not: {@link #isPropName} deliberately accepts an unbounded family of spellings for one property -
     * underscores are stripped, so {@code c_ity}, {@code ci__ty}, {@code _city_} and so on all resolve to
     * {@code city} - and each distinct spelling used to be cached forever.</p>
     *
     * <p>The canonical {@code propName -> accessor} entries seeded by {@link #loadPropGetSetMethodList} do not
     * go through here: their number <i>is</i> bounded by the bean, and they must never be crowded out.</p>
     *
     * @param <V> the cached accessor type ({@link Method} or {@link Field}), or its not-found sentinel
     * @param propMap the per-class accessor cache
     * @param propName the caller-supplied property name that was looked up
     * @param value the resolved accessor, or the cache's not-found sentinel
     */
    private static <V> void cachePropLookup(final Map<String, V> propMap, final String propName, final V value) {
        if (propMap.size() < MAX_CACHED_NAMES) {
            propMap.put(propName, value);
        }
    }

    /**
     * Checks if the specified class is a bean class.
     *
     * <p>A class is <b>not</b> a bean class if it has been passed to {@link #registerNonBeanClass(Class)};
     * that registration outranks every rule below. Otherwise a class is considered a bean class if it:</p>
     * <ul>
     *   <li>Is annotated with {@code @Entity} (including {@code javax.persistence.Entity} or {@code jakarta.persistence.Entity}), or</li>
     *   <li>Is a record class (Java 14+) or annotated with {@code @Record}, or</li>
     *   <li>Has at least one property (see {@link #getPropNameList(Class)} for what counts as one - a
     *       getter/setter pair, or simply a {@code public} non-{@code static} non-{@code final} field) and is not a
     *       {@link CharSequence}, {@link Number}, {@link Map}, {@link Collection}, or {@link Map.Entry} implementation</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.isBeanClass(User.class);      // returns true  (POJO with a getter/setter pair)
     * Beans.isBeanClass(Integer.class);   // returns false (Number subclass)
     * Beans.isBeanClass(String.class);    // returns false (CharSequence)
     * Beans.isBeanClass(null);            // returns false
     *
     * class Money { private long cents; public long getCents() { return cents; } public void setCents(long c) { cents = c; } }
     * Beans.isBeanClass(Money.class);     // returns true
     * Beans.registerNonBeanClass(Money.class);
     * Beans.isBeanClass(Money.class);     // returns false (explicitly registered as a non-bean)
     * }</pre>
     *
     * @param cls the class to be checked.
     * @return {@code true} if the specified class is a bean class, {@code false} otherwise.
     * @see #getPropNameList(Class)
     * @see #registerNonBeanClass(Class)
     */
    public static boolean isBeanClass(final Class<?> cls) {
        if (cls == null) {
            return false;
        }

        // The explicit non-bean registry outranks every other rule, including the @Entity/@Record
        // annotations. It used to be consulted only by the property scan, so registering an annotated
        // entity left this method answering `true` for a class whose getPropNameList() was now empty -
        // an inconsistent pair that made `if (isBeanClass(c)) { ...iterate props... }` silently do nothing.
        if (registeredNonBeanClass.containsKey(cls)) {
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
     * Builds the "this is not a bean class" failure for the {@code getProp*} lookups, naming the actual reason.
     *
     * <p>{@link #isBeanClass(Class)} answers {@code false} for several quite different situations, and the old
     * single message ("No property getter/setter method or public field found") only described one of them:
     * {@code String}, {@code Integer} and {@code ArrayList} all have accessors, they are simply excluded by
     * type, and an explicitly registered non-bean class may have had plenty. Reporting which rule rejected the
     * class turns an apparently wrong message into an actionable one.</p>
     *
     * @param cls the class that {@link #isBeanClass(Class)} rejected
     * @return the exception to throw
     */
    private static IllegalArgumentException newNotABeanClassException(final Class<?> cls) {
        final String reason;

        if (registeredNonBeanClass.containsKey(cls)) {
            reason = "it is registered as a non-bean class - see Beans.registerNonBeanClass(Class)";
        } else if (CharSequence.class.isAssignableFrom(cls)) {
            reason = "CharSequence implementations are never treated as beans";
        } else if (Number.class.isAssignableFrom(cls)) {
            reason = "Number implementations are never treated as beans";
        } else if (Map.Entry.class.isAssignableFrom(cls)) {
            reason = "Map.Entry implementations are never treated as beans";
        } else if (Map.class.isAssignableFrom(cls)) {
            reason = "Map implementations are never treated as beans";
        } else if (Collection.class.isAssignableFrom(cls)) {
            reason = "Collection implementations are never treated as beans";
        } else {
            reason = "no property getter/setter method or public field was found";
        }

        return new IllegalArgumentException("Not a bean class: " + ClassUtil.getCanonicalClassName(cls) + " - " + reason);
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
     * @param beanType the bean type to get bean information for; must not be {@code null}.
     * @return a {@link BeanInfo} instance containing metadata about the specified type.
     * @throws IllegalArgumentException if {@code beanType} is {@code null}, or the specified type is not a bean
     *         class (no property getter/setter method or public field found).
     * @see ParserUtil#getBeanInfo(Class)
     */
    public static BeanInfo getBeanInfo(final java.lang.reflect.Type beanType) throws IllegalArgumentException {
        N.checkArgNotNull(beanType, cs.beanType);

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
     * class Person {
     *     private final String name;
     *     private Person(String name) { this.name = name; }
     *     public static Builder builder() { return new Builder(); }
     *     static class Builder {
     *         private String name;
     *         public Builder name(String name) { this.name = name; return this; }
     *         public Person build() { return new Person(name); }
     *     }
     * }
     * class User {}
     *
     * // Person has a static builder() method returning a Builder with a build() method
     * Beans.BuilderInfo info = Beans.getBuilderInfo(Person.class);   // returns a non-null BuilderInfo
     * if (info != null) {
     *     Person.Builder builder = (Person.Builder) info.newBuilder();
     *     builder.name("Ada");
     *     Person instance = (Person) info.build(builder);
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

        if (!isBuildMethod(buildMethod, beanClass)) {
            try {
                buildMethod = builderClass.getDeclaredMethod("create");
            } catch (final Exception e) {
                // ignore
            }
        }

        return isBuildMethod(buildMethod, beanClass) ? buildMethod : null;
    }

    /**
     * Whether {@code method} is usable as a builder's terminal {@code build()}/{@code create()}.
     *
     * <p>It has to be an <i>instance</i> method: {@link #getBuilderInfo(Class)} invokes it on the populated
     * builder, and a {@code static} one would be called with that builder silently discarded, returning a bean
     * with none of the values the caller set. The mirror-image check on the builder factory
     * ({@link #getBuilderMethod(Class)}) has always required {@code static}; this end had no modifier check
     * at all beyond {@code public}.</p>
     *
     * @param method the candidate, or {@code null}
     * @param beanClass the type the builder must produce
     * @return {@code true} if {@code method} can be used to build {@code beanClass}
     */
    private static boolean isBuildMethod(final Method method, final Class<?> beanClass) {
        return method != null && method.getParameterCount() == 0 && Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers())
                && beanClass.isAssignableFrom(method.getReturnType());
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
     * <p>The registration is <b>inherited and absolute</b>: {@link #isBeanClass(Class)} answers {@code false}
     * for {@code cls} even when it is annotated {@code @Entity} or is a record, and the accessors {@code cls}
     * declares stop being properties of its subclasses too - a subclass keeps only what it declares itself.
     * That is how the built-in registrations ({@link java.util.Date}, {@link java.util.Calendar}, ...) keep a
     * bean that extends one of them from exposing the base type's accessors. Already-introspected classes are
     * re-derived, so the effect does not depend on whether they were introspected before or after this call.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Money { private long cents; public long getCents() { return cents; } public void setCents(long c) { cents = c; } }
     * class Price extends Money { private String currency;
     *                             public String getCurrency() { return currency; }
     *                             public void setCurrency(String c) { currency = c; } }
     *
     * Beans.isBeanClass(Money.class);        // returns true before registration
     * Beans.getPropNameList(Price.class);    // returns ["cents", "currency"]
     *
     * // From now on Money is treated as a simple value type during introspection.
     * Beans.registerNonBeanClass(Money.class);
     *
     * Beans.isBeanClass(Money.class);        // returns false
     * Beans.getPropNameList(Money.class);    // returns []
     * Beans.getPropNameList(Price.class);    // returns ["currency"] - "cents" is inherited from a non-bean
     *
     * Beans.registerNonBeanClass(Money.class);   // no exception thrown
     * }</pre>
     *
     * @param cls the class to be registered as a non-bean class; must not be {@code null}.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     * @see #isBeanClass(Class)
     */
    public static void registerNonBeanClass(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        applyRegistration(cls, () -> {
            registeredNonBeanClass.put(cls, cls);
            registeredXmlBindingClassList.put(cls, false);
        });
    }

    /**
     * Records a registration and drops every property model it could have changed - {@code cls}'s own, and
     * that of every already-introspected subtype of {@code cls}, because a registration against a base type
     * also governs the accessors its subtypes inherit.
     *
     * <p>All four registration APIs route through here. Two of them used to skip the subtype sweep entirely,
     * which made their effect depend on introspection order: registering a base type before any subtype was
     * introspected worked, registering it afterwards silently did not.</p>
     *
     * <p>The invalidation is deliberately <i>all</i> that happens under the monitor. The stale models are not
     * re-derived here: re-deriving runs application code (see {@link #loadPropGetSetMethodList(Class)}), and
     * the next lookup rebuilds them lazily with exactly the same result.</p>
     *
     * @param cls the class the registration is about
     * @param registration the pool update to perform; run while holding the metadata monitor
     */
    @SuppressWarnings("deprecation")
    private static void applyRegistration(final Class<?> cls, final Runnable registration) {
        final Set<Class<?>> classesToRefresh = N.newLinkedHashSet();

        synchronized (METADATA_LOCK) {
            registration.run();

            classesToRefresh.add(cls);

            // A registration for a base type also applies to already-introspected subtypes. The index is
            // copied because invalidateBeanMetadata below removes from it.
            for (final Class<?> cachedClass : new ArrayList<>(introspectedClasses.keySet())) {
                if (cls.isAssignableFrom(cachedClass)) {
                    classesToRefresh.add(cachedClass);
                }
            }

            for (final Class<?> classToRefresh : classesToRefresh) {
                invalidateBeanMetadata(classToRefresh);
            }
        }

        for (final Class<?> classToRefresh : classesToRefresh) {
            ParserUtil.refreshBeanPropInfo(classToRefresh);
        }
    }

    /**
     * Drops every piece of cached metadata derived from {@code cls}'s property model.
     *
     * <p>The registration APIs each used to clear their own hand-picked subset of the pools below, and the
     * subsets disagreed: {@code registerXmlBindingClass} and {@code registerNonBeanClass} left
     * {@code beanDeclaredPropNameListPool} and the {@code beanProp*MethodPool}s holding the pre-registration
     * property model, and none of them cleared {@code beanClassPool}, so {@link #isBeanClass(Class)} could keep
     * answering {@code true} for a class whose last property had just been excluded. Every registration now
     * routes through this one method so a class cannot be half-invalidated.</p>
     *
     * <p>{@code declaredFieldPool} is deliberately not cleared: it caches raw {@link Class#getDeclaredFields()}
     * output, which no registration can change.</p>
     *
     * <p>If {@code cls} has an already-detected builder, that builder class's metadata is dropped too, because
     * {@link #publishPropAccessors} writes part of it (the builder setter pools) as a side effect of
     * introspecting {@code cls}. Strictly, those pools are derived from {@code builderClass.getMethods()} and
     * {@link #getPropNameByMethod}, neither of which consults a registration, so a registration against
     * {@code cls} cannot by itself make them wrong - the drop is a deliberately conservative choice, and
     * {@code BeansRegressionBTest.testD6_registrationInvalidatesTheBuilderClassPoolsToo} pins it. <b>Do not
     * "simplify" it away.</b> What a caller sees afterwards is the re-derived builder model, and
     * {@link #builderOwnerMap} is what makes that re-derivation go back through {@code cls}: a standalone scan of
     * a canonical builder (private fields, fluent one-argument methods) derives no properties at all, so without
     * that index the drop would silently replace the builder's setters with an empty model. {@code builderMap} is
     * read without computing, because a builder that has never been detected cannot have populated those pools
     * either. The builder detection itself, and the owner index, are purely structural and are never
     * invalidated.</p>
     *
     * <p>The drop has to be <i>symmetric</i>, which is why the builder class goes through exactly the same
     * {@link #dropDerivedMetadata(Class)} as {@code cls} rather than through a hand-picked subset of it. Any
     * partial drop leaves two pools disagreeing, and the {@code getProp*} families do not all read the same one:
     * {@link #loadPropGetSetMethodList} decides "already introspected?" from {@code beanDeclaredPropGetMethodPool}
     * alone, so an entry surviving there stops the model from ever being republished - which is how
     * {@code getPropSetters(builderClass)} came to spin forever waiting for the setter pool that had been
     * dropped; and {@link #getPropGetter(Class, String)} answers from {@code beanPropGetMethodPool}, so an entry
     * surviving there is returned as-is and the re-derivation this method exists to force never happens.</p>
     *
     * <p>Callers must hold the {@link #METADATA_LOCK} monitor.</p>
     *
     * @param cls the class whose derived metadata is now stale
     */
    private static void invalidateBeanMetadata(final Class<?> cls) {
        // Every registration funnels through here, so this is the one place that has to advertise "the
        // property model changed" to a scan that is running concurrently - see beanMetadataEpoch.
        beanMetadataEpoch.incrementAndGet();

        final BuilderInfo builderInfo = builderMap.get(cls);

        // The cls == builderClass case needs no separate drop: the call below covers it.
        if (builderInfo != null && builderInfo.builderClass != null && !builderInfo.builderClass.equals(cls)) {
            dropDerivedMetadata(builderInfo.builderClass);
        }

        dropDerivedMetadata(cls);
    }

    /**
     * Removes every pool entry derived from one class's property model, so that no pool is left holding a value
     * that disagrees with another - see {@link #invalidateBeanMetadata(Class)}, the only caller, which applies
     * this to the bean and to its builder class alike.
     *
     * <p>{@code declaredFieldPool}, {@code methodPropNamePool}, {@code builderMap} and {@link #builderOwnerMap}
     * are deliberately not touched: each of them answers a question about the class's own shape, which no
     * registration changes.</p>
     *
     * <p>Callers must hold the {@link #METADATA_LOCK} monitor.</p>
     *
     * @param cls the class whose derived metadata is now stale
     */
    private static void dropDerivedMetadata(final Class<?> cls) {
        beanClassPool.remove(cls);
        beanDiffIgnoredPropNamesPool.remove(cls);
        beanDeclaredPropNameListPool.remove(cls);
        beanDeclaredPropFieldPool.remove(cls);
        beanDeclaredPropGetMethodPool.remove(cls);
        beanDeclaredPropSetMethodPool.remove(cls);
        beanPropFieldPool.remove(cls);
        beanPropGetMethodPool.remove(cls);
        beanPropSetMethodPool.remove(cls);
        beanInlinePropGetMethodPool.remove(cls);

        // Keep the subtype index in step with the caches it indexes: this class is no longer introspected,
        // so a later registration must not try to refresh it until it has been scanned again.
        introspectedClasses.remove(cls);
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
     * @throws IllegalArgumentException if {@code cls} is {@code null} or {@code propName} is empty.
     */
    public static void registerNonPropertyAccessor(final Class<?> cls, final String propName) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);
        N.checkArgNotEmpty(propName, cs.propName);

        applyRegistration(cls, () -> {
            synchronized (registeredNonPropGetSetMethodPool) {
                final Set<String> set = registeredNonPropGetSetMethodPool.computeIfAbsent(cls, k -> N.newHashSet());

                set.add(propName);
            }
        });
    }

    /**
     * Registers a getter or setter method as the property accessor for the specified property name.
     * The method must be recognized as a getter (starts with {@code get}, {@code is}, or {@code has},
     * or matches a field name) or a setter (starts with {@code set}, or matches a field name).
     * The registration applies to the method's declaring class and all assignable subclasses.
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
     * @throws IllegalArgumentException if {@code propName} is empty, {@code method} is {@code null}, the method is
     *         not a valid getter or setter, or if {@code propName} is already registered with a different method.
     */
    public static void registerPropertyAccessor(final String propName, final Method method) throws IllegalArgumentException {
        N.checkArgNotEmpty(propName, cs.propName);
        N.checkArgNotNull(method, cs.method);

        final Class<?> cls = method.getDeclaringClass();
        final boolean isGetter = isGetMethod(method);

        if (!isGetter && !isSetMethod(method)) {
            throw new IllegalArgumentException("The name of property getter/setter method must start with 'get/is/has' or 'set': " + method.getName());
        }

        // Warm the accessor pools before taking the metadata monitor: the conflict check below needs the
        // discovered accessors, and deriving them runs application code, which must not happen under the
        // global lock. The null fallback inside the registration is only for the case where a concurrent
        // registration invalidates this warm-up in between.
        loadPropGetSetMethodList(cls);

        applyRegistration(cls, () -> {
            final Map<Class<?>, Map<String, Method>> registeredPool = isGetter ? registeredPropGetMethodPool : registeredPropSetMethodPool;

            // The DECLARED pools, not beanPropGetMethodPool/beanPropSetMethodPool: those also memoize the
            // outcome of every tolerant getPropGetter/getPropSetter lookup under the caller's own spelling
            // (see cachePropLookup), so an earlier, side-effect-free lookup of this very alias would make the
            // conflict check fire where the same registration succeeds without it. Registering an alias must
            // not depend on whether somebody looked it up first.
            final Map<String, Method> propMethodMap = isGetter ? getPropGetters(cls) : getPropSetters(cls);

            checkPropertyAccessorConflict(propName, method, cls, propMethodMap, registeredPool);

            registeredPool.computeIfAbsent(cls, k -> new ConcurrentCacheMap<>(16)).put(propName, method);
        });
    }

    /**
     * @throws IllegalArgumentException if the property already has a conflicting registered accessor.
     */
    @SuppressWarnings("deprecation")
    private static void checkPropertyAccessorConflict(final String propName, final Method method, final Class<?> cls, final Map<String, Method> propMethodMap,
            final Map<Class<?>, Map<String, Method>> registeredMethodPool) throws IllegalArgumentException {
        final Map<String, Method> directlyRegisteredMethodMap = registeredMethodPool.get(cls);
        final Method directlyRegisteredMethod = directlyRegisteredMethodMap == null ? null : directlyRegisteredMethodMap.get(propName);

        if (directlyRegisteredMethod != null) {
            if (!method.equals(directlyRegisteredMethod)) {
                throw new IllegalArgumentException(propName + " has already been registered with different method: " + directlyRegisteredMethod.getName());
            }

            return;
        }

        final Method existingMethod = propMethodMap.get(propName);

        if (existingMethod != null && existingMethod != ClassUtil.SENTINEL_METHOD && !method.equals(existingMethod)
                && !hasInheritedPropertyAccessor(propName, cls, registeredMethodPool)) {
            throw new IllegalArgumentException(propName + " has already been registered with different method: " + existingMethod.getName());
        }
    }

    private static boolean hasInheritedPropertyAccessor(final String propName, final Class<?> cls,
            final Map<Class<?>, Map<String, Method>> registeredMethodPool) {
        for (final Map.Entry<Class<?>, Map<String, Method>> entry : registeredMethodPool.entrySet()) {
            if (entry.getKey() != cls && entry.getKey().isAssignableFrom(cls) && entry.getValue().containsKey(propName)) {
                return true;
            }
        }

        return false;
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
     * Beans.registerXmlBindingClass(JaxbBean.class);       // no exception thrown
     * }</pre>
     *
     * @param cls the class to be registered for XML binding; must not be {@code null}.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    public static void registerXmlBindingClass(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        // The map also holds FALSE entries (registerNonBeanClass, and demotion when a registered class turns
        // out not to be instantiable - which now happens lazily, the first time a JAXB getter is probed), so
        // only an existing TRUE registration may short-circuit.
        if (Boolean.TRUE.equals(registeredXmlBindingClassList.get(cls))) {
            return;
        }

        applyRegistration(cls, () -> registeredXmlBindingClassList.put(cls, true));
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
     * Beans.getPropNameByMethod(User.class.getMethod("getName"));             // returns "name"
     * Beans.getPropNameByMethod(User.class.getMethod("setAge", int.class));   // returns "age"
     * Beans.getPropNameByMethod(User.class.getMethod("getActive"));           // returns "active"
     * Beans.getPropNameByMethod(null);                                        // throws IllegalArgumentException
     * }</pre>
     *
     * @param getSetMethod the method whose property name is to be retrieved.
     * @return the property name associated with the specified method, or the raw method name itself
     *         if the method does not look like a getter/setter and no backing field matches.
     * @throws IllegalArgumentException if {@code getSetMethod} is {@code null}
     */
    public static String getPropNameByMethod(final Method getSetMethod) throws IllegalArgumentException {
        N.checkArgNotNull(getSetMethod, cs.getSetMethod);

        final Map<Method, String> namesByMethod = methodPropNamePool.computeIfAbsent(getSetMethod.getDeclaringClass(), k -> new ConcurrentCacheMap<>(16));
        String propName = namesByMethod.get(getSetMethod);

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

            namesByMethod.put(getSetMethod, propName);
        }

        return propName;
    }

    /**
     * Returns an immutable list of property names for the specified class.
     *
     * <p><b>What counts as a property.</b> This is <i>not</i> the {@link java.beans.Introspector} model, and
     * it is neither "has a getter" nor "has a getter and/or a setter". A member becomes a property when it is:</p>
     * <ul>
     *   <li>a getter ({@code getX}/{@code isX}/{@code hasX}, or a method named exactly like a field) that has a
     *       matching setter; or</li>
     *   <li>a field-backed getter on a class that has a builder or is immutable (no no-arg constructor but
     *       a public all-args one); or</li>
     *   <li>a getter on a class annotated {@code @Entity} or on a record, including a computed getter with
     *       no backing field; or a JAXB-style {@link Collection}/{@link Map} getter enabled via
     *       {@link #registerXmlBindingClass(Class)}, which likewise need not have a backing field; or</li>
     *   <li>a {@code public}, non-{@code static}, non-{@code final} field, with or without any accessor.</li>
     * </ul>
     *
     * <p>Consequently a setter with no getter, and a getter with no setter on an ordinary mutable class
     * (including a computed getter with no backing field), are <b>excluded</b>; a bare public field is
     * <b>included</b>. {@link #registerPropertyAccessor(String, Method)} and
     * {@link #registerNonPropertyAccessor(Class, String)} adjust the result.</p>
     *
     * <p><b>Order.</b> Every field-backed property comes first - superclasses first, then the class itself, in
     * backing-field declaration order within each level - followed by every property discovered from an accessor
     * alone, across all levels. The two groups are not interleaved: for a subclass whose superclass has both a
     * field-backed and an accessor-only property, the subclass's field-backed property still precedes the
     * superclass's accessor-only one. That trailing group is ordered by {@link Class#getMethods()}, whose order
     * the JLS does not specify - do not depend on it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class User {
     *     private String name;
     *     private int age;
     *     private Boolean active;
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public int getAge() { return age; }
     *     public void setAge(int age) { this.age = age; }
     *     public Boolean getActive() { return active; }
     *     public void setActive(Boolean active) { this.active = active; }
     * }
     * Beans.getPropNameList(User.class);   // returns ["name", "age", "active"] (field-declaration order)
     * Beans.getPropNameList(null);         // throws IllegalArgumentException
     *
     * class Mixed {
     *     public String tag;                                          // public field, no accessors
     *     private String name;
     *     private String secret;
     *     private String label;
     *     public String getName() { return name; }                    // getter + setter
     *     public void setName(String name) { this.name = name; }
     *     public void setSecret(String secret) { this.secret = secret; }   // setter only
     *     public String getLabel() { return label; }                       // getter only
     *     public String getComputed() { return name + label; }             // no field, no setter
     * }
     * Beans.getPropNameList(Mixed.class);   // returns ["tag", "name"]
     * }</pre>
     *
     * @param cls the class whose property names are to be retrieved; must not be {@code null}.
     * @return an immutable list of property names for the specified class; empty if it has no properties.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     * @see #isBeanClass(Class)
     * @see #getPropGetters(Class)
     * @see #getPropFields(Class)
     */
    public static ImmutableList<String> getPropNameList(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        ImmutableList<String> propNameList = beanDeclaredPropNameListPool.get(cls);

        // `while`, not `if`: a registration on another thread can invalidate the model between the load and
        // the re-read, and handing back a null pool is not something any caller of this family checks for.
        while (propNameList == null) {
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
     * class User {
     *     private String name;
     *     private int age;
     *     private Boolean active;
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public int getAge() { return age; }
     *     public void setAge(int age) { this.age = age; }
     *     public Boolean getActive() { return active; }
     *     public void setActive(Boolean active) { this.active = active; }
     * }
     * Beans.getPropNames(User.class, Arrays.asList("age"));             // returns ["name", "active"]
     * Beans.getPropNames(User.class, Arrays.asList("age", "active"));   // returns ["name"]
     * Beans.getPropNames(User.class, (Collection<String>) null);        // returns ["name", "age", "active"]
     * Beans.getPropNames((Class<?>) null, Arrays.asList("age"));        // throws IllegalArgumentException
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
    public static List<String> getPropNames(final Class<?> cls, final Collection<String> propNameToExclude) throws IllegalArgumentException {
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
     * class User {
     *     private String name;
     *     private int age;
     *     private Boolean active;
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public int getAge() { return age; }
     *     public void setAge(int age) { this.age = age; }
     *     public Boolean getActive() { return active; }
     *     public void setActive(Boolean active) { this.active = active; }
     * }
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
    public static List<String> getPropNames(final Class<?> cls, final Set<String> propNameToExclude) throws IllegalArgumentException {
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
     *        (an {@code IllegalArgumentException} is thrown otherwise).
     * @param ignoreNullValue if {@code true}, properties with {@code null} values are excluded from the result.
     * @return a mutable list of property names of the given bean object;
     *         properties with {@code null} values are excluded when {@code ignoreNullValue} is {@code true}.
     * @see #getPropNameList(Class)
     * @see #getPropNames(Object, Predicate)
     * @throws IllegalArgumentException if {@code bean} is {@code null}
     */
    public static List<String> getPropNames(final Object bean, final boolean ignoreNullValue) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);

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
     *        (an {@code IllegalArgumentException} is thrown otherwise).
     * @param propNameFilter the predicate to filter property names.
     * @return a list of property names for the specified bean, filtered by the given predicate.
     * @throws IllegalArgumentException if {@code bean} is {@code null}, or if {@code propNameFilter} is {@code null}.
     */
    public static List<String> getPropNames(final Object bean, final Predicate<? super String> propNameFilter) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);
        N.checkArgNotNull(propNameFilter, cs.propNameFilter);

        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        final List<String> result = new ArrayList<>(beanInfo.propInfoList.size());

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
     *        (an {@code IllegalArgumentException} is thrown otherwise).
     * @param propNameValueFilter the bi-predicate to filter property names and values, where the first parameter is the property name and the second parameter is the property value.
     * @return a list of property names for the specified bean, filtered by the given bi-predicate.
     * @throws IllegalArgumentException if {@code bean} is {@code null}, or if {@code propNameValueFilter} is {@code null}.
     */
    public static List<String> getPropNames(final Object bean, final BiPredicate<? super String, Object> propNameValueFilter) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);
        N.checkArgNotNull(propNameValueFilter, cs.propNameValueFilter);

        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        final List<String> result = new ArrayList<>(beanInfo.propInfoList.size());

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
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public Date getLastModified() { return lastModified; }
     *     public void setLastModified(Date lastModified) { this.lastModified = lastModified; }
     * }
     *
     * Beans.getIgnoredPropNamesForDiff(User.class);      // returns ["lastModified"]
     * Beans.getIgnoredPropNamesForDiff(Address.class);   // returns [] (no @DiffIgnore properties)
     * }</pre>
     *
     * @param cls the class for which the diff-ignored property names are to be retrieved; must be a bean class.
     * @return an immutable set of property names excluded from diff operations; never {@code null}.
     * @throws IllegalArgumentException if {@code cls} is {@code null}, or is not a bean class - the message
     *         names the rule that rejected it (see {@link #isBeanClass(Class)}).
     * @see com.landawn.abacus.util.Difference.MapDifference
     * @see com.landawn.abacus.util.Difference.BeanDifference#of(Object, Object)
     */
    public static ImmutableSet<String> getIgnoredPropNamesForDiff(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        ImmutableSet<String> propNames = beanDiffIgnoredPropNamesPool.get(cls);

        if (propNames == null) {
            if (!isBeanClass(cls)) {
                // Checked here so the failure names the rule that rejected the class, exactly as the
                // getProp* family does. Falling through to ParserUtil.getBeanInfo produced the older,
                // uniform "no property getter/setter method or public field found" text, which is simply
                // wrong for a String or an ArrayList - they have plenty, and are excluded by type.
                throw newNotABeanClassException(cls);
            }

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

    // javax/jakarta persistence are optional at runtime. Referencing javax.persistence.Entity.class inline
    // threw a NoClassDefFoundError whenever the dependency was absent - once per annotation, per call, and
    // annotatedWithEntity is called from inside the per-field and per-method loops of
    // loadPropGetSetMethodList. Resolving each name once here turns that into a single lookup at class-init.
    private static final Class<? extends Annotation> JAVAX_ENTITY_ANNOTATION = loadAnnotationClass("javax.persistence.Entity");

    private static final Class<? extends Annotation> JAKARTA_ENTITY_ANNOTATION = loadAnnotationClass("jakarta.persistence.Entity");

    @SuppressWarnings("unchecked")
    private static Class<? extends Annotation> loadAnnotationClass(final String className) {
        try {
            return (Class<? extends Annotation>) Class.forName(className);
        } catch (final Throwable e) { // NoClassDefFoundError/LinkageError as well as ClassNotFoundException
            return null;
        }
    }

    private static boolean annotatedWithEntity(final Class<?> cls) {
        return cls.getAnnotation(Entity.class) != null //
                || (JAVAX_ENTITY_ANNOTATION != null && cls.getAnnotation(JAVAX_ENTITY_ANNOTATION) != null)
                || (JAKARTA_ENTITY_ANNOTATION != null && cls.getAnnotation(JAKARTA_ENTITY_ANNOTATION) != null);
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

    /**
     * Whether {@code method} was declared by a class that {@link #registerNonBeanClass(Class)} excluded.
     *
     * <p>The hierarchy walk in {@link #loadPropGetSetMethodList} skips a registered level outright, which is
     * enough for that level's <i>declared fields</i> - but not for its accessors: the per-level scan reads
     * {@code clazz.getMethods()}, which also returns every <i>inherited</i> public method, so a subclass level
     * happily re-discovered everything the registered superclass declared. {@code class MyDate extends
     * java.util.Date} therefore exposed {@code year}, {@code time}, {@code seconds}, {@code month},
     * {@code hours}, {@code minutes} and {@code date} as read/write properties, even though
     * {@code java.util.Date} is one of this class's own built-in registrations. Skipping a registered
     * <i>declaring</i> class makes the exclusion actually inherit.</p>
     *
     * @param method the accessor being considered
     * @return {@code true} if the method's declaring class is registered as a non-bean class
     */
    private static boolean isDeclaredByNonBeanClass(final Method method) {
        return registeredNonBeanClass.containsKey(method.getDeclaringClass());
    }

    private static boolean isGetMethod(final Method method) {
        if (Object.class.equals(method.getDeclaringClass()) || Modifier.isStatic(method.getModifiers())) {
            return false;
        }

        final String mn = method.getName();

        return (mn.startsWith(GET) || mn.startsWith(IS) || mn.startsWith(HAS) || getDeclaredField(method.getDeclaringClass(), mn) != null)
                && (N.isEmpty(method.getParameterTypes())) && !void.class.equals(method.getReturnType()) && !nonGetSetMethodName.contains(mn);
    }

    private static boolean isJAXBGetMethod(final Class<?> cls, final LazyInstance instance, final Method method, final Field field) {
        try {
            // Cheap, instance-free checks first. Only a class that clears all of them is worth constructing:
            // instance.get() is what actually runs the target's constructor.
            if (!(Collection.class.isAssignableFrom(method.getReturnType()) || Map.class.isAssignableFrom(method.getReturnType()))) {
                return false;
            }

            if (!(registeredXmlBindingClassList.getOrDefault(cls, false) || N.anyMatch(cls.getAnnotations(), Beans::isXmlTypeAnno)
                    || N.anyMatch(method.getAnnotations(), Beans::isXmlElementAnno)
                    || (field != null && N.anyMatch(field.getAnnotations(), Beans::isXmlElementAnno)))) {
                return false;
            }

            final Object bean = instance.get();

            return bean != null && ClassUtil.invokeMethod(bean, method) != null;
        } catch (final Throwable e) { // NOSONAR - Error as well: this constructs the bean and invokes a user getter
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

    /**
     * Decides whether {@code inputPropName} is an acceptable spelling of the property whose canonical name is
     * {@code propNameByMethod} (case-insensitive, underscores ignored, {@code get}/{@code set}/{@code is}/{@code has}
     * prefix and {@code SimpleClass.prop} qualification allowed).
     *
     * <p>Names longer than {@link #MAX_PROP_NAME_LENGTH} simply do not match. This used to throw
     * {@link IllegalArgumentException}, which turned every tolerant lookup into a hard failure: the throw
     * propagated out of {@code getPropGetter} through {@code ParserUtil.BeanInfo.getPropInfo(String)} and so out
     * of {@code getPropValue(bean, name, /*ignoreUnmatchedProperty*&#47; true)} and
     * {@code mapToBean(map, /*ignoreUnmatchedProperty*&#47; true, type)} - modes whose whole contract is to ignore
     * names the bean does not have. An over-long name is simply "no such property".</p>
     *
     * <p>{@code ParserUtil.BeanInfo.isPropName} is a copy of this method (the two live in different packages and
     * neither can see the other's private members). Keep the two in sync; they diverged on exactly this length
     * check before.</p>
     *
     * @param cls the class the property is being looked up on
     * @param inputPropName the caller-supplied spelling
     * @param propNameByMethod the bean's canonical property name
     * @return {@code true} if the two name the same property
     */
    private static boolean isPropName(final Class<?> cls, String inputPropName, final String propNameByMethod) {
        // Trim before measuring: the cap is about the length of the *name*, and applying it to the raw input
        // made a short but whitespace-padded spelling fail to match for no reason a caller could see.
        inputPropName = inputPropName.trim();

        if (inputPropName.length() > MAX_PROP_NAME_LENGTH) {
            return false;
        }

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
     * The outcome of one property scan: everything {@link #scanPropAccessors(Class)} derived from a class's
     * shape, before any registration has been applied to it and before anything has been published.
     *
     * @param builderClass the detected builder class, or {@code null} if the bean has no builder
     * @param propFieldMap property name to backing field, in field-declaration order
     * @param propGetMethodMap property name to getter
     * @param propSetMethodMap property name to setter
     */
    private record PropScan(Class<?> builderClass, Map<String, Field> propFieldMap, Map<String, Method> propGetMethodMap,
            Map<String, Method> propSetMethodMap) {
    }

    /**
     * Loads the property getter and setter method list for the specified class.
     *
     * <p>This is the single entry point every {@code getProp*} lookup funnels through when its pool has no
     * entry for {@code cls}, so it is also where {@code null} is rejected: guarding here gives the whole
     * family the {@link IllegalArgumentException} the class contract promises, instead of the
     * {@link NullPointerException} that used to come out of the superclass walk.</p>
     *
     * <p><b>Why this is two phases.</b> Discovery unavoidably runs <i>application</i> code: reading a class's
     * {@code public static final String} constants triggers its static initializer, and a JAXB-style
     * collection getter can only be recognised by constructing the bean and invoking the getter. Doing that
     * while holding the one global metadata monitor ({@link #METADATA_LOCK}) meant a single bean whose
     * {@code <clinit>} or constructor waited on another thread froze introspection of <i>every other class</i>
     * process-wide. The scan therefore normally runs unlocked and only publication takes the monitor.
     * Repeated invalidation by registrations eventually triggers a scan under the monitor to ensure progress.
     * Two threads may scan the same class concurrently; one then discards its derived metadata.</p>
     *
     * @param cls the class to load property getter and setter methods for
     * @throws IllegalArgumentException if {@code cls} is {@code null}
     */
    private static void loadPropGetSetMethodList(final Class<?> cls) throws IllegalArgumentException {
        N.checkArgNotNull(cls, cs.cls);

        for (int attempt = 0;; attempt++) {
            if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                return;
            }

            if (attempt >= MAX_UNLOCKED_SCAN_ATTEMPTS) {
                // Registrations kept invalidating the model faster than we could publish it. Scan under the
                // monitor instead: that reintroduces running application code under the lock, but it cannot
                // be raced, so this always terminates. Unreachable in practice - registrations happen at
                // startup, not in a loop.
                synchronized (METADATA_LOCK) {
                    if (!beanDeclaredPropGetMethodPool.containsKey(cls)) {
                        publishPropAccessors(cls, scanPropAccessors(cls));
                    }
                }

                return;
            }

            // Phase 1: derive the property model. Unlocked - see above.
            final long epoch = beanMetadataEpoch.get();
            final PropScan scan = scanPropAccessors(cls);

            // Phase 2: apply the registrations and publish, atomically with respect to them.
            synchronized (METADATA_LOCK) {
                if (beanDeclaredPropGetMethodPool.containsKey(cls)) {
                    return;
                }

                if (beanMetadataEpoch.get() != epoch) {
                    // A registration landed while we were scanning, so `scan` describes the old model.
                    // Nothing has been published or mutated yet, so simply scan again.
                    continue;
                }

                publishPropAccessors(cls, scan);

                return;
            }
        }
    }

    /**
     * The public methods of one hierarchy level, minus any declared by a class that
     * {@link #registerNonBeanClass(Class)} excluded.
     *
     * <p>{@link Class#getMethods()} copies its result array on every call, so this is memoized per level and
     * shared between the immutability pre-scan and the main scan rather than recomputed by each.</p>
     *
     * @param clazz the hierarchy level being scanned
     * @return the accessor candidates for {@code clazz}
     */
    private static List<Method> accessorCandidates(final Class<?> clazz) {
        final Method[] declared = clazz.getMethods();
        final List<Method> methods = new ArrayList<>(declared.length);

        // getMethods() returns inherited public methods too, so skipping a registered level in the caller is
        // not enough to honour registerNonBeanClass - the filter has to be by DECLARING class. Applied once
        // here rather than inside the O(fields x methods) pairing loop.
        for (final Method method : declared) {
            if (!isDeclaredByNonBeanClass(method)) {
                methods.add(method);
            }
        }

        return methods;
    }

    /**
     * Pairs each field {@code clazz} declares with the getter that backs it, in field-declaration order.
     *
     * <p>A field with no matching getter is kept only when it is a {@code public} non-{@code static}
     * non-{@code final} field - a bare public field is a property in its own right. When several methods
     * match one field the longest name wins, so an explicit {@code getFoo()} beats a bare {@code foo()}.</p>
     *
     * @param clazz the hierarchy level being scanned
     * @param methods {@code clazz.getMethods()}, already filtered of accessors declared by a registered
     *        non-bean class
     * @return {@code (field, getter-or-null)} pairs in declaration order
     */
    private static List<Tuple2<Field, Method>> pairFieldsWithGetters(final Class<?> clazz, final List<Method> methods) {
        final Field[] declaredFields = clazz.getDeclaredFields();
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

        return fieldGetMethodList;
    }

    /**
     * Answers whether {@code cls} is an <i>immutable</i> bean: one that has no no-arg constructor but does
     * have a {@code public} constructor taking exactly its field-backed getters, in hierarchy order. Such a
     * class has no setters by design, so its getters are still properties.
     *
     * <p>Two constructor signatures are tried, in order:</p>
     * <ol>
     *   <li>every field-backed getter in the hierarchy, superclass-first and in declaration order within each
     *       level - the order a hand-written {@code Sub(a, b)} constructor that chains {@code super(a)} uses;</li>
     *   <li>only {@code cls}'s own declared fields.</li>
     * </ol>
     *
     * <p>Form 2 is what this used to check, and it is kept so that nothing previously classified as immutable
     * stops being so - a class that mixes an inherited mutable property with its own final ones still matches
     * it. Form 1 is the addition: without it the lookup could only ever succeed for a class with no bean
     * superclass, so a normal immutable subclass silently ended up with <i>zero</i> properties.</p>
     *
     * <p>The match is on the <b>exact ordered parameter types</b>, never on the parameter count: an
     * arity-only test would classify any mutable class that happens to declare a same-arity constructor as
     * immutable, and silently expose its getter-only methods as properties.</p>
     *
     * <p>Levels registered via {@link #registerNonBeanClass(Class)} are skipped, exactly as the main scan
     * skips them, so their fields never enter the constructor signature.</p>
     *
     * @param cls the class being introspected
     * @param allClasses {@code cls} followed by its superclasses, as collected by the caller
     * @param methodsByLevel per-level accessor-candidate cache, populated here and reused by the main scan
     * @param fieldGetMethodsByLevel per-level pairing cache, populated here and reused by the main scan
     * @return {@code true} if {@code cls} is an immutable bean
     */
    private static boolean isImmutableBeanClass(final Class<?> cls, final List<Class<?>> allClasses, final Map<Class<?>, List<Method>> methodsByLevel,
            final Map<Class<?>, List<Tuple2<Field, Method>>> fieldGetMethodsByLevel) {
        if (registeredNonBeanClass.containsKey(cls)) {
            // The main scan skips cls's own level entirely, so cls contributes no properties and the question
            // does not arise. Answering it from the superclass levels alone could flip a registered non-bean
            // class to "immutable" and start exposing its inherited getter-only methods - which the previous
            // implementation, deriving this from cls's own fields inside the skipped level, never did.
            return false;
        }

        // Keyed by field name so a subclass field that shadows a superclass one contributes a single
        // constructor parameter. The superclass entry wins, matching the main scan, whose
        // `propGetMethodMap.containsKey(propName)` guard keeps the first (superclass-first) property seen.
        final Map<String, Class<?>> hierarchyArgTypes = new LinkedHashMap<>();
        final List<Class<?>> ownArgTypes = new ArrayList<>();

        for (int i = allClasses.size() - 1; i >= 0; i--) {
            final Class<?> clazz = allClasses.get(i);

            if (registeredNonBeanClass.containsKey(clazz)) {
                continue;
            }

            final List<Method> methods = methodsByLevel.computeIfAbsent(clazz, Beans::accessorCandidates);

            for (final Tuple2<Field, Method> tp : fieldGetMethodsByLevel.computeIfAbsent(clazz, k -> pairFieldsWithGetters(k, methods))) {
                if (tp._2 != null) {
                    hierarchyArgTypes.putIfAbsent(tp._1.getName(), tp._1.getType());

                    if (clazz == cls) {
                        ownArgTypes.add(tp._1.getType());
                    }
                }
            }
        }

        return hasPublicConstructor(cls, hierarchyArgTypes.values().toArray(new Class<?>[0])) //
                || hasPublicConstructor(cls, ownArgTypes.toArray(new Class<?>[0]));
    }

    /** Whether {@code cls} declares a {@code public} constructor with exactly {@code argTypes}, in that order. */
    private static boolean hasPublicConstructor(final Class<?> cls, final Class<?>[] argTypes) {
        if (argTypes.length == 0) {
            // A no-arg constructor cannot mark a class immutable - the caller only gets here when there is none.
            return false;
        }

        final Constructor<?> constructor = ClassUtil.getDeclaredConstructor(cls, argTypes);

        return constructor != null && Modifier.isPublic(constructor.getModifiers());
    }

    /**
     * Derives {@code cls}'s property model from its shape alone - no registration is applied and nothing is
     * published. Normally runs without the {@link #METADATA_LOCK} monitor; see
     * {@link #loadPropGetSetMethodList(Class)} for why.
     *
     * @param cls the class to scan
     * @return the derived model
     */
    @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT")
    private static PropScan scanPropAccessors(final Class<?> cls) {
        // Introspection must not construct the class merely to read its metadata: newInstance() runs an
        // arbitrary user constructor, which in real beans opens files, registers listeners or starts
        // threads - and every isBeanClass/getPropNameList call used to trigger it. The instance exists
        // only so isJAXBGetMethod can see whether a JAXB-style collection getter returns non-null, so it
        // is created on demand and at most once.
        final LazyInstance instance = new LazyInstance(cls);

        final List<Class<?>> allClasses = new ArrayList<>();
        allClasses.add(cls);
        Class<?> superClass = null;

        while ((superClass = allClasses.get(allClasses.size() - 1).getSuperclass()) != null && !superClass.equals(Object.class)) {
            allClasses.add(superClass);
        }

        final BuilderInfo builderInfo = getBuilderInfo(cls);
        final Class<?> builderClass = builderInfo == null ? null : builderInfo.builderClass();
        // Constant for the whole scan - hoisted out of the per-field and per-method loops below.
        final boolean isEntityClass = annotatedWithEntity(cls);

        final Map<String, Field> propFieldMap = new LinkedHashMap<>();
        final Map<String, Method> propGetMethodMap = new LinkedHashMap<>();
        final Map<String, Method> propSetMethodMap = new LinkedHashMap<>();

        // Mirrors propGetMethodMap's values so "has this getter already been claimed under another property
        // name?" is O(1). It used to be propGetMethodMap.containsValue(method), a linear scan run once per
        // method of every hierarchy level - O(methods x properties) per level on a wide bean.
        // A HashSet, not an identity set: Class.getMethods() hands out a fresh Method copy on every call and
        // this loop calls it once per level, so the same inherited getter is a different instance at each
        // level and only Method.equals - which is what containsValue used - recognises it.
        final Set<Method> claimedGetMethods = new HashSet<>();

        Class<?> clazz = null;
        Method setMethod = null;

        final Constructor<?> noArgConstructor = ClassUtil.getDeclaredConstructor(cls);

        // Per-level scan state, computed once and shared between the immutability pre-scan and the main loop
        // below. Both are plain HashMaps: they are local to this call and never escape it.
        final Map<Class<?>, List<Method>> methodsByLevel = new HashMap<>();
        final Map<Class<?>, List<Tuple2<Field, Method>>> fieldGetMethodsByLevel = new HashMap<>();

        // Immutability is a property of the WHOLE hierarchy, so it has to be settled before the first level
        // is processed. It used to be derived inside the loop, from `cls`'s own declared fields only and only
        // once the loop reached `cls` (the last level). Both halves were wrong: a normal immutable subclass
        // takes its superclass's fields in its constructor too, so the constructor lookup never matched and
        // the class silently ended up with ZERO properties; and the superclass levels, which run first, saw
        // isImmutable == false and dropped their own getter-only properties.
        final boolean isImmutable = noArgConstructor == null && isImmutableBeanClass(cls, allClasses, methodsByLevel, fieldGetMethodsByLevel);

        // The accessor candidates of the class being introspected, i.e. its whole public API including
        // inherited methods. Setter pairing is done against these rather than against the level currently
        // being walked, so a getter and its setter may be declared by different classes - see getSetMethod.
        final List<Method> beanCandidates = methodsByLevel.computeIfAbsent(cls, Beans::accessorCandidates);

        for (int i = allClasses.size() - 1; i >= 0; i--) {
            clazz = allClasses.get(i);

            if (registeredNonBeanClass.containsKey(clazz)) {
                continue;
            }

            final Map<String, String> staticFinalFields = getPublicStaticStringFields(clazz);

            // Memoized: when the immutability pre-scan ran it already built both of these for this level,
            // and each is expensive (getMethods() copies its array; the pairing is O(fields x methods)).
            final List<Method> methods = methodsByLevel.computeIfAbsent(clazz, Beans::accessorCandidates);
            final List<Tuple2<Field, Method>> fieldGetMethodList = fieldGetMethodsByLevel.computeIfAbsent(clazz, k -> pairFieldsWithGetters(k, methods));

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

                        setMethod = getSetMethod(cls, beanCandidates, method);

                        if (setMethod != null) {
                            //ClassUtil.setAccessibleQuietly(field, true);
                            ClassUtil.setAccessibleQuietly(method, true);
                            ClassUtil.setAccessibleQuietly(setMethod, true);

                            propFieldMap.put(propName, field);
                            propGetMethodMap.put(propName, method);
                            claimedGetMethods.add(method);
                            propSetMethodMap.put(propName, setMethod);

                            continue;
                        }

                        // isJAXBGetMethod last: it is the only disjunct that can construct `cls` and invoke
                        // a user getter, and the four cheap structural checks decide the same way.
                        if (isEntityClass || Beans.isRecordClass(clazz) || builderClass != null || isImmutable
                                || isJAXBGetMethod(cls, instance, method, field)) {
                            //ClassUtil.setAccessibleQuietly(field, true);
                            ClassUtil.setAccessibleQuietly(method, true);

                            propFieldMap.put(propName, field);
                            propGetMethodMap.put(propName, method);
                            claimedGetMethods.add(method);

                            //NOSONAR
                        }
                    } else if (Modifier.isPublic(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
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

                    setMethod = getSetMethod(cls, beanCandidates, method);

                    if (setMethod != null && !claimedGetMethods.contains(method)) {
                        ClassUtil.setAccessibleQuietly(method, true);
                        ClassUtil.setAccessibleQuietly(setMethod, true);

                        propGetMethodMap.put(propName, method);
                        claimedGetMethods.add(method);
                        propSetMethodMap.put(propName, setMethod);

                        continue;
                    }

                    // isJAXBGetMethod last - see the field loop above.
                    if ((isEntityClass || Beans.isRecordClass(clazz) || isJAXBGetMethod(cls, instance, method, null)) && !claimedGetMethods.contains(method)) {
                        ClassUtil.setAccessibleQuietly(method, true);

                        propGetMethodMap.put(propName, method);
                        claimedGetMethods.add(method);

                        //NOSONAR
                    }
                }
            }
        }

        return new PropScan(builderClass, propFieldMap, propGetMethodMap, propSetMethodMap);
    }

    /**
     * Applies the registration overrides to a completed {@link #scanPropAccessors(Class) scan} and publishes
     * it into the per-class pools.
     *
     * <p>Callers must hold the {@link #METADATA_LOCK} monitor: the registration pools are read
     * here, so this half has to be atomic with respect to the registration APIs. It runs no application code.</p>
     *
     * @param cls the class being published
     * @param scan the derived property model
     */
    private static void publishPropAccessors(final Class<?> cls, final PropScan scan) {
        final Class<?> builderClass = scan.builderClass();
        final Map<String, Field> propFieldMap = scan.propFieldMap();
        final Map<String, Method> propGetMethodMap = scan.propGetMethodMap();
        final Map<String, Method> propSetMethodMap = scan.propSetMethodMap();

        applyRegisteredPropertyAccessors(cls, propGetMethodMap, registeredPropGetMethodPool);
        applyRegisteredPropertyAccessors(cls, propSetMethodMap, registeredPropSetMethodPool);

        synchronized (registeredNonPropGetSetMethodPool) {
            for (final Map.Entry<Class<?>, Set<String>> entry : registeredNonPropGetSetMethodPool.entrySet()) { //NOSONAR
                if (entry.getKey().isAssignableFrom(cls)) {
                    final Set<String> set = entry.getValue();
                    final Set<String> propertyNames = new LinkedHashSet<>(propFieldMap.keySet());
                    propertyNames.addAll(propGetMethodMap.keySet());
                    propertyNames.addAll(propSetMethodMap.keySet());

                    for (final String nonPropName : set) {
                        for (final String propName : propertyNames) {
                            if (propName.equalsIgnoreCase(nonPropName)) {
                                propFieldMap.remove(propName);
                                propGetMethodMap.remove(propName);
                                propSetMethodMap.remove(propName);
                            }
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
        final Map<String, Field> tempFieldMap = new ConcurrentCacheMap<>(N.max(64, propFieldMap.size()));
        tempFieldMap.putAll(propFieldMap);
        beanPropFieldPool.put(cls, tempFieldMap);

        final ImmutableMap<String, Method> unmodifiableGetMethodMap = ImmutableMap.wrap(propGetMethodMap);
        //noinspection ResultOfMethodCallIgnored
        unmodifiableGetMethodMap.keySet(); // initialize? //NOSONAR

        // Replace, never merge into whatever is there. Nothing can publish getters for `cls` before this
        // point - the caller holds METADATA_LOCK and has just checked that cls has not been introspected -
        // so the old "merge if present" branch was unreachable for getters, and had it ever run it would
        // have carried SENTINEL_METHOD negative-lookup entries across an invalidation.
        final Map<String, Method> newGetMethodMap = new ConcurrentCacheMap<>(N.max(64, propGetMethodMap.size()));
        newGetMethodMap.putAll(propGetMethodMap);
        beanPropGetMethodPool.put(cls, newGetMethodMap);

        final Map<String, Method> existingDeclaredSetters = beanDeclaredPropSetMethodPool.get(cls);

        if (existingDeclaredSetters != null) {
            for (final Map.Entry<String, Method> entry : existingDeclaredSetters.entrySet()) {
                propSetMethodMap.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        // for Double-Checked Locking is Broke to initialize it before put it into map.
        final ImmutableMap<String, Method> unmodifiableSetMethodMap = ImmutableMap.wrap(propSetMethodMap);
        //noinspection ResultOfMethodCallIgnored
        unmodifiableSetMethodMap.keySet(); // initialize? //NOSONAR
        beanDeclaredPropSetMethodPool.put(cls, unmodifiableSetMethodMap);

        final Map<String, Method> newSetMethodMap = new ConcurrentCacheMap<>(N.max(64, propSetMethodMap.size()));
        newSetMethodMap.putAll(propSetMethodMap);
        beanPropSetMethodPool.put(cls, newSetMethodMap);

        // LinkedHashSet keeps field-then-getter order while making membership O(1)
        // (the previous ArrayList.contains() merge was O(n²) for large beans).
        final Set<String> propNameSet = new LinkedHashSet<>(propFieldMap.keySet());
        propNameSet.addAll(propGetMethodMap.keySet());
        final List<String> propNameList = new ArrayList<>(propNameSet);

        beanDeclaredPropNameListPool.put(cls, ImmutableList.wrap(propNameList));

        if (builderClass != null) {
            String propName = null;

            final Map<String, Method> builderPropSetMethodMap = new LinkedHashMap<>();
            final Method[] builderMethods = builderClass.getMethods();

            for (final Method method : builderMethods) {
                // !isStatic: a builder setter is invoked on the builder instance, so a static factory such
                // as `public static Builder of(String)` is not one - it would be called with the builder
                // discarded.
                if (Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && !Object.class.equals(method.getDeclaringClass())
                        && method.getParameterCount() == 1
                        && (void.class.equals(method.getReturnType()) || method.getReturnType().isAssignableFrom(builderClass))) {
                    propName = getPropNameByMethod(method);
                    builderPropSetMethodMap.put(propName, method);
                }
            }

            // Mirror of the union above, for the other introspection order: if the builder class has already
            // been introspected in its own right, its own setter model is still valid and must not be
            // discarded here. Without this, whether the builder keeps its own setters depended on whether
            // the bean or the builder was introspected first. The builder-derived names win a shared key,
            // because those are the names mapToBean/copyAs drive the builder by.
            final Map<String, Method> existingBuilderSetters = beanDeclaredPropSetMethodPool.get(builderClass);

            if (existingBuilderSetters != null) {
                for (final Map.Entry<String, Method> entry : existingBuilderSetters.entrySet()) {
                    builderPropSetMethodMap.putIfAbsent(entry.getKey(), entry.getValue());
                }
            }

            final ImmutableMap<String, Method> unmodifiableBuilderPropSetMethodMap = ImmutableMap.wrap(builderPropSetMethodMap);
            //noinspection ResultOfMethodCallIgnored
            unmodifiableBuilderPropSetMethodMap.keySet(); // initialize? //NOSONAR
            beanDeclaredPropSetMethodPool.put(builderClass, unmodifiableBuilderPropSetMethodMap);

            final Map<String, Method> tmp = new ConcurrentCacheMap<>(N.max(64, builderPropSetMethodMap.size()));
            tmp.putAll(builderPropSetMethodMap);
            beanPropSetMethodPool.put(builderClass, tmp);

            // Remember which bean the two pools just written came from: this is the only place that knows it, and
            // it is what lets a later lookup on the builder class re-derive them from here rather than from a
            // standalone scan of the builder. See builderOwnerMap.
            builderOwnerMap.put(builderClass, cls);
        }

        // The caller holds METADATA_LOCK, which also guards this index.
        introspectedClasses.put(cls, Boolean.TRUE);

        // LAST, deliberately. beanDeclaredPropGetMethodPool is the key every entry point tests to decide
        // "already introspected?", and loadPropGetSetMethodList now tests it *without* the monitor. If it
        // were published before its siblings, a reader could take that shortcut and then dereference a
        // pool this method had not filled in yet. Writing it last also gives the reader the
        // happens-before edge for all of the writes above (the volatile write inside ClassCache.put).
        beanDeclaredPropGetMethodPool.put(cls, unmodifiableGetMethodMap);
    }

    /**
     * Creates - on demand, at most once, and only when some caller actually needs it - a throwaway instance of
     * the class currently being introspected.
     *
     * <p>Not thread-safe by design, and it does not need to be: each {@link #scanPropAccessors(Class)} call
     * creates its own instance and never publishes it, so the instance is confined to one thread for its whole
     * life. It does not rely on the {@link #METADATA_LOCK} monitor: scans normally run outside it;
     * see {@link #loadPropGetSetMethodList(Class)}.</p>
     */
    private static final class LazyInstance {
        private final Class<?> cls;

        private boolean resolved = false;

        private Object instance = null;

        LazyInstance(final Class<?> cls) {
            this.cls = cls;
        }

        Object get() {
            if (resolved) {
                return instance;
            }

            resolved = true;

            if (registeredNonBeanClass.containsKey(cls)) {
                return null;
            }

            try {
                instance = cls.getDeclaredConstructor().newInstance();
            } catch (final Throwable e) { // NOSONAR - Error as well: newInstance() runs <clinit> and a user constructor
                if (logger.isDebugEnabled() && !(Strings.isNotEmpty(cls.getPackageName()) && cls.getPackageName().startsWith("java."))) {
                    logger.debug(e, "Unable to instantiate {} while discovering property accessors; getter-based setter checks will be skipped", cls.getName());
                }

                if (registeredXmlBindingClassList.containsKey(cls)) {
                    registeredXmlBindingClassList.put(cls, false);
                }
            }

            return instance;
        }
    }

    /**
     * Overlays the accessors registered via {@link #registerPropertyAccessor(String, Method)} onto a freshly
     * scanned property model.
     *
     * <p>{@link #registerNonBeanClass(Class)} outranks this: a registration against a class that has since
     * been declared a non-bean is ignored, and so is one inherited from a non-bean base. Without that rule the
     * overlay put back what the scan had just excluded, so {@code registerNonBeanClass} was not the "absolute"
     * exclusion it documents - {@code getPropNameList} still answered {@code [amount]} for a class whose
     * {@code isBeanClass} was {@code false}, and its subclasses inherited that phantom property while
     * {@code beanToMap} on the same instance threw "not a bean class".</p>
     *
     * @param cls the class being published
     * @param propMethodMap the scanned model to overlay, modified in place
     * @param registeredMethodPool the getter or setter registration pool
     * @throws IllegalArgumentException if inherited registered accessors for the same property are ambiguous.
     */
    private static void applyRegisteredPropertyAccessors(final Class<?> cls, final Map<String, Method> propMethodMap,
            final Map<Class<?>, Map<String, Method>> registeredMethodPool) throws IllegalArgumentException {
        if (registeredNonBeanClass.containsKey(cls)) {
            return;
        }

        final List<Map.Entry<Class<?>, Map<String, Method>>> applicableRegistrations = new ArrayList<>();
        final Set<String> registeredPropNames = new LinkedHashSet<>();

        for (final Map.Entry<Class<?>, Map<String, Method>> entry : registeredMethodPool.entrySet()) {
            // A registration inherited from a non-bean base is excluded for the same reason the scan skips
            // that base's own accessors: a subclass keeps only what it declares itself.
            if (entry.getKey().isAssignableFrom(cls) && !registeredNonBeanClass.containsKey(entry.getKey())) {
                applicableRegistrations.add(entry);
                registeredPropNames.addAll(entry.getValue().keySet());
            }
        }

        for (final String propName : registeredPropNames) {
            Method selectedMethod = null;

            for (final Map.Entry<Class<?>, Map<String, Method>> candidateEntry : applicableRegistrations) {
                final Method candidateMethod = candidateEntry.getValue().get(propName);

                if (candidateMethod == null || isShadowedPropertyAccessor(propName, candidateEntry.getKey(), applicableRegistrations)) {
                    continue;
                }

                if (selectedMethod != null && !areCompatiblePropertyAccessors(selectedMethod, candidateMethod)) {
                    throw new IllegalArgumentException("Ambiguous registered property accessor for property '" + propName + "' in class "
                            + ClassUtil.getCanonicalClassName(cls) + ": " + selectedMethod + " and " + candidateMethod);
                }

                if (selectedMethod == null || candidateMethod.getDeclaringClass().getName().compareTo(selectedMethod.getDeclaringClass().getName()) < 0) {
                    selectedMethod = candidateMethod;
                }
            }

            if (selectedMethod != null) {
                propMethodMap.put(propName, selectedMethod);
            }
        }
    }

    private static boolean isShadowedPropertyAccessor(final String propName, final Class<?> candidateClass,
            final List<Map.Entry<Class<?>, Map<String, Method>>> applicableRegistrations) {
        for (final Map.Entry<Class<?>, Map<String, Method>> otherEntry : applicableRegistrations) {
            final Class<?> otherClass = otherEntry.getKey();

            if (candidateClass != otherClass && candidateClass.isAssignableFrom(otherClass) && otherEntry.getValue().containsKey(propName)) {
                return true;
            }
        }

        return false;
    }

    private static boolean areCompatiblePropertyAccessors(final Method left, final Method right) {
        return left.getName().equals(right.getName()) && left.getReturnType().equals(right.getReturnType())
                && Arrays.equals(left.getParameterTypes(), right.getParameterTypes());
    }

    /**
     * Finds the setter that pairs with {@code getMethod} for the bean class being introspected.
     *
     * <p>The search covers the <b>whole hierarchy</b>, not just the getter's own declaring class. It used to
     * look only at {@code getMethod.getDeclaringClass()}'s declared methods, which silently required the
     * getter and the setter to be declared by the same class - so an ordinary read-only base with a mutating
     * subclass ({@code Base.getName()} + {@code Sub.setName(String)}), and its mirror image, produced no
     * property at all: {@code name} was absent from {@link #getPropNameList(Class)}, and {@code beanToMap},
     * {@code copy}, {@code mergeInto} and {@code mapToBean} all dropped it without a word.</p>
     *
     * <p>{@code candidates} is {@code beanClass}'s accessor candidates - {@link Class#getMethods()}, which
     * already includes inherited public methods, minus anything declared by a registered non-bean class. It
     * is searched first, and the per-class {@code lookupDeclaredMethod} probes are kept as a fallback because
     * they also see <i>non-public</i> declared setters, which this class has always accepted and
     * {@code getMethods()} does not report.</p>
     *
     * <p>Note that {@code candidates} belongs to the class being introspected, not to the hierarchy level
     * currently being walked. That is deliberate and is what makes the pairing work: the property model is
     * built for {@code beanClass}, so the setter may legitimately live anywhere in {@code beanClass}'s public
     * API. Introspecting the base type on its own still sees only its own methods, so a getter-only base
     * class stays getter-only.</p>
     *
     * @param beanClass the class being introspected
     * @param candidates {@code beanClass}'s accessor candidates
     * @param getMethod the getter to pair
     * @return the matching setter, or {@code null} if there is none
     */
    private static Method getSetMethod(final Class<?> beanClass, final List<Method> candidates, final Method getMethod) {
        final Class<?> declaringClass = getMethod.getDeclaringClass();
        final String getMethodName = getMethod.getName();
        final Class<?> propType = getMethod.getReturnType();

        final String setMethodName = SET
                + (getMethodName.substring(getMethodName.startsWith(IS) ? 2 : ((getMethodName.startsWith(HAS) || getMethodName.startsWith(GET)) ? 3 : 0)));

        Method setMethod = resolveSetter(beanClass, declaringClass, candidates, setMethodName, propType);

        // A getter named exactly like its field (`name()` rather than `getName()`) pairs with a setter of the
        // same name (`name(String)`).
        if (setMethod == null && getDeclaredField(declaringClass, getMethodName) != null) {
            setMethod = resolveSetter(beanClass, declaringClass, candidates, getMethodName, propType);
        }

        return ((setMethod != null)
                && (void.class.equals(setMethod.getReturnType()) || setMethod.getReturnType().isAssignableFrom(setMethod.getDeclaringClass()))) ? setMethod
                        : null;
    }

    /**
     * Resolves a setter named {@code setMethodName} taking {@code propType}, in decreasing order of
     * confidence:
     * <ol>
     *   <li>an exact name match among {@code candidates} - the bean's whole public API, inherited included;</li>
     *   <li>a declared method of the getter's own class, then of the bean class - these also see
     *       <i>non-public</i> setters, which this class has always accepted and {@link Class#getMethods()}
     *       does not report;</li>
     *   <li>last, a case-insensitive match among {@code candidates}, mirroring the case-insensitive fallback
     *       {@link ClassUtil#lookupDeclaredMethod(Class, String, Class...)} already applies within one class.</li>
     * </ol>
     *
     * <p>The case-insensitive sweep runs <b>after</b> the declared lookups on purpose: running it with the
     * exact sweep would let an inherited {@code setname} shadow a declared {@code setName}.</p>
     */
    private static Method resolveSetter(final Class<?> beanClass, final Class<?> declaringClass, final List<Method> candidates, final String setMethodName,
            final Class<?> propType) {
        Method setMethod = findAccessibleSetter(candidates, setMethodName, propType, true);

        if (setMethod == null) {
            setMethod = ClassUtil.lookupDeclaredMethod(declaringClass, setMethodName, propType);
        }

        if (setMethod == null && !beanClass.equals(declaringClass)) {
            setMethod = ClassUtil.lookupDeclaredMethod(beanClass, setMethodName, propType);
        }

        return setMethod != null ? setMethod : findAccessibleSetter(candidates, setMethodName, propType, false);
    }

    /**
     * Finds a one-argument, non-{@code static} method named {@code setMethodName} taking {@code propType}
     * among {@code candidates}.
     *
     * <p>{@code static} is excluded here: a static {@code setX(String)} would be invoked with the bean
     * discarded, and {@link Class#getMethods()} reports inherited statics that no other source did, so
     * without this filter widening the search would have started matching them.
     * <br><b>This does not make a static setter unreachable</b>: the declared-method fallbacks in
     * {@link #resolveSetter} do not filter by modifier, so a {@code static setX} declared in the getter's
     * own class is still paired with it, exactly as before. That is pre-existing behaviour and is left
     * alone deliberately - changing it is an API decision, not part of widening the search.</p>
     *
     * @param exactName {@code true} to require an exact name match, {@code false} to match ignoring case
     */
    private static Method findAccessibleSetter(final List<Method> candidates, final String setMethodName, final Class<?> propType, final boolean exactName) {
        for (final Method method : candidates) {
            if (Modifier.isStatic(method.getModifiers()) || method.getParameterCount() != 1 || !propType.equals(method.getParameterTypes()[0])) {
                continue;
            }

            if (exactName ? method.getName().equals(setMethodName) : method.getName().equalsIgnoreCase(setMethodName)) {
                return method;
            }
        }

        return null;
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
        // computeIfAbsent locks only this class's slot, where the old double-checked block locked the whole
        // pool for every class in the JVM.
        final Map<String, Field> fieldMap = declaredFieldPool.computeIfAbsent(cls, k -> {
            Field[] fields = null;

            try {
                fields = k.getDeclaredFields();
            } catch (final SecurityException e) {
                // ignore
            }

            final Map<String, Field> m = new ConcurrentCacheMap<>(fields == null ? 0 : N.max(16, fields.length));

            if (fields != null) {
                for (final Field field : fields) {
                    m.put(field.getName(), field);
                }
            }

            return m;
        });

        return fieldMap.get(fieldName);
    }

    /**
     * Collects the values of {@code cls}'s {@code public static final String} fields into a
     * {@code value -> value} map.
     *
     * <p>The identity mapping is deliberate and this is <i>not</i> a no-op lookup table: the caller uses it as
     * {@code propName = staticFinalFields.get(propName) != null ? staticFinalFields.get(propName) : propName}
     * to canonicalize a derived property name onto the exact {@code String} instance held by the class's own
     * constant, so that {@code ==} comparisons against that constant succeed. Do not "simplify" it away.</p>
     *
     * <p><b>This initializes {@code cls}.</b> {@link Field#get(Object)} on a static field runs the declaring
     * class's static initializer, so introspecting a class runs its {@code <clinit>} - which is application
     * code that can throw. An {@link ExceptionInInitializerError} (and the {@link NoClassDefFoundError} every
     * later access then raises) is an {@link Error}, not an {@link Exception}, so it used to escape straight
     * out of {@link #isBeanClass(Class)} - a method documented to answer {@code true}/{@code false} and never
     * to throw. A class whose initializer fails simply contributes no constants.</p>
     *
     * @param cls the class to scan
     * @return a map whose keys and values are both the constants' values; empty if there are none, or if the
     *         class could not be initialized
     */
    private static Map<String, String> getPublicStaticStringFields(final Class<?> cls) {
        final Map<String, String> staticFinalFields = new HashMap<>();

        for (final Field field : cls.getFields()) {
            if (Modifier.isPublic(field.getModifiers()) && Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers())
                    && String.class.equals(field.getType())) {
                String value;

                try {
                    value = (String) field.get(null);
                    staticFinalFields.put(value, value);
                } catch (final Throwable e) { // NOSONAR - Error as well: reading a static field runs <clinit>
                    if (logger.isDebugEnabled()) {
                        logger.debug(e, "Unable to read the constant {}.{} while discovering property accessors; it will not be used to canonicalize "
                                + "property names", cls.getName(), field.getName());
                    }
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
     * @param cls the class from which the field is to be retrieved; must not be {@code null}.
     * @param propName the name of the property whose backing field is to be retrieved. A name longer than 128
     *        characters never matches a property (it is treated as "not found", not as an error).
     * @return the field associated with the specified property name, or {@code null} if no matching field is found and the class is a bean class.
     * @throws IllegalArgumentException if {@code cls} or {@code propName} is {@code null}, or if no matching field is found and the
     *         specified class is not a bean class.
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Field getPropField(final Class<?> cls, final String propName) throws IllegalArgumentException {
        N.checkArgNotNull(propName, cs.propName);

        Map<String, Field> propFieldMap = beanPropFieldPool.get(cls);

        while (propFieldMap == null) { // `while`: see getPropNameList(Class)
            Beans.loadPropGetSetMethodList(cls);
            propFieldMap = beanPropFieldPool.get(cls);
        }

        Field field = propFieldMap.get(propName);

        if (field == null) {
            if (!Beans.isBeanClass(cls)) {
                throw newNotABeanClassException(cls);
            }

            synchronized (METADATA_LOCK) {
                // Scan the FIELD map, not the getter map. Scanning the getters meant an alias could only ever
                // resolve for a property that happens to have one, so a property backed solely by a public
                // field (`public String user_name;`) matched its exact spelling and nothing else - while the
                // sibling getPropGetter/getPropSetter resolved aliases for their own maps. The old loop also
                // stopped at the first *getter* whose name matched even when that name had no field, so a
                // later, field-backed match was never reached.
                for (final Map.Entry<String, Field> entry : getPropFields(cls).entrySet()) {
                    if (Beans.isPropName(cls, propName, entry.getKey())) {
                        field = entry.getValue();

                        break;
                    }
                }

                if ((field == null) && !propName.equalsIgnoreCase(Beans.normalizePropName(propName))) {
                    field = getPropField(cls, Beans.normalizePropName(propName));
                }

                // Cache the outcome - hit or miss - so the same spelling is not re-scanned next time. Both
                // go through cachePropLookup: the key is the caller's spelling, which is unbounded either way.
                if (field == null) {
                    field = ClassUtil.SENTINEL_FIELD;
                }

                //    ClassUtil.setAccessibleQuietly(field, true);
                cachePropLookup(propFieldMap, propName, field);
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
     * fields.containsKey("name");     // returns true
     * fields.get("name").getType();   // returns class java.lang.String
     * fields.get("nonExistent");      // returns null
     * }</pre>
     *
     * @param cls the class whose property fields are to be retrieved; must not be {@code null}.
     * @return an immutable map of property name to backing {@link Field} for the specified class; never {@code null}.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    public static ImmutableMap<String, Field> getPropFields(final Class<?> cls) throws IllegalArgumentException {
        ImmutableMap<String, Field> getterMethodList = beanDeclaredPropFieldPool.get(cls);

        while (getterMethodList == null) { // `while`: see getPropNameList(Class)
            Beans.loadPropGetSetMethodList(cls);
            getterMethodList = beanDeclaredPropFieldPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property get method available on the specified {@code cls}, including inherited methods,
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
     * @param cls the class from which the property get method is to be retrieved; must not be {@code null}.
     * @param propName the name of the property whose get method is to be retrieved. A name longer than 128
     *        characters never matches a property (it is treated as "not found", not as an error).
     * @return the property get method available on the specified class, or {@code null} if no matching method
     *         is found and the class is a bean class.
     * @throws IllegalArgumentException if {@code cls} or {@code propName} is {@code null}, or if no matching method is found and the
     *         specified class is not a bean class.
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Method getPropGetter(final Class<?> cls, final String propName) throws IllegalArgumentException {
        N.checkArgNotNull(propName, cs.propName);

        Map<String, Method> propGetMethodMap = beanPropGetMethodPool.get(cls);

        while (propGetMethodMap == null) { // `while`: see getPropNameList(Class)
            Beans.loadPropGetSetMethodList(cls);
            propGetMethodMap = beanPropGetMethodPool.get(cls);
        }

        Method method = propGetMethodMap.get(propName);

        if (method == null) {
            if (!Beans.isBeanClass(cls)) {
                throw newNotABeanClassException(cls);
            }

            synchronized (METADATA_LOCK) {
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

                // Cache the outcome - hit or miss - so the same spelling is not re-scanned next time. Both
                // go through cachePropLookup: the key is the caller's spelling, which is unbounded either way.
                if (method == null) {
                    method = ClassUtil.SENTINEL_METHOD;
                }

                cachePropLookup(propGetMethodMap, propName, method);
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
     * @param cls the class from which the property getter methods are to be retrieved; must not be {@code null}.
     * @return an immutable map of property name to getter {@link Method} for the specified class; never {@code null}.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    public static ImmutableMap<String, Method> getPropGetters(final Class<?> cls) throws IllegalArgumentException {
        ImmutableMap<String, Method> getterMethodList = beanDeclaredPropGetMethodPool.get(cls);

        while (getterMethodList == null) { // `while`: see getPropNameList(Class)
            Beans.loadPropGetSetMethodList(cls);
            getterMethodList = beanDeclaredPropGetMethodPool.get(cls);
        }

        return getterMethodList;
    }

    /**
     * Returns the property set method available on the specified {@code cls}, including inherited methods,
     * with the specified property name {@code propName}.
     * {@code null} is returned if no matching method is found and {@code cls} is a bean class.
     *
     * <p>Call {@link #registerXmlBindingClass(Class)} first to retrieve the property
     * getter/setter method for a class/bean generated according to the JAXB specification.</p>
     *
     * <p>A bean's <i>builder</i> class is accepted too, although it is not a bean class in its own right:
     * introspecting a bean that has a builder also publishes that builder's fluent one-argument setters, and
     * this method resolves them, with the same tolerant name matching. For a property of the bean that the
     * builder does not expose it returns {@code null}, exactly as it does for an unknown property of a bean
     * class. It is the bean's introspection that makes the builder recognisable here, so a builder class
     * looked up before its bean has ever been introspected is still rejected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.getPropSetter(User.class, "name").getName();   // returns "setName"
     * Beans.getPropSetter(User.class, "NAME").getName();   // returns "setName" (case-insensitive)
     * Beans.getPropSetter(User.class, "nonExistent");      // returns null (User is a bean class)
     * }</pre>
     *
     * @param cls the class from which the property set method is to be retrieved; must not be {@code null}.
     * @param propName the name of the property whose set method is to be retrieved. A name longer than 128
     *        characters never matches a property (it is treated as "not found", not as an error).
     * @return the property set method available on the specified class, or {@code null} if no matching method
     *         is found and the class is a bean class, or the builder class of an already-introspected one.
     * @throws IllegalArgumentException if {@code cls} or {@code propName} is {@code null}, or if no matching method is found and the
     *         specified class is neither a bean class nor the builder class of an already-introspected one.
     */
    @MayReturnNull
    @SuppressWarnings("deprecation")
    public static Method getPropSetter(final Class<?> cls, final String propName) throws IllegalArgumentException {
        N.checkArgNotNull(propName, cs.propName);

        Map<String, Method> propSetMethodMap = beanPropSetMethodPool.get(cls);

        if (propSetMethodMap == null) {
            loadBuilderSettersThroughOwner(cls); // see getPropSetters(Class)
            propSetMethodMap = beanPropSetMethodPool.get(cls);
        }

        while (propSetMethodMap == null) { // `while`: see getPropNameList(Class)
            Beans.loadPropGetSetMethodList(cls);
            propSetMethodMap = beanPropSetMethodPool.get(cls);
        }

        Method method = propSetMethodMap.get(propName);

        if (method == null) {
            // A published builder class is not a bean class in its own right - no getters, no public fields -
            // yet publishPropAccessors fills its setter pools while introspecting the bean it builds, and
            // ParserUtil drives the builder through this method for every property of that bean, including the
            // ones the builder does not expose. builderOwnerMap names exactly those classes, so a class that
            // genuinely has no property model is still rejected.
            if (!Beans.isBeanClass(cls) && builderOwnerMap.get(cls) == null) {
                throw newNotABeanClassException(cls);
            }

            synchronized (METADATA_LOCK) {
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

                // Cache the outcome - hit or miss - so the same spelling is not re-scanned next time. Both
                // go through cachePropLookup: the key is the caller's spelling, which is unbounded either way.
                if (method == null) {
                    method = ClassUtil.SENTINEL_METHOD;
                }

                cachePropLookup(propSetMethodMap, propName, method);
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
     * <p>A bean's <i>builder</i> class is accepted too: introspecting a bean that has a builder also publishes
     * that builder's fluent one-argument setters under this pool. That model is re-derived through the bean the
     * builder builds, so once the bean has been introspected the answer no longer depends on which of the two
     * classes was introspected first, nor on whether a registration has invalidated it in between. A builder
     * class whose bean has never been introspected has no such model at all: a standalone scan of a canonical
     * builder derives no properties, so the map is empty.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Method> setters = Beans.getPropSetters(User.class);
     * setters.get("name").getName();   // returns "setName"
     * setters.containsKey("age");      // returns true
     * setters.get("nonExistent");      // returns null
     * }</pre>
     *
     * @param cls the class from which the property setter methods are to be retrieved; must not be {@code null}.
     * @return an immutable map of property name to setter {@link Method} for the specified class; never {@code null}.
     * @throws IllegalArgumentException if {@code cls} is {@code null}.
     */
    public static ImmutableMap<String, Method> getPropSetters(final Class<?> cls) throws IllegalArgumentException {
        ImmutableMap<String, Method> setterMethodList = beanDeclaredPropSetMethodPool.get(cls);

        if (setterMethodList == null) {
            loadBuilderSettersThroughOwner(cls);
            setterMethodList = beanDeclaredPropSetMethodPool.get(cls);
        }

        while (setterMethodList == null) { // `while`: see getPropNameList(Class)
            Beans.loadPropGetSetMethodList(cls);
            setterMethodList = beanDeclaredPropSetMethodPool.get(cls);
        }

        return setterMethodList;
    }

    /**
     * Republishes a builder class's setter model by introspecting the bean it builds, when that model is missing
     * - which is the state {@link #invalidateBeanMetadata(Class)} leaves behind for the builder of a bean a
     * registration touched.
     *
     * <p>Only the owning bean can derive it: a canonical builder has private fields and fluent one-argument
     * methods, so a standalone scan of it derives no properties at all and would publish an empty setter model
     * that then answers every later lookup. This is deliberately attempted once, and the callers fall through to
     * their own re-derivation loop afterwards, so termination never depends on the owner actually republishing
     * anything - it does not when the builder was invalidated in its own right, with the bean still
     * introspected.</p>
     *
     * @param cls the class being looked up, which may or may not be a known builder class
     */
    private static void loadBuilderSettersThroughOwner(final Class<?> cls) {
        final Class<?> ownerBeanClass = builderOwnerMap.get(cls);

        if (ownerBeanClass != null && !ownerBeanClass.equals(cls)) {
            loadPropGetSetMethodList(ownerBeanClass);
        }
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
     * @throws IllegalArgumentException if {@code propGetMethod} is {@code null}, an instance method receives an
     *         incompatible {@code bean}, or the method requires arguments.
     * @throws NullPointerException if {@code bean} is {@code null} and {@code propGetMethod} is an instance method.
     * @throws RuntimeException if access to the getter is denied or the invoked getter throws an exception.
     */
    @SuppressWarnings("unchecked")
    @MayReturnNull
    public static <T> T getPropValue(final Object bean, final Method propGetMethod) throws IllegalArgumentException, NullPointerException, RuntimeException {
        N.checkArgNotNull(propGetMethod, cs.propGetMethod);

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
     * String name = Beans.getPropValue(user, "name");   // returns "John"
     * Integer age = Beans.getPropValue(user, "age");    // returns 25
     * Beans.getPropValue(user, "nonExistent");          // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the property value.
     * @param bean the object from which the property value is to be retrieved; must not be {@code null}.
     * @param propName the name of the property whose value is to be retrieved.
     * @return the value of the specified property; may be {@code null}.
     * @throws IllegalArgumentException if {@code bean} is {@code null}, or if no property with the given name is found.
     * @see #getPropValue(Object, Method)
     * @see #getPropValue(Object, String, boolean)
     */
    @MayReturnNull
    public static <T> T getPropValue(final Object bean, final String propName) throws IllegalArgumentException {
        return getPropValue(bean, propName, false);
    }

    /**
     * Resolves a dot-separated property path (e.g. {@code "address.city"}) to the chain of getters that walks
     * it, memoized per class and per caller-supplied spelling.
     *
     * <p>Only reached for a name {@code BeanInfo} could not resolve as a single property. Each segment is
     * resolved against the <i>declared</i> return type of the previous one, so the chain is a function of the
     * bean class rather than of the data. An unresolvable path - a non-bean intermediate, or a segment with no
     * getter - yields an empty chain rather than an exception, leaving each caller free to decide whether that
     * is "not found and ignored" or an error.</p>
     *
     * <p>{@link #getPropValue(Object, String, boolean)} and {@link #getPropValueIfPresent(Object, String)} used
     * to carry a verbatim copy of this each; the two are the only callers and must stay in agreement about
     * which paths resolve.</p>
     *
     * @param cls the bean class the path starts from
     * @param propName the caller-supplied, possibly dot-separated property name
     * @return the getters to invoke in order, or an empty list if the path does not resolve; never {@code null}
     */
    private static List<Method> resolveInlinePropGetMethods(final Class<?> cls, final String propName) {
        Map<String, List<Method>> inlinePropGetMethodMap = beanInlinePropGetMethodPool.get(cls);
        List<Method> inlinePropGetMethodQueue = null;

        if (inlinePropGetMethodMap == null) {
            inlinePropGetMethodMap = new ConcurrentCacheMap<>(getPropNameList(cls).size());
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

            // Capped for the same reason the accessor caches are (see cachePropLookup): this branch is
            // reached only for names BeanInfo could not resolve, so the key is an unresolved, caller-supplied
            // name and an empty queue - a miss - is the common entry. Skipping the insert only costs a
            // re-split on the next lookup.
            cachePropLookup(inlinePropGetMethodMap, propName, inlinePropGetMethodQueue);
        }

        return inlinePropGetMethodQueue;
    }

    /**
     * Returns the value of the specified property by invoking the getter method associated with the given property name on the provided bean.
     * If the property cannot be found and ignoreUnmatchedProperty is {@code true}, it returns {@code null}.
     *
     * <p>This method also supports nested properties using dot notation.</p>
     *
     * <p>For nested paths, if an intermediate property resolves to {@code null} the leaf is unreachable and this
     * method returns the default value of the final getter's return type (e.g.&nbsp;{@code 0} for primitive numeric
     * types, {@code false} for {@code boolean}); this is non-{@code null} only when the final getter's return type
     * is a primitive type &mdash; for a wrapper or reference leaf type the default is {@code null}. That keeps a
     * primitive-typed leaf assignable without unboxing a {@code null}, but it means the result cannot be told apart
     * from a property that genuinely holds the default: use {@link #getPropValueIfPresent(Object, String)}, which
     * returns {@link com.landawn.abacus.util.u.Nullable#empty()} for an unreachable path, when that distinction
     * matters.</p>
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
     * // Unreachable nested path (address is null) -> the leaf type's default, here null for String
     * Beans.getPropValue(new Order(), "address.city", false);   // returns null
     *
     * // Non-existent property with ignore flag
     * Beans.getPropValue(order, "unknown", true);    // returns null
     * Beans.getPropValue(order, "unknown", false);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the property value.
     * @param bean the object from which the property value is to be retrieved; must not be {@code null}.
     * @param propName the name of the property whose value is to be retrieved; supports dot notation for nested properties.
     * @param ignoreUnmatchedProperty if {@code true}, returns {@code null} when the property is not found;
     *        if {@code false}, throws {@link IllegalArgumentException}.
     * @return the value of the specified property, or {@code null} if the property is not found and
     *         {@code ignoreUnmatchedProperty} is {@code true}.
     * @throws IllegalArgumentException if {@code bean} is {@code null}, or if the specified property cannot be found and {@code ignoreUnmatchedProperty}
     *         is {@code false}.
     */
    @MayReturnNull
    public static <T> T getPropValue(final Object bean, final String propName, final boolean ignoreUnmatchedProperty) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);

        final Class<?> cls = bean.getClass();
        final ParserUtil.PropInfo propInfo = ParserUtil.getBeanInfo(cls).getPropInfo(propName);

        if (propInfo != null) {
            return propInfo.getPropValue(bean);
        }
        final List<Method> inlinePropGetMethodQueue = resolveInlinePropGetMethods(cls, propName);

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
                // Deliberate, and locked by NTest.testPropGetSetValue_2: an unreachable nested path reads as
                // the leaf type's default so that a primitive-typed leaf can still be assigned without
                // unboxing a null. Callers that must tell "unreachable" from "present and null" use
                // getPropValueIfPresent(Object, String).
                return (T) N.defaultValueOf(inlinePropGetMethodQueue.get(len - 1).getReturnType());
            }
        }

        return (T) propBean;
    }

    /**
     * Returns the value of the specified property wrapped in a {@link Nullable}, distinguishing
     * "property present (value may be {@code null})" from "property absent / unreachable".
     *
     * <p>Unlike {@link #getPropValue(Object, String, boolean)} — which (with {@code ignoreUnmatchedProperty=true})
     * returns {@code null} both for a genuinely {@code null} value and for an unmatched property, and returns
     * the leaf type's default value when a nested intermediate is {@code null} — this method returns:</p>
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
     * Order order = new Order();                            // id == null, address == null
     *
     * Beans.getPropValueIfPresent(order, "id");             // Nullable.of(null)  -> present, value null
     * Beans.getPropValueIfPresent(order, "unknown");        // Nullable.empty()   -> no such property
     * Beans.getPropValueIfPresent(order, "address.city");   // Nullable.empty()   -> address is null (unreachable)
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
     * @throws IllegalArgumentException if {@code bean} is {@code null}
     */
    @SuppressWarnings("unchecked")
    public static <T> Nullable<T> getPropValueIfPresent(final Object bean, final String propName) throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);

        final Class<?> cls = bean.getClass();
        final ParserUtil.PropInfo propInfo = ParserUtil.getBeanInfo(cls).getPropInfo(propName);

        if (propInfo != null) {
            return Nullable.of((T) propInfo.getPropValue(bean));
        }

        final List<Method> inlinePropGetMethodQueue = resolveInlinePropGetMethods(cls, propName);

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
     * <p>If the setter rejects {@code propValue} outright, the value is converted to the property's declared
     * type and the setter is retried. That conversion is performed by {@link N#convert(Object, Type)} and can
     * itself fail, so this method may also propagate whatever that throws - typically
     * {@link IllegalArgumentException} or {@link com.landawn.abacus.exception.ParsingException} - rather than a
     * wrapped reflection exception.</p>
     *
     * @return the actual value that was passed to the setter (after any type conversion); may be {@code null}
     *         when {@code propValue} is {@code null} and the setter's parameter type has a {@code null} default
     *         value (e.g. an object/reference type).
     * @throws IllegalArgumentException if {@code propSetMethod} is {@code null}, or if {@code propValue} cannot be converted to the property's type.
     * @throws NullPointerException if {@code bean} is {@code null} and {@code propSetMethod} is an instance method.
     * @throws RuntimeException wrapping {@link IllegalAccessException} or {@link InvocationTargetException}
     *         if the setter is inaccessible or itself throws; these are propagated immediately, without the
     *         type-converting retry (that retry only applies when the setter rejects the value's type).
     */
    @MayReturnNull
    public static Object setPropValue(final Object bean, final Method propSetMethod, Object propValue)
            throws IllegalArgumentException, NullPointerException, RuntimeException {
        N.checkArgNotNull(propSetMethod, cs.propSetMethod);

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
     * Beans.setPropValue(user, "name", "John");     // user is updated with name "John"
     * Beans.setPropValue(user, "age", 25);          // user is updated with age 25
     * Beans.setPropValue(user, "nonExistent", 1);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the object on which the property value is to be set.
     * @param propName the name of the property whose value is to be set.
     * @param propValue the value to set; if {@code null}, the property's type default is used.
     * @throws IllegalArgumentException if {@code bean} is {@code null}, or if the specified property cannot be found or set.
     * @throws UnsupportedOperationException if {@code bean} is an instance of a class treated as an immutable bean
     *         (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); its properties cannot be set in place - build a new instance instead.
     * @deprecated replaced by {@link ParserUtil.BeanInfo#setPropValue(Object, String, Object)}
     */
    @Deprecated
    public static void setPropValue(final Object bean, final String propName, final Object propValue)
            throws IllegalArgumentException, UnsupportedOperationException {
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
     * Beans.setPropValue(user, "unknown", "value", true);    // returns false (not found, ignored)
     * Beans.setPropValue(user, "unknown", "value", false);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the object on which the property value is to be set.
     * @param propName the name of the property whose value is to be set.
     * @param propValue the value to set; if {@code null}, the property's type default is used.
     * @param ignoreUnmatchedProperty if {@code true}, returns {@code false} when the property is not found
     *        instead of throwing an exception.
     * @return {@code true} if the property value was set successfully, {@code false} if the property
     *         was not found and {@code ignoreUnmatchedProperty} is {@code true}.
     * @throws IllegalArgumentException if {@code bean} is {@code null}, or if the property cannot be found and {@code ignoreUnmatchedProperty} is
     *         {@code false}.
     * @throws UnsupportedOperationException if {@code bean} is an instance of a class treated as an immutable bean
     *         (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); its properties cannot be set in place - build a new instance instead.
     * @deprecated replaced by {@link ParserUtil.BeanInfo#setPropValue(Object, String, Object, boolean)}
     */
    @Deprecated
    public static boolean setPropValue(final Object bean, final String propName, final Object propValue, final boolean ignoreUnmatchedProperty)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(bean, cs.bean);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        checkInPlaceWritable(beanInfo, bean);

        return beanInfo.setPropValue(bean, propName, propValue, ignoreUnmatchedProperty);
    }

    /**
     * Sets the property value returned by invoking the getter method on the provided bean.
     * The returned type of the get method should be {@code Collection} or {@code Map}.
     * The specified property value must be a {@code Collection} when the getter returns a
     * {@code Collection}, or a {@code Map} when the getter returns a {@code Map}; its contents
     * are copied into the returned collection or map (the concrete implementations need not match).
     * If {@code propValue} is {@code null}, this method does nothing.
     *
     * <p>The input contents are captured before the destination is cleared, so a backed view of the
     * destination is supported. Map entries retain their key identities and encounter order during the
     * copy; the destination map determines key equivalence when those entries are inserted.</p>
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
     *        must be a {@code Collection} if the getter returns a {@code Collection}, or a
     *        {@code Map} if the getter returns a {@code Map} (the concrete implementations need not match).
     *        If {@code null}, the method does nothing.
     * @throws IllegalArgumentException if {@code propValue} is non-null and {@code propGetMethod} is null,
     *         or if the getter does not return a {@link java.util.Collection} or {@link Map}.
     * @throws NullPointerException if {@code propValue} is non-{@code null}, {@code bean} is {@code null} and
     *         {@code propGetMethod} is an instance method.
     * @throws RuntimeException if access to the getter is denied or the invoked getter throws an exception.
     */
    @SuppressWarnings("unchecked")
    public static void setPropValueByGetter(final Object bean, final Method propGetMethod, final Object propValue)
            throws IllegalArgumentException, NullPointerException, RuntimeException {
        if (propValue == null) {
            return;
        }

        final Object rt = ClassUtil.invokeMethod(bean, propGetMethod);

        if (rt instanceof Collection<?> c) {
            if (rt == propValue) {
                return;
            }

            if (!(propValue instanceof Collection)) {
                throw new IllegalArgumentException("Getter method '" + propGetMethod.getName() + "' returns a Collection, but the specified value is a: "
                        + propValue.getClass().getCanonicalName());
            }

            // Snapshot before clearing: propValue is frequently a view over the very collection about to be
            // cleared (Collections.unmodifiableList(bean.getTags()), list.subList(..)), and clearing first
            // emptied both - silently, or with a ConcurrentModificationException.
            final List<Object> newValues = new ArrayList<>((Collection<Object>) propValue);

            c.clear();
            ((Collection<Object>) c).addAll(newValues);
        } else if (rt instanceof Map<?, ?> m) {
            if (rt == propValue) {
                return;
            }

            if (!(propValue instanceof Map)) {
                throw new IllegalArgumentException("Getter method '" + propGetMethod.getName() + "' returns a Map, but the specified value is a: "
                        + propValue.getClass().getCanonicalName());
            }

            // Snapshot entries without imposing a different key equivalence, and detach live map entries before clearing.
            final List<ImmutableEntry<Object, Object>> newEntries = new ArrayList<>(((Map<?, ?>) propValue).size());

            for (final Map.Entry<?, ?> entry : ((Map<?, ?>) propValue).entrySet()) {
                newEntries.add(ImmutableEntry.of(entry.getKey(), entry.getValue()));
            }

            m.clear();

            for (final ImmutableEntry<Object, Object> entry : newEntries) {
                ((Map<Object, Object>) m).put(entry.getKey(), entry.getValue());
            }
        } else {
            throw new IllegalArgumentException("Failed to set property value by getter method '" + propGetMethod.getName() + "': it returns "
                    + (rt == null ? "null" : "a " + rt.getClass().getCanonicalName()) + " rather than a Collection or Map");
        }
    }

    /**
     * Normalizes the given property name by converting it to camel case and remapping the single
     * reserved word this class knows about.
     * This method is designed for field/method/class/column/table names,
     * and both source and target strings are cached for performance.
     *
     * <p>The method performs the following transformations:</p>
     * <ul>
     *   <li>Converts underscore-separated names to camelCase</li>
     *   <li>Maps the reserved word {@code "class"} to {@code "clazz"}. That is the <i>only</i> remapping:
     *       other Java keywords and literals ({@code "int"}, {@code "enum"}, {@code "null"}, ...) are returned
     *       unchanged and are therefore still illegal Java identifiers</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Beans.normalizePropName("user_name");        // returns "userName"
     * Beans.normalizePropName("class");            // returns "clazz" (reserved keyword)
     * Beans.normalizePropName("ID");               // returns "id"
     * Beans.normalizePropName("address_line_1");   // returns "addressLine1"
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

            cacheName(formalizedPropNamePool, str, newPropName);
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
            cacheName(camelCasePropNamePool, str, newPropName);
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
            cacheName(snakeCasePropNamePool, str, result);
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
            cacheName(screamingSnakeCasePropNamePool, str, result);
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
     * class Address {
     *     private String city;
     *     private String zipCode;
     *     public String getCity() { return city; }
     *     public void setCity(String city) { this.city = city; }
     *     public String getZipCode() { return zipCode; }
     *     public void setZipCode(String zipCode) { this.zipCode = zipCode; }
     * }
     * class User {
     *     private String name;
     *     private int age;
     *     private Address address;
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public int getAge() { return age; }
     *     public void setAge(int age) { this.age = age; }
     *     public Address getAddress() { return address; }
     *     public void setAddress(Address address) { this.address = address; }
     * }
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or is not a valid bean class.
     * @see #mapToBean(Map, boolean, Class)
     * @see #mapToBean(Map, Collection, Class)
     * @see #beanToFlatMap(Object)
     * @see #beanToMap(Object)
     */
    @MayReturnNull
    public static <T> T mapToBean(final Map<String, Object> map, final Class<? extends T> targetType) throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or is not a valid bean class, or if
     *         {@code ignoreUnmatchedProperty} is {@code false} and an unmatched key is encountered.
     * @see #mapToBean(Map, Class)
     * @see #mapToBean(Map, Collection, Class)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T mapToBean(final Map<String, Object> map, final boolean ignoreUnmatchedProperty, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);
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
                if (propValue != null && isNestedBeanProp(propInfo) && Type.of(propValue.getClass()).isMap()) {
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
     * <p><b>Unmatched names are rejected at the top level only.</b> Every name in {@code selectPropNames} must
     * resolve against {@code targetType} - as a property or as a dot-separated path - or this method throws.
     * A selected name whose value is a nested {@code Map}, however, is converted with
     * {@link #mapToBean(Map, Class)}, which ignores unmatched keys: a key of that nested map that the nested
     * bean type has no property for is silently dropped rather than reported. Use
     * {@link #mapToBean(Map, boolean, Class)} with {@code false} when the strict rule has to apply at every
     * level.
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
     *        collection are left at their default values. A selected name that the map does not contain is
     *        skipped, so the new bean keeps whatever its constructor or field initializer set; mapping a name
     *        to an explicit {@code null} is the way to clear a property.
     * @param targetType the class of the bean to create; must be a valid bean class.
     * @return a new bean of the specified type with the selected properties populated from the map,
     *         or {@code null} if {@code map} is {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or is not a valid bean class, or if a selected property
     *         does not exist in the target bean class. A key of a <i>nested</i> map that the nested bean type has
     *         no property for does not throw - it is ignored; see above.
     * @see #mapToBean(Map, boolean, Class)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T mapToBean(final Map<String, Object> map, final Collection<String> selectPropNames, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

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
            propInfo = beanInfo.getPropInfo(propName);

            // Validate the selection even when there is nothing to write - this is the same predicate
            // BeanInfo.setPropValue(.., false) uses, and it is what used to reject a bogus name from inside
            // the write below. mapsToBeans applies it via checkSelectPropNames before its empty-input
            // shortcut, so both entry points reject the same selections.
            if (propInfo == null && beanInfo.getPropInfoChain(propName).isEmpty()) {
                throw new IllegalArgumentException(noSetterFoundMessage(propName, targetType));
            }

            // A selected name the map does not contain is skipped rather than written as the type's default:
            // writing it clobbered a constructor/field initializer, and the sibling mapToBean(Map, Class),
            // which iterates entrySet(), already leaves such a property alone. An explicit null in the map is
            // still written, so clearing a property remains possible.
            if (!map.containsKey(propName)) {
                continue;
            }

            propValue = map.get(propName);

            if (propInfo == null) {
                beanInfo.setPropValue(result, propName, propValue, false);
            } else {
                if (propValue != null && isNestedBeanProp(propInfo) && Type.of(propValue.getClass()).isMap()) {
                    // Deliberately the lenient 2-arg overload: this method's strictness is about the *selection*,
                    // which only names top-level properties. An unmatched key inside a nested map is dropped -
                    // documented above. mapToBean(Map, boolean, Class) is the overload that propagates strictness.
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or is not a valid bean class.
     * @see #mapToBean(Map, Class)
     * @see #mapsToBeans(Collection, Collection, Class)
     */
    public static <T> List<T> mapsToBeans(final Collection<? extends Map<String, Object>> mapList, final Class<? extends T> targetType)
            throws IllegalArgumentException {
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or is not a valid bean class, or if
     *         {@code ignoreUnmatchedProperty} is {@code false} and a map contains an unmatched key.
     * @see #mapToBean(Map, Class)
     */
    public static <T> List<T> mapsToBeans(final Collection<? extends Map<String, Object>> mapList, final boolean ignoreUnmatchedProperty,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null} or is not a valid bean class, or if a selected property
     *         does not exist in the target bean class.
     * @see #mapToBean(Map, Class)
     */
    public static <T> List<T> mapsToBeans(final Collection<? extends Map<String, Object>> mapList, final Collection<String> selectPropNames,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkBeanClass(targetType);

        final int size = N.size(mapList);
        final List<T> beanList = new ArrayList<>(size);

        if (size == 0) {
            // Validate the selection before the empty-input shortcut. mapToBean checks every selected name
            // whether or not the map carries it ("validate the selection even when there is nothing to
            // write"), so returning early here made the documented @throws fire for
            // mapsToBeans([oneMap], ["bogus"], T) but not for mapsToBeans([], ["bogus"], T) - the same bad
            // selection accepted or rejected depending only on how many maps happened to be supplied.
            checkSelectPropNames(selectPropNames, targetType);

            return beanList;
        }

        for (final Map<String, Object> map : mapList) {
            beanList.add(mapToBean(map, selectPropNames, targetType));
        }

        return beanList;
    }

    /**
     * Rejects any name in {@code selectPropNames} that {@code targetType} has no settable property for, using
     * exactly the predicate {@code mapToBean(Map, Collection, Class)} applies per name. A {@code null} or
     * empty selection has nothing to check.
     */
    private static void checkSelectPropNames(final Collection<String> selectPropNames, final Class<?> targetType) {
        if (N.isEmpty(selectPropNames)) {
            return;
        }

        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType);

        for (final String propName : selectPropNames) {
            if (beanInfo.getPropInfo(propName) == null && beanInfo.getPropInfoChain(propName).isEmpty()) {
                throw new IllegalArgumentException(noSetterFoundMessage(propName, targetType));
            }
        }
    }

    private static String noSetterFoundMessage(final String propName, final Class<?> targetType) {
        return "No setter method found with property name: " + propName + " in class: " + targetType.getCanonicalName();
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
     * Beans.beanToMap(user);            // returns {name=John, age=25} (null "active" omitted)
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
     * TreeMap<String, Object> sortedMap = Beans.beanToMap(user, IntFunctions.<String, Object> ofTreeMap());
     *
     * Beans.beanToMap((Object) null, IntFunctions.<String, Object> ofMap());   // returns {} (empty map)
     * }</pre>
     *
     * @param <M> the type of the resulting Map.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type containing the non-{@code null} property name-value pairs of the bean;
     *         never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

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
     * Beans.beanToMap(user, (Collection<String>) null);      // returns all non-null props
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
    public static Map<String, Object> beanToMap(final Object bean, final Collection<String> selectPropNames) throws IllegalArgumentException {
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
     * TreeMap<String, Object> sortedMap = Beans.beanToMap(user, Arrays.asList("name", "age"), IntFunctions.<String, Object> ofTreeMap());
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
     * @throws IllegalArgumentException if a selected property does not exist in the bean class, or if
     *         {@code mapSupplier} is {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

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
     * class User {
     *     private String firstName;
     *     private String lastName;
     *     public String getFirstName() { return firstName; }
     *     public void setFirstName(String firstName) { this.firstName = firstName; }
     *     public String getLastName() { return lastName; }
     *     public void setLastName(String lastName) { this.lastName = lastName; }
     * }
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     * Collection<String> props = Arrays.asList("firstName", "lastName");
     *
     * Beans.beanToMap(user, props, NamingPolicy.SNAKE_CASE, IntFunctions.<String, Object> ofLinkedHashMap());
     * // returns {first_name=John, last_name=Doe}
     *
     * Beans.beanToMap(user, props, NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.<String, Object> ofLinkedHashMap());
     * // returns {FIRST_NAME=John, LAST_NAME=Doe}
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param selectPropNames the property names to include. If {@code null}, all non-{@code null}
     *        properties are included. If empty, no properties are included. Selected properties are
     *        included even when their values are {@code null}. A name may be any spelling the property
     *        resolver accepts (case-insensitive, underscore-stripped, or {@code get}/{@code is}/
     *        {@code has}-prefixed); the emitted key is always the bean's own property name with
     *        {@code keyNamingPolicy} applied, so two spellings of one property yield one entry.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type with property name-value pairs; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist in the bean class, or if
     *         {@code mapSupplier} is {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

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
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param output the map into which the bean's non-{@code null} properties will be put. Existing entries are preserved unless overwritten by a
     *        generated key. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     */
    public static void beanToMap(final Object bean, final Map<String, Object> output) throws IllegalArgumentException {
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
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included in the map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. Selected properties are included even when their values are {@code null}.
     * @param output the map into which the bean's properties will be put. Existing entries are preserved unless overwritten by a generated key. Must
     *        not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if a selected property does not exist in the bean class.
     */
    public static void beanToMap(final Object bean, final Collection<String> selectPropNames, final Map<String, Object> output)
            throws IllegalArgumentException {
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
     * class User {
     *     private String firstName;
     *     private String lastName;
     *     public String getFirstName() { return firstName; }
     *     public void setFirstName(String firstName) { this.firstName = firstName; }
     *     public String getLastName() { return lastName; }
     *     public void setLastName(String lastName) { this.lastName = lastName; }
     * }
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     *
     * Map<String, Object> outputMap = new LinkedHashMap<>();
     * Beans.beanToMap(user, Arrays.asList("firstName", "lastName"), NamingPolicy.SNAKE_CASE, outputMap);
     * // outputMap: {first_name=John, last_name=Doe}
     * }</pre>
     *
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param selectPropNames the property names to include. If {@code null}, all non-{@code null}
     *        properties are included. If empty, no properties are included. Selected properties are
     *        included even when their values are {@code null}. A name may be any spelling the property
     *        resolver accepts (case-insensitive, underscore-stripped, or {@code get}/{@code is}/
     *        {@code has}-prefixed); the emitted key is always the bean's own property name with
     *        {@code keyNamingPolicy} applied, so two spellings of one property yield one entry.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param output the map into which the bean's properties will be put. Existing entries are preserved unless overwritten by a generated key. Must
     *        not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if a selected property does not exist in the bean class.
     */
    public static void beanToMap(final Object bean, final Collection<String> selectPropNames, NamingPolicy keyNamingPolicy, final Map<String, Object> output)
            throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.CAMEL_CASE : keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (selectPropNames == null) {
            beanToMap(bean, true, null, keyNamingPolicy, output);
        } else {
            ParserUtil.PropInfo propInfo = null;

            // Two accepted spellings of one property ("firstName" and "first_name", say) resolve to the same
            // PropInfo and must be emitted once: the key is derived from propInfo.name, so the second write
            // only overwrote the first - but it still ran the getter a second time. BeanMapBuilder has
            // always de-duplicated this way; the direct overloads did not.
            final Set<String> seen = N.newHashSet(selectPropNames.size());

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass); //NOSONAR
                }

                if (!seen.add(propInfo.name)) {
                    continue;
                }

                // propInfo.name, not propName: a selection name may be any spelling getPropInfo accepts
                // (case-insensitive, underscore-stripped, get/is/has-prefixed), and echoing the caller's
                // spelling made the key caller-dependent - so beanToMap(bean, ["getFirstName"], SNAKE_CASE)
                // emitted "get_first_name", and this overload disagreed with the unselected one, which has
                // always keyed off propInfo.name.
                output.put(convertMapKey(propInfo.name, keyNamingPolicy), propInfo.getPropValue(bean));
            }
        }
    }

    /**
     * The map key for {@code propName} under {@code keyNamingPolicy}.
     *
     * <p>{@link NamingPolicy#CAMEL_CASE} (the default, including a {@code null} policy) and
     * {@link NamingPolicy#NO_CHANGE} both emit the property name verbatim rather than re-running the
     * camelCase converter over it &mdash; see the class documentation for why.</p>
     *
     * @param propName the property name to convert
     * @param keyNamingPolicy the policy to apply; {@code null} is treated as {@link NamingPolicy#CAMEL_CASE}
     * @return the key to store under
     */
    private static String convertMapKey(final String propName, final NamingPolicy keyNamingPolicy) {
        return isVerbatimKeyPolicy(keyNamingPolicy) ? propName : keyNamingPolicy.convert(propName);
    }

    /**
     * Whether {@code keyNamingPolicy} emits property names verbatim, so the converter never has to run.
     *
     * <p>This is the single definition of that rule. The conversion loops hoist it out of their per-property
     * loop rather than calling {@link #convertMapKey(String, NamingPolicy)} for every entry, and the rule had
     * ended up spelled out inline at each of those sites - four copies of one decision that has to stay
     * identical for the naming contract in the class documentation to hold.</p>
     *
     * @param keyNamingPolicy the policy to test; {@code null} is treated as {@link NamingPolicy#CAMEL_CASE}
     * @return {@code true} if keys are the bean's property names unchanged
     */
    private static boolean isVerbatimKeyPolicy(final NamingPolicy keyNamingPolicy) {
        return keyNamingPolicy == null || NamingPolicy.CAMEL_CASE == keyNamingPolicy || NamingPolicy.NO_CHANGE == keyNamingPolicy;
    }

    /**
     * Whether a property expands into a nested map under {@code deepBeanToMap}/{@code beanToFlatMap}, and
     * (equivalently) whether {@code mapToBean} converts a nested {@code Map} value back into it.
     *
     * <p>These two directions have to agree for the round trip the class documents to hold, so they share this
     * one predicate rather than each testing a {@code Type} of its own. The decision is made from the
     * property's <b>declared</b> type, never from the runtime class of its value - see {@link #putDeep}.</p>
     *
     * @param propInfo the property being classified
     * @return {@code true} if the property is treated as a nested bean
     */
    private static boolean isNestedBeanProp(final PropInfo propInfo) {
        return propInfo.jsonXmlType.isBean();
    }

    /**
     * Emits one already-read property into a <i>deep</i> bean-to-map result.
     *
     * <p>Whether the value is nested is decided by the property's <b>declared</b> type, not by the runtime
     * class of {@code propValue}: an {@code Object}- or interface-typed property is emitted as-is even when
     * it happens to hold a bean. That keeps the output shape a function of the bean class rather than of the
     * data, keeps it invertible by {@link #mapToBean(Map, Class)} (which gates on the same declared type),
     * and avoids expanding values such as {@link Throwable} or {@link java.io.File}, which are
     * structurally beans but whose useful state is not in their properties.</p>
     *
     * @param propInfo metadata for the property being emitted
     * @param propName the name to derive the key from
     * @param propValue the value already read from the bean
     * @param ignoreNullProperty whether {@code null}-valued properties of the nested bean are dropped
     * @param keyNamingPolicy the policy applied to the key
     * @param nestedMapSupplier creates the map for a nested bean; {@code null} means {@link java.util.LinkedHashMap}
     * @param output the map being filled
     */
    private static void putDeep(final PropInfo propInfo, final String propName, final Object propValue, final boolean ignoreNullProperty,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends Map<String, Object>> nestedMapSupplier, final Map<String, Object> output) {
        final String key = convertMapKey(propName, keyNamingPolicy);

        if (propValue == null || !isNestedBeanProp(propInfo)) {
            output.put(key, propValue);
        } else {
            final IntFunction<? extends Map<String, Object>> supplier = nestedMapSupplierOrDefault(nestedMapSupplier);
            final Map<String, Object> nested = supplier.apply(getPropNameList(propValue.getClass()).size());

            deepBeanToMapAll(propValue, ignoreNullProperty, null, keyNamingPolicy, nestedMapSupplier, nested);
            output.put(key, nested);
        }
    }

    /**
     * The supplier to build a nested bean's map with, defaulting to {@link java.util.LinkedHashMap}.
     *
     * @param nestedMapSupplier the caller's supplier, or {@code null}
     * @return a non-{@code null} supplier
     */
    private static IntFunction<? extends Map<String, Object>> nestedMapSupplierOrDefault(final IntFunction<? extends Map<String, Object>> nestedMapSupplier) {
        return nestedMapSupplier == null ? IntFunctions.ofLinkedHashMap() : nestedMapSupplier;
    }

    /**
     * Emits one already-read property into a <i>flat</i> bean-to-map result, recursing under a dotted key
     * when the property's declared type is a bean. See {@link #putDeep} for why the declared type decides.
     *
     * @param propInfo metadata for the property being emitted
     * @param propName the name to derive the key from
     * @param propValue the value already read from the bean
     * @param ignoreNullProperty whether {@code null}-valued properties of the nested bean are dropped
     * @param keyNamingPolicy the policy applied to the key
     * @param output the map being filled
     */
    private static void putFlat(final PropInfo propInfo, final String propName, final Object propValue, final boolean ignoreNullProperty,
            final NamingPolicy keyNamingPolicy, final Map<String, Object> output) {
        final String key = convertMapKey(propName, keyNamingPolicy);

        if (propValue == null || !isNestedBeanProp(propInfo)) {
            output.put(key, propValue);
        } else {
            beanToFlatMap(propValue, ignoreNullProperty, null, keyNamingPolicy, key, output);
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
     * TreeMap<String, Object> sortedMap = Beans.beanToMap(user, true, ignoredProps, IntFunctions.<String, Object> ofTreeMap());
     * // sortedMap: {email=john@example.com, name=John} (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the map to be returned.
     * @param bean the bean object to be converted into a map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are excluded from the map.
     * @param ignoredPropNames a set of property names to exclude from the map; ignored if {@code null}.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type with the bean's property name-value pairs; never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

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
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @return a {@link java.util.LinkedHashMap} with the bean's property name-value pairs; never {@code null}.
     */
    public static Map<String, Object> beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) {
        return beanToMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a map with optional {@code null} property filtering, property exclusion, and key naming policy.
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
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param mapSupplier a function that creates a new Map instance given an initial capacity.
     * @return a map of the specified type with the bean's property name-value pairs; never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}.
     */
    public static <M extends Map<String, Object>> M beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

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
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are not added to the output map.
     * @param output the map into which the bean's properties will be put. Existing entries are preserved unless overwritten by a generated key. Must
     *        not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     */
    public static void beanToMap(final Object bean, final boolean ignoreNullProperty, final Map<String, Object> output) throws IllegalArgumentException {
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
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are not added to the output map.
     * @param ignoredPropNames a set of property names to exclude from the output map; ignored if {@code null}.
     * @param output the map into which the bean's properties will be put. Existing entries are preserved unless overwritten by a generated key. Must
     *        not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     */
    public static void beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames, final Map<String, Object> output)
            throws IllegalArgumentException {
        beanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a map and stores the result in the provided map with optional {@code null} property filtering and property exclusion.
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
     * @param bean the bean object to be converted into a map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values are not added to the output map.
     * @param ignoredPropNames a set of property names to exclude from the output map; ignored if {@code null}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param output the map into which the bean's properties will be put. Existing entries are preserved unless overwritten by a generated key. Must
     *        not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     */
    public static void beanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames, NamingPolicy keyNamingPolicy,
            final Map<String, Object> output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.CAMEL_CASE : keyNamingPolicy;
        final boolean isCamelCaseOrNoChange = isVerbatimKeyPolicy(keyNamingPolicy);
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

            output.put(isCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName), propValue);
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * The resulting map uses LinkedHashMap to preserve property order.
     * By default, properties with {@code null} values are omitted.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean) throws IllegalArgumentException {
        return deepBeanToMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * The map type is determined by the provided mapSupplier.
     * By default, properties with {@code null} values are omitted.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     * TreeMap<String, Object> sortedDeepMap = Beans.deepBeanToMap(user, IntFunctions.<String, Object> ofTreeMap());
     * // sortedDeepMap: {
     * //   address={city=New York},
     * //   name=John
     * // } (sorted by key)
     * }</pre>
     *
     * @param <M> the type of the Map to which the bean will be converted.
     * @param bean the bean to be converted into a Map; if {@code null}, an empty map is returned.
     * @param mapSupplier a supplier function to create the Map instance. It is used for <i>every</i> map the
     *        conversion creates, including the nested map of each nested bean.
     * @return a Map of the specified type representing the provided bean; never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final IntFunction<? extends M> mapSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return deepBeanToMap(bean, null, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Only properties specified in selectPropNames are included. If {@code selectPropNames} is
     * {@code null}, all non-{@code null} properties are included. If it is empty, no properties
     * are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     *        <br>A selected nested bean whose every property is {@code null} therefore comes back as an empty
     *        nested map rather than being dropped; the {@code beanToFlatMap} twin, which cannot represent an
     *        empty nested object in a flat key space, drops it instead. Use {@link Beans#mapBuilder(Object)},
     *        whose null policy applies at every level, when the nested {@code null}s must survive.
     * @return a {@link java.util.LinkedHashMap} representation of the provided bean; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist, or if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final Collection<String> selectPropNames) throws IllegalArgumentException {
        return deepBeanToMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Only properties specified in selectPropNames are included, and the map type is determined by mapSupplier.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     * Map<String, Object> customDeepMap = Beans.deepBeanToMap(user, props, IntFunctions.<String, Object> ofMap());
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
     *        <br>A selected nested bean whose every property is {@code null} therefore comes back as an empty
     *        nested map rather than being dropped; the {@code beanToFlatMap} twin, which cannot represent an
     *        empty nested object in a flat key space, drops it instead. Use {@link Beans#mapBuilder(Object)},
     *        whose null policy applies at every level, when the nested {@code null}s must survive.
     * @param mapSupplier a supplier function to create the Map instance. It is used for <i>every</i> map the
     *        conversion creates, including the nested map of each nested bean.
     * @return a Map of the specified type representing the provided bean; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist, or if {@code mapSupplier} is {@code null}, or if the traversed bean
     *         graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return deepBeanToMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     * The keys in the map are transformed according to the specified naming policy.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     *     NamingPolicy.SNAKE_CASE, LinkedHashMap::new);
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
     *        <br>A selected nested bean whose every property is {@code null} therefore comes back as an empty
     *        nested map rather than being dropped; the {@code beanToFlatMap} twin, which cannot represent an
     *        empty nested object in a flat key space, drops it instead. Use {@link Beans#mapBuilder(Object)},
     *        whose null policy applies at every level, when the nested {@code null}s must survive.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param mapSupplier a supplier function to create the Map instance into which the bean properties will be
     *        put. It is used for <i>every</i> map the conversion creates, including the nested map of each
     *        nested bean.
     * @return a Map of the specified type representing the provided bean; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist, or if {@code mapSupplier} is {@code null}, or if the traversed bean
     *         graph contains a reference cycle.
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final M output = mapSupplier.apply(selectPropNames == null ? getPropNameList(bean.getClass()).size() : selectPropNames.size());

        deepBeanToMapSelected(bean, selectPropNames, true, keyNamingPolicy, mapSupplier, output);

        return output;
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * By default, only non-{@code null} properties are included and stored in the provided output map.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     * @param bean the bean to be converted into a Map; if {@code null}, the output map is not modified.
     * @param output the map into which the bean's properties will be put. Nested beans become {@link java.util.LinkedHashMap}s, since this overload
     *        is given a map rather than a supplier; use an overload taking a {@code mapSupplier} to control the nested map type as well. Must not be
     *        {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void deepBeanToMap(final Object bean, final Map<String, Object> output) throws IllegalArgumentException {
        deepBeanToMap(bean, null, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Only properties specified in selectPropNames are included and stored in the provided output map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     * @param bean the bean to be converted into a Map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included even when
     *        {@code null}, while {@code null} properties inside nested beans are always omitted.
     *        <br>A selected nested bean whose every property is {@code null} therefore comes back as an empty
     *        nested map rather than being dropped; the {@code beanToFlatMap} twin, which cannot represent an
     *        empty nested object in a flat key space, drops it instead. Use {@link Beans#mapBuilder(Object)},
     *        whose null policy applies at every level, when the nested {@code null}s must survive.
     * @param output the map into which the bean's properties will be put. Nested beans become {@link java.util.LinkedHashMap}s, since this overload
     *        is given a map rather than a supplier; use an overload taking a {@code mapSupplier} to control the nested map type as well. Must not be
     *        {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if a selected property does not exist, or if the traversed bean graph contains a
     *         reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void deepBeanToMap(final Object bean, final Collection<String> selectPropNames, final Map<String, Object> output)
            throws IllegalArgumentException {
        deepBeanToMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Only properties specified in selectPropNames are included, keys are transformed according to the naming policy, and results are stored in the output map.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     * @param bean the bean to be converted into a Map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included during the conversion process.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included even when
     *        {@code null}, while {@code null} properties inside nested beans are always omitted.
     *        <br>A selected nested bean whose every property is {@code null} therefore comes back as an empty
     *        nested map rather than being dropped; the {@code beanToFlatMap} twin, which cannot represent an
     *        empty nested object in a flat key space, drops it instead. Use {@link Beans#mapBuilder(Object)},
     *        whose null policy applies at every level, when the nested {@code null}s must survive.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param output the map into which the bean's properties will be put. Nested beans become {@link java.util.LinkedHashMap}s, since this overload
     *        is given a map rather than a supplier; use an overload taking a {@code mapSupplier} to control the nested map type as well. Must not be
     *        {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if a selected property does not exist, or if the traversed bean graph contains a
     *         reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void deepBeanToMap(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final Map<String, Object> output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        deepBeanToMapSelected(bean, selectPropNames, true, keyNamingPolicy, null, output);
    }

    /**
     * The shared body of the selection-based {@code deepBeanToMap} overloads.
     *
     * @param bean the bean to convert; {@code null} leaves {@code output} untouched
     * @param selectPropNames the properties to emit, or {@code null} for "all, minus nulls"
     * @param nestedIgnoreNullProperty whether {@code null} properties of a <i>nested</i> bean are dropped. The
     *        public selection overloads pass {@code true} (their documented behaviour); {@link BeanMapBuilder}
     *        passes its own {@code skipNulls} so that its null policy is the same at every level
     * @param keyNamingPolicy the policy applied to the keys
     * @param nestedMapSupplier creates the map for a nested bean; {@code null} means {@link java.util.LinkedHashMap}
     * @param output the map being filled
     * @throws IllegalArgumentException if a selected property does not exist, or the traversed bean graph is cyclic
     */
    private static void deepBeanToMapSelected(final Object bean, final Collection<String> selectPropNames, final boolean nestedIgnoreNullProperty,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends Map<String, Object>> nestedMapSupplier, final Map<String, Object> output)
            throws IllegalArgumentException {
        if (bean == null) {
            return;
        }

        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (selectPropNames == null) {
            deepBeanToMapAll(bean, nestedIgnoreNullProperty, null, keyNamingPolicy, nestedMapSupplier, output);
        } else {
            ParserUtil.PropInfo propInfo = null;

            // Two accepted spellings of one property ("firstName" and "first_name", say) resolve to the same
            // PropInfo and must be emitted once: the key is derived from propInfo.name, so the second write
            // only overwrote the first - but it still ran the getter a second time. BeanMapBuilder has
            // always de-duplicated this way; the direct overloads did not.
            final Set<String> seen = N.newHashSet(selectPropNames.size());

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                if (!seen.add(propInfo.name)) {
                    continue;
                }

                // propInfo.name, not propName - see beanToMap(Object, Collection, NamingPolicy, Map).
                putDeep(propInfo, propInfo.name, propInfo.getPropValue(bean), nestedIgnoreNullProperty, keyNamingPolicy, nestedMapSupplier, output);
            }
        }
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Whether {@code null} values are included is controlled by {@code ignoreNullProperty}.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a User bean with nested Address
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * // result: {name=John, age=25, address={city=NYC, zipCode=10001}}
     * Map<String, Object> result = Beans.deepBeanToMap(user, false);
     *
     * // With ignoreNullProperty=true
     * User userWithNull = new User("Jane", null, null);
     * Map<String, Object> filtered = Beans.deepBeanToMap(userWithNull, true);
     * // filtered: {name=Jane} (null properties excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @return a {@link java.util.LinkedHashMap} representation of the bean where nested beans are recursively converted to Maps; never {@code null}.
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final boolean ignoreNullProperty) throws IllegalArgumentException {
        return deepBeanToMap(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Properties whose names are in the ignoredPropNames set will be excluded from the conversion.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a User bean with multiple properties
     * User user = new User("John", 25, "john@example.com", new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("email", "age"));
     * Map<String, Object> result = Beans.deepBeanToMap(user, false, ignored);
     * // result: {name=John, address={city=NYC}} (email and age excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process. Can be {@code null}.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @return a {@link java.util.LinkedHashMap} representation of the bean with specified properties excluded; never {@code null}.
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames)
            throws IllegalArgumentException {
        return deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * The resulting Map type can be customized using the mapSupplier function.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a TreeMap instead of default LinkedHashMap
     * User user = new User("John", 25, new Address("NYC"));
     * TreeMap<String, Object> result = Beans.deepBeanToMap(user, false, null,
     *     size -> new TreeMap<>());
     * // result: TreeMap with {address={city=NYC}, age=25, name=John} (sorted keys)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial
     *        capacity. It is used for <i>every</i> map the conversion creates, including the nested map of each
     *        nested bean.
     * @return a Map of the specified type containing the bean properties; never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * The keys in the resulting Map can be transformed according to the specified naming policy.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given a bean with camelCase properties
     * User user = new User();
     * user.setFirstName("John");
     * user.setLastName("Doe");
     *
     * Map<String, Object> snakeCase = Beans.deepBeanToMap(user, false, null,
     *     NamingPolicy.SNAKE_CASE);
     * // snakeCase: {first_name=John, last_name=Doe}
     *
     * Map<String, Object> upperCase = Beans.deepBeanToMap(user, false, null,
     *     NamingPolicy.SCREAMING_SNAKE_CASE);
     * // upperCase: {FIRST_NAME=John, LAST_NAME=Doe}
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @return a {@link java.util.LinkedHashMap} representation of the bean with keys transformed according to the naming policy; never {@code null}.
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) throws IllegalArgumentException {
        return deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts the provided bean into a Map where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Provides full control over the conversion process including naming policy and Map type.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom conversion with all options
     * User user = new User("John", (Integer) null, new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalId"));
     *
     * LinkedHashMap<String, Object> result = Beans.deepBeanToMap(user, true, ignored,
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
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial
     *        capacity. It is used for <i>every</i> map the conversion creates, including the nested map of each
     *        nested bean.
     * @return a Map of the specified type with full customization applied; never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        if (bean == null) {
            return mapSupplier.apply(0);
        }

        final int beanPropNameSize = getPropNameList(bean.getClass()).size();
        final int initCapacity = N.max(0, beanPropNameSize - N.size(ignoredPropNames));

        final M output = mapSupplier.apply(initCapacity);

        deepBeanToMapAll(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, mapSupplier, output);

        return output;
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * The conversion is performed in-place into the provided output Map.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Reuse existing map
     * Map<String, Object> existingMap = new HashMap<>();
     * existingMap.put("timestamp", System.currentTimeMillis());
     *
     * User user = new User("John", 25);
     * Beans.deepBeanToMap(user, false, existingMap); // existingMap contains {timestamp=..., name=John, age=25}
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param output the Map instance into which the bean properties will be put. Existing entries are preserved unless overwritten by a generated
     *        key. Nested beans become {@link java.util.LinkedHashMap}s, since this overload is given a map rather than a supplier; use an overload
     *        taking a {@code mapSupplier} to control the nested map type as well. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Map<String, Object> output) throws IllegalArgumentException {
        deepBeanToMap(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * Properties whose names are in the ignoredPropNames set will be excluded from the conversion.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Populate map with selective properties
     * Map<String, Object> output = new HashMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("password", "ssn"));
     *
     * User user = new User("John", "pass123", "123-45-6789");
     * Beans.deepBeanToMap(user, false, ignored, output);
     * // output: {name=John} (sensitive fields excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param output the Map instance into which the bean properties will be put. Existing entries are preserved unless overwritten by a generated
     *        key. Nested beans become {@link java.util.LinkedHashMap}s, since this overload is given a map rather than a supplier; use an overload
     *        taking a {@code mapSupplier} to control the nested map type as well. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #deepBeanToMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames, final Map<String, Object> output)
            throws IllegalArgumentException {
        deepBeanToMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts the provided bean into the specified Map instance where the keys are the property names of the bean and the values are the corresponding property values.
     * This method recursively converts non-null properties declared as nested bean types into maps;
     * see the class documentation for property types that remain unchanged.
     * The conversion process can be customized by specifying properties to ignore, whether to ignore {@code null} properties, and the naming policy for keys.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full control over in-place conversion
     * Map<String, Object> output = new TreeMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("id"));
     *
     * Product product = new Product("Widget", 29.99, new Category("Electronics"));
     * Beans.deepBeanToMap(product, true, ignored, NamingPolicy.SCREAMING_SNAKE_CASE, output);
     * // output: {CATEGORY={NAME=Electronics}, NAME=Widget, PRICE=29.99} (sorted)
     * }</pre>
     *
     * @param bean the bean object to be converted into a Map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties of the bean with {@code null} values will not be included in the output Map.
     * @param ignoredPropNames a set of property names to be ignored during the conversion process.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not matched.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param output the Map instance into which the bean properties will be put. Nested beans become {@link java.util.LinkedHashMap}s, since this
     *        overload is given a map rather than a supplier; use an overload taking a {@code mapSupplier} to control the nested map type as well.
     *        Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     */
    public static void deepBeanToMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final Map<String, Object> output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        deepBeanToMapAll(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, null, output);
    }

    /**
     * The shared body of the exclusion-based {@code deepBeanToMap} overloads.
     *
     * @param bean the bean to convert; {@code null} leaves {@code output} untouched
     * @param ignoreNullProperty whether {@code null}-valued properties are dropped, at every level
     * @param ignoredPropNames top-level property names to skip; not applied to nested beans
     * @param keyNamingPolicy the policy applied to the keys
     * @param nestedMapSupplier creates the map for a nested bean; {@code null} means {@link java.util.LinkedHashMap}
     * @param output the map being filled
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle
     */
    private static void deepBeanToMapAll(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends Map<String, Object>> nestedMapSupplier, final Map<String, Object> output)
            throws IllegalArgumentException {
        if (bean == null) {
            return;
        }

        // Cycle guard: if `bean` is already being converted higher up the call stack (e.g. bidirectional
        // `parent <-> child` references), reject the input instead of infinite-recursing into a
        // StackOverflowError. Bailing out silently used to hand back a map that was quietly missing a whole
        // branch; Maps.flatten already refuses a cyclic structure the same way.
        if (!enterDeepBean(bean)) {
            throw new IllegalArgumentException("Cyclic bean reference cannot be converted to a map: " + ClassUtil.getCanonicalClassName(bean.getClass()));
        }
        try {
            final boolean isCamelCaseOrNoChange = isVerbatimKeyPolicy(keyNamingPolicy);

            final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
            final Class<?> beanClass = bean.getClass();
            final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
            final IntFunction<? extends Map<String, Object>> supplier = nestedMapSupplierOrDefault(nestedMapSupplier);

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

                final String key = isCamelCaseOrNoChange ? propName : keyNamingPolicy.convert(propName);

                if ((propValue == null) || !isNestedBeanProp(propInfo)) {
                    output.put(key, propValue);
                } else {
                    // The caller's supplier builds every level, not just the outermost map: a
                    // deepBeanToMap(bean, IntFunctions.ofTreeMap()) whose nested beans came back as
                    // LinkedHashMaps was silently ignoring the supplier everywhere but the top.
                    final Map<String, Object> nested = supplier.apply(getPropNameList(propValue.getClass()).size());

                    deepBeanToMapAll(propValue, ignoreNullProperty, null, keyNamingPolicy, nestedMapSupplier, nested);
                    output.put(key, nested);
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
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Given nested beans
     * User user = new User("John", new Address("NYC", "10001"));
     * // flat: {name=John, address.city=NYC, address.zipCode=10001}
     * Map<String, Object> flat = Beans.beanToFlatMap(user);
     *
     * // Deep nesting
     * Company company = new Company("TechCorp",
     *     new Address("NYC", new Location(40.7128, -74.0060)));
     * Map<String, Object> result = Beans.beanToFlatMap(company);
     * // result: {name=TechCorp, address.city=NYC,
     * //          address.location.latitude=40.7128,
     * //          address.location.longitude=-74.0060}
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @return a map representing the bean object with nested properties flattened using dot notation; never {@code null}.
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean) throws IllegalArgumentException {
        return beanToFlatMap(bean, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation where nested properties are represented with dot notation.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * The type of Map returned can be customized using the mapSupplier. By default, properties with {@code null} values are omitted.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a sorted flat map
     * User user = new User("John", new Address("NYC", "10001"));
     * TreeMap<String, Object> sortedFlat = Beans.beanToFlatMap(user,
     *     size -> new TreeMap<>());
     * // sortedFlat: {address.city=NYC, address.zipCode=10001, name=John} (sorted)
     * }</pre>
     *
     * @param <M> the type of Map to be returned.
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with nested properties flattened; never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final IntFunction<? extends M> mapSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return beanToFlatMap(bean, null, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included in the result. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select specific properties including nested ones
     * User user = new User("John", 25, new Address("NYC", "10001"));
     * Collection<String> select = Arrays.asList("name", "address");
     * // result: {name=John, address.city=NYC, address.zipCode=10001}
     * Map<String, Object> result = Beans.beanToFlatMap(user, select);
     *
     * // Select only top-level properties
     * Collection<String> topLevel = Arrays.asList("name", "age");
     * Map<String, Object> flat = Beans.beanToFlatMap(user, topLevel);
     * // flat: {name=John, age=25} (address excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param selectPropNames a collection of property names to be included in the resulting map. Nested properties of selected beans are automatically included.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     *        <br><b>A selected nested bean can therefore contribute no key at all:</b> a flat map has no way to
     *        represent an empty nested object, so a nested bean whose every property is {@code null} simply
     *        disappears. Given a {@code User} whose {@code address} is non-{@code null} but whose
     *        {@code address.city} is {@code null}, {@code beanToFlatMap(user, ["address"])} returns {@code {}} -
     *        while the deep twin {@code deepBeanToMap(user, ["address"])} returns {@code {address={}}}. Use
     *        {@link Beans#mapBuilder(Object)}, whose null policy applies at every level, when a selected
     *        property must always appear: {@code mapBuilder(user).flat().select("address").toMap()} yields
     *        {@code {address.city=null}}.
     * @return a map with only the selected properties flattened; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist, or if the traversed bean graph contains a reference cycle.
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final Collection<String> selectPropNames) throws IllegalArgumentException {
        return beanToFlatMap(bean, selectPropNames, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation with only selected properties and custom Map type.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines property selection with Map type customization. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Select properties and use custom map type
     * Employee emp = new Employee("John", "IT", new Manager("Jane"));
     * Collection<String> select = Arrays.asList("name", "manager");
     *
     * LinkedHashMap<String, Object> result = Beans.beanToFlatMap(emp, select,
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
     *        <br><b>A selected nested bean can therefore contribute no key at all:</b> a flat map has no way to
     *        represent an empty nested object, so a nested bean whose every property is {@code null} simply
     *        disappears. Given a {@code User} whose {@code address} is non-{@code null} but whose
     *        {@code address.city} is {@code null}, {@code beanToFlatMap(user, ["address"])} returns {@code {}} -
     *        while the deep twin {@code deepBeanToMap(user, ["address"])} returns {@code {address={}}}. Use
     *        {@link Beans#mapBuilder(Object)}, whose null policy applies at every level, when a selected
     *        property must always appear: {@code mapBuilder(user).flat().select("address").toMap()} yields
     *        {@code {address.city=null}}.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a map of the specified type with selected properties flattened; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist, or if {@code mapSupplier} is {@code null}, or if the traversed bean
     *         graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final Collection<String> selectPropNames,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return beanToFlatMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and a specified naming policy.
     * This method takes a bean object and transforms it into a map where the keys are the property names of the bean and the values are the corresponding property values.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * If {@code selectPropNames} is {@code null}, all non-{@code null} properties are included.
     * If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // With naming policy transformation
     * User user = new User();
     * user.setFirstName("John");
     * user.setHomeAddress(new Address("NYC"));
     *
     * Collection<String> select = Arrays.asList("firstName", "homeAddress");
     * Map<String, Object> snakeCase = Beans.beanToFlatMap(user, select,
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
     *        <br><b>A selected nested bean can therefore contribute no key at all:</b> a flat map has no way to
     *        represent an empty nested object, so a nested bean whose every property is {@code null} simply
     *        disappears. Given a {@code User} whose {@code address} is non-{@code null} but whose
     *        {@code address.city} is {@code null}, {@code beanToFlatMap(user, ["address"])} returns {@code {}} -
     *        while the deep twin {@code deepBeanToMap(user, ["address"])} returns {@code {address={}}}. Use
     *        {@link Beans#mapBuilder(Object)}, whose null policy applies at every level, when a selected
     *        property must always appear: {@code mapBuilder(user).flat().select("address").toMap()} yields
     *        {@code {address.city=null}}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param mapSupplier a function that generates a new map instance. The function argument is the initial map capacity.
     * @return a map of the specified type with the bean's (selected) properties flattened using dot notation for nested beans; never {@code null}.
     * @throws IllegalArgumentException if a selected property does not exist, or if {@code mapSupplier} is {@code null}, or if the traversed bean
     *         graph contains a reference cycle.
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final Collection<String> selectPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

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
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Populate existing map with flattened bean
     * Map<String, Object> output = new HashMap<>();
     * output.put("version", "1.0");
     *
     * User user = new User("John", new Address("NYC"));
     * Beans.beanToFlatMap(user, output);
     * // output: {version=1.0, name=John, address.city=NYC}
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param output the Map instance into which the flattened bean properties will be put. Existing entries are preserved unless overwritten by a
     *        generated key. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void beanToFlatMap(final Object bean, final Map<String, Object> output) throws IllegalArgumentException {
        beanToFlatMap(bean, null, output);
    }

    /**
     * Converts a bean object into a flat map representation with selected properties and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Only properties specified in selectPropNames are included. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Selective flattening into existing map
     * Map<String, Object> output = new LinkedHashMap<>();
     * Collection<String> select = Arrays.asList("name", "contact");
     *
     * Customer customer = new Customer("John", "123-456",
     *     new Contact("john@email.com", "555-1234"));
     * Beans.beanToFlatMap(customer, select, output);
     * // output: {name=John, contact.email=john@email.com, contact.phone=555-1234}
     * // (customerId excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included in the output map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     *        <br><b>A selected nested bean can therefore contribute no key at all:</b> a flat map has no way to
     *        represent an empty nested object, so a nested bean whose every property is {@code null} simply
     *        disappears. Given a {@code User} whose {@code address} is non-{@code null} but whose
     *        {@code address.city} is {@code null}, {@code beanToFlatMap(user, ["address"])} returns {@code {}} -
     *        while the deep twin {@code deepBeanToMap(user, ["address"])} returns {@code {address={}}}. Use
     *        {@link Beans#mapBuilder(Object)}, whose null policy applies at every level, when a selected
     *        property must always appear: {@code mapBuilder(user).flat().select("address").toMap()} yields
     *        {@code {address.city=null}}.
     * @param output the Map instance into which the flattened bean properties will be put. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if a selected property does not exist, or if the traversed bean graph contains a
     *         reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void beanToFlatMap(final Object bean, final Collection<String> selectPropNames, final Map<String, Object> output)
            throws IllegalArgumentException {
        beanToFlatMap(bean, selectPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation with full customization options and stores the result in the provided Map instance.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides control over property selection and key naming policy. If {@code selectPropNames} is {@code null},
     * all non-{@code null} properties are included. If it is empty, no properties are included.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full customization of flattening process
     * Map<String, Object> output = new TreeMap<>();
     * Collection<String> select = Arrays.asList("productName", "category");
     *
     * Product product = new Product("WidgetPro",
     *     new Category("Electronics", "Gadgets"));
     * Beans.beanToFlatMap(product, select, NamingPolicy.SCREAMING_SNAKE_CASE, output);
     * // output: {CATEGORY.NAME=Electronics, CATEGORY.SUBCATEGORY=Gadgets,
     * //          PRODUCT_NAME=WidgetPro} (sorted, uppercase)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param selectPropNames a collection of property names to be included in the output map.
     *        If {@code null}, all non-{@code null} properties are included. If empty, no properties
     *        are included. In selection mode, selected top-level properties are included
     *        even when {@code null}, while {@code null} properties inside nested beans are always omitted.
     *        <br><b>A selected nested bean can therefore contribute no key at all:</b> a flat map has no way to
     *        represent an empty nested object, so a nested bean whose every property is {@code null} simply
     *        disappears. Given a {@code User} whose {@code address} is non-{@code null} but whose
     *        {@code address.city} is {@code null}, {@code beanToFlatMap(user, ["address"])} returns {@code {}} -
     *        while the deep twin {@code deepBeanToMap(user, ["address"])} returns {@code {address={}}}. Use
     *        {@link Beans#mapBuilder(Object)}, whose null policy applies at every level, when a selected
     *        property must always appear: {@code mapBuilder(user).flat().select("address").toMap()} yields
     *        {@code {address.city=null}}.
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param output the Map instance into which the flattened bean properties will be put. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if a selected property does not exist, or if the traversed bean graph contains a
     *         reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void beanToFlatMap(final Object bean, final Collection<String> selectPropNames, final NamingPolicy keyNamingPolicy,
            final Map<String, Object> output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        beanToFlatMapSelected(bean, selectPropNames, true, keyNamingPolicy, output);
    }

    /**
     * The shared body of the selection-based {@code beanToFlatMap} overloads.
     *
     * @param bean the bean to flatten; {@code null} leaves {@code output} untouched
     * @param selectPropNames the properties to emit, or {@code null} for "all, minus nulls"
     * @param nestedIgnoreNullProperty whether {@code null} properties of a <i>nested</i> bean are dropped. The
     *        public selection overloads pass {@code true} (their documented behaviour); {@link BeanMapBuilder}
     *        passes its own {@code skipNulls} so that its null policy is the same at every level
     * @param keyNamingPolicy the policy applied to the keys; {@code null} means {@link NamingPolicy#CAMEL_CASE}
     * @param output the map being filled
     * @throws IllegalArgumentException if a selected property does not exist, or the traversed bean graph is cyclic
     */
    private static void beanToFlatMapSelected(final Object bean, final Collection<String> selectPropNames, final boolean nestedIgnoreNullProperty,
            NamingPolicy keyNamingPolicy, final Map<String, Object> output) throws IllegalArgumentException {
        if (bean == null) {
            return;
        }

        keyNamingPolicy = keyNamingPolicy == null ? NamingPolicy.CAMEL_CASE : keyNamingPolicy;
        final Class<?> beanClass = bean.getClass();
        final ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);

        if (selectPropNames == null) {
            beanToFlatMap(bean, nestedIgnoreNullProperty, null, keyNamingPolicy, output);
        } else {
            ParserUtil.PropInfo propInfo = null;

            // Two accepted spellings of one property ("firstName" and "first_name", say) resolve to the same
            // PropInfo and must be emitted once: the key is derived from propInfo.name, so the second write
            // only overwrote the first - but it still ran the getter a second time. BeanMapBuilder has
            // always de-duplicated this way; the direct overloads did not.
            final Set<String> seen = N.newHashSet(selectPropNames.size());

            for (final String propName : selectPropNames) {
                propInfo = beanInfo.getPropInfo(propName);

                if (propInfo == null) {
                    throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanClass);
                }

                if (!seen.add(propInfo.name)) {
                    continue;
                }

                // propInfo.name, not propName - see beanToMap(Object, Collection, NamingPolicy, Map). This
                // one also produced a self-inconsistent key: the parent segment came from the caller while
                // the nested segments came from the nested bean's PropInfos, so beanToFlatMap(u, ["ADDRESS"])
                // emitted "ADDRESS.city".
                putFlat(propInfo, propInfo.name, propInfo.getPropValue(bean), nestedIgnoreNullProperty, keyNamingPolicy, output);
            }
        }
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} property handling.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Properties with {@code null} values can be included or excluded based on the ignoreNullProperty parameter.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Include null properties
     * User user = new User("John", (Integer) null, new Address("NYC", (String) null));
     * // withNulls: {name=John, age=null, address.city=NYC, address.zipCode=null}
     * Map<String, Object> withNulls = Beans.beanToFlatMap(user, false);
     *
     * // Exclude null properties
     * Map<String, Object> noNulls = Beans.beanToFlatMap(user, true);
     * // noNulls: {name=John, address.city=NYC}
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, an empty map is returned.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the resulting map.
     * @return a flat map representation of the bean with {@code null} handling as specified; never {@code null}.
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final boolean ignoreNullProperty) throws IllegalArgumentException {
        return beanToFlatMap(bean, ignoreNullProperty, (Set<String>) null);
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} property handling and property exclusion.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines {@code null} value filtering with property name exclusion.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Filter nulls and exclude specific properties
     * User user = new User("John", (Integer) null, "secret123", new Address("NYC"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("password"));
     *
     * Map<String, Object> result = Beans.beanToFlatMap(user, true, ignored);
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
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames)
            throws IllegalArgumentException {
        return beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE);
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} handling, property exclusion, and Map type.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides flexibility in filtering and Map implementation.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Custom map with filtering
     * Employee emp = new Employee("John", null, "IT", new Office("Building A"));
     * Set<String> ignored = new HashSet<>(Arrays.asList("department"));
     *
     * TreeMap<String, Object> result = Beans.beanToFlatMap(emp, true, ignored,
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
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

        return beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, mapSupplier);
    }

    /**
     * Converts a bean object into a flat map representation with control over {@code null} handling, property exclusion, and key naming policy.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Provides comprehensive control over the flattening process.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
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
     * Map<String, Object> result = Beans.beanToFlatMap(profile, true, ignored,
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
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @return a flat map with comprehensive customization applied; never {@code null}.
     * @throws IllegalArgumentException if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static Map<String, Object> beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy) throws IllegalArgumentException {
        return beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, IntFunctions.ofLinkedHashMap());
    }

    /**
     * Converts a bean object into a flat map representation with full control over all conversion aspects.
     * Values from nested beans are set to the resulting map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This is the most flexible variant offering complete customization.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Complete customization example
     * Order order = new Order("ORD-123", null,
     *     new Customer("John", new Address("NYC", "10001")));
     * Set<String> ignored = new HashSet<>(Arrays.asList("internalNotes"));
     *
     * LinkedHashMap<String, Object> result = Beans.beanToFlatMap(order, true, ignored,
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
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param mapSupplier a function that creates a new Map instance. The function argument is the initial capacity.
     * @return a fully customized flat map representation of the bean; never {@code null}.
     * @throws IllegalArgumentException if {@code mapSupplier} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static <M extends Map<String, Object>> M beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
        N.checkArgNotNull(mapSupplier, cs.mapSupplier);

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
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Populate existing map with null filtering
     * Map<String, Object> output = new HashMap<>();
     * output.put("timestamp", new Date());
     *
     * User user = new User("John", (Integer) null, new Address("NYC"));
     * Beans.beanToFlatMap(user, true, output);
     * // output: {timestamp=..., name=John, address.city=NYC}
     * // (age null is excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param output the map into which the flattened bean properties will be put. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Map<String, Object> output) throws IllegalArgumentException {
        beanToFlatMap(bean, ignoreNullProperty, null, output);
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with filtering options.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * Combines in-place operation with {@code null} handling and property exclusion.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // In-place population with multiple filters
     * Map<String, Object> output = new LinkedHashMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("password", "ssn"));
     *
     * Account account = new Account("john123", "pass", null, "123-45-6789");
     * Beans.beanToFlatMap(account, true, ignored, output);
     * // output: {username=john123}
     * // (password and ssn ignored, balance null excluded)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames a set of property names to be excluded from the output map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @param output the map into which the flattened bean properties will be put. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames, final Map<String, Object> output)
            throws IllegalArgumentException {
        beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, NamingPolicy.CAMEL_CASE, output);
    }

    /**
     * Converts a bean object into a flat map representation and stores the result in the provided Map instance with full customization.
     * Values from nested beans are set to the map with property names concatenated with a dot, e.g., {@code "address.city"}.
     * This method provides complete control over the in-place flattening operation.
     *
     * <p><b>Cycles encountered while traversing nested-bean properties are rejected.</b> For example, a
     * bidirectional {@code parent <-> child} pair throws {@link IllegalArgumentException} when both
     * properties are traversed. Cycles inside values emitted unchanged are not inspected.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Full control in-place flattening
     * Map<String, Object> output = new TreeMap<>();
     * Set<String> ignored = new HashSet<>(Arrays.asList("metadata"));
     *
     * Document doc = new Document("Report", null,
     *     new Author("John", new Department("Research")));
     * Beans.beanToFlatMap(doc, true, ignored,
     *     NamingPolicy.SNAKE_CASE, output);
     * // output: {author.department.name=Research, author.name=John, title=Report}
     * // (sorted keys, snake_case, version null excluded, metadata ignored)
     * }</pre>
     *
     * @param bean the bean object to be converted into a flat map; if {@code null}, the output map is not modified.
     * @param ignoreNullProperty if {@code true}, properties with {@code null} values will not be included in the output map.
     * @param ignoredPropNames a set of property names to be excluded from the output map.
     *        Applies to TOP-LEVEL property names only; properties inside nested beans are not
     *        matched (dotted names such as {@code "address.city"} are not supported).
     * @param keyNamingPolicy the naming policy applied to map keys; if {@code null}, defaults to
     *        {@link NamingPolicy#CAMEL_CASE}. {@link NamingPolicy#CAMEL_CASE} and {@link NamingPolicy#NO_CHANGE}
     *        both emit the bean's property names unchanged &mdash; see the class documentation.
     * @param output the map into which the flattened bean properties will be put. Must not be {@code null}.
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if the traversed bean graph contains a reference cycle.
     * @see #beanToFlatMap(Object, Collection, NamingPolicy, IntFunction)
     */
    public static void beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final Map<String, Object> output) throws IllegalArgumentException {
        N.checkArgNotNull(output, cs.output);

        beanToFlatMap(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, null, output);
    }

    /**
     * @throws IllegalArgumentException if flattening encounters a cyclic bean reference.
     */
    private static void beanToFlatMap(final Object bean, final boolean ignoreNullProperty, final Collection<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final String parentPropName, final Map<String, Object> output) throws IllegalArgumentException {
        if (bean == null) {
            return;
        }

        // Cycle guard - see deepBeanToMap(Object, boolean, Set, NamingPolicy, Map).
        if (!enterDeepBean(bean)) {
            throw new IllegalArgumentException("Cyclic bean reference cannot be flattened: " + ClassUtil.getCanonicalClassName(bean.getClass()));
        }
        try {
            beanToFlatMapBody(bean, ignoreNullProperty, ignoredPropNames, keyNamingPolicy, parentPropName, output);
        } finally {
            exitDeepBean(bean);
        }
    }

    private static void beanToFlatMapBody(final Object bean, final boolean ignoreNullProperty, final Collection<String> ignoredPropNames,
            final NamingPolicy keyNamingPolicy, final String parentPropName, final Map<String, Object> output) {
        final boolean isCamelCaseOrNoChange = isVerbatimKeyPolicy(keyNamingPolicy);

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

            if ((propValue == null) || !isNestedBeanProp(propInfo)) {
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
     * properties (including {@code null}-valued ones, at every level) are converted, using
     * {@link NamingPolicy#CAMEL_CASE} keys and a {@link java.util.LinkedHashMap}, in shallow mode.</p>
     *
     * <p><b>This is not a drop-in replacement for {@link #beanToMap(Object)}:</b> {@code beanToMap(bean)} drops
     * {@code null}-valued properties, whereas the builder keeps them unless you call
     * {@link BeanMapBuilder#skipNulls()}. {@code Beans.mapBuilder(bean).skipNulls().toMap()} is the equivalent
     * of {@code Beans.beanToMap(bean)}. The builder's default was chosen to be "convert everything unless told
     * otherwise", so that each configuration method only ever removes properties.</p>
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
     * <p>Unlike the {@code deepBeanToMap}/{@code beanToFlatMap} overloads that take a {@code selectPropNames}
     * collection - which always drop {@code null} properties inside nested beans - this builder applies
     * {@link #skipNulls()} at every level, so with the default (nulls kept) a nested bean whose properties are
     * all {@code null} still appears in the result.</p>
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
         * @param propNames the property names to include; {@code null} clears the selection.
         * @return this builder.
         */
        public final BeanMapBuilder select(final String... propNames) {
            return select(propNames == null ? null : java.util.Arrays.asList(propNames));
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
         * Excludes the specified properties from the conversion. Applies to <b>top-level</b> property names
         * only: under {@link #deep()} or {@link #flat()} a nested bean's property of the same name is still
         * emitted. Calling this replaces any previously specified exclusions.
         *
         * @param propNames the property names to exclude; {@code null} or empty clears the exclusions.
         * @return this builder.
         */
        public final BeanMapBuilder exclude(final String... propNames) {
            return exclude(propNames == null ? null : java.util.Arrays.asList(propNames));
        }

        /**
         * Excludes the specified properties from the conversion. Applies to <b>top-level</b> property names
         * only: under {@link #deep()} or {@link #flat()} a nested bean's property of the same name is still
         * emitted. Calling this replaces any previously specified exclusions.
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
         * @throws IllegalArgumentException if {@code propFilter} is {@code null}.
         */
        public BeanMapBuilder filter(final BiPredicate<? super String, Object> propFilter) throws IllegalArgumentException {
            N.checkArgNotNull(propFilter, cs.propFilter);

            this.propFilter = propFilter;
            return this;
        }

        /**
         * Drops properties whose value is {@code null}, at the top level and inside nested beans alike. By
         * default {@code null}-valued properties are kept.
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
         * Converts properties declared as nested bean types recursively into nested maps. Mutually exclusive with
         * {@link #flat()}: calling both (in either order) throws {@link IllegalStateException}. Calling
         * {@code deep()} more than once is harmless.
         *
         * @return this builder.
         * @throws IllegalStateException if {@link #flat()} has already been called on this builder.
         */
        public BeanMapBuilder deep() throws IllegalStateException {
            if (shape == Shape.FLAT) {
                throw new IllegalStateException("flat() was already called; deep() and flat() are mutually exclusive");
            }

            shape = Shape.DEEP;
            return this;
        }

        /**
         * Flattens properties declared as nested bean types into the resulting map using dot-separated keys (e.g.
         * {@code "address.city"}). Mutually exclusive with {@link #deep()}: calling both (in either order)
         * throws {@link IllegalStateException}. Calling {@code flat()} more than once is harmless.
         *
         * @return this builder.
         * @throws IllegalStateException if {@link #deep()} has already been called on this builder.
         */
        public BeanMapBuilder flat() throws IllegalStateException {
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
         * @throws IllegalArgumentException if a property selected via {@code select(...)} is not found in the bean class.
         */
        public Map<String, Object> toMap() throws IllegalArgumentException {
            return toMap(IntFunctions.ofLinkedHashMap());
        }

        /**
         * Performs the conversion into a map created by the given supplier. Under {@link #deep()} the supplier
         * also creates each nested bean's map.
         *
         * @param <M> the map type.
         * @param mapSupplier a function that creates a new map given an initial capacity.
         * @return the created map with the converted properties; never {@code null}.
         * @throws IllegalArgumentException if a property selected via {@code select(...)} is not found in the bean class, or if
         *         {@code mapSupplier} is {@code null}.
         */
        public <M extends Map<String, Object>> M toMap(final IntFunction<? extends M> mapSupplier) throws IllegalArgumentException {
            N.checkArgNotNull(mapSupplier, cs.mapSupplier);

            final Selection selection = select();
            final M output = mapSupplier.apply(selection == null ? 0 : selection.names().size());
            fill(selection, mapSupplier, output);
            return output;
        }

        /**
         * Performs the conversion into the provided map, which is returned. Existing entries are preserved unless
         * overwritten by a converted key. Under {@link #deep()}, nested beans become
         * {@link java.util.LinkedHashMap}s, since this terminal is given a map rather than a supplier; use
         * {@link #toMap(IntFunction)} to control the nested map type as well.
         *
         * @param <M> the map type.
         * @param output the map to fill; must not be {@code null}.
         * @return {@code output}.
         * @throws IllegalArgumentException if {@code output} is {@code null}, or if a property selected via
         *         {@code select(...)} is not found in the bean class.
         */
        public <M extends Map<String, Object>> M into(final M output) throws IllegalArgumentException {
            N.checkArgNotNull(output, cs.output);

            fill(select(), null, output);

            return output;
        }

        /**
         * The properties this builder will emit, in order, plus the values already read for them.
         *
         * <p>{@code values} is {@code null} unless {@code skipNulls} or a {@code propFilter} forced the
         * getters to be read during selection; when it is non-{@code null} it is index-aligned with
         * {@code names}. The names are <b>canonical</b> and de-duplicated: two accepted
         * spellings of one property - {@code select("firstName", "FirstName")} - contribute a single entry,
         * read once.</p>
         *
         * @param names the canonical names of the properties to emit, in selection order, without repeats
         * @param values the corresponding values, or {@code null} if no getter has been read yet
         */
        private record Selection(List<String> names, List<Object> values) {
        }

        /**
         * Resolves which properties to emit, reading each getter <b>at most once</b>.
         *
         * <p>Reading once matters for more than speed: this used to decide inclusion from one read and then
         * let the conversion read the getter a second time, so a getter whose value changed between the two
         * could report non-{@code null} to {@code skipNulls} and then store a {@code null}.</p>
         *
         * @return the resolved selection, or {@code null} if there is no bean to convert
         * @throws IllegalArgumentException if a name passed to {@code select(...)} is not a property
         */
        private Selection select() throws IllegalArgumentException {
            if (bean == null) {
                return null;
            }

            final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
            final boolean needValue = skipNulls || propFilter != null;
            final List<String> names = new ArrayList<>();
            final List<Object> values = needValue ? new ArrayList<>() : null;
            final Set<String> excludes = canonicalExcludes(beanInfo);

            if (selectPropNames == null) {
                // propInfoList cannot contain the same property twice, so no de-duplication is needed here.
                for (final PropInfo propInfo : beanInfo.propInfoList) {
                    accept(propInfo, needValue, excludes, null, names, values);
                }
            } else {
                // Two accepted spellings of one property (say "firstName" and "first_name") resolve to the
                // same PropInfo and must contribute one entry: without this the getter would be read twice
                // and a propFilter consulted twice for the same value.
                final Set<String> seen = N.newHashSet(selectPropNames.size());

                for (final String propName : selectPropNames) {
                    final PropInfo propInfo = beanInfo.getPropInfo(propName);

                    if (propInfo == null) {
                        throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + bean.getClass());
                    }

                    accept(propInfo, needValue, excludes, seen, names, values);
                }
            }

            return new Selection(names, values);
        }

        /**
         * Resolves the excluded names to canonical property names, so an exclusion matches whichever
         * spelling the caller used.
         *
         * <p>{@code exclude} used to be compared against whatever spelling {@code select} had recorded,
         * which made it mean opposite things in the two paths: with no {@code select},
         * {@code exclude("first_name")} silently did nothing while {@code exclude("firstName")} worked;
         * after {@code select("first_name")} it was exactly reversed.</p>
         *
         * <p>An exclusion that matches no property is kept as written rather than rejected - {@code exclude}
         * is a filter, not a selection, so an unmatched name simply excludes nothing.</p>
         */
        private Set<String> canonicalExcludes(final BeanInfo beanInfo) {
            if (excludePropNames == null) {
                return null;
            }

            final Set<String> canonical = N.newHashSet(excludePropNames.size());

            for (final String propName : excludePropNames) {
                final PropInfo propInfo = beanInfo.getPropInfo(propName);

                canonical.add(propInfo == null ? propName : propInfo.name);
            }

            return canonical;
        }

        private void accept(final PropInfo propInfo, final boolean needValue, final Set<String> excludes, final Set<String> seen, final List<String> names,
                final List<Object> values) {
            // Always the canonical name: it is what becomes the map key, what `excludes` is resolved to, and
            // what a propFilter is handed - so a filter written against property names keeps matching after
            // a select(..) is added.
            final String propName = propInfo.name;

            if (seen != null && !seen.add(propName)) {
                return;
            }

            if (excludes != null && excludes.contains(propName)) {
                return;
            }

            if (!needValue) {
                names.add(propName);
                return;
            }

            final Object value = propInfo.getPropValue(bean);

            if ((skipNulls && value == null) || (propFilter != null && !propFilter.test(propName, value))) {
                return;
            }

            names.add(propName);
            values.add(value);
        }

        /**
         * Emits the resolved selection into {@code output}.
         *
         * <p>{@code skipNulls} is carried into the nested levels, not applied only to the top one. The
         * per-property helpers used to hard-code "drop nested nulls", which contradicted this builder's
         * documented default of converting every property: under {@link #flat()} a nested bean whose
         * properties were all {@code null} disappeared from the result entirely, and under {@link #deep()} it
         * came back as an empty map.</p>
         *
         * @param selection the properties (and, when already read, the values) to emit
         * @param nestedMapSupplier builds nested maps under {@link #deep()}; {@code null} means
         *        {@link java.util.LinkedHashMap}
         * @param output the map being filled
         */
        private void fill(final Selection selection, final IntFunction<? extends Map<String, Object>> nestedMapSupplier, final Map<String, Object> output) {
            if (bean == null) {
                return;
            }

            if (selection.values() == null) {
                // No getter has been read yet, so delegating still reads each one exactly once.
                switch (shape) {
                    case DEEP:
                        deepBeanToMapSelected(bean, selection.names(), skipNulls, namingPolicy, nestedMapSupplier, output);
                        break;
                    case FLAT:
                        beanToFlatMapSelected(bean, selection.names(), skipNulls, namingPolicy, output);
                        break;
                    default:
                        beanToMap(bean, selection.names(), namingPolicy, output);
                }

                return;
            }

            // Emit the captured values through the same per-property helpers the conversion methods use, so
            // this path cannot drift from them.
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

            for (int i = 0, size = selection.names().size(); i < size; i++) {
                final String propName = selection.names().get(i);
                final Object propValue = selection.values().get(i);

                switch (shape) {
                    case DEEP:
                        putDeep(beanInfo.getPropInfo(propName), propName, propValue, skipNulls, namingPolicy, nestedMapSupplier, output);
                        break;
                    case FLAT:
                        putFlat(beanInfo.getPropInfo(propName), propName, propValue, skipNulls, namingPolicy, output);
                        break;
                    default:
                        output.put(convertMapKey(propName, namingPolicy), propValue);
                }
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
     * @param targetType the class to instantiate; must not be {@code null} and must have an accessible
     *        no-argument constructor.
     * @return a new instance of the specified class; never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or the class cannot be
     *         instantiated (e.g., abstract, no accessible no-arg constructor).
     */
    public static <T> T newBean(final Class<T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(targetType, cs.targetType);

        return N.newInstance(targetType);
    }

    /**
     * Remembers, per class, that Kryo could not copy it, so the next copy of the same class does not pay for
     * the failed attempt again.
     *
     * <p><b>Why {@link ClassValue} and not a {@code Set<Class<?>>}:</b> a {@code static} set of {@code Class}
     * objects is never evicted and therefore pins every class it holds - and its {@code ClassLoader} - for the
     * life of the JVM. {@code ClassValue} stores its value on the class itself, so the entry dies with the
     * class. (A {@code WeakHashMap<Class<?>, ?>} would <em>not</em> work here for the general case: a value
     * that refers back to its key keeps the weak entry alive.)</p>
     *
     * <p><b>Why the two flags are separate:</b> {@code deepCopy} and {@code shallowCopy} exercise different
     * amounts of the object graph, so a class Kryo cannot deep-copy may still be shallow-copyable. One shared
     * flag disabled both paths on either failure.</p>
     */
    private static final ClassValue<KryoSupport> kryoSupport = new ClassValue<>() {
        @Override
        protected KryoSupport computeValue(final Class<?> type) {
            return new KryoSupport();
        }
    };

    /** Per-class Kryo fallback state; see {@link #kryoSupport}. */
    private static final class KryoSupport {
        private volatile boolean deepCopyUnsupported = false;

        private volatile boolean shallowCopyUnsupported = false;
    }

    /**
     * Creates a deep clone of the given object using Kryo copying or an XML round trip.
     *
     * <p>This method first attempts Kryo's object-copy operation when available, then falls back
     * to serializing and deserializing XML. Both mechanisms copy nested objects as supported by
     * their serializers; objects treated as immutable may be shared.</p>
     *
     * <p>The object must support Kryo copying or XML serialization. If the Kryo copy
     * throws a {@link RuntimeException}, the method falls back to XML serialization.</p>
     *
     * <p>Kryo is used only for a same-class copy when Kryo is on the classpath, and that path
     * preserves cycles and shared identity. {@link #deepCopyAs(Object, Class)} to a different type
     * always uses XML. The XML round-trip does not restore cyclic graphs and is capped at the
     * parser's serialization depth (256); a cycle typically surfaces as a parse/serialization
     * exception rather than a restored back-reference.</p>
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
     * @param obj the object to clone; must support Kryo copying or Abacus XML serialization.
     * @return a deep clone of the object, or {@code null} if {@code obj} is {@code null}.
     * @throws RuntimeException if the object cannot be copied by either mechanism. Kryo failures are absorbed
     *         (the XML round trip is tried next), but a failure of the XML round trip itself propagates as
     *         whatever the parser raises - typically a {@code ParseException}, or an
     *         {@link Error} such as {@code NoClassDefFoundError} when the XML binding API is absent.
     * @see #deepCopyAs(Object, Class)
     */
    @MayReturnNull
    @SuppressWarnings("unchecked")
    public static <T> T deepCopy(final T obj) throws RuntimeException {
        if (obj == null) {
            return null; // NOSONAR
        }

        return (T) deepCopyAs(obj, obj.getClass());
    }

    /**
     * Creates a deep clone of the given object and converts it to the specified target type.
     *
     * <p>A same-class copy may use Kryo's object-copy operation. Otherwise, this method serializes
     * the object to XML and deserializes it as an instance of the target type. This is useful for
     * creating type-converted copies or for ensuring type safety when cloning objects.</p>
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
     * User empty = Beans.deepCopyAs(null, User.class);   // returns a new empty User
     *
     * Beans.deepCopyAs(user, null);                      // throws IllegalArgumentException
     * }</pre>
     *
     * @param <T> the type of the target object.
     * @param obj the source object; if {@code null}, a new empty instance of {@code targetType} is returned.
     * @param targetType the class of the target type to create; must not be {@code null}.
     * @return a new instance of the target type populated from the source object; never {@code null}.
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if the object cannot be copied by either mechanism - see {@link #deepCopy(Object)}.
     *         A Kryo failure is absorbed and remembered per class so it is not retried; an {@link Error} raised by
     *         Kryo is <em>not</em> absorbed, because an {@code OutOfMemoryError} or {@code StackOverflowError}
     *         says nothing about the class and must reach the caller.
     */
    @SuppressWarnings("unchecked")
    public static <T> T deepCopyAs(final Object obj, @NotNull final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
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

        if (Utils.kryoParser != null && targetType.equals(srcCls) && !kryoSupport.get(srcCls).deepCopyUnsupported) {
            try {
                copy = Utils.kryoParser.deepCopy(obj);
            } catch (final RuntimeException e) {
                // RuntimeException only, never Throwable: Kryo signals "I cannot handle this shape" with a
                // RuntimeException, and that verdict is a property of the class, so it is worth remembering.
                // An Error is not - an OutOfMemoryError or StackOverflowError raised while copying one large
                // graph says nothing about the class, and swallowing it used to disable the fast path for
                // that class permanently while hiding a real VM problem from the caller.
                kryoSupport.get(srcCls).deepCopyUnsupported = true;

                // Fall through to the XML round-trip below.
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
     * <p><b>Non-property state:</b> because nothing here narrows the copy, this overload can take the optimized
     * whole-object shortcut described in {@link #copyAs(Object, Class)} - and so can
     * {@link #copy(Object, Collection)} with a {@code null} selection and {@link #copy(Object, BiPredicate)} with
     * {@link BiPredicates#alwaysTrue()}. That shortcut also carries fields which are <i>not</i> properties (a
     * private field with no accessor, for instance), and it is only available when the optional Kryo library
     * initializes successfully. Do not rely on non-property field state being either preserved or reset; pass an
     * explicit selection when only the properties must be copied.</p>
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
     * @return a new instance of the same class as {@code sourceBean} with all properties copied,
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
     * @return a new instance of the same class as {@code sourceBean} with the selected properties copied,
     *         or {@code null} if {@code sourceBean} is {@code null}.
     * @throws IllegalArgumentException if a selected property is not found in the bean class.
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final Collection<String> selectPropNames) throws IllegalArgumentException {
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
     * @return a new instance of the same class as {@code sourceBean} with the properties that pass
     *         {@code propFilter} copied, or {@code null} if {@code sourceBean} is {@code null}.
     * @throws IllegalArgumentException if {@code propFilter} is {@code null}.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    @MayReturnNull
    public static <T> T copy(final T sourceBean, final BiPredicate<? super String, Object> propFilter) throws IllegalArgumentException {
        N.checkArgNotNull(propFilter, cs.propFilter);

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
     * Beans.copyAs(null, User.class);                    // returns a new empty (non-null) instance
     * Beans.copyAs(user, (Class<User>) null);            // throws IllegalArgumentException
     * }</pre>
     *
     * <p><b>Non-property state:</b> when {@code targetType} is the source's own class and no property
     * selection or name conversion is requested, an optimized whole-object copy may be used instead of a
     * property-by-property copy. That copy also carries fields which are <i>not</i> properties, and it is
     * only available when the optional Kryo library initializes successfully. Do not rely on non-property
     * field state being either preserved or reset by this method.
     * <br>The same shortcut is reachable from every {@code copyAs} overload that ends up asking for "all
     * properties, unrenamed, into the source's own class": a {@code null} {@code selectPropNames}, a
     * {@code propFilter} of {@link BiPredicates#alwaysTrue()}, and a {@code propNameConverter} of
     * {@link Fn#identity()}. It is <em>not</em> reachable from
     * {@link #copyAs(Object, boolean, Set, Class)}, which must skip {@code null}/default source values and
     * so always copies property by property.</p>
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a selected property is not found
     *         in the source bean or in the target bean.
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a selected property is not found
     *         in the source bean or its (converted) name is not found in the target bean, or if
     *         {@code propNameConverter} is {@code null}.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    @SuppressWarnings("unchecked")
    public static <T> T copyAs(final Object sourceBean, final Collection<String> selectPropNames, final Function<String, String> propNameConverter,
            @NotNull final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(propNameConverter, cs.propNameConverter);
        N.checkArgNotNull(targetType, cs.targetType);

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            // The Kryo shortcut copies properties verbatim, so it must not be taken when a
            // non-identity propNameConverter is supposed to remap property names.
            if (selectPropNames == null && propNameConverter == Fn.<String> identity() && Utils.kryoParser != null && targetType.equals(srcCls)
                    && !kryoSupport.get(srcCls).shallowCopyUnsupported) {
                try {
                    final T copy = (T) Utils.kryoParser.shallowCopy(sourceBean);

                    if (copy != null) {
                        return copy;
                    }
                } catch (final RuntimeException e) {
                    // See deepCopyAs(Object, Class): a RuntimeException is Kryo's structural verdict on the
                    // class and is remembered; an Error is instance-specific and must reach the caller.
                    kryoSupport.get(srcCls).shallowCopyUnsupported = true;

                    // Fall through to the property-by-property copy below.
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
     * <p><b>A filter is a selection, not a sieve.</b> Unlike {@link #copyAs(Object, Class)} - which silently
     * skips a source property the target does not have, and is what makes copying into a narrower DTO work -
     * a property that <i>passes</i> the filter is treated as explicitly requested, so it must exist on the
     * target too or an {@link IllegalArgumentException} is thrown. Exclude such properties in the filter
     * itself, or drop the filter, if that is not what you want.</p>
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a source property that passes
     *         the filter has no matching property in the target bean, or if {@code propFilter} is {@code null}.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T copyAs(final Object sourceBean, final BiPredicate<? super String, Object> propFilter, final Class<? extends T> targetType)
            throws IllegalArgumentException {
        N.checkArgNotNull(propFilter, cs.propFilter);

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
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if a source property that passes
     *         the filter has no matching (converted) property name in the target bean, or if any of
     *         {@code propFilter}, {@code propNameConverter} is {@code null}.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T copyAs(final Object sourceBean, final BiPredicate<? super String, Object> propFilter, final Function<String, String> propNameConverter,
            final Class<? extends T> targetType) throws IllegalArgumentException {
        N.checkArgNotNull(propFilter, cs.propFilter);
        N.checkArgNotNull(propNameConverter, cs.propNameConverter);
        N.checkArgNotNull(targetType, cs.targetType);

        if (sourceBean != null) {
            final Class<?> srcCls = sourceBean.getClass();

            // The Kryo shortcut copies properties verbatim, so it must not be taken when a
            // non-identity propNameConverter is supposed to remap property names.
            if (propFilter == BiPredicates.alwaysTrue() && propNameConverter == Fn.<String> identity() && Utils.kryoParser != null && targetType.equals(srcCls)
                    && !kryoSupport.get(srcCls).shallowCopyUnsupported) {
                try {
                    final T copy = (T) Utils.kryoParser.shallowCopy(sourceBean);

                    if (copy != null) {
                        return copy;
                    }
                } catch (final RuntimeException e) {
                    // See deepCopyAs(Object, Class): a RuntimeException is Kryo's structural verdict on the
                    // class and is remembered; an Error is instance-specific and must reach the caller.
                    kryoSupport.get(srcCls).shallowCopyUnsupported = true;

                    // Fall through to the property-by-property copy below.
                }
            }
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetType);
        Object result = targetBeanInfo.createBeanResult();

        if (sourceBean != null) {
            mergeIntoIf(sourceBean, result, propFilter, propNameConverter, Fn.selectFirst(), targetBeanInfo);
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
     * @throws IllegalArgumentException if {@code targetType} is {@code null}, or if {@code ignoreUnmatchedProperty}
     *         is {@code false} and an unmatched property with a non-{@code null} source value is found.
     */
    @SuppressWarnings("unchecked")
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

    /**
     * @throws UnsupportedOperationException if a non-null source would be merged into a finished immutable or builder-based target bean
     * @throws IllegalArgumentException if a source or target property cannot be resolved or assigned under the requested unmatched-property policy
     * @throws RuntimeException if inspecting the source bean or reading, converting, or writing a selected property fails
     */
    @SuppressWarnings("deprecation")
    private static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty,
            final Set<String> ignoredPropNames, final BeanInfo targetBeanInfo)
            throws UnsupportedOperationException, IllegalArgumentException, RuntimeException {
        if (sourceBean == null) {
            return targetBean;
        }

        // A finished record/immutable/builder-based target cannot be written to. Checked here rather than in
        // each public overload because every mergeInto funnels through these three; the copy family reaches
        // them with createBeanResult()'s constructor-argument array, which checkInPlaceWritable lets through.
        checkInPlaceWritable(targetBeanInfo, targetBean);

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

    // Fn.selectFirst() is a singleton (Fn.RETURN_FIRST), so identity is a reliable test. The whole copy family
    // merges with it, and it never looks at its second argument - which lets the merge loops below skip the
    // target read entirely. That matters beyond speed: createBeanResult() hands back a BUILDER for a
    // builder-based target, and reading a bean property off a builder throws (see checkInPlaceWritable).
    private static final BinaryOperator<?> SELECT_FIRST_MERGE_FUNC = Fn.selectFirst();

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
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     */
    public static <T> T mergeInto(final Object sourceBean, final T targetBean) throws IllegalArgumentException, UnsupportedOperationException {
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
     * // Sum numeric properties; otherwise keep the source value.
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
     *        the value to set on the target.
     * @return {@code targetBean} with merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if {@code mergeFunc} is
     *         {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, final T targetBean, final BinaryOperator<?> mergeFunc)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(mergeFunc, cs.mergeFunc);

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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if {@code ignoreUnmatchedProperty}
     *         is {@code false} and an unmatched property is found.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty,
            final Set<String> ignoredPropNames) throws IllegalArgumentException, UnsupportedOperationException {
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if {@code ignoreUnmatchedProperty}
     *         is {@code false} and an unmatched property is found, or if {@code mergeFunc} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final boolean ignoreUnmatchedProperty,
            final Set<String> ignoredPropNames, final BinaryOperator<?> mergeFunc) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(targetBean, cs.targetBean);
        N.checkArgNotNull(mergeFunc, cs.mergeFunc);

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());
        checkInPlaceWritable(targetBeanInfo, targetBean);

        final BinaryOperator<Object> objMergeFunc = (BinaryOperator<Object>) mergeFunc;

        // Two passes - see MergeStep: with ignoreUnmatchedProperty == false the first unmatched property must
        // not leave the caller's target half-merged.
        final List<MergeStep> steps = new ArrayList<>(srcBeanInfo.propInfoList.size());
        PropInfo targetPropInfo = null;

        for (final PropInfo propInfo : srcBeanInfo.propInfoList) {
            if (ignoredPropNames == null || !ignoredPropNames.contains(propInfo.name)) {
                targetPropInfo = targetBeanInfo.getPropInfo(propInfo);

                if (targetPropInfo == null) {
                    if (!ignoreUnmatchedProperty) {
                        throw new IllegalArgumentException("No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                    }
                } else {
                    steps.add(new MergeStep(targetPropInfo, propInfo.getPropValue(sourceBean)));
                }
            }
        }

        applyMergeSteps(steps, targetBean, objMergeFunc);

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
     * <p>When source and target are the same bean, matching source values are read before any target property
     * is written, including when the converter exchanges property names. For distinct beans, matched values
     * are applied as they are read; a failure in user code or a setter may leave earlier assignments in place.</p>
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if any of
     *         {@code propNameConverter}, {@code mergeFunc} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, final T targetBean, final Function<String, String> propNameConverter,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(targetBean, cs.targetBean);
        N.checkArgNotNull(propNameConverter, cs.propNameConverter);
        N.checkArgNotNull(mergeFunc, cs.mergeFunc);

        if (sourceBean == null) {
            return targetBean;
        }

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        return mergeInto(sourceBean, targetBean, (Collection<String>) null, propNameConverter, mergeFunc, targetBeanInfo);
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property is not found
     *         in the source bean or in the target bean.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames)
            throws IllegalArgumentException, UnsupportedOperationException {
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property is not found
     *         in the source bean or in the target bean, or if {@code mergeFunc} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(mergeFunc, cs.mergeFunc);

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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property is not found
     *         in the source bean or in the target bean, or if {@code propNameConverter} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(targetBean, cs.targetBean);
        N.checkArgNotNull(propNameConverter, cs.propNameConverter);

        return mergeInto(sourceBean, targetBean, selectPropNames, propNameConverter, DEFAULT_MERGE_FUNC);
    }

    /**
     * Merges selected properties from the source bean into the target bean with property name conversion
     * and a custom merge function.
     *
     * <p>This method provides the most flexible selective merging, combining property selection,
     * name conversion, and custom merge logic.</p>
     *
     * <p>Matching source values are read before any target property is written when properties are explicitly
     * selected or source and target are the same bean. With {@code selectPropNames == null} and distinct beans,
     * matched values are applied as they are read. A failure in user code or a setter may leave earlier
     * assignments in place.</p>
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a selected property is not found
     *         in the source bean or in the target bean, or if any of {@code propNameConverter}, {@code mergeFunc} is
     *         {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeInto(final Object sourceBean, @NotNull final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(targetBean, cs.targetBean);
        N.checkArgNotNull(propNameConverter, cs.propNameConverter);
        N.checkArgNotNull(mergeFunc, cs.mergeFunc);

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        return mergeInto(sourceBean, targetBean, selectPropNames, propNameConverter, mergeFunc, targetBeanInfo);
    }

    /**
     * @throws IllegalArgumentException if a selected property is absent from the source or target bean.
     */
    private static <T> T mergeInto(final Object sourceBean, final T targetBean, final Collection<String> selectPropNames,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc, final BeanInfo targetBeanInfo)
            throws IllegalArgumentException {
        if (sourceBean == null) {
            return targetBean;
        }

        // A finished record/immutable/builder-based target cannot be written to. Checked here rather than in
        // each public overload because every mergeInto funnels through these three; the copy family reaches
        // them with createBeanResult()'s constructor-argument array, which checkInPlaceWritable lets through.
        checkInPlaceWritable(targetBeanInfo, targetBean);

        final boolean isIdentityPropNameConverter = propNameConverter == Fn.<String> identity();
        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BinaryOperator<Object> objMergeFunc = (BinaryOperator<Object>) mergeFunc;

        String targetPropName = null;
        PropInfo targetPropInfo = null;

        if (selectPropNames == null) {
            // A self-merge must read all source values before a renamed assignment can overwrite one.
            // Distinct beans retain the direct-copy path without a list and one record per property.
            final List<MergeStep> steps = sourceBean == targetBean ? new ArrayList<>(srcBeanInfo.propInfoList.size()) : null;
            final boolean selectFirst = steps == null && objMergeFunc == SELECT_FIRST_MERGE_FUNC;

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
                    // unmatched source properties are deliberately skipped on this path
                    // (the selectPropNames-based overload throws for explicitly selected names)
                } else if (steps != null) {
                    steps.add(new MergeStep(targetPropInfo, propInfo.getPropValue(sourceBean)));
                } else {
                    final Object sourceValue = propInfo.getPropValue(sourceBean);
                    targetPropInfo.setPropValue(targetBean,
                            selectFirst ? sourceValue : objMergeFunc.apply(sourceValue, targetPropInfo.getPropValue(targetBean)));
                }
            }

            if (steps != null) {
                applyMergeSteps(steps, targetBean, objMergeFunc);
            }
        } else {
            // Two passes: an explicitly selected name that is missing from either bean must not leave the
            // caller's target half-merged. Resolving first also reads each source getter exactly once.
            final List<MergeStep> steps = new ArrayList<>(selectPropNames.size());
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
                    // An explicitly selected name must exist on both sides; only the null-selection branch
                    // above is allowed to skip a source property the target does not have.
                    throw new IllegalArgumentException("No property found by name: " + propName + " in target bean class: " + targetBean.getClass()); //NOSONAR
                }

                steps.add(new MergeStep(targetPropInfo, propInfo.getPropValue(sourceBean)));
            }

            applyMergeSteps(steps, targetBean, objMergeFunc);
        }

        return targetBean;
    }

    /**
     * One resolved source-value-to-target-property assignment, queued when selected properties need
     * prevalidation or a self-merge could overwrite unread source values. Collecting the whole plan before
     * the first {@code setPropValue} also prevents resolution failures from leaving a partially updated target.
     * A failure while applying the plan can still leave earlier assignments in place.
     *
     * @param targetPropInfo the property to write on the target bean
     * @param sourceValue the value already read from the source bean
     */
    private record MergeStep(PropInfo targetPropInfo, Object sourceValue) {
    }

    /**
     * Writes a resolved merge plan onto the target bean. The target value is read here rather than during
     * resolution, so a merge function still sees the target's state as of its own assignment.
     *
     * @param steps the resolved assignments, in source-property order
     * @param targetBean the bean being merged into
     * @param mergeFunc receives {@code (sourceValue, targetValue)} and returns the value to write
     */
    private static void applyMergeSteps(final List<MergeStep> steps, final Object targetBean, final BinaryOperator<Object> mergeFunc) {
        // See SELECT_FIRST_MERGE_FUNC: selectFirst ignores the target value, so reading it is not just wasted
        // work - it is what made copyAs/copy throw for a builder-based target.
        final boolean selectFirst = mergeFunc == SELECT_FIRST_MERGE_FUNC;

        for (final MergeStep step : steps) {
            step.targetPropInfo()
                    .setPropValue(targetBean,
                            selectFirst ? step.sourceValue() : mergeFunc.apply(step.sourceValue(), step.targetPropInfo().getPropValue(targetBean)));
        }
    }

    /**
     * Merges properties from the source bean into the target bean based on a filter predicate.
     *
     * <p>The predicate receives each property name and value from the source bean and
     * determines whether that property should be merged into the target.
     * The default merge strategy is null-preserving: a non-{@code null} source value replaces the
     * target value, while a {@code null} source value leaves the existing target value unchanged.</p>
     *
     * <p><b>A filter is a selection, not a sieve.</b> {@link #mergeInto(Object, Object)} and the
     * {@code mergeFunc}-only overload silently skip a source property the target does not have; a property
     * that <i>passes</i> this filter is instead treated as explicitly requested, so it must exist on the
     * target or an {@link IllegalArgumentException} is thrown - the same rule an explicit
     * {@code selectPropNames} collection follows.</p>
     *
     * <p><b>Why {@code mergeIntoIf} rather than another {@code mergeInto} overload:</b> this family used to
     * be named {@code mergeInto}, which made {@code mergeInto(src, tgt, (a, b) -> a)} an "ambiguous method
     * call" compile error. A {@link BiPredicate} and a {@link BinaryOperator} are both two-argument
     * functional interfaces, so with an implicitly typed lambda - which is not pertinent to applicability
     * (JLS 15.12.2.2) - both three-argument overloads were applicable and neither was more specific. The
     * only way to call either was to wrap the lambda in {@code Fn.p(..)} or {@code Fn.o(..)}. The distinct
     * name removes the collision, so both {@code mergeInto(src, tgt, (a, b) -> a)} and
     * {@code mergeIntoIf(src, tgt, (n, v) -> v != null)} now compile as written.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User source = new User("John", 0, null);
     * User target = new User("Jane", 25, "jane@example.com");
     *
     * // Only merge non-null and non-zero values.
     * Beans.mergeIntoIf(source, target,
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes the
     *         filter has no matching property in the target bean, or if {@code propFilter} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeIntoIf(final Object sourceBean, final T targetBean, final BiPredicate<? super String, Object> propFilter)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(propFilter, cs.propFilter);

        return mergeIntoIf(sourceBean, targetBean, propFilter, DEFAULT_MERGE_FUNC);
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
     * Beans.mergeIntoIf(source, target,
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes the
     *         filter has no matching property in the target bean, or if any of {@code propFilter},
     *         {@code mergeFunc} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeIntoIf(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final BinaryOperator<?> mergeFunc) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(propFilter, cs.propFilter);
        N.checkArgNotNull(mergeFunc, cs.mergeFunc);

        return mergeIntoIf(sourceBean, targetBean, propFilter, Fn.identity(), mergeFunc);
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
     * Beans.mergeIntoIf(source, target,
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
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes the
     *         filter has no matching property in the target bean, or if any of {@code propFilter},
     *         {@code propNameConverter} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeIntoIf(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final Function<String, String> propNameConverter) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(propFilter, cs.propFilter);
        N.checkArgNotNull(propNameConverter, cs.propNameConverter);

        return mergeIntoIf(sourceBean, targetBean, propFilter, propNameConverter, DEFAULT_MERGE_FUNC);
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
     * Beans.mergeIntoIf(source, target,
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
     *        merge the property into the target.
     * @param propNameConverter a function that converts each source property name to the corresponding
     *        target property name; use {@link Fn#identity()} to keep names unchanged.
     * @param mergeFunc a binary operator that receives {@code (sourceValue, targetValue)} and returns
     *        the value to set on the target.
     * @return {@code targetBean} with filtered, name-converted, and merged properties applied.
     * @throws IllegalArgumentException if {@code targetBean} is {@code null}, or if a property that passes the
     *         filter has no matching property in the target bean, or if any of {@code propFilter},
     *         {@code propNameConverter}, {@code mergeFunc} is {@code null}.
     * @throws UnsupportedOperationException if {@code targetBean} is an instance of a class treated as an immutable
     *         bean (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); it cannot be merged into in place.
     * @see Fn#identity()
     * @see Fn#selectFirst()
     */
    public static <T> T mergeIntoIf(final Object sourceBean, @NotNull final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc)
            throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(targetBean, cs.targetBean);
        N.checkArgNotNull(propFilter, cs.propFilter);
        N.checkArgNotNull(propNameConverter, cs.propNameConverter);
        N.checkArgNotNull(mergeFunc, cs.mergeFunc);

        final BeanInfo targetBeanInfo = ParserUtil.getBeanInfo(targetBean.getClass());

        return mergeIntoIf(sourceBean, targetBean, propFilter, propNameConverter, mergeFunc, targetBeanInfo);
    }

    /**
     * @throws IllegalArgumentException if a selected source property has no matching target property.
     */
    private static <T> T mergeIntoIf(final Object sourceBean, final T targetBean, final BiPredicate<? super String, Object> propFilter,
            final Function<String, String> propNameConverter, final BinaryOperator<?> mergeFunc, final BeanInfo targetBeanInfo)
            throws IllegalArgumentException {
        if (sourceBean == null) {
            return targetBean;
        }

        // A finished record/immutable/builder-based target cannot be written to. Checked here rather than in
        // each public overload because every mergeInto funnels through these three; the copy family reaches
        // them with createBeanResult()'s constructor-argument array, which checkInPlaceWritable lets through.
        checkInPlaceWritable(targetBeanInfo, targetBean);

        final boolean isIdentityPropNameConverter = propNameConverter == Fn.<String> identity();
        final BeanInfo srcBeanInfo = ParserUtil.getBeanInfo(sourceBean.getClass());
        final BinaryOperator<Object> objPropMergeFunc = (BinaryOperator<Object>) mergeFunc;

        // Two passes - see MergeStep: the plan is resolved before the first write so a failure part-way
        // through cannot leave the caller's target half-merged.
        final List<MergeStep> steps = new ArrayList<>(srcBeanInfo.propInfoList.size());
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
                    // Deliberately strict, and test-locked (BeansTest.testCopyAs_biPredicate_
                    // unmatchedTargetProp_throws, .testCopyInto_BiPredicate_UnmatchedTargetProp_Throws,
                    // BeansReviewFixes20260830Test.testC010_mergeIntoFilteredIsAllOrNothing). A propFilter is
                    // read as a *selection* - the caller named the properties to move - so, exactly like an
                    // explicit selectPropNames collection, every selected name must exist on both sides. The
                    // no-filter and mergeFunc-only overloads are the lenient ones.
                    throw new IllegalArgumentException("No property found by name: " + propInfo.name + " in target bean class: " + targetBean.getClass());
                }

                steps.add(new MergeStep(targetPropInfo, propValue));
            }
        }

        applyMergeSteps(steps, targetBean, objPropMergeFunc);

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
     * @throws UnsupportedOperationException if {@code bean} is an instance of a class treated as an immutable bean
     *         (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); its properties cannot be set in place - build a new instance instead.
     */
    public static void clearProps(final Object bean, final String... propNames) throws IllegalArgumentException, UnsupportedOperationException {
        if (bean == null || N.isEmpty(propNames)) {
            return;
        }

        clearProps(bean, Arrays.asList(propNames));
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
     * Beans.clearProps(user, Arrays.asList("name", "age"));
     * // user.getName() returns null; user.getAge() returns 0; user.getActive() still true
     *
     * Beans.clearProps(user, Collections.emptyList());   // no change
     * Beans.clearProps(null, Arrays.asList("name"));     // no change
     * }</pre>
     *
     * @param bean the bean object whose properties are to be cleared; if {@code null}, the method does nothing.
     * @param propNames the collection of property names to clear; if empty, the method does nothing.
     * @throws IllegalArgumentException if any name in {@code propNames} is not a property of the bean.
     * @throws UnsupportedOperationException if {@code bean} is an instance of a class treated as an immutable bean
     *         (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); its properties cannot be set in place - build a new instance instead.
     */
    public static void clearProps(final Object bean, final Collection<String> propNames) throws IllegalArgumentException, UnsupportedOperationException {
        if (bean == null || N.isEmpty(propNames)) {
            return;
        }

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());
        checkInPlaceWritable(beanInfo, bean);

        // Two passes: an unknown name must not leave the bean half-cleared. beanInfo.setPropValue rejects an
        // unknown name itself, so the first pass exists purely to fail before the first write.
        for (final String propName : propNames) {
            checkPropExists(beanInfo, propName);
        }

        for (final String propName : propNames) {
            beanInfo.setPropValue(bean, propName, null);
        }
    }

    /**
     * Throws if {@code propName} is not settable on {@code beanInfo}'s class, without writing anything.
     *
     * <p>A dot-separated name is resolved with {@link BeanInfo#getPropInfoChain(String)}, the same predicate the
     * write itself uses. It used to be waved through unchecked, which quietly defeated the all-or-nothing
     * guarantee of {@link #clearProps(Object, String...)}: {@code clearProps(bean, "name", "bogus.path")}
     * cleared {@code name} and only then threw. Only the <i>path</i> is validated here; what the intermediate
     * values happen to be at write time cannot be known without reading the bean, and the write itself
     * instantiates any missing intermediate bean as it descends.</p>
     *
     * @param beanInfo metadata for the bean being modified
     * @param propName the property name to check; may be a dot-separated nested path
     * @throws IllegalArgumentException if the bean class has no such property
     */
    private static void checkPropExists(final BeanInfo beanInfo, final String propName) throws IllegalArgumentException {
        if (beanInfo.getPropInfo(propName) != null) {
            return;
        }

        if (propName.indexOf(SK._PERIOD) < 0 || beanInfo.getPropInfoChain(propName).isEmpty()) {
            throw new IllegalArgumentException("No setter method found with property name: " + propName + " in class: " + beanInfo.clazz.getCanonicalName());
        }
    }

    /**
     * Rejects an attempt to write into a finished instance of a bean this library treats as immutable
     * ({@link BeanInfo#isImmutable}).
     *
     * <p>Such a bean is still a bean: {@link #isBeanClass(Class)} answers {@code true} for it and
     * {@link #getPropNameList(Class)} lists its properties, so the read side ({@code beanToMap},
     * {@code stream}) and the new-instance side ({@code mapToBean}, {@code newRandomBean}) both work. Only
     * in-place writing is impossible. The message therefore recommends {@code mapToBean}; {@code copyAs}
     * also constructs new instances, including builder-based targets, without reading finished-bean properties
     * from the builder. Without this check {@code PropInfo.setPropValue} reached its immutable-bean branch, which
     * assumes it was handed the {@code Object[]} of constructor arguments that
     * {@code BeanInfo.createBeanResult()} produces, and the caller got a bare
     * {@code ClassCastException: class Xxx cannot be cast to class [Ljava.lang.Object;} naming an internal
     * representation they never asked for.</p>
     *
     * <p><b>{@code isInstance} is the load-bearing half of the test.</b> {@code copyAs}, {@code mapToBean}
     * and the copy-flavoured {@code mergeInto} calls legitimately write into that constructor-argument array
     * (or into a builder) for exactly these classes, and neither is an instance of the bean class - so they
     * pass, while a finished instance does not.</p>
     *
     * <p><b>"Immutable" here is {@code BeanInfo}'s verdict, which is broader than "declares no setter".</b>
     * A class with perfectly good setters is also classified immutable when it has no accessible no-arg
     * constructor, because that is what {@code BeanInfo} probes with. The message therefore names the
     * condition rather than claiming the class has no setters - it may well have some, they simply cannot be
     * reached without an instance the library knows how to create.</p>
     *
     * @param beanInfo metadata for the target's class
     * @param target the object about to be written to
     * @throws UnsupportedOperationException if {@code target} is a finished instance of an immutable bean class
     */
    private static void checkInPlaceWritable(final BeanInfo beanInfo, final Object target) throws UnsupportedOperationException {
        if (beanInfo.isImmutable && beanInfo.clazz.isInstance(target)) {
            throw new UnsupportedOperationException("Cannot set properties on " + ClassUtil.getCanonicalClassName(beanInfo.clazz)
                    + " in place: it is treated as an immutable bean - a record, a builder-based class, one with no writable "
                    + "property, or one with no accessible no-arg constructor. Build a new instance instead, for example with "
                    + "Beans.mapToBean(Beans.beanToMap(bean), " + ClassUtil.getSimpleClassName(beanInfo.clazz) + ".class).");
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
     * Beans.clearAllProps(user);
     * // user.getName() returns null; user.getAge() returns 0; user.getActive() returns null
     *
     * Beans.clearAllProps(null);   // no change
     * }</pre>
     *
     * @param bean the bean object whose properties are to be erased. If this is {@code null}, the method does nothing.
     * @throws UnsupportedOperationException if {@code bean} is an instance of a class treated as an immutable bean
     *         (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); its properties cannot be set in place - build a new instance instead.
     */
    public static void clearAllProps(final Object bean) throws UnsupportedOperationException {
        if (bean == null) {
            return;
        }

        final Class<?> cls = bean.getClass();
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        checkInPlaceWritable(beanInfo, bean);

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
     *   <li>String - a 16-character UUID fragment, except that a property whose <i>name</i> contains
     *       {@code "email"} (ignoring case) gets a syntactically plausible address instead
     *       ({@code <a 12-character prefix of a canonical UUID>@email.com}, e.g. {@code b329ba66-935@email.com}),
     *       so generated data passes naive e-mail validation</li>
     *   <li>Date and Calendar (current timestamp)</li>
     *   <li>Number subclasses</li>
     *   <li>Nested bean objects (filled recursively; a reference cycle stops the recursion and leaves the
     *       property at its default value)</li>
     * </ul>
     *
     * <p><b>Unsupported property types are still written:</b> a property whose type is not in the list above -
     * a {@link java.util.Collection}, a {@link Map}, an {@code enum} or a {@code java.time} type, for example -
     * is <i>set to its type's default value</i> ({@code null} for a reference type). On an already-populated bean that clears the
     * property rather than leaving it alone. Pass an explicit property list to
     * {@link #randomize(Object, Collection)} to limit which properties are touched.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = new User();
     * Beans.randomize(user);
     * // every property is now filled with a random value
     * // user.getName() is a non-null random String (e.g. a 16-char UUID fragment)
     *
     * Beans.randomize((Object) null);   // throws IllegalArgumentException
     * }</pre>
     *
     * @param bean the bean object to populate; must not be {@code null} and must be a valid bean class.
     * @throws IllegalArgumentException if {@code bean} is {@code null} or not a valid bean class.
     * @throws UnsupportedOperationException if {@code bean} is an instance of a class treated as an immutable bean
     *         (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); its properties cannot be set in place - build a new instance instead.
     */
    public static void randomize(final Object bean) throws IllegalArgumentException, UnsupportedOperationException {
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
     * <p><b>Unsupported property types are still written:</b> a property whose type is not one of the types
     * {@link #randomize(Object)} lists as supported - a {@link Collection}, a {@link Map}, an {@code enum} or a
     * {@code java.time} type, for example - is <i>set to its type's default value</i> ({@code null} for a
     * reference type). On an already-populated bean that clears the property rather than leaving it alone.</p>
     *
     * @param bean the bean object to populate; must not be {@code null} and must be a valid bean class.
     * @param propNamesToFill the names of the properties to fill with random values; must not be {@code null}.
     * @throws IllegalArgumentException if {@code bean} is {@code null} or not a valid bean class, if
     *         {@code propNamesToFill} is {@code null}, or if a property name is not found in the bean.
     * @throws UnsupportedOperationException if {@code bean} is an instance of a class treated as an immutable bean
     *         (a record, a builder-based class, one with no writable property, or one with no accessible no-arg
     *         constructor); its properties cannot be set in place - build a new instance instead.
     */
    public static void randomize(final Object bean, final Collection<String> propNamesToFill) throws IllegalArgumentException, UnsupportedOperationException {
        N.checkArgNotNull(bean, cs.bean);

        final Class<?> beanClass = bean.getClass();
        N.checkBeanClass(beanClass);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        checkInPlaceWritable(beanInfo, bean);

        fillInRandomScope(beanClass, bean, resolvePropInfos(beanInfo, propNamesToFill));
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
     * <p><b>Unsupported property types are still written:</b> a property whose type is not one of the types
     * {@link #randomize(Object)} lists as supported - a {@link Collection}, a {@link Map}, an {@code enum} or a
     * {@code java.time} type, for example - is <i>set to its type's default value</i> ({@code null} for a
     * reference type). On an already-populated bean that clears the property rather than leaving it alone.</p>
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
     * class User {
     *     private String name;
     *     private int age;
     *     private Boolean active;
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public int getAge() { return age; }
     *     public void setAge(int age) { this.age = age; }
     *     public Boolean getActive() { return active; }
     *     public void setActive(Boolean active) { this.active = active; }
     * }
     * User user = Beans.newRandomBean(User.class, Arrays.asList("name"));
     *
     * // user.getName() is a non-null random value; user.getAge() stays 0; active stays null
     * }</pre>
     *
     * <p><b>Unsupported property types are still written:</b> a property whose type is not one of the types
     * {@link #randomize(Object)} lists as supported - a {@link Collection}, a {@link Map}, an {@code enum} or a
     * {@code java.time} type, for example - is <i>set to its type's default value</i> ({@code null} for a
     * reference type). On an already-populated bean that clears the property rather than leaving it alone.</p>
     *
     * @param <T> the type of the bean.
     * @param beanClass the class to instantiate and populate; must not be {@code null} and must be a valid bean class.
     * @param propNamesToFill the names of the properties to fill with random values; must not be {@code null}.
     * @return a new instance with the specified properties filled with random values; never {@code null}.
     * @throws IllegalArgumentException if {@code beanClass} is {@code null} or not a valid bean class, if
     *         {@code propNamesToFill} is {@code null}, or if a property name is not found in the class.
     */
    public static <T> T newRandomBean(final Class<? extends T> beanClass, final Collection<String> propNamesToFill) throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        N.checkBeanClass(beanClass);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        final List<PropInfo> propInfos = resolvePropInfos(beanInfo, propNamesToFill);

        final Object result = beanInfo.createBeanResult();

        fillInRandomScope(beanClass, result, propInfos);

        return beanInfo.finishBeanResult(result);
    }

    /** Per-thread visited-class set used by the random-bean family to break reference cycles. */
    private static final ThreadLocal<java.util.Set<Class<?>>> NEW_RANDOM_VISITED = ThreadLocal.withInitial(java.util.HashSet::new);

    /**
     * Fills {@code bean} with random values while {@code beanClass} is marked as "being filled" on this
     * thread, so a self-referential property stops instead of recursing forever.
     *
     * <p>Every entry point into the random-bean family goes through here. {@code newRandomBeanList} used to
     * call {@code populateWithRandomValues} directly, which left its own class off the visited set: for
     * {@code class Node { Node parent; }}, {@code newRandomBean(Node.class)} produced {@code parent == null}
     * while {@code newRandomBeanList(Node.class, 1)} produced a non-{@code null} {@code parent} - the same
     * class, two different graph depths, depending only on which factory was called.</p>
     *
     * @param beanClass the class being filled, marked as in-progress for the duration
     * @param bean the instance (or the constructor-argument array) to write into
     * @param propInfos the already-resolved properties to fill
     */
    private static void fillInRandomScope(final Class<?> beanClass, final Object bean, final List<PropInfo> propInfos) {
        final boolean topLevel = enterRandomScope(beanClass);

        try {
            populateWithRandomValues(bean, propInfos);
        } finally {
            exitRandomScope(beanClass, topLevel);
        }
    }

    /**
     * Marks {@code beanClass} as being filled on this thread. Mirrors {@link #enterDeepBean(Object)}.
     *
     * @param beanClass the class entering the scope
     * @return whether this call opened the outermost scope, to be passed back to
     *         {@link #exitRandomScope(Class, boolean)}
     */
    private static boolean enterRandomScope(final Class<?> beanClass) {
        final java.util.Set<Class<?>> visited = NEW_RANDOM_VISITED.get();
        final boolean topLevel = visited.isEmpty();
        visited.add(beanClass);

        return topLevel;
    }

    /** Ends a {@link #enterRandomScope(Class)} scope, dropping the thread-local once the outermost one closes. */
    private static void exitRandomScope(final Class<?> beanClass, final boolean topLevel) {
        NEW_RANDOM_VISITED.get().remove(beanClass);

        if (topLevel) {
            NEW_RANDOM_VISITED.remove();
        }
    }

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
     * <p><b>Unsupported property types are still written:</b> a property whose type is not one of the types
     * {@link #randomize(Object)} lists as supported - a {@link Collection}, a {@link Map}, an {@code enum} or a
     * {@code java.time} type, for example - is <i>set to its type's default value</i> ({@code null} for a
     * reference type). On an already-populated bean that clears the property rather than leaving it alone.</p>
     *
     * @param <T> the type of the bean.
     * @param beanClass the class to instantiate and populate; must not be {@code null} and must be a valid bean class.
     * @param count the number of instances to create; must not be negative.
     * @return a list containing exactly {@code count} newly created and fully populated bean instances;
     *         never {@code null}.
     * @throws IllegalArgumentException if {@code beanClass} is {@code null}, not a valid bean class, or
     *         {@code count} is negative.
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
     * class User {
     *     private String name;
     *     private int age;
     *     private Boolean active;
     *     public String getName() { return name; }
     *     public void setName(String name) { this.name = name; }
     *     public int getAge() { return age; }
     *     public void setAge(int age) { this.age = age; }
     *     public Boolean getActive() { return active; }
     *     public void setActive(Boolean active) { this.active = active; }
     * }
     * // users.size() == 2; each user has a random name but age stays 0 and active stays null
     * List<User> users = Beans.newRandomBeanList(User.class, Arrays.asList("name"), 2);
     *
     * Beans.newRandomBeanList(User.class, Arrays.asList("name"), 0);   // returns [] (empty list)
     * }</pre>
     *
     * <p><b>Unsupported property types are still written:</b> a property whose type is not one of the types
     * {@link #randomize(Object)} lists as supported - a {@link Collection}, a {@link Map}, an {@code enum} or a
     * {@code java.time} type, for example - is <i>set to its type's default value</i> ({@code null} for a
     * reference type). On an already-populated bean that clears the property rather than leaving it alone.</p>
     *
     * @param <T> the type of the bean.
     * @param beanClass the class to instantiate and populate; must not be {@code null} and must be a valid bean class.
     * @param propNamesToFill the names of the properties to fill with random values; must not be {@code null}.
     * @param count the number of instances to create; must not be negative.
     * @return a list containing exactly {@code count} newly created bean instances with the specified
     *         properties filled; never {@code null}.
     * @throws IllegalArgumentException if {@code beanClass} is {@code null} or not a valid bean class,
     *         {@code propNamesToFill} is {@code null} or contains an unknown property name, or {@code count} is negative.
     */
    public static <T> List<T> newRandomBeanList(final Class<? extends T> beanClass, final Collection<String> propNamesToFill, final int count)
            throws IllegalArgumentException {
        N.checkArgNotNull(beanClass, cs.beanClass);
        N.checkBeanClass(beanClass);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(beanClass);
        // Resolve once, up front: with count == 0 the loop below never runs, so validating inside it would
        // let newRandomBeanList(cls, null, 0) and newRandomBeanList(cls, List.of("bogus"), 0) return an empty
        // list instead of throwing, contradicting this method's @throws.
        final List<PropInfo> propInfos = resolvePropInfos(beanInfo, propNamesToFill);
        N.checkArgNotNegative(count, cs.count);

        final List<T> resultList = new ArrayList<>(count);
        Object result = null;

        // One scope around the whole batch, not one per element: entering it is what stops a self-referential
        // bean expanding a level further here than newRandomBean produces for the same class, and the class
        // is "being filled" for the batch's whole duration - so re-entering per element would only churn the
        // thread-local.
        final boolean topLevel = enterRandomScope(beanClass);

        try {
            for (int i = 0; i < count; i++) {
                result = beanInfo.createBeanResult();

                populateWithRandomValues(result, propInfos);

                resultList.add(beanInfo.finishBeanResult(result));
            }
        } finally {
            exitRandomScope(beanClass, topLevel);
        }

        return resultList;
    }

    /**
     * Resolves every name in {@code propNamesToFill} to its {@link PropInfo}, rejecting the whole request if
     * any name is unknown.
     *
     * <p>Resolving before the first write prevents unknown-property errors from causing partial updates: validating inside
     * the write loop left {@code randomize(bean, List.of("name", "bogus"))} with a randomized {@code name} and
     * an {@link IllegalArgumentException}.</p>
     *
     * @param beanInfo metadata for the bean being filled
     * @param propNamesToFill the property names to resolve; must not be {@code null}
     * @return the resolved property metadata, in the order the names were given
     * @throws IllegalArgumentException if {@code propNamesToFill} is {@code null} or names a property that the
     *         bean class does not have
     */
    private static List<PropInfo> resolvePropInfos(final BeanInfo beanInfo, final Collection<String> propNamesToFill) throws IllegalArgumentException {
        N.checkArgNotNull(propNamesToFill, cs.propNamesToFill);

        final List<PropInfo> propInfos = new ArrayList<>(propNamesToFill.size());

        for (final String propName : propNamesToFill) {
            final PropInfo propInfo = beanInfo.getPropInfo(propName);

            if (propInfo == null) {
                throw new IllegalArgumentException("Property: " + propName + " is not found in bean class: " + beanInfo.clazz);
            }

            propInfos.add(propInfo);
        }

        return propInfos;
    }

    private static void populateWithRandomValues(final Object bean, final List<PropInfo> propInfos) {
        Type<Object> type = null;
        Class<?> parameterClass = null;
        Object propValue = null;

        for (final PropInfo propInfo : propInfos) {
            final String propName = propInfo.name;

            parameterClass = propInfo.clazz;
            // `type`, not `jsonXmlType`: every branch below dispatches on propInfo.clazz, which is
            // propInfo.type.javaType(). Producing the value from a different Type than the one that chose the
            // branch can hand the setter an instance of the wrong class.
            type = propInfo.type;

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
                propValue = (byte) N.RAND.nextInt();
            } else if (short.class.equals(parameterClass) || Short.class.equals(parameterClass)) {
                propValue = (short) N.RAND.nextInt();
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
     * <p>The stream is <b>lazy</b>: the getters are invoked by the terminal operation, not by this call, so
     * the values observed are the bean's state at consumption time. Consume the stream before mutating the
     * bean, or use {@link #beanToMap(Object)} for an eager snapshot.</p>
     *
     * @param bean the bean object to extract properties from; must not be {@code null}.
     * @return a {@link Stream} of {@link Map.Entry} objects where each key is a property name
     *         and each value is the corresponding property value (which may be {@code null}).
     * @throws IllegalArgumentException if {@code bean} is {@code null}.
     */
    public static Stream<Map.Entry<String, Object>> stream(final Object bean) throws IllegalArgumentException {
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
     * @return a lazy {@link Stream} of {@link Map.Entry} objects where each key is a property name
     *         and each value is the corresponding property value (which may be {@code null}),
     *         containing only those properties for which {@code propFilter} returned {@code true}. As with
     *         {@link #stream(Object)}, the getters run at consumption time, not at call time.
     * @throws IllegalArgumentException if {@code bean} is {@code null}, or if {@code propFilter} is {@code null}.
     */
    public static Stream<Map.Entry<String, Object>> stream(final Object bean, final BiPredicate<? super String, Object> propFilter)
            throws IllegalArgumentException {
        N.checkArgNotNull(bean, cs.bean);
        N.checkArgNotNull(propFilter, cs.propFilter);

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(bean.getClass());

        return Stream.of(beanInfo.propInfoList).map(propInfo -> {
            final Object propValue = propInfo.getPropValue(bean);
            return propFilter.test(propInfo.name, propValue) ? N.newEntry(propInfo.name, propValue) : null;
        }).skipNulls();
    }
}
