package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CommonUtil104Test extends TestBase {

    // Helper classes
    private static TestBean createBean(String name, int value) {
        TestBean bean = new TestBean();
        bean.setName(name);
        bean.setValue(value);
        return bean;
    }

    public static class TestBean {
        private String name;
        private int value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class DelayedElement implements Delayed {
        private final long delayTime;
        private final long startTime;

        public DelayedElement(long delayMillis) {
            this.delayTime = delayMillis;
            this.startTime = System.currentTimeMillis();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            long diff = startTime + delayTime - System.currentTimeMillis();
            return unit.convert(diff, TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o) {
            return Long.compare(this.getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }
    }

    @Test
    public void testToList_BooleanArray() {
        boolean[] array = { true, false, true };
        List<Boolean> list = N.toList(array);
        assertEquals(Arrays.asList(true, false, true), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new boolean[0]));
        assertEquals(new ArrayList<>(), N.toList((boolean[]) null));
    }

    @Test
    public void testToList_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        List<Boolean> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(false, true, false), list);

        // Empty range
        assertEquals(new ArrayList<>(), N.toList(array, 2, 2));

        assertThrows(IndexOutOfBoundsException.class, () -> N.toList(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toList(array, 0, 6));
    }

    @Test
    public void testToList_CharArray() {
        char[] array = { 'a', 'b', 'c' };
        List<Character> list = N.toList(array);
        assertEquals(Arrays.asList('a', 'b', 'c'), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new char[0]));
        assertEquals(new ArrayList<>(), N.toList((char[]) null));
    }

    @Test
    public void testToList_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList('b', 'c', 'd'), list);
    }

    @Test
    public void testToList_ByteArray() {
        byte[] array = { 1, 2, 3 };
        List<Byte> list = N.toList(array);
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new byte[0]));
        assertEquals(new ArrayList<>(), N.toList((byte[]) null));
    }

    @Test
    public void testToList_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        List<Byte> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4), list);
    }

    @Test
    public void testToList_ShortArray() {
        short[] array = { 1, 2, 3 };
        List<Short> list = N.toList(array);
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new short[0]));
        assertEquals(new ArrayList<>(), N.toList((short[]) null));
    }

    @Test
    public void testToList_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        List<Short> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList((short) 2, (short) 3, (short) 4), list);
    }

    @Test
    public void testToList_IntArray() {
        int[] array = { 1, 2, 3 };
        List<Integer> list = N.toList(array);
        assertEquals(Arrays.asList(1, 2, 3), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new int[0]));
        assertEquals(new ArrayList<>(), N.toList((int[]) null));
    }

    @Test
    public void testToList_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        List<Integer> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2, 3, 4), list);
    }

    @Test
    public void testToList_LongArray() {
        long[] array = { 1L, 2L, 3L };
        List<Long> list = N.toList(array);
        assertEquals(Arrays.asList(1L, 2L, 3L), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new long[0]));
        assertEquals(new ArrayList<>(), N.toList((long[]) null));
    }

    @Test
    public void testToList_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        List<Long> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2L, 3L, 4L), list);
    }

    @Test
    public void testToList_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        List<Float> list = N.toList(array);
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new float[0]));
        assertEquals(new ArrayList<>(), N.toList((float[]) null));
    }

    @Test
    public void testToList_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<Float> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f), list);
    }

    @Test
    public void testToList_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        List<Double> list = N.toList(array);
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new double[0]));
        assertEquals(new ArrayList<>(), N.toList((double[]) null));
    }

    @Test
    public void testToList_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<Double> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList(2.0, 3.0, 4.0), list);
    }

    @Test
    public void testToList_ObjectArray() {
        String[] array = { "a", "b", "c" };
        List<String> list = N.toList(array);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        // Empty array
        assertEquals(new ArrayList<>(), N.toList(new String[0]));
        assertEquals(new ArrayList<>(), N.toList((String[]) null));
    }

    @Test
    public void testToList_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> list = N.toList(array, 1, 4);
        assertEquals(Arrays.asList("b", "c", "d"), list);

        // Full range
        List<String> fullList = N.toList(array, 0, array.length);
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), fullList);

        // Empty range
        assertEquals(new ArrayList<>(), N.toList(array, 2, 2));
    }

    @Test
    public void testToList_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        List<String> list = N.toList(iter);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        // Null iterator
        List<String> nullList = N.toList((Iterator<String>) null);
        assertEquals(new ArrayList<>(), nullList);
    }

    @Test
    public void testToSet_BooleanArray() {
        boolean[] array = { true, false, true, false };
        Set<Boolean> set = N.toSet(array);
        assertEquals(2, set.size());
        assertTrue(set.contains(true));
        assertTrue(set.contains(false));

        // Empty array
        assertEquals(new HashSet<>(), N.toSet(new boolean[0]));
        assertEquals(new HashSet<>(), N.toSet((boolean[]) null));
    }

    @Test
    public void testToSet_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        Set<Boolean> set = N.toSet(array, 1, 4);
        assertEquals(2, set.size());
        assertTrue(set.contains(true));
        assertTrue(set.contains(false));
    }

    @Test
    public void testToSet_CharArray() {
        char[] array = { 'a', 'b', 'c', 'a' };
        Set<Character> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains('a'));
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
    }

    @Test
    public void testToSet_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        Set<Character> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains('b'));
        assertTrue(set.contains('c'));
        assertTrue(set.contains('d'));
    }

    @Test
    public void testToSet_ByteArray() {
        byte[] array = { 1, 2, 3, 1, 2 };
        Set<Byte> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 1));
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));
    }

    @Test
    public void testToSet_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        Set<Byte> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains((byte) 2));
        assertTrue(set.contains((byte) 3));
        assertTrue(set.contains((byte) 4));
    }

    @Test
    public void testToSet_ShortArray() {
        short[] array = { 1, 2, 3, 1, 2 };
        Set<Short> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 1));
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
    }

    @Test
    public void testToSet_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        Set<Short> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains((short) 2));
        assertTrue(set.contains((short) 3));
        assertTrue(set.contains((short) 4));
    }

    @Test
    public void testToSet_IntArray() {
        int[] array = { 1, 2, 3, 1, 2 };
        Set<Integer> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void testToSet_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        Set<Integer> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }

    @Test
    public void testToSet_LongArray() {
        long[] array = { 1L, 2L, 3L, 1L, 2L };
        Set<Long> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1L));
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
    }

    @Test
    public void testToSet_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        Set<Long> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2L));
        assertTrue(set.contains(3L));
        assertTrue(set.contains(4L));
    }

    @Test
    public void testToSet_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f, 1.0f, 2.0f };
        Set<Float> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1.0f));
        assertTrue(set.contains(2.0f));
        assertTrue(set.contains(3.0f));
    }

    @Test
    public void testToSet_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        Set<Float> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2.0f));
        assertTrue(set.contains(3.0f));
        assertTrue(set.contains(4.0f));
    }

    @Test
    public void testToSet_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0, 1.0, 2.0 };
        Set<Double> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains(1.0));
        assertTrue(set.contains(2.0));
        assertTrue(set.contains(3.0));
    }

    @Test
    public void testToSet_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        Set<Double> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains(2.0));
        assertTrue(set.contains(3.0));
        assertTrue(set.contains(4.0));
    }

    @Test
    public void testToSet_ObjectArray() {
        String[] array = { "a", "b", "c", "a", "b" };
        Set<String> set = N.toSet(array);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        // Empty array
        assertEquals(new HashSet<>(), N.toSet(new String[0]));
        assertEquals(new HashSet<>(), N.toSet((String[]) null));
    }

    @Test
    public void testToSet_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        Set<String> set = N.toSet(array, 1, 4);
        assertEquals(3, set.size());
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertTrue(set.contains("d"));
    }

    @Test
    public void testToSet_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c", "a", "b").iterator();
        Set<String> set = N.toSet(iter);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        // Null iterator
        Set<String> nullSet = N.toSet((Iterator<String>) null);
        assertEquals(new HashSet<>(), nullSet);
    }

    @Test
    public void testToCollection_BooleanArray() {
        boolean[] array = { true, false, true };
        List<Boolean> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(true, false, true), list);

        LinkedList<Boolean> linkedList = N.toCollection(array, size -> new LinkedList<>());
        assertEquals(3, linkedList.size());
    }

    @Test
    public void testToCollection_BooleanArrayRange() {
        boolean[] array = { true, false, true, false, true };
        List<Boolean> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(false, true, false), list);
    }

    @Test
    public void testToCollection_CharArray() {
        char[] array = { 'a', 'b', 'c' };
        List<Character> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList('a', 'b', 'c'), list);
    }

    @Test
    public void testToCollection_CharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        List<Character> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList('b', 'c', 'd'), list);
    }

    @Test
    public void testToCollection_ByteArray() {
        byte[] array = { 1, 2, 3 };
        List<Byte> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), list);
    }

    @Test
    public void testToCollection_ByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        List<Byte> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((byte) 2, (byte) 3, (byte) 4), list);
    }

    @Test
    public void testToCollection_ShortArray() {
        short[] array = { 1, 2, 3 };
        List<Short> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), list);
    }

    @Test
    public void testToCollection_ShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        List<Short> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList((short) 2, (short) 3, (short) 4), list);
    }

    @Test
    public void testToCollection_IntArray() {
        int[] array = { 1, 2, 3 };
        List<Integer> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testToCollection_IntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        List<Integer> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2, 3, 4), list);
    }

    @Test
    public void testToCollection_LongArray() {
        long[] array = { 1L, 2L, 3L };
        List<Long> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1L, 2L, 3L), list);
    }

    @Test
    public void testToCollection_LongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        List<Long> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2L, 3L, 4L), list);
    }

    @Test
    public void testToCollection_FloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f };
        List<Float> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), list);
    }

    @Test
    public void testToCollection_FloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        List<Float> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2.0f, 3.0f, 4.0f), list);
    }

    @Test
    public void testToCollection_DoubleArray() {
        double[] array = { 1.0, 2.0, 3.0 };
        List<Double> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(1.0, 2.0, 3.0), list);
    }

    @Test
    public void testToCollection_DoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        List<Double> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList(2.0, 3.0, 4.0), list);
    }

    @Test
    public void testToCollection_ObjectArray() {
        String[] array = { "a", "b", "c" };
        List<String> list = N.toCollection(array, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList("a", "b", "c"), list);

        // Empty array
        List<String> emptyList = N.toCollection(new String[0], size -> new ArrayList<>(size));
        assertEquals(new ArrayList<>(), emptyList);
    }

    @Test
    public void testToCollection_ObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        List<String> list = N.toCollection(array, 1, 4, size -> new ArrayList<>(size));
        assertEquals(Arrays.asList("b", "c", "d"), list);
    }

    @Test
    public void testToCollection_Iterable() {
        {
            List<String> source = Arrays.asList("a", "b", "c");
            Set<String> set = N.toCollection(source, size -> new HashSet<>(size));
            assertEquals(3, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));

            // Empty iterable
            Set<String> emptySet = N.toCollection(new ArrayList<String>(), size -> new HashSet<>(size));
            assertTrue(emptySet.isEmpty());
        }
        {
            Iterable<String> source = createIterable("a", "b", "c");
            Set<String> set = N.toCollection(source, size -> new HashSet<>(size));
            assertEquals(3, set.size());
            assertTrue(set.contains("a"));
            assertTrue(set.contains("b"));
            assertTrue(set.contains("c"));
        }

    }

    @Test
    public void testToCollection_Iterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        List<String> list = N.toCollection(iter, () -> new ArrayList<>());
        assertEquals(Arrays.asList("a", "b", "c"), list);

        // Null iterator
        List<String> nullList = N.toCollection((Iterator<String>) null, () -> new ArrayList<>());
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testToMap_IterableWithKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2), createBean("bean3", 3));

        Map<String, TestBean> map = N.toMap(beans, TestBean::getName);
        assertEquals(3, map.size());
        assertEquals(1, map.get("bean1").getValue());
        assertEquals(2, map.get("bean2").getValue());
        assertEquals(3, map.get("bean3").getValue());

        // Empty iterable
        Map<String, TestBean> emptyMap = N.toMap(new ArrayList<TestBean>(), TestBean::getName);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testToMap_IterableWithKeyValueExtractors() {
        List<TestBean> beans = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2), createBean("bean3", 3));

        Map<String, Integer> map = N.toMap(beans, TestBean::getName, TestBean::getValue);
        assertEquals(3, map.size());
        assertEquals(1, map.get("bean1").intValue());
        assertEquals(2, map.get("bean2").intValue());
        assertEquals(3, map.get("bean3").intValue());
    }

    @Test
    public void testToMap_IterableWithKeyValueExtractorsAndSupplier() {
        List<TestBean> beans = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2), createBean("bean3", 3));

        TreeMap<String, Integer> map = N.toMap(beans, TestBean::getName, TestBean::getValue, size -> new TreeMap<>());
        assertEquals(3, map.size());
        assertEquals("bean1", map.firstKey());
        assertEquals("bean3", map.lastKey());
    }

    @Test
    public void testToMap_IterableWithMergeFunction() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 2), createBean("group2", 3));

        Map<String, Integer> map = N.toMap(beans, TestBean::getName, TestBean::getValue, (v1, v2) -> v1 + v2, // Sum values for duplicate keys
                size -> new HashMap<>());
        assertEquals(2, map.size());
        assertEquals(3, map.get("group1").intValue()); // 1 + 2
        assertEquals(3, map.get("group2").intValue());
    }

    @Test
    public void testToMap_IteratorWithKeyExtractor() {
        Iterator<TestBean> iter = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2)).iterator();

        Map<String, TestBean> map = N.toMap(iter, TestBean::getName);
        assertEquals(2, map.size());
        assertEquals(1, map.get("bean1").getValue());
        assertEquals(2, map.get("bean2").getValue());

        // Null iterator
        Map<String, TestBean> nullMap = N.toMap((Iterator<TestBean>) null, TestBean::getName);
        assertTrue(nullMap.isEmpty());
    }

    @Test
    public void testToMap_IteratorWithKeyValueExtractors() {
        Iterator<TestBean> iter = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2)).iterator();

        Map<String, Integer> map = N.toMap(iter, TestBean::getName, TestBean::getValue);
        assertEquals(2, map.size());
        assertEquals(1, map.get("bean1").intValue());
        assertEquals(2, map.get("bean2").intValue());
    }

    @Test
    public void testToMap_IteratorWithKeyValueExtractorsAndSupplier() {
        Iterator<TestBean> iter = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2)).iterator();

        TreeMap<String, Integer> map = N.toMap(iter, TestBean::getName, TestBean::getValue, () -> new TreeMap<>());
        assertEquals(2, map.size());
        assertEquals("bean1", map.firstKey());
    }

    @Test
    public void testToMap_IteratorWithMergeFunction() {
        Iterator<TestBean> iter = Arrays.asList(createBean("group1", 1), createBean("group1", 2), createBean("group2", 3)).iterator();

        Map<String, Integer> map = N.toMap(iter, TestBean::getName, TestBean::getValue, (v1, v2) -> v1 + v2, () -> new HashMap<>());
        assertEquals(2, map.size());
        assertEquals(3, map.get("group1").intValue());
        assertEquals(3, map.get("group2").intValue());
    }

    @Test
    public void testAsArray() {
        String[] array = N.asArray("a", "b", "c");
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        Integer[] intArray = N.asArray(1, 2, 3);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, intArray);

        Object[] emptyArray = N.asArray();
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testAsMap() {
        Map<String, Integer> map = N.asMap("a", 1);
        assertEquals(1, map.size());
        assertEquals(1, map.get("a").intValue());

        map = N.asMap("a", 1, "b", 2);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());

        map = N.asMap("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());

        map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4);
        assertEquals(4, map.size());

        map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        assertEquals(5, map.size());

        map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        assertEquals(6, map.size());

        map = N.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, map.size());
    }

    @Test
    public void testAsMap_VarArgs() {
        Map<String, Integer> map = N.asMap("key1", 1, "key2", 2, "key3", 3);
        assertEquals(3, map.size());
        assertEquals(1, map.get("key1").intValue());
        assertEquals(2, map.get("key2").intValue());
        assertEquals(3, map.get("key3").intValue());

        // Empty varargs
        Map<String, Object> emptyMap = N.asMap();
        assertTrue(emptyMap.isEmpty());

        // With another map
        Map<String, String> sourceMap = new HashMap<>();
        sourceMap.put("a", "1");
        sourceMap.put("b", "2");
        Map<String, String> resultMap = N.asMap(sourceMap);
        assertEquals(2, resultMap.size());
        assertEquals("1", resultMap.get("a"));
        assertEquals("2", resultMap.get("b"));

        // With bean object
        TestBean bean = createBean("test", 123);
        Map<String, Object> beanMap = N.asMap(bean);
        assertEquals("test", beanMap.get("name"));
        assertEquals(123, beanMap.get("value"));

        // Wrong number of arguments
        assertThrows(IllegalArgumentException.class, () -> N.asMap("key1", 1, "key2"));
    }

    @Test
    public void testAsLinkedHashMap() {
        Map<String, Integer> map = N.asLinkedHashMap("a", 1);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(1, map.size());

        map = N.asLinkedHashMap("a", 1, "b", 2);
        assertEquals(2, map.size());

        map = N.asLinkedHashMap("a", 1, "b", 2, "c", 3);
        assertEquals(3, map.size());

        map = N.asLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4);
        assertEquals(4, map.size());

        map = N.asLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        assertEquals(5, map.size());

        map = N.asLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        assertEquals(6, map.size());

        map = N.asLinkedHashMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        assertEquals(7, map.size());

        // Check order is preserved
        Iterator<String> iter = map.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testAsLinkedHashMap_VarArgs() {
        Map<String, Object> map = N.asLinkedHashMap("key1", 1, "key2", 2);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(2, map.size());

        // Empty varargs
        Map<String, Object> emptyMap = N.asLinkedHashMap();
        assertTrue(emptyMap instanceof LinkedHashMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testAsProps() {
        Map<String, Object> props = N.asProps("prop1", "value1");
        assertEquals(1, props.size());
        assertEquals("value1", props.get("prop1"));

        props = N.asProps("prop1", "value1", "prop2", "value2");
        assertEquals(2, props.size());

        props = N.asProps("prop1", "value1", "prop2", "value2", "prop3", "value3");
        assertEquals(3, props.size());

        props = N.asProps("prop1", "value1", "prop2", "value2", "prop3", "value3", "prop4", "value4");
        assertEquals(4, props.size());

        props = N.asProps("prop1", "value1", "prop2", "value2", "prop3", "value3", "prop4", "value4", "prop5", "value5");
        assertEquals(5, props.size());
    }

    @Test
    public void testAsProps_VarArgs() {
        Map<String, Object> props = N.asProps("key1", 1, "key2", 2);
        assertEquals(2, props.size());

        // Empty varargs
        Map<String, Object> emptyProps = N.asProps();
        assertTrue(emptyProps.isEmpty());
    }

    @Test
    public void testAsList() {
        List<String> list = N.asList("a");
        assertEquals(1, list.size());
        assertEquals("a", list.get(0));

        list = N.asList("a", "b");
        assertEquals(2, list.size());

        list = N.asList("a", "b", "c");
        assertEquals(3, list.size());

        list = N.asList("a", "b", "c", "d");
        assertEquals(4, list.size());

        list = N.asList("a", "b", "c", "d", "e");
        assertEquals(5, list.size());

        list = N.asList("a", "b", "c", "d", "e", "f");
        assertEquals(6, list.size());

        list = N.asList("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, list.size());

        list = N.asList("a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(8, list.size());

        list = N.asList("a", "b", "c", "d", "e", "f", "g", "h", "i");
        assertEquals(9, list.size());
    }

    @Test
    public void testAsList_VarArgs() {
        String[] array = { "a", "b", "c" };
        List<String> list = N.asList(array);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        // Empty array
        List<String> emptyList = N.asList();
        assertTrue(emptyList.isEmpty());

        // Null array
        List<String> nullList = N.asList((String[]) null);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testAsLinkedList() {
        LinkedList<String> list = N.asLinkedList("a");
        assertEquals(1, list.size());
        assertEquals("a", list.getFirst());

        list = N.asLinkedList("a", "b");
        assertEquals(2, list.size());

        list = N.asLinkedList("a", "b", "c");
        assertEquals(3, list.size());

        list = N.asLinkedList("a", "b", "c", "d");
        assertEquals(4, list.size());

        list = N.asLinkedList("a", "b", "c", "d", "e");
        assertEquals(5, list.size());

        list = N.asLinkedList("a", "b", "c", "d", "e", "f");
        assertEquals(6, list.size());

        list = N.asLinkedList("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, list.size());
    }

    @Test
    public void testAsLinkedList_VarArgs() {
        String[] array = { "a", "b", "c" };
        LinkedList<String> list = N.asLinkedList(array);
        assertEquals(3, list.size());

        // Empty array
        LinkedList<String> emptyList = N.asLinkedList();
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testAsSet() {
        Set<String> set = N.asSet("a");
        assertEquals(1, set.size());
        assertTrue(set.contains("a"));

        set = N.asSet("a", "b");
        assertEquals(2, set.size());

        set = N.asSet("a", "b", "c");
        assertEquals(3, set.size());

        set = N.asSet("a", "b", "c", "d");
        assertEquals(4, set.size());

        set = N.asSet("a", "b", "c", "d", "e");
        assertEquals(5, set.size());

        set = N.asSet("a", "b", "c", "d", "e", "f");
        assertEquals(6, set.size());

        set = N.asSet("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, set.size());

        set = N.asSet("a", "b", "c", "d", "e", "f", "g", "h");
        assertEquals(8, set.size());

        set = N.asSet("a", "b", "c", "d", "e", "f", "g", "h", "i");
        assertEquals(9, set.size());

        // Test duplicates
        set = N.asSet("a", "b", "a");
        assertEquals(2, set.size());
    }

    @Test
    public void testAsSet_VarArgs() {
        String[] array = { "a", "b", "c", "a" };
        Set<String> set = N.asSet(array);
        assertEquals(3, set.size());

        // Empty array
        Set<String> emptySet = N.asSet();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAsLinkedHashSet() {
        Set<String> set = N.asLinkedHashSet("a");
        assertTrue(set instanceof LinkedHashSet);
        assertEquals(1, set.size());

        set = N.asLinkedHashSet("a", "b");
        assertEquals(2, set.size());

        set = N.asLinkedHashSet("a", "b", "c");
        assertEquals(3, set.size());

        set = N.asLinkedHashSet("a", "b", "c", "d");
        assertEquals(4, set.size());

        set = N.asLinkedHashSet("a", "b", "c", "d", "e");
        assertEquals(5, set.size());

        set = N.asLinkedHashSet("a", "b", "c", "d", "e", "f");
        assertEquals(6, set.size());

        set = N.asLinkedHashSet("a", "b", "c", "d", "e", "f", "g");
        assertEquals(7, set.size());

        // Check order is preserved
        Iterator<String> iter = set.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
    }

    @Test
    public void testAsLinkedHashSet_VarArgs() {
        String[] array = { "a", "b", "c" };
        Set<String> set = N.asLinkedHashSet(array);
        assertTrue(set instanceof LinkedHashSet);
        assertEquals(3, set.size());
    }

    @Test
    public void testAsSortedSet() {
        SortedSet<String> set = N.asSortedSet("b", "a", "c");
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        // Empty array
        SortedSet<String> emptySet = N.asSortedSet();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAsNavigableSet() {
        NavigableSet<String> set = N.asNavigableSet("b", "a", "c");
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());
        assertEquals("b", set.higher("a"));
        assertEquals("b", set.lower("c"));

        // Empty array
        NavigableSet<String> emptySet = N.asNavigableSet();
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAsQueue() {
        Queue<String> queue = N.asQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertEquals("a", queue.poll());
        assertEquals("b", queue.poll());
        assertEquals("c", queue.poll());
    }

    @Test
    public void testAsArrayBlockingQueue() {
        ArrayBlockingQueue<String> queue = N.asArrayBlockingQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertTrue(queue instanceof ArrayBlockingQueue);

        // Empty array
        ArrayBlockingQueue<String> emptyQueue = N.asArrayBlockingQueue();
        assertTrue(emptyQueue.isEmpty());
    }

    @Test
    public void testAsLinkedBlockingQueue() {
        LinkedBlockingQueue<String> queue = N.asLinkedBlockingQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertTrue(queue instanceof LinkedBlockingQueue);
    }

    @Test
    public void testAsConcurrentLinkedQueue() {
        ConcurrentLinkedQueue<String> queue = N.asConcurrentLinkedQueue("a", "b", "c");
        assertEquals(3, queue.size());
        assertTrue(queue instanceof ConcurrentLinkedQueue);
    }

    @Test
    public void testAsDelayQueue() {
        DelayedElement e1 = new DelayedElement(100);
        DelayedElement e2 = new DelayedElement(200);
        DelayQueue<DelayedElement> queue = N.asDelayQueue(e1, e2);
        assertEquals(2, queue.size());
        assertTrue(queue instanceof DelayQueue);
    }

    @Test
    public void testAsPriorityQueue() {
        PriorityQueue<String> queue = N.asPriorityQueue("b", "a", "c");
        assertEquals(3, queue.size());
        assertEquals("a", queue.poll());
        assertEquals("b", queue.poll());
        assertEquals("c", queue.poll());
    }

    @Test
    public void testAsDeque() {
        Deque<String> deque = N.asDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testAsArrayDeque() {
        ArrayDeque<String> deque = N.asArrayDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertTrue(deque instanceof ArrayDeque);
    }

    @Test
    public void testAsLinkedBlockingDeque() {
        LinkedBlockingDeque<String> deque = N.asLinkedBlockingDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertTrue(deque instanceof LinkedBlockingDeque);
    }

    @Test
    public void testAsConcurrentLinkedDeque() {
        ConcurrentLinkedDeque<String> deque = N.asConcurrentLinkedDeque("a", "b", "c");
        assertEquals(3, deque.size());
        assertTrue(deque instanceof ConcurrentLinkedDeque);
    }

    @Test
    public void testAsMultiset() {
        Multiset<String> multiset = N.asMultiset("a", "b", "a", "c", "b", "a");
        assertEquals(6, multiset.size());
        assertEquals(3, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testNewMapWithEmptyArguments() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> result = N.newMap(map);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertSame(map, result);
    }

    @Test
    public void testNewMapWithSingleMapArgument() {
        Map<String, Integer> sourceMap = new HashMap<>();
        sourceMap.put("one", 1);
        sourceMap.put("two", 2);

        Map<String, Integer> targetMap = new HashMap<>();
        Map<String, Integer> result = N.newMap(targetMap, sourceMap);

        assertEquals(2, result.size());
        assertEquals(1, result.get("one"));
        assertEquals(2, result.get("two"));
        assertSame(targetMap, result);
    }

    @Test
    public void testNewMapWithKeyValuePairs() {
        Map<String, String> map = new HashMap<>();
        Map<String, String> result = N.newMap(map, "key1", "value1", "key2", "value2");

        assertEquals(2, result.size());
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
        assertSame(map, result);
    }

    @Test
    public void testNewMapWithMultipleKeyValuePairs() {
        Map<Integer, String> map = new HashMap<>();
        Map<Integer, String> result = N.newMap(map, 1, "one", 2, "two", 3, "three");

        assertEquals(3, result.size());
        assertEquals("one", result.get(1));
        assertEquals("two", result.get(2));
        assertEquals("three", result.get(3));
    }

    @Test
    public void testNewMapWithOddNumberOfArguments() {
        Map<String, String> map = new HashMap<>();

        assertThrows(IllegalArgumentException.class, () -> {
            N.newMap(map, "key1", "value1", "key2");
        });
    }

    @Test
    public void testNewMapWithDifferentMapImplementations() {
        // Test with LinkedHashMap
        Map<String, String> linkedMap = new LinkedHashMap<>();
        Map<String, String> result1 = N.newMap(linkedMap, "a", "1", "b", "2");
        assertTrue(result1 instanceof LinkedHashMap);
        assertEquals(2, result1.size());

        // Test with TreeMap
        Map<String, String> treeMap = new TreeMap<>();
        Map<String, String> result2 = N.newMap(treeMap, "x", "10", "y", "20");
        assertTrue(result2 instanceof TreeMap);
        assertEquals(2, result2.size());
    }

    @Test
    public void testAsSingletonList() {
        String element = "test";
        List<String> list = N.asSingletonList(element);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            list.add("another");
        });
    }

    @Test
    public void testAsSingletonListWithNull() {
        List<String> list = N.asSingletonList(null);

        assertNotNull(list);
        assertEquals(1, list.size());
        assertNull(list.get(0));
    }

    @Test
    public void testAsSingletonListWithDifferentTypes() {
        List<Integer> intList = N.asSingletonList(42);
        assertEquals(42, intList.get(0));

        List<Double> doubleList = N.asSingletonList(3.14);
        assertEquals(3.14, doubleList.get(0));

        List<Object> objList = N.asSingletonList(new Object());
        assertNotNull(objList.get(0));
    }

    @Test
    public void testAsSingletonSet() {
        String element = "test";
        Set<String> set = N.asSingletonSet(element);

        assertNotNull(set);
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            set.add("another");
        });
    }

    @Test
    public void testAsSingletonSetWithNull() {
        Set<String> set = N.asSingletonSet(null);

        assertNotNull(set);
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testAsSingletonSetWithDifferentTypes() {
        Set<Integer> intSet = N.asSingletonSet(100);
        assertTrue(intSet.contains(100));

        Set<Boolean> boolSet = N.asSingletonSet(true);
        assertTrue(boolSet.contains(true));
    }

    @Test
    public void testAsSingletonMap() {
        Map<String, Integer> map = N.asSingletonMap("key", 42);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals(42, map.get("key"));
        assertTrue(map.containsKey("key"));

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            map.put("anotherKey", 100);
        });
    }

    @Test
    public void testAsSingletonMapWithNullKey() {
        Map<String, String> map = N.asSingletonMap(null, "value");

        assertNotNull(map);
        assertEquals(1, map.size());
        assertEquals("value", map.get(null));
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testAsSingletonMapWithNullValue() {
        Map<String, String> map = N.asSingletonMap("key", null);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertNull(map.get("key"));
        assertTrue(map.containsKey("key"));
    }

    @Test
    public void testAsSingletonMapWithNullKeyAndValue() {
        Map<String, String> map = N.asSingletonMap(null, null);

        assertNotNull(map);
        assertEquals(1, map.size());
        assertNull(map.get(null));
        assertTrue(map.containsKey(null));
    }

    @Test
    public void testEmptyList() {
        List<String> list = N.emptyList();

        assertNotNull(list);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            list.add("element");
        });
    }

    @Test
    public void testEmptyListReturnsSameInstance() {
        List<String> list1 = N.emptyList();
        List<Integer> list2 = N.emptyList();

        // Should return the same instance due to type erasure
        assertSame(list1, list2);
    }

    @Test
    public void testEmptySet() {
        Set<String> set = N.emptySet();

        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            set.add("element");
        });
    }

    @Test
    public void testEmptySetReturnsSameInstance() {
        Set<String> set1 = N.emptySet();
        Set<Integer> set2 = N.emptySet();

        // Should return the same instance due to type erasure
        assertSame(set1, set2);
    }

    @Test
    public void testEmptySortedSet() {
        SortedSet<String> set = N.emptySortedSet();

        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            set.add("element");
        });
    }

    @Test
    public void testEmptySortedSetReturnsSameInstance() {
        SortedSet<String> set1 = N.emptySortedSet();
        SortedSet<Integer> set2 = N.emptySortedSet();

        // Should return the same instance due to type erasure
        assertSame(set1, set2);
    }

    @Test
    public void testEmptyNavigableSet() {
        NavigableSet<String> set = N.emptyNavigableSet();

        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // Test immutability
        assertThrows(UnsupportedOperationException.class, () -> {
            set.add("element");
        });
    }

    @Test
    public void testEmptyNavigableSetReturnsSameInstance() {
        NavigableSet<String> set1 = N.emptyNavigableSet();
        NavigableSet<Integer> set2 = N.emptyNavigableSet();

        // Should return the same instance due to type erasure
        assertSame(set1, set2);
    }
}
