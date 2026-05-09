package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BufferedXmlWriterTest extends TestBase {

    @Test
    public void testGetCharQuotation_IsNullChar() {
        BufferedXmlWriter w = new BufferedXmlWriter();
        // Returning '\0' signals "no explicit quotation wrapping required".
        assertEquals(SK.CHAR_ZERO, w.getCharQuotation());
    }

    @Test
    public void testWriteCharacter_EscapesAmpersand() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.writeCharacter("AT&T");
        assertEquals("AT&amp;T", w.toString());
    }

    @Test
    public void testWriteCharacter_EscapesLessThanGreaterThan() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.writeCharacter("1 < 2 > 0");
        assertEquals("1 &lt; 2 &gt; 0", w.toString());
    }

    @Test
    public void testWriteCharacter_EscapesDoubleQuoteAndApostrophe() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.writeCharacter("\"'");
        assertEquals("&quot;&apos;", w.toString());
    }

    @Test
    public void testWriteCharacter_ControlChars_HexEscaped() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.writeCharacter("\t\n\r");
        // Tab=0x9, LF=0xa, CR=0xd
        assertEquals("&#x9;&#xa;&#xd;", w.toString());
    }

    @Test
    public void testWriteCharacter_NullChar_HexEscaped() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.writeCharacter("a" + (char) 0x00 + "b");
        assertEquals("a&#x0;b", w.toString());
    }

    @Test
    public void testWriteCharacter_DELIsHexEscaped() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.writeCharacter(String.valueOf((char) 0x7F));
        assertEquals("&#x7f;", w.toString());
    }

    @Test
    public void testWriteCharacter_NonAsciiAbove127_PassesThroughVerbatim() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        // Replacement table only covers 0..127; non-ASCII passes through.
        String s = String.valueOf((char) 0x00E9) + (char) 0x4E16; // e-acute, world
        w.writeCharacter(s);
        assertEquals(s, w.toString());
    }

    @Test
    public void testWriteCharacter_SurrogatePair_PreservedVerbatim() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        String emoji = new String(Character.toChars(0x1F600));
        w.writeCharacter(emoji);
        assertEquals(emoji, w.toString());
    }

    @Test
    public void testWriteCharacter_NullString_WritesLiteralNullToken() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.writeCharacter((String) null);
        assertEquals("null", w.toString());
    }

    @Test
    public void testWriteCharacter_CharArrayPortion() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        char[] chars = "abc&def".toCharArray();
        w.writeCharacter(chars, 2, 3); // "c&d"
        assertEquals("c&amp;d", w.toString());
    }

    @Test
    public void testPlainWriteIsNotEscaped() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.write("<root>");
        assertEquals("<root>", w.toString());
    }

    @Test
    public void testConstructor_OutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (BufferedXmlWriter w = new BufferedXmlWriter(baos)) {
            w.writeCharacter("<x>");
            w.flush();
        }
        assertEquals("&lt;x&gt;", baos.toString());
    }

    @Test
    public void testConstructor_Writer() throws IOException {
        StringWriter sw = new StringWriter();
        try (BufferedXmlWriter w = new BufferedXmlWriter(sw)) {
            w.writeCharacter("&");
            w.flush();
        }
        assertEquals("&amp;", sw.toString());
    }

    @Test
    public void testWriteAfterClose_Throws() throws IOException {
        BufferedXmlWriter w = new BufferedXmlWriter();
        w.close();
        assertThrows(IOException.class, () -> w.writeCharacter("x"));
    }
}
