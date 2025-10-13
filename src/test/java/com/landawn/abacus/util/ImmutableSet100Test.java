package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableSet100Test extends TestBase {

    @Test
    public void testEmpty() {
        ImmutableSet<String> emptySet = ImmutableSet.empty();
        Assertions.assertTrue(emptySet.isEmpty());
        Assertions.assertEquals(0, emptySet.size());
        Assertions.assertFalse(emptySet.contains("anything"));
    }

    @Test
    public void testJust() {
        ImmutableSet<String> set = ImmutableSet.just("single");
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains("single"));
        Assertions.assertFalse(set.contains("other"));
    }

    @Test
    public void testOf_SingleElement() {
        ImmutableSet<Integer> set = ImmutableSet.of(42);
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains(42));
    }

    @Test
    public void testOf_TwoElements() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b");
        Assertions.assertEquals(2, set.size());
        Assertions.assertTrue(set.contains("a"));
        Assertions.assertTrue(set.contains("b"));
    }

    @Test
    public void testOf_TwoElements_Duplicates() {
        ImmutableSet<String> set = ImmutableSet.of("same", "same");
        Assertions.assertEquals(1, set.size());
        Assertions.assertTrue(set.contains("same"));
    }

    @Test
    public void testOf_ThreeElements() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3);
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains(1));
        Assertions.assertTrue(set.contains(2));
        Assertions.assertTrue(set.contains(3));
    }

    @Test
    public void testOf_FourElements() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c", "d");
        Assertions.assertEquals(4, set.size());
        Assertions.assertTrue(set.contains("d"));
    }

    @Test
    public void testOf_FiveElements() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5);
        Assertions.assertEquals(5, set.size());
        Assertions.assertTrue(set.contains(5));
    }

    @Test
    public void testOf_SixElements() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c", "d", "e", "f");
        Assertions.assertEquals(6, set.size());
        Assertions.assertTrue(set.contains("f"));
    }

    @Test
    public void testOf_SevenElements() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7);
        Assertions.assertEquals(7, set.size());
        Assertions.assertTrue(set.contains(7));
    }

    @Test
    public void testOf_EightElements() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c", "d", "e", "f", "g", "h");
        Assertions.assertEquals(8, set.size());
        Assertions.assertTrue(set.contains("h"));
    }

    @Test
    public void testOf_NineElements() {
        ImmutableSet<Integer> set = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        Assertions.assertEquals(9, set.size());
        Assertions.assertTrue(set.contains(9));
    }

    @Test
    public void testOf_TenElements() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
        Assertions.assertEquals(10, set.size());
        Assertions.assertTrue(set.contains("j"));
    }

    @Test
    public void testOf_VarArgs_Empty() {
        ImmutableSet<String> set = ImmutableSet.of();
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testOf_VarArgs_Null() {
        ImmutableSet<String> set = ImmutableSet.of((String[]) null);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testOf_VarArgs_WithDuplicates() {
        String[] array = { "red", "green", "blue", "red" };
        ImmutableSet<String> set = ImmutableSet.of(array);
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains("red"));
        Assertions.assertTrue(set.contains("green"));
        Assertions.assertTrue(set.contains("blue"));
    }

    @Test
    public void testCopyOf_Collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        ImmutableSet<String> set = ImmutableSet.copyOf(list);
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains("a"));
        Assertions.assertTrue(set.contains("b"));
        Assertions.assertTrue(set.contains("c"));
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableSet<String> original = ImmutableSet.of("a", "b");
        ImmutableSet<String> copy = ImmutableSet.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_EmptyCollection() {
        ImmutableSet<String> set = ImmutableSet.copyOf(new ArrayList<>());
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testCopyOf_NullCollection() {
        ImmutableSet<String> set = ImmutableSet.copyOf(null);
        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testCopyOf_PreservesOrder() {
        LinkedHashSet<String> linkedSet = new LinkedHashSet<>();
        linkedSet.add("first");
        linkedSet.add("second");
        linkedSet.add("third");

        ImmutableSet<String> set = ImmutableSet.copyOf(linkedSet);
        Iterator<String> iter = set.iterator();
        Assertions.assertEquals("first", iter.next());
        Assertions.assertEquals("second", iter.next());
        Assertions.assertEquals("third", iter.next());
    }

    @Test
    public void testWrap() {
        Set<String> mutableSet = new HashSet<>();
        mutableSet.add("initial");

        ImmutableSet<String> wrapped = ImmutableSet.wrap(mutableSet);
        Assertions.assertEquals(1, wrapped.size());

        mutableSet.add("added");
        Assertions.assertEquals(2, wrapped.size());
        Assertions.assertTrue(wrapped.contains("added"));
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableSet<String> original = ImmutableSet.of("a");
        ImmutableSet<String> wrapped = ImmutableSet.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableSet<String> wrapped = ImmutableSet.wrap((Set<String>) null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testWrap_Collection_Deprecated() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            ImmutableSet.wrap(Arrays.asList("a", "b"));
        });
    }

    @Test
    public void testBuilder() {
        ImmutableSet<String> set = ImmutableSet.<String> builder().add("one").add("two", "three").addAll(Arrays.asList("four", "five")).build();

        Assertions.assertEquals(5, set.size());
        Assertions.assertTrue(set.contains("one"));
        Assertions.assertTrue(set.contains("five"));
    }

    @Test
    public void testBuilder_WithDuplicates() {
        ImmutableSet<Integer> set = ImmutableSet.<Integer> builder().add(1).add(1).add(2, 2, 3).build();

        Assertions.assertEquals(3, set.size());
    }

    @Test
    public void testBuilder_WithIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        ImmutableSet<String> set = ImmutableSet.<String> builder().addAll(list.iterator()).build();

        Assertions.assertEquals(3, set.size());
    }

    @Test
    public void testBuilder_WithNullIterator() {
        ImmutableSet<String> set = ImmutableSet.<String> builder().addAll((Iterator<String>) null).build();

        Assertions.assertTrue(set.isEmpty());
    }

    @Test
    public void testBuilder_WithBackingSet() {
        Set<String> backingSet = new LinkedHashSet<>();
        ImmutableSet<String> set = ImmutableSet.builder(backingSet).add("a").add("b").build();

        Assertions.assertEquals(2, set.size());
        Assertions.assertEquals(2, backingSet.size());
    }

    @Test
    public void testIterator() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c");
        ObjIterator<String> iter = set.iterator();

        Set<String> collected = new HashSet<>();
        while (iter.hasNext()) {
            collected.add(iter.next());
        }

        Assertions.assertEquals(3, collected.size());
        Assertions.assertTrue(collected.contains("a"));
        Assertions.assertTrue(collected.contains("b"));
        Assertions.assertTrue(collected.contains("c"));
    }

    @Test
    public void testContains() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b", "c");

        Assertions.assertTrue(set.contains("a"));
        Assertions.assertFalse(set.contains("d"));
        Assertions.assertFalse(set.contains(null));
    }

    @Test
    public void testContainsWithNull() {
        Set<String> setWithNull = new HashSet<>();
        setWithNull.add("a");
        setWithNull.add(null);

        ImmutableSet<String> set = ImmutableSet.copyOf(setWithNull);
        Assertions.assertTrue(set.contains(null));
        Assertions.assertTrue(set.contains("a"));
    }

    @Test
    public void testMutationMethods_ThrowUnsupported() {
        ImmutableSet<String> set = ImmutableSet.of("a", "b");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.add("c"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.addAll(Arrays.asList("d", "e")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.removeAll(Arrays.asList("a")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.retainAll(Arrays.asList("a")));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> set.removeIf(s -> s.equals("a")));
    }
}
