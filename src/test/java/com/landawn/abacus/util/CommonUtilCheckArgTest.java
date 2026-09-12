package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CommonUtilCheckArgTest extends CommonUtilTestSupport {

    @Test
    public void testCheckArgNotNull() {
        String obj = "test";
        assertSame(obj, CommonUtil.checkArgNotNull(obj));
        assertSame(obj, CommonUtil.checkArgNotNull(obj, "testObject"));
        Integer num = 1;
        assertSame(num, CommonUtil.checkArgNotNull(num));
    }

    @Test
    public void testCheckArgNotNull_EdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null));
        assertEquals("'testObject' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "testObject")).getMessage());
        assertEquals("Custom error message for null object",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "Custom error message for null object")).getMessage());
        assertEquals(" must not be null (custom message)",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, " must not be null (custom message)")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, (String) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Collection<?>) null, (String) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, (String) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, (String) null));
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null, (String) null));
    }

    @Test
    public void testCheckArgNotEmpty() {
        String s = "test";
        assertSame(s, CommonUtil.checkArgNotEmpty(s, "charSeq"));
        assertSame("  ", CommonUtil.checkArgNotEmpty("  ", "padding"));
        StringBuilder sb = new StringBuilder("abc");
        assertSame(sb, CommonUtil.checkArgNotEmpty(sb, "charSeqBuilder"));

        boolean[] bools = { true, false };
        assertSame(bools, CommonUtil.checkArgNotEmpty(bools, "boolArray"));
        char[] chars = { 'a', 'b' };
        assertSame(chars, CommonUtil.checkArgNotEmpty(chars, "charArr"));
        byte[] bytes = { 1, 2 };
        assertSame(bytes, CommonUtil.checkArgNotEmpty(bytes, "byteArr"));
        short[] shorts = { 1, 2 };
        assertSame(shorts, CommonUtil.checkArgNotEmpty(shorts, "shortArr"));
        int[] ints = { 1, 2 };
        assertSame(ints, CommonUtil.checkArgNotEmpty(ints, "intArr"));
        long[] longs = { 1L, 2L };
        assertSame(longs, CommonUtil.checkArgNotEmpty(longs, "longArr"));
        float[] floats = { 1.0f, 2.0f };
        assertSame(floats, CommonUtil.checkArgNotEmpty(floats, "floatArr"));
        double[] doubles = { 1.0, 2.0 };
        assertSame(doubles, CommonUtil.checkArgNotEmpty(doubles, "doubleArr"));
        String[] objs = { "a", "b" };
        assertSame(objs, CommonUtil.checkArgNotEmpty(objs, "objArr"));

        List<String> list = Arrays.asList("a", "b");
        assertSame(list, CommonUtil.checkArgNotEmpty(list, "coll"));
        Iterator<String> iterator = list.iterator();
        assertSame(iterator, CommonUtil.checkArgNotEmpty(iterator, "iterator"));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertSame(map, CommonUtil.checkArgNotEmpty(map, "map"));

        BooleanList pList = BooleanList.of(true, false);
        assertSame(pList, CommonUtil.checkArgNotEmpty(pList, "pList"));
        IntList intList = IntList.of(1, 2, 3);
        assertSame(intList, CommonUtil.checkArgNotEmpty(intList, "list"));
        Multiset<String> multiset = Multiset.of("a", "b");
        assertSame(multiset, CommonUtil.checkArgNotEmpty(multiset, "multiset"));
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("key", 1);
        assertSame(multimap, CommonUtil.checkArgNotEmpty(multimap, "mm"));
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        assertSame(ds, CommonUtil.checkArgNotEmpty(ds, "ds"));
    }

    @Test
    public void testCheckArgNotEmpty_EdgeCase() {
        assertEquals("'charSeq' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String) null, "charSeq")).getMessage());
        assertEquals("'charSeq' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "charSeq")).getMessage());
        assertEquals("Custom error message for null/empty charSeq",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String) null, "Custom error message for null/empty charSeq"))
                        .getMessage());

        assertEquals("'boolArray' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((boolean[]) null, "boolArray")).getMessage());
        assertEquals("'boolArray' cannot be null or empty",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new boolean[0], "boolArray")).getMessage());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((char[]) null, "charArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new char[0], "charArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((byte[]) null, "byteArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new byte[0], "byteArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((short[]) null, "shortArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new short[0], "shortArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "intArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[0], "intArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((long[]) null, "longArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new long[0], "longArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((float[]) null, "floatArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new float[0], "floatArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((double[]) null, "doubleArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new double[0], "doubleArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Object[]) null, "objArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new Object[0], "objArr"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Collection<?>) null, "coll"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyList(), "coll"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Iterator<?>) null, "iterator"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyIterator(), "iterator"));
        Iterator<String> consumed = Arrays.asList("a").iterator();
        consumed.next();
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(consumed, "consumedIterator"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Map<?, ?>) null, "map"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(Collections.emptyMap(), "map"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((BooleanList) null, "pList"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new BooleanList(), "pList"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((IntList) null, "list"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(IntList.of(), "list"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Multiset<String>) null, "ms"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new Multiset<>(), "ms"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((ListMultimap<String, Integer>) null, "mm"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(CommonUtil.newListMultimap(), "mm"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((Dataset) null, "ds"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(CommonUtil.emptyDataset(), "ds"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(CommonUtil.newEmptyDataset(), "ds"));
    }

    @Test
    public void testCheckArgNotBlank() {
        String s = "test";
        assertSame(s, CommonUtil.checkArgNotBlank(s, "notBlankStr"));
        String padded = "  test  ";
        assertSame(padded, CommonUtil.checkArgNotBlank(padded, "notBlankStrWithSpaces"));
        StringBuilder sb = new StringBuilder("  test  ");
        assertSame(sb, CommonUtil.checkArgNotBlank(sb, "sb"));
    }

    @Test
    public void testCheckArgNotBlank_EdgeCase() {
        assertEquals("'notBlankStr' cannot be null or empty or blank",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank(null, "notBlankStr")).getMessage());
        assertEquals("'notBlankStr' cannot be null or empty or blank",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("", "notBlankStr")).getMessage());
        assertEquals("'notBlankStr' cannot be null or empty or blank",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("   ", "notBlankStr")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank("\t\n", "str"));
        assertEquals("Custom error for blank string",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotBlank(null, "Custom error for blank string")).getMessage());
    }

    @Test
    public void testCheckArgNotNegative() {
        assertEquals((byte) 0, CommonUtil.checkArgNotNegative((byte) 0, "byteArg"));
        assertEquals((byte) 10, CommonUtil.checkArgNotNegative((byte) 10, "byteArg"));
        assertEquals(Byte.MAX_VALUE, CommonUtil.checkArgNotNegative(Byte.MAX_VALUE, "byteArg"));
        assertEquals((short) 0, CommonUtil.checkArgNotNegative((short) 0, "shortArg"));
        assertEquals(Short.MAX_VALUE, CommonUtil.checkArgNotNegative(Short.MAX_VALUE, "shortArg"));
        assertEquals(0, CommonUtil.checkArgNotNegative(0, "intArg"));
        assertEquals(Integer.MAX_VALUE, CommonUtil.checkArgNotNegative(Integer.MAX_VALUE, "intArg"));
        assertEquals(0L, CommonUtil.checkArgNotNegative(0L, "longArg"));
        assertEquals(Long.MAX_VALUE, CommonUtil.checkArgNotNegative(Long.MAX_VALUE, "longArg"));
        assertEquals(0.0f, CommonUtil.checkArgNotNegative(0.0f, "floatArg"));
        assertEquals(Float.MAX_VALUE, CommonUtil.checkArgNotNegative(Float.MAX_VALUE, "floatArg"));
        assertEquals(0.0, CommonUtil.checkArgNotNegative(0.0, "doubleArg"));
        assertEquals(Double.MAX_VALUE, CommonUtil.checkArgNotNegative(Double.MAX_VALUE, "doubleArg"));
    }

    @Test
    public void testCheckArgNotNegative_EdgeCase() {
        assertEquals("'byteArg' cannot be negative: -1",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "byteArg")).getMessage());
        assertEquals("Custom msg", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((byte) -1, "Custom msg")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Byte.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((short) -1, "shortArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Short.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "intArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Integer.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1L, "longArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Long.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1f, "floatArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Float.MAX_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Float.NaN, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-0.1, "doubleArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-Double.MAX_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(Double.NaN, "val"));
        assertEquals("value must not be negative",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative((short) -1, "value must not be negative")).getMessage());
        assertEquals("int value must not be negative",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-5, "int value must not be negative")).getMessage());
        assertEquals("long value must not be negative",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1L, "long value must not be negative")).getMessage());
        assertEquals("float value must not be negative",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1.0f, "float value must not be negative")).getMessage());
        assertEquals("double value must not be negative",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1.0, "double value must not be negative")).getMessage());
    }

    @Test
    public void testCheckArgPositive() {
        assertEquals((byte) 1, CommonUtil.checkArgPositive((byte) 1, "byteArg"));
        assertEquals(Byte.MAX_VALUE, CommonUtil.checkArgPositive(Byte.MAX_VALUE, "byteArg"));
        assertEquals((short) 1, CommonUtil.checkArgPositive((short) 1, "shortArg"));
        assertEquals(Short.MAX_VALUE, CommonUtil.checkArgPositive(Short.MAX_VALUE, "shortArg"));
        assertEquals(1, CommonUtil.checkArgPositive(1, "intArg"));
        assertEquals(Integer.MAX_VALUE, CommonUtil.checkArgPositive(Integer.MAX_VALUE, "intArg"));
        assertEquals(1L, CommonUtil.checkArgPositive(1L, "longArg"));
        assertEquals(Long.MAX_VALUE, CommonUtil.checkArgPositive(Long.MAX_VALUE, "longArg"));
        assertEquals(0.1f, CommonUtil.checkArgPositive(0.1f, "floatArg"));
        assertEquals(Float.MAX_VALUE, CommonUtil.checkArgPositive(Float.MAX_VALUE, "floatArg"));
        assertEquals(0.1, CommonUtil.checkArgPositive(0.1, "doubleArg"));
        assertEquals(Double.MAX_VALUE, CommonUtil.checkArgPositive(Double.MAX_VALUE, "doubleArg"));
    }

    @Test
    public void testCheckArgPositive_EdgeCase() {
        assertEquals("'byteArg' cannot be zero or negative: 0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "byteArg")).getMessage());
        assertEquals("'byteArg' cannot be zero or negative: -1",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) -1, "byteArg")).getMessage());
        assertEquals("Custom msg", assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((byte) 0, "Custom msg")).getMessage());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Byte.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) 0, "shortArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) -1, "shortArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Short.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, "intArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1, "intArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Integer.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "longArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-1L, "longArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Long.MIN_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "floatArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1f, "floatArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-Float.MAX_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Float.NaN, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0, "doubleArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-0.1, "doubleArg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(-Double.MAX_VALUE, "val"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(Double.NaN, "val"));
        assertEquals("short value must be positive",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive((short) 0, "short value must be positive")).getMessage());
        assertEquals("int value must be positive",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0, "int value must be positive")).getMessage());
        assertEquals("long value must be positive",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0L, "long value must be positive")).getMessage());
        assertEquals("float value must be positive",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0f, "float value must be positive")).getMessage());
        assertEquals("double value must be positive",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgPositive(0.0, "double value must be positive")).getMessage());
    }
}
