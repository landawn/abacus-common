package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class SeqZipTest extends SeqTestSupport {

    @Test
    public void testZip_Arrays() throws Exception {
        assertEquals(Arrays.asList("1a", "2b"), Seq.zip(new Integer[] { 1, 2, 3 }, new String[] { "a", "b" }, (x, y) -> x + y).toList());
        assertTrue(Seq.zip(new Integer[] {}, new String[] {}, (n, s) -> n + s).toList().isEmpty());
        assertEquals(Arrays.asList("1atrue", "2bfalse"),
                Seq.zip(new Integer[] { 1, 2 }, new String[] { "a", "b", "c" }, new Boolean[] { true, false }, (x, y, z) -> x + y + z).toList());
        assertEquals(Arrays.asList("a1", "null2", "c3"),
                Seq.zip(new String[] { "a", null, "c" }, new String[] { "1", "2", "3" }, (s1, s2) -> (s1 == null ? "null" : s1) + s2).toList());
    }

    @Test
    public void testZip_Arrays_Defaults() throws Exception {
        assertEquals(Arrays.asList("1x", "2y", "0z"), Seq.zip(new Integer[] { 1, 2 }, new String[] { "x", "y", "z" }, 0, "default", (x, y) -> x + y).toList());
        assertEquals(Arrays.asList("1a", "2b", "3Y", "4Y"), Seq.zip(new Integer[] { 1, 2, 3, 4 }, new String[] { "a", "b" }, 0, "Y", (n, s) -> n + s).toList());
        assertEquals(Arrays.asList("1xtrue", "0yfalse", "0defBtrue"),
                Seq.zip(new Integer[] { 1 }, new String[] { "x", "y" }, new Boolean[] { true, false, true }, 0, "defB", false, (x, y, z) -> "" + x + y + z)
                        .toList());
        assertEquals(Arrays.asList("1a1.1", "2b0.0", "0c0.0"),
                Seq.zip(new Integer[] { 1, 2 }, new String[] { "a", "b", "c" }, new Double[] { 1.1 }, 0, "X", 0.0, (n, s, d) -> n + s + d).toList());
    }

    @Test
    public void testZip_Iterables() throws Exception {
        assertEquals(Arrays.asList("1a", "2b"), Seq.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b"), (x, y) -> x + y).toList());
        Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        assertEquals(3, Seq.zip(Arrays.asList(1, 2, 3), set, (n, s) -> n + s).toList().size());
        assertEquals(Arrays.asList("1atrue", "2bfalse"),
                Seq.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), Arrays.asList(true, false), (x, y, z) -> x + y + z).toList());
        assertEquals(Arrays.asList("1a1.1", "2b2.2"),
                Seq.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"), Arrays.asList(1.1, 2.2), (n, s, d) -> n + s + d).toList());
    }

    @Test
    public void testZip_Iterables_Defaults() throws Exception {
        assertEquals(Arrays.asList("1x", "2y", "0z"), Seq.zip(Arrays.asList(1, 2), Arrays.asList("x", "y", "z"), 0, "default", (x, y) -> x + y).toList());
        assertEquals(Arrays.asList("1xtrue", "0yfalse", "0defBtrue"),
                Seq.zip(Arrays.asList(1), Arrays.asList("x", "y"), Arrays.asList(true, false, true), 0, "defB", false, (x, y, z) -> "" + x + y + z).toList());
        assertEquals(Arrays.asList("1a1.1", "0b2.2", "0X3.3"),
                Seq.zip(Arrays.asList(1), Arrays.asList("a", "b"), Arrays.asList(1.1, 2.2, 3.3), 0, "X", 0.0, (n, s, d) -> n + s + d).toList());
    }

    @Test
    public void testZip_Iterators() throws Exception {
        assertEquals(Arrays.asList("1a", "2b"), Seq.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a", "b").iterator(), (x, y) -> x + y).toList());
        assertEquals(Arrays.asList("1a", "2b", "3c"),
                Seq.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a", "b", "c", "d").iterator(), (n, s) -> n + s).toList());
        assertEquals(Arrays.asList("1atrue", "2bfalse"),
                Seq.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), Arrays.asList(true, false).iterator(), (x, y, z) -> x + y + z)
                        .toList());
        assertEquals(Arrays.asList("1a1.1", "2b2.2"), Seq
                .zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), Arrays.asList(1.1, 2.2, 3.3).iterator(), (n, s, d) -> n + s + d)
                .toList());
    }

    @Test
    public void testZip_Iterators_Defaults() throws Exception {
        assertEquals(Arrays.asList("1x", "2y", "0z"),
                Seq.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("x", "y", "z").iterator(), 0, "default", (x, y) -> x + y).toList());
        assertEquals(Arrays.asList("1a", "2b", "3Z"),
                Seq.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a", "b").iterator(), 0, "Z", (n, s) -> n + s).toList());
        assertEquals(
                Arrays.asList("1a1.1", "99b2.2", "99c9.9"), Seq
                        .zip(Arrays.asList(1).iterator(), Arrays.asList("a", "b", "c").iterator(), Arrays.asList(1.1, 2.2).iterator(), 99, "Y", 9.9,
                                (n, s, d) -> n + s + d)
                        .toList());
    }

    @Test
    public void testZip_Seqs() throws Exception {
        assertEquals(Arrays.asList("1a", "2b"), Seq.zip(Seq.of(1, 2, 3), Seq.of("a", "b"), (x, y) -> x + y).toList());
        assertEquals(Arrays.asList("1atrue", "2bfalse"), Seq.zip(Seq.of(1, 2), Seq.of("a", "b", "c"), Seq.of(true, false), (x, y, z) -> x + y + z).toList());
        List<Pair<Integer, String>> pairs = Seq.zip(Seq.of(1, 2, 3), Seq.of("a", "b", "c"), Pair::of).toList();
        assertEquals(3, pairs.size());
        assertEquals(Integer.valueOf(1), pairs.get(0).left());
        assertEquals("a", pairs.get(0).right());
    }

    @Test
    public void testZip_Seqs_Defaults() throws Exception {
        assertEquals(Arrays.asList("1x", "2y", "0z"), Seq.zip(Seq.of(1, 2), Seq.of("x", "y", "z"), 0, "default", (x, y) -> x + y).toList());
        assertEquals(Arrays.asList("1a", "2b", "0c", "0d"), Seq.zip(Seq.of(1, 2), Seq.of("a", "b", "c", "d"), 0, "X", (n, s) -> n + s).toList());
        assertEquals(Arrays.asList("1a1.1", "0b2.2", "0X3.3"),
                Seq.zip(Seq.of(1), Seq.of("a", "b"), Seq.of(1.1, 2.2, 3.3), 0, "X", 0.0, (n, s, d) -> n + s + d).toList());
    }

    @Test
    public void testZipWith_Collection() throws Exception {
        assertEquals(Arrays.asList("a1", "b2", "c3"), Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), (s, i) -> s + i).toList());
        assertEquals(Arrays.asList("1a", "2b"), Seq.of(1, 2).zipWith(Arrays.asList("a", "b", "c"), (i, s) -> i + s).toList());
        assertEquals(Arrays.asList("1a", "2b", "0c"), Seq.of(1, 2).zipWith(Arrays.asList("a", "b", "c"), 0, "def", (i, s) -> i + s).toList());
        assertEquals(Arrays.asList("a1", "b2", "c99"), Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2), "x", 99, (s, i) -> s + i).toList());
        assertEquals(Arrays.asList("a1", "b2", "z3"), Seq.of("a", "b").zipWith(Arrays.asList(1, 2, 3), "z", 0, (s, i) -> s + i).toList());
        assertArrayEquals(new String[] { "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y)
                        .skip(2)
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "c3", "z4", "z5" },
                Seq.<String, RuntimeException> of("a", "b", "c")
                        .zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y)
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWith_TwoCollections() throws Exception {
        assertEquals(Arrays.asList("1atrue", "2bfalse"),
                Seq.of(1, 2).zipWith(Arrays.asList("a", "b", "c"), Arrays.asList(true, false), (i, s, bool) -> i + s + bool).toList());
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0defStrue"),
                Seq.of(1).zipWith(Arrays.asList("a", "b"), Arrays.asList(true, false, true), 0, "defS", false, (i, s, bool) -> i + s + bool).toList());
        assertEquals(Arrays.asList("a1x", "b2w", "z3w"),
                Seq.of("a", "b").zipWith(Arrays.asList(1, 2, 3), Arrays.asList("x"), "z", 0, "w", (s, i, s2) -> s + i + s2).toList());
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"),
                Seq.of("a", "b").zipWith(Arrays.asList(1), Arrays.asList(true, false, true), "x", 99, false, (s, i, b) -> s + i + b).toList());
        assertArrayEquals(new String[] { "b2y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .skip(1)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWith_Seq() throws Exception {
        assertEquals(Arrays.asList("a1", "b2", "c3"), Seq.of("a", "b", "c").zipWith(Seq.of(1, 2, 3), (s, i) -> s + i).toList());
        assertEquals(Arrays.asList("1a", "2b"), Seq.of(1, 2).zipWith(Seq.of("a", "b", "c"), (i, s) -> i + s).toList());
        assertEquals(Arrays.asList("1a", "2b", "0c"), Seq.of(1, 2).zipWith(Seq.of("a", "b", "c"), 0, "def", (i, s) -> i + s).toList());
        assertEquals(Arrays.asList("a1", "b2", "z3"), Seq.of("a", "b").zipWith(Seq.of(1, 2, 3), "z", 0, (s, i) -> s + i).toList());
        assertEquals(Arrays.asList("a1", "b2", "c99"), Seq.of("a", "b", "c").zipWith(Seq.of(1, 2), "x", 99, (s, i) -> s + i).toList());
        assertArrayEquals(new String[] { "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).skip(2).toArray(String[]::new));
    }

    @Test
    public void testZipWith_TwoSeqs() throws Exception {
        assertEquals(Arrays.asList("1atrue", "2bfalse"),
                Seq.of(1, 2).zipWith(Seq.of("a", "b", "c"), Seq.of(true, false), (i, s, bool) -> i + s + bool).toList());
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0defStrue"),
                Seq.of(1).zipWith(Seq.of("a", "b"), Seq.of(true, false, true), 0, "defS", false, (i, s, bool) -> i + s + bool).toList());
        assertEquals(Arrays.asList("a1x", "b2w", "z3w"),
                Seq.of("a", "b").zipWith(Seq.of(1, 2, 3), Seq.of("x"), "z", 0, "w", (s, i, s2) -> s + i + s2).toList());
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"),
                Seq.of("a", "b").zipWith(Seq.of(1), Seq.of(true, false, true), "x", 99, false, (s, i, b) -> s + i + b).toList());
    }

    @Test
    public void zip_arrayZipperThrowDoesNotSkipPair() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger remainingThrows = new java.util.concurrent.atomic.AtomicInteger(1);
        final Seq<String, RuntimeException> seq = Seq.zip(new Integer[] { 1, 2, 3 }, new String[] { "a", "b", "c" }, (i, s) -> {
            if (i == 2 && remainingThrows.getAndDecrement() > 0) {
                throw new IllegalArgumentException("boom");
            }
            return i + s;
        });
        final Throwables.Iterator<String, RuntimeException> iter = seq.iteratorEx();
        assertEquals("1a", iter.next());
        assertThrows(IllegalArgumentException.class, iter::next);
        assertEquals("2b", iter.next());
        assertEquals("3c", iter.next());
        seq.close();
    }

    @Test
    public void zip_tripleArrayZipperThrowDoesNotSkipTriple() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger remainingThrows = new java.util.concurrent.atomic.AtomicInteger(1);
        final Seq<String, RuntimeException> seq = Seq.zip(new Integer[] { 1, 2 }, new String[] { "a", "b" }, new Boolean[] { true, false }, (i, s, z) -> {
            if (i == 2 && remainingThrows.getAndDecrement() > 0) {
                throw new IllegalArgumentException("boom");
            }
            return i + s + z;
        });
        final Throwables.Iterator<String, RuntimeException> iter = seq.iteratorEx();
        assertEquals("1atrue", iter.next());
        assertThrows(IllegalArgumentException.class, iter::next);
        assertEquals("2bfalse", iter.next());
        seq.close();
    }
}
