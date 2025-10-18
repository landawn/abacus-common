package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class JAXBParser2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        XMLParser parser = new JAXBParser();
        assertNotNull(parser);
    }

    @Test
    public void test_constructor_withConfig() {
        XMLSerializationConfig xsc = new XMLSerializationConfig();
        XMLDeserializationConfig xdc = new XMLDeserializationConfig();
        XMLParser parser = new JAXBParser(xsc, xdc);
        assertNotNull(parser);
    }
}
