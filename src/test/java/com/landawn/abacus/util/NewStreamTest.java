package com.landawn.abacus.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.Stream;

public class NewStreamTest {

    @Test
    public void test() {
        N.println(Stream.of(1, 2, 3).groupTo(Fn.<Integer> identity(), Suppliers.<Integer, List<Integer>> ofLinkedHashMap()));

        Stream.of(1, 2, 3).filter(it -> it > 4).acceptIfNotEmpty(s -> s.forEach(Fn.println())).orElse(() -> N.println("hello"));

        Stream.of(1, 2, 3).applyIfNotEmpty(s -> s.allMatch(i -> i > 0)).ifPresent(Fn.println());

    }

}
