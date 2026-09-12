package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableListTest extends TestBase {

    private static final class MutableImmutableNamedList<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
    }

    @Test
    public void testEmpty() {
        ImmutableList<String> emptyList = ImmutableList.empty();
        Assertions.assertTrue(emptyList.isEmpty());
        Assertions.assertEquals(0, emptyList.size());
        Assertions.assertSame(ImmutableList.empty(), emptyList);
    }

    @Test
    public void testReverse_Empty() {
        ImmutableList<String> list = ImmutableList.empty();
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        Assertions.assertSame(list, reversed);
    }

    @Test
    public void testSize() {
        Assertions.assertEquals(0, ImmutableList.empty().size());
        Assertions.assertEquals(1, ImmutableList.of("a").size());
        Assertions.assertEquals(3, ImmutableList.of("a", "b", "c").size());
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(ImmutableList.empty().isEmpty());
        Assertions.assertFalse(ImmutableList.of("a").isEmpty());
        Assertions.assertFalse(ImmutableList.of("a", "b").isEmpty());
    }

    @Test
    public void testOf_TwoElements() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
    }

    @Test
    public void testOf_ThreeElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(2, list.get(1));
        Assertions.assertEquals(3, list.get(2));
    }

    @Test
    public void testOf_FourElements() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("d", list.get(3));
    }

    @Test
    public void testOf_FiveElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5);
        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(5, list.get(4));
    }

    @Test
    public void testOf_SixElements() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f");
        Assertions.assertEquals(6, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("f", list.get(5));
    }

    @Test
    public void testOf_SevenElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5, 6, 7);
        Assertions.assertEquals(7, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(7, list.get(6));
    }

    @Test
    public void testOf_EightElements() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h");
        Assertions.assertEquals(8, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("h", list.get(7));
    }

    @Test
    public void testOf_NineElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Assertions.assertEquals(9, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(9, list.get(8));
    }

    @Test
    public void testOf_TenElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Assertions.assertEquals(10, list.size());
        Assertions.assertEquals(1, list.get(0));
        Assertions.assertEquals(10, list.get(9));
    }

    @Test
    public void testOf_VarArgs() {
        ImmutableList<String> list = ImmutableList.of("one", "two", "three", "four");
        Assertions.assertEquals(4, list.size());
        Assertions.assertEquals("one", list.get(0));
        Assertions.assertEquals("two", list.get(1));
        Assertions.assertEquals("three", list.get(2));
        Assertions.assertEquals("four", list.get(3));
    }

    @Test
    public void testReverse() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();

        Assertions.assertEquals(3, reversed.size());
        Assertions.assertEquals("c", reversed.get(0));
        Assertions.assertEquals("b", reversed.get(1));
        Assertions.assertEquals("a", reversed.get(2));
    }

    @Test
    public void testReverse_Contains() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();

        Assertions.assertTrue(reversed.contains("a"));
        Assertions.assertTrue(reversed.contains("c"));
        Assertions.assertTrue(reversed.contains("e"));
        Assertions.assertFalse(reversed.contains("x"));
    }

    @Test
    public void testReverse_IndexOf() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();

        Assertions.assertEquals(4, reversed.indexOf("a"));
        Assertions.assertEquals(3, reversed.indexOf("b"));
        Assertions.assertEquals(2, reversed.indexOf("c"));
        Assertions.assertEquals(1, reversed.indexOf("d"));
        Assertions.assertEquals(0, reversed.indexOf("e"));
        Assertions.assertEquals(-1, reversed.indexOf("x"));
    }

    @Test
    public void testReverse_LastIndexOf() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();

        Assertions.assertEquals(4, reversed.lastIndexOf("a"));
        Assertions.assertEquals(0, reversed.lastIndexOf("e"));
    }

    @Test
    public void testReverse_SubList() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        ImmutableList<String> sub = reversed.subList(1, 3);

        Assertions.assertEquals(2, sub.size());
        Assertions.assertEquals("d", sub.get(0));
        Assertions.assertEquals("c", sub.get(1));
    }

    @Test
    public void testReverse_Size() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        Assertions.assertEquals(4, reversed.size());
    }

    @Test
    public void testReverse_Iterator() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        ObjIterator<String> iter = reversed.iterator();

        List<String> collected = new ArrayList<>();
        while (iter.hasNext()) {
            collected.add(iter.next());
        }

        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), collected);
    }

    @Test
    public void testReverse_ListIterator() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        ImmutableListIterator<String> iter = reversed.listIterator();

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertFalse(iter.hasPrevious());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertTrue(iter.hasPrevious());
        Assertions.assertEquals("b", iter.previous());

        ImmutableListIterator<String> iterAt2 = reversed.listIterator(2);
        Assertions.assertEquals("a", iterAt2.next());
        Assertions.assertFalse(iterAt2.hasNext());
    }

    @Test
    public void testIterator() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ObjIterator<String> iter = list.iterator();

        List<String> collected = new ArrayList<>();
        while (iter.hasNext()) {
            collected.add(iter.next());
        }

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testToArray() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        Object[] array = list.toArray();

        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    @Test
    public void testContainsAll() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");

        Assertions.assertTrue(list.containsAll(Arrays.asList("a", "c")));
        Assertions.assertTrue(list.containsAll(Arrays.asList("b", "d")));
        Assertions.assertFalse(list.containsAll(Arrays.asList("a", "x")));
    }

    @Test
    public void testEquals() {
        ImmutableList<String> list1 = ImmutableList.of("a", "b", "c");
        ImmutableList<String> list2 = ImmutableList.of("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(list1, list2);
        Assertions.assertEquals(list1, list3);
    }

    @Test
    public void testHashCode() {
        ImmutableList<String> list1 = ImmutableList.of("a", "b", "c");
        ImmutableList<String> list2 = ImmutableList.of("a", "b", "c");

        Assertions.assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testReverse_Operations() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();

        Assertions.assertTrue(reversed.contains("c"));

        Assertions.assertEquals(0, reversed.indexOf("e"));
        Assertions.assertEquals(4, reversed.indexOf("a"));

        Assertions.assertEquals(0, reversed.lastIndexOf("e"));
        Assertions.assertEquals(4, reversed.lastIndexOf("a"));

        ImmutableList<String> sub = reversed.subList(1, 3);
        Assertions.assertEquals("d", sub.get(0));
        Assertions.assertEquals("c", sub.get(1));
    }

    @Test
    public void testReverse_IndexOf_WithDuplicates() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "a", "b", "a");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        // reversed: ["a", "b", "a", "b", "a"]
        // forward has "a" at 0,2,4 so lastIndexOf("a") is 4 -> reversed indexOf is 5-1-4=0
        Assertions.assertEquals(0, reversed.indexOf("a"));
        // forward has "b" at 1,3 so lastIndexOf("b") is 3 -> reversed indexOf is 5-1-3=1
        Assertions.assertEquals(1, reversed.indexOf("b"));
    }

    @Test
    public void testReverse_LastIndexOf_WithDuplicates() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "a", "b", "a");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        // forward has "a" at 0,2,4 so indexOf("a") is 0 -> reversed lastIndexOf is 5-1-0=4
        Assertions.assertEquals(4, reversed.lastIndexOf("a"));
        // forward has "b" at 1,3 so indexOf("b") is 1 -> reversed lastIndexOf is 5-1-1=3
        Assertions.assertEquals(3, reversed.lastIndexOf("b"));
    }

    //
    @Test
    public void testOf_SingleElement() {
        ImmutableList<Integer> list = ImmutableList.of(42);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(42, list.get(0));
    }

    //
    //
    @Test
    public void testOf_VarArgs_WithNullElement() {
        ImmutableList<String> list = ImmutableList.of("a", null, "c");
        Assertions.assertEquals(3, list.size());
        Assertions.assertNull(list.get(1));
    }

    @Test
    public void testReverse_DoubleReverse() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        ImmutableList<String> doubleReversed = (ImmutableList<String>) reversed.reversed();

        Assertions.assertSame(list, doubleReversed);
    }

    @Test
    public void testReverse_SingleElement() {
        ImmutableList<String> list = ImmutableList.of("single");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        Assertions.assertSame(list, reversed);
    }

    @Test
    public void testReverse_ToArray() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();

        Object[] array = reversed.toArray();
        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("c", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("a", array[2]);

        String[] typedArray = reversed.toArray(new String[0]);
        Assertions.assertEquals(3, typedArray.length);
        Assertions.assertEquals("c", typedArray[0]);
        Assertions.assertEquals("a", typedArray[2]);

        String[] largerArray = reversed.toArray(new String[5]);
        Assertions.assertEquals(5, largerArray.length);
        Assertions.assertEquals("c", largerArray[0]);
        Assertions.assertEquals("a", largerArray[2]);
        Assertions.assertNull(largerArray[3]);
    }

    @Test
    public void testContains() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");

        Assertions.assertTrue(list.contains("a"));
        Assertions.assertTrue(list.contains("b"));
        Assertions.assertTrue(list.contains("c"));
        Assertions.assertFalse(list.contains("d"));
        Assertions.assertFalse(list.contains(null));
    }

    @Test
    public void testContains_WithNull() {
        ImmutableList<String> list = ImmutableList.of("a", null, "c");
        Assertions.assertTrue(list.contains(null));
    }

    @Test
    public void testToString() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        String str = list.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("a"));
        Assertions.assertTrue(str.contains("b"));
        Assertions.assertTrue(str.contains("c"));

        ImmutableList<String> empty = ImmutableList.empty();
        Assertions.assertEquals("[]", empty.toString());
    }

    @Test
    public void testMutationMethods_Add_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.add("c"));
    }

    @Test
    public void testMutationMethods_AddAll_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.addAll(Arrays.asList("c", "d")));
    }

    @Test
    public void testMutationMethods_Remove_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.remove("a"));
    }

    @Test
    public void testMutationMethods_RemoveAll_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.removeAll(Arrays.asList("a")));
    }

    @Test
    public void testMutationMethods_RetainAll_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.retainAll(Arrays.asList("a")));
    }

    @Test
    public void testMutationMethods_Clear_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.clear());
    }

    @Test
    public void testMutationMethods_RemoveIf_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.removeIf(s -> s.equals("a")));
    }

    @Test
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableList<String> list = ImmutableList.of("a", "b");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.add("c"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.add(0, "c"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.addAll(Arrays.asList("c")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.addAll(0, Arrays.asList("c")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.removeAll(Arrays.asList("a")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.retainAll(Arrays.asList("a")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.set(0, "c"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.replaceAll(UnaryOperator.identity()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.sort(null));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.removeIf(s -> true));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.removeIf(null));
    }

    @Test
    public void testReverse_Iterator_Remove_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = (ImmutableList<String>) list.reversed();
        ObjIterator<String> iter = reversed.iterator();
        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    @Test
    public void testCopyOf() {
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ImmutableList<String> immutable = ImmutableList.copyOf(mutable);

        Assertions.assertEquals(3, immutable.size());
        Assertions.assertEquals("a", immutable.get(0));
        Assertions.assertEquals("b", immutable.get(1));
        Assertions.assertEquals("c", immutable.get(2));

        mutable.add("d");
        Assertions.assertEquals(3, immutable.size());
    }

    @Test
    public void testCopyOf_FromNonListCollection() {
        java.util.Set<String> set = new java.util.LinkedHashSet<>(Arrays.asList("x", "y", "z"));
        ImmutableList<String> list = ImmutableList.copyOf(set);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("x", list.get(0));
    }

    @Test
    public void testCopyOf_Array() {
        String[] array = { "a", "b", "c" };
        ImmutableList<String> list = ImmutableList.copyOf(array);
        Assertions.assertNotNull(list);
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));

        // Verify defensive copy
        array[0] = "x";
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testCopyOf_Array_Empty() {
        ImmutableList<String> list = ImmutableList.copyOf(new String[0]);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.isEmpty());
        Assertions.assertSame(ImmutableList.empty(), list);
    }

    @Test
    public void testCopyOf_Array_Null() {
        ImmutableList<String> list = ImmutableList.copyOf((String[]) null);
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list.isEmpty());
        Assertions.assertSame(ImmutableList.empty(), list);
    }

    @Test
    public void testCopyOf_Array_SingleElement() {
        String[] array = { "only" };
        ImmutableList<String> list = ImmutableList.copyOf(array);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("only", list.get(0));
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableList<String> original = ImmutableList.of("a", "b");
        ImmutableList<String> copy = ImmutableList.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableList<String> list = ImmutableList.copyOf(new ArrayList<>());
        Assertions.assertTrue(list.isEmpty());
        Assertions.assertSame(ImmutableList.empty(), list);
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableList<String> list = ImmutableList.copyOf((Collection<String>) null);
        Assertions.assertTrue(list.isEmpty());
        Assertions.assertSame(ImmutableList.empty(), list);
    }

    @Test
    public void testCopyOf_WithNullElements() {
        List<String> withNull = new ArrayList<>();
        withNull.add("a");
        withNull.add(null);
        withNull.add("c");

        ImmutableList<String> list = ImmutableList.copyOf(withNull);
        Assertions.assertEquals(3, list.size());
        Assertions.assertNull(list.get(1));
    }

    @Test
    public void testWithNullElements() {
        List<String> withNull = new ArrayList<>();
        withNull.add("a");
        withNull.add(null);
        withNull.add("c");

        ImmutableList<String> list = ImmutableList.copyOf(withNull);
        Assertions.assertEquals(3, list.size());
        Assertions.assertNull(list.get(1));
        Assertions.assertTrue(list.contains(null));
        Assertions.assertEquals(1, list.indexOf(null));
    }

    @Test
    public void testWrap() {
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        ImmutableList<String> wrapped = ImmutableList.wrap(mutable);

        Assertions.assertEquals(2, wrapped.size());

        mutable.add("c");
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertEquals("c", wrapped.get(2));
    }

    @Test
    public void testWrapDoesNotTrustBackingListClassNameForImmutability() {
        MutableImmutableNamedList<String> backing = new MutableImmutableNamedList<>();
        backing.add("a");

        ImmutableList<String> wrapped = ImmutableList.wrap(backing);
        Iterator<String> iterator = wrapped.iterator();
        iterator.next();
        ListIterator<String> listIterator = wrapped.listIterator();
        listIterator.next();

        Assertions.assertThrows(UnsupportedOperationException.class, iterator::remove);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> listIterator.set("b"));
        Assertions.assertEquals(Arrays.asList("a"), backing);
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableList<String> original = ImmutableList.of("a");
        ImmutableList<String> wrapped = ImmutableList.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableList<String> wrapped = ImmutableList.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
        Assertions.assertSame(ImmutableList.empty(), wrapped);
    }

    @Test
    public void testWrap_Collection_Deprecated() {
        Collection<String> collection = new HashSet<>(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableList.wrap(collection);
        });
    }

    @Test
    public void testGet() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));
    }

    @Test
    public void testGet_IndexOutOfBounds() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
    }

    @Test
    public void testIndexOf() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "b");
        Assertions.assertEquals(0, list.indexOf("a"));
        Assertions.assertEquals(1, list.indexOf("b"));
        Assertions.assertEquals(2, list.indexOf("c"));
        Assertions.assertEquals(-1, list.indexOf("d"));
    }

    @Test
    public void testIndexOf_WithNull() {
        ImmutableList<String> list = ImmutableList.of("a", null, "c");
        Assertions.assertEquals(1, list.indexOf(null));

        ImmutableList<String> listWithoutNull = ImmutableList.of("a", "b", "c");
        Assertions.assertEquals(-1, listWithoutNull.indexOf(null));
    }

    @Test
    public void testLastIndexOf() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "b");
        Assertions.assertEquals(0, list.lastIndexOf("a"));
        Assertions.assertEquals(3, list.lastIndexOf("b"));
        Assertions.assertEquals(2, list.lastIndexOf("c"));
        Assertions.assertEquals(-1, list.lastIndexOf("d"));
    }

    @Test
    public void testLastIndexOf_WithNull() {
        ImmutableList<String> list = ImmutableList.of("a", null, "c", null);
        Assertions.assertEquals(3, list.lastIndexOf(null));
    }

    @Test
    public void testListIterator() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableListIterator<String> iter = list.listIterator();

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testListIterator_WithIndex() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");
        ImmutableListIterator<String> iter = list.listIterator(2);

        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("c", iter.next());
        Assertions.assertTrue(iter.hasPrevious());
        Assertions.assertEquals("c", iter.previous());
        Assertions.assertEquals("b", iter.previous());
    }

    @Test
    public void testListIterator_WithIndex_Bounds() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");

        Assertions.assertNotNull(list.listIterator(0));
        Assertions.assertNotNull(list.listIterator(3));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(4));
    }

    @Test
    public void testSubList() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
        ImmutableList<String> sub = list.subList(1, 4);

        Assertions.assertEquals(3, sub.size());
        Assertions.assertEquals("b", sub.get(0));
        Assertions.assertEquals("c", sub.get(1));
        Assertions.assertEquals("d", sub.get(2));
    }

    @Test
    public void testSubList_FullRange() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> sub = list.subList(0, 3);
        Assertions.assertEquals(3, sub.size());
        Assertions.assertEquals(list.get(0), sub.get(0));
    }

    @Test
    public void testSubList_Empty() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> sub = list.subList(1, 1);
        Assertions.assertTrue(sub.isEmpty());
    }

    @Test
    public void testSubList_Invalid() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.subList(-1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> list.subList(0, 4));
        Assertions.assertThrows(IllegalArgumentException.class, () -> list.subList(2, 1));
    }

    @Test
    public void testSubList_IsImmutable() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d");
        ImmutableList<String> sub = list.subList(1, 3);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> sub.add("x"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> sub.remove(0));
    }

    @Test
    public void testAddAll_WithIndex_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.addAll(0, Arrays.asList("c")));
    }

    @Test
    public void testSet_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.set(0, "c"));
    }

    @Test
    public void testAdd_WithIndex_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.add(0, "c"));
    }

    @Test
    public void testRemove_WithIndex_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    }

    @Test
    public void testReplaceAll_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        UnaryOperator<String> operator = String::toUpperCase;
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.replaceAll(operator));
    }

    @Test
    public void testSort_ThrowsException() {
        ImmutableList<String> list = ImmutableList.of("c", "a", "b");
        Comparator<String> comparator = String::compareTo;
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.sort(comparator));
    }

    @Test
    public void testSort_WithNull_ThrowsUnsupportedOperationException() {
        // null is a legal argument to List.sort (it means natural ordering), so a read-only list must
        // report that it cannot be sorted rather than rejecting the argument.
        ImmutableList<String> list = ImmutableList.of("a", "b");
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.sort(null));
    }

    @Test
    public void testBuilder() {
        ImmutableList<String> list = ImmutableList.<String> builder().add("one").add("two", "three").addAll(Arrays.asList("four", "five")).build();

        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals("one", list.get(0));
        Assertions.assertEquals("two", list.get(1));
        Assertions.assertEquals("three", list.get(2));
        Assertions.assertEquals("four", list.get(3));
        Assertions.assertEquals("five", list.get(4));
    }

    @Test
    public void testBuilder_Add_Varargs() {
        ImmutableList<String> list = ImmutableList.<String> builder().add("a", "b", "c").build();
        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testBuilder_AddAll_Collection() {
        List<String> source = Arrays.asList("a", "b", "c");
        ImmutableList<String> list = ImmutableList.<String> builder().addAll(source).build();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testBuilder_AddAll_Iterator() {
        List<String> source = Arrays.asList("a", "b", "c");
        ImmutableList<String> list = ImmutableList.<String> builder().addAll(source.iterator()).build();

        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals("a", list.get(0));
    }

    @Test
    public void testBuilder_WithBackingList() {
        List<String> backing = new ArrayList<>();
        ImmutableList<String> list = ImmutableList.builder(backing).add("a").add("b").build();

        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, backing.size());
        Assertions.assertEquals("a", backing.get(0));
        Assertions.assertEquals("b", backing.get(1));
    }

    @Test
    public void testBuilder_WithIterator() {
        List<String> source = Arrays.asList("a", "b", "c");
        ImmutableList<String> list = ImmutableList.<String> builder().addAll(source.iterator()).build();

        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testBuilder_Empty() {
        ImmutableList<String> list = ImmutableList.<String> builder().build();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testBuilder_Add_Single() {
        ImmutableList<String> list = ImmutableList.<String> builder().add("single").build();
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals("single", list.get(0));
    }

    @Test
    public void testBuilder_Add_Varargs_Null() {
        ImmutableList<String> list = ImmutableList.<String> builder().add((String[]) null).build();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testBuilder_Add_Varargs_Empty() {
        ImmutableList<String> list = ImmutableList.<String> builder().add(new String[0]).build();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testBuilder_AddAll_Collection_Null() {
        ImmutableList<String> list = ImmutableList.<String> builder().addAll((Collection<String>) null).build();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testBuilder_AddAll_Collection_Empty() {
        ImmutableList<String> list = ImmutableList.<String> builder().addAll(Collections.emptyList()).build();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testBuilder_AddAll_Iterator_Null() {
        ImmutableList<String> list = ImmutableList.<String> builder().addAll((Iterator<String>) null).build();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testBuilder_AddAll_Iterator_Empty() {
        ImmutableList<String> list = ImmutableList.<String> builder().addAll(Collections.emptyIterator()).build();
        Assertions.assertTrue(list.isEmpty());
    }

    @Test
    public void testBuilder_Chaining() {
        ImmutableList.Builder<String> builder = ImmutableList.<String> builder();
        ImmutableList.Builder<String> result1 = builder.add("a");
        ImmutableList.Builder<String> result2 = result1.add("b", "c");
        ImmutableList.Builder<String> result3 = result2.addAll(Arrays.asList("d"));
        ImmutableList.Builder<String> result4 = result3.addAll(Arrays.asList("e").iterator());

        Assertions.assertSame(builder, result1);
        Assertions.assertSame(builder, result2);
        Assertions.assertSame(builder, result3);
        Assertions.assertSame(builder, result4);

        ImmutableList<String> list = builder.build();
        Assertions.assertEquals(5, list.size());
    }

    @Test
    public void testReversed_EqualsAndHashCode() {
        ImmutableList<Integer> forward = ImmutableList.of(1, 2, 3);
        ImmutableList<Integer> reversed = (ImmutableList<Integer>) forward.reversed();

        // Regression: the reversed view's own equals()/hashCode() must reflect its
        // public [3, 2, 1] order (per the java.util.List contract), not the forward
        // backing list's [1, 2, 3] order. (Before the fix, ReverseImmutableList did
        // not override equals/hashCode and reported the forward list's values.)
        Assertions.assertTrue(reversed.equals(Arrays.asList(3, 2, 1)));
        Assertions.assertTrue(reversed.equals(ImmutableList.of(3, 2, 1)));
        Assertions.assertEquals(Arrays.asList(3, 2, 1).hashCode(), reversed.hashCode());
        Assertions.assertEquals(ImmutableList.of(3, 2, 1).hashCode(), reversed.hashCode());

        // AbstractList.equals from the other side also works (iterator-based).
        Assertions.assertEquals(Arrays.asList(3, 2, 1), reversed);

        // reversed view must NOT equal the original forward order
        Assertions.assertFalse(reversed.equals(forward));
        Assertions.assertFalse(reversed.equals(ImmutableList.of(1, 2, 3)));
        Assertions.assertFalse(reversed.equals(Arrays.asList(1, 2, 3)));

        Assertions.assertTrue(reversed.equals(reversed));
        Assertions.assertFalse(reversed.equals("not a list"));

        // The forward-side asymmetry is now fixed by the isReorderedView() hook: the raw
        // backing-store fast path is skipped when the ARGUMENT is a reordered view, so the
        // general ImmutableCollection unwrap contract for plain instances is untouched.
        Assertions.assertFalse(forward.equals(reversed));
        Assertions.assertTrue(ImmutableList.of(3, 2, 1).equals(reversed));
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testReversedToStringReflectsIterationOrder() {
        // regression: ReverseImmutableList inherited toString() from the (forward-ordered)
        // backing collection and printed "[1, 2, 3]"
        Assertions.assertEquals("[3, 2, 1]", ImmutableList.of(1, 2, 3).reversed().toString());
    }

    // ------------------------------------------------------------------------------------------
    // copyOf() must produce an independent value, even from a wrap()/builder-backed view.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testCopyOf_copiesAWrappedView() {
        final List<String> live = new ArrayList<>(Arrays.asList("x", "y"));
        final ImmutableList<String> view = ImmutableList.wrap(live);
        final ImmutableList<String> copy = ImmutableList.copyOf(view);

        Assertions.assertNotSame(view, copy);

        live.add("z");

        Assertions.assertEquals(Arrays.asList("x", "y", "z"), view);
        Assertions.assertEquals(Arrays.asList("x", "y"), copy);
    }

    @Test
    public void testCopyOf_returnsSameInstanceForAnOwningList() {
        final ImmutableList<String> owned = ImmutableList.of("a", "b");
        Assertions.assertSame(owned, ImmutableList.copyOf(owned));
        Assertions.assertSame(ImmutableList.empty(), ImmutableList.copyOf(ImmutableList.empty()));

        final ImmutableList<String> copied = ImmutableList.copyOf(new ArrayList<>(Arrays.asList("a", "b")));
        Assertions.assertSame(copied, ImmutableList.copyOf(copied));
    }

    @Test
    public void testCopyOf_ownershipOfSubList() {
        final ImmutableList<String> owned = ImmutableList.of("a", "b", "c");
        final ImmutableList<String> ownedSub = owned.subList(0, 2);
        Assertions.assertSame(ownedSub, ImmutableList.copyOf(ownedSub));

        final List<String> live = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final ImmutableList<String> viewSub = ImmutableList.wrap(live).subList(0, 2);
        final ImmutableList<String> copy = ImmutableList.copyOf(viewSub);
        Assertions.assertNotSame(viewSub, copy);

        live.set(0, "changed");
        Assertions.assertEquals(Arrays.asList("changed", "b"), viewSub);
        Assertions.assertEquals(Arrays.asList("a", "b"), copy);
    }

    @Test
    public void testCopyOf_builderResults() {
        // the no-arg builder owns its storage -> copyOf can return it unchanged
        final ImmutableList.Builder<String> privateBuilder = ImmutableList.<String> builder().add("a");
        final ImmutableList<String> fromPrivateStorage = privateBuilder.build();
        Assertions.assertSame(fromPrivateStorage, ImmutableList.copyOf(fromPrivateStorage));
        final ImmutableList<String> rebuiltPrivateStorage = privateBuilder.build();
        Assertions.assertSame(rebuiltPrivateStorage, ImmutableList.copyOf(rebuiltPrivateStorage));
        Assertions.assertEquals(fromPrivateStorage, rebuiltPrivateStorage);

        // builder(holder) leaves the holder with the caller -> copyOf must copy
        final List<String> holder = new ArrayList<>();
        final ImmutableList.Builder<String> holderBuilder = ImmutableList.builder(holder).add("a");
        final ImmutableList<String> fromHolder = holderBuilder.build();
        final ImmutableList<String> copy = ImmutableList.copyOf(fromHolder);
        Assertions.assertNotSame(fromHolder, copy);

        holder.add("b");
        Assertions.assertEquals(Arrays.asList("a", "b"), fromHolder);
        Assertions.assertEquals(Collections.singletonList("a"), copy);

        final ImmutableList<String> rebuiltHolder = holderBuilder.build();
        final ImmutableList<String> laterCopy = ImmutableList.copyOf(rebuiltHolder);
        Assertions.assertNotSame(rebuiltHolder, laterCopy);
        Assertions.assertEquals(Arrays.asList("a", "b"), laterCopy);
        holder.clear();
        Assertions.assertTrue(rebuiltHolder.isEmpty());
        Assertions.assertEquals(Arrays.asList("a", "b"), laterCopy);
        Assertions.assertEquals(Collections.singletonList("a"), copy);
    }

    // ------------------------------------------------------------------------------------------
    // reversed() over a live backing list
    // ------------------------------------------------------------------------------------------

    @Test
    public void testReversed_tracksAGrowingWrappedList() {
        final List<Integer> live = new ArrayList<>(Arrays.asList(1, 2));
        final List<Integer> reversed = ImmutableList.wrap(live).reversed();

        Assertions.assertEquals(Arrays.asList(2, 1), reversed);

        live.add(3);

        Assertions.assertEquals(Arrays.asList(3, 2, 1), reversed);
        Assertions.assertEquals(3, reversed.size());
        Assertions.assertEquals(3, reversed.get(0));
        Assertions.assertEquals(1, reversed.get(2));
        Assertions.assertEquals("[3, 2, 1]", reversed.toString());
        Assertions.assertArrayEquals(new Object[] { 3, 2, 1 }, reversed.toArray());
        Assertions.assertEquals(Arrays.asList(3, 2, 1).hashCode(), reversed.hashCode());
        Assertions.assertEquals(Arrays.asList(3, 2, 1), reversed.stream().toList());
    }

    @Test
    public void testReversed_tracksAShrinkingWrappedList() {
        final List<Integer> live = new ArrayList<>(Arrays.asList(1, 2, 3));
        final List<Integer> reversed = ImmutableList.wrap(live).reversed();

        live.remove(2);

        Assertions.assertEquals(Arrays.asList(2, 1), reversed);
        Assertions.assertEquals(0, reversed.indexOf(2));
        Assertions.assertEquals(-1, reversed.indexOf(3));
        Assertions.assertEquals(-1, reversed.lastIndexOf(3));
    }

    @Test
    public void testReversed_singleElementWrappedListIsARealView() {
        final List<Integer> live = new ArrayList<>(Collections.singletonList(1));
        final ImmutableList<Integer> wrapped = ImmutableList.wrap(live);
        final List<Integer> reversed = wrapped.reversed();

        Assertions.assertNotSame(wrapped, reversed);

        live.add(2);

        Assertions.assertEquals(Arrays.asList(2, 1), reversed);
    }

    @Test
    public void testReversed_shortCircuitsOnlyForAnOwningList() {
        final ImmutableList<Integer> single = ImmutableList.of(1);
        Assertions.assertSame(single, single.reversed());
        Assertions.assertSame(ImmutableList.empty(), ImmutableList.empty().reversed());

        final ImmutableList<Integer> three = ImmutableList.of(1, 2, 3);
        Assertions.assertNotSame(three, three.reversed());
        Assertions.assertSame(three, ((ImmutableList<Integer>) three.reversed()).reversed());
    }

    @Test
    public void testReversed_iterationOrderOfEveryTraversal() {
        final ImmutableList<Integer> reversed = (ImmutableList<Integer>) ImmutableList.of(1, 2, 3).reversed();

        Assertions.assertEquals(Arrays.asList(3, 2, 1), reversed.stream().toList());
        Assertions.assertEquals(Arrays.asList(3, 2, 1), reversed.parallelStream().toList());

        final List<Integer> seen = new ArrayList<>();
        reversed.forEach(seen::add);
        Assertions.assertEquals(Arrays.asList(3, 2, 1), seen);

        final List<Integer> spliterated = new ArrayList<>();
        reversed.spliterator().forEachRemaining(spliterated::add);
        Assertions.assertEquals(Arrays.asList(3, 2, 1), spliterated);

        Assertions.assertThrows(NullPointerException.class, () -> reversed.forEach(null));
    }

    @Test
    public void testReversed_listIteratorTracksLiveSize() {
        final List<Integer> live = new ArrayList<>(Arrays.asList(1, 2));
        final List<Integer> reversed = ImmutableList.wrap(live).reversed();

        live.add(3);

        final ListIterator<Integer> iter = reversed.listIterator();
        Assertions.assertEquals(3, iter.next());
        Assertions.assertEquals(2, iter.next());
        Assertions.assertEquals(1, iter.next());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertEquals(1, iter.previous());
        Assertions.assertEquals(2, iter.previous());
        Assertions.assertThrows(UnsupportedOperationException.class, iter::remove);
    }

    @Test
    public void testReversed_getOutOfBounds() {
        final List<Integer> reversed = ImmutableList.of(1, 2, 3).reversed();

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> reversed.get(-1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> reversed.get(3));
    }

    @Test
    public void testReversed_subListExceptionsMatchTheForwardList() {
        final ImmutableList<Integer> forward = ImmutableList.of(1, 2, 3);
        final ImmutableList<Integer> reversed = (ImmutableList<Integer>) forward.reversed();

        // an inverted range is IllegalArgumentException in both directions (as in java.util.List)
        Assertions.assertThrows(IllegalArgumentException.class, () -> forward.subList(2, 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> reversed.subList(2, 1));

        // an out-of-range endpoint is IndexOutOfBoundsException in both directions
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> forward.subList(-1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> reversed.subList(-1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> forward.subList(0, 4));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> reversed.subList(0, 4));

        Assertions.assertEquals(Arrays.asList(3, 2), reversed.subList(0, 2));
        Assertions.assertEquals(Collections.emptyList(), reversed.subList(1, 1));
    }

    // ------------------------------------------------------------------------------------------
    // RandomAccess + spliterator delegation
    // ------------------------------------------------------------------------------------------

    @Test
    public void testRandomAccessMarkerFollowsTheBackingList() {
        Assertions.assertTrue(ImmutableList.of(1, 2, 3) instanceof java.util.RandomAccess);
        Assertions.assertTrue(ImmutableList.copyOf(Arrays.asList(1, 2, 3)) instanceof java.util.RandomAccess);
        Assertions.assertTrue(ImmutableList.wrap(new ArrayList<>(Arrays.asList(1, 2))) instanceof java.util.RandomAccess);
        Assertions.assertTrue(ImmutableList.of(1, 2, 3).subList(0, 2) instanceof java.util.RandomAccess);
        Assertions.assertTrue(ImmutableList.empty() instanceof java.util.RandomAccess);

        Assertions.assertFalse(ImmutableList.wrap(new java.util.LinkedList<>(Arrays.asList(1, 2))) instanceof java.util.RandomAccess);
    }

    @Test
    public void testSpliteratorDelegatesToTheBackingList() {
        final ImmutableList<Integer> list = ImmutableList.of(1, 2, 3);
        final java.util.Spliterator<Integer> sp = list.spliterator();

        Assertions.assertTrue(sp.hasCharacteristics(java.util.Spliterator.ORDERED));
        Assertions.assertTrue(sp.hasCharacteristics(java.util.Spliterator.SIZED));
        Assertions.assertTrue(sp.hasCharacteristics(java.util.Spliterator.SUBSIZED));
        Assertions.assertEquals(3, sp.getExactSizeIfKnown());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), list.stream().toList());
    }

    @Test
    public void testForEachDelegatesButKeepsOrderAndNullCheck() {
        final List<Integer> seen = new ArrayList<>();
        ImmutableList.of(1, 2, 3).forEach(seen::add);

        Assertions.assertEquals(Arrays.asList(1, 2, 3), seen);
        Assertions.assertThrows(NullPointerException.class, () -> ImmutableList.of(1).forEach(null));
    }

    // ------------------------------------------------------------------------------------------
    // Builder is consumed by build()
    // ------------------------------------------------------------------------------------------

    @Test
    public void testBuilderIsConsumedByBuild() {
        final ImmutableList.Builder<String> builder = ImmutableList.builder();
        final ImmutableList<String> built = builder.add("one").build();

        Assertions.assertEquals(Collections.singletonList("one"), built);
        Assertions.assertThrows(IllegalStateException.class, () -> builder.add("two"));
        Assertions.assertThrows(IllegalStateException.class, () -> builder.add("two", "three"));
        Assertions.assertThrows(IllegalStateException.class, () -> builder.addAll(Arrays.asList("two")));
        Assertions.assertThrows(IllegalStateException.class, () -> builder.addAll(Arrays.asList("two").iterator()));

        // the previously returned list is untouched, and build() stays repeatable
        Assertions.assertEquals(Collections.singletonList("one"), built);
        Assertions.assertEquals(built, builder.build());
    }

    @Test
    public void testReversed_nestedWithSubList() {
        final ImmutableList<Integer> forward = ImmutableList.of(1, 2, 3, 4, 5);
        final ImmutableList<Integer> reversed = (ImmutableList<Integer>) forward.reversed();

        Assertions.assertEquals(Arrays.asList(5, 4, 3, 2, 1), reversed);
        Assertions.assertEquals(Arrays.asList(4, 3), reversed.subList(1, 3));
        Assertions.assertEquals(Arrays.asList(3, 4), reversed.subList(1, 3).reversed());
        Assertions.assertEquals(Arrays.asList(4, 3, 2), forward.subList(1, 4).reversed());
        Assertions.assertEquals(Collections.emptyList(), reversed.subList(2, 2));
        Assertions.assertEquals(Collections.singletonList(3), reversed.subList(2, 3));
        Assertions.assertEquals(forward, reversed.reversed());
        Assertions.assertSame(forward, reversed.reversed());

        // a sublist of a reversed view is still fully read-only
        Assertions.assertThrows(UnsupportedOperationException.class, () -> reversed.subList(0, 2).add(9));
    }

    @Test
    public void testReversed_isDeclaredToReturnImmutableList() {
        // reversed() narrows java.util.List.reversed() to ImmutableList<E>: none of the assignments below
        // needs a cast, which is also why ReverseImmutableList.subList() no longer has to downcast.
        final ImmutableList<Integer> forward = ImmutableList.of(1, 2, 3, 4, 5);
        final ImmutableList<Integer> reversed = forward.reversed();
        final ImmutableList<Integer> roundTrip = reversed.reversed();
        final ImmutableList<Integer> reversedSub = reversed.subList(1, 4);

        Assertions.assertEquals(Arrays.asList(5, 4, 3, 2, 1), reversed);
        Assertions.assertSame(forward, roundTrip);
        Assertions.assertEquals(Arrays.asList(4, 3, 2), reversedSub);

        // the java.util.List.reversed() override still dispatches here through the generated bridge
        final List<Integer> viaListInterface = ((List<Integer>) forward).reversed();
        Assertions.assertTrue(viaListInterface instanceof ImmutableList);
        Assertions.assertEquals(Arrays.asList(5, 4, 3, 2, 1), viaListInterface);

        // a wrap()-backed list never takes the identity shortcut, even when empty
        final ImmutableList<Integer> wrapped = ImmutableList.wrap(new ArrayList<Integer>());
        Assertions.assertNotSame(wrapped, wrapped.reversed());
    }

    // ------------------------------------------------------------------------------------------
    // empty() singleton: spliterator contract, null-friendliness (G47-003), and the SequencedCollection
    // mutators / reversed-view range checks (G47-007, G47-008, G47-009)
    // ------------------------------------------------------------------------------------------

    @Test
    public void testEmptySpliteratorReportsOrderedLikeEveryOtherList() {
        // java.util.List.spliterator() promises SIZED and ORDERED, but Collections.emptyList() drops
        // ORDERED, so the shared empty singleton must not use it.
        final java.util.Spliterator<Object> sp = ImmutableList.empty().spliterator();

        Assertions.assertEquals(java.util.Spliterator.ORDERED | java.util.Spliterator.SIZED | java.util.Spliterator.SUBSIZED, sp.characteristics());
        Assertions.assertTrue(sp.hasCharacteristics(java.util.Spliterator.ORDERED));
        Assertions.assertEquals(0, sp.getExactSizeIfKnown());

        // the empty singleton now answers exactly as a non-empty list and as an empty wrap() of the same content
        Assertions.assertEquals(ImmutableList.of(1).spliterator().characteristics(), sp.characteristics());
        Assertions.assertEquals(ImmutableList.wrap(new ArrayList<Integer>()).spliterator().characteristics(), sp.characteristics());

        // Stream.concat intersects both sides' characteristics, so an unordered empty prefix used to strip
        // ORDERED from the whole stream and make a parallel findFirst() stop meaning "first".
        final List<Integer> big = new ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            big.add(i);
        }

        Assertions.assertTrue(java.util.stream.Stream.concat(ImmutableList.<Integer> empty().stream(), big.stream())
                .spliterator()
                .hasCharacteristics(java.util.Spliterator.ORDERED));

        for (int i = 0; i < 50; i++) {
            Assertions.assertEquals(java.util.Optional.of(0),
                    java.util.stream.Stream.concat(ImmutableList.<Integer> empty().stream(), big.stream()).parallel().findFirst());
        }
    }

    @Test
    public void testEmptyStillAcceptsNullQueriesAndStaysRandomAccess() {
        // pins the other half of the fix above: List.of() would report the right characteristics but its
        // contains(null)/indexOf(null) throw NullPointerException, which this empty list must never do.
        Assertions.assertFalse(ImmutableList.empty().contains(null));
        Assertions.assertEquals(-1, ImmutableList.empty().indexOf(null));
        Assertions.assertEquals(-1, ImmutableList.empty().lastIndexOf(null));
        Assertions.assertFalse(ImmutableList.empty().containsAll(Collections.singletonList(null)));

        Assertions.assertTrue(ImmutableList.empty() instanceof java.util.RandomAccess);
        Assertions.assertEquals(Collections.emptyList(), ImmutableList.empty());
        Assertions.assertEquals(1, ImmutableList.empty().hashCode());
        Assertions.assertEquals("[]", ImmutableList.empty().toString());
        Assertions.assertSame(ImmutableList.empty(), ImmutableList.copyOf(Collections.emptyList()));
        Assertions.assertSame(ImmutableList.empty(), ImmutableList.empty().reversed());
    }

    @Test
    public void testReversedListIteratorReportsABadIndexLikeTheForwardList() {
        final ImmutableList<String> forward = ImmutableList.of("a", "b", "c");
        final ImmutableList<String> reversed = forward.reversed();

        final IndexOutOfBoundsException forwardTooBig = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> forward.listIterator(4));
        final IndexOutOfBoundsException reversedTooBig = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> reversed.listIterator(4));
        final IndexOutOfBoundsException reversedNegative = Assertions.assertThrows(IndexOutOfBoundsException.class, () -> reversed.listIterator(-1));

        Assertions.assertEquals("Index: 4, Size: 3", forwardTooBig.getMessage());
        Assertions.assertEquals("Index: 4, Size: 3", reversedTooBig.getMessage());
        Assertions.assertEquals("Index: -1, Size: 3", reversedNegative.getMessage());

        // the accepted range is unchanged: 0 .. size
        Assertions.assertEquals("c", reversed.listIterator(0).next());
        Assertions.assertFalse(reversed.listIterator(3).hasNext());
        Assertions.assertTrue(reversed.listIterator(3).hasPrevious());
    }

    @Test
    public void testAddFirstAndAddLastAreDeprecatedOverridesLikeTheOtherMutators() throws Exception {
        // addFirst/addLast were the only List mutators left un-overridden, so they were the only ones a
        // caller could reach without the compile-time deprecation warning this class raises for every other.
        Assertions.assertTrue(ImmutableList.class.getDeclaredMethod("addFirst", Object.class).isAnnotationPresent(Deprecated.class));
        Assertions.assertTrue(ImmutableList.class.getDeclaredMethod("addLast", Object.class).isAnnotationPresent(Deprecated.class));

        final ImmutableList<String> list = ImmutableList.of("a");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.addFirst("b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.addLast("b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> ImmutableList.<String> empty().addFirst("b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> list.reversed().addLast("b"));
        Assertions.assertEquals(Collections.singletonList("a"), list);
    }

    @Test
    public void testReversedViewOfAnOwningListIsItselfOwning() {
        // the class javadoc and copyOf()'s own javadoc list the subList(...) and reversed() views of an
        // owning list among the owning instances, which is what lets copyOf() hand them back unchanged.
        final ImmutableList<String> owned = ImmutableList.of("a", "b", "c");
        final ImmutableList<String> reversed = owned.reversed();
        Assertions.assertSame(reversed, ImmutableList.copyOf(reversed));

        // the subList half of the same claim, on the forward list and on the reversed view
        final ImmutableList<String> sub = owned.subList(1, 3);
        Assertions.assertSame(sub, ImmutableList.copyOf(sub));
        final ImmutableList<String> reversedSub = reversed.subList(0, 2);
        Assertions.assertSame(reversedSub, ImmutableList.copyOf(reversedSub));

        // two elements, so reversed() really builds a ReverseImmutableList instead of short-circuiting to
        // this same instance the way an owning list of size <= 1 does
        final ImmutableList<String> fromBuilder = ImmutableList.<String> builder().add("a").add("b").build();
        Assertions.assertSame(fromBuilder, ImmutableList.copyOf(fromBuilder));
        final ImmutableList<String> builderReversed = fromBuilder.reversed();
        Assertions.assertNotSame(fromBuilder, builderReversed);
        Assertions.assertSame(builderReversed, ImmutableList.copyOf(builderReversed));

        // a wrap()-backed list stays a live view, and so do its derived views
        final List<String> live = new ArrayList<>(Arrays.asList("a", "b"));
        final ImmutableList<String> viewReversed = ImmutableList.wrap(live).reversed();
        Assertions.assertNotSame(viewReversed, ImmutableList.copyOf(viewReversed));

        final ImmutableList<String> viewSub = ImmutableList.wrap(live).subList(0, 2);
        Assertions.assertNotSame(viewSub, ImmutableList.copyOf(viewSub));
    }
}
