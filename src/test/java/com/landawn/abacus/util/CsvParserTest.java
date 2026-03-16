package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;

@Tag("new-test")
public class CsvParserTest extends TestBase {

    @Test
    public void testDefaultConstructor() {
        CsvParser parser = new CsvParser();
        assertEquals(',', parser.getSeparator());
        assertEquals('"', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
        assertFalse(parser.isStrictQuotes());
        assertTrue(parser.isIgnoreLeadingWhiteSpace());
        assertFalse(parser.isIgnoreQuotations());
    }

    @Test
    public void testConstructorWithSeparator() {
        CsvParser parser = new CsvParser('|');
        assertEquals('|', parser.getSeparator());
        assertEquals('"', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
    }

    @Test
    public void testConstructorWithSeparatorAndQuote() {
        CsvParser parser = new CsvParser('|', '\'');
        assertEquals('|', parser.getSeparator());
        assertEquals('\'', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
    }

    @Test
    public void testConstructorWithSeparatorQuoteAndEscape() {
        CsvParser parser = new CsvParser('|', '\'', '/');
        assertEquals('|', parser.getSeparator());
        assertEquals('\'', parser.getQuoteChar());
        assertEquals('/', parser.getEscape());
    }

    @Test
    public void testConstructorWithStrictQuotes() {
        CsvParser parser = new CsvParser(',', '"', '\\', true);
        assertTrue(parser.isStrictQuotes());
        assertTrue(parser.isIgnoreLeadingWhiteSpace());
        assertFalse(parser.isIgnoreQuotations());
    }

    @Test
    public void testConstructorWithIgnoreLeadingWhiteSpace() {
        CsvParser parser = new CsvParser(',', '"', '\\', false, false);
        assertFalse(parser.isStrictQuotes());
        assertFalse(parser.isIgnoreLeadingWhiteSpace());
        assertFalse(parser.isIgnoreQuotations());
    }

    @Test
    public void testConstructorWithAllParameters() {
        CsvParser parser = new CsvParser(',', '"', '\\', true, false, true);
        assertEquals(',', parser.getSeparator());
        assertEquals('"', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
        assertTrue(parser.isStrictQuotes());
        assertFalse(parser.isIgnoreLeadingWhiteSpace());
        assertTrue(parser.isIgnoreQuotations());
    }

    @Test
    public void testConstructorWithSameCharacters() {
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(',', ',', '\\'));
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(',', '"', ','));
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(',', '"', '"'));
    }

    @Test
    public void testConstructorWithNullSeparator() {
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(CsvParser.NULL_CHARACTER, '"', '\\'));
    }

    @Test
    public void testGetSeparator() {
        CsvParser parser = new CsvParser('\t');
        assertEquals('\t', parser.getSeparator());
    }

    @Test
    public void testGetQuoteChar() {
        CsvParser parser = new CsvParser(',', '\'');
        assertEquals('\'', parser.getQuoteChar());
    }

    @Test
    public void testGetEscape() {
        CsvParser parser = new CsvParser(',', '"', '/');
        assertEquals('/', parser.getEscape());
    }

    @Test
    public void testIsStrictQuotes() {
        CsvParser defaultParser = new CsvParser();
        assertFalse(defaultParser.isStrictQuotes());

        CsvParser strictParser = new CsvParser(',', '"', '\\', true);
        assertTrue(strictParser.isStrictQuotes());
    }

    @Test
    public void testIsIgnoreLeadingWhiteSpace() {
        CsvParser defaultParser = new CsvParser();
        assertTrue(defaultParser.isIgnoreLeadingWhiteSpace());

        CsvParser noIgnoreParser = new CsvParser(',', '"', '\\', false, false);
        assertFalse(noIgnoreParser.isIgnoreLeadingWhiteSpace());
    }

    @Test
    public void testIsIgnoreQuotations() {
        CsvParser defaultParser = new CsvParser();
        assertFalse(defaultParser.isIgnoreQuotations());

        CsvParser ignoreParser = new CsvParser(',', '"', '\\', false, true, true);
        assertTrue(ignoreParser.isIgnoreQuotations());
    }

    @Test
    public void testParseLineSimple() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,b,c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineNull() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseLineEmpty() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("");
        assertEquals(1, result.size());
        assertEquals("", result.get(0));
    }

    @Test
    public void testParseLineWithQuotes() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,\"b,c\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b,c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEscapedQuotes() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,\"b\\\"c\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\"c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEscapedEscape() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,b\\\\c,d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\\\\c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithLeadingWhitespace() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,  b  ,  \"c\" ");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithoutIgnoringLeadingWhitespace() throws ParsingException {
        CsvParser parser = new CsvParser(',', '"', '\\', false, false);
        List<String> result = parser.parseLine("a,  b  ,  \"c\"  ");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("  b  ", result.get(1));
        assertEquals("  \"c\"  ", result.get(2));
    }

    @Test
    public void testParseLineWithStrictQuotes() throws ParsingException {
        CsvParser parser = new CsvParser(',', '"', '\\', true);
        List<String> result = parser.parseLine("\"a\",\"b\"xxx,\"c\"");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithIgnoreQuotations() throws ParsingException {
        CsvParser parser = new CsvParser(',', '"', '\\', false, true, true);
        List<String> result = parser.parseLine("\"a\",\"b\",\"c\"");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineToArray() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] result = parser.parseLineToArray("a,b,c");
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

    @Test
    public void testParseLineToArrayNull() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] result = parser.parseLineToArray(null);
        assertEquals(0, result.length);
    }

    @Test
    public void testParseLineToArray_NotNull() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] result = parser.parseLineToArray("x");
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals("x", result[0]);
    }

    @Test
    public void testParseLineToArrayWithOutput() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] output = new String[3];
        parser.parseLineToArray("a,b,c", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
        assertEquals("c", output[2]);
    }

    @Test
    public void testParseLineToArrayWithOutputNull() {
        CsvParser parser = new CsvParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parseLineToArray("a,b,c", null));
    }

    @Test
    public void testParseLineToArrayWithOutput_QuotedFields() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] output = new String[3];
        parser.parseLineToArray("a,\"b,c\",d", output);
        assertEquals("a", output[0]);
        assertEquals("b,c", output[1]);
        assertEquals("d", output[2]);
    }

    @Test
    public void testParseLineWithUnterminatedQuote() {
        CsvParser parser = new CsvParser();
        assertThrows(ParsingException.class, () -> parser.parseLine("a,\"b,c"));
    }

    @Test
    public void testParseLineWithCustomSeparator() throws ParsingException {
        CsvParser parser = new CsvParser('|');
        List<String> result = parser.parseLine("a|b|c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithCustomQuote() throws ParsingException {
        CsvParser parser = new CsvParser(',', '\'');
        List<String> result = parser.parseLine("a,'b,c',d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b,c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithCustomEscape() throws ParsingException {
        CsvParser parser = new CsvParser(',', '"', '/');
        List<String> result = parser.parseLine("a,\"b/\"c\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\"c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEmptyFields() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,,c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithOnlyCommas() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine(",,,");
        assertEquals(4, result.size());
        for (String field : result) {
            assertEquals("", field);
        }
    }

    @Test
    public void testParseLineComplexExample() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("\"John \"\"Johnny\"\" Doe\",30,\"New York, NY\"");
        assertEquals(3, result.size());
        assertEquals("John \"Johnny\" Doe", result.get(0));
        assertEquals("30", result.get(1));
        assertEquals("New York, NY", result.get(2));
    }

    @Test
    public void testParseLineWithTrailingComma() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,b,c,");
        assertEquals(4, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
        assertEquals("", result.get(3));
    }

    @Test
    public void testParseLineWithNewlineInQuotes() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,\"b\nc\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\nc", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithTabsInQuotes() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,\"b\tc\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\tc", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEmbeddedQuoteInMiddle() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,bc\"d\"ef,g");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("bc\"d\"ef", result.get(1));
        assertEquals("g", result.get(2));
    }

    @Test
    public void testParseLineToArrayWithFewerFieldsThanOutput() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] output = new String[5];
        parser.parseLineToArray("a,b,c", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
        assertEquals("c", output[2]);
        assertNull(output[3]);
        assertNull(output[4]);
    }

    @Test
    public void testParseLineToArrayWithMoreFieldsThanOutput() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] output = new String[2];
        parser.parseLineToArray("a,b ", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
    }

    @Test
    public void testParseLineToArrayWithNullLine() throws ParsingException {
        CsvParser parser = new CsvParser();
        String[] output = new String[3];
        parser.parseLineToArray(null, output);
        // null line should not populate output
        assertNull(output[0]);
    }

    @Test
    public void testConstants() {
        assertEquals(',', CsvParser.DEFAULT_SEPARATOR);
        assertEquals('"', CsvParser.DEFAULT_QUOTE_CHARACTER);
        assertEquals('\\', CsvParser.DEFAULT_ESCAPE_CHARACTER);
        assertFalse(CsvParser.DEFAULT_STRICT_QUOTES);
        assertTrue(CsvParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
        assertFalse(CsvParser.DEFAULT_IGNORE_QUOTATIONS);
        assertEquals('\0', CsvParser.NULL_CHARACTER);
        assertEquals(1024, CsvParser.INITIAL_READ_SIZE);
        assertEquals(128, CsvParser.READ_BUFFER_SIZE);
    }

    @Test
    public void testDoubleQuoteEscaping() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("\"John \"\"Johnny\"\" Doe\",42");
        assertEquals(2, result.size());
        assertEquals("John \"Johnny\" Doe", result.get(0));
        assertEquals("42", result.get(1));
    }

    @Test
    public void testEscapeInsideQuotes() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("\"a\\\"b\",c");
        assertEquals(2, result.size());
        assertEquals("a\"b", result.get(0));
        assertEquals("c", result.get(1));
    }

    @Test
    public void testEscapeOutsideQuotes() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a\\b,c");
        assertEquals(2, result.size());
        // Outside quotes, escape not followed by quote/escape is kept as-is
        assertEquals("a\\b", result.get(0));
        assertEquals("c", result.get(1));
    }

    @Test
    public void testTabSeparator() throws ParsingException {
        CsvParser parser = new CsvParser('\t');
        List<String> result = parser.parseLine("a\tb\tc");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testSingleQuoteChar() throws ParsingException {
        CsvParser parser = new CsvParser(',', '\'', '\\');
        List<String> result = parser.parseLine("'hello,world',test");
        assertEquals(2, result.size());
        assertEquals("hello,world", result.get(0));
        assertEquals("test", result.get(1));
    }

    @Test
    public void testNullQuoteCharAndNullEscape() throws ParsingException {
        CsvParser parser = new CsvParser(',', CsvParser.NULL_CHARACTER, CsvParser.NULL_CHARACTER);
        List<String> result = parser.parseLine("a,b,c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
    }

    @Test
    public void testIgnoreQuotationsWithEmbeddedSeparator() throws ParsingException {
        CsvParser parser = new CsvParser(',', '"', '\\', false, true, true);
        List<String> result = parser.parseLine("a,\"b\",c");
        assertEquals(3, result.size());
        // Quotes are treated as regular chars when ignoreQuotations=true
        assertEquals("a", result.get(0));
    }

    @Test
    public void testStrictQuotesIgnoresUnquoted() throws ParsingException {
        CsvParser parser = new CsvParser(',', '"', '\\', true);
        List<String> result = parser.parseLine("unquoted,\"quoted\"");
        assertEquals(2, result.size());
        assertEquals("", result.get(0)); // strict quotes ignores unquoted text
        assertEquals("quoted", result.get(1));
    }

    @Test
    public void testParseLineWithWhitespaceOnlyField() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("a,   ,c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("", result.get(1)); // leading whitespace trimmed
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineSingleField() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("hello");
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }

    @Test
    public void testParseLineSingleQuotedField() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> result = parser.parseLine("\"hello\"");
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0));
    }
}
