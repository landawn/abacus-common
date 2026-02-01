package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.Difference.MapDifference;

@Tag("new-test")
public class Difference101Test extends TestBase {

    @Test
    public void testMapDifferenceCollectionsWithDifferentIdExtractors() {
        Map<String, Object> map1a = new HashMap<>();
        map1a.put("id", "A");
        map1a.put("value", 100);
        map1a.put("type", "X");

        Map<String, Object> map1b = new HashMap<>();
        map1b.put("id", "B");
        map1b.put("value", 200);
        map1b.put("type", "Y");

        List<Map<String, Object>> col1 = Arrays.asList(map1a, map1b);

        Map<String, Object> map2a = new HashMap<>();
        map2a.put("code", "A");
        map2a.put("value", 100);
        map2a.put("category", "Z");

        Map<String, Object> map2c = new HashMap<>();
        map2c.put("code", "C");
        map2c.put("value", 300);
        map2c.put("category", "W");

        List<Map<String, Object>> col2 = Arrays.asList(map2a, map2c);

        Function<Map<? extends String, ? extends Object>, String> idExtractor1 = m -> (String) m.get("id");
        Function<Map<? extends String, ? extends Object>, String> idExtractor2 = m -> (String) m.get("code");

        MapDifference<List<Map<String, Object>>, List<Map<String, Object>>, Map<String, MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = MapDifference
                .of(col1, col2, idExtractor1, idExtractor2);

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals("B", diff.onlyOnLeft().get(0).get("id"));

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals("C", diff.onlyOnRight().get(0).get("code"));

        assertEquals(1, diff.differentValues().size());
        assertTrue(diff.differentValues().containsKey("A"));

        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.differentValues().get("A");
        assertEquals(1, innerDiff.common().size());
        assertEquals(100, innerDiff.common().get("value"));
        assertEquals(2, innerDiff.onlyOnLeft().size());
        assertEquals(2, innerDiff.onlyOnRight().size());
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
        map2a.put("age", 26);
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

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals("Bob", diff.onlyOnLeft().get(0).get("name"));

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals("Charlie", diff.onlyOnRight().get(0).get("name"));

        assertEquals(1, diff.differentValues().size());
        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> innerDiff = diff.differentValues().get(1);
        assertEquals(1, innerDiff.common().size());
        assertEquals("Alice", innerDiff.common().get("name"));
        assertEquals(1, innerDiff.differentValues().size());
        assertEquals(Pair.of(25, 26), innerDiff.differentValues().get("age"));
    }

    @Test
    public void testMapDifferenceEmptyCollections() {
        List<Map<String, Integer>> emptyList1 = new ArrayList<>();
        List<Map<String, Integer>> emptyList2 = new ArrayList<>();

        MapDifference<List<Map<String, Integer>>, List<Map<String, Integer>>, Map<Integer, MapDifference<Map<String, Integer>, Map<String, Integer>, Map<String, Pair<Integer, Integer>>>>> diff = MapDifference
                .of(emptyList1, emptyList2, m -> m.get("id"));

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
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

        assertEquals(1, diff1.onlyOnLeft().size());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertFalse(diff1.areEqual());

        MapDifference<List<Map<String, String>>, List<Map<String, String>>, Map<String, MapDifference<Map<String, String>, Map<String, String>, Map<String, Pair<String, String>>>>> diff2 = MapDifference
                .of(emptyList, list1, m -> m.get("id"));

        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(1, diff2.onlyOnRight().size());
        assertFalse(diff2.areEqual());
    }

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

        assertEquals(4, diff.common().size());
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(30, 31), diff.differentValues().get("age"));
    }

    @Test
    public void testBeanDifferenceWithNullNestedObjects() {
        PersonBean person1 = new PersonBean(1L, "John", "Doe", 30);
        person1.setAddress(new Address("123 Main St", "NYC"));

        PersonBean person2 = new PersonBean(1L, "John", "Doe", 30);
        person2.setAddress(null);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2);

        assertEquals(4, diff.common().size());
        assertEquals(1, diff.differentValues().size());
        assertTrue(diff.differentValues().containsKey("address"));
        assertEquals(person1.getAddress(), diff.differentValues().get("address").left());
        assertNull(diff.differentValues().get("address").right());
    }

    @Test
    public void testBeanDifferenceCollectionsWithDifferentIdExtractors() {
        PersonBean person1a = new PersonBean(1L, "Alice", "Smith", 25);
        PersonBean person1b = new PersonBean(2L, "Bob", "Jones", 30);
        List<PersonBean> list1 = Arrays.asList(person1a, person1b);

        PersonBean person2a = new PersonBean(10L, "Alice", "Smith", 26);
        PersonBean person2c = new PersonBean(30L, "Charlie", "Brown", 35);
        List<PersonBean> list2 = Arrays.asList(person2a, person2c);

        Function<PersonBean, String> nameExtractor = p -> p.getFirstName() + " " + p.getLastName();

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(list1, list2, nameExtractor);

        assertTrue(diff.common().isEmpty());
        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals("Bob", diff.onlyOnLeft().get(0).getFirstName());

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals("Charlie", diff.onlyOnRight().get(0).getFirstName());

        assertEquals(1, diff.differentValues().size());
        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> aliceDiff = diff.differentValues().get("Alice Smith");
        assertEquals(2, aliceDiff.common().size());
        assertEquals(2, aliceDiff.differentValues().size());
    }

    @Test
    public void testBeanDifferenceCollectionsWithPropNamesAndDifferentExtractors() {
        PersonBean person1a = new PersonBean(1L, "Alice", "Smith", 25);
        PersonBean person1b = new PersonBean(2L, "Bob", "Jones", 30);
        List<PersonBean> list1 = Arrays.asList(person1a, person1b);

        PersonBean person2a = new PersonBean(100L, "Alice", "SMITH", 25);
        PersonBean person2c = new PersonBean(200L, "Charlie", "Brown", 35);
        List<PersonBean> list2 = Arrays.asList(person2a, person2c);

        Collection<String> propsToCompare = Arrays.asList("firstName", "age");

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<String, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(list1, list2, propsToCompare, PersonBean::getFirstName, PersonBean::getFirstName);

        assertEquals(1, diff.common().size());
        assertEquals("Alice", diff.common().get(0).getFirstName());

        assertEquals(1, diff.onlyOnLeft().size());
        assertEquals("Bob", diff.onlyOnLeft().get(0).getFirstName());

        assertEquals(1, diff.onlyOnRight().size());
        assertEquals("Charlie", diff.onlyOnRight().get(0).getFirstName());

        assertTrue(diff.differentValues().isEmpty());
    }

    @Test
    public void testBeanDifferenceWithBiPredicateValueEquivalence() {
        PersonBean person1 = new PersonBean(1L, "JOHN", "DOE", 30);
        PersonBean person2 = new PersonBean(1L, "john", "doe", 30);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2, (v1, v2) -> {
            if (v1 instanceof String && v2 instanceof String) {
                return ((String) v1).equalsIgnoreCase((String) v2);
            }
            return Objects.equals(v1, v2);
        });

        assertEquals(4, diff.common().size());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceEmptyCollections() {
        List<PersonBean> emptyList1 = new ArrayList<>();
        List<PersonBean> emptyList2 = new ArrayList<>();

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<Long, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff = BeanDifference
                .of(emptyList1, emptyList2, PersonBean::getId);

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.areEqual());
    }

    @Test
    public void testBeanDifferenceCollectionsOneEmpty() {
        PersonBean person = new PersonBean(1L, "Test", "User", 25);
        List<PersonBean> list1 = Arrays.asList(person);
        List<PersonBean> emptyList = new ArrayList<>();

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<Long, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff1 = BeanDifference
                .of(list1, emptyList, PersonBean::getId);

        assertEquals(1, diff1.onlyOnLeft().size());
        assertTrue(diff1.onlyOnRight().isEmpty());
        assertFalse(diff1.areEqual());

        BeanDifference<List<PersonBean>, List<PersonBean>, Map<Long, BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>>>> diff2 = BeanDifference
                .of(emptyList, list1, PersonBean::getId);

        assertTrue(diff2.onlyOnLeft().isEmpty());
        assertEquals(1, diff2.onlyOnRight().size());
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

        assertEquals(3, diff.common().size());
        assertEquals(1, diff.differentValues().size());
        assertEquals(Pair.of(null, "Doe"), diff.differentValues().get("lastName"));
    }

    @Test
    public void testBeanDifferenceBothNullPropertyValues() {
        PersonBean person1 = new PersonBean(1L, "John", null, null);
        PersonBean person2 = new PersonBean(1L, "John", null, null);

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2);

        assertEquals(2, diff.common().size());
        assertTrue(diff.differentValues().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
    }

    @Test
    public void testBeanDifferenceBothNullPropertyValuesWithPropNames() {
        PersonBean person1 = new PersonBean(1L, "John", null, null);
        PersonBean person2 = new PersonBean(1L, "John", null, null);

        Collection<String> propsToCompare = Arrays.asList("lastName", "age");

        BeanDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = BeanDifference.of(person1, person2, propsToCompare);

        assertEquals(2, diff.common().size());
        assertTrue(diff.differentValues().isEmpty());
    }

    @Test
    public void testComplexMapDifferenceScenario() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("string", "value1");
        map1.put("integer", 42);
        map1.put("double", 3.14);
        map1.put("boolean", true);
        map1.put("list", Arrays.asList(1, 2, 3));
        map1.put("null", null);
        map1.put("onlyInFirst", "unique");

        Map<String, Object> map2 = new HashMap<>();
        map2.put("string", "value2");
        map2.put("integer", 42);
        map2.put("double", 3.14159);
        map2.put("boolean", true);
        map2.put("list", Arrays.asList(1, 2, 3));
        map2.put("null", null);
        map2.put("onlyInSecond", "unique");

        MapDifference<Map<String, Object>, Map<String, Object>, Map<String, Pair<Object, Object>>> diff = MapDifference.of(map1, map2);

        assertEquals(4, diff.common().size());
        assertEquals(1, diff.onlyOnLeft().size());
        assertTrue(diff.onlyOnLeft().containsKey("onlyInFirst"));
        assertEquals(1, diff.onlyOnRight().size());
        assertTrue(diff.onlyOnRight().containsKey("onlyInSecond"));
        assertEquals(2, diff.differentValues().size());
        assertEquals(Pair.of("value1", "value2"), diff.differentValues().get("string"));
        assertEquals(Pair.of(3.14, 3.14159), diff.differentValues().get("double"));
    }

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

        assertTrue(diff.common() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnLeft() instanceof LinkedHashMap);
        assertTrue(diff.onlyOnRight() instanceof LinkedHashMap);
        assertTrue(diff.differentValues() instanceof LinkedHashMap);
    }

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

        assertTrue(diff.common().isEmpty());
        assertTrue(diff.onlyOnLeft().isEmpty());
        assertTrue(diff.onlyOnRight().isEmpty());
        assertEquals(3, diff.differentValues().size());
        assertFalse(diff.areEqual());
    }

    @Test
    public void testDifferenceWithLargeCollections() {
        List<Integer> list1 = new ArrayList<>();
        List<Integer> list2 = new ArrayList<>();

        for (int i = 0; i < 10000; i++) {
            list1.add(i);
        }
        for (int i = 5000; i < 15000; i++) {
            list2.add(i);
        }

        Difference<List<Integer>, List<Integer>> diff = Difference.of(list1, list2);

        assertEquals(5000, diff.common().size());
        assertEquals(5000, diff.onlyOnLeft().size());
        assertEquals(5000, diff.onlyOnRight().size());

        assertTrue(diff.common().contains(7500));
        assertTrue(diff.onlyOnLeft().contains(2500));
        assertTrue(diff.onlyOnRight().contains(12500));
    }

    @Test
    public void testDifferenceAllDuplicates() {
        List<String> list1 = Arrays.asList("a", "a", "a", "a");
        List<String> list2 = Arrays.asList("a", "a", "b", "b");

        Difference<List<String>, List<String>> diff = Difference.of(list1, list2);

        assertEquals(Arrays.asList("a", "a"), diff.common());
        assertEquals(Arrays.asList("a", "a"), diff.onlyOnLeft());
        assertEquals(Arrays.asList("b", "b"), diff.onlyOnRight());
    }

    @Test
    public void testPrimitiveListsWithExtremeValues() {
        IntList list1 = IntList.of(Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE);
        IntList list2 = IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE);

        Difference<IntList, IntList> diff = Difference.of(list1, list2);

        assertEquals(IntList.of(Integer.MIN_VALUE, 0, Integer.MAX_VALUE), diff.common());
        assertEquals(IntList.of(-1, 1), diff.onlyOnLeft());
        assertTrue(diff.onlyOnRight().isEmpty());
    }

    @Test
    public void testFloatListsWithNaN() {
        FloatList list1 = FloatList.of(1.0f, Float.NaN, 3.0f);
        FloatList list2 = FloatList.of(Float.NaN, 3.0f, 4.0f);

        Difference<FloatList, FloatList> diff = Difference.of(list1, list2);

        assertEquals(FloatList.of(Float.NaN, 3.0f), diff.common());
        assertEquals(FloatList.of(1.0f), diff.onlyOnLeft());
        assertEquals(FloatList.of(4.0f), diff.onlyOnRight());
    }

    @Test
    public void testDoubleListsWithInfinity() {
        DoubleList list1 = DoubleList.of(Double.NEGATIVE_INFINITY, 0.0, Double.POSITIVE_INFINITY);
        DoubleList list2 = DoubleList.of(Double.NEGATIVE_INFINITY, 1.0, Double.POSITIVE_INFINITY);

        Difference<DoubleList, DoubleList> diff = Difference.of(list1, list2);

        assertEquals(DoubleList.of(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY), diff.common());
        assertEquals(DoubleList.of(0.0), diff.onlyOnLeft());
        assertEquals(DoubleList.of(1.0), diff.onlyOnRight());
    }
}
