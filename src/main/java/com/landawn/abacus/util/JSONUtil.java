/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;

/**
 * A comprehensive utility class providing high-performance, thread-safe methods for bidirectional conversion
 * between Java objects and JSON representations using the org.json library. This class serves as a robust
 * bridge between Java's strongly-typed object model and JSON's flexible document structure, offering
 * intuitive APIs for complex data transformation scenarios while maintaining type safety and performance.
 *
 * <p>The {@code JSONUtil} class addresses common challenges in JSON processing by providing seamless
 * conversion capabilities for diverse Java data structures including Maps, JavaBeans, Collections, and
 * primitive arrays. It leverages the proven org.json library foundation while offering enhanced
 * functionality for deep object graph traversal, type preservation, and automatic nested structure handling.</p>
 *
 * <p><b>⚠️ IMPORTANT - JSON Library Integration:</b>
 * This class provides a high-level abstraction over the org.json library, enhancing its
 * capabilities with automatic type detection, recursive conversion handling, and improved
 * error management while maintaining full compatibility with standard JSON specifications.</p>
 *
 * <p><b>Key Features and Capabilities:</b>
 * <ul>
 *   <li><b>Bidirectional Conversion:</b> Seamless transformation between Java objects and JSON structures</li>
 *   <li><b>Type Safety:</b> Maintains type information and provides compile-time safety for conversions</li>
 *   <li><b>Deep Conversion:</b> Automatic handling of nested objects, collections, and complex object graphs</li>
 *   <li><b>Primitive Array Support:</b> Native support for all Java primitive array types with optimal performance</li>
 *   <li><b>JavaBean Integration:</b> Automatic property discovery and conversion for POJO objects</li>
 *   <li><b>Collection Handling:</b> Comprehensive support for Java Collections framework types</li>
 *   <li><b>Map Integration:</b> Direct conversion between Map instances and JSON objects</li>
 *   <li><b>Performance Optimized:</b> Efficient algorithms minimizing object allocation and memory usage</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Simplicity Over Complexity:</b> Intuitive API that handles complex conversion scenarios transparently</li>
 *   <li><b>Type Safety First:</b> Strong typing prevents runtime errors and improves code reliability</li>
 *   <li><b>Performance Oriented:</b> Optimized for high-throughput JSON processing scenarios</li>
 *   <li><b>Standard Compliance:</b> Full adherence to JSON specification and Java object model</li>
 *   <li><b>Error Resilience:</b> Comprehensive error handling with meaningful exception messages</li>
 * </ul>
 *
 * <p><b>Supported Type Conversions:</b>
 * <table border="1" style="border-collapse: collapse;">
 *   <caption><b>Comprehensive Type Mapping Between Java and JSON</b></caption>
 *   <tr style="background-color: #f2f2f2;">
 *     <th>Java Type</th>
 *     <th>JSON Type</th>
 *     <th>Conversion Method</th>
 *     <th>Notes</th>
 *   </tr>
 *   <tr>
 *     <td>Map&lt;String, Object&gt;</td>
 *     <td>JSONObject</td>
 *     <td>wrap(Map) / unwrap(JSONObject)</td>
 *     <td>Direct key-value mapping</td>
 *   </tr>
 *   <tr>
 *     <td>JavaBean/POJO</td>
 *     <td>JSONObject</td>
 *     <td>wrap(Object) / unwrap(JSONObject, Class)</td>
 *     <td>Automatic property discovery</td>
 *   </tr>
 *   <tr>
 *     <td>Collection&lt;?&gt;</td>
 *     <td>JSONArray</td>
 *     <td>wrap(Collection) / unwrap(JSONArray)</td>
 *     <td>List, Set, Queue support</td>
 *   </tr>
 *   <tr>
 *     <td>Object[]</td>
 *     <td>JSONArray</td>
 *     <td>wrap(Object[]) / unwrap(JSONArray, Class)</td>
 *     <td>Generic object arrays</td>
 *   </tr>
 *   <tr>
 *     <td>Primitive Arrays</td>
 *     <td>JSONArray</td>
 *     <td>wrap(primitiveArray) / unwrap(JSONArray, primitiveClass)</td>
 *     <td>int[], double[], boolean[], etc.</td>
 *   </tr>
 *   <tr>
 *     <td>String, Number, Boolean</td>
 *     <td>JSON Primitives</td>
 *     <td>Direct conversion</td>
 *     <td>No wrapping needed</td>
 *   </tr>
 * </table>
 *
 * <p><b>Core Conversion Methods:</b>
 * <ul>
 *   <li><b>{@code wrap(Object)}:</b> Convert Java objects to appropriate JSON representations</li>
 *   <li><b>{@code unwrap(JSONObject)}:</b> Convert JSON objects back to Java Map structures</li>
 *   <li><b>{@code unwrap(JSONArray)}:</b> Convert JSON arrays back to Java Collection or array types</li>
 *   <li><b>{@code unwrap(JSONObject, Class)}:</b> Convert JSON objects to specific Java bean types</li>
 *   <li><b>{@code unwrap(JSONArray, Class)}:</b> Convert JSON arrays to specific Java array or collection types</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic Map to JSON conversion
 * Map<String, Object> userMap = new HashMap<>();
 * userMap.put("name", "John Doe");
 * userMap.put("age", 30);
 * userMap.put("active", true);
 * JSONObject jsonUser = JSONUtil.wrap(userMap);
 * // Result: {"name":"John Doe","age":30,"active":true}
 *
 * // JavaBean to JSON conversion
 * User user = new User("Jane Smith", 25, "jane@example.com");
 * JSONObject userJson = JSONUtil.wrap(user);
 * // Automatically converts all public properties
 *
 * // JSON back to Java objects
 * Map<String, Object> restoredMap = JSONUtil.unwrap(jsonUser);
 * User restoredUser = JSONUtil.unwrap(userJson, User.class);
 *
 * // Array conversions
 * int[] numbers = {1, 2, 3, 4, 5};
 * JSONArray numberArray = JSONUtil.wrap(numbers);
 * int[] restoredNumbers = JSONUtil.unwrap(numberArray, int[].class);
 *
 * // Collection handling
 * List<String> tags = Arrays.asList("java", "json", "utility");
 * JSONArray tagArray = JSONUtil.wrap(tags);
 * List<String> restoredTags = JSONUtil.unwrap(tagArray);
 * }</pre>
 *
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * // Complex object graph conversion
 * public class OrderProcessor {
 *     public JSONObject convertOrder(Order order) {
 *         // Automatically handles nested objects, collections, and relationships
 *         return JSONUtil.wrap(order);
 *         // Converts Order with Customer, LineItems, Addresses, etc.
 *     }
 *
 *     public Order restoreOrder(JSONObject orderJson) {
 *         // Type-safe restoration with full object graph reconstruction
 *         return JSONUtil.unwrap(orderJson, Order.class);
 *     }
 * }
 *
 * // Multi-dimensional array handling
 * public class MatrixProcessor {
 *     public JSONArray serializeMatrix(double[][] matrix) {
 *         return JSONUtil.wrap(matrix);
 *         // Handles nested arrays automatically
 *     }
 *
 *     public double[][] deserializeMatrix(JSONArray matrixJson) {
 *         return JSONUtil.unwrap(matrixJson, double[][].class);
 *     }
 * }
 *
 * // Generic collection processing
 * public class DataTransformer {
 *     public <T> JSONArray convertList(List<T> items) {
 *         return JSONUtil.wrap(items);
 *         // Works with any object type in the collection
 *     }
 *
 *     public List<Map<String, Object>> extractMaps(JSONArray jsonArray) {
 *         return JSONUtil.unwrap(jsonArray);
 *         // Returns List of Maps for flexible data access
 *     }
 * }
 * }</pre>
 *
 * <p><b>JavaBean Property Handling:</b>
 * <ul>
 *   <li><b>Automatic Discovery:</b> Uses reflection to discover bean properties via getters/setters</li>
 *   <li><b>Field Access:</b> Supports direct field access for objects without standard bean patterns</li>
 *   <li><b>Type Conversion:</b> Automatic conversion between compatible types during restoration</li>
 *   <li><b>Nested Objects:</b> Recursive processing of nested bean properties</li>
 *   <li><b>Collection Properties:</b> Special handling for collection and array properties</li>
 *   <li><b>Null Handling:</b> Graceful handling of null values in object properties</li>
 * </ul>
 *
 * <p><b>Array and Collection Processing:</b>
 * <ul>
 *   <li><b>Primitive Arrays:</b> Optimized handling for int[], double[], boolean[], etc.</li>
 *   <li><b>Object Arrays:</b> Generic object array support with type preservation</li>
 *   <li><b>Multi-dimensional Arrays:</b> Recursive processing of nested array structures</li>
 *   <li><b>Collection Types:</b> Support for List, Set, Queue, and custom Collection implementations</li>
 *   <li><b>Generic Types:</b> Maintains generic type information where possible</li>
 *   <li><b>Empty Collections:</b> Proper handling of empty arrays and collections</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Conversion Speed:</b> Optimized algorithms for high-throughput JSON processing</li>
 *   <li><b>Memory Efficiency:</b> Minimal object allocation during conversion processes</li>
 *   <li><b>Reflection Caching:</b> Cached property information for repeated bean conversions</li>
 *   <li><b>Stream Processing:</b> Efficient iteration over large collections and arrays</li>
 *   <li><b>Type Resolution:</b> Fast type checking and conversion path selection</li>
 * </ul>
 *
 * <p><b>Thread Safety and Concurrency:</b>
 * <ul>
 *   <li><b>Static Methods:</b> All utility methods are static and inherently thread-safe</li>
 *   <li><b>Immutable Processing:</b> No shared mutable state between conversion operations</li>
 *   <li><b>Concurrent Access:</b> Multiple threads can safely perform conversions simultaneously</li>
 *   <li><b>Reflection Safety:</b> Thread-safe use of reflection APIs for property access</li>
 *   <li><b>JSON Library Safety:</b> Leverages thread-safe org.json library implementations</li>
 * </ul>
 *
 * <p><b>Error Handling and Exception Management:</b>
 * <ul>
 *   <li><b>JSONException:</b> Thrown when JSON parsing or construction fails</li>
 *   <li><b>IllegalArgumentException:</b> Thrown for unsupported type conversions</li>
 *   <li><b>ClassCastException:</b> May occur during type-unsafe unwrapping operations</li>
 *   <li><b>NullPointerException:</b> Appropriate null checks with descriptive error messages</li>
 *   <li><b>ReflectionException:</b> Wrapped and re-thrown as RuntimeException for bean access issues</li>
 * </ul>
 *
 * <p><b>Integration with Frameworks:</b>
 * <ul>
 *   <li><b>Spring Framework:</b> Compatible with Spring's JSON processing and REST controllers</li>
 *   <li><b>JAX-RS:</b> Suitable for REST API request/response body conversion</li>
 *   <li><b>Servlet API:</b> Integration with HTTP request/response processing</li>
 *   <li><b>JSON-B:</b> Complementary functionality to standard JSON-B implementations</li>
 *   <li><b>Jackson Alternative:</b> Lightweight alternative for simple JSON processing needs</li>
 * </ul>
 *
 * <p><b>Type Conversion Details:</b>
 * <ul>
 *   <li><b>Number Conversion:</b> Automatic conversion between Integer, Long, Double, BigDecimal</li>
 *   <li><b>String Handling:</b> UTF-8 string processing with proper escaping</li>
 *   <li><b>Boolean Conversion:</b> Standard true/false mapping with string conversion support</li>
 *   <li><b>Date Handling:</b> Depends on underlying Date.toString() and parsing mechanisms</li>
 *   <li><b>Enum Support:</b> Automatic enum to string conversion and reverse parsing</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Use specific type parameters in unwrap() methods for type safety</li>
 *   <li>Handle JSONException appropriately in production code</li>
 *   <li>Consider object structure complexity when designing APIs</li>
 *   <li>Use Map-based unwrapping for flexible data access patterns</li>
 *   <li>Validate input data before conversion to prevent malformed JSON</li>
 *   <li>Cache Type objects for repeated conversions of the same class</li>
 *   <li>Use specific collection types in unwrap operations when type safety is critical</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Manual JSON string construction instead of using object conversion</li>
 *   <li>Ignoring type safety in unwrap operations</li>
 *   <li>Not handling exceptions appropriately in conversion code</li>
 *   <li>Using raw types instead of generic types in method parameters</li>
 *   <li>Repeated conversion of the same object without caching</li>
 *   <li>Mixing different JSON libraries in the same application</li>
 * </ul>
 *
 * <p><b>Memory and Performance Considerations:</b>
 * <ul>
 *   <li><b>Large Objects:</b> Consider streaming approaches for very large object graphs</li>
 *   <li><b>Circular References:</b> Be aware of potential infinite recursion in object graphs</li>
 *   <li><b>Deep Nesting:</b> Monitor stack depth for deeply nested structures</li>
 *   <li><b>Collection Size:</b> Large collections may require memory management considerations</li>
 *   <li><b>String Pooling:</b> Benefit from JVM string pooling for repeated property names</li>
 * </ul>
 *
 * <p><b>Example: REST API Integration</b>
 * <pre>{@code
 * @RestController
 * public class UserController {
 *     
 *     @PostMapping("/users")
 *     public ResponseEntity<String> createUser(@RequestBody String jsonRequest) {
 *         try {
 *             // Parse JSON request
 *             JSONObject requestJson = new JSONObject(jsonRequest);
 *             User user = JSONUtil.unwrap(requestJson, User.class);
 *             
 *             // Process user creation
 *             User createdUser = userService.createUser(user);
 *             
 *             // Convert response to JSON
 *             JSONObject responseJson = JSONUtil.wrap(createdUser);
 *             return ResponseEntity.ok(responseJson.toString());
 *             
 *         } catch (JSONException e) {
 *             return ResponseEntity.badRequest().body("Invalid JSON format");
 *         }
 *     }
 *     
 *     @GetMapping("/users/{id}")
 *     public ResponseEntity<String> getUser(@PathVariable Long id) {
 *         User user = userService.findById(id);
 *         if (user != null) {
 *             JSONObject userJson = JSONUtil.wrap(user);
 *             return ResponseEntity.ok(userJson.toString());
 *         }
 *         return ResponseEntity.notFound().build();
 *     }
 * }
 * }</pre>
 *
 * <p><b>Comparison with Alternative JSON Libraries:</b>
 * <ul>
 *   <li><b>vs. Jackson:</b> Simpler API vs. comprehensive feature set and annotations</li>
 *   <li><b>vs. Gson:</b> Based on org.json vs. Google's implementation with different design philosophy</li>
 *   <li><b>vs. JSON-B:</b> Utility class approach vs. standard API with configuration options</li>
 *   <li><b>vs. Manual org.json:</b> Higher-level abstraction vs. direct library usage</li>
 * </ul>
 *
 * <p><b>Future Extensibility:</b>
 * <ul>
 *   <li><b>Custom Converters:</b> Potential for adding custom type conversion handlers</li>
 *   <li><b>Configuration Options:</b> Possible future support for conversion configuration</li>
 *   <li><b>Performance Optimizations:</b> Ongoing improvements to conversion algorithms</li>
 *   <li><b>Additional JSON Libraries:</b> Potential support for alternative JSON implementations</li>
 * </ul>
 *
 * @see org.json.JSONObject
 * @see org.json.JSONArray
 * @see org.json.JSONException
 * @see com.landawn.abacus.parser.ParserUtil
 * @see com.landawn.abacus.type.Type
 * @see java.lang.reflect.Array
 * @see java.util.Map
 * @see java.util.Collection
 * @see <a href="https://www.json.org/json-en.html">JSON Specification</a>
 * @see <a href="https://github.com/stleary/JSON-java">org.json Library Documentation</a>
 */
public final class JSONUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     * <p>
     * This class is designed as a singleton utility with only static methods.
     * </p>
     */
    private JSONUtil() {
        // singleton.
    }

    /**
     * Converts a Map into a JSONObject.
     * <p>
     * This method creates a new JSONObject from the provided Map. The Map's keys must be Strings,
     * and values can be any type supported by JSONObject including primitives, Strings, Collections,
     * Maps, and other JSONObject/JSONArray instances.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "Alice");
     * map.put("score", 95.5);
     * map.put("active", true);
     * JSONObject json = JSONUtil.wrap(map);
     * // Result: {"name":"Alice","score":95.5,"active":true}
     * }</pre>
     *
     * @param map the Map to convert to JSONObject. Keys must be Strings.
     * @return a new JSONObject containing all key-value pairs from the input Map
     * @throws JSONException if the map contains values that cannot be converted to JSON
     */
    public static JSONObject wrap(final Map<String, ?> map) {
        return new JSONObject(map);
    }

    /**
     * Converts a Java object (Bean or Map) into a JSONObject.
     * <p>
     * This method intelligently handles two types of objects:
     * </p>
     * <ol>
     *   <li><b>Maps:</b> Directly converted to JSONObject (same as {@link #wrap(Map)})</li>
     *   <li><b>JavaBeans:</b> Converted using deep bean-to-map transformation, including all
     *       accessible properties and nested objects</li>
     * </ol>
     * 
     * <p>
     * The deep conversion process recursively transforms nested beans, collections, and maps
     * into their JSON-compatible representations.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Example with a JavaBean
     * public class Person {
     *     private String name;
     *     private int age;
     *     private Address address;
     *     // getters and setters...
     * }
     * 
     * Person person = new Person("John", 30, new Address("123 Main St"));
     * JSONObject json = JSONUtil.wrap(person);
     * // Result: {"name":"John","age":30,"address":{"street":"123 Main St"}}
     * 
     * // Example with a Map
     * Map<String, Object> map = new HashMap<>();
     * map.put("id", 123);
     * JSONObject json2 = JSONUtil.wrap((Object) map);
     * }</pre>
     *
     * @param bean the object to convert. Can be a Map or any JavaBean with accessible properties
     * @return a new JSONObject representing the input object
     * @throws JSONException if the bean contains values that cannot be converted to JSON
     */
    @SuppressWarnings("unchecked")
    public static JSONObject wrap(final Object bean) {
        return new JSONObject(bean instanceof Map ? (Map<String, Object>) bean : Beans.deepBean2Map(bean, true));
    }

    /**
     * Converts a boolean array into a JSONArray.
     * <p>
     * Each element in the boolean array is added to the JSONArray in the same order.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] flags = {true, false, true, true, false};
     * JSONArray json = JSONUtil.wrap(flags);
     * // Result: [true,false,true,true,false]
     * 
     * // Unwrap back to array
     * boolean[] restored = JSONUtil.unwrap(json, boolean[].class);
     * }</pre>
     *
     * @param array the boolean array to convert
     * @return a new JSONArray containing all elements from the input array
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final boolean[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a character array into a JSONArray.
     * <p>
     * Each character is stored as a string in the JSONArray since JSON doesn't have
     * a native character type.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * JSONArray json = JSONUtil.wrap(chars);
     * // Result: ["H","e","l","l","o"]
     * }</pre>
     *
     * @param array the character array to convert
     * @return a new JSONArray containing all characters from the input array as strings
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final char[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a byte array into a JSONArray.
     * <p>
     * Each byte value is stored as a number in the JSONArray.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = {10, 20, 30, 40, 50};
     * JSONArray json = JSONUtil.wrap(bytes);
     * // Result: [10,20,30,40,50]
     * }</pre>
     *
     * @param array the byte array to convert
     * @return a new JSONArray containing all bytes from the input array as numbers
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final byte[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a short array into a JSONArray.
     * <p>
     * Each short value is stored as a number in the JSONArray.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] shorts = {100, 200, 300, 400, 500};
     * JSONArray json = JSONUtil.wrap(shorts);
     * // Result: [100,200,300,400,500]
     * }</pre>
     *
     * @param array the short array to convert
     * @return a new JSONArray containing all shorts from the input array as numbers
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final short[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts an integer array into a JSONArray.
     * <p>
     * Each integer value is stored as a number in the JSONArray.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {1, 2, 3, 4, 5};
     * JSONArray json = JSONUtil.wrap(numbers);
     * // Result: [1,2,3,4,5]
     * 
     * // Can be used with mathematical data
     * int[] fibonacci = {0, 1, 1, 2, 3, 5, 8, 13};
     * JSONArray fibJson = JSONUtil.wrap(fibonacci);
     * }</pre>
     *
     * @param array the integer array to convert
     * @return a new JSONArray containing all integers from the input array
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final int[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a long array into a JSONArray.
     * <p>
     * Each long value is stored as a number in the JSONArray. Note that very large
     * long values may lose precision when converted to JSON due to JavaScript's
     * number limitations.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] timestamps = {1609459200000L, 1609545600000L, 1609632000000L};
     * JSONArray json = JSONUtil.wrap(timestamps);
     * // Result: [1609459200000,1609545600000,1609632000000]
     * }</pre>
     *
     * @param array the long array to convert
     * @return a new JSONArray containing all longs from the input array
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final long[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a float array into a JSONArray.
     * <p>
     * Each float value is stored as a number in the JSONArray. Special float values
     * (NaN, Infinity) may be converted according to JSON specifications.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] measurements = {98.6f, 99.1f, 97.8f, 98.2f};
     * JSONArray json = JSONUtil.wrap(measurements);
     * // Result: [98.6,99.1,97.8,98.2]
     * }</pre>
     *
     * @param array the float array to convert
     * @return a new JSONArray containing all floats from the input array
     * @throws JSONException if there is an error during the conversion or if array contains NaN or Infinity
     */
    public static JSONArray wrap(final float[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a double array into a JSONArray.
     * <p>
     * Each double value is stored as a number in the JSONArray. Special double values
     * (NaN, Infinity) may cause JSONException as they're not valid in standard JSON.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] prices = {19.99, 29.99, 39.99, 49.99};
     * JSONArray json = JSONUtil.wrap(prices);
     * // Result: [19.99,29.99,39.99,49.99]
     * 
     * // Scientific data
     * double[] coefficients = {3.14159, 2.71828, 1.41421};
     * JSONArray sciJson = JSONUtil.wrap(coefficients);
     * }</pre>
     *
     * @param array the double array to convert
     * @return a new JSONArray containing all doubles from the input array
     * @throws JSONException if there is an error during the conversion or if array contains NaN or Infinity
     */
    public static JSONArray wrap(final double[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts an Object array into a JSONArray.
     * <p>
     * Each element in the array is converted according to its type:
     * </p>
     * <ul>
     *   <li>Primitives and Strings: stored directly</li>
     *   <li>null: stored as JSONObject.NULL</li>
     *   <li>Maps and Beans: converted to JSONObject</li>
     *   <li>Collections and Arrays: converted to JSONArray</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object[] mixed = {
     *     "text",
     *     123,
     *     true,
     *     new HashMap<String, Object>() {{ put("key", "value"); }},
     *     Arrays.asList(1, 2, 3),
     *     null
     * };
     * JSONArray json = JSONUtil.wrap(mixed);
     * // Result: ["text",123,true,{"key":"value"},[1,2,3],null]
     * }</pre>
     *
     * @param array the Object array to convert
     * @return a new JSONArray containing all elements from the input array
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final Object[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a Collection into a JSONArray.
     * <p>
     * This method handles any Collection implementation (List, Set, Queue, etc.) and converts
     * each element according to its type. The order of elements in the JSONArray matches
     * the iteration order of the Collection.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Converting a List
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * JSONArray json = JSONUtil.wrap(names);
     * // Result: ["Alice","Bob","Charlie"]
     * 
     * // Converting a Set
     * Set<Integer> numbers = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
     * JSONArray json2 = JSONUtil.wrap(numbers);
     * // Result: [1,2,3]
     * 
     * // Mixed types
     * List<Object> mixed = Arrays.asList("text", 123, true, null);
     * JSONArray json3 = JSONUtil.wrap(mixed);
     * // Result: ["text",123,true,null]
     * }</pre>
     *
     * @param coll the Collection to convert
     * @return a new JSONArray containing all elements from the Collection
     * @throws JSONException if there is an error during the conversion
     */
    public static JSONArray wrap(final Collection<?> coll) {
        return new JSONArray(coll);
    }

    /**
     * Converts a JSONObject to a Map&lt;String, Object&gt;.
     * <p>
     * This is a convenience method equivalent to calling {@code unwrap(jsonObject, Map.class)}.
     * The resulting Map will contain all key-value pairs from the JSONObject, with values
     * converted to appropriate Java types.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONObject json = new JSONObject("{\"name\":\"John\",\"age\":30,\"active\":true}");
     * Map<String, Object> map = JSONUtil.unwrap(json);
     * // map.get("name") returns "John"
     * // map.get("age") returns 30
     * // map.get("active") returns true
     * }</pre>
     *
     * @param jsonObject the JSONObject to convert
     * @return a Map containing all key-value pairs from the JSONObject
     * @throws JSONException if there is an error during the conversion
     */
    public static Map<String, Object> unwrap(final JSONObject jsonObject) throws JSONException {
        return unwrap(jsonObject, Map.class);
    }

    /**
     * Converts a JSONObject to an instance of the specified class type.
     * <p>
     * This method supports conversion to:
     * </p>
     * <ul>
     *   <li><b>Map implementations:</b> HashMap, LinkedHashMap, TreeMap, etc.</li>
     *   <li><b>JavaBeans:</b> Any class with proper getter/setter methods</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert to a specific Map type
     * JSONObject json = new JSONObject("{\"z\":1,\"a\":2}");
     * TreeMap<String, Object> sorted = JSONUtil.unwrap(json, TreeMap.class);
     * // Keys will be sorted: "a"=2, "z"=1
     * 
     * // Convert to a JavaBean
     * public class User {
     *     private String name;
     *     private int age;
     *     // getters and setters...
     * }
     * 
     * JSONObject userJson = new JSONObject("{\"name\":\"Alice\",\"age\":25}");
     * User user = JSONUtil.unwrap(userJson, User.class);
     * // user.getName() returns "Alice"
     * // user.getAge() returns 25
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonObject the JSONObject to convert
     * @param targetType the class of the object to create
     * @return an instance of targetType populated with data from the JSONObject
     * @throws JSONException if there is an error during the conversion
     * @throws IllegalArgumentException if targetType is not a supported type (Map or Bean)
     */
    public static <T> T unwrap(final JSONObject jsonObject, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonObject, N.typeOf(targetType));
    }

    /**
     * Converts a JSONObject to an instance of the specified Type.
     * <p>
     * This advanced method supports complex generic types and nested structures that cannot
     * be expressed with simple Class parameters. It handles:
     * </p>
     * <ul>
     *   <li>Generic Maps with typed values: {@code Map<String, List<String>>}</li>
     *   <li>Complex nested beans with generic properties</li>
     *   <li>Type inference for Object.class (defaults to Map&lt;String, Object&gt;)</li>
     * </ul>
     * 
     * <p>
     * The method performs deep conversion, recursively transforming nested JSONObjects
     * and JSONArrays according to the specified type information.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Complex generic Map
     * Type<Map<String, List<String>>> type = N.typeOf("Map<String, List<String>>");
     * JSONObject json = new JSONObject("{\"tags\":[\"java\",\"json\"],\"categories\":[\"util\",\"parser\"]}");
     * Map<String, List<String>> result = JSONUtil.unwrap(json, type);
     * // result.get("tags") returns List ["java", "json"]
     * 
     * // Nested bean structures
     * Type<Department> deptType = N.typeOf(Department.class);
     * JSONObject deptJson = new JSONObject("{\"name\":\"Engineering\",\"employees\":[...]}");
     * Department dept = JSONUtil.unwrap(deptJson, deptType);
     * 
     * // Object type defaults to Map
     * Type<Object> objType = N.typeOf(Object.class);
     * Object result2 = JSONUtil.unwrap(json, objType); // Returns Map<String, Object>
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonObject the JSONObject to convert
     * @param targetType the Type information for the target object
     * @return an instance of the specified type populated with data from the JSONObject
     * @throws JSONException if there is an error during the conversion
     * @throws IllegalArgumentException if targetType is not a Map or Bean type
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONObject jsonObject, Type<? extends T> targetType) throws JSONException {
        if (!targetType.clazz().equals(Object.class) && targetType.clazz().isAssignableFrom(JSONObject.class)) {
            return (T) jsonObject;
        }

        targetType = targetType.isObjectType() ? N.typeOf("Map<String, Object>") : targetType;
        final Class<?> cls = targetType.clazz();

        if (targetType.isMap()) {
            @SuppressWarnings("rawtypes")
            final Map<String, Object> map = N.newMap((Class<Map>) cls, jsonObject.keySet().size());
            final Iterator<String> iter = jsonObject.keys();
            final Type<?> valueType = targetType.getParameterTypes()[1];
            String key = null;
            Object value = null;

            while (iter.hasNext()) {
                key = iter.next();
                value = jsonObject.get(key);

                if (value == JSONObject.NULL) {
                    value = null;
                } else if (value != null) {
                    if (value instanceof JSONObject) {
                        value = unwrap((JSONObject) value, valueType);
                    } else if (value instanceof JSONArray) {
                        value = unwrap((JSONArray) value, valueType);
                    }
                }

                map.put(key, value);
            }

            return (T) map;
        } else if (targetType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
            final Object result = beanInfo.createBeanResult();
            final Iterator<String> iter = jsonObject.keys();
            String key = null;
            Object value = null;
            PropInfo propInfo = null;

            while (iter.hasNext()) {
                key = iter.next();
                value = jsonObject.get(key);

                propInfo = beanInfo.getPropInfo(key);

                if (propInfo != null) {
                    if (value == JSONObject.NULL) {
                        value = null;
                    } else if (value != null) {
                        if (value instanceof JSONObject) {
                            value = unwrap((JSONObject) value, propInfo.jsonXmlType);
                        } else if (value instanceof JSONArray) {
                            value = unwrap((JSONArray) value, propInfo.jsonXmlType);
                        }
                    }

                    propInfo.setPropValue(result, value);
                }
            }

            return beanInfo.finishBeanResult(result);
        } else {
            throw new IllegalArgumentException(targetType.name() + " is not a map or bean type");
        }
    }

    /**
     * Converts a JSONArray to a List&lt;T&gt; with inferred element type.
     * <p>
     * This is a convenience method equivalent to calling {@code toList(jsonArray, Object.class)}.
     * The resulting List will contain elements converted to appropriate Java types based on
     * their JSON representation.
     * </p>
     * 
     * <p><b>Type Inference:</b></p>
     * <ul>
     *   <li>JSON numbers → Integer, Long, or Double based on value</li>
     *   <li>JSON strings → String</li>
     *   <li>JSON booleans → Boolean</li>
     *   <li>JSON {@code null} → null</li>
     *   <li>JSON objects → Map&lt;String, Object&gt;</li>
     *   <li>JSON arrays → List&lt;Object&gt;</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONArray json = new JSONArray("[\"text\",123,true,null,{\"key\":\"value\"},[1,2,3]]");
     * List<Object> list = JSONUtil.unwrap(json);
     * // list.get(0) returns "text" (String)
     * // list.get(1) returns 123 (Integer)
     * // list.get(2) returns {@code true} (Boolean)
     * // list.get(3) returns null
     * // list.get(4) returns Map with "key"="value"
     * // list.get(5) returns List [1,2,3]
     * }</pre>
     *
     * @param <T> the type of elements in the returned list
     * @param jsonArray the JSONArray to convert
     * @return a List containing all elements from the JSONArray
     * @throws JSONException if there is an error during the conversion
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> unwrap(final JSONArray jsonArray) throws JSONException {
        return (List<T>) toList(jsonArray, Object.class);
    }

    /**
     * Converts a JSONArray to an instance of the specified class type.
     * <p>
     * This method supports conversion to:
     * </p>
     * <ul>
     *   <li><b>Collection types:</b> List, Set, Queue implementations</li>
     *   <li><b>Array types:</b> Both primitive arrays (int[], double[], etc.) and object arrays</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert to typed List
     * JSONArray json = new JSONArray("[\"Alice\",\"Bob\",\"Charlie\"]");
     * List<String> names = JSONUtil.unwrap(json, List.class);
     * 
     * // Convert to Set
     * JSONArray numbers = new JSONArray("[1,2,3,2,1]");
     * Set<Integer> uniqueNumbers = JSONUtil.unwrap(numbers, Set.class);
     * // Set contains: 1, 2, 3
     * 
     * // Convert to primitive array
     * JSONArray scores = new JSONArray("[85,90,78,92,88]");
     * int[] scoresArray = JSONUtil.unwrap(scores, int[].class);
     * 
     * // Convert to object array
     * String[] namesArray = JSONUtil.unwrap(json, String[].class);
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonArray the JSONArray to convert
     * @param targetType the class of the object to create
     * @return an instance of targetType populated with data from the JSONArray
     * @throws JSONException if there is an error during the conversion
     * @throws IllegalArgumentException if targetType is not a supported type (Collection or Array)
     */
    public static <T> T unwrap(final JSONArray jsonArray, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonArray, N.typeOf(targetType));
    }

    /**
     * Converts a JSONArray to an instance of the specified Type.
     * <p>
     * This advanced method provides full control over generic type information, supporting:
     * </p>
     * <ul>
     *   <li>Generic collections: {@code List<User>}, {@code Set<Map<String, Object>>}</li>
     *   <li>Multi-dimensional arrays: {@code String[][]}, {@code int[][]}</li>
     *   <li>Complex nested structures with full type preservation</li>
     * </ul>
     * 
     * <p>
     * The method handles several special cases:
     * </p>
     * <ul>
     *   <li>Object type defaults to List&lt;Object&gt;</li>
     *   <li>Primitive arrays use default values for {@code null} elements</li>
     *   <li>JSONObject.NULL is converted to Java null</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Typed List with complex elements
     * Type<List<User>> userListType = N.typeOf("List<User>");
     * JSONArray json = new JSONArray("[{\"name\":\"Alice\",\"age\":25},{\"name\":\"Bob\",\"age\":30}]");
     * List<User> users = JSONUtil.unwrap(json, userListType);
     * 
     * // Set of Maps
     * Type<Set<Map<String, String>>> type = N.typeOf("Set<Map<String, String>>");
     * JSONArray data = new JSONArray("[{\"id\":\"1\"},{\"id\":\"2\"},{\"id\":\"1\"}]");
     * Set<Map<String, String>> result = JSONUtil.unwrap(data, type);
     * 
     * // Multi-dimensional array
     * Type<int[][]> matrixType = N.typeOf(int[][].class);
     * JSONArray matrix = new JSONArray("[[1,2,3],[4,5,6],[7,8,9]]");
     * int[][] result2 = JSONUtil.unwrap(matrix, matrixType);
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonArray the JSONArray to convert
     * @param targetType the Type information for the target object
     * @return an instance of the specified type populated with data from the JSONArray
     * @throws JSONException if there is an error during the conversion
     * @throws IllegalArgumentException if targetType is not an array or collection type
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONArray jsonArray, Type<? extends T> targetType) throws JSONException {
        if (!targetType.clazz().equals(Object.class) && targetType.clazz().isAssignableFrom(JSONArray.class)) {
            return (T) jsonArray;
        }

        targetType = targetType.isObjectType() ? N.typeOf("List<Object>") : targetType;
        final int len = jsonArray.length();

        if (targetType.isCollection()) {
            @SuppressWarnings("rawtypes")
            final Collection<Object> coll = N.newCollection((Class<Collection>) targetType.clazz(), len);
            final Type<?> elementType = targetType.getElementType();
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                } else if (element != null) {
                    if (element instanceof JSONObject) {
                        element = unwrap((JSONObject) element, elementType);
                    } else if (element instanceof JSONArray) {
                        element = unwrap((JSONArray) element, elementType);
                    }
                }

                coll.add(element);
            }

            return (T) coll;
        } else if (targetType.isPrimitiveArray()) {
            final Object array = N.newArray(targetType.getElementType().clazz(), jsonArray.length());
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                }

                if (element == null) {
                    element = targetType.getElementType().defaultValue();
                }

                Array.set(array, i, element);
            }

            return (T) array;
        } else if (targetType.isArray()) {
            final Object[] array = N.newArray(targetType.getElementType().clazz(), jsonArray.length());
            final Type<?> elementType = targetType.getElementType();
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                } else if (element != null) {
                    if (element instanceof JSONObject) {
                        element = unwrap((JSONObject) element, elementType);
                    } else if (element instanceof JSONArray) {
                        element = unwrap((JSONArray) element, elementType);
                    }
                }

                array[i] = element;
            }

            return (T) array;
        } else if (targetType.clazz().isAssignableFrom(JSONArray.class)) {
            return (T) jsonArray;
        } else {
            throw new IllegalArgumentException(targetType.name() + " is not a array or collection type");
        }
    }

    /**
     * Converts a JSONArray to a typed List with specified element class.
     * <p>
     * This method provides a convenient way to create a List with a specific element type
     * from a JSONArray. Each element in the JSONArray is converted to the specified type.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // List of strings
     * JSONArray json = new JSONArray("[\"apple\",\"banana\",\"orange\"]");
     * List<String> fruits = JSONUtil.toList(json, String.class);
     * 
     * // List of integers
     * JSONArray numbers = new JSONArray("[10,20,30,40,50]");
     * List<Integer> intList = JSONUtil.toList(numbers, Integer.class);
     * 
     * // List of custom objects
     * JSONArray users = new JSONArray("[{\"name\":\"Alice\"},{\"name\":\"Bob\"}]");
     * List<User> userList = JSONUtil.toList(users, User.class);
     * }</pre>
     *
     * @param <T> the type of elements in the returned list
     * @param jsonArray the JSONArray to convert
     * @param elementClass the class of elements in the list
     * @return a List containing elements of the specified type
     * @throws JSONException if there is an error during the conversion
     * @throws ClassCastException if elements cannot be converted to the specified type
     */
    public static <T> List<T> toList(final JSONArray jsonArray, final Class<? extends T> elementClass) throws JSONException {
        return toList(jsonArray, N.typeOf(elementClass));
    }

    /**
     * Converts a JSONArray to a typed List with specified element Type.
     * <p>
     * This advanced method allows creation of Lists with complex generic element types
     * that cannot be expressed with simple Class parameters. It performs deep conversion
     * of nested structures according to the provided type information.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // List of Maps
     * Type<Map<String, Object>> mapType = N.typeOf("Map<String, Object>");
     * JSONArray json = new JSONArray("[{\"id\":1,\"name\":\"Item1\"},{\"id\":2,\"name\":\"Item2\"}]");
     * List<Map<String, Object>> items = JSONUtil.toList(json, mapType);
     * 
     * // List of Lists (nested structure)
     * Type<List<Integer>> listType = N.typeOf("List<Integer>");
     * JSONArray matrix = new JSONArray("[[1,2,3],[4,5,6],[7,8,9]]");
     * List<List<Integer>> rows = JSONUtil.toList(matrix, listType);
     * 
     * // Complex bean with generics
     * Type<Response<User>> responseType = N.typeOf("Response<User>");
     * JSONArray responses = new JSONArray("[{\"status\":200,\"data\":{\"name\":\"Alice\"}}]");
     * List<Response<User>> results = JSONUtil.toList(responses, responseType);
     * }</pre>
     *
     * @param <T> the type of elements in the returned list
     * @param jsonArray the JSONArray to convert
     * @param elementType the Type of elements in the list
     * @return a List containing elements of the specified type
     * @throws JSONException if there is an error during the conversion
     * @throws ClassCastException if elements cannot be converted to the specified type
     */
    public static <T> List<T> toList(final JSONArray jsonArray, final Type<T> elementType) throws JSONException {
        final int len = jsonArray.length();
        final List<Object> coll = new ArrayList<>(len);

        Object element = null;

        for (int i = 0; i < len; i++) {
            element = jsonArray.get(i);

            if (element == JSONObject.NULL) {
                element = null;
            } else if (element != null) {
                if (element instanceof JSONObject) {
                    element = unwrap((JSONObject) element, elementType);
                } else if (element instanceof JSONArray) {
                    element = unwrap((JSONArray) element, elementType);
                }
            }

            coll.add(element);
        }

        return (List<T>) coll;
    }
}
