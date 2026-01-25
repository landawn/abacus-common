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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CommonUtil103Test extends TestBase {

    @Test
    public void testUnmodifiableList() {
        List<String> nullList = null;
        List<String> unmodifiable = CommonUtil.unmodifiableList(nullList);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        List<String> list = new ArrayList<>();
        list.add("test");
        List<String> unmodifiableList = CommonUtil.unmodifiableList(list);
        assertNotNull(unmodifiableList);
        assertEquals(1, unmodifiableList.size());
        assertEquals("test", unmodifiableList.get(0));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableList.add("new"));
    }

    @Test
    public void testUnmodifiableSet() {
        Set<String> nullSet = null;
        Set<String> unmodifiable = CommonUtil.unmodifiableSet(nullSet);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        Set<String> set = new HashSet<>();
        set.add("test");
        Set<String> unmodifiableSet = CommonUtil.unmodifiableSet(set);
        assertNotNull(unmodifiableSet);
        assertEquals(1, unmodifiableSet.size());
        assertTrue(unmodifiableSet.contains("test"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.add("new"));
    }

    @Test
    public void testUnmodifiableSortedSet() {
        SortedSet<String> nullSet = null;
        SortedSet<String> unmodifiable = CommonUtil.unmodifiableSortedSet(nullSet);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        SortedSet<String> set = new TreeSet<>();
        set.add("test1");
        set.add("test2");
        SortedSet<String> unmodifiableSet = CommonUtil.unmodifiableSortedSet(set);
        assertNotNull(unmodifiableSet);
        assertEquals(2, unmodifiableSet.size());
        assertEquals("test1", unmodifiableSet.first());
        assertEquals("test2", unmodifiableSet.last());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.add("new"));
    }

    @Test
    public void testUnmodifiableNavigableSet() {
        NavigableSet<String> nullSet = null;
        NavigableSet<String> unmodifiable = CommonUtil.unmodifiableNavigableSet(nullSet);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        NavigableSet<String> set = new TreeSet<>();
        set.add("test1");
        set.add("test2");
        NavigableSet<String> unmodifiableSet = CommonUtil.unmodifiableNavigableSet(set);
        assertNotNull(unmodifiableSet);
        assertEquals(2, unmodifiableSet.size());
        assertEquals("test1", unmodifiableSet.first());
        assertEquals("test2", unmodifiableSet.last());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.add("new"));
    }

    @Test
    public void testUnmodifiableMap() {
        Map<String, String> nullMap = null;
        Map<String, String> unmodifiable = CommonUtil.unmodifiableMap(nullMap);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        Map<String, String> unmodifiableMap = CommonUtil.unmodifiableMap(map);
        assertNotNull(unmodifiableMap);
        assertEquals(1, unmodifiableMap.size());
        assertEquals("value", unmodifiableMap.get("key"));
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.put("new", "value"));
    }

    @Test
    public void testUnmodifiableSortedMap() {
        SortedMap<String, String> nullMap = null;
        SortedMap<String, String> unmodifiable = CommonUtil.unmodifiableSortedMap(nullMap);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        SortedMap<String, String> map = new TreeMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        SortedMap<String, String> unmodifiableMap = CommonUtil.unmodifiableSortedMap(map);
        assertNotNull(unmodifiableMap);
        assertEquals(2, unmodifiableMap.size());
        assertEquals("key1", unmodifiableMap.firstKey());
        assertEquals("key2", unmodifiableMap.lastKey());
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableMap.put("new", "value"));
    }

    @Test
    public void testUnmodifiableNavigableMap() {
        NavigableMap<String, String> nullMap = null;
        NavigableMap<String, String> unmodifiable = CommonUtil.unmodifiableNavigableMap(nullMap);
        assertNotNull(unmodifiable);
        assertTrue(unmodifiable.isEmpty());

        NavigableMap<String, String> map = new TreeMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        NavigableMap<String, String> unmodifiableMap = CommonUtil.unmodifiableNavigableMap(map);
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

        TestInterface proxy = CommonUtil.newProxyInstance(TestInterface.class, handler);
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
                }
                return null;
            }
        };

        Object proxy = CommonUtil.newProxyInstance(new Class[] { TestInterface.class, Runnable.class }, handler);
        assertNotNull(proxy);
        assertTrue(proxy instanceof TestInterface);
        assertTrue(proxy instanceof Runnable);
        assertEquals("test", ((TestInterface) proxy).test());
    }

    @Test
    public void testNewInstance_SimpleClass() {
        String str = CommonUtil.newInstance(String.class);
        assertNotNull(str);
        assertEquals("", str);

        ArrayList<String> list = CommonUtil.newInstance(ArrayList.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testNewInstance_AbstractClass() {
        List<String> list = CommonUtil.newInstance(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Map<String, String> map = CommonUtil.newInstance(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Number.class));
    }

    @Test
    public void testNewInstance_NestedClass() {
        StaticNested nested = CommonUtil.newInstance(StaticNested.class);
        assertNotNull(nested);

        OuterClass outer = new OuterClass();
        OuterClass.InnerClass inner = CommonUtil.newInstance(OuterClass.InnerClass.class);
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
        Collection<String> list = CommonUtil.newCollection(List.class);
        assertNotNull(list);
        assertTrue(list.isEmpty());

        Collection<String> set = CommonUtil.<String> newCollection(Set.class);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> queue = CommonUtil.newCollection(Queue.class);
        assertNotNull(queue);
        assertTrue(queue.isEmpty());
    }

    @Test
    public void testNewCollection_WithSize() {
        Collection<String> list = CommonUtil.newCollection(ArrayList.class, 10);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());

        Collection<String> set = CommonUtil.newCollection(HashSet.class, 10);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        Collection<String> collection = CommonUtil.newCollection(LinkedList.class, 5);
        assertNotNull(collection);
        assertTrue(collection instanceof LinkedList);
    }

    @Test
    public void testNewMap_NoSize() {
        Map<String, Integer> map = CommonUtil.newMap(Map.class);
        assertNotNull(map);
        assertTrue(map.isEmpty());

        Map<String, Integer> hashMap = CommonUtil.newMap(HashMap.class);
        assertNotNull(hashMap);
        assertTrue(hashMap.isEmpty());

        Map<String, Integer> treeMap = CommonUtil.newMap(TreeMap.class);
        assertNotNull(treeMap);
        assertTrue(treeMap.isEmpty());
    }

    @Test
    public void testNewMap_WithSize() {
        Map<String, Integer> map = CommonUtil.newMap(HashMap.class, 10);
        assertNotNull(map);
        assertTrue(map.isEmpty());
        map.put("test", 1);
        assertEquals(1, map.size());

        Map<String, Integer> linkedMap = CommonUtil.newMap(LinkedHashMap.class, 10);
        assertNotNull(linkedMap);
        assertTrue(linkedMap instanceof LinkedHashMap);
    }

    @Test
    public void testNewArray_SingleDimension() {
        int[] intArray = CommonUtil.newArray(int.class, 10);
        assertNotNull(intArray);
        assertEquals(10, intArray.length);

        String[] strArray = CommonUtil.newArray(String.class, 5);
        assertNotNull(strArray);
        assertEquals(5, strArray.length);

        Object[] objArray = CommonUtil.newArray(Object.class, 0);
        assertNotNull(objArray);
        assertEquals(0, objArray.length);

        assertThrows(NegativeArraySizeException.class, () -> CommonUtil.newArray(int.class, -1));
    }

    @Test
    public void testNewArray_MultiDimension() {
        int[][] int2D = CommonUtil.newArray(int.class, 3, 4);
        assertNotNull(int2D);
        assertEquals(3, int2D.length);
        assertEquals(4, int2D[0].length);

        String[][][] str3D = CommonUtil.newArray(String.class, 2, 3, 4);
        assertNotNull(str3D);
        assertEquals(2, str3D.length);
        assertEquals(3, str3D[0].length);
        assertEquals(4, str3D[0][0].length);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newArray(null, 10));
        assertThrows(NegativeArraySizeException.class, () -> CommonUtil.newArray(int.class, 5, -1));
    }

    @Test
    public void testNewArrayList_NoArgs() {
        ArrayList<String> list = CommonUtil.newArrayList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewArrayList_WithCapacity() {
        ArrayList<String> list = CommonUtil.newArrayList(100);
        assertNotNull(list);
        assertTrue(list.isEmpty());
        for (int i = 0; i < 50; i++) {
            list.add("item" + i);
        }
        assertEquals(50, list.size());
    }

    @Test
    public void testNewArrayList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayList<String> list = CommonUtil.newArrayList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));

        ArrayList<String> emptyList = CommonUtil.newArrayList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        ArrayList<String> nullList = CommonUtil.newArrayList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testNewLinkedList_NoArgs() {
        LinkedList<String> list = CommonUtil.newLinkedList();
        assertNotNull(list);
        assertTrue(list.isEmpty());
        list.add("test");
        assertEquals(1, list.size());
    }

    @Test
    public void testNewLinkedList_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        LinkedList<String> list = CommonUtil.newLinkedList(source);
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.getFirst());
        assertEquals("c", list.getLast());

        LinkedList<String> emptyList = CommonUtil.newLinkedList(new ArrayList<>());
        assertNotNull(emptyList);
        assertTrue(emptyList.isEmpty());

        LinkedList<String> nullList = CommonUtil.newLinkedList(null);
        assertNotNull(nullList);
        assertTrue(nullList.isEmpty());
    }

    @Test
    public void testNewHashSet_NoArgs() {
        Set<String> set = CommonUtil.newHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCapacity() {
        Set<String> set = CommonUtil.newHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c", "a");
        Set<String> set = CommonUtil.newHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<String> emptySet = CommonUtil.newHashSet(new ArrayList<>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        Set<String> nullSet = CommonUtil.newHashSet(null);
        assertNotNull(nullSet);
        assertTrue(nullSet.isEmpty());
    }

    @Test
    public void testNewLinkedHashSet_NoArgs() {
        Set<String> set = CommonUtil.newLinkedHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewLinkedHashSet_WithCapacity() {
        Set<String> set = CommonUtil.newLinkedHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
        assertTrue(set instanceof LinkedHashSet);
    }

    @Test
    public void testNewLinkedHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = CommonUtil.newLinkedHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set instanceof LinkedHashSet);

        Iterator<String> iter = set.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewTreeSet_NoArgs() {
        TreeSet<String> set = CommonUtil.newTreeSet();
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
        TreeSet<String> set = CommonUtil.newTreeSet(reverseComparator);
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
        TreeSet<String> set = CommonUtil.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        TreeSet<String> emptySet = CommonUtil.newTreeSet(new ArrayList<String>());
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testNewTreeSet_WithSortedSet() {
        SortedSet<String> source = new TreeSet<>();
        source.add("b");
        source.add("a");
        source.add("c");

        TreeSet<String> set = CommonUtil.newTreeSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertEquals("a", set.first());
        assertEquals("c", set.last());

        assertTrue(CommonUtil.newTreeSet((SortedSet<String>) null).isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_NoArgs() {
        Set<String> set = CommonUtil.newConcurrentHashSet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
        set.add("test");
        assertEquals(1, set.size());
    }

    @Test
    public void testNewConcurrentHashSet_WithCapacity() {
        Set<String> set = CommonUtil.newConcurrentHashSet(100);
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        Set<String> set = CommonUtil.newConcurrentHashSet(source);
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testNewSetFromMap() {
        Map<String, Boolean> map = new HashMap<>();
        Set<String> set = CommonUtil.newSetFromMap(map);
        assertNotNull(set);
        assertTrue(set.isEmpty());

        set.add("test");
        assertTrue(set.contains("test"));
        assertEquals(Boolean.TRUE, map.get("test"));
    }

    @Test
    public void testNewMultiset_NoArgs() {
        Multiset<String> multiset = CommonUtil.newMultiset();
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCapacity() {
        Multiset<String> multiset = CommonUtil.newMultiset(100);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapType() {
        Multiset<String> multiset = CommonUtil.newMultiset(LinkedHashMap.class);
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithMapSupplier() {
        Multiset<String> multiset = CommonUtil.newMultiset(() -> new TreeMap<String, MutableInt>());
        assertNotNull(multiset);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testNewMultiset_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> multiset = CommonUtil.newMultiset(source);
        assertNotNull(multiset);
        assertEquals(6, multiset.size());
        assertEquals(3, multiset.getCount("a"));
        assertEquals(2, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testNewArrayDeque_NoArgs() {
        ArrayDeque<String> deque = CommonUtil.newArrayDeque();
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCapacity() {
        ArrayDeque<String> deque = CommonUtil.newArrayDeque(100);
        assertNotNull(deque);
        assertTrue(deque.isEmpty());
    }

    @Test
    public void testNewArrayDeque_WithCollection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ArrayDeque<String> deque = CommonUtil.newArrayDeque(source);
        assertNotNull(deque);
        assertEquals(3, deque.size());
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 123);
        assertNotNull(entry);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());

        entry.setValue(456);
        assertEquals(456, entry.getValue());
    }

    @Test
    public void testNewImmutableEntry() {
        ImmutableEntry<String, Integer> entry = CommonUtil.newImmutableEntry("key", 123);
        assertNotNull(entry);
        assertEquals("key", entry.getKey());
        assertEquals(123, entry.getValue());

        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(456));
    }

    @Test
    public void testNewHashMap_NoArgs() {
        Map<String, Integer> map = CommonUtil.newHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof HashMap);
    }

    @Test
    public void testNewHashMap_WithCapacity() {
        Map<String, Integer> map = CommonUtil.newHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        Map<String, Integer> map = CommonUtil.newHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());

        Map<String, Integer> emptyMap = CommonUtil.newHashMap(new HashMap<>());
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNewHashMap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2), createBean("bean3", 3));

        Map<String, TestBean> map = CommonUtil.newHashMap(beans, TestBean::getName);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals(1, map.get("bean1").getValue());
        assertEquals(2, map.get("bean2").getValue());
        assertEquals(3, map.get("bean3").getValue());

        Map<String, TestBean> emptyMap = CommonUtil.newHashMap(new ArrayList<TestBean>(), TestBean::getName);
        assertNotNull(emptyMap);
        assertTrue(emptyMap.isEmpty());
    }

    @Test
    public void testNewLinkedHashMap_NoArgs() {
        Map<String, Integer> map = CommonUtil.newLinkedHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    public void testNewLinkedHashMap_WithCapacity() {
        Map<String, Integer> map = CommonUtil.newLinkedHashMap(100);
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

        Map<String, Integer> map = CommonUtil.newLinkedHashMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());

        Iterator<String> iter = map.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewLinkedHashMap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("bean1", 1), createBean("bean2", 2), createBean("bean3", 3));

        Map<String, TestBean> map = CommonUtil.newLinkedHashMap(beans, TestBean::getName);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertTrue(map instanceof LinkedHashMap);

        Iterator<String> iter = map.keySet().iterator();
        assertEquals("bean1", iter.next());
        assertEquals("bean2", iter.next());
        assertEquals("bean3", iter.next());
    }

    @Test
    public void testNewTreeMap_NoArgs() {
        TreeMap<String, Integer> map = CommonUtil.newTreeMap();
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
        TreeMap<String, Integer> map = CommonUtil.newTreeMap(reverseComparator);
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

        TreeMap<String, Integer> map = CommonUtil.newTreeMap(source);
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

        TreeMap<String, Integer> map = CommonUtil.newTreeMap(source);
        assertNotNull(map);
        assertEquals(3, map.size());
        assertEquals("a", map.firstKey());
        assertEquals("c", map.lastKey());

        assertTrue(CommonUtil.newTreeMap((SortedMap<String, Integer>) null).isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_NoArgs() {
        IdentityHashMap<String, Integer> map = CommonUtil.newIdentityHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithCapacity() {
        IdentityHashMap<String, Integer> map = CommonUtil.newIdentityHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewIdentityHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        IdentityHashMap<String, Integer> map = CommonUtil.newIdentityHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
    }

    @Test
    public void testNewConcurrentHashMap_NoArgs() {
        ConcurrentHashMap<String, Integer> map = CommonUtil.newConcurrentHashMap();
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithCapacity() {
        ConcurrentHashMap<String, Integer> map = CommonUtil.newConcurrentHashMap(100);
        assertNotNull(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void testNewConcurrentHashMap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ConcurrentHashMap<String, Integer> map = CommonUtil.newConcurrentHashMap(source);
        assertNotNull(map);
        assertEquals(2, map.size());
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("b").intValue());
    }

    @Test
    public void testNewBiMap_NoArgs() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap();
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacity() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(100);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithCapacityAndLoadFactor() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(100, 0.75f);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapTypes() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(LinkedHashMap.class, TreeMap.class);
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewBiMap_WithMapSuppliers() {
        BiMap<String, Integer> biMap = CommonUtil.newBiMap(() -> new LinkedHashMap<String, Integer>(), () -> new TreeMap<Integer, String>());
        assertNotNull(biMap);
        assertTrue(biMap.isEmpty());
    }

    @Test
    public void testNewMultimap() {
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newMultimap(() -> new HashMap<>(), () -> new ArrayList<>());
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMapType() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(LinkedHashMap.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMapAndValueTypes() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(LinkedHashMap.class, LinkedList.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithSuppliers() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(() -> new TreeMap<String, List<Integer>>(), () -> new LinkedList<Integer>());
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("a", 3);

        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewListMultimap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 2), createBean("group2", 3));

        ListMultimap<String, TestBean> multimap = CommonUtil.newListMultimap(beans, TestBean::getName);
        assertNotNull(multimap);
        assertEquals(2, multimap.get("group1").size());
        assertEquals(1, multimap.get("group2").size());
    }

    @Test
    public void testNewListMultimap_WithCollectionAndExtractors() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 2), createBean("group2", 3));

        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap(beans, TestBean::getName, TestBean::getValue);
        assertNotNull(multimap);
        assertEquals(Arrays.asList(1, 2), multimap.get("group1"));
        assertEquals(Arrays.asList(3), multimap.get("group2"));
    }

    @Test
    public void testNewLinkedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = CommonUtil.newLinkedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithCapacity() {
        ListMultimap<String, Integer> multimap = CommonUtil.newLinkedListMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        ListMultimap<String, Integer> multimap = CommonUtil.newLinkedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewSortedListMultimap_NoArgs() {
        ListMultimap<String, Integer> multimap = CommonUtil.newSortedListMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedListMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        ListMultimap<String, Integer> multimap = CommonUtil.newSortedListMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.totalValueCount());

        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMapType() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(LinkedHashMap.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMapAndValueTypes() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(LinkedHashMap.class, LinkedHashSet.class);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithSuppliers() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(() -> new TreeMap<String, Set<Integer>>(), () -> new LinkedHashSet<Integer>());
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewSetMultimap_WithCollectionAndKeyExtractor() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 1), createBean("group1", 2), createBean("group2", 3));

        SetMultimap<String, TestBean> multimap = CommonUtil.newSetMultimap(beans, TestBean::getName);
        assertNotNull(multimap);
        assertEquals(3, multimap.get("group1").size());
        assertEquals(1, multimap.get("group2").size());
    }

    @Test
    public void testNewSetMultimap_WithCollectionAndExtractors() {
        List<TestBean> beans = Arrays.asList(createBean("group1", 1), createBean("group1", 1), createBean("group1", 2), createBean("group2", 3));

        SetMultimap<String, Integer> multimap = CommonUtil.newSetMultimap(beans, TestBean::getName, TestBean::getValue);
        assertNotNull(multimap);
        assertEquals(2, multimap.get("group1").size());
        assertEquals(1, multimap.get("group2").size());
    }

    @Test
    public void testNewLinkedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = CommonUtil.newLinkedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithCapacity() {
        SetMultimap<String, Integer> multimap = CommonUtil.newLinkedSetMultimap(100);
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewLinkedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        SetMultimap<String, Integer> multimap = CommonUtil.newLinkedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(2, multimap.totalValueCount());
    }

    @Test
    public void testNewSortedSetMultimap_NoArgs() {
        SetMultimap<String, Integer> multimap = CommonUtil.newSortedSetMultimap();
        assertNotNull(multimap);
        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testNewSortedSetMultimap_WithMap() {
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        source.put("c", 3);

        SetMultimap<String, Integer> multimap = CommonUtil.newSortedSetMultimap(source);
        assertNotNull(multimap);
        assertEquals(3, multimap.totalValueCount());

        Iterator<String> iter = multimap.keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewEmptyDataset() {
        Dataset ds = CommonUtil.newEmptyDataset();
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.size());
        assertEquals(0, ds.columnCount());
    }

    @Test
    public void testNewEmptyDataset_WithColumnNames() {
        List<String> columnNames = Arrays.asList("col1", "col2", "col3");
        Dataset ds = CommonUtil.newEmptyDataset(columnNames);
        assertNotNull(ds);
        assertTrue(ds.isEmpty());
        assertEquals(0, ds.size());
        assertEquals(3, ds.columnCount());
        assertEquals(columnNames, ds.columnNames());
    }

    @Test
    public void testNewEmptyDataset_WithProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("key", "value");

        List<String> columnNames = Arrays.asList("col1", "col2");
        Dataset ds = CommonUtil.newEmptyDataset(columnNames, properties);
        assertNotNull(ds);
        assertEquals(0, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals("value", ds.getProperties().get("key"));
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

        Dataset ds = CommonUtil.newDataset(rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertTrue(ds.columnNames().contains("name"));
        assertTrue(ds.columnNames().contains("age"));
    }

    @Test
    public void testNewDataset_FromRowsWithProperties() {
        List<Map<String, Object>> rows = Arrays.asList(createMap("name", "John", "age", 30));

        Map<String, Object> properties = new HashMap<>();
        properties.put("source", "test");

        Dataset ds = CommonUtil.newDataset(rows, properties);
        assertNotNull(ds);
        assertEquals(1, ds.size());
        assertEquals("test", ds.getProperties().get("source"));
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRows() {
        List<String> columnNames = Arrays.asList("name", "age");
        List<Object[]> rows = Arrays.asList(new Object[] { "John", 30 }, new Object[] { "Jane", 25 });

        Dataset ds = CommonUtil.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(columnNames, ds.columnNames());
    }

    @Test
    public void testNewDataset_WithColumnNamesAndRowArray() {
        List<String> columnNames = Arrays.asList("name", "age");
        Object[][] rows = new Object[][] { { "John", 30 }, { "Jane", 25 } };

        Dataset ds = CommonUtil.newDataset(columnNames, rows);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
    }

    @Test
    public void testNewDataset_FromKeyValueMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("John", 30);
        map.put("Jane", 25);

        Dataset ds = CommonUtil.newDataset("Name", "Age", map);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(Arrays.asList("Name", "Age"), ds.columnNames());
    }

    @Test
    public void testNewDataset_FromMapOfCollections() {
        Map<String, List<Object>> map = new LinkedHashMap<>();
        map.put("name", Arrays.asList("John", "Jane"));
        map.put("age", Arrays.asList(30, 25));

        Dataset ds = CommonUtil.newDataset(map);
        assertNotNull(ds);
        assertEquals(2, ds.size());
        assertEquals(2, ds.columnCount());
        assertEquals(Arrays.asList("name", "age"), ds.columnNames());
    }

    @Test
    public void testNewDataset_SingleColumn() {
        List<String> values = Arrays.asList("A", "B", "C");
        Dataset ds = CommonUtil.newDataset("Letter", values);
        assertNotNull(ds);
        assertEquals(3, ds.size());
        assertEquals(1, ds.columnCount());
        assertEquals("Letter", ds.columnNames().get(0));
    }

    @Test
    public void testMerge_TwoDatasets() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }, new Object[] { "B" }));

        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }, new Object[] { 2 }));

        Dataset merged = CommonUtil.merge(ds1, ds2);
        assertNotNull(merged);
        assertEquals(4, merged.size());
        assertEquals(2, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(null, ds2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(ds1, null));
    }

    @Test
    public void testMerge_ThreeDatasets() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" }));
        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 }));
        Dataset ds3 = CommonUtil.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true }));

        Dataset merged = CommonUtil.merge(ds1, ds2, ds3);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(null, ds2, ds3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(ds1, null, ds3));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(ds1, ds2, (Dataset) null));
    }

    @Test
    public void testMerge_CollectionOfDatasets() {
        List<Dataset> datasets = Arrays.asList(CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(new Object[] { "A" })),
                CommonUtil.newDataset(Arrays.asList("col2"), Arrays.asList(new Object[] { 1 })),
                CommonUtil.newDataset(Arrays.asList("col3"), Arrays.asList(new Object[] { true })));

        Dataset merged = CommonUtil.merge(datasets);
        assertNotNull(merged);
        assertEquals(3, merged.size());
        assertEquals(3, merged.columnCount());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(new ArrayList<>()));

        Dataset single = CommonUtil.merge(Arrays.asList(datasets.get(0)));
        assertNotNull(single);
        assertEquals(1, single.size());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge((Collection<Dataset>) null));
    }

    @Test
    public void testMerge_WithRequiresSameColumns() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1", "col2"), CommonUtil.asSingletonList(new Object[] { "A", 1 }));

        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col1", "col2"), CommonUtil.asSingletonList(new Object[] { "B", 2 }));

        Dataset ds3 = CommonUtil.newDataset(Arrays.asList("col1", "col3"), CommonUtil.asSingletonList(new Object[] { "C", 3 }));

        Dataset merged = CommonUtil.merge(Arrays.asList(ds1, ds2), true);
        assertNotNull(merged);
        assertEquals(2, merged.size());

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.merge(Arrays.asList(ds1, ds3), true));
    }

    @Test
    public void testToArray_Collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        Object[] array = CommonUtil.toArray(list);
        assertArrayEquals(new Object[] { "a", "b", "c" }, array);

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        Object[] setArray = CommonUtil.toArray(set);
        assertEquals(3, setArray.length);

        Object[] emptyArray = CommonUtil.toArray(new ArrayList<>());
        assertEquals(0, emptyArray.length);

        Object[] nullArray = CommonUtil.toArray(null);
        assertEquals(0, nullArray.length);
    }

    @Test
    public void testToArray_CollectionRange() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Object[] array = CommonUtil.toArray(list, 1, 4);
        assertArrayEquals(new Object[] { "b", "c", "d" }, array);

        Object[] fullArray = CommonUtil.toArray(list, 0, list.size());
        assertArrayEquals(new Object[] { "a", "b", "c", "d", "e" }, fullArray);

        Object[] emptyArray = CommonUtil.toArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(list, 3, 2));
    }

    @Test
    public void testToArray_01() {
        {
            Collection<String> list = CommonUtil.asLinkedHashSet("a", "b", "c", "d", "e");
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4));
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, new String[1]));
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, String[]::new));
            assertArrayEquals(new Object[] { "b", "c", "d" }, CommonUtil.toArray(list, 1, 4, String[].class));
        }
        {
            Collection<Boolean> c = CommonUtil.asLinkedList(true, true, false, null, true);
            assertArrayEquals(new boolean[] { true, false, false }, CommonUtil.toBooleanArray(c, 1, 4, false));
        }
        {
            Collection<Byte> c = CommonUtil.asLinkedList((byte) 1, (byte) 2, (byte) 3, null, (byte) 4);
            assertArrayEquals(new byte[] { 2, 3, 0 }, CommonUtil.toByteArray(c, 1, 4, (byte) 0));
        }
        {
            Collection<Short> c = CommonUtil.asLinkedList((short) 1, (short) 2, (short) 3, null, (short) 4);
            assertArrayEquals(new short[] { 2, 3, 0 }, CommonUtil.toShortArray(c, 1, 4, (short) 0));
        }
        {
            Collection<Integer> c = CommonUtil.asLinkedList(1, 2, 3, null, 4);
            assertArrayEquals(new int[] { 2, 3, 0 }, CommonUtil.toIntArray(c, 1, 4, 0));
        }
        {
            Collection<Long> c = CommonUtil.asLinkedList(1L, 2L, 3L, null, 4L);
            assertArrayEquals(new long[] { 2L, 3L, 0L }, CommonUtil.toLongArray(c, 1, 4, 0L));
        }
        {
            Collection<Float> c = CommonUtil.asLinkedList(1.0f, 2.0f, 3.0f, null, 4.0f);
            assertArrayEquals(new float[] { 2.0f, 3.0f, 0.0f }, CommonUtil.toFloatArray(c, 1, 4, 0.0f));
        }
        {
            Collection<Double> c = CommonUtil.asLinkedList(1.0d, 2.0d, 3.0d, null, 4.0d);
            assertArrayEquals(new double[] { 2.0d, 3.0d, 0.0d }, CommonUtil.toDoubleArray(c, 1, 4, 0.0d));
        }
    }

    @Test
    public void testToArray_WithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c");

        String[] target = new String[5];
        String[] result = CommonUtil.toArray(list, target);
        assertSame(target, result);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
        assertNull(result[3]);

        String[] smallTarget = new String[2];
        String[] newResult = CommonUtil.toArray(list, smallTarget);
        assertNotSame(smallTarget, newResult);
        assertEquals(3, newResult.length);

        String[] emptyResult = CommonUtil.toArray(new ArrayList<String>(), new String[0]);
        assertEquals(0, emptyResult.length);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetArray() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        String[] target = new String[3];
        String[] result = CommonUtil.toArray(list, 1, 4, target);
        assertSame(target, result);
        assertArrayEquals(new String[] { "b", "c", "d" }, result);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToArray_WithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = CommonUtil.toArray(list, String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        String[] emptyArray = CommonUtil.toArray(new ArrayList<String>(), String[]::new);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToArray_RangeWithArraySupplier() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = CommonUtil.toArray(list, 1, 4, String[]::new);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);
    }

    @Test
    public void testToArray_WithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = CommonUtil.toArray(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        String[] emptyArray = CommonUtil.toArray(new ArrayList<String>(), String[].class);
        assertEquals(0, emptyArray.length);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, (String[]) null));
    }

    @Test
    public void testToArray_RangeWithTargetType() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String[] array = CommonUtil.toArray(list, 1, 4, String[].class);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(list, 1, 4, (String[]) null));
    }

    @Test
    public void testToBooleanArray() {
        List<Boolean> list = Arrays.asList(true, false, true, null, false);

        boolean[] array = CommonUtil.toBooleanArray(list);
        assertArrayEquals(new boolean[] { true, false, true, false, false }, array);

        boolean[] arrayWithDefault = CommonUtil.toBooleanArray(list, true);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, arrayWithDefault);

        boolean[] emptyArray = CommonUtil.toBooleanArray(new ArrayList<>());
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToBooleanArray_Range() {
        List<Boolean> list = Arrays.asList(true, false, true, null, false);

        boolean[] array = CommonUtil.toBooleanArray(list, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false }, array);

        boolean[] arrayWithDefault = CommonUtil.toBooleanArray(list, 1, 4, true);
        assertArrayEquals(new boolean[] { false, true, true }, arrayWithDefault);

        boolean[] emptyArray = CommonUtil.toBooleanArray(list, 2, 2);
        assertEquals(0, emptyArray.length);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toBooleanArray(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toBooleanArray(list, 0, 6));
    }

    @Test
    public void testToBooleanArray_FromByteArray() {
        byte[] bytes = { 0, 1, -1, 127, -128 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, CommonUtil.toBooleanArray(bytes));

        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray(new byte[0]));
        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray((byte[]) null));
    }

    @Test
    public void testToBooleanArray_FromIntArray() {
        int[] ints = { 0, 1, -1, 100, -100 };
        boolean[] expected = { false, true, false, true, false };
        assertArrayEquals(expected, CommonUtil.toBooleanArray(ints));

        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray(new int[0]));
        assertArrayEquals(new boolean[0], CommonUtil.toBooleanArray((int[]) null));
    }

    @Test
    public void testToCharArray() {
        List<Character> list = Arrays.asList('a', 'b', 'c', null, 'd');

        char[] array = CommonUtil.toCharArray(list);
        assertArrayEquals(new char[] { 'a', 'b', 'c', '\0', 'd' }, array);

        char[] arrayWithDefault = CommonUtil.toCharArray(list, 'X');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'X', 'd' }, arrayWithDefault);
    }

    @Test
    public void testToCharArray_Range() {
        List<Character> list = Arrays.asList('a', 'b', 'c', null, 'd');

        char[] array = CommonUtil.toCharArray(list, 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', '\0' }, array);

        char[] arrayWithDefault = CommonUtil.toCharArray(list, 1, 4, 'X');
        assertArrayEquals(new char[] { 'b', 'c', 'X' }, arrayWithDefault);
    }

    @Test
    public void testToByteArray() {
        List<Number> list = Arrays.asList((byte) 1, 2, 3L, null, 5.0);

        byte[] array = CommonUtil.toByteArray(list);
        assertArrayEquals(new byte[] { 1, 2, 3, 0, 5 }, array);

        byte[] arrayWithDefault = CommonUtil.toByteArray(list, (byte) -1);
        assertArrayEquals(new byte[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToByteArray_Range() {
        List<Number> list = Arrays.asList((byte) 1, 2, 3L, null, 5.0);

        byte[] array = CommonUtil.toByteArray(list, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 0 }, array);

        byte[] arrayWithDefault = CommonUtil.toByteArray(list, 1, 4, (byte) -1);
        assertArrayEquals(new byte[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToByteArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        byte[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, CommonUtil.toByteArray(bools));

        assertArrayEquals(new byte[0], CommonUtil.toByteArray(new boolean[0]));
        assertArrayEquals(new byte[0], CommonUtil.toByteArray((boolean[]) null));
    }

    @Test
    public void testToShortArray() {
        List<Number> list = Arrays.asList((short) 1, 2, 3L, null, 5.0);

        short[] array = CommonUtil.toShortArray(list);
        assertArrayEquals(new short[] { 1, 2, 3, 0, 5 }, array);

        short[] arrayWithDefault = CommonUtil.toShortArray(list, (short) -1);
        assertArrayEquals(new short[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToShortArray_Range() {
        List<Number> list = Arrays.asList((short) 1, 2, 3L, null, 5.0);

        short[] array = CommonUtil.toShortArray(list, 1, 4);
        assertArrayEquals(new short[] { 2, 3, 0 }, array);

        short[] arrayWithDefault = CommonUtil.toShortArray(list, 1, 4, (short) -1);
        assertArrayEquals(new short[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        int[] array = CommonUtil.toIntArray(list);
        assertArrayEquals(new int[] { 1, 2, 3, 0, 5 }, array);

        int[] arrayWithDefault = CommonUtil.toIntArray(list, -1);
        assertArrayEquals(new int[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        int[] array = CommonUtil.toIntArray(list, 1, 4);
        assertArrayEquals(new int[] { 2, 3, 0 }, array);

        int[] arrayWithDefault = CommonUtil.toIntArray(list, 1, 4, -1);
        assertArrayEquals(new int[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToIntArray_FromCharArray() {
        char[] chars = { 'A', 'B', 'C' };
        int[] expected = { 65, 66, 67 };
        assertArrayEquals(expected, CommonUtil.toIntArray(chars));

        assertArrayEquals(new int[0], CommonUtil.toIntArray(new char[0]));
        assertArrayEquals(new int[0], CommonUtil.toIntArray((char[]) null));
    }

    @Test
    public void testToIntArray_FromBooleanArray() {
        boolean[] bools = { true, false, true, false };
        int[] expected = { 1, 0, 1, 0 };
        assertArrayEquals(expected, CommonUtil.toIntArray(bools));

        assertArrayEquals(new int[0], CommonUtil.toIntArray(new boolean[0]));
        assertArrayEquals(new int[0], CommonUtil.toIntArray((boolean[]) null));
    }

    @Test
    public void testToLongArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        long[] array = CommonUtil.toLongArray(list);
        assertArrayEquals(new long[] { 1, 2, 3, 0, 5 }, array);

        long[] arrayWithDefault = CommonUtil.toLongArray(list, -1L);
        assertArrayEquals(new long[] { 1, 2, 3, -1, 5 }, arrayWithDefault);
    }

    @Test
    public void testToLongArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        long[] array = CommonUtil.toLongArray(list, 1, 4);
        assertArrayEquals(new long[] { 2, 3, 0 }, array);

        long[] arrayWithDefault = CommonUtil.toLongArray(list, 1, 4, -1L);
        assertArrayEquals(new long[] { 2, 3, -1 }, arrayWithDefault);
    }

    @Test
    public void testToFloatArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        float[] array = CommonUtil.toFloatArray(list);
        assertArrayEquals(new float[] { 1, 2, 3, 0, 5 }, array, 0.0f);

        float[] arrayWithDefault = CommonUtil.toFloatArray(list, -1.0f);
        assertArrayEquals(new float[] { 1, 2, 3, -1, 5 }, arrayWithDefault, 0.0f);
    }

    @Test
    public void testToFloatArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        float[] array = CommonUtil.toFloatArray(list, 1, 4);
        assertArrayEquals(new float[] { 2, 3, 0 }, array, 0.0f);

        float[] arrayWithDefault = CommonUtil.toFloatArray(list, 1, 4, -1.0f);
        assertArrayEquals(new float[] { 2, 3, -1 }, arrayWithDefault, 0.0f);
    }

    @Test
    public void testToDoubleArray() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        double[] array = CommonUtil.toDoubleArray(list);
        assertArrayEquals(new double[] { 1, 2, 3, 0, 5 }, array, 0.0);

        double[] arrayWithDefault = CommonUtil.toDoubleArray(list, -1.0);
        assertArrayEquals(new double[] { 1, 2, 3, -1, 5 }, arrayWithDefault, 0.0);
    }

    @Test
    public void testToDoubleArray_Range() {
        List<Number> list = Arrays.asList(1, 2L, 3.0, null, (byte) 5);

        double[] array = CommonUtil.toDoubleArray(list, 1, 4);
        assertArrayEquals(new double[] { 2, 3, 0 }, array, 0.0);

        double[] arrayWithDefault = CommonUtil.toDoubleArray(list, 1, 4, -1.0);
        assertArrayEquals(new double[] { 2, 3, -1 }, arrayWithDefault, 0.0);
    }

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
