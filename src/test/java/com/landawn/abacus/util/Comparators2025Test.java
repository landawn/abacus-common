package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;

import lombok.AllArgsConstructor;
import lombok.Data;

@Tag("2025")
public class Comparators2025Test extends TestBase {

    @Data
    @AllArgsConstructor
    public static class Person implements Comparable<Person> {
        String name;
        Integer age;
        Double salary;

        public Person(String name, Integer age) {
            this.name = name;
            this.age = age;
            this.salary = null;
        }

        @Override
        public int compareTo(Person o) {
            if (this.age == null) {
                return o.age == null ? 0 : -1;
            }
            if (o.age == null) {
                return 1;
            }
            return this.age.compareTo(o.age);
        }
    }

    @Test
    public void test_naturalOrder_withNulls() {
        Comparator<Integer> comp = Comparators.naturalOrder();
        List<Integer> list = new ArrayList<>(Arrays.asList(null, null, 3, 1, 2));
        list.sort(comp);
        assertEquals(Arrays.asList(null, null, 1, 2, 3), list);
    }

    @Test
    public void test_naturalOrder_allNulls() {
        Comparator<String> comp = Comparators.naturalOrder();
        List<String> list = new ArrayList<>(Arrays.asList(null, null, null));
        list.sort(comp);
        assertEquals(Arrays.asList(null, null, null), list);
    }

    @Test
    public void test_naturalOrder_emptyList() {
        Comparator<String> comp = Comparators.naturalOrder();
        List<String> list = new ArrayList<>();
        list.sort(comp);
        assertEquals(Arrays.asList(), list);
    }

    @Test
    public void test_nullsFirst_noArgs() {
        Comparator<String> comp = Comparators.nullsFirst();
        assertEquals(-1, comp.compare(null, "a"));
        assertEquals(1, comp.compare("a", null));
        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare("a", "b") < 0);
    }

    @Test
    public void test_nullsFirst_withNullComparator() {
        Comparator<String> comp = Comparators.nullsFirst(null);
        assertNotNull(comp);
        assertEquals(-1, comp.compare(null, "a"));
        assertEquals(1, comp.compare("a", null));
    }

    @Test
    public void test_nullsFirst_withSameComparator() {
        Comparator<String> comp = Comparators.nullsFirst(Comparators.nullsFirst());
        assertNotNull(comp);
    }

    @Test
    public void test_nullsFirstBy_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.nullsFirstBy(null);
        });
    }

    @Test
    public void test_nullsFirstBy_withNullKeys() {
        Function<Person, Integer> extractor = p -> p.age;
        Comparator<Person> comp = Comparators.nullsFirstBy(extractor);

        Person p1 = new Person("Alice", null);
        Person p2 = new Person("Bob", 25);
        Person p3 = new Person("Charlie", null);

        assertTrue(comp.compare(p1, p2) < 0);
        assertTrue(comp.compare(p2, p1) > 0);
        assertEquals(0, comp.compare(p1, p3));
    }

    @Test
    public void test_nullsFirstOrElseEqual() {
        Comparator<String> comp = Comparators.nullsFirstOrElseEqual();
        assertEquals(-1, comp.compare(null, "a"));
        assertEquals(1, comp.compare("a", null));
        assertEquals(0, comp.compare(null, null));
        assertEquals(0, comp.compare("a", "b"));
        assertEquals(0, comp.compare("z", "a"));
    }

    @Test
    public void test_nullsLast_noArgs() {
        Comparator<String> comp = Comparators.nullsLast();
        assertEquals(1, comp.compare(null, "a"));
        assertEquals(-1, comp.compare("a", null));
        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare("a", "b") < 0);
    }

    @Test
    public void test_nullsLast_withNullComparator() {
        Comparator<String> comp = Comparators.nullsLast(null);
        assertNotNull(comp);
        assertEquals(1, comp.compare(null, "a"));
        assertEquals(-1, comp.compare("a", null));
    }

    @Test
    public void test_nullsLast_withSameComparator() {
        Comparator<String> comp = Comparators.nullsLast(Comparators.nullsLast());
        assertNotNull(comp);
    }

    @Test
    public void test_nullsLastBy_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.nullsLastBy(null);
        });
    }

    @Test
    public void test_nullsLastBy_withNullKeys() {
        Function<Person, Integer> extractor = p -> p.age;
        Comparator<Person> comp = Comparators.nullsLastBy(extractor);

        Person p1 = new Person("Alice", null);
        Person p2 = new Person("Bob", 25);
        Person p3 = new Person("Charlie", null);

        assertTrue(comp.compare(p1, p2) > 0);
        assertTrue(comp.compare(p2, p1) < 0);
        assertEquals(0, comp.compare(p1, p3));
    }

    @Test
    public void test_nullsLastOrElseEqual() {
        Comparator<String> comp = Comparators.nullsLastOrElseEqual();
        assertEquals(1, comp.compare(null, "a"));
        assertEquals(-1, comp.compare("a", null));
        assertEquals(0, comp.compare(null, null));
        assertEquals(0, comp.compare("a", "b"));
        assertEquals(0, comp.compare("z", "a"));
    }

    @Test
    public void test_emptiesFirst_noArgs() {
        Comparator<u.Optional<Integer>> comp = Comparators.emptiesFirst();

        u.Optional<Integer> empty1 = u.Optional.empty();
        u.Optional<Integer> empty2 = u.Optional.empty();
        u.Optional<Integer> val1 = u.Optional.of(1);
        u.Optional<Integer> val2 = u.Optional.of(2);

        assertTrue(comp.compare(empty1, val1) < 0);
        assertTrue(comp.compare(val1, empty1) > 0);
        assertEquals(0, comp.compare(empty1, empty2));
        assertTrue(comp.compare(val1, val2) < 0);
    }

    @Test
    public void test_emptiesFirst_withComparator_nullArg() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.emptiesFirst(null);
        });
    }

    @Test
    public void test_emptiesFirst_withNullOptional() {
        Comparator<u.Optional<String>> comp = Comparators.emptiesFirst(Comparator.naturalOrder());

        u.Optional<String> nullOpt = null;
        u.Optional<String> empty = u.Optional.empty();
        u.Optional<String> val = u.Optional.of("test");

        assertEquals(0, comp.compare(nullOpt, empty));
        assertTrue(comp.compare(nullOpt, val) < 0);
        assertTrue(comp.compare(val, nullOpt) > 0);
    }

    @Test
    public void test_emptiesLast_noArgs() {
        Comparator<u.Optional<Integer>> comp = Comparators.emptiesLast();

        u.Optional<Integer> empty1 = u.Optional.empty();
        u.Optional<Integer> empty2 = u.Optional.empty();
        u.Optional<Integer> val1 = u.Optional.of(1);
        u.Optional<Integer> val2 = u.Optional.of(2);

        assertTrue(comp.compare(empty1, val1) > 0);
        assertTrue(comp.compare(val1, empty1) < 0);
        assertEquals(0, comp.compare(empty1, empty2));
        assertTrue(comp.compare(val1, val2) < 0);
    }

    @Test
    public void test_emptiesLast_withComparator_nullArg() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.emptiesLast(null);
        });
    }

    @Test
    public void test_emptiesLast_withNullOptional() {
        Comparator<u.Optional<String>> comp = Comparators.emptiesLast(Comparator.naturalOrder());

        u.Optional<String> nullOpt = null;
        u.Optional<String> empty = u.Optional.empty();
        u.Optional<String> val = u.Optional.of("test");

        assertEquals(0, comp.compare(nullOpt, empty));
        assertTrue(comp.compare(nullOpt, val) > 0);
        assertTrue(comp.compare(val, nullOpt) < 0);
    }

    @Test
    public void test_comparingBy_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBy((Function<String, String>) null);
        });
    }

    @Test
    public void test_comparingBy_withNullValues() {
        Function<Person, String> extractor = p -> p.name;
        Comparator<Person> comp = Comparators.comparingBy(extractor);

        Person p1 = new Person(null, 30);
        Person p2 = new Person("Bob", 25);

        assertTrue(comp.compare(p1, p2) < 0);
        assertTrue(comp.compare(p2, p1) > 0);
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsFirst_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsFirst(null);
        });
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsFirst_nullObjects() {
        Function<Person, Integer> extractor = p -> p.age;
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsFirst(extractor);

        Person p1 = null;
        Person p2 = new Person("Bob", 25);
        Person p3 = null;

        assertTrue(comp.compare(p1, p2) < 0);
        assertTrue(comp.compare(p2, p1) > 0);
        assertEquals(0, comp.compare(p1, p3));
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsLast_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsLast(null);
        });
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsLast_nullObjects() {
        Function<Person, Integer> extractor = p -> p.age;
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsLast(extractor);

        Person p1 = null;
        Person p2 = new Person("Bob", 25);
        Person p3 = null;

        assertTrue(comp.compare(p1, p2) > 0);
        assertTrue(comp.compare(p2, p1) < 0);
        assertEquals(0, comp.compare(p1, p3));
    }

    @Test
    public void test_comparingBy_withComparator_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBy(null, Comparator.naturalOrder());
        });
    }

    @Test
    public void test_comparingBy_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBy(p -> p, null);
        });
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsFirst_withComparator_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsFirst(null, Comparator.naturalOrder());
        });
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsFirst_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsFirst(p -> p, null);
        });
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsLast_withComparator_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsLast(null, Comparator.naturalOrder());
        });
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsLast_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsLast(p -> p, null);
        });
    }

    @Test
    public void test_comparingBoolean_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBoolean(null);
        });
    }

    @Test
    public void test_comparingBoolean_allCombinations() {
        ToBooleanFunction<Person> extractor = p -> p.age != null && p.age > 30;
        Comparator<Person> comp = Comparators.comparingBoolean(extractor);

        Person young = new Person("Young", 20);
        Person old = new Person("Old", 40);

        assertTrue(comp.compare(young, old) < 0);
        assertTrue(comp.compare(old, young) > 0);
        assertEquals(0, comp.compare(young, new Person("Another", 25)));
    }

    @Test
    public void test_comparingChar_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingChar(null);
        });
    }

    @Test
    public void test_comparingChar_sorting() {
        ToCharFunction<String> extractor = s -> s.charAt(0);
        Comparator<String> comp = Comparators.comparingChar(extractor);

        List<String> list = new ArrayList<>(Arrays.asList("zebra", "apple", "banana"));
        list.sort(comp);
        assertEquals(Arrays.asList("apple", "banana", "zebra"), list);
    }

    @Test
    public void test_comparingByte_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByte(null);
        });
    }

    @Test
    public void test_comparingByte_negativeValues() {
        ToByteFunction<Person> extractor = p -> (p.age != null ? p.age.byteValue() : 0);
        Comparator<Person> comp = Comparators.comparingByte(extractor);

        Person p1 = new Person("A", -10);
        Person p2 = new Person("B", 10);

        assertTrue(comp.compare(p1, p2) < 0);
        assertTrue(comp.compare(p2, p1) > 0);
    }

    @Test
    public void test_comparingShort_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingShort(null);
        });
    }

    @Test
    public void test_comparingShort_negativeValues() {
        ToShortFunction<Person> extractor = p -> (p.age != null ? p.age.shortValue() : 0);
        Comparator<Person> comp = Comparators.comparingShort(extractor);

        Person p1 = new Person("A", -100);
        Person p2 = new Person("B", 100);

        assertTrue(comp.compare(p1, p2) < 0);
        assertTrue(comp.compare(p2, p1) > 0);
    }

    @Test
    public void test_comparingInt_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingInt(null);
        });
    }

    @Test
    public void test_comparingLong_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingLong(null);
        });
    }

    @Test
    public void test_comparingFloat_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingFloat(null);
        });
    }

    @Test
    public void test_comparingFloat_withNaN() {
        ToFloatFunction<String> extractor = s -> {
            if ("NaN".equals(s))
                return Float.NaN;
            if ("Inf".equals(s))
                return Float.POSITIVE_INFINITY;
            if ("-Inf".equals(s))
                return Float.NEGATIVE_INFINITY;
            return Float.parseFloat(s);
        };
        Comparator<String> comp = Comparators.comparingFloat(extractor);

        assertTrue(comp.compare("NaN", "1.0") > 0);
        assertTrue(comp.compare("Inf", "1000.0") > 0);
        assertTrue(comp.compare("-Inf", "-1000.0") < 0);
    }

    @Test
    public void test_comparingDouble_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingDouble(null);
        });
    }

    @Test
    public void test_comparingIgnoreCase_withNulls() {
        Comparator<String> comp = Comparators.comparingIgnoreCase();

        assertEquals(-1, comp.compare(null, "a"));
        assertEquals(1, comp.compare("a", null));
        assertEquals(0, comp.compare(null, null));
        assertEquals(0, comp.compare("ABC", "abc"));
    }

    @Test
    public void test_comparingIgnoreCase_withExtractor_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingIgnoreCase(null);
        });
    }

    @Test
    public void test_comparingIgnoreCase_withExtractor_nullValues() {
        Function<Person, String> extractor = p -> p.name;
        Comparator<Person> comp = Comparators.comparingIgnoreCase(extractor);

        Person p1 = new Person(null, 30);
        Person p2 = new Person("Bob", 25);

        assertTrue(comp.compare(p1, p2) < 0);
        assertTrue(comp.compare(p2, p1) > 0);
    }

    @Test
    public void test_comparingByKey_withNulls() {
        Comparator<Entry<String, Integer>> comp = Comparators.comparingByKey();

        Entry<String, Integer> e1 = new SimpleEntry<>(null, 1);
        Entry<String, Integer> e2 = new SimpleEntry<>("a", 2);

        assertTrue(comp.compare(e1, e2) < 0);
        assertTrue(comp.compare(e2, e1) > 0);
    }

    @Test
    public void test_comparingByValue_withNulls() {
        Comparator<Entry<String, Integer>> comp = Comparators.comparingByValue();

        Entry<String, Integer> e1 = new SimpleEntry<>("a", null);
        Entry<String, Integer> e2 = new SimpleEntry<>("b", 2);

        assertTrue(comp.compare(e1, e2) < 0);
        assertTrue(comp.compare(e2, e1) > 0);
    }

    @Test
    public void test_comparingByKey_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByKey(null);
        });
    }

    @Test
    public void test_comparingByValue_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByValue(null);
        });
    }

    @Test
    public void test_comparingByLength_withNulls() {
        Comparator<String> comp = Comparators.comparingByLength();

        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, "a") < 0);
        assertTrue(comp.compare("a", null) > 0);
        assertEquals(0, comp.compare("abc", "xyz"));
    }

    @Test
    public void test_comparingByArrayLength_withNulls() {
        Comparator<int[]> comp = Comparators.comparingByArrayLength();

        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, new int[] { 1 }) < 0);
        assertTrue(comp.compare(new int[] { 1 }, null) > 0);
    }

    @Test
    public void test_comparingByArrayLength_differentTypes() {
        Comparator<Object> comp = Comparators.comparingByArrayLength();

        int[] arr1 = { 1, 2, 3 };
        String[] arr2 = { "a", "b" };

        assertTrue(comp.compare(arr1, arr2) > 0);
        assertTrue(comp.compare(arr2, arr1) < 0);
    }

    @Test
    public void test_comparingBySize_withNulls() {
        Comparator<Collection<String>> comp = Comparators.comparingBySize();

        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, Arrays.asList("a")) < 0);
        assertTrue(comp.compare(Arrays.asList("a"), null) > 0);
    }

    @Test
    public void test_comparingByMapSize_withNulls() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingByMapSize();

        Map<String, Integer> map1 = new HashMap<>();
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("a", 1);

        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, map1) == 0);
        assertTrue(comp.compare(map1, map2) < 0);
    }

    @Test
    public void test_comparingObjArray_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingObjArray(null);
        });
    }

    @Test
    public void test_comparingArray_noArgs_withNulls() {
        Comparator<Integer[]> comp = Comparators.comparingArray();

        Integer[] arr1 = { null, 1, 2 };
        Integer[] arr2 = { 1, 2, 3 };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr2, arr1) > 0);
    }

    @Test
    public void test_comparingArray_noArgs_differentLengths() {
        Comparator<Integer[]> comp = Comparators.comparingArray();

        Integer[] arr1 = { 1, 2 };
        Integer[] arr2 = { 1, 2, 3 };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr2, arr1) > 0);
    }

    @Test
    public void test_comparingArray_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingArray(null);
        });
    }

    @Test
    public void test_comparingCollection_noArgs_withNulls() {
        Comparator<Collection<Integer>> comp = Comparators.comparingCollection();

        List<Integer> list1 = Arrays.asList(null, 1, 2);
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        assertTrue(comp.compare(list1, list2) < 0);
        assertTrue(comp.compare(list2, list1) > 0);
    }

    @Test
    public void test_comparingCollection_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingCollection(null);
        });
    }

    @Test
    public void test_comparingCollection_emptyCollections() {
        Comparator<Collection<String>> comp = Comparators.comparingCollection();

        List<String> empty1 = new ArrayList<>();
        List<String> empty2 = new ArrayList<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void test_comparingIterable_noArgs_differentLengths() {
        Comparator<Iterable<Integer>> comp = Comparators.comparingIterable();

        List<Integer> list1 = Arrays.asList(1, 2);
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        assertTrue(comp.compare(list1, list2) < 0);
        assertTrue(comp.compare(list2, list1) > 0);
    }

    @Test
    public void test_comparingIterable_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingIterable(null);
        });
    }

    @Test
    public void test_comparingIterable_emptyIterables() {
        Comparator<Iterable<String>> comp = Comparators.comparingIterable();

        List<String> empty1 = new ArrayList<>();
        List<String> empty2 = new ArrayList<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void test_comparingIterator_noArgs_differentLengths() {
        Comparator<Iterator<Integer>> comp = Comparators.comparingIterator();

        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        assertTrue(comp.compare(iter1, iter2) < 0);
    }

    @Test
    public void test_comparingIterator_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingIterator(null);
        });
    }

    @Test
    public void test_comparingIterator_emptyIterators() {
        Comparator<Iterator<String>> comp = Comparators.comparingIterator();

        Iterator<String> empty1 = new ArrayList<String>().iterator();
        Iterator<String> empty2 = new ArrayList<String>().iterator();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void test_comparingMapByKey_noArgs_differentSizes() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByKey();

        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("a", 1);
        map2.put("b", 2);

        assertTrue(comp.compare(map1, map2) < 0);
        assertTrue(comp.compare(map2, map1) > 0);
    }

    @Test
    public void test_comparingMapByKey_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingMapByKey(null);
        });
    }

    @Test
    public void test_comparingMapByKey_emptyMaps() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByKey();

        Map<String, Integer> empty1 = new HashMap<>();
        Map<String, Integer> empty2 = new HashMap<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void test_comparingMapByValue_noArgs_differentValues() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByValue();

        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("a", 2);

        assertTrue(comp.compare(map1, map2) < 0);
        assertTrue(comp.compare(map2, map1) > 0);
    }

    @Test
    public void test_comparingMapByValue_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingMapByValue(null);
        });
    }

    @Test
    public void test_comparingMapByValue_emptyMaps() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByValue();

        Map<String, Integer> empty1 = new HashMap<>();
        Map<String, Integer> empty2 = new HashMap<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void test_comparingBeanByProps_nullProps() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBeanByProps(null);
        });
    }

    @Test
    public void test_reverseOrder_noArgs_withNulls() {
        Comparator<Integer> comp = Comparators.reverseOrder();

        List<Integer> list = new ArrayList<>(Arrays.asList(3, null, 1, 4, 2));
        list.sort(comp);
        assertEquals(Arrays.asList(4, 3, 2, 1, null), list);
    }

    @Test
    public void test_reverseOrder_noArgs_allNulls() {
        Comparator<String> comp = Comparators.reverseOrder();

        List<String> list = new ArrayList<>(Arrays.asList(null, null, null));
        list.sort(comp);
        assertEquals(Arrays.asList(null, null, null), list);
    }

    @Test
    public void test_reverseOrder_withNullComparator() {
        Comparator<String> comp = Comparators.reverseOrder(null);
        assertNotNull(comp);
    }

    @Test
    public void test_reverseOrder_doubleReverse() {
        Comparator<Integer> natural = Comparators.naturalOrder();
        Comparator<Integer> reversed = Comparators.reverseOrder(natural);
        Comparator<Integer> doubleReversed = Comparators.reverseOrder(reversed);

        List<Integer> list = new ArrayList<>(Arrays.asList(3, 1, 2));
        list.sort(doubleReversed);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_reversedComparingBoolean_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingBoolean(null);
        });
    }

    @Test
    public void test_reversedComparingChar_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingChar(null);
        });
    }

    @Test
    public void test_reversedComparingByte_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByte(null);
        });
    }

    @Test
    public void test_reversedComparingShort_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingShort(null);
        });
    }

    @Test
    public void test_reversedComparingInt_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingInt(null);
        });
    }

    @Test
    public void test_reversedComparingLong_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingLong(null);
        });
    }

    @Test
    public void test_reversedComparingFloat_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingFloat(null);
        });
    }

    @Test
    public void test_reversedComparingDouble_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingDouble(null);
        });
    }

    @Test
    public void test_reversedComparingBy_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingBy(null);
        });
    }

    @Test
    public void test_reversedComparingByIfNotNullOrElseNullsFirst_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByIfNotNullOrElseNullsFirst(null);
        });
    }

    @Test
    public void test_reversedComparingByIfNotNullOrElseNullsLast_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByIfNotNullOrElseNullsLast(null);
        });
    }

    @Test
    public void test_reversedComparingByKey_withNulls() {
        Comparator<Entry<String, Integer>> comp = Comparators.reversedComparingByKey();

        Entry<String, Integer> e1 = new SimpleEntry<>("a", 1);
        Entry<String, Integer> e2 = new SimpleEntry<>("b", 2);

        assertTrue(comp.compare(e1, e2) > 0);
        assertTrue(comp.compare(e2, e1) < 0);
    }

    @Test
    public void test_reversedComparingByValue_withNulls() {
        Comparator<Entry<String, Integer>> comp = Comparators.reversedComparingByValue();

        Entry<String, Integer> e1 = new SimpleEntry<>("a", 1);
        Entry<String, Integer> e2 = new SimpleEntry<>("b", 2);

        assertTrue(comp.compare(e1, e2) > 0);
        assertTrue(comp.compare(e2, e1) < 0);
    }

    @Test
    public void test_reversedComparingByKey_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByKey(null);
        });
    }

    @Test
    public void test_reversedComparingByValue_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByValue(null);
        });
    }

    @Test
    public void test_BOOLEAN_ARRAY_COMPARATOR() {
        Comparator<boolean[]> comp = Comparators.BOOLEAN_ARRAY_COMPARATOR;

        boolean[] arr1 = { false, false, true };
        boolean[] arr2 = { false, true, false };
        boolean[] arr3 = { false, false };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_CHAR_ARRAY_COMPARATOR() {
        Comparator<char[]> comp = Comparators.CHAR_ARRAY_COMPARATOR;

        char[] arr1 = { 'a', 'b', 'c' };
        char[] arr2 = { 'a', 'b', 'd' };
        char[] arr3 = { 'a', 'b' };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_BYTE_ARRAY_COMPARATOR() {
        Comparator<byte[]> comp = Comparators.BYTE_ARRAY_COMPARATOR;

        byte[] arr1 = { 1, 2, 3 };
        byte[] arr2 = { 1, 2, 4 };
        byte[] arr3 = { 1, 2 };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_SHORT_ARRAY_COMPARATOR() {
        Comparator<short[]> comp = Comparators.SHORT_ARRAY_COMPARATOR;

        short[] arr1 = { 1, 2, 3 };
        short[] arr2 = { 1, 2, 4 };
        short[] arr3 = { 1, 2 };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_INT_ARRAY_COMPARATOR() {
        Comparator<int[]> comp = Comparators.INT_ARRAY_COMPARATOR;

        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 4 };
        int[] arr3 = { 1, 2 };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_LONG_ARRAY_COMPARATOR() {
        Comparator<long[]> comp = Comparators.LONG_ARRAY_COMPARATOR;

        long[] arr1 = { 1L, 2L, 3L };
        long[] arr2 = { 1L, 2L, 4L };
        long[] arr3 = { 1L, 2L };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_FLOAT_ARRAY_COMPARATOR() {
        Comparator<float[]> comp = Comparators.FLOAT_ARRAY_COMPARATOR;

        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.0f, 2.0f, 4.0f };
        float[] arr3 = { 1.0f, 2.0f };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_FLOAT_ARRAY_COMPARATOR_withNaN() {
        Comparator<float[]> comp = Comparators.FLOAT_ARRAY_COMPARATOR;

        float[] arr1 = { 1.0f, Float.NaN };
        float[] arr2 = { 1.0f, 2.0f };

        assertTrue(comp.compare(arr1, arr2) > 0);
    }

    @Test
    public void test_DOUBLE_ARRAY_COMPARATOR() {
        Comparator<double[]> comp = Comparators.DOUBLE_ARRAY_COMPARATOR;

        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.0, 2.0, 4.0 };
        double[] arr3 = { 1.0, 2.0 };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_DOUBLE_ARRAY_COMPARATOR_withNaN() {
        Comparator<double[]> comp = Comparators.DOUBLE_ARRAY_COMPARATOR;

        double[] arr1 = { 1.0, Double.NaN };
        double[] arr2 = { 1.0, 2.0 };

        assertTrue(comp.compare(arr1, arr2) > 0);
    }

    @Test
    public void test_OBJECT_ARRAY_COMPARATOR() {
        Comparator<Object[]> comp = Comparators.OBJECT_ARRAY_COMPARATOR;

        Object[] arr1 = { "a", "b", "c" };
        Object[] arr2 = { "a", "b", "d" };
        Object[] arr3 = { "a", "b" };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr1, arr3) > 0);
        assertEquals(0, comp.compare(arr1, arr1));
    }

    @Test
    public void test_OBJECT_ARRAY_COMPARATOR_withNulls() {
        Comparator<Object[]> comp = Comparators.OBJECT_ARRAY_COMPARATOR;

        Object[] arr1 = { "a", null, "c" };
        Object[] arr2 = { "a", "b", "c" };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr2, arr1) > 0);
    }

    @Test
    public void test_COLLECTION_COMPARATOR() {
        @SuppressWarnings("rawtypes")
        Comparator<Collection> comp = Comparators.COLLECTION_COMPARATOR;

        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");
        List<String> list3 = Arrays.asList("a", "b");

        assertTrue(comp.compare(list1, list2) < 0);
        assertTrue(comp.compare(list1, list3) > 0);
        assertEquals(0, comp.compare(list1, list1));
    }

    @Test
    public void test_COLLECTION_COMPARATOR_withNulls() {
        @SuppressWarnings("rawtypes")
        Comparator<Collection> comp = Comparators.COLLECTION_COMPARATOR;

        List<String> list1 = Arrays.asList("a", null, "c");
        List<String> list2 = Arrays.asList("a", "b", "c");

        assertTrue(comp.compare(list1, list2) < 0);
        assertTrue(comp.compare(list2, list1) > 0);
    }

    @Test
    public void test_COLLECTION_COMPARATOR_emptyCollections() {
        @SuppressWarnings("rawtypes")
        Comparator<Collection> comp = Comparators.COLLECTION_COMPARATOR;

        List<String> empty1 = new ArrayList<>();
        List<String> empty2 = new ArrayList<>();
        List<String> nonEmpty = Arrays.asList("a");

        assertEquals(0, comp.compare(empty1, empty2));
        assertTrue(comp.compare(empty1, nonEmpty) < 0);
        assertTrue(comp.compare(nonEmpty, empty1) > 0);
    }
}
