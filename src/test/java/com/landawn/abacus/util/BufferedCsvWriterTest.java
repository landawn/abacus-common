package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BufferedCsvWriterTest extends TestBase {

    @Test
    public void testReplacementChars_QuoteIsDoubledByDefault() {
        // RFC 4180: a literal quote inside a quoted field is escaped by doubling it.
        assertArrayEquals("\"\"".toCharArray(), BufferedCsvWriter.REPLACEMENT_CHARS['"']);
    }

    @Test
    public void testReplacementChars_BackslashQuote_BackslashEscape() {
        // The alternate (non-RFC) backslash mode escapes " as \"
        assertArrayEquals("\\\"".toCharArray(), BufferedCsvWriter.REPLACEMENT_CHARS_BACK_SLASH['"']);
    }

    @Test
    public void testReplacementChars_PreserveCRLFTabFormFeedBackspace_AsLiteral() {
        // RFC 4180 fields may contain CR / LF / TAB literally inside a quoted field.
        // These must NOT be transformed.
        assertNull(BufferedCsvWriter.REPLACEMENT_CHARS['\r']);
        assertNull(BufferedCsvWriter.REPLACEMENT_CHARS['\n']);
        assertNull(BufferedCsvWriter.REPLACEMENT_CHARS['\t']);
        assertNull(BufferedCsvWriter.REPLACEMENT_CHARS['\b']);
        assertNull(BufferedCsvWriter.REPLACEMENT_CHARS['\f']);
        assertNull(BufferedCsvWriter.REPLACEMENT_CHARS['\\']);
    }

    @Test
    public void testReplacementChars_ControlCharsBelow32_HexEscaped_ExceptAllowed() {
        for (int i = 0; i < 32; i++) {
            char c = (char) i;
            if (c == '\r' || c == '\n' || c == '\t' || c == '\b' || c == '\f') {
                assertNull(BufferedCsvWriter.REPLACEMENT_CHARS[c],
                        "char 0x" + Integer.toHexString(i) + " must be left literal");
            } else {
                assertTrue(BufferedCsvWriter.REPLACEMENT_CHARS[c] != null,
                        "char 0x" + Integer.toHexString(i) + " must be escaped");
            }
        }
    }

    @Test
    public void testReplacementChars_LineParaSeparators_Escaped() {
        // U+2028 and U+2029 are escaped to remain JS-eval safe.
        assertArrayEquals("\\u2028".toCharArray(), BufferedCsvWriter.REPLACEMENT_CHARS[0x2028]);
        assertArrayEquals("\\u2029".toCharArray(), BufferedCsvWriter.REPLACEMENT_CHARS[0x2029]);
    }

    @Test
    public void testWriteCharacter_DoublesEmbeddedQuote() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.writeCharacter("She said \"hi\"");
        assertEquals("She said \"\"hi\"\"", w.toString());
    }

    @Test
    public void testWriteCharacter_PreservesCommaAndNewline_VerbatimWithinTheChar() throws IOException {
        // Note: BufferedCsvWriter only escapes characters char-by-char. The caller is
        // expected to wrap whole field in surrounding quotes when a delimiter/newline
        // appears (RFC 4180 record-wrap responsibility lies with the writer driver).
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.writeCharacter("a,b\nc\rd\te");
        assertEquals("a,b\nc\rd\te", w.toString());
    }

    @Test
    public void testWriteCharacter_PreservesBackslash() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.writeCharacter("c:\\path\\file.txt");
        assertEquals("c:\\path\\file.txt", w.toString());
    }

    @Test
    public void testWriteCharacter_NullCharIsHexEscaped() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.writeCharacter("a" + (char) 0x00 + "b");
        assertEquals("a\\u0000b", w.toString());
    }

    @Test
    public void testWriteCharacter_DELIsHexEscaped() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.writeCharacter(String.valueOf((char) 0x7F));
        assertEquals("\\u007f", w.toString());
    }

    @Test
    public void testIsBackSlash_DefaultIsFalse() {
        try {
            CsvUtil.resetEscapeCharForWrite();
            BufferedCsvWriter w = new BufferedCsvWriter();
            assertFalse(w.isBackSlash());
        } finally {
            CsvUtil.resetEscapeCharForWrite();
        }
    }

    @Test
    public void testIsBackSlash_TrueWhenConfigured() throws IOException {
        try {
            CsvUtil.setEscapeCharToBackSlashForWrite();
            BufferedCsvWriter w = new BufferedCsvWriter();
            assertTrue(w.isBackSlash());
            // Quote is escaped as \" in this mode.
            w.writeCharacter("\"x\"");
            assertEquals("\\\"x\\\"", w.toString());
        } finally {
            CsvUtil.resetEscapeCharForWrite();
        }
    }

    @Test
    public void testWriteCharacter_NullString_WritesLiteralNullToken() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.writeCharacter((String) null);
        assertEquals("null", w.toString());
    }

    @Test
    public void testWriteCharacter_CharArray_PortionEscaping() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        char[] chars = "ab\"cd\"ef".toCharArray();
        w.writeCharacter(chars, 2, 4); // "\"cd\""
        assertEquals("\"\"cd\"\"", w.toString());
    }

    @Test
    public void testPlainWriteIsNotEscaped() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.write("\"a\",\"b\"\n");
        assertEquals("\"a\",\"b\"\n", w.toString());
    }

    @Test
    public void testConstructor_OutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BufferedCsvWriter w = new BufferedCsvWriter(baos)) {
            w.writeCharacter("a\"b");
            w.flush();
        }
        assertEquals("a\"\"b", baos.toString());
    }

    @Test
    public void testConstructor_Writer() throws IOException {
        StringWriter sw = new StringWriter();
        try (BufferedCsvWriter w = new BufferedCsvWriter(sw)) {
            w.writeCharacter("hi\"there");
            w.flush();
        }
        assertEquals("hi\"\"there", sw.toString());
    }

    @Test
    public void testWriteAfterClose_Throws() throws IOException {
        BufferedCsvWriter w = new BufferedCsvWriter();
        w.close();
        assertThrows(IOException.class, () -> w.writeCharacter("x"));
    }
}
