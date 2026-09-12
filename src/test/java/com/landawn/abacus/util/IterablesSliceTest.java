package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

public class IterablesSliceTest extends IterablesTestSupport {
    // ============================================================

    @Test
    public void testSlice_FromList() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 1, 4);
        assertEquals(3, slice.size());
        assertFalse(slice.isEmpty());
        assertTrue(slice.contains("b"));
        assertTrue(slice.contains("c"));
        assertTrue(slice.contains("d"));
        assertFalse(slice.contains("a"));
        assertFalse(slice.contains("e"));
    }

    @Test
    public void testSlice_FromArray() {
        String[] array = { "x", "y", "z", "w" };
        Iterables.Slice<String> slice = new Iterables.Slice<>(array, 0, 3);
        assertEquals(3, slice.size());
        assertTrue(slice.contains("x"));
        assertTrue(slice.contains("y"));
        assertTrue(slice.contains("z"));
        assertFalse(slice.contains("w"));
    }

    @Test
    public void testSlice_FromCollection_NonList() {
        // Use HashSet (non-List) with known contents
        TreeSet<String> set = new TreeSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        Iterables.Slice<String> slice = new Iterables.Slice<>(set, 1, 4);
        assertEquals(3, slice.size());
        assertFalse(slice.isEmpty());
    }

    @Test
    public void testSlice_IsEmpty_EmptySlice() {
        List<String> list = Arrays.asList("a", "b", "c");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 1, 1);
        assertTrue(slice.isEmpty());
        assertEquals(0, slice.size());
    }

    @Test
    public void testSlice_ContainsAll() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 1, 3);
        assertTrue(slice.containsAll(Arrays.asList("b", "c")));
        assertFalse(slice.containsAll(Arrays.asList("b", "c", "d")));
        assertTrue(slice.containsAll(Arrays.asList()));
    }

    @Test
    public void testSlice_Iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 1, 3);
        Iterator<String> iter = slice.iterator();
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSlice_Iterator_Empty() {
        List<String> list = Arrays.asList("a", "b", "c");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 2, 2);
        Iterator<String> iter = slice.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSlice_ToArray() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Iterables.Slice<Integer> slice = new Iterables.Slice<>(list, 1, 4);
        Object[] arr = slice.toArray();
        assertEquals(3, arr.length);
        assertEquals(2, arr[0]);
        assertEquals(3, arr[1]);
        assertEquals(4, arr[2]);
    }

    @Test
    public void testSlice_ToArray_Typed() {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 0, 3);
        String[] arr = slice.toArray(new String[0]);
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("b", arr[1]);
        assertEquals("c", arr[2]);
    }

    @Test
    public void testSlice_ToArray_Typed_LargerArray() {
        List<String> list = Arrays.asList("a", "b", "c");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 0, 2);
        String[] arr = slice.toArray(new String[5]);
        // arr[2] should be null sentinel
        assertNull(arr[2]);
    }

    @Test
    public void testSlice_FullList() {
        List<Integer> list = Arrays.asList(10, 20, 30);
        Iterables.Slice<Integer> slice = new Iterables.Slice<>(list, 0, 3);
        assertEquals(3, slice.size());
        Object[] arr = slice.toArray();
        assertEquals(10, arr[0]);
        assertEquals(20, arr[1]);
        assertEquals(30, arr[2]);
    }

    @Test
    public void testSlice_FromTail() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Iterables.Slice<String> slice = new Iterables.Slice<>(list, 3, 5);
        assertEquals(2, slice.size());
        assertTrue(slice.contains("d"));
        assertTrue(slice.contains("e"));
        assertFalse(slice.contains("c"));
    }

    @Test
    public void testSlice_toString_nonListCollection_rendersOnlyTheSlice() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));

        assertEquals("[b, c, d]", CommonUtil.slice(set, 1, 4).toString());
        assertEquals("[a, b, c]", CommonUtil.slice(set, 0, 3).toString());
        assertEquals("[e]", CommonUtil.slice(set, 4, 5).toString());
        assertEquals("[a, b, c, d, e]", CommonUtil.slice(set, 0, 5).toString());
    }

    @Test
    public void testSlice_toString_emptySlice() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));
        final ImmutableCollection<String> slice = CommonUtil.slice(set, 2, 2);

        assertEquals(0, slice.size());
        assertTrue(slice.isEmpty());
        assertEquals("[]", slice.toString());
    }

    @Test
    public void testSlice_toString_agreesWithIterationOrder() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d", "e"));

        for (int from = 0; from <= set.size(); from++) {
            for (int to = from; to <= set.size(); to++) {
                final ImmutableCollection<String> slice = CommonUtil.slice(set, from, to);
                assertEquals(new ArrayList<>(slice).toString(), slice.toString(), "slice [" + from + ", " + to + ")");
                assertEquals(to - from, slice.size());
            }
        }
    }

    @Test
    public void testSlice_toString_withNullElement() {
        final Set<String> set = new LinkedHashSet<>(Arrays.asList("a", null, "c"));

        assertEquals("[null, c]", CommonUtil.slice(set, 1, 3).toString());
        assertEquals("[a, null]", CommonUtil.slice(set, 0, 2).toString());
    }

    @Test
    public void testSlice_toString_listPathStillCorrect() {
        assertEquals("[b, c, d]", CommonUtil.slice(Arrays.asList("a", "b", "c", "d", "e"), 1, 4).toString());
    }

    @Test
    public void testSlice_toString_directConstruction() {
        assertEquals("[b, c, d]", new Iterables.Slice<>(Arrays.asList("a", "b", "c", "d", "e"), 1, 4).toString());
        assertEquals("[x, y, z]", new Iterables.Slice<>(new String[] { "x", "y", "z", "w" }, 0, 3).toString());
        assertEquals("[]", new Iterables.Slice<>(Arrays.asList("a", "b", "c"), 1, 1).toString());
    }

    @Test
    public void testSlice_sizeStaysConsistentWithIteratorWhenBackingCollectionShrinks() {
        final Set<Integer> backing = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        final Collection<Integer> slice = CommonUtil.slice(backing, 1, 4); // non-List source: the range is applied lazily

        assertEquals(3, slice.size());
        assertEquals("[2, 3, 4]", slice.toString());
        assertArrayEquals(new Object[] { 2, 3, 4 }, slice.toArray());

        backing.remove(5);
        backing.remove(4);
        backing.remove(3); // backing is now [1, 2]; only element 2 is still inside [1, 4)

        // size() used to keep reporting 3, so toArray() called next() three times and blew up with
        // NoSuchElementException. It must now agree with what the iterator actually yields.
        assertEquals(1, slice.size());
        assertFalse(slice.isEmpty());
        assertEquals("[2]", slice.toString());
        assertArrayEquals(new Object[] { 2 }, slice.toArray());
        assertArrayEquals(new Integer[] { 2 }, slice.toArray(new Integer[0]));

        backing.remove(2);
        backing.remove(1); // backing is now empty

        assertEquals(0, slice.size());
        assertTrue(slice.isEmpty());
        assertEquals("[]", slice.toString());
        assertArrayEquals(new Object[0], slice.toArray());
        assertArrayEquals(new Integer[0], slice.toArray(new Integer[0]));
    }

    @Test
    public void testSlice_toArrayWithLargerArrayNullTerminatesAtTheRealCount() {
        final Set<Integer> backing = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        final Collection<Integer> slice = CommonUtil.slice(backing, 1, 4);

        assertArrayEquals(new Integer[] { 2, 3, 4, null, 9 }, slice.toArray(new Integer[] { 9, 9, 9, 9, 9 }));

        backing.remove(4);
        backing.remove(3); // backing is now [1, 2, 5]; positions 1 and 2 fall inside [1, 4)

        assertEquals(2, slice.size());
        assertArrayEquals(new Integer[] { 2, 5, null, 9, 9 }, slice.toArray(new Integer[] { 9, 9, 9, 9, 9 }));

        backing.remove(5); // backing is now [1, 2]: only position 1 is left inside the range

        assertEquals(1, slice.size());
        assertArrayEquals(new Integer[] { 2, null, 9, 9, 9 }, slice.toArray(new Integer[] { 9, 9, 9, 9, 9 }));
    }

    @Test
    public void testSlice_growingBackingCollectionStillHonoursTheOriginalRange() {
        final Set<Integer> backing = new LinkedHashSet<>(Arrays.asList(1, 2, 3, 4, 5));
        final Collection<Integer> slice = CommonUtil.slice(backing, 1, 4);

        backing.add(6);
        backing.add(7);

        assertEquals(3, slice.size());
        assertArrayEquals(new Object[] { 2, 3, 4 }, slice.toArray());
    }
}
