package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbstractXmlParser2025Test extends TestBase {

    @Test
    public void test_XmlParser_creation() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_basic() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = new XmlParserImpl(XmlParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }
}
