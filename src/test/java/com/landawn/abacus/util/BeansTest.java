package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BeansTest extends BeansTestSupport {

    @Test
    public void testMergeIntoSnapshotsSourceBeforeRenamingOnSameBean() {
        for (int variant = 0; variant < 2; variant++) {
            final Address bean = new Address();
            bean.setStreet("original street");
            bean.setCity("original city");
            bean.setZipCode("12345");
            final java.util.function.Function<String, String> swapNames = name -> switch (name) {
                case "street" -> "city";
                case "city" -> "street";
                default -> name;
            };

            final Address result = variant == 0 ? Beans.mergeInto(bean, bean, swapNames, Fn.selectFirst())
                    : Beans.mergeInto(bean, bean, (Collection<String>) null, swapNames, Fn.selectFirst());

            org.junit.jupiter.api.Assertions.assertSame(bean, result);
            assertEquals("original city", bean.getStreet());
            assertEquals("original street", bean.getCity());
            assertEquals("12345", bean.getZipCode());
        }
    }

    @Test
    public void testMergeIntoDistinctBeansAppliesValuesInPropertyOrder() {
        final Address source = new Address();
        source.setStreet("source street");
        source.setCity("source city");
        source.setZipCode("12345");
        final Address target = new Address();
        final java.util.concurrent.atomic.AtomicInteger resolved = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger applied = new java.util.concurrent.atomic.AtomicInteger();

        // Distinct beans keep the original read/apply order; only self-merges need the source snapshot.
        Beans.mergeInto(source, target, (Collection<String>) null, name -> {
            assertEquals(resolved.getAndIncrement(), applied.get());
            return switch (name) {
                case "street" -> "city";
                case "city" -> "street";
                default -> name;
            };
        }, (sourceValue, targetValue) -> {
            applied.incrementAndGet();
            return sourceValue;
        });

        assertEquals(3, applied.get());
        assertEquals("source city", target.getStreet());
        assertEquals("source street", target.getCity());
        assertEquals("12345", target.getZipCode());
        assertEquals("source street", source.getStreet());
        assertEquals("source city", source.getCity());
    }

    @Test
    public void testComputedGetterDiscoveryForEntityAndRecord() {
        assertEquals(List.of("id"), Beans.getPropNameList(OrdinaryComputedGetter.class));
        assertTrue(Beans.getPropNameList(EntityComputedGetter.class).contains("computed"));
        assertEquals(List.of("id", "computed"), Beans.getPropNameList(RecordComputedGetter.class));
        assertEquals("derived", Beans.getPropValue(new EntityComputedGetter(), "computed"));
        assertEquals("derived", Beans.getPropValue(new RecordComputedGetter(7), "computed"));
        OrdinaryComputedGetter ordinary = new OrdinaryComputedGetter();
        ordinary.setId(7);
        assertEquals(7, Beans.<Integer> getPropValue(ordinary, "id"));
        assertFalse(Beans.getPropValueIfPresent(ordinary, "computed").isPresent());
    }

    public static class OrdinaryComputedGetter {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getComputed() {
            return "derived";
        }
    }

    @com.landawn.abacus.annotation.Entity
    public static class EntityComputedGetter {
        public String getComputed() {
            return "derived";
        }
    }

    public record RecordComputedGetter(int id) {
        public String getComputed() {
            return "derived";
        }
    }

    @Test
    public void testIsBeanClass() {
        assertTrue(Beans.isBeanClass(SimpleBean.class));
        assertTrue(Beans.isBeanClass(EntityBean.class));
        assertTrue(Beans.isBeanClass(RecordBean.class));
        assertFalse(Beans.isBeanClass(String.class));
        assertFalse(Beans.isBeanClass(Integer.class));
        assertFalse(Beans.isBeanClass(Map.Entry.class));
        assertFalse(Beans.isBeanClass(null));
    }

    @Test
    public void testIsBeanClass_NonBeanTypes() {
        assertFalse(Beans.isBeanClass(int.class));
        assertFalse(Beans.isBeanClass(void.class));
        assertFalse(Beans.isBeanClass(int[].class));
        assertFalse(Beans.isBeanClass(Object.class));
        assertFalse(Beans.isBeanClass(java.util.Date.class));
        assertFalse(Beans.isBeanClass(HashMap.class));
        assertFalse(Beans.isBeanClass(java.util.ArrayList.class));
    }

    @Test
    public void testIsRecordClass() {
        assertTrue(Beans.isRecordClass(RecordBean.class));
        assertFalse(Beans.isRecordClass(SimpleBean.class));
        assertFalse(Beans.isRecordClass(EntityBean.class));
        assertFalse(Beans.isRecordClass(String.class));
        assertFalse(Beans.isRecordClass(int.class));
        assertFalse(Beans.isRecordClass(null));
    }

    @Test
    public void testRefreshBeanPropInfo() {
        Beans.refreshBeanPropInfo(SimpleBean.class);
        Beans.refreshBeanPropInfo(NestedBean.class);
        Beans.refreshBeanPropInfo(EntityBean.class);

        assertNotNull(Beans.getBeanInfo(SimpleBean.class));
        assertFalse(Beans.getBeanInfo(SimpleBean.class).propInfoList.isEmpty());
        assertNotNull(Beans.getBeanInfo(EntityBean.class));
    }

    @Test
    public void testNormalizePropName() {
        assertEquals("userName", Beans.normalizePropName("user_name"));
        assertEquals("firstName", Beans.normalizePropName("first_name"));
        assertEquals("addressLine1", Beans.normalizePropName("address_line_1"));
        assertEquals("myPropertyName", Beans.normalizePropName("my_property_name"));
        assertEquals("userIdValue", Beans.normalizePropName("user_id_value"));
        assertEquals("id", Beans.normalizePropName("id"));
        assertEquals("simple", Beans.normalizePropName("simple"));
        assertEquals("alreadyCamel", Beans.normalizePropName("alreadyCamel"));
    }

    @Test
    public void testNormalizePropName_ClassKeyword() {
        assertEquals("clazz", Beans.normalizePropName("class"));
        assertEquals("clazz", Beans.normalizePropName("CLASS"));
        assertEquals("id", Beans.normalizePropName("ID"));
        assertEquals("url", Beans.normalizePropName("URL"));
    }

    @Test
    public void testNewBean() {
        SimpleBean bean = Beans.newBean(SimpleBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());

        NestedBean nested = Beans.newBean(NestedBean.class);
        assertNotNull(nested);
        assertNull(nested.getId());

        EntityBean entity = Beans.newBean(EntityBean.class);
        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getValue());
    }

    @Test
    public void testClearProps() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.clearProps(bean, "name", "age");
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(true, bean.getActive());

        bean = new SimpleBean("Jane", 30);
        bean.setActive(true);
        Beans.clearProps(bean, Arrays.asList("name"));
        assertNull(bean.getName());
        assertEquals(30, bean.getAge());
        assertEquals(true, bean.getActive());

        Beans.clearProps(bean, "age", "active");
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testClearProps_EmptyOrNull() {
        SimpleBean bean = new SimpleBean("Bob", 35);
        Beans.clearProps(bean, new String[0]);
        assertEquals("Bob", bean.getName());

        Beans.clearProps(bean, Collections.emptyList());
        assertEquals("Bob", bean.getName());
        assertEquals(35, bean.getAge());

        assertDoesNotThrow(() -> Beans.clearProps(null, "name"));
        assertDoesNotThrow(() -> Beans.clearProps(null, Arrays.asList("name")));
    }

    @Test
    public void testClearAllProps() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);
        Beans.clearAllProps(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());

        assertDoesNotThrow(() -> Beans.clearAllProps(null));
    }

    @Test
    public void testRandomize() {
        SimpleBean bean = new SimpleBean();
        Beans.randomize(bean);
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNotNull(bean.getActive());

        bean = new SimpleBean();
        Beans.randomize(bean, Arrays.asList("name", "age"));
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNull(bean.getActive());

        bean = new SimpleBean();
        Beans.randomize(bean, Arrays.asList("name"));
        assertNotNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    @Test
    public void testRandomize_NullInput() {
        SimpleBean bean = new SimpleBean();
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize(bean, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean(SimpleBean.class, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(SimpleBean.class, (Collection<String>) null, 2));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean((Class<?>) null));
    }

    @Test
    public void testNewRandomBean() {
        SimpleBean filled = Beans.newRandomBean(SimpleBean.class);
        assertNotNull(filled);
        assertNotNull(filled.getName());
        assertTrue(filled.getAge() != 0);

        filled = Beans.newRandomBean(SimpleBean.class, Arrays.asList("name"));
        assertNotNull(filled.getName());
        assertEquals(0, filled.getAge());
        assertNull(filled.getActive());

        filled = Beans.newRandomBean(SimpleBean.class, Arrays.asList("age"));
        assertTrue(filled.getAge() != 0);
        assertNull(filled.getName());
    }

    @Test
    public void testNewRandomBeanList() {
        List<SimpleBean> list = Beans.newRandomBeanList(SimpleBean.class, 5);
        assertEquals(5, list.size());
        for (SimpleBean bean : list) {
            assertNotNull(bean);
            assertNotNull(bean.getName());
        }

        list = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name", "age"), 3);
        assertEquals(3, list.size());
        for (SimpleBean bean : list) {
            assertNotNull(bean.getName());
            assertTrue(bean.getAge() != 0);
            assertNull(bean.getActive());
        }

        list = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name"), 3);
        assertEquals(3, list.size());
        for (SimpleBean bean : list) {
            assertNotNull(bean.getName());
            assertEquals(0, bean.getAge());
        }

        assertTrue(Beans.newRandomBeanList(SimpleBean.class, 0).isEmpty());
        assertTrue(Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name"), 0).isEmpty());
    }

    @Test
    public void testStream() {
        List<Map.Entry<String, Object>> entries = Beans.stream(simpleBean).toList();
        Map<String, Object> asMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            asMap.put(entry.getKey(), entry.getValue());
        }
        assertEquals("John", asMap.get("name"));
        assertEquals(25, asMap.get("age"));
        assertEquals(true, asMap.get("active"));

        List<Map.Entry<String, Object>> stringProps = Beans.stream(simpleBean, (name, value) -> value instanceof String).toList();
        assertEquals(1, stringProps.size());
        assertEquals("name", stringProps.get(0).getKey());
        assertEquals("John", stringProps.get(0).getValue());

        List<Map.Entry<String, Object>> nameFilter = Beans.stream(simpleBean, (name, value) -> name.startsWith("a")).toList();
        assertTrue(nameFilter.stream().allMatch(e -> e.getKey().startsWith("a")));
        assertTrue(nameFilter.stream().anyMatch(e -> "age".equals(e.getKey())));
        assertTrue(nameFilter.stream().anyMatch(e -> "active".equals(e.getKey())));

        assertTrue(Beans.stream(simpleBean, (name, value) -> false).toList().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Beans.stream(null));
    }

    @Test
    public void testStream_SkipNullValues() {
        SimpleBean bean = new SimpleBean("Owen", 40);
        List<Map.Entry<String, Object>> nonNull = Beans.stream(bean, (name, val) -> val != null).toList();
        assertFalse(nonNull.stream().anyMatch(e -> "active".equals(e.getKey())));
        assertTrue(nonNull.stream().anyMatch(e -> "name".equals(e.getKey()) && "Owen".equals(e.getValue())));
    }

    public static class RandomizeEmailBean {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }
    }

    /**
     * Pins the shape {@code randomize(Object)} documents for an e-mail-named property: the local part is the
     * first 12 characters of a canonical (hyphenated) UUID, so it is 8 hex digits, a hyphen and 3 hex digits -
     * never 12 hex characters.
     */
    @Test
    public void testRandomize_EmailPropertyLocalPartIsAUuidPrefix() {
        for (int i = 0; i < 5; i++) {
            final RandomizeEmailBean bean = new RandomizeEmailBean();
            Beans.randomize(bean);

            final String email = bean.getEmail();
            assertTrue(email.endsWith("@email.com"), email);

            final String local = email.substring(0, email.indexOf('@'));
            assertEquals(12, local.length(), local);
            assertEquals('-', local.charAt(8), local);
            assertTrue(local.matches("[0-9a-f]{8}-[0-9a-f]{3}"), local);
        }
    }
}
