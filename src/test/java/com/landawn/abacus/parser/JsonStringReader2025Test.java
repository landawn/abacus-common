package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("2025")
public class JsonStringReader2025Test extends TestBase {

    @Test
    public void test_parse_string() {
        String json = "{\"name\":\"John\"}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);
        assertNotNull(reader);
        reader.close();
    }

    @Test
    public void test_parse_stringWithRange() {
        String json = "prefix{\"name\":\"John\"}suffix";
        JsonReader reader = JsonStringReader.parse(json, 6, 20, new char[256]);
        assertNotNull(reader);
        reader.close();
    }

    @Test
    public void test_nextToken() {
        String json = "{\"key\":\"value\"}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        int token = reader.nextToken();
        assertEquals(JsonReader.START_BRACE, token);

        reader.close();
    }

    @Test
    public void test_nextToken_withType() {
        String json = "123";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        Type<Integer> intType = Type.of(Integer.class);
        int token = reader.nextToken(intType);

        reader.close();
    }

    @Test
    public void test_lastToken() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        int lastToken = reader.lastToken();

        reader.close();
    }

    @Test
    public void test_hasText_true() {
        String json = "\"test\"";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();
        boolean hasText = reader.hasText();
        assertTrue(hasText);

        reader.close();
    }

    @Test
    public void test_hasText_false() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        boolean hasText = reader.hasText();
        assertFalse(hasText);

        reader.close();
    }

    @Test
    public void test_getText() {
        String json = "\"hello\"";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();
        String text = reader.getText();
        assertEquals("hello", text);

        reader.close();
    }

    @Test
    public void test_readValue() {
        String json = "123";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        Type<Integer> intType = Type.of(Integer.class);
        Integer value = reader.readValue(intType);
        assertEquals(123, value);

        reader.close();
    }

    @Test
    public void test_close() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);
        reader.close();
    }
}
