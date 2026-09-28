package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.ByteStream;
import com.landawn.abacus.util.stream.CharStream;
import com.landawn.abacus.util.stream.ShortStream;

@Tag("unit")
public class PerformancePrimitiveRegressionTest extends TestBase {
    private static final Class<?>[] PRIMITIVES = { boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class };
    private static final String[] NAMES = { "Boolean", "Byte", "Char", "Short", "Int", "Long", "Float", "Double" };

    @Test
    public void byteMultisetComparisonKeepsAllocationBoundedAndInputsUnchanged() {
        final byte[] left = new byte[1_000_000];
        final byte[] right = new byte[left.length];
        for (int i = 0; i < left.length; i++) {
            left[i] = (byte) i;
            right[right.length - 1 - i] = left[i];
        }
        final byte[] leftCopy = left.clone();
        final byte[] rightCopy = right.clone();
        // Warm class initialization before measuring the operation's temporary storage.
        assertTrue(N.containsSameElements(new byte[] { -128, 0, 127 }, new byte[] { 127, -128, 0 }));
        final com.sun.management.ThreadMXBean allocation = allocationBean();
        final long thread = Thread.currentThread().threadId();
        final long before = allocation.getThreadAllocatedBytes(thread);
        final boolean same = N.containsSameElements(left, right);
        final long allocated = allocation.getThreadAllocatedBytes(thread) - before;
        assertTrue(same);
        assertTrue(allocated < 100_000, "allocated " + allocated + " bytes comparing byte arrays");
        assertArrayEquals(leftCopy, left);
        assertArrayEquals(rightCopy, right);
        right[0]++;
        assertFalse(N.containsSameElements(left, right));
        assertTrue(N.containsSameElements((byte[]) null, new byte[0]));
        assertFalse(N.containsSameElements((byte[]) null, new byte[] { 0 }));
    }

    @Test
    public void charAndShortComparisonsPreserveCountsAcrossSizesAndDomains() {
        for (final int size : new int[] { 1, 2, 16, 256, 1749, 1750, 1751, 1752, 65536, 65537 }) {
            final char[] chars = new char[size];
            final short[] shorts = new short[size];
            final char[] reversedChars = new char[size];
            final short[] reversedShorts = new short[size];
            for (int i = 0; i < size; i++) {
                // An odd multiplier visits all 65,536 bit patterns before repeating.
                chars[i] = (char) (i * 40503);
                shorts[i] = (short) chars[i];
                reversedChars[size - 1 - i] = chars[i];
                reversedShorts[size - 1 - i] = shorts[i];
            }
            final char[] charsCopy = chars.clone();
            final short[] shortsCopy = shorts.clone();
            final char[] reversedCharsCopy = reversedChars.clone();
            final short[] reversedShortsCopy = reversedShorts.clone();
            assertTrue(N.containsSameElements(chars, reversedChars), "char size " + size);
            assertTrue(N.containsSameElements(shorts, reversedShorts), "short size " + size);
            assertArrayEquals(charsCopy, chars);
            assertArrayEquals(shortsCopy, shorts);
            assertArrayEquals(reversedCharsCopy, reversedChars);
            assertArrayEquals(reversedShortsCopy, reversedShorts);
            reversedChars[size / 2] ^= 1;
            reversedShorts[size / 2] ^= 1;
            assertFalse(N.containsSameElements(chars, reversedChars), "char size " + size);
            assertFalse(N.containsSameElements(shorts, reversedShorts), "short size " + size);
        }

        final char[] chars = new char[100_000];
        final short[] shorts = new short[chars.length];
        Arrays.fill(chars, Character.MAX_VALUE);
        Arrays.fill(shorts, Short.MIN_VALUE);
        final char[] otherChars = chars.clone();
        final short[] otherShorts = shorts.clone();
        assertTrue(N.containsSameElements(chars, otherChars));
        assertTrue(N.containsSameElements(shorts, otherShorts));
        otherChars[otherChars.length - 1] = Character.MIN_VALUE;
        otherShorts[otherShorts.length - 1] = Short.MAX_VALUE;
        assertFalse(N.containsSameElements(chars, otherChars));
        assertFalse(N.containsSameElements(shorts, otherShorts));
        assertTrue(N.containsSameElements((char[]) null, new char[0]));
        assertTrue(N.containsSameElements(new short[0], (short[]) null));
        assertFalse(N.containsSameElements(chars, (char[]) null));
        assertFalse(N.containsSameElements(shorts, new short[1]));
        assertTrue(N.containsSameElements(chars, chars));
        assertTrue(N.containsSameElements(shorts, shorts));
    }

    @Test
    public void charAndShortComparisonsBoundLargeAllocationsAndKeepSmallInputsCheap() {
        final char[] chars = new char[1_000_000];
        final short[] shorts = new short[chars.length];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) i;
            shorts[i] = (short) i;
        }
        final char[] otherChars = chars.clone();
        final short[] otherShorts = shorts.clone();
        assertTrue(N.containsSameElements(chars, otherChars));
        assertTrue(N.containsSameElements(shorts, otherShorts));
        final com.sun.management.ThreadMXBean allocation = allocationBean();
        final long thread = Thread.currentThread().threadId();
        long before = allocation.getThreadAllocatedBytes(thread);
        final boolean sameChars = N.containsSameElements(chars, otherChars);
        final boolean sameShorts = N.containsSameElements(shorts, otherShorts);
        long allocated = allocation.getThreadAllocatedBytes(thread) - before;
        assertTrue(sameChars);
        assertTrue(sameShorts);
        assertTrue(allocated < 600_000, "allocated " + allocated + " bytes comparing large char and short arrays");

        final char[] smallChars = { 0, Character.MAX_VALUE, 'a', 'a' };
        final char[] otherSmallChars = { 'a', 0, 'a', Character.MAX_VALUE };
        final short[] smallShorts = { Short.MIN_VALUE, Short.MAX_VALUE, 0, -1 };
        final short[] otherSmallShorts = { 0, -1, Short.MAX_VALUE, Short.MIN_VALUE };
        assertTrue(N.containsSameElements(smallChars, otherSmallChars));
        assertTrue(N.containsSameElements(smallShorts, otherSmallShorts));
        boolean sameSmallInputs = true;
        before = allocation.getThreadAllocatedBytes(thread);
        for (int i = 0; i < 100; i++) {
            sameSmallInputs &= N.containsSameElements(smallChars, otherSmallChars);
            sameSmallInputs &= N.containsSameElements(smallShorts, otherSmallShorts);
        }
        allocated = allocation.getThreadAllocatedBytes(thread) - before;
        assertTrue(sameSmallInputs);
        assertTrue(allocated < 100_000, "allocated " + allocated + " bytes comparing small char and short arrays");
    }

    @Test
    public void repetitivePrimitiveDuplicateRemovalAllocatesByDistinctCount() {
        final int[] ints = new int[1_000_000];
        final long[] longs = new long[ints.length];
        final float[] floats = new float[ints.length];
        final double[] doubles = new double[ints.length];
        for (int i = 0; i < ints.length; i++) {
            ints[i] = i & 1;
            longs[i] = i & 1;
            floats[i] = i & 1;
            doubles[i] = i & 1;
        }
        N.removeDuplicates(new int[] { 0, 1, 0 }, false);
        N.removeDuplicates(new long[] { 0, 1, 0 }, false);
        N.removeDuplicates(new float[] { 0, 1, 0 }, false);
        N.removeDuplicates(new double[] { 0, 1, 0 }, false);
        final com.sun.management.ThreadMXBean allocation = allocationBean();
        final long thread = Thread.currentThread().threadId();
        final long before = allocation.getThreadAllocatedBytes(thread);
        final int[] distinctInts = N.removeDuplicates(ints, false);
        final long[] distinctLongs = N.removeDuplicates(longs, false);
        final float[] distinctFloats = N.removeDuplicates(floats, false);
        final double[] distinctDoubles = N.removeDuplicates(doubles, false);
        final long allocated = allocation.getThreadAllocatedBytes(thread) - before;
        assertArrayEquals(new int[] { 0, 1 }, distinctInts);
        assertArrayEquals(new long[] { 0, 1 }, distinctLongs);
        assertArrayEquals(new float[] { 0, 1 }, distinctFloats);
        assertArrayEquals(new double[] { 0, 1 }, distinctDoubles);
        assertTrue(allocated < 100_000, "allocated " + allocated + " bytes removing repeated primitive values");
    }

    private static com.sun.management.ThreadMXBean allocationBean() {
        Assumptions.assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        final com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        return bean;
    }

    @Test
    public void emptyLeftSetOperationsReturnIndependentEmptyListsForEveryPrimitive() throws Exception {
        for (int i = 0; i < PRIMITIVES.length; i++) {
            final Class<?> type = Class.forName("com.landawn.abacus.util." + NAMES[i] + "List");
            final Object array = Array.newInstance(PRIMITIVES[i], 10);
            final Object right = type.getMethod("of", array.getClass()).invoke(null, array);
            final Object left = type.getConstructor().newInstance();
            for (final String operation : new String[] { "intersection", "difference" }) {
                final Object result = type.getMethod(operation, type).invoke(left, right);
                assertNotSame(left, result);
                assertEquals(0, type.getMethod("size").invoke(result));
                assertEquals(0, type.getMethod("size").invoke(type.getMethod(operation, array.getClass()).invoke(left, array)));
                assertEquals(0, type.getMethod("size").invoke(type.getMethod(operation, type).invoke(left, new Object[] { null })));
            }
        }
        assertArrayEquals(new int[] { 2, 2 }, IntList.of(1, 2, 2, 3).intersection(IntList.of(2, 2, 4)).toArray());
        assertArrayEquals(new int[] { 1, 3 }, IntList.of(1, 2, 2, 3).difference(IntList.of(2, 2, 4)).toArray());
    }

    @Test
    public void narrowPrimitiveSetsPreserveDomainsAndRanges() {
        final byte[] allBytes = new byte[256];
        for (int i = 0; i < allBytes.length; i++) allBytes[i] = (byte) i;
        assertEquals(256, ByteList.of(allBytes).toSet().size());
        assertEquals(256, ByteStream.of(allBytes).toSet().size());
        assertEquals(Set.of((byte) 1), ByteList.of((byte) 0, (byte) 1, (byte) 1, (byte) 2).toSet(1, 3));
        assertEquals(Set.of(false, true), BooleanList.of(false, true, false).toSet());
        assertEquals(Set.of('\0', '\uffff'), CharList.of('\0', '\uffff', '\0').toSet());
        assertEquals(Set.of(Short.MIN_VALUE, Short.MAX_VALUE), ShortList.of(Short.MIN_VALUE, Short.MAX_VALUE).toSet());
        assertThrows(IndexOutOfBoundsException.class, () -> ByteList.of((byte) 1).toSet(1, 0));
        assertTrue(BooleanList.of(true).toSet(1, 1).isEmpty());
    }

    @Test
    public void emptyLeftAndNarrowDomainCollectionsDoNotAllocateFromInputCardinality() {
        Assumptions.assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean);
        final com.sun.management.ThreadMXBean bean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        Assumptions.assumeTrue(bean.isThreadAllocatedMemorySupported());
        bean.setThreadAllocatedMemoryEnabled(true);
        final int[] distinct = new int[250_000];
        for (int i = 0; i < distinct.length; i++) distinct[i] = i;
        final IntList right = IntList.of(distinct), empty = new IntList();
        final byte[] bytes = new byte[1_000_000];
        final char[] chars = new char[1_000_000];
        final short[] shorts = new short[1_000_000];
        final boolean[] booleans = new boolean[1_000_000];
        // Initialize factories before measuring allocation attributable to input cardinality.
        ByteStream.of((byte) 0).toSet(); CharStream.of('a').toSet(); ShortStream.of((short) 0).toSet();
        ByteList.of((byte) 0).toSet(); CharList.of('a').toSet(); ShortList.of((short) 0).toSet(); BooleanList.of(false).toSet();
        final long thread = Thread.currentThread().threadId(), before = bean.getThreadAllocatedBytes(thread);
        assertTrue(empty.intersection(right).isEmpty());
        assertTrue(empty.difference(right).isEmpty());
        assertEquals(1, ByteStream.of(bytes).toSet().size());
        assertEquals(1, CharStream.of(chars).toSet().size());
        assertEquals(1, ShortStream.of(shorts).toSet().size());
        assertEquals(1, ByteList.of(bytes).toSet().size());
        assertEquals(1, CharList.of(chars).toSet().size());
        assertEquals(1, ShortList.of(shorts).toSet().size());
        assertEquals(1, BooleanList.of(booleans).toSet().size());
        final long allocated = bean.getThreadAllocatedBytes(thread) - before;
        assertTrue(allocated < 5_000_000, "allocated " + allocated + " bytes for bounded-domain results");
    }

    @Test
    public void everyDeferredPrimitiveIteratorClearsItsFactoryAfterInitialization() throws Exception {
        for (int i = 0; i < PRIMITIVES.length; i++) {
            final Class<?> type = Class.forName("com.landawn.abacus.util." + NAMES[i] + "Iterator");
            final Object array = Array.newInstance(PRIMITIVES[i], 1);
            final Object source = type.getMethod("of", array.getClass()).invoke(null, array);
            final AtomicInteger calls = new AtomicInteger();
            final Supplier<Object> factory = () -> { calls.incrementAndGet(); return source; };
            final Iterator<?> iterator = (Iterator<?>) type.getMethod("defer", Supplier.class).invoke(null, factory);
            assertEquals(0, calls.get());
            assertTrue(iterator.hasNext());
            assertNotNull(iterator.next());
            assertFalse(iterator.hasNext());
            assertEquals(1, calls.get());
            assertFactoryReleased(iterator, factory);
        }
        final Supplier<Iterator<Integer>> factory = () -> java.util.List.of(1).iterator();
        final ObjIterator<Integer> objectIterator = ObjIterator.defer(factory);
        assertEquals(1, objectIterator.next());
        assertFactoryReleased(objectIterator, factory);
    }

    private static void assertFactoryReleased(final Object iterator, final Object factory) throws Exception {
        for (final Field f : iterator.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            final Object value = f.get(iterator);
            assertNotSame(factory, value, "factory remains in " + f.getName());
            if (value instanceof Holder<?> holder) assertNull(holder.value());
        }
    }

    @Test
    public void deferredFailureCachingNullAndRecursiveContractsArePreserved() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final RuntimeException failure = new RuntimeException("once");
        final Supplier<IntIterator> factory = () -> { calls.incrementAndGet(); throw failure; };
        final IntIterator iterator = IntIterator.defer(factory);
        assertSame(failure, assertThrows(RuntimeException.class, iterator::hasNext));
        assertSame(failure, assertThrows(RuntimeException.class, iterator::nextInt));
        assertEquals(1, calls.get());
        assertFactoryReleased(iterator, factory);
        assertThrows(IllegalStateException.class, () -> IntIterator.defer(() -> null).hasNext());
        assertFalse(ObjIterator.defer(() -> null).hasNext());
        final IntIterator[] recursive = new IntIterator[1];
        recursive[0] = IntIterator.defer(() -> { recursive[0].hasNext(); return IntIterator.empty(); });
        final IllegalStateException recursion = assertThrows(IllegalStateException.class, recursive[0]::hasNext);
        assertSame(recursion, assertThrows(IllegalStateException.class, recursive[0]::hasNext));
    }

    @Test
    public void everyDeferredPrimitiveCachesFailuresNullAndRecursiveInitialization() throws Exception {
        for (final String name : NAMES) {
            final Class<?> type = Class.forName("com.landawn.abacus.util." + name + "Iterator");
            for (final Throwable failure : new Throwable[] { new IllegalArgumentException(name), new AssertionError(name) }) {
                final AtomicInteger calls = new AtomicInteger();
                final Supplier<Object> factory = () -> {
                    calls.incrementAndGet();
                    if (failure instanceof RuntimeException runtime) throw runtime;
                    throw (Error) failure;
                };
                final Iterator<?> iterator = (Iterator<?>) type.getMethod("defer", Supplier.class).invoke(null, factory);
                assertSame(failure, assertThrows(failure.getClass(), iterator::hasNext), name);
                assertSame(failure, assertThrows(failure.getClass(), iterator::next), name);
                assertEquals(1, calls.get(), name);
                assertFactoryReleased(iterator, factory);
            }
            final Supplier<Object> nullFactory = () -> null;
            final Iterator<?> nullIterator = (Iterator<?>) type.getMethod("defer", Supplier.class).invoke(null, nullFactory);
            final IllegalStateException nullFailure = assertThrows(IllegalStateException.class, nullIterator::hasNext);
            assertSame(nullFailure, assertThrows(IllegalStateException.class, nullIterator::next), name);
            assertFactoryReleased(nullIterator, nullFactory);
            for (final boolean selfReturn : new boolean[] { false, true }) {
                final Iterator<?>[] reference = new Iterator<?>[1];
                final Supplier<Object> recursive = () -> {
                    if (!selfReturn) reference[0].hasNext();
                    return reference[0];
                };
                reference[0] = (Iterator<?>) type.getMethod("defer", Supplier.class).invoke(null, recursive);
                final IllegalStateException failure = assertThrows(IllegalStateException.class, reference[0]::hasNext);
                assertSame(failure, assertThrows(IllegalStateException.class, reference[0]::next), name);
                assertFactoryReleased(reference[0], recursive);
            }
        }
    }

    @Test
    public void fullCharAndShortDomainsAndCustomSetSuppliersRemainSupported() {
        final char[] chars = new char[65_536];
        final short[] shorts = new short[65_536];
        for (int i = 0; i < chars.length; i++) { chars[i] = (char) i; shorts[i] = (short) i; }
        assertEquals(65_536, CharList.of(chars).toSet().size());
        assertEquals(65_536, CharStream.of(chars).toSet().size());
        assertEquals(65_536, ShortList.of(shorts).toSet().size());
        assertEquals(65_536, ShortStream.of(shorts).toSet().size());
        final AtomicInteger requestedCapacity = new AtomicInteger();
        final Set<Byte> custom = ByteList.of(new byte[1_000]).toCollection(size -> {
            requestedCapacity.set(size);
            return new java.util.LinkedHashSet<>();
        });
        assertEquals(1_000, requestedCapacity.get());
        assertEquals(Set.of((byte) 0), custom);
    }
}
