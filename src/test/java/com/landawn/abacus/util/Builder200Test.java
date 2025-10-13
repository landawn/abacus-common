package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.BooleanListBuilder;
import com.landawn.abacus.util.Builder.ByteListBuilder;
import com.landawn.abacus.util.Builder.CharListBuilder;
import com.landawn.abacus.util.Builder.CollectionBuilder;
import com.landawn.abacus.util.Builder.DatasetBuilder;
import com.landawn.abacus.util.Builder.DoubleListBuilder;
import com.landawn.abacus.util.Builder.FloatListBuilder;
import com.landawn.abacus.util.Builder.IntListBuilder;
import com.landawn.abacus.util.Builder.ListBuilder;
import com.landawn.abacus.util.Builder.LongListBuilder;
import com.landawn.abacus.util.Builder.MapBuilder;
import com.landawn.abacus.util.Builder.MultimapBuilder;
import com.landawn.abacus.util.Builder.MultisetBuilder;
import com.landawn.abacus.util.Builder.ShortListBuilder;

@Tag("new-test")
public class Builder200Test extends TestBase {

    @Test
    public void testOfBooleanList() {
        BooleanList bl = BooleanList.of(true, false);
        BooleanListBuilder builder = Builder.of(bl);
        assertNotNull(builder);
        assertSame(bl, builder.val());
    }

    @Test
    public void testOfCharList() {
        CharList cl = CharList.of('a', 'b');
        CharListBuilder builder = Builder.of(cl);
        assertNotNull(builder);
        assertSame(cl, builder.val());
    }

    @Test
    public void testOfByteList() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2);
        ByteListBuilder builder = Builder.of(bl);
        assertNotNull(builder);
        assertSame(bl, builder.val());
    }

    @Test
    public void testOfShortList() {
        ShortList sl = ShortList.of((short) 1, (short) 2);
        ShortListBuilder builder = Builder.of(sl);
        assertNotNull(builder);
        assertSame(sl, builder.val());
    }

    @Test
    public void testOfIntList() {
        IntList il = IntList.of(1, 2);
        IntListBuilder builder = Builder.of(il);
        assertNotNull(builder);
        assertSame(il, builder.val());
    }

    @Test
    public void testOfLongList() {
        LongList ll = LongList.of(1L, 2L);
        LongListBuilder builder = Builder.of(ll);
        assertNotNull(builder);
        assertSame(ll, builder.val());
    }

    @Test
    public void testOfFloatList() {
        FloatList fl = FloatList.of(1.0f, 2.0f);
        FloatListBuilder builder = Builder.of(fl);
        assertNotNull(builder);
        assertSame(fl, builder.val());
    }

    @Test
    public void testOfDoubleList() {
        DoubleList dl = DoubleList.of(1.0, 2.0);
        DoubleListBuilder builder = Builder.of(dl);
        assertNotNull(builder);
        assertSame(dl, builder.val());
    }

    @Test
    public void testOfList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        assertNotNull(builder);
        assertSame(list, builder.val());
    }

    @Test
    public void testOfCollection() {
        Collection<String> coll = new HashSet<>(Arrays.asList("a", "b"));
        CollectionBuilder<String, Collection<String>> builder = Builder.of(coll);
        assertNotNull(builder);
        assertSame(coll, builder.val());
    }

    @Test
    public void testOfMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        assertNotNull(builder);
        assertSame(map, builder.val());
    }

    @Test
    public void testOfMultiset() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        assertNotNull(builder);
        assertSame(multiset, builder.val());
    }

    @Test
    public void testOfMultimap() {
        Multimap<String, Integer, List<Integer>> multimap = N.newListMultimap();
        multimap.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);
        assertNotNull(builder);
        assertSame(multimap, builder.val());
    }

    @Test
    public void testOfDataset() {
        Dataset dataset = new RowDataset(Arrays.asList("col1"), N.asList(new ArrayList<>()));
        DatasetBuilder builder = Builder.of(dataset);
        assertNotNull(builder);
        assertSame(dataset, builder.val());
    }

    @Test
    public void testGenericOf() {
        String str = "test";
        Builder<String> builder = Builder.of(str);
        assertEquals(str, builder.val());

        List<Integer> list = new ArrayList<>();
        Builder<List<Integer>> listBuilder = Builder.of(list);
        assertTrue(listBuilder instanceof ListBuilder);

        Map<String, String> map = new HashMap<>();
        Builder<Map<String, String>> mapBuilder = Builder.of(map);
        assertTrue(mapBuilder instanceof MapBuilder);
    }

    @Test
    public void testVal() {
        ListBuilder<String, List<String>> builder = Builder.of(N.asList("test"));
        assertEquals(N.asList("test"), builder.val());
    }

    @Test
    public void testAccept() {
        AtomicInteger counter = new AtomicInteger(0);
        ListBuilder<String, List<String>> builder = Builder.of(N.asList("test"));
        builder.accept(s -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testBooleanListBuilder() {
        BooleanList bl = BooleanList.of(true);
        BooleanListBuilder builder = Builder.of(bl);
        builder.set(0, false).add(true).add(0, true).addAll(BooleanList.of(false, false));
        assertEquals(BooleanList.of(true, false, true, false, false), bl);
        builder.addAll(1, BooleanList.of(true, true));
        assertEquals(BooleanList.of(true, true, true, false, true, false, false), bl);
        builder.remove(true);
        assertEquals(BooleanList.of(true, true, false, true, false, false), bl);
        builder.removeAll(BooleanList.of(false));
        assertEquals(BooleanList.of(true, true, true), bl);
    }

    @Test
    public void testCharListBuilder() {
        CharList cl = CharList.of('a');
        CharListBuilder builder = Builder.of(cl);
        builder.set(0, 'b').add('c').add(0, 'd');
        assertEquals(CharList.of('d', 'b', 'c'), cl);
        builder.addAll(CharList.of('e', 'f'));
        assertEquals(CharList.of('d', 'b', 'c', 'e', 'f'), cl);
        builder.addAll(1, CharList.of('x', 'y'));
        assertEquals(CharList.of('d', 'x', 'y', 'b', 'c', 'e', 'f'), cl);
        builder.remove('d');
        assertEquals(CharList.of('x', 'y', 'b', 'c', 'e', 'f'), cl);
        builder.removeAll(CharList.of('x', 'y'));
        assertEquals(CharList.of('b', 'c', 'e', 'f'), cl);
    }

    @Test
    public void testByteListBuilder() {
        ByteList bl = ByteList.of((byte) 1);
        ByteListBuilder builder = Builder.of(bl);
        builder.set(0, (byte) 2).add((byte) 3).add(0, (byte) 4);
        assertEquals(ByteList.of((byte) 4, (byte) 2, (byte) 3), bl);
        builder.addAll(ByteList.of((byte) 5, (byte) 6));
        assertEquals(ByteList.of((byte) 4, (byte) 2, (byte) 3, (byte) 5, (byte) 6), bl);
        builder.remove((byte) 2);
        assertEquals(ByteList.of((byte) 4, (byte) 3, (byte) 5, (byte) 6), bl);
        builder.removeAll(ByteList.of((byte) 5, (byte) 6));
        assertEquals(ByteList.of((byte) 4, (byte) 3), bl);
    }

    @Test
    public void testShortListBuilder() {
        ShortList sl = ShortList.of((short) 1);
        ShortListBuilder builder = Builder.of(sl);
        builder.set(0, (short) 2).add((short) 3).add(0, (short) 4);
        assertEquals(ShortList.of((short) 4, (short) 2, (short) 3), sl);
        builder.addAll(ShortList.of((short) 5, (short) 6));
        assertEquals(ShortList.of((short) 4, (short) 2, (short) 3, (short) 5, (short) 6), sl);
        builder.remove((short) 2);
        assertEquals(ShortList.of((short) 4, (short) 3, (short) 5, (short) 6), sl);
        builder.removeAll(ShortList.of((short) 5, (short) 6));
        assertEquals(ShortList.of((short) 4, (short) 3), sl);
    }

    @Test
    public void testIntListBuilder() {
        IntList il = IntList.of(1);
        IntListBuilder builder = Builder.of(il);
        builder.set(0, 2).add(3).add(0, 4);
        assertEquals(IntList.of(4, 2, 3), il);
        builder.addAll(IntList.of(5, 6));
        assertEquals(IntList.of(4, 2, 3, 5, 6), il);
        builder.remove(2);
        assertEquals(IntList.of(4, 3, 5, 6), il);
        builder.removeAll(IntList.of(5, 6));
        assertEquals(IntList.of(4, 3), il);
    }

    @Test
    public void testLongListBuilder() {
        LongList ll = LongList.of(1L);
        LongListBuilder builder = Builder.of(ll);
        builder.set(0, 2L).add(3L).add(0, 4L);
        assertEquals(LongList.of(4L, 2L, 3L), ll);
        builder.addAll(LongList.of(5L, 6L));
        assertEquals(LongList.of(4L, 2L, 3L, 5L, 6L), ll);
        builder.remove(2L);
        assertEquals(LongList.of(4L, 3L, 5L, 6L), ll);
        builder.removeAll(LongList.of(5L, 6L));
        assertEquals(LongList.of(4L, 3L), ll);
    }

    @Test
    public void testFloatListBuilder() {
        FloatList fl = FloatList.of(1.0f);
        FloatListBuilder builder = Builder.of(fl);
        builder.set(0, 2.0f).add(3.0f).add(0, 4.0f);
        assertEquals(FloatList.of(4.0f, 2.0f, 3.0f), fl);
        builder.addAll(FloatList.of(5.0f, 6.0f));
        assertEquals(FloatList.of(4.0f, 2.0f, 3.0f, 5.0f, 6.0f), fl);
        builder.remove(2.0f);
        assertEquals(FloatList.of(4.0f, 3.0f, 5.0f, 6.0f), fl);
        builder.removeAll(FloatList.of(5.0f, 6.0f));
        assertEquals(FloatList.of(4.0f, 3.0f), fl);
    }

    @Test
    public void testDoubleListBuilder() {
        DoubleList dl = DoubleList.of(1.0);
        DoubleListBuilder builder = Builder.of(dl);
        builder.set(0, 2.0).add(3.0).add(0, 4.0);
        assertEquals(DoubleList.of(4.0, 2.0, 3.0), dl);
        builder.addAll(DoubleList.of(5.0, 6.0));
        assertEquals(DoubleList.of(4.0, 2.0, 3.0, 5.0, 6.0), dl);
        builder.remove(2.0);
        assertEquals(DoubleList.of(4.0, 3.0, 5.0, 6.0), dl);
        builder.removeAll(DoubleList.of(5.0, 6.0));
        assertEquals(DoubleList.of(4.0, 3.0), dl);
    }

    @Test
    public void testCollectionBuilder() {
        Collection<String> coll = new ArrayList<>();
        CollectionBuilder<String, Collection<String>> builder = Builder.of(coll);
        builder.add("a").addAll(Arrays.asList("b", "c")).addAll("d", "e");
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), coll);
        builder.remove("a");
        assertEquals(Arrays.asList("b", "c", "d", "e"), coll);
        builder.removeAll(Arrays.asList("b", "c"));
        assertEquals(Arrays.asList("d", "e"), coll);
        builder.removeAll("d", "e");
        assertTrue(coll.isEmpty());
    }

    @Test
    public void testListBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.add("a").add(0, "b").addAll(Arrays.asList("c", "d")).addAll(0, Arrays.asList("e", "f"));
        assertEquals(Arrays.asList("e", "f", "b", "a", "c", "d"), list);
        builder.remove(0);
        assertEquals(Arrays.asList("f", "b", "a", "c", "d"), list);
    }

    @Test
    public void testMultisetBuilder() {
        Multiset<String> multiset = N.newMultiset();
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.add("a").add("a", 2).setCount("b", 3);
        assertEquals(3, multiset.count("a"));
        assertEquals(3, multiset.count("b"));
        builder.remove("a");
        assertEquals(2, multiset.count("a"));
        builder.remove("a", 2);
        assertEquals(0, multiset.count("a"));
        builder.removeAllOccurrences(Arrays.asList("b"));
        assertEquals(0, multiset.count("b"));
    }

    @Test
    public void testMultimapBuilder() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put("a", 1).put("a", 2).putMany("b", Arrays.asList(3, 4));
        assertEquals(Arrays.asList(1, 2), multimap.get("a"));
        assertEquals(Arrays.asList(3, 4), multimap.get("b"));
        builder.removeOne("a", 1);
        assertEquals(Arrays.asList(2), multimap.get("a"));
        builder.removeAll("b");
        assertFalse(multimap.containsKey("b"));
    }

    @Test
    public void testDatasetBuilder() {
        List<String> columnNames = new ArrayList<>(Arrays.asList("ID", "Name"));
        List<List<?>> columns = new ArrayList<>();
        columns.add(new ArrayList<>(Arrays.asList(1, 2)));
        columns.add(new ArrayList<>(Arrays.asList("John", "Jane")));
        Dataset dataset = Dataset.columns(columnNames, columns);
        DatasetBuilder builder = Builder.of(dataset);

        builder.renameColumn("Name", "FullName");
        assertTrue(dataset.columnNameList().contains("FullName"));
        assertFalse(dataset.columnNameList().contains("Name"));

        builder.addColumn("Age", Arrays.asList(30, 25));
        assertEquals(3, dataset.columnCount());
        assertEquals(Arrays.asList(30, 25), dataset.getColumn("Age"));

        builder.removeColumn("ID");
        assertEquals(2, dataset.columnCount());

        builder.updateColumn("Age", (Integer age) -> age + 1);
        assertEquals(Arrays.asList(31, 26), dataset.getColumn("Age"));
    }

    @Test
    public void testMapBuilder() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);

        builder.put("b", 2);
        assertEquals(2, map.size());

        Map<String, Integer> anotherMap = new HashMap<>();
        anotherMap.put("c", 3);
        builder.putAll(anotherMap);
        assertEquals(3, map.size());

        builder.putIfAbsent("a", 10);
        assertEquals(1, (int) map.get("a"));

        builder.putIfAbsent("d", 4);
        assertEquals(4, (int) map.get("d"));

        builder.putIfAbsent("e", () -> 5);
        assertEquals(5, (int) map.get("e"));

        builder.remove("a");
        assertFalse(map.containsKey("a"));

        builder.removeAll(Arrays.asList("b", "c"));
        assertFalse(map.isEmpty());
    }

    @Test
    public void testComparisonBuilder() {
        assertEquals(0, Builder.compare(1, 1).result());
        assertEquals(1, Builder.compare(2, 1).result());
        assertEquals(-1, Builder.compare(1, 2).result());

        assertEquals(-1, Builder.compare("a", "b").result());

        assertEquals(0, Builder.compare((String) null, (String) null, Comparators.naturalOrder()).result());
        assertEquals(-1, Builder.compare(null, "a", Comparators.naturalOrder()).result());

        assertEquals(-1, Builder.compare(1, 2).compare(3, 1).result());

        assertEquals(-1, Builder.compareNullLess(null, "a").result());
        assertEquals(1, Builder.compareNullBigger(null, "a").result());

        assertEquals(-1, Builder.compareFalseLess(false, true).result());
        assertEquals(1, Builder.compareTrueLess(false, true).result());
    }

    @Test
    public void testEquivalenceBuilder() {
        assertTrue(Builder.equals(1, 1).result());
        assertFalse(Builder.equals(1, 2).result());

        assertTrue(Builder.equals("a", "a").result());
        assertFalse(Builder.equals("a", "b").result());

        assertTrue(Builder.equals(null, null).result());
        assertFalse(Builder.equals(null, "a").result());

        assertFalse(Builder.equals(1, 2).equals(3, 3).result());

        assertTrue(Builder.equals(true, true).result());
    }

    @Test
    public void testHashCodeBuilder() {
        int expected = 31 * N.hashCode(1) + N.hashCode("a");
        assertEquals(expected, Builder.hash(1).hash("a").result());

        expected = 31 * (31 * N.hashCode(1) + N.hashCode("a")) + N.hashCode("b");
        assertEquals(expected, Builder.hash(1).hash("a").hash("b").result());

        ToIntFunction<String> lenFunc = String::length;
        expected = 31 * N.hashCode(1) + lenFunc.applyAsInt("abc");
        assertEquals(expected, Builder.hash(1).hash("abc", lenFunc).result());
    }
}
