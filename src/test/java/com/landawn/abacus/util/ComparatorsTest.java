package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;

import lombok.AllArgsConstructor;
import lombok.Data;

public class ComparatorsTest extends TestBase {

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
    public void testNaturalOrder() {
        Comparator<String> comp = Comparators.naturalOrder();
        List<String> list = new ArrayList<>(Arrays.asList("banana", null, "apple", "cherry"));
        list.sort(comp);
        assertEquals(Arrays.asList(null, "apple", "banana", "cherry"), list);

        List<Integer> intList = new ArrayList<>(Arrays.asList(3, null, 1, 4, 2));
        intList.sort(Comparators.naturalOrder());
        assertEquals(Arrays.asList(null, 1, 2, 3, 4), intList);
    }

    @Test
    public void testNaturalOrder_comparable() {
        Comparator<String> comp = Comparators.naturalOrder();
        assertTrue(comp.compare("apple", "banana") < 0);
        assertTrue(comp.compare("banana", "apple") > 0);
        assertEquals(0, comp.compare("apple", "apple"));
        assertTrue(comp.compare(null, "apple") < 0);
        assertTrue(comp.compare("apple", null) > 0);
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
    public void testNullsFirst() {
        Comparator<Integer> comp = Comparators.nullsFirst();
        List<Integer> list = new ArrayList<>(Arrays.asList(3, null, 1, null, 2));
        list.sort(comp);
        assertEquals(Arrays.asList(null, null, 1, 2, 3), list);

        List<String> list2 = new ArrayList<>(Arrays.asList("b", null, "a"));
        list2.sort(Comparators.nullsFirst());
        assertEquals(Arrays.asList(null, "a", "b"), list2);
    }

    @Test
    public void testNullsFirstWithComparator() {
        Comparator<String> reverseComp = Comparator.reverseOrder();
        Comparator<String> comp = Comparators.nullsFirst(reverseComp);

        List<String> list = new ArrayList<>(Arrays.asList("apple", null, "banana", null, "cherry"));
        list.sort(comp);
        assertEquals(Arrays.asList(null, null, "cherry", "banana", "apple"), list);

        Comparator<Integer> nullComp = Comparators.nullsFirst(null);
        List<Integer> intList = new ArrayList<>(Arrays.asList(3, null, 1, 2));
        intList.sort(nullComp);
        assertEquals(Arrays.asList(null, 1, 2, 3), intList);
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
    public void test_nullsFirstBy_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.nullsFirstBy(null);
        });
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
    public void testNullsFirstOrElseEqual() {
        Comparator<String> comp = Comparators.nullsFirstOrElseEqual();
        List<String> list = Arrays.asList("b", null, "a", null, "c");
        List<String> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        assertNull(sorted.get(0));
        assertNull(sorted.get(1));
        assertEquals("b", sorted.get(2));
        assertEquals("a", sorted.get(3));
        assertEquals("c", sorted.get(4));
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
    public void testNullsLast() {
        Comparator<String> comp = Comparators.nullsLast();
        List<String> list = new ArrayList<>(Arrays.asList("banana", null, "apple", null, "cherry"));
        list.sort(comp);
        assertEquals(Arrays.asList("apple", "banana", "cherry", null, null), list);
    }

    @Test
    public void testNullsLastWithComparator() {
        Comparator<Integer> reverseComp = Comparator.reverseOrder();
        Comparator<Integer> comp = Comparators.nullsLast(reverseComp);

        List<Integer> list = new ArrayList<>(Arrays.asList(3, null, 1, 4, null, 2));
        list.sort(comp);
        assertEquals(Arrays.asList(4, 3, 2, 1, null, null), list);

        Comparator<String> nullComp = Comparators.nullsLast(null);
        List<String> strList = new ArrayList<>(Arrays.asList("b", null, "a"));
        strList.sort(nullComp);
        assertEquals(Arrays.asList("a", "b", null), strList);
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
    public void test_nullsLastBy_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.nullsLastBy(null);
        });
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
    public void testNullsLastOrElseEqual() {
        Comparator<String> comp = Comparators.nullsLastOrElseEqual();
        List<String> list = Arrays.asList("b", null, "a", null, "c");
        List<String> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        assertEquals("b", sorted.get(0));
        assertEquals("a", sorted.get(1));
        assertEquals("c", sorted.get(2));
        assertNull(sorted.get(3));
        assertNull(sorted.get(4));
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
    public void testEmptiesFirst_withComparator_presentValues() {
        Comparator<u.Optional<Integer>> comp = Comparators.emptiesFirst(Comparator.naturalOrder());
        // Both present - compare by value
        assertTrue(comp.compare(u.Optional.of(1), u.Optional.of(2)) < 0);
        assertTrue(comp.compare(u.Optional.of(2), u.Optional.of(1)) > 0);
        assertEquals(0, comp.compare(u.Optional.of(5), u.Optional.of(5)));
        // Empty vs present
        assertTrue(comp.compare(u.Optional.empty(), u.Optional.of(1)) < 0);
        assertTrue(comp.compare(u.Optional.of(1), u.Optional.empty()) > 0);
        assertEquals(0, comp.compare(u.Optional.empty(), u.Optional.empty()));
    }

    @Test
    public void test_emptiesFirst_withComparator_nullArg() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.emptiesFirst(null);
        });
    }

    @Test
    public void testEmptiesFirst() {
        Comparator<u.Optional<String>> comp = Comparators.emptiesFirst();
        List<u.Optional<String>> list = Arrays.asList(u.Optional.of("b"), u.Optional.empty(), u.Optional.of("a"), null);
        List<u.Optional<String>> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        assertTrue(sorted.get(0) == null || sorted.get(0).isEmpty());
        assertTrue(sorted.get(1) == null || sorted.get(1).isEmpty());
        assertEquals("a", sorted.get(2).get());
        assertEquals("b", sorted.get(3).get());

        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        Comparator<u.Optional<String>> comp2 = Comparators.emptiesFirst(lengthComp);
        List<u.Optional<String>> list2 = Arrays.asList(u.Optional.of("long"), u.Optional.empty(), u.Optional.of("a"));
        list2.sort(comp2);
        assertTrue(list2.get(0).isEmpty());
        assertEquals("a", list2.get(1).get());
        assertEquals("long", list2.get(2).get());

        assertThrows(IllegalArgumentException.class, () -> Comparators.emptiesFirst(null));
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
    public void testEmptiesLast_withComparator_presentValues() {
        Comparator<u.Optional<Integer>> comp = Comparators.emptiesLast(Comparator.naturalOrder());
        // Both present - compare by value
        assertTrue(comp.compare(u.Optional.of(1), u.Optional.of(2)) < 0);
        assertTrue(comp.compare(u.Optional.of(2), u.Optional.of(1)) > 0);
        assertEquals(0, comp.compare(u.Optional.of(5), u.Optional.of(5)));
        // Empty vs present
        assertTrue(comp.compare(u.Optional.empty(), u.Optional.of(1)) > 0);
        assertTrue(comp.compare(u.Optional.of(1), u.Optional.empty()) < 0);
        assertEquals(0, comp.compare(u.Optional.empty(), u.Optional.empty()));
    }

    @Test
    public void test_emptiesLast_withComparator_nullArg() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.emptiesLast(null);
        });
    }

    @Test
    public void testEmptiesLast() {
        Comparator<u.Optional<Integer>> comp = Comparators.emptiesLast();
        List<u.Optional<Integer>> list = Arrays.asList(u.Optional.of(2), u.Optional.empty(), u.Optional.of(1), null);
        List<u.Optional<Integer>> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        assertEquals(1, sorted.get(0).get());
        assertEquals(2, sorted.get(1).get());
        assertTrue(sorted.get(2) == null || sorted.get(2).isEmpty());
        assertTrue(sorted.get(3) == null || sorted.get(3).isEmpty());

        Comparator<Integer> reverseComp = Comparator.reverseOrder();
        Comparator<u.Optional<Integer>> comp2 = Comparators.emptiesLast(reverseComp);
        List<u.Optional<Integer>> list2 = Arrays.asList(u.Optional.of(1), u.Optional.empty(), u.Optional.of(2));
        list2.sort(comp2);
        assertEquals(2, list2.get(0).get());
        assertEquals(1, list2.get(1).get());
        assertTrue(list2.get(2).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Comparators.emptiesLast(null));
    }

    @Test
    public void testComparingByWithComparator_sorted() {
        Comparator<String> comp = Comparators.comparingBy(String::length, Comparator.naturalOrder());
        List<String> list = Arrays.asList("banana", "kiwi", "fig");
        list.sort(comp);
        assertEquals("fig", list.get(0));
        assertEquals("kiwi", list.get(1));
        assertEquals("banana", list.get(2));
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
    public void test_comparingBy_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBy((Function<String, String>) null);
        });
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
    public void testComparingBy() {
        Function<Person, Integer> ageExtractor = p -> p.age;
        Comparator<Person> comp = Comparators.comparingBy(ageExtractor);

        List<Person> list = Arrays.asList(new Person("Alice", 30), new Person("Bob", 25), new Person("Charlie", 35));
        list.sort(comp);

        assertEquals(25, list.get(0).age);
        assertEquals(30, list.get(1).age);
        assertEquals(35, list.get(2).age);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBy(null));
    }

    @Test
    public void testComparingByWithComparator() {
        Function<Person, String> nameExtractor = p -> p.name;
        Comparator<String> ignoreCaseComp = String.CASE_INSENSITIVE_ORDER;
        Comparator<Person> comp = Comparators.comparingBy(nameExtractor, ignoreCaseComp);

        List<Person> list = Arrays.asList(new Person("alice", 30), new Person("BOB", 25), new Person("Charlie", 35));
        list.sort(comp);

        assertEquals("alice", list.get(0).name);
        assertEquals("BOB", list.get(1).name);
        assertEquals("Charlie", list.get(2).name);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBy(nameExtractor, null));
        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBy(null, ignoreCaseComp));
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
    public void testComparingByIfNotNullOrElseNullsFirstWithComparator() {
        Function<Person, String> nameExtractor = p -> p.name;
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsFirst(nameExtractor, lengthComp);

        List<Person> list = new ArrayList<>(Arrays.asList(new Person("Alice", 30), null, new Person("Bob", 25), new Person("Charlie", 35)));
        list.sort(comp);

        assertNull(list.get(0));
        assertEquals("Bob", list.get(1).name);
        assertEquals("Alice", list.get(2).name);
        assertEquals("Charlie", list.get(3).name);
    }

    // --- Additional gap-filling tests ---

    @Test
    public void testComparingByIfNotNullOrElseNullsFirst_sorting() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("Charlie", 25), null, new Person("Alice", 30), new Person("Bob", 20)));
        people.sort(Comparators.comparingByIfNotNullOrElseNullsFirst(Person::getName));
        assertNull(people.get(0));
        assertEquals("Alice", people.get(1).getName());
        assertEquals("Bob", people.get(2).getName());
        assertEquals("Charlie", people.get(3).getName());
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsFirst_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsFirst(null);
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
    public void testComparingByIfNotNullOrElseNullsLastWithComparator() {
        Function<Person, String> nameExtractor = p -> p.name;
        Comparator<String> reverseComp = Comparator.reverseOrder();
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsLast(nameExtractor, reverseComp);

        List<Person> list = new ArrayList<>(Arrays.asList(new Person("Alice", 30), null, new Person("Bob", 25), new Person("Charlie", 35)));
        list.sort(comp);

        assertEquals("Charlie", list.get(0).name);
        assertEquals("Bob", list.get(1).name);
        assertEquals("Alice", list.get(2).name);
        assertNull(list.get(3));
    }

    @Test
    public void testComparingByIfNotNullOrElseNullsLast_sorting() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("Charlie", 25), null, new Person("Alice", 30), new Person("Bob", 20)));
        people.sort(Comparators.comparingByIfNotNullOrElseNullsLast(Person::getName));
        assertEquals("Alice", people.get(0).getName());
        assertEquals("Bob", people.get(1).getName());
        assertEquals("Charlie", people.get(2).getName());
        assertNull(people.get(3));
    }

    @Test
    public void test_comparingByIfNotNullOrElseNullsLast_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByIfNotNullOrElseNullsLast(null);
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
    public void test_comparingBoolean_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBoolean(null);
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
    public void test_comparingChar_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingChar(null);
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
    public void test_comparingByte_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByte(null);
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
    public void test_comparingShort_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingShort(null);
        });
    }

    @Test
    public void test_comparingInt_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingInt(null);
        });
    }

    @Test
    public void testComparingInt() {
        ToIntFunction<Person> ageExtractor = p -> p.age;
        Comparator<Person> comp = Comparators.comparingInt(ageExtractor);

        List<Person> list = Arrays.asList(new Person("Alice", 30), new Person("Bob", 25), new Person("Charlie", 35));
        list.sort(comp);

        assertEquals(25, list.get(0).age);
        assertEquals(30, list.get(1).age);
        assertEquals(35, list.get(2).age);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingInt(null));
    }

    @Test
    public void testComparingLong_sorting() {
        List<Person> people = Arrays.asList(new Person("Alice", 30), new Person("Bob", 20), new Person("Charlie", 25));
        people.sort(Comparators.comparingLong(p -> (long) p.getAge()));
        assertEquals("Bob", people.get(0).getName());
        assertEquals("Charlie", people.get(1).getName());
        assertEquals("Alice", people.get(2).getName());
    }

    @Test
    public void test_comparingLong_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingLong(null);
        });
    }

    @Test
    public void testComparingFloat_sorting() {
        List<Person> people = Arrays.asList(new Person("Alice", 30, 50000.0), new Person("Bob", 20, 30000.0), new Person("Charlie", 25, 40000.0));
        people.sort(Comparators.comparingFloat(p -> p.getSalary().floatValue()));
        assertEquals("Bob", people.get(0).getName());
        assertEquals("Charlie", people.get(1).getName());
        assertEquals("Alice", people.get(2).getName());
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
    public void test_comparingFloat_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingFloat(null);
        });
    }

    @Test
    public void testComparingDouble_sorting() {
        List<Person> people = Arrays.asList(new Person("Alice", 30, 50000.0), new Person("Bob", 20, 30000.0), new Person("Charlie", 25, 40000.0));
        people.sort(Comparators.comparingDouble(Person::getSalary));
        assertEquals("Bob", people.get(0).getName());
        assertEquals("Charlie", people.get(1).getName());
        assertEquals("Alice", people.get(2).getName());
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
    public void test_comparingIgnoreCase_withExtractor_nullValues() {
        Function<Person, String> extractor = p -> p.name;
        Comparator<Person> comp = Comparators.comparingIgnoreCase(extractor);

        Person p1 = new Person(null, 30);
        Person p2 = new Person("Bob", 25);

        assertTrue(comp.compare(p1, p2) < 0);
        assertTrue(comp.compare(p2, p1) > 0);
    }

    @Test
    public void testComparingIgnoreCase() {
        Comparator<String> comp = Comparators.comparingIgnoreCase();
        List<String> list = Arrays.asList("john", "Alice", null, "BOB", "charlie");
        list.sort(comp);

        assertNull(list.get(0));
        assertEquals("Alice", list.get(1));
        assertEquals("BOB", list.get(2));
        assertEquals("charlie", list.get(3));
        assertEquals("john", list.get(4));
    }

    @Test
    public void test_comparingIgnoreCase_withExtractor_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingIgnoreCase(null);
        });
    }

    @Test
    public void testComparingIgnoreCaseWithExtractor() {
        Function<Person, String> nameExtractor = p -> p.name;
        Comparator<Person> comp = Comparators.comparingIgnoreCase(nameExtractor);

        List<Person> list = Arrays.asList(new Person("john", 30), new Person("Alice", 25), new Person("BOB", 35), new Person("charlie", 28));
        list.sort(comp);

        assertEquals("Alice", list.get(0).name);
        assertEquals("BOB", list.get(1).name);
        assertEquals("charlie", list.get(2).name);
        assertEquals("john", list.get(3).name);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingIgnoreCase(null));
    }

    @Test
    public void testComparingByKey() {
        Comparator<Map.Entry<String, Integer>> comp = Comparators.comparingByKey();
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals("apple", entries.get(0).getKey());
        assertEquals("banana", entries.get(1).getKey());
        assertEquals("cherry", entries.get(2).getKey());
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
    public void test_comparingByKey_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByKey(null);
        });
    }

    @Test
    public void testComparingByKeyWithComparator() {
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        Comparator<Map.Entry<String, Integer>> comp = Comparators.comparingByKey(lengthComp);

        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("a", 4),
                new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals("a", entries.get(0).getKey());
        assertEquals("apple", entries.get(1).getKey());
        assertTrue(entries.get(2).getKey().equals("banana") || entries.get(2).getKey().equals("cherry"));

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingByKey(null));
    }

    @Test
    public void testComparingByValue() {
        Comparator<Map.Entry<String, Integer>> comp = Comparators.comparingByValue();
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals(1, entries.get(0).getValue());
        assertEquals(2, entries.get(1).getValue());
        assertEquals(3, entries.get(2).getValue());
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
    public void test_comparingByValue_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingByValue(null);
        });
    }

    @Test
    public void testComparingByValueWithComparator() {
        Comparator<Integer> reverseComp = Comparator.reverseOrder();
        Comparator<Map.Entry<String, Integer>> comp = Comparators.comparingByValue(reverseComp);

        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals(3, entries.get(0).getValue());
        assertEquals(2, entries.get(1).getValue());
        assertEquals(1, entries.get(2).getValue());

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingByValue(null));
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
    public void testComparingByLength() {
        Comparator<CharSequence> comp = Comparators.comparingByLength();
        List<String> list = Arrays.asList("short", "a", "medium", "very long string", null);
        list.sort(comp);

        assertNull(list.get(0));
        assertEquals("a", list.get(1));
        assertEquals("short", list.get(2));
        assertEquals("medium", list.get(3));
        assertEquals("very long string", list.get(4));

        List<CharSequence> mixed = Arrays.asList(new StringBuilder("test"), "hi", new StringBuffer("longer"));
        mixed.sort(comp);
        assertEquals("hi", mixed.get(0).toString());
        assertEquals("test", mixed.get(1).toString());
        assertEquals("longer", mixed.get(2).toString());
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
    public void test_comparingByArrayLength_withNulls() {
        Comparator<int[]> comp = Comparators.comparingByArrayLength();

        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, new int[] { 1 }) < 0);
        assertTrue(comp.compare(new int[] { 1 }, null) > 0);
    }

    @Test
    public void testComparingByArrayLength() {
        Comparator<Object> comp = Comparators.comparingByArrayLength();

        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1 };
        int[] arr3 = { 1, 2, 3, 4, 5 };
        String[] arr4 = { "a", "b" };

        List<Object> arrays = Arrays.asList(arr3, arr1, null, arr2, arr4);
        arrays.sort(comp);

        assertNull(arrays.get(0));
        assertSame(arr2, arrays.get(1));
        assertSame(arr4, arrays.get(2));
        assertSame(arr1, arrays.get(3));
        assertSame(arr3, arrays.get(4));
    }

    @Test
    public void test_comparingBySize_withNulls() {
        Comparator<Collection<String>> comp = Comparators.comparingBySize();

        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, Arrays.asList("a")) < 0);
        assertTrue(comp.compare(Arrays.asList("a"), null) > 0);
    }

    @Test
    public void testComparingBySize() {
        Comparator<Collection<String>> comp = Comparators.comparingBySize();

        List<Collection<String>> collections = Arrays.asList(Arrays.asList("a", "b", "c"), Arrays.asList("x"), null, Arrays.asList("m", "n"),
                new HashSet<>(Arrays.asList("p", "q", "r", "s")));
        collections.sort(comp);

        assertNull(collections.get(0));
        assertEquals(1, collections.get(1).size());
        assertEquals(2, collections.get(2).size());
        assertEquals(3, collections.get(3).size());
        assertEquals(4, collections.get(4).size());
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
    public void testComparingByMapSize() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingByMapSize();

        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);
        map1.put("c", 3);

        Map<String, Integer> map2 = Collections.singletonMap("x", 1);

        Map<String, Integer> map3 = new HashMap<>();
        map3.put("m", 1);
        map3.put("n", 2);

        List<Map<String, Integer>> maps = Arrays.asList(map1, null, map2, map3);
        maps.sort(comp);

        assertNull(maps.get(0));
        assertEquals(1, maps.get(1).size());
        assertEquals(2, maps.get(2).size());
        assertEquals(3, maps.get(3).size());
    }

    @Test
    public void test_comparingObjArray_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingObjArray(null);
        });
    }

    @Test
    public void testComparingObjArray() {
        Comparator<Object[]> comp = Comparators.comparingObjArray(String.CASE_INSENSITIVE_ORDER);

        Object[] arr1 = { "apple", "banana" };
        Object[] arr2 = { "APPLE", "CHERRY" };
        Object[] arr3 = { "apple", "BANANA", "cherry" };

        List<Object[]> arrays = Arrays.asList(arr2, arr1, arr3);
        arrays.sort(comp);

        assertSame(arr1, arrays.get(0));
        assertSame(arr3, arrays.get(1));
        assertSame(arr2, arrays.get(2));

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingObjArray(null));
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
    public void test_comparingArray_noArgs_withNulls() {
        Comparator<Integer[]> comp = Comparators.comparingArray();

        Integer[] arr1 = { null, 1, 2 };
        Integer[] arr2 = { 1, 2, 3 };

        assertTrue(comp.compare(arr1, arr2) < 0);
        assertTrue(comp.compare(arr2, arr1) > 0);
    }

    @Test
    public void testComparatorFactoryOverloads_SizeFallbackAfterEqualPrefix() {
        Comparator<String[]> arrayComparator = Comparators.comparingArray(String.CASE_INSENSITIVE_ORDER);
        assertTrue(arrayComparator.compare(new String[] { "a", "B" }, new String[] { "A", "b", "c" }) < 0);

        Comparator<List<String>> collectionComparator = Comparators.comparingCollection(String.CASE_INSENSITIVE_ORDER);
        assertTrue(collectionComparator.compare(Arrays.asList("a", "B"), Arrays.asList("A", "b", "c")) < 0);

        Comparator<Iterable<String>> iterableComparator = Comparators.comparingIterable(String.CASE_INSENSITIVE_ORDER);
        assertTrue(iterableComparator.compare(Arrays.asList("a", "B"), Arrays.asList("A", "b", "c")) < 0);

        Map<String, Integer> shorterByKey = new LinkedHashMap<>();
        shorterByKey.put("a", 1);
        shorterByKey.put("b", 2);
        Map<String, Integer> longerByKey = new LinkedHashMap<>();
        longerByKey.put("A", 1);
        longerByKey.put("B", 2);
        longerByKey.put("c", 3);
        Comparator<Map<String, Integer>> mapByKeyComparator = Comparators.comparingMapByKey(String.CASE_INSENSITIVE_ORDER);
        assertTrue(mapByKeyComparator.compare(shorterByKey, longerByKey) < 0);

        Map<Integer, String> shorterByValue = new LinkedHashMap<>();
        shorterByValue.put(1, "a");
        shorterByValue.put(2, "b");
        Map<Integer, String> longerByValue = new LinkedHashMap<>();
        longerByValue.put(1, "A");
        longerByValue.put(2, "B");
        longerByValue.put(3, "c");
        Comparator<Map<Integer, String>> mapByValueComparator = Comparators.comparingMapByValue(String.CASE_INSENSITIVE_ORDER);
        assertTrue(mapByValueComparator.compare(shorterByValue, longerByValue) < 0);
    }

    @Test
    public void test_comparingArray_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingArray(null);
        });
    }

    @Test
    public void testComparingArray() {
        Comparator<String[]> comp1 = Comparators.comparingArray();
        String[] arr1 = { "apple", "banana" };
        String[] arr2 = { "apple", "cherry" };
        assertTrue(comp1.compare(arr1, arr2) < 0);

        Comparator<Integer[]> comp2 = Comparators.comparingArray(Integer::compare);
        Integer[] intArr1 = { 1, 2, 3 };
        Integer[] intArr2 = { 1, 2, 3, 4 };
        assertTrue(comp2.compare(intArr1, intArr2) < 0);

        assertTrue(comp2.compare(null, intArr1) < 0);
        assertTrue(comp2.compare(intArr1, null) > 0);
        assertEquals(0, comp2.compare(null, null));

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingArray(null));
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
    public void test_comparingCollection_emptyCollections() {
        Comparator<Collection<String>> comp = Comparators.comparingCollection();

        List<String> empty1 = new ArrayList<>();
        List<String> empty2 = new ArrayList<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void test_comparingCollection_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingCollection(null);
        });
    }

    @Test
    public void testComparingCollection() {
        Comparator<List<Integer>> comp1 = Comparators.comparingCollection();
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 4);
        assertTrue(comp1.compare(list1, list2) < 0);

        Comparator<List<String>> comp2 = Comparators.comparingCollection(String.CASE_INSENSITIVE_ORDER);
        List<String> strList1 = Arrays.asList("apple", "BANANA");
        List<String> strList2 = Arrays.asList("APPLE", "banana", "cherry");
        assertTrue(comp2.compare(strList1, strList2) < 0);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingCollection(null));
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
    public void test_comparingIterable_emptyIterables() {
        Comparator<Iterable<String>> comp = Comparators.comparingIterable();

        List<String> empty1 = new ArrayList<>();
        List<String> empty2 = new ArrayList<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void testComparingIterable_withComparator_equalPrefix() {
        Comparator<Iterable<String>> comp = Comparators.comparingIterable(Comparator.naturalOrder());
        // Equal contents - should return 0
        assertEquals(0, comp.compare(Arrays.asList("a", "b"), Arrays.asList("a", "b")));
        // a has more elements
        assertTrue(comp.compare(Arrays.asList("a", "b", "c"), Arrays.asList("a", "b")) > 0);
        // b has more elements
        assertTrue(comp.compare(Arrays.asList("a", "b"), Arrays.asList("a", "b", "c")) < 0);
        // null iterables
        assertEquals(0, comp.compare(null, null));
        assertTrue(comp.compare(null, Arrays.asList("a")) < 0);
    }

    @Test
    public void test_comparingIterable_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingIterable(null);
        });
    }

    @Test
    public void testComparingIterable() {
        Comparator<Iterable<String>> comp1 = Comparators.comparingIterable();
        Iterable<String> iter1 = Arrays.asList("apple", "banana");
        Iterable<String> iter2 = Arrays.asList("apple", "cherry");
        assertTrue(comp1.compare(iter1, iter2) < 0);

        Comparator<Iterable<Integer>> comp2 = Comparators.comparingIterable(Integer::compare);
        Iterable<Integer> intIter1 = Arrays.asList(1, 2, 3);
        Iterable<Integer> intIter2 = Arrays.asList(1, 2, 3, 4);
        assertTrue(comp2.compare(intIter1, intIter2) < 0);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingIterable(null));
    }

    @Test
    public void test_comparingIterator_noArgs_differentLengths() {
        Comparator<Iterator<Integer>> comp = Comparators.comparingIterator();

        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        assertTrue(comp.compare(iter1, iter2) < 0);
    }

    @Test
    public void test_comparingIterator_emptyIterators() {
        Comparator<Iterator<String>> comp = Comparators.comparingIterator();

        Iterator<String> empty1 = new ArrayList<String>().iterator();
        Iterator<String> empty2 = new ArrayList<String>().iterator();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void testComparingIterator_withComparator_equalPrefix() {
        Comparator<Iterator<String>> comp = Comparators.comparingIterator(Comparator.naturalOrder());
        // Equal - same elements
        assertEquals(0, comp.compare(Arrays.asList("a", "b").iterator(), Arrays.asList("a", "b").iterator()));
        // First shorter
        assertTrue(comp.compare(Arrays.asList("a").iterator(), Arrays.asList("a", "b").iterator()) < 0);
        // First longer
        assertTrue(comp.compare(Arrays.asList("a", "b").iterator(), Arrays.asList("a").iterator()) > 0);
    }

    @Test
    public void test_comparingIterator_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingIterator(null);
        });
    }

    @Test
    public void testComparingIterator() {
        Comparator<Iterator<Integer>> comp1 = Comparators.comparingIterator();
        Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 4).iterator();
        assertTrue(comp1.compare(iter1, iter2) < 0);

        Comparator<Iterator<String>> comp2 = Comparators.comparingIterator(String::compareToIgnoreCase);
        Iterator<String> strIter1 = Arrays.asList("apple", "BANANA").iterator();
        Iterator<String> strIter2 = Arrays.asList("APPLE", "banana", "cherry").iterator();
        assertEquals(-1, comp2.compare(strIter1, strIter2));

        assertFalse(strIter1.hasNext());
        assertTrue(strIter2.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingIterator(null));
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
    public void test_comparingMapByKey_emptyMaps() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByKey();

        Map<String, Integer> empty1 = new HashMap<>();
        Map<String, Integer> empty2 = new HashMap<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void testComparingMapByKey_withComparator_equalElements() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByKey(Comparator.naturalOrder());
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("a", 3);
        m2.put("b", 4);
        assertEquals(0, comp.compare(m1, m2)); // same keys in order
    }

    @Test
    public void test_comparingMapByKey_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingMapByKey(null);
        });
    }

    @Test
    public void testComparingMapByKey() {
        Comparator<Map<String, Integer>> comp1 = Comparators.comparingMapByKey();

        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("apple", 1);
        map1.put("banana", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("apple", 1);
        map2.put("cherry", 3);

        assertTrue(comp1.compare(map1, map2) < 0);

        Comparator<Map<String, Integer>> comp2 = Comparators.comparingMapByKey(String.CASE_INSENSITIVE_ORDER);

        Map<String, Integer> map3 = new LinkedHashMap<>();
        map3.put("APPLE", 1);
        map3.put("BANANA", 2);

        assertTrue(comp2.compare(map1, map3) == 0);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingMapByKey(null));
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
    public void test_comparingMapByValue_emptyMaps() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByValue();

        Map<String, Integer> empty1 = new HashMap<>();
        Map<String, Integer> empty2 = new HashMap<>();

        assertEquals(0, comp.compare(empty1, empty2));
    }

    @Test
    public void testComparingMapByValue_withComparator_equalElements() {
        Comparator<Map<String, Integer>> comp = Comparators.comparingMapByValue(Comparator.naturalOrder());
        Map<String, Integer> m1 = new LinkedHashMap<>();
        m1.put("a", 1);
        m1.put("b", 2);
        Map<String, Integer> m2 = new LinkedHashMap<>();
        m2.put("x", 1);
        m2.put("y", 2);
        assertEquals(0, comp.compare(m1, m2)); // same values in order
    }

    @Test
    public void test_comparingMapByValue_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingMapByValue(null);
        });
    }

    @Test
    public void testComparingMapByValue() {
        Comparator<Map<String, Integer>> comp1 = Comparators.comparingMapByValue();

        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("x", 1);
        map2.put("y", 3);

        assertTrue(comp1.compare(map1, map2) < 0);

        Comparator<Map<String, Integer>> comp2 = Comparators.comparingMapByValue(Comparator.reverseOrder());

        assertTrue(comp2.compare(map1, map2) > 0);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingMapByValue(null));
    }

    @Test
    public void test_comparingBeanByProps_nullProps() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.comparingBeanByProps(null);
        });
    }

    @Test
    public void testComparingBeanByProps() {
        List<String> props = Arrays.asList("age", "name", "non-existed-prop");
        Comparator<Person> comp = Comparators.comparingBeanByProps(props);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBeanByProps(null));
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
    public void testReverseOrder() {
        Comparator<Integer> comp = Comparators.reverseOrder();
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 9);
        list.sort(comp);
        assertEquals(Arrays.asList(9, 5, 4, 3, 1, 1), list);

        List<String> strList = Arrays.asList("apple", "zebra", "banana");
        strList.sort(Comparators.reverseOrder());
        assertEquals(Arrays.asList("zebra", "banana", "apple"), strList);
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
    public void testReverseOrderWithComparator() {
        Comparator<String> comp1 = Comparators.reverseOrder(null);
        List<String> list1 = Arrays.asList("b", "a", "c");
        list1.sort(comp1);
        assertEquals(Arrays.asList("c", "b", "a"), list1);

        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        Comparator<String> reversed = Comparators.reverseOrder(lengthComp);
        List<String> list2 = Arrays.asList("short", "a", "medium");
        list2.sort(reversed);
        assertEquals(Arrays.asList("medium", "short", "a"), list2);

        Comparator<Integer> natural = Comparators.naturalOrder();
        Comparator<Integer> reversed1 = Comparators.reverseOrder(natural);
        Comparator<Integer> reversed2 = Comparators.reverseOrder(reversed1);
        List<Integer> nums = Arrays.asList(3, 1, 2);
        nums.sort(reversed2);
        assertEquals(Arrays.asList(1, 2, 3), nums);
    }

    @Test
    public void testReversedComparingBoolean_sorting() {
        List<String> items = Arrays.asList("short", "loooong", "mid");
        items.sort(Comparators.reversedComparingBoolean(s -> s.length() > 4));
        assertEquals(Arrays.asList("short", "loooong", "mid"), items);
    }

    @Test
    public void test_reversedComparingBoolean_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingBoolean(null);
        });
    }

    @Test
    public void testReversedComparingChar_sorting() {
        List<String> items = Arrays.asList("apple", "cherry", "banana");
        items.sort(Comparators.reversedComparingChar(s -> s.charAt(0)));
        assertEquals("cherry", items.get(0));
        assertEquals("banana", items.get(1));
        assertEquals("apple", items.get(2));
    }

    @Test
    public void test_reversedComparingChar_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingChar(null);
        });
    }

    @Test
    public void testReversedComparingByte_sorting() {
        List<Integer> items = Arrays.asList(1, 3, 2);
        items.sort(Comparators.reversedComparingByte(Integer::byteValue));
        assertEquals(Integer.valueOf(3), items.get(0));
        assertEquals(Integer.valueOf(2), items.get(1));
        assertEquals(Integer.valueOf(1), items.get(2));
    }

    @Test
    public void test_reversedComparingByte_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByte(null);
        });
    }

    @Test
    public void testReversedComparingShort_sorting() {
        List<Integer> items = Arrays.asList(100, 300, 200);
        items.sort(Comparators.reversedComparingShort(Integer::shortValue));
        assertEquals(Integer.valueOf(300), items.get(0));
        assertEquals(Integer.valueOf(200), items.get(1));
        assertEquals(Integer.valueOf(100), items.get(2));
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
    public void testReversedComparingInt() {
        ToIntFunction<Person> ageExtractor = p -> p.age;
        Comparator<Person> comp = Comparators.reversedComparingInt(ageExtractor);

        List<Person> list = Arrays.asList(new Person("Alice", 30), new Person("Bob", 25), new Person("Charlie", 35));
        list.sort(comp);

        assertEquals(35, list.get(0).age);
        assertEquals(30, list.get(1).age);
        assertEquals(25, list.get(2).age);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingInt(null));
    }

    @Test
    public void testReversedComparingLong_sorting() {
        List<Person> people = Arrays.asList(new Person("Alice", 30), new Person("Bob", 20), new Person("Charlie", 25));
        people.sort(Comparators.reversedComparingLong(p -> (long) p.getAge()));
        assertEquals("Alice", people.get(0).getName());
        assertEquals("Charlie", people.get(1).getName());
        assertEquals("Bob", people.get(2).getName());
    }

    @Test
    public void test_reversedComparingLong_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingLong(null);
        });
    }

    @Test
    public void testReversedComparingFloat_sorting() {
        List<Person> people = Arrays.asList(new Person("Alice", 30, 50000.0), new Person("Bob", 20, 30000.0), new Person("Charlie", 25, 40000.0));
        people.sort(Comparators.reversedComparingFloat(p -> p.getSalary().floatValue()));
        assertEquals("Alice", people.get(0).getName());
        assertEquals("Charlie", people.get(1).getName());
        assertEquals("Bob", people.get(2).getName());
    }

    @Test
    public void test_reversedComparingFloat_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingFloat(null);
        });
    }

    @Test
    public void testReversedComparingDouble_sorting() {
        List<Person> people = Arrays.asList(new Person("Alice", 30, 50000.0), new Person("Bob", 20, 30000.0), new Person("Charlie", 25, 40000.0));
        people.sort(Comparators.reversedComparingDouble(Person::getSalary));
        assertEquals("Alice", people.get(0).getName());
        assertEquals("Charlie", people.get(1).getName());
        assertEquals("Bob", people.get(2).getName());
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
    public void testReversedComparingBy() {
        Function<Person, String> nameExtractor = p -> p.name;
        Comparator<Person> comp = Comparators.reversedComparingBy(nameExtractor);

        List<Person> list = Arrays.asList(new Person("Bob", 25), new Person("Alice", 30), new Person("Charlie", 35));
        list.sort(comp);

        assertEquals("Charlie", list.get(0).name);
        assertEquals("Bob", list.get(1).name);
        assertEquals("Alice", list.get(2).name);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingBy(null));
    }

    @Test
    public void testReversedComparingByIfNotNullOrElseNullsFirst_sorting() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("Alice", 30), null, new Person("Charlie", 25), new Person("Bob", 20)));
        people.sort(Comparators.reversedComparingByIfNotNullOrElseNullsFirst(Person::getName));
        assertNull(people.get(0));
        assertEquals("Charlie", people.get(1).getName());
        assertEquals("Bob", people.get(2).getName());
        assertEquals("Alice", people.get(3).getName());
    }

    @Test
    public void test_reversedComparingByIfNotNullOrElseNullsFirst_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByIfNotNullOrElseNullsFirst(null);
        });
    }

    @Test
    public void testReversedComparingByIfNotNullOrElseNullsLast_sorting() {
        List<Person> people = new ArrayList<>(Arrays.asList(new Person("Alice", 30), null, new Person("Charlie", 25), new Person("Bob", 20)));
        people.sort(Comparators.reversedComparingByIfNotNullOrElseNullsLast(Person::getName));
        assertEquals("Charlie", people.get(0).getName());
        assertEquals("Bob", people.get(1).getName());
        assertEquals("Alice", people.get(2).getName());
        assertNull(people.get(3));
    }

    @Test
    public void test_reversedComparingByIfNotNullOrElseNullsLast_nullExtractor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByIfNotNullOrElseNullsLast(null);
        });
    }

    @Test
    public void testReversedComparingByKey() {
        Comparator<Map.Entry<String, Integer>> comp = Comparators.reversedComparingByKey();
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals("cherry", entries.get(0).getKey());
        assertEquals("banana", entries.get(1).getKey());
        assertEquals("apple", entries.get(2).getKey());
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
    public void test_reversedComparingByKey_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByKey(null);
        });
    }

    @Test
    public void testReversedComparingByKeyWithComparator() {
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        Comparator<Map.Entry<String, Integer>> comp = Comparators.reversedComparingByKey(lengthComp);

        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("a", 1), new SimpleEntry<>("banana", 2), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertTrue(entries.get(0).getKey().equals("banana") || entries.get(0).getKey().equals("cherry"));
        assertTrue(entries.get(1).getKey().equals("banana") || entries.get(1).getKey().equals("cherry"));
        assertEquals("a", entries.get(2).getKey());

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingByKey(null));
    }

    @Test
    public void testReversedComparingByValue() {
        Comparator<Map.Entry<String, Integer>> comp = Comparators.reversedComparingByValue();
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals(3, entries.get(0).getValue());
        assertEquals(2, entries.get(1).getValue());
        assertEquals(1, entries.get(2).getValue());
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
    public void test_reversedComparingByValue_withComparator_nullComparator() {
        assertThrows(IllegalArgumentException.class, () -> {
            Comparators.reversedComparingByValue(null);
        });
    }

    @Test
    public void testReversedComparingByValueWithComparator() {
        Comparator<Integer> absComp = Comparator.comparingInt(Math::abs);
        Comparator<Map.Entry<String, Integer>> comp = Comparators.reversedComparingByValue(absComp);

        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("a", -2), new SimpleEntry<>("b", 1), new SimpleEntry<>("c", -3));
        entries.sort(comp);

        assertEquals(-3, entries.get(0).getValue());
        assertEquals(-2, entries.get(1).getValue());
        assertEquals(1, entries.get(2).getValue());

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingByValue(null));
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

    @Test
    public void testArrayComparators() {
        boolean[] boolArr1 = { false, true, false };
        boolean[] boolArr2 = { false, true, true };
        boolean[] boolArr3 = { false, true };

        assertTrue(Comparators.BOOLEAN_ARRAY_COMPARATOR.compare(boolArr1, boolArr2) < 0);
        assertTrue(Comparators.BOOLEAN_ARRAY_COMPARATOR.compare(boolArr3, boolArr1) < 0);

        char[] charArr1 = { 'a', 'b', 'c' };
        char[] charArr2 = { 'a', 'b', 'd' };
        assertTrue(Comparators.CHAR_ARRAY_COMPARATOR.compare(charArr1, charArr2) < 0);

        byte[] byteArr1 = { 1, 2, 3 };
        byte[] byteArr2 = { 1, 2, 4 };
        assertTrue(Comparators.BYTE_ARRAY_COMPARATOR.compare(byteArr1, byteArr2) < 0);

        short[] shortArr1 = { 100, 200, 300 };
        short[] shortArr2 = { 100, 200, 400 };
        assertTrue(Comparators.SHORT_ARRAY_COMPARATOR.compare(shortArr1, shortArr2) < 0);

        int[] intArr1 = { 1, 2, 3 };
        int[] intArr2 = { 1, 2, 4 };
        assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(intArr1, intArr2) < 0);

        long[] longArr1 = { 1L, 2L, 3L };
        long[] longArr2 = { 1L, 2L, 4L };
        assertTrue(Comparators.LONG_ARRAY_COMPARATOR.compare(longArr1, longArr2) < 0);

        float[] floatArr1 = { 1.0f, 2.0f, 3.0f };
        float[] floatArr2 = { 1.0f, 2.0f, 3.5f };
        assertTrue(Comparators.FLOAT_ARRAY_COMPARATOR.compare(floatArr1, floatArr2) < 0);

        double[] doubleArr1 = { 1.0, 2.0, 3.0 };
        double[] doubleArr2 = { 1.0, 2.0, 3.5 };
        assertTrue(Comparators.DOUBLE_ARRAY_COMPARATOR.compare(doubleArr1, doubleArr2) < 0);

        Object[] objArr1 = { "a", "b", "c" };
        Object[] objArr2 = { "a", "b", "d" };
        assertTrue(Comparators.OBJECT_ARRAY_COMPARATOR.compare(objArr1, objArr2) < 0);

        Collection<String> coll1 = Arrays.asList("a", "b", "c");
        Collection<String> coll2 = Arrays.asList("a", "b", "d");
        assertTrue(Comparators.COLLECTION_COMPARATOR.compare(coll1, coll2) < 0);
    }

    @Test
    public void testNullHandlingInArrayComparators() {
        assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(null, new int[] { 1 }) < 0);
        assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(new int[] { 1 }, null) > 0);
        assertEquals(0, Comparators.INT_ARRAY_COMPARATOR.compare(null, null));

        assertEquals(0, Comparators.DOUBLE_ARRAY_COMPARATOR.compare(new double[0], new double[0]));
        assertTrue(Comparators.DOUBLE_ARRAY_COMPARATOR.compare(new double[0], new double[] { 1.0 }) < 0);
    }

    @Test
    public void testArrayComparators_equalContentDifferentLength() {
        // boolean - same prefix, different length (covers the Integer.compare path)
        assertTrue(Comparators.BOOLEAN_ARRAY_COMPARATOR.compare(new boolean[] { true }, new boolean[] { true, false }) < 0);
        assertTrue(Comparators.BOOLEAN_ARRAY_COMPARATOR.compare(new boolean[] { true, false }, new boolean[] { true }) > 0);
        assertEquals(0, Comparators.BOOLEAN_ARRAY_COMPARATOR.compare(new boolean[] { true }, new boolean[] { true }));

        // char
        assertTrue(Comparators.CHAR_ARRAY_COMPARATOR.compare(new char[] { 'a' }, new char[] { 'a', 'b' }) < 0);
        assertEquals(0, Comparators.CHAR_ARRAY_COMPARATOR.compare(new char[] { 'a', 'b' }, new char[] { 'a', 'b' }));

        // byte
        assertTrue(Comparators.BYTE_ARRAY_COMPARATOR.compare(new byte[] { 1 }, new byte[] { 1, 2 }) < 0);
        assertEquals(0, Comparators.BYTE_ARRAY_COMPARATOR.compare(new byte[] { 1, 2 }, new byte[] { 1, 2 }));

        // short
        assertTrue(Comparators.SHORT_ARRAY_COMPARATOR.compare(new short[] { 1 }, new short[] { 1, 2 }) < 0);
        assertEquals(0, Comparators.SHORT_ARRAY_COMPARATOR.compare(new short[] { 1, 2 }, new short[] { 1, 2 }));

        // int
        assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(new int[] { 1 }, new int[] { 1, 2 }) < 0);
        assertEquals(0, Comparators.INT_ARRAY_COMPARATOR.compare(new int[] { 1, 2 }, new int[] { 1, 2 }));

        // long
        assertTrue(Comparators.LONG_ARRAY_COMPARATOR.compare(new long[] { 1L }, new long[] { 1L, 2L }) < 0);
        assertEquals(0, Comparators.LONG_ARRAY_COMPARATOR.compare(new long[] { 1L, 2L }, new long[] { 1L, 2L }));

        // float
        assertTrue(Comparators.FLOAT_ARRAY_COMPARATOR.compare(new float[] { 1.0f }, new float[] { 1.0f, 2.0f }) < 0);
        assertEquals(0, Comparators.FLOAT_ARRAY_COMPARATOR.compare(new float[] { 1.0f, 2.0f }, new float[] { 1.0f, 2.0f }));

        // double
        assertTrue(Comparators.DOUBLE_ARRAY_COMPARATOR.compare(new double[] { 1.0 }, new double[] { 1.0, 2.0 }) < 0);
        assertEquals(0, Comparators.DOUBLE_ARRAY_COMPARATOR.compare(new double[] { 1.0, 2.0 }, new double[] { 1.0, 2.0 }));

        // object array
        assertTrue(Comparators.OBJECT_ARRAY_COMPARATOR.compare(new Object[] { "a" }, new Object[] { "a", "b" }) < 0);
        assertEquals(0, Comparators.OBJECT_ARRAY_COMPARATOR.compare(new Object[] { "a", "b" }, new Object[] { "a", "b" }));

        // collection
        assertEquals(0, Comparators.COLLECTION_COMPARATOR.compare(Arrays.asList("a", "b"), Arrays.asList("a", "b")));
        assertTrue(Comparators.COLLECTION_COMPARATOR.compare(Arrays.asList("a"), Arrays.asList("a", "b")) < 0);
    }

}
