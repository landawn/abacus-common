package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;
import com.landawn.abacus.util.function.BiPredicate;

@Tag("new-test")
public class Difference100Test extends TestBase {

    @Test
    public void testOfBooleanArrays() {
        boolean[] a = { true, false, true, false };
        boolean[] b = { false, true, true };

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, true), diff.inCommon());
        assertEquals(BooleanList.of(false), diff.onLeftOnly());
        assertEquals(BooleanList.of(), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfBooleanArraysEqual() {
        boolean[] a = { true, false, true };
        boolean[] b = { true, false, true };

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, true), diff.inCommon());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfCharArrays() {
        char[] a = { 'a', 'b', 'c', 'd' };
        char[] b = { 'b', 'c', 'e' };

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertEquals(CharList.of('b', 'c'), diff.inCommon());
        assertEquals(CharList.of('a', 'd'), diff.onLeftOnly());
        assertEquals(CharList.of('e'), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfByteArrays() {
        byte[] a = { 1, 2, 3, 4 };
        byte[] b = { 2, 3, 5 };

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.inCommon());
        assertEquals(ByteList.of((byte) 1, (byte) 4), diff.onLeftOnly());
        assertEquals(ByteList.of((byte) 5), diff.onRightOnly());
    }

    @Test
    public void testOfShortArrays() {
        short[] a = { 10, 20, 30 };
        short[] b = { 20, 30, 40 };

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertEquals(ShortList.of((short) 20, (short) 30), diff.inCommon());
        assertEquals(ShortList.of((short) 10), diff.onLeftOnly());
        assertEquals(ShortList.of((short) 40), diff.onRightOnly());
    }

    @Test
    public void testOfIntArrays() {
        int[] a = { 1, 2, 3, 4, 5 };
        int[] b = { 3, 4, 5, 6, 7 };

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(3, 4, 5), diff.inCommon());
        assertEquals(IntList.of(1, 2), diff.onLeftOnly());
        assertEquals(IntList.of(6, 7), diff.onRightOnly());
    }

    @Test
    public void testOfLongArrays() {
        long[] a = { 100L, 200L, 300L };
        long[] b = { 200L, 300L, 400L };

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertEquals(LongList.of(200L, 300L), diff.inCommon());
        assertEquals(LongList.of(100L), diff.onLeftOnly());
        assertEquals(LongList.of(400L), diff.onRightOnly());
    }

    @Test
    public void testOfFloatArrays() {
        float[] a = { 1.1f, 2.2f, 3.3f };
        float[] b = { 2.2f, 3.3f, 4.4f };

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(2.2f, 3.3f), diff.inCommon());
        assertEquals(FloatList.of(1.1f), diff.onLeftOnly());
        assertEquals(FloatList.of(4.4f), diff.onRightOnly());
    }

    @Test
    public void testOfDoubleArrays() {
        double[] a = { 1.11, 2.22, 3.33 };
        double[] b = { 2.22, 3.33, 4.44 };

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(2.22, 3.33), diff.inCommon());
        assertEquals(DoubleList.of(1.11), diff.onLeftOnly());
        assertEquals(DoubleList.of(4.44), diff.onRightOnly());
    }

    @Test
    public void testOfGenericArrays() {
        String[] a = { "apple", "banana", "cherry" };
        String[] b = { "banana", "cherry", "date" };

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("banana", "cherry"), diff.inCommon());
        assertEquals(Arrays.asList("apple"), diff.onLeftOnly());
        assertEquals(Arrays.asList("date"), diff.onRightOnly());
    }

    @Test
    public void testOfCollections() {
        List<Integer> a = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> b = Arrays.asList(3, 4, 5, 6, 7);

        Difference<List<Integer>, List<Integer>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList(3, 4, 5), diff.inCommon());
        assertEquals(Arrays.asList(1, 2), diff.onLeftOnly());
        assertEquals(Arrays.asList(6, 7), diff.onRightOnly());
    }

    @Test
    public void testOfCollectionsWithDuplicates() {
        List<String> a = Arrays.asList("a", "b", "b", "c");
        List<String> b = Arrays.asList("b", "c", "c", "d");

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.inCommon());
        assertEquals(Arrays.asList("a", "b"), diff.onLeftOnly());
        assertEquals(Arrays.asList("c", "d"), diff.onRightOnly());
    }

    @Test
    public void testOfEmptyCollections() {
        List<String> a = new ArrayList<>();
        List<String> b = new ArrayList<>();

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testOfOneEmptyCollection() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = new ArrayList<>();

        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(Arrays.asList("a", "b"), diff.onLeftOnly());
        assertTrue(diff.onRightOnly().isEmpty());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOfBooleanLists() {
        BooleanList a = BooleanList.of(true, false, true);
        BooleanList b = BooleanList.of(false, true, true);

        Difference<BooleanList, BooleanList> diff = Difference.of(a, b);

        assertEquals(BooleanList.of(true, false, true), diff.inCommon());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
    }

    @Test
    public void testOfCharLists() {
        CharList a = CharList.of('x', 'y', 'z');
        CharList b = CharList.of('y', 'z', 'w');

        Difference<CharList, CharList> diff = Difference.of(a, b);

        assertEquals(CharList.of('y', 'z'), diff.inCommon());
        assertEquals(CharList.of('x'), diff.onLeftOnly());
        assertEquals(CharList.of('w'), diff.onRightOnly());
    }

    @Test
    public void testOfByteLists() {
        ByteList a = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        ByteList b = ByteList.of((byte) 2, (byte) 3, (byte) 4);

        Difference<ByteList, ByteList> diff = Difference.of(a, b);

        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.inCommon());
        assertEquals(ByteList.of((byte) 1), diff.onLeftOnly());
        assertEquals(ByteList.of((byte) 4), diff.onRightOnly());
    }

    @Test
    public void testOfShortLists() {
        ShortList a = ShortList.of((short) 10, (short) 20, (short) 30);
        ShortList b = ShortList.of((short) 20, (short) 30, (short) 40);

        Difference<ShortList, ShortList> diff = Difference.of(a, b);

        assertEquals(ShortList.of((short) 20, (short) 30), diff.inCommon());
        assertEquals(ShortList.of((short) 10), diff.onLeftOnly());
        assertEquals(ShortList.of((short) 40), diff.onRightOnly());
    }

    @Test
    public void testOfIntLists() {
        IntList a = IntList.of(100, 200, 300);
        IntList b = IntList.of(200, 300, 400);

        Difference<IntList, IntList> diff = Difference.of(a, b);

        assertEquals(IntList.of(200, 300), diff.inCommon());
        assertEquals(IntList.of(100), diff.onLeftOnly());
        assertEquals(IntList.of(400), diff.onRightOnly());
    }

    @Test
    public void testOfLongLists() {
        LongList a = LongList.of(1000L, 2000L, 3000L);
        LongList b = LongList.of(2000L, 3000L, 4000L);

        Difference<LongList, LongList> diff = Difference.of(a, b);

        assertEquals(LongList.of(2000L, 3000L), diff.inCommon());
        assertEquals(LongList.of(1000L), diff.onLeftOnly());
        assertEquals(LongList.of(4000L), diff.onRightOnly());
    }

    @Test
    public void testOfFloatLists() {
        FloatList a = FloatList.of(1.5f, 2.5f, 3.5f);
        FloatList b = FloatList.of(2.5f, 3.5f, 4.5f);

        Difference<FloatList, FloatList> diff = Difference.of(a, b);

        assertEquals(FloatList.of(2.5f, 3.5f), diff.inCommon());
        assertEquals(FloatList.of(1.5f), diff.onLeftOnly());
        assertEquals(FloatList.of(4.5f), diff.onRightOnly());
    }

    @Test
    public void testOfDoubleLists() {
        DoubleList a = DoubleList.of(1.25, 2.25, 3.25);
        DoubleList b = DoubleList.of(2.25, 3.25, 4.25);

        Difference<DoubleList, DoubleList> diff = Difference.of(a, b);

        assertEquals(DoubleList.of(2.25, 3.25), diff.inCommon());
        assertEquals(DoubleList.of(1.25), diff.onLeftOnly());
        assertEquals(DoubleList.of(4.25), diff.onRightOnly());
    }

    @Test
    public void testEqualsAndHashCode() {
        List<String> a1 = Arrays.asList("a", "b", "c");
        List<String> b1 = Arrays.asList("b", "c", "d");
        Difference<List<String>, List<String>> diff1 = Difference.of(a1, b1);

        List<String> a2 = Arrays.asList("a", "b", "c");
        List<String> b2 = Arrays.asList("b", "c", "d");
        Difference<List<String>, List<String>> diff2 = Difference.of(a2, b2);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());

        List<String> a3 = Arrays.asList("a", "b");
        List<String> b3 = Arrays.asList("b", "c");
        Difference<List<String>, List<String>> diff3 = Difference.of(a3, b3);

        assertNotEquals(diff1, diff3);
        assertNotEquals(diff1, null);
        assertNotEquals(diff1, "not a difference");
        assertEquals(diff1, diff1);
    }

    @Test
    public void testToString() {
        List<String> a = Arrays.asList("a", "b");
        List<String> b = Arrays.asList("b", "c");
        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        String str = diff.toString();
        assertTrue(str.contains("areEqual=false"));
        assertTrue(str.contains("inCommon"));
        assertTrue(str.contains("onLeftOnly"));
        assertTrue(str.contains("onRightOnly"));
    }

    @Test
    public void testMapDifferenceBasic() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        map2.put("c", 4);
        map2.put("d", 5);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(1, diff.inCommon().size());
        assertEquals(Integer.valueOf(2), diff.inCommon().get("b"));

        assertEquals(1, diff.onLeftOnly().size());
        assertEquals(Integer.valueOf(1), diff.onLeftOnly().get("a"));

        assertEquals(1, diff.onRightOnly().size());
        assertEquals(Integer.valueOf(5), diff.onRightOnly().get("d"));

        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(3, 4), diff.withDifferentValues().get("c"));

        assertFalse(diff.areEqual());
    }

    @Test
    public void testMapDifferenceEqual() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("x", "foo");
        map1.put("y", "bar");

        Map<String, String> map2 = new HashMap<>();
        map2.put("x", "foo");
        map2.put("y", "bar");

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2);

        assertEquals(2, diff.inCommon().size());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceWithKeysToCompare() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 4);
        map2.put("d", 5);

        Collection<String> keysToCompare = Arrays.asList("a", "b");

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, keysToCompare);

        assertEquals(1, diff.inCommon().size());
        assertEquals(Integer.valueOf(1), diff.inCommon().get("a"));

        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());

        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(2, 4), diff.withDifferentValues().get("b"));
    }

    @Test
    public void testMapDifferenceWithCustomEquivalence() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "HELLO");
        map1.put("b", "WORLD");

        Map<String, String> map2 = new HashMap<>();
        map2.put("a", "hello");
        map2.put("b", "world");

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2,
                (v1, v2) -> v1.equalsIgnoreCase(v2));

        assertEquals(2, diff.inCommon().size());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceWithTriPredicateEquivalence() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("threshold", 100);
        map1.put("value", 50);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("threshold", 105);
        map2.put("value", 200);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, (k, v1, v2) -> {
            if ("threshold".equals(k)) {
                return Math.abs(v1 - v2) <= 10;
            }
            return v1.equals(v2);
        });

        assertEquals(1, diff.inCommon().size());
        assertTrue(diff.inCommon().containsKey("threshold"));

        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(50, 200), diff.withDifferentValues().get("value"));
    }

    @Test
    public void testMapDifferenceNullValues() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", null);
        map1.put("b", "value");

        Map<String, String> map2 = new HashMap<>();
        map2.put("a", null);
        map2.put("b", null);

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2);

        assertEquals(1, diff.inCommon().size());
        assertNull(diff.inCommon().get("a"));

        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of("value", null), diff.withDifferentValues().get("b"));
    }

    @Test
    public void testMapDifferenceLinkedHashMap() {
        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("b", 2);
        map2.put("a", 1);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.inCommon() instanceof LinkedHashMap);
        assertTrue(diff.onLeftOnly() instanceof LinkedHashMap);
        assertTrue(diff.onRightOnly() instanceof LinkedHashMap);
        assertTrue(diff.withDifferentValues() instanceof LinkedHashMap);
    }

    @Test
    public void testMapDifferenceCollections() {
        Map<String, Integer> map1a = new HashMap<>();
        map1a.put("id", 1);
        map1a.put("value", 100);

        Map<String, Integer> map1b = new HashMap<>();
        map1b.put("id", 2);
        map1b.put("value", 200);

        List<Map<String, Integer>> col1 = Arrays.asList(map1a, map1b);

        Map<String, Integer> map2a = new HashMap<>();
        map2a.put("id", 1);
        map2a.put("value", 101);

        Map<String, Integer> map2c = new HashMap<>();
        map2c.put("id", 3);
        map2c.put("value", 300);

        List<Map<String, Integer>> col2 = Arrays.asList(map2a, map2c);

        MapDifference<List<Map<String, Integer>>, List<Map<String, Integer>>, Map<Integer, MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>>>> diff = MapDifference
                .of(col1, col2, m -> m.get("id"));

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.onLeftOnly().size());
        assertEquals(Integer.valueOf(200), diff.onLeftOnly().get(0).get("value"));

        assertEquals(1, diff.onRightOnly().size());
        assertEquals(Integer.valueOf(300), diff.onRightOnly().get(0).get("value"));

        assertEquals(1, diff.withDifferentValues().size());
        assertTrue(diff.withDifferentValues().containsKey(1));
    }

    public static class TestBean {
        private String name;
        private int value;
        private boolean active;

        public TestBean() {
        }

        public TestBean(String name, int value, boolean active) {
            this.name = name;
            this.value = value;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    @Test
    public void testBeanDifferenceBasic() {
        TestBean bean1 = new TestBean("test", 100, true);
        TestBean bean2 = new TestBean("test", 200, false);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2);

        assertEquals(1, diff.inCommon().size());
        assertEquals("test", diff.inCommon().get("name"));

        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());

        assertEquals(2, diff.withDifferentValues().size());
        assertEquals(Pair.of(100, 200), diff.withDifferentValues().get("value"));
        assertEquals(Pair.of(true, false), diff.withDifferentValues().get("active"));

        assertFalse(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceEqual() {
        TestBean bean1 = new TestBean("same", 42, true);
        TestBean bean2 = new TestBean("same", 42, true);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2);

        assertEquals(3, diff.inCommon().size());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceWithPropNames() {
        TestBean bean1 = new TestBean("test1", 100, true);
        TestBean bean2 = new TestBean("test2", 100, false);

        Collection<String> propsToCompare = Arrays.asList("name", "value");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2, propsToCompare);

        assertEquals(1, diff.inCommon().size());
        assertEquals(100, diff.inCommon().get("value"));

        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of("test1", "test2"), diff.withDifferentValues().get("name"));

        assertFalse(diff.withDifferentValues().containsKey("active"));
    }

    @Test
    public void testBeanDifferenceNullBeans() {
        TestBean bean = new TestBean("test", 100, true);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff1 = BeanDifference.of(null, bean);
        assertTrue(diff1.inCommon().isEmpty());
        assertTrue(diff1.onLeftOnly().isEmpty());
        assertEquals(3, diff1.onRightOnly().size());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff2 = BeanDifference.of(bean, null);
        assertTrue(diff2.inCommon().isEmpty());
        assertEquals(3, diff2.onLeftOnly().size());
        assertTrue(diff2.onRightOnly().isEmpty());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff3 = BeanDifference.of((TestBean) null, (TestBean) null);
        assertTrue(diff3.areEqual());
    }

    @Test
    public void testBeanDifferenceCollections() {
        TestBean bean1a = new TestBean("A", 100, true);
        TestBean bean1b = new TestBean("B", 200, false);
        List<TestBean> col1 = Arrays.asList(bean1a, bean1b);

        TestBean bean2a = new TestBean("A", 101, true);
        TestBean bean2c = new TestBean("C", 300, true);
        List<TestBean> col2 = Arrays.asList(bean2a, bean2c);

        BeanDifference<List<TestBean>, List<TestBean>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(col1, col2, TestBean::getName);

        assertTrue(diff.inCommon().isEmpty());

        assertEquals(1, diff.onLeftOnly().size());
        assertEquals("B", diff.onLeftOnly().get(0).getName());

        assertEquals(1, diff.onRightOnly().size());
        assertEquals("C", diff.onRightOnly().get(0).getName());

        assertEquals(1, diff.withDifferentValues().size());
        assertTrue(diff.withDifferentValues().containsKey("A"));
    }

    @Test
    public void testBeanDifferenceWithCustomEquivalence() {
        TestBean bean1 = new TestBean("TEST", 100, true);
        TestBean bean2 = new TestBean("test", 100, true);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2,
                (propName, v1, v2) -> {
                    if ("name".equals(propName) && v1 instanceof String && v2 instanceof String) {
                        return ((String) v1).equalsIgnoreCase((String) v2);
                    }
                    return CommonUtil.equals(v1, v2);
                });

        assertEquals(3, diff.inCommon().size());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceNonBeanClass() {
        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of("not a bean", new TestBean()));
    }

    @Test
    public void testMapDifferenceNullEquivalence() {
        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();
        assertThrows(IllegalArgumentException.class, () -> MapDifference.of(map1, map2, (BiPredicate<Integer, Integer>) null));
    }

    @Test
    public void testMapDifferenceEqualsHashCodeToString() {
        Map<String, Integer> map1a = new HashMap<>();
        map1a.put("a", 1);
        Map<String, Integer> map2a = new HashMap<>();
        map2a.put("a", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1a, map2a);

        Map<String, Integer> map1b = new HashMap<>();
        map1b.put("a", 1);
        Map<String, Integer> map2b = new HashMap<>();
        map2b.put("a", 2);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(map1b, map2b);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());

        String str = diff1.toString();
        assertTrue(str.contains("withDifferentValues"));
        assertTrue(str.contains("areEqual=false"));
    }
}
