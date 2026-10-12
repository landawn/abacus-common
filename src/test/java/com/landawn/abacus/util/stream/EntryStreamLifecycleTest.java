package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;

/** EntryStream lifecycle contracts. */
public class EntryStreamLifecycleTest extends EntryStreamTestSupport {

    // close

    @Test
    public void testClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        EntryStream<String, Integer> stream = EntryStream.of(testMap).onClose(() -> closed.set(true));
        stream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testEntriesRejectClosedStreamImmediately() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        Assertions.assertThrows(IllegalStateException.class, stream::entries);
    }

    @Test
    public void testKeysRejectClosedMapBackedStream() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        Assertions.assertThrows(IllegalStateException.class, stream::keys);
    }

    @Test
    public void testValuesRejectClosedMapBackedStream() {
        EntryStream<String, Integer> stream = EntryStream.of(testMap);
        stream.close();

        Assertions.assertThrows(IllegalStateException.class, stream::values);
    }



    /**
     * r9502 added {@code checkArgNotNull(collector, cs.collector)} to both three-arg {@code collapseBy*}
     * overloads; r9509 (a blank-message revert of that commit's EntryStream work) removed it and the matching
     * {@code @throws} line, and nothing restored either. The collector was then only reached through
     * {@code Collectors.mapping(mapper, collector)}, evaluated as an argument to {@code _stream.collapse(...)} -
     * a static check that names {@code downstream} and has no stream to close.
     *
     * <p>Asserting only {@code IllegalArgumentException} would be vacuous: the unfixed code already threw one.
     * The test has to pin the parameter name and the close.
     */
    @Test
    public void testCollapseBy_nullCollectorIsRejectedAndTheStreamIsClosed() {
        final MutableBoolean closed = MutableBoolean.of(false);

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> EntryStream.of("a", 1, "a", 2).onClose(closed::setTrue).collapseByKey((k1, k2) -> k1.equals(k2), Map.Entry::getValue, null));
        assertTrue(e.getMessage().contains("collector"), "message must name the parameter, was: " + e.getMessage());
        assertTrue(closed.isTrue(), "collapseByKey(.., null) must close the stream before throwing");

        closed.setFalse();
        e = assertThrows(IllegalArgumentException.class,
                () -> EntryStream.of("a", 1, "b", 1).onClose(closed::setTrue).collapseByValue((v1, v2) -> v1.equals(v2), Map.Entry::getKey, null));
        assertTrue(e.getMessage().contains("collector"), "message must name the parameter, was: " + e.getMessage());
        assertTrue(closed.isTrue(), "collapseByValue(.., null) must close the stream before throwing");

        // the ordinary paths are unaffected
        assertEquals(N.asList(N.asList(1, 2)),
                EntryStream.of("a", 1, "a", 2).collapseByKey((k1, k2) -> k1.equals(k2), Map.Entry::getValue, Collectors.toList()).toList());
    }

    @Test
    public void testFlatMapClosesMappedEntryStream() {
        final EntryStream<String, Integer> mappedStream = EntryStream.of("mapped", 1);

        assertEquals(1, EntryStream.of("source", 1).flatMap(entry -> mappedStream).count());
        assertThrows(IllegalStateException.class, () -> mappedStream.count());
    }

    @Test
    public void testFlatMapKeyAndValueCloseMappedStreams() {
        final Stream<String> mappedKeys = Stream.of("mapped-key");
        final Stream<Integer> mappedValues = Stream.of(2);

        assertEquals(1, EntryStream.of("source", 1).flatMapKey(key -> mappedKeys).count());
        assertEquals(1, EntryStream.of("source", 1).flatMapValue(value -> mappedValues).count());
        assertThrows(IllegalStateException.class, () -> mappedKeys.count());
        assertThrows(IllegalStateException.class, () -> mappedValues.count());
    }



    // G095-01: close handlers still run exactly once, and a mapper failure propagates unchanged and closes the stream
    @Test
    public void testFlatmapKeyValue_sequentialCloseAndMapperFailure() {
        final AtomicInteger closed = new AtomicInteger();
        final List<Map.Entry<String, Integer>> result = EntryStream.of("a", 1, "b", 2)
                .onClose(closed::incrementAndGet)
                .flatmapValue(v -> Arrays.asList(v, -v))
                .toList();

        assertEquals(Arrays.asList(g095Entry("a", 1), g095Entry("a", -1), g095Entry("b", 2), g095Entry("b", -2)), result);
        assertEquals(1, closed.get());

        final AtomicInteger closedOnFailure = new AtomicInteger();
        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> EntryStream.of("a", 1, "b", 2).onClose(closedOnFailure::incrementAndGet).flatmapKey((k, v) -> {
                    if (k.equals("b")) {
                        throw new IllegalStateException("boom-" + k);
                    }

                    return Collections.singletonList(k + v);
                }).toList());

        assertEquals("boom-b", ex.getMessage());
        assertEquals(1, closedOnFailure.get());

        final EntryStream<String, Integer> stream = EntryStream.of("a", 1);
        stream.close();
        assertThrows(IllegalStateException.class, () -> stream.flatmapKey(k -> Arrays.asList(k)));
        assertThrows(IllegalArgumentException.class, () -> EntryStream.of("a", 1).flatmapValue((Function<Integer, Collection<Integer>>) null));
    }

    @Test
    public void testKeysPropagateCloseHandlers() {
        AtomicInteger closeCount = new AtomicInteger();

        EntryStream.of(testMap).onClose(closeCount::incrementAndGet).keys().count();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        EntryStream.of(testMap).onClose(() -> closed.set(true)).count();
        assertTrue(closed.get());
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            long count = EntryStream.of(testMap).parallel(executor).filter(e -> e.getValue() > 2).count();

            assertEquals(3, count);

            List<Entry<String, Integer>> result = EntryStream.of(testMap)
                    .parallel(3, executor)
                    .map(e -> new AbstractMap.SimpleEntry<>(e.getKey().toUpperCase(), e.getValue() * 2))
                    .toList();

            assertEquals(5, result.size());
            assertTrue(result.stream().anyMatch(e -> "ONE".equals(e.getKey()) && e.getValue() == 2));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testResourceManagement() {
        AtomicBoolean resourceClosed = new AtomicBoolean(false);

        try {
            EntryStream.of(testMap).onClose(() -> resourceClosed.set(true)).map(e -> {
                throw new RuntimeException("Force exception");
            }).toList();
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
        }

        assertTrue(resourceClosed.get());
    }



    // Stream review 2026-09-09 (pass B) - selectByKey/selectByValue argument validation

    /**
     * {@code Fn.instanceOf(clazz)} was evaluated as the ARGUMENT to {@code filterByKey}/{@code filterByValue}, so a
     * null class threw before the delegate's own closing {@code checkArgNotNull} ran, leaving the stream open.
     * Every sibling ({@code filterByKey}, {@code filterByValue}, {@code mapKey}, ...) closes first.
     * See also {@code testCollapseBy_nullCollectorIsRejectedAndTheStreamIsClosed} for the same shape.
     */
    @Test
    public void testSelectByKeyValue_nullClassIsRejectedAndTheStreamIsClosed() {
        final MutableBoolean closed = MutableBoolean.of(false);

        assertThrows(IllegalArgumentException.class, () -> EntryStream.of("a", 1).onClose(closed::setTrue).selectByKey(null));
        assertTrue(closed.isTrue(), "selectByKey(null) must close the stream before throwing");

        closed.setFalse();
        assertThrows(IllegalArgumentException.class, () -> EntryStream.of("a", 1).onClose(closed::setTrue).selectByValue(null));
        assertTrue(closed.isTrue(), "selectByValue(null) must close the stream before throwing");

        // the ordinary paths are unaffected
        assertEquals(1, EntryStream.of("a", 1).selectByKey(String.class).count());
        assertEquals(0, EntryStream.of("a", 1).selectByKey(Integer.class).count());
        assertEquals(1, EntryStream.of("a", 1).selectByValue(Integer.class).count());
    }

    @Test
    public void testTransformViaStreamDeferredTreatsNullResultAsEmptyAndClosesInput() {
        final AtomicInteger inputClosed = new AtomicInteger();
        final EntryStream<String, Integer> input = EntryStream.of("a", 1, "b", 2).onClose(inputClosed::incrementAndGet);

        final List<Map.Entry<String, Integer>> result = input.<String, Integer> transformViaStream(s -> null, true).toList();

        assertTrue(result.isEmpty());
        assertEquals(1, inputClosed.get());
    }

    @Test
    public void testTransformViaStreamTreatsNullResultAsEmptyAndClosesInput() {
        final AtomicInteger inputClosed = new AtomicInteger();
        final EntryStream<String, Integer> input = EntryStream.of("a", 1, "b", 2).onClose(inputClosed::incrementAndGet);

        final EntryStream<String, Integer> result = input.<String, Integer> transformViaStream(s -> null);

        assertEquals(0, result.count());
        assertEquals(1, inputClosed.get());
    }

    @Test
    public void testValuesPropagateCloseHandlers() {
        AtomicInteger closeCount = new AtomicInteger();

        EntryStream.of(testMap).onClose(closeCount::incrementAndGet).values().count();

        assertEquals(1, closeCount.get());
    }

}
