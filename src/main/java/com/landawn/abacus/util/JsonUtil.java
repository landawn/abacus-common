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
 * arrays (both primitive and object) and {@link Collection} instances to
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
 * <p><b>{@code null} inputs are not treated uniformly across the {@code wrap} overloads.</b>
 * {@link #wrap(Map)}, {@link #wrap(Object)} and {@link #wrap(Collection)} accept {@code null} and produce an
 * empty {@link JSONObject} / {@link JSONArray}. The nine array overloads ({@code boolean[]} through
 * {@code double[]}, plus {@link #wrap(Object[])}) reject {@code null} with an {@link IllegalArgumentException}.
 * The {@code unwrap} and {@code toList} methods reject a {@code null} source or target type with an
 * {@link IllegalArgumentException}.
 *
 * <p><b>Key order is undefined.</b> {@link JSONObject} stores its entries in a {@link java.util.HashMap}, so it
 * has no insertion or sorted order to preserve, and the {@code Map} returned by {@link #unwrap(JSONObject)} is
 * likewise unordered. Request an ordered map explicitly - for example
 * {@code unwrap(jsonObject, LinkedHashMap.class)} or {@code unwrap(jsonObject, TreeMap.class)} - if you need a
 * deterministic iteration order; note that {@link java.util.LinkedHashMap} only freezes whatever order the
 * source {@code JSONObject} happened to have.
 *
 * <p><b>Nested JSON-to-Java conversion:</b> Explicit map, collection and array targets are reconstructed
 * recursively, including ordinary Java containers stored inside JSON nodes. Declared nested element
 * types and the JSON null sentinel are honored. Ordinary Java values requested as {@code Object}
 * remain identical and opaque; native JSON nodes are still adapted. Container factories must return
 * fresh, empty, correctly typed storage, independent of every input and earlier output. Converted
 * map-key collisions and cycles actually traversed during conversion raise {@link IllegalArgumentException}.
 * Opaque Object leaves may contain cycles.</p>
 *
 * <p><b>Circular references during Java-to-JSON wrapping:</b> Cyclic object graphs are unsupported, and how the failure surfaces depends
 * on where the cycle is:
 * <ul>
 *   <li>A bean that reaches itself through bean properties only (directly or mutually) is <i>detected</i>:
 *       {@link #wrap(Object)} throws {@link IllegalArgumentException} with the message
 *       "Cyclic bean reference cannot be converted to a map".</li>
 *   <li>A {@link Collection} or {@link Map} that contains itself hits org.json's own guard and throws
 *       {@link JSONException} ("has reached recursion depth limit of 512").</li>
 *   <li>A cycle that runs <i>through</i> a collection or array on its way back to a bean - a bean holding a
 *       {@code List} that contains the bean, or an {@code Object[]} containing itself - is <b>not</b>
 *       detected and overflows the stack with a {@link StackOverflowError}. That is an {@link Error}, not an
 *       exception: a {@code catch (Exception)} around the call will not stop it.</li>
 * </ul>
 * Callers must ensure the object graph is acyclic rather than relying on any of these.
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
     * Java {@code null} values are omitted by the org.json constructor. A {@code null} map is
     * treated as an empty map.
     *
     * @param map the map to convert; may be {@code null}; keys must be non-null {@link String}s
     * @return a new {@link JSONObject} containing the non-null-valued entries from the input map
     * @throws NullPointerException if {@code map} contains a null key
     * @throws JSONException if a value is a non-finite number, or wrapping nested values exceeds the JSON nesting limit or detects a recursive bean property
     */
    public static JSONObject wrap(final Map<String, ?> map) throws NullPointerException, JSONException {
        return new JSONObject(map);
    }

    /**
     * Converts a Java object (JavaBean or {@link Map}) into a {@link JSONObject}.
     *
     * <p>If {@code bean} is a {@link Map} instance, it is passed directly to the
     * {@link JSONObject} constructor. Otherwise, the object is first converted to a
     * {@link Map} via {@code Beans.deepBeanToMap(bean, true)}, which recursively
     * resolves properties declared as nested bean types. Collections, arrays, maps, and other
     * values are then wrapped by the {@link JSONObject} constructor. The second argument
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
     * <p><b>Only {@link Map} and JavaBean arguments are supported.</b> Anything else - a {@link String}, a boxed
     * primitive, an array, or a {@link Collection} - has no bean properties and is rejected with an
     * {@link IllegalArgumentException}. This is easy to hit by accident: a variable whose <i>static</i> type is
     * {@link Object} binds to this overload even when it holds a {@link Collection} or an array, so
     * {@link #wrap(Collection)} / {@link #wrap(Object[])} are not selected. Cast to the intended parameter type
     * when the static type is {@link Object}.
     *
     * @param bean the object to convert; may be {@code null}, a {@link Map}, or any JavaBean with accessible properties
     * @return a new {@link JSONObject} representing the input object; an empty {@link JSONObject} if {@code bean} is {@code null}
     * @throws IllegalArgumentException if {@code bean} is neither {@code null}, a {@link Map}, nor a type with
     *         accessible bean properties
     * @throws NullPointerException if the input is a map containing a null key
     * @throws JSONException if any property value cannot be converted to a valid JSON type - in particular a
     *         {@code NaN} or infinite {@link Double}/{@link Float} property, which the JSON specification does
     *         not support and this class rejects outright.
     * @throws RuntimeException if reading bean metadata or a property fails during conversion
     */
    @SuppressWarnings("unchecked")
    public static JSONObject wrap(final Object bean) throws IllegalArgumentException, NullPointerException, JSONException, RuntimeException {
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
     * @param array the non-null boolean array to convert
     * @return a new {@link JSONArray} containing all elements from the input array
     * @throws IllegalArgumentException if {@code array} is {@code null}
     */
    public static JSONArray wrap(final boolean[] array) throws IllegalArgumentException {
        N.checkArgNotNull(array, cs.array);

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
     * @param array the non-null character array to convert
     * @return a new {@link JSONArray} containing the characters from the input array
     * @throws IllegalArgumentException if {@code array} is {@code null}
     */
    public static JSONArray wrap(final char[] array) throws IllegalArgumentException {
        N.checkArgNotNull(array, cs.array);

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
     * @param array the non-null byte array to convert
     * @return a new {@link JSONArray} containing all byte values from the input array as numbers
     * @throws IllegalArgumentException if {@code array} is {@code null}
     */
    public static JSONArray wrap(final byte[] array) throws IllegalArgumentException {
        N.checkArgNotNull(array, cs.array);

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
     * @param array the non-null short array to convert
     * @return a new {@link JSONArray} containing all short values from the input array as numbers
     * @throws IllegalArgumentException if {@code array} is {@code null}
     */
    public static JSONArray wrap(final short[] array) throws IllegalArgumentException {
        N.checkArgNotNull(array, cs.array);

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
     * @param array the non-null integer array to convert
     * @return a new {@link JSONArray} containing all integer values from the input array
     * @throws IllegalArgumentException if {@code array} is {@code null}
     */
    public static JSONArray wrap(final int[] array) throws IllegalArgumentException {
        N.checkArgNotNull(array, cs.array);

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
     * @param array the non-null long array to convert
     * @return a new {@link JSONArray} containing all long values from the input array
     * @throws IllegalArgumentException if {@code array} is {@code null}
     */
    public static JSONArray wrap(final long[] array) throws IllegalArgumentException {
        N.checkArgNotNull(array, cs.array);

        return new JSONArray(array);
    }

    /**
     * Converts a {@code float} array into a {@link JSONArray}.
     *
     * <p>Each float value is stored as a JSON number. The standard JSON specification does not
     * support {@code NaN} or {@code Infinity}; the org.json constructor throws a
     * {@link JSONException} for such values.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * float[] measurements = {98.6f, 99.1f, 97.8f, 98.2f};
     * JSONArray json = JsonUtil.wrap(measurements);
     * // Result: [98.6,99.1,97.8,98.2]
     * }</pre>
     *
     * @param array the non-null float array to convert
     * @return a new {@link JSONArray} containing all float values from the input array
     * @throws IllegalArgumentException if {@code array} is {@code null}
     * @throws JSONException if an array element is NaN or infinite
     */
    public static JSONArray wrap(final float[] array) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(array, cs.array);

        return new JSONArray(array);
    }

    /**
     * Converts a {@code double} array into a {@link JSONArray}.
     *
     * <p>Each double value is stored as a JSON number. The standard JSON specification does not
     * support {@code NaN} or {@code Infinity}; the org.json constructor throws a
     * {@link JSONException} for such values.
     *
     * <p><b>Usage example:</b>
     * <pre>{@code
     * double[] prices = {19.99, 29.99, 39.99, 49.99};
     * JSONArray json = JsonUtil.wrap(prices);
     * // Result: [19.99,29.99,39.99,49.99]
     * }</pre>
     *
     * @param array the non-null double array to convert
     * @return a new {@link JSONArray} containing all double values from the input array
     * @throws IllegalArgumentException if {@code array} is {@code null}
     * @throws JSONException if an array element is NaN or infinite
     */
    public static JSONArray wrap(final double[] array) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(array, cs.array);

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
     * @param array the non-null object array to convert
     * @return a new {@link JSONArray} containing all elements from the input array
     * @throws IllegalArgumentException if {@code array} is {@code null}
     * @throws JSONException if a value is a non-finite number, or wrapping a nested container or bean exceeds the JSON nesting limit or detects a recursive bean property
     */
    public static JSONArray wrap(final Object[] array) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(array, cs.array);

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
     * A {@code null} collection is treated as empty.
     *
     * @param coll the collection to convert; may be {@code null}
     * @return a new {@link JSONArray} containing all elements from the collection in iteration order
     * @throws JSONException if a value is a non-finite number, or wrapping a nested container or bean exceeds the JSON nesting limit or detects a recursive bean property
     */
    public static JSONArray wrap(final Collection<?> coll) throws JSONException {
        return new JSONArray(coll);
    }

    /**
     * Converts a {@link JSONObject} to a {@code Map<String, Object>}.
     *
     * <p>Convenience overload equivalent to {@link #unwrap(JSONObject, Class) unwrap(jsonObject, Map.class)}.
     * Values in the returned map are converted to the most appropriate Java types
     * (e.g., JSON booleans to {@link Boolean}, nested JSON objects to {@code Map<String, Object>},
     * nested JSON arrays to {@code List<Object>}).
     *
     * <p><b>Numbers.</b> A <i>parsed</i> number literal becomes {@link Integer} or {@link Long} when it is an
     * integer that fits, {@link java.math.BigInteger} when it does not, and {@link java.math.BigDecimal} - not
     * {@link Double} - when it is written in decimal or exponent notation. The exceptions are the literals
     * {@code BigDecimal} itself cannot parse, which org.json falls back to {@code Double} for: a negative literal
     * whose value is zero ({@code -0}, {@code -0.0}, {@code -0e5}) and a negative exponent past
     * {@code BigDecimal}'s {@code int} scale ({@code 1e-2147483648}). A value that was <i>put</i> into the
     * {@link JSONObject} programmatically is returned unchanged, keeping whatever box type the caller used, so a
     * value stored as a Java {@code Double} comes back as a {@code Double} and one stored as a {@link Float} comes
     * back as a {@code Float}, while the same number parsed from JSON text comes back as a {@code BigDecimal}. To get a
     * specific box type, use {@link #unwrap(JSONObject, Type)} with a parameterised map type (for example
     * {@code Type.of("Map<String, Double>")}), or {@link #unwrap(JSONObject, Class)} with a bean class whose
     * property has that type; {@code unwrap(jsonObject, Map.class)} leaves the box types as they are, because the
     * value type of a raw {@code Map} is {@code Object}.
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
     * @throws IllegalArgumentException if {@code jsonObject} is null, a traversed JSON-container conversion is cyclic, or a container factory fails to supply a fresh empty instance of the requested type.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if a container factory or an operation on a resulting container throws an unchecked exception.
     * @see #unwrap(JSONObject, Class)
     * @see #unwrap(JSONObject, Type)
     */
    public static Map<String, Object> unwrap(final JSONObject jsonObject) throws IllegalArgumentException, JSONException, RuntimeException {
        return unwrap(jsonObject, Map.class);
    }

    /**
     * Converts a {@link JSONObject} to an instance of the specified class.
     *
     * <p>Delegates to {@link #unwrap(JSONObject, Type)}. Supports:
     * <ul>
     *   <li>{@link Map} implementations ({@link java.util.HashMap},
     *       {@link java.util.LinkedHashMap}, {@link java.util.TreeMap}, etc.)</li>
     *   <li>JavaBean classes with accessible properties, including supported records and immutable beans;
     *       incompatible scalar values are converted to the declared property type before construction</li>
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
     * @throws IllegalArgumentException if {@code jsonObject} or {@code targetType} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    public static <T> T unwrap(final JSONObject jsonObject, final Class<? extends T> targetType)
            throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * <p>Conversion is recursive: map keys and scalar values are converted to their declared
     * types, and nested {@link JSONObject} and {@link JSONArray} values are converted according
     * to the value type or property type declared in {@code targetType}.
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
     * @throws IllegalArgumentException if {@code jsonObject} or {@code targetType} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONObject jsonObject, Type<? extends T> targetType) throws IllegalArgumentException, JSONException, RuntimeException {
        return unwrap(jsonObject, targetType, new JsonConversionContext());
    }

    /**
     * @throws IllegalArgumentException if {@code jsonObject} or {@code targetType} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    private static <T> T unwrap(final JSONObject jsonObject, Type<? extends T> targetType, final JsonConversionContext active)
            throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(jsonObject, cs.jsonObject);
        N.checkArgNotNull(targetType, cs.targetType);

        if (!targetType.javaType().equals(Object.class) && targetType.javaType().isAssignableFrom(JSONObject.class)) {
            return (T) jsonObject;
        }

        targetType = targetType.isObject() ? Type.of("Map<String, Object>") : targetType;
        enterJsonConversion(jsonObject, active);
        try {
            final Class<?> cls = targetType.javaType();

            if (targetType.isMap()) {
                @SuppressWarnings("rawtypes")
                final Map<Object, Object> map = N.newMap((Class<Map>) cls, jsonObject.keySet().size());
                validateJsonOutput(map, targetType, active);
                final Iterator<String> iter = jsonObject.keys();
                final Type<?> keyType = targetType.parameterTypes().get(0);
                final Type<?> valueType = targetType.parameterTypes().get(1);
                String key = null;
                Object convertedKey = null;
                Object value = null;

                while (iter.hasNext()) {
                    key = iter.next();
                    convertedKey = keyType.javaType().isAssignableFrom(String.class) ? key : keyType.valueOf(key);
                    value = jsonObject.get(key);

                    if (value == JSONObject.NULL) {
                        value = null;
                    } else if (value != null) {
                        if (value instanceof JSONObject) {
                            value = unwrap((JSONObject) value, valueType, active);
                        } else if (value instanceof JSONArray) {
                            value = unwrap((JSONArray) value, valueType, active);
                        } else if (canConvertJavaContainer(value, valueType)) {
                            value = convertJavaContainer(value, valueType, active);
                        } else if (!valueType.javaType().isAssignableFrom(value.getClass())) {
                            rejectJsonCycles(value, active);
                            value = valueType.valueOf(value);
                        }
                    }

                    if (map.containsKey(convertedKey)) {
                        throw new IllegalArgumentException("Converted map keys collide");
                    }
                    map.put(convertedKey, value);
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
                                value = unwrap((JSONObject) value, propInfo.jsonXmlType, active);
                            } else if (value instanceof JSONArray) {
                                value = unwrap((JSONArray) value, propInfo.jsonXmlType, active);
                            } else if (canConvertJavaContainer(value, propInfo.jsonXmlType)) {
                                value = convertJavaContainer(value, propInfo.jsonXmlType, active);
                            } else if (!propInfo.jsonXmlType.javaType().isInstance(value)) {
                                // Bean setters may convert incompatible containers to scalar text.
                                rejectJsonCycles(value, active);
                            }
                        }

                        propInfo.setPropValue(result, value);
                        if (beanInfo.isImmutable && result instanceof Object[]) {
                            // Constructor arguments are stored without the scalar conversion performed by setters.
                            final Object storedValue = propInfo.getPropValue(result);
                            if (storedValue != null && !propInfo.jsonXmlType.javaType().isInstance(storedValue)) {
                                propInfo.setPropValue(result, N.convert(storedValue, propInfo.jsonXmlType.javaType()));
                            }
                        }
                    }
                }

                return beanInfo.finishBeanResult(result);
            } else {
                throw new IllegalArgumentException(targetType.name() + " is not a map or bean type");
            }

        } finally {
            active.activeContainers.remove(jsonObject);
        }
    }

    /**
     * Converts a {@link JSONArray} to a {@code List<T>} with inferred element types.
     *
     * <p>Convenience overload equivalent to {@link #toList(JSONArray, Class) toList(jsonArray, Object.class)}.
     * JSON values are mapped to Java types as follows:
     * <ul>
     *   <li>JSON numbers &rarr; {@link Integer} or {@link Long} for an integer literal that fits,
     *       {@link java.math.BigInteger} for one that does not, and {@link java.math.BigDecimal} - not
     *       {@link Double} - for a decimal or exponent literal; the exceptions are the literals
     *       {@code BigDecimal} itself cannot parse, which org.json falls back to {@code Double} for - a negative
     *       literal whose value is zero ({@code -0}, {@code -0.0}, {@code -0e5}) and a negative exponent past
     *       {@code BigDecimal}'s {@code int} scale ({@code 1e-2147483648}). A number that was
     *       put into the {@link JSONArray} programmatically is returned unchanged, so {@code Double},
     *       {@link Float} and other box types are possible too; use
     *       {@link #toList(JSONArray, Class) toList(jsonArray, Double.class)} when a specific box type is
     *       required</li>
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
     * <p>The returned list is a {@code List<Object>} because the element types are decided by the JSON, not by
     * the caller. Use {@link #toList(JSONArray, Class)} or {@link #unwrap(JSONArray, Type)} when you need a
     * list with a checked element type.
     *
     * @param jsonArray the {@link JSONArray} to convert
     * @return a {@link java.util.List} containing all elements from the {@link JSONArray}
     * @throws IllegalArgumentException if {@code jsonArray} is null, a traversed JSON-container conversion is cyclic, or a container factory fails to supply a fresh empty instance of the requested type.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if a container factory or an operation on a resulting container throws an unchecked exception.
     * @see #toList(JSONArray, Class)
     */
    public static List<Object> unwrap(final JSONArray jsonArray) throws IllegalArgumentException, JSONException, RuntimeException {
        return toList(jsonArray, Object.class);
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
     * @throws IllegalArgumentException if {@code jsonArray} or {@code targetType} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    public static <T> T unwrap(final JSONArray jsonArray, final Class<? extends T> targetType)
            throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * @throws IllegalArgumentException if {@code jsonArray} or {@code targetType} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    @SuppressWarnings("unchecked")
    public static <T> T unwrap(final JSONArray jsonArray, Type<? extends T> targetType) throws IllegalArgumentException, JSONException, RuntimeException {
        return unwrap(jsonArray, targetType, new JsonConversionContext());
    }

    /**
     * @throws IllegalArgumentException if {@code jsonArray} or {@code targetType} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    private static <T> T unwrap(final JSONArray jsonArray, Type<? extends T> targetType, final JsonConversionContext active)
            throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(jsonArray, cs.jsonArray);
        N.checkArgNotNull(targetType, cs.targetType);

        if (!targetType.javaType().equals(Object.class) && targetType.javaType().isAssignableFrom(JSONArray.class)) {
            return (T) jsonArray;
        }

        targetType = targetType.isObject() ? Type.of("List<Object>") : targetType;
        enterJsonConversion(jsonArray, active);
        try {
            final int len = jsonArray.length();

            if (targetType.isCollection()) {
                @SuppressWarnings("rawtypes")
                final Collection<Object> coll = N.newCollection((Class<Collection>) targetType.javaType(), len);
                validateJsonOutput(coll, targetType, active);
                final Type<?> elementType = targetType.elementType();
                Object element = null;

                for (int i = 0; i < len; i++) {
                    element = jsonArray.get(i);

                    if (element == JSONObject.NULL) {
                        element = null;
                    } else if (element != null) {
                        if (element instanceof JSONObject) {
                            element = unwrap((JSONObject) element, elementType, active);
                        } else if (element instanceof JSONArray) {
                            element = unwrap((JSONArray) element, elementType, active);
                        } else if (canConvertJavaContainer(element, elementType)) {
                            element = convertJavaContainer(element, elementType, active);
                        } else if (!elementType.javaType().isAssignableFrom(element.getClass())) {
                            rejectJsonCycles(element, active);
                            element = elementType.valueOf(element);
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
                        rejectJsonCycles(element, active);
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
                            element = unwrap((JSONObject) element, elementType, active);
                        } else if (element instanceof JSONArray) {
                            element = unwrap((JSONArray) element, elementType, active);
                        } else if (canConvertJavaContainer(element, elementType)) {
                            element = convertJavaContainer(element, elementType, active);
                        } else if (!elementType.javaType().isAssignableFrom(element.getClass())) {
                            rejectJsonCycles(element, active);
                            element = elementType.valueOf(element);
                        }
                    }

                    array[i] = element;
                }

                return (T) array;
            } else {
                // A type assignable from JSONArray is impossible here: such types already returned
                // from the guard at the top of the method.
                throw new IllegalArgumentException(targetType.name() + " is not an array or collection type");
            }

        } finally {
            active.activeContainers.remove(jsonArray);
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
     * @throws IllegalArgumentException if {@code jsonArray} or {@code elementClass} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    public static <T> List<T> toList(final JSONArray jsonArray, final Class<? extends T> elementClass)
            throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(elementClass, cs.elementClass);

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
     * @throws IllegalArgumentException if {@code jsonArray} or {@code elementType} is null, a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    public static <T> List<T> toList(final JSONArray jsonArray, final Type<T> elementType) throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(jsonArray, cs.jsonArray);
        N.checkArgNotNull(elementType, cs.elementType);

        final JsonConversionContext active = new JsonConversionContext();
        indexJsonGraph(jsonArray, active);
        final int len = jsonArray.length();
        final List<Object> coll = new ArrayList<>(len);

        Object element = null;

        for (int i = 0; i < len; i++) {
            element = jsonArray.get(i);

            if (element == JSONObject.NULL) {
                element = null;
            } else if (element != null) {
                if (element instanceof JSONObject) {
                    element = unwrap((JSONObject) element, elementType, active);
                } else if (element instanceof JSONArray) {
                    element = unwrap((JSONArray) element, elementType, active);
                } else if (canConvertJavaContainer(element, elementType)) {
                    element = convertJavaContainer(element, elementType, active);
                } else if (!elementType.javaType().isAssignableFrom(element.getClass())) {
                    rejectJsonCycles(element, active);
                    element = elementType.valueOf(element);
                }
            }

            coll.add(element);
        }

        return (List<T>) coll;
    }

    /**
     * @throws JSONException if reading JSON graph contents fails
     * @throws IllegalArgumentException if {@code value} is already being converted on the active recursion path
     */
    private static void enterJsonConversion(final Object value, final JsonConversionContext context) throws JSONException, IllegalArgumentException {
        indexJsonGraph(value, context);
        if (context.activeContainers.put(value, Boolean.TRUE) != null) {
            throw new IllegalArgumentException("Cyclic JSON container conversion");
        }
    }

    private static boolean canConvertJavaContainer(final Object value, final Type<?> target) {
        return value instanceof Map && target.isMap()
                || (value instanceof Collection || value.getClass().isArray()) && (target.isCollection() || target.isArray());
    }

    /**
     * @throws IllegalArgumentException if a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    private static Object convertJavaElement(final Object value, final Type<?> target, final JsonConversionContext context)
            throws IllegalArgumentException, JSONException, RuntimeException {
        if (value == null || value == JSONObject.NULL) {
            return target.defaultValue();
        }
        if (value instanceof JSONObject object) {
            return unwrap(object, target, context);
        }
        if (value instanceof JSONArray array) {
            return unwrap(array, target, context);
        }
        if (canConvertJavaContainer(value, target)) {
            return convertJavaContainer(value, target, context);
        }
        // Ordinary Object leaves remain opaque, even if they contain cycles. Native JSON was handled above.
        if (target.javaType().isInstance(value)) {
            return value;
        }
        rejectJsonCycles(value, context);
        return target.valueOf(value);
    }

    /**
     * @throws IllegalArgumentException if a requested target kind is incompatible with the source, a conversion encounters cyclic containers or colliding map keys, a container factory fails to supply a fresh empty instance of the requested type, or a selected converter rejects an argument.
     * @throws JSONException if reading a JSONObject member or JSONArray element fails during traversal
     * @throws RuntimeException if construction, property access, a target container operation, or a type-specific conversion fails; the concrete exception depends on that operation
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Object convertJavaContainer(final Object value, final Type<?> target, final JsonConversionContext context)
            throws IllegalArgumentException, JSONException, RuntimeException {
        enterJsonConversion(value, context);
        try {
            if (target.isMap()) {
                final Map<?, ?> source = (Map<?, ?>) value;
                final Map output = N.newMap((Class) target.javaType(), source.size());
                validateJsonOutput(output, target, context);
                for (final Map.Entry<?, ?> entry : source.entrySet()) {
                    final Object key = convertJavaElement(entry.getKey(), target.parameterTypes().get(0), context);
                    if (output.containsKey(key)) {
                        throw new IllegalArgumentException("Converted map keys collide");
                    }
                    output.put(key, convertJavaElement(entry.getValue(), target.parameterTypes().get(1), context));
                }
                return output;
            }
            final Collection<?> source = value instanceof Collection ? (Collection<?>) value : null;
            final int length = source == null ? Array.getLength(value) : source.size();
            final Iterator<?> iterator = source == null ? null : source.iterator();
            final Type<?> elementType = target.elementType();
            if (target.isArray()) {
                final Object output = Array.newInstance(target.javaType().getComponentType(), length);
                for (int i = 0; i < length; i++) {
                    Array.set(output, i, convertJavaElement(iterator == null ? Array.get(value, i) : iterator.next(), elementType, context));
                }
                return output;
            }
            final Collection output = N.newCollection((Class) target.javaType(), length);
            validateJsonOutput(output, target, context);
            for (int i = 0; i < length; i++) {
                output.add(convertJavaElement(iterator == null ? Array.get(value, i) : iterator.next(), elementType, context));
            }
            return output;
        } finally {
            context.activeContainers.remove(value);
        }
    }

    private static final class JsonConversionContext {
        final java.util.IdentityHashMap<Object, Boolean> activeContainers = new java.util.IdentityHashMap<>();
        final java.util.IdentityHashMap<Object, Boolean> sourceAndOutputContainers = new java.util.IdentityHashMap<>();
        final java.util.IdentityHashMap<Object, Boolean> acyclicContainers = new java.util.IdentityHashMap<>();
    }

    private record JsonVisit(Object value, boolean finished) {
    }

    private static boolean isJsonGraphNode(final Object value) {
        return value instanceof JSONObject || value instanceof JSONArray || value instanceof Map || value instanceof Collection || value instanceof Object[];
    }

    // Index the full input before any factory runs, including empty siblings reached later in conversion.
    // Merely indexing an opaque Object leaf does not reject its internal cycles.
    /**
     * @throws JSONException if reading JSON graph contents fails
     */
    private static void indexJsonGraph(final Object root, final JsonConversionContext context) throws JSONException {
        visitJsonGraph(root, context.sourceAndOutputContainers, false);
    }

    /**
     * @throws JSONException if reading JSON graph contents fails
     * @throws IllegalArgumentException if the value graph contains a cycle
     */
    private static void rejectJsonCycles(final Object root, final JsonConversionContext context) throws JSONException, IllegalArgumentException {
        // Scalar converters may recursively format Java containers, bypassing the typed-container traversal.
        visitJsonGraph(root, context.acyclicContainers, true);
    }

    /**
     * @throws JSONException if reading JSON graph contents fails
     * @throws IllegalArgumentException if {@code rejectCycles} is true and the graph contains a cycle
     */
    private static void visitJsonGraph(final Object root, final java.util.IdentityHashMap<Object, Boolean> visited, final boolean rejectCycles)
            throws JSONException, IllegalArgumentException {
        if (!isJsonGraphNode(root) || visited.containsKey(root)) {
            return;
        }
        final java.util.ArrayDeque<JsonVisit> pending = new java.util.ArrayDeque<>();
        final java.util.IdentityHashMap<Object, Boolean> visiting = new java.util.IdentityHashMap<>();
        pending.push(new JsonVisit(root, false));
        while (!pending.isEmpty()) {
            final JsonVisit visit = pending.pop();
            final Object value = visit.value();
            if (!isJsonGraphNode(value) || visited.containsKey(value)) {
                continue;
            }
            if (visit.finished()) {
                visited.put(value, Boolean.TRUE);
                visiting.remove(value);
                continue;
            }
            if (visiting.put(value, Boolean.TRUE) != null) {
                if (rejectCycles) {
                    throw new IllegalArgumentException("Cyclic JSON container conversion");
                }
                continue;
            }
            pending.push(new JsonVisit(value, true));
            if (value instanceof JSONObject object) {
                for (final String key : object.keySet()) {
                    pushJsonGraphNode(pending, object.get(key));
                }
            } else if (value instanceof JSONArray array) {
                for (int i = 0; i < array.length(); i++) {
                    pushJsonGraphNode(pending, array.get(i));
                }
            } else if (value instanceof Map<?, ?> map) {
                for (final Map.Entry<?, ?> entry : map.entrySet()) {
                    pushJsonGraphNode(pending, entry.getKey());
                    pushJsonGraphNode(pending, entry.getValue());
                }
            } else if (value instanceof Collection<?> collection) {
                for (final Object element : collection) {
                    pushJsonGraphNode(pending, element);
                }
            } else {
                for (final Object element : (Object[]) value) {
                    pushJsonGraphNode(pending, element);
                }
            }
        }
    }

    // Only container nodes need a stack entry: a scalar leaf is discarded on pop anyway, and a JSON document
    // is mostly scalars, so filtering here keeps the walk from allocating a JsonVisit per leaf.
    private static void pushJsonGraphNode(final java.util.ArrayDeque<JsonVisit> pending, final Object value) {
        if (isJsonGraphNode(value)) {
            pending.push(new JsonVisit(value, false));
        }
    }

    /**
     * @throws IllegalArgumentException if {@code output} is not an instance of the requested type, is a non-empty container, or is a source or previously produced container
     */
    private static void validateJsonOutput(final Object output, final Type<?> target, final JsonConversionContext context) throws IllegalArgumentException {
        if (!target.javaType().isInstance(output) || context.sourceAndOutputContainers.containsKey(output)
                || output instanceof Collection<?> collection && !collection.isEmpty() || output instanceof Map<?, ?> map && !map.isEmpty()) {
            throw new IllegalArgumentException("Container factory must create a fresh empty " + target.name());
        }
        context.sourceAndOutputContainers.put(output, Boolean.TRUE);
    }
}
