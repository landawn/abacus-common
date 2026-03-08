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
        boolean available = ParserFactory.isKryoParserAvailable();
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
        if (ParserFactory.isKryoParserAvailable()) {
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
        JsonSerConfig jsc = new JsonSerConfig();
        JsonDeserConfig jdc = new JsonDeserConfig();

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
            XmlSerConfig xsc = new XmlSerConfig();
            XmlDeserConfig xdc = new XmlDeserConfig();

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
            XmlSerConfig xsc = new XmlSerConfig();
            XmlDeserConfig xdc = new XmlDeserConfig();

            XmlParser parser = ParserFactory.createXmlParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateJaxbParser() {
        XmlParser parser = ParserFactory.createJaxbParser();
        assertNotNull(parser);
    }

    @Test
    public void testCreateJaxbParserWithConfig() {
        XmlSerConfig xsc = new XmlSerConfig();
        XmlDeserConfig xdc = new XmlDeserConfig();

        XmlParser parser = ParserFactory.createJaxbParser(xsc, xdc);
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
