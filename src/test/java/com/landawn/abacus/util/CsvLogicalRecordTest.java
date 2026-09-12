package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;

public class CsvLogicalRecordTest extends TestBase {
    @Test
    void exactLineEndingsAndQuotedHeadersSurviveLoadAndJsonRoundTrips() {
        for (String newline : List.of("\n", "\r", "\r\n")) {
            String header = "\u4e2d" + newline + "\"name\\";
            String value = "\ud83d\ude00" + newline + "\"value\\";
            String csv = "\uFEFF" + quote(header) + newline + quote(value) + newline;
            Dataset data = CsvUtil.load(new StringReader(csv));
            assertEquals(List.of(header), data.columnNames());
            assertEquals(value, data.get(0, 0));
            StringWriter json = new StringWriter();
            assertEquals(1, CsvUtil.csvToJson(new StringReader(csv), null, json, null));
            List<Map<String, String>> expected = List.of(Map.of(header, value));
            assertEquals(expected, N.fromJson(json.toString(), new TypeReference<List<Map<String, String>>>() {
            }.type()));
            StringWriter rebuilt = new StringWriter();
            assertEquals(1, CsvUtil.jsonToCsv(new StringReader(json.toString()), null, rebuilt));
            assertEquals(value, CsvUtil.load(new StringReader(rebuilt.toString())).get(0, 0));
        }
    }

    @Test
    void paginationAndConversionUseLogicalRowsAcrossAllLoadAndStreamCores() {
        String csv = "id,text\r\n1,\"first\nline\"\r\n2,\"second\r\nline\"\r\n3,last";
        Dataset plain = CsvUtil.load(new StringReader(csv), null, 1, 1, row -> true);
        Dataset bean = CsvUtil.load(new StringReader(csv), null, 1, 1, row -> true, RowBean.class);
        Dataset typed = CsvUtil.load(new StringReader(csv), null, 1, 1, row -> true, Map.of("id", CommonUtil.typeOf(Integer.class)));
        Dataset extracted = CsvUtil.load(new StringReader(csv), null, 1, 1, row -> true, (headers, row, output) -> {
            output[0] = Integer.valueOf(row.get(0));
            output[1] = row.get(1);
        });
        for (Dataset data : List.of(plain, bean, typed, extracted)) {
            assertEquals(1, data.size());
            assertEquals("second\r\nline", data.get(0, 1));
        }
        assertEquals(2, bean.<Integer> get(0, 0));
        assertEquals(2, typed.<Integer> get(0, 0));
        assertEquals(2, extracted.<Integer> get(0, 0));
        try (var stream = CsvUtil.stream(new StringReader(csv), null, 1, 1, row -> true, String[].class, false)) {
            assertArrayEquals(new String[] { "2", "second\r\nline" }, stream.toList().get(0));
        }
        try (var stream = CsvUtil.stream(new StringReader(csv), List.of("text"), 1, 1, row -> true, (headers, row) -> row.get(0), false)) {
            assertEquals(List.of("second\r\nline"), stream.toList());
        }
        StringWriter json = new StringWriter();
        assertEquals(1, CsvUtil.converter().source(new StringReader(csv)).offset(1).count(1).csvToJson(json));
        assertEquals(List.of(Map.of("id", "2", "text", "second\r\nline")), N.fromJson(json.toString(), new TypeReference<List<Map<String, String>>>() {
        }.type()));
        Dataset filtered = CsvUtil.load(new StringReader(csv), null, 0, 1, row -> row[0].equals("2"));
        assertEquals("second\r\nline", filtered.get(0, 1));
    }

    @Test
    void customHeaderAndRowCallbacksKeepIndependentPhysicalLineFraming() {
        var oldHeader = CsvUtil.getCurrentHeaderParser();
        var oldLine = CsvUtil.getCurrentLineParser();
        try {
            CsvUtil.setHeaderParser(line -> new String[] { line });
            CsvUtil.setLineParser(CsvUtil.CSV_LINE_PARSER);
            assertEquals("a\nb", CsvUtil.load(new StringReader("custom\n\"a\nb\"\n")).get(0, 0));
            CsvUtil.setHeaderParser(CsvUtil.CSV_HEADER_PARSER);
            List<String> physical = new ArrayList<>();
            CsvUtil.setLineParser((line, out) -> {
                physical.add(line);
                out[0] = line;
            });
            Dataset data = CsvUtil.load(new StringReader("\"head\nname\"\r\n\"a\nb\"\r\n"));
            assertEquals(List.of("head\nname"), data.columnNames());
            assertEquals(List.of("\"a", "b\""), physical);
            assertEquals(2, data.size());
        } finally {
            CsvUtil.setHeaderParser(oldHeader);
            CsvUtil.setLineParser(oldLine);
        }
    }

    @Test
    void streamsCaptureFramingLazilyAndHonorReaderOwnership() {
        TrackingReader reader = new TrackingReader("text\n\"a\nb\"\nlast");
        var oldLine = CsvUtil.getCurrentLineParser();
        try (var stream = CsvUtil.stream(reader, String.class, false)) {
            assertEquals(0, reader.readCalls);
            CsvUtil.setLineParser((line, out) -> out[0] = "changed");
            assertEquals(List.of("a\nb", "last"), stream.toList());
        } finally {
            CsvUtil.setLineParser(oldLine);
        }
        assertFalse(reader.closed);
        TrackingReader owned = new TrackingReader("text\n\"a\nb\"");
        try (var stream = CsvUtil.stream(owned, String.class, true)) {
            assertEquals(List.of("a\nb"), stream.toList());
        }
        assertTrue(owned.closed);
        TrackingReader eager = new TrackingReader("text\n\"a\nb\"");
        CsvUtil.load(eager);
        assertFalse(eager.closed);
        TrackingReader conversion = new TrackingReader("text\n\"a\nb\"");
        CsvUtil.csvToJson(conversion, null, new StringWriter(), null);
        assertFalse(conversion.closed);
    }

    @Test
    void emptyMalformedAndEmbeddedQuoteCasesRespectRecordBoundaries() {
        assertEquals(0, CsvUtil.load(new StringReader("")).size());
        assertEquals(0, CsvUtil.load(new StringReader("h\n")).size());
        assertEquals(1, CsvUtil.load(new StringReader("h\n\n")).size());
        assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader("h\n\"open\nfield")));
        assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader("\"open\nheader")));
        try (var stream = CsvUtil.stream(new StringReader("h\n\"open\nfield"), String.class, true)) {
            assertThrows(ParsingException.class, stream::toList);
        }
        assertThrows(ParsingException.class, () -> CsvUtil.csvToJson(new StringReader("h\n\"open"), null, new StringWriter(), null));
        for (String record : List.of("ab\"cd", "\"\"  \"literal", " \"line\nline\"suffix", "\"a\"\"b\"")) {
            String expected = new CsvParser().parseLine(record).get(0);
            Dataset data = CsvUtil.load(new StringReader("h\n" + record + "\r\nlast"));
            assertEquals(2, data.size());
            assertEquals(expected, data.get(0, 0));
            assertEquals("last", data.get(1, 0));
        }
        Reader broken = new Reader() {
            @Override
            public int read(char[] chars, int offset, int length) throws IOException {
                throw new IOException("read failed");
            }

            @Override
            public void close() {
            }
        };
        assertThrows(UncheckedIOException.class, () -> CsvUtil.load(broken));
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    public static class RowBean {
        private int id;
        private String text;

        public int getId() {
            return id;
        }

        public void setId(int value) {
            id = value;
        }

        public String getText() {
            return text;
        }

        public void setText(String value) {
            text = value;
        }
    }

    private static class TrackingReader extends Reader {
        private final StringReader delegate;
        int readCalls;
        boolean closed;

        TrackingReader(String value) {
            delegate = new StringReader(value);
        }

        @Override
        public int read(char[] chars, int offset, int length) throws IOException {
            readCalls++;
            return delegate.read(chars, offset, Math.min(length, 1));
        }

        @Override
        public void close() {
            closed = true;
            delegate.close();
        }
    }
}
