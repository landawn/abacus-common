package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Stack;
import java.util.Vector;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.OptionalInt;

public class IndexOfTest extends IndexTestSupport {

    @Test
    public void testOf_BooleanArray() {
        final boolean[] array = { true, false, true, false };
        assertEquals(OptionalInt.of(0), Index.of(array, true));
        assertEquals(OptionalInt.of(1), Index.of(array, false));
        assertEquals(OptionalInt.of(2), Index.of(array, true, 2));
        assertEquals(OptionalInt.empty(), Index.of(array, true, 3));
        assertEquals(OptionalInt.empty(), Index.of((boolean[]) null, true));
        assertEquals(OptionalInt.empty(), Index.of(new boolean[0], true));
    }

    @Test
    public void testOf_CharArray() {
        final char[] array = { 'a', 'b', 'c', 'a', '中', '\u0000', '\uffff' };
        assertEquals(OptionalInt.of(0), Index.of(array, 'a'));
        assertEquals(OptionalInt.of(3), Index.of(array, 'a', 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 'd'));
        assertEquals(OptionalInt.of(4), Index.of(array, '中'));
        assertEquals(OptionalInt.of(5), Index.of(array, '\u0000'));
        assertEquals(OptionalInt.of(6), Index.of(array, '\uffff'));
    }

    @Test
    public void testOf_ByteArray() {
        final byte[] array = { 1, 2, 3, 1 };
        assertEquals(OptionalInt.of(0), Index.of(array, (byte) 1));
        assertEquals(OptionalInt.of(3), Index.of(array, (byte) 1, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, (byte) 4));
    }

    @Test
    public void testOf_ShortArray() {
        final short[] array = { 10, 20, 30, 20 };
        assertEquals(OptionalInt.of(1), Index.of(array, (short) 20));
        assertEquals(OptionalInt.of(3), Index.of(array, (short) 20, 2));
        assertEquals(OptionalInt.empty(), Index.of(array, (short) 50));
    }

    @Test
    public void testOf_IntArray() {
        final int[] array = { 100, 200, 300, 200 };
        assertEquals(OptionalInt.of(1), Index.of(array, 200));
        assertEquals(OptionalInt.of(3), Index.of(array, 200, 2));
        assertEquals(OptionalInt.empty(), Index.of(array, 500));
        assertEquals(OptionalInt.of(0), Index.of(array, 100, -3));
        assertEquals(OptionalInt.of(0), Index.of(array, 100, Integer.MIN_VALUE));
        assertEquals(OptionalInt.empty(), Index.of(array, 300, 100));
    }

    @Test
    public void testOf_LongArray() {
        final long[] array = { 1000L, 2000L, 3000L, 2000L };
        assertEquals(OptionalInt.of(1), Index.of(array, 2000L));
        assertEquals(OptionalInt.of(3), Index.of(array, 2000L, 2));
        assertEquals(OptionalInt.empty(), Index.of(array, 5000L));
    }

    @Test
    public void testOf_FloatArray() {
        final float[] array = { 1.1f, 2.2f, 3.3f, 2.2f };
        assertEquals(OptionalInt.of(1), Index.of(array, 2.2f));
        assertEquals(OptionalInt.of(3), Index.of(array, 2.2f, 2));
        assertEquals(OptionalInt.empty(), Index.of(array, 5.5f));
    }

    @Test
    public void testOf_DoubleArray() {
        final double[] array = { 1.0, 2.0, 3.0, 1.05 };
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0));
        assertEquals(OptionalInt.of(1), Index.of(array, 2.0, 1));
        assertEquals(OptionalInt.empty(), Index.of(array, 4.0));
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0, 0.1));
        assertEquals(OptionalInt.of(3), Index.of(array, 1.0, 1, 0.1));
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0, 0.01));
    }

    @Test
    public void testOf_DoubleArray_Tolerance() {
        final double[] array = { 1.0, 1.05, 1.1, 1.15, 1.2 };
        assertEquals(OptionalInt.of(2), Index.of(array, 1.1, 0, 0.0));
        assertEquals(OptionalInt.of(1), Index.of(array, 1.0, 1, 0.051));
        assertEquals(OptionalInt.of(0), Index.of(array, 1.0, 0, 0.04999));
        assertEquals(OptionalInt.empty(), Index.of(array, 0.9, 0, 0.05));
        assertThrows(IllegalArgumentException.class, () -> Index.of(array, 1.25, 0, -0.05));
    }

    @Test
    public void testOf_FloatArray_NaNAndSignedZero() {
        final float[] arr = { 1.0f, -0.0f, 0.0f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NaN };
        assertEquals(OptionalInt.of(1), Index.of(arr, -0.0f));
        assertEquals(OptionalInt.of(2), Index.of(arr, 0.0f));
        assertEquals(OptionalInt.of(3), Index.of(arr, Float.NaN));
        assertEquals(OptionalInt.of(6), Index.of(arr, Float.NaN, 4));
        assertEquals(OptionalInt.of(4), Index.of(arr, Float.POSITIVE_INFINITY));
        assertEquals(OptionalInt.of(5), Index.of(arr, Float.NEGATIVE_INFINITY));

        final BitSet all = Index.allOf(new float[] { 1.0f, Float.NaN, 2.0f }, Float.NaN);
        assertEquals(1, all.cardinality());
        assertTrue(all.get(1));
    }

    @Test
    public void testOf_DoubleArray_SignedZeroAndExtremes() {
        final double[] zeros = { -0.0d, 0.0d };
        assertEquals(OptionalInt.of(0), Index.of(zeros, -0.0d));
        assertEquals(OptionalInt.of(1), Index.of(zeros, 0.0d));

        final double[] arr = { Double.MIN_VALUE, Double.MAX_VALUE, -Double.MIN_VALUE, -Double.MAX_VALUE };
        assertEquals(OptionalInt.of(0), Index.of(arr, Double.MIN_VALUE));
        assertEquals(OptionalInt.of(1), Index.of(arr, Double.MAX_VALUE));
        assertEquals(OptionalInt.of(2), Index.of(arr, -Double.MIN_VALUE));
        assertEquals(OptionalInt.of(3), Index.of(arr, -Double.MAX_VALUE));
    }

    @Test
    public void testOf_ObjectArray() {
        final String[] array = { "a", "b", "c", "a" };
        assertEquals(OptionalInt.of(0), Index.of(array, "a"));
        assertEquals(OptionalInt.of(3), Index.of(array, "a", 1));
        assertEquals(OptionalInt.empty(), Index.of(array, "d"));
        assertEquals(OptionalInt.empty(), Index.of((Object[]) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(new Object[0], "a"));
    }

    @Test
    public void testOf_Collection() {
        final String[] data = { "a", "b", "c", "b", "d" };
        final List<String> list = Arrays.asList(data);
        assertEquals(OptionalInt.of(0), Index.of(list, "a"));
        assertEquals(OptionalInt.of(3), Index.of(list, "b", 2));
        assertEquals(OptionalInt.empty(), Index.of(list, "z"));
        assertEquals(OptionalInt.empty(), Index.of((List<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(Collections.emptyList(), "a"));

        final Vector<String> vector = new Vector<>(list);
        assertEquals(OptionalInt.of(1), Index.of(vector, "b"));
        final Stack<String> stack = new Stack<>();
        Collections.addAll(stack, data);
        assertEquals(OptionalInt.of(1), Index.of(stack, "b"));
        assertEquals(OptionalInt.of(1), Index.of(Collections.unmodifiableList(list), "b"));
    }

    @Test
    public void testOf_Iterator() {
        final List<String> list = Arrays.asList("a", "b", "c", "a", "b");
        assertEquals(OptionalInt.of(0), Index.of(list.iterator(), "a"));
        assertEquals(OptionalInt.of(3), Index.of(list.iterator(), "a", 3));
        assertEquals(OptionalInt.empty(), Index.of(list.iterator(), "d"));
        assertEquals(OptionalInt.empty(), Index.of((Iterator<String>) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of(Collections.emptyIterator(), "a"));
    }

    @Test
    public void testOf_String() {
        final String str = "hello world hello";
        assertEquals(OptionalInt.of(0), Index.of(str, 'h'));
        assertEquals(OptionalInt.of(12), Index.of(str, 'h', 1));
        assertEquals(OptionalInt.empty(), Index.of(str, 'z'));
        assertEquals(OptionalInt.of(0), Index.of(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.of(str, "world"));
        assertEquals(OptionalInt.empty(), Index.of(str, "test"));
        assertEquals(OptionalInt.empty(), Index.of((String) null, 'a'));
        assertEquals(OptionalInt.empty(), Index.of("", 'a'));
        assertEquals(OptionalInt.empty(), Index.of((String) null, "a"));
        assertEquals(OptionalInt.empty(), Index.of("", "a"));

        final String special = "a\tb\nc\rd\0e";
        assertEquals(OptionalInt.of(1), Index.of(special, '\t'));
        assertEquals(OptionalInt.of(3), Index.of(special, '\n'));
        assertEquals(OptionalInt.of(5), Index.of(special, '\r'));
        assertEquals(OptionalInt.of(7), Index.of(special, '\0'));
    }

    @Test
    public void testOf_String_EmptySubstring_Clamping() {
        assertEquals(OptionalInt.of(0), Index.of("aaa", "", -1));
        assertEquals(OptionalInt.of(0), Index.of("aaa", "", 0));
        assertEquals(OptionalInt.of(2), Index.of("aaa", "", 2));
        assertEquals(OptionalInt.of(3), Index.of("aaa", "", 3));
        assertEquals(OptionalInt.of(3), Index.of("aaa", "", 5));
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase("aaa", "", -1));
        assertEquals(OptionalInt.of(3), Index.ofIgnoreCase("aaa", "", 4));
        assertEquals(OptionalInt.of(2), Index.last("aaa", "", 2));
        assertEquals(OptionalInt.of(3), Index.last("aaa", "", 10));
        assertFalse(Index.last("aaa", "", -1).isPresent());
        assertEquals(OptionalInt.of(3), Index.lastOfIgnoreCase("aaa", "", 10));
        assertFalse(Index.lastOfIgnoreCase("aaa", "", -1).isPresent());
        assertFalse(Index.of((String) null, "", 0).isPresent());
        assertFalse(Index.ofIgnoreCase((String) null, "", 0).isPresent());
        assertFalse(Index.last((String) null, "", 0).isPresent());
        assertFalse(Index.lastOfIgnoreCase((String) null, "", 0).isPresent());
    }

    @Test
    public void testOf_String_SupplementaryCodePoint() {
        final String str = "a\uD835\uDD0Ab";
        assertEquals(OptionalInt.of(1), Index.of(str, 0x1D50A));
        assertFalse(Index.of(str, 0x1D50A, 2).isPresent());
        assertEquals(OptionalInt.of(1), Index.last(str, 0x1D50A));
        assertFalse(Index.last(str, 0x1D50A, 0).isPresent());
        assertEquals(OptionalInt.of(3), Index.of(str, 'b'));
    }

    @Test
    public void testOfIgnoreCase() {
        final String str = "Hello World HELLO";
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase(str, "hello"));
        assertEquals(OptionalInt.of(6), Index.ofIgnoreCase(str, "WORLD"));
        assertEquals(OptionalInt.of(12), Index.ofIgnoreCase(str, "hello", 1));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(str, "goodbye"));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(str, "world", 10));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase((String) null, "a"));
        assertEquals(OptionalInt.of(1), Index.ofIgnoreCase("aBcAbC", "BC"));
        assertEquals(OptionalInt.of(4), Index.ofIgnoreCase("aBcAbC", "bc", 2));
        assertEquals(OptionalInt.of(1), Index.ofIgnoreCase("aBcAbC", "bc", -1));
    }

    @Test
    public void testOfSubArray() {
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new boolean[] { true, false, true, true, false }, new boolean[] { true, true }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new char[] { 'a', 'b', 'c', 'd' }, new char[] { 'c', 'd' }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new byte[] { 1, 2, 3, 4 }, new byte[] { 3, 4 }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new short[] { 10, 20, 30, 40 }, new short[] { 30, 40 }));
        assertEquals(OptionalInt.of(0), Index.ofSubArray(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(new int[] { 1, 2, 3, 4, 5, 1, 2, 3 }, 1, new int[] { 1, 2, 3 }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new long[] { 1L, 2L, 3L, 4L }, new long[] { 3L, 4L }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new float[] { 1f, 2f, 3f, 4f }, new float[] { 3f, 4f }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new double[] { 1d, 2d, 3d, 4d }, new double[] { 3d, 4d }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new String[] { "a", "b", "c", "d" }, new String[] { "c", "d" }));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(new String[] { "a", "b", "c" }, new String[] { "b", "d" }));
        assertEquals(OptionalInt.of(0), Index.ofSubArray(new int[] { 1, 2, 3 }, new int[0]));
        assertEquals(OptionalInt.empty(), Index.ofSubArray((int[]) null, new int[] { 1 }));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(new int[] { 1 }, (int[]) null));
    }

    @Test
    public void testOfSubArray_Range() {
        final boolean[] bools = { true, false, true, false, true };
        assertEquals(OptionalInt.of(0), Index.ofSubArray(bools, 0, new boolean[] { false, true, false, true }, 1, 2));
        assertEquals(OptionalInt.of(1), Index.ofSubArray(bools, 0, new boolean[] { false, true, false, true }, 0, 3));
        assertEquals(OptionalInt.of(0), Index.ofSubArray(bools, 0, bools, 0, 0));
        assertEquals(OptionalInt.of(5), Index.ofSubArray(bools, 10, bools, 0, 0));

        assertEquals(OptionalInt.of(1), Index.ofSubArray(new long[] { 9L, 4L, 5L, 6L }, 0, new long[] { 0L, 4L, 5L, 7L }, 1, 2));
        assertEquals(OptionalInt.of(1), Index.ofSubArray(new float[] { 9f, 4f, 5f, 6f }, 0, new float[] { 0f, 4f, 5f, 7f }, 1, 2));
        assertEquals(OptionalInt.of(1), Index.ofSubArray(new double[] { 3d, 7d, 8d, 9d }, 0, new double[] { 0d, 7d, 8d, 1d }, 1, 2));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(new String[] { "x", "y", "b", "c", "d" }, 1, new String[] { "a", "b", "c", "z" }, 1, 2));

        final long[] source = { 1L, 2L, 3L, 4L, 5L };
        final long[] sub = { 2L, 3L, 4L };
        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, 0, sub, 0, 3));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, 0, new long[] { 2L, 9L }, 0, 2));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, 10, sub, 0, 3));
        assertEquals(OptionalInt.empty(), Index.ofSubArray(source, 4, sub, 0, 3));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(source, 2, sub, 0, 0));
        assertEquals(OptionalInt.empty(), Index.ofSubArray((long[]) null, 0, sub, 0, 0));
        assertEquals(OptionalInt.of(1), Index.ofSubArray(source, -1, sub, 0, 3));
        assertEquals(OptionalInt.of(3), Index.ofSubArray(new long[] { 1L, 2L, 3L }, 10, new long[0], 0, 0));
        assertFalse(Index.ofSubArray((char[]) null, 0, new char[0], 0, 0).isPresent());
        assertFalse(Index.ofSubArray(new char[] { 'a' }, 0, (char[]) null, 0, 0).isPresent());
    }

    @Test
    public void testOfSubArray_InvalidRange() {
        final int[] array = { 1, 2, 3, 4, 5 };
        final int[] subArray = { 2, 3, 4 };
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(array, 0, subArray, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(array, 0, subArray, 0, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(array, 0, subArray, 2, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(array, 0, subArray, 1, 5));
    }

    @Test
    public void testOfSubArray_NaNAndSignedZero() {
        final float[] fSource = { 1.0f, Float.NaN, 2.0f, Float.NaN };
        assertEquals(OptionalInt.of(1), Index.ofSubArray(fSource, new float[] { Float.NaN, 2.0f }));
        assertEquals(OptionalInt.of(1), Index.lastOfSubArray(fSource, new float[] { Float.NaN, 2.0f }));
        assertEquals(OptionalInt.of(2), Index.ofSubArray(fSource, new float[] { 2.0f, Float.NaN }));

        final float[] nzSource = { -0.0f, 1.0f };
        assertEquals(OptionalInt.of(0), Index.ofSubArray(nzSource, new float[] { -0.0f }));
        assertFalse(Index.ofSubArray(nzSource, new float[] { 0.0f }).isPresent());

        final double[] dSource = { 1.0, Double.NaN, 3.0 };
        assertEquals(OptionalInt.of(1), Index.ofSubArray(dSource, new double[] { Double.NaN, 3.0 }));
        assertEquals(OptionalInt.of(1), Index.lastOfSubArray(dSource, new double[] { Double.NaN, 3.0 }));

        final double[] dzSource = { -0.0d, 1.0d };
        assertEquals(OptionalInt.of(0), Index.lastOfSubArray(dzSource, new double[] { -0.0d }));
        assertFalse(Index.lastOfSubArray(dzSource, new double[] { 0.0d }).isPresent());
    }

    @Test
    public void testOfSubList() {
        final List<Integer> source = Arrays.asList(1, 2, 3, 4, 5, 1, 2, 3);
        assertEquals(OptionalInt.of(0), Index.ofSubList(source, Arrays.asList(1, 2, 3)));
        assertEquals(OptionalInt.of(5), Index.ofSubList(source, 1, Arrays.asList(1, 2, 3)));

        final List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        assertEquals(OptionalInt.of(2), Index.ofSubList(list, 0, Arrays.asList("extra", "cherry", "date", "extra"), 1, 2));
        assertEquals(OptionalInt.empty(), Index.ofSubList(list, 0, Arrays.asList("x", "y"), 0, 2));
        assertEquals(OptionalInt.empty(), Index.ofSubList(null, 0, list, 0, 0));
        assertEquals(OptionalInt.empty(), Index.ofSubList(list, 0, null, 0, 0));
        assertEquals(OptionalInt.empty(), Index.ofSubList(list, 10, Arrays.asList("a"), 0, 1));

        final List<String> linkedSource = new LinkedList<>(list);
        final List<String> linkedSub = new LinkedList<>(Arrays.asList("banana", "cherry"));
        assertEquals(OptionalInt.of(1), Index.ofSubList(linkedSource, 0, linkedSub, 0, 2));
    }

    @Test
    public void testOfSubList_MixedRandomAccess() {
        final List<String> raSource = Arrays.asList("a", "b", "c", "b", "c", "d");
        final List<String> raSub = Arrays.asList("b", "c");
        final List<String> llSource = new LinkedList<>(raSource);
        final List<String> llSub = new LinkedList<>(raSub);

        assertEquals(OptionalInt.of(1), Index.ofSubList(raSource, llSub));
        assertEquals(OptionalInt.of(3), Index.ofSubList(raSource, 2, llSub));
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(raSource, llSub));
        assertEquals(OptionalInt.of(1), Index.lastOfSubList(raSource, 2, llSub));
        assertEquals(OptionalInt.of(1), Index.ofSubList(llSource, raSub));
        assertEquals(OptionalInt.of(3), Index.ofSubList(llSource, 2, raSub));
        assertEquals(OptionalInt.of(3), Index.lastOfSubList(llSource, raSub));

        final List<String> llSub3 = new LinkedList<>(Arrays.asList("x", "b", "c"));
        assertEquals(OptionalInt.of(3), Index.ofSubList(raSource, 2, llSub3, 1, 2));
        assertEquals(OptionalInt.of(1), Index.lastOfSubList(raSource, 2, llSub3, 1, 2));
        assertEquals(Index.ofSubList(raSource, 2, raSub), Index.ofSubList(raSource, 2, llSub));
        assertEquals(Index.lastOfSubList(raSource, 2, raSub), Index.lastOfSubList(raSource, 2, llSub));
    }

    /**
     * G31-002 contract pin: a negative {@code sizeToMatch} comes back from {@code N.checkFromIndexSize} as an
     * {@link IllegalArgumentException}, not as the {@link IndexOutOfBoundsException} every other invalid slice
     * raises - so it does NOT reach a caller's {@code catch (IndexOutOfBoundsException)}.
     */
    @Test
    public void testSubArrayAndSubList_NegativeSizeToMatchThrowsIllegalArgumentException() {
        final int[] array = { 1, 2, 3 };
        final int[] sub = { 1, 2 };
        final List<Integer> list = Arrays.asList(1, 2, 3);
        final List<Integer> subList = Arrays.asList(1, 2);

        assertThrows(IllegalArgumentException.class, () -> Index.ofSubArray(array, 0, sub, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> Index.lastOfSubArray(array, 2, sub, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> Index.ofSubList(list, 0, subList, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> Index.lastOfSubList(list, 2, subList, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> Index.ofSubArray(new String[] { "a" }, 0, new String[] { "a" }, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> Index.lastOfSubArray(new char[] { 'a' }, 0, new char[] { 'a' }, 0, -1));
        // N.len(null) is 0, so a null pattern with a negative size is still the size complaint, not an NPE
        assertThrows(IllegalArgumentException.class, () -> Index.ofSubArray(array, 0, (int[]) null, 0, -1));

        // IllegalArgumentException is not an IndexOutOfBoundsException, so it escapes the documented catch
        assertFalse(IndexOutOfBoundsException.class.isAssignableFrom(IllegalArgumentException.class));
        RuntimeException caught = null;
        try {
            Index.ofSubArray(array, 0, sub, 0, -1);
        } catch (final IndexOutOfBoundsException e) {
            caught = e;
        } catch (final IllegalArgumentException e) {
            caught = e;
        }
        assertEquals(IllegalArgumentException.class, caught.getClass());

        // every other invalid slice still raises IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(array, 0, sub, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubArray(array, 0, sub, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.lastOfSubArray(array, 2, sub, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.ofSubList(list, 0, subList, -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Index.lastOfSubList(list, 2, subList, 0, 5));
    }

    /**
     * G31-003 contract pin: the ignore-case substring searches are a same-length UTF-16 compare via
     * {@link String#regionMatches(boolean, int, String, int, int)}, not Unicode case folding - a length-changing
     * mapping such as {@code "ß"}/{@code "SS"} does not match, while same-length non-ASCII mappings do.
     */
    @Test
    public void testOfIgnoreCase_SameLengthUtf16CompareNotCaseFolding() {
        final String source = "straße";

        // length-changing mappings do NOT match, on any of the four String overloads
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(source, "STRASSE"));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(source, "SS"));
        assertEquals(OptionalInt.empty(), Index.ofIgnoreCase(source, "SS", 0));
        assertEquals(OptionalInt.empty(), Index.lastOfIgnoreCase(source, "SS"));
        assertEquals(OptionalInt.empty(), Index.lastOfIgnoreCase(source, "SS", source.length()));
        assertEquals("STRASSE", source.toUpperCase(Locale.ROOT)); // a case-folding search WOULD match here

        // same-length non-ASCII mappings DO match, so the contract is "same-length", not "ASCII only"
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase("cafÉ", "café"));
        assertEquals(OptionalInt.of(3), Index.ofIgnoreCase("le CAFÉ", "café"));
        assertEquals(OptionalInt.of(0), Index.ofIgnoreCase("ÄÖÜ", "äöü", 0));
        assertEquals(OptionalInt.of(0), Index.lastOfIgnoreCase("ÄÖÜ", "äöü"));
        assertEquals(OptionalInt.of(0), Index.lastOfIgnoreCase("ÄÖÜ", "äöü", 2));
        assertEquals(OptionalInt.of(4), Index.ofIgnoreCase(source, "ß"));
    }
}
