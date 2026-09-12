package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BeansSetTest extends BeansTestSupport {

    @Test
    public void testSetPropValue() throws Exception {
        SimpleBean bean = new SimpleBean();
        Beans.setPropValue(bean, "name", "Jane");
        assertEquals("Jane", bean.getName());
        Beans.setPropValue(bean, "age", 30);
        assertEquals(30, bean.getAge());
        Beans.setPropValue(bean, "active", true);
        assertEquals(true, bean.getActive());

        Beans.setPropValue(bean, "active", null);
        assertNull(bean.getActive());

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.setPropValue(bean, setName, "Bob");
        assertEquals("Bob", bean.getName());
        Beans.setPropValue(bean, setName, null);
        assertNull(bean.getName());

        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        Beans.setPropValue(bean, setAge, 42);
        assertEquals(42, bean.getAge());
        Beans.setPropValue(bean, setAge, null);
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testSetPropValue_NestedAndUnmatched() {
        Beans.setPropValue(nestedBean, "address.city", "Boston");
        assertEquals("Boston", nestedBean.getAddress().getCity());

        SimpleBean bean = new SimpleBean();
        assertTrue(Beans.setPropValue(bean, "name", "Test", false));
        assertEquals("Test", bean.getName());

        assertFalse(Beans.setPropValue(bean, "nonExistent", "value", true));
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(bean, "nonExistent", "value", false));
    }

    @Test
    public void testSetPropValueByGetter() throws Exception {
        CollectionBean bean = new CollectionBean();
        List<String> list = new ArrayList<>();
        list.add("old");
        bean.setItems(list);

        Method getItems = CollectionBean.class.getMethod("getItems");
        Beans.setPropValueByGetter(bean, getItems, Arrays.asList("new1", "new2"));
        assertEquals(Arrays.asList("new1", "new2"), bean.getItems());

        Beans.setPropValueByGetter(bean, getItems, Arrays.asList("x", "y", "z"));
        assertEquals(3, bean.getItems().size());
        assertTrue(bean.getItems().contains("x"));

        Beans.setPropValueByGetter(bean, getItems, Collections.emptyList());
        assertEquals(0, bean.getItems().size());

        bean.setItems(new ArrayList<>(Arrays.asList("a")));
        Beans.setPropValueByGetter(bean, getItems, null);
        assertEquals(1, bean.getItems().size());

        bean.setItems(new ArrayList<>(Arrays.asList("a", "b")));
        Beans.setPropValueByGetter(bean, getItems, bean.getItems());
        assertEquals(Arrays.asList("a", "b"), bean.getItems());

        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValueByGetter(simpleBean, SimpleBean.class.getMethod("getName"), "newValue"));
    }

    @Test
    public void testSetPropValueByGetter_Map() throws Exception {
        MapBean bean = new MapBean();
        Map<String, Integer> values = new IdentityHashMap<>();
        String first = new String("key");
        String second = new String("key");
        values.put(first, 1);
        values.put(second, 2);
        bean.setValues(values);

        Beans.setPropValueByGetter(bean, MapBean.class.getMethod("getValues"), Collections.unmodifiableMap(values));
        assertEquals(2, values.size());
        assertEquals(Integer.valueOf(1), values.get(first));
        assertEquals(Integer.valueOf(2), values.get(second));

        Map<String, Integer> ordered = new LinkedHashMap<>();
        ordered.put("b", 2);
        ordered.put("a", 1);
        bean.setValues(ordered);
        Beans.setPropValueByGetter(bean, MapBean.class.getMethod("getValues"), Collections.unmodifiableMap(ordered));
        assertEquals(List.of("b", "a"), new ArrayList<>(ordered.keySet()));

        bean.setValues(new LinkedHashMap<>(Map.of("one", 1, "two", 2)));
        Beans.setPropValueByGetter(bean, MapBean.class.getMethod("getValues"), bean.getValues());
        assertEquals(Map.of("one", 1, "two", 2), bean.getValues());
        assertNotNull(bean.getValues());
    }

    public static class ThrowingSetterBean {
        static int calls;
        private int age;

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public String getX() {
            return "x";
        }

        public void setX(final String x) {
            calls++;
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Pins the {@code @throws RuntimeException} contract of {@code setPropValue(Object, Method, Object)}: a
     * setter that is inaccessible or throws is propagated immediately, <i>without</i> the type-converting retry.
     * The invocation count is the observable part - a retry would call the setter twice.
     */
    @Test
    public void testSetPropValue_InvocationFailureIsNotRetriedWithAConvertedValue() throws Exception {
        final Method setX = ThrowingSetterBean.class.getMethod("setX", String.class);

        ThrowingSetterBean.calls = 0;
        assertThrows(IllegalStateException.class, () -> Beans.setPropValue(new ThrowingSetterBean(), setX, "v"));
        assertEquals(1, ThrowingSetterBean.calls);

        // The null-value branch has no retry at all.
        ThrowingSetterBean.calls = 0;
        assertThrows(IllegalStateException.class, () -> Beans.setPropValue(new ThrowingSetterBean(), setX, null));
        assertEquals(1, ThrowingSetterBean.calls);

        // The retry exists only for the other case: the setter rejecting the value's type.
        final ThrowingSetterBean bean = new ThrowingSetterBean();
        final Method setAge = ThrowingSetterBean.class.getMethod("setAge", int.class);
        assertEquals(42, Beans.setPropValue(bean, setAge, "42"));
        assertEquals(42, bean.getAge());
    }
}
