/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

public class MapsTest extends AbstractTest {

    @Test
    public void test_bean2Map() {

        Account account1 = Beans.fill(Account.class);
        Map<String, Object> map1 = Beans.bean2Map(account1);
        N.println(map1);

        Beans.bean2Map(account1, new HashMap<String, Object>());
        Map<String, Object> map2 = Beans.bean2Map(account1, IntFunctions.ofMap());
        N.println(map2);
    }

    @Test
    public void test_MapDifference() {
        Account account1 = Beans.fill(Account.class);
        Account account2 = Beans.fill(Account.class);
        account2.setFirstName(account1.getFirstName());

        var mapDiff = BeanDifference.of(account1, account2);

        N.println(mapDiff);
        N.println(mapDiff.inCommon());
        N.println(mapDiff.onLeftOnly());
        N.println(mapDiff.onRightOnly());
        N.println(mapDiff.withDifferentValues());

        mapDiff = BeanDifference.of(account1, account2, N.asList("id", "firstName", "lastName2"));

        N.println(mapDiff);
        N.println(mapDiff.inCommon());
        N.println(mapDiff.onLeftOnly());
        N.println(mapDiff.onRightOnly());
        N.println(mapDiff.withDifferentValues());

    }

    @Test
    public void test_getByPath() {
        Map map = N.asMap("key1", "val1");
        assertEquals("val1", Maps.getByPath(map, "key1"));

        map = N.asMap("key1", N.asList("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = N.asMap("key1", N.asSet("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", "val2")));
        assertEquals("val2", Maps.getByPath(map, "key1[0][1]"));

        map = N.asMap("key1", N.asSet(N.asList(N.asSet("val1"))));
        assertEquals("val1", Maps.getByPath(map, "key1[0][0][0]"));

        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", "val22"))));
        assertEquals("val22", Maps.getByPath(map, "key1[0][1].key2"));

        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", N.asList("val22", N.asMap("key3", "val33"))))));
        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));

        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", N.asList("val22", N.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));

        map = N.asMap("key1", N.asList(N.asLinkedHashSet("val1", N.asMap("key2", N.asList("val22", N.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][1].key22[1].key3"));

        map = N.asMap("key1", N.asMap("key2", null));
        assertNull(Maps.getByPath(map, "key1.key2.key3"));
    }

    @Test
    public void test_flatToMap() {
        Map<String, List<Object>> map = new HashMap<>(
                ImmutableMap.of("a", Lists.newArrayList(1, 2, 3), "b", Lists.newArrayList(4, 5, 6), "c", Lists.newArrayList(7, 8)));

        N.println(map);

        int maxValueSize = Stream.ofValues(map).mapToInt(List::size).max().orElseZero();

        List<Map<String, Object>> list = IntStream.range(0, maxValueSize)
                .mapToObj(it -> Stream.of(map).filter(e -> e.getValue().size() > it).toMap(Entry::getKey, e -> e.getValue().get(it)))
                .toList();

        N.println(list);

        list = Maps.flatToMap(map);
        N.println(list);

        list = Stream.just(map).map(Fn.<String, Object> flatmapValue()).first().orElseThrow();
        N.println(list);
    }

    @Test
    public void test_flatten() {
        Map<String, Object> map = N.asMap("a", N.asMap("b", N.asMap("c", N.asMap("d", 4), "c2", 3), "b2", 2), "a2", 1);
        N.println(map);

        Map<String, Object> result = Maps.flatten(map);
        N.println(result);

        Map<String, Object> map2 = Maps.unflatten(result);
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_difference() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = N.asMap("a", 1, "b", 3);

        N.println(Maps.intersection(map, map2));
        assertEquals(N.asMap("a", 1), Maps.intersection(map, map2));

        N.println(Maps.difference(map, map2));
        assertEquals(N.asMap("b", Pair.of(2, Nullable.of(3)), "c", Pair.of(3, Nullable.empty())), Maps.difference(map, map2));

        N.println(Maps.symmetricDifference(map, map2));

        assertEquals(N.asMap("b", Pair.of(Nullable.of(2), Nullable.of(3)), "c", Pair.of(Nullable.of(3), Nullable.empty())),
                Maps.symmetricDifference(map, map2));

    }

    @Test
    public void test_getOrDefault() {
        Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);

        assertEquals(1, Maps.getOrDefaultIfAbsent(map, "a", 0).intValue());
        assertEquals(0, Maps.getOrDefaultIfAbsent(map, "d", 0).intValue());

        assertEquals(N.asList(1, 0), Maps.getOrDefaultIfAbsentForEach(map, N.asList("a", "d"), 0));

        assertEquals(N.asList(1), Maps.getIfPresentForEach(map, N.asList("a", "d")));
    }
}
