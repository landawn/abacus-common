package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BeansMapTest extends BeansTestSupport {

    @Test
    public void testMapToBean() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane");
        map.put("age", 30);
        map.put("active", false);

        SimpleBean bean = Beans.mapToBean(map, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(30, bean.getAge());
        assertEquals(false, bean.getActive());

        assertNull(Beans.mapToBean((Map<String, Object>) null, SimpleBean.class));

        bean = Beans.mapToBean(new HashMap<>(), SimpleBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testMapToBean_IgnoreUnmatched() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Jane");
        map.put("age", null);
        map.put("unknownField", "value");

        SimpleBean bean = Beans.mapToBean(map, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertNull(bean.getActive());

        map.put("age", 25);
        bean = Beans.mapToBean(map, true, SimpleBean.class);
        assertEquals("Jane", bean.getName());
        assertEquals(25, bean.getAge());
    }

    @Test
    public void testMapToBean_SelectPropNames() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Nora");
        map.put("age", 31);
        map.put("active", true);

        SimpleBean bean = Beans.mapToBean(map, Arrays.asList("name"), SimpleBean.class);
        assertEquals("Nora", bean.getName());
        assertEquals(0, bean.getAge());

        bean = Beans.mapToBean(map, Arrays.asList("name", "age"), SimpleBean.class);
        assertEquals("Nora", bean.getName());
        assertEquals(31, bean.getAge());
        assertNull(bean.getActive());

        bean = Beans.mapToBean(map, (Collection<String>) null, SimpleBean.class);
        assertEquals("Nora", bean.getName());
        assertEquals(31, bean.getAge());
        assertEquals(true, bean.getActive());

        bean = Beans.mapToBean(map, Collections.emptyList(), SimpleBean.class);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());

        assertNull(Beans.mapToBean((Map<String, Object>) null, Arrays.asList("name"), SimpleBean.class));
    }

    @Test
    public void testMapToBean_NestedMap() {
        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("city", "Boston");
        addressMap.put("street", "Elm St");

        Map<String, Object> map = new HashMap<>();
        map.put("id", "456");
        map.put("address", addressMap);

        NestedBean bean = Beans.mapToBean(map, Arrays.asList("id", "address"), NestedBean.class);
        assertEquals("456", bean.getId());
        assertNotNull(bean.getAddress());
        assertEquals("Boston", bean.getAddress().getCity());
    }

    @Test
    public void testMapToBean_DottedKeys() {
        Map<String, Object> flat = new LinkedHashMap<>();
        flat.put("id", "789");
        flat.put("address.city", "Boston");
        flat.put("address.zipCode", "02101");
        flat.put("simpleBean.name", "Ann");
        flat.put("simpleBean.age", 40);

        NestedBean bean = Beans.mapToBean(flat, NestedBean.class);
        assertEquals("789", bean.getId());
        assertEquals("Boston", bean.getAddress().getCity());
        assertEquals("02101", bean.getAddress().getZipCode());
        assertEquals("Ann", bean.getSimpleBean().getName());
        assertEquals(40, bean.getSimpleBean().getAge());
    }

    @Test
    public void testMapToBean_RoundTripFromFlatMap() {
        Map<String, Object> flat = Beans.beanToFlatMap(nestedBean);
        assertEquals("New York", flat.get("address.city"));
        assertEquals("John", flat.get("simpleBean.name"));
        assertTrue(!(flat.get("address") instanceof Map));

        NestedBean restored = Beans.mapToBean(flat, NestedBean.class);
        assertEquals("123", restored.getId());
        assertEquals("John", restored.getSimpleBean().getName());
        assertEquals(25, restored.getSimpleBean().getAge());
        assertEquals("New York", restored.getAddress().getCity());
        assertEquals("5th Avenue", restored.getAddress().getStreet());
    }

    @Test
    public void testMapToBean_SelectionIsStrictOnlyAtTopLevel() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("city", "NYC");
        nested.put("bogus", 1);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("address", nested);

        BeansUserFixture user = Beans.mapToBean(map, CommonUtil.asList("address"), BeansUserFixture.class);
        assertNotNull(user.getAddress());
        assertEquals("NYC", user.getAddress().getCity());

        Map<String, Object> topLevel = new LinkedHashMap<>();
        topLevel.put("bogus", 1);
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(topLevel, CommonUtil.asList("bogus"), BeansUserFixture.class));

        Map<String, Object> dotted = new LinkedHashMap<>();
        dotted.put("address.bogus", 1);
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(dotted, CommonUtil.asList("address.bogus"), BeansUserFixture.class));

        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(map, false, BeansUserFixture.class));
        assertEquals("NYC", Beans.mapToBean(map, true, BeansUserFixture.class).getAddress().getCity());
    }

    @Test
    public void testMapsToBeans() {
        List<Map<String, Object>> mapList = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", 25);
        mapList.add(map1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Jane");
        map2.put("age", 30);
        mapList.add(map2);

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals("Jane", beans.get(1).getName());

        beans = Beans.mapsToBeans(mapList, Arrays.asList("name"), SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());

        assertTrue(Beans.mapsToBeans(Collections.emptyList(), SimpleBean.class).isEmpty());
    }

    @Test
    public void testMapsToBeans_IgnoreUnmatched() {
        List<Map<String, Object>> mapList = new ArrayList<>();
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", null);
        mapList.add(map1);
        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", null);
        map2.put("age", 25);
        mapList.add(map2);

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, true, SimpleBean.class);
        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());
        assertNull(beans.get(1).getName());
        assertEquals(25, beans.get(1).getAge());
    }

    @Test
    public void testMapsToBeans_SelectPropNames() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Owen");
        map.put("age", 42);
        map.put("active", false);
        List<Map<String, Object>> mapList = Collections.singletonList(map);

        List<SimpleBean> beans = Beans.mapsToBeans(mapList, (Collection<String>) null, SimpleBean.class);
        assertEquals(1, beans.size());
        assertEquals("Owen", beans.get(0).getName());
        assertEquals(42, beans.get(0).getAge());
        assertEquals(false, beans.get(0).getActive());

        beans = Beans.mapsToBeans(mapList, Collections.emptyList(), SimpleBean.class);
        assertEquals(1, beans.size());
        assertNull(beans.get(0).getName());
        assertEquals(0, beans.get(0).getAge());

        beans = Beans.mapsToBeans(mapList, Arrays.asList("name", "age"), SimpleBean.class);
        assertEquals("Owen", beans.get(0).getName());
        assertEquals(42, beans.get(0).getAge());
        assertNull(beans.get(0).getActive());
    }

    /**
     * {@code mapToBean(Map, Collection, Class)} validates every selected name whether or not the map carries
     * it, so a name the target bean has no setter for is rejected with {@code IllegalArgumentException}. The
     * bulk {@code mapsToBeans} form returned early on empty input before reaching that check, so the same bad
     * selection was accepted or rejected depending only on how many maps were supplied - while both overloads
     * document the identical {@code @throws}.
     */
    @Test
    public void test_mapsToBeans_validatesSelectionOnEmptyInput_regression_20260918() {
        final List<String> bogus = Arrays.asList("noSuchPropertyOnSimpleBean");

        // Reference behaviour: with one map the bad selection is rejected.
        final List<Map<String, Object>> oneMap = new ArrayList<>();
        oneMap.add(new HashMap<>(Collections.singletonMap("name", "x")));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(oneMap, bogus, SimpleBean.class));

        // Before the fix these returned an empty list instead of throwing.
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(Collections.emptyList(), bogus, SimpleBean.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapsToBeans(null, bogus, SimpleBean.class));

        // A valid selection is still accepted on empty input, and still yields an empty list.
        assertTrue(Beans.mapsToBeans(Collections.emptyList(), Arrays.asList("name"), SimpleBean.class).isEmpty());
        assertTrue(Beans.mapsToBeans(null, Arrays.asList("name"), SimpleBean.class).isEmpty());

        // A null selection means "all properties" and has nothing to validate.
        assertTrue(Beans.mapsToBeans(Collections.emptyList(), null, SimpleBean.class).isEmpty());

        // An empty selection has nothing to validate either.
        assertTrue(Beans.mapsToBeans(Collections.emptyList(), Collections.emptyList(), SimpleBean.class).isEmpty());
    }
}
