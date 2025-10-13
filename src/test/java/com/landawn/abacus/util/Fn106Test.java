package com.landawn.abacus.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;

@Tag("new-test")
public class Fn106Test extends TestBase {

    @Nested
    public class LongSuppliersTest extends TestBase {

        @Test
        public void testOfCurrentTimeMillis() {
            LongSupplier supplier = Fn.LongSuppliers.ofCurrentTimeMillis();
            Assertions.assertNotNull(supplier);

            long time1 = supplier.getAsLong();
            Assertions.assertTrue(time1 > 0);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            long time2 = supplier.getAsLong();
            Assertions.assertTrue(time2 >= time1);
        }

        @Test
        public void testOfCurrentTimeMillisMultipleCalls() {
            LongSupplier supplier1 = Fn.LongSuppliers.ofCurrentTimeMillis();
            LongSupplier supplier2 = Fn.LongSuppliers.ofCurrentTimeMillis();

            Assertions.assertSame(supplier1, supplier2);
        }
    }

    @Nested
    public class SuppliersTest extends TestBase {

        @Test
        public void testOf_Supplier() {
            Supplier<String> original = () -> "test";
            Supplier<String> result = Suppliers.of(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("test", result.get());
        }

        @Test
        public void testOf_WithFunction() {
            String input = "test";
            Function<String, Integer> func = String::length;
            Supplier<Integer> supplier = Suppliers.of(input, func);

            Assertions.assertNotNull(supplier);
            Assertions.assertEquals(4, supplier.get());
            Assertions.assertEquals(4, supplier.get());
        }

        @Test
        public void testOfInstance() {
            String instance = "test instance";
            Supplier<String> supplier = Suppliers.ofInstance(instance);

            Assertions.assertNotNull(supplier);
            Assertions.assertSame(instance, supplier.get());
            Assertions.assertSame(instance, supplier.get());
        }

        @Test
        public void testOfUUID() {
            Supplier<String> supplier = Suppliers.ofUUID();
            Assertions.assertNotNull(supplier);

            String uuid1 = supplier.get();
            String uuid2 = supplier.get();

            Assertions.assertNotNull(uuid1);
            Assertions.assertNotNull(uuid2);
            Assertions.assertNotEquals(uuid1, uuid2);

            Assertions.assertSame(supplier, Suppliers.ofUUID());
        }

        @Test
        public void testOfGUID() {
            Supplier<String> supplier = Suppliers.ofGUID();
            Assertions.assertNotNull(supplier);

            String guid1 = supplier.get();
            String guid2 = supplier.get();

            Assertions.assertNotNull(guid1);
            Assertions.assertNotNull(guid2);
            Assertions.assertNotEquals(guid1, guid2);

            Assertions.assertSame(supplier, Suppliers.ofGUID());
        }

        @Test
        public void testOfEmptyBooleanArray() {
            Supplier<boolean[]> supplier = Suppliers.ofEmptyBooleanArray();
            Assertions.assertNotNull(supplier);

            boolean[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyBooleanArray());
        }

        @Test
        public void testOfEmptyCharArray() {
            Supplier<char[]> supplier = Suppliers.ofEmptyCharArray();
            Assertions.assertNotNull(supplier);

            char[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyCharArray());
        }

        @Test
        public void testOfEmptyByteArray() {
            Supplier<byte[]> supplier = Suppliers.ofEmptyByteArray();
            Assertions.assertNotNull(supplier);

            byte[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyByteArray());
        }

        @Test
        public void testOfEmptyShortArray() {
            Supplier<short[]> supplier = Suppliers.ofEmptyShortArray();
            Assertions.assertNotNull(supplier);

            short[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyShortArray());
        }

        @Test
        public void testOfEmptyIntArray() {
            Supplier<int[]> supplier = Suppliers.ofEmptyIntArray();
            Assertions.assertNotNull(supplier);

            int[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyIntArray());
        }

        @Test
        public void testOfEmptyLongArray() {
            Supplier<long[]> supplier = Suppliers.ofEmptyLongArray();
            Assertions.assertNotNull(supplier);

            long[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyLongArray());
        }

        @Test
        public void testOfEmptyFloatArray() {
            Supplier<float[]> supplier = Suppliers.ofEmptyFloatArray();
            Assertions.assertNotNull(supplier);

            float[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyFloatArray());
        }

        @Test
        public void testOfEmptyDoubleArray() {
            Supplier<double[]> supplier = Suppliers.ofEmptyDoubleArray();
            Assertions.assertNotNull(supplier);

            double[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyDoubleArray());
        }

        @Test
        public void testOfEmptyStringArray() {
            Supplier<String[]> supplier = Suppliers.ofEmptyStringArray();
            Assertions.assertNotNull(supplier);

            String[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyStringArray());
        }

        @Test
        public void testOfEmptyObjectArray() {
            Supplier<Object[]> supplier = Suppliers.ofEmptyObjectArray();
            Assertions.assertNotNull(supplier);

            Object[] array = supplier.get();
            Assertions.assertNotNull(array);
            Assertions.assertEquals(0, array.length);

            Assertions.assertSame(array, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyObjectArray());
        }

        @Test
        public void testOfEmptyString() {
            Supplier<String> supplier = Suppliers.ofEmptyString();
            Assertions.assertNotNull(supplier);

            String str = supplier.get();
            Assertions.assertNotNull(str);
            Assertions.assertEquals("", str);
            Assertions.assertEquals(0, str.length());

            Assertions.assertSame(str, supplier.get());
            Assertions.assertSame(supplier, Suppliers.ofEmptyString());
        }

        @Test
        public void testOfBooleanList() {
            Supplier<BooleanList> supplier = Suppliers.ofBooleanList();
            Assertions.assertNotNull(supplier);

            BooleanList list1 = supplier.get();
            BooleanList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofBooleanList());
        }

        @Test
        public void testOfCharList() {
            Supplier<CharList> supplier = Suppliers.ofCharList();
            Assertions.assertNotNull(supplier);

            CharList list1 = supplier.get();
            CharList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofCharList());
        }

        @Test
        public void testOfByteList() {
            Supplier<ByteList> supplier = Suppliers.ofByteList();
            Assertions.assertNotNull(supplier);

            ByteList list1 = supplier.get();
            ByteList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofByteList());
        }

        @Test
        public void testOfShortList() {
            Supplier<ShortList> supplier = Suppliers.ofShortList();
            Assertions.assertNotNull(supplier);

            ShortList list1 = supplier.get();
            ShortList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofShortList());
        }

        @Test
        public void testOfIntList() {
            Supplier<IntList> supplier = Suppliers.ofIntList();
            Assertions.assertNotNull(supplier);

            IntList list1 = supplier.get();
            IntList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofIntList());
        }

        @Test
        public void testOfLongList() {
            Supplier<LongList> supplier = Suppliers.ofLongList();
            Assertions.assertNotNull(supplier);

            LongList list1 = supplier.get();
            LongList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofLongList());
        }

        @Test
        public void testOfFloatList() {
            Supplier<FloatList> supplier = Suppliers.ofFloatList();
            Assertions.assertNotNull(supplier);

            FloatList list1 = supplier.get();
            FloatList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofFloatList());
        }

        @Test
        public void testOfDoubleList() {
            Supplier<DoubleList> supplier = Suppliers.ofDoubleList();
            Assertions.assertNotNull(supplier);

            DoubleList list1 = supplier.get();
            DoubleList list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofDoubleList());
        }

        @Test
        public void testOfList() {
            Supplier<List<String>> supplier = Suppliers.ofList();
            Assertions.assertNotNull(supplier);

            List<String> list1 = supplier.get();
            List<String> list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());
            Assertions.assertTrue(list1 instanceof ArrayList);

            Assertions.assertSame(supplier, Suppliers.ofList());
        }

        @Test
        public void testOfLinkedList() {
            Supplier<LinkedList<String>> supplier = Suppliers.ofLinkedList();
            Assertions.assertNotNull(supplier);

            LinkedList<String> list1 = supplier.get();
            LinkedList<String> list2 = supplier.get();

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(supplier, Suppliers.ofLinkedList());
        }

        @Test
        public void testOfSet() {
            Supplier<Set<String>> supplier = Suppliers.ofSet();
            Assertions.assertNotNull(supplier);

            Set<String> set1 = supplier.get();
            Set<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof HashSet);

            Assertions.assertSame(supplier, Suppliers.ofSet());
        }

        @Test
        public void testOfLinkedHashSet() {
            Supplier<Set<String>> supplier = Suppliers.ofLinkedHashSet();
            Assertions.assertNotNull(supplier);

            Set<String> set1 = supplier.get();
            Set<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof LinkedHashSet);

            Assertions.assertSame(supplier, Suppliers.ofLinkedHashSet());
        }

        @Test
        public void testOfSortedSet() {
            Supplier<SortedSet<String>> supplier = Suppliers.ofSortedSet();
            Assertions.assertNotNull(supplier);

            SortedSet<String> set1 = supplier.get();
            SortedSet<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof TreeSet);

            Assertions.assertSame(supplier, Suppliers.ofSortedSet());
        }

        @Test
        public void testOfNavigableSet() {
            Supplier<NavigableSet<String>> supplier = Suppliers.ofNavigableSet();
            Assertions.assertNotNull(supplier);

            NavigableSet<String> set1 = supplier.get();
            NavigableSet<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof TreeSet);

            Assertions.assertSame(supplier, Suppliers.ofNavigableSet());
        }

        @Test
        public void testOfTreeSet() {
            Supplier<TreeSet<String>> supplier = Suppliers.ofTreeSet();
            Assertions.assertNotNull(supplier);

            TreeSet<String> set1 = supplier.get();
            TreeSet<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());

            Assertions.assertSame(supplier, Suppliers.ofTreeSet());
        }

        @Test
        public void testOfQueue() {
            Supplier<Queue<String>> supplier = Suppliers.ofQueue();
            Assertions.assertNotNull(supplier);

            Queue<String> queue1 = supplier.get();
            Queue<String> queue2 = supplier.get();

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());
            Assertions.assertTrue(queue1 instanceof LinkedList);

            Assertions.assertSame(supplier, Suppliers.ofQueue());
        }

        @Test
        public void testOfDeque() {
            Supplier<Deque<String>> supplier = Suppliers.ofDeque();
            Assertions.assertNotNull(supplier);

            Deque<String> deque1 = supplier.get();
            Deque<String> deque2 = supplier.get();

            Assertions.assertNotNull(deque1);
            Assertions.assertNotNull(deque2);
            Assertions.assertNotSame(deque1, deque2);
            Assertions.assertEquals(0, deque1.size());
            Assertions.assertTrue(deque1 instanceof LinkedList);

            Assertions.assertSame(supplier, Suppliers.ofDeque());
        }

        @Test
        public void testOfArrayDeque() {
            Supplier<ArrayDeque<String>> supplier = Suppliers.ofArrayDeque();
            Assertions.assertNotNull(supplier);

            ArrayDeque<String> deque1 = supplier.get();
            ArrayDeque<String> deque2 = supplier.get();

            Assertions.assertNotNull(deque1);
            Assertions.assertNotNull(deque2);
            Assertions.assertNotSame(deque1, deque2);
            Assertions.assertEquals(0, deque1.size());

            Assertions.assertSame(supplier, Suppliers.ofArrayDeque());
        }

        @Test
        public void testOfLinkedBlockingQueue() {
            Supplier<LinkedBlockingQueue<String>> supplier = Suppliers.ofLinkedBlockingQueue();
            Assertions.assertNotNull(supplier);

            LinkedBlockingQueue<String> queue1 = supplier.get();
            LinkedBlockingQueue<String> queue2 = supplier.get();

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());

            Assertions.assertSame(supplier, Suppliers.ofLinkedBlockingQueue());
        }

        @Test
        public void testOfLinkedBlockingDeque() {
            Supplier<LinkedBlockingDeque<String>> supplier = Suppliers.ofLinkedBlockingDeque();
            Assertions.assertNotNull(supplier);

            LinkedBlockingDeque<String> deque1 = supplier.get();
            LinkedBlockingDeque<String> deque2 = supplier.get();

            Assertions.assertNotNull(deque1);
            Assertions.assertNotNull(deque2);
            Assertions.assertNotSame(deque1, deque2);
            Assertions.assertEquals(0, deque1.size());

            Assertions.assertSame(supplier, Suppliers.ofLinkedBlockingDeque());
        }

        @Test
        public void testOfConcurrentLinkedQueue() {
            Supplier<ConcurrentLinkedQueue<String>> supplier = Suppliers.ofConcurrentLinkedQueue();
            Assertions.assertNotNull(supplier);

            ConcurrentLinkedQueue<String> queue1 = supplier.get();
            ConcurrentLinkedQueue<String> queue2 = supplier.get();

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());

            Assertions.assertSame(supplier, Suppliers.ofConcurrentLinkedQueue());
        }

        @Test
        public void testOfPriorityQueue() {
            Supplier<PriorityQueue<String>> supplier = Suppliers.ofPriorityQueue();
            Assertions.assertNotNull(supplier);

            PriorityQueue<String> queue1 = supplier.get();
            PriorityQueue<String> queue2 = supplier.get();

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());

            Assertions.assertSame(supplier, Suppliers.ofPriorityQueue());
        }

        @Test
        public void testOfMap() {
            Supplier<Map<String, Integer>> supplier = Suppliers.ofMap();
            Assertions.assertNotNull(supplier);

            Map<String, Integer> map1 = supplier.get();
            Map<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof HashMap);

            Assertions.assertSame(supplier, Suppliers.ofMap());
        }

        @Test
        public void testOfLinkedHashMap() {
            Supplier<Map<String, Integer>> supplier = Suppliers.ofLinkedHashMap();
            Assertions.assertNotNull(supplier);

            Map<String, Integer> map1 = supplier.get();
            Map<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof LinkedHashMap);

            Assertions.assertSame(supplier, Suppliers.ofLinkedHashMap());
        }

        @Test
        public void testOfIdentityHashMap() {
            Supplier<IdentityHashMap<String, Integer>> supplier = Suppliers.ofIdentityHashMap();
            Assertions.assertNotNull(supplier);

            IdentityHashMap<String, Integer> map1 = supplier.get();
            IdentityHashMap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(supplier, Suppliers.ofIdentityHashMap());
        }

        @Test
        public void testOfSortedMap() {
            Supplier<SortedMap<String, Integer>> supplier = Suppliers.ofSortedMap();
            Assertions.assertNotNull(supplier);

            SortedMap<String, Integer> map1 = supplier.get();
            SortedMap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof TreeMap);

            Assertions.assertSame(supplier, Suppliers.ofSortedMap());
        }

        @Test
        public void testOfNavigableMap() {
            Supplier<NavigableMap<String, Integer>> supplier = Suppliers.ofNavigableMap();
            Assertions.assertNotNull(supplier);

            NavigableMap<String, Integer> map1 = supplier.get();
            NavigableMap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof TreeMap);

            Assertions.assertSame(supplier, Suppliers.ofNavigableMap());
        }

        @Test
        public void testOfTreeMap() {
            Supplier<TreeMap<String, Integer>> supplier = Suppliers.ofTreeMap();
            Assertions.assertNotNull(supplier);

            TreeMap<String, Integer> map1 = supplier.get();
            TreeMap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(supplier, Suppliers.ofTreeMap());
        }

        @Test
        public void testOfConcurrentMap() {
            Supplier<ConcurrentMap<String, Integer>> supplier = Suppliers.ofConcurrentMap();
            Assertions.assertNotNull(supplier);

            ConcurrentMap<String, Integer> map1 = supplier.get();
            ConcurrentMap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof ConcurrentHashMap);

            Assertions.assertSame(supplier, Suppliers.ofConcurrentMap());
        }

        @Test
        public void testOfConcurrentHashMap() {
            Supplier<ConcurrentHashMap<String, Integer>> supplier = Suppliers.ofConcurrentHashMap();
            Assertions.assertNotNull(supplier);

            ConcurrentHashMap<String, Integer> map1 = supplier.get();
            ConcurrentHashMap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(supplier, Suppliers.ofConcurrentHashMap());
        }

        @Test
        public void testOfConcurrentHashSet() {
            Supplier<Set<String>> supplier = Suppliers.ofConcurrentHashSet();
            Assertions.assertNotNull(supplier);

            Set<String> set1 = supplier.get();
            Set<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());

            Assertions.assertSame(supplier, Suppliers.ofConcurrentHashSet());
        }

        @Test
        public void testOfBiMap() {
            Supplier<BiMap<String, Integer>> supplier = Suppliers.ofBiMap();
            Assertions.assertNotNull(supplier);

            BiMap<String, Integer> map1 = supplier.get();
            BiMap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(supplier, Suppliers.ofBiMap());
        }

        @Test
        public void testOfMultiset() {
            Supplier<Multiset<String>> supplier = Suppliers.ofMultiset();
            Assertions.assertNotNull(supplier);

            Multiset<String> set1 = supplier.get();
            Multiset<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());

            Assertions.assertSame(supplier, Suppliers.ofMultiset());
        }

        @Test
        public void testOfMultisetWithMapType() {
            Supplier<Multiset<String>> supplier = Suppliers.ofMultiset(LinkedHashMap.class);
            Assertions.assertNotNull(supplier);

            Multiset<String> set1 = supplier.get();
            Multiset<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
        }

        @Test
        public void testOfMultisetWithMapSupplier() {
            java.util.function.Supplier<Map<String, ?>> mapSupplier = LinkedHashMap::new;
            Supplier<Multiset<String>> supplier = Suppliers.ofMultiset(mapSupplier);
            Assertions.assertNotNull(supplier);

            Multiset<String> set1 = supplier.get();
            Multiset<String> set2 = supplier.get();

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
        }

        @Test
        public void testOfListMultimap() {
            Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap();
            Assertions.assertNotNull(supplier);

            ListMultimap<String, Integer> map1 = supplier.get();
            ListMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(supplier, Suppliers.ofListMultimap());
        }

        @Test
        public void testOfListMultimapWithMapType() {
            Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap(LinkedHashMap.class);
            Assertions.assertNotNull(supplier);

            ListMultimap<String, Integer> map1 = supplier.get();
            ListMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
        }

        @Test
        public void testOfListMultimapWithMapAndValueType() {
            Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap(HashMap.class, LinkedList.class);
            Assertions.assertNotNull(supplier);

            ListMultimap<String, Integer> map1 = supplier.get();
            ListMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
        }

        @Test
        public void testOfListMultimapWithSuppliers() {
            java.util.function.Supplier<Map<String, List<Integer>>> mapSupplier = HashMap::new;
            java.util.function.Supplier<List<Integer>> valueSupplier = ArrayList::new;
            Supplier<ListMultimap<String, Integer>> supplier = Suppliers.ofListMultimap(mapSupplier, valueSupplier);
            Assertions.assertNotNull(supplier);

            ListMultimap<String, Integer> map1 = supplier.get();
            ListMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
        }

        @Test
        public void testOfSetMultimap() {
            Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap();
            Assertions.assertNotNull(supplier);

            SetMultimap<String, Integer> map1 = supplier.get();
            SetMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(supplier, Suppliers.ofSetMultimap());
        }

        @Test
        public void testOfSetMultimapWithMapType() {
            Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap(LinkedHashMap.class);
            Assertions.assertNotNull(supplier);

            SetMultimap<String, Integer> map1 = supplier.get();
            SetMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
        }

        @Test
        public void testOfSetMultimapWithMapAndValueType() {
            Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap(HashMap.class, LinkedHashSet.class);
            Assertions.assertNotNull(supplier);

            SetMultimap<String, Integer> map1 = supplier.get();
            SetMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
        }

        @Test
        public void testOfSetMultimapWithSuppliers() {
            java.util.function.Supplier<Map<String, Set<Integer>>> mapSupplier = HashMap::new;
            java.util.function.Supplier<Set<Integer>> valueSupplier = HashSet::new;
            Supplier<SetMultimap<String, Integer>> supplier = Suppliers.ofSetMultimap(mapSupplier, valueSupplier);
            Assertions.assertNotNull(supplier);

            SetMultimap<String, Integer> map1 = supplier.get();
            SetMultimap<String, Integer> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
        }

        @Test
        public void testOfMultimap() {
            java.util.function.Supplier<Map<String, List<Integer>>> mapSupplier = HashMap::new;
            java.util.function.Supplier<List<Integer>> valueSupplier = ArrayList::new;
            Supplier<Multimap<String, Integer, List<Integer>>> supplier = Suppliers.ofMultimap(mapSupplier, valueSupplier);
            Assertions.assertNotNull(supplier);

            Multimap<String, Integer, List<Integer>> map1 = supplier.get();
            Multimap<String, Integer, List<Integer>> map2 = supplier.get();

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
        }

        @Test
        public void testOfStringBuilder() {
            Supplier<StringBuilder> supplier = Suppliers.ofStringBuilder();
            Assertions.assertNotNull(supplier);

            StringBuilder sb1 = supplier.get();
            StringBuilder sb2 = supplier.get();

            Assertions.assertNotNull(sb1);
            Assertions.assertNotNull(sb2);
            Assertions.assertNotSame(sb1, sb2);
            Assertions.assertEquals(0, sb1.length());

            Assertions.assertSame(supplier, Suppliers.ofStringBuilder());
        }

        @Test
        public void testOfCollection() {
            Supplier<? extends Collection<String>> supplier = Suppliers.<String> ofCollection(ArrayList.class);
            Assertions.assertNotNull(supplier);
            Collection<String> coll = supplier.get();
            Assertions.assertTrue(coll instanceof ArrayList);

            supplier = Suppliers.<String> ofCollection(LinkedList.class);
            coll = supplier.get();
            Assertions.assertTrue(coll instanceof LinkedList);

            supplier = Suppliers.<String> ofCollection(HashSet.class);
            coll = supplier.get();
            Assertions.assertTrue(coll instanceof HashSet);

            supplier = Suppliers.<String> ofCollection(Collection.class);
            coll = supplier.get();
            Assertions.assertTrue(coll instanceof ArrayList);

            Supplier<? extends Collection<String>> supplier2 = Suppliers.ofCollection(ArrayList.class);
            Assertions.assertSame(supplier2, Suppliers.ofCollection(ArrayList.class));
        }

        @Test
        public void testOfCollectionWithInvalidType() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Suppliers.ofCollection((Class) Map.class);
            });
        }

        @Test
        public void testOfMap_Class() {
            Supplier<? extends Map<String, Integer>> supplier = Suppliers.ofMap(HashMap.class);
            Assertions.assertNotNull(supplier);
            Map<String, Integer> map = supplier.get();
            Assertions.assertTrue(map instanceof HashMap);

            supplier = Suppliers.ofMap(LinkedHashMap.class);
            map = supplier.get();
            Assertions.assertTrue(map instanceof LinkedHashMap);

            supplier = Suppliers.ofMap(TreeMap.class);
            map = supplier.get();
            Assertions.assertTrue(map instanceof TreeMap);

            supplier = Suppliers.ofMap(Map.class);
            map = supplier.get();
            Assertions.assertTrue(map instanceof HashMap);

            Supplier<? extends Map<String, Integer>> supplier2 = Suppliers.ofMap(HashMap.class);
            Assertions.assertSame(supplier2, Suppliers.ofMap(HashMap.class));
        }

        @Test
        public void testOfMapWithInvalidType() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Suppliers.ofMap((Class) List.class);
            });
        }

        @Test
        public void testRegisterForCollection() {
            class CustomCollection<T> extends ArrayList<T> {
            }

            java.util.function.Supplier<CustomCollection> customSupplier = CustomCollection::new;

            Assertions.assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForCollection(CustomCollection.class, customSupplier));
        }

        @Test
        public void testRegisterForCollectionWithBuiltinClass() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Suppliers.registerForCollection(ArrayList.class, ArrayList::new);
            });
        }

        @Test
        public void testRegisterForMap() {
            class CustomMap<K, V> extends HashMap<K, V> {
            }

            java.util.function.Supplier<CustomMap> customSupplier = CustomMap::new;

            Assertions.assertThrows(IllegalArgumentException.class, () -> Suppliers.registerForMap(CustomMap.class, customSupplier));

        }

        @Test
        public void testRegisterForMapWithBuiltinClass() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                Suppliers.registerForMap(HashMap.class, HashMap::new);
            });
        }

        @Test
        public void testOfImmutableList() {
            Assertions.assertThrows(UnsupportedOperationException.class, () -> {
                Suppliers.ofImmutableList();
            });
        }

        @Test
        public void testOfImmutableSet() {
            Assertions.assertThrows(UnsupportedOperationException.class, () -> {
                Suppliers.ofImmutableSet();
            });
        }

        @Test
        public void testOfImmutableMap() {
            Assertions.assertThrows(UnsupportedOperationException.class, () -> {
                Suppliers.ofImmutableMap();
            });
        }

        @Test
        public void testNewException() {
            Supplier<Exception> supplier = Suppliers.newException();
            Assertions.assertNotNull(supplier);

            Exception ex1 = supplier.get();
            Exception ex2 = supplier.get();

            Assertions.assertNotNull(ex1);
            Assertions.assertNotNull(ex2);
            Assertions.assertNotSame(ex1, ex2);

            Assertions.assertSame(supplier, Suppliers.newException());
        }

        @Test
        public void testNewRuntimeException() {
            Supplier<RuntimeException> supplier = Suppliers.newRuntimeException();
            Assertions.assertNotNull(supplier);

            RuntimeException ex1 = supplier.get();
            RuntimeException ex2 = supplier.get();

            Assertions.assertNotNull(ex1);
            Assertions.assertNotNull(ex2);
            Assertions.assertNotSame(ex1, ex2);

            Assertions.assertSame(supplier, Suppliers.newRuntimeException());
        }

        @Test
        public void testNewNoSuchElementException() {
            Supplier<NoSuchElementException> supplier = Suppliers.newNoSuchElementException();
            Assertions.assertNotNull(supplier);

            NoSuchElementException ex1 = supplier.get();
            NoSuchElementException ex2 = supplier.get();

            Assertions.assertNotNull(ex1);
            Assertions.assertNotNull(ex2);
            Assertions.assertNotSame(ex1, ex2);

            Assertions.assertSame(supplier, Suppliers.newNoSuchElementException());
        }
    }

    @Nested
    public class IntFunctionsTest extends TestBase {

        @Test
        public void testOf() {
            IntFunction<String> original = size -> "Size: " + size;
            IntFunction<String> result = IntFunctions.of(original);
            Assertions.assertSame(original, result);
            Assertions.assertEquals("Size: 5", result.apply(5));
        }

        @Test
        public void testOfBooleanArray() {
            IntFunction<boolean[]> func = IntFunctions.ofBooleanArray();
            Assertions.assertNotNull(func);

            boolean[] array1 = func.apply(5);
            boolean[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (boolean b : array1) {
                Assertions.assertFalse(b);
            }

            Assertions.assertSame(func, IntFunctions.ofBooleanArray());
        }

        @Test
        public void testOfCharArray() {
            IntFunction<char[]> func = IntFunctions.ofCharArray();
            Assertions.assertNotNull(func);

            char[] array1 = func.apply(5);
            char[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (char c : array1) {
                Assertions.assertEquals('\u0000', c);
            }

            Assertions.assertSame(func, IntFunctions.ofCharArray());
        }

        @Test
        public void testOfByteArray() {
            IntFunction<byte[]> func = IntFunctions.ofByteArray();
            Assertions.assertNotNull(func);

            byte[] array1 = func.apply(5);
            byte[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (byte b : array1) {
                Assertions.assertEquals((byte) 0, b);
            }

            Assertions.assertSame(func, IntFunctions.ofByteArray());
        }

        @Test
        public void testOfShortArray() {
            IntFunction<short[]> func = IntFunctions.ofShortArray();
            Assertions.assertNotNull(func);

            short[] array1 = func.apply(5);
            short[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (short s : array1) {
                Assertions.assertEquals((short) 0, s);
            }

            Assertions.assertSame(func, IntFunctions.ofShortArray());
        }

        @Test
        public void testOfIntArray() {
            IntFunction<int[]> func = IntFunctions.ofIntArray();
            Assertions.assertNotNull(func);

            int[] array1 = func.apply(5);
            int[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (int i : array1) {
                Assertions.assertEquals(0, i);
            }

            Assertions.assertSame(func, IntFunctions.ofIntArray());
        }

        @Test
        public void testOfLongArray() {
            IntFunction<long[]> func = IntFunctions.ofLongArray();
            Assertions.assertNotNull(func);

            long[] array1 = func.apply(5);
            long[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (long l : array1) {
                Assertions.assertEquals(0L, l);
            }

            Assertions.assertSame(func, IntFunctions.ofLongArray());
        }

        @Test
        public void testOfFloatArray() {
            IntFunction<float[]> func = IntFunctions.ofFloatArray();
            Assertions.assertNotNull(func);

            float[] array1 = func.apply(5);
            float[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (float f : array1) {
                Assertions.assertEquals(0.0f, f);
            }

            Assertions.assertSame(func, IntFunctions.ofFloatArray());
        }

        @Test
        public void testOfDoubleArray() {
            IntFunction<double[]> func = IntFunctions.ofDoubleArray();
            Assertions.assertNotNull(func);

            double[] array1 = func.apply(5);
            double[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (double d : array1) {
                Assertions.assertEquals(0.0, d);
            }

            Assertions.assertSame(func, IntFunctions.ofDoubleArray());
        }

        @Test
        public void testOfStringArray() {
            IntFunction<String[]> func = IntFunctions.ofStringArray();
            Assertions.assertNotNull(func);

            String[] array1 = func.apply(5);
            String[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (String s : array1) {
                Assertions.assertNull(s);
            }

            Assertions.assertSame(func, IntFunctions.ofStringArray());
        }

        @Test
        public void testOfObjectArray() {
            IntFunction<Object[]> func = IntFunctions.ofObjectArray();
            Assertions.assertNotNull(func);

            Object[] array1 = func.apply(5);
            Object[] array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertEquals(5, array1.length);
            Assertions.assertEquals(10, array2.length);
            Assertions.assertNotSame(array1, array2);

            for (Object o : array1) {
                Assertions.assertNull(o);
            }

            Assertions.assertSame(func, IntFunctions.ofObjectArray());
        }

        @Test
        public void testOfBooleanList() {
            IntFunction<BooleanList> func = IntFunctions.ofBooleanList();
            Assertions.assertNotNull(func);

            BooleanList list1 = func.apply(5);
            BooleanList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofBooleanList());
        }

        @Test
        public void testOfCharList() {
            IntFunction<CharList> func = IntFunctions.ofCharList();
            Assertions.assertNotNull(func);

            CharList list1 = func.apply(5);
            CharList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofCharList());
        }

        @Test
        public void testOfByteList() {
            IntFunction<ByteList> func = IntFunctions.ofByteList();
            Assertions.assertNotNull(func);

            ByteList list1 = func.apply(5);
            ByteList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofByteList());
        }

        @Test
        public void testOfShortList() {
            IntFunction<ShortList> func = IntFunctions.ofShortList();
            Assertions.assertNotNull(func);

            ShortList list1 = func.apply(5);
            ShortList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofShortList());
        }

        @Test
        public void testOfIntList() {
            IntFunction<IntList> func = IntFunctions.ofIntList();
            Assertions.assertNotNull(func);

            IntList list1 = func.apply(5);
            IntList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofIntList());
        }

        @Test
        public void testOfLongList() {
            IntFunction<LongList> func = IntFunctions.ofLongList();
            Assertions.assertNotNull(func);

            LongList list1 = func.apply(5);
            LongList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofLongList());
        }

        @Test
        public void testOfFloatList() {
            IntFunction<FloatList> func = IntFunctions.ofFloatList();
            Assertions.assertNotNull(func);

            FloatList list1 = func.apply(5);
            FloatList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofFloatList());
        }

        @Test
        public void testOfDoubleList() {
            IntFunction<DoubleList> func = IntFunctions.ofDoubleList();
            Assertions.assertNotNull(func);

            DoubleList list1 = func.apply(5);
            DoubleList list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofDoubleList());
        }

        @Test
        public void testOfList() {
            IntFunction<List<String>> func = IntFunctions.ofList();
            Assertions.assertNotNull(func);

            List<String> list1 = func.apply(5);
            List<String> list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());
            Assertions.assertTrue(list1 instanceof ArrayList);

            Assertions.assertSame(func, IntFunctions.ofList());
        }

        @Test
        public void testOfLinkedList() {
            IntFunction<LinkedList<String>> func = IntFunctions.ofLinkedList();
            Assertions.assertNotNull(func);

            LinkedList<String> list1 = func.apply(5);
            LinkedList<String> list2 = func.apply(10);

            Assertions.assertNotNull(list1);
            Assertions.assertNotNull(list2);
            Assertions.assertNotSame(list1, list2);
            Assertions.assertEquals(0, list1.size());

            Assertions.assertSame(func, IntFunctions.ofLinkedList());
        }

        @Test
        public void testOfSet() {
            IntFunction<Set<String>> func = IntFunctions.ofSet();
            Assertions.assertNotNull(func);

            Set<String> set1 = func.apply(5);
            Set<String> set2 = func.apply(10);

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof HashSet);

            Assertions.assertSame(func, IntFunctions.ofSet());
        }

        @Test
        public void testOfLinkedHashSet() {
            IntFunction<Set<String>> func = IntFunctions.ofLinkedHashSet();
            Assertions.assertNotNull(func);

            Set<String> set1 = func.apply(5);
            Set<String> set2 = func.apply(10);

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof LinkedHashSet);

            Assertions.assertSame(func, IntFunctions.ofLinkedHashSet());
        }

        @Test
        public void testOfSortedSet() {
            IntFunction<SortedSet<String>> func = IntFunctions.ofSortedSet();
            Assertions.assertNotNull(func);

            SortedSet<String> set1 = func.apply(5);
            SortedSet<String> set2 = func.apply(10);

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof TreeSet);

            Assertions.assertSame(func, IntFunctions.ofSortedSet());
        }

        @Test
        public void testOfNavigableSet() {
            IntFunction<NavigableSet<String>> func = IntFunctions.ofNavigableSet();
            Assertions.assertNotNull(func);

            NavigableSet<String> set1 = func.apply(5);
            NavigableSet<String> set2 = func.apply(10);

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());
            Assertions.assertTrue(set1 instanceof TreeSet);

            Assertions.assertSame(func, IntFunctions.ofNavigableSet());
        }

        @Test
        public void testOfTreeSet() {
            IntFunction<TreeSet<String>> func = IntFunctions.ofTreeSet();
            Assertions.assertNotNull(func);

            TreeSet<String> set1 = func.apply(5);
            TreeSet<String> set2 = func.apply(10);

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());

            Assertions.assertSame(func, IntFunctions.ofTreeSet());
        }

        @Test
        public void testOfQueue() {
            IntFunction<Queue<String>> func = IntFunctions.ofQueue();
            Assertions.assertNotNull(func);

            Queue<String> queue1 = func.apply(5);
            Queue<String> queue2 = func.apply(10);

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());
            Assertions.assertTrue(queue1 instanceof LinkedList);

            Assertions.assertSame(func, IntFunctions.ofQueue());
        }

        @Test
        public void testOfDeque() {
            IntFunction<Deque<String>> func = IntFunctions.ofDeque();
            Assertions.assertNotNull(func);

            Deque<String> deque1 = func.apply(5);
            Deque<String> deque2 = func.apply(10);

            Assertions.assertNotNull(deque1);
            Assertions.assertNotNull(deque2);
            Assertions.assertNotSame(deque1, deque2);
            Assertions.assertEquals(0, deque1.size());
            Assertions.assertTrue(deque1 instanceof LinkedList);

            Assertions.assertSame(func, IntFunctions.ofDeque());
        }

        @Test
        public void testOfArrayDeque() {
            IntFunction<ArrayDeque<String>> func = IntFunctions.ofArrayDeque();
            Assertions.assertNotNull(func);

            ArrayDeque<String> deque1 = func.apply(5);
            ArrayDeque<String> deque2 = func.apply(10);

            Assertions.assertNotNull(deque1);
            Assertions.assertNotNull(deque2);
            Assertions.assertNotSame(deque1, deque2);
            Assertions.assertEquals(0, deque1.size());

            Assertions.assertSame(func, IntFunctions.ofArrayDeque());
        }

        @Test
        public void testOfLinkedBlockingQueue() {
            IntFunction<LinkedBlockingQueue<String>> func = IntFunctions.ofLinkedBlockingQueue();
            Assertions.assertNotNull(func);

            LinkedBlockingQueue<String> queue1 = func.apply(5);
            LinkedBlockingQueue<String> queue2 = func.apply(10);

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());
            Assertions.assertEquals(5, queue1.remainingCapacity());
            Assertions.assertEquals(10, queue2.remainingCapacity());

            Assertions.assertSame(func, IntFunctions.ofLinkedBlockingQueue());
        }

        @Test
        public void testOfArrayBlockingQueue() {
            IntFunction<ArrayBlockingQueue<String>> func = IntFunctions.ofArrayBlockingQueue();
            Assertions.assertNotNull(func);

            ArrayBlockingQueue<String> queue1 = func.apply(5);
            ArrayBlockingQueue<String> queue2 = func.apply(10);

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());
            Assertions.assertEquals(5, queue1.remainingCapacity());
            Assertions.assertEquals(10, queue2.remainingCapacity());

            Assertions.assertSame(func, IntFunctions.ofArrayBlockingQueue());
        }

        @Test
        public void testOfLinkedBlockingDeque() {
            IntFunction<LinkedBlockingDeque<String>> func = IntFunctions.ofLinkedBlockingDeque();
            Assertions.assertNotNull(func);

            LinkedBlockingDeque<String> deque1 = func.apply(5);
            LinkedBlockingDeque<String> deque2 = func.apply(10);

            Assertions.assertNotNull(deque1);
            Assertions.assertNotNull(deque2);
            Assertions.assertNotSame(deque1, deque2);
            Assertions.assertEquals(0, deque1.size());
            Assertions.assertEquals(5, deque1.remainingCapacity());
            Assertions.assertEquals(10, deque2.remainingCapacity());

            Assertions.assertSame(func, IntFunctions.ofLinkedBlockingDeque());
        }

        @Test
        public void testOfConcurrentLinkedQueue() {
            IntFunction<ConcurrentLinkedQueue<String>> func = IntFunctions.ofConcurrentLinkedQueue();
            Assertions.assertNotNull(func);

            ConcurrentLinkedQueue<String> queue1 = func.apply(5);
            ConcurrentLinkedQueue<String> queue2 = func.apply(10);

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());

            Assertions.assertSame(func, IntFunctions.ofConcurrentLinkedQueue());
        }

        @Test
        public void testOfPriorityQueue() {
            IntFunction<PriorityQueue<String>> func = IntFunctions.ofPriorityQueue();
            Assertions.assertNotNull(func);

            PriorityQueue<String> queue1 = func.apply(5);
            PriorityQueue<String> queue2 = func.apply(10);

            Assertions.assertNotNull(queue1);
            Assertions.assertNotNull(queue2);
            Assertions.assertNotSame(queue1, queue2);
            Assertions.assertEquals(0, queue1.size());

            Assertions.assertSame(func, IntFunctions.ofPriorityQueue());
        }

        @Test
        public void testOfMap() {
            IntFunction<Map<String, Integer>> func = IntFunctions.ofMap();
            Assertions.assertNotNull(func);

            Map<String, Integer> map1 = func.apply(5);
            Map<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof HashMap);

            Assertions.assertSame(func, IntFunctions.ofMap());
        }

        @Test
        public void testOfLinkedHashMap() {
            IntFunction<Map<String, Integer>> func = IntFunctions.ofLinkedHashMap();
            Assertions.assertNotNull(func);

            Map<String, Integer> map1 = func.apply(5);
            Map<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof LinkedHashMap);

            Assertions.assertSame(func, IntFunctions.ofLinkedHashMap());
        }

        @Test
        public void testOfIdentityHashMap() {
            IntFunction<IdentityHashMap<String, Integer>> func = IntFunctions.ofIdentityHashMap();
            Assertions.assertNotNull(func);

            IdentityHashMap<String, Integer> map1 = func.apply(5);
            IdentityHashMap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(func, IntFunctions.ofIdentityHashMap());
        }

        @Test
        public void testOfSortedMap() {
            IntFunction<SortedMap<String, Integer>> func = IntFunctions.ofSortedMap();
            Assertions.assertNotNull(func);

            SortedMap<String, Integer> map1 = func.apply(5);
            SortedMap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof TreeMap);

            Assertions.assertSame(func, IntFunctions.ofSortedMap());
        }

        @Test
        public void testOfNavigableMap() {
            IntFunction<NavigableMap<String, Integer>> func = IntFunctions.ofNavigableMap();
            Assertions.assertNotNull(func);

            NavigableMap<String, Integer> map1 = func.apply(5);
            NavigableMap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof TreeMap);

            Assertions.assertSame(func, IntFunctions.ofNavigableMap());
        }

        @Test
        public void testOfTreeMap() {
            IntFunction<TreeMap<String, Integer>> func = IntFunctions.ofTreeMap();
            Assertions.assertNotNull(func);

            TreeMap<String, Integer> map1 = func.apply(5);
            TreeMap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(func, IntFunctions.ofTreeMap());
        }

        @Test
        public void testOfConcurrentMap() {
            IntFunction<ConcurrentMap<String, Integer>> func = IntFunctions.ofConcurrentMap();
            Assertions.assertNotNull(func);

            ConcurrentMap<String, Integer> map1 = func.apply(5);
            ConcurrentMap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());
            Assertions.assertTrue(map1 instanceof ConcurrentHashMap);

            Assertions.assertSame(func, IntFunctions.ofConcurrentMap());
        }

        @Test
        public void testOfConcurrentHashMap() {
            IntFunction<ConcurrentHashMap<String, Integer>> func = IntFunctions.ofConcurrentHashMap();
            Assertions.assertNotNull(func);

            ConcurrentHashMap<String, Integer> map1 = func.apply(5);
            ConcurrentHashMap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(func, IntFunctions.ofConcurrentHashMap());
        }

        @Test
        public void testOfBiMap() {
            IntFunction<BiMap<String, Integer>> func = IntFunctions.ofBiMap();
            Assertions.assertNotNull(func);

            BiMap<String, Integer> map1 = func.apply(5);
            BiMap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(func, IntFunctions.ofBiMap());
        }

        @Test
        public void testOfMultiset() {
            IntFunction<Multiset<String>> func = IntFunctions.ofMultiset();
            Assertions.assertNotNull(func);

            Multiset<String> set1 = func.apply(5);
            Multiset<String> set2 = func.apply(10);

            Assertions.assertNotNull(set1);
            Assertions.assertNotNull(set2);
            Assertions.assertNotSame(set1, set2);
            Assertions.assertEquals(0, set1.size());

            Assertions.assertSame(func, IntFunctions.ofMultiset());
        }

        @Test
        public void testOfListMultimap() {
            IntFunction<ListMultimap<String, Integer>> func = IntFunctions.ofListMultimap();
            Assertions.assertNotNull(func);

            ListMultimap<String, Integer> map1 = func.apply(5);
            ListMultimap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(func, IntFunctions.ofListMultimap());
        }

        @Test
        public void testOfSetMultimap() {
            IntFunction<SetMultimap<String, Integer>> func = IntFunctions.ofSetMultimap();
            Assertions.assertNotNull(func);

            SetMultimap<String, Integer> map1 = func.apply(5);
            SetMultimap<String, Integer> map2 = func.apply(10);

            Assertions.assertNotNull(map1);
            Assertions.assertNotNull(map2);
            Assertions.assertNotSame(map1, map2);
            Assertions.assertEquals(0, map1.size());

            Assertions.assertSame(func, IntFunctions.ofSetMultimap());
        }

        @Test
        public void testOfDisposableArray() {
            IntFunction<DisposableObjArray> func = IntFunctions.ofDisposableArray();
            Assertions.assertNotNull(func);

            DisposableObjArray array1 = func.apply(5);
            DisposableObjArray array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertSame(array1, array2);
        }

        @Test
        public void testOfDisposableArrayWithComponentType() {
            IntFunction<DisposableArray<String>> func = IntFunctions.ofDisposableArray(String.class);
            Assertions.assertNotNull(func);

            DisposableArray<String> array1 = func.apply(5);
            DisposableArray<String> array2 = func.apply(10);

            Assertions.assertNotNull(array1);
            Assertions.assertNotNull(array2);
            Assertions.assertSame(array1, array2);
        }

        @Test
        public void testOfCollection() {
            IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(ArrayList.class);
            Assertions.assertNotNull(func);
            Collection<String> coll = func.apply(10);
            Assertions.assertTrue(coll instanceof ArrayList);

            func = IntFunctions.ofCollection(LinkedList.class);
            coll = func.apply(10);
            Assertions.assertTrue(coll instanceof LinkedList);

            func = IntFunctions.ofCollection(HashSet.class);
            coll = func.apply(10);
            Assertions.assertTrue(coll instanceof HashSet);

            func = IntFunctions.ofCollection(Collection.class);
            coll = func.apply(10);
            Assertions.assertTrue(coll instanceof ArrayList);

            IntFunction<? extends Collection<String>> func2 = IntFunctions.ofCollection(ArrayList.class);
            Assertions.assertSame(func2, IntFunctions.ofCollection(ArrayList.class));
        }

        @Test
        public void testOfCollectionWithInvalidType() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                IntFunctions.ofCollection((Class) Map.class);
            });
        }

        @Test
        public void testOfMap_Class() {
            IntFunction<? extends Map<String, Integer>> func = IntFunctions.ofMap(HashMap.class);
            Assertions.assertNotNull(func);
            Map<String, Integer> map = func.apply(10);
            Assertions.assertTrue(map instanceof HashMap);

            func = IntFunctions.ofMap(LinkedHashMap.class);
            map = func.apply(10);
            Assertions.assertTrue(map instanceof LinkedHashMap);

            func = IntFunctions.ofMap(TreeMap.class);
            map = func.apply(10);
            Assertions.assertTrue(map instanceof TreeMap);

            func = IntFunctions.ofMap(Map.class);
            map = func.apply(10);
            Assertions.assertTrue(map instanceof HashMap);

            IntFunction<? extends Map<String, Integer>> func2 = IntFunctions.ofMap(HashMap.class);
            Assertions.assertSame(func2, IntFunctions.ofMap(HashMap.class));
        }

        @Test
        public void testOfMapWithInvalidType() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                IntFunctions.ofMap((Class) List.class);
            });
        }

        @Test
        public void testRegisterForCollection() {
            class CustomCollection<T> extends ArrayList<T> {
                public CustomCollection(int initialCapacity) {
                    super(initialCapacity);
                }
            }

            java.util.function.IntFunction<CustomCollection> customCreator = CustomCollection::new;
            Assertions.assertThrows(IllegalArgumentException.class, () -> IntFunctions.registerForCollection(CustomCollection.class, customCreator));

        }

        @Test
        public void testRegisterForCollectionWithBuiltinClass() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                IntFunctions.registerForCollection(ArrayList.class, ArrayList::new);
            });
        }

        @Test
        public void testRegisterForMap() {
            class CustomMap<K, V> extends HashMap<K, V> {
                public CustomMap(int initialCapacity) {
                    super(initialCapacity);
                }
            }

            java.util.function.IntFunction<CustomMap> customCreator = CustomMap::new;

            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                IntFunctions.registerForMap(CustomMap.class, customCreator);
            });
        }

        @Test
        public void testRegisterForMapWithBuiltinClass() {
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                IntFunctions.registerForMap(HashMap.class, HashMap::new);
            });
        }

        @Test
        public void testOfImmutableList() {
            Assertions.assertThrows(UnsupportedOperationException.class, () -> {
                IntFunctions.ofImmutableList();
            });
        }

        @Test
        public void testOfImmutableSet() {
            Assertions.assertThrows(UnsupportedOperationException.class, () -> {
                IntFunctions.ofImmutableSet();
            });
        }

        @Test
        public void testOfImmutableMap() {
            Assertions.assertThrows(UnsupportedOperationException.class, () -> {
                IntFunctions.ofImmutableMap();
            });
        }
    }
}
