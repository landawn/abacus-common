/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.parser;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.esotericsoftware.kryo.Serializer;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple2;

/**
 * A factory for creating Parser objects.
 *
 * @author Haiyang Li
 * @since 0.8
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
            } catch (Throwable e) {
                // ignore;
            }

            isAbacusXMLAvailable = isAvailable;
        }

        {

            boolean isAvailable = false;

            try {
                new XMLParserImpl(XMLParserType.StAX);
                isAvailable = true;
            } catch (Throwable e) {
                // ignore;
            }

            isXMLAvailable = isAvailable;
        }

        {
            boolean isAvailable = false;

            try {
                org.apache.avro.Schema schema = new org.apache.avro.Schema.Parser().parse("{\"namespace\": \"example.avro\",\r\n" + " \"type\": \"record\",\r\n" //NOSONAR
                        + " \"name\": \"User\",\r\n" + " \"fields\": [\r\n" + "     {\"name\": \"name\", \"type\": \"string\"},\r\n"
                        + "     {\"name\": \"favorite_number\",  \"type\": [\"int\", \"null\"]},\r\n"
                        + "     {\"name\": \"favorite_color\", \"type\": [\"string\", \"null\"]}\r\n" + " ]\r\n" + "}");

                new org.apache.avro.generic.GenericData.Record(schema);
                isAvailable = true;
            } catch (Throwable e) {
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
            } catch (Throwable e) {
                // ignore;
            }

            isKryoAvailable = isAvailable;
        }
    }

    static final Set<Class<?>> _kryoClassSet = new HashSet<>();

    static final Map<Class<?>, Integer> _kryoClassIdMap = new ConcurrentHashMap<>();

    static final Map<Class<?>, Serializer<?>> _kryoClassSerializerMap = new ConcurrentHashMap<>();

    static final Map<Class<?>, Tuple2<Serializer<?>, Integer>> _kryoClassSerializerIdMap = new ConcurrentHashMap<>();

    private ParserFactory() {
        // singleton
    }

    /**
     * Checks if is abacus XML available.
     *
     * @return true, if is abacus XML available
     */
    public static boolean isAbacusXMLAvailable() {
        return isAbacusXMLAvailable;
    }

    /**
     * Checks if is XML available.
     *
     * @return true, if is XML available
     */
    public static boolean isXMLAvailable() {
        return isXMLAvailable;
    }

    /**
     * Checks if is avro available.
     *
     * @return true, if is avro available
     */
    public static boolean isAvroAvailable() {
        return isAvroAvailable;
    }

    /**
     * Checks if is kryo available.
     *
     * @return true, if is kryo available
     */
    public static boolean isKryoAvailable() {
        return isKryoAvailable;
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    public static AvroParser createAvroParser() {
        return new AvroParser();
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    public static KryoParser createKryoParser() {
        return new KryoParser();
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    public static JSONParser createJSONParser() {
        return new JSONParserImpl();
    }

    /**
     *
     * @param jsc
     * @param jdc
     * @return
     */
    public static JSONParser createJSONParser(final JSONSerializationConfig jsc, final JSONDeserializationConfig jdc) {
        return new JSONParserImpl(jsc, jdc);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    public static XMLParser createAbacusXMLParser() {
        return new AbacusXMLParserImpl(XMLParserType.StAX);
    }

    /**
     *
     * @param xsc
     * @param xdc
     * @return
     */
    public static XMLParser createAbacusXMLParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        return new AbacusXMLParserImpl(XMLParserType.StAX, xsc, xdc);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    static XMLParser createAbacusXMLSAXParser() {
        return new AbacusXMLParserImpl(XMLParserType.SAX);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    static XMLParser createAbacusXMLStAXParser() {
        return new AbacusXMLParserImpl(XMLParserType.StAX);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    static XMLParser createAbacusXMLDOMParser() {
        return new AbacusXMLParserImpl(XMLParserType.DOM);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    public static XMLParser createXMLParser() {
        return new XMLParserImpl(XMLParserType.StAX);
    }

    /**
     * Creates a new Parser object.
     *
     * @param xsc
     * @param xdc
     * @return
     */
    public static XMLParser createXMLParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        return new XMLParserImpl(XMLParserType.StAX, xsc, xdc);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    static XMLParser createXMLStAXParser() {
        return new XMLParserImpl(XMLParserType.StAX);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    static XMLParser createXMLDOMParser() {
        return new XMLParserImpl(XMLParserType.DOM);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    public static XMLParser createJAXBParser() {
        return new JAXBParser();
    }

    /**
     *
     *
     * @param xsc
     * @param xdc
     * @return
     */
    public static XMLParser createJAXBParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        return new JAXBParser(xsc, xdc);
    }

    //    /**
    //     * Creates a new Parser object.
    //     *
    //     * @return
    //     */
    //    public static JacksonMapper createJacksonMapper() {
    //        return new JacksonMapper();
    //    }
    //
    //    /**
    //     *
    //     * @param jmc
    //     * @return
    //     */
    //    public static JacksonMapper createJacksonMapper(final JacksonMapperConfig jmc) {
    //        return new JacksonMapper(jmc);
    //    }

    /**
     * 
     *
     * @param type 
     * @throws IllegalArgumentException 
     */
    public static void registerKryo(final Class<?> type) throws IllegalArgumentException {
        N.checkArgNotNull(type, "type");

        _kryoClassSet.add(type);
    }

    /**
     * 
     *
     * @param type 
     * @param id 
     * @throws IllegalArgumentException 
     */
    public static void registerKryo(final Class<?> type, final int id) throws IllegalArgumentException {
        N.checkArgNotNull(type, "type");

        _kryoClassIdMap.put(type, id);
    }

    /**
     * 
     *
     * @param type 
     * @param serializer 
     * @throws IllegalArgumentException 
     */
    public static void registerKryo(final Class<?> type, final Serializer<?> serializer) throws IllegalArgumentException {
        N.checkArgNotNull(type, "type");
        N.checkArgNotNull(serializer, "serializer");

        _kryoClassSerializerMap.put(type, serializer);
    }

    /**
     * 
     *
     * @param type 
     * @param serializer 
     * @param id 
     * @throws IllegalArgumentException 
     */
    public static void registerKryo(final Class<?> type, final Serializer<?> serializer, final int id) throws IllegalArgumentException {
        N.checkArgNotNull(type, "type");
        N.checkArgNotNull(serializer, "serializer");

        _kryoClassSerializerIdMap.put(type, Tuple.of(serializer, id));
    }
}
