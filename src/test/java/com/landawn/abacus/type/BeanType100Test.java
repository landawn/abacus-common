package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type.SerializationType;

@Tag("new-test")
public class BeanType100Test extends TestBase {

    private Type<TestBean> beanType;

    @BeforeEach
    public void setUp() {
        beanType = createType(TestBean.class);
    }

    @Test
    public void testClazz() {
        assertEquals(TestBean.class, beanType.clazz());
    }

    @Test
    public void testIsBean() {
        assertTrue(beanType.isBean());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(beanType.isSerializable());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(SerializationType.ENTITY, beanType.getSerializationType());
    }

    @Test
    public void testStringOf() {
        assertNull(beanType.stringOf(null));

        TestBean bean = new TestBean();
        bean.setId(123);
        bean.setName("Test");

        String json = beanType.stringOf(bean);
        assertNotNull(json);
        assertTrue(json.contains("123"));
        assertTrue(json.contains("Test"));
        assertTrue(json.contains("id"));
        assertTrue(json.contains("name"));
    }

    @Test
    public void testValueOf() {
        assertNull(beanType.valueOf(""));
        assertNull(beanType.valueOf((String) null));

        String json = "{\"id\":456,\"name\":\"Example\"}";
        TestBean bean = beanType.valueOf(json);
        assertNotNull(bean);
        assertEquals(456, bean.getId());
        assertEquals("Example", bean.getName());

        assertThrows(Exception.class, () -> beanType.valueOf("{invalid json}"));
    }

    @Test
    public void testRoundTrip() {
        TestBean original = new TestBean();
        original.setId(789);
        original.setName("RoundTrip Test");

        String json = beanType.stringOf(original);
        TestBean decoded = beanType.valueOf(json);

        assertNotNull(decoded);
        assertEquals(original.getId(), decoded.getId());
        assertEquals(original.getName(), decoded.getName());
    }

    public static class TestBean {
        private int id;
        private String name;

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
}
