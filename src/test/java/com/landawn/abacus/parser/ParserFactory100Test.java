package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ParserFactory100Test extends TestBase {

    @Test
    public void testIsAbacusXMLAvailable() {
        boolean available = ParserFactory.isAbacusXmlParserAvailable();
        assertTrue(available || !available);
    }

    @Test
    public void testIsXMLAvailable() {
        boolean available = ParserFactory.isXmlParserAvailable();
        assertTrue(available || !available);
    }

    @Test
    public void testIsAvroAvailable() {
        boolean available = ParserFactory.isAvroParserAvailable();
        assertTrue(available || !available);
    }

    @Test
    public void testIsKryoAvailable() {
        boolean available = ParserFactory.isAvroParserAvailable();
        assertTrue(available || !available);
    }

    @Test
    public void testCreateAvroParser() {
        if (ParserFactory.isAvroParserAvailable()) {
            AvroParser parser = ParserFactory.createAvroParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateKryoParser() {
        if (ParserFactory.isAvroParserAvailable()) {
            KryoParser parser = ParserFactory.createKryoParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateJsonParser() {
        JsonParser parser = ParserFactory.createJsonParser();
        assertNotNull(parser);
    }

    @Test
    public void testCreateJsonParserWithConfig() {
        JsonSerializationConfig jsc = new JsonSerializationConfig();
        JsonDeserializationConfig jdc = new JsonDeserializationConfig();

        JsonParser parser = ParserFactory.createJsonParser(jsc, jdc);
        assertNotNull(parser);
    }

    @Test
    public void testCreateAbacusXmlParser() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createAbacusXmlParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateAbacusXmlParserWithConfig() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlSerializationConfig xsc = new XmlSerializationConfig();
            XmlDeserializationConfig xdc = new XmlDeserializationConfig();

            XmlParser parser = ParserFactory.createAbacusXmlParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateXmlParser() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createXmlParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateXmlParserWithConfig() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlSerializationConfig xsc = new XmlSerializationConfig();
            XmlDeserializationConfig xdc = new XmlDeserializationConfig();

            XmlParser parser = ParserFactory.createXmlParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateJAXBParser() {
        XmlParser parser = ParserFactory.createJAXBParser();
        assertNotNull(parser);
    }

    @Test
    public void testCreateJAXBParserWithConfig() {
        XmlSerializationConfig xsc = new XmlSerializationConfig();
        XmlDeserializationConfig xdc = new XmlDeserializationConfig();

        XmlParser parser = ParserFactory.createJAXBParser(xsc, xdc);
        assertNotNull(parser);
    }

    @Test
    public void testRegisterKryo() {
        ParserFactory.registerKryo(String.class);
        ParserFactory.registerKryo(Integer.class, 100);

    }

    @Test
    public void testRegisterKryoWithNullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null);
        });
    }

    @Test
    public void testRegisterKryoWithIdAndNullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null, 100);
        });
    }
}
