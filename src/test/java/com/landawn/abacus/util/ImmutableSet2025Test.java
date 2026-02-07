package com.landawn.abacus.util;

import static org.junit.Assert.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ImmutableSet2025Test extends TestBase {

    @Test
    public void test_empty() {
        ImmutableSet<String> empty = ImmutableSet.empty();
        assertNotNull(empty);
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());

        ImmutableSet<String> empty2 = ImmutableSet.empty();
        assertSame(empty, empty2);

        ImmutableSet<Integer> emptyInt = ImmutableSet.empty();
        assertSame(empty, emptyInt);
    }

    //    @Test
    //    public void test_just() {
    //        ImmutableSet<String> set = ImmutableSet.just("hello");
    //        assertNotNull(set);
    //        assertEquals(1, set.size());
    //        assertTrue(set.contains("hello"));
    //        assertFalse(set.contains("world"));
    //
    //        ImmutableSet<String> setWithNull = ImmutableSet.just(null);
    //        assertEquals(1, setWithNull.size());
    //        assertTrue(setWithNull.contains(null));
    //
    //        assertThrows(UnsupportedOperationException.class, () -> set.add("new"));
    //    }

    @Test
    public void test_of_single() {
        ImmutableSet<String> set = ImmutableSet.of("test");
        assertNotNull(set);
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));

        ImmutableSet<Integer> setWithNull = ImmutableSet.of((Integer) null);
        assertEquals(1, setWithNull.size());
        assertTrue(setWithNull.contains(null));

        assertThrows(UnsupportedOperationException.class, () -> set.add("new"));
    }

    @Test
    public void test_of_two() {
        ImmutableSet<String> set = ImmutableSet.of("first", "second");
        assertNotNull(set);
        assertEquals(2, set.size());
        assertTrue(set.contains("first"));
        assertTrue(set.contains("second"));

        ImmutableSet<String> setWithDup = ImmutableSet.of("same", "same");
        assertEquals(1, setWithDup.size());
        assertTrue(setWithDup.contains("same"));

        ImmutableSet<String> setWithNull = ImmutableSet.of("value", null);
        assertEquals(2, setWithNull.size());
        assertTrue(setWithNull.contains("value"));
        assertTrue(setWithNull.contains(null));
    }

    @Test
    public void test_of_three() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c");
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        ImmutableSet<String> setWithDup = ImmutableSet.of("x", "x", "y");
        assertEquals(2, setWithDup.size());
        assertTrue(setWithDup.contains("x"));
        assertTrue(setWithDup.contains("y"));
    }

    @Test
    public void test_of_four() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4);
        assertNotNull(set);
        assertEquals(4, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(4));

        ImmutableSet<Integer> setWithDup = ImmutableSet.of(1, 1, 2, 2);
        assertEquals(2, setWithDup.size());
    }

    @Test
    public void test_of_five() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5);
        assertNotNull(set);
        assertEquals(5, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(5));

        ImmutableSet<Integer> setWithDup = ImmutableSet.of(1, 1, 1, 2, 2);
        assertEquals(2, setWithDup.size());
    }

    @Test
    public void test_of_six() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5, 6);
        assertNotNull(set);
        assertEquals(6, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(6));
    }

    @Test
    public void test_of_seven() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7);
        assertNotNull(set);
        assertEquals(7, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(7));
    }

    @Test
    public void test_of_eight() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8);
        assertNotNull(set);
        assertEquals(8, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(8));
    }

    @Test
    public void test_of_nine() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        assertNotNull(set);
        assertEquals(9, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(9));
    }

    @Test
    public void test_of_ten() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        assertNotNull(set);
        assertEquals(10, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(10));

        ImmutableSet<Integer> setWithNull = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, null);
        assertEquals(10, setWithNull.size());
        assertTrue(setWithNull.contains(null));
    }

    //    @Test
    //    public void test_of_varargs() {
    //        String[] array = { "a", "b", "c", "d" };
    //        ImmutableSet<String> set = ImmutableSet.of(array);
    //        assertNotNull(set);
    //        assertEquals(4, set.size());
    //        assertTrue(set.contains("a"));
    //        assertTrue(set.contains("d"));
    //
    //        ImmutableSet<String> emptySet = ImmutableSet.of((String[]) null);
    //        assertNotNull(emptySet);
    //        assertTrue(emptySet.isEmpty());
    //
    //        ImmutableSet<String> emptySet2 = ImmutableSet.of(new String[0]);
    //        assertNotNull(emptySet2);
    //        assertTrue(emptySet2.isEmpty());
    //        assertSame(ImmutableSet.empty(), emptySet2);
    //
    //        String[] arrayWithDup = { "x", "y", "x", "z" };
    //        ImmutableSet<String> setWithDup = ImmutableSet.of(arrayWithDup);
    //        assertEquals(3, setWithDup.size());
    //
    //        String[] arrayWithNull = { "a", null, "b" };
    //        ImmutableSet<String> setWithNull = ImmutableSet.of(arrayWithNull);
    //        assertEquals(3, setWithNull.size());
    //        assertTrue(setWithNull.contains(null));
    //
    //        String[] ordered = { "first", "second", "third" };
    //        ImmutableSet<String> orderedSet = ImmutableSet.of(ordered);
    //        List<String> list = new ArrayList<>(orderedSet);
    //        assertEquals("first", list.get(0));
    //        assertEquals("second", list.get(1));
    //        assertEquals("third", list.get(2));
    //    }

    @Test
    public void test_copyOf() {
        List<String> list = Arrays.asList("a", "b", "c");
        ImmutableSet<String> setFromList = ImmutableSet.copyOf(list);
        assertNotNull(setFromList);
        assertEquals(3, setFromList.size());
        assertTrue(setFromList.contains("a"));

        Set<Integer> hashSet = new HashSet<>(Arrays.asList(1, 2, 3));
        ImmutableSet<Integer> setFromHashSet = ImmutableSet.copyOf(hashSet);
        assertEquals(3, setFromHashSet.size());

        Set<String> linkedHashSet = new LinkedHashSet<>(Arrays.asList("x", "y", "z"));
        ImmutableSet<String> setFromLinkedHashSet = ImmutableSet.copyOf(linkedHashSet);
        assertEquals(3, setFromLinkedHashSet.size());
        List<String> orderedList = new ArrayList<>(setFromLinkedHashSet);
        assertEquals("x", orderedList.get(0));

        Set<Integer> treeSet = new TreeSet<>(Arrays.asList(3, 1, 2));
        ImmutableSet<Integer> setFromTreeSet = ImmutableSet.copyOf(treeSet);
        assertEquals(3, setFromTreeSet.size());

        ImmutableSet<String> emptySet = ImmutableSet.copyOf((Collection<String>) null);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        assertSame(ImmutableSet.empty(), emptySet);

        ImmutableSet<String> emptySet2 = ImmutableSet.copyOf(new ArrayList<>());
        assertTrue(emptySet2.isEmpty());
        assertSame(ImmutableSet.empty(), emptySet2);

        ImmutableSet<String> immutable = ImmutableSet.of("a", "b");
        ImmutableSet<String> copied = ImmutableSet.copyOf(immutable);
        assertSame(immutable, copied);

        List<String> mutableList = new ArrayList<>(Arrays.asList("m", "n"));
        ImmutableSet<String> setCopy = ImmutableSet.copyOf(mutableList);
        mutableList.add("o");
        assertEquals(2, setCopy.size());
        assertFalse(setCopy.contains("o"));

        List<String> listWithDup = Arrays.asList("a", "b", "a", "c");
        ImmutableSet<String> setNoDup = ImmutableSet.copyOf(listWithDup);
        assertEquals(3, setNoDup.size());
    }

    @Test
    public void test_wrap() {
        Set<String> mutableSet = new HashSet<>(Arrays.asList("a", "b", "c"));
        ImmutableSet<String> wrapped = ImmutableSet.wrap(mutableSet);
        assertNotNull(wrapped);
        assertEquals(3, wrapped.size());

        mutableSet.add("d");
        assertEquals(4, wrapped.size());
        assertTrue(wrapped.contains("d"));

        ImmutableSet<String> emptySet = ImmutableSet.wrap(null);
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        assertSame(ImmutableSet.empty(), emptySet);

        ImmutableSet<String> immutable = ImmutableSet.of("x", "y");
        ImmutableSet<String> wrappedImmutable = ImmutableSet.wrap(immutable);
        assertSame(immutable, wrappedImmutable);

        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2));
        ImmutableSet<Integer> wrappedSet = ImmutableSet.wrap(set);
        assertThrows(UnsupportedOperationException.class, () -> wrappedSet.add(3));
    }

    @Test
    public void test_wrap_collection_deprecated() {
        Collection<String> collection = Arrays.asList("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> ImmutableSet.wrap(collection));
    }

    @Test
    public void test_builder_default() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        assertNotNull(builder);

        ImmutableSet<String> emptySet = ImmutableSet.<String> builder().build();
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());

        ImmutableSet<String> set = ImmutableSet.<String> builder().add("a").add("b").add("c").build();
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void test_builder_withHolder() {
        Set<String> holder = new LinkedHashSet<>();
        ImmutableSet<String> set = ImmutableSet.builder(holder).add("first").add("second").add("third").build();

        assertEquals(3, set.size());
        List<String> list = new ArrayList<>(set);
        assertEquals("first", list.get(0));
        assertEquals("second", list.get(1));
        assertEquals("third", list.get(2));

        Set<Integer> prePopulated = new HashSet<>(Arrays.asList(1, 2));
        ImmutableSet<Integer> setFromPrePopulated = ImmutableSet.builder(prePopulated).add(3).build();
        assertEquals(3, setFromPrePopulated.size());
        assertTrue(setFromPrePopulated.contains(1));
        assertTrue(setFromPrePopulated.contains(3));
    }

    @Test
    public void test_builder_add_single() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        ImmutableSet.Builder<String> result = builder.add("element");

        assertSame(builder, result);

        ImmutableSet<String> set = builder.build();
        assertEquals(1, set.size());
        assertTrue(set.contains("element"));

        ImmutableSet<String> setWithNull = ImmutableSet.<String> builder().add((String) null).build();
        assertEquals(1, setWithNull.size());
        assertTrue(setWithNull.contains(null));

        ImmutableSet<String> setWithDup = ImmutableSet.<String> builder().add("same").add("same").build();
        assertEquals(1, setWithDup.size());
    }

    @Test
    public void test_builder_add_varargs() {
        ImmutableSet<String> set = ImmutableSet.<String> builder().add("a", "b", "c").build();
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        ImmutableSet<String> setNullArray = ImmutableSet.<String> builder().add((String[]) null).build();
        assertTrue(setNullArray.isEmpty());

        ImmutableSet<String> setEmptyArray = ImmutableSet.<String> builder().add(new String[0]).build();
        assertTrue(setEmptyArray.isEmpty());

        ImmutableSet<String> setWithNull = ImmutableSet.<String> builder().add("x", null, "y").build();
        assertEquals(3, setWithNull.size());
        assertTrue(setWithNull.contains(null));

        ImmutableSet<String> setWithDup = ImmutableSet.<String> builder().add("a", "b", "a", "c").build();
        assertEquals(3, setWithDup.size());

        ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
        ImmutableSet.Builder<Integer> result = builder.add(1, 2, 3);
        assertSame(builder, result);
    }

    @Test
    public void test_builder_addAll_collection() {
        List<String> list = Arrays.asList("a", "b", "c");
        ImmutableSet<String> set = ImmutableSet.<String> builder().addAll(list).build();
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));

        ImmutableSet<String> setNullColl = ImmutableSet.<String> builder().addAll((Collection<String>) null).build();
        assertTrue(setNullColl.isEmpty());

        ImmutableSet<String> setEmptyColl = ImmutableSet.<String> builder().addAll(new ArrayList<>()).build();
        assertTrue(setEmptyColl.isEmpty());

        List<String> listWithDup = Arrays.asList("x", "y", "x");
        ImmutableSet<String> setNoDup = ImmutableSet.<String> builder().addAll(listWithDup).build();
        assertEquals(2, setNoDup.size());

        ImmutableSet<String> combined = ImmutableSet.<String> builder().add("first").addAll(Arrays.asList("second", "third")).add("fourth").build();
        assertEquals(4, combined.size());

        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        ImmutableSet.Builder<String> result = builder.addAll(list);
        assertSame(builder, result);
    }

    @Test
    public void test_builder_addAll_iterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iterator = list.iterator();
        ImmutableSet<String> set = ImmutableSet.<String> builder().addAll(iterator).build();
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));

        ImmutableSet<String> setNullIter = ImmutableSet.<String> builder().addAll((Iterator<String>) null).build();
        assertTrue(setNullIter.isEmpty());

        Iterator<String> emptyIter = new ArrayList<String>().iterator();
        ImmutableSet<String> setEmptyIter = ImmutableSet.<String> builder().addAll(emptyIter).build();
        assertTrue(setEmptyIter.isEmpty());

        List<String> listWithDup = Arrays.asList("x", "y", "x", "z");
        ImmutableSet<String> setNoDup = ImmutableSet.<String> builder().addAll(listWithDup.iterator()).build();
        assertEquals(3, setNoDup.size());

        ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
        ImmutableSet.Builder<Integer> result = builder.addAll(Arrays.asList(1, 2, 3).iterator());
        assertSame(builder, result);
    }

    @Test
    public void test_builder_build() {
        ImmutableSet<String> set = ImmutableSet.<String> builder().add("a").add("b", "c").addAll(Arrays.asList("d", "e")).build();

        assertNotNull(set);
        assertEquals(5, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("e"));

        assertThrows(UnsupportedOperationException.class, () -> set.add("f"));
        assertThrows(UnsupportedOperationException.class, () -> set.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> set.clear());

        ImmutableSet.Builder<String> builder = ImmutableSet.<String> builder().add("x", "y");
        ImmutableSet<String> set1 = builder.build();
        assertEquals(2, set1.size());
    }

    @Test
    public void test_immutability() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c");

        assertThrows(UnsupportedOperationException.class, () -> set.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> set.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> set.clear());
        assertThrows(UnsupportedOperationException.class, () -> set.addAll(Arrays.asList("x", "y")));
        assertThrows(UnsupportedOperationException.class, () -> set.removeAll(Arrays.asList("a", "b")));
        assertThrows(UnsupportedOperationException.class, () -> set.retainAll(Arrays.asList("a")));

        Iterator<String> iterator = set.iterator();
        assertTrue(iterator.hasNext());
        iterator.next();
        assertThrows(UnsupportedOperationException.class, () -> iterator.remove());
    }

    @Test
    public void test_setOperations() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c");

        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertFalse(set.contains("d"));
        assertFalse(set.contains(null));

        assertTrue(set.containsAll(Arrays.asList("a", "b")));
        assertFalse(set.containsAll(Arrays.asList("a", "d")));
        assertTrue(set.containsAll(new ArrayList<>()));

        assertEquals(3, set.size());
        assertFalse(set.isEmpty());

        ImmutableSet<String> emptySet = ImmutableSet.empty();
        assertEquals(0, emptySet.size());
        assertTrue(emptySet.isEmpty());

        Object[] array = set.toArray();
        assertEquals(3, array.length);

        String[] stringArray = set.toArray(new String[0]);
        assertEquals(3, stringArray.length);

        String[] largerArray = set.toArray(new String[5]);
        assertEquals(5, largerArray.length);

        Iterator<String> iter = set.iterator();
        int count = 0;
        while (iter.hasNext()) {
            assertNotNull(iter.next());
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void test_equals_hashCode() {
        ImmutableSet<String> set1 = ImmutableSet.of("a", "b", "c");
        ImmutableSet<String> set2 = ImmutableSet.of("a", "b", "c");
        ImmutableSet<String> set3 = ImmutableSet.of("a", "b", "d");

        assertEquals(set1, set2);
        assertFalse(set1.equals(set3));

        assertEquals(set1.hashCode(), set2.hashCode());

        Set<String> hashSet = new HashSet<>(Arrays.asList("a", "b", "c"));
        assertNotEquals(set1, hashSet);
        assertEquals(hashSet, set1);
    }

    @Test
    public void test_toString() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b");
        String str = set.toString();
        assertNotNull(str);
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));

        ImmutableSet<String> empty = ImmutableSet.empty();
        assertNotNull(empty.toString());
    }

    @Test
    public void test_orderPreservation() {
        List<String> list = Arrays.asList("first", "second", "third", "fourth");
        ImmutableSet<String> setFromList = ImmutableSet.copyOf(list);

        List<String> resultList = new ArrayList<>(setFromList);
        assertEquals("first", resultList.get(0));
        assertEquals("second", resultList.get(1));
        assertEquals("third", resultList.get(2));
        assertEquals("fourth", resultList.get(3));

        Set<String> linkedHashSet = new LinkedHashSet<>(Arrays.asList("one", "two", "three"));
        ImmutableSet<String> setFromLinkedHashSet = ImmutableSet.copyOf(linkedHashSet);

        List<String> resultList2 = new ArrayList<>(setFromLinkedHashSet);
        assertEquals("one", resultList2.get(0));
        assertEquals("two", resultList2.get(1));
        assertEquals("three", resultList2.get(2));

        ImmutableSet<String> orderedSet = ImmutableSet.of("alpha", "beta", "gamma");
        List<String> orderedList = new ArrayList<>(orderedSet);
        assertEquals("alpha", orderedList.get(0));
        assertEquals("beta", orderedList.get(1));
        assertEquals("gamma", orderedList.get(2));
    }

    @Test
    public void test_nullElementSupport() {
        ImmutableSet<String> setWithNull = ImmutableSet.of((String) null);
        assertTrue(setWithNull.contains(null));
        assertEquals(1, setWithNull.size());

        ImmutableSet<String> setWithNullAndOthers = ImmutableSet.of("a", null, "b");
        assertTrue(setWithNullAndOthers.contains(null));
        assertTrue(setWithNullAndOthers.contains("a"));
        assertEquals(3, setWithNullAndOthers.size());

        ImmutableSet<String> setFromArray = ImmutableSet.of("x", null, "y");
        assertTrue(setFromArray.contains(null));
        assertEquals(3, setFromArray.size());

        List<String> listWithNull = Arrays.asList("m", null, "n");
        ImmutableSet<String> setFromList = ImmutableSet.copyOf(listWithNull);
        assertTrue(setFromList.contains(null));
        assertEquals(3, setFromList.size());

        ImmutableSet<String> setFromBuilder = ImmutableSet.<String> builder().add("p").add((String) null).add("q").build();
        assertTrue(setFromBuilder.contains(null));
        assertEquals(3, setFromBuilder.size());
    }

    @Test
    public void test_multipleNullElements() {
        ImmutableSet<String> set = ImmutableSet.of(null, null, "a", null);
        assertEquals(2, set.size());
        assertTrue(set.contains(null));
        assertTrue(set.contains("a"));
    }

    @Test
    public void test_largeSet() {
        ImmutableSet.Builder<Integer> builder = ImmutableSet.builder();
        for (int i = 0; i < 100; i++) {
            builder.add(i);
        }
        ImmutableSet<Integer> largeSet = builder.build();

        assertEquals(100, largeSet.size());
        assertTrue(largeSet.contains(0));
        assertTrue(largeSet.contains(99));
        assertFalse(largeSet.contains(100));

        assertThrows(UnsupportedOperationException.class, () -> largeSet.add(100));
    }

}
