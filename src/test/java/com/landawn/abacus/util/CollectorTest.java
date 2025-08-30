/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Tuple.Tuple7;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.stream.Stream;

public class CollectorTest extends AbstractTest {

    @Test
    public void test_combine() {
        List<Collector<? super String, ?, ?>> downstreams = N.asList(Collectors.summingInt(String::length), Collectors.averagingDoubleOrEmpty(String::length),
                Collectors.toList(), Collectors.toSet(), Collectors.minMax(), Collectors.commonPrefix(), Collectors.counting());

        var result = Stream.range(100, 200)
                .map(String::valueOf)
                .collect(MoreCollectors.combine(downstreams, a -> Tuple.of((Integer) a[0], (OptionalDouble) a[1], (List<String>) a[2], (Set<String>) a[3],
                        (Optional<Pair<String, String>>) a[4], (String) a[5], (Long) a[6])));

        Tuple7<Integer, OptionalDouble, List<String>, Set<String>, Optional<Pair<String, String>>, String, Long> result2 = result;
        result2.forEach(Fn.println());

        Stream.range(100, 200) //
                .map(String::valueOf)
                .collect(MoreCollectors.summingBigDecimal(it -> BigDecimal.valueOf(it.length()), it -> BigDecimal.valueOf(it.length() + 1)))
                .forEach(Fn.println());

    }
}
