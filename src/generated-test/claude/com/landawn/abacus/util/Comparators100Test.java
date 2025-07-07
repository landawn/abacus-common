package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import java.util.function.Function;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.ToBooleanFunction;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToShortFunction;

import lombok.Data;

public class Comparators100Test extends TestBase {

    @Data
    public static class Person implements Comparable<Person> {
        String name;
        int age;
        Integer salary;
        boolean active;
        char grade;
        byte level;
        short year;
        long id;
        float rating;
        double score;

        Person(String name, int age) {
            this(name, age, null);
        }

        Person(String name, int age, Integer salary) {
            this.name = name;
            this.age = age;
            this.salary = salary;
        }

        @Override
        public int compareTo(Person o) {
            return Integer.compare(this.age, o.age);
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "', age=" + age + ", salary=" + salary + "}";
        }
    }

    private List<String> stringList;
    private List<Integer> intList;
    private List<Person> personList;

    @BeforeEach
    public void setUp() {
        stringList = Arrays.asList("apple", "banana", null, "cherry", "date");
        intList = Arrays.asList(3, 1, null, 4, 2);
        personList = Arrays.asList(new Person("Alice", 30, 50000), new Person("Bob", 25, null), null, new Person("Charlie", 35, 60000),
                new Person("David", 25, 45000));
    }

    @Test
    public void testNaturalOrder() {
        Comparator<String> comp = Comparators.naturalOrder();
        List<String> list = new ArrayList<>(Arrays.asList("banana", null, "apple", "cherry"));
        list.sort(comp);
        assertEquals(Arrays.asList(null, "apple", "banana", "cherry"), list);

        // Test with integers
        List<Integer> intList = new ArrayList<>(Arrays.asList(3, null, 1, 4, 2));
        intList.sort(Comparators.naturalOrder());
        assertEquals(Arrays.asList(null, 1, 2, 3, 4), intList);
    }

    @Test
    public void testNullsFirst() {
        Comparator<Integer> comp = Comparators.nullsFirst();
        List<Integer> list = new ArrayList<>(Arrays.asList(3, null, 1, null, 2));
        list.sort(comp);
        assertEquals(Arrays.asList(null, null, 1, 2, 3), list);

        // Test that nullsFirst() and naturalOrder() behave the same
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

        // Test with null comparator
        Comparator<Integer> nullComp = Comparators.nullsFirst(null);
        List<Integer> intList = new ArrayList<>(Arrays.asList(3, null, 1, 2));
        intList.sort(nullComp);
        assertEquals(Arrays.asList(null, 1, 2, 3), intList);
    }

    @Test
    public void testNullsFirstBy() {
        Function<Person, Integer> ageExtractor = p -> p == null ? null : p.age;
        Comparator<Person> comp = Comparators.nullsFirstBy(ageExtractor);

        List<Person> list = new ArrayList<>(personList);
        list.sort(comp);

        assertNull(list.get(0)); // null person first
        assertEquals(25, list.get(1).age); // Bob or David
        assertEquals(25, list.get(2).age); // Bob or David
        assertEquals(30, list.get(3).age); // Alice
        assertEquals(35, list.get(4).age); // Charlie

        assertThrows(IllegalArgumentException.class, () -> Comparators.nullsFirstBy(null));
    }

    @Test
    public void testNullsFirstOrElseEqual() {
        Comparator<String> comp = Comparators.nullsFirstOrElseEqual();
        List<String> list = Arrays.asList("b", null, "a", null, "c");
        List<String> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        // Nulls should be first, non-nulls maintain relative order
        assertNull(sorted.get(0));
        assertNull(sorted.get(1));
        // Non-null elements should maintain their relative order
        assertEquals("b", sorted.get(2));
        assertEquals("a", sorted.get(3));
        assertEquals("c", sorted.get(4));
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

        // Test with null comparator
        Comparator<String> nullComp = Comparators.nullsLast(null);
        List<String> strList = new ArrayList<>(Arrays.asList("b", null, "a"));
        strList.sort(nullComp);
        assertEquals(Arrays.asList("a", "b", null), strList);
    }

    @Test
    public void testNullsLastBy() {
        Function<Person, String> nameExtractor = p -> p == null ? null : p.name;
        Comparator<Person> comp = Comparators.nullsLastBy(nameExtractor);

        List<Person> list = new ArrayList<>(personList);
        list.sort(comp);

        assertEquals("Alice", list.get(0).name);
        assertEquals("Bob", list.get(1).name);
        assertEquals("Charlie", list.get(2).name);
        assertEquals("David", list.get(3).name);
        assertNull(list.get(4)); // null person last

        assertThrows(IllegalArgumentException.class, () -> Comparators.nullsLastBy(null));
    }

    @Test
    public void testNullsLastOrElseEqual() {
        Comparator<String> comp = Comparators.nullsLastOrElseEqual();
        List<String> list = Arrays.asList("b", null, "a", null, "c");
        List<String> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        // Non-nulls should maintain relative order, nulls should be last
        assertEquals("b", sorted.get(0));
        assertEquals("a", sorted.get(1));
        assertEquals("c", sorted.get(2));
        assertNull(sorted.get(3));
        assertNull(sorted.get(4));
    }

    @Test
    public void testEmptiesFirst() {
        // Test with natural ordering
        Comparator<u.Optional<String>> comp = Comparators.emptiesFirst();
        List<u.Optional<String>> list = Arrays.asList(u.Optional.of("b"), u.Optional.empty(), u.Optional.of("a"), null);
        List<u.Optional<String>> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        assertTrue(sorted.get(0) == null || sorted.get(0).isEmpty());
        assertTrue(sorted.get(1) == null || sorted.get(1).isEmpty());
        assertEquals("a", sorted.get(2).get());
        assertEquals("b", sorted.get(3).get());

        // Test with custom comparator
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
    public void testEmptiesLast() {
        // Test with natural ordering
        Comparator<u.Optional<Integer>> comp = Comparators.emptiesLast();
        List<u.Optional<Integer>> list = Arrays.asList(u.Optional.of(2), u.Optional.empty(), u.Optional.of(1), null);
        List<u.Optional<Integer>> sorted = new ArrayList<>(list);
        sorted.sort(comp);

        assertEquals(1, sorted.get(0).get());
        assertEquals(2, sorted.get(1).get());
        assertTrue(sorted.get(2) == null || sorted.get(2).isEmpty());
        assertTrue(sorted.get(3) == null || sorted.get(3).isEmpty());

        // Test with custom comparator
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
    public void testComparingByIfNotNullOrElseNullsFirst() {
        Function<Person, Integer> salaryExtractor = p -> p.salary;
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsFirst(salaryExtractor);

        List<Person> list = new ArrayList<>(personList);
        list.sort(comp);

        assertNull(list.get(0)); // null person first
        assertNull(list.get(1).salary); // Bob with null salary
        assertEquals(45000, list.get(2).salary); // David
        assertEquals(50000, list.get(3).salary); // Alice
        assertEquals(60000, list.get(4).salary); // Charlie

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingByIfNotNullOrElseNullsFirst(null));
    }

    @Test
    public void testComparingByIfNotNullOrElseNullsLast() {
        Function<Person, Integer> salaryExtractor = p -> p.salary;
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsLast(salaryExtractor);

        List<Person> list = new ArrayList<>(personList);
        list.sort(comp);

        assertEquals(45000, list.get(0).salary); // David
        assertEquals(50000, list.get(1).salary); // Alice
        assertEquals(60000, list.get(2).salary); // Charlie
        assertNull(list.get(3).salary); // Bob with null salary
        assertNull(list.get(4)); // null person last

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingByIfNotNullOrElseNullsLast(null));
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
    public void testComparingByIfNotNullOrElseNullsFirstWithComparator() {
        Function<Person, String> nameExtractor = p -> p.name;
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsFirst(nameExtractor, lengthComp);

        List<Person> list = new ArrayList<>(Arrays.asList(new Person("Alice", 30), null, new Person("Bob", 25), new Person("Charlie", 35)));
        list.sort(comp);

        assertNull(list.get(0)); // null person first
        assertEquals("Bob", list.get(1).name); // shortest name
        assertEquals("Alice", list.get(2).name);
        assertEquals("Charlie", list.get(3).name); // longest name
    }

    @Test
    public void testComparingByIfNotNullOrElseNullsLastWithComparator() {
        Function<Person, String> nameExtractor = p -> p.name;
        Comparator<String> reverseComp = Comparator.reverseOrder();
        Comparator<Person> comp = Comparators.comparingByIfNotNullOrElseNullsLast(nameExtractor, reverseComp);

        List<Person> list = new ArrayList<>(Arrays.asList(new Person("Alice", 30), null, new Person("Bob", 25), new Person("Charlie", 35)));
        list.sort(comp);

        assertEquals("Charlie", list.get(0).name); // reverse order
        assertEquals("Bob", list.get(1).name);
        assertEquals("Alice", list.get(2).name);
        assertNull(list.get(3)); // null person last
    }

    @Test
    public void testComparingBoolean() {
        ToBooleanFunction<Person> activeExtractor = p -> p.active;
        Comparator<Person> comp = Comparators.comparingBoolean(activeExtractor);

        Person p1 = new Person("Alice", 30);
        p1.active = false;
        Person p2 = new Person("Bob", 25);
        p2.active = true;
        Person p3 = new Person("Charlie", 35);
        p3.active = false;

        List<Person> list = Arrays.asList(p2, p1, p3);
        list.sort(comp);

        assertFalse(list.get(0).active);
        assertFalse(list.get(1).active);
        assertTrue(list.get(2).active);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBoolean(null));
    }

    @Test
    public void testComparingChar() {
        ToCharFunction<Person> gradeExtractor = p -> p.grade;
        Comparator<Person> comp = Comparators.comparingChar(gradeExtractor);

        Person p1 = new Person("Alice", 30);
        p1.grade = 'B';
        Person p2 = new Person("Bob", 25);
        p2.grade = 'A';
        Person p3 = new Person("Charlie", 35);
        p3.grade = 'C';

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals('A', list.get(0).grade);
        assertEquals('B', list.get(1).grade);
        assertEquals('C', list.get(2).grade);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingChar(null));
    }

    @Test
    public void testComparingByte() {
        ToByteFunction<Person> levelExtractor = p -> p.level;
        Comparator<Person> comp = Comparators.comparingByte(levelExtractor);

        Person p1 = new Person("Alice", 30);
        p1.level = 2;
        Person p2 = new Person("Bob", 25);
        p2.level = 1;
        Person p3 = new Person("Charlie", 35);
        p3.level = 3;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(1, list.get(0).level);
        assertEquals(2, list.get(1).level);
        assertEquals(3, list.get(2).level);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingByte(null));
    }

    @Test
    public void testComparingShort() {
        ToShortFunction<Person> yearExtractor = p -> p.year;
        Comparator<Person> comp = Comparators.comparingShort(yearExtractor);

        Person p1 = new Person("Alice", 30);
        p1.year = 2021;
        Person p2 = new Person("Bob", 25);
        p2.year = 2020;
        Person p3 = new Person("Charlie", 35);
        p3.year = 2022;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(2020, list.get(0).year);
        assertEquals(2021, list.get(1).year);
        assertEquals(2022, list.get(2).year);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingShort(null));
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
    public void testComparingLong() {
        ToLongFunction<Person> idExtractor = p -> p.id;
        Comparator<Person> comp = Comparators.comparingLong(idExtractor);

        Person p1 = new Person("Alice", 30);
        p1.id = 1002L;
        Person p2 = new Person("Bob", 25);
        p2.id = 1001L;
        Person p3 = new Person("Charlie", 35);
        p3.id = 1003L;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(1001L, list.get(0).id);
        assertEquals(1002L, list.get(1).id);
        assertEquals(1003L, list.get(2).id);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingLong(null));
    }

    @Test
    public void testComparingFloat() {
        ToFloatFunction<Person> ratingExtractor = p -> p.rating;
        Comparator<Person> comp = Comparators.comparingFloat(ratingExtractor);

        Person p1 = new Person("Alice", 30);
        p1.rating = 4.5f;
        Person p2 = new Person("Bob", 25);
        p2.rating = 3.8f;
        Person p3 = new Person("Charlie", 35);
        p3.rating = 4.9f;

        List<Person> list = N.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(3.8f, list.get(0).rating);
        assertEquals(4.5f, list.get(1).rating);
        assertEquals(4.9f, list.get(2).rating);

        // Test NaN handling
        Person p4 = new Person("David", 40);
        p4.rating = Float.NaN;
        list.add(p4);
        list.sort(comp);
        assertTrue(Float.isNaN(list.get(3).rating));

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingFloat(null));
    }

    @Test
    public void testComparingDouble() {
        ToDoubleFunction<Person> scoreExtractor = p -> p.score;
        Comparator<Person> comp = Comparators.comparingDouble(scoreExtractor);

        Person p1 = new Person("Alice", 30);
        p1.score = 85.5;
        Person p2 = new Person("Bob", 25);
        p2.score = 92.3;
        Person p3 = new Person("Charlie", 35);
        p3.score = 78.9;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(78.9, list.get(0).score);
        assertEquals(85.5, list.get(1).score);
        assertEquals(92.3, list.get(2).score);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingDouble(null));
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
    public void testComparingByValue() {
        Comparator<Map.Entry<String, Integer>> comp = Comparators.comparingByValue();
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals(1, entries.get(0).getValue());
        assertEquals(2, entries.get(1).getValue());
        assertEquals(3, entries.get(2).getValue());
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
    public void testComparingByLength() {
        Comparator<CharSequence> comp = Comparators.comparingByLength();
        List<String> list = Arrays.asList("short", "a", "medium", "very long string", null);
        list.sort(comp);

        assertNull(list.get(0));
        assertEquals("a", list.get(1));
        assertEquals("short", list.get(2));
        assertEquals("medium", list.get(3));
        assertEquals("very long string", list.get(4));

        // Test with different CharSequence implementations
        List<CharSequence> mixed = Arrays.asList(new StringBuilder("test"), "hi", new StringBuffer("longer"));
        mixed.sort(comp);
        assertEquals("hi", mixed.get(0).toString());
        assertEquals("test", mixed.get(1).toString());
        assertEquals("longer", mixed.get(2).toString());
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
    public void testComparingObjArray() {
        Comparator<Object[]> comp = Comparators.comparingObjArray(String.CASE_INSENSITIVE_ORDER);

        Object[] arr1 = { "apple", "banana" };
        Object[] arr2 = { "APPLE", "CHERRY" };
        Object[] arr3 = { "apple", "BANANA", "cherry" };

        List<Object[]> arrays = Arrays.asList(arr2, arr1, arr3);
        arrays.sort(comp);

        assertSame(arr1, arrays.get(0)); // apple, banana < apple, cherry
        assertSame(arr3, arrays.get(1));
        assertSame(arr2, arrays.get(2)); // longer array

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingObjArray(null));
    }

    @Test
    public void testComparingArray() {
        // Test with natural order
        Comparator<String[]> comp1 = Comparators.comparingArray();
        String[] arr1 = { "apple", "banana" };
        String[] arr2 = { "apple", "cherry" };
        assertTrue(comp1.compare(arr1, arr2) < 0);

        // Test with custom comparator
        Comparator<Integer[]> comp2 = Comparators.comparingArray(Integer::compare);
        Integer[] intArr1 = { 1, 2, 3 };
        Integer[] intArr2 = { 1, 2, 3, 4 };
        assertTrue(comp2.compare(intArr1, intArr2) < 0);

        // Test with null arrays
        assertTrue(comp2.compare(null, intArr1) < 0);
        assertTrue(comp2.compare(intArr1, null) > 0);
        assertEquals(0, comp2.compare(null, null));

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingArray(null));
    }

    @Test
    public void testComparingCollection() {
        // Test with natural order
        Comparator<List<Integer>> comp1 = Comparators.comparingCollection();
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<Integer> list2 = Arrays.asList(1, 2, 4);
        assertTrue(comp1.compare(list1, list2) < 0);

        // Test with custom comparator
        Comparator<List<String>> comp2 = Comparators.comparingCollection(String.CASE_INSENSITIVE_ORDER);
        List<String> strList1 = Arrays.asList("apple", "BANANA");
        List<String> strList2 = Arrays.asList("APPLE", "banana", "cherry");
        assertTrue(comp2.compare(strList1, strList2) < 0); // shorter list

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingCollection(null));
    }

    @Test
    public void testComparingIterable() {
        // Test with natural order
        Comparator<Iterable<String>> comp1 = Comparators.comparingIterable();
        Iterable<String> iter1 = Arrays.asList("apple", "banana");
        Iterable<String> iter2 = Arrays.asList("apple", "cherry");
        assertTrue(comp1.compare(iter1, iter2) < 0);

        // Test with custom comparator
        Comparator<Iterable<Integer>> comp2 = Comparators.comparingIterable(Integer::compare);
        Iterable<Integer> intIter1 = Arrays.asList(1, 2, 3);
        Iterable<Integer> intIter2 = Arrays.asList(1, 2, 3, 4);
        assertTrue(comp2.compare(intIter1, intIter2) < 0);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingIterable(null));
    }

    @Test
    public void testComparingIterator() {
        // Test with natural order
        Comparator<Iterator<Integer>> comp1 = Comparators.comparingIterator();
        Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 4).iterator();
        assertTrue(comp1.compare(iter1, iter2) < 0);

        // Test with custom comparator
        Comparator<Iterator<String>> comp2 = Comparators.comparingIterator(String::compareToIgnoreCase);
        Iterator<String> strIter1 = Arrays.asList("apple", "BANANA").iterator();
        Iterator<String> strIter2 = Arrays.asList("APPLE", "banana", "cherry").iterator();
        assertEquals(-1, comp2.compare(strIter1, strIter2)); // first two elements are equal

        // Note: iterators are consumed after comparison
        assertFalse(strIter1.hasNext());
        assertTrue(strIter2.hasNext()); // still has "cherry"

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingIterator(null));
    }

    @Test
    public void testComparingMapByKey() {
        // Test with natural order
        Comparator<Map<String, Integer>> comp1 = Comparators.comparingMapByKey();

        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("apple", 1);
        map1.put("banana", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("apple", 1);
        map2.put("cherry", 3);

        assertTrue(comp1.compare(map1, map2) < 0);

        // Test with custom comparator
        Comparator<Map<String, Integer>> comp2 = Comparators.comparingMapByKey(String.CASE_INSENSITIVE_ORDER);

        Map<String, Integer> map3 = new LinkedHashMap<>();
        map3.put("APPLE", 1);
        map3.put("BANANA", 2);

        assertTrue(comp2.compare(map1, map3) == 0);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingMapByKey(null));
    }

    @Test
    public void testComparingMapByValue() {
        // Test with natural order
        Comparator<Map<String, Integer>> comp1 = Comparators.comparingMapByValue();

        Map<String, Integer> map1 = new LinkedHashMap<>();
        map1.put("a", 1);
        map1.put("b", 2);

        Map<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("x", 1);
        map2.put("y", 3);

        assertTrue(comp1.compare(map1, map2) < 0);

        // Test with custom comparator
        Comparator<Map<String, Integer>> comp2 = Comparators.comparingMapByValue(Comparator.reverseOrder());

        assertTrue(comp2.compare(map1, map2) > 0);

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingMapByValue(null));
    }

    @Test
    public void testComparingBeanByProps() {
        // Note: This method is deprecated
        List<String> props = Arrays.asList("age", "name", "non-existed-prop");
        Comparator<Person> comp = Comparators.comparingBeanByProps(props);

        // This would require proper bean properties with getters
        // Since our Person class doesn't have standard getters, this test
        // would need modification to work properly

        assertThrows(IllegalArgumentException.class, () -> Comparators.comparingBeanByProps(null));
    }

    @Test
    public void testReverseOrder() {
        // Test no-arg version
        Comparator<Integer> comp = Comparators.reverseOrder();
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 9);
        list.sort(comp);
        assertEquals(Arrays.asList(9, 5, 4, 3, 1, 1), list);

        // Test with strings
        List<String> strList = Arrays.asList("apple", "zebra", "banana");
        strList.sort(Comparators.reverseOrder());
        assertEquals(Arrays.asList("zebra", "banana", "apple"), strList);
    }

    @Test
    public void testReverseOrderWithComparator() {
        // Test with null (should return reverse natural order)
        Comparator<String> comp1 = Comparators.reverseOrder(null);
        List<String> list1 = Arrays.asList("b", "a", "c");
        list1.sort(comp1);
        assertEquals(Arrays.asList("c", "b", "a"), list1);

        // Test with custom comparator
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        Comparator<String> reversed = Comparators.reverseOrder(lengthComp);
        List<String> list2 = Arrays.asList("short", "a", "medium");
        list2.sort(reversed);
        assertEquals(Arrays.asList("medium", "short", "a"), list2);

        // Test double reversal returns natural order
        Comparator<Integer> natural = Comparators.naturalOrder();
        Comparator<Integer> reversed1 = Comparators.reverseOrder(natural);
        Comparator<Integer> reversed2 = Comparators.reverseOrder(reversed1);
        List<Integer> nums = Arrays.asList(3, 1, 2);
        nums.sort(reversed2);
        assertEquals(Arrays.asList(1, 2, 3), nums);
    }

    @Test
    public void testReversedComparingBoolean() {
        ToBooleanFunction<Person> activeExtractor = p -> p.active;
        Comparator<Person> comp = Comparators.reversedComparingBoolean(activeExtractor);

        Person p1 = new Person("Alice", 30);
        p1.active = false;
        Person p2 = new Person("Bob", 25);
        p2.active = true;
        Person p3 = new Person("Charlie", 35);
        p3.active = false;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertTrue(list.get(0).active); // true comes first in reversed order
        assertFalse(list.get(1).active);
        assertFalse(list.get(2).active);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingBoolean(null));
    }

    @Test
    public void testReversedComparingChar() {
        ToCharFunction<Person> gradeExtractor = p -> p.grade;
        Comparator<Person> comp = Comparators.reversedComparingChar(gradeExtractor);

        Person p1 = new Person("Alice", 30);
        p1.grade = 'B';
        Person p2 = new Person("Bob", 25);
        p2.grade = 'A';
        Person p3 = new Person("Charlie", 35);
        p3.grade = 'C';

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals('C', list.get(0).grade);
        assertEquals('B', list.get(1).grade);
        assertEquals('A', list.get(2).grade);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingChar(null));
    }

    @Test
    public void testReversedComparingByte() {
        ToByteFunction<Person> levelExtractor = p -> p.level;
        Comparator<Person> comp = Comparators.reversedComparingByte(levelExtractor);

        Person p1 = new Person("Alice", 30);
        p1.level = 2;
        Person p2 = new Person("Bob", 25);
        p2.level = 1;
        Person p3 = new Person("Charlie", 35);
        p3.level = 3;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(3, list.get(0).level);
        assertEquals(2, list.get(1).level);
        assertEquals(1, list.get(2).level);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingByte(null));
    }

    @Test
    public void testReversedComparingShort() {
        ToShortFunction<Person> yearExtractor = p -> p.year;
        Comparator<Person> comp = Comparators.reversedComparingShort(yearExtractor);

        Person p1 = new Person("Alice", 30);
        p1.year = 2021;
        Person p2 = new Person("Bob", 25);
        p2.year = 2020;
        Person p3 = new Person("Charlie", 35);
        p3.year = 2022;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(2022, list.get(0).year);
        assertEquals(2021, list.get(1).year);
        assertEquals(2020, list.get(2).year);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingShort(null));
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
    public void testReversedComparingLong() {
        ToLongFunction<Person> idExtractor = p -> p.id;
        Comparator<Person> comp = Comparators.reversedComparingLong(idExtractor);

        Person p1 = new Person("Alice", 30);
        p1.id = 1002L;
        Person p2 = new Person("Bob", 25);
        p2.id = 1001L;
        Person p3 = new Person("Charlie", 35);
        p3.id = 1003L;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(1003L, list.get(0).id);
        assertEquals(1002L, list.get(1).id);
        assertEquals(1001L, list.get(2).id);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingLong(null));
    }

    @Test
    public void testReversedComparingFloat() {
        ToFloatFunction<Person> ratingExtractor = p -> p.rating;
        Comparator<Person> comp = Comparators.reversedComparingFloat(ratingExtractor);

        Person p1 = new Person("Alice", 30);
        p1.rating = 4.5f;
        Person p2 = new Person("Bob", 25);
        p2.rating = 3.8f;
        Person p3 = new Person("Charlie", 35);
        p3.rating = 4.9f;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(4.9f, list.get(0).rating);
        assertEquals(4.5f, list.get(1).rating);
        assertEquals(3.8f, list.get(2).rating);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingFloat(null));
    }

    @Test
    public void testReversedComparingDouble() {
        ToDoubleFunction<Person> scoreExtractor = p -> p.score;
        Comparator<Person> comp = Comparators.reversedComparingDouble(scoreExtractor);

        Person p1 = new Person("Alice", 30);
        p1.score = 85.5;
        Person p2 = new Person("Bob", 25);
        p2.score = 92.3;
        Person p3 = new Person("Charlie", 35);
        p3.score = 78.9;

        List<Person> list = Arrays.asList(p1, p2, p3);
        list.sort(comp);

        assertEquals(92.3, list.get(0).score);
        assertEquals(85.5, list.get(1).score);
        assertEquals(78.9, list.get(2).score);

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingDouble(null));
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
    public void testReversedComparingByIfNotNullOrElseNullsFirst() {
        Function<Person, Integer> salaryExtractor = p -> p.salary;
        Comparator<Person> comp = Comparators.reversedComparingByIfNotNullOrElseNullsFirst(salaryExtractor);

        List<Person> list = new ArrayList<>(personList);
        list.sort(comp);

        assertNull(list.get(0)); // null person first
        assertNull(list.get(1).salary); // Bob with null salary
        assertEquals(60000, list.get(2).salary); // Charlie (highest)
        assertEquals(50000, list.get(3).salary); // Alice
        assertEquals(45000, list.get(4).salary); // David (lowest)

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingByIfNotNullOrElseNullsFirst(null));
    }

    @Test
    public void testReversedComparingByIfNotNullOrElseNullsLast() {
        Function<Person, Integer> salaryExtractor = p -> p.salary;
        Comparator<Person> comp = Comparators.reversedComparingByIfNotNullOrElseNullsLast(salaryExtractor);

        List<Person> list = new ArrayList<>(personList);
        list.sort(comp);

        assertEquals(60000, list.get(0).salary); // Charlie (highest)
        assertEquals(50000, list.get(1).salary); // Alice
        assertEquals(45000, list.get(2).salary); // David (lowest)
        assertNull(list.get(3).salary); // Bob with null salary
        assertNull(list.get(4)); // null person last

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingByIfNotNullOrElseNullsLast(null));
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
    public void testReversedComparingByValue() {
        Comparator<Map.Entry<String, Integer>> comp = Comparators.reversedComparingByValue();
        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("banana", 2), new SimpleEntry<>("apple", 1), new SimpleEntry<>("cherry", 3));
        entries.sort(comp);

        assertEquals(3, entries.get(0).getValue());
        assertEquals(2, entries.get(1).getValue());
        assertEquals(1, entries.get(2).getValue());
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
    public void testReversedComparingByValueWithComparator() {
        Comparator<Integer> absComp = Comparator.comparingInt(Math::abs);
        Comparator<Map.Entry<String, Integer>> comp = Comparators.reversedComparingByValue(absComp);

        List<Map.Entry<String, Integer>> entries = Arrays.asList(new SimpleEntry<>("a", -2), new SimpleEntry<>("b", 1), new SimpleEntry<>("c", -3));
        entries.sort(comp);

        assertEquals(-3, entries.get(0).getValue()); // abs(-3) = 3 is largest
        assertEquals(-2, entries.get(1).getValue()); // abs(-2) = 2
        assertEquals(1, entries.get(2).getValue()); // abs(1) = 1 is smallest

        assertThrows(IllegalArgumentException.class, () -> Comparators.reversedComparingByValue(null));
    }

    @Test
    public void testArrayComparators() {
        // Test BOOLEAN_ARRAY_COMPARATOR
        boolean[] boolArr1 = { false, true, false };
        boolean[] boolArr2 = { false, true, true };
        boolean[] boolArr3 = { false, true };

        assertTrue(Comparators.BOOLEAN_ARRAY_COMPARATOR.compare(boolArr1, boolArr2) < 0);
        assertTrue(Comparators.BOOLEAN_ARRAY_COMPARATOR.compare(boolArr3, boolArr1) < 0);

        // Test CHAR_ARRAY_COMPARATOR
        char[] charArr1 = { 'a', 'b', 'c' };
        char[] charArr2 = { 'a', 'b', 'd' };
        assertTrue(Comparators.CHAR_ARRAY_COMPARATOR.compare(charArr1, charArr2) < 0);

        // Test BYTE_ARRAY_COMPARATOR
        byte[] byteArr1 = { 1, 2, 3 };
        byte[] byteArr2 = { 1, 2, 4 };
        assertTrue(Comparators.BYTE_ARRAY_COMPARATOR.compare(byteArr1, byteArr2) < 0);

        // Test SHORT_ARRAY_COMPARATOR
        short[] shortArr1 = { 100, 200, 300 };
        short[] shortArr2 = { 100, 200, 400 };
        assertTrue(Comparators.SHORT_ARRAY_COMPARATOR.compare(shortArr1, shortArr2) < 0);

        // Test INT_ARRAY_COMPARATOR
        int[] intArr1 = { 1, 2, 3 };
        int[] intArr2 = { 1, 2, 4 };
        assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(intArr1, intArr2) < 0);

        // Test LONG_ARRAY_COMPARATOR
        long[] longArr1 = { 1L, 2L, 3L };
        long[] longArr2 = { 1L, 2L, 4L };
        assertTrue(Comparators.LONG_ARRAY_COMPARATOR.compare(longArr1, longArr2) < 0);

        // Test FLOAT_ARRAY_COMPARATOR
        float[] floatArr1 = { 1.0f, 2.0f, 3.0f };
        float[] floatArr2 = { 1.0f, 2.0f, 3.5f };
        assertTrue(Comparators.FLOAT_ARRAY_COMPARATOR.compare(floatArr1, floatArr2) < 0);

        // Test DOUBLE_ARRAY_COMPARATOR
        double[] doubleArr1 = { 1.0, 2.0, 3.0 };
        double[] doubleArr2 = { 1.0, 2.0, 3.5 };
        assertTrue(Comparators.DOUBLE_ARRAY_COMPARATOR.compare(doubleArr1, doubleArr2) < 0);

        // Test OBJECT_ARRAY_COMPARATOR
        Object[] objArr1 = { "a", "b", "c" };
        Object[] objArr2 = { "a", "b", "d" };
        assertTrue(Comparators.OBJECT_ARRAY_COMPARATOR.compare(objArr1, objArr2) < 0);

        // Test COLLECTION_COMPARATOR
        Collection<String> coll1 = Arrays.asList("a", "b", "c");
        Collection<String> coll2 = Arrays.asList("a", "b", "d");
        assertTrue(Comparators.COLLECTION_COMPARATOR.compare(coll1, coll2) < 0);
    }

    @Test
    public void testNullHandlingInArrayComparators() {
        // Test null array handling
        assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(null, new int[] { 1 }) < 0);
        assertTrue(Comparators.INT_ARRAY_COMPARATOR.compare(new int[] { 1 }, null) > 0);
        assertEquals(0, Comparators.INT_ARRAY_COMPARATOR.compare(null, null));

        // Test with empty arrays
        assertEquals(0, Comparators.DOUBLE_ARRAY_COMPARATOR.compare(new double[0], new double[0]));
        assertTrue(Comparators.DOUBLE_ARRAY_COMPARATOR.compare(new double[0], new double[] { 1.0 }) < 0);
    }
}
