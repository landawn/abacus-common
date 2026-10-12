package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.RowDataset;

@Tag("unit")
class ParserMemoryOptimization20261004Test extends TestBase {
    private final JsonParser parser = ParserFactory.createJsonParser();

    @Test
    void rangedReaderCopiesOnlySelectionAndPreservesScalarSpelling() {
        String source = "x".repeat(1_000_000) + "[12345]" + "y".repeat(1_000_000);
        JsonStringReader reader = new JsonStringReader(source, 1_000_000, 1_000_007, new char[32]);
        assertEquals(7, reader.strValue.length);
        assertArrayEquals(new int[] { 12345 }, parser.deserialize(source, 1_000_000, 1_000_007, null, int[].class));
        assertEquals("007", parser.deserialize("x007y", 1, 4, null, String.class));
        assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(source, -1, 2, new char[32]));
    }

    @Test
    void streamTextWritesBoundedChunksWithoutClosingInput() throws IOException {
        String text = "a".repeat(8191) + "\uD83D\uDE00\"\\\n" + "z".repeat(30000);
        TrackingInput input = new TrackingInput(text.getBytes(StandardCharsets.UTF_8));
        StringWriter destination = new StringWriter();
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter(destination);
        JsonSerConfig config = new JsonSerConfig();
        try {
            Type.<InputStream> of(InputStream.class).serializeTo(writer, input, config);
            writer.flush();
        } finally {
            Objectory.recycle(writer);
        }
        assertFalse(input.closed);
        assertEquals(0, input.available());
        assertTrue(input.maxRead <= 8192);
        assertEquals(List.of(text), parser.deserialize("[" + destination + "]", List.class));

        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("source failure");
            }
        };
        BufferedJsonWriter failingWriter = Objectory.createBufferedJsonWriter();
        try {
            assertThrows(UncheckedIOException.class, () -> Type.<InputStream> of(InputStream.class).serializeTo(failingWriter, broken, config));
        } finally {
            Objectory.recycle(failingWriter);
        }
    }

    @Test
    void kryoStreamsLargePayloadAndConsumesTrailingBytesWithoutClosing() throws ReflectiveOperationException {
        KryoParser kryo = ParserFactory.createKryoParser();
        String value = "large payload".repeat(100000);
        byte[] encoded = kryo.encode(value);
        byte[] withTrailing = Arrays.copyOf(encoded, encoded.length + 20000);
        for (int iteration = 0; iteration < 3; iteration++) {
            TrackingInput input = new TrackingInput(withTrailing);
            assertEquals(value, kryo.deserialize(input, null, (Class<String>) null));
            assertEquals(0, input.available());
            assertFalse(input.closed);
            assertTrue(input.maxRead <= 8192);
            assertEquals(value, kryo.decode(encoded));
        }
        InputStream broken = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("broken Kryo source");
            }
        };
        assertThrows(UncheckedIOException.class, () -> kryo.deserialize(broken, null, (Class<String>) null));

        var poolField = KryoParser.class.getDeclaredField("streamingInputPool");
        poolField.setAccessible(true);
        var charsField = com.esotericsoftware.kryo.io.Input.class.getDeclaredField("chars");
        charsField.setAccessible(true);
        List<?> pool = (List<?>) poolField.get(null);
        synchronized (pool) {
            for (Object pooled : pool) {
                var input = (com.esotericsoftware.kryo.io.Input) pooled;
                assertNull(input.getInputStream());
                assertEquals(8192, input.getBuffer().length);
                assertTrue(((char[]) charsField.get(input)).length <= 8192);
            }
        }
    }

    @Test
    void datasetRowsMatchMapSerializationAcrossFormattingAndFilters() {
        Dataset dataset = new RowDataset(List.of("a", "b"), List.of(Arrays.asList(1, 2, 3), Arrays.asList("x", null, "z")));
        for (boolean pretty : new boolean[] { false, true }) {
            for (boolean bracket : new boolean[] { false, true }) {
                JsonSerConfig config = new JsonSerConfig().setWriteDatasetAsRows(true).setPrettyFormat(pretty).setBracketRootValue(bracket);
                assertEquals(parser.serialize(dataset.toList(LinkedHashMap.class), config), parser.serialize(dataset, config));
            }
        }
        JsonSerConfig ignored = new JsonSerConfig().setWriteDatasetAsRows(true).setIgnoredPropNames(Map.class, Set.of("a"));
        assertEquals(parser.serialize(dataset.toList(LinkedHashMap.class), ignored), parser.serialize(dataset, ignored));
    }

    @Test
    void graphlessValidationKeepsGenericConversionAndGrammarRules() {
        for (String json : List.of("{}", "[]", "[null,1.5,\"x\",{\"n\":[1,2]}]", "{x:NaN}", "[1,]", "[1 2]", "{} []", "[\"a\" 2]", "{x:}", "{x:1,y:[2,3]}",
                "[999999999999999999999999999999999999999999999]")) {
            Throwable expected = failure(() -> parser.deserialize(json, new JsonDeserConfig(), Object.class));
            Throwable actual = failure(() -> parser.validate(json));
            assertEquals(expected == null ? null : expected.getClass(), actual == null ? null : actual.getClass(), json);
        }
    }

    @Test
    void primitiveContainersKeepTheirDistinctConversionRules() {
        assertArrayEquals(new int[] { 1, 0, 2 }, parser.deserialize("[1.5,null,2]", int[].class));
        assertArrayEquals(new int[] { 0, 1, 0 }, parser.deserialize("[,1,]", int[].class));
        assertThrows(RuntimeException.class, () -> parser.deserialize("[1.5]", IntList.class));
        assertEquals(IntList.of(12345, 12346), parser.deserialize(new StringReader("[12345,12346]"), IntList.class));
        assertEquals(CharList.of('1', '\n', ' '), parser.deserialize("[1,'\\n',' ']", CharList.class));
        // Boolean text, quoted or not: 1 is true, 0 and -1 are false, and any other text is rejected (code review 2026-10-05).
        assertEquals(BooleanList.of(true, false, true, false), parser.deserialize("[1,0,\"1\",-1]", BooleanList.class));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize("[1,2]", BooleanList.class));
        DoubleList values = parser.deserialize("[-0.0,NaN,Infinity]", DoubleList.class);
        assertEquals(Double.doubleToRawLongBits(-0.0), Double.doubleToRawLongBits(values.get(0)));
        assertTrue(Double.isNaN(values.get(1)));
        assertEquals(Double.POSITIVE_INFINITY, values.get(2));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize("[1,]", IntList.class));
        assertThrows(RuntimeException.class, () -> parser.deserialize("[1] [2]", IntList.class));
        assertNull(parser.deserialize("   ", IntList.class));
    }

    private static Throwable failure(Runnable task) {
        try {
            task.run();
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static final class TrackingInput extends ByteArrayInputStream {
        private boolean closed;
        private int maxRead;

        TrackingInput(byte[] data) {
            super(data);
        }

        @Override
        public synchronized int read(byte[] data, int offset, int length) {
            maxRead = Math.max(maxRead, length);
            return super.read(data, offset, length);
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}
