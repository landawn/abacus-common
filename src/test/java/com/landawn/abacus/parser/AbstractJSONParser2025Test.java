package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbstractJSONParser2025Test extends TestBase {

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
        assertNotNull(result);
    }

    @Test
    public void test_serialize_integer() {
        JSONParser parser = new JSONParserImpl();
        String json = parser.serialize(123);
        assertNotNull(json);
    }

    @Test
    public void test_deserialize_integer() {
        JSONParser parser = new JSONParserImpl();
        Integer result = parser.deserialize("123", Integer.class);
        assertNotNull(result);
    }
}
