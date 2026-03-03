package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbstractJsonReader2025Test extends TestBase {

    @Test
    public void test_constants() {
        assertEquals(-1, JsonReader.EOF);
        assertEquals(0, JsonReader.UNDEFINED);
        assertEquals(1, JsonReader.START_BRACE);
        assertEquals(2, JsonReader.END_BRACE);
        assertEquals(3, JsonReader.START_BRACKET);
        assertEquals(4, JsonReader.END_BRACKET);
        assertEquals(5, JsonReader.START_DOUBLE_QUOTE);
        assertEquals(6, JsonReader.END_DOUBLE_QUOTE);
        assertEquals(7, JsonReader.START_SINGLE_QUOTE);
        assertEquals(8, JsonReader.END_SINGLE_QUOTE);
        assertEquals(9, JsonReader.COLON);
        assertEquals(10, JsonReader.COMMA);
    }

    @Test
    public void test_nextToken_string() {
        String json = "{\"name\":\"John\"}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        int token = reader.nextToken();
        assertEquals(JsonReader.START_BRACE, token);

        reader.close();
    }

    @Test
    public void test_hasText() {
        String json = "\"test\"";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();

        reader.close();
    }

    @Test
    public void test_getText() {
        String json = "\"test\"";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();
        String text = reader.getText();
        assertNotNull(text);

        reader.close();
    }
}
