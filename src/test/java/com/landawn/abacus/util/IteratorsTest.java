package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

public class IteratorsTest {

    @Test
    public void test_cycle() {
        N.println(Iterators.cycle(1, 2, 3).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2, 3)).limit(10).toList());
        N.println(Strings.repeat("#", 80));
        N.println(Iterators.cycle(CommonUtil.asSet(1), 0).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1), 1).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1), 2).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1), 3).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1), 4).limit(10).toList());
        N.println(Strings.repeat("#", 80));
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2), 0).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2), 1).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2), 2).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2), 3).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2), 4).limit(10).toList());
        N.println(Strings.repeat("#", 80));
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2, 3), 0).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2, 3), 1).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2, 3), 2).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2, 3), 3).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.asSet(1, 2, 3), 4).limit(10).toList());
        N.println(Strings.repeat("#", 80));
        N.println(Stream.of(CommonUtil.asSet(1, 2, 3)).cycled(0).limit(10).toList());
        N.println(Stream.of(CommonUtil.asSet(1, 2, 3)).cycled(1).limit(10).toList());
        N.println(Stream.of(CommonUtil.asSet(1, 2, 3)).cycled(2).limit(10).toList());
        N.println(Stream.of(CommonUtil.asSet(1, 2, 3)).cycled(3).limit(10).toList());
        N.println(Stream.of(CommonUtil.asSet(1, 2, 3)).cycled(4).limit(10).toList());
        N.println(Strings.repeat("#", 80));

        N.println(Iterators.cycle(CommonUtil.EMPTY_CHAR_OBJ_ARRAY).limit(10).toList());
        N.println(Iterators.cycle(CommonUtil.emptyList()).limit(10).toList());
    }

    @Test
    public void test_ObjListIterator() {
        final ObjListIterator<String> iterA = ObjListIterator.empty();
        final ListIterator<Object> iterB = List.of().listIterator();

        assertEquals(iterB.previousIndex(), iterA.previousIndex());
        assertEquals(iterB.nextIndex(), iterA.nextIndex());
        assertEquals(iterB.previousIndex(), iterA.previousIndex());
        assertEquals(iterB.nextIndex(), iterA.nextIndex());
    }

    @Test
    public void test_filter_map() throws Exception {

        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.map(iter, it -> it + "1").forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.flatMap(iter, it -> CommonUtil.asList(it + "1", it + "2")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.flatmap(iter, it -> CommonUtil.asArray(it + "1", it + "2")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
    }

    @Test
    public void test_filter_map_2() throws Exception {

        {
            final List<String> list = CommonUtil.asList("a", "b", "c");
            N.map(list, it -> it + "1").forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.asList("a", "b", "c");
            N.flatMap(list, it -> CommonUtil.asList(it + "1", it + "2")).forEach(Fn.println());
            N.println("==========================================================================");
        }

    }

    @Test
    public void test_filter() throws Exception {
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.filter(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.takeWhile(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.takeWhileInclusive(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.dropWhile(iter, it -> !it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
        {
            final Iterator<String> iter = ObjIterator.of("a", "b", "c");
            Iterators.skipUntil(iter, it -> it.equals("b")).forEachRemaining(Fn.println());
            N.println("==========================================================================");
        }
    }

    @Test
    public void test_skipUntil() throws Exception {
        {
            final int[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            final List<Integer> list = CommonUtil.toList(a);
            final List<Integer> ret = N.skipUntil(list, it -> it > 3);
            assertEquals(CommonUtil.asList(4, 5, 6, 7, 8, 9), ret);
        }
        {
            final int[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            final List<Integer> list = CommonUtil.toList(a);
            final List<Integer> ret = Stream.of(list).skipUntil(it -> it > 3).toList();
            assertEquals(CommonUtil.asList(4, 5, 6, 7, 8, 9), ret);
            final int[] b = IntStream.of(a).skipUntil(it -> it > 3).toArray();
            assertEquals(CommonUtil.asList(4, 5, 6, 7, 8, 9), CommonUtil.toList(b));
        }
    }

    @Test
    public void test_filter_2() throws Exception {
        {
            final List<String> list = CommonUtil.asList("a", "b", "c");
            N.filter(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.asList("a", "b", "c");
            N.takeWhile(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.asList("a", "b", "c");
            N.takeWhileInclusive(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.asList("a", "b", "c");
            N.dropWhile(list, it -> !it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
        {
            final List<String> list = CommonUtil.asList("a", "b", "c");
            N.skipUntil(list, it -> it.equals("b")).forEach(Fn.println());
            N.println("==========================================================================");
        }
    }

    @Test
    public void test_mergeSorted() throws Exception {
        ObjIterator<Integer> a = ObjIterator.of(1, 3, 5);
        ObjIterator<Integer> b = ObjIterator.of(2, 4, 6);

        Iterators.mergeSorted(a, b, Comparators.NATURAL_ORDER).foreachRemaining(Fn.println());

        N.println("==========================================================================");

        a = ObjIterator.of(1, 3, 5);
        b = ObjIterator.of(2, 4, 6);

        com.google.common.collect.Iterators.mergeSorted(CommonUtil.asList(a, b), Comparators.NATURAL_ORDER).forEachRemaining(Fn.println());
    }

    @Test
    public void test_repeat() throws Exception {
        Stream.of(Iterators.repeatElements(CommonUtil.asList(1, 2, 3), 3)).println();
        Stream.of(Iterators.cycle(CommonUtil.asList(1, 2, 3), 3)).println();
        Stream.of(Iterators.repeatElementsToSize(CommonUtil.asList(1, 2, 3), 7)).println();
        Stream.of(Iterators.cycleToSize(CommonUtil.asList(1, 2, 3), 5)).println();

        assertEquals(3, CommonUtil.repeatElementsToSize(CommonUtil.asList(1, 2, 3, 4, 5, 6), 3).size());
        assertEquals(8, CommonUtil.repeatElementsToSize(CommonUtil.asList(1, 2, 3, 4, 5, 6), 8).size());

        assertEquals(3, CommonUtil.cycleToSize(CommonUtil.asList(1, 2, 3, 4, 5, 6), 3).size());
        assertEquals(8, CommonUtil.cycleToSize(CommonUtil.asList(1, 2, 3, 4, 5, 6), 8).size());
    }

    @Test
    public void test_forEachPair_2() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).forEachTriple(4, action);
    }

    @Test
    public void test_forEachPair_2_1() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).forEachTriple(4, action);
    }

    @Test
    public void test_forEachPair_3() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList()).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(4, action);
        N.println("==========================================================================");
    }

    @Test
    public void test_forEachPair_3_1() throws Exception {

        final Throwables.BiConsumer<Integer, Integer, RuntimeException> action2 = (a, b) -> N.println(a + " - " + b);

        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(1, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(2, action2);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).forEachPair(3, action2);
        N.println("==========================================================================");

        final Throwables.TriConsumer<Integer, Integer, Integer, RuntimeException> action = (a, b, c) -> N.println(a + " - " + b + " - " + c);

        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(1, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(2, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(3, action);
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toArray()).forEachTriple(4, action);
        N.println("==========================================================================");
    }

    @Test
    public void test_forEachPair_4_1() throws Exception {

        new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b) {
                return a + b;
            }
        };

        final BiFunction<Integer, Integer, String> action2_1 = (a, b) -> a + "-" + b;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        new TriFunction<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b, final Integer c) {
                return a + b + c;
            }
        };

        final TriFunction<Integer, Integer, Integer, String> action3_1 = (a, b, c) -> a + "-" + b + "-" + c;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).slidingMap(4, true, action3_1).println();

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList().iterator()).parallel(2).slidingMap(4, true, action3_1).println();
    }

    @Test
    public void test_forEachPair_4_2() throws Exception {

        new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b) {
                return a + b;
            }
        };

        final BiFunction<Integer, Integer, String> action2_1 = (a, b) -> a + "-" + b;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, action2_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, true, action2_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, true, action2_1).println();
        N.println("==========================================================================");

        new TriFunction<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b, final Integer c) {
                return a + b + c;
            }
        };

        final TriFunction<Integer, Integer, Integer, String> action3_1 = (a, b, c) -> a + "-" + b + "-" + c;

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).slidingMap(4, true, action3_1).println();

        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(4, action3_1).println();
        N.println("==========================================================================");
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(1, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(2, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(3, true, action3_1).println();
        Stream.of(IntList.range(1, 10).toList()).parallel(2).slidingMap(4, true, action3_1).println();
    }

    @Test
    public void test_forEachPair_4_3() throws Exception {

        new BiFunction<Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b) {
                return a + b;
            }
        };

        final BiFunction<Integer, Integer, String> action2_1 = (a, b) -> a + "-" + b;

        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action2_1).stream().println();
        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action2_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action2_1).stream().println();
        N.println("==========================================================================");

        new TriFunction<Integer, Integer, Integer, Integer>() {
            @Override
            public Integer apply(final Integer a, final Integer b, final Integer c) {
                return a + b + c;
            }
        };

        final TriFunction<Integer, Integer, Integer, String> action3_1 = (a, b, c) -> a + "-" + b + "-" + c;

        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(4, action3_1).stream().println();
        N.println("==========================================================================");
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(1, true, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(2, true, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(3, true, action3_1).stream().println();
        Seq.<Integer, RuntimeException> of(IntList.range(1, 10).toList().iterator()).slidingMap(4, true, action3_1).stream().println();

    }

}
