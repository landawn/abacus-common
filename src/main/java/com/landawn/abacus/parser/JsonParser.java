/*
 * Copyright (C) 2015 HaiYang Li
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

package com.landawn.abacus.parser;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Map;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.stream.Stream;

/**
 * Interface for JSON parsing and serialization operations.
 * This interface extends the base Parser interface and provides additional methods
 * specifically for JSON processing, including streaming support and direct string parsing.
 *
 * <p>The JsonParser provides various methods for:</p>
 * <ul>
 *   <li>Deserializing JSON strings, files, streams, and readers into Java objects</li>
 *   <li>Serializing Java objects to JSON format</li>
 *   <li>Streaming large JSON arrays for memory-efficient processing</li>
 *   <li>Customizing serialization and deserialization behavior through configuration</li>
 * </ul>
 *
 * <p>Numeric values requested as {@link java.math.BigDecimal} preserve the input's decimal
 * precision and scale. Unquoted decimal values converted to integral targets are truncated
 * toward zero: {@code [1.50]} read as {@code List<Integer>} yields {@code [1]}. Quoted numeric
 * text uses the target type's parser, so {@code ["1.50"]} rejects that same integral target.
 * Index and timestamp metadata in {@code Indexed}/{@code Timed} require integer notation.</p>
 *
 * <p>Object properties require a value after the colon, including ignored or unknown properties.
 * Use an explicit {@code null} or a quoted empty string when that is the intended value;
 * a missing value is rejected with {@code ParsingException}.</p>
 *
 * <p><b>{@code parse()} vs {@code deserialize()}:</b></p>
 * <p>Both methods convert JSON text into Java objects. They overlap deliberately for {@code String} input:</p>
 * <ul>
 *   <li>{@code parse(String, ...)} — A {@code String}-only convenience surface and an intentional alias of
 *       the inherited {@code deserialize(String, ...)} overloads (same default/config behavior). It additionally
 *       supports JSON array-like strings that may not be strictly bracketed (e.g., CSV rows like
 *       {@code "a","b","c"} without surrounding {@code []}) and offers the parse-into-existing
 *       array/collection/map forms not present on {@code deserialize()}. Prefer {@code parse()} when working
 *       directly with JSON strings, especially for populating existing containers.</li>
 *   <li>{@code deserialize()} — Inherited from {@link Parser}, this is the general-purpose deserialization
 *       method that supports multiple input sources: {@code String}, {@code File}, {@code InputStream},
 *       and {@code Reader}. Use {@code deserialize()} when reading from files, streams, or readers.</li>
 *   <li>Range deserialization ({@link #deserialize(String, int, int, Type)} and its overloads) is a
 *       {@code String}-only optimization that lives under the {@code deserialize} name for grouping with the
 *       other {@code deserialize} overloads; there is no corresponding {@code parse(String, int, int, ...)} form.</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JsonParser parser = ParserFactory.createJsonParser();
 *
 * // Parse JSON string to object
 * Person person = parser.parse("{\"name\":\"John\",\"age\":30}", Person.class);
 *
 * // Parse JSON with configuration
 * JsonDeserConfig config = new JsonDeserConfig()
 *     .setIgnoreUnmatchedProperty(true);
 * Person person2 = parser.parse(jsonString, config, Person.class);
 *
 * // Stream parsing for large JSON arrays
 * try (Stream<Person> stream = parser.stream(largeJsonFile, Type.of(Person.class))) {
 *     stream.filter(p -> p.getAge() > 18)
 *           .forEach(System.out::println);
 * }
 * }</pre>
 *
 * @see JsonSerConfig
 * @see JsonDeserConfig
 * @see ParserFactory
 */
public interface JsonParser extends Parser<JsonSerConfig, JsonDeserConfig> {

    /**
     * Parses a JSON string into an object of the specified type.
     * This is a convenience method that uses default deserialization configuration.
     *
     * <p>This {@code parse(String, ...)} overload is an intentional {@code String}-convenience alias of the
     * inherited {@link Parser#deserialize(String, Type)}; both produce identical results for {@code String}
     * input. See the class-level documentation for when to prefer {@code parse()} over {@code deserialize()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = parser.parse(json, Type.of(Person.class));
     *
     * // For collections
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<Item> items = parser.parse(jsonArray, Type.of(new com.landawn.abacus.util.TypeReference<List<Item>>() {}));
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the target
     *               type's default value, or an empty value, is returned)
     * @param targetType the type of the target object to deserialize into (must not be {@code null})
     * @return an instance of {@code T} parsed from the JSON string; if {@code source} is {@code null} or
     *         empty, the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     * @see #parseInto(String, Collection)
     * @see #parseInto(String, Map)
     */
    <T> T parse(String source, Type<? extends T> targetType) throws IllegalArgumentException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an object of the specified type.
     * This is a convenience method that uses default deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = parser.parse(json, Person.class);
     *
     * // For collections
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<Map<String, Object>> items = parser.parse(jsonArray, List.class);
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the target
     *               type's default value, or an empty value, is returned)
     * @param targetType the class of the target object to deserialize into (must not be {@code null})
     * @return an instance of {@code T} parsed from the JSON string; if {@code source} is {@code null} or
     *         empty, the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    <T> T parse(String source, Class<? extends T> targetType) throws IllegalArgumentException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an object of the specified type with custom configuration.
     * The configuration allows control over deserialization behavior such as
     * ignoring unknown properties, handling {@code null} values, and type mappings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setReadNullToEmpty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30,\"unknown\":\"value\"}";
     * Person person = parser.parse(json, config, Type.of(Person.class));
     * // "unknown" field will be ignored
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the target
     *               type's default value, or an empty value, is returned)
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the type of the target object to deserialize into (must not be {@code null})
     * @return an instance of {@code T} parsed from the JSON string; if {@code source} is {@code null} or
     *         empty, the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    <T> T parse(String source, JsonDeserConfig config, Type<? extends T> targetType) throws IllegalArgumentException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an object of the specified type with custom configuration.
     * The configuration allows control over deserialization behavior such as
     * ignoring unknown properties, handling {@code null} values, and type mappings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true)
     *     .setReadNullToEmpty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30,\"unknown\":\"value\"}";
     * Person person = parser.parse(json, config, Person.class);
     * // "unknown" field will be ignored
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the target
     *               type's default value, or an empty value, is returned)
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the class of the target object to deserialize into (must not be {@code null})
     * @return an instance of {@code T} parsed from the JSON string; if {@code source} is {@code null} or
     *         empty, the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    <T> T parse(String source, JsonDeserConfig config, Class<? extends T> targetType) throws IllegalArgumentException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an existing array.
     * The array must have room for all retained elements; unused output slots are left unchanged.
     * This method fills the provided array with parsed values from the JSON array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[1, 2, 3, 4, 5]";
     * Integer[] numbers = new Integer[5];
     * parser.parseInto(json, numbers);
     * // numbers array is now filled with values [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the method returns without
     *               modifying {@code output}); accepts an array or the parser's supported unwrapped element sequence
     * @param output the pre-allocated array to populate with parsed values (must not be {@code null})
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IndexOutOfBoundsException if more parsed elements are retained than the output array can hold
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    void parseInto(String source, Object[] output) throws IllegalArgumentException, IndexOutOfBoundsException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an existing array with custom configuration.
     * The array must have room for all retained elements; unused output slots are left unchanged.
     * This method fills the provided array with parsed values from the JSON array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * String json = "[1, 2, 3]";
     * Integer[] numbers = new Integer[3];
     * parser.parseInto(json, config, numbers);
     * }</pre>
     *
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the method returns without
     *               modifying {@code output}); accepts an array or the parser's supported unwrapped element sequence
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param output the pre-allocated array to populate with parsed values (must not be {@code null})
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws IndexOutOfBoundsException if more parsed elements are retained than the output array can hold
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    void parseInto(String source, JsonDeserConfig config, Object[] output)
            throws IllegalArgumentException, IndexOutOfBoundsException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an existing Collection.
     * The parsed elements are added to the collection without clearing existing elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[\"apple\", \"banana\", \"orange\"]";
     * List<String> fruits = new ArrayList<>();
     * parser.parseInto(json, fruits);
     * // fruits now contains the parsed values added to any existing elements
     * }</pre>
     *
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the method returns without
     *               modifying {@code output}); accepts an array or the parser's supported unwrapped element sequence
     * @param output the Collection to populate with parsed values, must not be {@code null}; existing elements are preserved
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UnsupportedOperationException if parsing attempts to add an element to a collection that does not support insertion.
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    void parseInto(String source, Collection<?> output) throws IllegalArgumentException, UnsupportedOperationException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an existing Collection with custom configuration.
     * The parsed elements are added to the collection without clearing existing elements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * String json = "[\"item1\", \"item2\"]";
     * List<String> items = new ArrayList<>();
     * parser.parseInto(json, config, items);
     * }</pre>
     *
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the method returns without
     *               modifying {@code output}); accepts an array or the parser's supported unwrapped element sequence
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param output the Collection to populate with parsed values, must not be {@code null}; existing elements are preserved
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UnsupportedOperationException if parsing attempts to add an element to a collection that does not support insertion.
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    void parseInto(String source, JsonDeserConfig config, Collection<?> output)
            throws IllegalArgumentException, UnsupportedOperationException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an existing Map.
     * The parsed entries are added to the map without clearing existing entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
     * Map<String, String> map = new HashMap<>();
     * parser.parseInto(json, map);
     * // map now contains the parsed entries added to any existing entries
     * }</pre>
     *
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the method returns
     *               without modifying {@code output}); must contain a JSON object when non-empty
     * @param output the Map to populate with parsed key-value pairs, must not be {@code null}; existing entries with other keys are preserved,
     *        while matching keys are overwritten
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UnsupportedOperationException if parsing attempts to add an entry to a map that does not support insertion.
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    void parseInto(String source, Map<?, ?> output) throws IllegalArgumentException, UnsupportedOperationException, ParsingException, UncheckedIOException;

    /**
     * Parses a JSON string into an existing Map with custom configuration.
     * The parsed entries are added to the map without clearing existing entries.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
     * Map<String, String> map = new HashMap<>();
     * parser.parseInto(json, config, map);
     * }</pre>
     *
     * @param source the JSON string to parse (may be {@code null} or empty, in which case the method returns
     *               without modifying {@code output}); must contain a JSON object when non-empty
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param output the Map to populate with parsed key-value pairs, must not be {@code null}; existing entries with other keys are preserved,
     *        while matching keys are overwritten
     * @throws IllegalArgumentException if {@code output} is {@code null}.
     * @throws UnsupportedOperationException if parsing attempts to add an entry to a map that does not support insertion.
     * @throws ParsingException if the source contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    void parseInto(String source, JsonDeserConfig config, Map<?, ?> output)
            throws IllegalArgumentException, UnsupportedOperationException, ParsingException, UncheckedIOException;

    /**
     * Deserializes a range of a JSON string into an object of the specified type.
     * Useful when the JSON payload is embedded in a larger string. The default implementation
     * ({@link JsonParserImpl}) parses the range without allocating an intermediate {@code String} copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\"}suffix";
     * // Deserialize only the JSON object part (indices 6 to 21)
     * Person person = parser.deserialize(json, 6, 21, Type.of(Person.class));
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to deserialize
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param targetType the type of the target object (must not be {@code null})
     * @return an instance of {@code T} deserialized from the JSON string; if the selected range is empty,
     *         the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws IndexOutOfBoundsException if the indices are out of bounds or {@code fromIndex > toIndex}
     * @throws ParsingException if the selected range contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, Type<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, ParsingException, UncheckedIOException;

    /**
     * Deserializes a range of a JSON string into an object of the specified type.
     * Useful when the JSON payload is embedded in a larger string. The default implementation
     * ({@link JsonParserImpl}) parses the range without allocating an intermediate {@code String} copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\"}suffix";
     * // Deserialize only the JSON object part (indices 6 to 21)
     * Person person = parser.deserialize(json, 6, 21, Person.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to deserialize
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param targetType the class of the target object (must not be {@code null})
     * @return an instance of {@code T} deserialized from the JSON string; if the selected range is empty,
     *         the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws IndexOutOfBoundsException if the indices are out of bounds or {@code fromIndex > toIndex}
     * @throws ParsingException if the selected range contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, Class<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, ParsingException, UncheckedIOException;

    /**
     * Deserializes a range of a JSON string into an object with custom configuration.
     * Useful when the JSON payload is embedded in a larger string. The default implementation
     * ({@link JsonParserImpl}) parses the range without allocating an intermediate {@code String} copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * String json = "prefix{\"name\":\"John\",\"extra\":\"ignored\"}suffix";
     * Person person = parser.deserialize(json, 6, 39, config, Type.of(Person.class));
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to deserialize
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param config the deserialization configuration to control parsing behavior
     * @param targetType the type of the target object (must not be {@code null})
     * @return an instance of {@code T} deserialized from the JSON string; if the selected range is empty,
     *         the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws IndexOutOfBoundsException if the indices are out of bounds or {@code fromIndex > toIndex}
     * @throws ParsingException if the selected range contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, JsonDeserConfig config, Type<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, ParsingException, UncheckedIOException;

    /**
     * Deserializes a range of a JSON string into an object with custom configuration.
     * Useful when the JSON payload is embedded in a larger string. The default implementation
     * ({@link JsonParserImpl}) parses the range without allocating an intermediate {@code String} copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * String json = "prefix{\"name\":\"John\",\"extra\":\"ignored\"}suffix";
     * Person person = parser.deserialize(json, 6, 39, config, Person.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to deserialize
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param config the deserialization configuration to control parsing behavior
     * @param targetType the class of the target object (must not be {@code null})
     * @return an instance of {@code T} deserialized from the JSON string; if the selected range is empty,
     *         the target type's default value (or an empty value) is returned
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws IndexOutOfBoundsException if the indices are out of bounds or {@code fromIndex > toIndex}
     * @throws ParsingException if the selected range contains invalid JSON
     * @throws UncheckedIOException if a delegated value reader or converter reports an I/O failure while materializing values from the
     *         JSON text
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, JsonDeserConfig config, Class<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, ParsingException, UncheckedIOException;

    /**
     * Creates a stream for parsing JSON array elements lazily from a JSON string.
     * The stream should be closed after use to free resources.
     *
     * <p>The source String is already in memory, but its elements are parsed on demand as the stream
     * is consumed, without materializing the entire array of Java objects at once.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[{\"id\":1},{\"id\":2},{\"id\":3}]";
     * try (Stream<MyObject> stream = parser.stream(json, Type.of(MyObject.class))) {
     *     stream.filter(obj -> obj.getId() > 1)
     *           .forEach(obj -> processObject(obj));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the JSON string containing a JSON array (may be {@code null} or empty, in which case
     *               an empty stream is returned)
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code elementType} is null or unsupported for streaming.
     * @throws UnsupportedOperationException if the root of the source is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the returned stream.
     */
    <T> Stream<T> stream(String source, Type<? extends T> elementType) throws IllegalArgumentException, UnsupportedOperationException, ParsingException;

    /**
     * Creates a stream for parsing JSON array elements lazily with custom configuration from a JSON string.
     * The stream should be closed after use to free resources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * String json = "[{\"id\":1,\"extra\":\"data\"},{\"id\":2}]";
     * try (Stream<MyObject> stream = parser.stream(json, config, Type.of(MyObject.class))) {
     *     stream.forEach(obj -> process(obj));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the JSON string containing a JSON array (may be {@code null} or empty, in which case
     *               an empty stream is returned)
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code elementType} is null or unsupported for streaming.
     * @throws UnsupportedOperationException if the root of the source is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the returned stream.
     */
    <T> Stream<T> stream(String source, JsonDeserConfig config, Type<? extends T> elementType)
            throws IllegalArgumentException, UnsupportedOperationException, ParsingException;

    /**
     * Creates a stream for parsing JSON array elements from a file.
     * The stream should be closed after use to free resources and close the underlying file handle.
     *
     * <p>This method is ideal for processing large JSON files containing arrays
     * without loading the entire file into memory. Elements are parsed lazily as consumed.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File jsonFile = new File("data.json");
     * try (Stream<Person> stream = parser.stream(jsonFile, Type.of(Person.class))) {
     *     long count = stream.filter(p -> p.getAge() > 21).count();
     *     System.out.println("Adults: " + count);
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the JSON file containing a JSON array, must exist and be readable
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code source} is null or a directory that cannot be opened, or {@code elementType} is null or unsupported for streaming.
     * @throws UncheckedIOException if opening or initially reading the source fails; later read failures are raised while consuming the returned stream.
     * @throws UnsupportedOperationException if the root of the file content is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the returned stream.
     */
    <T> Stream<T> stream(File source, Type<? extends T> elementType)
            throws IllegalArgumentException, UncheckedIOException, UnsupportedOperationException, ParsingException;

    /**
     * Creates a stream for parsing JSON array elements from a file with custom configuration.
     * The stream should be closed after use to free resources and close the underlying file handle.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * File jsonFile = new File("data.json");
     * try (Stream<Person> stream = parser.stream(jsonFile, config, Type.of(Person.class))) {
     *     stream.forEach(person -> process(person));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the JSON file containing a JSON array, must exist and be readable
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code source} is null or a directory that cannot be opened, or {@code elementType} is null or unsupported for streaming.
     * @throws UncheckedIOException if opening or initially reading the source fails; later read failures are raised while consuming the returned stream.
     * @throws UnsupportedOperationException if the root of the file content is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the returned stream.
     */
    <T> Stream<T> stream(File source, JsonDeserConfig config, Type<? extends T> elementType)
            throws IllegalArgumentException, UncheckedIOException, UnsupportedOperationException, ParsingException;

    /**
     * Creates a stream for parsing JSON array elements from an InputStream.
     * The closeInputStreamWhenStreamIsClosed parameter controls whether the input stream
     * is closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (InputStream is = new FileInputStream("data.json");
     *     Stream<Item> stream = parser.stream(is, true, Type.of(Item.class))) {
     *     stream.limit(100)
     *           .forEach(item -> process(item));
     * }
     * // InputStream is automatically closed when stream is closed
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the input stream containing a JSON array, must not be {@code null}
     * @param closeInputStreamWhenStreamIsClosed if {@code true}, the input stream will be closed when the returned stream is closed
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code source} is null, or {@code elementType} is null or unsupported for streaming.
     * @throws UncheckedIOException if opening or initially reading the source fails; later read failures are raised
     *         while consuming the returned stream.
     * @throws UnsupportedOperationException if the root of the source content is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the
     *         returned stream.
     */
    <T> Stream<T> stream(InputStream source, boolean closeInputStreamWhenStreamIsClosed, Type<? extends T> elementType)
            throws IllegalArgumentException, UncheckedIOException, UnsupportedOperationException, ParsingException;

    /**
     * Creates a stream for parsing JSON array elements from an InputStream with custom configuration.
     * The closeInputStreamWhenStreamIsClosed parameter controls whether the input stream
     * is closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * try (InputStream is = new FileInputStream("data.json");
     *     Stream<Item> stream = parser.stream(is, true, config, Type.of(Item.class))) {
     *     stream.forEach(item -> process(item));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the input stream containing a JSON array, must not be {@code null}
     * @param closeInputStreamWhenStreamIsClosed if {@code true}, the input stream will be closed when the returned stream is closed
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code source} is null, or {@code elementType} is null or unsupported for streaming.
     * @throws UncheckedIOException if opening or initially reading the source fails; later read failures are raised
     *         while consuming the returned stream.
     * @throws UnsupportedOperationException if the root of the source content is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the
     *         returned stream.
     */
    <T> Stream<T> stream(InputStream source, boolean closeInputStreamWhenStreamIsClosed, JsonDeserConfig config, Type<? extends T> elementType)
            throws IllegalArgumentException, UncheckedIOException, UnsupportedOperationException, ParsingException;

    /**
     * Creates a stream for parsing JSON array elements from a Reader.
     * The closeReaderWhenStreamIsClosed parameter controls whether the reader
     * is closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("data.json");
     *      Stream<Product> stream = parser.stream(reader, true, Type.of(Product.class))) {
     *     Map<String, List<Product>> grouped = stream
     *         .collect(java.util.stream.Collectors.groupingBy(Product::getCategory));
     * }
     * // Reader is automatically closed when stream is closed
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the reader containing a JSON array, must not be {@code null}
     * @param closeReaderWhenStreamIsClosed if {@code true}, the reader will be closed when the returned stream is closed
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code source} is null, or {@code elementType} is null or unsupported for streaming.
     * @throws UncheckedIOException if opening or initially reading the source fails; later read failures are raised
     *         while consuming the returned stream.
     * @throws UnsupportedOperationException if the root of the source content is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the
     *         returned stream.
     */
    <T> Stream<T> stream(Reader source, boolean closeReaderWhenStreamIsClosed, Type<? extends T> elementType)
            throws IllegalArgumentException, UncheckedIOException, UnsupportedOperationException, ParsingException;

    /**
     * Creates a stream for parsing JSON array elements from a Reader with custom configuration.
     * The closeReaderWhenStreamIsClosed parameter controls whether the reader
     * is closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonDeserConfig config = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     * try (Reader reader = new FileReader("data.json");
     *      Stream<Product> stream = parser.stream(reader, true, config, Type.of(Product.class))) {
     *      stream.forEach(product -> process(product));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the reader containing a JSON array, must not be {@code null}
     * @param closeReaderWhenStreamIsClosed if {@code true}, the reader will be closed when the returned stream is closed
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/MapEntity/Collection/Array/Dataset/Sheet/EntityId element types are supported.
     * @return a {@code Stream} of parsed elements that must be closed after use; never {@code null}
     * @throws IllegalArgumentException if {@code source} is null, or {@code elementType} is null or unsupported for streaming.
     * @throws UncheckedIOException if opening or initially reading the source fails; later read failures are raised
     *         while consuming the returned stream.
     * @throws UnsupportedOperationException if the root of the source content is a JSON object or a quoted string
     * @throws ParsingException if the initial token is an unquoted scalar or malformed JSON; malformed later elements fail while consuming the
     *         returned stream.
     */
    <T> Stream<T> stream(Reader source, boolean closeReaderWhenStreamIsClosed, JsonDeserConfig config, Type<? extends T> elementType)
            throws IllegalArgumentException, UncheckedIOException, UnsupportedOperationException, ParsingException;
}
