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
 * A utility class providing comprehensive JSON manipulation capabilities.
 * <p>
 * The JSONUtil class serves as a bridge between Java objects and JSON representations,
 * offering bidirectional conversion between various Java data structures and their JSON equivalents.
 * It leverages the org.json library for JSON processing while providing a more intuitive API
 * for common conversion scenarios.
 * </p>
 * 
 * <h2>Key Features:</h2>
 * <ul>
 *   <li><b>Object Wrapping:</b> Convert Java objects (Maps, Beans, Arrays, Collections) to JSON format</li>
 *   <li><b>JSON Unwrapping:</b> Convert JSON structures back to Java objects with type safety</li>
 *   <li><b>Primitive Array Support:</b> Handle all Java primitive array types</li>
 *   <li><b>Deep Conversion:</b> Automatically handle nested structures and complex object graphs</li>
 *   <li><b>Type Preservation:</b> Maintain type information during conversion processes</li>
 * </ul>
 * 
 * <h2>Supported Conversions:</h2>
 * <table border="1">
 *   <tr><th>Java Type</th><th>JSON Type</th><th>Method</th></tr>
 *   <tr><td>Map&lt;String, ?&gt;</td><td>JSONObject</td><td>wrap(Map) / unwrap(JSONObject)</td></tr>
 *   <tr><td>JavaBean</td><td>JSONObject</td><td>wrap(Object) / unwrap(JSONObject, Class)</td></tr>
 *   <tr><td>Collection&lt;?&gt;</td><td>JSONArray</td><td>wrap(Collection) / unwrap(JSONArray)</td></tr>
 *   <tr><td>Arrays (all types)</td><td>JSONArray</td><td>wrap(array) / unwrap(JSONArray, Class)</td></tr>
 * </table>
 * 
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Converting a Map to JSON
 * Map<String, Object> data = new HashMap<>();
 * data.put("name", "John Doe");
 * data.put("age", 30);
 * JSONObject json = JSONUtil.wrap(data);
 * 
 * // Converting a Bean to JSON
 * User user = new User("Jane", 25);
 * JSONObject userJson = JSONUtil.wrap(user);
 * 
 * // Converting JSON back to Java objects
 * Map<String, Object> map = JSONUtil.unwrap(json);
 * User restoredUser = JSONUtil.unwrap(userJson, User.class);
 * 
 * // Working with arrays
 * int[] numbers = {1, 2, 3, 4, 5};
 * JSONArray jsonArray = JSONUtil.wrap(numbers);
 * int[] restoredNumbers = JSONUtil.unwrap(jsonArray, int[].class);
 * }</pre>
 * 
 * <h2>Thread Safety:</h2>
 * <p>
 * All methods in this class are stateless and thread-safe. The class uses a private constructor
 * to prevent instantiation, following the utility class pattern.
 * </p>
 * 
 * <h2>Exception Handling:</h2>
 * <p>
 * Methods may throw {@link JSONException} when JSON parsing or construction fails,
 * and {@link IllegalArgumentException} when type conversions are not supported.
 * </p>
 *
 * @see org.json.JSONObject
 * @see org.json.JSONArray
 * @see com.landawn.abacus.type.Type
 * @since 1.0
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
     * <p><b>Example:</b></p>
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
     * <p><b>Examples:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Examples:</b></p>
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
     * <p><b>Example:</b></p>
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
     * <p><b>Examples:</b></p>
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
     * <p><b>Examples:</b></p>
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
        targetType = targetType.isObjectType() ? N.typeOf("Map<String, Object>") : targetType;
        final Class<?> cls = targetType.clazz();

        if (targetType.clazz().isAssignableFrom(JSONObject.class)) {
            return (T) jsonObject;
        } else if (targetType.isMap()) {
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
     *   <li>JSON null → null</li>
     *   <li>JSON objects → Map&lt;String, Object&gt;</li>
     *   <li>JSON arrays → List&lt;Object&gt;</li>
     * </ul>
     * 
     * <p><b>Example:</b></p>
     * <pre>{@code
     * JSONArray json = new JSONArray("[\"text\",123,true,null,{\"key\":\"value\"},[1,2,3]]");
     * List<Object> list = JSONUtil.unwrap(json);
     * // list.get(0) returns "text" (String)
     * // list.get(1) returns 123 (Integer)
     * // list.get(2) returns true (Boolean)
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
     * <p><b>Examples:</b></p>
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
     *   <li>Primitive arrays use default values for null elements</li>
     *   <li>JSONObject.NULL is converted to Java null</li>
     * </ul>
     * 
     * <p><b>Examples:</b></p>
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
     * <p><b>Examples:</b></p>
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
        return toList(jsonArray, Type.of(elementClass));
    }

    /**
     * Converts a JSONArray to a typed List with specified element Type.
     * <p>
     * This advanced method allows creation of Lists with complex generic element types
     * that cannot be expressed with simple Class parameters. It performs deep conversion
     * of nested structures according to the provided type information.
     * </p>
     * 
     * <p><b>Examples:</b></p>
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