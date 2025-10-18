package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbstractXMLParser2025Test extends TestBase {

    @Test
    public void test_XMLParser_creation() {
        if (ParserFactory.isXMLAvailable()) {
            XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
            assertNotNull(parser);
        }
    }

    @Test
    public void test_serialize_basic() {
        if (ParserFactory.isXMLAvailable()) {
            XMLParser parser = new XMLParserImpl(XMLParserType.StAX);
            String xml = parser.serialize("test");
            assertNotNull(xml);
        }
    }
}
