package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

public class JsonStringReaderTest extends TestBase {

    private final char[] cbuf = new char[256];

    private JsonReader parse(final String json) {
        return JsonStringReader.parse(json, cbuf);
    }

    private static final class ExposedJsonStringReader extends JsonStringReader {
        ExposedJsonStringReader(final String str, final char[] cbuf) {
            super(str, cbuf);
        }

        public void throwUnexpectedNonStringTokenForTest() {
            throwExceptionDueToUnexpectedNonStringToken();
        }
    }

    private static final class CapturingSymbolReader implements JsonReader.SymbolReader {
        private String propName;
        private int fromIndex;
        private int toIndex;

        @Override
        public ParserUtil.PropInfo getPropInfo(final String propName) {
            this.propName = propName;
            return null;
        }

        @Override
        public ParserUtil.PropInfo readPropInfo(final char[] cbuf, final int fromIndex, final int toIndex) {
            this.propName = String.valueOf(cbuf, fromIndex, toIndex - fromIndex);
            this.fromIndex = fromIndex;
            this.toIndex = toIndex;
            return null;
        }
    }

    @Test
    public void testReadDecimalFloatRoundsDirectlyFromText() {
        Type<Float> floatType = N.typeOf(Float.class);
        for (String value : new String[] { "0.5000000298023224", "-0.5000000298023224", "0.5000000298023224f", "-0.5000000298023224F" }) {
            JsonReader reader = JsonStringReader.parse(value, new char[64]);
            reader.nextToken(floatType);
            assertEquals(Float.floatToIntBits(Float.parseFloat(value)), Float.floatToIntBits(reader.readValue(floatType)), value);
        }
    }

    @Test
    public void testConstructor_InvalidRange() {
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[10], 5, 3, new char[256], null));
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[10], -1, 5, new char[256], null));
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[10], 0, -1, new char[256], null));
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[2], 0, 3, new char[256], null));
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[2], 3, 3, new char[256], null));
    }

    @Test
    public void testParse() {
        JsonReader reader = parse("{\"key\":\"value\"}");
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.close();

        String ranged = "prefix{\"key\":\"value\"}suffix";
        reader = JsonStringReader.parse(ranged, 6, ranged.length() - 6, cbuf);
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.close();
    }

    @Test
    public void testReadObjectAndArray() {
        JsonReader reader = parse("{\"name\":\"John\"}");
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

        reader = parse("  {  \"key\"  :  \"value\"  }  ");
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("key", reader.getText());
        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("value", reader.getText());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());

        reader = parse("[ 1 , 2 , 3 ]");
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals("1", reader.getText());
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("2", reader.getText());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("3", reader.getText());
        assertEquals(JsonReader.EOF, reader.nextToken());

        reader = parse("{\"outer\":{\"inner\":123}}");
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

        reader = parse("[\"string\", 123, true, null, 45.67]");
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

        reader = parse("[1,,3]");
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals("1", reader.getText());
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("3", reader.getText());

        reader = parse("{'key':'value'}");
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        assertEquals(JsonReader.START_SINGLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_SINGLE_QUOTE, reader.nextToken());
        assertEquals("key", reader.getText());
        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.START_SINGLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_SINGLE_QUOTE, reader.nextToken());
        assertEquals("value", reader.getText());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());

        reader = parse("");
        assertEquals(JsonReader.EOF, reader.nextToken());
        assertFalse(reader.hasText());
    }

    @Test
    public void testReadNumbers() {
        JsonReader reader = parse("[123, -456, 78.9, 1.23e10, -4.56E-7]");
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals("123", reader.getText());
        assertEquals(123, reader.readValue(N.typeOf(Integer.class)));
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("-456", reader.getText());
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("78.9", reader.getText());
        assertEquals(78.9, reader.readValue(N.typeOf(Double.class)), 0.001);
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("1.23e10", reader.getText());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("-4.56E-7", reader.getText());

        reader = parse("[123L, 456l, 78.9f, 12.34F, 56.78d, 90.12D]");
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals(123L, reader.readValue(N.typeOf(Long.class)));
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(456L, reader.readValue(N.typeOf(Long.class)));
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(78.9f, reader.readValue(N.typeOf(Float.class)), 0.001f);
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(12.34f, reader.readValue(N.typeOf(Float.class)), 0.001f);
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals(56.78d, reader.readValue(N.typeOf(Double.class)), 0.001);
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals(90.12d, reader.readValue(N.typeOf(Double.class)), 0.001);

        reader = parse("[0, -0, 0.0, -0.0]");
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals("0", reader.getText());
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("-0", reader.getText());
        assertEquals(JsonReader.COMMA, reader.nextToken());
        assertEquals("0.0", reader.getText());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("-0.0", reader.getText());

        reader = parse("[.5, -.5]");
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals(".5", reader.getText());
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("-.5", reader.getText());

        reader = parse("42");
        reader.nextToken(N.typeOf(Float.class));
        assertEquals(42.0f, reader.readValue(N.typeOf(Float.class)), 0.001f);
        reader = parse("100");
        reader.nextToken(N.typeOf(Double.class));
        assertEquals(100.0, reader.readValue(N.typeOf(Double.class)), 0.001);

        Type<Integer> intType = N.typeOf(Integer.class);
        reader = parse("-42");
        reader.nextToken(intType);
        assertEquals(-42, reader.readValue(intType));
        reader = parse("+42");
        reader.nextToken(intType);
        assertEquals(42, reader.readValue(intType));

        reader = parse("9.9e38");
        reader.nextToken();
        assertNotNull(reader.readValue(N.typeOf(Object.class)));
    }

    @Test
    public void testReadBooleansAndNull() {
        JsonReader reader = parse("[true, false]");
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        reader.nextToken();
        assertEquals("true", reader.getText());
        assertEquals(true, reader.readValue(N.typeOf(Boolean.class)));
        assertEquals(JsonReader.END_BRACKET, reader.nextToken());
        assertEquals("false", reader.getText());
        assertEquals(false, reader.readValue(N.typeOf(Boolean.class)));

        reader = parse("null");
        reader.nextToken();
        assertEquals("null", reader.getText());
        assertNull(reader.readValue(N.typeOf(String.class)));

        reader = parse("\"\"");
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("", reader.getText());

        reader = parse("{\"a\":false}");
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("a", reader.getText());
        assertEquals(JsonReader.COLON, reader.nextToken());
        assertEquals(JsonReader.END_BRACE, reader.nextToken());
        assertEquals("false", reader.getText());
        assertEquals(Boolean.FALSE, reader.readValue(N.typeOf(Boolean.class)));

        reader = parse("falsehood");
        assertEquals(JsonReader.EOF, reader.nextToken());
        assertEquals("falsehood", reader.getText());
    }

    @Test
    public void testReadEscapes() {
        JsonReader reader = parse("\"\\n\\r\\t\\b\\f\\\\\\/\\\"\"");
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("\n\r\t\b\f\\/\"", reader.getText());

        reader = parse("\"\\u0048\\u0065\\u006C\\u006C\\u006F\"");
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("Hello", reader.getText());

        reader = parse("\"\\uD83D\\uDE00\"");
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(2, reader.getText().length());
        assertEquals(0x1F600, reader.getText().codePointAt(0));

        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < 1000; i++) {
            sb.append("a");
        }
        reader = JsonStringReader.parse(sb.append("\"").toString(), new char[512]);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(1000, reader.getText().length());

        reader = JsonStringReader.parse("\"abcd\\nend\"", new char[4]);
        reader.nextToken();
        reader.nextToken();
        assertTrue(reader.getText().length() > 0);

        reader = JsonStringReader.parse("\"a\\nb\"", new char[1]);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("a\nb", reader.getText());

        reader = JsonStringReader.parse("\"\\n\"", new char[0]);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("\n", reader.getText());
    }

    @Test
    public void testReadEscapes_EdgeCase() {
        JsonReader reader = parse("\"\\");
        reader.nextToken();
        assertThrows(ParsingException.class, reader::nextToken);

        reader = parse("\"\\u00");
        reader.nextToken();
        assertThrows(ParsingException.class, reader::nextToken);

        reader = parse("\"\\uXYZW\"");
        reader.nextToken();
        assertThrows(ParsingException.class, reader::nextToken);

        reader = parse("\"\\uZ000\"");
        reader.nextToken();
        ParsingException ex = assertThrows(ParsingException.class, reader::nextToken);
        assertTrue(ex.getMessage().contains("'Z'"), ex.getMessage());
        assertFalse(ex.getMessage().contains("'" + (int) 'Z' + "'"), ex.getMessage());

        reader = parse("\"abc");
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertThrows(ParsingException.class, reader::nextToken);

        reader = parse("'abc");
        assertEquals(JsonReader.START_SINGLE_QUOTE, reader.nextToken());
        assertThrows(ParsingException.class, reader::nextToken);
    }

    @Test
    public void testLastTokenAndHasText() {
        JsonReader reader = parse("{}");
        assertEquals(-1, reader.lastToken());
        reader.nextToken();
        assertEquals(JsonReader.START_BRACE, reader.nextToken() == JsonReader.END_BRACE ? JsonReader.START_BRACE : reader.lastToken());
        reader.close();

        reader = parse("{\"a\":1}");
        assertEquals(-1, reader.lastToken());
        reader.nextToken();
        assertEquals(-1, reader.lastToken());
        reader.nextToken();
        assertEquals(JsonReader.START_BRACE, reader.lastToken());

        reader = parse("\"hello\"");
        reader.nextToken();
        reader.nextToken();
        assertTrue(reader.hasText());
        assertEquals("hello", reader.getText());
        reader.close();

        reader = parse("{}");
        reader.nextToken();
        assertFalse(reader.hasText());
        reader.close();

        reader = parse("123");
        assertNotNull(reader.nextToken(Type.of(Integer.class)));
        reader.close();
    }

    @Test
    public void testReadPropInfo() {
        JsonReader reader = JsonStringReader.parse("\"name\"", new char[256]);
        CapturingSymbolReader symbolReader = new CapturingSymbolReader();
        reader.nextToken();
        reader.nextToken();
        assertNull(reader.readPropInfo(symbolReader));
        assertEquals("name", symbolReader.propName);
        assertEquals(1, symbolReader.fromIndex);
        assertEquals(5, symbolReader.toIndex);

        reader = JsonStringReader.parse("\"first\\nname\"", new char[256]);
        symbolReader = new CapturingSymbolReader();
        reader.nextToken();
        reader.nextToken();
        assertNull(reader.readPropInfo(symbolReader));
        assertEquals("first\nname", symbolReader.propName);
        assertEquals(0, symbolReader.fromIndex);
        assertEquals("first\nname".length(), symbolReader.toIndex);
    }

    @Test
    public void testReadValue() {
        JsonReader reader = parse("123");
        reader.nextToken();
        assertEquals(123, reader.readValue(Type.of(Integer.class)));
        assertEquals(123, reader.readValue(N.typeOf(Object.class)));

        reader = parse("true");
        reader.nextToken();
        assertEquals(Boolean.TRUE, reader.readValue(N.typeOf(Boolean.class)));
        assertEquals("true", reader.readValue(N.typeOf(String.class)));

        reader = parse("false");
        reader.nextToken();
        assertEquals(Boolean.FALSE, reader.readValue(N.typeOf(Boolean.class)));
        assertEquals("false", reader.readValue(N.typeOf(String.class)));

        reader = parse("3.14");
        reader.nextToken(N.typeOf(Double.class));
        assertEquals(3.14, reader.readValue(N.typeOf(Double.class)), 0.001);

        reader = parse("9876543210");
        reader.nextToken(N.typeOf(Long.class));
        assertEquals(9876543210L, reader.readValue(N.typeOf(Long.class)));

        reader = parse("1.5");
        reader.nextToken(N.typeOf(Float.class));
        assertEquals(1.5f, reader.readValue(N.typeOf(Float.class)), 0.001f);

        reader = parse("\"hello world\"");
        reader.nextToken();
        reader.nextToken();
        assertEquals("hello world", reader.readValue(N.typeOf(String.class)));

        reader = parse("42");
        reader.nextToken();
        assertEquals(42, reader.readValue(N.typeOf(Object.class)));
        reader = parse("9999999999");
        reader.nextToken();
        assertEquals(9999999999L, reader.readValue(N.typeOf(Object.class)));

        reader = parse("42");
        reader.nextToken(N.typeOf(Integer.class));
        assertEquals("42", reader.readValue(N.typeOf(String.class)));

        reader = parse("null");
        reader.nextToken();
        assertNull(reader.readValue(N.typeOf(String.class)));
    }

    @Test
    public void testClose() {
        parse("{}").close();
        JsonStringReader stringReader = new JsonStringReader("{}", cbuf);
        stringReader.close();
        assertNotNull(stringReader);

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

    @Test
    public void testThrowExceptionDueToUnexpectedNonStringToken() {
        ExposedJsonStringReader reader = new ExposedJsonStringReader("{invalid", new char[32]);
        ParsingException exception = assertThrows(ParsingException.class, reader::throwUnexpectedNonStringTokenForTest);
        assertTrue(exception.getMessage().contains("expected"));
    }
    // ---------------------------------------------------------------------------------------------
    // Review fixes 2026-09-06 (P7-01..P7-04). Helpers: a token trace is "<token>" or "<token>:<text>"
    // (text only when hasText() is true), so a stale/phantom text shows up in the trace.
    // ---------------------------------------------------------------------------------------------

    private static List<String> reviewFixes20260906_P7_trace(final JsonReader reader) {
        final List<String> trace = new ArrayList<>();
        int token;

        do {
            token = reader.nextToken();
            trace.add(reader.hasText() ? token + ":" + reader.getText() : String.valueOf(token));
        } while (token != JsonReader.EOF);

        return trace;
    }

    private static String t(final int token) {
        return String.valueOf(token);
    }

    private static String t(final int token, final String text) {
        return token + ":" + text;
    }

    private static final int[] REVIEW_FIXES_20260906_P7_RBUF_SIZES = { 1, 2, 3, 4, 5, 8, 64, 4096 };

    private static JsonReader reviewFixes20260906_P7_reader(final String json, final int rbufSize, final int cbufSize) {
        return JsonStringReader.parse(json, new char[cbufSize]);
    }

    // P7-01: a root value followed only by whitespace must reach EOF with hasText()==false. Before the
    // fix JsonStreamReader.refill() rebased startIndexForText to 0 BEFORE the read, so at EOF the stale
    // buffer tail ("[1]\n", "]\n", "\n" depending on rbuf) was reported as text and the parser rejected
    // the document. The string reader never had the bug; it is the reference here.
    @Test
    public void reviewFixes20260906_P7_01_trailingWhitespaceAfterRootIsNotText() {
        final String[] inputs = { "[1]\n", "[1] ", "[1]\r\n", " [1] \n ", "[1]\n\n  ", "{\"a\":1}\n", "[\"x\"]\n", "[true]\n", "\"x\"\n", "[1] \t", "[1,2]\n",
                "{\"a\":1}\r\n", "{\"a\":\"x\"}   ", "[]\n", "{}\n" };

        for (final String json : inputs) {
            final List<String> expected = reviewFixes20260906_P7_trace(JsonStringReader.parse(json, new char[256]));
            assertEquals(t(JsonReader.EOF), expected.get(expected.size() - 1), "string reader: " + json);

            for (final int rbufSize : REVIEW_FIXES_20260906_P7_RBUF_SIZES) {
                for (final int cbufSize : new int[] { 0, 256 }) {
                    final JsonReader reader = reviewFixes20260906_P7_reader(json, rbufSize, cbufSize);
                    final List<String> trace = reviewFixes20260906_P7_trace(reader);
                    final String label = "rbuf=" + rbufSize + " cbuf=" + cbufSize + " json=" + json.replace("\n", "\\n").replace("\r", "\\r");

                    assertEquals(expected, trace, label);
                    assertFalse(reader.hasText(), label);
                    assertEquals("", reader.getText(), label);
                }
            }
        }
    }

    // P7-01: a whitespace-only source has no text either (previously fabricated a " " element for List targets).
    @Test
    public void reviewFixes20260906_P7_01_whitespaceOnlyInputHasNoText() {
        for (final String json : new String[] { " ", "   ", "\n\n\t ", "\r\n" }) {
            for (final int rbufSize : REVIEW_FIXES_20260906_P7_RBUF_SIZES) {
                final JsonReader reader = reviewFixes20260906_P7_reader(json, rbufSize, 64);
                assertEquals(JsonReader.EOF, reader.nextToken());
                assertFalse(reader.hasText(), "rbuf=" + rbufSize);
                assertEquals("", reader.getText());
            }
        }
    }

    // P7-01 negatives: real content after the root is still reported (the parser rejects it).
    @Test
    public void reviewFixes20260906_P7_01_contentAfterRootIsStillReported() {
        for (final int rbufSize : REVIEW_FIXES_20260906_P7_RBUF_SIZES) {
            assertEquals(List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF, "x")),
                    reviewFixes20260906_P7_trace(reviewFixes20260906_P7_reader("[1] x", rbufSize, 64)), "rbuf=" + rbufSize);
            assertEquals(List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "1"), t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "2"),
                    t(JsonReader.EOF)), reviewFixes20260906_P7_trace(reviewFixes20260906_P7_reader("[1]\n[2]", rbufSize, 64)), "rbuf=" + rbufSize);
            // a non-ASCII space is text, not whitespace (unchanged)
            assertEquals(List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF, "\u00A0")),
                    reviewFixes20260906_P7_trace(reviewFixes20260906_P7_reader("[1]\u00A0", rbufSize, 64)), "rbuf=" + rbufSize);
        }
    }

    // P7-01: unquoted scalar roots keep their text (documented raw-text contract), only the phantom tail is gone.
    @Test
    public void reviewFixes20260906_P7_01_scalarRootKeepsItsText() {
        for (final int rbufSize : REVIEW_FIXES_20260906_P7_RBUF_SIZES) {
            JsonReader reader = reviewFixes20260906_P7_reader("1\n", rbufSize, 64);
            assertEquals(JsonReader.EOF, reader.nextToken(N.typeOf(Integer.class)));
            assertTrue(reader.hasText());
            assertEquals("1", reader.getText());
            assertEquals(Integer.valueOf(1), reader.readValue(N.typeOf(Integer.class)));

            reader = reviewFixes20260906_P7_reader("true\n", rbufSize, 64);
            assertEquals(JsonReader.EOF, reader.nextToken());
            assertEquals("true", reader.getText());
            assertEquals(Boolean.TRUE, reader.readValue(N.typeOf(Boolean.class)));
        }
    }

    // P7-02/P7-03: a near-miss literal (tru, nul, fals, t, n, f, ...) must stop at the structural char that
    // terminates it instead of swallowing it; the stream reader must agree with the string reader for every
    // buffer size (P7-03: it used to eat the ']' in "[t]"; the string reader's remaining-length guards made
    // "[f,1]" tokenize differently from "[t,1]").
    @Test
    public void reviewFixes20260906_P7_02_nearMissLiteralStopsAtStructuralChar() {
        final Map<String, List<String>> expectations = new LinkedHashMap<>();
        expectations.put("[tru,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "tru"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[nul,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "nul"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[fals,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "fals"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[t,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "t"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[f,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "f"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[n,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "n"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[t:1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COLON, "t"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[t,f]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "t"), t(JsonReader.END_BRACKET, "f"), t(JsonReader.EOF)));
        expectations.put("[tru]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "tru"), t(JsonReader.EOF)));
        expectations.put("[nul]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "nul"), t(JsonReader.EOF)));
        expectations.put("[fal]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "fal"), t(JsonReader.EOF)));
        expectations.put("[t]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "t"), t(JsonReader.EOF)));
        expectations.put("[n]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "n"), t(JsonReader.EOF)));
        expectations.put("[f]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "f"), t(JsonReader.EOF)));
        expectations.put("[tr]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACKET, "tr"), t(JsonReader.EOF)));
        expectations.put("[f}", List.of(t(JsonReader.START_BRACKET), t(JsonReader.END_BRACE, "f"), t(JsonReader.EOF)));
        expectations.put("{\"a\":t}", List.of(t(JsonReader.START_BRACE), t(JsonReader.START_DOUBLE_QUOTE), t(JsonReader.END_DOUBLE_QUOTE, "a"),
                t(JsonReader.COLON), t(JsonReader.END_BRACE, "t"), t(JsonReader.EOF)));
        expectations.put("{\"a\":tru,\"b\":1}",
                List.of(t(JsonReader.START_BRACE), t(JsonReader.START_DOUBLE_QUOTE), t(JsonReader.END_DOUBLE_QUOTE, "a"), t(JsonReader.COLON),
                        t(JsonReader.COMMA, "tru"), t(JsonReader.START_DOUBLE_QUOTE), t(JsonReader.END_DOUBLE_QUOTE, "b"), t(JsonReader.COLON),
                        t(JsonReader.END_BRACE, "1"), t(JsonReader.EOF)));
        expectations.put("[t\"x\"]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.START_DOUBLE_QUOTE, "t"), t(JsonReader.END_DOUBLE_QUOTE, "x"),
                t(JsonReader.END_BRACKET), t(JsonReader.EOF)));
        expectations.put("a,t,b", List.of(t(JsonReader.COMMA, "a"), t(JsonReader.COMMA, "t"), t(JsonReader.EOF, "b")));
        expectations.put("t,f,n", List.of(t(JsonReader.COMMA, "t"), t(JsonReader.COMMA, "f"), t(JsonReader.EOF, "n")));
        expectations.put("[t\n,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "t"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));
        expectations.put("[abc,1]", List.of(t(JsonReader.START_BRACKET), t(JsonReader.COMMA, "abc"), t(JsonReader.END_BRACKET, "1"), t(JsonReader.EOF)));

        for (final Map.Entry<String, List<String>> e : expectations.entrySet()) {
            for (final int rbufSize : new int[] { 1, 2, 3, 64 }) {
                for (final int cbufSize : new int[] { 0, 64 }) {
                    assertEquals(e.getValue(), reviewFixes20260906_P7_trace(reviewFixes20260906_P7_reader(e.getKey(), rbufSize, cbufSize)),
                            "rbuf=" + rbufSize + " cbuf=" + cbufSize + " json=" + e.getKey());
                }
            }
        }
    }

    // P7-02 regression guards: real literals, literal-prefixed words and whitespace inside a literal are unchanged,
    // including literals split across refills (rbufSize is ignored).
    @Test
    public void reviewFixes20260906_P7_02_realLiteralsAndPrefixedWordsUnchanged() {
        final Type<Object> objType = N.typeOf(Object.class);

        for (final int rbufSize : new int[] { 1, 2, 3, 64 }) {
            JsonReader reader = reviewFixes20260906_P7_reader("[true,false,null]", rbufSize, 64);
            assertEquals(JsonReader.START_BRACKET, reader.nextToken());
            assertEquals(JsonReader.COMMA, reader.nextToken());
            assertSame(AbstractJsonReader.TRUE, reader.getText(), "rbuf=" + rbufSize);
            assertEquals(Boolean.TRUE, reader.readValue(objType));
            assertEquals(JsonReader.COMMA, reader.nextToken());
            assertSame(AbstractJsonReader.FALSE, reader.getText());
            assertEquals(Boolean.FALSE, reader.readValue(objType));
            assertEquals(JsonReader.END_BRACKET, reader.nextToken());
            assertSame(AbstractJsonReader.NULL, reader.getText());
            assertNull(reader.readValue(objType));
            assertEquals(JsonReader.EOF, reader.nextToken());

            reader = reviewFixes20260906_P7_reader("[ true ,\ttrue\n]", rbufSize, 64);
            assertEquals(JsonReader.START_BRACKET, reader.nextToken());
            assertEquals(JsonReader.COMMA, reader.nextToken());
            assertEquals(Boolean.TRUE, reader.readValue(objType));
            assertEquals(JsonReader.END_BRACKET, reader.nextToken());
            assertEquals(Boolean.TRUE, reader.readValue(objType));

            reader = reviewFixes20260906_P7_reader("{\"a\":false}", rbufSize, 64);
            assertEquals(JsonReader.START_BRACE, reader.nextToken());
            reader.nextToken();
            reader.nextToken();
            assertEquals(JsonReader.COLON, reader.nextToken());
            assertEquals(JsonReader.END_BRACE, reader.nextToken());
            assertEquals(Boolean.FALSE, reader.readValue(N.typeOf(Boolean.class)));

            for (final String word : new String[] { "truex", "falsey", "nullx", "falsehood", "trx", "nux" }) {
                reader = reviewFixes20260906_P7_reader("[" + word + "]", rbufSize, 64);
                assertEquals(JsonReader.START_BRACKET, reader.nextToken());
                assertEquals(JsonReader.END_BRACKET, reader.nextToken());
                assertEquals(word, reader.getText(), "rbuf=" + rbufSize);
                assertEquals(word, reader.readValue(objType));
            }

            for (final String bad : new String[] { "[t rue]", "[fal se]", "[nul l]", "[true false]" }) {
                final JsonReader r = reviewFixes20260906_P7_reader(bad, rbufSize, 64);
                assertEquals(JsonReader.START_BRACKET, r.nextToken());
                assertThrows(ParsingException.class, r::nextToken, "rbuf=" + rbufSize + " json=" + bad);
            }

            for (final String bare : new String[] { "t", "tr", "tru", "true", "f", "n", "nul" }) {
                reader = reviewFixes20260906_P7_reader(bare, rbufSize, 64);
                assertEquals(JsonReader.EOF, reader.nextToken());
                assertEquals(bare, reader.getText(), "rbuf=" + rbufSize);
            }
        }
    }

    // P7-04: an unquoted number read into a String target keeps its spelling (the fast-path Number used to
    // re-spell "007" as "7", "1.50" as "1.5", "+5" as "5", "123L" as "123"); other targets are unchanged.
    @Test
    public void reviewFixes20260906_P7_04_unquotedNumberIntoStringKeepsSpelling() {
        final Type<String> stringType = N.typeOf(String.class);
        final Type<Integer> intType = N.typeOf(Integer.class);
        final String[] tokens = { "1.50", "007", "+5", "123L", "1.5f", "1.5L", "010", "-0", "00", "1e5", "42", "0x1F", "12345678901234567890", "1.0" };

        for (final int rbufSize : new int[] { 1, 2, 64 }) {
            for (final String token : tokens) {
                for (final Type<?> hint : new Type<?>[] { stringType, intType, null }) {
                    for (final String json : new String[] { token, " " + token + " ", token + "\n", "[" + token + "]" }) {
                        final JsonReader reader = reviewFixes20260906_P7_reader(json, rbufSize, 64);

                        if (json.startsWith("[")) {
                            assertEquals(JsonReader.START_BRACKET, reader.nextToken());
                            assertEquals(JsonReader.END_BRACKET, reader.nextToken(hint));
                        } else {
                            assertEquals(JsonReader.EOF, reader.nextToken(hint));
                        }

                        final String label = "rbuf=" + rbufSize + " hint=" + hint + " json=" + json;
                        assertEquals(token, reader.getText(), label);
                        assertEquals(token, reader.readValue(stringType), label);
                    }
                }
            }

            // regression guards: numeric and Object targets still get numbers
            JsonReader reader = reviewFixes20260906_P7_reader("007", rbufSize, 64);
            reader.nextToken(intType);
            assertEquals(Integer.valueOf(7), reader.readValue(intType));

            reader = reviewFixes20260906_P7_reader("1.50", rbufSize, 64);
            reader.nextToken();
            assertEquals(Double.valueOf(1.5), reader.readValue(N.typeOf(Object.class)));

            reader = reviewFixes20260906_P7_reader("1.50", rbufSize, 64);
            reader.nextToken(stringType);
            assertEquals(Double.valueOf(1.5), reader.readValue(N.typeOf(Double.class)));

            reader = reviewFixes20260906_P7_reader("1.50", rbufSize, 64);
            reader.nextToken(stringType);
            assertEquals(new java.math.BigDecimal("1.50"), reader.readValue(N.typeOf(java.math.BigDecimal.class)));

            // a quoted number is a string either way
            reader = reviewFixes20260906_P7_reader("\"007\"", rbufSize, 64);
            reader.nextToken();
            reader.nextToken(stringType);
            assertEquals("007", reader.readValue(stringType));
        }
    }

    private static Object fixG08_readNumber(final String token, final Class<?> targetClass) {
        final JsonReader reader = JsonStringReader.parse(token, new char[64]);
        reader.nextToken();
        return reader.readValue(N.typeOf(targetClass));
    }

    // G08-1: the raw-token re-parse branch fired for targets that cannot read the token's spelling. A decimal
    // token is cached as Double, so "1.5" reached the integral handlers (Numbers.toInt/toLong reject the
    // fractional spelling) and a Java type suffix reached BigInteger/BigDecimal ("123L", "1.5f") - both threw
    // NumberFormatException where converting the cached value succeeded before the branch existed.
    @Test
    public void fixG08_F1_rawTokenIsOnlyReparsedWhenTheTargetCanReadIt() {
        assertEquals(Integer.valueOf(1), fixG08_readNumber("1.5", int.class));
        assertEquals(Long.valueOf(1L), fixG08_readNumber("1.5", long.class));
        assertEquals(Short.valueOf((short) 1), fixG08_readNumber("1.5", short.class));
        assertEquals(Byte.valueOf((byte) 1), fixG08_readNumber("1.5", byte.class));
        assertEquals(Integer.valueOf(-3), fixG08_readNumber("-3.7", Integer.class));
        assertEquals(java.math.BigInteger.ONE, fixG08_readNumber("1.5", java.math.BigInteger.class));

        // a Java type suffix is stripped before the raw token reaches a parser that rejects it
        assertEquals(java.math.BigInteger.valueOf(123), fixG08_readNumber("123L", java.math.BigInteger.class));
        assertEquals(java.math.BigInteger.valueOf(123), fixG08_readNumber("123d", java.math.BigInteger.class));
        assertEquals(new java.math.BigDecimal("1.5"), fixG08_readNumber("1.5f", java.math.BigDecimal.class));
        assertEquals(Long.valueOf(123L), fixG08_readNumber("123L", long.class));

        // the precision this branch exists for is untouched
        assertEquals(new java.math.BigDecimal("1.50"), fixG08_readNumber("1.50", java.math.BigDecimal.class));
        assertEquals(java.math.BigInteger.valueOf(123), fixG08_readNumber("123", java.math.BigInteger.class));
        assertEquals(Float.valueOf(1.5f), fixG08_readNumber("1.5", float.class));
        assertEquals(Double.valueOf(1.5d), fixG08_readNumber("1.5", double.class));

        // an out-of-range value is still rejected (as ArithmeticException), never silently truncated
        assertThrows(ArithmeticException.class, () -> fixG08_readNumber("999999999999.5", byte.class));
    }

    // R03: the fractional-token short circuit added to readValue must answer exactly what the raw-token
    // attempt answered through its NumberFormatException, for every class it now short-circuits - the
    // committed G08-1 test covered six of the nine, leaving the Long/Short/Byte wrappers unpinned.
    @Test
    public void reviewFixes20260908_fractionalTokensTruncateTowardZeroForEveryIntegralTarget() {
        for (final Class<?> target : List.of(int.class, Integer.class, long.class, Long.class, short.class, Short.class, byte.class, Byte.class,
                java.math.BigInteger.class)) {
            assertEquals("1", String.valueOf(fixG08_readNumber("1.9", target)), target.getName());
            assertEquals("-1", String.valueOf(fixG08_readNumber("-1.9", target)), target.getName());
            assertEquals("0", String.valueOf(fixG08_readNumber("0.5", target)), target.getName());
            assertEquals("1", String.valueOf(fixG08_readNumber("1.5f", target)), target.getName());
        }

        // the targets that CAN read a fractional spelling still get the token's own text, not the cache
        assertEquals(new java.math.BigDecimal("1.90"), fixG08_readNumber("1.90", java.math.BigDecimal.class));
        assertEquals(Float.valueOf(1.9f), fixG08_readNumber("1.9", float.class));
        assertEquals(Double.valueOf(1.9d), fixG08_readNumber("1.9", double.class));

        // and an out-of-range truncation is still rejected rather than wrapped around
        assertThrows(ArithmeticException.class, () -> fixG08_readNumber("99999.5", byte.class));
        assertThrows(ArithmeticException.class, () -> fixG08_readNumber("99999.5", short.class));
    }

    // ---- perf review 2026-10-07 G15 begin ----

    /** Reads one unquoted token the way the parser does: the value-type hint of nextToken is the slot's own type. */
    private static Object g15_read(final String token, final Type<?> hint, final Type<?> target) {
        final JsonReader reader = JsonStringReader.parse(token, new char[64]);
        reader.nextToken(hint);
        return reader.readValue(target);
    }

    private static Object g15_read(final String token, final Class<?> targetClass) {
        final Type<?> type = N.typeOf(targetClass);
        return g15_read(token, type, type);
    }

    /** A Type whose javaType is Integer but which is not the built-in handler: it must keep its own conversion route. */
    private static final class G15CustomIntegerType extends com.landawn.abacus.type.AbstractType<Integer> {
        G15CustomIntegerType() {
            super("G15CustomInteger");
        }

        @Override
        public Class<Integer> javaType() {
            return Integer.class;
        }

        @Override
        public boolean isString() {
            // routes readValue to the "keep the token text" branch, which is observably not the Integer result
            return true;
        }

        @Override
        public String stringOf(final Integer x) {
            return x == null ? null : x.toString();
        }

        @Override
        public Integer valueOf(final String str) {
            return str == null ? null : Integer.valueOf(str.trim());
        }
    }

    // G15-01: a built-in int/long/float/double slot hands back the value the number fast path cached for it
    // (an int-hinted integral token is cached as Long): same wrappers, same cache identity, same range failures.
    @Test
    public void testReadValue_builtInNumericSlotsPinned() {
        // Long cache -> int/Integer slot: narrowed through Integer.valueOf (cache identity preserved)
        assertSame(Integer.valueOf(7), g15_read("7", int.class));
        assertSame(Integer.valueOf(7), g15_read("7", Integer.class));
        assertSame(Integer.valueOf(-128), g15_read("-128", int.class));
        assertEquals(Integer.valueOf(300), g15_read("300", int.class));
        assertEquals(Integer.valueOf(300), g15_read("300", Integer.class));
        assertEquals(Integer.valueOf(Integer.MAX_VALUE), g15_read("2147483647", int.class));
        assertEquals(Integer.valueOf(Integer.MIN_VALUE), g15_read("-2147483648", Integer.class));
        assertEquals(Integer.valueOf(123), g15_read("123L", int.class));
        assertEquals(Integer.valueOf(123), g15_read("123l", Integer.class));
        assertEquals(Integer.valueOf(5), g15_read("+5", int.class));
        assertEquals(Integer.valueOf(0), g15_read("-0", int.class));

        // one past the int range still fails the same way (range check of the generic conversion)
        assertThrows(ArithmeticException.class, () -> g15_read("2147483648", int.class));
        assertThrows(ArithmeticException.class, () -> g15_read("2147483648L", Integer.class));
        assertThrows(ArithmeticException.class, () -> g15_read("-2147483649", int.class));

        // Long cache -> long/Long slot: the cached wrapper itself
        assertSame(Long.valueOf(7L), g15_read("7", long.class));
        assertSame(Long.valueOf(7L), g15_read("7", Long.class));
        assertEquals(Long.valueOf(2147483648L), g15_read("2147483648", long.class));
        assertEquals(Long.valueOf(Long.MAX_VALUE), g15_read("9223372036854775807", Long.class));
        assertEquals(Long.valueOf(Long.MIN_VALUE), g15_read("-9223372036854775808", long.class));
        assertEquals(Long.valueOf(123L), g15_read("123L", long.class));
        // 19 digits beyond the long range are not cached: the token text reaches the handler, which reports the overflow
        assertThrows(ArithmeticException.class, () -> g15_read("9223372036854775808", long.class));

        // Double cache -> double/Double slot, Float cache -> float/Float slot
        assertEquals(Double.valueOf(1.5d), g15_read("1.5", double.class));
        assertEquals(Double.valueOf(1.5d), g15_read("1.5", Double.class));
        assertEquals(Double.valueOf(7d), g15_read("7", double.class));
        assertEquals(Double.valueOf(7d), g15_read("7", Double.class));
        assertEquals(Double.valueOf(0.1d), g15_read("0.1", double.class));
        assertEquals(Double.valueOf(1.5d), g15_read("1.5d", double.class));
        assertEquals(Float.valueOf(1.5f), g15_read("1.5", float.class));
        assertEquals(Float.valueOf(1.5f), g15_read("1.5", Float.class));
        assertEquals(Float.valueOf(0.1f), g15_read("0.1", float.class));
        assertEquals(Float.valueOf(7f), g15_read("7", float.class));
        assertEquals(Float.valueOf(1.5f), g15_read("1.5f", Float.class));

        // spellings the fast path does not cache (negative zero, 17+ digit mantissa, exponent) still go through the text
        assertEquals(Long.valueOf(Double.doubleToRawLongBits(-0.0d)), Long.valueOf(Double.doubleToRawLongBits((Double) g15_read("-0.0", double.class))));
        assertEquals(Double.valueOf(0.30000000000000004d), g15_read("0.30000000000000004", double.class));
        assertEquals(Double.valueOf(1000d), g15_read("1e3", double.class));
        assertEquals(Integer.valueOf(1000), g15_read("1000", int.class));

        // Integer cache (untyped hint) -> int/Integer slot: the cached wrapper; -> long slot: widened through the generic conversion
        final Type<Object> objectHint = N.typeOf(Object.class);
        assertSame(Integer.valueOf(7), g15_read("7", objectHint, N.typeOf(int.class)));
        assertSame(Integer.valueOf(7), g15_read("7", objectHint, N.typeOf(Integer.class)));
        assertEquals(Integer.valueOf(300), g15_read("300", objectHint, N.typeOf(Integer.class)));
        assertEquals(Long.valueOf(7L), g15_read("7", objectHint, N.typeOf(long.class)));
        assertEquals(Double.valueOf(7d), g15_read("7", objectHint, N.typeOf(double.class)));
        assertEquals(Long.valueOf(2147483648L), g15_read("2147483648", objectHint, N.typeOf(Long.class)));

        // cross-representation slots keep their exact routes: a cached decimal into an integral slot truncates,
        // a cached float into a double slot re-reads the token (no double rounding), a cached long into a double slot widens
        assertEquals(Integer.valueOf(1), g15_read("1.5", int.class));
        assertEquals(Long.valueOf(1L), g15_read("1.9", long.class));
        assertEquals(Double.valueOf(1.5d), g15_read("1.5f", N.typeOf(float.class), N.typeOf(double.class)));
        assertEquals(Double.valueOf(0.1d), g15_read("0.1f", N.typeOf(float.class), N.typeOf(double.class)));
        assertEquals(Float.valueOf(0.1f), g15_read("0.1", N.typeOf(double.class), N.typeOf(float.class)));
        assertEquals(Double.valueOf(7d), g15_read("7", N.typeOf(long.class), N.typeOf(double.class)));
        assertEquals(Short.valueOf((short) 7), g15_read("7", short.class));
        assertEquals(Byte.valueOf((byte) 7), g15_read("7", byte.class));
        assertEquals(new java.math.BigDecimal("1.50"), g15_read("1.50", java.math.BigDecimal.class));
        assertEquals(java.math.BigInteger.valueOf(7), g15_read("7", java.math.BigInteger.class));
        assertEquals("007", g15_read("007", N.typeOf(int.class), N.typeOf(String.class)));
        assertEquals(Integer.valueOf(7), g15_read("7", objectHint, objectHint));

        // quoted numbers never touch the numeric cache
        final JsonReader quoted = JsonStringReader.parse("\"7\"", new char[64]);
        quoted.nextToken(N.typeOf(int.class));
        quoted.nextToken(N.typeOf(int.class));
        assertEquals(Integer.valueOf(7), quoted.readValue(N.typeOf(int.class)));

        // the same through the parser, both readers
        final JsonParser parser = ParserFactory.createJsonParser();
        final String doc = "[7,300,-128,2147483647,-2147483648,123L,1.5,0]";
        final int[] expectedInts = { 7, 300, -128, Integer.MAX_VALUE, Integer.MIN_VALUE, 123, 1, 0 };
        org.junit.jupiter.api.Assertions.assertArrayEquals(expectedInts, parser.deserialize(doc, int[].class));
        org.junit.jupiter.api.Assertions.assertArrayEquals(expectedInts, parser.deserialize(new java.io.StringReader(doc), int[].class));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 7, 300, -128, Integer.MAX_VALUE, Integer.MIN_VALUE, 123, 1, 0 },
                parser.deserialize(doc, long[].class));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 7, 300, -128, Integer.MAX_VALUE, Integer.MIN_VALUE, 123, 1.5, 0 },
                parser.deserialize(doc, double[].class), 0d);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 7, 300, -128, Integer.MAX_VALUE, Integer.MIN_VALUE, 123, 1.5f, 0 },
                parser.deserialize(doc, float[].class), 0f);
        assertEquals(List.of(7, 300, -128, Integer.MAX_VALUE, Integer.MIN_VALUE, 123, 1, 0), parser.deserialize(doc, Type.<List<Integer>> of("List<Integer>")));
        assertThrows(ArithmeticException.class, () -> parser.deserialize("[2147483648]", int[].class));
    }

    // G15-01: only the exact built-in handlers take the short route; a custom Type with the same javaType keeps its own path
    @Test
    public void testReadValue_customTypeWithBuiltInJavaTypeKeepsItsOwnRoute() {
        final G15CustomIntegerType custom = new G15CustomIntegerType();

        // isNumber() is false, so the token is cached as Integer, which the javaType match hands back as is
        assertEquals(Integer.valueOf(7), g15_read("7", custom, custom));
        assertEquals(Integer.valueOf(300), g15_read("300", custom, custom));

        // a Long cache (number hint) into the custom type: the custom type's own route (here: the token text through
        // its isString branch), never the built-in narrowing to Integer
        assertEquals("7", g15_read("7", N.typeOf(int.class), custom));
        assertEquals("2147483648", g15_read("2147483648", N.typeOf(long.class), custom));

        // and the built-in handler next to it is unaffected
        assertSame(Integer.valueOf(7), g15_read("7", custom, N.typeOf(Integer.class)));
    }

    // ---- perf review 2026-10-07 G15 end ----
}
