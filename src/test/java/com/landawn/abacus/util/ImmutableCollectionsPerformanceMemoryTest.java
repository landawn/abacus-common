package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for ImmutableCollections. */
@Tag("slow-test")
public class ImmutableCollectionsPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G034 end ----
    // ---- perf review 2026-10-08 G035 begin ----
    @Nested
    class G035 {

        private List<Integer> scrambledIds(final int count, final long seed) {
            final List<Integer> ids = new ArrayList<>(count);

            for (int i = 0; i < count; i++) {
                ids.add(i);
            }

            Collections.shuffle(ids, new Random(seed));

            return ids;
        }

        /**
         * A reference BiMap whose backing maps are far larger than either builder sizing (the current 32-bucket tables or
         * the default 16-bucket ones recommended by G035-02), so a matching result cannot depend on the table capacity.
         */
        private BiMap<String, Integer> referenceBiMap() {
            return new BiMap<>(() -> new LinkedHashMap<>(1024), () -> new LinkedHashMap<>(1024));
        }

        /**
         * @param expected the expected forward mappings, in the expected iteration order
         * @param expectedInverse the expected value-to-key mappings in the inverse view's iteration order (the reverse
         *        map's own order, which differs from the forward value order once a key's value has been replaced)
         * @param actual the map under test
         */
        private void assertSameContentAndOrder(final Map<String, Integer> expected, final Map<Integer, String> expectedInverse,
                final ImmutableBiMap<String, Integer> actual) {
            assertEquals(new ArrayList<>(expected.keySet()), new ArrayList<>(actual.keySet()));
            assertEquals(new ArrayList<>(expected.values()), new ArrayList<>(actual.values()));
            assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
            assertEquals(expected.size(), actual.size());
            assertEquals(expected.isEmpty(), actual.isEmpty());
            assertEquals(expected, actual);
            assertTrue(actual.equals(expected));
            assertEquals(expected.hashCode(), actual.hashCode());
            assertEquals(expected.toString(), actual.toString());

            for (final Map.Entry<String, Integer> entry : expected.entrySet()) {
                assertEquals(entry.getValue(), actual.get(entry.getKey()));
                assertTrue(actual.containsKey(entry.getKey()));
                assertTrue(actual.containsValue(entry.getValue()));
                assertEquals(entry.getKey(), actual.getByValue(entry.getValue()));
            }

            assertNull(actual.get("absent"));
            assertNull(actual.getByValue(-1));
            assertNull(actual.getByValue(null));

            final ImmutableBiMap<Integer, String> inverse = actual.inverse();
            assertEquals(new ArrayList<>(expectedInverse.keySet()), new ArrayList<>(inverse.keySet()));
            assertEquals(new ArrayList<>(expectedInverse.values()), new ArrayList<>(inverse.values()));
            assertEquals(new ArrayList<>(expectedInverse.entrySet()), new ArrayList<>(inverse.entrySet()));
            assertEquals(expectedInverse, inverse);
            assertSame(actual, inverse.inverse());
            assertSame(inverse, actual.inverse());
            // A consumed no-argument builder owns its storage, so copyOf hands the same instance back.
            assertSame(actual, ImmutableBiMap.copyOf(actual));
        }

        // G035-02 (OPEN): pins builder() order, lookups, views and inverse across the 12/13, 24/25 and 48/49 resize thresholds
        @Test
        void testBuilder_encounterOrderAcrossResizeThresholds() {
            for (final int count : new int[] { 0, 1, 2, 3, 6, 11, 12, 13, 16, 23, 24, 25, 48, 49, 100, 1000 }) {
                final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
                final Map<String, Integer> expected = new LinkedHashMap<>();
                final Map<Integer, String> expectedInverse = new LinkedHashMap<>();

                for (final Integer id : scrambledIds(count, 31L * count + 7)) {
                    assertSame(builder, builder.put("k" + id, id * 7 + 1));
                    expected.put("k" + id, id * 7 + 1);
                    expectedInverse.put(id * 7 + 1, "k" + id);
                }

                assertSameContentAndOrder(expected, expectedInverse, builder.build());
            }
        }

        // G035-02 (OPEN): pins builder() put/putAll semantics (replace, duplicate value, null, partial failure) vs a reference
        @Test
        void testBuilder_putSemanticsMatchLargeCapacityReference() {
            final Random random = new Random(20261008L);

            for (int round = 0; round < 300; round++) {
                final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
                final BiMap<String, Integer> reference = referenceBiMap();
                final int operations = random.nextInt(70);

                for (int i = 0; i < operations; i++) {
                    final String key = random.nextInt(12) == 0 ? null : "k" + random.nextInt(30);
                    final Integer value = random.nextInt(12) == 0 ? null : random.nextInt(40);
                    String expectedFailure = null;

                    try {
                        reference.put(key, value);
                    } catch (final IllegalArgumentException e) {
                        expectedFailure = e.getMessage();
                    }

                    if (expectedFailure == null) {
                        assertSame(builder, builder.put(key, value));
                    } else {
                        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> builder.put(key, value));
                        assertEquals(expectedFailure, e.getMessage());
                    }
                }

                final Map<String, Integer> batch = new LinkedHashMap<>();

                for (int i = 0, batchSize = random.nextInt(20); i < batchSize; i++) {
                    batch.put("b" + random.nextInt(30), random.nextInt(60));
                }

                String expectedFailure = null;

                try {
                    reference.putAll(batch);
                } catch (final IllegalArgumentException e) {
                    expectedFailure = e.getMessage();
                }

                if (expectedFailure == null) {
                    assertSame(builder, builder.putAll(batch));
                } else {
                    final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> builder.putAll(batch));
                    assertEquals(expectedFailure, e.getMessage());
                }

                final ImmutableBiMap<String, Integer> built = builder.build();
                assertSameContentAndOrder(new LinkedHashMap<>(reference), new LinkedHashMap<>(reference.inverse()), built);
                assertThrows(IllegalStateException.class, () -> builder.put("late", -2));
                assertEquals(reference.size(), built.size());
            }
        }

        // G035-01 (OPEN): pins isEmpty() of every view shape (incl. N.slice's Slice and a stale subList's CME) for the candidate
        @Test
        void testIsEmpty_viewsAgreeWithSizeAcrossBackings() {
            final List<Collection<?>> nonEmpty = new ArrayList<>();
            final List<Collection<?>> empty = new ArrayList<>();

            final ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4);
            nonEmpty.add(list);
            nonEmpty.add(list.subList(1, 2));
            nonEmpty.add(list.reversed());
            nonEmpty.add(list.reversed().subList(0, 1));
            nonEmpty.add(ImmutableList.copyOf(Arrays.asList(5, 6)));
            nonEmpty.add(ImmutableList.wrap(new LinkedList<>(Arrays.asList(1))));
            empty.add(list.subList(2, 2));
            empty.add(list.reversed().subList(3, 3));
            empty.add(ImmutableList.empty());
            empty.add(ImmutableList.copyOf(new ArrayList<Integer>()));

            final ImmutableNavigableSet<Integer> navigableSet = ImmutableNavigableSet.copyOf(new TreeSet<>(Arrays.asList(10, 20, 30, 40)));
            nonEmpty.add(navigableSet);
            nonEmpty.add(navigableSet.headSet(20, true));
            nonEmpty.add(navigableSet.tailSet(40, true));
            nonEmpty.add(navigableSet.subSet(15, true, 25, false));
            nonEmpty.add(navigableSet.descendingSet());
            empty.add(navigableSet.headSet(10, false));
            empty.add(navigableSet.tailSet(40, false));
            empty.add(navigableSet.subSet(21, true, 29, true));
            empty.add(navigableSet.descendingSet().headSet(40, false));

            final ImmutableNavigableMap<Integer, String> navigableMap = ImmutableNavigableMap.copyOf(new TreeMap<>(Map.of(1, "a", 3, "c", 5, "e")));
            nonEmpty.add(navigableMap.navigableKeySet());
            nonEmpty.add(navigableMap.headMap(3, true).navigableKeySet());
            nonEmpty.add(navigableMap.tailMap(5, true).descendingKeySet());
            empty.add(navigableMap.headMap(1, false).navigableKeySet());
            empty.add(navigableMap.subMap(2, true, 2, true).descendingKeySet());

            nonEmpty.add(ImmutableSet.of("x"));
            nonEmpty.add(ImmutableSet.wrap(new HashSet<>(Arrays.asList("y"))));
            empty.add(ImmutableSet.empty());
            empty.add(ImmutableCollection.wrap(null));

            final LinkedHashSet<String> linked = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
            nonEmpty.add(CommonUtil.slice(linked, 0, 2));
            nonEmpty.add(CommonUtil.slice(linked, 2, 3));
            empty.add(CommonUtil.slice(linked, 1, 1));

            final java.util.concurrent.ConcurrentLinkedQueue<Integer> queue = new java.util.concurrent.ConcurrentLinkedQueue<>();
            final ImmutableCollection<Integer> queueView = ImmutableCollection.wrap(queue);
            empty.add(queueView);

            for (final Collection<?> c : nonEmpty) {
                assertFalse(c.isEmpty(), () -> c.getClass() + " " + c);
                assertTrue(c.size() > 0, () -> c.getClass() + " " + c);
            }

            for (final Collection<?> c : empty) {
                assertTrue(c.isEmpty(), () -> c.getClass() + " " + c);
                assertEquals(0, c.size(), () -> c.getClass() + " " + c);
            }

            // Live views follow their backing collection.
            queue.add(7);
            assertFalse(queueView.isEmpty());
            queue.clear();
            assertTrue(queueView.isEmpty());

            final List<Integer> backing = new ArrayList<>();
            final ImmutableList<Integer> live = ImmutableList.wrap(backing);
            assertTrue(live.isEmpty());
            assertTrue(live.reversed().isEmpty());
            backing.add(1);
            assertFalse(live.isEmpty());
            assertFalse(live.reversed().isEmpty());

            // A stale sublist of a wrap()-backed ArrayList reports the structural change from isEmpty() as well.
            final List<Integer> source = new ArrayList<>(Arrays.asList(1, 2, 3));
            final ImmutableList<Integer> staleSubList = ImmutableList.wrap(source).subList(0, 2);
            source.add(4);
            assertThrows(java.util.ConcurrentModificationException.class, staleSubList::isEmpty);
        }
    }
}
