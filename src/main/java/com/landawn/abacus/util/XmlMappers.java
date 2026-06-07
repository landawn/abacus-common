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
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

/**
 * A high-performance utility class for XML serialization and deserialization based on Jackson's {@code XmlMapper}.
 * This class provides convenient static methods for converting between Java objects and XML representations,
 * with support for various input/output formats and configuration options.
 *
 * <p>Features include:</p>
 * <ul>
 *   <li>Object pooling for XmlMapper instances to improve performance</li>
 *   <li>Support for pretty-printing XML output</li>
 *   <li>Configuration support for serialization and deserialization</li>
 *   <li>Multiple input/output formats (String, File, Stream, Reader/Writer, etc.)</li>
 *   <li>TypeReference support for complex generic types</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Simple serialization
 * Person person = new Person("John", 30);
 * String xml = XmlMappers.toXml(person);
 *
 * // Deserialization with TypeReference
 * String listXml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
 * List<String> list = XmlMappers.fromXml(listXml, new TypeReference<List<String>>() {});
 * }</pre>
 *
 * <p>This class is not instantiable.</p>
 *
 * @see XmlMapper
 * @see TypeReference
 * @see SerializationConfig
 * @see DeserializationConfig
 */
public final class XmlMappers {
    private static final int POOL_SIZE = 128;
    private static final List<XmlMapper> mapperPool = new ArrayList<>(POOL_SIZE);

    private static final XmlMapper defaultXmlMapper = new XmlMapper();
    private static final XmlMapper defaultXmlMapperForPretty = (XmlMapper) new XmlMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private static final SerializationConfig defaultSerializationConfig = defaultXmlMapper.getSerializationConfig();
    private static final DeserializationConfig defaultDeserializationConfig = defaultXmlMapper.getDeserializationConfig();

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

    private XmlMappers() {
        // Utility class - prevent instantiation
    }

    /**
     * Serializes the specified object to an XML string using default configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String xml = XmlMappers.toXml(person);
     * // Result: <Person><name>John</name><age>30</age></Person>
     * }</pre>
     *
     * @param obj the object to serialize
     * @return the XML string representation of the object
     * @throws RuntimeException if serialization fails
     */
    public static String toXml(final Object obj) {
        try {
            return defaultXmlMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML string with optional pretty formatting.
     * When pretty format is enabled, the output XML will be indented for better readability.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * String prettyXml = XmlMappers.toXml(person, true);
     * // Result with indentation:
     * // <Person>
     * //   <name>John</name>
     * //   <age>30</age>
     * // </Person>
     * }</pre>
     *
     * @param obj the object to serialize
     * @param prettyFormat {@code true} to enable pretty printing with indentation, {@code false} for compact output
     * @return the XML string representation of the object
     * @throws RuntimeException if serialization fails
     */
    public static String toXml(final Object obj, final boolean prettyFormat) {
        try {
            if (prettyFormat) {
                return defaultXmlMapperForPretty.writeValueAsString(obj);
            } else {
                return defaultXmlMapper.writeValueAsString(obj);
            }
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML string with custom serialization features.
     * This method allows fine-grained control over serialization behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", null);
     * String xml = XmlMappers.toXml(person,
     *     SerializationFeature.WRAP_ROOT_VALUE,
     *     SerializationFeature.INDENT_OUTPUT);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param first the first serialization feature to enable
     * @param features additional serialization features to enable
     * @return the XML string representation of the object
     * @throws RuntimeException if serialization fails
     * @see #toXml(Object, SerializationConfig)
     */
    public static String toXml(final Object obj, final SerializationFeature first, final SerializationFeature... features) {
        return toXml(obj, defaultSerializationConfig.with(first, features));
    }

    /**
     * Serializes the specified object to an XML string using a custom serialization configuration.
     * This method provides maximum flexibility for controlling serialization behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig()
     *     .with(SerializationFeature.WRAP_ROOT_VALUE)
     *     .with(SerializationFeature.INDENT_OUTPUT);
     * String xml = XmlMappers.toXml(person, config);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param config the serialization configuration to use
     * @return the XML string representation of the object
     * @throws RuntimeException if serialization fails
     */
    public static String toXml(final Object obj, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.writeValueAsString(obj);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to an XML file using default configuration.
     * The file will be created if it doesn't exist, or overwritten if it does.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * File output = new File("person.xml");
     * XmlMappers.toXml(person, output);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output file to write the XML to
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final File output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML file using a custom serialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT);
     * XmlMappers.toXml(person, new File("person.xml"), config);
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output file to write the XML to
     * @param config the serialization configuration to use
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final File output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to an XML output stream using default configuration.
     * The stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * try (FileOutputStream fos = new FileOutputStream("person.xml")) {
     *     XmlMappers.toXml(person, fos);
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output stream to write the XML to
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final OutputStream output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML output stream using a custom serialization configuration.
     * The stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
     * ByteArrayOutputStream out = new ByteArrayOutputStream();
     * XmlMappers.toXml(N.asMap("name", "Bob"), out, config);   // writes indented <ImmutableMap>...</ImmutableMap>
     * String xml = out.toString();                             // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the output stream to write the XML to
     * @param config the serialization configuration to use
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final OutputStream output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to an XML writer using default configuration.
     * The writer is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Person person = new Person("John", 30);
     * try (StringWriter writer = new StringWriter()) {
     *     XmlMappers.toXml(person, writer);
     *     String xml = writer.toString();
     * }
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the writer to write the XML to
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final Writer output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to an XML writer using a custom serialization configuration.
     * The writer is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
     * StringWriter writer = new StringWriter();
     * XmlMappers.toXml(N.asMap("name", "Bob"), writer, config);   // writes indented XML to the writer
     * String xml = writer.toString();                             // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the writer to write the XML to
     * @param config the serialization configuration to use
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final Writer output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Serializes the specified object to a DataOutput using default configuration.
     * This method is useful for writing XML to binary protocols.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
     * DataOutput out = new DataOutputStream(bytes);
     * XmlMappers.toXml(N.asMap("name", "Bob"), out);   // writes <ImmutableMap><name>Bob</name></ImmutableMap>
     * String xml = bytes.toString();                   // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the DataOutput to write the XML to
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final DataOutput output) {
        try {
            defaultXmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Serializes the specified object to a DataOutput using a custom serialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig().with(SerializationFeature.INDENT_OUTPUT);
     * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
     * DataOutput out = new DataOutputStream(bytes);
     * XmlMappers.toXml(N.asMap("name", "Bob"), out, config);   // writes indented XML
     * String xml = bytes.toString();                           // contains "<name>Bob</name>"
     * }</pre>
     *
     * @param obj the object to serialize
     * @param output the DataOutput to write the XML to
     * @param config the serialization configuration to use
     * @throws RuntimeException if an I/O error occurs
     */
    public static void toXml(final Object obj, final DataOutput output, final SerializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            xmlMapper.writeValue(output, obj);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a byte array into an object of the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] xmlBytes = "<Person><name>John</name><age>30</age></Person>".getBytes();
     * Person person = XmlMappers.fromXml(xmlBytes, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array to deserialize
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a portion of a byte array into an object of the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = "##<LinkedHashMap><name>Bob</name></LinkedHashMap>".getBytes();
     * Map<String, Object> map = XmlMappers.fromXml(bytes, 2, bytes.length - 2, Map.class);
     * // map -> {name=Bob}; the leading "##" is skipped via offset 2
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array containing the data
     * @param offset the start offset in the array
     * @param len the number of bytes to read
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final int offset, final int len, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified type.
     * This is one of the most commonly used methods for XML deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<Person><name>John</name><age>30</age></Person>";
     * Person person = XmlMappers.fromXml(xml, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final JsonProcessingException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified type with custom deserialization features.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<Person><name>John</name><age>30</age></Person>";
     * Person person = XmlMappers.fromXml(xml, Person.class,
     *     DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the class of the object to deserialize to
     * @param first the first deserialization feature to enable
     * @param features additional deserialization features to enable
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see #fromXml(String, Class, DeserializationConfig)
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final Class<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromXml(xml, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     * Deserializes an XML string into an object of the specified type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
     * Map<String, Object> map = XmlMappers.fromXml(xml, Map.class, config);   // map -> {name=Bob}
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("person.xml");
     * Person person = XmlMappers.fromXml(xmlFile, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or file cannot be read
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig()
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * File xmlFile = new File("person.xml");
     * Person person = XmlMappers.fromXml(xmlFile, Person.class, config);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or file cannot be read
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified type.
     * The stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (FileInputStream fis = new FileInputStream("person.xml")) {
     *     Person person = XmlMappers.fromXml(fis, Person.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified type using a custom deserialization configuration.
     * The stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
     * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
     *     Map<String, Object> map = XmlMappers.fromXml(in, Map.class, config);   // map -> {name=Bob}
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified type.
     * The reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (StringReader reader = new StringReader(xmlString)) {
     *     Person person = XmlMappers.fromXml(reader, Person.class);
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified type using a custom deserialization configuration.
     * The reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
     * try (Reader reader = new StringReader(xml)) {
     *     Map<String, Object> map = XmlMappers.fromXml(reader, Map.class, config);   // map -> {name=Bob}
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified type.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL xmlUrl = new URL("http://example.com/person.xml");
     * Person person = XmlMappers.fromXml(xmlUrl, Person.class);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or the URL cannot be accessed
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    @SuppressWarnings("deprecation")
    public static <T> T fromXml(final URL xml, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified type using a custom deserialization configuration.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * URL xmlUrl = new File("person.xml").toURI().toURL();
     * Person person = XmlMappers.fromXml(xmlUrl, Person.class, config);
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or the URL cannot be accessed
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    @SuppressWarnings("deprecation")
    public static <T> T fromXml(final URL xml, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified type.
     * This method is useful for reading XML from binary protocols.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * Person person = XmlMappers.fromXml(in, Person.class);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the class of the object to deserialize to
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final Class<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * Person person = XmlMappers.fromXml(in, Person.class, config);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the class of the object to deserialize to
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final Class<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a byte array into an object of the specified generic type.
     * Use this method when deserializing generic types like List&lt;String&gt; or Map&lt;String, Object&gt;.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] xmlBytes = "<ArrayList><item>a</item><item>b</item></ArrayList>".getBytes();
     * List<String> list = XmlMappers.fromXml(xmlBytes, new TypeReference<List<String>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array to deserialize
     * @param targetType the type reference describing the target type
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a portion of a byte array into an object of the specified generic type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] bytes = "##<List><item>a</item><item>b</item></List>".getBytes();
     * List<String> list = XmlMappers.fromXml(bytes, 2, bytes.length - 2,
     *         new TypeReference<List<String>>() {});
     * // list -> [a, b]; the leading "##" is skipped via offset 2
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML byte array containing the data
     * @param offset the start offset in the array
     * @param len the number of bytes to read
     * @param targetType the type reference describing the target type
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final byte[] xml, final int offset, final int len, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, offset, len, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified generic type.
     * This is the most commonly used method for deserializing generic types from XML.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<LinkedHashMap><key1>value1</key1><key2>value2</key2></LinkedHashMap>";
     * Map<String, String> map = XmlMappers.fromXml(xml, new TypeReference<Map<String, String>>() {});
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes an XML string into an object of the specified generic type with custom deserialization features.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<List><item>a</item><item>b</item></List>";
     * List<String> list = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {},
     *         DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the type reference describing the target type
     * @param first the first deserialization feature to enable
     * @param features additional deserialization features to enable
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final TypeReference<? extends T> targetType, final DeserializationFeature first,
            final DeserializationFeature... features) {
        return fromXml(xml, targetType, defaultDeserializationConfig.with(first, features));
    }

    /**
     * Deserializes an XML string into an object of the specified generic type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<List><item>a</item><item>b</item></List>";
     * List<String> list = XmlMappers.fromXml(xml, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML string to deserialize
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final String xml, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified generic type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File xmlFile = new File("items.xml");                                                    // contains <List><item>a</item><item>b</item></List>
     * List<String> list = XmlMappers.fromXml(xmlFile, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or file cannot be read
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a file into an object of the specified generic type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * File xmlFile = new File("items.xml");                                                            // contains <List><item>a</item><item>b</item></List>
     * List<String> list = XmlMappers.fromXml(xmlFile, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the XML file to read from
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or file cannot be read
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final File xml, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified generic type.
     * The stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
     *     List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from an input stream into an object of the specified generic type using a custom deserialization configuration.
     * The stream is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
     *     List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the input stream containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final InputStream xml, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified generic type.
     * The reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (Reader reader = new StringReader(xml)) {
     *     List<String> list = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a reader into an object of the specified generic type using a custom deserialization configuration.
     * The reader is not closed by this method.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * String xml = "<List><item>a</item><item>b</item></List>";
     * try (Reader reader = new StringReader(xml)) {
     *     List<String> list = XmlMappers.fromXml(reader, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the reader containing XML data
     * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final Reader xml, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified generic type.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * URL xmlUrl = new File("items.xml").toURI().toURL();                                     // file contains a <List> of items
     * List<String> list = XmlMappers.fromXml(xmlUrl, new TypeReference<List<String>>() {});   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the type reference describing the target type
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or the URL cannot be accessed
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    @SuppressWarnings("deprecation")
    public static <T> T fromXml(final URL xml, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a URL into an object of the specified generic type using a custom deserialization configuration.
     * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * URL xmlUrl = new File("items.xml").toURI().toURL();                                             // file contains a <List> of items
     * List<String> list = XmlMappers.fromXml(xmlUrl, new TypeReference<List<String>>() {}, config);   // list -> [a, b]
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the URL pointing to XML data
     * @param targetType the type reference describing the target type
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails or the URL cannot be accessed
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    @SuppressWarnings("deprecation")
    public static <T> T fromXml(final URL xml, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, targetType);
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified generic type.
     * This method is useful for reading XML from binary protocols.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the type reference describing the target type
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final TypeReference<? extends T> targetType) {
        try {
            return defaultXmlMapper.readValue(xml, defaultXmlMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Deserializes XML from a DataInput into an object of the specified generic type using a custom deserialization configuration.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig();
     * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
     * // Note: the underlying Jackson XML format does not support reading from a DataInput
     * // source, so this call throws an UnsupportedOperationException at runtime.
     * List<String> list = XmlMappers.fromXml(in, new TypeReference<List<String>>() {}, config);   // throws UnsupportedOperationException
     * }</pre>
     *
     * @param <T> the type of the object to return
     * @param xml the DataInput containing XML data
     * @param targetType the type reference describing the target type
     * @param config the deserialization configuration to use
     * @return the deserialized object
     * @throws RuntimeException if deserialization fails
     * @see com.fasterxml.jackson.core.type.TypeReference
     */
    public static <T> T fromXml(final DataInput xml, final TypeReference<? extends T> targetType, final DeserializationConfig config) {
        final XmlMapper xmlMapper = getXmlMapper(config);

        try {
            return xmlMapper.readValue(xml, xmlMapper.constructType(targetType));
        } catch (final IOException e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        } finally {
            recycle(xmlMapper);
        }
    }

    /**
     * Creates a new SerializationConfig instance with default settings.
     * This config can be customized and used with the toXml methods for fine-grained control over serialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SerializationConfig config = XmlMappers.createSerializationConfig()
     *     .with(SerializationFeature.INDENT_OUTPUT)
     *     .without(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
     * String xml = XmlMappers.toXml(object, config);
     * }</pre>
     *
     * @return a new SerializationConfig instance
     */
    public static SerializationConfig createSerializationConfig() {
        // final SerializationConfig copy = defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);

        return defaultSerializationConfigForCopy.without(serializationFeatureNotEnabledByDefault);
    }

    /**
     * Creates a new {@link DeserializationConfig} instance with default settings.
     * This config can be customized and used with the fromXml methods for fine-grained control over deserialization.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DeserializationConfig config = XmlMappers.createDeserializationConfig()
     *     .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
     *     .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
     * Person person = XmlMappers.fromXml(xml, Person.class, config);
     * }</pre>
     *
     * @return a new {@link DeserializationConfig} instance
     */
    public static DeserializationConfig createDeserializationConfig() {
        // final DeserializationConfig copy = defaultDeserializationConfigForCopy.without(deserializationFeatureNotEnabledByDefault);

        return defaultDeserializationConfigForCopy.without(deserializationFeatureNotEnabledByDefault);
    }

    /**
     * Retrieves an XmlMapper from the pool configured with the specified serialization configuration.
     * If the pool is empty, a new XmlMapper is created. This method is used internally for object pooling
     * to improve performance by reusing mapper instances.
     *
     * @param config the serialization configuration to apply, or {@code null} to use the default mapper
     * @return an XmlMapper configured with the specified serialization config
     */
    static XmlMapper getXmlMapper(final SerializationConfig config) {
        if (config == null) {
            return defaultXmlMapper;
        }

        XmlMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new XmlMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    /**
     * Retrieves an XmlMapper from the pool configured with the specified deserialization configuration.
     * If the pool is empty, a new XmlMapper is created. This method is used internally for object pooling
     * to improve performance by reusing mapper instances.
     *
     * @param config the deserialization configuration to apply, or {@code null} to use the default mapper
     * @return an XmlMapper configured with the specified deserialization config
     */
    static XmlMapper getXmlMapper(final DeserializationConfig config) {
        if (config == null) {
            return defaultXmlMapper;
        }

        XmlMapper mapper = null;

        synchronized (mapperPool) {
            if (mapperPool.size() > 0) {
                mapper = mapperPool.remove(mapperPool.size() - 1);
            }
        }

        if (mapper == null) {
            mapper = new XmlMapper();
        }

        mapper.setConfig(config);

        return mapper;
    }

    /**
     * Returns an XmlMapper to the pool for reuse after resetting it to default configuration.
     * This method resets both serialization and deserialization configs to their defaults
     * before returning the mapper to the pool. If the pool is full, the mapper is discarded.
     *
     * @param mapper the XmlMapper to return to the pool, or {@code null} (which is ignored)
     */
    static void recycle(final XmlMapper mapper) {
        if (mapper == null || mapper == defaultXmlMapper || mapper == defaultXmlMapperForPretty) {
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
     * Wraps an XmlMapper instance to provide convenient serialization and deserialization methods.
     * This allows you to use a pre-configured XmlMapper with the same convenient API as the static methods.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlMapper customMapper = new XmlMapper();
     * customMapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
     * XmlMappers.One wrapper = XmlMappers.wrap(customMapper);
     * String xml = wrapper.toXml(person);
     * }</pre>
     *
     * @param xmlMapper the XmlMapper instance to wrap
     * @return a {@link One} instance wrapping the provided XmlMapper
     * @see One
     */
    public static One wrap(final XmlMapper xmlMapper) {
        return new One(xmlMapper);
    }

    /**
     * A wrapper class that provides convenient instance methods for XML serialization and deserialization
     * using a specific XmlMapper instance. This class mirrors the static methods of XmlMappers but uses
     * the wrapped XmlMapper for all operations.
     *
     * <p>This is useful when you need to use a customized XmlMapper repeatedly without having to
     * pass configuration objects to every method call.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlMapper mapper = new XmlMapper();
     * mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, true);
     * XmlMappers.One xmlMappers = XmlMappers.wrap(mapper);
     *
     * // Use the wrapped mapper for multiple operations
     * String xml1 = xmlMappers.toXml(object1);
     * String xml2 = xmlMappers.toXml(object2, true);   // returns with pretty print
     * Person person = xmlMappers.fromXml(xmlString, Person.class);
     * }</pre>
     *
     */
    public static final class One {
        private final XmlMapper xmlMapper;
        private final XmlMapper xmlMapperForPretty;

        /**
         * Creates a {@code One} instance wrapping the specified {@link XmlMapper}.
         * A copy of the mapper with {@link SerializationFeature#INDENT_OUTPUT} enabled is
         * created internally to support pretty-print serialization.
         *
         * @param xmlMapper the XmlMapper to wrap; must not be {@code null}
         */
        One(final XmlMapper xmlMapper) {
            this.xmlMapper = xmlMapper;
            xmlMapperForPretty = xmlMapper.copy();

            xmlMapperForPretty.enable(SerializationFeature.INDENT_OUTPUT);
        }

        /**
         * Serializes the specified object to an XML string using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Person person = new Person("John", 30);
         * String xml = xmlMappers.toXml(person);
         * }</pre>
         *
         * @param obj the object to serialize
         * @return the XML string representation of the object
         * @throws RuntimeException if serialization fails
         */
        public String toXml(final Object obj) {
            try {
                return xmlMapper.writeValueAsString(obj);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to an XML string with optional pretty formatting
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String compact = xmlMappers.toXml(N.asMap("name", "Bob"), false);   // <ImmutableMap><name>Bob</name></ImmutableMap>
         * String pretty = xmlMappers.toXml(N.asMap("name", "Bob"), true);     // same content, indented across lines
         * }</pre>
         *
         * @param obj the object to serialize
         * @param prettyFormat {@code true} to enable pretty printing with indentation
         * @return the XML string representation of the object
         * @throws RuntimeException if serialization fails
         */
        public String toXml(final Object obj, final boolean prettyFormat) {
            try {
                if (prettyFormat) {
                    return xmlMapperForPretty.writeValueAsString(obj);
                } else {
                    return xmlMapper.writeValueAsString(obj);
                }
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to an XML file using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * File output = new File("person.xml");
         * xmlMappers.toXml(N.asMap("name", "Bob"), output);   // writes <ImmutableMap><name>Bob</name></ImmutableMap>
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the output file to write the XML to
         * @throws RuntimeException if an I/O error occurs
         */
        public void toXml(final Object obj, final File output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to an output stream using the wrapped XmlMapper.
         * The stream is not closed by this method.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * ByteArrayOutputStream out = new ByteArrayOutputStream();
         * xmlMappers.toXml(N.asMap("name", "Bob"), out);   // writes XML; out contains "<name>Bob</name>"
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the output stream to write the XML to
         * @throws RuntimeException if an I/O error occurs
         */
        public void toXml(final Object obj, final OutputStream output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to a writer using the wrapped XmlMapper.
         * The writer is not closed by this method.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * StringWriter writer = new StringWriter();
         * xmlMappers.toXml(N.asMap("name", "Bob"), writer);   // writer.toString() contains "<name>Bob</name>"
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the writer to write the XML to
         * @throws RuntimeException if an I/O error occurs
         */
        public void toXml(final Object obj, final Writer output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Serializes the specified object to a DataOutput using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * ByteArrayOutputStream bytes = new ByteArrayOutputStream();
         * DataOutput out = new DataOutputStream(bytes);
         * xmlMappers.toXml(N.asMap("name", "Bob"), out);   // bytes.toString() contains "<name>Bob</name>"
         * }</pre>
         *
         * @param obj the object to serialize
         * @param output the DataOutput to write the XML to
         * @throws RuntimeException if an I/O error occurs
         */
        public void toXml(final Object obj, final DataOutput output) {
            try {
                xmlMapper.writeValue(output, obj);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a byte array into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "<LinkedHashMap><name>Bob</name></LinkedHashMap>".getBytes();
         * Map<String, Object> map = xmlMappers.fromXml(bytes, Map.class);   // map -> {name=Bob}
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array to deserialize
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a portion of a byte array into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "##<LinkedHashMap><name>Bob</name></LinkedHashMap>".getBytes();
         * Map<String, Object> map = xmlMappers.fromXml(bytes, 2, bytes.length - 2, Map.class);   // map -> {name=Bob}
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array containing the data
         * @param offset the start offset in the array
         * @param len the number of bytes to read
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final int offset, final int len, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes an XML string into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String xml = "<Person><name>John</name><age>30</age></Person>";
         * Person person = xmlMappers.fromXml(xml, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML string to deserialize
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String xml, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final JsonProcessingException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a file into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * File xmlFile = new File("person.xml");
         * Person person = xmlMappers.fromXml(xmlFile, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML file to read from
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails or file cannot be read
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final File xml, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from an input stream into an object of the specified type
         * using the wrapped XmlMapper. The stream is not closed by this method.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
         * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
         *     Map<String, Object> map = xmlMappers.fromXml(in, Map.class);   // map -> {name=Bob}
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the input stream containing XML data
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final InputStream xml, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a reader into an object of the specified type
         * using the wrapped XmlMapper. The reader is not closed by this method.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<LinkedHashMap><name>Bob</name></LinkedHashMap>";
         * try (Reader reader = new StringReader(xml)) {
         *     Map<String, Object> map = xmlMappers.fromXml(reader, Map.class);   // map -> {name=Bob}
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the reader containing XML data
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final Reader xml, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a URL into an object of the specified type
         * using the wrapped XmlMapper.
         * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * URL xmlUrl = new File("person.xml").toURI().toURL();
         * Person person = xmlMappers.fromXml(xmlUrl, Person.class);
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the URL pointing to XML data
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails or the URL cannot be accessed
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        @SuppressWarnings("deprecation")
        public <T> T fromXml(final URL xml, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a DataInput into an object of the specified type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
         * // Note: the underlying Jackson XML format does not support reading from a DataInput
         * // source, so this call throws an UnsupportedOperationException at runtime.
         * Person person = xmlMappers.fromXml(in, Person.class);   // throws UnsupportedOperationException
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the DataInput containing XML data
         * @param targetType the class of the object to deserialize to
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final DataInput xml, final Class<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a byte array into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "<List><item>a</item><item>b</item></List>".getBytes();
         * List<String> list = xmlMappers.fromXml(bytes, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array to deserialize
         * @param targetType the type reference describing the target type
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a portion of a byte array into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * byte[] bytes = "##<List><item>a</item><item>b</item></List>".getBytes();
         * List<String> list = xmlMappers.fromXml(bytes, 2, bytes.length - 2,
         *         new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML byte array containing the data
         * @param offset the start offset in the array
         * @param len the number of bytes to read
         * @param targetType the type reference describing the target type
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final byte[] xml, final int offset, final int len, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, offset, len, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes an XML string into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * String xml = "<ArrayList><item>a</item><item>b</item></ArrayList>";
         * List<String> list = xmlMappers.fromXml(xml, new TypeReference<List<String>>() {});
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML string to deserialize
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final String xml, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a file into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * File xmlFile = new File("items.xml");                                                    // contains <List><item>a</item><item>b</item></List>
         * List<String> list = xmlMappers.fromXml(xmlFile, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the XML file to read from
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails or file cannot be read
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final File xml, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from an input stream into an object of the specified generic type
         * using the wrapped XmlMapper. The stream is not closed by this method.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<List><item>a</item><item>b</item></List>";
         * try (InputStream in = new ByteArrayInputStream(xml.getBytes())) {
         *     List<String> list = xmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the InputStream containing XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final InputStream xml, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a reader into an object of the specified generic type
         * using the wrapped XmlMapper. The reader is not closed by this method.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * String xml = "<List><item>a</item><item>b</item></List>";
         * try (Reader reader = new StringReader(xml)) {
         *     List<String> list = xmlMappers.fromXml(reader, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the reader containing XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final Reader xml, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a URL into an object of the specified generic type
         * using the wrapped XmlMapper.
         * The URL may use any supported protocol (e.g., {@code http}, {@code file}, {@code jar}).
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * URL xmlUrl = new File("items.xml").toURI().toURL();                                     // file contains a <List> of items
         * List<String> list = xmlMappers.fromXml(xmlUrl, new TypeReference<List<String>>() {});   // list -> [a, b]
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the URL pointing to XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails or the URL cannot be accessed
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        @SuppressWarnings("deprecation")
        public <T> T fromXml(final URL xml, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, targetType);
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }

        /**
         * Deserializes XML from a DataInput into an object of the specified generic type
         * using the wrapped XmlMapper.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * XmlMappers.One xmlMappers = XmlMappers.wrap(new XmlMapper());
         * DataInput in = new DataInputStream(new ByteArrayInputStream(xmlBytes));
         * // Note: the underlying Jackson XML format does not support reading from a DataInput
         * // source, so this call throws an UnsupportedOperationException at runtime.
         * List<String> list = xmlMappers.fromXml(in, new TypeReference<List<String>>() {});   // throws UnsupportedOperationException
         * }</pre>
         *
         * @param <T> the type of the object to return
         * @param xml the DataInput containing XML data
         * @param targetType the type reference describing the target type, can be the {@code Type} of {@code Bean/Array/Collection/Map}
         * @return the deserialized object
         * @throws RuntimeException if deserialization fails
         * @see com.fasterxml.jackson.core.type.TypeReference
         */
        public <T> T fromXml(final DataInput xml, final TypeReference<? extends T> targetType) {
            try {
                return xmlMapper.readValue(xml, xmlMapper.constructType(targetType));
            } catch (final IOException e) {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }
}
