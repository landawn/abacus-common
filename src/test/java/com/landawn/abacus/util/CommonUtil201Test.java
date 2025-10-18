package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class CommonUtil201Test extends TestBase {

    public static class TestBean {
        private String name;
        private int age;
        private double salary;
        private boolean active;
        private TestBean nestedBean;
        private List<String> tags;
        private Map<String, String> attributes;

        public TestBean(String name, int age, double salary, boolean active) {
            this.name = name;
            this.age = age;
            this.salary = salary;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public double getSalary() {
            return salary;
        }

        public void setSalary(double salary) {
            this.salary = salary;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        public TestBean getNestedBean() {
            return nestedBean;
        }

        public void setNestedBean(TestBean nestedBean) {
            this.nestedBean = nestedBean;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(List<String> tags) {
            this.tags = tags;
        }

        public Map<String, String> getAttributes() {
            return attributes;
        }

        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        public static List<String> getPropNames() {
            return Arrays.asList("name", "age", "salary", "active", "nestedBean", "tags", "attributes");
        }

        public Object getPropValue(String propName) {
            switch (propName) {
                case "name":
                    return name;
                case "age":
                    return age;
                case "salary":
                    return salary;
                case "active":
                    return active;
                case "nestedBean":
                    return nestedBean;
                case "tags":
                    return tags;
                case "attributes":
                    return attributes;
                default:
                    throw new IllegalArgumentException("No such property: " + propName);
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestBean testBean = (TestBean) o;
            return age == testBean.age && Double.compare(testBean.salary, salary) == 0 && active == testBean.active && Objects.equals(name, testBean.name)
                    && Objects.equals(nestedBean, testBean.nestedBean) && Objects.equals(tags, testBean.tags)
                    && Objects.equals(attributes, testBean.attributes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, age, salary, active, nestedBean, tags, attributes);
        }

        @Override
        public String toString() {
            return "TestBean@" + Integer.toHexString(System.identityHashCode(this));
        }
    }

    private static int mockCompareByProps(final Object bean1, final Object bean2, final Collection<String> propNamesToCompare) {
        if (bean1 == bean2)
            return 0;
        if (bean1 == null || bean2 == null)
            return bean1 == null ? -1 : 1;
        if (!bean1.getClass().equals(bean2.getClass()) || !(bean1 instanceof TestBean)) {
            return 1;
        }

        TestBean b1 = (TestBean) bean1;
        TestBean b2 = (TestBean) bean2;

        for (String propName : propNamesToCompare) {
            Object val1 = b1.getPropValue(propName);
            Object val2 = b2.getPropValue(propName);
            if (!CommonUtil.equals(val1, val2)) {
                return val1 == null ? -1 : (val2 == null ? 1 : String.valueOf(val1).compareTo(String.valueOf(val2)));
            }
        }
        return 0;
    }

    @Test
    public void testEquals_boolean() {
        assertTrue(CommonUtil.equals(true, true));
        assertTrue(CommonUtil.equals(false, false));
        assertFalse(CommonUtil.equals(true, false));
    }

    @Test
    public void testEquals_char() {
        assertTrue(CommonUtil.equals('a', 'a'));
        assertFalse(CommonUtil.equals('a', 'b'));
    }

    @Test
    public void testEquals_byte() {
        assertTrue(CommonUtil.equals((byte) 1, (byte) 1));
        assertFalse(CommonUtil.equals((byte) 1, (byte) 2));
    }

    @Test
    public void testEquals_short() {
        assertTrue(CommonUtil.equals((short) 10, (short) 10));
        assertFalse(CommonUtil.equals((short) 10, (short) 20));
    }

    @Test
    public void testEquals_int() {
        assertTrue(CommonUtil.equals(100, 100));
        assertFalse(CommonUtil.equals(100, 200));
    }

    @Test
    public void testEquals_long() {
        assertTrue(CommonUtil.equals(1000L, 1000L));
        assertFalse(CommonUtil.equals(1000L, 2000L));
    }

    @Test
    public void testEquals_float() {
        assertTrue(CommonUtil.equals(1.0f, 1.0f));
        assertFalse(CommonUtil.equals(1.0f, 1.1f));
        assertTrue(CommonUtil.equals(Float.NaN, Float.NaN));
    }

    @Test
    public void testEquals_double() {
        assertTrue(CommonUtil.equals(1.0, 1.0));
        assertFalse(CommonUtil.equals(1.0, 1.1));
        assertTrue(CommonUtil.equals(Double.NaN, Double.NaN));
    }

    @Test
    public void testEquals_String() {
        assertTrue(CommonUtil.equals("abc", "abc"));
        assertFalse(CommonUtil.equals("abc", "def"));
        assertTrue(CommonUtil.equals((String) null, (String) null));
        assertFalse(CommonUtil.equals("abc", null));
        assertFalse(CommonUtil.equals(null, "abc"));
        assertFalse(CommonUtil.equals("abc", "abcd"));
    }

    @Test
    public void testEqualsIgnoreCase_String() {
        assertTrue(CommonUtil.equalsIgnoreCase("abc", "ABC"));
        assertTrue(CommonUtil.equalsIgnoreCase("abc", "abc"));
        assertFalse(CommonUtil.equalsIgnoreCase("abc", "def"));
        assertTrue(CommonUtil.equalsIgnoreCase((String) null, (String) null));
        assertFalse(CommonUtil.equalsIgnoreCase("abc", null));
        assertFalse(CommonUtil.equalsIgnoreCase(null, "abc"));
    }

    @Test
    public void testEquals_Object() {
        assertTrue(CommonUtil.equals((Object) null, (Object) null));
        assertFalse(CommonUtil.equals(new Object(), null));
        assertFalse(CommonUtil.equals(null, new Object()));

        Object obj1 = new Object();
        Object obj2 = new Object();
        assertTrue(CommonUtil.equals(obj1, obj1));
        assertFalse(CommonUtil.equals(obj1, obj2));

        assertTrue(CommonUtil.equals("Test", "Test"));
        assertFalse(CommonUtil.equals("Test", "test"));

        assertTrue(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 3 }));
        assertFalse(CommonUtil.equals(new int[] { 1, 2 }, new long[] { 1, 2 }));

        assertTrue(CommonUtil.equals(new String[] { "a" }, new String[] { "a" }));
        assertFalse(CommonUtil.equals(new String[] { "a" }, new String[] { "b" }));
        assertFalse(CommonUtil.equals(new String[] { "a" }, new Object[] { new Object() }));
    }

    @Test
    public void testEquals_booleanArray() {
        assertTrue(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        assertFalse(CommonUtil.equals(new boolean[] { true }, new boolean[] { false }));
        assertTrue(CommonUtil.equals((boolean[]) null, (boolean[]) null));
        assertFalse(CommonUtil.equals(new boolean[0], (boolean[]) null));
        assertFalse(CommonUtil.equals(new boolean[] { true }, new boolean[] { true, false }));
    }

    @Test
    public void testEquals_booleanArrayRange() {
        boolean[] a1 = { true, false, true, true };
        boolean[] a2 = { false, true, true, false };
        boolean[] a3 = { true, false, true, true };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 0, a3, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 3, a3, 0, 2));
    }

    @Test
    public void testEquals_intArray() {
        assertTrue(CommonUtil.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(CommonUtil.equals(new int[] { 1 }, new int[] { 2 }));
    }

    @Test
    public void testEquals_intArrayRange() {
        int[] a1 = { 1, 2, 3, 4 };
        int[] a2 = { 0, 2, 3, 5 };
        int[] a3 = { 1, 2, 3, 4 };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 0, a3, 0, 5));
    }

    @Test
    public void testEquals_floatArrayRange() {
        float[] a1 = { 1.0f, 2.0f, Float.NaN, 4.0f };
        float[] a2 = { 0.0f, 2.0f, Float.NaN, 5.0f };
        float[] a3 = { 1.0f, 2.0f, Float.NaN, 4.0f };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEquals_doubleArrayRange() {
        double[] a1 = { 1.0, 2.0, Double.NaN, 4.0 };
        double[] a2 = { 0.0, 2.0, Double.NaN, 5.0 };
        double[] a3 = { 1.0, 2.0, Double.NaN, 4.0 };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEquals_ObjectArray() {
        Object[] oa1 = { "a", 1, null };
        Object[] oa2 = { "a", 1, null };
        Object[] oa3 = { "a", 2, null };
        assertTrue(CommonUtil.equals(oa1, oa2));
        assertFalse(CommonUtil.equals(oa1, oa3));
        assertTrue(CommonUtil.equals((Object[]) null, (Object[]) null));
        assertFalse(CommonUtil.equals(new Object[0], (Object[]) null));
    }

    @Test
    public void testEquals_ObjectArrayRange() {
        Object[] a1 = { "hello", "world", "!" };
        Object[] a2 = { "start", "world", "!" };
        Object[] a3 = { "hello", "world", "!" };

        assertTrue(CommonUtil.equals(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equals(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equals(a1, 0, a1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(a1, 0, a3, 0, 4));
    }

    @Test
    public void testDeepEquals_Object() {
        assertTrue(CommonUtil.deepEquals(null, null));
        assertFalse(CommonUtil.deepEquals(new Object(), null));

        Object[] arr1 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr2 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr3 = { 1, new String[] { "a", "c" }, new int[] { 10, 20 } };
        Object[] arr4 = { 1, new String[] { "a", "b" }, new long[] { 10, 20 } };

        assertTrue(CommonUtil.deepEquals(arr1, arr2));
        assertFalse(CommonUtil.deepEquals(arr1, arr3));
        assertFalse(CommonUtil.deepEquals(arr1, arr4));

        assertTrue(CommonUtil.deepEquals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(CommonUtil.deepEquals(new int[] { 1, 2 }, new int[] { 1, 3 }));
        assertFalse(CommonUtil.deepEquals(new int[] { 1, 2 }, new Integer[] { 1, 2 }));

        assertTrue(CommonUtil.deepEquals("test", "test"));
        assertFalse(CommonUtil.deepEquals("test", "Test"));
    }

    @Test
    public void testDeepEquals_ObjectArray() {
        Object[] a = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] b = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] c = { 1, new String[] { "a", "DIFFERENT" }, new int[] { 10, 20 } };
        assertTrue(CommonUtil.deepEquals(a, b));
        assertFalse(CommonUtil.deepEquals(a, c));
        assertTrue(CommonUtil.deepEquals((Object[]) null, (Object[]) null));
    }

    @Test
    public void testDeepEquals_ObjectArrayRange() {
        Object[] arr1 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };
        Object[] arr2 = { "other", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "end" };
        Object[] arr3 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };

        assertTrue(CommonUtil.deepEquals(arr1, 1, arr3, 1, 3));
        assertFalse(CommonUtil.deepEquals(arr1, 0, arr2, 0, 2));
        assertTrue(CommonUtil.deepEquals(arr1, 0, arr1, 0, 0));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.deepEquals(arr1, 0, arr3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.deepEquals(arr1, 0, arr3, 0, 6));

        Object[] cyclic1 = new Object[1];
        cyclic1[0] = cyclic1;
        Object[] cyclic2 = new Object[1];
        cyclic2[0] = cyclic2;
        Object[] cyclic3 = new Object[1];
        cyclic3[0] = new Object[1];

    }

    @Test
    public void testEqualsIgnoreCase_StringArray() {
        String[] sa1 = { "abc", "DEF", null };
        String[] sa2 = { "ABC", "def", null };
        String[] sa3 = { "abc", "def", "ghi" };
        assertTrue(CommonUtil.equalsIgnoreCase(sa1, sa2));
        assertFalse(CommonUtil.equalsIgnoreCase(sa1, sa3));
        assertTrue(CommonUtil.equalsIgnoreCase((String[]) null, (String[]) null));
    }

    @Test
    public void testEqualsIgnoreCase_StringArrayRange() {
        String[] a1 = { "Hello", "WORLD", "Java", "Test" };
        String[] a2 = { "start", "world", "java", "end" };
        String[] a3 = { "HELLO", "world", "JAVA", "TEST" };

        assertTrue(CommonUtil.equalsIgnoreCase(a1, 1, a3, 1, 2));
        assertFalse(CommonUtil.equalsIgnoreCase(a1, 0, a2, 0, 2));
        assertTrue(CommonUtil.equalsIgnoreCase(a1, 0, a1, 0, 0));

        String[] an1 = { "Test", null, "Me" };
        String[] an2 = { "test", null, "me" };
        String[] an3 = { "test", "NotNULL", "me" };
        assertTrue(CommonUtil.equalsIgnoreCase(an1, 0, an2, 0, 3));
        assertFalse(CommonUtil.equalsIgnoreCase(an1, 0, an3, 0, 3));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equalsIgnoreCase(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equalsIgnoreCase(a1, 0, a3, 0, 5));
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

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("a", 1);
        map3.put("b", 2);
        map3.put("c", 3);

        Map<String, Integer> map4 = new HashMap<>();
        map4.put("a", 1);
        map4.put("b", 20);
        map4.put("c", 3);

        Collection<String> keys = Arrays.asList("a", "b");
        Collection<String> keysWithC = Arrays.asList("a", "b", "c");

        assertTrue(CommonUtil.equalsByKeys(map1, map3, keysWithC));
        assertTrue(CommonUtil.equalsByKeys(map1, map3, keys));
        assertFalse(CommonUtil.equalsByKeys(map1, map2, keysWithC));
        assertTrue(CommonUtil.equalsByKeys(map1, map2, keys));
        assertFalse(CommonUtil.equalsByKeys(map1, map4, keysWithC));

        assertTrue(CommonUtil.equalsByKeys(null, null, keys));
        assertFalse(CommonUtil.equalsByKeys(map1, null, keys));
        assertFalse(CommonUtil.equalsByKeys(null, map1, keys));

        Map<String, Integer> mapWithNullValue1 = new HashMap<>();
        mapWithNullValue1.put("k1", null);
        Map<String, Integer> mapWithNullValue2 = new HashMap<>();
        mapWithNullValue2.put("k1", null);
        Map<String, Integer> mapWithNonNullValue = new HashMap<>();
        mapWithNonNullValue.put("k1", 1);

        assertTrue(CommonUtil.equalsByKeys(mapWithNullValue1, mapWithNullValue2, Collections.singletonList("k1")));
        assertFalse(CommonUtil.equalsByKeys(mapWithNullValue1, mapWithNonNullValue, Collections.singletonList("k1")));

        Map<String, Integer> emptyMap = Collections.emptyMap();
        assertFalse(CommonUtil.equalsByKeys(mapWithNullValue1, emptyMap, Collections.singletonList("k1")));

        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equalsByKeys(map1, map2, Collections.emptyList()));
    }

    @Test
    public void testEqualsByProps() {

        TestBean bean1 = new TestBean("Alice", 30, 50000, true);
        TestBean bean2 = new TestBean("Alice", 30, 50000, true);
        TestBean bean3 = new TestBean("Bob", 30, 50000, true);
        TestBean bean4 = new TestBean("Alice", 31, 50000, true);

        Collection<String> propsAll = TestBean.getPropNames();
        Collection<String> propsNameAge = Arrays.asList("name", "age");

        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByProps(bean1, bean2, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByProps(bean1, bean2, null));

        assertTrue(mockCompareByProps(bean1, bean2, propsAll) == 0);
        assertFalse(mockCompareByProps(bean1, bean3, propsAll) == 0);
        assertTrue(mockCompareByProps(bean1, bean3, Collections.singletonList("age")) == 0);
        assertFalse(mockCompareByProps(bean1, bean4, propsNameAge) == 0);
    }

    @Test
    public void testEqualsByCommonProps() {
        TestBean beanA1 = new TestBean("SameName", 25, 60000, false);
        TestBean beanA2 = new TestBean("SameName", 25, 60000, false);
        TestBean beanB = new TestBean("DifferentName", 25, 60000, false);

        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(null, beanA2));
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(beanA1, null));

    }

    @Test
    public void testHashCode_boolean() {
        assertEquals(1231, CommonUtil.hashCode(true));
        assertEquals(1237, CommonUtil.hashCode(false));
    }

    @Test
    public void testHashCode_char() {
        assertEquals('a', CommonUtil.hashCode('a'));
    }

    @Test
    public void testHashCode_byte() {
        assertEquals((byte) 5, CommonUtil.hashCode((byte) 5));
    }

    @Test
    public void testHashCode_short() {
        assertEquals((short) 100, CommonUtil.hashCode((short) 100));
    }

    @Test
    public void testHashCode_int() {
        assertEquals(12345, CommonUtil.hashCode(12345));
    }

    @Test
    public void testHashCode_long() {
        assertEquals(Long.hashCode(123456789L), CommonUtil.hashCode(123456789L));
    }

    @Test
    public void testHashCode_float() {
        assertEquals(Float.floatToIntBits(1.23f), CommonUtil.hashCode(1.23f));
        assertEquals(Float.floatToIntBits(Float.NaN), CommonUtil.hashCode(Float.NaN));
    }

    @Test
    public void testHashCode_double() {
        assertEquals(Double.hashCode(1.2345), CommonUtil.hashCode(1.2345));
        assertEquals(Double.hashCode(Double.NaN), CommonUtil.hashCode(Double.NaN));
    }

    @Test
    public void testHashCode_Object() {
        assertEquals(0, CommonUtil.hashCode((Object) null));
        String s = "test";
        assertEquals(s.hashCode(), CommonUtil.hashCode(s));

        int[] arr = { 1, 2, 3 };
        assertTrue(CommonUtil.hashCode(arr) != 0);
        assertEquals(CommonUtil.hashCode(arr), CommonUtil.hashCode(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testHashCode_booleanArray() {
        assertEquals(0, CommonUtil.hashCode((boolean[]) null));
        boolean[] ba1 = { true, false };
        boolean[] ba2 = { true, false };
        boolean[] ba3 = { false, true };
        assertEquals(CommonUtil.hashCode(ba1), CommonUtil.hashCode(ba2));
        assertNotEquals(CommonUtil.hashCode(ba1), CommonUtil.hashCode(ba3));
        assertEquals(Arrays.hashCode(new boolean[0]), CommonUtil.hashCode(new boolean[0]));
    }

    @Test
    public void testHashCode_booleanArrayRange() {
        boolean[] arr = { true, false, true, false, true };
        assertEquals(0, CommonUtil.hashCode((boolean[]) null, 0, 0));

        int fullHash = CommonUtil.hashCode(arr, 0, arr.length);
        int partialHash = CommonUtil.hashCode(arr, 1, 3);

        assertNotEquals(0, fullHash);
        assertNotEquals(0, partialHash);
        assertNotEquals(fullHash, partialHash);

        assertEquals(1, CommonUtil.hashCode(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.hashCode(arr, 3, 2));
    }

    @Test
    public void testHashCode_intArray() {
        assertEquals(0, CommonUtil.hashCode((int[]) null));
        int[] arr = { 1, 2 };
        int expected = 1;
        expected = 31 * expected + 1;
        expected = 31 * expected + 2;
        assertEquals(expected, CommonUtil.hashCode(arr));
    }

    @Test
    public void testHashCode_intArrayRange() {
        int[] arr = { 10, 20, 30, 40, 50 };
        int expected = 1;
        expected = 31 * expected + 20;
        expected = 31 * expected + 30;
        assertEquals(expected, CommonUtil.hashCode(arr, 1, 3));
        assertEquals(1, CommonUtil.hashCode(arr, 1, 1));
    }

    @Test
    public void testHashCode_ObjectArray() {
        assertEquals(0, CommonUtil.hashCode((Object[]) null));
        Object[] oa1 = { "a", 1, null };
        Object[] oa2 = { "a", 1, null };
        Object[] oa3 = { "a", 2, null };
        assertEquals(CommonUtil.hashCode(oa1), CommonUtil.hashCode(oa2));
        assertNotEquals(CommonUtil.hashCode(oa1), CommonUtil.hashCode(oa3));

        int expected = 1;
        expected = 31 * expected + "a".hashCode();
        expected = 31 * expected + Integer.valueOf(1).hashCode();
        expected = 31 * expected + 0;
        assertEquals(expected, CommonUtil.hashCode(oa1));
    }

    @Test
    public void testHashCode_ObjectArrayRange() {
        Object[] arr = { "A", "B", null, "D" };
        int expected = 1;
        expected = 31 * expected + "B".hashCode();
        expected = 31 * expected + 0;
        assertEquals(expected, CommonUtil.hashCode(arr, 1, 3));
        assertEquals(1, CommonUtil.hashCode(arr, 1, 1));
    }

    @Test
    public void testDeepHashCode_Object() {
        assertEquals(0, CommonUtil.deepHashCode(null));
        assertEquals("test".hashCode(), CommonUtil.deepHashCode("test"));

        Object[] arr1 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr2 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr3 = { 1, new String[] { "a", "c" }, new int[] { 10, 20 } };

        assertEquals(CommonUtil.deepHashCode(arr1), CommonUtil.deepHashCode(arr2));
        assertNotEquals(CommonUtil.deepHashCode(arr1), CommonUtil.deepHashCode(arr3));

        assertEquals(CommonUtil.hashCode(new int[] { 1, 2 }), CommonUtil.deepHashCode(new int[] { 1, 2 }));
    }

    @Test
    public void testDeepHashCode_ObjectArray() {
        assertEquals(0, CommonUtil.deepHashCode((Object[]) null));
        Object[] arr = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };

        int expected = 1;
        expected = 31 * expected + Integer.valueOf(1).hashCode();
        expected = 31 * expected + CommonUtil.deepHashCode(new String[] { "a", "b" });
        expected = 31 * expected + CommonUtil.deepHashCode(new int[] { 10, 20 });

        assertEquals(expected, CommonUtil.deepHashCode(arr));
    }

    @Test
    public void testDeepHashCode_ObjectArrayRange() {
        Object[] arr = { "X", new int[] { 100, 200 }, new String[] { "deep" } };
        int expected = 1;
        expected = 31 * expected + CommonUtil.deepHashCode(new int[] { 100, 200 });
        expected = 31 * expected + CommonUtil.deepHashCode(new String[] { "deep" });
        assertEquals(expected, CommonUtil.deepHashCode(arr, 1, 3));
        assertEquals(1, CommonUtil.deepHashCode(arr, 1, 1));

    }

    @Test
    public void testToString_boolean() {
        assertEquals(String.valueOf(true), CommonUtil.toString(true));
    }

    @Test
    public void testToString_char() {
        assertEquals(String.valueOf('z'), CommonUtil.toString('z'));
    }

    @Test
    public void testToString_byte() {
        assertEquals(String.valueOf((byte) 12), CommonUtil.toString((byte) 12));
    }

    @Test
    public void testToString_short() {
        assertEquals(String.valueOf((short) 123), CommonUtil.toString((short) 123));
    }

    @Test
    public void testToString_int() {
        assertEquals(String.valueOf(12345), CommonUtil.toString(12345));
    }

    @Test
    public void testToString_long() {
        assertEquals(String.valueOf(1234567L), CommonUtil.toString(1234567L));
    }

    @Test
    public void testToString_float() {
        assertEquals(String.valueOf(1.2f), CommonUtil.toString(1.2f));
    }

    @Test
    public void testToString_double() {
        assertEquals(String.valueOf(1.23), CommonUtil.toString(1.23));
    }

    @Test
    public void testToString_Object() {
        assertEquals("null", CommonUtil.toString((Object) null));
        assertEquals("test", CommonUtil.toString("test"));
        assertEquals("[1, 2, 3]", CommonUtil.toString(Arrays.asList(1, 2, 3)));
        Iterator<Integer> iterator = Arrays.asList(4, 5).iterator();
        assertEquals("[4, 5]", CommonUtil.toString(iterator));

        assertEquals(String.valueOf(true), CommonUtil.toString(Boolean.TRUE));
        assertEquals(String.valueOf('c'), CommonUtil.toString(Character.valueOf('c')));
    }

    @Test
    public void testToString_Object_defaultIfNull() {
        assertEquals("default", CommonUtil.toString(null, "default"));
        assertEquals("test", CommonUtil.toString("test", "default"));
    }

    @Test
    public void testToString_booleanArray() {
        assertEquals("null", CommonUtil.toString((boolean[]) null));
        assertEquals("[]", CommonUtil.toString(new boolean[0]));
        assertEquals("[true, false, true]", CommonUtil.toString(new boolean[] { true, false, true }));
    }

    @Test
    public void testToString_booleanArrayRange() {
        boolean[] arr = { true, false, true, true, false };
        assertEquals("[false, true, true]", CommonUtil.toString(arr, 1, 4));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toString(arr, 0, 6));
    }

    @Test
    public void testToString_charArray() {
        assertEquals("null", CommonUtil.toString((char[]) null));
        assertEquals("[]", CommonUtil.toString(new char[0]));
        assertEquals("[a, b, c]", CommonUtil.toString(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testToString_charArrayRange() {
        char[] arr = { 'h', 'e', 'l', 'l', 'o' };
        assertEquals("[e, l, l]", CommonUtil.toString(arr, 1, 4));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toString(arr, 0, 6));
    }

    @Test
    public void testToString_intArray() {
        assertEquals("null", CommonUtil.toString((int[]) null));
        assertEquals("[]", CommonUtil.toString(new int[0]));
        assertEquals("[1, 2, 3]", CommonUtil.toString(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testToString_intArrayRange() {
        int[] arr = { 10, 20, 30, 40 };
        assertEquals("[20, 30]", CommonUtil.toString(arr, 1, 3));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));
    }

    @Test
    public void testToString_ObjectArray() {
        assertEquals("null", CommonUtil.toString((Object[]) null));
        assertEquals("[]", CommonUtil.toString(new Object[0]));
        assertEquals("[hello, 1, null]", CommonUtil.toString(new Object[] { "hello", 1, null }));
    }

    @Test
    public void testToString_ObjectArrayRange() {
        Object[] arr = { "one", 2, "three", null, 5.0 };
        assertEquals("[2, three, null]", CommonUtil.toString(arr, 1, 4));
        assertEquals("[]", CommonUtil.toString(arr, 1, 1));
    }

    @Test
    public void testDeepToString_Object() {
        assertEquals("null", CommonUtil.deepToString(null));
        assertEquals("simple string", CommonUtil.deepToString("simple string"));

        assertEquals("[true, false]", CommonUtil.deepToString(new boolean[] { true, false }));
        assertEquals("[[a, b], [c, d]]", CommonUtil.deepToString(new String[][] { { "a", "b" }, { "c", "d" } }));

        Object[] nested = { 1, new String[] { "x", "y" }, new int[] { 100, 200 } };
        assertEquals("[1, [x, y], [100, 200]]", CommonUtil.deepToString(nested));
    }

    @Test
    public void testDeepToString_ObjectArray() {
        assertEquals("null", CommonUtil.deepToString((Object[]) null));
        assertEquals("[]", CommonUtil.deepToString(new Object[0]));

        Object[] arr = { "A", new int[] { 1, 2 }, new String[] { "B", "C" } };
        assertEquals("[A, [1, 2], [B, C]]", CommonUtil.deepToString(arr));
    }

    @Test
    public void testDeepToString_ObjectArrayRange() {
        Object[] arr = { "start", new Object[] { "nested1", new int[] { 10, 20 } }, "middle", new String[] { "s1", "s2" }, "end" };
        String expected = "[[nested1, [10, 20]], middle, [s1, s2]]";
        assertEquals(expected, CommonUtil.deepToString(arr, 1, 4));
        assertEquals("[]", CommonUtil.deepToString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.deepToString(arr, 0, 6));
    }

    @Test
    public void testDeepToString_ObjectArray_Cyclic() {
        Object[] cyclicArray = new Object[2];
        cyclicArray[0] = "Element 1";
        cyclicArray[1] = cyclicArray;
        assertEquals("[Element 1, [...]]", CommonUtil.deepToString(cyclicArray));

        Object[] arr = new Object[1];
        arr[0] = arr;
        assertEquals("[[...]]", CommonUtil.deepToString(arr));

        Object[] a = new Object[2];
        Object[] b = new Object[] { "b" };
        a[0] = b;
        a[1] = b;
        assertEquals("[[b], [b]]", CommonUtil.deepToString(a));

        Object[] parent = new Object[1];
        Object[] child = new Object[1];
        parent[0] = child;
        child[0] = parent;
        assertEquals("[[[...]]]", CommonUtil.deepToString(parent));
    }

    @Test
    public void testDeepToString_ObjectArray_defaultIfNull() {
        assertEquals("fallback", CommonUtil.deepToString(null, "fallback"));
        assertEquals("[]", CommonUtil.deepToString(new Object[0], "fallback"));
    }
}
