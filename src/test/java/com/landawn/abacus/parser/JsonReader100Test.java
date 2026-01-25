package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class JsonReader100Test extends TestBase {

    private JsonReader reader;
    private char[] cbuf;

    @BeforeEach
    public void setUp() {
        cbuf = new char[256];
    }

    @Test
    public void testNextTokenWithObject() {
        String json = "{\"name\":\"John\",\"age\":30}";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("name", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("John", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("age", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals("30", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testNextTokenWithArray() {
        String json = "[1,2,3]";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("1", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("2", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("3", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testHasText() {
        String json = "{\"key\":\"value\"}";
        reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();
        assertFalse(reader.hasText());

        reader.nextToken();
        reader.nextToken();
        assertTrue(reader.hasText());
        assertEquals("key", reader.getText());

        reader.nextToken();
        assertFalse(reader.hasText());

        reader.nextToken();
        reader.nextToken();
        assertTrue(reader.hasText());
        assertEquals("value", reader.getText());
    }

    @Test
    public void testGetText() {
        String json = "{\"message\":\"Hello World\"}";
        reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();
        reader.nextToken();
        reader.nextToken();
        assertEquals("message", reader.getText());

        reader.nextToken();
        reader.nextToken();
        reader.nextToken();
        assertEquals("Hello World", reader.getText());
    }

    @Test
    public void testReadValueString() {
        String json = "\"test string\"";
        reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();
        reader.nextToken();

        Type<String> stringType = N.typeOf(String.class);
        String value = reader.readValue(stringType);
        assertEquals("test string", value);
    }

    @Test
    public void testReadValueNumber() {
        String json = "123";
        reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();

        Type<Integer> intType = N.typeOf(Integer.class);
        Integer value = reader.readValue(intType);
        assertEquals(123, value);
    }

    @Test
    public void testReadValueBoolean() {
        String json = "true";
        reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();

        Type<Boolean> boolType = N.typeOf(Boolean.class);
        Boolean value = reader.readValue(boolType);
        assertEquals(true, value);
    }

    @Test
    public void testReadValueNull() {
        String json = "null";
        reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();

        Type<String> stringType = N.typeOf(String.class);
        String value = reader.readValue(stringType);
        assertNull(value);
    }

    @Test
    public void testSpecialCharacters() {
        String json = "{\"key\":\"value\\nwith\\nnewlines\"}";
        reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();
        reader.nextToken();
        reader.nextToken();
        assertEquals("key", reader.getText());

        reader.nextToken();
        reader.nextToken();
        reader.nextToken();
        assertEquals("value\nwith\nnewlines", reader.getText());
    }

    @Test
    public void testEmptyObject() {
        String json = "{}";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testEmptyArray() {
        String json = "[]";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testNestedStructure() {
        String json = "{\"outer\":{\"inner\":\"value\"}}";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("outer", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("inner", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("value", reader.getText());

        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());
    }

    @Test
    public void testArrayOfObjects() {
        String json = "[{\"id\":1},{\"id\":2}]";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("id", reader.getText());
        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals("1", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("id", reader.getText());
        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals("2", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
    }

    @Test
    public void testSingleQuotes() {
        String json = "{'key':'value'}";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_S, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_S, reader.nextToken());
        assertEquals("key", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_S, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_S, reader.nextToken());
        assertEquals("value", reader.getText());

        assertEquals(JsonReader.END_BRACE, reader.nextToken());
    }

    @Test
    public void testFloatingPointNumbers() {
        String json = "[3.14, -2.5, 1.23e10, 4.56E-7]";
        reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("3.14", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("-2.5", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("1.23e10", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("4.56E-7", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testClose() {
        String json = "{}";
        reader = JsonStringReader.parse(json, cbuf);

        assertDoesNotThrow(() -> reader.close());
    }
}
