package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.EscapeUtil.CharSequenceTranslator;

public class EscapeUtilTest extends TestBase {

    @Test
    public void testEscapeJava_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeJava("Hello World"));
    }

    @Test
    public void testEscapeJava_WithNewline() {
        Assertions.assertEquals("Hello\\nWorld", EscapeUtil.escapeJava("Hello\nWorld"));
    }

    @Test
    public void testEscapeJava_WithTab() {
        Assertions.assertEquals("Hello\\tWorld", EscapeUtil.escapeJava("Hello\tWorld"));
    }

    @Test
    public void testEscapeJava_WithQuotes() {
        Assertions.assertEquals("He said \\\"Hi\\\"", EscapeUtil.escapeJava("He said \"Hi\""));
    }

    @Test
    public void testEscapeJava_WithBackslash() {
        Assertions.assertEquals("C:\\\\temp\\\\file", EscapeUtil.escapeJava("C:\\temp\\file"));
    }

    @Test
    public void testEscapeJava_WithCarriageReturn() {
        Assertions.assertEquals("Line1\\rLine2", EscapeUtil.escapeJava("Line1\rLine2"));
    }

    @Test
    public void testEscapeJava_WithFormFeed() {
        Assertions.assertEquals("Page\\fBreak", EscapeUtil.escapeJava("Page\fBreak"));
    }

    @Test
    public void testEscapeJava_WithBackspace() {
        Assertions.assertEquals("Back\\bSpace", EscapeUtil.escapeJava("Back\bSpace"));
    }

    @Test
    public void testEscapeJavaWithSurrogatePairs() {
        String emoji = "Hello 😀 World";
        String escaped = EscapeUtil.escapeJava(emoji);
        assertEquals("Hello \\uD83D\\uDE00 World", escaped);
        assertEquals(emoji, EscapeUtil.unescapeJava(escaped));
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

    // =====================================================================
    // Additional tests for untested methods and edge-case code paths
    // Ordered to follow source declaration order
    // =====================================================================

    // --- escapeJava: Unicode escape for non-ASCII within BMP ---

    @Test
    public void testEscapeJava_NonAsciiCharacters() {
        // Characters outside printable ASCII (32-127) should be Unicode-escaped
        assertEquals("\\u00E9", EscapeUtil.escapeJava("\u00E9"));
        assertEquals("\\u4E16\\u754C", EscapeUtil.escapeJava("\u4E16\u754C")); // Chinese characters
    }

    @Test
    public void testEscapeJava_Null() {
        Assertions.assertNull(EscapeUtil.escapeJava(null));
    }

    @Test
    public void testEscapeJava_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeJava(""));
    }

    @Test
    public void testEscapeJava_WithSurrogatePairs() {
        String emoji = "😀";
        String escaped = EscapeUtil.escapeJava(emoji);
        Assertions.assertNotNull(escaped);
        String unescaped = EscapeUtil.unescapeJava(escaped);
        Assertions.assertEquals(emoji, unescaped);
    }

    @Test
    public void testEscapeJava() {
        assertNull(EscapeUtil.escapeJava(null), "Escaping null should return null");
        assertEquals("", EscapeUtil.escapeJava(""), "Escaping an empty string should return an empty string");
        assertEquals("hello", EscapeUtil.escapeJava("hello"), "String without special characters should remain unchanged");
        assertEquals("He didn't say, \\\"Stop!\\\"", EscapeUtil.escapeJava("He didn't say, \"Stop!\""), "Double quotes should be escaped");
        assertEquals("C:\\\\path\\\\to\\\\file", EscapeUtil.escapeJava("C:\\path\\to\\file"), "Backslashes should be escaped");
        assertEquals("\\n\\t\\b\\r\\f", EscapeUtil.escapeJava("\n\t\b\r\f"), "Control characters should be escaped");
        assertEquals("\\u00E9", EscapeUtil.escapeJava("\u00E9"), "Non-ASCII characters should be Unicode escaped");
        assertEquals("\\u0101", EscapeUtil.escapeJava("\u0101"), "Non-ASCII characters should be Unicode escaped");
    }

    @Test
    public void testEscapeEcmaScript_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeEcmaScript("Hello World"));
    }

    @Test
    public void testEscapeEcmaScript_WithForwardSlash() {
        String result = EscapeUtil.escapeEcmaScript("</script>");
        Assertions.assertTrue(result.contains("\\/"));
    }

    @Test
    public void testEscapeEcmaScript_WithDoubleQuote() {
        Assertions.assertEquals("He said \\\"Hi\\\"", EscapeUtil.escapeEcmaScript("He said \"Hi\""));
    }

    @Test
    public void testEscapeEcmaScript_AllSpecialChars() {
        String result = EscapeUtil.escapeEcmaScript("'\"\\/");
        Assertions.assertTrue(result.contains("\\'"));
        Assertions.assertTrue(result.contains("\\\""));
        Assertions.assertTrue(result.contains("\\/"));
    }

    // --- escapeEcmaScript: backslash escaping ---

    @Test
    public void testEscapeEcmaScript_WithBackslash() {
        assertEquals("C:\\\\temp", EscapeUtil.escapeEcmaScript("C:\\temp"));
    }

    @Test
    public void testEscapeEcmaScript_WithSingleQuote() {
        Assertions.assertEquals("Don\\'t", EscapeUtil.escapeEcmaScript("Don't"));
    }

    @Test
    public void testEscapeEcmaScript_Null() {
        Assertions.assertNull(EscapeUtil.escapeEcmaScript(null));
    }

    @Test
    public void testEscapeEcmaScript_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeEcmaScript(""));
    }

    @Test
    public void testEscapeEcmaScript() {
        assertNull(EscapeUtil.escapeEcmaScript(null));
        assertEquals("", EscapeUtil.escapeEcmaScript(""));
        assertEquals("He didn\\'t say, \\\"Stop!\\\"", EscapeUtil.escapeEcmaScript("He didn't say, \"Stop!\""), "Single and double quotes should be escaped");
        assertEquals("<\\/script>", EscapeUtil.escapeEcmaScript("</script>"), "Forward slash should be escaped");
    }

    @Test
    public void testEscapeJson_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeJson("Hello World"));
    }

    @Test
    public void testEscapeJson_WithNewline() {
        Assertions.assertEquals("Hello\\nWorld", EscapeUtil.escapeJson("Hello\nWorld"));
    }

    @Test
    public void testEscapeJson_WithForwardSlash() {
        String result = EscapeUtil.escapeJson("path/to/file");
        Assertions.assertTrue(result.contains("\\/"));
    }

    @Test
    public void testEscapeJson_WithQuotes() {
        Assertions.assertEquals("Say \\\"Hello\\\"", EscapeUtil.escapeJson("Say \"Hello\""));
    }

    @Test
    public void testEscapeJson_ControlCharacters() {
        String result = EscapeUtil.escapeJson("Hello\t\n\r\fWorld");
        Assertions.assertTrue(result.contains("\\t"));
        Assertions.assertTrue(result.contains("\\n"));
        Assertions.assertTrue(result.contains("\\r"));
    }

    // --- escapeJson: backslash escaping ---

    @Test
    public void testEscapeJson_WithBackslash() {
        assertEquals("C:\\\\temp", EscapeUtil.escapeJson("C:\\temp"));
    }

    @Test
    public void testEscapeJson_Null() {
        Assertions.assertNull(EscapeUtil.escapeJson(null));
    }

    @Test
    public void testEscapeJson_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeJson(""));
    }

    @Test
    public void testEscapeJson() {
        assertNull(EscapeUtil.escapeJson(null));
        assertEquals("", EscapeUtil.escapeJson(""));
        assertEquals("He didn't say, \\\"Stop!\\\"", EscapeUtil.escapeJson("He didn't say, \"Stop!\""), "Double quotes should be escaped");
        assertEquals("He didn't say", EscapeUtil.escapeJson("He didn't say"), "Single quotes should not be escaped in JSON");
        assertEquals("<\\/script>", EscapeUtil.escapeJson("</script>"), "Forward slash should be escaped in JSON");
        assertEquals("\\b\\f\\n\\r\\t", EscapeUtil.escapeJson("\b\f\n\r\t"), "JSON control characters should be escaped");
    }

    @Test
    public void testUnescapeJava_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.unescapeJava("Hello World"));
    }

    @Test
    public void testUnescapeJava_WithNewline() {
        Assertions.assertEquals("Hello\nWorld", EscapeUtil.unescapeJava("Hello\\nWorld"));
    }

    @Test
    public void testUnescapeJava_WithTab() {
        Assertions.assertEquals("Hello\tWorld", EscapeUtil.unescapeJava("Hello\\tWorld"));
    }

    @Test
    public void testUnescapeJava_WithQuotes() {
        Assertions.assertEquals("He said \"Hi\"", EscapeUtil.unescapeJava("He said \\\"Hi\\\""));
    }

    @Test
    public void testUnescapeJava_WithBackslash() {
        Assertions.assertEquals("C:\\temp\\file", EscapeUtil.unescapeJava("C:\\\\temp\\\\file"));
    }

    @Test
    public void testUnescapeJava_WithUnicodeEscape() {
        Assertions.assertEquals("Hello", EscapeUtil.unescapeJava("\\u0048\\u0065\\u006C\\u006C\\u006F"));
    }

    @Test
    public void testUnescapeJava_WithOctalEscape() {
        Assertions.assertEquals("A", EscapeUtil.unescapeJava("\\101"));
    }

    // --- Round-trip tests ---

    @Test
    public void testEscapeUnescapeJava_RoundTrip() {
        String original = "Hello\nWorld\t\"quoted\" \\ \u00E9 \u4E16\u754C";
        assertEquals(original, EscapeUtil.unescapeJava(EscapeUtil.escapeJava(original)));
    }

    @Test
    public void testUnescapeJava_Null() {
        Assertions.assertNull(EscapeUtil.unescapeJava(null));
    }

    @Test
    public void testUnescapeJava_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeJava(""));
    }

    @Test
    public void testUnescapeJava_TrailingBackslash() {
        String result = EscapeUtil.unescapeJava("test\\");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testUnescapeJava() {
        assertNull(EscapeUtil.unescapeJava(null), "Unescaping null should return null");
        assertEquals("", EscapeUtil.unescapeJava(""), "Unescaping an empty string should return an empty string");
        assertEquals("hello", EscapeUtil.unescapeJava("hello"), "String without escapes should remain unchanged");
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeJava("He didn't say, \\\"Stop!\\\""), "Escaped double quotes should be unescaped");
        assertEquals("C:\\path\\to\\file", EscapeUtil.unescapeJava("C:\\\\path\\\\to\\\\file"), "Escaped backslashes should be unescaped");
        assertEquals("\n\t\b\r\f", EscapeUtil.unescapeJava("\\n\\t\\b\\r\\f"), "Escaped control characters should be unescaped");
        assertEquals("\u00E9", EscapeUtil.unescapeJava("\\u00E9"), "Unicode escapes should be unescaped");
        assertEquals("'", EscapeUtil.unescapeJava("\\'"), "Escaped single quote should be unescaped");
        assertEquals("\u0025", EscapeUtil.unescapeJava("\\45"), "Octal escapes should be unescaped");
        assertEquals("test", EscapeUtil.unescapeJava("test\\"), "A single trailing backslash should be removed");
        assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("\\u123G"),
                "Invalid hex character in Unicode escape should throw exception");
        assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("\\u123"), "Incomplete Unicode escape should throw exception");
    }

    @Test
    public void testInvalidUnicodeEscape() {
        assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("\\uXXXX"));
    }

    @Test
    public void testIncompleteUnicodeEscape() {
        assertThrows(IllegalArgumentException.class, () -> EscapeUtil.unescapeJava("\\u123"));
    }

    @Test
    public void testUnescapeEcmaScript_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.unescapeEcmaScript("Hello World"));
    }

    @Test
    public void testUnescapeEcmaScript_WithForwardSlash() {
        Assertions.assertEquals("</script>", EscapeUtil.unescapeEcmaScript("<\\/script>"));
    }

    @Test
    public void testEscapeUnescapeEcmaScript_RoundTrip() {
        String original = "Don't stop / \"go\" \\ \n\t";
        assertEquals(original, EscapeUtil.unescapeEcmaScript(EscapeUtil.escapeEcmaScript(original)));
    }

    @Test
    public void testUnescapeEcmaScript_WithSingleQuote() {
        Assertions.assertEquals("Don't", EscapeUtil.unescapeEcmaScript("Don\\'t"));
    }

    @Test
    public void testUnescapeEcmaScript_Null() {
        Assertions.assertNull(EscapeUtil.unescapeEcmaScript(null));
    }

    @Test
    public void testUnescapeEcmaScript_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeEcmaScript(""));
    }

    @Test
    public void testUnescapeEcmaScript() {
        assertNull(EscapeUtil.unescapeEcmaScript(null));
        assertEquals("", EscapeUtil.unescapeEcmaScript(""));
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeEcmaScript("He didn\\'t say, \\\"Stop!\\\""));
        assertEquals("</script>", EscapeUtil.unescapeEcmaScript("<\\/script>"));
    }

    @Test
    public void testUnescapeJson_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.unescapeJson("Hello World"));
    }

    @Test
    public void testUnescapeJson_WithNewline() {
        Assertions.assertEquals("Hello\nWorld", EscapeUtil.unescapeJson("Hello\\nWorld"));
    }

    @Test
    public void testUnescapeJson_WithForwardSlash() {
        Assertions.assertEquals("path/to/file", EscapeUtil.unescapeJson("path\\/to\\/file"));
    }

    @Test
    public void testUnescapeJson_WithQuotes() {
        Assertions.assertEquals("Say \"Hello\"", EscapeUtil.unescapeJson("Say \\\"Hello\\\""));
    }

    @Test
    public void testUnescapeJson_AllEscapeSequences() {
        String result = EscapeUtil.unescapeJson("\\t\\n\\r\\f\\b\\\\\\/\\\"");
        Assertions.assertEquals("\t\n\r\f\b\\/\"", result);
    }

    // --- unescapeJson: backslash within content ---

    @Test
    public void testUnescapeJson_WithBackslash() {
        assertEquals("C:\\temp", EscapeUtil.unescapeJson("C:\\\\temp"));
    }

    @Test
    public void testUnescapeJson_Null() {
        Assertions.assertNull(EscapeUtil.unescapeJson(null));
    }

    @Test
    public void testUnescapeJson_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeJson(""));
    }

    @Test
    public void testUnescapeJson() {
        assertNull(EscapeUtil.unescapeJson(null));
        assertEquals("", EscapeUtil.unescapeJson(""));
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeJson("He didn't say, \\\"Stop!\\\""));
        assertEquals("</script>", EscapeUtil.unescapeJson("<\\/script>"));
    }

    @Test
    public void testEscapeHtml4_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeHtml4("Hello World"));
    }

    @Test
    public void testEscapeHtml4_WithLessThan() {
        Assertions.assertEquals("&lt;p&gt;", EscapeUtil.escapeHtml4("<p>"));
    }

    @Test
    public void testEscapeHtml4_WithAmpersand() {
        Assertions.assertEquals("bread &amp; butter", EscapeUtil.escapeHtml4("bread & butter"));
    }

    @Test
    public void testEscapeHtml4_WithQuotes() {
        Assertions.assertEquals("&quot;quoted&quot;", EscapeUtil.escapeHtml4("\"quoted\""));
    }

    @Test
    public void testEscapeHtml4_WithGreaterThan() {
        Assertions.assertEquals("&gt;", EscapeUtil.escapeHtml4(">"));
    }

    @Test
    public void testEscapeHtml4_AllBasicEntities() {
        String result = EscapeUtil.escapeHtml4("<>&\"");
        Assertions.assertEquals("&lt;&gt;&amp;&quot;", result);
    }

    // --- escapeHtml4: ISO-8859-1 entities ---

    @Test
    public void testEscapeHtml4_ISO8859_1() {
        assertEquals("&copy;", EscapeUtil.escapeHtml4("\u00A9"));
        assertEquals("&reg;", EscapeUtil.escapeHtml4("\u00AE"));
        assertEquals("&nbsp;", EscapeUtil.escapeHtml4("\u00A0"));
    }

    @Test
    public void testEscapeHtml4_Null() {
        Assertions.assertNull(EscapeUtil.escapeHtml4(null));
    }

    @Test
    public void testEscapeHtml4_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeHtml4(""));
    }

    @Test
    public void testEscapeHtml4() {
        assertNull(EscapeUtil.escapeHtml4(null));
        assertEquals("", EscapeUtil.escapeHtml4(""));
        assertEquals("hello", EscapeUtil.escapeHtml4("hello"));
        assertEquals("&lt;tag&gt;", EscapeUtil.escapeHtml4("<tag>"));
        assertEquals("&quot;bread&quot; &amp; &quot;butter&quot;", EscapeUtil.escapeHtml4("\"bread\" & \"butter\""));
        assertEquals("'", EscapeUtil.escapeHtml4("'"), "Apostrophe is not escaped in HTML4");
        assertEquals("&copy;", EscapeUtil.escapeHtml4("©"), "ISO-8859-1 characters should be escaped");
        assertEquals("&euro;", EscapeUtil.escapeHtml4("€"), "HTML4 extended entities should be escaped");
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
    public void testEscapeHtml3_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeHtml3("Hello World"));
    }

    @Test
    public void testEscapeHtml3_WithLessThan() {
        Assertions.assertEquals("&lt;div&gt;", EscapeUtil.escapeHtml3("<div>"));
    }

    @Test
    public void testEscapeHtml3_WithAmpersand() {
        Assertions.assertEquals("A &amp; B", EscapeUtil.escapeHtml3("A & B"));
    }

    @Test
    public void testEscapeHtml3_Null() {
        Assertions.assertNull(EscapeUtil.escapeHtml3(null));
    }

    @Test
    public void testEscapeHtml3_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeHtml3(""));
    }

    @Test
    public void testEscapeHtml3() {
        assertNull(EscapeUtil.escapeHtml3(null));
        assertEquals("", EscapeUtil.escapeHtml3(""));
        assertEquals("&lt;tag&gt;", EscapeUtil.escapeHtml3("<tag>"));
        assertEquals("&copy;", EscapeUtil.escapeHtml3("©"), "ISO-8859-1 characters should be escaped");
        assertEquals("€", EscapeUtil.escapeHtml3("€"), "HTML4 extended entities should not be escaped in HTML3");
    }

    @Test
    public void testUnescapeHtml4_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.unescapeHtml4("Hello World"));
    }

    @Test
    public void testUnescapeHtml4_WithLessThan() {
        Assertions.assertEquals("<p>", EscapeUtil.unescapeHtml4("&lt;p&gt;"));
    }

    @Test
    public void testUnescapeHtml4_WithAmpersand() {
        Assertions.assertEquals("bread & butter", EscapeUtil.unescapeHtml4("bread &amp; butter"));
    }

    @Test
    public void testUnescapeHtml4_WithQuotes() {
        Assertions.assertEquals("\"quoted\"", EscapeUtil.unescapeHtml4("&quot;quoted&quot;"));
    }

    @Test
    public void testUnescapeHtml4_WithNumericEntity() {
        Assertions.assertEquals("A", EscapeUtil.unescapeHtml4("&#65;"));
    }

    @Test
    public void testUnescapeHtml4_WithHexEntity() {
        Assertions.assertEquals("A", EscapeUtil.unescapeHtml4("&#x41;"));
    }

    @Test
    public void testUnescapeHtml4_WithExtendedEntity() {
        Assertions.assertEquals("©", EscapeUtil.unescapeHtml4("&copy;"));
    }

    @Test
    public void testUnescapeHtml4_UnrecognizedEntity() {
        Assertions.assertEquals("&unknown;", EscapeUtil.unescapeHtml4("&unknown;"));
    }

    @Test
    public void testUnescapeHtml4_DecimalEntity_NoHexDigits() {
        Assertions.assertEquals("&#65", EscapeUtil.unescapeHtml4("&#65"));
    }

    // --- unescapeHtml4: ISO-8859-1 entities round-trip ---

    @Test
    public void testUnescapeHtml4_ISO8859_1_RoundTrip() {
        assertEquals("\u00A9", EscapeUtil.unescapeHtml4("&copy;"));
        assertEquals("\u00AE", EscapeUtil.unescapeHtml4("&reg;"));
        assertEquals("\u00A0", EscapeUtil.unescapeHtml4("&nbsp;"));
    }

    @Test
    public void testEscapeUnescapeHtml4_RoundTrip() {
        String original = "<div class=\"test\">A & B</div>";
        assertEquals(original, EscapeUtil.unescapeHtml4(EscapeUtil.escapeHtml4(original)));
    }

    @Test
    public void testUnescapeHtml4_Null() {
        Assertions.assertNull(EscapeUtil.unescapeHtml4(null));
    }

    @Test
    public void testUnescapeHtml4_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeHtml4(""));
    }

    @Test
    public void testUnescapeHtml4_EmptyEntity() {
        Assertions.assertEquals("&#;", EscapeUtil.unescapeHtml4("&#;"));
    }

    @Test
    public void testUnescapeHtml4_EmptyHexEntity() {
        Assertions.assertEquals("&#x;", EscapeUtil.unescapeHtml4("&#x;"));
    }

    @Test
    public void testUnescapeHtml4() {
        assertNull(EscapeUtil.unescapeHtml4(null));
        assertEquals("", EscapeUtil.unescapeHtml4(""));
        assertEquals("<tag>", EscapeUtil.unescapeHtml4("&lt;tag&gt;"));
        assertEquals("\"bread\" & \"butter\"", EscapeUtil.unescapeHtml4("&quot;bread&quot; &amp; &quot;butter&quot;"));
        assertEquals("©", EscapeUtil.unescapeHtml4("&copy;"));
        assertEquals("€", EscapeUtil.unescapeHtml4("&euro;"));
        assertEquals("<", EscapeUtil.unescapeHtml4("&#60;"), "Decimal numeric entities should be unescaped");
        assertEquals("€", EscapeUtil.unescapeHtml4("&#x20AC;"), "Hexadecimal numeric entities should be unescaped");
        assertEquals("&zzzz;", EscapeUtil.unescapeHtml4("&zzzz;"), "Unrecognized entities should be left alone");
        assertEquals("&#60", EscapeUtil.unescapeHtml4("&#60"), "Numeric entities without a semicolon should not be unescaped by default");
    }

    @Test
    public void testUnescapeHtml3_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.unescapeHtml3("Hello World"));
    }

    @Test
    public void testUnescapeHtml3_WithLessThan() {
        Assertions.assertEquals("<div>", EscapeUtil.unescapeHtml3("&lt;div&gt;"));
    }

    @Test
    public void testUnescapeHtml3_WithAmpersand() {
        Assertions.assertEquals("A & B", EscapeUtil.unescapeHtml3("A &amp; B"));
    }

    @Test
    public void testUnescapeHtml3_Null() {
        Assertions.assertNull(EscapeUtil.unescapeHtml3(null));
    }

    @Test
    public void testUnescapeHtml3_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeHtml3(""));
    }

    @Test
    public void testUnescapeHtml3() {
        assertNull(EscapeUtil.unescapeHtml3(null));
        assertEquals("", EscapeUtil.unescapeHtml3(""));
        assertEquals("<tag>", EscapeUtil.unescapeHtml3("&lt;tag&gt;"));
        assertEquals("©", EscapeUtil.unescapeHtml3("&copy;"));
        assertEquals("&euro;", EscapeUtil.unescapeHtml3("&euro;"), "HTML4 extended entities should not be unescaped in HTML3");
    }

    @Test
    public void testEscapeXml10_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeXml10("Hello World"));
    }

    @Test
    public void testEscapeXml10_WithLessThan() {
        Assertions.assertEquals("&lt;root&gt;", EscapeUtil.escapeXml10("<root>"));
    }

    @Test
    public void testEscapeXml10_WithAmpersand() {
        Assertions.assertEquals("A &amp; B", EscapeUtil.escapeXml10("A & B"));
    }

    @Test
    public void testEscapeXml10_WithApostrophe() {
        Assertions.assertEquals("It&apos;s", EscapeUtil.escapeXml10("It's"));
    }

    @Test
    public void testEscapeXml10_WithQuotes() {
        Assertions.assertEquals("&quot;cool&quot;", EscapeUtil.escapeXml10("\"cool\""));
    }

    @Test
    public void testEscapeXml10_RemovesUnpairedSurrogates() {
        char highSurrogate = '\uD800';
        String input = "test" + highSurrogate + "end";
        String result = EscapeUtil.escapeXml10(input);
        Assertions.assertFalse(result.contains(String.valueOf(highSurrogate)));
    }

    @Test
    public void testXmlControlCharacterHandling() {
        String withControls = "Text\u0000\u0001\u0002\u0003\u0004";
        String escaped10 = EscapeUtil.escapeXml10(withControls);
        assertEquals("Text", escaped10);

        String escaped11 = EscapeUtil.escapeXml11(withControls);
        assertTrue(escaped11.startsWith("Text"));
    }

    // --- escapeXml10: numeric escaping for control chars in range 0x7f-0x84 and 0x86-0x9f ---

    @Test
    public void testEscapeXml10_NumericEscapeControlRange() {
        // Characters in 0x7f-0x84 and 0x86-0x9f should be numeric-escaped
        String result = EscapeUtil.escapeXml10("\u007F");
        assertTrue(result.contains("&#"));
    }

    @Test
    public void testEscapeXml10_Null() {
        Assertions.assertNull(EscapeUtil.escapeXml10(null));
    }

    @Test
    public void testEscapeXml10_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeXml10(""));
    }

    @Test
    public void testEscapeXml10_WithInvalidControlChars() {
        String result = EscapeUtil.escapeXml10("Hello\u0000World");
        Assertions.assertFalse(result.contains("\u0000"));
    }

    @Test
    public void testEscapeXml10() {
        assertNull(EscapeUtil.escapeXml10(null));
        assertEquals("", EscapeUtil.escapeXml10(""));
        assertEquals("&lt;test&gt;", EscapeUtil.escapeXml10("<test>"));
        assertEquals("&quot;bread&quot; &amp; &apos;butter&apos;", EscapeUtil.escapeXml10("\"bread\" & 'butter'"));
        assertEquals("", EscapeUtil.escapeXml10("\u0000\u0001\u0008"), "Invalid XML 1.0 characters should be removed");
        assertEquals("\u0085", EscapeUtil.escapeXml10("\u0085"), "Certain control characters should be escaped as numeric entities");
        assertEquals("\t\n\r", EscapeUtil.escapeXml10("\t\n\r"), "Valid XML 1.0 control characters should pass through");
        assertEquals("", EscapeUtil.escapeXml10("\uD800"), "Unpaired high surrogate should be removed");
        assertEquals("a", EscapeUtil.escapeXml10("\uD800a"), "Unpaired high surrogate should be removed, leaving following character");
    }

    @Test
    public void testEscapeXml11_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeXml11("Hello World"));
    }

    @Test
    public void testEscapeXml11_WithLessThan() {
        Assertions.assertEquals("&lt;tag&gt;", EscapeUtil.escapeXml11("<tag>"));
    }

    @Test
    public void testEscapeXml11_WithAmpersand() {
        Assertions.assertEquals("A &amp; B", EscapeUtil.escapeXml11("A & B"));
    }

    // --- escapeXml11: control chars are numeric-escaped ---

    @Test
    public void testEscapeXml11_ControlCharsNumericEscaped() {
        // Characters 0x1-0x8 should be numeric-escaped in XML 1.1
        assertEquals("&#2;", EscapeUtil.escapeXml11("\u0002"));
        assertEquals("&#8;", EscapeUtil.escapeXml11("\u0008"));
    }

    @Test
    public void testEscapeXml11_Null() {
        Assertions.assertNull(EscapeUtil.escapeXml11(null));
    }

    @Test
    public void testEscapeXml11_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeXml11(""));
    }

    @Test
    public void testEscapeXml11_WithNullByte() {
        String result = EscapeUtil.escapeXml11("Hello\u0000World");
        Assertions.assertFalse(result.contains("\u0000"));
    }

    @Test
    public void testEscapeXml11() {
        assertNull(EscapeUtil.escapeXml11(null));
        assertEquals("", EscapeUtil.escapeXml11(""));
        assertEquals("&lt;test&gt;", EscapeUtil.escapeXml11("<test>"));
        assertEquals("", EscapeUtil.escapeXml11("\u0000"), "Null character should be removed");
        assertEquals("&#1;", EscapeUtil.escapeXml11("\u0001"), "Control characters invalid in XML 1.0 but valid in 1.1 should be escaped");
        assertEquals("&#11;", EscapeUtil.escapeXml11("\u000b"), "Control characters should be escaped");
        assertEquals("\t", EscapeUtil.escapeXml11("\t"), "Valid control characters should pass through");
    }

    // --- escapeXml11: 0xFFFE and 0xFFFF should be removed ---

    @Test
    public void testEscapeXml11_RemovesInvalidChars() {
        assertEquals("", EscapeUtil.escapeXml11("\uFFFE"));
        assertEquals("", EscapeUtil.escapeXml11("\uFFFF"));
    }

    @Test
    public void testUnescapeXml_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.unescapeXml("Hello World"));
    }

    @Test
    public void testUnescapeXml_WithLessThan() {
        Assertions.assertEquals("<root>", EscapeUtil.unescapeXml("&lt;root&gt;"));
    }

    @Test
    public void testUnescapeXml_WithAmpersand() {
        Assertions.assertEquals("A & B", EscapeUtil.unescapeXml("A &amp; B"));
    }

    @Test
    public void testUnescapeXml_WithApostrophe() {
        Assertions.assertEquals("'quoted'", EscapeUtil.unescapeXml("&apos;quoted&apos;"));
    }

    @Test
    public void testUnescapeXml_WithQuotes() {
        Assertions.assertEquals("\"text\"", EscapeUtil.unescapeXml("&quot;text&quot;"));
    }

    @Test
    public void testUnescapeXml_WithNumericEntity() {
        Assertions.assertEquals("A", EscapeUtil.unescapeXml("&#65;"));
    }

    @Test
    public void testUnescapeXml_WithHexEntity() {
        Assertions.assertEquals("A", EscapeUtil.unescapeXml("&#x41;"));
    }

    @Test
    public void testUnescapeXml_AllBasicEntities() {
        String result = EscapeUtil.unescapeXml("&lt;&gt;&amp;&quot;&apos;");
        Assertions.assertEquals("<>&\"'", result);
    }

    @Test
    public void testEscapeUnescapeXml10_RoundTrip() {
        String original = "<root attr=\"val\">'text' & more</root>";
        assertEquals(original, EscapeUtil.unescapeXml(EscapeUtil.escapeXml10(original)));
    }

    @Test
    public void testUnescapeXml_Null() {
        Assertions.assertNull(EscapeUtil.unescapeXml(null));
    }

    @Test
    public void testUnescapeXml_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeXml(""));
    }

    @Test
    public void testUnescapeXml() {
        assertNull(EscapeUtil.unescapeXml(null));
        assertEquals("", EscapeUtil.unescapeXml(""));
        assertEquals("<test>", EscapeUtil.unescapeXml("&lt;test&gt;"));
        assertEquals("\"bread\" & 'butter'", EscapeUtil.unescapeXml("&quot;bread&quot; &amp; &apos;butter&apos;"), "Basic 5 XML entities should be unescaped");
        assertEquals("<", EscapeUtil.unescapeXml("&#60;"), "Numeric entities should be unescaped");
        assertEquals("&nbsp;", EscapeUtil.unescapeXml("&nbsp;"), "Non-basic XML entities should not be unescaped");
    }

    @Test
    public void testEscapeCsv_BasicString() {
        Assertions.assertEquals("simple", EscapeUtil.escapeCsv("simple"));
    }

    @Test
    public void testEscapeCsv_WithComma() {
        Assertions.assertEquals("\"hello,world\"", EscapeUtil.escapeCsv("hello,world"));
    }

    @Test
    public void testEscapeCsv_WithQuote() {
        Assertions.assertEquals("\"say \"\"hi\"\"\"", EscapeUtil.escapeCsv("say \"hi\""));
    }

    @Test
    public void testEscapeCsv_WithNewline() {
        String result = EscapeUtil.escapeCsv("line1\nline2");
        Assertions.assertTrue(result.startsWith("\"") && result.endsWith("\""));
    }

    @Test
    public void testEscapeCsv_WithCarriageReturn() {
        String result = EscapeUtil.escapeCsv("line1\rline2");
        Assertions.assertTrue(result.startsWith("\"") && result.endsWith("\""));
    }

    @Test
    public void testEscapeCsv_NoSpecialChars() {
        Assertions.assertEquals("normal text", EscapeUtil.escapeCsv("normal text"));
    }

    // --- escapeCsv: string with only a quote ---

    @Test
    public void testEscapeCsv_OnlyQuote() {
        assertEquals("\"\"\"\"", EscapeUtil.escapeCsv("\""));
    }

    @Test
    public void testEscapeCsv_Null() {
        Assertions.assertNull(EscapeUtil.escapeCsv(null));
    }

    @Test
    public void testEscapeCsv_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeCsv(""));
    }

    @Test
    public void testEscapeCsv_MultipleQuotes() {
        String input = "He said \"Hello\" and \"Goodbye\"";
        String result = EscapeUtil.escapeCsv(input);
        Assertions.assertTrue(result.contains("\"\""));
    }

    @Test
    public void testEscapeCsv() {
        assertNull(EscapeUtil.escapeCsv(null));
        assertEquals("", EscapeUtil.escapeCsv(""));
        assertEquals("abc", EscapeUtil.escapeCsv("abc"), "String without special CSV chars should be unchanged");
        assertEquals("\"a,b\"", EscapeUtil.escapeCsv("a,b"), "String with comma should be quoted");
        assertEquals("\"a\"\"b\"", EscapeUtil.escapeCsv("a\"b"), "String with double quote should be quoted and the quote escaped");
        assertEquals("\"a\nb\"", EscapeUtil.escapeCsv("a\nb"), "String with newline should be quoted");
        assertEquals("\"a\rb\"", EscapeUtil.escapeCsv("a\rb"), "String with carriage return should be quoted");
        assertEquals("\"a,\"\"b\n\"", EscapeUtil.escapeCsv("a,\"b\n"), "String with multiple special chars should be correctly escaped");
        assertEquals("\"\"\"abc\"\"\"", EscapeUtil.escapeCsv("\"abc\""), "String containing quotes should have them escaped and be quoted");
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
    public void testUnescapeCsv_BasicString() {
        Assertions.assertEquals("simple", EscapeUtil.unescapeCsv("simple"));
    }

    @Test
    public void testUnescapeCsv_WithComma() {
        Assertions.assertEquals("hello,world", EscapeUtil.unescapeCsv("\"hello,world\""));
    }

    @Test
    public void testUnescapeCsv_WithQuote() {
        Assertions.assertEquals("say \"hi\"", EscapeUtil.unescapeCsv("\"say \"\"hi\"\"\""));
    }

    @Test
    public void testUnescapeCsv_WithNewline() {
        Assertions.assertEquals("line1\nline2", EscapeUtil.unescapeCsv("\"line1\nline2\""));
    }

    @Test
    public void testUnescapeCsv_NotQuoted() {
        Assertions.assertEquals("normal text", EscapeUtil.unescapeCsv("normal text"));
    }

    @Test
    public void testUnescapeCsv_QuotedWithoutSpecialChars() {
        Assertions.assertEquals("\"simple\"", EscapeUtil.unescapeCsv("\"simple\""));
    }

    // --- unescapeCsv: malformed (starts with quote, no end quote) ---

    @Test
    public void testUnescapeCsv_MalformedStartsWithQuote() {
        // Starts and doesn't end with quote - should be returned unchanged
        assertEquals("\"abc", EscapeUtil.unescapeCsv("\"abc"));
    }

    // --- unescapeCsv: malformed (ends with quote, no start quote) ---

    @Test
    public void testUnescapeCsv_MalformedEndsWithQuote() {
        assertEquals("abc\"", EscapeUtil.unescapeCsv("abc\""));
    }

    @Test
    public void testEscapeUnescapeCsv_RoundTrip() {
        String original = "field with, comma and \"quotes\"";
        assertEquals(original, EscapeUtil.unescapeCsv(EscapeUtil.escapeCsv(original)));
    }

    @Test
    public void testUnescapeCsv_Null() {
        Assertions.assertNull(EscapeUtil.unescapeCsv(null));
    }

    @Test
    public void testCsvUnescaper_EmptyString_NoCrash() {
        Assertions.assertEquals("", EscapeUtil.unescapeCsv(""));
    }

    @Test
    public void testCsvUnescaper_SingleChar() {
        Assertions.assertEquals("a", EscapeUtil.unescapeCsv("a"));
    }

    @Test
    public void testUnescapeCsv_QuotedEmptyString() {
        Assertions.assertEquals("\"\"", EscapeUtil.unescapeCsv("\"\""));
    }

    @Test
    public void testUnescapeCsv() {
        assertNull(EscapeUtil.unescapeCsv(null));
        assertEquals("", EscapeUtil.unescapeCsv(""));
        assertEquals("abc", EscapeUtil.unescapeCsv("abc"), "Unquoted string should be unchanged");
        assertEquals("\"abc\"", EscapeUtil.unescapeCsv("\"abc\""), "Quoted string without special chars should be unchanged");
        assertEquals("a,b", EscapeUtil.unescapeCsv("\"a,b\""), "Quoted string with comma should have quotes removed");
        assertEquals("a\"b", EscapeUtil.unescapeCsv("\"a\"\"b\""), "Escaped double quotes should be unescaped");
        assertEquals("a\nb", EscapeUtil.unescapeCsv("\"a\nb\""), "Quoted string with newline should have quotes removed");
        assertEquals("\"abc", EscapeUtil.unescapeCsv("\"abc"), "Malformed CSV (only starting quote) should be unchanged");
        assertEquals("abc\"", EscapeUtil.unescapeCsv("abc\""), "Malformed CSV (only ending quote) should be unchanged");
    }

    @Test
    public void testCharSequenceTranslator_TranslateWrapsIOException() {
        CharSequenceTranslator translator = new CharSequenceTranslator() {
            @Override
            public int translate(CharSequence input, int index, Writer out) throws IOException {
                throw new IOException("forced");
            }
        };

        RuntimeException exception = assertThrows(RuntimeException.class, () -> translator.translate("boom"));

        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    public void testUnicodeEscaper_ToUtf16Escape_BasicMultilingualPlane() {
        EscapeUtil.UnicodeEscaper escaper = new EscapeUtil.UnicodeEscaper();

        assertEquals("\\u41", escaper.toUtf16Escape('A'));
    }

    @Test
    public void testCharSequenceTranslator_Translate_String() {
        String result = EscapeUtil.ESCAPE_JAVA.translate("Hello\nWorld");
        Assertions.assertEquals("Hello\\nWorld", result);
    }

    @Test
    public void testCharSequenceTranslator_Translate_Null() {
        String result = EscapeUtil.ESCAPE_JAVA.translate(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testCharSequenceTranslator_Translate_EmptyString() {
        String result = EscapeUtil.ESCAPE_JAVA.translate("");
        Assertions.assertEquals("", result);
    }

    @Test
    public void testCharSequenceTranslator_Translate_ToWriter() throws IOException {
        StringWriter writer = new StringWriter();
        EscapeUtil.ESCAPE_JAVA.translate("Hello\nWorld", writer);
        Assertions.assertEquals("Hello\\nWorld", writer.toString());
    }

    @Test
    public void testCharSequenceTranslator_Translate_ToWriter_Null() throws IOException {
        StringWriter writer = new StringWriter();
        EscapeUtil.ESCAPE_JAVA.translate(null, writer);
        Assertions.assertEquals("", writer.toString());
    }

    @Test
    public void testCharSequenceTranslator_Translate_NullWriter() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            EscapeUtil.ESCAPE_JAVA.translate("test", null);
        });
    }

    @Test
    public void testCharSequenceTranslator_Hex_Zero() {
        Assertions.assertEquals("0", EscapeUtil.CharSequenceTranslator.hex(0));
    }

    @Test
    public void testCharSequenceTranslator_Hex_Decimal() {
        Assertions.assertEquals("A", EscapeUtil.CharSequenceTranslator.hex(10));
    }

    @Test
    public void testCharSequenceTranslator_Hex_LargeValue() {
        Assertions.assertEquals("FF", EscapeUtil.CharSequenceTranslator.hex(255));
    }

    @Test
    public void testCharSequenceTranslator_Hex_UpperCase() {
        String result = EscapeUtil.CharSequenceTranslator.hex(0xABCD);
        Assertions.assertEquals("ABCD", result);
    }

    @Test
    public void testBeanArrays_ISO8859_1_ESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.ISO8859_1_ESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testBeanArrays_ISO8859_1_UNESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.ISO8859_1_UNESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testBeanArrays_HTML40_EXTENDED_ESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.HTML40_EXTENDED_ESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testBeanArrays_HTML40_EXTENDED_UNESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.HTML40_EXTENDED_UNESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testBeanArrays_BASIC_ESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.BASIC_ESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
    }

    @Test
    public void testBeanArrays_BASIC_UNESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.BASIC_UNESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.length);
    }

    @Test
    public void testBeanArrays_APOS_ESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.APOS_ESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.length);
    }

    @Test
    public void testBeanArrays_APOS_UNESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.APOS_UNESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.length);
    }

    @Test
    public void testBeanArrays_JAVA_CTRL_CHARS_ESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.JAVA_CTRL_CHARS_ESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testBeanArrays_JAVA_CTRL_CHARS_UNESCAPE_NotNull() {
        String[][] result = EscapeUtil.BeanArrays.JAVA_CTRL_CHARS_UNESCAPE();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.length > 0);
    }

    @Test
    public void testBeanArrays_Invert_BasicArray() {
        String[][] input = { { "a", "b" }, { "c", "d" } };
        String[][] result = EscapeUtil.BeanArrays.invert(input);
        Assertions.assertEquals("b", result[0][0]);
        Assertions.assertEquals("a", result[0][1]);
        Assertions.assertEquals("d", result[1][0]);
        Assertions.assertEquals("c", result[1][1]);
    }

    @Test
    public void testBeanArrays_Invert_EmptyArray() {
        String[][] input = {};
        String[][] result = EscapeUtil.BeanArrays.invert(input);
        Assertions.assertEquals(0, result.length);
    }

    // --- CharSequenceTranslator.with() ---

    @Test
    public void testCharSequenceTranslator_With() {
        CharSequenceTranslator combined = EscapeUtil.ESCAPE_JAVA.with(EscapeUtil.ESCAPE_HTML4);
        assertNotNull(combined);
        // Should still escape Java sequences
        String result = combined.translate("Hello\nWorld");
        assertNotNull(result);
    }

    @Test
    public void testCharSequenceTranslator_With_EmptyArray() {
        CharSequenceTranslator combined = EscapeUtil.ESCAPE_JAVA.with();
        assertNotNull(combined);
        String result = combined.translate("test");
        assertNotNull(result);
    }

    // --- AggregateTranslator ---

    @Test
    public void testAggregateTranslator_Constructor() {
        EscapeUtil.AggregateTranslator agg = new EscapeUtil.AggregateTranslator(EscapeUtil.ESCAPE_JAVA);
        assertNotNull(agg);
    }

    @Test
    public void testAggregateTranslator_Translate() throws IOException {
        EscapeUtil.AggregateTranslator agg = new EscapeUtil.AggregateTranslator(EscapeUtil.ESCAPE_JAVA);
        StringWriter writer = new StringWriter();
        int consumed = agg.translate("Hello\nWorld", 5, writer);
        assertTrue(consumed > 0);
        assertEquals("\\n", writer.toString());
    }

    @Test
    public void testAggregateTranslator_Translate_NoMatch() throws IOException {
        EscapeUtil.AggregateTranslator agg = new EscapeUtil.AggregateTranslator(new EscapeUtil.LookupTranslator(new String[][] { { "X", "Y" } }));
        StringWriter writer = new StringWriter();
        int consumed = agg.translate("Hello", 0, writer);
        assertEquals(0, consumed);
    }

    // --- JavaUnicodeEscaper ---

    @Test
    public void testJavaUnicodeEscaper_Above() {
        EscapeUtil.JavaUnicodeEscaper escaper = EscapeUtil.JavaUnicodeEscaper.above(127);
        assertNotNull(escaper);
        String result = escaper.translate("\u00E9");
        assertEquals("\\u00E9", result);
    }

    @Test
    public void testJavaUnicodeEscaper_Below() {
        EscapeUtil.JavaUnicodeEscaper escaper = EscapeUtil.JavaUnicodeEscaper.below(128);
        assertNotNull(escaper);
        String result = escaper.translate("A");
        assertEquals("\\u0041", result);
    }

    @Test
    public void testJavaUnicodeEscaper_Between() {
        EscapeUtil.JavaUnicodeEscaper escaper = EscapeUtil.JavaUnicodeEscaper.between(65, 90);
        assertNotNull(escaper);
        String result = escaper.translate("A");
        assertEquals("\\u0041", result);
        // 'a' (97) is outside the range
        result = escaper.translate("a");
        assertEquals("a", result);
    }

    @Test
    public void testJavaUnicodeEscaper_OutsideOf() {
        EscapeUtil.JavaUnicodeEscaper escaper = EscapeUtil.JavaUnicodeEscaper.outsideOf(65, 90);
        assertNotNull(escaper);
        // 'A' (65) is inside the range, should NOT be escaped
        String result = escaper.translate("A");
        assertEquals("A", result);
    }

    @Test
    public void testJavaUnicodeEscaper_Constructor() {
        EscapeUtil.JavaUnicodeEscaper escaper = new EscapeUtil.JavaUnicodeEscaper(0, 127, true);
        assertNotNull(escaper);
    }

    // --- NumericBeanEscaper ---

    @Test
    public void testNumericBeanEscaper_DefaultConstructor() {
        EscapeUtil.NumericBeanEscaper escaper = new EscapeUtil.NumericBeanEscaper();
        assertNotNull(escaper);
    }

    @Test
    public void testNumericBeanEscaper_Below() {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.below(128);
        assertNotNull(escaper);
        String result = escaper.translate("A");
        assertEquals("&#65;", result);
    }

    @Test
    public void testNumericBeanEscaper_Above() {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.above(127);
        assertNotNull(escaper);
        String result = escaper.translate("\u00E9");
        assertEquals("&#233;", result);
    }

    @Test
    public void testNumericBeanEscaper_Between() {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.between(65, 90);
        assertNotNull(escaper);
        String result = escaper.translate("A");
        assertEquals("&#65;", result);
        // 'a' (97) is outside range - should NOT be escaped
        result = escaper.translate("a");
        assertEquals("a", result);
    }

    @Test
    public void testNumericBeanEscaper_OutsideOf() {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.outsideOf(65, 90);
        assertNotNull(escaper);
        // 'A' is inside range - should NOT be escaped
        String result = escaper.translate("A");
        assertEquals("A", result);
    }

    @Test
    public void testNumericBeanEscaper_Translate() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = new EscapeUtil.NumericBeanEscaper();
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(65, writer);
        assertTrue(translated);
        assertEquals("&#65;", writer.toString());
    }

    // --- UnicodeUnpairedSurrogateRemover ---

    @Test
    public void testUnicodeUnpairedSurrogateRemover_Translate_Surrogate() throws IOException {
        EscapeUtil.UnicodeUnpairedSurrogateRemover remover = new EscapeUtil.UnicodeUnpairedSurrogateRemover();
        StringWriter writer = new StringWriter();
        boolean result = remover.translate(Character.MIN_SURROGATE, writer);
        assertTrue(result);
        assertEquals("", writer.toString());
    }

    @Test
    public void testUnicodeUnpairedSurrogateRemover_Translate_NonSurrogate() throws IOException {
        EscapeUtil.UnicodeUnpairedSurrogateRemover remover = new EscapeUtil.UnicodeUnpairedSurrogateRemover();
        StringWriter writer = new StringWriter();
        boolean result = remover.translate(65, writer);
        assertFalse(result);
    }

    // --- UnicodeUnescaper ---

    @Test
    public void testUnicodeUnescaper_Translate() throws IOException {
        EscapeUtil.UnicodeUnescaper unescaper = new EscapeUtil.UnicodeUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\u0041rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    @Test
    public void testUnicodeUnescaper_Translate_NoMatch() throws IOException {
        EscapeUtil.UnicodeUnescaper unescaper = new EscapeUtil.UnicodeUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("Hello", 0, writer);
        assertEquals(0, consumed);
    }

    @Test
    public void testUnicodeUnescaper_Translate_InvalidHex() {
        EscapeUtil.UnicodeUnescaper unescaper = new EscapeUtil.UnicodeUnescaper();
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> unescaper.translate("\\uZZZZ", 0, writer));
    }

    @Test
    public void testUnicodeUnescaper_Translate_TooShort() {
        EscapeUtil.UnicodeUnescaper unescaper = new EscapeUtil.UnicodeUnescaper();
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> unescaper.translate("\\u12", 0, writer));
    }

    // --- UnicodeEscaper ---

    @Test
    public void testUnicodeEscaper_DefaultConstructor() {
        EscapeUtil.UnicodeEscaper escaper = new EscapeUtil.UnicodeEscaper();
        assertNotNull(escaper);
    }

    @Test
    public void testUnicodeEscaper_Below() {
        EscapeUtil.UnicodeEscaper escaper = EscapeUtil.UnicodeEscaper.below(128);
        assertNotNull(escaper);
        String result = escaper.translate("A");
        assertEquals("\\u0041", result);
    }

    @Test
    public void testUnicodeEscaper_Above() {
        EscapeUtil.UnicodeEscaper escaper = EscapeUtil.UnicodeEscaper.above(127);
        assertNotNull(escaper);
        String result = escaper.translate("\u00E9");
        assertEquals("\\u00E9", result);
    }

    @Test
    public void testUnicodeEscaper_OutsideOf() {
        EscapeUtil.UnicodeEscaper escaper = EscapeUtil.UnicodeEscaper.outsideOf(65, 90);
        assertNotNull(escaper);
        // 'A' (65) inside range - NOT escaped
        String result = escaper.translate("A");
        assertEquals("A", result);
    }

    @Test
    public void testUnicodeEscaper_Between() {
        EscapeUtil.UnicodeEscaper escaper = EscapeUtil.UnicodeEscaper.between(65, 90);
        assertNotNull(escaper);
        String result = escaper.translate("A");
        assertEquals("\\u0041", result);
        result = escaper.translate("a");
        assertEquals("a", result);
    }

    @Test
    public void testUnicodeEscaper_Translate() throws IOException {
        EscapeUtil.UnicodeEscaper escaper = new EscapeUtil.UnicodeEscaper();
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(65, writer);
        assertTrue(translated);
        assertEquals("\\u0041", writer.toString());
    }

    // --- OctalUnescaper ---

    @Test
    public void testOctalUnescaper_Translate() throws IOException {
        EscapeUtil.OctalUnescaper unescaper = new EscapeUtil.OctalUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\101rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    @Test
    public void testOctalUnescaper_Translate_NoMatch() throws IOException {
        EscapeUtil.OctalUnescaper unescaper = new EscapeUtil.OctalUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("Hello", 0, writer);
        assertEquals(0, consumed);
    }

    @Test
    public void testOctalUnescaper_Translate_SingleDigit() throws IOException {
        EscapeUtil.OctalUnescaper unescaper = new EscapeUtil.OctalUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\1", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("\u0001", writer.toString());
    }

    @Test
    public void testOctalUnescaper_Translate_TwoDigits() throws IOException {
        EscapeUtil.OctalUnescaper unescaper = new EscapeUtil.OctalUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\45rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("%", writer.toString());
    }

    // --- NumericBeanUnescaper ---

    @Test
    public void testNumericBeanUnescaper_Constructor_Default() {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        assertTrue(unescaper.isSet(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonRequired));
    }

    @Test
    public void testNumericBeanUnescaper_Constructor_WithOption() {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonOptional);
        assertTrue(unescaper.isSet(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonOptional));
        assertFalse(unescaper.isSet(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonRequired));
    }

    @Test
    public void testNumericBeanUnescaper_IsSet() {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper(EscapeUtil.NumericBeanUnescaper.OPTION.errorIfNoSemiColon);
        assertTrue(unescaper.isSet(EscapeUtil.NumericBeanUnescaper.OPTION.errorIfNoSemiColon));
        assertFalse(unescaper.isSet(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonOptional));
    }

    @Test
    public void testNumericBeanUnescaper_Translate_Decimal() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#65;rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    @Test
    public void testNumericBeanUnescaper_Translate_Hex() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#x41;rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    @Test
    public void testNumericBeanUnescaper_Translate_NoMatch() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("Hello", 0, writer);
        assertEquals(0, consumed);
    }

    @Test
    public void testNumericBeanUnescaper_Translate_SemiColonRequired() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonRequired);
        StringWriter writer = new StringWriter();
        // No semicolon - should return 0
        int consumed = unescaper.translate("&#65rest", 0, writer);
        assertEquals(0, consumed);
    }

    @Test
    public void testNumericBeanUnescaper_Translate_ErrorIfNoSemiColon() {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper(EscapeUtil.NumericBeanUnescaper.OPTION.errorIfNoSemiColon);
        StringWriter writer = new StringWriter();
        assertThrows(IllegalArgumentException.class, () -> unescaper.translate("&#65rest", 0, writer));
    }

    @Test
    public void testNumericBeanUnescaper_Translate_SemiColonOptional() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonOptional);
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#65rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    @Test
    public void testNumericBeanUnescaper_OPTION_Values() {
        EscapeUtil.NumericBeanUnescaper.OPTION[] values = EscapeUtil.NumericBeanUnescaper.OPTION.values();
        assertEquals(3, values.length);
    }

    // --- LookupTranslator ---

    @Test
    public void testLookupTranslator_Constructor_And_Translate() throws IOException {
        EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(new String[][] { { "abc", "xyz" }, { "de", "FG" } });
        StringWriter writer = new StringWriter();
        int consumed = translator.translate("abcde", 0, writer);
        assertEquals(3, consumed);
        assertEquals("xyz", writer.toString());
    }

    @Test
    public void testLookupTranslator_Translate_NoMatch() throws IOException {
        EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(new String[][] { { "abc", "xyz" } });
        StringWriter writer = new StringWriter();
        int consumed = translator.translate("Hello", 0, writer);
        assertEquals(0, consumed);
    }

    @Test
    public void testLookupTranslator_Translate_MultipleEntries() throws IOException {
        EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(new String[][] { { "<", "&lt;" }, { ">", "&gt;" }, { "&", "&amp;" } });
        String result = translator.translate("<div>&</div>");
        assertEquals("&lt;div&gt;&amp;&lt;/div&gt;", result);
    }

    // --- CodePointTranslator ---

    @Test
    public void testCodePointTranslator_Translate() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = new EscapeUtil.NumericBeanEscaper();
        StringWriter writer = new StringWriter();
        // CodePointTranslator.translate(CharSequence, int, Writer) should delegate
        int consumed = escaper.translate("A", 0, writer);
        assertEquals(1, consumed);
        assertEquals("&#65;", writer.toString());
    }

    @Test
    public void testCodePointTranslator_Translate_NotConsumed() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.between(200, 300);
        StringWriter writer = new StringWriter();
        int consumed = escaper.translate("A", 0, writer);
        assertEquals(0, consumed);
    }

    // --- CsvEscaper ---

    @Test
    public void testCsvEscaper_Translate_WithSpecialChars() throws IOException {
        EscapeUtil.CsvEscaper escaper = new EscapeUtil.CsvEscaper();
        StringWriter writer = new StringWriter();
        int consumed = escaper.translate("hello,world", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("\"hello,world\"", writer.toString());
    }

    @Test
    public void testCsvEscaper_Translate_NoSpecialChars() throws IOException {
        EscapeUtil.CsvEscaper escaper = new EscapeUtil.CsvEscaper();
        StringWriter writer = new StringWriter();
        int consumed = escaper.translate("simple", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("simple", writer.toString());
    }

    @Test
    public void testCsvEscaper_Translate_NonZeroIndex() {
        EscapeUtil.CsvEscaper escaper = new EscapeUtil.CsvEscaper();
        StringWriter writer = new StringWriter();
        assertThrows(IllegalStateException.class, () -> escaper.translate("test", 1, writer));
    }

    // --- CsvUnescaper ---

    @Test
    public void testCsvUnescaper_Translate_Quoted() throws IOException {
        EscapeUtil.CsvUnescaper unescaper = new EscapeUtil.CsvUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\"hello,world\"", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("hello,world", writer.toString());
    }

    @Test
    public void testCsvUnescaper_Translate_Unquoted() throws IOException {
        EscapeUtil.CsvUnescaper unescaper = new EscapeUtil.CsvUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("simple", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("simple", writer.toString());
    }

    @Test
    public void testCsvUnescaper_Translate_NonZeroIndex() {
        EscapeUtil.CsvUnescaper unescaper = new EscapeUtil.CsvUnescaper();
        StringWriter writer = new StringWriter();
        assertThrows(IllegalStateException.class, () -> unescaper.translate("test", 1, writer));
    }

    @Test
    public void testCsvUnescaper_Translate_EscapedQuotes() throws IOException {
        EscapeUtil.CsvUnescaper unescaper = new EscapeUtil.CsvUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\"say \"\"hi\"\"\"", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("say \"hi\"", writer.toString());
    }

    // --- CharSequenceTranslator.translate(CharSequence, Writer): surrogate pair handling ---

    @Test
    public void testCharSequenceTranslator_Translate_SurrogatePairWriter() throws IOException {
        // Test that the translate(CharSequence, Writer) correctly handles surrogate pairs
        StringWriter writer = new StringWriter();
        EscapeUtil.ESCAPE_HTML4.translate("Hello \uD83D\uDE00 World", writer);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.contains("\uD83D\uDE00")); // emoji should pass through HTML escaping
    }

    // --- CharSequenceTranslator.with: chaining multiple translators ---

    @Test
    public void testCharSequenceTranslator_With_MultipleTranslators() {
        CharSequenceTranslator combined = EscapeUtil.ESCAPE_JAVA.with(EscapeUtil.ESCAPE_HTML4, EscapeUtil.ESCAPE_CSV);
        assertNotNull(combined);
        String result = combined.translate("Hello\nWorld");
        assertEquals("\"Hello\nWorld\"", result);
    }

    // --- JavaUnicodeEscaper.toUtf16Escape: supplementary character ---

    @Test
    public void testJavaUnicodeEscaper_ToUtf16Escape_Supplementary() {
        EscapeUtil.JavaUnicodeEscaper escaper = EscapeUtil.JavaUnicodeEscaper.between(0, Integer.MAX_VALUE);
        // Supplementary character (emoji) should produce \\uXXXX\\uXXXX
        String result = escaper.translate("\uD83D\uDE00"); // U+1F600
        assertTrue(result.contains("\\uD83D"));
        assertTrue(result.contains("\\uDE00"));
    }

    // --- JavaUnicodeEscaper: translate ASCII within range ---

    @Test
    public void testJavaUnicodeEscaper_Between_AsciiInRange() {
        EscapeUtil.JavaUnicodeEscaper escaper = EscapeUtil.JavaUnicodeEscaper.between(48, 57); // digits 0-9
        assertEquals("\\u0030", escaper.translate("0"));
        assertEquals("a", escaper.translate("a")); // outside range
    }

    // --- NumericBeanEscaper: default constructor escapes all codepoints ---

    @Test
    public void testNumericBeanEscaper_DefaultConstructor_EscapesAll() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = new EscapeUtil.NumericBeanEscaper();
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(90, writer); // 'Z'
        assertTrue(translated);
        assertEquals("&#90;", writer.toString());
    }

    // --- NumericBeanEscaper.translate: between=true, outside range returns false ---

    @Test
    public void testNumericBeanEscaper_Between_OutsideRange() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.between(65, 90);
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(50, writer); // '2' is outside 65-90
        assertFalse(translated);
        assertEquals("", writer.toString());
    }

    // --- NumericBeanEscaper.translate: between=false (outsideOf), inside range returns false ---

    @Test
    public void testNumericBeanEscaper_OutsideOf_InsideRange() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.outsideOf(65, 90);
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(65, writer); // 'A' is inside range
        assertFalse(translated);
        assertEquals("", writer.toString());
    }

    // --- NumericBeanEscaper.translate: between=false (outsideOf), outside range returns true ---

    @Test
    public void testNumericBeanEscaper_OutsideOf_OutsideRange() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = EscapeUtil.NumericBeanEscaper.outsideOf(65, 90);
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(50, writer); // '2' is outside 65-90
        assertTrue(translated);
        assertEquals("&#50;", writer.toString());
    }

    // --- UnicodeUnpairedSurrogateRemover: max surrogate boundary ---

    @Test
    public void testUnicodeUnpairedSurrogateRemover_MaxSurrogate() throws IOException {
        EscapeUtil.UnicodeUnpairedSurrogateRemover remover = new EscapeUtil.UnicodeUnpairedSurrogateRemover();
        StringWriter writer = new StringWriter();
        boolean result = remover.translate(Character.MAX_SURROGATE, writer);
        assertTrue(result);
        assertEquals("", writer.toString());
    }

    // --- UnicodeUnpairedSurrogateRemover: just below surrogate range ---

    @Test
    public void testUnicodeUnpairedSurrogateRemover_BelowSurrogateRange() throws IOException {
        EscapeUtil.UnicodeUnpairedSurrogateRemover remover = new EscapeUtil.UnicodeUnpairedSurrogateRemover();
        StringWriter writer = new StringWriter();
        boolean result = remover.translate(Character.MIN_SURROGATE - 1, writer);
        assertFalse(result);
    }

    // --- UnicodeUnpairedSurrogateRemover: just above surrogate range ---

    @Test
    public void testUnicodeUnpairedSurrogateRemover_AboveSurrogateRange() throws IOException {
        EscapeUtil.UnicodeUnpairedSurrogateRemover remover = new EscapeUtil.UnicodeUnpairedSurrogateRemover();
        StringWriter writer = new StringWriter();
        boolean result = remover.translate(Character.MAX_SURROGATE + 1, writer);
        assertFalse(result);
    }

    // --- UnicodeUnescaper: multiple 'u' chars (\\uuXXXX) ---

    @Test
    public void testUnicodeUnescaper_MultipleU() throws IOException {
        EscapeUtil.UnicodeUnescaper unescaper = new EscapeUtil.UnicodeUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\uu0041rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    // --- UnicodeUnescaper: with '+' sign (\\u+XXXX) ---

    @Test
    public void testUnicodeUnescaper_WithPlusSign() throws IOException {
        EscapeUtil.UnicodeUnescaper unescaper = new EscapeUtil.UnicodeUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\u+0041rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    // --- UnicodeUnescaper: backslash not followed by 'u' ---

    @Test
    public void testUnicodeUnescaper_BackslashNotU() throws IOException {
        EscapeUtil.UnicodeUnescaper unescaper = new EscapeUtil.UnicodeUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\n", 0, writer);
        assertEquals(0, consumed);
    }

    // --- UnicodeEscaper: translate with codepoint > 0xFFFF ---

    @Test
    public void testUnicodeEscaper_Translate_SupplementaryCodepoint() throws IOException {
        EscapeUtil.UnicodeEscaper escaper = new EscapeUtil.UnicodeEscaper();
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(0x1F600, writer); // smiley emoji
        assertTrue(translated);
        String result = writer.toString();
        // Should produce surrogate pair escape
        assertTrue(result.contains("\\u"));
    }

    // --- UnicodeEscaper: between=true, codepoint outside range ---

    @Test
    public void testUnicodeEscaper_Between_OutsideRange() throws IOException {
        EscapeUtil.UnicodeEscaper escaper = EscapeUtil.UnicodeEscaper.between(65, 90);
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(50, writer);
        assertFalse(translated);
    }

    // --- UnicodeEscaper: between=false (outsideOf), codepoint inside range ---

    @Test
    public void testUnicodeEscaper_OutsideOf_InsideRange() throws IOException {
        EscapeUtil.UnicodeEscaper escaper = EscapeUtil.UnicodeEscaper.outsideOf(65, 90);
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(65, writer);
        assertFalse(translated);
    }

    // --- UnicodeEscaper.toUtf16Escape: codepoint > 0xFFFF ---

    @Test
    public void testUnicodeEscaper_ToUtf16Escape_Supplementary() throws IOException {
        EscapeUtil.UnicodeEscaper escaper = new EscapeUtil.UnicodeEscaper();
        StringWriter writer = new StringWriter();
        // This will call toUtf16Escape for supplementary character
        boolean translated = escaper.translate(0x10000, writer);
        assertTrue(translated);
        assertTrue(writer.toString().startsWith("\\u"));
    }

    // --- UnicodeEscaper.toUtf16Escape: codepoint <= 0xFFFF ---

    @Test
    public void testUnicodeEscaper_ToUtf16Escape_BMP() throws IOException {
        EscapeUtil.UnicodeEscaper escaper = new EscapeUtil.UnicodeEscaper();
        StringWriter writer = new StringWriter();
        boolean translated = escaper.translate(0x41, writer); // 'A'
        assertTrue(translated);
        assertEquals("\\u0041", writer.toString());
    }

    // --- OctalUnescaper: three digits with leading 0-3 ---

    @Test
    public void testOctalUnescaper_ThreeDigits() throws IOException {
        EscapeUtil.OctalUnescaper unescaper = new EscapeUtil.OctalUnescaper();
        StringWriter writer = new StringWriter();
        // \377 = 255 = 0xFF
        int consumed = unescaper.translate("\\377rest", 0, writer);
        assertEquals(4, consumed); // backslash + 3 digits
        assertEquals("\u00FF", writer.toString());
    }

    // --- OctalUnescaper: two digits where first > 3 (cannot be three digits) ---

    @Test
    public void testOctalUnescaper_TwoDigitsFirstAboveThree() throws IOException {
        EscapeUtil.OctalUnescaper unescaper = new EscapeUtil.OctalUnescaper();
        StringWriter writer = new StringWriter();
        // \45 = 37 = '%' - first digit 4 > 3 so max 2 octal digits
        int consumed = unescaper.translate("\\450rest", 0, writer);
        assertEquals(3, consumed); // backslash + 2 digits (stops at '0' because 4 > 3)
        assertEquals("%", writer.toString());
    }

    // --- OctalUnescaper: backslash followed by non-octal digit ---

    @Test
    public void testOctalUnescaper_BackslashNonOctal() throws IOException {
        EscapeUtil.OctalUnescaper unescaper = new EscapeUtil.OctalUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\\9", 0, writer); // 9 is not octal
        assertEquals(0, consumed);
    }

    // --- NumericBeanUnescaper: hex with uppercase X ---

    @Test
    public void testNumericBeanUnescaper_Translate_HexUppercaseX() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#X41;rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("A", writer.toString());
    }

    // --- NumericBeanUnescaper: codepoint > 0xFFFF (supplementary) ---

    @Test
    public void testNumericBeanUnescaper_Translate_SupplementaryCodepoint() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        StringWriter writer = new StringWriter();
        // &#x1F600; = smiley emoji
        int consumed = unescaper.translate("&#x1F600;rest", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("\uD83D\uDE00", writer.toString());
    }

    // --- NumericBeanUnescaper: invalid number format (returns 0) ---

    @Test
    public void testNumericBeanUnescaper_Translate_EmptyDigits() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        StringWriter writer = new StringWriter();
        // &#; has no digits between # and ;
        int consumed = unescaper.translate("&#;rest", 0, writer);
        assertEquals(0, consumed);
    }

    // --- NumericBeanUnescaper: &#x at end of string ---

    @Test
    public void testNumericBeanUnescaper_Translate_HexAtEndOfString() throws IOException {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("&#x", 0, writer);
        assertEquals(0, consumed);
    }

    // --- NumericBeanUnescaper: constructor with multiple options ---

    @Test
    public void testNumericBeanUnescaper_Constructor_MultipleOptions() {
        EscapeUtil.NumericBeanUnescaper unescaper = new EscapeUtil.NumericBeanUnescaper(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonOptional,
                EscapeUtil.NumericBeanUnescaper.OPTION.errorIfNoSemiColon);
        assertTrue(unescaper.isSet(EscapeUtil.NumericBeanUnescaper.OPTION.semiColonOptional));
        assertTrue(unescaper.isSet(EscapeUtil.NumericBeanUnescaper.OPTION.errorIfNoSemiColon));
    }

    // --- LookupTranslator: greedy matching (longest match first) ---

    @Test
    public void testLookupTranslator_GreedyMatch() throws IOException {
        EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(new String[][] { { "ab", "SHORT" }, { "abc", "LONG" } });
        StringWriter writer = new StringWriter();
        int consumed = translator.translate("abcdef", 0, writer);
        assertEquals(3, consumed); // Should match "abc" (longest)
        assertEquals("LONG", writer.toString());
    }

    // --- LookupTranslator: input shorter than longest key ---

    @Test
    public void testLookupTranslator_InputShorterThanLongestKey() throws IOException {
        EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(new String[][] { { "abcdef", "LONG" }, { "ab", "SHORT" } });
        StringWriter writer = new StringWriter();
        int consumed = translator.translate("ab", 0, writer);
        assertEquals(2, consumed);
        assertEquals("SHORT", writer.toString());
    }

    // --- LookupTranslator: match at non-zero index ---

    @Test
    public void testLookupTranslator_MatchAtNonZeroIndex() throws IOException {
        EscapeUtil.LookupTranslator translator = new EscapeUtil.LookupTranslator(new String[][] { { "<", "&lt;" } });
        StringWriter writer = new StringWriter();
        int consumed = translator.translate("a<b", 1, writer);
        assertEquals(1, consumed);
        assertEquals("&lt;", writer.toString());
    }

    // --- CodePointTranslator: translate with supplementary codepoint ---

    @Test
    public void testCodePointTranslator_SupplementaryCodepoint() throws IOException {
        EscapeUtil.NumericBeanEscaper escaper = new EscapeUtil.NumericBeanEscaper();
        StringWriter writer = new StringWriter();
        // Use a string with a supplementary character
        int consumed = escaper.translate("\uD83D\uDE00", 0, writer);
        assertEquals(1, consumed); // CodePointTranslator returns 1 for consumed codepoint
    }

    // --- CsvEscaper: string with all special chars combined ---

    @Test
    public void testCsvEscaper_AllSpecialChars() throws IOException {
        EscapeUtil.CsvEscaper escaper = new EscapeUtil.CsvEscaper();
        StringWriter writer = new StringWriter();
        int consumed = escaper.translate("a,b\"c\nd\re", 0, writer);
        assertTrue(consumed > 0);
        assertTrue(writer.toString().startsWith("\""));
        assertTrue(writer.toString().endsWith("\""));
    }

    // --- CsvUnescaper: quoted string with carriage return ---

    @Test
    public void testCsvUnescaper_QuotedWithCarriageReturn() throws IOException {
        EscapeUtil.CsvUnescaper unescaper = new EscapeUtil.CsvUnescaper();
        StringWriter writer = new StringWriter();
        int consumed = unescaper.translate("\"a\rb\"", 0, writer);
        assertTrue(consumed > 0);
        assertEquals("a\rb", writer.toString());
    }

    // --- BeanArrays: verify BASIC_ESCAPE content ---

    @Test
    public void testBeanArrays_BASIC_ESCAPE_Content() {
        String[][] result = EscapeUtil.BeanArrays.BASIC_ESCAPE();
        // Verify it contains the 4 basic entities: " & < >
        assertEquals("\"", result[0][0]);
        assertEquals("&quot;", result[0][1]);
        assertEquals("&", result[1][0]);
        assertEquals("&amp;", result[1][1]);
        assertEquals("<", result[2][0]);
        assertEquals("&lt;", result[2][1]);
        assertEquals(">", result[3][0]);
        assertEquals("&gt;", result[3][1]);
    }

    // --- BeanArrays: verify APOS_ESCAPE content ---

    @Test
    public void testBeanArrays_APOS_ESCAPE_Content() {
        String[][] result = EscapeUtil.BeanArrays.APOS_ESCAPE();
        assertEquals("'", result[0][0]);
        assertEquals("&apos;", result[0][1]);
    }

    // --- BeanArrays: verify JAVA_CTRL_CHARS_ESCAPE content ---

    @Test
    public void testBeanArrays_JAVA_CTRL_CHARS_ESCAPE_Content() {
        String[][] result = EscapeUtil.BeanArrays.JAVA_CTRL_CHARS_ESCAPE();
        assertEquals(5, result.length);
        // Should contain \b, \n, \t, \f, \r
        assertEquals("\b", result[0][0]);
        assertEquals("\\b", result[0][1]);
        assertEquals("\n", result[1][0]);
        assertEquals("\\n", result[1][1]);
    }

    // --- BeanArrays: invert preserves array dimensions ---

    @Test
    public void testBeanArrays_Invert_SingleElement() {
        String[][] input = { { "key", "value" } };
        String[][] result = EscapeUtil.BeanArrays.invert(input);
        assertEquals(1, result.length);
        assertEquals("value", result[0][0]);
        assertEquals("key", result[0][1]);
    }

    // --- BeanArrays: ISO8859_1 arrays are cloned (defensive copy) ---

    @Test
    public void testBeanArrays_ISO8859_1_ESCAPE_DefensiveCopy() {
        String[][] first = EscapeUtil.BeanArrays.ISO8859_1_ESCAPE();
        String[][] second = EscapeUtil.BeanArrays.ISO8859_1_ESCAPE();
        assertFalse(first == second); // different array instances
        assertEquals(first.length, second.length);
    }

    // --- BeanArrays: HTML40_EXTENDED arrays are cloned ---

    @Test
    public void testBeanArrays_HTML40_EXTENDED_ESCAPE_DefensiveCopy() {
        String[][] first = EscapeUtil.BeanArrays.HTML40_EXTENDED_ESCAPE();
        String[][] second = EscapeUtil.BeanArrays.HTML40_EXTENDED_ESCAPE();
        assertFalse(first == second);
        assertEquals(first.length, second.length);
    }

}
