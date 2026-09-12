package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.Stream;

public class TriIteratorTest extends TestBase {

    private static TriIterator<Integer, String, Boolean> sample() {
        return TriIterator.zip(new Integer[] { 1, 2, 3 }, new String[] { "a", "b", "c" }, new Boolean[] { true, false, true });
    }

    @Test
    public void testEmpty() {
        TriIterator<String, Integer, Boolean> iter = TriIterator.empty();
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
        assertThrows(NoSuchElementException.class, () -> iter.next((a, b, c) -> {
        }));
        // empty() validates the action like the other seven next(action) implementations (see
        // IteratorRegressionTest#testProtectedNextRejectsNullAction): a null action is an
        // IllegalArgumentException, not the NoSuchElementException that exhaustion reports.
        assertThrows(IllegalArgumentException.class,
                () -> iter.next((Throwables.TriConsumer<? super String, ? super Integer, ? super Boolean, RuntimeException>) null));
        assertThrows(UnsupportedOperationException.class, iter::remove);
        assertEquals(0, iter.count());
        assertTrue(iter.toList().isEmpty());
        assertTrue(iter.toSet().isEmpty());
        assertTrue(iter.toCollection(ArrayList::new).isEmpty());
        assertTrue(iter.toImmutableList().isEmpty());
        assertTrue(iter.toImmutableSet().isEmpty());
        assertEquals(0, iter.toArray().length);

        AtomicInteger count = new AtomicInteger();
        iter.forEachRemaining((a, b, c) -> count.incrementAndGet());
        iter.foreachRemaining((a, b, c) -> count.incrementAndGet());
        assertEquals(0, count.get());
        assertFalse(iter.map((a, b, c) -> a + b + c).hasNext());
        assertEquals(0, iter.stream().count());

        assertThrows(IllegalArgumentException.class,
                () -> iter.forEachRemaining((com.landawn.abacus.util.function.TriConsumer<? super String, ? super Integer, ? super Boolean>) null));
        assertThrows(IllegalArgumentException.class,
                () -> iter.foreachRemaining((Throwables.TriConsumer<? super String, ? super Integer, ? super Boolean, RuntimeException>) null));
        assertThrows(IllegalArgumentException.class,
                () -> iter.map((com.landawn.abacus.util.function.TriFunction<? super String, ? super Integer, ? super Boolean, Object>) null));
    }

    @Test
    public void testGenerate() {
        AtomicInteger n = new AtomicInteger();
        TriIterator<Integer, String, Double> infinite = TriIterator.generate(triple -> triple.set(n.get(), "item" + n.get(), n.getAndIncrement() * 1.5));
        assertTrue(infinite.hasNext());
        assertEquals(Triple.of(0, "item0", 0.0), infinite.next());
        assertEquals(Triple.of(1, "item1", 1.5), infinite.next());
        assertEquals(3, TriIterator.generate(triple -> triple.set(1, "x", 1.0)).limit(3).toList().size());

        MutableInt counter = MutableInt.of(0);
        List<Triple<Integer, String, Boolean>> finite = TriIterator
                .<Integer, String, Boolean> generate(() -> counter.value() < 3, triple -> triple.set(counter.getAndIncrement(), "v", true))
                .toList();
        assertEquals(3, finite.size());
        assertEquals(Triple.of(0, "v", true), finite.get(0));
        assertEquals(Triple.of(2, "v", true), finite.get(2));

        List<Integer> indices = new ArrayList<>();
        TriIterator.<Integer, String, Boolean> generate(0, 3, (i, triple) -> triple.set(i, "val" + i, i % 2 == 0))
                .forEachRemaining((a, b, c) -> indices.add(a));
        assertEquals(List.of(0, 1, 2), indices);
        assertEquals(List.of("0:val0", "1:val1"),
                TriIterator.generate(0, 2, (i, triple) -> triple.set(i, "val" + i, true)).map((a, b, c) -> a + ":" + b).toList());

        TriIterator<Integer, Integer, Integer> counted = TriIterator.generate(0, 5, (i, triple) -> triple.set(i, i, i));
        counted.next();
        counted.next();
        assertEquals(3, counted.count());
        assertFalse(counted.hasNext());
    }

    @Test
    public void testGenerate_EdgeCase() {
        assertFalse(TriIterator.generate(4, 4, (i, triple) -> triple.set(i, i, i)).hasNext());
        assertThrows(IndexOutOfBoundsException.class, () -> TriIterator.generate(5, 2, (i, triple) -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate((Consumer<Triple<String, Integer, Double>>) null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(null, triple -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(() -> true, null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.generate(0, 5, null));

        TriIterator<Integer, String, Boolean> indexed = TriIterator.generate(0, 2, (index, output) -> {
            if (index == 0) {
                output.set(0, "first", true);
            } else {
                output.setLeft(1);
            }
        });
        assertEquals(Triple.of(0, "first", true), indexed.next());
        assertEquals(Triple.of(1, null, null), indexed.next());

        AtomicInteger index = new AtomicInteger();
        TriIterator<Integer, String, Boolean> conditional = TriIterator.generate(() -> index.get() < 2, output -> {
            if (index.getAndIncrement() == 0) {
                output.set(0, "first", true);
            } else {
                output.setLeft(1);
            }
        });
        assertEquals(Triple.of(0, "first", true), conditional.next());
        assertEquals(Triple.of(1, null, null), conditional.next());

        AtomicInteger probes = new AtomicInteger();
        TriIterator<Integer, Integer, Integer> latched = TriIterator.generate(() -> probes.getAndIncrement() > 0, output -> output.set(1, 1, 1));
        assertFalse(latched.hasNext());
        assertFalse(latched.hasNext());
        assertThrows(NoSuchElementException.class, latched::next);
        assertEquals(1, probes.get());

        AtomicInteger attempts = new AtomicInteger();
        TriIterator<Integer, Integer, Integer> limited = TriIterator.<Integer, Integer, Integer> generate(7, 8, (i, output) -> {
            if (attempts.getAndIncrement() == 0) {
                throw new IllegalStateException("transient");
            }
            output.set(i, i, i);
        }).limit(1);
        assertThrows(IllegalStateException.class, limited::next);
        assertEquals(Triple.of(7, 7, 7), limited.next());
        assertFalse(limited.hasNext());
    }

    @Test
    public void testZipArrays() {
        assertEquals(List.of(Triple.of("a", 1, 1.1), Triple.of("b", 2, 2.2), Triple.of("c", 3, 3.3)),
                TriIterator.zip(new String[] { "a", "b", "c" }, new Integer[] { 1, 2, 3 }, new Double[] { 1.1, 2.2, 3.3 }).toList());
        assertEquals(List.of(Triple.of(1, "a", true)),
                TriIterator.zip(new Integer[] { 1, 2 }, new String[] { "a", "b", "c" }, new Boolean[] { true }).toList());
        assertEquals(List.of(Triple.of("Alice", 25, true), Triple.of("Bob", 30, false), Triple.of("Unknown", 35, false)),
                TriIterator.zip(new String[] { "Alice", "Bob" }, new Integer[] { 25, 30, 35 }, new Boolean[] { true }, "Unknown", 0, false).toList());
        assertEquals(List.of(Triple.of(1, "a", false), Triple.of(-1, "b", false)),
                TriIterator.zip(new Integer[] { 1 }, new String[] { "a", "b" }, new Boolean[] {}, -1, "z", false).toList());
        assertEquals(List.of(Triple.of("a", 1, true), Triple.of(null, null, false)),
                TriIterator.zip(new String[] { "a", null }, new Integer[] { 1, null }, new Boolean[] { true, false }).toList());
        assertFalse(TriIterator.zip((String[]) null, (Integer[]) null, (Boolean[]) null).hasNext());
        assertFalse(TriIterator.zip(new String[0], new Integer[0], new Boolean[0]).hasNext());
    }

    @Test
    public void testZipIterables() {
        List<Triple<String, Integer, Boolean>> results = new ArrayList<>();
        TriIterator.zip(List.of("x", "y", "z"), List.of(10, 20, 30), List.of(true, false, true)).forEachRemaining((a, b, c) -> results.add(Triple.of(a, b, c)));
        assertEquals(Triple.of("y", 20, false), results.get(1));
        assertEquals(List.of(Triple.of(1, "a", 1.1)), TriIterator.zip(List.of(1, 2), List.of("a"), List.of(1.1, 2.2)).toList());
        assertEquals(List.of(Triple.of("Alice", 25, false), Triple.of("Unknown", 30, false)),
                TriIterator.zip(List.of("Alice"), List.of(25, 30), new ArrayList<Boolean>(), "Unknown", 0, false).toList());
        assertFalse(TriIterator.zip((Iterable<String>) null, List.of(1), List.of(true)).hasNext());
        assertFalse(TriIterator.zip(List.of("a"), (Iterable<Integer>) null, List.of(true)).hasNext());
        assertFalse(TriIterator.zip(List.of("a"), List.of(1), (Iterable<Boolean>) null).hasNext());
    }

    @Test
    public void testZipIterators() {
        for (boolean padded : new boolean[] { false, true }) {
            Iterator<Integer> first = List.of(1, 2).iterator();
            Iterator<Integer> second = List.of(10, 20).iterator();
            Iterator<Integer> failingThird = new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Integer next() {
                    throw new IllegalStateException("third source failed");
                }
            };
            TriIterator<Integer, Integer, Integer> zipped = padded ? TriIterator.zip(first, second, failingThird, 0, 0, 0)
                    : TriIterator.zip(first, second, failingThird);
            assertThrows(IllegalStateException.class, zipped::next);
            assertEquals(2, first.next());
            assertEquals(20, second.next());
        }

        TriIterator<String, Integer, Character> iter = TriIterator.zip(List.of("p", "q", "r").iterator(), List.of(100, 200, 300).iterator(),
                List.of('A', 'B', 'C').iterator());
        assertEquals(Triple.of("p", 100, 'A'), iter.next());
        assertEquals(List.of("q200B", "r300C"), iter.map((a, b, c) -> a + b + c).toList());

        assertEquals(List.of(Triple.of("a", 1, true), Triple.of("b", 0, false)),
                TriIterator.zip(List.of("a", "b").iterator(), List.of(1).iterator(), List.of(true).iterator(), "x", 0, false).toList());
        assertTrue(TriIterator.zip(List.of("a").iterator(), List.of(1).iterator(), List.of(true).iterator(), "x", 0, false).limit(Long.MAX_VALUE).hasNext());
        assertFalse(TriIterator.zip((Iterator<String>) null, List.of(1).iterator(), List.of(true).iterator()).hasNext());
        assertFalse(TriIterator.zip(List.of("a").iterator(), (Iterator<Integer>) null, List.of(true).iterator()).hasNext());
        assertFalse(TriIterator.zip(List.of("a").iterator(), List.of(1).iterator(), (Iterator<Boolean>) null).hasNext());

        TriIterator<Integer, String, Boolean> exhausted = sample();
        exhausted.toList();
        assertFalse(exhausted.hasNext());
        assertThrows(NoSuchElementException.class, exhausted::next);
        assertThrows(UnsupportedOperationException.class, () -> sample().remove());
    }

    @Test
    public void testUnzip() {
        List<String> values = List.of("1:one:true", "2:two:false");
        TriIterator<Integer, String, Boolean> iter = TriIterator.unzip(values, (str, triple) -> {
            String[] parts = str.split(":");
            triple.set(Integer.parseInt(parts[0]), parts[1], Boolean.parseBoolean(parts[2]));
        });
        assertEquals(List.of(Triple.of(1, "one", true), Triple.of(2, "two", false)), iter.toList());

        assertEquals(List.of(Triple.of(1, "a", true)),
                TriIterator.unzip(List.of(Triple.of(1, "a", true)), (src, dest) -> dest.set(src.left(), src.middle(), src.right())).toList());

        Triple<LinkedHashSet<Integer>, ArrayList<String>, ArrayList<Boolean>> collections = TriIterator.unzip(values, (str, triple) -> {
            String[] parts = str.split(":");
            triple.set(Integer.parseInt(parts[0]), parts[1], Boolean.parseBoolean(parts[2]));
        }, LinkedHashSet::new, ArrayList::new, ArrayList::new);
        assertEquals(Set.of(1, 2), collections.left());

        assertFalse(TriIterator.unzip((Iterable<String>) null, (s, triple) -> {
        }).hasNext());
        assertFalse(TriIterator.unzip((Iterator<String>) null, (s, triple) -> {
        }).hasNext());
        assertThrows(IllegalArgumentException.class, () -> TriIterator.unzip(List.of("a"), null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.unzip((Iterable<String>) null, null));
        assertThrows(IllegalArgumentException.class, () -> TriIterator.unzip(List.of("a").iterator(), null));

        Iterator<Integer> source = List.of(1).iterator();
        TriIterator<Integer, Integer, Integer> failing = TriIterator.unzip(source, (value, output) -> {
            throw new IllegalStateException("cannot decode");
        });
        assertThrows(IllegalStateException.class, failing::next);
        assertFalse(failing.hasNext());
        assertThrows(NoSuchElementException.class, failing::next);
    }

    @Test
    public void testForEachRemaining() throws Exception {
        List<Triple<Integer, String, Boolean>> collected = new ArrayList<>();
        sample().forEachRemaining(collected::add);
        assertEquals(3, collected.size());

        List<Integer> remaining = new ArrayList<>();
        TriIterator<Integer, String, Boolean> iter = sample();
        iter.next();
        iter.forEachRemaining((a, b, c) -> remaining.add(a));
        assertEquals(List.of(2, 3), remaining);

        List<Integer> seen = new ArrayList<>();
        Exception thrown = assertThrows(Exception.class, () -> sample().foreachRemaining((a, b, c) -> {
            seen.add(a);
            if (a == 2) {
                throw new Exception("stop");
            }
        }));
        assertEquals("stop", thrown.getMessage());
        assertEquals(List.of(1, 2), seen);
        assertThrows(NoSuchElementException.class, () -> TriIterator.empty().next((a, b, c) -> {
        }));
        // Updated from IllegalArgumentException: forEachRemaining(Consumer) overrides
        // java.util.Iterator.forEachRemaining, whose contract specifies NullPointerException for a null action.
        // The 8 primitive iterators were converted at r9506; TriIterator/BiIterator now match them.
        assertThrows(NullPointerException.class, () -> sample().forEachRemaining((Consumer<Triple<Integer, String, Boolean>>) null));
        // The class's own overload is NOT a JDK override and deliberately keeps IllegalArgumentException.
        assertThrows(IllegalArgumentException.class,
                () -> sample().forEachRemaining((com.landawn.abacus.util.function.TriConsumer<? super Integer, ? super String, ? super Boolean>) null));
    }

    @Test
    public void testSkip() {
        assertEquals(List.of(Triple.of(3, "c", true)), sample().skip(2).toList());
        assertFalse(sample().skip(10).hasNext());
        TriIterator<Integer, String, Boolean> original = sample();
        assertSame(original, original.skip(0));
        assertThrows(IllegalArgumentException.class, () -> sample().skip(-1));
        assertEquals(List.of(2, 3), sample().skip(1).map((a, b, c) -> a).toList());
        AtomicInteger generated = new AtomicInteger();
        ObjIterator<Integer> mapped = TriIterator.<Integer, Integer, Integer> generate(0, 3, (index, output) -> {
            generated.incrementAndGet();
            output.set(index, index, index);
        }).skip(1).map((a, b, c) -> a);
        assertEquals(0, generated.get());
        assertTrue(mapped.hasNext());
        assertEquals(1, generated.get());
        assertEquals(List.of(1, 2), mapped.toList());
        assertEquals(3, generated.get());
        assertFalse(sample().skip(Long.MAX_VALUE).hasNext());

        AtomicInteger failures = new AtomicInteger();
        TriIterator<Integer, Integer, Integer> skipped = TriIterator.<Integer, Integer, Integer> generate(0, 4, (index, output) -> {
            if (index == 1 && failures.getAndIncrement() == 0) {
                throw new IllegalStateException("transient");
            }
            output.set(index, index, index);
        }).skip(2);
        assertThrows(IllegalStateException.class, skipped::hasNext);
        assertTrue(skipped.hasNext());
        assertEquals(Triple.of(2, 2, 2), skipped.next());
    }

    @Test
    public void testLimit() {
        assertEquals(List.of(Triple.of(1, "a", true), Triple.of(2, "b", false)), sample().limit(2).toList());
        assertEquals(3, sample().limit(10).toList().size());
        assertFalse(sample().limit(0).hasNext());
        assertThrows(IllegalArgumentException.class, () -> sample().limit(-1));
        assertEquals(3, sample().limit(Long.MAX_VALUE).toList().size());

        ObjIterator<Integer> mapped = sample().limit(2).map((a, b, c) -> a + b.length() + (c ? 100 : 0));
        assertEquals(102, mapped.next());
        assertEquals(3, mapped.next());
        assertFalse(mapped.hasNext());
    }

    @Test
    public void testFilter() {
        assertEquals(List.of(Triple.of(1, "a", true), Triple.of(3, "c", true)), sample().filter((num, str, flag) -> flag).toList());
        assertEquals(3, sample().filter((a, b, c) -> true).toList().size());
        assertFalse(sample().filter((a, b, c) -> false).hasNext());
        assertThrows(IllegalArgumentException.class, () -> sample().filter(null));
        assertEquals(List.of("B", "C"), sample().filter((a, b, c) -> a > 1).map((a, b, c) -> b.toUpperCase()).toList());
    }

    @Test
    public void testMap() {
        assertEquals(List.of("1a", "2b", "3c"), sample().map((a, b, c) -> a + b).toList());
        assertEquals(List.of("2b"), sample().filter((a, b, c) -> !c).map((a, b, c) -> a + b).toList());
        assertThrows(IllegalArgumentException.class, () -> sample().map(null));
        assertNull(sample().map((a, b, c) -> a == 2 ? null : a).toList().get(1));
    }

    @Test
    public void testStream() {
        Stream<Triple<Integer, String, Boolean>> stream = sample().stream();
        assertNotNull(stream);
        assertEquals(3, stream.count());
        assertEquals(List.of("1=a", "2=b", "3=c"), sample().stream((a, b, c) -> a + "=" + b).toList());
        assertEquals(0, TriIterator.empty().stream().count());
    }

    @Test
    public void testToArrayAndToList() {
        Triple<Integer, String, Boolean>[] array = sample().toArray();
        assertEquals(3, array.length);
        assertEquals(Triple.of(1, "a", true), array[0]);
        assertEquals(0, TriIterator.empty().toArray().length);

        @SuppressWarnings({ "deprecation", "unchecked" })
        Triple<Integer, String, Boolean>[] typed = sample().toArray(new Triple[0]);
        assertEquals(3, typed.length);

        @SuppressWarnings({ "deprecation", "unchecked" })
        Triple<Integer, String, Boolean>[] larger = sample().toArray(new Triple[5]);
        assertEquals(5, larger.length);
        assertEquals(Triple.of(1, "a", true), larger[0]);
        assertNull(larger[3]);

        assertEquals(List.of(Triple.of(1, "a", true), Triple.of(2, "b", false), Triple.of(3, "c", true)), sample().toList());
        assertEquals(Set.of(Triple.of(1, "a", true), Triple.of(2, "b", false), Triple.of(3, "c", true)), sample().toSet());
        assertEquals(1, TriIterator.zip(new Integer[] { 1, 1 }, new String[] { "a", "a" }, new Boolean[] { true, true }).toSet().size());

        ImmutableList<Triple<Integer, String, Boolean>> immutableList = sample().toImmutableList();
        assertEquals(3, immutableList.size());
        assertThrows(UnsupportedOperationException.class, () -> immutableList.add(Triple.of(4, "d", false)));
        ImmutableSet<Triple<Integer, String, Boolean>> immutableSet = sample().toImmutableSet();
        assertTrue(immutableSet.contains(Triple.of(1, "a", true)));
        assertThrows(UnsupportedOperationException.class, () -> immutableSet.add(Triple.of(4, "d", false)));
        assertEquals(Triple.of(1, "a", true), sample().toCollection(LinkedList::new).getFirst());
    }

    @Test
    public void testUnzipToLists() {
        Triple<List<Integer>, List<String>, List<Boolean>> lists = sample().unzipToLists(ArrayList::new);
        assertEquals(List.of(1, 2, 3), lists.left());
        assertEquals(List.of("a", "b", "c"), lists.middle());
        assertEquals(List.of(true, false, true), lists.right());

        Triple<List<Integer>, List<String>, List<Boolean>> linked = sample().unzipToLists(LinkedList::new);
        assertTrue(linked.left() instanceof LinkedList);
        Triple<List<Integer>, List<String>, List<Boolean>> empty = TriIterator.<Integer, String, Boolean> empty().unzipToLists(ArrayList::new);
        assertTrue(empty.left().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().unzipToLists(() -> null));

        TriIterator<Integer, String, Boolean> source = sample();
        assertThrows(IllegalArgumentException.class, () -> source.unzipToCollections(ArrayList::new, () -> null, ArrayList::new));
        assertTrue(source.hasNext());
    }

    @Test
    public void testUnzipToSets() {
        Triple<Set<Integer>, Set<String>, Set<Boolean>> sets = sample().unzipToSets(HashSet::new);
        assertEquals(Set.of(1, 2, 3), sets.left());
        assertEquals(Set.of("a", "b", "c"), sets.middle());
        assertEquals(Set.of(true, false), sets.right());

        Triple<Set<Integer>, Set<String>, Set<Boolean>> ordered = sample().unzipToSets(LinkedHashSet::new);
        assertEquals(1, ordered.left().iterator().next());
        Triple<Set<Integer>, Set<String>, Set<Boolean>> empty = TriIterator.<Integer, String, Boolean> empty().unzipToSets(HashSet::new);
        assertTrue(empty.left().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> TriIterator.empty().unzipToSets(() -> null));
    }

    @Test
    public void testChaining() {
        List<String> results = TriIterator.<Integer, String, Double> generate(0, 20, (i, triple) -> triple.set(i, "val" + i, i * 0.5))
                .skip(2)
                .limit(10)
                .filter((a, b, c) -> a % 3 == 0)
                .map((a, b, c) -> b + "=" + c)
                .toList();
        assertEquals(List.of("val3=1.5", "val6=3.0", "val9=4.5"), results);
        assertEquals(2,
                TriIterator
                        .zip(new String[] { "a", "b", "c", "d", "e", "f" }, new Integer[] { 1, 2, 3, 4, 5, 6 }, new Double[] { 1.1, 2.2, 3.3, 4.4, 5.5, 6.6 })
                        .skip(1)
                        .limit(4)
                        .filter((s, i, d) -> i % 2 == 0)
                        .toList()
                        .size());
    }
}
