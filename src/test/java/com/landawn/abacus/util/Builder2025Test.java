package com.landawn.abacus.util;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

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

@Tag("2025")
public class Builder2025Test extends TestBase {

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
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        assertNotNull(builder);
        assertEquals(1, builder.val().size());
    }

    @Test
    public void testMultimapBuilder_put() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put("a", 1).put("a", 2);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putMap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.put(map);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testMultimapBuilder_putMany() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany("a", Arrays.asList(1, 2, 3));
        assertEquals(3, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putManyMap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(map);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_putManyMultimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        ListMultimap<String, Integer> other = N.newListMultimap();
        other.put("a", 1);
        other.put("a", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.putMany(other);
        assertEquals(2, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeOne() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeOne("a", 1);
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeOneMap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
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
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("b", 3);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeAll("a");
        assertFalse(builder.val().containsKey("a"));
    }

    @Test
    public void testMultimapBuilder_removeMany() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        multimap.put("a", 3);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany("a", Arrays.asList(1, 2));
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testMultimapBuilder_removeManyMap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
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
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        multimap.put("a", 1);
        multimap.put("a", 2);
        ListMultimap<String, Integer> toRemove = N.newListMultimap();
        toRemove.put("a", 1);
        MultimapBuilder<String, Integer, List<Integer>, ListMultimap<String, Integer>> builder = Builder.of(multimap);
        builder.removeMany(toRemove);
        assertEquals(1, builder.val().get("a").size());
    }

    @Test
    public void testDatasetBuilder_of() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        assertNotNull(builder);
        assertEquals(2, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_renameColumn() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumn("col1", "newCol1");
        assertTrue(builder.val().columnNameList().contains("newCol1"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsMap() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        Map<String, String> renames = new HashMap<>();
        renames.put("col1", "newCol1");
        renames.put("col2", "newCol2");
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(renames);
        assertTrue(builder.val().columnNameList().contains("newCol1"));
        assertTrue(builder.val().columnNameList().contains("newCol2"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(name -> "prefix_" + name);
        assertTrue(builder.val().columnNameList().contains("prefix_col1"));
    }

    @Test
    public void testDatasetBuilder_renameColumnsCollectionFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.renameColumns(Arrays.asList("col1"), name -> name.toUpperCase());
        assertTrue(builder.val().columnNameList().contains("COL1"));
        assertTrue(builder.val().columnNameList().contains("col2"));
    }

    @Test
    public void testDatasetBuilder_addColumn() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("col2", Arrays.asList(3, 4));
        assertEquals(2, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_addColumnAtIndex() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "col0", Arrays.asList(0, 0));
        assertEquals("col0", builder.val().columnNameList().get(0));
    }

    @Test
    public void testDatasetBuilder_removeColumn() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumn("col2");
        assertEquals(1, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_removeColumnsCollection() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2", "col3"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumns(Arrays.asList("col2", "col3"));
        assertEquals(1, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_removeColumnsPredicate() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "temp1", "temp2"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.removeColumns(name -> name.startsWith("temp"));
        assertEquals(1, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_prepend() {
        Dataset dataset1 = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset dataset2 = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset1);
        builder.prepend(dataset2);
        assertEquals(2, builder.val().size());
    }

    @Test
    public void testDatasetBuilder_append() {
        Dataset dataset1 = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
        Dataset dataset2 = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(2)));
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
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Builder<?> builder = Builder.of(multimap);
        assertTrue(builder instanceof MultimapBuilder);
    }

    @Test
    public void testBuilder_ofDataset_returnsDatasetBuilder() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1)));
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

    @Test
    public void testBuilder_map() {
        Builder<String> builder = Builder.of("hello");
        Builder<Integer> mapped = builder.map(String::length);
        assertEquals(5, mapped.val());
    }

    @Test
    public void testBuilder_filter() {
        Builder<String> builder = Builder.of("hello");
        u.Optional<String> filtered = builder.filter(s -> s.length() > 3);
        assertTrue(filtered.isPresent());
        assertEquals("hello", filtered.get());
    }

    @Test
    public void testBuilder_filter_notPresent() {
        Builder<String> builder = Builder.of("hi");
        u.Optional<String> filtered = builder.filter(s -> s.length() > 3);
        assertFalse(filtered.isPresent());
    }

    @Test
    public void testDatasetBuilder_addColumn_withFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("col2", "col1", (Integer val) -> val * 2);
        assertEquals(2, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "col0", "col1", (Integer val) -> val * 10);
        assertEquals("col0", builder.val().columnNameList().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withMultiColumnFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("sum", Arrays.asList("col1", "col2"), arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertEquals(3, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withMultiColumnFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "sum", Arrays.asList("col1", "col2"), arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertEquals("sum", builder.val().columnNameList().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withBiFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("product", Tuple.of("col1", "col2"), (Integer a, Integer b) -> a * b);
        assertEquals(3, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withBiFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "product", Tuple.of("col1", "col2"), (Integer a, Integer b) -> a * b);
        assertEquals("product", builder.val().columnNameList().get(0));
    }

    @Test
    public void testDatasetBuilder_addColumn_withTriFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn("sum", Tuple.of("a", "b", "c"), (Integer x, Integer y, Integer z) -> x + y + z);
        assertEquals(4, builder.val().columnNameList().size());
    }

    @Test
    public void testDatasetBuilder_addColumn_atIndex_withTriFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5, 6)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.addColumn(0, "sum", Tuple.of("a", "b", "c"), (Integer x, Integer y, Integer z) -> x + y + z);
        assertEquals("sum", builder.val().columnNameList().get(0));
    }

    @Test
    public void testDatasetBuilder_updateColumn() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateColumn("col1", (Integer val) -> val * 2);
        assertEquals(2, (Integer) builder.val().absolute(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_updateColumns() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateColumns(Arrays.asList("col1", "col2"), (rowIdx, colName, val) -> ((Integer) val) * 10);
        assertEquals(10, (Integer) builder.val().absolute(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_convertColumn() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList("123")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.convertColumn("col1", Integer.class);
        assertTrue(builder.val().absolute(0).get("col1") instanceof Integer);
    }

    @Test
    public void testDatasetBuilder_convertColumns() {
        Dataset dataset = N.newDataset(Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList("123", "456")));
        Map<String, Class<?>> conversions = new HashMap<>();
        conversions.put("col1", Integer.class);
        conversions.put("col2", Integer.class);
        DatasetBuilder builder = Builder.of(dataset);
        builder.convertColumns(conversions);
        assertTrue(builder.val().absolute(0).get("col1") instanceof Integer);
    }

    @Test
    public void testDatasetBuilder_combineColumns() {
        Dataset dataset = N.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Arrays.asList("a", "b"), "sum", arr -> {
            return ((Integer) arr.get(0)) + ((Integer) arr.get(1));
        });
        assertTrue(builder.val().columnNameList().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_combineColumns_withBiFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("a", "b"), Arrays.asList(Arrays.asList(1, 2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Tuple.of("a", "b"), "sum", (Integer x, Integer y) -> x + y);
        assertTrue(builder.val().columnNameList().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_combineColumns_withTriFunction() {
        Dataset dataset = N.newDataset(Arrays.asList("a", "b", "c"), Arrays.asList(Arrays.asList(1, 2, 3)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.combineColumns(Tuple.of("a", "b", "c"), "sum", (Integer x, Integer y, Integer z) -> x + y + z);
        assertTrue(builder.val().columnNameList().contains("sum"));
    }

    @Test
    public void testDatasetBuilder_divideColumn() {
        Dataset dataset = N.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Arrays.asList("col1", "col2"), (String val) -> Arrays.asList(val.split(",")));
        assertTrue(builder.val().columnNameList().contains("col1"));
        assertTrue(builder.val().columnNameList().contains("col2"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withBiConsumer() {
        Dataset dataset = N.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Arrays.asList("col1", "col2"), (String val, Object[] output) -> {
            String[] parts = val.split(",");
            output[0] = parts[0];
            output[1] = parts[1];
        });
        assertTrue(builder.val().columnNameList().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withPair() {
        Dataset dataset = N.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Tuple.of("col1", "col2"), (String val, Pair<Object, Object> output) -> {
            String[] parts = val.split(",");
            output.setLeft(parts[0]);
            output.setRight(parts[1]);
        });
        assertTrue(builder.val().columnNameList().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_divideColumn_withTriple() {
        Dataset dataset = N.newDataset(Arrays.asList("full"), Arrays.asList(Arrays.asList("a,b,c")));
        DatasetBuilder builder = Builder.of(dataset);
        builder.divideColumn("full", Tuple.of("col1", "col2", "col3"), (String val, Triple<Object, Object, Object> output) -> {
            String[] parts = val.split(",");
            output.setLeft(parts[0]);
            output.setMiddle(parts[1]);
            output.setRight(parts[2]);
        });
        assertTrue(builder.val().columnNameList().contains("col1"));
    }

    @Test
    public void testDatasetBuilder_updateAll() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateAll((Integer val) -> val * 2);
        assertEquals(2, (Integer) builder.val().absolute(0).get("col1"));
    }

    @Test
    public void testDatasetBuilder_updateAll_withRowIndexAndColumnName() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.updateAll((rowIdx, colName, val) -> ((Integer) val) + rowIdx);
        assertEquals(1, (Integer) builder.val().absolute(0).get("col1"));
        assertEquals(3, (Integer) builder.val().absolute(1).get("col1"));
    }

    @Test
    public void testDatasetBuilder_replaceIf() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.replaceIf((Integer val) -> val == 2, 0);
        assertEquals(0, (Integer) builder.val().absolute(1).get("col1"));
    }

    @Test
    public void testDatasetBuilder_replaceIf_withPredicate() {
        Dataset dataset = N.newDataset(Arrays.asList("col1"), Arrays.asList(Arrays.asList(1), Arrays.asList(2)));
        DatasetBuilder builder = Builder.of(dataset);
        builder.replaceIf((rowIdx, colName, val) -> ((Integer) val) > 1, 99);
        assertEquals(99, (Integer) builder.val().absolute(1).get("col1"));
    }
}
