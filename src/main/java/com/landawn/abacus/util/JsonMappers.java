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
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/**
 * A high-performance utility class for JSON serialization and deserialization operations using Jackson's JsonMapper.
 * This class provides a comprehensive set of static methods for converting Java objects to JSON and parsing JSON back
 * to Java objects from various sources including strings, files, streams, and URLs.
 *
 * <p>The class maintains bounded caches of configuration-bound JsonMapper instances, keyed by the supplied
 * {@link SerializationConfig} / {@link DeserializationConfig}. A cached mapper is never reconfigured after its first
 * use, preventing Jackson's serializer and root-deserializer caches from leaking one call's configuration into
 * another. Checked processing and I/O exceptions are wrapped in runtime exceptions.</p>
 *
 * <p><b>Reuse your config objects.</b> Jackson's config classes do not override {@code equals}/{@code hashCode},
 * so these caches match on object identity. Each {@code create...Config()} call creates a fresh config with
 * independent {@code ConfigOverrides}. Methods such as {@code with(...)} can create another config or return
 * the same instance when nothing changes; derived configs can share their overrides. Creating a fresh config
 * for every operation also constructs a new cached mapper each time. Finish configuring a config before use,
 * retain it, and pass it repeatedly without mutating it or its shared components. The
 * {@code SerializationFeature}/{@code DeserializationFeature} overloads do not use these caches at all - they
 * derive a lightweight {@link ObjectWriter}/{@link ObjectReader} instead - and are the cheaper choice when you only
 * need to toggle features.</p>
 *
 * <p><b>Input conventions:</b> a {@code null} {@code SerializationConfig}/{@code DeserializationConfig} means
 * "use the default configuration". An empty or blank JSON source is <i>not</i> treated as {@code null}: it fails
 * with a wrapped {@code MismatchedInputException} ("No content to map"). Compare {@link FastJson}, which returns
 * {@code null} for empty input. File output targets must already have an existing parent directory; unlike
 * {@link FastJson}, this class does not create one.</p>
 *
 * <p><b>Number types when the target is {@code Map} or {@code Object}.</b> A JSON decimal is bound to a
 * {@link Double} here ({@code 1.5} becomes {@code Double}, and {@code -0.0} keeps its sign), whereas
 * {@link FastJson} produces a {@link java.math.BigDecimal} for the same input and loses the sign of
 * {@code -0.0}. Integers agree across both ({@code int}-range to {@link Integer}, wider to {@link Long},
 * beyond {@code long} to {@link java.math.BigInteger}). Code that casts the result of
 * {@code fromJson(json, Map.class).get(key)} to a concrete numeric type is therefore not portable between
 * the two facades. Deserialize into a typed bean when the numeric type matters.</p>
 *
 * <p><b>Date and {@code java.time} handling.</b> {@link java.util.Date}, {@link java.sql.Timestamp},
 * {@link java.sql.Date} and {@link java.util.Calendar} are written as epoch milliseconds (Jackson's
 * {@code WRITE_DATES_AS_TIMESTAMPS} default), which is time-zone independent; {@link XmlMappers} writes the
 * identical value. This does <i>not</i> match {@link FastJson}, whose default is an offsetless local-time
 * string: reading FastJson's date text into a {@code Date}-typed property here throws
 * {@code InvalidFormatException}, while reading it into an untyped {@code Map} silently yields the raw
 * {@link String}. One exception to the zone-independence: {@link java.sql.Time} is written as wall-clock
 * {@code "HH:mm:ss"} by <i>both</i> facades, carries no date at all, and is therefore zone-dependent in
 * both. Finally, no Java-8 time module is registered here and this library does not declare
 * {@code jackson-datatype-jsr310}, so serializing values such as
 * {@code Instant}, {@code LocalDate} and {@code ZonedDateTime} fails with a wrapped {@code InvalidDefinitionException};
 * register {@code JavaTimeModule} on your own mapper and {@link #wrap(ObjectMapper)} it.</p>
 *
 * <p><b>A bean with no discoverable properties is rejected.</b> Jackson's
 * {@link SerializationFeature#FAIL_ON_EMPTY_BEANS} is on by default, so serializing an object that exposes
 * no properties fails with a wrapped {@code InvalidDefinitionException} instead of producing {@code "{}"}.
 * The feature overloads can only turn features <i>on</i>; to allow it, pass a config built as
 * {@code createSerializationConfig().without(SerializationFeature.FAIL_ON_EMPTY_BEANS)} to the
 * {@link SerializationConfig} overload, which then yields {@code "{}"}. {@link FastJson} writes
 * {@code "{}"} for the same input without any configuration, and {@link JsonUtil} rejects it with an
 * {@link IllegalArgumentException}.</p>
 *
 * <p><b>Non-finite values become quoted strings.</b> JSON cannot represent {@code NaN} or {@code Infinity},
 * so Jackson writes them as the strings {@code "NaN"}, {@code "Infinity"} and {@code "-Infinity"}. Reading
 * back into a <i>typed</i> target ({@code double}/{@link Double} field) restores the original value exactly,
 * and other parsers accept the document. Reading back into an <i>untyped</i> target leaves them as
 * {@link String}: {@code fromJson(json, Map.class).get(k)} yields {@code "NaN"} the {@code String}, not a
 * {@code Double}. Note also that a bare top-level {@code "Infinity"} written this way makes
 * {@link FastJson#fromJson(String, Class)} raise an {@link ArrayIndexOutOfBoundsException} - see that
 * class's parse-failure note. For comparison: {@code FastJson} writes non-finite values as {@code null} and
 * loses them, and {@link JsonUtil} rejects them outright.</p>
 *
 * <p><b>Byte output escapes characters outside the BMP.</b> Jackson's UTF-8 generator writes a non-BMP
 * character such as an emoji as a {@code &#92;uXXXX&#92;uXXXX} surrogate escape, while the {@code String}- and
 * {@code Writer}-based paths emit unescaped UTF-16 characters. When that character output is encoded as UTF-8,
 * {@code toJson(obj)} and {@code toJson(obj, outputStream)}
 * can produce <i>different bytes</i> for the same object. Both are valid JSON and both parse back to the same
 * value, but do not compare, hash or sign the output of one path against the other.</p>
 *
 * <p><b>A failed write does not leave the target file intact.</b> The {@code File} overloads open (and
 * therefore truncate) the target before serialization runs, so if serialization throws part-way the file is
 * left empty or holding a fragment and any previous content is gone. Serialize to a {@code String} or a
 * {@code byte[]} first, or write to a temporary file and rename, when the destination must survive a failure.
 * The same applies to a caller-supplied {@code OutputStream} or {@code Writer}, which may already have
 * received a partial document when the exception is thrown.</p>
 *
 * <p><b>Security:</b> Default polymorphic typing is not enabled by this class. Enabling it in a caller-provided
 * {@link DeserializationConfig} can allow JSON input to select Java implementation types; use an appropriately
 * restrictive polymorphic type validator and never enable unrestricted polymorphic deserialization for untrusted input.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Thread-safe static methods for JSON operations</li>
 *   <li>Support for custom serialization/deserialization configurations</li>
 *   <li>Pretty printing support for formatted JSON output</li>
 *   <li>Generic type support through TypeReference</li>
 *   <li>Multiple input/output source types (String, File, Stream, URL, etc.)</li>
 *   <li>Bounded caches of mappers for custom configuration reuse</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
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
 * @see JsonUtil
 */
public final class JsonMappers {
    private static final int POOL_SIZE = 128;
    private static final Map<SerializationConfig, JsonMapper> serializationMapperPool = new LinkedHashMap<>(POOL_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<SerializationConfig, JsonMapper> eldest) {
            return size() > POOL_SIZE;
        }
    };
    private static final Map<DeserializationConfig, JsonMapper> deserializationMapperPool = new LinkedHashMap<>(POOL_SIZE, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(final Map.Entry<DeserializationConfig, JsonMapper> eldest) {
            return size() > POOL_SIZE;
        }
    };

    private static final JsonMapper defaultJsonMapper = new JsonMapper();

    /**
     * Pretty-printing is done through an {@link ObjectWriter} derived from {@link #defaultJsonMapper} rather than
     * through a second, separately configured mapper. An {@code ObjectWriter} is immutable and thread-safe, shares
     * the mapper's serializer cache, and needs no {@code copy()} - which {@link ObjectMapper#copy()} refuses to
     * perform for subclasses that do not override it. {@code writer(INDENT_OUTPUT)} is used rather than
     * {@code writerWithDefaultPrettyPrinter()} so that the feature itself is enabled on the writer's config:
     * a custom serializer that queries {@code SerializerProvider.isEnabled(INDENT_OUTPUT)} still sees {@code true},
     * exactly as it did when this was a mapper with the feature enabled.
     */
    private static final ObjectWriter defaultJsonWriterForPretty = defaultJsonMapper.writer(SerializationFeature.INDENT_OUTPUT);

    private JsonMappers() {
        // Utility class - prevent instantiation
    }

    /**
     * @throws IllegalArgumentException if {@code json} is {@code null} or {@code len} is negative.
     * @throws IndexOutOfBoundsException if {@code offset} is negative or the range exceeds {@code json.length}.
     */
    private static void checkByteRange(final byte[] json, final int offset, final int len) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(json, cs.json);
        N.checkFromIndexSize(offset, len, json.length);
    }

    /**
     * Serializes the specified object to a JSON string using default configuration.
     * This method provides the simplest way to convert a Java object to JSON format.
     *
     * <p>The serialization uses Jackson's default configuration settings. For custom
     * serialization behavior, use the overloaded methods that accept configuration parameters.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = JsonMappers.toJson(person);
     * // Result: {"name":"John","age":30}
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @return a JSON string representation of the object
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see #toJson(Object, boolean)
     * @see #toJson(Object, SerializationFeature, SerializationFeature...)
     */
    public static String toJson(final Object obj) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
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
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param prettyFormat if {@code true}, the output will be formatted with indentation and line breaks;
     *                     if {@code false}, output will be compact (single line)
     * @return a JSON string representation of the object
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see #toJson(Object)
     */
    public static String toJson(final Object obj, final boolean prettyFormat) throws RuntimeException {
        try {
            if (prettyFormat) {
                return defaultJsonWriterForPretty.writeValueAsString(obj);
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
     *   <li>WRAP_ROOT_VALUE - Wrap the root value with the type name</li>
     * </ul>
     *
     * <p>Note that each supplied feature is <i>enabled</i>; this method cannot disable a feature.
     * To disable a feature (or otherwise customize the configuration), use
     * {@link #toJson(Object, SerializationConfig)} with a config built via
     * {@link #createSerializationConfig()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param first the first serialization feature to apply (required to ensure at least one feature)
     * @param features additional serialization features to apply; may be empty but not {@code null}
     * @return a JSON string representation of the object
     * @throws IllegalArgumentException if {@code first} or the {@code features} array is {@code null}.
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see SerializationFeature
     * @see #toJson(Object, SerializationConfig)
     */
    @SafeVarargs
    public static String toJson(final Object obj, final SerializationFeature first, final SerializationFeature... features)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(first, cs.first);
        N.checkArgNotNull(features, cs.features);

        // Uses an ObjectWriter rather than a feature-derived SerializationConfig: SerializationConfig does not
        // override equals/hashCode, so a config built here can never be found again in the mapper cache and every
        // call would construct (and retain) a brand-new JsonMapper. ObjectWriter is immutable, thread-safe, cheap
        // to derive, and shares the default mapper's serializer cache.
        try {
            return defaultJsonMapper.writer(first, features).writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to a JSON string using a custom serialization configuration.
     * This method provides maximum flexibility by accepting a complete SerializationConfig object.
     *
     * <p>The supplied configuration controls Jackson's serialization settings. It does not transfer
     * mapper-level components such as registered modules, serializer factories, or a custom JSON factory.
     * Use {@link #wrap(ObjectMapper)} when those mapper-level customizations are required.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param config the custom serialization configuration to use; if {@code null}, uses default configuration
     * @return a JSON string representation of the object
     * @throws RuntimeException if serialization fails due to invalid object structure or configuration
     * @see #createSerializationConfig()
     * @see SerializationConfig
     */
    public static String toJson(final Object obj, final SerializationConfig config) throws RuntimeException {
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
     * <p>The character encoding used is UTF-8. The parent directory must already
     * exist; this method does not create missing parent directories.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * File outputFile = new File("data/person.json");
     * JsonMappers.toJson(person, outputFile);
     * // File now contains: {"name":"John","age":30}
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the file to write the JSON to; the parent directory must already exist
     * @throws RuntimeException if serialization fails or file cannot be written
     * @see #toJson(Object, File, SerializationConfig)
     */
    public static void toJson(final Object obj, final File output) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT);
     *
     * JsonMappers.toJson(myData, new File("formatted-data.json"), config);
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the file to write the JSON to; the parent directory must already exist
     * @param config the custom serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if serialization fails or file cannot be written
     * @see #toJson(Object, File)
     * @see SerializationConfig
     */
    public static void toJson(final Object obj, final File output, final SerializationConfig config) throws RuntimeException {
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
     * Note: the stream is closed after writing, because {@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}
     * is enabled by default on the underlying mapper.
     *
     * <p>This method is useful for writing JSON to network streams, HTTP responses,
     * or any other output stream.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("data.json")) {
     *     JsonMappers.toJson(myObject, fos);
     * }
     *
     * // Writing to HTTP response
     * JsonMappers.toJson(responseData, httpServletResponse.getOutputStream());
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the output stream to write the JSON to; closed after writing by Jackson's default auto-close behavior
     * @throws RuntimeException if serialization fails or writing to stream fails
     * @see #toJson(Object, OutputStream, SerializationConfig)
     */
    public static void toJson(final Object obj, final OutputStream output) throws RuntimeException {
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
     * <p>The stream is closed after writing (Jackson's default auto-close behavior).
     * Use this when you need specific serialization behavior for stream-based output,
     * such as custom date formats or pretty printing for HTTP responses.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT)
     *     .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     *
     * JsonMappers.toJson(apiResponse, response.getOutputStream(), config);
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the output stream to write the JSON to; closed after writing by Jackson's default auto-close behavior
     * @param config the custom serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if serialization fails or writing to stream fails
     * @see #toJson(Object, OutputStream)
     * @see SerializationConfig
     */
    public static void toJson(final Object obj, final OutputStream output, final SerializationConfig config) throws RuntimeException {
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
     * Note: the writer is closed after writing, because {@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}
     * is enabled by default on the underlying mapper.
     *
     * <p>This method is ideal for writing JSON to character-based outputs such as
     * StringWriter, FileWriter, or any custom Writer implementation. The character
     * encoding is handled by the Writer itself.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the writer to write the JSON to; closed after writing by Jackson's default auto-close behavior
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, Writer, SerializationConfig)
     */
    public static void toJson(final Object obj, final Writer output) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT);
     *
     * try (FileWriter writer = new FileWriter("formatted.json")) {
     *     JsonMappers.toJson(myData, writer, config);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the writer to write the JSON to; closed after writing by Jackson's default auto-close behavior
     * @param config the custom serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, Writer)
     * @see SerializationConfig
     */
    public static void toJson(final Object obj, final Writer output, final SerializationConfig config) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (DataOutputStream dos = new DataOutputStream(
     *         new FileOutputStream("data.bin"))) {
     *     // Write some binary data
     *     dos.writeInt(42);
     *     // Write JSON data
     *     JsonMappers.toJson(myObject, (DataOutput) dos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the DataOutput to write the JSON to
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, DataOutput, SerializationConfig)
     * @see DataOutput
     */
    public static void toJson(final Object obj, final DataOutput output) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = JsonMappers.createSerializationConfig()
     *     .without(SerializationFeature.INDENT_OUTPUT);
     *
     * try (RandomAccessFile raf = new RandomAccessFile("data.bin", "rw")) {
     *     JsonMappers.toJson(myObject, raf, config);
     * }
     * }</pre>
     *
     * @param obj the object to serialize; can be {@code null} (produces "null")
     * @param output the DataOutput to write the JSON to
     * @param config the custom serialization configuration to use; if {@code null}, uses default configuration
     * @throws RuntimeException if serialization fails or writing fails
     * @see #toJson(Object, DataOutput)
     * @see SerializationConfig
     * @see DataOutput
     */
    public static void toJson(final Object obj, final DataOutput output, final SerializationConfig config) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if deserialization fails due to invalid JSON or type mismatch
     * @see #fromJson(byte[], int, int, Class)
     * @see #fromJson(byte[], TypeReference)
     */
    public static <T> T fromJson(final byte[] json, final Class<? extends T> targetType) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code json} is {@code null} or {@code len} is negative.
     * @throws IndexOutOfBoundsException if the requested segment is outside {@code json}
     * @throws RuntimeException if deserialization fails or the JSON is invalid
     * @see #fromJson(byte[], Class)
     */
    public static <T> T fromJson(final byte[] json, final int offset, final int len, final Class<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
        checkByteRange(json, offset, len);

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
     * primitives, and {@code null} values. For complex generic types, use the TypeReference
     * overload instead.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * Person nullPerson = JsonMappers.fromJson(nullJson, Person.class);   // returns null
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; {@code null} if JSON string is "null"
     * @throws RuntimeException if deserialization fails due to invalid JSON or type mismatch
     * @see #fromJson(String, TypeReference)
     * @see #fromJson(String, Class, DeserializationFeature, DeserializationFeature...)
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType) throws RuntimeException {
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
     * <p>Note that each supplied feature is <i>enabled</i>; this method cannot disable a feature.
     * To disable a feature (or otherwise customize the configuration), use
     * {@link #fromJson(String, Class, DeserializationConfig)} with a config built via
     * {@link #createDeserializationConfig()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use BigDecimal for floating-point values
     * String json = "{\"name\":\"John\",\"balance\":30.5}";
     * Map<String, Object> account = JsonMappers.fromJson(json, Map.class,
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
     * @param features additional deserialization features to apply; may be empty but not {@code null}
     * @return the deserialized object; {@code null} if JSON string is "null"
     * @throws IllegalArgumentException if {@code targetType}, {@code first} or the {@code features} array is {@code null}.
     * @throws RuntimeException if deserialization fails
     * @see DeserializationFeature
     * @see #fromJson(String, Class, DeserializationConfig)
     */
    @SafeVarargs
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(first, cs.first);
        N.checkArgNotNull(features, cs.features);

        // See toJson(Object, SerializationFeature, SerializationFeature...) for why this uses an ObjectReader
        // instead of a feature-derived DeserializationConfig.
        try {
            return defaultJsonMapper.reader(first, features).forType(targetType).readValue(json);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a string using a custom deserialization configuration.
     * This method provides maximum control over the deserialization process.
     *
     * <p>The supplied configuration controls Jackson's deserialization settings. It does not transfer
     * mapper-level components such as registered modules, deserializer factories, or a custom JSON factory.
     * Use {@link #wrap(ObjectMapper)} when those mapper-level customizations are required.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON string is "null"
     * @throws RuntimeException if deserialization fails
     * @see #createDeserializationConfig()
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final DeserializationConfig config) throws RuntimeException {
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
     * This method parses a JSON value from the file. Trailing content is rejected only when
     * Jackson's corresponding validation feature is enabled.
     *
     * <p>The file is expected to contain valid JSON data encoded in UTF-8.
     * The method handles all file I/O operations internally.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see #fromJson(File, Class, DeserializationConfig)
     * @see #fromJson(File, TypeReference)
     */
    public static <T> T fromJson(final File json, final Class<? extends T> targetType) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see #fromJson(File, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final File json, final Class<? extends T> targetType, final DeserializationConfig config) throws RuntimeException {
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
     * Note: the stream is closed after reading, because {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}
     * is enabled by default on the underlying mapper.
     *
     * <p>This method is ideal for reading JSON from network connections, file streams,
     * or any other input source.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(InputStream, Class, DeserializationConfig)
     * @see #fromJson(InputStream, TypeReference)
     */
    public static <T> T fromJson(final InputStream json, final Class<? extends T> targetType) throws RuntimeException {
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
     * <p>The stream is closed after reading (Jackson's default auto-close behavior).
     * Use this when reading JSON from streams that require special handling, such as
     * lenient parsing for external APIs.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(InputStream, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final InputStream json, final Class<? extends T> targetType, final DeserializationConfig config) throws RuntimeException {
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
     * Note: the reader is closed after reading, because {@code JsonParser.Feature.AUTO_CLOSE_SOURCE}
     * is enabled by default on the underlying mapper.
     *
     * <p>This method is useful when you need to control the character encoding
     * or when working with character-based input sources. The Reader handles
     * the character encoding.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(Reader, Class, DeserializationConfig)
     * @see #fromJson(Reader, TypeReference)
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType) throws RuntimeException {
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
     * <p>The reader is closed after reading (Jackson's default auto-close behavior).
     * Use this when reading JSON from character sources that require special handling.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(Reader, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType, final DeserializationConfig config) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}.
     * @throws RuntimeException if opening, reading, or closing the URL stream fails, or its JSON cannot be deserialized to {@code targetType}
     * @see #fromJson(URL, Class, DeserializationConfig)
     * @see #fromJson(URL, TypeReference)
     */
    public static <T> T fromJson(final URL json, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);

        try (InputStream is = json.openStream()) {
            return defaultJsonMapper.readValue(is, targetType);
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
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}.
     * @throws RuntimeException if opening, reading, or closing the URL stream fails, or its JSON cannot be deserialized to {@code targetType}
     * @see #fromJson(URL, Class)
     * @see DeserializationConfig
     */
    public static <T> T fromJson(final URL json, final Class<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);

        final JsonMapper jsonMapper = getJsonMapper(config);

        try (InputStream is = json.openStream()) {
            return jsonMapper.readValue(is, targetType);
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (DataInputStream dis = new DataInputStream(
     *         new FileInputStream("data.bin"))) {
     *     // Read some binary data
     *     int version = dis.readInt();
     *     // Read JSON data
     *     Config config = JsonMappers.fromJson((DataInput) dis, Config.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the DataInput containing JSON content
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(DataInput, Class, DeserializationConfig)
     * @see #fromJson(DataInput, TypeReference)
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final Class<? extends T> targetType) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see #fromJson(DataInput, Class)
     * @see DeserializationConfig
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final Class<? extends T> targetType, final DeserializationConfig config) throws RuntimeException {
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Deserialize a List of objects
     * byte[] jsonBytes = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]".getBytes(StandardCharsets.UTF_8);
     * List<Person> people = JsonMappers.fromJson(jsonBytes,
     *     new TypeReference<List<Person>>() {});
     *
     * // Deserialize a Map
     * byte[] mapBytes = "{\"key1\":\"value1\",\"key2\":\"value2\"}".getBytes(StandardCharsets.UTF_8);
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if deserialization fails or type doesn't match
     * @see TypeReference
     * @see #fromJson(byte[], Class)
     */
    public static <T> T fromJson(final byte[] json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}, or {@code len} is
     *         negative.
     * @throws IndexOutOfBoundsException if the requested segment is outside {@code json}
     * @throws RuntimeException if deserialization fails or the JSON is invalid
     * @see TypeReference
     * @see #fromJson(byte[], int, int, Class)
     */
    public static <T> T fromJson(final byte[] json, final int offset, final int len, final TypeReference<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
        checkByteRange(json, offset, len);

        N.checkArgNotNull(targetType, cs.targetType);

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
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if deserialization fails or JSON is invalid
     * @see TypeReference
     * @see #fromJson(String, Class)
     * @see #fromJson(String, TypeReference, DeserializationFeature, DeserializationFeature...)
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * handling, such as collections that should accept single values as arrays.
     * Each supplied feature is <i>enabled</i>; this method cannot disable a feature.
     * To disable a feature, use {@link #fromJson(String, TypeReference, DeserializationConfig)}
     * with a config built via {@link #createDeserializationConfig()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Accept single value as array for List
     * String singleValue = "\"single-item\"";
     * List<String> list = JsonMappers.fromJson(singleValue,
     *     new TypeReference<List<String>>() {},
     *     DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
     *
     * // Use BigDecimal for decimal values in Object-typed properties
     * List<FinancialRecord> records = JsonMappers.fromJson(jsonData,
     *     new TypeReference<List<FinancialRecord>>() {},
     *     DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the JSON content as a string
     * @param targetType TypeReference capturing the generic type information
     * @param first the first deserialization feature to apply (required)
     * @param features additional deserialization features to apply; may be empty but not {@code null}
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType}, {@code first} or the {@code features} array is {@code null}.
     * @throws RuntimeException if deserialization fails
     * @see TypeReference
     * @see DeserializationFeature
     * @see #fromJson(String, TypeReference, DeserializationConfig)
     */
    @SafeVarargs
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(first, cs.first);
        N.checkArgNotNull(features, cs.features);

        // See toJson(Object, SerializationFeature, SerializationFeature...) for why this uses an ObjectReader
        // instead of a feature-derived DeserializationConfig.
        try {
            return defaultJsonMapper.reader(first, features).forType(targetType).readValue(json);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a string using TypeReference with custom configuration.
     * This method provides maximum flexibility for deserializing generic types.
     *
     * <p>Use this method when you need complex configuration for generic types,
     * such as custom deserializers for collection elements or special date handling
     * in maps.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if deserialization fails
     * @see TypeReference
     * @see DeserializationConfig
     * @see #createDeserializationConfig()
     */
    public static <T> T fromJson(final String json, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see TypeReference
     * @see #fromJson(File, Class)
     * @see #fromJson(File, TypeReference, DeserializationConfig)
     */
    public static <T> T fromJson(final File json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * <p><b>Usage Examples:</b></p>
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
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if file cannot be read or JSON is invalid
     * @see TypeReference
     * @see DeserializationConfig
     * @see #fromJson(File, TypeReference)
     */
    public static <T> T fromJson(final File json, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * The stream is closed after reading (Jackson's default auto-close behavior).
     *
     * <p>This method is ideal for deserializing collections, maps, and other generic
     * types from network streams, file streams, or classpath resources.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if reading fails or JSON is invalid
     * @see TypeReference
     * @see #fromJson(InputStream, Class)
     * @see #fromJson(InputStream, TypeReference, DeserializationConfig)
     */
    public static <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from an input stream into a Java object of the specified generic type with
     * custom deserialization configuration. The stream is closed after reading (Jackson's default
     * auto-close behavior).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read a list of users with custom configuration
     * try (FileInputStream fis = new FileInputStream("users.json")) {
     *     DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *         .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     *     List<User> users = JsonMappers.fromJson(fis,
     *         new TypeReference<List<User>>() {}, config);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the input stream containing JSON content; closed after reading by Jackson's default auto-close behavior
     * @param targetType TypeReference capturing the generic type information
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if reading fails or the JSON is invalid
     * @see TypeReference
     * @see DeserializationConfig
     * @see #fromJson(InputStream, TypeReference)
     */
    public static <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * Deserializes JSON from a Reader into a Java object of the specified generic type using default
     * configuration. Suitable for character-based sources such as {@link java.io.StringReader} or
     * {@link java.io.FileReader}. The reader is closed after reading (Jackson's default auto-close behavior).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read JSON from a StringReader
     * StringReader reader = new StringReader("[{\"name\":\"John\"},{\"name\":\"Jane\"}]");
     * List<Person> people = JsonMappers.fromJson(reader,
     *     new TypeReference<List<Person>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the reader containing JSON content; closed after reading by Jackson's default auto-close behavior
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if reading fails or the JSON is invalid
     * @see TypeReference
     * @see #fromJson(Reader, TypeReference, DeserializationConfig)
     * @see #fromJson(Reader, Class)
     */
    public static <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultJsonMapper.readValue(json, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a Reader into a Java object of the specified generic type with custom
     * deserialization configuration. The reader is closed after reading (Jackson's default
     * auto-close behavior).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read JSON with lenient parsing configuration
     * try (FileReader reader = new FileReader("data.json")) {
     *     DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *         .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     *     Map<String, Object> data = JsonMappers.fromJson(reader,
     *         new TypeReference<Map<String, Object>>() {}, config);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the reader containing JSON content; closed after reading by Jackson's default auto-close behavior
     * @param targetType TypeReference capturing the generic type information
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if reading fails or the JSON is invalid
     * @see TypeReference
     * @see DeserializationConfig
     * @see #fromJson(Reader, TypeReference)
     */
    public static <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * types fetched from remote HTTP endpoints or classpath resources.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Fetch and deserialize JSON from a web API
     * URL apiUrl = new URL("https://api.example.com/users");
     * List<User> users = JsonMappers.fromJson(apiUrl,
     *     new TypeReference<List<User>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the URL pointing to JSON data to deserialize
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}.
     * @throws RuntimeException if opening, reading, or closing the URL stream fails, or its JSON cannot be deserialized to {@code targetType}
     * @see TypeReference
     * @see #fromJson(URL, TypeReference, DeserializationConfig)
     * @see #fromJson(URL, Class)
     */
    public static <T> T fromJson(final URL json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);

        try (InputStream is = json.openStream()) {
            return defaultJsonMapper.readValue(is, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a URL into a Java object of the specified generic type with custom deserialization configuration.
     * This method provides control over the deserialization process when fetching JSON from URLs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Fetch JSON while ignoring unknown properties
     * URL url = new URL("https://api.example.com/events");
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * List<Event> events = JsonMappers.fromJson(url,
     *     new TypeReference<List<Event>>() {}, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the URL pointing to JSON data to deserialize
     * @param targetType TypeReference capturing the generic type information
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}.
     * @throws RuntimeException if opening, reading, or closing the URL stream fails, or its JSON cannot be deserialized to {@code targetType}
     * @see TypeReference
     * @see DeserializationConfig
     * @see #fromJson(URL, TypeReference)
     */
    public static <T> T fromJson(final URL json, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);

        final JsonMapper jsonMapper = getJsonMapper(config);

        try (InputStream is = json.openStream()) {
            return jsonMapper.readValue(is, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(jsonMapper);
        }
    }

    /**
     * Deserializes JSON from a {@link DataInput} into a Java object of the specified generic type
     * using the default configuration. Useful for reading JSON embedded in custom binary protocols.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read JSON from a DataInputStream
     * DataInputStream dis = new DataInputStream(inputStream);
     * List<Product> products = JsonMappers.fromJson((DataInput) dis,
     *     new TypeReference<List<Product>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the DataInput containing JSON content
     * @param targetType TypeReference capturing the generic type information
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if reading fails or the JSON is invalid
     * @see TypeReference
     * @see #fromJson(DataInput, TypeReference, DeserializationConfig)
     * @see #fromJson(DataInput, Class)
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

        try {
            return defaultJsonMapper.readValue(json, defaultJsonMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a {@link DataInput} into a Java object of the specified generic type
     * with custom deserialization configuration. Useful for reading JSON embedded in custom binary
     * protocols where special deserialization handling is required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Read JSON with custom configuration from binary protocol
     * DataInputStream dis = new DataInputStream(socket.getInputStream());
     * DeserializationConfig config = JsonMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS);
     * List<Order> orders = JsonMappers.fromJson((DataInput) dis,
     *     new TypeReference<List<Order>>() {}, config);
     * }</pre>
     *
     * @param <T> the type of the object to deserialize to
     * @param json the DataInput containing JSON content
     * @param targetType TypeReference capturing the generic type information
     * @param config the custom deserialization configuration; if {@code null}, uses default
     * @return the deserialized object; {@code null} if JSON contains "null"
     * @throws IllegalArgumentException if {@code targetType} is {@code null}.
     * @throws RuntimeException if reading fails or the JSON is invalid
     * @see TypeReference
     * @see DeserializationConfig
     * @see #fromJson(DataInput, TypeReference)
     * @see DataInput
     */
    public static <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType, final DeserializationConfig config)
            throws IllegalArgumentException, RuntimeException {
        N.checkArgNotNull(targetType, cs.targetType);

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
     * It has its own {@code ConfigOverrides}, independent of the default mapper. Configurations derived from
     * this result with {@code with(...)} or {@code without(...)} can share those overrides, so a later
     * {@code withPropertyInclusion(...)} call can affect the derived configurations too. Other components,
     * including the pretty printer, date format and annotation introspector, may remain shared with other
     * mappers. Supply replacements rather than mutating shared components, and finish all configuration
     * before passing it to a serialization operation.
     *
     * <p>Each call creates a copy of the default mapper to obtain the configuration. Reuse a completed
     * configuration to avoid repeating that allocation for every operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
        // Derived from a throw-away copy of the default mapper rather than from the default mapper itself:
        // with(..)/without(..) carry the mapper's ConfigOverrides by reference, and
        // SerializationConfig.withPropertyInclusion(..) writes through that shared object in place and returns
        // the same config, so a config derived directly from defaultJsonMapper would let one caller change
        // toJson(..) for the whole process. ObjectMapper.copy() does ConfigOverrides.copy(), giving the returned
        // config its own.
        return defaultJsonMapper.copy().getSerializationConfig();
    }

    /**
     * Creates a new DeserializationConfig instance with default settings.
     * This method provides a base configuration that can be customized for specific deserialization needs.
     * It has its own {@code ConfigOverrides}, independent of the default mapper. Configurations derived from
     * this result can share those overrides. Other components, including the date format and annotation
     * introspector, may remain shared with other mappers. Supply replacements rather than mutating shared
     * components, and finish all configuration before passing it to a deserialization operation.
     *
     * <p>Each call creates a copy of the default mapper to obtain the configuration. Reuse a completed
     * configuration to avoid repeating that allocation for every operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
        // Same reason as createSerializationConfig(): copy() is what gives the returned config a ConfigOverrides
        // of its own instead of defaultJsonMapper's.
        return defaultJsonMapper.copy().getDeserializationConfig();
    }

    private static JsonMapper getJsonMapper(final SerializationConfig config) {
        if (config == null) {
            return defaultJsonMapper;
        }

        synchronized (serializationMapperPool) {
            JsonMapper mapper = serializationMapperPool.get(config);
            if (mapper == null) {
                mapper = new JsonMapper();
                mapper.setConfig(config);
                serializationMapperPool.put(config, mapper);
            }

            return mapper;
        }
    }

    private static JsonMapper getJsonMapper(final DeserializationConfig config) {
        if (config == null) {
            return defaultJsonMapper;
        }

        synchronized (deserializationMapperPool) {
            JsonMapper mapper = deserializationMapperPool.get(config);
            if (mapper == null) {
                mapper = new JsonMapper();
                mapper.setConfig(config);
                deserializationMapperPool.put(config, mapper);
            }

            return mapper;
        }
    }

    private static void recycle(@SuppressWarnings("unused") final JsonMapper mapper) {
        // Configuration-bound mappers are retained in the bounded caches above. Reconfiguring a
        // mapper after use is unsafe because Jackson retains serializers and root deserializers.
    }

    /**
     * Wraps a Jackson ObjectMapper instance to provide convenient JSON operations through the {@link One} wrapper class.
     * This method allows using a custom ObjectMapper with specific configurations while benefiting from
     * the simplified API provided by the {@link One} wrapper.
     *
     * <p>The supplied mapper remains caller-owned and is used directly for compact output and all reads; this
     * class neither resets nor closes it. Pretty-printed output goes through an {@link ObjectWriter} derived from
     * the mapper when the wrapper is created. Configure the mapper completely before wrapping it: if it is mutated
     * afterwards, which of those changes the pretty-printing path picks up is unspecified.</p>
     *
     * <p><b>The output format follows the supplied mapper, not this class's name.</b> Wrapping an
     * {@code XmlMapper} (or any other {@link ObjectMapper} subclass bound to a non-JSON format) makes
     * {@code toJson} emit that format instead of JSON. Pass a JSON-capable mapper unless that is intended.</p>
     *
     * <p><b>Usage Examples:</b></p>
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
     * @param jsonMapper the ObjectMapper instance to wrap; must not be {@code null}. Subclasses are supported:
     *        no {@link ObjectMapper#copy()} is performed, so mappers that do not override {@code copy()} are accepted.
     * @return a {@link One} instance wrapping the provided ObjectMapper
     * @throws IllegalArgumentException if {@code jsonMapper} is {@code null}
     * @see One
     * @see ObjectMapper
     */
    public static One wrap(final ObjectMapper jsonMapper) throws IllegalArgumentException {
        return new One(N.checkArgNotNull(jsonMapper, cs.jsonMapper));
    }

    /**
     * A wrapper class that provides convenient JSON serialization and deserialization methods using a specific ObjectMapper instance.
     * This class encapsulates a configured ObjectMapper and provides a simplified API for common JSON operations
     * with support for pretty printing and various input/output sources.
     *
     * <p>Key features:</p>
     * <ul>
     *   <li>Encapsulates a specific ObjectMapper configuration</li>
     *   <li>Automatic pretty printing support with a derived ObjectWriter</li>
     *   <li>Consistent exception handling (wraps checked exceptions as RuntimeException)</li>
     *   <li>Support for multiple input/output formats</li>
     * </ul>
     *
     * <p>This class is typically obtained through {@link JsonMappers#wrap(ObjectMapper)} rather than
     * instantiated directly. Configure the supplied mapper before wrapping it and do not mutate it while this
     * wrapper is in use: compact output and every read go straight to the wrapped mapper, while pretty-printed
     * output goes through an {@link ObjectWriter} derived from it at construction time, so mutating the mapper
     * afterwards can make the two paths disagree.</p>
     *
     * @see JsonMappers#wrap(ObjectMapper)
     */
    public static final class One {

        private final ObjectMapper jsonMapper;
        private final ObjectWriter jsonWriterForPretty;

        /**
         * Creates a wrapper around the specified mapper. The mapper is used as-is for compact output and for
         * every read; an {@link ObjectWriter} with {@link SerializationFeature#INDENT_OUTPUT} enabled is derived
         * from it at construction time and used whenever pretty formatting is requested. The mapper is neither
         * copied nor modified, so subclasses that do not override {@link ObjectMapper#copy()} are supported.
         *
         * @param jsonMapper the mapper to wrap; must not be {@code null}
         * @see JsonMappers#wrap(ObjectMapper)
         */
        One(final ObjectMapper jsonMapper) {
            this.jsonMapper = jsonMapper;
            // Deriving an ObjectWriter instead of copy()-ing the mapper: ObjectMapper.copy() throws
            // IllegalStateException for any subclass that does not override it, which made wrap() reject
            // perfectly usable custom mappers. writer(INDENT_OUTPUT) - rather than
            // writerWithDefaultPrettyPrinter() - keeps the feature enabled on the writer's config, so a
            // custom serializer querying isEnabled(INDENT_OUTPUT) still sees true.
            jsonWriterForPretty = jsonMapper.writer(SerializationFeature.INDENT_OUTPUT);
        }

        /**
         * Serializes a Java object to its JSON string representation using the wrapped ObjectMapper.
         * This method provides the most common use case for JSON serialization.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person person = new Person("John", 30);
         * String json = jsonOps.toJson(person);
         * // Result: {"name":"John","age":30}
         * }</pre>
         *
         * @param obj the object to serialize; can be {@code null} (produces {@code "null"})
         * @return a JSON string representation of the object
         * @throws RuntimeException if serialization fails
         */
        public String toJson(final Object obj) throws RuntimeException {
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Map<String, Object> data = new java.util.LinkedHashMap<>();
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
         * @param obj the object to serialize; can be {@code null} (produces {@code "null"})
         * @param prettyFormat if {@code true}, formats the JSON with indentation and line breaks; if {@code false},
         *                     serializes with the wrapped mapper exactly as configured. Note that {@code false} does
         *                     not force compact output: a wrapped mapper that already enables
         *                     {@link SerializationFeature#INDENT_OUTPUT} still produces indented JSON. This flag can
         *                     only add pretty printing, never remove it.
         * @return a JSON string representation of the object
         * @throws RuntimeException if serialization fails
         */
        public String toJson(final Object obj, final boolean prettyFormat) throws RuntimeException {
            try {
                if (prettyFormat) {
                    return jsonWriterForPretty.writeValueAsString(obj);
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
         * <p>The character encoding used is UTF-8. The parent directory must already exist;
         * this method does not create missing parent directories.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<User> users = getUserList();
         * File outputFile = new File("users.json");
         * jsonOps.toJson(users, outputFile);
         * }</pre>
         *
         * @param obj the object to serialize; can be {@code null} (produces {@code "null"})
         * @param output the file to write the JSON to; created or overwritten as needed, but its
         *               parent directory must already exist
         * @throws RuntimeException if serialization fails or the file cannot be written
         */
        public void toJson(final Object obj, final File output) throws RuntimeException {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes a Java object to JSON and writes it to the specified OutputStream.
         * Note: with Jackson's default settings the wrapped mapper closes the stream after
         * writing ({@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Product product = new Product("Widget", 29.99);
         * try (FileOutputStream fos = new FileOutputStream("product.json")) {
         *     jsonOps.toJson(product, fos);
         * }
         * }</pre>
         *
         * @param obj the object to serialize; can be {@code null} (produces {@code "null"})
         * @param output the output stream to write the JSON to; closed after writing by Jackson's default auto-close behavior
         * @throws RuntimeException if serialization fails or writing to the stream fails
         */
        public void toJson(final Object obj, final OutputStream output) throws RuntimeException {
            try {
                jsonMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes a Java object to JSON and writes it to the specified Writer.
         * This method is useful for character-based output destinations. Note: with Jackson's default
         * settings the wrapped mapper closes the writer after writing
         * ({@code JsonGenerator.Feature.AUTO_CLOSE_TARGET}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Order order = new Order(12345, "Processing");
         * try (StringWriter writer = new StringWriter()) {
         *     jsonOps.toJson(order, writer);
         *     String json = writer.toString();
         * }
         * }</pre>
         *
         * @param obj the object to serialize; can be {@code null} (produces {@code "null"})
         * @param output the writer to write the JSON to; closed after writing by Jackson's default auto-close behavior
         * @throws RuntimeException if serialization fails or writing fails
         */
        public void toJson(final Object obj, final Writer output) throws RuntimeException {
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Message message = new Message("Hello", System.currentTimeMillis());
         * DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
         * jsonOps.toJson(message, (DataOutput) dos);
         * }</pre>
         *
         * @param obj the object to serialize; can be {@code null} (produces {@code "null"})
         * @param output the DataOutput to write the JSON to
         * @throws RuntimeException if serialization fails or writing fails
         * @see DataOutput
         */
        public void toJson(final Object obj, final DataOutput output) throws RuntimeException {
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * byte[] jsonBytes = getJsonDataFromNetwork();
         * User user = jsonOps.fromJson(jsonBytes, User.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json byte array containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final byte[] json, final Class<? extends T> targetType) throws RuntimeException {
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * byte[] buffer = inputStream.readAllBytes();   // read the complete JSON document
         * Product product = jsonOps.fromJson(buffer, 0, buffer.length, Product.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json byte array containing JSON data
         * @param offset the starting position in the array
         * @param len the number of bytes to read
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code json} is {@code null} or {@code len} is negative.
         * @throws IndexOutOfBoundsException if the requested segment is outside {@code json}
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final byte[] json, final int offset, final int len, final Class<? extends T> targetType)
                throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
            checkByteRange(json, offset, len);

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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String json = "{\"name\":\"John\",\"age\":30}";
         * Person person = jsonOps.fromJson(json, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json JSON string to deserialize
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws RuntimeException wrapping any JsonProcessingException that occurs during deserialization
         */
        public <T> T fromJson(final String json, final Class<? extends T> targetType) throws RuntimeException {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a File into a Java object of the specified type.
         * This method parses a value from the file using the wrapped mapper's settings.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * File configFile = new File("config.json");
         * Configuration config = jsonOps.fromJson(configFile, Configuration.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the file containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws RuntimeException wrapping any IOException that occurs during file reading or deserialization
         */
        public <T> T fromJson(final File json, final Class<? extends T> targetType) throws RuntimeException {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from an InputStream into a Java object of the specified type.
         * Note: with Jackson's default settings the wrapped mapper closes the stream after
         * reading ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * try (FileInputStream fis = new FileInputStream("data.json")) {
         *     DataModel model = jsonOps.fromJson(fis, DataModel.class);
         * }
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the InputStream containing JSON data; closed after reading by Jackson's default auto-close behavior
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final InputStream json, final Class<? extends T> targetType) throws RuntimeException {
            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a Reader into a Java object of the specified type.
         * This method is suitable for character-based input sources. Note: with Jackson's default
         * settings the wrapped mapper closes the reader after reading
         * ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * try (FileReader reader = new FileReader("users.json")) {
         *     UserList users = jsonOps.fromJson(reader, UserList.class);
         * }
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the Reader containing JSON data; closed after reading by Jackson's default auto-close behavior
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         */
        public <T> T fromJson(final Reader json, final Class<? extends T> targetType) throws RuntimeException {
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * URL apiEndpoint = new URL("https://api.example.com/weather");
         * WeatherData weather = jsonOps.fromJson(apiEndpoint, WeatherData.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the URL pointing to JSON data
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}.
         * @throws RuntimeException if opening, reading, or closing the URL stream fails, or its JSON cannot be deserialized to {@code targetType}
         */
        public <T> T fromJson(final URL json, final Class<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(json, cs.json);
            N.checkArgNotNull(targetType, cs.targetType);

            try (InputStream is = json.openStream()) {
                return jsonMapper.readValue(is, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a DataInput into a Java object of the specified type.
         * This method is useful for custom binary protocols.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DataInputStream dis = new DataInputStream(socket.getInputStream());
         * Command command = jsonOps.fromJson((DataInput) dis, Command.class);
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the DataInput containing JSON data
         * @param targetType the class of the target object
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see DataInput
         */
        public <T> T fromJson(final DataInput json, final Class<? extends T> targetType) throws RuntimeException {
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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * byte[] jsonBytes = getJsonArrayBytes();
         * List<Product> products = jsonOps.fromJson(jsonBytes,
         *     new TypeReference<List<Product>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json byte array containing JSON data
         * @param targetType TypeReference describing the target type
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code targetType} is {@code null}.
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final byte[] json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

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
         * <p><b>Usage Examples:</b></p>
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
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}, or {@code len}
         *         is negative.
         * @throws IndexOutOfBoundsException if the requested segment is outside {@code json}
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final byte[] json, final int offset, final int len, final TypeReference<? extends T> targetType)
                throws IllegalArgumentException, IndexOutOfBoundsException, RuntimeException {
            checkByteRange(json, offset, len);

            N.checkArgNotNull(targetType, cs.targetType);

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
         * <p><b>Usage Examples:</b></p>
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
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code targetType} is {@code null}.
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final String json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * File dataFile = new File("complex-data.json");
         * List<Map<String, Object>> complexData = jsonOps.fromJson(dataFile,
         *     new TypeReference<List<Map<String, Object>>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the file containing JSON data
         * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code targetType} is {@code null}.
         * @throws RuntimeException wrapping any IOException that occurs during file reading or deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final File json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from an InputStream into a Java object of the specified generic type.
         * This method handles stream-based input with complex generic types. Note: with Jackson's
         * default settings the wrapped mapper closes the stream after reading
         * ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * try (InputStream is = getClass().getResourceAsStream("/data.json")) {
         *     Set<Category> categories = jsonOps.fromJson(is,
         *         new TypeReference<Set<Category>>() {});
         * }
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the InputStream containing JSON data; closed after reading by Jackson's default auto-close behavior
         * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code targetType} is {@code null}.
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final InputStream json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return jsonMapper.readValue(json, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a Reader into a Java object of the specified generic type.
         * This method combines character-based input with generic type support. Note: with Jackson's
         * default settings the wrapped mapper closes the reader after reading
         * ({@code JsonParser.Feature.AUTO_CLOSE_SOURCE}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * StringReader reader = new StringReader(jsonString);
         * Queue<Task> taskQueue = jsonOps.fromJson(reader,
         *     new TypeReference<Queue<Task>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the Reader containing JSON data; closed after reading by Jackson's default auto-close behavior
         * @param targetType TypeReference describing the target type, can be Bean/Array/Collection/Map
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code targetType} is {@code null}.
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         */
        public <T> T fromJson(final Reader json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

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
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * URL restApi = new URL("https://api.example.com/inventory");
         * List<InventoryItem> inventory = jsonOps.fromJson(restApi,
         *     new TypeReference<List<InventoryItem>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the URL pointing to JSON data
         * @param targetType TypeReference describing the target type
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}.
         * @throws RuntimeException if opening, reading, or closing the URL stream fails, or its JSON cannot be deserialized to {@code targetType}
         * @see TypeReference
         */
        public <T> T fromJson(final URL json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(json, cs.json);
            N.checkArgNotNull(targetType, cs.targetType);

            try (InputStream is = json.openStream()) {
                return jsonMapper.readValue(is, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes JSON from a DataInput into a Java object of the specified generic type.
         * This method supports binary protocol reading with complex generic types.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * DataInputStream dis = new DataInputStream(binaryStream);
         * Map<Long, UserProfile> profiles = jsonOps.fromJson((DataInput) dis,
         *     new TypeReference<Map<Long, UserProfile>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to deserialize to
         * @param json the DataInput containing JSON data
         * @param targetType TypeReference describing the target type
         * @return the deserialized object; {@code null} if JSON contains "null"
         * @throws IllegalArgumentException if {@code targetType} is {@code null}.
         * @throws RuntimeException wrapping any IOException that occurs during deserialization
         * @see TypeReference
         * @see DataInput
         */
        public <T> T fromJson(final DataInput json, final TypeReference<? extends T> targetType) throws IllegalArgumentException, RuntimeException {
            N.checkArgNotNull(targetType, cs.targetType);

            try {
                return jsonMapper.readValue(json, jsonMapper.constructType(targetType));
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }
}
