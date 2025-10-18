package com.landawn.abacus.util;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.AbstractQueue;
import java.util.AbstractSet;
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
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;

@Tag("new-test")
public class IntFunctions100Test extends TestBase {

    @Test
    public void testOf() {
        IntFunction<String> result = IntFunctions.of(size -> "Size: " + size);
        Assertions.assertEquals("Size: 10", result.apply(10));
    }

    @Test
    public void testOfBooleanArray() {
        IntFunction<boolean[]> func = IntFunctions.ofBooleanArray();
        Assertions.assertNotNull(func);

        boolean[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (boolean b : array1) {
            Assertions.assertFalse(b);
        }

        boolean[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);

        boolean[] array3 = func.apply(100);
        Assertions.assertEquals(100, array3.length);
    }

    @Test
    public void testOfCharArray() {
        IntFunction<char[]> func = IntFunctions.ofCharArray();
        Assertions.assertNotNull(func);

        char[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (char c : array1) {
            Assertions.assertEquals('\u0000', c);
        }

        char[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfByteArray() {
        IntFunction<byte[]> func = IntFunctions.ofByteArray();
        Assertions.assertNotNull(func);

        byte[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (byte b : array1) {
            Assertions.assertEquals(0, b);
        }

        byte[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfShortArray() {
        IntFunction<short[]> func = IntFunctions.ofShortArray();
        Assertions.assertNotNull(func);

        short[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (short s : array1) {
            Assertions.assertEquals(0, s);
        }

        short[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfIntArray() {
        IntFunction<int[]> func = IntFunctions.ofIntArray();
        Assertions.assertNotNull(func);

        int[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (int i : array1) {
            Assertions.assertEquals(0, i);
        }

        int[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfLongArray() {
        IntFunction<long[]> func = IntFunctions.ofLongArray();
        Assertions.assertNotNull(func);

        long[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (long l : array1) {
            Assertions.assertEquals(0L, l);
        }

        long[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfFloatArray() {
        IntFunction<float[]> func = IntFunctions.ofFloatArray();
        Assertions.assertNotNull(func);

        float[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (float f : array1) {
            Assertions.assertEquals(0.0f, f);
        }

        float[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfDoubleArray() {
        IntFunction<double[]> func = IntFunctions.ofDoubleArray();
        Assertions.assertNotNull(func);

        double[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (double d : array1) {
            Assertions.assertEquals(0.0, d);
        }

        double[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfStringArray() {
        IntFunction<String[]> func = IntFunctions.ofStringArray();
        Assertions.assertNotNull(func);

        String[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (String s : array1) {
            Assertions.assertNull(s);
        }

        String[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfObjectArray() {
        IntFunction<Object[]> func = IntFunctions.ofObjectArray();
        Assertions.assertNotNull(func);

        Object[] array1 = func.apply(5);
        Assertions.assertEquals(5, array1.length);
        for (Object o : array1) {
            Assertions.assertNull(o);
        }

        Object[] array2 = func.apply(0);
        Assertions.assertEquals(0, array2.length);
    }

    @Test
    public void testOfBooleanList() {
        IntFunction<BooleanList> func = IntFunctions.ofBooleanList();
        Assertions.assertNotNull(func);

        BooleanList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        BooleanList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfCharList() {
        IntFunction<CharList> func = IntFunctions.ofCharList();
        Assertions.assertNotNull(func);

        CharList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        CharList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfByteList() {
        IntFunction<ByteList> func = IntFunctions.ofByteList();
        Assertions.assertNotNull(func);

        ByteList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        ByteList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfShortList() {
        IntFunction<ShortList> func = IntFunctions.ofShortList();
        Assertions.assertNotNull(func);

        ShortList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        ShortList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfIntList() {
        IntFunction<IntList> func = IntFunctions.ofIntList();
        Assertions.assertNotNull(func);

        IntList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        IntList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfLongList() {
        IntFunction<LongList> func = IntFunctions.ofLongList();
        Assertions.assertNotNull(func);

        LongList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        LongList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfFloatList() {
        IntFunction<FloatList> func = IntFunctions.ofFloatList();
        Assertions.assertNotNull(func);

        FloatList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        FloatList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfDoubleList() {
        IntFunction<DoubleList> func = IntFunctions.ofDoubleList();
        Assertions.assertNotNull(func);

        DoubleList list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertEquals(0, list1.size());

        DoubleList list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfList() {
        IntFunction<List<String>> func = IntFunctions.ofList();
        Assertions.assertNotNull(func);

        List<String> list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertTrue(list1 instanceof ArrayList);
        Assertions.assertEquals(0, list1.size());

        List<String> list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfLinkedList() {
        IntFunction<LinkedList<String>> func = IntFunctions.ofLinkedList();
        Assertions.assertNotNull(func);

        LinkedList<String> list1 = func.apply(10);
        Assertions.assertNotNull(list1);
        Assertions.assertTrue(list1 instanceof LinkedList);
        Assertions.assertEquals(0, list1.size());

        LinkedList<String> list2 = func.apply(0);
        Assertions.assertNotNull(list2);
        Assertions.assertEquals(0, list2.size());
    }

    @Test
    public void testOfSet() {
        IntFunction<Set<String>> func = IntFunctions.ofSet();
        Assertions.assertNotNull(func);

        Set<String> set1 = func.apply(10);
        Assertions.assertNotNull(set1);
        Assertions.assertTrue(set1 instanceof HashSet);
        Assertions.assertEquals(0, set1.size());

        Set<String> set2 = func.apply(0);
        Assertions.assertNotNull(set2);
        Assertions.assertEquals(0, set2.size());
    }

    @Test
    public void testOfLinkedHashSet() {
        IntFunction<Set<String>> func = IntFunctions.ofLinkedHashSet();
        Assertions.assertNotNull(func);

        Set<String> set1 = func.apply(10);
        Assertions.assertNotNull(set1);
        Assertions.assertTrue(set1 instanceof LinkedHashSet);
        Assertions.assertEquals(0, set1.size());

        Set<String> set2 = func.apply(0);
        Assertions.assertNotNull(set2);
        Assertions.assertEquals(0, set2.size());
    }

    @Test
    public void testOfSortedSet() {
        IntFunction<SortedSet<String>> func = IntFunctions.ofSortedSet();
        Assertions.assertNotNull(func);

        SortedSet<String> set1 = func.apply(10);
        Assertions.assertNotNull(set1);
        Assertions.assertTrue(set1 instanceof TreeSet);
        Assertions.assertEquals(0, set1.size());

        SortedSet<String> set2 = func.apply(0);
        Assertions.assertNotNull(set2);
        Assertions.assertEquals(0, set2.size());
    }

    @Test
    public void testOfNavigableSet() {
        IntFunction<NavigableSet<String>> func = IntFunctions.ofNavigableSet();
        Assertions.assertNotNull(func);

        NavigableSet<String> set1 = func.apply(10);
        Assertions.assertNotNull(set1);
        Assertions.assertTrue(set1 instanceof TreeSet);
        Assertions.assertEquals(0, set1.size());

        NavigableSet<String> set2 = func.apply(0);
        Assertions.assertNotNull(set2);
        Assertions.assertEquals(0, set2.size());
    }

    @Test
    public void testOfTreeSet() {
        IntFunction<TreeSet<String>> func = IntFunctions.ofTreeSet();
        Assertions.assertNotNull(func);

        TreeSet<String> set1 = func.apply(10);
        Assertions.assertNotNull(set1);
        Assertions.assertEquals(0, set1.size());

        TreeSet<String> set2 = func.apply(0);
        Assertions.assertNotNull(set2);
        Assertions.assertEquals(0, set2.size());
    }

    @Test
    public void testOfQueue() {
        IntFunction<Queue<String>> func = IntFunctions.ofQueue();
        Assertions.assertNotNull(func);

        Queue<String> queue1 = func.apply(10);
        Assertions.assertNotNull(queue1);
        Assertions.assertTrue(queue1 instanceof LinkedList);
        Assertions.assertEquals(0, queue1.size());

        Queue<String> queue2 = func.apply(0);
        Assertions.assertNotNull(queue2);
        Assertions.assertEquals(0, queue2.size());
    }

    @Test
    public void testOfDeque() {
        IntFunction<Deque<String>> func = IntFunctions.ofDeque();
        Assertions.assertNotNull(func);

        Deque<String> deque1 = func.apply(10);
        Assertions.assertNotNull(deque1);
        Assertions.assertTrue(deque1 instanceof LinkedList);
        Assertions.assertEquals(0, deque1.size());

        Deque<String> deque2 = func.apply(0);
        Assertions.assertNotNull(deque2);
        Assertions.assertEquals(0, deque2.size());
    }

    @Test
    public void testOfArrayDeque() {
        IntFunction<ArrayDeque<String>> func = IntFunctions.ofArrayDeque();
        Assertions.assertNotNull(func);

        ArrayDeque<String> deque1 = func.apply(10);
        Assertions.assertNotNull(deque1);
        Assertions.assertEquals(0, deque1.size());

        ArrayDeque<String> deque2 = func.apply(0);
        Assertions.assertNotNull(deque2);
        Assertions.assertEquals(0, deque2.size());
    }

    @Test
    public void testOfLinkedBlockingQueue() {
        IntFunction<LinkedBlockingQueue<String>> func = IntFunctions.ofLinkedBlockingQueue();
        Assertions.assertNotNull(func);

        LinkedBlockingQueue<String> queue1 = func.apply(10);
        Assertions.assertNotNull(queue1);
        Assertions.assertEquals(0, queue1.size());
        Assertions.assertEquals(10, queue1.remainingCapacity());

        LinkedBlockingQueue<String> queue2 = func.apply(100);
        Assertions.assertNotNull(queue2);
        Assertions.assertEquals(0, queue2.size());
        Assertions.assertEquals(100, queue2.remainingCapacity());
    }

    @Test
    public void testOfArrayBlockingQueue() {
        IntFunction<ArrayBlockingQueue<String>> func = IntFunctions.ofArrayBlockingQueue();
        Assertions.assertNotNull(func);

        ArrayBlockingQueue<String> queue1 = func.apply(10);
        Assertions.assertNotNull(queue1);
        Assertions.assertEquals(0, queue1.size());
        Assertions.assertEquals(10, queue1.remainingCapacity());

        ArrayBlockingQueue<String> queue2 = func.apply(100);
        Assertions.assertNotNull(queue2);
        Assertions.assertEquals(0, queue2.size());
        Assertions.assertEquals(100, queue2.remainingCapacity());
    }

    @Test
    public void testOfLinkedBlockingDeque() {
        IntFunction<LinkedBlockingDeque<String>> func = IntFunctions.ofLinkedBlockingDeque();
        Assertions.assertNotNull(func);

        LinkedBlockingDeque<String> deque1 = func.apply(10);
        Assertions.assertNotNull(deque1);
        Assertions.assertEquals(0, deque1.size());
        Assertions.assertEquals(10, deque1.remainingCapacity());

        LinkedBlockingDeque<String> deque2 = func.apply(100);
        Assertions.assertNotNull(deque2);
        Assertions.assertEquals(0, deque2.size());
        Assertions.assertEquals(100, deque2.remainingCapacity());
    }

    @Test
    public void testOfConcurrentLinkedQueue() {
        IntFunction<ConcurrentLinkedQueue<String>> func = IntFunctions.ofConcurrentLinkedQueue();
        Assertions.assertNotNull(func);

        ConcurrentLinkedQueue<String> queue1 = func.apply(10);
        Assertions.assertNotNull(queue1);
        Assertions.assertEquals(0, queue1.size());

        ConcurrentLinkedQueue<String> queue2 = func.apply(0);
        Assertions.assertNotNull(queue2);
        Assertions.assertEquals(0, queue2.size());
    }

    @Test
    public void testOfPriorityQueue() {
        IntFunction<PriorityQueue<String>> func = IntFunctions.ofPriorityQueue();
        Assertions.assertNotNull(func);

        PriorityQueue<String> queue1 = func.apply(10);
        Assertions.assertNotNull(queue1);
        Assertions.assertEquals(0, queue1.size());

        PriorityQueue<String> queue2 = func.apply(1);
        Assertions.assertNotNull(queue2);
        Assertions.assertEquals(0, queue2.size());
    }

    @Test
    public void testOfMap() {
        IntFunction<Map<String, Integer>> func = IntFunctions.ofMap();
        Assertions.assertNotNull(func);

        Map<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertTrue(map1 instanceof HashMap);
        Assertions.assertEquals(0, map1.size());

        Map<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfLinkedHashMap() {
        IntFunction<Map<String, Integer>> func = IntFunctions.ofLinkedHashMap();
        Assertions.assertNotNull(func);

        Map<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertTrue(map1 instanceof LinkedHashMap);
        Assertions.assertEquals(0, map1.size());

        Map<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfIdentityHashMap() {
        IntFunction<IdentityHashMap<String, Integer>> func = IntFunctions.ofIdentityHashMap();
        Assertions.assertNotNull(func);

        IdentityHashMap<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertEquals(0, map1.size());

        IdentityHashMap<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfSortedMap() {
        IntFunction<SortedMap<String, Integer>> func = IntFunctions.ofSortedMap();
        Assertions.assertNotNull(func);

        SortedMap<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertTrue(map1 instanceof TreeMap);
        Assertions.assertEquals(0, map1.size());

        SortedMap<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfNavigableMap() {
        IntFunction<NavigableMap<String, Integer>> func = IntFunctions.ofNavigableMap();
        Assertions.assertNotNull(func);

        NavigableMap<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertTrue(map1 instanceof TreeMap);
        Assertions.assertEquals(0, map1.size());

        NavigableMap<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfTreeMap() {
        IntFunction<TreeMap<String, Integer>> func = IntFunctions.ofTreeMap();
        Assertions.assertNotNull(func);

        TreeMap<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertEquals(0, map1.size());

        TreeMap<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfConcurrentMap() {
        IntFunction<ConcurrentMap<String, Integer>> func = IntFunctions.ofConcurrentMap();
        Assertions.assertNotNull(func);

        ConcurrentMap<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertTrue(map1 instanceof ConcurrentHashMap);
        Assertions.assertEquals(0, map1.size());

        ConcurrentMap<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfConcurrentHashMap() {
        IntFunction<ConcurrentHashMap<String, Integer>> func = IntFunctions.ofConcurrentHashMap();
        Assertions.assertNotNull(func);

        ConcurrentHashMap<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertEquals(0, map1.size());

        ConcurrentHashMap<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfBiMap() {
        IntFunction<BiMap<String, Integer>> func = IntFunctions.ofBiMap();
        Assertions.assertNotNull(func);

        BiMap<String, Integer> map1 = func.apply(10);
        Assertions.assertNotNull(map1);
        Assertions.assertEquals(0, map1.size());

        BiMap<String, Integer> map2 = func.apply(0);
        Assertions.assertNotNull(map2);
        Assertions.assertEquals(0, map2.size());
    }

    @Test
    public void testOfMultiset() {
        IntFunction<Multiset<String>> func = IntFunctions.ofMultiset();
        Assertions.assertNotNull(func);

        Multiset<String> multiset1 = func.apply(10);
        Assertions.assertNotNull(multiset1);
        Assertions.assertEquals(0, multiset1.size());

        Multiset<String> multiset2 = func.apply(0);
        Assertions.assertNotNull(multiset2);
        Assertions.assertEquals(0, multiset2.size());
    }

    @Test
    public void testOfListMultimap() {
        IntFunction<ListMultimap<String, Integer>> func = IntFunctions.ofListMultimap();
        Assertions.assertNotNull(func);

        ListMultimap<String, Integer> multimap1 = func.apply(10);
        Assertions.assertNotNull(multimap1);
        Assertions.assertEquals(0, multimap1.size());

        ListMultimap<String, Integer> multimap2 = func.apply(0);
        Assertions.assertNotNull(multimap2);
        Assertions.assertEquals(0, multimap2.size());
    }

    @Test
    public void testOfSetMultimap() {
        IntFunction<SetMultimap<String, Integer>> func = IntFunctions.ofSetMultimap();
        Assertions.assertNotNull(func);

        SetMultimap<String, Integer> multimap1 = func.apply(10);
        Assertions.assertNotNull(multimap1);
        Assertions.assertEquals(0, multimap1.size());

        SetMultimap<String, Integer> multimap2 = func.apply(0);
        Assertions.assertNotNull(multimap2);
        Assertions.assertEquals(0, multimap2.size());
    }

    @Test
    public void testOfDisposableArray() {
        IntFunction<DisposableObjArray> func = IntFunctions.ofDisposableArray();
        Assertions.assertNotNull(func);

        DisposableObjArray array1 = func.apply(10);
        Assertions.assertNotNull(array1);

        DisposableObjArray array2 = func.apply(20);
        Assertions.assertSame(array1, array2);

        IntFunction<DisposableObjArray> func2 = IntFunctions.ofDisposableArray();
        DisposableObjArray array3 = func2.apply(15);
        Assertions.assertNotSame(array1, array3);
    }

    @Test
    public void testOfDisposableArrayWithComponentType() {
        IntFunction<DisposableArray<String>> func = IntFunctions.ofDisposableArray(String.class);
        Assertions.assertNotNull(func);

        DisposableArray<String> array1 = func.apply(10);
        Assertions.assertNotNull(array1);

        DisposableArray<String> array2 = func.apply(20);
        Assertions.assertSame(array1, array2);

        IntFunction<DisposableArray<String>> func2 = IntFunctions.ofDisposableArray(String.class);
        DisposableArray<String> array3 = func2.apply(15);
        Assertions.assertNotSame(array1, array3);

        IntFunction<DisposableArray<Integer>> intFunc = IntFunctions.ofDisposableArray(Integer.class);
        DisposableArray<Integer> intArray = intFunc.apply(5);
        Assertions.assertNotNull(intArray);
    }

    @Test
    public void testOfCollectionWithArrayList() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(ArrayList.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof ArrayList);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithLinkedList() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(LinkedList.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof LinkedList);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithHashSet() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(HashSet.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof HashSet);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithLinkedHashSet() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(LinkedHashSet.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof LinkedHashSet);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithTreeSet() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(TreeSet.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof TreeSet);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithPriorityQueue() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(PriorityQueue.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof PriorityQueue);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithArrayDeque() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(ArrayDeque.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof ArrayDeque);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithConcurrentLinkedQueue() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(ConcurrentLinkedQueue.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof ConcurrentLinkedQueue);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithLinkedBlockingQueue() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(LinkedBlockingQueue.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof LinkedBlockingQueue);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithArrayBlockingQueue() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(ArrayBlockingQueue.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof ArrayBlockingQueue);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithLinkedBlockingDeque() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(LinkedBlockingDeque.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof LinkedBlockingDeque);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithAbstractClasses() {
        IntFunction<? extends Collection<String>> func1 = IntFunctions.ofCollection(Collection.class);
        Collection<String> coll1 = func1.apply(10);
        Assertions.assertTrue(coll1 instanceof ArrayList);

        IntFunction<? extends Collection<String>> func2 = IntFunctions.ofCollection(AbstractCollection.class);
        Collection<String> coll2 = func2.apply(10);
        Assertions.assertTrue(coll2 instanceof ArrayList);

        IntFunction<? extends Collection<String>> func3 = IntFunctions.ofCollection(List.class);
        Collection<String> coll3 = func3.apply(10);
        Assertions.assertTrue(coll3 instanceof ArrayList);

        IntFunction<? extends Collection<String>> func4 = IntFunctions.ofCollection(AbstractList.class);
        Collection<String> coll4 = func4.apply(10);
        Assertions.assertTrue(coll4 instanceof ArrayList);

        IntFunction<? extends Collection<String>> func5 = IntFunctions.ofCollection(Set.class);
        Collection<String> coll5 = func5.apply(10);
        Assertions.assertTrue(coll5 instanceof HashSet);

        IntFunction<? extends Collection<String>> func6 = IntFunctions.ofCollection(AbstractSet.class);
        Collection<String> coll6 = func6.apply(10);
        Assertions.assertTrue(coll6 instanceof HashSet);

        IntFunction<? extends Collection<String>> func7 = IntFunctions.ofCollection(Queue.class);
        Collection<String> coll7 = func7.apply(10);
        Assertions.assertTrue(coll7 instanceof LinkedList);

        IntFunction<? extends Collection<String>> func8 = IntFunctions.ofCollection(AbstractQueue.class);
        Collection<String> coll8 = func8.apply(10);
        Assertions.assertTrue(coll8 instanceof LinkedList);

        IntFunction<? extends Collection<String>> func9 = IntFunctions.ofCollection(Deque.class);
        Collection<String> coll9 = func9.apply(10);
        Assertions.assertTrue(coll9 instanceof LinkedList);

        IntFunction<? extends Collection<String>> func10 = IntFunctions.ofCollection(BlockingQueue.class);
        Collection<String> coll10 = func10.apply(10);
        Assertions.assertTrue(coll10 instanceof LinkedBlockingQueue);

        IntFunction<? extends Collection<String>> func11 = IntFunctions.ofCollection(BlockingDeque.class);
        Collection<String> coll11 = func11.apply(10);
        Assertions.assertTrue(coll11 instanceof LinkedBlockingDeque);
    }

    @Test
    public void testOfCollectionWithSortedSet() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(SortedSet.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof TreeSet);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithNavigableSet() {
        IntFunction<? extends Collection<String>> func = IntFunctions.ofCollection(NavigableSet.class);
        Assertions.assertNotNull(func);

        Collection<String> coll = func.apply(10);
        Assertions.assertNotNull(coll);
        Assertions.assertTrue(coll instanceof TreeSet);
        Assertions.assertEquals(0, coll.size());
    }

    @Test
    public void testOfCollectionWithIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IntFunctions.ofCollection((Class) String.class);
        });

        Assertions.assertThrows(NullPointerException.class, () -> {
            IntFunctions.ofCollection(null);
        });
    }

    @Test
    public void testOfCollectionCaching() {
        IntFunction<? extends Collection<String>> func1 = IntFunctions.ofCollection(ArrayList.class);
        IntFunction<? extends Collection<String>> func2 = IntFunctions.ofCollection(ArrayList.class);
        Assertions.assertSame(func1, func2);

        IntFunction<? extends Collection<String>> func3 = IntFunctions.ofCollection(LinkedList.class);
        Assertions.assertNotSame(func1, func3);
    }

    @Test
    public void testOfMapWithHashMap() {
        IntFunction<? extends Map<String, Integer>> func = IntFunctions.ofMap(HashMap.class);
        Assertions.assertNotNull(func);

        Map<String, Integer> map = func.apply(10);
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof HashMap);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void testOfMapWithLinkedHashMap() {
        IntFunction<? extends Map<String, Integer>> func = IntFunctions.ofMap(LinkedHashMap.class);
        Assertions.assertNotNull(func);

        Map<String, Integer> map = func.apply(10);
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof LinkedHashMap);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void testOfMapWithTreeMap() {
        IntFunction<? extends Map<String, Integer>> func = IntFunctions.ofMap(TreeMap.class);
        Assertions.assertNotNull(func);

        Map<String, Integer> map = func.apply(10);
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof TreeMap);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void testOfMapWithIdentityHashMap() {
        IntFunction<? extends Map<String, Integer>> func = IntFunctions.ofMap(IdentityHashMap.class);
        Assertions.assertNotNull(func);

        Map<String, Integer> map = func.apply(10);
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof IdentityHashMap);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void testOfMapWithConcurrentHashMap() {
        IntFunction<? extends Map<String, Integer>> func = IntFunctions.ofMap(ConcurrentHashMap.class);
        Assertions.assertNotNull(func);

        Map<String, Integer> map = func.apply(10);
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof ConcurrentHashMap);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void testOfMapWithAbstractClasses() {
        IntFunction<? extends Map<String, Integer>> func1 = IntFunctions.ofMap(Map.class);
        Map<String, Integer> map1 = func1.apply(10);
        Assertions.assertTrue(map1 instanceof HashMap);

        IntFunction<? extends Map<String, Integer>> func2 = IntFunctions.ofMap(AbstractMap.class);
        Map<String, Integer> map2 = func2.apply(10);
        Assertions.assertTrue(map2 instanceof HashMap);

        IntFunction<? extends Map<String, Integer>> func3 = IntFunctions.ofMap(SortedMap.class);
        Map<String, Integer> map3 = func3.apply(10);
        Assertions.assertTrue(map3 instanceof TreeMap);

        IntFunction<? extends Map<String, Integer>> func4 = IntFunctions.ofMap(NavigableMap.class);
        Map<String, Integer> map4 = func4.apply(10);
        Assertions.assertTrue(map4 instanceof TreeMap);

        IntFunction<? extends Map<String, Integer>> func5 = IntFunctions.ofMap(ConcurrentMap.class);
        Map<String, Integer> map5 = func5.apply(10);
        Assertions.assertTrue(map5 instanceof ConcurrentHashMap);
    }

    @Test
    public void testOfMapWithBiMap() {
        IntFunction<? extends Map<String, Integer>> func = IntFunctions.ofMap(BiMap.class);
        Assertions.assertNotNull(func);

        Map<String, Integer> map = func.apply(10);
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof BiMap);
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void testOfMapWithIllegalArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            IntFunctions.ofMap((Class) String.class);
        });

        Assertions.assertThrows(NullPointerException.class, () -> {
            IntFunctions.ofMap(null);
        });
    }

    @Test
    public void testOfMapCaching() {
        IntFunction<? extends Map<String, Integer>> func1 = IntFunctions.ofMap(HashMap.class);
        IntFunction<? extends Map<String, Integer>> func2 = IntFunctions.ofMap(HashMap.class);
        Assertions.assertSame(func1, func2);

        IntFunction<? extends Map<String, Integer>> func3 = IntFunctions.ofMap(LinkedHashMap.class);
        Assertions.assertNotSame(func1, func3);
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
