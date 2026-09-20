package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Extra PrimitiveList / IntList pins that are not already covered by IntList* tests:
 * backing-array sharing, addAll of self, successive removeRange leftovers, and
 * compactAfterRemovingIndices.
 */
public class PrimitiveListTest extends TestBase {

    @Test
    public void testSymmetricDifferencePreservesReceiverPrefixAndOtherOccurrenceOrder() {
        final IntList receiver = IntList.of(1, 9);
        final IntList other = IntList.of(1, 2, 1);
        assertLogical(receiver.symmetricDifference(other), 9, 1, 2);
        assertLogical(receiver.symmetricDifference(other.toArray()), 9, 1, 2);
        assertLogical(receiver, 1, 9);
        assertLogical(other, 1, 2, 1);
    }

    @Test
    public void testInternalArray() {
        final IntList list = IntList.of(1, 2, 3);
        final int[] internal = list.internalArray();
        assertTrue(internal.length >= 3);
        assertArrayEquals(new int[] { 1, 2, 3 }, Arrays.copyOf(internal, list.size()));
    }

    @Test
    public void testOf_SharesBackingArray() {
        final int[] backing = { 1, 2, 3, 4, 5 };
        final IntList list = IntList.of(backing, 3);
        assertSame(backing, list.internalArray());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.toArray());
        list.set(0, 9);
        assertEquals(9, backing[0]);
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(backing, 6));
    }

    @Test
    public void testAddAll_SelfAndAtIndex() {
        final int[] backing = { 1, 2, 3 };
        final IntList list = IntList.of(backing, 2);
        list.set(1, 4);
        list.add(5);
        list.add(1, 6);
        assertLogical(list, 1, 6, 4, 5);

        assertTrue(list.addAll(list));
        assertLogical(list, 1, 6, 4, 5, 1, 6, 4, 5);

        assertTrue(list.addAll(2, list));
        assertLogical(list, 1, 6, 1, 6, 4, 5, 1, 6, 4, 5, 4, 5, 1, 6, 4, 5);
    }

    @Test
    public void testAddAll_ArrayAtIndex() {
        final IntList list = IntList.of(1, 2, 3, 4, 5);
        assertTrue(list.addAll(2, new int[] { 10, 11 }));
        assertLogical(list, 1, 2, 10, 11, 3, 4, 5);

        assertFalse(list.addAll(0, new int[] {}));
        assertFalse(list.addAll((int[]) null));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(-1, new int[] { 1 }));
        assertThrows(IndexOutOfBoundsException.class, () -> list.addAll(list.size() + 1, new int[] { 1 }));
    }

    @Test
    public void testRemoveRange_SuccessiveLeavesBackingPrefix() {
        final ByteList list = ByteList.range((byte) 0, (byte) 10);
        final int capacity = list.internalArray().length;

        list.removeRange(1, 4);
        assertLogical(list, (byte) 0, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9);
        assertEquals(capacity, list.internalArray().length);

        list.removeRange(1, 4);
        assertLogical(list, (byte) 0, (byte) 7, (byte) 8, (byte) 9);

        list.removeRange(1, 4);
        assertLogical(list, (byte) 0);
        assertEquals(capacity, list.internalArray().length);
    }

    @Test
    public void testRemoveRange_SameIndexAndInvalid() {
        final IntList list = IntList.of(1, 2, 3, 4, 5);
        list.removeRange(2, 2);
        assertLogical(list, 1, 2, 3, 4, 5);
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeRange(2, 1));
    }

    @Test
    public void testMoveRange_ToStartAndEnd() {
        final ByteList toStart = ByteList.range((byte) 0, (byte) 10);
        toStart.moveRange(1, 4, 0);
        assertLogical(toStart, (byte) 1, (byte) 2, (byte) 3, (byte) 0, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9);

        final ByteList toEnd = ByteList.range((byte) 0, (byte) 10);
        toEnd.moveRange(1, 4, 7);
        assertLogical(toEnd, (byte) 0, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9, (byte) 1, (byte) 2, (byte) 3);
    }

    @Test
    public void testMoveRange_RejectsPositionsBeyondLogicalSize() {
        final BooleanList booleans = new BooleanList(8);
        booleans.addAll(new boolean[] { true, false, true });
        assertThrows(IndexOutOfBoundsException.class, () -> booleans.moveRange(0, 1, 3));
        assertArrayEquals(new boolean[] { true, false, true }, booleans.toArray());

        final CharList chars = new CharList(8);
        chars.addAll(new char[] { 'a', 'b', 'c' });
        assertThrows(IndexOutOfBoundsException.class, () -> chars.moveRange(0, 1, 3));
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, chars.toArray());

        final ByteList bytes = new ByteList(8);
        bytes.addAll(new byte[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> bytes.moveRange(0, 1, 3));
        assertArrayEquals(new byte[] { 1, 2, 3 }, bytes.toArray());

        final ShortList shorts = new ShortList(8);
        shorts.addAll(new short[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> shorts.moveRange(0, 1, 3));
        assertArrayEquals(new short[] { 1, 2, 3 }, shorts.toArray());

        final IntList ints = new IntList(8);
        ints.addAll(new int[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> ints.moveRange(0, 1, 3));
        assertArrayEquals(new int[] { 1, 2, 3 }, ints.toArray());

        final LongList longs = new LongList(8);
        longs.addAll(new long[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> longs.moveRange(0, 1, 3));
        assertArrayEquals(new long[] { 1, 2, 3 }, longs.toArray());

        final FloatList floats = new FloatList(8);
        floats.addAll(new float[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> floats.moveRange(0, 1, 3));
        assertArrayEquals(new float[] { 1, 2, 3 }, floats.toArray());

        final DoubleList doubles = new DoubleList(8);
        doubles.addAll(new double[] { 1, 2, 3 });
        assertThrows(IndexOutOfBoundsException.class, () -> doubles.moveRange(0, 1, 3));
        assertArrayEquals(new double[] { 1, 2, 3 }, doubles.toArray());
    }

    @Test
    public void testReplaceRange_EmptyShrinkAndExpandLeaveBackingPrefix() {
        ByteList list = ByteList.range((byte) 0, (byte) 10);
        list.replaceRange(1, 3, new byte[] {});
        assertLogical(list, (byte) 0, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9);

        list = ByteList.range((byte) 0, (byte) 10);
        list.replaceRange(1, 3, new byte[] { 9 });
        assertLogical(list, (byte) 0, (byte) 9, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9);

        list = ByteList.range((byte) 0, (byte) 10);
        list.replaceRange(1, 3, new byte[] { 9, 9 });
        assertLogical(list, (byte) 0, (byte) 9, (byte) 9, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9);

        list = ByteList.range((byte) 0, (byte) 10);
        list.replaceRange(1, 3, new byte[] { 9, 9, 9 });
        assertLogical(list, (byte) 0, (byte) 9, (byte) 9, (byte) 9, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8, (byte) 9);
    }

    @Test
    public void testRemoveDuplicates_BooleanAndByteLeaveBackingPrefix() {
        final ByteList bytes = ByteList.of((byte) 0, (byte) 1, (byte) 2, (byte) 1, (byte) 1, (byte) 3, (byte) 2);
        assertTrue(bytes.removeDuplicates());
        assertLogical(bytes, (byte) 0, (byte) 1, (byte) 2, (byte) 3);

        final BooleanList mixed = BooleanList.of(true, false, true, true);
        assertTrue(mixed.removeDuplicates());
        assertLogical(mixed, true, false);

        final BooleanList leadingFalse = BooleanList.of(false, false, true, false);
        assertTrue(leadingFalse.removeDuplicates());
        assertLogical(leadingFalse, false, true);

        final BooleanList allFalse = BooleanList.of(false, false, false);
        assertTrue(allFalse.removeDuplicates());
        assertLogical(allFalse, false);
    }

    @Test
    public void testCopy_NegativeStep_EmptyPrimitiveLists() {
        assertTrue(BooleanList.of().copy(0, -1, -1).isEmpty());
        assertTrue(ByteList.of().copy(0, -1, -1).isEmpty());
        assertTrue(CharList.of().copy(0, -1, -1).isEmpty());
        assertTrue(ShortList.of().copy(0, -1, -1).isEmpty());
        assertTrue(IntList.of().copy(0, -1, -1).isEmpty());
        assertTrue(LongList.of().copy(0, -1, -1).isEmpty());
        assertTrue(FloatList.of().copy(0, -1, -1).isEmpty());
        assertTrue(DoubleList.of().copy(0, -1, -1).isEmpty());
    }

    @Test
    public void testOf_BoxedNullsBecomeZero() {
        assertArrayEquals(new int[] { 1, 0, 3 }, IntList.of(CommonUtil.toIntArray(CommonUtil.toList(1, null, 3))).toArray());
        assertArrayEquals(new long[] { 1L, 0L, 3L }, LongList.of(CommonUtil.toLongArray(CommonUtil.toList(1L, null, 3L))).toArray());
        assertArrayEquals(new float[] { 1f, 0f, 3f }, FloatList.of(CommonUtil.toFloatArray(CommonUtil.toList(1f, null, 3f))).toArray());
        assertArrayEquals(new double[] { 1d, 0d, 3d }, DoubleList.of(CommonUtil.toDoubleArray(CommonUtil.toList(1d, null, 3d))).toArray());
    }

    @Test
    public void testTrimToSize_ReducesBackingLength() {
        final IntList list = new IntList(100);
        list.add(1);
        list.add(2);
        list.add(3);
        assertTrue(list.internalArray().length >= 100);
        list.trimToSize();
        assertSame(list, list.trimToSize());
        assertArrayEquals(new int[] { 1, 2, 3 }, list.internalArray());
    }

    @Test
    public void testCompactAfterRemovingIndices_SupportsPrimitiveArraysAndValidatesBeforeMutation() {
        final int[] ints = { 10, 20, 30, 40, 50, 99 };
        assertEquals(3, PrimitiveList.compactAfterRemovingIndices(ints, 5, new int[] { 3, 1, 3, 1 }));
        assertArrayEquals(new int[] { 10, 30, 50 }, Arrays.copyOf(ints, 3));

        final boolean[] booleans = { true, false, true, false };
        assertEquals(2, PrimitiveList.compactAfterRemovingIndices(booleans, 4, new int[] { 2, 0 }));
        assertArrayEquals(new boolean[] { false, false }, Arrays.copyOf(booleans, 2));

        final long[] invalid = { 1L, 2L, 3L };
        assertThrows(IndexOutOfBoundsException.class, () -> PrimitiveList.compactAfterRemovingIndices(invalid, 3, new int[] { 1, 3 }));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, invalid);
    }

    private static void assertLogical(final IntList list, final int... expected) {
        assertArrayEquals(expected, list.toArray());
        assertTrue(list.internalArray().length >= list.size());
        assertArrayEquals(expected, Arrays.copyOf(list.internalArray(), list.size()));
    }

    private static void assertLogical(final ByteList list, final byte... expected) {
        assertArrayEquals(expected, list.toArray());
        assertTrue(list.internalArray().length >= list.size());
        assertArrayEquals(expected, Arrays.copyOf(list.internalArray(), list.size()));
    }

    /**
     * Doc pin for the class javadoc's "Search Operations" bullet: contains/indexOf/lastIndexOf exist on all
     * eight concrete lists, but {@code BooleanList} is the one with no {@code binarySearch}.
     */
    @Test
    public void testSearchOperationsBulletHoldsForEveryConcreteList() {
        final Class<?>[] concreteLists = { BooleanList.class, ByteList.class, CharList.class, ShortList.class, IntList.class, LongList.class, FloatList.class,
                DoubleList.class };

        for (final Class<?> cls : concreteLists) {
            assertTrue(hasMethodNamed(cls, "contains"), cls.getSimpleName());
            assertTrue(hasMethodNamed(cls, "indexOf"), cls.getSimpleName());
            assertTrue(hasMethodNamed(cls, "lastIndexOf"), cls.getSimpleName());
            assertEquals(cls != BooleanList.class, hasMethodNamed(cls, "binarySearch"), cls.getSimpleName() + ".binarySearch");
        }
    }

    private static boolean hasMethodNamed(final Class<?> cls, final String name) {
        return Arrays.stream(cls.getMethods()).anyMatch(m -> name.equals(m.getName()));
    }

    private static void assertLogical(final BooleanList list, final boolean... expected) {
        assertArrayEquals(expected, list.toArray());
        assertTrue(list.internalArray().length >= list.size());
        assertArrayEquals(expected, Arrays.copyOf(list.internalArray(), list.size()));
    }
}
