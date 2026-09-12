package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

public class BeansToTest extends BeansTestSupport {

    @Test
    public void testToCamelCase() {
        assertEquals("userName", Beans.toCamelCase("user_name"));
        assertEquals("firstName", Beans.toCamelCase("FIRST_NAME"));
        assertEquals("firstName", Beans.toCamelCase("first_name"));
        assertEquals("addressLine1", Beans.toCamelCase("address-line-1"));
        assertEquals("addressLine", Beans.toCamelCase("address-line"));
        assertEquals("userId", Beans.toCamelCase("user_id"));
        assertEquals("userId", Beans.toCamelCase("USER_ID"));
        assertEquals("id", Beans.toCamelCase("id"));
        assertEquals("name", Beans.toCamelCase("name"));
        assertEquals("name", Beans.toCamelCase("NAME"));
        assertEquals("address1", Beans.toCamelCase("address_1"));
        assertEquals("line2Value", Beans.toCamelCase("line_2_value"));
        assertEquals("", Beans.toCamelCase(""));
        assertNull(Beans.toCamelCase((String) null));
    }

    @Test
    public void testToSnakeCase() {
        assertEquals("user_name", Beans.toSnakeCase("userName"));
        assertEquals("first_name", Beans.toSnakeCase("FirstName"));
        assertEquals("first_name", Beans.toSnakeCase("firstName"));
        assertEquals("user_id", Beans.toSnakeCase("userID"));
        assertEquals("url", Beans.toSnakeCase("URL"));
        assertEquals("simple", Beans.toSnakeCase("simple"));
        assertEquals("a", Beans.toSnakeCase("a"));
        assertEquals("a", Beans.toSnakeCase("A"));
        assertEquals("", Beans.toSnakeCase(""));
        assertNull(Beans.toSnakeCase((String) null));
    }

    @Test
    public void testToScreamingSnakeCase() {
        assertEquals("USER_NAME", Beans.toScreamingSnakeCase("userName"));
        assertEquals("FIRST_NAME", Beans.toScreamingSnakeCase("firstName"));
        assertEquals("USER_NAME", Beans.toScreamingSnakeCase("UserName"));
        assertEquals("USER_ID", Beans.toScreamingSnakeCase("userID"));
        assertEquals("URL", Beans.toScreamingSnakeCase("URL"));
        assertEquals("SIMPLE", Beans.toScreamingSnakeCase("SIMPLE"));
        assertEquals("A", Beans.toScreamingSnakeCase("a"));
        assertEquals("A", Beans.toScreamingSnakeCase("A"));
        assertEquals("", Beans.toScreamingSnakeCase(""));
        assertNull(Beans.toScreamingSnakeCase((String) null));
    }

    @Test
    public void testMapBuilder() {
        Map<String, Object> all = Beans.mapBuilder(simpleBean).toMap();
        assertTrue(all instanceof LinkedHashMap);
        assertEquals(3, all.size());
        assertEquals("John", all.get("name"));
        assertEquals(25, all.get("age"));
        assertEquals(true, all.get("active"));

        Map<String, Object> sel = Beans.mapBuilder(simpleBean).select("name", "age").toMap();
        assertEquals(2, sel.size());
        assertTrue(sel.containsKey("name"));
        assertTrue(sel.containsKey("age"));

        Map<String, Object> exc = Beans.mapBuilder(simpleBean).exclude("active").toMap();
        assertEquals(2, exc.size());
        assertFalse(exc.containsKey("active"));

        assertThrows(IllegalArgumentException.class, () -> Beans.mapBuilder(simpleBean).select("nope").toMap());
    }

    @Test
    public void testMapBuilder_FilterAndSkipNulls() {
        SimpleBean bean = new SimpleBean("John", 25);

        Map<String, Object> strings = Beans.mapBuilder(bean).filter((name, value) -> value instanceof String).toMap();
        assertEquals(1, strings.size());
        assertEquals("John", strings.get("name"));

        Map<String, Object> nonNull = Beans.mapBuilder(bean).skipNulls().toMap();
        assertEquals(2, nonNull.size());
        assertFalse(nonNull.containsKey("active"));

        Map<String, Object> withNulls = Beans.mapBuilder(bean).toMap();
        assertEquals(3, withNulls.size());
        assertTrue(withNulls.containsKey("active"));
        assertNull(withNulls.get("active"));
    }

    @Test
    public void testMapBuilder_NamingSupplierInto() {
        NestedBean nb = new NestedBean();
        nb.setSimpleBean(new SimpleBean("x", 1));

        Map<String, Object> snake = Beans.mapBuilder(nb).select("simpleBean").naming(NamingPolicy.SNAKE_CASE).toMap();
        assertTrue(snake.containsKey("simple_bean"));

        Map<String, Object> tree = Beans.mapBuilder(simpleBean).toMap(size -> new TreeMap<>());
        assertTrue(tree instanceof TreeMap);
        assertEquals(3, tree.size());

        Map<String, Object> existing = new LinkedHashMap<>();
        existing.put("id", 1);
        Map<String, Object> filled = Beans.mapBuilder(simpleBean).select("name").into(existing);
        assertTrue(existing == filled);
        assertEquals(1, existing.get("id"));
        assertEquals("John", existing.get("name"));
    }

    @Test
    public void testMapBuilder_DeepAndFlat() {
        NestedBean bean = new NestedBean();
        bean.setId("u1");
        bean.setAddress(new Address("NYC"));
        bean.setSimpleBean(null);
        bean.setTags(null);

        Map<String, Object> deep = Beans.mapBuilder(bean).skipNulls().deep().toMap();
        assertEquals("u1", deep.get("id"));
        assertTrue(deep.get("address") instanceof Map);
        assertEquals("NYC", ((Map<?, ?>) deep.get("address")).get("city"));

        Map<String, Object> flat = Beans.mapBuilder(bean).skipNulls().flat().toMap();
        assertEquals("u1", flat.get("id"));
        assertEquals("NYC", flat.get("address.city"));

        assertThrows(IllegalStateException.class, () -> Beans.mapBuilder(bean).flat().deep());
        assertThrows(IllegalStateException.class, () -> Beans.mapBuilder(bean).deep().flat());

        Map<String, Object> repeated = Beans.mapBuilder(bean).skipNulls().deep().deep().toMap();
        assertTrue(repeated.get("address") instanceof Map);
    }

    @Test
    public void testMapBuilder_NullBean() {
        assertTrue(Beans.mapBuilder((Object) null).toMap().isEmpty());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("x", 1);
        assertTrue(Beans.mapBuilder((Object) null).into(out) == out);
        assertEquals(1, out.size());
    }

    /** Single-property bean for the key-derivation examples in the {@link Beans} class javadoc. */
    public static class KeyNamingBean {
        private String firstName;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }
    }

    /**
     * Pins the "Key Naming" paragraph of the {@link Beans} class javadoc: the key comes from the matched
     * property's own name with the {@link NamingPolicy} applied, never from the spelling that was passed, and
     * {@code CAMEL_CASE} emits exactly the keys the default JSON serialization uses. Every call here is one of
     * the javadoc's examples, so the examples cannot drift back to an overload that does not exist.
     */
    @Test
    public void testBeanToMap_KeyNamingExamplesFromTheClassJavadoc() {
        final KeyNamingBean user = new KeyNamingBean();
        user.setFirstName("John");

        assertEquals("{first_name=John}",
                Beans.beanToMap(user, java.util.List.of("getFirstName"), NamingPolicy.SNAKE_CASE, IntFunctions.ofLinkedHashMap()).toString());
        assertEquals("{first_name=John}", Beans.beanToMap(user, null, NamingPolicy.SNAKE_CASE, IntFunctions.ofLinkedHashMap()).toString());

        assertEquals("{firstName=John}", Beans.beanToMap(user, null, NamingPolicy.CAMEL_CASE, IntFunctions.ofLinkedHashMap()).toString());
        assertEquals("{\"firstName\": \"John\"}", N.toJson(user));

        // Two spellings of one property collapse to a single entry.
        assertEquals("{firstName=John}", Beans.beanToMap(user, java.util.List.of("firstName", "first_name")).toString());
    }
}
