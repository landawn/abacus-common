package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.stream.Stream;

public class ComparatorTest extends AbstractTest {

    @Test
    public void test_comparingMapByKey() {

        Map<String, Integer> map1 = N.asMap("a", 1, "c", 3);
        Map<String, Integer> map2 = N.asMap("b", 2, "d", 4);

        List<Map<String, Integer>> maps = N.asList(map1, map2);

        N.println(maps);
        N.sort(maps, Comparators.comparingMapByKey());
        N.println(maps);

        N.sort(maps, Comparators.comparingMapByKey().reversed());
        N.println(maps);

        N.sort(maps, Comparators.comparingMapByValue());
        N.println(maps);

        N.sort(maps, Comparators.comparingMapByValue().reversed());
        N.println(maps);

        N.sort(maps, Comparators.comparingMapByValue());
        N.println(maps);

        N.sort(maps, Comparators.comparingMapByValue(Comparators.reverseOrder()));
        N.println(maps);

        map1.entrySet().stream().sorted(Comparators.<String, Integer> comparingByKey().reversed()).forEach(Fn.println());
    }

    @Test
    public void test_01() {
        String[] a = { "a", "b" };
        Object[] b = { "a", "b", "c" };
        assertEquals(-1, Comparators.OBJECT_ARRAY_COMPARATOR.compare(a, b));
        assertEquals(1, Comparators.OBJECT_ARRAY_COMPARATOR.compare(b, a));

        Stream.of(N.asArray(a, b)).sorted(Comparators.OBJECT_ARRAY_COMPARATOR).forEach(Fn.println());
        Stream.of(N.asArray(b, a)).sorted(Comparators.OBJECT_ARRAY_COMPARATOR).forEach(Fn.println());
    }

    @Test
    public void test_02() {
        List<String> a = N.asList("a", "b");
        Collection<Object> b = N.asLinkedHashSet("a", "b", "c");
        assertEquals(-1, Comparators.COLLECTION_COMPARATOR.compare(a, b));
        assertEquals(1, Comparators.COLLECTION_COMPARATOR.compare(b, a));

        Stream.of(N.asArray(a, b)).sorted(Comparators.COLLECTION_COMPARATOR).forEach(Fn.println());
        Stream.of(N.asArray(b, a)).sorted(Comparators.COLLECTION_COMPARATOR).forEach(Fn.println());
    }

}
