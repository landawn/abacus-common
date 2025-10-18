package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;

@Tag("2025")
public class DeserializationConfig2025Test extends TestBase {

    private TestDeserializationConfig config;

    private static class TestDeserializationConfig extends DeserializationConfig<TestDeserializationConfig> {
        // Concrete implementation for testing
    }

    private static class TestBean {
        private String name;
        private int age;
        private TestAddress address;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public TestAddress getAddress() {
            return address;
        }

        public void setAddress(TestAddress address) {
            this.address = address;
        }
    }

    private static class TestAddress {
        private String street;
        private String city;

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }
    }

    @BeforeEach
    public void setUp() {
        config = new TestDeserializationConfig();
    }

    @Test
    public void test_ignoreUnmatchedProperty_defaultValue() {
        assertTrue(config.ignoreUnmatchedProperty());
    }

    @Test
    public void test_ignoreUnmatchedProperty_setTrue() {
        config.ignoreUnmatchedProperty(true);
        assertTrue(config.ignoreUnmatchedProperty());
    }

    @Test
    public void test_ignoreUnmatchedProperty_setFalse() {
        config.ignoreUnmatchedProperty(false);
        assertFalse(config.ignoreUnmatchedProperty());
    }

    @Test
    public void test_ignoreUnmatchedProperty_methodChaining() {
        TestDeserializationConfig result = config.ignoreUnmatchedProperty(false);
        assertEquals(config, result);
        assertFalse(config.ignoreUnmatchedProperty());
    }

    @Test
    public void test_getElementType_defaultValue() {
        assertNull(config.getElementType());
    }

    @Test
    public void test_setElementType_withClass() {
        config.setElementType(String.class);
        assertNotNull(config.getElementType());
        assertEquals(String.class, config.getElementType().clazz());
    }

    @Test
    public void test_setElementType_withType() {
        Type<Integer> type = N.typeOf(Integer.class);
        config.setElementType(type);
        assertSame(type, config.getElementType());
    }

    @Test
    public void test_setElementType_withString() {
        config.setElementType("String");
        assertNotNull(config.getElementType());
        assertEquals(String.class, config.getElementType().clazz());
    }

    @Test
    public void test_setElementType_methodChaining() {
        TestDeserializationConfig result = config.setElementType(String.class);
        assertEquals(config, result);
    }

    @Test
    public void test_getMapKeyType_defaultValue() {
        assertNull(config.getMapKeyType());
    }

    @Test
    public void test_setMapKeyType_withClass() {
        config.setMapKeyType(String.class);
        assertNotNull(config.getMapKeyType());
        assertEquals(String.class, config.getMapKeyType().clazz());
    }

    @Test
    public void test_setMapKeyType_withType() {
        Type<String> type = N.typeOf(String.class);
        config.setMapKeyType(type);
        assertSame(type, config.getMapKeyType());
    }

    @Test
    public void test_setMapKeyType_withString() {
        config.setMapKeyType("String");
        assertNotNull(config.getMapKeyType());
        assertEquals(String.class, config.getMapKeyType().clazz());
    }

    @Test
    public void test_setMapKeyType_methodChaining() {
        TestDeserializationConfig result = config.setMapKeyType(String.class);
        assertEquals(config, result);
    }

    @Test
    public void test_getMapValueType_defaultValue() {
        assertNull(config.getMapValueType());
    }

    @Test
    public void test_setMapValueType_withClass() {
        config.setMapValueType(Integer.class);
        assertNotNull(config.getMapValueType());
        assertEquals(Integer.class, config.getMapValueType().clazz());
    }

    @Test
    public void test_setMapValueType_withType() {
        Type<Integer> type = N.typeOf(Integer.class);
        config.setMapValueType(type);
        assertSame(type, config.getMapValueType());
    }

    @Test
    public void test_setMapValueType_withString() {
        config.setMapValueType("Integer");
        assertNotNull(config.getMapValueType());
        assertEquals(Integer.class, config.getMapValueType().clazz());
    }

    @Test
    public void test_setMapValueType_methodChaining() {
        TestDeserializationConfig result = config.setMapValueType(Integer.class);
        assertEquals(config, result);
    }

    @Test
    public void test_hasValueTypes_defaultFalse() {
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void test_hasValueTypes_withValueTypeMap() {
        config.setValueType("key", String.class);
        assertTrue(config.hasValueTypes());
    }

    @Test
    public void test_hasValueTypes_withBeanClass() {
        config.setValueTypesByBeanClass(TestBean.class);
        assertTrue(config.hasValueTypes());
    }

    @Test
    public void test_getValueType_withoutDefault() {
        assertNull(config.getValueType("unknown"));
    }

    @Test
    public void test_getValueType_withDefault() {
        Type<String> defaultType = N.typeOf(String.class);
        Type<String> result = config.getValueType("unknown", defaultType);
        assertSame(defaultType, result);
    }

    @Test
    public void test_getValueType_configured() {
        config.setValueType("name", String.class);
        Type<?> result = config.getValueType("name");
        assertNotNull(result);
        assertEquals(String.class, result.clazz());
    }

    @Test
    public void test_getValueTypeClass_withoutDefault() {
        assertNull(config.getValueTypeClass("unknown"));
    }

    @Test
    public void test_getValueTypeClass_withDefault() {
        Class<?> result = config.getValueTypeClass("unknown", String.class);
        assertEquals(String.class, result);
    }

    @Test
    public void test_getValueTypeClass_configured() {
        config.setValueType("name", String.class);
        Class<?> result = config.getValueTypeClass("name");
        assertEquals(String.class, result);
    }

    @Test
    public void test_setValueType_withClass() {
        config.setValueType("name", String.class);
        assertEquals(String.class, config.getValueTypeClass("name"));
    }

    @Test
    public void test_setValueType_withType() {
        Type<String> type = N.typeOf(String.class);
        config.setValueType("name", type);
        assertSame(type, config.getValueType("name"));
    }

    @Test
    public void test_setValueType_withString() {
        config.setValueType("name", "String");
        assertEquals(String.class, config.getValueTypeClass("name"));
    }

    @Test
    public void test_setValueType_methodChaining() {
        TestDeserializationConfig result = config.setValueType("name", String.class);
        assertEquals(config, result);
    }

    @Test
    public void test_setValueTypes_withMap() {
        Map<String, Type<?>> types = new HashMap<>();
        types.put("name", N.typeOf(String.class));
        types.put("age", N.typeOf(Integer.class));

        config.setValueTypes(types);

        assertEquals(String.class, config.getValueTypeClass("name"));
        assertEquals(Integer.class, config.getValueTypeClass("age"));
    }

    @Test
    public void test_setValueTypes_methodChaining() {
        Map<String, Type<?>> types = new HashMap<>();
        TestDeserializationConfig result = config.setValueTypes(types);
        assertEquals(config, result);
    }

    @Test
    public void test_setValueTypesByBeanClass() {
        config.setValueTypesByBeanClass(TestBean.class);
        assertNotNull(config.getValueType("name"));
        assertNotNull(config.getValueType("age"));
        assertNotNull(config.getValueType("address"));
    }

    @Test
    public void test_setValueTypesByBeanClass_withNull() {
        config.setValueTypesByBeanClass(TestBean.class);
        assertTrue(config.hasValueTypes());

        config.setValueTypesByBeanClass(null);
        assertFalse(config.hasValueTypes());
    }

    @Test
    public void test_setValueTypesByBeanClass_methodChaining() {
        TestDeserializationConfig result = config.setValueTypesByBeanClass(TestBean.class);
        assertEquals(config, result);
    }

    @Test
    public void test_hashCode_defaultConfig() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        TestDeserializationConfig config2 = new TestDeserializationConfig();
        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_hashCode_withSameSettings() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setElementType(String.class);
        config1.ignoreUnmatchedProperty(false);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setElementType(String.class);
        config2.ignoreUnmatchedProperty(false);

        assertEquals(config1.hashCode(), config2.hashCode());
    }

    @Test
    public void test_equals_sameInstance() {
        assertTrue(config.equals(config));
    }

    @Test
    public void test_equals_defaultConfigs() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        TestDeserializationConfig config2 = new TestDeserializationConfig();
        assertTrue(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentIgnoreUnmatchedProperty() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.ignoreUnmatchedProperty(true);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.ignoreUnmatchedProperty(false);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentElementType() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setElementType(String.class);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setElementType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentMapKeyType() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setMapKeyType(String.class);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setMapKeyType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withDifferentMapValueType() {
        TestDeserializationConfig config1 = new TestDeserializationConfig();
        config1.setMapValueType(String.class);

        TestDeserializationConfig config2 = new TestDeserializationConfig();
        config2.setMapValueType(Integer.class);

        assertFalse(config1.equals(config2));
    }

    @Test
    public void test_equals_withNull() {
        assertFalse(config.equals(null));
    }

    @Test
    public void test_equals_withDifferentType() {
        assertFalse(config.equals("not a config"));
    }

    @Test
    public void test_toString_defaultConfig() {
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("ignoredPropNames"));
        assertTrue(result.contains("ignoreUnmatchedProperty"));
        assertTrue(result.contains("elementType"));
        assertTrue(result.contains("mapKeyType"));
        assertTrue(result.contains("mapValueType"));
    }

    @Test
    public void test_toString_withElementType() {
        config.setElementType(String.class);
        String result = config.toString();
        assertNotNull(result);
        assertTrue(result.contains("String"));
    }

    @Test
    public void test_copy_createsNewInstance() {
        TestDeserializationConfig copy = config.copy();
        assertNotNull(copy);
        assertNotSame(config, copy);
    }

    @Test
    public void test_copy_copiesSettings() {
        config.setElementType(String.class);
        config.setMapKeyType(String.class);
        config.setMapValueType(Integer.class);
        config.ignoreUnmatchedProperty(false);

        TestDeserializationConfig copy = config.copy();

        assertNotNull(copy.getElementType());
        assertNotNull(copy.getMapKeyType());
        assertNotNull(copy.getMapValueType());
        assertFalse(copy.ignoreUnmatchedProperty());
    }

    @Test
    public void test_methodChaining_complex() {
        TestDeserializationConfig result = config.ignoreUnmatchedProperty(false)
                .setElementType(String.class)
                .setMapKeyType(String.class)
                .setMapValueType(Integer.class)
                .setValueType("name", String.class);

        assertEquals(config, result);
        assertFalse(config.ignoreUnmatchedProperty());
        assertNotNull(config.getElementType());
        assertNotNull(config.getMapKeyType());
        assertNotNull(config.getMapValueType());
        assertNotNull(config.getValueType("name"));
    }
}
