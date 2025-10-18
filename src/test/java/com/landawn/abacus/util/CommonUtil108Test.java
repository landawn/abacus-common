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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CommonUtil108Test extends TestBase {

    @Test
    public void testCloneDouble3DArray() {
        double[][][] nullArray = null;
        assertNull(CommonUtil.clone(nullArray));

        double[][][] emptyArray = new double[0][][];
        double[][][] clonedEmpty = CommonUtil.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        double[][][] original = new double[][][] { { { 1.0, 2.0 }, { 3.0, 4.0 } }, { { 5.0, 6.0 }, { 7.0, 8.0 } } };

        double[][][] cloned = CommonUtil.clone(original);
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
        assertNull(CommonUtil.clone(nullArray));

        String[][][] emptyArray = new String[0][][];
        String[][][] clonedEmpty = CommonUtil.clone(emptyArray);
        assertNotNull(clonedEmpty);
        assertEquals(0, clonedEmpty.length);

        String[][][] original = new String[][][] { { { "a", "b" }, { "c", "d" } }, { { "e", "f" }, { "g", "h" } } };

        String[][][] cloned = CommonUtil.clone(original);
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
        assertTrue(CommonUtil.isSorted((boolean[]) null));
        assertTrue(CommonUtil.isSorted(new boolean[0]));
        assertTrue(CommonUtil.isSorted(new boolean[] { true }));

        assertTrue(CommonUtil.isSorted(new boolean[] { false, false, true, true }));
        assertTrue(CommonUtil.isSorted(new boolean[] { false, true }));

        assertFalse(CommonUtil.isSorted(new boolean[] { true, false }));
        assertFalse(CommonUtil.isSorted(new boolean[] { false, true, false }));
    }

    @Test
    public void testIsSortedBooleanArrayRange() {
        boolean[] array = { true, false, false, true, true };

        assertTrue(CommonUtil.isSorted(array, 1, 3));
        assertTrue(CommonUtil.isSorted(array, 3, 5));
        assertFalse(CommonUtil.isSorted(array, 0, 3));

        assertTrue(CommonUtil.isSorted(array, 2, 2));
        assertTrue(CommonUtil.isSorted(array, 2, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, 3, 2));
    }

    @Test
    public void testIsSortedCharArray() {
        assertTrue(CommonUtil.isSorted((char[]) null));
        assertTrue(CommonUtil.isSorted(new char[0]));
        assertTrue(CommonUtil.isSorted(new char[] { 'a' }));

        assertTrue(CommonUtil.isSorted(new char[] { 'a', 'b', 'c', 'd' }));
        assertTrue(CommonUtil.isSorted(new char[] { 'a', 'a', 'b', 'b' }));

        assertFalse(CommonUtil.isSorted(new char[] { 'b', 'a' }));
        assertFalse(CommonUtil.isSorted(new char[] { 'a', 'c', 'b' }));
    }

    @Test
    public void testIsSortedCharArrayRange() {
        char[] array = { 'd', 'a', 'b', 'c', 'e' };

        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(array, -1, 2));
    }

    @Test
    public void testIsSortedByteArray() {
        assertTrue(CommonUtil.isSorted((byte[]) null));
        assertTrue(CommonUtil.isSorted(new byte[0]));
        assertTrue(CommonUtil.isSorted(new byte[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new byte[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedByteArrayRange() {
        byte[] array = { 5, 1, 2, 3, 0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedShortArray() {
        assertTrue(CommonUtil.isSorted((short[]) null));
        assertTrue(CommonUtil.isSorted(new short[0]));
        assertTrue(CommonUtil.isSorted(new short[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new short[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedShortArrayRange() {
        short[] array = { 5, 1, 2, 3, 0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedIntArray() {
        assertTrue(CommonUtil.isSorted((int[]) null));
        assertTrue(CommonUtil.isSorted(new int[0]));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3 }));
        assertFalse(CommonUtil.isSorted(new int[] { 3, 2, 1 }));
    }

    @Test
    public void testIsSortedIntArrayRange() {
        int[] array = { 5, 1, 2, 3, 0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedLongArray() {
        assertTrue(CommonUtil.isSorted((long[]) null));
        assertTrue(CommonUtil.isSorted(new long[0]));
        assertTrue(CommonUtil.isSorted(new long[] { 1L, 2L, 3L }));
        assertFalse(CommonUtil.isSorted(new long[] { 3L, 2L, 1L }));
    }

    @Test
    public void testIsSortedLongArrayRange() {
        long[] array = { 5L, 1L, 2L, 3L, 0L };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedFloatArray() {
        assertTrue(CommonUtil.isSorted((float[]) null));
        assertTrue(CommonUtil.isSorted(new float[0]));
        assertTrue(CommonUtil.isSorted(new float[] { 1.0f, 2.0f, 3.0f }));
        assertFalse(CommonUtil.isSorted(new float[] { 3.0f, 2.0f, 1.0f }));

        assertTrue(CommonUtil.isSorted(new float[] { 1.0f, 2.0f, Float.NaN }));
    }

    @Test
    public void testIsSortedFloatArrayRange() {
        float[] array = { 5.0f, 1.0f, 2.0f, 3.0f, 0.0f };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedDoubleArray() {
        assertTrue(CommonUtil.isSorted((double[]) null));
        assertTrue(CommonUtil.isSorted(new double[0]));
        assertTrue(CommonUtil.isSorted(new double[] { 1.0, 2.0, 3.0 }));
        assertFalse(CommonUtil.isSorted(new double[] { 3.0, 2.0, 1.0 }));

        assertTrue(CommonUtil.isSorted(new double[] { 1.0, 2.0, Double.NaN }));
    }

    @Test
    public void testIsSortedDoubleArrayRange() {
        double[] array = { 5.0, 1.0, 2.0, 3.0, 0.0 };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArray() {
        assertTrue(CommonUtil.isSorted((String[]) null));
        assertTrue(CommonUtil.isSorted(new String[0]));
        assertTrue(CommonUtil.isSorted(new String[] { "a", "b", "c" }));
        assertFalse(CommonUtil.isSorted(new String[] { "c", "b", "a" }));

        assertTrue(CommonUtil.isSorted(new String[] { null, null, "a", "b" }));
        assertFalse(CommonUtil.isSorted(new String[] { "a", null, "b" }));
    }

    @Test
    public void testIsSortedObjectArrayRange() {
        String[] array = { "d", "a", "b", "c", "e" };
        assertTrue(CommonUtil.isSorted(array, 1, 4));
        assertFalse(CommonUtil.isSorted(array, 0, 3));
    }

    @Test
    public void testIsSortedObjectArrayWithComparator() {
        String[] array = { "aaa", "bb", "c" };

        assertTrue(CommonUtil.isSorted(array, Comparator.naturalOrder()));

        assertTrue(CommonUtil.isSorted(array, Comparator.comparing(String::length).reversed()));

        assertTrue(CommonUtil.isSorted(array, null));
    }

    @Test
    public void testIsSortedObjectArrayRangeWithComparator() {
        String[] array = { "d", "aaa", "bb", "c", "e" };

        assertTrue(CommonUtil.isSorted(array, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(CommonUtil.isSorted(array, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testIsSortedCollection() {
        assertTrue(CommonUtil.isSorted((Collection<String>) null));
        assertTrue(CommonUtil.isSorted(new ArrayList<String>()));
        assertTrue(CommonUtil.isSorted(Arrays.asList("a", "b", "c")));
        assertFalse(CommonUtil.isSorted(Arrays.asList("c", "b", "a")));

        assertTrue(CommonUtil.isSorted(Arrays.asList(null, null, "a", "b")));
    }

    @Test
    public void testIsSortedCollectionRange() {
        List<String> list = Arrays.asList("d", "a", "b", "c", "e");
        assertTrue(CommonUtil.isSorted(list, 1, 4));
        assertFalse(CommonUtil.isSorted(list, 0, 3));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(list, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.isSorted(list, 0, 6));
    }

    @Test
    public void testIsSortedCollectionWithComparator() {
        List<String> list = Arrays.asList("aaa", "bb", "c");

        assertTrue(CommonUtil.isSorted(list, Comparator.naturalOrder()));
        assertTrue(CommonUtil.isSorted(list, Comparator.comparing(String::length).reversed()));
    }

    @Test
    public void testIsSortedCollectionRangeWithComparator() {
        List<String> list = Arrays.asList("d", "aaa", "bb", "c", "e");

        assertTrue(CommonUtil.isSorted(list, 1, 4, Comparator.comparing(String::length).reversed()));
        assertFalse(CommonUtil.isSorted(list, 0, 3, Comparator.naturalOrder()));
    }

    @Test
    public void testSortBooleanArray() {
        CommonUtil.sort((boolean[]) null);
        CommonUtil.sort(new boolean[0]);

        boolean[] array = { true, false, true, false, false };
        CommonUtil.sort(array);
        assertArrayEquals(new boolean[] { false, false, false, true, true }, array);

        boolean[] sorted = { false, false, true, true };
        CommonUtil.sort(sorted);
        assertArrayEquals(new boolean[] { false, false, true, true }, sorted);
    }

    @Test
    public void testSortBooleanArrayRange() {
        boolean[] array = { true, true, false, true, false };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new boolean[] { true, false, true, true, false }, array);

        boolean[] array2 = { true, false };
        CommonUtil.sort(array2, 1, 1);
        assertArrayEquals(new boolean[] { true, false }, array2);

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.sort(array, -1, 2));
    }

    @Test
    public void testSortCharArray() {
        CommonUtil.sort((char[]) null);
        CommonUtil.sort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        CommonUtil.sort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testSortCharArrayRange() {
        char[] array = { 'd', 'c', 'b', 'a', 'e' };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new char[] { 'd', 'a', 'b', 'c', 'e' }, array);
    }

    @Test
    public void testSortByteArray() {
        CommonUtil.sort((byte[]) null);
        CommonUtil.sort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        CommonUtil.sort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArray() {
        CommonUtil.sort((short[]) null);
        CommonUtil.sort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        CommonUtil.sort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArray() {
        CommonUtil.sort((int[]) null);
        CommonUtil.sort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        CommonUtil.sort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testSortLongArray() {
        CommonUtil.sort((long[]) null);
        CommonUtil.sort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        CommonUtil.sort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testSortFloatArray() {
        CommonUtil.sort((float[]) null);
        CommonUtil.sort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        CommonUtil.sort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testSortDoubleArray() {
        CommonUtil.sort((double[]) null);
        CommonUtil.sort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        CommonUtil.sort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testSortObjectArray() {
        CommonUtil.sort((Object[]) null);
        CommonUtil.sort(new Object[0]);

        String[] array = { "d", "b", "a", "c" };
        CommonUtil.sort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);

        String[] arrayWithNulls = { "b", null, "a", null };
        CommonUtil.sort(arrayWithNulls);
        assertArrayEquals(new String[] { null, null, "a", "b" }, arrayWithNulls);
    }

    @Test
    public void testSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        CommonUtil.sort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        CommonUtil.sort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        CommonUtil.sort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testSortList() {
        CommonUtil.sort((List<String>) null);
        CommonUtil.sort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        CommonUtil.sort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        CommonUtil.sort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        CommonUtil.sort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    @Test
    public void testSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        CommonUtil.sort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);

        List<String> emptyList = new ArrayList<>();
        CommonUtil.sort(emptyList, 0, 0, Comparator.naturalOrder());
        assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testSortByArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.sortBy(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Bob", people[2].getName());
    }

    @Test
    public void testSortByList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.sortBy(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
        assertEquals("John", people.get(1).getName());
        assertEquals("Bob", people.get(2).getName());
    }

    @Test
    public void testSortByIntArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.sortByInt(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Bob", people[2].getName());
    }

    @Test
    public void testSortByIntList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.sortByInt(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testSortByLongArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.sortByLong(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testSortByLongList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.sortByLong(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testSortByFloatArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.sortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testSortByFloatList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.sortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testSortByDoubleArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.sortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testSortByDoubleList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.sortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortCharArray() {
        CommonUtil.parallelSort((char[]) null);
        CommonUtil.parallelSort(new char[0]);

        char[] array = { 'd', 'b', 'a', 'c' };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, array);
    }

    @Test
    public void testParallelSortCharArrayRange() {
        char[] array = { 'e', 'd', 'c', 'b', 'a' };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new char[] { 'e', 'b', 'c', 'd', 'a' }, array);
    }

    @Test
    public void testParallelSortByteArray() {
        CommonUtil.parallelSort((byte[]) null);
        CommonUtil.parallelSort(new byte[0]);

        byte[] array = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortByteArrayRange() {
        byte[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new byte[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArray() {
        CommonUtil.parallelSort((short[]) null);
        CommonUtil.parallelSort(new short[0]);

        short[] array = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new short[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortShortArrayRange() {
        short[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new short[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArray() {
        CommonUtil.parallelSort((int[]) null);
        CommonUtil.parallelSort(new int[0]);

        int[] array = { 4, 2, 1, 3 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortIntArrayRange() {
        int[] array = { 5, 3, 2, 1, 4 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new int[] { 5, 1, 2, 3, 4 }, array);
    }

    @Test
    public void testParallelSortLongArray() {
        CommonUtil.parallelSort((long[]) null);
        CommonUtil.parallelSort(new long[0]);

        long[] array = { 4L, 2L, 1L, 3L };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortLongArrayRange() {
        long[] array = { 5L, 3L, 2L, 1L, 4L };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new long[] { 5L, 1L, 2L, 3L, 4L }, array);
    }

    @Test
    public void testParallelSortFloatArray() {
        CommonUtil.parallelSort((float[]) null);
        CommonUtil.parallelSort(new float[0]);

        float[] array = { 4.0f, 2.0f, 1.0f, 3.0f };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortFloatArrayRange() {
        float[] array = { 5.0f, 3.0f, 2.0f, 1.0f, 4.0f };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new float[] { 5.0f, 1.0f, 2.0f, 3.0f, 4.0f }, array);
    }

    @Test
    public void testParallelSortDoubleArray() {
        CommonUtil.parallelSort((double[]) null);
        CommonUtil.parallelSort(new double[0]);

        double[] array = { 4.0, 2.0, 1.0, 3.0 };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortDoubleArrayRange() {
        double[] array = { 5.0, 3.0, 2.0, 1.0, 4.0 };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new double[] { 5.0, 1.0, 2.0, 3.0, 4.0 }, array);
    }

    @Test
    public void testParallelSortObjectArray() {
        CommonUtil.parallelSort((String[]) null);
        CommonUtil.parallelSort(new String[0]);

        String[] array = { "d", "b", "a", "c" };
        CommonUtil.parallelSort(array);
        assertArrayEquals(new String[] { "a", "b", "c", "d" }, array);
    }

    @Test
    public void testParallelSortObjectArrayRange() {
        String[] array = { "e", "d", "c", "b", "a" };
        CommonUtil.parallelSort(array, 1, 4);
        assertArrayEquals(new String[] { "e", "b", "c", "d", "a" }, array);
    }

    @Test
    public void testParallelSortObjectArrayWithComparator() {
        String[] array = { "aaa", "b", "cc" };
        CommonUtil.parallelSort(array, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "b", "cc", "aaa" }, array);
    }

    @Test
    public void testParallelSortObjectArrayRangeWithComparator() {
        String[] array = { "e", "aaa", "b", "cc", "d" };
        CommonUtil.parallelSort(array, 1, 4, Comparator.comparing(String::length));
        assertArrayEquals(new String[] { "e", "b", "cc", "aaa", "d" }, array);
    }

    @Test
    public void testParallelSortList() {
        CommonUtil.parallelSort((List<String>) null);
        CommonUtil.parallelSort(new ArrayList<String>());

        List<String> list = new ArrayList<>(Arrays.asList("d", "b", "a", "c"));
        CommonUtil.parallelSort(list);
        assertEquals(Arrays.asList("a", "b", "c", "d"), list);
    }

    @Test
    public void testParallelSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "d", "c", "b", "a"));
        CommonUtil.parallelSort(list, 1, 4);
        assertEquals(Arrays.asList("e", "b", "c", "d", "a"), list);
    }

    @Test
    public void testParallelSortListWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("aaa", "b", "cc"));
        CommonUtil.parallelSort(list, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("b", "cc", "aaa"), list);
    }

    @Test
    public void testParallelSortListRangeWithComparator() {
        List<String> list = new ArrayList<>(Arrays.asList("e", "aaa", "b", "cc", "d"));
        CommonUtil.parallelSort(list, 1, 4, Comparator.comparing(String::length));
        assertEquals(Arrays.asList("e", "b", "cc", "aaa", "d"), list);
    }

    @Test
    public void testParallelSortByArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.parallelSortBy(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Bob", people[2].getName());
    }

    @Test
    public void testParallelSortByList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.parallelSortBy(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByIntArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.parallelSortByInt(people, Person::getAge);
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByIntList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.parallelSortByInt(people, Person::getAge);
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByLongArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.parallelSortByLong(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByLongList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.parallelSortByLong(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByFloatArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.parallelSortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByFloatList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.parallelSortByFloat(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testParallelSortByDoubleArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.parallelSortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people[0].getName());
    }

    @Test
    public void testParallelSortByDoubleList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.parallelSortByDouble(people, p -> p.getAge());
        assertEquals("Jane", people.get(0).getName());
    }

    @Test
    public void testReverseSortBooleanArray() {
        CommonUtil.reverseSort((boolean[]) null);

        boolean[] array = { false, true, false, true, true };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new boolean[] { true, true, true, false, false }, array);
    }

    @Test
    public void testReverseSortBooleanArrayRange() {
        boolean[] array = { false, false, true, false, true };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new boolean[] { false, true, false, false, true }, array);
    }

    @Test
    public void testReverseSortCharArray() {
        char[] array = { 'a', 'c', 'b', 'd' };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new char[] { 'd', 'c', 'b', 'a' }, array);
    }

    @Test
    public void testReverseSortCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new char[] { 'a', 'd', 'c', 'b', 'e' }, array);
    }

    @Test
    public void testReverseSortByteArray() {
        byte[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortShortArray() {
        short[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new short[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortShortArrayRange() {
        short[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new short[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortIntArray() {
        int[] array = { 1, 3, 2, 4 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new int[] { 4, 3, 2, 1 }, array);
    }

    @Test
    public void testReverseSortIntArrayRange() {
        int[] array = { 1, 2, 3, 4, 5 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new int[] { 1, 4, 3, 2, 5 }, array);
    }

    @Test
    public void testReverseSortLongArray() {
        long[] array = { 1L, 3L, 2L, 4L };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new long[] { 4L, 3L, 2L, 1L }, array);
    }

    @Test
    public void testReverseSortLongArrayRange() {
        long[] array = { 1L, 2L, 3L, 4L, 5L };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new long[] { 1L, 4L, 3L, 2L, 5L }, array);
    }

    @Test
    public void testReverseSortFloatArray() {
        float[] array = { 1.0f, 3.0f, 2.0f, 4.0f };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, array);
    }

    @Test
    public void testReverseSortFloatArrayRange() {
        float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new float[] { 1.0f, 4.0f, 3.0f, 2.0f, 5.0f }, array);
    }

    @Test
    public void testReverseSortDoubleArray() {
        double[] array = { 1.0, 3.0, 2.0, 4.0 };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new double[] { 4.0, 3.0, 2.0, 1.0 }, array);
    }

    @Test
    public void testReverseSortDoubleArrayRange() {
        double[] array = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new double[] { 1.0, 4.0, 3.0, 2.0, 5.0 }, array);
    }

    @Test
    public void testReverseSortObjectArray() {
        String[] array = { "a", "c", "b", "d" };
        CommonUtil.reverseSort(array);
        assertArrayEquals(new String[] { "d", "c", "b", "a" }, array);
    }

    @Test
    public void testReverseSortObjectArrayRange() {
        String[] array = { "a", "b", "c", "d", "e" };
        CommonUtil.reverseSort(array, 1, 4);
        assertArrayEquals(new String[] { "a", "d", "c", "b", "e" }, array);
    }

    @Test
    public void testReverseSortList() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "c", "b", "d"));
        CommonUtil.reverseSort(list);
        assertEquals(Arrays.asList("d", "c", "b", "a"), list);
    }

    @Test
    public void testReverseSortListRange() {
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c", "d", "e"));
        CommonUtil.reverseSort(list, 1, 4);
        assertEquals(Arrays.asList("a", "d", "c", "b", "e"), list);
    }

    @Test
    public void testReverseSortByArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.reverseSortBy(people, Person::getAge);
        assertEquals("Bob", people[0].getName());
        assertEquals("John", people[1].getName());
        assertEquals("Jane", people[2].getName());
    }

    @Test
    public void testReverseSortByList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.reverseSortBy(people, Person::getAge);
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByIntArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.reverseSortByInt(people, Person::getAge);
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByIntList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.reverseSortByInt(people, Person::getAge);
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByLongArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.reverseSortByLong(people, p -> p.getAge());
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByLongList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.reverseSortByLong(people, p -> p.getAge());
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByFloatArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.reverseSortByFloat(people, p -> p.getAge());
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByFloatList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.reverseSortByFloat(people, p -> p.getAge());
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testReverseSortByDoubleArray() {
        Person[] people = { new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35) };

        CommonUtil.reverseSortByDouble(people, p -> p.getAge());
        assertEquals("Bob", people[0].getName());
    }

    @Test
    public void testReverseSortByDoubleList() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("John", 30), new Person("Jane", 25), new Person("Bob", 35)));

        CommonUtil.reverseSortByDouble(people, p -> p.getAge());
        assertEquals("Bob", people.get(0).getName());
    }

    @Test
    public void testBinarySearchCharArray() {
        assertEquals(-1, CommonUtil.binarySearch((char[]) null, 'a'));
        assertEquals(-1, CommonUtil.binarySearch(new char[0], 'a'));

        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(0, CommonUtil.binarySearch(array, 'a'));
        assertEquals(2, CommonUtil.binarySearch(array, 'c'));
        assertEquals(4, CommonUtil.binarySearch(array, 'e'));
        assertTrue(CommonUtil.binarySearch(array, 'f') < 0);
    }

    @Test
    public void testBinarySearchCharArrayRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(2, CommonUtil.binarySearch(array, 1, 4, 'c'));
        assertTrue(CommonUtil.binarySearch(array, 1, 4, 'a') < 0);
        assertTrue(CommonUtil.binarySearch(array, 1, 4, 'e') < 0);
    }

    @Test
    public void testBinarySearchByteArray() {
        assertEquals(-1, CommonUtil.binarySearch((byte[]) null, (byte) 1));
        assertEquals(-1, CommonUtil.binarySearch(new byte[0], (byte) 1));

        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, CommonUtil.binarySearch(array, (byte) 1));
        assertEquals(2, CommonUtil.binarySearch(array, (byte) 3));
        assertEquals(4, CommonUtil.binarySearch(array, (byte) 5));
        assertTrue(CommonUtil.binarySearch(array, (byte) 6) < 0);
    }

    @Test
    public void testBinarySearchByteArrayRange() {
        byte[] array = { 1, 2, 3, 4, 5 };
        assertEquals(2, CommonUtil.binarySearch(array, 1, 4, (byte) 3));
        assertTrue(CommonUtil.binarySearch(array, 1, 4, (byte) 1) < 0);
        assertTrue(CommonUtil.binarySearch(array, 1, 4, (byte) 5) < 0);
    }

    @Test
    public void testBinarySearchShortArray() {
        assertEquals(-1, CommonUtil.binarySearch((short[]) null, (short) 1));
        assertEquals(-1, CommonUtil.binarySearch(new short[0], (short) 1));

        short[] array = { 1, 2, 3, 4, 5 };
        assertEquals(0, CommonUtil.binarySearch(array, (short) 1));
        assertEquals(2, CommonUtil.binarySearch(array, (short) 3));
        assertEquals(4, CommonUtil.binarySearch(array, (short) 5));
        assertTrue(CommonUtil.binarySearch(array, (short) 6) < 0);
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
