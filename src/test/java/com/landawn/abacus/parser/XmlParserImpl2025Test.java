package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class XmlParserImpl2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_constructor_withConfig() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlSerializationConfig xsc = new XmlSerializationConfig();
            XmlDeserializationConfig xdc = new XmlDeserializationConfig();
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX, xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_string() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }
}
