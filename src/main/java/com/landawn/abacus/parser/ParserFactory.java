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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Serializer;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.cs;

/**
 * Factory class for creating various parser instances.
 * This factory provides methods to create parsers for different serialization formats
 * including JSON, XML, Avro, and Kryo. It also manages parser availability checks
 * and Kryo class registration.
 * 
 * <p>The factory performs availability checks for optional dependencies at class loading time:
 * <ul>
 *   <li>Abacus XML support (requires XML processing libraries)</li>
 *   <li>Standard XML support (requires JAXP)</li>
 *   <li>Avro support (requires Apache Avro library)</li>
 *   <li>Kryo support (requires Kryo library)</li>
 * </ul>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check availability before creating parsers
 * if (ParserFactory.isKryoParserAvailable()) {
 *     KryoParser parser = ParserFactory.createKryoParser();
 *     // Use Kryo parser
 * }
 * 
 * // Create JSON parser (always available)
 * JsonParser jsonParser = ParserFactory.createJsonParser();
 * 
 * // Create XML parser with configuration
 * XmlSerializationConfig xsc = new XmlSerializationConfig();
 * XmlDeserializationConfig xdc = new XmlDeserializationConfig();
 * XmlParser xmlParser = ParserFactory.createXmlParser(xsc, xdc);
 * 
 * // Register Kryo classes for better performance
 * ParserFactory.registerKryo(MyClass.class);
 * ParserFactory.registerKryo(MyClass.class, 100);   // with ID
 * }</pre>
 * 
 * @see JsonParser
 * @see XmlParser
 * @see AvroParser
 * @see KryoParser
 */
@SuppressWarnings("unused")
public final class ParserFactory {

    // Check for Android.
    private static final boolean isAbacusXmlParserAvailable;

    private static final boolean isXmlParserAvailable;

    private static final boolean isAvroParserAvailable;

    private static final boolean isKryoParserAvailable;

    static {
        // initial N to avoid below error if 'ParserFactory' is called before N initialized.
        //    java.lang.NoClassDefFoundError: Could not initialize class com.landawn.abacus.parser.JsonParserImpl
        //    at com.landawn.abacus.parser.ParserFactory.createJsonParser(ParserFactory.java:188)
        //    at com.landawn.abacus.util.MongoDBExecutor.<clinit>(MongoDBExecutor.java:92)
        //    at com.landawn.abacus.util.MongoDBExecutorTest.<clinit>(MongoDBExecutorTest.java:42)

        {
            boolean isAvailable = false;

            try {
                new AbacusXmlParserImpl(XmlParserType.StAX);
                isAvailable = true;
            } catch (final Throwable e) {
                // ignore;
            }

            isAbacusXmlParserAvailable = isAvailable;
        }

        {

            boolean isAvailable = false;

            try {
                new XmlParserImpl(XmlParserType.StAX);
                isAvailable = true;
            } catch (final Throwable e) {
                // ignore;
            }

            isXmlParserAvailable = isAvailable;
        }

        {
            boolean isAvailable = false;

            try {
                final org.apache.avro.Schema schema = new org.apache.avro.Schema.Parser().parse("""
                        {"namespace": "example.avro",\r
                         "type": "record",\r
                         "name": "User",\r
                         "fields": [\r
                             {"name": "name", "type": "string"},\r
                             {"name": "favorite_number",  "type": ["int", "null"]},\r
                             {"name": "favorite_color", "type": ["string", "null"]}\r
                         ]\r
                        }""");

                new org.apache.avro.generic.GenericData.Record(schema);
                isAvailable = true;
            } catch (final Throwable e) {
                // ignore;
            }

            isAvroParserAvailable = isAvailable;
        }

        {
            boolean isAvailable = false;

            try {
                Class.forName("com.esotericsoftware.kryo.Kryo");
                ParserFactory.createKryoParser();
                isAvailable = true;
            } catch (final Throwable e) {
                // ignore;
            }

            isKryoParserAvailable = isAvailable;
        }
    }

    static final Set<Class<?>> _kryoClassSet = Collections.newSetFromMap(new ConcurrentHashMap<>());

    static final Map<Class<?>, Integer> _kryoClassIdMap = new ConcurrentHashMap<>();

    static final Map<Class<?>, Serializer<?>> _kryoClassSerializerMap = new ConcurrentHashMap<>();

    static final Map<Class<?>, Tuple2<Serializer<?>, Integer>> _kryoClassSerializerIdMap = new ConcurrentHashMap<>();

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ParserFactory() {
        // singleton
    }

    /**
     * Checks if abacus-common XML parser is available.
     * abacus-common XML parser provides extended XML functionality specific to the abacus-common framework.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isAbacusXmlParserAvailable()) {
     *     XmlParser parser = ParserFactory.createAbacusXmlParser();
     * }
     * }</pre>
     * 
     * @return {@code true} if abacus-common XML parser is available, {@code false} otherwise
     */
    public static boolean isAbacusXmlParserAvailable() {
        return isAbacusXmlParserAvailable;
    }

    /**
     * Checks if standard XML parser is available.
     * This checks for the availability of standard Java XML processing capabilities.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isXmlParserAvailable()) {
     *     XmlParser parser = ParserFactory.createXmlParser();
     * }
     * }</pre>
     * 
     * @return {@code true} if XML parser is available, {@code false} otherwise
     */
    public static boolean isXmlParserAvailable() {
        return isXmlParserAvailable;
    }

    /**
     * Checks if Apache Avro parser is available.
     * Avro is a data serialization system that provides rich data structures and a compact, fast, binary data format.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isAvroParserAvailable()) {
     *     AvroParser parser = ParserFactory.createAvroParser();
     * }
     * }</pre>
     * 
     * @return {@code true} if Avro parser is available, {@code false} otherwise
     */
    public static boolean isAvroParserAvailable() {
        return isAvroParserAvailable;
    }

    /**
     * Checks if Kryo parser is available.
     * Kryo is a fast and efficient object graph serialization framework for Java.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isKryoParserAvailable()) {
     *     KryoParser parser = ParserFactory.createKryoParser();
     * }
     * }</pre>
     * 
     * @return {@code true} if Kryo parser is available, {@code false} otherwise
     */
    public static boolean isKryoParserAvailable() {
        return isKryoParserAvailable;
    }

    /**
     * Creates a new Avro parser instance.
     * Avro provides schema evolution and is particularly useful for data interchange.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isAvroParserAvailable()) {
     *     AvroParser parser = ParserFactory.createAvroParser();
     *     // Use Avro parser
     * }
     * }</pre>
     *
     * @return a new {@link AvroParser} instance
     * @throws NoClassDefFoundError if Avro library is not available
     */
    public static AvroParser createAvroParser() {
        return new AvroParser();
    }

    /**
     * Creates a new Kryo parser instance.
     * Kryo provides fast binary serialization with automatic deep copying.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isKryoParserAvailable()) {
     *     KryoParser parser = ParserFactory.createKryoParser();
     *     byte[] data = parser.serialize(myObject);
     * }
     * }</pre>
     *
     * @return a new {@link KryoParser} instance
     * @throws NoClassDefFoundError if Kryo library is not available
     */
    public static KryoParser createKryoParser() {
        return new KryoParser();
    }

    /**
     * Creates a new JSON parser instance with default configuration.
     * JSON parser is always available as it has no external dependencies.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonParser parser = ParserFactory.createJsonParser();
     * String json = parser.serialize(myObject);
     * MyObject obj = parser.deserialize(json, MyObject.class);
     * }</pre>
     *
     * @return a new {@link JsonParser} instance
     * @throws NoClassDefFoundError if JSON parser implementation is not available
     */
    public static JsonParser createJsonParser() {
        return new JsonParserImpl();
    }

    /**
     * Creates a new JSON parser instance with specified configurations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JsonSerializationConfig jsc = new JsonSerializationConfig()
     *     .prettyFormat(true)
     *     .writeLongAsString(true);
     * JsonDeserializationConfig jdc = new JsonDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     *
     * JsonParser parser = ParserFactory.createJsonParser(jsc, jdc);
     * }</pre>
     *
     * @param jsc the JSON serialization configuration (may be {@code null} for default behavior)
     * @param jdc the JSON deserialization configuration (may be {@code null} for default behavior)
     * @return a new {@link JsonParser} instance with the specified configurations
     * @throws NoClassDefFoundError if JSON parser implementation is not available
     */
    public static JsonParser createJsonParser(final JsonSerializationConfig jsc, final JsonDeserializationConfig jdc) {
        return new JsonParserImpl(jsc, jdc);
    }

    /**
     * Creates a new abacus-common XML parser instance with default configuration.
     * Uses StAX (Streaming API for XML) as the default parser type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isAbacusXmlParserAvailable()) {
     *     XmlParser parser = ParserFactory.createAbacusXmlParser();
     *     String xml = parser.serialize(myObject);
     * }
     * }</pre>
     *
     * @return a new abacus-common {@link XmlParser} instance
     * @throws NoClassDefFoundError if abacus-common XML support is not available
     */
    public static XmlParser createAbacusXmlParser() {
        return new AbacusXmlParserImpl(XmlParserType.StAX);
    }

    /**
     * Creates a new abacus-common XML parser instance with specified configurations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerializationConfig xsc = new XmlSerializationConfig()
     *     .tagByPropertyName(true)
     *     .writeTypeInfo(false);
     * XmlDeserializationConfig xdc = new XmlDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     *
     * XmlParser parser = ParserFactory.createAbacusXmlParser(xsc, xdc);
     * }</pre>
     *
     * @param xsc the XML serialization configuration (may be {@code null} for default behavior)
     * @param xdc the XML deserialization configuration (may be {@code null} for default behavior)
     * @return a new abacus-common {@link XmlParser} instance with the specified configurations
     * @throws NoClassDefFoundError if abacus-common XML support is not available
     */
    public static XmlParser createAbacusXmlParser(final XmlSerializationConfig xsc, final XmlDeserializationConfig xdc) {
        return new AbacusXmlParserImpl(XmlParserType.StAX, xsc, xdc);
    }

    /**
     * Creates a new abacus-common XML SAX parser instance.
     * SAX parser is event-driven and memory efficient for large documents.
     * 
     * @return a new abacus-common XML SAX parser
     */
    static XmlParser createAbacusXmlSAXParser() {
        return new AbacusXmlParserImpl(XmlParserType.SAX);
    }

    /**
     * Creates a new abacus-common XML StAX parser instance.
     * StAX parser provides a good balance between performance and ease of use.
     * 
     * @return a new abacus-common XML StAX parser
     */
    static XmlParser createAbacusXmlStAXParser() {
        return new AbacusXmlParserImpl(XmlParserType.StAX);
    }

    /**
     * Creates a new abacus-common XML DOM parser instance.
     * DOM parser loads the entire document into memory but provides random access.
     * 
     * @return a new abacus-common XML DOM parser
     */
    static XmlParser createAbacusXmlDOMParser() {
        return new AbacusXmlParserImpl(XmlParserType.DOM);
    }

    /**
     * Creates a new standard XML parser instance with default configuration.
     * Uses StAX (Streaming API for XML) as the default parser type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isXmlParserAvailable()) {
     *     XmlParser parser = ParserFactory.createXmlParser();
     *     String xml = parser.serialize(myObject);
     * }
     * }</pre>
     *
     * @return a new standard {@link XmlParser} instance
     * @throws NoClassDefFoundError if XML support is not available
     */
    public static XmlParser createXmlParser() {
        return new XmlParserImpl(XmlParserType.StAX);
    }

    /**
     * Creates a new standard XML parser instance with specified configurations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerializationConfig xsc = new XmlSerializationConfig()
     *     .prettyFormat(true);
     * XmlDeserializationConfig xdc = new XmlDeserializationConfig()
     *     .setElementType(String.class);
     *
     * XmlParser parser = ParserFactory.createXmlParser(xsc, xdc);
     * }</pre>
     *
     * @param xsc the XML serialization configuration (may be {@code null} for default behavior)
     * @param xdc the XML deserialization configuration (may be {@code null} for default behavior)
     * @return a new standard {@link XmlParser} instance with the specified configurations
     * @throws NoClassDefFoundError if XML support is not available
     */
    public static XmlParser createXmlParser(final XmlSerializationConfig xsc, final XmlDeserializationConfig xdc) {
        return new XmlParserImpl(XmlParserType.StAX, xsc, xdc);
    }

    /**
     * Creates a new XML StAX parser instance.
     * 
     * @return a new XML StAX parser
     */
    static XmlParser createXmlStAXParser() {
        return new XmlParserImpl(XmlParserType.StAX);
    }

    /**
     * Creates a new XML DOM parser instance.
     * 
     * @return a new XML DOM parser
     */
    static XmlParser createXmlDOMParser() {
        return new XmlParserImpl(XmlParserType.DOM);
    }

    /**
     * Creates a new JAXB parser instance with default configuration.
     * JAXB (Java Architecture for XML Binding) provides annotation-based XML binding.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlParser parser = ParserFactory.createJAXBParser();
     * // Use with JAXB-annotated classes
     * }</pre>
     *
     * @return a new JAXB {@link XmlParser} instance
     * @throws NoClassDefFoundError if JAXB implementation is not available
     */
    public static XmlParser createJAXBParser() {
        return new JAXBParser();
    }

    /**
     * Creates a new JAXB parser instance with specified configurations.
     * JAXB (Java Architecture for XML Binding) provides annotation-based XML binding with customizable behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XmlSerializationConfig xsc = new XmlSerializationConfig().prettyFormat(true);
     * XmlDeserializationConfig xdc = new XmlDeserializationConfig();
     * XmlParser parser = ParserFactory.createJAXBParser(xsc, xdc);
     * }</pre>
     *
     * @param xsc the XML serialization configuration (may be {@code null} for default behavior)
     * @param xdc the XML deserialization configuration (may be {@code null} for default behavior)
     * @return a new JAXB {@link XmlParser} instance with the specified configurations
     * @throws NoClassDefFoundError if JAXB implementation is not available
     */
    public static XmlParser createJAXBParser(final XmlSerializationConfig xsc, final XmlDeserializationConfig xdc) {
        return new JAXBParser(xsc, xdc);
    }

    /**
     * Registers a class with Kryo for serialization.
     * Registration can improve performance and reduce serialized size.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Register commonly used classes
     * ParserFactory.registerKryo(MyDomainObject.class);
     * ParserFactory.registerKryo(MyValueObject.class);
     * }</pre>
     *
     * @param type the class to register (must not be {@code null})
     * @throws IllegalArgumentException if type is {@code null}
     */
    public static void registerKryo(final Class<?> type) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);

        _kryoClassSet.add(type);
    }

    /**
     * Registers a class with Kryo using a specific ID.
     * Using fixed IDs ensures compatibility across different JVM instances.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Register with fixed IDs for compatibility
     * ParserFactory.registerKryo(User.class, 100);
     * ParserFactory.registerKryo(Order.class, 101);
     * ParserFactory.registerKryo(Product.class, 102);
     * }</pre>
     *
     * @param type the class to register (must not be {@code null})
     * @param id the unique ID for this class
     * @throws IllegalArgumentException if type is {@code null}
     */
    public static void registerKryo(final Class<?> type, final int id) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);

        _kryoClassIdMap.put(type, id);
    }

    /**
     * Registers a class with Kryo using a custom serializer.
     * Custom serializers can handle special serialization requirements.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Register with custom serializer
     * ParserFactory.registerKryo(DateTime.class, new DateTimeSerializer());
     * ParserFactory.registerKryo(Money.class, new MoneySerializer());
     * }</pre>
     *
     * @param type the class to register (must not be {@code null})
     * @param serializer the custom serializer for this class (must not be {@code null})
     * @throws IllegalArgumentException if type or serializer is {@code null}
     */
    public static void registerKryo(final Class<?> type, final Serializer<?> serializer) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);
        N.checkArgNotNull(serializer, cs.serializer);

        _kryoClassSerializerMap.put(type, serializer);
    }

    /**
     * Registers a class with Kryo using a custom serializer and specific ID.
     * Combines the benefits of custom serialization and fixed IDs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Register with both custom serializer and ID
     * ParserFactory.registerKryo(BigDecimal.class, new BigDecimalSerializer(), 200);
     * ParserFactory.registerKryo(UUID.class, new UUIDSerializer(), 201);
     * }</pre>
     *
     * @param type the class to register (must not be {@code null})
     * @param serializer the custom serializer for this class (must not be {@code null})
     * @param id the unique ID for this class
     * @throws IllegalArgumentException if type or serializer is {@code null}
     */
    public static void registerKryo(final Class<?> type, final Serializer<?> serializer, final int id) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);
        N.checkArgNotNull(serializer, cs.serializer);

        _kryoClassSerializerIdMap.put(type, Tuple.of(serializer, id));
    }
}
