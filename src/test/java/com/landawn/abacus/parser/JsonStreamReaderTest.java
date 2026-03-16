package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("2025")
public class JsonStreamReaderTest extends TestBase {

    private char[] rbuf = new char[1024];
    private char[] cbuf = new char[256];

    @Test
    public void test_parse() {
        String json = "{\"name\":\"John\"}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[1024], new char[256]);
        assertNotNull(reader);
        reader.close();
    }

    @Test
    public void test_nextToken() {
        String json = "{\"key\":\"value\"}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[1024], new char[256]);

        reader.nextToken();

        reader.close();
        assertNotNull(reader);
    }

    @Test
    public void test_hasText() {
        String json = "\"test\"";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[1024], new char[256]);

        reader.nextToken();
        reader.nextToken();
        reader.hasText();

        reader.close();
        assertNotNull(reader);
    }

    @Test
    public void test_getText() {
        String json = "\"hello\"";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[1024], new char[256]);

        reader.nextToken();
        reader.nextToken();
        reader.getText();

        reader.close();
        assertNotNull(reader);
    }

    @Test
    public void testParseFromReader() {
        String json = "{\"key\":\"value\"}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertNotNull(reader);
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
    }

    @Test
    public void testReadLargeJson() throws IOException {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0)
                sb.append(",");
            sb.append("{\"id\":").append(i).append(",\"name\":\"item").append(i).append("\"}");
        }
        sb.append("]");

        StringReader stringReader = new StringReader(sb.toString());
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[64], new char[64]);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("id", reader.getText());
        assertEquals(JsonReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("0", reader.getText());

        int objectCount = 1;
        int token;
        while ((token = reader.nextToken()) != JsonReader.EOF) {
            if (token == JsonReader.START_BRACE) {
                objectCount++;
            }
        }
        assertEquals(100, objectCount);
    }

    @Test
    public void testReadStreamedNumbers() throws IOException {
        String json = "[123456789012345, -987654321098765, 3.14159265358979, 2.71828182845905]";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        Type<Long> longType = N.typeOf(Long.class);
        assertEquals(123456789012345L, reader.readValue(longType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(-987654321098765L, reader.readValue(longType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        Type<Double> doubleType = N.typeOf(Double.class);
        assertEquals(3.14159265358979, reader.readValue(doubleType), 0.00000000000001);

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals(2.71828182845905, reader.readValue(doubleType), 0.00000000000001);

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadStreamedStrings() throws IOException {
        String json = "[\"first\", \"second with spaces\", \"third\\nwith\\nnewlines\"]";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        reader.nextToken();
        assertEquals("first", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("second with spaces", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("third\nwith\nnewlines", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
    }

    @Test
    public void testReadStreamedBooleans() throws IOException {
        String json = "[true, false, true, false]";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        Type<Boolean> boolType = N.typeOf(Boolean.class);

        reader.nextToken();
        assertEquals(true, reader.readValue(boolType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(false, reader.readValue(boolType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(true, reader.readValue(boolType));

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals(false, reader.readValue(boolType));

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadStreamedNull() throws IOException {
        String json = "[null, \"not null\", null]";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        Type<String> stringType = N.typeOf(String.class);

        reader.nextToken();
        assertNull(reader.readValue(stringType));

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        reader.nextToken();
        assertEquals("not null", reader.readValue(stringType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        reader.nextToken();
        assertNull(reader.readValue(stringType));

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadVeryLongString() throws IOException {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < 500; i++) {
            sb.append("abcdefghij");
        }
        sb.append("\"");

        StringReader stringReader = new StringReader(sb.toString());
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[128], new char[256]);

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(5000, reader.getText().length());
    }

    @Test
    public void testReadUnicodeEscapes() throws IOException {
        String json = "\"\\u0048\\u0065\\u006C\\u006C\\u006F \\u4E16\\u754C\"";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("Hello 世界", reader.getText());
    }

    @Test
    public void testReadStreamWithSmallBuffer() throws IOException {
        String json = "{\"a\":1,\"b\":2,\"c\":3}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, new char[4], new char[4]);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("a", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("1", reader.getText());

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        reader.nextToken();
        assertEquals("b", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("2", reader.getText());

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        reader.nextToken();
        assertEquals("c", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("3", reader.getText());

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadEmptyStream() throws IOException {
        StringReader stringReader = new StringReader("");
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testCloseReader() throws IOException {
        StringReader stringReader = new StringReader("{}");
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        reader.nextToken();
        assertDoesNotThrow(() -> reader.close());
    }

    @Test
    public void testReadLongNumbersWithTypeSuffix() throws IOException {
        String json = "[123L, 45.6f, 78.9d]";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        Type<Long> longType = N.typeOf(Long.class);
        assertEquals(123L, reader.readValue(longType));

        assertEquals(JsonReader.COMMA, reader.nextToken());
        Type<Float> floatType = N.typeOf(Float.class);
        assertEquals(45.6f, reader.readValue(floatType), 0.001f);

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        Type<Double> doubleType = N.typeOf(Double.class);
        assertEquals(78.9d, reader.readValue(doubleType), 0.001);

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadNegativeNumbers() throws IOException {
        String json = "[-42, -3.14]";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        Type<Integer> intType = N.typeOf(Integer.class);
        reader.nextToken(intType);
        assertEquals(-42, reader.readValue(intType));

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        Type<Double> doubleType = N.typeOf(Double.class);
        assertEquals(-3.14, reader.readValue(doubleType), 0.001);

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadSingleQuotedStrings() throws IOException {
        String json = "{'key':'value'}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_SINGLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_SINGLE_QUOTE, reader.nextToken());
        assertEquals("key", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_SINGLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_SINGLE_QUOTE, reader.nextToken());
        assertEquals("value", reader.getText());

        assertEquals(JsonReader.END_BRACE, reader.nextToken());
    }

    @Test
    public void testReadEscapedStrings() throws IOException {
        String json = "[\"line1\\nline2\", \"tab\\there\"]";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        reader.nextToken();
        assertEquals("line1\nline2", reader.getText());

        assertEquals(JsonReader.COMMA, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("tab\there", reader.getText());

        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
    }

    @Test
    public void testReadNestedObject() throws IOException {
        String json = "{\"a\":{\"b\":1}}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("a", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("b", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("1", reader.getText());

        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals(JsonReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadValueAsObject() throws IOException {
        String json = "123";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        reader.nextToken();
        Type<Object> objType = N.typeOf(Object.class);
        Object value = reader.readValue(objType);
        assertEquals(123, value);
    }

    @Test
    public void testLastToken() throws IOException {
        String json = "{\"a\":1}";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(-1, reader.lastToken());

        reader.nextToken(); // START_BRACE
        assertEquals(-1, reader.lastToken());

        reader.nextToken(); // START_DOUBLE_QUOTE
        assertEquals(JsonReader.START_BRACE, reader.lastToken());
    }

    @Test
    public void testHasText() throws IOException {
        String json = "42";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        reader.nextToken();
        assertTrue(reader.hasText());
    }

    @Test
    public void testReadWhitespaceOnly() throws IOException {
        String json = "   ";
        StringReader stringReader = new StringReader(json);
        JsonReader reader = JsonStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JsonReader.EOF, reader.nextToken());
    }

}
