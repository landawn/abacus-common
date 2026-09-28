package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.CsvUtil.CsvRecordReader;

@Tag("unit")
class CsvRecordReaderTest {
    /** Any line parser other than CSV_LINE_PARSER (by identity) frames physical lines. */
    private static final BiConsumer<String, String[]> PHYSICAL_LINE_PARSER = CsvUtil.CSV_LINE_PARSER_BY_SPLITTER;
    @Test
    void currentRecordAndDestinationClearing() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("a,b\nc\n\n"));
        String[] row = new String[2];
        assertThrows(IllegalStateException.class, () -> reader.parseRecordInto(row));
        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { "a", "b" }, row);
        reader.parseRecordInto(CsvUtil.CSV_LINE_PARSER, row);
        assertArrayEquals(new String[] { "a", "b" }, row);
        assertEquals("c", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { "c", null }, row);
        assertEquals("", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { "", null }, row);
        assertFalse(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        assertThrows(IllegalStateException.class, () -> reader.parseRecordInto(row));
        assertNull(reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        assertFalse(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
    }

    @Test
    void multilineBufferBoundariesAndBom() throws IOException {
        String value = "x".repeat(8191) + "\r\ny\"z";
        String csv = "\uFEFF\"first\nname\",second\r\n\"" + value.replace("\"", "\"\"") + "\",end\r\nlast,row";
        CsvRecordReader reader = new CsvRecordReader(new StringReader(csv));
        assertArrayEquals(new String[] { "first\nname", "second" }, reader.readHeader(CsvUtil.CSV_HEADER_PARSER));
        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        String[] row = new String[2];
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { value, "end" }, row);
        assertEquals("last,row", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        assertNull(reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
    }

    @Test
    void customParsersUsePhysicalLines() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("\"a\nb\",c"));
        BiConsumer<String, String[]> parser = (text, row) -> row[0] = text;
        String[] row = new String[2];
        assertTrue(reader.nextRecord(parser));
        reader.parseRecordInto(parser, row);
        assertArrayEquals(new String[] { "\"a", null }, row);
        assertTrue(reader.nextRecord(parser));
        reader.parseRecordInto(parser, row);
        assertEquals("b\",c", row[0]);
        CsvRecordReader wrapped = new CsvRecordReader(new StringReader("\"a\nb\""));
        BiConsumer<String, String[]> wrapper = (text, dest) -> CsvUtil.CSV_LINE_PARSER.accept(text, dest);
        assertTrue(wrapped.nextRecord(wrapper));
        assertThrows(ParsingException.class, () -> wrapped.parseRecordInto(wrapper, row));
    }

    @Test
    void failedReadInvalidatesCurrentRecord() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("ok\n\"unterminated"));
        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        assertThrows(ParsingException.class, () -> reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        assertThrows(IllegalStateException.class, () -> reader.parseRecordInto(new String[1]));
        CsvRecordReader tooWide = new CsvRecordReader(new StringReader("a,b"));
        assertTrue(tooWide.nextRecord(CsvUtil.CSV_LINE_PARSER));
        assertThrows(ParsingException.class, () -> tooWide.parseRecordInto(new String[1]));
    }

    @Test
    void streamIsLazySkipsWithoutParsingAndReusesArray() {
        class Source extends StringReader {
            int reads;
            boolean closed;
            Source() { super("skip,too,many\na,b\nc"); }
            @Override public int read(char[] chars, int offset, int length) throws IOException {
                reads++;
                return super.read(chars, offset, length);
            }
            @Override public void close() { closed = true; super.close(); }
        }
        Source source = new Source();
        CsvRecordReader reader = new CsvRecordReader(source);
        String[] row = new String[2];
        try (var stream = reader.stream(CsvUtil.CSV_LINE_PARSER, 1, row)) {
            assertEquals(0, source.reads);
            List<String[]> rows = stream.toList();
            assertEquals(2, rows.size());
            assertSame(row, rows.get(0));
            assertSame(row, rows.get(1));
            assertArrayEquals(new String[] { "c", null }, row);
        }
        assertFalse(source.closed);
        source.close();
    }

    @Test
    void streamStartsAfterCurrentRecordAndCanCopyRows() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("header\na\nb"));
        assertArrayEquals(new String[] { "header" }, reader.readHeader(CsvUtil.CSV_HEADER_PARSER));
        try (var stream = reader.stream(CsvUtil.CSV_LINE_PARSER, 0, new String[1])) {
            List<String[]> rows = stream.map(it -> it.clone()).toList();
            assertArrayEquals(new String[] { "a" }, rows.get(0));
            assertArrayEquals(new String[] { "b" }, rows.get(1));
            assertNotSame(rows.get(0), rows.get(1));
        }
    }

    @Test
    void validatesArgumentsAndWrapsStreamIoFailures() {
        assertThrows(IllegalArgumentException.class, () -> new CsvRecordReader(null));
        CsvRecordReader reader = new CsvRecordReader(new StringReader(""));
        assertThrows(IllegalArgumentException.class, () -> reader.nextRecord(null));
        assertThrows(IllegalArgumentException.class, () -> reader.parseRecordInto((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> reader.parseRecordInto(null, new String[1]));
        assertThrows(IllegalArgumentException.class, () -> reader.stream(CsvUtil.CSV_LINE_PARSER, -1, new String[1]));
        assertThrows(IllegalArgumentException.class, () -> reader.stream(null, 0, new String[1]));
        assertThrows(IllegalArgumentException.class, () -> reader.stream(CsvUtil.CSV_LINE_PARSER, 0, null));
        Reader broken = new Reader() {
            @Override public int read(char[] chars, int offset, int length) throws IOException { throw new IOException("broken"); }
            @Override public void close() { }
        };
        CsvRecordReader failing = new CsvRecordReader(broken);
        try (var stream = failing.stream(CsvUtil.CSV_LINE_PARSER, 0, new String[1])) {
            assertThrows(UncheckedIOException.class, stream::toList);
        }
        assertThrows(IllegalStateException.class, () -> failing.parseRecordInto(new String[1]));
    }

    @Test
    void physicalReadsPreserveTextAndHandleAllTerminators() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("\"a\r\nb\"\rc\n\nlast"));
        for (String expected : List.of("\"a", "b\"", "c", "", "last")) {
            assertEquals(expected, reader.nextRecordText(PHYSICAL_LINE_PARSER));
        }
        assertNull(reader.nextRecordText(PHYSICAL_LINE_PARSER));
        assertThrows(IllegalStateException.class, () -> reader.parseRecordInto(new String[1]));
        assertNull(new CsvRecordReader(new StringReader("")).readHeader(CsvUtil.CSV_HEADER_PARSER));
    }

    @Test
    void ioFailureAfterSuccessInvalidatesCurrentRecordForBothReadMethods() throws IOException {
        for (boolean useNextRecord : new boolean[] { false, true }) {
            IOException failure = new IOException("failed after a record");
            Reader source = new Reader() {
                boolean delivered;

                @Override
                public int read(char[] chars, int offset, int length) throws IOException {
                    if (delivered) {
                        throw failure;
                    }
                    delivered = true;
                    chars[offset] = 'a';
                    chars[offset + 1] = '\n';
                    return 2;
                }

                @Override
                public void close() {
                }
            };
            CsvRecordReader reader = new CsvRecordReader(source);
            assertEquals("a", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
            String[] row = { "previous" };
            if (useNextRecord) {
                assertSame(failure, assertThrows(IOException.class, () -> reader.nextRecord(CsvUtil.CSV_LINE_PARSER)));
            } else {
                assertSame(failure, assertThrows(IOException.class, () -> reader.nextRecordText(CsvUtil.CSV_LINE_PARSER)));
            }
            assertThrows(IllegalStateException.class, () -> reader.parseRecordInto(row));
            assertArrayEquals(new String[] { "previous" }, row);
        }
    }

    @Test
    void parsingCanBeRetriedAfterOverflowOrCustomParserFailure() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("a,b\nc"));
        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        assertThrows(ParsingException.class, () -> reader.parseRecordInto(new String[0]));
        assertThrows(ParsingException.class, () -> reader.parseRecordInto(new String[1]));
        String[] row = new String[3];
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { "a", "b", null }, row);

        RuntimeException failure = new IllegalStateException("custom parser failed");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> reader.parseRecordInto((text, out) -> { throw failure; }, row)));
        IndexOutOfBoundsException overflow = new IndexOutOfBoundsException("custom overflow");
        assertSame(overflow, assertThrows(ParsingException.class,
                () -> reader.parseRecordInto((text, out) -> { throw overflow; }, row)).getCause());
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { "a", "b", null }, row);
        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { "c", null, null }, row);
    }

    @Test
    void rejectedArgumentsDoNotConsumeOrInvalidateCurrentRecord() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("a\nb"));
        assertEquals("a", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        assertThrows(IllegalArgumentException.class, () -> reader.nextRecord(null));
        assertThrows(IllegalArgumentException.class, () -> reader.parseRecordInto(CsvUtil.CSV_LINE_PARSER, null));
        assertThrows(IllegalArgumentException.class, () -> reader.stream(CsvUtil.CSV_LINE_PARSER, -1, new String[1]));
        String[] row = new String[1];
        reader.parseRecordInto(row);
        assertEquals("a", row[0]);
        assertEquals("b", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
    }

    @Test
    void streamsHandleEmptyInputExcessOffsetsAndMalformedSkippedRecords() {
        for (String input : List.of("", "a\nb")) {
            for (long offset : new long[] { 2, Long.MAX_VALUE }) {
                CsvRecordReader reader = new CsvRecordReader(new StringReader(input));
                try (var stream = reader.stream(CsvUtil.CSV_LINE_PARSER, offset, new String[1])) {
                    assertTrue(stream.toList().isEmpty());
                }
                assertThrows(IllegalStateException.class, () -> reader.parseRecordInto(new String[1]));
            }
        }
        for (long offset : new long[] { 0, 1 }) {
            CsvRecordReader reader = new CsvRecordReader(new StringReader("\"unterminated"));
            try (var stream = reader.stream(CsvUtil.CSV_LINE_PARSER, offset, new String[1])) {
                assertThrows(ParsingException.class, stream::toList);
            }
            assertThrows(IllegalStateException.class, () -> reader.parseRecordInto(new String[1]));
        }
    }

    @Test
    void closingUnconsumedOrShortCircuitedStreamAllowsFurtherReading() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("a\nb\nc"));
        reader.stream(CsvUtil.CSV_LINE_PARSER, 1, new String[1]).close();
        assertEquals("a", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        try (var stream = reader.stream(CsvUtil.CSV_LINE_PARSER, 0, new String[1])) {
            assertEquals(List.of("b"), stream.limit(1).map(row -> row[0]).toList());
        }
        assertEquals("c", reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        assertNull(reader.nextRecordText(CsvUtil.CSV_LINE_PARSER));
    }

    @Test
    void readHeaderFramesByParserStripsBomAndBecomesCurrentRecord() throws IOException {
        // CSV_HEADER_PARSER: a logical record, so the quoted line break stays inside the first column name.
        CsvRecordReader logical = new CsvRecordReader(new StringReader("\uFEFF\"a\nb\",c\n1,2"));
        assertArrayEquals(new String[] { "a\nb", "c" }, logical.readHeader(CsvUtil.CSV_HEADER_PARSER));
        String[] row = new String[2];
        logical.parseRecordInto(row); // the header is the current record, as read: its text still starts with the BOM
        assertArrayEquals(new String[] { "\uFEFF\"a\nb\"", "c" }, row);
        assertTrue(logical.nextRecord(CsvUtil.CSV_LINE_PARSER));
        logical.parseRecordInto(row);
        assertArrayEquals(new String[] { "1", "2" }, row);

        // Any other header parser: one physical line, BOM removed before the parser sees it.
        List<String> seen = new java.util.ArrayList<>();
        java.util.function.Function<String, String[]> custom = text -> {
            seen.add(text);
            return text.split(",");
        };
        CsvRecordReader physical = new CsvRecordReader(new StringReader("\uFEFFx,\"y\nz\",w"));
        assertArrayEquals(new String[] { "x", "\"y" }, physical.readHeader(custom));
        assertEquals(List.of("x,\"y"), seen);
        assertEquals("z\",w", physical.nextRecordText(PHYSICAL_LINE_PARSER));

        // EOF: null, and no current record.
        CsvRecordReader empty = new CsvRecordReader(new StringReader(""));
        assertNull(empty.readHeader(custom));
        assertThrows(IllegalStateException.class, () -> empty.parseRecordInto(new String[1]));
    }

    @Test
    void reparsingBomHeaderWithQuotedCommaUsesOriginalText() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("\uFEFF\"a,b\",c\n1,2"));
        String[] header = reader.readHeader(CsvUtil.CSV_HEADER_PARSER);
        assertArrayEquals(new String[] { "a,b", "c" }, header);

        // Only readHeader's parser sees BOM-stripped text; the retained record still has the BOM before the quote.
        String[] raw = new String[1];
        reader.parseRecordInto((text, output) -> output[0] = text, raw);
        assertEquals("\uFEFF\"a,b\",c", raw[0]);
        assertThrows(ParsingException.class, () -> reader.parseRecordInto(new String[header.length]));
        assertThrows(ParsingException.class, () -> reader.parseRecordInto(CsvUtil.CSV_LINE_PARSER, new String[header.length]));
        String[] reparsed = new String[3];
        reader.parseRecordInto(reparsed);
        assertArrayEquals(new String[] { "\uFEFF\"a", "b\"", "c" }, reparsed);

        // Reparsing (including failure) does not consume the next data record or alter the returned header.
        assertArrayEquals(new String[] { "a,b", "c" }, header);
        assertTrue(reader.nextRecord(CsvUtil.CSV_LINE_PARSER));
        String[] row = new String[2];
        reader.parseRecordInto(row);
        assertArrayEquals(new String[] { "1", "2" }, row);
    }

    @Test
    void readHeaderValidatesAndPropagatesParserFailures() {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("a,b\nc,d"));
        assertThrows(IllegalArgumentException.class, () -> reader.readHeader(null));
        RuntimeException failure = new IllegalStateException("bad header");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> reader.readHeader(text -> {
            throw failure;
        })));
        CsvRecordReader unterminated = new CsvRecordReader(new StringReader("\"a,b"));
        assertThrows(ParsingException.class, () -> unterminated.readHeader(CsvUtil.CSV_HEADER_PARSER));
        assertThrows(IllegalStateException.class, () -> unterminated.parseRecordInto(new String[2]));
    }

    @Test
    void headerParserReturningNullIsNotMistakenForEndOfInput() throws IOException {
        CsvRecordReader reader = new CsvRecordReader(new StringReader("a,b\nc,d"));
        assertEquals("headerParser returned null", assertThrows(NullPointerException.class, () -> reader.readHeader(text -> null)).getMessage());
        assertNull(new CsvRecordReader(new StringReader("")).readHeader(text -> null)); // end of input: the parser is not called

        // The loaders fail as they did before readHeader existed, instead of returning an empty result for a non-empty source.
        CsvUtil.setHeaderParser(text -> null);
        try {
            assertThrows(NullPointerException.class, () -> CsvUtil.load(new StringReader("a,b\nc,d")));
            assertThrows(NullPointerException.class,
                    () -> CsvUtil.stream(new StringReader("a,b\nc,d"), null, 0, Long.MAX_VALUE, it -> true, (names, row) -> row.get(0), false).toList());
            assertThrows(NullPointerException.class, () -> CsvUtil.csvToJson(new StringReader("a,b\nc,d"), null, new java.io.StringWriter(), null));
            assertEquals(0, CsvUtil.load(new StringReader("")).size());
        } finally {
            CsvUtil.resetHeaderParser();
        }
    }

    @Test
    void nextRecordTextFramesByParser() throws IOException {
        CsvRecordReader logical = new CsvRecordReader(new StringReader("\"a\nb\",c\r\nd"));
        assertEquals("\"a\nb\",c", logical.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        String[] row = new String[2];
        logical.parseRecordInto(row);
        assertArrayEquals(new String[] { "a\nb", "c" }, row);
        assertEquals("d", logical.nextRecordText(CsvUtil.CSV_LINE_PARSER));
        assertNull(logical.nextRecordText(CsvUtil.CSV_LINE_PARSER));

        CsvRecordReader physical = new CsvRecordReader(new StringReader("\"a\nb\",c"));
        assertEquals("\"a", physical.nextRecordText(PHYSICAL_LINE_PARSER));
        assertEquals("b\",c", physical.nextRecordText(PHYSICAL_LINE_PARSER));
        assertNull(physical.nextRecordText(PHYSICAL_LINE_PARSER));
        assertThrows(IllegalArgumentException.class, () -> physical.nextRecordText(null));
    }
}
