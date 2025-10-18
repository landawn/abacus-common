package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ParserFactory2025Test extends TestBase {

    @Test
    public void test_isAbacusXMLAvailable() {
        // Just verify the method can be called
        ParserFactory.isAbacusXMLAvailable();
    }

    @Test
    public void test_isXMLAvailable() {
        ParserFactory.isXMLAvailable();
    }

    @Test
    public void test_isAvroAvailable() {
        ParserFactory.isAvroAvailable();
    }

    @Test
    public void test_isKryoAvailable() {
        ParserFactory.isKryoAvailable();
    }

    @Test
    public void test_createJSONParser() {
        JSONParser parser = ParserFactory.createJSONParser();
        assertNotNull(parser);
    }

    @Test
    public void test_createJSONParser_withConfig() {
        JSONSerializationConfig jsc = new JSONSerializationConfig();
        JSONDeserializationConfig jdc = new JSONDeserializationConfig();
        JSONParser parser = ParserFactory.createJSONParser(jsc, jdc);
        assertNotNull(parser);
    }

    @Test
    public void test_createAvroParser() {
        if (ParserFactory.isAvroAvailable()) {
            AvroParser parser = ParserFactory.createAvroParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createKryoParser() {
        if (ParserFactory.isKryoAvailable()) {
            KryoParser parser = ParserFactory.createKryoParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createAbacusXMLParser() {
        if (ParserFactory.isAbacusXMLAvailable()) {
            XMLParser parser = ParserFactory.createAbacusXMLParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createAbacusXMLParser_withConfig() {
        if (ParserFactory.isAbacusXMLAvailable()) {
            XMLSerializationConfig xsc = new XMLSerializationConfig();
            XMLDeserializationConfig xdc = new XMLDeserializationConfig();
            XMLParser parser = ParserFactory.createAbacusXMLParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createXMLParser() {
        if (ParserFactory.isXMLAvailable()) {
            XMLParser parser = ParserFactory.createXMLParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createXMLParser_withConfig() {
        if (ParserFactory.isXMLAvailable()) {
            XMLSerializationConfig xsc = new XMLSerializationConfig();
            XMLDeserializationConfig xdc = new XMLDeserializationConfig();
            XMLParser parser = ParserFactory.createXMLParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createJAXBParser() {
        XMLParser parser = ParserFactory.createJAXBParser();
        assertNotNull(parser);
    }

    @Test
    public void test_createJAXBParser_withConfig() {
        XMLSerializationConfig xsc = new XMLSerializationConfig();
        XMLDeserializationConfig xdc = new XMLDeserializationConfig();
        XMLParser parser = ParserFactory.createJAXBParser(xsc, xdc);
        assertNotNull(parser);
    }

    @Test
    public void test_registerKryo_class() {
        if (ParserFactory.isKryoAvailable()) {
            ParserFactory.registerKryo(String.class);
        }
    }

    @Test
    public void test_registerKryo_class_null() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null));
    }

    @Test
    public void test_registerKryo_classWithId() {
        if (ParserFactory.isKryoAvailable()) {
            ParserFactory.registerKryo(Integer.class, 100);
        }
    }

    @Test
    public void test_registerKryo_classWithId_null() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null, 100));
    }

    @Test
    public void test_registerKryo_classWithSerializerAndId_nullSerializer() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(String.class, null, 200));
    }
}
