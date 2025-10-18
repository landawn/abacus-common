package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParseException;

@Tag("new-test")
public class CSVParser100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        CSVParser parser = new CSVParser();
        assertEquals(',', parser.getSeparator());
        assertEquals('"', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
        assertFalse(parser.isStrictQuotes());
        assertTrue(parser.isIgnoreLeadingWhiteSpace());
        assertFalse(parser.isIgnoreQuotations());
    }

    @Test
    public void testConstructorWithSeparator() {
        CSVParser parser = new CSVParser('|');
        assertEquals('|', parser.getSeparator());
        assertEquals('"', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
    }

    @Test
    public void testConstructorWithSeparatorAndQuote() {
        CSVParser parser = new CSVParser('|', '\'');
        assertEquals('|', parser.getSeparator());
        assertEquals('\'', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
    }

    @Test
    public void testConstructorWithSeparatorQuoteAndEscape() {
        CSVParser parser = new CSVParser('|', '\'', '/');
        assertEquals('|', parser.getSeparator());
        assertEquals('\'', parser.getQuoteChar());
        assertEquals('/', parser.getEscape());
    }

    @Test
    public void testConstructorWithAllParameters() {
        CSVParser parser = new CSVParser(',', '"', '\\', true, false, true);
        assertEquals(',', parser.getSeparator());
        assertEquals('"', parser.getQuoteChar());
        assertEquals('\\', parser.getEscape());
        assertTrue(parser.isStrictQuotes());
        assertFalse(parser.isIgnoreLeadingWhiteSpace());
        assertTrue(parser.isIgnoreQuotations());
    }

    @Test
    public void testConstructorWithSameCharacters() {
        assertThrows(UnsupportedOperationException.class, () -> new CSVParser(',', ',', '\\'));
        assertThrows(UnsupportedOperationException.class, () -> new CSVParser(',', '"', ','));
        assertThrows(UnsupportedOperationException.class, () -> new CSVParser(',', '"', '"'));
    }

    @Test
    public void testConstructorWithNullSeparator() {
        assertThrows(UnsupportedOperationException.class, () -> new CSVParser(CSVParser.NULL_CHARACTER, '"', '\\'));
    }

    @Test
    public void testParseLineSimple() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,b,c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineNull() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testParseLineEmpty() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("");
        assertEquals(1, result.size());
        assertEquals("", result.get(0));
    }

    @Test
    public void testParseLineWithQuotes() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,\"b,c\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b,c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEscapedQuotes() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,\"b\\\"c\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\"c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEscapedEscape() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,b\\\\c,d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\\\\c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithLeadingWhitespace() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,  b  ,  \"c\" ");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithoutIgnoringLeadingWhitespace() throws ParseException {
        CSVParser parser = new CSVParser(',', '"', '\\', false, false);
        List<String> result = parser.parseLine("a,  b  ,  \"c\"  ");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("  b  ", result.get(1));
        assertEquals("  \"c\"  ", result.get(2));
    }

    @Test
    public void testParseLineWithStrictQuotes() throws ParseException {
        CSVParser parser = new CSVParser(',', '"', '\\', true);
        List<String> result = parser.parseLine("\"a\",\"b\"xxx,\"c\"");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithIgnoreQuotations() throws ParseException {
        CSVParser parser = new CSVParser(',', '"', '\\', false, true, true);
        List<String> result = parser.parseLine("\"a\",\"b\",\"c\"");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineToArray() throws ParseException {
        CSVParser parser = new CSVParser();
        String[] result = parser.parseLineToArray("a,b,c");
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

    @Test
    public void testParseLineToArrayNull() throws ParseException {
        CSVParser parser = new CSVParser();
        String[] result = parser.parseLineToArray(null);
        assertEquals(0, result.length);
    }

    @Test
    public void testParseLineToArrayWithOutput() throws ParseException {
        CSVParser parser = new CSVParser();
        String[] output = new String[3];
        parser.parseLineToArray("a,b,c", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
        assertEquals("c", output[2]);
    }

    @Test
    public void testParseLineToArrayWithOutputNull() {
        CSVParser parser = new CSVParser();
        assertThrows(IllegalArgumentException.class, () -> parser.parseLineToArray("a,b,c", null));
    }

    @Test
    public void testParseLineWithUnterminatedQuote() {
        CSVParser parser = new CSVParser();
        assertThrows(ParseException.class, () -> parser.parseLine("a,\"b,c"));
    }

    @Test
    public void testParseLineWithCustomSeparator() throws ParseException {
        CSVParser parser = new CSVParser('|');
        List<String> result = parser.parseLine("a|b|c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithCustomQuote() throws ParseException {
        CSVParser parser = new CSVParser(',', '\'');
        List<String> result = parser.parseLine("a,'b,c',d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b,c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithCustomEscape() throws ParseException {
        CSVParser parser = new CSVParser(',', '"', '/');
        List<String> result = parser.parseLine("a,\"b/\"c\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\"c", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEmptyFields() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,,c");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testParseLineWithOnlyCommas() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine(",,,");
        assertEquals(4, result.size());
        for (String field : result) {
            assertEquals("", field);
        }
    }

    @Test
    public void testParseLineComplexExample() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("\"John \"\"Johnny\"\" Doe\",30,\"New York, NY\"");
        assertEquals(3, result.size());
        assertEquals("John \"Johnny\" Doe", result.get(0));
        assertEquals("30", result.get(1));
        assertEquals("New York, NY", result.get(2));
    }

    @Test
    public void testParseLineWithTrailingComma() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,b,c,");
        assertEquals(4, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
        assertEquals("", result.get(3));
    }

    @Test
    public void testParseLineWithNewlineInQuotes() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,\"b\nc\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\nc", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithTabsInQuotes() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,\"b\tc\",d");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b\tc", result.get(1));
        assertEquals("d", result.get(2));
    }

    @Test
    public void testParseLineWithEmbeddedQuoteInMiddle() throws ParseException {
        CSVParser parser = new CSVParser();
        List<String> result = parser.parseLine("a,bc\"d\"ef,g");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("bc\"d\"ef", result.get(1));
        assertEquals("g", result.get(2));
    }

    @Test
    public void testParseLineToArrayWithFewerFieldsThanOutput() throws ParseException {
        CSVParser parser = new CSVParser();
        String[] output = new String[5];
        parser.parseLineToArray("a,b,c", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
        assertEquals("c", output[2]);
        assertNull(output[3]);
        assertNull(output[4]);
    }

    @Test
    public void testParseLineToArrayWithMoreFieldsThanOutput() throws ParseException {
        CSVParser parser = new CSVParser();
        String[] output = new String[2];
        parser.parseLineToArray("a,b ", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
    }
}
