package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class JAXBParser2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        XmlParser parser = new JAXBParser();
        assertNotNull(parser);
    }

    @Test
    public void test_constructor_withConfig() {
        XmlSerializationConfig xsc = new XmlSerializationConfig();
        XmlDeserializationConfig xdc = new XmlDeserializationConfig();
        XmlParser parser = new JAXBParser(xsc, xdc);
        assertNotNull(parser);
    }
}
