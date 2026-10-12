package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.AbstractType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Sheet;
import com.landawn.abacus.util.ShortList;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class JsonPerformanceMemoryFixes20261006Test extends TestBase {
    private final JsonParserImpl parser = new JsonParserImpl();
    // Subclasses retain the original scalar dispatch and reflective primitive writer hooks.
    private final JsonParserImpl originalDispatch = new JsonParserImpl() {
    };

    private enum Label {
        A, B
    }

    private static Object outcome(final Supplier<?> operation) {
        try {
            return operation.get();
        } catch (final RuntimeException e) {
            return e.getClass();
        }
    }

    @Test
    void scalarRootsKeepConversionNullAndRangeContracts() {
        final List<Type<?>> types = List.of(Type.of(String.class), Type.of(Integer.class), Type.of(Number.class), Type.of(BigDecimal.class),
                Type.of(Label.class), Type.of("java.util.Optional<BigDecimal>"));

        for (final Type<?> type : types) {
            for (final String source : new String[] { null, "", "  ", "null", "123", "1.00", "'123'", "A", "[1]", "1 2" }) {
                for (final boolean nullToEmpty : new boolean[] { false, true }) {
                    final JsonDeserConfig config = JsonDeserConfig.create().setReadNullToEmpty(nullToEmpty);
                    assertEquals(outcome(() -> originalDispatch.parse(source, config, type)), outcome(() -> parser.parse(source, config, type)));
                    assertEquals(outcome(() -> originalDispatch.deserialize(source, config, type)), outcome(() -> parser.deserialize(source, config, type)));
                    if (source != null) {
                        final String framed = "xx" + source + "yy";
                        assertEquals(outcome(() -> originalDispatch.deserialize(framed, 2, framed.length() - 2, config, type)),
                                outcome(() -> parser.deserialize(framed, 2, framed.length() - 2, config, type)));
                        assertEquals(outcome(() -> originalDispatch.deserialize(new StringReader(source), config, type)),
                                outcome(() -> parser.deserialize(new StringReader(source), config, type)));
                    }
                }
            }
        }

        final String source = "value".repeat(200_000);
        assertSame(source, parser.parse(source, String.class));
        assertSame(source, parser.deserialize(source, String.class));
        assertSame(source, parser.deserialize(source, 0, source.length(), null, Type.of(String.class)));
        assertThrows(IndexOutOfBoundsException.class, () -> parser.deserialize("a", -1, 1, null, Type.of(String.class)));
        assertThrows(ParsingException.class, () -> parser.deserialize("[1] [2]", int[].class));
    }

    @Test
    void shortReadMakesFirstStreamValueAvailableWithoutAnotherRead() {
        final PrefixReader reader = new PrefixReader();
        try (var values = parser.stream(reader, false, Type.of(Map.class))) {
            assertEquals(1, reader.readCalls);
            assertEquals(Map.of(), values.iterator().next());
            assertEquals(1, reader.readCalls);
        }
        assertFalse(reader.closed);
    }

    @Test
    void shortReadsAndBoundedZeroRetryKeepEscapeAndEofHandling() {
        final String source = "{\"text\":\"a\\n\\u0041\\t\\\\z\",\"n\":[1.00,-2,3e4],\"tail\":true} \n";
        final Object expected = parser.deserialize(source, Map.class);
        for (int chunk = 1; chunk <= 17; chunk++) {
            assertEquals(expected, parser.deserialize(new ChunkReader(source, chunk, true), Map.class));
        }
        final Reader noProgress = new Reader() {
            int reads;

            @Override
            public int read(final char[] buf, final int off, final int len) {
                assertTrue(++reads <= 2, "zero progress must not spin");
                return 0;
            }

            @Override
            public void close() {
            }
        };
        assertEquals(JsonReader.EOF, JsonStreamReader.parse(noProgress, new char[8], new char[0]).nextToken());
    }

    @Test
    void lateEscapeReservesKnownPrefixOnceAndTinyBuffersStillWork() {
        final String prefix = "x".repeat(1_000_000);
        final CapacityReader reader = new CapacityReader("\"" + prefix + "\\n\"", new char[1]);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(prefix + "\n", reader.readValue(Type.of(String.class)));
        assertEquals(1, reader.growths);
        for (int size = 0; size <= 2; size++) {
            final JsonReader tiny = JsonStringReader.parse("\"\\n\\t\\u0041\"", new char[size]);
            tiny.nextToken();
            tiny.nextToken();
            assertEquals("\n\tA", tiny.readValue(Type.of(String.class)));
        }
    }

    @Test
    void primitiveArraysAndListsPreserveUnbracketedFormattingAndFlushing() {
        final Object[] arrays = { new char[] { '\'', '"', '\n', '\\', '\uD83D', '\uDE00' }, new boolean[] { true, false },
                new byte[] { Byte.MIN_VALUE, 0, Byte.MAX_VALUE }, new short[] { Short.MIN_VALUE, 0, Short.MAX_VALUE },
                new int[] { Integer.MIN_VALUE, 10_000, Integer.MAX_VALUE }, new long[] { Long.MIN_VALUE, 0, Long.MAX_VALUE },
                new float[] { -0.0f, 1.25f, Float.NaN, Float.POSITIVE_INFINITY }, new double[] { -0.0, 1.25, Double.NaN, Double.NEGATIVE_INFINITY } };
        final Object[] lists = { CharList.of((char[]) arrays[0]), BooleanList.of((boolean[]) arrays[1]), ByteList.of((byte[]) arrays[2]),
                ShortList.of((short[]) arrays[3]), IntList.of((int[]) arrays[4]), LongList.of((long[]) arrays[5]), FloatList.of((float[]) arrays[6]),
                DoubleList.of((double[]) arrays[7]) };

        for (final boolean pretty : new boolean[] { false, true }) {
            for (final char quotation : new char[] { 0, '\'', '"' }) {
                for (final boolean longAsString : new boolean[] { false, true }) {
                    final JsonSerConfig config = JsonSerConfig.create()
                            .setBracketRootValue(false)
                            .setPrettyFormat(pretty)
                            .setIndentation("\t  ")
                            .setCharQuotation(quotation)
                            .setStringQuotation(quotation)
                            .setWriteLongAsString(longAsString)
                            .setCircularReferenceSupported(true);
                    for (final Object[] values : new Object[][] { arrays, lists }) {
                        for (final Object value : values) {
                            final String expected = originalDispatch.serialize(value, config);
                            assertEquals(expected, parser.serialize(value, config));
                            final StringWriter output = new StringWriter();
                            parser.serialize(value, config, output);
                            assertEquals(expected, output.toString());
                        }
                    }
                }
            }
        }
        final IntList padded = new IntList(64);
        padded.add(7);
        padded.add(8);
        assertEquals("7, 8", parser.serialize(padded, JsonSerConfig.create().setBracketRootValue(false)));
        assertEquals("", parser.serialize(new int[0], JsonSerConfig.create().setBracketRootValue(false).setPrettyFormat(true)));
        assertEquals("", parser.serialize(new IntList(), JsonSerConfig.create().setBracketRootValue(false).setPrettyFormat(true)));
    }

    @Test
    void wideTabularColumnsRetainMetadataOrderAndRepeatedColumnSemantics() {
        final StringBuilder names = new StringBuilder();
        final StringBuilder columns = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            if (i > 0) {
                names.append(',');
                columns.append(',');
            }
            names.append("'c").append(i).append('\'');
            columns.append("'c").append(255 - i).append("':[").append(255 - i).append(']');
        }
        final String datasetJson = "{columnNames:[" + names + "],columns:{" + columns + "}}";
        final String sheetJson = "{rowKeySet:['r'],columnKeySet:[" + names + "],columns:{" + columns + "}}";
        for (final boolean streamed : new boolean[] { false, true }) {
            final Dataset dataset = streamed ? parser.deserialize(new ChunkReader(datasetJson, 7, false), Dataset.class)
                    : parser.deserialize(datasetJson, Dataset.class);
            final Sheet<String, String, Integer> sheet = streamed ? parser.deserialize(new ChunkReader(sheetJson, 7, false), Sheet.class)
                    : parser.deserialize(sheetJson, Sheet.class);
            assertEquals(256, dataset.columnCount());
            for (int i = 0; i < 256; i++) {
                assertEquals(List.of(i), dataset.getColumn("c" + i));
                assertEquals(i, sheet.get("r", "c" + i));
            }
        }
        final Dataset replaced = parser.deserialize("{columnNames:['old'],columnNames:['a'],columns:{a:[1],a:[2]}}", Dataset.class);
        assertEquals(List.of(2), replaced.getColumn("a"));
        assertThrows(ParsingException.class, () -> parser.deserialize("{columnNames:['a','a'],columns:{a:[1]}}", Dataset.class));
        assertThrows(ParsingException.class, () -> parser.deserialize("{columnNames:['a'],columns:{b:[1]}}", Dataset.class));

        final Sheet<String, int[], Integer> arrayKeys = parser.deserialize("{rowKeySet:['r'],columnKeySet:[[1],[1]],columns:{'[1]':[10],'[1]':[20]}}",
                Type.of("Sheet<String, int[], Integer>"));
        final List<int[]> keys = new ArrayList<>(arrayKeys.columnKeySet());
        assertNotSame(keys.get(0), keys.get(1));
        assertEquals(10, arrayKeys.get("r", keys.get(0)));
        assertEquals(20, arrayKeys.get("r", keys.get(1)));
    }

    @Test
    void copyOnWriteListsKeepTypeOutputAndHandlerContracts() throws IOException {
        final Type<CopyOnWriteArrayList<Integer>> type = Type.of("java.util.concurrent.CopyOnWriteArrayList<Integer>");
        final CopyOnWriteArrayList<Integer> result = parser.deserialize("[1,null,2]", type);
        assertEquals(CopyOnWriteArrayList.class, result.getClass());
        assertEquals(java.util.Arrays.asList(1, null, 2), result);

        final CopyOnWriteArrayList<Object> output = new CopyOnWriteArrayList<>(List.of(0));
        final JsonReader reader = JsonStringReader.parse("[1,2]", new char[8]);
        assertSame(output, parser.parse("[1,2]", reader, JsonDeserConfig.create(), type, output));
        assertEquals(List.of(0, 1, 2), output);

        final List<Class<?>> handlerTargets = new ArrayList<>();
        final BiConsumer<Collection<Object>, Object> handler = (collection, value) -> {
            handlerTargets.add(collection.getClass());
            collection.add(value);
        };
        final Object handled = parser.readCollection(JsonStringReader.parse("[1,2]", new char[8]), JsonDeserConfig.create(), null, handler, true,
                CopyOnWriteArrayList.class, type, null);
        assertEquals(List.of(CopyOnWriteArrayList.class, CopyOnWriteArrayList.class), handlerTargets);
        assertEquals(List.of(1, 2), handled);
    }

    @Test
    void decimalRangeConversionPreservesScaleSuffixesAndCustomStringHooks() {
        final String json = "[123,1.00,0.1000,1.5f,007,1e-10,-0.00,9007199254740993.123]";
        final List<BigDecimal> expected = List.of(new BigDecimal("123"), new BigDecimal("1.00"), new BigDecimal("0.1000"), new BigDecimal("1.5"),
                new BigDecimal("7"), new BigDecimal("1e-10"), new BigDecimal("-0.00"), new BigDecimal("9007199254740993.123"));
        final Type<List<BigDecimal>> type = Type.of("List<BigDecimal>");
        assertEquals(expected, parser.deserialize(json, type));
        assertEquals(expected, parser.deserialize(new ChunkReader(json, 2, false), type));

        final int[] calls = { 0 };
        final Type<BigDecimal> custom = new AbstractType<>("JsonPerformanceDecimal") {
            @Override
            public Class<BigDecimal> javaType() {
                return BigDecimal.class;
            }

            @Override
            public String stringOf(final BigDecimal value) {
                return value.toString();
            }

            @Override
            public BigDecimal valueOf(final String text) {
                calls[0]++;
                return new BigDecimal(text);
            }

            @Override
            public BigDecimal valueOf(final char[] chars, final int off, final int len) {
                throw new AssertionError("custom String hook bypassed");
            }
        };
        final JsonReader reader = JsonStringReader.parse("1.00,", new char[8]);
        reader.nextToken();
        assertEquals(new BigDecimal("1.00"), reader.readValue(custom));
        assertEquals(1, calls[0]);
    }

    private static final class PrefixReader extends Reader {
        int readCalls;
        boolean closed;

        @Override
        public int read(final char[] buf, final int offset, final int length) throws IOException {
            if (++readCalls > 1) {
                throw new IOException("later input is not available yet");
            }
            final String prefix = "[{},";
            prefix.getChars(0, prefix.length(), buf, offset);
            return prefix.length();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class ChunkReader extends Reader {
        private final String source;
        private final int chunk;
        private boolean firstZero;
        private int position;

        ChunkReader(final String source, final int chunk, final boolean firstZero) {
            this.source = source;
            this.chunk = chunk;
            this.firstZero = firstZero;
        }

        @Override
        public int read(final char[] buf, final int offset, final int length) {
            if (firstZero) {
                firstZero = false;
                return 0;
            }
            if (position == source.length()) {
                return -1;
            }
            final int count = Math.min(Math.min(chunk, length), source.length() - position);
            source.getChars(position, position + count, buf, offset);
            position += count;
            return count;
        }

        @Override
        public void close() {
        }
    }

    private static final class CapacityReader extends JsonStringReader {
        int growths;

        CapacityReader(final String source, final char[] scratch) {
            super(source, scratch);
        }

        @Override
        protected void ensureCharBufferCapacity(final long capacity) {
            if (capacity > cbufLen) {
                growths++;
            }
            super.ensureCharBufferCapacity(capacity);
        }
    }
}
