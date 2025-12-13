/*
 * Copyright (C) 2018 HaiYang Li
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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A specialized utility class that provides convenient typed Class references for parameterized collection types,
 * designed to work around Java's type erasure limitations while maintaining type safety and code readability.
 * This class serves as a bridge between Java's generic type system and APIs that require Class objects,
 * particularly useful in reflection, serialization, and framework integration scenarios.
 *
 * <p>Due to Java's type erasure, generic type information is not available at runtime, making it impossible
 * to obtain a {@code Class<List<String>>} object directly. This utility provides a convenient way to obtain
 * typed Class references that can be used for type hints, API parameters, and improved code documentation,
 * even though the actual generic type parameters are erased at runtime.</p>
 *
 * <p><b>⚠️ IMPORTANT - Type Erasure Limitation:</b>
 * <ul>
 *   <li>The Class objects returned by all methods do NOT contain actual runtime type parameter information</li>
 *   <li>These are primarily useful for providing type hints to APIs that accept Class parameters</li>
 *   <li>For true runtime type parameter information, use {@code Type.of()} or {@code TypeReference}</li>
 *   <li>The generic type parameters exist only for compile-time type safety and documentation</li>
 * </ul>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Type Safety:</b> Provides compile-time type checking for parameterized collection types</li>
 *   <li><b>Code Readability:</b> Clear, expressive method names that document intended generic types</li>
 *   <li><b>API Integration:</b> Seamless integration with frameworks and APIs expecting Class parameters</li>
 *   <li><b>Predefined Constants:</b> Common collection type combinations available as static constants</li>
 *   <li><b>Comprehensive Coverage:</b> Support for all major Java collection interfaces and implementations</li>
 *   <li><b>Performance Optimized:</b> Zero runtime overhead beyond normal Class object usage</li>
 *   <li><b>Null-Safe Design:</b> All methods properly handle parameter validation</li>
 *   <li><b>Framework Friendly:</b> Designed for use with serialization, ORM, and dependency injection frameworks</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Pragmatic Type Safety:</b> Provides type safety where possible within JVM limitations</li>
 *   <li><b>Developer Experience:</b> Prioritizes code clarity and ease of use over theoretical purity</li>
 *   <li><b>Framework Integration:</b> Designed to work seamlessly with existing Java frameworks and libraries</li>
 *   <li><b>Performance First:</b> Zero runtime overhead beyond standard Java reflection costs</li>
 *   <li><b>Comprehensive Coverage:</b> Supports all common collection types and patterns</li>
 * </ul>
 *
 * <p><b>Supported Collection Types:</b>
 * <ul>
 *   <li><b>Core Collections:</b> {@code List}, {@code Set}, {@code Map}, {@code Queue}, {@code Deque}</li>
 *   <li><b>Sorted Collections:</b> {@code SortedSet}, {@code SortedMap}, {@code NavigableSet}, {@code NavigableMap}</li>
 *   <li><b>Concurrent Collections:</b> {@code ConcurrentMap}, {@code BlockingQueue}, {@code ConcurrentLinkedQueue}</li>
 *   <li><b>Specialized Collections:</b> {@code BiMap}, {@code Multiset}, {@code ListMultimap}, {@code SetMultimap}</li>
 *   <li><b>Implementation-Specific:</b> {@code ArrayList}, {@code LinkedList}, {@code HashSet}, {@code TreeMap}, etc.</li>
 * </ul>
 *
 * <p><b>Method Categories:</b>
 * <ul>
 *   <li><b>Generic Methods:</b> {@code ofList()}, {@code ofSet()}, {@code ofMap()} - Work with interface types</li>
 *   <li><b>Implementation-Specific:</b> {@code ofArrayList()}, {@code ofHashMap()} - Target specific implementations</li>
 *   <li><b>Parameterized Variants:</b> Methods accepting Class parameters for documentation purposes</li>
 *   <li><b>Constants:</b> Pre-defined Class objects for common type combinations</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic collection type references
 * Class<List<String>> stringList = Clazz.ofList(String.class);
 * Class<Set<Integer>> integerSet = Clazz.ofSet(Integer.class);
 * Class<Map<String, Object>> stringObjectMap = Clazz.ofMap(String.class, Object.class);
 *
 * // Using predefined constants
 * Class<Map<String, Object>> propsMap = Clazz.PROPS_MAP;
 * Class<List<String>> stringList = Clazz.STRING_LIST;
 * Class<Set<String>> stringSet = Clazz.STRING_SET;
 *
 * // Implementation-specific types
 * Class<ArrayList<String>> arrayList = Clazz.ofArrayList(String.class);
 * Class<HashMap<String, Object>> hashMap = Clazz.ofHashMap(String.class, Object.class);
 * Class<TreeSet<Integer>> treeSet = Clazz.ofTreeSet(Integer.class);
 *
 * // Concurrent collection types
 * Class<ConcurrentMap<String, Object>> concurrentMap = Clazz.ofConcurrentHashMap(String.class, Object.class);
 * Class<BlockingQueue<String>> blockingQueue = Clazz.ofLinkedBlockingQueue(String.class);
 * }</pre>
 *
 * <p><b>Framework Integration Examples:</b>
 * <pre>{@code
 * // JSON/XML serialization frameworks
 * ObjectMapper mapper = new ObjectMapper();
 * List<Person> people = mapper.readValue(json, Clazz.ofList(Person.class));
 *
 * // Dependency injection frameworks
 * @Inject
 * Provider<List<Service>> serviceProvider;
 * // Can use Clazz.ofList(Service.class) for type hints
 *
 * // ORM and database frameworks
 * Query query = entityManager.createQuery("SELECT p FROM Person p", Clazz.ofList(Person.class));
 *
 * // Configuration and properties handling
 * Properties props = loadProperties();
 * Map<String, Object> configMap = convertToMap(props, Clazz.PROPS_MAP);
 * }</pre>
 *
 * <p><b>Specialized Collection Support:</b>
 * <ul>
 *   <li><b>BiMap:</b> Bidirectional map collections with {@code ofBiMap()} methods</li>
 *   <li><b>Multiset:</b> Collections allowing duplicate elements with {@code ofMultiset()} methods</li>
 *   <li><b>Multimap:</b> Maps with multiple values per key via {@code ofListMultimap()} and {@code ofSetMultimap()}</li>
 *   <li><b>Concurrent Collections:</b> Thread-safe collections for concurrent programming</li>
 * </ul>
 *
 * <p><b>Type Parameter Documentation:</b>
 * <ul>
 *   <li>All methods accepting Class parameters use {@code @SuppressWarnings("unused")} annotations</li>
 *   <li>The Class parameters serve purely as documentation and compile-time type checking</li>
 *   <li>Runtime behavior is identical whether parameters are provided or not</li>
 *   <li>Method overloads exist for convenience - parameterized and non-parameterized versions</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Creation Cost:</b> O(1) - Simple cast operations with no additional computation</li>
 *   <li><b>Memory Overhead:</b> Zero - Returns existing Class objects via type casting</li>
 *   <li><b>Runtime Impact:</b> Minimal - Equivalent to direct Class.class access</li>
 *   <li><b>Compile-Time Benefits:</b> Enhanced type safety and IDE support</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Design:</b> All methods are static and stateless</li>
 *   <li><b>No Mutable State:</b> No instance variables or mutable static fields</li>
 *   <li><b>Class Object Safety:</b> Class objects are inherently thread-safe</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 * </ul>
 *
 * <p><b>Predefined Constants:</b>
 * <ul>
 *   <li><b>{@code PROPS_MAP}:</b> {@code Map<String, Object>} implemented as LinkedHashMap</li>
 *   <li><b>{@code STRING_LIST}:</b> {@code List<String>} for common string collections</li>
 *   <li><b>{@code STRING_SET}:</b> {@code Set<String>} for unique string collections</li>
 *   <li><b>{@code OBJECT_LIST}:</b> {@code List<Object>} for heterogeneous collections</li>
 * </ul>
 *
 * <p><b>Integration with Type System:</b>
 * <ul>
 *   <li><b>com.landawn.abacus.type.Type:</b> For full runtime type information including generics</li>
 *   <li><b>TypeReference:</b> For creating TypeToken objects with complete generic information</li>
 *   <li><b>Class Objects:</b> For basic type information and framework integration</li>
 *   <li><b>Reflection APIs:</b> Standard Java reflection works with returned Class objects</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use this class for framework APIs that require Class parameters but need type hints</li>
 *   <li>Prefer predefined constants for common type combinations to reduce object allocation</li>
 *   <li>Use {@code Type.of()} or {@code TypeReference} when actual runtime generic information is needed</li>
 *   <li>Document the intended generic types clearly when using these Class objects</li>
 *   <li>Consider using parameterized method variants for better code documentation</li>
 *   <li>Cache frequently used Class references in static final fields</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Expecting runtime generic type information from returned Class objects</li>
 *   <li>Using this class when {@code Type} or {@code TypeReference} would be more appropriate</li>
 *   <li>Creating new Class references repeatedly instead of using cached constants</li>
 *   <li>Assuming different parameterized versions return different Class objects</li>
 *   <li>Using for collections where raw types would be more appropriate</li>
 * </ul>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. Raw Class.class:</b> Provides type safety and documentation vs. raw types</li>
 *   <li><b>vs. Type.of():</b> Simpler but no runtime generic info vs. complete type information</li>
 *   <li><b>vs. TypeReference:</b> Lighter weight but less powerful vs. full generic support</li>
 *   <li><b>vs. Manual Casting:</b> Type-safe and documented vs. error-prone manual casts</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>No Runtime Exceptions:</b> All methods are guaranteed to succeed</li>
 *   <li><b>Compile-Time Safety:</b> Generic type constraints prevent most errors at compile time</li>
 *   <li><b>ClassCastException:</b> Potential only if returned Class is misused with incompatible types</li>
 *   <li><b>Parameter Validation:</b> Methods are designed to be null-safe where appropriate</li>
 * </ul>
 *
 * <p><b>Use Cases and Applications:</b>
 * <ul>
 *   <li><b>JSON/XML Serialization:</b> Providing type hints to serialization frameworks</li>
 *   <li><b>Dependency Injection:</b> Documenting generic types in injection configurations</li>
 *   <li><b>ORM Frameworks:</b> Specifying collection types for database mapping</li>
 *   <li><b>Configuration Systems:</b> Type-safe configuration property handling</li>
 *   <li><b>API Documentation:</b> Clearly expressing intended generic types in method signatures</li>
 *   <li><b>Testing Frameworks:</b> Providing type information for mock object creation</li>
 * </ul>
 *
 * <p><b>Example: Configuration System Integration</b>
 * <pre>{@code
 * public class ConfigurationManager {
 *     private final Map<String, Object> properties;
 *
 *     public ConfigurationManager(Properties props) {
 *         // Using Clazz for type-safe conversion
 *         this.properties = convertProperties(props, Clazz.PROPS_MAP);
 *     }
 *
 *     @SuppressWarnings("unchecked")
 *     public <T> List<T> getListProperty(String key, Class<T> elementType) {
 *         Object value = properties.get(key);
 *         if (value instanceof String) {
 *             // Parse string to list using type information
 *             return parseStringToList((String) value, Clazz.ofList(elementType));
 *         }
 *         return (List<T>) value;
 *     }
 *
 *     public List<String> getStringList(String key) {
 *         return getListProperty(key, String.class);
 *     }
 *
 *     public Set<Integer> getIntegerSet(String key) {
 *         List<Integer> list = getListProperty(key, Integer.class);
 *         return new HashSet<>(list);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Static Analysis and IDE Benefits:</b>
 * <ul>
 *   <li><b>Type Inference:</b> IDEs can provide better code completion and error detection</li>
 *   <li><b>Refactoring Support:</b> Type-safe refactoring across generic collection usage</li>
 *   <li><b>Documentation:</b> Clear intent expression in method signatures and variable declarations</li>
 *   <li><b>Code Navigation:</b> Better find-usages and dependency analysis</li>
 * </ul>
 *
 * @see com.landawn.abacus.type.Type
 * @see TypeReference
 * @see TypeReference.TypeToken
 * @see Class
 * @see java.lang.reflect.ParameterizedType
 * @see java.util.Collection
 * @see java.util.Map
 * @see java.util.concurrent.ConcurrentMap
 */
@SuppressWarnings("java:S1172")
public final class Clazz {

    /**
     * A constant representing the class type for {@code Map<String, Object>} with LinkedHashMap implementation.
     * Commonly used for properties or configuration maps.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> PROPS_MAP = (Class) LinkedHashMap.class;

    /**
     * A constant representing the class type for {@code Map<String, Object>}.
     * This refers to the Map interface, not a specific implementation.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> MAP = (Class) Map.class;

    /**
     * A constant representing the class type for {@code Map<String, Object>} with LinkedHashMap implementation.
     * Useful when ordering of map entries needs to be preserved.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Map<String, Object>> LINKED_HASH_MAP = (Class) LinkedHashMap.class;

    /**
     * A constant representing the class type for {@code List<String>}.
     * One of the most commonly used generic list types.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<String>> STRING_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Integer>}.
     * Commonly used for lists of integer values.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Integer>> INTEGER_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Long>}.
     * Commonly used for lists of long integer values.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Long>> LONG_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Double>}.
     * Commonly used for lists of floating-point values.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Double>> DOUBLE_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code List<Object>}.
     * Used for lists that can contain any type of objects.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<List<Object>> OBJECT_LIST = (Class) List.class;

    /**
     * A constant representing the class type for {@code Set<String>}.
     * Commonly used for unique string collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<String>> STRING_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Integer>}.
     * Commonly used for unique integer collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Integer>> INTEGER_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Long>}.
     * Commonly used for unique long integer collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Long>> LONG_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Double>}.
     * Commonly used for unique floating-point collections.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Double>> DOUBLE_SET = (Class) Set.class;

    /**
     * A constant representing the class type for {@code Set<Object>}.
     * Used for sets that can contain any type of objects.
     */
    @SuppressWarnings("rawtypes")
    public static final Class<Set<Object>> OBJECT_SET = (Class) Set.class;

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Clazz() {
        // singleton.
    }

    /**
     * Returns a typed Class reference for the specified class.
     * This method provides a convenient way to cast a class to a more specific generic type,
     * allowing the compiler to infer generic type parameters without requiring explicit casting.
     *
     * <p>This method is particularly useful when working with APIs that accept {@code Class<?>}
     * parameters but you want to maintain type safety at compile time. The method performs
     * an unchecked cast internally but provides a cleaner API surface.</p>
     *
     * <p><b>Warning:</b> The returned Class object does NOT contain actual type parameter information
     * due to Java's type erasure. At runtime, {@code ArrayList<String>.class} and
     * {@code ArrayList<Integer>.class} are the same object. This method only provides compile-time
     * type checking.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Instead of: Class<ArrayList<String>> cls = (Class<ArrayList<String>>) ArrayList.class;
     * Class<ArrayList<String>> arrayListClass = Clazz.of(ArrayList.class);
     *
     * // Useful for method parameters
     * public <T> T deserialize(String json, Class<T> type) { ... }
     * MyObject obj = deserialize(json, Clazz.of(MyObject.class));
     * }</pre>
     *
     * @param <T> the target type parameter.
     * @param cls the class to cast; must not be null
     * @return a typed Class reference with generic type information.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<T> of(final Class<? super T> cls) {
        return (Class) cls;
    }

    /**
     * Returns a Class reference for {@code List} with unspecified element type.
     * This is equivalent to {@code List.class} but with proper generic typing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<List<String>> listClass = Clazz.ofList();
     * }</pre>
     *
     * @param <T> the element type of the list.
     * @return the Class object representing {@code List<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList() {
        return (Class) List.class;
    }

    /**
     * Returns a Class reference for {@code List} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     *
     * <p>This method returns the {@code List} interface class, not a concrete implementation.
     * The actual implementation will be determined by the framework or code that instantiates
     * the collection. This is useful for APIs that accept a Class parameter to determine the
     * target collection type for deserialization or conversion operations.</p>
     *
     * <p><b>Warning:</b> The returned Class object does NOT contain actual type parameter information
     * due to Java's type erasure. The {@code eleCls} parameter is only used for compile-time type
     * inference and is not stored or used at runtime.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For type-safe API calls
     * Class<List<String>> stringListClass = Clazz.ofList(String.class);
     * Class<List<Integer>> intListClass = Clazz.ofList(Integer.class);
     *
     * // Common use case with serialization frameworks
     * List<String> result = deserializer.readValue(json, Clazz.ofList(String.class));
     * }</pre>
     *
     * @param <T> the element type of the list.
     * @param eleCls the class of elements (used only for type inference, not retained at runtime).
     * @return the Class object representing the {@code List} interface.
     * @see #ofLinkedList(Class) for a specific List implementation
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#ofList(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofList(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) List.class;
    }

    /**
     * Returns a Class reference for {@code LinkedList} with unspecified element type.
     * Useful when you specifically need a LinkedList implementation reference.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<List<String>> linkedListClass = Clazz.ofLinkedList();
     * }</pre>
     *
     * @param <T> the element type of the list.
     * @return the Class object representing {@code LinkedList<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList() {
        return (Class) LinkedList.class;
    }

    /**
     * Returns a Class reference for {@code LinkedList} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<List<String>> linkedListClass = Clazz.ofLinkedList(String.class);
     * }</pre>
     *
     * @param <T> the element type of the list.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code LinkedList<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<List<T>> ofLinkedList(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedList.class;
    }

    /**
     * Returns a Class reference for {@code List<Map<K, V>>} with the specified key and value types.
     * Useful for representing lists of maps, such as database query results.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<List<Map<String, Object>>> listOfMapsClass = Clazz.ofListOfMap(String.class, Object.class);
     * }</pre>
     *
     * @param <K> the key type of the maps in the list.
     * @param <V> the value type of the maps in the list.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code List<Map<K, V>>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<List<Map<K, V>>> ofListOfMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) List.class;
    }

    /**
     * Returns a Class reference for {@code Set<Map<K, V>>} with the specified key and value types.
     * Useful for representing unique collections of maps.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Set<Map<String, Integer>>> setOfMapsClass = Clazz.ofSetOfMap(String.class, Integer.class);
     * }</pre>
     *
     * @param <K> the key type of the maps in the set.
     * @param <V> the value type of the maps in the set.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code Set<Map<K, V>>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Set<Map<K, V>>> ofSetOfMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) Set.class;
    }

    /**
     * Returns a Class reference for {@code Set} with unspecified element type.
     * This is equivalent to {@code Set.class} but with proper generic typing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Set<String>> setClass = Clazz.ofSet();
     * }</pre>
     *
     * @param <T> the element type of the set.
     * @return the Class object representing {@code Set<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet() {
        return (Class) Set.class;
    }

    /**
     * Returns a Class reference for {@code Set} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Set<String>> stringSetClass = Clazz.ofSet(String.class);
     * Class<Set<Integer>> intSetClass = Clazz.ofSet(Integer.class);
     * }</pre>
     *
     * @param <T> the element type of the set.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code Set<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Set.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashSet} with unspecified element type.
     * Useful when you need a Set that preserves insertion order.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Set<String>> linkedHashSetClass = Clazz.ofLinkedHashSet();
     * }</pre>
     *
     * @param <T> the element type of the set.
     * @return the Class object representing {@code LinkedHashSet<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet() {
        return (Class) LinkedHashSet.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Set<String>> linkedHashSetClass = Clazz.ofLinkedHashSet(String.class);
     * }</pre>
     *
     * @param <T> the element type of the set.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code LinkedHashSet<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Set<T>> ofLinkedHashSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedHashSet.class;
    }

    /**
     * Returns a Class reference for {@code SortedSet} with unspecified element type.
     * SortedSet maintains elements in sorted order according to their natural ordering or a comparator.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<SortedSet<String>> sortedSetClass = Clazz.ofSortedSet();
     * }</pre>
     *
     * @param <T> the element type of the sorted set.
     * @return the Class object representing {@code SortedSet<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet() {
        return (Class) SortedSet.class;
    }

    /**
     * Returns a Class reference for {@code SortedSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<SortedSet<Integer>> sortedIntSetClass = Clazz.ofSortedSet(Integer.class);
     * }</pre>
     *
     * @param <T> the element type of the sorted set.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code SortedSet<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<SortedSet<T>> ofSortedSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) SortedSet.class;
    }

    /**
     * Returns a Class reference for {@code NavigableSet} with unspecified element type.
     * NavigableSet extends SortedSet with navigation methods for finding closest matches.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableSet<String>> navigableSetClass = Clazz.ofNavigableSet();
     * }</pre>
     *
     * @param <T> the element type of the navigable set.
     * @return the Class object representing {@code NavigableSet<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet() {
        return (Class) NavigableSet.class;
    }

    /**
     * Returns a Class reference for {@code NavigableSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableSet<Double>> navigableDoubleSetClass = Clazz.ofNavigableSet(Double.class);
     * }</pre>
     *
     * @param <T> the element type of the navigable set.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code NavigableSet<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofNavigableSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) NavigableSet.class;
    }

    /**
     * Returns a Class reference for {@code TreeSet} with unspecified element type.
     * TreeSet is a NavigableSet implementation based on a TreeMap.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableSet<String>> treeSetClass = Clazz.ofTreeSet();
     * }</pre>
     *
     * @param <T> the element type of the tree set.
     * @return the Class object representing {@code TreeSet<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet() {
        return (Class) TreeSet.class;
    }

    /**
     * Returns a Class reference for {@code TreeSet} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     *
     * <p>TreeSet is a {@code NavigableSet} implementation based on a Red-Black tree. Elements are
     * sorted according to their natural ordering (using {@code Comparable}) or by a {@code Comparator}
     * provided at set creation time. This implementation provides guaranteed log(n) time cost for
     * basic operations (add, remove, contains).</p>
     *
     * <p><b>Performance:</b> TreeSet provides O(log n) time for add, remove, and contains operations.
     * This is slower than HashSet (O(1) average), but maintains elements in sorted order.</p>
     *
     * <p><b>Note:</b> TreeSet is NOT thread-safe. For concurrent access, wrap with
     * {@code Collections.synchronizedSortedSet()} or use a concurrent implementation.</p>
     *
     * <p><b>Warning:</b> The returned Class object does NOT contain actual type parameter information
     * due to Java's type erasure. The {@code eleCls} parameter is only used for compile-time type
     * inference.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableSet<String>> treeSetClass = Clazz.ofTreeSet(String.class);
     *
     * // Elements will be automatically sorted
     * NavigableSet<Integer> numbers = new TreeSet<>();
     * numbers.addAll(Arrays.asList(5, 2, 8, 1));
     * // numbers now contains: [1, 2, 5, 8]
     * }</pre>
     *
     * @param <T> the element type of the tree set.
     * @param eleCls the class of elements (used only for type inference, not retained at runtime).
     * @return the Class object representing the {@code TreeSet} concrete class.
     * @see #ofNavigableSet(Class) for the NavigableSet interface
     * @see #ofSortedSet(Class) for the SortedSet interface
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<NavigableSet<T>> ofTreeSet(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) TreeSet.class;
    }

    /**
     * Returns a Class reference for {@code Queue} with unspecified element type.
     * Queue represents a collection designed for holding elements prior to processing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Queue<String>> queueClass = Clazz.ofQueue();
     * }</pre>
     *
     * @param <T> the element type of the queue.
     * @return the Class object representing {@code Queue<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue() {
        return (Class) Queue.class;
    }

    /**
     * Returns a Class reference for {@code Queue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Queue<Task>> taskQueueClass = Clazz.ofQueue(Task.class);
     * }</pre>
     *
     * @param <T> the element type of the queue.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code Queue<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Queue.class;
    }

    /**
     * Returns a Class reference for {@code Deque} with unspecified element type.
     * Deque (double-ended queue) supports element insertion and removal at both ends.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Deque<String>> dequeClass = Clazz.ofDeque();
     * }</pre>
     *
     * @param <T> the element type of the deque.
     * @return the Class object representing {@code Deque<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque() {
        return (Class) Deque.class;
    }

    /**
     * Returns a Class reference for {@code Deque} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Deque<Integer>> intDequeClass = Clazz.ofDeque(Integer.class);
     * }</pre>
     *
     * @param <T> the element type of the deque.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code Deque<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofDeque(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Deque.class;
    }

    /**
     * Returns a Class reference for {@code ArrayDeque} with unspecified element type.
     * ArrayDeque is a resizable-array implementation of the Deque interface.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Deque<String>> arrayDequeClass = Clazz.ofArrayDeque();
     * }</pre>
     *
     * @param <T> the element type of the array deque.
     * @return the Class object representing {@code ArrayDeque<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque() {
        return (Class) ArrayDeque.class;
    }

    /**
     * Returns a Class reference for {@code ArrayDeque} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Deque<String>> arrayDequeClass = Clazz.ofArrayDeque(String.class);
     * }</pre>
     *
     * @param <T> the element type of the array deque.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code ArrayDeque<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Deque<T>> ofArrayDeque(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) ArrayDeque.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentLinkedQueue} with unspecified element type.
     * ConcurrentLinkedQueue is an unbounded thread-safe queue based on linked nodes.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Queue<String>> concurrentQueueClass = Clazz.ofConcurrentLinkedQueue();
     * }</pre>
     *
     * @param <T> the element type of the concurrent queue.
     * @return the Class object representing {@code ConcurrentLinkedQueue<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue() {
        return (Class) ConcurrentLinkedQueue.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentLinkedQueue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Queue<Message>> messageQueueClass = Clazz.ofConcurrentLinkedQueue(Message.class);
     * }</pre>
     *
     * @param <T> the element type of the concurrent queue.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code ConcurrentLinkedQueue<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofConcurrentLinkedQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) ConcurrentLinkedQueue.class;
    }

    /**
     * Returns a Class reference for {@code PriorityQueue} with unspecified element type.
     * PriorityQueue is an unbounded priority queue based on a priority heap.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Queue<Task>> priorityQueueClass = Clazz.ofPriorityQueue();
     * }</pre>
     *
     * @param <T> the element type of the priority queue.
     * @return the Class object representing {@code PriorityQueue<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue() {
        return (Class) PriorityQueue.class;
    }

    /**
     * Returns a Class reference for {@code PriorityQueue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Queue<Integer>> intPriorityQueueClass = Clazz.ofPriorityQueue(Integer.class);
     * }</pre>
     *
     * @param <T> the element type of the priority queue.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code PriorityQueue<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Queue<T>> ofPriorityQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) PriorityQueue.class;
    }

    /**
     * Returns a Class reference for {@code LinkedBlockingQueue} with unspecified element type.
     * LinkedBlockingQueue is an optionally-bounded blocking queue based on linked nodes.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<BlockingQueue<String>> blockingQueueClass = Clazz.ofLinkedBlockingQueue();
     * }</pre>
     *
     * @param <T> the element type of the blocking queue.
     * @return the Class object representing {@code LinkedBlockingQueue<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue() {
        return (Class) LinkedBlockingQueue.class;
    }

    /**
     * Returns a Class reference for {@code LinkedBlockingQueue} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     *
     * <p>LinkedBlockingQueue is an optionally-bounded blocking queue based on linked nodes.
     * It implements the {@code BlockingQueue} interface and provides thread-safe FIFO ordering of elements.
     * The queue can be created with or without a capacity constraint. If no capacity is specified,
     * it defaults to {@code Integer.MAX_VALUE}.</p>
     *
     * <p><b>Thread Safety:</b> This queue is fully thread-safe. All queue operations are atomic and
     * blocking methods like {@code put()} and {@code take()} will wait until space is available or
     * an element becomes available, respectively.</p>
     *
     * <p><b>Use Cases:</b> Ideal for producer-consumer scenarios where you need to coordinate work
     * between multiple threads. Commonly used in thread pools, async processing pipelines, and
     * message passing between threads.</p>
     *
     * <p><b>Warning:</b> The returned Class object does NOT contain actual type parameter information
     * due to Java's type erasure. The {@code eleCls} parameter is only used for compile-time type
     * inference.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<BlockingQueue<Task>> taskQueueClass = Clazz.ofLinkedBlockingQueue(Task.class);
     *
     * // Producer-consumer pattern
     * BlockingQueue<Task> queue = new LinkedBlockingQueue<>(100);   // capacity of 100
     * queue.put(new Task());                                        // blocks if queue is full
     * Task task = queue.take();                                     // blocks if queue is empty
     * }</pre>
     *
     * @param <T> the element type of the blocking queue.
     * @param eleCls the class of elements (used only for type inference, not retained at runtime).
     * @return the Class object representing the {@code LinkedBlockingQueue} concrete class.
     * @see #ofQueue(Class) for a non-blocking queue
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<BlockingQueue<T>> ofLinkedBlockingQueue(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) LinkedBlockingQueue.class;
    }

    /**
     * Returns a Class reference for {@code Collection} with unspecified element type.
     * Collection is the root interface in the collection hierarchy.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Collection<String>> collectionClass = Clazz.ofCollection();
     * }</pre>
     *
     * @param <T> the element type of the collection.
     * @return the Class object representing {@code Collection<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection() {
        return (Class) Collection.class;
    }

    /**
     * Returns a Class reference for {@code Collection} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Collection<User>> userCollectionClass = Clazz.ofCollection(User.class);
     * }</pre>
     *
     * @param <T> the element type of the collection.
     * @param eleCls the class of elements (used only for type inference).
     * @return the Class object representing {@code Collection<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Collection<T>> ofCollection(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Collection.class;
    }

    /**
     * Returns a Class reference for {@code Map} with unspecified key and value types.
     * This is equivalent to {@code Map.class} but with proper generic typing.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Map<String, Object>> mapClass = Clazz.ofMap();
     * }</pre>
     *
     * @param <K> the key type of the map.
     * @param <V> the value type of the map.
     * @return the Class object representing {@code Map<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap() {
        return (Class) Map.class;
    }

    /**
     * Returns a Class reference for {@code Map} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Map<String, Integer>> stringIntMapClass = Clazz.ofMap(String.class, Integer.class);
     * Class<Map<Long, User>> userMapClass = Clazz.ofMap(Long.class, User.class);
     * }</pre>
     *
     * @param <K> the key type of the map.
     * @param <V> the value type of the map.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code Map<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofMap(@SuppressWarnings("unused") final Class<K> keyCls, @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) Map.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashMap} with unspecified key and value types.
     * LinkedHashMap maintains insertion order of entries.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Map<String, Object>> linkedMapClass = Clazz.ofLinkedHashMap();
     * }</pre>
     *
     * @param <K> the key type of the map.
     * @param <V> the value type of the map.
     * @return the Class object representing {@code LinkedHashMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofLinkedHashMap() {
        return (Class) LinkedHashMap.class;
    }

    /**
     * Returns a Class reference for {@code LinkedHashMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Map<String, Object>> propsMapClass = Clazz.ofLinkedHashMap(String.class, Object.class);
     * }</pre>
     *
     * @param <K> the key type of the map.
     * @param <V> the value type of the map.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code LinkedHashMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<Map<K, V>> ofLinkedHashMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) LinkedHashMap.class;
    }

    /**
     * Returns a Class reference for {@code SortedMap} with unspecified key and value types.
     * SortedMap maintains mappings in ascending key order.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<SortedMap<String, Integer>> sortedMapClass = Clazz.ofSortedMap();
     * }</pre>
     *
     * @param <K> the key type of the sorted map.
     * @param <V> the value type of the sorted map.
     * @return the Class object representing {@code SortedMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<SortedMap<K, V>> ofSortedMap() {
        return (Class) SortedMap.class;
    }

    /**
     * Returns a Class reference for {@code SortedMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<SortedMap<Integer, String>> sortedMapClass = Clazz.ofSortedMap(Integer.class, String.class);
     * }</pre>
     *
     * @param <K> the key type of the sorted map.
     * @param <V> the value type of the sorted map.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code SortedMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<SortedMap<K, V>> ofSortedMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) SortedMap.class;
    }

    /**
     * Returns a Class reference for {@code NavigableMap} with unspecified key and value types.
     * NavigableMap extends SortedMap with navigation methods returning the closest matches for given search targets.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableMap<String, Object>> navigableMapClass = Clazz.ofNavigableMap();
     * }</pre>
     *
     * @param <K> the key type of the navigable map.
     * @param <V> the value type of the navigable map.
     * @return the Class object representing {@code NavigableMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofNavigableMap() {
        return (Class) NavigableMap.class;
    }

    /**
     * Returns a Class reference for {@code NavigableMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableMap<Date, Event>> eventMapClass = Clazz.ofNavigableMap(Date.class, Event.class);
     * }</pre>
     *
     * @param <K> the key type of the navigable map.
     * @param <V> the value type of the navigable map.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code NavigableMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofNavigableMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) NavigableMap.class;
    }

    /**
     * Returns a Class reference for {@code TreeMap} with unspecified key and value types.
     * TreeMap is a Red-Black tree based NavigableMap implementation.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableMap<String, Integer>> treeMapClass = Clazz.ofTreeMap();
     * }</pre>
     *
     * @param <K> the key type of the tree map.
     * @param <V> the value type of the tree map.
     * @return the Class object representing {@code TreeMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofTreeMap() {
        return (Class) TreeMap.class;
    }

    /**
     * Returns a Class reference for {@code TreeMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<NavigableMap<String, List<String>>> treeMapClass = Clazz.ofTreeMap(String.class, List.class);
     * }</pre>
     *
     * @param <K> the key type of the tree map.
     * @param <V> the value type of the tree map.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code TreeMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<NavigableMap<K, V>> ofTreeMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) TreeMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentMap} with unspecified key and value types.
     * ConcurrentMap provides thread-safe operations on a map.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<ConcurrentMap<String, Object>> concurrentMapClass = Clazz.ofConcurrentMap();
     * }</pre>
     *
     * @param <K> the key type of the concurrent map.
     * @param <V> the value type of the concurrent map.
     * @return the Class object representing {@code ConcurrentMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentMap() {
        return (Class) ConcurrentMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<ConcurrentMap<Long, Session>> sessionMapClass = Clazz.ofConcurrentMap(Long.class, Session.class);
     * }</pre>
     *
     * @param <K> the key type of the concurrent map.
     * @param <V> the value type of the concurrent map.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueCls the class of map values (used only for type inference).
     * @return the Class object representing {@code ConcurrentMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) ConcurrentMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentHashMap} with unspecified key and value types.
     * ConcurrentHashMap is a hash table supporting full concurrency of retrievals and high expected concurrency for updates.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<ConcurrentMap<String, Object>> concurrentHashMapClass = Clazz.ofConcurrentHashMap();
     * }</pre>
     *
     * @param <K> the key type of the concurrent hash map.
     * @param <V> the value type of the concurrent hash map.
     * @return the Class object representing {@code ConcurrentHashMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentHashMap() {
        return (Class) ConcurrentHashMap.class;
    }

    /**
     * Returns a Class reference for {@code ConcurrentHashMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     *
     * <p>ConcurrentHashMap is a thread-safe implementation that provides high concurrency for both
     * read and write operations. It uses lock striping to allow multiple threads to update the map
     * concurrently without blocking each other. Unlike Hashtable or synchronized HashMap, it does
     * not lock the entire map, making it ideal for high-concurrency scenarios.</p>
     *
     * <p><b>Thread Safety:</b> This implementation is fully thread-safe. Retrieval operations do not
     * block, and updates can be performed concurrently by multiple threads.</p>
     *
     * <p><b>Warning:</b> The returned Class object does NOT contain actual type parameter information
     * due to Java's type erasure. The {@code keyCls} and {@code valueCls} parameters are only used
     * for compile-time type inference.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For thread-safe caching or shared state
     * Class<ConcurrentMap<String, AtomicInteger>> counterMapClass =
     *     Clazz.ofConcurrentHashMap(String.class, AtomicInteger.class);
     *
     * // Common use case
     * ConcurrentMap<String, User> userCache = new ConcurrentHashMap<>();
     * Class<ConcurrentMap<String, User>> cacheType = Clazz.ofConcurrentHashMap(String.class, User.class);
     * }</pre>
     *
     * @param <K> the key type of the concurrent hash map.
     * @param <V> the value type of the concurrent hash map.
     * @param keyCls the class of map keys (used only for type inference, not retained at runtime).
     * @param valueCls the class of map values (used only for type inference, not retained at runtime).
     * @return the Class object representing the {@code ConcurrentHashMap} concrete class.
     * @see #ofConcurrentMap(Class, Class) for the ConcurrentMap interface
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<ConcurrentMap<K, V>> ofConcurrentHashMap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) ConcurrentHashMap.class;
    }

    /**
     * Returns a Class reference for {@code BiMap} with unspecified key and value types.
     * BiMap is a bidirectional map that preserves the uniqueness of its values as well as that of its keys.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<BiMap<String, Integer>> biMapClass = Clazz.ofBiMap();
     * }</pre>
     *
     * @param <K> the key type of the bimap.
     * @param <V> the value type of the bimap.
     * @return the Class object representing {@code BiMap<K, V>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap() {
        return (Class) BiMap.class;
    }

    /**
     * Returns a Class reference for {@code BiMap} with the specified key and value types.
     * The key and value class parameters are used only for type inference and are not retained at runtime.
     *
     * <p>BiMap (Bidirectional Map) is a specialized map that maintains a one-to-one correspondence
     * between keys and values. Unlike a regular map, both keys AND values must be unique. BiMap
     * provides an {@code inverse()} view that treats values as keys and keys as values, enabling
     * efficient bidirectional lookups.</p>
     *
     * <p><b>Uniqueness Constraint:</b> If you attempt to insert a value that already exists,
     * BiMap will throw an {@code IllegalArgumentException}. Use {@code forcePut()} to replace
     * both the existing key-value and value-key mappings.</p>
     *
     * <p><b>Use Cases:</b> Ideal for scenarios requiring bidirectional lookup, such as:
     * <ul>
     *   <li>ID to name mappings (and vice versa)</li>
     *   <li>Code to description dictionaries</li>
     *   <li>Two-way translation tables</li>
     * </ul>
     *
     * <p><b>Warning:</b> The returned Class object does NOT contain actual type parameter information
     * due to Java's type erasure. The {@code keyCls} and {@code valueCls} parameters are only used
     * for compile-time type inference.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<BiMap<String, Integer>> idMapClass = Clazz.ofBiMap(String.class, Integer.class);
     *
     * // BiMap enforces uniqueness on both keys and values
     * BiMap<String, Integer> userIds = BiMap.create();
     * userIds.put("alice", 101);
     * userIds.put("bob", 102);
     * String name = userIds.inverse().get(101);   // returns "alice"
     * }</pre>
     *
     * @param <K> the key type of the bimap.
     * @param <V> the value type of the bimap.
     * @param keyCls the class of map keys (used only for type inference, not retained at runtime).
     * @param valueCls the class of map values (used only for type inference, not retained at runtime).
     * @return the Class object representing the {@code BiMap} interface.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Class<BiMap<K, V>> ofBiMap(@SuppressWarnings("unused") final Class<K> keyCls, @SuppressWarnings("unused") final Class<V> valueCls) {
        return (Class) BiMap.class;
    }

    /**
     * Returns a Class reference for {@code Multiset} with unspecified element type.
     * Multiset is a collection that supports order-independent equality, like Set, but may have duplicate elements.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Multiset<String>> multisetClass = Clazz.ofMultiset();
     * }</pre>
     *
     * @param <T> the element type of the multiset.
     * @return the Class object representing {@code Multiset<T>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset() {
        return (Class) Multiset.class;
    }

    /**
     * Returns a Class reference for {@code Multiset} with the specified element type.
     * The element class parameter is used only for type inference and is not retained at runtime.
     *
     * <p>Multiset (also known as a bag) is a collection that supports order-independent equality
     * like {@code Set}, but may contain duplicate elements. It maintains a count of how many times
     * each element appears. Unlike {@code List}, the order of elements is not significant, and
     * unlike {@code Set}, duplicates are allowed and counted.</p>
     *
     * <p><b>Key Features:</b>
     * <ul>
     *   <li>Tracks element occurrences/counts</li>
     *   <li>Efficient count queries: {@code count(element)}</li>
     *   <li>Add/remove operations update occurrence counts</li>
     *   <li>Order-independent equality</li>
     * </ul>
     *
     * <p><b>Use Cases:</b> Ideal for scenarios requiring frequency counting, such as:
     * <ul>
     *   <li>Word frequency counting in text analysis</li>
     *   <li>Histogram data structures</li>
     *   <li>Statistical aggregation</li>
     *   <li>Voting/polling systems</li>
     * </ul>
     *
     * <p><b>Warning:</b> The returned Class object does NOT contain actual type parameter information
     * due to Java's type erasure. The {@code eleCls} parameter is only used for compile-time type
     * inference.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<Multiset<String>> wordCountClass = Clazz.ofMultiset(String.class);
     *
     * // Count word occurrences
     * Multiset<String> words = Multiset.create();
     * words.add("apple");
     * words.add("banana");
     * words.add("apple");
     * int count = words.count("apple");   // returns 2
     * }</pre>
     *
     * @param <T> the element type of the multiset.
     * @param eleCls the class of elements (used only for type inference, not retained at runtime).
     * @return the Class object representing the {@code Multiset} interface.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <T> Class<Multiset<T>> ofMultiset(@SuppressWarnings("unused") final Class<T> eleCls) {
        return (Class) Multiset.class;
    }

    /**
     * Returns a Class reference for {@code ListMultimap} with unspecified key and value element types.
     * ListMultimap is a multimap that can hold multiple values per key in a List collection.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<ListMultimap<String, Integer>> multiMapClass = Clazz.ofListMultimap();
     * }</pre>
     *
     * @param <K> the key type of the list multimap.
     * @param <E> the element type of the value collections.
     * @return the Class object representing {@code ListMultimap<K, E>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<ListMultimap<K, E>> ofListMultimap() {
        return (Class) ListMultimap.class;
    }

    /**
     * Returns a Class reference for {@code ListMultimap} with the specified key and value element types.
     * The key and value element class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<ListMultimap<String, Integer>> groupedDataClass = 
     *     Clazz.ofListMultimap(String.class, Integer.class);
     * }</pre>
     *
     * @param <K> the key type of the list multimap.
     * @param <E> the element type of the value collections.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueEleCls the class of value collection elements (used only for type inference).
     * @return the Class object representing {@code ListMultimap<K, E>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<ListMultimap<K, E>> ofListMultimap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<E> valueEleCls) {
        return (Class) ListMultimap.class;
    }

    /**
     * Returns a Class reference for {@code SetMultimap} with unspecified key and value element types.
     * SetMultimap is a multimap that can hold multiple unique values per key in a Set collection.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<SetMultimap<String, Integer>> multiMapClass = Clazz.ofSetMultimap();
     * }</pre>
     *
     * @param <K> the key type of the set multimap.
     * @param <E> the element type of the value collections.
     * @return the Class object representing {@code SetMultimap<K, E>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<SetMultimap<K, E>> ofSetMultimap() {
        return (Class) SetMultimap.class;
    }

    /**
     * Returns a Class reference for {@code SetMultimap} with the specified key and value element types.
     * The key and value element class parameters are used only for type inference and are not retained at runtime.
     * 
     * <p><b>Warning:</b> The returned Class object does not contain actual type parameter information.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Class<SetMultimap<String, String>> tagMapClass = 
     *     Clazz.ofSetMultimap(String.class, String.class);
     * }</pre>
     *
     * @param <K> the key type of the set multimap.
     * @param <E> the element type of the value collections.
     * @param keyCls the class of map keys (used only for type inference).
     * @param valueEleCls the class of value collection elements (used only for type inference).
     * @return the Class object representing {@code SetMultimap<K, E>}.
     * @see TypeReference#type()
     * @see com.landawn.abacus.type.Type#of(String)
     * @see com.landawn.abacus.type.Type#of(Class)
     */
    @SuppressWarnings("rawtypes")
    public static <K, E> Class<SetMultimap<K, E>> ofSetMultimap(@SuppressWarnings("unused") final Class<K> keyCls,
            @SuppressWarnings("unused") final Class<E> valueEleCls) {
        return (Class) SetMultimap.class;
    }
}
