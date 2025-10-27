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

import org.springframework.http.converter.json.AbstractJsonHttpMessageConverter;
import org.springframework.lang.Nullable;

import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.N;

/**
 * Spring HTTP message converter for JSON serialization and deserialization using Abacus JSON utilities.
 * This converter integrates Abacus's JSON processing capabilities with Spring's HTTP message conversion framework,
 * allowing seamless conversion between Java objects and JSON in Spring MVC/WebFlux applications.
 * 
 * <p>This converter extends Spring's {@link AbstractJsonHttpMessageConverter} and delegates the actual
 * JSON processing to Abacus's {@link N} utility class, which provides high-performance JSON operations.</p>
 * 
 * <p>The converter supports reading JSON from HTTP requests and writing JSON to HTTP responses,
 * handling all standard Java types as well as custom POJOs. It automatically handles content type
 * negotiation for "application/json" and related media types.</p>
 * 
 * <p><b>Usage Example in Spring Configuration:</b></p>
 * <pre>{@code
 * @Configuration
 * @EnableWebMvc
 * public class WebConfig implements WebMvcConfigurer {
 *     
 *     @Override
 *     public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
 *         converters.add(new JSONHttpMessageConverter());
 *     }
 * }
 * }</pre>
 * 
 * <p><b>Usage Example with RestTemplate:</b></p>
 * <pre>{@code
 * RestTemplate restTemplate = new RestTemplate();
 * restTemplate.getMessageConverters().add(0, new JSONHttpMessageConverter());
 * 
 * // Now the RestTemplate will use Abacus for JSON processing
 * MyObject result = restTemplate.getForObject("https://api.example.com/data", MyObject.class);
 * }</pre>
 * 
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>High-performance JSON processing using Abacus utilities</li>
 *   <li>Support for complex generic types through TypeFactory</li>
 *   <li>Seamless integration with Spring MVC and WebFlux</li>
 *   <li>Automatic content type handling for JSON media types</li>
 *   <li>Thread-safe implementation suitable for singleton usage</li>
 * </ul>
 * 
 * @since 1.0
 * @see AbstractJsonHttpMessageConverter
 * @see N#fromJson(Reader, com.landawn.abacus.type.Type)
 * @see N#toJson(Object, Writer)
 */
public class JSONHttpMessageConverter extends AbstractJsonHttpMessageConverter {

    /**
     * Constructs a new JSONHttpMessageConverter with default configuration.
     * The converter will handle "application/json" and related JSON media types by default.
     *
     * <p>This constructor initializes the converter with standard JSON media type support
     * inherited from {@link AbstractJsonHttpMessageConverter}, including:</p>
     * <ul>
     *   <li>application/json</li>
     *   <li>application/*+json</li>
     * </ul>
     *
     * <p>The converter is thread-safe and can be safely used as a singleton in Spring applications.
     * It automatically integrates with Spring's content negotiation mechanism to handle JSON
     * serialization and deserialization for REST endpoints.</p>
     *
     * <p><b>Example usage in Spring MVC:</b></p>
     * <pre>{@code
     * @Configuration
     * public class WebConfig implements WebMvcConfigurer {
     *     @Override
     *     public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
     *         converters.add(new JSONHttpMessageConverter());
     *     }
     * }
     * }</pre>
     *
     * <p><b>Example usage with RestTemplate:</b></p>
     * <pre>{@code
     * RestTemplate restTemplate = new RestTemplate();
     * restTemplate.getMessageConverters().add(0, new JSONHttpMessageConverter());
     * }</pre>
     */
    public JSONHttpMessageConverter() { //NOSONAR
    }

    /**
     * Reads JSON content from the provided Reader and deserializes it into an object of the specified type.
     * This method is called by Spring's HTTP message conversion framework when processing incoming JSON requests.
     *
     * <p>The method uses Abacus's JSON deserialization capabilities through {@link N#fromJson(Reader, com.landawn.abacus.type.Type)}
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
     * @return the deserialized object of the specified type, never {@code null} unless the JSON content is "null"
     * @throws com.landawn.abacus.exception.UncheckedIOException if an I/O error occurs while reading from the Reader
     * @throws IllegalArgumentException if the JSON content cannot be mapped to the target type due to type mismatch
     * @throws RuntimeException if JSON parsing fails due to malformed JSON or other parsing errors
     */
    @Override
    protected Object readInternal(final Type resolvedType, final Reader reader) {
        return N.fromJson(reader, TypeFactory.getType(resolvedType));
    }

    /**
     * Serializes the given object to JSON and writes it to the provided Writer.
     * This method is called by Spring's HTTP message conversion framework when producing JSON responses.
     *
     * <p>The method uses Abacus's JSON serialization capabilities through {@link N#toJson(Object, Writer)}
     * to convert Java objects into JSON format. The serialization process automatically handles circular
     * references, custom date formats, and complex nested object graphs. The type parameter is available
     * for potential future use but is currently not utilized as Abacus can infer types from the object itself.</p>
     *
     * <p><b>Serialization Features:</b></p>
     * <ul>
     *   <li>Automatic conversion of JavaBean properties to JSON fields</li>
     *   <li>Null value handling (by default, {@code null} fields are included as "null")</li>
     *   <li>Support for collections, maps, and arrays</li>
     *   <li>Automatic date/time formatting to ISO-8601 or custom formats</li>
     *   <li>Handling of enums (serialized as their name by default)</li>
     *   <li>Support for nested and complex object graphs</li>
     * </ul>
     *
     * <p><b>Example usage in Spring Controller:</b></p>
     * <pre>{@code
     * @GetMapping("/users/{id}")
     * public User getUser(@PathVariable Long id) {
     *     User user = userService.findById(id);
     *     return user; // Automatically serialized to JSON by this method
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
     *     .roles(Arrays.asList("admin", "user"))
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
     * @param obj the object to serialize to JSON. Can be {@code null}, in which case the JSON output will be "null".
     *            Can be any Java object including primitives, collections, maps, POJOs, or complex nested structures.
     * @param type the type information for the object. This parameter is currently unused but provided for
     *             future compatibility with Spring's type system. May be {@code null}.
     * @param writer the Writer to write the JSON output to. The Writer is managed by Spring's framework
     *               and will be flushed and closed automatically after this method returns.
     * @throws com.landawn.abacus.exception.UncheckedIOException if an I/O error occurs while writing to the Writer
     * @throws RuntimeException if JSON serialization fails due to unsupported types or serialization errors
     */
    @Override
    protected void writeInternal(final Object obj, final @Nullable Type type, final Writer writer) {
        N.toJson(obj, writer);
    }
}
