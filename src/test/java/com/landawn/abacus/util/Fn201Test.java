package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Predicate;

public class Fn201Test extends TestBase {

    @Test
    public void and() {
        Predicate<Integer> p1 = i -> i > 0;
        Predicate<Integer> p2 = i -> i < 2;
        assertThat(Fn.and(p1, p2).test(1)).isTrue();
        assertThat(Fn.and(p1, p2).test(2)).isFalse();
    }

    @Test
    public void or() {
        Predicate<Integer> p1 = i -> i > 2;
        Predicate<Integer> p2 = i -> i < 1;
        assertThat(Fn.or(p1, p2).test(3)).isTrue();
        assertThat(Fn.or(p1, p2).test(0)).isTrue();
        assertThat(Fn.or(p1, p2).test(1)).isFalse();
    }

    @Test
    public void testByKey() {
        Predicate<String> p = s -> s.startsWith("a");
        assertThat(Fn.<String, Integer> testByKey(p).test(Fn.<String, Integer> entry().apply("abc", 1))).isTrue();
    }

    @Test
    public void testByValue() {
        Predicate<Integer> p = i -> i > 0;
        assertThat(Fn.<String, Integer> testByValue(p).test(Fn.<String, Integer> entry().apply("a", 1))).isTrue();
    }

    @Test
    public void acceptByKey() {
        // This method is a consumer, so we can't directly assert on its output.
        // We can verify that it doesn't throw an exception.
        Fn.<String, Integer> acceptByKey(System.out::println).accept(Fn.<String, Integer> entry().apply("abc", 1));
    }

    @Test
    public void acceptByValue() {
        Fn.<String, Integer> acceptByValue(System.out::println).accept(Fn.<String, Integer> entry().apply("abc", 1));
    }

    @Test
    public void applyByKey() {
        Function<String, Integer> f = String::length;
        assertThat(Fn.<String, Integer, Integer> applyByKey(f).apply(Fn.<String, Integer> entry().apply("abc", 1))).isEqualTo(3);
    }

    @Test
    public void applyByValue() {
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.<String, Integer, Integer> applyByValue(f).apply(Fn.<String, Integer> entry().apply("abc", 1))).isEqualTo(2);
    }

    @Test
    public void mapKey() {
        Function<String, String> f = String::toUpperCase;
        assertThat(Fn.<String, Integer, String> mapKey(f).apply(Fn.<String, Integer> entry().apply("abc", 1)).getKey()).isEqualTo("ABC");
    }

    @Test
    public void mapValue() {
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.<String, Integer, Integer> mapValue(f).apply(Fn.<String, Integer> entry().apply("abc", 1)).getValue()).isEqualTo(2);
    }

    @Test
    public void testKeyVal() {
        BiPredicate<String, Integer> p = (s, i) -> s.length() == i;
        assertThat(Fn.testKeyVal(p).test(Fn.<String, Integer> entry().apply("abc", 1))).isFalse();
    }

    @Test
    public void acceptKeyVal() {
        Fn.<String, Integer> acceptKeyVal((s, i) -> {
        }).accept(Fn.<String, Integer> entry().apply("abc", 1));
    }

    @Test
    public void applyKeyVal() {
        assertThat(Fn.<String, Integer, String> applyKeyVal((s, i) -> s + i).apply(Fn.<String, Integer> entry().apply("abc", 1))).isEqualTo("abc1");
    }

    @Test
    public void acceptIfNotNull() {
        Fn.acceptIfNotNull(System.out::println).accept("a");
        Fn.acceptIfNotNull(System.out::println).accept(null);
    }

    @Test
    public void acceptIf() {
        Predicate<Integer> p = i -> i > 0;
        Fn.acceptIf(p, System.out::println).accept(1);
        Fn.acceptIf(p, System.out::println).accept(0);
    }

    @Test
    public void acceptIfOrElse() {
        Predicate<Integer> p = i -> i > 0;
        Fn.acceptIfOrElse(p, System.out::println, System.out::println).accept(1);
    }

    @Test
    public void applyIfNotNullOrEmpty() {
        Function<List<Integer>, List<Integer>> f = l -> {
            l.add(1);
            return l;
        };
        assertThat(Fn.applyIfNotNullOrEmpty(f).apply((List<Integer>) null)).isNotNull();
    }

    @Test
    public void applyIfNotNullOrDefault() {
        Function<String, Integer> f1 = String::length;
        Function<Integer, Integer> f2 = i -> i + 1;
        assertThat(Fn.applyIfNotNullOrDefault(f1, f2, 0).apply(null)).isZero();
        assertThat(Fn.applyIfNotNullOrDefault(f1, f2, 0).apply("a")).isEqualTo(2);
    }

    @Test
    public void applyIfNotNullOrElseGet() {
        Function<String, Integer> f1 = String::length;
        Function<Integer, Integer> f2 = i -> i + 1;
        assertThat(Fn.applyIfNotNullOrElseGet(f1, f2, () -> 0).apply(null)).isZero();
        assertThat(Fn.applyIfNotNullOrElseGet(f1, f2, () -> 0).apply("a")).isEqualTo(2);
    }

    @Test
    public void applyIfOrElseDefault() {
        Predicate<Integer> p = i -> i > 0;
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.applyIfOrElseDefault(p, f, 0).apply(1)).isEqualTo(2);
        assertThat(Fn.applyIfOrElseDefault(p, f, 0).apply(0)).isZero();
    }

    @Test
    public void applyIfOrElseGet() {
        Predicate<Integer> p = i -> i > 0;
        Function<Integer, Integer> f = i -> i + 1;
        assertThat(Fn.applyIfOrElseGet(p, f, () -> 0).apply(1)).isEqualTo(2);
        assertThat(Fn.applyIfOrElseGet(p, f, () -> 0).apply(0)).isZero();
    }

    @Test
    public void flatmapValue() {
        Map<String, Collection<Integer>> map = new HashMap<>();
        map.put("a", Arrays.asList(1, 2));
        map.put("b", Arrays.asList(3, 4));
        assertThat(Fn.<String, Integer> flatmapValue().apply(map)).hasSize(2);
    }

    @Test
    public void parseByte() {
        assertThat(Fn.parseByte().applyAsByte("1")).isEqualTo((byte) 1);
    }

    @Test
    public void parseShort() {
        assertThat(Fn.parseShort().applyAsShort("1")).isEqualTo((short) 1);
    }

    @Test
    public void parseInt() {
        assertThat(Fn.parseInt().applyAsInt("1")).isEqualTo(1);
    }

    @Test
    public void parseLong() {
        assertThat(Fn.parseLong().applyAsLong("1")).isEqualTo(1L);
    }

    @Test
    public void parseFloat() {
        assertThat(Fn.parseFloat().applyAsFloat("1.0")).isEqualTo(1.0f);
    }

    @Test
    public void parseDouble() {
        assertThat(Fn.parseDouble().applyAsDouble("1.0")).isEqualTo(1.0);
    }

    @Test
    public void createNumber() {
        assertThat(Fn.createNumber().apply("1")).isEqualTo(1);
    }

    @Test
    public void numToInt() {
        assertThat(Fn.numToInt().applyAsInt(1.0)).isEqualTo(1);
    }

    @Test
    public void numToLong() {
        assertThat(Fn.numToLong().applyAsLong(1.0)).isEqualTo(1L);
    }

    @Test
    public void numToDouble() {
        assertThat(Fn.numToDouble().applyAsDouble(1)).isEqualTo(1.0);
    }

    @Test
    public void atMost() {
        Predicate<Integer> p = Fn.atMost(1);
        assertThat(p.test(1)).isTrue();
        assertThat(p.test(1)).isFalse();
    }

    @Test
    public void limitThenFilter() {
        Predicate<Integer> p = Fn.limitThenFilter(1, i -> i > 0);
        assertThat(p.test(1)).isTrue();
        assertThat(p.test(1)).isFalse();
    }

    @Test
    public void filterThenLimit() {
        Predicate<Integer> p = Fn.filterThenLimit(i -> i > 0, 1);
        assertThat(p.test(1)).isTrue();
        assertThat(p.test(1)).isFalse();
    }

    @Test
    public void indexed() {
        assertThat(Fn.indexed().apply("a").index()).isZero();
    }

    @Test
    public void selectFirst() {
        assertThat(Fn.selectFirst().apply(1, 2)).isEqualTo(1);
    }

    @Test
    public void selectSecond() {
        assertThat(Fn.selectSecond().apply(1, 2)).isEqualTo(2);
    }

    @Test
    public void min() {
        assertThat(Fn.<Integer> min().apply(1, 2)).isEqualTo(1);
    }

    @Test
    public void max() {
        assertThat(Fn.<Integer> max().apply(1, 2)).isEqualTo(2);
    }
}
