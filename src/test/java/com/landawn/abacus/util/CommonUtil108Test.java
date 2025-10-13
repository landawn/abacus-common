package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CommonUtil108Test extends TestBase {

    @Test
    public void testCloneDouble3DArray() {
        double[][][] nullArray = null;
        assertNull(N.clone(nullArray));

        double[][][] emptyArray = new double[0][][];
        double[][][] clonedEmpty = N.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        double[][][] original = new double[][][] { { { 1.0, 2.0 }, { 3.0, 4.0 } }, { { 5.0, 6.0 }, { 7.0, 8.0 } } };

        double[][][] cloned = N.clone(original);
        assertNotNull(cloned);
        assertNotSame(original, cloned);
        assertEquals(original.length, cloned.length);

        for (int i = 0; i < original.length; i++) {
            assertNotSame(original[i], cloned[i]);
            assertEquals(original[i].length, cloned[i].length);

            for (int j = 0; j < original[i].length; j++) {
                assertNotSame(original[i][j], cloned[i][j]);
                assertArrayEquals(original[i][j], cloned[i][j]);
            }
        }

        cloned[0][0][0] = 99.0;
        assertNotEquals(original[0][0][0], cloned[0][0][0]);
    }

    @Test
    public void testCloneGeneric3DArray() {
        String[][][] nullArray = null;
        assertNull(N.clone(nullArray));

        String[][][] emptyArray = new String[0][][];
        String[][][] clonedEmpty = N.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        String[][][] original = new String[][][] { { { "a", "b" }, { "c", "d" } }, { { "e", "f" }, { "g", "h" } } };

        String[][][] cloned = N.clone(original);
        assertNotNull(cloned);
        assertNotSame(original, cloned);
        assertEquals(original.length, cloned.length);

        for (int i = 0; i < original.length; i++) {
            assertNotSame(original[i], cloned[i]);
            assertEquals(original[i].length, cloned[i].length);

            for (int j = 0; j < original[i].length; j++) {
                assertNotSame(original[i][j], cloned[i][j]);
                assertArrayEquals(original[i][j], cloned[i][j]);
            }
        }
    }

    @Test
    public void testIsSortedBooleanArray() {
        assertTrue(N.isSorted((boolean[]) null));
        assertTrue(N.isSorted(new boolean[0]));
        assertTrue(N.isSorted(new boolean[] { true }));

        assertTrue(N.isSorted(new boolean[] { false, false, true, true }));
        assertTrue(N.isSorted(new boolean[] { false, true }));

        assertFalse(N.isSorted(new boolean[] { true, false }));
        assertFalse(N.isSorted(new boolean[] { false, true, false }));
    }

    @Test
    public void testIsSortedBooleanArrayRange() {
        boolean[] array = { true, false, false, true, true };

        assertTrue(N.isSorted(array, 1, 3));
        assertTrue(N.isSorted(array, 3, 5));
        assertFalse(N.isSorted(array, 0, 3));

        assertTrue(N.isSorted(array, 2, 2));
        assertTrue(N.isSorted(array, 2, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, 3, 2));
    }

    @Test
    public void testIsSortedCharArray() {
        assertTrue(N.isSorted((char[]) null));
        assertTrue(N.isSorted(new char[0]));
        assertTrue(N.isSorted(new char[] { 'a' }));

        assertTrue(N.isSorted(new char[] { 'a', 'b', 'c', 'd' }));
        assertTrue(N.isSorted(new char[] { 'a', 'a', 'b', 'b' }));

        assertFalse(N.isSorted(new char[] { 'b', 'a' }));
        assertFalse(N.isSorted(new char[] { 'a', 'c', 'b' }));
    }

    @Test
    public void testIsSortedCharArrayRange() {
        char[] array = { 'd', 'a', 'b', 'c', 'e' };

        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(array, -1, 2));
    }

    @Test
    public void testIsSortedByteArray() {
        assertTrue(N.isSorted((byte[]) null));
        assertTrue(N.isSorted(new byte[0]));
        assertTrue(N.isSorted(new byte[] { 1, 2, 3 }));
        assertFalse(N.isSorted(new byte[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedByteArrayRange() {
        byte[] array = { 5, 1, 2, 3, 0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedShortArray() {
        assertTrue(N.isSorted((short[]) null));
        assertTrue(N.isSorted(new short[0]));
        assertTrue(N.isSorted(new short[] { 1, 2, 3 }));
        assertFalse(N.isSorted(new short[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedShortArrayRange() {
        short[] array = { 5, 1, 2, 3, 0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedIntArray() {
        assertTrue(N.isSorted((int[]) null));
        assertTrue(N.isSorted(new int[0]));
        assertTrue(N.isSorted(new int[] { 1, 2, 3 }));
        assertFalse(N.isSorted(new int[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedIntArrayRange() {
        int[] array = { 5, 1, 2, 3, 0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedLongArray() {
        assertTrue(N.isSorted((long[]) null));
        assertTrue(N.isSorted(new long[0]));
        assertTrue(N.isSorted(new long[] { 1L, 2L, 3L }));
        assertFalse(N.isSorted(new long[] { 3L, 2L, 1L }));
    }

    @Test
    public void testIsSortedLongArrayRange() {
        long[] array = { 5L, 1L, 2L, 3L, 0L };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedFloatArray() {
        assertTrue(N.isSorted((float[]) null));
        assertTrue(N.isSorted(new float[0]));
        assertTrue(N.isSorted(new float[] { 1.0f, 2.0f, 3.0f }));
        assertFalse(N.isSorted(new float[] { 3.0f, 2.0f, 1.0f }));

        assertTrue(N.isSorted(new float[] { 1.0f, 2.0f, Float.NaN }));
    }

    @Test
    public void testIsSortedFloatArrayRange() {
        float[] array = { 5.0f, 1.0f, 2.0f, 3.0f, 0.0f };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedDoubleArray() {
        assertTrue(N.isSorted((double[]) null));
        assertTrue(N.isSorted(new double[0]));
        assertTrue(N.isSorted(new double[] { 1.0, 2.0, 3.0 }));
        assertFalse(N.isSorted(new double[] { 3.0, 2.0, 1.0 }));

        assertTrue(N.isSorted(new double[] { 1.0, 2.0, Double.NaN }));
    }

    @Test
    public void testIsSortedDoubleArrayRange() {
        double[] array = { 5.0, 1.0, 2.0, 3.0, 0.0 };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArray() {
        assertTrue(N.isSorted((String[]) null));
        assertTrue(N.isSorted(new String[0]));
        assertTrue(N.isSorted(new String[] { "a", "b", "c" }));
        assertFalse(N.isSorted(new String[] { "c", "b", "a" }));

        assertTrue(N.isSorted(new String[] { null, null, "a", "b" }));
        assertFalse(N.isSorted(new String[] { "a", null, "b" }));
    }

    @Test
    public void testIsSortedObjectArrayRange() {
        String[] array = { "d", "a", "b", "c", "e" };
        assertTrue(N.isSorted(array, 1, 4));
        assertFalse(N.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArrayWithComparator() {
        String[] array = { "aaa", "bb", "c" };

        assertTrue(N.isSorted(array, Comparator.naturalOrder()));

        assertTrue(N.isSorted(array, Comparator.comparing(String::length).reversed()));

        assertTrue(N.isSorted(array, null));
    }

    @Test
    public void testIsSortedObjectArrayRangeWithComparator() {
        String[] array = { "d", "aaa", "bb", "c", "e" };

        assertTrue(N.isSorted(array, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(N.isSorted(array, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testIsSortedCollection() {
        assertTrue(N.isSorted((Collection<String>) null));
        assertTrue(N.isSorted(new ArrayList<String>()));
        assertTrue(N.isSorted(Arrays.asList("a", "b", "c")));
        assertFalse(N.isSorted(Arrays.asList("c", "b", "a")));

        assertTrue(N.isSorted(Arrays.asList(null, null, "a", "b")));
    }

    @Test
    public void testIsSortedCollectionRange() {
        List<String> list = Arrays.asList("d", "a", "b", "c", "e");
        assertTrue(N.isSorted(list, 1, 4));
        assertFalse(N.isSorted(list, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.isSorted(list, 0, 6));
    }

    @Test
    public void testIsSortedCollectionWithComparator() {
        List<String> list = Arrays.asList("aaa", "bb", "c");

        assertTrue(N.isSorted(list, Comparator.naturalOrder()));
        assertTrue(N.isSorted(list, Comparator.comparing(String::length).reversed()));
    }

    @Test
    public void testIsSortedCollectionRangeWithComparator() {
        List<String> list = Arrays.asList("d", "aaa", "bb", "c", "e");

        assertTrue(N.isSorted(list, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(N.isSorted(list, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testSortBooleanArray() {
        N.sort((boolean[]) null);
        N.sort(new boolean[0]);

        boolean[] array = { true, false, true, false, false };
        N.sort(array);
        assertArrayEquals(new boolean[] { false, false, false, true, true }, array);

        boolean[] sorted = { false, false, true, true };
        N.sort(sorted);
        assertArrayEquals(new boolean[] { false, false, true, true }, sorted);
    }

    @Test
    public void testSortBooleanArrayRange() {
        boolean[] array = { true, true, false, true, false };
        N.sort(array, 1, 4);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, array);

        boolean[] array2 = { true, false };
        N.sort(array2, 1, 1);
        assertArrayEquals(new boolean[] { true, false }, array2);

        assertThrows(IndexOutOfBoundsException.class, () -> N.sort(array, -1, 2));
    }

    @Test
    public void testSortCharArray() {
        N.sort((char[]) null);
        N.sort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        N.sort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testSortCharArrayRange() {
        char[] array = { 'd', 'c', 'b', 'a', 'e' };
        N.sort(array, 1, 4);
        assertArrayEquals(new char[] { 'd', 'a', 'b', 'c', 'e' }, array);
    }

    @Test
    public void testSortByteArray() {
        N.sort((byte[]) null);
        N.sort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        N.sort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        N.sort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArray() {
        N.sort((short[]) null);
        N.sort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        N.sort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        N.sort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArray() {
        N.sort((int[]) null);
        N.sort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        N.sort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        N.sort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortLongArray() {
        N.sort((long[]) null);
        N.sort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        N.sort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        N.sort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortFloatArray() {
        N.sort((float[]) null);
        N.sort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        N.sort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        N.sort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortDoubleArray() {
        N.sort((double[]) null);
        N.sort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        N.sort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        N.sort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortObjectArray() {
        N.sort((Object[]) null);
        N.sort(new Object[0]);

        String[] array = { "d", "b", "a", "c" };
        N.sort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);

        String[] arrayWithNulls = { "b", null, "a", null };
        N.sort(arrayWithNulls);
        assertArrayEquals(new String[] { null, null, "a", "b" }, arrayWithNulls);
    }

    @Test
    public void testSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        N.sort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        N.sort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        N.sort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testSortList() {
        N.sort((List<String>) null);
        N.sort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        N.sort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        N.sort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        N.sort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    @Test
    public void testSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        N.sort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);

        List<String> emptyList = new ArrayList<>();
        N.sort(emptyList, 0, 0, Comparator.naturalOrder());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testSortByArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.sortBy(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Bob", people[2].getName());
    }

    @Test
    public void testSortByList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.sortBy(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
        assertEquals("John", people.get(1).getName());
        assertEquals("Bob", people.get(2).getName());
    }

    @Test
    public void testSortByIntArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.sortByInt(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Bob", people[2].getName());
    }

    @Test
    public void testSortByIntList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.sortByInt(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testSortByLongArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.sortByLong(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testSortByLongList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.sortByLong(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testSortByFloatArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.sortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testSortByFloatList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.sortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testSortByDoubleArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.sortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testSortByDoubleList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.sortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortCharArray() {
        N.parallelSort((char[]) null);
        N.parallelSort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        N.parallelSort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testParallelSortCharArrayRange() {
        char[] array = { 'e', 'd', 'c', 'b', 'a' };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new char[] { 'e', 'b', 'c', 'd', 'a' }, array);
    }

    @Test
    public void testParallelSortByteArray() {
        N.parallelSort((byte[]) null);
        N.parallelSort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        N.parallelSort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArray() {
        N.parallelSort((short[]) null);
        N.parallelSort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        N.parallelSort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArray() {
        N.parallelSort((int[]) null);
        N.parallelSort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        N.parallelSort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortLongArray() {
        N.parallelSort((long[]) null);
        N.parallelSort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        N.parallelSort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortFloatArray() {
        N.parallelSort((float[]) null);
        N.parallelSort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        N.parallelSort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortDoubleArray() {
        N.parallelSort((double[]) null);
        N.parallelSort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        N.parallelSort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortObjectArray() {
        N.parallelSort((String[]) null);
        N.parallelSort(new String[0]);

        String[] array = { "d", "b", "a", "c" };
        N.parallelSort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);
    }

    @Test
    public void testParallelSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        N.parallelSort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testParallelSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        N.parallelSort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testParallelSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        N.parallelSort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testParallelSortList() {
        N.parallelSort((List<String>) null);
        N.parallelSort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        N.parallelSort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testParallelSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        N.parallelSort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testParallelSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        N.parallelSort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    @Test
    public void testParallelSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        N.parallelSort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);
    }

    @Test
    public void testParallelSortByArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.parallelSortBy(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Bob", people[2].getName());
    }

    @Test
    public void testParallelSortByList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.parallelSortBy(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByIntArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.parallelSortByInt(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByIntList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.parallelSortByInt(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByLongArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.parallelSortByLong(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByLongList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.parallelSortByLong(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByFloatArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.parallelSortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByFloatList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.parallelSortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByDoubleArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.parallelSortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByDoubleList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.parallelSortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testReverseSortBooleanArray() {
        N.reverseSort((boolean[]) null);

        boolean[] array = { false, true, false, true, true };
        N.reverseSort(array);
        assertArrayEquals(new boolean[] { true, true, true, false, false }, array);
    }

    @Test
    public void testReverseSortBooleanArrayRange() {
        boolean[] array = { false, false, true, false, true };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false, false, true }, array);
    }

    @Test
    public void testReverseSortCharArray() {
        char[] array = { 'a', 'c', 'b', 'd' };
        N.reverseSort(array);
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a' }, array);
    }

    @Test
    public void testReverseSortCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new char[] { 'a', 'd', 'c', 'b', 'e' }, array);
    }

    @Test
    public void testReverseSortByteArray() {
        byte[] array = { 1, 3, 2, 4 };
        N.reverseSort(array);
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortShortArray() {
        short[] array = { 1, 3, 2, 4 };
        N.reverseSort(array);
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortIntArray() {
        int[] array = { 1, 3, 2, 4 };
        N.reverseSort(array);
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortIntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new int[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortLongArray() {
        long[] array = { 1L, 3L, 2L, 4L };
        N.reverseSort(array);
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, array);
    }

    @Test
    public void testReverseSortLongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new long[] { 1L, 4L, 3L, 2L, 5L }, array);
    }

    @Test
    public void testReverseSortFloatArray() {
        float[] array = { 1.0f, 3.0f, 2.0f, 4.0f };
        N.reverseSort(array);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, array);
    }

    @Test
    public void testReverseSortFloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f }, array);
    }

    @Test
    public void testReverseSortDoubleArray() {
        double[] array = { 1.0, 3.0, 2.0, 4.0 };
        N.reverseSort(array);
        assertArrayEquals(new double[] { 4.0, 3.0, 2.0, 1.0 }, array);
    }

    @Test
    public void testReverseSortDoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new double[] { 1.0, 4.0, 3.0, 2.0, 5.0 }, array);
    }

    @Test
    public void testReverseSortObjectArray() {
        String[] array = { "a", "c", "b", "d" };
        N.reverseSort(array);
        assertArrayEquals(new String[] { "d", "c", "b", "a" }, array);
    }

    @Test
    public void testReverseSortObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        N.reverseSort(array, 1, 4);
        assertArrayEquals(new String[] { "a", "d", "c", "b", "e" }, array);
    }

    @Test
    public void testReverseSortList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c", "b", "d"));
        N.reverseSort(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverseSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        N.reverseSort(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testReverseSortByArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.reverseSortBy(people, Person::getAge);
        assertEquals("Bob", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Jane", people[2].getName());
    }

    @Test
    public void testReverseSortByList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.reverseSortBy(people, Person::getAge);
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByIntArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.reverseSortByInt(people, Person::getAge);
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByIntList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.reverseSortByInt(people, Person::getAge);
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByLongArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.reverseSortByLong(people, p -> p.getAge());
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByLongList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.reverseSortByLong(people, p -> p.getAge());
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByFloatArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.reverseSortByFloat(people, p -> p.getAge());
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByFloatList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.reverseSortByFloat(people, p -> p.getAge());
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByDoubleArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        N.reverseSortByDouble(people, p -> p.getAge());
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByDoubleList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        N.reverseSortByDouble(people, p -> p.getAge());
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testBinarySearchCharArray() {
        assertEquals(-1, N.binarySearch((char[]) null, 'a'));
        assertEquals(-1, N.binarySearch(new char[0], 'a'));

        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(0, N.binarySearch(array, 'a'));
        assertEquals(2, N.binarySearch(array, 'c'));
        assertEquals(4, N.binarySearch(array, 'e'));
        assertTrue(N.binarySearch(array, 'f') < 0);
    }

    @Test
    public void testBinarySearchCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(2, N.binarySearch(array, 1, 4, 'c'));
        assertTrue(N.binarySearch(array, 1, 4, 'a') < 0);
        assertTrue(N.binarySearch(array, 1, 4, 'e') < 0);
    }

    @Test
    public void testBinarySearchByteArray() {
        assertEquals(-1, N.binarySearch((byte[]) null, (byte) 1));
        assertEquals(-1, N.binarySearch(new byte[0], (byte) 1));

        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, N.binarySearch(array, (byte) 1));
        assertEquals(2, N.binarySearch(array, (byte) 3));
        assertEquals(4, N.binarySearch(array, (byte) 5));
        assertTrue(N.binarySearch(array, (byte) 6) < 0);
    }

    @Test
    public void testBinarySearchByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(2, N.binarySearch(array, 1, 4, (byte) 3));
        assertTrue(N.binarySearch(array, 1, 4, (byte) 1) < 0);
        assertTrue(N.binarySearch(array, 1, 4, (byte) 5) < 0);
    }

    @Test
    public void testBinarySearchShortArray() {
        assertEquals(-1, N.binarySearch((short[]) null, (short) 1));
        assertEquals(-1, N.binarySearch(new short[0], (short) 1));

        short[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, N.binarySearch(array, (short) 1));
        assertEquals(2, N.binarySearch(array, (short) 3));
        assertEquals(4, N.binarySearch(array, (short) 5));
        assertTrue(N.binarySearch(array, (short) 6) < 0);
    }

    private static class Person implements Comparable<Person> {
        private String name;
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        @Override
        public int compareTo(Person other) {
            return Integer.compare(this.age, other.age);
        }
    }
}
