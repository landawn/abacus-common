package com.landawn.abacus.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Enumerations100Test extends TestBase {

    @Test
    public void testEmpty() {
        Enumeration<String> empty = Enumerations.empty();
        Assertions.assertFalse(empty.hasMoreElements());
        Assertions.assertThrows(NoSuchElementException.class, () -> empty.nextElement());

        Enumeration<Integer> empty2 = Enumerations.empty();
        Assertions.assertSame(empty, empty2);
    }

    @Test
    public void testJust() {
        Enumeration<String> single = Enumerations.just("Hello");
        Assertions.assertTrue(single.hasMoreElements());
        Assertions.assertEquals("Hello", single.nextElement());
        Assertions.assertFalse(single.hasMoreElements());
        Assertions.assertThrows(NoSuchElementException.class, () -> single.nextElement());

        Enumeration<String> nullEnum = Enumerations.just(null);
        Assertions.assertTrue(nullEnum.hasMoreElements());
        Assertions.assertNull(nullEnum.nextElement());
        Assertions.assertFalse(nullEnum.hasMoreElements());

        Enumeration<Integer> intEnum = Enumerations.just(42);
        Assertions.assertEquals(42, intEnum.nextElement());

        Enumeration<List<String>> listEnum = Enumerations.just(Arrays.asList("a", "b"));
        Assertions.assertEquals(Arrays.asList("a", "b"), listEnum.nextElement());
    }

    @Test
    public void testOf() {
        Enumeration<String> enum1 = Enumerations.of("a", "b", "c");
        Assertions.assertTrue(enum1.hasMoreElements());
        Assertions.assertEquals("a", enum1.nextElement());
        Assertions.assertTrue(enum1.hasMoreElements());
        Assertions.assertEquals("b", enum1.nextElement());
        Assertions.assertTrue(enum1.hasMoreElements());
        Assertions.assertEquals("c", enum1.nextElement());
        Assertions.assertFalse(enum1.hasMoreElements());
        Assertions.assertThrows(NoSuchElementException.class, () -> enum1.nextElement());

        Enumeration<String> emptyEnum = Enumerations.of();
        Assertions.assertFalse(emptyEnum.hasMoreElements());

        Enumeration<String> nullArrayEnum = Enumerations.of((String[]) null);
        Assertions.assertFalse(nullArrayEnum.hasMoreElements());

        Enumeration<Integer> singleEnum = Enumerations.of(100);
        Assertions.assertEquals(100, singleEnum.nextElement());

        Enumeration<String> withNulls = Enumerations.of("a", null, "c");
        Assertions.assertEquals("a", withNulls.nextElement());
        Assertions.assertNull(withNulls.nextElement());
        Assertions.assertEquals("c", withNulls.nextElement());
    }

    @Test
    public void testCreateFromCollection() {
        List<String> list = Arrays.asList("x", "y", "z");
        Enumeration<String> listEnum = Enumerations.create(list);
        Assertions.assertEquals("x", listEnum.nextElement());
        Assertions.assertEquals("y", listEnum.nextElement());
        Assertions.assertEquals("z", listEnum.nextElement());
        Assertions.assertFalse(listEnum.hasMoreElements());

        Set<Integer> set = new LinkedHashSet<>();
        set.add(1);
        set.add(2);
        set.add(3);
        Enumeration<Integer> setEnum = Enumerations.create(set);
        Assertions.assertEquals(1, setEnum.nextElement());
        Assertions.assertEquals(2, setEnum.nextElement());
        Assertions.assertEquals(3, setEnum.nextElement());

        Enumeration<String> emptyEnum = Enumerations.create(new ArrayList<String>());
        Assertions.assertFalse(emptyEnum.hasMoreElements());

        Enumeration<String> nullEnum = Enumerations.create((Collection<String>) null);
        Assertions.assertFalse(nullEnum.hasMoreElements());

        List<String> withNulls = Arrays.asList("a", null, "b");
        Enumeration<String> nullsEnum = Enumerations.create(withNulls);
        Assertions.assertEquals("a", nullsEnum.nextElement());
        Assertions.assertNull(nullsEnum.nextElement());
        Assertions.assertEquals("b", nullsEnum.nextElement());
    }

    @Test
    public void testCreateFromIterator() {
        List<String> list = Arrays.asList("foo", "bar", "baz");
        Enumeration<String> enum1 = Enumerations.create(list.iterator());
        Assertions.assertEquals("foo", enum1.nextElement());
        Assertions.assertEquals("bar", enum1.nextElement());
        Assertions.assertEquals("baz", enum1.nextElement());
        Assertions.assertFalse(enum1.hasMoreElements());

        Enumeration<String> emptyEnum = Enumerations.create(new ArrayList<String>().iterator());
        Assertions.assertFalse(emptyEnum.hasMoreElements());

        Iterator<Integer> iter = new Iterator<>() {
            private int count = 0;

            @Override
            public boolean hasNext() {
                return count < 3;
            }

            @Override
            public Integer next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return count++;
            }
        };

        Enumeration<Integer> delegatingEnum = Enumerations.create(iter);
        Assertions.assertEquals(0, delegatingEnum.nextElement());
        Assertions.assertEquals(1, delegatingEnum.nextElement());
        Assertions.assertEquals(2, delegatingEnum.nextElement());
        Assertions.assertFalse(delegatingEnum.hasMoreElements());
    }

    @Test
    public void testConcatVarargs() {
        Enumeration<String> enum1 = Enumerations.of("a", "b");
        Enumeration<String> enum2 = Enumerations.of("c", "d");
        Enumeration<String> enum3 = Enumerations.of("e", "f");

        Enumeration<String> combined = Enumerations.concat(enum1, enum2, enum3);
        Assertions.assertEquals("a", combined.nextElement());
        Assertions.assertEquals("b", combined.nextElement());
        Assertions.assertEquals("c", combined.nextElement());
        Assertions.assertEquals("d", combined.nextElement());
        Assertions.assertEquals("e", combined.nextElement());
        Assertions.assertEquals("f", combined.nextElement());
        Assertions.assertFalse(combined.hasMoreElements());

        Enumeration<String> empty1 = Enumerations.empty();
        Enumeration<String> nonEmpty = Enumerations.of("x");
        Enumeration<String> empty2 = Enumerations.empty();

        Enumeration<String> combined2 = Enumerations.concat(empty1, nonEmpty, empty2);
        Assertions.assertEquals("x", combined2.nextElement());
        Assertions.assertFalse(combined2.hasMoreElements());

        Enumeration<String> emptyConcat = Enumerations.concat();
        Assertions.assertFalse(emptyConcat.hasMoreElements());

        Enumeration<String> nullConcat = Enumerations.concat((Enumeration<String>[]) null);
        Assertions.assertFalse(nullConcat.hasMoreElements());
    }

    @Test
    public void testConcatCollection() {
        List<Enumeration<Integer>> enums = new ArrayList<>();
        enums.add(Enumerations.of(1, 2));
        enums.add(Enumerations.of(3, 4));
        enums.add(Enumerations.of(5, 6));

        Enumeration<Integer> combined = Enumerations.concat(enums);
        for (int i = 1; i <= 6; i++) {
            Assertions.assertEquals(i, combined.nextElement());
        }
        Assertions.assertFalse(combined.hasMoreElements());

        Enumeration<String> emptyConcat = Enumerations.concat(new ArrayList<Enumeration<String>>());
        Assertions.assertFalse(emptyConcat.hasMoreElements());

        Enumeration<String> nullConcat = Enumerations.concat((Collection<Enumeration<String>>) null);
        Assertions.assertFalse(nullConcat.hasMoreElements());

        List<Enumeration<String>> mixed = new ArrayList<>();
        mixed.add(Enumerations.empty());
        mixed.add(Enumerations.of("a", "b"));
        mixed.add(Enumerations.empty());
        mixed.add(Enumerations.of("c"));
        mixed.add(Enumerations.empty());

        Enumeration<String> mixedConcat = Enumerations.concat(mixed);
        Assertions.assertEquals("a", mixedConcat.nextElement());
        Assertions.assertEquals("b", mixedConcat.nextElement());
        Assertions.assertEquals("c", mixedConcat.nextElement());
        Assertions.assertFalse(mixedConcat.hasMoreElements());
    }

    @Test
    public void testToIterator() {
        Enumeration<String> enum1 = Enumerations.of("hello", "world");
        ObjIterator<String> iter = Enumerations.toIterator(enum1);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("hello", iter.next());
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("world", iter.next());
        Assertions.assertFalse(iter.hasNext());

        ObjIterator<String> emptyIter = Enumerations.toIterator(Enumerations.empty());
        Assertions.assertFalse(emptyIter.hasNext());

        ObjIterator<String> nullIter = Enumerations.toIterator(null);
        Assertions.assertFalse(nullIter.hasNext());

        Enumeration<Integer> countingEnum = new Enumeration<>() {
            private int count = 0;

            @Override
            public boolean hasMoreElements() {
                return count < 3;
            }

            @Override
            public Integer nextElement() {
                return count++;
            }
        };

        ObjIterator<Integer> delegatingIter = Enumerations.toIterator(countingEnum);
        Assertions.assertEquals(0, delegatingIter.next());
        Assertions.assertEquals(1, delegatingIter.next());
        Assertions.assertEquals(2, delegatingIter.next());
    }

    @Test
    public void testToList() {
        Enumeration<String> enum1 = Enumerations.of("one", "two", "three");
        List<String> list = Enumerations.toList(enum1);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("one", list.get(0));
        Assertions.assertEquals("two", list.get(1));
        Assertions.assertEquals("three", list.get(2));

        List<String> emptyList = Enumerations.toList(Enumerations.empty());
        Assertions.assertTrue(emptyList.isEmpty());

        List<String> nullList = Enumerations.toList(null);
        Assertions.assertTrue(nullList.isEmpty());

        Enumeration<String> withNulls = Enumerations.of("a", null, "b");
        List<String> listWithNulls = Enumerations.toList(withNulls);
        Assertions.assertEquals(3, listWithNulls.size());
        Assertions.assertEquals("a", listWithNulls.get(0));
        Assertions.assertNull(listWithNulls.get(1));
        Assertions.assertEquals("b", listWithNulls.get(2));

        list.add("four");
        Assertions.assertEquals(4, list.size());
    }

    @Test
    public void testToSet() {
        Enumeration<String> enum1 = Enumerations.of("red", "green", "blue", "red");
        Set<String> set = Enumerations.toSet(enum1);
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains("red"));
        Assertions.assertTrue(set.contains("green"));
        Assertions.assertTrue(set.contains("blue"));

        Set<String> emptySet = Enumerations.toSet(Enumerations.empty());
        Assertions.assertTrue(emptySet.isEmpty());

        Set<String> nullSet = Enumerations.toSet(null);
        Assertions.assertTrue(nullSet.isEmpty());

        Enumeration<String> withNulls = Enumerations.of("a", null, "b", null);
        Set<String> setWithNulls = Enumerations.toSet(withNulls);
        Assertions.assertEquals(3, setWithNulls.size());
        Assertions.assertTrue(setWithNulls.contains("a"));
        Assertions.assertTrue(setWithNulls.contains("b"));
        Assertions.assertTrue(setWithNulls.contains(null));

        set.add("yellow");
        Assertions.assertEquals(4, set.size());
    }

    @Test
    public void testToCollection() {
        Enumeration<String> enum1 = Enumerations.of("first", "second", "third");
        LinkedList<String> linkedList = Enumerations.toCollection(enum1, LinkedList::new);
        Assertions.assertEquals(3, linkedList.size());
        Assertions.assertEquals("first", linkedList.getFirst());
        Assertions.assertEquals("third", linkedList.getLast());

        Enumeration<Integer> enum2 = Enumerations.of(3, 1, 4, 1, 5);
        TreeSet<Integer> treeSet = Enumerations.toCollection(enum2, TreeSet::new);
        Assertions.assertEquals(4, treeSet.size());
        Assertions.assertEquals(1, treeSet.first());
        Assertions.assertEquals(5, treeSet.last());

        Enumeration<String> enum3 = Enumerations.of("x", "y", "z");
        ArrayDeque<String> deque = Enumerations.toCollection(enum3, ArrayDeque::new);
        Assertions.assertEquals(3, deque.size());
        Assertions.assertEquals("x", deque.pollFirst());
        Assertions.assertEquals("z", deque.pollLast());

        ArrayList<String> emptyList = Enumerations.toCollection(Enumerations.empty(), ArrayList::new);
        Assertions.assertTrue(emptyList.isEmpty());

        HashSet<String> nullSet = Enumerations.toCollection(null, HashSet::new);
        Assertions.assertTrue(nullSet.isEmpty());

        Vector<String> vector = Enumerations.toCollection(Enumerations.of("a", "b"), Vector::new);
        Assertions.assertEquals(2, vector.size());
        Assertions.assertEquals("a", vector.get(0));
        Assertions.assertEquals("b", vector.get(1));
    }
}
