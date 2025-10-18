package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbacusXMLParserImpl2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        if (ParserFactory.isAbacusXMLAvailable()) {
            XMLParser parser = new AbacusXMLParserImpl(XMLParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_constructor_withConfig() {
        if (ParserFactory.isAbacusXMLAvailable()) {
            XMLSerializationConfig xsc = new XMLSerializationConfig();
            XMLDeserializationConfig xdc = new XMLDeserializationConfig();
            XMLParser parser = new AbacusXMLParserImpl(XMLParserType.StAX, xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_string() {
        if (ParserFactory.isAbacusXMLAvailable()) {
            XMLParser parser = new AbacusXMLParserImpl(XMLParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }
}
