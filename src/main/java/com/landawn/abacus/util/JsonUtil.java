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
 * Utility class providing static methods for bidirectional conversion between Java objects and
 * JSON representations using the org.json library.
 *
 * <p>Supports conversion of {@link Map} and JavaBean objects to {@link JSONObject}, and of
 * collections, arrays (both primitive and object), and {@link Collection} instances to
 * {@link JSONArray}. The corresponding {@code unwrap} methods reconstruct Java objects from
 * {@link JSONObject} and {@link JSONArray} inputs.
 *
 * <p><b>Supported Java-to-JSON type mappings:</b>
 * <ul>
 *   <li>{@code Map<String, ?>} &rarr; {@link JSONObject} (via {@link #wrap(Map)})</li>
 *   <li>JavaBean / {@link Map} &rarr; {@link JSONObject} (via {@link #wrap(Object)})</li>
 *   <li>{@link Collection} &rarr; {@link JSONArray} (via {@link #wrap(Collection)})</li>
 *   <li>{@code Object[]} &rarr; {@link JSONArray} (via {@link #wrap(Object[])})</li>
 *   <li>Primitive arrays ({@code int[]}, {@code double[]}, {@code boolean[]}, etc.) &rarr; {@link JSONArray}</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> All methods are static with no shared mutable state; this class is
 * safe for concurrent use.
 *
 * <p><b>Exception handling:</b> Methods throw {@link JSONException} when org.json encounters an
 * error, and {@link IllegalArgumentException} when an unsupported target type is requested.
 *
 * <p><b>Circular references:</b> Object graphs containing cycles will cause infinite recursion
 * during wrapping. Callers must ensure the object graph is acyclic.
 *
 * <p><b>Usage examples:</b>
 * <pre>{@code
 * // Map to JSONObject and back
 * Map<String, Object> userMap = new HashMap<>();
 * userMap.put("name", "John");
 * userMap.put("age", 30);
 * JSONObject jsonUser = JsonUtil.wrap(userMap);
 * Map<String, Object> restored = JsonUtil.unwrap(jsonUser);
 *
 * // Primitive array round-trip
 * int[] numbers = {1, 2, 3};
 * JSONArray arr = JsonUtil.wrap(numbers);
 * int[] restoredNumbers = JsonUtil.unwrap(arr, int[].class);
 *
 * // JavaBean round-trip
 * JSONObject beanJson = JsonUtil.wrap(myBean);
 * MyBean restoredBean = JsonUtil.unwrap(beanJson, MyBean.class);
 * }</pre>
 *
 * @see org.json.JSONObject
 * @see org.json.JSONArray
 * @see org.json.JSONException
 * @see com.landawn.abacus.parser.ParserUtil
 * @see com.landawn.abacus.type.Type
 */
public final class JsonUtil {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private JsonUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Converts a {@link Map} into a {@link JSONObject}.
     *
     * <p>Creates a new {@link JSONObject} from the provided map. Values may be any type
     * accepted by the {@link JSONObject} constructor, including primitives, {@link String},
     * {@link Collection}, other {@link Map}, {@link JSONObject}, and {@link JSONArray} instances.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * Map<String, Object> map = new HashMap<>();
     * map.put("name", "Alice");
     * map.put("score", 95.5);
     * map.put("active", true);
     * JSONObject json = JsonUtil.wrap(map);
     * // Result: {"name":"Alice","score":95.5,"active":true}
     * }</pre>
     *
     * @param map the map to convert; keys must be {@link String}s
     * @return a new {@link JSONObject} containing all key-value pairs from the input map
     * @throws JSONException if any value in the map cannot be represented as a valid JSON type
     */
    public static JSONObject wrap(final Map<String, ?> map) {
        return new JSONObject(map);
    }

    /**
     * Converts a Java object (JavaBean or {@link Map}) into a {@link JSONObject}.
     *
     * <p>If {@code bean} is a {@link Map} instance, it is passed directly to the
     * {@link JSONObject} constructor. Otherwise, the object is first converted to a
     * {@link Map} via {@code Beans.deepBeanToMap(bean, true)}, which recursively
     * resolves nested bean properties, collections, and maps. The second argument
     * ({@code ignoreNullProperty == true}) means properties whose value is {@code null}
     * are omitted from the resulting {@link JSONObject}.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * Person person = new Person("John", 30, new Address("123 Main St"));
     * JSONObject json = JsonUtil.wrap(person);
     * // Result: {"name":"John","age":30,"address":{"street":"123 Main St"}}
     *
     * Map<String, Object> map = new HashMap<>();
     * map.put("id", 123);
     * JSONObject json2 = JsonUtil.wrap((Object) map);
     * }</pre>
     *
     * @param bean the object to convert; may be a {@link Map} or any JavaBean with accessible properties
     * @return a new {@link JSONObject} representing the input object
     * @throws JSONException if any property value cannot be converted to a valid JSON type
     */
    @SuppressWarnings("unchecked")
    public static JSONObject wrap(final Object bean) {
        return new JSONObject(bean instanceof Map ? (Map<String, Object>) bean : Beans.deepBeanToMap(bean, true));
    }

    /**
     * Converts a {@code boolean} array into a {@link JSONArray}.
     *
     * <p>Each element is appended to the array in iteration order.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * boolean[] flags = {true, false, true, true, false};
     * JSONArray json = JsonUtil.wrap(flags);
     * // Result: [true,false,true,true,false]
     * }</pre>
     *
     * @param array the boolean array to convert
     * @return a new {@link JSONArray} containing all elements from the input array
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final boolean[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a {@code char} array into a {@link JSONArray}.
     *
     * <p>Each {@code char} element is boxed to a {@link Character} and stored in the
     * {@link JSONArray}. JSON has no native character type; when the array is serialized,
     * each character is rendered as a single-character JSON string (e.g., {@code ["H","e","l","l","o"]}).
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * char[] chars = {'H', 'e', 'l', 'l', 'o'};
     * JSONArray json = JsonUtil.wrap(chars);
     * // Serializes as: ["H","e","l","l","o"]
     * }</pre>
     *
     * @param array the character array to convert
     * @return a new {@link JSONArray} containing the characters from the input array
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final char[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a {@code byte} array into a {@link JSONArray}.
     *
     * <p>Each byte value is stored as a JSON number.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * byte[] bytes = {10, 20, 30, 40, 50};
     * JSONArray json = JsonUtil.wrap(bytes);
     * // Result: [10,20,30,40,50]
     * }</pre>
     *
     * @param array the byte array to convert
     * @return a new {@link JSONArray} containing all byte values from the input array as numbers
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final byte[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a {@code short} array into a {@link JSONArray}.
     *
     * <p>Each short value is stored as a JSON number.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * short[] shorts = {100, 200, 300, 400, 500};
     * JSONArray json = JsonUtil.wrap(shorts);
     * // Result: [100,200,300,400,500]
     * }</pre>
     *
     * @param array the short array to convert
     * @return a new {@link JSONArray} containing all short values from the input array as numbers
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final short[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts an {@code int} array into a {@link JSONArray}.
     *
     * <p>Each integer value is stored as a JSON number.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * int[] numbers = {1, 2, 3, 4, 5};
     * JSONArray json = JsonUtil.wrap(numbers);
     * // Result: [1,2,3,4,5]
     * }</pre>
     *
     * @param array the integer array to convert
     * @return a new {@link JSONArray} containing all integer values from the input array
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final int[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a {@code long} array into a {@link JSONArray}.
     *
     * <p>Each long value is stored as a JSON number. Note that JSON consumers implemented in
     * JavaScript may lose precision for values outside the safe integer range
     * ({@code -(2^53 - 1)} to {@code 2^53 - 1}).
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * long[] timestamps = {1609459200000L, 1609545600000L, 1609632000000L};
     * JSONArray json = JsonUtil.wrap(timestamps);
     * // Result: [1609459200000,1609545600000,1609632000000]
     * }</pre>
     *
     * @param array the long array to convert
     * @return a new {@link JSONArray} containing all long values from the input array
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final long[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a {@code float} array into a {@link JSONArray}.
     *
     * <p>Each float value is stored as a JSON number. The standard JSON specification does not
     * support {@code NaN} or {@code Infinity}; the org.json library may throw a
     * {@link JSONException} for such values depending on its configuration.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * float[] measurements = {98.6f, 99.1f, 97.8f, 98.2f};
     * JSONArray json = JsonUtil.wrap(measurements);
     * // Result: [98.6,99.1,97.8,98.2]
     * }</pre>
     *
     * @param array the float array to convert
     * @return a new {@link JSONArray} containing all float values from the input array
     * @throws JSONException if an error occurs during conversion, or if the array contains
     *         {@code NaN} or {@code Infinity}
     */
    public static JSONArray wrap(final float[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a {@code double} array into a {@link JSONArray}.
     *
     * <p>Each double value is stored as a JSON number. The standard JSON specification does not
     * support {@code NaN} or {@code Infinity}; the org.json library may throw a
     * {@link JSONException} for such values depending on its configuration.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * double[] prices = {19.99, 29.99, 39.99, 49.99};
     * JSONArray json = JsonUtil.wrap(prices);
     * // Result: [19.99,29.99,39.99,49.99]
     * }</pre>
     *
     * @param array the double array to convert
     * @return a new {@link JSONArray} containing all double values from the input array
     * @throws JSONException if an error occurs during conversion, or if the array contains
     *         {@code NaN} or {@code Infinity}
     */
    public static JSONArray wrap(final double[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts an {@link Object} array into a {@link JSONArray}.
     *
     * <p>Each element is wrapped by the org.json library according to its type:
     * <ul>
     *   <li>Numbers, booleans, strings: stored directly as JSON primitives</li>
     *   <li>{@code null}: stored as {@link JSONObject#NULL}</li>
     *   <li>{@link Map} and JavaBean instances: converted to {@link JSONObject}</li>
     *   <li>{@link Collection} and array instances: converted to {@link JSONArray}</li>
     * </ul>
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * Object[] mixed = {"text", 123, true, null,
     *     new HashMap<String, Object>() {{ put("key", "value"); }},
     *     Arrays.asList(1, 2, 3)};
     * JSONArray json = JsonUtil.wrap(mixed);
     * // Result: ["text",123,true,null,{"key":"value"},[1,2,3]]
     * }</pre>
     *
     * @param array the object array to convert
     * @return a new {@link JSONArray} containing all elements from the input array
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final Object[] array) throws JSONException {
        return new JSONArray(array);
    }

    /**
     * Converts a {@link Collection} into a {@link JSONArray}.
     *
     * <p>Accepts any {@link Collection} implementation ({@link java.util.List},
     * {@link java.util.Set}, {@link java.util.Queue}, etc.). Elements are stored in
     * iteration order. Each element is wrapped by the org.json library the same way as in
     * {@link #wrap(Object[])}.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
     * JSONArray json = JsonUtil.wrap(names);
     * // Result: ["Alice","Bob","Charlie"]
     * }</pre>
     *
     * @param coll the collection to convert
     * @return a new {@link JSONArray} containing all elements from the collection in iteration order
     * @throws JSONException if an error occurs during conversion
     */
    public static JSONArray wrap(final Collection<?> coll) {
        return new JSONArray(coll);
    }

    /**
     * Converts a {@link JSONObject} to a {@code Map<String, Object>}.
     *
     * <p>Convenience overload equivalent to {@link #unwrap(JSONObject, Class) unwrap(jsonObject, Map.class)}.
     * Values in the returned map are converted to the most appropriate Java types
     * (e.g., JSON numbers to {@link Integer}/{@link Long}/{@link Double}, JSON booleans to
     * {@link Boolean}, nested JSON objects to {@code Map<String, Object>},
     * nested JSON arrays to {@code List<Object>}).
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * JSONObject json = new JSONObject("{\"name\":\"John\",\"age\":30,\"active\":true}");
     * Map<String, Object> map = JsonUtil.unwrap(json);
     * // map.get("name") -> "John", map.get("age") -> 30, map.get("active") -> true
     * }</pre>
     *
     * @param jsonObject the {@link JSONObject} to convert
     * @return a {@code Map<String, Object>} containing all key-value pairs from the {@link JSONObject}
     * @throws JSONException if an error occurs during conversion
     * @see #unwrap(JSONObject, Class)
     * @see #unwrap(JSONObject, Type)
     */
    public static Map<String, Object> unwrap(final JSONObject jsonObject) throws JSONException {
        return unwrap(jsonObject, Map.class);
    }

    /**
     * Converts a {@link JSONObject} to an instance of the specified class.
     *
     * <p>Delegates to {@link #unwrap(JSONObject, Type)}. Supports:
     * <ul>
     *   <li>{@link Map} implementations ({@link java.util.HashMap},
     *       {@link java.util.LinkedHashMap}, {@link java.util.TreeMap}, etc.)</li>
     *   <li>JavaBean classes with accessible properties</li>
     * </ul>
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * JSONObject userJson = new JSONObject("{\"name\":\"Alice\",\"age\":25}");
     * User user = JsonUtil.unwrap(userJson, User.class);
     * // user.getName() -> "Alice", user.getAge() -> 25
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonObject the {@link JSONObject} to convert
     * @param targetType the class of the object to create
     * @return an instance of {@code targetType} populated with data from the {@link JSONObject}
     * @throws JSONException if an error occurs during conversion
     * @throws IllegalArgumentException if {@code targetType} is not a {@link Map} or bean type
     */
    public static <T> T unwrap(final JSONObject jsonObject, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonObject, Type.of(targetType));
    }

    /**
     * Converts a {@link JSONObject} to an instance of the specified {@link Type}.
     *
     * <p>Supports complex generic types that cannot be expressed with a simple {@link Class}:
     * <ul>
     *   <li>Generic {@link Map} types, e.g. {@code Map<String, List<String>>}</li>
     *   <li>JavaBean types, including those with collection/bean properties</li>
     * </ul>
     *
     * <p>Special cases:
     * <ul>
     *   <li>If {@code targetType} represents {@code Object}, it is treated as
     *       {@code Map<String, Object>}.</li>
     *   <li>If {@code targetType} is assignable from {@link JSONObject} (and is not
     *       {@code Object}), the {@code jsonObject} argument is returned as-is.</li>
     * </ul>
     *
     * <p>Conversion is recursive: nested {@link JSONObject} and {@link JSONArray} values are
     * converted according to the value type or property type declared in {@code targetType}.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * Type<Map<String, List<String>>> type = Type.of("Map<String, List<String>>");
     * JSONObject json = new JSONObject("{\"tags\":[\"java\",\"json\"]}");
     * Map<String, List<String>> result = JsonUtil.unwrap(json, type);
     * // result.get("tags") -> ["java", "json"]
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonObject the {@link JSONObject} to convert
     * @param targetType the {@link Type} describing the target object
     * @return an instance of the type described by {@code targetType}, populated with data
     *         from the {@link JSONObject}
     * @throws JSONException if an error occurs during conversion
     * @throws IllegalArgumentException if {@code targetType} is not a {@link Map} or bean type
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONObject jsonObject, Type<? extends T> targetType) throws JSONException {
        if (!targetType.javaType().equals(Object.class) && targetType.javaType().isAssignableFrom(JSONObject.class)) {
            return (T) jsonObject;
        }

        targetType = targetType.isObject() ? Type.of("Map<String, Object>") : targetType;
        final Class<?> cls = targetType.javaType();

        if (targetType.isMap()) {
            @SuppressWarnings("rawtypes")
            final Map<String, Object> map = N.newMap((Class<Map>) cls, jsonObject.keySet().size());
            final Iterator<String> iter = jsonObject.keys();
            final Type<?> valueType = targetType.parameterTypes().get(1);
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
                    } else if (!valueType.javaType().isAssignableFrom(value.getClass())) {
                        value = valueType.valueOf(value);
                    }
                }

                map.put(key, value);
            }

            return (T) map;
        } else if (targetType.isBean()) {
            final BeanInfo beanInfo = ParserUtil.getBeanInfo(targetType.reflectType());
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
     * Converts a {@link JSONArray} to a {@code List<T>} with inferred element types.
     *
     * <p>Convenience overload equivalent to {@link #toList(JSONArray, Class) toList(jsonArray, Object.class)}.
     * JSON values are mapped to Java types as follows:
     * <ul>
     *   <li>JSON numbers &rarr; {@link Integer}, {@link Long}, or {@link Double} depending on value</li>
     *   <li>JSON strings &rarr; {@link String}</li>
     *   <li>JSON booleans &rarr; {@link Boolean}</li>
     *   <li>JSON {@code null} &rarr; {@code null}</li>
     *   <li>JSON objects &rarr; {@code Map<String, Object>}</li>
     *   <li>JSON arrays &rarr; {@code List<Object>}</li>
     * </ul>
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * JSONArray json = new JSONArray("[\"text\",123,true,null]");
     * List<Object> list = JsonUtil.unwrap(json);
     * // list -> ["text", 123, true, null]
     * }</pre>
     *
     * @param <T> the unchecked element type of the returned list
     * @param jsonArray the {@link JSONArray} to convert
     * @return a {@link java.util.List} containing all elements from the {@link JSONArray}
     * @throws JSONException if an error occurs during conversion
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> unwrap(final JSONArray jsonArray) throws JSONException {
        return (List<T>) toList(jsonArray, Object.class);
    }

    /**
     * Converts a {@link JSONArray} to an instance of the specified class.
     *
     * <p>Delegates to {@link #unwrap(JSONArray, Type)}. Supports:
     * <ul>
     *   <li>{@link java.util.Collection} implementations ({@link java.util.List},
     *       {@link java.util.Set}, etc.)</li>
     *   <li>Primitive arrays ({@code int[]}, {@code double[]}, etc.) and object arrays</li>
     * </ul>
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * JSONArray scores = new JSONArray("[85,90,78,92]");
     * int[] arr = JsonUtil.unwrap(scores, int[].class);
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonArray the {@link JSONArray} to convert
     * @param targetType the class of the object to create
     * @return an instance of {@code targetType} populated with data from the {@link JSONArray}
     * @throws JSONException if an error occurs during conversion
     * @throws IllegalArgumentException if {@code targetType} is not a collection or array type
     */
    public static <T> T unwrap(final JSONArray jsonArray, final Class<? extends T> targetType) throws JSONException {
        return unwrap(jsonArray, Type.of(targetType));
    }

    /**
     * Converts a {@link JSONArray} to an instance of the specified {@link Type}.
     *
     * <p>Supports generic types that cannot be expressed with a plain {@link Class}:
     * <ul>
     *   <li>Generic collections: {@code List<User>}, {@code Set<Map<String, Object>>}</li>
     *   <li>Multi-dimensional arrays: {@code String[][]}, {@code int[][]}</li>
     * </ul>
     *
     * <p>Special cases:
     * <ul>
     *   <li>If {@code targetType} represents {@code Object}, it is treated as
     *       {@code List<Object>}.</li>
     *   <li>If {@code targetType} is assignable from {@link JSONArray} (and is not
     *       {@code Object}), the {@code jsonArray} argument is returned as-is.</li>
     *   <li>For primitive array targets, {@link JSONObject#NULL} elements are replaced by
     *       the element type's default value (e.g., {@code 0} for {@code int}).</li>
     * </ul>
     *
     * <p>Conversion is recursive: nested {@link JSONObject} and {@link JSONArray} elements are
     * converted according to the element type declared in {@code targetType}.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * Type<List<User>> userListType = Type.of("List<User>");
     * JSONArray json = new JSONArray("[{\"name\":\"Alice\",\"age\":25}]");
     * List<User> users = JsonUtil.unwrap(json, userListType);
     * }</pre>
     *
     * @param <T> the type of object to return
     * @param jsonArray the {@link JSONArray} to convert
     * @param targetType the {@link Type} describing the target object
     * @return an instance of the type described by {@code targetType}, populated with data
     *         from the {@link JSONArray}
     * @throws JSONException if an error occurs during conversion
     * @throws IllegalArgumentException if {@code targetType} is not an array or collection type
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONArray jsonArray, Type<? extends T> targetType) throws JSONException {
        if (!targetType.javaType().equals(Object.class) && targetType.javaType().isAssignableFrom(JSONArray.class)) {
            return (T) jsonArray;
        }

        targetType = targetType.isObject() ? Type.of("List<Object>") : targetType;
        final int len = jsonArray.length();

        if (targetType.isCollection()) {
            @SuppressWarnings("rawtypes")
            final Collection<Object> coll = N.newCollection((Class<Collection>) targetType.javaType(), len);
            final Type<?> elementType = targetType.elementType();
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
            final Type<?> elementType = targetType.elementType();
            final Object array = N.newArray(elementType.javaType(), jsonArray.length());
            Object element = null;

            for (int i = 0; i < len; i++) {
                element = jsonArray.get(i);

                if (element == JSONObject.NULL) {
                    element = null;
                }

                if (element == null) {
                    element = elementType.defaultValue();
                } else {
                    element = elementType.valueOf(element);
                }

                Array.set(array, i, element);
            }

            return (T) array;
        } else if (targetType.isArray()) {
            final Object[] array = N.newArray(targetType.elementType().javaType(), jsonArray.length());
            final Type<?> elementType = targetType.elementType();
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
                    } else if (!elementType.javaType().isAssignableFrom(element.getClass())) {
                        element = elementType.valueOf(element);
                    }
                }

                array[i] = element;
            }

            return (T) array;
        } else if (targetType.javaType().isAssignableFrom(JSONArray.class)) {
            return (T) jsonArray;
        } else {
            throw new IllegalArgumentException(targetType.name() + " is not a array or collection type");
        }
    }

    /**
     * Converts a {@link JSONArray} to a typed {@link java.util.List} with the specified element class.
     *
     * <p>Delegates to {@link #toList(JSONArray, Type)}. Each element in the {@link JSONArray} is
     * converted to an instance of {@code elementClass} where applicable (e.g., nested
     * {@link JSONObject} elements are converted to the target type via
     * {@link #unwrap(JSONObject, Type)}).
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * JSONArray users = new JSONArray("[{\"name\":\"Alice\"},{\"name\":\"Bob\"}]");
     * List<User> userList = JsonUtil.toList(users, User.class);
     * }</pre>
     *
     * @param <T> the type of elements in the returned list
     * @param jsonArray the {@link JSONArray} to convert
     * @param elementClass the class of elements in the list
     * @return a {@link java.util.List} containing elements converted to {@code elementClass}
     * @throws JSONException if an error occurs during conversion
     */
    public static <T> List<T> toList(final JSONArray jsonArray, final Class<? extends T> elementClass) throws JSONException {
        return toList(jsonArray, Type.of(elementClass));
    }

    /**
     * Converts a {@link JSONArray} to a typed {@link java.util.List} with the specified element {@link Type}.
     *
     * <p>Supports complex generic element types that cannot be expressed with a plain {@link Class}.
     * Nested {@link JSONObject} and {@link JSONArray} elements are recursively converted according to
     * {@code elementType}. Scalar elements are converted through {@code elementType} when they are not
     * already assignable to its Java type. {@link JSONObject#NULL} is converted to {@code null}.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * Type<Map<String, Object>> mapType = Type.of("Map<String, Object>");
     * JSONArray json = new JSONArray("[{\"id\":1,\"name\":\"Item1\"},{\"id\":2,\"name\":\"Item2\"}]");
     * List<Map<String, Object>> items = JsonUtil.toList(json, mapType);
     * }</pre>
     *
     * @param <T> the type of elements in the returned list
     * @param jsonArray the {@link JSONArray} to convert
     * @param elementType the {@link Type} of each element
     * @return a {@link java.util.List} containing elements converted to the specified type
     * @throws JSONException if an error occurs during conversion
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
                } else if (!elementType.javaType().isAssignableFrom(element.getClass())) {
                    element = elementType.valueOf(element);
                }
            }

            coll.add(element);
        }

        return (List<T>) coll;
    }
}
