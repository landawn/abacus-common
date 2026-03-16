package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
@Tag("2025")
public class PrimitiveListTest extends AbstractTest {

    private IntList list;

    @BeforeEach
    public void setUp() {
        list = IntList.of(1, 2, 3, 4, 5);
    }

    @Test
    public void test_removeDuplicates() {
        {
            final ByteList list = ByteList.range((byte) 0, (byte) 10);

            assertFalse(list.removeDuplicates());
            assertEquals(10, list.size());

        }

        {
            final ByteList list = ByteList.of((byte) 0, (byte) 1, (byte) 2, (byte) 1, (byte) 1, (byte) 3, (byte) 2);

            assertTrue(list.removeDuplicates());
            assertEquals(4, list.size());
            N.println(list.internalArray());
        }

        {
            final BooleanList list = BooleanList.of(true, false, true, true);

            assertTrue(list.removeDuplicates());
            assertEquals(2, list.size());
            N.println(list.internalArray());
        }

        {
            final BooleanList list = BooleanList.of(false, false, true, false);

            assertTrue(list.removeDuplicates());
            assertEquals(2, list.size());
            N.println(list.internalArray());
        }

        {
            final BooleanList list = BooleanList.of(false, false, false);

            assertTrue(list.removeDuplicates());
            assertEquals(1, list.size());
            N.println(list.internalArray());
        }

        {
            final BooleanList list = BooleanList.of(false, false);

            assertTrue(list.removeDuplicates());
            assertEquals(1, list.size());
            N.println(list.internalArray());
        }
    }

    @Test
    public void test_removeRange() {
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.removeRange(1, 4);

            N.println(byteList.internalArray());

            byteList.removeRange(1, 4);

            N.println(byteList.internalArray());

            byteList.removeRange(1, 4);

            N.println(byteList.internalArray());
        }
    }

    @Test
    public void test_moveRange() {
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.moveRange(1, 4, 0);

            N.println(byteList.internalArray());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.moveRange(1, 4, 7);

            N.println(byteList.internalArray());
        }
    }

    @Test
    public void test_replaceRange() {
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] {});

            N.println(byteList.internalArray());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] { 9 });

            N.println(byteList.internalArray());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] { 9, 9 });

            N.println(byteList.internalArray());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] { 9, 9, 9 });

            N.println(byteList.internalArray());
        }
    }

    @Test
    public void test_removeAt() {
        final LongList list = LongList.of(1, 2, 3, 4, 5, 6);

        list.removeAt(1, 3, 5);

        assertEquals(3, list.size());

        list.removeIf(value -> value % 2 == 1);

        assertEquals(0, list.size());
    }

    @Test
    public void test_CharList() {
        CharList arrayList = new CharList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new CharList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = CharList.of(CommonUtil.EMPTY_CHAR_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = CharList.of(Array.of('a', 'b', 'c'), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of('a', 'b', 'c'), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of('a', 'b'), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals('a', arrayList.get(0));

        arrayList.set(1, 'd');
        assertEquals('d', arrayList.get(1));

        arrayList.add('e');
        assertEquals('e', arrayList.get(2));

        arrayList.add(1, 'f');
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of('a', 'f', 'd', 'e'), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove('a');
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences('e');
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(CharList.of(Array.of('a', 'd', 'f')));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(CharList.of(Array.of('a', 'b', 'c')));

        arrayList.retainAll(CharList.of(Array.of('a', 'd')));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of('a'), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);
        assertFalse(arrayList.contains('a'));

        arrayList.addAll(CharList.of(Array.of('a', 'b', 'c')));
        assertTrue(arrayList.contains('a'));
        assertTrue(arrayList.containsAll(CharList.of(Array.of('a', 'c'))));
        assertFalse(arrayList.contains('d'));
        assertFalse(arrayList.containsAll(CharList.of(Array.of('a', 'd'))));

        final CharList subList = arrayList.copy(1, 3);
        assertEquals(subList, CharList.of(Array.of('b', 'c')));

        assertEquals(1, arrayList.indexOf('b'));
        assertEquals(1, arrayList.lastIndexOf('b'));

        assertEquals(-1, arrayList.indexOf('d'));
        assertEquals(-1, arrayList.lastIndexOf('d'));

        arrayList.add(0, 'd');

        arrayList.sort();
        assertTrue(CommonUtil.equals(Array.of('a', 'b', 'c', 'd'), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.remove('c');
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of('a', 'b', 'd'), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList('a', 'b', 'd'), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add('a');
        arrayList.add('b');

        try {
            CharList.of(Array.of('a', 'b', 'c'), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, 'c');
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.remove('a');

        assertEquals('b', CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);

        final CharList charList = CharList.of(CommonUtil.toCharArray(CommonUtil.toList('a', 'b', 'c')));
        N.println(charList);
    }

    @Test
    public void test_ByteList() {
        ByteList arrayList = new ByteList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new ByteList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = ByteList.of(CommonUtil.EMPTY_BYTE_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = ByteList.of(Array.of((byte) 1, (byte) 2, (byte) 3), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2, (byte) 3), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals((byte) 1, arrayList.get(0));

        arrayList.set(1, (byte) 4);
        assertEquals((byte) 4, arrayList.get(1));

        arrayList.add((byte) 5);
        assertEquals((byte) 5, arrayList.get(2));

        arrayList.add(1, (byte) 6);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 6, (byte) 4, (byte) 5), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove((byte) 1);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences((byte) 5);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(ByteList.of(Array.of((byte) 1, (byte) 4, (byte) 6)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(ByteList.of(Array.of((byte) 1, (byte) 2, (byte) 3)));

        arrayList.retainAll(ByteList.of(Array.of((byte) 1, (byte) 4)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of((byte) 1), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);
        assertFalse(arrayList.contains((byte) 1));

        arrayList.addAll(ByteList.of(Array.of((byte) 1, (byte) 2, (byte) 3)));
        assertTrue(arrayList.contains((byte) 1));
        assertTrue(arrayList.containsAll(ByteList.of(Array.of((byte) 1, (byte) 3))));
        assertFalse(arrayList.contains((byte) 4));
        assertFalse(arrayList.containsAll(ByteList.of(Array.of((byte) 1, (byte) 4))));

        final ByteList subList = arrayList.copy(1, 3);
        assertEquals(subList, ByteList.of(Array.of((byte) 2, (byte) 3)));

        assertEquals(1, arrayList.indexOf((byte) 2));
        assertEquals(1, arrayList.lastIndexOf((byte) 2));

        assertEquals(-1, arrayList.indexOf((byte) 4));
        assertEquals(-1, arrayList.lastIndexOf((byte) 4));

        arrayList.add(0, (byte) 4);

        arrayList.sort();
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2, (byte) 3, (byte) 4), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.remove((byte) 3);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2, (byte) 4), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList((byte) 1, (byte) 2, (byte) 4), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add((byte) 1);
        arrayList.add((byte) 2);

        try {
            ByteList.of(Array.of((byte) 1, (byte) 2, (byte) 3), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, (byte) 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.remove((byte) 1);

        assertEquals((byte) 2, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_ShortList() {
        ShortList arrayList = new ShortList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new ShortList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = ShortList.of(CommonUtil.EMPTY_SHORT_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = ShortList.of(Array.of((short) 1, (short) 2, (short) 3), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2, (short) 3), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals((short) 1, arrayList.get(0));

        arrayList.set(1, (short) 4);
        assertEquals((short) 4, arrayList.get(1));

        arrayList.add((short) 5);
        assertEquals((short) 5, arrayList.get(2));

        arrayList.add(1, (short) 6);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 6, (short) 4, (short) 5),
                CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove((short) 1);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences((short) 5);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(ShortList.of(Array.of((short) 1, (short) 4, (short) 6)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(ShortList.of(Array.of((short) 1, (short) 2, (short) 3)));

        arrayList.retainAll(ShortList.of(Array.of((short) 1, (short) 4)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of((short) 1), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);
        assertFalse(arrayList.contains((short) 1));

        arrayList.addAll(ShortList.of(Array.of((short) 1, (short) 2, (short) 3)));
        assertTrue(arrayList.contains((short) 1));
        assertTrue(arrayList.containsAll(ShortList.of(Array.of((short) 1, (short) 3))));
        assertFalse(arrayList.contains((short) 4));
        assertFalse(arrayList.containsAll(ShortList.of(Array.of((short) 1, (short) 4))));

        final ShortList subList = arrayList.copy(1, 3);
        assertEquals(subList, ShortList.of(Array.of((short) 2, (short) 3)));

        assertEquals(1, arrayList.indexOf((short) 2));
        assertEquals(1, arrayList.lastIndexOf((short) 2));

        assertEquals(-1, arrayList.indexOf((short) 4));
        assertEquals(-1, arrayList.lastIndexOf((short) 4));

        arrayList.add(0, (short) 4);

        arrayList.sort();
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2, (short) 3, (short) 4),
                CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.remove((short) 3);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2, (short) 4), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList((short) 1, (short) 2, (short) 4), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add((short) 1);
        arrayList.add((short) 2);

        try {
            ShortList.of(Array.of((short) 1, (short) 2, (short) 3), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, (short) 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.remove((short) 1);

        assertEquals((short) 2, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_delete() {
        IntList list = IntList.range(1, 10);
        list.removeRange(0, 9);
        list = IntList.range(1, 10);
        list.removeRange(0, 8);
        list = IntList.range(1, 10);
        list.removeRange(1, 8);
        list = IntList.range(1, 10);
        list.removeRange(1, 9);
        assertNotNull(list);
    }

    @Test
    public void test_IntList() {
        N.println(IntList.of(CommonUtil.toIntArray(CommonUtil.toList(1, null, 3))));
        N.println(LongList.of(CommonUtil.toLongArray(CommonUtil.toList(1L, null, 3L))));
        N.println(FloatList.of(CommonUtil.toFloatArray(CommonUtil.toList(1f, null, 3f))));
        N.println(DoubleList.of(CommonUtil.toDoubleArray(CommonUtil.toList(1d, null, 3d))));

        IntList a = IntList.of(1, 2, 3);
        IntList b = IntList.of(1, 2, 3);

        assertTrue(a.equals(b));
        assertTrue(b.equals(a));

        a = IntList.of(1, 2, 3);
        b = IntList.of(1, 2, 3, 4);

        assertFalse(a.equals(b));
        assertFalse(b.equals(a));

        float f = Long.MAX_VALUE;
        N.println(f);
        N.println((long) f == Long.MAX_VALUE);
        f = Long.MIN_VALUE;
        N.println(f);
        N.println((long) f == Long.MIN_VALUE);

        long l = (long) Float.MIN_VALUE;
        N.println(l);
        N.println(l == Long.MIN_VALUE);
        l = (long) Float.MAX_VALUE;
        N.println(l == Long.MAX_VALUE);

        l = (long) -1000000.03958390f;
        N.println(l);

        IntList arrayList = new IntList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new IntList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = IntList.of(CommonUtil.EMPTY_INT_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = IntList.of(Array.of(1, 2, 3), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1, 2, 3), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of(1, 2), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals(1, arrayList.get(0));

        arrayList.set(1, 4);
        assertEquals(4, arrayList.get(1));

        arrayList.add(5);
        assertEquals(5, arrayList.get(2));

        arrayList.add(1, 6);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1, 6, 4, 5), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove(1);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(IntList.of(Array.of(1, 4, 6)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(IntList.of(Array.of(1, 2, 3)));

        arrayList.retainAll(IntList.of(Array.of(1, 4)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);
        assertFalse(arrayList.contains(1));

        arrayList.addAll(IntList.of(Array.of(1, 2, 3)));
        assertTrue(arrayList.contains(1));
        assertTrue(arrayList.containsAll(IntList.of(Array.of(1, 3))));
        assertFalse(arrayList.contains(4));
        assertFalse(arrayList.containsAll(IntList.of(Array.of(1, 4))));

        final IntList subList = arrayList.copy(1, 3);
        assertEquals(subList, IntList.of(Array.of(2, 3)));

        assertEquals(1, arrayList.indexOf(2));
        assertEquals(1, arrayList.lastIndexOf(2));

        assertEquals(-1, arrayList.indexOf(4));
        assertEquals(-1, arrayList.lastIndexOf(4));

        arrayList.add(0, 4);

        arrayList.sort();
        assertTrue(CommonUtil.equals(Array.of(1, 2, 3, 4), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.remove(3);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1, 2, 4), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList(1, 2, 4), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add(1);
        arrayList.add(2);

        try {
            IntList.of(Array.of(1, 2, 3), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.remove(1);

        assertEquals(2, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_LongList() {
        LongList arrayList = new LongList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new LongList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = LongList.of(CommonUtil.EMPTY_LONG_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = LongList.of(Array.of(1L, 2L, 3L), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1L, 2L, 3L), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of(1L, 2L), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals(1L, arrayList.get(0));

        arrayList.set(1, 4L);
        assertEquals(4L, arrayList.get(1));

        arrayList.add(5L);
        assertEquals(5L, arrayList.get(2));

        arrayList.add(1, 6L);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1L, 6L, 4L, 5L), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove(1L);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5L);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(LongList.of(Array.of(1L, 4L, 6L)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(LongList.of(Array.of(1L, 2L, 3L)));

        arrayList.retainAll(LongList.of(Array.of(1L, 4L)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1L), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);
        assertFalse(arrayList.contains(1L));

        arrayList.addAll(LongList.of(Array.of(1L, 2L, 3L)));
        assertTrue(arrayList.contains(1L));
        assertTrue(arrayList.containsAll(LongList.of(Array.of(1L, 3L))));
        assertFalse(arrayList.contains(4L));
        assertFalse(arrayList.containsAll(LongList.of(Array.of(1L, 4L))));

        final LongList subList = arrayList.copy(1, 3);
        assertEquals(subList, LongList.of(Array.of(2L, 3L)));

        assertEquals(1, arrayList.indexOf(2L));
        assertEquals(1, arrayList.lastIndexOf(2L));

        assertEquals(-1, arrayList.indexOf(4L));
        assertEquals(-1, arrayList.lastIndexOf(4L));

        arrayList.add(0, 4L);

        arrayList.sort();
        assertTrue(CommonUtil.equals(Array.of(1L, 2L, 3L, 4L), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.remove(3L);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1L, 2L, 4L), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList(1L, 2L, 4L), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add(1L);
        arrayList.add(2L);

        try {
            LongList.of(Array.of(1L, 2L, 3L), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, 3L);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.remove(1L);

        assertEquals(2L, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_FloatList() {
        FloatList arrayList = new FloatList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new FloatList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = FloatList.of(CommonUtil.EMPTY_FLOAT_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = FloatList.of(Array.of(1f, 2f, 3f), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1f, 2f, 3f), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of(1f, 2f), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals(1f, arrayList.get(0));

        arrayList.set(1, 4f);
        assertEquals(4f, arrayList.get(1));

        arrayList.add(5f);
        assertEquals(5f, arrayList.get(2));

        arrayList.add(1, 6f);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1f, 6f, 4f, 5f), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove(1f);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5f);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(FloatList.of(Array.of(1f, 4f, 6f)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(FloatList.of(Array.of(1f, 2f, 3f)));

        arrayList.retainAll(FloatList.of(Array.of(1f, 4f)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1f), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);
        assertFalse(arrayList.contains(1f));

        arrayList.addAll(FloatList.of(Array.of(1f, 2f, 3f)));
        assertTrue(arrayList.contains(1f));
        assertTrue(arrayList.containsAll(FloatList.of(Array.of(1f, 3f))));
        assertFalse(arrayList.contains(4f));
        assertFalse(arrayList.containsAll(FloatList.of(Array.of(1f, 4f))));

        final FloatList subList = arrayList.copy(1, 3);
        assertEquals(subList, FloatList.of(Array.of(2f, 3f)));

        assertEquals(1, arrayList.indexOf(2f));
        assertEquals(1, arrayList.lastIndexOf(2f));

        assertEquals(-1, arrayList.indexOf(4f));
        assertEquals(-1, arrayList.lastIndexOf(4f));

        arrayList.add(0, 4f);

        arrayList.sort();
        assertTrue(CommonUtil.equals(Array.of(1f, 2f, 3f, 4f), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.remove(3f);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1f, 2f, 4f), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList(1f, 2f, 4f), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add(1f);
        arrayList.add(2f);

        try {
            FloatList.of(Array.of(1f, 2f, 3f), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, 3f);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.remove(1f);

        assertEquals(2f, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_DoubleList() {
        DoubleList arrayList = new DoubleList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new DoubleList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = DoubleList.of(CommonUtil.EMPTY_DOUBLE_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = DoubleList.of(Array.of(1d, 2d, 3d), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1d, 2d, 3d), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of(1d, 2d), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals(1d, arrayList.get(0));

        arrayList.set(1, 4d);
        assertEquals(4d, arrayList.get(1));

        arrayList.add(5d);
        assertEquals(5d, arrayList.get(2));

        arrayList.add(1, 6d);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1d, 6d, 4d, 5d), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove(1d);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5d);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(DoubleList.of(Array.of(1d, 4d, 6d)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(DoubleList.of(Array.of(1d, 2d, 3d)));

        arrayList.retainAll(DoubleList.of(Array.of(1d, 4d)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1d), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);
        assertFalse(arrayList.contains(1d));

        arrayList.addAll(DoubleList.of(Array.of(1d, 2d, 3d)));
        assertTrue(arrayList.contains(1d));
        assertTrue(arrayList.containsAll(DoubleList.of(Array.of(1d, 3d))));
        assertFalse(arrayList.contains(4d));
        assertFalse(arrayList.containsAll(DoubleList.of(Array.of(1d, 4d))));

        final DoubleList subList = arrayList.copy(1, 3);
        assertEquals(subList, DoubleList.of(Array.of(2d, 3d)));

        assertEquals(1, arrayList.indexOf(2d));
        assertEquals(1, arrayList.lastIndexOf(2d));

        assertEquals(-1, arrayList.indexOf(4d));
        assertEquals(-1, arrayList.lastIndexOf(4d));

        arrayList.add(0, 4d);

        arrayList.sort();
        assertTrue(CommonUtil.equals(Array.of(1d, 2d, 3d, 4d), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.remove(3d);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1d, 2d, 4d), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList(1d, 2d, 4d), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add(1d);
        arrayList.add(2d);

        try {
            DoubleList.of(Array.of(1d, 2d, 3d), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, 3d);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.remove(1d);

        assertEquals(2d, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_BooleanList() {
        BooleanList arrayList = new BooleanList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = new BooleanList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = BooleanList.of(CommonUtil.EMPTY_BOOLEAN_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList = BooleanList.of(Array.of(false, true, false), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(false, true, false), arrayList.internalArray()));
        assertTrue(CommonUtil.equals(Array.of(false, true), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        assertEquals(false, arrayList.get(0));

        arrayList.set(1, true);
        assertEquals(true, arrayList.get(1));

        arrayList.add(false);
        assertEquals(false, arrayList.get(2));

        arrayList.add(1, true);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(false, true, true, false), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.remove(false);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(false);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.removeAll(BooleanList.of(Array.of(false, true, true)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList.addAll(BooleanList.of(Array.of(false, true, false)));

        arrayList.retainAll(BooleanList.of(Array.of(true)));
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(true), CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())));

        arrayList.removeAt(0);

        arrayList.addAll(BooleanList.of(Array.of(false, true, false)));
        assertTrue(arrayList.contains(false));
        assertTrue(arrayList.containsAll(BooleanList.of(Array.of(false, false))));
        assertTrue(arrayList.contains(true));
        assertTrue(arrayList.containsAll(BooleanList.of(Array.of(true, true))));

        final BooleanList subList = arrayList.copy(1, 3);
        assertEquals(subList, BooleanList.of(Array.of(true, false)));

        assertEquals(1, arrayList.indexOf(true));
        assertEquals(2, arrayList.lastIndexOf(false));

        arrayList.add(0, true);

        arrayList.remove(false);
        N.println(CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(true, true, false), arrayList.internalArray()));

        assertTrue(CommonUtil.equals(CommonUtil.toList(true, true, false), arrayList.toList()));

        assertTrue(CommonUtil.toSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

        arrayList.clear();

        assertTrue(arrayList.isEmpty());

        N.println(arrayList);

        arrayList.add(false);
        arrayList.add(true);

        try {
            BooleanList.of(Array.of(false, true, false), 5);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.get(2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.add(3, false);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(-1, 0);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(0, 3);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        try {
            arrayList.copy(3, 2);
            fail("Should throw IndexOutOfBoundsException");
        } catch (final IndexOutOfBoundsException e) {

        }

        arrayList.sort();

        arrayList.remove(false);

        assertEquals(true, CommonUtil.copyOfRange(arrayList.internalArray(), 0, arrayList.size())[0]);
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        IntList list = new IntList(10);
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
        assertEquals(10, list.internalArray().length);
    }

    @Test
    public void testConstructorWithArrayAndSize() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = new IntList(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(a, 6));
    }

    @Test
    public void testOfArrayAndSize() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = IntList.of(a, 3);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(a, 6));
    }

    @Test
    public void testCopyOfRange() {
        int[] a = { 1, 2, 3, 4, 5 };
        IntList list = IntList.copyOf(a, 1, 4);
        assertEquals(3, list.size());
        assertArrayEquals(new int[] { 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRangeWithStep() {
        IntList list = IntList.range(1, 10, 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
    }

    @Test
    public void testRangeClosedWithStep() {
        IntList list = IntList.rangeClosed(1, 10, 2);
        assertArrayEquals(new int[] { 1, 3, 5, 7, 9 }, list.toArray());
    }

    @Test
    public void testRandomWithRange() {
        IntList list = IntList.random(1, 10, 5);
        assertEquals(5, list.size());
        for (int i = 0; i < list.size(); i++) {
            assertTrue(list.get(i) >= 1 && list.get(i) < 10);
        }
        assertThrows(IllegalArgumentException.class, () -> IntList.random(10, 1, 5));
    }

    @Test
    public void testGet() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
        assertEquals(3, list.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(3));
    }

    @Test
    public void testAdd() {
        IntList list = new IntList();
        list.add(10);
        list.add(20);
        assertEquals(2, list.size());
        assertArrayEquals(new int[] { 10, 20 }, list.toArray());
    }

    @Test
    public void testAddAll() {
        IntList list = IntList.of(1, 2);
        IntList toAdd = IntList.of(3, 4);
        list.addAll(toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testAddAllArray() {
        IntList list = IntList.of(1, 2);
        int[] toAdd = { 3, 4 };
        list.addAll(toAdd);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testRemove() {
        IntList list = IntList.of(1, 2, 3, 2);
        assertTrue(list.remove(2));
        assertArrayEquals(new int[] { 1, 3, 2 }, list.toArray());
        assertFalse(list.remove(99));
    }

    @Test
    public void testRemoveAllIntList() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 2);
        IntList toRemove = IntList.of(2, 4);
        assertTrue(list.removeAll(toRemove));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRemoveIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntPredicate predicate = (n) -> n % 2 == 0;
        assertTrue(list.removeIf(predicate));
        assertArrayEquals(new int[] { 1, 3, 5 }, list.toArray());
    }

    @Test
    public void testRetainAll() {
        IntList list = IntList.of(1, 2, 3, 2, 4);
        IntList toRetain = IntList.of(2, 4, 5);
        assertTrue(list.retainAll(toRetain));
        assertArrayEquals(new int[] { 2, 2, 4 }, list.toArray());
    }

    @Test
    public void testDelete() {
        IntList list = IntList.of(1, 2, 3);
        int deleted = list.removeAt(1);
        assertEquals(2, deleted);
        assertArrayEquals(new int[] { 1, 3 }, list.toArray());
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(2));
    }

    @Test
    public void testDeleteRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        list.removeRange(1, 4);
        assertArrayEquals(new int[] { 0, 4, 5 }, list.toArray());
    }

    @Test
    public void testReplaceRange() {
        IntList list = IntList.of(0, 1, 2, 3, 4, 5);
        IntList replacement = IntList.of(9, 8, 7);
        list.replaceRange(1, 4, replacement);
        assertArrayEquals(new int[] { 0, 9, 8, 7, 4, 5 }, list.toArray());
    }

    @Test
    public void testReplaceAll() {
        IntList list = IntList.of(1, 2, 1, 3, 1);
        int count = list.replaceAll(1, 99);
        assertEquals(3, count);
        assertArrayEquals(new int[] { 99, 2, 99, 3, 99 }, list.toArray());
    }

    @Test
    public void testReplaceIf() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        IntPredicate predicate = (n) -> n > 3;
        assertTrue(list.replaceIf(predicate, 99));
        assertArrayEquals(new int[] { 1, 2, 3, 99, 99 }, list.toArray());
    }

    @Test
    public void testFillRange() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.fill(1, 4, 99);
        assertArrayEquals(new int[] { 1, 99, 99, 99, 5 }, list.toArray());
    }

    @Test
    public void testContainsAny() {
        IntList list = IntList.of(1, 2, 3);
        IntList any1 = IntList.of(4, 5, 2);
        assertTrue(list.containsAny(any1));
        IntList any2 = IntList.of(4, 5, 6);
        assertFalse(list.containsAny(any2));
    }

    @Test
    public void testDisjoint() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(4, 5, 6);
        assertTrue(list1.disjoint(list2));
        IntList list3 = IntList.of(3, 4, 5);
        assertFalse(list1.disjoint(list3));
    }

    @Test
    public void testDifference() {
        IntList list1 = IntList.of(1, 2, 2, 3, 4);
        IntList list2 = IntList.of(2, 3, 5);
        IntList difference = list1.difference(list2);
        assertArrayEquals(new int[] { 1, 2, 4 }, difference.toArray());
    }

    @Test
    public void testOccurrencesOf() {
        IntList list = IntList.of(1, 2, 1, 3, 1, 2);
        assertEquals(3, list.frequency(1));
        assertEquals(2, list.frequency(2));
        assertEquals(0, list.frequency(4));
    }

    @Test
    public void testIndexOfFromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(3, list.indexOf(2, 2));
    }

    @Test
    public void testLastIndexOfFromIndex() {
        IntList list = IntList.of(1, 2, 3, 2, 1);
        assertEquals(1, list.lastIndexOf(2, 2));
    }

    @Test
    public void testMax() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        assertEquals(9, list.max().getAsInt());
    }

    @Test
    public void testForEach() {
        IntList list = IntList.of(1, 2, 3);
        final int[] sum = { 0 };
        IntConsumer consumer = (n) -> sum[0] += n;
        list.forEach(consumer);
        assertEquals(6, sum[0]);
    }

    @Test
    public void testLast() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals(3, list.last().getAsInt());
        assertTrue(new IntList().last().isEmpty());
    }

    @Test
    public void testHasDuplicates() {
        IntList listWithDuplicates = IntList.of(1, 2, 1, 3);
        assertTrue(listWithDuplicates.containsDuplicates());
        IntList listWithoutDuplicates = IntList.of(1, 2, 3, 4);
        assertFalse(listWithoutDuplicates.containsDuplicates());
    }

    @Test
    public void testSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.sort();
        assertArrayEquals(new int[] { 1, 1, 3, 4, 5, 9 }, list.toArray());
    }

    @Test
    public void testReverseSort() {
        IntList list = IntList.of(3, 1, 4, 1, 5, 9);
        list.reverseSort();
        assertArrayEquals(new int[] { 9, 5, 4, 3, 1, 1 }, list.toArray());
    }

    @Test
    public void testReverse() {
        IntList list = IntList.of(1, 2, 3, 4, 5);
        list.reverse();
        assertArrayEquals(new int[] { 5, 4, 3, 2, 1 }, list.toArray());
    }

    @Test
    public void testShuffle() {
        IntList list1 = IntList.of(1, 2, 3, 4, 5);
        IntList list2 = list1.copy();
        list1.shuffle();
        assertFalse(Arrays.equals(list1.toArray(), list2.toArray()) && list1.size() > 1);
    }

    @Test
    public void testSwap() {
        IntList list = IntList.of(1, 2, 3, 4);
        list.swap(1, 3);
        assertArrayEquals(new int[] { 1, 4, 3, 2 }, list.toArray());
    }

    @Test
    public void testSplit() {
        IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7);
        List<IntList> chunks = list.split(3);
        assertEquals(3, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0).toArray());
        assertArrayEquals(new int[] { 4, 5, 6 }, chunks.get(1).toArray());
        assertArrayEquals(new int[] { 7 }, chunks.get(2).toArray());
    }

    @Test
    public void testClear() {
        IntList list = IntList.of(1, 2, 3);
        list.clear();
        assertEquals(0, list.size());
        assertTrue(list.isEmpty());
    }

    @Test
    public void testToArray() {
        int[] a = { 1, 2, 3 };
        IntList list = IntList.of(a);
        assertArrayEquals(a, list.toArray());
        assertNotSame(a, list.toArray());
    }

    @Test
    public void testAddFirstAndLast() {
        IntList list = IntList.of(2, 3);
        list.addFirst(1);
        list.addLast(4);
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, list.toArray());
    }

    @Test
    public void testHashCode() {
        IntList list1 = IntList.of(1, 2, 3);
        IntList list2 = IntList.of(1, 2, 3);
        assertEquals(list1.hashCode(), list2.hashCode());
        IntList list3 = IntList.of(1, 2, 4);
        assertNotEquals(list1.hashCode(), list3.hashCode());
    }

    @Test
    public void testToString() {
        IntList list = IntList.of(1, 2, 3);
        assertEquals("[1, 2, 3]", list.toString());
        IntList emptyList = new IntList();
        assertEquals("[]", emptyList.toString());
    }

    @Test
    @DisplayName("Test addAll(L c) method")
    public void testAddAllList() {
        IntList other = IntList.of(6, 7, 8);
        boolean result = list.addAll(other);

        assertTrue(result);
        assertEquals(8, list.size());
        assertEquals(6, list.get(5));
        assertEquals(7, list.get(6));
        assertEquals(8, list.get(7));
    }

    @Test
    @DisplayName("Test addAll(int index, L c) method")
    public void testAddAllListAtIndex() {
        IntList other = IntList.of(10, 11);
        boolean result = list.addAll(2, other);

        assertTrue(result);
        assertEquals(7, list.size());
        assertEquals(10, list.get(2));
        assertEquals(11, list.get(3));
        assertEquals(3, list.get(4));
    }

    @Test
    @DisplayName("Test removeAll(L c) method")
    public void testRemoveAllList() {
        IntList toRemove = IntList.of(2, 4);
        boolean result = list.removeAll(toRemove);

        assertTrue(result);
        assertEquals(3, list.size());
        assertEquals(1, list.get(0));
        assertEquals(3, list.get(1));
        assertEquals(5, list.get(2));
    }

    @Test
    @DisplayName("Test retainAll(L c) method")
    public void testRetainAllList() {
        IntList toRetain = IntList.of(2, 3, 6);
        boolean result = list.retainAll(toRetain);

        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals(2, list.get(0));
        assertEquals(3, list.get(1));
    }

    @Test
    @DisplayName("Test replaceRange(int fromIndex, int toIndex, L replacement) method")
    public void testReplaceRangeList() {
        IntList replacement = IntList.of(10, 11);
        list.replaceRange(1, 3, replacement);

        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(10, list.get(1));
        assertEquals(11, list.get(2));
        assertEquals(4, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    @DisplayName("Test containsAny(L l) method")
    public void testContainsAnyList() {
        IntList other1 = IntList.of(6, 7, 3);
        IntList other2 = IntList.of(6, 7, 8);

        assertTrue(list.containsAny(other1));
        assertFalse(list.containsAny(other2));
    }

    @Test
    @DisplayName("Test containsAny(A a) method")
    public void testContainsAnyArray() {
        int[] array1 = { 6, 7, 3 };
        int[] array2 = { 6, 7, 8 };

        assertTrue(list.containsAny(array1));
        assertFalse(list.containsAny(array2));
    }

    @Test
    @DisplayName("Test containsAll(L l) method")
    public void testContainsAllList() {
        IntList subset = IntList.of(2, 3);
        IntList notSubset = IntList.of(2, 6);

        assertTrue(list.containsAll(subset));
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    @DisplayName("Test containsAll(A a) method")
    public void testContainsAllArray() {
        int[] subset = { 2, 3 };
        int[] notSubset = { 2, 6 };

        assertTrue(list.containsAll(subset));
        assertFalse(list.containsAll(notSubset));
    }

    @Test
    @DisplayName("Test disjoint(L l) method")
    public void testDisjointList() {
        IntList disjoint = IntList.of(6, 7, 8);
        IntList notDisjoint = IntList.of(3, 6, 7);

        assertTrue(list.disjoint(disjoint));
        assertFalse(list.disjoint(notDisjoint));
    }

    @Test
    @DisplayName("Test disjoint(A a) method")
    public void testDisjointArray() {
        int[] disjoint = { 6, 7, 8 };
        int[] notDisjoint = { 3, 6, 7 };

        assertTrue(list.disjoint(disjoint));
        assertFalse(list.disjoint(notDisjoint));
    }

    @Test
    @DisplayName("Test intersection(L b) method")
    public void testIntersectionList() {
        IntList other = IntList.of(3, 4, 5, 6, 7);
        IntList result = list.intersection(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(3, 4, 5), result);
    }

    @Test
    @DisplayName("Test intersection(A b) method")
    public void testIntersectionArray() {
        int[] other = { 3, 4, 5, 6, 7 };
        IntList result = list.intersection(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(3, 4, 5), result);
    }

    @Test
    @DisplayName("Test difference(L b) method")
    public void testDifferenceList() {
        IntList other = IntList.of(3, 4, 6);
        IntList result = list.difference(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(1, 2, 5), result);
    }

    @Test
    @DisplayName("Test difference(A a) method")
    public void testDifferenceArray() {
        int[] other = { 3, 4, 6 };
        IntList result = list.difference(other);

        assertEquals(3, result.size());
        assertEquals(IntList.of(1, 2, 5), result);
    }

    @Test
    @DisplayName("Test symmetricDifference(L b) method")
    public void testSymmetricDifferenceList() {
        IntList other = IntList.of(4, 5, 6, 7);
        IntList result = list.symmetricDifference(other);

        assertEquals(5, result.size());
        assertTrue(result.containsAll(IntList.of(1, 2, 3, 6, 7)));
    }

    @Test
    @DisplayName("Test symmetricDifference(A b) method")
    public void testSymmetricDifferenceArray() {
        int[] other = { 4, 5, 6, 7 };
        IntList result = list.symmetricDifference(other);

        assertEquals(5, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
        assertTrue(result.contains(6));
        assertTrue(result.contains(7));
    }

    @Test
    @DisplayName("Test distinct(int fromIndex, int toIndex) method")
    public void testDistinctRange() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3, 3, 4);
        IntList result = withDups.distinct(1, 6);

        assertEquals(2, result.size());
        assertEquals(IntList.of(2, 3), result);
    }

    @Test
    @DisplayName("Test reverse(int fromIndex, int toIndex) method")
    public void testReverseRange() {
        list.reverse(1, 4);

        assertEquals(5, list.size());
        assertEquals(1, list.get(0));
        assertEquals(4, list.get(1));
        assertEquals(3, list.get(2));
        assertEquals(2, list.get(3));
        assertEquals(5, list.get(4));
    }

    @Test
    @DisplayName("Test copy(int fromIndex, int toIndex) method")
    public void testCopyRange() {
        IntList copy = list.copy(1, 4);

        assertEquals(3, copy.size());
        assertEquals(IntList.of(2, 3, 4), copy);
    }

    @Test
    @DisplayName("Test copy(int fromIndex, int toIndex, int step) method")
    public void testCopyRangeWithStep() {
        IntList copy = list.copy(0, 5, 2);

        assertEquals(3, copy.size());
        assertEquals(IntList.of(1, 3, 5), copy);
    }

    @Test
    @DisplayName("Test split(int fromIndex, int toIndex, int chunkSize) method")
    public void testSplitRange() {
        List<IntList> chunks = list.split(1, 5, 2);

        assertEquals(2, chunks.size());
        assertEquals(IntList.of(2, 3), chunks.get(0));
        assertEquals(IntList.of(4, 5), chunks.get(1));
    }

    @Test
    @DisplayName("Test isEmpty() method")
    public void testIsEmpty() {
        assertFalse(list.isEmpty());

        list.clear();
        assertTrue(list.isEmpty());
    }

    @Test
    @DisplayName("Test size() method")
    public void testSize() {
        assertEquals(5, list.size());

        list.add(6);
        assertEquals(6, list.size());

        list.clear();
        assertEquals(0, list.size());
    }

    @Test
    @DisplayName("Test boxed(int fromIndex, int toIndex) method")
    public void testBoxedRange() {
        List<Integer> boxed = list.boxed(1, 4);

        assertEquals(3, boxed.size());
        assertEquals(Integer.valueOf(2), boxed.get(0));
        assertEquals(Integer.valueOf(4), boxed.get(2));
    }

    @Test
    @DisplayName("Test toSet() method")
    public void testToSet() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3);
        Set<Integer> set = withDups.toSet();

        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    @DisplayName("Test toSet(int fromIndex, int toIndex) method")
    public void testToSetRange() {
        Set<Integer> set = list.toSet(1, 4);

        assertEquals(3, set.size());
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        assertTrue(set.contains(4));
    }

    @Test
    @DisplayName("Test toCollection(IntFunction<C> supplier) method")
    public void testToCollection() {
        LinkedList<Integer> collection = list.toCollection(size -> new LinkedList<>());

        assertEquals(5, collection.size());
        assertEquals(Integer.valueOf(1), collection.getFirst());
        assertEquals(Integer.valueOf(5), collection.getLast());
    }

    @Test
    @DisplayName("Test toCollection(int fromIndex, int toIndex, IntFunction<C> supplier) method")
    public void testToCollectionRange() {
        ArrayList<Integer> collection = list.toCollection(1, 4, ArrayList::new);

        assertEquals(3, collection.size());
        assertEquals(Integer.valueOf(2), collection.get(0));
        assertEquals(Integer.valueOf(4), collection.get(2));
    }

    @Test
    @DisplayName("Test toMultiset() method")
    public void testToMultiset() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3, 3);
        Multiset<Integer> multiset = withDups.toMultiset();

        assertEquals(1, multiset.getCount(1));
        assertEquals(2, multiset.getCount(2));
        assertEquals(3, multiset.getCount(3));
    }

    @Test
    @DisplayName("Test toMultiset(int fromIndex, int toIndex) method")
    public void testToMultisetRange() {
        IntList withDups = IntList.of(1, 2, 2, 3, 3, 3);
        Multiset<Integer> multiset = withDups.toMultiset(1, 5);

        assertEquals(2, multiset.getCount(2));
        assertEquals(2, multiset.getCount(3));
        assertEquals(0, multiset.getCount(1));
    }

    @Test
    @DisplayName("Test toMultiset(IntFunction<Multiset<B>> supplier) method")
    public void testToMultisetWithSupplier() {
        Multiset<Integer> multiset = list.toMultiset(size -> new Multiset<>());

        assertEquals(1, multiset.getCount(1));
        assertEquals(1, multiset.getCount(5));
    }

    @Test
    @DisplayName("Test toMultiset(int fromIndex, int toIndex, IntFunction<Multiset<B>> supplier) method")
    public void testToMultisetRangeWithSupplier() {
        Multiset<Integer> multiset = list.toMultiset(1, 4, size -> new Multiset<>());

        assertEquals(1, multiset.getCount(2));
        assertEquals(1, multiset.getCount(3));
        assertEquals(1, multiset.getCount(4));
        assertEquals(0, multiset.getCount(1));
    }

    @Test
    @DisplayName("Test iterator() method")
    public void testIterator() {
        Iterator<Integer> iterator = list.iterator();

        assertTrue(iterator.hasNext());
        assertEquals(Integer.valueOf(1), iterator.next());
        assertEquals(Integer.valueOf(2), iterator.next());

        int count = 2;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        assertEquals(5, count);
    }

    @Test
    @DisplayName("Test edge cases and exceptions")
    public void testEdgeCases() {
        IntList emptyList = IntList.of();

        assertTrue(emptyList.isEmpty());
        assertEquals(0, emptyList.size());
        assertFalse(emptyList.containsDuplicates());
        assertTrue(emptyList.isSorted());

        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(-1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(1, 10));
        assertThrows(IndexOutOfBoundsException.class, () -> list.copy(3, 1));

        list.addAll((IntList) null);
        list.removeAll((IntList) null);
    }

    // ===================== Additional tests for untested PrimitiveList methods =====================

    @Test
    @DisplayName("Test addAll(int index, A values) inserts array at index")
    public void testAddAllArrayAtIndex() {
        int[] toAdd = { 10, 11 };
        boolean result = list.addAll(2, toAdd);

        assertTrue(result);
        assertEquals(7, list.size());
        assertEquals(10, list.get(2));
        assertEquals(11, list.get(3));
        assertEquals(3, list.get(4));
    }

    @Test
    @DisplayName("Test addAll(int index, A values) with empty array")
    public void testAddAllArrayAtIndex_EmptyArray() {
        int[] toAdd = {};
        boolean result = list.addAll(0, toAdd);

        assertFalse(result);
        assertEquals(5, list.size());
    }

    @Test
    @DisplayName("Test addAll(int index, A values) at start and end")
    public void testAddAllArrayAtIndex_StartAndEnd() {
        IntList l = IntList.of(2, 3);
        l.addAll(0, new int[] { 1 });
        l.addAll(l.size(), new int[] { 4 });
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, l.toArray());
    }

    @Test
    @DisplayName("Test addAll(int index, A values) with invalid index")
    public void testAddAllArrayAtIndex_InvalidIndex() {
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, new int[] { 10 }));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(list.size() + 1, new int[] { 10 }));
    }

    @Test
    @DisplayName("Test removeAll(A values) removes matching elements")
    public void testRemoveAllArray() {
        IntList l = IntList.of(1, 2, 3, 4, 5, 2);
        boolean result = l.removeAll(new int[] { 2, 4 });

        assertTrue(result);
        assertArrayEquals(new int[] { 1, 3, 5 }, l.toArray());
    }

    @Test
    @DisplayName("Test removeAll(A values) with no match")
    public void testRemoveAllArray_NoMatch() {
        IntList l = IntList.of(1, 2, 3);
        boolean result = l.removeAll(new int[] { 4, 5 });

        assertFalse(result);
        assertEquals(3, l.size());
    }

    @Test
    @DisplayName("Test removeAll(A values) with null/empty array")
    public void testRemoveAllArray_NullOrEmpty() {
        IntList l = IntList.of(1, 2, 3);
        assertFalse(l.removeAll((int[]) null));
        assertFalse(l.removeAll(new int[] {}));
        assertEquals(3, l.size());
    }

    @Test
    @DisplayName("Test retainAll(A values) retains only matching elements")
    public void testRetainAllArray() {
        IntList l = IntList.of(1, 2, 3, 2, 4);
        boolean result = l.retainAll(new int[] { 2, 4, 5 });

        assertTrue(result);
        assertArrayEquals(new int[] { 2, 2, 4 }, l.toArray());
    }

    @Test
    @DisplayName("Test retainAll(A values) with no match clears list")
    public void testRetainAllArray_NoMatch() {
        IntList l = IntList.of(1, 2, 3);
        boolean result = l.retainAll(new int[] { 4, 5 });

        assertTrue(result);
        assertEquals(0, l.size());
    }

    @Test
    @DisplayName("Test retainAll(A values) with null/empty array")
    public void testRetainAllArray_NullOrEmpty() {
        IntList l = IntList.of(1, 2, 3);
        assertTrue(l.retainAll((int[]) null));
        assertEquals(0, l.size());
    }

    @Test
    @DisplayName("Test replaceRange(int, int, A) replaces range with array")
    public void testReplaceRangeArray() {
        IntList l = IntList.of(0, 1, 2, 3, 4, 5);
        l.replaceRange(1, 4, new int[] { 9, 8, 7 });

        assertArrayEquals(new int[] { 0, 9, 8, 7, 4, 5 }, l.toArray());
    }

    @Test
    @DisplayName("Test replaceRange(int, int, A) with larger replacement")
    public void testReplaceRangeArray_LargerReplacement() {
        IntList l = IntList.of(1, 2, 3);
        l.replaceRange(1, 2, new int[] { 10, 11, 12 });

        assertArrayEquals(new int[] { 1, 10, 11, 12, 3 }, l.toArray());
    }

    @Test
    @DisplayName("Test replaceRange(int, int, A) with empty replacement deletes range")
    public void testReplaceRangeArray_EmptyReplacement() {
        IntList l = IntList.of(1, 2, 3, 4, 5);
        l.replaceRange(1, 4, new int[] {});

        assertArrayEquals(new int[] { 1, 5 }, l.toArray());
    }

    @Test
    @DisplayName("Test replaceRange(int, int, A) with null replacement deletes range")
    public void testReplaceRangeArray_NullReplacement() {
        IntList l = IntList.of(1, 2, 3, 4, 5);
        l.replaceRange(1, 4, (int[]) null);

        assertArrayEquals(new int[] { 1, 5 }, l.toArray());
    }

    @Test
    @DisplayName("Test replaceRange(int, int, A) with invalid range")
    public void testReplaceRangeArray_InvalidRange() {
        IntList l = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> l.replaceRange(-1, 2, new int[] { 10 }));
        assertThrows(IndexOutOfBoundsException.class, () -> l.replaceRange(0, 4, new int[] { 10 }));
    }

    @Test
    @DisplayName("Test distinct() returns list with unique elements")
    public void testDistinct() {
        IntList withDups = IntList.of(1, 2, 2, 3, 1, 4, 3);
        IntList result = withDups.distinct();

        assertEquals(4, result.size());
        assertEquals(IntList.of(1, 2, 3, 4), result);
    }

    @Test
    @DisplayName("Test distinct() on empty list")
    public void testDistinct_Empty() {
        IntList emptyList = IntList.of();
        IntList result = emptyList.distinct();

        assertEquals(0, result.size());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test distinct() on list without duplicates")
    public void testDistinct_NoDuplicates() {
        IntList l = IntList.of(1, 2, 3, 4);
        IntList result = l.distinct();

        assertEquals(4, result.size());
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, result.toArray());
    }

    @Test
    @DisplayName("Test isSorted() on sorted list")
    public void testIsSorted() {
        IntList sorted = IntList.of(1, 2, 3, 4, 5);
        assertTrue(sorted.isSorted());
    }

    @Test
    @DisplayName("Test isSorted() on unsorted list")
    public void testIsSorted_Unsorted() {
        IntList unsorted = IntList.of(1, 3, 2, 4);
        assertFalse(unsorted.isSorted());
    }

    @Test
    @DisplayName("Test isSorted() on empty and single element list")
    public void testIsSorted_EmptyAndSingle() {
        assertTrue(IntList.of().isSorted());
        assertTrue(IntList.of(42).isSorted());
    }

    @Test
    @DisplayName("Test isSorted() with equal consecutive elements")
    public void testIsSorted_WithEquals() {
        IntList l = IntList.of(1, 2, 2, 3, 3, 5);
        assertTrue(l.isSorted());
    }

    @Test
    @DisplayName("Test rotate(int distance) rotates elements right")
    public void testRotate() {
        IntList l = IntList.of(1, 2, 3, 4, 5);
        l.rotate(2);
        assertArrayEquals(new int[] { 4, 5, 1, 2, 3 }, l.toArray());
    }

    @Test
    @DisplayName("Test rotate(int distance) with negative distance rotates left")
    public void testRotate_Negative() {
        IntList l = IntList.of(1, 2, 3, 4, 5);
        l.rotate(-2);
        assertArrayEquals(new int[] { 3, 4, 5, 1, 2 }, l.toArray());
    }

    @Test
    @DisplayName("Test rotate(int distance) with zero distance")
    public void testRotate_Zero() {
        IntList l = IntList.of(1, 2, 3);
        l.rotate(0);
        assertArrayEquals(new int[] { 1, 2, 3 }, l.toArray());
    }

    @Test
    @DisplayName("Test rotate(int distance) with distance equal to size")
    public void testRotate_FullRotation() {
        IntList l = IntList.of(1, 2, 3);
        l.rotate(3);
        assertArrayEquals(new int[] { 1, 2, 3 }, l.toArray());
    }

    @Test
    @DisplayName("Test rotate(int distance) on empty list")
    public void testRotate_Empty() {
        IntList l = IntList.of();
        l.rotate(5);
        assertTrue(l.isEmpty());
    }

    @Test
    @DisplayName("Test shuffle(Random rnd) with seeded random")
    public void testShuffleWithRandom() {
        IntList l1 = IntList.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        IntList l2 = l1.copy();
        Random rnd = new Random(42);
        l1.shuffle(rnd);
        // After shuffle with a specific seed, the order should change
        assertEquals(l2.size(), l1.size());
        // Ensure same elements
        l1.sort();
        l2.sort();
        assertArrayEquals(l1.toArray(), l2.toArray());
    }

    @Test
    @DisplayName("Test shuffle(Random rnd) produces deterministic result with same seed")
    public void testShuffleWithRandom_Deterministic() {
        IntList l1 = IntList.of(1, 2, 3, 4, 5);
        IntList l2 = IntList.of(1, 2, 3, 4, 5);
        l1.shuffle(new Random(123));
        l2.shuffle(new Random(123));
        assertArrayEquals(l1.toArray(), l2.toArray());
    }

    @Test
    @DisplayName("Test copy() returns independent copy of all elements")
    public void testCopy() {
        IntList original = IntList.of(1, 2, 3);
        IntList copy = original.copy();

        assertEquals(original.size(), copy.size());
        assertArrayEquals(original.toArray(), copy.toArray());
        assertNotSame(original, copy);

        // Modifications to copy should not affect original
        copy.add(4);
        assertEquals(3, original.size());
        assertEquals(4, copy.size());
    }

    @Test
    @DisplayName("Test copy() on empty list")
    public void testCopy_Empty() {
        IntList empty = IntList.of();
        IntList copy = empty.copy();
        assertTrue(copy.isEmpty());
        assertNotSame(empty, copy);
    }

    @Test
    @DisplayName("Test trimToSize() reduces capacity to size")
    public void testTrimToSize() {
        IntList l = new IntList(100);
        l.add(1);
        l.add(2);
        l.add(3);
        assertEquals(3, l.size());
        assertTrue(l.internalArray().length >= 100);

        l.trimToSize();
        assertEquals(3, l.size());
        assertEquals(3, l.internalArray().length);
        assertArrayEquals(new int[] { 1, 2, 3 }, l.toArray());
    }

    @Test
    @DisplayName("Test trimToSize() on empty list")
    public void testTrimToSize_Empty() {
        IntList l = new IntList(100);
        l.trimToSize();
        assertEquals(0, l.size());
    }

    @Test
    @DisplayName("Test trimToSize() returns same list for chaining")
    public void testTrimToSize_Chaining() {
        IntList l = IntList.of(1, 2, 3);
        IntList result = l.trimToSize();
        assertTrue(result == l);
    }

    @Test
    @DisplayName("Test boxed() returns boxed list of all elements")
    public void testBoxed() {
        IntList l = IntList.of(1, 2, 3);
        List<Integer> boxed = l.boxed();

        assertEquals(3, boxed.size());
        assertEquals(Integer.valueOf(1), boxed.get(0));
        assertEquals(Integer.valueOf(2), boxed.get(1));
        assertEquals(Integer.valueOf(3), boxed.get(2));
    }

    @Test
    @DisplayName("Test boxed() on empty list")
    public void testBoxed_Empty() {
        IntList l = IntList.of();
        List<Integer> boxed = l.boxed();
        assertTrue(boxed.isEmpty());
    }

    @Test
    @DisplayName("Test toList() delegates to boxed()")
    public void testToListDeprecated() {
        IntList l = IntList.of(1, 2, 3);
        @SuppressWarnings("deprecation")
        List<Integer> result = l.toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(2));
    }

    @Test
    @DisplayName("Test toList(int, int) delegates to boxed(int, int)")
    public void testToListRangeDeprecated() {
        IntList l = IntList.of(1, 2, 3, 4, 5);
        @SuppressWarnings("deprecation")
        List<Integer> result = l.toList(1, 4);

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(2), result.get(0));
        assertEquals(Integer.valueOf(4), result.get(2));
    }

    @Test
    @DisplayName("Test toList() on empty list")
    public void testToListDeprecated_Empty() {
        IntList l = IntList.of();
        @SuppressWarnings("deprecation")
        List<Integer> result = l.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test internalArray() returns backing array")
    public void testInternalArray() {
        IntList l = IntList.of(1, 2, 3);
        int[] internal = l.internalArray();
        assertNotNull(internal);
        assertTrue(internal.length >= 3);
        assertEquals(1, internal[0]);
        assertEquals(2, internal[1]);
        assertEquals(3, internal[2]);
    }

    @Test
    @DisplayName("Test rangeCheck throws for out of bounds index")
    public void testRangeCheck() {
        IntList l = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(3));
        assertThrows(IndexOutOfBoundsException.class, () -> l.get(100));
    }

    @Test
    @DisplayName("Test throwNoSuchElementExceptionIfEmpty on IntList")
    public void testThrowNoSuchElementExceptionIfEmpty() {
        IntList emptyList = IntList.of();
        // first() on empty IntList should return empty optional
        assertTrue(emptyList.first().isEmpty());
    }

    @Test
    @DisplayName("Test swap with same indices")
    public void testSwap_SameIndex() {
        IntList l = IntList.of(1, 2, 3);
        l.swap(1, 1);
        assertArrayEquals(new int[] { 1, 2, 3 }, l.toArray());
    }

    @Test
    @DisplayName("Test swap with invalid indices")
    public void testSwap_InvalidIndex() {
        IntList l = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> l.swap(-1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> l.swap(0, 3));
    }

    @Test
    @DisplayName("Test moveRange moves elements correctly")
    public void testMoveRange_Forward() {
        IntList l = IntList.of(0, 1, 2, 3, 4, 5);
        l.moveRange(1, 3, 3);
        // Elements [1, 2] moved to position 3
        assertEquals(6, l.size());
    }

    @Test
    @DisplayName("Test removeRange with equal fromIndex and toIndex")
    public void testRemoveRange_SameIndex() {
        IntList l = IntList.of(1, 2, 3, 4, 5);
        l.removeRange(2, 2);
        assertEquals(5, l.size());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, l.toArray());
    }

    @Test
    @DisplayName("Test removeRange with invalid range")
    public void testRemoveRange_InvalidRange() {
        IntList l = IntList.of(1, 2, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> l.removeRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> l.removeRange(0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> l.removeRange(2, 1));
    }

    @Test
    @DisplayName("Test containsDuplicates on empty list")
    public void testContainsDuplicates_Empty() {
        IntList emptyList = IntList.of();
        assertFalse(emptyList.containsDuplicates());
    }

    @Test
    @DisplayName("Test containsDuplicates on single element")
    public void testContainsDuplicates_Single() {
        IntList single = IntList.of(1);
        assertFalse(single.containsDuplicates());
    }

    @Test
    @DisplayName("Test split with chunkSize larger than list size")
    public void testSplit_ChunkSizeLargerThanList() {
        IntList l = IntList.of(1, 2, 3);
        List<IntList> chunks = l.split(10);
        assertEquals(1, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0).toArray());
    }

    @Test
    @DisplayName("Test split with chunkSize equal to list size")
    public void testSplit_ChunkSizeEqualsListSize() {
        IntList l = IntList.of(1, 2, 3);
        List<IntList> chunks = l.split(3);
        assertEquals(1, chunks.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, chunks.get(0).toArray());
    }

    @Test
    @DisplayName("Test split on empty list")
    public void testSplit_EmptyList() {
        IntList l = IntList.of();
        List<IntList> chunks = l.split(3);
        assertEquals(0, chunks.size());
    }

    @Test
    @DisplayName("Test copy(int, int, int) with reverse step")
    public void testCopyRangeWithStep_Reverse() {
        IntList l = IntList.of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        IntList result = l.copy(8, 2, -2);
        assertArrayEquals(new int[] { 8, 6, 4 }, result.toArray());
    }

    @Test
    @DisplayName("Test removeDuplicates on list without duplicates")
    public void testRemoveDuplicates_NoDuplicates() {
        IntList l = IntList.of(1, 2, 3, 4);
        assertFalse(l.removeDuplicates());
        assertEquals(4, l.size());
    }

    @Test
    @DisplayName("Test removeDuplicates on empty list")
    public void testRemoveDuplicates_Empty() {
        IntList l = IntList.of();
        assertFalse(l.removeDuplicates());
        assertEquals(0, l.size());
    }

    @Test
    @DisplayName("Test addAll(L) with null list does not modify")
    public void testAddAllList_Null() {
        int sizeBefore = list.size();
        boolean result = list.addAll((IntList) null);
        assertFalse(result);
        assertEquals(sizeBefore, list.size());
    }

    @Test
    @DisplayName("Test addAll(L) with empty list does not modify")
    public void testAddAllList_Empty() {
        int sizeBefore = list.size();
        boolean result = list.addAll(IntList.of());
        assertFalse(result);
        assertEquals(sizeBefore, list.size());
    }

    @Test
    @DisplayName("Test reverse on empty list")
    public void testReverse_Empty() {
        IntList l = IntList.of();
        l.reverse();
        assertTrue(l.isEmpty());
    }

    @Test
    @DisplayName("Test reverse on single element")
    public void testReverse_Single() {
        IntList l = IntList.of(42);
        l.reverse();
        assertArrayEquals(new int[] { 42 }, l.toArray());
    }

    @Test
    @DisplayName("Test sort on empty list")
    public void testSort_Empty() {
        IntList l = IntList.of();
        l.sort();
        assertTrue(l.isEmpty());
    }

    @Test
    @DisplayName("Test reverseSort on empty list")
    public void testReverseSort_Empty() {
        IntList l = IntList.of();
        l.reverseSort();
        assertTrue(l.isEmpty());
    }

    @Test
    @DisplayName("Test clear on already empty list")
    public void testClear_AlreadyEmpty() {
        IntList l = IntList.of();
        l.clear();
        assertTrue(l.isEmpty());
        assertEquals(0, l.size());
    }

    @Test
    @DisplayName("Test removeAt(int...) with multiple indices")
    public void testRemoveAtMultipleIndices() {
        IntList l = IntList.of(10, 20, 30, 40, 50);
        l.removeAt(new int[] { 1, 3 });
        assertEquals(3, l.size());
    }

    @Test
    @DisplayName("Test toSet on empty list")
    public void testToSet_Empty() {
        IntList l = IntList.of();
        Set<Integer> set = l.toSet();
        assertTrue(set.isEmpty());
    }

    @Test
    @DisplayName("Test toCollection on empty list")
    public void testToCollection_Empty() {
        IntList l = IntList.of();
        ArrayList<Integer> collection = l.toCollection(ArrayList::new);
        assertTrue(collection.isEmpty());
    }

    @Test
    @DisplayName("Test toMultiset on empty list")
    public void testToMultiset_Empty() {
        IntList l = IntList.of();
        Multiset<Integer> multiset = l.toMultiset();
        assertEquals(0, multiset.size());
    }

    @Test
    @DisplayName("Test iterator on empty list")
    public void testIterator_Empty() {
        IntList l = IntList.of();
        Iterator<Integer> iter = l.iterator();
        assertFalse(iter.hasNext());
    }

    @Test
    @DisplayName("Test containsAny with empty other list")
    public void testContainsAny_EmptyOther() {
        assertFalse(list.containsAny(IntList.of()));
        assertFalse(list.containsAny(new int[] {}));
    }

    @Test
    @DisplayName("Test containsAll with empty other list")
    public void testContainsAll_EmptyOther() {
        assertTrue(list.containsAll(IntList.of()));
        assertTrue(list.containsAll(new int[] {}));
    }

    @Test
    @DisplayName("Test disjoint with empty other list")
    public void testDisjoint_EmptyOther() {
        assertTrue(list.disjoint(IntList.of()));
        assertTrue(list.disjoint(new int[] {}));
    }

    @Test
    @DisplayName("Test intersection with empty other list")
    public void testIntersection_EmptyOther() {
        IntList result = list.intersection(IntList.of());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Test difference with empty other list")
    public void testDifference_EmptyOther() {
        IntList result = list.difference(IntList.of());
        assertEquals(list.size(), result.size());
    }

    @Test
    @DisplayName("Test symmetricDifference with empty other list")
    public void testSymmetricDifference_EmptyOther() {
        IntList result = list.symmetricDifference(IntList.of());
        assertEquals(list.size(), result.size());
    }

}
