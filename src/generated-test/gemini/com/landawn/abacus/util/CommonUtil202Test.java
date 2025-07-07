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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

// Assuming N.java and its dependencies (com.landawn.abacus.*, etc.) are in the classpath.
// Specifically, constants like Strings.NULL, Strings.ELEMENT_SEPARATOR, WD.BRACKET_L, etc.,
// and helper methods like N.typeOf(), N.CLASS_TYPE_ENUM, N.checkArgNotEmpty(), etc.,
// are assumed to be available and function as expected.
public class CommonUtil202Test extends TestBase {

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
            // In a real test, this would come from a mocked ClassUtil.getPropNameList(TestBean.class)
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
            // A more reflective toString might be used by N.toString(Object)
            return "TestBean@" + Integer.toHexString(System.identityHashCode(this));
        }
    }

    // Simplified mock for N.compareByProps for testing N.equalsByProps.
    // The actual N.compareByProps in com.landawn.abacus.util.N might be more complex
    // and use reflection or specific property accessors.
    // This mock assumes that N.equals is used for property value comparison.
    private static int mockCompareByProps(final Object bean1, final Object bean2, final Collection<String> propNamesToCompare) {
        if (bean1 == bean2)
            return 0;
        if (bean1 == null || bean2 == null)
            return bean1 == null ? -1 : 1;
        if (!bean1.getClass().equals(bean2.getClass()) || !(bean1 instanceof TestBean)) {
            return 1; // Not equal for simplicity if types differ or not TestBean
        }

        TestBean b1 = (TestBean) bean1;
        TestBean b2 = (TestBean) bean2;

        for (String propName : propNamesToCompare) {
            Object val1 = b1.getPropValue(propName); // Simplified property access
            Object val2 = b2.getPropValue(propName); // Simplified property access
            if (!N.equals(val1, val2)) {
                // This doesn't provide a true comparison value (-1, 0, 1) but indicates inequality.
                // A real compareByProps would provide a consistent ordering.
                return (val1 == null) ? -1 : ((val2 == null) ? 1 : String.valueOf(val1).compareTo(String.valueOf(val2)));
            }
        }
        return 0; // All specified properties are equal
    }

    // ================================ Tests for N.java (Part 1: length/size, isEmpty/isBlank...) =================================

    //region len/size tests
    @Test
    public void testLen_CharSequence() {
        assertEquals(0, N.len((CharSequence) null));
        assertEquals(0, N.len(""));
        assertEquals(3, N.len("abc"));
    }

    @Test
    public void testLen_booleanArray() {
        assertEquals(0, N.len((boolean[]) null));
        assertEquals(0, N.len(new boolean[0]));
        assertEquals(2, N.len(new boolean[] { true, false }));
    }

    @Test
    public void testLen_charArray() {
        assertEquals(0, N.len((char[]) null));
        assertEquals(0, N.len(new char[0]));
        assertEquals(3, N.len(new char[] { 'a', 'b', 'c' }));
    }

    @Test
    public void testLen_byteArray() {
        assertEquals(0, N.len((byte[]) null));
        assertEquals(0, N.len(new byte[0]));
        assertEquals(2, N.len(new byte[] { 1, 2 }));
    }

    @Test
    public void testLen_shortArray() {
        assertEquals(0, N.len((short[]) null));
        assertEquals(0, N.len(new short[0]));
        assertEquals(2, N.len(new short[] { 10, 20 }));
    }

    @Test
    public void testLen_intArray() {
        assertEquals(0, N.len((int[]) null));
        assertEquals(0, N.len(new int[0]));
        assertEquals(2, N.len(new int[] { 100, 200 }));
    }

    @Test
    public void testLen_longArray() {
        assertEquals(0, N.len((long[]) null));
        assertEquals(0, N.len(new long[0]));
        assertEquals(2, N.len(new long[] { 1000L, 2000L }));
    }

    @Test
    public void testLen_floatArray() {
        assertEquals(0, N.len((float[]) null));
        assertEquals(0, N.len(new float[0]));
        assertEquals(2, N.len(new float[] { 1.0f, 2.0f }));
    }

    @Test
    public void testLen_doubleArray() {
        assertEquals(0, N.len((double[]) null));
        assertEquals(0, N.len(new double[0]));
        assertEquals(2, N.len(new double[] { 1.0, 2.0 }));
    }

    @Test
    public void testLen_ObjectArray() {
        assertEquals(0, N.len((Object[]) null));
        assertEquals(0, N.len(new Object[0]));
        assertEquals(2, N.len(new Object[] { "a", "b" }));
    }

    @Test
    public void testSize_Collection() {
        assertEquals(0, N.size((Collection<?>) null));
        assertEquals(0, N.size(Collections.emptyList()));
        assertEquals(2, N.size(Arrays.asList(1, 2)));
    }

    @Test
    public void testSize_Map() {
        assertEquals(0, N.size((Map<?, ?>) null));
        assertEquals(0, N.size(Collections.emptyMap()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals(2, N.size(map));
    }

    @Test
    public void testSize_PrimitiveList() {
        // Assuming PrimitiveList is a mockable or instantiable type for testing
        // For now, we'll test with null as PrimitiveList might be an interface/abstract class
        // from com.landawn.abacus.util that's not fully defined here.
        assertEquals(0, N.size((PrimitiveList) null));
        // PrimitiveList mockList = mock(PrimitiveList.class);
        // when(mockList.size()).thenReturn(2);
        // assertEquals(2, N.size(mockList));
        // when(mockList.size()).thenReturn(0);
        // assertEquals(0, N.size(mockList));
    }

    //endregion

    //region isEmpty tests
    @Test
    public void testIsEmpty_CharSequence() {
        assertTrue(N.isEmpty((CharSequence) null));
        assertTrue(N.isEmpty(""));
        assertFalse(N.isEmpty("abc"));
        assertFalse(N.isEmpty(" "));
    }

    @Test
    public void testIsEmpty_booleanArray() {
        assertTrue(N.isEmpty((boolean[]) null));
        assertTrue(N.isEmpty(new boolean[0]));
        assertFalse(N.isEmpty(new boolean[] { true }));
    }

    @Test
    public void testIsEmpty_charArray() {
        assertTrue(N.isEmpty((char[]) null));
        assertTrue(N.isEmpty(new char[0]));
        assertFalse(N.isEmpty(new char[] { 'a' }));
    }

    @Test
    public void testIsEmpty_byteArray() {
        assertTrue(N.isEmpty((byte[]) null));
        assertTrue(N.isEmpty(new byte[0]));
        assertFalse(N.isEmpty(new byte[] { 1 }));
    }

    @Test
    public void testIsEmpty_shortArray() {
        assertTrue(N.isEmpty((short[]) null));
        assertTrue(N.isEmpty(new short[0]));
        assertFalse(N.isEmpty(new short[] { 1 }));
    }

    @Test
    public void testIsEmpty_intArray() {
        assertTrue(N.isEmpty((int[]) null));
        assertTrue(N.isEmpty(new int[0]));
        assertFalse(N.isEmpty(new int[] { 1 }));
    }

    @Test
    public void testIsEmpty_longArray() {
        assertTrue(N.isEmpty((long[]) null));
        assertTrue(N.isEmpty(new long[0]));
        assertFalse(N.isEmpty(new long[] { 1L }));
    }

    @Test
    public void testIsEmpty_floatArray() {
        assertTrue(N.isEmpty((float[]) null));
        assertTrue(N.isEmpty(new float[0]));
        assertFalse(N.isEmpty(new float[] { 1f }));
    }

    @Test
    public void testIsEmpty_doubleArray() {
        assertTrue(N.isEmpty((double[]) null));
        assertTrue(N.isEmpty(new double[0]));
        assertFalse(N.isEmpty(new double[] { 1.0 }));
    }

    @Test
    public void testIsEmpty_ObjectArray() {
        assertTrue(N.isEmpty((Object[]) null));
        assertTrue(N.isEmpty(new Object[0]));
        assertFalse(N.isEmpty(new Object[] { "a" }));
    }

    @Test
    public void testIsEmpty_Collection() {
        assertTrue(N.isEmpty((Collection<?>) null));
        assertTrue(N.isEmpty(Collections.emptyList()));
        assertFalse(N.isEmpty(Arrays.asList(1, 2)));
    }

    @Test
    public void testIsEmpty_Iterable() {
        assertTrue(N.isEmpty((Iterable<?>) null));
        assertTrue(N.isEmpty(Collections.emptyList())); // Test with a Collection
        assertFalse(N.isEmpty(Arrays.asList(1)));

        // Test with a non-Collection Iterable (e.g., custom Iterable)
        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertTrue(N.isEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertFalse(N.isEmpty(nonEmptyIterable));
    }

    @Test
    public void testIsEmpty_Iterator() {
        assertTrue(N.isEmpty((Iterator<?>) null));
        assertTrue(N.isEmpty(Collections.emptyIterator()));
        assertFalse(N.isEmpty(Arrays.asList(1, 2).iterator()));
    }

    @Test
    public void testIsEmpty_Map() {
        assertTrue(N.isEmpty((Map<?, ?>) null));
        assertTrue(N.isEmpty(Collections.emptyMap()));
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertFalse(N.isEmpty(map));
    }

    @Test
    public void testIsEmpty_PrimitiveList() {
        assertTrue(N.isEmpty((PrimitiveList) null));
        // PrimitiveList emptyList = mock(PrimitiveList.class);
        // when(emptyList.isEmpty()).thenReturn(true);
        // assertTrue(N.isEmpty(emptyList));
        // PrimitiveList nonEmptyList = mock(PrimitiveList.class);
        // when(nonEmptyList.isEmpty()).thenReturn(false);
        // assertFalse(N.isEmpty(nonEmptyList));
    }

    @Test
    public void testIsEmpty_Multiset() {
        assertTrue(N.isEmpty((Multiset<?>) null));
        // Multiset emptySet = mock(Multiset.class);
        // when(emptySet.isEmpty()).thenReturn(true);
        // assertTrue(N.isEmpty(emptySet));
        // Multiset nonEmptySet = mock(Multiset.class);
        // when(nonEmptySet.isEmpty()).thenReturn(false);
        // assertFalse(N.isEmpty(nonEmptySet));
    }

    @Test
    public void testIsEmpty_Multimap() {
        assertTrue(N.isEmpty((Multimap<?, ?, ?>) null));
        // Multimap emptyMap = mock(Multimap.class);
        // when(emptyMap.isEmpty()).thenReturn(true);
        // assertTrue(N.isEmpty(emptyMap));
        // Multimap nonEmptyMap = mock(Multimap.class);
        // when(nonEmptyMap.isEmpty()).thenReturn(false);
        // assertFalse(N.isEmpty(nonEmptyMap));
    }

    @Test
    public void testIsEmpty_DataSet() {
        assertTrue(N.isEmpty((DataSet) null));
        // DataSet emptyDs = mock(DataSet.class);
        // when(emptyDs.isEmpty()).thenReturn(true);
        // assertTrue(N.isEmpty(emptyDs));
        // DataSet nonEmptyDs = mock(DataSet.class);
        // when(nonEmptyDs.isEmpty()).thenReturn(false);
        // assertFalse(N.isEmpty(nonEmptyDs));
    }

    //endregion

    //region isBlank, isTrue, isFalse, isNotTrue, isNotFalse tests
    @Test
    public void testIsBlank() {
        assertTrue(N.isBlank(null));
        assertTrue(N.isBlank(""));
        assertTrue(N.isBlank(" "));
        assertTrue(N.isBlank("   "));
        assertTrue(N.isBlank("\t\n\r"));
        assertFalse(N.isBlank("abc"));
        assertFalse(N.isBlank(" a "));
    }

    @Test
    public void testIsTrue() {
        assertTrue(N.isTrue(Boolean.TRUE));
        assertFalse(N.isTrue(Boolean.FALSE));
        assertFalse(N.isTrue(null));
    }

    @Test
    public void testIsNotTrue() {
        assertTrue(N.isNotTrue(null));
        assertTrue(N.isNotTrue(Boolean.FALSE));
        assertFalse(N.isNotTrue(Boolean.TRUE));
    }

    @Test
    public void testIsFalse() {
        assertTrue(N.isFalse(Boolean.FALSE));
        assertFalse(N.isFalse(Boolean.TRUE));
        assertFalse(N.isFalse(null));
    }

    @Test
    public void testIsNotFalse() {
        assertTrue(N.isNotFalse(null));
        assertTrue(N.isNotFalse(Boolean.TRUE));
        assertFalse(N.isNotFalse(Boolean.FALSE));
    }
    //endregion

    //region notEmpty tests
    @Test
    public void testNotEmpty_CharSequence() {
        assertFalse(N.notEmpty((String) null));
        assertFalse(N.notEmpty(""));
        assertTrue(N.notEmpty("abc"));
        assertTrue(N.notEmpty(" "));
    }

    // Similar notEmpty tests for all array types, Collection, Iterable, Iterator, Map, etc.
    @Test
    public void testNotEmpty_ObjectArray() {
        assertFalse(N.notEmpty((Object[]) null));
        assertFalse(N.notEmpty(new Object[0]));
        assertTrue(N.notEmpty(new Object[] { "a" }));
    }

    @Test
    public void testNotEmpty_Collection() {
        assertFalse(N.notEmpty((Collection<?>) null));
        assertFalse(N.notEmpty(Collections.emptyList()));
        assertTrue(N.notEmpty(Arrays.asList(1)));
    }

    @Test
    public void testNotEmpty_Iterable() {
        assertFalse(N.notEmpty((Iterable<?>) null));
        assertFalse(N.notEmpty(Collections.emptyList())); // Test with a Collection
        assertTrue(N.notEmpty(Arrays.asList(1)));

        Iterable<Integer> emptyIterable = () -> Collections.<Integer> emptyIterator();
        assertFalse(N.notEmpty(emptyIterable));

        Iterable<Integer> nonEmptyIterable = () -> Arrays.asList(1, 2).iterator();
        assertTrue(N.notEmpty(nonEmptyIterable));
    }

    @Test
    public void testNotEmpty_Iterator() {
        assertFalse(N.notEmpty((Iterator<?>) null));
        assertFalse(N.notEmpty(Collections.emptyIterator()));
        assertTrue(N.notEmpty(Arrays.asList(1).iterator()));
    }

    @Test
    public void testNotEmpty_Map() {
        assertFalse(N.notEmpty((Map<Object, Object>) null));
        assertFalse(N.notEmpty(Collections.emptyMap()));
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(N.notEmpty(map));
    }

    //endregion

    //region notBlank tests
    @Test
    public void testNotBlank() {
        assertFalse(N.notBlank(null));
        assertFalse(N.notBlank(""));
        assertFalse(N.notBlank(" "));
        assertTrue(N.notBlank("abc"));
        assertTrue(N.notBlank(" a "));
    }
    //endregion

    //region anyNull tests
    @Test
    public void testAnyNull_TwoObjects() {
        assertTrue(N.anyNull(null, "a"));
        assertTrue(N.anyNull("a", null));
        assertTrue(N.anyNull(null, null));
        assertFalse(N.anyNull("a", "b"));
    }

    @Test
    public void testAnyNull_ThreeObjects() {
        assertTrue(N.anyNull(null, "a", "b"));
        assertTrue(N.anyNull("a", null, "b"));
        assertTrue(N.anyNull("a", "b", null));
        assertTrue(N.anyNull(null, null, "b"));
        assertFalse(N.anyNull("a", "b", "c"));
    }

    @Test
    public void testAnyNull_VarArgs() {
        assertFalse(N.anyNull()); // Empty varargs
        assertFalse(N.anyNull("a", "b", "c"));
        assertTrue(N.anyNull("a", null, "c"));
        assertFalse(N.anyNull((Object[]) null)); // This is tricky: is the array itself null, or an array containing nulls?
                                                 // N.anyNull((Object[])null) -> isEmpty checks if array is null, returns false.
                                                 // This behavior might be unexpected for some.
                                                 // If you pass a literal null, it becomes Object[] {null}
                                                 // N.anyNull((Object)null) -> this would be one element which is null -> true
        assertTrue(N.anyNull(new Object[] { null }));
    }

    @Test
    public void testAnyNull_VarArgs_explicitNullArray() {
        // This path is specific: if the varargs array itself is null.
        // The implementation of isEmpty(Object[]) will return true for a null array,
        // so anyNull((Object[]) null) -> isEmpty returns true, so loop is skipped, returns false.
        assertFalse(N.anyNull((Object[]) null));
    }

    @Test
    public void testAnyNull_Iterable() {
        assertFalse(N.anyNull((Iterable<?>) null));
        assertFalse(N.anyNull(Collections.emptyList()));
        assertFalse(N.anyNull(Arrays.asList("a", "b")));
        assertTrue(N.anyNull(Arrays.asList("a", null, "b")));
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        assertTrue(N.anyNull(listWithNull));

    }
    //endregion

    //region anyEmpty tests (CharSequence, Array, Collection, Map)
    @Test
    public void testAnyEmpty_TwoCharSequences() {
        assertTrue(N.anyEmpty(null, "a"));
        assertTrue(N.anyEmpty("", "a"));
        assertTrue(N.anyEmpty("a", null));
        assertTrue(N.anyEmpty("a", ""));
        assertFalse(N.anyEmpty("a", "b"));
    }

    @Test
    public void testAnyEmpty_ThreeCharSequences() {
        assertTrue(N.anyEmpty(null, "a", "b"));
        assertTrue(N.anyEmpty("", "a", "b"));
        assertTrue(N.anyEmpty("a", "", "b"));
        assertFalse(N.anyEmpty("a", "b", "c"));
    }

    @Test
    public void testAnyEmpty_CharSequenceVarArgs() {
        // This delegates to Strings.isAnyEmpty
        assertTrue(N.anyEmpty((String) null));
        assertFalse(N.anyEmpty((String[]) null)); // Strings.isAnyEmpty behavior
        assertTrue(N.anyEmpty(null, "foo"));
        assertTrue(N.anyEmpty("", "bar"));
        assertFalse(N.anyEmpty("foo", "bar"));
        assertFalse(N.anyEmpty(new String[] {})); // Strings.isAnyEmpty behavior
        assertTrue(N.anyEmpty(new String[] { "" }));
    }

    @Test
    public void testAnyEmpty_CharSequenceIterable() {
        // This delegates to Strings.isAnyEmpty
        assertTrue(N.anyEmpty(Arrays.asList(null, "a")));
        assertTrue(N.anyEmpty(Arrays.asList("", "a")));
        assertFalse(N.anyEmpty(Arrays.asList("a", "b")));
        assertFalse(N.anyEmpty(Collections.<CharSequence> emptyList())); // Strings.isAnyEmpty behavior
    }

    @Test
    public void testAnyEmpty_TwoObjectArrays() {
        assertTrue(N.anyEmpty(null, new Object[] { "a" }));
        assertTrue(N.anyEmpty(new Object[0], new Object[] { "a" }));
        assertTrue(N.anyEmpty(new Object[] { "a" }, null));
        assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0]));
        assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));
    }

    @Test
    public void testAnyEmpty_ThreeObjectArrays() {
        assertTrue(N.anyEmpty(null, new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(N.anyEmpty(new Object[0], new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(N.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "b" }));
        assertFalse(N.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));
    }

    @Test
    public void testAnyEmpty_TwoCollections() {
        assertTrue(N.anyEmpty(null, Arrays.asList("a")));
        assertTrue(N.anyEmpty(Collections.emptyList(), Arrays.asList("a")));
        assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b")));
    }

    @Test
    public void testAnyEmpty_ThreeCollections() {
        assertTrue(N.anyEmpty(null, Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(N.anyEmpty(Collections.emptyList(), Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(N.anyEmpty(Arrays.asList("a"), Collections.emptyList(), Arrays.asList("b")));
        assertFalse(N.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));
    }

    @Test
    public void testAnyEmpty_TwoMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(N.anyEmpty(null, nonEmptyMap));
        assertTrue(N.anyEmpty(Collections.emptyMap(), nonEmptyMap));
        assertFalse(N.anyEmpty(nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyEmpty_ThreeMaps() {
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(N.anyEmpty(null, nonEmptyMap, nonEmptyMap));
        assertTrue(N.anyEmpty(Collections.emptyMap(), nonEmptyMap, nonEmptyMap));
        assertTrue(N.anyEmpty(nonEmptyMap, Collections.emptyMap(), nonEmptyMap));
        assertFalse(N.anyEmpty(nonEmptyMap, nonEmptyMap, nonEmptyMap));
    }

    //endregion

    //region anyBlank tests
    @Test
    public void testAnyBlank_TwoCharSequences() {
        assertTrue(N.anyBlank(null, "a"));
        assertTrue(N.anyBlank(" ", "a"));
        assertTrue(N.anyBlank("a", null));
        assertTrue(N.anyBlank("a", " "));
        assertFalse(N.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_ThreeCharSequences() {
        assertTrue(N.anyBlank(null, "a", "b"));
        assertTrue(N.anyBlank(" ", "a", "b"));
        assertTrue(N.anyBlank("a", " ", "b"));
        assertFalse(N.anyBlank("a", "b", "c"));
    }

    @Test
    public void testAnyBlank_CharSequenceVarArgs() {
        assertFalse(N.anyBlank((CharSequence[]) null)); // N.isEmpty on null array is true, then returns false
        assertFalse(N.anyBlank()); // N.isEmpty on empty array is true, then returns false
        assertTrue(N.anyBlank((CharSequence) null));
        assertTrue(N.anyBlank(" "));
        assertTrue(N.anyBlank("a", " "));
        assertFalse(N.anyBlank("a", "b"));
    }

    @Test
    public void testAnyBlank_CharSequenceIterable() {
        assertFalse(N.anyBlank((Iterable<CharSequence>) null));
        assertFalse(N.anyBlank(Collections.emptyList()));
        assertTrue(N.anyBlank(Arrays.asList(null, "a")));
        assertTrue(N.anyBlank(Arrays.asList(" ", "a")));
        assertFalse(N.anyBlank(Arrays.asList("a", "b")));
    }
    //endregion

    //region allNull tests
    @Test
    public void testAllNull_TwoObjects() {
        assertFalse(N.allNull(null, "a"));
        assertFalse(N.allNull("a", null));
        assertTrue(N.allNull(null, null));
        assertFalse(N.allNull("a", "b"));
    }

    @Test
    public void testAllNull_ThreeObjects() {
        assertFalse(N.allNull(null, "a", "b"));
        assertTrue(N.allNull(null, null, null));
        assertFalse(N.allNull("a", "b", "c"));
    }

    @Test
    public void testAllNull_VarArgs() {
        assertTrue(N.allNull()); // Empty varargs
        assertTrue(N.allNull((Object) null));
        assertTrue(N.allNull(null, null, null));
        assertFalse(N.allNull("a", null, null));
        assertFalse(N.allNull("a", "b", "c"));
        assertTrue(N.allNull((Object[]) null)); // N.isEmpty on null array is true, returns true
    }

    @Test
    public void testAllNull_Iterable() {
        assertTrue(N.allNull((Iterable<?>) null));
        assertTrue(N.allNull(Collections.emptyList()));
        assertFalse(N.allNull(Arrays.asList("a", "b")));
        assertFalse(N.allNull(Arrays.asList("a", null, "b")));
        assertTrue(N.allNull(Arrays.asList(null, null)));
        List<String> listWithNulls = new ArrayList<>();
        listWithNulls.add(null);
        listWithNulls.add(null);
        assertTrue(N.allNull(listWithNulls));
    }
    //endregion

    //region allEmpty tests
    @Test
    public void testAllEmpty_TwoCharSequences() {
        assertFalse(N.allEmpty(null, "a"));
        assertTrue(N.allEmpty(null, ""));
        assertTrue(N.allEmpty((String) null, (String) null));
        assertTrue(N.allEmpty("", ""));
        assertFalse(N.allEmpty("a", ""));
    }

    @Test
    public void testAllEmpty_ThreeCharSequences() {
        assertFalse(N.allEmpty(null, "", "a"));
        assertTrue(N.allEmpty(null, "", null));
        assertFalse(N.allEmpty("a", "b", "c"));
    }

    @Test
    public void testAllEmpty_CharSequenceVarArgs() {
        assertTrue(N.allEmpty((CharSequence[]) null)); // isEmpty is true for null array.
        assertTrue(N.allEmpty()); // isEmpty is true for empty array.
        assertTrue(N.allEmpty((CharSequence) null));
        assertTrue(N.allEmpty(""));
        assertTrue(N.allEmpty(null, ""));
        assertFalse(N.allEmpty(null, "a"));
    }

    @Test
    public void testAllEmpty_CharSequenceIterable() {
        assertTrue(N.allEmpty((Iterable<CharSequence>) null));
        assertTrue(N.allEmpty(Collections.emptyList()));
        assertTrue(N.allEmpty(Arrays.asList(null, "")));
        assertFalse(N.allEmpty(Arrays.asList(null, "a")));
    }

    @Test
    public void testAllEmpty_TwoObjectArrays() {
        assertTrue(N.allEmpty((Object[]) null, (Object[]) null));
        assertTrue(N.allEmpty(new Object[0], null));
        assertTrue(N.allEmpty(null, new Object[0]));
        assertTrue(N.allEmpty(new Object[0], new Object[0]));
        assertFalse(N.allEmpty(new Object[] { "a" }, new Object[0]));
    }

    @Test
    public void testAllEmpty_ThreeObjectArrays() {
        assertTrue(N.allEmpty((Object[]) null, (Object[]) null, (Object[]) null));
        assertTrue(N.allEmpty(new Object[0], null, new Object[0]));
        assertFalse(N.allEmpty(new Object[] { "a" }, new Object[0], null));
    }

    @Test
    public void testAllEmpty_TwoCollections() {
        assertTrue(N.allEmpty((List) null, (List) null));
        assertTrue(N.allEmpty(Collections.emptyList(), null));
        assertTrue(N.allEmpty(null, Collections.emptyList()));
        assertTrue(N.allEmpty(Collections.emptyList(), Collections.emptyList()));
        assertFalse(N.allEmpty(Arrays.asList("a"), Collections.emptyList()));
    }

    @Test
    public void testAllEmpty_ThreeCollections() {
        assertTrue(N.allEmpty((List) null, (List) null, (List) null));
        assertTrue(N.allEmpty(Collections.emptyList(), null, Collections.emptyList()));
        assertFalse(N.allEmpty(Arrays.asList("a"), Collections.emptyList(), null));
    }

    @Test
    public void testAllEmpty_TwoMaps() {
        assertTrue(N.allEmpty((Map) null, (Map) null));
        assertTrue(N.allEmpty(Collections.emptyMap(), null));
        assertTrue(N.allEmpty(null, Collections.emptyMap()));
        assertTrue(N.allEmpty(Collections.emptyMap(), Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(N.allEmpty(nonEmptyMap, Collections.emptyMap()));
    }

    @Test
    public void testAllEmpty_ThreeMaps() {
        assertTrue(N.allEmpty((Map) null, (Map) null, (Map) null));
        assertTrue(N.allEmpty(Collections.emptyMap(), null, Collections.emptyMap()));
        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertFalse(N.allEmpty(nonEmptyMap, Collections.emptyMap(), null));
    }

    //endregion

    //region allBlank tests
    @Test
    public void testAllBlank_TwoCharSequences() {
        assertFalse(N.allBlank(null, "a"));
        assertTrue(N.allBlank(null, " "));
        assertTrue(N.allBlank(null, null));
        assertTrue(N.allBlank(" ", " "));
        assertFalse(N.allBlank("a", " "));
    }

    @Test
    public void testAllBlank_ThreeCharSequences() {
        assertFalse(N.allBlank(null, " ", "a"));
        assertTrue(N.allBlank(null, " ", null));
        assertFalse(N.allBlank("a", "b", "c"));
    }

    @Test
    public void testAllBlank_CharSequenceVarArgs() {
        assertTrue(N.allBlank((CharSequence[]) null)); // isEmpty true
        assertTrue(N.allBlank()); // isEmpty true
        assertTrue(N.allBlank((CharSequence) null));
        assertTrue(N.allBlank(" "));
        assertTrue(N.allBlank(null, " ", "\t"));
        assertFalse(N.allBlank(null, "a"));
    }

    @Test
    public void testAllBlank_CharSequenceIterable() {
        assertTrue(N.allBlank((Iterable<CharSequence>) null));
        assertTrue(N.allBlank(Collections.emptyList()));
        assertTrue(N.allBlank(Arrays.asList(null, " ", "\t")));
        assertFalse(N.allBlank(Arrays.asList(null, "a")));
    }
    //endregion

    // ================================ Tests for N.java (Part 2: equals/hashCode/toString...) =================================

    // ... Similar tests for byte[], short[], int[], long[], float[], double[] (full and range)

}
