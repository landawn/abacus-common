package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;

/**
 * Unit tests for the Difference class and its inner classes.
 */
public class Difference200Test extends TestBase {

    // Test cases for the base Difference class
    @Test
    public void testOf_booleanArrays() {
        Difference<BooleanList, BooleanList> diff = Difference.of(new boolean[] { true, false, true }, new boolean[] { true, true, false, false });
        assertEquals(BooleanList.of(true, false, true), diff.inCommon());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertEquals(BooleanList.of(false), diff.onRightOnly());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testOf_charArrays() {
        Difference<CharList, CharList> diff = Difference.of(new char[] { 'a', 'b', 'c' }, new char[] { 'b', 'c', 'd' });
        assertEquals(CharList.of('b', 'c'), diff.inCommon());
        assertEquals(CharList.of('a'), diff.onLeftOnly());
        assertEquals(CharList.of('d'), diff.onRightOnly());
    }

    @Test
    public void testOf_byteArrays() {
        Difference<ByteList, ByteList> diff = Difference.of(new byte[] { 1, 2, 3 }, new byte[] { 2, 3, 4 });
        assertEquals(ByteList.of((byte) 2, (byte) 3), diff.inCommon());
        assertEquals(ByteList.of((byte) 1), diff.onLeftOnly());
        assertEquals(ByteList.of((byte) 4), diff.onRightOnly());
    }

    @Test
    public void testOf_shortArrays() {
        Difference<ShortList, ShortList> diff = Difference.of(new short[] { 1, 2, 3 }, new short[] { 2, 3, 4 });
        assertEquals(ShortList.of((short) 2, (short) 3), diff.inCommon());
        assertEquals(ShortList.of((short) 1), diff.onLeftOnly());
        assertEquals(ShortList.of((short) 4), diff.onRightOnly());
    }

    @Test
    public void testOf_intArrays() {
        Difference<IntList, IntList> diff = Difference.of(new int[] { 1, 2, 3 }, new int[] { 2, 3, 4 });
        assertEquals(IntList.of(2, 3), diff.inCommon());
        assertEquals(IntList.of(1), diff.onLeftOnly());
        assertEquals(IntList.of(4), diff.onRightOnly());
    }

    @Test
    public void testOf_longArrays() {
        Difference<LongList, LongList> diff = Difference.of(new long[] { 1L, 2L, 3L }, new long[] { 2L, 3L, 4L });
        assertEquals(LongList.of(2L, 3L), diff.inCommon());
        assertEquals(LongList.of(1L), diff.onLeftOnly());
        assertEquals(LongList.of(4L), diff.onRightOnly());
    }

    @Test
    public void testOf_floatArrays() {
        Difference<FloatList, FloatList> diff = Difference.of(new float[] { 1.0f, 2.0f, 3.0f }, new float[] { 2.0f, 3.0f, 4.0f });
        assertEquals(FloatList.of(2.0f, 3.0f), diff.inCommon());
        assertEquals(FloatList.of(1.0f), diff.onLeftOnly());
        assertEquals(FloatList.of(4.0f), diff.onRightOnly());
    }

    @Test
    public void testOf_doubleArrays() {
        Difference<DoubleList, DoubleList> diff = Difference.of(new double[] { 1.0, 2.0, 3.0 }, new double[] { 2.0, 3.0, 4.0 });
        assertEquals(DoubleList.of(2.0, 3.0), diff.inCommon());
        assertEquals(DoubleList.of(1.0), diff.onLeftOnly());
        assertEquals(DoubleList.of(4.0), diff.onRightOnly());
    }

    @Test
    public void testOf_ObjectArrays() {
        Difference<List<String>, List<String>> diff = Difference.of(new String[] { "a", "b", "c" }, new String[] { "b", "c", "d" });
        assertEquals(Arrays.asList("b", "c"), diff.inCommon());
        assertEquals(Collections.singletonList("a"), diff.onLeftOnly());
        assertEquals(Collections.singletonList("d"), diff.onRightOnly());
    }

    @Test
    public void testOf_Collections() {
        Collection<String> a = Arrays.asList("a", "b", "c", "c");
        Collection<String> b = Arrays.asList("b", "c", "d");
        Difference<List<String>, List<String>> diff = Difference.of(a, b);

        assertEquals(Arrays.asList("b", "c"), diff.inCommon());
        assertEquals(Arrays.asList("a", "c"), diff.onLeftOnly());
        assertEquals(Collections.singletonList("d"), diff.onRightOnly());
    }

    @Test
    public void testOf_Collections_empty() {
        Difference<List<String>, List<String>> diff1 = Difference.of(new ArrayList<>(), new ArrayList<>());
        assertTrue(diff1.inCommon().isEmpty());
        assertTrue(diff1.onLeftOnly().isEmpty());
        assertTrue(diff1.onRightOnly().isEmpty());
        assertTrue(diff1.areEqual());

        Difference<List<String>, List<String>> diff2 = Difference.of(Arrays.asList("a"), new ArrayList<>());
        assertTrue(diff2.inCommon().isEmpty());
        assertEquals(Arrays.asList("a"), diff2.onLeftOnly());
        assertTrue(diff2.onRightOnly().isEmpty());

        Difference<List<String>, List<String>> diff3 = Difference.of(new ArrayList<>(), Arrays.asList("b"));
        assertTrue(diff3.inCommon().isEmpty());
        assertTrue(diff3.onLeftOnly().isEmpty());
        assertEquals(Arrays.asList("b"), diff3.onRightOnly());
    }

    @Test
    public void testDifference_equalsAndHashCode() {
        Difference<List<String>, List<String>> diff1 = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        Difference<List<String>, List<String>> diff2 = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        Difference<List<String>, List<String>> diff3 = Difference.of(Arrays.asList("x", "y"), Arrays.asList("y", "z"));

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
        assertNotEquals(diff1, diff3);
        assertNotEquals(diff1, null);
        assertNotEquals(diff1, new Object());
    }

    @Test
    public void testDifference_toString() {
        Difference<List<String>, List<String>> diff = Difference.of(Arrays.asList("a", "b"), Arrays.asList("b", "c"));
        String expected = "{areEqual=false, inCommon=[b], onLeftOnly=[a], onRightOnly=[c]}";
        assertEquals(expected, diff.toString());
    }

    // Test cases for MapDifference
    @Test
    public void testMapDifference_of() {
        Map<String, Integer> map1 = N.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = N.asMap("b", 2, "c", 4, "d", 5);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertEquals(Collections.singletonMap("b", 2), diff.inCommon());
        assertEquals(Collections.singletonMap("a", 1), diff.onLeftOnly());
        assertEquals(Collections.singletonMap("d", 5), diff.onRightOnly());
        assertEquals(Collections.singletonMap("c", Pair.of(3, 4)), diff.withDifferentValues());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testMapDifference_of_withKeysToCompare() {
        Map<String, Integer> map1 = N.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = N.asMap("b", 2, "c", 4, "d", 5);
        Collection<String> keysToCompare = Arrays.asList("b", "c");

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2, keysToCompare);

        assertEquals(Collections.singletonMap("b", 2), diff.inCommon());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertEquals(Collections.singletonMap("c", Pair.of(3, 4)), diff.withDifferentValues());
    }

    @Test
    public void testMapDifference_of_withValueEquivalence() {
        Map<String, String> map1 = N.asMap("a", "hello", "b", "WORLD");
        Map<String, String> map2 = N.asMap("a", "HELLO", "c", "test");

        MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>> diff = MapDifference.of(map1, map2,
                String::equalsIgnoreCase);

        assertEquals(Collections.singletonMap("a", "hello"), diff.inCommon());
        assertEquals(Collections.singletonMap("b", "WORLD"), diff.onLeftOnly());
        assertEquals(Collections.singletonMap("c", "test"), diff.onRightOnly());
        assertTrue(diff.withDifferentValues().isEmpty());
    }

    @Test
    public void testMapDifference_of_collectionOfMaps() {
        Map<String, String> mapA1 = N.asMap("id", "1", "value", "A");
        Map<String, String> mapA2 = N.asMap("id", "2", "value", "B");
        List<Map<String, String>> listA = Arrays.asList(mapA1, mapA2);

        Map<String, String> mapB1 = N.asMap("id", "2", "value", "C");
        Map<String, String> mapB2 = N.asMap("id", "3", "value", "D");
        List<Map<String, String>> listB = Arrays.asList(mapB1, mapB2);

        MapDifference<List<Map<String, String>>, List<Map<String, String>>, Map<String, MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>>>> diff = MapDifference
                .of(listA, listB, map -> map.get("id"));

        assertEquals(Collections.singletonList(mapA1), diff.onLeftOnly());
        assertEquals(Collections.singletonList(mapB2), diff.onRightOnly());
        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of("B", "C"), diff.withDifferentValues().get("2").withDifferentValues().get("value"));
    }

    @Test
    public void testMapDifference_equalsAndHashCode() {
        Map<String, Integer> map1 = N.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = N.asMap("b", 3, "c", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff1 = MapDifference.of(map1, map2);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff2 = MapDifference.of(map1, map2);

        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    public void testMapDifference_toString() {
        Map<String, Integer> map1 = N.asMap("a", 1, "b", 2);
        Map<String, Integer> map2 = N.asMap("b", 3, "c", 4);
        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);
        String expected = "{areEqual=false, inCommon={}, onLeftOnly={a=1}, onRightOnly={c=4}, withDifferentValues={b=(2, 3)}}";
        assertEquals(expected, diff.toString());
    }

    // Test cases for BeanDifference

    // Helper bean classes for testing
    public static class SimpleBean {
        private int id;
        private String name;

        public SimpleBean(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        // For stable hashcode in tests
        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            SimpleBean that = (SimpleBean) obj;
            return id == that.id && N.equals(name, that.name);
        }
    }

    @Test
    public void testBeanDifference_of() {
        SimpleBean bean1 = new SimpleBean(1, "John");
        SimpleBean bean2 = new SimpleBean(1, "Jane");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2);

        assertTrue(diff.inCommon().containsKey("id") && diff.inCommon().get("id").equals(1));
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertEquals(Pair.of("John", "Jane"), diff.withDifferentValues().get("name"));
        assertFalse(diff.areEqual());
    }

    @Test
    public void testBeanDifference_of_withPropNamesToCompare() {
        SimpleBean bean1 = new SimpleBean(1, "John");
        SimpleBean bean2 = new SimpleBean(2, "John");

        Collection<String> propsToCompare = Collections.singletonList("name");
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2, propsToCompare);

        assertEquals("John", diff.inCommon().get("name"));
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifference_of_collectionOfBeans() {
        List<SimpleBean> list1 = Arrays.asList(new SimpleBean(1, "A"), new SimpleBean(2, "B"));
        List<SimpleBean> list2 = Arrays.asList(new SimpleBean(2, "C"), new SimpleBean(3, "D"));

        BeanDifference<List<SimpleBean>, List<SimpleBean>, Map<Integer, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(list1, list2, SimpleBean::getId);

        assertEquals(1, diff.onLeftOnly().size());
        assertEquals(new SimpleBean(1, "A"), diff.onLeftOnly().get(0));
        assertEquals(1, diff.onRightOnly().size());
        assertEquals(new SimpleBean(3, "D"), diff.onRightOnly().get(0));
        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of("B", "C"), diff.withDifferentValues().get(2).withDifferentValues().get("name"));

    }

    @Test
    public void testBeanDifference_of_nullBeans() {
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of((SimpleBean) null,
                (SimpleBean) null);
        assertTrue(diff.areEqual());

        SimpleBean bean1 = new SimpleBean(1, "A");
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff2 = BeanDifference.of(bean1, null);
        assertFalse(diff2.areEqual());
        assertFalse(diff2.onLeftOnly().isEmpty());
        assertTrue(diff2.onRightOnly().isEmpty());

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff3 = BeanDifference.of(null, bean1);
        assertFalse(diff3.areEqual());
        assertTrue(diff3.onLeftOnly().isEmpty());
        assertFalse(diff3.onRightOnly().isEmpty());
    }

    @Test
    public void testBeanDifference_equalsAndHashCode() {
        SimpleBean bean1 = new SimpleBean(1, "A");
        SimpleBean bean2 = new SimpleBean(2, "B");
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff1 = BeanDifference.of(bean1, bean2);
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff2 = BeanDifference.of(bean1, bean2);
        assertEquals(diff1, diff2);
        assertEquals(diff1.hashCode(), diff2.hashCode());
    }

    @Test
    public void testBeanDifference_toString() {
        SimpleBean bean1 = new SimpleBean(1, "A");
        SimpleBean bean2 = new SimpleBean(1, "B");
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(bean1, bean2);
        String expected = "{areEqual=false, inCommon={id=1}, onLeftOnly={}, onRightOnly={}, withDifferentValues={name=(A, B)}}";
        // Note: property order in toString might not be guaranteed for beans, this check is a bit fragile.
        assertTrue(diff.toString().contains("areEqual=false"));
        assertTrue(diff.toString().contains("inCommon={id=1}"));
        assertTrue(diff.toString().contains("onLeftOnly={}"));
        assertTrue(diff.toString().contains("onRightOnly={}"));
        assertTrue(diff.toString().contains("withDifferentValues={name=(A, B)}"));
    }
}
