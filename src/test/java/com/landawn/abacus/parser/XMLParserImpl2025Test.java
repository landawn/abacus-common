package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class XMLParserImpl2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        if (ParserFactory.isXMLAvailable()) {
            XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_constructor_withConfig() {
        if (ParserFactory.isXMLAvailable()) {
            XMLSerializationConfig xsc = new XMLSerializationConfig();
            XMLDeserializationConfig xdc = new XMLDeserializationConfig();
            XMLParser parser = new XMLParserImpl(XMLParserType.StAX, xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_string() {
        if (ParserFactory.isXMLAvailable()) {
            XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }
}
