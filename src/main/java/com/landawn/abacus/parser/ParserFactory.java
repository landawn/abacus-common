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
 * if (ParserFactory.isKryoAvailable()) {
 *     KryoParser parser = ParserFactory.createKryoParser();
 *     // Use Kryo parser
 * }
 * 
 * // Create JSON parser (always available)
 * JSONParser jsonParser = ParserFactory.createJSONParser();
 * 
 * // Create XML parser with configuration
 * XMLSerializationConfig xsc = new XMLSerializationConfig();
 * XMLDeserializationConfig xdc = new XMLDeserializationConfig();
 * XMLParser xmlParser = ParserFactory.createXMLParser(xsc, xdc);
 * 
 * // Register Kryo classes for better performance
 * ParserFactory.registerKryo(MyClass.class);
 * ParserFactory.registerKryo(MyClass.class, 100); // with ID
 * }</pre>
 * 
 * @see JSONParser
 * @see XMLParser
 * @see AvroParser
 * @see KryoParser
 * @since 1.0
 */
@SuppressWarnings("unused")
public final class ParserFactory {

    // Check for Android.
    private static final boolean isAbacusXMLAvailable;

    private static final boolean isXMLAvailable;

    private static final boolean isAvroAvailable;

    private static final boolean isKryoAvailable;

    static {
        // initial N to avoid below error if 'ParserFactory' is called before N initialized.
        //    java.lang.NoClassDefFoundError: Could not initialize class com.landawn.abacus.parser.JSONParserImpl
        //    at com.landawn.abacus.parser.ParserFactory.createJSONParser(ParserFactory.java:188)
        //    at com.landawn.abacus.util.MongoDBExecutor.<clinit>(MongoDBExecutor.java:92)
        //    at com.landawn.abacus.util.MongoDBExecutorTest.<clinit>(MongoDBExecutorTest.java:42)

        {
            boolean isAvailable = false;

            try {
                new AbacusXMLParserImpl(XMLParserType.StAX);
                isAvailable = true;
            } catch (final Throwable e) {
                // ignore;
            }

            isAbacusXMLAvailable = isAvailable;
        }

        {

            boolean isAvailable = false;

            try {
                new XMLParserImpl(XMLParserType.StAX);
                isAvailable = true;
            } catch (final Throwable e) {
                // ignore;
            }

            isXMLAvailable = isAvailable;
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

            isAvroAvailable = isAvailable;
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

            isKryoAvailable = isAvailable;
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
     * Checks if Abacus XML parser is available.
     * Abacus XML parser provides extended XML functionality specific to the Abacus framework.
     * 
     * @return {@code true} if Abacus XML parser is available, {@code false} otherwise
     */
    public static boolean isAbacusXMLAvailable() {
        return isAbacusXMLAvailable;
    }

    /**
     * Checks if standard XML parser is available.
     * This checks for the availability of standard Java XML processing capabilities.
     * 
     * @return {@code true} if XML parser is available, {@code false} otherwise
     */
    public static boolean isXMLAvailable() {
        return isXMLAvailable;
    }

    /**
     * Checks if Apache Avro parser is available.
     * Avro is a data serialization system that provides rich data structures and a compact, fast, binary data format.
     * 
     * @return {@code true} if Avro parser is available, {@code false} otherwise
     */
    public static boolean isAvroAvailable() {
        return isAvroAvailable;
    }

    /**
     * Checks if Kryo parser is available.
     * Kryo is a fast and efficient object graph serialization framework for Java.
     * 
     * @return {@code true} if Kryo parser is available, {@code false} otherwise
     */
    public static boolean isKryoAvailable() {
        return isKryoAvailable;
    }

    /**
     * Creates a new Avro parser instance.
     * Avro provides schema evolution and is particularly useful for data interchange.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isAvroAvailable()) {
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
     * if (ParserFactory.isKryoAvailable()) {
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
     * JSONParser parser = ParserFactory.createJSONParser();
     * String json = parser.serialize(myObject);
     * MyObject obj = parser.deserialize(json, MyObject.class);
     * }</pre>
     *
     * @return a new {@link JSONParser} instance
     */
    public static JSONParser createJSONParser() {
        return new JSONParserImpl();
    }

    /**
     * Creates a new JSON parser instance with specified configurations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JSONSerializationConfig jsc = new JSONSerializationConfig()
     *     .prettyFormat(true)
     *     .writeLongAsString(true);
     * JSONDeserializationConfig jdc = new JSONDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     *
     * JSONParser parser = ParserFactory.createJSONParser(jsc, jdc);
     * }</pre>
     *
     * @param jsc the JSON serialization configuration
     * @param jdc the JSON deserialization configuration
     * @return a new {@link JSONParser} instance with the specified configurations
     */
    public static JSONParser createJSONParser(final JSONSerializationConfig jsc, final JSONDeserializationConfig jdc) {
        return new JSONParserImpl(jsc, jdc);
    }

    /**
     * Creates a new Abacus XML parser instance with default configuration.
     * Uses StAX (Streaming API for XML) as the default parser type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isAbacusXMLAvailable()) {
     *     XMLParser parser = ParserFactory.createAbacusXMLParser();
     *     String xml = parser.serialize(myObject);
     * }
     * }</pre>
     *
     * @return a new Abacus {@link XMLParser} instance
     * @throws NoClassDefFoundError if Abacus XML support is not available
     */
    public static XMLParser createAbacusXMLParser() {
        return new AbacusXMLParserImpl(XMLParserType.StAX);
    }

    /**
     * Creates a new Abacus XML parser instance with specified configurations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLSerializationConfig xsc = new XMLSerializationConfig()
     *     .tagByPropertyName(true)
     *     .writeTypeInfo(false);
     * XMLDeserializationConfig xdc = new XMLDeserializationConfig()
     *     .ignoreUnmatchedProperty(true);
     *
     * XMLParser parser = ParserFactory.createAbacusXMLParser(xsc, xdc);
     * }</pre>
     *
     * @param xsc the XML serialization configuration
     * @param xdc the XML deserialization configuration
     * @return a new Abacus {@link XMLParser} instance with the specified configurations
     * @throws NoClassDefFoundError if Abacus XML support is not available
     */
    public static XMLParser createAbacusXMLParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        return new AbacusXMLParserImpl(XMLParserType.StAX, xsc, xdc);
    }

    /**
     * Creates a new Abacus XML SAX parser instance.
     * SAX parser is event-driven and memory efficient for large documents.
     * 
     * @return a new Abacus XML SAX parser
     */
    static XMLParser createAbacusXMLSAXParser() {
        return new AbacusXMLParserImpl(XMLParserType.SAX);
    }

    /**
     * Creates a new Abacus XML StAX parser instance.
     * StAX parser provides a good balance between performance and ease of use.
     * 
     * @return a new Abacus XML StAX parser
     */
    static XMLParser createAbacusXMLStAXParser() {
        return new AbacusXMLParserImpl(XMLParserType.StAX);
    }

    /**
     * Creates a new Abacus XML DOM parser instance.
     * DOM parser loads the entire document into memory but provides random access.
     * 
     * @return a new Abacus XML DOM parser
     */
    static XMLParser createAbacusXMLDOMParser() {
        return new AbacusXMLParserImpl(XMLParserType.DOM);
    }

    /**
     * Creates a new standard XML parser instance with default configuration.
     * Uses StAX (Streaming API for XML) as the default parser type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (ParserFactory.isXMLAvailable()) {
     *     XMLParser parser = ParserFactory.createXMLParser();
     *     String xml = parser.serialize(myObject);
     * }
     * }</pre>
     *
     * @return a new standard {@link XMLParser} instance
     * @throws NoClassDefFoundError if XML support is not available
     */
    public static XMLParser createXMLParser() {
        return new XMLParserImpl(XMLParserType.StAX);
    }

    /**
     * Creates a new standard XML parser instance with specified configurations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLSerializationConfig xsc = new XMLSerializationConfig()
     *     .prettyFormat(true);
     * XMLDeserializationConfig xdc = new XMLDeserializationConfig()
     *     .setElementType(String.class);
     *
     * XMLParser parser = ParserFactory.createXMLParser(xsc, xdc);
     * }</pre>
     *
     * @param xsc the XML serialization configuration
     * @param xdc the XML deserialization configuration
     * @return a new standard {@link XMLParser} instance with the specified configurations
     * @throws NoClassDefFoundError if XML support is not available
     */
    public static XMLParser createXMLParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        return new XMLParserImpl(XMLParserType.StAX, xsc, xdc);
    }

    /**
     * Creates a new XML StAX parser instance.
     * 
     * @return a new XML StAX parser
     */
    static XMLParser createXMLStAXParser() {
        return new XMLParserImpl(XMLParserType.StAX);
    }

    /**
     * Creates a new XML DOM parser instance.
     * 
     * @return a new XML DOM parser
     */
    static XMLParser createXMLDOMParser() {
        return new XMLParserImpl(XMLParserType.DOM);
    }

    /**
     * Creates a new JAXB parser instance with default configuration.
     * JAXB (Java Architecture for XML Binding) provides annotation-based XML binding.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLParser parser = ParserFactory.createJAXBParser();
     * // Use with JAXB-annotated classes
     * }</pre>
     *
     * @return a new JAXB {@link XMLParser} instance
     */
    public static XMLParser createJAXBParser() {
        return new JAXBParser();
    }

    /**
     * Creates a new JAXB parser instance with specified configurations.
     * JAXB (Java Architecture for XML Binding) provides annotation-based XML binding with customizable behavior.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * XMLSerializationConfig xsc = new XMLSerializationConfig().prettyFormat(true);
     * XMLDeserializationConfig xdc = new XMLDeserializationConfig();
     * XMLParser parser = ParserFactory.createJAXBParser(xsc, xdc);
     * }</pre>
     *
     * @param xsc the XML serialization configuration, configures how objects are converted to XML
     * @param xdc the XML deserialization configuration, configures how XML is converted to objects
     * @return a new JAXB {@link XMLParser} instance with the specified configurations
     */
    public static XMLParser createJAXBParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
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
     * @param type the class to register
     * @throws IllegalArgumentException if type is null
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
     * @param type the class to register
     * @param id the unique ID for this class
     * @throws IllegalArgumentException if type is null
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
     * @param type the class to register
     * @param serializer the custom serializer for this class
     * @throws IllegalArgumentException if type or serializer is null
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
     * @param type the class to register
     * @param serializer the custom serializer for this class
     * @param id the unique ID for this class
     * @throws IllegalArgumentException if type or serializer is null
     */
    public static void registerKryo(final Class<?> type, final Serializer<?> serializer, final int id) throws IllegalArgumentException {
        N.checkArgNotNull(type, cs.type);
        N.checkArgNotNull(serializer, cs.serializer);

        _kryoClassSerializerIdMap.put(type, Tuple.of(serializer, id));
    }
}
