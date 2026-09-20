package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
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

public class JsonStreamReaderTest extends TestBase {

    private final char[] rbuf = new char[1024];
    private final char[] cbuf = new char[256];

    private JsonReader parse(final String json) {
        return JsonStreamReader.parse(new StringReader(json), rbuf, cbuf);
    }

    private JsonStreamReader stream(final String json, final int rbufSize) {
        return new JsonStreamReader(new StringReader(json), new char[rbufSize], new char[64]);
    }

    @Test
    public void testReadDecimalFloatRoundsDirectlyFromText() {
        Type<Float> floatType = N.typeOf(Float.class);
        for (String value : new String[] { "0.5000000298023224", "-0.5000000298023224", "0.5000000298023224f", "-0.5000000298023224F" }) {
            for (int bufferSize : new int[] { 1, 2, 8, 64 }) {
                JsonStreamReader reader = new JsonStreamReader(new StringReader(value), new char[bufferSize], new char[64]);
                reader.nextToken(floatType);
                assertEquals(Float.floatToIntBits(Float.parseFloat(value)), Float.floatToIntBits(reader.readValue(floatType)), value);
            }
        }
    }

    @Test
    public void testParse() {
        JsonStreamReader ranged = new JsonStreamReader(new StringReader(""), "{}rest".toCharArray(), 0, 2, new char[256]);
        assertEquals(JsonReader.START_BRACE, ranged.nextToken());
        assertEquals(JsonReader.END_BRACE, ranged.nextToken());

        JsonReader reader = JsonStreamReader.parse(new StringReader("{\"name\":\"John\"}"), new char[1024], new char[256]);
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.close();

        reader = parse("{\"key\":\"value\"}");
        assertEquals(JsonReader.START_BRACE, reader.nextToken());

        reader = JsonStreamReader.parse(new StringReader("\"escaped\\ntext\""), new char[2], new char[0]);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("escaped\ntext", reader.getText());
        reader.close();

        assertThrows(IllegalArgumentException.class, () -> JsonStreamReader.parse(new StringReader("{}"), new char[0], new char[8]));
        assertThrows(IllegalArgumentException.class, () -> JsonStreamReader.parse(null, new char[8], new char[8]));
        assertThrows(IllegalArgumentException.class, () -> JsonStreamReader.parse(new StringReader("{}"), null, new char[8]));
        assertThrows(IllegalArgumentException.class, () -> JsonStreamReader.parse(new StringReader("{}"), new char[8], null));
    }

    @Test
    public void testIncompleteTokens_EdgeCase() {
        assertEquals(-1, stream("t", 64).nextToken());
        assertEquals(-1, stream("f", 64).nextToken());
        assertEquals(-1, stream("foobar", 64).nextToken());
        assertEquals(JsonReader.COMMA, stream("foobar,", 2).nextToken());
    }

    @Test
    public void testReadEscapeAndUnicode() {
        JsonStreamReader reader = stream("\"\\b\"", 64);
        reader.nextToken();
        reader.nextToken();
        assertEquals("\b", reader.getText());

        reader = stream("\"\\u0048ello\"", 4);
        reader.nextToken();
        reader.nextToken();
        assertTrue(reader.getText().startsWith("H"));

        reader = stream("\"\\uD83D\\uDE00\"", 3);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(2, reader.getText().length());
        assertEquals(0x1F600, reader.getText().codePointAt(0));

        JsonReader r = parse("\"\\u0048\\u0065\\u006C\\u006C\\u006F \\u4E16\\u754C\"");
        assertEquals(JsonReader.START_DOUBLE_QUOTE, r.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, r.nextToken());
        assertEquals("Hello 世界", r.getText());

        reader = stream("\"test\\", 64);
        reader.nextToken();
        assertThrows(ParsingException.class, reader::nextToken);

        reader = stream("\"\\u00", 64);
        reader.nextToken();
        assertThrows(ParsingException.class, reader::nextToken);

        reader = stream("\"\\uXYZW\"", 64);
        reader.nextToken();
        assertThrows(ParsingException.class, reader::nextToken);
    }

    @Test
    public void testReadNumbers() {
        JsonStreamReader reader = stream("42", 64);
        Type<Float> floatType = N.typeOf(Float.class);
        reader.nextToken(floatType);
        assertEquals(42.0f, reader.readValue(floatType), 0.001f);

        reader = stream("100", 64);
        Type<Double> doubleType = N.typeOf(Double.class);
        reader.nextToken(doubleType);
        assertEquals(100.0, reader.readValue(doubleType), 0.001);

        reader = stream("12345678901234567890", 64);
        reader.nextToken();
        assertNotNull(reader.readValue(N.typeOf(Object.class)));

        JsonReader r = parse("[123456789012345, -987654321098765, 3.14159265358979, 2.71828182845905]");
        assertEquals(JsonReader.START_BRACKET, r.nextToken());
        r.nextToken();
        assertEquals(123456789012345L, r.readValue(N.typeOf(Long.class)));
        assertEquals(JsonReader.COMMA, r.nextToken());
        assertEquals(-987654321098765L, r.readValue(N.typeOf(Long.class)));
        assertEquals(JsonReader.COMMA, r.nextToken());
        assertEquals(3.14159265358979, r.readValue(N.typeOf(Double.class)), 0.00000000000001);
        assertEquals(JsonReader.END_BRACKET, r.nextToken());
        assertEquals(2.71828182845905, r.readValue(N.typeOf(Double.class)), 0.00000000000001);
        assertEquals(JsonReader.EOF, r.nextToken());

        r = parse("[123L, 45.6f, 78.9d]");
        assertEquals(JsonReader.START_BRACKET, r.nextToken());
        r.nextToken();
        assertEquals(123L, r.readValue(N.typeOf(Long.class)));
        assertEquals(JsonReader.COMMA, r.nextToken());
        assertEquals(45.6f, r.readValue(N.typeOf(Float.class)), 0.001f);
        assertEquals(JsonReader.END_BRACKET, r.nextToken());
        assertEquals(78.9d, r.readValue(N.typeOf(Double.class)), 0.001);

        r = parse("[-42, -3.14]");
        assertEquals(JsonReader.START_BRACKET, r.nextToken());
        r.nextToken(N.typeOf(Integer.class));
        assertEquals(-42, r.readValue(N.typeOf(Integer.class)));
        assertEquals(JsonReader.END_BRACKET, r.nextToken());
        assertEquals(-3.14, r.readValue(N.typeOf(Double.class)), 0.001);

        r = parse("123");
        r.nextToken();
        assertEquals(123, r.readValue(N.typeOf(Object.class)));
    }

    @Test
    public void testReadValues() {
        JsonReader r = parse("[\"first\", \"second with spaces\", \"third\\nwith\\nnewlines\"]");
        assertEquals(JsonReader.START_BRACKET, r.nextToken());
        r.nextToken();
        r.nextToken();
        assertEquals("first", r.getText());
        assertEquals(JsonReader.COMMA, r.nextToken());
        r.nextToken();
        r.nextToken();
        assertEquals("second with spaces", r.getText());
        assertEquals(JsonReader.COMMA, r.nextToken());
        r.nextToken();
        r.nextToken();
        assertEquals("third\nwith\nnewlines", r.getText());

        r = parse("[true, false, true, false]");
        assertEquals(JsonReader.START_BRACKET, r.nextToken());
        Type<Boolean> boolType = N.typeOf(Boolean.class);
        r.nextToken();
        assertEquals(true, r.readValue(boolType));
        assertEquals(JsonReader.COMMA, r.nextToken());
        assertEquals(false, r.readValue(boolType));
        assertEquals(JsonReader.COMMA, r.nextToken());
        assertEquals(true, r.readValue(boolType));
        assertEquals(JsonReader.END_BRACKET, r.nextToken());
        assertEquals(false, r.readValue(boolType));

        r = parse("[null, \"not null\", null]");
        assertEquals(JsonReader.START_BRACKET, r.nextToken());
        Type<String> stringType = N.typeOf(String.class);
        r.nextToken();
        assertNull(r.readValue(stringType));
        assertEquals(JsonReader.START_DOUBLE_QUOTE, r.nextToken());
        r.nextToken();
        assertEquals("not null", r.readValue(stringType));
        assertEquals(JsonReader.COMMA, r.nextToken());
        r.nextToken();
        assertNull(r.readValue(stringType));

        r = parse("{'key':'value'}");
        assertEquals(JsonReader.START_BRACE, r.nextToken());
        assertEquals(JsonReader.START_SINGLE_QUOTE, r.nextToken());
        assertEquals(JsonReader.END_SINGLE_QUOTE, r.nextToken());
        assertEquals("key", r.getText());
        assertEquals(JsonReader.COLON, r.nextToken());
        assertEquals(JsonReader.START_SINGLE_QUOTE, r.nextToken());
        assertEquals(JsonReader.END_SINGLE_QUOTE, r.nextToken());
        assertEquals("value", r.getText());
        assertEquals(JsonReader.END_BRACE, r.nextToken());

        r = parse("[\"line1\\nline2\", \"tab\\there\"]");
        assertEquals(JsonReader.START_BRACKET, r.nextToken());
        r.nextToken();
        r.nextToken();
        assertEquals("line1\nline2", r.getText());
        assertEquals(JsonReader.COMMA, r.nextToken());
        r.nextToken();
        r.nextToken();
        assertEquals("tab\there", r.getText());

        r = parse("{\"a\":{\"b\":1}}");
        assertEquals(JsonReader.START_BRACE, r.nextToken());
        r.nextToken();
        r.nextToken();
        assertEquals("a", r.getText());
        assertEquals(JsonReader.COLON, r.nextToken());
        assertEquals(JsonReader.START_BRACE, r.nextToken());
        r.nextToken();
        r.nextToken();
        assertEquals("b", r.getText());
        assertEquals(JsonReader.COLON, r.nextToken());
        r.nextToken();
        assertEquals("1", r.getText());
        assertEquals(JsonReader.END_BRACE, r.nextToken());
        assertEquals(JsonReader.EOF, r.nextToken());
    }

    @Test
    public void testReadWithSmallBuffer() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append("{\"id\":").append(i).append(",\"name\":\"item").append(i).append("\"}");
        }
        JsonReader reader = JsonStreamReader.parse(new StringReader(sb.append("]").toString()), new char[64], new char[64]);
        assertEquals(JsonReader.START_BRACKET, reader.nextToken());
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.nextToken();
        reader.nextToken();
        assertEquals("id", reader.getText());
        int objectCount = 1;
        int token;
        while ((token = reader.nextToken()) != JsonReader.EOF) {
            if (token == JsonReader.START_BRACE) {
                objectCount++;
            }
        }
        assertEquals(100, objectCount);

        sb = new StringBuilder("\"");
        for (int i = 0; i < 500; i++) {
            sb.append("abcdefghij");
        }
        reader = JsonStreamReader.parse(new StringReader(sb.append("\"").toString()), new char[128], new char[256]);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(5000, reader.getText().length());

        reader = JsonStreamReader.parse(new StringReader("{\"a\":1,\"b\":2,\"c\":3}"), new char[4], new char[4]);
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

        JsonStreamReader stream = stream("{\"a\":false}", 2);
        assertEquals(JsonReader.START_BRACE, stream.nextToken());
        stream.nextToken();
        stream.nextToken();
        assertEquals("a", stream.getText());
        assertEquals(JsonReader.COLON, stream.nextToken());
        assertEquals(JsonReader.END_BRACE, stream.nextToken());
        assertEquals("false", stream.getText());
        assertEquals(Boolean.FALSE, stream.readValue(N.typeOf(Boolean.class)));
    }

    @Test
    public void testTokensAndClose() {
        JsonReader reader = parse("\"test\"");
        reader.nextToken();
        reader.nextToken();
        assertTrue(reader.hasText());
        assertEquals("test", reader.getText());
        reader.close();

        reader = parse("{\"a\":1}");
        assertEquals(-1, reader.lastToken());
        reader.nextToken();
        assertEquals(-1, reader.lastToken());
        reader.nextToken();
        assertEquals(JsonReader.START_BRACE, reader.lastToken());

        reader = parse("42");
        reader.nextToken();
        assertTrue(reader.hasText());

        assertEquals(JsonReader.EOF, parse("").nextToken());
        assertEquals(JsonReader.EOF, parse("   ").nextToken());

        reader = parse("{}");
        reader.nextToken();
        assertDoesNotThrow(reader::close);

        reader = parse("{\"key\":\"value\"}");
        assertEquals(JsonReader.START_BRACE, reader.nextToken());
        reader.close();
    }

    @Test
    public void testRefill_ReaderThrowsIOException_WrapsAsUncheckedIOException() {
        Reader failingReader = new Reader() {
            @Override
            public int read(final char[] buf, final int off, final int len) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() {
            }
        };
        JsonStreamReader reader = new JsonStreamReader(failingReader, new char[4], new char[64]);
        assertThrows(UncheckedIOException.class, reader::nextToken);
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
        return new JsonStreamReader(new StringReader(json), new char[rbufSize], new char[cbufSize]);
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
    // including literals split across refills (rbuf 1..3).
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

}
