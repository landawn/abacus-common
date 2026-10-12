package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for BiMap. */
@Tag("slow-test")
public class BiMapPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G003 begin ----
    @Nested
    class G003 {
        /**
         * Builds a LinkedHashMap whose insertion order is neither sorted nor hash order, so an order mismatch shows.
         */
        private LinkedHashMap<String, Integer> scrambledLinkedHashMap(final int size, final long seed) {
            final List<Integer> ids = new ArrayList<>();

            for (int i = 0; i < size; i++) {
                ids.add(i);
            }

            Collections.shuffle(ids, new Random(seed));

            final LinkedHashMap<String, Integer> result = new LinkedHashMap<>();

            for (final Integer id : ids) {
                result.put("k" + id, id * 3 + 1);
            }

            return result;
        }

        /**
         * Asserts that {@code copy} is an independent BiMap with the same content and the same forward order as
         * {@code source}, and also the same reverse-map order when {@code checkInverseOrder} is set (a copy fills its
         * reverse map in forward order, so that holds only when the source's two maps list the pairs alike).
         */
        private <K, V> void assertFaithfulCopy(final BiMap<K, V> source, final BiMap<K, V> copy, final boolean checkInverseOrder) {
            assertNotSame(source, copy);
            assertNotSame(source.keyMap, copy.keyMap);
            assertNotSame(source.keyMap, copy.valueMap);
            assertNotSame(source.valueMap, copy.keyMap);
            assertNotSame(source.valueMap, copy.valueMap);
            assertSame(source.keyMapSupplier, copy.keyMapSupplier);
            assertSame(source.valueMapSupplier, copy.valueMapSupplier);
            assertEquals(source, copy);
            assertEquals(source.inverse(), copy.inverse());
            assertEquals(source.size(), copy.inverse().size());
            assertEquals(new ArrayList<>(source.keySet()), new ArrayList<>(copy.keySet()));
            assertEquals(new ArrayList<>(source.values()), new ArrayList<>(copy.values()));
            assertEquals(new ArrayList<>(source.entrySet()), new ArrayList<>(copy.entrySet()));

            if (checkInverseOrder) {
                assertEquals(new ArrayList<>(source.inverse().keySet()), new ArrayList<>(copy.inverse().keySet()));
                assertEquals(new ArrayList<>(source.inverse().values()), new ArrayList<>(copy.inverse().values()));
            }
        }

        // G003-01: copy() of a BiMap backed by the library's LinkedHashMap suppliers, on both sides of the 12-entry presizing threshold
        @Test
        void testCopy_linkedHashMapBackedSizesAroundPresizeThreshold() {
            for (final int size : new int[] { 0, 1, 2, 11, 12, 13, 23, 24, 25, 47, 48, 49, 96, 97, 1000, 3000 }) {
                final LinkedHashMap<String, Integer> template = scrambledLinkedHashMap(size, size);
                final BiMap<String, Integer> source = BiMap.copyOf(template);
                assertSame(Suppliers.ofLinkedHashMap(), source.keyMapSupplier);
                assertSame(Suppliers.ofLinkedHashMap(), source.valueMapSupplier);

                final BiMap<String, Integer> copy = source.copy();

                assertFaithfulCopy(source, copy, true);
                assertEquals(LinkedHashMap.class, copy.keyMap.getClass());
                assertEquals(LinkedHashMap.class, copy.valueMap.getClass());
                assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(copy.keySet()));
                assertEquals(new ArrayList<>(template.values()), new ArrayList<>(copy.values()));
                assertEquals(new ArrayList<>(template.values()), new ArrayList<>(copy.inverse().keySet()));

                // A copy of the copy, and a copy of the inverse view, keep the same orders.
                assertFaithfulCopy(copy, copy.copy(), true);
                final BiMap<Integer, String> inverseCopy = source.inverse().copy();
                assertEquals(new ArrayList<>(template.values()), new ArrayList<>(inverseCopy.keySet()));
                assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(inverseCopy.values()));
                assertEquals(LinkedHashMap.class, inverseCopy.keyMap.getClass());
                assertEquals(LinkedHashMap.class, inverseCopy.valueMap.getClass());

                // The copy is independent and keeps behaving like an insertion-ordered, bijective BiMap.
                copy.put("new", -1);
                assertFalse(source.containsKey("new"));
                assertEquals("new", new ArrayList<>(copy.keySet()).get(copy.size() - 1));
                assertThrows(IllegalArgumentException.class, () -> copy.put("other", -1));

                if (size > 0) {
                    final String firstKey = template.keySet().iterator().next();
                    copy.put(firstKey, -2);
                    assertEquals(firstKey, copy.keySet().iterator().next());
                    assertEquals(firstKey, copy.getByValue(-2));
                    assertEquals(template.get(firstKey), source.get(firstKey));

                    source.remove(firstKey);
                    assertTrue(copy.containsKey(firstKey));
                    assertEquals(size + 1, copy.size());
                    assertEquals(size - 1, source.size());
                }
            }
        }

        // G003-01: class-based and explicitly passed library LinkedHashMap suppliers, after removals, re-insertions and forcePut
        @Test
        void testCopy_linkedHashMapSuppliersAfterRemovalsAndForcePut() {
            final List<BiMap<String, Integer>> sources = new ArrayList<>();
            sources.add(new BiMap<>(LinkedHashMap.class, LinkedHashMap.class));
            sources.add(new BiMap<>(Suppliers.ofLinkedHashMap(), Suppliers.ofLinkedHashMap()));

            for (final BiMap<String, Integer> source : sources) {
                for (int i = 0; i < 60; i++) {
                    source.put("k" + (i * 37 % 61), i);
                }

                for (int i = 0; i < 60; i += 4) {
                    source.remove("k" + (i * 37 % 61));
                }

                source.put("k" + (8 * 37 % 61), 1000);
                source.forcePut("displacer", 5);
                source.putIfAbsent("late", 2000);
                source.forcePut("k" + (5 * 37 % 61), 3000);

                final BiMap<String, Integer> copy = source.copy();
                assertFaithfulCopy(source, copy, true);
                assertFaithfulCopy(source.inverse(), source.inverse().copy(), true);
                assertFaithfulCopy(copy, copy.copy(), true);
            }
        }

        // G003-01: maps that are not the library's LinkedHashMap supplier pair still come from their suppliers, once per side
        @Test
        void testCopy_otherSuppliersStillUsed() {
            final AtomicInteger keyMapCalls = new AtomicInteger();
            final AtomicInteger valueMapCalls = new AtomicInteger();
            final BiMap<String, Integer> counted = new BiMap<>(() -> {
                keyMapCalls.incrementAndGet();
                return new LinkedHashMap<>();
            }, () -> {
                valueMapCalls.incrementAndGet();
                return new LinkedHashMap<>();
            });
            counted.putAll(scrambledLinkedHashMap(40, 7));
            keyMapCalls.set(0);
            valueMapCalls.set(0);
            final BiMap<String, Integer> countedCopy = counted.copy();
            assertEquals(1, keyMapCalls.get());
            assertEquals(1, valueMapCalls.get());
            assertFaithfulCopy(counted, countedCopy, true);

            // HashMap-backed (default): copied through its suppliers, same content and order.
            final BiMap<String, Integer> hashed = new BiMap<>();
            hashed.putAll(scrambledLinkedHashMap(40, 8));
            final BiMap<String, Integer> hashedCopy = hashed.copy();
            assertFaithfulCopy(hashed, hashedCopy, false);
            assertEquals(HashMap.class, hashedCopy.keyMap.getClass());
            assertEquals(HashMap.class, hashedCopy.valueMap.getClass());

            // Comparator-keyed forward map with a LinkedHashMap reverse map: mixed suppliers.
            final TreeMap<String, Integer> sorted = new TreeMap<>(Collections.reverseOrder());
            sorted.putAll(scrambledLinkedHashMap(40, 9));
            final BiMap<String, Integer> sortedBiMap = BiMap.copyOf(sorted);
            final BiMap<String, Integer> sortedCopy = sortedBiMap.copy();
            assertFaithfulCopy(sortedBiMap, sortedCopy, true);
            assertEquals(new ArrayList<>(sorted.keySet()), new ArrayList<>(sortedCopy.keySet()));
            assertEquals(LinkedHashMap.class, sortedCopy.valueMap.getClass());

            // A supplier that hands out one shared instance is still rejected.
            final Map<String, Integer> shared = new LinkedHashMap<>();
            final BiMap<String, Integer> aliasing = new BiMap<>(() -> shared, LinkedHashMap::new);
            assertThrows(IllegalArgumentException.class, aliasing::copy);
            aliasing.put("a", 1);
            assertThrows(IllegalArgumentException.class, aliasing::copy);
        }

        /**
         * A value whose hashCode can be made to fail after it has been stored.
         */
        private final class FlakyValue {
            private final int id;
            private boolean armed;

            FlakyValue(final int id) {
                this.id = id;
            }

            @Override
            public int hashCode() {
                if (armed) {
                    throw new IllegalStateException("hashCode failed for " + id);
                }

                return id;
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof FlakyValue && ((FlakyValue) obj).id == id;
            }
        }

        // G003-01: a failing element callback during a presized copy propagates unchanged and leaves the source intact
        @Test
        void testCopy_linkedHashMapBackedFailingValueHashCode() {
            final LinkedHashMap<String, FlakyValue> template = new LinkedHashMap<>();
            final List<FlakyValue> values = new ArrayList<>();

            for (int i = 0; i < 30; i++) {
                final FlakyValue value = new FlakyValue(i);
                values.add(value);
                template.put("k" + i, value);
            }

            final BiMap<String, FlakyValue> source = BiMap.copyOf(template);
            values.get(0).armed = true;

            final IllegalStateException e = assertThrows(IllegalStateException.class, source::copy);
            assertEquals("hashCode failed for 0", e.getMessage());

            values.get(0).armed = false;
            values.get(17).armed = true;
            final IllegalStateException e17 = assertThrows(IllegalStateException.class, source::copy);
            assertEquals("hashCode failed for 17", e17.getMessage());

            values.get(17).armed = false;
            assertEquals(30, source.size());
            assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(source.keySet()));
            final BiMap<String, FlakyValue> copy = source.copy();
            assertEquals(new ArrayList<>(template.keySet()), new ArrayList<>(copy.keySet()));
            assertEquals(new ArrayList<>(source.inverse().keySet()), new ArrayList<>(copy.inverse().keySet()));
        }
    }
}
