package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;

/**
 * Pins CSV record framing where records, line terminators and doubled quotes straddle the blocks CsvUtil reads
 * (256 characters first, doubling up to 8192 per read). Every case is read through a pooled reader (plain Reader
 * source), a caller-supplied BufferedReader, and a source that returns one character per read.
 */
public class CsvRecordFramingBufferTest extends TestBase {

    private static final int BLOCK = 8192;

    /** Where the block boundaries fall for a source that fills every read: 256, +512, +1024, ... then every 8192. */
    private static final int[] BOUNDARIES = { 256, 768, 1792, 3840, 7936, 16128 };

    /** Returns one character per read, so every block boundary falls somewhere else. */
    private static final class OneCharReader extends Reader {
        private final String text;
        private int position;

        OneCharReader(final String text) {
            this.text = text;
        }

        @Override
        public int read(final char[] buffer, final int offset, final int length) {
            if (position >= text.length()) {
                return -1;
            }

            buffer[offset] = text.charAt(position++);
            return 1;
        }

        @Override
        public void close() {
        }
    }

    private static List<Function<String, Reader>> sources() {
        return List.of(StringReader::new, text -> new BufferedReader(new StringReader(text)), OneCharReader::new);
    }

    @Test
    public void testLineTerminatorsStraddlingTheBlockBoundary() {
        for (final String newline : List.of("\n", "\r", "\r\n")) {
            final String header = "h1,h2" + newline;

            // Moves the end of the first data row across each block boundary, one character at a time.
            for (final int boundary : BOUNDARIES) {
                for (int length = boundary - header.length() - 8; length <= boundary - header.length() + 4; length++) {
                    final String first = Strings.repeat('x', length - 3);
                    final String csv = header + first + ",v1" + newline + "a,b" + newline;

                    for (final Function<String, Reader> source : sources()) {
                        final Dataset data = CsvUtil.load(source.apply(csv));

                        assertEquals(2, data.size(), () -> "length " + csv.length());
                        assertEquals(first, data.get(0, 0));
                        assertEquals("v1", data.get(0, 1));
                        assertEquals("a", data.get(1, 0));
                        assertEquals("b", data.get(1, 1));
                    }
                }
            }
        }
    }

    @Test
    public void testQuotedLineBreaksAndDoubledQuotesAcrossTheBlockBoundary() {
        for (final String newline : List.of("\n", "\r", "\r\n")) {
            for (final int boundary : BOUNDARIES) {
                // The doubled quote and the quoted line break sit at 7 + padding.
                for (int padding = boundary - 16; padding <= boundary - 3; padding++) {
                    final String value = Strings.repeat('p', padding) + "\"" + newline + "\"q";
                    final String csv = "h1,h2\n\"" + value.replace("\"", "\"\"") + "\",tail\nnext,row\n";

                    for (final Function<String, Reader> source : sources()) {
                        final Dataset data = CsvUtil.load(source.apply(csv));

                        assertEquals(2, data.size());
                        assertEquals(value, data.get(0, 0));
                        assertEquals("tail", data.get(0, 1));
                        assertEquals("next", data.get(1, 0));
                    }
                }
            }
        }
    }

    @Test
    public void testRecordsSpanningManyBlocks() {
        final String quoted = Strings.repeat("line\r\nwith \"\"quote\"\", ", 20_000);
        final String unquoted = Strings.repeat('u', 5 * BLOCK + 17);
        final String csv = "\uFEFFid,text\n" + unquoted + ",\"" + quoted + "\"\r\n1,2";
        final String expected = quoted.replace("\"\"", "\"");

        for (final Function<String, Reader> source : sources()) {
            final Dataset data = CsvUtil.load(source.apply(csv));

            assertEquals(List.of("id", "text"), data.columnNames());
            assertEquals(2, data.size());
            assertEquals(unquoted, data.get(0, 0));
            assertEquals(expected, data.get(0, 1));
            assertEquals("1", data.get(1, 0));
            assertEquals("2", data.get(1, 1));

            final StringWriter json = new StringWriter();
            assertEquals(2, CsvUtil.csvToJson(source.apply(csv), null, json, null));
        }
    }

    @Test
    public void testOffsetCountAndEmptyLinesAcrossBlocks() {
        final StringBuilder csv = new StringBuilder("n\r\n");

        for (int i = 0; i < 5000; i++) {
            csv.append(i).append(i % 7 == 0 ? "\r" : "\r\n");
        }

        for (final Function<String, Reader> source : sources()) {
            final Dataset all = CsvUtil.load(source.apply(csv.toString()));
            assertEquals(5000, all.size());
            assertEquals("4999", all.get(4999, 0));

            final Dataset page = CsvUtil.load(source.apply(csv.toString()), null, 4000, 3);
            assertEquals(3, page.size());
            assertEquals("4000", page.get(0, 0));
            assertEquals("4002", page.get(2, 0));
        }

        // "\r\r\n" is a CR line end followed by an empty CRLF line; the same framing as before the block reads.
        assertEquals(3, CsvUtil.load(new StringReader("n\ra\r\r\nb")).size());
    }

    @Test
    public void testUnterminatedQuoteAcrossBlocks() {
        final String csv = "h\n\"" + Strings.repeat('z', 3 * BLOCK);

        for (final Function<String, Reader> source : sources()) {
            final ParsingException e = assertThrows(ParsingException.class, () -> CsvUtil.load(source.apply(csv)));

            assertTrue(e.getMessage().startsWith("Un-terminated quoted field at end of CSV input: \"zzz"), e.getMessage());
            assertTrue(e.getMessage().endsWith("..."), e.getMessage());
        }
    }

    /**
     * The loaders split data records for CSV_LINE_PARSER on the framed characters; every field rule must match
     * what CsvParser gives for the same line.
     */
    @Test
    public void testDefaultLineParserFieldRulesMatchCsvParser() {
        final String[] lines = { "a,b,c", " a , b ,c ", "\"a\",\"b\"", " \"a\" , \"b\" ", "\"a,b\",c", "\"a\"\"b\",c", "\"\"\"\"\"\"", "\"\",\"\",",
                "\"ab\"cd,e", "\"ab\" cd ,e", "\"ab\"  \"cd\",e", "ab\"cd\"e,f", "a\"b,c", "\"a\"\"\" x,y", ",,", " , , ", "", "   ",
                "\u00A0a\u00A0,\u2003b", "\"line\r\nbreak\",\"cr\ronly\"", "\"\" x", "x\"", "\"a\"\t", "q,\"\"" };

        for (final String line : lines) {
            final String[] expected = new CsvParser().parseLineToArray(line);
            final StringBuilder header = new StringBuilder();

            for (int i = 0; i < expected.length; i++) {
                header.append(i == 0 ? "" : ",").append('h').append(i);
            }

            for (final Function<String, Reader> source : sources()) {
                final Dataset data = CsvUtil.load(source.apply(header + "\n" + line + "\n"));
                final List<Object> actual = new ArrayList<>();

                for (int i = 0; i < expected.length; i++) {
                    actual.add(data.get(0, i));
                }

                assertArrayEquals(expected, actual.toArray(), line);
            }
        }
    }

    @Test
    public void testDefaultLineParserFailuresMatchCsvParser() {
        final ParsingException tooMany = assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader("h\na,\"b\"")));
        assertEquals("CSV data row has more fields than the expected 1 column(s): a,\"b\"", tooMany.getMessage());
        assertTrue(tooMany.getCause() instanceof ArrayIndexOutOfBoundsException);
        assertEquals("Index 1 out of bounds for length 1", tooMany.getCause().getMessage());

        // Skipped records are framed but not parsed, so a ragged record before the offset is not an error.
        assertEquals("c", CsvUtil.load(new StringReader("h\na,b\nc"), null, 1, Long.MAX_VALUE).get(0, 0));
        assertEquals(List.of("c"), CsvUtil.stream(new StringReader("h\na,b\nc"), null, 1, Long.MAX_VALUE, it -> true, (names, row) -> row.get(0), false)
                .toList());
    }

    /** The retention limit of CsvRecordReader's reusable buffers (MAX_RETAINED_RECORD_CAPACITY). */
    private static final int MAX_RETAINED = 64 * 1024;

    private static String hugeRowsCsv() {
        final StringBuilder csv = new StringBuilder("a,b\n");
        csv.append(Strings.repeat('p', 300_000)).append(",plain\n");
        csv.append('"').append(Strings.repeat("e\"\"", 150_000)).append("\",escaped\n");
        csv.append('"').append(Strings.repeat('s', 200_000)).append("\" tail,suffix\n");

        for (int i = 0; i < 50; i++) {
            csv.append("r").append(i).append(",\"q\"\"").append(i).append("\"\n");
        }

        return csv.toString();
    }

    @Test
    public void testLargeRecordsAndEscapedFieldsFollowedBySmallRows() {
        final String csv = hugeRowsCsv();

        for (final Function<String, Reader> source : sources()) {
            final Dataset data = CsvUtil.load(source.apply(csv));

            assertEquals(53, data.size());
            assertEquals(Strings.repeat('p', 300_000), data.get(0, 0));
            assertEquals(Strings.repeat("e\"", 150_000), data.get(1, 0));
            assertEquals("escaped", data.get(1, 1));
            assertEquals(Strings.repeat('s', 200_000) + " tail", data.get(2, 0));
            assertEquals("r0", data.get(3, 0));
            assertEquals("q\"0", data.get(3, 1));
            assertEquals("r49", data.get(52, 0));
            assertEquals("q\"49", data.get(52, 1));

            final List<String> streamed = CsvUtil.stream(source.apply(csv), null, 2, Long.MAX_VALUE, it -> true, (names, row) -> row.get(1), false)
                    .toList();
            assertEquals(51, streamed.size());
            assertEquals("suffix", streamed.get(0));
            assertEquals("q\"49", streamed.get(50));
        }
    }

    /**
     * A huge record or field must not keep CsvRecordReader's buffers alive once the reader has moved on: the quoted-field
     * builder is released as soon as its value is taken, and the spanned-record copy and record builder when the next
     * record is framed.
     */
    @Test
    public void testOversizedBuffersAreReleasedOnceNoLongerNeeded() throws Exception {
        final CsvUtil.CsvRecordReader reader = new CsvUtil.CsvRecordReader(new StringReader(hugeRowsCsv()));
        final String[] row = new String[2];

        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER)); // header
        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER)); // 300,000-char plain record
        reader.parseRecordInto(row);
        assertTrue(capacityOf(reader, "recordBuilder") <= MAX_RETAINED, "record builder kept after copying a huge record");

        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER)); // huge escaped field
        reader.parseRecordInto(row);
        assertEquals("escaped", row[1]);
        assertTrue(capacityOf(reader, "fieldBuilder") <= MAX_RETAINED, "field builder kept after taking a huge field's value");

        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER)); // huge quoted field + suffix
        reader.parseRecordInto(row);
        assertEquals("suffix", row[1]);

        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER)); // first small row
        reader.parseRecordInto(row);
        assertEquals("q\"0", row[1]);

        for (final String name : new String[] { "spannedRecord", "recordChars", "recordBuilder", "fieldBuilder" }) {
            assertTrue(capacityOf(reader, name) <= MAX_RETAINED, () -> name + " still holds a huge buffer after moving to a small record");
        }

        while (reader.nextRecord(CsvUtil.CSV_LINE_PARSER)) {
            reader.parseRecordInto(row);
        }

        assertEquals("q\"49", row[1]);
        assertFalse(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
    }

    /** The length of a char[] field or the capacity of a StringBuilder field of CsvRecordReader; 0 when null. */
    private static int capacityOf(final Object reader, final String name) throws Exception {
        final Field field = reader.getClass().getDeclaredField(name);
        field.setAccessible(true);
        final Object value = field.get(reader);

        return value == null ? 0 : value instanceof char[] chars ? chars.length : ((StringBuilder) value).capacity();
    }
}
