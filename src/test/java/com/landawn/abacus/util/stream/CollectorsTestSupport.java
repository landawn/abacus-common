package com.landawn.abacus.util.stream;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Shared fixtures and lifecycle for the Collectors contract tests. */
public abstract class CollectorsTestSupport extends TestBase {

    protected List<String> stringList;

    protected List<Integer> integerList;

    protected List<Double> doubleList;

    protected List<Person> personList;

    protected List<Boolean> booleanList;

    protected List<Character> charList;

    protected List<Byte> byteList;

    protected List<Short> shortList;

    protected List<Long> longList;

    protected List<Float> floatList;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        private String name;
        private int age;
        private String department;
        private BigInteger bigIntValue;
        private BigDecimal bigDecValue;
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
        personList = Arrays.asList(new Person("Alice", 25, "Engineering", BigInteger.valueOf(100), BigDecimal.valueOf(100.5)),
                new Person("Bob", 30, "Engineering", BigInteger.valueOf(200), BigDecimal.valueOf(200.5)),
                new Person("Charlie", 35, "Sales", BigInteger.valueOf(300), BigDecimal.valueOf(300.5)),
                new Person("David", 28, "Sales", BigInteger.valueOf(400), BigDecimal.valueOf(400.5)),
                new Person("Eve", 32, "HR", BigInteger.valueOf(500), BigDecimal.valueOf(500.5)));
        booleanList = Arrays.asList(true, false, true, false, true);
        charList = Arrays.asList('a', 'b', 'c', 'd', 'e');
        byteList = Arrays.asList((byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5);
        shortList = Arrays.asList((short) 10, (short) 20, (short) 30, (short) 40, (short) 50);
        longList = Arrays.asList(100L, 200L, 300L, 400L, 500L);
        floatList = Arrays.asList(1.1f, 2.2f, 3.3f, 4.4f, 5.5f);
    }

    // ---- perf review 2026-09-26 G112 begin ----
    // The previous accumulator of commonPrefix()/commonSuffix(), rebuilt from the public Strings helpers: returns the finished result.
    protected static String g112CommonAffixReference(final List<CharSequence> elements, final boolean prefix) {
        CharSequence left = null;
        int length = -1;

        for (final CharSequence t : elements) {
            if (length == -1) {
                left = t;
                length = N.len(t);
            } else if (length > 0) {
                length = Math.min(length,
                        prefix ? com.landawn.abacus.util.Strings.lengthOfCommonPrefix(left, t) : com.landawn.abacus.util.Strings.lengthOfCommonSuffix(left, t));
                left = N.len(t) < N.len(left) ? t : left;
            }
        }

        if (left == null || length <= 0) {
            return "";
        }

        return prefix ? left.subSequence(0, length).toString() : left.subSequence(left.length() - length, left.length()).toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected static String g112CollectSplit(final Collector collector, final List<CharSequence> first, final List<CharSequence> second) {
        final Object a = collector.supplier().get();
        final Object b = collector.supplier().get();
        first.forEach(e -> collector.accumulator().accept(a, e));
        second.forEach(e -> collector.accumulator().accept(b, e));
        return (String) collector.finisher().apply(collector.combiner().apply(a, b));
    }

    protected static String g112RandomString(final java.util.Random random, final char[] alphabet, final int maxLength) {
        final int length = random.nextInt(maxLength + 1);
        final char[] chars = new char[length];

        for (int i = 0; i < length; i++) {
            chars[i] = alphabet[random.nextInt(alphabet.length)];
        }

        return new String(chars);
    }
}
