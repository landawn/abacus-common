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
 * @author HaiYang Li
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
     * <p><b>Example usage:</b></p>
     * <pre>{@code
     * JSONHttpMessageConverter converter = new JSONHttpMessageConverter();
     * // The converter is now ready to be added to Spring's message converters
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
     * generic types properly.</p>
     * 
     * <p><b>Implementation Details:</b></p>
     * <ul>
     *   <li>Delegates to Abacus's N.fromJson() method for actual deserialization</li>
     *   <li>Uses TypeFactory to properly handle generic type information</li>
     *   <li>Supports all types that Abacus JSON can deserialize</li>
     *   <li>The Reader is not closed by this method (handled by the framework)</li>
     * </ul>
     * 
     * <p><b>Example JSON input:</b></p>
     * <pre>{@code
     * {
     *   "id": 123,
     *   "name": "John Doe",
     *   "active": true
     * }
     * }</pre>
     * 
     * <p>Would be deserialized to an object of the type specified by resolvedType parameter.</p>
     * 
     * @param resolvedType the target type to deserialize the JSON into, including generic type information
     * @param reader the Reader containing the JSON content to be deserialized
     * @return the deserialized object of the specified type
     * @throws RuntimeException if JSON parsing fails or the content cannot be mapped to the target type
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
     * to convert Java objects into JSON format. The type parameter is available for potential future use
     * but is currently not utilized as Abacus can infer types from the object itself.</p>
     * 
     * <p><b>Implementation Details:</b></p>
     * <ul>
     *   <li>Delegates to Abacus's N.toJson() method for actual serialization</li>
     *   <li>Handles null objects appropriately (serializes to "null")</li>
     *   <li>Supports all types that Abacus JSON can serialize</li>
     *   <li>The Writer is not closed by this method (handled by the framework)</li>
     *   <li>The type parameter is currently unused but provided for future extensibility</li>
     * </ul>
     * 
     * <p><b>Example object:</b></p>
     * <pre>{@code
     * User user = new User(123, "John Doe", true);
     * }</pre>
     * 
     * <p>Would be serialized to JSON like:</p>
     * <pre>{@code
     * {
     *   "id": 123,
     *   "name": "John Doe",
     *   "active": true
     * }
     * }</pre>
     * 
     * @param obj the object to serialize to JSON (can be null)
     * @param type the type information for the object (currently unused, provided for future compatibility)
     * @param writer the Writer to write the JSON output to
     * @throws RuntimeException if JSON serialization fails
     */
    @Override
    protected void writeInternal(final Object obj, final @Nullable Type type, final Writer writer) {
        N.toJson(obj, writer);
    }
}