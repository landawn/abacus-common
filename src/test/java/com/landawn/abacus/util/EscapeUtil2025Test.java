package com.landawn.abacus.util;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class EscapeUtil2025Test extends TestBase {

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
    public void testEscapeJava_Null() {
        Assertions.assertNull(EscapeUtil.escapeJava(null));
    }

    @Test
    public void testEscapeJava_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeJava(""));
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
    public void testUnescapeJava_Null() {
        Assertions.assertNull(EscapeUtil.unescapeJava(null));
    }

    @Test
    public void testUnescapeJava_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeJava(""));
    }

    @Test
    public void testUnescapeJava_WithUnicodeEscape() {
        Assertions.assertEquals("Hello", EscapeUtil.unescapeJava("\\u0048\\u0065\\u006C\\u006C\\u006F"));
    }

    @Test
    public void testUnescapeJava_WithOctalEscape() {
        Assertions.assertEquals("A", EscapeUtil.unescapeJava("\\101"));
    }

    @Test
    public void testEscapeEcmaScript_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.escapeEcmaScript("Hello World"));
    }

    @Test
    public void testEscapeEcmaScript_WithSingleQuote() {
        Assertions.assertEquals("Don\\'t", EscapeUtil.escapeEcmaScript("Don't"));
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
    public void testEscapeEcmaScript_Null() {
        Assertions.assertNull(EscapeUtil.escapeEcmaScript(null));
    }

    @Test
    public void testEscapeEcmaScript_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeEcmaScript(""));
    }

    @Test
    public void testUnescapeEcmaScript_BasicString() {
        Assertions.assertEquals("Hello World", EscapeUtil.unescapeEcmaScript("Hello World"));
    }

    @Test
    public void testUnescapeEcmaScript_WithSingleQuote() {
        Assertions.assertEquals("Don't", EscapeUtil.unescapeEcmaScript("Don\\'t"));
    }

    @Test
    public void testUnescapeEcmaScript_WithForwardSlash() {
        Assertions.assertEquals("</script>", EscapeUtil.unescapeEcmaScript("<\\/script>"));
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
    public void testEscapeJson_Null() {
        Assertions.assertNull(EscapeUtil.escapeJson(null));
    }

    @Test
    public void testEscapeJson_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeJson(""));
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
    public void testUnescapeJson_Null() {
        Assertions.assertNull(EscapeUtil.unescapeJson(null));
    }

    @Test
    public void testUnescapeJson_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeJson(""));
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
    public void testEscapeHtml4_Null() {
        Assertions.assertNull(EscapeUtil.escapeHtml4(null));
    }

    @Test
    public void testEscapeHtml4_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeHtml4(""));
    }

    @Test
    public void testEscapeHtml4_WithGreaterThan() {
        Assertions.assertEquals("&gt;", EscapeUtil.escapeHtml4(">"));
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
    public void testUnescapeHtml4_Null() {
        Assertions.assertNull(EscapeUtil.unescapeHtml4(null));
    }

    @Test
    public void testUnescapeHtml4_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeHtml4(""));
    }

    @Test
    public void testUnescapeHtml4_UnrecognizedEntity() {
        Assertions.assertEquals("&unknown;", EscapeUtil.unescapeHtml4("&unknown;"));
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
    public void testUnescapeXml_Null() {
        Assertions.assertNull(EscapeUtil.unescapeXml(null));
    }

    @Test
    public void testUnescapeXml_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeXml(""));
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
    public void testEscapeCsv_Null() {
        Assertions.assertNull(EscapeUtil.escapeCsv(null));
    }

    @Test
    public void testEscapeCsv_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.escapeCsv(""));
    }

    @Test
    public void testEscapeCsv_NoSpecialChars() {
        Assertions.assertEquals("normal text", EscapeUtil.escapeCsv("normal text"));
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
    public void testUnescapeCsv_Null() {
        Assertions.assertNull(EscapeUtil.unescapeCsv(null));
    }

    @Test
    public void testUnescapeCsv_EmptyString() {
        Assertions.assertEquals("", EscapeUtil.unescapeCsv(""));
    }

    @Test
    public void testUnescapeCsv_NotQuoted() {
        Assertions.assertEquals("normal text", EscapeUtil.unescapeCsv("normal text"));
    }

    @Test
    public void testUnescapeCsv_QuotedWithoutSpecialChars() {
        Assertions.assertEquals("\"simple\"", EscapeUtil.unescapeCsv("\"simple\""));
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

    @Test
    public void testUnescapeHtml4_DecimalEntity_NoHexDigits() {
        Assertions.assertEquals("&#65", EscapeUtil.unescapeHtml4("&#65"));
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
    public void testCsvUnescaper_EmptyString_NoCrash() {
        Assertions.assertEquals("", EscapeUtil.unescapeCsv(""));
    }

    @Test
    public void testCsvUnescaper_SingleChar() {
        Assertions.assertEquals("a", EscapeUtil.unescapeCsv("a"));
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
    public void testEscapeXml10_RemovesUnpairedSurrogates() {
        char highSurrogate = '\uD800';
        String input = "test" + highSurrogate + "end";
        String result = EscapeUtil.escapeXml10(input);
        Assertions.assertFalse(result.contains(String.valueOf(highSurrogate)));
    }

    @Test
    public void testUnescapeJava_TrailingBackslash() {
        String result = EscapeUtil.unescapeJava("test\\");
        Assertions.assertNotNull(result);
    }

    @Test
    public void testEscapeCsv_MultipleQuotes() {
        String input = "He said \"Hello\" and \"Goodbye\"";
        String result = EscapeUtil.escapeCsv(input);
        Assertions.assertTrue(result.contains("\"\""));
    }

    @Test
    public void testUnescapeCsv_QuotedEmptyString() {
        Assertions.assertEquals("\"\"", EscapeUtil.unescapeCsv("\"\""));
    }

    @Test
    public void testEscapeHtml4_AllBasicEntities() {
        String result = EscapeUtil.escapeHtml4("<>&\"");
        Assertions.assertEquals("&lt;&gt;&amp;&quot;", result);
    }

    @Test
    public void testUnescapeXml_AllBasicEntities() {
        String result = EscapeUtil.unescapeXml("&lt;&gt;&amp;&quot;&apos;");
        Assertions.assertEquals("<>&\"'", result);
    }

    @Test
    public void testEscapeJson_ControlCharacters() {
        String result = EscapeUtil.escapeJson("Hello\t\n\r\fWorld");
        Assertions.assertTrue(result.contains("\\t"));
        Assertions.assertTrue(result.contains("\\n"));
        Assertions.assertTrue(result.contains("\\r"));
    }

    @Test
    public void testUnescapeJson_AllEscapeSequences() {
        String result = EscapeUtil.unescapeJson("\\t\\n\\r\\f\\b\\\\\\/\\\"");
        Assertions.assertEquals("\t\n\r\f\b\\/\"", result);
    }

    @Test
    public void testEscapeEcmaScript_AllSpecialChars() {
        String result = EscapeUtil.escapeEcmaScript("'\"\\/");
        Assertions.assertTrue(result.contains("\\'"));
        Assertions.assertTrue(result.contains("\\\""));
        Assertions.assertTrue(result.contains("\\/"));
    }
}
