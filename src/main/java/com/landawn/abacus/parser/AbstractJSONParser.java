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
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.stream.Stream;

abstract class AbstractJSONParser extends AbstractParser<JSONSerializationConfig, JSONDeserializationConfig> implements JSONParser {

    protected static final char _BRACE_L = WD._BRACE_L;

    protected static final char _BRACE_R = WD._BRACE_R;

    protected static final char _BRACKET_L = WD._BRACKET_L;

    protected static final char _BRACKET_R = WD._BRACKET_R;

    protected static final char _D_QUOTATION = WD._QUOTATION_D;

    protected static final char _S_QUOTATION = WD._QUOTATION_S;

    protected static final Type<Object> objType = TypeFactory.getType(Object.class);

    protected static final Type<String> strType = TypeFactory.getType(String.class);

    protected static final Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

    protected static final Type<?> defaultKeyType = objType;

    protected static final Type<?> defaultValueType = objType;

    protected static final String[] REPLACEMENT_CHARS;

    static {
        final int length = 128;
        REPLACEMENT_CHARS = new String[length];

        for (int i = 0; i < length; i++) {
            REPLACEMENT_CHARS[i] = String.format("\\u%04x", i);
        }
    }

    protected final JSONSerializationConfig defaultJSONSerializationConfig;

    protected final JSONDeserializationConfig defaultJSONDeserializationConfig;

    protected AbstractJSONParser() {
        this(null, null);
    }

    protected AbstractJSONParser(final JSONSerializationConfig jsc, final JSONDeserializationConfig jdc) {
        defaultJSONSerializationConfig = jsc != null ? jsc : new JSONSerializationConfig();
        defaultJSONDeserializationConfig = jdc != null ? jdc : new JSONDeserializationConfig();
    }

    /**
     * Deserializes a JSON string into an object of the specified target class using default deserialization configuration.
     * This method provides a convenient way to parse JSON without specifying custom configuration options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * User user = parser.readString("{\"name\":\"John\",\"age\":30}", User.class);
     * List<String> names = parser.readString("[\"Alice\",\"Bob\"]", List.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string to deserialize
     * @param targetClass the class of the target object to deserialize into
     * @return an instance of the target class populated with data from the JSON string
     */
    @Override
    public <T> T readString(final String source, final Class<? extends T> targetClass) {
        return readString(source, null, targetClass);
    }

    /**
     * Deserializes a JSON string into an object of the specified target class using custom deserialization configuration.
     * This method allows fine-grained control over the deserialization process through configuration options.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * User user = parser.readString("{\"name\":\"John\"}", config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string to deserialize
     * @param config the deserialization configuration to use, or {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into
     * @return an instance of the target class populated with data from the JSON string
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    @Override
    public <T> T readString(final String source, final JSONDeserializationConfig config, final Class<? extends T> targetClass)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes a JSON array string and populates the provided object array with the deserialized elements.
     * This method uses default deserialization configuration and directly populates the output array.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object[] output = new Object[3];
     * parser.readString("[\"Alice\",\"Bob\",\"Charlie\"]", output);
     * }</pre>
     *
     * @param source the JSON array string to deserialize
     * @param output the array to populate with deserialized elements
     */
    @Override
    public void readString(final String source, final Object[] output) {
        readString(source, null, output);
    }

    /**
     * Deserializes a JSON array string and populates the provided object array using custom deserialization configuration.
     * This method allows fine-grained control over how array elements are deserialized.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object[] output = new Object[3];
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * parser.readString("[\"Alice\",\"Bob\",\"Charlie\"]", config, output);
     * }</pre>
     *
     * @param source the JSON array string to deserialize
     * @param config the deserialization configuration to use, or {@code null} to use default configuration
     * @param output the array to populate with deserialized elements
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Object[] output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes a JSON array string and populates the provided collection with the deserialized elements.
     * This method uses default deserialization configuration and adds elements to the output collection.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> output = new ArrayList<>();
     * parser.readString("[\"Alice\",\"Bob\"]", output);
     * }</pre>
     *
     * @param source the JSON array string to deserialize
     * @param output the collection to populate with deserialized elements
     */
    @Override
    public void readString(final String source, final Collection<?> output) {
        readString(source, null, output);
    }

    /**
     * Deserializes a JSON array string and populates the provided collection using custom deserialization configuration.
     * This method allows fine-grained control over how collection elements are deserialized.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> output = new ArrayList<>();
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * parser.readString("[\"Alice\",\"Bob\"]", config, output);
     * }</pre>
     *
     * @param source the JSON array string to deserialize
     * @param config the deserialization configuration to use, or {@code null} to use default configuration
     * @param output the collection to populate with deserialized elements
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Collection<?> output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes a JSON object string and populates the provided map with the deserialized key-value pairs.
     * This method uses default deserialization configuration and adds entries to the output map.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> output = new HashMap<>();
     * parser.readString("{\"name\":\"John\",\"age\":30}", output);
     * }</pre>
     *
     * @param source the JSON object string to deserialize
     * @param output the map to populate with deserialized key-value pairs
     */
    @Override
    public void readString(final String source, final Map<?, ?> output) {
        readString(source, null, output);
    }

    /**
     * Deserializes a JSON object string and populates the provided map using custom deserialization configuration.
     * This method allows fine-grained control over how map entries are deserialized.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Object> output = new HashMap<>();
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * parser.readString("{\"name\":\"John\",\"age\":30}", config, output);
     * }</pre>
     *
     * @param source the JSON object string to deserialize
     * @param config the deserialization configuration to use, or {@code null} to use default configuration
     * @param output the map to populate with deserialized key-value pairs
     * @throws UnsupportedOperationException if this operation is not supported by the implementation
     */
    @Override
    public void readString(final String source, final JSONDeserializationConfig config, final Map<?, ?> output) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    /**
     * Deserializes a substring of a JSON string into an object of the specified target class using default configuration.
     * This method is useful when parsing a portion of a larger JSON string without creating a substring copy.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"users\":[{\"name\":\"John\"}]}";
     * User user = parser.deserialize(json, 10, 25, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string containing the substring to deserialize
     * @param fromIndex the beginning index of the substring, inclusive
     * @param toIndex the ending index of the substring, exclusive
     * @param targetClass the class of the target object to deserialize into
     * @return an instance of the target class populated with data from the JSON substring
     */
    @Override
    public <T> T deserialize(final String source, final int fromIndex, final int toIndex, final Class<? extends T> targetClass) {
        return deserialize(source, fromIndex, toIndex, null, targetClass);
    }

    /**
     * Deserializes a substring of a JSON string into an object using custom deserialization configuration.
     * This method allows fine-grained control over the deserialization process for a specific portion of the input.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String json = "{\"users\":[{\"name\":\"John\"}]}";
     * JSONDeserializationConfig config = new JSONDeserializationConfig();
     * User user = parser.deserialize(json, 10, 25, config, User.class);
     * }</pre>
     *
     * @param <T> the type of the target class
     * @param source the JSON string containing the substring to deserialize
     * @param fromIndex the beginning index of the substring, inclusive
     * @param toIndex the ending index of the substring, exclusive
     * @param config the deserialization configuration to use, or {@code null} to use default configuration
     * @param targetClass the class of the target object to deserialize into
     * @return an instance of the target class populated with data from the JSON substring
     */
    @Override
    public <T> T deserialize(final String source, final int fromIndex, final int toIndex, final JSONDeserializationConfig config,
            final Class<? extends T> targetClass) {
        return deserialize(source.substring(fromIndex, toIndex), config, targetClass);
    }

    /**
     * Creates a stream that lazily deserializes elements from a JSON array string.
     * This method provides efficient streaming of large JSON arrays without loading all elements into memory at once.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Stream<User> userStream = parser.stream("[{\"name\":\"John\"},{\"name\":\"Jane\"}]", Type.of(User.class));
     * userStream.forEach(System.out::println);
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param source the JSON array string to stream
     * @param elementType the type of elements to deserialize
     * @return a Stream of deserialized elements
     */
    @Override
    public <T> Stream<T> stream(final String source, final Type<? extends T> elementType) {
        return stream(source, null, elementType);
    }

    /**
     * Creates a stream that lazily deserializes elements from a JSON array file.
     * This method provides efficient streaming of large JSON files without loading the entire file into memory.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File jsonFile = new File("users.json");
     * Stream<User> userStream = parser.stream(jsonFile, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param source the file containing the JSON array to stream
     * @param elementType the type of elements to deserialize
     * @return a Stream of deserialized elements
     */
    @Override
    public <T> Stream<T> stream(final File source, final Type<? extends T> elementType) {
        return stream(source, null, elementType);
    }

    /**
     * Creates a stream that lazily deserializes elements from a JSON array input stream.
     * This method provides efficient streaming from input streams with optional automatic resource management.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * InputStream is = new FileInputStream("users.json");
     * Stream<User> userStream = parser.stream(is, true, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param source the input stream containing the JSON array to stream
     * @param closeInputStreamWhenStreamIsClosed whether to close the input stream when the stream is closed
     * @param elementType the type of elements to deserialize
     * @return a Stream of deserialized elements
     */
    @Override
    public <T> Stream<T> stream(final InputStream source, final boolean closeInputStreamWhenStreamIsClosed, final Type<? extends T> elementType) {
        return stream(source, null, closeInputStreamWhenStreamIsClosed, elementType);
    }

    /**
     * Creates a stream that lazily deserializes elements from a JSON array reader.
     * This method provides efficient streaming from readers with optional automatic resource management.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("users.json");
     * Stream<User> userStream = parser.stream(reader, true, Type.of(User.class));
     * }</pre>
     *
     * @param <T> the type of elements in the stream
     * @param reader the reader containing the JSON array to stream
     * @param closeReaderWhenStreamIsClosed whether to close the reader when the stream is closed
     * @param elementType the type of elements to deserialize
     * @return a Stream of deserialized elements
     */
    @Override
    public <T> Stream<T> stream(final Reader reader, final boolean closeReaderWhenStreamIsClosed, final Type<? extends T> elementType) {
        return stream(reader, null, closeReaderWhenStreamIsClosed, elementType);
    }

    protected JSONSerializationConfig check(JSONSerializationConfig config) {
        return config == null ? defaultJSONSerializationConfig : config;
    }

    protected JSONDeserializationConfig check(JSONDeserializationConfig config) {
        return config == null ? defaultJSONDeserializationConfig : config;
    }
}
