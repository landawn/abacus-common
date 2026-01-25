package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbacusXmlParserImpl2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_constructor_withConfig() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlSerializationConfig xsc = new XmlSerializationConfig();
            XmlDeserializationConfig xdc = new XmlDeserializationConfig();
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX, xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_string() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }
}
