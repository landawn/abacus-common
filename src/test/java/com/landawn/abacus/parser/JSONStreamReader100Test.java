package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class JSONStreamReader100Test extends TestBase {

    private char[] rbuf;
    private char[] cbuf;

    @BeforeEach
    public void setUp() {
        rbuf = new char[256];
        cbuf = new char[256];
    }

    @Test
    public void testParseFromReader() {
        String json = "{\"key\":\"value\"}";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertNotNull(reader);
        assertEquals(JSONReader.START_BRACE, reader.nextToken());
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
        JSONReader reader = JSONStreamReader.parse(stringReader, new char[64], new char[64]);

        assertEquals(JSONReader.START_BRACKET, reader.nextToken());

        assertEquals(JSONReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("id", reader.getText());
        assertEquals(JSONReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("0", reader.getText());

        int objectCount = 1;
        int token;
        while ((token = reader.nextToken()) != JSONReader.EOF) {
            if (token == JSONReader.START_BRACE) {
                objectCount++;
            }
        }
        assertEquals(100, objectCount);
    }

    @Test
    public void testReadStreamedNumbers() throws IOException {
        String json = "[123456789012345, -987654321098765, 3.14159265358979, 2.71828182845905]";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JSONReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        Type<Long> longType = N.typeOf(Long.class);
        assertEquals(123456789012345L, reader.readValue(longType));

        assertEquals(JSONReader.COMMA, reader.nextToken());
        assertEquals(-987654321098765L, reader.readValue(longType));

        assertEquals(JSONReader.COMMA, reader.nextToken());
        Type<Double> doubleType = N.typeOf(Double.class);
        assertEquals(3.14159265358979, reader.readValue(doubleType), 0.00000000000001);

        assertEquals(JSONReader.END_BRACKET, reader.nextToken());
        assertEquals(2.71828182845905, reader.readValue(doubleType), 0.00000000000001);

        assertEquals(JSONReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadStreamedStrings() throws IOException {
        String json = "[\"first\", \"second with spaces\", \"third\\nwith\\nnewlines\"]";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JSONReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        reader.nextToken();
        assertEquals("first", reader.getText());

        assertEquals(JSONReader.COMMA, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("second with spaces", reader.getText());

        assertEquals(JSONReader.COMMA, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("third\nwith\nnewlines", reader.getText());

        assertEquals(JSONReader.END_BRACKET, reader.nextToken());
    }

    @Test
    public void testReadStreamedBooleans() throws IOException {
        String json = "[true, false, true, false]";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JSONReader.START_BRACKET, reader.nextToken());

        Type<Boolean> boolType = N.typeOf(Boolean.class);

        reader.nextToken();
        assertEquals(true, reader.readValue(boolType));

        assertEquals(JSONReader.COMMA, reader.nextToken());
        assertEquals(false, reader.readValue(boolType));

        assertEquals(JSONReader.COMMA, reader.nextToken());
        assertEquals(true, reader.readValue(boolType));

        assertEquals(JSONReader.END_BRACKET, reader.nextToken());
        assertEquals(false, reader.readValue(boolType));

        assertEquals(JSONReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadStreamedNull() throws IOException {
        String json = "[null, \"not null\", null]";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JSONReader.START_BRACKET, reader.nextToken());

        Type<String> stringType = N.typeOf(String.class);

        reader.nextToken();
        assertNull(reader.readValue(stringType));

        assertEquals(JSONReader.START_QUOTATION_D, reader.nextToken());
        reader.nextToken();
        assertEquals("not null", reader.readValue(stringType));

        assertEquals(JSONReader.COMMA, reader.nextToken());
        reader.nextToken();
        assertNull(reader.readValue(stringType));

        assertEquals(JSONReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadVeryLongString() throws IOException {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < 500; i++) {
            sb.append("abcdefghij");
        }
        sb.append("\"");

        StringReader stringReader = new StringReader(sb.toString());
        JSONReader reader = JSONStreamReader.parse(stringReader, new char[128], new char[256]);

        assertEquals(JSONReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JSONReader.END_QUOTATION_D, reader.nextToken());
        assertEquals(5000, reader.getText().length());
    }

    @Test
    public void testReadUnicodeEscapes() throws IOException {
        String json = "\"\\u0048\\u0065\\u006C\\u006C\\u006F \\u4E16\\u754C\"";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JSONReader.START_QUOTATION_D, reader.nextToken());
        assertEquals(JSONReader.END_QUOTATION_D, reader.nextToken());
        assertEquals("Hello 世界", reader.getText());
    }

    @Test
    public void testReadStreamWithSmallBuffer() throws IOException {
        String json = "{\"a\":1,\"b\":2,\"c\":3}";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, new char[4], new char[4]);

        assertEquals(JSONReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("a", reader.getText());

        assertEquals(JSONReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("1", reader.getText());

        assertEquals(JSONReader.START_QUOTATION_D, reader.nextToken());
        reader.nextToken();
        assertEquals("b", reader.getText());

        assertEquals(JSONReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("2", reader.getText());

        assertEquals(JSONReader.START_QUOTATION_D, reader.nextToken());
        reader.nextToken();
        assertEquals("c", reader.getText());

        assertEquals(JSONReader.COLON, reader.nextToken());
        reader.nextToken();
        assertEquals("3", reader.getText());

        assertEquals(JSONReader.EOF, reader.nextToken());
    }

    @Test
    public void testReadEmptyStream() throws IOException {
        StringReader stringReader = new StringReader("");
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JSONReader.EOF, reader.nextToken());
    }

    @Test
    public void testCloseReader() throws IOException {
        StringReader stringReader = new StringReader("{}");
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        reader.nextToken();
        assertDoesNotThrow(() -> reader.close());
    }

    @Test
    public void testReadLongNumbersWithTypeSuffix() throws IOException {
        String json = "[123L, 45.6f, 78.9d]";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, rbuf, cbuf);

        assertEquals(JSONReader.START_BRACKET, reader.nextToken());

        reader.nextToken();
        Type<Long> longType = N.typeOf(Long.class);
        assertEquals(123L, reader.readValue(longType));

        assertEquals(JSONReader.COMMA, reader.nextToken());
        Type<Float> floatType = N.typeOf(Float.class);
        assertEquals(45.6f, reader.readValue(floatType), 0.001f);

        assertEquals(JSONReader.END_BRACKET, reader.nextToken());
        Type<Double> doubleType = N.typeOf(Double.class);
        assertEquals(78.9d, reader.readValue(doubleType), 0.001);

        assertEquals(JSONReader.EOF, reader.nextToken());
    }
}
