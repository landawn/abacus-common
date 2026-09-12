package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;

public class CsvParserTest extends TestBase {

    @Test
    public void testConstructor() {
        CsvParser def = new CsvParser();
        assertEquals(',', def.getSeparator());
        assertEquals('"', def.getQuoteChar());
        assertEquals(CsvParser.NULL_CHARACTER, def.getEscape());
        assertFalse(def.isStrictQuotes());
        assertTrue(def.isIgnoreLeadingWhitespace());
        assertFalse(def.isIgnoreQuotations());

        CsvParser sep = new CsvParser('|');
        assertEquals('|', sep.getSeparator());
        assertEquals('"', sep.getQuoteChar());

        CsvParser quote = new CsvParser('|', '\'');
        assertEquals('|', quote.getSeparator());
        assertEquals('\'', quote.getQuoteChar());

        CsvParser escape = new CsvParser('|', '\'', '/');
        assertEquals('/', escape.getEscape());

        CsvParser strict = new CsvParser(',', '"', '\\', true);
        assertTrue(strict.isStrictQuotes());
        assertTrue(strict.isIgnoreLeadingWhitespace());

        CsvParser noTrim = new CsvParser(',', '"', '\\', false, false);
        assertFalse(noTrim.isStrictQuotes());
        assertFalse(noTrim.isIgnoreLeadingWhitespace());

        CsvParser all = new CsvParser(',', '"', '\\', true, false, true);
        assertTrue(all.isStrictQuotes());
        assertFalse(all.isIgnoreLeadingWhitespace());
        assertTrue(all.isIgnoreQuotations());

        assertEquals('\t', new CsvParser('\t').getSeparator());
        assertEquals('\'', new CsvParser(',', '\'').getQuoteChar());
        assertEquals('/', new CsvParser(',', '"', '/').getEscape());
        assertTrue(new CsvParser(',', '"', '\\', false, true, true).isIgnoreQuotations());
    }

    @Test
    public void testConstructor_Invalid() {
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(',', ',', '\\'));
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(',', '"', ','));
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(',', '"', '"'));
        assertThrows(UnsupportedOperationException.class, () -> new CsvParser(CsvParser.NULL_CHARACTER, '"', '\\'));
    }

    @Test
    public void testConstants() {
        assertEquals(',', CsvParser.DEFAULT_SEPARATOR);
        assertEquals('"', CsvParser.DEFAULT_QUOTE_CHARACTER);
        assertEquals(CsvParser.NULL_CHARACTER, CsvParser.DEFAULT_ESCAPE_CHARACTER);
        assertFalse(CsvParser.DEFAULT_STRICT_QUOTES);
        assertTrue(CsvParser.DEFAULT_IGNORE_LEADING_WHITESPACE);
        assertFalse(CsvParser.DEFAULT_IGNORE_QUOTATIONS);
        assertEquals('\0', CsvParser.NULL_CHARACTER);
        assertEquals(1024, CsvParser.INITIAL_READ_SIZE);
        assertEquals(128, CsvParser.READ_BUFFER_SIZE);
    }

    @Test
    public void testParseLine() throws ParsingException {
        CsvParser parser = new CsvParser();
        assertEquals(List.of("a", "b", "c"), parser.parseLine("a,b,c"));
        assertTrue(parser.parseLine(null).isEmpty());
        assertEquals(List.of(""), parser.parseLine(""));
        assertEquals(List.of("hello"), parser.parseLine("hello"));
        assertEquals(List.of("hello"), parser.parseLine("\"hello\""));
        assertEquals(List.of("a", "", "c"), parser.parseLine("a,,c"));
        assertEquals(List.of("", "", "", ""), parser.parseLine(",,,"));
        assertEquals(List.of("a", "b", "c", ""), parser.parseLine("a,b,c,"));
        assertEquals(List.of("a", "", "c"), parser.parseLine("a,   ,c"));
        assertEquals(List.of("a;b", "c;d"), parser.parseLine("a;b,c;d"));
        assertEquals(List.of("a", "b", "c"), new CsvParser('|').parseLine("a|b|c"));
        assertEquals(List.of("a", "b", "c"), new CsvParser(';').parseLine("a;b;c"));
        assertEquals(List.of("a", "b", "c"), new CsvParser('\t').parseLine("a\tb\tc"));
        assertEquals(List.of("a\0b", "c\0d"), new CsvParser(',', CsvParser.NULL_CHARACTER, CsvParser.NULL_CHARACTER).parseLine("a\0b,c\0d"));
        assertEquals(List.of("a", "b", "c"), new CsvParser(',', CsvParser.NULL_CHARACTER, CsvParser.NULL_CHARACTER).parseLine("a,b,c"));
    }

    @Test
    public void testParseLine_QuotesAndEscape() throws ParsingException {
        CsvParser parser = new CsvParser();
        assertEquals(List.of("a", "b,c", "d"), parser.parseLine("a,\"b,c\",d"));
        assertEquals(List.of("John \"Johnny\" Doe", "42"), parser.parseLine("\"John \"\"Johnny\"\" Doe\",42"));
        assertEquals(List.of("John \"Johnny\" Doe", "30", "New York, NY"), parser.parseLine("\"John \"\"Johnny\"\" Doe\",30,\"New York, NY\""));
        assertEquals(List.of("He said \"hi\" today", "x"), parser.parseLine("\"He said \"\"hi\"\" today\",x"));
        assertEquals(List.of("a\"\"b", "c"), parser.parseLine("a\"\"b,c"));
        assertEquals(List.of("a", "bc\"d\"ef", "g"), parser.parseLine("a,bc\"d\"ef,g"));
        assertEquals(List.of("abc\"", "d"), parser.parseLine("abc\",d"));
        assertEquals(List.of("cleandirty", "text"), parser.parseLine("\"clean\"dirty,\"text\""));
        assertEquals(List.of("ax", "b"), parser.parseLine("\"a\"x,b"));
        assertEquals(List.of("a", "b\nc", "d"), parser.parseLine("a,\"b\nc\",d"));
        assertEquals(List.of("a", "b\tc", "d"), parser.parseLine("a,\"b\tc\",d"));
        assertEquals(List.of("a", "line1\r\nline2", "c"), parser.parseLine("a,\"line1\r\nline2\",c"));
        assertEquals(List.of("a", "x\ry", "b"), parser.parseLine("a,\"x\ry\",b"));
        assertEquals(List.of("  abc  ", "y"), parser.parseLine("\"  abc  \",y"));

        CsvParser backslash = new CsvParser(',', '"', '\\');
        assertEquals(List.of("a\"b", "c"), backslash.parseLine("\"a\\\"b\",c"));
        assertEquals(List.of("a", "b\"c", "d"), backslash.parseLine("a,\"b\\\"c\",d"));
        assertEquals(List.of("a\\b", "c"), parser.parseLine("a\\b,c"));
        assertEquals(List.of("a", "b\\\\c", "d"), parser.parseLine("a,b\\\\c,d"));
        assertEquals(List.of("hello,world", "test"), new CsvParser(',', '\'', '\\').parseLine("'hello,world',test"));
        assertEquals(List.of("It's ok", "2"), new CsvParser(',', '\'', '\\').parseLine("'It''s ok',2"));
        assertEquals(List.of("a", "b\"c", "d"), new CsvParser(',', '"', '/').parseLine("a,\"b/\"c\",d"));

        CsvParser ignoreQuotes = new CsvParser(',', '"', '\\', false, true, true);
        assertEquals(3, ignoreQuotes.parseLine("a,\"b\",c").size());
        assertEquals(List.of("a", "b", "c"), ignoreQuotes.parseLine("\"a\",\"b\",\"c\""));
        // With ignoreQuotations on, a quoted region protects neither a separator nor an escape:
        // the separator still splits, the escape stays literal and a doubled quote is not collapsed
        // (contrast the quoted-region results asserted for parser/backslash above).
        assertEquals(List.of("a,b", "c"), parser.parseLine("\"a,b\",c"));
        assertEquals(List.of("a", "b", "c"), ignoreQuotes.parseLine("\"a,b\",c"));
        assertEquals(List.of("a\\\"b", "c"), ignoreQuotes.parseLine("\"a\\\"b\",c"));
        assertEquals(List.of("a\"b"), parser.parseLine("\"a\"\"b\""));
        assertEquals(List.of("a\"\"b"), ignoreQuotes.parseLine("\"a\"\"b\""));

        CsvParser strict = new CsvParser(',', '"', '\\', true);
        assertEquals(List.of("", "quoted"), strict.parseLine("unquoted,\"quoted\""));
        assertEquals(List.of("a", "b", "c"), strict.parseLine("\"a\",\"b\"xxx,\"c\""));

        assertThrows(ParsingException.class, () -> parser.parseLine("a,\"b,c"));
        assertThrows(ParsingException.class, () -> backslash.parseLine("a,\"b\\\" "));
    }

    @Test
    public void testParseLine_Whitespace() throws ParsingException {
        CsvParser parser = new CsvParser();
        assertEquals(List.of("a", "b", "c"), parser.parseLine("a,  b  ,  \"c\" "));
        assertEquals(42, Integer.parseInt(parser.parseLine("  42  ,x").get(0)));
        assertEquals(List.of("a", "b", "c"), parser.parseLine("a,\"b\" ,c"));
        assertEquals(List.of("a", "b", "c"), parser.parseLine("a, \"b\" ,c"));

        CsvParser noTrim = new CsvParser(',', '"', '\\', false, false);
        assertEquals(List.of("a", "  b  ", "  \"c\"  "), noTrim.parseLine("a,  b  ,  \"c\"  "));
        assertEquals("  42  ", noTrim.parseLine("  42  ,x").get(0));
        assertThrows(NumberFormatException.class, () -> Integer.parseInt(noTrim.parseLine("  42  ,x").get(0)));
    }

    @Test
    public void testParseLine_WhitespaceSeparator() throws ParsingException {
        CsvParser tab = new CsvParser('\t');
        assertEquals(Arrays.asList("a", "", "b"), tab.parseLine("a\t\tb"));
        assertEquals(Arrays.asList("", "", ""), tab.parseLine("\t\t"));
        assertEquals(Arrays.asList("a", "", "", "b"), tab.parseLine("a\t\t\tb"));
        assertEquals(Arrays.asList("a", "", "b"), tab.parseLine("a\t \tb"));
        assertEquals(Arrays.asList("a", "b"), tab.parseLine("a\t  b"));

        CsvParser space = new CsvParser(' ');
        assertEquals(Arrays.asList("a", "", "b"), space.parseLine("a  b"));
        assertEquals(Arrays.asList("a", "b"), space.parseLine("a b"));
    }

    @Test
    public void testParseLineToArray() throws ParsingException {
        CsvParser parser = new CsvParser();
        assertEquals(List.of("a", "b", "c"), Arrays.asList(parser.parseLineToArray("a,b,c")));
        assertEquals(0, parser.parseLineToArray(null).length);
        assertEquals(List.of("x"), Arrays.asList(parser.parseLineToArray("x")));

        String[] output = new String[3];
        parser.parseLineInto("a,b,c", output);
        assertEquals(List.of("a", "b", "c"), Arrays.asList(output));
        parser.parseLineInto("a,\"b,c\",d", output);
        assertEquals(List.of("a", "b,c", "d"), Arrays.asList(output));

        String[] larger = new String[5];
        parser.parseLineInto("a,b,c", larger);
        assertEquals("a", larger[0]);
        assertEquals("c", larger[2]);
        assertNull(larger[3]);
        assertNull(larger[4]);

        String[] smaller = new String[2];
        parser.parseLineInto("a,b ", smaller);
        assertEquals("a", smaller[0]);
        assertEquals("b", smaller[1]);

        String[] nullLine = new String[3];
        parser.parseLineInto(null, nullLine);
        assertNull(nullLine[0]);

        assertThrows(IllegalArgumentException.class, () -> parser.parseLineInto("a,b,c", (String[]) null));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> parser.parseLineInto("a,b,c,d", new String[2]));

        String[] tabOut = new String[3];
        new CsvParser('\t').parseLineInto("a\t\tb", tabOut);
        assertEquals("a", tabOut[0]);
        assertEquals("", tabOut[1]);
        assertEquals("b", tabOut[2]);
    }

    @Test
    public void testParseLineIntoCollection() throws ParsingException {
        CsvParser parser = new CsvParser();
        List<String> output = new ArrayList<>();
        output.add("existing");
        parser.parseLineInto("a,b,c", output);
        assertEquals(List.of("existing", "a", "b", "c"), output);

        parser.parseLineInto("d,\"e,f\"", output);
        assertEquals(List.of("existing", "a", "b", "c", "d", "e,f"), output);

        parser.parseLineInto(null, output);
        assertEquals(List.of("existing", "a", "b", "c", "d", "e,f"), output);

        List<String> emptyLine = new ArrayList<>();
        parser.parseLineInto("", emptyLine);
        assertEquals(List.of(""), emptyLine);

        List<String> failed = new ArrayList<>();
        failed.add("keep");
        assertThrows(ParsingException.class, () -> parser.parseLineInto("\"unterminated", failed));
        assertEquals(List.of("keep"), failed);

        HashSet<String> set = new HashSet<>();
        parser.parseLineInto("a,b,a", set);
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));

        assertThrows(IllegalArgumentException.class, () -> parser.parseLineInto("a,b,c", (List<String>) null));
        assertThrows(UnsupportedOperationException.class, () -> parser.parseLineInto("a,b", ImmutableList.of("z")));
        parser.parseLineInto(null, ImmutableList.of("z"));

        List<String> tabOut = new ArrayList<>();
        new CsvParser('\t').parseLineInto("a\t\tb", tabOut);
        assertEquals(List.of("a", "", "b"), tabOut);
    }

    @Test
    public void testParseLineIntoValidatesAndParsesBeforeInsertion() {
        final CsvParser parser = new CsvParser();
        final List<String> output = new ArrayList<>(List.of("keep"));

        // A malformed later field must not append the earlier, valid field.
        assertThrows(ParsingException.class, () -> parser.parseLineInto("first,\"unterminated", output));
        assertEquals(List.of("keep"), output);
        assertThrows(ParsingException.class, () -> parser.parseLineInto("first,\"unterminated", List.of("keep")));
        assertThrows(IllegalArgumentException.class, () -> parser.parseLineInto(null, (List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> parser.parseLineInto("\"unterminated", (List<String>) null));
        assertThrows(UnsupportedOperationException.class, () -> parser.parseLineInto("", List.of()));
    }

    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testParseLineIntoPropagatesCollectionRestrictions() {
        final CsvParser parser = new CsvParser();
        final java.util.concurrent.ArrayBlockingQueue<String> bounded = new java.util.concurrent.ArrayBlockingQueue<>(1);

        // Insertion failure can leave an already parsed prefix in the destination.
        assertThrows(IllegalStateException.class, () -> parser.parseLineInto("a,b", bounded));
        assertEquals(List.of("a"), new ArrayList<>(bounded));
        final java.util.Collection<String> checked = (java.util.Collection) java.util.Collections.checkedCollection(new ArrayList<Integer>(), Integer.class);
        assertThrows(ClassCastException.class, () -> parser.parseLineInto("1", checked));
        assertTrue(checked.isEmpty());
    }

    @Test
    public void testParseLine_IgnoreQuotationsConsumesDelimitingQuote() throws ParsingException {
        CsvParser keepQuotes = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, true, false);
        CsvParser ignoreQuotes = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, true, true);

        // A quote at the end of the line is data for the unquoted-field rule, a delimiter when
        // ignoreQuotations is on.
        assertEquals(List.of("ab\""), keepQuotes.parseLine("ab\""));
        assertEquals(List.of("ab"), ignoreQuotes.parseLine("ab\""));

        // Same split for a quote sitting immediately before a separator.
        assertEquals(List.of("ab\"", "c"), keepQuotes.parseLine("ab\",c"));
        assertEquals(List.of("ab", "c"), ignoreQuotes.parseLine("ab\",c"));

        // A quote that opens a field has no field data before it, so it is consumed as well.
        assertEquals(List.of("ab"), ignoreQuotes.parseLine("\"ab"));
        assertEquals(List.of("", "y"), ignoreQuotes.parseLine(",\"y"));

        // A quote surrounded by field data on both sides is kept by both rules.
        assertEquals(List.of("a", "bc\"d\"ef", "g"), keepQuotes.parseLine("a,bc\"d\"ef,g"));
        assertEquals(List.of("a", "bc\"d\"ef", "g"), ignoreQuotes.parseLine("a,bc\"d\"ef,g"));
    }
}
