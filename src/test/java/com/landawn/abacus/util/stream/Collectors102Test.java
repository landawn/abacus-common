package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class Collectors102Test extends TestBase {

    private List<String> stringList;
    private List<Integer> integerList;
    private List<Double> doubleList;
    private List<Person> personList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
        private String department;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Employee {
        private String name;
        private List<String> skills;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Department {
        private String name;
        private List<String> skills;
    }

    @BeforeEach
    public void setUp() {
        stringList = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        integerList = Arrays.asList(1, 2, 3, 4, 5);
        doubleList = Arrays.asList(1.1, 2.2, 3.3, 4.4, 5.5);
        personList = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 35, "Sales"),
                new Person("David", 28, "Sales"), new Person("Eve", 32, "HR"));
    }

    @Test
    public void test_toCollection_with_atMost() {
        List<Integer> result = Stream.range(0, 30).parallel(3).collect(Collectors.toCollection(10, () -> new ArrayList<>()));

        assertNotNull(result);
        assertEquals(10, result.size());
    }

    @Test
    public void testSummingIntToLong() {
        Tuple3<Long, Long, Long> result = Stream.of("a", "bb", "ccc")
                .collect(MoreCollectors.summingIntToLong(e -> e.length(), e -> e.length() * 2, e -> e.length() * 3));

        Assertions.assertEquals(6L, result._1); // 1 + 2 + 3
        Assertions.assertEquals(12L, result._2);
        Assertions.assertEquals(18L, result._3);
    }
}
