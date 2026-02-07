package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.function.UnaryOperator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableList101Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableList<String> emptyList = ImmutableList.empty();
        Assertions.assertTrue(emptyList.isEmpty());
        Assertions.assertEquals(0, emptyList.size());
    }

    //    @Test
    //    public void testJust() {
    //        ImmutableList<String> list = ImmutableList.just("single");
    //        Assertions.assertEquals(1, list.size());
    //        Assertions.assertEquals("single", list.get(0));
    //    }

    @Test
    public void testOf_SingleElement() {
        ImmutableList<Integer> list = ImmutableList.of(42);
        Assertions.assertEquals(1, list.size());
        Assertions.assertEquals(42, list.get(0));
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
        Assertions.assertEquals("d", list.get(3));
    }

    @Test
    public void testOf_FiveElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5);
        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals(5, list.get(4));
    }

    @Test
    public void testOf_SixElements() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f");
        Assertions.assertEquals(6, list.size());
        Assertions.assertEquals("f", list.get(5));
    }

    @Test
    public void testOf_SevenElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5, 6, 7);
        Assertions.assertEquals(7, list.size());
        Assertions.assertEquals(7, list.get(6));
    }

    @Test
    public void testOf_EightElements() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h");
        Assertions.assertEquals(8, list.size());
        Assertions.assertEquals("h", list.get(7));
    }

    @Test
    public void testOf_NineElements() {
        ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Assertions.assertEquals(9, list.size());
        Assertions.assertEquals(9, list.get(8));
    }

    @Test
    public void testOf_TenElements() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        Assertions.assertEquals(10, list.size());
        Assertions.assertEquals("j", list.get(9));
    }

    //    @Test
    //    public void testOf_VarArgs() {
    //        String[] array = { "one", "two", "three" };
    //        ImmutableList<String> list = ImmutableList.of(array);
    //        Assertions.assertEquals(3, list.size());
    //        Assertions.assertEquals("one", list.get(0));
    //        Assertions.assertEquals("three", list.get(2));
    //    }
    //
    //    @Test
    //    public void testOf_VarArgs_Empty() {
    //        ImmutableList<String> list = ImmutableList.of();
    //        Assertions.assertTrue(list.isEmpty());
    //    }
    //
    //    @Test
    //    public void testOf_VarArgs_Null() {
    //        ImmutableList<String> list = ImmutableList.of((String[]) null);
    //        Assertions.assertTrue(list.isEmpty());
    //    }

    @Test
    public void testCopyOf() {
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b", "c"));
        ImmutableList<String> immutable = ImmutableList.copyOf(mutable);

        Assertions.assertEquals(3, immutable.size());
        Assertions.assertEquals("a", immutable.get(0));

        mutable.add("d");
        Assertions.assertEquals(3, immutable.size());
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
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableList<String> list = ImmutableList.copyOf((Collection<String>) null);
        Assertions.assertTrue(list.isEmpty());
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
    public void testWrap_AlreadyImmutable() {
        ImmutableList<String> original = ImmutableList.of("a");
        ImmutableList<String> wrapped = ImmutableList.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableList<String> wrapped = ImmutableList.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_Collection_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableList.wrap(new HashSet<>(Arrays.asList("a", "b")));
        });
    }

    @Test
    public void testGet() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        Assertions.assertEquals("a", list.get(0));
        Assertions.assertEquals("b", list.get(1));
        Assertions.assertEquals("c", list.get(2));

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
        Assertions.assertEquals(-1, list.indexOf(null));
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

        Assertions.assertEquals("c", iter.next());
        Assertions.assertEquals("c", iter.previous());
        Assertions.assertEquals("b", iter.previous());
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
    public void testReverse() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");
        ImmutableList<String> reversed = list.reversed();

        Assertions.assertEquals(3, reversed.size());
        Assertions.assertEquals("c", reversed.get(0));
        Assertions.assertEquals("b", reversed.get(1));
        Assertions.assertEquals("a", reversed.get(2));

        ImmutableList<String> doubleReversed = reversed.reversed();
        Assertions.assertSame(list, doubleReversed);
    }

    @Test
    public void testReverse_SingleElement() {
        ImmutableList<String> list = ImmutableList.of("single");
        ImmutableList<String> reversed = list.reversed();
        Assertions.assertSame(list, reversed);
    }

    @Test
    public void testReverse_Empty() {
        ImmutableList<String> list = ImmutableList.empty();
        ImmutableList<String> reversed = list.reversed();
        Assertions.assertSame(list, reversed);
    }

    @Test
    public void testReverse_Operations() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c", "d", "e");
        ImmutableList<String> reversed = list.reversed();

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
    public void testBuilder() {
        ImmutableList<String> list = ImmutableList.<String> builder().add("one").add("two", "three").addAll(Arrays.asList("four", "five")).build();

        Assertions.assertEquals(5, list.size());
        Assertions.assertEquals("one", list.get(0));
        Assertions.assertEquals("five", list.get(4));
    }

    @Test
    public void testBuilder_WithIterator() {
        List<String> source = Arrays.asList("a", "b", "c");
        ImmutableList<String> list = ImmutableList.<String> builder().addAll(source.iterator()).build();

        Assertions.assertEquals(3, list.size());
    }

    @Test
    public void testBuilder_WithBackingList() {
        List<String> backing = new ArrayList<>();
        ImmutableList<String> list = ImmutableList.builder(backing).add("a").add("b").build();

        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(2, backing.size());
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
    public void testContains() {
        ImmutableList<String> list = ImmutableList.of("a", "b", "c");

        Assertions.assertTrue(list.contains("a"));
        Assertions.assertTrue(list.contains("b"));
        Assertions.assertTrue(list.contains("c"));
        Assertions.assertFalse(list.contains("d"));
        Assertions.assertFalse(list.contains(null));
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
}
