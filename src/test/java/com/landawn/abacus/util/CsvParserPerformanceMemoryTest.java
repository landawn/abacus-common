package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for CsvParser. */
@Tag("slow-test")
public class CsvParserPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G015 end ----
    // ---- perf review 2026-10-08 G017 begin ----
    @Nested
    class G017 {

        // G017-01: unquoted fields with surrounding whitespace are stripped (now from the builder, one String per field)
        @Test
        void testParseLine_paddedUnquotedFields() {
            final CsvParser parser = new CsvParser();

            assertEquals(Arrays.asList("a", "b", "c", "d"), parser.parseLine("a , b ,c  ,  d  "));
            assertEquals(Arrays.asList("John", "Smith", "42"), parser.parseLine("John      ,Smith     ,42        "));
            assertEquals(Arrays.asList("New York", "x"), parser.parseLine("New York  , x"));
            assertEquals(Arrays.asList("", "", ""), parser.parseLine(" , ,"));
            assertEquals(Arrays.asList(""), parser.parseLine("   "));
            assertEquals(Arrays.asList(""), parser.parseLine(""));
            assertEquals(Arrays.asList("a", "b"), parser.parseLine("a,b   "));
            assertEquals(Arrays.asList("x", "y"), parser.parseLine("x \u001F,y"));
            // quoted fields keep their inner whitespace; whitespace around the quotes is dropped
            assertEquals(Arrays.asList(" a ", "b "), parser.parseLine("\" a \",\"b \""));
            assertEquals(Arrays.asList("a", "b"), parser.parseLine("\"a\"  ,  \"b\"  "));
        }

        // G017-01: Character.isWhitespace decides what is stripped (incl. non-Latin-1 whitespace); N\P is data
        @Test
        void testParseLine_unicodeWhitespaceAndNonLatin1Fields() {
            final CsvParser parser = new CsvParser();

            assertEquals(Arrays.asList("a", "b"), parser.parseLine("\u2003a\u2003,\u3000b\u3000"));
            assertEquals(Arrays.asList("\u00A0a\u00A0", "c"), parser.parseLine("\u00A0a\u00A0,c"));
            assertEquals(Arrays.asList("\u4E2D\u6587", "\u00E9"), parser.parseLine("\u4E2D\u6587 , \u00E9 "));
        }

        // G017-01: leading whitespace inside the field buffer (quote characters consumed by ignoreQuotations) is stripped too
        @Test
        void testParseLine_ignoreQuotationsLeadingWhitespaceInField() {
            final CsvParser ignoreQuotes = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, true, true);

            assertEquals(Arrays.asList("a", "b", "c"), ignoreQuotes.parseLine("a,\" b \",c"));
            assertEquals(Arrays.asList("a", "b"), ignoreQuotes.parseLine("\" a \",\"b \""));
        }

        // G017-01: nothing is stripped when ignoreLeadingWhitespace is off
        @Test
        void testParseLine_noTrimKeepsWhitespace() {
            final CsvParser noTrim = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, false);

            assertEquals(Arrays.asList("a ", " b ", "c  ", "  d  "), noTrim.parseLine("a , b ,c  ,  d  "));
            assertEquals(Arrays.asList("   "), noTrim.parseLine("   "));
            assertEquals(Arrays.asList("\u2003a\u2003", "\u3000b\u3000"), noTrim.parseLine("\u2003a\u2003,\u3000b\u3000"));
        }

        // G017-01: the array/collection overloads strip the same way; an overflow still fails after the leading slots are written
        @Test
        void testParseLineInto_paddedFields() {
            final CsvParser parser = new CsvParser();

            final String[] output = new String[4];
            Arrays.fill(output, "#");
            parser.parseLineInto("x  , y  ,z", output);
            assertArrayEquals(new String[] { "x", "y", "z", "#" }, output);

            assertArrayEquals(new String[] { "p", "q", "r" }, parser.parseLineToArray("  p  ,q  ,  r"));

            final List<String> collected = new ArrayList<>(Arrays.asList("keep"));
            parser.parseLineInto(" m ,n  ", collected);
            assertEquals(Arrays.asList("keep", "m", "n"), collected);

            final String[] small = new String[2];
            Arrays.fill(small, "#");
            assertThrows(ArrayIndexOutOfBoundsException.class, () -> parser.parseLineInto("x  , y  , z  ", small));
            assertArrayEquals(new String[] { "x", "y" }, small);
        }

        // G017-01: stripped values are ordinary equal Strings, including empty and non-Latin-1 results
        @Test
        void testParseLine_strippedValuesAreOrdinaryStrings() {
            final List<String> fields = new CsvParser().parseLine("  ab  ,   ,\u4E2D  ");

            assertEquals(3, fields.size());
            assertEquals("ab", fields.get(0));
            assertEquals("ab".hashCode(), fields.get(0).hashCode());
            assertTrue(fields.get(1).isEmpty());
            assertEquals("\u4E2D", fields.get(2));
            assertEquals(1, fields.get(2).length());
        }

        // G017-02: runs of ordinary characters are copied in bulk - fields longer than the pooled builder, non-Latin-1 inside a run
        @Test
        void testParseLine_longRunsAndNonLatin1() {
            final CsvParser parser = new CsvParser();
            final String big = Strings.repeat('x', 20000) + "\u4E2D" + Strings.repeat('y', 20000);

            final List<String> fields = parser.parseLine(big + ",tail," + big);
            assertEquals(3, fields.size());
            assertEquals(big, fields.get(0));
            assertEquals("tail", fields.get(1));
            assertEquals(big, fields.get(2));

            final String quotedBody = Strings.repeat("a,b ", 5000);
            assertEquals(Arrays.asList(quotedBody, "z"), parser.parseLine("\"" + quotedBody + "\",z"));

            // a UTF-16 field followed by Latin-1 fields that reuse the same builder
            assertEquals(Arrays.asList("\u00E9t\u00E9 \u4E2D\u6587 ok", "plain", "x"), parser.parseLine("\u00E9t\u00E9 \u4E2D\u6587 ok,plain,x"));

            final String[] output = new String[2];
            parser.parseLineInto(big + "," + big, output);
            assertArrayEquals(new String[] { big, big }, output);
        }

        // G017-02: a run stops at every escape, quote and unquoted separator; separators inside quotes stay data
        @Test
        void testParseLine_runBoundaries() {
            final CsvParser escapeParser = new CsvParser(',', '"', '\\');

            assertEquals(Arrays.asList("ab\"cd\\ef", "g"), escapeParser.parseLine("\"ab\\\"cd\\\\ef\",g"));
            assertEquals(Arrays.asList("a\\b", "c"), escapeParser.parseLine("a\\b,c"));
            assertEquals(Arrays.asList("a,b,c", "d"), new CsvParser().parseLine("\"a,b,c\",d"));
            assertEquals(Arrays.asList("a\u0000b", "c"), new CsvParser().parseLine("a\u0000b,c"));
            assertEquals(Arrays.asList("bc\"d\"ef", "g"), new CsvParser().parseLine("bc\"d\"ef,g"));

            // strict quotes: characters outside the quotes are still dropped
            final CsvParser strict = new CsvParser(',', '"', '\\', true);
            assertEquals(Arrays.asList("in side", "q", ""), strict.parseLine("xx\"in side\"yy,\"q\",zz"));

            // ignoreQuotations: quote characters end a run, embedded ones are kept as data
            final CsvParser ignoreQuotes = new CsvParser(',', '"', CsvParser.NULL_CHARACTER, false, true, true);
            assertEquals(Arrays.asList("ab\"cd", "e"), ignoreQuotes.parseLine("ab\"cd,e"));
        }
    }
}
