package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Covers the fixes applied after the 2026-09-02 review of {@code PrimitiveList} and its eight
 * concrete subclasses. Each test names the finding it pins.
 */
public class PrimitiveListRegressionATest extends TestBase {

    // ---------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------

    private static byte[] serialize(final Serializable o) throws Exception {
        final java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();

        try (ObjectOutputStream oo = new ObjectOutputStream(bo)) {
            oo.writeObject(o);
        }

        return bo.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T deserialize(final byte[] bytes) throws Exception {
        try (ObjectInputStream oi = new ObjectInputStream(new ByteArrayInputStream(bytes))) {
            return (T) oi.readObject();
        }
    }

    private static boolean containsBigEndianInt(final byte[] haystack, final int needle) {
        final byte[] n = { (byte) (needle >>> 24), (byte) (needle >>> 16), (byte) (needle >>> 8), (byte) needle };

        outer: for (int i = 0; i + 4 <= haystack.length; i++) {
            for (int k = 0; k < 4; k++) {
                if (haystack[i + k] != n[k]) {
                    continue outer;
                }
            }

            return true;
        }

        return false;
    }

    // ---------------------------------------------------------------------------------------
    // B3 / T1 - serialization writes only the live prefix and validates on read
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_B3_serialization_roundTrip_allTypes() throws Exception {
        assertEquals(BooleanList.of(true, false, true), deserialize(serialize(BooleanList.of(true, false, true))));
        assertEquals(CharList.of('a', 'b'), deserialize(serialize(CharList.of('a', 'b'))));
        assertEquals(ByteList.of((byte) 1, (byte) -2), deserialize(serialize(ByteList.of((byte) 1, (byte) -2))));
        assertEquals(ShortList.of((short) 1, (short) -2), deserialize(serialize(ShortList.of((short) 1, (short) -2))));
        assertEquals(IntList.of(1, -2, 3), deserialize(serialize(IntList.of(1, -2, 3))));
        assertEquals(LongList.of(1L, Long.MIN_VALUE), deserialize(serialize(LongList.of(1L, Long.MIN_VALUE))));
        assertEquals(FloatList.of(1f, Float.NaN, -0.0f), deserialize(serialize(FloatList.of(1f, Float.NaN, -0.0f))));
        assertEquals(DoubleList.of(1d, Double.NaN, -0.0d), deserialize(serialize(DoubleList.of(1d, Double.NaN, -0.0d))));
    }

    @Test
    public void test_B3_serialization_roundTrip_empty() throws Exception {
        assertEquals(new IntList(), deserialize(serialize(new IntList())));
        assertTrue(((IntList) deserialize(serialize(new IntList(100)))).isEmpty());
        assertEquals(new BooleanList(), deserialize(serialize(new BooleanList())));
        assertEquals(new DoubleList(), deserialize(serialize(new DoubleList())));
    }

    @Test
    public void test_B3_capacityIsNotSerialized() throws Exception {
        final IntList spacious = new IntList(1000);
        spacious.addAll(new int[] { 1, 2, 3 });

        final IntList exact = IntList.of(1, 2, 3);

        // the two lists hold the same elements, so they must serialize to the same bytes
        assertArrayEquals(serialize(exact), serialize(spacious));

        final IntList back = deserialize(serialize(spacious));
        assertEquals(exact, back);
        assertEquals(3, back.internalArray().length);
    }

    @Test
    public void test_B3_unusedTailIsNotDisclosed() throws Exception {
        // an array-backed list is a window onto the caller's buffer; the tail must not be written out
        final int[] backing = { 1, 2, 0xCAFEBABE, 0xDEADBEEF };
        final IntList view = new IntList(backing, 2);

        final byte[] bytes = serialize(view);
        assertFalse(containsBigEndianInt(bytes, 0xCAFEBABE));
        assertFalse(containsBigEndianInt(bytes, 0xDEADBEEF));

        final IntList back = deserialize(bytes);
        assertEquals(IntList.of(1, 2), back);
        assertArrayEquals(new int[] { 1, 2 }, back.internalArray());
    }

    @Test
    public void test_B3_trimToSizeDoesNotChangeTheSerializedForm() throws Exception {
        final IntList spacious = new IntList(64);
        spacious.addAll(new int[] { 5, 6, 7 });

        final byte[] before = serialize(spacious);
        final byte[] after = serialize(spacious.copy().trimToSize());

        assertArrayEquals(after, before);
    }

    @Test
    public void test_B3_corruptStreamIsRejected() throws Exception {
        final byte[] good = serialize(IntList.of(10, 20, 30));
        final byte[] bad = good.clone();

        // bump the primitive int field 'size' from 3 to 99; it is written just before TC_ARRAY (0x75)
        boolean patched = false;

        for (int i = 0; i + 5 <= bad.length; i++) {
            if (bad[i] == 0 && bad[i + 1] == 0 && bad[i + 2] == 0 && bad[i + 3] == 3 && (bad[i + 4] & 0xFF) == 0x75) {
                bad[i + 3] = 99;
                patched = true;
                break;
            }
        }

        assertTrue(patched, "test setup: could not locate the serialized 'size' field");

        final Exception e = assertThrows(InvalidObjectException.class, () -> deserialize(bad));
        assertTrue(e.getMessage().contains("size=99"), e.getMessage());
    }

    @Test
    public void test_B3_streamsWrittenBeforeTheFixAreStillReadable() throws Exception {
        // captured from the pre-fix build: default serialization, size 3 inside a capacity-8 array
        final String oldIntList = "rO0ABXNyAB9jb20ubGFuZGF3bi5hYmFjdXMudXRpbC5JbnRMaXN0eDTNhLbOhlACAAJJAARzaXplWwALZWxlbWVudERhdGF0"
                + "AAJbSXhyACVjb20ubGFuZGF3bi5hYmFjdXMudXRpbC5QcmltaXRpdmVMaXN0FOIR93r7N8MCAAB4cAAAAAN1cgACW0lNumAmduqy"
                + "pQIAAHhwAAAACAAAAAoAAAAUAAAAHgAAAAAAAAAAAAAAAAAAAAAAAAAA";
        final IntList back = deserialize(Base64.getDecoder().decode(oldIntList));
        assertEquals(IntList.of(10, 20, 30), back);

        final String oldDoubleList = "rO0ABXNyACJjb20ubGFuZGF3bi5hYmFjdXMudXRpbC5Eb3VibGVMaXN0CqHwKXI19wUCAAJJAARzaXplWwALZWxlbWVu"
                + "dERhdGF0AAJbRHhyACVjb20ubGFuZGF3bi5hYmFjdXMudXRpbC5QcmltaXRpdmVMaXN0FOIR93r7N8MCAAB4cAAAAAJ1cgACW0Q+"
                + "powUq2NaHgIAAHhwAAAABj/4AAAAAAAAf/gAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA==";
        final DoubleList backD = deserialize(Base64.getDecoder().decode(oldDoubleList));
        assertEquals(DoubleList.of(1.5d, Double.NaN), backD);
    }

    @Test
    public void test_B3_addingWriteObjectIsACompatibleFormatChange() throws Exception {
        // Declaring writeObject sets SC_WRITE_METHOD in the class descriptor and appends a
        // TC_ENDBLOCKDATA marker, so the bytes are NOT identical to the old default form - that is the
        // spec's compatible-evolution path. What matters is that both directions still read, which the
        // pinned pre-fix stream above covers for old -> new; new -> old was verified out of tree by
        // deserializing a stream from this build with a pre-fix build of the same classes.
        final byte[] bytes = serialize(IntList.of(10, 20, 30));

        assertEquals((byte) 0x78, bytes[bytes.length - 1], "stream should end with TC_ENDBLOCKDATA");
        assertEquals(IntList.of(10, 20, 30), deserialize(bytes));

        // serialVersionUID must not have moved - a pre-fix reader matches on it
        assertEquals(8661773953226671696L, java.io.ObjectStreamClass.lookup(IntList.class).getSerialVersionUID());
    }

    // ---------------------------------------------------------------------------------------
    // D1 - removeIf: no allocation when nothing matches, strong exception safety
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_D1_removeIf_noMatchLeavesTheListIdentical() {
        final IntList list = IntList.of(1, 3, 5, 7);
        final int[] backingBefore = list.internalArray();

        assertFalse(list.removeIf(v -> v % 2 == 0));
        assertEquals(IntList.of(1, 3, 5, 7), list);
        // no reallocation of the backing array either
        assertTrue(backingBefore == list.internalArray());
    }

    @Test
    public void test_D1_removeIf_basicShapes() {
        IntList list = IntList.of(1, -2, 3, -4, 5);
        assertTrue(list.removeIf(v -> v < 0));
        assertEquals(IntList.of(1, 3, 5), list);

        list = IntList.of(1, 2, 3);
        assertTrue(list.removeIf(v -> true));
        assertTrue(list.isEmpty());
        assertEquals(0, list.size());

        list = IntList.of(1, 2, 3);
        assertTrue(list.removeIf(v -> v == 1)); // first element only
        assertEquals(IntList.of(2, 3), list);

        list = IntList.of(1, 2, 3);
        assertTrue(list.removeIf(v -> v == 3)); // last element only
        assertEquals(IntList.of(1, 2), list);

        assertFalse(new IntList().removeIf(v -> true));
        assertFalse(new IntList().removeIf(v -> false));
    }

    @Test
    public void test_D1_removeIf_acrossTheBitsetWordBoundary() {
        // the marker set is a long[]; make sure bit 63/64/65 and multi-word runs behave
        for (final int n : new int[] { 1, 63, 64, 65, 127, 128, 129, 200 }) {
            final int[] base = new int[n];

            for (int i = 0; i < n; i++) {
                base[i] = i;
            }

            IntList list = IntList.of(base.clone());
            assertTrue(list.removeIf(v -> v % 2 == 0));
            for (int i = 0; i < list.size(); i++) {
                assertEquals(1 + 2 * i, list.get(i), "n=" + n);
            }
            assertEquals((n + 1) / 2, n - list.size(), "n=" + n);

            list = IntList.of(base.clone());
            assertTrue(list.removeIf(v -> v >= 64) == (n > 64));
            assertEquals(Math.min(n, 64), list.size(), "n=" + n);

            list = IntList.of(base.clone());
            assertTrue(list.removeIf(v -> v < 64));
            assertEquals(Math.max(0, n - 64), list.size(), "n=" + n);

            list = IntList.of(base.clone());
            assertTrue(list.removeIf(v -> true));
            assertTrue(list.isEmpty(), "n=" + n);

            list = IntList.of(base.clone());
            assertFalse(list.removeIf(v -> false));
            assertArrayEquals(base, list.toArray(), "n=" + n);
        }
    }

    @Test
    public void test_D1_removeIf_isExceptionSafe() {
        final IntList list = IntList.of(1, 2, 3, 4, 5);

        // the predicate matches index 1 and then throws at index 2
        assertThrows(IllegalStateException.class, () -> list.removeIf(v -> {
            if (v == 3) {
                throw new IllegalStateException("boom");
            }
            return v % 2 == 0;
        }));

        assertEquals(IntList.of(1, 2, 3, 4, 5), list);

        // throwing before any match must be equally safe
        assertThrows(IllegalStateException.class, () -> list.removeIf(v -> {
            throw new IllegalStateException("boom");
        }));

        assertEquals(IntList.of(1, 2, 3, 4, 5), list);
    }

    @Test
    public void test_D1_removeIf_visitsEveryElementExactlyOnce() {
        final AtomicInteger calls = new AtomicInteger();
        final IntList list = IntList.of(1, 2, 3, 4, 5, 6, 7);

        list.removeIf(v -> {
            calls.incrementAndGet();
            return v % 3 == 0;
        });

        assertEquals(7, calls.get());
        assertEquals(IntList.of(1, 2, 4, 5, 7), list);
    }

    @Test
    public void test_D1_removeIf_nullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> IntList.of(1).removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new IntList().removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new BooleanList().removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new CharList().removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new ByteList().removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new ShortList().removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new LongList().removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new FloatList().removeIf(null));
        assertThrows(IllegalArgumentException.class, () -> new DoubleList().removeIf(null));
    }

    @Test
    public void test_D1_removeIf_allSiblingTypes() {
        final BooleanList bl = BooleanList.of(true, false, true, true, false);
        assertTrue(bl.removeIf(v -> v));
        assertEquals(BooleanList.of(false, false), bl);

        final CharList cl = CharList.of('a', 'b', 'c', 'd');
        assertTrue(cl.removeIf(v -> v % 2 == 0));
        assertEquals(CharList.of('a', 'c'), cl);

        final ByteList byl = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        assertTrue(byl.removeIf(v -> v > 2));
        assertEquals(ByteList.of((byte) 1, (byte) 2), byl);

        final ShortList sl = ShortList.of((short) 1, (short) 2, (short) 3);
        assertTrue(sl.removeIf(v -> v == 1));
        assertEquals(ShortList.of((short) 2, (short) 3), sl);

        final LongList ll = LongList.of(1L, 2L, 3L, 4L, 5L);
        assertTrue(ll.removeIf(v -> v % 2 == 1));
        assertEquals(LongList.of(2L, 4L), ll);

        // NaN-aware predicates still see every element
        final FloatList fl = FloatList.of(1f, Float.NaN, 2f, Float.NaN);
        assertTrue(fl.removeIf(Float::isNaN));
        assertEquals(FloatList.of(1f, 2f), fl);

        final DoubleList dl = DoubleList.of(-0.0d, 0.0d, 1.0d);
        assertTrue(dl.removeIf(v -> v > 0.5d));
        assertEquals(DoubleList.of(-0.0d, 0.0d), dl);
    }

    @Test
    public void test_D1_removeIf_matchesAReferenceModelExhaustively() {
        for (int n = 0; n <= 10; n++) {
            for (int mask = 0; mask < (1 << n); mask++) {
                final int m = mask;
                final int[] base = new int[n];

                for (int i = 0; i < n; i++) {
                    base[i] = i;
                }

                final IntList actual = IntList.of(base.clone());
                final boolean changed = actual.removeIf(v -> (m & (1 << v)) != 0);

                final IntList expected = new IntList();

                for (int i = 0; i < n; i++) {
                    if ((m & (1 << i)) == 0) {
                        expected.add(i);
                    }
                }

                assertEquals(expected, actual, "n=" + n + " mask=" + mask);
                assertEquals(expected.size() != n, changed, "n=" + n + " mask=" + mask);
            }
        }
    }

    @Test
    public void test_D1_removeIf_randomizedAcrossEveryType() {
        final Random rnd = new Random(20260902L);

        for (int trial = 0; trial < 600; trial++) {
            final int n = rnd.nextInt(300);
            final int threshold = rnd.nextInt(10);

            final boolean[] booleans = new boolean[n];
            final char[] chars = new char[n];
            final byte[] bytes = new byte[n];
            final short[] shorts = new short[n];
            final int[] ints = new int[n];
            final long[] longs = new long[n];
            final float[] floats = new float[n];
            final double[] doubles = new double[n];

            for (int i = 0; i < n; i++) {
                final int v = rnd.nextInt(10);
                booleans[i] = v < threshold;
                chars[i] = (char) v;
                bytes[i] = (byte) v;
                shorts[i] = (short) v;
                ints[i] = v;
                longs[i] = v;
                floats[i] = v;
                doubles[i] = v;
            }

            final IntList expected = new IntList();

            for (final int v : ints) {
                if (v >= threshold) {
                    expected.add(v);
                }
            }

            final boolean shouldChange = expected.size() != n;

            final CharList cl = CharList.of(chars);
            assertEquals(shouldChange, cl.removeIf(v -> v < threshold), "char n=" + n);
            assertEquals(expected.size(), cl.size(), "char n=" + n);

            final ByteList byl = ByteList.of(bytes);
            assertEquals(shouldChange, byl.removeIf(v -> v < threshold), "byte n=" + n);
            assertEquals(expected.size(), byl.size(), "byte n=" + n);

            final ShortList sl = ShortList.of(shorts);
            assertEquals(shouldChange, sl.removeIf(v -> v < threshold), "short n=" + n);
            assertEquals(expected.size(), sl.size(), "short n=" + n);

            final IntList il = IntList.of(ints);
            assertEquals(shouldChange, il.removeIf(v -> v < threshold), "int n=" + n);
            assertEquals(expected, il, "int n=" + n);

            final LongList ll = LongList.of(longs);
            assertEquals(shouldChange, ll.removeIf(v -> v < threshold), "long n=" + n);
            assertEquals(expected.toLongList(), ll, "long n=" + n);

            final FloatList fl = FloatList.of(floats);
            assertEquals(shouldChange, fl.removeIf(v -> v < threshold), "float n=" + n);
            assertEquals(expected.toFloatList(), fl, "float n=" + n);

            final DoubleList dl = DoubleList.of(doubles);
            assertEquals(shouldChange, dl.removeIf(v -> v < threshold), "double n=" + n);
            assertEquals(expected.toDoubleList(), dl, "double n=" + n);

            // booleans: everything below the threshold is true, so removing true leaves the false run
            long falseCount = 0;

            for (final boolean b : booleans) {
                if (!b) {
                    falseCount++;
                }
            }

            final BooleanList bl = BooleanList.of(booleans);
            assertEquals(falseCount != n, bl.removeIf(v -> v), "boolean n=" + n);
            assertEquals(falseCount, bl.size(), "boolean n=" + n);
        }
    }

    @Test
    public void test_B3_serializationRoundTripIsStableOverRandomLists() throws Exception {
        final Random rnd = new Random(7L);

        for (int trial = 0; trial < 300; trial++) {
            final int n = rnd.nextInt(40);
            final int spare = rnd.nextInt(40);

            final IntList list = new IntList(n + spare);

            for (int i = 0; i < n; i++) {
                list.add(rnd.nextInt());
            }

            final byte[] bytes = serialize(list);
            final IntList back = deserialize(bytes);

            assertEquals(list, back);
            assertEquals(n, back.internalArray().length, "capacity must be trimmed on the wire");
            // serializing the restored list must reproduce the very same bytes
            assertArrayEquals(bytes, serialize(back));

            final DoubleList doubles = new DoubleList(n + spare);

            for (int i = 0; i < n; i++) {
                doubles.add(rnd.nextBoolean() ? Double.NaN : rnd.nextDouble() - 0.5d);
            }

            assertEquals(doubles, deserialize(serialize(doubles)));
        }
    }

    // ---------------------------------------------------------------------------------------
    // D2 - replaceRange grows with the amortized policy
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_D2_replaceRangeGrowsAmortized() {
        final IntList list = new IntList();

        for (int i = 0; i < 1000; i++) {
            list.replaceRange(list.size(), list.size(), new int[] { i });
        }

        assertEquals(1000, list.size());
        // exact-size growth would leave capacity == size after every call
        assertTrue(list.internalArray().length > list.size(), "capacity " + list.internalArray().length + " should exceed size " + list.size());

        for (int i = 0; i < 1000; i++) {
            assertEquals(i, list.get(i));
        }
    }

    @Test
    public void test_D2_replaceRangeResultsAreUnchanged() {
        final Random rnd = new Random(20260902L);

        for (int trial = 0; trial < 3000; trial++) {
            final int n = rnd.nextInt(10);
            final int[] base = new int[n];

            for (int i = 0; i < n; i++) {
                base[i] = rnd.nextInt(20);
            }

            final int from = rnd.nextInt(n + 1);
            final int to = from + rnd.nextInt(n - from + 1);
            final int m = rnd.nextInt(5);
            final int[] replacement = new int[m];

            for (int i = 0; i < m; i++) {
                replacement[i] = 100 + i;
            }

            final List<Integer> expected = new ArrayList<>();

            for (int i = 0; i < from; i++) {
                expected.add(base[i]);
            }
            for (final int v : replacement) {
                expected.add(v);
            }
            for (int i = to; i < n; i++) {
                expected.add(base[i]);
            }

            final IntList viaArray = IntList.of(base.clone());
            viaArray.replaceRange(from, to, replacement);

            final IntList viaList = IntList.of(base.clone());
            viaList.replaceRange(from, to, IntList.of(replacement.clone()));

            assertEquals(expected, viaArray.boxed());
            assertEquals(expected, viaList.boxed());
        }
    }

    @Test
    public void test_D2_replaceRangeWithSelf() {
        final IntList list = IntList.of(1, 2, 3, 4, 5);
        list.replaceRange(1, 2, list);
        assertEquals(IntList.of(1, 1, 2, 3, 4, 5, 3, 4, 5), list);
    }

    // ---------------------------------------------------------------------------------------
    // D4 - batchRemove goes through the documented needToSet hook
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_D4_removeAllAndRetainAllPickTheSameAnswerOnBothPaths() {
        // needToSet(5, 100) is true while the old hardcoded test (c.size() > 3 && size() > 9) was false,
        // so this shape now takes the hash path; the answer must be identical either way
        final IntList small = IntList.of(0, 1, 2, 3, 4);
        final IntList large = new IntList();

        for (int i = 0; i < 100; i++) {
            large.add(i % 3);
        }

        final IntList removed = small.copy();
        assertTrue(removed.removeAll(large));
        assertEquals(IntList.of(3, 4), removed);

        final IntList retained = small.copy();
        assertTrue(retained.retainAll(large));
        assertEquals(IntList.of(0, 1, 2), retained);
    }

    @Test
    public void test_D4_batchRemoveMatchesAReferenceModel() {
        final Random rnd = new Random(4242L);

        for (int trial = 0; trial < 4000; trial++) {
            final IntList a = new IntList();
            final IntList b = new IntList();

            for (int i = 0, n = rnd.nextInt(25); i < n; i++) {
                a.add(rnd.nextInt(8));
            }
            for (int i = 0, n = rnd.nextInt(25); i < n; i++) {
                b.add(rnd.nextInt(8));
            }

            final IntList expectedRemoved = new IntList();
            final IntList expectedRetained = new IntList();

            for (int i = 0; i < a.size(); i++) {
                if (b.contains(a.get(i))) {
                    expectedRetained.add(a.get(i));
                } else {
                    expectedRemoved.add(a.get(i));
                }
            }

            final IntList actualRemoved = a.copy();
            actualRemoved.removeAll(b);
            assertEquals(expectedRemoved, actualRemoved, "a=" + a + " b=" + b);

            final IntList actualRetained = a.copy();
            actualRetained.retainAll(b);
            assertEquals(expectedRetained, actualRetained, "a=" + a + " b=" + b);
        }
    }

    @Test
    public void test_D4_batchRemovePathsAgreeOnNaNAndSignedZero() {
        // needToSet now selects the hash path in shapes the old hardcoded test sent down the linear path.
        // The two paths compare differently for float/double - boxed Float.equals on the hash side versus
        // N.equals (Float.compare) on the linear side - so they must be proven to agree on exactly the
        // values where those two could diverge.
        final float[] pool = { 0.0f, -0.0f, Float.NaN, 1.0f, -1.0f, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY };
        final Random rnd = new Random(2026090201L);

        for (int trial = 0; trial < 3000; trial++) {
            // sizes straddle needToSet's thresholds (min > 3 && max > 9) in both directions
            final FloatList a = new FloatList();
            final FloatList b = new FloatList();

            for (int i = 0, n = rnd.nextInt(26); i < n; i++) {
                a.add(pool[rnd.nextInt(pool.length)]);
            }
            for (int i = 0, n = rnd.nextInt(26); i < n; i++) {
                b.add(pool[rnd.nextInt(pool.length)]);
            }

            final FloatList expectedRemoved = new FloatList();
            final FloatList expectedRetained = new FloatList();

            for (int i = 0; i < a.size(); i++) {
                if (b.contains(a.get(i))) {
                    expectedRetained.add(a.get(i));
                } else {
                    expectedRemoved.add(a.get(i));
                }
            }

            final FloatList removed = a.copy();
            removed.removeAll(b);
            assertEquals(expectedRemoved, removed, "removeAll a=" + a + " b=" + b);

            final FloatList retained = a.copy();
            retained.retainAll(b);
            assertEquals(expectedRetained, retained, "retainAll a=" + a + " b=" + b);

            boolean expectedContainsAll = true;

            for (int j = 0; j < b.size(); j++) {
                if (!a.contains(b.get(j))) {
                    expectedContainsAll = false;
                    break;
                }
            }

            assertEquals(expectedContainsAll, a.containsAll(b), "containsAll a=" + a + " b=" + b);

            boolean expectedDisjoint = true;

            for (int j = 0; j < b.size() && expectedDisjoint; j++) {
                if (a.contains(b.get(j))) {
                    expectedDisjoint = false;
                }
            }

            assertEquals(expectedDisjoint, a.disjoint(b), "disjoint a=" + a + " b=" + b);
        }

        // the specific values that make the two comparison mechanisms interesting
        assertEquals(DoubleList.of(-0.0d), DoubleList.of(0.0d, -0.0d).difference(DoubleList.of(0.0d)));
        final DoubleList nan = DoubleList.of(Double.NaN, 1d, Double.NaN, 2d, 3d, 4d, 5d, 6d, 7d, 8d, 9d, 10d);
        final DoubleList probe = DoubleList.of(Double.NaN, 1d, 2d, 3d);
        final DoubleList retained = nan.copy();
        retained.retainAll(probe);
        assertEquals(DoubleList.of(Double.NaN, 1d, Double.NaN, 2d, 3d), retained);
    }

    @Test
    public void test_J1_streamCaptureVersusLiveContents() {
        // pins the three claims the stream() javadoc now makes
        final IntList inPlace = new IntList(10);
        inPlace.addAll(new int[] { 1, 2, 3 });
        final com.landawn.abacus.util.stream.IntStream live = inPlace.stream();
        inPlace.set(0, 99);
        assertArrayEquals(new int[] { 99, 2, 3 }, live.toArray(), "contents stay live");

        final IntList growing = new IntList(10);
        growing.addAll(new int[] { 1, 2, 3 });
        final com.landawn.abacus.util.stream.IntStream ranged = growing.stream();
        growing.add(4);
        assertArrayEquals(new int[] { 1, 2, 3 }, ranged.toArray(), "growth does not extend the stream");

        final IntList reallocating = new IntList(4);
        reallocating.addAll(new int[] { 1, 2, 3, 4 });
        final int[] oldBacking = reallocating.internalArray();
        final com.landawn.abacus.util.stream.IntStream stale = reallocating.stream();
        reallocating.add(5);
        assertTrue(oldBacking != reallocating.internalArray(), "test setup: the add should have reallocated");
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, stale.toArray(), "a reallocation leaves the stream on the old array");
        reallocating.set(0, 99);
        assertEquals(1, oldBacking[0], "the write lands on the new array only");
    }

    @Test
    public void test_D4_toSetGoesThroughCreateSetSupplier() {
        final Set<Integer> set = IntList.of(3, 1, 2, 3, 1).toSet();
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));

        assertEquals(CommonUtil.newHashSet(Arrays.asList(1, 2, 3)), IntList.of(1, 2, 3, 1).toSet(0, 4));
        assertTrue(new IntList().toSet().isEmpty());
    }

    @Test
    public void test_D6_commonUtilAcceptsAParameterizedPrimitiveList() {
        // N.size/isEmpty/notEmpty took a RAW PrimitiveList, which is what made adding Iterable<B>
        // report an ambiguity. They are parameterized now; this pins that they still accept every shape.
        final PrimitiveList<?, ?, ?> wildcard = IntList.of(1, 2, 3);

        assertEquals(3, CommonUtil.size(wildcard));
        assertFalse(CommonUtil.isEmpty(wildcard));
        assertTrue(CommonUtil.notEmpty(wildcard));

        final PrimitiveList<?, ?, ?> empty = new DoubleList();
        assertEquals(0, CommonUtil.size(empty));
        assertTrue(CommonUtil.isEmpty(empty));
        assertFalse(CommonUtil.notEmpty(empty));

        assertEquals(0, CommonUtil.size((PrimitiveList<?, ?, ?>) null));
        assertTrue(CommonUtil.isEmpty((PrimitiveList<?, ?, ?>) null));
        assertFalse(CommonUtil.notEmpty((PrimitiveList<?, ?, ?>) null));

        // the concrete types still resolve to the same overload
        assertEquals(2, CommonUtil.size(BooleanList.of(true, false)));
        assertEquals(2, CommonUtil.size(CharList.of('a', 'b')));
        assertEquals(2, CommonUtil.size(ByteList.of((byte) 1, (byte) 2)));
        assertEquals(2, CommonUtil.size(ShortList.of((short) 1, (short) 2)));
        assertEquals(2, CommonUtil.size(LongList.of(1L, 2L)));
        assertEquals(2, CommonUtil.size(FloatList.of(1f, 2f)));

        // If carried the same two raw parameters; it has no boolean accessor, so observe the branch taken
        assertEquals("then", branchOf(If.isEmpty(new IntList())));
        assertEquals("orElse", branchOf(If.isEmpty(wildcard)));
        assertEquals("then", branchOf(If.notEmpty(wildcard)));
        assertEquals("then", branchOf(If.isEmpty((PrimitiveList<?, ?, ?>) null)));
        assertEquals("orElse", branchOf(If.notEmpty((PrimitiveList<?, ?, ?>) null)));
    }

    private static String branchOf(final If condition) {
        final String[] taken = { null };
        condition.then(() -> taken[0] = "then").orElse(() -> taken[0] = "orElse");
        return taken[0];
    }

    @Test
    public void test_D4_needToSetIsTheOnlyAlgorithmSwitch() {
        // the hook's contract: min > 3 && max > 9. Walk both sides of every boundary and confirm the
        // answer never depends on which branch batchRemove took.
        for (final int selfSize : new int[] { 0, 1, 3, 4, 5, 9, 10, 11, 30 }) {
            for (final int otherSize : new int[] { 0, 1, 3, 4, 5, 9, 10, 11, 30 }) {
                final IntList self = new IntList();
                final IntList other = new IntList();

                for (int i = 0; i < selfSize; i++) {
                    self.add(i % 7);
                }
                for (int i = 0; i < otherSize; i++) {
                    other.add(i % 5);
                }

                final IntList expectedRemoved = new IntList();
                final IntList expectedRetained = new IntList();

                for (int i = 0; i < self.size(); i++) {
                    if (other.contains(self.get(i))) {
                        expectedRetained.add(self.get(i));
                    } else {
                        expectedRemoved.add(self.get(i));
                    }
                }

                final String where = "selfSize=" + selfSize + " otherSize=" + otherSize;

                final IntList removed = self.copy();
                removed.removeAll(other);
                assertEquals(expectedRemoved, removed, where);

                final IntList retained = self.copy();
                retained.retainAll(other);
                assertEquals(expectedRetained, retained, where);
            }
        }
    }

    // ---------------------------------------------------------------------------------------
    // D5 - split returns an ordinary mutable list, not a raw-cast view
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_D5_splitReturnsAMutableListOfLists() {
        final List<IntList> parts = IntList.of(1, 2, 3, 4, 5, 6, 7).split(3);

        assertEquals(3, parts.size());
        assertEquals(IntList.of(1, 2, 3), parts.get(0));
        assertEquals(IntList.of(4, 5, 6), parts.get(1));
        assertEquals(IntList.of(7), parts.get(2));

        // the result must be an ordinary list; the old raw-cast version depended on N.split's
        // internals for this to hold
        parts.add(IntList.of(9));
        assertEquals(4, parts.size());
        assertInstanceOf(IntList.class, parts.get(0));

        assertTrue(new IntList().split(3).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> IntList.of(1, 2).split(0));
        assertThrows(IllegalArgumentException.class, () -> IntList.of(1, 2).split(-1));
    }

    @Test
    public void test_D5_splitAllSiblingTypes() {
        assertEquals(2, BooleanList.of(true, false, true).split(2).size());
        assertEquals(2, CharList.of('a', 'b', 'c').split(2).size());
        assertEquals(2, ByteList.of((byte) 1, (byte) 2, (byte) 3).split(2).size());
        assertEquals(2, ShortList.of((short) 1, (short) 2, (short) 3).split(2).size());
        assertEquals(2, IntList.of(1, 2, 3).split(2).size());
        assertEquals(2, LongList.of(1L, 2L, 3L).split(2).size());
        assertEquals(2, FloatList.of(1f, 2f, 3f).split(2).size());
        assertEquals(2, DoubleList.of(1d, 2d, 3d).split(2).size());

        assertEquals(DoubleList.of(3d), DoubleList.of(1d, 2d, 3d).split(2).get(1));
    }

    // ---------------------------------------------------------------------------------------
    // D12 - addLast
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_D12_addLastAppendsForEveryType() {
        final BooleanList bl = BooleanList.of(true);
        bl.addLast(false);
        assertEquals(BooleanList.of(true, false), bl);

        final CharList cl = CharList.of('a');
        cl.addLast('b');
        assertEquals(CharList.of('a', 'b'), cl);

        final ByteList byl = ByteList.of((byte) 1);
        byl.addLast((byte) 2);
        assertEquals(ByteList.of((byte) 1, (byte) 2), byl);

        final ShortList sl = ShortList.of((short) 1);
        sl.addLast((short) 2);
        assertEquals(ShortList.of((short) 1, (short) 2), sl);

        final IntList il = IntList.of(1);
        il.addLast(2);
        assertEquals(IntList.of(1, 2), il);

        final LongList ll = LongList.of(1L);
        ll.addLast(2L);
        assertEquals(LongList.of(1L, 2L), ll);

        final FloatList fl = FloatList.of(1f);
        fl.addLast(2f);
        assertEquals(FloatList.of(1f, 2f), fl);

        final DoubleList dl = DoubleList.of(1d);
        dl.addLast(2d);
        assertEquals(DoubleList.of(1d, 2d), dl);

        // on an empty list, and interleaved with addFirst
        final IntList empty = new IntList();
        empty.addLast(5);
        empty.addFirst(4);
        empty.addLast(6);
        assertEquals(IntList.of(4, 5, 6), empty);
    }

    // ---------------------------------------------------------------------------------------
    // B5 - both array constructors reject null with a named message
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_B5_arrayConstructorsRejectNullWithANamedMessage() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new IntList((int[]) null)).getMessage().contains("'a'"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> new IntList((int[]) null, 0)).getMessage().contains("'a'"));

        assertThrows(IllegalArgumentException.class, () -> new BooleanList((boolean[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> new CharList((char[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ByteList((byte[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> new ShortList((short[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> new LongList((long[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> new FloatList((float[]) null, 0));
        assertThrows(IllegalArgumentException.class, () -> new DoubleList((double[]) null, 0));

        // the null-tolerant static factories are unaffected
        assertTrue(IntList.of((int[]) null).isEmpty());
        assertTrue(IntList.of((int[]) null, 0).isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // C-033 - a negative size throws IllegalArgumentException, not IndexOutOfBoundsException
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_C033_negativeSizeThrowsIllegalArgumentException() {
        // N.checkFromIndexSize deliberately reports a negative size as IllegalArgumentException,
        // unlike Objects.checkFromIndexSize which reports IndexOutOfBoundsException for every violation
        assertThrows(IllegalArgumentException.class, () -> new IntList(new int[2], -1));
        assertThrows(IllegalArgumentException.class, () -> IntList.of(new int[2], -1));

        assertThrows(IllegalArgumentException.class, () -> new BooleanList(new boolean[2], -1));
        assertThrows(IllegalArgumentException.class, () -> BooleanList.of(new boolean[2], -1));
        assertThrows(IllegalArgumentException.class, () -> new CharList(new char[2], -1));
        assertThrows(IllegalArgumentException.class, () -> CharList.of(new char[2], -1));
        assertThrows(IllegalArgumentException.class, () -> new ByteList(new byte[2], -1));
        assertThrows(IllegalArgumentException.class, () -> ByteList.of(new byte[2], -1));
        assertThrows(IllegalArgumentException.class, () -> new ShortList(new short[2], -1));
        assertThrows(IllegalArgumentException.class, () -> ShortList.of(new short[2], -1));
        assertThrows(IllegalArgumentException.class, () -> new LongList(new long[2], -1));
        assertThrows(IllegalArgumentException.class, () -> LongList.of(new long[2], -1));
        assertThrows(IllegalArgumentException.class, () -> new FloatList(new float[2], -1));
        assertThrows(IllegalArgumentException.class, () -> FloatList.of(new float[2], -1));
        assertThrows(IllegalArgumentException.class, () -> new DoubleList(new double[2], -1));
        assertThrows(IllegalArgumentException.class, () -> DoubleList.of(new double[2], -1));

        // a size beyond the array is still IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> new IntList(new int[2], 3));
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of(new int[2], 3));
        assertThrows(IndexOutOfBoundsException.class, () -> new DoubleList(new double[2], 3));
        assertThrows(IndexOutOfBoundsException.class, () -> DoubleList.of(new double[2], 3));

        // the boundaries themselves are legal
        assertTrue(IntList.of(new int[2], 0).isEmpty());
        assertEquals(2, IntList.of(new int[2], 2).size());
        assertTrue(new IntList(new int[2], 0).isEmpty());
        assertEquals(2, new IntList(new int[2], 2).size());

        // of(..) tolerates a null array only when the size is 0
        assertTrue(IntList.of((int[]) null, 0).isEmpty());
        assertThrows(IndexOutOfBoundsException.class, () -> IntList.of((int[]) null, 1));
        assertThrows(IllegalArgumentException.class, () -> IntList.of((int[]) null, -1));
    }

    // ---------------------------------------------------------------------------------------
    // C-034 - shuffle() and random(..) use different generators; both must be documented
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_C034_shuffleUsesADifferentGeneratorThanRandom() {
        // shuffle() must not consume the class's SecureRandom: it goes through ThreadLocalRandom.
        // Observable property: shuffle() is still a permutation of the input for every type.
        final int[] base = new int[64];

        for (int i = 0; i < base.length; i++) {
            base[i] = i;
        }

        final IntList list = IntList.of(base.clone());
        list.shuffle();

        final IntList sorted = list.copy();
        sorted.sort();
        assertArrayEquals(base, sorted.toArray(), "shuffle() must permute, not alter, the elements");

        // a seeded Random still gives a reproducible permutation through the explicit overload
        final IntList a = IntList.of(base.clone());
        final IntList b = IntList.of(base.clone());
        a.shuffle(new Random(42L));
        b.shuffle(new Random(42L));
        assertEquals(a, b, "shuffle(Random) must be reproducible for a seeded generator");

        // and every sibling still permutes
        final DoubleList dl = DoubleList.of(1d, 2d, 3d, Double.NaN);
        dl.shuffle();
        assertEquals(4, dl.size());
        assertTrue(dl.contains(Double.NaN));

        final BooleanList bl = BooleanList.of(true, true, false);
        bl.shuffle();
        assertEquals(2, bl.frequency(true));
        assertEquals(1, bl.frequency(false));

        final CharList cl = CharList.of('a', 'b', 'c');
        cl.shuffle();
        final CharList clSorted = cl.copy();
        clSorted.sort();
        assertEquals(CharList.of('a', 'b', 'c'), clSorted);
    }

    // ---------------------------------------------------------------------------------------
    // B6 - exception messages name the failed precondition
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_B6_charListRandomNamesTheFailedPrecondition() {
        final IllegalArgumentException onNull = assertThrows(IllegalArgumentException.class, () -> CharList.random((char[]) null, 3));
        assertNotNull(onNull.getMessage());
        assertTrue(onNull.getMessage().contains("candidates"), onNull.getMessage());

        final IllegalArgumentException onEmpty = assertThrows(IllegalArgumentException.class, () -> CharList.random(new char[0], 3));
        assertNotNull(onEmpty.getMessage());
        assertTrue(onEmpty.getMessage().contains("candidates"), onEmpty.getMessage());

        // the single-candidate fast path still works
        assertEquals(CharList.of('z', 'z', 'z'), CharList.random(new char[] { 'z' }, 3));
        assertEquals(3, CharList.random(new char[] { 'a', 'b' }, 3).size());
        assertThrows(NegativeArraySizeException.class, () -> CharList.random(new char[] { 'a', 'b' }, -1));
    }

    // ---------------------------------------------------------------------------------------
    // C-039 - the null-operand contract PrimitiveList documents, for every type
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_C039_nullOperandContractForEveryType() {
        final IntList l = IntList.of(1, 2, 3);

        // "If null or empty, this list remains unchanged"
        assertFalse(l.copy().addAll((int[]) null));
        assertFalse(l.copy().addAll((IntList) null));
        assertFalse(l.copy().addAll(1, (int[]) null));
        assertFalse(l.copy().addAll(1, (IntList) null));
        assertFalse(l.copy().removeAll((int[]) null));
        assertFalse(l.copy().removeAll((IntList) null));

        // query operands
        assertTrue(l.containsAll((int[]) null));
        assertTrue(l.containsAll((IntList) null));
        assertFalse(l.containsAny((int[]) null));
        assertFalse(l.containsAny((IntList) null));
        assertTrue(l.disjoint((int[]) null));
        assertTrue(l.disjoint((IntList) null));

        // set operations
        assertTrue(l.intersection((int[]) null).isEmpty());
        assertTrue(l.intersection((IntList) null).isEmpty());
        assertEquals(l, l.difference((int[]) null));
        assertEquals(l, l.difference((IntList) null));
        assertEquals(l, l.symmetricDifference((int[]) null));
        assertEquals(l, l.symmetricDifference((IntList) null));

        // "If null or empty, this list will be cleared"
        final IntList retained = l.copy();
        assertTrue(retained.retainAll((int[]) null));
        assertTrue(retained.isEmpty());
        assertFalse(new IntList().retainAll((int[]) null), "an already-empty list is not modified");

        // "Null or empty array results in no change"
        final IntList untouched = l.copy();
        untouched.removeAllAt((int[]) null);
        assertEquals(l, untouched);

        // "If null or empty, the range is simply deleted"
        final IntList spliced = l.copy();
        spliced.replaceRange(0, 1, (int[]) null);
        assertEquals(IntList.of(2, 3), spliced);

        // the same contract holds for every sibling
        assertTrue(BooleanList.of(true).containsAll((boolean[]) null));
        assertTrue(CharList.of('a').containsAll((char[]) null));
        assertTrue(ByteList.of((byte) 1).containsAll((byte[]) null));
        assertTrue(ShortList.of((short) 1).containsAll((short[]) null));
        assertTrue(LongList.of(1L).containsAll((long[]) null));
        assertTrue(FloatList.of(1f).containsAll((float[]) null));
        assertTrue(DoubleList.of(1d).containsAll((double[]) null));

        assertEquals(DoubleList.of(1d, 2d), DoubleList.of(1d, 2d).difference((double[]) null));
        assertEquals(BooleanList.of(true), BooleanList.of(true).symmetricDifference((boolean[]) null));

        final CharList cleared = CharList.of('a', 'b');
        assertTrue(cleared.retainAll((char[]) null));
        assertTrue(cleared.isEmpty());
    }

    // ---------------------------------------------------------------------------------------
    // C-040 - CharList holds UTF-16 code units; surrogates are opaque, never paired
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_C040_charListTreatsSurrogatesAsIndependentCodeUnits() {
        // U+1F600 is a surrogate pair; a CharList stores it as two independent chars
        final String emoji = "\uD83D\uDE00";
        final CharList cl = CharList.of(emoji.toCharArray());

        assertEquals(2, cl.size(), "a supplementary code point occupies two code units");
        assertEquals('\uD83D', cl.get(0));
        assertEquals('\uDE00', cl.get(1));

        // ordering, searching and dedup all work on the raw code unit, with no pairing logic
        assertEquals(0, cl.indexOf('\uD83D'));
        assertEquals(1, cl.indexOf('\uDE00'));
        assertTrue(cl.contains('\uD83D'));
        assertFalse(cl.containsDuplicates());

        // an unpaired (lone) surrogate is a perfectly ordinary element
        final CharList lone = CharList.of('\uD800', 'a', '\uDFFF');
        assertEquals(3, lone.size());
        assertEquals(1, lone.frequency('\uD800'));
        lone.sort();
        assertEquals(CharList.of('a', '\uD800', '\uDFFF'), lone, "sorted by code unit value");

        // reverse() must not try to keep pairs together
        final CharList rev = CharList.of(emoji.toCharArray());
        rev.reverse();
        assertEquals(CharList.of('\uDE00', '\uD83D'), rev);

        // round-tripping through toArray preserves the units exactly
        assertEquals(emoji, new String(CharList.of(emoji.toCharArray()).toArray()));

        // the full code-unit range is representable, including U+FFFF
        final CharList edge = CharList.of('\u0000', '\uFFFF');
        assertEquals(2, edge.size());
        assertEquals('\uFFFF', edge.getLast());
        assertEquals(0, edge.getFirst());
    }

    // ---------------------------------------------------------------------------------------
    // B2 / T2 - symmetricDifference: pin the documented duplicate-cancellation ordering
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_B2_symmetricDifferenceOrderingWithPartialDuplicateCancellation() {
        // the second operand's surviving occurrence is emitted at the earliest uncancelled position,
        // so this is [2, 1] and not difference(b) ++ b.difference(this), which would be [1, 2]
        assertEquals(IntList.of(2, 1), IntList.of(2).symmetricDifference(IntList.of(2, 1, 2)));
        assertEquals(IntList.of(2, 1), IntList.of(2).symmetricDifference(new int[] { 2, 1, 2 }));

        assertEquals(BooleanList.of(true, false), BooleanList.of(true).symmetricDifference(BooleanList.of(true, false, true)));
        assertEquals(CharList.of('b', 'a'), CharList.of('b').symmetricDifference(CharList.of('b', 'a', 'b')));
        assertEquals(ByteList.of((byte) 2, (byte) 1), ByteList.of((byte) 2).symmetricDifference(ByteList.of((byte) 2, (byte) 1, (byte) 2)));
        assertEquals(ShortList.of((short) 2, (short) 1), ShortList.of((short) 2).symmetricDifference(ShortList.of((short) 2, (short) 1, (short) 2)));
        assertEquals(LongList.of(2L, 1L), LongList.of(2L).symmetricDifference(LongList.of(2L, 1L, 2L)));
        assertEquals(FloatList.of(2f, 1f), FloatList.of(2f).symmetricDifference(FloatList.of(2f, 1f, 2f)));
        assertEquals(DoubleList.of(2d, 1d), DoubleList.of(2d).symmetricDifference(DoubleList.of(2d, 1d, 2d)));
    }

    @Test
    public void test_B2_symmetricDifferenceTakesTheEarliestSurvivingOccurrences() {
        // the documented rule: when a value survives n times, those n occurrences come from that value's
        // EARLIEST positions in the second operand, emitted in index order

        // 5 survives once -> b[0]; 7 survives once -> b[1].  A "latest occurrence" rule would give [7, 5].
        assertEquals(IntList.of(5, 7), IntList.of(5).symmetricDifference(IntList.of(5, 7, 5)));

        // surplus greater than one: 5 survives twice (b[0], b[2]), 7 survives twice (b[1], b[3]);
        // b[4] is the cancelled 5.  Emitted in index order -> [5, 7, 5, 7]
        assertEquals(IntList.of(5, 7, 5, 7), IntList.of(5).symmetricDifference(IntList.of(5, 7, 5, 7, 5)));

        // and it still differs from concatenating the two differences, which yields [7, 5, 7, 5]
        final IntList concatenated = IntList.of(5).difference(IntList.of(5, 7, 5, 7, 5));
        concatenated.addAll(IntList.of(5, 7, 5, 7, 5).difference(IntList.of(5)));
        assertEquals(IntList.of(7, 5, 7, 5), concatenated);
    }

    @Test
    public void test_B2_symmetricDifferenceContentAlwaysMatchesTheTwoDifferences() {
        final Random rnd = new Random(99L);

        for (int trial = 0; trial < 5000; trial++) {
            final IntList a = new IntList();
            final IntList b = new IntList();

            for (int i = 0, n = rnd.nextInt(7); i < n; i++) {
                a.add(rnd.nextInt(4));
            }
            for (int i = 0, n = rnd.nextInt(7); i < n; i++) {
                b.add(rnd.nextInt(4));
            }

            final IntList concatenated = a.difference(b);
            concatenated.addAll(b.difference(a));

            final IntList symmetric = a.symmetricDifference(b);

            final IntList sortedConcatenated = concatenated.copy();
            sortedConcatenated.sort();

            final IntList sortedSymmetric = symmetric.copy();
            sortedSymmetric.sort();

            // documented: same elements, and the second operand's part is a subsequence of b by value
            assertEquals(sortedConcatenated, sortedSymmetric, "a=" + a + " b=" + b);

            final IntList tail = symmetric.copy(a.difference(b).size(), symmetric.size());
            int j = 0;

            for (int i = 0; i < b.size() && j < tail.size(); i++) {
                if (b.get(i) == tail.get(j)) {
                    j++;
                }
            }

            assertEquals(tail.size(), j, "tail " + tail + " is not a subsequence of b=" + b);
        }
    }

    // ---------------------------------------------------------------------------------------
    // B4 / T3 - copy(from, to, step) with a step whose sign contradicts the range
    // ---------------------------------------------------------------------------------------

    @Test
    public void test_B4_copyWithContradictoryStepReturnsEmpty() {
        final IntList list = IntList.of(10, 11, 12, 13, 14);

        assertTrue(list.copy(0, 5, -1).isEmpty());
        assertTrue(list.copy(0, 5, -3).isEmpty());
        assertTrue(list.copy(4, 0, 1).isEmpty());
        assertTrue(list.copy(4, -1, 1).isEmpty());

        // the supported directions still work
        assertEquals(IntList.of(10, 12, 14), list.copy(0, 5, 2));
        assertEquals(IntList.of(14, 13, 12, 11, 10), list.copy(5, -1, -1));
        assertEquals(IntList.of(14, 13, 12, 11), list.copy(4, 0, -1));

        // only a zero step is rejected
        assertThrows(IllegalArgumentException.class, () -> list.copy(0, 5, 0));

        assertTrue(new IntList().copy(0, 0, -1).isEmpty());
        assertTrue(DoubleList.of(1d, 2d, 3d).copy(0, 3, -1).isEmpty());
        assertTrue(BooleanList.of(true, false).copy(0, 2, -1).isEmpty());
    }
}
