package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.BooleanListBuilder;
import com.landawn.abacus.util.Builder.ByteListBuilder;
import com.landawn.abacus.util.Builder.CharListBuilder;
import com.landawn.abacus.util.Builder.CollectionBuilder;
import com.landawn.abacus.util.Builder.DoubleListBuilder;
import com.landawn.abacus.util.Builder.FloatListBuilder;
import com.landawn.abacus.util.Builder.HashCodeBuilder;
import com.landawn.abacus.util.Builder.IntListBuilder;
import com.landawn.abacus.util.Builder.ListBuilder;
import com.landawn.abacus.util.Builder.LongListBuilder;
import com.landawn.abacus.util.Builder.MapBuilder;
import com.landawn.abacus.util.Builder.MultimapBuilder;
import com.landawn.abacus.util.Builder.MultisetBuilder;
import com.landawn.abacus.util.Builder.ShortListBuilder;
import com.landawn.abacus.util.stream.Stream;

@Tag("new-test")
public class Builder100Test extends TestBase {

    private List<String> testList;
    private Map<String, Integer> testMap;
    private Set<String> testSet;

    @BeforeEach
    public void setUp() {
        testList = new ArrayList<>(Arrays.asList("a", "b", "c"));
        testMap = new HashMap<>();
        testMap.put("one", 1);
        testMap.put("two", 2);
        testSet = new HashSet<>(Arrays.asList("x", "y", "z"));
    }

    @Test
    public void testBuilderOf() {
        String value = "test";
        Builder<String> builder = Builder.of(value);

        assertNotNull(builder);
        assertEquals(value, builder.val());
    }

    @Test
    public void testBuilderOfNull() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((List<String>) null));
    }

    @Test
    public void testBuilderVal() {
        String value = "test";
        Builder<String> builder = Builder.of(value);

        assertEquals(value, builder.val());
    }

    @Test
    public void testBuilderMap() {
        Builder<String> builder = Builder.of("test");
        Builder<Integer> mapped = builder.map(String::length);

        assertEquals(Integer.valueOf(4), mapped.val());
    }

    @Test
    public void testBuilderFilter() {
        Builder<String> builder = Builder.of("test");

        u.Optional<String> present = builder.filter(s -> s.length() > 3);
        assertTrue(present.isPresent());
        assertEquals("test", present.get());

        u.Optional<String> absent = builder.filter(s -> s.length() > 10);
        assertFalse(absent.isPresent());
    }

    @Test
    public void testBuilderAccept() {
        List<String> result = new ArrayList<>();
        Builder<String> builder = Builder.of("test");

        Builder<String> same = builder.accept(result::add);

        assertSame(builder, same);
        assertEquals(Arrays.asList("test"), result);
    }

    @Test
    public void testBuilderApply() {
        Builder<String> builder = Builder.of("test");
        Integer length = builder.apply(String::length);

        assertEquals(Integer.valueOf(4), length);
    }

    @Test
    public void testBuilderStream() {
        Builder<String> builder = Builder.of("test");
        Stream<String> stream = builder.stream();

        assertEquals(1, stream.count());
    }

    @Test
    public void testBooleanListBuilder() {
        BooleanList boolList = new BooleanList();
        boolList.add(true);
        boolList.add(false);

        BooleanListBuilder builder = Builder.of(boolList);

        builder.set(0, false).add(true).add(1, false);

        assertEquals(4, boolList.size());
        assertFalse(boolList.get(0));
        assertFalse(boolList.get(1));
        assertFalse(boolList.get(2));
        assertTrue(boolList.get(3));
    }

    @Test
    public void testBooleanListBuilderAddAll() {
        BooleanList list1 = new BooleanList();
        list1.add(true);

        BooleanList list2 = new BooleanList();
        list2.add(false);
        list2.add(true);

        Builder.of(list1).addAll(list2);

        assertEquals(3, list1.size());
        assertTrue(list1.get(0));
        assertFalse(list1.get(1));
        assertTrue(list1.get(2));
    }

    @Test
    public void testBooleanListBuilderRemove() {
        BooleanList list = new BooleanList();
        list.add(true);
        list.add(false);
        list.add(true);

        Builder.of(list).remove(true);

        assertEquals(2, list.size());
        assertFalse(list.get(0));
        assertTrue(list.get(1));
    }

    @Test
    public void testCharListBuilder() {
        CharList charList = new CharList();
        charList.add('a');

        CharListBuilder builder = Builder.of(charList);

        builder.set(0, 'z').add('b').add(1, 'y');

        assertEquals(3, charList.size());
        assertEquals('z', charList.get(0));
        assertEquals('y', charList.get(1));
        assertEquals('b', charList.get(2));
    }

    @Test
    public void testByteListBuilder() {
        ByteList byteList = new ByteList();
        byteList.add((byte) 1);

        ByteListBuilder builder = Builder.of(byteList);

        builder.set(0, (byte) 10).add((byte) 20).removeAll(ByteList.of((byte) 10));

        assertEquals(1, byteList.size());
        assertEquals((byte) 20, byteList.get(0));
    }

    @Test
    public void testShortListBuilder() {
        ShortList shortList = new ShortList();
        shortList.add((short) 100);

        ShortListBuilder builder = Builder.of(shortList);

        builder.add((short) 200).add(0, (short) 50);

        assertEquals(3, shortList.size());
        assertEquals((short) 50, shortList.get(0));
        assertEquals((short) 100, shortList.get(1));
        assertEquals((short) 200, shortList.get(2));
    }

    @Test
    public void testIntListBuilder() {
        IntList intList = new IntList();
        intList.add(10);

        IntListBuilder builder = Builder.of(intList);

        builder.set(0, 20).add(30).add(1, 25).remove(20);

        assertEquals(2, intList.size());
        assertEquals(25, intList.get(0));
        assertEquals(30, intList.get(1));
    }

    @Test
    public void testLongListBuilder() {
        LongList longList = new LongList();
        longList.add(100L);

        LongListBuilder builder = Builder.of(longList);

        builder.add(200L).addAll(LongList.of(300L, 400L)).remove(100L);

        assertEquals(3, longList.size());
        assertEquals(200L, longList.get(0));
        assertEquals(300L, longList.get(1));
        assertEquals(400L, longList.get(2));
    }

    @Test
    public void testFloatListBuilder() {
        FloatList floatList = new FloatList();
        floatList.add(1.5f);

        FloatListBuilder builder = Builder.of(floatList);

        builder.set(0, 2.5f).add(3.5f).addAll(0, FloatList.of(0.5f));

        assertEquals(3, floatList.size());
        assertEquals(0.5f, floatList.get(0), 0.001f);
        assertEquals(2.5f, floatList.get(1), 0.001f);
        assertEquals(3.5f, floatList.get(2), 0.001f);
    }

    @Test
    public void testDoubleListBuilder() {
        DoubleList doubleList = new DoubleList();
        doubleList.add(1.5);

        DoubleListBuilder builder = Builder.of(doubleList);

        builder.set(0, 2.5).add(3.5).add(1, 2.0).removeAll(DoubleList.of(2.0));

        assertEquals(2, doubleList.size());
        assertEquals(2.5, doubleList.get(0), 0.001);
        assertEquals(3.5, doubleList.get(1), 0.001);
    }

    @Test
    public void testListBuilder() {
        ListBuilder<String, List<String>> builder = Builder.of(testList);

        builder.add("d").add(1, "a2").addAll(Arrays.asList("e", "f")).remove(0);

        assertEquals(Arrays.asList("a2", "b", "c", "d", "e", "f"), testList);
    }

    @Test
    public void testListBuilderAddAllAtIndex() {
        ListBuilder<String, List<String>> builder = Builder.of(testList);

        builder.addAll(1, Arrays.asList("x", "y"));

        assertEquals(Arrays.asList("a", "x", "y", "b", "c"), testList);
    }

    @Test
    public void testCollectionBuilder() {
        CollectionBuilder<String, Set<String>> builder = Builder.of(testSet);

        builder.add("w").addAll(Arrays.asList("u", "v")).remove("x").removeAll(Arrays.asList("y", "z"));

        assertEquals(new HashSet<>(Arrays.asList("w", "u", "v")), testSet);
    }

    @Test
    public void testCollectionBuilderVarargs() {
        CollectionBuilder<String, Set<String>> builder = Builder.of(testSet);

        builder.addAll("p", "q").removeAll("x", "y");

        assertTrue(testSet.contains("p"));
        assertTrue(testSet.contains("q"));
        assertFalse(testSet.contains("x"));
        assertFalse(testSet.contains("y"));
    }

    @Test
    public void testMultisetBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 2);

        MultisetBuilder<String> builder = Builder.of(multiset);

        builder.setCount("b", 3).add("a").add("c", 2).remove("a", 1).removeAllOccurrences("b");

        assertEquals(2, multiset.count("a"));
        assertEquals(0, multiset.count("b"));
        assertEquals(2, multiset.count("c"));
    }

    @Test
    public void testMapBuilder() {
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(testMap);

        builder.put("three", 3).putAll(Collections.singletonMap("four", 4)).remove("one").removeAll(Arrays.asList("two"));

        assertEquals(2, testMap.size());
        assertEquals(Integer.valueOf(3), testMap.get("three"));
        assertEquals(Integer.valueOf(4), testMap.get("four"));
        assertNull(testMap.get("one"));
        assertNull(testMap.get("two"));
    }

    @Test
    public void testMapBuilderPutIfAbsent() {
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(testMap);

        builder.putIfAbsent("one", 10).putIfAbsent("three", 3).putIfAbsent("four", () -> 4);

        assertEquals(Integer.valueOf(1), testMap.get("one"));
        assertEquals(Integer.valueOf(3), testMap.get("three"));
        assertEquals(Integer.valueOf(4), testMap.get("four"));
    }

    @Test
    public void testMultimapBuilder() {
        Multimap<String, String, List<String>> multimap = N.newListMultimap();
        multimap.put("key1", "value1");

        MultimapBuilder<String, String, List<String>, Multimap<String, String, List<String>>> builder = Builder.of(multimap);

        builder.put("key1", "value2").put(Collections.singletonMap("key2", "value3")).putMany("key3", Arrays.asList("v1", "v2")).removeOne("key1", "value1");

        assertEquals(Arrays.asList("value2"), multimap.get("key1"));
        assertEquals(Arrays.asList("value3"), multimap.get("key2"));
        assertEquals(Arrays.asList("v1", "v2"), multimap.get("key3"));
    }

    @Test
    public void testComparisonBuilder() {
        assertEquals(0, Builder.compare("a", "a").result());
        assertEquals(-1, Builder.compare("a", "b").result());
        assertEquals(1, Builder.compare("b", "a").result());

        assertEquals(0, Builder.compare(5, 5).result());
        assertEquals(-1, Builder.compare(5, 10).result());
        assertEquals(1, Builder.compare(10, 5).result());
    }

    @Test
    public void testComparisonBuilderChain() {
        int result = Builder.compare("a", "a").compare(5, 10).compare(true, false).result();

        assertEquals(-1, result);

        result = Builder.compare("b", "a").compare(5, 10).result();

        assertEquals(1, result);
    }

    @Test
    public void testComparisonBuilderPrimitives() {
        assertEquals(0, Builder.compare('a', 'a').result());
        assertEquals(0, Builder.compare((byte) 5, (byte) 5).result());
        assertEquals(0, Builder.compare((short) 10, (short) 10).result());
        assertEquals(0, Builder.compare(100L, 100L).result());
        assertEquals(0, Builder.compare(1.5f, 1.5f).result());
        assertEquals(0, Builder.compare(2.5, 2.5).result());

        assertEquals(-1, Builder.compareFalseLess(false, true).result());
        assertEquals(-1, Builder.compareTrueLess(true, false).result());
    }

    @Test
    public void testComparisonBuilderNullHandling() {
        assertEquals(-1, Builder.compareNullLess(null, "a").result());
        assertEquals(1, Builder.compareNullLess("a", null).result());
        assertEquals(0, Builder.compareNullLess(null, null).result());

        assertEquals(1, Builder.compareNullBigger(null, "a").result());
        assertEquals(-1, Builder.compareNullBigger("a", null).result());
        assertEquals(0, Builder.compareNullBigger(null, null).result());
    }

    @Test
    public void testEquivalenceBuilder() {
        assertTrue(Builder.equals("a", "a").result());
        assertFalse(Builder.equals("a", "b").result());

        assertTrue(Builder.equals(5, 5).result());
        assertFalse(Builder.equals(5, 10).result());
    }

    @Test
    public void testEquivalenceBuilderChain() {
        boolean result = Builder.equals("a", "a").equals(5, 5).equals(true, true).result();

        assertTrue(result);

        result = Builder.equals("a", "a").equals(5, 10).equals(true, true).result();

        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilderPrimitives() {
        assertTrue(Builder.equals(true, true).result());
        assertTrue(Builder.equals('a', 'a').result());
        assertTrue(Builder.equals((byte) 5, (byte) 5).result());
        assertTrue(Builder.equals((short) 10, (short) 10).result());
        assertTrue(Builder.equals(100, 100).result());
        assertTrue(Builder.equals(100L, 100L).result());
        assertTrue(Builder.equals(1.5f, 1.5f).result());
        assertTrue(Builder.equals(2.5, 2.5).result());
    }

    @Test
    public void testEquivalenceBuilderWithFunction() {
        BiPredicate<String, String> caseInsensitive = (s1, s2) -> s1.equalsIgnoreCase(s2);

        assertTrue(Builder.equals("Hello", "HELLO", caseInsensitive).result());
        assertFalse(Builder.equals("Hello", "World", caseInsensitive).result());
    }

    @Test
    public void testHashCodeBuilder() {
        int hash1 = Builder.hash("test").result();
        int hash2 = Builder.hash("test").result();

        assertEquals(hash1, hash2);

        int hash3 = Builder.hash("different").result();
        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testHashCodeBuilderChain() {
        int hash1 = Builder.hash("a").hash(5).hash(true).result();

        int hash2 = Builder.hash("a").hash(5).hash(true).result();

        assertEquals(hash1, hash2);

        int hash3 = Builder.hash("a").hash(5).hash(false).result();

        assertNotEquals(hash1, hash3);
    }

    @Test
    public void testHashCodeBuilderPrimitives() {
        HashCodeBuilder builder = Builder.hash(true).hash('a').hash((byte) 5).hash((short) 10).hash(100).hash(100L).hash(1.5f).hash(2.5);

        int result = builder.result();
        assertTrue(result != 0);
    }

    @Test
    public void testHashCodeBuilderWithFunction() {
        ToIntFunction<String> lengthHash = String::length;

        int hash1 = Builder.hash("test", lengthHash).result();
        int hash2 = Builder.hash("same", lengthHash).result();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testBuilderOfSpecificTypes() {
        ArrayList<String> arrayList = new ArrayList<>(Arrays.asList("a", "b"));
        Builder<List<String>> arrayListBuilder = Builder.of(arrayList);
        assertTrue(arrayListBuilder instanceof ListBuilder);

        LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("x", "y"));
        Builder<List<String>> linkedListBuilder = Builder.of(linkedList);
        assertTrue(linkedListBuilder instanceof ListBuilder);

        HashSet<String> hashSet = new HashSet<>(Arrays.asList("p", "q"));
        Builder<Collection<String>> hashSetBuilder = Builder.of(hashSet);
        assertTrue(hashSetBuilder instanceof CollectionBuilder);

        TreeMap<String, Integer> treeMap = new TreeMap<>();
        treeMap.put("one", 1);
        Builder<Map<String, Integer>> treeMapBuilder = Builder.of(treeMap);
        assertTrue(treeMapBuilder instanceof MapBuilder);
    }

    @Test
    public void testEmptyCollections() {
        List<String> emptyList = new ArrayList<>();
        ListBuilder<String, List<String>> listBuilder = Builder.of(emptyList);
        listBuilder.add("first");
        assertEquals(1, emptyList.size());

        Map<String, Integer> emptyMap = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> mapBuilder = Builder.of(emptyMap);
        mapBuilder.put("key", 1);
        assertEquals(1, emptyMap.size());

        Multiset<String> emptyMultiset = new Multiset<>();
        MultisetBuilder<String> multisetBuilder = Builder.of(emptyMultiset);
        multisetBuilder.add("item", 3);
        assertEquals(3, emptyMultiset.count("item"));
    }

    @Test
    public void testBuilderReuse() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ListBuilder<String, List<String>> builder = Builder.of(list);

        builder.add("c");
        assertEquals(3, list.size());

        builder.add("d");
        assertEquals(4, list.size());

        builder.remove("a");
        assertEquals(3, list.size());
    }
}
