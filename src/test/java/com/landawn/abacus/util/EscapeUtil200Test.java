package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class EscapeUtil200Test extends TestBase {

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
    public void testEscapeEcmaScript() {
        assertNull(EscapeUtil.escapeEcmaScript(null));
        assertEquals("", EscapeUtil.escapeEcmaScript(""));
        assertEquals("He didn\\'t say, \\\"Stop!\\\"", EscapeUtil.escapeEcmaScript("He didn't say, \"Stop!\""), "Single and double quotes should be escaped");
        assertEquals("<\\/script>", EscapeUtil.escapeEcmaScript("</script>"), "Forward slash should be escaped");
    }

    @Test
    public void testUnescapeEcmaScript() {
        assertNull(EscapeUtil.unescapeEcmaScript(null));
        assertEquals("", EscapeUtil.unescapeEcmaScript(""));
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeEcmaScript("He didn\\'t say, \\\"Stop!\\\""));
        assertEquals("</script>", EscapeUtil.unescapeEcmaScript("<\\/script>"));
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
    public void testUnescapeJson() {
        assertNull(EscapeUtil.unescapeJson(null));
        assertEquals("", EscapeUtil.unescapeJson(""));
        assertEquals("He didn't say, \"Stop!\"", EscapeUtil.unescapeJson("He didn't say, \\\"Stop!\\\""));
        assertEquals("</script>", EscapeUtil.unescapeJson("<\\/script>"));
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
    public void testEscapeHtml3() {
        assertNull(EscapeUtil.escapeHtml3(null));
        assertEquals("", EscapeUtil.escapeHtml3(""));
        assertEquals("&lt;tag&gt;", EscapeUtil.escapeHtml3("<tag>"));
        assertEquals("&copy;", EscapeUtil.escapeHtml3("©"), "ISO-8859-1 characters should be escaped");
        assertEquals("€", EscapeUtil.escapeHtml3("€"), "HTML4 extended entities should not be escaped in HTML3");
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
    public void testEscapeXml11() {
        assertNull(EscapeUtil.escapeXml11(null));
        assertEquals("", EscapeUtil.escapeXml11(""));
        assertEquals("&lt;test&gt;", EscapeUtil.escapeXml11("<test>"));
        assertEquals("", EscapeUtil.escapeXml11("\u0000"), "Null character should be removed");
        assertEquals("&#1;", EscapeUtil.escapeXml11("\u0001"), "Control characters invalid in XML 1.0 but valid in 1.1 should be escaped");
        assertEquals("&#11;", EscapeUtil.escapeXml11("\u000b"), "Control characters should be escaped");
        assertEquals("\t", EscapeUtil.escapeXml11("\t"), "Valid control characters should pass through");
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
}
