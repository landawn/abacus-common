package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class EscapeUtil100Test extends TestBase {

    @Test
    public void testEscapeJava() {
        assertNull(EscapeUtil.escapeJava(null));
        assertEquals("", EscapeUtil.escapeJava(""));
        assertEquals("He didn't say, \\\"Stop!\\\"", EscapeUtil.escapeJava("He didn't say, \"Stop!\""));
        assertEquals("\\\\", EscapeUtil.escapeJava("\\"));
        assertEquals("\\n", EscapeUtil.escapeJava("\n"));
        assertEquals("\\r", EscapeUtil.escapeJava("\r"));
        assertEquals("\\t", EscapeUtil.escapeJava("\t"));
        assertEquals("\\b", EscapeUtil.escapeJava("\b"));
        assertEquals("\\f", EscapeUtil.escapeJava("\f"));
        assertEquals("\\u00A0", EscapeUtil.escapeJava("\u00A0"));
        assertEquals("\\u1234", EscapeUtil.escapeJava("\u1234"));
    }

    @Test
    public void testUnescapeJava() {
        assertNull(EscapeUtil.unescapeJava(null));
        assertEquals("", EscapeUtil.unescapeJava(""));
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeJava("He didn't say, \\\"Stop!\\\""));
        assertEquals("\\", EscapeUtil.unescapeJava("\\\\"));
        assertEquals("\n", EscapeUtil.unescapeJava("\\n"));
        assertEquals("\r", EscapeUtil.unescapeJava("\\r"));
        assertEquals("\t", EscapeUtil.unescapeJava("\\t"));
        assertEquals("\b", EscapeUtil.unescapeJava("\\b"));
        assertEquals("\f", EscapeUtil.unescapeJava("\\f"));
        assertEquals("\u00A0", EscapeUtil.unescapeJava("\\u00A0"));
        assertEquals("\u1234", EscapeUtil.unescapeJava("\\u1234"));
        assertEquals("test\u1234", EscapeUtil.unescapeJava("test\\u1234"));
        assertEquals("A", EscapeUtil.unescapeJava("\\101"));
        assertEquals("%", EscapeUtil.unescapeJava("\\45"));
    }

    @Test
    public void testEscapeEcmaScript() {
        assertNull(EscapeUtil.escapeEcmaScript(null));
        assertEquals("", EscapeUtil.escapeEcmaScript(""));
        assertEquals("He didn\\'t say, \\\"Stop!\\\"", EscapeUtil.escapeEcmaScript("He didn't say, \"Stop!\""));
        assertEquals("\\/", EscapeUtil.escapeEcmaScript("/"));
        assertEquals("\\\\", EscapeUtil.escapeEcmaScript("\\"));
        assertEquals("\\n", EscapeUtil.escapeEcmaScript("\n"));
        assertEquals("\\r", EscapeUtil.escapeEcmaScript("\r"));
        assertEquals("\\t", EscapeUtil.escapeEcmaScript("\t"));
    }

    @Test
    public void testUnescapeEcmaScript() {
        assertNull(EscapeUtil.unescapeEcmaScript(null));
        assertEquals("", EscapeUtil.unescapeEcmaScript(""));
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeEcmaScript("He didn\\'t say, \\\"Stop!\\\""));
        assertEquals("/", EscapeUtil.unescapeEcmaScript("\\/"));
        assertEquals("\\", EscapeUtil.unescapeEcmaScript("\\\\"));
        assertEquals("\n", EscapeUtil.unescapeEcmaScript("\\n"));
        assertEquals("\r", EscapeUtil.unescapeEcmaScript("\\r"));
        assertEquals("\t", EscapeUtil.unescapeEcmaScript("\\t"));
    }

    @Test
    public void testEscapeJson() {
        assertNull(EscapeUtil.escapeJson(null));
        assertEquals("", EscapeUtil.escapeJson(""));
        assertEquals("He didn't say, \\\"Stop!\\\"", EscapeUtil.escapeJson("He didn't say, \"Stop!\""));
        assertEquals("\\/", EscapeUtil.escapeJson("/"));
        assertEquals("\\\\", EscapeUtil.escapeJson("\\"));
        assertEquals("\\n", EscapeUtil.escapeJson("\n"));
        assertEquals("\\r", EscapeUtil.escapeJson("\r"));
        assertEquals("\\t", EscapeUtil.escapeJson("\t"));
        assertEquals("\\u00A0", EscapeUtil.escapeJson("\u00A0"));
    }

    @Test
    public void testUnescapeJson() {
        assertNull(EscapeUtil.unescapeJson(null));
        assertEquals("", EscapeUtil.unescapeJson(""));
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeJson("He didn't say, \\\"Stop!\\\""));
        assertEquals("/", EscapeUtil.unescapeJson("\\/"));
        assertEquals("\\", EscapeUtil.unescapeJson("\\\\"));
        assertEquals("\n", EscapeUtil.unescapeJson("\\n"));
        assertEquals("\r", EscapeUtil.unescapeJson("\\r"));
        assertEquals("\t", EscapeUtil.unescapeJson("\\t"));
        assertEquals("\u00A0", EscapeUtil.unescapeJson("\\u00A0"));
    }

    @Test
    public void testEscapeHtml3() {
        assertNull(EscapeUtil.escapeHtml3(null));
        assertEquals("", EscapeUtil.escapeHtml3(""));
        assertEquals("&lt;p&gt;Hello&lt;/p&gt;", EscapeUtil.escapeHtml3("<p>Hello</p>"));
        assertEquals("&quot;Hello&quot;", EscapeUtil.escapeHtml3("\"Hello\""));
        assertEquals("Tom &amp; Jerry", EscapeUtil.escapeHtml3("Tom & Jerry"));
        assertEquals("&copy; 2023", EscapeUtil.escapeHtml3("© 2023"));
        assertEquals("caf&eacute;", EscapeUtil.escapeHtml3("café"));
    }

    @Test
    public void testUnescapeHtml3() {
        assertNull(EscapeUtil.unescapeHtml3(null));
        assertEquals("", EscapeUtil.unescapeHtml3(""));
        assertEquals("<p>Hello</p>", EscapeUtil.unescapeHtml3("&lt;p&gt;Hello&lt;/p&gt;"));
        assertEquals("\"Hello\"", EscapeUtil.unescapeHtml3("&quot;Hello&quot;"));
        assertEquals("Tom & Jerry", EscapeUtil.unescapeHtml3("Tom &amp; Jerry"));
        assertEquals("© 2023", EscapeUtil.unescapeHtml3("&copy; 2023"));
        assertEquals("café", EscapeUtil.unescapeHtml3("caf&eacute;"));
        assertEquals("A", EscapeUtil.unescapeHtml3("&#65;"));
        assertEquals("A", EscapeUtil.unescapeHtml3("&#x41;"));
    }

    @Test
    public void testEscapeHtml4() {
        assertNull(EscapeUtil.escapeHtml4(null));
        assertEquals("", EscapeUtil.escapeHtml4(""));
        assertEquals("&lt;p&gt;Hello&lt;/p&gt;", EscapeUtil.escapeHtml4("<p>Hello</p>"));
        assertEquals("&quot;bread&quot; &amp; &quot;butter&quot;", EscapeUtil.escapeHtml4("\"bread\" & \"butter\""));
        assertEquals("&copy; 2023", EscapeUtil.escapeHtml4("© 2023"));
        assertEquals("&alpha;&beta;&gamma;", EscapeUtil.escapeHtml4("αβγ"));
        assertEquals("&euro;100", EscapeUtil.escapeHtml4("€100"));
    }

    @Test
    public void testUnescapeHtml4() {
        assertNull(EscapeUtil.unescapeHtml4(null));
        assertEquals("", EscapeUtil.unescapeHtml4(""));
        assertEquals("<p>Hello</p>", EscapeUtil.unescapeHtml4("&lt;p&gt;Hello&lt;/p&gt;"));
        assertEquals("\"bread\" & \"butter\"", EscapeUtil.unescapeHtml4("&quot;bread&quot; &amp; &quot;butter&quot;"));
        assertEquals("© 2023", EscapeUtil.unescapeHtml4("&copy; 2023"));
        assertEquals("αβγ", EscapeUtil.unescapeHtml4("&alpha;&beta;&gamma;"));
        assertEquals("€100", EscapeUtil.unescapeHtml4("&euro;100"));
        assertEquals("<Français>", EscapeUtil.unescapeHtml4("&lt;Fran&ccedil;ais&gt;"));
        assertEquals(">&zzzz;x", EscapeUtil.unescapeHtml4("&gt;&zzzz;x"));
    }

    @Test
    public void testEscapeXml10() {
        assertNull(EscapeUtil.escapeXml10(null));
        assertEquals("", EscapeUtil.escapeXml10(""));
        assertEquals("&quot;bread&quot; &amp; &quot;butter&quot;", EscapeUtil.escapeXml10("\"bread\" & \"butter\""));
        assertEquals("&lt;tag&gt;", EscapeUtil.escapeXml10("<tag>"));
        assertEquals("&apos;single quote&apos;", EscapeUtil.escapeXml10("'single quote'"));
        assertEquals("", EscapeUtil.escapeXml10("\u0000"));
        assertEquals("", EscapeUtil.escapeXml10("\u0001"));
        assertEquals("text", EscapeUtil.escapeXml10("text\u0000"));
        assertEquals("valid text", EscapeUtil.escapeXml10("valid text"));
        assertEquals("\t\n\r", EscapeUtil.escapeXml10("\t\n\r"));
    }

    @Test
    public void testEscapeXml11() {
        assertNull(EscapeUtil.escapeXml11(null));
        assertEquals("", EscapeUtil.escapeXml11(""));
        assertEquals("&quot;bread&quot; &amp; &quot;butter&quot;", EscapeUtil.escapeXml11("\"bread\" & \"butter\""));
        assertEquals("&lt;tag&gt;", EscapeUtil.escapeXml11("<tag>"));
        assertEquals("&apos;single quote&apos;", EscapeUtil.escapeXml11("'single quote'"));
        assertEquals("", EscapeUtil.escapeXml11("\u0000"));
        assertEquals("&#11;&#12;", EscapeUtil.escapeXml11("\u000B\u000C"));
        assertEquals("&#1;", EscapeUtil.escapeXml11("\u0001"));
    }

    @Test
    public void testUnescapeXml() {
        assertNull(EscapeUtil.unescapeXml(null));
        assertEquals("", EscapeUtil.unescapeXml(""));
        assertEquals("\"bread\" & \"butter\"", EscapeUtil.unescapeXml("&quot;bread&quot; &amp; &quot;butter&quot;"));
        assertEquals("<tag>", EscapeUtil.unescapeXml("&lt;tag&gt;"));
        assertEquals("'single quote'", EscapeUtil.unescapeXml("&apos;single quote&apos;"));
        assertEquals("A", EscapeUtil.unescapeXml("&#65;"));
        assertEquals("A", EscapeUtil.unescapeXml("&#x41;"));
        assertEquals("€", EscapeUtil.unescapeXml("&#8364;"));
        assertEquals("€", EscapeUtil.unescapeXml("&#x20AC;"));
    }

    @Test
    public void testEscapeCsv() {
        assertNull(EscapeUtil.escapeCsv(null));
        assertEquals("", EscapeUtil.escapeCsv(""));
        assertEquals("simple", EscapeUtil.escapeCsv("simple"));
        assertEquals("\"with,comma\"", EscapeUtil.escapeCsv("with,comma"));
        assertEquals("\"with\nnewline\"", EscapeUtil.escapeCsv("with\nnewline"));
        assertEquals("\"with\rcarriage\"", EscapeUtil.escapeCsv("with\rcarriage"));
        assertEquals("\"with \"\"quotes\"\"\"", EscapeUtil.escapeCsv("with \"quotes\""));
        assertEquals("\"comma, and \"\"quotes\"\"\"", EscapeUtil.escapeCsv("comma, and \"quotes\""));
    }

    @Test
    public void testUnescapeCsv() {
        assertNull(EscapeUtil.unescapeCsv(null));
        assertEquals("", EscapeUtil.unescapeCsv(""));
        assertEquals("simple", EscapeUtil.unescapeCsv("simple"));
        assertEquals("with,comma", EscapeUtil.unescapeCsv("\"with,comma\""));
        assertEquals("with\nnewline", EscapeUtil.unescapeCsv("\"with\nnewline\""));
        assertEquals("with\rcarriage", EscapeUtil.unescapeCsv("\"with\rcarriage\""));
        assertEquals("with \"quotes\"", EscapeUtil.unescapeCsv("\"with \"\"quotes\"\"\""));
        assertEquals("comma, and \"quotes\"", EscapeUtil.unescapeCsv("\"comma, and \"\"quotes\"\"\""));
        assertEquals("not quoted", EscapeUtil.unescapeCsv("not quoted"));
    }

    @Test
    public void testEscapeJavaWithSurrogatePairs() {
        String emoji = "Hello 😀 World";
        String escaped = EscapeUtil.escapeJava(emoji);
        assertEquals("Hello \\uD83D\\uDE00 World", escaped);
        assertEquals(emoji, EscapeUtil.unescapeJava(escaped));
    }

    @Test
    public void testHtmlEntityEdgeCases() {
        assertEquals("&lt;&lt;&gt;&gt;", EscapeUtil.escapeHtml4("<<>>"));
        assertEquals("<<>>", EscapeUtil.unescapeHtml4("&lt;&lt;&gt;&gt;"));

        String mixed = "Normal & <b>bold</b> text";
        String escaped = EscapeUtil.escapeHtml4(mixed);
        assertEquals("Normal &amp; &lt;b&gt;bold&lt;/b&gt; text", escaped);
        assertEquals(mixed, EscapeUtil.unescapeHtml4(escaped));
    }

    @Test
    public void testXmlControlCharacterHandling() {
        String withControls = "Text\u0000\u0001\u0002\u0003\u0004";
        String escaped10 = EscapeUtil.escapeXml10(withControls);
        assertEquals("Text", escaped10);

        String escaped11 = EscapeUtil.escapeXml11(withControls);
        assertTrue(escaped11.startsWith("Text"));
    }

    @Test
    public void testCsvComplexCases() {
        assertEquals("", EscapeUtil.escapeCsv(""));

        String complex = "Line 1\nLine 2\rLine 3,\"quoted\"";
        String escaped = EscapeUtil.escapeCsv(complex);
        assertEquals("\"Line 1\nLine 2\rLine 3,\"\"quoted\"\"\"", escaped);
        assertEquals(complex, EscapeUtil.unescapeCsv(escaped));
    }

    @Test
    public void testUnicodeHandling() {
        String unicode = "Hello 世界 🌍";

        String javaEscaped = EscapeUtil.escapeJava(unicode);
        assertEquals(unicode, EscapeUtil.unescapeJava(javaEscaped));

        String jsonEscaped = EscapeUtil.escapeJson(unicode);
        assertEquals(unicode, EscapeUtil.unescapeJson(jsonEscaped));

        String htmlEscaped = EscapeUtil.escapeHtml4(unicode);
        assertEquals(unicode, EscapeUtil.unescapeHtml4(htmlEscaped));
    }

    @Test
    public void testInvalidUnicodeEscape() {
        assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("\\uXXXX"));
    }

    @Test
    public void testIncompleteUnicodeEscape() {
        assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("\\u123"));
    }
}
