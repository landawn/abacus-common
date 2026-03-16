package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.Vector;
import java.util.function.Predicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;

public class IndexTest {

    @Test
    public void test_indexOfSubArray() {
        final int[] a = { 1, 2, 3, 1, 2, 2, 3, 1, 2, 2, 3 };
        int[] b = { 1, 2, 2, 3 };
        N.println(Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(3, Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));

        N.println(Index.lastOfSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(7, Index.lastOfSubArray(a, 100, b, 0, b.length).orElse(-1));

        b = new int[] { 1, 2, 2, 2, 3 };
        N.println(Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(-1, Index.ofSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(4, Index.ofSubArray(a, 1, b, 2, 3).orElse(-1));
        assertEquals(-1, Index.ofSubList(CommonUtil.toList(a), 0, CommonUtil.toList(b), 0, b.length).orElse(-1));
        assertEquals(4, Index.ofSubList(CommonUtil.toList(a), 1, CommonUtil.toList(b), 2, 3).orElse(-1));

        N.println(Index.lastOfSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(-1, Index.lastOfSubArray(a, 0, b, 0, b.length).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 1, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 2, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubArray(a, a.length - 3, b, 2, 3).orElse(-1));
        assertEquals(4, Index.lastOfSubArray(a, a.length - 4, b, 2, 3).orElse(-1));
        assertEquals(8, Index.lastOfSubList(CommonUtil.toList(a), a.length - 1, CommonUtil.toList(b), 2, 3).orElse(-1));
        assertEquals(4, Index.lastOfSubList(CommonUtil.toList(a), a.length - 4, CommonUtil.toList(b), 2, 3).orElse(-1));

        assertEquals(8, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 1).orElse(-1));
        assertEquals(8, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 2).orElse(-1));
        assertEquals(4, Index.last(Strings.join(a, ""), Strings.join(b, 2, 5, ""), a.length - 4).orElse(-1));
    }

    @Test
    public void test_of_boolean_array() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.of(a, true);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.of(a, false);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        boolean[] b = { true, true };
        result = Index.of(b, false);
        assertFalse(result.isPresent());

        result = Index.of((boolean[]) null, true);
        assertFalse(result.isPresent());

        result = Index.of(new boolean[0], true);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_boolean_array_fromIndex() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.of(a, true, 1);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.of(a, true, 3);
        assertFalse(result.isPresent());

        result = Index.of(a, true, 10);
        assertFalse(result.isPresent());

        result = Index.of(a, true, -1);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.of((boolean[]) null, true, 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_char_array() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.of(a, 'b');
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 'd');
        assertFalse(result.isPresent());

        result = Index.of((char[]) null, 'a');
        assertFalse(result.isPresent());

        result = Index.of(new char[0], 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_char_array_fromIndex() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.of(a, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 'b', -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 'b', 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_byte_array() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (byte) 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (byte) 5);
        assertFalse(result.isPresent());

        result = Index.of((byte[]) null, (byte) 1);
        assertFalse(result.isPresent());

        result = Index.of(new byte[0], (byte) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_byte_array_fromIndex() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (byte) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, (byte) 2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (byte) 2, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_short_array() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (short) 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (short) 5);
        assertFalse(result.isPresent());

        result = Index.of((short[]) null, (short) 1);
        assertFalse(result.isPresent());

        result = Index.of(new short[0], (short) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_short_array_fromIndex() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (short) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, (short) 2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (short) 2, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_int_array() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5);
        assertFalse(result.isPresent());

        result = Index.of((int[]) null, 1);
        assertFalse(result.isPresent());

        result = Index.of(new int[0], 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_int_array_fromIndex() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, 2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_long_array() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.of(a, 2L);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5L);
        assertFalse(result.isPresent());

        result = Index.of((long[]) null, 1L);
        assertFalse(result.isPresent());

        result = Index.of(new long[0], 1L);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_long_array_fromIndex() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.of(a, 2L, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2L, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2L, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_float_array() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.of(a, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5.0f);
        assertFalse(result.isPresent());

        result = Index.of((float[]) null, 1.0f);
        assertFalse(result.isPresent());

        result = Index.of(new float[0], 1.0f);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_float_array_fromIndex() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.of(a, 2.0f, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2.0f, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0f, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.of(a, 2.0);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5.0);
        assertFalse(result.isPresent());

        result = Index.of((double[]) null, 1.0);
        assertFalse(result.isPresent());

        result = Index.of(new double[0], 1.0);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array_fromIndex() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.of(a, 2.0, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2.0, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array_tolerance() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.of(a, 2.0, 0.2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0, 0.05);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of((double[]) null, 2.0, 0.1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array_tolerance_fromIndex() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.of(a, 2.0, 0.2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2.0, 0.2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0, 0.05, 2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Object_array() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.of(a, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, "d");
        assertFalse(result.isPresent());

        String[] b = { "a", null, "c" };
        result = Index.of(b, null);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of((Object[]) null, "a");
        assertFalse(result.isPresent());

        result = Index.of(new Object[0], "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Object_array_fromIndex() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.of(a, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, "b", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, "b", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Collection() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.of(c, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(c, "d");
        assertFalse(result.isPresent());

        List<String> b = Arrays.asList("a", null, "c");
        result = Index.of(b, null);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of((Collection<?>) null, "a");
        assertFalse(result.isPresent());

        result = Index.of(new ArrayList<>(), "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Collection_fromIndex() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.of(c, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(c, "b", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(c, "b", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Iterator<String> iter = list.iterator();
        OptionalInt result = Index.of(iter, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        iter = list.iterator();
        result = Index.of(iter, "d");
        assertFalse(result.isPresent());

        result = Index.of((Iterator<?>) null, "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Iterator_fromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Iterator<String> iter = list.iterator();
        OptionalInt result = Index.of(iter, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        iter = list.iterator();
        result = Index.of(iter, "b", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void test_of_String_char() {
        String str = "abcb";
        OptionalInt result = Index.of(str, 'b');
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, 'd');
        assertFalse(result.isPresent());

        result = Index.of((String) null, 'a');
        assertFalse(result.isPresent());

        result = Index.of("", 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_String_char_fromIndex() {
        String str = "abcb";
        OptionalInt result = Index.of(str, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(str, 'b', -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, 'b', 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_String_String() {
        String str = "abcabc";
        OptionalInt result = Index.of(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, "de");
        assertFalse(result.isPresent());

        result = Index.of((String) null, "a");
        assertFalse(result.isPresent());

        result = Index.of("", "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_String_String_fromIndex() {
        String str = "abcabc";
        OptionalInt result = Index.of(str, "bc", 2);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.of(str, "bc", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, "bc", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofIgnoreCase_String() {
        String str = "aBcAbC";
        OptionalInt result = Index.ofIgnoreCase(str, "BC");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.ofIgnoreCase(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.ofIgnoreCase(str, "DE");
        assertFalse(result.isPresent());

        result = Index.ofIgnoreCase(null, "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofIgnoreCase_String_fromIndex() {
        String str = "aBcAbC";
        OptionalInt result = Index.ofIgnoreCase(str, "BC", 2);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofIgnoreCase(str, "bc", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.ofIgnoreCase(str, "bc", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_boolean() {
        boolean[] source = { true, false, true, true, false };
        boolean[] sub = { true, true };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        boolean[] notFound = { false, false };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new boolean[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((boolean[]) null, sub);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, (boolean[]) null);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_boolean_fromIndex() {
        boolean[] source = { true, false, true, true, false, true, true };
        boolean[] sub = { true, true };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 10, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_boolean_fromIndex_startIndex_size() {
        boolean[] source = { true, false, true, true, false };
        boolean[] sub = { false, true, true, false };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((boolean[]) null, 0, sub, 0, 2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_char() {
        char[] source = { 'a', 'b', 'c', 'd', 'e' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        char[] notFound = { 'x', 'y' };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new char[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((char[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_char_fromIndex() {
        char[] source = { 'a', 'b', 'c', 'd', 'c', 'd' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_char_fromIndex_startIndex_size() {
        char[] source = { 'a', 'b', 'c', 'd', 'e' };
        char[] sub = { 'x', 'c', 'd', 'y' };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_byte() {
        byte[] source = { 1, 2, 3, 4, 5 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        byte[] notFound = { 9, 8 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new byte[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((byte[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_byte_fromIndex() {
        byte[] source = { 1, 2, 3, 4, 3, 4 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_byte_fromIndex_startIndex_size() {
        byte[] source = { 1, 2, 3, 4, 5 };
        byte[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_short() {
        short[] source = { 1, 2, 3, 4, 5 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        short[] notFound = { 9, 8 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new short[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((short[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_short_fromIndex() {
        short[] source = { 1, 2, 3, 4, 3, 4 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_short_fromIndex_startIndex_size() {
        short[] source = { 1, 2, 3, 4, 5 };
        short[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_int() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        int[] notFound = { 9, 8 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new int[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((int[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_int_fromIndex() {
        int[] source = { 1, 2, 3, 4, 3, 4 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_int_fromIndex_startIndex_size() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_long() {
        long[] source = { 1L, 2L, 3L, 4L, 5L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        long[] notFound = { 9L, 8L };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new long[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((long[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_long_fromIndex() {
        long[] source = { 1L, 2L, 3L, 4L, 3L, 4L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_long_fromIndex_startIndex_size() {
        long[] source = { 1L, 2L, 3L, 4L, 5L };
        long[] sub = { 0L, 3L, 4L, 0L };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_float() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        float[] notFound = { 9.0f, 8.0f };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new float[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((float[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_float_fromIndex() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f, 4.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_float_fromIndex_startIndex_size() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] sub = { 0.0f, 3.0f, 4.0f, 0.0f };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_double() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        double[] notFound = { 9.0, 8.0 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new double[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((double[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_double_fromIndex() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 3.0, 4.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_double_fromIndex_startIndex_size() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] sub = { 0.0, 3.0, 4.0, 0.0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_Object() {
        String[] source = { "a", "b", "c", "d", "e" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        String[] notFound = { "x", "y" };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new String[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((Object[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_Object_fromIndex() {
        String[] source = { "a", "b", "c", "d", "c", "d" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_Object_fromIndex_startIndex_size() {
        String[] source = { "a", "b", "c", "d", "e" };
        String[] sub = { "x", "c", "d", "y" };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.ofSubList(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        List<String> notFound = Arrays.asList("x", "y");
        result = Index.ofSubList(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubList(source, new ArrayList<>());
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubList(null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubList_fromIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.ofSubList(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubList(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubList_fromIndex_startIndex_size() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("x", "c", "d", "y");
        OptionalInt result = Index.ofSubList(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubList(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_last_boolean_array() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.last(a, true);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.last(a, false);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        boolean[] b = { true, true };
        result = Index.last(b, false);
        assertFalse(result.isPresent());

        result = Index.last((boolean[]) null, true);
        assertFalse(result.isPresent());

        result = Index.last(new boolean[0], true);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_boolean_array_startIndexFromBack() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.last(a, true, 1);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.last(a, true, 10);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.last(a, true, -1);
        assertFalse(result.isPresent());

        result = Index.last((boolean[]) null, true, 2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_char_array() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.last(a, 'b');
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 'd');
        assertFalse(result.isPresent());

        result = Index.last((char[]) null, 'a');
        assertFalse(result.isPresent());

        result = Index.last(new char[0], 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_char_array_startIndexFromBack() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.last(a, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 'b', 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_byte_array() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (byte) 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, (byte) 5);
        assertFalse(result.isPresent());

        result = Index.last((byte[]) null, (byte) 1);
        assertFalse(result.isPresent());

        result = Index.last(new byte[0], (byte) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_byte_array_startIndexFromBack() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (byte) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, (byte) 2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_short_array() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (short) 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, (short) 5);
        assertFalse(result.isPresent());

        result = Index.last((short[]) null, (short) 1);
        assertFalse(result.isPresent());

        result = Index.last(new short[0], (short) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_short_array_startIndexFromBack() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (short) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, (short) 2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_int_array() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5);
        assertFalse(result.isPresent());

        result = Index.last((int[]) null, 1);
        assertFalse(result.isPresent());

        result = Index.last(new int[0], 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_int_array_startIndexFromBack() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, 2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_long_array() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.last(a, 2L);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5L);
        assertFalse(result.isPresent());

        result = Index.last((long[]) null, 1L);
        assertFalse(result.isPresent());

        result = Index.last(new long[0], 1L);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_long_array_startIndexFromBack() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.last(a, 2L, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2L, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_float_array() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.last(a, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5.0f);
        assertFalse(result.isPresent());

        result = Index.last((float[]) null, 1.0f);
        assertFalse(result.isPresent());

        result = Index.last(new float[0], 1.0f);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_float_array_startIndexFromBack() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.last(a, 2.0f, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2.0f, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_double_array() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.last(a, 2.0);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5.0);
        assertFalse(result.isPresent());

        result = Index.last((double[]) null, 1.0);
        assertFalse(result.isPresent());

        result = Index.last(new double[0], 1.0);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_double_array_startIndexFromBack() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.last(a, 2.0, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2.0, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_double_array_tolerance() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.last(a, 2.0, 0.2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 2.0, 0.05);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last((double[]) null, 2.0, 0.1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_double_array_tolerance_startIndexFromBack() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.last(a, 2.0, 0.2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2.0, 0.2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_Object_array() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.last(a, "b");
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, "d");
        assertFalse(result.isPresent());

        String[] b = { "a", null, "c", null };
        result = Index.last(b, null);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last((Object[]) null, "a");
        assertFalse(result.isPresent());

        result = Index.last(new Object[0], "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_Object_array_startIndexFromBack() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.last(a, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, "b", 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_Collection() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.last(c, "b");
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(c, "d");
        assertFalse(result.isPresent());

        List<String> b = Arrays.asList("a", null, "c", null);
        result = Index.last(b, null);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last((Collection<?>) null, "a");
        assertFalse(result.isPresent());

        result = Index.last(new ArrayList<>(), "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_Collection_startIndexFromBack() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.last(c, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(c, "b", 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_String_char() {
        String str = "abcb";
        OptionalInt result = Index.last(str, 'b');
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(str, 'd');
        assertFalse(result.isPresent());

        result = Index.last((String) null, 'a');
        assertFalse(result.isPresent());

        result = Index.last("", 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_String_char_startIndexFromBack() {
        String str = "abcb";
        OptionalInt result = Index.last(str, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(str, 'b', 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_String_String() {
        String str = "abcabc";
        OptionalInt result = Index.last(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.last(str, "de");
        assertFalse(result.isPresent());

        result = Index.last((String) null, "a");
        assertFalse(result.isPresent());

        result = Index.last("", "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_String_String_startIndexFromBack() {
        String str = "abcabc";
        OptionalInt result = Index.last(str, "bc", 3);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(str, "bc", 10);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());
    }

    @Test
    public void test_lastOfIgnoreCase_String() {
        String str = "aBcAbC";
        OptionalInt result = Index.lastOfIgnoreCase(str, "BC");
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.lastOfIgnoreCase(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.lastOfIgnoreCase(str, "DE");
        assertFalse(result.isPresent());

        result = Index.lastOfIgnoreCase(null, "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfIgnoreCase_String_startIndexFromBack() {
        String str = "aBcAbC";
        OptionalInt result = Index.lastOfIgnoreCase(str, "BC", 3);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.lastOfIgnoreCase(str, "bc", 10);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_boolean() {
        boolean[] source = { true, false, true, true, false, true, true };
        boolean[] sub = { true, true };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());

        boolean[] notFound = { false, false };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new boolean[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());

        result = Index.lastOfSubArray((boolean[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_boolean_startIndexFromBack() {
        boolean[] source = { true, false, true, true, false, true, true };
        boolean[] sub = { true, true };
        OptionalInt result = Index.lastOfSubArray(source, 4, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_boolean_startIndexFromBack_startIndex_size() {
        boolean[] source = { true, false, true, true, false };
        boolean[] sub = { false, true, true, false };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_char() {
        char[] source = { 'a', 'b', 'c', 'd', 'c', 'd' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        char[] notFound = { 'x', 'y' };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new char[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_char_startIndexFromBack() {
        char[] source = { 'a', 'b', 'c', 'd', 'c', 'd' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_char_startIndexFromBack_startIndex_size() {
        char[] source = { 'a', 'b', 'c', 'd', 'e' };
        char[] sub = { 'x', 'c', 'd', 'y' };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_byte() {
        byte[] source = { 1, 2, 3, 4, 3, 4 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        byte[] notFound = { 9, 8 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new byte[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_byte_startIndexFromBack() {
        byte[] source = { 1, 2, 3, 4, 3, 4 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_byte_startIndexFromBack_startIndex_size() {
        byte[] source = { 1, 2, 3, 4, 5 };
        byte[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_short() {
        short[] source = { 1, 2, 3, 4, 3, 4 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        short[] notFound = { 9, 8 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new short[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_short_startIndexFromBack() {
        short[] source = { 1, 2, 3, 4, 3, 4 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_short_startIndexFromBack_startIndex_size() {
        short[] source = { 1, 2, 3, 4, 5 };
        short[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_int() {
        int[] source = { 1, 2, 3, 4, 3, 4 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        int[] notFound = { 9, 8 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new int[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_int_startIndexFromBack() {
        int[] source = { 1, 2, 3, 4, 3, 4 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_int_startIndexFromBack_startIndex_size() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_long() {
        long[] source = { 1L, 2L, 3L, 4L, 3L, 4L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        long[] notFound = { 9L, 8L };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new long[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_long_startIndexFromBack() {
        long[] source = { 1L, 2L, 3L, 4L, 3L, 4L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_long_startIndexFromBack_startIndex_size() {
        long[] source = { 1L, 2L, 3L, 4L, 5L };
        long[] sub = { 0L, 3L, 4L, 0L };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_float() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f, 4.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        float[] notFound = { 9.0f, 8.0f };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new float[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_float_startIndexFromBack() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f, 4.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_float_startIndexFromBack_startIndex_size() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] sub = { 0.0f, 3.0f, 4.0f, 0.0f };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_double() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 3.0, 4.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        double[] notFound = { 9.0, 8.0 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new double[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_double_startIndexFromBack() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 3.0, 4.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_double_startIndexFromBack_startIndex_size() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] sub = { 0.0, 3.0, 4.0, 0.0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_Object() {
        String[] source = { "a", "b", "c", "d", "c", "d" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        String[] notFound = { "x", "y" };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new String[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_Object_startIndexFromBack() {
        String[] source = { "a", "b", "c", "d", "c", "d" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_Object_startIndexFromBack_startIndex_size() {
        String[] source = { "a", "b", "c", "d", "e" };
        String[] sub = { "x", "c", "d", "y" };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.lastOfSubList(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        List<String> notFound = Arrays.asList("x", "y");
        result = Index.lastOfSubList(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubList(source, new ArrayList<>());
        assertTrue(result.isPresent());
        assertEquals(source.size(), result.getAsInt());
    }

    @Test
    public void test_lastOfSubList_startIndexFromBack() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.lastOfSubList(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubList(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubList_startIndexFromBack_startIndex_size() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("x", "c", "d", "y");
        OptionalInt result = Index.lastOfSubList(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubList(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_allOf_boolean_array() {
        boolean[] a = { true, false, true, false, true };
        BitSet result = Index.allOf(a, true);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, false);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        boolean[] b = { true, true };
        result = Index.allOf(b, false);
        assertEquals(0, result.cardinality());

        result = Index.allOf((boolean[]) null, true);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_boolean_array_fromIndex() {
        boolean[] a = { true, false, true, false, true };
        BitSet result = Index.allOf(a, true, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, true, -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(a, true, 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_byte_array() {
        byte[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (byte) 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (byte) 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((byte[]) null, (byte) 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_byte_array_fromIndex() {
        byte[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (byte) 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (byte) 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_char_array() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        BitSet result = Index.allOf(a, 'a');
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 'd');
        assertEquals(0, result.cardinality());

        result = Index.allOf((char[]) null, 'a');
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_char_array_fromIndex() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        BitSet result = Index.allOf(a, 'a', 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 'a', -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_short_array() {
        short[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (short) 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (short) 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((short[]) null, (short) 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_short_array_fromIndex() {
        short[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (short) 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (short) 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_int_array() {
        int[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((int[]) null, 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_int_array_fromIndex() {
        int[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_long_array() {
        long[] a = { 1L, 2L, 1L, 3L, 1L };
        BitSet result = Index.allOf(a, 1L);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5L);
        assertEquals(0, result.cardinality());

        result = Index.allOf((long[]) null, 1L);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_long_array_fromIndex() {
        long[] a = { 1L, 2L, 1L, 3L, 1L };
        BitSet result = Index.allOf(a, 1L, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1L, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_float_array() {
        float[] a = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        BitSet result = Index.allOf(a, 1.0f);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5.0f);
        assertEquals(0, result.cardinality());

        result = Index.allOf((float[]) null, 1.0f);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_float_array_fromIndex() {
        float[] a = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        BitSet result = Index.allOf(a, 1.0f, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1.0f, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_double_array() {
        double[] a = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        BitSet result = Index.allOf(a, 1.0);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5.0);
        assertEquals(0, result.cardinality());

        result = Index.allOf((double[]) null, 1.0);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_fromIndex() {
        double[] a = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        BitSet result = Index.allOf(a, 1.0, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1.0, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_tolerance() {
        double[] a = { 1.0, 2.0, 2.1, 3.0, 2.05 };
        BitSet result = Index.allOf(a, 2.0, 0.2);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0, 0.01);
        assertEquals(1, result.cardinality());
        assertTrue(result.get(1));

        result = Index.allOf((double[]) null, 2.0, 0.1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_tolerance_fromIndex() {
        double[] a = { 1.0, 2.0, 2.1, 3.0, 2.05 };
        BitSet result = Index.allOf(a, 2.0, 0.2, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0, 0.2, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array() {
        String[] a = { "a", "b", "a", "c", "a" };
        BitSet result = Index.allOf(a, "a");
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, "d");
        assertEquals(0, result.cardinality());

        String[] b = { "a", null, "c", null };
        result = Index.allOf(b, (String) null);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        result = Index.allOf((Object[]) null, "a");
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_fromIndex() {
        String[] a = { "a", "b", "a", "c", "a" };
        BitSet result = Index.allOf(a, "a", 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, "a", -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Collection() {
        List<String> c = Arrays.asList("a", "b", "a", "c", "a");
        BitSet result = Index.allOf(c, "a");
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, "d");
        assertEquals(0, result.cardinality());

        List<String> b = Arrays.asList("a", null, "c", null);
        result = Index.allOf(b, (String) null);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        result = Index.allOf((Collection<?>) null, "a");
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_fromIndex() {
        List<String> c = Arrays.asList("a", "b", "a", "c", "a");
        BitSet result = Index.allOf(c, "a", 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, "a", -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_Predicate() {
        String[] a = { "apple", "banana", "avocado", "cherry", "apricot" };
        BitSet result = Index.allOf(a, s -> s.startsWith("a"));
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, s -> s.length() > 6);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf((String[]) null, s -> s.startsWith("a"));
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_Predicate_fromIndex() {
        String[] a = { "apple", "banana", "avocado", "cherry", "apricot" };
        BitSet result = Index.allOf(a, s -> s.startsWith("a"), 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, s -> s.startsWith("a"), -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(a, s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_Predicate() {
        List<String> c = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
        BitSet result = Index.allOf(c, s -> s.startsWith("a"));
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, s -> s.length() > 6);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf((Collection<String>) null, s -> s.startsWith("a"));
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_Predicate_fromIndex() {
        List<String> c = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
        BitSet result = Index.allOf(c, s -> s.startsWith("a"), 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, s -> s.startsWith("a"), -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(c, s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testOfBooleanArray() {
        boolean[] array = { true, false, true, false };
        assertEquals(OptionalInt.of(0), Index.of(array, true));
        assertEquals(OptionalInt.of(1), Index.of(array, false));
        assertEquals(OptionalInt.of(2), Index.of(array, true, 2));
        assertEquals(OptionalInt.empty(), Index.of(array, true, 3));
    }

    @Test
    public void testOfCharArray() {
        char[] array = { 'a', 'b', 'c', 'a' };
        assertEquals(OptionalInt.of(0), Index.of(array, 'a'));
        assertEquals(OptionalInt.of(1), Index.of(array, 'b'));
        assertEquals(OptionalInt.of(3), Index.of(array, 'a', 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 'd'));
    }

    @Test
    public void testOfByteArray() {
        byte[] array = { 1, 2, 3, 1 };
        assertEquals(OptionalInt.of(0), Index.of(array, (byte) 1));
        assertEquals(OptionalInt.of(1), Index.of(array, (byte) 2));
        assertEquals(OptionalInt.of(3), Index.of(array, (byte) 1, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, (byte) 4));
    }

    @Test
    public void testOfShortArray() {
        short[] array = { 1, 2, 3, 1 };
        assertEquals(OptionalInt.of(0), Index.of(array, (short) 1));
        assertEquals(OptionalInt.of(1), Index.of(array, (short) 2));
        assertEquals(OptionalInt.of(3), Index.of(array, (short) 1, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, (short) 4));
    }

    @Test
    public void testOfIntArray() {
        int[] array = { 1, 2, 3, 1 };
        assertEquals(OptionalInt.of(0), Index.of(array, 1));
        assertEquals(OptionalInt.of(1), Index.of(array, 2));
        assertEquals(OptionalInt.of(3), Index.of(array, 1, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4));
    }

    @Test
    public void testOfLongArray() {
        long[] array = { 1L, 2L, 3L, 1L };
        assertEquals(OptionalInt.of(0), Index.of(array, 1L));
        assertEquals(OptionalInt.of(1), Index.of(array, 2L));
        assertEquals(OptionalInt.of(3), Index.of(array, 1L, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4L));
    }

    @Test
    public void testOfFloatArray() {
        float[] array = { 1.0f, 2.0f, 3.0f, 1.0f };
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0f));
        assertEquals(OptionalInt.of(1), Index.of(array, 2.0f));
        assertEquals(OptionalInt.of(3), Index.of(array, 1.0f, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4.0f));
    }

    @Test
    public void testOfDoubleArray() {
        double[] array = { 1.0, 2.0, 3.0, 1.0 };
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0));
        assertEquals(OptionalInt.of(1), Index.of(array, 2.0));
        assertEquals(OptionalInt.of(3), Index.of(array, 1.0, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4.0));
    }

    @Test
    public void testOfDoubleArrayWithTolerance() {
        double[] array = { 1.0, 2.0, 3.0, 1.05 };
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0.1));
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0.01));
        assertEquals(OptionalInt.of(3), Index.of(array, 1.0, 0.1, 1));

    }

    @Test
    public void testOfObjectArray() {
        String[] array = { "a", "b", "c", "a" };
        assertEquals(OptionalInt.of(0), Index.of(array, "a"));
        assertEquals(OptionalInt.of(1), Index.of(array, "b"));
        assertEquals(OptionalInt.of(3), Index.of(array, "a", 1));
        assertEquals(OptionalInt.empty(), Index.of(array, "d"));
        assertEquals(OptionalInt.empty(), Index.of((Object[]) null, "a"));
    }

    @Test
    public void testOfCollection() {
        List<String> list = Arrays.asList("a", "b", "c", "a");
        assertEquals(OptionalInt.of(0), Index.of(list, "a"));
        assertEquals(OptionalInt.of(1), Index.of(list, "b"));
        assertEquals(OptionalInt.of(3), Index.of(list, "a", 1));
        assertEquals(OptionalInt.empty(), Index.of(list, "d"));
    }

    @Test
    public void testOfString() {
        String str = "abcadef";
        assertEquals(OptionalInt.of(0), Index.of(str, 'a'));
        assertEquals(OptionalInt.of(1), Index.of(str, 'b'));
        assertEquals(OptionalInt.of(3), Index.of(str, 'a', 1));
        assertEquals(OptionalInt.empty(), Index.of(str, 'g'));
        assertEquals(OptionalInt.of(0), Index.of(str, "a"));
        assertEquals(OptionalInt.of(3), Index.of(str, "ad"));
        assertEquals(OptionalInt.empty(), Index.of(str, "g"));
    }

    @Test
    public void testOfIgnoreCase() {
        String str = "aBcDeF";
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase(str, "abc"));
        assertEquals(OptionalInt.of(3), Index.ofIgnoreCase(str, "def"));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(str, "ghi"));
    }

    @Test
    public void testOfSubArray() {
        int[] source = { 1, 2, 3, 4, 5, 1, 2, 3 };
        int[] target = { 1, 2, 3 };
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, target));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(source, 1, target));
    }

    @Test
    public void testOfSubList() {
        List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 1, 2, 3);
        List<Integer> target = Arrays.asList(1, 2, 3);
        assertEquals(OptionalInt.of(0), Index.ofSubList(source, target));
        assertEquals(OptionalInt.of(5), Index.ofSubList(source, 1, target));
    }

    @Test
    public void testLast() {
        int[] array = { 1, 2, 3, 1, 2, 3 };
        assertEquals(OptionalInt.of(3), Index.last(array, 1));
        assertEquals(OptionalInt.of(3), Index.last(array, 1, 3));
    }

    @Test
    public void testLastOfSubArray() {
        int[] source = { 1, 2, 3, 4, 5, 1, 2, 3 };
        int[] target = { 1, 2, 3 };
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, target));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 5, target));
        assertEquals(OptionalInt.of(0), Index.lastOfSubArray(source, 4, target));
    }

    @Test
    public void testAllOf() {
        int[] array = { 1, 2, 1, 3, 1 };
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1));
    }

    @Test
    public void testAllOfWithPredicate() {
        String[] array = { "apple", "banana", "avocado", "orange" };
        Predicate<String> startsWithA = s -> s.startsWith("a");
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(2);
        assertEquals(expected, Index.allOf(array, startsWithA));
    }

    @Test
    public void testOf_withNullAndEmptyInputs() {
        assertEquals(OptionalInt.empty(), Index.of((boolean[]) null, true));
        assertEquals(OptionalInt.empty(), Index.of(new boolean[0], true));
        assertEquals(OptionalInt.empty(), Index.of((char[]) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.of(new char[0], 'a'));
        assertEquals(OptionalInt.empty(), Index.of((byte[]) null, (byte) 1));
        assertEquals(OptionalInt.empty(), Index.of(new byte[0], (byte) 1));
        assertEquals(OptionalInt.empty(), Index.of((short[]) null, (short) 1));
        assertEquals(OptionalInt.empty(), Index.of(new short[0], (short) 1));
        assertEquals(OptionalInt.empty(), Index.of((int[]) null, 1));
        assertEquals(OptionalInt.empty(), Index.of(new int[0], 1));
        assertEquals(OptionalInt.empty(), Index.of((long[]) null, 1L));
        assertEquals(OptionalInt.empty(), Index.of(new long[0], 1L));
        assertEquals(OptionalInt.empty(), Index.of((float[]) null, 1.0f));
        assertEquals(OptionalInt.empty(), Index.of(new float[0], 1.0f));
        assertEquals(OptionalInt.empty(), Index.of((double[]) null, 1.0));
        assertEquals(OptionalInt.empty(), Index.of(new double[0], 1.0));

        assertEquals(OptionalInt.empty(), Index.of((Object[]) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(new Object[0], "a"));

        assertEquals(OptionalInt.empty(), Index.of((List<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(Collections.emptyList(), "a"));

        assertEquals(OptionalInt.empty(), Index.of((Iterator<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(Collections.emptyIterator(), "a"));

        assertEquals(OptionalInt.empty(), Index.of((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.of("", 'a'));
        assertEquals(OptionalInt.empty(), Index.of((String) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of("", "a"));
    }

    @Test
    public void testOfIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "a", "b");
        assertEquals(OptionalInt.of(0), Index.of(list.iterator(), "a"));
        assertEquals(OptionalInt.of(1), Index.of(list.iterator(), "b"));
        assertEquals(OptionalInt.of(3), Index.of(list.iterator(), "a", 3));
        assertEquals(OptionalInt.empty(), Index.of(list.iterator(), "d"));
    }

    @Test
    public void testOfDoubleWithTolerance_EdgeCases() {
        double[] array = { 1.0, 1.05, 1.1, 1.15, 1.2 };
        assertEquals(OptionalInt.of(2), Index.of(array, 1.1, 0.0));
        assertEquals(OptionalInt.of(1), Index.of(array, 1.0, 0.051, 1));
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0.04999));
        assertEquals(OptionalInt.empty(), Index.of(array, 0.9, 0.05));
    }

    @Test
    public void testOfSubArray_EdgeCases() {
        int[] source = { 1, 2, 3, 4 };
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, new int[0]));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, 2, new int[0]));
        assertEquals(OptionalInt.of(4), Index.ofSubArray(source, 5, new int[0]));
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, -1, new int[0]));

        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, new int[] { 1, 2, 3, 4, 5 }));

        assertEquals(OptionalInt.empty(), Index.ofSubArray((int[]) null, new int[] { 1 }));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, null));
        assertEquals(OptionalInt.empty(), Index.ofSubArray((int[]) null, null));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, 2, null));

        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, new int[] { 5, 6 }));
    }

    @Test
    public void testOfSubList_EdgeCases() {
        List<Integer> source = Arrays.asList(1, 2, 3, 4);
        assertEquals(OptionalInt.of(0), Index.ofSubList(source, Collections.emptyList()));
        assertEquals(OptionalInt.of(2), Index.ofSubList(source, 2, Collections.emptyList()));

        assertEquals(OptionalInt.empty(), Index.ofSubList(source, Arrays.asList(1, 2, 3, 4, 5)));

        assertEquals(OptionalInt.empty(), Index.ofSubList(null, Arrays.asList(1)));
        assertEquals(OptionalInt.empty(), Index.ofSubList(source, null));
        assertEquals(OptionalInt.empty(), Index.ofSubList(null, null));
    }

    @Test
    public void testLast_withNullAndEmptyInputs() {
        assertEquals(OptionalInt.empty(), Index.last((boolean[]) null, true));
        assertEquals(OptionalInt.empty(), Index.last(new boolean[0], true));

        assertEquals(OptionalInt.empty(), Index.last((Object[]) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last(new Object[0], "a"));

        assertEquals(OptionalInt.empty(), Index.last((List<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last(Collections.emptyList(), "a"));

        assertEquals(OptionalInt.empty(), Index.last((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.last("", 'a'));
        assertEquals(OptionalInt.empty(), Index.last((String) null, "a"));
        assertEquals(OptionalInt.empty(), Index.last("", "a"));
    }

    @Test
    public void testLastOfIgnoreCase() {
        String str = "AbcDefAbc";
        assertEquals(OptionalInt.of(6), Index.lastOfIgnoreCase(str, "ABC"));
        assertEquals(OptionalInt.of(0), Index.lastOfIgnoreCase(str, "ABC", 5));
        assertEquals(OptionalInt.empty(), Index.lastOfIgnoreCase(str, "XYZ"));
    }

    @Test
    public void testLastOfSubArray_EdgeCases() {
        int[] source = { 1, 2, 3, 1, 2, 3, 4 };
        assertEquals(OptionalInt.of(7), Index.lastOfSubArray(source, new int[0]));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 5, new int[0]));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, -1, new int[0]));

        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(new int[] { 1, 2 }, new int[] { 1, 2, 3 }));

        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(null, new int[] { 1 }));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, null));

        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, new int[] { 5, 6 }));
    }

    @Test
    public void testLastOfSubList_EdgeCases() {
        List<Integer> source = Arrays.asList(1, 2, 3, 1, 2, 3, 4);
        List<Integer> target = Arrays.asList(1, 2, 3);
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(source, target));
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(source, 3, target));
        assertEquals(OptionalInt.of(0), Index.lastOfSubList(source, 2, target));
    }

    @Test
    public void testAllOf_withNullAndEmpty() {
        assertTrue(Index.allOf((int[]) null, 1).isEmpty());
        assertTrue(Index.allOf(new int[0], 1).isEmpty());

        assertTrue(Index.allOf((Object[]) null, "a").isEmpty());
        assertTrue(Index.allOf(new Object[0], "a").isEmpty());

        assertTrue(Index.allOf((List<String>) null, "a").isEmpty());
        assertTrue(Index.allOf(Collections.emptyList(), "a").isEmpty());

        Predicate<String> p = s -> s.length() > 0;
        assertTrue(Index.allOf((String[]) null, p).isEmpty());
        assertTrue(Index.allOf(new String[0], p).isEmpty());
        assertTrue(Index.allOf((List<String>) null, p).isEmpty());
        assertTrue(Index.allOf(Collections.emptyList(), p).isEmpty());
    }

    @Test
    public void testAllOf_withFromIndex() {
        int[] array = { 1, 2, 1, 3, 1 };
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1, 1));
        assertEquals(expected, Index.allOf(array, 1, 2));

        BitSet expected2 = new BitSet();
        expected2.set(4);
        assertEquals(expected2, Index.allOf(array, 1, 3));
        assertTrue(Index.allOf(array, 1, 5).isEmpty());
    }

    @Test
    public void testAllOf_collectionWithFromIndex() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "a");
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(list, "a", 1));
    }

    @Test
    public void testAllOf_predicateWithFromIndex() {
        String[] array = { "apple", "banana", "avocado", "orange", "apricot" };
        Predicate<String> startsWithA = s -> s.startsWith("a");
        BitSet expected = new BitSet();
        expected.set(2);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, startsWithA, 1));
    }

    @Test
    public void testAllOf_doubleWithTolerance() {
        double[] array = { 1.0, 1.05, 2.0, 1.1, 0.95 };
        BitSet expected = new BitSet();
        expected.set(0);
        expected.set(1);
        expected.set(3);
        expected.set(4);
        assertEquals(expected, Index.allOf(array, 1.0, 0.1));

        BitSet expectedFromIndex = new BitSet();
        expectedFromIndex.set(1);
        expectedFromIndex.set(3);
        expectedFromIndex.set(4);
        assertEquals(expectedFromIndex, Index.allOf(array, 1.0, 0.1, 1));
    }

    @Test
    public void testOf_BooleanArray() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.of(array, true);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.of(array, false);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(null, true);
        Assertions.assertFalse(result.isPresent());

        result = Index.of(new boolean[0], true);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_BooleanArray_WithFromIndex() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.of(array, true, 1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.of(array, false, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, true, 5);
        Assertions.assertFalse(result.isPresent());

        result = Index.of(array, true, -1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());
    }

    @Test
    public void testOf_CharArray() {
        char[] array = { 'a', 'b', 'c', 'b', 'd' };

        OptionalInt result = Index.of(array, 'b');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 'e');
        Assertions.assertFalse(result.isPresent());

        result = Index.of((char[]) null, 'a');
        Assertions.assertFalse(result.isPresent());

        result = Index.of(new char[0], 'a');
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_CharArray_WithFromIndex() {
        char[] array = { 'a', 'b', 'c', 'b', 'd' };

        OptionalInt result = Index.of(array, 'b', 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, 'a', 1);
        Assertions.assertFalse(result.isPresent());

        result = Index.of(array, 'b', -1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());
    }

    @Test
    public void testOf_ByteArray() {
        byte[] array = { 1, 2, 3, 2, 4 };

        OptionalInt result = Index.of(array, (byte) 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, (byte) 5);
        Assertions.assertFalse(result.isPresent());

        result = Index.of((byte[]) null, (byte) 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_ByteArray_WithFromIndex() {
        byte[] array = { 1, 2, 3, 2, 4 };

        OptionalInt result = Index.of(array, (byte) 2, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, (byte) 1, 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_ShortArray() {
        short[] array = { 10, 20, 30, 20, 40 };

        OptionalInt result = Index.of(array, (short) 20);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, (short) 50);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_ShortArray_WithFromIndex() {
        short[] array = { 10, 20, 30, 20, 40 };

        OptionalInt result = Index.of(array, (short) 20, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_IntArray() {
        int[] array = { 100, 200, 300, 200, 400 };

        OptionalInt result = Index.of(array, 200);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 500);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_IntArray_WithFromIndex() {
        int[] array = { 100, 200, 300, 200, 400 };

        OptionalInt result = Index.of(array, 200, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_LongArray() {
        long[] array = { 1000L, 2000L, 3000L, 2000L, 4000L };

        OptionalInt result = Index.of(array, 2000L);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 5000L);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_LongArray_WithFromIndex() {
        long[] array = { 1000L, 2000L, 3000L, 2000L, 4000L };

        OptionalInt result = Index.of(array, 2000L, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_FloatArray() {
        float[] array = { 1.1f, 2.2f, 3.3f, 2.2f, 4.4f };

        OptionalInt result = Index.of(array, 2.2f);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 5.5f);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_FloatArray_WithFromIndex() {
        float[] array = { 1.1f, 2.2f, 3.3f, 2.2f, 4.4f };

        OptionalInt result = Index.of(array, 2.2f, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_DoubleArray() {
        double[] array = { 1.11, 2.22, 3.33, 2.22, 4.44 };

        OptionalInt result = Index.of(array, 2.22);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 5.55);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_DoubleArray_WithFromIndex() {
        double[] array = { 1.11, 2.22, 3.33, 2.22, 4.44 };

        OptionalInt result = Index.of(array, 2.22, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_DoubleArray_WithTolerance() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.of(array, 2.0, 0.01);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 3.0, 0.01);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.of(array, 2.5, 0.1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_DoubleArray_WithToleranceAndFromIndex() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.of(array, 3.0, 0.01, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.of(array, 3.0, 0.01, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_ObjectArray() {
        String[] array = { "apple", "banana", "cherry", "banana", "date" };

        OptionalInt result = Index.of(array, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, "grape");
        Assertions.assertFalse(result.isPresent());

        result = Index.of(array, null);
        Assertions.assertFalse(result.isPresent());

        String[] arrayWithNull = { "apple", null, "cherry" };
        result = Index.of(arrayWithNull, null);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());
    }

    @Test
    public void testOf_ObjectArray_WithFromIndex() {
        String[] array = { "apple", "banana", "cherry", "banana", "date" };

        OptionalInt result = Index.of(array, "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, "apple", 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(list, "grape");
        Assertions.assertFalse(result.isPresent());

        result = Index.of((Collection<?>) null, "apple");
        Assertions.assertFalse(result.isPresent());

        result = Index.of(new ArrayList<>(), "apple");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_Collection_WithFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list, "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(list, "apple", 1);
        Assertions.assertFalse(result.isPresent());

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.of(linkedList, "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_Iterator() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list.iterator(), "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(list.iterator(), "grape");
        Assertions.assertFalse(result.isPresent());

        result = Index.of((Iterator<?>) null, "apple");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_Iterator_WithFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list.iterator(), "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(list.iterator(), "apple", 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_Char() {
        String str = "hello world";

        OptionalInt result = Index.of(str, 'o');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get());

        result = Index.of(str, 'z');
        Assertions.assertFalse(result.isPresent());

        result = Index.of((String) null, 'a');
        Assertions.assertFalse(result.isPresent());

        result = Index.of("", 'a');
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_Char_WithFromIndex() {
        String str = "hello world";

        OptionalInt result = Index.of(str, 'o', 5);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(7, result.get());

        result = Index.of(str, 'h', 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_String() {
        String str = "hello world hello";

        OptionalInt result = Index.of(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.of(str, "world");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());

        result = Index.of(str, "goodbye");
        Assertions.assertFalse(result.isPresent());

        result = Index.of((String) null, "hello");
        Assertions.assertFalse(result.isPresent());

        result = Index.of(str, null);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_String_WithFromIndex() {
        String str = "hello world hello";

        OptionalInt result = Index.of(str, "hello", 1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());

        result = Index.of(str, "world", 10);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfIgnoreCase_String() {
        String str = "Hello World HELLO";

        OptionalInt result = Index.ofIgnoreCase(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofIgnoreCase(str, "WORLD");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());

        result = Index.ofIgnoreCase(str, "goodbye");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfIgnoreCase_String_WithFromIndex() {
        String str = "Hello World HELLO";

        OptionalInt result = Index.ofIgnoreCase(str, "hello", 1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());

        result = Index.ofIgnoreCase(str, "world", 10);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfSubArray_BooleanArray() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        boolean[] subArray2 = { false, true };
        result = Index.ofSubArray(array, subArray2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        boolean[] notFound = { false, false, false };
        result = Index.ofSubArray(array, notFound);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(null, subArray);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(array, null);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(array, new boolean[0]);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());
    }

    @Test
    public void testOfSubArray_BooleanArray_WithFromIndex() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.ofSubArray(array, 2, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.ofSubArray(array, 4, subArray);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(array, -1, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());
    }

    @Test
    public void testOfSubArray_BooleanArray_WithRange() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { false, true, false, true };

        OptionalInt result = Index.ofSubArray(array, 0, subArray, 1, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofSubArray(array, 0, subArray, 0, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.ofSubArray(array, 0, subArray, 0, 0);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofSubArray(array, 10, subArray, 0, 0);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());
    }

    @Test
    public void testOfSubArray_CharArray() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        char[] subArray = { 'c', 'd' };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_ByteArray() {
        byte[] array = { 1, 2, 3, 4, 5 };
        byte[] subArray = { 3, 4 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_ShortArray() {
        short[] array = { 10, 20, 30, 40, 50 };
        short[] subArray = { 30, 40 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_IntArray() {
        int[] array = { 100, 200, 300, 400, 500 };
        int[] subArray = { 300, 400 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_LongArray() {
        long[] array = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] subArray = { 3000L, 4000L };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_FloatArray() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        float[] subArray = { 3.3f, 4.4f };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_DoubleArray() {
        double[] array = { 1.11, 2.22, 3.33, 4.44, 5.55 };
        double[] subArray = { 3.33, 4.44 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_ObjectArray() {
        String[] array = { "apple", "banana", "cherry", "date", "elderberry" };
        String[] subArray = { "cherry", "date" };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        String[] notFound = { "banana", "date" };
        result = Index.ofSubArray(array, notFound);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfSubList_WithFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "cherry");
        List<String> subList = Arrays.asList("apple", "banana");

        OptionalInt result = Index.ofSubList(list, 1, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.ofSubList(list, 3, subList);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfSubList_WithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        List<String> subList = Arrays.asList("extra", "cherry", "date", "extra");

        OptionalInt result = Index.ofSubList(list, 0, subList, 1, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubList_NonRandomAccess() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("apple", "banana", "cherry", "date"));
        LinkedList<String> subList = new LinkedList<>(Arrays.asList("banana", "cherry"));

        OptionalInt result = Index.ofSubList(list, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());
    }

    @Test
    public void testLast_BooleanArray() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.last(array, true);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get());

        result = Index.last(array, false);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.last(null, true);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast_BooleanArray_WithStartIndex() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.last(array, true, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.last(array, false, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.last(array, true, -1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast_CharArray() {
        char[] array = { 'a', 'b', 'c', 'b', 'd' };

        OptionalInt result = Index.last(array, 'b');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_ByteArray() {
        byte[] array = { 1, 2, 3, 2, 4 };

        OptionalInt result = Index.last(array, (byte) 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_ShortArray() {
        short[] array = { 10, 20, 30, 20, 40 };

        OptionalInt result = Index.last(array, (short) 20);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_IntArray() {
        int[] array = { 100, 200, 300, 200, 400 };

        OptionalInt result = Index.last(array, 200);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_LongArray() {
        long[] array = { 1000L, 2000L, 3000L, 2000L, 4000L };

        OptionalInt result = Index.last(array, 2000L);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_FloatArray() {
        float[] array = { 1.1f, 2.2f, 3.3f, 2.2f, 4.4f };

        OptionalInt result = Index.last(array, 2.2f);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_DoubleArray() {
        double[] array = { 1.11, 2.22, 3.33, 2.22, 4.44 };

        OptionalInt result = Index.last(array, 2.22);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_DoubleArray_WithTolerance() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.last(array, 3.0, 0.01);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_DoubleArray_WithToleranceAndStartIndex() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.last(array, 3.0, 0.01, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.last(array, 3.0, 0.01, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testLast_ObjectArray() {
        String[] array = { "apple", "banana", "cherry", "banana", "date" };

        OptionalInt result = Index.last(array, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.last(list, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.last(linkedList, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_String_Char() {
        String str = "hello world";

        OptionalInt result = Index.last(str, 'o');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(7, result.get());

        result = Index.last(str, 'z');
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast_String_String() {
        String str = "hello world hello";

        OptionalInt result = Index.last(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());

        result = Index.last(str, "world");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());
    }

    @Test
    public void testLastOfIgnoreCase_String() {
        String str = "Hello World HELLO";

        OptionalInt result = Index.lastOfIgnoreCase(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());
    }

    @Test
    public void testLastOfSubArray_BooleanArray() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.lastOfSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.lastOfSubArray(array, new boolean[0]);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());
    }

    @Test
    public void testLastOfSubArray_BooleanArray_WithStartIndex() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.lastOfSubArray(array, 1, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.lastOfSubArray(array, -1, subArray);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLastOfSubArray_CharArray() {
        char[] array = { 'a', 'b', 'c', 'a', 'b', 'c' };
        char[] subArray = { 'a', 'b' };

        OptionalInt result = Index.lastOfSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLastOfSubArray_ObjectArray() {
        String[] array = { "apple", "banana", "apple", "banana", "cherry" };
        String[] subArray = { "apple", "banana" };

        OptionalInt result = Index.lastOfSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testLastOfSubList() {
        List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "cherry");
        List<String> subList = Arrays.asList("apple", "banana");

        OptionalInt result = Index.lastOfSubList(list, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.lastOfSubList(list, new ArrayList<>());
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());
    }

    @Test
    public void testLastOfSubList_NonRandomAccess() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("apple", "banana", "apple", "banana"));
        LinkedList<String> subList = new LinkedList<>(Arrays.asList("apple", "banana"));

        OptionalInt result = Index.lastOfSubList(list, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testAllOf_BooleanArray() {
        boolean[] array = { true, false, true, false, true };

        BitSet result = Index.allOf(array, true);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, false);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        result = Index.allOf(null, true);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_BooleanArray_WithFromIndex() {
        boolean[] array = { true, false, true, false, true };

        BitSet result = Index.allOf(array, true, 2);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, true, 10);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_ByteArray() {
        byte[] array = { 1, 2, 1, 2, 1 };

        BitSet result = Index.allOf(array, (byte) 1);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_CharArray() {
        char[] array = { 'a', 'b', 'a', 'b', 'a' };

        BitSet result = Index.allOf(array, 'a');
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_ShortArray() {
        short[] array = { 10, 20, 10, 20, 10 };

        BitSet result = Index.allOf(array, (short) 10);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_IntArray() {
        int[] array = { 100, 200, 100, 200, 100 };

        BitSet result = Index.allOf(array, 100);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_LongArray() {
        long[] array = { 1000L, 2000L, 1000L, 2000L, 1000L };

        BitSet result = Index.allOf(array, 1000L);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_FloatArray() {
        float[] array = { 1.1f, 2.2f, 1.1f, 2.2f, 1.1f };

        BitSet result = Index.allOf(array, 1.1f);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_DoubleArray() {
        double[] array = { 1.11, 2.22, 1.11, 2.22, 1.11 };

        BitSet result = Index.allOf(array, 1.11);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_DoubleArray_WithTolerance() {
        double[] array = { 1.0, 2.001, 1.01, 2.999, 0.99 };

        BitSet result = Index.allOf(array, 1.0, 0.02);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_ObjectArray() {
        String[] array = { "apple", "banana", "apple", "banana", "apple" };

        BitSet result = Index.allOf(array, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, (String) null);
        Assertions.assertEquals(0, result.cardinality());

        String[] arrayWithNull = { "apple", null, "apple", null };
        result = Index.allOf(arrayWithNull, (String) null);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "apple");

        BitSet result = Index.allOf(list, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.allOf(linkedList, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_Array_WithPredicate() {
        String[] array = { "apple", "apricot", "banana", "avocado", "cherry" };

        BitSet result = Index.allOf(array, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        result = Index.allOf(array, s -> s.length() > 10);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_Array_WithPredicate_FromIndex() {
        String[] array = { "apple", "apricot", "banana", "avocado", "cherry" };

        BitSet result = Index.allOf(array, s -> s.startsWith("a"), 2);
        Assertions.assertEquals(1, result.cardinality());
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Collection_WithPredicate() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "cherry");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.allOf(linkedList, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Collection_WithPredicate_FromIndex() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "cherry");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"), 2);
        Assertions.assertEquals(1, result.cardinality());
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testEdgeCases_EmptyArrays() {
        OptionalInt result = Index.of(new int[0], 1);
        Assertions.assertFalse(result.isPresent());

        result = Index.last(new int[0], 1);
        Assertions.assertFalse(result.isPresent());

        BitSet bitSet = Index.allOf(new int[0], 1);
        Assertions.assertEquals(0, bitSet.cardinality());
    }

    @Test
    public void testEdgeCases_NullInputs() {
        OptionalInt result = Index.of((int[]) null, 1);
        Assertions.assertFalse(result.isPresent());

        result = Index.last((int[]) null, 1);
        Assertions.assertFalse(result.isPresent());

        BitSet bitSet = Index.allOf((int[]) null, 1);
        Assertions.assertEquals(0, bitSet.cardinality());
    }

    @Test
    public void testEdgeCases_LargeFromIndex() {
        int[] array = { 1, 2, 3, 4, 5 };

        OptionalInt result = Index.of(array, 3, 100);
        Assertions.assertFalse(result.isPresent());

        BitSet bitSet = Index.allOf(array, 3, 100);
        Assertions.assertEquals(0, bitSet.cardinality());
    }

    @Test
    public void testEdgeCases_NegativeFromIndex() {
        int[] array = { 1, 2, 3, 4, 5 };

        OptionalInt result = Index.of(array, 1, -10);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        BitSet bitSet = Index.allOf(array, 1, -10);
        Assertions.assertEquals(1, bitSet.cardinality());
        Assertions.assertTrue(bitSet.get(0));
    }

    @Test
    public void testOfSubArray_EmptySubArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        int[] emptySubArray = {};

        OptionalInt result = Index.ofSubArray(array, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofSubArray(array, 2, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testLastOfSubArray_EmptySubArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        int[] emptySubArray = {};

        OptionalInt result = Index.lastOfSubArray(array, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());

        result = Index.lastOfSubArray(array, 3, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOfSubArray_IndexOutOfBounds() {
        int[] array = { 1, 2, 3, 4, 5 };
        int[] subArray = { 2, 3, 4 };

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Index.ofSubArray(array, 0, subArray, -1, 2);
        });

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Index.ofSubArray(array, 0, subArray, 0, 10);
        });

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Index.ofSubArray(array, 0, subArray, 2, 2);
        });
    }

    @Test
    public void testOf_FloatArray_SpecialValues() {
        float[] arr = { 1.0f, -0.0f, 0.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN };

        assertEquals(OptionalInt.of(1), Index.of(arr, -0.0f));
        assertEquals(OptionalInt.of(2), Index.of(arr, 0.0f));

        assertEquals(OptionalInt.of(3), Index.of(arr, Float.NaN));
        assertEquals(OptionalInt.of(6), Index.of(arr, Float.NaN, 4));

        assertEquals(OptionalInt.of(4), Index.of(arr, Float.POSITIVE_INFINITY));
        assertEquals(OptionalInt.of(5), Index.of(arr, Float.NEGATIVE_INFINITY));
    }

    @Test
    public void testOf_DoubleArray_EdgeCases() {
        double[] arr = { Double.MIN_VALUE, Double.MAX_VALUE, -Double.MIN_VALUE, -Double.MAX_VALUE };

        assertEquals(OptionalInt.of(0), Index.of(arr, Double.MIN_VALUE));
        assertEquals(OptionalInt.of(1), Index.of(arr, Double.MAX_VALUE));
        assertEquals(OptionalInt.of(2), Index.of(arr, -Double.MIN_VALUE));
        assertEquals(OptionalInt.of(3), Index.of(arr, -Double.MAX_VALUE));

        double[] toleranceArr = { 1.0, 1.1, 1.2, 1.3, 1.4, 1.5 };
        assertEquals(OptionalInt.of(2), Index.of(toleranceArr, 1.25, 0.051));
        assertEquals(OptionalInt.empty(), Index.of(toleranceArr, 1.25, 0.04));

        assertThrows(IllegalArgumentException.class, () -> Index.of(toleranceArr, 1.25, -0.05));
    }

    @Test
    public void testOf_CharArray_UnicodeCharacters() {
        char[] arr = { 'a', '中', '文', '\u0000', '\uffff' };

        assertEquals(OptionalInt.of(1), Index.of(arr, '中'));
        assertEquals(OptionalInt.of(2), Index.of(arr, '文'));
        assertEquals(OptionalInt.of(3), Index.of(arr, '\u0000'));
        assertEquals(OptionalInt.of(4), Index.of(arr, '\uffff'));
    }

    @Test
    public void testOf_Collection_VariousImplementations() {
        String[] data = { "a", "b", "c", "b", "d" };

        Vector<String> vector = new Vector<>(Arrays.asList(data));
        assertEquals(OptionalInt.of(1), Index.of(vector, "b"));
        assertEquals(OptionalInt.of(3), Index.of(vector, "b", 2));

        Stack<String> stack = new Stack<>();
        Collections.addAll(stack, data);
        assertEquals(OptionalInt.of(1), Index.of(stack, "b"));

        List<String> unmodifiable = Collections.unmodifiableList(Arrays.asList(data));
        assertEquals(OptionalInt.of(1), Index.of(unmodifiable, "b"));
    }

    @Test
    public void testOf_String_ComplexCases() {
        assertEquals(OptionalInt.of(0), Index.of("", ""));
        assertEquals(OptionalInt.of(0), Index.of("hello", ""));
        assertEquals(OptionalInt.of(3), Index.of("hello", "", 3));
        assertEquals(OptionalInt.empty(), Index.of("hello", "", 10));

        String special = "a\tb\nc\rd\0e";
        assertEquals(OptionalInt.of(1), Index.of(special, '\t'));
        assertEquals(OptionalInt.of(3), Index.of(special, '\n'));
        assertEquals(OptionalInt.of(5), Index.of(special, '\r'));
        assertEquals(OptionalInt.of(7), Index.of(special, '\0'));

        String pattern = "aabaabaaab";
        assertEquals(OptionalInt.of(0), Index.of(pattern, "aab"));
        assertEquals(OptionalInt.of(3), Index.of(pattern, "aab", 1));
        assertEquals(OptionalInt.of(7), Index.of(pattern, "aab", 4));
    }

    @Test
    public void testOfSubArray_BoundaryConditions() {
        int[] source = { 1, 2, 3, 4, 5 };

        int[] largeSub = { 1, 2, 3, 4, 5, 6 };
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, largeSub));

        int[] equalSub = { 1, 2, 3, 4, 5 };
        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, equalSub));

        int[] singleSub = { 3 };
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, singleSub));

        assertEquals(OptionalInt.empty(), Index.ofSubArray(null, singleSub));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, null));
        assertEquals(OptionalInt.empty(), Index.ofSubArray((int[]) null, (int[]) null));
    }

    @Test
    public void testLast_PrimitiveArrays_AllTypes() {
        boolean[] boolArr = { true, false, true, false, true };
        assertEquals(OptionalInt.of(4), Index.last(boolArr, true));
        assertEquals(OptionalInt.of(3), Index.last(boolArr, false));
        assertEquals(OptionalInt.of(2), Index.last(boolArr, true, 3));

        char[] charArr = { 'a', 'b', 'c', 'b', 'a' };
        assertEquals(OptionalInt.of(4), Index.last(charArr, 'a'));
        assertEquals(OptionalInt.of(3), Index.last(charArr, 'b'));

        byte[] byteArr = { 1, 2, 3, 2, 1 };
        assertEquals(OptionalInt.of(4), Index.last(byteArr, (byte) 1));
        assertEquals(OptionalInt.of(3), Index.last(byteArr, (byte) 2));

        short[] shortArr = { 10, 20, 30, 20, 10 };
        assertEquals(OptionalInt.of(4), Index.last(shortArr, (short) 10));
        assertEquals(OptionalInt.of(3), Index.last(shortArr, (short) 20));

        long[] longArr = { 100L, 200L, 300L, 200L, 100L };
        assertEquals(OptionalInt.of(4), Index.last(longArr, 100L));
        assertEquals(OptionalInt.of(3), Index.last(longArr, 200L));

        float[] floatArr = { 1.1f, 2.2f, 3.3f, 2.2f, 1.1f };
        assertEquals(OptionalInt.of(4), Index.last(floatArr, 1.1f));
        assertEquals(OptionalInt.of(3), Index.last(floatArr, 2.2f));
    }

    @Test
    public void testLast_String_SpecialCases() {
        assertEquals(OptionalInt.of(5), Index.last("hello", ""));
        assertEquals(OptionalInt.of(3), Index.last("hello", "", 3));
        assertEquals(OptionalInt.empty(), Index.last("hello", "", -1));

        String repeating = "abababab";
        assertEquals(OptionalInt.of(6), Index.last(repeating, "ab"));
        assertEquals(OptionalInt.of(4), Index.last(repeating, "ab", 5));
        assertEquals(OptionalInt.of(2), Index.last(repeating, "ab", 3));
    }

    @Test
    public void testLastOfSubArray_AllPrimitiveTypes() {
        boolean[] boolSource = { true, false, true, false, true, false };
        boolean[] boolSub = { true, false };
        assertEquals(OptionalInt.of(4), Index.lastOfSubArray(boolSource, boolSub));
        assertEquals(OptionalInt.of(2), Index.lastOfSubArray(boolSource, 3, boolSub));

        char[] charSource = "abcabcabc".toCharArray();
        char[] charSub = "abc".toCharArray();
        assertEquals(OptionalInt.of(6), Index.lastOfSubArray(charSource, charSub));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(charSource, 5, charSub));

        byte[] byteSource = { 1, 2, 3, 1, 2, 3, 1, 2, 3 };
        byte[] byteSub = { 1, 2, 3 };
        assertEquals(OptionalInt.of(6), Index.lastOfSubArray(byteSource, byteSub));

        short[] shortSource = { 10, 20, 30, 10, 20, 30 };
        short[] shortSub = { 10, 20, 30 };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(shortSource, shortSub));

        int[] intSource = { 10, 20, 30, 10, 20, 30 };
        int[] intSub = { 10, 20, 30 };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(intSource, intSub));

        long[] longSource = { 100L, 200L, 300L, 100L, 200L, 300L };
        long[] longSub = { 100L, 200L, 300L };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(longSource, longSub));

        float[] floatSource = { 1.1f, 2.2f, 3.3f, 1.1f, 2.2f, 3.3f };
        float[] floatSub = { 1.1f, 2.2f };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(floatSource, floatSub));

        double[] doubleSource = { 1.1, 2.2, 3.3, 1.1, 2.2, 3.3 };
        double[] doubleSub = { 1.1, 2.2 };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(doubleSource, doubleSub));

        String[] strSource = { "10", "20", "30", "10", "20", "30" };
        String[] strSub = { "10", "20", "30" };
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(strSource, strSub));
    }

    @Test
    public void testLastOfSubArray_SpecialCases() {
        int[] source = { 1, 2, 3, 4, 5 };

        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 10, new int[0]));

        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, -1, new int[] { 1 }));

        int[] endSource = { 1, 2, 3, 4, 5, 6, 7 };
        int[] endSub = { 6, 7 };
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(endSource, endSub));
    }

    @Test
    public void testAllOf_AllPrimitiveTypes() {
        boolean[] boolArr = { true, false, true, false, true };
        BitSet boolResult = Index.allOf(boolArr, true);
        assertTrue(boolResult.get(0));
        assertTrue(boolResult.get(2));
        assertTrue(boolResult.get(4));
        assertEquals(3, boolResult.cardinality());

        char[] charArr = { 'a', 'b', 'a', 'c', 'a' };
        BitSet charResult = Index.allOf(charArr, 'a');
        assertTrue(charResult.get(0));
        assertTrue(charResult.get(2));
        assertTrue(charResult.get(4));
        assertEquals(3, charResult.cardinality());

        byte[] byteArr = { 1, 2, 1, 3, 1 };
        BitSet byteResult = Index.allOf(byteArr, (byte) 1);
        assertTrue(byteResult.get(0));
        assertTrue(byteResult.get(2));
        assertTrue(byteResult.get(4));
        assertEquals(3, byteResult.cardinality());

        short[] shortArr = { 10, 20, 10, 30, 10 };
        BitSet shortResult = Index.allOf(shortArr, (short) 10);
        assertTrue(shortResult.get(0));
        assertTrue(shortResult.get(2));
        assertTrue(shortResult.get(4));
        assertEquals(3, shortResult.cardinality());

        long[] longArr = { 100L, 200L, 100L, 300L, 100L };
        BitSet longResult = Index.allOf(longArr, 100L);
        assertTrue(longResult.get(0));
        assertTrue(longResult.get(2));
        assertTrue(longResult.get(4));
        assertEquals(3, longResult.cardinality());

        float[] floatArr = { 1.1f, 2.2f, 1.1f, 3.3f, 1.1f };
        BitSet floatResult = Index.allOf(floatArr, 1.1f);
        assertTrue(floatResult.get(0));
        assertTrue(floatResult.get(2));
        assertTrue(floatResult.get(4));
        assertEquals(3, floatResult.cardinality());
    }

    @Test
    public void testAllOf_FloatSpecialValues() {
        float[] arr = { 1.0f, Float.NaN, 2.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NaN };

        BitSet nanResult = Index.allOf(arr, Float.NaN);
        assertTrue(nanResult.get(1));
        assertTrue(nanResult.get(3));
        assertTrue(nanResult.get(5));
        assertEquals(3, nanResult.cardinality());

        BitSet infResult = Index.allOf(arr, Float.POSITIVE_INFINITY);
        assertTrue(infResult.get(4));
        assertEquals(1, infResult.cardinality());
    }

    @Test
    public void testAllOf_WithPredicate_ComplexConditions() {
        String[] strArr = { "apple", null, "application", "banana", null, "apply" };

        BitSet nullResult = Index.allOf(strArr, s -> s == null);
        assertTrue(nullResult.get(1));
        assertTrue(nullResult.get(4));
        assertEquals(2, nullResult.cardinality());

        BitSet appResult = Index.allOf(strArr, s -> s != null && s.startsWith("app"));
        assertTrue(appResult.get(0));
        assertTrue(appResult.get(2));
        assertTrue(appResult.get(5));
        assertEquals(3, appResult.cardinality());

        Integer[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12 };

        BitSet div3Result = Index.allOf(numbers, n -> n % 3 == 0);
        assertTrue(div3Result.get(2));
        assertTrue(div3Result.get(5));
        assertTrue(div3Result.get(8));
        assertTrue(div3Result.get(11));
        assertEquals(4, div3Result.cardinality());

        BitSet primeResult = Index.allOf(numbers, n -> {
            if (n < 2)
                return false;
            for (int i = 2; i * i <= n; i++) {
                if (n % i == 0)
                    return false;
            }
            return true;
        });
        assertTrue(primeResult.get(1));
        assertTrue(primeResult.get(2));
        assertTrue(primeResult.get(4));
        assertTrue(primeResult.get(6));
        assertTrue(primeResult.get(10));
        assertEquals(5, primeResult.cardinality());
    }

    @Test
    public void testAllOf_EmptyResultSet() {
        int[] arr = { 1, 2, 3, 4, 5 };

        BitSet result = Index.allOf(arr, 10);
        assertTrue(result.isEmpty());
        assertEquals(0, result.cardinality());

        Integer[] intArr = { 1, 2, 3, 4, 5 };
        result = Index.allOf(intArr, n -> n > 10);
        assertTrue(result.isEmpty());
        assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_LargeArrayPerformance() {
        int size = 10000;
        int[] largeArray = new int[size];
        for (int i = 0; i < size; i++) {
            largeArray[i] = i % 10;
        }

        BitSet result = Index.allOf(largeArray, 5);
        assertEquals(1000, result.cardinality());

        assertTrue(result.get(5));
        assertTrue(result.get(15));
        assertTrue(result.get(9995));
        assertFalse(result.get(0));
        assertFalse(result.get(9999));
    }

    @Test
    public void testStressTest_MixedNullElements() {
        Object[] arr = new Object[100];
        for (int i = 0; i < arr.length; i++) {
            arr[i] = (i % 3 == 0) ? null : "value" + (i % 5);
        }

        BitSet nulls = Index.allOf(arr, (Object) null);
        assertEquals(34, nulls.cardinality());

        BitSet value0 = Index.allOf(arr, "value0");
        assertEquals(13, value0.cardinality());
    }

    @Test
    public void testBoundaryConditions_MaxArraySize() {
        int[] arr = new int[1000];
        Arrays.fill(arr, 42);
        arr[0] = 1;
        arr[500] = 1;
        arr[999] = 1;

        BitSet result = Index.allOf(arr, 1);
        assertTrue(result.get(0));
        assertTrue(result.get(500));
        assertTrue(result.get(999));
        assertEquals(3, result.cardinality());
    }

    @Test
    public void testThreadSafety_Considerations() {

        final int[] sharedArray = { 1, 2, 3, 2, 1, 2, 3 };
        final int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            assertEquals(OptionalInt.of(1), Index.of(sharedArray, 2));
            assertEquals(OptionalInt.of(5), Index.last(sharedArray, 2));
            BitSet all2s = Index.allOf(sharedArray, 2);
            assertEquals(3, all2s.cardinality());
        }
    }

    @Test
    public void testOf_String() {
        String str = "hello world";

        assertEquals(OptionalInt.of(0), Index.of(str, 'h'));
        assertEquals(OptionalInt.of(2), Index.of(str, 'l'));
        assertEquals(OptionalInt.of(3), Index.of(str, 'l', 3));
        assertEquals(OptionalInt.empty(), Index.of(str, 'z'));

        assertEquals(OptionalInt.of(0), Index.of(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.of(str, "world"));
        assertEquals(OptionalInt.empty(), Index.of(str, "test"));
        assertEquals(OptionalInt.of(2), Index.of(str, "ll"));

        assertEquals(OptionalInt.empty(), Index.of((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.of((String) null, "test"));
    }

    @Test
    public void testLast_String() {
        String str = "hello world hello";

        assertEquals(OptionalInt.of(15), Index.last(str, 'l'));
        assertEquals(OptionalInt.of(12), Index.last(str, 'h'));
        assertEquals(OptionalInt.empty(), Index.last(str, 'z'));

        assertEquals(OptionalInt.of(12), Index.last(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.last(str, "world"));
        assertEquals(OptionalInt.empty(), Index.last(str, "test"));
    }

    @Test
    public void testLastOfSubArray_IntArray() {
        int[] source = { 1, 2, 3, 4, 2, 3, 5, 2, 3 };
        int[] sub1 = { 2, 3 };
        int[] sub2 = { 3, 5 };
        int[] sub3 = { 1, 3 };

        assertEquals(OptionalInt.of(7), Index.lastOfSubArray(source, sub1));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, sub2));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, sub3));
        assertEquals(OptionalInt.of(4), Index.lastOfSubArray(source, 6, sub1));

        assertEquals(OptionalInt.of(9), Index.lastOfSubArray(source, new int[0]));
        assertEquals(OptionalInt.of(5), Index.lastOfSubArray(source, 5, new int[0]));
    }

    @Test
    public void testAllOf_WithPredicate_Array() {
        Integer[] arr = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        BitSet result = Index.allOf(arr, n -> n % 2 == 0);
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertTrue(result.get(5));
        assertTrue(result.get(7));
        assertTrue(result.get(9));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
        assertFalse(result.get(6));
        assertFalse(result.get(8));

        result = Index.allOf(arr, n -> n > 5, 5);
        assertTrue(result.get(5));
        assertTrue(result.get(6));
        assertTrue(result.get(7));
        assertTrue(result.get(8));
        assertTrue(result.get(9));
        assertFalse(result.get(0));
        assertFalse(result.get(4));
    }

    @Test
    public void testAllOf_WithPredicate_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"));
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));
        assertFalse(result.get(1));
        assertFalse(result.get(3));
    }

    @Test
    public void testEdgeCases_EmptyArraysAndCollections() {
        assertEquals(OptionalInt.empty(), Index.of(new int[0], 1));
        assertEquals(OptionalInt.empty(), Index.last(new int[0], 1));
        assertTrue(Index.allOf(new int[0], 1).isEmpty());

        List<String> emptyList = new ArrayList<>();
        assertEquals(OptionalInt.empty(), Index.of(emptyList, "a"));
        assertEquals(OptionalInt.empty(), Index.last(emptyList, "a"));
        assertTrue(Index.allOf(emptyList, "a").isEmpty());
    }

    @Test
    public void testOfSubArray_InvalidRange() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 2, 3, 4 };

        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(source, 0, sub, 1, 5));
    }

    @Test
    public void testSpecialValues_NaN() {
        double[] arr = { 1.0, Double.NaN, 3.0, Double.NaN, 5.0 };

        assertEquals(OptionalInt.of(1), Index.of(arr, Double.NaN));
        assertEquals(OptionalInt.of(3), Index.last(arr, Double.NaN));

        BitSet result = Index.allOf(arr, Double.NaN);
        assertTrue(result.get(1));
        assertTrue(result.get(3));
        assertFalse(result.get(0));
        assertFalse(result.get(2));
        assertFalse(result.get(4));
    }

    @Test
    public void testSpecialValues_InfinityValues() {
        double[] arr = { 1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY, 5.0 };

        assertEquals(OptionalInt.of(1), Index.of(arr, Double.POSITIVE_INFINITY));
        assertEquals(OptionalInt.of(3), Index.of(arr, Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testOfSubArray_ObjectEmptySubArrayWithFromIndex() {
        final Object[] source = { "a", "b", "c" };
        final Object[] empty = {};

        assertEquals(OptionalInt.of(0), Index.ofSubArray(source, -1, empty, 0, 0));
        assertEquals(OptionalInt.of(source.length), Index.ofSubArray(source, 10, empty, 0, 0));
        assertEquals(OptionalInt.empty(), Index.ofSubArray((Object[]) null, 0, empty, 0, 0));
    }

    @Test
    public void testLastOfSubArray_DoubleEmptySubArrayWithStartIndex() {
        final double[] source = { 1.0, 2.0, 3.0 };
        final double[] empty = {};

        assertEquals(OptionalInt.of(source.length), Index.lastOfSubArray(source, 10, empty, 0, 0));
        assertEquals(OptionalInt.of(1), Index.lastOfSubArray(source, 1, empty, 0, 0));
        assertEquals(OptionalInt.empty(), Index.lastOfSubArray(source, -1, empty, 0, 0));
    }

    @Test
    public void testOfSubArray_SubRangeForAdditionalTypes() {
        assertEquals(OptionalInt.of(1), Index.ofSubArray(new long[] { 9L, 4L, 5L, 6L }, 0, new long[] { 0L, 4L, 5L, 7L }, 1, 2));
        assertEquals(OptionalInt.of(1), Index.ofSubArray(new float[] { 9f, 4f, 5f, 6f }, 0, new float[] { 0f, 4f, 5f, 7f }, 1, 2));
        assertEquals(OptionalInt.of(1), Index.ofSubArray(new double[] { 3d, 7d, 8d, 9d }, 0, new double[] { 0d, 7d, 8d, 1d }, 1, 2));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new String[] { "x", "y", "b", "c", "d" }, 1, new String[] { "a", "b", "c", "z" }, 1, 2));
        assertEquals(OptionalInt.of(3), Index.lastOfSubArray(new String[] { "a", "b", "c", "b", "c" }, 4, new String[] { "x", "b", "c", "y" }, 1, 2));
    }

}
