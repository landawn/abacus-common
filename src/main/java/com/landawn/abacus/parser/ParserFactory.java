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

// TODO: Auto-generated Javadoc
/**
 * A factory for creating Parser objects.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class ParserFactory {

    /** The Constant isAbacusXMLAvailable. */
    // Check for Android.
    private static final boolean isAbacusXMLAvailable;

    /** The Constant isXMLAvailable. */
    private static final boolean isXMLAvailable;

    /** The Constant isAvroAvailable. */
    private static final boolean isAvroAvailable;

    /** The Constant isKryoAvailable. */
    private static final boolean isKryoAvailable;

    static {
        // initial N to avoid below error if 'ParserFactory' is called before N initialized.
        //    java.lang.NoClassDefFoundError: Could not initialize class com.landawn.abacus.parser.JSONParserImpl
        //    at com.landawn.abacus.parser.ParserFactory.createJSONParser(ParserFactory.java:188)
        //    at com.landawn.abacus.util.MongoDBExecutor.<clinit>(MongoDBExecutor.java:92)
        //    at com.landawn.abacus.util.MongoDBExecutorTest.<clinit>(MongoDBExecutorTest.java:42)

        {
            Boolean isAvailable = false;

            try {
                new AbacusXMLParserImpl(XMLParserType.StAX);
                isAvailable = true;
            } catch (Throwable e) {
                // ignore;
            }

            isAbacusXMLAvailable = isAvailable;
        }

        {

            Boolean isAvailable = false;

            try {
                new XMLParserImpl(XMLParserType.StAX);
                isAvailable = true;
            } catch (Throwable e) {
                // ignore;
            }

            isXMLAvailable = isAvailable;
        }

        {
            Boolean isAvailable = false;

            try {
                org.apache.avro.Schema schema = new org.apache.avro.Schema.Parser().parse("{\"namespace\": \"example.avro\",\r\n" + " \"type\": \"record\",\r\n"
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
            Boolean isAvailable = false;

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

    /**
     * Instantiates a new parser factory.
     */
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

    public static XMLParser createJAXBParser(final XMLSerializationConfig xsc, final XMLDeserializationConfig xdc) {
        return new JAXBParser(xsc, xdc);
    }

    /**
     * Creates a new Parser object.
     *
     * @return
     */
    public static JacksonMapper createJacksonMapper() {
        return new JacksonMapper();
    }

    /**
     *
     * @param jmc
     * @return
     */
    public static JacksonMapper createJacksonMapper(final JacksonMapperConfig jmc) {
        return new JacksonMapper(jmc);
    }
}
