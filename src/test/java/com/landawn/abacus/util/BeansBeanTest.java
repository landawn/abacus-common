package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

public class BeansBeanTest extends BeansTestSupport {

    @Test
    public void testBeanToMap() {
        Map<String, Object> map = Beans.beanToMap(simpleBean);
        assertEquals("John", map.get("name"));
        assertEquals(25, map.get("age"));
        assertEquals(true, map.get("active"));
        assertTrue(Beans.beanToMap((Object) null).isEmpty());

        CollectionBean bean = new CollectionBean();
        bean.setItems(Arrays.asList("item1", "item2"));
        @SuppressWarnings("unchecked")
        List<String> mappedItems = (List<String>) Beans.beanToMap(bean).get("items");
        assertEquals(2, mappedItems.size());
    }

    @Test
    public void testBeanToMap_MapSupplier() {
        Map<String, Object> linked = Beans.beanToMap(simpleBean, IntFunctions.ofLinkedHashMap());
        assertTrue(linked instanceof LinkedHashMap);
        assertEquals("John", linked.get("name"));

        TreeMap<String, Object> tree = Beans.beanToMap(simpleBean, IntFunctions.ofTreeMap());
        assertTrue(tree instanceof TreeMap);
        assertEquals("John", tree.get("name"));

        assertTrue(Beans.beanToMap((Object) null, IntFunctions.ofLinkedHashMap()).isEmpty());
    }

    @Test
    public void testBeanToMap_SelectPropNames() {
        Map<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));

        map = Beans.beanToMap(simpleBean, Arrays.asList("name", "active"));
        assertEquals(2, map.size());

        map = Beans.beanToMap(simpleBean, (Collection<String>) null);
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));
        assertTrue(map.containsKey("active"));

        assertTrue(Beans.beanToMap(simpleBean, Collections.emptyList()).isEmpty());
        assertTrue(Beans.beanToMap(simpleBean, Collections.emptyList(), IntFunctions.ofLinkedHashMap()).isEmpty());
        assertTrue(Beans.beanToMap(simpleBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap()).isEmpty());

        TreeMap<String, Object> tree = Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), IntFunctions.ofTreeMap());
        assertEquals(2, tree.size());
        assertFalse(tree.containsKey("active"));

        map = Beans.beanToMap(simpleBean, (Collection<String>) null, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_SelectPropNamesAndNaming() {
        Map<String, Object> map = Beans.beanToMap(simpleBean, Arrays.asList("name"), NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofLinkedHashMap());
        assertTrue(map.containsKey("NAME"));
        assertEquals(1, map.size());

        TreeMap<String, Object> tree = Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), NamingPolicy.NO_CHANGE, IntFunctions.ofTreeMap());
        assertEquals(2, tree.size());
        assertTrue(tree.containsKey("name"));

        map = Beans.beanToMap(simpleBean, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("age"));

        map = Beans.beanToMap(simpleBean, null, NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("name"));
        assertTrue(map.containsKey("active"));
    }

    @Test
    public void testBeanToMap_Output() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, output);
        assertEquals("John", output.get("name"));
        assertEquals(25, output.get("age"));
        assertEquals(true, output.get("active"));

        output.clear();
        Beans.beanToMap(simpleBean, Arrays.asList("name"), output);
        assertEquals(1, output.size());
        assertEquals("John", output.get("name"));

        output.clear();
        output.put("existing", "value");
        Beans.beanToMap(simpleBean, Collections.emptyList(), output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));

        Beans.beanToMap(simpleBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, output);
        assertEquals(1, output.size());

        output.clear();
        Beans.beanToMap(simpleBean, Arrays.asList("name", "age"), NamingPolicy.SCREAMING_SNAKE_CASE, output);
        assertTrue(output.containsKey("NAME"));
        assertEquals("John", output.get("NAME"));

        output.clear();
        Beans.beanToMap(null, Arrays.asList("name"), NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());

        assertThrows(IllegalArgumentException.class,
                () -> Beans.beanToMap(simpleBean, Arrays.asList("nonExistentProp"), NamingPolicy.NO_CHANGE, new HashMap<>()));
    }

    @Test
    public void testBeanToMap_IgnoreNull() {
        simpleBean.setActive(null);
        Map<String, Object> map = Beans.beanToMap(simpleBean, true);
        assertFalse(map.containsKey("active"));
        assertTrue(map.containsKey("name"));

        map = Beans.beanToMap(simpleBean, false);
        assertTrue(map.containsKey("active"));
        assertNull(map.get("active"));

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, true, output);
        assertFalse(output.containsKey("active"));

        output.clear();
        Beans.beanToMap(simpleBean, false, output);
        assertTrue(output.containsKey("active"));
        assertNull(output.get("active"));
    }

    @Test
    public void testBeanToMap_IgnoredPropNames() {
        Set<String> ignored = new HashSet<>();
        ignored.add("age");
        simpleBean.setActive(null);

        Map<String, Object> map = Beans.beanToMap(simpleBean, true, ignored);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));

        map = Beans.beanToMap(simpleBean, false, ignored, IntFunctions.ofTreeMap());
        assertTrue(map instanceof TreeMap);
        assertFalse(map.containsKey("age"));
        assertTrue(map.containsKey("active"));

        map = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE);
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));

        map = Beans.beanToMap(simpleBean, true, ignored, NamingPolicy.SNAKE_CASE);
        assertFalse(map.containsKey("active"));

        map = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("name"));
        assertFalse(map.containsKey("age"));

        TreeMap<String, Object> tree = Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofTreeMap());
        assertFalse(tree.containsKey("age"));

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToMap(simpleBean, false, ignored, output);
        assertFalse(output.containsKey("age"));

        output.clear();
        Beans.beanToMap(simpleBean, true, ignored, NamingPolicy.SNAKE_CASE, output);
        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));

        output.clear();
        Beans.beanToMap(simpleBean, false, ignored, NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("name"));
        assertFalse(output.containsKey("age"));

        output.clear();
        SimpleBean bean = new SimpleBean("Alice", 30);
        bean.setActive(null);
        Beans.beanToMap(bean, true, null, NamingPolicy.CAMEL_CASE, output);
        assertEquals("Alice", output.get("name"));
        assertFalse(output.containsKey("active"));

        output.clear();
        Beans.beanToMap(null, false, null, NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());

        assertTrue(Beans.beanToMap((Object) null, true, null, NamingPolicy.NO_CHANGE).isEmpty());
    }

    @Test
    public void testBeanToMap_IgnoredPropNamesLargerThanBeanProps() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(Boolean.TRUE);
        Set<String> ignored = new HashSet<>(Arrays.asList("name", "age", "active", "x1", "x2", "x3", "x4"));

        Map<String, Object> map = assertDoesNotThrow(() -> Beans.beanToMap(bean, false, ignored));
        assertTrue(map.isEmpty());
        assertTrue(assertDoesNotThrow(() -> Beans.deepBeanToMap(bean, false, ignored)).isEmpty());
        assertTrue(assertDoesNotThrow(() -> Beans.beanToFlatMap(bean, false, ignored)).isEmpty());
    }

    @Test
    public void testBeanToFlatMap() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertEquals("John", map.get("simpleBean.name"));
        assertEquals(25, map.get("simpleBean.age"));
        assertEquals("New York", map.get("address.city"));
        assertEquals("5th Avenue", map.get("address.street"));
        assertTrue(Beans.beanToFlatMap((Object) null).isEmpty());
    }

    @Test
    public void testBeanToFlatMap_MapSupplier() {
        TreeMap<String, Object> tree = Beans.beanToFlatMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(tree instanceof TreeMap);
        assertEquals("123", tree.get("id"));
        assertEquals("John", tree.get("simpleBean.name"));
        assertTrue(Beans.beanToFlatMap((Object) null, IntFunctions.ofLinkedHashMap()).isEmpty());
    }

    @Test
    public void testBeanToFlatMap_SelectPropNames() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"));
        assertEquals("123", map.get("id"));
        assertTrue(map.containsKey("address.city"));
        assertFalse(map.containsKey("simpleBean.name"));

        map = Beans.beanToFlatMap(nestedBean, (Collection<String>) null);
        assertTrue(map.containsKey("simpleBean.name"));

        assertTrue(Beans.beanToFlatMap(nestedBean, Collections.emptyList()).isEmpty());
        assertTrue(Beans.beanToFlatMap(nestedBean, Collections.emptyList(), IntFunctions.ofLinkedHashMap()).isEmpty());
        assertTrue(Beans.beanToFlatMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap()).isEmpty());

        TreeMap<String, Object> tree = Beans.beanToFlatMap(nestedBean, Arrays.asList("address"), IntFunctions.ofTreeMap());
        assertTrue(tree.containsKey("address.city"));
        assertFalse(tree.containsKey("simpleBean.name"));
    }

    @Test
    public void testBeanToFlatMap_SelectPropNamesAndNaming() {
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("simple_bean.name"));

        map = Beans.beanToFlatMap(nestedBean, Arrays.asList("simpleBean"), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("simple_bean.name"));

        map = Beans.beanToFlatMap(nestedBean, null, NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("simpleBean.name"));
    }

    @Test
    public void testBeanToFlatMap_Output() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, output);
        assertEquals("123", output.get("id"));
        assertEquals("John", output.get("simpleBean.name"));

        output.clear();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("id"), output);
        assertTrue(output.containsKey("id"));

        output.clear();
        output.put("existing", "value");
        Beans.beanToFlatMap(nestedBean, Collections.emptyList(), output);
        assertEquals(1, output.size());
        Beans.beanToFlatMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, output);
        assertEquals(1, output.size());

        output.clear();
        Beans.beanToFlatMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("address.city"));

        output.clear();
        Beans.beanToFlatMap(null, Arrays.asList("name"), NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testBeanToFlatMap_IgnoreNull() {
        nestedBean.getAddress().setZipCode(null);
        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true);
        assertFalse(map.containsKey("address.zipCode"));
        assertTrue(map.containsKey("address.city"));

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, true, output);
        assertFalse(output.containsKey("address.zipCode"));

        output.clear();
        Beans.beanToFlatMap(nestedBean, false, output);
        assertTrue(output.containsKey("address.zipCode"));
    }

    @Test
    public void testBeanToFlatMap_IgnoredPropNames() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.beanToFlatMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simpleBean.name"));

        map = Beans.beanToFlatMap(nestedBean, false, ignored, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));

        map = Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name"));

        map = Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean.name"));

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.beanToFlatMap(nestedBean, false, ignored, output);
        assertFalse(output.containsKey("id"));

        output.clear();
        Beans.beanToFlatMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, output);
        assertFalse(output.containsKey("id"));

        nestedBean.getAddress().setZipCode(null);
        ignored = new HashSet<>();
        ignored.add("tags");
        map = Beans.beanToFlatMap(nestedBean, true, ignored, NamingPolicy.NO_CHANGE);
        assertFalse(map.containsKey("tags"));
        assertFalse(map.containsKey("address.zipCode"));
        assertTrue(map.containsKey("address.city"));

        ignored = new HashSet<>(Arrays.asList("age"));
        SimpleBean bean = new SimpleBean("Frank", 50);
        output.clear();
        Beans.beanToFlatMap(bean, true, ignored, NamingPolicy.CAMEL_CASE, output);
        assertEquals("Frank", output.get("name"));
        assertFalse(output.containsKey("active"));
        assertFalse(output.containsKey("age"));

        assertTrue(Beans.beanToFlatMap((Object) null, true, null, NamingPolicy.NO_CHANGE).isEmpty());
    }
}
