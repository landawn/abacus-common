package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

public class AbstractJsonReaderTest extends TestBase {

    @Test
    public void test_nextToken_string() {
        String json = "{\"name\":\"John\"}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        int token = reader.nextToken();
        assertEquals(JsonReader.START_BRACE, token);

        reader.close();
    }

    @Test
    public void testNextTokenNoArgDelegatesToStringType() {
        String json = "{\"key\":\"value\"}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        // nextToken() with no args should work and return START_BRACE
        int token = reader.nextToken();
        assertEquals(JsonReader.START_BRACE, token);

        reader.close();
    }

    @Test
    public void testNextTokenNoArgWithNumber() {
        String json = "123";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        // Using no-arg nextToken (delegates to String type)
        int token = reader.nextToken();
        assertTrue(reader.hasText());
        assertEquals("123", reader.getText());

        reader.close();
    }

    @Test
    public void testNextTokenNoArgWithBoolean() {
        String json = "true";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        assertTrue(reader.hasText());
        assertEquals("true", reader.getText());

        reader.close();
    }

    @Test
    public void testNextTokenNoArgWithArray() {
        String json = "[1,2]";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        int token = reader.nextToken();
        assertEquals(JsonReader.START_BRACKET, token);

        reader.nextToken();
        assertEquals("1", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("2", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
        reader.close();
    }

    @Test
    public void testNextTokenNoArgViaStreamReader() {
        String json = "{\"a\":1}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[1024], new char[256]);

        // nextToken() no-arg should work for stream reader too
        int token = reader.nextToken();
        assertEquals(JsonReader.START_BRACE, token);

        reader.close();
    }

    @Test
    public void testReadValueWithObjectTypeForTrue() {
        String json = "true";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);
        reader.nextToken();

        Type<Object> objType = N.typeOf(Object.class);
        Object value = reader.readValue(objType);
        assertEquals(Boolean.TRUE, value);
    }

    @Test
    public void testReadValueWithObjectTypeForFalse() {
        String json = "false";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);
        reader.nextToken();

        Type<Object> objType = N.typeOf(Object.class);
        Object value = reader.readValue(objType);
        assertEquals(Boolean.FALSE, value);
    }

    @Test
    public void testHasTextFalseForStructuralToken() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken(); // START_BRACE
        assertFalse(reader.hasText());

        reader.close();
    }

    @Test
    public void testHasTextTrueForValue() {
        String json = "42";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        assertTrue(reader.hasText());

        reader.close();
    }

    @Test
    public void test_hasText() {
        String json = "\"test\"";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();

        reader.close();
        assertNotNull(reader);
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

    @Test
    public void testNextTokenNoArgWithNull() {
        String json = "null";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        assertTrue(reader.hasText());
        assertEquals("null", reader.getText());

        reader.close();
    }

    @Test
    public void testNextTokenNoArgWithEmptyObject() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals(JsonReader.EOF, reader.nextToken());

        reader.close();
    }

    @Test
    public void testNextTokenNoArgWithEmptyArray() {
        String json = "[]";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals(JsonReader.EOF, reader.nextToken());

        reader.close();
    }

    @Test
    public void testNextTokenWithTypeVsNoArg() {
        String json = "42";
        // With no-arg (String type)
        JsonReader reader1 = JsonStringReader.parse(json, new char[256]);
        reader1.nextToken();
        String text1 = reader1.getText();
        reader1.close();

        // With Integer type
        JsonReader reader2 = JsonStringReader.parse(json, new char[256]);
        reader2.nextToken(N.typeOf(Integer.class));
        String text2 = reader2.getText();
        reader2.close();

        // Both should produce same text
        assertEquals(text1, text2);
    }

    @Test
    public void testReadValueWithObjectTypeForNull() {
        String json = "null";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);
        reader.nextToken();

        Type<Object> objType = N.typeOf(Object.class);
        // null read as null for Object type returns null
        Object value = reader.readValue(objType);
        assertNull(value);
    }

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
}
