package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Lists;
import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Difference.BeanDifference;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class MapsTest extends AbstractTest {

    @Test
    public void test_beanToMap() {

        Account account1 = Beans.fill(Account.class);
        Map<String, Object> map1 = Beans.beanToMap(account1);
        N.println(map1);

        Beans.beanToMap(account1, new HashMap<String, Object>());
        Map<String, Object> map2 = Beans.beanToMap(account1, IntFunctions.ofMap());
        N.println(map2);
    }

    @Test
    public void test_MapDifference() {
        Account account1 = Beans.fill(Account.class);
        Account account2 = Beans.fill(Account.class);
        account2.setFirstName(account1.getFirstName());

        var mapDiff = BeanDifference.of(account1, account2);

        N.println(mapDiff);
        N.println(mapDiff.common());
        N.println(mapDiff.leftOnly());
        N.println(mapDiff.rightOnly());
        N.println(mapDiff.differentValues());

        mapDiff = BeanDifference.of(account1, account2, CommonUtil.asList("id", "firstName", "lastName2"));

        N.println(mapDiff);
        N.println(mapDiff.common());
        N.println(mapDiff.leftOnly());
        N.println(mapDiff.rightOnly());
        N.println(mapDiff.differentValues());

    }

    @Test
    public void test_getByPath() {
        Map map = CommonUtil.asMap("key1", "val1");
        assertEquals("val1", Maps.getByPath(map, "key1"));

        map = CommonUtil.asMap("key1", CommonUtil.asList("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = CommonUtil.asMap("key1", CommonUtil.asSet("val1"));
        assertEquals("val1", Maps.getByPath(map, "key1[0]"));

        map = CommonUtil.asMap("key1", CommonUtil.asList(CommonUtil.asLinkedHashSet("val1", "val2")));
        assertEquals("val2", Maps.getByPath(map, "key1[0][1]"));

        map = CommonUtil.asMap("key1", CommonUtil.asSet(CommonUtil.asList(CommonUtil.asSet("val1"))));
        assertEquals("val1", Maps.getByPath(map, "key1[0][0][0]"));

        map = CommonUtil.asMap("key1", CommonUtil.asList(CommonUtil.asLinkedHashSet("val1", CommonUtil.asMap("key2", "val22"))));
        assertEquals("val22", Maps.getByPath(map, "key1[0][1].key2"));

        map = CommonUtil.asMap("key1",
                CommonUtil.asList(CommonUtil.asLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.asList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertEquals("val33", Maps.getByPath(map, "key1[0][1].key2[1].key3"));

        map = CommonUtil.asMap("key1",
                CommonUtil.asList(CommonUtil.asLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.asList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][2].key2[1].key3"));

        map = CommonUtil.asMap("key1",
                CommonUtil.asList(CommonUtil.asLinkedHashSet("val1", CommonUtil.asMap("key2", CommonUtil.asList("val22", CommonUtil.asMap("key3", "val33"))))));
        assertNull(Maps.getByPath(map, "key1[0][1].key22[1].key3"));

        map = CommonUtil.asMap("key1", CommonUtil.asMap("key2", null));
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
        Map<String, Object> map = CommonUtil.asMap("a", CommonUtil.asMap("b", CommonUtil.asMap("c", CommonUtil.asMap("d", 4), "c2", 3), "b2", 2), "a2", 1);
        N.println(map);

        Map<String, Object> result = Maps.flatten(map);
        N.println(result);

        Map<String, Object> map2 = Maps.unflatten(result);
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_difference() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);
        Map<String, Integer> map2 = CommonUtil.asMap("a", 1, "b", 3);

        N.println(Maps.intersection(map, map2));
        assertEquals(CommonUtil.asMap("a", 1), Maps.intersection(map, map2));

        N.println(Maps.difference(map, map2));
        assertEquals(CommonUtil.asMap("b", Pair.of(2, Nullable.of(3)), "c", Pair.of(3, Nullable.empty())), Maps.difference(map, map2));

        N.println(Maps.symmetricDifference(map, map2));

        assertEquals(CommonUtil.asMap("b", Pair.of(Nullable.of(2), Nullable.of(3)), "c", Pair.of(Nullable.of(3), Nullable.empty())),
                Maps.symmetricDifference(map, map2));

    }

    @Test
    public void test_getOrDefault() {
        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3);

        assertEquals(1, Maps.getOrDefaultIfAbsent(map, "a", 0).intValue());
        assertEquals(0, Maps.getOrDefaultIfAbsent(map, "d", 0).intValue());

        assertEquals(CommonUtil.asList(1, 0), Maps.getValuesOrDefault(map, CommonUtil.asList("a", "d"), 0));

        assertEquals(CommonUtil.asList(1), Maps.getValuesIfPresent(map, CommonUtil.asList("a", "d")));
    }
}
