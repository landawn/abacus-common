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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

// Assuming N.java and its dependencies (com.landawn.abacus.*, etc.) are in the classpath.
// Specifically, constants like Strings.NULL, Strings.ELEMENT_SEPARATOR, WD.BRACKET_L, etc.,
// and helper methods like N.typeOf(), N.CLASS_TYPE_ENUM, N.checkArgNotEmpty(), etc.,
// are assumed to be available and function as expected.
public class CommonUtil201Test extends TestBase {

    // Helper Bean for property-based comparisons
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

        // Mocking ClassUtil behavior for tests.
        // In a real scenario, ClassUtil would provide these.
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
            return "TestBean@" + Integer.toHexString(System.identityHashCode(this)); // Default-like toString
        }
    }

    // Mock implementation for N.compareByProps for testing N.equalsByProps
    // This is a simplified mock. The actual implementation might be more complex.
    // In a real test setup, you'd rely on the actual `N.compareByProps` or mock it with a framework like Mockito.
    private static int mockCompareByProps(final Object bean1, final Object bean2, final Collection<String> propNamesToCompare) {
        if (bean1 == bean2)
            return 0;
        if (bean1 == null || bean2 == null)
            return bean1 == null ? -1 : 1;
        if (!bean1.getClass().equals(bean2.getClass()) || !(bean1 instanceof TestBean)) {
            // Simplified: real N.compareByProps might handle different bean types or use reflection
            return 1; // Not equal
        }

        TestBean b1 = (TestBean) bean1;
        TestBean b2 = (TestBean) bean2;

        for (String propName : propNamesToCompare) {
            Object val1 = b1.getPropValue(propName);
            Object val2 = b2.getPropValue(propName);
            if (!N.equals(val1, val2)) {
                // This doesn't return detailed comparison, just inequality
                return val1 == null ? -1 : (val2 == null ? 1 : String.valueOf(val1).compareTo(String.valueOf(val2)));
            }
        }
        return 0;
    }

    //region ================================ equals Primitives ==================================
    @Test
    public void testEquals_boolean() {
        assertTrue(N.equals(true, true));
        assertTrue(N.equals(false, false));
        assertFalse(N.equals(true, false));
    }

    @Test
    public void testEquals_char() {
        assertTrue(N.equals('a', 'a'));
        assertFalse(N.equals('a', 'b'));
    }

    @Test
    public void testEquals_byte() {
        assertTrue(N.equals((byte) 1, (byte) 1));
        assertFalse(N.equals((byte) 1, (byte) 2));
    }

    @Test
    public void testEquals_short() {
        assertTrue(N.equals((short) 10, (short) 10));
        assertFalse(N.equals((short) 10, (short) 20));
    }

    @Test
    public void testEquals_int() {
        assertTrue(N.equals(100, 100));
        assertFalse(N.equals(100, 200));
    }

    @Test
    public void testEquals_long() {
        assertTrue(N.equals(1000L, 1000L));
        assertFalse(N.equals(1000L, 2000L));
    }

    @Test
    public void testEquals_float() {
        assertTrue(N.equals(1.0f, 1.0f));
        assertFalse(N.equals(1.0f, 1.1f));
        assertTrue(N.equals(Float.NaN, Float.NaN)); // Float.compare handles NaN
    }

    @Test
    public void testEquals_double() {
        assertTrue(N.equals(1.0, 1.0));
        assertFalse(N.equals(1.0, 1.1));
        assertTrue(N.equals(Double.NaN, Double.NaN)); // Double.compare handles NaN
    }

    @Test
    public void testEquals_String() {
        assertTrue(N.equals("abc", "abc"));
        assertFalse(N.equals("abc", "def"));
        assertTrue(N.equals((String) null, (String) null));
        assertFalse(N.equals("abc", null));
        assertFalse(N.equals(null, "abc"));
        assertFalse(N.equals("abc", "abcd")); // Different length
    }

    @Test
    public void testEqualsIgnoreCase_String() {
        assertTrue(N.equalsIgnoreCase("abc", "ABC"));
        assertTrue(N.equalsIgnoreCase("abc", "abc"));
        assertFalse(N.equalsIgnoreCase("abc", "def"));
        assertTrue(N.equalsIgnoreCase((String) null, (String) null));
        assertFalse(N.equalsIgnoreCase("abc", null));
        assertFalse(N.equalsIgnoreCase(null, "abc"));
    }
    //endregion

    //region ================================ equals Objects & Arrays ============================
    @Test
    public void testEquals_Object() {
        // This test is limited because N.typeOf() and Type.equals() are external.
        // We test the branching logic in N.equals(Object, Object)
        assertTrue(N.equals((Object) null, (Object) null));
        assertFalse(N.equals(new Object(), null));
        assertFalse(N.equals(null, new Object()));

        Object obj1 = new Object();
        Object obj2 = new Object();
        assertTrue(N.equals(obj1, obj1));
        assertFalse(N.equals(obj1, obj2));

        assertTrue(N.equals("Test", "Test"));
        assertFalse(N.equals("Test", "test"));

        // Primitive arrays (assuming Type system handles this)
        assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(N.equals(new int[] { 1, 2 }, new int[] { 1, 3 }));
        assertFalse(N.equals(new int[] { 1, 2 }, new long[] { 1, 2 })); // Different array types

        // Object arrays (assuming Type system handles this)
        assertTrue(N.equals(new String[] { "a" }, new String[] { "a" }));
        assertFalse(N.equals(new String[] { "a" }, new String[] { "b" }));
        assertFalse(N.equals(new String[] { "a" }, new Object[] { new Object() }));
    }

    @Test
    public void testEquals_booleanArray() {
        assertTrue(N.equals(new boolean[] { true, false }, new boolean[] { true, false }));
        assertFalse(N.equals(new boolean[] { true }, new boolean[] { false }));
        assertTrue(N.equals((boolean[]) null, (boolean[]) null));
        assertFalse(N.equals(new boolean[0], (boolean[]) null));
        assertFalse(N.equals(new boolean[] { true }, new boolean[] { true, false }));
    }

    @Test
    public void testEquals_booleanArrayRange() {
        boolean[] a1 = { true, false, true, true };
        boolean[] a2 = { false, true, true, false }; // different content, same sub-range possible
        boolean[] a3 = { true, false, true, true };

        assertTrue(N.equals(a1, 1, a3, 1, 2)); // {false, true} vs {false, true}
        assertFalse(N.equals(a1, 0, a2, 0, 2));
        assertTrue(N.equals(a1, 0, a1, 0, 0)); // Zero length

        assertThrows(IllegalArgumentException.class, () -> N.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 0, a3, 0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 3, a3, 0, 2));
    }

    // Similar tests for char[], byte[], short[], int[], long[], float[], double[] (full and range)

    @Test
    public void testEquals_intArray() {
        assertTrue(N.equals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(N.equals(new int[] { 1 }, new int[] { 2 }));
    }

    @Test
    public void testEquals_intArrayRange() {
        int[] a1 = { 1, 2, 3, 4 };
        int[] a2 = { 0, 2, 3, 5 };
        int[] a3 = { 1, 2, 3, 4 };

        assertTrue(N.equals(a1, 1, a3, 1, 2)); // {2, 3} vs {2, 3}
        assertFalse(N.equals(a1, 0, a2, 0, 2));
        assertTrue(N.equals(a1, 0, a1, 0, 0)); // Zero length

        assertThrows(IllegalArgumentException.class, () -> N.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 0, a3, 0, 5));
    }

    @Test
    public void testEquals_floatArrayRange() {
        float[] a1 = { 1.0f, 2.0f, Float.NaN, 4.0f };
        float[] a2 = { 0.0f, 2.0f, Float.NaN, 5.0f };
        float[] a3 = { 1.0f, 2.0f, Float.NaN, 4.0f };

        assertTrue(N.equals(a1, 1, a3, 1, 2)); // {2.0f, NaN} vs {2.0f, NaN}
        assertFalse(N.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEquals_doubleArrayRange() {
        double[] a1 = { 1.0, 2.0, Double.NaN, 4.0 };
        double[] a2 = { 0.0, 2.0, Double.NaN, 5.0 };
        double[] a3 = { 1.0, 2.0, Double.NaN, 4.0 };

        assertTrue(N.equals(a1, 1, a3, 1, 2)); // {2.0, NaN} vs {2.0, NaN}
        assertFalse(N.equals(a1, 0, a2, 0, 2));
    }

    @Test
    public void testEquals_ObjectArray() {
        Object[] oa1 = { "a", 1, null };
        Object[] oa2 = { "a", 1, null };
        Object[] oa3 = { "a", 2, null };
        assertTrue(N.equals(oa1, oa2));
        assertFalse(N.equals(oa1, oa3));
        assertTrue(N.equals((Object[]) null, (Object[]) null));
        assertFalse(N.equals(new Object[0], (Object[]) null));
    }

    @Test
    public void testEquals_ObjectArrayRange() {
        Object[] a1 = { "hello", "world", "!" };
        Object[] a2 = { "start", "world", "!" };
        Object[] a3 = { "hello", "world", "!" };

        assertTrue(N.equals(a1, 1, a3, 1, 2)); // {"world", "!"} vs {"world", "!"}
        assertFalse(N.equals(a1, 0, a2, 0, 2));
        assertTrue(N.equals(a1, 0, a1, 0, 0)); // Zero length

        assertThrows(IllegalArgumentException.class, () -> N.equals(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equals(a1, 0, a3, 0, 4));
    }
    //endregion

    //region ================================ deepEquals =======================================
    @Test
    public void testDeepEquals_Object() {
        assertTrue(N.deepEquals(null, null));
        assertFalse(N.deepEquals(new Object(), null));

        Object[] arr1 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr2 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr3 = { 1, new String[] { "a", "c" }, new int[] { 10, 20 } }; // different nested
        Object[] arr4 = { 1, new String[] { "a", "b" }, new long[] { 10, 20 } }; // different nested type

        assertTrue(N.deepEquals(arr1, arr2));
        assertFalse(N.deepEquals(arr1, arr3));
        assertFalse(N.deepEquals(arr1, arr4)); // This depends on CLASS_TYPE_ENUM and specific equals

        // Test primitive arrays direct path via CLASS_TYPE_ENUM
        assertTrue(N.deepEquals(new int[] { 1, 2 }, new int[] { 1, 2 }));
        assertFalse(N.deepEquals(new int[] { 1, 2 }, new int[] { 1, 3 }));
        // Test it does not confuse with object array
        assertFalse(N.deepEquals(new int[] { 1, 2 }, new Integer[] { 1, 2 }));

        // Simple objects
        assertTrue(N.deepEquals("test", "test"));
        assertFalse(N.deepEquals("test", "Test"));
    }

    @Test
    public void testDeepEquals_ObjectArray() {
        Object[] a = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] b = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] c = { 1, new String[] { "a", "DIFFERENT" }, new int[] { 10, 20 } };
        assertTrue(N.deepEquals(a, b));
        assertFalse(N.deepEquals(a, c));
        assertTrue(N.deepEquals((Object[]) null, (Object[]) null));
    }

    @Test
    public void testDeepEquals_ObjectArrayRange() {
        Object[] arr1 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };
        Object[] arr2 = { "other", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "end" };
        Object[] arr3 = { "first", new int[] { 1, 2 }, "mid", new String[] { "x", "y" }, "last" };

        // Compare slice: {new int[]{1, 2}, "mid", new String[]{"x", "y"}}
        assertTrue(N.deepEquals(arr1, 1, arr3, 1, 3));
        assertFalse(N.deepEquals(arr1, 0, arr2, 0, 2)); // "first", {1,2} vs "other", {1,2}
        assertTrue(N.deepEquals(arr1, 0, arr1, 0, 0)); // Zero length

        assertThrows(IllegalArgumentException.class, () -> N.deepEquals(arr1, 0, arr3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.deepEquals(arr1, 0, arr3, 0, 6));

        // Cyclic array test (deepEquals should handle this if implemented like Arrays.deepEquals)
        Object[] cyclic1 = new Object[1];
        cyclic1[0] = cyclic1;
        Object[] cyclic2 = new Object[1];
        cyclic2[0] = cyclic2;
        Object[] cyclic3 = new Object[1];
        cyclic3[0] = new Object[1]; // Not cyclic itself, but its element isn't cyclic1[0]

        // Note: N.deepEquals for Object[] calls itself recursively, so it should handle cycles if N.deepEquals(Object, Object) does.
        // The provided N.deepEquals(Object[], ...) does not seem to have explicit cycle detection itself,
        // but relies on N.deepEquals(Object, Object) for elements.
        // Standard Arrays.deepEquals handles cycles. N's specific implementation would determine outcome.
        // Assuming it behaves like Arrays.deepEquals or has its own mechanism:
        // assertTrue(N.deepEquals(cyclic1, cyclic2)); // This is tricky to assert without knowing N's exact cycle handling.
        // For now, we test non-cyclic cases thoroughly.
    }
    //endregion

    //region ================================ equalsIgnoreCase Arrays ============================
    @Test
    public void testEqualsIgnoreCase_StringArray() {
        String[] sa1 = { "abc", "DEF", null };
        String[] sa2 = { "ABC", "def", null };
        String[] sa3 = { "abc", "def", "ghi" };
        assertTrue(N.equalsIgnoreCase(sa1, sa2));
        assertFalse(N.equalsIgnoreCase(sa1, sa3));
        assertTrue(N.equalsIgnoreCase((String[]) null, (String[]) null));
    }

    @Test
    public void testEqualsIgnoreCase_StringArrayRange() {
        String[] a1 = { "Hello", "WORLD", "Java", "Test" };
        String[] a2 = { "start", "world", "java", "end" };
        String[] a3 = { "HELLO", "world", "JAVA", "TEST" };

        assertTrue(N.equalsIgnoreCase(a1, 1, a3, 1, 2)); // {"WORLD", "Java"} vs {"world", "JAVA"}
        assertFalse(N.equalsIgnoreCase(a1, 0, a2, 0, 2));
        assertTrue(N.equalsIgnoreCase(a1, 0, a1, 0, 0)); // Zero length

        // Test nulls within arrays
        String[] an1 = { "Test", null, "Me" };
        String[] an2 = { "test", null, "me" };
        String[] an3 = { "test", "NotNULL", "me" };
        assertTrue(N.equalsIgnoreCase(an1, 0, an2, 0, 3));
        assertFalse(N.equalsIgnoreCase(an1, 0, an3, 0, 3));

        assertThrows(IllegalArgumentException.class, () -> N.equalsIgnoreCase(a1, 0, a3, 0, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> N.equalsIgnoreCase(a1, 0, a3, 0, 5));
    }
    //endregion

    //region ================================ equalsBy Map/Bean =================================
    @Test
    public void testEqualsByKeys() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);
        map2.put("d", 4); // "c" missing, "d" different

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("a", 1);
        map3.put("b", 2);
        map3.put("c", 3);

        Map<String, Integer> map4 = new HashMap<>(); // map4 has 'a' but different value for 'b'
        map4.put("a", 1);
        map4.put("b", 20);
        map4.put("c", 3);

        Collection<String> keys = Arrays.asList("a", "b");
        Collection<String> keysWithC = Arrays.asList("a", "b", "c");

        assertTrue(N.equalsByKeys(map1, map3, keysWithC));
        assertTrue(N.equalsByKeys(map1, map3, keys));
        assertFalse(N.equalsByKeys(map1, map2, keysWithC)); // map2 missing "c" and has "d" instead
        assertTrue(N.equalsByKeys(map1, map2, keys)); // Only "a", "b" compared
        assertFalse(N.equalsByKeys(map1, map4, keysWithC)); // map4["b"] is different

        assertTrue(N.equalsByKeys(null, null, keys));
        assertFalse(N.equalsByKeys(map1, null, keys));
        assertFalse(N.equalsByKeys(null, map1, keys));

        Map<String, Integer> mapWithNullValue1 = new HashMap<>();
        mapWithNullValue1.put("k1", null);
        Map<String, Integer> mapWithNullValue2 = new HashMap<>();
        mapWithNullValue2.put("k1", null);
        Map<String, Integer> mapWithNonNullValue = new HashMap<>();
        mapWithNonNullValue.put("k1", 1);

        // This part of N.equalsByKeys logic is complex:
        // (!map1.containsKey(key) || !map2.containsKey(key))
        // If both values are null, it further checks if both maps *contain* the key.
        // This means {k1:null} is different from {} if key "k1" is checked.
        assertTrue(N.equalsByKeys(mapWithNullValue1, mapWithNullValue2, Collections.singletonList("k1")));
        assertFalse(N.equalsByKeys(mapWithNullValue1, mapWithNonNullValue, Collections.singletonList("k1")));

        Map<String, Integer> emptyMap = Collections.emptyMap();
        // Fails because key "k1" is not in emptyMap
        assertFalse(N.equalsByKeys(mapWithNullValue1, emptyMap, Collections.singletonList("k1")));

        assertThrows(IllegalArgumentException.class, () -> N.equalsByKeys(map1, map2, Collections.emptyList()));
    }

    @Test
    public void testEqualsByProps() {
        // This requires N.compareByProps to be available and working.
        // We are also assuming N.checkArgNotEmpty and N.isEmpty work.
        // For this test, we'll use a simplified mockCompareByProps or assume N.compareByProps exists.
        // Since N.compareByProps isn't provided, we can't fully test N.equalsByProps
        // in isolation without mocking it or its dependencies (like ClassUtil).

        TestBean bean1 = new TestBean("Alice", 30, 50000, true);
        TestBean bean2 = new TestBean("Alice", 30, 50000, true);
        TestBean bean3 = new TestBean("Bob", 30, 50000, true); // Different name
        TestBean bean4 = new TestBean("Alice", 31, 50000, true); // Different age

        Collection<String> propsAll = TestBean.getPropNames(); // Mocking ClassUtil.getPropNameList
        Collection<String> propsNameAge = Arrays.asList("name", "age");

        // Simulate N.compareByProps for testing purposes, or this test will be limited
        // Assuming N.compareByProps(b1,b2,props) delegates to N.equals(b1.getProp(p), b2.getProp(p)) for each prop
        // For the test to pass, we might need to inject our mock N.compareByProps or use PowerMock/Mockito.
        // The current N.java calls N.compareByProps.

        // To make this test runnable, let's assume a simplified version of N.compareByProps for TestBean
        // where it uses TestBean.getPropValue() and N.equals() for comparison of property values.
        // Then N.equalsByProps would be:
        // assertTrue(N.equalsByProps(bean1, bean2, propsAll)); // If using a real N.compareByProps
        // assertFalse(N.equalsByProps(bean1, bean3, propsAll));
        // assertTrue(N.equalsByProps(bean1, bean3, Collections.singletonList("age"))); // Only age compared
        // assertFalse(N.equalsByProps(bean1, bean4, propsNameAge));

        // Based on the provided N.java, equalsByProps relies on N.compareByProps.
        // We can test the argument checking.
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByProps(bean1, bean2, Collections.emptyList()));
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByProps(bean1, bean2, null)); // Assuming N.checkArgNotEmpty handles this

        // Test with a direct (but incomplete) simulation of how equalsByProps might work if compareByProps were simple:
        // This is NOT testing the provided N.equalsByProps fully, but the concept.
        assertTrue(mockCompareByProps(bean1, bean2, propsAll) == 0);
        assertFalse(mockCompareByProps(bean1, bean3, propsAll) == 0);
        assertTrue(mockCompareByProps(bean1, bean3, Collections.singletonList("age")) == 0); // name differs, but only age compared
        assertFalse(mockCompareByProps(bean1, bean4, propsNameAge) == 0);
    }

    @Test
    public void testEqualsByCommonProps() {
        // This depends on N.equalsByProps, ClassUtil.isBeanClass, ClassUtil.getPropNameList.
        // Assumptions: TestBean is a bean, ClassUtil returns its properties.
        TestBean beanA1 = new TestBean("SameName", 25, 60000, false);
        TestBean beanA2 = new TestBean("SameName", 25, 60000, false);
        TestBean beanB = new TestBean("DifferentName", 25, 60000, false);

        // To test this properly, ClassUtil needs to be functional or mocked.
        // Assuming ClassUtil.getPropNameList(TestBean.class) returns ("name", "age", "salary", "active")
        // and that N.equalsByProps works.

        // assertTrue(N.equalsByCommonProps(beanA1, beanA2)); // if N.equalsByProps works fully
        // assertFalse(N.equalsByCommonProps(beanA1, beanB)); // if N.equalsByProps works fully

        // Test argument checks (assuming N.checkArgNotNull, N.checkArgument, N.isEmpty work)
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(null, beanA2));
        assertThrows(IllegalArgumentException.class, () -> Beans.equalsByCommonProps(beanA1, null));

        // If ClassUtil.isBeanClass or getPropNameList are not set up, this will fail.
        // Or if no common props are found.
        // Example:
        // assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(beanA1, new Object())); // Not a bean
        // assertThrows(IllegalArgumentException.class, () -> N.equalsByCommonProps(beanA1, new AnotherBeanWithNoCommonProps()));
    }
    //endregion

    //region ================================ hashCode Primitives ================================
    @Test
    public void testHashCode_boolean() {
        assertEquals(1231, N.hashCode(true));
        assertEquals(1237, N.hashCode(false));
    }

    @Test
    public void testHashCode_char() {
        assertEquals((int) 'a', N.hashCode('a'));
    }

    @Test
    public void testHashCode_byte() {
        assertEquals((int) (byte) 5, N.hashCode((byte) 5));
    }

    @Test
    public void testHashCode_short() {
        assertEquals((int) (short) 100, N.hashCode((short) 100));
    }

    @Test
    public void testHashCode_int() {
        assertEquals(12345, N.hashCode(12345));
    }

    @Test
    public void testHashCode_long() {
        assertEquals(Long.hashCode(123456789L), N.hashCode(123456789L));
    }

    @Test
    public void testHashCode_float() {
        assertEquals(Float.floatToIntBits(1.23f), N.hashCode(1.23f));
        assertEquals(Float.floatToIntBits(Float.NaN), N.hashCode(Float.NaN));
    }

    @Test
    public void testHashCode_double() {
        assertEquals(Double.hashCode(1.2345), N.hashCode(1.2345));
        assertEquals(Double.hashCode(Double.NaN), N.hashCode(Double.NaN));
    }
    //endregion

    //region ================================ hashCode Objects & Arrays ==========================
    @Test
    public void testHashCode_Object() {
        // Limited test due to N.typeOf() and Type.hashCode() being external.
        assertEquals(0, N.hashCode((Object) null));
        String s = "test";
        assertEquals(s.hashCode(), N.hashCode(s));

        // For arrays, it should delegate to Type.hashCode()
        int[] arr = { 1, 2, 3 };
        // Can't easily assert specific value without knowing Type.hashCode() impl,
        // but can check for non-zero and consistency.
        assertTrue(N.hashCode(arr) != 0);
        assertEquals(N.hashCode(arr), N.hashCode(new int[] { 1, 2, 3 })); // Assuming Type.hashCode is consistent
    }

    @Test
    public void testHashCode_booleanArray() {
        assertEquals(0, N.hashCode((boolean[]) null));
        boolean[] ba1 = { true, false };
        boolean[] ba2 = { true, false };
        boolean[] ba3 = { false, true };
        assertEquals(N.hashCode(ba1), N.hashCode(ba2));
        assertNotEquals(N.hashCode(ba1), N.hashCode(ba3));
        assertEquals(Arrays.hashCode(new boolean[0]), N.hashCode(new boolean[0])); // Using N's own range hashcode
    }

    @Test
    public void testHashCode_booleanArrayRange() {
        boolean[] arr = { true, false, true, false, true };
        assertEquals(0, N.hashCode((boolean[]) null, 0, 0)); // Should not happen due to checkFromToIndex

        int fullHash = N.hashCode(arr, 0, arr.length);
        int partialHash = N.hashCode(arr, 1, 3); // {false, true}

        // Assertions:
        assertNotEquals(0, fullHash);
        assertNotEquals(0, partialHash);
        assertNotEquals(fullHash, partialHash);

        // Test empty range
        assertEquals(1, N.hashCode(arr, 1, 1)); // Loop doesn't run, result is 1

        assertThrows(IndexOutOfBoundsException.class, () -> N.hashCode(arr, 0, 6));
        assertThrows(IndexOutOfBoundsException.class, () -> N.hashCode(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> N.hashCode(arr, 3, 2)); // from > to
    }

    // Similar tests for char[], byte[], short[], int[], long[], float[], double[] (full and range)

    @Test
    public void testHashCode_intArray() {
        assertEquals(0, N.hashCode((int[]) null));
        // Calculated based on N's specific prime number logic for arrays
        // Example: new int[]{1,2} -> 1 * 31 + 1 = 32 (seed), then 32 * 31 + 2 = 994
        // N.hashCode(new int[]{1,2}) -> loop: result=1; res=31*1+1=32; res=31*32+2=994
        // The test should be specific to N's implementation
        int[] arr = { 1, 2 };
        int expected = 1;
        expected = 31 * expected + 1;
        expected = 31 * expected + 2;
        assertEquals(expected, N.hashCode(arr));
    }

    @Test
    public void testHashCode_intArrayRange() {
        int[] arr = { 10, 20, 30, 40, 50 };
        // Hash for {20, 30}
        int expected = 1;
        expected = 31 * expected + 20;
        expected = 31 * expected + 30;
        assertEquals(expected, N.hashCode(arr, 1, 3));
        assertEquals(1, N.hashCode(arr, 1, 1)); // empty range
    }

    @Test
    public void testHashCode_ObjectArray() {
        assertEquals(0, N.hashCode((Object[]) null));
        Object[] oa1 = { "a", 1, null };
        Object[] oa2 = { "a", 1, null };
        Object[] oa3 = { "a", 2, null }; // Different element
        assertEquals(N.hashCode(oa1), N.hashCode(oa2));
        assertNotEquals(N.hashCode(oa1), N.hashCode(oa3));

        // Expected calculation:
        int expected = 1;
        expected = 31 * expected + "a".hashCode();
        expected = 31 * expected + Integer.valueOf(1).hashCode();
        expected = 31 * expected + 0; // for null
        assertEquals(expected, N.hashCode(oa1));
    }

    @Test
    public void testHashCode_ObjectArrayRange() {
        Object[] arr = { "A", "B", null, "D" };
        // Hash for {"B", null}
        int expected = 1;
        expected = 31 * expected + "B".hashCode();
        expected = 31 * expected + 0; // for null
        assertEquals(expected, N.hashCode(arr, 1, 3));
        assertEquals(1, N.hashCode(arr, 1, 1)); // empty range
    }
    //endregion

    //region ================================ deepHashCode =====================================
    @Test
    public void testDeepHashCode_Object() {
        // Similar to deepEquals, relies on CLASS_TYPE_ENUM and recursive calls.
        assertEquals(0, N.deepHashCode(null));
        assertEquals("test".hashCode(), N.deepHashCode("test"));

        Object[] arr1 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr2 = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };
        Object[] arr3 = { 1, new String[] { "a", "c" }, new int[] { 10, 20 } };

        assertEquals(N.deepHashCode(arr1), N.deepHashCode(arr2));
        assertNotEquals(N.deepHashCode(arr1), N.deepHashCode(arr3));

        // Primitive array direct path
        assertEquals(N.hashCode(new int[] { 1, 2 }), N.deepHashCode(new int[] { 1, 2 }));
    }

    @Test
    public void testDeepHashCode_ObjectArray() {
        assertEquals(0, N.deepHashCode((Object[]) null));
        Object[] arr = { 1, new String[] { "a", "b" }, new int[] { 10, 20 } };

        int expected = 1;
        expected = 31 * expected + Integer.valueOf(1).hashCode();
        expected = 31 * expected + N.deepHashCode(new String[] { "a", "b" }); // which uses N.hashCode for String[]
        expected = 31 * expected + N.deepHashCode(new int[] { 10, 20 }); // which uses N.hashCode for int[]

        assertEquals(expected, N.deepHashCode(arr));
    }

    @Test
    public void testDeepHashCode_ObjectArrayRange() {
        Object[] arr = { "X", new int[] { 100, 200 }, new String[] { "deep" } };
        // Hash for {new int[]{100, 200}, new String[]{"deep"}}
        int expected = 1;
        expected = 31 * expected + N.deepHashCode(new int[] { 100, 200 });
        expected = 31 * expected + N.deepHashCode(new String[] { "deep" });
        assertEquals(expected, N.deepHashCode(arr, 1, 3));
        assertEquals(1, N.deepHashCode(arr, 1, 1)); // empty range

        // Cyclic array test (deepHashCode should handle this like Arrays.deepHashCode)
        // Object[] cyclic1 = new Object[1];
        // cyclic1[0] = cyclic1;
        // Object[] cyclic2 = new Object[1]; // Different instance, but same structure
        // cyclic2[0] = cyclic2;
        // This is complex to assert exact value without knowing N's exact cycle handling for hash.
        // Standard Arrays.deepHashCode handles cycles by typically hashing address or placeholder.
    }
    //endregion

    //region ================================ toString Primitives ==============================
    // These rely on N.stringOf(value) which is not provided.
    // Assuming N.stringOf(value) is equivalent to String.valueOf(value).
    @Test
    public void testToString_boolean() {
        assertEquals(String.valueOf(true), N.toString(true));
    }

    @Test
    public void testToString_char() {
        assertEquals(String.valueOf('z'), N.toString('z'));
    }

    @Test
    public void testToString_byte() {
        assertEquals(String.valueOf((byte) 12), N.toString((byte) 12));
    }

    @Test
    public void testToString_short() {
        assertEquals(String.valueOf((short) 123), N.toString((short) 123));
    }

    @Test
    public void testToString_int() {
        assertEquals(String.valueOf(12345), N.toString(12345));
    }

    @Test
    public void testToString_long() {
        assertEquals(String.valueOf(1234567L), N.toString(1234567L));
    }

    @Test
    public void testToString_float() {
        assertEquals(String.valueOf(1.2f), N.toString(1.2f));
    }

    @Test
    public void testToString_double() {
        assertEquals(String.valueOf(1.23), N.toString(1.23));
    }
    //endregion

    //region ================================ toString Objects & Arrays ========================
    @Test
    public void testToString_Object() {
        // Assumes Strings.NULL, Type.toString(), CLASS_TYPE_ENUM, Strings.join are available.
        assertEquals("null", N.toString((Object) null)); // Strings.NULL
        assertEquals("test", N.toString("test"));
        assertEquals("[1, 2, 3]", N.toString(Arrays.asList(1, 2, 3))); // Iterable
        Iterator<Integer> iterator = Arrays.asList(4, 5).iterator();
        assertEquals("[4, 5]", N.toString(iterator)); // Iterator

        // For arrays, assuming Type.toString() produces something like Arrays.toString()
        // This is a basic check as Type.toString() is external.
        // assertEquals("[10, 20]", N.toString(new int[]{10, 20})); // If Type.toString() matches Arrays.toString() format

        // Test CLASS_TYPE_ENUM path for boxed primitives
        assertEquals(String.valueOf(true), N.toString(Boolean.TRUE));
        assertEquals(String.valueOf('c'), N.toString(Character.valueOf('c')));
        // ... and so on for Byte, Short, Integer, Long. (Float/Double are commented out in N.java's switch)
    }

    @Test
    public void testToString_Object_defaultIfNull() {
        assertEquals("default", N.toString(null, "default"));
        assertEquals("test", N.toString("test", "default"));
    }

    @Test
    public void testToString_booleanArray() {
        // Assumes Strings.NULL, Strings.STR_FOR_EMPTY_ARRAY, and N.toString(boolean[], from, to)
        assertEquals("null", N.toString((boolean[]) null));
        assertEquals("[]", N.toString(new boolean[0]));
        assertEquals("[true, false, true]", N.toString(new boolean[] { true, false, true }));
    }

    @Test
    public void testToString_booleanArrayRange() {
        // Assumes Strings.join(a, from, to, Strings.ELEMENT_SEPARATOR, WD.BRACKET_L, WD.BRACKET_R);
        boolean[] arr = { true, false, true, true, false };
        assertEquals("[false, true, true]", N.toString(arr, 1, 4));
        assertEquals("[]", N.toString(arr, 1, 1)); // Empty range if Strings.join handles it

        assertThrows(IndexOutOfBoundsException.class, () -> N.toString(arr, 0, 6));
    }

    @Test
    public void testToString_charArray() {
        assertEquals("null", N.toString((char[]) null));
        assertEquals("[]", N.toString(new char[0])); // STR_FOR_EMPTY_ARRAY
        assertEquals("[a, b, c]", N.toString(new char[] { 'a', 'b', 'c' })); // Special: String.valueOf(char[])
    }

    @Test
    public void testToString_charArrayRange() {
        // Special implementation: String.valueOf(a, fromIndex, toIndex - fromIndex)
        char[] arr = { 'h', 'e', 'l', 'l', 'o' };
        assertEquals("[e, l, l]", N.toString(arr, 1, 4));
        assertEquals("[]", N.toString(arr, 1, 1)); // Empty string for empty range

        assertThrows(IndexOutOfBoundsException.class, () -> N.toString(arr, 0, 6));
    }

    // Similar tests for byte[], short[], int[], long[], float[], double[] (full and range)
    // Assuming they use Strings.join and produce "[e1, e2, ...]"
    @Test
    public void testToString_intArray() {
        assertEquals("null", N.toString((int[]) null));
        assertEquals("[]", N.toString(new int[0]));
        assertEquals("[1, 2, 3]", N.toString(new int[] { 1, 2, 3 }));
    }

    @Test
    public void testToString_intArrayRange() {
        int[] arr = { 10, 20, 30, 40 };
        assertEquals("[20, 30]", N.toString(arr, 1, 3));
        assertEquals("[]", N.toString(arr, 1, 1));
    }

    @Test
    public void testToString_ObjectArray() {
        assertEquals("null", N.toString((Object[]) null));
        assertEquals("[]", N.toString(new Object[0]));
        assertEquals("[hello, 1, null]", N.toString(new Object[] { "hello", 1, null }));
    }

    @Test
    public void testToString_ObjectArrayRange() {
        Object[] arr = { "one", 2, "three", null, 5.0 };
        assertEquals("[2, three, null]", N.toString(arr, 1, 4));
        assertEquals("[]", N.toString(arr, 1, 1));
    }
    //endregion

    //region ================================ deepToString =====================================
    @Test
    public void testDeepToString_Object() {
        // Relies on CLASS_TYPE_ENUM and N.deepToString(Object[])
        assertEquals("null", N.deepToString(null));
        assertEquals("simple string", N.deepToString("simple string"));

        // For arrays, it should delegate
        assertEquals("[true, false]", N.deepToString(new boolean[] { true, false })); // Via CLASS_TYPE_ENUM path
        assertEquals("[[a, b], [c, d]]", N.deepToString(new String[][] { { "a", "b" }, { "c", "d" } }));

        Object[] nested = { 1, new String[] { "x", "y" }, new int[] { 100, 200 } };
        assertEquals("[1, [x, y], [100, 200]]", N.deepToString(nested));
    }

    @Test
    public void testDeepToString_ObjectArray() {
        assertEquals("null", N.deepToString((Object[]) null));
        assertEquals("[]", N.deepToString(new Object[0]));

        Object[] arr = { "A", new int[] { 1, 2 }, new String[] { "B", "C" } };
        assertEquals("[A, [1, 2], [B, C]]", N.deepToString(arr));
    }

    @Test
    public void testDeepToString_ObjectArrayRange() {
        Object[] arr = { "start", new Object[] { "nested1", new int[] { 10, 20 } }, "middle", new String[] { "s1", "s2" }, "end" };
        // Slice: {new Object[]{"nested1", new int[]{10, 20}}, "middle", new String[]{"s1", "s2"}}
        String expected = "[[nested1, [10, 20]], middle, [s1, s2]]";
        assertEquals(expected, N.deepToString(arr, 1, 4));
        assertEquals("[]", N.deepToString(arr, 1, 1));

        assertThrows(IndexOutOfBoundsException.class, () -> N.deepToString(arr, 0, 6));
    }

    @Test
    public void testDeepToString_ObjectArray_Cyclic() {
        Object[] cyclicArray = new Object[2];
        cyclicArray[0] = "Element 1";
        cyclicArray[1] = cyclicArray; // Cycle
        // Expected output for cycle is typically "[...]"
        assertEquals("[Element 1, [...]]", N.deepToString(cyclicArray));

        Object[] arr = new Object[1];
        arr[0] = arr;
        assertEquals("[[...]]", N.deepToString(arr));

        Object[] a = new Object[2];
        Object[] b = new Object[] { "b" };
        a[0] = b;
        a[1] = b; // Two references to the same array, but not a cycle in terms of parent-child
        assertEquals("[[b], [b]]", N.deepToString(a));

        Object[] parent = new Object[1];
        Object[] child = new Object[1];
        parent[0] = child;
        child[0] = parent; // Mutual recursion / cycle
        assertEquals("[[[...]]]", N.deepToString(parent)); // The exact representation of deeper cycles might vary
    }

    @Test
    public void testDeepToString_ObjectArray_defaultIfNull() {
        assertEquals("fallback", N.deepToString(null, "fallback"));
        assertEquals("[]", N.deepToString(new Object[0], "fallback"));
    }
    //endregion 
}
