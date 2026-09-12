package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BufferedJsonWriterTest extends TestBase {

    @Test
    public void testStaticFields() {
        assertTrue(BufferedJsonWriter.LENGTH_OF_REPLACEMENT_CHARS > 0);
    }

    @Test
    public void testReplacementChars_Quote() {
        assertEquals("\\\"", new String(BufferedJsonWriter.REPLACEMENT_CHARS['"']));
    }

    @Test
    public void testReplacementChars_Backslash() {
        assertEquals("\\\\", new String(BufferedJsonWriter.REPLACEMENT_CHARS['\\']));
    }

    @Test
    public void testReplacementChars_Control_TabBackspaceNewlineCR_Formfeed() {
        assertEquals("\\t", new String(BufferedJsonWriter.REPLACEMENT_CHARS['\t']));
        assertEquals("\\b", new String(BufferedJsonWriter.REPLACEMENT_CHARS['\b']));
        assertEquals("\\n", new String(BufferedJsonWriter.REPLACEMENT_CHARS['\n']));
        assertEquals("\\r", new String(BufferedJsonWriter.REPLACEMENT_CHARS['\r']));
        assertEquals("\\f", new String(BufferedJsonWriter.REPLACEMENT_CHARS['\f']));
    }

    @Test
    public void testReplacementChars_AllControls_AreEscaped() {
        for (int i = 0; i < 32; i++) {
            assertTrue(BufferedJsonWriter.REPLACEMENT_CHARS[i] != null, "control char 0x" + Integer.toHexString(i) + " must be escaped");
        }
        assertTrue(BufferedJsonWriter.REPLACEMENT_CHARS[127] != null);
        assertEquals("\\u007f", new String(BufferedJsonWriter.REPLACEMENT_CHARS[127]));
    }

    @Test
    public void testReplacementChars_LineAndParaSeparator() {
        // U+2028 / U+2029 must be escaped to keep JSON valid as embedded JS source.
        assertEquals("\\u2028", new String(BufferedJsonWriter.REPLACEMENT_CHARS[0x2028]));
        assertEquals("\\u2029", new String(BufferedJsonWriter.REPLACEMENT_CHARS[0x2029]));
    }

    @Test
    public void testReplacementChars_PlainAsciiPrintable_NotEscaped() {
        // Letters, digits, symbols: no replacement.
        assertNull(BufferedJsonWriter.REPLACEMENT_CHARS['A']);
        assertNull(BufferedJsonWriter.REPLACEMENT_CHARS['0']);
        assertNull(BufferedJsonWriter.REPLACEMENT_CHARS[' ']);
        // Forward slash is NOT required to be escaped per RFC 8259.
        assertNull(BufferedJsonWriter.REPLACEMENT_CHARS['/']);
        // Comma is NOT escaped inside string content per RFC 8259.
        assertNull(BufferedJsonWriter.REPLACEMENT_CHARS[',']);
        // Single quote is not required to be escaped.
        assertNull(BufferedJsonWriter.REPLACEMENT_CHARS['\'']);
    }

    @Test
    public void testHtmlSafeReplacementChars_ExtraEscapes() {
        assertEquals("\\u003c", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['<']));
        assertEquals("\\u003e", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['>']));
        assertEquals("\\u0026", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['&']));
        assertEquals("\\u003d", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['=']));
        assertEquals("\\u0027", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['\'']));
    }

    @Test
    public void testHtmlSafeReplacementChars_StillCoversBaseEscapes() {
        // HTML-safe table is a clone of the regular one, so base escapes survive.
        assertEquals("\\\"", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['"']));
        assertEquals("\\\\", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['\\']));
        assertEquals("\\n", new String(BufferedJsonWriter.HTML_SAFE_REPLACEMENT_CHARS['\n']));
    }

    @Test
    public void testWriteCharacter_EscapesQuotesAndBackslash() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter("He said \"hi\\bye\"");
        assertEquals("He said \\\"hi\\\\bye\\\"", w.toString());
    }

    @Test
    public void testWriteCharacter_EscapesNewlinesAndTabs() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter("a\nb\tc\rd\be\ff");
        assertEquals("a\\nb\\tc\\rd\\be\\ff", w.toString());
    }

    @Test
    public void testWriteCharacter_NullCharIsUnicodeEscaped() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter("a" + (char) 0x00 + "b");
        assertEquals("a\\u0000b", w.toString());
    }

    @Test
    public void testWriteCharacter_DEL_AndOtherControls() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter(String.valueOf((char) 0x7F));
        assertEquals("\\u007f", w.toString());
    }

    @Test
    public void testWriteCharacter_SurrogatePair_PreservedVerbatim() throws IOException {
        // U+1F600 is a non-BMP char encoded as surrogate pair D83D DE00.
        String emoji = new String(Character.toChars(0x1F600));
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter(emoji);
        assertEquals(emoji, w.toString());
    }

    @Test
    public void testWriteCharacter_LineSeparator_AndParaSeparator() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter(String.valueOf((char) 0x2028) + (char) 0x2029);
        assertEquals("\\u2028\\u2029", w.toString());
    }

    @Test
    public void testWriteCharacter_NullString_WritesLiteralNullToken() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter((String) null);
        assertEquals("null", w.toString());
    }

    @Test
    public void testWriteCharacter_CharArray_PortionEscaping() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        char[] chars = "before\"middle\"after".toCharArray();
        w.writeCharacter(chars, 6, 8); // "\"middle\""
        assertEquals("\\\"middle\\\"", w.toString());
    }

    @Test
    public void testWriteCharacter_CharArray_InvalidBoundsThrows() {
        BufferedJsonWriter w = new BufferedJsonWriter();
        assertThrows(IndexOutOfBoundsException.class, () -> w.writeCharacter(new char[] { 'a' }, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> w.writeCharacter(new char[] { 'a' }, 0, 5));
    }

    @Test
    public void testWriteCharacter_SingleChar() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.writeCharacter('"');
        w.writeCharacter('\\');
        w.writeCharacter('A');
        assertEquals("\\\"\\\\A", w.toString());
    }

    @Test
    public void testPlainWriteIsNotEscaped() throws IOException {
        // The plain write(...) methods write verbatim — useful for emitting structural JSON.
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.write("\"key\":");
        assertEquals("\"key\":", w.toString());
    }

    @Test
    public void testConstructor_OutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BufferedJsonWriter w = new BufferedJsonWriter(baos)) {
            w.writeCharacter("a\"b");
            w.flush();
        }
        assertEquals("a\\\"b", baos.toString());
    }

    @Test
    public void testConstructor_Writer() throws IOException {
        StringWriter sw = new StringWriter();
        try (BufferedJsonWriter w = new BufferedJsonWriter(sw)) {
            w.writeCharacter("\\");
            w.flush();
        }
        assertEquals("\\\\", sw.toString());
    }

    @Test
    public void testWriteAfterClose_Throws() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.close();
        assertThrows(IOException.class, () -> w.writeCharacter("x"));
    }

    @Test
    public void testEmptyEscapedWritesAfterCloseThrow() throws IOException {
        BufferedJsonWriter w = new BufferedJsonWriter();
        w.close();

        assertThrows(IOException.class, () -> w.writeCharacter(new char[0]));
        assertThrows(IOException.class, () -> w.writeCharacter(""));
    }

    // ------------------------------------------------------------------ self-review 2026-09-07 (R1): P1-07
    // (unpaired surrogates escaped) landed in r9486 as five overrides in this class, but was exercised only
    // through the parser, which reaches writeCharacter(String) and writeCharacter(char). The two char[]
    // overloads and the String range overload carry the whole hand-written scan loop.

    private interface ReviewFixes20260907R1Write {
        void write(BufferedJsonWriter w) throws IOException;
    }

    private static String reviewFixes20260907R1Via(final ReviewFixes20260907R1Write w) throws IOException {
        final BufferedJsonWriter writer = new BufferedJsonWriter();
        w.write(writer);
        return writer.toString();
    }

    @Test
    public void reviewFixes20260907R1UnpairedSurrogatesEscapedByEveryOverload() throws IOException {
        // { input, expected output }: a lone surrogate becomes the six-character \\uXXXX escape (it has no
        // UTF-8 encoding, so the OutputStream-backed writer used to emit '?'), a well-formed pair is copied
        // through unchanged.
        final String[][] cases = { { "abc", "abc" }, { "a\"b", "a\\\"b" }, //
                { "\uD800", "\\ud800" }, { "\uDC00", "\\udc00" }, //
                { "a\uD800", "a\\ud800" }, { "\uD800a", "\\ud800a" }, //
                { "\uD800\uD800", "\\ud800\\ud800" }, { "\uDC00\uDC00", "\\udc00\\udc00" }, //
                { "\uDE00\uD83D", "\\ude00\\ud83d" }, // a reversed pair is two unpaired units
                { "\uD83D\uDE00", "\uD83D\uDE00" }, { "a\uD83D\uDE00b", "a\uD83D\uDE00b" }, //
                { "\uD83D\uDE00\uD800", "\uD83D\uDE00\\ud800" }, { "\uD800\uD83D\uDE00", "\\ud800\uD83D\uDE00" }, //
                { "\uD83D\uDE00\uD83D\uDE00", "\uD83D\uDE00\uD83D\uDE00" }, //
                { "", "" }, { "a\nb", "a\\nb" }, { "\uD800\"\uDC00", "\\ud800\\\"\\udc00" } };

        for (final String[] c : cases) {
            final String in = c[0];
            final String want = c[1];
            assertEquals(want, reviewFixes20260907R1Via(w -> w.writeCharacter(in)), "writeCharacter(String)");
            assertEquals(want, reviewFixes20260907R1Via(w -> w.writeCharacter(in, 0, in.length())), "writeCharacter(String,off,len)");
            assertEquals(want, reviewFixes20260907R1Via(w -> w.writeCharacter(in.toCharArray())), "writeCharacter(char[])");
            assertEquals(want, reviewFixes20260907R1Via(w -> w.writeCharacter(in.toCharArray(), 0, in.length())), "writeCharacter(char[],off,len)");
        }

        // a single char is always escaped when it is a surrogate: on its own it can never be half of a pair
        assertEquals("\\ud800", reviewFixes20260907R1Via(w -> w.writeCharacter('\uD800')));
        assertEquals("\\udc00", reviewFixes20260907R1Via(w -> w.writeCharacter('\uDC00')));
        assertEquals("A", reviewFixes20260907R1Via(w -> w.writeCharacter('A')));

        // a requested range that cuts a pair in half yields two escapes (each half is unpaired within the range)
        final String pair = "x\uD83D\uDE00y";
        assertEquals("x\uD83D\uDE00y", reviewFixes20260907R1Via(w -> w.writeCharacter(pair, 0, 4)));
        assertEquals("x\\ud83d", reviewFixes20260907R1Via(w -> w.writeCharacter(pair, 0, 2)));
        assertEquals("\\ude00y", reviewFixes20260907R1Via(w -> w.writeCharacter(pair, 2, 2)));
        assertEquals("x\\ud83d", reviewFixes20260907R1Via(w -> w.writeCharacter(pair.toCharArray(), 0, 2)));
        assertEquals("\\ude00y", reviewFixes20260907R1Via(w -> w.writeCharacter(pair.toCharArray(), 2, 2)));
        // only the requested range is scanned, at both ends
        assertEquals("\\ud800\\\"\\udc00", reviewFixes20260907R1Via(w -> w.writeCharacter("z\uD800\"\uDC00z".toCharArray(), 1, 3)));
        assertEquals("\\ud800\\\"\\udc00", reviewFixes20260907R1Via(w -> w.writeCharacter("z\uD800\"\uDC00z", 1, 3)));

        // the null / bounds contracts of the base class are unchanged by the overrides
        assertEquals("null", reviewFixes20260907R1Via(w -> w.writeCharacter((String) null)));
        assertEquals("null", reviewFixes20260907R1Via(w -> w.writeCharacter((String) null, 0, 4)));
        assertEquals("ul", reviewFixes20260907R1Via(w -> w.writeCharacter((String) null, 1, 2)));
        assertEquals("", reviewFixes20260907R1Via(w -> w.writeCharacter(new char[] { 'a' }, 1, 0)));
        assertThrows(IndexOutOfBoundsException.class, () -> reviewFixes20260907R1Via(w -> w.writeCharacter("abc", 1, 5)));
        assertThrows(IndexOutOfBoundsException.class, () -> reviewFixes20260907R1Via(w -> w.writeCharacter("abc".toCharArray(), 1, 5)));
        assertThrows(NullPointerException.class, () -> reviewFixes20260907R1Via(w -> w.writeCharacter((char[]) null)));

        // the OutputStream- and Writer-backed writers agree with the String-backed one
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (BufferedJsonWriter w = new BufferedJsonWriter(bos)) {
            w.writeCharacter("a\uD800b");
            w.flush();
        }
        assertEquals("a\\ud800b", bos.toString("UTF-8"));

        final StringWriter sw = new StringWriter();
        try (BufferedJsonWriter w = new BufferedJsonWriter(sw)) {
            w.writeCharacter("a\uDC00b".toCharArray());
            w.flush();
        }
        assertEquals("a\\udc00b", sw.toString());
    }

}
