package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

public class SeqForTest extends SeqTestSupport {

    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);

        collected.clear();
        AtomicBoolean completed = new AtomicBoolean(false);
        Seq.of(1, 2, 3).forEach(collected::add, () -> completed.set(true));
        assertEquals(Arrays.asList(1, 2, 3), collected);
        assertTrue(completed.get());

        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        drainWithException(closed);
        assertThrows(IllegalStateException.class, () -> closed.forEach(x -> {
        }));
    }

    @Test
    public void testForEach_FlatMapper() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of("a", "b").forEach(s -> Arrays.asList(1, 2, 3), (s, i) -> result.add(s + i));
        assertEquals(Arrays.asList("a1", "a2", "a3", "b1", "b2", "b3"), result);

        Map<Integer, List<String>> byNum = new HashMap<>();
        Seq.of(1, 2)
                .forEach(num -> Arrays.asList(String.valueOf(num), "s" + num),
                        (originalNum, mappedString) -> byNum.computeIfAbsent(originalNum, k -> new ArrayList<>()).add(mappedString));
        assertEquals(Arrays.asList("1", "s1"), byNum.get(1));
        assertEquals(Arrays.asList("2", "s2"), byNum.get(2));
    }

    @Test
    public void testForEach_TwoFlatMappers() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of("a", "b").forEach(s -> Arrays.asList(1, 2), i -> Arrays.asList("x", "y"), (s, i, s2) -> result.add(s + i + s2));
        assertEquals(Arrays.asList("a1x", "a1y", "a2x", "a2y", "b1x", "b1y", "b2x", "b2y"), result);

        List<Triple<Integer, String, Character>> triples = new ArrayList<>();
        Seq.of(1, 2)
                .forEach(num -> Arrays.asList("A" + num, "B" + num), str -> Arrays.asList(str.charAt(0), str.charAt(1)),
                        (originalNum, intermediateStr, finalChar) -> triples.add(Triple.of(originalNum, intermediateStr, finalChar)));
        List<Triple<Integer, String, Character>> expected = Arrays.asList(Triple.of(1, "A1", 'A'), Triple.of(1, "A1", '1'), Triple.of(1, "B1", 'B'),
                Triple.of(1, "B1", '1'), Triple.of(2, "A2", 'A'), Triple.of(2, "A2", '2'), Triple.of(2, "B2", 'B'), Triple.of(2, "B2", '2'));
        assertEquals(expected.size(), triples.size());
        assertTrue(triples.containsAll(expected) && expected.containsAll(triples));
    }

    @Test
    public void testForEachIndexed() throws Exception {
        List<String> collected = new ArrayList<>();
        Seq.of("a", "b", "c").forEachIndexed((i, s) -> collected.add(i + ":" + s));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), collected);
    }

    @Test
    public void testForEachUntil() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachUntil((x, flag) -> {
            collected.add(x);
            if (x >= 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);

        MutableBoolean flag = MutableBoolean.of(false);
        collected.clear();
        Seq.of(1, 2, 3, 4, 5).forEachUntil(flag, val -> {
            collected.add(val);
            if (val == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);

        assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).forEachUntil((Throwables.BiConsumer<Integer, MutableBoolean, Exception>) null));
        assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of().forEachUntil((Throwables.BiConsumer<Integer, MutableBoolean, Exception>) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).forEachUntil(null, x -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).forEachUntil(MutableBoolean.of(false), null));
    }

    @Test
    public void testForEachUntil_Closes() throws Exception {
        final Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        final List<Integer> seen = new ArrayList<>();
        seq.forEachUntil((v, flagToBreak) -> {
            seen.add(v);
            if (v == 2) {
                flagToBreak.setTrue();
            }
        });
        assertEquals(CommonUtil.asList(1, 2), seen);
        assertThrows(IllegalStateException.class, seq::toList);

        final Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        final MutableBoolean flagToBreak = MutableBoolean.of(false);
        seen.clear();
        seq2.forEachUntil(flagToBreak, v -> {
            seen.add(v);
            if (v == 2) {
                flagToBreak.setTrue();
            }
        });
        assertEquals(CommonUtil.asList(1, 2), seen);
        assertThrows(IllegalStateException.class, seq2::toList);

        final MutableBoolean closed = MutableBoolean.of(false);
        Seq.<Integer, Exception> of(1, 2).onClose(closed::setTrue).forEachUntil((v, flag) -> {
        });
        assertTrue(closed.value());
    }

    @Test
    public void testForEachPair() throws Exception {
        List<String> collected = new ArrayList<>();
        Seq.of(1, 2, 3, 4).forEachPair((a, b) -> collected.add(a + "-" + b));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), collected);

        collected.clear();
        Seq.of(1, 2, 3, 4, 5, 6).forEachPair(2, (a, b) -> collected.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), collected);

        collected.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8).forEachPair(3, (a, b) -> collected.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "4,5", "7,8"), collected);

        assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).forEachPair(2, (Throwables.BiConsumer<Integer, Integer, Exception>) null));
        assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of().forEachPair(2, (Throwables.BiConsumer<Integer, Integer, Exception>) null));
    }

    @Test
    public void testForEachTriple() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5", "4,5,6"), result);

        result.clear();
        Seq.of(1, 2, 3).forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3"), result);

        result.clear();
        Seq.of(1, 2, 3, 4, 5).forEachTriple(1, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"), result);

        result.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9).forEachTriple(3, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "4,5,6", "7,8,9"), result);

        result.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7).forEachTriple(2, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "3,4,5", "5,6,7"), result);

        result.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).forEachTriple(4, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "5,6,7", "9,10,11"), result);

        assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of(1, 2, 3).forEachTriple(3, (Throwables.TriConsumer<Integer, Integer, Integer, Exception>) null));
        assertThrows(IllegalArgumentException.class,
                () -> Seq.<Integer, Exception> of().forEachTriple(3, (Throwables.TriConsumer<Integer, Integer, Integer, Exception>) null));
    }
}
