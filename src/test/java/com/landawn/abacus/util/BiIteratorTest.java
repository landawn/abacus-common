package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.IntObjConsumer;
import com.landawn.abacus.util.stream.EntryStream;

public class BiIteratorTest extends TestBase {

    private static Map<String, Integer> abc() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        return map;
    }

    @Test
    public void testEmpty() {
        BiIterator<String, Integer> iter = BiIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertThrows(NoSuchElementException.class, () -> iter.next((k, v) -> {
        }));
        assertThrows(UnsupportedOperationException.class, iter::remove);
        assertEquals(0, iter.count());
        assertTrue(iter.toList().isEmpty());
        assertTrue(iter.toSet().isEmpty());
        assertTrue(iter.toCollection(ArrayList::new).isEmpty());
        assertTrue(iter.toImmutableList().isEmpty());
        assertTrue(iter.toImmutableSet().isEmpty());
        assertEquals(0, iter.toArray().length);

        AtomicInteger count = new AtomicInteger();
        iter.forEachRemaining((a, b) -> count.incrementAndGet());
        iter.foreachRemaining((a, b) -> count.incrementAndGet());
        assertEquals(0, count.get());
        assertDoesNotThrow(() -> iter.foreachRemaining((Throwables.BiConsumer<String, Integer, Exception>) (a, b) -> {
            throw new Exception("unused");
        }));
        assertFalse(iter.map((a, b) -> a + b).hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.map((a, b) -> a + b).next());
        assertEquals(0, iter.stream().count());

        assertThrows(IllegalArgumentException.class,
                () -> iter.forEachRemaining((com.landawn.abacus.util.function.BiConsumer<? super String, ? super Integer>) null));
        assertThrows(IllegalArgumentException.class,
                () -> iter.foreachRemaining((Throwables.BiConsumer<? super String, ? super Integer, RuntimeException>) null));
        // The deprecated Consumer<Pair> overload overrides java.util.Iterator.forEachRemaining, whose contract
        // specifies NullPointerException - unlike the two overloads above, which are BiIterator's own API.
        assertThrows(NullPointerException.class, () -> iter.forEachRemaining((Consumer<Pair<String, Integer>>) null));

        assertFalse(BiIterator.<String, Integer> empty().skip(10).limit(5).filter((a, b) -> true).hasNext());
    }

    @Test
    public void testOfMap() {
        BiIterator<String, Integer> iter = BiIterator.of(abc());
        assertEquals(Pair.of("a", 1), iter.next());

        List<String> keys = new ArrayList<>();
        iter.forEachRemaining((k, v) -> keys.add(k));
        assertEquals(List.of("b", "c"), keys);

        assertFalse(BiIterator.of((Map<String, Integer>) null).hasNext());
        assertFalse(BiIterator.of(new HashMap<String, Integer>()).hasNext());
        assertEquals(3, BiIterator.of(abc()).count());

        ImmutableList<Pair<String, Integer>> list = BiIterator.of(abc()).toImmutableList();
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3)), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(Pair.of("d", 4)));

        ImmutableSet<Pair<String, Integer>> set = BiIterator.of(abc()).toImmutableSet();
        assertEquals(3, set.size());
        assertTrue(set.contains(Pair.of("a", 1)));
        assertThrows(UnsupportedOperationException.class, () -> set.add(Pair.of("d", 4)));

        assertEquals(Set.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3)), BiIterator.of(abc()).toSet());
        LinkedList<Pair<String, Integer>> collection = BiIterator.of(abc()).toCollection(LinkedList::new);
        assertEquals(Pair.of("a", 1), collection.getFirst());
        assertEquals(Pair.of("c", 3), collection.getLast());
        assertThrows(UnsupportedOperationException.class, () -> BiIterator.of(abc()).remove());
    }

    @Test
    public void testOfMapEntryIterator() {
        BiIterator<String, Integer> iter = BiIterator.of(abc().entrySet().iterator());
        assertEquals(Pair.of("a", 1), iter.next());
        assertEquals("b2", iter.map((k, v) -> k + v).next());
        assertEquals(Pair.of("c", 3), iter.next());
        assertFalse(iter.hasNext());

        assertFalse(BiIterator.of((Iterator<Map.Entry<String, Integer>>) null).hasNext());
        assertFalse(BiIterator.of(new HashMap<String, Integer>().entrySet().iterator()).hasNext());
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        BiIterator<Integer, String> infinite = BiIterator.generate(pair -> pair.set(n.getAndIncrement(), "v" + n.get()));
        assertTrue(infinite.hasNext());
        assertEquals(Pair.of(0, "v1"), infinite.next());
        assertEquals(Pair.of(1, "v2"), infinite.next());
        assertTrue(infinite.hasNext());

        MutableInt counter = MutableInt.of(0);
        BooleanSupplier hasNext = () -> counter.value() < 3;
        Consumer<Pair<Integer, String>> output = pair -> {
            int val = counter.getAndIncrement();
            pair.set(val, String.valueOf(val));
        };
        BiIterator<Integer, String> finite = BiIterator.generate(hasNext, output);
        assertEquals(List.of(Pair.of(0, "0"), Pair.of(1, "1"), Pair.of(2, "2")), finite.toList());
        assertFalse(finite.hasNext());
        assertThrows(NoSuchElementException.class, finite::next);

        MutableInt c2 = MutableInt.of(0);
        List<Integer> keys = new ArrayList<>();
        BiIterator.<Integer, String> generate(() -> c2.value() < 3, pair -> {
            int val = c2.getAndIncrement();
            pair.set(val, String.valueOf(val));
        }).forEachRemaining((k, v) -> keys.add(k));
        assertEquals(List.of(0, 1, 2), keys);

        MutableInt c3 = MutableInt.of(0);
        ObjIterator<String> mapped = BiIterator.generate(() -> c3.value() < 2, pair -> {
            int val = c3.getAndIncrement();
            pair.set(val, String.valueOf(val));
        }).map((i, s) -> s + "-" + i);
        assertEquals("0-0", mapped.next());
        assertEquals("1-1", mapped.next());
        assertFalse(mapped.hasNext());

        IntObjConsumer<Pair<String, Integer>> indexed = (index, pair) -> pair.set("key" + index, index * 10);
        BiIterator<String, Integer> fromIndex = BiIterator.generate(0, 3, indexed);
        assertEquals(List.of(Pair.of("key0", 0), Pair.of("key1", 10), Pair.of("key2", 20)), fromIndex.toList());

        List<String> itemKeys = new ArrayList<>();
        BiIterator.<String, Integer> generate(0, 3, (index, pair) -> pair.set("item" + index, index)).forEachRemaining((k, v) -> itemKeys.add(k));
        assertEquals(List.of("item0", "item1", "item2"), itemKeys);
        assertEquals(List.of("key0=0", "key1=1"),
                BiIterator.<String, Integer> generate(0, 2, (index, pair) -> pair.set("key" + index, index)).map((k, v) -> k + "=" + v).toList());

        BiIterator<Integer, Integer> counted = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        counted.next();
        counted.next();
        assertEquals(3, counted.count());
        assertFalse(counted.hasNext());
    }

    @Test
    public void testGenerate_EdgeCase() {
        assertFalse(BiIterator.generate(5, 5, (i, pair) -> pair.set(i, i)).hasNext());
        assertEquals(4,
                BiIterator.generate(Integer.MAX_VALUE - 5, Integer.MAX_VALUE - 1, (index, pair) -> pair.set((long) index, (long) index)).toList().size());
        assertThrows(IndexOutOfBoundsException.class, () -> BiIterator.generate(5, 2, (i, pair) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(0, 5, null));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(null, pair -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.generate(() -> true, null));
        assertThrows(RuntimeException.class, () -> BiIterator.generate(() -> true, pair -> {
            throw new RuntimeException("boom");
        }).next());

        BiIterator<Integer, String> lastWriteWins = BiIterator.<Integer, String> generate(() -> true, pair -> {
            pair.set(1, "first");
            pair.set(2, "second");
        }).limit(1);
        assertEquals(Pair.of(2, "second"), lastWriteWins.next());

        AtomicInteger probes = new AtomicInteger();
        BiIterator<Integer, Integer> latched = BiIterator.generate(() -> probes.getAndIncrement() < 1, pair -> pair.set(7, 7));
        assertTrue(latched.hasNext());
        latched.next();
        assertFalse(latched.hasNext());
        probes.set(0);
        assertFalse(latched.hasNext());

        AtomicInteger n = new AtomicInteger();
        BiIterator<Integer, String> cleared = BiIterator.generate(() -> n.get() < 2, pair -> {
            int i = n.getAndIncrement();
            pair.setLeft(i);
            if (i == 0) {
                pair.setRight("first");
            }
        });
        assertEquals(Pair.of(0, "first"), cleared.next());
        assertEquals(Pair.of(1, null), cleared.next());

        BiIterator<Integer, String> indexedCleared = BiIterator.generate(0, 2, (i, pair) -> {
            pair.setLeft(i);
            if (i == 0) {
                pair.setRight("first");
            }
        });
        assertEquals(Pair.of(0, "first"), indexedCleared.next());
        assertEquals(Pair.of(1, null), indexedCleared.next());
    }

    @Test
    public void testZipArrays() {
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3)),
                BiIterator.zip(new String[] { "a", "b", "c" }, new Integer[] { 1, 2, 3 }).toList());
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2)), BiIterator.zip(new String[] { "a", "b", "c", "d" }, new Integer[] { 1, 2 }).toList());
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("default", 3), Pair.of("default", 4)),
                BiIterator.zip(new String[] { "a", "b" }, new Integer[] { 1, 2, 3, 4 }, "default", -1).toList());
        assertEquals(List.of(Pair.of("a", 99), Pair.of("b", 99), Pair.of("c", 99)),
                BiIterator.zip(new String[] { "a", "b", "c" }, new Integer[0], "default", 99).toList());
        assertEquals(List.of(Pair.of("a", 1), Pair.of(null, 2), Pair.of("c", null)),
                BiIterator.zip(new String[] { "a", null, "c" }, new Integer[] { 1, 2, null }).toList());
        assertFalse(BiIterator.zip((String[]) null, (Integer[]) null).hasNext());
        assertFalse(BiIterator.zip(new String[0], new Integer[0]).hasNext());
        assertFalse(BiIterator.zip((String[]) null, new Integer[0]).hasNext());
        assertFalse(BiIterator.zip(new String[0], (Integer[]) null).hasNext());
        assertEquals(Set.of(Pair.of("a", 1)), BiIterator.zip(new String[] { "a", "a" }, new Integer[] { 1, 1 }).toSet());
    }

    @Test
    public void testZipIterables() {
        assertEquals(Pair.of("x", 10), BiIterator.zip(List.of("x", "y", "z"), new LinkedHashSet<>(List.of(10, 20, 30))).next());
        List<String> keys = new ArrayList<>();
        BiIterator.zip(List.of("a"), List.of(1, 2, 3), "missing", 0).forEachRemaining((k, v) -> keys.add(k));
        assertEquals(List.of("a", "missing", "missing"), keys);
        assertFalse(BiIterator.zip((Iterable<String>) null, (Iterable<Integer>) null).hasNext());
        assertFalse(BiIterator.zip((Iterable<String>) null, List.of(1, 2, 3)).hasNext());
        assertFalse(BiIterator.zip(List.of("a", "b"), (Iterable<Integer>) null).hasNext());
    }

    @Test
    public void testZipIterators() {
        ObjIterator<String> mapped = BiIterator.zip(List.of("one", "two", "three").iterator(), List.of(1, 2, 3, 4).iterator()).map((s, i) -> s + ":" + i);
        assertEquals(List.of("one:1", "two:2", "three:3"), mapped.toList());
        assertEquals(2, BiIterator.zip(List.of("Alice", "Bob", "Charlie").iterator(), List.of(25, 30).iterator()).toList().size());

        BiIterator<String, Integer> withDefaults = BiIterator.zip(List.of("a", "b", "c").iterator(), List.of(1).iterator(), null, 99);
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 99), Pair.of("c", 99)), withDefaults.toList());
        assertFalse(BiIterator.zip((Iterator<String>) null, List.of(1, 2).iterator()).hasNext());
        assertFalse(BiIterator.zip(List.of("a").iterator(), (Iterator<Integer>) null).hasNext());
        assertFalse(BiIterator.zip((Iterator<String>) null, (Iterator<Integer>) null, "Default", 0).hasNext());

        BiIterator<String, Integer> exhausted = BiIterator.zip(new String[] { "a" }, new Integer[] { 1 });
        assertEquals(Pair.of("a", 1), exhausted.next());
        assertFalse(exhausted.hasNext());
        assertTrue(exhausted.toList().isEmpty());
        assertThrows(NoSuchElementException.class, exhausted::next);
    }

    @Test
    public void testZipConsumesLeftBeforeRightSourceFailure() {
        for (final boolean withDefaults : new boolean[] { false, true }) {
            Iterator<Integer> left = List.of(1, 2, 3).iterator();
            IllegalStateException failure = new IllegalStateException("right source failed");
            Iterator<String> right = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public String next() {
                    throw failure;
                }
            };
            BiIterator<Integer, String> zipped = withDefaults ? BiIterator.zip(left, right, 0, "missing") : BiIterator.zip(left, right);

            assertSame(failure, assertThrows(IllegalStateException.class, zipped::next));
            assertEquals(2, left.next());
        }
    }

    @Test
    public void testUnzip() {
        List<String> values = List.of("1:one", "2:two", "3:three");
        BiIterator<Integer, String> iter = BiIterator.unzip(values, (str, pair) -> {
            String[] parts = str.split(":");
            pair.set(Integer.parseInt(parts[0]), parts[1]);
        });
        assertEquals(List.of(Pair.of(1, "one"), Pair.of(2, "two"), Pair.of(3, "three")), iter.toList());

        BiConsumer<String, Pair<String, Integer>> splitEq = (str, pair) -> {
            String[] parts = str.split("=");
            pair.set(parts[0], Integer.parseInt(parts[1]));
        };
        List<String> keys = new ArrayList<>();
        BiIterator.unzip(List.of("a=1", "b=2").iterator(), splitEq).forEachRemaining((k, v) -> keys.add(k));
        assertEquals(List.of("a", "b"), keys);

        Pair<LinkedHashSet<String>, ArrayList<Integer>> fromIterable = BiIterator.unzip(List.of("a=1", "b=2", "a=3"), splitEq, LinkedHashSet::new,
                ArrayList::new);
        assertEquals(new LinkedHashSet<>(List.of("a", "b")), fromIterable.left());
        assertEquals(List.of(1, 2, 3), fromIterable.right());

        Pair<LinkedHashSet<String>, ArrayList<Integer>> fromIterator = BiIterator.unzip(List.of("a=1", "b=2", "a=3").iterator(), splitEq, LinkedHashSet::new,
                ArrayList::new);
        assertEquals(List.of(1, 2, 3), fromIterator.right());

        assertFalse(BiIterator.unzip((Iterable<String>) null, (s, pair) -> {
        }).hasNext());
        assertFalse(BiIterator.unzip((Iterator<String>) null, (s, pair) -> {
        }).hasNext());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.unzip(List.of("a"), null));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.unzip((Iterable<String>) null, null));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.unzip(List.of("a").iterator(), null));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.unzip((Iterator<String>) null, null));
    }

    @Test
    public void testForEachRemaining() throws Exception {
        List<Pair<String, Integer>> pairs = new ArrayList<>();
        BiIterator.of(abc()).forEachRemaining((Consumer<Pair<String, Integer>>) pairs::add);
        assertEquals(3, pairs.size());

        Map<String, Integer> remaining = new HashMap<>();
        BiIterator<String, Integer> iter = BiIterator.zip(new String[] { "x", "y", "z" }, new Integer[] { 10, 20, 30 });
        iter.next();
        iter.forEachRemaining(remaining::put);
        assertEquals(Map.of("y", 20, "z", 30), remaining);

        List<String> keys = new ArrayList<>();
        Exception thrown = assertThrows(Exception.class, () -> BiIterator.of(abc()).foreachRemaining((k, v) -> {
            keys.add(k);
            if ("b".equals(k)) {
                throw new Exception("stop");
            }
        }));
        assertEquals("stop", thrown.getMessage());
        assertEquals(List.of("a", "b"), keys);

        Exception nextThrown = assertThrows(Exception.class, () -> BiIterator.of(Map.of("error", 1)).next((k, v) -> {
            throw new Exception("next");
        }));
        assertEquals("next", nextThrown.getMessage());
    }

    @Test
    public void testSkip() {
        assertEquals(List.of(Pair.of("c", 3)), BiIterator.of(abc()).skip(2).toList());
        assertFalse(BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i)).skip(10).hasNext());
        BiIterator<Integer, Integer> original = BiIterator.generate(0, 5, (i, pair) -> pair.set(i, i));
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> BiIterator.of(abc()).skip(-1));
        assertEquals(List.of("b", "c"), BiIterator.of(abc()).skip(1).map((k, v) -> k).toList());
        AtomicInteger generated = new AtomicInteger();
        ObjIterator<Integer> mapped = BiIterator.<Integer, Integer> generate(0, 3, (index, output) -> {
            generated.incrementAndGet();
            output.set(index, index);
        }).skip(1).map((a, b) -> a);
        assertEquals(0, generated.get());
        assertTrue(mapped.hasNext());
        assertEquals(1, generated.get());
        assertEquals(List.of(1, 2), mapped.toList());
        assertEquals(3, generated.get());

        List<String> keys = new ArrayList<>();
        BiIterator.of(abc()).skip(1).forEachRemaining((k, v) -> keys.add(k));
        assertEquals(List.of("b", "c"), keys);
        assertFalse(BiIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }).skip(Long.MAX_VALUE).hasNext());
    }

    @Test
    public void testLimit() {
        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2)), BiIterator.of(abc()).limit(2).toList());
        assertEquals(3, BiIterator.generate(0, 3, (i, pair) -> pair.set(i, i)).limit(10).toList().size());
        assertFalse(BiIterator.of(abc()).limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.of(abc()).limit(-1));
        assertEquals(List.of("a", "b"), BiIterator.of(abc()).limit(2).map((k, v) -> k).toList());

        List<String> keys = new ArrayList<>();
        BiIterator.of(abc()).limit(2).forEachRemaining((k, v) -> keys.add(k));
        assertEquals(List.of("a", "b"), keys);

        Map<Integer, String> numbered = new LinkedHashMap<>();
        for (int i = 0; i < 20; i++) {
            numbered.put(i, "value" + i);
        }
        List<Pair<Integer, String>> skipped = BiIterator.of(numbered).limit(10).skip(5).toList();
        assertEquals(5, skipped.size());
        assertEquals(5, skipped.get(0).left());
        assertEquals(2, BiIterator.zip(new Integer[] { 1, 2 }, new Integer[] { 3, 4 }).limit(Long.MAX_VALUE).toList().size());
    }

    @Test
    public void testFilter() {
        assertEquals(List.of(Pair.of("b", 2)), BiIterator.of(abc()).filter((k, v) -> v % 2 == 0).toList());
        assertEquals(3, BiIterator.of(abc()).filter((a, b) -> true).toList().size());
        assertFalse(BiIterator.of(abc()).filter((a, b) -> false).hasNext());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.of(abc()).filter(null));

        List<String> keys = new ArrayList<>();
        BiIterator.of(abc()).filter((k, v) -> v > 1).forEachRemaining((k, v) -> keys.add(k));
        assertEquals(List.of("b", "c"), keys);
        assertEquals(List.of("B2", "C3"), BiIterator.of(abc()).filter((k, v) -> k.compareTo("b") >= 0).map((k, v) -> k.toUpperCase() + v).toList());
        assertTrue(BiIterator.of(abc()).filter((k, v) -> v % 2 == 0).filter((k, v) -> v % 3 == 0).toList().isEmpty());
    }

    @Test
    public void testMap() {
        assertEquals(List.of("a1", "b2", "c3"), BiIterator.of(abc()).map((k, v) -> k + v).toList());
        assertEquals(List.of(0, 3, 6, 9, 12), BiIterator.<Integer, Integer> generate(0, 5, (i, pair) -> pair.set(i, i * 2)).map((a, b) -> a + b).toList());
        assertEquals(List.of(2, 4), BiIterator.of(abc()).filter((k, v) -> k.length() == 1 && v <= 2).map((k, v) -> v * 2).toList());
        ObjIterator<String> withNull = BiIterator.zip(new String[] { "a", "b", "c" }, new Integer[] { 1, 2, 3 }).map((s, i) -> i == 2 ? null : s + i);
        assertEquals("a1", withNull.next());
        assertNull(withNull.next());
        assertEquals("c3", withNull.next());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.of(abc()).map(null));
        ObjIterator<Integer> exploding = BiIterator.zip(new String[] { "a", "b" }, new Integer[] { 1, 0 }).map((s, i) -> 10 / i);
        assertEquals(10, exploding.next());
        assertThrows(ArithmeticException.class, exploding::next);
    }

    @Test
    public void testStream() {
        assertEquals(2,
                BiIterator.zip(new String[] { "apple", "banana", "cherry" }, new Integer[] { 5, 6, 6 }).stream().filter(e -> e.getValue() == 6).count());
        assertEquals(List.of("a=1", "b=2", "c=3"), BiIterator.of(abc()).stream((k, v) -> k + "=" + v).toList());
        EntryStream<String, Integer> stream = BiIterator.of(abc()).stream();
        assertNotNull(stream);
        assertEquals(3, stream.toList().size());
        assertEquals(0, BiIterator.<String, Integer> empty().stream().count());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().stream(null));
    }

    @Test
    public void testToArrayAndToList() {
        Pair<String, Integer>[] array = BiIterator.of(abc()).toArray();
        assertEquals(3, array.length);
        assertEquals(Pair.of("a", 1), array[0]);
        assertEquals(0, BiIterator.empty().toArray().length);

        @SuppressWarnings({ "deprecation", "unchecked" })
        Pair<String, Integer>[] typed = BiIterator.of(abc()).toArray(new Pair[0]);
        assertEquals(3, typed.length);
        assertEquals(Pair.of("a", 1), typed[0]);

        assertEquals(List.of(Pair.of("a", 1), Pair.of("b", 2), Pair.of("c", 3)), BiIterator.of(abc()).toList());
        assertTrue(BiIterator.empty().toList().isEmpty());
    }

    @Test
    public void testUnzipToLists() {
        Pair<List<String>, List<Integer>> lists = BiIterator.of(abc()).unzipToLists(ArrayList::new);
        assertEquals(List.of("a", "b", "c"), lists.left());
        assertEquals(List.of(1, 2, 3), lists.right());

        Pair<List<String>, List<Integer>> linked = BiIterator.zip(new String[] { "a", "b" }, new Integer[] { 1, 2 }).unzipToLists(LinkedList::new);
        assertTrue(linked.left() instanceof LinkedList);
        assertEquals(2, linked.right().size());

        Pair<List<String>, List<Integer>> empty = BiIterator.<String, Integer> empty().unzipToLists(LinkedList::new);
        assertTrue(empty.left().isEmpty());
        assertTrue(empty.right().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().unzipToLists(() -> null));

        BiIterator<Integer, String> source = BiIterator.zip(new Integer[] { 1 }, new String[] { "a" });
        assertThrows(IllegalArgumentException.class, () -> source.unzipToCollections(ArrayList::new, () -> null));
        assertTrue(source.hasNext());
    }

    @Test
    public void testUnzipToSets() {
        Pair<Set<String>, Set<Integer>> sets = BiIterator.of(abc()).unzipToSets(HashSet::new);
        assertEquals(Set.of("a", "b", "c"), sets.left());
        assertEquals(Set.of(1, 2, 3), sets.right());

        Pair<Set<String>, Set<Integer>> ordered = BiIterator.of(abc()).unzipToSets(LinkedHashSet::new);
        assertEquals("a", ordered.left().iterator().next());
        assertEquals(1, ordered.right().iterator().next());

        Pair<Set<String>, Set<Integer>> unique = BiIterator.zip(new String[] { "a", "b", "a" }, new Integer[] { 1, 2, 1 }).unzipToSets(TreeSet::new);
        assertTrue(unique.left() instanceof TreeSet);
        assertEquals(Set.of("a", "b"), unique.left());
        assertEquals(Set.of(1, 2), unique.right());

        Pair<Set<String>, Set<Integer>> empty = BiIterator.<String, Integer> empty().unzipToSets(HashSet::new);
        assertTrue(empty.left().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> BiIterator.empty().unzipToSets(() -> null));
    }

    @Test
    public void testChaining() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        assertEquals(List.of(Pair.of("b", 2), Pair.of("d", 4)), BiIterator.of(map).skip(1).limit(3).filter((k, v) -> v % 2 == 0).toList());

        List<String> deep = BiIterator.<Integer, Integer> generate(0, 1000, (i, pair) -> pair.set(i, i * 2))
                .skip(100)
                .limit(800)
                .filter((a, b) -> a % 2 == 0)
                .filter((a, b) -> b % 4 == 0)
                .skip(50)
                .limit(100)
                .map((a, b) -> a + ":" + b)
                .toList();
        assertTrue(deep.size() <= 100);
        assertTrue(deep.stream().allMatch(s -> {
            String[] parts = s.split(":");
            return Integer.parseInt(parts[0]) % 2 == 0 && Integer.parseInt(parts[1]) % 4 == 0;
        }));
        assertEquals(100, BiIterator.<Integer, String> generate(0, 10000, (i, pair) -> pair.set(i, "v" + i)).filter((k, v) -> k % 100 == 0).count());
    }
}
