package com.landawn.abacus.parser;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class JSONParserImpl2025Test extends TestBase {

    @Test
    public void test_constructor_default() {
        JSONParser parser = new JSONParserImpl();
        assertNotNull(parser);
    }

    @Test
    public void test_constructor_withConfig() {
        JSONSerializationConfig jsc = new JSONSerializationConfig();
        JSONDeserializationConfig jdc = new JSONDeserializationConfig();
        JSONParser parser = new JSONParserImpl(jsc, jdc);
        assertNotNull(parser);
    }

    @Test
    public void test_serialize_string() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize("test");
        assertNotNull(json);
    }

    @Test
    public void test_deserialize_string() {
        JSONParser parser = new JSONParserImpl();
        String result = parser.deserialize("\"test\"", String.class);
        assertEquals("\"test\"", result);
    }

    @Test
    public void test_serialize_integer() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize(123);
        assertEquals("123", json);
    }

    @Test
    public void test_deserialize_integer() {
        JSONParser parser = new JSONParserImpl();
        Integer result = parser.deserialize("123", Integer.class);
        assertEquals(123, result);
    }

    @Test
    public void test_serialize_boolean() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize(true);
        assertEquals("true", json);
    }

    @Test
    public void test_deserialize_boolean() {
        JSONParser parser = new JSONParserImpl();
        Boolean result = parser.deserialize("true", Boolean.class);
        assertEquals(true, result);
    }

    @Test
    public void test_serialize_null() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize(null);
        assertNull(json);
    }
}
