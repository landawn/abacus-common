package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CommonUtil103Test extends TestBase {

    @Test
    public void testUnmodifiableList() {
        List<String> nullList = null;
        List<String> unmodifiable = N.unmodifiableList(nullList);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        List<String> list = new ArrayList<>();
        list.add("test");
        List<String> unmodifiableList = N.unmodifiableList(list);
        assertNotNull(unmodifiableList);
        assertEquals(1, unmodifiableList.size());
        assertEquals("test", unmodifiableList.get(0));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("new"));
    }

    @Test
    public void testUnmodifiableSet() {
        Set<String> nullSet = null;
        Set<String> unmodifiable = N.unmodifiableSet(nullSet);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        Set<String> set = new HashSet<>();
        set.add("test");
        Set<String> unmodifiableSet = N.unmodifiableSet(set);
        assertNotNull(unmodifiableSet);
        assertEquals(1, unmodifiableSet.size());
        assertTrue(unmodifiableSet.contains("test"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.add("new"));
    }

    @Test
    public void testUnmodifiableSortedSet() {
        SortedSet<String> nullSet = null;
        SortedSet<String> unmodifiable = N.unmodifiableSortedSet(nullSet);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        SortedSet<String> set = new TreeSet<>();
        set.add("test1");
        set.add("test2");
        SortedSet<String> unmodifiableSet = N.unmodifiableSortedSet(set);
        assertNotNull(unmodifiableSet);
        assertEquals(2, unmodifiableSet.size());
        assertEquals("test1", unmodifiableSet.first());
        assertEquals("test2", unmodifiableSet.last());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.add("new"));
    }

    @Test
    public void testUnmodifiableNavigableSet() {
        NavigableSet<String> nullSet = null;
        NavigableSet<String> unmodifiable = N.unmodifiableNavigableSet(nullSet);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        NavigableSet<String> set = new TreeSet<>();
        set.add("test1");
        set.add("test2");
        NavigableSet<String> unmodifiableSet = N.unmodifiableNavigableSet(set);
        assertNotNull(unmodifiableSet);
        assertEquals(2, unmodifiableSet.size());
        assertEquals("test1", unmodifiableSet.first());
        assertEquals("test2", unmodifiableSet.last());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.add("new"));
    }

    @Test
    public void testUnmodifiableMap() {
        Map<String, String> nullMap = null;
        Map<String, String> unmodifiable = N.unmodifiableMap(nullMap);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        Map<String, String> unmodifiableMap = N.unmodifiableMap(map);
        assertNotNull(unmodifiableMap);
        assertEquals(1, unmodifiableMap.size());
        assertEquals("value", unmodifiableMap.get("key"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.put("new", "value"));
    }

    @Test
    public void testUnmodifiableSortedMap() {
        SortedMap<String, String> nullMap = null;
        SortedMap<String, String> unmodifiable = N.unmodifiableSortedMap(nullMap);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        SortedMap<String, String> map = new TreeMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        SortedMap<String, String> unmodifiableMap = N.unmodifiableSortedMap(map);
        assertNotNull(unmodifiableMap);
        assertEquals(2, unmodifiableMap.size());
        assertEquals("key1", unmodifiableMap.firstKey());
        assertEquals("key2", unmodifiableMap.lastKey());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.put("new", "value"));
    }

    @Test
    public void testUnmodifiableNavigableMap() {
        NavigableMap<String, String> nullMap = null;
        NavigableMap<String, String> unmodifiable = N.unmodifiableNavigableMap(nullMap);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        NavigableMap<String, String> map = new TreeMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        NavigableMap<String, String> unmodifiableMap = N.unmodifiableNavigableMap(map);
        assertNotNull(unmodifiableMap);
        assertEquals(2, unmodifiableMap.size());
        assertEquals("key1", unmodifiableMap.firstKey());
        assertEquals("key2", unmodifiableMap.lastKey());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.put("new", "value"));
    }

    @Test
    public void testNewProxyInstance_SingleInterface() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("test".equals(method.getName())) {
                    return "proxied";
                }
                return null;
            }
        };

        TestInterface proxy = N.newProxyInstance(TestInterface.class, handler);
        assertNotNull(proxy);
        assertEquals("proxied", proxy.test());
    }

    @Test
    public void testNewProxyInstance_MultipleInterfaces() {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if ("test".equals(method.getName())) {
                    return "test";
                } else if ("run".equals(method.getName())) {
                    // run method
                }
                return null;
            }
        };

        Object proxy = N.newProxyInstance(new Class[] { TestInterface.class, Runnable.class }, handler);
        assertNotNull(proxy);
        assertTrue(proxy instanceof TestInterface);
        assertTrue(proxy instanceof Runnable);
        assertEquals("test", ((TestInterface) proxy).test());
    }

    @Test
    public void testNewInstance_SimpleClass() {
        String str = N.newInstance(String.class);
        assertNotNull(str);
        assertEquals("", str);

        ArrayList<String> list = N.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewInstance_AbstractClass() {
        List<String> list = N.newInstance(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Map<String, String> map = N.newInstance(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> N.newInstance(Number.class));
    }

    @Test
    public void testNewInstance_NestedClass() {
        // Test with static nested class
        StaticNested nested = N.newInstance(StaticNested.class);
        assertNotNull(nested);

        // Test with non-static inner class - this requires special handling
        OuterClass outer = new OuterClass();
        OuterClass.InnerClass inner = N.newInstance(OuterClass.InnerClass.class);
        assertNotNull(inner);
    }

    @Test
    public void testNewBean() {
        TestBean bean = Beans.newBean(TestBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getValue());
    }

    @Test
    public void testNewCollection_NoSize() {
        Collection<String> list = N.newCollection(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Collection<String> set = N.<String> newCollection(Set.class);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> queue = N.newCollection(Queue.class);
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testNewCollection_WithSize() {
        Collection<String> list = N.newCollection(ArrayList.class, 10);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());

        Collection<String> set = N.newCollection(HashSet.class, 10);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> collection = N.newCollection(LinkedList.class, 5);
        assertNotNull(collection);
        assertTrue(collection instanceof LinkedList);
    }

    @Test
    public void testNewMap_NoSize() {
        Map<String, Integer> map = N.newMap(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        Map<String, Integer> hashMap = N.newMap(HashMap.class);
        assertNotNull(hashMap);
        assertTrue(hashMap.isEmpty());

        Map<String, Integer> treeMap = N.newMap(TreeMap.class);
        assertNotNull(treeMap);
        assertTrue(treeMap.isEmpty());
    }

    @Test
    public void testNewMap_WithSize() {
        Map<String, Integer> map = N.newMap(HashMap.class, 10);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        map.put("test", 1);
        assertEquals(1, map.size());

        Map<String, Integer> linkedMap = N.newMap(LinkedHashMap.class, 10);
        assertNotNull(linkedMap);
        assertTrue(linkedMap instanceof LinkedHashMap);
    }

    @Test
    public void testNewArray_SingleDimension() {
        int[] intArray = N.newArray(int.class, 10);
        assertNotNull(intArray);
        assertEquals(10, intArray.length);

        String[] strArray = N.newArray(String.class, 5);
        assertNotNull(strArray);
        assertEquals(5, strArray.length);

        Object[] objArray = N.newArray(Object.class, 0);
        assertNotNull(objArray);
        assertEquals(0, objArray.length);

        assertThrows(NegativeArraySizeException.class, () -> N.newArray(int.class, -1));
    }

    @Test
    public void testNewArray_MultiDimension() {
        int[][] int2D = N.newArray(int.class, 3, 4);
        assertNotNull(int2D);
        assertEquals(3, int2D.length);
        assertEquals(4, int2D[0].length);

        String[][][] str3D = N.newArray(String.class, 2, 3, 4);
        assertNotNull(str3D);
        assertEquals(2, str3D.length);
        assertEquals(3, str3D[0].length);
        assertEquals(4, str3D[0][0].length);

        assertThrows(IllegalArgumentException.class, () -> N.newArray(null, 10));
        assertThrows(NegativeArraySizeException.class, () -> N.newArray(int.class, 5, -1));
    }

    @Test
    public void testNewArrayList_NoArgs() {
        ArrayList<String> list = N.newArrayList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewArrayList_WithCapacity() {
        ArrayList<String> list = N.newArrayList(100);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        // Add elements to test it works
        for (int i = 0; i < 50; i++) {
            list.add("item" + i);
        }
        assertEquals(50, list.size());
    }

    @Test
    public void testNewArrayList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayList<String> list = N.newArrayList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        ArrayList<String> emptyList = N.newArrayList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        ArrayList<String> nullList = N.newArrayList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testNewLinkedList_NoArgs() {
        LinkedList<String> list = N.newLinkedList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewLinkedList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        LinkedList<String> list = N.newLinkedList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.getFirst());
        assertEquals("c", list.getLast());

        LinkedList<String> emptyList = N.newLinkedList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        LinkedList<String> nullList = N.newLinkedList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testNewHashSet_NoArgs() {
        Set<String> set = N.newHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCapacity() {
        Set<String> set = N.newHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c", "a");
        Set<String> set = N.newHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size()); // "a" appears only once
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> emptySet = N.newHashSet(new ArrayList<>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        Set<String> nullSet = N.newHashSet(null);
        assertNotNull(nullSet);
        assertTrue(nullSet.isEmpty());
    }

    @Test
    public void testNewLinkedHashSet_NoArgs() {
        Set<String> set = N.newLinkedHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewLinkedHashSet_WithCapacity() {
        Set<String> set = N.newLinkedHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewLinkedHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = N.newLinkedHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set instanceof LinkedHashSet);

        // Check order is preserved
        Iterator<String> iter = set.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewTreeSet_NoArgs() {
        TreeSet<String> set = N.newTreeSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("b");
        set.add("a");
        set.add("c");
        assertEquals("a", set.first());
        assertEquals("c", set.last());
    }

    @Test
    public void testNewTreeSet_WithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        TreeSet<String> set = N.newTreeSet(reverseComparator);
        assertNotNull(set);
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("c", set.first());
        assertEquals("a", set.last());
    }

    @Test
    public void testNewTreeSet_WithCollection() {
        List<String> source = Arrays.asList("b", "a", "c");
        TreeSet<String> set = N.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        TreeSet<String> emptySet = N.newTreeSet(new ArrayList<String>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNewTreeSet_WithSortedSet() {
        SortedSet<String> source = new TreeSet<>();
        source.add("b");
        source.add("a");
        source.add("c");

        TreeSet<String> set = N.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        assertTrue(N.newTreeSet((SortedSet<String>) null).isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_NoArgs() {
        Set<String> set = N.newConcurrentHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewConcurrentHashSet_WithCapacity() {
        Set<String> set = N.newConcurrentHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = N.newConcurrentHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testNewSetFromMap() {
        Map<String, Boolean> map = new HashMap<>();
        Set<String> set = N.newSetFromMap(map);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        set.add("test");
        assertTrue(set.contains("test"));
        assertEquals(Boolean.TRUE, map.get("test"));
    }

    @Test
    public void testNewMultiset_NoArgs() {
        Multiset<String> multiset = N.newMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCapacity() {
        Multiset<String> multiset = N.newMultiset(100);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapType() {
        Multiset<String> multiset = N.newMultiset(LinkedHashMap.class);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapSupplier() {
        Multiset<String> multiset = N.newMultiset(() -> new TreeMap<String, MutableInt>());
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> multiset = N.newMultiset(source);
        assertNotNull(multiset);
        assertEquals(6, multiset.size());
        assertEquals(3, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testNewArrayDeque_NoArgs() {
        ArrayDeque<String> deque = N.newArrayDeque();
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCapacity() {
        ArrayDeque<String> deque = N.newArrayDeque(100);
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayDeque<String> deque = N.newArrayDeque(source);
        assertNotNull(deque);
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = N.newEntry("key", 123);
        assertNotNull(entry);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());

        // Test setValue
        entry.setValue(456);
        assertEquals(456, entry.getValue());
    }

    @Test
    public void testNewImmutableEntry() {
        ImmutableEntry<String, Integer> entry = N.newImmutableEntry("key", 123);
        assertNotNull(entry);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());

        // ImmutableEntry should not allow setValue
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(456));
    }

    @Test
    public void testNewHashMap_NoArgs() {
        Map<String, Integer> map = N.newHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testNewHashMap_WithCapacity() {
        Map<String, Integer> map = N.newHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        Map<String, Integer> map = N.newHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());

        Map<String, Integer> emptyMap = N.newHashMap(new HashMap<>());
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNewHashMap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2), createBean("bean3", 3));

        Map<String, TestBean> map = N.newHashMap(beans, TestBean::getName);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals(1, map.get("bean1").getValue());
        assertEquals(2, map.get("bean2").getValue());
        assertEquals(3, map.get("bean3").getValue());

        Map<String, TestBean> emptyMap = N.newHashMap(new ArrayList<TestBean>(), TestBean::getName);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNewLinkedHashMap_NoArgs() {
        Map<String, Integer> map = N.newLinkedHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testNewLinkedHashMap_WithCapacity() {
        Map<String, Integer> map = N.newLinkedHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testNewLinkedHashMap_WithMap() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);

        Map<String, Integer> map = N.newLinkedHashMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());

        // Check order is preserved
        Iterator<String> iter = map.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewLinkedHashMap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2), createBean("bean3", 3));

        Map<String, TestBean> map = N.newLinkedHashMap(beans, TestBean::getName);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertTrue(map instanceof LinkedHashMap);

        // Check order is preserved
        Iterator<String> iter = map.keySet().iterator();
        assertEquals("bean1", iter.next());
        assertEquals("bean2", iter.next());
        assertEquals("bean3", iter.next());
    }

    @Test
    public void testNewTreeMap_NoArgs() {
        TreeMap<String, Integer> map = N.newTreeMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());

        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());
    }

    @Test
    public void testNewTreeMap_WithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        TreeMap<String, Integer> map = N.newTreeMap(reverseComparator);
        assertNotNull(map);

        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("c", map.firstKey());
        assertEquals("a", map.lastKey());
    }

    @Test
    public void testNewTreeMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        TreeMap<String, Integer> map = N.newTreeMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());
    }

    @Test
    public void testNewTreeMap_WithSortedMap() {
        Map<String, Integer> source = new TreeMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        TreeMap<String, Integer> map = N.newTreeMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());

        assertTrue(N.newTreeMap((SortedMap<String, Integer>) null).isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_NoArgs() {
        IdentityHashMap<String, Integer> map = N.newIdentityHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithCapacity() {
        IdentityHashMap<String, Integer> map = N.newIdentityHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        IdentityHashMap<String, Integer> map = N.newIdentityHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
    }

    @Test
    public void testNewConcurrentHashMap_NoArgs() {
        ConcurrentHashMap<String, Integer> map = N.newConcurrentHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithCapacity() {
        ConcurrentHashMap<String, Integer> map = N.newConcurrentHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ConcurrentHashMap<String, Integer> map = N.newConcurrentHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
    }

    @Test
    public void testNewBiMap_NoArgs() {
        BiMap<String, Integer> biMap = N.newBiMap();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacity() {
        BiMap<String, Integer> biMap = N.newBiMap(100);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacityAndLoadFactor() {
        BiMap<String, Integer> biMap = N.newBiMap(100, 0.75f);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapTypes() {
        BiMap<String, Integer> biMap = N.newBiMap(LinkedHashMap.class, TreeMap.class);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapSuppliers() {
        BiMap<String, Integer> biMap = N.newBiMap(() -> new LinkedHashMap<String, Integer>(), () -> new TreeMap<Integer, String>());
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewMultimap() {
        Multimap<String, Integer, List<Integer>> multimap = N.newMultimap(() -> new HashMap<>(), () -> new ArrayList<>());
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = N.newListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMapType() {
        ListMultimap<String, Integer> multimap = N.newListMultimap(LinkedHashMap.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMapAndValueTypes() {
        ListMultimap<String, Integer> multimap = N.newListMultimap(LinkedHashMap.class, LinkedList.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithSuppliers() {
        ListMultimap<String, Integer> multimap = N.newListMultimap(() -> new TreeMap<String, List<Integer>>(), () -> new LinkedList<Integer>());
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("a", 3); // Will overwrite previous "a"

        ListMultimap<String, Integer> multimap = N.newListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.size());
    }

    @Test
    public void testNewListMultimap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 2), createBean("group2", 3));

        ListMultimap<String, TestBean> multimap = N.newListMultimap(beans, TestBean::getName);
        assertNotNull(multimap);
        assertEquals(2, multimap.get("group1").size());
        assertEquals(1, multimap.get("group2").size());
    }

    @Test
    public void testNewListMultimap_WithCollectionAndExtractors() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 2), createBean("group2", 3));

        ListMultimap<String, Integer> multimap = N.newListMultimap(beans, TestBean::getName, TestBean::getValue);
        assertNotNull(multimap);
        assertEquals(Arrays.asList(1, 2), multimap.get("group1"));
        assertEquals(Arrays.asList(3), multimap.get("group2"));
    }

    @Test
    public void testNewLinkedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = N.newLinkedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = N.newLinkedListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ListMultimap<String, Integer> multimap = N.newLinkedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.size());
    }

    @Test
    public void testNewSortedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = N.newSortedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        ListMultimap<String, Integer> multimap = N.newSortedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.size());

        // Keys should be sorted
        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMapType() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap(LinkedHashMap.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMapAndValueTypes() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap(LinkedHashMap.class, LinkedHashSet.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithSuppliers() {
        SetMultimap<String, Integer> multimap = N.newSetMultimap(() -> new TreeMap<String, Set<Integer>>(), () -> new LinkedHashSet<Integer>());
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        SetMultimap<String, Integer> multimap = N.newSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.size());
    }

    @Test
    public void testNewSetMultimap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 1), // Duplicate value
                createBean("group1", 2), createBean("group2", 3));

        SetMultimap<String, TestBean> multimap = N.newSetMultimap(beans, TestBean::getName);
        assertNotNull(multimap);
        assertEquals(3, multimap.get("group1").size()); // All 3 beans despite same value
        assertEquals(1, multimap.get("group2").size());
    }

    @Test
    public void testNewSetMultimap_WithCollectionAndExtractors() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 1), // Duplicate value
                createBean("group1", 2), createBean("group2", 3));

        SetMultimap<String, Integer> multimap = N.newSetMultimap(beans, TestBean::getName, TestBean::getValue);
        assertNotNull(multimap);
        assertEquals(2, multimap.get("group1").size()); // Only 1 and 2 (no duplicates)
        assertEquals(1, multimap.get("group2").size());
    }

    @Test
    public void testNewLinkedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.size());
    }

    @Test
    public void testNewSortedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = N.newSortedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        SetMultimap<String, Integer> multimap = N.newSortedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.size());

        // Keys should be sorted
        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewEmptyDataset() {
        Dataset ds = N.newEmptyDataset();
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.size());
        assertEquals(0, ds.columnCount());
    }

    @Test
    public void testNewEmptyDataset_WithColumnNames() {
        List<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Dataset ds = N.newEmptyDataset(columnNames);
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.size());
        assertEquals(3, ds.columnCount());
        assertEquals(columnNames, ds.columnNameList());
    }

    @Test
    public void testNewEmptyDataset_WithProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", "value");

        List<String> columnNames = Arrays.asList("col1", "col2");
        Dataset ds = N.newEmptyDataset(columnNames, properties);
        assertNotNull(ds);
        assertEquals(0, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals("value", ds.properties().get("key"));
    }

    @Test
    public void testNewDataset_FromRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "John");
        row1.put("age", 30);
        rows.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Jane");
        row2.put("age", 25);
        rows.add(row2);

        Dataset ds = N.newDataset(rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertTrue(ds.columnNameList().contains("name"));
        assertTrue(ds.columnNameList().contains("age"));
    }

    @Test
    public void testNewDataset_FromRowsWithProperties() {
        List<Map<String, Object>> rows = Arrays.asList(createMap("name", "John", "age", 30));

        Map<String, Object> properties = new HashMap<>();
        properties.put("source", "test");

        Dataset ds = N.newDataset(rows, properties);
        assertNotNull(ds);
        assertEquals(1, ds.size());
        assertEquals("test", ds.properties().get("source"));
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRows() {
        List<String> columnNames = Arrays.asList("name", "age");
        List<Object[]> rows = Arrays.asList(new Object[] { "John", 30 }, new Object[] { "Jane", 25 });

        Dataset ds = N.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(columnNames, ds.columnNameList());
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRowArray() {
        List<String> columnNames = Arrays.asList("name", "age");
        Object[][] rows = new Object[][] { { "John", 30 }, { "Jane", 25 } };

        Dataset ds = N.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
    }

    @Test
    public void testNewDataset_FromKeyValueMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("John", 30);
        map.put("Jane", 25);

        Dataset ds = N.newDataset("Name", "Age", map);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(Arrays.asList("Name", "Age"), ds.columnNameList());
    }

    @Test
    public void testNewDataset_FromMapOfCollections() {
        Map<String, List<Object>> map = new LinkedHashMap<>();
        map.put("name", Arrays.asList("John", "Jane"));
        map.put("age", Arrays.asList(30, 25));

        Dataset ds = N.newDataset(map);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(Arrays.asList("name", "age"), ds.columnNameList());
    }

    @Test
    public void testNewDataset_SingleColumn() {
        List<String> values = Arrays.asList("A", "B", "C");
        Dataset ds = N.newDataset("Letter", values);
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(1, ds.columnCount());
        assertEquals("Letter", ds.columnNameList().get(0));
    }

    @Test
    public void testMerge_TwoDatasets() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }, new Object[] { "B" }));

        Dataset ds2 = N.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }, new Object[] { 2 }));

        Dataset merged = N.merge(ds1, ds2);
        assertNotNull(merged);
        assertEquals(4, merged.size()); // 2 + 2 rows
        assertEquals(2, merged.columnCount()); // col1 and col2

        assertThrows(IllegalArgumentException.class, () -> N.merge(null, ds2));
        assertThrows(IllegalArgumentException.class, () -> N.merge(ds1, null));
    }

    @Test
    public void testMerge_ThreeDatasets() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }));
        Dataset ds2 = N.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }));
        Dataset ds3 = N.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true }));

        Dataset merged = N.merge(ds1, ds2, ds3);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> N.merge(null, ds2, ds3));
        assertThrows(IllegalArgumentException.class, () -> N.merge(ds1, null, ds3));
        assertThrows(IllegalArgumentException.class, () -> N.merge(ds1, ds2, (Dataset) null));
    }

    @Test
    public void testMerge_CollectionOfDatasets() {
        List<Dataset> datasets = Arrays.asList(N.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" })),
                N.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 })),
                N.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true })));

        Dataset merged = N.merge(datasets);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        // Test empty collection
        assertThrows(IllegalArgumentException.class, () -> N.merge(new ArrayList<Dataset>()));

        // Test single dataset
        Dataset single = N.merge(Arrays.asList(datasets.get(0)));
        assertNotNull(single);
        assertEquals(1, single.size());

        assertThrows(IllegalArgumentException.class, () -> N.merge((Collection<Dataset>) null));
    }

    @Test
    public void testMerge_WithRequiresSameColumns() {
        Dataset ds1 = N.newDataset(Arrays.asList("col1", "col2"), N.asSingletonList(new Object[] { "A", 1 }));

        Dataset ds2 = N.newDataset(Arrays.asList("col1", "col2"), N.asSingletonList(new Object[] { "B", 2 }));

        Dataset ds3 = N.newDataset(Arrays.asList("col1", "col3"), N.asSingletonList(new Object[] { "C", 3 }));

        // Should work with same columns
        Dataset merged = N.merge(Arrays.asList(ds1, ds2), true);
        assertNotNull(merged);
        assertEquals(2, merged.size());

        // Should fail with different columns
        assertThrows(IllegalArgumentException.class, () -> N.merge(Arrays.asList(ds1, ds3), true));
    }

    @Test
    public void testToArray_Collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Object[] array = N.toArray(list);
        assertArrayEquals(new Object[] { "a", "b", "c" }, array);

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        Object[] setArray = N.toArray(set);
        assertEquals(3, setArray.length);

        // Empty collection
        Object[] emptyArray = N.toArray(new ArrayList<>());
        assertEquals(0, emptyArray.length);

        // Null collection
        Object[] nullArray = N.toArray(null);
        assertEquals(0, nullArray.length);
    }

    @Test
    public void testToArray_CollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object[] array = N.toArray(list, 1, 4);
        assertArrayEquals(new Object[] { "b", "c", "d" }, array);

        // Full range
        Object[] fullArray = N.toArray(list, 0, list.size());
        assertArrayEquals(new Object[] { "a", "b", "c", "d", "e" }, fullArray);

        // Empty range
        Object[] emptyArray = N.toArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toArray(list, 3, 2));
    }

    @Test
    public void testToArray_01() {
        {
            Collection<String> list = N.asLinkedHashSet("a", "b", "c", "d", "e");
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4));
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4, new String[1]));
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4, String[]::new));
            assertArrayEquals(new Object[] { "b", "c", "d" }, N.toArray(list, 1, 4, String[].class));
        }
        {
            Collection<Boolean> c = N.asLinkedList(true, true, false, null, true);
            assertArrayEquals(new boolean[] { true, false, false }, N.toBooleanArray(c, 1, 4, false));
        }
        {
            Collection<Byte> c = N.asLinkedList((byte) 1, (byte) 2, (byte) 3, null, (byte) 4);
            assertArrayEquals(new byte[] { 2, 3, 0 }, N.toByteArray(c, 1, 4, (byte) 0));
        }
        {
            Collection<Short> c = N.asLinkedList((short) 1, (short) 2, (short) 3, null, (short) 4);
            assertArrayEquals(new short[] { 2, 3, 0 }, N.toShortArray(c, 1, 4, (short) 0));
        }
        {
            Collection<Integer> c = N.asLinkedList(1, 2, 3, null, 4);
            assertArrayEquals(new int[] { 2, 3, 0 }, N.toIntArray(c, 1, 4, 0));
        }
        {
            Collection<Long> c = N.asLinkedList(1L, 2L, 3L, null, 4L);
            assertArrayEquals(new long[] { 2L, 3L, 0L }, N.toLongArray(c, 1, 4, 0L));
        }
        {
            Collection<Float> c = N.asLinkedList(1.0f, 2.0f, 3.0f, null, 4.0f);
            assertArrayEquals(new float[] { 2.0f, 3.0f, 0.0f }, N.toFloatArray(c, 1, 4, 0.0f));
        }
        {
            Collection<Double> c = N.asLinkedList(1.0d, 2.0d, 3.0d, null, 4.0d);
            assertArrayEquals(new double[] { 2.0d, 3.0d, 0.0d }, N.toDoubleArray(c, 1, 4, 0.0d));
        }
    }

    @Test
    public void testToArray_WithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c");

        // Target array is large enough
        String[] target = new String[5];
        String[] result = N.toArray(list, target);
        assertSame(target, result);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertNull(result[3]); // Rest should be null

        // Target array is too small
        String[] smallTarget = new String[2];
        String[] newResult = N.toArray(list, smallTarget);
        assertNotSame(smallTarget, newResult);
        assertEquals(3, newResult.length);

        // Empty collection
        String[] emptyResult = N.toArray(new ArrayList<String>(), new String[0]);
        assertEquals(0, emptyResult.length);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        String[] target = new String[3];
        String[] result = N.toArray(list, 1, 4, target);
        assertSame(target, result);
        assertArrayEquals(new String[] { "b", "c", "d" }, result);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToArray_WithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = N.toArray(list, String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        // Empty collection
        String[] emptyArray = N.toArray(new ArrayList<String>(), String[]::new);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToArray_RangeWithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = N.toArray(list, 1, 4, String[]::new);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);
    }

    @Test
    public void testToArray_WithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = N.toArray(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        // Empty collection
        String[] emptyArray = N.toArray(new ArrayList<String>(), String[].class);
        assertEquals(0, emptyArray.length);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = N.toArray(list, 1, 4, String[].class);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);

        assertThrows(IllegalArgumentException.class, () -> N.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToBooleanArray() {
        List<Boolean> list = Arrays.asList(true, false, true, null, false);

        // Default false for null
        boolean[] array = N.toBooleanArray(list);
        assertArrayEquals(new boolean[] { true, false, true, false, false }, array);

        // Custom default for null
        boolean[] arrayWithDefault = N.toBooleanArray(list, true);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, arrayWithDefault);

        // Empty collection
        boolean[] emptyArray = N.toBooleanArray(new ArrayList<Boolean>());
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToBooleanArray_Range() {
        List<Boolean> list = Arrays.asList(true, false, true, null, false);

        boolean[] array = N.toBooleanArray(list, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false }, array);

        boolean[] arrayWithDefault = N.toBooleanArray(list, 1, 4, true);
        assertArrayEquals(new boolean[] { false, true, true }, arrayWithDefault);

        // Empty range
        boolean[] emptyArray = N.toBooleanArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> N.toBooleanArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.toBooleanArray(list, 0, 6));
    }

    @Test
    public void testToBooleanArray_FromByteArray() {
        byte[] bytes = { 0, 1, -1, 127, -128 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, N.toBooleanArray(bytes));

        // Empty array
        assertArrayEquals(new boolean[0], N.toBooleanArray(new byte[0]));
        assertArrayEquals(new boolean[0], N.toBooleanArray((byte[]) null));
    }

    @Test
    public void testToBooleanArray_FromIntArray() {
        int[] ints = { 0, 1, -1, 100, -100 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, N.toBooleanArray(ints));

        // Empty array
        assertArrayEquals(new boolean[0], N.toBooleanArray(new int[0]));
        assertArrayEquals(new boolean[0], N.toBooleanArray((int[]) null));
    }

    @Test
    public void testToCharArray() {
        List<Character> list = Arrays.asList('a', 'b', 'c', null, 'd');

        // Default '\0' for null
        char[] array = N.toCharArray(list);
        assertArrayEquals(new char[] { 'a', 'b', 'c', '\0', 'd' }, array);

        // Custom default for null
        char[] arrayWithDefault = N.toCharArray(list, 'X');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'X', 'd' }, arrayWithDefault);
    }

    @Test
    public void testToCharArray_Range() {
        List<Character> list = Arrays.asList('a', 'b', 'c', null, 'd');

        char[] array = N.toCharArray(list, 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', '\0' }, array);

        char[] arrayWithDefault = N.toCharArray(list, 1, 4, 'X');
        assertArrayEquals(new char[] { 'b', 'c', 'X' }, arrayWithDefault);
    }

    @Test
    public void testToByteArray() {
        List<Number> list = Arrays.asList((byte) 1, 2, 3L, null, 5.0);

        // Default 0 for null
        byte[] array = N.toByteArray(list);
        assertArrayEquals(new byte[] { 1, 2, 3, 0, 5 }, array);

        // Custom default for null
        byte[] arrayWithDefault = N.toByteArray(list, (byte) -1);
        assertArrayEquals(new byte[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToByteArray_Range() {
        List<Number> list = Arrays.asList((byte) 1, 2, 3L, null, 5.0);

        byte[] array = N.toByteArray(list, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 0 }, array);

        byte[] arrayWithDefault = N.toByteArray(list, 1, 4, (byte) -1);
        assertArrayEquals(new byte[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToByteArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        byte[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, N.toByteArray(bools));

        // Empty array
        assertArrayEquals(new byte[0], N.toByteArray(new boolean[0]));
        assertArrayEquals(new byte[0], N.toByteArray((boolean[]) null));
    }

    @Test
    public void testToShortArray() {
        List<Number> list = Arrays.asList((short) 1, 2, 3L, null, 5.0);

        // Default 0 for null
        short[] array = N.toShortArray(list);
        assertArrayEquals(new short[] { 1, 2, 3, 0, 5 }, array);

        // Custom default for null
        short[] arrayWithDefault = N.toShortArray(list, (short) -1);
        assertArrayEquals(new short[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToShortArray_Range() {
        List<Number> list = Arrays.asList((short) 1, 2, 3L, null, 5.0);

        short[] array = N.toShortArray(list, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 0 }, array);

        short[] arrayWithDefault = N.toShortArray(list, 1, 4, (short) -1);
        assertArrayEquals(new short[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        // Default 0 for null
        int[] array = N.toIntArray(list);
        assertArrayEquals(new int[] { 1, 2, 3, 0, 5 }, array);

        // Custom default for null
        int[] arrayWithDefault = N.toIntArray(list, -1);
        assertArrayEquals(new int[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        int[] array = N.toIntArray(list, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 0 }, array);

        int[] arrayWithDefault = N.toIntArray(list, 1, 4, -1);
        assertArrayEquals(new int[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray_FromCharArray() {
        char[] chars = { 'A', 'B', 'C' };
        int[] expected = { 65, 66, 67 };
        assertArrayEquals(expected, N.toIntArray(chars));

        // Empty array
        assertArrayEquals(new int[0], N.toIntArray(new char[0]));
        assertArrayEquals(new int[0], N.toIntArray((char[]) null));
    }

    @Test
    public void testToIntArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        int[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, N.toIntArray(bools));

        // Empty array
        assertArrayEquals(new int[0], N.toIntArray(new boolean[0]));
        assertArrayEquals(new int[0], N.toIntArray((boolean[]) null));
    }

    @Test
    public void testToLongArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        // Default 0 for null
        long[] array = N.toLongArray(list);
        assertArrayEquals(new long[] { 1, 2, 3, 0, 5 }, array);

        // Custom default for null
        long[] arrayWithDefault = N.toLongArray(list, -1L);
        assertArrayEquals(new long[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToLongArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        long[] array = N.toLongArray(list, 1, 4);
        assertArrayEquals(new long[] { 2, 3, 0 }, array);

        long[] arrayWithDefault = N.toLongArray(list, 1, 4, -1L);
        assertArrayEquals(new long[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToFloatArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        // Default 0 for null
        float[] array = N.toFloatArray(list);
        assertArrayEquals(new float[] { 1, 2, 3, 0, 5 }, array, 0.0f);

        // Custom default for null
        float[] arrayWithDefault = N.toFloatArray(list, -1.0f);
        assertArrayEquals(new float[] { 1, 2, 3, -1, 5 }, arrayWithDefault, 0.0f);
    }

    @Test
    public void testToFloatArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        float[] array = N.toFloatArray(list, 1, 4);
        assertArrayEquals(new float[] { 2, 3, 0 }, array, 0.0f);

        float[] arrayWithDefault = N.toFloatArray(list, 1, 4, -1.0f);
        assertArrayEquals(new float[] { 2, 3, -1 }, arrayWithDefault, 0.0f);
    }

    @Test
    public void testToDoubleArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        // Default 0 for null
        double[] array = N.toDoubleArray(list);
        assertArrayEquals(new double[] { 1, 2, 3, 0, 5 }, array, 0.0);

        // Custom default for null
        double[] arrayWithDefault = N.toDoubleArray(list, -1.0);
        assertArrayEquals(new double[] { 1, 2, 3, -1, 5 }, arrayWithDefault, 0.0);
    }

    @Test
    public void testToDoubleArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        double[] array = N.toDoubleArray(list, 1, 4);
        assertArrayEquals(new double[] { 2, 3, 0 }, array, 0.0);

        double[] arrayWithDefault = N.toDoubleArray(list, 1, 4, -1.0);
        assertArrayEquals(new double[] { 2, 3, -1 }, arrayWithDefault, 0.0);
    }

    // Helper methods
    private static TestBean createBean(String name, int value) {
        TestBean bean = new TestBean();
        bean.setName(name);
        bean.setValue(value);
        return bean;
    }

    private static Map<String, Object> createMap(String key1, Object value1, String key2, Object value2) {
        Map<String, Object> map = new HashMap<>();
        map.put(key1, value1);
        map.put(key2, value2);
        return map;
    }

    // Test interfaces and classes
    interface TestInterface {
        String test();
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

    public static class OuterClass {
        public class InnerClass {
            public InnerClass() {
            }
        }

        public OuterClass() {
        }
    }

    public static class StaticNested {
        public StaticNested() {
        }
    }
}
