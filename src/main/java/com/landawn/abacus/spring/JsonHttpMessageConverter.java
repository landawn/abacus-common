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

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJsonHttpMessageConverter;

import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.N;

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
 * <p><b>Supported media types:</b> by default the converter handles the media types inherited from
 * {@link AbstractJsonHttpMessageConverter} (typically {@code application/json} and
 * {@code application/*+json}). To register additional media types (e.g. a vendor {@code +json} type
 * or {@code text/json}) at construction time, use one of the constructors that accept
 * {@link MediaType} values. The supported media types may also be changed after construction via the
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
     * @see JsonSerConfig
     * @see JsonDeserConfig
     * @see com.landawn.abacus.parser.Exclusion
     */
    public JsonHttpMessageConverter(final JsonSerConfig jsc, final JsonDeserConfig jdc) throws IllegalArgumentException {
        N.checkArgNotNull(jsc, "jsc");
        N.checkArgNotNull(jdc, "jdc");

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
     * <p>If no media types are supplied (an empty {@code supportedMediaTypes} argument), the parent's
     * default media types are retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonHttpMessageConverter converter = new JsonHttpMessageConverter(
     *     MediaType.APPLICATION_JSON,
     *     new MediaType("application", "vnd.api+json"));
     * }</pre>
     *
     * @param supportedMediaTypes the media types this converter should support. If none are supplied,
     *                            the inherited default media types are kept unchanged.
     * @see #JsonHttpMessageConverter(JsonSerConfig, JsonDeserConfig, MediaType...)
     */
    public JsonHttpMessageConverter(final MediaType... supportedMediaTypes) {
        this(new JsonSerConfig(), new JsonDeserConfig(), supportedMediaTypes);
    }

    /**
     * Constructs a new JsonHttpMessageConverter with custom serialization and deserialization
     * configurations and the specified supported media types.
     * This constructor combines full control over JSON processing behavior with control over the
     * media types the converter advertises, all at construction time.
     *
     * <p>If no media types are supplied (an empty {@code supportedMediaTypes} argument), the parent's
     * default media types are retained.</p>
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
     * @param supportedMediaTypes the media types this converter should support. If none are supplied,
     *                            the inherited default media types are kept unchanged.
     * @see JsonSerConfig
     * @see JsonDeserConfig
     */
    public JsonHttpMessageConverter(final JsonSerConfig jsc, final JsonDeserConfig jdc, final MediaType... supportedMediaTypes)
            throws IllegalArgumentException {
        N.checkArgNotNull(jsc, "jsc");
        N.checkArgNotNull(jdc, "jdc");

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
     * <p>If {@code supportedMediaTypes} is {@code null} or empty, the parent's default media types
     * are retained.</p>
     *
     * @param jsc the serialization configuration controlling how Java objects are converted to JSON. Must not be {@code null}.
     * @param jdc the deserialization configuration controlling how JSON is converted to Java objects. Must not be {@code null}.
     * @param supportedMediaTypes the media types this converter should support. If {@code null} or empty,
     *                            the inherited default media types are kept unchanged.
     * @see JsonSerConfig
     * @see JsonDeserConfig
     */
    public JsonHttpMessageConverter(final JsonSerConfig jsc, final JsonDeserConfig jdc, final List<MediaType> supportedMediaTypes)
            throws IllegalArgumentException {
        N.checkArgNotNull(jsc, "jsc");
        N.checkArgNotNull(jdc, "jdc");

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
     * @param resolvedType the target type to deserialize the JSON into, including generic type information.
     *                     This is the actual runtime type resolved from the method signature or type parameter.
     * @param reader the Reader containing the JSON content to be deserialized. The Reader is managed by
     *               Spring's framework and will be closed automatically after this method returns.
     * @return the deserialized object of the specified type
     * @throws com.landawn.abacus.exception.UncheckedIOException if an I/O error occurs while reading from the Reader
     * @throws IllegalArgumentException if the JSON content cannot be mapped to the target type due to type mismatch
     * @throws RuntimeException if JSON parsing fails due to malformed JSON or other parsing errors
     */
    @Override
    protected Object readInternal(final Type resolvedType, final Reader reader) {
        return N.fromJson(reader, jdc, TypeFactory.getType(resolvedType));
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
     * @param obj the object to serialize to JSON. Can be {@code null}, in which case nothing is written to the output.
     *            Can be any Java object including primitives, collections, maps, POJOs, or complex nested structures.
     * @param type the declared return type from the controller method, provided by Spring's framework.
     *             <b>Currently unused</b> by this implementation as Abacus can infer types from the object.
     *             Kept for interface compliance and potential future use. May be {@code null}.
     * @param writer the Writer to write the JSON output to. The Writer is managed by Spring's framework
     *               and will be flushed and closed automatically after this method returns.
     * @throws com.landawn.abacus.exception.UncheckedIOException if an I/O error occurs while writing to the Writer
     * @throws RuntimeException if JSON serialization fails due to unsupported types or serialization errors
     */
    @Override
    protected void writeInternal(final Object obj, final Type type, final Writer writer) {
        N.toJson(obj, jsc, writer);
    }
}
