package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class AbstractJSONReader2025Test extends TestBase {

    @Test
    public void test_constants() {
        assertEquals(-1, JSONReader.EOF);
        assertEquals(0, JSONReader.UNDEFINED);
        assertEquals(1, JSONReader.START_BRACE);
        assertEquals(2, JSONReader.END_BRACE);
        assertEquals(3, JSONReader.START_BRACKET);
        assertEquals(4, JSONReader.END_BRACKET);
        assertEquals(5, JSONReader.START_QUOTATION_D);
        assertEquals(6, JSONReader.END_QUOTATION_D);
        assertEquals(7, JSONReader.START_QUOTATION_S);
        assertEquals(8, JSONReader.END_QUOTATION_S);
        assertEquals(9, JSONReader.COLON);
        assertEquals(10, JSONReader.COMMA);
    }

    @Test
    public void test_nextToken_string() {
        String json = "{\"name\":\"John\"}";
        JSONReader reader = JSONStringReader.parse(json, new char[256]);

        int token = reader.nextToken();
        assertEquals(JSONReader.START_BRACE, token);

        reader.close();
    }

    @Test
    public void test_hasText() {
        String json = "\"test\"";
        JSONReader reader = JSONStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();

        reader.close();
    }

    @Test
    public void test_getText() {
        String json = "\"test\"";
        JSONReader reader = JSONStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();
        String text = reader.getText();
        assertNotNull(text);

        reader.close();
    }
}
