package com.landawn.abacus.util;

import java.util.Map;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.IntFunctions;
import com.landawn.abacus.util.stream.Stream;

public class IteratorTest {

    @Test
    public void test_BiIterator_filter() {
        Map<Integer, String> map = N.toMap(Stream.range(1, 10).toList(), Fn.identity(), v -> v + ": aaa", IntFunctions.ofLinkedHashMap());

        BiPredicate<? super Integer, ? super String> predicate_1 = (a, b) -> a > 2;

        BiIterator.of(map).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).filter(predicate_1).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).filter(predicate_1).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());

    }

    @Test
    public void test_BiIterator_limit() {
        Map<Integer, String> map = N.toMap(Stream.range(1, 10).toList(), Fn.identity(), v -> v + ": aaa", IntFunctions.ofLinkedHashMap());

        BiIterator.of(map).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(0).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(1).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(map.size()).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(Long.MAX_VALUE).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(0).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(1).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(map.size()).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).limit(Long.MAX_VALUE).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());

    }

    @Test
    public void test_BiIterator_skip() {
        Map<Integer, String> map = N.toMap(Stream.range(1, 10).toList(), Fn.identity(), v -> v + ": aaa", IntFunctions.ofLinkedHashMap());

        BiIterator.of(map).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(0).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(1).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(map.size()).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(Long.MAX_VALUE).foreachRemaining((a, b) -> N.println(a + "=" + b));
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(0).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(1).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(map.size()).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());
        N.println(Strings.repeat("=", 80));
        BiIterator.of(map).skip(Long.MAX_VALUE).map((a, b) -> a + "=" + b).forEachRemaining(Fn.println());

    }

    @Test
    public void test_BiIterator() {
        Map<Integer, String> map = N.toMap(Stream.range(1, 10).toList(), Fn.identity(), v -> v + ": aaa", IntFunctions.ofLinkedHashMap());

        BiIterator.of(map).toSet().forEach(Fn.println());

        BiIterator.of(map).toList().forEach(Fn.println());

        Pair<Integer, String>[] a = BiIterator.of(map).toArray();
        N.forEach(a, Fn.println());
    }

    @Test
    public void test() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5);
        N.forEach(Iterators.skipAndLimit(iter, 0, 2), Fn.println());

        iter = ObjIterator.of(1, 2, 3, 4, 5);
        N.forEach(Iterators.skipAndLimit(iter, 1, 3), Fn.println());
    }

}
