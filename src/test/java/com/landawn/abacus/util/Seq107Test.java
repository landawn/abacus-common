package com.landawn.abacus.util;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("new-test")
public class Seq107Test extends TestBase {
    @Test
    public void testFilterWithSkipCountAndToArray() throws Exception {
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).skip(2).count());
        assertArrayEquals(new String[] { "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).skip(2).toArray(String[]::new));
    }

    @Test
    public void testFilterWithActionOnDroppedItemWithSkipCountAndToArray() throws Exception {
        List<String> dropped = new ArrayList<>();
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).count());
        assertEquals(1, dropped.size());

        dropped.clear();
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).skip(2).count());

        dropped.clear();
        assertArrayEquals(new String[] { "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).toArray(String[]::new));

        dropped.clear();
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testTakeWhileWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "c" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDropWhileWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).skip(1).toArray(String[]::new));
    }

    @Test
    public void testDropWhileWithActionOnDroppedItemWithSkipCountAndToArray() throws Exception {
        List<String> dropped = new ArrayList<>();
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).count());
        assertEquals(3, dropped.size());

        dropped.clear();
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).toArray(String[]::new));

        dropped.clear();
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).skip(1).toArray(String[]::new));
    }

    @Test
    public void testSkipUntilWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).skip(1).toArray(String[]::new));
    }

    @Test
    public void testDistinctWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().toArray(String[]::new));
        assertArrayEquals(new String[] { "c" }, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().skip(2).toArray(String[]::new));
    }

    @Test
    public void testDistinctByWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).skip(2).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).toArray(String[]::new));
        assertArrayEquals(new String[] { "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "A", "B", "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapIfNotNullWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "A", "C", "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapFirstWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "A", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapFirstOrElseWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").skip(2).count());
        assertArrayEquals(new String[] { "FIRST", "b!", "c!", "d!", "e!" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").toArray(String[]::new));
        assertArrayEquals(new String[] { "c!", "d!", "e!" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapLastWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapLastOrElseWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").skip(2).count());
        assertArrayEquals(new String[] { "a!", "b!", "c!", "d!", "LAST" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").toArray(String[]::new));
        assertArrayEquals(new String[] { "c!", "d!", "LAST" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").skip(2).toArray(String[]::new));
    }

    @Test
    public void testFlatMapWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).skip(4).toArray(String[]::new));
    }

    @Test
    public void testFlatmapWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).skip(4).toArray(String[]::new));
    }

    @Test
    public void testFlattmapWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flattmap(s -> new String[] { s, s.toUpperCase() }).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flattmap(s -> new String[] { s, s.toUpperCase() }).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flattmap(s -> new String[] { s, s.toUpperCase() }).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flattmap(s -> new String[] { s, s.toUpperCase() }).skip(4).toArray(String[]::new));
    }

    @Test
    public void testFlatmapIfNotNullWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", null, "c", null, "e").flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase())).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", null, "c", null, "e").flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase())).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "c", "C", "e", "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e").flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase())).toArray(String[]::new));
        assertArrayEquals(new String[] { "e", "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()))
                        .skip(4)
                        .toArray(String[]::new));
    }

    @Test
    public void testMapPartialWithSkipCountAndToArray() throws Exception {
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testMapPartialToIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).skip(2).count());
        assertArrayEquals(new Integer[] { 97, 98, 99, 100, 101 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 99, 100, 101 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).skip(2).toArray(Integer[]::new));
    }

    @Test
    public void testMapPartialToLongWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).skip(2).count());
        assertArrayEquals(new Long[] { 97L, 98L, 99L, 100L, 101L },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).toArray(Long[]::new));
        assertArrayEquals(new Long[] { 99L, 100L, 101L },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).skip(2).toArray(Long[]::new));
    }

    @Test
    public void testMapPartialToDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0))).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0))).skip(2).count());
        assertArrayEquals(new Double[] { 97.0, 98.0, 99.0, 100.0, 101.0 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0))).toArray(Double[]::new));
        assertArrayEquals(new Double[] { 99.0, 100.0, 101.0 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0)))
                        .skip(2)
                        .toArray(Double[]::new));
    }

    @Test
    public void testMapMultiWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
            consumer.accept(s);
            consumer.accept(s.toUpperCase());
        }).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
            consumer.accept(s);
            consumer.accept(s.toUpperCase());
        }).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
                    consumer.accept(s);
                    consumer.accept(s.toUpperCase());
                }).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
            consumer.accept(s);
            consumer.accept(s.toUpperCase());
        }).skip(4).toArray(String[]::new));
    }

    @Test
    public void testSlidingMapBiFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).skip(2).count());
        assertArrayEquals(new String[] { "ab", "bc", "cd", "de" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).toArray(String[]::new));
        assertArrayEquals(new String[] { "cd", "de" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSlidingMapWithIncrementWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).skip(2).count());
        assertArrayEquals(new String[] { "ab", "cd", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSlidingMapWithIncrementIgnoreNotPairedWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(0, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).skip(2).count());
        assertArrayEquals(new String[] { "ab", "cd" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).toArray(String[]::new));
        assertArrayEquals(new String[] {},
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSlidingMapTriFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c)).count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c))
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "abc", "bcd", "cde" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "cde" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c))
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testIntersectionWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).skip(2).count());
        assertArrayEquals(new String[] { "b", "c", "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).toArray(String[]::new));
        assertArrayEquals(new String[] { "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testIntersectionWithMapperWithSkipCountAndToArray() throws Exception {
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F")).count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "b", "c", "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testDifferenceWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).skip(1).count());
        assertArrayEquals(new String[] { "a", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).skip(1).toArray(String[]::new));
    }

    @Test
    public void testDifferenceWithMapperWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F")).count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F")).skip(1).count());
        assertArrayEquals(new String[] { "a", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .skip(1)
                        .toArray(String[]::new));
    }

    @Test
    public void testSymmetricDifferenceWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").symmetricDifference(Arrays.asList("b", "c", "d", "f")).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").symmetricDifference(Arrays.asList("b", "c", "d", "f")).skip(1).count());
        assertArrayEquals(new String[] { "a", "e", "f" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").symmetricDifference(Arrays.asList("b", "c", "d", "f")).toArray(String[]::new));
        assertArrayEquals(new String[] { "e", "f" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .symmetricDifference(Arrays.asList("b", "c", "d", "f"))
                        .skip(1)
                        .toArray(String[]::new));
    }

    @Test
    public void testPrependArrayWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").skip(2).count());
        assertArrayEquals(new String[] { "x", "y", "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").toArray(String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testPrependCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).skip(2).count());
        assertArrayEquals(new String[] { "x", "y", "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).toArray(String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPrependOptionalWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).count());
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).skip(2).count());
        assertArrayEquals(new String[] { "x", "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).toArray(String[]::new));
        assertArrayEquals(new String[] { "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendArrayWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendOptionalWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).count());
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "x" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e", "x" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendIfEmptyArrayWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testDefaultIfEmptyWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").skip(2).toArray(String[]::new));
    }

    @Test
    public void testThrowIfEmptyWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().skip(2).toArray(String[]::new));
    }

    @Test
    public void testIfEmptyWithSkipCountAndToArray() throws Exception {
        List<String> actions = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).count());
        assertEquals(0, actions.size());

        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).skip(2).count());

        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).toArray(String[]::new));

        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testOnEachWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onEach(seen::add).count());
        assertEquals(5, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onEach(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onEach(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onEach(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testOnFirstWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testOnLastWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).count());
        assertEquals(5, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekFirstWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekLastWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekIfPredicateWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).count());
        assertEquals(3, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSplitIntWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).skip(1).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList("a", "b", "c"), result.get(0));
        assertEquals(Arrays.asList("d", "e"), result.get(1));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).skip(1).toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList("d", "e"), result.get(0));
    }

    @Test
    public void testSplitPredicateWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).skip(1).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d", "e"), result.get(1));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).skip(1).toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList("c", "d", "e"), result.get(0));
    }

    @Test
    public void testSlidingIntWithSkipCountAndToArray() throws Exception {
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).skip(2).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).toList();
        assertEquals(4, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("b", "c"), result.get(1));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).skip(2).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList("c", "d"), result.get(0));
        assertEquals(Arrays.asList("d", "e"), result.get(1));
    }

    @Test
    public void testSlidingIntIntWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).skip(2).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d"), result.get(1));
        assertEquals(Arrays.asList("e"), result.get(2));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).skip(2).toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList("e"), result.get(0));
    }

    @Test
    public void testSkipWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).skip(2).count());
        assertArrayEquals(new String[] { "c", "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSkipNullsWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().skip(2).count());
        assertArrayEquals(new String[] { "a", "c", "e" }, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().skip(2).toArray(String[]::new));
    }

    @Test
    public void testSkipLastWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testLimitWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).toArray(String[]::new));
        assertArrayEquals(new String[] { "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).skip(2).toArray(String[]::new));
    }

    @Test
    public void testStepWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).skip(2).count());
        assertArrayEquals(new String[] { "a", "c", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testIndexedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().skip(2).count());

        Indexed<String>[] result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().toArray(Indexed[]::new);
        assertEquals(5, result.length);
        assertEquals("a", result[0].value());
        assertEquals(0, result[0].index());
        assertEquals("e", result[4].value());
        assertEquals(4, result[4].index());

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().skip(2).toArray(Indexed[]::new);
        assertEquals(3, result.length);
        assertEquals("c", result[0].value());
        assertEquals(2, result[0].index());
    }

    @Test
    public void testBufferedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().skip(2).toArray(String[]::new));
    }

    @Test
    public void testBufferedIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMergeWithCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(5,
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWithCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y).skip(2).count());
        assertArrayEquals(new String[] { "a1", "b2", "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y).toArray(String[]::new));
        assertArrayEquals(new String[] { "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y)
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWithCollectionDefaultsWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c").zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y).count());
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c").zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y).skip(2).count());
        assertArrayEquals(new String[] { "a1", "b2", "c3", "z4", "z5" },
                Seq.<String, RuntimeException> of("a", "b", "c")
                        .zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y)
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "c3", "z4", "z5" },
                Seq.<String, RuntimeException> of("a", "b", "c")
                        .zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y)
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWithTwoCollectionsWithSkipCountAndToArray() throws Exception {
        assertEquals(2,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "a1x", "b2y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "b2y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .skip(1)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWithSeqWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).skip(2).count());
        assertArrayEquals(new String[] { "a1", "b2", "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).toArray(String[]::new));
        assertArrayEquals(new String[] { "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).skip(2).toArray(String[]::new));
    }

    @Test
    public void testInterspersWithSkipCountAndToArray() throws Exception {
        assertEquals(9, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").skip(4).count());
        assertArrayEquals(new String[] { "a", "-", "b", "-", "c", "-", "d", "-", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "-", "d", "-", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").skip(4).toArray(String[]::new));
    }

    @Test
    public void testCycledWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c").cycled().limit(5).count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c").cycled().skip(10).limit(5).count());
        assertArrayEquals(new String[] { "a", "b", "c", "a", "b" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled().limit(5).toArray(String[]::new));
        assertArrayEquals(new String[] { "b", "c", "a" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled().skip(1).limit(3).toArray(String[]::new));
    }

    @Test
    public void testCycledLongWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).count());
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testRateLimitedDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).skip(2).toArray(String[]::new));
    }

    @Test
    public void testRateLimitedRateLimiterWithSkipCountAndToArray() throws Exception {
        RateLimiter rateLimiter = RateLimiter.create(1000);
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDelayWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDelayJavaTimeWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDistinctMergeFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).skip(2).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c" },
                Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).toArray(String[]::new));
        assertArrayEquals(new String[] { "b", "B", "c" },
                Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDistinctByMergeFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).skip(2).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).toArray(String[]::new));
        assertArrayEquals(new String[] { "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).skip(2).toArray(String[]::new));
    }

    @Test
    public void testFlatmapIfNotNullTwoMappersWithSkipCountAndToArray() throws Exception {
        assertEquals(8,
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .count());
        assertEquals(6,
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "a!", "a?", "A!", "A?", "c!", "c?", "C!", "C?" },
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "A!", "A?", "c!", "c?", "C!", "C?" },
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testGroupByKeyMapperWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").groupBy(String::length).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").groupBy(String::length).skip(2).count());

        Map.Entry<Integer, List<String>>[] entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb")
                .groupBy(String::length)
                .toArray(Map.Entry[]::new);
        assertEquals(3, entries.length);

        entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").groupBy(String::length).skip(2).toArray(Map.Entry[]::new);
        assertEquals(1, entries.length);
    }

    @Test
    public void testPartitionByWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").partitionBy(s -> s.compareTo("c") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").partitionBy(s -> s.compareTo("c") < 0).skip(1).count());

        Map.Entry<Boolean, List<String>>[] entries = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                .partitionBy(s -> s.compareTo("c") < 0)
                .toArray(Map.Entry[]::new);
        assertEquals(2, entries.length);

        entries = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").partitionBy(s -> s.compareTo("c") < 0).skip(1).toArray(Map.Entry[]::new);
        assertEquals(1, entries.length);
    }

    @Test
    public void testCountByWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").countBy(String::length).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").countBy(String::length).skip(2).count());

        Map.Entry<Integer, Integer>[] entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb")
                .countBy(String::length)
                .toArray(Map.Entry[]::new);
        assertEquals(3, entries.length);

        entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").countBy(String::length).skip(2).toArray(Map.Entry[]::new);
        assertEquals(1, entries.length);
    }

    @Test
    public void testTopIntWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).skip(2).count());

        String[] top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).toArray(String[]::new);
        assertEquals(3, top.length);
        assertTrue(Arrays.asList(top).contains("c"));
        assertTrue(Arrays.asList(top).contains("d"));
        assertTrue(Arrays.asList(top).contains("e"));

        top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).skip(2).toArray(String[]::new);
        assertEquals(1, top.length);
    }

    @Test
    public void testTopIntComparatorWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).skip(2).count());

        String[] top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).toArray(String[]::new);
        assertEquals(3, top.length);
        assertTrue(Arrays.asList(top).contains("a"));
        assertTrue(Arrays.asList(top).contains("b"));
        assertTrue(Arrays.asList(top).contains("c"));

        top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).skip(2).toArray(String[]::new);
        assertEquals(1, top.length);
    }

    @Test
    public void testReversedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().skip(2).toArray(String[]::new));
    }

    @Test
    public void testRotatedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).skip(2).count());
        assertArrayEquals(new String[] { "d", "e", "a", "b", "c" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testShuffledWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().skip(2).count());

        String[] shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().toArray(String[]::new);
        assertEquals(5, shuffled.length);
        assertTrue(Arrays.asList(shuffled).contains("a"));
        assertTrue(Arrays.asList(shuffled).contains("b"));
        assertTrue(Arrays.asList(shuffled).contains("c"));
        assertTrue(Arrays.asList(shuffled).contains("d"));
        assertTrue(Arrays.asList(shuffled).contains("e"));

        shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().skip(2).toArray(String[]::new);
        assertEquals(3, shuffled.length);
    }

    @Test
    public void testShuffledRandomWithSkipCountAndToArray() throws Exception {
        Random random = new Random(42);
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).count());

        random = new Random(42);
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).skip(2).count());

        random = new Random(42);
        String[] shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).toArray(String[]::new);
        assertEquals(5, shuffled.length);

        random = new Random(42);
        shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).skip(2).toArray(String[]::new);
        assertEquals(3, shuffled.length);
    }

    @Test
    public void testSortedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" }, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedComparatorWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedByIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).skip(2).count());
        assertArrayEquals(new String[] { "b", "cc", "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedByLongWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "b", "cc", "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedByDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "b", "cc", "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedByWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSortedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSortedComparatorWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSortedByIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).skip(2).count());
        assertArrayEquals(new String[] { "eeeee", "dddd", "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSortedByLongWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByLong(s -> (long) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByLong(s -> (long) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "eeeee", "dddd", "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByLong(s -> (long) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee")
                        .reverseSortedByLong(s -> (long) s.length())
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testReverseSortedByDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByDouble(s -> (double) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByDouble(s -> (double) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "eeeee", "dddd", "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByDouble(s -> (double) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee")
                        .reverseSortedByDouble(s -> (double) s.length())
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testReverseSortedByWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testLastWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).skip(1).toArray(String[]::new));
    }

    @Test
    public void testTakeLastWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).skip(1).toArray(String[]::new));
    }

    @Test
    public void testSplitAtIntWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).skip(1).count());

        Seq<String, RuntimeException>[] split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).toArray(Seq[]::new);
        assertEquals(2, split.length);
        assertArrayEquals(new String[] { "a", "b", "c" }, split[0].toArray(String[]::new));
        assertArrayEquals(new String[] { "d", "e" }, split[1].toArray(String[]::new));

        split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).skip(1).toArray(Seq[]::new);
        assertEquals(1, split.length);
        assertArrayEquals(new String[] { "d", "e" }, split[0].toArray(String[]::new));
    }

    @Test
    public void testSplitAtPredicateWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(s -> s.compareTo("c") > 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(s -> s.compareTo("c") > 0).skip(1).count());

        Seq<String, RuntimeException>[] split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                .splitAt(s -> s.compareTo("c") > 0)
                .toArray(Seq[]::new);
        assertEquals(2, split.length);
        assertArrayEquals(new String[] { "a", "b", "c" }, split[0].toArray(String[]::new));
        assertArrayEquals(new String[] { "d", "e" }, split[1].toArray(String[]::new));

        split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(s -> s.compareTo("c") > 0).skip(1).toArray(Seq[]::new);
        assertEquals(1, split.length);
        assertArrayEquals(new String[] { "d", "e" }, split[0].toArray(String[]::new));
    }
}
