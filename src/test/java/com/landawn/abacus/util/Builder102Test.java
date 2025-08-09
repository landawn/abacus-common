package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

public class Builder102Test extends TestBase {

    @Test
    public void testOfBooleanList() {
        BooleanList list = new BooleanList();
        Builder.BooleanListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfCharList() {
        CharList list = new CharList();
        Builder.CharListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfByteList() {
        ByteList list = new ByteList();
        Builder.ByteListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfShortList() {
        ShortList list = new ShortList();
        Builder.ShortListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfIntList() {
        IntList list = new IntList();
        Builder.IntListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfLongList() {
        LongList list = new LongList();
        Builder.LongListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfFloatList() {
        FloatList list = new FloatList();
        Builder.FloatListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfDoubleList() {
        DoubleList list = new DoubleList();
        Builder.DoubleListBuilder builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfList() {
        List<String> list = new ArrayList<>();
        Builder.ListBuilder<String, List<String>> builder = Builder.of(list);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(list, builder.val());
    }

    @Test
    public void testOfCollection() {
        Set<String> set = new HashSet<>();
        Builder.CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(set, builder.val());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new HashMap<>();
        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(map, builder.val());
    }

    @Test
    public void testOfMultiset() {
        Multiset<String> multiset = new Multiset<>();
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(multiset, builder.val());
    }

    @Test
    public void testOfMultimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(multimap, builder.val());
    }

    @Test
    public void testOfDataSet() {
        DataSet dataSet = new RowDataSet(Arrays.asList("col1"), Arrays.asList(Arrays.asList("val1")));
        Builder.DataSetBuilder builder = Builder.of(dataSet);
        Assertions.assertNotNull(builder);
        Assertions.assertSame(dataSet, builder.val());
    }

    @Test
    public void testOfGeneric() {
        String str = "test";
        Builder<String> builder = Builder.of(str);
        Assertions.assertNotNull(builder);
        Assertions.assertEquals(str, builder.val());

        // Test with different types
        Integer num = 42;
        Builder<Integer> intBuilder = Builder.of(num);
        Assertions.assertEquals(num, intBuilder.val());

        // Test automatic type selection
        List<String> list = new ArrayList<>();
        Builder builder2 = Builder.of(list);
        Assertions.assertTrue(builder2 instanceof Builder.ListBuilder);
    }

    @Test
    public void testOfNullArgument() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> Builder.of((String) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Builder.of((List<?>) null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Builder.of((Map<?, ?>) null));
    }

    @Test
    public void testVal() {
        String value = "test";
        Builder<String> builder = Builder.of(value);
        Assertions.assertEquals(value, builder.val());
    }

    @Test
    public void testMap() {
        Builder<String> builder = Builder.of("test");
        Builder<Integer> mappedBuilder = builder.map(String::length);
        Assertions.assertEquals(Integer.valueOf(4), mappedBuilder.val());
    }

    @Test
    public void testFilter() {
        Builder<String> builder = Builder.of("test");
        Optional<String> result = builder.filter(s -> s.length() > 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals("test", result.get());

        Optional<String> emptyResult = builder.filter(s -> s.length() > 10);
        Assertions.assertFalse(emptyResult.isPresent());
    }

    @Test
    public void testAccept() {
        List<String> list = new ArrayList<>();
        Builder<List<String>> builder = Builder.of(list);

        Builder<List<String>> result = builder.accept(l -> l.add("test"));
        Assertions.assertSame(builder, result);
        Assertions.assertTrue(list.contains("test"));
    }

    @Test
    public void testApply() {
        Builder<String> builder = Builder.of("test");
        Integer length = builder.apply(String::length);
        Assertions.assertEquals(Integer.valueOf(4), length);
    }

    @Test
    public void testStream() {
        Builder<String> builder = Builder.of("test");
        Stream<String> stream = builder.stream();
        Assertions.assertNotNull(stream);
        Assertions.assertEquals(1, stream.count());
    }

    // BooleanListBuilder tests
    @Test
    public void testBooleanListBuilderSet() {
        BooleanList list = new BooleanList();
        list.add(true);
        Builder.BooleanListBuilder builder = Builder.of(list);

        builder.set(0, false);
        Assertions.assertFalse(list.get(0));
    }

    @Test
    public void testBooleanListBuilderAdd() {
        BooleanList list = new BooleanList();
        Builder.BooleanListBuilder builder = Builder.of(list);

        builder.add(true);
        Assertions.assertEquals(1, list.size());
        Assertions.assertTrue(list.get(0));
    }

    @Test
    public void testBooleanListBuilderAddAtIndex() {
        BooleanList list = new BooleanList();
        list.add(true);
        Builder.BooleanListBuilder builder = Builder.of(list);

        builder.add(0, false);
        Assertions.assertEquals(2, list.size());
        Assertions.assertFalse(list.get(0));
        Assertions.assertTrue(list.get(1));
    }

    @Test
    public void testBooleanListBuilderAddAll() {
        BooleanList list = new BooleanList();
        BooleanList toAdd = new BooleanList();
        toAdd.add(true);
        toAdd.add(false);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);

        Assertions.assertEquals(2, list.size());
        Assertions.assertTrue(list.get(0));
        Assertions.assertFalse(list.get(1));
    }

    @Test
    public void testBooleanListBuilderAddAllAtIndex() {
        BooleanList list = new BooleanList();
        list.add(true);
        BooleanList toAdd = new BooleanList();
        toAdd.add(false);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.addAll(0, toAdd);

        Assertions.assertEquals(2, list.size());
        Assertions.assertFalse(list.get(0));
        Assertions.assertTrue(list.get(1));
    }

    @Test
    public void testBooleanListBuilderRemove() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.remove(true);

        Assertions.assertEquals(1, list.size());
        Assertions.assertFalse(list.get(0));
    }

    @Test
    public void testBooleanListBuilderRemoveAll() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);
        list.add(true);

        BooleanList toRemove = new BooleanList();
        toRemove.add(true);

        Builder.BooleanListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);

        Assertions.assertEquals(1, list.size());
        Assertions.assertFalse(list.get(0));
    }

    // Similar pattern tests for other primitive list builders
    @Test
    public void testCharListBuilderOperations() {
        CharList list = new CharList();
        Builder.CharListBuilder builder = Builder.of(list);

        builder.add('a').add(0, 'b').set(0, 'c');
        Assertions.assertEquals('c', list.get(0));
        Assertions.assertEquals('a', list.get(1));

        CharList toAdd = new CharList();
        toAdd.add('d');
        builder.addAll(toAdd);
        Assertions.assertEquals(3, list.size());

        builder.remove('c');
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testByteListBuilderOperations() {
        ByteList list = new ByteList();
        Builder.ByteListBuilder builder = Builder.of(list);

        builder.add((byte) 1).add(0, (byte) 2);
        Assertions.assertEquals(2, list.size());

        builder.set(0, (byte) 3);
        Assertions.assertEquals((byte) 3, list.get(0));
    }

    @Test
    public void testShortListBuilderOperations() {
        ShortList list = new ShortList();
        Builder.ShortListBuilder builder = Builder.of(list);

        builder.add((short) 100).remove((short) 100);
        Assertions.assertEquals(0, list.size());
    }

    @Test
    public void testIntListBuilderOperations() {
        IntList list = new IntList();
        Builder.IntListBuilder builder = Builder.of(list);

        builder.add(42).add(0, 24);
        Assertions.assertEquals(24, list.get(0));
        Assertions.assertEquals(42, list.get(1));
    }

    @Test
    public void testLongListBuilderOperations() {
        LongList list = new LongList();
        Builder.LongListBuilder builder = Builder.of(list);

        builder.add(1000L).add(2000L);
        Assertions.assertEquals(2, list.size());
    }

    @Test
    public void testFloatListBuilderOperations() {
        FloatList list = new FloatList();
        Builder.FloatListBuilder builder = Builder.of(list);

        builder.add(1.5f).add(2.5f);
        builder.remove(1.5f);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(2.5f, list.get(0), 0.001f);
    }

    @Test
    public void testDoubleListBuilderOperations() {
        DoubleList list = new DoubleList();
        Builder.DoubleListBuilder builder = Builder.of(list);

        builder.add(3.14).add(2.71);
        DoubleList toRemove = new DoubleList();
        toRemove.add(3.14);
        builder.removeAll(toRemove);
        Assertions.assertEquals(1, list.size());
    }

    // ListBuilder tests
    @Test
    public void testListBuilderOperations() {
        List<String> list = new ArrayList<>();
        Builder.ListBuilder<String, List<String>> builder = Builder.of(list);

        builder.add("one").add(0, "zero").addAll(Arrays.asList("two", "three"));
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals("zero", list.get(0));

        builder.remove("zero");
        Assertions.assertEquals(3, list.size());

        builder.removeAll(Arrays.asList("one", "two"));
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("three", list.get(0));
    }

    @Test
    public void testListBuilderAddAllAtIndex() {
        List<String> list = new ArrayList<>();
        list.add("one");
        Builder.ListBuilder<String, List<String>> builder = Builder.of(list);

        builder.addAll(0, Arrays.asList("zero"));
        Assertions.assertEquals("zero", list.get(0));
        Assertions.assertEquals("one", list.get(1));
    }

    @Test
    public void testListBuilderRemoveAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Builder.ListBuilder<String, List<String>> builder = Builder.of(list);

        builder.remove(1);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("c", list.get(1));
    }

    // CollectionBuilder tests
    @Test
    public void testCollectionBuilderOperations() {
        Set<String> set = new HashSet<>();
        Builder.CollectionBuilder<String, Set<String>> builder = Builder.of(set);

        builder.add("one").addAll(Arrays.asList("two", "three"));
        Assertions.assertEquals(3, set.size());

        builder.remove("one");
        Assertions.assertEquals(2, set.size());

        builder.removeAll(Arrays.asList("two", "three"));
        Assertions.assertEquals(0, set.size());
    }

    @Test
    public void testCollectionBuilderAddAllVarargs() {
        Set<Integer> set = new LinkedHashSet<>();
        Builder.CollectionBuilder<Integer, Set<Integer>> builder = Builder.of(set);

        builder.addAll(1, 2, 3);
        Assertions.assertEquals(3, set.size());

        builder.removeAll(1, 2);
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains(3));
    }

    // MultisetBuilder tests
    @Test
    public void testMultisetBuilderSetCount() {
        Multiset<String> multiset = new Multiset<>();
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.setCount("test", 3);
        Assertions.assertEquals(3, multiset.getCount("test"));

        builder.setCount("test", 0);
        Assertions.assertEquals(0, multiset.getCount("test"));
    }

    @Test
    public void testMultisetBuilderAdd() {
        Multiset<String> multiset = new Multiset<>();
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.add("test");
        Assertions.assertEquals(1, multiset.getCount("test"));

        builder.add("test", 2);
        Assertions.assertEquals(3, multiset.getCount("test"));
    }

    @Test
    public void testMultisetBuilderRemove() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("test", 3);
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.remove("test");
        Assertions.assertEquals(2, multiset.getCount("test"));

        builder.remove("test", 2);
        Assertions.assertEquals(0, multiset.getCount("test"));
    }

    @Test
    public void testMultisetBuilderRemoveAllOccurrences() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("test", 5);
        multiset.add("other", 3);
        Builder.MultisetBuilder<String> builder = Builder.of(multiset);

        builder.removeAllOccurrences("test");
        Assertions.assertEquals(0, multiset.getCount("test"));
        Assertions.assertEquals(3, multiset.getCount("other"));

        builder.removeAllOccurrences(Arrays.asList("other"));
        Assertions.assertEquals(0, multiset.getCount("other"));
    }

    // MapBuilder tests
    @Test
    public void testMapBuilderPut() {
        Map<String, Integer> map = new HashMap<>();
        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);

        builder.put("one", 1).put("two", 2);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(Integer.valueOf(1), map.get("one"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testMapBuilderPutAll() {
        Map<String, Integer> map = new HashMap<>();
        Map<String, Integer> toAdd = new HashMap<>();
        toAdd.put("three", 3);
        toAdd.put("four", 4);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putAll(toAdd);

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(Integer.valueOf(3), map.get("three"));
    }

    @Test
    public void testMapBuilderPutIfAbsent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("one", 10);
        builder.putIfAbsent("two", 2);

        Assertions.assertEquals(Integer.valueOf(1), map.get("one"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testMapBuilderPutIfAbsentWithSupplier() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("one", () -> 10);
        builder.putIfAbsent("two", () -> 2);

        Assertions.assertEquals(Integer.valueOf(1), map.get("one"));
        Assertions.assertEquals(Integer.valueOf(2), map.get("two"));
    }

    @Test
    public void testMapBuilderRemove() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.remove("one");

        Assertions.assertEquals(1, map.size());
        Assertions.assertFalse(map.containsKey("one"));
    }

    @Test
    public void testMapBuilderRemoveAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);

        Builder.MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.removeAll(Arrays.asList("one", "two"));

        Assertions.assertEquals(1, map.size());
        Assertions.assertTrue(map.containsKey("three"));
    }

    // MultimapBuilder tests
    @Test
    public void testMultimapBuilderPut() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);

        builder.put("key", 1).put("key", 2);
        Assertions.assertEquals(2, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderPutMap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Map<String, Integer> map = new HashMap<>();
        map.put("key1", 1);
        map.put("key2", 2);

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put(map);

        Assertions.assertEquals(1, multimap.get("key1").size());
        Assertions.assertEquals(1, multimap.get("key2").size());
    }

    @Test
    public void testMultimapBuilderPutMany() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);

        builder.putMany("key", Arrays.asList(1, 2, 3));
        Assertions.assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderPutManyMap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Map<String, List<Integer>> map = new HashMap<>();
        map.put("key1", Arrays.asList(1, 2));
        map.put("key2", Arrays.asList(3, 4));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(map);

        Assertions.assertEquals(2, multimap.get("key1").size());
        Assertions.assertEquals(2, multimap.get("key2").size());
    }

    @Test
    public void testMultimapBuilderPutManyMultimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        ListMultimap<String, Integer> toAdd = N.newListMultimap();
        toAdd.putMany("key", Arrays.asList(1, 2, 3));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(toAdd);

        Assertions.assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderRemoveOne() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("key", 1);
        multimap.put("key", 2);

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne("key", 1);

        Assertions.assertEquals(1, multimap.get("key").size());
        Assertions.assertEquals(Integer.valueOf(2), multimap.get("key").get(0));
    }

    @Test
    public void testMultimapBuilderRemoveOneMap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key2", 2);

        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("key1", 1);

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne(toRemove);

        Assertions.assertFalse(multimap.containsKey("key1"));
        Assertions.assertEquals(1, multimap.get("key2").size());
    }

    @Test
    public void testMultimapBuilderRemoveAll() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.putMany("key", N.asList(1, 2, 3));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeAll("key");

        Assertions.assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testMultimapBuilderRemoveMany() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.putMany("key", Arrays.asList(1, 2, 3, 4));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany("key", Arrays.asList(1, 3));

        Assertions.assertEquals(2, multimap.get("key").size());
        Assertions.assertTrue(multimap.get("key").contains(2));
        Assertions.assertTrue(multimap.get("key").contains(4));
    }

    // DataSetBuilder tests would require more complex setup, showing a few examples
    @Test
    public void testDataSetBuilderRenameColumn() {
        DataSet ds = new RowDataSet(Arrays.asList("oldName"), Arrays.asList(Arrays.asList("value")));
        Builder.DataSetBuilder builder = Builder.of(ds);

        builder.renameColumn("oldName", "newName");
        Assertions.assertTrue(ds.columnNameList().contains("newName"));
        Assertions.assertFalse(ds.columnNameList().contains("oldName"));
    }

    @Test
    public void testDataSetBuilderAddColumn() {
        DataSet ds = new RowDataSet(N.asList("col1"), N.asList(Arrays.asList("val1")));
        Builder.DataSetBuilder builder = Builder.of(ds);

        builder.addColumn("col2", N.asList("val2"));
        Assertions.assertEquals(2, ds.columnCount());
        Assertions.assertTrue(ds.columnNameList().contains("col2"));
    }

    @Test
    public void testDataSetBuilderRemoveColumn() {
        DataSet ds = new RowDataSet(N.asList("col1", "col2"), N.asList(N.asList("val1"), N.asList("val2")));
        Builder.DataSetBuilder builder = Builder.of(ds);

        builder.removeColumn("col2");
        Assertions.assertEquals(1, ds.columnCount());
        Assertions.assertFalse(ds.columnNameList().contains("col2"));
    }

    // Comparison Builder tests
    @Test
    public void testCompare() {
        int result = Builder.compare("a", "b").result();
        Assertions.assertTrue(result < 0);

        result = Builder.compare("b", "a").result();
        Assertions.assertTrue(result > 0);

        result = Builder.compare("a", "a").result();
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testCompareWithComparator() {
        Comparator<String> reverseComparator = Comparator.reverseOrder();
        int result = Builder.compare("a", "b", reverseComparator).result();
        Assertions.assertTrue(result > 0);
    }

    @Test
    public void testCompareNullLess() {
        int result = Builder.compareNullLess(null, "a").result();
        Assertions.assertTrue(result < 0);

        result = Builder.compareNullLess("a", null).result();
        Assertions.assertTrue(result > 0);

        result = Builder.compareNullLess(null, null).result();
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testCompareNullBigger() {
        int result = Builder.compareNullBigger(null, "a").result();
        Assertions.assertTrue(result > 0);

        result = Builder.compareNullBigger("a", null).result();
        Assertions.assertTrue(result < 0);
    }

    @Test
    public void testCompareFalseLess() {
        int result = Builder.compareFalseLess(false, true).result();
        Assertions.assertTrue(result < 0);

        result = Builder.compareFalseLess(true, false).result();
        Assertions.assertTrue(result > 0);

        result = Builder.compareFalseLess(true, true).result();
        Assertions.assertEquals(0, result);
    }

    @Test
    public void testCompareTrueLess() {
        int result = Builder.compareTrueLess(true, false).result();
        Assertions.assertTrue(result < 0);

        result = Builder.compareTrueLess(false, true).result();
        Assertions.assertTrue(result > 0);
    }

    @Test
    public void testComparePrimitives() {
        Assertions.assertEquals(0, Builder.compare('a', 'a').result());
        Assertions.assertTrue(Builder.compare('a', 'b').result() < 0);

        Assertions.assertEquals(0, Builder.compare((byte) 1, (byte) 1).result());
        Assertions.assertTrue(Builder.compare((byte) 1, (byte) 2).result() < 0);

        Assertions.assertEquals(0, Builder.compare((short) 1, (short) 1).result());
        Assertions.assertTrue(Builder.compare((short) 1, (short) 2).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1, 1).result());
        Assertions.assertTrue(Builder.compare(1, 2).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1L, 1L).result());
        Assertions.assertTrue(Builder.compare(1L, 2L).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1.0f, 1.0f).result());
        Assertions.assertTrue(Builder.compare(1.0f, 2.0f).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1.0, 1.0).result());
        Assertions.assertTrue(Builder.compare(1.0, 2.0).result() < 0);
    }

    @Test
    public void testCompareWithTolerance() {
        Assertions.assertEquals(0, Builder.compare(1.0f, 1.001f, 0.01f).result());
        Assertions.assertTrue(Builder.compare(1.0f, 1.1f, 0.01f).result() < 0);

        Assertions.assertEquals(0, Builder.compare(1.0, 1.001, 0.01).result());
        Assertions.assertTrue(Builder.compare(1.0, 1.1, 0.01).result() < 0);
    }

    @Test
    public void testComparisonChain() {
        int result = Builder.compare("a", "a").compare(1, 2).result();
        Assertions.assertTrue(result < 0);

        result = Builder.compare("a", "b").compare(2, 1).result();
        Assertions.assertTrue(result < 0);
    }

    // EquivalenceBuilder tests
    @Test
    public void testEquals() {
        boolean result = Builder.equals("a", "a").result();
        Assertions.assertTrue(result);

        result = Builder.equals("a", "b").result();
        Assertions.assertFalse(result);

        result = Builder.equals(null, null).result();
        Assertions.assertTrue(result);

        result = Builder.equals("a", null).result();
        Assertions.assertFalse(result);
    }

    @Test
    public void testEqualsWithFunction() {
        BiPredicate<String, String> caseInsensitiveEquals = (a, b) -> a.equalsIgnoreCase(b);

        boolean result = Builder.equals("Hello", "hello", caseInsensitiveEquals).result();
        Assertions.assertTrue(result);
    }

    @Test
    public void testEqualsPrimitives() {
        Assertions.assertTrue(Builder.equals(true, true).result());
        Assertions.assertFalse(Builder.equals(true, false).result());

        Assertions.assertTrue(Builder.equals('a', 'a').result());
        Assertions.assertFalse(Builder.equals('a', 'b').result());

        Assertions.assertTrue(Builder.equals((byte) 1, (byte) 1).result());
        Assertions.assertFalse(Builder.equals((byte) 1, (byte) 2).result());

        Assertions.assertTrue(Builder.equals((short) 1, (short) 1).result());
        Assertions.assertFalse(Builder.equals((short) 1, (short) 2).result());

        Assertions.assertTrue(Builder.equals(1, 1).result());
        Assertions.assertFalse(Builder.equals(1, 2).result());

        Assertions.assertTrue(Builder.equals(1L, 1L).result());
        Assertions.assertFalse(Builder.equals(1L, 2L).result());

        Assertions.assertTrue(Builder.equals(1.0f, 1.0f).result());
        Assertions.assertFalse(Builder.equals(1.0f, 2.0f).result());

        Assertions.assertTrue(Builder.equals(1.0, 1.0).result());
        Assertions.assertFalse(Builder.equals(1.0, 2.0).result());
    }

    @Test
    public void testEqualsWithTolerance() {
        Assertions.assertTrue(Builder.equals(1.0f, 1.001f, 0.01f).result());
        Assertions.assertFalse(Builder.equals(1.0f, 1.1f, 0.01f).result());

        Assertions.assertTrue(Builder.equals(1.0, 1.001, 0.01).result());
        Assertions.assertFalse(Builder.equals(1.0, 1.1, 0.01).result());
    }

    @Test
    public void testEquivalenceChain() {
        boolean result = Builder.equals("a", "a").equals(1, 1).result();
        Assertions.assertTrue(result);

        result = Builder.equals("a", "a").equals(1, 2).result();
        Assertions.assertFalse(result);
    }

    // HashCodeBuilder tests
    @Test
    public void testHash() {
        int hash1 = Builder.hash("test").result();
        int hash2 = Builder.hash("test").result();
        Assertions.assertEquals(hash1, hash2);

        int hash3 = Builder.hash("different").result();
        Assertions.assertNotEquals(hash1, hash3);
    }

    @Test
    public void testHashWithFunction() {
        ToIntFunction<String> lengthHash = String::length;
        int hash1 = Builder.hash("test", lengthHash).result();
        int hash2 = Builder.hash("same", lengthHash).result();
        Assertions.assertEquals(hash1, hash2);
    }

    @Test
    public void testHashPrimitives() {
        int hash = Builder.hash(true).result();
        Assertions.assertNotEquals(0, hash);

        hash = Builder.hash('a').result();
        Assertions.assertNotEquals(0, hash);

        hash = Builder.hash((byte) 1).result();
        Assertions.assertNotEquals(0, hash);

        hash = Builder.hash((short) 1).result();
        Assertions.assertNotEquals(0, hash);

        hash = Builder.hash(1).result();
        Assertions.assertNotEquals(0, hash);

        hash = Builder.hash(1L).result();
        Assertions.assertNotEquals(0, hash);

        hash = Builder.hash(1.0f).result();
        Assertions.assertNotEquals(0, hash);

        hash = Builder.hash(1.0).result();
        Assertions.assertNotEquals(0, hash);
    }

    @Test
    public void testHashChain() {
        int hash1 = Builder.hash("test").hash(42).hash(true).result();

        int hash2 = Builder.hash("test").hash(42).hash(true).result();

        Assertions.assertEquals(hash1, hash2);

        int hash3 = Builder.hash("test").hash(42).hash(false).result();

        Assertions.assertNotEquals(hash1, hash3);
    }
}