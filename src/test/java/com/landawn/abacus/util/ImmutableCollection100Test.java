package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ImmutableCollection100Test extends TestBase {

    @Test
    public void testWrap() {
        Collection<String> collection = Arrays.asList("a", "b", "c");
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(collection);

        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.contains("a"));
        Assertions.assertTrue(wrapped.contains("b"));
        Assertions.assertTrue(wrapped.contains("c"));
    }

    @Test
    public void testWrap_Null() {
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
        Assertions.assertTrue(wrapped instanceof ImmutableList);
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(list);
        Assertions.assertSame(list, wrapped);
    }

    @Test
    public void testWrap_ReflectsChanges() {
        List<String> mutable = new ArrayList<>(Arrays.asList("a", "b"));
        ImmutableCollection<String> wrapped = ImmutableCollection.wrap(mutable);

        Assertions.assertEquals(2, wrapped.size());

        mutable.add("c");
        Assertions.assertEquals(3, wrapped.size());
        Assertions.assertTrue(wrapped.contains("c"));
    }

    @Test
    public void testAdd_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.add("c"));
    }

    @Test
    public void testAddAll_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.addAll(Arrays.asList("c", "d")));
    }

    @Test
    public void testRemove_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.remove("a"));
    }

    @Test
    public void testRemoveIf_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.removeIf(s -> s.equals("a")));
    }

    @Test
    public void testRemoveAll_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.removeAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testRetainAll_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.retainAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testClear_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> collection.clear());
    }

    @Test
    public void testContains() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));

        Assertions.assertTrue(collection.contains("a"));
        Assertions.assertTrue(collection.contains("b"));
        Assertions.assertTrue(collection.contains("c"));
        Assertions.assertFalse(collection.contains("d"));
        Assertions.assertFalse(collection.contains(null));
    }

    @Test
    public void testContains_WithNull() {
        List<String> listWithNull = Arrays.asList("a", null, "c");
        ImmutableCollection<String> collection = ImmutableCollection.wrap(listWithNull);

        Assertions.assertTrue(collection.contains(null));
        Assertions.assertTrue(collection.contains("a"));
        Assertions.assertFalse(collection.contains("b"));
    }

    @Test
    public void testIterator() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("x", "y", "z"));
        ObjIterator<String> iter = collection.iterator();

        List<String> collected = new ArrayList<>();
        while (iter.hasNext()) {
            collected.add(iter.next());
        }

        Assertions.assertEquals(Arrays.asList("x", "y", "z"), collected);
    }

    @Test
    public void testIterator_Remove_ThrowsUnsupported() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        ObjIterator<String> iter = collection.iterator();

        iter.next();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> iter.remove());
    }

    @Test
    public void testSize() {
        Assertions.assertEquals(0, ImmutableCollection.wrap(Collections.emptyList()).size());
        Assertions.assertEquals(1, ImmutableCollection.wrap(Arrays.asList("a")).size());
        Assertions.assertEquals(3, ImmutableCollection.wrap(Arrays.asList("a", "b", "c")).size());
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(ImmutableCollection.wrap(Collections.emptyList()).isEmpty());
        Assertions.assertFalse(ImmutableCollection.wrap(Arrays.asList("a")).isEmpty());
    }

    @Test
    public void testToArray() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        Object[] array = collection.toArray();

        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    @Test
    public void testToArray_WithType() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b", "c"));
        String[] array = collection.toArray(new String[0]);

        Assertions.assertEquals(3, array.length);
        Assertions.assertEquals("a", array[0]);
        Assertions.assertEquals("b", array[1]);
        Assertions.assertEquals("c", array[2]);
    }

    @Test
    public void testToArray_PreSizedArray() {
        ImmutableCollection<String> collection = ImmutableCollection.wrap(Arrays.asList("a", "b"));
        String[] array = new String[5];
        String[] result = collection.toArray(array);

        Assertions.assertSame(array, result);
        Assertions.assertEquals("a", result[0]);
        Assertions.assertEquals("b", result[1]);
        Assertions.assertNull(result[2]);
    }

    @Test
    public void testEquals() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<String> list3 = Arrays.asList("a", "b", "d");

        ImmutableCollection<String> immutable1 = ImmutableCollection.wrap(list1);
        ImmutableCollection<String> immutable2 = ImmutableCollection.wrap(list2);
        ImmutableCollection<String> immutable3 = ImmutableCollection.wrap(list3);

        Assertions.assertEquals(immutable1, immutable2);
        Assertions.assertNotEquals(immutable1, list2);

        Assertions.assertNotEquals(immutable1, immutable3);
        Assertions.assertNotEquals(immutable1, list3);

        Assertions.assertNotEquals(immutable1, "not a collection");
        Assertions.assertNotEquals(immutable1, null);
    }

    @Test
    public void testHashCode() {
        List<String> list = Arrays.asList("a", "b", "c");
        ImmutableCollection<String> immutable = ImmutableCollection.wrap(list);

        Assertions.assertEquals(list.hashCode(), immutable.hashCode());
    }

    @Test
    public void testToString() {
        List<String> list = Arrays.asList("a", "b", "c");
        ImmutableCollection<String> immutable = ImmutableCollection.wrap(list);

        Assertions.assertEquals(list.toString(), immutable.toString());
    }

    @Test
    public void testWithDifferentCollectionTypes() {
        Set<Integer> set = new HashSet<>(Arrays.asList(1, 2, 3));
        ImmutableCollection<Integer> immutableSet = ImmutableCollection.wrap(set);
        Assertions.assertEquals(3, immutableSet.size());

        Queue<String> queue = new LinkedList<>(Arrays.asList("first", "second"));
        ImmutableCollection<String> immutableQueue = ImmutableCollection.wrap(queue);
        Assertions.assertEquals(2, immutableQueue.size());
    }
}
