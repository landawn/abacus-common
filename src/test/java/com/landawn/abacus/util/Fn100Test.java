package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.UnaryOperator;

@Tag("new-test")
public class Fn100Test extends TestBase {

    @Test
    public void testMemoize() {
        final AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoize(() -> counter.incrementAndGet());

        assertEquals(Integer.valueOf(1), memoized.get());
        assertEquals(Integer.valueOf(1), memoized.get());
        assertEquals(Integer.valueOf(1), memoized.get());
        assertEquals(1, counter.get());
    }

    @Test
    public void testMemoizeWithExpiration() throws InterruptedException {
        final AtomicInteger counter = new AtomicInteger(0);
        Supplier<Integer> memoized = Fn.memoizeWithExpiration(() -> counter.incrementAndGet(), 100, TimeUnit.MILLISECONDS);

        assertEquals(Integer.valueOf(1), memoized.get());
        assertEquals(Integer.valueOf(1), memoized.get());

        Thread.sleep(150);

        assertEquals(Integer.valueOf(2), memoized.get());
        assertEquals(Integer.valueOf(2), memoized.get());
        assertEquals(2, counter.get());
    }

    @Test
    public void testMemoizeFunction() {
        final AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> memoized = Fn.memoize(str -> {
            counter.incrementAndGet();
            return CommonUtil.len(str);
        });

        assertEquals(Integer.valueOf(5), memoized.apply("hello"));
        assertEquals(Integer.valueOf(5), memoized.apply("hello"));
        assertEquals(Integer.valueOf(5), memoized.apply("world"));
        assertEquals(Integer.valueOf(5), memoized.apply("world"));
        assertEquals(2, counter.get());

        assertEquals(Integer.valueOf(0), memoized.apply(null));
        assertEquals(Integer.valueOf(0), memoized.apply(null));
        assertEquals(3, counter.get());
    }

    @Test
    public void testClose() {
        MockAutoCloseable closeable = new MockAutoCloseable();
        Runnable closeRunnable = Fn.close(closeable);

        assertFalse(closeable.isClosed());
        closeRunnable.run();
        assertTrue(closeable.isClosed());

        closeRunnable.run();
        assertTrue(closeable.isClosed());
    }

    @Test
    public void testCloseAll() {
        MockAutoCloseable closeable1 = new MockAutoCloseable();
        MockAutoCloseable closeable2 = new MockAutoCloseable();

        Runnable closeRunnable = Fn.closeAll(closeable1, closeable2);

        assertFalse(closeable1.isClosed());
        assertFalse(closeable2.isClosed());

        closeRunnable.run();

        assertTrue(closeable1.isClosed());
        assertTrue(closeable2.isClosed());
    }

    @Test
    public void testEmptyAction() {
        Runnable empty = Fn.emptyAction();
        empty.run();
    }

    @Test
    public void testDoNothing() {
        Consumer<String> doNothing = Fn.emptyConsumer();
        doNothing.accept("test");
        doNothing.accept(null);
    }

    @Test
    public void testThrowRuntimeException() {
        Consumer<String> thrower = Fn.throwRuntimeException("Test error");

        try {
            thrower.accept("any value");
            fail("Should have thrown RuntimeException");
        } catch (RuntimeException e) {
            assertEquals("Test error", e.getMessage());
        }
    }

    @Test
    public void testThrowException() {
        Consumer<String> thrower = Fn.throwException(() -> new IllegalStateException("Custom error"));

        try {
            thrower.accept("any value");
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Custom error", e.getMessage());
        }
    }

    @Test
    public void testSleepConsumer() throws InterruptedException {
        Consumer<String> sleeper = Fn.sleep(50);

        long start = System.currentTimeMillis();
        sleeper.accept("test");
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration >= 50);
        assertTrue(duration < 100);
    }

    @Test
    public void testPrintln() {
        Consumer<String> printer = Fn.println();
        printer.accept("test");
        printer.accept(null);
    }

    @Test
    public void testToStr() {
        Function<Object, String> toStr = Fn.toStr();

        assertEquals("test", toStr.apply("test"));
        assertEquals("123", toStr.apply(123));
        assertEquals("null", toStr.apply(null));
        assertEquals("[1, 2, 3]", toStr.apply(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testToCamelCase() {
        UnaryOperator<String> toCamelCase = Fn.toCamelCase();

        assertEquals("helloWorld", toCamelCase.apply("hello_world"));
        assertEquals("helloWorld", toCamelCase.apply("HELLO_WORLD"));
        assertEquals("helloWorld", toCamelCase.apply("hello-world"));
    }

    @Test
    public void testToLowerCase() {
        UnaryOperator<String> toLowerCase = Fn.toLowerCase();

        assertEquals("hello", toLowerCase.apply("HELLO"));
        assertEquals("hello world", toLowerCase.apply("Hello World"));
        assertEquals("", toLowerCase.apply(""));
        assertNull(toLowerCase.apply(null));
    }

    @Test
    public void testToUpperCase() {
        UnaryOperator<String> toUpperCase = Fn.toUpperCase();

        assertEquals("HELLO", toUpperCase.apply("hello"));
        assertEquals("HELLO WORLD", toUpperCase.apply("Hello World"));
        assertEquals("", toUpperCase.apply(""));
        assertNull(toUpperCase.apply(null));
    }

    @Test
    public void testIdentity() {
        Function<String, String> identity = Fn.identity();

        assertEquals("test", identity.apply("test"));
        assertNull(identity.apply(null));

        List<Integer> list = Arrays.asList(1, 2, 3);
        assertSame(list, Fn.<List<Integer>> identity().apply(list));
    }

    @Test
    public void testKeyed() {
        Function<String, Keyed<Integer, String>> keyed = Fn.keyed(String::length);

        Keyed<Integer, String> result = keyed.apply("hello");
        assertEquals(Integer.valueOf(5), result.key());
        assertEquals("hello", result.val());
    }

    @Test
    public void testKey() {
        Function<Map.Entry<String, Integer>, String> keyFunc = Fn.key();

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 42);
        assertEquals("key", keyFunc.apply(entry));
    }

    @Test
    public void testValue() {
        Function<Map.Entry<String, Integer>, Integer> valueFunc = Fn.value();

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 42);
        assertEquals(Integer.valueOf(42), valueFunc.apply(entry));
    }

    @Test
    public void testLeft() {
        Function<Pair<String, Integer>, String> leftFunc = Fn.left();

        Pair<String, Integer> pair = Pair.of("left", 42);
        assertEquals("left", leftFunc.apply(pair));
    }

    @Test
    public void testRight() {
        Function<Pair<String, Integer>, Integer> rightFunc = Fn.right();

        Pair<String, Integer> pair = Pair.of("left", 42);
        assertEquals(Integer.valueOf(42), rightFunc.apply(pair));
    }

    @Test
    public void testInverse() {
        Function<Map.Entry<String, Integer>, Map.Entry<Integer, String>> inverseFunc = Fn.invert();

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 42);
        Map.Entry<Integer, String> inverted = inverseFunc.apply(entry);

        assertEquals(Integer.valueOf(42), inverted.getKey());
        assertEquals("key", inverted.getValue());
    }

    @Test
    public void testEntry() {
        BiFunction<String, Integer, Map.Entry<String, Integer>> entryFunc = Fn.entry();

        Map.Entry<String, Integer> entry = entryFunc.apply("key", 42);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(42), entry.getValue());
    }

    @Test
    public void testPair() {
        BiFunction<String, Integer, Pair<String, Integer>> pairFunc = Fn.pair();

        Pair<String, Integer> pair = pairFunc.apply("left", 42);
        assertEquals("left", pair.left());
        assertEquals(Integer.valueOf(42), pair.right());
    }

    @Test
    public void testTriple() {
        TriFunction<String, Integer, Boolean, Triple<String, Integer, Boolean>> tripleFunc = Fn.triple();

        Triple<String, Integer, Boolean> triple = tripleFunc.apply("left", 42, true);
        assertEquals("left", triple.left());
        assertEquals(Integer.valueOf(42), triple.middle());
        assertEquals(Boolean.TRUE, triple.right());
    }

    @Test
    public void testTrim() {
        UnaryOperator<String> trim = Fn.trim();

        assertEquals("hello", trim.apply("  hello  "));
        assertEquals("", trim.apply("   "));
        assertNull(trim.apply(null));
    }

    @Test
    public void testTrimToEmpty() {
        UnaryOperator<String> trimToEmpty = Fn.trimToEmpty();

        assertEquals("hello", trimToEmpty.apply("  hello  "));
        assertEquals("", trimToEmpty.apply("   "));
        assertEquals("", trimToEmpty.apply(null));
    }

    @Test
    public void testTrimToNull() {
        UnaryOperator<String> trimToNull = Fn.trimToNull();

        assertEquals("hello", trimToNull.apply("  hello  "));
        assertNull(trimToNull.apply("   "));
        assertNull(trimToNull.apply(null));
    }

    @Test
    public void testNullToEmpty() {
        UnaryOperator<String> nullToEmpty = Fn.nullToEmpty();

        assertEquals("hello", nullToEmpty.apply("hello"));
        assertEquals("", nullToEmpty.apply(null));
    }

    @Test
    public void testLength() {
        Function<CharSequence, Integer> length = Fn.length();

        assertEquals(Integer.valueOf(5), length.apply("hello"));
        assertEquals(Integer.valueOf(0), length.apply(""));
        assertEquals(Integer.valueOf(0), length.apply(null));
        assertEquals(Integer.valueOf(10), length.apply(new StringBuilder("1234567890")));
    }

    @Test
    public void testLen() {
        Function<Object[], Integer> len = Fn.len();

        assertEquals(Integer.valueOf(3), len.apply(new String[] { "a", "b", "c" }));
        assertEquals(Integer.valueOf(0), len.apply(new Object[0]));
        assertEquals(Integer.valueOf(0), len.apply(null));
    }

    @Test
    public void testSize() {
        Function<Collection<String>, Integer> size = Fn.size();

        assertEquals(Integer.valueOf(3), size.apply(Arrays.asList("a", "b", "c")));
        assertEquals(Integer.valueOf(0), size.apply(new ArrayList<>()));
        assertEquals(Integer.valueOf(0), size.apply(null));
    }

    @Test
    public void testAlwaysTrue() {
        Predicate<String> alwaysTrue = Fn.alwaysTrue();

        assertTrue(alwaysTrue.test("anything"));
        assertTrue(alwaysTrue.test(null));
        assertTrue(alwaysTrue.test(""));
    }

    @Test
    public void testAlwaysFalse() {
        Predicate<String> alwaysFalse = Fn.alwaysFalse();

        assertFalse(alwaysFalse.test("anything"));
        assertFalse(alwaysFalse.test(null));
        assertFalse(alwaysFalse.test(""));
    }

    @Test
    public void testIsNull() {
        Predicate<String> isNull = Fn.isNull();

        assertTrue(isNull.test(null));
        assertFalse(isNull.test(""));
        assertFalse(isNull.test("hello"));
    }

    @Test
    public void testIsEmpty() {
        Predicate<CharSequence> isEmpty = Fn.isEmpty();

        assertTrue(isEmpty.test(""));
        assertTrue(isEmpty.test(null));
        assertFalse(isEmpty.test(" "));
        assertFalse(isEmpty.test("hello"));
    }

    @Test
    public void testIsBlank() {
        Predicate<CharSequence> isBlank = Fn.isBlank();

        assertTrue(isBlank.test(""));
        assertTrue(isBlank.test("   "));
        assertTrue(isBlank.test(null));
        assertFalse(isBlank.test("hello"));
        assertFalse(isBlank.test("  hello  "));
    }

    @Test
    public void testNotNull() {
        Predicate<String> notNull = Fn.notNull();

        assertFalse(notNull.test(null));
        assertTrue(notNull.test(""));
        assertTrue(notNull.test("hello"));
    }

    @Test
    public void testNotEmpty() {
        Predicate<CharSequence> notEmpty = Fn.notEmpty();

        assertFalse(notEmpty.test(""));
        assertFalse(notEmpty.test(null));
        assertTrue(notEmpty.test(" "));
        assertTrue(notEmpty.test("hello"));
    }

    @Test
    public void testNotBlank() {
        Predicate<CharSequence> notBlank = Fn.notBlank();

        assertFalse(notBlank.test(""));
        assertFalse(notBlank.test("   "));
        assertFalse(notBlank.test(null));
        assertTrue(notBlank.test("hello"));
        assertTrue(notBlank.test("  hello  "));
    }

    @Test
    public void testEqual() {
        Predicate<String> equalHello = Fn.equal("hello");

        assertTrue(equalHello.test("hello"));
        assertFalse(equalHello.test("Hello"));
        assertFalse(equalHello.test(null));

        Predicate<String> equalNull = Fn.equal(null);
        assertTrue(equalNull.test(null));
        assertFalse(equalNull.test(""));
    }

    @Test
    public void testNotEqual() {
        Predicate<String> notEqualHello = Fn.notEqual("hello");

        assertFalse(notEqualHello.test("hello"));
        assertTrue(notEqualHello.test("Hello"));
        assertTrue(notEqualHello.test(null));
    }

    @Test
    public void testGreaterThan() {
        Predicate<Integer> greaterThan5 = Fn.greaterThan(5);

        assertTrue(greaterThan5.test(6));
        assertTrue(greaterThan5.test(10));
        assertFalse(greaterThan5.test(5));
        assertFalse(greaterThan5.test(4));
    }

    @Test
    public void testLessThan() {
        Predicate<Integer> lessThan5 = Fn.lessThan(5);

        assertTrue(lessThan5.test(4));
        assertTrue(lessThan5.test(0));
        assertFalse(lessThan5.test(5));
        assertFalse(lessThan5.test(6));
    }

    @Test
    public void testBetween() {
        Predicate<Integer> between5And10 = Fn.between(5, 10);

        assertTrue(between5And10.test(6));
        assertTrue(between5And10.test(9));
        assertFalse(between5And10.test(5));
        assertFalse(between5And10.test(10));
        assertFalse(between5And10.test(4));
        assertFalse(between5And10.test(11));
    }

    @Test
    public void testIn() {
        Collection<String> collection = Arrays.asList("a", "b", "c");
        Predicate<String> inCollection = Fn.in(collection);

        assertTrue(inCollection.test("a"));
        assertTrue(inCollection.test("b"));
        assertTrue(inCollection.test("c"));
        assertFalse(inCollection.test("d"));
        assertFalse(inCollection.test(null));
    }

    @Test
    public void testNotIn() {
        Collection<String> collection = Arrays.asList("a", "b", "c");
        Predicate<String> notInCollection = Fn.notIn(collection);

        assertFalse(notInCollection.test("a"));
        assertFalse(notInCollection.test("b"));
        assertFalse(notInCollection.test("c"));
        assertTrue(notInCollection.test("d"));
        assertTrue(notInCollection.test(null));
    }

    @Test
    public void testInstanceOf() {
        Predicate<Object> isString = Fn.instanceOf(String.class);

        assertTrue(isString.test("hello"));
        assertFalse(isString.test(123));
        assertFalse(isString.test(null));
    }

    @Test
    public void testStartsWith() {
        Predicate<String> startsWithHello = Fn.startsWith("hello");

        assertTrue(startsWithHello.test("hello world"));
        assertTrue(startsWithHello.test("hello"));
        assertFalse(startsWithHello.test("world hello"));
        assertFalse(startsWithHello.test(null));
    }

    @Test
    public void testEndsWith() {
        Predicate<String> endsWithWorld = Fn.endsWith("world");

        assertTrue(endsWithWorld.test("hello world"));
        assertTrue(endsWithWorld.test("world"));
        assertFalse(endsWithWorld.test("world hello"));
        assertFalse(endsWithWorld.test(null));
    }

    @Test
    public void testContains() {
        Predicate<String> containsLlo = Fn.contains("llo");

        assertTrue(containsLlo.test("hello"));
        assertTrue(containsLlo.test("llo"));
        assertFalse(containsLlo.test("helo"));
        assertFalse(containsLlo.test(null));
    }

    @Test
    public void testNot() {
        Predicate<String> isNull = Fn.isNull();
        Predicate<String> notNull = Fn.not(isNull);

        assertTrue(notNull.test("hello"));
        assertFalse(notNull.test(null));
    }

    @Test
    public void testAnd() {
        Predicate<String> notNull = Fn.notNull();
        Predicate<String> notEmpty = Fn.notEmpty();
        Predicate<String> notNullAndNotEmpty = Fn.and(notNull, notEmpty);

        assertTrue(notNullAndNotEmpty.test("hello"));
        assertFalse(notNullAndNotEmpty.test(null));
        assertFalse(notNullAndNotEmpty.test(""));
    }

    @Test
    public void testOr() {
        Predicate<String> isNull = Fn.isNull();
        Predicate<String> isEmpty = Fn.isEmpty();
        Predicate<String> isNullOrEmpty = Fn.or(isNull, isEmpty);

        assertTrue(isNullOrEmpty.test(null));
        assertTrue(isNullOrEmpty.test(""));
        assertFalse(isNullOrEmpty.test("hello"));
    }

    @Test
    public void testTestByKey() {
        Predicate<Map.Entry<String, Integer>> keyIsHello = Fn.testByKey(Fn.equal("hello"));

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("hello", 42);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("world", 42);

        assertTrue(keyIsHello.test(entry1));
        assertFalse(keyIsHello.test(entry2));
    }

    @Test
    public void testTestByValue() {
        Predicate<Map.Entry<String, Integer>> valueIs42 = Fn.testByValue(Fn.equal(42));

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("hello", 42);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("world", 24);

        assertTrue(valueIs42.test(entry1));
        assertFalse(valueIs42.test(entry2));
    }

    @Test
    public void testAcceptByKey() {
        List<String> keys = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> acceptKey = Fn.acceptByKey(keys::add);

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("hello", 42);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("world", 24);

        acceptKey.accept(entry1);
        acceptKey.accept(entry2);

        assertEquals(Arrays.asList("hello", "world"), keys);
    }

    @Test
    public void testAcceptByValue() {
        List<Integer> values = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> acceptValue = Fn.acceptByValue(values::add);

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("hello", 42);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("world", 24);

        acceptValue.accept(entry1);
        acceptValue.accept(entry2);

        assertEquals(Arrays.asList(42, 24), values);
    }

    @Test
    public void testApplyByKey() {
        Function<Map.Entry<String, Integer>, Integer> keyLength = Fn.applyByKey(String::length);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("hello", 42);
        assertEquals(Integer.valueOf(5), keyLength.apply(entry));
    }

    @Test
    public void testApplyByValue() {
        Function<Map.Entry<String, Integer>, String> valueToString = Fn.applyByValue(Object::toString);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("hello", 42);
        assertEquals("42", valueToString.apply(entry));
    }

    @Test
    public void testAtMost() {
        Predicate<String> atMost2 = Fn.atMost(2);

        assertTrue(atMost2.test("first"));
        assertTrue(atMost2.test("second"));
        assertFalse(atMost2.test("third"));
        assertFalse(atMost2.test("fourth"));
    }

    @Test
    public void testSelectFirst() {
        BinaryOperator<String> selectFirst = Fn.selectFirst();

        assertEquals("first", selectFirst.apply("first", "second"));
        assertEquals(null, selectFirst.apply(null, "second"));
    }

    @Test
    public void testSelectSecond() {
        BinaryOperator<String> selectSecond = Fn.selectSecond();

        assertEquals("second", selectSecond.apply("first", "second"));
        assertEquals("second", selectSecond.apply(null, "second"));
    }

    @Test
    public void testMin() {
        BinaryOperator<Integer> min = Fn.min();

        assertEquals(Integer.valueOf(3), min.apply(5, 3));
        assertEquals(Integer.valueOf(3), min.apply(3, 5));
        assertEquals(Integer.valueOf(3), min.apply(3, 3));
    }

    @Test
    public void testMax() {
        BinaryOperator<Integer> max = Fn.max();

        assertEquals(Integer.valueOf(5), max.apply(5, 3));
        assertEquals(Integer.valueOf(5), max.apply(3, 5));
        assertEquals(Integer.valueOf(5), max.apply(5, 5));
    }

    @Test
    public void testMinBy() {
        BinaryOperator<String> minByLength = Fn.minBy(String::length);

        assertEquals("hi", minByLength.apply("hello", "hi"));
        assertEquals("hi", minByLength.apply("hi", "hello"));
        assertEquals("hello", minByLength.apply("hello", "world"));
    }

    @Test
    public void testMaxBy() {
        BinaryOperator<String> maxByLength = Fn.maxBy(String::length);

        assertEquals("hello", maxByLength.apply("hello", "hi"));
        assertEquals("hello", maxByLength.apply("hi", "hello"));
        assertEquals("hello", maxByLength.apply("hello", "world"));
    }

    @Test
    public void testThrowingMerger() {
        BinaryOperator<String> throwingMerger = Fn.throwingMerger();

        try {
            throwingMerger.apply("first", "second");
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().contains("Duplicate key"));
        }
    }

    @Test
    public void testIgnoringMerger() {
        BinaryOperator<String> ignoringMerger = Fn.ignoringMerger();

        assertEquals("first", ignoringMerger.apply("first", "second"));
    }

    @Test
    public void testReplacingMerger() {
        BinaryOperator<String> replacingMerger = Fn.replacingMerger();

        assertEquals("second", replacingMerger.apply("first", "second"));
    }

    @Test
    public void testGetAsInt() {
        OptionalInt opt = OptionalInt.of(42);
        assertEquals(42, Fn.GET_AS_INT.applyAsInt(opt));
    }

    @Test
    public void testGetAsLong() {
        OptionalLong opt = OptionalLong.of(42L);
        assertEquals(42L, Fn.GET_AS_LONG.applyAsLong(opt));
    }

    @Test
    public void testGetAsDouble() {
        OptionalDouble opt = OptionalDouble.of(42.5);
        assertEquals(42.5, Fn.GET_AS_DOUBLE.applyAsDouble(opt), 0.001);
    }

    @Test
    public void testIsPresentInt() {
        assertTrue(Fn.IS_PRESENT_INT.test(OptionalInt.of(42)));
        assertFalse(Fn.IS_PRESENT_INT.test(OptionalInt.empty()));
    }

    @Test
    public void testIsPresentLong() {
        assertTrue(Fn.IS_PRESENT_LONG.test(OptionalLong.of(42L)));
        assertFalse(Fn.IS_PRESENT_LONG.test(OptionalLong.empty()));
    }

    @Test
    public void testIsPresentDouble() {
        assertTrue(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.of(42.5)));
        assertFalse(Fn.IS_PRESENT_DOUBLE.test(OptionalDouble.empty()));
    }

    @Test
    public void testC2f() {
        Consumer<String> consumer = str -> System.out.println("Consumed: " + str);
        Function<String, Void> function = Fn.c2f(consumer);

        assertNull(function.apply("test"));
    }

    @Test
    public void testC2fWithReturn() {
        Consumer<String> consumer = str -> System.out.println("Consumed: " + str);
        Function<String, Integer> function = Fn.c2f(consumer, 42);

        assertEquals(Integer.valueOf(42), function.apply("test"));
    }

    @Test
    public void testF2c() {
        Function<String, Integer> function = String::length;
        Consumer<String> consumer = Fn.f2c(function);

        consumer.accept("test");
    }

    @Test
    public void testParseInt() {
        ToIntFunction<String> parseInt = Fn.parseInt();

        assertEquals(42, parseInt.applyAsInt("42"));
        assertEquals(-123, parseInt.applyAsInt("-123"));
        assertEquals(0, parseInt.applyAsInt("0"));
    }

    @Test
    public void testParseLong() {
        ToLongFunction<String> parseLong = Fn.parseLong();

        assertEquals(42L, parseLong.applyAsLong("42"));
        assertEquals(-123L, parseLong.applyAsLong("-123"));
        assertEquals(0L, parseLong.applyAsLong("0"));
    }

    @Test
    public void testParseDouble() {
        ToDoubleFunction<String> parseDouble = Fn.parseDouble();

        assertEquals(42.5, parseDouble.applyAsDouble("42.5"), 0.001);
        assertEquals(-123.456, parseDouble.applyAsDouble("-123.456"), 0.001);
        assertEquals(0.0, parseDouble.applyAsDouble("0"), 0.001);
    }

    @Test
    public void testNumToInt() {
        ToIntFunction<Number> numToInt = Fn.numToInt();

        assertEquals(42, numToInt.applyAsInt(42));
        assertEquals(42, numToInt.applyAsInt(42L));
        assertEquals(42, numToInt.applyAsInt(42.0));
        assertEquals(42, numToInt.applyAsInt(42.5));
    }

    @Test
    public void testNumToLong() {
        ToLongFunction<Number> numToLong = Fn.numToLong();

        assertEquals(42L, numToLong.applyAsLong(42));
        assertEquals(42L, numToLong.applyAsLong(42L));
        assertEquals(42L, numToLong.applyAsLong(42.0));
    }

    @Test
    public void testNumToDouble() {
        ToDoubleFunction<Number> numToDouble = Fn.numToDouble();

        assertEquals(42.0, numToDouble.applyAsDouble(42), 0.001);
        assertEquals(42.0, numToDouble.applyAsDouble(42L), 0.001);
        assertEquals(42.5, numToDouble.applyAsDouble(42.5), 0.001);
    }

    @Test
    public void testGetIfPresentOrElseNull() {
        Function<u.Optional<String>, String> getOrNull = Fn.getIfPresentOrElseNull();

        assertEquals("hello", getOrNull.apply(Optional.of("hello")));
        assertNull(getOrNull.apply(Optional.<String> empty()));
    }

    @Test
    public void testIsPresent() {
        Predicate<Optional<String>> isPresent = Fn.isPresent();

        assertTrue(isPresent.test(Optional.of("hello")));
        assertFalse(isPresent.test(Optional.<String> empty()));
    }

    @Test
    public void testGetIfPresentOrElseNullJdk() {
        Function<java.util.Optional<String>, String> getOrNull = Fn.getIfPresentOrElseNullJdk();

        assertEquals("hello", getOrNull.apply(java.util.Optional.of("hello")));
        assertNull(getOrNull.apply(java.util.Optional.<String> empty()));
    }

    @Test
    public void testIsPresentJdk() {
        Predicate<java.util.Optional<String>> isPresent = Fn.isPresentJdk();

        assertTrue(isPresent.test(java.util.Optional.of("hello")));
        assertFalse(isPresent.test(java.util.Optional.<String> empty()));
    }

    private static class MockAutoCloseable implements AutoCloseable {
        private boolean closed = false;

        @Override
        public void close() {
            closed = true;
        }

        public boolean isClosed() {
            return closed;
        }
    }
}
