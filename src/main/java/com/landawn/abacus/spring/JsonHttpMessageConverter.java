/*
 * Copyright (C) 2020 HaiYang Li
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

package com.landawn.abacus.spring;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJsonHttpMessageConverter;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.type.EnumType;
import com.landawn.abacus.type.ImmutableMapEntryType;
import com.landawn.abacus.type.IndexedType;
import com.landawn.abacus.type.MapEntryType;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.PairType;
import com.landawn.abacus.type.TimedType;
import com.landawn.abacus.type.TripleType;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

/**
 * Spring HTTP message converter for JSON serialization and deserialization using abacus-common JSON utilities.
 * This converter integrates Abacus's JSON processing capabilities with Spring's HTTP message conversion framework,
 * allowing seamless conversion between Java objects and JSON in Spring MVC and other
 * {@code HttpMessageConverter}-based clients such as {@code RestTemplate}.
 *
 * <p>This converter extends Spring's {@link AbstractJsonHttpMessageConverter} and delegates the actual
 * JSON processing to Abacus's {@link N} utility class.</p>
 *
 * <p>The converter supports reading JSON from HTTP requests and writing JSON to HTTP responses,
 * handling all standard Java types as well as custom POJOs. It automatically handles content type
 * negotiation for "application/json" and related media types.</p>
 *
 * <p>Root JDK and Abacus {@code Optional} values and Abacus {@code Nullable} values use the JSON
 * representation of their contained value, including arrays and objects. Reading retains the
 * declared generic element type. Empty wrappers and present-null {@code Nullable} values write
 * JSON {@code null}; reading that token produces an empty outer wrapper.</p>
 *
 * <p>{@code Holder} roots likewise use their contained value; JSON {@code null} reads as a holder
 * containing null. Entries, pairs, tuples, indexed/timed values, primitive lists and custom value
 * objects retain their type handler's structured representation and conversion rules. Their nested
 * configuration behavior is the same as direct Abacus parser use. Custom value accessors should be
 * side-effect-free: detecting a custom structured representation can invoke an accessor twice.</p>
 *
 * <p><b>Usage Examples in Spring Configuration:</b></p>
 * <pre>{@code
 * @Configuration
 * @EnableWebMvc
 * public class WebConfig implements WebMvcConfigurer {
 *
 *     @Override
 *     public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
 *         converters.add(new JsonHttpMessageConverter());
 *     }
 * }
 * }</pre>
 *
 * <p><b>Usage Examples with RestTemplate:</b></p>
 * <pre>{@code
 * RestTemplate restTemplate = new RestTemplate();
 * restTemplate.getMessageConverters().add(0, new JsonHttpMessageConverter());
 *
 * // Now the RestTemplate will use abacus-common for JSON processing
 * MyObject result = restTemplate.getForObject("https://api.example.com/data", MyObject.class);
 * }</pre>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>High-performance JSON processing using abacus-common utilities</li>
 *   <li>Support for complex generic types through TypeFactory</li>
 *   <li>Seamless integration with Spring MVC and {@code HttpMessageConverter}-based clients</li>
 *   <li>Automatic content type handling for JSON media types</li>
 *   <li>Suitable for singleton usage once its mutable parser configurations and inherited
 *       supported-media-type list have been fully configured</li>
 * </ul>
 *
 * <p><b>Thread safety:</b> the supplied {@link JsonSerConfig} and {@link JsonDeserConfig} instances
 * are retained by reference. A converter may be shared safely after construction provided those
 * configurations, and inherited converter settings such as supported media types, are not mutated
 * concurrently with request processing.</p>
 *
 * <p>Scalar roots use JSON value syntax: strings are quoted and escaped, and {@code null} is written
 * as the JSON literal. Scalar input must contain one valid JSON value; empty bodies and nonstandard
 * scalar syntax are rejected. Scalar serialization settings must also produce valid JSON (for example,
 * nonfinite numbers are rejected).</p>
 *
 * <p><b>Serialization settings that cannot produce JSON are rejected outright</b>, so a bean, map or
 * collection root obeys the same "emit valid JSON" rule as a scalar root rather than quietly writing
 * something no JSON parser accepts. A {@link JsonSerConfig} whose string or char quotation is not
 * {@code '"'}, or which has {@code quotePropName}, {@code quoteMapKey} or {@code bracketRootValue}
 * turned off, is refused with an {@link IllegalArgumentException} - by the constructors, and again on
 * each write, because the configuration is mutable and is retained by reference.</p>
 *
 * <p><b>Supported media types:</b> by default the converter handles the media types inherited from
 * {@link AbstractJsonHttpMessageConverter} (typically {@code application/json} and
 * {@code application/*+json}). Constructors accepting {@link MediaType} values replace those defaults
 * when given a non-empty list or array; include the default types explicitly to retain them alongside
 * custom types such as {@code text/json}. A {@code null} or empty list or array retains the defaults.
 * The supported media types may also be changed after construction via the
 * inherited {@code setSupportedMediaTypes(List)} method.</p>
 *
 * <p><b>Note on naming:</b> the class is intentionally named {@code JsonHttpMessageConverter} (without
 * an {@code Abacus} prefix) for backward compatibility. There is currently no sibling Spring
 * converter for other formats in this package, so the unprefixed name is unambiguous in practice.</p>
 *
 * @see AbstractJsonHttpMessageConverter
 * @see N#fromJson(Reader, JsonDeserConfig, com.landawn.abacus.type.Type)
 * @see N#toJson(Object, JsonSerConfig, Writer)
 * @see JsonSerConfig
 * @see JsonDeserConfig
 */
public class JsonHttpMessageConverter extends AbstractJsonHttpMessageConverter {

    private static final Pattern JSON_NUMBER = Pattern.compile("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?");

    private final JsonSerConfig jsc;
    private final JsonDeserConfig jdc;

    /**
     * Constructs a new JsonHttpMessageConverter with default configuration.
     * The converter will handle "application/json" and related JSON media types by default.
     *
     * <p>This constructor initializes the converter with standard JSON media type support
     * inherited from {@link AbstractJsonHttpMessageConverter}, including:</p>
     * <ul>
     *   <li>application/json</li>
     *   <li>application/*+json</li>
     * </ul>
     *
     * <p>The converter can be used as a singleton after configuration is complete. Its parser
     * configurations and inherited converter settings must not be mutated concurrently with request
     * processing. It integrates with Spring's content negotiation mechanism to handle JSON
     * serialization and deserialization for REST endpoints.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * RestTemplate restTemplate = new RestTemplate();
     * restTemplate.getMessageConverters().add(0, new JsonHttpMessageConverter());
     * }</pre>
     *
     */
    public JsonHttpMessageConverter() {
        this(new JsonSerConfig(), new JsonDeserConfig());
    }

    /**
     * Constructs a new JsonHttpMessageConverter with custom serialization and deserialization configurations.
     * This constructor allows fine-grained control over JSON processing behavior, including field exclusion,
     * date formatting, {@code null} handling, and other serialization/deserialization options.
     *
     * <p>Use this constructor when you need to customize the JSON processing behavior beyond the defaults.
     * Common customizations include:</p>
     * <ul>
     *   <li>Excluding {@code null} or default values from serialization output</li>
     *   <li>Customizing date/time formatting patterns</li>
     *   <li>Ignoring unknown properties during deserialization</li>
     *   <li>Specifying property inclusion/exclusion rules</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Configure serialization to exclude null values
     * JsonSerConfig serConfig = new JsonSerConfig()
     *     .setExclusion(Exclusion.NULL)
     *     .setSkipTransientField(true);
     *
     * // Configure deserialization to ignore unknown properties
     * JsonDeserConfig deserConfig = new JsonDeserConfig()
     *     .setIgnoreUnmatchedProperty(true);
     *
     * JsonHttpMessageConverter converter = new JsonHttpMessageConverter(serConfig, deserConfig);
     *
     * // Use in Spring configuration
     * restTemplate.getMessageConverters().add(0, converter);
     * }</pre>
     *
     * @param jsc the serialization configuration controlling how Java objects are converted to JSON.
     *            Must not be {@code null}. Use {@link JsonSerConfig} to customize serialization behavior.
     * @param jdc the deserialization configuration controlling how JSON is converted to Java objects.
     *            Must not be {@code null}. Use {@link JsonDeserConfig} to customize deserialization behavior.
     *            Both configuration objects are retained by reference and should not be mutated while
     *            the converter is serving concurrent requests.
     * @throws IllegalArgumentException if {@code jsc} or {@code jdc} is {@code null}, or if {@code jsc} cannot
     *         produce valid JSON (a string or char quotation other than {@code '"'}, or
     *         {@code quotePropName}/{@code quoteMapKey}/{@code bracketRootValue} turned off).
     * @see JsonSerConfig
     * @see JsonDeserConfig
     * @see com.landawn.abacus.parser.Exclusion
     */
    public JsonHttpMessageConverter(final JsonSerConfig jsc, final JsonDeserConfig jdc) throws IllegalArgumentException {
        N.checkArgNotNull(jsc, cs.jsc);
        N.checkArgNotNull(jdc, cs.jdc);
        checkJsonCapable(jsc);

        this.jsc = jsc;
        this.jdc = jdc;
    }

    /**
     * Constructs a new JsonHttpMessageConverter with default JSON configurations and the specified
     * supported media types.
     * This constructor is a convenience for setting the supported media types at construction time
     * instead of calling the inherited {@code setSupportedMediaTypes(List)} afterwards. It is useful
     * when the converter must advertise additional or non-standard JSON media types (for example a
     * vendor {@code +json} type such as {@code application/vnd.api+json}, or {@code text/json}).
     *
     * <p>A non-empty {@code supportedMediaTypes} array replaces the parent's default media types.
     * If the array is {@code null} or empty, those defaults are retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonHttpMessageConverter converter = new JsonHttpMessageConverter(
     *     MediaType.APPLICATION_JSON,
     *     new MediaType("application", "vnd.api+json"));
     * }</pre>
     *
     * @param supportedMediaTypes the media types this converter should support. If {@code null} or empty,
     *                            the inherited default media types are kept unchanged.
     * @see #JsonHttpMessageConverter(JsonSerConfig, JsonDeserConfig, MediaType...)
     */
    @SafeVarargs
    public JsonHttpMessageConverter(final MediaType... supportedMediaTypes) {
        this(new JsonSerConfig(), new JsonDeserConfig(), supportedMediaTypes);
    }

    /**
     * Constructs a new JsonHttpMessageConverter with custom serialization and deserialization
     * configurations and the specified supported media types.
     * This constructor combines full control over JSON processing behavior with control over the
     * media types the converter advertises, all at construction time.
     *
     * <p>A non-empty {@code supportedMediaTypes} array replaces the parent's default media types.
     * If the array is {@code null} or empty, those defaults are retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerConfig serConfig = new JsonSerConfig().setExclusion(Exclusion.NULL);
     * JsonDeserConfig deserConfig = new JsonDeserConfig().setIgnoreUnmatchedProperty(true);
     *
     * JsonHttpMessageConverter converter = new JsonHttpMessageConverter(
     *     serConfig, deserConfig,
     *     MediaType.APPLICATION_JSON,
     *     new MediaType("application", "vnd.api+json"));
     * }</pre>
     *
     * @param jsc the serialization configuration controlling how Java objects are converted to JSON. Must not be {@code null}.
     * @param jdc the deserialization configuration controlling how JSON is converted to Java objects. Must not be {@code null}.
     * @param supportedMediaTypes the media types this converter should support. If {@code null} or empty,
     *                            the inherited default media types are kept unchanged.
     * @throws IllegalArgumentException if {@code jsc} or {@code jdc} is {@code null}, or if {@code jsc} cannot
     *         produce valid JSON (a string or char quotation other than {@code '"'}, or
     *         {@code quotePropName}/{@code quoteMapKey}/{@code bracketRootValue} turned off).
     * @see JsonSerConfig
     * @see JsonDeserConfig
     */
    @SafeVarargs
    public JsonHttpMessageConverter(final JsonSerConfig jsc, final JsonDeserConfig jdc, final MediaType... supportedMediaTypes)
            throws IllegalArgumentException {
        N.checkArgNotNull(jsc, cs.jsc);
        N.checkArgNotNull(jdc, cs.jdc);
        checkJsonCapable(jsc);

        this.jsc = jsc;
        this.jdc = jdc;

        if (supportedMediaTypes != null && supportedMediaTypes.length > 0) {
            setSupportedMediaTypes(Arrays.asList(supportedMediaTypes));
        }
    }

    /**
     * Constructs a new JsonHttpMessageConverter with custom serialization and deserialization
     * configurations and the specified supported media types.
     * This {@link List}-based overload behaves identically to
     * {@link #JsonHttpMessageConverter(JsonSerConfig, JsonDeserConfig, MediaType...)}.
     *
     * <p>A non-empty {@code supportedMediaTypes} list replaces the parent's default media types.
     * If the list is {@code null} or empty, those defaults are retained.</p>
     *
     * @param jsc the serialization configuration controlling how Java objects are converted to JSON. Must not be {@code null}.
     * @param jdc the deserialization configuration controlling how JSON is converted to Java objects. Must not be {@code null}.
     * @param supportedMediaTypes the media types this converter should support. If {@code null} or empty,
     *                            the inherited default media types are kept unchanged.
     * @throws IllegalArgumentException if {@code jsc} or {@code jdc} is {@code null}, or if {@code jsc} cannot
     *         produce valid JSON (a string or char quotation other than {@code '"'}, or
     *         {@code quotePropName}/{@code quoteMapKey}/{@code bracketRootValue} turned off).
     * @see JsonSerConfig
     * @see JsonDeserConfig
     */
    public JsonHttpMessageConverter(final JsonSerConfig jsc, final JsonDeserConfig jdc, final List<MediaType> supportedMediaTypes)
            throws IllegalArgumentException {
        N.checkArgNotNull(jsc, cs.jsc);
        N.checkArgNotNull(jdc, cs.jdc);
        checkJsonCapable(jsc);

        this.jsc = jsc;
        this.jdc = jdc;

        if (N.notEmpty(supportedMediaTypes)) {
            setSupportedMediaTypes(supportedMediaTypes);
        }
    }

    /**
     * Reads JSON content from the provided Reader and deserializes it into an object of the specified type.
     * This method is called by Spring's HTTP message conversion framework when processing incoming JSON requests.
     *
     * <p>The method uses Abacus's JSON deserialization capabilities through {@link N#fromJson(Reader, JsonDeserConfig, com.landawn.abacus.type.Type)}
     * to convert the JSON content into the appropriate Java object. The TypeFactory is used to handle complex
     * generic types properly, ensuring that parameterized types (such as {@code List<User>} or {@code Map<String, Object>})
     * are correctly deserialized with their full type information preserved.</p>
     *
     * <p><b>Supported Types:</b></p>
     * <ul>
     *   <li>All primitive types and their wrappers (int, Integer, boolean, Boolean, etc.)</li>
     *   <li>Standard Java types (String, Date, BigDecimal, etc.)</li>
     *   <li>Collections (List, Set, Map) with generic type preservation</li>
     *   <li>Custom POJOs with public fields or JavaBean properties</li>
     *   <li>Arrays and nested complex types</li>
     * </ul>
     *
     * <p><b>Example usage in Spring Controller:</b></p>
     * <pre>{@code
     * @PostMapping("/users")
     * public ResponseEntity<User> createUser(@RequestBody User user) {
     *     // The user object is automatically deserialized by this method
     *     return ResponseEntity.ok(user);
     * }
     * }</pre>
     *
     * <p><b>Example JSON input for User type:</b></p>
     * <pre>{@code
     * {
     *   "id": 123,
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "active": true,
     *   "roles": ["admin", "user"]
     * }
     * }</pre>
     *
     * <p><b>Shape leniency:</b> a JSON object body read into a {@code List<T>} target is handed to the
     * abacus parser unchanged, which yields a one-element list whose element is a {@code Map} - the
     * declared element type is not enforced. Callers that need strictness must validate the result.
     * Spring's {@code read(...)} wraps every exception thrown here in
     * {@code org.springframework.http.converter.HttpMessageNotReadableException}, keeping the original as
     * its cause.</p>
     *
     * @param resolvedType the target type to deserialize the JSON into, including generic type information.
     *                     This is the actual runtime type resolved from the method signature or type parameter.
     * @param reader the Reader containing the JSON content to be deserialized. This method leaves it
     *               open; the caller or HTTP infrastructure retains responsibility for the input resource.
     * @return the deserialized object of the specified type
     * @throws ParsingException if the content is not one JSON value of the
     *         required shape (for example an array body for a bean target, or an empty body for a scalar target)
     * @throws UncheckedIOException if reading JSON characters from {@code reader} fails while converting the HTTP message body
     * @throws RuntimeException (for example {@link NumberFormatException}) if a JSON token cannot be
     *         converted to the target or element type, or if the JSON is otherwise malformed
     */
    @Override
    protected Object readInternal(final Type resolvedType, final Reader reader) throws ParsingException, UncheckedIOException, RuntimeException {
        return readJson(TypeFactory.getType(resolvedType), reader);
    }

    /**
     * @throws UncheckedIOException if reading from the request reader fails
     * @throws ParsingException if the request does not contain exactly one JSON value compatible with the target type
     */
    private Object readJson(final com.landawn.abacus.type.Type<?> targetType, final Reader reader) throws UncheckedIOException, ParsingException {
        try {
            final PushbackReader input = new PushbackReader(reader);
            int first;

            do {
                first = input.read();
            } while (isJsonWhitespace(first));

            if (first != -1) {
                input.unread(first);
            }

            final Class<?> targetClass = targetType.javaType();
            if ((targetClass == java.util.Optional.class || targetClass == Optional.class || targetClass == Nullable.class || targetClass == Holder.class)
                    && first != 'n' && first != -1) {
                // Generic wrappers can contain any JSON shape. Decode their declared element type;
                // keep literal null on the strict wrapper path below so the outer wrapper stays empty,
                // including nested wrappers and configurations that read null strings as empty strings.
                final Object value = readJson(targetType.elementType(), input);
                if (targetClass == Holder.class) {
                    return Holder.of(value);
                }
                if (targetClass == java.util.Optional.class) {
                    return java.util.Optional.ofNullable(value);
                }
                return targetClass == Optional.class ? Optional.ofNullable(value) : Nullable.of(value);
            }

            if (first == '{' || first == '[') {
                if (!usesDirectValueConversion(targetType)) {
                    return N.fromJson(input, jdc, targetType);
                }
                if (isStructuredValueType(targetType) || isCustomValueType(targetType)) {
                    final String json = IOUtil.readAllToString(input);
                    // Direct type conversion consumes raw text and bypasses the parser's EOF check.
                    // Validate a complete root independently before applying its declared generic type.
                    N.fromJson(json, new JsonDeserConfig(), Object.class);
                    return N.fromJson(json, jdc, targetType);
                }
            } else if (!usesDirectValueConversion(targetType) && first == -1 && !targetType.isObject()) {
                return N.fromJson(input, jdc, targetType);
            }

            final String source = IOUtil.readAllToString(input);
            int end = source.length();
            while (end > 0 && isJsonWhitespace(source.charAt(end - 1))) {
                end--;
            }
            final String scalar = source.substring(0, end);
            if (!isJsonScalar(scalar) || (!targetType.isObject() && !usesDirectValueConversion(targetType) && !"null".equals(scalar))) {
                throw new ParsingException("Expected one JSON scalar value");
            }
            if (targetClass == Holder.class && "null".equals(scalar)) {
                return Holder.of(null);
            }

            // The parser's root scalar shortcut converts raw text. A singleton array selects its
            // JSON token conversion instead. Disable filtering only on this private scalar wrapper
            // so null/empty values retain their slot, without changing the caller's configuration.
            final JsonDeserConfig scalarConfig = jdc.copy().setElementType(targetType).setIgnoreNullOrEmpty(false);
            final Object[] values = N.fromJson("[" + scalar + "]", scalarConfig, Object[].class);
            if (values.length != 1) {
                throw new ParsingException("Expected one JSON scalar value");
            }
            return values[0];
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static boolean usesDirectValueConversion(final com.landawn.abacus.type.Type<?> type) {
        return type.isSerializable() && !type.isArray() && !type.isCollection();
    }

    private static boolean isStructuredValueType(final com.landawn.abacus.type.Type<?> type) {
        // These direct handlers encode containers despite reporting neither isArray nor isCollection.
        // Keep their root parser path: erased element handlers can otherwise quote nested JSON values.
        return type.isPrimitiveList() || type instanceof MapEntryType<?, ?> || type instanceof ImmutableMapEntryType<?, ?> || type instanceof TimedType<?>
                || type instanceof IndexedType<?> || type instanceof PairType<?, ?> || type instanceof TripleType<?, ?, ?>
                || Tuple.class.isAssignableFrom(type.javaType())
                // Multiset and Multimap match the rule above exactly - isArray/isCollection/isMap all false,
                // isSerializable true, and stringOf emits JSON object text - but were missing, so the writer
                // took the scalar path and emitted the container's JSON as a quoted, escaped *string*
                // ("{\"x\": 2}" instead of {"x": 2}), while the reader rejected a well-formed object body
                // with "Expected one JSON scalar value". Multimap covers ListMultimap/SetMultimap.
                || Multiset.class.isAssignableFrom(type.javaType()) || Multimap.class.isAssignableFrom(type.javaType());
    }

    private static boolean isCustomValueType(final com.landawn.abacus.type.Type<?> type) {
        return type instanceof ObjectType<?> || type instanceof EnumType<?>;
    }

    private static boolean isJsonWhitespace(final int ch) {
        return ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n';
    }

    private static boolean isJsonScalar(final String value) {
        if (value.isEmpty()) {
            return false;
        }
        if (value.charAt(0) != '"') {
            return "null".equals(value) || "true".equals(value) || "false".equals(value) || JSON_NUMBER.matcher(value).matches();
        }

        // Scan strings iteratively: a repeated regex alternative can overflow the stack on large
        // HTTP values. Validate escapes before using the parser, which also accepts non-JSON syntax.
        for (int i = 1; i < value.length(); i++) {
            final char ch = value.charAt(i);
            if (ch == '"') {
                return i == value.length() - 1;
            }
            if (ch < 0x20) {
                return false;
            }
            if (ch == '\\') {
                if (++i >= value.length()) {
                    return false;
                }
                final char escape = value.charAt(i);
                if (escape == 'u') {
                    for (int digit = 0; digit < 4; digit++) {
                        if (++i >= value.length() || "0123456789abcdefABCDEF".indexOf(value.charAt(i)) < 0) {
                            return false;
                        }
                    }
                } else if ("\"\\/bfnrt".indexOf(escape) < 0) {
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Serializes the given object to JSON and writes it to the provided Writer.
     * This method is called by Spring's HTTP message conversion framework when producing JSON responses.
     *
     * <p>The method uses Abacus's JSON serialization capabilities through {@link N#toJson(Object, JsonSerConfig, Writer)}
     * to convert Java objects into JSON format.</p>
     *
     * <p><b>About the <i>type</i> Parameter:</b><br>
     * The {@code type} parameter is provided by Spring's framework and represents the declared return type
     * from the controller method. However, this implementation <b>does not use</b> the type parameter because
     * Abacus's JSON serialization can infer all necessary type information from the object itself at runtime.
     * The parameter is kept for interface compliance with Spring's {@link AbstractJsonHttpMessageConverter}
     * and for potential future enhancements. This design allows the serializer to handle polymorphic types
     * and dynamic objects correctly without requiring explicit type declarations.</p>
     *
     * <p><b>Serialization Features:</b></p>
     * <ul>
     *   <li>Automatic conversion of JavaBean properties to JSON fields</li>
     *   <li>Null value handling (by default, {@code null} fields are omitted from the output; use {@code JsonSerConfig.setExclusion(Exclusion.NONE)} to include them)</li>
     *   <li>Support for collections, maps, and arrays</li>
     *   <li>Date/time serialization as epoch milliseconds by default, or ISO-8601/custom formats via {@code JsonSerConfig.setDateTimeFormat}</li>
     *   <li>Handling of enums (serialized as their name by default)</li>
     *   <li>Support for nested and complex object graphs</li>
     * </ul>
     *
     * <p><b>Example usage in Spring Controller:</b></p>
     * <pre>{@code
     * @GetMapping("/users/{id}")
     * public User getUser(@PathVariable Long id) {
     *     User user = userService.findById(id);
     *     return user;  // user is automatically serialized to JSON by this method
     * }
     * }</pre>
     *
     * <p><b>Example User object:</b></p>
     * <pre>{@code
     * User user = User.builder()
     *     .id(123L)
     *     .name("John Doe")
     *     .email("john@example.com")
     *     .active(true)
     *     .roles(List.of("admin", "user"))
     *     .build();
     * }</pre>
     *
     * <p><b>Produces JSON output:</b></p>
     * <pre>{@code
     * {
     *   "id": 123,
     *   "name": "John Doe",
     *   "email": "john@example.com",
     *   "active": true,
     *   "roles": ["admin", "user"]
     * }
     * }</pre>
     *
     * @param obj the object to serialize to JSON. Can be {@code null}, in which case the JSON literal {@code null} is written.
     *            Can be any Java object including primitives, collections, maps, POJOs, or complex nested structures.
     * @param type the declared return type from the controller method, provided by Spring's framework.
     *             <b>Currently unused</b> by this implementation as Abacus can infer types from the object.
     *             Kept for interface compliance and potential future use. May be {@code null}.
     * @param writer the Writer to write the JSON output to. This method leaves it open. When invoked
     *               through the Spring 7 superclass's HTTP write path, that superclass closes the Writer
     *               after successful serialization; a direct caller remains responsible for closing it.
     * @throws IllegalArgumentException if the serialization configuration can no longer produce valid JSON (it is retained by reference and may
     *         have been mutated since construction)
     * @throws ParsingException if unwrapping the root value encounters a circular reference, or its value cannot be
     *         serialized as JSON
     * @throws RuntimeException if JSON serialization fails due to unsupported types or serialization errors
     * @throws UncheckedIOException if writing the JSON message body to {@code writer}, or reading a resource-backed value during its
     *         serialization, fails
     */
    @Override
    protected void writeInternal(final Object obj, final Type type, final Writer writer)
            throws IllegalArgumentException, ParsingException, RuntimeException, UncheckedIOException {
        // Re-checked per write: the configuration is retained by reference and is mutable, and the scalar
        // validation below only guards the scalar half - a bean/map/collection root goes straight to the
        // parser, so an unquoted property name or map key would otherwise leave the converter here.
        checkJsonCapable(jsc);

        Object value = obj;
        IdentityHashMap<Object, Boolean> holders = null;
        // Erased optional type handlers see Object as their element type and can quote a collection
        // as text. Unwrap before selecting the runtime JSON shape; nested empty wrappers become null.
        while (true) {
            if (value instanceof java.util.Optional<?> optional) {
                value = optional.orElse(null);
            } else if (value instanceof Optional<?> optional) {
                value = optional.orElse(null);
            } else if (value instanceof Nullable<?> nullable) {
                value = nullable.orElse(null);
            } else if (value instanceof Holder<?> holder) {
                if (holders == null) {
                    holders = new IdentityHashMap<>();
                }
                if (holders.put(holder, Boolean.TRUE) != null) {
                    throw new ParsingException("Circular reference in root value holders");
                }
                value = holder.value();
            } else {
                break;
            }
        }
        final com.landawn.abacus.type.Type<Object> valueType = value == null ? null : TypeFactory.getType(value.getClass());
        if (value != null && !usesDirectValueConversion(valueType)) {
            N.toJson(value, jsc, writer);
            return;
        }
        if (value != null && isStructuredValueType(valueType)) {
            writeStructuredValue(value, writer);
            return;
        }

        final BufferedJsonWriter buffer = Objectory.createBufferedJsonWriter();
        try {
            // serializeTo supplies JSON quoting/configuration that the parser's raw root shortcut
            // omits. Validate before writing so invalid scalar settings cannot emit a partial value.
            if (value == null) {
                buffer.write("null");
            } else {
                valueType.serializeTo(buffer, value, jsc);
            }
            final String json = buffer.toString();
            if (isCustomValueType(valueType) && (json.startsWith("[") || json.startsWith("{"))) {
                // No public metadata exposes a custom handler's JSON shape. Probe it once, then use
                // the root conversion to preserve nested runtime types (e.g. a Pair-backed value).
                writeStructuredValue(value, writer);
                return;
            }
            if (!isJsonScalar(json)) {
                throw new ParsingException("Scalar serialization did not produce a valid JSON value");
            }
            writer.write(json);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            Objectory.recycle(buffer);
        }
    }

    /**
     * Rejects a serialization configuration that cannot produce valid JSON, whatever the root's shape is.
     *
     * <p>The per-response {@code isJsonScalar} check only sees a scalar root; a bean, map or collection root
     * is handed to the parser directly. Checking the configuration instead applies one rule to both halves,
     * and it fails where the mistake was made rather than mid-response.</p>
     *
     * @param config the serialization configuration to validate
     * @throws IllegalArgumentException if the configuration would emit unquoted strings, chars, property
     *         names or map keys, or a structured root without its enclosing brackets
     */
    private static void checkJsonCapable(final JsonSerConfig config) throws IllegalArgumentException {
        if (!config.isBracketRootValue()) {
            // The response body is a single document: without the root brackets a bean writes
            // "id": 1, "name": "w" and a list writes "a", "b" - neither is a JSON value.
            throw new IllegalArgumentException("JsonSerConfig.bracketRootValue must be true to emit valid JSON");
        }

        if (config.getStringQuotation() != '"') {
            throw new IllegalArgumentException(
                    "JsonSerConfig.stringQuotation must be '\"' to emit valid JSON, but is: " + quotationOf(config.getStringQuotation()));
        }

        if (config.getCharQuotation() != '"') {
            throw new IllegalArgumentException(
                    "JsonSerConfig.charQuotation must be '\"' to emit valid JSON, but is: " + quotationOf(config.getCharQuotation()));
        }

        if (!config.isQuotePropName()) {
            throw new IllegalArgumentException("JsonSerConfig.quotePropName must be true to emit valid JSON");
        }

        if (!config.isQuoteMapKey()) {
            throw new IllegalArgumentException("JsonSerConfig.quoteMapKey must be true to emit valid JSON");
        }
    }

    private static String quotationOf(final char quotation) {
        // 0 means "no quotation" and must not be put into the message as a raw NUL character.
        return quotation == 0 ? "none" : "'" + quotation + "'";
    }

    /**
     * @throws ParsingException if serialization produces text that is not a valid JSON value
     * @throws UncheckedIOException if writing the serialized JSON to the response writer fails
     */
    private void writeStructuredValue(final Object value, final Writer writer) throws ParsingException, UncheckedIOException {
        final StringWriter buffer = new StringWriter();
        N.toJson(value, jsc, buffer);
        final String json = buffer.toString();
        N.fromJson(json, new JsonDeserConfig(), Object.class);
        try {
            writer.write(json);
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
