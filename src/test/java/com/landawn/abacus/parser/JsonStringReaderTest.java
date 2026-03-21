package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringReader;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

public class JsonStringReaderTest extends TestBase {

    private char[] cbuf = new char[256];

    private static final class ExposedJsonStringReader extends JsonStringReader {
        ExposedJsonStringReader(final String str, final char[] cbuf) {
            super(str, cbuf);
        }

        void throwUnexpectedNonStringTokenForTest() {
            throwExceptionDueToUnexpectedNonStringToken();
        }
    }

    // Constructor validation and uncovered branch coverage

    @Test
    public void testConstructor_InvalidRange_ToIndexLessThanBeginIndex_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[10], 5, 3, new char[256], null));
    }

    @Test
    public void testConstructor_NegativeBeginIndex_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[10], -1, 5, new char[256], null));
    }

    @Test
    public void testConstructor_NegativeToIndex_ThrowsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[10], 0, -1, new char[256], null));
    }

    @Test
    public void testReadSimpleObject() {
        String json = "{\"name\":\"John\"}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("name", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
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
    public void testReadEscapedCharacters() {
        String json = "\"\\n\\r\\t\\b\\f\\\\\\/\\\"\"";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("\n\r\t\b\f\\/\"", reader.getText());
    }

    @Test
    public void testReadUnicodeEscape() {
        String json = "\"\\u0048\\u0065\\u006C\\u006C\\u006F\"";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("Hello", reader.getText());
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
    public void testReadWhitespace() {
        String json = "  {  \"key\"  :  \"value\"  }  ";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("key", reader.getText());

        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
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
    public void testReadVeryLongString() {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < 1000; i++) {
            sb.append("a");
        }
        sb.append("\"");

        JsonReader reader = JsonStringReader.parse(sb.toString(), new char[512]);

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
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

    // readNumber: integer with Float type (no decimal point) -> numValue = (float) ret
    @Test
    public void testReadIntegerWithFloatType_NumValueCastToFloat() {
        String json = "42";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken(N.typeOf(Float.class));
        Float value = reader.readValue(N.typeOf(Float.class));
        assertEquals(42.0f, value, 0.001f);
    }

    // readNumber: integer with Double type (no decimal point) -> numValue = (double) ret
    @Test
    public void testReadIntegerWithDoubleType_NumValueCastToDouble() {
        String json = "100";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken(N.typeOf(Double.class));
        Double value = reader.readValue(N.typeOf(Double.class));
        assertEquals(100.0, value, 0.001);
    }

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
    public void testReadNull() {
        String json = "null";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken();
        assertEquals("null", reader.getText());
        Type<String> stringType = N.typeOf(String.class);
        assertNull(reader.readValue(stringType));
    }

    @Test
    public void testReadEmptyString() {
        String json = "\"\"";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("", reader.getText());
    }

    @Test
    public void testReadSingleQuotedString() {
        String json = "{'key':'value'}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

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
    public void testEmptyInput() {
        String json = "";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(JsonReader.EOF, reader.nextToken());
        assertFalse(reader.hasText());
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

    @Test
    public void testReadNegativeNumber() {
        String json = "-42";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        Type<Integer> intType = N.typeOf(Integer.class);
        reader.nextToken(intType);
        Integer value = reader.readValue(intType);
        assertEquals(-42, value);
    }

    @Test
    public void testReadPositiveSignNumber() {
        String json = "+42";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        Type<Integer> intType = N.typeOf(Integer.class);
        reader.nextToken(intType);
        Integer value = reader.readValue(intType);
        assertEquals(42, value);
    }

    @Test
    public void testLastTokenInitially() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        assertEquals(-1, reader.lastToken());
    }

    @Test
    public void test_lastToken() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        int lastToken = reader.lastToken();

        reader.close();
        assertNotNull(lastToken);
    }

    @Test
    public void testLastTokenAfterMultipleReads() {
        String json = "{\"a\":1}";
        JsonReader reader = JsonStringReader.parse(json, cbuf);

        reader.nextToken(); // START_BRACE
        assertEquals(-1, reader.lastToken());

        reader.nextToken(); // START_DOUBLE_QUOTE
        assertEquals(JsonReader.START_BRACE, reader.lastToken());
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
        assertNotNull(token);
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

    // enlargeCharBuffer: buffer overflow in saveToBuffer -> enlarges when prefix content > cbuf size
    @Test
    public void testReadString_PrefixLargerThanCbuf_TriggersEnlargement() {
        // Tiny cbuf=4, prefix "abcd" (4 chars) then escape forces saveToBuffer + enlargeCharBuffer
        String json = "\"abcd\\nend\"";
        JsonReader reader = JsonStringReader.parse(json, new char[4]);
        reader.nextToken(); // START_DOUBLE_QUOTE
        reader.nextToken(); // END_DOUBLE_QUOTE - saveToBuffer enlarges cbuf
        assertTrue(reader.getText().length() > 0);
    }

    @Test
    public void testThrowExceptionDueToUnexpectedNonStringToken() {
        ExposedJsonStringReader reader = new ExposedJsonStringReader("{invalid", new char[32]);

        ParsingException exception = assertThrows(ParsingException.class, reader::throwUnexpectedNonStringTokenForTest);
        assertTrue(exception.getMessage().contains("expected"));
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
    public void testReadValueTrueForBooleanType() {
        String json = "true";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken();

        Type<Boolean> boolType = N.typeOf(Boolean.class);
        assertEquals(Boolean.TRUE, reader.readValue(boolType));
    }

    @Test
    public void testReadValueFalseForBooleanType() {
        String json = "false";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken();

        Type<Boolean> boolType = N.typeOf(Boolean.class);
        assertEquals(Boolean.FALSE, reader.readValue(boolType));
    }

    @Test
    public void testReadValueDoubleWithType() {
        String json = "3.14";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        Type<Double> doubleType = N.typeOf(Double.class);
        reader.nextToken(doubleType);
        Double value = reader.readValue(doubleType);
        assertEquals(3.14, value, 0.001);
    }

    @Test
    public void testReadValueLongWithType() {
        String json = "9876543210";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        Type<Long> longType = N.typeOf(Long.class);
        reader.nextToken(longType);
        Long value = reader.readValue(longType);
        assertEquals(9876543210L, value);
    }

    @Test
    public void testReadValueFloatWithType() {
        String json = "1.5";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        Type<Float> floatType = N.typeOf(Float.class);
        reader.nextToken(floatType);
        Float value = reader.readValue(floatType);
        assertEquals(1.5f, value, 0.001f);
    }

    @Test
    public void testReadValueStringFromQuotes() {
        String json = "\"hello world\"";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        reader.nextToken();
        Type<String> strType = N.typeOf(String.class);
        String value = reader.readValue(strType);
        assertEquals("hello world", value);
    }

    @Test
    public void testReadValueObjectForNumber() {
        String json = "42";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        Type<Object> objType = N.typeOf(Object.class);
        Object value = reader.readValue(objType);
        assertEquals(42, value);
    }

    @Test
    public void testReadValueObjectForLargeNumber() {
        String json = "9999999999";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);

        reader.nextToken();
        Type<Object> objType = N.typeOf(Object.class);
        Object value = reader.readValue(objType);
        assertEquals(9999999999L, value);
    }

    @Test
    public void testReadValueStringFromNumberToken() {
        JsonReader reader = JsonStringReader.parse("42", new char[256]);

        reader.nextToken(N.typeOf(Integer.class));
        String value = reader.readValue(N.typeOf(String.class));

        assertEquals("42", value);
    }

    // readValue text branch: text == TRUE but type is not boolean/object -> type.valueOf(text)
    @Test
    public void testReadValue_TrueTokenAsStringType_ReturnsStringTrue() {
        String json = "true";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken();
        String value = reader.readValue(N.typeOf(String.class));
        assertEquals("true", value);
    }

    @Test
    public void testReadValue_FalseTokenAsStringType_ReturnsStringFalse() {
        String json = "false";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken();
        String value = reader.readValue(N.typeOf(String.class));
        assertEquals("false", value);
    }

    @Test
    public void testReadValueNullForStringType() {
        String json = "null";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken();

        Type<String> strType = N.typeOf(String.class);
        assertNull(reader.readValue(strType));
    }

    // readValue as Object for a float-producing number string (createNumber -> Float -> L631-632)
    @Test
    public void testReadValue_ObjectForFloatSuffixString_ReturnsNumericValue() {
        // A number without type hint that looks like a float but fails fast parse
        // "9.9e38" — very large, might produce Float from createNumber
        String json = "9.9e38";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken();
        Type<Object> objType = N.typeOf(Object.class);
        Object value = reader.readValue(objType);
        assertNotNull(value);
    }

    @Test
    public void test_close() {
        String json = "{}";
        JsonReader reader = JsonStringReader.parse(json, new char[256]);
        reader.close();
        assertNotNull(reader);
    }

    @Test
    public void testCloseWithNullReader() {
        String json = "{}";
        JsonStringReader reader = new JsonStringReader(json, new char[256]);
        // reader field is null for string-based reader, close should be no-op
        reader.close();
        assertNotNull(reader);
    }

    // close(): reader != null and reader.close() throws IOException -> UncheckedIOException
    @Test
    public void testClose_ReaderThrowsIOException_WrapsAsUncheckedIOException() {
        java.io.Reader failingReader = new java.io.Reader() {
            @Override
            public int read(final char[] buf, final int off, final int len) throws IOException {
                return -1;
            }

            @Override
            public void close() throws IOException {
                throw new IOException("simulated close failure");
            }
        };
        char[] content = "{}".toCharArray();
        JsonStringReader reader = new JsonStringReader(content, 0, content.length, new char[256], failingReader);
        assertThrows(UncheckedIOException.class, reader::close);
    }

    // readEscapeCharacter: backslash at end of string -> throws ParsingException
    @Test
    public void testReadEscapeCharacter_AtEndOfInput_ThrowsParsingException() {
        // JSON string: "\  (one double-quote + one backslash, no closing quote)
        String json = "\"\\";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken(); // START_DOUBLE_QUOTE
        assertThrows(ParsingException.class, () -> reader.nextToken());
    }

    // readEscapeCharacter: incomplete unicode (backslash-u with only 2 hex digits then end) -> L740
    @Test
    public void testReadEscapeCharacter_IncompleteUnicode_AtEndOfString_ThrowsParsingException() {
        // JSON string ends before 4 hex digits of unicode escape: backslash + u + 0 + 0 (no closing quote)
        String json = "\"" + "\\u00";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken(); // START_DOUBLE_QUOTE
        assertThrows(ParsingException.class, () -> reader.nextToken());
    }

    // readEscapeCharacter: invalid hex digit in unicode escape -> L754
    @Test
    public void testReadEscapeCharacter_InvalidHexDigit_ThrowsParsingException() {
        // backslash-u followed by XYZW — X is not a valid hex digit
        String json = "\"" + "\\uXYZW\"";
        JsonReader reader = JsonStringReader.parse(json, cbuf);
        reader.nextToken(); // START_DOUBLE_QUOTE
        assertThrows(ParsingException.class, () -> reader.nextToken());
    }

}
