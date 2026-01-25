package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ParserFactory2025Test extends TestBase {

    @Test
    public void test_isAbacusXmlParserAvailable() {
        // Just verify the method can be called
        ParserFactory.isAbacusXmlParserAvailable();
    }

    @Test
    public void test_isXmlParserAvailable() {
        ParserFactory.isXmlParserAvailable();
    }

    @Test
    public void test_isAvroParserAvailable() {
        ParserFactory.isAvroParserAvailable();
    }

    @Test
    public void test_createJsonParser() {
        JsonParser parser = ParserFactory.createJsonParser();
        assertNotNull(parser);
    }

    @Test
    public void test_createJsonParser_withConfig() {
        JsonSerializationConfig jsc = new JsonSerializationConfig();
        JsonDeserializationConfig jdc = new JsonDeserializationConfig();
        JsonParser parser = ParserFactory.createJsonParser(jsc, jdc);
        assertNotNull(parser);
    }

    @Test
    public void test_createAvroParser() {
        if (ParserFactory.isAvroParserAvailable()) {
            AvroParser parser = ParserFactory.createAvroParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createKryoParser() {
        if (ParserFactory.isAvroParserAvailable()) {
            KryoParser parser = ParserFactory.createKryoParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createAbacusXmlParser() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createAbacusXmlParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createAbacusXmlParser_withConfig() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlSerializationConfig xsc = new XmlSerializationConfig();
            XmlDeserializationConfig xdc = new XmlDeserializationConfig();
            XmlParser parser = ParserFactory.createAbacusXmlParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createXmlParser() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createXmlParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createXmlParser_withConfig() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlSerializationConfig xsc = new XmlSerializationConfig();
            XmlDeserializationConfig xdc = new XmlDeserializationConfig();
            XmlParser parser = ParserFactory.createXmlParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_createJAXBParser() {
        XmlParser parser = ParserFactory.createJAXBParser();
        assertNotNull(parser);
    }

    @Test
    public void test_createJAXBParser_withConfig() {
        XmlSerializationConfig xsc = new XmlSerializationConfig();
        XmlDeserializationConfig xdc = new XmlDeserializationConfig();
        XmlParser parser = ParserFactory.createJAXBParser(xsc, xdc);
        assertNotNull(parser);
    }

    @Test
    public void test_registerKryo_class() {
        if (ParserFactory.isAvroParserAvailable()) {
            ParserFactory.registerKryo(String.class);
        }
    }

    @Test
    public void test_registerKryo_class_null() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null));
    }

    @Test
    public void test_registerKryo_classWithId() {
        if (ParserFactory.isAvroParserAvailable()) {
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
