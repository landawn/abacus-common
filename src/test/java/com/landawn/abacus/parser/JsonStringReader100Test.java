package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class JsonStringReader100Test extends TestBase {

    private char[] cbuf;

    @BeforeEach
    public void setUp() {
        cbuf = new char[256];
    }

    @Test
    public void testParseString() {
        String json = "{\"key\":\"value\"}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertNotNull(reader);
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
    }

    @Test
    public void testParseSubstring() {
        String json = "prefix{\"key\":\"value\"}suffix";
        JsonReader reader = JsonStringReader.parse(json, 6, json.length() - 6, cbuf);

        assertNotNull(reader);
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
    }

    @Test
    public void testReadSimpleObject() {
        String json = "{\"name\":\"John\"}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("name", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("John", reader.getText());

        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadNumbers() {
        String json = "[123, -456, 78.9, 1.23e10, -4.56E-7]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        assertEquals("123", reader.getText());
        Type<Integer> intType = N.typeOf(Integer.class);
        assertEquals(123, reader.readValue(intType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("-456", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("78.9", reader.getText());
        Type<Double> doubleType = N.typeOf(Double.class);
        assertEquals(78.9, reader.readValue(doubleType), 0.001);

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("1.23e10", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("-4.56E-7", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadBooleans() {
        String json = "[true, false]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        assertEquals("true", reader.getText());
        Type<Boolean> boolType = N.typeOf(Boolean.class);
        assertEquals(true, reader.readValue(boolType));

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("false", reader.getText());
        assertEquals(false, reader.readValue(boolType));

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadNull() {
        String json = "null";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();
        assertEquals("null", reader.getText());
        Type<String> stringType = N.typeOf(String.class);
        assertNull(reader.readValue(stringType));
    }

    @Test
    public void testReadEscapedCharacters() {
        String json = "\"\\n\\r\\t\\b\\f\\\\\\/\\\"\"";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("\n\r\t\b\f\\/\"", reader.getText());
    }

    @Test
    public void testReadUnicodeEscape() {
        String json = "\"\\u0048\\u0065\\u006C\\u006C\\u006F\"";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("Hello", reader.getText());
    }

    @Test
    public void testReadEmptyString() {
        String json = "\"\"";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("", reader.getText());
    }

    @Test
    public void testReadLongNumbers() {
        String json = "[123L, 456l, 78.9f, 12.34F, 56.78d, 90.12D]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        Type<Long> longType = N.typeOf(Long.class);
        assertEquals(123L, reader.readValue(longType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(456L, reader.readValue(longType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        Type<Float> floatType = N.typeOf(Float.class);
        assertEquals(78.9f, reader.readValue(floatType), 0.001f);

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(12.34f, reader.readValue(floatType), 0.001f);

        assertEquals(JsonReader.COMMA, reader.nextToken());
        Type<Double> doubleType = N.typeOf(Double.class);
        assertEquals(56.78d, reader.readValue(doubleType), 0.001);

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals(90.12d, reader.readValue(doubleType), 0.001);

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadSingleQuotedString() {
        String json = "{'key':'value'}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

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
    public void testReadWhitespace() {
        String json = "  {  \"key\"  :  \"value\"  }  ";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("key", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("value", reader.getText());

        assertEquals(JsonReader.END_BRACE, reader.nextToken());
    }

    @Test
    public void testReadArrayWithWhitespace() {
        String json = "[ 1 , 2 , 3 ]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals("1", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("2", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("3", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadNestedObjects() {
        String json = "{\"outer\":{\"inner\":123}}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

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
        assertEquals("123", reader.getText());

        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadMixedArray() {
        String json = "[\"string\", 123, true, null, 45.67]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        reader.nextToken();
        assertEquals("string", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        reader.nextToken();
        assertEquals("123", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("true", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("null", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("45.67", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadVeryLongString() {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < 1000; i++) {
            sb.append("a");
        }
        sb.append("\"");

        JsonReader reader = JsonStringReader.parse(sb.toString(), new char[512]);

        assertEquals(JsonReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JsonReader.END_QUOTATION_D, reader.nextToken());
        assertEquals(1000, reader.getText().length());
    }

    @Test
    public void testReadSpecialNumbers() {
        String json = "[0, -0, 0.0, -0.0]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        assertEquals("0", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("-0", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("0.0", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("-0.0", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testEmptyInput() {
        String json = "";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.EOF, reader.nextToken());
        assertFalse(reader.hasText());
    }

    @Test
    public void testReadValueAsObject() {
        String json = "123";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();
        Type<Object> objType = N.typeOf(Object.class);
        Object value = reader.readValue(objType);
        assertTrue(value instanceof Integer);
        assertEquals(123, value);
    }

    @Test
    public void testReadDecimalWithLeadingDot() {
        String json = "[.5, -.5]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        assertEquals(".5", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("-.5", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testMultipleConsecutiveCommas() {
        String json = "[1,,3]";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals("1", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("3", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }
}
