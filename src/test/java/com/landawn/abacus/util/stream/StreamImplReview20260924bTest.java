package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.ObjIterator;

/**
 * Cycle-2 regression tests (2026-09-24) for the object-stream implementation classes ({@code AbstractStream},
 * {@code ArrayStream}, {@code IteratorStream}, {@code Parallel*Stream}), {@code StreamBase}, {@code BaseStream} and
 * {@code Collectors}. Ledger: {@code scripts/cross_review/StreamFamily_Fn_MoreCollectors_Seq_Fnn_Throwables_ledger_2026-09-24.md},
 * table "Cycle 2 findings".
 */
public class StreamImplReview20260924bTest extends TestBase {

    private static List<Integer> range(final int from, final int to) {
        final List<Integer> list = new ArrayList<>();

        for (int i = from; i < to; i++) {
            list.add(i);
        }

        return list;
    }

    /**
     * The source kinds of an object stream: array, iterator, parallel array (default ITERATOR split strategy), parallel
     * iterator and - U23-01 (2026-09-25) - parallel array with the ARRAY split strategy, whose terminals carry their
     * own copies of the C-016/C-101 factory-null checks.
     */
    private static List<Function<List<Integer>, Stream<Integer>>> sourceKinds() {
        return Arrays.asList(list -> Stream.of(list.toArray(new Integer[0])), list -> Stream.of(list.iterator()),
                list -> Stream.of(list.toArray(new Integer[0])).parallel(3), list -> Stream.of(list.iterator()).parallel(3),
                list -> Stream.of(list.toArray(new Integer[0])).parallel(new BaseStream.ParallelSettings(3, BaseStream.SplitStrategy.ARRAY, null)));
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-086 IteratorStream split/sliding: exact pre-size up to a threshold, trimmed above it; supplier gets the
    // exact count; the reused scratch buffer of a custom supplier is reset before it is filled.
    // ---------------------------------------------------------------------------------------------------------

    /** Capacity of an ArrayList, or -1 when java.util is not opened to this module (the Maven suite). */
    private static int capacity(final Object list) {
        try {
            final Field f = ArrayList.class.getDeclaredField("elementData");
            f.setAccessible(true);
            return ((Object[]) f.get(list)).length;
        } catch (final Exception | Error e) { // InaccessibleObjectException without --add-opens java.base/java.util
            return -1;
        }
    }

    @Test
    public void testC086_splitAndSlidingChunksCarryNoSpareCapacity() {
        final List<List<Integer>> chunks = Stream.of(range(0, 100_000).iterator()).split(100_000).toList();
        assertEquals(1, chunks.size());
        assertEquals(range(0, 100_000), chunks.get(0));
        final int cap = capacity(chunks.get(0));
        Assumptions.assumeTrue(cap >= 0, "ArrayList capacity is not observable without --add-opens java.base/java.util");
        assertEquals(100_000, cap); // c1final: 132,859 (grown from 1024 by 1.5x)

        // Above the exact-presize threshold the chunk size is only an upper bound: trimmed to the element count.
        final List<Integer> huge = Stream.of(Arrays.asList(1, 2, 3).iterator()).split(Integer.MAX_VALUE).first().orElseThrow();
        assertEquals(Arrays.asList(1, 2, 3), huge);
        assertEquals(3, capacity(huge)); // c1final: 1024

        final List<Integer> bigAboveThreshold = Stream.of(range(0, 70_000).iterator()).split(100_000_000).first().orElseThrow();
        assertEquals(70_000, bigAboveThreshold.size());
        assertEquals(70_000, capacity(bigAboveThreshold));

        final List<List<Integer>> windows = Stream.of(range(0, 100_005).iterator()).sliding(100_000, 5).toList();
        assertEquals(2, windows.size());
        assertEquals(100_000, capacity(windows.get(0))); // c1final: 132,859
        assertEquals(range(5, 100_005), windows.get(1));

        final List<Integer> slidingHuge = Stream.of(Arrays.asList(1, 2).iterator()).sliding(Integer.MAX_VALUE).first().orElseThrow();
        assertEquals(Arrays.asList(1, 2), slidingHuge);
        assertEquals(2, capacity(slidingHuge));
    }

    @Test
    public void testC086_customSupplierGetsExactCountsAndDistinctChunks() {
        final List<Integer> requested = new ArrayList<>();
        final IntFunction<LinkedList<Integer>> supplier = n -> {
            requested.add(n);
            return new LinkedList<>();
        };

        final List<LinkedList<Integer>> chunks = Stream.of(range(1, 11).iterator()).split(4, supplier).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3, 4), Arrays.asList(5, 6, 7, 8), Arrays.asList(9, 10)), chunks);
        assertEquals(Arrays.asList(4, 4, 2), requested);
        assertNotSame(chunks.get(0), chunks.get(1));

        requested.clear();
        final List<LinkedList<Integer>> windows = Stream.of(range(1, 7).iterator()).sliding(3, 2, supplier).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(3, 4, 5), Arrays.asList(5, 6)), windows);
        assertEquals(Arrays.asList(3, 3, 2), requested);

        // Bounded suppliers keep working: they never receive more than they were asked to hold.
        assertEquals(Arrays.asList(40, 40, 20),
                Stream.of(range(0, 100).iterator()).split(40, IntFunctions.ofArrayBlockingQueue()).map(Collection::size).toList());

        // The default factory still hands out the list itself (no copy) and every chunk is a distinct list.
        final List<List<Integer>> defaults = Stream.of(range(0, 5).iterator()).split(2, IntFunctions.ofList()).toList();
        assertEquals(Arrays.asList(Arrays.asList(0, 1), Arrays.asList(2, 3), Arrays.asList(4)), defaults);
        assertNotSame(defaults.get(0), defaults.get(1));
    }

    @Test
    public void testC086_reusedBufferIsResetAfterASourceFailure() {
        // The source fails once on 5; the half-read chunk [4] must not leak into the next chunk.
        final Iterator<Integer> failingOnce = new ObjIterator<>() {
            private int next = 1;
            private boolean failed = false;

            @Override
            public boolean hasNext() {
                return next <= 10;
            }

            @Override
            public Integer next() {
                if (next == 5 && !failed) {
                    failed = true;
                    next++;
                    throw new IllegalStateException("boom");
                }

                return next++;
            }
        };

        final Iterator<LinkedList<Integer>> it = Stream.of(failingOnce).split(3, n -> new LinkedList<>()).iterator();
        assertEquals(Arrays.asList(1, 2, 3), it.next());
        assertThrows(IllegalStateException.class, it::next);
        assertEquals(Arrays.asList(6, 7, 8), it.next());
        assertEquals(Arrays.asList(9, 10), it.next());
        assertFalse(it.hasNext());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-090 buffered(int): the bound is not allocated up front; bounded blocking kept.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC090_bufferedWithHugeBoundDoesNotPreallocate() {
        final AtomicInteger closed = new AtomicInteger();
        // c1final: OutOfMemoryError "Requested array size exceeds VM limit" at the intermediate call; source left open.
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(Arrays.asList(1, 2, 3).iterator()).onClose(closed::incrementAndGet).buffered(Integer.MAX_VALUE).toList());
        assertEquals(1, closed.get());

        assertEquals(range(0, 100), Stream.of(range(0, 100).iterator()).buffered(1 << 28).toList());
        assertEquals(range(0, 100), Stream.of(range(0, 100).iterator()).buffered(1).toList());
        assertEquals(range(0, 100), Stream.of(range(0, 100).iterator()).buffered(StreamBase.MAX_BUFFERED_SIZE).toList());
        assertEquals(range(0, 100), Stream.of(range(0, 100).iterator()).buffered(StreamBase.MAX_BUFFERED_SIZE + 1).toList());
    }

    @Test
    public void testC090_bufferedStaysBounded() {
        for (final int bufferSize : new int[] { 2, StreamBase.MAX_BUFFERED_SIZE + 100 }) { // ArrayBlockingQueue and LinkedBlockingQueue paths
            final AtomicInteger pulled = new AtomicInteger();

            assertTimeoutPreemptively(Duration.ofSeconds(20), () -> {
                try (Stream<Integer> s = Stream.of(new ObjIterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        return true;
                    }

                    @Override
                    public Integer next() {
                        return pulled.incrementAndGet();
                    }
                }).buffered(bufferSize)) {
                    final Iterator<Integer> it = s.iterator();
                    assertEquals(1, it.next());
                    Thread.sleep(300);
                    // consumed 1 + queue capacity + at most one element held by the blocked producer
                    assertTrue(pulled.get() <= bufferSize + 2, "pulled " + pulled.get() + " with bufferSize " + bufferSize);
                }
            });
        }
    }

    @Test
    public void testC090_bufferedArgumentValidationUnchanged() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = Stream.of(Arrays.asList(1).iterator()).onClose(closed::incrementAndGet);
        assertThrows(IllegalArgumentException.class, () -> s.buffered(0));
        assertEquals(1, closed.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-098 Collectors.toConcurrentMap: the duplicate-key message names the key (accumulator and combiner).
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC098_toConcurrentMapDuplicateKeyNamesKey() {
        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> Stream.of("é", "b", "é").collect(Collectors.toConcurrentMap(x -> x, String::length)));
        assertEquals("Duplicate key é (attempted merging values 1 and 1)", e.getMessage());

        e = assertThrows(IllegalStateException.class,
                () -> Stream.of("a", "a").collect(Collectors.toConcurrentMap(x -> x, x -> 1, (Supplier<ConcurrentMap<String, Integer>>) ConcurrentHashMap::new)));
        assertEquals("Duplicate key a (attempted merging values 1 and 1)", e.getMessage());

        e = assertThrows(IllegalStateException.class,
                () -> Stream.of(range(0, 1000).toArray(new Integer[0])).parallel(4).collect(Collectors.toConcurrentMap(x -> x % 500, x -> x)));
        assertTrue(e.getMessage().startsWith("Duplicate key "), e.getMessage());

        // The combiner path (used when containers are merged, e.g. by a JDK parallel stream).
        final java.util.stream.Collector<String, ?, ConcurrentMap<String, Integer>> c = Collectors.toConcurrentMap(x -> x, String::length);
        @SuppressWarnings("unchecked")
        final BinaryOperator<ConcurrentMap<String, Integer>> combiner = (BinaryOperator<ConcurrentMap<String, Integer>>) (BinaryOperator<?>) c.combiner();
        final ConcurrentMap<String, Integer> m1 = new ConcurrentHashMap<>(Map.of("a", 1, "k", 1));
        final ConcurrentMap<String, Integer> m2 = new ConcurrentHashMap<>(Map.of("k", 2));
        e = assertThrows(IllegalStateException.class, () -> combiner.apply(m1, m2));
        assertEquals("Duplicate key k (attempted merging values 1 and 2)", e.getMessage());

        final ConcurrentMap<String, Integer> m3 = new ConcurrentHashMap<>(Map.of("z", 3));
        assertEquals(Map.of("a", 1, "k", 1, "z", 3), combiner.apply(m1, m3));
    }

    @Test
    public void testC098_toConcurrentMapOtherBehaviourUnchanged() {
        assertEquals(Map.of("a", 1, "bb", 2), Stream.of("a", "bb").collect(Collectors.toConcurrentMap(x -> x, String::length)));
        assertEquals(Map.of("a", 3), Stream.of("a", "a", "a").collect(Collectors.toConcurrentMap(x -> x, x -> 1, Integer::sum)));
        // ConcurrentMap.merge rejects a null value with NPE; the throwing-merger path keeps that.
        assertThrows(NullPointerException.class, () -> Stream.of("a").collect(Collectors.toConcurrentMap(x -> x, x -> (Integer) null)));
        // toMap still names the key too.
        assertEquals("Duplicate key a (attempted merging values 1 and 1)",
                assertThrows(IllegalStateException.class, () -> Stream.of("a", "a").collect(Collectors.toMap(x -> x, x -> 1))).getMessage());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-100 persistToCsv: later bean rows may be of another class that has every header property.
    // ---------------------------------------------------------------------------------------------------------

    public static class Animal {
        private String name;

        public Animal() {
        }

        public Animal(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class Dog extends Animal {
        private String breed;

        public Dog() {
        }

        public Dog(final String name, final String breed) {
            super(name);
            this.breed = breed;
        }

        public String getBreed() {
            return breed;
        }

        public void setBreed(final String breed) {
            this.breed = breed;
        }
    }

    public static class Cat extends Animal {
        private int lives;

        public Cat() {
        }

        public Cat(final String name, final int lives) {
            super(name);
            this.lives = lives;
        }

        public int getLives() {
            return lives;
        }

        public void setLives(final int lives) {
            this.lives = lives;
        }
    }

    public static class Parrot extends Animal {
        public Parrot() {
        }

        public Parrot(final String name) {
            super(name);
        }

        @Override
        public String getName() {
            return "Polly-" + super.getName();
        }
    }

    public static class Rock {
        private int weight;

        public int getWeight() {
            return weight;
        }

        public void setWeight(final int weight) {
            this.weight = weight;
        }
    }

    @Test
    public void testC100_persistToCsvSiblingSubclassesWithSupertypeHeaders() throws Exception {
        final StringWriter w = new StringWriter();
        // c1final: IAE "CSV row 2 is a ...Cat, but the first row is a ...Dog"
        assertEquals(3, Stream.<Animal> of(new Dog("rex", "lab"), new Cat("tom", 9), new Dog("fido", "pug")).persistToCsv(Arrays.asList("name"), w));
        assertEquals("\"name\"\n\"rex\"\n\"tom\"\n\"fido\"", w.toString().trim());

        // Each row is read through its own class's accessor (an overriding getter is honoured).
        final StringWriter w2 = new StringWriter();
        assertEquals(3, Stream.<Animal> of(new Dog("rex", "lab"), new Parrot("pa"), new Cat("ü", 1)).persistToCsv(Arrays.asList("name"), w2));
        assertEquals("\"name\"\n\"rex\"\n\"Polly-pa\"\n\"ü\"", w2.toString().trim());

        // A supertype first row followed by a subclass (worked before and still works).
        final StringWriter w3 = new StringWriter();
        assertEquals(2, Stream.<Animal> of(new Animal("a"), new Dog("d", "x")).persistToCsv(Arrays.asList("name"), w3));
        assertEquals("\"name\"\n\"a\"\n\"d\"", w3.toString().trim());

        // Iterator-backed source, same rule.
        final StringWriter w4 = new StringWriter();
        assertEquals(2, Stream.<Animal> of(Arrays.<Animal> asList(new Cat("c", 3), new Dog("d", "x")).iterator()).persistToCsv(Arrays.asList("name"), w4));
        assertEquals("\"name\"\n\"c\"\n\"d\"", w4.toString().trim());
    }

    @Test
    public void testC100_persistToCsvRowLackingAHeaderPropertyIsRejected() {
        // Headers derived from the first row (Dog: breed, name); a Cat has no 'breed'.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> Stream.<Animal> of(new Dog("rex", "lab"), new Cat("tom", 9)).persistToCsv(new StringWriter()));
        assertTrue(e.getMessage().contains("CSV row 2") && e.getMessage().contains("'breed'"), e.getMessage());

        // Explicit headers: the third row (a Cat) lacks 'breed'.
        e = assertThrows(IllegalArgumentException.class, () -> Stream.<Animal> of(new Dog("a", "b"), new Dog("c", "d"), new Cat("tom", 9))
                .persistToCsv(Arrays.asList("name", "breed"), new StringWriter()));
        assertTrue(e.getMessage().contains("CSV row 3") && e.getMessage().contains("'breed'"), e.getMessage());

        // An unrelated bean lacking the property.
        e = assertThrows(IllegalArgumentException.class,
                () -> Stream.<Object> of(new Dog("a", "b"), new Rock()).persistToCsv(Arrays.asList("name"), new StringWriter()));
        assertTrue(e.getMessage().contains("CSV row 2") && e.getMessage().contains("'name'"), e.getMessage());

        // A non-bean row after a bean row.
        e = assertThrows(IllegalArgumentException.class,
                () -> Stream.<Object> of(new Dog("a", "b"), "text").persistToCsv(Arrays.asList("name"), new StringWriter()));
        assertTrue(e.getMessage().contains("CSV row 2") && e.getMessage().contains("bean"), e.getMessage());

        // A null row.
        e = assertThrows(IllegalArgumentException.class,
                () -> Stream.<Object> of(new Dog("a", "b"), null).persistToCsv(Arrays.asList("name"), new StringWriter()));
        assertTrue(e.getMessage().contains("CSV row 2") && e.getMessage().contains("null"), e.getMessage());
    }

    @Test
    public void testC100_persistToCsvClosesStreamOnRejectedRow() {
        final AtomicInteger closed = new AtomicInteger();
        assertThrows(IllegalArgumentException.class, () -> Stream.<Object> of(new Dog("a", "b"), new Rock())
                .onClose(closed::incrementAndGet)
                .persistToCsv(Arrays.asList("name"), new StringWriter()));
        assertEquals(1, closed.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-111 AbstractStream.sorted(Comparator): a boxed flagged-sorted primitive stream is returned as is.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC111_boxedSortedRangeIsNotResorted() {
        final Stream<Integer> ints = IntStream.range(0, 5).boxed();
        assertSame(ints, ints.sorted()); // c1final: a new, re-sorted stream
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), ints.toList());

        final Stream<Long> longs = LongStream.range(-2, 3).boxed();
        assertSame(longs, longs.sorted());
        assertEquals(Arrays.asList(-2L, -1L, 0L, 1L, 2L), longs.toList());

        final Stream<Double> doubles = DoubleStream.of(3.0, -0.0, 0.0, Double.NaN, -1.0).sorted().boxed();
        assertSame(doubles, doubles.sorted());
        assertEquals(Arrays.asList(-1.0, -0.0, 0.0, 3.0, Double.NaN), doubles.toList());

        final Stream<Character> chars = CharStream.of('c', 'a', 'b').sorted().boxed();
        assertSame(chars, chars.sorted());
        assertEquals(Arrays.asList('a', 'b', 'c'), chars.toList());

        // A huge flagged range no longer materializes (c1final: full copy + sort, OOM for MIN..MAX).
        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertEquals(Arrays.asList(Integer.MIN_VALUE, Integer.MIN_VALUE + 1),
                        IntStream.range(Integer.MIN_VALUE, Integer.MAX_VALUE).boxed().sorted().limit(2).toList()));
    }

    @Test
    public void testC111_otherComparatorsStillSort() {
        assertEquals(Arrays.asList(4, 3, 2, 1, 0), IntStream.range(0, 5).boxed().sorted(Comparator.reverseOrder()).toList());
        assertEquals(Arrays.asList(4, 3, 2, 1, 0), IntStream.range(0, 5).boxed().reverseSorted().toList());
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(3, 1, 2).sorted().toList());
        assertEquals(Arrays.asList(1, 2, 3), Stream.of(Arrays.asList(3, 1, 2).iterator()).sorted().sorted().toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-112 StreamBase.collectingCombiner: every unsupported container type is an IllegalArgumentException.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC112_combinerRejectsUnsupportedContainersWithIAE() {
        @SuppressWarnings("unchecked")
        final java.util.function.BiConsumer<Object, Object> combiner = StreamBase.collectingCombiner;
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> combiner.accept(new AtomicInteger(), new AtomicInteger()));
        assertTrue(e.getMessage().contains("AtomicInteger cannot be combined"), e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> combiner.accept(new int[1], new int[1]));
        assertTrue(e.getMessage().contains("cannot be combined"), e.getMessage());

        final AtomicInteger closed = new AtomicInteger();
        e = assertThrows(IllegalArgumentException.class, () -> Stream.of(range(0, 1000).toArray(new Integer[0]))
                .parallel(4)
                .onClose(closed::incrementAndGet)
                .collect(AtomicInteger::new, (a, x) -> a.addAndGet(x)));
        assertTrue(e.getMessage().contains("cannot be combined"), e.getMessage());
        assertEquals(1, closed.get());

        e = assertThrows(IllegalArgumentException.class,
                () -> IntStream.range(0, 1000).parallel(4).collect(AtomicInteger::new, (a, x) -> a.addAndGet(x)));
        assertTrue(e.getMessage().contains("cannot be combined"), e.getMessage());

        // Supported containers still combine.
        assertEquals(1000, Stream.of(range(0, 1000).iterator()).parallel(4).collect(ArrayList::new, ArrayList::add).size());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-115 Stream.containsDuplicates(): array elements compare by content, like distinct().
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC115_containsDuplicatesComparesArraysByContent() {
        final int[] a1 = { 1 };
        final int[] a2 = { 1 };
        assertTrue(Stream.of(a1, a2).containsDuplicates()); // c1final: false (identity)
        assertTrue(Stream.of(Arrays.asList(a1, a2).iterator()).containsDuplicates());
        assertTrue(Stream.of(a1, a2).parallel(2).containsDuplicates());
        assertEquals(1, Stream.of(a1, a2).distinct().count());

        assertTrue(Stream.<Object> of(new String[] { "é", null }, new String[] { "é", null }).containsDuplicates());
        assertFalse(Stream.of(new int[] { 1 }, new int[] { 2 }).containsDuplicates());
        assertFalse(Stream.of(new long[] { 1 }, new int[] { 1 }).containsDuplicates());
        assertTrue(Stream.of(a1, a1).containsDuplicates());

        assertTrue(Stream.of("a", null, null).containsDuplicates());
        assertFalse(Stream.of("a", null, "b").containsDuplicates());
        assertFalse(Stream.<String> empty().containsDuplicates());
        assertTrue(Stream.of(1, 2, 1).containsDuplicates());
    }

    // ---------------------------------------------------------------------------------------------------------
    // D1-04 split(Predicate, ..): the predicate is evaluated exactly once per element.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testD104_splitByPredicateTestsEachElementOnce() {
        final List<Integer> data = Arrays.asList(1, 3, 2, 4, 5, 7, 6);
        final List<List<Integer>> expected = Arrays.asList(Arrays.asList(1, 3), Arrays.asList(2, 4), Arrays.asList(5, 7), Arrays.asList(6));

        for (final Function<List<Integer>, Stream<Integer>> kind : Arrays.<Function<List<Integer>, Stream<Integer>>> asList(
                list -> Stream.of(list.toArray(new Integer[0])), list -> Stream.of(list.iterator()))) {
            final AtomicInteger calls = new AtomicInteger();
            final Predicate<Integer> even = x -> {
                calls.incrementAndGet();
                return x % 2 == 0;
            };

            assertEquals(expected, kind.apply(data).split(even).toList());
            assertEquals(data.size(), calls.get()); // c1final: 10 (each group boundary tested twice)

            calls.set(0);
            assertEquals(expected, kind.apply(data).split(even, ArrayList::new).toList());
            assertEquals(data.size(), calls.get());

            calls.set(0);
            assertEquals(expected, kind.apply(data).split(even, java.util.stream.Collectors.toList()).toList());
            assertEquals(data.size(), calls.get());

            calls.set(0);
            assertEquals(Arrays.asList(Arrays.asList(1)), kind.apply(Arrays.asList(1)).split(even).toList());
            assertEquals(1, calls.get());

            calls.set(0);
            assertEquals(Collections.emptyList(), kind.apply(Collections.<Integer> emptyList()).split(even).toList());
            assertEquals(0, calls.get());

            // Every element its own group.
            calls.set(0);
            assertEquals(Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3)), kind.apply(Arrays.asList(1, 2, 3)).split(even).toList());
            assertEquals(3, calls.get());
        }
    }

    @Test
    public void testD104_splitByPredicateRetriesAfterAFailingPredicate() {
        final AtomicInteger calls = new AtomicInteger();
        final Predicate<Integer> failsOnceOn2 = x -> {
            if (x == 2 && calls.incrementAndGet() == 1) {
                throw new IllegalStateException("boom");
            }

            return x >= 2;
        };

        final Iterator<List<Integer>> it = Stream.of(1, 2, 3).split(failsOnceOn2).iterator();
        assertThrows(IllegalStateException.class, it::next);
        assertEquals(Arrays.asList(2, 3), it.next());
        assertFalse(it.hasNext());
    }

    // ---------------------------------------------------------------------------------------------------------
    // D3-05 / D5-06 transform: a transfer that closes and returns its own argument is rejected like any other
    // closed result.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testD305_transformReturningClosedSelfThrows() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = Stream.of(1, 2).onClose(closed::incrementAndGet);
        assertThrows(IllegalStateException.class, () -> s.transform(x -> {
            x.close();
            return x;
        })); // c1final: returned the closed stream
        assertEquals(1, closed.get());

        final Stream<Integer> s2 = Stream.of(Arrays.asList(1, 2).iterator());
        assertThrows(IllegalStateException.class, () -> s2.transform(x -> {
            x.count();
            return x;
        }));

        final IntStream is = IntStream.of(1, 2);
        assertThrows(IllegalStateException.class, () -> is.transform(x -> {
            x.close();
            return x;
        }));
    }

    @Test
    public void testD305_transformReturningOpenSelfUnchanged() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> s = Stream.of(1, 2).onClose(closed::incrementAndGet);
        final Stream<Integer> same = s.transform(x -> x);
        assertSame(s, same);
        assertEquals(Arrays.asList(1, 2), same.toList());
        assertEquals(1, closed.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // D5-05 onEachSave(.., File): a failed open is retried (and fails again) instead of a later bare NPE.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testD505_onEachSaveFailedOpenIsSticky() throws Exception {
        final File dir = Files.createTempDirectory("fx2impl_onEachSave").toFile();

        try {
            final Stream<Integer> s = Stream.of(1, 2).onEachSave(x -> String.valueOf(x), dir);
            final Iterator<Integer> it = s.iterator();
            final RuntimeException first = assertThrows(RuntimeException.class, it::hasNext);
            final RuntimeException second = assertThrows(RuntimeException.class, it::hasNext); // c1final: true
            assertEquals(first.getClass(), second.getClass());
            assertThrows(first.getClass(), it::next); // c1final: NPE "this.bw" is null
            s.close();

            final Stream<Integer> s2 = Stream.of(1, 2).onEachSave((x, w) -> w.write(String.valueOf(x)), dir);
            final Iterator<Integer> it2 = s2.iterator();
            final RuntimeException first2 = assertThrows(RuntimeException.class, it2::hasNext);
            assertThrows(first2.getClass(), it2::hasNext);
            s2.close();
        } finally {
            dir.delete();
        }
    }

    @Test
    public void testD505_onEachSaveStillWritesFile() throws Exception {
        final File f = File.createTempFile("fx2impl_onEachSave", ".txt");

        try {
            assertEquals(Arrays.asList(1, 2), Stream.of(1, 2).onEachSave(x -> "v" + x, f).toList());
            assertEquals(Arrays.asList("v1", "v2"), Files.readAllLines(f.toPath()));
        } finally {
            f.delete();
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // D5-07 mapPartial*: the null-Optional message names the mapper's optional type.
    // ---------------------------------------------------------------------------------------------------------

    private static String npeMessage(final org.junit.jupiter.api.function.Executable op) {
        return assertThrows(NullPointerException.class, op).getMessage();
    }

    @Test
    public void testD507_mapPartialNullOptionalMessageNamesTheType() {
        assertEquals("mapper returned a null Optional; return Optional.empty() for no result", npeMessage(() -> Stream.of(1).mapPartial(x -> null).toList()));
        assertEquals("mapper returned a null OptionalInt; return OptionalInt.empty() for no result",
                npeMessage(() -> Stream.of(1).mapPartialToInt(x -> null).toArray()));
        assertEquals("mapper returned a null OptionalLong; return OptionalLong.empty() for no result",
                npeMessage(() -> Stream.of(1).mapPartialToLong(x -> null).toArray()));
        assertEquals("mapper returned a null OptionalDouble; return OptionalDouble.empty() for no result",
                npeMessage(() -> Stream.of(1).mapPartialToDouble(x -> null).toArray()));
        assertEquals("mapper returned a null java.util.Optional; return java.util.Optional.empty() for no result",
                npeMessage(() -> Stream.of(1).mapPartialJdk(x -> null).toList()));
        assertEquals("mapper returned a null java.util.OptionalInt; return java.util.OptionalInt.empty() for no result",
                npeMessage(() -> Stream.of(1).mapPartialToIntJdk(x -> null).toArray()));
        assertEquals("mapper returned a null java.util.OptionalLong; return java.util.OptionalLong.empty() for no result",
                npeMessage(() -> Stream.of(Arrays.asList(1).iterator()).mapPartialToLongJdk(x -> null).toArray()));
        assertEquals("mapper returned a null java.util.OptionalDouble; return java.util.OptionalDouble.empty() for no result",
                npeMessage(() -> Stream.of(1).parallel(2).mapPartialToDoubleJdk(x -> null).toArray()));
    }

    // ---------------------------------------------------------------------------------------------------------
    // R3-04 throwIfEmpty(Supplier): a supplier returning null -> NPE "exceptionSupplier returned null".
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testR304_throwIfEmptyNullExceptionIsNPE() {
        final Supplier<RuntimeException> nullSupplier = () -> null;
        NullPointerException e = assertThrows(NullPointerException.class, () -> Stream.<Integer> empty().throwIfEmpty(nullSupplier).toList());
        assertEquals("exceptionSupplier returned null", e.getMessage());
        e = assertThrows(NullPointerException.class, () -> Stream.of(Collections.<Integer> emptyIterator()).throwIfEmpty(nullSupplier).count());
        assertEquals("exceptionSupplier returned null", e.getMessage());
        e = assertThrows(NullPointerException.class, () -> IntStream.empty().throwIfEmpty(nullSupplier).count());
        assertEquals("exceptionSupplier returned null", e.getMessage());

        // Not called for a non-empty stream; a real exception is thrown as is.
        assertEquals(Arrays.asList(1), Stream.of(1).throwIfEmpty(nullSupplier).toList());
        assertThrows(UnsupportedOperationException.class, () -> Stream.empty().throwIfEmpty(UnsupportedOperationException::new).count());
    }

    // ---------------------------------------------------------------------------------------------------------
    // R9-04 (doc lock) primitive symmetricDifference emits a null collection element as the primitive default.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testR904_primitiveSymmetricDifferenceNullElementIsEmittedAsDefault() {
        assertEquals(Arrays.asList(0, 0), IntStream.of(0).symmetricDifference(Arrays.asList((Integer) null)).boxed().toList());
        assertEquals(Arrays.asList(1L, 0L, 2L), LongStream.of(1).symmetricDifference(Arrays.asList(null, 2L)).boxed().toList());
        assertEquals(Arrays.asList(1.0, 0.0, 2.0), DoubleStream.of(1).symmetricDifference(Arrays.asList(null, 2.0)).boxed().toList());
        assertEquals(Arrays.asList('a', '\0', 'b'), CharStream.of('a').symmetricDifference(Arrays.asList(null, 'b')).boxed().toList());
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-101 collect(Supplier, ..) / toArray(IntFunction): a null container -> NPE, empty and non-empty, closed.
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testC101_collectWithNullContainerIsNPE() {
        for (final Function<List<Integer>, Stream<Integer>> kind : sourceKinds()) {
            for (final List<Integer> data : Arrays.asList(Collections.<Integer> emptyList(), Arrays.asList(1), range(0, 100))) {
                final AtomicInteger closed = new AtomicInteger();
                NullPointerException e = assertThrows(NullPointerException.class,
                        () -> kind.apply(data).onClose(closed::incrementAndGet).collect(() -> (List<Integer>) null, List::add));
                assertEquals("supplier returned null", e.getMessage());
                assertEquals(1, closed.get());

                closed.set(0);
                e = assertThrows(NullPointerException.class,
                        () -> kind.apply(data).onClose(closed::incrementAndGet).collect(() -> (List<Integer>) null, List::add, List::addAll));
                assertEquals("supplier returned null", e.getMessage());
                assertEquals(1, closed.get());

                // A null-tolerant accumulator does not make a null container acceptable.
                e = assertThrows(NullPointerException.class, () -> kind.apply(data).collect(() -> (Object) null, (c, x) -> {
                }));
                assertEquals("supplier returned null", e.getMessage());
            }
        }

        assertEquals("supplier returned null", assertThrows(NullPointerException.class, () -> EntryStream.<String, Integer> empty()
                .collect(() -> (List<Map.Entry<String, Integer>>) null, List::add)).getMessage());
    }

    @Test
    public void testC101_collectWithValidContainerUnchanged() {
        for (final Function<List<Integer>, Stream<Integer>> kind : sourceKinds()) {
            final List<Integer> result = kind.apply(range(0, 100)).collect(ArrayList::new, List::add);
            Collections.sort(result);
            assertEquals(range(0, 100), result);
            assertEquals(new ArrayList<>(), kind.apply(Collections.<Integer> emptyList()).collect(ArrayList::new, List::add));
        }
    }

    @Test
    public void testC101_toArrayWithNullArrayIsNPE() {
        for (final Function<List<Integer>, Stream<Integer>> kind : sourceKinds()) {
            for (final List<Integer> data : Arrays.asList(Collections.<Integer> emptyList(), Arrays.asList(1, 2))) {
                final AtomicInteger closed = new AtomicInteger();
                final NullPointerException e = assertThrows(NullPointerException.class,
                        () -> kind.apply(data).onClose(closed::incrementAndGet).toArray(n -> (Integer[]) null));
                assertEquals("generator returned null", e.getMessage());
                assertEquals(1, closed.get());
            }

            assertEquals(Arrays.asList(1, 2), Arrays.asList(kind.apply(Arrays.asList(1, 2)).sorted().toArray(Integer[]::new)));
            // The documented leniency for a short generator array is unchanged.
            assertEquals(2, kind.apply(Arrays.asList(1, 2)).toArray(n -> new Integer[0]).length);
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // C-083 fullJoin/rightJoin(Stream, ..) and joinByRange(.., mapperForUnJoinedElements): the tail is built only
    // when traversal reaches it; close()/limit(0)/a left failure neither drains b nor calls the mapper.
    // ---------------------------------------------------------------------------------------------------------

    private static final class Right {
        final AtomicInteger pulled = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();

        Stream<Integer> stream(final Integer... values) {
            return Stream.of(values).map(x -> {
                pulled.incrementAndGet();
                return x;
            }).onClose(closed::incrementAndGet);
        }

        Stream<Integer> failingAt(final int bad, final Integer... values) {
            return Stream.of(values).map(x -> {
                pulled.incrementAndGet();

                if (x == bad) {
                    throw new IllegalArgumentException("elemB");
                }

                return x;
            }).onClose(closed::incrementAndGet);
        }
    }

    private interface JoinOp {
        Stream<String> apply(Stream<Integer> left, Stream<Integer> b);
    }

    private static List<JoinOp> fullAndRightJoins() {
        return Arrays.asList((left, b) -> left.fullJoin(b, x -> x, y -> y, (l, r) -> l + ":" + r),
                (left, b) -> left.rightJoin(b, x -> x, y -> y, (l, r) -> l + ":" + r));
    }

    @Test
    public void testC083_fullAndRightJoinCloseDoesNotConsumeB() {
        for (final JoinOp join : fullAndRightJoins()) {
            Right r = new Right();
            join.apply(Stream.of(1, 2, 9), r.stream(1, 2, 3, 4)).close();
            assertEquals(0, r.pulled.get()); // c1final: 4
            assertEquals(1, r.closed.get());

            r = new Right();
            assertEquals(Collections.emptyList(), join.apply(Stream.of(1, 2, 9), r.stream(1, 2, 3, 4)).limit(0).toList());
            assertEquals(0, r.pulled.get());
            assertEquals(1, r.closed.get());

            r = new Right();
            final Stream<Integer> failingLeft = Stream.of(1, 2, 9).map(x -> {
                throw new IllegalStateException("leftFail");
            });
            final Right rr = r;
            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> join.apply(failingLeft, rr.stream(1, 2)).toList());
            assertEquals("leftFail", e.getMessage());
            assertEquals(0, r.pulled.get());
            assertEquals(1, r.closed.get());
        }
    }

    @Test
    public void testC083_fullAndRightJoinSurfaceTheRealFailureOfB() {
        for (final JoinOp join : fullAndRightJoins()) {
            for (final Stream<Integer> left : Arrays.asList(Stream.of(1, 2, 9), Stream.<Integer> empty())) {
                final Right r = new Right();
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> join.apply(left, r.failingAt(3, 1, 2, 3, 4)).toList());
                assertEquals("elemB", e.getMessage()); // c1final: ISE "This stream is already terminated."
                assertEquals(1, r.closed.get());
            }

            final Right r = new Right();
            final Stream<Integer> b = r.stream(1, 2).onClose(() -> {
                throw new IllegalStateException("closeB");
            });
            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> join.apply(Stream.of(1), b).toList());
            assertEquals("closeB", e.getMessage());
        }
    }

    @Test
    public void testC083_fullAndRightJoinTraversalUnchanged() {
        Right r = new Right();
        assertEquals(Arrays.asList("1:1", "2:2", "9:null", "null:3", "null:4"),
                Stream.of(1, 2, 9).fullJoin(r.stream(1, 2, 3, 4), x -> x, y -> y, (l, rt) -> l + ":" + rt).toList());
        assertEquals(1, r.closed.get());

        r = new Right();
        assertEquals(Arrays.asList("1:1", "2:2", "null:3", "null:4"),
                Stream.of(1, 2, 9).rightJoin(r.stream(1, 2, 3, 4), x -> x, y -> y, (l, rt) -> l + ":" + rt).toList());
        assertEquals(1, r.closed.get());

        // Empty left: b is still read on traversal and every right element is unjoined.
        r = new Right();
        assertEquals(Arrays.asList("null:1", "null:2"), Stream.<Integer> empty().fullJoin(r.stream(1, 2), x -> x, y -> y, (l, rt) -> l + ":" + rt).toList());
        assertEquals(2, r.pulled.get());
        assertEquals(1, r.closed.get());

        // Parallel left.
        r = new Right();
        final List<String> par = Stream.of(1, 2, 9, 5)
                .parallel(3)
                .fullJoin(r.stream(1, 2, 3, 4), x -> x, y -> y, (l, rt) -> l + ":" + rt)
                .sorted()
                .toList();
        assertEquals(Arrays.asList("1:1", "2:2", "5:null", "9:null", "null:3", "null:4"), par);
        assertEquals(1, r.closed.get());

        // Partial traversal reads b (materialized on the first left element) but still closes it once.
        r = new Right();
        assertEquals(Arrays.asList("1:1"), Stream.of(1, 2, 9).fullJoin(r.stream(1, 2, 3, 4), x -> x, y -> y, (l, rt) -> l + ":" + rt).limit(1).toList());
        assertEquals(1, r.closed.get());
    }

    private static class RangeJoin {
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger tailClosed = new AtomicInteger();

        Function<Iterator<Integer>, Stream<String>> mapper() {
            return iter -> {
                mapperCalls.incrementAndGet();
                return Stream.of(iter).map(x -> "u" + x).onClose(tailClosed::incrementAndGet);
            };
        }
    }

    private interface RangeJoinOp {
        Stream<String> apply(Stream<Integer> left, Right r, RangeJoin j);
    }

    private static List<RangeJoinOp> rangeJoins() {
        return Arrays.asList(
                (left, r, j) -> left.joinByRange(r.stream(1, 2, 3, 6, 7), (l, x) -> x <= l, java.util.stream.Collectors.toList(), (l, xs) -> l + "=" + xs,
                        j.mapper()),
                (left, r, j) -> left.joinByRange(r.stream(1, 2, 3, 6, 7).iterator(), (l, x) -> x <= l, java.util.stream.Collectors.toList(),
                        (l, xs) -> l + "=" + xs, j.mapper()));
    }

    @Test
    public void testC083_joinByRangeMapperNotCalledOnEarlyClose() {
        for (final RangeJoinOp op : rangeJoins()) {
            Right r = new Right();
            RangeJoin j = new RangeJoin();
            op.apply(Stream.of(1, 5), r, j).close();
            assertEquals(0, j.mapperCalls.get()); // c1final: 1
            assertEquals(0, r.pulled.get());

            r = new Right();
            j = new RangeJoin();
            assertEquals(Collections.emptyList(), op.apply(Stream.of(1, 5), r, j).limit(0).toList());
            assertEquals(0, j.mapperCalls.get());

            r = new Right();
            j = new RangeJoin();
            assertEquals(Arrays.asList("1=[1]"), op.apply(Stream.of(1, 5), r, j).limit(1).toList());
            assertEquals(0, j.mapperCalls.get()); // c1final: 1

            // A failing left surfaces as itself; the (throwing) mapper is never called.
            final Right r2 = new Right();
            final Stream<Integer> failingLeft = Stream.of(1, 5).map(x -> {
                throw new IllegalStateException("leftFail");
            });
            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> op.apply(failingLeft, r2, new RangeJoin() {
                @Override
                Function<Iterator<Integer>, Stream<String>> mapper() {
                    return iter -> {
                        throw new IllegalArgumentException("mapper should not run");
                    };
                }
            }).toList());
            assertEquals("leftFail", e.getMessage());
        }
    }

    @Test
    public void testC083_joinByRangeTraversalUnchanged() {
        for (final RangeJoinOp op : rangeJoins()) {
            Right r = new Right();
            RangeJoin j = new RangeJoin();
            assertEquals(Arrays.asList("1=[1]", "5=[2, 3]", "u6", "u7"), op.apply(Stream.of(1, 5), r, j).toList());
            assertEquals(1, j.mapperCalls.get());
            assertEquals(1, j.tailClosed.get());

            // Empty left: every right element goes to the mapper.
            r = new Right();
            j = new RangeJoin();
            assertEquals(Arrays.asList("u1", "u2", "u3", "u6", "u7"), op.apply(Stream.<Integer> empty(), r, j).toList());
            assertEquals(1, j.mapperCalls.get());
            assertEquals(1, j.tailClosed.get());

            // Everything joined: the mapper sees no remainder.
            r = new Right();
            j = new RangeJoin();
            assertEquals(Arrays.asList("10=[1, 2, 3, 6, 7]"), op.apply(Stream.of(10), r, j).toList());

            // Partial traversal into the tail still closes the mapper's stream.
            r = new Right();
            j = new RangeJoin();
            assertEquals(Arrays.asList("1=[1]", "5=[2, 3]", "u6"), op.apply(Stream.of(1, 5), r, j).limit(3).toList());
            assertEquals(1, j.mapperCalls.get());
            assertEquals(1, j.tailClosed.get());

            // Parallel left.
            r = new Right();
            j = new RangeJoin();
            assertEquals(Arrays.asList("1=[1]", "5=[2, 3]", "u6", "u7"), op.apply(Stream.of(1, 5).parallel(2), r, j).sorted().toList());
        }

        // The Stream-b overload closes b exactly once, traversed or not.
        final Right r = new Right();
        Stream.of(1, 5).joinByRange(r.stream(1, 2), (l, x) -> x <= l, java.util.stream.Collectors.toList(), (l, xs) -> l + "=" + xs, iter -> Stream.<String> empty())
                .close();
        assertEquals(1, r.closed.get());
        final Right r2 = new Right();
        Stream.of(1, 5)
                .joinByRange(r2.stream(1, 2), (l, x) -> x <= l, java.util.stream.Collectors.toList(), (l, xs) -> l + "=" + xs, iter -> Stream.<String> empty())
                .toList();
        assertEquals(1, r2.closed.get());
    }

    // ---------------------------------------------------------------------------------------------------------
    // Doc-only nits locked by behaviour: R8 N6 (appendIfEmpty/defaultIfEmpty null supplier result = empty) and
    // MoreCollectors multi-column averages share one count (R7-04 / D3-06).
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void testDocLocks() {
        assertEquals(0, Stream.<Integer> empty().defaultIfEmpty(() -> null).count());
        assertEquals(0, IntStream.empty().appendIfEmpty(() -> null).count());
        assertEquals(Arrays.asList(1), Stream.of(1).appendIfEmpty(() -> null).toList());

        final com.landawn.abacus.util.Tuple.Tuple2<Double, Double> avg = Stream.of(1, 2, 3)
                .collect(Collectors.MoreCollectors.averagingInt(x -> x, x -> x * 10));
        assertEquals(2.0, avg._1);
        assertEquals(20.0, avg._2);

        final Map<String, Integer> m = new HashMap<>();
        m.put("a", 1);
        assertEquals(m, Stream.of("a").collect(Collectors.toConcurrentMap(x -> x, x -> 1)));
    }
}
