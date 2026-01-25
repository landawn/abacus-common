package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalShort;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.function.UnaryOperator;
import com.landawn.abacus.util.stream.Collectors;

@Tag("new-test")
public class Fn101Test extends TestBase {

    @Test
    public void testToJson() {
        Function<Object, String> toJson = Fn.toJson();

        assertEquals("hello", toJson.apply("hello"));
        assertEquals("123", toJson.apply(123));
        assertEquals("[1, 2, 3]", toJson.apply(Arrays.asList(1, 2, 3)));

        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String json = toJson.apply(map);
        assertTrue(json.contains("\"key\""));
        assertTrue(json.contains("\"value\""));
    }

    @Test
    public void testToXml() {
        Function<Object, String> toXml = Fn.toXml();

        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        String xml = toXml.apply(map);
        assertTrue(xml.contains("<key>"));
        assertTrue(xml.contains("value"));
        assertTrue(xml.contains("</key>"));
    }

    @Test
    public void testWrap() {
        Function<String, Wrapper<String>> wrap = Fn.wrap();

        Wrapper<String> wrapped = wrap.apply("hello");
        assertEquals("hello", wrapped.value());

        Wrapper<String> wrappedNull = wrap.apply(null);
        assertNull(wrappedNull.value());
    }

    @Test
    public void testWrapWithHashEquals() {
        Function<String, Wrapper<String>> wrap = Fn.wrap(str -> str == null ? 0 : str.toUpperCase().hashCode(),
                (a, b) -> a == null ? b == null : a.equalsIgnoreCase(b));

        Wrapper<String> wrapped1 = wrap.apply("hello");
        Wrapper<String> wrapped2 = wrap.apply("HELLO");

        assertEquals(wrapped1.hashCode(), wrapped2.hashCode());
        assertTrue(wrapped1.equals(wrapped2));
    }

    @Test
    public void testUnwrap() {
        Function<Wrapper<String>, String> unwrap = Fn.unwrap();

        Wrapper<String> wrapped = Wrapper.of("hello");
        assertEquals("hello", unwrap.apply(wrapped));

        Wrapper<String> wrappedNull = Wrapper.of(null);
        assertNull(unwrap.apply(wrappedNull));
    }

    @Test
    public void testEntryWithKey() {
        Function<Integer, Map.Entry<String, Integer>> entryWithKey = Fn.entryWithKey("fixedKey");

        Map.Entry<String, Integer> entry = entryWithKey.apply(42);
        assertEquals("fixedKey", entry.getKey());
        assertEquals(Integer.valueOf(42), entry.getValue());
    }

    @Test
    public void testEntryByKeyMapper() {
        Function<String, Map.Entry<Integer, String>> entryByKey = Fn.entryByKeyMapper(String::length);

        Map.Entry<Integer, String> entry = entryByKey.apply("hello");
        assertEquals(Integer.valueOf(5), entry.getKey());
        assertEquals("hello", entry.getValue());
    }

    @Test
    public void testEntryWithValue() {
        Function<String, Map.Entry<String, Integer>> entryWithValue = Fn.entryWithValue(42);

        Map.Entry<String, Integer> entry = entryWithValue.apply("key");
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(42), entry.getValue());
    }

    @Test
    public void testEntryByValueMapper() {
        Function<String, Map.Entry<String, Integer>> entryByValue = Fn.entryByValueMapper(String::length);

        Map.Entry<String, Integer> entry = entryByValue.apply("hello");
        assertEquals("hello", entry.getKey());
        assertEquals(Integer.valueOf(5), entry.getValue());
    }

    @Test
    public void testTuple1() {
        Function<String, Tuple1<String>> tuple1 = Fn.tuple1();

        Tuple1<String> tuple = tuple1.apply("hello");
        assertEquals("hello", tuple._1);
    }

    @Test
    public void testTuple2() {
        BiFunction<String, Integer, Tuple2<String, Integer>> tuple2 = Fn.tuple2();

        Tuple2<String, Integer> tuple = tuple2.apply("hello", 42);
        assertEquals("hello", tuple._1);
        assertEquals(Integer.valueOf(42), tuple._2);
    }

    @Test
    public void testTuple3() {
        TriFunction<String, Integer, Boolean, Tuple3<String, Integer, Boolean>> tuple3 = Fn.tuple3();

        Tuple3<String, Integer, Boolean> tuple = tuple3.apply("hello", 42, true);
        assertEquals("hello", tuple._1);
        assertEquals(Integer.valueOf(42), tuple._2);
        assertEquals(Boolean.TRUE, tuple._3);
    }

    @Test
    public void testTuple4() {
        QuadFunction<String, Integer, Boolean, Double, Tuple4<String, Integer, Boolean, Double>> tuple4 = Fn.tuple4();

        Tuple4<String, Integer, Boolean, Double> tuple = tuple4.apply("hello", 42, true, 3.14);
        assertEquals("hello", tuple._1);
        assertEquals(Integer.valueOf(42), tuple._2);
        assertEquals(Boolean.TRUE, tuple._3);
        assertEquals(Double.valueOf(3.14), tuple._4);
    }

    @Test
    public void testStrip() {
        UnaryOperator<String> strip = Fn.strip();

        assertEquals("hello", strip.apply("  hello  "));
        assertEquals("hello world", strip.apply("\t hello world \n"));
        assertEquals("", strip.apply("   "));
        assertNull(strip.apply(null));
    }

    @Test
    public void testStripToEmpty() {
        UnaryOperator<String> stripToEmpty = Fn.stripToEmpty();

        assertEquals("hello", stripToEmpty.apply("  hello  "));
        assertEquals("", stripToEmpty.apply("   "));
        assertEquals("", stripToEmpty.apply(null));
    }

    @Test
    public void testStripToNull() {
        UnaryOperator<String> stripToNull = Fn.stripToNull();

        assertEquals("hello", stripToNull.apply("  hello  "));
        assertNull(stripToNull.apply("   "));
        assertNull(stripToNull.apply(null));
    }

    @Test
    public void testNullToEmptyList() {
        UnaryOperator<List<String>> nullToEmptyList = Fn.nullToEmptyList();

        List<String> list = Arrays.asList("a", "b");
        assertSame(list, nullToEmptyList.apply(list));

        List<String> emptyList = nullToEmptyList.apply(null);
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testNullToEmptySet() {
        UnaryOperator<Set<String>> nullToEmptySet = Fn.nullToEmptySet();

        Set<String> set = new HashSet<>(Arrays.asList("a", "b"));
        assertSame(set, nullToEmptySet.apply(set));

        Set<String> emptySet = nullToEmptySet.apply(null);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNullToEmptyMap() {
        UnaryOperator<Map<String, Integer>> nullToEmptyMap = Fn.nullToEmptyMap();

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertSame(map, nullToEmptyMap.apply(map));

        Map<String, Integer> emptyMap = nullToEmptyMap.apply(null);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testSizeM() {
        Function<Map<String, Integer>, Integer> sizeM = Fn.sizeM();

        Map<String, Integer> map = new HashMap<>();
        assertEquals(Integer.valueOf(0), sizeM.apply(map));

        map.put("a", 1);
        map.put("b", 2);
        assertEquals(Integer.valueOf(2), sizeM.apply(map));

        assertEquals(Integer.valueOf(0), sizeM.apply(null));
    }

    @Test
    public void testCast() {
        Function<Object, String> castToString = Fn.cast(String.class);

        assertEquals("hello", castToString.apply("hello"));

        try {
            String str = castToString.apply(123);
            N.println(str);
            fail("Should have thrown ClassCastException");
        } catch (ClassCastException e) {
        }
    }

    @Test
    public void testIsNullWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueIsNull = Fn.isNull(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", null);
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");

        assertTrue(valueIsNull.test(entry1));
        assertFalse(valueIsNull.test(entry2));
    }

    @Test
    public void testIsEmptyWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueIsEmpty = Fn.isEmpty(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertTrue(valueIsEmpty.test(entry1));
        assertFalse(valueIsEmpty.test(entry2));
        assertTrue(valueIsEmpty.test(entry3));
    }

    @Test
    public void testIsBlankWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueIsBlank = Fn.isBlank(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "   ");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertTrue(valueIsBlank.test(entry1));
        assertFalse(valueIsBlank.test(entry2));
        assertTrue(valueIsBlank.test(entry3));
    }

    @Test
    public void testIsEmptyA() {
        Predicate<String[]> isEmptyA = Fn.isEmptyA();

        assertTrue(isEmptyA.test(new String[0]));
        assertTrue(isEmptyA.test(null));
        assertFalse(isEmptyA.test(new String[] { "a" }));
    }

    @Test
    public void testIsEmptyC() {
        Predicate<Collection<String>> isEmptyC = Fn.isEmptyC();

        assertTrue(isEmptyC.test(new ArrayList<>()));
        assertTrue(isEmptyC.test(null));
        assertFalse(isEmptyC.test(Arrays.asList("a")));
    }

    @Test
    public void testIsEmptyM() {
        Predicate<Map<String, Integer>> isEmptyM = Fn.isEmptyM();

        assertTrue(isEmptyM.test(new HashMap<>()));
        assertTrue(isEmptyM.test(null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(isEmptyM.test(map));
    }

    @Test
    public void testNotNullWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueNotNull = Fn.notNull(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", null);
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");

        assertFalse(valueNotNull.test(entry1));
        assertTrue(valueNotNull.test(entry2));
    }

    @Test
    public void testNotEmptyWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueNotEmpty = Fn.notEmpty(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertFalse(valueNotEmpty.test(entry1));
        assertTrue(valueNotEmpty.test(entry2));
        assertFalse(valueNotEmpty.test(entry3));
    }

    @Test
    public void testNotBlankWithValueExtractor() {
        Predicate<Map.Entry<String, String>> valueNotBlank = Fn.notBlank(Map.Entry::getValue);

        Map.Entry<String, String> entry1 = new AbstractMap.SimpleEntry<>("key", "   ");
        Map.Entry<String, String> entry2 = new AbstractMap.SimpleEntry<>("key", "value");
        Map.Entry<String, String> entry3 = new AbstractMap.SimpleEntry<>("key", null);

        assertFalse(valueNotBlank.test(entry1));
        assertTrue(valueNotBlank.test(entry2));
        assertFalse(valueNotBlank.test(entry3));
    }

    @Test
    public void testNotEmptyA() {
        Predicate<String[]> notEmptyA = Fn.notEmptyA();

        assertFalse(notEmptyA.test(new String[0]));
        assertFalse(notEmptyA.test(null));
        assertTrue(notEmptyA.test(new String[] { "a" }));
    }

    @Test
    public void testNotEmptyC() {
        Predicate<Collection<String>> notEmptyC = Fn.notEmptyC();

        assertFalse(notEmptyC.test(new ArrayList<>()));
        assertFalse(notEmptyC.test(null));
        assertTrue(notEmptyC.test(Arrays.asList("a")));
    }

    @Test
    public void testNotEmptyM() {
        Predicate<Map<String, Integer>> notEmptyM = Fn.notEmptyM();

        assertFalse(notEmptyM.test(new HashMap<>()));
        assertFalse(notEmptyM.test(null));

        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertTrue(notEmptyM.test(map));
    }

    @Test
    public void testIsFile() {
        Predicate<File> isFile = Fn.isFile();

        File file = new File("test.txt");
        File dir = new File(".");

        assertFalse(isFile.test(null));
    }

    @Test
    public void testIsDirectory() {
        Predicate<File> isDirectory = Fn.isDirectory();

        File file = new File("test.txt");
        File dir = new File(".");

        assertFalse(isDirectory.test(null));
    }

    @Test
    public void testEqOr() {
        Predicate<String> eqAorB = Fn.eqOr("a", "b");

        assertTrue(eqAorB.test("a"));
        assertTrue(eqAorB.test("b"));
        assertFalse(eqAorB.test("c"));
        assertFalse(eqAorB.test(null));
    }

    @Test
    public void testEqOr3() {
        Predicate<String> eqAorBorC = Fn.eqOr("a", "b", "c");

        assertTrue(eqAorBorC.test("a"));
        assertTrue(eqAorBorC.test("b"));
        assertTrue(eqAorBorC.test("c"));
        assertFalse(eqAorBorC.test("d"));
        assertFalse(eqAorBorC.test(null));
    }

    @Test
    public void testGreaterEqual() {
        Predicate<Integer> greaterEqual5 = Fn.greaterEqual(5);

        assertTrue(greaterEqual5.test(6));
        assertTrue(greaterEqual5.test(5));
        assertFalse(greaterEqual5.test(4));
    }

    @Test
    public void testLessEqual() {
        Predicate<Integer> lessEqual5 = Fn.lessEqual(5);

        assertTrue(lessEqual5.test(4));
        assertTrue(lessEqual5.test(5));
        assertFalse(lessEqual5.test(6));
    }

    @Test
    public void testGtAndLt() {
        Predicate<Integer> between5And10 = Fn.gtAndLt(5, 10);

        assertTrue(between5And10.test(6));
        assertTrue(between5And10.test(9));
        assertFalse(between5And10.test(5));
        assertFalse(between5And10.test(10));
    }

    @Test
    public void testGeAndLt() {
        Predicate<Integer> from5To10 = Fn.geAndLt(5, 10);

        assertTrue(from5To10.test(5));
        assertTrue(from5To10.test(9));
        assertFalse(from5To10.test(4));
        assertFalse(from5To10.test(10));
    }

    @Test
    public void testGeAndLe() {
        Predicate<Integer> from5To10Inclusive = Fn.geAndLe(5, 10);

        assertTrue(from5To10Inclusive.test(5));
        assertTrue(from5To10Inclusive.test(10));
        assertTrue(from5To10Inclusive.test(7));
        assertFalse(from5To10Inclusive.test(4));
        assertFalse(from5To10Inclusive.test(11));
    }

    @Test
    public void testGtAndLe() {
        Predicate<Integer> from5To10 = Fn.gtAndLe(5, 10);

        assertTrue(from5To10.test(6));
        assertTrue(from5To10.test(10));
        assertFalse(from5To10.test(5));
        assertFalse(from5To10.test(11));
    }

    @Test
    public void testSubtypeOf() {
        Predicate<Class<?>> subtypeOfNumber = Fn.subtypeOf(Number.class);

        assertTrue(subtypeOfNumber.test(Integer.class));
        assertTrue(subtypeOfNumber.test(Double.class));
        assertTrue(subtypeOfNumber.test(Number.class));
        assertFalse(subtypeOfNumber.test(String.class));
    }

    @Test
    public void testNotStartsWith() {
        Predicate<String> notStartsWithHello = Fn.notStartsWith("hello");

        assertFalse(notStartsWithHello.test("hello world"));
        assertTrue(notStartsWithHello.test("world hello"));
        assertTrue(notStartsWithHello.test(null));
    }

    @Test
    public void testNotEndsWith() {
        Predicate<String> notEndsWithWorld = Fn.notEndsWith("world");

        assertFalse(notEndsWithWorld.test("hello world"));
        assertTrue(notEndsWithWorld.test("world hello"));
        assertTrue(notEndsWithWorld.test(null));
    }

    @Test
    public void testNotContains() {
        Predicate<String> notContainsLlo = Fn.notContains("llo");

        assertFalse(notContainsLlo.test("hello"));
        assertTrue(notContainsLlo.test("halo"));
        assertTrue(notContainsLlo.test(null));
    }

    @Test
    public void testMatches() {
        Pattern pattern = Pattern.compile("\\d+");
        Predicate<CharSequence> matchesDigits = Fn.matches(pattern);

        assertTrue(matchesDigits.test("123"));
        assertTrue(matchesDigits.test("abc123def"));
        assertFalse(matchesDigits.test("abc"));
    }

    @Test
    public void testBiPredicateEqual() {
        BiPredicate<String, String> equal = Fn.equal();

        assertTrue(equal.test("hello", "hello"));
        assertFalse(equal.test("hello", "world"));
        assertTrue(equal.test(null, null));
        assertFalse(equal.test(null, "hello"));
    }

    @Test
    public void testBiPredicateNotEqual() {
        BiPredicate<String, String> notEqual = Fn.notEqual();

        assertFalse(notEqual.test("hello", "hello"));
        assertTrue(notEqual.test("hello", "world"));
        assertFalse(notEqual.test(null, null));
        assertTrue(notEqual.test(null, "hello"));
    }

    @Test
    public void testBiPredicateGreaterThan() {
        BiPredicate<Integer, Integer> greaterThan = Fn.greaterThan();

        assertTrue(greaterThan.test(5, 3));
        assertFalse(greaterThan.test(3, 5));
        assertFalse(greaterThan.test(5, 5));
    }

    @Test
    public void testBiPredicateGreaterEqual() {
        BiPredicate<Integer, Integer> greaterEqual = Fn.greaterEqual();

        assertTrue(greaterEqual.test(5, 3));
        assertFalse(greaterEqual.test(3, 5));
        assertTrue(greaterEqual.test(5, 5));
    }

    @Test
    public void testBiPredicateLessThan() {
        BiPredicate<Integer, Integer> lessThan = Fn.lessThan();

        assertTrue(lessThan.test(3, 5));
        assertFalse(lessThan.test(5, 3));
        assertFalse(lessThan.test(5, 5));
    }

    @Test
    public void testBiPredicateLessEqual() {
        BiPredicate<Integer, Integer> lessEqual = Fn.lessEqual();

        assertTrue(lessEqual.test(3, 5));
        assertFalse(lessEqual.test(5, 3));
        assertTrue(lessEqual.test(5, 5));
    }

    @Test
    public void testNotBiPredicate() {
        BiPredicate<String, String> equal = Fn.equal();
        BiPredicate<String, String> notEqual = Fn.not(equal);

        assertFalse(notEqual.test("hello", "hello"));
        assertTrue(notEqual.test("hello", "world"));
    }

    @Test
    public void testNotTriPredicate() {
        TriPredicate<String, String, String> allEqual = (a, b, c) -> a.equals(b) && b.equals(c);
        TriPredicate<String, String, String> notAllEqual = Fn.not(allEqual);

        assertFalse(notAllEqual.test("a", "a", "a"));
        assertTrue(notAllEqual.test("a", "b", "a"));
    }

    @Test
    public void testAndBooleanSupplier() {
        BooleanSupplier alwaysTrue = () -> true;
        BooleanSupplier alwaysFalse = () -> false;

        BooleanSupplier trueAndTrue = Fn.and(alwaysTrue, alwaysTrue);
        assertTrue(trueAndTrue.getAsBoolean());

        BooleanSupplier trueAndFalse = Fn.and(alwaysTrue, alwaysFalse);
        assertFalse(trueAndFalse.getAsBoolean());
    }

    @Test
    public void testAndBooleanSupplier3() {
        BooleanSupplier alwaysTrue = () -> true;
        BooleanSupplier alwaysFalse = () -> false;

        BooleanSupplier allTrue = Fn.and(alwaysTrue, alwaysTrue, alwaysTrue);
        assertTrue(allTrue.getAsBoolean());

        BooleanSupplier oneFalse = Fn.and(alwaysTrue, alwaysFalse, alwaysTrue);
        assertFalse(oneFalse.getAsBoolean());
    }

    @Test
    public void testAndThreePredicates() {
        Predicate<String> notNull = Fn.notNull();
        Predicate<String> notEmpty = Fn.notEmpty();
        Predicate<String> longerThan3 = s -> s.length() > 3;

        Predicate<String> combined = Fn.and(notNull, notEmpty, longerThan3);

        assertTrue(combined.test("hello"));
        assertFalse(combined.test("hi"));
        assertFalse(combined.test(""));
        assertFalse(combined.test(null));
    }

    @Test
    public void testAndCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(n -> n > 0, n -> n < 100, n -> n % 2 == 0);

        Predicate<Integer> combined = Fn.and(predicates);

        assertTrue(combined.test(50));
        assertFalse(combined.test(51));
        assertFalse(combined.test(0));
        assertFalse(combined.test(100));
    }

    @Test
    public void testAndBiPredicateThree() {
        BiPredicate<String, Integer> notNullAndPositive = (s, n) -> s != null && n > 0;
        BiPredicate<String, Integer> longerThanValue = (s, n) -> s.length() > n;
        BiPredicate<String, Integer> containsA = (s, n) -> s.contains("a");

        BiPredicate<String, Integer> combined = Fn.and(notNullAndPositive, longerThanValue, containsA);

        assertTrue(combined.test("banana", 3));
        assertFalse(combined.test("bnn", 2));
        assertFalse(combined.test("a", 2));
    }

    @Test
    public void testOrBooleanSupplier() {
        BooleanSupplier alwaysTrue = () -> true;
        BooleanSupplier alwaysFalse = () -> false;

        BooleanSupplier falseOrFalse = Fn.or(alwaysFalse, alwaysFalse);
        assertFalse(falseOrFalse.getAsBoolean());

        BooleanSupplier trueOrFalse = Fn.or(alwaysTrue, alwaysFalse);
        assertTrue(trueOrFalse.getAsBoolean());
    }

    @Test
    public void testOrBooleanSupplier3() {
        BooleanSupplier alwaysTrue = () -> true;
        BooleanSupplier alwaysFalse = () -> false;

        BooleanSupplier allFalse = Fn.or(alwaysFalse, alwaysFalse, alwaysFalse);
        assertFalse(allFalse.getAsBoolean());

        BooleanSupplier oneTrue = Fn.or(alwaysFalse, alwaysTrue, alwaysFalse);
        assertTrue(oneTrue.getAsBoolean());
    }

    @Test
    public void testOrThreePredicates() {
        Predicate<String> isEmpty = Fn.isEmpty();
        Predicate<String> isBlank = Fn.isBlank();
        Predicate<String> equalsNone = s -> "none".equals(s);

        Predicate<String> combined = Fn.or(isEmpty, isBlank, equalsNone);

        assertTrue(combined.test(""));
        assertTrue(combined.test("   "));
        assertTrue(combined.test("none"));
        assertFalse(combined.test("hello"));
    }

    @Test
    public void testOrCollection() {
        List<Predicate<Integer>> predicates = Arrays.asList(n -> n < 0, n -> n > 100, n -> n == 50);

        Predicate<Integer> combined = Fn.or(predicates);

        assertTrue(combined.test(-5));
        assertTrue(combined.test(150));
        assertTrue(combined.test(50));
        assertFalse(combined.test(25));
    }

    @Test
    public void testMapKey() {
        Function<Map.Entry<String, Integer>, Map.Entry<Integer, Integer>> mapKey = Fn.mapKey(String::length);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("hello", 42);
        Map.Entry<Integer, Integer> mapped = mapKey.apply(entry);

        assertEquals(Integer.valueOf(5), mapped.getKey());
        assertEquals(Integer.valueOf(42), mapped.getValue());
    }

    @Test
    public void testMapValue() {
        Function<Map.Entry<String, Integer>, Map.Entry<String, String>> mapValue = Fn.mapValue(Object::toString);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("hello", 42);
        Map.Entry<String, String> mapped = mapValue.apply(entry);

        assertEquals("hello", mapped.getKey());
        assertEquals("42", mapped.getValue());
    }

    @Test
    public void testTestKeyVal() {
        Predicate<Map.Entry<String, Integer>> keyLengthEqualsValue = Fn.testKeyVal((k, v) -> k.length() == v);

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("hello", 5);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("hi", 5);

        assertTrue(keyLengthEqualsValue.test(entry1));
        assertFalse(keyLengthEqualsValue.test(entry2));
    }

    @Test
    public void testAcceptKeyVal() {
        List<String> results = new ArrayList<>();
        Consumer<Map.Entry<String, Integer>> acceptKeyVal = Fn.acceptKeyVal((k, v) -> results.add(k + ":" + v));

        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("hello", 5);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("world", 10);

        acceptKeyVal.accept(entry1);
        acceptKeyVal.accept(entry2);

        assertEquals(Arrays.asList("hello:5", "world:10"), results);
    }

    @Test
    public void testApplyKeyVal() {
        Function<Map.Entry<String, Integer>, String> applyKeyVal = Fn.applyKeyVal((k, v) -> k + " has length " + v);

        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("hello", 5);
        assertEquals("hello has length 5", applyKeyVal.apply(entry));
    }

    @Test
    public void testAcceptIfNotNull() {
        List<String> results = new ArrayList<>();
        Consumer<String> acceptIfNotNull = Fn.acceptIfNotNull(results::add);

        acceptIfNotNull.accept("hello");
        acceptIfNotNull.accept(null);
        acceptIfNotNull.accept("world");

        assertEquals(Arrays.asList("hello", "world"), results);
    }

    @Test
    public void testAcceptIf() {
        List<String> results = new ArrayList<>();
        Consumer<String> acceptIfLong = Fn.acceptIf(s -> s.length() > 3, results::add);

        acceptIfLong.accept("hello");
        acceptIfLong.accept("hi");
        acceptIfLong.accept("world");

        assertEquals(Arrays.asList("hello", "world"), results);
    }

    @Test
    public void testAcceptIfOrElse() {
        List<String> longStrings = new ArrayList<>();
        List<String> shortStrings = new ArrayList<>();

        Consumer<String> acceptIfOrElse = Fn.acceptIfOrElse(s -> s.length() > 3, longStrings::add, shortStrings::add);

        acceptIfOrElse.accept("hello");
        acceptIfOrElse.accept("hi");
        acceptIfOrElse.accept("world");
        acceptIfOrElse.accept("bye");

        assertEquals(Arrays.asList("hello", "world"), longStrings);
        assertEquals(Arrays.asList("hi", "bye"), shortStrings);
    }

    @Test
    public void testApplyIfNotNullOrEmpty() {
        Function<List<String>, Collection<String>> upperCase = Fn
                .applyIfNotNullOrEmpty(list -> list.stream().map(String::toUpperCase).collect(Collectors.toList()));

        assertEquals(Arrays.asList("HELLO", "WORLD"), upperCase.apply(Arrays.asList("hello", "world")));
        assertTrue(upperCase.apply(null).isEmpty());
    }

    @Test
    public void testApplyIfNotNullOrDefault() {
        Function<String, Integer> getLengthOrDefault = Fn.applyIfNotNullOrDefault(s -> s, String::length, -1);

        assertEquals(Integer.valueOf(5), getLengthOrDefault.apply("hello"));
        assertEquals(Integer.valueOf(-1), getLengthOrDefault.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrDefault3Functions() {
        Function<String, Integer> complexFunction = Fn.applyIfNotNullOrDefault(s -> s.toUpperCase(), s -> s.substring(0, 3), String::length, -1);

        assertEquals(Integer.valueOf(3), complexFunction.apply("hello"));
        assertEquals(Integer.valueOf(-1), complexFunction.apply(null));
    }

    @Test
    public void testApplyIfNotNullOrElseGet() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, Integer> getLengthOrSupplied = Fn.applyIfNotNullOrElseGet(s -> s, String::length, counter::incrementAndGet);

        assertEquals(Integer.valueOf(5), getLengthOrSupplied.apply("hello"));
        assertEquals(Integer.valueOf(1), getLengthOrSupplied.apply(null));
        assertEquals(Integer.valueOf(2), getLengthOrSupplied.apply(null));
    }

    @Test
    public void testApplyIfOrElseDefault() {
        Function<String, String> upperIfLong = Fn.applyIfOrElseDefault(s -> s.length() > 3, String::toUpperCase, "TOO_SHORT");

        assertEquals("HELLO", upperIfLong.apply("hello"));
        assertEquals("TOO_SHORT", upperIfLong.apply("hi"));
    }

    @Test
    public void testApplyIfOrElseGet() {
        AtomicInteger counter = new AtomicInteger(0);
        Function<String, String> upperIfLongOrCount = Fn.applyIfOrElseGet(s -> s.length() > 3, String::toUpperCase, () -> "SHORT_" + counter.incrementAndGet());

        assertEquals("HELLO", upperIfLongOrCount.apply("hello"));
        assertEquals("SHORT_1", upperIfLongOrCount.apply("hi"));
        assertEquals("SHORT_2", upperIfLongOrCount.apply("bye"));
    }

    @Test
    public void testFlatmapValue() {
        Function<Map<String, ? extends Collection<Integer>>, List<Map<String, Integer>>> flatmap = Fn.<String, Integer> flatmapValue();

        Map<String, Collection<Integer>> input = new HashMap<>();
        input.put("a", Arrays.asList(1, 2, 3));
        input.put("b", Arrays.asList(4, 5));

        List<Map<String, Integer>> result = flatmap.apply(input);

        assertEquals(3, result.size());
        assertTrue(result.get(0).containsKey("a"));
        assertTrue(result.get(0).containsKey("b"));
        assertEquals(Integer.valueOf(1), result.get(0).get("a"));
        assertEquals(Integer.valueOf(4), result.get(0).get("b"));
    }

    @Test
    public void testParseByte() {
        ToByteFunction<String> parseByte = Fn.parseByte();

        assertEquals((byte) 42, parseByte.applyAsByte("42"));
        assertEquals((byte) -128, parseByte.applyAsByte("-128"));
        assertEquals((byte) 127, parseByte.applyAsByte("127"));
    }

    @Test
    public void testParseShort() {
        ToShortFunction<String> parseShort = Fn.parseShort();

        assertEquals((short) 42, parseShort.applyAsShort("42"));
        assertEquals((short) -32768, parseShort.applyAsShort("-32768"));
        assertEquals((short) 32767, parseShort.applyAsShort("32767"));
    }

    @Test
    public void testParseFloat() {
        ToFloatFunction<String> parseFloat = Fn.parseFloat();

        assertEquals(42.5f, parseFloat.applyAsFloat("42.5"), 0.001f);
        assertEquals(-123.456f, parseFloat.applyAsFloat("-123.456"), 0.001f);
        assertEquals(0.0f, parseFloat.applyAsFloat("0"), 0.001f);
    }

    @Test
    public void testCreateNumber() {
        Function<String, Number> createNumber = Fn.createNumber();

        assertEquals(42, createNumber.apply("42"));
        assertEquals(42L, createNumber.apply("42L"));
        assertEquals(42.5, createNumber.apply("42.5"));
        assertNull(createNumber.apply(""));
        assertNull(createNumber.apply(null));
    }

    @Test
    public void testLimitThenFilter() {
        Predicate<Integer> limitThenEven = Fn.limitThenFilter(3, n -> n % 2 == 0);

        assertTrue(limitThenEven.test(2));
        assertFalse(limitThenEven.test(3));
        assertTrue(limitThenEven.test(4));
        assertFalse(limitThenEven.test(6));
    }

    @Test
    public void testFilterThenLimit() {
        Predicate<Integer> evenThenLimit = Fn.filterThenLimit(n -> n % 2 == 0, 2);

        assertFalse(evenThenLimit.test(1));
        assertTrue(evenThenLimit.test(2));
        assertFalse(evenThenLimit.test(3));
        assertTrue(evenThenLimit.test(4));
        assertFalse(evenThenLimit.test(6));
    }

    @Test
    public void testTimeLimit() throws InterruptedException {
        Predicate<String> timeLimit = Fn.timeLimit(100);

        assertTrue(timeLimit.test("first"));
        Thread.sleep(50);
        assertTrue(timeLimit.test("second"));
        Thread.sleep(89);
        assertFalse(timeLimit.test("third"));
    }

    @Test
    public void testTimeLimitWithDuration() throws InterruptedException {
        Predicate<String> timeLimit = Fn.timeLimit(Duration.ofMillis(100));

        assertTrue(timeLimit.test("first"));
        Thread.sleep(50);
        assertTrue(timeLimit.test("second"));
        Thread.sleep(89);
        assertFalse(timeLimit.test("third"));
    }

    @Test
    public void testIndexed() {
        Function<String, Indexed<String>> indexed = Fn.indexed();

        Indexed<String> first = indexed.apply("hello");
        assertEquals("hello", first.value());
        assertEquals(0, first.index());

        Indexed<String> second = indexed.apply("world");
        assertEquals("world", second.value());
        assertEquals(1, second.index());
    }

    @Test
    public void testIndexedPredicate() {
        Predicate<String> indexedPredicate = Fn.indexed((index, value) -> index % 2 == 0 || value.length() > 5);

        assertTrue(indexedPredicate.test("short"));
        assertTrue(indexedPredicate.test("verylongstring"));
        assertTrue(indexedPredicate.test("any"));
        assertFalse(indexedPredicate.test("no"));
    }

    @Test
    public void testCompareTo() {
        Function<String, Integer> compareToHello = Fn.compareTo("hello");

        assertEquals(0, compareToHello.apply("hello").intValue());
        assertTrue(compareToHello.apply("world") > 0);
        assertTrue(compareToHello.apply("apple") < 0);
    }

    @Test
    public void testCompareToWithComparator() {
        Function<String, Integer> compareToHelloIgnoreCase = Fn.compareTo("HELLO", String.CASE_INSENSITIVE_ORDER);

        assertEquals(0, compareToHelloIgnoreCase.apply("hello").intValue());
        assertEquals(0, compareToHelloIgnoreCase.apply("HELLO").intValue());
        assertTrue(compareToHelloIgnoreCase.apply("world") > 0);
    }

    @Test
    public void testCompare() {
        BiFunction<String, String, Integer> compare = Fn.compare();

        assertEquals(0, compare.apply("hello", "hello").intValue());
        assertTrue(compare.apply("world", "hello") > 0);
        assertTrue(compare.apply("apple", "hello") < 0);
    }

    @Test
    public void testCompareWithComparator() {
        BiFunction<String, String, Integer> compareIgnoreCase = Fn.compare(String.CASE_INSENSITIVE_ORDER);

        assertEquals(0, compareIgnoreCase.apply("hello", "HELLO").intValue());
        assertTrue(compareIgnoreCase.apply("WORLD", "hello") > 0);
    }

    @Test
    public void testFutureGet() {
        Function<Future<String>, String> futureGet = Fn.futureGet();

        CompletableFuture<String> future = CompletableFuture.completedFuture("result");
        assertEquals("result", futureGet.apply(future));
    }

    @Test
    public void testFutureGetOrDefaultOnError() {
        Function<Future<String>, String> futureGetOrDefault = Fn.futureGetOrDefaultOnError("default");

        CompletableFuture<String> successFuture = CompletableFuture.completedFuture("success");
        assertEquals("success", futureGetOrDefault.apply(successFuture));

        CompletableFuture<String> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new RuntimeException("error"));
        assertEquals("default", futureGetOrDefault.apply(failedFuture));
    }

    @Test
    public void testFrom() {
        java.util.function.Supplier<String> jdkSupplier = () -> "hello";
        Supplier<String> abacusSupplier = Fn.from(jdkSupplier);
        assertEquals("hello", abacusSupplier.get());

        java.util.function.Predicate<String> jdkPredicate = s -> s.length() > 3;
        Predicate<String> abacusPredicate = Fn.from(jdkPredicate);
        assertTrue(abacusPredicate.test("hello"));
        assertFalse(abacusPredicate.test("hi"));

        java.util.function.Function<String, Integer> jdkFunction = String::length;
        Function<String, Integer> abacusFunction = Fn.from(jdkFunction);
        assertEquals(Integer.valueOf(5), abacusFunction.apply("hello"));
    }

    @Test
    public void testAlternate() {
        BiFunction<String, String, MergeResult> alternate = Fn.alternate();

        assertEquals(MergeResult.TAKE_FIRST, alternate.apply("a", "b"));
        assertEquals(MergeResult.TAKE_SECOND, alternate.apply("c", "d"));
        assertEquals(MergeResult.TAKE_FIRST, alternate.apply("e", "f"));
    }

    @Test
    public void testLongSuppliersCurrentTimeMillis() {
        LongSupplier currentTime = Fn.LongSuppliers.ofCurrentTimeMillis();

        long time1 = currentTime.getAsLong();
        assertTrue(time1 > 0);

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
        }

        long time2 = currentTime.getAsLong();
        assertTrue(time2 >= time1);
    }

    @Test
    public void testOptionalOperations() {
        OptionalBoolean ob = OptionalBoolean.of(true);
        assertTrue(Fn.GET_AS_BOOLEAN.applyAsBoolean(ob));

        OptionalChar oc = OptionalChar.of('A');
        assertEquals('A', Fn.GET_AS_CHAR.applyAsChar(oc));

        OptionalByte oby = OptionalByte.of((byte) 42);
        assertEquals(42, Fn.GET_AS_BYTE.applyAsByte(oby));

        OptionalShort os = OptionalShort.of((short) 42);
        assertEquals(42, Fn.GET_AS_SHORT.applyAsShort(os));

        OptionalFloat of = OptionalFloat.of(42.5f);
        assertEquals(42.5f, Fn.GET_AS_FLOAT.applyAsFloat(of), 0.001f);

        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.empty()));

        assertTrue(Fn.IS_PRESENT_CHAR.test(OptionalChar.of('A')));
        assertFalse(Fn.IS_PRESENT_CHAR.test(OptionalChar.empty()));

        assertTrue(Fn.IS_PRESENT_BYTE.test(OptionalByte.of((byte) 1)));
        assertFalse(Fn.IS_PRESENT_BYTE.test(OptionalByte.empty()));

        assertTrue(Fn.IS_PRESENT_SHORT.test(OptionalShort.of((short) 1)));
        assertFalse(Fn.IS_PRESENT_SHORT.test(OptionalShort.empty()));

        assertTrue(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.of(1.0f)));
        assertFalse(Fn.IS_PRESENT_FLOAT.test(OptionalFloat.empty()));
    }

    @Test
    public void testJdkOptionalOperations() {
        java.util.OptionalInt oi = java.util.OptionalInt.of(42);
        assertEquals(42, Fn.GET_AS_INT_JDK.applyAsInt(oi));

        java.util.OptionalLong ol = java.util.OptionalLong.of(42L);
        assertEquals(42L, Fn.GET_AS_LONG_JDK.applyAsLong(ol));

        java.util.OptionalDouble od = java.util.OptionalDouble.of(42.5);
        assertEquals(42.5, Fn.GET_AS_DOUBLE_JDK.applyAsDouble(od), 0.001);

        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));

        assertTrue(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.of(1L)));
        assertFalse(Fn.IS_PRESENT_LONG_JDK.test(java.util.OptionalLong.empty()));

        assertTrue(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.of(1.0)));
        assertFalse(Fn.IS_PRESENT_DOUBLE_JDK.test(java.util.OptionalDouble.empty()));
    }

    @Test
    public void testShutDown() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Runnable shutdown = Fn.shutDown(executor);

        assertFalse(executor.isShutdown());
        shutdown.run();
        assertTrue(executor.isShutdown());

        shutdown.run();
        assertTrue(executor.isShutdown());
    }

    @Test
    public void testShutDownWithTimeout() throws InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        });

        Runnable shutdown = Fn.shutDown(executor, 100, TimeUnit.MILLISECONDS);

        assertFalse(executor.isTerminated());
        shutdown.run();

        Thread.sleep(150);
        assertTrue(executor.isTerminated());
    }

    @Test
    public void testRateLimiter() {
        Consumer<String> rateLimited = Fn.rateLimiter(5.0);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 5; i++) {
            rateLimited.accept("test" + i);
        }
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 1500);
    }

    @Test
    public void testRateLimiterWithInstance() {
        RateLimiter rateLimiter = RateLimiter.create(10.0);
        Consumer<String> rateLimited = Fn.rateLimiter(rateLimiter);

        rateLimited.accept("test");
    }

    @Test
    public void testPrintlnWithSeparator() {
        BiConsumer<String, Integer> printEqual = Fn.println("=");
        BiConsumer<String, Integer> printColon = Fn.println(":");
        BiConsumer<String, Integer> printComma = Fn.println(",");

        printEqual.accept("key", 42);
        printColon.accept("key", 42);
        printComma.accept("key", 42);
    }

    @Test
    public void testToSnakeCase() {
        UnaryOperator<String> toSnakeCase = Fn.toSnakeCase();

        assertEquals("hello_world", toSnakeCase.apply("HelloWorld"));
        assertEquals("hello_world", toSnakeCase.apply("helloWorld"));
        assertEquals("hello_world", toSnakeCase.apply("HELLO_WORLD"));
    }

    @Test
    public void testToScreamingSnakeCase() {
        UnaryOperator<String> toScreamingSnakeCase = Fn.toScreamingSnakeCase();

        assertEquals("HELLO_WORLD", toScreamingSnakeCase.apply("HelloWorld"));
        assertEquals("HELLO_WORLD", toScreamingSnakeCase.apply("helloWorld"));
        assertEquals("HELLO_WORLD", toScreamingSnakeCase.apply("hello_world"));
    }

    @Test
    public void testToRuntimeException() {
        Function<Throwable, RuntimeException> toRuntimeException = Fn.toRuntimeException();

        RuntimeException re = toRuntimeException.apply(new IOException("test"));
        assertTrue(re instanceof RuntimeException);
        assertTrue(re.getMessage().contains("test") || re.getCause().getMessage().contains("test"));
    }

    @Test
    public void testR2c() {
        Runnable runnable = () -> System.out.println("Running");
        Callable<Void> callable = Fn.r2c(runnable);

        try {
            assertNull(callable.call());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testR2cWithReturn() {
        Runnable runnable = () -> System.out.println("Running");
        Callable<Integer> callable = Fn.r2c(runnable, 42);

        try {
            assertEquals(Integer.valueOf(42), callable.call());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testC2r() {
        Callable<String> callable = () -> "result";
        Runnable runnable = Fn.c2r(callable);

        runnable.run();
    }

    @Test
    public void testJr2r() {
        java.lang.Runnable javaRunnable = () -> System.out.println("Running");
        Runnable abacusRunnable = Fn.jr2r(javaRunnable);

        abacusRunnable.run();
    }

    @Test
    public void testJc2c() {
        java.util.concurrent.Callable<String> javaCallable = () -> "result";
        Callable<String> abacusCallable = Fn.jc2c(javaCallable);

        assertEquals("result", abacusCallable.call());
    }

    @Test
    public void testJc2r() {
        java.util.concurrent.Callable<String> callable = () -> "result";
        Runnable runnable = Fn.jc2r(callable);

        runnable.run();
    }

    @Test
    public void testRr() {
        Throwables.Runnable<IOException> throwableRunnable = () -> {
            if (false)
                throw new IOException("test");
        };

        Runnable runnable = Fn.rr(throwableRunnable);
        runnable.run();
    }

    @Test
    public void testCc() {
        Throwables.Callable<String, IOException> throwableCallable = () -> {
            if (false)
                throw new IOException("test");
            return "result";
        };

        Callable<String> callable = Fn.cc(throwableCallable);
        assertEquals("result", callable.call());
    }

    @Test
    public void testR() {
        Runnable runnable = Fn.r(() -> System.out.println("test"));
        assertNotNull(runnable);
        runnable.run();
    }

    @Test
    public void testC() {
        Callable<String> callable = Fn.c(() -> "result");
        assertNotNull(callable);
        assertEquals("result", callable.call());
    }

    @Test
    public void testJr() {
        java.lang.Runnable runnable = Fn.jr(() -> System.out.println("test"));
        assertNotNull(runnable);
        runnable.run();
    }

    @Test
    public void testJc() {
        java.util.concurrent.Callable<String> callable = Fn.jc(() -> "result");
        assertNotNull(callable);

        try {
            assertEquals("result", callable.call());
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }
}
