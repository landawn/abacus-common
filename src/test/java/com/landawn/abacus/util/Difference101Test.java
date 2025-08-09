package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;

public class Difference101Test extends TestBase {

    // Additional tests for MapDifference.of with collection of maps and different key extractors
    @Test
    public void testMapDifferenceCollectionsWithDifferentIdExtractors() {
        // First collection of maps with String keys
        Map<String, Object> map1a = new HashMap<>();
        map1a.put("id", "A");
        map1a.put("value", 100);
        map1a.put("type", "X");

        Map<String, Object> map1b = new HashMap<>();
        map1b.put("id", "B");
        map1b.put("value", 200);
        map1b.put("type", "Y");

        List<Map<String, Object>> col1 = Arrays.asList(map1a, map1b);

        // Second collection of maps with different structure
        Map<String, Object> map2a = new HashMap<>();
        map2a.put("code", "A");
        map2a.put("value", 100);
        map2a.put("category", "Z");

        Map<String, Object> map2c = new HashMap<>();
        map2c.put("code", "C");
        map2c.put("value", 300);
        map2c.put("category", "W");

        List<Map<String, Object>> col2 = Arrays.asList(map2a, map2c);

        // Different id extractors for each collection
        Function<Map<? extends String, ? extends Object>, String> idExtractor1 = m -> (String) m.get("id");
        Function<Map<? extends String, ? extends Object>, String> idExtractor2 = m -> (String) m.get("code");

        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, Map<String, MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = MapDifference
                .of(col1, col2, idExtractor1, idExtractor2);

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.onLeftOnly().size());
        assertEquals("B", diff.onLeftOnly().get(0).get("id"));

        assertEquals(1, diff.onRightOnly().size());
        assertEquals("C", diff.onRightOnly().get(0).get("code"));

        assertEquals(1, diff.withDifferentValues().size());
        assertTrue(diff.withDifferentValues().containsKey("A"));

        // Check the inner difference for map with id/code "A"
        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.withDifferentValues().get("A");
        assertEquals(1, innerDiff.inCommon().size());
        assertEquals(100, innerDiff.inCommon().get("value"));
        assertEquals(2, innerDiff.onLeftOnly().size()); // id and type
        assertEquals(2, innerDiff.onRightOnly().size()); // code and category
    }

    @Test
    public void testMapDifferenceCollectionsWithKeysToCompareAndDifferentExtractors() {
        Map<String, Object> map1a = new HashMap<>();
        map1a.put("id", 1);
        map1a.put("name", "Alice");
        map1a.put("age", 25);
        map1a.put("city", "NYC");

        Map<String, Object> map1b = new HashMap<>();
        map1b.put("id", 2);
        map1b.put("name", "Bob");
        map1b.put("age", 30);
        map1b.put("city", "LA");

        List<Map<String, Object>> col1 = Arrays.asList(map1a, map1b);

        Map<String, Object> map2a = new HashMap<>();
        map2a.put("userId", 1);
        map2a.put("name", "Alice");
        map2a.put("age", 26); // Different age
        map2a.put("country", "USA");

        Map<String, Object> map2c = new HashMap<>();
        map2c.put("userId", 3);
        map2c.put("name", "Charlie");
        map2c.put("age", 35);
        map2c.put("country", "Canada");

        List<Map<String, Object>> col2 = Arrays.asList(map2a, map2c);

        Collection<String> keysToCompare = Arrays.asList("name", "age");

        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, Map<Integer, MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = MapDifference
                .of(col1, col2, keysToCompare, m -> (Integer) m.get("id"), m -> (Integer) m.get("userId"));

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.onLeftOnly().size());
        assertEquals("Bob", diff.onLeftOnly().get(0).get("name"));

        assertEquals(1, diff.onRightOnly().size());
        assertEquals("Charlie", diff.onRightOnly().get(0).get("name"));

        assertEquals(1, diff.withDifferentValues().size());
        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.withDifferentValues().get(1);
        assertEquals(1, innerDiff.inCommon().size());
        assertEquals("Alice", innerDiff.inCommon().get("name"));
        assertEquals(1, innerDiff.withDifferentValues().size());
        assertEquals(Pair.of(25, 26), innerDiff.withDifferentValues().get("age"));
    }

    @Test
    public void testMapDifferenceEmptyCollections() {
        List<Map<String, Integer>> emptyList1 = new ArrayList<>();
        List<Map<String, Integer>> emptyList2 = new ArrayList<>();

        MapDifference<List<Map<String, Integer>>, List<Map<String, Integer>>, Map<Integer, MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>>>> diff = MapDifference
                .of(emptyList1, emptyList2, m -> m.get("id"));

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testMapDifferenceCollectionsOneEmpty() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("id", "X");
        map1.put("value", "test");
        List<Map<String, String>> list1 = Arrays.asList(map1);
        List<Map<String, String>> emptyList = new ArrayList<>();

        MapDifference<List<Map<String, String>>, List<Map<String, String>>, Map<String, MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>>>> diff1 = MapDifference
                .of(list1, emptyList, m -> m.get("id"));

        assertEquals(1, diff1.onLeftOnly().size());
        assertTrue(diff1.onRightOnly().isEmpty());
        assertFalse(diff1.areEqual());

        MapDifference<List<Map<String, String>>, List<Map<String, String>>, Map<String, MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>>>> diff2 = MapDifference
                .of(emptyList, list1, m -> m.get("id"));

        assertTrue(diff2.onLeftOnly().isEmpty());
        assertEquals(1, diff2.onRightOnly().size());
        assertFalse(diff2.areEqual());
    }

    // Additional BeanDifference tests
    public static class PersonBean {
        private Long id;
        private String firstName;
        private String lastName;
        private Integer age;
        private Address address;

        public PersonBean() {
        }

        public PersonBean(Long id, String firstName, String lastName, Integer age) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
            this.age = age;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public Address getAddress() {
            return address;
        }

        public void setAddress(Address address) {
            this.address = address;
        }
    }

    public static class Address {
        private String street;
        private String city;

        public Address() {
        }

        public Address(String street, String city) {
            this.street = street;
            this.city = city;
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            Address address = (Address) obj;
            return Objects.equals(street, address.street) && Objects.equals(city, address.city);
        }

        @Override
        public int hashCode() {
            return Objects.hash(street, city);
        }
    }

    @Test
    public void testBeanDifferenceWithNestedObjects() {
        PersonBean person1 = new PersonBean(1L, "John", "Doe", 30);
        person1.setAddress(new Address("123 Main St", "NYC"));

        PersonBean person2 = new PersonBean(1L, "John", "Doe", 31);
        person2.setAddress(new Address("123 Main St", "NYC"));

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2);

        assertEquals(4, diff.inCommon().size()); // id, firstName, lastName, address
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(30, 31), diff.withDifferentValues().get("age"));
    }

    @Test
    public void testBeanDifferenceWithNullNestedObjects() {
        PersonBean person1 = new PersonBean(1L, "John", "Doe", 30);
        person1.setAddress(new Address("123 Main St", "NYC"));

        PersonBean person2 = new PersonBean(1L, "John", "Doe", 30);
        person2.setAddress(null);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2);

        assertEquals(4, diff.inCommon().size()); // id, firstName, lastName
        assertEquals(1, diff.withDifferentValues().size());
        assertTrue(diff.withDifferentValues().containsKey("address"));
        assertEquals(person1.getAddress(), diff.withDifferentValues().get("address").left());
        assertNull(diff.withDifferentValues().get("address").right());
    }

    @Test
    public void testBeanDifferenceCollectionsWithDifferentIdExtractors() {
        PersonBean person1a = new PersonBean(1L, "Alice", "Smith", 25);
        PersonBean person1b = new PersonBean(2L, "Bob", "Jones", 30);
        List<PersonBean> list1 = Arrays.asList(person1a, person1b);

        PersonBean person2a = new PersonBean(10L, "Alice", "Smith", 26); // Different id but same name
        PersonBean person2c = new PersonBean(30L, "Charlie", "Brown", 35);
        List<PersonBean> list2 = Arrays.asList(person2a, person2c);

        // Use full name as identifier instead of id
        Function<PersonBean, String> nameExtractor = p -> p.getFirstName() + " " + p.getLastName();

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(list1, list2, nameExtractor);

        assertTrue(diff.inCommon().isEmpty());
        assertEquals(1, diff.onLeftOnly().size());
        assertEquals("Bob", diff.onLeftOnly().get(0).getFirstName());

        assertEquals(1, diff.onRightOnly().size());
        assertEquals("Charlie", diff.onRightOnly().get(0).getFirstName());

        assertEquals(1, diff.withDifferentValues().size());
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> aliceDiff = diff.withDifferentValues().get("Alice Smith");
        assertEquals(2, aliceDiff.inCommon().size()); // firstName, lastName
        assertEquals(2, aliceDiff.withDifferentValues().size()); // id and age are different
    }

    @Test
    public void testBeanDifferenceCollectionsWithPropNamesAndDifferentExtractors() {
        PersonBean person1a = new PersonBean(1L, "Alice", "Smith", 25);
        PersonBean person1b = new PersonBean(2L, "Bob", "Jones", 30);
        List<PersonBean> list1 = Arrays.asList(person1a, person1b);

        PersonBean person2a = new PersonBean(100L, "Alice", "SMITH", 25); // Different id and lastName case
        PersonBean person2c = new PersonBean(200L, "Charlie", "Brown", 35);
        List<PersonBean> list2 = Arrays.asList(person2a, person2c);

        Collection<String> propsToCompare = Arrays.asList("firstName", "age");

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(list1, list2, propsToCompare, PersonBean::getFirstName, // Use firstName as identifier
                        PersonBean::getFirstName);

        assertEquals(1, diff.inCommon().size()); // Alice matches on firstName and age
        assertEquals("Alice", diff.inCommon().get(0).getFirstName());

        assertEquals(1, diff.onLeftOnly().size());
        assertEquals("Bob", diff.onLeftOnly().get(0).getFirstName());

        assertEquals(1, diff.onRightOnly().size());
        assertEquals("Charlie", diff.onRightOnly().get(0).getFirstName());

        assertTrue(diff.withDifferentValues().isEmpty()); // Alice matches on compared properties
    }

    @Test
    public void testBeanDifferenceWithBiPredicateValueEquivalence() {
        PersonBean person1 = new PersonBean(1L, "JOHN", "DOE", 30);
        PersonBean person2 = new PersonBean(1L, "john", "doe", 30);

        // Case-insensitive string comparison
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2, (v1, v2) -> {
            if (v1 instanceof String && v2 instanceof String) {
                return ((String) v1).equalsIgnoreCase((String) v2);
            }
            return Objects.equals(v1, v2);
        });

        assertEquals(4, diff.inCommon().size()); // All properties match with custom equivalence
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceEmptyCollections() {
        List<PersonBean> emptyList1 = new ArrayList<>();
        List<PersonBean> emptyList2 = new ArrayList<>();

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<Long, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(emptyList1, emptyList2, PersonBean::getId);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceCollectionsOneEmpty() {
        PersonBean person = new PersonBean(1L, "Test", "User", 25);
        List<PersonBean> list1 = Arrays.asList(person);
        List<PersonBean> emptyList = new ArrayList<>();

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<Long, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff1 = BeanDifference
                .of(list1, emptyList, PersonBean::getId);

        assertEquals(1, diff1.onLeftOnly().size());
        assertTrue(diff1.onRightOnly().isEmpty());
        assertFalse(diff1.areEqual());

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<Long, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff2 = BeanDifference
                .of(emptyList, list1, PersonBean::getId);

        assertTrue(diff2.onLeftOnly().isEmpty());
        assertEquals(1, diff2.onRightOnly().size());
        assertFalse(diff2.areEqual());
    }

    @Test
    public void testBeanDifferenceCollectionWithNonBeanClass() {
        List<String> stringList = Arrays.asList("not a bean");
        List<String> anotherList = Arrays.asList("also not a bean");

        assertThrows(IllegalArgumentException.class, () -> BeanDifference.of(stringList, anotherList, Function.identity()));
    }

    @Test
    public void testBeanDifferenceNullPropertyValues() {
        PersonBean person1 = new PersonBean(1L, "John", null, 30);
        PersonBean person2 = new PersonBean(1L, "John", "Doe", 30);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2);

        assertEquals(3, diff.inCommon().size()); // id and firstName
        assertEquals(1, diff.withDifferentValues().size());
        assertEquals(Pair.of(null, "Doe"), diff.withDifferentValues().get("lastName"));
    }

    @Test
    public void testBeanDifferenceBothNullPropertyValues() {
        PersonBean person1 = new PersonBean(1L, "John", null, null);
        PersonBean person2 = new PersonBean(1L, "John", null, null);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2);

        // When comparing all properties, null values are ignored
        assertEquals(2, diff.inCommon().size()); // id and firstName
        assertTrue(diff.withDifferentValues().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
    }

    @Test
    public void testBeanDifferenceBothNullPropertyValuesWithPropNames() {
        PersonBean person1 = new PersonBean(1L, "John", null, null);
        PersonBean person2 = new PersonBean(1L, "John", null, null);

        Collection<String> propsToCompare = Arrays.asList("lastName", "age");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2, propsToCompare);

        // When specific properties are compared, null values are included
        assertEquals(2, diff.inCommon().size()); // lastName and age (both null)
        assertTrue(diff.withDifferentValues().isEmpty());
    }

    // Test for complex scenarios with multiple differences
    @Test
    public void testComplexMapDifferenceScenario() {
        // Create complex maps with various data types
        Map<String, Object> map1 = new HashMap<>();
        map1.put("string", "value1");
        map1.put("integer", 42);
        map1.put("double", 3.14);
        map1.put("boolean", true);
        map1.put("list", Arrays.asList(1, 2, 3));
        map1.put("null", null);
        map1.put("onlyInFirst", "unique");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("string", "value2"); // Different
        map2.put("integer", 42); // Same
        map2.put("double", 3.14159); // Different
        map2.put("boolean", true); // Same
        map2.put("list", Arrays.asList(1, 2, 3)); // Same
        map2.put("null", null); // Same
        map2.put("onlyInSecond", "unique");

        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = MapDifference.of(map1, map2);

        assertEquals(4, diff.inCommon().size()); // integer, boolean, list, null
        assertEquals(1, diff.onLeftOnly().size());
        assertTrue(diff.onLeftOnly().containsKey("onlyInFirst"));
        assertEquals(1, diff.onRightOnly().size());
        assertTrue(diff.onRightOnly().containsKey("onlyInSecond"));
        assertEquals(2, diff.withDifferentValues().size());
        assertEquals(Pair.of("value1", "value2"), diff.withDifferentValues().get("string"));
        assertEquals(Pair.of(3.14, 3.14159), diff.withDifferentValues().get("double"));
    }

    // Test TreeMap to ensure sorted order is maintained
    @Test
    public void testMapDifferenceWithTreeMap() {
        Map<String, Integer> map1 = new TreeMap<>();
        map1.put("c", 3);
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new TreeMap<>();
        map2.put("b", 2);
        map2.put("c", 4);
        map2.put("d", 5);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        // Should preserve LinkedHashMap for sorted maps
        assertTrue(diff.inCommon() instanceof LinkedHashMap);
        assertTrue(diff.onLeftOnly() instanceof LinkedHashMap);
        assertTrue(diff.onRightOnly() instanceof LinkedHashMap);
        assertTrue(diff.withDifferentValues() instanceof LinkedHashMap);
    }

    // Edge case: Maps with same keys but all different values
    @Test
    public void testMapDifferenceAllDifferentValues() {
        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 10);
        map2.put("b", 20);
        map2.put("c", 30);

        MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>> diff = MapDifference.of(map1, map2);

        assertTrue(diff.inCommon().isEmpty());
        assertTrue(diff.onLeftOnly().isEmpty());
        assertTrue(diff.onRightOnly().isEmpty());
        assertEquals(3, diff.withDifferentValues().size());
        assertFalse(diff.areEqual());
    }

    // Test with very large collections to ensure performance
    @Test
    public void testDifferenceWithLargeCollections() {
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();

        // Create large lists with some overlap
        for (int i = 0; i < 10000; i++) {
            list1.add(i);
        }
        for (int i = 5000; i < 15000; i++) {
            list2.add(i);
        }

        Difference<List<Integer>, List<Integer>> diff = Difference.of(list1, list2);

        assertEquals(5000, diff.inCommon().size());
        assertEquals(5000, diff.onLeftOnly().size());
        assertEquals(5000, diff.onRightOnly().size());

        // Verify some samples
        assertTrue(diff.inCommon().contains(7500));
        assertTrue(diff.onLeftOnly().contains(2500));
        assertTrue(diff.onRightOnly().contains(12500));
    }

    // Test handling of collections with all duplicates
    @Test
    public void testDifferenceAllDuplicates() {
        List<String> list1 = Arrays.asList("a", "a", "a", "a");
        List<String> list2 = Arrays.asList("a", "a", "b", "b");

        Difference<List<String>, List<String>> diff = Difference.of(list1, list2);

        assertEquals(Arrays.asList("a", "a"), diff.inCommon());
        assertEquals(Arrays.asList("a", "a"), diff.onLeftOnly());
        assertEquals(Arrays.asList("b", "b"), diff.onRightOnly());
    }

    // Test primitive lists with extreme values
    @Test
    public void testPrimitiveListsWithExtremeValues() {
        IntList list1 = IntList.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
        IntList list2 = IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);

        Difference<IntList, IntList> diff = Difference.of(list1, list2);

        assertEquals(IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), diff.inCommon());
        assertEquals(IntList.of(-1, 1), diff.onLeftOnly());
        assertTrue(diff.onRightOnly().isEmpty());
    }

    @Test
    public void testFloatListsWithNaN() {
        FloatList list1 = FloatList.of(1.0f, Float.NaN, 3.0f);
        FloatList list2 = FloatList.of(Float.NaN, 3.0f, 4.0f);

        Difference<FloatList, FloatList> diff = Difference.of(list1, list2);

        // NaN != NaN, so they won't be in common
        assertEquals(FloatList.of(Float.NaN, 3.0f), diff.inCommon());
        assertEquals(FloatList.of(1.0f), diff.onLeftOnly());
        assertEquals(FloatList.of(4.0f), diff.onRightOnly());
    }

    @Test
    public void testDoubleListsWithInfinity() {
        DoubleList list1 = DoubleList.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
        DoubleList list2 = DoubleList.of(Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY);

        Difference<DoubleList, DoubleList> diff = Difference.of(list1, list2);

        assertEquals(DoubleList.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), diff.inCommon());
        assertEquals(DoubleList.of(0.0), diff.onLeftOnly());
        assertEquals(DoubleList.of(1.0), diff.onRightOnly());
    }
}
