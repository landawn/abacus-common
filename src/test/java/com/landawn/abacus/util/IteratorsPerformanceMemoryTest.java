package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Iterators. */
// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
public class IteratorsPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-19 G019 begin ----
    @Nested
    class R1019G019 {

        /**
         * Runs full GCs (bounded) until the referent is collected; returns whether it was.
         */
        private boolean collected(final java.lang.ref.WeakReference<?> ref) {
            for (int i = 0; i < 40 && ref.get() != null; i++) {
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return ref.get() == null;
        }

        /**
         * Iterator that logs every hasNext()/next() call and can throw on a chosen call.
         */
        private final class Rec implements Iterator<Integer> {
            private final int id;
            private final Integer[] values;
            private final StringBuilder log;
            private final int throwOnHasNextCall;
            private final int throwOnNextCall;
            private int cursor = 0;
            private int hasNextCalls = 0;
            private int nextCalls = 0;

            Rec(final int id, final StringBuilder log, final int throwOnHasNextCall, final int throwOnNextCall, final Integer... values) {
                this.id = id;
                this.values = values;
                this.log = log;
                this.throwOnHasNextCall = throwOnHasNextCall;
                this.throwOnNextCall = throwOnNextCall;
            }

            @Override
            public boolean hasNext() {
                log.append('h').append(id);

                if (++hasNextCalls == throwOnHasNextCall) {
                    throw new IllegalStateException("hasNext" + id);
                }

                return cursor < values.length;
            }

            @Override
            public Integer next() {
                log.append('n').append(id);

                if (++nextCalls == throwOnNextCall) {
                    throw new IllegalStateException("next" + id);
                }

                if (cursor >= values.length) {
                    throw new NoSuchElementException("rec" + id);
                }

                return values[cursor++];
            }
        }

        private Iterator<Integer>[] recScenario(final StringBuilder log) {
            @SuppressWarnings("unchecked")
            final Iterator<Integer>[] a = new Iterator[] { new Rec(0, log, 0, 0), null, new Rec(1, log, 3, 0, 1, 2), new Rec(2, log, 0, 0),
                    new Rec(3, log, 0, 1, 3, null, 4), null };
            return a;
        }

        private List<Iterable<Integer>> iterableScenario(final StringBuilder log) {
            final List<Iterable<Integer>> list = new ArrayList<>();
            list.add(() -> {
                log.append("i0");
                return new Rec(0, log, 0, 0, 1);
            });
            list.add(null);
            list.add(() -> {
                log.append("i1");
                throw new IllegalStateException("iterator1");
            });
            list.add(Collections.emptyList());
            list.add(() -> {
                log.append("i2");
                return new Rec(2, log, 2, 0, 2, 3);
            });
            return list;
        }

        private String drive(final Iterator<?> it) {
            final StringBuilder sb = new StringBuilder();

            for (int guard = 0; guard < 100; guard++) {
                try {
                    if (!it.hasNext()) {
                        sb.append("END;");
                        break;
                    }

                    final Object value = it.next();
                    sb.append("v=").append(value).append(';');
                } catch (final IllegalStateException e) {
                    sb.append("ex=").append(e.getMessage()).append(';');
                }
            }

            try {
                it.next();
                sb.append("noNSEE");
            } catch (final NoSuchElementException e) {
                sb.append("NSEE");
            }

            return sb.toString();
        }

        // G019-01: concat(Iterator...) must make exactly the same calls on its sources as concat(Collection), incl. null/empty/throwing sources.
        @Test
        public void testConcatIterators_sameCallsAsCollectionForm() {
            final StringBuilder logVarargs = new StringBuilder();
            final String outVarargs = drive(Iterators.concat(recScenario(logVarargs)));
            final StringBuilder logCollection = new StringBuilder();
            final String outCollection = drive(Iterators.concat(Arrays.asList(recScenario(logCollection))));

            assertEquals(outCollection + " | " + logCollection, outVarargs + " | " + logVarargs);
            assertEquals("ex=hasNext1;v=1;v=2;ex=next3;v=3;v=null;v=4;END;NSEE", outVarargs);
        }

        // G019-01: concat(Iterable...) must call iterator()/hasNext()/next() exactly as concatIterables(Collection) does.
        @Test
        public void testConcatIterables_sameCallsAsCollectionForm() {
            final StringBuilder logVarargs = new StringBuilder();
            @SuppressWarnings("unchecked")
            final Iterable<Integer>[] array = iterableScenario(logVarargs).toArray(new Iterable[0]);
            final String outVarargs = drive(Iterators.concat(array));
            final StringBuilder logCollection = new StringBuilder();
            final String outCollection = drive(Iterators.concatIterables(iterableScenario(logCollection)));

            assertEquals(outCollection + " | " + logCollection, outVarargs + " | " + logVarargs);
            assertEquals("v=1;ex=iterator1;ex=hasNext2;v=2;v=3;END;NSEE", outVarargs);
        }

        // G019-01: element order, skipped null/empty arrays, and the caller's varargs array left untouched.
        @Test
        public void testConcatObjectArrays_orderAndCallerArrayUntouched() {
            final String[] first = { "a", null, "b" };
            final String[] last = { "c" };
            final String[][] outer = { first, null, new String[0], last };
            final List<String> result = new ArrayList<>();
            Iterators.concat(outer).forEachRemaining(result::add);

            assertEquals(Arrays.asList("a", null, "b", "c"), result);
            assertSame(first, outer[0]);
            assertNull(outer[1]);
            assertSame(last, outer[3]);
            assertFalse(Iterators.concat(new String[0], null).hasNext());
            assertFalse(Iterators.concat((String[][]) null).hasNext());

            final Iterator<Integer>[] iterators = recScenario(new StringBuilder());
            final Iterator<Integer>[] copy = iterators.clone();
            drive(Iterators.concat(iterators));
            assertArrayEquals(copy, iterators);
        }

        // G019-01: concat(Map...) skips null maps, keeps the lazy entrySet().iterator() call (no CME for a map changed before its turn).
        @Test
        public void testConcatMaps_orderNullsAndLazyEntryIteration() {
            final Map<String, Integer> m1 = new LinkedHashMap<>();
            m1.put("a", 1);
            final Map<String, Integer> m2 = new LinkedHashMap<>();
            m2.put("b", 2);
            final ObjIterator<Map.Entry<String, Integer>> it = Iterators.concat(m1, null, new HashMap<>(), m2);
            assertEquals("a", it.next().getKey());
            m2.put("c", 3);
            final List<String> keys = new ArrayList<>();
            it.forEachRemaining(e -> keys.add(e.getKey()));
            assertEquals(Arrays.asList("b", "c"), keys);
            @SuppressWarnings("unchecked")
            final Map<String, Integer>[] onlyNulls = new Map[] { null, null };
            assertFalse(Iterators.concat(onlyNulls).hasNext());
        }

        // G019-01: primitive concat overloads keep order, skip null/empty sources, and make the same source calls as before.
        @Test
        public void testConcatPrimitives_orderNullsAndSourceCalls() {
            final int[] x = { 1, 2 };
            final int[][] outer = { null, x, {}, { 3 } };
            final IntIterator it = Iterators.concat(outer);
            final IntList list = new IntList();

            while (it.hasNext()) {
                list.add(it.nextInt());
            }

            assertEquals(IntList.of(1, 2, 3), list);
            assertSame(x, outer[1]);
            assertThrows(NoSuchElementException.class, it::nextInt);
            assertFalse(Iterators.concat(new double[0], null).hasNext());

            final StringBuilder log = new StringBuilder();
            final IntIterator s0 = IntIterator.of(new int[0]);
            final IntIterator s1 = new IntIterator() {
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    log.append("h");
                    return cursor < 2;
                }

                @Override
                public int nextInt() {
                    log.append("n");
                    return 10 + cursor++;
                }
            };
            final IntIterator[] sources = { s0, null, s1 };
            final IntIterator concat = Iterators.concat(sources);
            final IntList got = new IntList();

            while (concat.hasNext()) {
                got.add(concat.nextInt());
            }

            assertEquals(IntList.of(10, 11), got);
            assertEquals("hhhnhhhnhh", log.toString());
            assertSame(s1, sources[2]);
        }

        // G019-01: BiIterator/TriIterator concat keep order through next(), forEachRemaining and map(), and leave the caller's array alone.
        @Test
        public void testConcatBiTri_orderForEachAndMap() {
            final Map<String, Integer> m1 = new LinkedHashMap<>();
            m1.put("a", 1);
            m1.put("b", 2);
            final Map<String, Integer> m2 = new LinkedHashMap<>();
            m2.put("c", 3);

            @SuppressWarnings("unchecked")
            final BiIterator<String, Integer>[] bis = new BiIterator[] { BiIterator.of(m1), null, BiIterator.of(new HashMap<String, Integer>()),
                    BiIterator.of(m2) };
            final BiIterator<String, Integer> first = bis[0];
            final List<String> viaForEach = new ArrayList<>();
            Iterators.concat(bis).forEachRemaining((k, v) -> viaForEach.add(k + v));
            assertEquals(Arrays.asList("a1", "b2", "c3"), viaForEach);
            assertSame(first, bis[0]);

            final List<String> viaMap = new ArrayList<>();
            Iterators.concat(BiIterator.of(m1), BiIterator.of(m2)).map((k, v) -> k + v).forEachRemaining(viaMap::add);
            assertEquals(Arrays.asList("a1", "b2", "c3"), viaMap);

            final BiIterator<String, Integer> pairs = Iterators.concat(BiIterator.of(m1), BiIterator.of(m2));
            assertEquals(Pair.of("a", 1), pairs.next());
            final List<String> rest = new ArrayList<>();
            pairs.forEachRemaining((k, v) -> rest.add(k + v));
            assertEquals(Arrays.asList("b2", "c3"), rest);
            assertThrows(NoSuchElementException.class, pairs::next);

            final List<String> tri = new ArrayList<>();
            Iterators
                    .concat(TriIterator.zip(new String[] { "a" }, new Integer[] { 1 }, new Boolean[] { true }), null,
                            TriIterator.zip(new String[] { "b", "c" }, new Integer[] { 2, 3 }, new Boolean[] { false, true }))
                    .map((x, y, z) -> x + y + z)
                    .forEachRemaining(tri::add);
            assertEquals(Arrays.asList("a1true", "b2false", "c3true"), tri);
        }

        private int[] newIntArray() {
            return new int[] { 7, 8, 9 };
        }

        // G019-01: an exhausted int[] source must not stay reachable from concat while a later source runs (RED on base).
        @Test
        public void testConcatIntArrays_releasesExhaustedSource() {
            int[] big = newIntArray();
            final java.lang.ref.WeakReference<int[]> ref = new java.lang.ref.WeakReference<>(big);
            final IntIterator it = Iterators.concat(big, new int[] { 1, 2 });
            big = null;

            assertEquals(7, it.nextInt());
            assertEquals(8, it.nextInt());
            assertEquals(9, it.nextInt());
            assertEquals(1, it.nextInt());
            assertTrue(collected(ref), "exhausted int[] source is still retained");
            assertEquals(2, it.nextInt());
            assertFalse(it.hasNext());
        }

        // G019-01: an exhausted IntIterator source must not stay reachable from concat while a later source runs (RED on base).
        @Test
        public void testConcatIntIterators_releasesExhaustedSource() {
            int[] big = newIntArray();
            final java.lang.ref.WeakReference<int[]> ref = new java.lang.ref.WeakReference<>(big);
            final IntIterator it = Iterators.concat(IntIterator.of(big), IntIterator.of(1, 2));
            big = null;

            assertEquals(7 + 8 + 9, it.nextInt() + it.nextInt() + it.nextInt());
            assertEquals(1, it.nextInt());
            assertTrue(collected(ref), "exhausted IntIterator source is still retained");
            assertEquals(2, it.nextInt());
            assertFalse(it.hasNext());
        }

        // G019-01: concat(T[]...) releases an exhausted array (RED on base).
        @Test
        public void testConcatObjectArrays_releasesExhaustedSource() {
            String[] big = { "a", "b" };
            final java.lang.ref.WeakReference<String[]> ref = new java.lang.ref.WeakReference<>(big);
            final ObjIterator<String> it = Iterators.concat(big, new String[] { "c", "d" });
            big = null;

            assertEquals("a", it.next());
            assertEquals("b", it.next());
            assertEquals("c", it.next());
            assertTrue(collected(ref), "exhausted array source is still retained");
            assertEquals("d", it.next());
            assertFalse(it.hasNext());
        }

        // G019-01: concat(Iterator...) releases an exhausted iterator and the data behind it (RED on base).
        @Test
        public void testConcatIterators_releasesExhaustedSource() {
            List<String> big = new ArrayList<>(Arrays.asList("a", "b"));
            final java.lang.ref.WeakReference<List<String>> ref = new java.lang.ref.WeakReference<>(big);
            final ObjIterator<String> it = Iterators.concat(big.iterator(), Arrays.asList("c", "d").iterator());
            big = null;

            assertEquals("a", it.next());
            assertEquals("b", it.next());
            assertEquals("c", it.next());
            assertTrue(collected(ref), "exhausted iterator source is still retained");
            assertEquals("d", it.next());
            assertFalse(it.hasNext());
        }

        // G019-01: concat(Iterable...) releases an exhausted Iterable (RED on base).
        @Test
        public void testConcatIterables_releasesExhaustedSource() {
            List<String> big = new ArrayList<>(Arrays.asList("a", "b"));
            final java.lang.ref.WeakReference<List<String>> ref = new java.lang.ref.WeakReference<>(big);
            final ObjIterator<String> it = Iterators.concat(big, Arrays.asList("c", "d"));
            big = null;

            assertEquals("a", it.next());
            assertEquals("b", it.next());
            assertEquals("c", it.next());
            assertTrue(collected(ref), "exhausted Iterable source is still retained");
            assertEquals("d", it.next());
            assertFalse(it.hasNext());
        }

        // G019-01: concat(Map...) releases an exhausted map (RED on base).
        @Test
        public void testConcatMaps_releasesExhaustedSource() {
            Map<String, Integer> big = new HashMap<>();
            big.put("a", 1);
            final java.lang.ref.WeakReference<Map<String, Integer>> ref = new java.lang.ref.WeakReference<>(big);
            final Map<String, Integer> last = new LinkedHashMap<>();
            last.put("b", 2);
            last.put("c", 3);
            final ObjIterator<Map.Entry<String, Integer>> it = Iterators.concat(big, last);
            big = null;

            assertEquals("a", it.next().getKey());
            assertEquals("b", it.next().getKey());
            assertTrue(collected(ref), "exhausted map source is still retained");
            assertEquals("c", it.next().getKey());
            assertFalse(it.hasNext());
        }

        // G019-01: concat(BiIterator...) and its map() view release an exhausted source (RED on base).
        @Test
        public void testConcatBiIterators_releasesExhaustedSource() {
            Map<String, Integer> big = new HashMap<>();
            big.put("a", 1);
            final java.lang.ref.WeakReference<Map<String, Integer>> ref = new java.lang.ref.WeakReference<>(big);
            final Map<String, Integer> last = new LinkedHashMap<>();
            last.put("b", 2);
            last.put("c", 3);
            final BiIterator<String, Integer> it = Iterators.concat(BiIterator.of(big), BiIterator.of(last));
            big = null;

            assertEquals("a", it.next().left());
            assertEquals("b", it.next().left());
            assertTrue(collected(ref), "exhausted BiIterator source is still retained");
            assertEquals("c", it.next().left());
            assertFalse(it.hasNext());

            Map<String, Integer> big2 = new HashMap<>();
            big2.put("x", 9);
            final java.lang.ref.WeakReference<Map<String, Integer>> ref2 = new java.lang.ref.WeakReference<>(big2);
            final ObjIterator<String> mapped = Iterators.concat(BiIterator.of(big2), BiIterator.of(last)).map((k, v) -> k + v);
            big2 = null;

            assertEquals("x9", mapped.next());
            assertEquals("b2", mapped.next());
            assertTrue(collected(ref2), "exhausted BiIterator source is still retained by map()");
            assertEquals("c3", mapped.next());
            assertFalse(mapped.hasNext());
        }

        // G019-01: concat(TriIterator...) releases an exhausted source (RED on base).
        @Test
        public void testConcatTriIterators_releasesExhaustedSource() {
            String[] big = { "a" };
            final java.lang.ref.WeakReference<String[]> ref = new java.lang.ref.WeakReference<>(big);
            final TriIterator<String, Integer, Boolean> it = Iterators.concat(TriIterator.zip(big, new Integer[] { 1 }, new Boolean[] { true }),
                    TriIterator.zip(new String[] { "b", "c" }, new Integer[] { 2, 3 }, new Boolean[] { false, true }));
            big = null;

            assertEquals("a", it.next().left());
            assertEquals("b", it.next().left());
            assertTrue(collected(ref), "exhausted TriIterator source is still retained");
            assertEquals("c", it.next().left());
            assertFalse(it.hasNext());
        }
    }

    // ---- perf review 2026-10-19 G019 end ----
    // ---- perf review 2026-10-19 G023 begin ----
    @Nested
    class R1019G023 {

        private boolean collected(final java.lang.ref.WeakReference<?> ref) {
            for (int i = 0; i < 40 && ref.get() != null; i++) {
                System.gc();

                try {
                    Thread.sleep(10);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            return ref.get() == null;
        }

        private void assertExhausted(final Iterator<?> it) {
            assertFalse(it.hasNext());
            assertFalse(it.hasNext());
            assertThrows(NoSuchElementException.class, it::next);
            assertFalse(it.hasNext());
        }

        // G023-01: once hasNext() has returned false, concat over primitive arrays no longer references the last array -
        // also for the single-source call (RED before the change).
        @Test
        public void testConcatPrimitiveArrays_releasesLastArrayAfterExhaustion() {
            final List<java.lang.ref.WeakReference<Object>> refs = new ArrayList<>();
            final List<Iterator<?>> iterators = new ArrayList<>();
            Object[] arrays = { new boolean[] { true, false }, new char[] { 'a', 'b' }, new byte[] { 1, 2 }, new short[] { 3, 4 }, new int[] { 5, 6 },
                    new long[] { 7, 8 }, new float[] { 9f, 10f }, new double[] { 11d, 12d } };

            for (final Object array : arrays) {
                refs.add(new java.lang.ref.WeakReference<>(array));
            }

            iterators.add(Iterators.concat((boolean[]) arrays[0]));
            iterators.add(Iterators.concat(new char[0], (char[]) arrays[1]));
            iterators.add(Iterators.concat((byte[]) arrays[2], null));
            iterators.add(Iterators.concat(new short[] { 0 }, (short[]) arrays[3]));
            iterators.add(Iterators.concat((int[]) arrays[4]));
            iterators.add(Iterators.concat((long[]) arrays[5]));
            iterators.add(Iterators.concat((float[]) arrays[6]));
            iterators.add(Iterators.concat((double[]) arrays[7]));
            arrays = null;

            final List<String> values = new ArrayList<>();

            for (final Iterator<?> it : iterators) {
                while (it.hasNext()) {
                    values.add(String.valueOf(it.next()));
                }

                assertExhausted(it);
            }

            assertEquals(Arrays.asList("true", "false", "a", "b", "1", "2", "0", "3", "4", "5", "6", "7", "8", "9.0", "10.0", "11.0", "12.0"), values);

            for (int i = 0; i < refs.size(); i++) {
                assertTrue(collected(refs.get(i)), "last array of source " + i + " is still retained");
            }

            assertFalse(iterators.isEmpty());
        }

        // G023-01: concat(T[]...) releases its last array once exhausted (RED before the change).
        @Test
        public void testConcatObjectArrays_releasesLastArrayAfterExhaustion() {
            String[] single = { "a", "b" };
            String[] last = { "d" };
            final java.lang.ref.WeakReference<String[]> singleRef = new java.lang.ref.WeakReference<>(single);
            final java.lang.ref.WeakReference<String[]> lastRef = new java.lang.ref.WeakReference<>(last);
            final ObjIterator<String> it1 = Iterators.concat(single);
            final ObjIterator<String> it2 = Iterators.concat(new String[] { "c" }, null, new String[0], last);
            single = null;
            last = null;

            assertEquals("a", it1.next());
            assertEquals("b", it1.next());
            assertExhausted(it1);
            assertEquals("c", it2.next());
            assertEquals("d", it2.next());
            assertExhausted(it2);

            assertTrue(collected(singleRef), "single array source is still retained");
            assertTrue(collected(lastRef), "last array source is still retained");
            assertNotNull(it1);
            assertNotNull(it2);
        }

        // G023-01: concat(T[]...) results are unchanged: order, null elements, skipped null/empty arrays, lazily read
        // arrays (a write before the element is reached is visible), the shared empty iterator, and next() past the end.
        @Test
        public void testConcatObjectArrays_resultsUnchanged() {
            final String[] first = { "a", null };
            final String[] second = { "b", "c" };
            final ObjIterator<String> it = Iterators.concat(first, null, new String[0], second);
            final List<String> result = new ArrayList<>();

            result.add(it.next());
            second[1] = "C";

            while (it.hasNext()) {
                result.add(it.next());
            }

            assertEquals(Arrays.asList("a", null, "b", "C"), result);
            assertExhausted(it);
            assertSame(ObjIterator.empty(), Iterators.concat(new String[0], null));
            assertSame(ObjIterator.empty(), Iterators.concat((String[][]) null));
            assertSame(ObjIterator.empty(), Iterators.concat(new String[0][]));

            final ObjIterator<Integer> forEach = Iterators.concat(new Integer[] { 1 }, new Integer[] { 2, 3 });
            final List<Integer> collected = new ArrayList<>();
            forEach.forEachRemaining(collected::add);
            assertEquals(Arrays.asList(1, 2, 3), collected);
            assertExhausted(forEach);

            assertEquals(Arrays.asList(4, 5), Iterators.concat(new Integer[] { 1, 2 }, new Integer[] { 3, 4, 5 }).skip(3).toList());
        }

        // G023-01: the iterator-backed overloads still poll their last source on every hasNext() after exhaustion,
        // exactly as before (which is why they keep referencing it).
        @Test
        public void testConcatIterators_lastSourceStillPolledAfterExhaustion() {
            final AtomicInteger hasNextCalls = new AtomicInteger();
            final Iterator<Integer> last = new Iterator<>() {
                private boolean done;

                @Override
                public boolean hasNext() {
                    hasNextCalls.incrementAndGet();
                    return !done;
                }

                @Override
                public Integer next() {
                    done = true;
                    return 1;
                }
            };

            final ObjIterator<Integer> it = Iterators.concat(Arrays.asList(0).iterator(), last);

            assertEquals(0, it.next().intValue());
            assertEquals(1, it.next().intValue());
            final int before = hasNextCalls.get();
            assertFalse(it.hasNext());
            assertFalse(it.hasNext());
            assertEquals(before + 4, hasNextCalls.get());
        }
    }
    // ---- perf review 2026-10-19 G023 end ----
}
