package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.stream.ByteStream;

public class ByteListTest extends TestBase {

    private ByteList list;

    @BeforeEach
    public void setUp() {
        list = new ByteList();
    }

    @Test
    public void test_constructor_withArray() {
        byte[] arr = { 1, 2, 3 };
        ByteList list = new ByteList(arr);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_constructor_withArrayAndSize() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteList list = new ByteList(arr, 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    @DisplayName("Test constructor that wraps an existing array")
    public void testConstructorWithArray() {
        byte[] data = { 1, 2, 3 };
        ByteList list = new ByteList(data);
        assertEquals(3, list.size());
        assertArrayEquals(data, list.toArray());
        data[0] = 10;
        assertEquals((byte) 10, list.get(0), "Internal array modification should be reflected");
    }

    @Test
    @DisplayName("Test 'array' method for direct access")
    public void testArrayMethod() {
        ByteList list = new ByteList(10);
        list.add((byte) 1);
        list.add((byte) 2);

        byte[] internalArray = list.internalArray();
        assertEquals(10, internalArray.length);
        assertEquals((byte) 1, internalArray[0]);
        assertEquals((byte) 2, internalArray[1]);

        internalArray[0] = (byte) 99;
        assertEquals((byte) 99, list.get(0));
    }

    @Test
    public void test_constructor_default() {
        ByteList list = new ByteList();
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_constructor_withCapacity() {
        ByteList list = new ByteList(10);
        assertNotNull(list);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test default constructor and basic properties")
    public void testEmptyConstructor() {
        ByteList list = new ByteList();
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        ByteList list = new ByteList(10);
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test constructors and static factories")
    public void testConstructorsAndFactories() {
        ByteList list1 = new ByteList();
        assertTrue(list1.isEmpty());

        ByteList list2 = new ByteList(20);
        assertTrue(list2.isEmpty());
        assertEquals(20, list2.internalArray().length);

        byte[] data = { 1, 2, 3 };
        ByteList list3 = new ByteList(data);
        assertEquals(3, list3.size());
        data[0] = 5;
        assertEquals((byte) 5, list3.get(0));

        ByteList list4 = ByteList.of((byte) 1, (byte) 2);
        assertArrayEquals(new byte[] { 1, 2 }, list4.toArray());

        byte[] original = { 10, 20 };
        ByteList list5 = ByteList.copyOf(original);
        original[0] = 15;
        assertEquals((byte) 10, list5.get(0));
        assertArrayEquals(new byte[] { 10, 20 }, list5.toArray());

        ByteList list6 = ByteList.copyOf(new byte[] { 1, 2, 3, 4, 5 }, 1, 4);
        assertArrayEquals(new byte[] { 2, 3, 4 }, list6.toArray());
    }

    @Test
    public void testConstructorWithZeroCapacity() {
        ByteList zeroCapList = new ByteList(0);
        assertEquals(0, zeroCapList.size());

        zeroCapList.add((byte) 1);
        assertEquals(1, zeroCapList.size());
        assertEquals((byte) 1, zeroCapList.get(0));
    }

    @Test
    public void test_constructor_withCapacity_negative() {
        assertThrows(IllegalArgumentException.class, () -> new ByteList(-1));
    }

    @Test
    public void test_constructor_withArrayAndSize_invalidSize() {
        byte[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> new ByteList(arr, 5));
        assertThrows(IllegalArgumentException.class, () -> new ByteList(arr, -1));
    }

    @Test
    public void testConstructors() {
        ByteList list1 = new ByteList();
        assertEquals(0, list1.size());

        ByteList list2 = new ByteList(10);
        assertEquals(0, list2.size());

        byte[] arr = { 1, 2, 3 };
        ByteList list3 = new ByteList(arr);
        assertEquals(3, list3.size());
        assertEquals((byte) 1, list3.get(0));
        assertEquals((byte) 2, list3.get(1));
        assertEquals((byte) 3, list3.get(2));

        byte[] arr2 = { 1, 2, 3, 4, 5 };
        ByteList list4 = new ByteList(arr2, 3);
        assertEquals(3, list4.size());
        assertEquals((byte) 1, list4.get(0));
        assertEquals((byte) 2, list4.get(1));
        assertEquals((byte) 3, list4.get(2));

        assertThrows(IndexOutOfBoundsException.class, () -> new ByteList(arr2, 10));

        assertThrows(NullPointerException.class, () -> new ByteList(null));
        assertThrows(NullPointerException.class, () -> new ByteList(null, 0));
    }

    @Test
    public void test_of_varargs() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
    }

    @Test
    public void test_of_withSize() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteList list = ByteList.of(arr, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void test_delete() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte deleted = list.removeAt(1);
        assertEquals(20, deleted);
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 10, 30 }, list.toArray());
    }

    @Test
    @DisplayName("Test static factory 'of' method")
    public void testOf() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(3, list.size());
        assertEquals((byte) 10, list.get(0));
        assertEquals((byte) 20, list.get(1));
        assertEquals((byte) 30, list.get(2));
        assertEquals("[10, 20, 30]", list.toString());
    }

    @Test
    @DisplayName("Test bulk operations: addAll, removeAll, retainAll")
    public void testBulkOps() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.addAll(ByteList.of((byte) 4, (byte) 5));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, list.toArray());

        list.removeAll(ByteList.of((byte) 2, (byte) 4));
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());

        list.retainAll(ByteList.of((byte) 3, (byte) 5, (byte) 7));
        assertArrayEquals(new byte[] { 3, 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'removeIf' and 'removeAllOccurrences' methods")
    public void testConditionalRemove() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 4, (byte) 2);

        assertTrue(list.removeAllOccurrences((byte) 2));
        assertArrayEquals(new byte[] { 1, 3, 4 }, list.toArray());

        list.add((byte) -1);
        assertTrue(list.removeIf(b -> b > 2));
        assertArrayEquals(new byte[] { 1, -1 }, list.toArray());
    }

    @Test
    @DisplayName("Test search methods: indexOf, lastIndexOf, contains")
    public void testSearch() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20, (byte) 10);
        assertTrue(list.contains((byte) 30));
        assertFalse(list.contains((byte) 99));
        assertEquals(0, list.indexOf((byte) 10));
        assertEquals(4, list.lastIndexOf((byte) 10));
        assertEquals(1, list.indexOf((byte) 20));
        assertEquals(3, list.lastIndexOf((byte) 20));
        assertEquals(-1, list.indexOf((byte) 99));
    }

    @Test
    @DisplayName("Test in-place modifications: reverse, rotate, swap, fill")
    public void testModifications() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);

        list.reverse();
        assertArrayEquals(new byte[] { 4, 3, 2, 1 }, list.toArray());

        list.rotate(1);
        assertArrayEquals(new byte[] { 1, 4, 3, 2 }, list.toArray());

        list.swap(1, 3);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());

        list.fill((byte) 0);
        assertArrayEquals(new byte[] { 0, 0, 0, 0 }, list.toArray());
    }

    @Test
    @DisplayName("Test conversion methods: boxed, toIntList, stream")
    public void testConversions() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);

        List<Byte> boxed = list.boxed();
        assertEquals(List.of((byte) 10, (byte) 20), boxed);

        IntList intList = list.toIntList();
        assertArrayEquals(new int[] { 10, 20 }, intList.toArray());

        assertEquals(30, list.stream().sum());
    }

    @Test
    @DisplayName("Test deletion by index and range")
    public void testDeleteMethods() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);

        byte deleted = list.removeAt(1);
        assertEquals((byte) 2, deleted);
        assertArrayEquals(new byte[] { 1, 3, 4, 5, 6 }, list.toArray());

        list.removeRange(1, 3);
        assertArrayEquals(new byte[] { 1, 5, 6 }, list.toArray());

        list.removeAt(0, 2);
        assertArrayEquals(new byte[] { 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test moveRange and replaceRange methods")
    public void testMoveAndReplaceRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.moveRange(1, 3, 3);
        assertArrayEquals(new byte[] { 1, 4, 5, 2, 3 }, list.toArray());

        list.replaceRange(0, 2, ByteList.of((byte) 9, (byte) 8, (byte) 7));
        assertArrayEquals(new byte[] { 9, 8, 7, 5, 2, 3 }, list.toArray());
    }

    @Test
    @DisplayName("Test replaceIf and replaceAll methods")
    public void testReplaceMethods() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40);
        assertTrue(list.replaceIf(b -> b > 25, (byte) 99));
        assertArrayEquals(new byte[] { 10, 20, 99, 99 }, list.toArray());

        list.replaceAll(b -> (byte) (b / 10));
        assertArrayEquals(new byte[] { 1, 2, 9, 9 }, list.toArray());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> result = list.toList();
        assertEquals(3, result.size());
        assertEquals(Byte.valueOf((byte) 1), result.get(0));
        assertEquals(Byte.valueOf((byte) 2), result.get(1));
        assertEquals(Byte.valueOf((byte) 3), result.get(2));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToListWithRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> result = list.toList(1, 4);
        assertEquals(3, result.size());
        assertEquals(Byte.valueOf((byte) 2), result.get(0));
        assertEquals(Byte.valueOf((byte) 3), result.get(1));
        assertEquals(Byte.valueOf((byte) 4), result.get(2));
    }

    @Test
    public void testToSet() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        Set<Byte> result = list.toSet();
        assertEquals(3, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 2));
        assertTrue(result.contains((byte) 3));
    }

    @Test
    public void testToSetWithRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 4);
        Set<Byte> result = list.toSet(1, 4);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 2));
        assertTrue(result.contains((byte) 3));
    }

    @Test
    public void test_of_emptyVarargs() {
        ByteList list = ByteList.of();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void test_of_nullArray() {
        ByteList list = ByteList.of((byte[]) null);
        assertEquals(0, list.size());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void test_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = list.internalArray();
        assertNotNull(arr);
        arr[0] = 99;
        assertEquals(99, list.get(0));
    }

    @Test
    @DisplayName("Test statistical methods: min, max, median")
    public void testStats() {
        ByteList list = ByteList.of((byte) 9, (byte) 2, (byte) 7, (byte) 5, (byte) 1);
        assertEquals(OptionalByte.of((byte) 1), list.min());
        assertEquals(OptionalByte.of((byte) 9), list.max());
        assertEquals(OptionalByte.of((byte) 5), list.median());

        ByteList emptyList = new ByteList();
        assertEquals(OptionalByte.empty(), emptyList.min());
        assertEquals(OptionalByte.empty(), emptyList.max());
        assertEquals(OptionalByte.empty(), emptyList.median());
    }

    @Test
    public void testStaticFactoryMethods() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(3, list1.size());
        assertEquals((byte) 1, list1.get(0));
        assertEquals((byte) 2, list1.get(1));
        assertEquals((byte) 3, list1.get(2));

        ByteList list2 = ByteList.of((byte[]) null);
        assertEquals(0, list2.size());

        byte[] arr = { 1, 2, 3, 4 };
        ByteList list3 = ByteList.of(arr, 2);
        assertEquals(2, list3.size());
        assertEquals((byte) 1, list3.get(0));
        assertEquals((byte) 2, list3.get(1));

        ByteList list4 = ByteList.copyOf(arr);
        assertEquals(4, list4.size());
        arr[0] = 10;
        assertEquals((byte) 1, list4.get(0));

        ByteList list5 = ByteList.copyOf(arr, 1, 3);
        assertEquals(2, list5.size());
        assertEquals((byte) 2, list5.get(0));
        assertEquals((byte) 3, list5.get(1));

        ByteList list6 = ByteList.repeat((byte) 5, 4);
        assertEquals(4, list6.size());
        for (int i = 0; i < 4; i++) {
            assertEquals((byte) 5, list6.get(i));
        }

        ByteList list7 = ByteList.random(10);
        assertEquals(10, list7.size());
        for (int i = 0; i < 10; i++) {
            byte value = list7.get(i);
            assertTrue(value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE);
        }
    }

    @Test
    public void test_of_withSize_invalidSize() {
        byte[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ByteList.of(arr, 5));
    }

    @Test
    public void test_delete_indexOutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
    }

    @Test
    public void testToSetWithRange_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toSet(0, 4));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToListWithRange_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toList(0, 4));
    }

    @Test
    public void test_copyOf() {
        byte[] arr = { 1, 2, 3 };
        ByteList list = ByteList.copyOf(arr);
        assertEquals(3, list.size());
        arr[0] = 99;
        assertEquals(1, list.get(0));
    }

    @Test
    public void test_copyOf_withRange() {
        byte[] arr = { 1, 2, 3, 4, 5 };
        ByteList list = ByteList.copyOf(arr, 1, 4);
        assertEquals(3, list.size());
        assertEquals(2, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(4, list.get(2));
    }

    @Test
    @DisplayName("Test static factory 'copyOf' method")
    public void testCopyOf() {
        byte[] original = { 1, 2, 3 };
        ByteList list = ByteList.copyOf(original);
        assertEquals(3, list.size());
        assertArrayEquals(original, list.toArray());
        original[0] = 10;
        assertEquals((byte) 1, list.get(0));
    }

    @Test
    public void test_copyOf_null() {
        ByteList list = ByteList.copyOf(null);
        assertEquals(0, list.size());
    }

    @Test
    public void test_range() {
        ByteList list = ByteList.range((byte) 1, (byte) 5);
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void test_range_withStep() {
        ByteList list = ByteList.range((byte) 0, (byte) 10, (byte) 2);
        assertEquals(5, list.size());
        assertArrayEquals(new byte[] { 0, 2, 4, 6, 8 }, list.toArray());
    }

    @Test
    @DisplayName("Test static factory 'range' and 'rangeClosed' methods")
    public void testRange() {
        ByteList rangeList = ByteList.range((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, rangeList.toArray());

        ByteList rangeClosedList = ByteList.rangeClosed((byte) 1, (byte) 5);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, rangeClosedList.toArray());

        ByteList rangeStepList = ByteList.range((byte) 0, (byte) 10, (byte) 2);
        assertArrayEquals(new byte[] { 0, 2, 4, 6, 8 }, rangeStepList.toArray());
    }

    @Test
    @DisplayName("Test range, repeat, and random factories")
    public void testRangeRepeatRandom() {
        assertArrayEquals(new byte[] { 5, 6, 7 }, ByteList.range((byte) 5, (byte) 8).toArray());
        assertArrayEquals(new byte[] { 5, 6, 7, 8 }, ByteList.rangeClosed((byte) 5, (byte) 8).toArray());
        assertArrayEquals(new byte[] { 0, 3, 6 }, ByteList.rangeClosed((byte) 0, (byte) 8, (byte) 3).toArray());
        assertArrayEquals(new byte[] { 7, 7, 7 }, ByteList.repeat((byte) 7, 3).toArray());
        assertEquals(10, ByteList.random(10).size());
    }

    @Test
    public void testRangeFactoryMethods() {
        ByteList list1 = ByteList.range((byte) 0, (byte) 5);
        assertEquals(5, list1.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((byte) i, list1.get(i));
        }

        ByteList list2 = ByteList.range((byte) 0, (byte) 10, (byte) 2);
        assertEquals(5, list2.size());
        assertEquals((byte) 0, list2.get(0));
        assertEquals((byte) 2, list2.get(1));
        assertEquals((byte) 4, list2.get(2));
        assertEquals((byte) 6, list2.get(3));
        assertEquals((byte) 8, list2.get(4));

        ByteList list3 = ByteList.rangeClosed((byte) 1, (byte) 5);
        assertEquals(5, list3.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((byte) (i + 1), list3.get(i));
        }

        ByteList list4 = ByteList.rangeClosed((byte) 1, (byte) 9, (byte) 2);
        assertEquals(5, list4.size());
        assertEquals((byte) 1, list4.get(0));
        assertEquals((byte) 3, list4.get(1));
        assertEquals((byte) 5, list4.get(2));
        assertEquals((byte) 7, list4.get(3));
        assertEquals((byte) 9, list4.get(4));

        ByteList list5 = ByteList.range((byte) -5, (byte) 0);
        assertEquals(5, list5.size());
        for (int i = 0; i < 5; i++) {
            assertEquals((byte) (-5 + i), list5.get(i));
        }
    }

    @Test
    public void test_range_negativeStep() {
        ByteList list = ByteList.range((byte) 10, (byte) 0, (byte) -2);
        assertEquals(5, list.size());
        assertArrayEquals(new byte[] { 10, 8, 6, 4, 2 }, list.toArray());
    }

    @Test
    public void testRangeWithExtremeValues() {
        ByteList list1 = ByteList.range((byte) 120, (byte) 127);
        assertEquals(7, list1.size());
        assertEquals((byte) 120, list1.get(0));
        assertEquals((byte) 126, list1.get(6));

        ByteList list2 = ByteList.range((byte) -5, (byte) 5);
        assertEquals(10, list2.size());
        assertEquals((byte) -5, list2.get(0));
        assertEquals((byte) 4, list2.get(9));

        ByteList list3 = ByteList.range((byte) -128, (byte) 127, (byte) 25);
        assertTrue(list3.size() > 0);
        assertEquals((byte) -128, list3.get(0));

        ByteList list4 = ByteList.rangeClosed(Byte.MIN_VALUE, Byte.MIN_VALUE);
        assertEquals(1, list4.size());
        assertEquals(Byte.MIN_VALUE, list4.get(0));

        ByteList list5 = ByteList.rangeClosed(Byte.MAX_VALUE, Byte.MAX_VALUE);
        assertEquals(1, list5.size());
        assertEquals(Byte.MAX_VALUE, list5.get(0));
    }

    @Test
    public void testRangeWithNegativeStep() {
        ByteList list1 = ByteList.range((byte) 10, (byte) 0, (byte) -1);
        assertEquals(10, list1.size());
        assertEquals((byte) 10, list1.get(0));
        assertEquals((byte) 1, list1.get(9));

        ByteList list2 = ByteList.rangeClosed((byte) 10, (byte) 0, (byte) -2);
        assertEquals(6, list2.size());
        assertEquals((byte) 10, list2.get(0));
        assertEquals((byte) 8, list2.get(1));
        assertEquals((byte) 0, list2.get(5));
    }

    @Test
    public void test_rangeClosed() {
        ByteList list = ByteList.rangeClosed((byte) 1, (byte) 5);
        assertEquals(5, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, list.toArray());
    }

    @Test
    public void test_rangeClosed_withStep() {
        ByteList list = ByteList.rangeClosed((byte) 0, (byte) 10, (byte) 2);
        assertEquals(6, list.size());
        assertArrayEquals(new byte[] { 0, 2, 4, 6, 8, 10 }, list.toArray());
    }

    @Test
    public void test_repeat() {
        ByteList list = ByteList.repeat((byte) 5, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 5, 5, 5 }, list.toArray());
    }

    @Test
    public void test_repeat_zeroLength() {
        ByteList list = ByteList.repeat((byte) 5, 0);
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test static factory 'repeat' and 'random' methods")
    public void testRepeatAndRandom() {
        ByteList repeatedList = ByteList.repeat((byte) 7, 4);
        assertArrayEquals(new byte[] { 7, 7, 7, 7 }, repeatedList.toArray());

        ByteList randomList = ByteList.random(10);
        assertEquals(10, randomList.size());
        assertNotNull(randomList);
    }

    @Test
    public void test_random() {
        ByteList list = ByteList.random(10);
        assertEquals(10, list.size());
        for (int i = 0; i < list.size(); i++) {
            byte val = list.get(i);
            assertTrue(val >= Byte.MIN_VALUE && val <= Byte.MAX_VALUE);
        }
    }

    @Test
    public void testArray() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        byte[] array = list.internalArray();
        assertEquals((byte) 1, array[0]);
        assertEquals((byte) 2, array[1]);
        assertEquals((byte) 3, array[2]);

        array[0] = 10;
        assertEquals((byte) 10, list.get(0));
    }

    @Test
    public void test_get() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(10, list.get(0));
        assertEquals(20, list.get(1));
        assertEquals(30, list.get(2));
    }

    @Test
    public void test_get_indexOutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @Test
    public void test_set() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte oldValue = list.set(1, (byte) 25);
        assertEquals(20, oldValue);
        assertEquals(25, list.get(1));
    }

    @Test
    @DisplayName("Test set-like operations: intersection, difference, symmetricDifference")
    public void testSetLikeOps() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 4);

        ByteList intersection = list1.intersection(list2);
        intersection.sort();
        assertArrayEquals(new byte[] { 2, 3 }, intersection.toArray());

        ByteList difference = list1.difference(list2);
        assertArrayEquals(new byte[] { 1, 2 }, difference.toArray());

        ByteList symmDiff = list1.symmetricDifference(list2);
        symmDiff.sort();
        assertArrayEquals(new byte[] { 1, 2, 4, 4 }, symmDiff.toArray());
    }

    @Test
    @DisplayName("Test set-like operations with primitive array inputs")
    public void testSetLikeOpsWithArrays() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 2);
        byte[] arr = { 2, 3, 3 };

        ByteList intersection = list.intersection(arr);
        assertArrayEquals(new byte[] { 2 }, intersection.toArray());

        ByteList difference = list.difference(arr);
        assertArrayEquals(new byte[] { 1, 2 }, difference.toArray());

        ByteList symmetricDifference = list.symmetricDifference(arr);
        symmetricDifference.sort();
        assertArrayEquals(new byte[] { 1, 2, 3, 3 }, symmetricDifference.toArray());
    }

    @Test
    public void test_set_indexOutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, (byte) 0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, (byte) 0));
    }

    @Test
    public void test_add() {
        ByteList list = new ByteList();
        list.add((byte) 1);
        list.add((byte) 2);
        assertEquals(2, list.size());
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    public void test_add_withIndex() {
        ByteList list = ByteList.of((byte) 1, (byte) 3);
        list.add(1, (byte) 2);
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, list.toArray());
    }

    @Test
    public void test_add_withIndex_atEnd() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        list.add(2, (byte) 3);
        assertEquals(3, list.size());
        assertEquals(3, list.get(2));
    }

    @Test
    @DisplayName("Test adding, getting, and setting elements")
    public void testAddGetSet() {
        ByteList list = new ByteList();
        list.add((byte) 5);
        list.add(0, (byte) 1);
        list.add((byte) 10);

        assertEquals(3, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 10, list.get(2));

        byte oldValue = list.set(1, (byte) 7);
        assertEquals((byte) 5, oldValue);
        assertEquals((byte) 7, list.get(1));
        assertArrayEquals(new byte[] { 1, 7, 10 }, list.toArray());
    }

    @Test
    @DisplayName("Test add, addFirst, addLast, and addAll methods")
    public void testAddMethods() {
        ByteList list = ByteList.of((byte) 10);
        list.add((byte) 20);
        list.add(1, (byte) 15);
        list.addFirst((byte) 5);
        list.addLast((byte) 25);
        assertArrayEquals(new byte[] { 5, 10, 15, 20, 25 }, list.toArray());

        list.addAll(new byte[] { 30, 35 });
        assertArrayEquals(new byte[] { 5, 10, 15, 20, 25, 30, 35 }, list.toArray());

        list.addAll(0, ByteList.of((byte) 1, (byte) 2));
        assertArrayEquals(new byte[] { 1, 2, 5, 10, 15, 20, 25, 30, 35 }, list.toArray());
    }

    @Test
    public void testAdd_AtIndex_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 3, (byte) 5);
        bl.add(1, (byte) 2);
        assertEquals(4, bl.size());
        assertEquals((byte) 2, bl.get(1));
        assertEquals((byte) 3, bl.get(2));
    }

    @Test
    public void testAdd_AtIndex_Beginning() {
        ByteList bl = ByteList.of((byte) 2, (byte) 3);
        bl.add(0, (byte) 1);
        assertEquals((byte) 1, bl.get(0));
        assertEquals(3, bl.size());
    }

    @Test
    public void test_add_withIndex_outOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(-1, (byte) 0));
        assertThrows(IndexOutOfBoundsException.class, () -> list.add(3, (byte) 0));
    }

    @Test
    public void test_addAll_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2);
        ByteList list2 = ByteList.of((byte) 3, (byte) 4);
        assertTrue(list1.addAll(list2));
        assertEquals(4, list1.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list1.toArray());
    }

    @Test
    public void test_addAll_byteList_withIndex() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 4);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3);
        assertTrue(list1.addAll(1, list2));
        assertEquals(4, list1.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list1.toArray());
    }

    @Test
    public void test_addAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        byte[] arr = { 3, 4 };
        assertTrue(list.addAll(arr));
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void test_addAll_array_withIndex() {
        ByteList list = ByteList.of((byte) 1, (byte) 4);
        byte[] arr = { 2, 3 };
        assertTrue(list.addAll(1, arr));
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAll_AtIndex_Array() {
        ByteList bl = ByteList.of((byte) 1, (byte) 4, (byte) 5);
        boolean changed = bl.addAll(1, new byte[] { (byte) 2, (byte) 3 });
        assertTrue(changed);
        assertEquals(5, bl.size());
        assertEquals((byte) 1, bl.get(0));
        assertEquals((byte) 2, bl.get(1));
        assertEquals((byte) 3, bl.get(2));
        assertEquals((byte) 4, bl.get(3));
    }

    @Test
    public void testAddAll_ByteList_WithElements() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2);
        boolean changed = bl.addAll(ByteList.of((byte) 3, (byte) 4));
        assertTrue(changed);
        assertEquals(4, bl.size());
        assertEquals((byte) 3, bl.get(2));
        assertEquals((byte) 4, bl.get(3));
    }

    @Test
    public void test_addAll_byteList_empty() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2);
        ByteList list2 = new ByteList();
        assertFalse(list1.addAll(list2));
        assertEquals(2, list1.size());
    }

    @Test
    public void test_addAll_array_null() {
        ByteList list = ByteList.of((byte) 1, (byte) 2);
        assertFalse(list.addAll((byte[]) null));
        assertEquals(2, list.size());
    }

    @Test
    public void testAddAll_AtIndex_Array_Empty() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2);
        boolean changed = bl.addAll(1, new byte[] {});
        assertFalse(changed);
        assertEquals(2, bl.size());
    }

    @Test
    public void test_remove() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        assertTrue(list.remove((byte) 2));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 2 }, list.toArray());
    }

    @Test
    public void test_remove_notFound() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list.remove((byte) 5));
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test remove, removeAll, and retainAll methods")
    public void testRemoveMethods() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 3);

        assertTrue(list.remove((byte) 3));
        assertArrayEquals(new byte[] { 1, 2, 4, 5, 3 }, list.toArray());

        assertTrue(list.removeAll(ByteList.of((byte) 1, (byte) 5, (byte) 9)));
        assertArrayEquals(new byte[] { 2, 4, 3 }, list.toArray());

        assertTrue(list.retainAll(new byte[] { 4, 3, 8 }));
        assertArrayEquals(new byte[] { 4, 3 }, list.toArray());
    }

    @Test
    public void test_removeAllOccurrences() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 4, (byte) 2);
        assertTrue(list.removeAllOccurrences((byte) 2));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 4 }, list.toArray());
    }

    @Test
    public void test_removeAllOccurrences_notFound() {
        ByteList list = ByteList.of((byte) 1, (byte) 3, (byte) 4);
        assertFalse(list.removeAllOccurrences((byte) 2));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAllOccurrences_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2);
        boolean changed = bl.removeAllOccurrences((byte) 2);
        assertTrue(changed);
        assertEquals(2, bl.size());
        assertFalse(bl.contains((byte) 2));
    }

    @Test
    public void testRemoveAllOccurrences_NotPresent() {
        ByteList bl = ByteList.of((byte) 1, (byte) 3, (byte) 5);
        boolean changed = bl.removeAllOccurrences((byte) 7);
        assertFalse(changed);
        assertEquals(3, bl.size());
    }

    @Test
    public void testFastRemove_ViaRemoveMethod() {
        // fastRemove is private; tested indirectly via remove(byte)
        ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40);
        boolean changed = bl.remove((byte) 20);
        assertTrue(changed);
        assertEquals(3, bl.size());
        assertEquals((byte) 10, bl.get(0));
        assertEquals((byte) 30, bl.get(1));
        assertEquals((byte) 40, bl.get(2));
    }

    @Test
    public void test_removeAll_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList toRemove = ByteList.of((byte) 2, (byte) 4);
        assertTrue(list.removeAll(toRemove));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void test_removeAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] toRemove = { 2, 4 };
        assertTrue(list.removeAll(toRemove));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void test_removeIf() {
        ByteList list = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
        assertTrue(list.removeIf(b -> b < 0));
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveIf_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
        boolean changed = bl.removeIf(b -> b < 0);
        assertTrue(changed);
        assertEquals(3, bl.size());
        assertFalse(bl.contains((byte) -2));
        assertFalse(bl.contains((byte) -4));
    }

    @Test
    public void test_removeIf_noneMatch() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list.removeIf(b -> b < 0));
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveIf_NoneMatch() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        boolean changed = bl.removeIf(b -> b < 0);
        assertFalse(changed);
        assertEquals(3, bl.size());
    }

    @Test
    public void test_removeDuplicates() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 2, (byte) 4);
        assertTrue(list.removeDuplicates());
        assertEquals(4, list.size());
        assertTrue(list.contains((byte) 1));
        assertTrue(list.contains((byte) 2));
        assertTrue(list.contains((byte) 3));
        assertTrue(list.contains((byte) 4));
    }

    @Test
    public void test_removeDuplicates_noDuplicates() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list.removeDuplicates());
        assertEquals(3, list.size());
    }

    @Test
    @DisplayName("Test 'removeDuplicates' method")
    public void testRemoveDuplicates() {
        ByteList list = ByteList.of((byte) 1, (byte) 3, (byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.removeDuplicates());
        assertArrayEquals(new byte[] { 1, 3, 2 }, list.toArray());

        ByteList sortedList = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        assertTrue(sortedList.removeDuplicates());
        assertArrayEquals(new byte[] { 1, 2, 3 }, sortedList.toArray());
    }

    // ---- newly added tests for uncovered methods ----

    @Test
    public void testRemoveDuplicates_Unsorted() {
        ByteList bl = ByteList.of((byte) 3, (byte) 1, (byte) 2, (byte) 1, (byte) 3);
        boolean changed = bl.removeDuplicates();
        assertTrue(changed);
        assertEquals(3, bl.size());
        assertFalse(bl.containsDuplicates());
    }

    @Test
    public void testRemoveDuplicates_Sorted() {
        ByteList bl = ByteList.of((byte) 1, (byte) 1, (byte) 2, (byte) 3, (byte) 3);
        boolean changed = bl.removeDuplicates();
        assertTrue(changed);
        assertEquals(3, bl.size());
        assertEquals((byte) 1, bl.get(0));
        assertEquals((byte) 2, bl.get(1));
        assertEquals((byte) 3, bl.get(2));
    }

    @Test
    public void testRemoveDuplicates_NoDuplicates() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        boolean changed = bl.removeDuplicates();
        assertFalse(changed);
        assertEquals(3, bl.size());
    }

    @Test
    public void testRemoveDuplicatesEmpty() {
        assertFalse(list.removeDuplicates());
    }

    @Test
    public void test_retainAll_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList toRetain = ByteList.of((byte) 2, (byte) 4, (byte) 6);
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 2, 4 }, list.toArray());
    }

    @Test
    public void test_retainAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] toRetain = { 2, 4, 6 };
        assertTrue(list.retainAll(toRetain));
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 2, 4 }, list.toArray());
    }

    @Test
    public void testRetainAll_ByteList_small() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        bl.retainAll(ByteList.of((byte) 2, (byte) 4));
        assertEquals(2, bl.size());
        assertEquals((byte) 2, bl.get(0));
        assertEquals((byte) 4, bl.get(1));
    }

    @Test
    public void testRetainAll_array() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        bl.retainAll(new byte[] { (byte) 1, (byte) 3 });
        assertEquals(2, bl.size());
        assertEquals((byte) 1, bl.get(0));
        assertEquals((byte) 3, bl.get(1));
    }

    @Test
    public void testBatchRemove_viaRetainAll_largeList() {
        // triggers the Set path (c.size() > 3 && size() > 9)
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10);
        ByteList retain = ByteList.of((byte) 1, (byte) 3, (byte) 5, (byte) 7);
        bl.retainAll(retain);
        assertEquals(4, bl.size());
        assertEquals((byte) 1, bl.get(0));
        assertEquals((byte) 3, bl.get(1));
    }

    @Test
    public void testBatchRemove_removeAll_smallCollection() {
        // batchRemove(c, false) with small collection (c.size() <= 3 or size() <= 9)
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        boolean changed = bl.removeAll(ByteList.of((byte) 2, (byte) 4));
        assertTrue(changed);
        assertEquals(3, bl.size());
        assertFalse(bl.contains((byte) 2));
        assertFalse(bl.contains((byte) 4));
        assertTrue(bl.contains((byte) 1));
    }

    @Test
    public void testBatchRemove_retainAll_largeCollection() {
        // batchRemove(c, true) with large collection (c.size() > 3 and size() > 9)
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 10);
        ByteList toRetain = ByteList.of((byte) 2, (byte) 4, (byte) 6, (byte) 8);
        boolean changed = bl.retainAll(toRetain);
        assertTrue(changed);
        assertEquals(4, bl.size());
        assertTrue(bl.contains((byte) 2));
        assertFalse(bl.contains((byte) 1));
    }

    @Test
    public void test_removeAt() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50);
        list.removeAt(1, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new byte[] { 10, 30, 50 }, list.toArray());
    }

    @Test
    public void test_removeAt_empty() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        list.removeAt();
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAtSingleIndex() {
        list.addAll(new byte[] { 10, 20, 30 });
        byte removed = list.removeAt(1);
        assertEquals((byte) 20, removed);
        assertEquals(2, list.size());
    }

    @Test
    public void testRemoveAtMultipleIndices() {
        list.addAll(new byte[] { 10, 20, 30, 40, 50 });
        list.removeAt(1, 3);
        assertEquals(3, list.size());
    }

    @Test
    public void testRemoveAt_MultipleIndices() {
        ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50);
        bl.removeAt(1, 3);
        assertEquals(3, bl.size());
        assertEquals((byte) 10, bl.get(0));
        assertEquals((byte) 30, bl.get(1));
        assertEquals((byte) 50, bl.get(2));
    }

    @Test
    public void testRemoveAt_SingleIndex() {
        ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        bl.removeAt(1);
        assertEquals(2, bl.size());
        assertEquals((byte) 10, bl.get(0));
        assertEquals((byte) 30, bl.get(1));
    }

    @Test
    public void test_removeRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 1, 5 }, list.toArray());
    }

    @Test
    public void testRemoveRange() {
        list.addAll(new byte[] { 1, 2, 3, 4, 5 });
        list.removeRange(1, 4);
        assertEquals(2, list.size());
        assertArrayEquals(new byte[] { 1, 5 }, list.toArray());
    }

    @Test
    public void testRemoveRange_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        bl.removeRange(1, 4);
        assertEquals(2, bl.size());
        assertEquals((byte) 1, bl.get(0));
        assertEquals((byte) 5, bl.get(1));
    }

    @Test
    public void testRemoveRange_EmptyRange_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        bl.removeRange(1, 1);
        assertEquals(3, bl.size());
    }

    @Test
    public void testMoveRange() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);
        list.add((byte) 5);

        list.moveRange(1, 3, 0);
        assertEquals(5, list.size());
        assertEquals((byte) 2, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 1, list.get(2));
        assertEquals((byte) 4, list.get(3));
        assertEquals((byte) 5, list.get(4));
    }

    @Test
    public void test_moveRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.moveRange(1, 3, 2);
        assertNotNull(list);
    }

    @Test
    public void test_replaceRange_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList replacement = ByteList.of((byte) 10, (byte) 11);
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
    }

    @Test
    public void test_replaceRange_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        byte[] replacement = { 10, 11 };
        list.replaceRange(1, 4, replacement);
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
    }

    @Test
    public void testReplaceRange() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);

        ByteList replacement = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        list.replaceRange(1, 3, replacement);
        assertEquals(5, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 20, list.get(2));
        assertEquals((byte) 30, list.get(3));
        assertEquals((byte) 4, list.get(4));

        list.clear();
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.replaceRange(1, 2, new byte[] { 100, 101 });
        assertEquals(4, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 100, list.get(1));
        assertEquals((byte) 101, list.get(2));
        assertEquals((byte) 3, list.get(3));

        list.replaceRange(1, 3, new byte[0]);
        assertEquals(2, list.size());
    }

    @Test
    public void testReplaceRangeByteList() {
        list.addAll(new byte[] { 1, 2, 3, 4, 5 });
        list.replaceRange(1, 4, ByteList.of((byte) 10, (byte) 11));
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
    }

    @Test
    public void testReplaceRangeArray() {
        list.addAll(new byte[] { 1, 2, 3, 4, 5 });
        list.replaceRange(1, 4, new byte[] { 10, 11 });
        assertEquals(4, list.size());
        assertArrayEquals(new byte[] { 1, 10, 11, 5 }, list.toArray());
    }

    @Test
    public void testReplaceRange_ByteList() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        bl.replaceRange(1, 3, ByteList.of((byte) 20, (byte) 30, (byte) 40));
        assertEquals(6, bl.size());
        assertEquals((byte) 20, bl.get(1));
    }

    @Test
    public void test_replaceAll_values() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
        int count = list.replaceAll((byte) 1, (byte) 9);
        assertEquals(3, count);
        assertArrayEquals(new byte[] { 9, 2, 9, 3, 9 }, list.toArray());
    }

    @Test
    public void test_replaceAll_operator() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.replaceAll(b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 2, 4, 6 }, list.toArray());
    }

    @Test
    @DisplayName("Test 'replaceAll' methods")
    public void testReplaceAll() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        int count = list.replaceAll((byte) 1, (byte) 5);
        assertEquals(2, count);
        assertArrayEquals(new byte[] { 5, 2, 5, 3 }, list.toArray());

        list.replaceAll(b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 10, 4, 10, 6 }, list.toArray());
    }

    @Test
    public void testReplaceAllOperator() {
        list.addAll(new byte[] { 1, 2, 3 });
        list.replaceAll(b -> (byte) (b * 2));
        assertArrayEquals(new byte[] { 2, 4, 6 }, list.toArray());
    }

    @Test
    public void test_replaceIf() {
        ByteList list = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4, (byte) 5);
        assertTrue(list.replaceIf(b -> b < 0, (byte) 0));
        assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, list.toArray());
    }

    @Test
    public void testReplaceIf() {
        list.addAll(new byte[] { 1, -2, 3, -4, 5 });
        assertTrue(list.replaceIf(b -> b < 0, (byte) 0));
        assertArrayEquals(new byte[] { 1, 0, 3, 0, 5 }, list.toArray());
    }

    @Test
    public void testReplaceIf_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) -2, (byte) 3, (byte) -4);
        boolean changed = bl.replaceIf(b -> b < 0, (byte) 0);
        assertTrue(changed);
        assertEquals((byte) 0, bl.get(1));
        assertEquals((byte) 0, bl.get(3));
        assertEquals((byte) 1, bl.get(0));
    }

    @Test
    public void testReplaceIfNoneMatch() {
        list.addAll(new byte[] { 1, 2, 3 });
        assertFalse(list.replaceIf(b -> b > 100, (byte) 0));
    }

    @Test
    public void testReplaceIf_NoneMatch_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        boolean changed = bl.replaceIf(b -> b < 0, (byte) 0);
        assertFalse(changed);
    }

    @Test
    public void test_fill() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.fill((byte) 9);
        assertArrayEquals(new byte[] { 9, 9, 9 }, list.toArray());
    }

    @Test
    public void test_fill_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.fill(1, 4, (byte) 9);
        assertArrayEquals(new byte[] { 1, 9, 9, 9, 5 }, list.toArray());
    }

    @Test
    public void testFill() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        list.fill((byte) 5);
        assertEquals(3, list.size());
        for (int i = 0; i < 3; i++) {
            assertEquals((byte) 5, list.get(i));
        }

        list.fill(1, 3, (byte) 10);
        assertEquals((byte) 5, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 10, list.get(2));
    }

    @Test
    public void testFillRange() {
        list.addAll(new byte[] { 1, 2, 3, 4, 5 });
        list.fill(1, 4, (byte) 9);
        assertArrayEquals(new byte[] { 1, 9, 9, 9, 5 }, list.toArray());
    }

    @Test
    public void test_contains() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.contains((byte) 2));
        assertFalse(list.contains((byte) 5));
    }

    @Test
    @DisplayName("Test 'contains', 'containsAny', 'containsAll', and 'disjoint' methods")
    public void testContainsAndDisjoint() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertTrue(list.contains((byte) 10));
        assertTrue(list.containsAny(new byte[] { 5, 15, 20 }));
        assertFalse(list.containsAny(new byte[] { 5, 15, 25 }));
        assertTrue(list.containsAll(ByteList.of((byte) 10, (byte) 30)));
        assertFalse(list.containsAll(ByteList.of((byte) 10, (byte) 40)));

        assertTrue(list.disjoint(ByteList.of((byte) 1, (byte) 2, (byte) 3)));
        assertFalse(list.disjoint(new byte[] { 15, 25, 30 }));
    }

    @Test
    public void testContains() {
        list.add((byte) 10);
        list.add((byte) 20);

        assertTrue(list.contains((byte) 10));
        assertTrue(list.contains((byte) 20));
        assertFalse(list.contains((byte) 30));

        list.clear();
        assertFalse(list.contains((byte) 10));
    }

    @Test
    public void test_containsAny_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList other = ByteList.of((byte) 3, (byte) 4, (byte) 5);
        assertTrue(list.containsAny(other));
    }

    @Test
    public void test_containsAny_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] other = { 3, 4, 5 };
        assertTrue(list.containsAny(other));
    }

    @Test
    public void testContainsAnyAndAll() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        assertTrue(list.containsAny(ByteList.of((byte) 1)));
        assertTrue(list.containsAny(ByteList.of((byte) 4, (byte) 3)));
        assertFalse(list.containsAny(ByteList.of((byte) 4, (byte) 5)));
        assertFalse(list.containsAny(new ByteList()));

        assertTrue(list.containsAny(new byte[] { 1 }));
        assertTrue(list.containsAny(new byte[] { 4, 2 }));
        assertFalse(list.containsAny(new byte[] { 4, 5 }));

        assertTrue(list.containsAll(ByteList.of((byte) 1, (byte) 2)));
        assertTrue(list.containsAll(ByteList.of((byte) 3)));
        assertFalse(list.containsAll(ByteList.of((byte) 1, (byte) 4)));
        assertTrue(list.containsAll(new ByteList()));

        assertTrue(list.containsAll(new byte[] { 1, 2 }));
        assertFalse(list.containsAll(new byte[] { 1, 4 }));
        assertTrue(list.containsAll(new byte[0]));

        list.clear();
        assertFalse(list.containsAny(ByteList.of((byte) 1)));
        assertFalse(list.containsAll(ByteList.of((byte) 1)));
        assertTrue(list.containsAll(new ByteList()));
    }

    @Test
    public void testContainsAnyByteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.containsAny(ByteList.of((byte) 3, (byte) 4)));
        assertFalse(list.containsAny(ByteList.of((byte) 4, (byte) 5)));
    }

    @Test
    public void testContainsAnyArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.containsAny(new byte[] { 3, 4 }));
        assertFalse(list.containsAny(new byte[] { 4, 5 }));
    }

    @Test
    public void test_containsAll_byteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteList other = ByteList.of((byte) 2, (byte) 3);
        assertTrue(list.containsAll(other));
    }

    @Test
    public void test_containsAll_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] other = { 2, 3 };
        assertTrue(list.containsAll(other));
    }

    @Test
    public void testContainsAllByteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.containsAll(ByteList.of((byte) 1, (byte) 2)));
        assertFalse(list.containsAll(ByteList.of((byte) 1, (byte) 4)));
    }

    @Test
    public void testContainsAllArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.containsAll(new byte[] { 1, 2 }));
        assertFalse(list.containsAll(new byte[] { 1, 4 }));
    }

    @Test
    public void testContainsAll_ByteList() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertTrue(bl.containsAll(ByteList.of((byte) 1, (byte) 3)));
        assertFalse(bl.containsAll(ByteList.of((byte) 1, (byte) 5)));
    }

    @Test
    public void test_disjoint_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 4, (byte) 5);
        assertTrue(list1.disjoint(list2));

        ByteList list3 = ByteList.of((byte) 3, (byte) 4);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void test_disjoint_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr1 = { 4, 5 };
        assertTrue(list.disjoint(arr1));

        byte[] arr2 = { 3, 4 };
        assertFalse(list.disjoint(arr2));
    }

    @Test
    public void testDisjoint() {
        list.add((byte) 1);
        list.add((byte) 2);

        assertFalse(list.disjoint(ByteList.of((byte) 1)));
        assertFalse(list.disjoint(ByteList.of((byte) 2, (byte) 3)));
        assertTrue(list.disjoint(ByteList.of((byte) 3, (byte) 4)));
        assertTrue(list.disjoint(new ByteList()));

        assertFalse(list.disjoint(new byte[] { 1 }));
        assertTrue(list.disjoint(new byte[] { 3, 4 }));
        assertTrue(list.disjoint(new byte[0]));

        list.clear();
        assertTrue(list.disjoint(ByteList.of((byte) 1)));
        assertTrue(list.disjoint(new byte[] { 2 }));
    }

    @Test
    public void testDisjointByteList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.disjoint(ByteList.of((byte) 4, (byte) 5)));
        assertFalse(list.disjoint(ByteList.of((byte) 3, (byte) 4)));
    }

    @Test
    public void testDisjointArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list.disjoint(new byte[] { 4, 5 }));
        assertFalse(list.disjoint(new byte[] { 3, 4 }));
    }

    @Test
    public void testDisjoint_ByteList() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(bl.disjoint(ByteList.of((byte) 4, (byte) 5)));
        assertFalse(bl.disjoint(ByteList.of((byte) 3, (byte) 6)));
    }

    @Test
    public void test_intersection_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteList list2 = ByteList.of((byte) 3, (byte) 4, (byte) 5);
        ByteList result = list1.intersection(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 3));
        assertTrue(result.contains((byte) 4));
    }

    @Test
    public void test_intersection_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] arr = { 3, 4, 5 };
        ByteList result = list.intersection(arr);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 3));
        assertTrue(result.contains((byte) 4));
    }

    @Test
    public void testIntersection() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 2);

        ByteList other = ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 2);
        ByteList result = list.intersection(other);
        assertEquals(3, result.size());
        assertEquals((byte) 2, result.get(0));
        assertEquals((byte) 3, result.get(1));
        assertEquals((byte) 2, result.get(2));

        result = list.intersection(new byte[] { 1, 1, 4 });
        assertEquals(1, result.size());
        assertEquals((byte) 1, result.get(0));

        result = list.intersection(new ByteList());
        assertEquals(0, result.size());
    }

    @Test
    public void testIntersectionByteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4);
        ByteList result = list1.intersection(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 2));
        assertTrue(result.contains((byte) 3));
    }

    @Test
    public void testIntersectionArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList result = list.intersection(new byte[] { 2, 3, 4 });
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 2));
    }

    @Test
    public void testIntersection_ByteList() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3);
        ByteList result = bl.intersection(ByteList.of((byte) 2, (byte) 2, (byte) 4));
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 2));
    }

    @Test
    public void testIntersection_ByteList_NoCommon() {
        ByteList bl = ByteList.of((byte) 1, (byte) 3, (byte) 5);
        ByteList result = bl.intersection(ByteList.of((byte) 2, (byte) 4));
        assertEquals(0, result.size());
    }

    @Test
    public void test_difference_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        ByteList list2 = ByteList.of((byte) 3, (byte) 4, (byte) 5);
        ByteList result = list1.difference(list2);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 2));
    }

    @Test
    public void test_difference_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        byte[] arr = { 3, 4, 5 };
        ByteList result = list.difference(arr);
        assertEquals(2, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 2));
    }

    @Test
    public void testDifference() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 2);

        ByteList other = ByteList.of((byte) 2, (byte) 2);
        ByteList result = list.difference(other);
        assertEquals(2, result.size());
        assertEquals((byte) 1, result.get(0));
        assertEquals((byte) 3, result.get(1));

        result = list.difference(new byte[] { 1, 3 });
        assertEquals(2, result.size());
        assertEquals((byte) 2, result.get(0));
        assertEquals((byte) 2, result.get(1));

        result = list.difference(new ByteList());
        assertEquals(4, result.size());
    }

    @Test
    public void testDifferenceByteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4);
        ByteList result = list1.difference(list2);
        assertEquals(1, result.size());
        assertTrue(result.contains((byte) 1));
    }

    @Test
    public void testDifferenceArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList result = list.difference(new byte[] { 2, 3, 4 });
        assertEquals(1, result.size());
        assertTrue(result.contains((byte) 1));
    }

    @Test
    public void testDifference_array() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        ByteList result = bl.difference(new byte[] { (byte) 2 });
        assertEquals(3, result.size());
    }

    @Test
    public void testDifference_ByteList() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2);
        ByteList result = bl.difference(ByteList.of((byte) 2));
        // removes one occurrence of 2, leaving [1, 3, 2]
        assertEquals(3, result.size());
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 3));
    }

    @Test
    public void testDifference_ByteList_Empty() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList result = bl.difference(ByteList.of());
        assertEquals(3, result.size());
    }

    @Test
    public void test_symmetricDifference_byteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4);
        ByteList result = list1.symmetricDifference(list2);
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 4));
        assertFalse(result.contains((byte) 2));
    }

    @Test
    public void test_symmetricDifference_array() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = { 2, 3, 4 };
        ByteList result = list.symmetricDifference(arr);
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 4));
    }

    @Test
    public void testSymmetricDifference() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        ByteList other = ByteList.of((byte) 2, (byte) 3, (byte) 4);
        ByteList result = list.symmetricDifference(other);
        assertEquals(2, result.size());
        assertEquals((byte) 1, result.get(0));
        assertEquals((byte) 4, result.get(1));

        result = list.symmetricDifference(new byte[] { 1, 2, 3, 4, 5 });
        assertEquals(2, result.size());
        assertEquals((byte) 4, result.get(0));
        assertEquals((byte) 5, result.get(1));

        result = list.symmetricDifference(new ByteList());
        assertEquals(3, result.size());
    }

    @Test
    public void testSymmetricDifferenceByteList() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 2, (byte) 3, (byte) 4);
        ByteList result = list1.symmetricDifference(list2);
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 4));
        assertFalse(result.contains((byte) 2));
    }

    @Test
    public void testSymmetricDifferenceArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList result = list.symmetricDifference(new byte[] { 2, 3, 4 });
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 4));
    }

    @Test
    public void testSymmetricDifference_array() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList result = bl.symmetricDifference(new byte[] { (byte) 2, (byte) 4 });
        assertTrue(result.contains((byte) 1));
        assertTrue(result.contains((byte) 3));
        assertTrue(result.contains((byte) 4));
        assertFalse(result.contains((byte) 2));
    }

    @Test
    public void test_frequency() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
        assertEquals(3, list.frequency((byte) 1));
        assertEquals(1, list.frequency((byte) 2));
        assertEquals(0, list.frequency((byte) 5));
    }

    @Test
    public void testOccurrencesOf() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 1);

        assertEquals(3, list.frequency((byte) 1));
        assertEquals(1, list.frequency((byte) 2));
        assertEquals(1, list.frequency((byte) 3));
        assertEquals(0, list.frequency((byte) 4));

        list.clear();
        assertEquals(0, list.frequency((byte) 1));
    }

    @Test
    public void testFrequency() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
        assertEquals(3, list.frequency((byte) 1));
        assertEquals(1, list.frequency((byte) 2));
        assertEquals(0, list.frequency((byte) 5));
    }

    @Test
    public void testFrequency_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 2);
        assertEquals(3, bl.frequency((byte) 2));
        assertEquals(1, bl.frequency((byte) 1));
        assertEquals(0, bl.frequency((byte) 9));
    }

    @Test
    public void test_indexOf() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(1, list.indexOf((byte) 20));
        assertEquals(-1, list.indexOf((byte) 50));
    }

    @Test
    public void testIndexOf() {
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);
        list.add((byte) 20);

        assertEquals(0, list.indexOf((byte) 10));
        assertEquals(1, list.indexOf((byte) 20));
        assertEquals(2, list.indexOf((byte) 30));
        assertEquals(-1, list.indexOf((byte) 40));

        assertEquals(3, list.indexOf((byte) 20, 2));
        assertEquals(-1, list.indexOf((byte) 10, 1));
        assertEquals(-1, list.indexOf((byte) 20, 4));

        list.clear();
        assertEquals(-1, list.indexOf((byte) 10));
    }

    @Test
    public void test_indexOf_fromIndex() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(3, list.indexOf((byte) 20, 2));
        assertEquals(-1, list.indexOf((byte) 20, 4));
    }

    @Test
    public void testIndexOfFromIndex() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(3, list.indexOf((byte) 20, 2));
    }

    @Test
    public void testIndexOf_WithFromIndex_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        int idx = bl.indexOf((byte) 1, 1);
        assertEquals(2, idx);
    }

    @Test
    public void testIndexOf_WithFromIndex_NotFound() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        int idx = bl.indexOf((byte) 1, 1);
        assertEquals(-1, idx);
    }

    @Test
    public void test_lastIndexOf() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(3, list.lastIndexOf((byte) 20));
        assertEquals(-1, list.lastIndexOf((byte) 50));
    }

    @Test
    public void testLastIndexOf() {
        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);
        list.add((byte) 20);

        assertEquals(0, list.lastIndexOf((byte) 10));
        assertEquals(3, list.lastIndexOf((byte) 20));
        assertEquals(2, list.lastIndexOf((byte) 30));
        assertEquals(-1, list.lastIndexOf((byte) 40));

        assertEquals(1, list.lastIndexOf((byte) 20, 2));
        assertEquals(-1, list.lastIndexOf((byte) 10, -1));
        assertEquals(3, list.lastIndexOf((byte) 20, 10));

        list.clear();
        assertEquals(-1, list.lastIndexOf((byte) 10));
    }

    @Test
    public void test_lastIndexOf_fromIndex() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(1, list.lastIndexOf((byte) 20, 2));
    }

    @Test
    public void testLastIndexOfFromIndex() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 20);
        assertEquals(1, list.lastIndexOf((byte) 20, 2));
    }

    @Test
    public void testLastIndexOf_withFromIndex() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 1);
        assertEquals(3, bl.lastIndexOf((byte) 2, 4));
        assertEquals(1, bl.lastIndexOf((byte) 2, 2));
        assertEquals(-1, bl.lastIndexOf((byte) 9, 4));
    }

    @Test
    public void testLastIndexOf_WithFromIndex_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2, (byte) 1);
        int idx = bl.lastIndexOf((byte) 2, 3);
        assertEquals(3, idx);
        int idx2 = bl.lastIndexOf((byte) 2, 2);
        assertEquals(1, idx2);
    }

    @Test
    public void test_min() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte min = list.min();
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsByte());
    }

    @Test
    public void test_min_empty() {
        ByteList list = new ByteList();
        OptionalByte min = list.min();
        assertFalse(min.isPresent());
    }

    @Test
    public void test_min_withRange() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte min = list.min(2, 5);
        assertTrue(min.isPresent());
        assertEquals(1, min.getAsByte());
    }

    @Test
    public void testMinMaxMedian() {
        assertFalse(list.min().isPresent());
        assertFalse(list.max().isPresent());
        assertFalse(list.median().isPresent());

        list.add((byte) 5);
        assertEquals(OptionalByte.of((byte) 5), list.min());
        assertEquals(OptionalByte.of((byte) 5), list.max());
        assertEquals(OptionalByte.of((byte) 5), list.median());

        list.clear();
        list.add((byte) 3);
        list.add((byte) 1);
        list.add((byte) 4);
        list.add((byte) 1);
        list.add((byte) 5);

        assertEquals(OptionalByte.of((byte) 1), list.min());
        assertEquals(OptionalByte.of((byte) 5), list.max());
        assertEquals(OptionalByte.of((byte) 3), list.median());

        assertEquals(OptionalByte.of((byte) 1), list.min(1, 4));
        assertEquals(OptionalByte.of((byte) 4), list.max(1, 4));
        assertEquals(OptionalByte.of((byte) 1), list.median(1, 4));

        assertFalse(list.min(2, 2).isPresent());
        assertFalse(list.max(2, 2).isPresent());
        assertFalse(list.median(2, 2).isPresent());
    }

    @Test
    public void testMin() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 5);
        OptionalByte min = list.min();
        assertTrue(min.isPresent());
        assertEquals((byte) 1, min.getAsByte());
    }

    @Test
    public void testMinEmpty() {
        assertFalse(list.min().isPresent());
    }

    @Test
    public void testMinRange() {
        ByteList list = ByteList.of((byte) 5, (byte) 3, (byte) 1, (byte) 4);
        OptionalByte min = list.min(1, 4);
        assertTrue(min.isPresent());
        assertEquals((byte) 1, min.getAsByte());
    }

    @Test
    public void test_max() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte max = list.max();
        assertTrue(max.isPresent());
        assertEquals(5, max.getAsByte());
    }

    @Test
    public void test_max_empty() {
        ByteList list = new ByteList();
        OptionalByte max = list.max();
        assertFalse(max.isPresent());
    }

    @Test
    public void test_max_withRange() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        OptionalByte max = list.max(0, 3);
        assertTrue(max.isPresent());
        assertEquals(4, max.getAsByte());
    }

    @Test
    public void testMax() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 5);
        OptionalByte max = list.max();
        assertTrue(max.isPresent());
        assertEquals((byte) 5, max.getAsByte());
    }

    @Test
    public void testMaxEmpty() {
        assertFalse(list.max().isPresent());
    }

    @Test
    public void testMaxRange() {
        ByteList list = ByteList.of((byte) 5, (byte) 3, (byte) 1, (byte) 4);
        OptionalByte max = list.max(1, 3);
        assertTrue(max.isPresent());
        assertEquals((byte) 3, max.getAsByte());
    }

    @Test
    public void test_median() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        OptionalByte median = list.median();
        assertTrue(median.isPresent());
        assertEquals(3, median.getAsByte());
    }

    @Test
    public void test_median_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        OptionalByte median = list.median(0, 3);
        assertTrue(median.isPresent());
    }

    @Test
    public void testMedian() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        OptionalByte median = list.median();
        assertTrue(median.isPresent());
        assertEquals((byte) 3, median.getAsByte());
    }

    @Test
    public void testMedianRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 5, (byte) 3, (byte) 4, (byte) 2);
        OptionalByte median = list.median(0, 3);
        assertTrue(median.isPresent());
    }

    @Test
    public void test_median_empty() {
        ByteList list = new ByteList();
        OptionalByte median = list.median();
        assertFalse(median.isPresent());
    }

    @Test
    public void testMedianEmpty() {
        assertFalse(list.median().isPresent());
    }

    @Test
    public void test_forEach() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        final int[] sum = { 0 };
        list.forEach(b -> sum[0] += b);
        assertEquals(6, sum[0]);
    }

    @Test
    public void test_forEach_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        final int[] sum = { 0 };
        list.forEach(1, 4, b -> sum[0] += b);
        assertEquals(9, sum[0]);
    }

    @Test
    @DisplayName("Test forEach method")
    public void testForEach() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        AtomicInteger sum = new AtomicInteger(0);
        list.forEach(b -> sum.addAndGet(b));
        assertEquals(6, sum.get());
    }

    @Test
    public void testForEachRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        int[] sum = { 0 };
        list.forEach(1, 4, b -> sum[0] += b);
        assertEquals(9, sum[0]);
    }

    @Test
    public void testForEach_withRange_ByteConsumer() {
        ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40, (byte) 50);
        int[] sum = { 0 };
        bl.forEach(1, 4, b -> sum[0] += b);
        assertEquals(90, sum[0]);
    }

    @Test
    public void test_first() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        OptionalByte first = list.first();
        assertTrue(first.isPresent());
        assertEquals(10, first.getAsByte());
    }

    @Test
    public void testFirstAndLast() {
        OptionalByte first = list.first();
        OptionalByte last = list.last();
        assertFalse(first.isPresent());
        assertFalse(last.isPresent());

        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        first = list.first();
        last = list.last();
        assertTrue(first.isPresent());
        assertTrue(last.isPresent());
        assertEquals((byte) 10, first.get());
        assertEquals((byte) 30, last.get());
    }

    @Test
    public void testFirst() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);
        OptionalByte first = list.first();
        assertTrue(first.isPresent());
        assertEquals((byte) 10, first.getAsByte());
    }

    @Test
    public void test_first_empty() {
        ByteList list = new ByteList();
        OptionalByte first = list.first();
        assertFalse(first.isPresent());
    }

    @Test
    public void testFirstEmpty() {
        assertFalse(list.first().isPresent());
    }

    @Test
    @DisplayName("Test first/last element accessors and removers")
    public void testFirstLastAccess() {
        ByteList list = ByteList.of((byte) 5, (byte) 10, (byte) 15);

        assertEquals((byte) 5, list.getFirst());
        assertEquals((byte) 15, list.getLast());
        assertEquals(OptionalByte.of((byte) 5), list.first());

        assertEquals((byte) 5, list.removeFirst());
        assertEquals((byte) 15, list.removeLast());
        assertArrayEquals(new byte[] { 10 }, list.toArray());

        ByteList emptyList = new ByteList();
        assertThrows(NoSuchElementException.class, emptyList::getFirst);
        assertThrows(NoSuchElementException.class, emptyList::removeLast);
    }

    @Test
    public void test_last() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        OptionalByte last = list.last();
        assertTrue(last.isPresent());
        assertEquals(30, last.getAsByte());
    }

    @Test
    public void testLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);
        OptionalByte last = list.last();
        assertTrue(last.isPresent());
        assertEquals((byte) 20, last.getAsByte());
    }

    @Test
    public void test_last_empty() {
        ByteList list = new ByteList();
        OptionalByte last = list.last();
        assertFalse(last.isPresent());
    }

    @Test
    public void testLastEmpty() {
        assertFalse(list.last().isPresent());
    }

    @Test
    public void test_distinct() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 2, (byte) 4);
        ByteList distinct = list.distinct(0, list.size());
        assertEquals(4, distinct.size());
        assertTrue(distinct.contains((byte) 1));
        assertTrue(distinct.contains((byte) 2));
        assertTrue(distinct.contains((byte) 3));
        assertTrue(distinct.contains((byte) 4));
    }

    @Test
    @DisplayName("Test 'distinct' and 'reverseSort' methods")
    public void testDistinctAndReverseSort() {
        ByteList list = ByteList.of((byte) 5, (byte) 1, (byte) 5, (byte) 2, (byte) 1);
        ByteList distinctList = list.distinct();
        assertArrayEquals(new byte[] { 5, 1, 2 }, distinctList.toArray());

        list.reverseSort();
        assertArrayEquals(new byte[] { 5, 5, 2, 1, 1 }, list.toArray());
    }

    @Test
    public void testDistinct() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 2);

        ByteList distinct = list.distinct(0, list.size());
        assertEquals(3, distinct.size());
        assertTrue(distinct.contains((byte) 1));
        assertTrue(distinct.contains((byte) 2));
        assertTrue(distinct.contains((byte) 3));

        distinct = list.distinct(1, 4);
        assertEquals(3, distinct.size());
    }

    // --- Tests for inherited PrimitiveList methods ---

    @Test
    public void testDistinctNoArg() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 2);
        ByteList distinct = list.distinct();
        assertEquals(3, distinct.size());
        assertTrue(distinct.contains((byte) 1));
        assertTrue(distinct.contains((byte) 2));
        assertTrue(distinct.contains((byte) 3));
    }

    @Test
    public void testDistinctNoArg_NoDuplicates() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList distinct = list.distinct();
        assertEquals(3, distinct.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, distinct.toArray());
    }

    @Test
    public void testDistinct_Range_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 2, (byte) 3, (byte) 1, (byte) 4);
        ByteList result = bl.distinct(1, 5);
        // distinct of [2, 2, 3, 1]
        assertEquals(3, result.size());
        assertTrue(result.contains((byte) 2));
        assertTrue(result.contains((byte) 3));
        assertTrue(result.contains((byte) 1));
    }

    @Test
    public void testDistinctEmptyRange() {
        list.addAll(new byte[] { 1, 2, 3 });
        ByteList result = list.distinct(1, 1);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDistinctNoArg_Empty() {
        ByteList distinct = list.distinct();
        assertTrue(distinct.isEmpty());
    }

    @Test
    public void test_containsDuplicates() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 1);
        assertTrue(list1.containsDuplicates());

        ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list2.containsDuplicates());
    }

    @Test
    public void testHasDuplicates() {
        assertFalse(list.containsDuplicates());

        list.add((byte) 1);
        assertFalse(list.containsDuplicates());

        list.add((byte) 2);
        assertFalse(list.containsDuplicates());

        list.add((byte) 1);
        assertTrue(list.containsDuplicates());
    }

    // --- Missing dedicated tests ---

    @Test
    public void testContainsDuplicates() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 1);
        assertTrue(list1.containsDuplicates());

        ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(list2.containsDuplicates());

        assertFalse(new ByteList().containsDuplicates());
    }

    @Test
    public void test_isSorted() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(list1.isSorted());

        ByteList list2 = ByteList.of((byte) 3, (byte) 1, (byte) 2);
        assertFalse(list2.isSorted());
    }

    @Test
    public void testIsSorted() {
        assertTrue(list.isSorted());

        list.add((byte) 1);
        assertTrue(list.isSorted());

        list.add((byte) 2);
        list.add((byte) 3);
        assertTrue(list.isSorted());

        list.add((byte) 1);
        assertFalse(list.isSorted());
    }

    @Test
    public void test_sort() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        list.sort();
        assertArrayEquals(new byte[] { 1, 1, 3, 4, 5 }, list.toArray());
    }

    @Test
    @DisplayName("Test sorting and binarySearch")
    public void testSortAndSearch() {
        ByteList list = ByteList.of((byte) 9, (byte) 2, (byte) 7, (byte) 5, (byte) 1);
        list.sort();
        assertArrayEquals(new byte[] { 1, 2, 5, 7, 9 }, list.toArray());
        assertTrue(list.isSorted());
        assertEquals(2, list.binarySearch((byte) 5));
        assertTrue(list.binarySearch((byte) 6) < 0);

        list.parallelSort();
        assertArrayEquals(new byte[] { 1, 2, 5, 7, 9 }, list.toArray());
    }

    @Test
    public void testSort() {
        list.add((byte) 3);
        list.add((byte) 1);
        list.add((byte) 4);
        list.add((byte) 1);
        list.add((byte) 5);

        list.sort();
        assertEquals(5, list.size());
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 1, list.get(1));
        assertEquals((byte) 3, list.get(2));
        assertEquals((byte) 4, list.get(3));
        assertEquals((byte) 5, list.get(4));
        assertTrue(list.isSorted());

        list.clear();
        list.sort();
        assertEquals(0, list.size());

        list.add((byte) 1);
        list.sort();
        assertEquals(1, list.size());
        assertEquals((byte) 1, list.get(0));
    }

    @Test
    public void test_parallelSort() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        list.parallelSort();
        assertArrayEquals(new byte[] { 1, 1, 3, 4, 5 }, list.toArray());
    }

    @Test
    public void testParallelSort() {
        for (int i = 100; i > 0; i--) {
            list.add((byte) (i % 128));
        }

        list.parallelSort();
        assertTrue(list.isSorted());

        for (int i = 1; i < list.size(); i++) {
            assertTrue(list.get(i - 1) <= list.get(i));
        }
    }

    @Test
    public void testParallelSort_bytes() {
        ByteList bl = ByteList.of((byte) 5, (byte) 3, (byte) 1, (byte) 4, (byte) 2);
        bl.parallelSort();
        assertEquals((byte) 1, bl.get(0));
        assertEquals((byte) 5, bl.get(4));
    }

    @Test
    public void test_reverseSort() {
        ByteList list = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        list.reverseSort();
        assertArrayEquals(new byte[] { 5, 4, 3, 1, 1 }, list.toArray());
    }

    @Test
    public void testReverseSort() {
        list.add((byte) 3);
        list.add((byte) 1);
        list.add((byte) 4);
        list.add((byte) 1);

        list.reverseSort();
        assertEquals(4, list.size());
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 1, list.get(2));
        assertEquals((byte) 1, list.get(3));
    }

    @Test
    public void testReverseSort_bytes() {
        ByteList bl = ByteList.of((byte) 3, (byte) 1, (byte) 4, (byte) 1, (byte) 5);
        bl.reverseSort();
        assertEquals((byte) 5, bl.get(0));
        assertEquals((byte) 1, bl.get(4));
    }

    @Test
    public void test_binarySearch() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        int index = list.binarySearch((byte) 3);
        assertEquals(2, index);
    }

    @Test
    public void test_binarySearch_notFound() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 4, (byte) 5);
        int index = list.binarySearch((byte) 3);
        assertTrue(index < 0);
    }

    @Test
    public void test_binarySearch_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        int index = list.binarySearch(2, 5, (byte) 4);
        assertEquals(3, index);
    }

    @Test
    public void testBinarySearch() {
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 5);
        list.add((byte) 7);
        list.add((byte) 9);

        assertTrue(list.isSorted());

        assertEquals(0, list.binarySearch((byte) 1));
        assertEquals(2, list.binarySearch((byte) 5));
        assertEquals(4, list.binarySearch((byte) 9));
        assertTrue(list.binarySearch((byte) 2) < 0);
        assertTrue(list.binarySearch((byte) 10) < 0);

        assertEquals(1, list.binarySearch(1, 4, (byte) 3));
        assertEquals(3, list.binarySearch(2, 5, (byte) 7));
        assertTrue(list.binarySearch(0, 3, (byte) 7) < 0);
    }

    @Test
    public void testBinarySearchRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        assertEquals(3, list.binarySearch(1, 5, (byte) 4));
        assertTrue(list.binarySearch(1, 3, (byte) 4) < 0);
    }

    @Test
    public void test_reverse() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.reverse();
        assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, list.toArray());
    }

    @Test
    public void test_reverse_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.reverse(1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, list.toArray());
    }

    @Test
    public void testReverse() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);

        list.reverse();
        assertEquals(4, list.size());
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 2, list.get(2));
        assertEquals((byte) 1, list.get(3));

        list.reverse(1, 3);
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 3, list.get(2));
        assertEquals((byte) 1, list.get(3));
    }

    @Test
    public void testReverseRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.reverse(1, 4);
        assertArrayEquals(new byte[] { 1, 4, 3, 2, 5 }, list.toArray());
    }

    @Test
    public void testReverse_noArg() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        bl.reverse();
        assertEquals((byte) 4, bl.get(0));
        assertEquals((byte) 1, bl.get(3));
    }

    @Test
    public void test_rotate() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.rotate(2);
        assertArrayEquals(new byte[] { 4, 5, 1, 2, 3 }, list.toArray());
    }

    @Test
    public void testRotate() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        list.add((byte) 4);

        list.rotate(1);
        assertEquals((byte) 4, list.get(0));
        assertEquals((byte) 1, list.get(1));
        assertEquals((byte) 2, list.get(2));
        assertEquals((byte) 3, list.get(3));

        list.rotate(-2);
        assertEquals((byte) 2, list.get(0));
        assertEquals((byte) 3, list.get(1));
        assertEquals((byte) 4, list.get(2));
        assertEquals((byte) 1, list.get(3));
    }

    @Test
    public void testRotate_bytes() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        bl.rotate(2);
        assertEquals((byte) 4, bl.get(0));
        assertEquals((byte) 5, bl.get(1));
    }

    @Test
    public void testRotateNegative() {
        list.addAll(new byte[] { 1, 2, 3, 4, 5 });
        list.rotate(-2);
        assertEquals((byte) 3, list.get(0));
    }

    @Test
    public void test_shuffle() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList copy = list.copy();
        list.shuffle();
        assertEquals(5, list.size());
        assertTrue(list.contains((byte) 1));
        assertTrue(list.contains((byte) 2));
        assertTrue(list.contains((byte) 3));
    }

    @Test
    public void test_shuffle_withRandom() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        list.shuffle(new java.util.Random(42));
        assertEquals(5, list.size());
    }

    @Test
    public void testShuffle() {
        for (int i = 0; i < 20; i++) {
            list.add((byte) i);
        }

        ByteList original = list.copy();
        list.shuffle();
        assertEquals(original.size(), list.size());

        for (int i = 0; i < original.size(); i++) {
            assertTrue(list.contains(original.get(i)));
        }

        list.shuffle(new Random(42));
        assertEquals(original.size(), list.size());
    }

    @Test
    public void testShuffleWithRandom() {
        list.addAll(new byte[] { 1, 2, 3, 4, 5 });
        list.shuffle(new Random(42));
        assertEquals(5, list.size());
    }

    @Test
    public void testShuffle_withRandom() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        Random rnd = new Random(123);
        bl.shuffle(rnd);
        assertEquals(5, bl.size());
    }

    @Test
    public void testShuffle_noArg() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        bl.shuffle();
        assertEquals(5, bl.size());
    }

    @Test
    public void test_swap() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.swap(0, 2);
        assertArrayEquals(new byte[] { 3, 2, 1 }, list.toArray());
    }

    @Test
    public void testSwap() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        list.swap(0, 2);
        assertEquals((byte) 3, list.get(0));
        assertEquals((byte) 2, list.get(1));
        assertEquals((byte) 1, list.get(2));

        list.swap(0, 1);
        assertEquals((byte) 2, list.get(0));
        assertEquals((byte) 3, list.get(1));

        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void testSwapOutOfBounds() {
        list.addAll(new byte[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(0, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.swap(-1, 0));
    }

    @Test
    public void test_copy_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertArrayEquals(new byte[] { 2, 3, 4 }, copy.toArray());
    }

    @Test
    public void test_copy_withStep() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        ByteList copy = list.copy(0, 6, 2);
        assertEquals(3, copy.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, copy.toArray());
    }

    @Test
    @DisplayName("Test 'copy' and 'split' methods")
    public void testCopyAndSplit() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        ByteList subCopy = list.copy(1, 4);
        assertArrayEquals(new byte[] { 2, 3, 4 }, subCopy.toArray());

        ByteList stepCopy = list.copy(0, 6, 2);
        assertArrayEquals(new byte[] { 1, 3, 5 }, stepCopy.toArray());

        List<ByteList> chunks = list.split(3);
        assertEquals(2, chunks.size());
        assertArrayEquals(new byte[] { 1, 2, 3 }, chunks.get(0).toArray());
        assertArrayEquals(new byte[] { 4, 5, 6 }, chunks.get(1).toArray());
    }

    @Test
    public void testCopy() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        ByteList copy = list.copy();
        assertEquals(list.size(), copy.size());
        for (int i = 0; i < list.size(); i++) {
            assertEquals(list.get(i), copy.get(i));
        }

        copy.set(0, (byte) 10);
        assertEquals((byte) 1, list.get(0));
        assertEquals((byte) 10, copy.get(0));

        ByteList partialCopy = list.copy(1, 3);
        assertEquals(2, partialCopy.size());
        assertEquals((byte) 2, partialCopy.get(0));
        assertEquals((byte) 3, partialCopy.get(1));

        list.clear();
        for (int i = 0; i < 10; i++) {
            list.add((byte) i);
        }
        ByteList steppedCopy = list.copy(0, 10, 2);
        assertEquals(5, steppedCopy.size());
        assertEquals((byte) 0, steppedCopy.get(0));
        assertEquals((byte) 2, steppedCopy.get(1));
        assertEquals((byte) 4, steppedCopy.get(2));
        assertEquals((byte) 6, steppedCopy.get(3));
        assertEquals((byte) 8, steppedCopy.get(4));
    }

    @Test
    public void testCopyRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList copy = list.copy(1, 4);
        assertEquals(3, copy.size());
        assertArrayEquals(new byte[] { 2, 3, 4 }, copy.toArray());
    }

    @Test
    public void testCopyWithStep() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        ByteList copy = list.copy(0, 6, 2);
        assertEquals(3, copy.size());
        assertArrayEquals(new byte[] { 1, 3, 5 }, copy.toArray());
    }

    @Test
    public void testCopy_withStep() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteList copied = bl.copy(0, 5, 2);
        assertEquals(3, copied.size());
        assertEquals((byte) 1, copied.get(0));
        assertEquals((byte) 3, copied.get(1));
        assertEquals((byte) 5, copied.get(2));
    }

    @Test
    public void test_copy() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList copy = list.copy();
        assertNotSame(list, copy);
        assertEquals(list.size(), copy.size());
        assertArrayEquals(list.toArray(), copy.toArray());
    }

    @Test
    @DisplayName("Test 'copy' and 'trimToSize' methods")
    public void testCopyAndTrim() {
        ByteList list = new ByteList(20);
        list.addAll(new byte[] { 1, 2, 3 });
        assertEquals(20, list.internalArray().length);
        list.trimToSize();
        assertEquals(3, list.internalArray().length);

        ByteList copy = list.copy();
        assertNotSame(list, copy);
        assertEquals(list, copy);
    }

    @Test
    public void testCopyEmptyList() {
        ByteList copy = list.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(list, copy);
    }

    @Test
    public void test_split() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<ByteList> chunks = list.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testSplit() {
        for (int i = 0; i < 7; i++) {
            list.add((byte) i);
        }

        List<ByteList> splits = list.split(0, list.size(), 3);
        assertEquals(3, splits.size());
        assertEquals(3, splits.get(0).size());
        assertEquals(3, splits.get(1).size());
        assertEquals(1, splits.get(2).size());

        assertEquals((byte) 0, splits.get(0).get(0));
        assertEquals((byte) 1, splits.get(0).get(1));
        assertEquals((byte) 2, splits.get(0).get(2));
    }

    @Test
    public void testSplitOneArg() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<ByteList> chunks = list.split(2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals(2, chunks.get(1).size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testSplitOneArg_ExactDivision() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        List<ByteList> chunks = list.split(2);
        assertEquals(2, chunks.size());
        assertArrayEquals(new byte[] { 1, 2 }, chunks.get(0).toArray());
        assertArrayEquals(new byte[] { 3, 4 }, chunks.get(1).toArray());
    }

    @Test
    public void testSplit_Range_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6);
        List<ByteList> chunks = bl.split(0, 6, 2);
        assertEquals(3, chunks.size());
        assertEquals(2, chunks.get(0).size());
        assertEquals((byte) 1, chunks.get(0).get(0));
        assertEquals((byte) 3, chunks.get(1).get(0));
    }

    @Test
    public void testSplit_UnevenRange_Byte() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<ByteList> chunks = bl.split(0, 5, 2);
        assertEquals(3, chunks.size());
        assertEquals(1, chunks.get(2).size());
    }

    @Test
    public void testSplitEmptyList() {
        List<ByteList> chunks = list.split(0, 0, 2);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitOneArg_Empty() {
        List<ByteList> chunks = list.split(3);
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void test_trimToSize() {
        ByteList list = new ByteList(100);
        list.add((byte) 1);
        list.add((byte) 2);
        ByteList result = list.trimToSize();
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testTrimToSize() {
        list.add((byte) 1);
        list.add((byte) 2);

        ByteList trimmed = list.trimToSize();
        assertSame(list, trimmed);
        assertEquals(2, list.size());
    }

    @Test
    public void test_clear() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClear() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testClear_nonEmpty() {
        ByteList bl = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        bl.clear();
        assertEquals(0, bl.size());
        assertTrue(bl.isEmpty());
    }

    @Test
    public void test_isEmpty() {
        ByteList list = new ByteList();
        assertTrue(list.isEmpty());
        list.add((byte) 1);
        assertFalse(list.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(list.isEmpty());

        list.add((byte) 1);
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testToList_Empty() {
        List<Byte> result = list.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToSet_Empty() {
        Set<Byte> result = list.toSet();
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_size() {
        ByteList list = new ByteList();
        assertEquals(0, list.size());
        list.add((byte) 1);
        assertEquals(1, list.size());
    }

    @Test
    public void testSize() {
        assertEquals(0, list.size());

        list.add((byte) 1);
        assertEquals(1, list.size());

        list.add((byte) 2);
        assertEquals(2, list.size());

        list.remove((byte) 1);
        assertEquals(1, list.size());
    }

    @Test
    public void test_boxed() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Byte.valueOf((byte) 1), boxed.get(0));
        assertEquals(Byte.valueOf((byte) 2), boxed.get(1));
        assertEquals(Byte.valueOf((byte) 3), boxed.get(2));
    }

    @Test
    public void test_boxed_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        List<Byte> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Byte.valueOf((byte) 2), boxed.get(0));
    }

    @Test
    public void testBoxed() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        List<Byte> boxed = list.boxed();
        assertEquals(3, boxed.size());
        assertEquals(Byte.valueOf((byte) 1), boxed.get(0));
        assertEquals(Byte.valueOf((byte) 2), boxed.get(1));
        assertEquals(Byte.valueOf((byte) 3), boxed.get(2));

        List<Byte> partialBoxed = list.boxed(1, 3);
        assertEquals(2, partialBoxed.size());
        assertEquals(Byte.valueOf((byte) 2), partialBoxed.get(0));
        assertEquals(Byte.valueOf((byte) 3), partialBoxed.get(1));
    }

    @Test
    public void testBoxedRange() {
        list.addAll(new byte[] { 1, 2, 3, 4, 5 });
        List<Byte> boxed = list.boxed(1, 4);
        assertEquals(3, boxed.size());
        assertEquals(Byte.valueOf((byte) 2), boxed.get(0));
    }

    @Test
    public void testBoxed_Range_Byte() {
        ByteList bl = ByteList.of((byte) 10, (byte) 20, (byte) 30, (byte) 40);
        List<Byte> boxed = bl.boxed(1, 3);
        assertEquals(2, boxed.size());
        assertEquals(Byte.valueOf((byte) 20), boxed.get(0));
        assertEquals(Byte.valueOf((byte) 30), boxed.get(1));
    }

    @Test
    public void testBoxedEmptyList() {
        assertTrue(list.boxed().isEmpty());
    }

    @Test
    public void test_toArray() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        byte[] arr = list.toArray();
        assertArrayEquals(new byte[] { 1, 2, 3 }, arr);
        arr[0] = 99;
        assertEquals(1, list.get(0));
    }

    @Test
    public void testToArray() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        byte[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals((byte) 1, array[0]);
        assertEquals((byte) 2, array[1]);
        assertEquals((byte) 3, array[2]);

        array[0] = 10;
        assertEquals((byte) 1, list.get(0));
    }

    @Test
    public void test_toIntList() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        IntList intList = list.toIntList();
        assertEquals(3, intList.size());
        assertEquals(1, intList.get(0));
        assertEquals(2, intList.get(1));
        assertEquals(3, intList.get(2));
    }

    @Test
    public void testToIntList() {
        list.add((byte) 1);
        list.add((byte) -1);
        list.add((byte) 127);
        list.add((byte) -128);

        IntList intList = list.toIntList();
        assertEquals(4, intList.size());
        assertEquals(1, intList.get(0));
        assertEquals(-1, intList.get(1));
        assertEquals(127, intList.get(2));
        assertEquals(-128, intList.get(3));
    }

    @Test
    public void test_toCollection() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        List<Byte> collection = list.toCollection(0, 3, ArrayList::new);
        assertEquals(3, collection.size());
        assertTrue(collection instanceof ArrayList);
    }

    @Test
    @DisplayName("Test 'toMultiset' and 'toCollection' methods")
    public void testToCollection() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 10);

        ArrayList<Byte> collection = list.toCollection(ArrayList::new);
        assertEquals(List.of((byte) 10, (byte) 20, (byte) 10), collection);

        Multiset<Byte> multiset = list.toMultiset();
        assertEquals(2, multiset.count((byte) 10));
        assertEquals(1, multiset.count((byte) 20));
    }

    @Test
    public void testToCollectionWithRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ArrayList<Byte> result = list.toCollection(1, 4, ArrayList::new);
        assertEquals(3, result.size());
        assertEquals(Byte.valueOf((byte) 2), result.get(0));
        assertEquals(Byte.valueOf((byte) 3), result.get(1));
        assertEquals(Byte.valueOf((byte) 4), result.get(2));
    }

    @Test
    public void testToCollection_Range_Byte() {
        ByteList bl = ByteList.of((byte) 5, (byte) 6, (byte) 7, (byte) 8);
        List<Byte> result = bl.toCollection(1, 3, ArrayList::new);
        assertEquals(2, result.size());
        assertEquals(Byte.valueOf((byte) 6), result.get(0));
        assertEquals(Byte.valueOf((byte) 7), result.get(1));
    }

    @Test
    public void testToCollectionOneArg() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ArrayList<Byte> result = list.toCollection(ArrayList::new);
        assertEquals(3, result.size());
        assertEquals(Byte.valueOf((byte) 1), result.get(0));
        assertEquals(Byte.valueOf((byte) 2), result.get(1));
        assertEquals(Byte.valueOf((byte) 3), result.get(2));
    }

    @Test
    public void testToCollectionOneArg_Empty() {
        ArrayList<Byte> result = list.toCollection(ArrayList::new);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testToCollectionWithRange_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(-1, 2, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(0, 4, ArrayList::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toCollection(2, 1, ArrayList::new));
    }

    @Test
    public void test_toMultiset() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        Multiset<Byte> multiset = list.toMultiset(0, 4, Multiset::new);
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count(Byte.valueOf((byte) 1)));
    }

    @Test
    public void testToMultiset() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 1);
        list.add((byte) 3);
        list.add((byte) 1);

        Multiset<Byte> multiset = list.toMultiset(0, list.size(), Multiset::new);
        assertEquals(3, multiset.count(Byte.valueOf((byte) 1)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 3)));
    }

    @Test
    public void testToMultisetNoArg() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3, (byte) 1);
        Multiset<Byte> multiset = list.toMultiset();
        assertEquals(3, multiset.count(Byte.valueOf((byte) 1)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 3)));
    }

    @Test
    public void testToMultisetTwoArgs() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        Multiset<Byte> multiset = list.toMultiset(0, 3);
        assertEquals(2, multiset.count(Byte.valueOf((byte) 1)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
        assertEquals(0, multiset.count(Byte.valueOf((byte) 3)));
    }

    @Test
    public void testToMultisetWithSupplier() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 1, (byte) 3);
        Multiset<Byte> multiset = list.toMultiset(Multiset::new);
        assertEquals(2, multiset.count(Byte.valueOf((byte) 1)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 2)));
        assertEquals(1, multiset.count(Byte.valueOf((byte) 3)));
    }

    @Test
    public void testToMultisetNoArg_Empty() {
        Multiset<Byte> multiset = list.toMultiset();
        assertEquals(0, multiset.size());
    }

    @Test
    public void testToMultisetWithRange_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4));
    }

    @Test
    public void testToMultisetThreeArgs_OutOfBounds() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(-1, 2, Multiset::new));
        assertThrows(IndexOutOfBoundsException.class, () -> list.toMultiset(0, 4, Multiset::new));
    }

    @Test
    public void test_iterator() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.nextByte());
        assertEquals(2, iter.nextByte());
        assertEquals(3, iter.nextByte());
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test iterator functionality")
    public void testIterator() {
        ByteList list = ByteList.of((byte) 100, (byte) -100);
        ByteIterator it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals((byte) 100, it.nextByte());
        assertTrue(it.hasNext());
        assertEquals((byte) -100, it.nextByte());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorEmptyList() {
        ByteIterator iter = list.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test stream creation on a sub-range")
    public void testStreamRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        long sum = list.stream(1, 4).sum();
        assertEquals(9, sum);
    }

    @Test
    public void testStream() {
        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);

        byte[] streamResult = list.stream().toArray();
        assertEquals(3, streamResult.length);
        assertEquals((byte) 1, streamResult[0]);
        assertEquals((byte) 2, streamResult[1]);
        assertEquals((byte) 3, streamResult[2]);

        byte[] partialStreamResult = list.stream(1, 3).toArray();
        assertEquals(2, partialStreamResult.length);
        assertEquals((byte) 2, partialStreamResult[0]);
        assertEquals((byte) 3, partialStreamResult[1]);
    }

    @Test
    public void test_stream() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream stream = list.stream();
        assertNotNull(stream);
        assertEquals(15, stream.sum());
    }

    @Test
    public void test_stream_withRange() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        ByteStream stream = list.stream(1, 4);
        assertNotNull(stream);
        assertEquals(9, stream.sum());
    }

    @Test
    public void testStreamEmptyList() {
        assertEquals(0, list.stream().count());
    }

    @Test
    public void test_getFirst() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(10, list.getFirst());
    }

    @Test
    public void testGetFirst() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);
        assertEquals((byte) 10, list.getFirst());
    }

    @Test
    public void test_getFirst_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void testGetFirstAndGetLast() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
        assertThrows(NoSuchElementException.class, () -> list.getLast());

        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        assertEquals((byte) 10, list.getFirst());
        assertEquals((byte) 30, list.getLast());
    }

    @Test
    public void testGetFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getFirst());
    }

    @Test
    public void test_getLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        assertEquals(30, list.getLast());
    }

    @Test
    public void testGetLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);
        assertEquals((byte) 20, list.getLast());
    }

    @Test
    public void test_getLast_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void testGetLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.getLast());
    }

    @Test
    public void test_addFirst() {
        ByteList list = ByteList.of((byte) 20, (byte) 30);
        list.addFirst((byte) 10);
        assertEquals(3, list.size());
        assertEquals(10, list.get(0));
    }

    @Test
    public void testAddFirstAndAddLast() {
        list.add((byte) 10);

        list.addFirst((byte) 5);
        assertEquals(2, list.size());
        assertEquals((byte) 5, list.get(0));
        assertEquals((byte) 10, list.get(1));

        list.addLast((byte) 15);
        assertEquals(3, list.size());
        assertEquals((byte) 5, list.get(0));
        assertEquals((byte) 10, list.get(1));
        assertEquals((byte) 15, list.get(2));
    }

    @Test
    public void testAddFirst() {
        list.add((byte) 20);
        list.addFirst((byte) 10);
        assertEquals(2, list.size());
        assertEquals((byte) 10, list.get(0));
    }

    @Test
    public void test_addLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20);
        list.addLast((byte) 30);
        assertEquals(3, list.size());
        assertEquals(30, list.get(2));
    }

    @Test
    public void testAddLast() {
        list.add((byte) 10);
        list.addLast((byte) 20);
        assertEquals(2, list.size());
        assertEquals((byte) 20, list.get(1));
    }

    @Test
    public void test_removeFirst() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte removed = list.removeFirst();
        assertEquals(10, removed);
        assertEquals(2, list.size());
        assertEquals(20, list.get(0));
    }

    @Test
    public void testRemoveFirst() {
        list.addAll(new byte[] { 10, 20, 30 });
        byte removed = list.removeFirst();
        assertEquals((byte) 10, removed);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeFirst_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void testRemoveFirstAndRemoveLast() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
        assertThrows(NoSuchElementException.class, () -> list.removeLast());

        list.add((byte) 10);
        list.add((byte) 20);
        list.add((byte) 30);

        assertEquals((byte) 10, list.removeFirst());
        assertEquals(2, list.size());
        assertEquals((byte) 20, list.get(0));
        assertEquals((byte) 30, list.get(1));

        assertEquals((byte) 30, list.removeLast());
        assertEquals(1, list.size());
        assertEquals((byte) 20, list.get(0));
    }

    @Test
    public void testRemoveFirstEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeFirst());
    }

    @Test
    public void test_removeLast() {
        ByteList list = ByteList.of((byte) 10, (byte) 20, (byte) 30);
        byte removed = list.removeLast();
        assertEquals(30, removed);
        assertEquals(2, list.size());
        assertEquals(20, list.get(1));
    }

    @Test
    public void testRemoveLast() {
        list.addAll(new byte[] { 10, 20, 30 });
        byte removed = list.removeLast();
        assertEquals((byte) 30, removed);
        assertEquals(2, list.size());
    }

    @Test
    public void test_removeLast_empty() {
        ByteList list = new ByteList();
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void testRemoveLastEmpty() {
        assertThrows(NoSuchElementException.class, () -> list.removeLast());
    }

    @Test
    public void test_hashCode() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testHashCode() {
        list.add((byte) 1);
        list.add((byte) 2);

        ByteList other = new ByteList();
        other.add((byte) 1);
        other.add((byte) 2);

        assertEquals(list.hashCode(), other.hashCode());

        other.add((byte) 3);
        assertNotEquals(list.hashCode(), other.hashCode());
    }

    @Test
    public void testEquals_ByteList() {
        ByteList bl1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList bl2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertTrue(bl1.equals(bl2));
    }

    @Test
    public void testEquals_DifferentContent() {
        ByteList bl1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList bl2 = ByteList.of((byte) 1, (byte) 2, (byte) 4);
        assertFalse(bl1.equals(bl2));
    }

    @Test
    public void testEquals_DifferentSize() {
        ByteList bl1 = ByteList.of((byte) 1, (byte) 2);
        ByteList bl2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        assertFalse(bl1.equals(bl2));
    }

    @Test
    public void test_equals() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list2 = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList list3 = ByteList.of((byte) 1, (byte) 2, (byte) 4);

        assertTrue(list1.equals(list2));
        assertFalse(list1.equals(list3));
        assertFalse(list1.equals(null));
    }

    @Test
    @DisplayName("Test equals and hashCode")
    public void testEqualsAndHashCode() {
        ByteList list1 = ByteList.of((byte) 1, (byte) 2);
        ByteList list2 = ByteList.of((byte) 1, (byte) 2);
        ByteList list3 = ByteList.of((byte) 2, (byte) 1);

        assertEquals(list1, list2);
        assertNotEquals(list1, list3);
        assertNotEquals(null, list1);
        assertNotEquals(list1, new Object());
        assertEquals(list1.hashCode(), list2.hashCode());
    }

    @Test
    public void testEquals() {
        list.add((byte) 1);
        list.add((byte) 2);

        assertEquals(list, list);

        ByteList other = new ByteList();
        other.add((byte) 1);
        other.add((byte) 2);
        assertEquals(list, other);

        other.add((byte) 3);
        assertNotEquals(list, other);

        ByteList different = new ByteList();
        different.add((byte) 2);
        different.add((byte) 1);
        assertNotEquals(list, different);

        assertNotEquals(list, null);

        assertNotEquals(list, "not a list");
    }

    @Test
    public void testEquals_Null() {
        ByteList bl = ByteList.of((byte) 1);
        assertFalse(bl.equals(null));
    }

    @Test
    public void testToString() {
        assertEquals("[]", list.toString());

        list.add((byte) 1);
        list.add((byte) 2);
        list.add((byte) 3);
        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    public void test_toString() {
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        String str = list.toString();
        assertNotNull(str);
        assertTrue(str.contains("1"));
        assertTrue(str.contains("2"));
        assertTrue(str.contains("3"));
    }

    @Test
    public void test_toString_empty() {
        ByteList list = new ByteList();
        String str = list.toString();
        assertNotNull(str);
    }

}
