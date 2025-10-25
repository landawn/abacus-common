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
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.TypeReference;
import com.landawn.abacus.annotation.Beta;

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
 */
public final class FastJson {

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FastJson() {
        // singleton for utility class.
    }

    /**
     * Converts the specified object to its JSON string representation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = FastJson.toJson(person);
     * // Result: {"name":"John","age":30}
     * }</pre>
     * 
     * @param obj the object to be converted to JSON string
     * @return the JSON string representation of the object, or "null" if the object is null
     */
    public static String toJson(final Object obj) {
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
     * // Result (pretty formatted):
     * // {
     * //   "name": "John",
     * //   "age": 30
     * // }
     * }</pre>
     * 
     * @param obj the object to be converted to JSON string
     * @param prettyFormat true to enable pretty formatting with indentation and line breaks, {@code false} for compact output
     * @return the JSON string representation of the object with or without pretty formatting
     */
    public static String toJson(final Object obj, final boolean prettyFormat) {
        if (prettyFormat) {
            return JSON.toJSONString(obj, JSONWriter.Feature.PrettyFormat);
        } else {
            return JSON.toJSONString(obj);
        }
    }

    /**
     * Converts the specified object to its JSON string representation using the specified JSONWriter features.
     * This method allows fine-grained control over the JSON serialization process by specifying various
     * writer features such as pretty formatting, null value handling, date formatting, etc.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String json = FastJson.toJson(person, JSONWriter.Feature.PrettyFormat, JSONWriter.Feature.WriteNulls);
     * }</pre>
     * 
     * @param obj the object to be converted to JSON string
     * @param features variable number of JSONWriter features to control serialization behavior
     * @return the JSON string representation of the object with the specified features applied
     */
    public static String toJson(final Object obj, final JSONWriter.Feature... features) {
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
     * @return the JSON string representation of the object using the specified context
     */
    public static String toJson(final Object obj, final JSONWriter.Context context) {
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
     * @throws RuntimeException if an I/O error occurs during file writing
     */
    public static void toJson(final Object obj, final File output) {
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
     * @throws RuntimeException if an I/O error occurs during file writing
     */
    public static void toJson(final Object obj, final File output, final JSONWriter.Feature... features) {
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
     * @throws RuntimeException if an I/O error occurs during file writing
     */
    public static void toJson(final Object obj, final File output, final JSONWriter.Context context) {
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
     * FastJson.toJson(person, new FileOutputStream("person.json"));
     * }</pre>
     * 
     * @param obj the object to be serialized to JSON
     * @param output the OutputStream where the JSON will be written
     */
    public static void toJson(final Object obj, final OutputStream output) {
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
     */
    public static void toJson(final Object obj, final OutputStream output, final JSONWriter.Feature... features) {
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
     */
    public static void toJson(final Object obj, final OutputStream output, final JSONWriter.Context context) {
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
     * FastJson.toJson(person, new FileWriter("person.json"));
     * }</pre>
     * 
     * @param obj the object to be serialized to JSON
     * @param output the Writer where the JSON will be written
     * @throws RuntimeException if an I/O error occurs during writing
     */
    public static void toJson(final Object obj, final Writer output) {
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
     * @throws RuntimeException if an I/O error occurs during writing
     */
    public static void toJson(final Object obj, final Writer output, final JSONWriter.Feature... features) {
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
     * @throws RuntimeException if an I/O error occurs during writing
     */
    public static void toJson(final Object obj, final Writer output, final JSONWriter.Context context) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final byte[] json, final Class<? extends T> targetType) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final byte[] json, final int offset, final int len, final Class<? extends T> targetType) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final JSONReader.Feature... features) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final Class<? extends T> targetType, final JSONReader.Context context) {
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
     * Type listType = new TypeToken<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(json, listType);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Type object representing the target type
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final Type targetType) {
        return JSON.parseObject(json, targetType);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target Type using the given JSONReader features.
     * This method combines the flexibility of Type-based deserialization with customizable reader features.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Type listType = new TypeToken<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(json, listType, JSONReader.Feature.SupportSmartMatch);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Type object representing the target type
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final Type targetType, final JSONReader.Feature... features) {
        return JSON.parseObject(json, targetType, features);
    }

    /**
     * Deserializes JSON from a string into an object of the specified target Type using the given JSONReader context.
     * This method provides the most comprehensive control over Type-based deserialization.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * Type listType = new TypeToken<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(json, listType, context);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the JSON string to be deserialized
     * @param targetType the Type object representing the target type
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final Type targetType, final JSONReader.Context context) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final TypeReference<T> typeReference) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final TypeReference<T> typeReference, final JSONReader.Feature... features) {
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
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final String json, final TypeReference<T> typeReference, final JSONReader.Context context) {
        return JSON.parseObject(json, typeReference.getType(), context);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target type.
     * This method is useful when reading JSON from various input sources such as files,
     * network streams, or other character-based input sources.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("person.json");
     * Person person = FastJson.fromJson(reader, Person.class);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the Reader containing JSON data
     * @param targetType the Class object representing the target type
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType) {
        return JSON.parseObject(json, targetType);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target type using the given JSONReader features.
     * This method allows customization of the deserialization process when reading from character-based sources.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("person.json");
     * Person person = FastJson.fromJson(reader, Person.class, JSONReader.Feature.SupportSmartMatch);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the Reader containing JSON data
     * @param targetType the Class object representing the target type
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType, final JSONReader.Feature... features) {
        return JSON.parseObject(json, targetType, features);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target type using the given JSONReader context.
     * This method provides comprehensive control over Reader-based deserialization by first reading all content
     * into a string and then parsing with the specified context.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * Person person = FastJson.fromJson(reader, Person.class, context);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the Reader containing JSON data
     * @param targetType the Class object representing the target type
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    @Beta
    public static <T> T fromJson(final Reader json, final Class<? extends T> targetType, final JSONReader.Context context) {
        return JSON.parseObject(IOUtil.readAllToString(json), targetType, context);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target Type.
     * This method is useful for deserializing generic types or complex type structures
     * from character-based input sources.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("people.json");
     * Type listType = new TypeToken<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(reader, listType);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the Reader containing JSON data
     * @param targetType the Type object representing the target type
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final Reader json, final Type targetType) {
        return JSON.parseObject(json, targetType);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target Type using the given JSONReader features.
     * This method combines the flexibility of Type-based deserialization with customizable reader features
     * for character-based input sources.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Reader reader = new FileReader("people.json");
     * Type listType = new TypeToken<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(reader, listType, JSONReader.Feature.SupportSmartMatch);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the Reader containing JSON data
     * @param targetType the Type object representing the target type
     * @param features variable number of JSONReader features to control deserialization behavior
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    public static <T> T fromJson(final Reader json, final Type targetType, final JSONReader.Feature... features) {
        return JSON.parseObject(json, targetType, features);
    }

    /**
     * Deserializes JSON from a Reader into an object of the specified target Type using the given JSONReader context.
     * This method provides comprehensive control over Type-based deserialization from character-based input sources
     * by first reading all content into a string and then parsing with the specified context.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONReader.Context context = new JSONReader.Context();
     * Type listType = new TypeToken<List<Person>>(){}.getType();
     * List<Person> people = FastJson.fromJson(reader, listType, context);
     * }</pre>
     * 
     * @param <T> the type of the target object
     * @param json the Reader containing JSON data
     * @param targetType the Type object representing the target type
     * @param context the JSONReader context containing deserialization configuration
     * @return the deserialized object of type T, or null if the JSON represents null
     */
    @Beta
    public static <T> T fromJson(final Reader json, final Type targetType, final JSONReader.Context context) {
        return JSON.parseObject(IOUtil.readAllToString(json), targetType, context);
    }
}
