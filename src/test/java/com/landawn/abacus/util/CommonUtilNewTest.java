package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
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
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

public class CommonUtilNewTest extends CommonUtilTestSupport {

    @Test
    public void testNewProxyInstance() {
        java.util.function.Function<String, String> func = s -> s.toUpperCase();
        Object proxy = CommonUtil.newProxyInstance(new Class<?>[] { java.util.function.Function.class }, (p, method, args) -> {
            if (method.getName().equals("apply")) {
                return func.apply((String) args[0]);
            }
            return null;
        });
        assertNotNull(proxy);
        assertNotNull(CommonUtil.newProxyInstance(Runnable.class, (p, method, args) -> null));
    }

    @Test
    public void testNewInstance() {
        assertEquals("", CommonUtil.newInstance(String.class));
        assertTrue(CommonUtil.newInstance(ArrayList.class).isEmpty());
        assertTrue(CommonUtil.newInstance(List.class).isEmpty());
        assertTrue(CommonUtil.newInstance(Map.class).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newInstance(Number.class));
        assertNotNull(CommonUtil.newInstance(NewInstanceTopOuter.Mid.Leaf.class));
        assertNotNull(CommonUtil.newInstance(NewInstanceTopOuter.Mid.class));
    }

    @Test
    public void testNewCollection() {
        Collection<String> list = CommonUtil.newCollection(List.class);
        assertTrue(list.isEmpty());
        Collection<String> arrayList = CommonUtil.newCollection(ArrayList.class, 10);
        assertTrue(arrayList instanceof ArrayList);
        assertTrue(CommonUtil.<String> newCollection(Set.class) instanceof Set);
        assertTrue(CommonUtil.newCollection(Queue.class) instanceof Queue);
        assertTrue(CommonUtil.newCollection(LinkedList.class, 5) instanceof LinkedList);
        assertTrue(CommonUtil.newCollection(HashSet.class, 10).isEmpty());
    }

    @Test
    public void testNewMap() {
        assertTrue(CommonUtil.newMap(Map.class).isEmpty());
        assertTrue(CommonUtil.newMap(HashMap.class) instanceof HashMap);
        assertTrue(CommonUtil.newMap(TreeMap.class) instanceof TreeMap);
        Map<String, Integer> sized = CommonUtil.newMap(HashMap.class, 10);
        sized.put("test", 1);
        assertEquals(1, sized.size());
        assertTrue(CommonUtil.newMap(LinkedHashMap.class, 10) instanceof LinkedHashMap);
    }

    @Test
    public void testNewArray() {
        String[] arr = CommonUtil.newArray(String.class, 3);
        assertEquals(3, arr.length);
        assertNull(arr[0]);
        assertEquals(10, ((int[]) CommonUtil.newArray(int.class, 10)).length);
        assertEquals(0, ((Object[]) CommonUtil.newArray(Object.class, 0)).length);
        assertThrows(NegativeArraySizeException.class, () -> CommonUtil.newArray(int.class, -1));
        int[][] int2D = CommonUtil.newArray(int.class, 3, 4);
        assertEquals(3, int2D.length);
        assertEquals(4, int2D[0].length);
        String[][][] str3D = CommonUtil.newArray(String.class, 2, 3, 4);
        assertEquals(4, str3D[0][0].length);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newArray(null, 10));
        assertThrows(NegativeArraySizeException.class, () -> CommonUtil.newArray(int.class, 5, -1));
    }

    @Test
    public void testNewArrayList() {
        ArrayList<String> empty = CommonUtil.newArrayList();
        empty.add("test");
        assertEquals(1, empty.size());
        assertTrue(CommonUtil.newArrayList(100).isEmpty());
        ArrayList<String> copied = CommonUtil.newArrayList(Arrays.asList("a", "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c"), copied);
        assertTrue(CommonUtil.newArrayList(new ArrayList<>()).isEmpty());
        assertTrue(CommonUtil.newArrayList(null).isEmpty());
    }

    @Test
    public void testNewLinkedList() {
        LinkedList<String> list = CommonUtil.newLinkedList();
        list.add("test");
        assertEquals(1, list.size());
        LinkedList<String> copied = CommonUtil.newLinkedList(Arrays.asList("a", "b", "c"));
        assertEquals("a", copied.getFirst());
        assertEquals("c", copied.getLast());
        assertTrue(CommonUtil.newLinkedList(null).isEmpty());
    }

    @Test
    public void testNewHashSet() {
        Set<String> set = CommonUtil.newHashSet();
        set.add("test");
        assertEquals(1, set.size());
        assertTrue(CommonUtil.newHashSet(100).isEmpty());
        Set<String> copied = CommonUtil.newHashSet(Arrays.asList("a", "b", "c", "a"));
        assertEquals(3, copied.size());
        assertTrue(CommonUtil.newHashSet(null).isEmpty());
    }

    @Test
    public void testNewLinkedHashSet() {
        Set<String> set = CommonUtil.newLinkedHashSet();
        assertTrue(set instanceof LinkedHashSet);
        assertTrue(CommonUtil.newLinkedHashSet(100) instanceof LinkedHashSet);
        Set<String> copied = CommonUtil.newLinkedHashSet(Arrays.asList("a", "b", "c"));
        Iterator<String> iter = copied.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testNewTreeSet() {
        TreeSet<String> set = CommonUtil.newTreeSet();
        set.add("b");
        set.add("a");
        set.add("c");
        assertEquals("a", set.first());
        TreeSet<String> reverse = CommonUtil.newTreeSet((a, b) -> b.compareTo(a));
        reverse.addAll(Arrays.asList("a", "b", "c"));
        assertEquals("c", reverse.first());
        TreeSet<String> copied = CommonUtil.newTreeSet(Arrays.asList("b", "a", "c"));
        assertEquals("a", copied.first());
        SortedSet<String> source = new TreeSet<>(Arrays.asList("b", "a", "c"));
        assertEquals("c", CommonUtil.newTreeSet(source).last());
        assertTrue(CommonUtil.newTreeSet((SortedSet<String>) null).isEmpty());
    }

    @Test
    public void testNewConcurrentHashSet() {
        Set<String> set = CommonUtil.newConcurrentHashSet();
        set.add("test");
        assertEquals(1, set.size());
        assertTrue(CommonUtil.newConcurrentHashSet(100).isEmpty());
        assertEquals(3, CommonUtil.newConcurrentHashSet(Arrays.asList("a", "b", "c")).size());
    }

    @Test
    public void testNewSetFromMap() {
        Map<String, Boolean> map = new HashMap<>();
        Set<String> set = CommonUtil.newSetFromMap(map);
        set.add("test");
        assertTrue(map.containsKey("test"));
    }

    @Test
    public void testNewMultiset() {
        assertTrue(CommonUtil.newMultiset().isEmpty());
        assertTrue(CommonUtil.newMultiset(100).isEmpty());
        assertTrue(CommonUtil.newMultiset(LinkedHashMap.class).isEmpty());
        assertTrue(CommonUtil.newMultiset(() -> new TreeMap<String, MutableInt>()).isEmpty());
        Multiset<String> ms = CommonUtil.newMultiset(Arrays.asList("a", "b", "a", "c", "b", "a"));
        assertEquals(6, ms.size());
        assertEquals(3, ms.getCount("a"));
    }

    @Test
    public void testNewArrayDeque() {
        ArrayDeque<String> deque = CommonUtil.newArrayDeque();
        assertTrue(deque.isEmpty());
        assertTrue(CommonUtil.newArrayDeque(100).isEmpty());
        ArrayDeque<String> copied = CommonUtil.newArrayDeque(Arrays.asList("a", "b", "c"));
        assertEquals("a", copied.getFirst());
        assertEquals("c", copied.getLast());
    }

    @Test
    public void testNewEntry() {
        Map.Entry<String, Integer> entry = CommonUtil.newEntry("key", 100);
        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());
        Map.Entry<String, Integer> immutable = CommonUtil.newImmutableEntry("key", 100);
        assertThrows(UnsupportedOperationException.class, () -> immutable.setValue(200));
    }

    @Test
    public void testNewHashMap() {
        Map<String, Integer> map = CommonUtil.newHashMap();
        map.put("a", 1);
        assertEquals(1, map.size());
        assertTrue(CommonUtil.newHashMap(100).isEmpty());
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        assertEquals(2, CommonUtil.newHashMap(source).size());
        Map<Character, String> extracted = CommonUtil.newHashMap(Arrays.asList("alice", "bob"), s -> s.charAt(0));
        assertEquals("alice", extracted.get('a'));
        assertTrue(CommonUtil.newHashMap(Collections.<String> emptyList(), s -> s).isEmpty());
    }

    @Test
    public void testNewLinkedHashMap() {
        Map<String, Integer> map = CommonUtil.newLinkedHashMap();
        assertTrue(map instanceof LinkedHashMap);
        assertTrue(CommonUtil.newLinkedHashMap(100) instanceof LinkedHashMap);
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        source.put("c", 3);
        Iterator<String> iter = CommonUtil.newLinkedHashMap(source).keySet().iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertEquals(2, CommonUtil.newLinkedHashMap(Arrays.asList("alice", "bob"), s -> s.charAt(0)).size());
        assertTrue(CommonUtil.newLinkedHashMap(Collections.<String> emptyList(), s -> s).isEmpty());
    }

    @Test
    public void testNewTreeMap() {
        TreeMap<String, Integer> map = CommonUtil.newTreeMap();
        map.put("b", 2);
        map.put("a", 1);
        map.put("c", 3);
        assertEquals("a", map.firstKey());
        TreeMap<String, Integer> reverse = CommonUtil.newTreeMap((Comparator<String>) (a, b) -> b.compareTo(a));
        reverse.put("a", 1);
        reverse.put("c", 3);
        assertEquals("c", reverse.firstKey());
        Map<String, Integer> source = new HashMap<>();
        source.put("b", 2);
        source.put("a", 1);
        assertEquals("a", CommonUtil.newTreeMap(source).firstKey());
        assertTrue(CommonUtil.newTreeMap((java.util.SortedMap<String, Integer>) null).isEmpty());
    }

    @Test
    public void testNewIdentityHashMap() {
        assertTrue(CommonUtil.newIdentityHashMap().isEmpty());
        assertTrue(CommonUtil.newIdentityHashMap(100).isEmpty());
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        assertEquals(2, CommonUtil.newIdentityHashMap(source).size());
        IdentityHashMap<String, Integer> typed = CommonUtil.newIdentityHashMap();
        assertNotNull(typed);
    }

    @Test
    public void testNewConcurrentHashMap() {
        assertTrue(CommonUtil.newConcurrentHashMap() instanceof ConcurrentHashMap);
        assertTrue(CommonUtil.newConcurrentHashMap(100).isEmpty());
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        assertEquals(1, CommonUtil.newConcurrentHashMap(source).get("a").intValue());
    }

    @Test
    public void testNewBiMap() {
        assertTrue(CommonUtil.newBiMap().isEmpty());
        assertTrue(CommonUtil.newBiMap(100).isEmpty());
        assertTrue(CommonUtil.newBiMap(100, 0.75f).isEmpty());
        assertTrue(CommonUtil.newBiMap(LinkedHashMap.class, TreeMap.class).isEmpty());
        assertTrue(CommonUtil.newBiMap(() -> new LinkedHashMap<String, Integer>(), () -> new TreeMap<Integer, String>()).isEmpty());
    }

    @Test
    public void testNewMultimap() {
        assertTrue(CommonUtil.newMultimap(HashMap::new, ArrayList::new).isEmpty());
        assertTrue(CommonUtil.newListMultimap().isEmpty());
        assertTrue(CommonUtil.newListMultimap(100).isEmpty());
        assertTrue(CommonUtil.newListMultimap(LinkedHashMap.class).isEmpty());
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        assertEquals(2, CommonUtil.newListMultimap(source).totalValueCount());
        ListMultimap<Character, String> byFirst = CommonUtil.newListMultimap(Arrays.asList("apple", "banana", "avocado"), s -> s.charAt(0));
        assertEquals(2, byFirst.get('a').size());
        ListMultimap<Character, Integer> lengths = CommonUtil.newListMultimap(Arrays.asList("apple", "banana"), s -> s.charAt(0), String::length);
        assertEquals(5, (int) lengths.get('a').get(0));
        assertTrue(CommonUtil.newLinkedListMultimap().isEmpty());
        assertTrue(CommonUtil.newLinkedListMultimap(100).isEmpty());
        assertEquals(2, CommonUtil.newLinkedListMultimap(source).totalValueCount());
        assertTrue(CommonUtil.newSortedListMultimap().isEmpty());
        ListMultimap<String, Integer> sorted = CommonUtil.newSortedListMultimap(new HashMap<String, Integer>() {
            {
                put("b", 2);
                put("a", 1);
                put("c", 3);
            }
        });
        Iterator<String> keys = sorted.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("b", keys.next());
        assertEquals("c", keys.next());
    }

    @Test
    public void testNewSetMultimap() {
        assertTrue(CommonUtil.newSetMultimap().isEmpty());
        assertTrue(CommonUtil.newSetMultimap(100).isEmpty());
        assertTrue(CommonUtil.newSetMultimap(LinkedHashMap.class).isEmpty());
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        SetMultimap<String, Integer> fromMap = CommonUtil.newSetMultimap(source);
        assertTrue(fromMap.get("a").contains(1));
        SetMultimap<Character, String> byFirst = CommonUtil.newSetMultimap(Arrays.asList("apple", "avocado", "banana"), w -> w.charAt(0));
        assertEquals(2, byFirst.get('a').size());
        SetMultimap<Character, Integer> lengths = CommonUtil.newSetMultimap(Arrays.asList("apple", "avocado", "banana"), w -> w.charAt(0), String::length);
        assertTrue(lengths.get('a').contains(5));
        assertTrue(lengths.get('a').contains(7));
        assertTrue(CommonUtil.newLinkedSetMultimap().isEmpty());
        assertTrue(CommonUtil.newLinkedSetMultimap(100).isEmpty());
        assertEquals(2, CommonUtil.newLinkedSetMultimap(source).totalValueCount());
        assertTrue(CommonUtil.newSortedSetMultimap().isEmpty());
        SetMultimap<String, Integer> sorted = CommonUtil.newSortedSetMultimap(new HashMap<String, Integer>() {
            {
                put("b", 2);
                put("a", 1);
                put("c", 3);
            }
        });
        Iterator<String> keys = sorted.keySet().iterator();
        assertEquals("a", keys.next());
        assertEquals("b", keys.next());
        assertEquals("c", keys.next());
    }

    @Test
    public void testNewEmptyDataset() {
        Dataset empty = CommonUtil.newEmptyDataset();
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.columnCount());
        List<String> cols = Arrays.asList("col1", "col2", "col3");
        Dataset named = CommonUtil.newEmptyDataset(cols);
        assertEquals(3, named.columnCount());
        assertEquals(cols, named.columnNames());
        Map<String, Object> props = new HashMap<>();
        props.put("key", "value");
        Dataset withProps = CommonUtil.newEmptyDataset(Arrays.asList("col1", "col2"), props);
        assertEquals("value", withProps.getProperties().get("key"));
    }

    @Test
    public void testNewDataset() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("name"), Arrays.asList(new Object[] { "Alice" }));
        assertEquals(1, ds.size());
        Dataset rows = CommonUtil.newDataset(Arrays.asList("name", "age"), Arrays.asList(new Object[] { "John", 30 }, new Object[] { "Jane", 25 }));
        assertEquals(2, rows.size());
        Dataset fromArray = CommonUtil.newDataset(Arrays.asList("name", "age"), new Object[][] { { "John", 30 }, { "Jane", 25 } });
        assertEquals(2, fromArray.size());
        Map<String, Integer> kv = new LinkedHashMap<>();
        kv.put("John", 30);
        kv.put("Jane", 25);
        Dataset fromKv = CommonUtil.newDataset("Name", "Age", kv);
        assertEquals(Arrays.asList("Name", "Age"), fromKv.columnNames());
        Map<String, List<Object>> columns = new LinkedHashMap<>();
        columns.put("name", Arrays.asList("John", "Jane"));
        columns.put("age", Arrays.asList(30, 25));
        Dataset fromCols = CommonUtil.newDataset(columns);
        assertEquals(2, fromCols.size());
        Dataset single = CommonUtil.newDataset("Letter", Arrays.asList("A", "B", "C"));
        assertEquals(1, single.columnCount());
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("source", "beanRows");
        Dataset beanRows = CommonUtil.newDataset(Arrays.asList("name", "age", "missing"),
                Arrays.asList(new DatasetRowBean("Tom", 10), new DatasetRowBean("Jerry", 12)), props);
        assertEquals("beanRows", beanRows.getProperties().get("source"));
        Dataset emptyRows = CommonUtil.newDataset(Arrays.asList("id", "name"), new ArrayList<>(), props);
        assertEquals(0, emptyRows.size());
        List<Map<String, Object>> mapRows = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "Alice");
        row1.put("age", 30);
        mapRows.add(row1);
        Dataset fromMapRows = CommonUtil.newDataset(Arrays.asList("name", "age"), mapRows, null);
        assertEquals(2, fromMapRows.columnCount());
        Dataset fromCollRows = CommonUtil.newDataset(Arrays.asList("x", "y"), Arrays.asList(Arrays.asList("a", 1), Arrays.asList("b", 2)), null);
        assertEquals(2, fromCollRows.size());
    }

    @Test
    public void testNewDataset_EdgeCase() {
        Dataset missing = CommonUtil.newDataset(Arrays.asList("name", "age", "missing"),
                Arrays.asList(new DatasetRowBean("Tom", 10), new DatasetRowBean("Jerry", 12)));
        assertNull(missing.get(0, missing.getColumnIndex("missing")));
        assertEquals("Tom", missing.get(0, missing.getColumnIndex("name")));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList((Object) new Object[] { 1, 2, 3 })));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.<Object> asList(1, 2, 3))));
        Dataset scalars = CommonUtil.newDataset(Arrays.asList("val"), Arrays.asList("x", "y", "z"));
        assertEquals("y", scalars.get(1, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList((Object) "unsupportedScalar")));
        List<Object> withNullRow = new ArrayList<>();
        withNullRow.add(null);
        withNullRow.add(new Object[] { 1, 2 });
        Dataset nullRow = CommonUtil.newDataset(Arrays.asList("a", "b"), withNullRow);
        assertNull(nullRow.get(0, 0));
        assertEquals(1, (int) nullRow.get(1, 0));
        Dataset auto = CommonUtil.newDataset(Arrays.asList(new DatasetRowBean("Tom", 10), new DatasetRowBean("Jerry", 12)));
        assertTrue(auto.columnNames().contains("name"));
        List<Object> firstNull = new ArrayList<>();
        firstNull.add(null);
        firstNull.add("something");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(firstNull));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(Arrays.asList((Object) "scalar")));
        Map<String, Integer> m = Collections.singletonMap("Alice", 95);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset(null, "Score", m));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset("", "Score", m));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset("Name", null, m));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newDataset("Name", "Name", m));
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("Alice", 95);
        scores.put("Bob", 87);
        Dataset kv = CommonUtil.newDataset("Name", "Score", scores);
        assertEquals("Alice", kv.get(0, kv.getColumnIndex("Name")));
        Map<String, List<Object>> ragged = new LinkedHashMap<>();
        ragged.put("Name", Arrays.<Object> asList("Alice", "Bob", "Charlie"));
        ragged.put("City", Arrays.<Object> asList("NYC", "LA"));
        Dataset padded = CommonUtil.newDataset(ragged);
        assertNull(padded.get(2, padded.getColumnIndex("City")));
        assertEquals("Charlie", padded.get(2, padded.getColumnIndex("Name")));
    }

    @Test
    public void testNewArray_sharedEmptyArrayAndRejectedComponentType() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newArray(null, 5));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newArray(Void.TYPE, 1));
        assertThrows(NegativeArraySizeException.class, () -> CommonUtil.newArray(String.class, -1));

        final String[] empty1 = CommonUtil.newArray(String.class, 0);
        final String[] empty2 = CommonUtil.newArray(String.class, 0);
        assertSame(empty1, empty2);
        assertEquals(0, empty1.length);

        final String[] fresh1 = CommonUtil.newArray(String.class, 1);
        final String[] fresh2 = CommonUtil.newArray(String.class, 1);
        assertNotSame(fresh1, fresh2);
    }

    @Test
    public void testNewCollections_negativeExpectedSizeIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newHashSet(-1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newLinkedHashSet(-1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newHashMap(-1));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.newLinkedHashMap(-1));

        assertEquals(0, CommonUtil.newHashSet(0).size());
        assertEquals(0, CommonUtil.newLinkedHashSet(0).size());
        assertEquals(0, CommonUtil.newHashMap(0).size());
        assertEquals(0, CommonUtil.newLinkedHashMap(0).size());
    }

}
