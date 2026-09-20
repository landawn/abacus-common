package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.Supplier;
import java.util.stream.Collector;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Throwables;

class StreamExceptionAuditTest extends TestBase {
    @TempDir
    Path tempDir;

    @Test
    void invalidCsvHeadersDoNotTruncateTheOutputOrTraverseTheSource() throws Exception {
        Path output = tempDir.resolve("existing.csv");
        Files.writeString(output, "existing content");
        AtomicInteger read = new AtomicInteger();
        AtomicInteger closed = new AtomicInteger();
        Stream<Map<String, Integer>> source = Stream.just(Map.of("value", 1)).onEach(row -> read.incrementAndGet()).onClose(closed::incrementAndGet);

        assertThrows(IllegalArgumentException.class, () -> source.persistToCsv(List.of(), output.toFile()));

        assertEquals("existing content", Files.readString(output));
        assertEquals(0, read.get());
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, () -> source.persistToCsv(null, output.toFile()));
    }

    @Test
    void nullCsvWriterIsRejectedBeforeCopyingHeaders() {
        AtomicInteger iterated = new AtomicInteger();
        Collection<String> headers = new AbstractCollection<>() {
            @Override
            public int size() {
                return 1;
            }

            @Override
            public Iterator<String> iterator() {
                iterated.incrementAndGet();
                return List.of("value").iterator();
            }
        };
        AtomicInteger closed = new AtomicInteger();
        Stream<Map<String, Integer>> source = Stream.just(Map.of("value", 1)).onClose(closed::incrementAndGet);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> source.persistToCsv(headers, (Writer) null));

        assertTrue(failure.getMessage().contains("output"));
        assertEquals(0, iterated.get());
        assertEquals(1, closed.get());
    }

    @Test
    void delegatedNumericValidationPrecedesNullCallbacks() {
        assertArgument("position", () -> Stream.of(1).splitAt(-1, (Collector<Integer, ?, List<Integer>>) null));
        assertArgument("windowSize", () -> EntryStream.of(Map.of("a", 1)).sliding(0, (java.util.function.IntFunction<List<Map.Entry<String, Integer>>>) null));
        assertArgument("n", () -> EntryStream.of(Map.of("a", 1)).skip(-1, null));
        assertArgument("atLeast",
                () -> EntryStream.of(Map.of("a", 1)).hasMatchCountBetween(-1, 1, (Throwables.BiPredicate<String, Integer, RuntimeException>) null));
        assertArgument("totalSize", () -> IntStream.splitByChunkCount(-1, 0, (IntBinaryOperator) null));
        assertArgument("maxThreadNum", () -> Stream.of(1).spsMap(-1, (Function<Integer, Integer>) null));
        assertArgument("chunkSize", () -> Stream.of(1).spsMap(1, 0, (Function<Integer, Integer>) null));

        assertEquals(List.of(2, 3), Stream.of(1, 2).spsMap(0, value -> value + 1).sorted().toList());
    }

    @Test
    void intervalValidationPrecedesTheSupplierAndPreservesLazyEvaluation() {
        assertArgument("interval", () -> Stream.interval(0, (Supplier<Integer>) null));
        assertArgument("delay", () -> Stream.interval(-1, 0, (Supplier<Integer>) null));
        assertArgument("milliseconds", () -> Stream.interval(0, 1, TimeUnit.NANOSECONDS, (Supplier<Integer>) null));
        AtomicInteger supplied = new AtomicInteger();
        try (Stream<Integer> ticks = Stream.interval(1, supplied::incrementAndGet)) {
            assertEquals(0, supplied.get());
            assertEquals(List.of(1), ticks.limit(1).toList());
        }
    }

    @Test
    void combinedCollectorsValidateTheirComponentsBeforeTheMerger() {
        assertArgument("downstreams", () -> Collectors.MoreCollectors.combine((Collection<Collector<Integer, ?, ?>>) null, null));
        assertArgument("downstreams[0]",
                () -> Collectors.MoreCollectors.combine(java.util.Arrays.<Collector<Integer, ?, ?>> asList((Collector<Integer, ?, ?>) null), null));
        assertArgument("merger", () -> Collectors.MoreCollectors.combine(List.of(Collectors.<Integer> toList()), null));
    }

    @Test
    void windowHandlerValidationPrecedesTheLaterCollector() {
        assertArgument("duration", () -> Stream.of(1).window(Duration.ofMillis(0), (Supplier<List<Integer>>) null));
        assertArgument("increment",
                () -> Stream.of(1).window(Duration.ofMillis(1), Duration.ofMillis(0), (java.util.function.LongSupplier) null, (Supplier<List<Integer>>) null));
        assertArgument("maxWindowSize", () -> Stream.of(1).window(Duration.ofMillis(1), 0, (Supplier<List<Integer>>) null));

        Stream.WindowHandler<Integer, List<Integer>> invalid = new Stream.WindowHandler<>() {
            @Override
            public int cacheSizeForLateData() {
                return 0;
            }
        };
        AtomicInteger closed = new AtomicInteger();
        Stream<Integer> sliding = Stream.of(1).onClose(closed::incrementAndGet);
        assertArgument("cacheSizeForLateData", () -> sliding.window(Duration.ofMillis(1), Duration.ofMillis(1), null, invalid, null, false));
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, () -> sliding.window(null, (Duration) null, null, invalid, null, false));

        Stream<Integer> sized = Stream.of(1).onClose(closed::incrementAndGet);
        assertArgument("cacheSizeForLateData", () -> sized.window(Duration.ofMillis(1), 1, null, invalid, null, false));
        assertEquals(2, closed.get());
    }

    private static void assertArgument(String messagePart, org.junit.jupiter.api.function.Executable action) {
        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, action);
        assertTrue(failure.getMessage().contains(messagePart), failure.getMessage());
    }
}
