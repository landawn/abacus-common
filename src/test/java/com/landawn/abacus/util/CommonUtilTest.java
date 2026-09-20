package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.type.Type;

public class CommonUtilTest extends CommonUtilTestSupport {

    @Test
    public void testToleranceIndexSearchUsesFuzzyEquality() {
        float[] floats = { Float.NaN, -0.0f, Float.NaN, +0.0f };
        assertEquals(0, N.indexOf(floats, Float.NaN, 0, 0.0f));
        assertEquals(2, N.lastIndexOf(floats, Float.NaN, 3, 0.0f));
        assertEquals(1, N.indexOf(floats, +0.0f, 0, 0.0f));
        assertEquals(3, N.lastIndexOf(floats, -0.0f, 3, 0.0f));
        float[] floatInfinity = { Float.NEGATIVE_INFINITY };
        assertEquals(0, N.indexOf(floatInfinity, Float.POSITIVE_INFINITY, 0, Float.POSITIVE_INFINITY));
        assertEquals(0, N.lastIndexOf(floatInfinity, Float.POSITIVE_INFINITY, 0, Float.POSITIVE_INFINITY));
        assertEquals(-1, N.indexOf(floatInfinity, Float.POSITIVE_INFINITY, 0, 0.0f));
        assertEquals(-1, N.lastIndexOf(floatInfinity, Float.POSITIVE_INFINITY, 0, 0.0f));
        assertEquals(-1, N.indexOf(floatInfinity, Float.NaN, 0, Float.POSITIVE_INFINITY));
        assertEquals(-1, N.lastIndexOf(floatInfinity, Float.NaN, 0, Float.POSITIVE_INFINITY));

        double[] doubles = { Double.NaN, -0.0, Double.NaN, +0.0 };
        assertEquals(0, N.indexOf(doubles, Double.NaN, 0, 0.0));
        assertEquals(2, N.lastIndexOf(doubles, Double.NaN, 3, 0.0));
        assertEquals(1, N.indexOf(doubles, +0.0, 0, 0.0));
        assertEquals(3, N.lastIndexOf(doubles, -0.0, 3, 0.0));
        double[] doubleInfinity = { Double.NEGATIVE_INFINITY };
        assertEquals(0, N.indexOf(doubleInfinity, Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY));
        assertEquals(0, N.lastIndexOf(doubleInfinity, Double.POSITIVE_INFINITY, 0, Double.POSITIVE_INFINITY));
        assertEquals(-1, N.indexOf(doubleInfinity, Double.POSITIVE_INFINITY, 0, 0.0));
        assertEquals(-1, N.lastIndexOf(doubleInfinity, Double.POSITIVE_INFINITY, 0, 0.0));
        assertEquals(-1, N.indexOf(doubleInfinity, Double.NaN, 0, Double.POSITIVE_INFINITY));
        assertEquals(-1, N.lastIndexOf(doubleInfinity, Double.NaN, 0, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testLastEntryMutationDependsOnMapEntrySupport() {
        TreeMap<String, Integer> tree = new TreeMap<>();
        tree.put("a", 1);
        tree.put("b", 2);
        Map.Entry<String, Integer> entry = N.lastEntry(tree).get();
        assertEquals("b", entry.getKey());
        assertEquals(2, entry.setValue(3));
        assertEquals(3, tree.get("b"));

        Map<String, Integer> concurrent = new java.util.concurrent.ConcurrentSkipListMap<>(tree);
        Map.Entry<String, Integer> concurrentEntry = N.lastEntry(concurrent).get();
        assertEquals("b", concurrentEntry.getKey());
        assertThrows(UnsupportedOperationException.class, () -> concurrentEntry.setValue(4));
        Map.Entry<String, Integer> unmodifiableEntry = N.lastEntry(Collections.unmodifiableMap(tree)).get();
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableEntry.setValue(4));
        assertEquals(3, concurrent.get("b"));
        assertEquals(3, tree.get("b"));
    }

    @Test
    public void testFindLastPredicateOrderDependsOnSourceOverload() {
        String[] values = { "a", "bb", "ccc" };
        List<String> visited = new ArrayList<>();
        java.util.function.Predicate<String> predicate = value -> {
            visited.add(value);
            return value.length() > 1;
        };
        assertEquals("ccc", N.findLast(values, predicate).get());
        assertEquals(Arrays.asList("ccc"), visited);
        visited.clear();
        Iterable<String> iterable = () -> Arrays.asList(values).iterator();
        assertEquals("ccc", N.findLast(iterable, predicate).get());
        assertEquals(Arrays.asList("ccc"), visited);
        visited.clear();
        Iterator<String> iterator = Arrays.asList(values).iterator();
        assertEquals("ccc", N.findLast(iterator, predicate).get());
        assertEquals(Arrays.asList(values), visited);
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testAsArrayAliasesWhileCollectionFactoriesCopy() {
        String[] source = { "a", "b" };
        String[] array = N.asArray(source);
        List<String> list = N.asList(source);
        Set<String> set = N.asSet(source);
        assertSame(source, array);
        array[0] = "changed";
        assertEquals("changed", source[0]);
        assertEquals(Arrays.asList("a", "b"), list);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b")), set);
    }

    @Test
    public void testNumericArrayNarrowingDependsOnNumberType() {
        List<Number> values = Arrays.asList(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, new java.math.BigDecimal("1E+400"));
        assertArrayEquals(new byte[] { -1, 0, 0, 0 }, N.toByteArray(values));
        assertArrayEquals(new short[] { -1, 0, 0, 0 }, N.toShortArray(values));
        assertArrayEquals(new int[] { Integer.MAX_VALUE, Integer.MIN_VALUE, 0, 0 }, N.toIntArray(values));
        assertArrayEquals(new long[] { Long.MAX_VALUE, Long.MIN_VALUE, 0, 0 }, N.toLongArray(values));
        assertArrayEquals(new float[] { Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY }, N.toFloatArray(values));
        assertArrayEquals(new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY }, N.toDoubleArray(values));
    }

    @Test
    public void testMutabilityClassificationPreservesExistingIterators() {
        List<String> list = new ArrayList<>(Arrays.asList("value"));
        Iterator<String> listIterator = list.iterator();
        assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(list));
        assertEquals("value", listIterator.next());
        assertFalse(listIterator.hasNext());
        assertEquals(Arrays.asList("value"), list);

        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        Iterator<String> keyIterator = map.keySet().iterator();
        assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(map));
        assertEquals("key", keyIterator.next());
        assertFalse(keyIterator.hasNext());
        assertEquals(Collections.singletonMap("key", 1), map);
    }

    @Test
    public void testNewInstanceSupportsCollectionAndMapInterfaces() {
        List<String> list = N.newInstance(List.class);
        Map<String, Integer> map = N.newInstance(Map.class);
        assertTrue(list.isEmpty());
        assertTrue(map.isEmpty());
        list.add("value");
        map.put("key", 1);
        assertEquals(Arrays.asList("value"), list);
        assertEquals(Collections.singletonMap("key", 1), map);
        assertNotSame(list, N.newInstance(List.class));
        assertNotSame(map, N.newInstance(Map.class));
        assertThrows(IllegalArgumentException.class, () -> N.newInstance(Runnable.class));
    }

    @Test
    public void testCollectionReorderingPreservesRepeatedFailure() {
        final List<java.util.function.Consumer<Collection<Integer>>> reorderings = Arrays.asList(N::reverse, c -> CommonUtil.rotate(c, 1), N::shuffle,
                c -> CommonUtil.shuffle(c, new Random(1)));
        for (final java.util.function.Consumer<Collection<Integer>> reordering : reorderings) {
            for (final Throwable failure : Arrays.asList(new IllegalStateException("repopulation rejected"), new AssertionError("repopulation rejected"))) {
                final int[] attempts = { 0 };
                final Collection<Integer> values = new LinkedHashSet<>() {
                    @Override
                    public boolean addAll(final Collection<? extends Integer> c) {
                        attempts[0]++;
                        if (failure instanceof RuntimeException) {
                            throw (RuntimeException) failure;
                        }
                        throw (Error) failure;
                    }
                };
                values.add(1);
                values.add(2);
                assertSame(failure, assertThrows(Throwable.class, () -> reordering.accept(values)));
                assertEquals(2, attempts[0], "repopulation and restoration must both be attempted");
                assertEquals(0, failure.getSuppressed().length);
                assertTrue(values.isEmpty());
            }
        }
    }

    @Test
    public void testCheckFromToIndex() {
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(0, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(0, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(5, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromToIndex(10, 10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(5, 4, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 11, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(11, 11, 10));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromToIndex(0, 0, -1)).getMessage().contains("negative"));
    }

    @Test
    public void testCheckFromIndexSize() {
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(0, 5, 10));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(0, 0, 0));
        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(10, 0, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(-1, 5, 10));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkFromIndexSize(0, 5, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(6, 5, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(0, 11, 10));
    }

    @Test
    public void testCheckIndex() {
        assertEquals(0, CommonUtil.checkIndex(0, 10));
        assertEquals(9, CommonUtil.checkIndex(9, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(10, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkIndex(0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkIndex(0, -1));
    }

    @Test
    public void testCheckPositionIndex() {
        assertEquals(0, CommonUtil.checkPositionIndex(0, 0));
        assertEquals(10, CommonUtil.checkPositionIndex(10, 10));
        assertEquals(5, CommonUtil.checkPositionIndex(5, 10, "myPosition"));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(11, 10));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkPositionIndex(0, -1));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(-1, 10, "testPosIndex")).getMessage()
                .contains("testPosIndex (-1) must not be negative"));
        assertTrue(assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkPositionIndex(11, 10, "testPosIndex")).getMessage()
                .contains("testPosIndex (11) must not be greater than size (10)"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkPositionIndex(0, -1, "testPosIndex")).getMessage()
                .contains("negative size: -1"));
    }

    @Test
    public void testCheckKeyNotNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", null);
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(map));
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(Collections.emptyMap()));
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull((Map<?, ?>) null));
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(map, "myMap"));
        assertDoesNotThrow(() -> CommonUtil.checkKeyNotNull(Collections.emptyMap(), "myMap"));
        Map<String, Integer> nullKey = new HashMap<>();
        nullKey.put(null, 1);
        assertEquals("null key is found in Map", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(nullKey)).getMessage());
        assertEquals("null key is found in myMap",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(nullKey, "myMap")).getMessage());
        assertEquals("Custom error for null key",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkKeyNotNull(nullKey, "Custom error for null key")).getMessage());
    }

    @Test
    public void testCheckValueNotNull() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(map));
        Map<Object, String> nullKey = new HashMap<>();
        nullKey.put(null, "value");
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull(nullKey));
        assertDoesNotThrow(() -> CommonUtil.checkValueNotNull((Map<?, ?>) null, "myMap"));
        Map<String, Integer> nullValue = new HashMap<>();
        nullValue.put("a", null);
        assertEquals("null value is found in Map", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(nullValue)).getMessage());
        assertEquals("null value is found in myMap",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(nullValue, "myMap")).getMessage());
        assertEquals("Custom error for null value",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkValueNotNull(nullValue, "Custom error for null value")).getMessage());
    }

    @Test
    public void testFormat() {
        assertEquals("template: [a, b, c]", CommonUtil.format("template", "a", "b", "c"));
        assertEquals("Hello World: [arg2, arg3]", CommonUtil.format("Hello {}", "World", "arg2", "arg3"));
        assertEquals("a and b: [c]", CommonUtil.format("{} and {}", "a", "b", "c"));
        assertEquals("x y z", CommonUtil.format("{} {} {}", "x", "y", "z"));
        assertEquals("Hello World [extra1, extra2]", CommonUtil.format("Hello {}", new Object[] { "World", "extra1", "extra2" }));
        assertEquals("no placeholders [x, y]", CommonUtil.format("no placeholders", new Object[] { "x", "y" }));
        assertEquals("template has no placeholder: [someArg]", CommonUtil.format("template has no placeholder", "someArg"));
        assertEquals("value 42 is invalid",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "value {} is invalid", 42)).getMessage());
        assertEquals("first and second",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s and %s", "first", "second")).getMessage());
        assertEquals("error message: [arg1, arg2]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "error message", "arg1", "arg2")).getMessage());
        assertEquals("only one %s %s",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "%s %s %s", "only one")).getMessage());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "myArg")).getMessage().contains("myArg"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "myArg")).getMessage().contains("myArg"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("  ", "myArg")).getMessage().contains("myArg"));
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgNotNegative(0, "zero");
            CommonUtil.checkArgPositive(1, "one");
            CommonUtil.checkElementIndex(0, 1);
            CommonUtil.checkPositionIndex(1, 1);
            CommonUtil.checkElementNotNull(new Object[0]);
            CommonUtil.checkElementNotNull(new ArrayList<>());
            CommonUtil.checkKeyNotNull(new HashMap<>());
            CommonUtil.checkValueNotNull(new HashMap<>());
        });
    }

    @Test
    public void testRequireNonNull() {
        String obj = "test";
        assertSame(obj, CommonUtil.requireNonNull(obj));
        assertSame(obj, CommonUtil.requireNonNull(obj, "testObject"));
        assertSame(obj, CommonUtil.requireNonNull(obj, () -> "This should not be called"));
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null));
        assertEquals("'testObject' cannot be null", assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "testObject")).getMessage());
        assertEquals("Custom error message for null object",
                assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "Custom error message for null object")).getMessage());
        final boolean[] called = { false };
        Supplier<String> supplier = () -> {
            called[0] = true;
            return "paramName";
        };
        assertEquals("'paramName' cannot be null", assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, supplier)).getMessage());
        assertTrue(called[0]);
        called[0] = false;
        assertSame(obj, CommonUtil.requireNonNull(obj, () -> {
            called[0] = true;
            return "Should not be called";
        }));
        assertFalse(called[0]);
        assertEquals("Custom detailed error message from supplier",
                assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, () -> "Custom detailed error message from supplier"))
                        .getMessage());
    }

    @Test
    public void testConcat() {
        assertTrue(CommonUtil.equals(N.concat(new String[] { "a", "b" }, new String[] { "c", "d" }), new String[] { "a", "b", "c", "d" }));
        assertTrue(CommonUtil.equals(N.concat(new int[] { 1, 2 }, new int[] { 3, 4 }), new int[] { 1, 2, 3, 4 }));
    }

    @Test
    public void testLen() {
        assertEquals(4, CommonUtil.len("test"));
        assertEquals(0, CommonUtil.len(""));
        assertEquals(0, CommonUtil.len((CharSequence) null));
        assertEquals(2, CommonUtil.len(new boolean[] { true, false }));
        assertEquals(0, CommonUtil.len((boolean[]) null));
        assertEquals(3, CommonUtil.len(new char[] { 'a', 'b', 'c' }));
        assertEquals(2, CommonUtil.len(new byte[] { 1, 2 }));
        assertEquals(2, CommonUtil.len(new short[] { 10, 20 }));
        assertEquals(3, CommonUtil.len(new int[] { 1, 2, 3 }));
        assertEquals(0, CommonUtil.len((int[]) null));
        assertEquals(3, CommonUtil.len(new long[] { 1L, 2L, 3L }));
        assertEquals(3, CommonUtil.len(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals(3, CommonUtil.len(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals(3, CommonUtil.len(new Object[] { "a", "b", "c" }));
        assertEquals(0, CommonUtil.len((Object[]) null));
    }

    @Test
    public void testSize() {
        assertEquals(3, CommonUtil.size(Arrays.asList("a", "b", "c")));
        assertEquals(0, CommonUtil.size((Collection<?>) null));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, CommonUtil.size(map));
        assertEquals(0, CommonUtil.size((Map<?, ?>) null));
        assertEquals(0, CommonUtil.size((PrimitiveList) null));
        assertEquals(0, CommonUtil.size(IntList.of()));
        assertEquals(3, CommonUtil.size(IntList.of(1, 2, 3)));
    }

    @Test
    public void testBooleanChecks() {
        assertTrue(CommonUtil.isTrue(Boolean.TRUE));
        assertFalse(CommonUtil.isTrue(Boolean.FALSE));
        assertFalse(CommonUtil.isTrue(null));
        assertTrue(CommonUtil.isNotTrue(null));
        assertTrue(CommonUtil.isFalse(Boolean.FALSE));
        assertFalse(CommonUtil.isFalse(null));
        assertTrue(CommonUtil.isNotFalse(null));
        assertFalse(CommonUtil.isNotFalse(Boolean.FALSE));
    }

    @Test
    public void testTypeOf() {
        Type<String> stringType = CommonUtil.typeOf(String.class);
        assertEquals(String.class, stringType.javaType());
        assertEquals(int.class, CommonUtil.typeOf(int.class).javaType());
        assertEquals(String.class, CommonUtil.typeOf("java.lang.String").javaType());
        assertEquals(int.class, CommonUtil.typeOf("int").javaType());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.typeOf((String) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.typeOf((Class<?>) null));
    }

    @Test
    public void testStringOf() {
        assertEquals("true", CommonUtil.stringOf(true));
        assertEquals("a", CommonUtil.stringOf('a'));
        assertEquals("0", CommonUtil.stringOf('0'));
        assertEquals("127", CommonUtil.stringOf((byte) 127));
        assertEquals("-128", CommonUtil.stringOf((byte) -128));
        assertEquals("32767", CommonUtil.stringOf(Short.MAX_VALUE));
        assertEquals("-32768", CommonUtil.stringOf(Short.MIN_VALUE));
        assertEquals("2147483647", CommonUtil.stringOf(Integer.MAX_VALUE));
        assertEquals("-2147483648", CommonUtil.stringOf(Integer.MIN_VALUE));
        assertEquals("9223372036854775807", CommonUtil.stringOf(Long.MAX_VALUE));
        assertEquals("-9223372036854775808", CommonUtil.stringOf(Long.MIN_VALUE));
        assertEquals("3.14", CommonUtil.stringOf(3.14f));
        assertEquals("3.14159", CommonUtil.stringOf(3.14159d));
        assertEquals("test", CommonUtil.stringOf("test"));
        assertNull(CommonUtil.stringOf((Object) null));
    }

    @Test
    public void testValueOfAndConvert() {
        assertEquals(Integer.valueOf(123), CommonUtil.valueOf("123", Integer.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.registerConverter(String.class, (obj, targetClass) -> obj));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.registerConverter(String[].class, (obj, targetClass) -> obj));
        Runnable source = (Runnable) java.lang.reflect.Proxy.newProxyInstance(Runnable.class.getClassLoader(), new Class<?>[] { Runnable.class },
                (proxy, method, args) -> null);
        assertTrue(CommonUtil.registerConverter(source.getClass(), (obj, targetClass) -> {
            if (targetClass == Integer.class) {
                return Integer.valueOf(7);
            }
            if (targetClass == String.class) {
                return "converted:7";
            }
            return null;
        }));
        assertEquals(7, CommonUtil.convert(source, Integer.class).intValue());
        assertEquals("converted:7", CommonUtil.convert(source, String.class));
        assertFalse(CommonUtil.registerConverter(source.getClass(), (obj, targetClass) -> obj));
    }

    @Test
    public void testCastIfAssignable() {
        assertEquals("test", CommonUtil.castIfAssignable("test", String.class).get());
        assertFalse(CommonUtil.castIfAssignable("test", Integer.class).isPresent());
        assertEquals("hello", CommonUtil.castIfAssignable("hello", CommonUtil.typeOf(String.class)).get());
        assertFalse(CommonUtil.castIfAssignable("notAnInt", CommonUtil.typeOf(Integer.class)).isPresent());
    }

    @Test
    public void testNegate() {
        assertEquals(Boolean.FALSE, CommonUtil.negate(Boolean.TRUE));
        assertEquals(Boolean.TRUE, CommonUtil.negate(Boolean.FALSE));
        assertNull(CommonUtil.negate((Boolean) null));
        boolean[] array = { true, false, true, false };
        CommonUtil.negate(array);
        assertArrayEquals(new boolean[] { false, true, false, true }, array);
        CommonUtil.negate(new boolean[0]);
        CommonUtil.negate((boolean[]) null);
        boolean[] range = { true, false, true, false, true };
        CommonUtil.negate(range, 1, 4);
        assertArrayEquals(new boolean[] { true, true, false, true, true }, range);
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.negate(range, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.negate(range, 3, 2));
    }

    @Test
    public void testEnumHelpers() {
        enum TestEnum {
            A, B, C
        }
        assertEquals(3, CommonUtil.enumListOf(TestEnum.class).size());
        assertNotNull(CommonUtil.enumSetOf(TestEnum.class));
        assertFalse(CommonUtil.enumNameMap(TestEnum.class).isEmpty());
    }

    @Test
    public void testUnmodifiable() {
        assertTrue(CommonUtil.unmodifiableCollection(null).isEmpty());
        Collection<String> unmodCol = CommonUtil.unmodifiableCollection(new ArrayList<>(Arrays.asList("a", "b")));
        assertThrows(UnsupportedOperationException.class, () -> unmodCol.add("c"));
        assertTrue(CommonUtil.unmodifiableList(null).isEmpty());
        List<String> unmod1 = CommonUtil.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b")));
        assertSame(unmod1, CommonUtil.unmodifiableList(unmod1));
        assertThrows(UnsupportedOperationException.class, () -> unmod1.add("d"));
        assertTrue(CommonUtil.unmodifiableSet(null).isEmpty());
        Set<String> unmodSet = CommonUtil.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("a", "b")));
        assertThrows(UnsupportedOperationException.class, () -> unmodSet.add("d"));
        SortedSet<Integer> unmodSortedSet = CommonUtil.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(3, 1, 2)));
        assertThrows(UnsupportedOperationException.class, () -> unmodSortedSet.add(4));
        NavigableSet<Integer> unmodNavSet = CommonUtil.unmodifiableNavigableSet(new TreeSet<>(Arrays.asList(3, 1, 2)));
        assertThrows(UnsupportedOperationException.class, () -> unmodNavSet.add(4));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> unmodMap = CommonUtil.unmodifiableMap(map);
        assertThrows(UnsupportedOperationException.class, () -> unmodMap.put("b", 2));
        SortedMap<String, Integer> sorted = new TreeMap<>();
        sorted.put("a", 1);
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.unmodifiableSortedMap(sorted).put("b", 2));
        NavigableMap<String, Integer> nav = new TreeMap<>();
        nav.put("a", 1);
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.unmodifiableNavigableMap(nav).put("b", 2));
        SortedSet<String> alreadySorted = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList("a")));
        assertSame(alreadySorted, CommonUtil.unmodifiableSortedSet(alreadySorted));
        NavigableSet<String> alreadyNav = Collections.unmodifiableNavigableSet(new TreeSet<>(Arrays.asList("a")));
        assertSame(alreadyNav, CommonUtil.unmodifiableNavigableSet(alreadyNav));
        Map<String, Integer> alreadyMap = Collections.unmodifiableMap(map);
        assertSame(alreadyMap, CommonUtil.unmodifiableMap(alreadyMap));
        SortedMap<String, Integer> alreadySortedMap = Collections.unmodifiableSortedMap(sorted);
        assertSame(alreadySortedMap, CommonUtil.unmodifiableSortedMap(alreadySortedMap));
        NavigableMap<String, Integer> alreadyNavMap = Collections.unmodifiableNavigableMap(nav);
        assertSame(alreadyNavMap, CommonUtil.unmodifiableNavigableMap(alreadyNavMap));
        assertEquals(Mutability.UNKNOWN, CommonUtil.mutabilityOf(Arrays.asList("a", "b")));
        final List<String> view = CommonUtil.unmodifiableList(Arrays.asList("x", "y"));
        assertThrows(UnsupportedOperationException.class, () -> view.set(0, "z"));
        final Map<String, Integer> backing = new HashMap<>();
        backing.put("k", 1);
        assertEquals(Mutability.UNKNOWN, CommonUtil.mutabilityOf(backing.keySet()));
        final Collection<String> keys = CommonUtil.unmodifiableCollection(backing.keySet());
        assertThrows(UnsupportedOperationException.class, () -> keys.remove("k"));
        assertEquals(1, backing.size());
    }

    @Test
    public void testMerge() {
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6),
                N.merge(Arrays.asList(Arrays.asList(1, 3, 5), Arrays.asList(2, 4, 6)), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND));
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1", "col2"), CommonUtil.asSingletonList(new Object[] { "A", 1 }));
        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col1", "col2"), CommonUtil.asSingletonList(new Object[] { "B", 2 }));
        Dataset ds3 = CommonUtil.newDataset(Arrays.asList("col1", "col2"), CommonUtil.asSingletonList(new Object[] { "C", 3 }));
        assertEquals(3, CommonUtil.merge(Arrays.asList(ds1, ds2, ds3), true).size());
        Dataset a = CommonUtil.newDataset(Arrays.asList("col1"), CommonUtil.asSingletonList(new Object[] { "A" }));
        Dataset b = CommonUtil.newDataset(Arrays.asList("col2"), CommonUtil.asSingletonList(new Object[] { 1 }));
        assertEquals(3, CommonUtil.merge(Arrays.asList(a, b, a), false).size());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(a, b), true));
        Dataset two = CommonUtil.merge(CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }, new Object[] { "B" })),
                CommonUtil.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }, new Object[] { 2 })));
        assertEquals(4, two.size());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(null, a));
        Dataset three = CommonUtil.merge(a, b, CommonUtil.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true })));
        assertEquals(3, three.columnCount());
        assertTrue(CommonUtil.merge(new ArrayList<>()).isEmpty());
        assertTrue(CommonUtil.merge((Collection<Dataset>) null).isEmpty());
        assertEquals(1, CommonUtil.merge(Arrays.asList(a)).size());
    }

    @Test
    public void testSlice() {
        assertEquals(Arrays.asList("b", "c"), CommonUtil.slice(new String[] { "a", "b", "c", "d", "e" }, 1, 3));
        assertEquals(3, CommonUtil.slice(Arrays.asList("a", "b", "c", "d", "e"), 1, 4).size());
        assertEquals(2, CommonUtil.slice(new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d")), 1, 3).size());
    }

    @Test
    public void testContainsSameElements() {
        assertTrue(N.containsAll(Arrays.asList("a", "b", "c", "d"), Arrays.asList("a", "c")));
        assertFalse(N.containsAll(Arrays.asList("a", "b"), Arrays.asList("a", "z")));
        assertTrue(N.containsAny(Arrays.asList("a", "b", "c"), Arrays.asList("a", "z")));
        assertFalse(N.containsAny(Arrays.asList("a", "b"), Arrays.asList("x", "z")));
        assertTrue(N.disjoint(Arrays.asList("a", "b"), Arrays.asList("d", "e")));
        assertFalse(N.disjoint(Arrays.asList("a", "b", "c"), Arrays.asList("c", "d")));
        List<String> a = Arrays.asList("a", "b", "c", "b");
        assertTrue(CommonUtil.containsSameElements(a, Arrays.asList("b", "a", "c", "b")));
        assertFalse(CommonUtil.containsSameElements(a, Arrays.asList("a", "b", "c")));
        assertTrue(CommonUtil.containsSameElements((Collection<?>) null, (Collection<?>) null));
        assertTrue(CommonUtil.containsSameElements(new boolean[] { true, false, true, false }, new boolean[] { false, true, false, true }));
        assertFalse(CommonUtil.containsSameElements(new boolean[] { true, false, true, false }, new boolean[] { true, false, true }));
        assertTrue(CommonUtil.containsSameElements((boolean[]) null, (boolean[]) null));
        assertTrue(CommonUtil.containsSameElements(new char[] { 'a', 'b', 'c', 'b' }, new char[] { 'b', 'a', 'c', 'b' }));
        assertTrue(CommonUtil.containsSameElements(new byte[] { 1, 2, 3, 2 }, new byte[] { 2, 1, 3, 2 }));
        assertTrue(CommonUtil.containsSameElements(new short[] { 1, 2, 3, 2 }, new short[] { 2, 1, 3, 2 }));
        assertTrue(CommonUtil.containsSameElements(new int[] { 1, 2, 3, 2 }, new int[] { 2, 1, 3, 2 }));
        assertFalse(CommonUtil.containsSameElements(new int[] { 1, 2, 3, 2 }, new int[] { 1, 2, 2, 2 }));
        assertTrue(CommonUtil.containsSameElements(new long[] { 1L, 2L, 3L, 2L }, new long[] { 2L, 1L, 3L, 2L }));
        assertTrue(CommonUtil.containsSameElements(new float[] { 1.0f, 2.0f, 3.0f, 2.0f }, new float[] { 2.0f, 1.0f, 3.0f, 2.0f }));
        assertTrue(CommonUtil.containsSameElements(new double[] { 1.0, 2.0, 3.0, 2.0 }, new double[] { 2.0, 1.0, 3.0, 2.0 }));
        assertTrue(CommonUtil.containsSameElements(new String[] { "a", "b", "c", "b" }, new String[] { "b", "a", "c", "b" }));
        assertTrue(CommonUtil.containsSameElements(new String[] { "a", null, "b", null }, new String[] { null, "a", null, "b" }));
        assertTrue(N.contains(new boolean[] { true, false, true }, true));
        assertFalse(N.contains((boolean[]) null, true));
        assertTrue(N.contains(new int[] { 1, 2, 3 }, 2));
        assertTrue(N.contains(new String[] { "a", "b", "c" }, "b"));
    }

    @Test
    public void testEmpty() {
        assertSame(CommonUtil.emptyList(), CommonUtil.emptyList());
        assertSame(CommonUtil.emptySet(), CommonUtil.emptySet());
        assertSame(CommonUtil.emptySortedSet(), CommonUtil.emptySortedSet());
        assertSame(CommonUtil.emptyNavigableSet(), CommonUtil.emptyNavigableSet());
        assertSame(CommonUtil.emptyMap(), CommonUtil.emptyMap());
        assertNotNull(CommonUtil.emptySortedMap());
        assertNotNull(CommonUtil.emptyNavigableMap());
        assertFalse(CommonUtil.emptyIterator().hasNext());
        assertFalse(CommonUtil.emptyListIterator().hasNext());
        assertNotNull(CommonUtil.emptyInputStream());
        assertTrue(CommonUtil.emptyDataset().isEmpty());
    }

    @Test
    public void testComparisons() {
        Comparator<Integer> reverse = (a, b) -> b.compareTo(a);
        assertTrue(CommonUtil.lessThan(3, 5));
        assertFalse(CommonUtil.lessThan(3, 3));
        assertFalse(CommonUtil.lessThan(1, 2, reverse));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.lessThan(1, 2, null));
        assertTrue(CommonUtil.lessThanOrEqual(3, 3));
        assertFalse(CommonUtil.lessThanOrEqual(1, 2, reverse));
        assertTrue(CommonUtil.le(3, 3));
        assertTrue(CommonUtil.le(2, 1, reverse));
        assertTrue(CommonUtil.greaterThan(5, 3));
        assertTrue(CommonUtil.greaterThan(1, 2, reverse));
        assertTrue(CommonUtil.greaterThanOrEqual(3, 3));
        assertTrue(CommonUtil.ge(3, 3));
        assertTrue(CommonUtil.ge(1, 2, reverse));
        assertTrue(CommonUtil.gtAndLt(5, 3, 7));
        assertTrue(CommonUtil.gtAndLt(5, 10, 1, reverse));
        assertTrue(CommonUtil.geAndLt(3, 3, 7));
        assertTrue(CommonUtil.geAndLt(10, 10, 1, reverse));
        assertTrue(CommonUtil.geAndLe(7, 3, 7));
        assertTrue(CommonUtil.geAndLe(1, 10, 1, reverse));
        assertTrue(CommonUtil.gtAndLe(7, 3, 7));
        assertFalse(CommonUtil.gtAndLe(10, 10, 1, reverse));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.gtAndLe(5, 1, 10, null));
    }

    @Test
    public void testGetElement() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("b", CommonUtil.getElement(list, 1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement(list, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.getElement(list, 4));
        assertEquals("b", CommonUtil.getElement(new LinkedHashSet<>(list), 1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement((Iterable<String>) null, 0));
        assertEquals("d", CommonUtil.getElement(list.iterator(), 3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.getElement((Iterator<String>) null, 0));
        assertEquals("only", CommonUtil.getOnlyElement(Arrays.asList("only")).get());
        assertFalse(CommonUtil.getOnlyElement(Collections.emptyList()).isPresent());
        assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.getOnlyElement((Iterable<String>) null).isPresent());
        assertEquals("only", CommonUtil.getOnlyElement(Arrays.asList("only").iterator()).get());
        assertFalse(CommonUtil.getOnlyElement(Collections.emptyIterator()).isPresent());
        assertThrows(TooManyElementsException.class, () -> CommonUtil.getOnlyElement(Arrays.asList("a", "b").iterator()));
        assertFalse(CommonUtil.getOnlyElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testSwap() {
        boolean[] bools = { true, false, true };
        CommonUtil.swap(bools, 0, 1);
        assertTrue(CommonUtil.equals(bools, new boolean[] { false, true, true }));
        char[] chars = { 'a', 'b', 'c' };
        CommonUtil.swap(chars, 0, 2);
        assertEquals('c', chars[0]);
        byte[] bytes = { 1, 2, 3 };
        CommonUtil.swap(bytes, 0, 2);
        assertEquals((byte) 3, bytes[0]);
        short[] shorts = { 1, 2, 3 };
        CommonUtil.swap(shorts, 0, 2);
        assertEquals((short) 3, shorts[0]);
        int[] ints = { 1, 2, 3 };
        CommonUtil.swap(ints, 0, 2);
        assertEquals(3, ints[0]);
        long[] longs = { 1L, 2L, 3L };
        CommonUtil.swap(longs, 0, 2);
        assertEquals(3L, longs[0]);
        float[] floats = { 1f, 2f, 3f };
        CommonUtil.swap(floats, 0, 2);
        assertEquals(3f, floats[0]);
        double[] doubles = { 1d, 2d, 3d };
        CommonUtil.swap(doubles, 0, 2);
        assertEquals(3d, doubles[0]);
        String[] objs = { "a", "b", "c" };
        CommonUtil.swap(objs, 0, 2);
        assertEquals("c", objs[0]);
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.swap(list, 1, 3);
        assertEquals("d", list.get(1));
        Pair<String, String> pair = Pair.of("left", "right");
        CommonUtil.swap(pair);
        assertEquals("right", pair.left());
        Triple<String, Integer, String> triple = Triple.of("left", 42, "right");
        CommonUtil.swap(triple);
        assertEquals("right", triple.left());
        assertEquals(42, triple.middle());
        Pair<Integer, Integer> p = Pair.of(1, 2);
        assertTrue(CommonUtil.swapIf(p, x -> x.left() < x.right()));
        assertFalse(CommonUtil.swapIf(p, x -> x.left() < x.right()));
        Triple<Integer, String, Integer> t = Triple.of(1, "middle", 2);
        assertTrue(CommonUtil.swapIf(t, x -> x.left() < x.right()));
        assertFalse(CommonUtil.swapIf(t, x -> x.left() < x.right()));
    }

    @Test
    public void testPadAndCycle() {
        List<String> left = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(CommonUtil.padLeft(left, 5, "x"));
        assertEquals(Arrays.asList("x", "x", "a", "b", "c"), left);
        assertFalse(CommonUtil.padLeft(left, 3, "x"));
        List<String> leftNull = new ArrayList<>(Arrays.asList("a"));
        assertTrue(CommonUtil.padLeft(leftNull, 3, null));
        assertNull(leftNull.get(0));
        List<String> right = new ArrayList<>(Arrays.asList("a", "b", "c"));
        assertTrue(CommonUtil.padRight(right, 5, "x"));
        assertEquals("x", right.get(4));
        assertFalse(CommonUtil.padRight(right, 2, "x"));
        List<String> rightNull = new ArrayList<>(Arrays.asList("a"));
        assertTrue(CommonUtil.padRight(rightNull, 3, null));
        assertNull(rightNull.get(2));
        assertEquals(Arrays.asList(1, 2, 3, 1, 2), CommonUtil.cycleToSize(Arrays.asList(1, 2, 3), 5));
        assertEquals(6, CommonUtil.cycleToSize(Arrays.asList(1, 2), 6).size());
        assertEquals(0, CommonUtil.cycleToSize(Arrays.asList(1, 2), 0).size());
    }

    @Test
    public void testMiscHelpers() {
        double[] arrWithNaN = { 1.0, Double.NaN, 3.0, Double.NaN };
        assertEquals(1, CommonUtil.indexOf(arrWithNaN, Double.NaN));
        assertEquals(3, CommonUtil.lastIndexOf(arrWithNaN, Double.NaN));
        double[] arrWithInf = { 1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY };
        assertEquals(1, CommonUtil.indexOf(arrWithInf, Double.POSITIVE_INFINITY));
        Integer[] allSame = { 5, 5, 5, 5, 5 };
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, CommonUtil.indicesOfMin(allSame));
        assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, CommonUtil.indicesOfMax(allSame));
        assertEquals(-1, CommonUtil.indexOf(new String[] { "a", "b", "c" }, "a", Integer.MAX_VALUE));
        assertEquals(-1, CommonUtil.lastIndexOf(new String[] { "a", "b", "c" }, "a", Integer.MIN_VALUE));
        Deque<String> deque = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
        assertNotNull(CommonUtil.getDescendingIteratorIfPossible(deque));
        assertNull(CommonUtil.getDescendingIteratorIfPossible(new ArrayList<>(Arrays.asList("a"))));
        Runnable mask = CommonUtil.createMask(Runnable.class);
        assertThrows(UnsupportedOperationException.class, () -> mask.run());
        assertEquals(1, N.min(new int[] { 3, 1, 4, 1, 5 }));
        assertEquals("a", N.min(new String[] { "c", "a", "b" }));
        assertEquals(5, N.max(new int[] { 3, 1, 4, 1, 5 }));
        assertEquals("c", N.max(new String[] { "c", "a", "b" }));
        assertEquals(3, N.lowerMedian(new int[] { 3, 1, 4, 1, 5 }));
        assertEquals(15, N.sum(new int[] { 1, 2, 3, 4, 5 }));
        assertEquals(3.0, N.average(new int[] { 1, 2, 3, 4, 5 }), 0.001);
        assertTrue(Beans.getPropNameList(TestBean.class).contains("name"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNameList((Class<?>) null));
        Set<String> exclude = new HashSet<>();
        exclude.add("name");
        assertFalse(Beans.getPropNames(TestBean.class, exclude).contains("name"));
        TestBean bean = new TestBean();
        bean.setName("test");
        assertEquals("test", Beans.getPropValue(bean, "name", true));
        assertNull(Beans.getPropValue(bean, "nonExistent", true));
        assertThrows(RuntimeException.class, () -> Beans.getPropValue(bean, "nonExistent", false));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(null, "name", true));
        for (int n = 1; n <= 4096; n++) {
            final int cap = CommonUtil.initHashCapacity(n);
            final int table = hashMapTableSizeFor(cap);
            final int threshold = (int) (table * 0.75f);
            assertTrue(threshold >= n, "expectedSize=" + n + " -> capacity=" + cap);
        }
        final class TrackingList extends AbstractList<Integer> {
            private final List<Integer> values = new ArrayList<>(Arrays.asList(9, 3, 1, 2, 8));
            private final List<Integer> writtenIndices = new ArrayList<>();

            @Override
            public Integer get(final int index) {
                return values.get(index);
            }

            @Override
            public int size() {
                return values.size();
            }

            @Override
            public Integer set(final int index, final Integer element) {
                writtenIndices.add(index);
                return values.set(index, element);
            }
        }
        final TrackingList sequential = new TrackingList();
        CommonUtil.sort(sequential, 1, 4, Comparator.naturalOrder());
        assertEquals(Arrays.asList(9, 1, 2, 3, 8), sequential);
        assertEquals(Arrays.asList(1, 2, 3), sequential.writtenIndices);
        final TrackingList parallel = new TrackingList();
        CommonUtil.parallelSort(parallel, 1, 4, Comparator.naturalOrder());
        assertEquals(Arrays.asList(9, 1, 2, 3, 8), parallel);
        assertEquals(Arrays.asList(1, 2, 3), parallel.writtenIndices);
    }

    @Test
    public void reviewFixes20260906_jdkUnmodifiableWrappersAreAllRecognised() {
        final java.util.List<String> nonRandomAccessView = java.util.Collections.unmodifiableList(new java.util.LinkedList<>(java.util.List.of("a", "b")));
        assertEquals("UnmodifiableList", nonRandomAccessView.getClass().getSimpleName());
        assertSame(nonRandomAccessView, CommonUtil.unmodifiableCollection(nonRandomAccessView));
        assertSame(nonRandomAccessView, CommonUtil.unmodifiableList(nonRandomAccessView));
        final java.util.Collection<String> seqColl = java.util.Collections.unmodifiableSequencedCollection(new java.util.ArrayList<>(java.util.List.of("a")));
        assertSame(seqColl, CommonUtil.unmodifiableCollection(seqColl));
        final java.util.Set<String> seqSet = java.util.Collections.unmodifiableSequencedSet(new java.util.LinkedHashSet<>(java.util.List.of("a")));
        assertSame(seqSet, CommonUtil.unmodifiableSet(seqSet));
        final java.util.Map<String, String> seqMap = java.util.Collections.unmodifiableSequencedMap(new java.util.LinkedHashMap<>(java.util.Map.of("k", "v")));
        assertSame(seqMap, CommonUtil.unmodifiableMap(seqMap));
        final java.util.List<String> randomAccessView = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(java.util.List.of("a")));
        assertSame(randomAccessView, CommonUtil.unmodifiableList(randomAccessView));
        final java.util.List<String> mutable = new java.util.ArrayList<>(java.util.List.of("a"));
        assertNotSame(mutable, CommonUtil.unmodifiableList(mutable));
    }

    @Test
    public void reviewFixes20260906_requireNonNullDeclaresOnlyNullPointerException() throws Exception {
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null));
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, "arg"));
        assertEquals("v", CommonUtil.requireNonNull("v"));
        assertArrayEquals(new Class<?>[] { NullPointerException.class }, N.class.getMethod("requireNonNull", Object.class).getExceptionTypes());
        assertArrayEquals(new Class<?>[] { NullPointerException.class }, N.class.getMethod("requireNonNull", Object.class, String.class).getExceptionTypes());
        assertArrayEquals(new Class<?>[] { NullPointerException.class },
                N.class.getMethod("requireNonNull", Object.class, java.util.function.Supplier.class).getExceptionTypes());
    }

    @Test
    public void reviewFixes20260906_toStringOfASelfReferentialCollectionIsNotCycleSafe() {
        final java.util.List<Object> cyclic = new java.util.ArrayList<>();
        cyclic.add("a");
        cyclic.add(cyclic);
        assertEquals("[a, (this Collection)]", cyclic.toString());
        assertThrows(StackOverflowError.class, () -> CommonUtil.toString(cyclic));
        assertEquals("[[a, (this Collection)]]", CommonUtil.deepToString(new Object[] { cyclic }));
    }

    @Test
    public void reviewFixes20260906_varargsFormatKeepsItsGuavaShape() {
        assertEquals("No placeholder: [a, b, c]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "No placeholder", "a", "b", "c")).getMessage());
        assertEquals("No placeholder [a, b, c, d]",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "No placeholder", "a", "b", "c", "d")).getMessage());
    }

    @Test
    public void reviewFixes20260906_rotateByAMultipleOfTheSizeIsANoOpForEveryCollection() {
        final java.util.Collection<String> unmodifiableList = java.util.Collections
                .unmodifiableList(new java.util.ArrayList<>(java.util.Arrays.asList("a", "b", "c")));
        final java.util.Collection<String> unmodifiableSet = java.util.Collections
                .unmodifiableSet(new java.util.LinkedHashSet<>(java.util.Arrays.asList("a", "b", "c")));
        assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiableList, 3));
        assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiableSet, 3));
        assertDoesNotThrow(() -> CommonUtil.rotate(unmodifiableSet, 0));
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.rotate(unmodifiableSet, 1));
        final java.util.Set<String> set = new java.util.LinkedHashSet<>(java.util.Arrays.asList("a", "b", "c"));
        final java.util.Iterator<String> it = set.iterator();
        CommonUtil.rotate(set, 0);
        assertEquals("a", it.next());
        CommonUtil.rotate(set, 1);
        final java.util.List<String> list = new java.util.ArrayList<>(java.util.Arrays.asList("a", "b", "c"));
        CommonUtil.rotate(list, 1);
        assertEquals(list, new java.util.ArrayList<>(set));
    }

    @Test
    public void reviewFixes20260906_reflectiveCopyReportsWhatItsJavadocSays() {
        final Object dest = new String[3];
        final Object src = new String[] { "a", "b", "c", "d" };
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copy((Object) null, 0, dest, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copy(src, 0, (Object) null, 0, 1));
        assertThrows(NullPointerException.class, () -> CommonUtil.copy((Object) null, 0, dest, 0, 0));
        assertThrows(NullPointerException.class, () -> CommonUtil.copy(src, 0, (Object) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copy("abc", 0, dest, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copy(src, 0, "abc", 0, 1));
        CommonUtil.copy(src, 1, dest, 0, 2);
        assertArrayEquals(new String[] { "b", "c", null }, (String[]) dest);
    }

    @Test
    public void reviewFixes20260906_mismatchReturnsTheShorterLengthForACommonPrefix() {
        assertEquals(2, CommonUtil.mismatch(new boolean[] { true, false }, new boolean[] { true, false, true }));
        assertEquals(2, CommonUtil.mismatch(new char[] { 'a', 'b' }, new char[] { 'a', 'b', 'c' }));
        assertEquals(2, CommonUtil.mismatch(new byte[] { 1, 2 }, new byte[] { 1, 2, 3 }));
        assertEquals(2, CommonUtil.mismatch(new short[] { 1, 2 }, new short[] { 1, 2, 3 }));
        assertEquals(2, CommonUtil.mismatch(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));
        assertEquals(2, CommonUtil.mismatch(new long[] { 1, 2 }, new long[] { 1, 2, 3 }));
        assertEquals(2, CommonUtil.mismatch(new float[] { 1, 2 }, new float[] { 1, 2, 3 }));
        assertEquals(2, CommonUtil.mismatch(new double[] { 1, 2 }, new double[] { 1, 2, 3 }));
        assertEquals(2, CommonUtil.mismatch(new String[] { "a", "b" }, new String[] { "a", "b", "c" }));
        assertEquals(2, CommonUtil.mismatch(new String[] { "a", "bb" }, new String[] { "x", "yy", "zzz" }, String::length));
        assertEquals(2, CommonUtil.mismatch(new int[] { 1, 2, 3 }, new int[] { 1, 2 }));
        assertEquals(-1, CommonUtil.mismatch(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertEquals(1, CommonUtil.mismatch(new int[] { 1, 9 }, new int[] { 1, 2, 3 }));
    }

    @Test
    public void reviewFixes20260906_stepTakingCopyOfRangeRejectsNullArguments() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((boolean[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((char[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((byte[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((short[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((int[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((long[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((float[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((double[]) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((Object[]) null, 0, 0, 2, String[].class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((List<String>) null, 0, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange((int[]) null, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.copyOfRange(new int[] { 1, 2 }, 0, 2, 0));
        assertEquals("", CommonUtil.copyOfRange((String) null, 0, 0, 2));
        assertArrayEquals(new int[] { 1, 3 }, CommonUtil.copyOfRange(new int[] { 1, 2, 3, 4 }, 0, 4, 2));
    }

    @Test
    public void reviewFixes20260906_lastIndexOfSubListBackIndexContract() {
        final List<Integer> src = Arrays.asList(1, 2, 3);
        assertEquals(1, CommonUtil.lastIndexOfSubList(src, Collections.emptyList(), 1));
        assertEquals(3, CommonUtil.lastIndexOfSubList(src, Collections.emptyList(), 9));
        assertEquals(-1, CommonUtil.lastIndexOfSubList(src, Collections.emptyList(), -1));
        assertEquals(-1, CommonUtil.lastIndexOfSubList(src, Arrays.asList(2), -1));
        assertEquals(-1, CommonUtil.lastIndexOfSubList(null, Arrays.asList(2), 0));
        assertEquals(-1, CommonUtil.lastIndexOfSubList(src, null, 0));
        assertEquals(2, CommonUtil.lastIndexOfSubList(Arrays.asList(1, 2, 1, 2), Arrays.asList(1, 2), 9));
        assertEquals(0, CommonUtil.lastIndexOfSubList(Arrays.asList(1, 2, 1, 2), Arrays.asList(1, 2), 1));
        assertEquals(-1, CommonUtil.lastIndexOfSubList(src, Arrays.asList(4), 2));
    }

    @Test
    public void testSwap_outOfRangeNullAndUnmodifiable() {
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.swap(new int[] { 1, 2 }, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.swap(new int[] { 1, 2 }, -1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.swap(new boolean[] { true, false }, 0, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.swap(new Object[] { "a", "b" }, 0, 2));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.swap((int[]) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.swap((List<?>) null, 0, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.swap(new ArrayList<>(Arrays.asList("a", "b")), 0, 5));
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.swap(List.of("a", "b"), 0, 1));
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.swap(List.of("a", "b"), 0, 0));

        final int[] a = { 10, 20, 30 };
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.swap(a, 1, 9));
        assertArrayEquals(new int[] { 10, 20, 30 }, a);
    }

}
