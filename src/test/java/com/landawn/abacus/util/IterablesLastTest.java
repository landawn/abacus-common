package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;

public class IterablesLastTest extends IterablesTestSupport {
    @Test
    public void testLastNonNullTwoArgs() {
        assertEquals("b", Nulls.lastNonNull("a", "b"));
        assertEquals("a", Nulls.lastNonNull("a", null));
        assertEquals("b", Nulls.lastNonNull(null, "b"));
        assertNull(Nulls.lastNonNull(null, null));
    }

    @Test
    public void testLastNonNullThreeArgs() {
        assertEquals("c", Nulls.lastNonNull("a", "b", "c"));
        assertEquals("b", Nulls.lastNonNull("a", "b", null));
        assertEquals("a", Nulls.lastNonNull("a", null, null));
        assertNull(Nulls.lastNonNull(null, null, null));
    }

    @Test
    public void testLastNonNullVarArgs() {
        String[] arr = { "first", "second", null, null };
        assertEquals("second", Nulls.lastNonNull(arr));

        String[] allNull = { null, null, null };
        assertNull(Nulls.lastNonNull(allNull));

        assertNull(Nulls.lastNonNull((String[]) null));
        assertNull(Nulls.lastNonNull(new String[0]));
    }

    @Test
    public void testLastNonNullIterable() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", Nulls.lastNonNull(list));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Nulls.lastNonNull(allNull));

        assertNull(Nulls.lastNonNull((Iterable<String>) null));
        assertNull(Nulls.lastNonNull(new ArrayList<String>()));
    }

    @Test
    public void testLastNonNullIterator() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", Nulls.lastNonNull(list.iterator()));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Nulls.lastNonNull(allNull.iterator()));

        assertNull(Nulls.lastNonNull((Iterator<String>) null));
    }

    @Test
    public void testLastNonNullOrDefaultIterable() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", CommonUtil.lastNonNullOrDefault(list, "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.lastNonNullOrDefault(allNull, "default"));

        assertEquals("default", CommonUtil.lastNonNullOrDefault((Iterable<String>) null, "default"));
        assertEquals("default", CommonUtil.lastNonNullOrDefault(new ArrayList<String>(), "default"));
    }

    @Test
    public void testLastNonNullOrDefaultIterator() {
        List<String> list = Arrays.asList("first", "second", null, null);
        assertEquals("second", CommonUtil.lastNonNullOrDefault(list.iterator(), "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.lastNonNullOrDefault(allNull.iterator(), "default"));

        assertEquals("default", CommonUtil.lastNonNullOrDefault((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastNonNull_TwoParams_BothNull() {
        String result = Nulls.lastNonNull(null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_TwoParams_FirstNonNull() {
        String result = Nulls.lastNonNull("first", null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testLastNonNull_TwoParams_SecondNonNull() {
        String result = Nulls.lastNonNull(null, "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_TwoParams_BothNonNull() {
        String result = Nulls.lastNonNull("first", "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_AllNull() {
        String result = Nulls.lastNonNull(null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_ThreeParams_FirstNonNull() {
        String result = Nulls.lastNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_SecondNonNull() {
        String result = Nulls.lastNonNull(null, "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_ThirdNonNull() {
        String result = Nulls.lastNonNull(null, null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_AllNonNull() {
        String result = Nulls.lastNonNull("first", "second", "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_ThreeParams_FirstAndSecondNonNull() {
        String result = Nulls.lastNonNull("first", "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_Varargs_NullArray() {
        String result = Nulls.lastNonNull((String[]) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_EmptyArray() {
        String result = Nulls.lastNonNull(new String[0]);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_AllNull() {
        String result = Nulls.lastNonNull(null, null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Varargs_MiddleNonNull() {
        String result = Nulls.lastNonNull(null, null, "middle", null, null);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Varargs_LastNonNull() {
        String result = Nulls.lastNonNull(null, null, null, "last");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Varargs_MultipleNonNull() {
        String result = Nulls.lastNonNull(null, "first", "second", null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNull_Iterable_Null() {
        String result = Nulls.lastNonNull((Iterable<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_Empty() {
        List<String> list = Collections.emptyList();
        String result = Nulls.lastNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_AllNull() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Nulls.lastNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterable_LastNonNull() {
        List<String> list = Arrays.asList(null, null, "last");
        String result = Nulls.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, "middle", null, null);
        String result = Nulls.lastNonNull(list);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Iterable_MultipleNonNull() {
        List<String> list = Arrays.asList(null, "first", "second", null);
        String result = Nulls.lastNonNull(list);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testLastNonNull_Iterable_RandomAccessList() {
        ArrayList<String> list = new ArrayList<>();
        list.add(null);
        list.add("first");
        list.add(null);
        list.add("last");
        list.add(null);
        String result = Nulls.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterable_NonRandomAccessList() {
        List<String> list = new LinkedList<>();
        list.add(null);
        list.add("first");
        list.add(null);
        list.add("last");
        list.add(null);
        String result = Nulls.lastNonNull(list);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterator_Null() {
        String result = Nulls.lastNonNull((Iterator<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_Empty() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Nulls.lastNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_AllNull() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Nulls.lastNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testLastNonNull_Iterator_LastNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "last").iterator();
        String result = Nulls.lastNonNull(iter);
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNull_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "middle", null, null).iterator();
        String result = Nulls.lastNonNull(iter);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_Iterator_MultipleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "first", "second", null, "third").iterator();
        String result = Nulls.lastNonNull(iter);
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_NullIterableReturnsDefault() {
        String result = CommonUtil.lastNonNullOrDefault((Iterable<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_EmptyReturnsDefault() {
        List<String> list = Collections.emptyList();
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_AllNullReturnsDefault() {
        List<String> list = Arrays.asList(null, null, null);
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_LastNonNull() {
        List<String> list = Arrays.asList(null, null, "last");
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, "middle", null, null);
        String result = CommonUtil.lastNonNullOrDefault(list, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_NullIteratorReturnsDefault() {
        String result = CommonUtil.lastNonNullOrDefault((Iterator<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_EmptyReturnsDefault() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_AllNullReturnsDefault() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_LastNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "last").iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "middle", null, null).iterator();
        String result = CommonUtil.lastNonNullOrDefault(iter, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testLastNonNull_WithIntegers() {
        Integer result = Nulls.lastNonNull(10, null, 42);
        Assertions.assertEquals(42, result);
    }

    @Test
    public void testLastNonNull_WithCustomObjects() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object result = Nulls.lastNonNull(obj1, null, obj2);
        Assertions.assertSame(obj2, result);
    }

    @Test
    public void testLastNonNull_SingleElement() {
        assertEquals("a", Nulls.lastNonNull(new String[] { "a" }));
        assertNull(Nulls.lastNonNull(new String[] { null }));
    }

    @Test
    public void testLastNonNull_AllNull_TwoArgs() {
        assertNull(Nulls.lastNonNull(null, null));
    }

    @Test
    public void testLastNonNull_AllNull_ThreeArgs() {
        assertNull(Nulls.lastNonNull(null, null, null));
    }

    @Test
    public void testLastNonNull_Iterable_NonList() {
        // Use a LinkedList (Iterable but not RandomAccess-List)
        java.util.LinkedList<String> linked = new java.util.LinkedList<>(Arrays.asList("a", null, "b", null));
        assertEquals("b", Nulls.lastNonNull(linked));
    }

    @Test
    public void testLastNonNull_Iterable_AllNull_NonList() {
        java.util.LinkedList<String> linked = new java.util.LinkedList<>(Arrays.asList((String) null, null));
        assertNull(Nulls.lastNonNull(linked));
    }

    @Test
    public void testLastNonNullOrDefault_Iterable_NullDefaultThrowsException() {
        List<String> list = Arrays.asList("test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.lastNonNullOrDefault(list, null);
        });
    }

    @Test
    public void testLastNonNullOrDefault_Iterator_NullDefaultThrowsException() {
        Iterator<String> iter = Arrays.asList("test").iterator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.lastNonNullOrDefault(iter, null);
        });
    }

    @Test
    public void testLastIndexOf() {
        Object[] array = { "a", "b", "c", "b" };
        OptionalInt result = Iterables.lastIndexOf(array, "b");
        assertTrue(result.isPresent());
        assertEquals(3, result.get());

        OptionalInt notFound = Iterables.lastIndexOf(array, "d");
        assertFalse(notFound.isPresent());
    }

    @Test
    public void testLastIndexOfArray_Dedicated() {
        OptionalInt result = Iterables.lastIndexOf(new Object[] { "a", "b", "a", "c" }, "a");
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testLastIndexOfArray_NotFound() {
        OptionalInt result = Iterables.lastIndexOf(new Object[] { "a", "b", "c" }, "z");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOfCollection_Dedicated() {
        OptionalInt result = Iterables.lastIndexOf(Arrays.asList("a", "b", "a", "c"), "a");
        assertTrue(result.isPresent());
        assertEquals(2, result.get());
    }

    @Test
    public void testLastIndexOfCollection_NotFound() {
        OptionalInt result = Iterables.lastIndexOf(Arrays.asList("a", "b", "c"), "z");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOfArray() {
        Integer[] arr = { 1, 2, 3, 2, 4 };
        OptionalInt result = Iterables.lastIndexOf(arr, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        assertFalse(Iterables.lastIndexOf(arr, 5).isPresent());
        assertFalse(Iterables.lastIndexOf((Object[]) null, 1).isPresent());
    }

    @Test
    public void testLastIndexOfCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3, 2, 4);
        OptionalInt result = Iterables.lastIndexOf(list, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        assertFalse(Iterables.lastIndexOf(list, 5).isPresent());
        assertFalse(Iterables.lastIndexOf((Collection<?>) null, 1).isPresent());
    }

    @Test
    public void testLastIndexOf_NullArray() {
        OptionalInt result = Iterables.lastIndexOf((Object[]) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOf_NullCollection() {
        OptionalInt result = Iterables.lastIndexOf((Collection<?>) null, "x");
        assertFalse(result.isPresent());
    }

    @Test
    public void testLastIndexOf_FindNull() {
        OptionalInt result = Iterables.lastIndexOf(new Object[] { "a", null, "b", null }, null);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }
}
