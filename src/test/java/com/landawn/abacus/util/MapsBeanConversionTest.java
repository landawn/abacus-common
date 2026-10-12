package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class MapsBeanConversionTest extends MapsTestSupport {

    @Test
    public void testBean2FlatMap() {
        Address address = new Address("LA", "90001");
        Person person = new Person("Jane", 25, address);
        Map<String, Object> flatMap = Beans.beanToFlatMap(person);

        assertEquals("Jane", flatMap.get("name"));
        assertEquals(25, flatMap.get("age"));
        assertEquals("LA", flatMap.get("address.city"));
        assertEquals("90001", flatMap.get("address.zip"));
    }

    @Test
    public void testBean2Map() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Map<String, Object> map = Beans.beanToMap(bean);
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertEquals(true, map.get("active"));
    }

    @Test
    public void testBean2Map_simple() {
        SimpleBean bean = new SimpleBean(1, "test");
        Map<String, Object> map = Beans.beanToMap(bean);
        assertEquals(Map.of("id", 1, "value", "test"), map);
    }

    @Test
    public void testBean2MapIgnoreNull() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setNullableField(null);

        Map<String, Object> map1 = Beans.beanToMap(bean, true);
        assertFalse(map1.containsKey("nullableField"));

        Map<String, Object> map2 = Beans.beanToMap(bean, false);
        assertTrue(map2.containsKey("nullableField"));
        assertNull(map2.get("nullableField"));
    }

    @Test
    public void testBean2MapWithComplexTypes() {
        ComplexBean bean = new ComplexBean();
        bean.setDate(new Date());
        bean.setBigDecimal(new BigDecimal("123.456"));
        bean.setStringList(Arrays.asList("a", "b", "c"));
        bean.setIntArray(new int[] { 1, 2, 3 });

        Map<String, Object> map = Beans.beanToMap(bean);
        assertNotNull(map.get("date"));
        assertEquals(bean.getBigDecimal(), map.get("bigDecimal"));
        assertEquals(bean.getStringList(), map.get("stringList"));
        assertArrayEquals(bean.getIntArray(), (int[]) map.get("intArray"));
    }

    @Test
    public void testBean2MapWithIgnoredProps() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Set<String> ignored = new HashSet<>(Arrays.asList("age", "active"));
        Map<String, Object> map = Beans.beanToMap(bean, false, ignored);

        assertEquals(3, map.size());
        assertEquals("John", map.get("name"));
        assertFalse(map.containsKey("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void testBean2MapWithMapSupplier() {
        TestBean bean = new TestBean();
        bean.setName("John");

        Map<String, Object> map = Beans.beanToMap(bean, IntFunctions.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals("John", map.get("name"));
    }

    @Test
    public void testBean2MapWithNamingPolicy() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);

        Map<String, Object> map = Beans.beanToMap(bean, null, NamingPolicy.SCREAMING_SNAKE_CASE, IntFunctions.ofMap());
        assertEquals("John", map.get("NAME"));
        assertEquals(30, map.get("AGE"));
    }

    @Test
    public void testBean2MapWithSelectProps() {
        TestBean bean = new TestBean();
        bean.setName("John");
        bean.setAge(30);
        bean.setActive(true);

        Map<String, Object> map = Beans.beanToMap(bean, Arrays.asList("name", "age"));
        assertEquals(2, map.size());
        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertFalse(map.containsKey("active"));
    }

    @Test
    public void test_beanToMap() {
        Account account1 = Beans.newRandomBean(Account.class);
        Map<String, Object> map1 = Beans.beanToMap(account1);
        assertFalse(map1.isEmpty());

        Map<String, Object> output = new HashMap<>();
        Beans.beanToMap(account1, output);
        assertEquals(map1, output);

        Map<String, Object> map2 = Beans.beanToMap(account1, IntFunctions.ofMap());
        assertEquals(map1, map2);
    }

    @Test
    public void testCircularReferenceHandling() {
        CircularBean bean1 = new CircularBean();
        CircularBean bean2 = new CircularBean();
        bean1.setName("Bean1");
        bean2.setName("Bean2");
        bean1.setReference(bean2);
        bean2.setReference(bean1);

        Map<String, Object> map = Beans.beanToMap(bean1);
        assertEquals("Bean1", map.get("name"));
        assertEquals(bean2, map.get("reference"));

        // A reference cycle is rejected rather than silently truncated (and never blows the stack).
        assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(bean1));
        assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(bean1));
    }

    @Test
    public void testDeepBean2Map() {
        Address address = new Address("NY", "10001");
        Person person = new Person("John", 30, address);
        Map<String, Object> map = Beans.deepBeanToMap(person);

        assertEquals("John", map.get("name"));
        assertEquals(30, map.get("age"));
        assertTrue(map.get("address") instanceof Map);
        Map<?, ?> addrMap = (Map<?, ?>) map.get("address");
        assertEquals("NY", addrMap.get("city"));
        assertEquals("10001", addrMap.get("zip"));
    }

    @Test
    public void testMap2Bean() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Beans.mapToBean(map, TestBean.class);
        assertNotNull(bean);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertTrue(bean.isActive());

        assertNull(Beans.mapToBean((Map) null, TestBean.class));
    }

    @Test
    public void testMap2Bean_simple() {
        Map<String, Object> map = Map.of("id", 10, "value", "hello", "extra", "ignored");
        SimpleBean bean = Beans.mapToBean(map, SimpleBean.class);
        assertEquals(10, bean.id);
        assertEquals("hello", bean.value);

        Map<String, Object> mapNoMatch = Map.of("id", 20, "value", "world", "extraField", "data");
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(mapNoMatch, false, SimpleBean.class));

    }

    @Test
    public void testMap2BeanCollection() {
        Map<String, Object> map1 = new HashMap<>();
        map1.put("name", "John");
        map1.put("age", 30);

        Map<String, Object> map2 = new HashMap<>();
        map2.put("name", "Jane");
        map2.put("age", 25);

        List<Map<String, Object>> maps = Arrays.asList(map1, map2);
        List<TestBean> beans = Beans.mapsToBeans(maps, TestBean.class);

        assertEquals(2, beans.size());
        assertEquals("John", beans.get(0).getName());
        assertEquals(30, beans.get(0).getAge());
        assertEquals("Jane", beans.get(1).getName());
        assertEquals(25, beans.get(1).getAge());

        List<TestBean> beans2 = Beans.mapsToBeans(maps, CommonUtil.toList("name"), TestBean.class);

        assertEquals(2, beans2.size());
        assertEquals("John", beans2.get(0).getName());
        assertEquals(0, beans2.get(0).getAge());
        assertEquals("Jane", beans2.get(1).getName());
        assertEquals(0, beans2.get(1).getAge());
    }

    @Test
    public void testMap2BeanWithFlags() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", null);
        map.put("unknownProperty", "value");

        TestBean bean1 = Beans.mapToBean(map, true, TestBean.class);
        assertEquals("John", bean1.getName());
        assertEquals(0, bean1.getAge());

        try {
            Beans.mapToBean(map, false, TestBean.class);
            fail("Should throw exception for unmatched property");
        } catch (Exception e) {
        }
    }

    @Test
    public void testMap2BeanWithNestedMaps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "Parent");

        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("value", "NestedValue");
        map.put("nestedBean", nestedMap);

        TestBean bean = Beans.mapToBean(map, TestBean.class);
        assertEquals("Parent", bean.getName());
        assertNotNull(bean.getNestedBean());
        assertEquals("NestedValue", bean.getNestedBean().getValue());
    }

    @Test
    public void testMap2BeanWithSelectProps() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", "John");
        map.put("age", 30);
        map.put("active", true);

        TestBean bean = Beans.mapToBean(map, Arrays.asList("name", "age"), TestBean.class);
        assertEquals("John", bean.getName());
        assertEquals(30, bean.getAge());
        assertFalse(bean.isActive());
    }
}
