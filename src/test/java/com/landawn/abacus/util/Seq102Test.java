package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;

@Tag("new-test")
public class Seq102Test extends TestBase {

    @Test
    public void testMapIfNotNull() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", null, "b", null, "c");
        List<String> result = seq.mapIfNotNull(String::toUpperCase).toList();
        assertEquals(Arrays.asList("A", "B", "C"), result);

        seq = Seq.of("x", "y", "z");
        result = seq.mapIfNotNull(s -> s + "1").toList();
        assertEquals(Arrays.asList("x1", "y1", "z1"), result);

        seq = Seq.<String, Exception> empty();
        result = seq.mapIfNotNull(String::toUpperCase).toList();
        assertTrue(result.isEmpty());

        seq = Seq.of(null, null, null);
        result = seq.mapIfNotNull(s -> s + "test").toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapFirst() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello", "world", "java");
        List<String> result = seq.mapFirst(String::toUpperCase).toList();
        assertEquals(Arrays.asList("HELLO", "world", "java"), result);

        seq = Seq.of("single");
        result = seq.mapFirst(s -> s + "!").toList();
        assertEquals(Arrays.asList("single!"), result);

        seq = Seq.<String, Exception> empty();
        result = seq.mapFirst(String::toUpperCase).toList();
        assertTrue(result.isEmpty());

        Seq<Integer, Exception> intSeq = Seq.of(1, 2, 3, 4);
        List<Integer> intResult = intSeq.mapFirst(n -> n * 100).toList();
        assertEquals(Arrays.asList(100, 2, 3, 4), intResult);
    }

    @Test
    public void testMapFirstOrElse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<String> result = seq.mapFirstOrElse(n -> "First: " + n, n -> "Other: " + n).toList();
        assertEquals(Arrays.asList("First: 1", "Other: 2", "Other: 3", "Other: 4"), result);

        seq = Seq.of(10);
        result = seq.mapFirstOrElse(n -> "FIRST", n -> "ELSE").toList();
        assertEquals(Arrays.asList("FIRST"), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.mapFirstOrElse(n -> "First: " + n, n -> "Other: " + n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapLast() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello", "world", "java");
        List<String> result = seq.mapLast(s -> s + "!").toList();
        assertEquals(Arrays.asList("hello", "world", "java!"), result);

        seq = Seq.of("single");
        result = seq.mapLast(String::toUpperCase).toList();
        assertEquals(Arrays.asList("SINGLE"), result);

        seq = Seq.<String, Exception> empty();
        result = seq.mapLast(s -> s + "!").toList();
        assertTrue(result.isEmpty());

        Seq<Integer, Exception> intSeq = Seq.of(1, 2, 3, 4);
        List<Integer> intResult = intSeq.mapLast(n -> n * 100).toList();
        assertEquals(Arrays.asList(1, 2, 3, 400), intResult);
    }

    @Test
    public void testMapLastOrElse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<String> result = seq.mapLastOrElse(n -> "Last: " + n, n -> "Other: " + n).toList();
        assertEquals(Arrays.asList("Other: 1", "Other: 2", "Other: 3", "Last: 4"), result);

        seq = Seq.of(10);
        result = seq.mapLastOrElse(n -> "LAST", n -> "ELSE").toList();
        assertEquals(Arrays.asList("LAST"), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.mapLastOrElse(n -> "Last: " + n, n -> "Other: " + n).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatMap() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flatMap(n -> Seq.of(n, n * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);

        Seq<String, Exception> sentences = Seq.of("Hello world", "Java programming");
        List<String> words = sentences.flatMap(s -> Seq.of(s.split(" "))).toList();
        assertEquals(Arrays.asList("Hello", "world", "Java", "programming"), words);

        seq = Seq.of(1, 2, 3);
        result = seq.flatMap(n -> n % 2 == 0 ? Seq.of(n) : Seq.<Integer, Exception> empty()).toList();
        assertEquals(Arrays.asList(2), result);

        seq = Seq.of(1, 2, 3);
        result = seq.flatMap(n -> n == 2 ? null : Seq.of(n)).toList();
        assertEquals(Arrays.asList(1, 3), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.flatMap(n -> Seq.of(n, n * 2)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmap() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flatmap(n -> Arrays.asList(n, n * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);

        Seq<String, Exception> words = Seq.of("Hello", "World");
        List<Character> chars = words.flatmap(s -> {
            List<Character> list = new ArrayList<>();
            for (char c : s.toCharArray()) {
                list.add(c);
            }
            return list;
        }).toList();
        assertEquals(Arrays.asList('H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd'), chars);

        seq = Seq.of(1, 2, 3);
        result = seq.flatmap(n -> n % 2 == 0 ? Arrays.asList(n) : Collections.emptyList()).toList();
        assertEquals(Arrays.asList(2), result);

        seq = Seq.of(1, 2, 3);
        result = seq.flatmap(n -> n == 2 ? null : Arrays.asList(n)).toList();
        assertEquals(Arrays.asList(1, 3), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.flatmap(n -> Arrays.asList(n, n * 2)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlatmapIfNotNull() throws Exception {
        Seq<String, Exception> seq = Seq.of("Hello", null, "World", null);
        List<Character> chars = seq.flatmapIfNotNull(s -> {
            List<Character> list = new ArrayList<>();
            for (char c : s.toCharArray()) {
                list.add(c);
            }
            return list;
        }).toList();
        assertEquals(Arrays.asList('H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd'), chars);

        seq = Seq.of("AB", "CD");
        chars = seq.flatmapIfNotNull(s -> Arrays.asList(s.charAt(0), s.charAt(1))).toList();
        assertEquals(Arrays.asList('A', 'B', 'C', 'D'), chars);

        seq = Seq.of(null, null, null);
        chars = seq.flatmapIfNotNull(s -> Arrays.asList('X')).toList();
        assertTrue(chars.isEmpty());
    }

    public static class Department {
        List<Team> teams;

        Department(Team... teams) {
            this.teams = Arrays.asList(teams);
        }

        List<Team> getTeams() {
            return teams;
        }
    }

    public static class Team {
        List<Employee> members;

        Team(Employee... members) {
            this.members = Arrays.asList(members);
        }

        List<Employee> getMembers() {
            return members;
        }
    }

    public static class Employee {
        String name;

        Employee(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Test
    public void testFlatmapIfNotNullWithTwoMappers() throws Exception {

        Department dept1 = new Department(new Team(new Employee("Alice"), new Employee("Bob")), null, new Team(new Employee("Charlie")));
        Department dept2 = new Department(new Team(new Employee("David")));

        Seq<Department, Exception> departments = Seq.of(dept1, null, dept2);
        List<Employee> allEmployees = departments.flatmapIfNotNull(Department::getTeams, Team::getMembers).toList();

        assertEquals(4, allEmployees.size());
        assertEquals("Alice", allEmployees.get(0).toString());
        assertEquals("Bob", allEmployees.get(1).toString());
        assertEquals("Charlie", allEmployees.get(2).toString());
        assertEquals("David", allEmployees.get(3).toString());
    }

    @Test
    public void testMapPartial() throws Exception {
        Seq<String, Exception> seq = Seq.of("1", "2", "abc", "3");
        List<Integer> numbers = seq.mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3), numbers);

        seq = Seq.of("10", "20", "30");
        numbers = seq.mapPartial(s -> Optional.of(Integer.parseInt(s))).toList();
        assertEquals(Arrays.asList(10, 20, 30), numbers);

        seq = Seq.of("a", "b", "c");
        numbers = seq.mapPartial(s -> Optional.<Integer> empty()).toList();
        assertTrue(numbers.isEmpty());

        seq = Seq.<String, Exception> empty();
        numbers = seq.mapPartial(s -> Optional.of(1)).toList();
        assertTrue(numbers.isEmpty());
    }

    @Test
    public void testMapPartialToInt() throws Exception {
        Seq<String, Exception> seq = Seq.of("1", "2", "abc", "3");
        List<Integer> numbers = seq.mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 2, 3), numbers);

        seq = Seq.of("10", "20", "30");
        numbers = seq.mapPartialToInt(s -> OptionalInt.of(Integer.parseInt(s))).toList();
        assertEquals(Arrays.asList(10, 20, 30), numbers);

        seq = Seq.<String, Exception> empty();
        numbers = seq.mapPartialToInt(s -> OptionalInt.of(1)).toList();
        assertTrue(numbers.isEmpty());
    }

    @Test
    public void testMapPartialToLong() throws Exception {
        Seq<String, Exception> seq = Seq.of("100", "200", "abc", "300");
        List<Long> numbers = seq.mapPartialToLong(s -> {
            try {
                return OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(100L, 200L, 300L), numbers);

        seq = Seq.of("1000", "2000", "3000");
        numbers = seq.mapPartialToLong(s -> OptionalLong.of(Long.parseLong(s))).toList();
        assertEquals(Arrays.asList(1000L, 2000L, 3000L), numbers);
    }

    @Test
    public void testMapPartialToDouble() throws Exception {
        Seq<String, Exception> seq = Seq.of("1.5", "2.7", "abc", "3.9");
        List<Double> numbers = seq.mapPartialToDouble(s -> {
            try {
                return OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return OptionalDouble.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1.5, 2.7, 3.9), numbers);

        seq = Seq.of("10.1", "20.2", "30.3");
        numbers = seq.mapPartialToDouble(s -> OptionalDouble.of(Double.parseDouble(s))).toList();
        assertEquals(Arrays.asList(10.1, 20.2, 30.3), numbers);
    }

    @Test
    public void testMapMulti() throws Exception {
        Seq<String, Exception> seq = Seq.of("Hi", "World");

        List<Character> chars = seq.mapMulti(Fnn.<String, Character, Exception> mc((str, consumer) -> {
            for (char c : str.toCharArray()) {
                consumer.accept(c);
            }
        })).toList();
        assertEquals(Arrays.asList('H', 'i', 'W', 'o', 'r', 'l', 'd'), chars);

        Seq<Integer, Exception> intSeq = Seq.of(1, 2, 3);
        List<Integer> result = intSeq.mapMulti(Fnn.<Integer, Integer, Exception> mc((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 2);
            consumer.accept(n * 3);
        })).toList();
        assertEquals(Arrays.asList(1, 2, 3, 2, 4, 6, 3, 6, 9), result);

        intSeq = Seq.of(1, 2, 3, 4, 5);
        result = intSeq.mapMulti(Fnn.<Integer, Integer, Exception> mc((n, consumer) -> {
            if (n % 2 == 0) {
                consumer.accept(n);
                consumer.accept(n * 10);
            }
        })).toList();
        assertEquals(Arrays.asList(2, 20, 4, 40), result);

        intSeq = Seq.<Integer, Exception> empty();
        result = intSeq.mapMulti(Fnn.<Integer, Integer, Exception> mc((n, consumer) -> consumer.accept(n))).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingMapBiFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<Integer> result = seq.slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7), result);

        seq = Seq.of(1);
        List<String> strResult = seq.slidingMap((a, b) -> String.valueOf(a) + "," + String.valueOf(b)).toList();
        assertEquals(Arrays.asList("1,null"), strResult);

        seq = Seq.<Integer, Exception> empty();
        result = seq.slidingMap((a, b) -> a + b).toList();
        assertTrue(result.isEmpty());

        Seq<String, Exception> strSeq = Seq.of("a", "b", "c");
        strResult = strSeq.slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList("ab", "bc"), strResult);
    }

    @Test
    public void testSlidingMapWithIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<Integer> result = seq.slidingMap(2, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 7), result);

        seq = Seq.of(1, 2, 3, 4, 5);
        List<String> strResult = seq.slidingMap(2, (a, b) -> a + "," + b).toList();
        assertEquals(Arrays.asList("1,2", "3,4", "5,null"), strResult);

        seq = Seq.of(1, 2, 3);
        result = seq.slidingMap(1, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        result = seq.slidingMap(3, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 9), result);
    }

    @Test
    public void testSlidingMapWithIgnoreNotPaired() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<String> result = seq.slidingMap(2, false, (a, b) -> a + "," + b).toList();
        assertEquals(Arrays.asList("1,2", "3,4", "5,null"), result);

        seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> intResult = seq.slidingMap(2, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 7), intResult);

        seq = Seq.of(1);
        intResult = seq.slidingMap(1, true, (a, b) -> a + b).toList();
        assertTrue(intResult.isEmpty());

        seq = Seq.of(1, 2, 3, 4);
        intResult = seq.slidingMap(2, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 7), intResult);
    }

    @Test
    public void testSlidingMapTriFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9, 12), result);

        seq = Seq.of(1);
        List<String> strResult = seq.slidingMap((a, b, c) -> String.format("%s,%s,%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1,null,null"), strResult);

        seq = Seq.of(1, 2);
        strResult = seq.slidingMap((a, b, c) -> String.format("%s,%s,%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1,2,null"), strResult);

        seq = Seq.<Integer, Exception> empty();
        result = seq.slidingMap((a, b, c) -> a + b + c).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingMapTriFunctionWithIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<Integer> result = seq.slidingMap(3, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 15), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<String> strResult = seq.slidingMap(3, (a, b, c) -> String.format("%s+%s+%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1+2+3", "4+5+6", "7+8+null"), strResult);

        seq = Seq.of(1, 2, 3, 4);
        result = seq.slidingMap(1, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        result = seq.slidingMap(2, (a, b, c) -> a + b + (c == null ? 0 : c)).toList();
        assertEquals(Arrays.asList(6, 12, 11), result);
    }

    @Test
    public void testSlidingMapTriFunctionWithIgnoreNotPaired() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7);
        List<String> result = seq.slidingMap(3, false, (a, b, c) -> String.format("%s+%s+%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1+2+3", "4+5+6", "7+null+null"), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6, 7);
        List<Integer> intResult = seq.slidingMap(3, true, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 15), intResult);

        seq = Seq.of(1, 2);
        intResult = seq.slidingMap(1, true, (a, b, c) -> a + b + c).toList();
        assertTrue(intResult.isEmpty());

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        intResult = seq.slidingMap(3, true, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 15), intResult);
    }

    @Test
    public void testGroupByKeyMapper() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e");
        Map<Integer, List<String>> groups = seq.groupBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList("a", "e"), groups.get(1));
        assertEquals(Arrays.asList("bb", "dd"), groups.get(2));
        assertEquals(Arrays.asList("ccc"), groups.get(3));

        seq = Seq.<String, Exception> empty();
        List<Map.Entry<Integer, List<String>>> result = seq.groupBy(String::length).toList();
        assertTrue(result.isEmpty());

        seq = Seq.of("hello");
        result = seq.groupBy(String::length).toList();
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getKey().intValue());
        assertEquals(Arrays.asList("hello"), result.get(0).getValue());
    }

    @Test
    public void testGroupByWithMapFactory() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e");
        List<Map.Entry<Integer, List<String>>> result = seq.groupBy(String::length, Suppliers.ofTreeMap()).toList();

        assertEquals(1, result.get(0).getKey().intValue());
        assertEquals(2, result.get(1).getKey().intValue());
        assertEquals(3, result.get(2).getKey().intValue());

        seq = Seq.of("ccc", "a", "bb");
        Map<Integer, List<String>> groups = seq.groupBy(String::length, Suppliers.ofLinkedHashMap())
                .toMap(Map.Entry::getKey, Map.Entry::getValue, Suppliers.ofLinkedHashMap());
        List<Integer> keys = new ArrayList<>(groups.keySet());
        assertEquals(Arrays.asList(3, 1, 2), keys);
    }

    @Test
    public void testGroupByWithValueMapper() throws Exception {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            String getName() {
                return name;
            }

            int getAge() {
                return age;
            }
        }

        Seq<Person, Exception> seq = Seq.of(new Person("Alice", 25), new Person("Bob", 30), new Person("Charlie", 25), new Person("David", 30));

        Map<Integer, List<String>> groups = seq.groupBy(Person::getAge, Person::getName).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList("Alice", "Charlie"), groups.get(25));
        assertEquals(Arrays.asList("Bob", "David"), groups.get(30));
    }

    @Test
    public void testGroupByWithValueMapperAndMapFactory() throws Exception {
        class Product {
            int id;
            String category;

            Product(int id, String category) {
                this.id = id;
                this.category = category;
            }

            int getId() {
                return id;
            }

            String getCategory() {
                return category;
            }
        }

        Seq<Product, Exception> seq = Seq.of(new Product(1, "Electronics"), new Product(2, "Books"), new Product(3, "Electronics"), new Product(4, "Books"));

        List<Map.Entry<String, List<Integer>>> result = seq.groupBy(Product::getCategory, Product::getId, TreeMap::new).toList();

        assertEquals("Books", result.get(0).getKey());
        assertEquals(Arrays.asList(2, 4), result.get(0).getValue());
        assertEquals("Electronics", result.get(1).getKey());
        assertEquals(Arrays.asList(1, 3), result.get(1).getValue());
    }

    @Test
    public void testGroupByWithMergeFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 4, 7, 2, 5, 8, 3, 6, 9);
        Map<Integer, Integer> sums = seq.groupBy(n -> n % 3, n -> n, Integer::sum).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(18, sums.get(0).intValue());
        assertEquals(12, sums.get(1).intValue());
        assertEquals(15, sums.get(2).intValue());
    }

    @Test
    public void testGroupByWithMergeFunctionAndMapFactory() throws Exception {
        class Sale {
            String dept;
            int amount;

            Sale(String dept, int amount) {
                this.dept = dept;
                this.amount = amount;
            }

            String getDept() {
                return dept;
            }

            int getAmount() {
                return amount;
            }
        }

        Seq<Sale, Exception> seq = Seq.of(new Sale("Sales", 100), new Sale("IT", 150), new Sale("Sales", 200), new Sale("IT", 120));

        List<Map.Entry<String, Integer>> result = seq.groupBy(Sale::getDept, Sale::getAmount, Integer::max, TreeMap::new).toList();

        assertEquals("IT", result.get(0).getKey());
        assertEquals(150, result.get(0).getValue().intValue());
        assertEquals("Sales", result.get(1).getKey());
        assertEquals(200, result.get(1).getValue().intValue());
    }

    @Test
    public void testGroupByWithDownstreamCollector() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e");
        Map<Integer, Long> counts = seq.groupBy(String::length, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, counts.get(1).longValue());
        assertEquals(2L, counts.get(2).longValue());
        assertEquals(1L, counts.get(3).longValue());

        class Employee {
            String dept;
            double salary;

            Employee(String dept, double salary) {
                this.dept = dept;
                this.salary = salary;
            }

            String getDept() {
                return dept;
            }

            double getSalary() {
                return salary;
            }
        }

        Seq<Employee, Exception> empSeq = Seq.of(new Employee("IT", 50000), new Employee("Sales", 40000), new Employee("IT", 60000),
                new Employee("Sales", 45000));

        Map<String, Double> avgSalaries = empSeq.groupBy(Employee::getDept, Collectors.averagingDouble(Employee::getSalary))
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(55000.0, avgSalaries.get("IT"), 0.01);
        assertEquals(42500.0, avgSalaries.get("Sales"), 0.01);
    }

    @Test
    public void testGroupByWithDownstreamCollectorAndMapFactory() throws Exception {
        Seq<String, Exception> seq = Seq.of("apple", "apricot", "banana", "berry");
        List<Map.Entry<Character, String>> result = seq.groupBy(s -> s.charAt(0), Collectors.joining(", "), TreeMap::new).toList();

        assertEquals('a', result.get(0).getKey().charValue());
        assertEquals("apple, apricot", result.get(0).getValue());
        assertEquals('b', result.get(1).getKey().charValue());
        assertEquals("banana, berry", result.get(1).getValue());
    }

    @Test
    public void testGroupByWithValueMapperAndDownstreamCollector() throws Exception {
        class Order {
            String customer;
            String product;

            Order(String customer, String product) {
                this.customer = customer;
                this.product = product;
            }

            String getCustomer() {
                return customer;
            }

            String getProduct() {
                return product;
            }
        }

        Seq<Order, Exception> seq = Seq.of(new Order("Alice", "Book"), new Order("Bob", "Pen"), new Order("Alice", "Notebook"), new Order("Bob", "Pencil"));

        Map<String, String> result = seq.groupBy(Order::getCustomer, Order::getProduct, Collectors.joining(", ")).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals("Book, Notebook", result.get("Alice"));
        assertEquals("Pen, Pencil", result.get("Bob"));
    }

    @Test
    public void testGroupByWithValueMapperDownstreamCollectorAndMapFactory() throws Exception {
        class Transaction {
            String type;
            int amount;

            Transaction(String type, int amount) {
                this.type = type;
                this.amount = amount;
            }

            String getType() {
                return type;
            }

            int getAmount() {
                return amount;
            }
        }

        Seq<Transaction, Exception> seq = Seq.of(new Transaction("Credit", 100), new Transaction("Debit", 50), new Transaction("Credit", 200),
                new Transaction("Debit", 75));

        List<Map.Entry<String, Long>> result = seq
                .groupBy(Transaction::getType, Transaction::getAmount, Collectors.summingLong(Integer::longValue), LinkedHashMap::new)
                .toList();

        assertEquals("Credit", result.get(0).getKey());
        assertEquals(300L, result.get(0).getValue().longValue());
        assertEquals("Debit", result.get(1).getKey());
        assertEquals(125L, result.get(1).getValue().longValue());
    }

    @Test
    public void testPartitionBy() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, List<Integer>> partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList(2, 4, 6), partitions.get(true));
        assertEquals(Arrays.asList(1, 3, 5), partitions.get(false));

        seq = Seq.of(2, 4, 6);
        partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList(2, 4, 6), partitions.get(true));
        assertTrue(partitions.get(false).isEmpty());

        seq = Seq.of(1, 3, 5);
        partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertTrue(partitions.get(true).isEmpty());
        assertEquals(Arrays.asList(1, 3, 5), partitions.get(false));

        seq = Seq.<Integer, Exception> empty();
        partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertTrue(partitions.get(true).isEmpty());
        assertTrue(partitions.get(false).isEmpty());
    }

    @Test
    public void testPartitionByWithDownstreamCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, Long> counts = seq.partitionBy(n -> n > 3, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(3L, counts.get(true).longValue());
        assertEquals(3L, counts.get(false).longValue());

        Seq<String, Exception> strSeq = Seq.of("", "hello", "", "world", "");
        Map<Boolean, String> joined = strSeq.partitionBy(String::isEmpty, Collectors.joining(", ")).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(", , ", joined.get(true));
        assertEquals("hello, world", joined.get(false));
    }

    @Test
    public void testCountBy() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e", "fff");
        Map<Integer, Integer> counts = seq.countBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2, counts.get(1).intValue());
        assertEquals(2, counts.get(2).intValue());
        assertEquals(2, counts.get(3).intValue());

        seq = Seq.<String, Exception> empty();
        assertTrue(seq.countBy(String::length).toList().isEmpty());

        seq = Seq.of("apple", "apricot", "banana", "berry", "cherry");
        counts = seq.countBy(s -> (int) s.charAt(0)).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2, counts.get((int) 'a').intValue());
        assertEquals(2, counts.get((int) 'b').intValue());
        assertEquals(1, counts.get((int) 'c').intValue());
    }

    @Test
    public void testCountByWithMapFactory() throws Exception {
        Seq<String, Exception> seq = Seq.of("z", "a", "z", "b", "a", "a");
        List<Map.Entry<Character, Integer>> result = seq.countBy(s -> s.charAt(0), TreeMap::new).toList();

        assertEquals('a', result.get(0).getKey().charValue());
        assertEquals(3, result.get(0).getValue().intValue());
        assertEquals('b', result.get(1).getKey().charValue());
        assertEquals(1, result.get(1).getValue().intValue());
        assertEquals('z', result.get(2).getKey().charValue());
        assertEquals(2, result.get(2).getValue().intValue());
    }

    @Test
    public void testIntersection() throws Exception {
        {
            Seq<Integer, Exception> seq = Seq.of(1, 1, 2, 3);
            List<Integer> list = Arrays.asList(1, 2, 2, 4);
            List<Integer> result = seq.intersection(list).toList();
            assertEquals(Arrays.asList(1, 2), result);

            seq = Seq.of(1, 2, 3);
            list = Arrays.asList(4, 5, 6);
            result = seq.intersection(list).toList();
            assertTrue(result.isEmpty());

            seq = Seq.of(1, 2, 3);
            result = seq.intersection(Collections.emptyList()).toList();
            assertTrue(result.isEmpty());

            seq = Seq.<Integer, Exception> empty();
            result = seq.intersection(Arrays.asList(1, 2, 3)).toList();
            assertTrue(result.isEmpty());
        }

        {
            Seq<String, Exception> seq = Seq.of("a", "a", "b");
            Set<String> set = new HashSet<>(Arrays.asList("a", "c"));
            List<String> strResult = seq.intersection(set).toList();
            assertEquals(Arrays.asList("a"), strResult);
        }
    }

    @Test
    public void testIntersectionWithMapper() throws Exception {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            String getName() {
                return name;
            }
        }

        Seq<Person, Exception> seq = Seq.of(new Person("Alice", 25), new Person("Bob", 30), new Person("Alice", 35));
        List<String> names = Arrays.asList("Alice", "Charlie");
        List<Person> result = seq.intersection(Person::getName, names).toList();
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
        assertEquals(25, result.get(0).age);

        class Product {
            int id;
            String name;

            Product(int id, String name) {
                this.id = id;
                this.name = name;
            }

            int getId() {
                return id;
            }
        }

        Seq<Product, Exception> products = Seq.of(new Product(1, "A"), new Product(2, "B"), new Product(3, "C"));
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 3, 4));
        List<Product> productResult = products.intersection(Product::getId, ids).toList();
        assertEquals(2, productResult.size());
        assertEquals(1, productResult.get(0).getId());
        assertEquals(3, productResult.get(1).getId());
    }

    @Test
    public void testDifference() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 1, 2, 3);
        List<Integer> list = Arrays.asList(1, 4);
        List<Integer> result = seq.difference(list).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<String, Exception> strSeq = Seq.of("apple", "orange");
        List<String> list2 = Arrays.asList("apple", "apple", "orange");
        List<String> strResult = strSeq.difference(list2).toList();
        assertTrue(strResult.isEmpty());

        seq = Seq.of(1, 2, 3);
        list = Arrays.asList(4, 5, 6);
        result = seq.difference(list).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        seq = Seq.of(1, 2, 3);
        result = seq.difference(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.difference(Arrays.asList(1, 2, 3)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDifferenceWithMapper() throws Exception {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            String getName() {
                return name;
            }
        }

        Seq<Person, Exception> seq = Seq.of(new Person("Alice", 25), new Person("Alice", 30), new Person("Bob", 35));
        List<String> names = Arrays.asList("Alice", "Charlie");
        List<Person> result = seq.difference(Person::getName, names).toList();
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).getName());
        assertEquals(30, result.get(0).age);
        assertEquals("Bob", result.get(1).getName());

        class Transaction {
            int id;

            Transaction(int id) {
                this.id = id;
            }

            int getId() {
                return id;
            }
        }

        Seq<Transaction, Exception> trans = Seq.of(new Transaction(101), new Transaction(102));
        List<Integer> ids = Arrays.asList(101, 101, 102);
        List<Transaction> transResult = trans.difference(Transaction::getId, ids).toList();
        assertTrue(transResult.isEmpty());
    }

    @Test
    public void testSymmetricDifference() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 1, 2, 3);
        List<Integer> list = Arrays.asList(1, 2, 2, 4);
        List<Integer> result = seq.symmetricDifference(list).toList();
        assertEquals(Arrays.asList(1, 3, 2, 4), result);

        seq = Seq.of(1, 2, 3);
        list = Arrays.asList(4, 5, 6);
        result = seq.symmetricDifference(list).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);

        seq = Seq.of(1, 2, 3);
        list = Arrays.asList(1, 2, 3);
        result = seq.symmetricDifference(list).toList();
        assertTrue(result.isEmpty());

        seq = Seq.of(1, 2, 3);
        result = seq.symmetricDifference(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.symmetricDifference(Arrays.asList(1, 2, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrependArray() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(3, 4, 5);
        List<Integer> result = seq.prepend(1, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.prepend(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        seq = Seq.of(1, 2, 3);
        result = seq.prepend(new Integer[0]).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrependCollection() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(3, 4, 5);
        List<Integer> toPrepend = Arrays.asList(1, 2);
        List<Integer> result = seq.prepend(toPrepend).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        seq = Seq.of(1, 2, 3);
        result = seq.prepend(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrependSeq() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("world");
        Seq<String, Exception> seq2 = Seq.of("hello", " ");
        List<String> result = seq1.prepend(seq2).toList();
        assertEquals(Arrays.asList("hello", " ", "world"), result);

        seq1 = Seq.of("a", "b");
        seq2 = Seq.<String, Exception> empty();
        result = seq1.prepend(seq2).toList();
        assertEquals(Arrays.asList("a", "b"), result);
    }

    @Test
    public void testPrependOptional() throws Exception {
        Seq<String, Exception> seq = Seq.of("world");
        Optional<String> maybeHello = Optional.of("hello");
        List<String> result = seq.prepend(maybeHello).toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        Optional<String> empty = Optional.empty();
        result = Seq.of("world").prepend(empty).toList();
        assertEquals(Arrays.asList("world"), result);

        seq = Seq.<String, Exception> empty();
        result = seq.prepend(maybeHello).toList();
        assertEquals(Arrays.asList("hello"), result);
    }

    @Test
    public void testAppendArray() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2);
        List<Integer> result = seq.append(3, 4, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.append(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        seq = Seq.of(1, 2, 3);
        result = seq.append(new Integer[0]).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAppendCollection() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b");
        List<String> toAppend = Arrays.asList("c", "d");
        List<String> result = seq.append(toAppend).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        seq = Seq.of("x", "y");
        result = seq.append(Collections.emptyList()).toList();
        assertEquals(Arrays.asList("x", "y"), result);
    }

    @Test
    public void testAppendSeq() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 2);
        Seq<Integer, Exception> seq2 = Seq.of(3, 4);
        List<Integer> result = seq1.append(seq2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);

        seq1 = Seq.of(1, 2);
        seq2 = Seq.<Integer, Exception> empty();
        result = seq1.append(seq2).toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testAppendOptional() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello");
        Optional<String> maybeWorld = Optional.of("world");
        List<String> result = seq.append(maybeWorld).toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        Optional<String> empty = Optional.empty();
        result = Seq.of("hello").append(empty).toList();
        assertEquals(Arrays.asList("hello"), result);

        seq = Seq.<String, Exception> empty();
        result = seq.append(maybeWorld).toList();
        assertEquals(Arrays.asList("world"), result);
    }

    @Test
    public void testAppendIfEmptyArray() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty();
        List<Integer> result = emptySeq.appendIfEmpty(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<Integer, Exception> nonEmptySeq = Seq.of(4, 5);
        result = nonEmptySeq.appendIfEmpty(1, 2, 3).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testAppendIfEmptyCollection() throws Exception {
        Seq<String, Exception> emptySeq = Seq.<String, Exception> empty();
        List<String> defaults = Arrays.asList("default1", "default2");
        List<String> result = emptySeq.appendIfEmpty(defaults).toList();
        assertEquals(Arrays.asList("default1", "default2"), result);

        Seq<String, Exception> nonEmptySeq = Seq.of("value");
        result = nonEmptySeq.appendIfEmpty(defaults).toList();
        assertEquals(Arrays.asList("value"), result);

        emptySeq = Seq.<String, Exception> empty();
        result = emptySeq.appendIfEmpty(Collections.emptyList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAppendIfEmptySupplier() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty();
        Supplier<Seq<Integer, Exception>> defaultSupplier = () -> Seq.of(1, 2, 3);
        List<Integer> result = emptySeq.appendIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<Integer, Exception> nonEmptySeq = Seq.of(4, 5);
        result = nonEmptySeq.appendIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(4, 5), result);

        emptySeq = Seq.<Integer, Exception> empty();
        result = emptySeq.appendIfEmpty(() -> Seq.<Integer, Exception> empty()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDefaultIfEmptyValue() throws Exception {
        Seq<String, Exception> emptySeq = Seq.<String, Exception> empty();
        List<String> result = emptySeq.defaultIfEmpty("default").toList();
        assertEquals(Arrays.asList("default"), result);

        Seq<String, Exception> nonEmptySeq = Seq.of("value");
        result = nonEmptySeq.defaultIfEmpty("default").toList();
        assertEquals(Arrays.asList("value"), result);
    }

    @Test
    public void testDefaultIfEmptySupplier() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty();
        Supplier<Seq<Integer, Exception>> defaultSupplier = () -> Seq.of(1, 2, 3);
        List<Integer> result = emptySeq.defaultIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<Integer, Exception> nonEmptySeq = Seq.of(4, 5);
        result = nonEmptySeq.defaultIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testThrowIfEmpty() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.throwIfEmpty().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<Integer, Exception> seq2 = Seq.<Integer, Exception> empty();
        assertThrows(NoSuchElementException.class, () -> seq2.throwIfEmpty().toList());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b");
        List<String> result = seq.throwIfEmpty(() -> new IllegalStateException("Sequence must not be empty")).toList();
        assertEquals(Arrays.asList("a", "b"), result);

        Seq<String, Exception> seq2 = Seq.<String, Exception> empty();
        assertThrows(IllegalStateException.class, () -> seq2.throwIfEmpty(() -> new IllegalStateException("Sequence must not be empty")).toList());
    }

    @Test
    public void testIfEmpty() throws Exception {
        List<String> messages = new ArrayList<>();
        Seq<String, Exception> seq = Seq.<String, Exception> empty();
        seq.ifEmpty(() -> messages.add("Sequence is empty")).forEach(s -> messages.add(s));
        assertEquals(Arrays.asList("Sequence is empty"), messages);

        messages.clear();
        seq = Seq.of("a", "b");
        seq.ifEmpty(() -> messages.add("Sequence is empty")).forEach(s -> messages.add(s));
        assertEquals(Arrays.asList("a", "b"), messages);
    }

    @Test
    public void testOnEach() throws Exception {
        List<Integer> sideEffect = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.onEach(sideEffect::add).filter(n -> n % 2 == 0).toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), sideEffect);

        sideEffect.clear();
        seq = Seq.<Integer, Exception> empty();
        result = seq.onEach(sideEffect::add).toList();
        assertTrue(result.isEmpty());
        assertTrue(sideEffect.isEmpty());
    }

    @Test
    public void testOnFirst() throws Exception {
        List<String> messages = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("first", "second", "third");
        seq.onFirst(s -> messages.add("Processing first: " + s)).forEach(s -> messages.add(s));
        assertEquals(Arrays.asList("Processing first: first", "first", "second", "third"), messages);

        messages.clear();
        seq = Seq.of("only");
        seq.onFirst(s -> messages.add("First: " + s)).forEach(s -> messages.add(s));
        assertEquals(Arrays.asList("First: only", "only"), messages);

        messages.clear();
        seq = Seq.<String, Exception> empty();
        seq.onFirst(s -> messages.add("First: " + s)).forEach(s -> messages.add(s));
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testOnLast() throws Exception {
        List<String> messages = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("first", "second", "last");
        seq.onLast(s -> messages.add("Processing last: " + s)).forEach(s -> messages.add(s));
        assertEquals(Arrays.asList("first", "second", "Processing last: last", "last"), messages);

        messages.clear();
        seq = Seq.of("only");
        seq.onLast(s -> messages.add("Last: " + s)).forEach(s -> messages.add(s));
        assertEquals(Arrays.asList("Last: only", "only"), messages);

        messages.clear();
        seq = Seq.<String, Exception> empty();
        seq.onLast(s -> messages.add("Last: " + s)).forEach(s -> messages.add(s));
        assertTrue(messages.isEmpty());
    }

    @Test
    public void testPeek() throws Exception {
        List<Integer> sideEffect = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.peek(n -> sideEffect.add(n)).filter(n -> n % 2 == 0).toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), sideEffect);
    }

    @Test
    public void testPeekFirst() throws Exception {
        List<String> messages = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("first", "second", "third");
        List<String> result = seq.peekFirst(s -> messages.add("First element: " + s)).map(String::toUpperCase).toList();
        assertEquals(Arrays.asList("FIRST", "SECOND", "THIRD"), result);
        assertEquals(Arrays.asList("First element: first"), messages);
    }

    @Test
    public void testPeekLast() throws Exception {
        List<String> messages = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("first", "second", "last");
        List<String> result = seq.peekLast(s -> messages.add("Last element: " + s)).map(String::toUpperCase).toList();
        assertEquals(Arrays.asList("FIRST", "SECOND", "LAST"), result);
        assertEquals(Arrays.asList("Last element: last"), messages);
    }

    @Test
    public void testPeekIfWithPredicate() throws Exception {
        List<Integer> evenNumbers = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.peekIf(n -> n % 2 == 0, evenNumbers::add).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(2, 4), evenNumbers);

        List<Integer> largeNumbers = new ArrayList<>();
        seq = Seq.of(1, 2, 3);
        result = seq.peekIf(n -> n > 10, largeNumbers::add).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(largeNumbers.isEmpty());
    }

    @Test
    public void testPeekIfWithBiPredicate() throws Exception {
        List<String> evenPositions = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("a", "b", "c", "d", "e");
        List<String> result = seq.peekIf((s, index) -> index % 2 == 0, evenPositions::add).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
        assertEquals(Arrays.asList("b", "d"), evenPositions);

        List<String> firstThree = new ArrayList<>();
        seq = Seq.of("1", "2", "3", "4", "5");
        result = seq.peekIf((s, index) -> index <= 3, firstThree::add).toList();
        assertEquals(Arrays.asList("1", "2", "3", "4", "5"), result);
        assertEquals(Arrays.asList("1", "2", "3"), firstThree);
    }

    @Test
    public void testSplitBySize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = seq.split(3).toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2, 3), chunks.get(0));
        assertEquals(Arrays.asList(4, 5, 6), chunks.get(1));
        assertEquals(Arrays.asList(7), chunks.get(2));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        chunks = seq.split(2).toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5, 6), chunks.get(2));

        seq = Seq.of(1, 2);
        chunks = seq.split(5).toList();
        assertEquals(1, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));

        seq = Seq.<Integer, Exception> empty();
        chunks = seq.split(3).toList();
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitBySizeWithCollectionSupplier() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b", "c", "d", "e");
        List<Set<String>> chunks = seq.split(2, IntFunctions.ofSet()).toList();
        assertEquals(3, chunks.size());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), chunks.get(0));
        assertEquals(new HashSet<>(Arrays.asList("c", "d")), chunks.get(1));
        assertEquals(new HashSet<>(Arrays.asList("e")), chunks.get(2));

        seq = Seq.of("1", "2", "3", "4");
        List<ArrayList<String>> arrayLists = seq.split(2, n -> new ArrayList<>(n)).toList();
        assertEquals(2, arrayLists.size());
        assertEquals(Arrays.asList("1", "2"), arrayLists.get(0));
        assertEquals(Arrays.asList("3", "4"), arrayLists.get(1));
    }

    @Test
    public void testSplitBySizeWithCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<String> joined = seq.split(2, com.landawn.abacus.util.stream.Collectors.joining(",")).toList();
        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), joined);

        seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<Integer> sums = seq.split(3, Collectors.summingInt(Integer::intValue)).toList();
        assertEquals(Arrays.asList(6, 15, 24), sums);

        seq = Seq.of(10, 20, 30, 40, 50);
        List<Double> averages = seq.split(2, Collectors.averagingInt(Integer::intValue)).toList();
        assertEquals(15.0, averages.get(0), 0.01);
        assertEquals(35.0, averages.get(1), 0.01);
        assertEquals(50.0, averages.get(2), 0.01);
    }

    @Test
    public void testSplitByPredicate() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 3, 5, 2, 4, 6, 7, 9);
        List<List<Integer>> groups = seq.split(n -> n % 2 == 0).toList();
        assertEquals(3, groups.size());
        assertEquals(Arrays.asList(1, 3, 5), groups.get(0));
        assertEquals(Arrays.asList(2, 4, 6), groups.get(1));
        assertEquals(Arrays.asList(7, 9), groups.get(2));

        Seq<String, Exception> strSeq = Seq.of("a", "ab", "abc", "d", "de");
        List<List<String>> strGroups = strSeq.split(s -> s.length() > 1).toList();
        assertEquals(4, strGroups.size());
        assertEquals(Arrays.asList("a"), strGroups.get(0));
        assertEquals(Arrays.asList("ab", "abc"), strGroups.get(1));
        assertEquals(Arrays.asList("d"), strGroups.get(2));
        assertEquals(Arrays.asList("de"), strGroups.get(3));

        seq = Seq.of(2, 4, 6, 8);
        groups = seq.split(n -> n % 2 == 0).toList();
        assertEquals(1, groups.size());
        assertEquals(Arrays.asList(2, 4, 6, 8), groups.get(0));

        seq = Seq.<Integer, Exception> empty();
        groups = seq.split(n -> n > 0).toList();
        assertTrue(groups.isEmpty());
    }

    @Test
    public void testSplitByPredicateWithCollectionSupplier() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "ab", "abc", "d", "de");
        List<Set<String>> groups = seq.split(s -> s.length() > 1, Suppliers.ofSet()).toList();
        assertEquals(4, groups.size());
        assertEquals(new HashSet<>(Arrays.asList("a")), groups.get(0));
        assertEquals(new HashSet<>(Arrays.asList("ab", "abc")), groups.get(1));

        Seq<Integer, Exception> intSeq = Seq.of(1, 2, 4, 8, 3, 5, 7);
        List<LinkedList<Integer>> linkedGroups = intSeq.split(n -> n % 2 == 0, LinkedList::new).toList();
        assertEquals(3, linkedGroups.size());
    }

    @Test
    public void testSplitByPredicateWithCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 3, 5, 2, 4, 6, 7, 9);
        List<String> joined = seq.split(n -> n % 2 == 0, com.landawn.abacus.util.stream.Collectors.joining(",")).toList();
        assertEquals(Arrays.asList("1,3,5", "2,4,6", "7,9"), joined);

        Seq<String, Exception> strSeq = Seq.of("a", "b", "cc", "dd", "e", "f");
        List<Long> counts = strSeq.split(s -> s.length() > 1, Collectors.counting()).toList();
        assertEquals(Arrays.asList(2L, 2L, 2L), counts);

        seq = Seq.of(1, 3, 2, 8, 6, 4, 5, 7);
        List<Optional<Integer>> maxValues = seq.split(n -> n > 4, com.landawn.abacus.util.stream.Collectors.max()).toList();
        assertEquals(3, maxValues.get(0).get().intValue());
        assertEquals(8, maxValues.get(1).get().intValue());
        assertEquals(4, maxValues.get(2).get().intValue());
        assertEquals(7, maxValues.get(3).get().intValue());
    }

    @Test
    public void testSplitAtPosition() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Seq<Integer, Exception>> split = seq.splitAt(3).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertEquals(Arrays.asList(4, 5), split.get(1).toList());

        seq = Seq.of(1, 2, 3, 4, 5);
        split = seq.splitAt(3).skip(1).toList();
        assertEquals(1, split.size());
        assertEquals(Arrays.asList(4, 5), split.get(0).toList());

        seq = Seq.of(1, 2, 3);
        split = seq.splitAt(0).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2, 3), split.get(1).toList());

        seq = Seq.of(1, 2, 3);
        split = seq.splitAt(3).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertTrue(split.get(1).toList().isEmpty());

        seq = Seq.of(1, 2);
        split = seq.splitAt(5).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2), split.get(0).toList());
        assertTrue(split.get(1).toList().isEmpty());

        seq = Seq.<Integer, Exception> empty();
        split = seq.splitAt(2).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertTrue(split.get(1).toList().isEmpty());
    }

    @Test
    public void testSplitAtPredicate() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Seq<Integer, Exception>> split = seq.splitAt(n -> n > 3).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertEquals(Arrays.asList(4, 5), split.get(1).toList());

        seq = Seq.of(1, 2, 3, 4, 5);
        split = seq.splitAt(n -> n > 3).skip(1).toList();
        assertEquals(1, split.size());
        assertEquals(Arrays.asList(4, 5), split.get(0).toList());

        seq = Seq.of(1, 2, 3);
        split = seq.splitAt(n -> n > 10).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertTrue(split.get(1).toList().isEmpty());

        seq = Seq.of(5, 1, 2, 3);
        split = seq.splitAt(n -> n > 3).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(5, 1, 2, 3), split.get(1).toList());

        seq = Seq.of(1, 2, 3, 5);
        split = seq.splitAt(n -> n > 4).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertEquals(Arrays.asList(5), split.get(1).toList());

        seq = Seq.<Integer, Exception> empty();
        split = seq.splitAt(n -> n > 0).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertTrue(split.get(1).toList().isEmpty());
    }
}
