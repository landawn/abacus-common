package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@SuppressWarnings("deprecation")
public class SeidTest extends TestBase {

    public static class SimpleUser {
        private int id;
        private String name;

        public SimpleUser(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class AnnotatedUser {
        @com.landawn.abacus.annotation.Id
        private int id;
        private String name;

        public AnnotatedUser(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Test
    public void testConstructor() {
        Seid byProp = new Seid("Order.orderId", 456);
        assertEquals("Order", byProp.entityName());
        assertEquals(456, (int) byProp.get("orderId"));

        Map<String, Object> props = new LinkedHashMap<>();
        props.put("Order.orderId", 100);
        Seid byMap = new Seid(props);
        assertEquals("Order", byMap.entityName());
        assertEquals(100, (int) byMap.get("orderId"));

        Seid byName = new Seid("MyEntity");
        assertEquals("MyEntity", byName.entityName());
        assertTrue(byName.isEmpty());
        assertEquals("", new Seid((String) null).entityName());
    }

    @Test
    public void testOf() {
        Seid two = Seid.of("User.id", 123, "User.name", "John");
        assertEquals("User", two.entityName());
        assertEquals(123, (int) two.get("id"));
        assertEquals("John", two.get("name"));
        assertEquals(2, two.size());

        Seid three = Seid.of("User.id", 1, "User.name", "John", "User.age", 30);
        assertEquals(1, (int) three.get("id"));
        assertEquals(30, (int) three.get("age"));
        assertEquals(3, three.size());

        Seid empty = Seid.of("User");
        assertEquals("User", empty.entityName());
        assertTrue(empty.isEmpty());
        assertEquals("", Seid.of((String) null).entityName());
        assertEquals("", Seid.of("").entityName());

        Seid single = Seid.of("User.id", 123);
        assertEquals("User", single.entityName());
        assertEquals(123, (int) single.get("id"));
        Seid simple = Seid.of("id", 123);
        assertEquals("", simple.entityName());
        assertEquals(123, (int) simple.get("id"));
        assertNull(Seid.of("User.id", null).get("id"));
        assertEquals(1, Seid.of("User.id", null).size());
    }

    @Test
    public void testOf_EdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> Seid.of(null, 1));
        Seid seid = Seid.of("User.id", 1);
        assertThrows(IllegalArgumentException.class, () -> seid.get(null));
        assertThrows(IllegalArgumentException.class, () -> seid.get("id", null));
        assertThrows(IllegalArgumentException.class, () -> seid.containsKey(null));
        assertThrows(IllegalArgumentException.class, () -> seid.set(null, 2));
        assertEquals(1, (int) seid.get("id"));
    }

    @Test
    public void testCreate() {
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("User.id", 123);
        props.put("User.version", 1);
        Seid fromMap = Seid.create(props);
        assertEquals("User", fromMap.entityName());
        assertEquals(123, (int) fromMap.get("id"));
        assertEquals(1, (int) fromMap.get("version"));
        assertThrows(IllegalArgumentException.class, () -> Seid.create(new HashMap<>()));

        SimpleUser user = new SimpleUser(42, "Alice");
        Seid fromEntity = Seid.create(user, java.util.Arrays.asList("id"));
        assertEquals("SimpleUser", fromEntity.entityName());
        assertEquals(Integer.valueOf(42), fromEntity.get("id"));
        assertThrows(IllegalArgumentException.class, () -> Seid.create(user, Collections.emptyList()));
        IllegalArgumentException missing = assertThrows(IllegalArgumentException.class, () -> Seid.create(user, Collections.singletonList("missing")));
        assertTrue(missing.getMessage().contains("missing"));
        assertTrue(missing.getMessage().contains("SimpleUser"));

        AnnotatedUser annotated = new AnnotatedUser(99, "Charlie");
        assertEquals(Integer.valueOf(99), Seid.create(annotated).get("id"));
    }

    @Test
    public void testGet() {
        Seid seid = Seid.of("User.id", 123);
        assertEquals("User", seid.entityName());
        assertEquals("", Seid.of("id", 123).entityName());
        assertEquals(123, (int) seid.get("id"));
        assertEquals(123, (int) seid.get("User.id"));
        assertNull(seid.get("nonexistent"));
        assertEquals(Boolean.TRUE, Seid.of("User.active", "true").get("active", Boolean.class));
        assertEquals(123L, seid.get("id", Long.class));
        assertEquals(0, Seid.of("User.id", null).get("id", int.class));
        assertEquals(25, Seid.of("User.age", 25).getInt("age"));
        assertEquals(25, Seid.of("User.age", 25L).getInt("age"));
        assertEquals(30, Seid.of("User.age", "30").getInt("age"));
        assertEquals(100L, Seid.of("User.id", 100L).getLong("id"));
        assertEquals(42L, Seid.of("User.id", 42).getLong("id"));
    }

    @Test
    public void testSet() {
        Seid seid = Seid.of("User");
        assertEquals(seid, seid.set("id", 100));
        assertEquals(100, (int) seid.get("id"));
        seid.set("id", 200);
        assertEquals(200, (int) seid.get("id"));
        seid.set("name", "John").set("age", 30);
        assertEquals("John", seid.get("name"));
        assertEquals(3, seid.size());

        Seid mapped = Seid.of("User");
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("id", 1);
        props.put("name", "Alice");
        mapped.set(props);
        assertEquals(1, (int) mapped.get("id"));
        assertEquals("Alice", mapped.get("name"));
        mapped.set(new HashMap<>());
        assertEquals(1, (int) mapped.get("id"));

        Seid atomic = Seid.of("User.id", 1);
        String rendered = atomic.toString();
        Map<String, Object> invalid = new LinkedHashMap<>();
        invalid.put("name", "Alice");
        invalid.put(null, 2);
        assertThrows(IllegalArgumentException.class, () -> atomic.set(invalid));
        assertEquals(1, atomic.size());
        assertFalse(atomic.containsKey("name"));
        assertEquals(rendered, atomic.toString());
    }

    @Test
    public void testContainsKeyAndViews() {
        Seid seid = Seid.of("User.id", 123, "User.name", "John");
        assertTrue(seid.containsKey("id"));
        assertTrue(seid.containsKey("User.id"));
        assertFalse(seid.containsKey("age"));
        assertFalse(Seid.of("User").containsKey("id"));

        Set<String> keys = seid.keySet();
        assertEquals(2, keys.size());
        assertTrue(keys.contains("id"));
        assertTrue(keys.contains("name"));
        assertTrue(Seid.of("User").keySet().isEmpty());
        String rendered = seid.toString();
        assertThrows(UnsupportedOperationException.class, () -> seid.keySet().remove("id"));
        assertEquals(2, seid.size());
        assertEquals(rendered, seid.toString());

        Set<Map.Entry<String, Object>> entries = Seid.of("User.id", 123).entrySet();
        assertEquals(1, entries.size());
        Map.Entry<String, Object> entry = entries.iterator().next();
        assertEquals("id", entry.getKey());
        assertEquals(123, entry.getValue());
        assertThrows(UnsupportedOperationException.class, () -> seid.entrySet().iterator().next().setValue("changed"));
        assertThrows(UnsupportedOperationException.class, seid.entrySet()::clear);
        assertEquals(rendered, seid.toString());
    }

    @Test
    public void testSizeClearCopy() {
        Seid empty = Seid.of("User");
        assertEquals(0, empty.size());
        assertTrue(empty.isEmpty());
        empty.set("id", 1);
        assertEquals(1, empty.size());
        assertFalse(empty.isEmpty());
        assertEquals(3, Seid.of("User.id", 1, "User.name", "John", "User.age", 30).size());

        Seid seid = Seid.of("User.id", 1, "User.name", "John");
        seid.clear();
        assertTrue(seid.isEmpty());
        assertEquals(0, seid.size());

        Seid original = Seid.of("User.id", 123, "User.name", "John");
        Seid copy = original.copy();
        assertEquals(original, copy);
        copy.set("id", 999);
        assertEquals(123, (int) original.get("id"));
        assertEquals(999, (int) copy.get("id"));
    }

    @Test
    public void testEqualsHashCodeToString() {
        Seid a = Seid.of("User.id", 123);
        Seid b = Seid.of("User.id", 123);
        assertEquals(a, a);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Seid.of("User.id", 456));
        assertNotEquals(a, Seid.of("User.name", "John"));
        assertNotEquals(a, "not a seid");
        assertNotEquals(a, null);
        assertEquals(a.hashCode(), a.hashCode());
        assertNotEquals(a.hashCode(), Seid.of("User.id", 456).hashCode());

        assertEquals("User: {}", Seid.of("User").toString());
        assertEquals("User: {id=123}", a.toString());
        assertEquals(a.toString(), a.toString());
        String two = Seid.of("User.id", 123, "User.name", "John").toString();
        assertTrue(two.startsWith("User: {"));
        assertTrue(two.contains("id=123"));
        assertTrue(two.contains("name=John"));
        assertTrue(two.endsWith("}"));
        String three = Seid.of("User.a", 1, "User.b", 2, "User.c", 3).toString();
        assertTrue(three.contains("a=1") && three.contains("b=2") && three.contains("c=3"));
    }

    @Test
    public void testEqualsHashCodeComparesArrayValuesByContent() {
        // Map.equals()/Map.hashCode() would compare an array-valued property by reference; the
        // documented contract is by value, so equals()/hashCode() compare deeply.
        Seid a = Seid.of("User.key", new int[] { 1, 2, 3 });
        Seid b = Seid.of("User.key", new int[] { 1, 2, 3 });
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, Seid.of("User.key", new int[] { 1, 2, 4 }));

        Seid bytesA = Seid.of("User.key", new byte[] { 7, 8 });
        Seid bytesB = Seid.of("User.key", new byte[] { 7, 8 });
        assertEquals(bytesA, bytesB);
        assertEquals(bytesA.hashCode(), bytesB.hashCode());

        // Nested arrays are compared element-wise too.
        Seid nestedA = Seid.of("User.key", new Object[] { new int[] { 1 }, "x" });
        Seid nestedB = Seid.of("User.key", new Object[] { new int[] { 1 }, "x" });
        assertEquals(nestedA, nestedB);
        assertEquals(nestedA.hashCode(), nestedB.hashCode());

        // A different array type with the same content is still a different value.
        assertNotEquals(Seid.of("User.key", new int[] { 1 }), Seid.of("User.key", new long[] { 1 }));

        // Multi-property ids and null values keep working.
        Seid multiA = Seid.of("User.id", 1).set("key", new String[] { "a", "b" });
        Seid multiB = Seid.of("User.id", 1).set("key", new String[] { "a", "b" });
        assertEquals(multiA, multiB);
        assertEquals(multiA.hashCode(), multiB.hashCode());
        assertNotEquals(multiA, Seid.of("User.id", 1).set("key", new String[] { "a", "c" }));

        Seid nullA = Seid.of("User.id", 1).set("key", null);
        Seid nullB = Seid.of("User.id", 1).set("key", null);
        assertEquals(nullA, nullB);
        assertEquals(nullA.hashCode(), nullB.hashCode());
        // Same size, different key set: "key"->null vs "other"->null must not be equal.
        assertNotEquals(nullA, Seid.of("User.id", 1).set("other", null));
        assertNotEquals(nullA, multiA);
    }

    @Test
    public void testHashCodeUnchangedForNonArrayValues() {
        // The deep hash keeps the plain Map.hashCode() value for non-array properties.
        Seid seid = Seid.of("User.id", 123, "User.name", "John");
        Map<String, Object> plain = new LinkedHashMap<>();
        plain.put("id", 123);
        plain.put("name", "John");
        assertEquals(31 * "User".hashCode() + plain.hashCode(), seid.hashCode());
        assertEquals(31 * "User".hashCode(), Seid.of("User").hashCode());
    }

    public static class PlainIdBean {
        private long id;
        private String name;

        public long getId() {
            return id;
        }

        public void setId(final long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class NamedUserIdBean {
        private Long userId;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(final Long userId) {
            this.userId = userId;
        }
    }

    public static class DoubleIdBean {
        private double id;

        public double getId() {
            return id;
        }

        public void setId(final double id) {
            this.id = id;
        }
    }

    @Test
    public void testCreateAcceptsTheUnannotatedIdPropertyFallback() {
        // create(Object) documents that an id property is either annotated as an id OR simply named `id` with
        // one of the recognised id types - no annotation is required for the second form
        final PlainIdBean bean = new PlainIdBean();
        bean.setId(7L);
        bean.setName("John");

        final Seid seid = Seid.create(bean);
        assertEquals(1, seid.size());
        final Long idValue = seid.get("id");
        assertEquals(Long.valueOf(7L), idValue);
        assertEquals(Collections.singletonList("id"), Seid.getIdFieldNames(PlainIdBean.class));

        // the fallback needs the exact name `id` ...
        assertTrue(Seid.getIdFieldNames(NamedUserIdBean.class).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Seid.create(new NamedUserIdBean()));

        // ... and a recognised id type
        assertTrue(Seid.getIdFieldNames(DoubleIdBean.class).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Seid.create(new DoubleIdBean()));
    }

    @Test
    public void testToStringReturnsThePooledBufferWhenAPropertyValueThrows() throws Exception {
        // the >= 3 property branch of toString borrows a StringBuilder from Objectory; a throwing toString()
        // on one of the values must not strand that buffer outside the pool
        final java.util.Queue<StringBuilder> pool = objectoryStringBuilderPool();
        Objectory.recycle(Objectory.createStringBuilder());
        final int poolSizeBefore = pool.size();

        final Seid seid = Seid.of("User.a", "1");
        seid.set("User.b", "2");
        seid.set("User.c", new Object() {
            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        });

        assertThrows(IllegalStateException.class, seid::toString);
        assertEquals(poolSizeBefore, pool.size());

        // the successful path is unchanged
        final Seid good = Seid.of("User.a", "1");
        good.set("User.b", "2");
        good.set("User.c", "3");
        assertEquals("User: {a=1, b=2, c=3}", good.toString());
        assertEquals(poolSizeBefore, pool.size());
    }

    @SuppressWarnings("unchecked")
    private static java.util.Queue<StringBuilder> objectoryStringBuilderPool() throws Exception {
        final java.lang.reflect.Field field = Objectory.class.getDeclaredField("stringBuilderPool");
        field.setAccessible(true);

        return (java.util.Queue<StringBuilder>) field.get(null);
    }
}
