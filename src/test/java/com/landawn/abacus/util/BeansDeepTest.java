package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

public class BeansDeepTest extends BeansTestSupport {

    @Test
    public void testDeepBeanToMap() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean);
        assertEquals("123", map.get("id"));
        assertTrue(map.get("simpleBean") instanceof Map);
        assertTrue(map.get("address") instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> simpleBeanMap = (Map<String, Object>) map.get("simpleBean");
        assertEquals("John", simpleBeanMap.get("name"));

        assertTrue(Beans.deepBeanToMap((Object) null).isEmpty());
    }

    @Test
    public void testDeepBeanToMap_MapSupplier() {
        TreeMap<String, Object> treeMap = Beans.deepBeanToMap(nestedBean, IntFunctions.ofTreeMap());
        assertTrue(treeMap instanceof TreeMap);
        assertEquals("123", treeMap.get("id"));
        assertTrue(treeMap.get("simpleBean") instanceof Map);
        assertTrue(Beans.deepBeanToMap((Object) null, IntFunctions.ofLinkedHashMap()).isEmpty());
    }

    @Test
    public void testDeepBeanToMap_SelectPropNames() {
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("address"));
        assertFalse(map.containsKey("simpleBean"));

        map = Beans.deepBeanToMap(nestedBean, (Collection<String>) null);
        assertTrue(map.containsKey("id"));
        assertTrue(map.get("simpleBean") instanceof Map);

        assertTrue(Beans.deepBeanToMap(nestedBean, Collections.emptyList()).isEmpty());
        assertTrue(Beans.deepBeanToMap(nestedBean, Collections.emptyList(), IntFunctions.ofLinkedHashMap()).isEmpty());
        assertTrue(Beans.deepBeanToMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap()).isEmpty());

        TreeMap<String, Object> tree = Beans.deepBeanToMap(nestedBean, Arrays.asList("id"), IntFunctions.ofTreeMap());
        assertEquals(1, tree.size());
        assertTrue(tree.containsKey("id"));
    }

    @Test
    public void testDeepBeanToMap_SelectPropNamesAndNaming() {
        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        snakeBean.setLastName("Doe");
        Map<String, Object> map = Beans.deepBeanToMap(snakeBean, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("first_name"));
        assertTrue(map.containsKey("last_name"));

        map = Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "simpleBean"), NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean"));

        map = Beans.deepBeanToMap(nestedBean, null, NamingPolicy.NO_CHANGE, IntFunctions.ofMap());
        assertTrue(map.containsKey("simpleBean"));
    }

    @Test
    public void testDeepBeanToMap_Output() {
        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, output);
        assertEquals("123", output.get("id"));
        assertTrue(output.get("simpleBean") instanceof Map);

        output.clear();
        Beans.deepBeanToMap(nestedBean, Arrays.asList("id"), output);
        assertEquals(1, output.size());

        output.clear();
        output.put("existing", "value");
        Beans.deepBeanToMap(nestedBean, Collections.emptyList(), output);
        assertEquals(1, output.size());
        assertEquals("value", output.get("existing"));

        Beans.deepBeanToMap(nestedBean, Collections.emptyList(), NamingPolicy.SNAKE_CASE, output);
        assertEquals(1, output.size());

        output.clear();
        Beans.deepBeanToMap(nestedBean, Arrays.asList("id", "address"), NamingPolicy.NO_CHANGE, output);
        assertTrue(output.containsKey("id"));
        assertTrue(output.containsKey("address"));

        output.clear();
        Beans.deepBeanToMap(null, Arrays.asList("name"), NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testDeepBeanToMap_IgnoreNull() {
        nestedBean.setSimpleBean(null);
        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true);
        assertFalse(map.containsKey("simpleBean"));
        assertTrue(map.containsKey("id"));

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, true, output);
        assertFalse(output.containsKey("simpleBean"));

        output.clear();
        Beans.deepBeanToMap(nestedBean, false, output);
        assertTrue(output.containsKey("simpleBean"));
        assertNull(output.get("simpleBean"));
    }

    @Test
    public void testDeepBeanToMap_IgnoredPropNames() {
        Set<String> ignored = new HashSet<>();
        ignored.add("id");

        Map<String, Object> map = Beans.deepBeanToMap(nestedBean, true, ignored);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("address"));

        map = Beans.deepBeanToMap(nestedBean, false, ignored, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));

        TreeMap<String, Object> tree = Beans.deepBeanToMap(nestedBean, false, ignored, IntFunctions.ofTreeMap());
        assertFalse(tree.containsKey("id"));

        map = Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE);
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean"));

        map = Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, IntFunctions.ofMap());
        assertFalse(map.containsKey("id"));
        assertTrue(map.containsKey("simple_bean"));

        Map<String, Object> output = new LinkedHashMap<>();
        Beans.deepBeanToMap(nestedBean, false, ignored, output);
        assertFalse(output.containsKey("id"));

        output.clear();
        Beans.deepBeanToMap(nestedBean, false, ignored, NamingPolicy.SNAKE_CASE, output);
        assertFalse(output.containsKey("id"));

        nestedBean.setSimpleBean(null);
        ignored = new HashSet<>();
        ignored.add("tags");
        map = Beans.deepBeanToMap(nestedBean, true, ignored, NamingPolicy.NO_CHANGE);
        assertFalse(map.containsKey("simpleBean"));
        assertFalse(map.containsKey("tags"));
        assertTrue(map.containsKey("id"));

        assertTrue(Beans.deepBeanToMap((Object) null, true, null, NamingPolicy.NO_CHANGE).isEmpty());
        output.clear();
        Beans.deepBeanToMap(null, false, null, NamingPolicy.CAMEL_CASE, output);
        assertTrue(output.isEmpty());
    }

    @Test
    public void testDeepCopy() {
        SimpleBean copy = Beans.deepCopy(simpleBean);
        assertNotSame(simpleBean, copy);
        assertEquals("John", copy.getName());
        assertEquals(25, copy.getAge());
        assertEquals(true, copy.getActive());
        copy.setName("Modified");
        assertEquals("John", simpleBean.getName());

        NestedBean nestedCopy = Beans.deepCopy(nestedBean);
        assertNotSame(nestedBean, nestedCopy);
        assertEquals("123", nestedCopy.getId());
        nestedCopy.getSimpleBean().setName("Modified");
        nestedCopy.getAddress().setCity("Boston");
        assertEquals("John", nestedBean.getSimpleBean().getName());
        assertEquals("New York", nestedBean.getAddress().getCity());

        assertNull(Beans.deepCopy(null));
    }

    @Test
    public void testDeepCopyAs() {
        SimpleBean copy = Beans.deepCopyAs(simpleBean, SimpleBean.class);
        assertNotSame(simpleBean, copy);
        assertEquals("John", copy.getName());
        assertEquals(25, copy.getAge());

        NestedBean nestedCopy = Beans.deepCopyAs(nestedBean, NestedBean.class);
        assertNotSame(nestedBean, nestedCopy);
        assertEquals("123", nestedCopy.getId());

        SimpleBean fromNull = Beans.deepCopyAs(null, SimpleBean.class);
        assertNotNull(fromNull);
        assertNull(fromNull.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.deepCopyAs(simpleBean, null));
    }
}
