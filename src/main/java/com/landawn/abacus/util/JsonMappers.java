/*
 * Copyright (C) 2024 HaiYang Li
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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * A high-performance utility class for JSON serialization and deserialization operations using Jackson's JsonMapper.
 * This class provides a comprehensive set of static methods for converting Java objects to JSON and parsing JSON back 
 * to Java objects from various sources including strings, files, streams, and URLs.
 * 
 * <p>The class maintains an internal pool of JsonMapper instances for performance optimization, reusing mappers
 * when custom configurations are needed. All methods handle exceptions internally and wrap them in RuntimeExceptions
 * for simplified error handling.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe static methods for JSON operations</li>
 *   <li>Support for custom serialization/deserialization configurations</li>
 *   <li>Pretty printing support for formatted JSON output</li>
 *   <li>Generic type support through TypeReference</li>
 *   <li>Multiple input/output source types (String, File, Stream, URL, etc.)</li>
 *   <li>Internal mapper pooling for performance optimization</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * // Simple serialization
 * Person person = new Person("John", 30);
 * String json = JsonMappers.toJson(person);
 * 
 * // Pretty formatted output
 * String prettyJson = JsonMappers.toJson(person, true);
 * 
 * // Simple deserialization
 * Person parsed = JsonMappers.fromJson(json, Person.class);
 * 
 * // Generic type handling
 * List<Person> people = JsonMappers.fromJson(jsonArray, 
 *     new TypeReference<List<Person>>() {});
 * 
 * // Custom configuration
 * String jsonWithTimestamps = JsonMappers.toJson(dateObject,
 *     SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 * }</pre>
 *
 * @see JsonMapper
 * @see TypeReference
 * @see SerializationFeature
 * @see DeserializationFeature
 * @since 1.0
 */
public final class JsonMappers {
    private static final int POOL_SIZE = 128;
    private static final List<JsonMapper> mapperPool = new ArrayList<>(POOL_SIZE);

    private static final JsonMapper defaultJsonMapper = new JsonMapper();
    private static final JsonMapper defaultJsonMapperForPretty = (JsonMapper) new JsonMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final SerializationConfig defaultSerializationConfig = defaultJsonMapper.getSerializationConfig();
    private static final DeserializationConfig defaultDeserializationConfig = defaultJsonMapper.getDeserializationConfig();

    private static final SerializationConfig defaultSerializationConfigForCopy;
    private static final SerializationFeature serializationFeatureNotEnabledByDefault;
    private static final DeserializationConfig defaultDeserializationConfigForCopy;
    private static final DeserializationFeature deserializationFeatureNotEnabledByDefault;

    static {
        {
            SerializationFeature tmp = null;
            for (final SerializationFeature serializationFeature : SerializationFeature.values()) {
                if (!defaultSerializationConfig.isEnabled(serializationFeature)) {
                    tmp = serializationFeature;
                    break;
                }
            }

            serializationFeatureNotEnabledByDefault = tmp;
            defaultSerializationConfigForCopy = defaultSerializationConfig.with(serializationFeatureNotEnabledByDefault);
        }

        {
            DeserializationFeature tmp = null;
            for (final DeserializationFeature deserializationFeature : DeserializationFeature.values()) {
                if (!defaultDeserializationConfig.isEnabled(deserializationFeature)) {
                    tmp = deserializationFeature;
                    break;
                }
            }

            deserializationFeatureNotEnabledByDefault = tmp;
            defaultDeserializationConfigForCopy = defaultDeserializationConfig.with(deserializationFeatureNotEnabledByDefault);
        }
    }

    private JsonMappers() {
        // singleton for utility class.
    }

    /**
     * Serializes the specified object to a JSON string using default configuration.
     * This method provides the simplest way to convert a Java object to JSON format.
     * 
     * <p>The serialization uses Jackson's default configuration settings. For custom
     * serialization behavior, use the overloaded methods that accept configuration parameters.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = JsonMappers.toJson(person);
     * // Result: {"name":"John","age":30}
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @return a JSON string representation of the object
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see #toJson(Object, boolean)
     * @see #toJson(Object, SerializationFeature, SerializationFeature...)
     */
    public static String toJson(final Object obj) {
        try {
            return defaultJsonMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to a JSON string with optional pretty formatting.
     * Pretty formatting adds line breaks and indentation to make the JSON human-readable.
     * 
     * <p>When pretty format is enabled, the output includes proper indentation and line breaks.
     * This is useful for debugging, logging, or generating human-readable configuration files.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = JsonMappers.toJson(person, true);
     * // Result:
     * // {
     * //   "name" : "John",
     * //   "age" : 30
     * // }
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param prettyFormat if true, the output will be formatted with indentation and line breaks;
     *                     if false, output will be compact (single line)
     * @return a JSON string representation of the object
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see #toJson(Object)
     */
    public static String toJson(final Object obj, final boolean prettyFormat) {
        try {
            if (prettyFormat) {
                return defaultJsonMapperForPretty.writeValueAsString(obj);
            } else {
                return defaultJsonMapper.writeValueAsString(obj);
            }
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to a JSON string with custom serialization features.
     * This method allows fine-grained control over the serialization process by enabling
     * specific Jackson features.
     * 
     * <p>Common serialization features include:</p>
     * <ul>
     *   <li>WRITE_DATES_AS_TIMESTAMPS - Write dates as numeric timestamps</li>
     *   <li>WRITE_ENUMS_USING_INDEX - Write enums using their ordinal values</li>
     *   <li>INDENT_OUTPUT - Pretty print the output</li>
     *   <li>WRITE_NULL_MAP_VALUES - Include null values in maps</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Serialize with multiple features
     * String json = JsonMappers.toJson(myObject, 
     *     SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
     *     SerializationFeature.WRITE_ENUMS_USING_INDEX);
     * 
     * // Serialize with pretty formatting
     * String prettyJson = JsonMappers.toJson(myObject,
     *     SerializationFeature.INDENT_OUTPUT);
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param first the first serialization feature to apply (required to ensure at least one feature)
     * @param features additional serialization features to apply (optional)
     * @return a JSON string representation of the object
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see SerializationFeature
     * @see #toJson(Object, SerializationConfig)
     */
    public static String toJson(final Object obj, final SerializationFeature first, final SerializationFeature... features) {
        return toJson(obj, defaultSerializationConfig.with(first, features));
    }

    /**
     * Serializes the specified object to a JSON string using a custom serialization configuration.
     * This method provides maximum flexibility by accepting a complete SerializationConfig object.
     * 
     * <p>Use this method when you need complex configuration that cannot be achieved with
     * individual features, such as custom serializers, date formats, or visibility settings.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Create custom configuration
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT)
     *     .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     * 
     * // Serialize with custom config
     * String json = JsonMappers.toJson(myObject, config);
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param config the custom serialization configuration to use; if null, uses default configuration
     * @return a JSON string representation of the object
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see #createSerializationConfig()
     * @see SerializationConfig
     */
    public static String toJson(final Object obj, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to a file.
     * The file is created if it doesn't exist, or overwritten if it does.
     * 
     * <p>This method handles all I/O operations internally, creating parent directories
     * if necessary. The character encoding used is UTF-8 by default.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * File outputFile = new File("data/person.json");
     * JsonMappers.toJson(person, outputFile);
     * // File now contains: {"name":"John","age":30}
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the file to write the JSON to; parent directories will be created if needed
     * @throws RuntimeException if serialization fails or file cannot be written
     * @see #toJson(Object, File, SerializationConfig)
     */
    public static void toJson(final Object obj, final File output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to a file using custom configuration.
     * This method combines file output with custom serialization settings.
     * 
     * <p>Use this method when you need to write formatted JSON to a file or apply
     * specific serialization rules for file-based output.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT);
     * 
     * JsonMappers.toJson(myData, new File("formatted-data.json"), config);
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the file to write the JSON to; parent directories will be created if needed
     * @param config the custom serialization configuration to use; if null, uses default configuration
     * @throws RuntimeException if serialization fails or file cannot be written
     * @see #toJson(Object, File)
     * @see SerializationConfig
     */
    public static void toJson(final Object obj, final File output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to an output stream.
     * The stream is not closed by this method, allowing for additional operations.
     * 
     * <p>This method is useful for writing JSON to network streams, HTTP responses,
     * or any other output stream. The caller is responsible for closing the stream.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("data.json")) {
     *     JsonMappers.toJson(myObject, fos);
     * }
     * 
     * // Writing to HTTP response
     * JsonMappers.toJson(responseData, httpServletResponse.getOutputStream());
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the output stream to write the JSON to; not closed by this method
     * @throws RuntimeException if serialization fails or writing to stream fails
     * @see #toJson(Object, OutputStream, SerializationConfig)
     */
    public static void toJson(final Object obj, final OutputStream output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to an output stream using custom configuration.
     * This method combines stream output with custom serialization settings.
     * 
     * <p>The stream is not closed by this method. Use this when you need specific
     * serialization behavior for stream-based output, such as custom date formats
     * or pretty printing for HTTP responses.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT)
     *     .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     * 
     * JsonMappers.toJson(apiResponse, response.getOutputStream(), config);
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the output stream to write the JSON to; not closed by this method
     * @param config the custom serialization configuration to use; if null, uses default configuration
     * @throws RuntimeException if serialization fails or writing to stream fails
     * @see #toJson(Object, OutputStream)
     * @see SerializationConfig
     */
    public static void toJson(final Object obj, final OutputStream output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to a Writer.
     * The writer is not closed by this method, allowing for additional operations.
     * 
     * <p>This method is ideal for writing JSON to character-based outputs such as
     * StringWriter, FileWriter, or any custom Writer implementation. The character
     * encoding is handled by the Writer itself.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Write to file with specific encoding
     * try (OutputStreamWriter writer = new OutputStreamWriter(
     *         new FileOutputStream("data.json"), StandardCharsets.UTF_8)) {
     *     JsonMappers.toJson(myObject, writer);
     * }
     * 
     * // Write to StringWriter for further processing
     * StringWriter sw = new StringWriter();
     * JsonMappers.toJson(myObject, sw);
     * String json = sw.toString();
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the writer to write the JSON to; not closed by this method
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, Writer, SerializationConfig)
     */
    public static void toJson(final Object obj, final Writer output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to a Writer using custom configuration.
     * This method combines writer output with custom serialization settings.
     * 
     * <p>Use this method when you need specific serialization behavior for character-based
     * output, such as pretty printing to a log file or custom formatting for templates.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT);
     * 
     * try (FileWriter writer = new FileWriter("formatted.json")) {
     *     JsonMappers.toJson(myData, writer, config);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the writer to write the JSON to; not closed by this method
     * @param config the custom serialization configuration to use; if null, uses default configuration
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, Writer)
     * @see SerializationConfig
     */
    public static void toJson(final Object obj, final Writer output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to a DataOutput.
     * This method is useful for writing JSON in binary protocols or custom serialization formats.
     * 
     * <p>DataOutput is typically used in scenarios involving RandomAccessFile,
     * DataOutputStream, or custom binary protocols that need to embed JSON data.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (DataOutputStream dos = new DataOutputStream(
     *         new FileOutputStream("data.bin"))) {
     *     // Write some binary data
     *     dos.writeInt(42);
     *     // Write JSON data
     *     JsonMappers.toJson(myObject, dos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the DataOutput to write the JSON to
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, DataOutput, SerializationConfig)
     * @see DataOutput
     */
    public static void toJson(final Object obj, final DataOutput output) {
        try {
            defaultJsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to a DataOutput using custom configuration.
     * This method combines DataOutput with custom serialization settings.
     * 
     * <p>Use this method when embedding JSON in binary formats with specific
     * serialization requirements, such as compact format without whitespace.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .without(SerializationFeature.INDENT_OUTPUT);
     * 
     * try (RandomAccessFile raf = new RandomAccessFile("data.bin", "rw")) {
     *     JsonMappers.toJson(myObject, raf, config);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; can be null (produces "null")
     * @param output the DataOutput to write the JSON to
     * @param config the custom serialization configuration to use; if null, uses default configuration
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, DataOutput)
     * @see SerializationConfig
     * @see DataOutput
     */
    public static void toJson(final Object obj, final DataOutput output, final SerializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            jsonMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a byte array to an object of the specified type.
     * This method assumes the byte array contains UTF-8 encoded JSON data.
     * 
     * <p>Use this method when working with JSON data from network protocols,
     * file systems, or any binary source. The entire byte array is processed.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * byte[] jsonBytes = "{\"name\":\"John\",\"age\":30}".getBytes(StandardCharsets.UTF_8);
     * Person person = JsonMappers.fromJson(jsonBytes, Person.class);
     * 
     * // From network
     * byte[] responseBytes = httpResponse.getBody();
     * ApiResponse response = JsonMappers.fromJson(responseBytes, ApiResponse.class);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a UTF-8 encoded byte array
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; never null unless JSON contains "null"
     * @throws RuntimeException if deserialization fails due to invalid JSON or type mismatch
     * @see #fromJson(byte[], int, int, Class)
     * @see #fromJson(byte[], TypeReference)
     */
    public static <T> T fromJson(final byte[] json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a portion of a byte array to an object of the specified type.
     * This method is useful when the JSON data is embedded within a larger byte array.
     * 
     * <p>The method processes only the specified portion of the byte array,
     * starting at the given offset and reading the specified number of bytes.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Buffer contains multiple JSON objects
     * byte[] buffer = loadBuffer();
     * int jsonStart = 100;
     * int jsonLength = 250;
     * 
     * Person person = JsonMappers.fromJson(buffer, jsonStart, jsonLength, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the byte array containing JSON content
     * @param offset the offset in the array where JSON data starts
     * @param len the number of bytes to read from the offset
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; never null unless JSON contains "null"
     * @throws RuntimeException if deserialization fails or array bounds are invalid
     * @throws IndexOutOfBoundsException if offset or length are invalid
     * @see #fromJson(byte[], Class)
     */
    public static <T> T fromJson(final byte[] json, final int offset, final int len, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a string to an object of the specified type.
     * This is the most commonly used deserialization method.
     * 
     * <p>The method handles all standard JSON types including objects, arrays,
     * primitives, and null values. For complex generic types, use the TypeReference
     * overload instead.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Simple object
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = JsonMappers.fromJson(json, Person.class);
     * 
     * // Array
     * String jsonArray = "[1,2,3,4,5]";
     * Integer[] numbers = JsonMappers.fromJson(jsonArray, Integer[].class);
     * 
     * // Handling null
     * String nullJson = "null";
     * Person nullPerson = JsonMappers.fromJson(nullJson, Person.class); // returns null
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; null if JSON string is "null"
     * @throws RuntimeException if deserialization fails due to invalid JSON or type mismatch
     * @see #fromJson(String, TypeReference)
     * @see #fromJson(String, Class, DeserializationFeature, DeserializationFeature...)
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a string to an object with custom deserialization features.
     * This method allows fine-grained control over the deserialization process.
     * 
     * <p>Common deserialization features include:</p>
     * <ul>
     *   <li>FAIL_ON_UNKNOWN_PROPERTIES - Fail if JSON contains unknown properties</li>
     *   <li>USE_BIG_DECIMAL_FOR_FLOATS - Use BigDecimal for floating point numbers</li>
     *   <li>ACCEPT_SINGLE_VALUE_AS_ARRAY - Accept single values as arrays</li>
     *   <li>READ_UNKNOWN_ENUM_VALUES_AS_NULL - Convert unknown enum values to null</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Ignore unknown properties and use BigDecimal for floats
     * String json = "{\"name\":\"John\",\"age\":30,\"unknown\":\"field\"}";
     * Person person = JsonMappers.fromJson(json, Person.class,
     *     DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * 
     * // Accept single value as array
     * String singleValue = "\"value\"";
     * List<String> list = JsonMappers.fromJson(singleValue, List.class,
     *     DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType the class of the object to deserialize to
     * @param first the first deserialization feature to apply (required)
     * @param features additional deserialization features to apply (optional)
     * @return the deserialized object; null if JSON string is "null"
     * @throws RuntimeException if deserialization fails
     * @see DeserializationFeature
     * @see #fromJson(String, Class, DeserializationConfig)
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromJson(json, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     * Deserializes JSON from a string using a custom deserialization configuration.
     * This method provides maximum control over the deserialization process.
     * 
     * <p>Use this method when you need complex configuration that cannot be achieved
     * with individual features, such as custom deserializers, date formats, or
     * visibility settings.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Create custom configuration
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
     *     .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * 
     * // Deserialize with custom config
     * Person person = JsonMappers.fromJson(json, Person.class, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType the class of the object to deserialize to
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON string is "null"
     * @throws RuntimeException if deserialization fails
     * @see #createDeserializationConfig()
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a file to an object of the specified type.
     * This method reads the entire file content and parses it as JSON.
     * 
     * <p>The file is expected to contain valid JSON data encoded in UTF-8.
     * The method handles all file I/O operations internally.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Read configuration from file
     * File configFile = new File("config/app-settings.json");
     * AppConfig config = JsonMappers.fromJson(configFile, AppConfig.class);
     * 
     * // Read data file
     * File dataFile = new File("data/users.json");
     * List<User> users = JsonMappers.fromJson(dataFile, 
     *     new TypeReference<List<User>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the file containing JSON content
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see #fromJson(File, Class, DeserializationConfig)
     * @see #fromJson(File, TypeReference)
     */
    public static <T> T fromJson(final File json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a file using custom deserialization configuration.
     * This method combines file input with custom deserialization settings.
     * 
     * <p>Use this method when reading JSON files that require special handling,
     * such as lenient parsing or custom date formats.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * 
     * // Read config file with unknown properties
     * AppConfig appConfig = JsonMappers.fromJson(
     *     new File("config.json"), AppConfig.class, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the file containing JSON content
     * @param targetType the class of the object to deserialize to
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see #fromJson(File, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final File json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from an input stream to an object of the specified type.
     * The stream is not closed by this method, allowing for additional operations.
     * 
     * <p>This method is ideal for reading JSON from network connections, file streams,
     * or any other input source. The caller is responsible for closing the stream.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Read from file
     * try (FileInputStream fis = new FileInputStream("data.json")) {
     *     Person person = JsonMappers.fromJson(fis, Person.class);
     * }
     * 
     * // Read from HTTP response
     * try (InputStream is = httpConnection.getInputStream()) {
     *     ApiResponse response = JsonMappers.fromJson(is, ApiResponse.class);
     * }
     * 
     * // Read from classpath resource
     * try (InputStream is = getClass().getResourceAsStream("/config.json")) {
     *     Config config = JsonMappers.fromJson(is, Config.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the input stream containing JSON content
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(InputStream, Class, DeserializationConfig)
     * @see #fromJson(InputStream, TypeReference)
     */
    public static <T> T fromJson(final InputStream json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from an input stream using custom deserialization configuration.
     * This method combines stream input with custom deserialization settings.
     * 
     * <p>The stream is not closed by this method. Use this when reading JSON from
     * streams that require special handling, such as lenient parsing for external APIs.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
     *     .with(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
     * 
     * try (InputStream is = externalApi.getDataStream()) {
     *     ApiData data = JsonMappers.fromJson(is, ApiData.class, config);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the input stream containing JSON content
     * @param targetType the class of the object to deserialize to
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(InputStream, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final InputStream json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a Reader to an object of the specified type.
     * The reader is not closed by this method, allowing for additional operations.
     * 
     * <p>This method is useful when you need to control the character encoding
     * or when working with character-based input sources. The Reader handles
     * the character encoding.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Read with specific encoding
     * try (InputStreamReader reader = new InputStreamReader(
     *         new FileInputStream("data.json"), StandardCharsets.UTF_8)) {
     *     Person person = JsonMappers.fromJson(reader, Person.class);
     * }
     * 
     * // Read from StringReader
     * String jsonString = "{\"name\":\"John\",\"age\":30}";
     * try (StringReader reader = new StringReader(jsonString)) {
     *     Person person = JsonMappers.fromJson(reader, Person.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the reader containing JSON content
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(Reader, Class, DeserializationConfig)
     * @see #fromJson(Reader, TypeReference)
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a Reader using custom deserialization configuration.
     * This method combines reader input with custom deserialization settings.
     * 
     * <p>The reader is not closed by this method. Use this when reading JSON from
     * character sources that require special handling.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * 
     * try (FileReader reader = new FileReader("financial-data.json")) {
     *     FinancialReport report = JsonMappers.fromJson(
     *         reader, FinancialReport.class, config);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the reader containing JSON content
     * @param targetType the class of the object to deserialize to
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(Reader, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a URL to an object of the specified type.
     * This method fetches JSON content from the specified URL and parses it.
     * 
     * <p>The method handles all network operations internally, including opening
     * the connection and reading the response. It's suitable for REST APIs and
     * web services that return JSON data.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Fetch user data from API
     * URL apiUrl = new URL("https://api.example.com/users/123");
     * User user = JsonMappers.fromJson(apiUrl, User.class);
     * 
     * // Load configuration from web
     * URL configUrl = new URL("https://config.example.com/app-config.json");
     * AppConfig config = JsonMappers.fromJson(configUrl, AppConfig.class);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the URL pointing to JSON content
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if network access fails or JSON is invalid
     * @see #fromJson(URL, Class, DeserializationConfig)
     * @see #fromJson(URL, TypeReference)
     */
    public static <T> T fromJson(final URL json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a URL using custom deserialization configuration.
     * This method combines URL input with custom deserialization settings.
     * 
     * <p>Use this method when fetching JSON from URLs that require special handling,
     * such as APIs that may include unknown properties or use non-standard formats.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * 
     * URL apiUrl = new URL("https://external-api.com/data");
     * ExternalData data = JsonMappers.fromJson(apiUrl, ExternalData.class, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the URL pointing to JSON content
     * @param targetType the class of the object to deserialize to
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if network access fails or JSON is invalid
     * @see #fromJson(URL, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final URL json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a DataInput to an object of the specified type.
     * This method is useful for reading JSON from binary protocols or custom formats.
     * 
     * <p>DataInput is typically used with RandomAccessFile, DataInputStream,
     * or custom binary protocols that embed JSON data.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * try (DataInputStream dis = new DataInputStream(
     *         new FileInputStream("data.bin"))) {
     *     // Read some binary data
     *     int version = dis.readInt();
     *     // Read JSON data
     *     Config config = JsonMappers.fromJson(dis, Config.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the DataInput containing JSON content
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(DataInput, Class, DeserializationConfig)
     * @see #fromJson(DataInput, TypeReference)
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final Class<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a DataInput using custom deserialization configuration.
     * This method combines DataInput with custom deserialization settings.
     * 
     * <p>Use this method when reading JSON from binary formats that require
     * special deserialization handling.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * 
     * try (RandomAccessFile raf = new RandomAccessFile("data.bin", "r")) {
     *     raf.seek(jsonOffset);
     *     FinancialData data = JsonMappers.fromJson(raf, FinancialData.class, config);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the DataInput containing JSON content
     * @param targetType the class of the object to deserialize to
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(DataInput, Class)
     * @see DeserializationConfig
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final Class<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a byte array to an object using TypeReference for generic types.
     * This method is essential for deserializing complex generic types like collections and maps.
     * 
     * <p>TypeReference captures the full generic type information at compile time,
     * allowing proper deserialization of parameterized types that would otherwise
     * be lost due to type erasure.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Deserialize a List of objects
     * byte[] jsonBytes = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]".getBytes();
     * List<Person> people = JsonMappers.fromJson(jsonBytes, 
     *     new TypeReference<List<Person>>() {});
     * 
     * // Deserialize a Map
     * byte[] mapBytes = "{\"key1\":\"value1\",\"key2\":\"value2\"}".getBytes();
     * Map<String, String> map = JsonMappers.fromJson(mapBytes,
     *     new TypeReference<Map<String, String>>() {});
     * 
     * // Deserialize nested generics
     * List<Map<String, Person>> complex = JsonMappers.fromJson(complexBytes,
     *     new TypeReference<List<Map<String, Person>>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a UTF-8 encoded byte array
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if deserialization fails or type doesn't match
     * @see TypeReference
     * @see #fromJson(byte[], Class)
     */
    public static <T> T fromJson(final byte[] json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a portion of a byte array using TypeReference for generic types.
     * This method combines partial array reading with generic type support.
     * 
     * <p>Use this method when working with generic types in byte buffers where
     * the JSON data is embedded within a larger array.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Buffer contains multiple JSON objects
     * byte[] buffer = loadBuffer();
     * int listStart = 100;
     * int listLength = 500;
     * 
     * List<Product> products = JsonMappers.fromJson(buffer, listStart, listLength,
     *     new TypeReference<List<Product>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the byte array containing JSON content
     * @param offset the offset in the array where JSON data starts
     * @param len the number of bytes to read from the offset
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if deserialization fails or bounds are invalid
     * @throws IndexOutOfBoundsException if offset or length are invalid
     * @see TypeReference
     * @see #fromJson(byte[], int, int, Class)
     */
    public static <T> T fromJson(final byte[] json, final int offset, final int len, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a string to an object using TypeReference for generic types.
     * This is the most commonly used method for deserializing generic types.
     * 
     * <p>TypeReference preserves full generic type information, enabling proper
     * deserialization of collections, maps, and other parameterized types.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Deserialize a List
     * String jsonArray = "[{\"id\":1,\"name\":\"Item1\"},{\"id\":2,\"name\":\"Item2\"}]";
     * List<Item> items = JsonMappers.fromJson(jsonArray,
     *     new TypeReference<List<Item>>() {});
     * 
     * // Deserialize a Map
     * String jsonMap = "{\"user1\":{\"name\":\"John\"},\"user2\":{\"name\":\"Jane\"}}";
     * Map<String, User> users = JsonMappers.fromJson(jsonMap,
     *     new TypeReference<Map<String, User>>() {});
     * 
     * // Deserialize complex nested types
     * Map<String, List<Order>> ordersByUser = JsonMappers.fromJson(complexJson,
     *     new TypeReference<Map<String, List<Order>>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if deserialization fails or JSON is invalid
     * @see TypeReference
     * @see #fromJson(String, Class)
     * @see #fromJson(String, TypeReference, DeserializationFeature, DeserializationFeature...)
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a string using TypeReference with custom deserialization features.
     * This method combines generic type support with custom deserialization control.
     * 
     * <p>Use this method when deserializing generic types that require special
     * handling, such as collections that should accept single values as arrays.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Accept single value as array for List
     * String singleValue = "\"single-item\"";
     * List<String> list = JsonMappers.fromJson(singleValue,
     *     new TypeReference<List<String>>() {},
     *     DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
     * 
     * // Use BigDecimal for financial data
     * List<FinancialRecord> records = JsonMappers.fromJson(jsonData,
     *     new TypeReference<List<FinancialRecord>>() {},
     *     DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType TypeReference capturing the generic type information
     * @param first the first deserialization feature to apply (required)
     * @param features additional deserialization features to apply (optional)
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if deserialization fails
     * @see TypeReference
     * @see DeserializationFeature
     * @see #fromJson(String, TypeReference, DeserializationConfig)
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromJson(json, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     * Deserializes JSON from a string using TypeReference with custom configuration.
     * This method provides maximum flexibility for deserializing generic types.
     * 
     * <p>Use this method when you need complex configuration for generic types,
     * such as custom deserializers for collection elements or special date handling
     * in maps.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Create custom configuration
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
     *     .with(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL);
     * 
     * // Deserialize with custom config
     * Map<String, List<Event>> eventMap = JsonMappers.fromJson(json,
     *     new TypeReference<Map<String, List<Event>>>() {}, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType TypeReference capturing the generic type information
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if deserialization fails
     * @see TypeReference
     * @see DeserializationConfig
     * @see #createDeserializationConfig()
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a file using TypeReference for generic types.
     * This method enables reading generic types from JSON files.
     * 
     * <p>The file is expected to contain valid JSON data encoded in UTF-8.
     * This method is particularly useful for loading collections or maps from
     * configuration or data files.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Load list of users from file
     * File usersFile = new File("data/users.json");
     * List<User> users = JsonMappers.fromJson(usersFile,
     *     new TypeReference<List<User>>() {});
     * 
     * // Load configuration map
     * File configFile = new File("config/settings.json");
     * Map<String, ConfigValue> settings = JsonMappers.fromJson(configFile,
     *     new TypeReference<Map<String, ConfigValue>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the file containing JSON content
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see TypeReference
     * @see #fromJson(File, Class)
     * @see #fromJson(File, TypeReference, DeserializationConfig)
     */
    public static <T> T fromJson(final File json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a file using TypeReference with custom configuration.
     * This method combines file input with generic type support and custom settings.
     * 
     * <p>Use this method when reading generic types from files that require
     * special deserialization handling, such as legacy data formats.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * 
     * // Read legacy data with extra fields
     * List<LegacyRecord> records = JsonMappers.fromJson(
     *     new File("legacy-data.json"),
     *     new TypeReference<List<LegacyRecord>>() {},
     *     config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the file containing JSON content
     * @param targetType TypeReference capturing the generic type information
     * @param config the custom deserialization configuration; if null, uses default
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see TypeReference
     * @see DeserializationConfig
     * @see #fromJson(File, TypeReference)
     */
    public static <T> T fromJson(final File json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from an input stream using TypeReference for generic types.
     * The stream is not closed by this method.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream.</p>
     *
     * <p>Example:</p>
     * <pre>{@code
     * // Read list from file stream
     * try (FileInputStream fis = new FileInputStream("items.json")) {
     *     List<Item> items = JsonMappers.fromJson(fis,
     *         new TypeReference<List<Item>>() {});
     * }
     * 
     * // Read map from classpath resource
     * try (InputStream is = getClass().getResourceAsStream("/data.json")) {
     *     Map<String, Object> data = JsonMappers.fromJson(is,
     *         new TypeReference<Map<String, Object>>() {});
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the input stream containing JSON content
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; null if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see TypeReference
     * @see #fromJson(InputStream, Class)
     * @see #fromJson(InputStream, TypeReference, DeserializationConfig)
     */
    public static <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from an InputStream into a Java object of the specified generic type with custom deserialization configuration.
     * This method allows fine-grained control over the deserialization process through a custom DeserializationConfig.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream.</p>
     * 
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Read a list of users with custom configuration
     * FileInputStream fis = new FileInputStream("users.json");
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * List<User> users = JsonMappers.fromJson(fis, 
     *     new TypeReference<List<User>>() {}, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the InputStream containing JSON data to deserialize
     * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map types
     * @param config custom deserialization configuration, if null uses default configuration
     * @return the deserialized object of type T
     * @throws RuntimeException wrapping any IOException that occurs during deserialization
     * @see TypeReference
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a Reader into a Java object of the specified generic type using default configuration.
     * This method is suitable for reading JSON from character-based sources like StringReader or FileReader.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream/Reader.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Read JSON from a StringReader
     * StringReader reader = new StringReader("[{\"name\":\"John\"},{\"name\":\"Jane\"}]");
     * List<Person> people = JsonMappers.fromJson(reader, 
     *     new TypeReference<List<Person>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the Reader containing JSON data to deserialize
     * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map types
     * @return the deserialized object of type T
     * @throws RuntimeException wrapping any IOException that occurs during deserialization
     * @see TypeReference
     */
    public static <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a Reader into a Java object of the specified generic type with custom deserialization configuration.
     * This method combines the flexibility of Reader-based input with custom configuration control.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream/Reader.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Read JSON with lenient parsing configuration
     * FileReader reader = new FileReader("data.json");
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * Map<String, Object> data = JsonMappers.fromJson(reader,
     *     new TypeReference<Map<String, Object>>() {}, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the Reader containing JSON data to deserialize
     * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map types
     * @param config custom deserialization configuration, if null uses default configuration
     * @return the deserialized object of type T
     * @throws RuntimeException wrapping any IOException that occurs during deserialization
     * @see TypeReference
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a URL into a Java object of the specified generic type using default configuration.
     * This method fetches JSON content from the specified URL and deserializes it.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream/Reader.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Fetch and deserialize JSON from a web API
     * URL apiUrl = new URL("https://api.example.com/users");
     * List<User> users = JsonMappers.fromJson(apiUrl, 
     *     new TypeReference<List<User>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the URL pointing to JSON data to deserialize
     * @param targetType TypeReference describing the target type
     * @return the deserialized object of type T
     * @throws RuntimeException wrapping any IOException that occurs during network access or deserialization
     * @see TypeReference
     */
    public static <T> T fromJson(final URL json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a URL into a Java object of the specified generic type with custom deserialization configuration.
     * This method provides control over the deserialization process when fetching JSON from URLs.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream/Reader.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Fetch JSON with custom date handling
     * URL url = new URL("https://api.example.com/events");
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
     * List<Event> events = JsonMappers.fromJson(url,
     *     new TypeReference<List<Event>>() {}, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the URL pointing to JSON data to deserialize
     * @param targetType TypeReference describing the target type
     * @param config custom deserialization configuration, if null uses default configuration
     * @return the deserialized object of type T
     * @throws RuntimeException wrapping any IOException that occurs during network access or deserialization
     * @see TypeReference
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final URL json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a DataInput into a Java object of the specified generic type using default configuration.
     * This method is useful for reading JSON from custom binary protocols or specialized I/O streams.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream/Reader.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Read JSON from a DataInputStream
     * DataInputStream dis = new DataInputStream(inputStream);
     * Product product = JsonMappers.fromJson(dis, 
     *     new TypeReference<Product>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the DataInput containing JSON data to deserialize
     * @param targetType TypeReference describing the target type
     * @return the deserialized object of type T
     * @throws RuntimeException wrapping any IOException that occurs during deserialization
     * @see TypeReference
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType) {
        try {
            return defaultJsonMapper.readValue(json, defaultJsonMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a DataInput into a Java object of the specified generic type with custom deserialization configuration.
     * This method combines DataInput flexibility with custom configuration control.
     * 
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or resources. The caller is responsible
     * for closing the stream/Reader.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Read JSON with custom configuration from binary protocol
     * DataInputStream dis = new DataInputStream(socket.getInputStream());
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * Order order = JsonMappers.fromJson(dis,
     *     new TypeReference<Order>() {}, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the DataInput containing JSON data to deserialize
     * @param targetType TypeReference describing the target type
     * @param config custom deserialization configuration, if null uses default configuration
     * @return the deserialized object of type T
     * @throws RuntimeException wrapping any IOException that occurs during deserialization
     * @see TypeReference
     * @see DeserializationConfig
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final JsonMapper jsonMapper = getJsonMapper(config);

        try {
            return jsonMapper.readValue(json, jsonMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Creates a new SerializationConfig instance with default settings.
     * This method provides a base configuration that can be customized for specific serialization needs.
     * The returned configuration is a copy and can be modified without affecting other operations.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Create custom serialization config
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT)
     *     .with(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     * }</pre>
     *
     * @return a new SerializationConfig instance with default settings
     * @see SerializationConfig
     * @see SerializationFeature
     */
    public static SerializationConfig createSerializationConfig() {
        // final SerializationConfig copy = defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);

        return defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);
    }

    /**
     * Creates a new DeserializationConfig instance with default settings.
     * This method provides a base configuration that can be customized for specific deserialization needs.
     * The returned configuration is a copy and can be modified without affecting other operations.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Create custom deserialization config
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
     *     .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
     * }</pre>
     *
     * @return a new DeserializationConfig instance with default settings
     * @see DeserializationConfig
     * @see DeserializationFeature
     */
    public static DeserializationConfig createDeserializationConfig() {
        // final DeserializationConfig copy = defaultDeserializationConfigForCopy.without(deserializationFeatureNotEnabledByDefault);

        return defaultDeserializationConfigForCopy.without(deserializationFeatureNotEnabledByDefault);
    }

    //    /**
    //     *
    //     * @param setter first parameter is the copy of default {@code SerializationConfig}
    //     * @return
    //     */
    //    public static SerializationConfig createSerializationConfig(final Function<? super SerializationConfig, ? extends SerializationConfig> setter) {
    //        return setter.apply(createSerializationConfig());
    //    }
    //
    //    /**
    //     *
    //     * @param setter first parameter is the copy of default {@code DeserializationConfig}
    //     * @return
    //     */
    //    public static DeserializationConfig createDeserializationConfig(final Function<? super DeserializationConfig, ? extends DeserializationConfig> setter) {
    //        return setter.apply(createDeserializationConfig());
    //    }

    static JsonMapper getJsonMapper(final SerializationConfig config) {
        if (config == null) {
            return defaultJsonMapper;
        }

        JsonMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new JsonMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    static JsonMapper getJsonMapper(final DeserializationConfig config) {
        if (config == null) {
            return defaultJsonMapper;
        }

        JsonMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new JsonMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    static void recycle(final JsonMapper mapper) {
        if (mapper == null) {
            return;
        }

        mapper.setConfig(defaultSerializationConfig);
        mapper.setConfig(defaultDeserializationConfig);

        synchronized (mapperPool) {
            if (mapperPool.size() < POOL_SIZE) {

                mapperPool.add(mapper);
            }
        }
    }

    /**
     * Wraps a Jackson ObjectMapper instance to provide convenient JSON operations through the One wrapper class.
     * This method allows using a custom ObjectMapper with specific configurations while benefiting from
     * the simplified API provided by the One wrapper.
     * 
     * <p>The wrapped mapper will be used for all JSON operations performed through the returned One instance.
     * A separate mapper instance with pretty printing enabled is automatically created for formatted output.</p>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * // Use custom ObjectMapper with specific modules
     * ObjectMapper customMapper = new ObjectMapper();
     * customMapper.registerModule(new JavaTimeModule());
     * customMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     * 
     * JsonMappers.One jsonOps = JsonMappers.wrap(customMapper);
     * String json = jsonOps.toJson(myObject);
     * MyObject obj = jsonOps.fromJson(json, MyObject.class);
     * }</pre>
     *
     * @param jsonMapper the ObjectMapper instance to wrap
     * @return a One instance wrapping the provided ObjectMapper
     * @see One
     * @see ObjectMapper
     */
    public static One wrap(final ObjectMapper jsonMapper) {
        return new One(jsonMapper);
    }

    /**
     * A wrapper class that provides convenient JSON serialization and deserialization methods using a specific ObjectMapper instance.
     * This class encapsulates a configured ObjectMapper and provides a simplified API for common JSON operations
     * with support for pretty printing and various input/output sources.
     * 
     * <p>Key features:</p>
     * <ul>
     *   <li>Encapsulates a specific ObjectMapper configuration</li>
     *   <li>Automatic pretty printing support with a separate mapper instance</li>
     *   <li>Consistent exception handling (wraps checked exceptions as RuntimeException)</li>
     *   <li>Support for multiple input/output formats</li>
     * </ul>
     * 
     * <p>This class is typically obtained through {@link JsonMappers#wrap(ObjectMapper)} rather than
     * instantiated directly.</p>
     * 
     * @see JsonMappers#wrap(ObjectMapper)
     */
    public static final class One {

        private final ObjectMapper jsonMapper;
        private final ObjectMapper jsonMapperForPretty;

        One(final ObjectMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
            jsonMapperForPretty = jsonMapper.copy();

            jsonMapperForPretty.enable(SerializationFeature.INDENT_OUTPUT);
        }

        /**
         * Serializes a Java object to its JSON string representation using the wrapped ObjectMapper.
         * This method provides the most common use case for JSON serialization.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Person person = new Person("John", 30);
         * String json = jsonOps.toJson(person);
         * // Result: {"name":"John","age":30}
         * }</pre>
         *
         * @param obj the object to serialize to JSON
         * @return JSON string representation of the object
         * @throws RuntimeException wrapping any JsonProcessingException that occurs during serialization
         */
        public String toJson(final Object obj) {
            try {
                return jsonMapper.writeValueAsString(obj);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes a Java object to its JSON string representation with optional pretty formatting.
         * When pretty format is enabled, the output includes indentation and line breaks for readability.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Map<String, Object> data = new HashMap<>();
         * data.put("name", "John");
         * data.put("age", 30);
         * 
         * String json = jsonOps.toJson(data, true);
         * // Result:
         * // {
         * //   "name" : "John",
         * //   "age" : 30
         * // }
         * }</pre>
         *
         * @param obj the object to serialize to JSON
         * @param prettyFormat if true, formats the JSON with indentation and line breaks
         * @return JSON string representation of the object, optionally formatted
         * @throws RuntimeException wrapping any JsonProcessingException that occurs during serialization
         */
        public String toJson(final Object obj, final boolean prettyFormat) {
            try {
                if (prettyFormat) {
                    return jsonMapperForPretty.writeValueAsString(obj);
                } else {
                    return jsonMapper.writeValueAsString(obj);
                }
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes a Java object to JSON and writes it to the specified file.
         * The file is created if it doesn't exist, or overwritten if it does.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * List<User> users = getUserList();
         * File outputFile = new File("users.json");
         * jsonOps.toJson(users, outputFile);
         * }</pre>
         *
         * @param obj the object to serialize to JSON
         * @param output the file to write the JSON to
         * @throws RuntimeException wrapping any IOException that occurs during file writing
         */
        public void toJson(final Object obj, final File output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes a Java object to JSON and writes it to the specified OutputStream.
         * The stream is not closed by this method, allowing for further operations.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Product product = new Product("Widget", 29.99);
         * try (FileOutputStream fos = new FileOutputStream("product.json")) {
         *     jsonOps.toJson(product, fos);
         * }
         * }</pre>
         *
         * @param obj the object to serialize to JSON
         * @param output the OutputStream to write the JSON to
         * @throws RuntimeException wrapping any IOException that occurs during writing
         */
        public void toJson(final Object obj, final OutputStream output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes a Java object to JSON and writes it to the specified Writer.
         * This method is useful for character-based output destinations.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Order order = new Order(12345, "Processing");
         * try (StringWriter writer = new StringWriter()) {
         *     jsonOps.toJson(order, writer);
         *     String json = writer.toString();
         * }
         * }</pre>
         *
         * @param obj the object to serialize to JSON
         * @param output the Writer to write the JSON to
         * @throws RuntimeException wrapping any IOException that occurs during writing
         */
        public void toJson(final Object obj, final Writer output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes a Java object to JSON and writes it to the specified DataOutput.
         * This method is useful for binary protocols or custom I/O implementations.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * Message message = new Message("Hello", System.currentTimeMillis());
         * DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
         * jsonOps.toJson(message, dos);
         * }</pre>
         *
         * @param obj the object to serialize to JSON
         * @param output the DataOutput to write the JSON to
         * @throws RuntimeException wrapping any IOException that occurs during writing
         * @see DataOutput
         */
        public void toJson(final Object obj, final DataOutput output) {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a byte array into a Java object of the specified type.
         * This method efficiently handles binary JSON data.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * byte[] jsonBytes = getJsonDataFromNetwork();
         * User user = jsonOps.fromJson(jsonBytes, User.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json byte array containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final byte[] json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a portion of a byte array into a Java object of the specified type.
         * This method allows reading JSON from a specific segment of a larger byte array.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * byte[] buffer = new byte[1024];
         * int bytesRead = inputStream.read(buffer);
         * Product product = jsonOps.fromJson(buffer, 0, bytesRead, Product.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json byte array containing JSON data
         * @param offset the starting position in the array
         * @param len the number of bytes to read
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final byte[] json, final int offset, final int len, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a String into a Java object of the specified type.
         * This is the most common deserialization use case.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String json = "{\"name\":\"John\",\"age\":30}";
         * Person person = jsonOps.fromJson(json, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json JSON string to deserialize
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any JsonProcessingException that occurs during deserialization
         */
        public <T> T fromJson(final String json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a File into a Java object of the specified type.
         * This method reads the entire file content and deserializes it.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * File configFile = new File("config.json");
         * Configuration config = jsonOps.fromJson(configFile, Configuration.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the file containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during file reading or deserialization
         */
        public <T> T fromJson(final File json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from an InputStream into a Java object of the specified type.
         * The stream is not closed by this method.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * try (FileInputStream fis = new FileInputStream("data.json")) {
         *     DataModel model = jsonOps.fromJson(fis, DataModel.class);
         * }
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the InputStream containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final InputStream json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a Reader into a Java object of the specified type.
         * This method is suitable for character-based input sources.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * try (FileReader reader = new FileReader("users.json")) {
         *     UserList users = jsonOps.fromJson(reader, UserList.class);
         * }
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the Reader containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final Reader json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a URL into a Java object of the specified type.
         * This method fetches content from the URL and deserializes it.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * URL apiEndpoint = new URL("https://api.example.com/weather");
         * WeatherData weather = jsonOps.fromJson(apiEndpoint, WeatherData.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the URL pointing to JSON data
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during network access or deserialization
         */
        public <T> T fromJson(final URL json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a DataInput into a Java object of the specified type.
         * This method is useful for custom binary protocols.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * DataInputStream dis = new DataInputStream(socket.getInputStream());
         * Command command = jsonOps.fromJson(dis, Command.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the DataInput containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see DataInput
         */
        public <T> T fromJson(final DataInput json, final Class<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a byte array into a Java object of the specified generic type.
         * This method supports complex generic types through TypeReference.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * byte[] jsonBytes = getJsonArrayBytes();
         * List<Product> products = jsonOps.fromJson(jsonBytes,
         *     new TypeReference<List<Product>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json byte array containing JSON data
         * @param targetType TypeReference describing the target type
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final byte[] json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a portion of a byte array into a Java object of the specified generic type.
         * This method combines array segment reading with generic type support.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * byte[] buffer = new byte[4096];
         * int length = readFromNetwork(buffer);
         * Map<String, Object> data = jsonOps.fromJson(buffer, 0, length,
         *     new TypeReference<Map<String, Object>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json byte array containing JSON data
         * @param offset the starting position in the array
         * @param len the number of bytes to read
         * @param targetType TypeReference describing the target type
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final byte[] json, final int offset, final int len, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a String into a Java object of the specified generic type.
         * This method is essential for handling complex generic types like collections and maps.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * String jsonArray = "[{\"id\":1,\"name\":\"Item1\"},{\"id\":2,\"name\":\"Item2\"}]";
         * List<Item> items = jsonOps.fromJson(jsonArray,
         *     new TypeReference<List<Item>>() {});
         * 
         * String jsonMap = "{\"key1\":{\"value\":100},\"key2\":{\"value\":200}}";
         * Map<String, ValueObject> map = jsonOps.fromJson(jsonMap,
         *     new TypeReference<Map<String, ValueObject>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json JSON string to deserialize
         * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final String json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a File into a Java object of the specified generic type.
         * This method reads files containing complex generic types.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * File dataFile = new File("complex-data.json");
         * List<Map<String, Object>> complexData = jsonOps.fromJson(dataFile,
         *     new TypeReference<List<Map<String, Object>>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the file containing JSON data
         * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during file reading or deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final File json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from an InputStream into a Java object of the specified generic type.
         * This method handles stream-based input with complex generic types.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * try (InputStream is = getClass().getResourceAsStream("/data.json")) {
         *     Set<Category> categories = jsonOps.fromJson(is,
         *         new TypeReference<Set<Category>>() {});
         * }
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the InputStream containing JSON data
         * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a Reader into a Java object of the specified generic type.
         * This method combines character-based input with generic type support.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * StringReader reader = new StringReader(jsonString);
         * Queue<Task> taskQueue = jsonOps.fromJson(reader,
         *     new TypeReference<Queue<Task>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the Reader containing JSON data
         * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a URL into a Java object of the specified generic type.
         * This method fetches and deserializes JSON with complex type support.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * URL restApi = new URL("https://api.example.com/inventory");
         * List<InventoryItem> inventory = jsonOps.fromJson(restApi,
         *     new TypeReference<List<InventoryItem>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the URL pointing to JSON data
         * @param targetType TypeReference describing the target type
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during network access or deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final URL json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a DataInput into a Java object of the specified generic type.
         * This method supports binary protocol reading with complex generic types.
         * 
         * <p>Example usage:</p>
         * <pre>{@code
         * DataInputStream dis = new DataInputStream(binaryStream);
         * Map<Long, UserProfile> profiles = jsonOps.fromJson(dis,
         *     new TypeReference<Map<Long, UserProfile>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the DataInput containing JSON data
         * @param targetType TypeReference describing the target type
         * @return the deserialized object of type T
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         * @see DataInput
         */
        public <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType) {
            try {
                return jsonMapper.readValue(json, jsonMapper.constructType(targetType));
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }
}
