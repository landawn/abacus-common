package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbstractParser2025Test extends TestBase {

    @Test
    public void test_JSONParser_creation() {
        JSONParser parser = new JSONParserImpl();
        assertNotNull(parser);
    }

    @Test
    public void test_serialize_deserialize_basic() {
        JSONParser parser = new JSONParserImpl();
        String data = "test";
        String json = parser.serialize(data);
        assertNotNull(json);

        String result = parser.deserialize(json, String.class);
        assertNotNull(result);
    }
}
