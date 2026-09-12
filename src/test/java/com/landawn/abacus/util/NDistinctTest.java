package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.Function;

import testfixtures.entity.extendDirty.basic.Account;

public class NDistinctTest extends NTestSupport {

    @Test
    public void testDistinctBySupplierControlsResultOrder() {
        String[] values = { "a", "z", "another" };
        java.util.function.IntFunction<java.util.TreeSet<String>> supplier = size -> new java.util.TreeSet<>(java.util.Comparator.reverseOrder());

        assertEquals(List.of("z", "a"), new java.util.ArrayList<>(N.distinctBy(values, s -> s.charAt(0), supplier)));
        assertEquals(List.of("z", "a"), new java.util.ArrayList<>(N.distinctBy(Arrays.asList(values), s -> s.charAt(0), supplier)));
        assertEquals(List.of("z", "a"), new java.util.ArrayList<>(N.distinctBy(Arrays.asList(values).iterator(), s -> s.charAt(0), supplier)));
        assertEquals(List.of("a", "z"), N.distinctBy(values, s -> s.charAt(0), java.util.ArrayList::new));
    }

    @Test
    public void testDistinct_arrays() {
        assertArrayEquals(new byte[] { 1, 2, 3 }, N.distinct(new byte[] { 1, 2, 1, 3, 2 }));
        assertArrayEquals(new byte[] {}, N.distinct(new byte[] {}));
        assertArrayEquals(new short[] { 1, 2, 3 }, N.distinct(new short[] { 1, 2, 1, 3, 2 }));
        assertArrayEquals(new int[] { 1, 2, 3 }, N.distinct(new int[] { 1, 2, 2, 3, 1, 3 }));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.distinct((int[]) null));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.distinct(new int[0]));
        assertArrayEquals(new int[] { 1 }, N.distinct(duplicateIntArray));
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, N.distinct(new int[] { 1, 2, 3, 4, 5 }));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, N.distinct(new long[] { 1L, 2L, 1L, 3L, 2L }));
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, N.distinct(new float[] { 1.0f, 2.0f, 1.0f, 3.0f }), 0.001f);

        char[] chars = N.distinct(new char[] { 'a', 'b', 'a', 'c', 'b', 'd' });
        assertEquals(4, chars.length);

        int[] arr = { 1, 2, 1, 3, 2, 4, 1 };
        assertArrayEquals(new int[] { 1, 2, 3 }, N.distinct(arr, 0, 4));
        assertArrayEquals(new int[] { 2, 4, 1 }, N.distinct(arr, 4, 7));
        assertArrayEquals(CommonUtil.EMPTY_INT_ARRAY, N.distinct(arr, 2, 2));
    }

    @Test
    public void testDistinct_objects() {
        assertEquals(List.of("a", "b", "c"), N.distinct(new String[] { "a", "b", "a", "c", "b" }));
        assertTrue(N.distinct((String[]) null).isEmpty());
        assertEquals(Arrays.asList("dup"), N.distinct(duplicateStringArray));
        assertEquals(List.of("a", "b", "c"), N.distinct(new String[] { "a", "b", "a", "c", "b", "d", "a" }, 0, 4));
        assertEquals(List.of("b", "d", "a"), N.distinct(new String[] { "a", "b", "a", "c", "b", "d", "a" }, 4, 7));

        List<Integer> withNulls = N.distinct(new Integer[] { 1, null, 2, null, 3, null, 1, 2, 3 });
        assertEquals(4, withNulls.size());
        assertTrue(withNulls.contains(null));

        assertEquals(List.of("x", "y", "z"), N.distinct(Arrays.asList("x", "y", "x", "z", "y")));
        assertTrue(N.distinct((Iterable<String>) null).isEmpty());
        assertEquals(3, N.distinct(Set.of("1", "2", "3")).size());
        assertEquals(3, N.distinct(new NTestSupport.CustomIterable<>(Arrays.asList("x", "y", "x", "z", "y"))).size());
        assertEquals(3, N.distinct(Arrays.asList(1, 2, 3, 2, 4, 3, 5), 1, 6).size());

        assertEquals(List.of("x", "y", "z"), N.distinct(Arrays.asList("x", "y", "x", "z", "y").iterator()));
        assertTrue(N.distinct((Iterator<String>) null).isEmpty());
    }

    @Test
    public void testDistinctBy() {
        String[] arr = { "apple", "apricot", "banana", "avocado", "blueberry" };
        assertEquals(List.of("apple", "banana"), N.distinctBy(arr, s -> s.charAt(0)));
        assertEquals(List.of("apricot", "banana"), N.distinctBy(arr, 1, 4, s -> s.charAt(0)));
        assertEquals(Set.of("apple", "banana"), N.distinctBy(arr, s -> s.charAt(0), HashSet::new));
        assertTrue(N.distinctBy((String[]) null, s -> s.charAt(0)).isEmpty());

        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "blueberry");
        assertEquals(List.of("apricot", "banana"), N.distinctBy(list, 1, 4, s -> s.charAt(0)));
        assertEquals(List.of("cat", "dog"), N.distinctBy(Arrays.asList("cat", "cow", "dog", "deer"), s -> s.charAt(0)));
        assertEquals(Set.of("cat", "dog"), N.distinctBy(Arrays.asList("cat", "cow", "dog", "deer", "dove"), s -> s.charAt(0), HashSet::new));
        assertEquals(0, N.distinctBy(Arrays.asList("a", "b", "c"), 2, 2, s -> s).size());

        List<String> linked = new LinkedList<>(Arrays.asList("apple", "apricot", "banana", "blueberry", "cherry"));
        List<String> fromLinked = N.distinctBy(linked, 0, 4, s -> s.charAt(0));
        assertEquals(List.of("apple", "banana"), fromLinked);

        assertEquals(List.of("cat", "dog"), N.distinctBy(Arrays.asList("cat", "cow", "dog", "deer").iterator(), s -> s.charAt(0)));
        assertEquals(Set.of("cat", "dog"), N.distinctBy(Arrays.asList("cat", "cow", "dog", "deer", "dove").iterator(), s -> s.charAt(0), HashSet::new));

        assertEquals(3, N.distinctBy(stringArray, String::length).size());
        assertEquals(3, N.distinctBy(stringArray, String::length, HashSet::new).size());

        Account[] accounts = { createAccount(Account.class), createAccount(Account.class), createAccount(Account.class) };
        assertEquals(N.distinctBy(accounts, (Function<Account, String>) Account::getFirstName),
                N.distinctBy(CommonUtil.toList(accounts), (Function<Account, String>) Account::getFirstName));
    }

    @Test
    public void testDistinct_floatAndDoubleUseEqualsSemantics() {
        // Float.equals / Double.equals, not ==: the two zeros stay, the two NaNs collapse.
        final float[] zeros = N.distinct(new float[] { 0.0f, -0.0f });
        assertEquals(2, zeros.length);
        assertEquals(Float.floatToIntBits(0.0f), Float.floatToIntBits(zeros[0]));
        assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(zeros[1]));

        final float[] nans = N.distinct(new float[] { Float.NaN, Float.NaN });
        assertEquals(1, nans.length);
        assertTrue(Float.isNaN(nans[0]));

        final double[] dZeros = N.distinct(new double[] { 0.0d, -0.0d });
        assertEquals(2, dZeros.length);
        assertEquals(Double.doubleToLongBits(-0.0d), Double.doubleToLongBits(dZeros[1]));

        final double[] dNans = N.distinct(new double[] { Double.NaN, Double.NaN });
        assertEquals(1, dNans.length);
        assertTrue(Double.isNaN(dNans[0]));
    }
}
