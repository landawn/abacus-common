package com.landawn.abacus.util;

import static org.junit.Assert.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.function.UnaryOperator;

@Tag("new-test")
public class Fn105Test extends TestBase {

    @Test
    public void testLimitThenFilterPredicate() {
        Predicate<Integer> predicate = Fn.limitThenFilter(3, i -> i % 2 == 0);

        assertTrue(predicate.test(2));
        assertFalse(predicate.test(3));
        assertTrue(predicate.test(4));
        assertFalse(predicate.test(6));

        Predicate<String> zeroLimitPredicate = Fn.limitThenFilter(0, s -> true);
        assertFalse(zeroLimitPredicate.test("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(-1, i -> true));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(1, (Predicate<Integer>) null));
    }

    @Test
    public void testLimitThenFilterBiPredicate() {
        BiPredicate<Integer, String> biPredicate = Fn.limitThenFilter(2, (i, s) -> s.length() == i);

        assertTrue(biPredicate.test(3, "abc"));
        assertFalse(biPredicate.test(2, "abc"));
        assertFalse(biPredicate.test(4, "test"));

        BiPredicate<Integer, String> zeroLimitBiPredicate = Fn.limitThenFilter(0, (i, s) -> true);
        assertFalse(zeroLimitBiPredicate.test(1, "test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(-1, (BiPredicate<Integer, String>) (i, s) -> true));

        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(1, (BiPredicate<Integer, String>) null));
    }

    @Test
    public void testFilterThenLimitPredicate() {
        Predicate<Integer> predicate = Fn.filterThenLimit(i -> i % 2 == 0, 2);

        assertFalse(predicate.test(1));
        assertTrue(predicate.test(2));
        assertFalse(predicate.test(3));
        assertTrue(predicate.test(4));
        assertFalse(predicate.test(6));

        Predicate<String> zeroLimitPredicate = Fn.filterThenLimit(s -> true, 0);
        assertFalse(zeroLimitPredicate.test("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit((Predicate) null, 1));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit(i -> true, -1));
    }

    @Test
    public void testFilterThenLimitBiPredicate() {
        BiPredicate<Integer, String> biPredicate = Fn.filterThenLimit((i, s) -> s.length() == i, 2);

        assertFalse(biPredicate.test(2, "abc"));
        assertTrue(biPredicate.test(3, "abc"));
        assertTrue(biPredicate.test(4, "test"));
        assertFalse(biPredicate.test(5, "hello"));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit((BiPredicate<Integer, String>) null, 1));

        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit((i, s) -> true, -1));
    }

    @Test
    public void testTimeLimitMillis() throws InterruptedException {
        Predicate<String> timeLimitPredicate = Fn.timeLimit(100);
        assertTrue(timeLimitPredicate.test("test1"));
        Thread.sleep(50);
        assertTrue(timeLimitPredicate.test("test2"));
        Thread.sleep(80);
        assertFalse(timeLimitPredicate.test("test3"));

        Predicate<Integer> zeroTimePredicate = Fn.timeLimit(0);
        assertFalse(zeroTimePredicate.test(1));

        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit(-1));
    }

    @Test
    public void testTimeLimitDuration() throws InterruptedException {
        Duration duration = Duration.ofMillis(100);
        Predicate<String> timeLimitPredicate = Fn.timeLimit(duration);
        assertTrue(timeLimitPredicate.test("test1"));
        Thread.sleep(200);
        assertFalse(timeLimitPredicate.test("test2"));

        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit(null));
    }

    @Test
    public void testIndexedFunction() {
        Function<String, Indexed<String>> indexedFunc = Fn.indexed();

        Indexed<String> first = indexedFunc.apply("a");
        assertEquals("a", first.value());
        assertEquals(0, first.index());

        Indexed<String> second = indexedFunc.apply("b");
        assertEquals("b", second.value());
        assertEquals(1, second.index());

        Indexed<String> third = indexedFunc.apply("c");
        assertEquals("c", third.value());
        assertEquals(2, third.index());
    }

    @Test
    public void testIndexedPredicate() {
        Predicate<String> indexedPredicate = Fn.indexed((index, value) -> index % 2 == 0);

        assertTrue(indexedPredicate.test("a"));
        assertFalse(indexedPredicate.test("b"));
        assertTrue(indexedPredicate.test("c"));
        assertFalse(indexedPredicate.test("d"));
    }

    @Test
    public void testSelectFirst() {
        BinaryOperator<String> selectFirst = Fn.selectFirst();
        assertEquals("first", selectFirst.apply("first", "second"));
        assertEquals(null, selectFirst.apply(null, "second"));
        assertEquals("first", selectFirst.apply("first", null));
    }

    @Test
    public void testSelectSecond() {
        BinaryOperator<String> selectSecond = Fn.selectSecond();
        assertEquals("second", selectSecond.apply("first", "second"));
        assertEquals("second", selectSecond.apply(null, "second"));
        assertEquals(null, selectSecond.apply("first", null));
    }

    @Test
    public void testMin() {
        BinaryOperator<Integer> min = Fn.min();
        assertEquals(Integer.valueOf(1), min.apply(1, 2));
        assertEquals(Integer.valueOf(1), min.apply(2, 1));
        assertEquals(Integer.valueOf(5), min.apply(5, 5));

        assertEquals(Integer.valueOf(1), min.apply(1, null));
        assertEquals(Integer.valueOf(1), min.apply(null, 1));
        assertNull(min.apply(null, null));
    }

    @Test
    public void testMinWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        BinaryOperator<String> minByLength = Fn.min(lengthComparator);

        assertEquals("ab", minByLength.apply("ab", "abc"));
        assertEquals("a", minByLength.apply("abc", "a"));
        assertEquals("test", minByLength.apply("test", "test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.min(null));
    }

    @Test
    public void testMinBy() {
        BinaryOperator<String> minByLength = Fn.minBy(N::len);

        assertEquals("ab", minByLength.apply("ab", "abc"));
        assertEquals("a", minByLength.apply("abc", "a"));

        assertEquals(null, minByLength.apply("test", null));
        assertEquals(null, minByLength.apply(null, "test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.minBy(null));
    }

    @Test
    public void testMinByKey() {
        BinaryOperator<Map.Entry<Integer, String>> minByKey = Fn.minByKey();

        Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<>(1, "one");
        Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<>(2, "two");

        assertEquals(entry1, minByKey.apply(entry1, entry2));
        assertEquals(entry1, minByKey.apply(entry2, entry1));
    }

    @Test
    public void testMinByValue() {
        BinaryOperator<Map.Entry<String, Integer>> minByValue = Fn.minByValue();

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("one", 1);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("two", 2);

        assertEquals(entry1, minByValue.apply(entry1, entry2));
        assertEquals(entry1, minByValue.apply(entry2, entry1));
    }

    @Test
    public void testMax() {
        BinaryOperator<Integer> max = Fn.max();
        assertEquals(Integer.valueOf(2), max.apply(1, 2));
        assertEquals(Integer.valueOf(2), max.apply(2, 1));
        assertEquals(Integer.valueOf(5), max.apply(5, 5));

        assertEquals(Integer.valueOf(1), max.apply(1, null));
        assertEquals(Integer.valueOf(1), max.apply(null, 1));
        assertNull(max.apply(null, null));
    }

    @Test
    public void testMaxWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        BinaryOperator<String> maxByLength = Fn.max(lengthComparator);

        assertEquals("abc", maxByLength.apply("ab", "abc"));
        assertEquals("abc", maxByLength.apply("abc", "a"));
        assertEquals("test", maxByLength.apply("test", "test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.max(null));
    }

    @Test
    public void testMaxBy() {
        BinaryOperator<String> maxByLength = Fn.maxBy(N::len);

        assertEquals("abc", maxByLength.apply("ab", "abc"));
        assertEquals("abc", maxByLength.apply("abc", "a"));

        assertEquals("test", maxByLength.apply("test", null));
        assertEquals("test", maxByLength.apply(null, "test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.maxBy(null));
    }

    @Test
    public void testMaxByKey() {
        BinaryOperator<Map.Entry<Integer, String>> maxByKey = Fn.maxByKey();

        Map.Entry<Integer, String> entry1 = new AbstractMap.SimpleEntry<>(1, "one");
        Map.Entry<Integer, String> entry2 = new AbstractMap.SimpleEntry<>(2, "two");

        assertEquals(entry2, maxByKey.apply(entry1, entry2));
        assertEquals(entry2, maxByKey.apply(entry2, entry1));
    }

    @Test
    public void testMaxByValue() {
        BinaryOperator<Map.Entry<String, Integer>> maxByValue = Fn.maxByValue();

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("one", 1);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("two", 2);

        assertEquals(entry2, maxByValue.apply(entry1, entry2));
        assertEquals(entry2, maxByValue.apply(entry2, entry1));
    }

    @Test
    public void testCompareTo() {
        Function<Integer, Integer> compareToFive = Fn.compareTo(5);

        assertTrue(compareToFive.apply(3) < 0);
        assertEquals(0, compareToFive.apply(5));
        assertTrue(compareToFive.apply(7) > 0);

        assertTrue(compareToFive.apply(null) < 0);
    }

    @Test
    public void testCompareToWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        Function<String, Integer> compareToHello = Fn.compareTo("hello", lengthComparator);

        assertTrue(compareToHello.apply("hi") < 0);
        assertEquals(0, compareToHello.apply("world"));
        assertTrue(compareToHello.apply("goodbye") > 0);

        Function<Integer, Integer> compareToFive = Fn.compareTo(5, null);
        assertEquals(0, compareToFive.apply(5));
    }

    @Test
    public void testCompare() {
        BiFunction<Integer, Integer, Integer> compare = Fn.compare();

        assertTrue(compare.apply(3, 5) < 0);
        assertEquals(0, compare.apply(5, 5));
        assertTrue(compare.apply(7, 5) > 0);
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(String::length);
        BiFunction<String, String, Integer> compareByLength = Fn.compare(lengthComparator);

        assertTrue(compareByLength.apply("hi", "hello") < 0);
        assertEquals(0, compareByLength.apply("hello", "world"));
        assertTrue(compareByLength.apply("goodbye", "hi") > 0);

        BiFunction<Integer, Integer, Integer> compareNatural = Fn.compare(null);
        assertEquals(0, compareNatural.apply(5, 5));
    }

    @Test
    public void testFutureGetOrDefaultOnError() throws Exception {
        CompletableFuture<String> successFuture = CompletableFuture.completedFuture("success");
        Function<Future<String>, String> getOrDefault = Fn.futureGetOrDefaultOnError("default");
        assertEquals("success", getOrDefault.apply(successFuture));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("error"));
        assertEquals("default", getOrDefault.apply(failedFuture));
    }

    @Test
    public void testFutureGet() throws Exception {
        CompletableFuture<String> successFuture = CompletableFuture.completedFuture("success");
        Function<Future<String>, String> futureGet = Fn.futureGet();
        assertEquals("success", futureGet.apply(successFuture));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("error"));
        assertThrows(RuntimeException.class, () -> futureGet.apply(failedFuture));
    }

    @Test
    public void testFromSupplier() {
        java.util.function.Supplier<String> javaSupplier = () -> "test";
        Supplier<String> abacusSupplier = Fn.from(javaSupplier);
        assertEquals("test", abacusSupplier.get());

        Supplier<String> originalSupplier = () -> "original";
        assertSame(originalSupplier, Fn.from(originalSupplier));
    }

    @Test
    public void testFromIntFunction() {
        java.util.function.IntFunction<String> javaIntFunction = i -> "value" + i;
        IntFunction<String> abacusIntFunction = Fn.from(javaIntFunction);
        assertEquals("value5", abacusIntFunction.apply(5));
    }

    @Test
    public void testFromPredicate() {
        java.util.function.Predicate<String> javaPredicate = s -> s.length() > 3;
        Predicate<String> abacusPredicate = Fn.from(javaPredicate);
        assertTrue(abacusPredicate.test("hello"));
        assertFalse(abacusPredicate.test("hi"));
    }

    @Test
    public void testFromBiPredicate() {
        java.util.function.BiPredicate<String, Integer> javaBiPredicate = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> abacusBiPredicate = Fn.from(javaBiPredicate);
        assertTrue(abacusBiPredicate.test("hello", 5));
        assertFalse(abacusBiPredicate.test("hello", 3));
    }

    @Test
    public void testFromConsumer() {
        List<String> list = new ArrayList<>();
        java.util.function.Consumer<String> javaConsumer = list::add;
        Consumer<String> abacusConsumer = Fn.from(javaConsumer);
        abacusConsumer.accept("test");
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
    }

    @Test
    public void testFromBiConsumer() {
        Map<String, Integer> map = new HashMap<>();
        java.util.function.BiConsumer<String, Integer> javaBiConsumer = map::put;
        BiConsumer<String, Integer> abacusBiConsumer = Fn.from(javaBiConsumer);
        abacusBiConsumer.accept("key", 5);
        assertEquals(1, map.size());
        assertEquals(5, map.get("key"));
    }

    @Test
    public void testFromFunction() {
        java.util.function.Function<String, Integer> javaFunction = String::length;
        Function<String, Integer> abacusFunction = Fn.from(javaFunction);
        assertEquals(5, abacusFunction.apply("hello"));
    }

    @Test
    public void testFromBiFunction() {
        java.util.function.BiFunction<String, String, String> javaBiFunction = String::concat;
        BiFunction<String, String, String> abacusBiFunction = Fn.from(javaBiFunction);
        assertEquals("helloworld", abacusBiFunction.apply("hello", "world"));
    }

    @Test
    public void testFromUnaryOperator() {
        java.util.function.UnaryOperator<String> javaUnaryOp = s -> s.toUpperCase();
        UnaryOperator<String> abacusUnaryOp = Fn.from(javaUnaryOp);
        assertEquals("HELLO", abacusUnaryOp.apply("hello"));
    }

    @Test
    public void testFromBinaryOperator() {
        java.util.function.BinaryOperator<Integer> javaBinaryOp = Integer::sum;
        BinaryOperator<Integer> abacusBinaryOp = Fn.from(javaBinaryOp);
        assertEquals(8, abacusBinaryOp.apply(3, 5));
    }

    @Test
    public void testSSupplier() {
        Supplier<String> supplier = () -> "test";
        assertSame(supplier, Fn.s(supplier));
    }

    @Test
    public void testSWithFunction() {
        Function<String, Integer> func = String::length;
        Supplier<Integer> supplier = Fn.s("hello", func);
        assertEquals(5, supplier.get());
    }

    @Test
    public void testPPredicate() {
        Predicate<String> predicate = s -> s.length() > 3;
        assertSame(predicate, Fn.p(predicate));
    }

    @Test
    public void testPWithBiPredicate() {
        java.util.function.BiPredicate<String, String> biPred = String::startsWith;
        Predicate<String> predicate = Fn.p("hello", biPred);
        assertTrue(predicate.test("he"));
        assertFalse(predicate.test("wo"));

        assertThrows(IllegalArgumentException.class, () -> Fn.p("test", (java.util.function.BiPredicate<String, String>) null));
    }

    @Test
    public void testPWithTriPredicate() {
        TriPredicate<String, Integer, Integer> triPred = (s, start, end) -> s.substring(start, end).equals("ell");
        Predicate<Integer> predicate = Fn.p("hello", 1, triPred);
        assertTrue(predicate.test(4));
        assertFalse(predicate.test(3));

        assertThrows(IllegalArgumentException.class, () -> Fn.p("a", "b", (TriPredicate<String, String, String>) null));
    }

    @Test
    public void testPBiPredicate() {
        BiPredicate<String, Integer> biPredicate = (s, i) -> s.length() == i;
        assertSame(biPredicate, Fn.p(biPredicate));
    }

    @Test
    public void testPBiPredicateWithTriPredicate() {
        TriPredicate<String, String, Integer> triPred = (prefix, s, len) -> s.startsWith(prefix) && s.length() == len;
        BiPredicate<String, Integer> biPredicate = Fn.p("he", triPred);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("world", 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.p("test", (TriPredicate<String, String, String>) null));
    }

    @Test
    public void testPTriPredicate() {
        TriPredicate<String, Integer, Boolean> triPredicate = (s, i, b) -> b && s.length() == i;
        assertSame(triPredicate, Fn.p(triPredicate));
    }

    @Test
    public void testCConsumer() {
        Consumer<String> consumer = System.out::println;
        assertSame(consumer, Fn.c(consumer));
    }

    @Test
    public void testCWithBiConsumer() {
        List<String> list = new ArrayList<>();
        java.util.function.BiConsumer<List<String>, String> biConsumer = List::add;
        Consumer<String> consumer = Fn.c(list, biConsumer);
        consumer.accept("test");
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c("test", (java.util.function.BiConsumer<String, String>) null));
    }

    @Test
    public void testCWithTriConsumer() {
        StringBuilder sb = new StringBuilder();
        TriConsumer<StringBuilder, String, String> triConsumer = (builder, s1, s2) -> builder.append(s1).append(s2);
        Consumer<String> consumer = Fn.c(sb, "hello", triConsumer);
        consumer.accept(" world");
        assertEquals("hello world", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.c("a", "b", (TriConsumer<String, String, String>) null));
    }

    @Test
    public void testCBiConsumer() {
        BiConsumer<String, Integer> biConsumer = (s, i) -> {
        };
        assertSame(biConsumer, Fn.c(biConsumer));
    }

    @Test
    public void testCBiConsumerWithTriConsumer() {
        StringBuilder sb = new StringBuilder();
        TriConsumer<StringBuilder, String, String> triConsumer = (builder, s1, s2) -> builder.append(s1).append(s2);
        BiConsumer<String, String> biConsumer = Fn.c(sb, triConsumer);
        biConsumer.accept("hello", " world");
        assertEquals("hello world", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.c("test", (TriConsumer<String, String, String>) null));
    }

    @Test
    public void testCTriConsumer() {
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
        };
        assertSame(triConsumer, Fn.c(triConsumer));
    }

    @Test
    public void testFFunction() {
        Function<String, Integer> function = String::length;
        assertSame(function, Fn.f(function));
    }

    @Test
    public void testFWithBiFunction() {
        java.util.function.BiFunction<String, String, String> biFunction = String::concat;
        Function<String, String> function = Fn.f("Hello ", biFunction);
        assertEquals("Hello World", function.apply("World"));

        assertThrows(IllegalArgumentException.class, () -> Fn.f("test", (java.util.function.BiFunction<String, String, String>) null));
    }

    @Test
    public void testFWithTriFunction() {
        TriFunction<String, Integer, Integer, String> triFunction = (s, start, end) -> s.substring(start, end);
        Function<Integer, String> function = Fn.f("hello", 0, triFunction);
        assertEquals("hel", function.apply(3));

        assertThrows(IllegalArgumentException.class, () -> Fn.f("a", "b", (TriFunction<String, String, String, String>) null));
    }

    @Test
    public void testFBiFunction() {
        BiFunction<String, Integer, Character> biFunction = (s, i) -> s.charAt(i);
        assertSame(biFunction, Fn.f(biFunction));
    }

    @Test
    public void testFBiFunctionWithTriFunction() {
        TriFunction<String, Integer, Integer, String> triFunction = (s, start, end) -> s.substring(start, end);
        BiFunction<Integer, Integer, String> biFunction = Fn.f("hello world", triFunction);
        assertEquals("hello", biFunction.apply(0, 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.f("test", (TriFunction<String, String, String, String>) null));
    }

    @Test
    public void testFTriFunction() {
        TriFunction<String, Integer, Boolean, String> triFunction = (s, i, b) -> b ? s.substring(i) : s;
        assertSame(triFunction, Fn.f(triFunction));
    }

    @Test
    public void testOUnaryOperator() {
        UnaryOperator<String> unaryOp = String::toUpperCase;
        assertSame(unaryOp, Fn.o(unaryOp));

        assertThrows(IllegalArgumentException.class, () -> Fn.o((UnaryOperator<String>) null));
    }

    @Test
    public void testOBinaryOperator() {
        BinaryOperator<Integer> binaryOp = Integer::sum;
        assertSame(binaryOp, Fn.o(binaryOp));

        assertThrows(IllegalArgumentException.class, () -> Fn.o((BinaryOperator<String>) null));
    }

    @Test
    public void testSsThrowableSupplier() {
        Throwables.Supplier<String, IOException> throwableSupplier = () -> "test";
        Supplier<String> supplier = Fn.ss(throwableSupplier);
        assertEquals("test", supplier.get());

        Throwables.Supplier<String, IOException> exceptionSupplier = () -> {
            throw new IOException("error");
        };
        Supplier<String> errorSupplier = Fn.ss(exceptionSupplier);
        assertThrows(RuntimeException.class, errorSupplier::get);

        assertThrows(IllegalArgumentException.class, () -> Fn.ss((Throwables.Supplier<String, Exception>) null));
    }

    @Test
    public void testSsWithFunction() {
        Throwables.Function<String, Integer, IOException> throwableFunc = String::length;
        Supplier<Integer> supplier = Fn.ss("hello", throwableFunc);
        assertEquals(5, supplier.get());

        Throwables.Function<String, Integer, IOException> exceptionFunc = s -> {
            throw new IOException("error");
        };
        Supplier<Integer> errorSupplier = Fn.ss("test", exceptionFunc);
        assertThrows(RuntimeException.class, errorSupplier::get);

        assertThrows(IllegalArgumentException.class, () -> Fn.ss("test", (Throwables.Function<String, String, Exception>) null));
    }

    @Test
    public void testPpThrowablePredicate() {
        Throwables.Predicate<String, IOException> throwablePred = s -> s.length() > 3;
        Predicate<String> predicate = Fn.pp(throwablePred);
        assertTrue(predicate.test("hello"));
        assertFalse(predicate.test("hi"));

        Throwables.Predicate<String, IOException> exceptionPred = s -> {
            throw new IOException("error");
        };
        Predicate<String> errorPredicate = Fn.pp(exceptionPred);
        assertThrows(RuntimeException.class, () -> errorPredicate.test("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp((Throwables.Predicate<String, Exception>) null));
    }

    @Test
    public void testPpWithBiPredicate() {
        Throwables.BiPredicate<String, String, IOException> throwableBiPred = String::startsWith;
        Predicate<String> predicate = Fn.pp("hello", throwableBiPred);
        assertTrue(predicate.test("he"));
        assertFalse(predicate.test("wo"));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp("test", (Throwables.BiPredicate<String, String, Exception>) null));
    }

    @Test
    public void testPpWithTriPredicate() {
        Throwables.TriPredicate<String, Integer, Integer, IOException> throwableTriPred = (s, start, end) -> s.substring(start, end).equals("ell");
        Predicate<Integer> predicate = Fn.pp("hello", 1, throwableTriPred);
        assertTrue(predicate.test(4));
        assertFalse(predicate.test(3));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp("a", "b", (Throwables.TriPredicate<String, String, String, Exception>) null));
    }

    @Test
    public void testPpThrowableBiPredicate() {
        Throwables.BiPredicate<String, Integer, IOException> throwableBiPred = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> biPredicate = Fn.pp(throwableBiPred);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("hello", 3));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp((Throwables.BiPredicate<String, String, Exception>) null));
    }

    @Test
    public void testPpBiPredicateWithTriPredicate() {
        Throwables.TriPredicate<String, String, Integer, IOException> throwableTriPred = (prefix, s, len) -> s.startsWith(prefix) && s.length() == len;
        BiPredicate<String, Integer> biPredicate = Fn.pp("he", throwableTriPred);
        assertTrue(biPredicate.test("hello", 5));
        assertFalse(biPredicate.test("world", 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp("test", (Throwables.TriPredicate<String, String, String, Exception>) null));
    }

    @Test
    public void testPpThrowableTriPredicate() {
        Throwables.TriPredicate<String, Integer, Boolean, IOException> throwableTriPred = (s, i, b) -> b && s.length() == i;
        TriPredicate<String, Integer, Boolean> triPredicate = Fn.pp(throwableTriPred);
        assertTrue(triPredicate.test("hello", 5, true));
        assertFalse(triPredicate.test("hello", 5, false));

        assertThrows(IllegalArgumentException.class, () -> Fn.pp((Throwables.TriPredicate<String, String, String, Exception>) null));
    }

    @Test
    public void testCcThrowableConsumer() {
        List<String> list = new ArrayList<>();
        Throwables.Consumer<String, IOException> throwableConsumer = list::add;
        Consumer<String> consumer = Fn.cc(throwableConsumer);
        consumer.accept("test");
        assertEquals(1, list.size());

        Throwables.Consumer<String, IOException> exceptionConsumer = s -> {
            throw new IOException("error");
        };
        Consumer<String> errorConsumer = Fn.cc(exceptionConsumer);
        assertThrows(RuntimeException.class, () -> errorConsumer.accept("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.cc((Throwables.Consumer<String, Exception>) null));
    }

    @Test
    public void testCcWithBiConsumer() {
        List<String> list = new ArrayList<>();
        Throwables.BiConsumer<List<String>, String, IOException> throwableBiConsumer = List::add;
        Consumer<String> consumer = Fn.cc(list, throwableBiConsumer);
        consumer.accept("test");
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.cc("test", (Throwables.BiConsumer<String, String, Exception>) null));
    }

    @Test
    public void testCcWithTriConsumer() {
        StringBuilder sb = new StringBuilder();
        Throwables.TriConsumer<StringBuilder, String, String, IOException> throwableTriConsumer = (builder, s1, s2) -> builder.append(s1).append(s2);
        Consumer<String> consumer = Fn.cc(sb, " world", throwableTriConsumer);
        consumer.accept("hello");
        assertEquals(" worldhello", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.cc("a", "b", (Throwables.TriConsumer<String, String, String, Exception>) null));
    }

    @Test
    public void testCcThrowableBiConsumer() {
        Map<String, Integer> map = new HashMap<>();
        Throwables.BiConsumer<String, Integer, IOException> throwableBiConsumer = map::put;
        BiConsumer<String, Integer> biConsumer = Fn.cc(throwableBiConsumer);
        biConsumer.accept("key", 5);
        assertEquals(1, map.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.cc((Throwables.BiConsumer<String, String, Exception>) null));
    }

    @Test
    public void testCcBiConsumerWithTriConsumer() {
        StringBuilder sb = new StringBuilder();
        Throwables.TriConsumer<StringBuilder, String, String, IOException> throwableTriConsumer = (builder, s1, s2) -> builder.append(s1).append(s2);
        BiConsumer<String, String> biConsumer = Fn.cc(sb, throwableTriConsumer);
        biConsumer.accept("hello", " world");
        assertEquals("hello world", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.cc("test", (Throwables.TriConsumer<String, String, String, Exception>) null));
    }

    @Test
    public void testCcThrowableTriConsumer() {
        Throwables.TriConsumer<String, Integer, Boolean, IOException> throwableTriConsumer = (s, i, b) -> {
        };
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.cc(throwableTriConsumer);
        assertNotNull(triConsumer);

        assertThrows(IllegalArgumentException.class, () -> Fn.cc((Throwables.TriConsumer<String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableFunction() {
        Throwables.Function<String, Integer, IOException> throwableFunc = String::length;
        Function<String, Integer> function = Fn.ff(throwableFunc);
        assertEquals(5, function.apply("hello"));

        Throwables.Function<String, Integer, IOException> exceptionFunc = s -> {
            throw new IOException("error");
        };
        Function<String, Integer> errorFunction = Fn.ff(exceptionFunc);
        assertThrows(RuntimeException.class, () -> errorFunction.apply("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.Function<String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableFunctionWithDefault() {
        Throwables.Function<String, Integer, IOException> throwableFunc = String::length;
        Function<String, Integer> function = Fn.ff(throwableFunc, -1);
        assertEquals(5, function.apply("hello"));

        Throwables.Function<String, Integer, IOException> exceptionFunc = s -> {
            throw new IOException("error");
        };
        Function<String, Integer> errorFunction = Fn.ff(exceptionFunc, -1);
        assertEquals(-1, errorFunction.apply("test"));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.Function<String, Integer, Exception>) null, -1));
    }

    @Test
    public void testFfWithBiFunction() {
        Throwables.BiFunction<String, String, String, IOException> throwableBiFunc = String::concat;
        Function<String, String> function = Fn.ff("Hello ", throwableBiFunc);
        assertEquals("Hello World", function.apply("World"));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff("test", (Throwables.BiFunction<String, String, String, Exception>) null));
    }

    @Test
    public void testFfWithTriFunction() {
        Throwables.TriFunction<String, Integer, Integer, String, IOException> throwableTriFunc = (s, start, end) -> s.substring(start, end);
        Function<Integer, String> function = Fn.ff("hello", 0, throwableTriFunc);
        assertEquals("hel", function.apply(3));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff("a", "b", (Throwables.TriFunction<String, String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableBiFunction() {
        Throwables.BiFunction<String, Integer, Character, IOException> throwableBiFunc = (s, i) -> s.charAt(i);
        BiFunction<String, Integer, Character> biFunction = Fn.ff(throwableBiFunc);
        assertEquals('e', biFunction.apply("hello", 1));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.BiFunction<String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableBiFunctionWithDefault() {
        Throwables.BiFunction<String, Integer, Character, IOException> throwableBiFunc = (s, i) -> s.charAt(i);
        BiFunction<String, Integer, Character> biFunction = Fn.ff(throwableBiFunc, '?');
        assertEquals('e', biFunction.apply("hello", 1));

        Throwables.BiFunction<String, Integer, Character, IOException> exceptionFunc = (s, i) -> {
            throw new IOException("error");
        };
        BiFunction<String, Integer, Character> errorFunction = Fn.ff(exceptionFunc, '?');
        assertEquals('?', errorFunction.apply("test", 0));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.BiFunction<String, Integer, Character, Exception>) null, '?'));
    }

    @Test
    public void testFfBiFunctionWithTriFunction() {
        Throwables.TriFunction<String, Integer, Integer, String, IOException> throwableTriFunc = (s, start, end) -> s.substring(start, end);
        BiFunction<Integer, Integer, String> biFunction = Fn.ff("hello world", throwableTriFunc);
        assertEquals("hello", biFunction.apply(0, 5));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff("test", (Throwables.TriFunction<String, String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableTriFunction() {
        Throwables.TriFunction<String, Integer, Boolean, String, IOException> throwableTriFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.ff(throwableTriFunc);
        assertEquals("llo", triFunction.apply("hello", 2, true));
        assertEquals("hello", triFunction.apply("hello", 2, false));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.TriFunction<String, String, String, String, Exception>) null));
    }

    @Test
    public void testFfThrowableTriFunctionWithDefault() {
        Throwables.TriFunction<String, Integer, Boolean, String, IOException> throwableTriFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.ff(throwableTriFunc, "default");
        assertEquals("llo", triFunction.apply("hello", 2, true));

        Throwables.TriFunction<String, Integer, Boolean, String, IOException> exceptionFunc = (s, i, b) -> {
            throw new IOException("error");
        };
        TriFunction<String, Integer, Boolean, String> errorFunction = Fn.ff(exceptionFunc, "default");
        assertEquals("default", errorFunction.apply("test", 0, true));

        assertThrows(IllegalArgumentException.class, () -> Fn.ff((Throwables.TriFunction<String, Integer, Boolean, String, Exception>) null, "default"));
    }

    @Test
    public void testSpPredicate() {
        Object mutex = new Object();
        AtomicInteger counter = new AtomicInteger(0);
        java.util.function.Predicate<String> pred = s -> {
            counter.incrementAndGet();
            return s.length() > 3;
        };

        Predicate<String> syncPred = Fn.sp(mutex, pred);
        assertTrue(syncPred.test("hello"));
        assertFalse(syncPred.test("hi"));
        assertEquals(2, counter.get());

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(null, pred));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(mutex, (java.util.function.Predicate<String>) null));
    }

    @Test
    public void testSpWithBiPredicate() {
        Object mutex = new Object();
        java.util.function.BiPredicate<String, String> biPred = String::startsWith;
        Predicate<String> syncPred = Fn.sp(mutex, "hello", biPred);
        assertTrue(syncPred.test("he"));
        assertFalse(syncPred.test("wo"));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(null, "test", biPred));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(mutex, "test", (java.util.function.BiPredicate<String, String>) null));
    }

    @Test
    public void testSpBiPredicate() {
        Object mutex = new Object();
        java.util.function.BiPredicate<String, Integer> biPred = (s, i) -> s.length() == i;
        BiPredicate<String, Integer> syncBiPred = Fn.sp(mutex, biPred);
        assertTrue(syncBiPred.test("hello", 5));
        assertFalse(syncBiPred.test("hello", 3));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(null, biPred));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(mutex, (java.util.function.BiPredicate<String, Integer>) null));
    }

    @Test
    public void testSpTriPredicate() {
        Object mutex = new Object();
        TriPredicate<String, Integer, Boolean> triPred = (s, i, b) -> b && s.length() == i;
        TriPredicate<String, Integer, Boolean> syncTriPred = Fn.sp(mutex, triPred);
        assertTrue(syncTriPred.test("hello", 5, true));
        assertFalse(syncTriPred.test("hello", 5, false));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(null, triPred));

        assertThrows(IllegalArgumentException.class, () -> Fn.sp(mutex, (TriPredicate<String, String, String>) null));
    }

    @Test
    public void testScConsumer() {
        Object mutex = new Object();
        List<String> list = new ArrayList<>();
        java.util.function.Consumer<String> consumer = list::add;
        Consumer<String> syncConsumer = Fn.sc(mutex, consumer);
        syncConsumer.accept("test");
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(null, consumer));

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(mutex, (java.util.function.Consumer<String>) null));
    }

    @Test
    public void testScWithBiConsumer() {
        Object mutex = new Object();
        List<String> list = new ArrayList<>();
        java.util.function.BiConsumer<List<String>, String> biConsumer = List::add;
        Consumer<String> syncConsumer = Fn.sc(mutex, list, biConsumer);
        syncConsumer.accept("test");
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(null, list, biConsumer));

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(mutex, "test", (java.util.function.BiConsumer<String, String>) null));
    }

    @Test
    public void testScBiConsumer() {
        Object mutex = new Object();
        Map<String, Integer> map = new HashMap<>();
        java.util.function.BiConsumer<String, Integer> biConsumer = map::put;
        BiConsumer<String, Integer> syncBiConsumer = Fn.sc(mutex, biConsumer);
        syncBiConsumer.accept("key", 5);
        assertEquals(1, map.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(null, biConsumer));

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(mutex, (java.util.function.BiConsumer<String, Integer>) null));
    }

    @Test
    public void testScTriConsumer() {
        Object mutex = new Object();
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
        };
        TriConsumer<String, Integer, Boolean> syncTriConsumer = Fn.sc(mutex, triConsumer);
        assertNotNull(syncTriConsumer);

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(null, triConsumer));

        assertThrows(IllegalArgumentException.class, () -> Fn.sc(mutex, (TriConsumer<String, String, String>) null));
    }

    @Test
    public void testSfFunction() {
        Object mutex = new Object();
        java.util.function.Function<String, Integer> func = String::length;
        Function<String, Integer> syncFunc = Fn.sf(mutex, func);
        assertEquals(5, syncFunc.apply("hello"));

        assertThrows(IllegalArgumentException.class, () -> Fn.sf(null, func));

        assertThrows(IllegalArgumentException.class, () -> Fn.sf(mutex, (java.util.function.Function<String, Integer>) null));
    }

    @Test
    public void testSfWithBiFunction() {
        Object mutex = new Object();
        java.util.function.BiFunction<String, String, String> biFunc = String::concat;
        Function<String, String> syncFunc = Fn.sf(mutex, "Hello ", biFunc);
        assertEquals("Hello World", syncFunc.apply("World"));

        assertThrows(IllegalArgumentException.class, () -> Fn.sf(null, "test", biFunc));

        assertThrows(IllegalArgumentException.class, () -> Fn.sf(mutex, "test", (java.util.function.BiFunction<String, String, String>) null));
    }

    @Test
    public void testSfBiFunction() {
        Object mutex = new Object();
        java.util.function.BiFunction<String, Integer, Character> biFunc = (s, i) -> s.charAt(i);
        BiFunction<String, Integer, Character> syncBiFunc = Fn.sf(mutex, biFunc);
        assertEquals('e', syncBiFunc.apply("hello", 1));

        assertThrows(IllegalArgumentException.class, () -> Fn.sf(null, biFunc));

        assertThrows(IllegalArgumentException.class, () -> Fn.sf(mutex, (java.util.function.BiFunction<String, Integer, Character>) null));
    }

    @Test
    public void testSfTriFunction() {
        Object mutex = new Object();
        TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriFunction<String, Integer, Boolean, String> syncTriFunc = Fn.sf(mutex, triFunc);
        assertEquals("llo", syncTriFunc.apply("hello", 2, true));
        assertEquals("hello", syncTriFunc.apply("hello", 2, false));
    }

    @Test
    public void testC2fConsumer() {
        List<String> list = new ArrayList<>();
        java.util.function.Consumer<String> consumer = list::add;
        Function<String, Void> function = Fn.c2f(consumer);
        assertNull(function.apply("test"));
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.Consumer<String>) null));
    }

    @Test
    public void testC2fConsumerWithReturn() {
        List<String> list = new ArrayList<>();
        java.util.function.Consumer<String> consumer = list::add;
        Function<String, Integer> function = Fn.c2f(consumer, 42);
        assertEquals(42, function.apply("test"));
        assertEquals(1, list.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.Consumer<String>) null, 42));
    }

    @Test
    public void testC2fBiConsumer() {
        Map<String, Integer> map = new HashMap<>();
        java.util.function.BiConsumer<String, Integer> biConsumer = map::put;
        BiFunction<String, Integer, Void> biFunction = Fn.c2f(biConsumer);
        assertNull(biFunction.apply("key", 5));
        assertEquals(1, map.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.BiConsumer<String, Integer>) null));
    }

    @Test
    public void testC2fBiConsumerWithReturn() {
        Map<String, Integer> map = new HashMap<>();
        java.util.function.BiConsumer<String, Integer> biConsumer = map::put;
        BiFunction<String, Integer, String> biFunction = Fn.c2f(biConsumer, "done");
        assertEquals("done", biFunction.apply("key", 5));
        assertEquals(1, map.size());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((java.util.function.BiConsumer<String, Integer>) null, "done"));
    }

    @Test
    public void testC2fTriConsumer() {
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
        };
        TriFunction<String, Integer, Boolean, Void> triFunction = Fn.c2f(triConsumer);
        assertNull(triFunction.apply("test", 1, true));

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((TriConsumer<String, Integer, Boolean>) null));
    }

    @Test
    public void testC2fTriConsumerWithReturn() {
        StringBuilder sb = new StringBuilder();
        TriConsumer<String, Integer, Boolean> triConsumer = (s, i, b) -> {
            if (b)
                sb.append(s).append(i);
        };
        TriFunction<String, Integer, Boolean, String> triFunction = Fn.c2f(triConsumer, "result");
        assertEquals("result", triFunction.apply("test", 1, true));
        assertEquals("test1", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2f((TriConsumer<String, Integer, Boolean>) null, "result"));
    }

    @Test
    public void testF2cFunction() {
        java.util.function.Function<String, Integer> func = String::length;
        Consumer<String> consumer = Fn.f2c(func);
        consumer.accept("hello");

        assertThrows(IllegalArgumentException.class, () -> Fn.f2c((java.util.function.Function<String, Integer>) null));
    }

    @Test
    public void testF2cBiFunction() {
        java.util.function.BiFunction<String, String, String> biFunc = String::concat;
        BiConsumer<String, String> biConsumer = Fn.f2c(biFunc);
        biConsumer.accept("hello", "world");

        assertThrows(IllegalArgumentException.class, () -> Fn.f2c((java.util.function.BiFunction<String, String, String>) null));
    }

    @Test
    public void testF2cTriFunction() {
        TriFunction<String, Integer, Boolean, String> triFunc = (s, i, b) -> b ? s.substring(i) : s;
        TriConsumer<String, Integer, Boolean> triConsumer = Fn.f2c(triFunc);
        triConsumer.accept("hello", 2, true);

        assertThrows(IllegalArgumentException.class, () -> Fn.f2c((TriFunction<String, Integer, Boolean, String>) null));
    }

    @Test
    public void testRrThrowableRunnable() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Throwables.Runnable<IOException> throwableRunnable = () -> executed.set(true);
        Runnable runnable = Fn.rr(throwableRunnable);
        runnable.run();
        assertTrue(executed.get());

        Throwables.Runnable<IOException> exceptionRunnable = () -> {
            throw new IOException("error");
        };
        Runnable errorRunnable = Fn.rr(exceptionRunnable);
        assertThrows(RuntimeException.class, errorRunnable::run);
    }

    @Test
    public void testCcThrowableCallable() throws Exception {
        Throwables.Callable<String, IOException> throwableCallable = () -> "test";
        Callable<String> callable = Fn.cc(throwableCallable);
        assertEquals("test", callable.call());

        Throwables.Callable<String, IOException> exceptionCallable = () -> {
            throw new IOException("error");
        };
        Callable<String> errorCallable = Fn.cc(exceptionCallable);
        assertThrows(RuntimeException.class, errorCallable::call);
    }

    @Test
    public void testR() {
        Runnable runnable = () -> {
        };
        assertSame(runnable, Fn.jr(runnable));

        assertThrows(IllegalArgumentException.class, () -> Fn.r(null));
    }

    @Test
    public void testC() {
        Callable<String> callable = () -> "test";
        assertSame(callable, Fn.jc(callable));

        assertThrows(IllegalArgumentException.class, () -> Fn.jc(null));
    }

    @Test
    public void testJr() {
        java.lang.Runnable javaRunnable = () -> {
        };
        assertSame(javaRunnable, Fn.jr(javaRunnable));

        assertThrows(IllegalArgumentException.class, () -> Fn.jr(null));
    }

    @Test
    public void testJc() {
        java.util.concurrent.Callable<String> javaCallable = () -> "test";
        assertSame(javaCallable, Fn.jc(javaCallable));

        assertThrows(IllegalArgumentException.class, () -> Fn.jc(null));
    }

    @Test
    public void testR2cRunnable() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        java.lang.Runnable runnable = () -> executed.set(true);
        Callable<Void> callable = Fn.r2c(runnable);
        assertNull(callable.call());
        assertTrue(executed.get());

        assertThrows(IllegalArgumentException.class, () -> Fn.r2c((java.lang.Runnable) null));
    }

    @Test
    public void testR2cRunnableWithReturn() throws Exception {
        AtomicBoolean executed = new AtomicBoolean(false);
        java.lang.Runnable runnable = () -> executed.set(true);
        Callable<String> callable = Fn.r2c(runnable, "done");
        assertEquals("done", callable.call());
        assertTrue(executed.get());

        assertThrows(IllegalArgumentException.class, () -> Fn.r2c((java.lang.Runnable) null, "done"));
    }

    @Test
    public void testC2r() {
        AtomicBoolean executed = new AtomicBoolean(false);
        Callable<String> callable = () -> {
            executed.set(true);
            return "test";
        };
        Runnable runnable = Fn.jc2r(callable);
        runnable.run();
        assertTrue(executed.get());

        assertThrows(IllegalArgumentException.class, () -> Fn.c2r(null));
    }

    @Test
    public void testJr2r() {
        AtomicBoolean executed = new AtomicBoolean(false);
        java.lang.Runnable javaRunnable = () -> executed.set(true);
        Runnable abacusRunnable = Fn.jr2r(javaRunnable);
        abacusRunnable.run();
        assertTrue(executed.get());

        Runnable originalRunnable = () -> {
        };
        assertNotSame(originalRunnable, Fn.jr2r(originalRunnable));

        assertThrows(IllegalArgumentException.class, () -> Fn.jr2r(null));
    }

    @Test
    public void testJc2c() throws Exception {
        java.util.concurrent.Callable<String> javaCallable = () -> "test";
        Callable<String> abacusCallable = Fn.jc2c(javaCallable);
        assertEquals("test", abacusCallable.call());

        Callable<String> originalCallable = () -> "original";
        assertNotSame(originalCallable, Fn.jc2c(originalCallable));

        java.util.concurrent.Callable<String> exceptionCallable = () -> {
            throw new Exception("error");
        };
        Callable<String> errorCallable = Fn.jc2c(exceptionCallable);
        assertThrows(RuntimeException.class, errorCallable::call);

        assertThrows(IllegalArgumentException.class, () -> Fn.jc2c(null));
    }

    @Test
    public void testJc2r() {
        AtomicBoolean executed = new AtomicBoolean(false);
        java.util.concurrent.Callable<String> callable = () -> {
            executed.set(true);
            return "test";
        };
        Runnable runnable = Fn.jc2r(callable);
        runnable.run();
        assertTrue(executed.get());

        java.util.concurrent.Callable<String> exceptionCallable = () -> {
            throw new Exception("error");
        };
        Runnable errorRunnable = Fn.jc2r(exceptionCallable);
        assertThrows(RuntimeException.class, errorRunnable::run);

        assertThrows(IllegalArgumentException.class, () -> Fn.jc2r(null));
    }

    @Test
    public void testThrowingMerger() {
        BinaryOperator<String> merger = Fn.throwingMerger();
        assertThrows(IllegalStateException.class, () -> merger.apply("a", "b"));
    }

    @Test
    public void testIgnoringMerger() {
        BinaryOperator<String> merger = Fn.ignoringMerger();
        assertEquals("first", merger.apply("first", "second"));
        assertEquals(null, merger.apply(null, "second"));
        assertEquals("first", merger.apply("first", null));
    }

    @Test
    public void testReplacingMerger() {
        BinaryOperator<String> merger = Fn.replacingMerger();
        assertEquals("second", merger.apply("first", "second"));
        assertEquals("second", merger.apply(null, "second"));
        assertEquals(null, merger.apply("first", null));
    }

    @Test
    public void testGetIfPresentOrElseNull() {
        Function<u.Optional<String>, String> getIfPresent = Fn.getIfPresentOrElseNull();

        u.Optional<String> present = u.Optional.of("value");
        assertEquals("value", getIfPresent.apply(present));

        u.Optional<String> empty = u.Optional.empty();
        assertNull(getIfPresent.apply(empty));
    }

    @Test
    public void testGetIfPresentOrElseNullJdk() {
        Function<java.util.Optional<String>, String> getIfPresent = Fn.getIfPresentOrElseNullJdk();

        java.util.Optional<String> present = java.util.Optional.of("value");
        assertEquals("value", getIfPresent.apply(present));

        java.util.Optional<String> empty = java.util.Optional.empty();
        assertNull(getIfPresent.apply(empty));
    }

    @Test
    public void testIsPresent() {
        Predicate<u.Optional<String>> isPresent = Fn.isPresent();

        u.Optional<String> present = u.Optional.of("value");
        assertTrue(isPresent.test(present));

        u.Optional<String> empty = u.Optional.empty();
        assertFalse(isPresent.test(empty));
    }

    @Test
    public void testIsPresentJdk() {
        Predicate<java.util.Optional<String>> isPresent = Fn.isPresentJdk();

        java.util.Optional<String> present = java.util.Optional.of("value");
        assertTrue(isPresent.test(present));

        java.util.Optional<String> empty = java.util.Optional.empty();
        assertFalse(isPresent.test(empty));
    }

    @Test
    public void testAlternate() {
        BiFunction<String, String, MergeResult> alternator = Fn.alternate();

        assertEquals(MergeResult.TAKE_FIRST, alternator.apply("a", "b"));
        assertEquals(MergeResult.TAKE_SECOND, alternator.apply("c", "d"));
        assertEquals(MergeResult.TAKE_FIRST, alternator.apply("e", "f"));
        assertEquals(MergeResult.TAKE_SECOND, alternator.apply("g", "h"));
    }

    @Test
    public void testComplexScenarios() {
        TriFunction<String, String, String, String> triFunc = (a, b, c) -> a + b + c;
        BiFunction<String, String, String> biFunc = Fn.f("Hello ", triFunc);
        Function<String, String> func = Fn.f("World", biFunc);
        assertEquals("Hello World!", func.apply("!"));

        BiPredicate<String, String> nullSafeBiPred = (s1, s2) -> s1 != null && s2 != null && s1.equals(s2);
        Predicate<String> pred = Fn.p(null, nullSafeBiPred);
        assertFalse(pred.test("test"));
        assertFalse(pred.test(null));
    }

    @Test
    public void testThreadSafetyScenarios() throws InterruptedException {
        final int threadCount = 10;
        final int iterationsPerThread = 100;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        final AtomicInteger totalPassed = new AtomicInteger(0);

        Predicate<Integer> limitPredicate = Fn.limitThenFilter(50, i -> true);

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < iterationsPerThread; j++) {
                    if (limitPredicate.test(j)) {
                        totalPassed.incrementAndGet();
                    }
                }
                latch.countDown();
            }).start();
        }

        latch.await();
        assertEquals(50, totalPassed.get());
    }

    @Test
    public void testExceptionHandlingInDepth() {
        Throwables.Function<String, String, IOException> throwableFunc = s -> {
            if (s.equals("error")) {
                throw new IOException("Test error");
            }
            return s.toUpperCase();
        };

        Function<String, String> safeFunc = Fn.ff(throwableFunc, "DEFAULT");
        assertEquals("HELLO", safeFunc.apply("hello"));
        assertEquals("DEFAULT", safeFunc.apply("error"));

        Function<String, String> propagatingFunc = Fn.ff(throwableFunc);
        assertEquals("HELLO", propagatingFunc.apply("hello"));
        assertThrows(RuntimeException.class, () -> propagatingFunc.apply("error"));
    }

    @Test
    public void testPerformanceCharacteristics() {
        Function<String, Indexed<String>> indexedFunc = Fn.indexed();
        List<Indexed<String>> results = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            results.add(indexedFunc.apply("item" + i));
        }

        for (int i = 0; i < results.size(); i++) {
            assertEquals(i, results.get(i).index());
            assertEquals("item" + i, results.get(i).value());
        }
    }

    @Test
    public void testMemoryLeakPrevention() throws InterruptedException {
        Predicate<String> timeLimitPred = Fn.timeLimit(50);
        assertTrue(timeLimitPred.test("test"));

        Thread.sleep(100);
        assertFalse(timeLimitPred.test("test"));

        for (int i = 0; i < 100; i++) {
            Fn.timeLimit(10);
        }
        Thread.sleep(50);
    }

    @Test
    public void testNullHandlingConsistency() {
        BinaryOperator<String> min = Fn.min();
        BinaryOperator<String> max = Fn.max();

        assertEquals("test", min.apply("test", null));
        assertEquals("test", min.apply(null, "test"));
        assertNull(min.apply(null, null));

        assertEquals("test", max.apply("test", null));
        assertEquals("test", max.apply(null, "test"));
        assertNull(max.apply(null, null));
    }

    @Test
    public void testTypeInferenceHelpers() {
        var supplier = Fn.s(() -> "test");
        assertEquals("test", supplier.get());

        var predicate = Fn.p((String s) -> s.length() > 3);
        assertTrue(predicate.test("hello"));

        var consumer = Fn.c((String s) -> {
        });
        consumer.accept("test");

        var function = Fn.f((String s) -> s.length());
        assertEquals(5, function.apply("hello"));
    }
}
