package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class DifferenceOfTest extends DifferenceTestSupport {

    @Test
    public void testOfArrays() {
        Difference<BooleanList, BooleanList> bools = Difference.of(new boolean[] { true, false, true, false }, new boolean[] { false, false, true });
        assertEquals(BooleanList.of(true, false, false), bools.common());
        assertEquals(BooleanList.of(true), bools.onlyOnLeft());
        assertTrue(bools.onlyOnRight().isEmpty());
        assertTrue(Difference.of(new boolean[] { true, false, true }, new boolean[] { true, false, true }).areEqual());

        Difference<CharList, CharList> chars = Difference.of(new char[] { 'a', 'b', 'c', 'b' }, new char[] { 'b', 'c', 'd', 'c' });
        assertEquals(CharList.of('b', 'c'), chars.common());
        assertEquals(CharList.of('a', 'b'), chars.onlyOnLeft());
        assertEquals(CharList.of('c', 'd'), chars.onlyOnRight());

        Difference<ByteList, ByteList> bytes = Difference.of(new byte[] { 1, 2, 3, 2 }, new byte[] { 2, 3, 4, 3 });
        assertEquals(ByteList.of((byte) 2, (byte) 3), bytes.common());
        assertEquals(ByteList.of((byte) 1, (byte) 2), bytes.onlyOnLeft());
        assertEquals(ByteList.of((byte) 3, (byte) 4), bytes.onlyOnRight());

        Difference<ShortList, ShortList> shorts = Difference.of(new short[] { 1, 2, 3, 2 }, new short[] { 2, 3, 4, 3 });
        assertEquals(ShortList.of((short) 2, (short) 3), shorts.common());
        assertEquals(ShortList.of((short) 1, (short) 2), shorts.onlyOnLeft());
        assertEquals(ShortList.of((short) 3, (short) 4), shorts.onlyOnRight());

        Difference<IntList, IntList> ints = Difference.of(new int[] { 1, 2, 3, 2 }, new int[] { 2, 3, 4, 3 });
        assertEquals(IntList.of(2, 3), ints.common());
        assertEquals(IntList.of(1, 2), ints.onlyOnLeft());
        assertEquals(IntList.of(3, 4), ints.onlyOnRight());

        Difference<LongList, LongList> longs = Difference.of(new long[] { 1L, 2L, 3L, 2L }, new long[] { 2L, 3L, 4L, 3L });
        assertEquals(LongList.of(2L, 3L), longs.common());
        assertEquals(LongList.of(1L, 2L), longs.onlyOnLeft());
        assertEquals(LongList.of(3L, 4L), longs.onlyOnRight());

        Difference<FloatList, FloatList> floats = Difference.of(new float[] { 1.0f, 2.0f, 3.0f, 2.0f }, new float[] { 2.0f, 3.0f, 4.0f, 3.0f });
        assertEquals(FloatList.of(2.0f, 3.0f), floats.common());
        assertEquals(FloatList.of(1.0f, 2.0f), floats.onlyOnLeft());
        assertEquals(FloatList.of(3.0f, 4.0f), floats.onlyOnRight());

        Difference<DoubleList, DoubleList> doubles = Difference.of(new double[] { 1.0, 2.0, 3.0, 2.0 }, new double[] { 2.0, 3.0, 4.0, 3.0 });
        assertEquals(DoubleList.of(2.0, 3.0), doubles.common());
        assertEquals(DoubleList.of(1.0, 2.0), doubles.onlyOnLeft());
        assertEquals(DoubleList.of(3.0, 4.0), doubles.onlyOnRight());

        Difference<List<String>, List<String>> objects = Difference.of(new String[] { "a", "b", "c", "b" }, new String[] { "b", "c", "d", "c" });
        assertEquals(Arrays.asList("b", "c"), objects.common());
        assertEquals(Arrays.asList("a", "b"), objects.onlyOnLeft());
        assertEquals(Arrays.asList("c", "d"), objects.onlyOnRight());
        assertFalse(objects.areEqual());
    }

    @Test
    public void testOfLists() {
        Difference<BooleanList, BooleanList> bools = Difference.of(BooleanList.of(true, false, true, false), BooleanList.of(false, false, true));
        assertEquals(BooleanList.of(true, false, false), bools.common());
        assertEquals(BooleanList.of(true), bools.onlyOnLeft());

        Difference<CharList, CharList> chars = Difference.of(CharList.of('a', 'b', 'c', 'b'), CharList.of('b', 'c', 'd', 'c'));
        assertEquals(CharList.of('b', 'c'), chars.common());
        assertEquals(CharList.of('a', 'b'), chars.onlyOnLeft());
        assertEquals(CharList.of('c', 'd'), chars.onlyOnRight());

        Difference<ByteList, ByteList> bytes = Difference.of(ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 2),
                ByteList.of((byte) 2, (byte) 3, (byte) 4, (byte) 3));
        assertEquals(ByteList.of((byte) 2, (byte) 3), bytes.common());
        Difference<ShortList, ShortList> shorts = Difference.of(ShortList.of((short) 1, (short) 2, (short) 3, (short) 2),
                ShortList.of((short) 2, (short) 3, (short) 4, (short) 3));
        assertEquals(ShortList.of((short) 2, (short) 3), shorts.common());
        Difference<IntList, IntList> ints = Difference.of(IntList.of(1, 2, 3, 2), IntList.of(2, 3, 4, 3));
        assertEquals(IntList.of(2, 3), ints.common());
        Difference<LongList, LongList> longs = Difference.of(LongList.of(1L, 2L, 3L, 2L), LongList.of(2L, 3L, 4L, 3L));
        assertEquals(LongList.of(2L, 3L), longs.common());
        Difference<FloatList, FloatList> floats = Difference.of(FloatList.of(1.0f, 2.0f, 3.0f, 2.0f), FloatList.of(2.0f, 3.0f, 4.0f, 3.0f));
        assertEquals(FloatList.of(2.0f, 3.0f), floats.common());
        Difference<DoubleList, DoubleList> doubles = Difference.of(DoubleList.of(1.0, 2.0, 3.0, 2.0), DoubleList.of(2.0, 3.0, 4.0, 3.0));
        assertEquals(DoubleList.of(2.0, 3.0), doubles.common());
    }

    @Test
    public void testOfCollections() {
        Difference<List<String>, List<String>> lists = Difference.of(Arrays.asList("a", "b", "c", "b"), Arrays.asList("b", "c", "d", "c"));
        assertEquals(Arrays.asList("b", "c"), lists.common());
        assertEquals(Arrays.asList("a", "b"), lists.onlyOnLeft());
        assertEquals(Arrays.asList("c", "d"), lists.onlyOnRight());

        Collection<String> a = Arrays.asList("a", "b", "c", "c");
        Collection<String> b = Arrays.asList("b", "c", "d");
        Difference<List<String>, List<String>> collections = Difference.of(a, b);
        assertEquals(Arrays.asList("b", "c"), collections.common());
        assertEquals(Arrays.asList("a", "c"), collections.onlyOnLeft());
        assertEquals(Collections.singletonList("d"), collections.onlyOnRight());
    }

    @Test
    public void testOf_EmptyAndNull() {
        assertTrue(Difference.of(new boolean[] {}, new boolean[] {}).areEqual());
        assertTrue(Difference.of((boolean[]) null, (boolean[]) null).areEqual());
        assertTrue(Difference.of(new char[] {}, new char[] {}).areEqual());
        assertTrue(Difference.of((char[]) null, (char[]) null).areEqual());
        assertTrue(Difference.of(new byte[] {}, new byte[] {}).areEqual());
        assertTrue(Difference.of((byte[]) null, (byte[]) null).areEqual());
        assertTrue(Difference.of(new short[] {}, new short[] {}).areEqual());
        assertTrue(Difference.of((short[]) null, (short[]) null).areEqual());
        assertTrue(Difference.of(new int[] {}, new int[] {}).areEqual());
        assertTrue(Difference.of((int[]) null, (int[]) null).areEqual());
        assertTrue(Difference.of(new long[] {}, new long[] {}).areEqual());
        assertTrue(Difference.of((long[]) null, (long[]) null).areEqual());
        assertTrue(Difference.of(new float[] {}, new float[] {}).areEqual());
        assertTrue(Difference.of((float[]) null, (float[]) null).areEqual());
        assertTrue(Difference.of(new double[] {}, new double[] {}).areEqual());
        assertTrue(Difference.of((double[]) null, (double[]) null).areEqual());
        assertTrue(Difference.of(new String[] {}, new String[] {}).areEqual());
        assertTrue(Difference.of((String[]) null, (String[]) null).areEqual());
        assertTrue(Difference.of(new ArrayList<String>(), new ArrayList<String>()).areEqual());
        assertTrue(Difference.of((Collection<String>) null, (Collection<String>) null).areEqual());
        assertTrue(Difference.of(BooleanList.of(), BooleanList.of()).areEqual());
        assertTrue(Difference.of((BooleanList) null, (BooleanList) null).areEqual());
        assertTrue(Difference.of(CharList.of(), CharList.of()).areEqual());
        assertTrue(Difference.of((CharList) null, (CharList) null).areEqual());
        assertTrue(Difference.of(ByteList.of(), ByteList.of()).areEqual());
        assertTrue(Difference.of((ByteList) null, (ByteList) null).areEqual());
        assertTrue(Difference.of(ShortList.of(), ShortList.of()).areEqual());
        assertTrue(Difference.of((ShortList) null, (ShortList) null).areEqual());
        assertTrue(Difference.of(IntList.of(), IntList.of()).areEqual());
        assertTrue(Difference.of((IntList) null, (IntList) null).areEqual());
        assertTrue(Difference.of(LongList.of(), LongList.of()).areEqual());
        assertTrue(Difference.of((LongList) null, (LongList) null).areEqual());
        assertTrue(Difference.of(FloatList.of(), FloatList.of()).areEqual());
        assertTrue(Difference.of((FloatList) null, (FloatList) null).areEqual());
        assertTrue(Difference.of(DoubleList.of(), DoubleList.of()).areEqual());
        assertTrue(Difference.of((DoubleList) null, (DoubleList) null).areEqual());

        Difference<List<String>, List<String>> oneEmpty = Difference.of(Arrays.asList("a", "b"), new ArrayList<>());
        assertEquals(Arrays.asList("a", "b"), oneEmpty.onlyOnLeft());
        assertTrue(oneEmpty.onlyOnRight().isEmpty());
        assertEquals(Arrays.asList("b"), Difference.of(new ArrayList<>(), Arrays.asList("b")).onlyOnRight());
        assertEquals(Arrays.asList("a"), Difference.of(Arrays.asList("a"), (Collection<String>) null).onlyOnLeft());
    }

    @Test
    public void testOf_OneNull() {
        BooleanList bools = BooleanList.of(true, false);
        assertEquals(bools, Difference.of(bools, (BooleanList) null).onlyOnLeft());
        assertEquals(bools, Difference.of((BooleanList) null, bools).onlyOnRight());
        CharList chars = CharList.of('a', 'b');
        assertEquals(chars, Difference.of(chars, (CharList) null).onlyOnLeft());
        assertEquals(chars, Difference.of((CharList) null, chars).onlyOnRight());
        ByteList bytes = ByteList.of((byte) 1, (byte) 2);
        assertEquals(bytes, Difference.of(bytes, (ByteList) null).onlyOnLeft());
        assertEquals(bytes, Difference.of((ByteList) null, bytes).onlyOnRight());
        ShortList shorts = ShortList.of((short) 1, (short) 2);
        assertEquals(shorts, Difference.of(shorts, (ShortList) null).onlyOnLeft());
        assertEquals(shorts, Difference.of((ShortList) null, shorts).onlyOnRight());
        IntList ints = IntList.of(1, 2);
        assertEquals(ints, Difference.of(ints, (IntList) null).onlyOnLeft());
        assertEquals(ints, Difference.of((IntList) null, ints).onlyOnRight());
        LongList longs = LongList.of(1L, 2L);
        assertEquals(longs, Difference.of(longs, (LongList) null).onlyOnLeft());
        assertEquals(longs, Difference.of((LongList) null, longs).onlyOnRight());
        FloatList floats = FloatList.of(1.0f, 2.0f);
        assertEquals(floats, Difference.of(floats, (FloatList) null).onlyOnLeft());
        assertEquals(floats, Difference.of((FloatList) null, floats).onlyOnRight());
        DoubleList doubles = DoubleList.of(1.0, 2.0);
        assertEquals(doubles, Difference.of(doubles, (DoubleList) null).onlyOnLeft());
        assertEquals(doubles, Difference.of((DoubleList) null, doubles).onlyOnRight());
        String[] objects = { "a", "b" };
        assertEquals(Arrays.asList("a", "b"), Difference.of(objects, (String[]) null).onlyOnLeft());
        assertEquals(Arrays.asList("a", "b"), Difference.of((String[]) null, objects).onlyOnRight());
    }

    // ---- perf review 2026-09-26 G032 begin ----
    private static <T> void assertSameDifferenceAsCollections(final List<T> left, final List<T> right,
            final Difference<? extends PrimitiveList<?, ?, ?>, ? extends PrimitiveList<?, ?, ?>> actual) {
        final Difference<List<T>, List<T>> expected = Difference.of(left, right);
        assertEquals(expected.common(), actual.common().boxed());
        assertEquals(expected.onlyOnLeft(), actual.onlyOnLeft().boxed());
        assertEquals(expected.onlyOnRight(), actual.onlyOnRight().boxed());
        assertEquals(expected.areEqual(), actual.areEqual());
    }

    // G032-01: boolean occurrence counting without a Multiset - same partition as the Collection overload
    @Test
    public void testOfBooleanList_countingMatchesCollectionOverload() {
        final boolean[][] samples = { {}, { true }, { false }, { true, true, true }, { false, false }, { true, false, true, false, false },
                { false, true, true } };

        for (final boolean[] x : samples) {
            for (final boolean[] y : samples) {
                final BooleanList a = BooleanList.of(x);
                final BooleanList b = BooleanList.of(y);
                assertSameDifferenceAsCollections(a.boxed(), b.boxed(), Difference.of(a, b));
            }
        }

        final java.util.Random random = new java.util.Random(32);

        for (int round = 0; round < 200; round++) {
            final BooleanList a = new BooleanList();
            final BooleanList b = new BooleanList();

            for (int i = random.nextInt(20); i > 0; i--) {
                a.add(random.nextInt(4) == 0);
            }

            for (int i = random.nextInt(20); i > 0; i--) {
                b.add(random.nextBoolean());
            }

            assertSameDifferenceAsCollections(a.boxed(), b.boxed(), Difference.of(a, b));
        }
    }

    // G032-01: byte occurrence counting indexed by the unsigned value - negative bytes and extremes included
    @Test
    public void testOfByteList_countingMatchesCollectionOverload() {
        final ByteList extremes = ByteList.of(Byte.MIN_VALUE, (byte) -1, (byte) 0, (byte) 1, Byte.MAX_VALUE, (byte) -1, Byte.MIN_VALUE);
        final ByteList other = ByteList.of((byte) -1, Byte.MAX_VALUE, (byte) 5, Byte.MIN_VALUE, (byte) -1, (byte) -1);
        assertSameDifferenceAsCollections(extremes.boxed(), other.boxed(), Difference.of(extremes, other));
        assertSameDifferenceAsCollections(other.boxed(), extremes.boxed(), Difference.of(other, extremes));
        assertSameDifferenceAsCollections(extremes.boxed(), extremes.boxed(), Difference.of(extremes, extremes.copy()));

        final Difference<ByteList, ByteList> diff = Difference.of(ByteList.of((byte) -128, (byte) 127, (byte) -128), ByteList.of((byte) 127, (byte) -128));
        assertEquals(ByteList.of((byte) -128, (byte) 127), diff.common());
        assertEquals(ByteList.of((byte) -128), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());

        final java.util.Random random = new java.util.Random(33);

        for (int round = 0; round < 200; round++) {
            final ByteList a = new ByteList();
            final ByteList b = new ByteList();
            final int spread = 1 + random.nextInt(256);

            for (int i = random.nextInt(30); i > 0; i--) {
                a.add((byte) (random.nextInt(spread) - 128));
            }

            for (int i = random.nextInt(30); i > 0; i--) {
                b.add((byte) (random.nextInt(spread) - 128));
            }

            assertSameDifferenceAsCollections(a.boxed(), b.boxed(), Difference.of(a, b));
        }
    }

    // G032-01: char counting-array path (dense range; left values below/above the right-hand range) and Multiset fallback (sparse range)
    @Test
    public void testOfCharList_countingMatchesCollectionOverload() {
        final CharList dense = CharList.of('a', 'c', 'e', 'c', 'a');
        final CharList probe = CharList.of('0', 'a', 'z', 'c', 'c', 'c', (char) 0, (char) 0xFFFF, 'e');
        assertSameDifferenceAsCollections(probe.boxed(), dense.boxed(), Difference.of(probe, dense));
        assertSameDifferenceAsCollections(dense.boxed(), probe.boxed(), Difference.of(dense, probe));

        // two values 65535 apart in a two-element list: Multiset fallback
        final CharList sparse = CharList.of((char) 0, (char) 0xFFFF);
        final CharList sparseLeft = CharList.of((char) 0xFFFF, 'x', (char) 0, (char) 0);
        final Difference<CharList, CharList> sparseDiff = Difference.of(sparseLeft, sparse);
        assertEquals(CharList.of((char) 0xFFFF, (char) 0), sparseDiff.common());
        assertEquals(CharList.of('x', (char) 0), sparseDiff.onlyOnLeft());
        assertTrue(sparseDiff.onlyOnRight().isEmpty());
        assertSameDifferenceAsCollections(sparseLeft.boxed(), sparse.boxed(), sparseDiff);
        assertSameDifferenceAsCollections(sparse.boxed(), sparseLeft.boxed(), Difference.of(sparse, sparseLeft));

        // threshold boundary for a small list: a range of exactly 256 values (counting array) and 257 values (Multiset)
        final CharList range256 = CharList.of((char) 1000, (char) 1255, (char) 1000);
        final CharList range257 = CharList.of((char) 1000, (char) 1256, (char) 1256);
        final CharList mixed = CharList.of((char) 1256, (char) 1000, (char) 999, (char) 1255, (char) 1257, (char) 1000, (char) 1000);
        assertSameDifferenceAsCollections(mixed.boxed(), range256.boxed(), Difference.of(mixed, range256));
        assertSameDifferenceAsCollections(mixed.boxed(), range257.boxed(), Difference.of(mixed, range257));

        final java.util.Random random = new java.util.Random(34);

        for (int round = 0; round < 300; round++) {
            final CharList a = new CharList();
            final CharList b = new CharList();
            final int spread = round % 3 == 0 ? 65536 : 1 + random.nextInt(round % 3 == 1 ? 40 : 600);
            final int base = random.nextInt(65536 - spread + 1);

            for (int i = random.nextInt(40); i > 0; i--) {
                a.add((char) (base + random.nextInt(spread)));
            }

            for (int i = random.nextInt(40); i > 0; i--) {
                b.add((char) (base + random.nextInt(spread)));
            }

            assertSameDifferenceAsCollections(a.boxed(), b.boxed(), Difference.of(a, b));
        }
    }

    // G032-01: short counting-array path (negative values, extremes, size-widened threshold) and Multiset fallback (sparse range)
    @Test
    public void testOfShortList_countingMatchesCollectionOverload() {
        final ShortList dense = ShortList.of((short) -3, (short) 0, (short) 4, (short) -3, (short) 4, (short) 4);
        final ShortList probe = ShortList.of((short) 4, (short) -4, (short) 5, (short) -3, (short) 4, Short.MIN_VALUE, Short.MAX_VALUE, (short) 4,
                (short) 4);
        assertSameDifferenceAsCollections(probe.boxed(), dense.boxed(), Difference.of(probe, dense));
        assertSameDifferenceAsCollections(dense.boxed(), probe.boxed(), Difference.of(dense, probe));

        final ShortList sparse = ShortList.of(Short.MAX_VALUE, Short.MIN_VALUE, Short.MAX_VALUE);
        final ShortList sparseLeft = ShortList.of(Short.MIN_VALUE, Short.MIN_VALUE, (short) 0, Short.MAX_VALUE);
        final Difference<ShortList, ShortList> sparseDiff = Difference.of(sparseLeft, sparse);
        assertEquals(ShortList.of(Short.MIN_VALUE, Short.MAX_VALUE), sparseDiff.common());
        assertEquals(ShortList.of(Short.MIN_VALUE, (short) 0), sparseDiff.onlyOnLeft());
        assertEquals(ShortList.of(Short.MAX_VALUE), sparseDiff.onlyOnRight());
        assertSameDifferenceAsCollections(sparseLeft.boxed(), sparse.boxed(), sparseDiff);
        assertSameDifferenceAsCollections(sparse.boxed(), sparseLeft.boxed(), Difference.of(sparse, sparseLeft));

        // a right-hand list larger than 256 elements widens the dense threshold to its size
        final ShortList large = new ShortList();

        for (int i = 0; i < 1000; i++) {
            large.add((short) ((i * 7) % 900 - 450));
        }

        final ShortList largeLeft = new ShortList();

        for (int i = 0; i < 1200; i++) {
            largeLeft.add((short) ((i * 11) % 1000 - 500));
        }

        assertSameDifferenceAsCollections(largeLeft.boxed(), large.boxed(), Difference.of(largeLeft, large));
        assertSameDifferenceAsCollections(large.boxed(), largeLeft.boxed(), Difference.of(large, largeLeft));

        final java.util.Random random = new java.util.Random(35);

        for (int round = 0; round < 300; round++) {
            final ShortList a = new ShortList();
            final ShortList b = new ShortList();
            final int spread = round % 3 == 0 ? 65536 : 1 + random.nextInt(round % 3 == 1 ? 40 : 600);
            final int base = Short.MIN_VALUE + random.nextInt(65536 - spread + 1);

            for (int i = random.nextInt(40); i > 0; i--) {
                a.add((short) (base + random.nextInt(spread)));
            }

            for (int i = random.nextInt(40); i > 0; i--) {
                b.add((short) (base + random.nextInt(spread)));
            }

            assertSameDifferenceAsCollections(a.boxed(), b.boxed(), Difference.of(a, b));
        }
    }
    // ---- perf review 2026-09-26 G032 end ----
}
