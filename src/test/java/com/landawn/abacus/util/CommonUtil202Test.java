package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
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
public class CommonUtil202Test extends TestBase {

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
                return (val1 == null) ? -1 : ((val2 == null) ? 1 : String.valueOf(val1).compareTo(String.valueOf(val2)));
            }
        }
        return 0;
    }

    @Test
    public void testLen_CharSequence() {
        assertEquals(0, CommonUtil.len((CharSequence) null));
        assertEquals(0, CommonUtil.len(""));
        assertEquals(3, CommonUtil.len("abc"));
    }

    @Test
    public void testLen_booleanArray() {
        assertEquals(0, CommonUtil.len((boolean[]) null));
        assertEquals(0, CommonUtil.len(new boolean[0]));
        assertEquals(2, CommonUtil.len(new boolean[] { true, false }));
    }

    @Test
    public void testLen_charArray() {
        assertEquals(0, CommonUtil.len((char[]) null));
        assertEquals(0, CommonUtil.len(new char[0]));
        assertEquals(3, CommonUtil.len(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testLen_byteArray() {
        assertEquals(0, CommonUtil.len((byte[]) null));
        assertEquals(0, CommonUtil.len(new byte[0]));
        assertEquals(2, CommonUtil.len(new byte[] { 1, 2 }));
    }

    @Test
    public void testLen_shortArray() {
        assertEquals(0, CommonUtil.len((short[]) null));
        assertEquals(0, CommonUtil.len(new short[0]));
        assertEquals(2, CommonUtil.len(new short[] { 10, 20 }));
    }

    @Test
    public void testLen_intArray() {
        assertEquals(0, CommonUtil.len((int[]) null));
        assertEquals(0, CommonUtil.len(new int[0]));
        assertEquals(2, CommonUtil.len(new int[] { 100, 200 }));
    }

    @Test
    public void testLen_longArray() {
        assertEquals(0, CommonUtil.len((long[]) null));
        assertEquals(0, CommonUtil.len(new long[0]));
        assertEquals(2, CommonUtil.len(new long[] { 1000L, 2000L }));
    }

    @Test
    public void testLen_floatArray() {
        assertEquals(0, CommonUtil.len((float[]) null));
        assertEquals(0, CommonUtil.len(new float[0]));
        assertEquals(2, CommonUtil.len(new float[] { 1.0f, 2.0f }));
    }

    @Test
    public void testLen_doubleArray() {
        assertEquals(0, CommonUtil.len((double[]) null));
        assertEquals(0, CommonUtil.len(new double[0]));
        assertEquals(2, CommonUtil.len(new double[] { 1.0, 2.0 }));
    }

    @Test
    public void testLen_ObjectArray() {
        assertEquals(0, CommonUtil.len((Object[]) null));
        assertEquals(0, CommonUtil.len(new Object[0]));
        assertEquals(2, CommonUtil.len(new Object[] { "a", "b" }));
    }

    @Test
    public void testSize_Collection() {
        assertEquals(0, CommonUtil.size((Collection<?>) null));
        assertEquals(0, CommonUtil.size(Collections.emptyList()));
        assertEquals(2, CommonUtil.size(Arrays.asList(1, 2)));
    }

    @Test
    public void testSize_Map() {
        assertEquals(0, CommonUtil.size((Map<?, ?>) null));
        assertEquals(0, CommonUtil.size(Collections.emptyMap()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, CommonUtil.size(map));
    }

    @Test
    public void testSize_PrimitiveList() {
        assertEquals(0, CommonUtil.size((PrimitiveList) null));
    }

    @Test
    public void testIsEmpty_CharSequence() {
        assertTrue(CommonUtil.isEmpty((CharSequence) null));
        assertTrue(CommonUtil.isEmpty(""));
        assertFalse(CommonUtil.isEmpty("abc"));
        assertFalse(CommonUtil.isEmpty(" "));
    }

    @Test
    public void testIsEmpty_booleanArray() {
        assertTrue(CommonUtil.isEmpty((boolean[]) null));
        assertTrue(CommonUtil.isEmpty(new boolean[0]));
        assertFalse(CommonUtil.isEmpty(new boolean[] { true }));
    }

    @Test
    public void testIsEmpty_charArray() {
        assertTrue(CommonUtil.isEmpty((char[]) null));
        assertTrue(CommonUtil.isEmpty(new char[0]));
        assertFalse(CommonUtil.isEmpty(new char[] { 'a' }));
    }

    @Test
    public void testIsEmpty_byteArray() {
        assertTrue(CommonUtil.isEmpty((byte[]) null));
        assertTrue(CommonUtil.isEmpty(new byte[0]));
        assertFalse(CommonUtil.isEmpty(new byte[] { 1 }));
    }

    @Test
    public void testIsEmpty_shortArray() {
        assertTrue(CommonUtil.isEmpty((short[]) null));
        assertTrue(CommonUtil.isEmpty(new short[0]));
        assertFalse(CommonUtil.isEmpty(new short[] { 1 }));
    }

    @Test
    public void testIsEmpty_intArray() {
        assertTrue(CommonUtil.isEmpty((int[]) null));
        assertTrue(CommonUtil.isEmpty(new int[0]));
        assertFalse(CommonUtil.isEmpty(new int[] { 1 }));
    }

    @Test
    public void testIsEmpty_longArray() {
        assertTrue(CommonUtil.isEmpty((long[]) null));
        assertTrue(CommonUtil.isEmpty(new long[0]));
        assertFalse(CommonUtil.isEmpty(new long[] { 1L }));
    }

    @Test
    public void testIsEmpty_floatArray() {
        assertTrue(CommonUtil.isEmpty((float[]) null));
        assertTrue(CommonUtil.isEmpty(new float[0]));
        assertFalse(CommonUtil.isEmpty(new float[] { 1f }));
    }

    @Test
    public void testIsEmpty_doubleArray() {
        assertTrue(CommonUtil.isEmpty((double[]) null));
        assertTrue(CommonUtil.isEmpty(new double[0]));
        assertFalse(CommonUtil.isEmpty(new double[] { 1.0 }));
    }

    @Test
    public void testIsEmpty_ObjectArray() {
        assertTrue(CommonUtil.isEmpty((Object[]) null));
        assertTrue(CommonUtil.isEmpty(new Object[0]));
        assertFalse(CommonUtil.isEmpty(new Object[] { "a" }));
    }

    @Test
    public void testIsEmpty_Collection() {
        assertTrue(CommonUtil.isEmpty((Collection<?>) null));
        assertTrue(CommonUtil.isEmpty(Collections.emptyList()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1, 2)));
    }

    @Test
    public void testIsEmpty_Iterable() {
        assertTrue(CommonUtil.isEmpty((Iterable<?>) null));
        assertTrue(CommonUtil.isEmpty(Collections.emptyList()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1)));

        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertTrue(CommonUtil.isEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertFalse(CommonUtil.isEmpty(nonEmptyIterable));
    }

    @Test
    public void testIsEmpty_Iterator() {
        assertTrue(CommonUtil.isEmpty((Iterator<?>) null));
        assertTrue(CommonUtil.isEmpty(Collections.emptyIterator()));
        assertFalse(CommonUtil.isEmpty(Arrays.asList(1, 2).iterator()));
    }

    @Test
    public void testIsEmpty_Map() {
        assertTrue(CommonUtil.isEmpty((Map<?, ?>) null));
        assertTrue(CommonUtil.isEmpty(Collections.emptyMap()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(CommonUtil.isEmpty(map));
    }

    @Test
    public void testIsEmpty_PrimitiveList() {
        assertTrue(CommonUtil.isEmpty((PrimitiveList) null));
    }

    @Test
    public void testIsEmpty_Multiset() {
        assertTrue(CommonUtil.isEmpty((Multiset<?>) null));
    }

    @Test
    public void testIsEmpty_Multimap() {
        assertTrue(CommonUtil.isEmpty((Multimap<?, ?, ?>) null));
    }

    @Test
    public void testIsEmpty_Dataset() {
        assertTrue(CommonUtil.isEmpty((Dataset) null));
    }

    @Test
    public void testIsBlank() {
        assertTrue(CommonUtil.isBlank(null));
        assertTrue(CommonUtil.isBlank(""));
        assertTrue(CommonUtil.isBlank(" "));
        assertTrue(CommonUtil.isBlank("   "));
        assertTrue(CommonUtil.isBlank("\t\n\r"));
        assertFalse(CommonUtil.isBlank("abc"));
        assertFalse(CommonUtil.isBlank(" a "));
    }

    @Test
    public void testIsTrue() {
        assertTrue(CommonUtil.isTrue(Boolean.TRUE));
        assertFalse(CommonUtil.isTrue(Boolean.FALSE));
        assertFalse(CommonUtil.isTrue(null));
    }

    @Test
    public void testIsNotTrue() {
        assertTrue(CommonUtil.isNotTrue(null));
        assertTrue(CommonUtil.isNotTrue(Boolean.FALSE));
        assertFalse(CommonUtil.isNotTrue(Boolean.TRUE));
    }

    @Test
    public void testIsFalse() {
        assertTrue(CommonUtil.isFalse(Boolean.FALSE));
        assertFalse(CommonUtil.isFalse(Boolean.TRUE));
        assertFalse(CommonUtil.isFalse(null));
    }

    @Test
    public void testIsNotFalse() {
        assertTrue(CommonUtil.isNotFalse(null));
        assertTrue(CommonUtil.isNotFalse(Boolean.TRUE));
        assertFalse(CommonUtil.isNotFalse(Boolean.FALSE));
    }

    @Test
    public void testNotEmpty_CharSequence() {
        assertFalse(CommonUtil.notEmpty((String) null));
        assertFalse(CommonUtil.notEmpty(""));
        assertTrue(CommonUtil.notEmpty("abc"));
        assertTrue(CommonUtil.notEmpty(" "));
    }

    @Test
    public void testNotEmpty_ObjectArray() {
        assertFalse(CommonUtil.notEmpty((Object[]) null));
        assertFalse(CommonUtil.notEmpty(new Object[0]));
        assertTrue(CommonUtil.notEmpty(new Object[] { "a" }));
    }

    @Test
    public void testNotEmpty_Collection() {
        assertFalse(CommonUtil.notEmpty((Collection<?>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1)));
    }

    @Test
    public void testNotEmpty_Iterable() {
        assertFalse(CommonUtil.notEmpty((Iterable<?>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1)));

        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertFalse(CommonUtil.notEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertTrue(CommonUtil.notEmpty(nonEmptyIterable));
    }

    @Test
    public void testNotEmpty_Iterator() {
        assertFalse(CommonUtil.notEmpty((Iterator<?>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyIterator()));
        assertTrue(CommonUtil.notEmpty(Arrays.asList(1).iterator()));
    }

    @Test
    public void testNotEmpty_Map() {
        assertFalse(CommonUtil.notEmpty((Map<Object, Object>) null));
        assertFalse(CommonUtil.notEmpty(Collections.emptyMap()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(CommonUtil.notEmpty(map));
    }

    @Test
    public void testNotBlank() {
        assertFalse(CommonUtil.notBlank(null));
        assertFalse(CommonUtil.notBlank(""));
        assertFalse(CommonUtil.notBlank(" "));
        assertTrue(CommonUtil.notBlank("abc"));
        assertTrue(CommonUtil.notBlank(" a "));
    }

    @Test
    public void testAnyNull_TwoObjects() {
        assertTrue(CommonUtil.anyNull(null, "a"));
        assertTrue(CommonUtil.anyNull("a", null));
        assertTrue(CommonUtil.anyNull(null, null));
        assertFalse(CommonUtil.anyNull("a", "b"));
    }

    @Test
    public void testAnyNull_ThreeObjects() {
        assertTrue(CommonUtil.anyNull(null, "a", "b"));
        assertTrue(CommonUtil.anyNull("a", null, "b"));
        assertTrue(CommonUtil.anyNull("a", "b", null));
        assertTrue(CommonUtil.anyNull(null, null, "b"));
        assertFalse(CommonUtil.anyNull("a", "b", "c"));
    }

    @Test
    public void testAnyNull_VarArgs() {
        assertFalse(CommonUtil.anyNull());
        assertFalse(CommonUtil.anyNull("a", "b", "c"));
        assertTrue(CommonUtil.anyNull("a", null, "c"));
        assertFalse(CommonUtil.anyNull((Object[]) null));
        assertTrue(CommonUtil.anyNull(new Object[] { null }));
    }

    @Test
    public void testAnyNull_VarArgs_explicitNullArray() {
        assertFalse(CommonUtil.anyNull((Object[]) null));
    }

    @Test
    public void testAnyNull_Iterable() {
        assertFalse(CommonUtil.anyNull((Iterable<?>) null));
        assertFalse(CommonUtil.anyNull(Collections.emptyList()));
        assertFalse(CommonUtil.anyNull(Arrays.asList("a", "b")));
        assertTrue(CommonUtil.anyNull(Arrays.asList("a", null, "b")));
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        assertTrue(CommonUtil.anyNull(listWithNull));

    }

    @Test
    public void testAnyEmpty_TwoCharSequences() {
        assertTrue(CommonUtil.anyEmpty(null, "a"));
        assertTrue(CommonUtil.anyEmpty("", "a"));
        assertTrue(CommonUtil.anyEmpty("a", null));
        assertTrue(CommonUtil.anyEmpty("a", ""));
        assertFalse(CommonUtil.anyEmpty("a", "b"));
    }

    @Test
    public void testAnyEmpty_ThreeCharSequences() {
        assertTrue(CommonUtil.anyEmpty(null, "a", "b"));
        assertTrue(CommonUtil.anyEmpty("", "a", "b"));
        assertTrue(CommonUtil.anyEmpty("a", "", "b"));
        assertFalse(CommonUtil.anyEmpty("a", "b", "c"));
    }

    @Test
    public void testAnyEmpty_CharSequenceVarArgs() {
        assertTrue(CommonUtil.anyEmpty((String) null));
        assertFalse(CommonUtil.anyEmpty((String[]) null));
        assertTrue(CommonUtil.anyEmpty(null, "foo"));
        assertTrue(CommonUtil.anyEmpty("", "bar"));
        assertFalse(CommonUtil.anyEmpty("foo", "bar"));
        assertFalse(CommonUtil.anyEmpty(new String[] {}));
        assertTrue(CommonUtil.anyEmpty(new String[] { "" }));
    }

    @Test
    public void testAnyEmpty_CharSequenceIterable() {
        assertTrue(CommonUtil.anyEmpty(Arrays.asList(null, "a")));
        assertTrue(CommonUtil.anyEmpty(Arrays.asList("", "a")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.anyEmpty(Collections.<CharSequence> emptyList()));
    }

    @Test
    public void testAnyEmpty_TwoObjectArrays() {
        assertTrue(CommonUtil.anyEmpty(null, new Object[] { "a" }));
        assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "a" }));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, null));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0]));
        assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));
    }

    @Test
    public void testAnyEmpty_ThreeObjectArrays() {
        assertTrue(CommonUtil.anyEmpty(null, new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "b" }));
        assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));
    }

    @Test
    public void testAnyEmpty_TwoCollections() {
        assertTrue(CommonUtil.anyEmpty(null, Arrays.asList("a")));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));
    }

    @Test
    public void testAnyEmpty_ThreeCollections() {
        assertTrue(CommonUtil.anyEmpty(null, Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyList(), Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), Collections.emptyList(), Arrays.asList("b")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
    }

    @Test
    public void testAnyEmpty_TwoMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(CommonUtil.anyEmpty(null, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyMap(), nonEmptyMap));
        assertFalse(CommonUtil.anyEmpty(nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyEmpty_ThreeMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(CommonUtil.anyEmpty(null, nonEmptyMap, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyMap(), nonEmptyMap, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(nonEmptyMap, Collections.emptyMap(), nonEmptyMap));
        assertFalse(CommonUtil.anyEmpty(nonEmptyMap, nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyBlank_TwoCharSequences() {
        assertTrue(CommonUtil.anyBlank(null, "a"));
        assertTrue(CommonUtil.anyBlank(" ", "a"));
        assertTrue(CommonUtil.anyBlank("a", null));
        assertTrue(CommonUtil.anyBlank("a", " "));
        assertFalse(CommonUtil.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_ThreeCharSequences() {
        assertTrue(CommonUtil.anyBlank(null, "a", "b"));
        assertTrue(CommonUtil.anyBlank(" ", "a", "b"));
        assertTrue(CommonUtil.anyBlank("a", " ", "b"));
        assertFalse(CommonUtil.anyBlank("a", "b", "c"));
    }

    @Test
    public void testAnyBlank_CharSequenceVarArgs() {
        assertFalse(CommonUtil.anyBlank((CharSequence[]) null));
        assertFalse(CommonUtil.anyBlank());
        assertTrue(CommonUtil.anyBlank((CharSequence) null));
        assertTrue(CommonUtil.anyBlank(" "));
        assertTrue(CommonUtil.anyBlank("a", " "));
        assertFalse(CommonUtil.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_CharSequenceIterable() {
        assertFalse(CommonUtil.anyBlank((Iterable<CharSequence>) null));
        assertFalse(CommonUtil.anyBlank(Collections.emptyList()));
        assertTrue(CommonUtil.anyBlank(Arrays.asList(null, "a")));
        assertTrue(CommonUtil.anyBlank(Arrays.asList(" ", "a")));
        assertFalse(CommonUtil.anyBlank(Arrays.asList("a", "b")));
    }

    @Test
    public void testAllNull_TwoObjects() {
        assertFalse(CommonUtil.allNull(null, "a"));
        assertFalse(CommonUtil.allNull("a", null));
        assertTrue(CommonUtil.allNull(null, null));
        assertFalse(CommonUtil.allNull("a", "b"));
    }

    @Test
    public void testAllNull_ThreeObjects() {
        assertFalse(CommonUtil.allNull(null, "a", "b"));
        assertTrue(CommonUtil.allNull(null, null, null));
        assertFalse(CommonUtil.allNull("a", "b", "c"));
    }

    @Test
    public void testAllNull_VarArgs() {
        assertTrue(CommonUtil.allNull());
        assertTrue(CommonUtil.allNull((Object) null));
        assertTrue(CommonUtil.allNull(null, null, null));
        assertFalse(CommonUtil.allNull("a", null, null));
        assertFalse(CommonUtil.allNull("a", "b", "c"));
        assertTrue(CommonUtil.allNull((Object[]) null));
    }

    @Test
    public void testAllNull_Iterable() {
        assertTrue(CommonUtil.allNull((Iterable<?>) null));
        assertTrue(CommonUtil.allNull(Collections.emptyList()));
        assertFalse(CommonUtil.allNull(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.allNull(Arrays.asList("a", null, "b")));
        assertTrue(CommonUtil.allNull(Arrays.asList(null, null)));
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        listWithNulls.add(null);
        assertTrue(CommonUtil.allNull(listWithNulls));
    }

    @Test
    public void testAllEmpty_TwoCharSequences() {
        assertFalse(CommonUtil.allEmpty(null, "a"));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertTrue(CommonUtil.allEmpty((String) null, (String) null));
        assertTrue(CommonUtil.allEmpty("", ""));
        assertFalse(CommonUtil.allEmpty("a", ""));
    }

    @Test
    public void testAllEmpty_ThreeCharSequences() {
        assertFalse(CommonUtil.allEmpty(null, "", "a"));
        assertTrue(CommonUtil.allEmpty(null, "", null));
        assertFalse(CommonUtil.allEmpty("a", "b", "c"));
    }

    @Test
    public void testAllEmpty_CharSequenceVarArgs() {
        assertTrue(CommonUtil.allEmpty((CharSequence[]) null));
        assertTrue(CommonUtil.allEmpty());
        assertTrue(CommonUtil.allEmpty((CharSequence) null));
        assertTrue(CommonUtil.allEmpty(""));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertFalse(CommonUtil.allEmpty(null, "a"));
    }

    @Test
    public void testAllEmpty_CharSequenceIterable() {
        assertTrue(CommonUtil.allEmpty((Iterable<CharSequence>) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.allEmpty(Arrays.asList(null, "")));
        assertFalse(CommonUtil.allEmpty(Arrays.asList(null, "a")));
    }

    @Test
    public void testAllEmpty_TwoObjectArrays() {
        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], null));
        assertTrue(CommonUtil.allEmpty(null, new Object[0]));
        assertTrue(CommonUtil.allEmpty(new Object[0], new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "a" }, new Object[0]));
    }

    @Test
    public void testAllEmpty_ThreeObjectArrays() {
        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], null, new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "a" }, new Object[0], null));
    }

    @Test
    public void testAllEmpty_TwoCollections() {
        assertTrue(CommonUtil.allEmpty((List) null, (List) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), null));
        assertTrue(CommonUtil.allEmpty(null, Collections.emptyList()));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), Collections.emptyList()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("a"), Collections.emptyList()));
    }

    @Test
    public void testAllEmpty_ThreeCollections() {
        assertTrue(CommonUtil.allEmpty((List) null, (List) null, (List) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), null, Collections.emptyList()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("a"), Collections.emptyList(), null));
    }

    @Test
    public void testAllEmpty_TwoMaps() {
        assertTrue(CommonUtil.allEmpty((Map) null, (Map) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), null));
        assertTrue(CommonUtil.allEmpty(null, Collections.emptyMap()));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(CommonUtil.allEmpty(nonEmptyMap, Collections.emptyMap()));
    }

    @Test
    public void testAllEmpty_ThreeMaps() {
        assertTrue(CommonUtil.allEmpty((Map) null, (Map) null, (Map) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), null, Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(CommonUtil.allEmpty(nonEmptyMap, Collections.emptyMap(), null));
    }

    @Test
    public void testAllBlank_TwoCharSequences() {
        assertFalse(CommonUtil.allBlank(null, "a"));
        assertTrue(CommonUtil.allBlank(null, " "));
        assertTrue(CommonUtil.allBlank(null, null));
        assertTrue(CommonUtil.allBlank(" ", " "));
        assertFalse(CommonUtil.allBlank("a", " "));
    }

    @Test
    public void testAllBlank_ThreeCharSequences() {
        assertFalse(CommonUtil.allBlank(null, " ", "a"));
        assertTrue(CommonUtil.allBlank(null, " ", null));
        assertFalse(CommonUtil.allBlank("a", "b", "c"));
    }

    @Test
    public void testAllBlank_CharSequenceVarArgs() {
        assertTrue(CommonUtil.allBlank((CharSequence[]) null));
        assertTrue(CommonUtil.allBlank());
        assertTrue(CommonUtil.allBlank((CharSequence) null));
        assertTrue(CommonUtil.allBlank(" "));
        assertTrue(CommonUtil.allBlank(null, " ", "\t"));
        assertFalse(CommonUtil.allBlank(null, "a"));
    }

    @Test
    public void testAllBlank_CharSequenceIterable() {
        assertTrue(CommonUtil.allBlank((Iterable<CharSequence>) null));
        assertTrue(CommonUtil.allBlank(Collections.emptyList()));
        assertTrue(CommonUtil.allBlank(Arrays.asList(null, " ", "\t")));
        assertFalse(CommonUtil.allBlank(Arrays.asList(null, "a")));
    }

}
