package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IterablesFirstTest extends IterablesTestSupport {
    @Test
    public void testFirstNonNullTwoArgs() {
        assertEquals("a", Nulls.firstNonNull("a", "b"));
        assertEquals("b", Nulls.firstNonNull(null, "b"));
        assertEquals("a", Nulls.firstNonNull("a", null));
        assertNull(Nulls.firstNonNull(null, null));
    }

    @Test
    public void testFirstNonNullThreeArgs() {
        assertEquals("a", Nulls.firstNonNull("a", "b", "c"));
        assertEquals("b", Nulls.firstNonNull(null, "b", "c"));
        assertEquals("c", Nulls.firstNonNull(null, null, "c"));
        assertNull(Nulls.firstNonNull(null, null, null));
    }

    @Test
    public void testFirstNonNullVarArgs() {
        String[] arr = { null, null, "found", "second" };
        assertEquals("found", Nulls.firstNonNull(arr));

        String[] allNull = { null, null, null };
        assertNull(Nulls.firstNonNull(allNull));

        assertNull(Nulls.firstNonNull((String[]) null));
        assertNull(Nulls.firstNonNull(new String[0]));
    }

    @Test
    public void testFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", Nulls.firstNonNull(list));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Nulls.firstNonNull(allNull));

        assertNull(Nulls.firstNonNull((Iterable<String>) null));
        assertNull(Nulls.firstNonNull(new ArrayList<String>()));
    }

    @Test
    public void testFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", Nulls.firstNonNull(list.iterator()));

        List<String> allNull = Arrays.asList(null, null, null);
        assertNull(Nulls.firstNonNull(allNull.iterator()));

        assertNull(Nulls.firstNonNull((Iterator<String>) null));
    }

    @Test
    public void testFirstNonNullOrDefaultIterable() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", CommonUtil.firstNonNullOrDefault(list, "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.firstNonNullOrDefault(allNull, "default"));

        assertEquals("default", CommonUtil.firstNonNullOrDefault((Iterable<String>) null, "default"));
        assertEquals("default", CommonUtil.firstNonNullOrDefault(new ArrayList<String>(), "default"));
    }

    @Test
    public void testFirstNonNullOrDefaultIterator() {
        List<String> list = Arrays.asList(null, null, "found", "second");
        assertEquals("found", CommonUtil.firstNonNullOrDefault(list.iterator(), "default"));

        List<String> allNull = Arrays.asList(null, null, null);
        assertEquals("default", CommonUtil.firstNonNullOrDefault(allNull.iterator(), "default"));

        assertEquals("default", CommonUtil.firstNonNullOrDefault((Iterator<String>) null, "default"));
    }

    @Test
    public void testFirstNonNull_TwoParams_BothNull() {
        String result = Nulls.firstNonNull(null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_TwoParams_FirstNonNull() {
        String result = Nulls.firstNonNull("first", null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_TwoParams_SecondNonNull() {
        String result = Nulls.firstNonNull(null, "second");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_TwoParams_BothNonNull() {
        String result = Nulls.firstNonNull("first", "second");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_AllNull() {
        String result = Nulls.firstNonNull(null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_FirstNonNull() {
        String result = Nulls.firstNonNull("first", null, null);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_SecondNonNull() {
        String result = Nulls.firstNonNull(null, "second", null);
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_ThirdNonNull() {
        String result = Nulls.firstNonNull(null, null, "third");
        Assertions.assertEquals("third", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_AllNonNull() {
        String result = Nulls.firstNonNull("first", "second", "third");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_ThreeParams_SecondAndThirdNonNull() {
        String result = Nulls.firstNonNull(null, "second", "third");
        Assertions.assertEquals("second", result);
    }

    @Test
    public void testFirstNonNull_Varargs_NullArray() {
        String result = Nulls.firstNonNull((String[]) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_EmptyArray() {
        String result = Nulls.firstNonNull(new String[0]);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_AllNull() {
        String result = Nulls.firstNonNull(null, null, null, null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Varargs_MiddleNonNull() {
        String result = Nulls.firstNonNull(null, null, "middle", null, null);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Varargs_LastNonNull() {
        String result = Nulls.firstNonNull(null, null, null, "last");
        Assertions.assertEquals("last", result);
    }

    @Test
    public void testFirstNonNull_Varargs_MultipleNonNull() {
        String result = Nulls.firstNonNull(null, "first", "second", null, "third");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterable_Null() {
        String result = Nulls.firstNonNull((Iterable<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_Empty() {
        List<String> list = Collections.emptyList();
        String result = Nulls.firstNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_AllNull() {
        List<String> list = Arrays.asList(null, null, null);
        String result = Nulls.firstNonNull(list);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterable_FirstNonNull() {
        List<String> list = Arrays.asList("first", null, null);
        String result = Nulls.firstNonNull(list);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, null, "middle", null);
        String result = Nulls.firstNonNull(list);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Iterable_MultipleNonNull() {
        List<String> list = Arrays.asList(null, "first", "second", null);
        String result = Nulls.firstNonNull(list);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterator_Null() {
        String result = Nulls.firstNonNull((Iterator<String>) null);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_Empty() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = Nulls.firstNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_AllNull() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = Nulls.firstNonNull(iter);
        Assertions.assertNull(result);
    }

    @Test
    public void testFirstNonNull_Iterator_FirstNonNull() {
        Iterator<String> iter = Arrays.asList("first", null, null).iterator();
        String result = Nulls.firstNonNull(iter);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNull_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "middle", null).iterator();
        String result = Nulls.firstNonNull(iter);
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_Iterator_MultipleNonNull() {
        Iterator<String> iter = Arrays.asList(null, "first", "second", null).iterator();
        String result = Nulls.firstNonNull(iter);
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_NullIterableReturnsDefault() {
        String result = CommonUtil.firstNonNullOrDefault((Iterable<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_EmptyReturnsDefault() {
        List<String> list = Collections.emptyList();
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_AllNullReturnsDefault() {
        List<String> list = Arrays.asList(null, null, null);
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_FirstNonNull() {
        List<String> list = Arrays.asList("first", null, null);
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_MiddleNonNull() {
        List<String> list = Arrays.asList(null, null, "middle", null);
        String result = CommonUtil.firstNonNullOrDefault(list, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_NullIteratorReturnsDefault() {
        String result = CommonUtil.firstNonNullOrDefault((Iterator<String>) null, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_EmptyReturnsDefault() {
        Iterator<String> iter = Collections.<String> emptyList().iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_AllNullReturnsDefault() {
        Iterator<String> iter = Arrays.asList((String) null, (String) null, (String) null).iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("default", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_FirstNonNull() {
        Iterator<String> iter = Arrays.asList("first", null, null).iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("first", result);
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_MiddleNonNull() {
        Iterator<String> iter = Arrays.asList(null, null, "middle", null).iterator();
        String result = CommonUtil.firstNonNullOrDefault(iter, "default");
        Assertions.assertEquals("middle", result);
    }

    @Test
    public void testFirstNonNull_WithIntegers() {
        Integer result = Nulls.firstNonNull(null, 42, 100);
        Assertions.assertEquals(42, result);
    }

    @Test
    public void testFirstNonNull_WithCustomObjects() {
        Object obj1 = new Object();
        Object obj2 = new Object();
        Object result = Nulls.firstNonNull(null, obj1, obj2);
        Assertions.assertSame(obj1, result);
    }

    // ===================== firstNonNull / lastNonNull edge cases =====================

    @Test
    public void testFirstNonNull_SingleElement() {
        assertEquals("a", Nulls.firstNonNull(new String[] { "a" }));
        assertNull(Nulls.firstNonNull(new String[] { null }));
    }

    // ===================== firstNonNull / lastNonNull error paths =====================

    @Test
    public void testFirstNonNull_AllNull_TwoArgs() {
        assertNull(Nulls.firstNonNull(null, null));
    }

    @Test
    public void testFirstNonNull_AllNull_ThreeArgs() {
        assertNull(Nulls.firstNonNull(null, null, null));
    }

    @Test
    public void testFirstNonNullOrDefault_Iterable_NullDefaultThrowsException() {
        List<String> list = Arrays.asList("test");
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.firstNonNullOrDefault(list, null);
        });
    }

    @Test
    public void testFirstNonNullOrDefault_Iterator_NullDefaultThrowsException() {
        Iterator<String> iter = Arrays.asList("test").iterator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            CommonUtil.firstNonNullOrDefault(iter, null);
        });
    }

    @Test
    public void testFirstLastNonNullSupportSingleUseIterables() {
        // regression: N.isEmpty consumed the iterator of single-use iterables, and the body then
        // requested a second iterator -> IllegalStateException for stream::iterator
        final Iterable<String> it1 = java.util.stream.Stream.of(null, "a")::iterator;
        assertEquals("a", Nulls.firstNonNull(it1));

        final Iterable<String> it2 = java.util.stream.Stream.of("b", null)::iterator;
        assertEquals("b", Nulls.lastNonNull(it2));
    }
}
