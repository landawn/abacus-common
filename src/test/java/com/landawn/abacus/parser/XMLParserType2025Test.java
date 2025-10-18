package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class XMLParserType2025Test extends TestBase {

    @Test
    public void test_values() {
        XMLParserType[] values = XMLParserType.values();
        assertNotNull(values);
        assertEquals(3, values.length);
    }

    @Test
    public void test_valueOf_SAX() {
        XMLParserType type = XMLParserType.valueOf("SAX");
        assertEquals(XMLParserType.SAX, type);
    }

    @Test
    public void test_valueOf_DOM() {
        XMLParserType type = XMLParserType.valueOf("DOM");
        assertEquals(XMLParserType.DOM, type);
    }

    @Test
    public void test_valueOf_StAX() {
        XMLParserType type = XMLParserType.valueOf("StAX");
        assertEquals(XMLParserType.StAX, type);
    }

    @Test
    public void test_SAX_value() {
        assertEquals("SAX", XMLParserType.SAX.toString());
        assertEquals(XMLParserType.SAX, XMLParserType.values()[0]);
    }

    @Test
    public void test_DOM_value() {
        assertEquals("DOM", XMLParserType.DOM.toString());
        assertEquals(XMLParserType.DOM, XMLParserType.values()[1]);
    }

    @Test
    public void test_StAX_value() {
        assertEquals("StAX", XMLParserType.StAX.toString());
        assertEquals(XMLParserType.StAX, XMLParserType.values()[2]);
    }

    @Test
    public void test_ordinal_SAX() {
        assertEquals(0, XMLParserType.SAX.ordinal());
    }

    @Test
    public void test_ordinal_DOM() {
        assertEquals(1, XMLParserType.DOM.ordinal());
    }

    @Test
    public void test_ordinal_StAX() {
        assertEquals(2, XMLParserType.StAX.ordinal());
    }

    @Test
    public void test_name_SAX() {
        assertEquals("SAX", XMLParserType.SAX.name());
    }

    @Test
    public void test_name_DOM() {
        assertEquals("DOM", XMLParserType.DOM.name());
    }

    @Test
    public void test_name_StAX() {
        assertEquals("StAX", XMLParserType.StAX.name());
    }
}
