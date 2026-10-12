package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Maps. */
@Tag("slow-test")
public class MapsPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G042 end ----
    // ---- perf review 2026-10-08 G043 begin ----
    @Nested
    class G043 {

        /**
         * The query-first algorithm {@code Maps.invert(Map, BiFunction)} used before the review, applied to a result map
         * created exactly as {@code Maps.newOrderingMap} creates it. Records every merge call.
         */
        private <K, V> Map<V, K> referenceInvert(final Map<K, V> map, final java.util.function.BiFunction<? super K, ? super K, ? extends K> mergeFunction) {
            final Map<V, K> result = map.getClass() == HashMap.class ? CommonUtil.newHashMap(map.size()) : CommonUtil.newLinkedHashMap(map.size());

            for (final Map.Entry<K, V> entry : map.entrySet()) {
                final K oldVal = result.get(entry.getValue());

                if (oldVal != null || result.containsKey(entry.getValue())) {
                    final K merged = mergeFunction.apply(oldVal, entry.getKey());

                    if (merged == null) {
                        result.remove(entry.getValue());
                    } else {
                        result.put(entry.getValue(), merged);
                    }
                } else {
                    result.put(entry.getValue(), entry.getKey());
                }
            }

            return result;
        }

        private java.util.function.BiFunction<String, String, String> recordingMerger(final List<String> calls, final int mode) {
            return (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);

                switch (mode) {
                    case 0:
                        return oldKey;
                    case 1:
                        return newKey;
                    case 2:
                        return null;
                    default:
                        // null for every third call, a concatenation otherwise
                        return calls.size() % 3 == 0 ? null : oldKey + "+" + newKey;
                }
            };
        }

        private void assertSameInvert(final Map<String, String> input, final int mode) {
            final List<String> expectedCalls = new ArrayList<>();
            final List<String> actualCalls = new ArrayList<>();
            final Map<String, String> expected = referenceInvert(input, recordingMerger(expectedCalls, mode));
            final Map<String, String> actual = Maps.invert(input, recordingMerger(actualCalls, mode));

            assertEquals(expected.getClass(), actual.getClass(), input.getClass().getSimpleName());
            assertEquals(expected, actual);
            // Same iteration order: identical put/remove sequence on a map of the same class and capacity.
            assertEquals(new ArrayList<>(expected.entrySet()), new ArrayList<>(actual.entrySet()));
            assertEquals(expectedCalls, actualCalls);
        }

        // G043-01: invert(Map, BiFunction) - random inputs with duplicate values, null keys and null values, every merge mode
        @Test
        public void testInvertWithMerge_randomInputsMatchQueryFirstReference() {
            final Random random = new Random(20261008L);

            for (int round = 0; round < 300; round++) {
                final int size = random.nextInt(round < 50 ? 6 : 60);
                final int distinctValues = 1 + random.nextInt(Math.max(1, size));
                final Map<String, String> hashMap = new HashMap<>();
                final Map<String, String> linkedHashMap = new LinkedHashMap<>();
                final Map<String, String> treeMap = new TreeMap<>();

                for (int i = 0; i < size; i++) {
                    final String key = random.nextInt(10) == 0 ? null : "k" + random.nextInt(size * 2 + 1);
                    final String value = random.nextInt(8) == 0 ? null : "v" + random.nextInt(distinctValues);
                    hashMap.put(key, value);
                    linkedHashMap.put(key, value);

                    if (key != null) {
                        treeMap.put(key, value);
                    }
                }

                for (int mode = 0; mode < 4; mode++) {
                    assertSameInvert(hashMap, mode);
                    assertSameInvert(linkedHashMap, mode);
                    assertSameInvert(treeMap, mode);
                }
            }
        }

        // G043-01: a null merge result removes the value; a later equal value is re-inserted at the end without a merge call
        @Test
        public void testInvertWithMerge_nullMergeRemovesThenReinsertsAtEnd() {
            final Map<String, String> map = new LinkedHashMap<>();
            map.put("a", "x");
            map.put("b", "y");
            map.put("c", "x");
            map.put("d", "z");
            map.put("e", "x");
            final List<String> calls = new ArrayList<>();

            final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);
                return null;
            });

            assertTrue(inverted instanceof LinkedHashMap);
            assertEquals(Arrays.asList("y", "z", "x"), new ArrayList<>(inverted.keySet()));
            assertEquals(Arrays.asList("b", "d", "e"), new ArrayList<>(inverted.values()));
            assertEquals(Arrays.asList("a|c"), calls);
        }

        // G043-01: a value first bound to the null input key is present (merge sees null), also in a HashMap result
        @Test
        public void testInvertWithMerge_valueBoundToNullKeyIsPresent() {
            final Map<String, String> map = new HashMap<>();
            map.put(null, "v");
            map.put("k", "v");
            map.put("other", "w");
            final List<String> calls = new ArrayList<>();

            final Map<String, String> inverted = Maps.invert(map, (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);
                return "merged";
            });

            assertTrue(inverted.getClass() == HashMap.class);
            assertEquals(2, inverted.size());
            assertEquals("merged", inverted.get("v"));
            assertEquals("other", inverted.get("w"));
            assertEquals(1, calls.size());
            assertTrue(calls.get(0).equals("null|k") || calls.get(0).equals("k|null"), calls.toString());

            // a null key that collides with nothing is stored as a null value
            final Map<String, String> single = new HashMap<>();
            single.put(null, "v");
            final Map<String, String> invertedSingle = Maps.invert(single, (oldKey, newKey) -> "never");
            assertTrue(invertedSingle.containsKey("v"));
            assertNull(invertedSingle.get("v"));
        }

        // G043-01: exceptions from the merge function and from a value's hashCode still propagate unchanged
        @Test
        public void testInvertWithMerge_failuresPropagate() {
            final Map<String, String> map = new LinkedHashMap<>();
            map.put("a", "x");
            map.put("b", "x");

            final IllegalStateException mergeFailure = assertThrows(IllegalStateException.class, () -> Maps.invert(map, (oldKey, newKey) -> {
                throw new IllegalStateException("merge " + oldKey + " " + newKey);
            }));
            assertEquals("merge a b", mergeFailure.getMessage());

            final Object badValue = new Object() {
                @Override
                public int hashCode() {
                    throw new UnsupportedOperationException("no hash");
                }
            };
            final Map<String, Object> withBadValue = new LinkedHashMap<>();
            withBadValue.put("a", "fine");
            withBadValue.put("b", badValue);
            final UnsupportedOperationException hashFailure = assertThrows(UnsupportedOperationException.class,
                    () -> Maps.invert(withBadValue, (oldKey, newKey) -> oldKey));
            assertEquals("no hash", hashFailure.getMessage());

            assertThrows(IllegalArgumentException.class, () -> Maps.invert(map, null));
            assertTrue(Maps.invert((Map<String, String>) null, (oldKey, newKey) -> oldKey).isEmpty());
        }

        private final class Item {
            final String name;
            final int seq;

            Item(final String name, final int seq) {
                this.name = name;
                this.seq = seq;
            }

            @Override
            public boolean equals(final Object obj) {
                return obj instanceof Item && ((Item) obj).name.equals(name);
            }

            @Override
            public int hashCode() {
                return name.hashCode();
            }

            @Override
            public String toString() {
                return name + "#" + seq;
            }
        }

        // G043-01: a BiMap input yields a BiMap result, which keeps the query-first sequence. With seq-ordered backing maps the
        // input holds equals()-equal keys and values; binding k2 before merging would hit the value-uniqueness check (k2 equals kX).
        @Test
        public void testInvertWithMerge_biMapResultKeepsQueryFirstSequence() {
            final java.util.Comparator<Item> bySeq = java.util.Comparator.comparingInt(item -> item.seq);
            final BiMap<Item, Item> input = new BiMap<>(() -> new TreeMap<>(bySeq), () -> new TreeMap<>(bySeq));
            final Item kX = new Item("kx", 0);
            final Item k1 = new Item("k1", 1);
            final Item k2 = new Item("kx", 2);
            final Item vX = new Item("vx", 10);
            final Item v1 = new Item("v", 11);
            final Item v2 = new Item("v", 12);
            input.put(kX, vX);
            input.put(k1, v1);
            input.put(k2, v2);
            final List<String> calls = new ArrayList<>();

            final Map<Item, Item> inverted = Maps.invert(input, (oldKey, newKey) -> {
                calls.add(oldKey + "|" + newKey);
                return oldKey;
            });

            assertTrue(inverted instanceof BiMap, inverted.getClass().getName());
            assertEquals(Arrays.asList("k1#1|kx#2"), calls);
            assertEquals(2, inverted.size());
            assertSame(kX, inverted.get(vX));
            assertSame(k1, inverted.get(v1));

            // an ordinary BiMap: no collisions, merge never called
            final BiMap<String, Integer> plain = new BiMap<>();
            plain.put("a", 1);
            plain.put("b", 2);
            final Map<Integer, String> invertedPlain = Maps.invert(plain, (oldKey, newKey) -> {
                throw new AssertionError("no merge expected");
            });
            assertTrue(invertedPlain instanceof BiMap);
            assertEquals(CommonUtil.asMap(1, "a", 2, "b"), invertedPlain);
        }

        @SuppressWarnings("serial")
        static final class NamedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
            public NamedLinkedHashMap() {
            }
        }

        // G043-02: newTargetMap - exact HashMap/LinkedHashMap templates yield an empty map of that exact class; other classes are unaffected
        @Test
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void testNewTargetMap_hashMapAndLinkedHashMapTemplates() {
            final Map<String, Integer> hashMap = new HashMap<>(CommonUtil.asMap("a", 1, "b", 2, "c", 3));
            final Map<String, Integer> linkedHashMap = new LinkedHashMap<>();
            linkedHashMap.put("c", 3);
            linkedHashMap.put("a", 1);
            linkedHashMap.put("b", 2);
            final Map<String, Integer> accessOrdered = new LinkedHashMap<>(16, 0.75f, true);
            accessOrdered.putAll(linkedHashMap);

            for (final int size : new int[] { 0, 1, 2, 3, 16, 1000 }) {
                for (final Map template : Arrays.asList(hashMap, linkedHashMap, accessOrdered)) {
                    final Map created = Maps.newTargetMap(template, size);
                    assertEquals(template.getClass(), created.getClass());
                    assertTrue(created.isEmpty());
                    assertNotSame(template, created);

                    final Map created3 = Maps.newTargetMap(template, size, size * 2);
                    assertEquals(template.getClass(), created3.getClass());
                    assertTrue(created3.isEmpty());
                }
            }

            // a LinkedHashMap result built from an access-ordered template is insertion-ordered (no access order carried over)
            final Map<String, Integer> fromAccessOrdered = Maps.newTargetMap(accessOrdered, 3);
            fromAccessOrdered.put("x", 1);
            fromAccessOrdered.put("y", 2);
            fromAccessOrdered.get("x");
            assertEquals(Arrays.asList("x", "y"), new ArrayList<>(fromAccessOrdered.keySet()));

            // negative sizes keep failing with the same IllegalArgumentException message
            final String expectedMessage = assertThrows(IllegalArgumentException.class, () -> CommonUtil.newHashMap(-1)).getMessage();
            assertEquals(expectedMessage, assertThrows(IllegalArgumentException.class, () -> Maps.newTargetMap(hashMap, -1)).getMessage());
            assertEquals(expectedMessage, assertThrows(IllegalArgumentException.class, () -> Maps.newTargetMap(linkedHashMap, -1)).getMessage());

            // subclasses, sorted maps and the null template keep their own paths
            final NamedLinkedHashMap<String, Integer> named = new NamedLinkedHashMap<>();
            named.put("a", 1);
            assertEquals(NamedLinkedHashMap.class, Maps.newTargetMap(named, 1).getClass());
            assertEquals(TreeMap.class, Maps.newTargetMap(new TreeMap<>(linkedHashMap), 3).getClass());
            assertEquals(HashMap.class, Maps.newTargetMap(null, 3).getClass());

            // the public operations built on it
            final Map<String, Integer> filtered = Maps.filter(linkedHashMap, (k, v) -> v > 1);
            assertEquals(LinkedHashMap.class, filtered.getClass());
            assertEquals(Arrays.asList("c", "b"), new ArrayList<>(filtered.keySet()));
            assertEquals(HashMap.class, Maps.filterByKey(hashMap, k -> !k.equals("a")).getClass());
            assertEquals(CommonUtil.asMap("b", 2, "c", 3), Maps.filterByValue(hashMap, v -> v > 1));
            assertEquals(LinkedHashMap.class, Maps.intersection(linkedHashMap, hashMap).getClass());
            assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(Maps.intersection(linkedHashMap, hashMap).keySet()));
            assertEquals(HashMap.class, Maps.difference(hashMap, CommonUtil.asMap("a", 1)).getClass());
            assertEquals(LinkedHashMap.class, Maps.symmetricDifference(linkedHashMap, hashMap).getClass());
            assertEquals(LinkedHashMap.class, Maps.intersection(linkedHashMap, null).getClass());
        }
    }
}
