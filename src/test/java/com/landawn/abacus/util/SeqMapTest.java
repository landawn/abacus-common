package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

public class SeqMapTest extends SeqTestSupport {

    @Test
    public void testMap() throws Exception {
        assertEquals(Arrays.asList(2, 4, 6), Seq.of(1, 2, 3).map(x -> x * 2).toList());
        assertEquals(Arrays.asList(1, 2, 3), Seq.of("1", "2", "3").map(Integer::parseInt).toList());
        assertArrayEquals(new String[] { "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).skip(2).toArray(String[]::new));

        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        drainWithException(closed);
        assertThrows(IllegalStateException.class, () -> closed.map(x -> x * 2));
    }

    @Test
    public void testMapIfNotNull() throws Exception {
        assertEquals(Arrays.asList(2, 6), Seq.of(1, null, 3).mapIfNotNull(x -> x * 2).toList());
        assertEquals(Arrays.asList("aa", "bb", "cc"), Seq.of("a", null, "b", null, "c").mapIfNotNull(s -> s + s).toList());
        assertArrayEquals(new String[] { "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapFirst() throws Exception {
        assertEquals(Arrays.asList(10, 2, 3), Seq.of(1, 2, 3).mapFirst(x -> x * 10).toList());
        assertTrue(Seq.<Integer, Exception> empty().mapFirst(x -> x * 10).toList().isEmpty());
        assertEquals(Arrays.asList(50), Seq.of(5).mapFirst(x -> x * 10).toList());
        assertArrayEquals(new String[] { "A", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapFirstOrElse() throws Exception {
        assertEquals(Arrays.asList(10, 4, 6), Seq.of(1, 2, 3).mapFirstOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(Arrays.asList(10, 3, 4), Seq.of(1, 2, 3).mapFirstOrElse(x -> x * 10, y -> y + 1).toList());
        assertTrue(Seq.<Integer, Exception> empty().mapFirstOrElse(x -> "first:" + x, x -> "other:" + x).toList().isEmpty());
        assertArrayEquals(new String[] { "FIRST", "b!", "c!", "d!", "e!" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").toArray(String[]::new));
    }

    @Test
    public void testMapLast() throws Exception {
        assertEquals(Arrays.asList(1, 2, 30), Seq.of(1, 2, 3).mapLast(x -> x * 10).toList());
        assertTrue(Seq.<Integer, Exception> empty().mapLast(x -> x * 10).toList().isEmpty());
        assertEquals(Arrays.asList(50), Seq.of(5).mapLast(x -> x * 10).toList());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapLastOrElse() throws Exception {
        assertEquals(Arrays.asList(2, 4, 30), Seq.of(1, 2, 3).mapLastOrElse(x -> x * 10, x -> x * 2).toList());
        assertEquals(Arrays.asList(2, 3, 30), Seq.of(1, 2, 3).mapLastOrElse(x -> x * 10, y -> y + 1).toList());
        assertTrue(Seq.<Integer, Exception> empty().mapLastOrElse(x -> "last:" + x, x -> "other:" + x).toList().isEmpty());
        assertArrayEquals(new String[] { "a!", "b!", "c!", "d!", "LAST" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").toArray(String[]::new));
    }

    @Test
    public void testMapPartial() throws Exception {
        assertEquals(Arrays.asList(20, 40), Seq.of(1, 2, 3, 4, 5).mapPartial(x -> x % 2 == 0 ? Optional.of(x * 10) : Optional.<Integer> empty()).toList());
        assertEquals(Arrays.asList(1, 2), Seq.of("1", "x", "2", "y").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList());
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), Seq.range(0, 10).mapPartial(it -> it % 2 == 0 ? Optional.of(it) : Optional.empty()).toList());
        assertArrayEquals(new String[] { "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .toArray(String[]::new));
    }

    @Test
    public void testMapPartialToInt() throws Exception {
        assertEquals(Arrays.asList(1, 3), Seq.of("1", "a", "3", "b").mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }).toList());
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), Seq.range(0, 10).mapPartialToInt(it -> it % 2 == 0 ? OptionalInt.of(it) : OptionalInt.empty()).toList());
        assertArrayEquals(new Integer[] { 99, 100, 101 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).skip(2).toArray(Integer[]::new));
    }

    @Test
    public void testMapPartialToLong() throws Exception {
        assertEquals(Arrays.asList(1L, 3L), Seq.of("1", "a", "3", "b").mapPartialToLong(s -> {
            try {
                return OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }).toList());
        assertArrayEquals(new Long[] { 99L, 100L, 101L },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).skip(2).toArray(Long[]::new));
    }

    @Test
    public void testMapPartialToDouble() throws Exception {
        assertEquals(Arrays.asList(1.5, 3.5), Seq.of("1.5", "a", "3.5", "b").mapPartialToDouble(s -> {
            try {
                return OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return OptionalDouble.empty();
            }
        }).toList());
        assertArrayEquals(new Double[] { 99.0, 100.0, 101.0 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0)))
                        .skip(2)
                        .toArray(Double[]::new));
    }

    @Test
    public void testMapMulti() throws Exception {
        List<Object> doubled = Seq.of(1, 2, 3).mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), doubled);

        assertEquals(Arrays.asList("N:1", "N:2", "Even:2", "N:3"), Seq.of(1, 2, 3).mapMulti((num, consumer) -> {
            consumer.accept("N:" + num);
            if (num % 2 == 0) {
                consumer.accept("Even:" + num);
            }
        }).toList());
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
            consumer.accept(s);
            consumer.accept(s.toUpperCase());
        }).skip(4).toArray(String[]::new));
    }
}
