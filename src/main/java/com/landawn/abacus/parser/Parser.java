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
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;

import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.UncheckedIOException;

/**
 * Generic interface for object serialization and deserialization parsers.
 * 
 * <p>This interface defines the contract for parsers that can convert objects to/from
 * various formats (JSON, XML, etc.). All implementations must be thread-safe.</p>
 * 
 * <p>Design principles:</p>
 * <ul>
 *   <li>1. Simple (is beautiful)</li>
 *   <li>2. Fast (is powerful)</li>
 *   <li>3. Concepts (must be integral and consistent)</li>
 * </ul>
 * 
 * <p>These principles can't be broken by any change or reason. Programmability is
 * prioritized over configurability. There is no extra support for configuration files
 * or annotations beyond {@link JsonXmlField}.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Parser<MySerConfig, MyDeserConfig> parser = new MyParser();
 * 
 * // Serialization
 * String json = parser.serialize(myObject);
 * parser.serialize(myObject, new File("output.json"));
 * 
 * // Deserialization
 * MyClass obj = parser.deserialize(json, MyClass.class);
 * }</pre>
 *
 * @param <SC> the serialization configuration type
 * @param <DC> the deserialization configuration type
 * @see JsonXmlField
 * @see SerializationConfig
 * @see DeserializationConfig
 */
public interface Parser<SC extends SerializationConfig<?>, DC extends DeserializationConfig<?>> {

    /**
     * Serializes an object to a string representation.
     * 
     * <p>This method uses default serialization settings.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * String json = parser.serialize(myObject);
     * }</pre>
     *
     * @param obj the object to serialize
     * @return the serialized string representation
     */
    String serialize(Object obj);

    /**
     * Serializes an object to a string representation using custom configuration.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MySerConfig config = new MySerConfig()
     *     .setPrettyFormat(true)
     *     .setDateFormat("yyyy-MM-dd");
     * String json = parser.serialize(myObject, config);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration to use
     * @return the serialized string representation
     */
    String serialize(Object obj, SC config);

    /**
     * Serializes an object to a file.
     * 
     * <p>This method uses default serialization settings. The file will be created
     * if it doesn't exist, or overwritten if it does.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * parser.serialize(myObject, new File("output.json"));
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output file to write to
     * @throws UncheckedIOException if an I/O error occurs
     */
    void serialize(Object obj, File output) throws UncheckedIOException;

    /**
     * Serializes an object to a file using custom configuration.
     * 
     * <p>The file will be created if it doesn't exist, or overwritten if it does.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MySerConfig config = new MySerConfig().setPrettyFormat(true);
     * parser.serialize(myObject, config, new File("output.json"));
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration to use
     * @param output the output file to write to
     * @throws UncheckedIOException if an I/O error occurs
     */
    void serialize(Object obj, SC config, File output) throws UncheckedIOException;

    /**
     * Serializes an object to an output stream.
     * 
     * <p>This method uses default serialization settings. The stream is not closed
     * after writing.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * try (FileOutputStream fos = new FileOutputStream("output.json")) {
     *     parser.serialize(myObject, fos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output stream to write to
     * @throws UncheckedIOException if an I/O error occurs
     */
    void serialize(Object obj, OutputStream output) throws UncheckedIOException;

    /**
     * Serializes an object to an output stream using custom configuration.
     * 
     * <p>The stream is not closed after writing.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MySerConfig config = new MySerConfig().setCharset("UTF-8");
     * try (FileOutputStream fos = new FileOutputStream("output.json")) {
     *     parser.serialize(myObject, config, fos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration to use
     * @param output the output stream to write to
     * @throws UncheckedIOException if an I/O error occurs
     */
    void serialize(Object obj, SC config, OutputStream output) throws UncheckedIOException;

    /**
     * Serializes an object to a writer.
     * 
     * <p>This method uses default serialization settings. The writer is not closed
     * after writing.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * try (FileWriter writer = new FileWriter("output.json")) {
     *     parser.serialize(myObject, writer);
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the writer to write to
     * @throws UncheckedIOException if an I/O error occurs
     */
    void serialize(Object obj, Writer output) throws UncheckedIOException;

    /**
     * Serializes an object to a writer using custom configuration.
     * 
     * <p>The writer is not closed after writing.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MySerConfig config = new MySerConfig().setIndent("  ");
     * try (FileWriter writer = new FileWriter("output.json")) {
     *     parser.serialize(myObject, config, writer);
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration to use
     * @param output the writer to write to
     * @throws UncheckedIOException if an I/O error occurs
     */
    void serialize(Object obj, SC config, Writer output) throws UncheckedIOException;

    /**
     * Deserializes an object from a string representation.
     * 
     * <p>This method uses default deserialization settings.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MyClass obj = parser.deserialize(jsonString, MyClass.class);
     * List<String> list = parser.deserialize(jsonArray, List.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source string to deserialize from
     * @param targetClass the class of the object to create
     * @return the deserialized object
     */
    <T> T deserialize(String source, Class<? extends T> targetClass);

    /**
     * Deserializes an object from a string representation using custom configuration.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MyDeserConfig config = new MyDeserConfig()
     *     .setIgnoreUnknownProperty(true)
     *     .setElementType(String.class);
     * List<String> list = parser.deserialize(jsonArray, config, List.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source string to deserialize from
     * @param config the deserialization configuration to use
     * @param targetClass the class of the object to create
     * @return the deserialized object
     */
    <T> T deserialize(String source, DC config, Class<? extends T> targetClass);

    /**
     * Deserializes an object from a file.
     * 
     * <p>This method uses default deserialization settings.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MyClass obj = parser.deserialize(new File("input.json"), MyClass.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source file to read from
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws UncheckedIOException if an I/O error occurs or the file doesn't exist
     */
    <T> T deserialize(File source, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     * Deserializes an object from a file using custom configuration.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MyDeserConfig config = new MyDeserConfig()
     *     .setDateFormat("yyyy-MM-dd");
     * MyClass obj = parser.deserialize(new File("input.json"), config, MyClass.class);
     * }</pre>
     *
     * @param <T> the target type
     * @param source the source file to read from
     * @param config the deserialization configuration to use
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws UncheckedIOException if an I/O error occurs or the file doesn't exist
     */
    <T> T deserialize(File source, DC config, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     * Deserializes an object from an input stream.
     * 
     * <p>This method uses default deserialization settings. The stream is not closed
     * after reading.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * try (FileInputStream fis = new FileInputStream("input.json")) {
     *     MyClass obj = parser.deserialize(fis, MyClass.class);
     * }
     * }</pre>
     *
     * @param <T> the target type
     * @param source the input stream to read from
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws UncheckedIOException if an I/O error occurs
     */
    <T> T deserialize(InputStream source, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     * Deserializes an object from an input stream using custom configuration.
     * 
     * <p>The stream is not closed after reading.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MyDeserConfig config = new MyDeserConfig().setCharset("UTF-8");
     * try (FileInputStream fis = new FileInputStream("input.json")) {
     *     MyClass obj = parser.deserialize(fis, config, MyClass.class);
     * }
     * }</pre>
     *
     * @param <T> the target type
     * @param source the input stream to read from
     * @param config the deserialization configuration to use
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws UncheckedIOException if an I/O error occurs
     */
    <T> T deserialize(InputStream source, DC config, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     * Deserializes an object from a reader.
     * 
     * <p>This method uses default deserialization settings. The reader is not closed
     * after reading.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * try (FileReader reader = new FileReader("input.json")) {
     *     MyClass obj = parser.deserialize(reader, MyClass.class);
     * }
     * }</pre>
     *
     * @param <T> the target type
     * @param source the reader to read from
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws UncheckedIOException if an I/O error occurs
     */
    <T> T deserialize(Reader source, Class<? extends T> targetClass) throws UncheckedIOException;

    /**
     * Deserializes an object from a reader using custom configuration.
     * 
     * <p>The reader is not closed after reading.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * MyDeserConfig config = new MyDeserConfig()
     *     .setMapKeyType(String.class)
     *     .setMapValueType(Integer.class);
     * try (FileReader reader = new FileReader("input.json")) {
     *     Map<String, Integer> map = parser.deserialize(reader, config, Map.class);
     * }
     * }</pre>
     *
     * @param <T> the target type
     * @param source the reader to read from
     * @param config the deserialization configuration to use
     * @param targetClass the class of the object to create
     * @return the deserialized object
     * @throws UncheckedIOException if an I/O error occurs
     */
    <T> T deserialize(Reader source, DC config, Class<? extends T> targetClass) throws UncheckedIOException;
}