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

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.stream.Stream;

/**
 * Interface for JSON parsing and serialization operations.
 * This interface extends the base Parser interface and provides additional methods
 * specifically for JSON processing, including streaming support and direct string parsing.
 * 
 * <p>The JSONParser provides various methods for:</p>
 * <ul>
 *   <li>Deserializing JSON strings, files, streams, and readers into Java objects</li>
 *   <li>Serializing Java objects to JSON format</li>
 *   <li>Streaming large JSON arrays for memory-efficient processing</li>
 *   <li>Customizing serialization and deserialization behavior through configuration</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * JSONParser parser = ParserFactory.createJSONParser();
 * 
 * // Parse JSON string to object
 * Person person = parser.readString("{\"name\":\"John\",\"age\":30}", Person.class);
 * 
 * // Parse JSON with configuration
 * JSONDeserializationConfig config = new JSONDeserializationConfig()
 *     .ignoreUnmatchedProperty(true);
 * Person person = parser.readString(jsonString, config, Person.class);
 * 
 * // Stream parsing for large JSON arrays
 * try (Stream<Person> stream = parser.stream(largeJsonFile, Person.class)) {
 *     stream.filter(p -> p.getAge() > 18)
 *           .forEach(System.out::println);
 * }
 * }</pre>
 * 
 * @see JSONSerializationConfig
 * @see JSONDeserializationConfig
 * @see ParserFactory
 */
public interface JSONParser extends Parser<JSONSerializationConfig, JSONDeserializationConfig> {

    /**
     * Parses a JSON string into an object of the specified type.
     * This is a convenience method that uses default deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = parser.readString(json, Type.of(Person.class));
     *
     * // For collections
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<Item> items = parser.readString(jsonArray, Type.of(new TypeReference<List<Item>>() {}.getType()));
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse, must be valid JSON format
     * @param targetType the type of the target object to deserialize into
     * @return the parsed object of type T, never null
     * @throws IllegalArgumentException if the source string is {@code null} or invalid JSON
     */
    <T> T readString(String source, Type<? extends T> targetType);

    /**
     * Parses a JSON string into an object of the specified type.
     * This is a convenience method that uses default deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = parser.readString(json, Person.class);
     *
     * // For collections
     * String jsonArray = "[{\"id\":1},{\"id\":2}]";
     * List<Item> items = parser.readString(jsonArray, List.class);
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse, must be valid JSON format
     * @param targetType the class of the target object to deserialize into
     * @return the parsed object of type T, never null
     * @throws IllegalArgumentException if the source string is {@code null} or invalid JSON
     */
    <T> T readString(String source, Class<? extends T> targetType);

    /**
     * Parses a JSON string into an object of the specified type with custom configuration.
     * The configuration allows control over deserialization behavior such as
     * ignoring unknown properties, handling {@code null} values, date formats, and more.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true)
     *     .readNullToEmpty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30,\"unknown\":\"value\"}";
     * Person person = parser.readString(json, config, Type.of(Person.class));
     * // "unknown" field will be ignored
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse, must be valid JSON format
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the type of the target object to deserialize into
     * @return the parsed object of type T, never null
     * @throws IllegalArgumentException if the source string is {@code null} or invalid JSON
     */
    <T> T readString(String source, JSONDeserializationConfig config, Type<? extends T> targetType);

    /**
     * Parses a JSON string into an object of the specified type with custom configuration.
     * The configuration allows control over deserialization behavior such as
     * ignoring unknown properties, handling {@code null} values, date formats, and more.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true)
     *     .readNullToEmpty(true);
     *
     * String json = "{\"name\":\"John\",\"age\":30,\"unknown\":\"value\"}";
     * Person person = parser.readString(json, config, Person.class);
     * // "unknown" field will be ignored
     * }</pre>
     *
     * @param <T> the target type parameter
     * @param source the JSON string to parse, must be valid JSON format
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param targetType the class of the target object to deserialize into
     * @return the parsed object of type T, never null
     * @throws IllegalArgumentException if the source string is {@code null} or invalid JSON
     */
    <T> T readString(String source, JSONDeserializationConfig config, Class<? extends T> targetType);

    /**
     * Parses a JSON string into an existing array.
     * The array must be pre-allocated with the correct size to match the JSON array length.
     * This method fills the provided array with parsed values from the JSON array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[1, 2, 3, 4, 5]";
     * Integer[] numbers = new Integer[5];
     * parser.readString(json, numbers);
     * // numbers array is now filled with values [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param source the JSON string to parse, must contain a JSON array
     * @param output the pre-allocated array to populate with parsed values, must not be null
     * @throws IllegalArgumentException if the source is {@code null}, invalid JSON, or array size doesn't match
     */
    void readString(String source, Object[] output);

    /**
     * Parses a JSON string into an existing array with custom configuration.
     * The array must be pre-allocated with the correct size to match the JSON array length.
     * This method fills the provided array with parsed values from the JSON array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * String json = "[1, 2, 3]";
     * Integer[] numbers = new Integer[3];
     * parser.readString(json, config, numbers);
     * }</pre>
     *
     * @param source the JSON string to parse, must contain a JSON array
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param output the pre-allocated array to populate with parsed values, must not be null
     * @throws IllegalArgumentException if the source is {@code null}, invalid JSON, or array size doesn't match
     */
    void readString(String source, JSONDeserializationConfig config, Object[] output);

    /**
     * Parses a JSON string into an existing Collection.
     * The collection is cleared before adding parsed elements from the JSON array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[\"apple\", \"banana\", \"orange\"]";
     * List<String> fruits = new ArrayList<>();
     * parser.readString(json, fruits);
     * // fruits now contains: ["apple", "banana", "orange"]
     * }</pre>
     *
     * @param source the JSON string to parse, must contain a JSON array
     * @param output the Collection to populate with parsed values, must not be {@code null} and will be cleared first
     * @throws IllegalArgumentException if the source is {@code null} or invalid JSON
     * @throws UnsupportedOperationException if the collection is unmodifiable
     */
    void readString(String source, Collection<?> output);

    /**
     * Parses a JSON string into an existing Collection with custom configuration.
     * The collection is cleared before adding parsed elements from the JSON array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * String json = "[\"item1\", \"item2\"]";
     * List<String> items = new ArrayList<>();
     * parser.readString(json, config, items);
     * }</pre>
     *
     * @param source the JSON string to parse, must contain a JSON array
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param output the Collection to populate with parsed values, must not be {@code null} and will be cleared first
     * @throws IllegalArgumentException if the source is {@code null} or invalid JSON
     * @throws UnsupportedOperationException if the collection is unmodifiable
     */
    void readString(String source, JSONDeserializationConfig config, Collection<?> output);

    /**
     * Parses a JSON string into an existing Map.
     * The map is cleared before adding parsed entries from the JSON object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
     * Map<String, String> map = new HashMap<>();
     * parser.readString(json, map);
     * // map now contains: {key1=value1, key2=value2}
     * }</pre>
     *
     * @param source the JSON string to parse, must contain a JSON object
     * @param output the Map to populate with parsed key-value pairs, must not be {@code null} and will be cleared first
     * @throws IllegalArgumentException if the source is {@code null} or invalid JSON
     * @throws UnsupportedOperationException if the map is unmodifiable
     */
    void readString(String source, Map<?, ?> output);

    /**
     * Parses a JSON string into an existing Map with custom configuration.
     * The map is cleared before adding parsed entries from the JSON object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * String json = "{\"key1\":\"value1\",\"key2\":\"value2\"}";
     * Map<String, String> map = new HashMap<>();
     * parser.readString(json, config, map);
     * }</pre>
     *
     * @param source the JSON string to parse, must contain a JSON object
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param output the Map to populate with parsed key-value pairs, must not be {@code null} and will be cleared first
     * @throws IllegalArgumentException if the source is {@code null} or invalid JSON
     * @throws UnsupportedOperationException if the map is unmodifiable
     */
    void readString(String source, JSONDeserializationConfig config, Map<?, ?> output);

    /**
     * Parses a substring of a JSON string into an object of the specified type.
     * This method allows parsing a portion of a larger string without creating a substring,
     * which can improve performance when working with large strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\"}suffix";
     * // Parse only the JSON object part (indices 6 to 23)
     * Person person = parser.deserialize(json, 6, 23, Type.of(Person.class));
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to parse
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param targetType the type of the target object
     * @return the parsed object of type T
     * @throws IndexOutOfBoundsException if the indices are out of bounds or fromIndex &gt; toIndex
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, Type<? extends T> targetType);

    /**
     * Parses a substring of a JSON string into an object of the specified type.
     * This method allows parsing a portion of a larger string without creating a substring,
     * which can improve performance when working with large strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "prefix{\"name\":\"John\"}suffix";
     * // Parse only the JSON object part (indices 6 to 23)
     * Person person = parser.deserialize(json, 6, 23, Person.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to parse
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param targetType the class of the target object
     * @return the parsed object of type T
     * @throws IndexOutOfBoundsException if the indices are out of bounds or fromIndex &gt; toIndex
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, Class<? extends T> targetType);

    /**
     * Parses a substring of a JSON string into an object with custom configuration.
     * This method allows parsing a portion of a larger string without creating a substring,
     * which can improve performance when working with large strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * String json = "prefix{\"name\":\"John\",\"extra\":\"ignored\"}suffix";
     * Person person = parser.deserialize(json, 6, 40, config, Type.of(Person.class));
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to parse
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param config the deserialization configuration to control parsing behavior
     * @param targetType the type of the target object
     * @return the parsed object of type T
     * @throws IndexOutOfBoundsException if the indices are out of bounds or fromIndex &gt; toIndex
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, JSONDeserializationConfig config, Type<? extends T> targetType);

    /**
     * Parses a substring of a JSON string into an object with custom configuration.
     * This method allows parsing a portion of a larger string without creating a substring,
     * which can improve performance when working with large strings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * String json = "prefix{\"name\":\"John\",\"extra\":\"ignored\"}suffix";
     * Person person = parser.deserialize(json, 6, 40, config, Person.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the JSON string containing the data to parse
     * @param fromIndex the starting index (inclusive) of the JSON content
     * @param toIndex the ending index (exclusive) of the JSON content
     * @param config the deserialization configuration to control parsing behavior
     * @param targetType the class of the target object
     * @return the parsed object of type T
     * @throws IndexOutOfBoundsException if the indices are out of bounds or fromIndex &gt; toIndex
     */
    <T> T deserialize(String source, int fromIndex, int toIndex, JSONDeserializationConfig config, Class<? extends T> targetType);

    /**
     * Creates a stream for parsing JSON array elements lazily from a JSON string.
     * The stream should be closed after use to free resources.
     *
     * <p>This method is useful for processing large JSON arrays without loading
     * the entire content into memory. Elements are parsed on-demand as the stream is consumed.</p>
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
     * @param source the JSON string containing a JSON array
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null}, invalid JSON, or not a JSON array
     */
    <T> Stream<T> stream(String source, Type<? extends T> elementType);

    /**
     * Creates a stream for parsing JSON array elements lazily with custom configuration from a JSON string.
     * The stream should be closed after use to free resources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * String json = "[{\"id\":1,\"extra\":\"data\"},{\"id\":2}]";
     * try (Stream<MyObject> stream = parser.stream(json, config, Type.of(MyObject.class))) {
     *     stream.forEach(obj -> process(obj));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the JSON string containing a JSON array
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null}, invalid JSON, or not a JSON array
     */
    <T> Stream<T> stream(String source, JSONDeserializationConfig config, Type<? extends T> elementType);

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
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null} or the file contains invalid JSON
     */
    <T> Stream<T> stream(File source, Type<? extends T> elementType);

    /**
     * Creates a stream for parsing JSON array elements from a file with custom configuration.
     * The stream should be closed after use to free resources and close the underlying file handle.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * File jsonFile = new File("data.json");
     * try (Stream<Person> stream = parser.stream(jsonFile, config, Type.of(Person.class))) {
     *     stream.forEach(person -> process(person));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the JSON file containing a JSON array, must exist and be readable
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null} or the file contains invalid JSON
     */
    <T> Stream<T> stream(File source, JSONDeserializationConfig config, Type<? extends T> elementType);

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
     * @param source the input stream containing a JSON array, must not be null
     * @param closeInputStreamWhenStreamIsClosed if {@code true}, the input stream will be closed when the returned stream is closed
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null} or contains invalid JSON
     */
    <T> Stream<T> stream(InputStream source, boolean closeInputStreamWhenStreamIsClosed, Type<? extends T> elementType);

    /**
     * Creates a stream for parsing JSON array elements from an InputStream with custom configuration.
     * The closeInputStreamWhenStreamIsClosed parameter controls whether the input stream
     * is closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * try (InputStream is = new FileInputStream("data.json");
     *     Stream<Item> stream = parser.stream(is, true, config, Type.of(Item.class))) {
     *     stream.forEach(item -> process(item));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the input stream containing a JSON array, must not be null
     * @param closeInputStreamWhenStreamIsClosed if {@code true}, the input stream will be closed when the returned stream is closed
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null} or contains invalid JSON
     */
    <T> Stream<T> stream(InputStream source, boolean closeInputStreamWhenStreamIsClosed, JSONDeserializationConfig config, Type<? extends T> elementType);

    /**
     * Creates a stream for parsing JSON array elements from a Reader.
     * The closeReaderWhenStreamIsClosed parameter controls whether the reader
     * is closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new FileReader("data.json");
     *     Stream<Product> stream = parser.stream(reader, true, Type.of(Product.class))) {
     *     Map<String, List<Product>> grouped = stream
     *         .collect(Collectors.groupingBy(Product::getCategory));
     * }
     * // Reader is automatically closed when stream is closed
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the reader containing a JSON array, must not be null
     * @param closeReaderWhenStreamIsClosed if {@code true}, the reader will be closed when the returned stream is closed
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null} or contains invalid JSON
     */
    <T> Stream<T> stream(Reader source, boolean closeReaderWhenStreamIsClosed, Type<? extends T> elementType);

    /**
     * Creates a stream for parsing JSON array elements from a Reader with custom configuration.
     * The closeReaderWhenStreamIsClosed parameter controls whether the reader
     * is closed when the returned stream is closed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     * try (Reader reader = new FileReader("data.json");
     *      Stream<Product> stream = parser.stream(reader, true, config,Type.of(Product.class))) {
     *      stream.forEach(product -> process(product));
     * }
     * }</pre>
     *
     * @param <T> the element type parameter
     * @param source the reader containing a JSON array, must not be null
     * @param closeReaderWhenStreamIsClosed if {@code true}, the reader will be closed when the returned stream is closed
     * @param config the deserialization configuration to use (may be {@code null} for default behavior)
     * @param elementType the type of array elements. Only Bean/Map/Collection/Array/Dataset element types are supported.
     * @return a Stream of parsed elements that must be closed after use
     * @throws IllegalArgumentException if the source is {@code null} or contains invalid JSON
     */
    <T> Stream<T> stream(Reader source, boolean closeReaderWhenStreamIsClosed, JSONDeserializationConfig config, Type<? extends T> elementType);
}
