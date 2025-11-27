package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class PrimitiveListTest extends AbstractTest {

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
            N.println(list.array());
        }

        {
            final BooleanList list = BooleanList.of(true, false, true, true);

            assertTrue(list.removeDuplicates());
            assertEquals(2, list.size());
            N.println(list.array());
        }

        {
            final BooleanList list = BooleanList.of(false, false, true, false);

            assertTrue(list.removeDuplicates());
            assertEquals(2, list.size());
            N.println(list.array());
        }

        {
            final BooleanList list = BooleanList.of(false, false, false);

            assertTrue(list.removeDuplicates());
            assertEquals(1, list.size());
            N.println(list.array());
        }

        {
            final BooleanList list = BooleanList.of(false, false);

            assertTrue(list.removeDuplicates());
            assertEquals(1, list.size());
            N.println(list.array());
        }
    }

    @Test
    public void test_deleteRange() {
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.deleteRange(1, 4);

            N.println(byteList.array());

            byteList.deleteRange(1, 4);

            N.println(byteList.array());

            byteList.deleteRange(1, 4);

            N.println(byteList.array());
        }
    }

    @Test
    public void test_moveRange() {
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.moveRange(1, 4, 0);

            N.println(byteList.array());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.moveRange(1, 4, 7);

            N.println(byteList.array());
        }
    }

    @Test
    public void test_replaceRange() {
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] {});

            N.println(byteList.array());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] { 9 });

            N.println(byteList.array());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] { 9, 9 });

            N.println(byteList.array());
        }
        {
            final ByteList byteList = ByteList.range((byte) 0, (byte) 10);

            byteList.replaceRange(1, 3, new byte[] { 9, 9, 9 });

            N.println(byteList.array());
        }
    }

    @Test
    public void test_deleteAllByIndices() {
        final LongList list = LongList.of(1, 2, 3, 4, 5, 6);

        list.deleteAllByIndices(1, 3, 5);

        assertEquals(3, list.size());

        list.removeIf(value -> value % 2 == 1);

        assertEquals(0, list.size());
    }

    @Test
    public void test_CharList() {
        CharList arrayList = new CharList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new CharList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = CharList.of(CommonUtil.EMPTY_CHAR_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_CHAR_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = CharList.of(Array.of('a', 'b', 'c'), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of('a', 'b', 'c'), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of('a', 'b'), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals('a', arrayList.get(0));

        arrayList.set(1, 'd');
        assertEquals('d', arrayList.get(1));

        arrayList.add('e');
        assertEquals('e', arrayList.get(2));

        arrayList.add(1, 'f');
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of('a', 'f', 'd', 'e'), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove('a');
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences('e');
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(CharList.of(Array.of('a', 'd', 'f')));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(CharList.of(Array.of('a', 'b', 'c')));

        arrayList.retainAll(CharList.of(Array.of('a', 'd')));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of('a'), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);
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
        assertTrue(CommonUtil.equals(Array.of('a', 'b', 'c', 'd'), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.remove('c');
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of('a', 'b', 'd'), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList('a', 'b', 'd'), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals('b', CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);

        final CharList charList = CharList.of(CommonUtil.toCharArray(CommonUtil.asList('a', 'b', 'c')));
        N.println(charList);
    }

    @Test
    public void test_ByteList() {
        ByteList arrayList = new ByteList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new ByteList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = ByteList.of(CommonUtil.EMPTY_BYTE_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BYTE_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = ByteList.of(Array.of((byte) 1, (byte) 2, (byte) 3), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2, (byte) 3), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals((byte) 1, arrayList.get(0));

        arrayList.set(1, (byte) 4);
        assertEquals((byte) 4, arrayList.get(1));

        arrayList.add((byte) 5);
        assertEquals((byte) 5, arrayList.get(2));

        arrayList.add(1, (byte) 6);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 6, (byte) 4, (byte) 5), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove((byte) 1);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences((byte) 5);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(ByteList.of(Array.of((byte) 1, (byte) 4, (byte) 6)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(ByteList.of(Array.of((byte) 1, (byte) 2, (byte) 3)));

        arrayList.retainAll(ByteList.of(Array.of((byte) 1, (byte) 4)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of((byte) 1), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);
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
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2, (byte) 3, (byte) 4), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.remove((byte) 3);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of((byte) 1, (byte) 2, (byte) 4), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList((byte) 1, (byte) 2, (byte) 4), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals((byte) 2, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_ShortList() {
        ShortList arrayList = new ShortList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new ShortList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = ShortList.of(CommonUtil.EMPTY_SHORT_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_SHORT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = ShortList.of(Array.of((short) 1, (short) 2, (short) 3), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2, (short) 3), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals((short) 1, arrayList.get(0));

        arrayList.set(1, (short) 4);
        assertEquals((short) 4, arrayList.get(1));

        arrayList.add((short) 5);
        assertEquals((short) 5, arrayList.get(2));

        arrayList.add(1, (short) 6);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 6, (short) 4, (short) 5), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove((short) 1);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences((short) 5);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(ShortList.of(Array.of((short) 1, (short) 4, (short) 6)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(ShortList.of(Array.of((short) 1, (short) 2, (short) 3)));

        arrayList.retainAll(ShortList.of(Array.of((short) 1, (short) 4)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of((short) 1), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);
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
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2, (short) 3, (short) 4), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.remove((short) 3);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of((short) 1, (short) 2, (short) 4), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList((short) 1, (short) 2, (short) 4), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals((short) 2, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_delete() {
        IntList list = IntList.range(1, 10);
        list.deleteRange(0, 9);
        list = IntList.range(1, 10);
        list.deleteRange(0, 8);
        list = IntList.range(1, 10);
        list.deleteRange(1, 8);
        list = IntList.range(1, 10);
        list.deleteRange(1, 9);
    }

    @Test
    public void test_IntList() {
        N.println(IntList.of(CommonUtil.toIntArray(CommonUtil.asList(1, null, 3))));
        N.println(LongList.of(CommonUtil.toLongArray(CommonUtil.asList(1L, null, 3L))));
        N.println(FloatList.of(CommonUtil.toFloatArray(CommonUtil.asList(1f, null, 3f))));
        N.println(DoubleList.of(CommonUtil.toDoubleArray(CommonUtil.asList(1d, null, 3d))));

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
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new IntList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = IntList.of(CommonUtil.EMPTY_INT_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_INT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = IntList.of(Array.of(1, 2, 3), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1, 2, 3), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of(1, 2), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals(1, arrayList.get(0));

        arrayList.set(1, 4);
        assertEquals(4, arrayList.get(1));

        arrayList.add(5);
        assertEquals(5, arrayList.get(2));

        arrayList.add(1, 6);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1, 6, 4, 5), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove(1);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(IntList.of(Array.of(1, 4, 6)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(IntList.of(Array.of(1, 2, 3)));

        arrayList.retainAll(IntList.of(Array.of(1, 4)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);
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
        assertTrue(CommonUtil.equals(Array.of(1, 2, 3, 4), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.remove(3);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1, 2, 4), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList(1, 2, 4), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals(2, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_LongList() {
        LongList arrayList = new LongList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new LongList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = LongList.of(CommonUtil.EMPTY_LONG_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_LONG_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = LongList.of(Array.of(1L, 2L, 3L), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1L, 2L, 3L), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of(1L, 2L), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals(1L, arrayList.get(0));

        arrayList.set(1, 4L);
        assertEquals(4L, arrayList.get(1));

        arrayList.add(5L);
        assertEquals(5L, arrayList.get(2));

        arrayList.add(1, 6L);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1L, 6L, 4L, 5L), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove(1L);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5L);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(LongList.of(Array.of(1L, 4L, 6L)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(LongList.of(Array.of(1L, 2L, 3L)));

        arrayList.retainAll(LongList.of(Array.of(1L, 4L)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1L), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);
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
        assertTrue(CommonUtil.equals(Array.of(1L, 2L, 3L, 4L), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.remove(3L);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1L, 2L, 4L), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList(1L, 2L, 4L), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals(2L, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_FloatList() {
        FloatList arrayList = new FloatList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new FloatList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = FloatList.of(CommonUtil.EMPTY_FLOAT_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_FLOAT_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = FloatList.of(Array.of(1f, 2f, 3f), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1f, 2f, 3f), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of(1f, 2f), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals(1f, arrayList.get(0));

        arrayList.set(1, 4f);
        assertEquals(4f, arrayList.get(1));

        arrayList.add(5f);
        assertEquals(5f, arrayList.get(2));

        arrayList.add(1, 6f);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1f, 6f, 4f, 5f), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove(1f);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5f);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(FloatList.of(Array.of(1f, 4f, 6f)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(FloatList.of(Array.of(1f, 2f, 3f)));

        arrayList.retainAll(FloatList.of(Array.of(1f, 4f)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1f), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);
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
        assertTrue(CommonUtil.equals(Array.of(1f, 2f, 3f, 4f), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.remove(3f);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1f, 2f, 4f), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList(1f, 2f, 4f), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals(2f, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_DoubleList() {
        DoubleList arrayList = new DoubleList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new DoubleList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = DoubleList.of(CommonUtil.EMPTY_DOUBLE_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_DOUBLE_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = DoubleList.of(Array.of(1d, 2d, 3d), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(1d, 2d, 3d), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of(1d, 2d), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals(1d, arrayList.get(0));

        arrayList.set(1, 4d);
        assertEquals(4d, arrayList.get(1));

        arrayList.add(5d);
        assertEquals(5d, arrayList.get(2));

        arrayList.add(1, 6d);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(1d, 6d, 4d, 5d), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove(1d);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(5d);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(DoubleList.of(Array.of(1d, 4d, 6d)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(DoubleList.of(Array.of(1d, 2d, 3d)));

        arrayList.retainAll(DoubleList.of(Array.of(1d, 4d)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(1d), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);
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
        assertTrue(CommonUtil.equals(Array.of(1d, 2d, 3d, 4d), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.remove(3d);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(1d, 2d, 4d), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList(1d, 2d, 4d), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals(2d, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);
    }

    @Test
    public void test_BooleanList() {
        BooleanList arrayList = new BooleanList();

        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = new BooleanList(10);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = BooleanList.of(CommonUtil.EMPTY_BOOLEAN_ARRAY);
        assertEquals(0, arrayList.size());
        assertTrue(CommonUtil.equals(CommonUtil.EMPTY_BOOLEAN_ARRAY, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList = BooleanList.of(Array.of(false, true, false), 2);
        assertEquals(2, arrayList.size());
        assertTrue(CommonUtil.equals(Array.of(false, true, false), arrayList.array()));
        assertTrue(CommonUtil.equals(Array.of(false, true), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        assertEquals(false, arrayList.get(0));

        arrayList.set(1, true);
        assertEquals(true, arrayList.get(1));

        arrayList.add(false);
        assertEquals(false, arrayList.get(2));

        arrayList.add(1, true);
        N.println(arrayList);
        assertTrue(CommonUtil.equals(Array.of(false, true, true, false), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.addAll(arrayList);

        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(2, arrayList);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.remove(false);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAllOccurrences(false);
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.removeAll(BooleanList.of(Array.of(false, true, true)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList.addAll(BooleanList.of(Array.of(false, true, false)));

        arrayList.retainAll(BooleanList.of(Array.of(true)));
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));
        assertTrue(CommonUtil.equals(Array.of(true), CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())));

        arrayList.delete(0);

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
        N.println(CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size()));

        arrayList = arrayList.trimToSize();
        assertTrue(CommonUtil.equals(Array.of(true, true, false), arrayList.array()));

        assertTrue(CommonUtil.equals(CommonUtil.asList(true, true, false), arrayList.toList()));

        assertTrue(CommonUtil.asSet(arrayList).contains(arrayList.copy(0, arrayList.size())));

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

        assertEquals(true, CommonUtil.copyOfRange(arrayList.array(), 0, arrayList.size())[0]);
    }

}
