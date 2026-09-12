package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class NRotateTest extends NTestSupport {

    @Test
    public void testTopNaNSelectionDependsOnRequestedCount() {
        float[] floats = { Float.NaN, 1f, Float.NaN };
        double[] doubles = { Double.NaN, 1d, Double.NaN };
        assertArrayEquals(new float[0], N.top(floats, 0));
        assertArrayEquals(new float[0], N.top(floats, 0, 2, 0));
        assertArrayEquals(new double[0], N.top(doubles, 0));
        assertArrayEquals(new double[0], N.top(doubles, 0, 2, 0));
        assertArrayEquals(new float[] { Float.NaN }, N.top(new float[] { Float.NaN, Float.NaN }, 1, Comparator.reverseOrder()));
        assertArrayEquals(new double[] { Double.NaN }, N.top(new double[] { Double.NaN, Double.NaN }, 0, 2, 1, Comparator.reverseOrder()));

        float[] selectedFloats = N.top(floats, 2, Comparator.reverseOrder());
        double[] selectedDoubles = N.top(doubles, 0, 3, 2, Comparator.reverseOrder());
        Arrays.sort(selectedFloats);
        Arrays.sort(selectedDoubles);
        assertArrayEquals(new float[] { 1f, Float.NaN }, selectedFloats);
        assertArrayEquals(new double[] { 1d, Double.NaN }, selectedDoubles);
        assertArrayEquals(floats, N.top(floats, 10, Comparator.reverseOrder()));
        assertArrayEquals(doubles, N.top(doubles, 0, 3, 10, Comparator.reverseOrder()));
    }

    @Test
    public void testRotate() {
        byte[] bytes = { 1, 2, 3, 4, 5, 6 };
        CommonUtil.rotate(bytes, 2);
        assertArrayEquals(new byte[] { 5, 6, 1, 2, 3, 4 }, bytes);

        short[] shorts = { 1, 2, 3, 4, 5, 6 };
        CommonUtil.rotate(shorts, 2);
        assertArrayEquals(new short[] { 5, 6, 1, 2, 3, 4 }, shorts);

        int[] ints = { 1, 2, 3, 4, 5, 6 };
        CommonUtil.rotate(ints, 2);
        assertArrayEquals(new int[] { 5, 6, 1, 2, 3, 4 }, ints);

        long[] longs = { 1, 2, 3, 4, 5, 6 };
        CommonUtil.rotate(longs, 2);
        assertArrayEquals(new long[] { 5, 6, 1, 2, 3, 4 }, longs);

        float[] floats = { 1, 2, 3, 4, 5, 6 };
        CommonUtil.rotate(floats, 2);
        assertArrayEquals(new float[] { 5, 6, 1, 2, 3, 4 }, floats);

        double[] doubles = { 1, 2, 3, 4, 5, 6 };
        CommonUtil.rotate(doubles, 2);
        assertArrayEquals(new double[] { 5, 6, 1, 2, 3, 4 }, doubles);

        String[] strings = { "1", "2", "3", "4", "5", "6" };
        CommonUtil.rotate(strings, 2);
        assertArrayEquals(new String[] { "5", "6", "1", "2", "3", "4" }, strings);

        List<String> list = CommonUtil.toList("1", "2", "3", "4", "5", "6");
        CommonUtil.rotate(list, 2);
        assertEquals(CommonUtil.toList("5", "6", "1", "2", "3", "4"), list);
    }

    @Test
    public void testSort() {
        int[] ints = { 5, 1, 3, 2, 4 };
        CommonUtil.sort(ints);
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, ints);
        int[] parallelInts = { 3, 1, 2 };
        CommonUtil.parallelSort(parallelInts);
        assertArrayEquals(new int[] { 1, 2, 3 }, parallelInts);

        long[] longs = { 5L, 1L, 3L, 2L, 4L };
        CommonUtil.sort(longs);
        assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, longs);
        CommonUtil.parallelSort(new long[] { 5L, 1L, 3L, 2L, 4L });

        double[] doubles = { 5D, 1D, 3D, 2D, 4D };
        CommonUtil.sort(doubles);
        assertArrayEquals(new double[] { 1D, 2D, 3D, 4D, 5D }, doubles);

        String[] strings = { "c", "a", "b" };
        CommonUtil.sort(strings);
        assertArrayEquals(new String[] { "a", "b", "c" }, strings);
        CommonUtil.parallelSort(new String[] { "c", "a", "b" });

        int[] values = Array.random(16);
        int[] sorted = values.clone();
        CommonUtil.sort(sorted);
        assertTrue(CommonUtil.isSorted(sorted));
    }

    @Test
    public void testToList_arrays() {
        assertEquals(false, CommonUtil.toList(new boolean[] { true, false, false, true, false }).get(2).booleanValue());
        assertEquals('1', CommonUtil.toList(new char[] { '3', '2', '1', '4', '5' }).get(2).charValue());
        assertEquals(1, CommonUtil.toList(new byte[] { 3, 2, 1, 4, 5 }).get(2).intValue());
        assertEquals(1, CommonUtil.toList(new short[] { 3, 2, 1, 4, 5 }).get(2).intValue());
        assertEquals(1, CommonUtil.toList(new int[] { 3, 2, 1, 4, 5 }).get(2).intValue());
        assertEquals(1, CommonUtil.toList(new long[] { 3, 2, 1, 4, 5 }).get(2).intValue());
        assertEquals(1, CommonUtil.toList(new float[] { 3, 2, 1, 4, 5 }).get(2).intValue());
        assertEquals(1, CommonUtil.toList(new double[] { 3, 2, 1, 4, 5 }).get(2).intValue());
        assertEquals("1", CommonUtil.toList(new String[] { "3", "2", "1", "4", "5" }).get(2));
    }

    @Test
    public void testCompare() {
        assertEquals(-1, CommonUtil.compare("a", "bc", (Comparator<String>) String::compareTo));
        assertEquals(0, CommonUtil.compare(1, 1));
        assertTrue(CommonUtil.compare(1, 2) < 0);
        assertTrue(CommonUtil.compare(2, 1) > 0);
        assertEquals(0, CommonUtil.compare(1.0, 1.0));
        assertEquals(0, CommonUtil.compare("a", "a"));
        assertEquals(0, CommonUtil.compare((String) null, (String) null));
        assertTrue(CommonUtil.compare(null, "a") < 0);
        assertTrue(CommonUtil.compare("a", null) > 0);
    }

    @Test
    public void testRequireNonNull() {
        String value = "value";
        assertSame(value, CommonUtil.requireNonNull(value));
        assertThrows(NullPointerException.class, () -> CommonUtil.requireNonNull(null));
    }

    @Test
    public void testConvert() {
        assertEquals(Byte.valueOf((byte) 12), CommonUtil.convert(12L, Byte.class));
        assertEquals(Integer.valueOf(12), CommonUtil.convert(12L, Integer.class));
        assertEquals(Float.valueOf(12f), CommonUtil.convert(12L, Float.class));
        assertEquals(Double.valueOf(12d), CommonUtil.convert(12f, Double.class));
        assertEquals((byte) 12, CommonUtil.convert(12d, byte.class));
        assertEquals(12, CommonUtil.convert(12d, int.class));
    }

    @Test
    public void testLenSizeNotEmpty() {
        assertEquals(5, CommonUtil.len(new int[] { 1, 2, 3, 4, 5 }));
        assertEquals(0, CommonUtil.len((int[]) null));
        assertEquals(3, CommonUtil.len(new String[] { "a", "b", "c" }));
        assertEquals(5, CommonUtil.size(Arrays.asList(1, 2, 3, 4, 5)));
        assertEquals(0, CommonUtil.size((Collection<?>) null));
        assertEquals(0, CommonUtil.size((Map<?, ?>) null));
        assertTrue(CommonUtil.notEmpty(new int[] { 1 }));
        assertFalse(CommonUtil.notEmpty((int[]) null));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1)));
        assertFalse(CommonUtil.notEmpty((String) null));
        assertTrue(CommonUtil.notEmpty("a"));
    }

    @Test
    public void testClone() {
        int[] intArr = { 1, 2, 3 };
        int[] intClone = CommonUtil.clone(intArr);
        assertArrayEquals(intArr, intClone);
        assertNotSame(intArr, intClone);
        assertNull(CommonUtil.clone((int[]) null));
        String[] strArr = { "a", "b", "c" };
        assertArrayEquals(strArr, CommonUtil.clone(strArr));
    }

    @Test
    public void testEquals() {
        assertTrue(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2, 3 }, new int[] { 1, 2, 4 }));
        assertTrue(CommonUtil.equals((int[]) null, (int[]) null));
        assertTrue(CommonUtil.equals(new String[] { null, "a" }, new String[] { null, "a" }));
        assertTrue(CommonUtil.equals("abc", "abc"));
        assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
        assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));
    }

    @Test
    public void testHashCode() {
        assertEquals(CommonUtil.hashCode(new int[] { 1, 2, 3 }), CommonUtil.hashCode(new int[] { 1, 2, 3 }));
        assertNotEquals(CommonUtil.hashCode(new int[] { 1, 2, 3 }), CommonUtil.hashCode(new int[] { 1, 2, 4 }));
        assertEquals(0, CommonUtil.hashCode((String) null));
    }

    @Test
    public void testNewArray() {
        String[] strArray = CommonUtil.newArray(String.class, 5);
        assertEquals(5, strArray.length);
        assertEquals(0, ((Object[]) CommonUtil.newArray(Object.class, 0)).length);
        Map<String, Integer> map = CommonUtil.newLinkedHashMap(10);
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(0, map.size());
    }

    @Test
    public void testCheckArg() {
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNull(null, "arg"));
        assertDoesNotThrow(() -> CommonUtil.checkArgNotNull("valid", "arg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((String) null, "arg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty("", "arg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty((int[]) null, "arg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotEmpty(new int[] {}, "arg"));
        assertDoesNotThrow(() -> {
            CommonUtil.checkArgNotEmpty("valid", "arg");
            CommonUtil.checkArgNotEmpty(new int[] { 1 }, "arg");
        });
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgNotNegative(-1, "arg"));
        assertDoesNotThrow(() -> CommonUtil.checkArgNotNegative(0, "arg"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkArgument(false, "Invalid argument"));
        assertDoesNotThrow(() -> CommonUtil.checkArgument(true, "Valid argument"));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(-1, 2, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(0, 6, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromToIndex(3, 2, 5));
        assertDoesNotThrow(() -> {
            CommonUtil.checkFromToIndex(0, 5, 5);
            CommonUtil.checkFromToIndex(2, 2, 5);
        });
    }

    @Test
    public void testPercentageRangeWithStepIncludesFloatingPointValues() {
        ImmutableSet<Percentage> result = Percentage.range(Percentage._5, Percentage._50, Percentage._5);
        assertTrue(result.contains(Percentage._5));
        assertTrue(result.contains(Percentage._35));
        assertTrue(result.contains(Percentage._45));
        assertFalse(result.contains(Percentage._50));
        assertEquals(9, result.size());

        ImmutableSet<Percentage> closed = Percentage.rangeClosed(Percentage._85, Percentage._99, Percentage._1);
        assertTrue(closed.contains(Percentage._85));
        assertTrue(closed.contains(Percentage._99));
    }

    @Test
    public void testSleepNonPositiveReturnsImmediately() {
        long t0 = System.nanoTime();
        N.sleep(0);
        N.sleep(-5);
        N.sleep(-1, java.util.concurrent.TimeUnit.SECONDS);
        N.sleepUninterruptibly(0);
        N.sleepUninterruptibly(-5);
        N.sleepUninterruptibly(-3, java.util.concurrent.TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000L;
        assertTrue(elapsedMs < 1_000, "non-positive sleep took unexpectedly long: " + elapsedMs + " ms");
    }

    @Test
    public void testShuffleEmptyArrayDoesNotThrow() {
        int[] empty = new int[0];
        CommonUtil.shuffle(empty);
        assertEquals(0, empty.length);
        Integer[] emptyObj = new Integer[0];
        CommonUtil.shuffle(emptyObj);
        assertEquals(0, emptyObj.length);
    }

    @Test
    public void testTopLongRange() {
        long[] a = { 5000L, 1000L, 3000L, 2000L, 4000L };
        long[] r = N.top(a, 1, 4, 2);
        Arrays.sort(r);
        assertArrayEquals(new long[] { 2000L, 3000L }, r);
        long[] full = N.top(a, 1, 4, 10);
        Arrays.sort(full);
        assertArrayEquals(new long[] { 1000L, 2000L, 3000L }, full);
        assertArrayEquals(new long[0], N.top(a, 0, a.length, 0));
    }

    @Test
    public void testExcludeAllToSet_HashSetSource() {
        Set<Integer> c = new HashSet<>(Arrays.asList(1, 2, 3, 4, 5, 6));
        Set<Integer> result = N.excludeAllToSet(c, Arrays.asList(2, 4, 6));
        assertEquals(new HashSet<>(Arrays.asList(1, 3, 5)), result);
    }

    @Test
    public void testUpdateAllList_LinkedListIteratorBranch() {
        List<Integer> list = new LinkedList<>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12));
        N.updateAll(list, x -> x * 10);
        assertEquals(120, list.get(11).intValue());
        assertEquals(10, list.get(0).intValue());
    }

    @Test
    public void testKthLargestFloatDoubleWithNaN() {
        final float[] fa = { 1f, 2f, Float.NaN };
        assertTrue(Float.isNaN(N.kthLargest(fa, 1)));
        assertEquals(2f, N.kthLargest(fa, 2), 0.0f);
        assertEquals(1f, N.kthLargest(fa, 3), 0.0f);

        final double[] da = { 1d, 2d, Double.NaN };
        assertTrue(Double.isNaN(N.kthLargest(da, 1)));
        assertEquals(2d, N.kthLargest(da, 2), 0.0d);
        assertEquals(1d, N.kthLargest(da, 3), 0.0d);
        assertEquals(1f, N.kthLargest(new float[] { 3f, 1f, 2f }, 3), 0.0f);
    }

    @Test
    public void testSleepUninterruptibly_continuesThroughInterrupt() throws Exception {
        final Thread main = Thread.currentThread();
        final Thread interrupter = new Thread(() -> {
            N.sleep(50);
            main.interrupt();
        });
        final long start = System.currentTimeMillis();
        interrupter.start();
        N.sleepUninterruptibly(300);
        final long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 250, "slept only " + elapsed + " ms despite interrupt");
        assertTrue(Thread.interrupted(), "interrupt status should be restored");
        interrupter.join();
    }

    @Test
    public void testContainsDuplicates_dependsOnLengthAndSortedFlag() {
        assertTrue(N.containsDuplicates(new String[] { "a", "b", "a" }, true));
        assertFalse(N.containsDuplicates(new String[] { "a", "b", "a", "c" }, true));
        assertTrue(N.containsDuplicates(new String[] { "a", "b", "a", "c" }, false));
        assertTrue(N.containsDuplicates(new String[] { "a", "a", "b", "c" }, true));
        assertFalse(N.containsDuplicates(new String[] { "a", "b", "c", "d" }, true));
    }

    @Test
    public void testFromJsonNullLiteralPicksTheConfigOverload() {
        final TypeReference<List<String>> listTypeRef = new TypeReference<>() {
        };
        final com.landawn.abacus.type.Type<List<String>> listType = listTypeRef.type();

        assertNull(N.fromJson((String) null, null, Map.class));
        assertNull(N.fromJson((String) null, null, listType));
        assertNull(N.fromXml((String) null, null, Map.class));
        assertNull(N.fromXml((String) null, null, listType));
        assertEquals(CommonUtil.asList("a"), N.fromJson("[\"a\"]", null, listType));

        final com.landawn.abacus.parser.JsonDeserConfig jdc = new com.landawn.abacus.parser.JsonDeserConfig();
        final com.landawn.abacus.parser.XmlDeserConfig xdc = new com.landawn.abacus.parser.XmlDeserConfig();
        assertNull(N.fromJson((String) null, jdc, Map.class));
        assertNull(N.fromXml((String) null, xdc, Map.class));

        final Map<String, Object> def = new HashMap<>();
        assertSame(def, N.fromJson((String) null, def, Map.class));
        assertSame(def, N.fromXml((String) null, def, Map.class));
        assertEquals(CommonUtil.asList("a"), N.fromJson("[\"a\"]", listType));
    }

    @Test
    public void testMaxCollectionRangeCheckRunsFirst() {
        assertThrows(IndexOutOfBoundsException.class, () -> N.max((List<Integer>) null, 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.max((List<Integer>) null, 0, 1, Comparator.<Integer> naturalOrder()));
        assertThrows(IllegalArgumentException.class, () -> N.max((List<Integer>) null, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> N.max(new ArrayList<Integer>(), 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.max((Integer[]) null, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> N.max((Integer[]) null, 0, 0));
        assertEquals(Integer.valueOf(3), N.max(Arrays.asList(3, 1, 2), 0, 2));
    }

    @Test
    public void testTopRanksFloatingPointByTotalOrdering() {
        assertArrayEquals(new double[] { Double.NaN }, N.top(new double[] { 1.0, Double.NaN, 2.0 }, 1));
        assertArrayEquals(new float[] { Float.NaN }, N.top(new float[] { 1.0f, Float.NaN, 2.0f }, 1));

        final double[] topTwo = N.top(new double[] { 1.0, Double.NaN, 2.0, 3.0 }, 0, 4, 2);
        Arrays.sort(topTwo);
        assertArrayEquals(new double[] { 3.0, Double.NaN }, topTwo);

        final double[] bottomTwo = N.top(new double[] { 1.0, Double.NaN, 2.0 }, 2, Comparator.<Double> reverseOrder());
        Arrays.sort(bottomTwo);
        assertArrayEquals(new double[] { 1.0, 2.0 }, bottomTwo);

        final float[] bottomTwoF = N.top(new float[] { 1.0f, Float.NaN, 2.0f, 3.0f }, 0, 4, 2, Comparator.<Float> reverseOrder());
        Arrays.sort(bottomTwoF);
        assertArrayEquals(new float[] { 1.0f, 2.0f }, bottomTwoF);

        assertTrue(Double.isNaN(N.min(new double[] { 1.0, Double.NaN, 2.0 })));
        assertTrue(Float.isNaN(N.min(new float[] { 1.0f, Float.NaN, 2.0f })));
        assertArrayEquals(new double[] { 3.0 }, N.top(new double[] { 1.0, 3.0, 2.0 }, 1));
    }

    @Test
    public void testRecursiveEqualsAndHashCodeContractsByMap() {
        final int OBJECT = 1;
        final int ARRAY = 2;
        final int ARRAY_RANGE = 3;
        final int DEEP = 4;
        final int EVERYTHING = 5;

        class EqualityHashKey {
            private final int mode;
            private final Object value;
            private final int fromIndex;
            private final int length;

            EqualityHashKey(final int mode, final Object value) {
                this(mode, value, 0, value instanceof Object[] ? ((Object[]) value).length : 0);
            }

            EqualityHashKey(final int mode, final Object value, final int fromIndex, final int length) {
                this.mode = mode;
                this.value = value;
                this.fromIndex = fromIndex;
                this.length = length;
            }

            @Override
            public boolean equals(final Object obj) {
                if (this == obj) {
                    return true;
                } else if (!(obj instanceof EqualityHashKey)) {
                    return false;
                }
                final EqualityHashKey other = (EqualityHashKey) obj;
                if (mode != other.mode) {
                    return false;
                }
                switch (mode) {
                    case OBJECT:
                        return CommonUtil.equals(value, other.value);
                    case ARRAY:
                        return CommonUtil.equals((Object[]) value, (Object[]) other.value);
                    case ARRAY_RANGE:
                        return length == other.length && CommonUtil.equals((Object[]) value, fromIndex, (Object[]) other.value, other.fromIndex, length);
                    case DEEP:
                        return CommonUtil.deepEquals(value, other.value);
                    case EVERYTHING:
                        return CommonUtil.equalsEverything(value, other.value);
                    default:
                        return false;
                }
            }

            @Override
            public int hashCode() {
                switch (mode) {
                    case OBJECT:
                        return CommonUtil.hashCode(value);
                    case ARRAY:
                        return CommonUtil.hashCode((Object[]) value);
                    case ARRAY_RANGE:
                        return CommonUtil.hashCode((Object[]) value, fromIndex, fromIndex + length);
                    case DEEP:
                        return CommonUtil.deepHashCode(value);
                    case EVERYTHING:
                        return Long.hashCode(CommonUtil.hashCodeEverything(value));
                    default:
                        return 0;
                }
            }
        }

        final Object objectLeft = new Object[] { "a", new int[] { 1, 2 }, new Object[] { new byte[] { 3, 4 }, null } };
        final Object objectRight = new Object[] { "a", new int[] { 1, 2 }, new Object[] { new byte[] { 3, 4 }, null } };
        assertFalse(CommonUtil.equals(objectLeft, objectRight));
        assertTrue(CommonUtil.deepEquals(objectLeft, objectRight));

        Map<EqualityHashKey, String> map = new HashMap<>();
        map.put(new EqualityHashKey(OBJECT, objectLeft), "object");
        assertEquals("object", map.get(new EqualityHashKey(OBJECT, objectLeft)));
        assertNull(map.get(new EqualityHashKey(OBJECT, objectRight)));

        final Object[] arrayLeft = { "a", "b" };
        final Object[] arrayRight = { "a", "b" };
        assertTrue(CommonUtil.equals(arrayLeft, arrayRight));
        map = new HashMap<>();
        map.put(new EqualityHashKey(ARRAY, arrayLeft), "array");
        assertEquals("array", map.get(new EqualityHashKey(ARRAY, arrayRight)));

        final Object[] rangeLeft = { "left-skip", "mid-a", "mid-b", "left-tail" };
        final Object[] rangeRight = { "right-skip", "mid-a", "mid-b", "right-tail" };
        assertTrue(CommonUtil.equals(rangeLeft, 1, rangeRight, 1, 2));
        map = new HashMap<>();
        map.put(new EqualityHashKey(ARRAY_RANGE, rangeLeft, 1, 2), "arrayRange");
        assertEquals("arrayRange", map.get(new EqualityHashKey(ARRAY_RANGE, rangeRight, 1, 2)));

        final Object deepLeft = new Object[] { new short[] { 1, 2 }, new Object[] { new double[] { 3.0d, 4.0d } } };
        final Object deepRight = new Object[] { new short[] { 1, 2 }, new Object[] { new double[] { 3.0d, 4.0d } } };
        assertTrue(CommonUtil.deepEquals(deepLeft, deepRight));
        map = new HashMap<>();
        map.put(new EqualityHashKey(DEEP, deepLeft), "deep");
        assertEquals("deep", map.get(new EqualityHashKey(DEEP, deepRight)));

        final Map<String, Object> everythingLeft = new LinkedHashMap<>();
        everythingLeft.put("numbers", new int[] { 1, 2 });
        everythingLeft.put("nested", Arrays.asList(new Object[] { new byte[] { 3, 4 }, "x" }, null));
        final Map<String, Object> everythingRight = new LinkedHashMap<>();
        everythingRight.put("numbers", new int[] { 1, 2 });
        everythingRight.put("nested", Arrays.asList(new Object[] { new byte[] { 3, 4 }, "x" }, null));
        assertTrue(CommonUtil.equalsEverything(everythingLeft, everythingRight));
        map = new HashMap<>();
        map.put(new EqualityHashKey(EVERYTHING, everythingLeft), "everything");
        assertEquals("everything", map.get(new EqualityHashKey(EVERYTHING, everythingRight)));
    }
}
