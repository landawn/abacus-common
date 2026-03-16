package com.landawn.abacus.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.BooleanListBuilder;
import com.landawn.abacus.util.Builder.ByteListBuilder;
import com.landawn.abacus.util.Builder.CharListBuilder;
import com.landawn.abacus.util.Builder.CollectionBuilder;
import com.landawn.abacus.util.Builder.ComparisonBuilder;
import com.landawn.abacus.util.Builder.DatasetBuilder;
import com.landawn.abacus.util.Builder.DoubleListBuilder;
import com.landawn.abacus.util.Builder.EquivalenceBuilder;
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

@Tag("2025")
public class BuilderTest extends TestBase {

    private Dataset dataset;
    private DatasetBuilder builder;
    private Dataset testDataset;
    private Set<String> testSet;

    @BeforeEach
    public void setUp() {
        dataset = Dataset.rows(Arrays.asList("name", "age", "salary", "department"),
                Arrays.asList(Arrays.asList("John", 30, 50000.0, "IT"), Arrays.asList("Jane", 25, 45000.0, "HR"), Arrays.asList("Bob", 35, 60000.0, "Sales")));
        builder = Builder.of(dataset);

        testDataset = Dataset.rows(Arrays.asList("name", "age", "city"),
                Arrays.asList(Arrays.asList("John", 30, "New York"), Arrays.asList("Jane", 25, "Los Angeles"), Arrays.asList("Bob", 35, "Chicago")));
        testSet = new LinkedHashSet<>(Arrays.asList("x", "y"));
    }

    @Test
    public void test_001() {
        Map<String, Integer> m = Builder.of(new HashMap<String, Integer>()).put("ab", 1).put("abc", 1).val();
        N.println(m);

        List<Long> list = Builder.of(new ArrayList<Long>()).val();
        N.println(list);

        Multiset<String> m2 = Builder.of(new Multiset<String>()).add("abc").add("123").val();
        N.println(m2);

        Multimap<String, Integer, List<Integer>> m3 = Builder.of(new Multimap<String, Integer, List<Integer>>()).put("abc", 123).val();
        N.println(m3);

        IntList intList = Builder.of(IntList.of(1, 2, 3)).add(1).remove(2).add(5).removeAll(IntList.of(1)).val();
        N.println(intList);
        assertNotNull(intList);
    }

    @Test
    public void testBooleanListBuilder_of() {
        BooleanList list = BooleanList.of(true, false, true);
        BooleanListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testBooleanListBuilder_set() {
        BooleanList list = BooleanList.of(true, false);
        BooleanListBuilder builder = Builder.of(list);
        builder.set(0, false);
        assertFalse(builder.val().get(0));
    }

    @Test
    public void testBooleanListBuilder_add() {
        BooleanList list = BooleanList.of();
        BooleanListBuilder builder = Builder.of(list);
        builder.add(true).add(false);
        assertEquals(2, builder.val().size());
        assertTrue(builder.val().get(0));
    }

    @Test
    public void testBooleanListBuilder_addAtIndex() {
        BooleanList list = BooleanList.of(true, false);
        BooleanListBuilder builder = Builder.of(list);
        builder.add(1, true);
        assertEquals(3, builder.val().size());
        assertTrue(builder.val().get(1));
    }

    @Test
    public void testBooleanListBuilder_addAll() {
        BooleanList list = BooleanList.of(true);
        BooleanList toAdd = BooleanList.of(false, true);
        BooleanListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testBooleanListBuilder_addAllAtIndex() {
        BooleanList list = BooleanList.of(true, true);
        BooleanList toAdd = BooleanList.of(false);
        BooleanListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals(3, builder.val().size());
        assertFalse(builder.val().get(1));
    }

    @Test
    public void testBooleanListBuilder_remove() {
        BooleanList list = BooleanList.of(true, false, true);
        BooleanListBuilder builder = Builder.of(list);
        builder.remove(false);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testBooleanListBuilder_removeAll() {
        BooleanList list = BooleanList.of(true, false, true, false);
        BooleanList toRemove = BooleanList.of(false);
        BooleanListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testCharListBuilder_of() {
        CharList list = CharList.of('a', 'b', 'c');
        CharListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCharListBuilder_set() {
        CharList list = CharList.of('a', 'b');
        CharListBuilder builder = Builder.of(list);
        builder.set(0, 'z');
        assertEquals('z', builder.val().get(0));
    }

    @Test
    public void testCharListBuilder_add() {
        CharList list = CharList.of();
        CharListBuilder builder = Builder.of(list);
        builder.add('a').add('b');
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testCharListBuilder_addAtIndex() {
        CharList list = CharList.of('a', 'c');
        CharListBuilder builder = Builder.of(list);
        builder.add(1, 'b');
        assertEquals('b', builder.val().get(1));
    }

    @Test
    public void testCharListBuilder_addAll() {
        CharList list = CharList.of('a');
        CharList toAdd = CharList.of('b', 'c');
        CharListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCharListBuilder_addAllAtIndex() {
        CharList list = CharList.of('a', 'c');
        CharList toAdd = CharList.of('b');
        CharListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals('b', builder.val().get(1));
    }

    @Test
    public void testCharListBuilder_remove() {
        CharList list = CharList.of('a', 'b', 'c');
        CharListBuilder builder = Builder.of(list);
        builder.remove('b');
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testCharListBuilder_removeAll() {
        CharList list = CharList.of('a', 'b', 'c', 'b');
        CharList toRemove = CharList.of('b');
        CharListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testByteListBuilder_of() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        ByteListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testByteListBuilder_set() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        ByteListBuilder builder = Builder.of(list);
        builder.set(0, (byte) 10);
        assertEquals((byte) 10, builder.val().get(0));
    }

    @Test
    public void testByteListBuilder_add() {
        ByteList list = ByteList.of();
        ByteListBuilder builder = Builder.of(list);
        builder.add((byte) 1).add((byte) 2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testByteListBuilder_addAtIndex() {
        ByteList list = ByteList.of((byte) 1, (byte) 3);
        ByteListBuilder builder = Builder.of(list);
        builder.add(1, (byte) 2);
        assertEquals((byte) 2, builder.val().get(1));
    }

    @Test
    public void testByteListBuilder_addAll() {
        ByteList list = ByteList.of((byte) 1);
        ByteList toAdd = ByteList.of((byte) 2, (byte) 3);
        ByteListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testByteListBuilder_addAllAtIndex() {
        ByteList list = ByteList.of((byte) 1, (byte) 3);
        ByteList toAdd = ByteList.of((byte) 2);
        ByteListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals((byte) 2, builder.val().get(1));
    }

    @Test
    public void testByteListBuilder_remove() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteListBuilder builder = Builder.of(list);
        builder.remove((byte) 2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testByteListBuilder_removeAll() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        ByteList toRemove = ByteList.of((byte) 2);
        ByteListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testShortListBuilder_of() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        ShortListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testShortListBuilder_set() {
        ShortList list = ShortList.of((short) 1, (short) 2);
        ShortListBuilder builder = Builder.of(list);
        builder.set(0, (short) 10);
        assertEquals((short) 10, builder.val().get(0));
    }

    @Test
    public void testShortListBuilder_add() {
        ShortList list = ShortList.of();
        ShortListBuilder builder = Builder.of(list);
        builder.add((short) 1).add((short) 2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testShortListBuilder_addAtIndex() {
        ShortList list = ShortList.of((short) 1, (short) 3);
        ShortListBuilder builder = Builder.of(list);
        builder.add(1, (short) 2);
        assertEquals((short) 2, builder.val().get(1));
    }

    @Test
    public void testShortListBuilder_addAll() {
        ShortList list = ShortList.of((short) 1);
        ShortList toAdd = ShortList.of((short) 2, (short) 3);
        ShortListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testShortListBuilder_addAllAtIndex() {
        ShortList list = ShortList.of((short) 1, (short) 3);
        ShortList toAdd = ShortList.of((short) 2);
        ShortListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals((short) 2, builder.val().get(1));
    }

    @Test
    public void testShortListBuilder_remove() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3);
        ShortListBuilder builder = Builder.of(list);
        builder.remove((short) 2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testShortListBuilder_removeAll() {
        ShortList list = ShortList.of((short) 1, (short) 2, (short) 3, (short) 2);
        ShortList toRemove = ShortList.of((short) 2);
        ShortListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testIntListBuilder_of() {
        IntList list = IntList.of(1, 2, 3);
        IntListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testIntListBuilder_set() {
        IntList list = IntList.of(1, 2);
        IntListBuilder builder = Builder.of(list);
        builder.set(0, 10);
        assertEquals(10, builder.val().get(0));
    }

    @Test
    public void testIntListBuilder_add() {
        IntList list = IntList.of();
        IntListBuilder builder = Builder.of(list);
        builder.add(1).add(2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testIntListBuilder_addAtIndex() {
        IntList list = IntList.of(1, 3);
        IntListBuilder builder = Builder.of(list);
        builder.add(1, 2);
        assertEquals(2, builder.val().get(1));
    }

    @Test
    public void testIntListBuilder_addAll() {
        IntList list = IntList.of(1);
        IntList toAdd = IntList.of(2, 3);
        IntListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testIntListBuilder_addAllAtIndex() {
        IntList list = IntList.of(1, 3);
        IntList toAdd = IntList.of(2);
        IntListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals(2, builder.val().get(1));
    }

    @Test
    public void testIntListBuilder_remove() {
        IntList list = IntList.of(1, 2, 3);
        IntListBuilder builder = Builder.of(list);
        builder.remove(2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testIntListBuilder_removeAll() {
        IntList list = IntList.of(1, 2, 3, 2);
        IntList toRemove = IntList.of(2);
        IntListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testLongListBuilder_of() {
        LongList list = LongList.of(1L, 2L, 3L);
        LongListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testLongListBuilder_set() {
        LongList list = LongList.of(1L, 2L);
        LongListBuilder builder = Builder.of(list);
        builder.set(0, 10L);
        assertEquals(10L, builder.val().get(0));
    }

    @Test
    public void testLongListBuilder_add() {
        LongList list = LongList.of();
        LongListBuilder builder = Builder.of(list);
        builder.add(1L).add(2L);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testLongListBuilder_addAtIndex() {
        LongList list = LongList.of(1L, 3L);
        LongListBuilder builder = Builder.of(list);
        builder.add(1, 2L);
        assertEquals(2L, builder.val().get(1));
    }

    @Test
    public void testLongListBuilder_addAll() {
        LongList list = LongList.of(1L);
        LongList toAdd = LongList.of(2L, 3L);
        LongListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testLongListBuilder_addAllAtIndex() {
        LongList list = LongList.of(1L, 3L);
        LongList toAdd = LongList.of(2L);
        LongListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals(2L, builder.val().get(1));
    }

    @Test
    public void testLongListBuilder_remove() {
        LongList list = LongList.of(1L, 2L, 3L);
        LongListBuilder builder = Builder.of(list);
        builder.remove(2L);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testLongListBuilder_removeAll() {
        LongList list = LongList.of(1L, 2L, 3L, 2L);
        LongList toRemove = LongList.of(2L);
        LongListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testFloatListBuilder_of() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testFloatListBuilder_set() {
        FloatList list = FloatList.of(1.0f, 2.0f);
        FloatListBuilder builder = Builder.of(list);
        builder.set(0, 10.0f);
        assertEquals(10.0f, builder.val().get(0), 0.001f);
    }

    @Test
    public void testFloatListBuilder_add() {
        FloatList list = FloatList.of();
        FloatListBuilder builder = Builder.of(list);
        builder.add(1.0f).add(2.0f);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testFloatListBuilder_addAtIndex() {
        FloatList list = FloatList.of(1.0f, 3.0f);
        FloatListBuilder builder = Builder.of(list);
        builder.add(1, 2.0f);
        assertEquals(2.0f, builder.val().get(1), 0.001f);
    }

    @Test
    public void testFloatListBuilder_addAll() {
        FloatList list = FloatList.of(1.0f);
        FloatList toAdd = FloatList.of(2.0f, 3.0f);
        FloatListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testFloatListBuilder_addAllAtIndex() {
        FloatList list = FloatList.of(1.0f, 3.0f);
        FloatList toAdd = FloatList.of(2.0f);
        FloatListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals(2.0f, builder.val().get(1), 0.001f);
    }

    @Test
    public void testFloatListBuilder_remove() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f);
        FloatListBuilder builder = Builder.of(list);
        builder.remove(2.0f);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testFloatListBuilder_removeAll() {
        FloatList list = FloatList.of(1.0f, 2.0f, 3.0f, 2.0f);
        FloatList toRemove = FloatList.of(2.0f);
        FloatListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testDoubleListBuilder_of() {
        DoubleList list = DoubleList.of(1.0, 2.0, 3.0);
        DoubleListBuilder builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testDoubleListBuilder_set() {
        DoubleList list = DoubleList.of(1.0, 2.0);
        DoubleListBuilder builder = Builder.of(list);
        builder.set(0, 10.0);
        assertEquals(10.0, builder.val().get(0), 0.001);
    }

    @Test
    public void testDoubleListBuilder_add() {
        DoubleList list = DoubleList.of();
        DoubleListBuilder builder = Builder.of(list);
        builder.add(1.0).add(2.0);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testDoubleListBuilder_addAtIndex() {
        DoubleList list = DoubleList.of(1.0, 3.0);
        DoubleListBuilder builder = Builder.of(list);
        builder.add(1, 2.0);
        assertEquals(2.0, builder.val().get(1), 0.001);
    }

    @Test
    public void testDoubleListBuilder_addAll() {
        DoubleList list = DoubleList.of(1.0);
        DoubleList toAdd = DoubleList.of(2.0, 3.0);
        DoubleListBuilder builder = Builder.of(list);
        builder.addAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testDoubleListBuilder_addAllAtIndex() {
        DoubleList list = DoubleList.of(1.0, 3.0);
        DoubleList toAdd = DoubleList.of(2.0);
        DoubleListBuilder builder = Builder.of(list);
        builder.addAll(1, toAdd);
        assertEquals(2.0, builder.val().get(1), 0.001);
    }

    @Test
    public void testDoubleListBuilder_remove() {
        DoubleList list = DoubleList.of(1.0, 2.0, 3.0);
        DoubleListBuilder builder = Builder.of(list);
        builder.remove(2.0);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testDoubleListBuilder_removeAll() {
        DoubleList list = DoubleList.of(1.0, 2.0, 3.0, 2.0);
        DoubleList toRemove = DoubleList.of(2.0);
        DoubleListBuilder builder = Builder.of(list);
        builder.removeAll(toRemove);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_of() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testListBuilder_add() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.add("a").add("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_addAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.add(1, "b");
        assertEquals("b", builder.val().get(1));
    }

    @Test
    public void testListBuilder_addAll() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll(Arrays.asList("b", "c"));
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testListBuilder_addAllVarargs() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll("b", "c");
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testListBuilder_addAllAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll(1, Arrays.asList("b"));
        assertEquals("b", builder.val().get(1));
    }

    @Test
    public void testListBuilder_addAllAtIndex_emptyCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.addAll(1, new ArrayList<>());
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_remove() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.remove("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_removeAtIndex() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.remove(1);
        assertEquals(2, builder.val().size());
        assertEquals("c", builder.val().get(1));
    }

    @Test
    public void testListBuilder_removeAll() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "b"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.removeAll(Arrays.asList("b"));
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testListBuilder_removeAllVarargs() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "b"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.removeAll("b", "c");
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_of() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        assertNotNull(builder);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_add() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.add("a").add("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_addAll() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.addAll(Arrays.asList("b", "c"));
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_addAllVarargs() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.addAll("b", "c");
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_remove() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.remove("b");
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_removeAll() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.removeAll(Arrays.asList("b", "c"));
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testCollectionBuilder_removeAllVarargs() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.removeAll("b", "c");
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMultisetBuilder_of() {
        Multiset<String> multiset = Multiset.of("a", "b", "a");
        MultisetBuilder<String> builder = Builder.of(multiset);
        assertNotNull(builder);
        assertEquals(2, (Integer) builder.val().get("a"));
    }

    @Test
    public void testMultisetBuilder_setCount() {
        Multiset<String> multiset = Multiset.of("a");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.setCount("a", 5);
        assertEquals(5, (Integer) builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_add() {
        Multiset<String> multiset = Multiset.of();
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.add("a").add("a");
        assertEquals(2, (Integer) builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_addWithOccurrences() {
        Multiset<String> multiset = Multiset.of();
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.add("a", 3);
        assertEquals(3, (Integer) builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_remove() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.remove("a");
        assertEquals(1, builder.val().getCount("a"));
    }

    @Test
    public void testMultisetBuilder_removeWithOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "a");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.remove("a", 2);
        assertEquals(1, builder.val().get("a"));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrences() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.removeAllOccurrences("a");
        assertEquals(0, builder.val().get("a"));
    }

    @Test
    public void testMultisetBuilder_removeAll() {
        Multiset<String> multiset = Multiset.of("a", "a", "b");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.removeAll(Arrays.asList("a"));
        assertEquals(0, builder.val().get("a"));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrencesCollection() {
        Multiset<String> multiset = Multiset.of("a", "a", "b", "c");
        MultisetBuilder<String> builder = Builder.of(multiset);
        builder.removeAllOccurrences(Arrays.asList("a", "b"));
        assertEquals(0, builder.val().get("a"));
        assertEquals(0, builder.val().get("b"));
    }

    @Test
    public void testMapBuilder_of() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        assertNotNull(builder);
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMapBuilder_put() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.put("a", 1).put("b", 2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testMapBuilder_putAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> toAdd = new HashMap<>();
        toAdd.put("b", 2);
        toAdd.put("c", 3);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putAll(toAdd);
        assertEquals(3, builder.val().size());
    }

    @Test
    public void testMapBuilder_putIfAbsent() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("a", 2);
        builder.putIfAbsent("b", 2);
        assertEquals(1, builder.val().get("a"));
        assertEquals(2, builder.val().get("b"));
    }

    @Test
    public void testMapBuilder_putIfAbsentSupplier() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        Supplier<Integer> supplier = () -> 5;
        builder.putIfAbsent("a", supplier);
        assertEquals(5, builder.val().get("a"));
    }

    @Test
    public void testMapBuilder_remove() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.remove("a");
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMapBuilder_removeAll() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.removeAll(Arrays.asList("a", "b"));
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMultimapBuilder_of() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        assertNotNull(builder);
        assertEquals(1, builder.val().totalValueCount());
    }

    @Test
    public void testMultimapBuilder_put() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put("a", 1).put("a", 2);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put(map);
        assertEquals(2, builder.val().totalValueCount());
    }

    @Test
    public void testMultimapBuilder_putMany() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany("a", Arrays.asList(1, 2, 3));
        assertEquals(3, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putManyMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(map);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putManyMultimap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> other = CommonUtil.newListMultimap();
        other.put("a", 1);
        other.put("a", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(other);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeOne() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne("a", 1);
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeOneMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("b", 2);
        Map<String, Integer> toRemove = new HashMap<>();
        toRemove.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne(toRemove);
        assertNull(builder.val().get("a"));
    }

    @Test
    public void testMultimapBuilder_removeAll() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeAll("a");
        assertFalse(builder.val().containsKey("a"));
    }

    @Test
    public void testMultimapBuilder_removeMany() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("a", 3);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany("a", Arrays.asList(1, 2));
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeManyMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        Map<String, Collection<Integer>> toRemove = new HashMap<>();
        toRemove.put("a", Arrays.asList(1, 2));
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany(toRemove);
        assertNull(builder.val().get("a"));
    }

    @Test
    public void testMultimapBuilder_removeManyMultimap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        ListMultimap<String, Integer> toRemove = CommonUtil.newListMultimap();
        toRemove.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany(toRemove);
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testDatasetBuilder_of() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        assertNotNull(builder);
        assertEquals(2, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_renameColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumn("col1", "newCol1");
        assertTrue(builder.val().columnNames().contains("newCol1"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsMap() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        Map<String, String> renames = new HashMap<>();
        renames.put("col1", "newCol1");
        renames.put("col2", "newCol2");
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(renames);
        assertTrue(builder.val().columnNames().contains("newCol1"));
        assertTrue(builder.val().columnNames().contains("newCol2"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(name -> "prefix_" + name);
        assertTrue(builder.val().columnNames().contains("prefix_col1"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsCollectionFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(Arrays.asList("col1"), name -> name.toUpperCase());
        assertTrue(builder.val().columnNames().contains("COL1"));
        assertTrue(builder.val().columnNames().contains("col2"));
    }

    @Test
    public void testDatasetBuilder_addColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("col2", Arrays.asList(3, 4));
        assertEquals(2, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumnAtIndex() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "col0", Arrays.asList(0, 0));
        assertEquals("col0", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_removeColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumn("col2");
        assertEquals(1, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_removeColumnsCollection() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2", "col3"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumns(Arrays.asList("col2", "col3"));
        assertEquals(1, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_removeColumnsPredicate() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "temp1", "temp2"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumns(name -> name.startsWith("temp"));
        assertEquals(1, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_prepend() {
        Dataset dataset1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset dataset2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset1);
        builder.prepend(dataset2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testDatasetBuilder_append() {
        Dataset dataset1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset dataset2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset1);
        builder.append(dataset2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testBuilder_ofGeneric() {
        String value = "test";
        Builder<String> builder = Builder.of(value);
        assertNotNull(builder);
        assertEquals(value, builder.val());
    }

    @Test
    public void testBuilder_ofNull() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((String) null));
    }

    @Test
    public void testBuilder_val() {
        String value = "test";
        Builder<String> builder = Builder.of(value);
        assertEquals(value, builder.val());
    }

    @Test
    public void testBuilder_accept() {
        List<String> list = new ArrayList<>();
        Builder<List<String>> builder = Builder.of(list);
        builder.accept(l -> l.add("test"));
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testBuilder_apply() {
        Builder<String> builder = Builder.of("test");
        int length = builder.apply(String::length);
        assertEquals(4, length);
    }

    @Test
    public void testBuilder_stream() {
        Builder<String> builder = Builder.of("test");
        assertEquals(1, builder.stream().count());
    }

    @Test
    public void testCompare_comparable() {
        int result = Builder.compare("a", "b").result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_comparableEqual() {
        int result = Builder.compare("a", "a").result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_withComparator() {
        int result = Builder.compare("a", "B", String.CASE_INSENSITIVE_ORDER).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareNullLess() {
        int result = Builder.compareNullLess(null, "value").result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareNullLess_bothNull() {
        int result = Builder.compareNullLess(null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompareNullBigger() {
        int result = Builder.compareNullBigger(null, "value").result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompareNullBigger_bothNull() {
        int result = Builder.compareNullBigger(null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompareFalseLess() {
        int result = Builder.compareFalseLess(false, true).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareFalseLess_equal() {
        int result = Builder.compareFalseLess(true, true).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompareTrueLess() {
        int result = Builder.compareTrueLess(true, false).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompareTrueLess_equal() {
        int result = Builder.compareTrueLess(false, false).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_char() {
        int result = Builder.compare('a', 'b').result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_byte() {
        int result = Builder.compare((byte) 1, (byte) 2).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_short() {
        int result = Builder.compare((short) 1, (short) 2).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_int() {
        int result = Builder.compare(1, 2).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_long() {
        int result = Builder.compare(1L, 2L).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_float() {
        int result = Builder.compare(1.0f, 2.0f).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_floatWithTolerance() {
        int result = Builder.compare(1.0001f, 1.0002f, 0.001f).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_double() {
        int result = Builder.compare(1.0, 2.0).result();
        assertTrue(result < 0);
    }

    @Test
    public void testCompare_doubleWithTolerance() {
        int result = Builder.compare(1.00001, 1.00002, 0.0001).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_chaining() {
        int result = Builder.compare("a", "a").compare(1, 2).result();
        assertTrue(result < 0);
    }

    @Test
    public void testComparisonBuilder_shortCircuit() {
        int result = Builder.compare("a", "b").compare(2, 1).result();
        assertTrue(result < 0);
    }

    @Test
    public void testEquals_objects() {
        boolean result = Builder.equals("test", "test").result();
        assertTrue(result);
    }

    @Test
    public void testEquals_objectsNotEqual() {
        boolean result = Builder.equals("test", "other").result();
        assertFalse(result);
    }

    @Test
    public void testEquals_objectsWithNull() {
        boolean result = Builder.equals(null, null).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_withPredicate() {
        boolean result = Builder.equals("test", "TEST", (a, b) -> a.equalsIgnoreCase(b)).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_boolean() {
        boolean result = Builder.equals(true, true).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_booleanNotEqual() {
        boolean result = Builder.equals(true, false).result();
        assertFalse(result);
    }

    @Test
    public void testEquals_char() {
        boolean result = Builder.equals('a', 'a').result();
        assertTrue(result);
    }

    @Test
    public void testEquals_byte() {
        boolean result = Builder.equals((byte) 1, (byte) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_short() {
        boolean result = Builder.equals((short) 1, (short) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_int() {
        boolean result = Builder.equals(1, 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_long() {
        boolean result = Builder.equals(1L, 1L).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_float() {
        boolean result = Builder.equals(1.0f, 1.0f).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_floatWithTolerance() {
        boolean result = Builder.equals(1.0001f, 1.0002f, 0.001f).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_double() {
        boolean result = Builder.equals(1.0, 1.0).result();
        assertTrue(result);
    }

    @Test
    public void testEquals_doubleWithTolerance() {
        boolean result = Builder.equals(1.00001, 1.00002, 0.0001).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_chaining() {
        boolean result = Builder.equals("a", "a").equals(1, 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_shortCircuit() {
        boolean result = Builder.equals("a", "b").equals(1, 1).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_isEquals() {
        boolean result = Builder.equals("test", "test").isEquals();
        assertTrue(result);
    }

    @Test
    public void testHash_object() {
        int hash = Builder.hash("test").result();
        assertEquals("test".hashCode(), hash);
    }

    @Test
    public void testHash_objectWithNull() {
        int hash = Builder.hash((Object) null).result();
        assertEquals(0, hash);
    }

    @Test
    public void testHash_withFunction() {
        int hash = Builder.hash("test", String::length).result();
        assertEquals(4, hash);
    }

    @Test
    public void testHash_boolean() {
        int hash = Builder.hash(true).result();
        assertEquals(1231, hash);
    }

    @Test
    public void testHash_booleanFalse() {
        int hash = Builder.hash(false).result();
        assertEquals(1237, hash);
    }

    @Test
    public void testHash_char() {
        int hash = Builder.hash('a').result();
        assertEquals('a', hash);
    }

    @Test
    public void testHash_byte() {
        int hash = Builder.hash((byte) 1).result();
        assertEquals(1, hash);
    }

    @Test
    public void testHash_short() {
        int hash = Builder.hash((short) 1).result();
        assertEquals(1, hash);
    }

    @Test
    public void testHash_int() {
        int hash = Builder.hash(1).result();
        assertEquals(1, hash);
    }

    @Test
    public void testHash_long() {
        long value = 1L;
        int expectedHash = (int) (value ^ (value >>> 32));
        int hash = Builder.hash(value).result();
        assertEquals(expectedHash, hash);
    }

    @Test
    public void testHash_float() {
        float value = 1.0f;
        int expectedHash = Float.floatToIntBits(value);
        int hash = Builder.hash(value).result();
        assertEquals(expectedHash, hash);
    }

    @Test
    public void testHash_double() {
        double value = 1.0;
        long bits = Double.doubleToLongBits(value);
        int expectedHash = (int) (bits ^ (bits >>> 32));
        int hash = Builder.hash(value).result();
        assertEquals(expectedHash, hash);
    }

    @Test
    public void testHashCodeBuilder_chaining() {
        int hash = Builder.hash("test").hash(1).hash(true).result();
        assertNotNull(hash);
    }

    @Test
    public void testHashCodeBuilder_multipleValues() {
        int hash1 = Builder.hash("a").hash(1).result();
        int hash2 = Builder.hash("b").hash(1).result();
        assertTrue(hash1 != hash2);
    }

    @Test
    public void testBuilder_ofList_returnsListBuilder() {
        List<String> list = new ArrayList<>();
        Builder<?> builder = Builder.of(list);
        assertTrue(builder instanceof ListBuilder);
    }

    @Test
    public void testBuilder_ofLinkedList_returnsListBuilder() {
        LinkedList<String> list = new LinkedList<>();
        Builder<?> builder = Builder.of(list);
        assertTrue(builder instanceof ListBuilder);
    }

    @Test
    public void testBuilder_ofSet_returnsCollectionBuilder() {
        Set<String> set = new HashSet<>();
        Builder<?> builder = Builder.of(set);
        assertTrue(builder instanceof CollectionBuilder);
    }

    @Test
    public void testBuilder_ofMap_returnsMapBuilder() {
        Map<String, Integer> map = new HashMap<>();
        Builder<?> builder = Builder.of(map);
        assertTrue(builder instanceof MapBuilder);
    }

    @Test
    public void testBuilder_ofTreeMap_returnsMapBuilder() {
        Map<String, Integer> map = new TreeMap<>();
        Builder<?> builder = Builder.of(map);
        assertTrue(builder instanceof MapBuilder);
    }

    @Test
    public void testBuilder_ofMultiset_returnsMultisetBuilder() {
        Multiset<String> multiset = Multiset.of();
        Builder<?> builder = Builder.of(multiset);
        assertTrue(builder instanceof MultisetBuilder);
    }

    @Test
    public void testBuilder_ofListMultimap_returnsMultimapBuilder() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Builder<?> builder = Builder.of(multimap);
        assertTrue(builder instanceof MultimapBuilder);
    }

    @Test
    public void testBuilder_ofDataset_returnsDatasetBuilder() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Builder<?> builder = Builder.of(dataset);
        assertTrue(builder instanceof DatasetBuilder);
    }

    @Test
    public void testBuilder_ofIntList_returnsIntListBuilder() {
        IntList list = IntList.of(1, 2, 3);
        Builder<?> builder = Builder.of(list);
        assertTrue(builder instanceof IntListBuilder);
    }

    @Test
    public void testBuilder_ofLongList_returnsLongListBuilder() {
        LongList list = LongList.of(1L, 2L, 3L);
        Builder<?> builder = Builder.of(list);
        assertTrue(builder instanceof LongListBuilder);
    }

    @Test
    public void testBuilder_ofDoubleList_returnsDoubleListBuilder() {
        DoubleList list = DoubleList.of(1.0, 2.0, 3.0);
        Builder<?> builder = Builder.of(list);
        assertTrue(builder instanceof DoubleListBuilder);
    }

    //
    //
    //    @Test
    //    public void testBuilder_filter_notPresent() {
    //        Builder<String> builder = Builder.of("hi");
    //        u.Optional<String> filtered = builder.filter(s -> s.length() > 3);
    //        assertFalse(filtered.isPresent());
    //    }

    //
    //
    //

    @Test
    public void testDatasetBuilder_addColumn_withFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("col2", "col1", (Integer val) -> val * 2);
        assertEquals(2, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "col0", "col1", (Integer val) -> val * 10);
        assertEquals("col0", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withMultiColumnFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("sum", Arrays.asList("col1", "col2"), arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertEquals(3, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withMultiColumnFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "sum", Arrays.asList("col1", "col2"), arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertEquals("sum", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withBiFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("product", Tuple.of("col1", "col2"), (Integer a, Integer b) -> a * b);
        assertEquals(3, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withBiFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "product", Tuple.of("col1", "col2"), (Integer a, Integer b) -> a * b);
        assertEquals("product", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withTriFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("sum", Tuple.of("a", "b", "c"), (Integer x, Integer y, Integer z) -> x + y + z);
        assertEquals(4, builder.val().columnNames().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withTriFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "sum", Tuple.of("a", "b", "c"), (Integer x, Integer y, Integer z) -> x + y + z);
        assertEquals("sum", builder.val().columnNames().get(0));
    }

    @Test
    public void testDatasetBuilder_updateColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateColumn("col1", (Integer val) -> val * 2);
        assertEquals(2, (Integer) builder.val().moveToRow(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_updateColumns() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateColumns(Arrays.asList("col1", "col2"), (rowIdx, colName, val) -> ((Integer) val) * 10);
        assertEquals(10, (Integer) builder.val().moveToRow(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_convertColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList("123")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.convertColumn("col1", Integer.class);
        assertTrue(builder.val().moveToRow(0).get("col1") instanceof Integer);
    }

    @Test
    public void testDatasetBuilder_convertColumns() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList("123", "456")));
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("col1", Integer.class);
        conversions.put("col2", Integer.class);
        DatasetBuilder builder = Builder.of(dataset);
        builder.convertColumns(conversions);
        assertTrue(builder.val().moveToRow(0).get("col1") instanceof Integer);
    }

    @Test
    public void testDatasetBuilder_combineColumns() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Arrays.asList("a", "b"), "sum", arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertTrue(builder.val().columnNames().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_combineColumns_withBiFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Tuple.of("a", "b"), "sum", (Integer x, Integer y) -> x + y);
        assertTrue(builder.val().columnNames().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_combineColumns_withTriFunction() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Tuple.of("a", "b", "c"), "sum", (Integer x, Integer y, Integer z) -> x + y + z);
        assertTrue(builder.val().columnNames().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_divideColumn() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Arrays.asList("col1", "col2"), (String val) -> Arrays.asList(val.split(",")));
        assertTrue(builder.val().columnNames().contains("col1"));
        assertTrue(builder.val().columnNames().contains("col2"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withBiConsumer() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Arrays.asList("col1", "col2"), (String val, Object[] output) -> {
            String[] parts = val.split(",");
            output[0] = parts[0];
            output[1] = parts[1];
        });
        assertTrue(builder.val().columnNames().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withPair() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Tuple.of("col1", "col2"), (String val, Pair<Object, Object> output) -> {
            String[] parts = val.split(",");
            output.setLeft(parts[0]);
            output.setRight(parts[1]);
        });
        assertTrue(builder.val().columnNames().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withTriple() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b,c")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Tuple.of("col1", "col2", "col3"), (String val, Triple<Object, Object, Object> output) -> {
            String[] parts = val.split(",");
            output.setLeft(parts[0]);
            output.setMiddle(parts[1]);
            output.setRight(parts[2]);
        });
        assertTrue(builder.val().columnNames().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_updateAll() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateAll((Integer val) -> val * 2);
        assertEquals(2, (Integer) builder.val().moveToRow(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_updateAll_withRowIndexAndColumnName() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateAll((rowIdx, colName, val) -> ((Integer) val) + rowIdx);
        assertEquals(1, (Integer) builder.val().moveToRow(0).get("col1"));
        assertEquals(3, (Integer) builder.val().moveToRow(1).get("col1"));
    }

    @Test
    public void testDatasetBuilder_replaceIf() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.replaceIf((Integer val) -> val == 2, 0);
        assertEquals(0, (Integer) builder.val().moveToRow(1).get("col1"));
    }

    @Test
    public void testDatasetBuilder_replaceIf_withPredicate() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.replaceIf((rowIdx, colName, val) -> ((Integer) val) > 1, 99);
        assertEquals(99, (Integer) builder.val().moveToRow(1).get("col1"));
    }

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
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newListMultimap();
        multimap.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);
        assertNotNull(builder);
        assertSame(multimap, builder.val());
    }

    @Test
    public void testOfDataset() {
        Dataset dataset = new RowDataset(Arrays.asList("col1"), CommonUtil.toList(new ArrayList<>()));
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
        ListBuilder<String, List<String>> builder = Builder.of(CommonUtil.toList("test"));
        assertEquals(CommonUtil.toList("test"), builder.val());
    }

    @Test
    public void testAccept() {
        AtomicInteger counter = new AtomicInteger(0);
        ListBuilder<String, List<String>> builder = Builder.of(CommonUtil.toList("test"));
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
        Multiset<String> multiset = CommonUtil.newMultiset();
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
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
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
        assertTrue(dataset.columnNames().contains("FullName"));
        assertFalse(dataset.columnNames().contains("Name"));

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
        int expected = 31 * CommonUtil.hashCode(1) + CommonUtil.hashCode("a");
        assertEquals(expected, Builder.hash(1).hash("a").result());

        expected = 31 * (31 * CommonUtil.hashCode(1) + CommonUtil.hashCode("a")) + CommonUtil.hashCode("b");
        assertEquals(expected, Builder.hash(1).hash("a").hash("b").result());

        ToIntFunction<String> lenFunc = String::length;
        expected = 31 * CommonUtil.hashCode(1) + lenFunc.applyAsInt("abc");
        assertEquals(expected, Builder.hash(1).hash("abc", lenFunc).result());
    }

    @Test
    public void testOfGeneric() {
        String str = "test";
        Builder<String> builder = Builder.of(str);
        Assertions.assertNotNull(builder);
        Assertions.assertEquals(str, builder.val());

        Integer num = 42;
        Builder<Integer> intBuilder = Builder.of(num);
        Assertions.assertEquals(num, intBuilder.val());

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

    //
    //    @Test
    //    public void testFilter() {
    //        Builder<String> builder = Builder.of("test");
    //        Optional<String> result = builder.filter(s -> s.length() > 3);
    //        Assertions.assertTrue(result.isPresent());
    //        Assertions.assertEquals("test", result.get());
    //
    //        Optional<String> emptyResult = builder.filter(s -> s.length() > 10);
    //        Assertions.assertFalse(emptyResult.isPresent());
    //    }

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

    @Test
    public void testMultimapBuilderPut() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);

        builder.put("key", 1).put("key", 2);
        Assertions.assertEquals(2, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderPutMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
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
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);

        builder.putMany("key", Arrays.asList(1, 2, 3));
        Assertions.assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderPutManyMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
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
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        ListMultimap<String, Integer> toAdd = CommonUtil.newListMultimap();
        toAdd.putValues("key", Arrays.asList(1, 2, 3));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(toAdd);

        Assertions.assertEquals(3, multimap.get("key").size());
    }

    @Test
    public void testMultimapBuilderRemoveOne() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.put("key", 1);
        multimap.put("key", 2);

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne("key", 1);

        Assertions.assertEquals(1, multimap.get("key").size());
        Assertions.assertEquals(Integer.valueOf(2), multimap.get("key").get(0));
    }

    @Test
    public void testMultimapBuilderRemoveOneMap() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
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
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.putValues("key", CommonUtil.toList(1, 2, 3));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeAll("key");

        Assertions.assertFalse(multimap.containsKey("key"));
    }

    @Test
    public void testMultimapBuilderRemoveMany() {
        ListMultimap<String, Integer> multimap = CommonUtil.newListMultimap();
        multimap.putValues("key", Arrays.asList(1, 2, 3, 4));

        Builder.MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany("key", Arrays.asList(1, 3));

        Assertions.assertEquals(2, multimap.get("key").size());
        Assertions.assertTrue(multimap.get("key").contains(2));
        Assertions.assertTrue(multimap.get("key").contains(4));
    }

    @Test
    public void testDatasetBuilderRenameColumn() {
        Dataset ds = new RowDataset(Arrays.asList("oldName"), Arrays.asList(Arrays.asList("value")));
        Builder.DatasetBuilder builder = Builder.of(ds);

        builder.renameColumn("oldName", "newName");
        Assertions.assertTrue(ds.columnNames().contains("newName"));
        Assertions.assertFalse(ds.columnNames().contains("oldName"));
    }

    @Test
    public void testDatasetBuilderAddColumn() {
        Dataset ds = new RowDataset(CommonUtil.toList("col1"), CommonUtil.toList(Arrays.asList("val1")));
        Builder.DatasetBuilder builder = Builder.of(ds);

        builder.addColumn("col2", CommonUtil.toList("val2"));
        Assertions.assertEquals(2, ds.columnCount());
        Assertions.assertTrue(ds.columnNames().contains("col2"));
    }

    @Test
    public void testDatasetBuilderRemoveColumn() {
        Dataset ds = new RowDataset(CommonUtil.toList("col1", "col2"), CommonUtil.toList(CommonUtil.toList("val1"), CommonUtil.toList("val2")));
        Builder.DatasetBuilder builder = Builder.of(ds);

        builder.removeColumn("col2");
        Assertions.assertEquals(1, ds.columnCount());
        Assertions.assertFalse(ds.columnNames().contains("col2"));
    }

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
    public void testDatasetBuilderDivideColumn() {
        testDataset.addColumn("fullName", Arrays.asList("John Doe", "Jane Smith", "Bob Jones"));

        DatasetBuilder builder = Builder.of(testDataset);

        Function<Object, List<String>> splitFunc = fullName -> Arrays.asList(fullName.toString().split(" "));

        builder.divideColumn("fullName", Arrays.asList("firstName", "lastName"), splitFunc);

        assertFalse(testDataset.columnNames().contains("fullName"));
        assertTrue(testDataset.columnNames().contains("firstName"));
        assertTrue(testDataset.columnNames().contains("lastName"));
    }

    @Test
    public void testMultimapBuilderPutManyFromMultimap() {
        Multimap<String, Integer, List<Integer>> multimap1 = CommonUtil.newListMultimap();
        Multimap<String, Integer, List<Integer>> multimap2 = CommonUtil.newListMultimap();

        multimap2.put("key1", 10);
        multimap2.put("key1", 20);
        multimap2.put("key2", 30);

        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap1);

        builder.putMany(multimap2);

        assertEquals(Arrays.asList(10, 20), multimap1.get("key1"));
        assertEquals(Arrays.asList(30), multimap1.get("key2"));
    }

    @Test
    public void testMultimapBuilderRemoveOperations() {
        Multimap<String, Integer, List<Integer>> multimap = CommonUtil.newListMultimap();
        multimap.put("key1", 1);
        multimap.put("key1", 2);
        multimap.put("key1", 3);
        multimap.put("key2", 4);
        multimap.put("key3", 5);

        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(multimap);

        Map<String, Integer> toRemoveOne = new HashMap<>();
        toRemoveOne.put("key1", 2);
        builder.removeOne(toRemoveOne);

        assertEquals(Arrays.asList(1, 3), multimap.get("key1"));

        builder.removeMany("key1", Arrays.asList(1, 3));
        assertNull(multimap.get("key1"));

        builder.removeAll("key2");
        assertNull(multimap.get("key2"));

        Map<String, Collection<Integer>> toRemoveMany = new HashMap<>();
        toRemoveMany.put("key3", Arrays.asList(5));
        builder.removeMany(toRemoveMany);
        assertNull(multimap.get("key3"));
    }

    @Test
    public void testComparisonBuilderWithCustomComparator() {
        Comparator<String> caseInsensitive = String.CASE_INSENSITIVE_ORDER;

        int result = Builder.compare("Hello", "hello", caseInsensitive).result();
        assertEquals(0, result);

        result = Builder.compare("ABC", "xyz", caseInsensitive).result();
        assertTrue(result < 0);
    }

    @Test
    public void testXBuilder() {
        String value = "test";
        Builder<String> builder = Builder.of(value);

        assertTrue(builder instanceof Builder);
        assertEquals(value, builder.val());
    }

    @Test
    public void testBuilderChainingComplex() {
        List<String> list = new ArrayList<>();

        Builder.of(list).add("first").addAll(Arrays.asList("second", "third")).add(1, "inserted").remove("third").accept(l -> l.add("fourth")).apply(l -> {
            Collections.reverse(l);
            return l;
        });

        assertEquals(Arrays.asList("fourth", "second", "inserted", "first"), list);
    }

    @Test
    public void testMultisetBuilderEdgeCases() {
        Multiset<String> multiset = CommonUtil.newMultiset();
        MultisetBuilder<String> builder = Builder.of(multiset);

        builder.add("item", 3).remove("item", 5);

        assertEquals(0, multiset.count("item"));

        builder.setCount("item2", 10).setCount("item2", 5);

        assertEquals(5, multiset.count("item2"));
    }

    @Test
    public void testMapBuilderPutIfAbsentEdgeCases() {
        Map<String, String> map = new HashMap<>();
        map.put("null-value", null);

        MapBuilder<String, String, Map<String, String>> builder = Builder.of(map);

        builder.putIfAbsent("null-value", "replacement");
        assertEquals("replacement", map.get("null-value"));

        map.put("null-value2", null);
        builder.putIfAbsent("null-value2", () -> "generated");
        assertEquals("generated", map.get("null-value2"));
    }

    @Test
    public void testHashCodeBuilderConsistency() {
        Object[] values = { "test", 123, true, 45.6, 'x' };

        int hash1 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        int hash2 = Builder.hash(values[0]).hash(values[1]).hash((boolean) values[2]).hash((double) values[3]).hash((char) values[4]).result();

        assertEquals(hash1, hash2);
    }

    @Test
    public void testEquivalenceBuilderShortCircuit() {
        boolean[] evaluated = { false, false, false };

        BiPredicate<String, String> tracker1 = (a, b) -> {
            evaluated[0] = true;
            return true;
        };

        BiPredicate<String, String> tracker2 = (a, b) -> {
            evaluated[1] = true;
            return false;
        };

        BiPredicate<String, String> tracker3 = (a, b) -> {
            evaluated[2] = true;
            return true;
        };

        boolean result = Builder.equals("a", "a", tracker1).equals("b", "b", tracker2).equals("c", "c", tracker3).result();

        assertFalse(result);
        assertTrue(evaluated[0]);
        assertTrue(evaluated[1]);
        assertFalse(evaluated[2]);
    }

    @Test
    public void testBuilderStream() {
        Builder<String> builder = Builder.of("test");
        Stream<String> stream = builder.stream();

        assertEquals(1, stream.count());
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

    // --- Tests for ComparisonBuilder instance methods ---

    @Test
    public void testComparisonBuilder_instance_compareNullLess() {
        // When first comparison is 0, compareNullLess should be evaluated
        int result = Builder.compare("a", "a").compareNullLess(null, "b").result();
        assertTrue(result < 0);

        result = Builder.compare("a", "a").compareNullLess("b", null).result();
        assertTrue(result > 0);

        result = Builder.compare("a", "a").compareNullLess((String) null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compareNullBigger() {
        int result = Builder.compare("a", "a").compareNullBigger(null, "b").result();
        assertTrue(result > 0);

        result = Builder.compare("a", "a").compareNullBigger("b", null).result();
        assertTrue(result < 0);

        result = Builder.compare("a", "a").compareNullBigger((String) null, (String) null).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compareFalseLess() {
        int result = Builder.compare(1, 1).compareFalseLess(false, true).result();
        assertTrue(result < 0);

        result = Builder.compare(1, 1).compareFalseLess(true, false).result();
        assertTrue(result > 0);

        result = Builder.compare(1, 1).compareFalseLess(true, true).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compareTrueLess() {
        int result = Builder.compare(1, 1).compareTrueLess(true, false).result();
        assertTrue(result < 0);

        result = Builder.compare(1, 1).compareTrueLess(false, true).result();
        assertTrue(result > 0);

        result = Builder.compare(1, 1).compareTrueLess(false, false).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compare_withComparator() {
        int result = Builder.compare("a", "a").compare("b", "a", Comparator.naturalOrder()).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_char() {
        int result = Builder.compare(1, 1).compare('b', 'a').result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_byte() {
        int result = Builder.compare(1, 1).compare((byte) 2, (byte) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_short() {
        int result = Builder.compare(1, 1).compare((short) 2, (short) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_int() {
        int result = Builder.compare(1, 1).compare(10, 5).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_long() {
        int result = Builder.compare(1, 1).compare(100L, 50L).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_float() {
        int result = Builder.compare(1, 1).compare(2.0f, 1.0f).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_floatWithTolerance() {
        int result = Builder.compare(1, 1).compare(1.0f, 1.0001f, 0.001f).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_instance_compare_double() {
        int result = Builder.compare(1, 1).compare(2.0, 1.0).result();
        assertTrue(result > 0);
    }

    @Test
    public void testComparisonBuilder_instance_compare_doubleWithTolerance() {
        int result = Builder.compare(1, 1).compare(1.0, 1.0001, 0.001).result();
        assertEquals(0, result);
    }

    @Test
    public void testComparisonBuilder_shortCircuit_skipsSubsequent() {
        // When first comparison is non-zero, subsequent should be skipped
        int result = Builder.compare("b", "a").compare("z", "a").compare(1, 2).result();
        // result should be from first comparison only
        assertTrue(result > 0);
    }

    // --- Tests for EquivalenceBuilder instance methods ---

    @Test
    public void testEquivalenceBuilder_instance_equals_boolean() {
        boolean result = Builder.equals("a", "a").equals(true, true).result();
        assertTrue(result);

        result = Builder.equals("a", "a").equals(true, false).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_char() {
        boolean result = Builder.equals("a", "a").equals('x', 'x').result();
        assertTrue(result);

        result = Builder.equals("a", "a").equals('x', 'y').result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_byte() {
        boolean result = Builder.equals("a", "a").equals((byte) 1, (byte) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_short() {
        boolean result = Builder.equals("a", "a").equals((short) 1, (short) 1).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_int() {
        boolean result = Builder.equals("a", "a").equals(42, 42).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_long() {
        boolean result = Builder.equals("a", "a").equals(100L, 100L).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_float() {
        boolean result = Builder.equals("a", "a").equals(1.5f, 1.5f).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_floatWithTolerance() {
        boolean result = Builder.equals("a", "a").equals(1.0f, 1.0001f, 0.001f).result();
        assertTrue(result);

        result = Builder.equals("a", "a").equals(1.0f, 2.0f, 0.001f).result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_double() {
        boolean result = Builder.equals("a", "a").equals(3.14, 3.14).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_doubleWithTolerance() {
        boolean result = Builder.equals("a", "a").equals(1.0, 1.0001, 0.001).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_object() {
        boolean result = Builder.equals(1, 1).equals("hello", "hello").result();
        assertTrue(result);

        result = Builder.equals(1, 1).equals("hello", "world").result();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_equals_withPredicate() {
        boolean result = Builder.equals(1, 1).equals("ABC", "abc", (a, b) -> a.equalsIgnoreCase(b)).result();
        assertTrue(result);
    }

    @Test
    public void testEquivalenceBuilder_instance_isEquals() {
        // isEquals is an alias for result
        boolean result = Builder.equals("a", "a").equals(1, 1).isEquals();
        assertTrue(result);

        result = Builder.equals("a", "b").isEquals();
        assertFalse(result);
    }

    @Test
    public void testEquivalenceBuilder_shortCircuit_skipsSubsequent() {
        // When first equals is false, subsequent should be skipped
        boolean result = Builder.equals("a", "b").equals(1, 1).equals(true, true).result();
        assertFalse(result);
    }

    // --- Tests for HashCodeBuilder instance methods ---

    @Test
    public void testHashCodeBuilder_instance_hash_object() {
        int result = Builder.hash("hello").hash("world").result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_withFunction() {
        int result = Builder.hash("hello").hash("world", String::length).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_boolean() {
        int result = Builder.hash(1).hash(true).result();
        int result2 = Builder.hash(1).hash(false).result();
        assertNotEquals(result, result2);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_char() {
        int result = Builder.hash(1).hash('a').result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_byte() {
        int result = Builder.hash(1).hash((byte) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_short() {
        int result = Builder.hash(1).hash((short) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_int() {
        int result = Builder.hash(1).hash(42).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_long() {
        int result = Builder.hash(1).hash(100L).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_float() {
        int result = Builder.hash(1).hash(3.14f).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHashCodeBuilder_instance_hash_double() {
        int result = Builder.hash(1).hash(3.14).result();
        assertNotEquals(0, result);
    }

    // --- Tests for DatasetBuilder combineColumns with class parameter ---

    @Test
    public void testDatasetBuilder_combineColumns_withClass() {
        Dataset dataset = CommonUtil.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList("x", "y")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Arrays.asList("a", "b"), "combined", Object[].class);
        assertTrue(builder.val().columnNames().contains("combined"));
    }

    // --- Tests for Builder.of static factory methods (specialized detection) ---

    @Test
    public void testBuilder_ofBooleanList_returnsSpecialized() {
        Builder<?> b = Builder.of(BooleanList.of(true, false));
        assertTrue(b instanceof BooleanListBuilder);
    }

    @Test
    public void testBuilder_ofCharList_returnsSpecialized() {
        Builder<?> b = Builder.of(CharList.of('a', 'b'));
        assertTrue(b instanceof CharListBuilder);
    }

    @Test
    public void testBuilder_ofByteList_returnsSpecialized() {
        Builder<?> b = Builder.of(ByteList.of((byte) 1));
        assertTrue(b instanceof ByteListBuilder);
    }

    @Test
    public void testBuilder_ofShortList_returnsSpecialized() {
        Builder<?> b = Builder.of(ShortList.of((short) 1));
        assertTrue(b instanceof ShortListBuilder);
    }

    @Test
    public void testBuilder_ofFloatList_returnsSpecialized() {
        Builder<?> b = Builder.of(FloatList.of(1.0f));
        assertTrue(b instanceof FloatListBuilder);
    }

    @Test
    public void testBuilder_ofSetMultimap_returnsMultimapBuilder() {
        Builder<?> b = Builder.of(new SetMultimap<>());
        assertTrue(b instanceof MultimapBuilder);
    }

    @Test
    public void testBuilder_ofRowDataset_returnsDatasetBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Builder<?> b = Builder.of(ds);
        assertTrue(b instanceof DatasetBuilder);
    }

    @Test
    public void testBuilder_ofUnknownType_returnsGenericBuilder() {
        // AtomicInteger is not a known collection type
        Builder<AtomicInteger> b = Builder.of(new AtomicInteger(5));
        assertEquals(5, b.val().get());
    }

    // --- Tests for static compare factory methods with more primitive types ---

    @Test
    public void testCompare_static_char() {
        int result = Builder.compare('b', 'a').result();
        assertTrue(result > 0);

        result = Builder.compare('a', 'a').result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_static_byte() {
        int result = Builder.compare((byte) 2, (byte) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_short() {
        int result = Builder.compare((short) 2, (short) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_int() {
        int result = Builder.compare(10, 5).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_long() {
        int result = Builder.compare(100L, 50L).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_float() {
        int result = Builder.compare(2.0f, 1.0f).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_floatWithTolerance() {
        int result = Builder.compare(1.0f, 1.0001f, 0.001f).result();
        assertEquals(0, result);
    }

    @Test
    public void testCompare_static_double() {
        int result = Builder.compare(2.0, 1.0).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_static_doubleWithTolerance() {
        int result = Builder.compare(1.0, 1.0001, 0.001).result();
        assertEquals(0, result);
    }

    // --- Tests for static equals factory methods with more primitive types ---

    @Test
    public void testEquals_static_boolean() {
        assertTrue(Builder.equals(true, true).result());
        assertFalse(Builder.equals(true, false).result());
    }

    @Test
    public void testEquals_static_char() {
        assertTrue(Builder.equals('a', 'a').result());
        assertFalse(Builder.equals('a', 'b').result());
    }

    @Test
    public void testEquals_static_byte() {
        assertTrue(Builder.equals((byte) 1, (byte) 1).result());
        assertFalse(Builder.equals((byte) 1, (byte) 2).result());
    }

    @Test
    public void testEquals_static_short() {
        assertTrue(Builder.equals((short) 1, (short) 1).result());
        assertFalse(Builder.equals((short) 1, (short) 2).result());
    }

    @Test
    public void testEquals_static_int() {
        assertTrue(Builder.equals(42, 42).result());
        assertFalse(Builder.equals(42, 43).result());
    }

    @Test
    public void testEquals_static_long() {
        assertTrue(Builder.equals(100L, 100L).result());
        assertFalse(Builder.equals(100L, 200L).result());
    }

    @Test
    public void testEquals_static_float() {
        assertTrue(Builder.equals(1.5f, 1.5f).result());
        assertFalse(Builder.equals(1.5f, 2.5f).result());
    }

    @Test
    public void testEquals_static_floatWithTolerance() {
        assertTrue(Builder.equals(1.0f, 1.0001f, 0.001f).result());
        assertFalse(Builder.equals(1.0f, 2.0f, 0.001f).result());
    }

    @Test
    public void testEquals_static_double() {
        assertTrue(Builder.equals(3.14, 3.14).result());
        assertFalse(Builder.equals(3.14, 2.71).result());
    }

    @Test
    public void testEquals_static_doubleWithTolerance() {
        assertTrue(Builder.equals(1.0, 1.0001, 0.001).result());
        assertFalse(Builder.equals(1.0, 2.0, 0.001).result());
    }

    // --- Tests for static hash factory methods with more types ---

    @Test
    public void testHash_static_boolean() {
        int hashTrue = Builder.hash(true).result();
        int hashFalse = Builder.hash(false).result();
        assertNotEquals(hashTrue, hashFalse);
    }

    @Test
    public void testHash_static_char() {
        int result = Builder.hash('a').result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_byte() {
        int result = Builder.hash((byte) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_short() {
        int result = Builder.hash((short) 5).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_int() {
        int result = Builder.hash(42).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_long() {
        int result = Builder.hash(100L).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_float() {
        int result = Builder.hash(3.14f).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_double() {
        int result = Builder.hash(3.14).result();
        assertNotEquals(0, result);
    }

    @Test
    public void testHash_static_object() {
        int result = Builder.hash("hello").result();
        assertNotEquals(0, result);

        // null should produce 0
        int nullResult = Builder.hash((Object) null).result();
        assertEquals(0, nullResult);
    }

    @Test
    public void testHash_static_withFunction() {
        int result = Builder.hash("hello", String::length).result();
        assertEquals(5, result);
    }

    // --- Tests for ListBuilder.remove(int index) vs remove(Object) ---

    @Test
    public void testListBuilder_removeAll_empty() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ListBuilder<String, List<String>> builder = Builder.of(list);
        builder.removeAll(Collections.emptyList());
        assertEquals(3, builder.val().size());
    }

    // --- Tests for CollectionBuilder removeAll(T... a) varargs ---

    @Test
    public void testCollectionBuilder_removeAllVarargs_empty() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b", "c"));
        CollectionBuilder<String, Set<String>> builder = Builder.of(set);
        builder.removeAll();
        assertEquals(3, builder.val().size());
    }

    // --- Tests for MapBuilder putIfAbsent with existing key ---

    @Test
    public void testMapBuilder_putIfAbsent_existingKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("key", 99);
        assertEquals(1, (int) builder.val().get("key"));
    }

    @Test
    public void testMapBuilder_putIfAbsentSupplier_existingKey() {
        Map<String, Integer> map = new HashMap<>();
        map.put("key", 1);
        MapBuilder<String, Integer, Map<String, Integer>> builder = Builder.of(map);
        builder.putIfAbsent("key", () -> 99);
        assertEquals(1, (int) builder.val().get("key"));
    }

    // --- Tests for MultimapBuilder additional operations ---

    @Test
    public void testMultimapBuilder_removeMany_collection() {
        Multimap<String, Integer, List<Integer>> mm = new Multimap<>();
        mm.putValues("key", Arrays.asList(1, 2, 3, 4));
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(mm);
        builder.removeMany("key", Arrays.asList(2, 3));
        assertEquals(2, builder.val().get("key").size());
    }

    @Test
    public void testMultimapBuilder_removeMany_multimap() {
        Multimap<String, Integer, List<Integer>> mm = new Multimap<>();
        mm.putValues("key", Arrays.asList(1, 2, 3));
        Multimap<String, Integer, List<Integer>> toRemove = new Multimap<>();
        toRemove.put("key", 2);
        MultimapBuilder<String, Integer, List<Integer>, Multimap<String, Integer, List<Integer>>> builder = Builder.of(mm);
        builder.removeMany(toRemove);
        assertEquals(2, builder.val().get("key").size());
    }

    // --- Test for Builder accept/apply chaining ---

    @Test
    public void testBuilder_accept_chaining() {
        List<String> list = new ArrayList<>();
        List<String> result = Builder.of(list).accept(l -> l.add("a")).accept(l -> l.add("b")).val();
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    public void testBuilder_apply_transform() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        int size = Builder.of(list).apply(List::size);
        assertEquals(3, size);
    }

    @Test
    public void testBuilder_stream_singleElement() {
        String value = "test";
        long count = Builder.of(value).stream().count();
        assertEquals(1, count);
        assertEquals("test", Builder.of(value).stream().findFirst().orElse(null));
    }

    // ========================================================================
    // Additional tests for untested methods and edge cases
    // ========================================================================

    // --- Builder.of(BooleanList) null argument ---
    @Test
    public void testOf_BooleanList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((BooleanList) null));
    }

    // --- Builder.of(CharList) null argument ---
    @Test
    public void testOf_CharList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((CharList) null));
    }

    // --- Builder.of(ByteList) null argument ---
    @Test
    public void testOf_ByteList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((ByteList) null));
    }

    // --- Builder.of(ShortList) null argument ---
    @Test
    public void testOf_ShortList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((ShortList) null));
    }

    // --- Builder.of(IntList) null argument ---
    @Test
    public void testOf_IntList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((IntList) null));
    }

    // --- Builder.of(LongList) null argument ---
    @Test
    public void testOf_LongList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((LongList) null));
    }

    // --- Builder.of(FloatList) null argument ---
    @Test
    public void testOf_FloatList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((FloatList) null));
    }

    // --- Builder.of(DoubleList) null argument ---
    @Test
    public void testOf_DoubleList_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((DoubleList) null));
    }

    // --- Builder.of(Collection) null argument ---
    @Test
    public void testOf_Collection_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Collection<String>) null));
    }

    // --- Builder.of(Map) null argument ---
    @Test
    public void testOf_Map_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Map<String, Integer>) null));
    }

    // --- Builder.of(Multiset) null argument ---
    @Test
    public void testOf_Multiset_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Multiset<String>) null));
    }

    // --- Builder.of(Dataset) null argument ---
    @Test
    public void testOf_Dataset_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Dataset) null));
    }

    // --- Builder accept returns same builder ---
    @Test
    public void testAccept_returnsSameBuilder() {
        Builder<String> builder = Builder.of("hello");
        Builder<String> returned = builder.accept(s -> {
        });
        assertSame(builder, returned);
    }

    // --- Builder accept multiple chained ---
    @Test
    public void testAccept_multipleChained() {
        List<Integer> list = new ArrayList<>();
        Builder.of(list).accept(l -> l.add(1)).accept(l -> l.add(2)).accept(l -> l.add(3));
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    // --- Builder apply with null function result ---
    @Test
    public void testApply_nullResult() {
        Builder<String> builder = Builder.of("test");
        Object result = builder.apply(s -> null);
        assertNull(result);
    }

    // --- Builder stream content ---
    @Test
    public void testStream_content() {
        String value = "hello";
        String result = Builder.of(value).stream().findFirst().orElse(null);
        assertEquals("hello", result);
    }

    // --- Builder val returns same reference ---
    @Test
    public void testVal_returnsSameReference() {
        List<String> list = new ArrayList<>();
        Builder<List<String>> builder = Builder.of(list);
        assertSame(list, builder.val());
    }

    // --- BooleanListBuilder set returns same builder ---
    @Test
    public void testBooleanListBuilder_set_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, false));
    }

    // --- BooleanListBuilder add returns same builder ---
    @Test
    public void testBooleanListBuilder_add_returnsBuilder() {
        BooleanList list = BooleanList.of();
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.add(true));
    }

    // --- BooleanListBuilder addAtIndex returns same builder ---
    @Test
    public void testBooleanListBuilder_addAtIndex_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, false));
    }

    // --- BooleanListBuilder addAll returns same builder ---
    @Test
    public void testBooleanListBuilder_addAll_returnsBuilder() {
        BooleanList list = BooleanList.of();
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(BooleanList.of(true)));
    }

    // --- BooleanListBuilder addAllAtIndex returns same builder ---
    @Test
    public void testBooleanListBuilder_addAllAtIndex_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, BooleanList.of(false)));
    }

    // --- BooleanListBuilder remove returns same builder ---
    @Test
    public void testBooleanListBuilder_remove_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.remove(true));
    }

    // --- BooleanListBuilder removeAll returns same builder ---
    @Test
    public void testBooleanListBuilder_removeAll_returnsBuilder() {
        BooleanList list = BooleanList.of(true);
        BooleanListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(BooleanList.of(true)));
    }

    // --- CharListBuilder returns same builder for all operations ---
    @Test
    public void testCharListBuilder_set_returnsBuilder() {
        CharList list = CharList.of('a');
        CharListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, 'z'));
    }

    @Test
    public void testCharListBuilder_add_returnsBuilder() {
        CharList list = CharList.of();
        CharListBuilder b = Builder.of(list);
        assertSame(b, b.add('a'));
    }

    @Test
    public void testCharListBuilder_addAtIndex_returnsBuilder() {
        CharList list = CharList.of('a');
        CharListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, 'b'));
    }

    @Test
    public void testCharListBuilder_addAll_returnsBuilder() {
        CharList list = CharList.of();
        CharListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(CharList.of('a')));
    }

    @Test
    public void testCharListBuilder_addAllAtIndex_returnsBuilder() {
        CharList list = CharList.of('a');
        CharListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, CharList.of('b')));
    }

    @Test
    public void testCharListBuilder_remove_returnsBuilder() {
        CharList list = CharList.of('a');
        CharListBuilder b = Builder.of(list);
        assertSame(b, b.remove('a'));
    }

    @Test
    public void testCharListBuilder_removeAll_returnsBuilder() {
        CharList list = CharList.of('a');
        CharListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(CharList.of('a')));
    }

    // --- ByteListBuilder returns same builder ---
    @Test
    public void testByteListBuilder_set_returnsBuilder() {
        ByteList list = ByteList.of((byte) 1);
        ByteListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, (byte) 2));
    }

    @Test
    public void testByteListBuilder_add_returnsBuilder() {
        ByteList list = ByteList.of();
        ByteListBuilder b = Builder.of(list);
        assertSame(b, b.add((byte) 1));
    }

    @Test
    public void testByteListBuilder_addAtIndex_returnsBuilder() {
        ByteList list = ByteList.of((byte) 1);
        ByteListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, (byte) 2));
    }

    @Test
    public void testByteListBuilder_addAll_returnsBuilder() {
        ByteList list = ByteList.of();
        ByteListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(ByteList.of((byte) 1)));
    }

    @Test
    public void testByteListBuilder_addAllAtIndex_returnsBuilder() {
        ByteList list = ByteList.of((byte) 1);
        ByteListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, ByteList.of((byte) 2)));
    }

    @Test
    public void testByteListBuilder_remove_returnsBuilder() {
        ByteList list = ByteList.of((byte) 1);
        ByteListBuilder b = Builder.of(list);
        assertSame(b, b.remove((byte) 1));
    }

    @Test
    public void testByteListBuilder_removeAll_returnsBuilder() {
        ByteList list = ByteList.of((byte) 1);
        ByteListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(ByteList.of((byte) 1)));
    }

    // --- ShortListBuilder returns same builder ---
    @Test
    public void testShortListBuilder_set_returnsBuilder() {
        ShortList list = ShortList.of((short) 1);
        ShortListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, (short) 2));
    }

    @Test
    public void testShortListBuilder_add_returnsBuilder() {
        ShortList list = ShortList.of();
        ShortListBuilder b = Builder.of(list);
        assertSame(b, b.add((short) 1));
    }

    @Test
    public void testShortListBuilder_addAtIndex_returnsBuilder() {
        ShortList list = ShortList.of((short) 1);
        ShortListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, (short) 2));
    }

    @Test
    public void testShortListBuilder_addAll_returnsBuilder() {
        ShortList list = ShortList.of();
        ShortListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(ShortList.of((short) 1)));
    }

    @Test
    public void testShortListBuilder_addAllAtIndex_returnsBuilder() {
        ShortList list = ShortList.of((short) 1);
        ShortListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, ShortList.of((short) 2)));
    }

    @Test
    public void testShortListBuilder_remove_returnsBuilder() {
        ShortList list = ShortList.of((short) 1);
        ShortListBuilder b = Builder.of(list);
        assertSame(b, b.remove((short) 1));
    }

    @Test
    public void testShortListBuilder_removeAll_returnsBuilder() {
        ShortList list = ShortList.of((short) 1);
        ShortListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(ShortList.of((short) 1)));
    }

    // --- IntListBuilder returns same builder ---
    @Test
    public void testIntListBuilder_set_returnsBuilder() {
        IntList list = IntList.of(1);
        IntListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, 2));
    }

    @Test
    public void testIntListBuilder_add_returnsBuilder() {
        IntList list = IntList.of();
        IntListBuilder b = Builder.of(list);
        assertSame(b, b.add(1));
    }

    @Test
    public void testIntListBuilder_addAtIndex_returnsBuilder() {
        IntList list = IntList.of(1);
        IntListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, 2));
    }

    @Test
    public void testIntListBuilder_addAll_returnsBuilder() {
        IntList list = IntList.of();
        IntListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(IntList.of(1)));
    }

    @Test
    public void testIntListBuilder_addAllAtIndex_returnsBuilder() {
        IntList list = IntList.of(1);
        IntListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, IntList.of(2)));
    }

    @Test
    public void testIntListBuilder_remove_returnsBuilder() {
        IntList list = IntList.of(1);
        IntListBuilder b = Builder.of(list);
        assertSame(b, b.remove(1));
    }

    @Test
    public void testIntListBuilder_removeAll_returnsBuilder() {
        IntList list = IntList.of(1);
        IntListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(IntList.of(1)));
    }

    // --- LongListBuilder returns same builder ---
    @Test
    public void testLongListBuilder_set_returnsBuilder() {
        LongList list = LongList.of(1L);
        LongListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, 2L));
    }

    @Test
    public void testLongListBuilder_add_returnsBuilder() {
        LongList list = LongList.of();
        LongListBuilder b = Builder.of(list);
        assertSame(b, b.add(1L));
    }

    @Test
    public void testLongListBuilder_addAtIndex_returnsBuilder() {
        LongList list = LongList.of(1L);
        LongListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, 2L));
    }

    @Test
    public void testLongListBuilder_addAll_returnsBuilder() {
        LongList list = LongList.of();
        LongListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(LongList.of(1L)));
    }

    @Test
    public void testLongListBuilder_addAllAtIndex_returnsBuilder() {
        LongList list = LongList.of(1L);
        LongListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, LongList.of(2L)));
    }

    @Test
    public void testLongListBuilder_remove_returnsBuilder() {
        LongList list = LongList.of(1L);
        LongListBuilder b = Builder.of(list);
        assertSame(b, b.remove(1L));
    }

    @Test
    public void testLongListBuilder_removeAll_returnsBuilder() {
        LongList list = LongList.of(1L);
        LongListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(LongList.of(1L)));
    }

    // --- FloatListBuilder returns same builder ---
    @Test
    public void testFloatListBuilder_set_returnsBuilder() {
        FloatList list = FloatList.of(1.0f);
        FloatListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, 2.0f));
    }

    @Test
    public void testFloatListBuilder_add_returnsBuilder() {
        FloatList list = FloatList.of();
        FloatListBuilder b = Builder.of(list);
        assertSame(b, b.add(1.0f));
    }

    @Test
    public void testFloatListBuilder_addAtIndex_returnsBuilder() {
        FloatList list = FloatList.of(1.0f);
        FloatListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, 2.0f));
    }

    @Test
    public void testFloatListBuilder_addAll_returnsBuilder() {
        FloatList list = FloatList.of();
        FloatListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(FloatList.of(1.0f)));
    }

    @Test
    public void testFloatListBuilder_addAllAtIndex_returnsBuilder() {
        FloatList list = FloatList.of(1.0f);
        FloatListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, FloatList.of(2.0f)));
    }

    @Test
    public void testFloatListBuilder_remove_returnsBuilder() {
        FloatList list = FloatList.of(1.0f);
        FloatListBuilder b = Builder.of(list);
        assertSame(b, b.remove(1.0f));
    }

    @Test
    public void testFloatListBuilder_removeAll_returnsBuilder() {
        FloatList list = FloatList.of(1.0f);
        FloatListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(FloatList.of(1.0f)));
    }

    // --- DoubleListBuilder returns same builder ---
    @Test
    public void testDoubleListBuilder_set_returnsBuilder() {
        DoubleList list = DoubleList.of(1.0);
        DoubleListBuilder b = Builder.of(list);
        assertSame(b, b.set(0, 2.0));
    }

    @Test
    public void testDoubleListBuilder_add_returnsBuilder() {
        DoubleList list = DoubleList.of();
        DoubleListBuilder b = Builder.of(list);
        assertSame(b, b.add(1.0));
    }

    @Test
    public void testDoubleListBuilder_addAtIndex_returnsBuilder() {
        DoubleList list = DoubleList.of(1.0);
        DoubleListBuilder b = Builder.of(list);
        assertSame(b, b.add(0, 2.0));
    }

    @Test
    public void testDoubleListBuilder_addAll_returnsBuilder() {
        DoubleList list = DoubleList.of();
        DoubleListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(DoubleList.of(1.0)));
    }

    @Test
    public void testDoubleListBuilder_addAllAtIndex_returnsBuilder() {
        DoubleList list = DoubleList.of(1.0);
        DoubleListBuilder b = Builder.of(list);
        assertSame(b, b.addAll(0, DoubleList.of(2.0)));
    }

    @Test
    public void testDoubleListBuilder_remove_returnsBuilder() {
        DoubleList list = DoubleList.of(1.0);
        DoubleListBuilder b = Builder.of(list);
        assertSame(b, b.remove(1.0));
    }

    @Test
    public void testDoubleListBuilder_removeAll_returnsBuilder() {
        DoubleList list = DoubleList.of(1.0);
        DoubleListBuilder b = Builder.of(list);
        assertSame(b, b.removeAll(DoubleList.of(1.0)));
    }

    // --- ListBuilder returns same builder ---
    @Test
    public void testListBuilder_add_returnsBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.add("a"));
    }

    @Test
    public void testListBuilder_addAll_returnsBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.addAll(Arrays.asList("a")));
    }

    @Test
    public void testListBuilder_addAllVarargs_returnsBuilder() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.addAll("a", "b"));
    }

    @Test
    public void testListBuilder_remove_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testListBuilder_removeAll_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }

    @Test
    public void testListBuilder_removeAllVarargs_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.removeAll("a", "b"));
    }

    @Test
    public void testListBuilder_addAtIndex_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.add(0, "b"));
    }

    @Test
    public void testListBuilder_addAllAtIndex_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.addAll(0, Arrays.asList("b")));
    }

    @Test
    public void testListBuilder_removeAtIndex_returnsBuilder() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        assertSame(b, b.remove(0));
    }

    // --- CollectionBuilder returns same builder ---
    @Test
    public void testCollectionBuilder_add_returnsBuilder() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.add("a"));
    }

    @Test
    public void testCollectionBuilder_addAll_returnsBuilder() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.addAll(Arrays.asList("a")));
    }

    @Test
    public void testCollectionBuilder_addAllVarargs_returnsBuilder() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.addAll("a", "b"));
    }

    @Test
    public void testCollectionBuilder_remove_returnsBuilder() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testCollectionBuilder_removeAll_returnsBuilder() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }

    @Test
    public void testCollectionBuilder_removeAllVarargs_returnsBuilder() {
        Set<String> set = new HashSet<>(Arrays.asList("a", "b"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        assertSame(b, b.removeAll("a", "b"));
    }

    // --- CollectionBuilder addAll/removeAll with null or empty ---
    @Test
    public void testCollectionBuilder_addAll_nullCollection() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.addAll((Collection<String>) null);
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionBuilder_removeAll_nullCollection() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.removeAll((Collection<?>) null);
        assertEquals(1, set.size());
    }

    @Test
    public void testListBuilder_addAll_nullCollection() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.addAll((Collection<String>) null);
        assertEquals(0, list.size());
    }

    @Test
    public void testListBuilder_removeAll_nullCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.removeAll((Collection<?>) null);
        assertEquals(1, list.size());
    }

    @Test
    public void testListBuilder_addAllAtIndex_nullCollection() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.addAll(0, (Collection<String>) null);
        assertEquals(1, list.size());
    }

    // --- MapBuilder returns same builder ---
    @Test
    public void testMapBuilder_put_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.put("a", 1));
    }

    @Test
    public void testMapBuilder_putAll_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.putAll(new HashMap<>()));
    }

    @Test
    public void testMapBuilder_putIfAbsent_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.putIfAbsent("a", 1));
    }

    @Test
    public void testMapBuilder_putIfAbsentSupplier_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.putIfAbsent("a", () -> 1));
    }

    @Test
    public void testMapBuilder_remove_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testMapBuilder_removeAll_returnsBuilder() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }

    // --- MapBuilder putAll with null ---
    @Test
    public void testMapBuilder_putAll_nullMap() {
        Map<String, Integer> map = new HashMap<>();
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        b.putAll(null);
        assertEquals(0, map.size());
    }

    // --- MapBuilder removeAll with null ---
    @Test
    public void testMapBuilder_removeAll_nullCollection() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        b.removeAll(null);
        assertEquals(1, map.size());
    }

    // --- MapBuilder removeAll with empty ---
    @Test
    public void testMapBuilder_removeAll_emptyCollection() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        MapBuilder<String, Integer, Map<String, Integer>> b = Builder.of(map);
        b.removeAll(Collections.emptyList());
        assertEquals(1, map.size());
    }

    // --- MultisetBuilder returns same builder ---
    @Test
    public void testMultisetBuilder_setCount_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.setCount("a", 5));
    }

    @Test
    public void testMultisetBuilder_add_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.add("a"));
    }

    @Test
    public void testMultisetBuilder_addWithOccurrences_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.add("a", 3));
    }

    @Test
    public void testMultisetBuilder_remove_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a");
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.remove("a"));
    }

    @Test
    public void testMultisetBuilder_removeWithOccurrences_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.remove("a", 2));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrences_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.removeAllOccurrences("a"));
    }

    @Test
    public void testMultisetBuilder_removeAllOccurrencesCollection_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.removeAllOccurrences(Arrays.asList("a")));
    }

    @Test
    public void testMultisetBuilder_removeAll_returnsBuilder() {
        Multiset<String> multiset = new Multiset<>();
        multiset.add("a", 3);
        MultisetBuilder<String> b = Builder.of(multiset);
        assertSame(b, b.removeAll(Arrays.asList("a")));
    }

    // --- MultimapBuilder returns same builder ---
    @Test
    public void testMultimapBuilder_put_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.put("a", 1));
    }

    @Test
    public void testMultimapBuilder_putMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.put(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_putMany_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.putMany("a", Arrays.asList(1)));
    }

    @Test
    public void testMultimapBuilder_putManyMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.putMany(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_putManyMultimap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.putMany(CommonUtil.newListMultimap()));
    }

    @Test
    public void testMultimapBuilder_removeOne_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeOne("a", 1));
    }

    @Test
    public void testMultimapBuilder_removeOneMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeOne(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_removeAll_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeAll("a"));
    }

    @Test
    public void testMultimapBuilder_removeMany_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        mm.putValues("a", Arrays.asList(1, 2));
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeMany("a", Arrays.asList(1)));
    }

    @Test
    public void testMultimapBuilder_removeManyMap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeMany(new HashMap<>()));
    }

    @Test
    public void testMultimapBuilder_removeManyMultimap_returnsBuilder() {
        ListMultimap<String, Integer> mm = CommonUtil.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> b = Builder.of(mm);
        assertSame(b, b.removeMany(CommonUtil.newListMultimap()));
    }

    // --- DatasetBuilder returns same builder for all methods ---
    @Test
    public void testDatasetBuilder_renameColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.renameColumn("col1", "col1_new"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsMap_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        Map<String, String> renames = new HashMap<>();
        renames.put("col1", "col1_new");
        assertSame(b, b.renameColumns(renames));
    }

    @Test
    public void testDatasetBuilder_renameColumnsCollectionFunction_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.renameColumns(Arrays.asList("col1"), name -> "new_" + name));
    }

    @Test
    public void testDatasetBuilder_renameColumnsFunction_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.renameColumns(name -> "prefix_" + name));
    }

    @Test
    public void testDatasetBuilder_addColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.addColumn("col2", Arrays.asList(2)));
    }

    @Test
    public void testDatasetBuilder_addColumnAtIndex_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.addColumn(0, "col0", Arrays.asList(0)));
    }

    @Test
    public void testDatasetBuilder_removeColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.removeColumn("col2"));
    }

    @Test
    public void testDatasetBuilder_removeColumnsCollection_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.removeColumns(Arrays.asList("col2")));
    }

    @Test
    public void testDatasetBuilder_removeColumnsPredicate_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1", "temp"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.removeColumns(name -> name.startsWith("temp")));
    }

    @Test
    public void testDatasetBuilder_updateColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.updateColumn("col1", (Integer v) -> v * 2));
    }

    @Test
    public void testDatasetBuilder_convertColumn_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList("123")));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.convertColumn("col1", Integer.class));
    }

    @Test
    public void testDatasetBuilder_prepend_returnsBuilder() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder b = Builder.of(ds1);
        assertSame(b, b.prepend(ds2));
    }

    @Test
    public void testDatasetBuilder_append_returnsBuilder() {
        Dataset ds1 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset ds2 = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder b = Builder.of(ds1);
        assertSame(b, b.append(ds2));
    }

    @Test
    public void testDatasetBuilder_updateAll_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.updateAll((Integer v) -> v * 2));
    }

    @Test
    public void testDatasetBuilder_replaceIf_returnsBuilder() {
        Dataset ds = CommonUtil.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        DatasetBuilder b = Builder.of(ds);
        assertSame(b, b.replaceIf((Integer v) -> v == 1, 99));
    }

    // --- ComparisonBuilder instance compare with Comparable ---
    @Test
    public void testComparisonBuilder_instance_compare_comparable() {
        int result = Builder.compare("a", "a").compare("x", "y").result();
        assertTrue(result < 0);
    }

    // --- ComparisonBuilder returns 0 when all equal ---
    @Test
    public void testComparisonBuilder_allEqual() {
        int result = Builder.compare("a", "a").compare(1, 1).compare(1L, 1L).compare(1.0f, 1.0f).compare(1.0, 1.0).result();
        assertEquals(0, result);
    }

    // --- ComparisonBuilder compare with Comparator when null ---
    @Test
    public void testComparisonBuilder_compare_nullComparator() {
        int result = Builder.compare("a", "a").compare("a", "b", (Comparator<String>) null).result();
        assertTrue(result < 0);
    }

    // --- CompareNullLess with non-null values ---
    @Test
    public void testCompareNullLess_bothNonNull() {
        int result = Builder.compareNullLess("b", "a").result();
        assertTrue(result > 0);
    }

    // --- CompareNullLess right is null ---
    @Test
    public void testCompareNullLess_rightNull() {
        int result = Builder.compareNullLess("a", null).result();
        assertTrue(result > 0);
    }

    // --- CompareNullBigger with non-null values ---
    @Test
    public void testCompareNullBigger_bothNonNull() {
        int result = Builder.compareNullBigger("a", "b").result();
        assertTrue(result < 0);
    }

    // --- CompareNullBigger right is null ---
    @Test
    public void testCompareNullBigger_rightNull() {
        int result = Builder.compareNullBigger("a", null).result();
        assertTrue(result < 0);
    }

    // --- CompareFalseLess true, false ---
    @Test
    public void testCompareFalseLess_trueGreaterThanFalse() {
        int result = Builder.compareFalseLess(true, false).result();
        assertTrue(result > 0);
    }

    // --- CompareTrueLess false, true ---
    @Test
    public void testCompareTrueLess_falseGreaterThanTrue() {
        int result = Builder.compareTrueLess(false, true).result();
        assertTrue(result > 0);
    }

    // --- EquivalenceBuilder equals with null/non-null ---
    @Test
    public void testEquals_objectNull_nonNull() {
        boolean result = Builder.equals(null, "hello").result();
        assertFalse(result);
    }

    @Test
    public void testEquals_objectNonNull_null() {
        boolean result = Builder.equals("hello", null).result();
        assertFalse(result);
    }

    // --- EquivalenceBuilder isEquals vs result ---
    @Test
    public void testEquivalenceBuilder_isEquals_sameAsResult() {
        EquivalenceBuilder eb = Builder.equals("a", "a").equals(1, 1);
        assertEquals(eb.result(), eb.isEquals());
    }

    // --- HashCodeBuilder hash(Object) with non-null ---
    @Test
    public void testHashCodeBuilder_hash_object_nonNull() {
        int hash = Builder.hash("hello").result();
        assertEquals("hello".hashCode(), hash);
    }

    // --- HashCodeBuilder hash(T, func) ---
    @Test
    public void testHashCodeBuilder_hash_withFunction_chain() {
        int hash = Builder.hash("abc").hash("xyz", String::length).result();
        int expected = 31 * "abc".hashCode() + 3;
        assertEquals(expected, hash);
    }

    // --- Builder.of with LinkedHashMap ---
    @Test
    public void testBuilder_ofLinkedHashMap_returnsMapBuilder() {
        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        Builder<?> b = Builder.of(map);
        assertTrue(b instanceof MapBuilder);
    }

    // --- Builder.of with LinkedHashSet ---
    @Test
    public void testBuilder_ofLinkedHashSet_returnsCollectionBuilder() {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        Builder<?> b = Builder.of(set);
        assertTrue(b instanceof CollectionBuilder);
    }

    // --- Compare static methods returning positive ---
    @Test
    public void testCompare_char_positive() {
        int result = Builder.compare('z', 'a').result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_byte_positive() {
        int result = Builder.compare((byte) 5, (byte) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_short_positive() {
        int result = Builder.compare((short) 5, (short) 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_int_positive() {
        int result = Builder.compare(5, 1).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_long_positive() {
        int result = Builder.compare(5L, 1L).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_float_positive() {
        int result = Builder.compare(5.0f, 1.0f).result();
        assertTrue(result > 0);
    }

    @Test
    public void testCompare_double_positive() {
        int result = Builder.compare(5.0, 1.0).result();
        assertTrue(result > 0);
    }

    // --- Compare char equal ---
    @Test
    public void testCompare_char_equal() {
        assertEquals(0, Builder.compare('x', 'x').result());
    }

    // --- Compare byte equal ---
    @Test
    public void testCompare_byte_equal() {
        assertEquals(0, Builder.compare((byte) 3, (byte) 3).result());
    }

    // --- Compare short equal ---
    @Test
    public void testCompare_short_equal() {
        assertEquals(0, Builder.compare((short) 3, (short) 3).result());
    }

    // --- Compare int equal ---
    @Test
    public void testCompare_int_equal() {
        assertEquals(0, Builder.compare(3, 3).result());
    }

    // --- Compare long equal ---
    @Test
    public void testCompare_long_equal() {
        assertEquals(0, Builder.compare(3L, 3L).result());
    }

    // --- Compare float equal ---
    @Test
    public void testCompare_float_equal() {
        assertEquals(0, Builder.compare(3.0f, 3.0f).result());
    }

    // --- Compare double equal ---
    @Test
    public void testCompare_double_equal() {
        assertEquals(0, Builder.compare(3.0, 3.0).result());
    }

    // --- Compare float with tolerance not equal ---
    @Test
    public void testCompare_floatWithTolerance_notEqual() {
        int result = Builder.compare(1.0f, 2.0f, 0.001f).result();
        assertTrue(result < 0);
    }

    // --- Compare double with tolerance not equal ---
    @Test
    public void testCompare_doubleWithTolerance_notEqual() {
        int result = Builder.compare(1.0, 2.0, 0.001).result();
        assertTrue(result < 0);
    }

    // --- Equals float not equal ---
    @Test
    public void testEquals_float_notEqual() {
        assertFalse(Builder.equals(1.0f, 2.0f).result());
    }

    // --- Equals double not equal ---
    @Test
    public void testEquals_double_notEqual() {
        assertFalse(Builder.equals(1.0, 2.0).result());
    }

    // --- Equals char not equal ---
    @Test
    public void testEquals_char_notEqual() {
        assertFalse(Builder.equals('a', 'b').result());
    }

    // --- Equals byte not equal ---
    @Test
    public void testEquals_byte_notEqual() {
        assertFalse(Builder.equals((byte) 1, (byte) 2).result());
    }

    // --- Equals short not equal ---
    @Test
    public void testEquals_short_notEqual() {
        assertFalse(Builder.equals((short) 1, (short) 2).result());
    }

    // --- Equals int not equal ---
    @Test
    public void testEquals_int_notEqual() {
        assertFalse(Builder.equals(1, 2).result());
    }

    // --- Equals long not equal ---
    @Test
    public void testEquals_long_notEqual() {
        assertFalse(Builder.equals(1L, 2L).result());
    }

    // --- Equals float with tolerance not equal ---
    @Test
    public void testEquals_floatWithTolerance_notEqual() {
        assertFalse(Builder.equals(1.0f, 2.0f, 0.001f).result());
    }

    // --- Equals double with tolerance not equal ---
    @Test
    public void testEquals_doubleWithTolerance_notEqual() {
        assertFalse(Builder.equals(1.0, 2.0, 0.001).result());
    }

    // --- Hash boolean false ---
    @Test
    public void testHash_booleanFalse_value() {
        assertEquals(1237, Builder.hash(false).result());
    }

    // --- Hash boolean true ---
    @Test
    public void testHash_booleanTrue_value() {
        assertEquals(1231, Builder.hash(true).result());
    }

    // --- ComparisonBuilder instance compareNullLess when already decided ---
    @Test
    public void testComparisonBuilder_instance_compareNullLess_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareNullLess(null, "x").result();
        assertTrue(result > 0);
    }

    // --- ComparisonBuilder instance compareNullBigger when already decided ---
    @Test
    public void testComparisonBuilder_instance_compareNullBigger_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareNullBigger(null, "x").result();
        assertTrue(result > 0);
    }

    // --- ComparisonBuilder instance compareFalseLess when already decided ---
    @Test
    public void testComparisonBuilder_instance_compareFalseLess_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareFalseLess(false, true).result();
        assertTrue(result > 0);
    }

    // --- ComparisonBuilder instance compareTrueLess when already decided ---
    @Test
    public void testComparisonBuilder_instance_compareTrueLess_skippedWhenDecided() {
        int result = Builder.compare("b", "a").compareTrueLess(true, false).result();
        assertTrue(result > 0);
    }

    // --- EquivalenceBuilder instance equals skipped when already false ---
    @Test
    public void testEquivalenceBuilder_instance_equals_skippedWhenFalse() {
        boolean result = Builder.equals("a", "b").equals("x", "x").equals(1, 1).result();
        assertFalse(result);
    }

    // --- EquivalenceBuilder instance equals with predicate skipped when already false ---
    @Test
    public void testEquivalenceBuilder_instance_equalsWithPredicate_skippedWhenFalse() {
        boolean result = Builder.equals("a", "b").equals("x", "x", (a, b) -> a.equals(b)).result();
        assertFalse(result);
    }

    // --- HashCodeBuilder instance hash chain correctness ---
    @Test
    public void testHashCodeBuilder_chain_correctness() {
        int h1 = 0;
        h1 = h1 * 31 + N.hashCode("a");
        h1 = h1 * 31 + N.hashCode(42);
        h1 = h1 * 31 + (true ? 1231 : 1237);
        h1 = h1 * 31 + N.hashCode('z');
        h1 = h1 * 31 + N.hashCode((byte) 1);
        h1 = h1 * 31 + N.hashCode((short) 2);
        h1 = h1 * 31 + N.hashCode(100L);
        h1 = h1 * 31 + N.hashCode(1.5f);
        h1 = h1 * 31 + N.hashCode(2.5);

        int h2 = Builder.hash("a").hash(42).hash(true).hash('z').hash((byte) 1).hash((short) 2).hash(100L).hash(1.5f).hash(2.5).result();
        assertEquals(h1, h2);
    }

    // --- Builder.of generic with unknown type ---
    @Test
    public void testBuilder_of_genericUnknownType() {
        StringBuilder sb = new StringBuilder("test");
        Builder<StringBuilder> b = Builder.of(sb);
        assertSame(sb, b.val());
        assertFalse(b instanceof ListBuilder);
        assertFalse(b instanceof MapBuilder);
        assertFalse(b instanceof CollectionBuilder);
    }

    // --- ListBuilder addAll varargs with null ---
    @Test
    public void testListBuilder_addAllVarargs_null() {
        List<String> list = new ArrayList<>();
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.addAll((String[]) null);
        assertEquals(0, list.size());
    }

    // --- ListBuilder removeAll varargs with null ---
    @Test
    public void testListBuilder_removeAllVarargs_null() {
        List<String> list = new ArrayList<>(Arrays.asList("a"));
        ListBuilder<String, List<String>> b = Builder.of(list);
        b.removeAll((String[]) null);
        assertEquals(1, list.size());
    }

    // --- CollectionBuilder addAll varargs with null ---
    @Test
    public void testCollectionBuilder_addAllVarargs_null() {
        Set<String> set = new HashSet<>();
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.addAll((String[]) null);
        assertEquals(0, set.size());
    }

    // --- CollectionBuilder removeAll varargs with null ---
    @Test
    public void testCollectionBuilder_removeAllVarargs_null() {
        Set<String> set = new HashSet<>(Arrays.asList("a"));
        CollectionBuilder<String, Set<String>> b = Builder.of(set);
        b.removeAll((String[]) null);
        assertEquals(1, set.size());
    }

    // --- Builder of with SetMultimap null ---
    @Test
    public void testOf_Multimap_null() {
        assertThrows(IllegalArgumentException.class, () -> Builder.of((Multimap<String, Integer, List<Integer>>) null));
    }

}
