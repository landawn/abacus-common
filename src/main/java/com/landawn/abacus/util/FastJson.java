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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * A utility class that provides convenient wrapper methods for JSON serialization and deserialization
 * operations using Alibaba's FastJSON2 library. This class serves as a simplified facade for common
 * JSON operations, offering various serialization formats and deserialization options.
 *
 * <p>This utility class supports:
 * <ul>
 * <li>Object to JSON string conversion with optional pretty formatting</li>
 * <li>JSON serialization to files, streams, and writers</li>
 * <li>JSON deserialization from strings, byte arrays, and readers</li>
 * <li>Type-safe deserialization using Class, Type, and TypeReference</li>
 * <li>Customization through JSONWriter and JSONReader features and contexts</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * String json = FastJson.toJson(myObject);
 * MyClass obj = FastJson.fromJson(json, MyClass.class);
 * }</pre>
 *
 * <p>Caller-supplied readers, writers and output streams are never closed. Callers are responsible for closing them. The {@code File} output
 * overloads open and close their own stream.</p>
 *
 * <p>Deserialization reads from a {@code String}, a {@code byte[]} (optionally a range of one) or a
 * {@code Reader}; there is no {@code File} or {@code InputStream} source overload. To read a file, wrap it
 * in a {@code Reader} (for example {@code new FileReader(f, StandardCharsets.UTF_8)}) and close it yourself.</p>
 *
 * <p><b>Argument validation:</b> every method rejects a {@code null} {@code targetType},
 * {@code typeReference}, {@code context}, {@code features} array or {@code output} target with an
 * {@link IllegalArgumentException}; the {@code Reader} and byte-array-range overloads reject a
 * {@code null} source the same way. A {@code null} JSON {@code String} is a legal input and yields
 * {@code null}. Individual methods also document their argument-validation exceptions.</p>
 *
 * <p><b>Parse failures:</b> a {@code fromJson} method normally throws
 * {@link com.alibaba.fastjson2.JSONException} (an unchecked exception) when the input is not well-formed
 * JSON, or when a well-formed document cannot be bound to the requested type. One documented exception to
 * that rule: binding a <i>top-level</i> quoted string that is not a
 * number to {@code Double}, {@code double}, {@code Float} or {@code float} - for example
 * {@code fromJson("\"Infinity\"", Double.class)} - raises an
 * {@link ArrayIndexOutOfBoundsException} from inside FastJSON2 instead. This is an upstream defect, not a
 * deliberate contract; the same string in a nested position ({@code {"d":"Infinity"}}) parses normally, and
 * a {@link java.math.BigDecimal} target reports the expected {@code JSONException}. It is reachable in
 * practice because {@link JsonMappers} writes a non-finite {@code Double} as exactly that top-level quoted
 * token. Catch {@link RuntimeException} rather than {@code JSONException} if you parse untrusted or
 * foreign-produced scalars.</p>
 *
 * <p><b>Unknown properties are ignored.</b> A JSON property with no matching target field is silently
 * discarded. This is the opposite of {@link JsonMappers} / {@link XmlMappers}, which enable Jackson's
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} by default and reject such input.</p>
 *
 * <p><b>Empty and blank input:</b> an empty {@code String}, an empty {@code byte[]}, a zero-length byte range
 * and an empty {@code Reader} all deserialize to {@code null}, as does the JSON literal {@code null}. Input
 * consisting only of whitespace is <i>not</i> empty and fails with a {@code JSONException}. Note that
 * {@link JsonMappers} does not share this convention: it rejects empty input instead of returning
 * {@code null}.</p>
 *
 * <p><b>Dates are written without a UTC offset.</b> {@link java.util.Date}, {@link java.sql.Timestamp} and
 * {@link java.util.Calendar} use FastJSON2's default format - a local wall-clock string such as
 * {@code "2021-01-01 09:00:00"} carrying no offset. The text is rendered in, and read back in, the JVM's
 * default time zone, so a document written in one zone and read in another silently denotes a <i>different
 * instant</i>: written in {@code Asia/Tokyo} and read in {@code UTC} it shifts by nine hours, with no
 * exception. Pass {@link com.alibaba.fastjson2.JSONWriter.Feature#WriterUtilDateAsMillis} to any
 * {@code toJson} overload that accepts features - the output is then byte-identical to what
 * {@link JsonMappers} writes - or set {@code setDateFormat("iso8601")} on a {@code JSONWriter.Context} to
 * get an explicit offset. Note that {@code WriterUtilDateAsMillis} covers {@code java.util.Date} only and
 * leaves {@code Timestamp} textual, and that {@code Context.setZoneId} does <i>not</i> fix this - it
 * stabilises the writer but the text still carries no offset. {@code java.time} types
 * ({@code Instant}, {@code OffsetDateTime}, {@code ZonedDateTime}) are written as ISO-8601 with their own
 * offset and are unaffected. The offsetless form is a property of the <i>format</i>, not of this class:
 * {@link JsonUtil}'s reader drifts on the same text.</p>
 *
 * <p><b>Non-finite values are silently lost.</b> {@code NaN}, {@code Infinity} and {@code -Infinity} are
 * serialized as JSON {@code null} - JSON itself cannot represent them - and become indistinguishable from
 * each other and from a genuine {@code null}. Reading such a document back gives {@code 0.0} where the
 * target is a primitive {@code double}/{@code float} (including {@code double[]}), and {@code null}
 * everywhere else: a boxed {@link Double} property, a {@code Map} value, a {@code List} element or a bare
 * top-level {@link Double}. No exception is raised on either side. Pass
 * {@link com.alibaba.fastjson2.JSONWriter.Feature#WriteFloatSpecialAsString} to any {@code toJson} overload
 * that accepts features to get a lossless document instead - its output is identical to what
 * {@link JsonMappers} writes by default and reads back unchanged. For comparison: {@code JsonMappers} writes
 * the quoted strings {@code "NaN"}/{@code "Infinity"}, {@link XmlMappers} writes bare {@code NaN}/
 * {@code Infinity} text, and {@link JsonUtil} rejects non-finite values with a {@code JSONException}.</p>
 *
 * <p><b>Number types when the target is {@code Map} or {@code Object}.</b> A JSON decimal is bound to a
 * {@link java.math.BigDecimal} here, not a {@link Double} - so {@code 1.5} comes back as
 * {@code BigDecimal}, and {@code -0.0} comes back as {@code BigDecimal 0.0} with its sign lost.
 * {@link JsonMappers} binds the same input to {@link Double} and preserves the sign. Integers agree across
 * both ({@code int}-range to {@link Integer}, wider to {@link Long}, beyond {@code long} to
 * {@link java.math.BigInteger}). Code that casts the result of {@code fromJson(json, Map.class).get(key)}
 * to a concrete numeric type is therefore not portable between the two facades; deserialize into a typed
 * bean when the numeric type matters.</p>
 *
 * <p><b>File failures:</b> a directory target is rejected with an {@link IllegalArgumentException}
 * by {@code IOUtil}'s argument check. Other failures opening or closing the file surface as this
 * library's {@link com.landawn.abacus.exception.UncheckedIOException}; failures writing JSON are
 * wrapped by FastJSON2 in a {@link com.alibaba.fastjson2.JSONException}.</p>
 *
 * <p><b>Byte input must be UTF-8 without a byte-order mark.</b> The {@code byte[]} overloads assume UTF-8:
 * a UTF-16 encoded document, or a UTF-8 document carrying a BOM, fails with a
 * {@link com.alibaba.fastjson2.JSONException}. {@link JsonMappers} does not share this restriction - Jackson
 * auto-detects the encoding and skips a BOM - so a file that {@code JsonMappers} reads may still be rejected
 * here. Strip the BOM, or decode to a {@code String} yourself, before calling.</p>
 *
 * <p><b>File output:</b> the {@code toJson(..., File, ...)} overloads create any missing parent directories
 * of the target file and truncate an existing file. {@link JsonMappers} requires the parent directory to
 * already exist. Truncation happens before serialization runs, so a call that throws part-way leaves the
 * target empty or partially written and its previous content gone. Serialize to a {@code String} first
 * to avoid truncating the destination on a serialization failure; this does not protect against an
 * I/O failure while writing the resulting text.</p>
 *
 * <p><b>Security:</b> This class does not enable automatic type resolution. Enabling
 * {@link JSONReader.Feature#SupportAutoType} allows JSON input to influence the Java type being
 * instantiated; do not enable it for untrusted input without an appropriately restrictive type policy.</p>
 *
 * @see com.alibaba.fastjson2.JSON
 * @see com.alibaba.fastjson2.JSONWriter.Feature
 * @see com.alibaba.fastjson2.JSONReader.Feature
 */
public final class FastJson {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FastJson() {
        // Utility class - prevent instantiation
    }

    /**
     * Validates a byte-array range before passing it to FastJSON2.
     *
     * @throws IllegalArgumentException if {@code json} is {@code null} or {@code len} is negative
     * @throws IndexOutOfBoundsException if {@code offset} is negative or the requested range exceeds {@code json.length}
     */
    private static void checkByteRange(final byte[] json, final int offset, final int len) throws IllegalArgumentException, IndexOutOfBoundsException {
        N.checkArgNotNull(json, cs.json);
        N.checkFromIndexSize(offset, len, json.length);
    }

    /**
     * Converts the specified object to its JSON string representation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = FastJson.toJson(person);
     * // Result: {"age":30,"name":"John"}
     * }</pre>
     *
     * @param obj the object to be converted to JSON string
     * @return the JSON string representation of the object, or the literal string {@code "null"} if the object is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     */
    public static String toJson(final Object obj) throws JSONException {
        return JSON.toJSONString(obj);
    }

    /**
     * Converts the specified object to its JSON string representation with optional pretty formatting.
     * When pretty formatting is enabled, the JSON output will be formatted with proper indentation
     * and line breaks for improved readability.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = FastJson.toJson(person, true);
     * // Result (pretty formatted; FastJSON2 indents with tabs and puts no space after the colon):
     * // {
     * //     "age":30,
     * //     "name":"John"
     * // }
     * }</pre>
     *
     * @param obj the object to be converted to JSON string
     * @param prettyFormat {@code true} to enable pretty formatting with indentation and line breaks, {@code false} for compact output
     * @return the pretty-formatted or compact JSON string representation of the object,
     *         or the literal string {@code "null"} if {@code obj} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     */
    public static String toJson(final Object obj, final boolean prettyFormat) throws JSONException {
        if (prettyFormat) {
            return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
        } else {
            return JSON.toJSONString(obj);
        }
    }

    /**
     * Converts the specified object to its JSON string representation using the specified JSONWriter features.
     * This method allows fine-grained control over the JSON serialization process by specifying various
     * writer features such as pretty formatting, {@code null} value handling, date formatting, etc.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = FastJson.toJson(person, JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteNulls);
     * }</pre>
     *
     * @param obj the object to be converted to JSON string
     * @param features variable number of JSONWriter features to control serialization behavior
     * @return the JSON string representation of the object with the specified features applied,
     *         or the literal string {@code "null"} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if {@code features} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     */
    @SafeVarargs
    public static String toJson(final Object obj, final JSONWriter.Feature... features) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(features, cs.features);

        return JSON.toJSONString(obj, features);
    }

    /**
     * Converts the specified object to its JSON string representation using the specified JSONWriter context.
     * The context provides comprehensive control over serialization behavior including custom serializers,
     * date formats, filters, and other serialization settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONWriter.Context context = new JSONWriter.Context();
     * context.setDateFormat("yyyy-MM-dd");
     * String json = FastJson.toJson(person, context);
     * }</pre>
     *
     * @param obj the object to be converted to JSON string
     * @param context the JSONWriter context containing serialization configuration
     * @return the JSON string representation of the object using the specified context,
     *         or the literal string {@code "null"} if {@code obj} is {@code null}
     * @throws IllegalArgumentException if {@code context} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     */
    public static String toJson(final Object obj, final JSONWriter.Context context) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(context, cs.context);

        return JSON.toJSONString(obj, context);
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified file.
     * This method creates or overwrites the target file with the JSON representation of the object.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * FastJson.toJson(person, new File("person.json"));
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the file where the JSON will be written
     * @throws IllegalArgumentException if {@code output} is {@code null}, or if {@code output} is a directory
     * @throws UncheckedIOException if opening or closing the output file fails
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration,
     *         or writing the JSON to the file fails
     */
    public static void toJson(final Object obj, final File output) throws IllegalArgumentException, UncheckedIOException, JSONException {
        N.checkArgNotNull(output, cs.output);

        try (OutputStream out = IOUtil.newFileOutputStream(output)) {
            JSON.writeTo(out, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified file using the given JSONWriter features.
     * This method provides control over the serialization format while writing directly to a file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * FastJson.toJson(person, new File("person.json"), JSONWriter.Feature.PrettyFormat);
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the file where the JSON will be written
     * @param features variable number of JSONWriter features to control serialization behavior
     * @throws IllegalArgumentException if {@code output} or {@code features} is {@code null}, or if {@code output} is a directory
     * @throws UncheckedIOException if opening or closing the output file fails
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration,
     *         or writing the JSON to the file fails
     */
    @SafeVarargs
    public static void toJson(final Object obj, final File output, final JSONWriter.Feature... features)
            throws IllegalArgumentException, UncheckedIOException, JSONException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNull(features, cs.features);

        try (OutputStream out = IOUtil.newFileOutputStream(output)) {
            JSON.writeTo(out, obj, features);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified file using the given JSONWriter context.
     * This method provides comprehensive control over the serialization process while writing directly to a file.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONWriter.Context context = new JSONWriter.Context();
     * context.setDateFormat("yyyy-MM-dd");
     * FastJson.toJson(person, new File("person.json"), context);
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the file where the JSON will be written
     * @param context the JSONWriter context containing serialization configuration
     * @throws IllegalArgumentException if {@code output} or {@code context} is {@code null}, or if {@code output} is a directory
     * @throws UncheckedIOException if opening or closing the output file fails
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration,
     *         or writing the JSON to the file fails
     */
    public static void toJson(final Object obj, final File output, final JSONWriter.Context context)
            throws IllegalArgumentException, UncheckedIOException, JSONException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNull(context, cs.context);

        try (OutputStream out = IOUtil.newFileOutputStream(output)) {
            JSON.writeTo(out, obj, context);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified OutputStream.
     * The caller is responsible for managing the OutputStream lifecycle (opening and closing).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * try (OutputStream output = new java.io.FileOutputStream("person.json")) {
     *     FastJson.toJson(person, output);
     * }
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the OutputStream where the JSON will be written
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     *         or writing JSON to {@code output} fails
     */
    public static void toJson(final Object obj, final OutputStream output) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(output, cs.output);

        JSON.writeTo(output, obj);
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified OutputStream using the given JSONWriter features.
     * The caller is responsible for managing the OutputStream lifecycle.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * FastJson.toJson(person, outputStream, JSONWriter.Feature.PrettyFormat);
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the OutputStream where the JSON will be written
     * @param features variable number of JSONWriter features to control serialization behavior
     * @throws IllegalArgumentException if {@code output} or {@code features} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     *         or writing JSON to {@code output} fails
     */
    @SafeVarargs
    public static void toJson(final Object obj, final OutputStream output, final JSONWriter.Feature... features)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNull(features, cs.features);

        JSON.writeTo(output, obj, features);
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified OutputStream using the given JSONWriter context.
     * The caller is responsible for managing the OutputStream lifecycle.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONWriter.Context context = new JSONWriter.Context();
     * FastJson.toJson(person, outputStream, context);
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the OutputStream where the JSON will be written
     * @param context the JSONWriter context containing serialization configuration
     * @throws IllegalArgumentException if {@code output} or {@code context} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     *         or writing JSON to {@code output} fails
     */
    public static void toJson(final Object obj, final OutputStream output, final JSONWriter.Context context) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNull(context, cs.context);

        JSON.writeTo(output, obj, context);
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified Writer.
     * The JSON string is first generated in memory and then written to the Writer.
     * The caller is responsible for managing the Writer lifecycle.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * try (Writer output = new java.io.FileWriter("person.json")) {
     *     FastJson.toJson(person, output);
     * }
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the Writer where the JSON will be written
     * @throws IllegalArgumentException if {@code output} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     * @throws RuntimeException if writing the serialized JSON to {@code output} fails
     */
    public static void toJson(final Object obj, final Writer output) throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(output, cs.output);

        final String json = JSON.toJSONString(obj);

        try {
            output.write(json);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified Writer using the given JSONWriter features.
     * The JSON string is generated with the specified features and then written to the Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * FastJson.toJson(person, writer, JSONWriter.Feature.PrettyFormat);
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the Writer where the JSON will be written
     * @param features variable number of JSONWriter features to control serialization behavior
     * @throws IllegalArgumentException if {@code output} or {@code features} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     * @throws RuntimeException if writing the serialized JSON to {@code output} fails
     */
    @SafeVarargs
    public static void toJson(final Object obj, final Writer output, final JSONWriter.Feature... features)
            throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNull(features, cs.features);

        final String json = JSON.toJSONString(obj, features);

        try {
            output.write(json);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to JSON and writes it to the specified Writer using the given JSONWriter context.
     * The JSON string is generated with the specified context and then written to the Writer.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONWriter.Context context = new JSONWriter.Context();
     * FastJson.toJson(person, writer, context);
     * }</pre>
     *
     * @param obj the object to be serialized to JSON
     * @param output the Writer where the JSON will be written
     * @param context the JSONWriter context containing serialization configuration
     * @throws IllegalArgumentException if {@code output} or {@code context} is {@code null}
     * @throws JSONException if FastJSON2 cannot serialize {@code obj} using the selected configuration
     * @throws RuntimeException if writing the serialized JSON to {@code output} fails
     */
    public static void toJson(final Object obj, final Writer output, final JSONWriter.Context context)
            throws IllegalArgumentException, JSONException, RuntimeException {
        N.checkArgNotNull(output, cs.output);
        N.checkArgNotNull(context, cs.context);

        final String json = JSON.toJSONString(obj, context);

        try {
            output.write(json);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes JSON from a byte array into an object of the specified target type.
     * This method is useful when working with JSON data received as byte arrays, such as
     * from network communications or binary storage.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] jsonBytes = "{\"name\":\"John\",\"age\":30}".getBytes();
     * Person person = FastJson.fromJson(jsonBytes, Person.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the byte array containing JSON data
     * @param targetType the Class object representing the target type
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final byte[] json, final Class<? extends T> targetType) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(targetType, cs.targetType);

        return JSON.parseObject(json, targetType);
    }

    /**
     * Deserializes JSON from a byte array segment into an object of the specified target type.
     * This method allows parsing JSON from a specific portion of a byte array, which is useful
     * when the JSON data is embedded within a larger byte array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] buffer = "prefix{\"name\":\"John\",\"age\":30}suffix".getBytes();
     * Person person = FastJson.fromJson(buffer, 6, 24, Person.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the byte array containing JSON data
     * @param offset the starting position in the byte array
     * @param len the number of bytes to read from the starting position
     * @param targetType the Class object representing the target type
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}, or {@code len} is negative
     * @throws IndexOutOfBoundsException if {@code offset} is negative, or if {@code offset + len}
     *         is greater than {@code json.length}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final byte[] json, final int offset, final int len, final Class<? extends T> targetType)
            throws IllegalArgumentException, IndexOutOfBoundsException, JSONException {
        checkByteRange(json, offset, len);
        N.checkArgNotNull(targetType, cs.targetType);

        return JSON.parseObject(json, offset, len, targetType);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target type.
     * This is the most commonly used deserialization method for simple object types.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = FastJson.fromJson(json, Person.class);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Class object representing the target type
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final String json, final Class<? extends T> targetType) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(targetType, cs.targetType);

        return JSON.parseObject(json, targetType);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target type using the given JSONReader features.
     * This method allows customization of the deserialization process through various reader features.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"name\":\"John\",\"age\":30}";
     * Person person = FastJson.fromJson(json, Person.class, JSONReader.Feature.SupportSmartMatch);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Class object representing the target type
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code targetType} or {@code features} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    @SafeVarargs
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final JSONReader.Feature... features)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(features, cs.features);

        return JSON.parseObject(json, targetType, features);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target type using the given JSONReader context.
     * The context provides comprehensive control over deserialization behavior including custom deserializers,
     * date formats, and other parsing settings.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * Person person = FastJson.fromJson(json, Person.class, context);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Class object representing the target type
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code targetType} or {@code context} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final JSONReader.Context context)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(context, cs.context);

        return JSON.parseObject(json, targetType, context);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target Type.
     * This method is useful for deserializing generic types or complex type structures
     * where Class objects are insufficient.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]";
     * Type listType = new TypeReference<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(json, listType);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Type object representing the target type
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code targetType} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final String json, final Type targetType) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(targetType, cs.targetType);

        return JSON.parseObject(json, targetType);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target Type using the given JSONReader features.
     * This method combines the flexibility of Type-based deserialization with customizable reader features.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type listType = new TypeReference<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(json, listType, JSONReader.Feature.SupportSmartMatch);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Type object representing the target type
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code targetType} or {@code features} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    @SafeVarargs
    public static <T> T fromJson(final String json, final Type targetType, final JSONReader.Feature... features)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(features, cs.features);

        return JSON.parseObject(json, targetType, features);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target Type using the given JSONReader context.
     * This method provides the most comprehensive control over Type-based deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * Type listType = new TypeReference<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(json, listType, context);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Type object representing the target type
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code targetType} or {@code context} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final String json, final Type targetType, final JSONReader.Context context) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(context, cs.context);

        return JSON.parseObject(json, targetType, context);
    }

    /**
     * Deserializes JSON from a string into an object of the type specified by the TypeReference.
     * TypeReference is particularly useful for preserving generic type information that would
     * otherwise be lost due to type erasure.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]";
     * List<Person> people = FastJson.fromJson(json, new TypeReference<List<Person>>(){});
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param typeReference the TypeReference object containing type information
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code typeReference} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final String json, final TypeReference<T> typeReference) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(typeReference, cs.typeReference);

        return JSON.parseObject(json, typeReference);
    }

    /**
     * Deserializes JSON from a string into an object of the type specified by the TypeReference using the given JSONReader features.
     * This method combines the type safety of TypeReference with customizable reader features.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "[{\"name\":\"John\"},{\"name\":\"Jane\"}]";
     * List<Person> people = FastJson.fromJson(json, new TypeReference<List<Person>>(){}, JSONReader.Feature.SupportSmartMatch);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param typeReference the TypeReference object containing type information
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code typeReference} or {@code features} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    @SafeVarargs
    public static <T> T fromJson(final String json, final TypeReference<T> typeReference, final JSONReader.Feature... features)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(typeReference, cs.typeReference);
        N.checkArgNotNull(features, cs.features);

        return JSON.parseObject(json, typeReference, features);
    }

    /**
     * Deserializes JSON from a string into an object of the type specified by the TypeReference using the given JSONReader context.
     * This method provides comprehensive control over TypeReference-based deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * List<Person> people = FastJson.fromJson(json, new TypeReference<List<Person>>(){}, context);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param typeReference the TypeReference object containing type information
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code typeReference} or {@code context} is {@code null}
     * @throws JSONException if the JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final String json, final TypeReference<T> typeReference, final JSONReader.Context context)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(typeReference, cs.typeReference);
        N.checkArgNotNull(context, cs.context);

        return JSON.parseObject(json, typeReference.getType(), context);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target type.
     * This method is useful when reading JSON from various input sources such as files,
     * network streams, or other character-based input sources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new java.io.FileReader("person.json")) {
     *     Person person = FastJson.fromJson(reader, Person.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the caller-owned Reader containing JSON data; this method never closes it
     * @param targetType the Class object representing the target type
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}
     * @throws JSONException if reading {@code json} fails, the JSON is malformed, or it cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);

        return JSON.parseObject(nonClosingReader(json), targetType);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target type using the given JSONReader features.
     * This method allows customization of the deserialization process when reading from character-based sources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new java.io.FileReader("person.json")) {
     *     Person person = FastJson.fromJson(reader, Person.class, JSONReader.Feature.SupportSmartMatch);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the caller-owned Reader containing JSON data; this method never closes it
     * @param targetType the Class object representing the target type
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code json}, {@code targetType}, or {@code features} is {@code null}
     * @throws JSONException if reading {@code json} fails, the JSON is malformed, or it cannot be bound to the requested type
     */
    @MayReturnNull
    @SafeVarargs
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType, final JSONReader.Feature... features)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(features, cs.features);

        return JSON.parseObject(nonClosingReader(json), targetType, features);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target type using the given JSONReader context.
     * This method reads the entire content of the Reader into a string first, then parses it with the
     * specified context. The caller is responsible for closing the Reader.
     *
     * <p><b>Note:</b> This method is marked {@link Beta} because it buffers the full Reader content
     * into memory before parsing, which may be unsuitable for very large inputs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * Person person = FastJson.fromJson(reader, Person.class, context);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the caller-owned Reader containing JSON data; this method never closes it; its full content is read into memory
     * @param targetType the Class object representing the target type
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code json}, {@code targetType}, or {@code context} is {@code null}
     * @throws UncheckedIOException if reading the JSON text from {@code json} fails
     * @throws JSONException if the buffered JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    @Beta
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType, final JSONReader.Context context)
            throws IllegalArgumentException, UncheckedIOException, JSONException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(context, cs.context);

        return JSON.parseObject(IOUtil.readAllToString(json), targetType, context);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target Type.
     * This method is useful for deserializing generic types or complex type structures
     * from character-based input sources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new java.io.FileReader("people.json")) {
     *     Type listType = new TypeReference<List<Person>>(){}.getType();
     *     List<Person> people = FastJson.fromJson(reader, listType);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the caller-owned Reader containing JSON data; this method never closes it
     * @param targetType the Type object representing the target type
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code json} or {@code targetType} is {@code null}
     * @throws JSONException if reading {@code json} fails, the JSON is malformed, or it cannot be bound to the requested type
     */
    @MayReturnNull
    public static <T> T fromJson(final Reader json, final Type targetType) throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);

        return JSON.parseObject(nonClosingReader(json), targetType);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target Type using the given JSONReader features.
     * This method combines the flexibility of Type-based deserialization with customizable reader features
     * for character-based input sources.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Reader reader = new java.io.FileReader("people.json")) {
     *     Type listType = new TypeReference<List<Person>>(){}.getType();
     *     List<Person> people = FastJson.fromJson(reader, listType, JSONReader.Feature.SupportSmartMatch);
     * }
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the caller-owned Reader containing JSON data; this method never closes it
     * @param targetType the Type object representing the target type
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code json}, {@code targetType}, or {@code features} is {@code null}
     * @throws JSONException if reading {@code json} fails, the JSON is malformed, or it cannot be bound to the requested type
     */
    @MayReturnNull
    @SafeVarargs
    public static <T> T fromJson(final Reader json, final Type targetType, final JSONReader.Feature... features)
            throws IllegalArgumentException, JSONException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(features, cs.features);

        return JSON.parseObject(nonClosingReader(json), targetType, features);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target Type using the given JSONReader context.
     * This method reads the entire content of the Reader into a string first, then parses it with the
     * specified context. The caller is responsible for closing the Reader.
     *
     * <p><b>Note:</b> This method is marked {@link Beta} because it buffers the full Reader content
     * into memory before parsing, which may be unsuitable for very large inputs.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * Type listType = new TypeReference<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(reader, listType, context);
     * }</pre>
     *
     * @param <T> the type of the target object
     * @param json the caller-owned Reader containing JSON data; this method never closes it; its full content is read into memory
     * @param targetType the Type object representing the target type
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or {@code null} if the JSON represents null
     * @throws IllegalArgumentException if {@code json}, {@code targetType}, or {@code context} is {@code null}
     * @throws UncheckedIOException if reading the JSON text from {@code json} fails
     * @throws JSONException if the buffered JSON is malformed or cannot be bound to the requested type
     */
    @MayReturnNull
    @Beta
    public static <T> T fromJson(final Reader json, final Type targetType, final JSONReader.Context context)
            throws IllegalArgumentException, UncheckedIOException, JSONException {
        N.checkArgNotNull(json, cs.json);
        N.checkArgNotNull(targetType, cs.targetType);
        N.checkArgNotNull(context, cs.context);

        return JSON.parseObject(IOUtil.readAllToString(json), targetType, context);
    }

    private static Reader nonClosingReader(final Reader source) {
        // FastJSON closes its parser-owned reader; shield the caller-owned source on every exit path.
        return new java.io.FilterReader(source) {
            @Override
            public void close() {
                // Ownership remains with the caller.
            }
        };
    }
}
