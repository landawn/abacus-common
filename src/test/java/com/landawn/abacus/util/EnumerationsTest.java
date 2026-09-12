package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class EnumerationsTest extends TestBase {

    @Test
    public void testEmptyAndJust() {
        Enumeration<String> empty = Enumerations.empty();
        assertFalse(empty.hasMoreElements());
        assertThrows(NoSuchElementException.class, empty::nextElement);
        assertSame(empty, Enumerations.empty());
        assertSame(Enumerations.<Integer> empty(), Enumerations.<Object> empty());

        Enumeration<String> single = Enumerations.just("Hello");
        assertTrue(single.hasMoreElements());
        assertEquals("Hello", single.nextElement());
        assertFalse(single.hasMoreElements());
        assertThrows(NoSuchElementException.class, single::nextElement);

        Enumeration<String> nullEnum = Enumerations.just(null);
        assertTrue(nullEnum.hasMoreElements());
        assertNull(nullEnum.nextElement());
        assertFalse(nullEnum.hasMoreElements());
        assertEquals(42, Enumerations.just(42).nextElement());
        assertEquals(Arrays.asList("a", "b"), Enumerations.just(Arrays.asList("a", "b")).nextElement());
    }

    @Test
    public void testOf() {
        Enumeration<String> enum1 = Enumerations.of("a", "b", "c");
        assertEquals("a", enum1.nextElement());
        assertEquals("b", enum1.nextElement());
        assertEquals("c", enum1.nextElement());
        assertFalse(enum1.hasMoreElements());
        assertThrows(NoSuchElementException.class, enum1::nextElement);

        assertFalse(Enumerations.of().hasMoreElements());
        assertFalse(Enumerations.of(new String[0]).hasMoreElements());
        assertFalse(Enumerations.of((String[]) null).hasMoreElements());
        assertEquals("only", Enumerations.of("only").nextElement());
        assertEquals(100, Enumerations.of(100).nextElement());

        Enumeration<String> withNulls = Enumerations.of("a", null, "c");
        assertEquals("a", withNulls.nextElement());
        assertNull(withNulls.nextElement());
        assertEquals("c", withNulls.nextElement());
        assertFalse(withNulls.hasMoreElements());

        Enumeration<Integer> two = Enumerations.of(1, 2);
        two.nextElement();
        two.nextElement();
        assertThrows(NoSuchElementException.class, two::nextElement);
    }

    @Test
    public void testCreate() {
        Enumeration<String> listEnum = Enumerations.create(Arrays.asList("x", "y", "z"));
        assertEquals("x", listEnum.nextElement());
        assertEquals("y", listEnum.nextElement());
        assertEquals("z", listEnum.nextElement());
        assertFalse(listEnum.hasMoreElements());

        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Enumeration<Integer> setEnum = Enumerations.create(set);
        assertEquals(1, setEnum.nextElement());
        assertEquals(2, setEnum.nextElement());
        assertEquals(3, setEnum.nextElement());

        assertFalse(Enumerations.create(new ArrayList<String>()).hasMoreElements());
        assertFalse(Enumerations.create((Collection<String>) null).hasMoreElements());
        assertEquals("solo", Enumerations.create(Arrays.asList("solo")).nextElement());

        Enumeration<String> nullsEnum = Enumerations.create(Arrays.asList("a", null, "b"));
        assertEquals("a", nullsEnum.nextElement());
        assertNull(nullsEnum.nextElement());
        assertEquals("b", nullsEnum.nextElement());

        Enumeration<String> fromIter = Enumerations.create(Arrays.asList("foo", "bar", "baz").iterator());
        assertEquals("foo", fromIter.nextElement());
        assertEquals("bar", fromIter.nextElement());
        assertEquals("baz", fromIter.nextElement());
        assertFalse(fromIter.hasMoreElements());
        assertFalse(Enumerations.create(new ArrayList<String>().iterator()).hasMoreElements());
        assertEquals("one", Enumerations.create(Arrays.asList("one").iterator()).nextElement());

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
        Enumeration<Integer> delegating = Enumerations.create(iter);
        assertEquals(0, delegating.nextElement());
        assertEquals(1, delegating.nextElement());
        assertEquals(2, delegating.nextElement());
        assertFalse(delegating.hasMoreElements());

        Enumeration<Integer> exhausted = Enumerations.create(Arrays.asList(1).iterator());
        exhausted.nextElement();
        assertFalse(exhausted.hasMoreElements());
        assertThrows(NoSuchElementException.class, exhausted::nextElement);
    }

    @Test
    public void testConcat() {
        Enumeration<String> combined = Enumerations.concat(Enumerations.of("a", "b"), Enumerations.of("c", "d"), Enumerations.of("e", "f"));
        assertEquals("a", combined.nextElement());
        assertEquals("b", combined.nextElement());
        assertEquals("c", combined.nextElement());
        assertEquals("d", combined.nextElement());
        assertEquals("e", combined.nextElement());
        assertEquals("f", combined.nextElement());
        assertFalse(combined.hasMoreElements());

        Enumeration<String> skipEmpty = Enumerations.concat(Enumerations.empty(), Enumerations.of("x"), Enumerations.empty());
        assertEquals("x", skipEmpty.nextElement());
        assertFalse(skipEmpty.hasMoreElements());

        Enumeration<String> middleEmpty = Enumerations.concat(Enumerations.of("a"), Enumerations.empty(), Enumerations.of("b"));
        assertEquals("a", middleEmpty.nextElement());
        assertEquals("b", middleEmpty.nextElement());
        assertFalse(middleEmpty.hasMoreElements());

        assertFalse(Enumerations.concat().hasMoreElements());
        assertFalse(Enumerations.concat((Enumeration<String>[]) null).hasMoreElements());
        assertFalse(Enumerations.concat(Enumerations.empty(), Enumerations.empty(), Enumerations.empty()).hasMoreElements());

        Enumeration<String> single = Enumerations.concat(Enumerations.of("a", "b"));
        assertEquals("a", single.nextElement());
        assertEquals("b", single.nextElement());

        List<Enumeration<Integer>> enums = new ArrayList<>();
        enums.add(Enumerations.of(1, 2));
        enums.add(Enumerations.of(3, 4));
        enums.add(Enumerations.of(5, 6));
        Enumeration<Integer> fromList = Enumerations.concat(enums);
        for (int i = 1; i <= 6; i++) {
            assertEquals(i, fromList.nextElement());
        }
        assertFalse(fromList.hasMoreElements());

        assertFalse(Enumerations.concat(new ArrayList<Enumeration<String>>()).hasMoreElements());
        assertFalse(Enumerations.concat((Collection<Enumeration<String>>) null).hasMoreElements());

        List<Enumeration<String>> mixed = Arrays.asList(Enumerations.empty(), Enumerations.of("a", "b"), Enumerations.empty(), Enumerations.of("c"),
                Enumerations.empty());
        Enumeration<String> mixedConcat = Enumerations.concat(mixed);
        assertEquals("a", mixedConcat.nextElement());
        assertEquals("b", mixedConcat.nextElement());
        assertEquals("c", mixedConcat.nextElement());
        assertFalse(mixedConcat.hasMoreElements());

        Enumeration<Integer> nextWithoutHasMore = Enumerations.concat(Arrays.asList(Enumerations.of(1, 2), Enumerations.of(3)));
        assertEquals(1, nextWithoutHasMore.nextElement());
        assertEquals(2, nextWithoutHasMore.nextElement());
        assertEquals(3, nextWithoutHasMore.nextElement());
        assertFalse(nextWithoutHasMore.hasMoreElements());
        assertThrows(NoSuchElementException.class, nextWithoutHasMore::nextElement);

        Enumeration<String> exhausted = Enumerations.concat(Arrays.asList(Enumerations.of("x")));
        exhausted.nextElement();
        assertThrows(NoSuchElementException.class, exhausted::nextElement);
    }

    @Test
    public void testToIterator() {
        ObjIterator<String> iter = Enumerations.toIterator(Enumerations.of("hello", "world"));
        assertEquals("hello", iter.next());
        assertEquals("world", iter.next());
        assertFalse(iter.hasNext());
        assertFalse(Enumerations.toIterator(Enumerations.empty()).hasNext());
        assertFalse(Enumerations.toIterator(null).hasNext());
        ObjIterator<String> single = Enumerations.toIterator(Enumerations.just("single"));
        assertEquals("single", single.next());
        assertFalse(single.hasNext());

        Enumeration<Integer> counting = new Enumeration<>() {
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
        ObjIterator<Integer> delegating = Enumerations.toIterator(counting);
        assertEquals(0, delegating.next());
        assertEquals(1, delegating.next());
        assertEquals(2, delegating.next());
    }

    @Test
    public void testToListSetCollection() {
        List<String> list = Enumerations.toList(Enumerations.of("one", "two", "three"));
        assertEquals(Arrays.asList("one", "two", "three"), list);
        list.add("four");
        assertEquals(4, list.size());
        assertTrue(Enumerations.toList(Enumerations.empty()).isEmpty());
        assertTrue(Enumerations.toList(null).isEmpty());
        assertEquals(Arrays.asList("a", null, "b"), Enumerations.toList(Enumerations.of("a", null, "b")));
        assertEquals(List.of(42), Enumerations.toList(Enumerations.just(42)));
        List<String> mutable = Enumerations.toList(Enumerations.of("a", "b"));
        mutable.add("c");
        mutable.remove("a");
        assertEquals(2, mutable.size());

        Set<String> set = Enumerations.toSet(Enumerations.of("red", "green", "blue", "red"));
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList("red", "green", "blue")));
        set.add("yellow");
        assertEquals(4, set.size());
        assertEquals(1, Enumerations.toSet(Enumerations.of("x", "x", "x")).size());
        assertEquals(Set.of("a", "b", "c"), Enumerations.toSet(Enumerations.of("a", "a", "b", "b", "c")));
        assertTrue(Enumerations.toSet(Enumerations.empty()).isEmpty());
        assertTrue(Enumerations.toSet(null).isEmpty());
        Set<String> withNulls = Enumerations.toSet(Enumerations.of("a", null, "b", null));
        assertEquals(3, withNulls.size());
        assertTrue(withNulls.contains(null));
        Set<String> mutableSet = Enumerations.toSet(Enumerations.of("a"));
        mutableSet.add("b");
        mutableSet.remove("a");
        assertEquals(1, mutableSet.size());

        LinkedList<String> linkedList = Enumerations.toCollection(Enumerations.of("first", "second", "third"), LinkedList::new);
        assertEquals("first", linkedList.getFirst());
        assertEquals("third", linkedList.getLast());
        TreeSet<Integer> treeSet = Enumerations.toCollection(Enumerations.of(3, 1, 4, 1, 5), TreeSet::new);
        assertEquals(1, treeSet.first());
        assertEquals(5, treeSet.last());
        ArrayDeque<String> deque = Enumerations.toCollection(Enumerations.of("x", "y", "z"), ArrayDeque::new);
        assertEquals("x", deque.pollFirst());
        assertEquals("z", deque.pollLast());
        assertTrue(Enumerations.toCollection(Enumerations.empty(), ArrayList::new).isEmpty());
        assertTrue(Enumerations.toCollection(null, HashSet::new).isEmpty());
        Vector<String> vector = Enumerations.toCollection(Enumerations.of("a", "b"), Vector::new);
        assertEquals(List.of("a", "b"), vector);
        assertEquals(List.of("only"), Enumerations.toCollection(Enumerations.just("only"), ArrayList::new));
        assertThrows(IllegalArgumentException.class, () -> Enumerations.<String, ArrayList<String>> toCollection(null, null));
        assertThrows(IllegalArgumentException.class, () -> Enumerations.<String, ArrayList<String>> toCollection(null, () -> null));
    }

    // Finding 134 (2026-09-08): "supplier result" contains a space and is longer than 9 characters, so
    // N.checkArgNotNull threw it verbatim - the caller saw the bare fragment instead of a message. The sibling
    // collectors (BooleanList, IntList, ...) all say "supplier returned null".
    @Test
    public void reviewFixes20260908_toCollectionReportsANullSuppliedCollectionWithAFullMessage() {
        final IllegalArgumentException missingSupplier = assertThrows(IllegalArgumentException.class,
                () -> Enumerations.<String, ArrayList<String>> toCollection(Enumerations.of("a"), null));
        assertEquals("'supplier' cannot be null", missingSupplier.getMessage());

        final IllegalArgumentException nullCollection = assertThrows(IllegalArgumentException.class,
                () -> Enumerations.<String, ArrayList<String>> toCollection(Enumerations.of("a"), () -> null));
        assertEquals("supplier returned null", nullCollection.getMessage());
    }
}
