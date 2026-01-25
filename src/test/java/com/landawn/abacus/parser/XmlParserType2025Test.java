package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class XmlParserType2025Test extends TestBase {

    @Test
    public void test_values() {
        XmlParserType[] values = XmlParserType.values();
        assertNotNull(values);
        assertEquals(3, values.length);
    }

    @Test
    public void test_valueOf_SAX() {
        XmlParserType type = XmlParserType.valueOf("SAX");
        assertEquals(XmlParserType.SAX, type);
    }

    @Test
    public void test_valueOf_DOM() {
        XmlParserType type = XmlParserType.valueOf("DOM");
        assertEquals(XmlParserType.DOM, type);
    }

    @Test
    public void test_valueOf_StAX() {
        XmlParserType type = XmlParserType.valueOf("StAX");
        assertEquals(XmlParserType.StAX, type);
    }

    @Test
    public void test_SAX_value() {
        assertEquals("SAX", XmlParserType.SAX.toString());
        assertEquals(XmlParserType.SAX, XmlParserType.values()[0]);
    }

    @Test
    public void test_DOM_value() {
        assertEquals("DOM", XmlParserType.DOM.toString());
        assertEquals(XmlParserType.DOM, XmlParserType.values()[1]);
    }

    @Test
    public void test_StAX_value() {
        assertEquals("StAX", XmlParserType.StAX.toString());
        assertEquals(XmlParserType.StAX, XmlParserType.values()[2]);
    }

    @Test
    public void test_ordinal_SAX() {
        assertEquals(0, XmlParserType.SAX.ordinal());
    }

    @Test
    public void test_ordinal_DOM() {
        assertEquals(1, XmlParserType.DOM.ordinal());
    }

    @Test
    public void test_ordinal_StAX() {
        assertEquals(2, XmlParserType.StAX.ordinal());
    }

    @Test
    public void test_name_SAX() {
        assertEquals("SAX", XmlParserType.SAX.name());
    }

    @Test
    public void test_name_DOM() {
        assertEquals("DOM", XmlParserType.DOM.name());
    }

    @Test
    public void test_name_StAX() {
        assertEquals("StAX", XmlParserType.StAX.name());
    }
}
