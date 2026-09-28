package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommonUtilEqualsTest extends CommonUtilTestSupport {

    @Test
    public void testEquals_primitives() {
        assertTrue(CommonUtil.equals(true, true));
        assertFalse(CommonUtil.equals(true, false));
        assertTrue(CommonUtil.equals('a', 'a'));
        assertFalse(CommonUtil.equals('a', 'b'));
        assertTrue(CommonUtil.equals((byte) 1, (byte) 1));
        assertFalse(CommonUtil.equals((byte) 1, (byte) 2));
        assertTrue(CommonUtil.equals((short) 1, (short) 1));
        assertFalse(CommonUtil.equals((short) 1, (short) 2));
        assertTrue(CommonUtil.equals(1, 1));
        assertFalse(CommonUtil.equals(1, 2));
        assertTrue(CommonUtil.equals(1L, 1L));
        assertFalse(CommonUtil.equals(1L, 2L));
        assertTrue(CommonUtil.equals(1.0f, 1.0f));
        assertFalse(CommonUtil.equals(1.0f, 1.1f));
        assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));
        assertTrue(CommonUtil.equals(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        assertTrue(CommonUtil.equals(1.0, 1.0));
        assertFalse(CommonUtil.equals(1.0, 1.1));
        assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
        assertTrue(CommonUtil.equals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    public void testEquals_objectsAndStrings() {
        assertTrue(CommonUtil.equals("test", "test"));
        assertFalse(CommonUtil.equals("test", "Test"));
        assertTrue(CommonUtil.equals((String) null, (String) null));
        assertFalse(CommonUtil.equals("test", null));
        assertFalse(CommonUtil.equals(null, "test"));
        assertTrue(CommonUtil.equals(Integer.valueOf(5), Integer.valueOf(5)));
        assertFalse(CommonUtil.equals(Integer.valueOf(5), Integer.valueOf(6)));
        assertTrue(CommonUtil.equals((Object) null, (Object) null));
        assertFalse(CommonUtil.equals((Object) null, "a"));
        assertFalse(CommonUtil.equals("a", (Object) null));
    }

    @Test
    public void testEquals_arrays() {
        assertTrue(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        assertFalse(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { false, true }));
        assertTrue(CommonUtil.equals((boolean[]) null, (boolean[]) null));
        assertFalse(CommonUtil.equals(new boolean[] { true }, null));
        assertFalse(CommonUtil.equals(new boolean[] { true }, new boolean[] { true, false }));

        assertTrue(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));
        assertFalse(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'a' }));
        assertTrue(CommonUtil.equals(new byte[] { 1, 2 }, new byte[] { 1, 2 }));
        assertTrue(CommonUtil.equals(new short[] { 1, 2 }, new short[] { 1, 2 }));
        assertTrue(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 2, 1 }));
        assertTrue(CommonUtil.equals((int[]) null, (int[]) null));
        assertTrue(CommonUtil.equals(new long[] { 1L, 2L }, new long[] { 1L, 2L }));
        assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));
        assertTrue(CommonUtil.equals(new float[] { Float.NaN }, new float[] { Float.NaN }));
        assertTrue(CommonUtil.equals(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));
        assertTrue(CommonUtil.equals(new double[] { Double.NaN }, new double[] { Double.NaN }));
        assertTrue(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "a", "b" }));
        assertFalse(CommonUtil.equals(new String[] { "a", "b" }, new String[] { "b", "a" }));
        assertTrue(CommonUtil.equals(new String[] { "a", null, "c" }, new String[] { "a", null, "c" }));
        assertFalse(CommonUtil.equals(new int[] { 1 }, new long[] { 1 }));
    }

    @Test
    public void testEquals_arraysWithDelta() {
        assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 1.01f, 2.01f, 3.01f }, 0.02f));
        assertFalse(CommonUtil.equals(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 1.1f, 2.1f, 3.1f }, 0.02f));
        assertTrue(CommonUtil.equals((float[]) null, null, 0.01f));
        assertFalse(CommonUtil.equals(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 1.0f, 2.0f }, 0.01f));

        assertTrue(CommonUtil.equals(new double[] { 1.0, 2.0, 3.0 }, new double[] { 1.01, 2.01, 3.01 }, 0.02));
        assertFalse(CommonUtil.equals(new double[] { 1.0, 2.0, 3.0 }, new double[] { 1.1, 2.1, 3.1 }, 0.02));
        assertTrue(CommonUtil.equals((double[]) null, null, 0.01));
    }

    @Test
    public void testEquals_arrayRange() {
        assertTrue(CommonUtil.equals(new boolean[] { true, false, true, false }, 1, new boolean[] { false, false, true, true }, 1, 2));
        assertFalse(CommonUtil.equals(new boolean[] { true, false, true, false }, 0, new boolean[] { false, false, true, true }, 0, 2));
        assertTrue(CommonUtil.equals(new boolean[] { true, false }, 0, new boolean[] { true, false }, 0, 0));

        assertTrue(CommonUtil.equals(new char[] { 'a', 'b', 'c', 'd' }, 1, new char[] { 'x', 'b', 'c', 'y' }, 1, 2));
        assertTrue(CommonUtil.equals(new byte[] { 1, 2, 3, 4 }, 1, new byte[] { 0, 2, 3, 0 }, 1, 2));
        assertTrue(CommonUtil.equals(new byte[] { 1, 2, 3 }, 0, new byte[] { 9, 8, 7 }, 0, 0));
        byte[] same = { 1, 2, 3 };
        assertTrue(CommonUtil.equals(same, 1, same, 1, 2));
        assertTrue(CommonUtil.equals(new short[] { 1, 2, 3, 4 }, 1, new short[] { 0, 2, 3, 0 }, 1, 2));
        assertTrue(CommonUtil.equals(new int[] { 1, 2, 3, 4 }, 1, new int[] { 0, 2, 3, 0 }, 1, 2));
        assertTrue(CommonUtil.equals(new long[] { 1L, 2L, 3L, 4L }, 1, new long[] { 0L, 2L, 3L, 0L }, 1, 2));
        assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, 1, new float[] { 0.0f, 2.0f, 3.0f, 0.0f }, 1, 2));
        assertTrue(CommonUtil.equals(new float[] { Float.NaN, 2.0f }, 0, new float[] { Float.NaN, 2.0f }, 0, 2));
        assertTrue(CommonUtil.equals(new float[] { 1.0f, 2.0f }, 0, new float[] { 9.0f, 8.0f }, 0, 0));
        assertTrue(CommonUtil.equals(new double[] { 1.0, 2.0, Double.NaN, 4.0 }, 1, new double[] { 1.0, 2.0, Double.NaN, 4.0 }, 1, 2));
        assertTrue(CommonUtil.equals(new String[] { "a", "b", "c", "d" }, 1, new String[] { "x", "b", "c", "y" }, 1, 2));
        assertFalse(CommonUtil.equals(new Integer[] { 1, 2, 3 }, 0, new Long[] { 1L, 2L, 3L }, 0, 2));

        boolean[] bools = { true, false, true, true };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(bools, 0, bools, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(bools, 0, bools, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(new long[] { 1L, 2L }, 0, new long[] { 1L, 2L }, 0, 10));
    }

    @Test
    public void testEquals_objectOverloadDoesNotCompareArrayContents() {
        int[] a = { 1, 2, 3 };
        int[] b = { 1, 2, 3 };
        assertFalse(CommonUtil.equals((Object) a, (Object) b));
        assertTrue(CommonUtil.equals((Object) a, (Object) a));
        assertTrue(CommonUtil.equals(a, b));
        assertTrue(CommonUtil.deepEquals(a, b));

        String[] s1 = { "x", "y" };
        String[] s2 = { "x", "y" };
        assertFalse(CommonUtil.equals((Object) s1, (Object) s2));
        assertTrue(CommonUtil.equals(s1, s2));
    }

    @Test
    public void testEqualsIgnoreCase() {
        assertTrue(CommonUtil.equalsIgnoreCase("test", "TEST"));
        assertTrue(CommonUtil.equalsIgnoreCase((String) null, (String) null));
        assertFalse(CommonUtil.equalsIgnoreCase("test", null));

        assertTrue(CommonUtil.equalsIgnoreCase(new String[] { "Hello", "World", "TEST" }, new String[] { "hello", "WORLD", "test" }));
        assertFalse(CommonUtil.equalsIgnoreCase(new String[] { "Hello", "World", "TEST" }, new String[] { "hello", "WORLD", "testing" }));
        assertTrue(CommonUtil.equalsIgnoreCase(new String[] { "Hello", null, "TEST" }, new String[] { "hello", null, "test" }));
        assertTrue(CommonUtil.equalsIgnoreCase((String[]) null, (String[]) null));

        String[] a = { "a", "Hello", "World", "d" };
        String[] b = { "x", "HELLO", "world", "y" };
        assertTrue(CommonUtil.equalsIgnoreCase(a, 1, b, 1, 2));
        assertFalse(CommonUtil.equalsIgnoreCase(a, 0, b, 0, 2));
        assertTrue(CommonUtil.equalsIgnoreCase(a, 0, a, 0, 2));
        assertTrue(CommonUtil.equalsIgnoreCase(a, 0, b, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equalsIgnoreCase(a, 0, b, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equalsIgnoreCase(a, 0, b, 0, 5));
    }

    @Test
    public void testEqualsInOrder() {
        assertTrue(CommonUtil.equalsInOrder(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b", "c")));
        assertFalse(CommonUtil.equalsInOrder(Arrays.asList("a", "b", "c"), Arrays.asList("c", "b", "a")));
        assertFalse(CommonUtil.equalsInOrder(Arrays.asList("a", "b"), Arrays.asList("a")));
        assertTrue(CommonUtil.equalsInOrder((Collection<?>) null, (Collection<?>) null));
        assertFalse(CommonUtil.equalsInOrder(Arrays.asList("a"), (Collection<?>) null));

        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("a", 1);
        m2.put("b", 2);
        Map<String, Integer> reordered = new LinkedHashMap<>();
        reordered.put("b", 2);
        reordered.put("a", 1);
        assertTrue(CommonUtil.equalsInOrder(m1, m2));
        assertFalse(CommonUtil.equalsInOrder(m1, reordered));
        assertFalse(CommonUtil.equalsInOrder(m1, Collections.singletonMap("a", 1)));
        assertTrue(CommonUtil.equalsInOrder((Map<?, ?>) null, (Map<?, ?>) null));
        assertTrue(CommonUtil.equalsInOrder(new HashMap<>(), (Map<?, ?>) null));
    }

    @Test
    public void testEqualsByKeys() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.put("d", 4);
        Map<String, Integer> map3 = new HashMap<>(map1);
        Map<String, Integer> map4 = new HashMap<>(map1);
        map4.put("b", 20);

        assertTrue(CommonUtil.equalsByKeys(map1, map3, Arrays.asList("a", "b", "c")));
        assertTrue(CommonUtil.equalsByKeys(map1, map2, Arrays.asList("a", "b")));
        assertFalse(CommonUtil.equalsByKeys(map1, map2, Arrays.asList("a", "b", "c")));
        assertFalse(CommonUtil.equalsByKeys(map1, map4, Arrays.asList("a", "b", "c")));
        assertTrue(CommonUtil.equalsByKeys(null, null, Arrays.asList("a")));
        assertFalse(CommonUtil.equalsByKeys(map1, null, Arrays.asList("a")));

        Map<String, Integer> nullValue1 = new HashMap<>();
        nullValue1.put("k1", null);
        Map<String, Integer> nullValue2 = new HashMap<>();
        nullValue2.put("k1", null);
        assertTrue(CommonUtil.equalsByKeys(nullValue1, nullValue2, Collections.singletonList("k1")));
        assertFalse(CommonUtil.equalsByKeys(nullValue1, Collections.singletonMap("k1", 1), Collections.singletonList("k1")));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equalsByKeys(map1, map2, Collections.emptyList()));
    }

    @Test
    public void testEqualsByProps() {
        assertDoesNotThrow(() -> {
            class LocalBean {
                private String name;

                public String getName() {
                    return name;
                }

                public void setName(String name) {
                    this.name = name;
                }
            }
            LocalBean bean1 = new LocalBean();
            bean1.setName("test");
            LocalBean bean2 = new LocalBean();
            bean2.setName("test");
            assertTrue(CommonUtil.equalsByProps(bean1, bean2, Arrays.asList("name")));
        });
    }

    @Test
    public void testEqualsByCommonProps() {
        TestBean b1 = new TestBean();
        b1.setName("Alice");
        b1.setValue("v1");
        TestBean b2 = new TestBean();
        b2.setName("Alice");
        b2.setValue("v1");
        assertTrue(CommonUtil.equalsByCommonProps(b1, b2));
        b2.setName("Bob");
        assertFalse(CommonUtil.equalsByCommonProps(b1, b2));
    }

    @Test
    public void testEqualsCollection() {
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.equalsCollection(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testEqualsEverything() {
        assertTrue(CommonUtil.equalsEverything(null, null));
        assertFalse(CommonUtil.equalsEverything(null, "a"));
        final Object o = new Object();
        assertTrue(CommonUtil.equalsEverything(o, o));
        assertDeepEqualAndHash("a", "a");
        assertFalse(CommonUtil.equalsEverything(1, "1"));

        assertDeepEqualAndHash(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 3));
        assertDeepEqualAndHash(new ArrayList<>(Arrays.asList("a", "b")), new LinkedList<>(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.equalsEverything(Arrays.asList(1, 2, 3), Arrays.asList(1, 2, 4)));

        assertTrue(CommonUtil.equalsEverything(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(1, 2, 3).iterator()));
        assertEquals(CommonUtil.hashCodeEverything(Arrays.asList(1, 2, 3).iterator()), CommonUtil.hashCodeEverything(Arrays.asList(1, 2, 3).iterator()));

        final Map<String, Integer> a = new LinkedHashMap<>();
        a.put("x", 1);
        a.put("y", 2);
        final Map<String, Integer> b = new LinkedHashMap<>();
        b.put("x", 1);
        b.put("y", 2);
        assertDeepEqualAndHash(a, b);
        final Map<String, Integer> reordered = new LinkedHashMap<>();
        reordered.put("y", 2);
        reordered.put("x", 1);
        assertFalse(CommonUtil.equalsEverything(a, reordered));

        assertDeepEqualAndHash(new int[] { 1, 2, 3 }, new int[] { 1, 2, 3 });
        assertDeepEqualAndHash(new Object[] { new int[] { 1, 2 } }, new Object[] { new int[] { 1, 2 } });
        assertFalse(CommonUtil.equalsEverything(new int[] { 1 }, new long[] { 1 }));
        assertFalse(CommonUtil.equalsEverything(Arrays.asList("a", "b"), new Object[] { "a", "b" }));

        DatasetRowBean b1 = new DatasetRowBean("Tom", 10);
        DatasetRowBean b2 = new DatasetRowBean("Tom", 10);
        assertDeepEqualAndHash(b1, b2);
        assertFalse(CommonUtil.equalsEverything(b1, new DatasetRowBean("Jerry", 12)));

        final Map<String, Object> left = new LinkedHashMap<>();
        left.put("values", Arrays.asList("alpha", Arrays.asList(1, 2), new int[] { 3, 4 }));
        left.put("flag", true);
        final Map<String, Object> right = new LinkedHashMap<>();
        right.put("values", Arrays.asList("alpha", Arrays.asList(1, 2), new int[] { 3, 4 }));
        right.put("flag", true);
        assertDeepEqualAndHash(left, right);
        final Map<String, Object> diff = new LinkedHashMap<>();
        diff.put("values", Arrays.asList("alpha", Arrays.asList(1, 2), new int[] { 3, 5 }));
        diff.put("flag", true);
        assertFalse(CommonUtil.equalsEverything(left, diff));
    }

    @Test
    public void rangeEquals_nullArrayAcceptedForEmptyRange() {
        assertTrue(N.equals((int[]) null, 0, new int[] { 1 }, 1, 0));
        assertTrue(N.equals((int[]) null, 0, (int[]) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals((int[]) null, 0, new int[] { 1 }, 0, 1));
        assertTrue(N.equals((String[]) null, 0, new String[] { "a" }, 1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equalsIgnoreCase((String[]) null, 0, new String[] { "a" }, 0, 1));
    }

    @Test
    public void testRangeOverloadsTakeACountNotAToIndex() {
        // the trailing 2 is a COUNT: a[1], a[2] are compared with b[0], b[1]
        assertTrue(CommonUtil.equals(new int[] { 9, 1, 2 }, 1, new int[] { 1, 2, 9 }, 0, 2));
        assertFalse(CommonUtil.equals(new int[] { 9, 1, 2 }, 0, new int[] { 1, 2, 9 }, 0, 2));

        assertEquals(0, CommonUtil.compareUnsigned(new byte[] { 1, 2, 3 }, 1, new byte[] { 9, 2, 3 }, 1, 2));

        assertDoesNotThrow(() -> CommonUtil.checkFromIndexSize(1, 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.checkFromIndexSize(1, 3, 3));

        final int[] dest = new int[3];
        CommonUtil.copy(new int[] { 1, 2, 3 }, 1, dest, 0, 2);
        assertTrue(CommonUtil.equals(new int[] { 2, 3, 0 }, dest));
    }

    @Test
    public void testTwoContainerRangeOverloads_onlyCompareAndMismatchAlsoAcceptCollections() {
        // Pins the class-javadoc index bullet of N/CommonUtil: all six two-container range overloads exist
        // over arrays, but only compare and mismatch also have Collection overloads - so e.g.
        // N.equals(c1, 0, c2, 0, 2) does NOT exist and the javadoc must not imply it does.
        final List<String> sixNames = Arrays.asList("equals", "deepEquals", "equalsIgnoreCase", "compare", "compareUnsigned", "mismatch");
        final Set<String> overArrays = new TreeSet<>();
        final Set<String> overCollections = new TreeSet<>();

        for (final Method m : N.class.getMethods()) {
            final Class<?>[] pt = m.getParameterTypes();

            if (!Modifier.isStatic(m.getModifiers()) || !sixNames.contains(m.getName()) || pt.length < 5 || pt[0] != pt[2] || pt[1] != int.class
                    || pt[3] != int.class || pt[4] != int.class) {
                continue; // not a (container, offset, container, offset, count) shape
            }

            if (pt[0].isArray()) {
                overArrays.add(m.getName());
            } else if (Collection.class.isAssignableFrom(pt[0])) {
                overCollections.add(m.getName());
            }
        }

        assertEquals(new TreeSet<>(sixNames), overArrays);
        assertEquals(new TreeSet<>(Arrays.asList("compare", "mismatch")), overCollections);

        for (final String arraysOnly : Arrays.asList("equals", "deepEquals", "equalsIgnoreCase", "compareUnsigned")) {
            assertThrows(NoSuchMethodException.class, () -> N.class.getMethod(arraysOnly, Collection.class, int.class, Collection.class, int.class, int.class));
        }

        assertDoesNotThrow(() -> N.class.getMethod("compare", Collection.class, int.class, Collection.class, int.class, int.class));
        assertDoesNotThrow(() -> N.class.getMethod("mismatch", Collection.class, int.class, Collection.class, int.class, int.class));

        // the same offset+count families are reachable through the public N facade, not only through CommonUtil
        assertDoesNotThrow(() -> N.checkFromIndexSize(1, 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> N.checkFromIndexSize(1, 3, 3));

        final int[] viaN = new int[3];
        N.copy(new int[] { 1, 2, 3 }, 1, viaN, 0, 2);
        assertArrayEquals(new int[] { 2, 3, 0 }, viaN);
    }


    @Test
    public void testEqualsByKeys_nullMapVersusEmptyAndNullValueVersusAbsentKey() {
        Map<String, Integer> empty = new HashMap<>();
        Map<String, Integer> nullValued = new HashMap<>();
        nullValued.put("k", null);

        assertFalse(CommonUtil.equalsByKeys(null, empty, Arrays.asList("k")));
        assertFalse(CommonUtil.equalsByKeys(empty, null, Arrays.asList("k")));
        assertTrue(CommonUtil.equalsInOrder((Map<String, Integer>) null, empty));

        assertFalse(CommonUtil.equalsByKeys(nullValued, empty, Arrays.asList("k")));
        assertFalse(CommonUtil.equalsByKeys(empty, nullValued, Arrays.asList("k")));
        assertTrue(CommonUtil.equalsByKeys(empty, new HashMap<String, Integer>(), Arrays.asList("k")));
    }


    @Test
    public void testEqualsEverythingTreatsPathAsLeafValue() {
        // A Path is an Iterable whose name elements are Paths iterating to themselves; walking it
        // element-wise used to recurse until StackOverflowError.
        final Path p1 = Paths.get("a", "b");
        final Path p2 = Paths.get("a", "b");
        assertTrue(CommonUtil.equalsEverything(p1, p2));
        assertFalse(CommonUtil.equalsEverything(p1, Paths.get("a", "c")));
        assertTrue(CommonUtil.equalsEverything(Arrays.asList(p1, "x"), Arrays.asList(p2, "x")));
        assertFalse(CommonUtil.equalsEverything(p1, Arrays.asList(Paths.get("a"), Paths.get("b"))));
    }

    @Test
    public void testHashCodeEverythingTreatsPathAsLeafValue() {
        final Path p1 = Paths.get("a", "b");
        final Path p2 = Paths.get("a", "b");
        assertEquals(CommonUtil.hashCodeEverything(p1), CommonUtil.hashCodeEverything(p2));
        assertEquals(CommonUtil.hashCodeEverything(Arrays.asList(p1, "x")), CommonUtil.hashCodeEverything(Arrays.asList(p2, "x")));
    }

    // ---- perf review 2026-09-26 G026 begin ----
    // G026-01: the primitive range overloads delegate to Arrays.equals(range); pin them against the element-wise
    // Float.compare / Double.compare / == loop on random offsets/lengths, incl. -0.0, NaN payloads and vector tails.
    @Test
    public void testEqualsRange_primitiveArraysMatchElementWiseReference() {
        final java.util.Random random = new java.util.Random(20260926L);
        final float[] floatPool = { 0.0f, -0.0f, 1.0f, Float.NaN, Float.intBitsToFloat(0x7fc00001), Float.POSITIVE_INFINITY };
        final double[] doublePool = { 0.0, -0.0, 1.0, Double.NaN, Double.longBitsToDouble(0x7ff8000000000001L), Double.NEGATIVE_INFINITY };

        for (int round = 0; round < 400; round++) {
            final int lenA = random.nextInt(80);
            final int lenB = random.nextInt(80);
            final int fromIndexA = random.nextInt(lenA + 1);
            final int fromIndexB = random.nextInt(lenB + 1);
            final int length = random.nextInt(Math.min(lenA - fromIndexA, lenB - fromIndexB) + 1);
            final int base = round % 3 == 0 ? 2 : 1; // tiny alphabets so that long equal runs are common

            final boolean[] za = new boolean[lenA], zb = new boolean[lenB];
            final char[] ca = new char[lenA], cb = new char[lenB];
            final byte[] ba = new byte[lenA], bb = new byte[lenB];
            final short[] sa = new short[lenA], sb = new short[lenB];
            final int[] ia = new int[lenA], ib = new int[lenB];
            final long[] la = new long[lenA], lb = new long[lenB];
            final float[] fa = new float[lenA], fb = new float[lenB];
            final double[] da = new double[lenA], db = new double[lenB];

            for (int i = 0; i < lenA; i++) {
                final int v = random.nextInt(base);
                za[i] = v == 0;
                ca[i] = (char) v;
                ba[i] = (byte) v;
                sa[i] = (short) v;
                ia[i] = v;
                la[i] = v;
                fa[i] = floatPool[random.nextInt(floatPool.length)];
                da[i] = doublePool[random.nextInt(doublePool.length)];
            }

            for (int i = 0; i < lenB; i++) {
                final int v = random.nextInt(base);
                zb[i] = v == 0;
                cb[i] = (char) v;
                bb[i] = (byte) v;
                sb[i] = (short) v;
                ib[i] = v;
                lb[i] = v;
                fb[i] = floatPool[random.nextInt(floatPool.length)];
                db[i] = doublePool[random.nextInt(doublePool.length)];
            }

            if (round % 2 == 0) {
                // copy the compared range of a into b, then (every other time) plant one mismatch
                for (int k = 0; k < length; k++) {
                    zb[fromIndexB + k] = za[fromIndexA + k];
                    cb[fromIndexB + k] = ca[fromIndexA + k];
                    bb[fromIndexB + k] = ba[fromIndexA + k];
                    sb[fromIndexB + k] = sa[fromIndexA + k];
                    ib[fromIndexB + k] = ia[fromIndexA + k];
                    lb[fromIndexB + k] = la[fromIndexA + k];
                    fb[fromIndexB + k] = fa[fromIndexA + k];
                    db[fromIndexB + k] = da[fromIndexA + k];
                }

                if (length > 0 && round % 4 == 0) {
                    final int k = fromIndexB + random.nextInt(length);
                    zb[k] = !zb[k];
                    cb[k]++;
                    bb[k]++;
                    sb[k]++;
                    ib[k]++;
                    lb[k]++;
                    fb[k] = Float.floatToRawIntBits(fb[k]) == 0 ? -0.0f : 0.0f;
                    db[k] = Double.doubleToRawLongBits(db[k]) == 0L ? -0.0 : 0.0;
                }
            }

            boolean expectedZ = true, expectedC = true, expectedB = true, expectedS = true;
            boolean expectedI = true, expectedL = true, expectedF = true, expectedD = true;

            for (int k = 0; k < length; k++) {
                expectedZ &= za[fromIndexA + k] == zb[fromIndexB + k];
                expectedC &= ca[fromIndexA + k] == cb[fromIndexB + k];
                expectedB &= ba[fromIndexA + k] == bb[fromIndexB + k];
                expectedS &= sa[fromIndexA + k] == sb[fromIndexB + k];
                expectedI &= ia[fromIndexA + k] == ib[fromIndexB + k];
                expectedL &= la[fromIndexA + k] == lb[fromIndexB + k];
                expectedF &= Float.compare(fa[fromIndexA + k], fb[fromIndexB + k]) == 0;
                expectedD &= Double.compare(da[fromIndexA + k], db[fromIndexB + k]) == 0;
            }

            final String ctx = "round=" + round + ", fromIndexA=" + fromIndexA + ", fromIndexB=" + fromIndexB + ", length=" + length;
            assertEquals(expectedZ, CommonUtil.equals(za, fromIndexA, zb, fromIndexB, length), ctx);
            assertEquals(expectedC, CommonUtil.equals(ca, fromIndexA, cb, fromIndexB, length), ctx);
            assertEquals(expectedB, CommonUtil.equals(ba, fromIndexA, bb, fromIndexB, length), ctx);
            assertEquals(expectedS, CommonUtil.equals(sa, fromIndexA, sb, fromIndexB, length), ctx);
            assertEquals(expectedI, CommonUtil.equals(ia, fromIndexA, ib, fromIndexB, length), ctx);
            assertEquals(expectedL, CommonUtil.equals(la, fromIndexA, lb, fromIndexB, length), ctx);
            assertEquals(expectedF, CommonUtil.equals(fa, fromIndexA, fb, fromIndexB, length), ctx);
            assertEquals(expectedD, CommonUtil.equals(da, fromIndexA, db, fromIndexB, length), ctx);
        }
    }

    // G026-01: one mismatch at any position of a long run is found; 0.0 vs -0.0 differ, NaN payloads compare equal.
    @Test
    public void testEqualsRange_singleMismatchAtEveryPosition() {
        final int n = 100;

        for (int pos = 0; pos < n; pos++) {
            final boolean[] za = new boolean[n + 3], zb = new boolean[n + 1];
            final short[] sa = new short[n + 3], sb = new short[n + 1];
            final int[] ia = new int[n + 3], ib = new int[n + 1];
            final long[] la = new long[n + 3], lb = new long[n + 1];
            final byte[] ba = new byte[n + 3], bb = new byte[n + 1];
            final char[] ca = new char[n + 3], cb = new char[n + 1];
            final float[] fa = new float[n + 3], fb = new float[n + 1];
            final double[] da = new double[n + 3], db = new double[n + 1];
            zb[1 + pos] = true;
            sb[1 + pos] = -1;
            ib[1 + pos] = 7;
            lb[1 + pos] = 7L;
            bb[1 + pos] = 7;
            cb[1 + pos] = 'x';
            fb[1 + pos] = -0.0f;
            db[1 + pos] = -0.0;

            assertFalse(CommonUtil.equals(za, 3, zb, 1, n));
            assertFalse(CommonUtil.equals(sa, 3, sb, 1, n));
            assertFalse(CommonUtil.equals(ia, 3, ib, 1, n));
            assertFalse(CommonUtil.equals(la, 3, lb, 1, n));
            assertFalse(CommonUtil.equals(ba, 3, bb, 1, n));
            assertFalse(CommonUtil.equals(ca, 3, cb, 1, n));
            assertFalse(CommonUtil.equals(fa, 3, fb, 1, n));
            assertFalse(CommonUtil.equals(da, 3, db, 1, n));
            assertTrue(CommonUtil.equals(za, 3, zb, 1, pos));
            assertTrue(CommonUtil.equals(ia, 3, ib, 1, pos));
            assertTrue(CommonUtil.equals(fa, 3, fb, 1, pos));
            assertTrue(CommonUtil.equals(da, 3, db, 1, pos));
        }

        final float[] nanA = { 1.0f, Float.NaN, Float.intBitsToFloat(0x7fc00001) };
        final float[] nanB = { 1.0f, Float.intBitsToFloat(0x7fc00002), Float.NaN };
        assertTrue(CommonUtil.equals(nanA, 0, nanB, 0, 3));
        final double[] doubleNanA = { Double.NaN, Double.longBitsToDouble(0x7ff8000000000001L) };
        final double[] doubleNanB = { Double.longBitsToDouble(0x7ff8000000000002L), Double.NaN };
        assertTrue(CommonUtil.equals(doubleNanA, 0, doubleNanB, 0, 2));

        // overlapping ranges of the same array
        final int[] same = { 1, 1, 1, 1, 2 };
        assertTrue(CommonUtil.equals(same, 0, same, 1, 3));
        assertFalse(CommonUtil.equals(same, 0, same, 1, 4));
        // validation is unchanged: null arrays only with length 0, bad ranges still throw
        assertTrue(CommonUtil.equals((double[]) null, 0, (double[]) null, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals((float[]) null, 0, new float[1], 0, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(new short[3], 2, new short[3], 0, 2));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new char[3], 0, new char[3], 0, -1));
    }
    // ---- perf review 2026-09-26 G026 end ----
}
