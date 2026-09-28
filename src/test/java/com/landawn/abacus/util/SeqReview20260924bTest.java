package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Stream;

/**
 * Regression tests for cycle 2 of the 2026-09-24 stream/function review, Seq side (findings C-084, C-085, C-086,
 * C-088, C-089, C-090, C-091, C-098 and the nits R3-04, R4-03, R4-04).
 */
public class SeqReview20260924bTest extends TestBase {

    // ------------------------------------------------------------------------------------------------------
    // C-084 split/sliding: a chunk left half-filled by a source failure must not leak into the next chunk
    // ------------------------------------------------------------------------------------------------------

    /** 1..n whose mapping step fails exactly once, on {@code failAt}. */
    private static Seq<Integer, RuntimeException> failingOnceAt(final int n, final int failAt) {
        final AtomicBoolean failed = new AtomicBoolean();
        final List<Integer> source = new ArrayList<>();

        for (int i = 1; i <= n; i++) {
            source.add(i);
        }

        return Seq.<Integer, RuntimeException> of(source).map(x -> {
            if (x == failAt && failed.compareAndSet(false, true)) {
                throw new IllegalStateException("boom@" + x);
            }

            return x;
        });
    }

    /**
     * Iterates the sequence manually (through {@code stream().iterator()}), continuing after each failure; a failure
     * is recorded as its message.
     */
    private static List<Object> drainContinuingAfterFailures(final Seq<? extends Collection<Integer>, RuntimeException> seq) {
        final List<Object> out = new ArrayList<>();

        try (Stream<? extends Collection<Integer>> st = seq.stream()) {
            final Iterator<? extends Collection<Integer>> iter = st.iterator();

            for (int guard = 0; guard < 100; guard++) {
                try {
                    if (!iter.hasNext()) {
                        return out;
                    }

                    out.add(new ArrayList<>(iter.next()));
                } catch (final IllegalStateException e) {
                    out.add(e.getMessage());
                }
            }
        }

        fail("iterator did not terminate");
        return out;
    }

    @Test
    public void testSplit_sourceFailureMidChunk_doesNotLeakIntoNextChunk() {
        final List<Object> expected = Arrays.asList(Arrays.asList(1, 2, 3), "boom@5", Arrays.asList(6, 7, 8), Arrays.asList(9, 10));

        // Custom supplier: the path that used to reuse an uncleared scratch buffer ([4, 6, 7, 8] before the fix).
        assertEquals(expected, drainContinuingAfterFailures(failingOnceAt(10, 5).split(3, n -> new ArrayList<>(n))));
        assertEquals(expected, drainContinuingAfterFailures(failingOnceAt(10, 5).split(3, ArrayList::new)));

        // Bounded collection: the stale elements used to overflow it ("Queue full") and lose the chunk [6, 7, 8].
        assertEquals(expected,
                drainContinuingAfterFailures(failingOnceAt(10, 5).split(3, IntFunctions.<Integer> ofArrayBlockingQueue())));
        assertEquals(expected, drainContinuingAfterFailures(failingOnceAt(10, 5).split(3, ArrayBlockingQueue::new)));

        // Default list factory (fresh list per chunk) gives the same result.
        assertEquals(expected, drainContinuingAfterFailures(failingOnceAt(10, 5).split(3)));
    }

    @Test
    public void testSplit_supplierNeverAskedForMoreThanChunkSizeAfterFailure() {
        final List<Integer> requested = new ArrayList<>();

        drainContinuingAfterFailures(failingOnceAt(10, 5).split(3, n -> {
            requested.add(n);
            return new ArrayList<>(n);
        }));

        assertEquals(Arrays.asList(3, 3, 2), requested);
    }

    @Test
    public void testSliding_sourceFailureMidWindow_doesNotLeakIntoNextWindow() {
        final List<Object> expectedOverlapping = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), "boom@5", Arrays.asList(4, 6, 7),
                Arrays.asList(6, 7, 8), Arrays.asList(7, 8, 9), Arrays.asList(8, 9, 10));

        assertEquals(expectedOverlapping, drainContinuingAfterFailures(failingOnceAt(10, 5).sliding(3, 1, n -> new ArrayList<>(n))));
        assertEquals(expectedOverlapping, drainContinuingAfterFailures(failingOnceAt(10, 5).sliding(3, 1, ArrayBlockingQueue::new)));
        assertEquals(expectedOverlapping, drainContinuingAfterFailures(failingOnceAt(10, 5).sliding(3, 1)));

        final List<Object> expectedTumbling = Arrays.asList(Arrays.asList(1, 2, 3), "boom@5", Arrays.asList(6, 7, 8), Arrays.asList(9, 10));

        assertEquals(expectedTumbling, drainContinuingAfterFailures(failingOnceAt(10, 5).sliding(3, 3, n -> new ArrayList<>(n))));
        assertEquals(expectedTumbling,
                drainContinuingAfterFailures(failingOnceAt(10, 5).sliding(3, 3, IntFunctions.<Integer> ofArrayBlockingQueue())));
    }

    @Test
    public void testSplitAndSliding_customSupplier_failureFreeStillExact() throws Exception {
        // Unicode elements, reused scratch buffer across many chunks, including a partial last chunk.
        final List<String> words = Arrays.asList("é", "中", "😀", "a", "b", "c", "d");

        assertEquals(Arrays.asList(Arrays.asList("é", "中", "😀"), Arrays.asList("a", "b", "c"), Arrays.asList("d")),
                Seq.<String, Exception> of(words).split(3, ArrayBlockingQueue::new).map(ArrayList::new).toList());
        assertEquals(Arrays.asList(Arrays.asList("é", "中"), Arrays.asList("中", "😀"), Arrays.asList("😀", "a"),
                Arrays.asList("a", "b"), Arrays.asList("b", "c"), Arrays.asList("c", "d")),
                Seq.<String, Exception> of(words).sliding(2, 1, ArrayBlockingQueue::new).map(ArrayList::new).toList());

        // empty source: the supplier is never asked
        assertEquals(Collections.emptyList(), Seq.<String, Exception> empty().split(3, n -> {
            throw new AssertionError("not expected");
        }).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-086 split/sliding default ArrayList: no growth slack in emitted chunks
    // ------------------------------------------------------------------------------------------------------

    private static Field arrayListElementData() {
        try {
            final Field f = ArrayList.class.getDeclaredField("elementData");
            f.setAccessible(true);
            return f;
        } catch (final Exception | Error e) { // InaccessibleObjectException without --add-opens java.base/java.util
            return null;
        }
    }

    private static int capacityOf(final Field elementData, final List<?> list) throws IllegalAccessException {
        return ((Object[]) elementData.get(list)).length;
    }

    @Test
    public void testSplitAndSliding_defaultListHasNoSpareCapacity() throws Exception {
        final Field elementData = arrayListElementData();
        Assumptions.assumeTrue(elementData != null, "ArrayList.elementData is not accessible (needs --add-opens java.base/java.util)");

        final List<Integer> source = new ArrayList<>();

        for (int i = 0; i < 150_000; i++) {
            source.add(i);
        }

        // at or below the pre-size bound: exact from the start
        for (final int chunkSize : new int[] { 1, 3, 1000, 5000, 1 << 16 }) {
            final List<Integer> first = Seq.<Integer, Exception> of(source).split(chunkSize).first().orElseThrow();
            assertEquals(chunkSize, first.size());
            assertEquals(chunkSize, capacityOf(elementData, first), "split(" + chunkSize + ")");

            final List<Integer> window = Seq.<Integer, Exception> of(source).sliding(chunkSize, 1).first().orElseThrow();
            assertEquals(chunkSize, capacityOf(elementData, window), "sliding(" + chunkSize + ", 1)");
        }

        // above the bound: grown while filled, then trimmed once
        for (final int chunkSize : new int[] { (1 << 16) + 1, 100_000 }) {
            final List<Integer> first = Seq.<Integer, Exception> of(source).split(chunkSize).first().orElseThrow();
            assertEquals(chunkSize, capacityOf(elementData, first), "split(" + chunkSize + ")");
            assertEquals(source.subList(0, chunkSize), first);

            final List<Integer> window = Seq.<Integer, Exception> of(source).sliding(chunkSize, 7).first().orElseThrow();
            assertEquals(chunkSize, capacityOf(elementData, window), "sliding(" + chunkSize + ", 7)");
        }
    }

    @Test
    public void testSplitAndSliding_largeChunks_contentAndSizes() throws Exception {
        final List<Integer> source = new ArrayList<>();

        for (int i = 0; i < 150_000; i++) {
            source.add(i);
        }

        final List<List<Integer>> chunks = Seq.<Integer, Exception> of(source).split(100_000).toList();
        assertEquals(2, chunks.size());
        assertEquals(source.subList(0, 100_000), chunks.get(0));
        assertEquals(source.subList(100_000, 150_000), chunks.get(1));

        final List<Integer> requested = new ArrayList<>();
        final IntFunction<List<Integer>> recording = n -> {
            requested.add(n);
            return new ArrayList<>(n);
        };

        assertEquals(chunks, Seq.<Integer, Exception> of(source).split(100_000, recording).toList());
        assertEquals(Arrays.asList(100_000, 50_000), requested);

        // oversized chunk on a tiny source still allocates nothing huge
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.<Integer, Exception> of(1, 2, 3).split(Integer.MAX_VALUE).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3)), Seq.<Integer, Exception> of(1, 2, 3).sliding(Integer.MAX_VALUE, 1).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-085 ofKeys/ofValues(Map, filter) accept a filter with a narrower exception type
    // ------------------------------------------------------------------------------------------------------

    // C085-BEGIN
    @Test
    public void testOfKeysOfValues_narrowerFilterExceptionType() throws Exception {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        final Throwables.Predicate<Integer, FileNotFoundException> valueFilter = v -> v > 1;
        final Throwables.Predicate<String, FileNotFoundException> keyFilter = k -> !k.equals("b");
        final Throwables.BiPredicate<String, Integer, FileNotFoundException> entryFilter = (k, v) -> k.equals("a") || v > 2;

        final Seq<String, IOException> keys1 = Seq.ofKeys(map, valueFilter);
        final Seq<String, IOException> keys2 = Seq.ofKeys(map, entryFilter);
        final Seq<Integer, IOException> values1 = Seq.ofValues(map, keyFilter);
        final Seq<Integer, IOException> values2 = Seq.ofValues(map, entryFilter);

        assertEquals(Arrays.asList("b", "c"), keys1.toList());
        assertEquals(Arrays.asList("a", "c"), keys2.toList());
        assertEquals(Arrays.asList(1, 3), values1.toList());
        assertEquals(Arrays.asList(1, 3), values2.toList());

        // a filter failure surfaces with its own (narrower) type
        final Throwables.Predicate<Integer, FileNotFoundException> failing = v -> {
            throw new FileNotFoundException("v=" + v);
        };
        final Seq<String, IOException> failingKeys = Seq.ofKeys(map, failing);
        assertThrows(FileNotFoundException.class, failingKeys::toList);

        // null / empty map
        assertEquals(Collections.emptyList(), Seq.<String, Integer, IOException> ofKeys(null, valueFilter).toList());
        assertEquals(Collections.emptyList(), Seq.<String, Integer, IOException> ofValues(new LinkedHashMap<>(), entryFilter).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.<String, Integer, IOException> ofKeys(map, (Throwables.Predicate<Integer, IOException>) null));
    }
    // C085-END

    // ------------------------------------------------------------------------------------------------------
    // C-089 split(CharSequence, ..): the content is captured at the call
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testSplitCharSequence_snapshotAtCall() throws Exception {
        final StringBuilder sb1 = new StringBuilder("aa,bb,cc");
        final Seq<String, Exception> s1 = Seq.split(sb1, ',');
        sb1.setLength(2);
        assertEquals(Arrays.asList("aa", "bb", "cc"), s1.toList());

        final StringBuilder sb2 = new StringBuilder("aa::bb::cc");
        final Seq<String, Exception> s2 = Seq.split(sb2, "::");
        sb2.setLength(0);
        sb2.append("x");
        assertEquals(Arrays.asList("aa", "bb", "cc"), s2.toList());

        final StringBuilder sb3 = new StringBuilder("aa  bb   cc");
        final Seq<String, Exception> s3 = Seq.split(sb3, Pattern.compile("\\s+"));
        sb3.setLength(3);
        assertEquals(Arrays.asList("aa", "bb", "cc"), s3.toList());

        // growing the builder afterwards is not reflected either
        final StringBuilder sb4 = new StringBuilder("é,😀");
        final Seq<String, Exception> s4 = Seq.split(sb4, ',');
        sb4.append(",tail");
        assertEquals(Arrays.asList("é", "😀"), s4.toList());
    }

    @Test
    public void testSplitCharSequence_nullAndEmpty() throws Exception {
        assertEquals(Collections.emptyList(), Seq.<Exception> split((CharSequence) null, ',').toList());
        assertEquals(Collections.emptyList(), Seq.<Exception> split((CharSequence) null, "::").toList());
        assertEquals(Collections.emptyList(), Seq.<Exception> split((CharSequence) null, Pattern.compile(",")).toList());
        assertEquals(Arrays.asList(""), Seq.<Exception> split(new StringBuilder(), ',').toList());
        assertEquals(Arrays.asList(""), Seq.<Exception> split(new StringBuilder(), "::").toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.<Exception> split(new StringBuilder("a"), ""));
        assertThrows(IllegalArgumentException.class, () -> Seq.<Exception> split(new StringBuilder("a"), (Pattern) null));
    }

    // ------------------------------------------------------------------------------------------------------
    // C-088 splitToLines: \R terminators, null / "" / trailing-terminator rules (doc-only; regression lock)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testSplitToLines_documentedTerminatorsAndEdgeRules() throws Exception {
        final String text = "a" + (char) 0x0B + "b" + (char) 0x0C + "c" + (char) 0x85 + "d" + (char) 0x2028 + "e" + (char) 0x2029 + "f\r\ng\rh\ni";

        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i"), Seq.<Exception> splitToLines(text).toList());
        assertEquals(Arrays.asList("a", "b", "c", "d", "e", "f", "g", "h", "i"), Seq.<Exception> splitToLines(text, false, false).toList());

        // CRLF is one terminator
        assertEquals(Arrays.asList("x", "y"), Seq.<Exception> splitToLines("x\r\ny").toList());

        // trailing terminator -> trailing empty line; "" -> one empty line; null -> empty
        assertEquals(Arrays.asList("a", "b", ""), Seq.<Exception> splitToLines("a\nb\n").toList());
        assertEquals(Arrays.asList(""), Seq.<Exception> splitToLines("").toList());
        assertEquals(Collections.emptyList(), Seq.<Exception> splitToLines(null).toList());
        assertEquals(Collections.emptyList(), Seq.<Exception> splitToLines(null, true, false).toList());

        // with omitEmptyLines those empty lines are dropped
        assertEquals(Arrays.asList(""), Seq.<Exception> splitToLines("", false, false).toList());
        assertEquals(Collections.emptyList(), Seq.<Exception> splitToLines("", false, true).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.<Exception> splitToLines("a\nb\n", false, true).toList());
        assertEquals(Arrays.asList("a", "b"), Seq.<Exception> splitToLines(" a \n\t\n b " + (char) 0x2028, true, true).toList());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-090 buffered(int): the bound is a limit, not an up-front allocation
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testBuffered_hugeBound_doesNotPreallocate() throws Exception {
        final AtomicInteger closed = new AtomicInteger();

        try {
            assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).onClose(closed::incrementAndGet).buffered(Integer.MAX_VALUE).toList());
            assertEquals(1, closed.get());

            assertEquals(Arrays.asList(1, 2, 3), Seq.<Integer, Exception> of(1, 2, 3).buffered(200_000_000).toList());
            assertEquals(Integer.valueOf(1), Seq.<Integer, Exception> of(1, 2, 3).buffered(Integer.MAX_VALUE).first().orElseThrow());
            assertEquals(Collections.emptyList(), Seq.<Integer, Exception> empty().buffered(Integer.MAX_VALUE).toList());
        } catch (final OutOfMemoryError e) {
            fail("buffered(huge bound) must not allocate the whole bound up front: " + e);
        }
    }

    @Test
    public void testBuffered_boundedAboveArrayQueueThreshold_stillBlocksProducer() throws Exception {
        final int bufferSize = (1 << 16) + 100;
        final int total = bufferSize * 3;
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();

        final Seq<Integer, Exception> seq = Seq.<Exception> range(0, total).peek(x -> reads.incrementAndGet()).onClose(closed::incrementAndGet);
        try (Stream<Integer> buffered = seq.buffered(bufferSize).stream()) {
            final Iterator<Integer> iter = buffered.iterator();

            assertEquals(Integer.valueOf(0), iter.next());

            final long deadline = System.currentTimeMillis() + 10_000;

            while (reads.get() < bufferSize && System.currentTimeMillis() < deadline) {
                Thread.sleep(5);
            }

            Thread.sleep(200);

            // consumer took 1; the queue holds at most bufferSize; the producer may hold one more waiting to be offered
            final int readAhead = reads.get();
            assertTrue(readAhead >= bufferSize, "producer should have filled the buffer: " + readAhead);
            assertTrue(readAhead <= bufferSize + 2, "producer ran past the bound: " + readAhead);
        }

        assertEquals(1, closed.get());
    }

    @Test
    public void testBuffered_contentOrderAndValidation() throws Exception {
        final List<Integer> source = new ArrayList<>();

        for (int i = 0; i < 100_000; i++) {
            source.add(i % 7 == 0 ? null : i);
        }

        assertEquals(source, Seq.<Integer, Exception> of(source).buffered(70_000).toList());
        assertEquals(source, Seq.<Integer, Exception> of(source).buffered(3).toList());

        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> s = Seq.<Integer, Exception> of(1).onClose(closed::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> s.buffered(0));
        assertEquals(1, closed.get());
    }

    // ------------------------------------------------------------------------------------------------------
    // C-091 toMap/toImmutableMap merge: a present key with a null value IS passed to the merge function
    // (doc-only; regression lock for the corrected wording)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testToMapMerge_nullCurrentValueReachesMergeFunction() throws Exception {
        final List<String> calls = new ArrayList<>();

        final Map<String, Integer> m = Seq.<Integer, Exception> of(1, 2).toMap(x -> "k", x -> x == 1 ? null : x, (a, b) -> {
            calls.add(a + "," + b);
            return a == null ? -1 : a + b;
        });

        assertEquals(Collections.singletonMap("k", -1), m);
        assertEquals(Arrays.asList("null,2"), calls);

        // a JDK-style merger that assumes Map.merge semantics sees the null
        assertThrows(NullPointerException.class, () -> Seq.<Integer, Exception> of(1, 2).toMap(x -> "k", x -> x == 1 ? null : x, Integer::sum));
        assertThrows(NullPointerException.class, () -> Seq.<Integer, Exception> of(1, 2).toImmutableMap(x -> "k", x -> x == 1 ? null : x, Integer::sum));
        assertThrows(NullPointerException.class,
                () -> Seq.<Integer, Exception> of(1, 2).toMap(x -> "k", x -> x == 1 ? null : x, Integer::sum, LinkedHashMap::new));

        // as with Map.merge, a null result removes the key
        assertEquals(Collections.emptyMap(), Seq.<Integer, Exception> of(1, 2).toMap(x -> "k", x -> x, (a, b) -> null));
        assertEquals(Collections.singletonMap("k", 3), Seq.<Integer, Exception> of(1, 2, 3).toMap(x -> "k", x -> x, (a, b) -> a == 1 ? null : a + b));
    }

    // ------------------------------------------------------------------------------------------------------
    // C-098 Seq.toMap duplicate-key message names the key
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testToMap_duplicateKeyMessageNamesTheKey() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("a", "b", "a").toMap(x -> x, x -> 1));
        assertEquals("Duplicate key a (attempted merging values 1 and 1)", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("a", "bb", "cc").toMap(String::length, x -> x, Suppliers.<Integer, String> ofLinkedHashMap()));
        assertEquals("Duplicate key 2 (attempted merging values bb and cc)", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("x", "y").toImmutableMap(x -> "k", x -> x));
        assertEquals("Duplicate key k (attempted merging values x and y)", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("x", "y").toMap(x -> "k", x -> x, Fnn.throwingMerger()));
        assertEquals("Duplicate key k (attempted merging values x and y)", e.getMessage());

        e = assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("x", "y").toMap(x -> "k", x -> x, (a, b) -> Fn.<String> throwingMerger().apply(a, b)));
        assertEquals("Duplicate key (attempted merging values x and y)", e.getMessage()); // a wrapper is not the shared merger

        // null key and null values
        e = assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("x", "y").toMap(x -> null, x -> x.equals("x") ? null : x));
        assertEquals("Duplicate key null (attempted merging values null and y)", e.getMessage());

        // Unicode key
        e = assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("中", "中").toMap(x -> x, x -> 0));
        assertEquals("Duplicate key 中 (attempted merging values 0 and 0)", e.getMessage());

        // the sequence is closed on the failure
        final AtomicInteger closed = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> Seq.<String, Exception> of("a", "a").onClose(closed::incrementAndGet).toMap(x -> x, x -> 1));
        assertEquals(1, closed.get());
    }

    @Test
    public void testToMap_noDuplicates_andCustomMergerUnchanged() throws Exception {
        final Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        assertEquals(expected, Seq.<String, Exception> of("a", "b").toMap(x -> x, x -> x.charAt(0) - 'a' + 1));
        assertEquals(Collections.singletonMap("k", 3), Seq.<Integer, Exception> of(1, 2).toMap(x -> "k", x -> x, Integer::sum));
        assertEquals(Collections.emptyMap(), Seq.<Integer, Exception> empty().toMap(x -> "k", x -> x));
    }

    // ------------------------------------------------------------------------------------------------------
    // R3-04 throwIfEmpty: an exception supplier that returns null -> NullPointerException
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testThrowIfEmpty_supplierReturnsNull() throws Exception {
        final AtomicInteger closed = new AtomicInteger();
        final Seq<Integer, Exception> empty = Seq.<Integer, Exception> empty().onClose(closed::incrementAndGet);

        final NullPointerException e = assertThrows(NullPointerException.class, () -> empty.throwIfEmpty(() -> null).toList());
        assertEquals("exceptionSupplier returned null", e.getMessage());
        assertEquals(1, closed.get());

        // non-empty: the supplier is never called
        assertEquals(Arrays.asList(1, 2), Seq.<Integer, Exception> of(1, 2).throwIfEmpty(() -> null).toList());

        // a non-null exception still propagates as itself
        final UnsupportedOperationException custom = new UnsupportedOperationException("custom");
        assertTrue(custom == assertThrows(UnsupportedOperationException.class, () -> Seq.<Integer, Exception> empty().throwIfEmpty(() -> custom).count()));
    }

    // ------------------------------------------------------------------------------------------------------
    // C-101 collect(Supplier, ..) / toArray(IntFunction): a null container/array -> NullPointerException
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testCollectAndToArray_nullContainerRejected() {
        for (final boolean empty : new boolean[] { false, true }) {
            final AtomicInteger closed = new AtomicInteger();

            NullPointerException e = assertThrows(NullPointerException.class,
                    () -> (empty ? Seq.<Integer, Exception> empty() : Seq.<Integer, Exception> of(1, 2)).onClose(closed::incrementAndGet)
                            .collect(() -> (List<Integer>) null, (c, t) -> {
                            }));
            assertEquals("supplier returned null", e.getMessage());
            assertEquals(1, closed.get());

            e = assertThrows(NullPointerException.class,
                    () -> (empty ? Seq.<Integer, Exception> empty() : Seq.<Integer, Exception> of(1, 2)).onClose(closed::incrementAndGet)
                            .collect(() -> (List<Integer>) null, List::add, c -> c));
            assertEquals("supplier returned null", e.getMessage());
            assertEquals(2, closed.get());

            e = assertThrows(NullPointerException.class,
                    () -> (empty ? Seq.<String, Exception> empty() : Seq.<String, Exception> of("a", "é")).onClose(closed::incrementAndGet)
                            .toArray(n -> (String[]) null));
            assertEquals("generator returned null", e.getMessage());
            assertEquals(3, closed.get());
        }
    }

    @Test
    public void testCollectAndToArray_normalPathsUnchanged() throws Exception {
        assertEquals(Arrays.asList("a", "é"), Seq.<String, Exception> of("a", "é").collect(ArrayList::new, List::add));
        assertEquals(Collections.emptyList(), Seq.<String, Exception> empty().collect(ArrayList::new, List::add));
        assertEquals(Integer.valueOf(2), Seq.<String, Exception> of("a", "b").collect(ArrayList::new, List::add, List::size));
        assertTrue(Arrays.equals(new String[] { "a", "b" }, Seq.<String, Exception> of("a", "b").toArray(String[]::new)));
        assertEquals(0, Seq.<String, Exception> empty().toArray(String[]::new).length);
    }

    // ------------------------------------------------------------------------------------------------------
    // R4-03 groupTo(k, v, downstream, mapFactory) javadoc example compiles and prints what it claims
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testGroupToJavadocExample() throws Exception {
        final LinkedHashMap<String, Optional<Double>> maxSalaryByDept = Seq.<String, Exception> of("S:1.0", "L:2.5", "L:3.0", "S:0.5")
                .groupTo(rec -> rec.substring(0, 1), rec -> Double.parseDouble(rec.substring(2)), Collectors.max(Double::compare), LinkedHashMap::new);

        assertEquals("{S=Optional[1.0], L=Optional[3.0]}", maxSalaryByDept.toString());
    }

    // ------------------------------------------------------------------------------------------------------
    // R4-04 transformViaStream / sps: a null returned stream is an empty sequence that still closes this one
    // (doc-only; regression lock)
    // ------------------------------------------------------------------------------------------------------

    @Test
    public void testTransformViaStreamAndSps_nullResultIsEmptyAndClosesSource() throws Exception {
        final ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            final List<java.util.function.Function<Seq<Integer, Exception>, Seq<Integer, Exception>>> ops = Arrays.asList( //
                    s -> s.transformViaStream(st -> null), //
                    s -> s.transformViaStream(st -> null, false), //
                    s -> s.transformViaStream(st -> null, true), //
                    s -> s.sps(st -> null), //
                    s -> s.sps(2, st -> null), //
                    s -> s.sps(2, executor, st -> null));

            for (int i = 0; i < ops.size(); i++) {
                final AtomicInteger closed = new AtomicInteger();
                assertEquals(Collections.emptyList(), ops.get(i).apply(Seq.<Integer, Exception> of(1, 2).onClose(closed::incrementAndGet)).toList(), "op " + i);
                assertEquals(1, closed.get(), "op " + i);

                // closing without traversal closes the source too
                final AtomicInteger closed2 = new AtomicInteger();
                ops.get(i).apply(Seq.<Integer, Exception> of(1, 2).onClose(closed2::incrementAndGet)).close();
                assertEquals(1, closed2.get(), "op " + i + " close only");
            }
        } finally {
            executor.shutdown();
        }
    }
}
